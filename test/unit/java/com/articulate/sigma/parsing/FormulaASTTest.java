package com.articulate.sigma.parsing;

import com.articulate.sigma.Formula;
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

    private Formula legacy(String input) {
        return new Formula(input);
    }

    @Test
    public void testListP() {
        String s1 = "(instance John Human)";
        assertEquals(legacy(s1).listP(), parse(s1).listP());
    }

    @Test
    public void testAtom() {
        String s1 = "(instance John Human)";
        assertEquals(legacy(s1).atom(), parse(s1).atom());
    }

    @Test
    public void testEmpty() {
        // Expr path: SExpr(null, []) should be treated as empty
        FormulaAST faExprEmpty = new FormulaAST();
        faExprEmpty.setFormula("()");
        faExprEmpty.expr = new Expr.SExpr(null, Collections.emptyList());
        assertTrue(faExprEmpty.empty());
        assertTrue(faExprEmpty.listP());

        // Chain: walk cdrAsFormula until the list is exhausted
        FormulaAST fa = parse("(instance John Human)");
        Formula cdr   = fa.cdrAsFormula();   // (John Human)
        Formula cddr  = cdr.cdrAsFormula();  // (Human)
        Formula cdddr = cddr.cdrAsFormula(); // ()
        assertFalse(cdr.empty());
        assertFalse(cddr.empty());
        assertTrue(cdddr.empty());

        // String-based Formula must agree at every step
        Formula f    = legacy("(instance John Human)");
        Formula fcdr = f.cdrAsFormula();
        Formula fcddr = fcdr.cdrAsFormula();
        Formula fcdddr = fcddr.cdrAsFormula();
        assertEquals(fcdr.empty(),   cdr.empty());
        assertEquals(fcddr.empty(),  cddr.empty());
        assertEquals(fcdddr.empty(), cdddr.empty());
    }

    @Test
    public void testNavigation() {
        String s = "(instance John Human)";
        FormulaAST fa = parse(s);
        Formula f = legacy(s);

        assertEquals(f.car(), fa.car());
        assertEquals(f.cdr(), fa.cdr());
        assertEquals(f.cadr(), fa.cadr());
        assertEquals(f.caddr(), fa.caddr());
        assertEquals(f.cddr(), fa.cddr());

        assertEquals(f.carAsFormula().getFormula(), fa.carAsFormula().getFormula());
        assertEquals(f.cdrAsFormula().getFormula(), fa.cdrAsFormula().getFormula());
        assertEquals(f.cddrAsFormula().getFormula(), fa.cddrAsFormula().getFormula());
    }

    @Test
    public void testNavigationComplex() {
        String s = "(=> (instance ?X Human) (attribute ?X Mortal))";
        FormulaAST fa = parse(s);
        Formula f = legacy(s);

        assertEquals(f.car(), fa.car());
        assertEquals(f.cdr(), fa.cdr());
        assertEquals(f.cadr(), fa.cadr());
        assertEquals(f.caddr(), fa.caddr());
        assertEquals(f.cddr(), fa.cddr());

        assertEquals(f.carAsFormula().getFormula(), fa.carAsFormula().getFormula());
        assertEquals(f.cdrAsFormula().getFormula(), fa.cdrAsFormula().getFormula());
        assertEquals(f.cddrAsFormula().getFormula(), fa.cddrAsFormula().getFormula());
    }

    // carAsFormula / getArgument return sub-FormulaAST with Atom expr — must match
    // Formula's atom/listP/car/cdr contract for non-list values.
    @Test
    public void testAtomSubExpr() {
        String s = "(instance John Human)";
        FormulaAST fa = parse(s);
        Formula f = legacy(s);

        Formula carFa = fa.carAsFormula(); // expr = Atom("instance")
        Formula carF  = f.carAsFormula();  // theFormula = "instance"

        assertEquals(carF.atom(),  carFa.atom());   // both true
        assertEquals(carF.listP(), carFa.listP());  // both false
        assertEquals(carF.empty(), carFa.empty());  // both false
        assertEquals(carF.car(),   carFa.car());    // both null (not a list)
        assertEquals(carF.cdr(),   carFa.cdr());    // both null (not a list)

        // getArgument(1) — "John"
        Formula arg1Fa = fa.getArgument(1);
        Formula arg1F  = f.getArgument(1);
        assertEquals(arg1F.getFormula(), arg1Fa.getFormula());
        assertEquals(arg1F.atom(),  arg1Fa.atom());   // both true
        assertEquals(arg1F.listP(), arg1Fa.listP());  // both false
        assertEquals(arg1F.car(),   arg1Fa.car());    // both null

        // getArgument(2) — "Human"
        Formula arg2Fa = fa.getArgument(2);
        Formula arg2F  = f.getArgument(2);
        assertEquals(arg2F.getFormula(), arg2Fa.getFormula());
        assertEquals(arg2F.atom(), arg2Fa.atom());
    }

    @Test
    public void testArguments() {
        String s = "(instance John Human)";
        FormulaAST fa = parse(s);
        Formula f = legacy(s);

        assertEquals(f.getStringArgument(0), fa.getStringArgument(0));
        assertEquals(f.getStringArgument(1), fa.getStringArgument(1));
        assertEquals(f.getStringArgument(2), fa.getStringArgument(2));
        assertEquals(f.getStringArgument(3), fa.getStringArgument(3));

        assertEquals(f.getArgument(1).getFormula(), fa.getArgument(1).getFormula());
        assertEquals(f.listLength(), fa.listLength());

        assertEquals(f.argumentsToArrayListString(1), fa.argumentsToArrayListString(1));
        assertEquals(f.complexArgumentsToArrayListString(0), fa.complexArgumentsToArrayListString(0));

        List<Formula> fList = f.complexArgumentsToArrayList(1);
        List<Formula> faList = fa.complexArgumentsToArrayList(1);
        assertEquals(fList.size(), faList.size());
        for (int i = 0; i < fList.size(); i++) {
            assertEquals(fList.get(i).getFormula(), faList.get(i).getFormula());
        }
    }

    @Test
    public void testArgumentsComplex() {
        String s = "(=> (instance ?X Human) (attribute ?X Mortal))";
        FormulaAST fa = parse(s);
        Formula f = legacy(s);

        assertEquals(f.getStringArgument(0), fa.getStringArgument(0));
        assertEquals(f.getStringArgument(1), fa.getStringArgument(1));
        assertEquals(f.getStringArgument(2), fa.getStringArgument(2));

        assertEquals(f.getArgument(1).getFormula(), fa.getArgument(1).getFormula());
        assertEquals(f.listLength(), fa.listLength());

        // FormulaAST is strict: returns null for complex formulas as per contract.
        // Legacy Formula is inconsistent (returns list if warmed up), so we don't compare them here.
        assertNull(fa.argumentsToArrayListString(1));

        assertEquals(f.complexArgumentsToArrayListString(0), fa.complexArgumentsToArrayListString(0));

        List<Formula> fList = f.complexArgumentsToArrayList(0);
        List<Formula> faList = fa.complexArgumentsToArrayList(0);
        assertEquals(fList.size(), faList.size());
        for (int i = 0; i < fList.size(); i++) {
            assertEquals(fList.get(i).getFormula(), faList.get(i).getFormula());
        }
    }

    @Test
    public void testLiteralToArrayList() {
        // Simple formula
        String s1 = "(instance John Human)";
        FormulaAST fa1 = parse(s1);
        Formula f1 = legacy(s1);
        assertEquals(f1.literalToArrayList(), fa1.literalToArrayList());

        // Complex formula — outer elements include sub-formulas
        String s2 = "(=> (instance ?X Human) (attribute ?X Mortal))";
        FormulaAST fa2 = parse(s2);
        Formula f2 = legacy(s2);
        assertEquals(f2.literalToArrayList(), fa2.literalToArrayList());
    }

    @Test
    public void testVariablesAndTerms() {
        String s = "(forall (?X) (=> (instance ?X Human) (attribute ?X Mortal)))";
        FormulaAST fa = parse(s);
        Formula f = legacy(s);

        assertEquals(f.collectAllVariables(), fa.collectAllVariables());
        assertEquals(f.collectQuantifiedVariables(), fa.collectQuantifiedVariables());
        assertEquals(f.collectUnquantifiedVariables(), fa.collectUnquantifiedVariables());
        assertEquals(f.collectTerms(), fa.collectTerms());
    }

    @Test
    public void testVariablesNested() {
        // Nested quantifiers — both ?X and ?Y must appear in quantified vars
        String s = "(forall (?X) (exists (?Y) (instance ?X ?Y)))";
        FormulaAST fa = parse(s);
        Formula f = legacy(s);

        Set<String> faQuant = fa.collectQuantifiedVariables();
        Set<String> fQuant  = f.collectQuantifiedVariables();
        assertEquals(fQuant, faQuant);
        assertTrue(faQuant.contains("?X"));
        assertTrue(faQuant.contains("?Y"));

        assertEquals(f.collectUnquantifiedVariables(), fa.collectUnquantifiedVariables());
        assertTrue(fa.collectUnquantifiedVariables().isEmpty());

        assertEquals(f.collectAllVariables(), fa.collectAllVariables());
    }

    @Test
    public void testClassification() {
        String s1 = "(instance John Human)";
        FormulaAST fa1 = parse(s1);
        Formula f1 = legacy(s1);

        assertTrue(fa1.isGround());
        assertEquals(f1.isGround(), fa1.isGround());
        assertTrue(fa1.isSimpleClause(null));
        assertFalse(fa1.isSimpleNegatedClause(null));
        assertFalse(fa1.isFunctionalTerm());
        assertTrue(fa1.isBinary());
        assertFalse(fa1.isExistentiallyQuantified());
        assertFalse(fa1.isUniversallyQuantified());

        String s2 = "(forall (?X) (instance ?X Human))";
        FormulaAST fa2 = parse(s2);
        assertFalse(fa2.isGround());
        assertTrue(fa2.isUniversallyQuantified());

        String s3 = "(not (instance John Human))";
        FormulaAST fa3 = parse(s3);
        assertTrue(fa3.isSimpleNegatedClause(null));
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
