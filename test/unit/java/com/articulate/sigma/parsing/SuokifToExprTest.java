package com.articulate.sigma.parsing;

import com.articulate.sigma.UnitTestBase;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for SuokifToExpr: verifies that KIF strings parsed via SuokifVisitor
 * produce correctly structured Expr trees, and that toKifString() round-trips
 * back to the original string.
 */
public class SuokifToExprTest extends UnitTestBase {

    private static Expr parse(String kif) {
        SuokifVisitor visitor = SuokifVisitor.parseString(kif);
        assertFalse("Parse should succeed for: " + kif, visitor.result.isEmpty());
        FormulaAST f = visitor.result.values().iterator().next();
        assertNotNull("expr field must be populated", f.expr);
        return f.expr;
    }

    // -----------------------------------------------------------------------
    // Round-trip: toKifString() must reproduce the original string
    // -----------------------------------------------------------------------

    @Test
    public void roundTripSimple() {
        String kif = "(likes John Mary)";
        assertEquals(kif, parse(kif).toKifString());
    }

    @Test
    public void roundTripImplication() {
        String kif = "(=> (instance ?X Animal) (instance ?X Entity))";
        assertEquals(kif, parse(kif).toKifString());
    }

    @Test
    public void roundTripForall() {
        String kif = "(forall (?X) (instance ?X Animal))";
        assertEquals(kif, parse(kif).toKifString());
    }

    @Test
    public void roundTripExists() {
        String kif = "(exists (?X) (instance ?X Animal))";
        assertEquals(kif, parse(kif).toKifString());
    }

    @Test
    public void roundTripAnd() {
        String kif = "(and (instance ?X Animal) (instance ?X Entity))";
        assertEquals(kif, parse(kif).toKifString());
    }

    @Test
    public void roundTripNot() {
        String kif = "(not (instance ?X Animal))";
        assertEquals(kif, parse(kif).toKifString());
    }

    @Test
    public void roundTripEqual() {
        String kif = "(equal ?X ?Y)";
        assertEquals(kif, parse(kif).toKifString());
    }

    @Test
    public void roundTripFunterm() {
        String kif = "(instance (HourFn 12 ?DAY) TimeInterval)";
        assertEquals(kif, parse(kif).toKifString());
    }

    @Test
    public void roundTripRowVar() {
        String kif = "(=> (and (minValue ?R ?ARG ?N) (?R @ARGS) (equal ?VAL (ListOrderFn (ListFn @ARGS) ?ARG))) (greaterThan ?VAL ?N))";
        assertEquals(kif, parse(kif).toKifString());
    }

    // -----------------------------------------------------------------------
    // Structural tests: verify the Expr tree shape
    // -----------------------------------------------------------------------

    @Test
    public void structureSimpleRelsent() {
        // (likes John Mary)  →  SExpr(Atom("likes"), [Atom("John"), Atom("Mary")])
        Expr e = parse("(likes John Mary)");
        assertTrue(e instanceof Expr.SExpr);
        Expr.SExpr se = (Expr.SExpr) e;
        assertEquals("likes", se.headName());
        assertEquals(2, se.args().size());
        assertEquals(new Expr.Atom("John"), se.args().get(0));
        assertEquals(new Expr.Atom("Mary"), se.args().get(1));
    }

    @Test
    public void structureImplication() {
        // (=> A B)  →  SExpr(Atom("=>"), [A, B])
        Expr e = parse("(=> (instance ?X Animal) (instance ?X Entity))");
        Expr.SExpr se = (Expr.SExpr) e;
        assertEquals("=>", se.headName());
        assertEquals(2, se.args().size());
        assertTrue(se.args().get(0) instanceof Expr.SExpr);
        assertTrue(se.args().get(1) instanceof Expr.SExpr);
    }

    @Test
    public void structureForallVarList() {
        // (forall (?X ?Y) body)  →  SExpr(Atom("forall"), [SExpr(null,[?X,?Y]), body])
        Expr e = parse("(forall (?X ?Y) (instance ?X Animal))");
        Expr.SExpr forall = (Expr.SExpr) e;
        assertEquals("forall", forall.headName());
        assertEquals(2, forall.args().size());

        // Variable list: SExpr with null head
        Expr.SExpr varList = (Expr.SExpr) forall.args().get(0);
        assertNull(varList.head());
        assertEquals(2, varList.args().size());
        assertEquals(new Expr.Var("?X"), varList.args().get(0));
        assertEquals(new Expr.Var("?Y"), varList.args().get(1));

        // Body
        assertTrue(forall.args().get(1) instanceof Expr.SExpr);
    }

    @Test
    public void structureExistsVarList() {
        Expr e = parse("(exists (?SYLLABLE2) (instance ?SYLLABLE2 Syllable))");
        Expr.SExpr exists = (Expr.SExpr) e;
        assertEquals("exists", exists.headName());
        Expr.SExpr varList = (Expr.SExpr) exists.args().get(0);
        assertNull(varList.head());
        assertEquals(1, varList.args().size());
        assertEquals(new Expr.Var("?SYLLABLE2"), varList.args().get(0));
    }

    @Test
    public void structureRowVar() {
        Expr e = parse("(instance ?R @ARGS)");
        Expr.SExpr se = (Expr.SExpr) e;
        assertEquals("instance", se.headName());
        assertTrue(se.args().get(1) instanceof Expr.RowVar);
        assertEquals("@ARGS", ((Expr.RowVar) se.args().get(1)).name());
    }

    @Test
    public void structureVariable() {
        Expr e = parse("(instance ?X Animal)");
        Expr.SExpr se = (Expr.SExpr) e;
        assertTrue(se.args().get(0) instanceof Expr.Var);
        assertEquals("?X", ((Expr.Var) se.args().get(0)).name());
        assertTrue(se.args().get(1) instanceof Expr.Atom);
    }

    @Test
    public void structureNumber() {
        Expr e = parse("(instance (HourFn 12 ?DAY) TimeInterval)");
        Expr.SExpr outer = (Expr.SExpr) e;
        Expr.SExpr funterm = (Expr.SExpr) outer.args().get(0);
        assertEquals("HourFn", funterm.headName());
        assertTrue(funterm.args().get(0) instanceof Expr.NumLiteral);
        assertEquals("12", ((Expr.NumLiteral) funterm.args().get(0)).value());
    }

    @Test
    public void structureIff() {
        Expr e = parse("(<=> (instance ?X Animal) (instance ?X Entity))");
        Expr.SExpr se = (Expr.SExpr) e;
        assertEquals("<=>", se.headName());
        assertEquals(2, se.args().size());
    }

    @Test
    public void structureNot() {
        Expr e = parse("(not (instance ?X Animal))");
        Expr.SExpr se = (Expr.SExpr) e;
        assertEquals("not", se.headName());
        assertEquals(1, se.args().size());
        assertTrue(se.args().get(0) instanceof Expr.SExpr);
    }

    // -----------------------------------------------------------------------
    // Multiple formulas in one parse call
    // -----------------------------------------------------------------------

    @Test
    public void multipleFormulas() {
        SuokifVisitor visitor = SuokifVisitor.parseString("(likes John Mary)\n(part Wheel1 Car2)\n");
        assertEquals(2, visitor.result.size());
        for (FormulaAST f : visitor.result.values()) {
            assertNotNull("Every formula must have an expr", f.expr);
        }
    }
}
