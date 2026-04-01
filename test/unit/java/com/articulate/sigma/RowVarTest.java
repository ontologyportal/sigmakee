package com.articulate.sigma;

import com.articulate.sigma.parsing.FormulaAST;
import com.articulate.sigma.parsing.RowVar;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * These tests follow PredVarInst.test( ), with the exception of that method's call to FormulaPreprocessor.
 * findExplicitTypesInAntecedent( ), which has been put into the FormulaPreprocessorTest class.
 * TODO: See how relevant the line "if (kb.kbCache.transInstOf("exhaustiveAttribute","VariableArityRelation"))"
 * at the start of the original PredVarInst.test( ) method is. Should these tests somehow reflect that?
 */
public class RowVarTest extends UnitTestBase  {

    /** ***************************************************************
     */
    @Test
    public void testFindRowVars() {

        System.out.println("\n=========== testFindRowVars =================");
        String stmt1 = "(links @ARGS)";

        Formula f = new Formula();
        f.read(stmt1);

        //RowVars.DEBUG = true;
        Set<String> vars = RowVars.findRowVars(f);
        assertTrue(vars != null && !vars.isEmpty());
        if (vars.contains("@ARGS") && vars.size() == 1)
            System.out.println("testFindRowVars(): success!");
        else
            System.err.println("testFindRowVars(): failure");
        assertTrue(vars.contains("@ARGS") && vars.size() == 1);
    }

    /** ***************************************************************
     */
    @Test
    public void testRowVarRels() {

        System.out.println("\n=========== testRowVarRels =================");
        String stmt1 = "(=>\n" +
                "  (and\n" +
                "    (minValue links ?ARG ?N)\n" +
                "    (links @ARGS)\n" +
                "    (equal ?VAL\n" +
                "      (ListOrderFn\n" +
                "        (ListFn @ARGS) ?ARG)))\n" +
                "  (greaterThan ?VAL ?N))";

        Formula f = new Formula();
        f.read(stmt1);

        //RowVars.DEBUG = true;
        Map<String, Set<String>> rels = RowVars.getRowVarRelations(f);
        assertTrue(rels != null && !rels.keySet().isEmpty());
        System.out.println("testRowVarRels(): rels: " + rels);
        if (rels.get("@ARGS").contains("links"))
            System.out.println("testRowVarRels(): success!");
        else
            System.err.println("testRowVarRels(): failure");
        assertTrue(rels.get("@ARGS").contains("links"));
    }

    /** ***************************************************************
     */
    @Test
    public void testLinks() {

        System.out.println("\n=========== testLinks =================");
        String stmt1 = "(=>\n" +
                "  (and\n" +
                "    (minValue links ?ARG ?N)\n" +
                "    (links @ARGS)\n" +
                "    (equal ?VAL\n" +
                "      (ListOrderFn\n" +
                "        (ListFn @ARGS) ?ARG)))\n" +
                "  (greaterThan ?VAL ?N))";

        Formula f = new Formula();
        f.read(stmt1);

        //RowVars.DEBUG = true;
        Map<String,Set<String>> rels = RowVars.getRowVarRelations(f);
        Map<String,Integer> rowVarMaxArities = RowVars.getRowVarMaxAritiesWithOtherArgs(rels, kb, f);
        int arity = kb.kbCache.valences.get("links");
        System.out.println("testLinks(): arity of 'links': " + arity);
        System.out.println("testLinks(): rels: " + rels);
        System.out.println("testLinks(): rowVarMaxArities: " + rowVarMaxArities);
        System.out.println("testLinks(): result: " + rowVarMaxArities.get("@ARGS"));
        System.out.println("testLinks(): expected: " + 3);
        if (3 == rowVarMaxArities.get("@ARGS"))
            System.out.println("testLinks(): success!");
        else
            System.err.println("testLinks(): failure");
        assertEquals(3, rowVarMaxArities.get("@ARGS").intValue());
    }

