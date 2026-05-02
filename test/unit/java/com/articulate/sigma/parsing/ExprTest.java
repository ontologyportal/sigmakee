package com.articulate.sigma.parsing;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for the Expr sealed hierarchy.
 * These tests exercise toKifString() and structural properties of each node type
 * without touching the parser.
 */
public class ExprTest {

    // -----------------------------------------------------------------------
    // Leaf node tests
    // -----------------------------------------------------------------------

    @Test
    public void atomToKif() {
        Expr.Atom a = new Expr.Atom("likes");
        assertEquals("likes", a.toKifString());
        assertEquals("likes", a.name());
    }

    @Test
    public void varToKif() {
        Expr.Var v = new Expr.Var("?X");
        assertEquals("?X", v.toKifString());
    }

    @Test
    public void rowVarToKif() {
        Expr.RowVar rv = new Expr.RowVar("@ARGS");
        assertEquals("@ARGS", rv.toKifString());
    }

    @Test
    public void numLiteralToKif() {
        Expr.NumLiteral n = new Expr.NumLiteral("42");
        assertEquals("42", n.toKifString());
        Expr.NumLiteral n2 = new Expr.NumLiteral("3.14");
        assertEquals("3.14", n2.toKifString());
    }

    @Test
    public void strLiteralToKif() {
        Expr.StrLiteral s = new Expr.StrLiteral("\"hello world\"");
        assertEquals("\"hello world\"", s.toKifString());
    }

    // -----------------------------------------------------------------------
    // SExpr (compound) tests
    // -----------------------------------------------------------------------

    @Test
    public void sexprSimpleRelsent() {
        // (likes John Mary)
        Expr e = new Expr.SExpr(
                new Expr.Atom("likes"),
                List.of(new Expr.Atom("John"), new Expr.Atom("Mary")));
        assertEquals("(likes John Mary)", e.toKifString());
    }

    @Test
    public void sexprHeadName() {
        Expr.SExpr e = new Expr.SExpr(new Expr.Atom("likes"),
                List.of(new Expr.Atom("John")));
        assertEquals("likes", e.headName());
    }

    @Test
    public void sexprHeadNameWhenNotAtom() {
        // head is a Var — headName() should return null
        Expr.SExpr e = new Expr.SExpr(new Expr.Var("?R"),
                List.of(new Expr.Atom("John")));
        assertNull(e.headName());
    }

    @Test
    public void sexprNullHeadVarList() {
        // (?X ?Y) — variable list inside a quantifier, head is null
        Expr varList = new Expr.SExpr(null,
                List.of(new Expr.Var("?X"), new Expr.Var("?Y")));
        assertEquals("(?X ?Y)", varList.toKifString());
    }

    @Test
    public void sexprImplication() {
        // (=> A B)
        Expr a = new Expr.Atom("A");
        Expr b = new Expr.Atom("B");
        Expr impl = new Expr.SExpr(new Expr.Atom("=>"), List.of(a, b));
        assertEquals("(=> A B)", impl.toKifString());
    }

    @Test
    public void sexprForall() {
        // (forall (?X) (instance ?X Animal))
        Expr varList = new Expr.SExpr(null, List.of(new Expr.Var("?X")));
        Expr body = new Expr.SExpr(new Expr.Atom("instance"),
                List.of(new Expr.Var("?X"), new Expr.Atom("Animal")));
        Expr forall = new Expr.SExpr(new Expr.Atom("forall"), List.of(varList, body));
        assertEquals("(forall (?X) (instance ?X Animal))", forall.toKifString());
    }

    @Test
    public void sexprNested() {
        // (=> (instance ?X Animal) (instance ?X Entity))
        Expr ant = new Expr.SExpr(new Expr.Atom("instance"),
                List.of(new Expr.Var("?X"), new Expr.Atom("Animal")));
        Expr con = new Expr.SExpr(new Expr.Atom("instance"),
                List.of(new Expr.Var("?X"), new Expr.Atom("Entity")));
        Expr impl = new Expr.SExpr(new Expr.Atom("=>"), List.of(ant, con));
        assertEquals("(=> (instance ?X Animal) (instance ?X Entity))", impl.toKifString());
    }

    @Test
    public void sexprWithRowVar() {
        // (ListFn @ARGS)
        Expr fn = new Expr.SExpr(new Expr.Atom("ListFn"),
                List.of(new Expr.RowVar("@ARGS")));
        assertEquals("(ListFn @ARGS)", fn.toKifString());
    }

    @Test
    public void sexprWithNumber() {
        // (HourFn 12 ?DAY)
        Expr e = new Expr.SExpr(new Expr.Atom("HourFn"),
                List.of(new Expr.NumLiteral("12"), new Expr.Var("?DAY")));
        assertEquals("(HourFn 12 ?DAY)", e.toKifString());
    }

    // -----------------------------------------------------------------------
    // Pattern matching tests (Java 21 sealed switch)
    // -----------------------------------------------------------------------

    @Test
    public void patternMatchAtom() {
        Expr e = new Expr.Atom("John");
        String result = switch (e) {
            case Expr.Atom a    -> "atom:" + a.name();
            case Expr.Var v     -> "var";
            case Expr.RowVar rv -> "rowvar";
            case Expr.NumLiteral n -> "num";
            case Expr.StrLiteral s -> "str";
            case Expr.SExpr se  -> "sexpr";
        };
        assertEquals("atom:John", result);
    }

    @Test
    public void patternMatchSExpr() {
        Expr e = new Expr.SExpr(new Expr.Atom("not"),
                List.of(new Expr.SExpr(new Expr.Atom("instance"),
                        List.of(new Expr.Var("?X"), new Expr.Atom("Animal")))));
        String result = switch (e) {
            case Expr.SExpr se when "not".equals(se.headName()) -> "negation";
            case Expr.SExpr se -> "other-sexpr";
            default -> "leaf";
        };
        assertEquals("negation", result);
    }

    // -----------------------------------------------------------------------
    // Record equality tests
    // -----------------------------------------------------------------------

    @Test
    public void recordEquality() {
        Expr.Atom a1 = new Expr.Atom("likes");
        Expr.Atom a2 = new Expr.Atom("likes");
        assertEquals(a1, a2);

        Expr.Var v1 = new Expr.Var("?X");
        Expr.Var v2 = new Expr.Var("?X");
        assertEquals(v1, v2);

        assertNotEquals(new Expr.Atom("a"), new Expr.Atom("b"));
    }

    @Test
    public void sexprEquality() {
        Expr e1 = new Expr.SExpr(new Expr.Atom("likes"),
                List.of(new Expr.Atom("John"), new Expr.Atom("Mary")));
        Expr e2 = new Expr.SExpr(new Expr.Atom("likes"),
                List.of(new Expr.Atom("John"), new Expr.Atom("Mary")));
        assertEquals(e1, e2);
    }
}
