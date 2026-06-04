package com.articulate.sigma.parsing;

import com.articulate.sigma.KB;
import org.junit.Test;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

public class FormulaASTTest {

    private FormulaAST parse(String input) {
        SuokifVisitor visitor = SuokifVisitor.parseSentence(input);
        return visitor.result.get(0);
    }

    @Test
    public void testFunctionalTerm() {
        // False case — predicate, not a function application
        FormulaAST faInst = parse("(instance John Human)");
        assertFalse(faInst.isFunctionalTerm());

        // True case — construct FormulaAST directly with an Expr whose head ends with "Fn"
        FormulaAST faFn = new FormulaAST();
        faFn.setFormula("(AdditionFn 1 2)");
        faFn.expr = new Expr.SExpr(
                new Expr.Atom("AdditionFn"),
                List.of(new Expr.NumLiteral("1"), new Expr.NumLiteral("2")));
        assertTrue(faFn.isFunctionalTerm());

        FormulaAST faFn2 = new FormulaAST();
        faFn2.setFormula("(MultiplicationFn 3 4)");
        faFn2.expr = new Expr.SExpr(
                new Expr.Atom("MultiplicationFn"),
                List.of(new Expr.NumLiteral("3"), new Expr.NumLiteral("4")));
        assertTrue(faFn2.isFunctionalTerm());
    }

    @Test
    public void testTransformation() {
        String s = "(instance ?X ?Y)";
        FormulaAST fa = parse(s);
        Map<String, String> m = new HashMap<>();
        m.put("?X", "John");
        m.put("?Y", "Human");

        FormulaAST faSub = fa.substituteVariables(m);
        assertEquals("(instance John Human)", faSub.getFormula());
        assertNotNull(faSub.expr);

        // After substituting both variables with constants the result must be ground
        assertTrue(faSub.isGround());
        assertTrue(faSub.collectAllVariables().isEmpty());

        // replaceVar — replaces one variable, leaves the other
        FormulaAST faRep = fa.replaceVar("?X", "Mary");
        assertEquals("(instance Mary ?Y)", faRep.getFormula());

        // "Mary" is a constant — only ?Y should remain as a variable
        assertFalse(faRep.isGround());
        Set<String> repVars = faRep.collectAllVariables();
        assertEquals(1, repVars.size());
        assertTrue(repVars.contains("?Y"));
    }

    // substituteVariables must infer the correct Expr node type from the replacement string.
    @Test
    public void testSubstitutionExprTypes() {
        FormulaAST fa = parse("(loves ?X ?Y)");

        // Var → Atom (constant replacement)
        FormulaAST sub1 = fa.substituteVariables(Map.of("?X", "John", "?Y", "Mary"));
        assertTrue(sub1.isGround());
        assertTrue(sub1.collectAllVariables().isEmpty());

        // Var → Var (rename)
        FormulaAST sub2 = fa.substituteVariables(Map.of("?X", "?Z"));
        assertFalse(sub2.isGround());
        Set<String> vars2 = sub2.collectAllVariables();
        assertTrue(vars2.contains("?Z"));
        assertFalse(vars2.contains("?X")); // original var is gone
        assertTrue(vars2.contains("?Y"));  // untouched var remains

        // Var → RowVar
        FormulaAST sub3 = fa.substituteVariables(Map.of("?X", "@ROW"));
        Set<String> vars3 = sub3.collectAllVariables();
        assertTrue(vars3.contains("@ROW"));
        assertFalse(vars3.contains("?X"));
    }

    // collectAllVariables must pick up row variables as well as regular variables.
    @Test
    public void testRowVariablesInCollection() {
        // Build a FormulaAST with a RowVar in the Expr tree directly,
        // since formulas with @ROW in argument position are preprocessed.
        FormulaAST fa = parse("(instance ?X Human)");
        // Inject @ROW via substituteVariables to get a known Expr tree
        FormulaAST faRow = fa.substituteVariables(Map.of("?X", "@ROW"));

        Set<String> vars = faRow.collectAllVariables();
        assertTrue(vars.contains("@ROW"));
        assertFalse(vars.contains("?X"));

        // A RowVar is NOT a ground formula
        assertFalse(faRow.isGround());
    }
}