    /** ***************************************************************
     */
    @Test
    public void testLinks2() {

        System.out.println("\n=========== testLinks2 =================");
        String stmt1 = "(=>\n" +
                "  (and\n" +
                "    (minValue links ?ARG ?N)\n" +
                "    (links @ARGS)\n" +
                "    (equal ?VAL\n" +
                "      (ListOrderFn\n" +
                "        (ListFn @ARGS) ?ARG)))\n" +
                "  (greaterThan ?VAL ?N))";

        Formula f = new Formula();
        f.read(stmt1);

        //RowVars.DEBUG = true;
        List<Formula> results = RowVars.expandRowVars(kb,f);
        String result = results.get(0).getFormula();
        String expected = "(=>\n" +
                "  (and\n" +
                "    (minValue links ?ARG ?N)\n" +
                "    (links ?ARGS2 ?ARGS3 ?ARGS4)\n" +
                "    (equal ?VAL\n" +
                "      (ListOrderFn\n" +
                "        (ListFn ?ARGS2 ?ARGS3 ?ARGS4) ?ARG)))\n" +
                "  (greaterThan ?VAL ?N))";
        System.out.println("testLinks2(): result: " + result);
        System.out.println("testLinks2(): expected: " + expected);
        if (expected.equals(result))
            System.out.println("testLinks2(): success!");
        else
            System.err.println("testLinks2(): failure");
        assertEquals(expected,result);
    }

    /** ***************************************************************
     * abstractCounterpart has arity 2, between has arity 3.  Sharing @ROW
     * means the two predicates demand different expansion sizes (2 vs 3).
     * findArities() must detect the conflict and mark the row var with 0.
     */
    @Test
    public void testFindAritiesConflictDetected() {

        System.out.println("\n=========== testFindAritiesConflictDetected =================");

        FormulaAST f = new FormulaAST();
        f.setFormula("(=> (abstractCounterpart @ROW) (between @ROW))");

        FormulaAST.RowStruct rs1 = new FormulaAST.RowStruct();
        rs1.rowvar  = "@ROW";
        rs1.pred    = "abstractCounterpart";
        rs1.literal = "(abstractCounterpart @ROW)";
        rs1.arity   = 1;

        FormulaAST.RowStruct rs2 = new FormulaAST.RowStruct();
        rs2.rowvar  = "@ROW";
        rs2.pred    = "between";
        rs2.literal = "(between @ROW)";
        rs2.arity   = 1;

        f.addRowVarStruct("@ROW", rs1);
        f.addRowVarStruct("@ROW", rs2);

        RowVar rv = new RowVar(kb);
        Map<String, Integer> arities = rv.findArities(f);
        System.out.println("testFindAritiesConflictDetected(): arities=" + arities);

        assertEquals("Arity conflict between arity-2 and arity-3 predicates must be marked as 0",
                0, arities.get("@ROW").intValue());
    }

    /** ***************************************************************
     * When findArities() marks a row variable with sentinel 0 (arity conflict),
     * expandRowVar() (string path) must return an empty set rather than emitting
     * an invalid formula.
     */
    @Test
    public void testExpandRowVarDropsConflictingFormula() {

        System.out.println("\n=========== testExpandRowVarDropsConflictingFormula =================");

        FormulaAST f = new FormulaAST();
        f.setFormula("(=> (abstractCounterpart @ROW) (between @ROW))");

        FormulaAST.RowStruct rs1 = new FormulaAST.RowStruct();
        rs1.rowvar  = "@ROW";
        rs1.pred    = "abstractCounterpart";
        rs1.literal = "(abstractCounterpart @ROW)";
        rs1.arity   = 1;

        FormulaAST.RowStruct rs2 = new FormulaAST.RowStruct();
        rs2.rowvar  = "@ROW";
        rs2.pred    = "between";
        rs2.literal = "(between @ROW)";
        rs2.arity   = 1;

        f.addRowVarStruct("@ROW", rs1);
        f.addRowVarStruct("@ROW", rs2);

        RowVar rv = new RowVar(kb);
        Set<FormulaAST> result = rv.expandRowVar(f);
        System.out.println("testExpandRowVarDropsConflictingFormula(): result size=" + result.size());

        assertTrue("Formula with row-var arity conflict must be dropped (empty result)", result.isEmpty());
    }

