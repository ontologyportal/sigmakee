package com.articulate.sigma.parsing;

import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ExprToTPTP}.
 *
 * <p>All tests run without a live KB (KB.isRelationInAnyKB is guarded with try-catch
 * in the production code), so atom mention-suffix decisions are made purely from
 * syntactic heuristics (lowercase first char, Fn suffix).</p>
 */
public class ExprToTPTPTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Expr.Atom  atom(String s)              { return new Expr.Atom(s); }
    private static Expr.Var   var(String s)               { return new Expr.Var(s); }
    private static Expr.RowVar rowvar(String s)           { return new Expr.RowVar(s); }
    private static Expr.NumLiteral num(String v)          { return new Expr.NumLiteral(v); }
    private static Expr.StrLiteral str(String v)          { return new Expr.StrLiteral(v); }

    private static Expr.SExpr se(String head, Expr... args) {
        return new Expr.SExpr(new Expr.Atom(head), List.of(args));
    }
    /** Null-head SExpr: used for quantifier var lists. */
    private static Expr.SExpr varList(Expr... vars) {
        return new Expr.SExpr(null, List.of(vars));
    }

    // -----------------------------------------------------------------------
    // Variable / RowVar translation
    // -----------------------------------------------------------------------

    @Test
    public void var_simpleQuestion() {
        assertEquals("V__X", ExprToTPTP.translateVarName("?X"));
    }

    @Test
    public void var_rowVar() {
        assertEquals("V__ROW", ExprToTPTP.translateVarName("@ROW"));
    }

    @Test
    public void var_dashInName() {
        assertEquals("V__MY_VAR", ExprToTPTP.translateVarName("?MY-VAR"));
    }

    @Test
    public void translateExpr_var() {
        assertEquals("V__X", ExprToTPTP.translateExpr(var("?X"), false, "fof"));
    }

    @Test
    public void translateExpr_rowVar() {
        assertEquals("V__ROW", ExprToTPTP.translateExpr(rowvar("@ROW"), false, "fof"));
    }

    // -----------------------------------------------------------------------
    // Number translation
    // -----------------------------------------------------------------------

    @Test
    public void number_fof_hideNumbers() {
        // In FOF mode with hideNumbers=true (default), numbers get n__ prefix
        assertEquals("n__42", ExprToTPTP.translateNumber("42", "fof"));
    }

    @Test
    public void number_fof_negative() {
        assertEquals("n___3_14", ExprToTPTP.translateNumber("-3.14", "fof"));
    }

    @Test
    public void number_tff_passthrough() {
        // TFF passes numbers through unchanged
        assertEquals("42", ExprToTPTP.translateNumber("42", "tff"));
        assertEquals("-3.14", ExprToTPTP.translateNumber("-3.14", "tff"));
    }

    @Test
    public void translateExpr_numLiteral_fof() {
        assertEquals("n__5", ExprToTPTP.translateExpr(num("5"), false, "fof"));
    }

    @Test
    public void translateExpr_numLiteral_tff() {
        assertEquals("5", ExprToTPTP.translateExpr(num("5"), false, "tff"));
    }

    // -----------------------------------------------------------------------
    // String literal translation
    // -----------------------------------------------------------------------

    @Test
    public void strLiteral_passthrough() {
        assertEquals("hello world", ExprToTPTP.translateExpr(str("hello world"), false, "fof"));
    }

    @Test
    public void strLiteral_stripsNewlines() {
        assertEquals("a b", ExprToTPTP.translateExpr(str("a\nb"), false, "fof"));
    }

    @Test
    public void strLiteral_stripsSingleQuotes() {
        // Apostrophes are removed (replaced with empty string, not a space)
        assertEquals("dont", ExprToTPTP.translateExpr(str("don't"), false, "fof"));
    }

    // -----------------------------------------------------------------------
    // Atom translation — head vs. argument position
    // -----------------------------------------------------------------------

    @Test
    public void atom_headPosition_upperCase() {
        // Upper-case symbols in head position: just s__ prefix, no __m
        assertEquals("s__Human", ExprToTPTP.translateAtom("Human", true, "fof"));
    }

    @Test
    public void atom_argPosition_upperCase_noMention() {
        // Upper-case, non-relation, non-Fn: no __m in unit-test context
        // (KB.isRelationInAnyKB returns false when KB unavailable)
        assertEquals("s__Human", ExprToTPTP.translateAtom("Human", false, "fof"));
    }

    @Test
    public void atom_argPosition_lowerCase_addsMention() {
        // Lowercase first char → __m suffix in argument position
        assertEquals("s__instance__m", ExprToTPTP.translateAtom("instance", false, "fof"));
    }

    @Test
    public void atom_headPosition_lowerCase_noMention() {
        // In head position, no __m even for lowercase
        assertEquals("s__instance", ExprToTPTP.translateAtom("instance", true, "fof"));
    }

    @Test
    public void atom_argPosition_fnSuffix_addsMention() {
        // Fn-suffixed term in argument position → __m
        assertEquals("s__AdditionFn__m", ExprToTPTP.translateAtom("AdditionFn", false, "fof"));
    }

    @Test
    public void atom_headPosition_dotReplaced() {
        // Dots replaced with underscores
        assertEquals("s__foo_bar", ExprToTPTP.translateAtom("foo.bar", true, "fof"));
    }

    @Test
    public void atom_headPosition_dashReplaced() {
        assertEquals("s__foo_bar", ExprToTPTP.translateAtom("foo-bar", true, "fof"));
    }

    // -----------------------------------------------------------------------
    // Logical connectives
    // -----------------------------------------------------------------------

    @Test
    public void not_simple() {
        // "instance" is lowercase → gets __m in arg position; "Human"/"Socrates" are
        // uppercase without KB available → no __m (KB.isRelationInAnyKB returns false)
        Expr expr = se("not", se("instance", atom("Socrates"), atom("Human")));
        String result = ExprToTPTP.translateExpr(expr, false, "fof");
        assertEquals("~(s__instance(s__Socrates,s__Human))", result);
    }

    @Test
    public void and_twoArgs() {
        Expr a = se("p", var("?X"));
        Expr b = se("q", var("?X"));
        Expr expr = se("and", a, b);
        String result = ExprToTPTP.translateExpr(expr, false, "fof");
        assertEquals("(s__p(V__X) & s__q(V__X))", result);
    }

    @Test
    public void and_threeArgs() {
        Expr a = se("p", var("?X"));
        Expr b = se("q", var("?X"));
        Expr c = se("r", var("?X"));
        Expr expr = se("and", a, b, c);
        String result = ExprToTPTP.translateExpr(expr, false, "fof");
        assertEquals("(s__p(V__X) & s__q(V__X) & s__r(V__X))", result);
    }

    @Test
    public void or_twoArgs() {
        Expr a = se("p", var("?X"));
        Expr b = se("q", var("?X"));
        Expr expr = se("or", a, b);
        String result = ExprToTPTP.translateExpr(expr, false, "fof");
        assertEquals("(s__p(V__X) | s__q(V__X))", result);
    }

    @Test
    public void xor_twoArgs() {
        Expr a = se("p", var("?X"));
        Expr b = se("q", var("?X"));
        Expr expr = se("xor", a, b);
        String result = ExprToTPTP.translateExpr(expr, false, "fof");
        assertEquals("(s__p(V__X) <~> s__q(V__X))", result);
    }

    @Test
    public void implication() {
        Expr a = se("p", var("?X"));
        Expr b = se("q", var("?X"));
        Expr expr = se("=>", a, b);
        String result = ExprToTPTP.translateExpr(expr, false, "fof");
        // Non-EProver: (A => B)
        assertEquals("(s__p(V__X) => s__q(V__X))", result);
    }

    @Test
    public void biconditional_expandsToTwoImplications() {
        Expr a = se("p", var("?X"));
        Expr b = se("q", var("?X"));
        Expr expr = se("<=>", a, b);
        String result = ExprToTPTP.translateExpr(expr, false, "fof");
        assertEquals("((s__p(V__X) => s__q(V__X)) & (s__q(V__X) => s__p(V__X)))", result);
    }

    @Test
    public void equal_twoArgs() {
        Expr expr = se("equal", var("?X"), var("?Y"));
        String result = ExprToTPTP.translateExpr(expr, false, "fof");
        assertEquals("(V__X = V__Y)", result);
    }

    // -----------------------------------------------------------------------
    // Quantifiers
    // -----------------------------------------------------------------------

    @Test
    public void forall_singleVar() {
        Expr body = se("instance", var("?X"), atom("Human"));
        Expr expr = se("forall", varList(var("?X")), body);
        String result = ExprToTPTP.translateExpr(expr, false, "fof");
        // "instance" lowercase → __m; "Human" uppercase, no KB → no __m
        assertEquals("(! [V__X] : (s__instance(V__X,s__Human)))", result);
    }

    @Test
    public void forall_twoVars() {
        Expr body = se("p", var("?X"), var("?Y"));
        Expr expr = se("forall", varList(var("?X"), var("?Y")), body);
        String result = ExprToTPTP.translateExpr(expr, false, "fof");
        assertEquals("(! [V__X,V__Y] : (s__p(V__X,V__Y)))", result);
    }

    @Test
    public void exists_singleVar() {
        Expr body = se("instance", var("?X"), atom("Human"));
        Expr expr = se("exists", varList(var("?X")), body);
        String result = ExprToTPTP.translateExpr(expr, false, "fof");
        assertEquals("(? [V__X] : (s__instance(V__X,s__Human)))", result);
    }

    @Test
    public void forall_rowVar() {
        Expr body = se("p", rowvar("@ROW"));
        Expr expr = se("forall", varList(rowvar("@ROW")), body);
        String result = ExprToTPTP.translateExpr(expr, false, "fof");
        assertEquals("(! [V__ROW] : (s__p(V__ROW)))", result);
    }

    // -----------------------------------------------------------------------
    // Function application
    // -----------------------------------------------------------------------

    @Test
    public void application_noArgs() {
        Expr expr = new Expr.SExpr(atom("Foo"), List.of());
        String result = ExprToTPTP.translateExpr(expr, false, "fof");
        assertEquals("s__Foo()", result);
    }

    @Test
    public void application_unary() {
        Expr expr = se("instance", atom("Socrates"), atom("Human"));
        String result = ExprToTPTP.translateExpr(expr, false, "fof");
        // "instance" is head → s__instance; uppercase args without KB → no __m
        assertEquals("s__instance(s__Socrates,s__Human)", result);
    }

    @Test
    public void application_nestedFnArg() {
        // (age Socrates (MeasureFn 30 Year))
        Expr measureFn = se("MeasureFn", num("30"), atom("Year"));
        Expr expr = se("age", atom("Socrates"), measureFn);
        String result = ExprToTPTP.translateExpr(expr, false, "fof");
        // MeasureFn as head: s__MeasureFn (no __m in head position)
        // 30 → n__30 in FOF
        // Year → uppercase, no KB → s__Year (no __m ... but wait Year starts with uppercase and not Fn suffix)
        // Socrates → s__Socrates (no mention without KB)
        assertEquals("s__age(s__Socrates,s__MeasureFn(n__30,s__Year))", result);
    }

    // -----------------------------------------------------------------------
    // Free variable wrapping in translate()
    // -----------------------------------------------------------------------

    @Test
    public void translate_noFreeVars_unchanged() {
        // Fully quantified formula — no wrapping
        Expr body = se("instance", var("?X"), atom("Human"));
        Expr expr = se("forall", varList(var("?X")), body);
        String result = ExprToTPTP.translate(expr, false, "fof");
        assertEquals("(! [V__X] : (s__instance(V__X,s__Human)))", result);
    }

    @Test
    public void translate_freeVar_wrapsInForall() {
        // ?X is free → wraps in ! [V__X] : (...)
        Expr expr = se("instance", var("?X"), atom("Human"));
        String result = ExprToTPTP.translate(expr, false, "fof");
        assertEquals("( ! [V__X] : (s__instance(V__X,s__Human) ) )", result);
    }

    @Test
    public void translate_freeVar_queryMode_wrapsInExists() {
        Expr expr = se("instance", var("?X"), atom("Human"));
        String result = ExprToTPTP.translate(expr, true, "fof");
        assertEquals("( ? [V__X] : (s__instance(V__X,s__Human) ) )", result);
    }

    @Test
    public void translate_multipleFreeVars() {
        Expr expr = se("p", var("?X"), var("?Y"));
        String result = ExprToTPTP.translate(expr, false, "fof");
        // Both ?X and ?Y are free — result must contain both in the var list
        assertTrue(result.startsWith("( ! ["));
        assertTrue(result.contains("V__X"));
        assertTrue(result.contains("V__Y"));
        assertTrue(result.contains("s__p(V__X,V__Y)"));
    }

    // -----------------------------------------------------------------------
    // collectFreeVars
    // -----------------------------------------------------------------------

    @Test
    public void collectFreeVars_atom() {
        // Atoms have no variables
        assertTrue(ExprToTPTP.collectFreeVars(atom("Human")).isEmpty());
    }

    @Test
    public void collectFreeVars_var() {
        Set<String> free = ExprToTPTP.collectFreeVars(var("?X"));
        assertEquals(Set.of("?X"), free);
    }

    @Test
    public void collectFreeVars_boundByForall() {
        Expr body = se("p", var("?X"));
        Expr expr = se("forall", varList(var("?X")), body);
        assertTrue(ExprToTPTP.collectFreeVars(expr).isEmpty());
    }

    @Test
    public void collectFreeVars_partiallyBound() {
        // (forall (?X) (p ?X ?Y)) — ?Y is free
        Expr body = se("p", var("?X"), var("?Y"));
        Expr expr = se("forall", varList(var("?X")), body);
        Set<String> free = ExprToTPTP.collectFreeVars(expr);
        assertEquals(Set.of("?Y"), free);
    }

    @Test
    public void collectFreeVars_nestedQuantifiers() {
        // (forall (?X) (exists (?Y) (p ?X ?Y))) — no free vars
        Expr body = se("p", var("?X"), var("?Y"));
        Expr inner = se("exists", varList(var("?Y")), body);
        Expr expr  = se("forall", varList(var("?X")), inner);
        assertTrue(ExprToTPTP.collectFreeVars(expr).isEmpty());
    }

    // -----------------------------------------------------------------------
    // TFF-specific arithmetic functions in head position
    // -----------------------------------------------------------------------

    @Test
    public void tff_additionFn_head() {
        Expr expr = se("AdditionFn", num("1"), num("2"));
        String result = ExprToTPTP.translateExpr(expr, false, "tff");
        assertEquals("sum(1,2)", result);
    }

    @Test
    public void tff_subtractionFn_head() {
        Expr expr = se("SubtractionFn", num("5"), num("3"));
        String result = ExprToTPTP.translateExpr(expr, false, "tff");
        assertEquals("difference(5,3)", result);
    }

    @Test
    public void tff_multiplicationFn_head() {
        Expr expr = se("MultiplicationFn", num("2"), num("3"));
        String result = ExprToTPTP.translateExpr(expr, false, "tff");
        assertEquals("product(2,3)", result);
    }

    @Test
    public void tff_divisionFn_head() {
        Expr expr = se("DivisionFn", num("6"), num("2"));
        String result = ExprToTPTP.translateExpr(expr, false, "tff");
        assertEquals("quotient(6,2)", result);
    }

    @Test
    public void tff_arithFn_fofMode_notReplaced() {
        // In FOF mode, AdditionFn stays as s__AdditionFn (not converted to sum)
        Expr expr = se("AdditionFn", num("1"), num("2"));
        String result = ExprToTPTP.translateExpr(expr, false, "fof");
        assertEquals("s__AdditionFn(n__1,n__2)", result);
    }

    // -----------------------------------------------------------------------
    // Nested formula (regression)
    // -----------------------------------------------------------------------

    @Test
    public void nestedImplication_forall_instance_subclass() {
        // (forall (?X) (=> (instance ?X Human) (instance ?X Animal)))
        Expr ant = se("instance", var("?X"), atom("Human"));
        Expr con = se("instance", var("?X"), atom("Animal"));
        Expr body = se("=>", ant, con);
        Expr expr = se("forall", varList(var("?X")), body);
        String result = ExprToTPTP.translate(expr, false, "fof");
        // "instance" lowercase → __m not added (it's the head here); uppercase constants no __m without KB
        assertEquals(
            "(! [V__X] : ((s__instance(V__X,s__Human) => s__instance(V__X,s__Animal))))",
            result
        );
    }
}
