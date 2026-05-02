package com.articulate.sigma.parsing;

import com.articulate.sigma.KIFAST;
import com.articulate.sigma.UnitTestBase;
import org.junit.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for KIFAST: verifies that the ANTLR-based parser produces the correct
 * formulaMap, formulas (predicate-position index), terms, and Expr trees.
 */
public class KIFASTTest extends UnitTestBase {

    private static KIFAST parse(String kif) {
        KIFAST k = new KIFAST();
        k.parse(new StringReader(kif));
        return k;
    }

    // -----------------------------------------------------------------------
    // formulaMap tests
    // -----------------------------------------------------------------------

    @Test
    public void simpleRelsentInFormulaMap() {
        KIFAST k = parse("(likes John Mary)");
        assertTrue("formula must be in formulaMap", k.formulaMap.containsKey("(likes John Mary)"));
        FormulaAST f = k.formulaMap.get("(likes John Mary)");
        assertNotNull(f);
        assertNotNull("Expr must be populated", f.expr);
        assertEquals("(likes John Mary)", f.expr.toKifString());
    }

    @Test
    public void twoFormulasInFormulaMap() {
        KIFAST k = parse("(likes John Mary)\n(part Wheel1 Car2)\n");
        assertEquals(2, k.formulaMap.size());
        assertTrue(k.formulaMap.containsKey("(likes John Mary)"));
        assertTrue(k.formulaMap.containsKey("(part Wheel1 Car2)"));
    }

    @Test
    public void noDuplicatesInFormulaMap() {
        KIFAST k = parse("(likes John Mary)\n(likes John Mary)\n");
        // Second occurrence is a duplicate and should not be stored again
        assertEquals(1, k.formulaMap.size());
    }

    @Test
    public void implicationInFormulaMap() {
        String kif = "(=> (instance ?X Man) (attribute ?X Mortal))";
        KIFAST k = parse(kif);
        assertTrue(k.formulaMap.containsKey(kif));
        FormulaAST f = k.formulaMap.get(kif);
        assertTrue(f.expr instanceof Expr.SExpr);
        assertEquals("=>", ((Expr.SExpr) f.expr).headName());
    }

    // -----------------------------------------------------------------------
    // Terms tests
    // -----------------------------------------------------------------------

    @Test
    public void termsCollected() {
        KIFAST k = parse("(instance Foo Bar)");
        assertTrue(k.terms.contains("instance"));
        assertTrue(k.terms.contains("Foo"));
        assertTrue(k.terms.contains("Bar"));
    }

    @Test
    public void variablesNotInTerms() {
        KIFAST k = parse("(instance ?X Animal)");
        assertFalse("Variables must not be in terms", k.terms.contains("?X"));
        assertTrue(k.terms.contains("instance"));
        assertTrue(k.terms.contains("Animal"));
    }

    @Test
    public void termFrequencyTracked() {
        KIFAST k = parse("(instance Foo Bar)\n(instance Baz Bar)\n");
        assertEquals(2, (int) k.termFrequency.get("instance"));
        assertEquals(2, (int) k.termFrequency.get("Bar"));
        assertEquals(1, (int) k.termFrequency.get("Foo"));
    }

    // -----------------------------------------------------------------------
    // Predicate-position index (formulas map) tests
    // -----------------------------------------------------------------------

    @Test
    public void argKeysForSimpleRelsent() {
        // (likes John Mary) → arg-0-likes, arg-1-John, arg-2-Mary
        KIFAST k = parse("(likes John Mary)");
        List<String> arg0 = k.formulas.get("arg-0-likes");
        assertNotNull("arg-0-likes must exist", arg0);
        assertTrue(arg0.contains("(likes John Mary)"));

        List<String> arg1 = k.formulas.get("arg-1-John");
        assertNotNull(arg1);
        assertTrue(arg1.contains("(likes John Mary)"));

        List<String> arg2 = k.formulas.get("arg-2-Mary");
        assertNotNull(arg2);
        assertTrue(arg2.contains("(likes John Mary)"));
    }

