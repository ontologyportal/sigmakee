package com.articulate.sigma.parsing;

import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ExprToTHF}.
 *
 * <p>All tests run without a live KB — ExprToTHF defers KB-dependent decisions
 * (relation mention-suffix) to ExprToTPTP which guards those with try-catch.</p>
 */
public class ExprToTHFTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Expr.Atom  atom(String s)    { return new Expr.Atom(s); }
    private static Expr.Var   var(String s)      { return new Expr.Var(s); }
    private static Expr.NumLiteral num(String v) { return new Expr.NumLiteral(v); }

    private static Expr.SExpr se(String head, Expr... args) {
        return new Expr.SExpr(new Expr.Atom(head), List.of(args));
    }

    /** Null-head SExpr: used for quantifier var lists. */
    private static Expr.SExpr varList(Expr... vars) {
        return new Expr.SExpr(null, List.of(vars));
    }

    private static Map<String, Set<String>> typeMap(String var, String... types) {
        Map<String, Set<String>> m = new HashMap<>();
        Set<String> ts = new HashSet<>(List.of(types));
        m.put(var, ts);
        return m;
    }

    private static Map<String, Set<String>> emptyMap() {
        return new HashMap<>();
    }

    // -----------------------------------------------------------------------
    // thfType() — variable type resolution
    // -----------------------------------------------------------------------

    @Test
    public void thfType_worldVar_byConvention() {
        // ?W1 matches the ?W\d+ pattern → "w"
        assertEquals("w", ExprToTHF.thfType("?W1", emptyMap(), true));
    }

    @Test
    public void thfType_worldVar_deepNesting() {
        // ?W3 also matches pattern even if not in typeMap
        assertEquals("w", ExprToTHF.thfType("?W3", emptyMap(), true));
    }

    @Test
    public void thfType_worldType_inMap() {
        Map<String, Set<String>> tm = typeMap("?X", "World");
        assertEquals("w", ExprToTHF.thfType("?X", tm, true));
    }

    @Test
    public void thfType_formulaType_modalMode() {
        Map<String, Set<String>> tm = typeMap("?F", "Formula");
        assertEquals("(w > $o)", ExprToTHF.thfType("?F", tm, true));
    }

    @Test
    public void thfType_formulaType_nonModalMode() {
        Map<String, Set<String>> tm = typeMap("?F", "Formula");
        assertEquals("$o", ExprToTHF.thfType("?F", tm, false));
    }

    @Test
    public void thfType_modalType() {
        Map<String, Set<String>> tm = typeMap("?M", "Modal");
        assertEquals("m", ExprToTHF.thfType("?M", tm, true));
    }

    @Test
    public void thfType_unknownVar_defaultsToI() {
        assertEquals("$i", ExprToTHF.thfType("?X", emptyMap(), true));
    }

    @Test
    public void thfType_nonModal_unknownVar_defaultsToI() {
        assertEquals("$i", ExprToTHF.thfType("?X", emptyMap(), false));
    }

    // -----------------------------------------------------------------------
    // translateNumberTHF()
    // -----------------------------------------------------------------------

    @Test
    public void number_positive_integer() {
        assertEquals("n__42", ExprToTHF.translateNumberTHF("42"));
    }

    @Test
    public void number_float() {
        assertEquals("n__3_14", ExprToTHF.translateNumberTHF("3.14"));
    }

    @Test
    public void number_negative() {
        assertEquals("n___5", ExprToTHF.translateNumberTHF("-5"));
    }

    // -----------------------------------------------------------------------
    // Atomic translation (delegates to ExprToTPTP.translateAtom)
    // -----------------------------------------------------------------------

    @Test
    public void translateExpr_var() {
        String result = ExprToTHF.translateExpr(var("?X"), false, emptyMap(), true);
        assertEquals("V__X", result);
    }

    @Test
    public void translateExpr_numLiteral() {
        String result = ExprToTHF.translateExpr(num("42"), false, emptyMap(), true);
        assertEquals("n__42", result);
    }

    @Test
    public void translateExpr_atom_inArgPosition() {
        // Uppercase atom in argument position → s__Animal (no __m since not a relation)
        String result = ExprToTHF.translateExpr(atom("Animal"), false, emptyMap(), true);
        assertEquals("s__Animal", result);
    }

    @Test
    public void translateExpr_atom_head() {
        String result = ExprToTHF.translateExpr(atom("instance"), true, emptyMap(), true);
        assertEquals("s__instance", result);
    }

    // -----------------------------------------------------------------------
    // Logical operators
    // -----------------------------------------------------------------------

    @Test
    public void translate_not() {
        // (not (foo ?X)) → ~(s__foo @ V__X)
        Expr expr = se("not", se("foo", var("?X")));
        String result = ExprToTHF.translate(expr, false, emptyMap());
        assertTrue("should contain ~(", result.contains("~("));
        assertTrue("should contain s__foo", result.contains("s__foo"));
    }

    @Test
    public void translate_and_twoArgs() {
        Expr expr = se("and",
                se("foo", var("?X")),
                se("bar", var("?Y")));
        String result = ExprToTHF.translate(expr, false, emptyMap());
        assertTrue("should contain &", result.contains("&"));
        assertTrue("should contain s__foo", result.contains("s__foo"));
        assertTrue("should contain s__bar", result.contains("s__bar"));
    }

    @Test
    public void translate_or() {
        Expr expr = se("or",
                se("foo", var("?X")),
                se("bar", var("?X")));
        String result = ExprToTHF.translate(expr, false, emptyMap());
        assertTrue("should contain |", result.contains("|"));
    }

    @Test
    public void translate_implication() {
        // (=> ant con) → (ant => con)
        Expr expr = se("=>",
                se("instance", var("?X"), atom("Animal")),
                se("hasPart", var("?X"), var("?Y")));
        String result = ExprToTHF.translate(expr, false, emptyMap());
        assertTrue("should contain =>", result.contains("=>"));
    }

    @Test
    public void translate_biconditional_expands() {
        // (<=> A B) → ((A => B) & (B => A))
        Expr expr = se("<=>",
                se("foo", var("?X")),
                se("bar", var("?X")));
        String result = ExprToTHF.translate(expr, false, emptyMap());
        assertTrue("should expand biconditional", result.contains("=>") && result.contains("&"));
    }

    @Test
    public void translate_equal() {
        // (equal A B) → (A = B)
        Expr expr = se("equal", var("?X"), var("?Y"));
        String result = ExprToTHF.translate(expr, false, emptyMap());
        assertTrue("should contain =", result.contains("="));
    }

    // -----------------------------------------------------------------------
    // Application — curried @ syntax
    // -----------------------------------------------------------------------

    @Test
    public void translate_application_oneArg() {
        // (foo ?X) → (s__foo @ V__X)
        Expr expr = se("foo", var("?X"));
        String result = ExprToTHF.translate(expr, false, emptyMap());
        assertTrue("should use curried @: " + result, result.contains("@"));
        assertTrue("should have s__foo", result.contains("s__foo"));
        assertTrue("should have V__X", result.contains("V__X"));
    }

    @Test
    public void translate_application_twoArgs() {
        // (instance ?X Animal) → (s__instance @ V__X @ s__Animal)
        Expr expr = se("instance", var("?X"), atom("Animal"));
        String result = ExprToTHF.translate(expr, false, emptyMap());
        assertTrue("two-arg: should use @: " + result, result.contains("@"));
        assertTrue("should have s__instance", result.contains("s__instance"));
        assertTrue("should have V__X", result.contains("V__X"));
        assertTrue("should have s__Animal", result.contains("s__Animal"));
        // Must NOT use FOF functional notation
        assertFalse("must NOT use s__instance(", result.contains("s__instance("));
    }

    @Test
    public void translate_nestedApplication() {
        // (foo (bar ?X)) → (s__foo @ (s__bar @ V__X))
        Expr expr = se("foo", se("bar", var("?X")));
        String result = ExprToTHF.translate(expr, false, emptyMap());
        assertTrue("should have nested @", result.contains("@"));
    }

    // -----------------------------------------------------------------------
    // Typed quantifiers
    // -----------------------------------------------------------------------

    @Test
    public void translate_forall_typedVars() {
        // (forall (?X) (foo ?X)) → ( ! [V__X:$i] : (s__foo @ V__X) )
        Expr expr = se("forall",
                varList(var("?X")),
                se("foo", var("?X")));
        String result = ExprToTHF.translate(expr, false, emptyMap());
        assertTrue("should have ! [", result.contains("! ["));
        assertTrue("should have V__X:$i", result.contains("V__X:$i"));
    }

    @Test
    public void translate_forall_worldVar() {
        // When ?W1 is in the var list, it should get type "w"
        Map<String, Set<String>> tm = new HashMap<>();  // empty; ?W1 typed by regex
        Expr expr = se("forall",
                varList(var("?X"), var("?W1")),
                se("foo", var("?X"), var("?W1")));
        String result = ExprToTHF.translate(expr, false, tm);
        assertTrue("should have V__W1:w: " + result, result.contains("V__W1:w"));
        assertTrue("should have V__X:$i", result.contains("V__X:$i"));
    }

    @Test
    public void translate_exists_typedVars() {
        // (exists (?X) (foo ?X)) → ( ? [V__X:$i] : (...) )
        Expr expr = se("exists",
                varList(var("?X")),
                se("foo", var("?X")));
        String result = ExprToTHF.translate(expr, false, emptyMap());
        assertTrue("should have ? [", result.contains("? ["));
        assertTrue("should have V__X:$i", result.contains("V__X:$i"));
    }

    // -----------------------------------------------------------------------
    // Free variable wrapping
    // -----------------------------------------------------------------------

    @Test
    public void translate_freeVars_universalWrap() {
        // (foo ?X ?Y) with no enclosing quantifier → wrapped with ! [V__X:$i,V__Y:$i]
        Expr expr = se("foo", var("?X"), var("?Y"));
        String result = ExprToTHF.translate(expr, false, emptyMap());
        assertTrue("free vars wrapped with ! [: " + result, result.contains("! ["));
        assertTrue("should contain V__X", result.contains("V__X"));
        assertTrue("should contain V__Y", result.contains("V__Y"));
        // Every free var should have ":$i" type annotation
        assertTrue("V__X:$i in: " + result, result.contains("V__X:$i"));
    }

    @Test
    public void translate_freeVar_worldTyped() {
        // (foo ?X ?W1): ?W1 matches pattern → type "w"
        Expr expr = se("foo", var("?X"), var("?W1"));
        String result = ExprToTHF.translate(expr, false, emptyMap());
        assertTrue("V__W1:w in: " + result, result.contains("V__W1:w"));
    }

    @Test
    public void translate_queryMode_existential() {
        // In query mode, free variables get ? quantifier
        Expr expr = se("foo", var("?X"));
        String result = ExprToTHF.translate(expr, true, emptyMap());
        assertTrue("query uses ? [: " + result, result.contains("? ["));
        assertFalse("query must NOT use ! [", result.contains("! ["));
    }

    @Test
    public void translate_noFreeVars_noWrapper() {
        // (forall (?X) body) — all vars bound, no outer wrapper
        Expr expr = se("forall",
                varList(var("?X")),
                se("foo", var("?X")));
        String result = ExprToTHF.translate(expr, false, emptyMap());
        // The outer "!" should appear exactly once (inside the quantifier)
        long bangCount = result.chars().filter(c -> c == '!').count();
        assertEquals("exactly one ! quantifier: " + result, 1, bangCount);
    }

    // -----------------------------------------------------------------------
    // Non-modal mode
    // -----------------------------------------------------------------------

    @Test
    public void translateNonModal_noWorldTypes() {
        // In non-modal mode, even ?W1 gets "$i" if typeMap doesn't say "World"
        // (the ?W\d+ regex is only active in modal mode)
        Map<String, Set<String>> tm = new HashMap<>();
        Expr expr = se("foo", var("?W1"), var("?X"));
        String result = ExprToTHF.translateNonModal(expr, false, tm);
        // Both vars should be $i since we're in non-modal mode
        assertFalse("non-modal must not use world type 'w': " + result,
                result.contains(":w"));
        assertTrue("non-modal uses @: " + result, result.contains("@"));
    }

    @Test
    public void translateNonModal_formulaTypeIsBooleanO() {
        Map<String, Set<String>> tm = typeMap("?F", "Formula");
        Expr expr = se("foo", var("?F"));
        String result = ExprToTHF.translateNonModal(expr, false, tm);
        // free var ?F should be typed as $o
        assertTrue("formula-typed var → $o: " + result, result.contains("V__F:$o"));
    }

    // -----------------------------------------------------------------------
    // buildTypedVarList() helper
    // -----------------------------------------------------------------------

    @Test
    public void buildTypedVarList_mixed() {
        Map<String, Set<String>> tm = new HashMap<>();
        tm.put("?X", new HashSet<>());  // unknown → $i
        Set<String> wt = new HashSet<>();
        wt.add("World");
        tm.put("?W1", wt);

        String result = ExprToTHF.buildTypedVarList(
                List.of("?X", "?W1"), tm, true);
        assertTrue("X gets $i: " + result, result.contains("V__X:$i"));
        // ?W1 matches ?W\d+ pattern → "w"
        assertTrue("W1 gets w: " + result, result.contains("V__W1:w"));
    }

    // -----------------------------------------------------------------------
    // Xor
    // -----------------------------------------------------------------------

    @Test
    public void translate_xor() {
        Expr expr = se("xor",
                se("foo", var("?X")),
                se("bar", var("?X")));
        String result = ExprToTHF.translate(expr, false, emptyMap());
        assertTrue("should contain <~>: " + result, result.contains("<~>"));
    }

    // -----------------------------------------------------------------------
    // accreln2 / accreln3 — modal operator arg must use head-position (no __m)
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Higher-order application — variable in head position
    // -----------------------------------------------------------------------

    @Test
    public void translate_varHead_higherOrderApplication() {
        // (?REL ?INST1 ?INST2) — predicate variable in head position
        // Must produce (V__REL @ V__INST1 @ V__INST2), NOT "V__INST1,V__INST2"
        Expr expr = new Expr.SExpr(var("?REL"), List.of(var("?INST1"), var("?INST2")));
        String result = ExprToTHF.translateExpr(expr, false, emptyMap(), true);
        assertTrue("var-head should produce @ application: " + result, result.contains("@"));
        assertTrue("should contain V__REL: " + result, result.contains("V__REL"));
        assertTrue("should contain V__INST1: " + result, result.contains("V__INST1"));
        assertTrue("should contain V__INST2: " + result, result.contains("V__INST2"));
        assertFalse("must NOT use comma notation: " + result, result.contains("V__INST1,V__INST2"));
    }

    @Test
    public void translate_accreln2_modalOpArgNoMentionSuffix() {
        // (accreln2 holdsDuring agent CW W1)
        // First arg "holdsDuring" is a lowercase modal operator typed as m.
        // It must translate as "s__holdsDuring" (no __m), not "s__holdsDuring__m".
        Expr expr = se("accreln2",
                atom("holdsDuring"),
                var("?A"),
                atom("CW"),
                var("?W1"));
        String result = ExprToTHF.translateExpr(expr, false, emptyMap(), true);
        assertTrue("modal op arg must be s__holdsDuring (no __m): " + result,
                result.contains("s__holdsDuring"));
        assertFalse("modal op arg must NOT be s__holdsDuring__m: " + result,
                result.contains("s__holdsDuring__m"));
    }

    @Test
    public void translate_accreln3_modalOpArgNoMentionSuffix() {
        // (accreln3 confersNorm a1 a2 CW W1)
        Expr expr = se("accreln3",
                atom("confersNorm"),
                var("?A1"),
                var("?A2"),
                atom("CW"),
                var("?W1"));
        String result = ExprToTHF.translateExpr(expr, false, emptyMap(), true);
        assertFalse("modal op arg must NOT be s__confersNorm__m: " + result,
                result.contains("s__confersNorm__m"));
        assertTrue("modal op arg must be s__confersNorm: " + result,
                result.contains("s__confersNorm"));
    }
}
