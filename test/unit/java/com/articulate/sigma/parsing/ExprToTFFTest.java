package com.articulate.sigma.parsing;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ExprToTFF}.
 *
 * <p>All tests run without a live KB (kb=null); sort inference relies solely on
 * {@code (instance ?X Type)} patterns within the formula.</p>
 */
public class ExprToTFFTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Expr.Atom     atom(String s)    { return new Expr.Atom(s); }
    private static Expr.Var      var(String s)     { return new Expr.Var(s); }
    private static Expr.NumLiteral num(String v)   { return new Expr.NumLiteral(v); }

    private static Expr.SExpr se(String head, Expr... args) {
        return new Expr.SExpr(new Expr.Atom(head), List.of(args));
    }
    private static Expr.SExpr varList(Expr... vars) {
        return new Expr.SExpr(null, List.of(vars));
    }

    // -----------------------------------------------------------------------
    // translateSortName
    // -----------------------------------------------------------------------

    @Test
    public void testSortNameInteger() {
        assertEquals("$int", ExprToTFF.translateSortName("Integer", null));
    }

    @Test
    public void testSortNameRealNumber() {
        assertEquals("$real", ExprToTFF.translateSortName("RealNumber", null));
    }

    @Test
    public void testSortNameRationalNumber() {
        assertEquals("$rat", ExprToTFF.translateSortName("RationalNumber", null));
    }

    @Test
    public void testSortNameSumoClass() {
        assertEquals("s__Dog", ExprToTFF.translateSortName("Dog", null));
    }

    @Test
    public void testSortNameNull() {
        assertEquals("$i", ExprToTFF.translateSortName(null, null));
    }

    @Test
    public void testSortNameEmpty() {
        assertEquals("$i", ExprToTFF.translateSortName("", null));
    }

    @Test
    public void testSortNamePassThrough() {
        assertEquals("$i",     ExprToTFF.translateSortName("$i",     null));
        assertEquals("$tType", ExprToTFF.translateSortName("$tType", null));
    }

    // -----------------------------------------------------------------------
    // inferVarSorts
    // -----------------------------------------------------------------------

    @Test
    public void testInferVarSortsFromInstance() {
        // (instance ?X Dog) — variable sort is always $i (no type declarations generated)
        Expr expr = se("instance", var("?X"), atom("Dog"));
        Map<String, String> sorts = ExprToTFF.inferVarSorts(expr, null);
        assertEquals("$i", sorts.get("?X"));
    }

    @Test
    public void testInferVarSortsFirstWins() {
        // (and (instance ?X Dog) (instance ?X Animal)) — all map to $i
        Expr expr = se("and",
                se("instance", var("?X"), atom("Dog")),
                se("instance", var("?X"), atom("Animal")));
        Map<String, String> sorts = ExprToTFF.inferVarSorts(expr, null);
        assertEquals("$i", sorts.get("?X"));
    }

    @Test
    public void testInferVarSortsNoConstraint() {
        Expr expr = se("instance", atom("Fido"), atom("Dog"));
        Map<String, String> sorts = ExprToTFF.inferVarSorts(expr, null);
        assertTrue(sorts.isEmpty());
    }

    // -----------------------------------------------------------------------
    // translate — simple formulas
    // -----------------------------------------------------------------------

    @Test
    public void testTranslateAtom() {
        // (instance Fido Dog) — no free vars, no sort wrapper
        Expr expr = se("instance", atom("Fido"), atom("Dog"));
        String result = ExprToTFF.translate(expr, false, null);
        assertNotNull(result);
        assertTrue(result.contains("s__instance"));
        assertTrue(result.contains("s__Fido"));
        assertTrue(result.contains("s__Dog"));
    }

    @Test
    public void testTranslateFreeVarGetsDefaultSort() {
        // (instance ?X Dog) — free var ?X gets $i (no KB to resolve Dog→sub of Integer)
        Expr expr = se("instance", var("?X"), atom("Dog"));
        String result = ExprToTFF.translate(expr, false, null);
        assertNotNull(result);
        // ?X gets $i — ExprToTFF doesn't generate type declarations so named sorts are unsafe
        assertTrue("Expected sort annotation for V__X", result.contains("V__X : $i"));
    }

    @Test
    public void testTranslateNot() {
        Expr expr = se("not", se("instance", atom("Fido"), atom("Dog")));
        String result = ExprToTFF.translate(expr, false, null);
        assertNotNull(result);
        assertTrue(result.startsWith("~("));
    }

    @Test
    public void testTranslateAnd() {
        Expr expr = se("and",
                se("instance", atom("Fido"), atom("Dog")),
                se("instance", atom("Rex"),  atom("Dog")));
        String result = ExprToTFF.translate(expr, false, null);
        assertNotNull(result);
        assertTrue(result.contains(" & "));
    }

    @Test
    public void testTranslateImplication() {
        // (=> (instance ?X Dog) (instance ?X Animal))
        Expr expr = se("=>",
                se("instance", var("?X"), atom("Dog")),
                se("instance", var("?X"), atom("Animal")));
        String result = ExprToTFF.translate(expr, false, null);
        assertNotNull(result);
        assertTrue(result.contains(" => "));
    }

    @Test
    public void testTranslateForall() {
        // (forall (?X) (instance ?X Dog))
        Expr expr = se("forall",
                varList(var("?X")),
                se("instance", var("?X"), atom("Dog")));
        String result = ExprToTFF.translate(expr, false, null);
        assertNotNull(result);
        assertTrue("Expected typed quantifier", result.contains("! [V__X : $i]"));
    }

    @Test
    public void testTranslateExists() {
        Expr expr = se("exists",
                varList(var("?X")),
                se("instance", var("?X"), atom("Cat")));
        String result = ExprToTFF.translate(expr, false, null);
        assertNotNull(result);
        assertTrue(result.contains("? [V__X : $i]"));
    }

    @Test
    public void testTranslateUnknownVarGetsDefaultSort() {
        // (forall (?X) (subclass ?X Entity)) — no instance pattern → $i
        Expr expr = se("forall",
                varList(var("?X")),
                se("subclass", var("?X"), atom("Entity")));
        String result = ExprToTFF.translate(expr, false, null);
        assertNotNull(result);
        assertTrue("Expected $i for unconstrained var", result.contains("V__X : $i"));
    }

    @Test
    public void testTranslateEqual() {
        Expr expr = se("equal", atom("Fido"), atom("Rex"));
        String result = ExprToTFF.translate(expr, false, null);
        assertNotNull(result);
        assertTrue(result.contains(" = "));
    }

    @Test
    public void testTranslateNumberWithoutKBReturnsNull() {
        // Numeric literals without a KB cannot be handled — return null for fallback
        Expr expr = se("equal", num("42"), num("0"));
        String result = ExprToTFF.translate(expr, false, null);
        assertNull("Expected null when kb=null and formula has numeric literals", result);
    }

    // -----------------------------------------------------------------------
    // translateKifString
    // -----------------------------------------------------------------------

    @Test
    public void testTranslateKifStringSimple() {
        String result = ExprToTFF.translateKifString("(instance Fido Dog)", false, null);
        assertNotNull(result);
        assertTrue(result.contains("s__instance"));
    }

    @Test
    public void testTranslateKifStringNullOnBadInput() {
        String result = ExprToTFF.translateKifString("(=> ", false, null);
        assertNull(result);
    }

    @Test
    public void testTranslateKifStringForall() {
        String result = ExprToTFF.translateKifString(
                "(forall (?X) (=> (instance ?X Dog) (subclass ?X Animal)))", false, null);
        assertNotNull(result);
        assertTrue(result.contains("V__X : $i"));
    }

    // -----------------------------------------------------------------------
    // instantiateNumericConstantsExpr
    // -----------------------------------------------------------------------

    @Test
    public void testInstantiateNumericConstantsExprPi() {
        // (equal Pi 3) → (equal 3.141592653589793 3)
        Expr expr = se("equal", atom("Pi"), num("3"));
        Expr result = ExprToTFF.instantiateNumericConstantsExpr(expr);
        assertNotNull(result);
        assertTrue("Pi should be replaced with its decimal value",
                result.toKifString().contains("3.141592653589793"));
        assertFalse("Pi atom should be gone", result.toKifString().contains("Pi"));
    }

    @Test
    public void testInstantiateNumericConstantsExprNumberE() {
        // (equal NumberE x) → (equal 2.718282 x)
        Expr expr = se("equal", atom("NumberE"), atom("x"));
        Expr result = ExprToTFF.instantiateNumericConstantsExpr(expr);
        assertNotNull(result);
        assertTrue("NumberE should be replaced", result.toKifString().contains("2.718282"));
    }

    @Test
    public void testInstantiateNumericConstantsExprInstancePiDropped() {
        // (instance Pi RealNumber) → null (formula dropped)
        Expr expr = se("instance", atom("Pi"), atom("RealNumber"));
        Expr result = ExprToTFF.instantiateNumericConstantsExpr(expr);
        assertNull("(instance Pi ...) should be dropped", result);
    }

    @Test
    public void testInstantiateNumericConstantsExprInstanceNumberEDropped() {
        // (instance NumberE RealNumber) → null
        Expr expr = se("instance", atom("NumberE"), atom("RealNumber"));
        Expr result = ExprToTFF.instantiateNumericConstantsExpr(expr);
        assertNull("(instance NumberE ...) should be dropped", result);
    }

    @Test
    public void testInstantiateNumericConstantsExprNoConstants() {
        // (instance Fido Dog) → unchanged
        Expr expr = se("instance", atom("Fido"), atom("Dog"));
        Expr result = ExprToTFF.instantiateNumericConstantsExpr(expr);
        assertEquals("No constants to replace: formula unchanged", expr, result);
    }

    @Test
    public void testInstantiateNumericConstantsExprNested() {
        // (=> (instance ?X Foo) (equal Pi 0)) → substitutes Pi deep in tree
        Expr expr = se("=>",
                se("instance", var("?X"), atom("Foo")),
                se("equal", atom("Pi"), num("0")));
        Expr result = ExprToTFF.instantiateNumericConstantsExpr(expr);
        assertNotNull(result);
        assertTrue(result.toKifString().contains("3.141592653589793"));
    }

    // -----------------------------------------------------------------------
    // modifyPrecondExpr
    // -----------------------------------------------------------------------

    @Test
    public void testModifyPrecondRemovesInteger() {
        // (instance ?X Integer) should be removed (returns null)
        Expr expr = se("instance", var("?X"), atom("Integer"));
        Expr result = ExprToTFF.modifyPrecondExpr(expr);
        assertNull("(instance ?X Integer) should be removed", result);
    }

    @Test
    public void testModifyPrecondRemovesRealNumber() {
        // (instance ?X RealNumber) should be removed
        Expr expr = se("instance", var("?X"), atom("RealNumber"));
        Expr result = ExprToTFF.modifyPrecondExpr(expr);
        assertNull("(instance ?X RealNumber) should be removed", result);
    }

    @Test
    public void testModifyPrecondDoesNotRemoveOtherInstances() {
        // (instance ?X Dog) should be kept
        Expr expr = se("instance", var("?X"), atom("Dog"));
        Expr result = ExprToTFF.modifyPrecondExpr(expr);
        assertNotNull("(instance ?X Dog) should be kept", result);
        assertEquals(expr, result);
    }

    @Test
    public void testModifyPrecondCompound() {
        // (and (instance ?X Integer) (instance ?X Dog)) → (and (instance ?X Dog))
        // which after elimUnitaryLogops becomes (instance ?X Dog)
        Expr expr = se("and",
                se("instance", var("?X"), atom("Integer")),
                se("instance", var("?X"), atom("Dog")));
        Expr result = ExprToTFF.modifyPrecondExpr(expr);
        assertNotNull(result);
        // The (and (instance ?X Integer)) part is removed, leaving (and (instance ?X Dog))
        // which is a unary and - elimUnitaryLogops will fix that
        String kif = result.toKifString();
        assertFalse("Integer instance should be gone", kif.contains("Integer"));
        assertTrue("Dog instance should remain", kif.contains("Dog"));
    }

    @Test
    public void testModifyPrecondImplication() {
        // (=> (instance ?X Integer) (foo ?X)) → (=> (foo ?X)) — unary =>
        Expr expr = se("=>",
                se("instance", var("?X"), atom("Integer")),
                se("foo", var("?X")));
        Expr result = ExprToTFF.modifyPrecondExpr(expr);
        assertNotNull(result);
        assertFalse("Integer instance removed from antecedent", result.toKifString().contains("Integer"));
    }

    // -----------------------------------------------------------------------
    // elimUnitaryLogopsExpr
    // -----------------------------------------------------------------------

    @Test
    public void testElimUnitaryLogopsNoArgs() {
        // (and) → null
        Expr expr = new Expr.SExpr(atom("and"), List.of());
        Expr result = ExprToTFF.elimUnitaryLogopsExpr(expr);
        assertNull("(and) with no args should yield null", result);
    }

    @Test
    public void testElimUnitaryLogopsOneArg() {
        // (and (instance Fido Dog)) → (instance Fido Dog)
        Expr inner = se("instance", atom("Fido"), atom("Dog"));
        Expr expr = new Expr.SExpr(atom("and"), List.of(inner));
        Expr result = ExprToTFF.elimUnitaryLogopsExpr(expr);
        assertNotNull(result);
        assertEquals("Unary and should unwrap to its single argument", inner, result);
    }

    @Test
    public void testElimUnitaryLogopsOrOneArg() {
        // (or (foo ?X)) → (foo ?X)
        Expr inner = se("foo", var("?X"));
        Expr expr = new Expr.SExpr(atom("or"), List.of(inner));
        Expr result = ExprToTFF.elimUnitaryLogopsExpr(expr);
        assertNotNull(result);
        assertEquals(inner, result);
    }

    @Test
    public void testElimUnitaryLogopsTwoArgs() {
        // (and (instance Fido Dog) (instance Rex Dog)) → unchanged
        Expr inner1 = se("instance", atom("Fido"), atom("Dog"));
        Expr inner2 = se("instance", atom("Rex"), atom("Dog"));
        Expr expr = se("and", inner1, inner2);
        Expr result = ExprToTFF.elimUnitaryLogopsExpr(expr);
        assertNotNull(result);
        assertEquals("Two-arg and should be unchanged", expr, result);
    }

    @Test
    public void testElimUnitaryLogopsNonLogical() {
        // (instance Fido Dog) is not a logical op → unchanged
        Expr expr = se("instance", atom("Fido"), atom("Dog"));
        Expr result = ExprToTFF.elimUnitaryLogopsExpr(expr);
        assertNotNull(result);
        assertEquals(expr, result);
    }

    @Test
    public void testElimUnitaryLogopsNestedUnary() {
        // (=> (and (foo ?X)) (bar ?X)) → (=> (foo ?X) (bar ?X))
        Expr expr = se("=>",
                new Expr.SExpr(atom("and"), List.of(se("foo", var("?X")))),
                se("bar", var("?X")));
        Expr result = ExprToTFF.elimUnitaryLogopsExpr(expr);
        assertNotNull(result);
        // The inner (and (foo ?X)) should become (foo ?X)
        assertFalse("Unary and should be eliminated", result.toKifString().contains("and"));
    }

    // -----------------------------------------------------------------------
    // collectAtomNamesExpr
    // -----------------------------------------------------------------------

    @Test
    public void testCollectAtomNamesSimple() {
        Expr expr = se("instance", atom("Fido"), atom("Dog"));
        Set<String> names = ExprToTFF.collectAtomNamesExpr(expr);
        assertTrue(names.contains("instance"));
        assertTrue(names.contains("Fido"));
        assertTrue(names.contains("Dog"));
    }

    @Test
    public void testCollectAtomNamesNested() {
        Expr expr = se("and", se("foo", atom("Pi")), se("bar", atom("NumberE")));
        Set<String> names = ExprToTFF.collectAtomNamesExpr(expr);
        assertTrue(names.contains("Pi"));
        assertTrue(names.contains("NumberE"));
    }

    @Test
    public void testCollectAtomNamesVarsNotIncluded() {
        // Vars (like ?X) are not Atoms — they should not appear
        Expr expr = se("instance", var("?X"), atom("Dog"));
        Set<String> names = ExprToTFF.collectAtomNamesExpr(expr);
        assertFalse("Variables should not be collected as atom names", names.contains("?X"));
        assertTrue(names.contains("Dog"));
    }

    // -----------------------------------------------------------------------
    // replaceAtomWithNumLiteralExpr
    // -----------------------------------------------------------------------

    @Test
    public void testReplaceAtomWithNumLiteralSimple() {
        Expr expr = atom("Pi");
        Map<String, String> replacements = Map.of("Pi", "3.14");
        Expr result = ExprToTFF.replaceAtomWithNumLiteralExpr(expr, replacements);
        assertTrue("Atom should be replaced with NumLiteral", result instanceof Expr.NumLiteral);
        assertEquals("3.14", ((Expr.NumLiteral) result).value());
    }

    @Test
    public void testReplaceAtomWithNumLiteralNoMatch() {
        Expr expr = atom("Dog");
        Map<String, String> replacements = Map.of("Pi", "3.14");
        Expr result = ExprToTFF.replaceAtomWithNumLiteralExpr(expr, replacements);
        assertEquals("No replacement for Dog — should be unchanged", expr, result);
    }

    @Test
    public void testReplaceAtomWithNumLiteralNested() {
        Expr expr = se("equal", atom("Pi"), atom("Foo"));
        Map<String, String> replacements = Map.of("Pi", "3.14");
        Expr result = ExprToTFF.replaceAtomWithNumLiteralExpr(expr, replacements);
        // The Pi should be replaced; Foo should be untouched
        Expr.SExpr se = (Expr.SExpr) result;
        assertTrue("First arg should be NumLiteral", se.args().get(0) instanceof Expr.NumLiteral);
        assertTrue("Second arg should remain Atom", se.args().get(1) instanceof Expr.Atom);
    }

    // -----------------------------------------------------------------------
    // removeInstanceOfExpr
    // -----------------------------------------------------------------------

    @Test
    public void testRemoveInstanceOfDirectMatch() {
        Expr expr = se("instance", var("?X"), atom("Integer"));
        Expr result = ExprToTFF.removeInstanceOfExpr(expr, Set.of("Integer"));
        assertNull("Matching instance should be removed", result);
    }

    @Test
    public void testRemoveInstanceOfNoMatch() {
        Expr expr = se("instance", var("?X"), atom("Dog"));
        Expr result = ExprToTFF.removeInstanceOfExpr(expr, Set.of("Integer", "RealNumber"));
        assertNotNull("Non-matching instance should be kept", result);
        assertEquals(expr, result);
    }

    @Test
    public void testRemoveInstanceOfInAnd() {
        // (and (instance ?X Integer) (instance ?X Dog)) → (and (instance ?X Dog))
        Expr toRemove = se("instance", var("?X"), atom("Integer"));
        Expr toKeep   = se("instance", var("?X"), atom("Dog"));
        Expr expr = se("and", toRemove, toKeep);
        Expr result = ExprToTFF.removeInstanceOfExpr(expr, Set.of("Integer"));
        assertNotNull(result);
        assertFalse("Integer instance removed", result.toKifString().contains("Integer"));
        assertTrue("Dog instance kept", result.toKifString().contains("Dog"));
    }

    // -----------------------------------------------------------------------
    // isBuiltInOrSubNumericType
    // -----------------------------------------------------------------------

    @Test
    public void testIsBuiltInOrSubNumericTypeInteger() {
        assertTrue(ExprToTFF.isBuiltInOrSubNumericType("Integer"));
    }

    @Test
    public void testIsBuiltInOrSubNumericTypeRealNumber() {
        assertTrue(ExprToTFF.isBuiltInOrSubNumericType("RealNumber"));
    }

    @Test
    public void testIsBuiltInOrSubNumericTypeRationalNumber() {
        assertTrue(ExprToTFF.isBuiltInOrSubNumericType("RationalNumber"));
    }

    @Test
    public void testIsBuiltInOrSubNumericTypeNonNumeric() {
        assertFalse(ExprToTFF.isBuiltInOrSubNumericType("Dog"));
        assertFalse(ExprToTFF.isBuiltInOrSubNumericType("Entity"));
    }

    @Test
    public void testIsBuiltInOrSubNumericTypeNullAndEmpty() {
        assertFalse(ExprToTFF.isBuiltInOrSubNumericType(null));
        assertFalse(ExprToTFF.isBuiltInOrSubNumericType(""));
    }

    // -----------------------------------------------------------------------
    // parseKifToExpr
    // -----------------------------------------------------------------------

    @Test
    public void testParseKifToExprSimple() {
        Expr result = ExprToTFF.parseKifToExpr("(instance Fido Dog)");
        assertNotNull("Should parse successfully", result);
        assertTrue("Should be SExpr", result instanceof Expr.SExpr);
        assertEquals("instance", ((Expr.SExpr) result).headName());
    }

    @Test
    public void testParseKifToExprComparison() {
        Expr result = ExprToTFF.parseKifToExpr("(greaterThan ?V 0)");
        assertNotNull("Should parse (greaterThan ?V 0)", result);
    }

    @Test
    public void testParseKifToExprNull() {
        assertNull("null input → null", ExprToTFF.parseKifToExpr(null));
        assertNull("empty input → null", ExprToTFF.parseKifToExpr(""));
    }

    @Test
    public void testParseKifToExprMalformed() {
        Expr result = ExprToTFF.parseKifToExpr("(=> ");
        assertNull("Malformed KIF should return null", result);
    }

    // -----------------------------------------------------------------------
    // cloneVarmap
    // -----------------------------------------------------------------------

    @Test
    public void testCloneVarmapIsDeepCopy() {
        Map<String, Set<String>> original = new HashMap<>();
        original.put("?X", new HashSet<>(Set.of("Integer")));
        Map<String, Set<String>> clone = ExprToTFF.cloneVarmap(original);

        assertEquals(original, clone);
        // Modifying clone should not affect original
        clone.get("?X").add("RealNumber");
        assertEquals(1, original.get("?X").size());
    }

    @Test
    public void testCloneVarmapEmpty() {
        Map<String, Set<String>> clone = ExprToTFF.cloneVarmap(Collections.emptyMap());
        assertTrue(clone.isEmpty());
    }

    // -----------------------------------------------------------------------
    // inconsistentVarTypesExpr — without KB (null kb → always returns false)
    // -----------------------------------------------------------------------

    @Test
    public void testInconsistentVarTypesNullKB() {
        Map<String, Set<String>> varmap = new HashMap<>();
        varmap.put("?X", new HashSet<>(Set.of("Integer", "Dog")));
        // Without KB, cannot check disjointness → returns false
        assertFalse(ExprToTFF.inconsistentVarTypesExpr(varmap, null));
    }

    @Test
    public void testInconsistentVarTypesNullMap() {
        assertFalse(ExprToTFF.inconsistentVarTypesExpr(null, null));
    }

    @Test
    public void testInconsistentVarTypesSingleType() {
        Map<String, Set<String>> varmap = new HashMap<>();
        varmap.put("?X", new HashSet<>(Set.of("Integer")));
        // Single type — no pair to check — never inconsistent
        assertFalse(ExprToTFF.inconsistentVarTypesExpr(varmap, null));
    }

    // -----------------------------------------------------------------------
    // buildQuantifiedResultArith — no KB (kb=null)
    // -----------------------------------------------------------------------

    @Test
    public void testBuildQuantifiedResultArithNoFreeVars() {
        Map<String, Set<String>> varmap = new HashMap<>();
        Set<String> freeVars = Collections.emptySet();
        String result = ExprToTFF.buildQuantifiedResultArith("s__foo()", false, varmap, freeVars, null);
        assertEquals("No free vars — body returned unchanged", "s__foo()", result);
    }

    @Test
    public void testBuildQuantifiedResultArithFreeVarNoType() {
        // Free var without a type in varmap: not added to quantifier list
        Map<String, Set<String>> varmap = new HashMap<>();
        Set<String> freeVars = Set.of("?X");
        String result = ExprToTFF.buildQuantifiedResultArith("s__foo(V__X)", false, varmap, freeVars, null);
        // No type → qlist stays empty → body returned unchanged
        assertEquals("s__foo(V__X)", result);
    }

    // -----------------------------------------------------------------------
    // modifyTypesToConstraintsExpr — only exercises the substitution infrastructure
    // (numericConstraints is populated by SUMOtoTFAform.initOnce() which needs a KB,
    //  so without a KB the map is empty and no substitution occurs)
    // -----------------------------------------------------------------------

    @Test
    public void testModifyTypesToConstraintsNoConstraints() {
        // With no numericConstraints populated (no initOnce called), the formula is unchanged
        Expr expr = se("instance", var("?X"), atom("NonNegativeInteger"));
        Expr result = ExprToTFF.modifyTypesToConstraintsExpr(expr);
        assertNotNull(result);
        // No constraint map → formula unchanged
        assertEquals(expr, result);
    }

    @Test
    public void testModifyTypesToConstraintsNullInput() {
        assertNull(ExprToTFF.modifyTypesToConstraintsExpr(null));
    }

    // -----------------------------------------------------------------------
    // replaceNumericConstraintPass
    // -----------------------------------------------------------------------

    @Test
    public void testReplaceNumericConstraintPassNoMatch() {
        // If numericConstraints is empty, no replacement happens
        Expr expr = se("instance", var("?X"), atom("NonNegativeInteger"));
        Expr result = ExprToTFF.replaceNumericConstraintPass(expr);
        // When the map is empty (no initOnce), should return the same expression
        assertNotNull(result);
    }

    @Test
    public void testReplaceNumericConstraintPassNonInstanceNode() {
        Expr expr = se("foo", var("?X"), atom("Bar"));
        Expr result = ExprToTFF.replaceNumericConstraintPass(expr);
        assertEquals("Non-instance node should be recursed but not changed", expr, result);
    }

    // -----------------------------------------------------------------------
    // seedMissingVarsToVarmap
    // -----------------------------------------------------------------------

    @Test
    public void testSeedMissingVarsToVarmapAddsVar() {
        Map<String, Set<String>> varmap = new HashMap<>();
        Expr expr = se("greaterThan", var("?X"), num("0"));
        ExprToTFF.seedMissingVarsToVarmap(expr, varmap);
        assertTrue("?X should be seeded with Entity", varmap.containsKey("?X"));
        assertTrue(varmap.get("?X").contains("Entity"));
    }

    @Test
    public void testSeedMissingVarsToVarmapDoesNotOverwriteExisting() {
        Map<String, Set<String>> varmap = new HashMap<>();
        varmap.put("?X", new HashSet<>(Set.of("Integer")));
        Expr expr = se("greaterThan", var("?X"), num("0"));
        ExprToTFF.seedMissingVarsToVarmap(expr, varmap);
        // Should keep Integer, not replace with Entity
        assertTrue("Integer should be preserved", varmap.get("?X").contains("Integer"));
        assertFalse("Entity should not be added when type already exists",
                varmap.get("?X").contains("Entity"));
    }

    @Test
    public void testSeedMissingVarsToVarmapNested() {
        Map<String, Set<String>> varmap = new HashMap<>();
        Expr expr = se("=>",
                se("instance", var("?X"), atom("Foo")),
                se("greaterThan", var("?Y"), var("?Z")));
        ExprToTFF.seedMissingVarsToVarmap(expr, varmap);
        assertTrue("?X seeded", varmap.containsKey("?X"));
        assertTrue("?Y seeded", varmap.containsKey("?Y"));
        assertTrue("?Z seeded", varmap.containsKey("?Z"));
    }

    @Test
    public void testSeedMissingVarsToVarmapIgnoresAtoms() {
        Map<String, Set<String>> varmap = new HashMap<>();
        Expr expr = se("instance", atom("Fido"), atom("Dog"));
        ExprToTFF.seedMissingVarsToVarmap(expr, varmap);
        assertTrue("No vars → varmap remains empty", varmap.isEmpty());
    }

    // -----------------------------------------------------------------------
    // mergeVarmapConstraint — basic coverage without KB
    // -----------------------------------------------------------------------

    @Test
    public void testMergeVarmapConstraintNullKBNoOp() {
        Map<String, Set<String>> varmap   = new HashMap<>();
        Map<String, Set<String>> newTypes = new HashMap<>();
        newTypes.put("?X", new HashSet<>(Set.of("Integer")));
        // null KB → isSubclass always false → oldVarTypes stays as-is (was empty, so newTypes wins)
        ExprToTFF.mergeVarmapConstraint(varmap, newTypes, null);
        // newt=Integer, oldt=null → varmap gets newTypes
        assertTrue(varmap.containsKey("?X"));
    }

    @Test
    public void testMergeVarmapConstraintEmptyNewTypes() {
        Map<String, Set<String>> varmap = new HashMap<>();
        varmap.put("?X", new HashSet<>(Set.of("Integer")));
        ExprToTFF.mergeVarmapConstraint(varmap, Collections.emptyMap(), null);
        // Nothing to merge → varmap unchanged
        assertEquals(Set.of("Integer"), varmap.get("?X"));
    }
}