    @Test
    public void antConsKeysForImplication() {
        // (=> (instance ?X Man) (attribute ?X Mortal))
        // → ant-instance, ant-Man, cons-attribute, cons-Mortal, arg-0-=>
        String kif = "(=> (instance ?X Man) (attribute ?X Mortal))";
        KIFAST k = parse(kif);

        List<String> arg0 = k.formulas.get("arg-0-=>");
        assertNotNull("arg-0-=> must exist", arg0);
        assertTrue(arg0.contains(kif));

        List<String> ant = k.formulas.get("ant-instance");
        assertNotNull("ant-instance must exist", ant);
        assertTrue(ant.contains(kif));

        List<String> antMan = k.formulas.get("ant-Man");
        assertNotNull("ant-Man must exist", antMan);
        assertTrue(antMan.contains(kif));

        List<String> cons = k.formulas.get("cons-attribute");
        assertNotNull("cons-attribute must exist", cons);
        assertTrue(cons.contains(kif));

        List<String> consMortal = k.formulas.get("cons-Mortal");
        assertNotNull("cons-Mortal must exist", consMortal);
        assertTrue(consMortal.contains(kif));
    }

    @Test
    public void stmtKeysForNested() {
        // (not (instance ?X Animal)) → arg-0-not, stmt-instance, stmt-Animal
        String kif = "(not (instance ?X Animal))";
        KIFAST k = parse(kif);

        assertNotNull("arg-0-not must exist", k.formulas.get("arg-0-not"));
        assertNotNull("stmt-instance must exist", k.formulas.get("stmt-instance"));
        assertNotNull("stmt-Animal must exist", k.formulas.get("stmt-Animal"));
    }

    @Test
    public void iffAntConsKeys() {
        String kif = "(<=> (instance ?X Man) (instance ?X Human))";
        KIFAST k = parse(kif);
        // <=> head → "arg-0-<=>"
        assertNotNull(k.formulas.get("arg-0-<=>"));
        // antecedent
        assertNotNull(k.formulas.get("ant-instance"));
        assertNotNull(k.formulas.get("ant-Man"));
        // consequent
        assertNotNull(k.formulas.get("cons-instance"));
        assertNotNull(k.formulas.get("cons-Human"));
    }

    @Test
    public void formulaStringIsOwnKey() {
        // The formula string is always one of its own keys in the formulas index
        String kif = "(instance Foo Bar)";
        KIFAST k = parse(kif);
        List<String> selfKey = k.formulas.get(kif);
        assertNotNull("Formula string must be its own key", selfKey);
        assertTrue(selfKey.contains(kif));
    }

    @Test
    public void nestedFunctionTerm() {
        // (instance (HourFn 12 ?DAY) TimeInterval)
        // → arg-0-instance, arg-2-TimeInterval, stmt-HourFn
        String kif = "(instance (HourFn 12 ?DAY) TimeInterval)";
        KIFAST k = parse(kif);
        assertNotNull("arg-0-instance", k.formulas.get("arg-0-instance"));
        assertNotNull("arg-2-TimeInterval", k.formulas.get("arg-2-TimeInterval"));
        assertNotNull("stmt-HourFn", k.formulas.get("stmt-HourFn"));
    }

    // -----------------------------------------------------------------------
    // Expr tree tests
    // -----------------------------------------------------------------------

    @Test
    public void exprFieldPopulated() {
        KIFAST k = parse("(instance Foo Bar)");
        FormulaAST f = k.formulaMap.get("(instance Foo Bar)");
        assertNotNull("expr must not be null", f.expr);
        assertTrue(f.expr instanceof Expr.SExpr);
    }

    @Test
    public void exprRoundTrip() {
        String kif = "(=> (and (instance ?X Man) (instance ?X Person)) (instance ?X Animal))";
        KIFAST k = parse(kif);
        FormulaAST f = k.formulaMap.get(kif);
        assertNotNull(f.expr);
        assertEquals(kif, f.expr.toKifString());
    }

    @Test
    public void multipleFormulaExprs() {
        String kif = "(likes John Mary)\n(part Wheel1 Car2)\n";
        KIFAST k = parse(kif);
        for (FormulaAST f : k.formulaMap.values()) {
            assertNotNull("Every FormulaAST must have an Expr", f.expr);
            // Round-trip check
            assertEquals(f.getFormula(), f.expr.toKifString());
        }
    }
}
