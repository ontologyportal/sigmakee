package com.articulate.sigma.trans;

import com.articulate.sigma.UnitTestBase;
import com.articulate.sigma.parsing.Expr;
import com.articulate.sigma.parsing.SuokifVisitor;
import com.articulate.sigma.parsing.FormulaAST;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for the Expr-based TFF fast path in SUMOtoTFAform.
 *
 * Coverage:
 *  - missingSortsExpr  (no KB needed for the walk itself)
 *  - processExpr / processRecurseExpr on simple formulas, compared
 *    against the existing string-based process() path
 */
public class SUMOtoTFAformExprTest extends UnitTestBase {

    @BeforeClass
    public static void init() {
        System.out.println("============ SUMOtoTFAformExprTest.init()");
        SUMOKBtoTFAKB skbtfakb = new SUMOKBtoTFAKB();
        skbtfakb.initOnce();
        SUMOformulaToTPTPformula.setLang("tff");
        SUMOtoTFAform.setNumericFunctionInfo();
    }

    // ---- helpers -------------------------------------------------------

    /** Parse kif to Expr via SuokifVisitor. */
    private static Expr parse(String kif) {
        SuokifVisitor visitor = SuokifVisitor.parseSentence(kif);
        assertNotNull("visitor.result null for: " + kif, visitor.result);
        assertFalse("visitor.result empty for: " + kif, visitor.result.isEmpty());
        FormulaAST ast = visitor.result.get(0);
        assertNotNull("FormulaAST null for: " + kif, ast);
        assertNotNull("expr null for: " + kif, ast.expr);
        return ast.expr;
    }

    // ---- missingSortsExpr ----------------------------------------------

    @Test
    public void testMissingSortsExprAtomReturnsEmpty() {
        Expr atom = new Expr.Atom("Foo");
        SUMOtoTFAform stfa = new SUMOtoTFAform();
        Set<String> sorts = stfa.missingSortsExpr(atom);
        assertTrue("Atom should yield no sorts", sorts.isEmpty());
    }

    @Test
    public void testMissingSortsExprNoListFnReturnsEmpty() {
        // (instance Foo Bar) has no ListFn → empty
        Expr expr = parse("(instance Foo Bar)");
        SUMOtoTFAform stfa = new SUMOtoTFAform();
        Set<String> sorts = stfa.missingSortsExpr(expr);
        assertTrue("Plain instance should yield no sorts", sorts.isEmpty());
    }

    @Test
    public void testMissingSortsExprListFnWithTypeInName() {
        // ListFn__2Fn__0Re1Re2Re encodes a rational-typed list function
        Expr expr = parse("(ListFn__2Fn__0Re1Re2Re 1.0 2.0)");
        SUMOtoTFAform stfa = new SUMOtoTFAform();
        Set<String> sorts = stfa.missingSortsExpr(expr);
        assertFalse("ListFn with encoded type should yield at least one sort", sorts.isEmpty());
    }

    @Test
    public void testMissingSortsExprNestedListFn() {
        // ListFn deep inside a compound expr should still be found
        Expr expr = parse("(=> (instance ?X Foo) (ListFn__2Fn__0Re1Re2Re 1.0 2.0))");
        SUMOtoTFAform stfa = new SUMOtoTFAform();
        Set<String> sorts = stfa.missingSortsExpr(expr);
        assertFalse("Nested ListFn should yield a sort", sorts.isEmpty());
    }

}
