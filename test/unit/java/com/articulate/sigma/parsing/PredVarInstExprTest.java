package com.articulate.sigma.parsing;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link PredVarInst#substituteVar(Expr, String, String)}.
 * These tests are pure tree-transformation tests — no KB or test base required.
 */
public class PredVarInstExprTest {

    // -----------------------------------------------------------------------
    // Helper: build Expr nodes concisely
    // -----------------------------------------------------------------------

    private static Expr.Atom atom(String name)   { return new Expr.Atom(name); }
    private static Expr.Var  var(String name)    { return new Expr.Var(name); }
    private static Expr.SExpr sexpr(Expr head, Expr... args) {
        return new Expr.SExpr(head, List.of(args));
    }

    // -----------------------------------------------------------------------
    // substituteVar — direct atom arg
    // -----------------------------------------------------------------------

    @Test
    public void substituteAtomArgVar() {
        // (foo ?REL bar) with ?REL→baz  →  (foo baz bar)
        Expr input  = sexpr(atom("foo"), var("?REL"), atom("bar"));
        Expr result = PredVarInst.substituteVar(input, "?REL", "baz");
        assertEquals("(foo baz bar)", result.toKifString());
    }

    @Test
    public void substituteDoesNotMatchOtherVar() {
        // (foo ?RELATED ?REL) — only ?REL should be replaced, not ?RELATED
        Expr input  = sexpr(atom("foo"), var("?RELATED"), var("?REL"));
        Expr result = PredVarInst.substituteVar(input, "?REL", "baz");
        assertEquals("(foo ?RELATED baz)", result.toKifString());
    }

    @Test
    public void substituteNoMatchReturnsUnchanged() {
        // (foo ?X ?Y) with ?REL→baz — nothing to replace
        Expr input  = sexpr(atom("foo"), var("?X"), var("?Y"));
        Expr result = PredVarInst.substituteVar(input, "?REL", "baz");
        assertEquals("(foo ?X ?Y)", result.toKifString());
    }

    // -----------------------------------------------------------------------
    // substituteVar — head is a variable (predicate variable usage)
    // -----------------------------------------------------------------------

    @Test
    public void substituteVarHead() {
        // (?REL x y) with ?REL→likes  →  (likes x y)
        Expr input  = sexpr(var("?REL"), atom("x"), atom("y"));
        Expr result = PredVarInst.substituteVar(input, "?REL", "likes");
        assertEquals("(likes x y)", result.toKifString());
    }

    @Test
    public void substituteVarHeadAndArgSameVar() {
        // (?REL ?REL z) with ?REL→foo  →  (foo foo z)
        Expr input  = sexpr(var("?REL"), var("?REL"), atom("z"));
        Expr result = PredVarInst.substituteVar(input, "?REL", "foo");
        assertEquals("(foo foo z)", result.toKifString());
    }

    // -----------------------------------------------------------------------
    // substituteVar — nested SExpr
    // -----------------------------------------------------------------------

    @Test
    public void substituteNested() {
        // (=> (?REL a b) (?REL c d)) with ?REL→likes  →  (=> (likes a b) (likes c d))
        Expr input = sexpr(atom("=>"),
                sexpr(var("?REL"), atom("a"), atom("b")),
                sexpr(var("?REL"), atom("c"), atom("d")));
        Expr result = PredVarInst.substituteVar(input, "?REL", "likes");
        assertEquals("(=> (likes a b) (likes c d))", result.toKifString());
    }

    @Test
    public void substituteDeepNesting() {
        // (and (or (?REL x) (foo y)) (?REL z))
        Expr input = sexpr(atom("and"),
                sexpr(atom("or"),
                        sexpr(var("?REL"), atom("x")),
                        sexpr(atom("foo"), atom("y"))),
                sexpr(var("?REL"), atom("z")));
        Expr result = PredVarInst.substituteVar(input, "?REL", "bar");
        assertEquals("(and (or (bar x) (foo y)) (bar z))", result.toKifString());
    }

    // -----------------------------------------------------------------------
    // substituteVar — leaf nodes are unchanged
    // -----------------------------------------------------------------------

    @Test
    public void substituteDoesNotTouchAtom() {
        Expr input  = atom("hello");
        Expr result = PredVarInst.substituteVar(input, "?REL", "baz");
        assertSame("Atom should be returned as-is", input, result);
    }

    @Test
    public void substituteDoesNotTouchNonMatchingVar() {
        Expr input  = var("?OTHER");
        Expr result = PredVarInst.substituteVar(input, "?REL", "baz");
        assertSame("Non-matching Var should be returned as-is", input, result);
    }

    @Test
    public void substituteMatchingVarReturnsNewAtom() {
        Expr input  = var("?REL");
        Expr result = PredVarInst.substituteVar(input, "?REL", "baz");
        assertTrue("expected Atom", result instanceof Expr.Atom);
        assertEquals("baz", ((Expr.Atom) result).name());
    }

    // -----------------------------------------------------------------------
    // substituteVar — quantifier var-list (null head)
    // -----------------------------------------------------------------------

    @Test
    public void substituteInsideQuantifierBody() {
        // (forall (?X ?Y) (?REL ?X ?Y))  with ?REL→loves
        Expr varList = new Expr.SExpr(null, List.of(var("?X"), var("?Y")));
        Expr body    = sexpr(var("?REL"), var("?X"), var("?Y"));
        Expr input   = sexpr(atom("forall"), varList, body);
        Expr result  = PredVarInst.substituteVar(input, "?REL", "loves");
        assertEquals("(forall (?X ?Y) (loves ?X ?Y))", result.toKifString());
    }

    // -----------------------------------------------------------------------
    // round-trip: formula string via toKifString stays consistent
    // -----------------------------------------------------------------------

    @Test
    public void roundTripAfterSubstitution() {
        // (=> (instance ?REL TransitiveRelation) (=> (and (?REL ?A ?B) (?REL ?B ?C)) (?REL ?A ?C)))
        Expr input = sexpr(atom("=>"),
                sexpr(atom("instance"), var("?REL"), atom("TransitiveRelation")),
                sexpr(atom("=>"),
                        sexpr(atom("and"),
                                sexpr(var("?REL"), var("?A"), var("?B")),
                                sexpr(var("?REL"), var("?B"), var("?C"))),
                        sexpr(var("?REL"), var("?A"), var("?C"))));

        Expr result = PredVarInst.substituteVar(input, "?REL", "subclass");
        String expected = "(=> (instance subclass TransitiveRelation) "
                + "(=> (and (subclass ?A ?B) (subclass ?B ?C)) (subclass ?A ?C)))";
        assertEquals(expected, result.toKifString());
    }
}
