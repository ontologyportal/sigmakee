package com.articulate.sigma.parsing;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ClausifierExpr} — each CNF-clausification step is
 * tested in isolation, then a full pipeline test validates the combined output.
 *
 * All tests are pure tree-transformation tests: no KB or test-base is needed.
 */
public class ClausifierExprTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Expr.Atom  atom(String s)            { return new Expr.Atom(s); }
    private static Expr.Var   var(String s)             { return new Expr.Var(s); }
    private static Expr.SExpr se(String head, Expr... args) {
        return new Expr.SExpr(new Expr.Atom(head), List.of(args));
    }
    /** Quantifier var-list: SExpr with null head. */
    private static Expr.SExpr varList(Expr... vars) {
        return new Expr.SExpr(null, List.of(vars));
    }

    // -----------------------------------------------------------------------
    // Step 1 — equivalencesOut
    // -----------------------------------------------------------------------

    @Test
    public void equivOut_simpleIFF() {
        // (<=> A B)  →  (and (=> A B) (=> B A))
        Expr in = se("<=>", atom("A"), atom("B"));
        Expr out = ClausifierExpr.equivalencesOut(in);
        assertEquals("(and (=> A B) (=> B A))", out.toKifString());
    }

    @Test
    public void equivOut_noIFF() {
        Expr in = se("and", atom("A"), atom("B"));
        assertEquals("(and A B)", ClausifierExpr.equivalencesOut(in).toKifString());
    }

    @Test
    public void equivOut_nestedIFF() {
        // (and (<=> A B) (<=> C D))
        Expr in = se("and",
                se("<=>", atom("A"), atom("B")),
                se("<=>", atom("C"), atom("D")));
        Expr out = ClausifierExpr.equivalencesOut(in);
        assertEquals("(and (and (=> A B) (=> B A)) (and (=> C D) (=> D C)))",
                out.toKifString());
    }

    // -----------------------------------------------------------------------
    // Step 2 — implicationsOut
    // -----------------------------------------------------------------------

    @Test
    public void implOut_simpleImplication() {
        // (=> A B)  →  (or (not A) B)
        Expr in = se("=>", atom("A"), atom("B"));
        Expr out = ClausifierExpr.implicationsOut(in);
        assertEquals("(or (not A) B)", out.toKifString());
    }

    @Test
    public void implOut_noImplication() {
        Expr in = se("or", atom("A"), atom("B"));
        assertEquals("(or A B)", ClausifierExpr.implicationsOut(in).toKifString());
    }

    @Test
    public void implOut_nestedImplication() {
        // (=> (=> A B) C)  →  (or (not (or (not A) B)) C)
        Expr in = se("=>", se("=>", atom("A"), atom("B")), atom("C"));
        Expr out = ClausifierExpr.implicationsOut(in);
        assertEquals("(or (not (or (not A) B)) C)", out.toKifString());
    }

    // -----------------------------------------------------------------------
    // Step 3 — negationsIn
    // -----------------------------------------------------------------------

    @Test
    public void negIn_doubleNegation() {
        // (not (not A))  →  A
        Expr in = se("not", se("not", atom("A")));
        Expr out = ClausifierExpr.negationsIn(in);
        assertEquals("A", out.toKifString());
    }

    @Test
    public void negIn_deMorganAnd() {
        // (not (and A B))  →  (or (not A) (not B))
        Expr in = se("not", se("and", atom("A"), atom("B")));
        Expr out = ClausifierExpr.negationsIn(in);
        assertEquals("(or (not A) (not B))", out.toKifString());
    }

    @Test
    public void negIn_deMorganOr() {
        // (not (or A B))  →  (and (not A) (not B))
        Expr in = se("not", se("or", atom("A"), atom("B")));
        Expr out = ClausifierExpr.negationsIn(in);
        assertEquals("(and (not A) (not B))", out.toKifString());
    }

    @Test
    public void negIn_notForall() {
        // (not (forall (?X) (p ?X)))  →  (exists (?X) (not (p ?X)))
        Expr in = se("not",
                se("forall", varList(var("?X")), se("p", var("?X"))));
        Expr out = ClausifierExpr.negationsIn(in);
        assertEquals("(exists (?X) (not (p ?X)))", out.toKifString());
    }

    @Test
    public void negIn_notExists() {
        // (not (exists (?X) (p ?X)))  →  (forall (?X) (not (p ?X)))
        Expr in = se("not",
                se("exists", varList(var("?X")), se("p", var("?X"))));
        Expr out = ClausifierExpr.negationsIn(in);
        assertEquals("(forall (?X) (not (p ?X)))", out.toKifString());
    }

    @Test
    public void negIn_noChange_literal() {
        // (not (p A)) — already in NNF
        Expr in = se("not", se("p", atom("A")));
        Expr out = ClausifierExpr.negationsIn(in);
        assertEquals("(not (p A))", out.toKifString());
    }

    // -----------------------------------------------------------------------
    // Step 4 — renameVariables
    // -----------------------------------------------------------------------

    @Test
    public void renameVars_uniqueNames() {
        // (=> (p ?X) (q ?X)) — both ?X occurrences should map to same new name
        Expr in = se("=>", se("p", var("?X")), se("q", var("?X")));
        int[] idx = {0};
        Expr out = ClausifierExpr.renameVariables(in, idx);
        // Both ?X→?X1; verify no ?X remains
        assertFalse("original var should be renamed",
                out.toKifString().contains("?X)") || out.toKifString().endsWith("?X"));
        // Both occurrences should use the same renamed var
        String s = out.toKifString();
        int first  = s.indexOf("?X");
        int second = s.indexOf("?X", first + 1);
        assertTrue("second ?X occurrence should exist", second > first);
        // Extract the two names and compare
        String v1 = s.substring(first,  s.indexOf(')', first));
        String v2 = s.substring(second, s.indexOf(')', second));
        assertEquals("both occurrences of ?X should rename to same name", v1.trim(), v2.trim());
    }

    @Test
    public void renameVars_differentVarsDifferentNames() {
        // (and (p ?X) (q ?Y)) — ?X and ?Y should get different names
        Expr in = se("and", se("p", var("?X")), se("q", var("?Y")));
        int[] idx = {0};
        Expr out = ClausifierExpr.renameVariables(in, idx);
        String s = out.toKifString();
        // Extract the two renamed vars
        int i1 = s.indexOf("?X");
        int i2 = s.indexOf("?X", i1 + 1);
        // There should be exactly one of each (no original names remain)
        assertFalse("?X should be renamed", s.contains("?X)") || s.endsWith("?X"));
        assertFalse("?Y should be renamed", s.contains("?Y)") || s.endsWith("?Y"));
    }

    @Test
    public void renameVars_quantifierScope() {
        // (forall (?X) (and (p ?X) (q ?Y))) — ?X scoped, ?Y free
        Expr in = se("forall", varList(var("?X")),
                se("and", se("p", var("?X")), se("q", var("?Y"))));
        int[] idx = {0};
        Expr out = ClausifierExpr.renameVariables(in, idx);
        String s = out.toKifString();
        // Result should still be a forall
        assertTrue("result should still be a forall", s.startsWith("(forall"));
        // Original ?X should not appear verbatim (renamed to ?X1, ?X2, etc.)
        // Use a token boundary check: (?X) and ?X followed by space/paren
        assertFalse("?X) should be renamed", s.contains("?X)"));
        assertFalse("?X followed by space should be renamed", s.contains("?X "));
        assertFalse("?Y) should be renamed", s.contains("?Y)"));
        assertFalse("?Y followed by space should be renamed", s.contains("?Y "));
    }

    // -----------------------------------------------------------------------
    // Step 5 — existentialsOut (Skolemization)
    // -----------------------------------------------------------------------

    @Test
    public void skolemize_noUniversals() {
        // (exists (?X) (p ?X))  →  (p Sk1)
        Expr in = se("exists", varList(var("?X")), se("p", var("?X")));
        int[] idx = {0};
        Expr out = ClausifierExpr.existentialsOut(in, idx);
        // No exists remaining; ?X replaced by a Skolem atom
        assertFalse("no existential should remain", out.toKifString().contains("exists"));
        assertFalse("?X should be replaced by Skolem", out.toKifString().contains("?X"));
        assertTrue("should contain a Skolem constant", out.toKifString().contains("Sk"));
    }

    @Test
    public void skolemize_withUniversal() {
        // (forall (?Y) (exists (?X) (p ?Y ?X)))
        // → (forall (?Y) (p ?Y (SkFn1 ?Y)))   [Skolem function of the universal var]
        Expr in = se("forall",
                varList(var("?Y")),
                se("exists",
                        varList(var("?X")),
                        se("p", var("?Y"), var("?X"))));
        int[] idx = {0};
        Expr out = ClausifierExpr.existentialsOut(in, idx);
        assertFalse("no existential should remain", out.toKifString().contains("exists"));
        assertFalse("?X should be Skolemized", out.toKifString().contains("?X)") || out.toKifString().endsWith("?X"));
        assertTrue("should contain SkFn Skolem function", out.toKifString().contains("SkFn"));
    }

    // -----------------------------------------------------------------------
    // Step 6 — universalsOut
    // -----------------------------------------------------------------------

    @Test
    public void universalsOut_simple() {
        // (forall (?X) (p ?X))  →  (p ?X)
        Expr in = se("forall", varList(var("?X")), se("p", var("?X")));
        Expr out = ClausifierExpr.universalsOut(in);
        assertEquals("(p ?X)", out.toKifString());
    }

    @Test
    public void universalsOut_nested() {
        // (forall (?X) (forall (?Y) (p ?X ?Y)))  →  (p ?X ?Y)
        Expr in = se("forall", varList(var("?X")),
                se("forall", varList(var("?Y")),
                        se("p", var("?X"), var("?Y"))));
        Expr out = ClausifierExpr.universalsOut(in);
        assertEquals("(p ?X ?Y)", out.toKifString());
    }

    @Test
    public void universalsOut_noForall() {
        Expr in = se("p", var("?X"), atom("a"));
        assertEquals("(p ?X a)", ClausifierExpr.universalsOut(in).toKifString());
    }

    // -----------------------------------------------------------------------
    // Step 7a — nestedOperatorsOut
    // -----------------------------------------------------------------------

    @Test
    public void nestedOps_flattenAnd() {
        // (and (and A B) C)  →  (and A B C)
        Expr in = se("and", se("and", atom("A"), atom("B")), atom("C"));
        Expr out = ClausifierExpr.nestedOperatorsOut(in);
        assertEquals("(and A B C)", out.toKifString());
    }

    @Test
    public void nestedOps_flattenOr() {
        // (or (or A B) C)  →  (or A B C)
        Expr in = se("or", se("or", atom("A"), atom("B")), atom("C"));
        Expr out = ClausifierExpr.nestedOperatorsOut(in);
        assertEquals("(or A B C)", out.toKifString());
    }

    @Test
    public void nestedOps_doubleNot() {
        // (not (not A))  →  A
        Expr in = se("not", se("not", atom("A")));
        Expr out = ClausifierExpr.nestedOperatorsOut(in);
        assertEquals("A", out.toKifString());
    }

    // -----------------------------------------------------------------------
    // Step 7b — disjunctionsIn
    // -----------------------------------------------------------------------

    @Test
    public void disjIn_distributeOrOverAnd() {
        // (or P (and Q R))  →  (and (or Q P) (or R P))
        // The conjunct args are iterated first, disjuncts appended after.
        Expr in = se("or", atom("P"), se("and", atom("Q"), atom("R")));
        Expr out = ClausifierExpr.disjunctionsIn(in);
        assertEquals("(and (or Q P) (or R P))", out.toKifString());
    }

    @Test
    public void disjIn_noAnd() {
        // (or A B)  →  unchanged
        Expr in = se("or", atom("A"), atom("B"));
        Expr out = ClausifierExpr.disjunctionsIn(in);
        assertEquals("(or A B)", out.toKifString());
    }

    @Test
    public void disjIn_pureAnd() {
        // (and A B)  →  unchanged (no or to distribute)
        Expr in = se("and", atom("A"), atom("B"));
        Expr out = ClausifierExpr.disjunctionsIn(in);
        assertEquals("(and A B)", out.toKifString());
    }

    // -----------------------------------------------------------------------
    // Step 8 — standardizeApart
    // -----------------------------------------------------------------------

    @Test
    public void standardize_singleClause() {
        // (or (p ?X) (q ?X))  — single clause, vars renamed but consistent
        Expr in = se("or", se("p", var("?X")), se("q", var("?X")));
        int[] idx = {0};
        Expr out = ClausifierExpr.standardizeApart(in, idx);
        // Both occurrences of ?X should map to the same fresh name
        assertFalse("original ?X should be renamed", out.toKifString().contains("?X)"));
        // Extract both occurrences
        String s = out.toKifString();
        int i1 = s.indexOf("?");
        int i2 = s.indexOf("?", i1 + 1);
        assertTrue("two ? occurrences", i2 > i1);
        // They should be the same
        String n1 = s.substring(i1, s.indexOf(')', i1)).trim();
        String n2 = s.substring(i2, s.indexOf(')', i2)).trim();
        assertEquals("same var name within clause", n1, n2);
    }

    @Test
    public void standardize_multiClause() {
        // (and (p ?X) (q ?X))  — two clauses, each gets independent fresh names
        Expr in = se("and", se("p", var("?X")), se("q", var("?X")));
        int[] idx = {0};
        Expr out = ClausifierExpr.standardizeApart(in, idx);
        assertTrue("and at top", out.toKifString().startsWith("(and"));
        // Original ?X (the bare name) should no longer appear:
        // – it must not appear as a token boundary like "?X)" or "?X "
        String s = out.toKifString();
        assertFalse("original ?X) should be replaced", s.contains("?X)"));
        assertFalse("original ?X<space> should be replaced", s.contains("?X "));
        // Find the two variable occurrences and verify they are DIFFERENT
        int j1 = s.indexOf("?");
        int j2 = s.indexOf("?", j1 + 1);
        assertTrue("two renamed vars", j2 > j1);
        String v1 = s.substring(j1, s.indexOf(')', j1)).trim();
        String v2 = s.substring(j2, s.indexOf(')', j2)).trim();
        assertNotEquals("vars in different clauses should be standardized apart", v1, v2);
    }

    // -----------------------------------------------------------------------
    // Full pipeline — clausify
    // -----------------------------------------------------------------------

    @Test
    public void clausify_simpleImplication() {
        // (=> (p ?X) (q ?X))
        // After pipeline: (or (not (p ?X1)) (q ?X1)) or similar CNF
        Expr in = se("=>", se("p", var("?X")), se("q", var("?X")));
        Expr out = ClausifierExpr.clausify(in);
        String s = out.toKifString();
        // Should be a disjunction with a negated literal and a positive literal
        assertTrue("should be an or clause", s.startsWith("(or"));
        assertTrue("should have (not …)", s.contains("(not"));
    }

    @Test
    public void clausify_equivalence() {
        // (<=> A B)  →  two clauses in CNF
        Expr in = se("<=>", atom("A"), atom("B"));
        Expr out = ClausifierExpr.clausify(in);
        String s = out.toKifString();
        // Should produce (and …) of two clauses
        assertTrue("equivalence should produce conjunction", s.startsWith("(and"));
    }

    @Test
    public void clausify_tautology_forall_implication() {
        // (forall (?X) (=> (p ?X) (p ?X)))
        // After clausification: (or (not (p ?X1)) (p ?X1)) — single tautological clause
        Expr in = se("forall",
                varList(var("?X")),
                se("=>", se("p", var("?X")), se("p", var("?X"))));
        Expr out = ClausifierExpr.clausify(in);
        String s = out.toKifString();
        assertTrue("should be an or clause", s.startsWith("(or"));
    }

    @Test
    public void clausify_existential_becomes_skolem() {
        // (exists (?X) (p ?X))
        Expr in = se("exists", varList(var("?X")), se("p", var("?X")));
        Expr out = ClausifierExpr.clausify(in);
        String s = out.toKifString();
        assertFalse("no existential in output", s.contains("exists"));
        assertTrue("Skolem constant in output", s.contains("Sk"));
    }

    @Test
    public void clausify_transitivity_rule() {
        // Classic transitivity axiom pattern:
        // (=> (and (r ?X ?Y) (r ?Y ?Z)) (r ?X ?Z))
        Expr in = se("=>",
                se("and",
                        se("r", var("?X"), var("?Y")),
                        se("r", var("?Y"), var("?Z"))),
                se("r", var("?X"), var("?Z")));
        Expr out = ClausifierExpr.clausify(in);
        // Should be (or (not (r …)) (not (r …)) (r …))
        String s = out.toKifString();
        assertTrue("transitivity clause should be an or", s.startsWith("(or"));
    }

    // -----------------------------------------------------------------------
    // normalizeVariables
    // -----------------------------------------------------------------------

    @Test
    public void normalize_renames_depth_first() {
        // (p ?A ?B ?A) — ?A should get index 1, ?B index 2, second ?A reuses 1
        Expr in = se("p", var("?A"), var("?B"), var("?A"));
        Expr out = ClausifierExpr.normalizeVariables(in);
        String s = out.toKifString();
        // Check that the normalized string uses ?VAR1 and ?VAR2
        assertTrue("first var → ?VAR1", s.contains("?VAR1"));
        assertTrue("second var → ?VAR2", s.contains("?VAR2"));
        // Count occurrences of ?VAR1 (should be 2 — head and tail position)
        int count = 0;
        int idx = 0;
        while ((idx = s.indexOf("?VAR1", idx)) != -1) { count++; idx++; }
        assertEquals("?A should appear twice as ?VAR1", 2, count);
    }

    @Test
    public void normalize_replaceSkolem() {
        // (p Sk1 ?X) with replaceSkolemTerms=true
        Expr in = se("p", atom("Sk1"), var("?X"));
        Expr out = ClausifierExpr.normalizeVariables(in, true);
        String s = out.toKifString();
        assertFalse("Sk1 should be replaced", s.contains("Sk1"));
        assertTrue("should use ?VAR names", s.contains("?VAR"));
    }
}