    /** ***************************************************************
     * Same arity conflict scenario, but through the Expr fast path.
     * expandRowVarExpr() must also return an empty set on arity conflict
     * (not splice @ROW with 0 vars, which would produce zero-arg predicates
     * and crash processOtherRelationExpr).
     */
    @Test
    public void testExpandRowVarExprDropsConflictingFormula() {

        System.out.println("\n=========== testExpandRowVarExprDropsConflictingFormula =================");

        FormulaAST f = new FormulaAST();
        f.setFormula("(=> (abstractCounterpart @ROW) (between @ROW))");

        FormulaAST.RowStruct rs1 = new FormulaAST.RowStruct();
        rs1.rowvar  = "@ROW";
        rs1.pred    = "abstractCounterpart";
        rs1.literal = "(abstractCounterpart @ROW)";
        rs1.arity   = 1;

        FormulaAST.RowStruct rs2 = new FormulaAST.RowStruct();
        rs2.rowvar  = "@ROW";
        rs2.pred    = "between";
        rs2.literal = "(between @ROW)";
        rs2.arity   = 1;

        f.addRowVarStruct("@ROW", rs1);
        f.addRowVarStruct("@ROW", rs2);
        // rowVarCache must be non-empty for preProcessExpr to trigger row-var expansion
        f.rowVarCache = new java.util.HashSet<>();
        f.rowVarCache.add("@ROW");
        // also set up a minimal Expr so expandRowVarExpr has something to work with
        f.expr = com.articulate.sigma.parsing.SuokifVisitor.parseSentence(f.getFormula()).result.get(0).expr;

        RowVar rv = new RowVar(kb);
        java.util.Set<com.articulate.sigma.parsing.Expr> result = rv.expandRowVarExpr(f);
        System.out.println("testExpandRowVarExprDropsConflictingFormula(): result size=" + result.size());

        assertTrue("Expr row-var expansion must drop formula with arity conflict (empty result)",
                result.isEmpty());
    }

    /** ***************************************************************
     * abstractCounterpart has arity 2 with non-numeric domain types;
     * identityElement has arity 2 with Integer at arg 2.  Both have the
     * same rowVarArity (no size conflict), but arg 2 has incompatible
     * types: Physical (non-numeric) vs Integer ($int).
     * findArities() must detect the domain-type conflict and mark the
     * row var with 0 so expandRowVar() drops the formula.
     */
    @Test
    public void testFindAritiesDomainTypeConflict() {

        System.out.println("\n=========== testFindAritiesDomainTypeConflict =================");

        // Skip if either predicate is absent from the test KB
        if (!kb.containsTerm("abstractCounterpart") || !kb.containsTerm("identityElement")) {
            System.out.println("testFindAritiesDomainTypeConflict: skipped (predicate not in KB)");
            return;
        }

        FormulaAST f = new FormulaAST();
        f.setFormula("(=> (abstractCounterpart @ROW) (identityElement @ROW))");

        // Both predicates are arity 2; @ROW is the sole arg (rs.arity=1), so rowVarArity=2
        FormulaAST.RowStruct rs1 = new FormulaAST.RowStruct();
        rs1.rowvar  = "@ROW";
        rs1.pred    = "abstractCounterpart";
        rs1.literal = "(abstractCounterpart @ROW)";
        rs1.arity   = 1;

        FormulaAST.RowStruct rs2 = new FormulaAST.RowStruct();
        rs2.rowvar  = "@ROW";
        rs2.pred    = "identityElement";
        rs2.literal = "(identityElement @ROW)";
        rs2.arity   = 1;

        f.addRowVarStruct("@ROW", rs1);
        f.addRowVarStruct("@ROW", rs2);

        RowVar rv = new RowVar(kb);
        Map<String, Integer> arities = rv.findArities(f);
        System.out.println("testFindAritiesDomainTypeConflict(): arities=" + arities);

        assertEquals("Domain type conflict (Physical vs Integer) at same expansion position must be marked as 0",
                0, arities.get("@ROW").intValue());
    }

    /** ***************************************************************
     */
    @Test
    public void testRowVarExp() {

        System.out.println("\n=========== testRowVarExp =================");
        String stmt = "(<=> (partition @ROW) (and (exhaustiveDecomposition @ROW) (disjointDecomposition @ROW)))";
        Formula f = new Formula();
        f.read(stmt);

        //RowVars.DEBUG = true;
        List<Formula> results = RowVars.expandRowVars(kb,f);
        System.out.println("testRowVarExp(: input: " + stmt);
        System.out.println("testRowVarExp(): results: " + results);
        System.out.println("testRowVarExp(): results size: " + results.size());
        System.out.println("testRowVarExp(): expected: " + 7);
        if (results.size() == RowVars.MAX_ARITY)
            System.out.println("testLinks(): success!");
        else
            System.out.println("testLinks(): failure");
        assertTrue(results.size() == RowVars.MAX_ARITY);
    }
}