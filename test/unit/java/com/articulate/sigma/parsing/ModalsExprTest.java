package com.articulate.sigma.parsing;

import com.articulate.sigma.trans.Modals;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for the Expr-based modal-processing methods in {@link Modals}.
 *
 * <p>All tests run without a live KB (kb=null).  The methods under test call
 * {@code kb.kbCache.getSignature()} only when the head predicate is neither a
 * logical operator, quantifier, rigid relation, reserved modal symbol, nor a
 * HOL-rewrite predicate.  Passing {@code null} is safe for the predicates used
 * in these tests because every predicate used is either rigid, a HOL-rewrite
 * head, or handled before the kb look-up path is reached.
 */
public class ModalsExprTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Expr.Atom  atom(String s)    { return new Expr.Atom(s); }
    private static Expr.Var   var(String s)      { return new Expr.Var(s); }

    private static Expr.SExpr se(String head, Expr... args) {
        return new Expr.SExpr(new Expr.Atom(head), List.of(args));
    }

    // -----------------------------------------------------------------------
    // makeWorldVarExpr
    // -----------------------------------------------------------------------

    @Test
    public void makeWorldVar_noConflict_returnsW0() {
        // A formula with no ?W vars → ?W0
        Expr expr = se("foo", var("?X"));
        assertEquals("?W0", Modals.makeWorldVarExpr(expr));
    }

    @Test
    public void makeWorldVar_W0Taken_returnsW1() {
        // Formula already uses ?W0 → skip to ?W1
        Expr expr = se("foo", var("?W0"), var("?X"));
        assertEquals("?W1", Modals.makeWorldVarExpr(expr));
    }

    @Test
    public void makeWorldVar_W0andW1Taken_returnsW2() {
        Expr expr = se("foo", var("?W0"), var("?W1"));
        assertEquals("?W2", Modals.makeWorldVarExpr(expr));
    }

    // -----------------------------------------------------------------------
    // markModalAttributeFormulaVarsExpr
    // -----------------------------------------------------------------------

    @Test
    public void markModalAttribute_addsFormulaType() {
        // (modalAttribute ?F Necessity) → ?F should get type "Formula"
        Expr expr = se("modalAttribute", var("?F"), atom("Necessity"));
        Map<String, Set<String>> typeMap = new HashMap<>();
        Modals.markModalAttributeFormulaVarsExpr(expr, typeMap);
        assertTrue("?F should have Formula type",
                typeMap.containsKey("?F") && typeMap.get("?F").contains("Formula"));
    }

    @Test
    public void markModalAttribute_noModalAttribute_emptyMap() {
        Expr expr = se("instance", var("?X"), atom("Animal"));
        Map<String, Set<String>> typeMap = new HashMap<>();
        Modals.markModalAttributeFormulaVarsExpr(expr, typeMap);
        assertTrue("No modalAttribute → typeMap should be empty", typeMap.isEmpty());
    }

    @Test
    public void markModalAttribute_nested_findsDeep() {
        // (and (foo ?X) (modalAttribute ?F Possibility))
        Expr expr = se("and",
                se("foo", var("?X")),
                se("modalAttribute", var("?F"), atom("Possibility")));
        Map<String, Set<String>> typeMap = new HashMap<>();
        Modals.markModalAttributeFormulaVarsExpr(expr, typeMap);
        assertTrue("nested modalAttribute: ?F should have Formula type",
                typeMap.containsKey("?F") && typeMap.get("?F").contains("Formula"));
    }

    // -----------------------------------------------------------------------
    // processModalsExpr — rigid predicates do NOT get world arg
    // -----------------------------------------------------------------------

    @Test
    public void processModals_rigidPredicate_noWorldArg() {
        // (instance ?X Animal) — instance is RIGID → no world arg added
        Expr expr = se("instance", var("?X"), atom("Animal"));
        Map.Entry<Expr, Map<String, Set<String>>> result =
                Modals.processModalsExpr(expr, null);
        Expr out = result.getKey();
        assertTrue("result should still be an SExpr", out instanceof Expr.SExpr);
        Expr.SExpr outSe = (Expr.SExpr) out;
        // Still 2 args (no world arg appended)
        assertEquals("rigid pred keeps 2 args", 2, outSe.args().size());
        assertEquals("head unchanged", "instance", outSe.headName());
    }

    @Test
    public void processModals_rigidPredicates_subclass() {
        // (subclass ?X Entity) — subclass is RIGID
        Expr expr = se("subclass", var("?X"), atom("Entity"));
        Map.Entry<Expr, Map<String, Set<String>>> result =
                Modals.processModalsExpr(expr, null);
        Expr.SExpr out = (Expr.SExpr) result.getKey();
        assertEquals("subclass keeps 2 args", 2, out.args().size());
    }

    // -----------------------------------------------------------------------
    // processModalsExpr — world type map
    // -----------------------------------------------------------------------

    @Test
    public void processModals_worldTypeMap_containsWorldEntry() {
        Expr expr = se("believes", var("?A"), se("instance", var("?X"), atom("Animal")));
        Map.Entry<Expr, Map<String, Set<String>>> result =
                Modals.processModalsExpr(expr, null);
        Map<String, Set<String>> worldTypes = result.getValue();
        // Should contain at least one entry whose value contains "World"
        boolean hasWorldEntry = worldTypes.values().stream()
                .anyMatch(ts -> ts.contains("World"));
        assertTrue("processModalsExpr should record World-typed variables",
                hasWorldEntry);
    }

    // -----------------------------------------------------------------------
    // processModalsExpr — HOL predicates (holdsDuring / believes)
    // -----------------------------------------------------------------------

    @Test
    public void processModals_holdsDuring_rewritesToAccreln2() {
        // (holdsDuring ?T (foo ?X)) → (=> (accreln2 holdsDuring ?T CW ?W2) (foo ?X ?W2))
        // (holdsDuring is in regHOLpred)
        Expr expr = se("holdsDuring", var("?T"), se("instance", var("?X"), atom("Dog")));
        Map.Entry<Expr, Map<String, Set<String>>> result =
                Modals.processModalsExpr(expr, null);
        Expr out = result.getKey();
        assertTrue("holdsDuring rewrites to SExpr", out instanceof Expr.SExpr);
        Expr.SExpr outSe = (Expr.SExpr) out;
        // Top-level should be (=> ...)
        assertEquals("top-level op is =>", "=>", outSe.headName());
        // Antecedent is (accreln2 ...)
        Expr ant = outSe.args().get(0);
        assertTrue("antecedent is SExpr", ant instanceof Expr.SExpr);
        assertEquals("antecedent head is accreln2", "accreln2",
                ((Expr.SExpr) ant).headName());
    }

    @Test
    public void processModals_believes_rewritesToAccreln2() {
        // (believes ?A (foo ?X)) — believes is in regHOLpred
        Expr expr = se("believes", var("?A"), se("instance", var("?X"), atom("Cat")));
        Map.Entry<Expr, Map<String, Set<String>>> result =
                Modals.processModalsExpr(expr, null);
        Expr.SExpr out = (Expr.SExpr) result.getKey();
        assertEquals("top-level op is =>", "=>", out.headName());
        Expr ant = out.args().get(0);
        assertEquals("antecedent head is accreln2", "accreln2",
                ((Expr.SExpr) ant).headName());
    }

    // -----------------------------------------------------------------------
    // processModalsExpr — HOL3 predicate (confersNorm)
    // -----------------------------------------------------------------------

    @Test
    public void processModals_confersNorm_rewritesToAccreln3() {
        // (confersNorm ?A ?B ?F) — confersNorm is in regHOL3pred
        Expr expr = se("confersNorm", var("?A"), var("?B"),
                se("instance", var("?X"), atom("Animal")));
        Map.Entry<Expr, Map<String, Set<String>>> result =
                Modals.processModalsExpr(expr, null);
        Expr.SExpr out = (Expr.SExpr) result.getKey();
        assertEquals("top-level op is =>", "=>", out.headName());
        Expr ant = out.args().get(0);
        assertTrue("antecedent is SExpr", ant instanceof Expr.SExpr);
        assertEquals("antecedent head is accreln3norm", "accreln3norm",
                ((Expr.SExpr) ant).headName());
    }

    // -----------------------------------------------------------------------
    // processModalsExpr — modalAttribute rewrite
    // -----------------------------------------------------------------------

    @Test
    public void processModals_modalAttribute_rewritesToAccreln1() {
        // (modalAttribute (foo ?X) Necessity) → (=> (accreln1 Necessity CW ?WN) (foo ?X ?WN))
        Expr expr = se("modalAttribute",
                se("instance", var("?X"), atom("Dog")),
                atom("Necessity"));
        Map.Entry<Expr, Map<String, Set<String>>> result =
                Modals.processModalsExpr(expr, null);
        Expr.SExpr out = (Expr.SExpr) result.getKey();
        assertEquals("top-level op is =>", "=>", out.headName());
        Expr ant = out.args().get(0);
        assertTrue("antecedent is SExpr", ant instanceof Expr.SExpr);
        assertEquals("antecedent head is accreln1", "accreln1",
                ((Expr.SExpr) ant).headName());
    }

    // -----------------------------------------------------------------------
    // processModalsExpr — logical operators preserve structure
    // -----------------------------------------------------------------------

    @Test
    public void processModals_and_preservesStructure() {
        // (and (instance ?X Dog) (instance ?Y Cat)) — all rigid → no world args
        Expr expr = se("and",
                se("instance", var("?X"), atom("Dog")),
                se("instance", var("?Y"), atom("Cat")));
        Map.Entry<Expr, Map<String, Set<String>>> result =
                Modals.processModalsExpr(expr, null);
        Expr.SExpr out = (Expr.SExpr) result.getKey();
        assertEquals("top-level and preserved", "and", out.headName());
        assertEquals("still 2 conjuncts", 2, out.args().size());
        // Each conjunct is still (instance ...) with 2 args
        Expr.SExpr c1 = (Expr.SExpr) out.args().get(0);
        assertEquals("conjunct 1 head", "instance", c1.headName());
        assertEquals("conjunct 1 arg count", 2, c1.args().size());
    }

    @Test
    public void processModals_implication_preservesStructure() {
        // (=> (instance ?X Dog) (instance ?X Animal)) — all rigid
        Expr expr = se("=>",
                se("instance", var("?X"), atom("Dog")),
                se("instance", var("?X"), atom("Animal")));
        Map.Entry<Expr, Map<String, Set<String>>> result =
                Modals.processModalsExpr(expr, null);
        Expr.SExpr out = (Expr.SExpr) result.getKey();
        assertEquals("top-level => preserved", "=>", out.headName());
    }

    // -----------------------------------------------------------------------
    // processRecurseExpr — quantifier var list not modified
    // -----------------------------------------------------------------------

    @Test
    public void processRecurse_quantifier_varListUntouched() {
        // (forall (?X) (instance ?X Dog)) — var list should not get world arg
        Expr.SExpr varList = new Expr.SExpr(null, List.of(var("?X")));
        Expr expr = new Expr.SExpr(atom("forall"), List.of(varList,
                se("instance", var("?X"), atom("Dog"))));
        Expr out = Modals.processRecurseExpr(expr, null, "W", 1);
        assertTrue(out instanceof Expr.SExpr);
        Expr.SExpr outSe = (Expr.SExpr) out;
        assertEquals("forall preserved", "forall", outSe.headName());
        // First arg (var list) should still be a null-head SExpr with exactly 1 arg
        Expr vl = outSe.args().get(0);
        assertTrue("var list is SExpr", vl instanceof Expr.SExpr);
        Expr.SExpr vlSe = (Expr.SExpr) vl;
        assertNull("var list has null head", vlSe.headName());
        assertEquals("var list has 1 entry", 1, vlSe.args().size());
    }
}
