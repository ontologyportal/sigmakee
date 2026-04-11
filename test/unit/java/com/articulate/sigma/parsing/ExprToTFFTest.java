package com.articulate.sigma.parsing;

import org.junit.Test;

import java.util.List;
import java.util.Map;

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
    public void testTranslateNumber() {
        // Formulas with numeric literals fall back to SUMOtoTFAform (returns null)
        // because arithmetic sort handling (predicate name mangling) is not implemented here.
        Expr expr = se("equal", num("42"), num("0"));
        String result = ExprToTFF.translate(expr, false, null);
        assertNull(result);
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
}
