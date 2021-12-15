package com.articulate.sigma;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
        HashSet<String> vars = RowVars.findRowVars(f);
        assertTrue(vars != null && vars.size() > 0);
        if (vars.contains("@ARGS") && vars.size() == 1)
            System.out.println("testFindRowVars(): success!");
        else
            System.out.println("testFindRowVars(): failure");
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
        HashMap<String, HashSet<String>> rels = RowVars.getRowVarRelations(f);
        assertTrue(rels != null && rels.keySet().size() > 0);
        System.out.println("testRowVarRels(): rels: " + rels);
        if (rels.get("@ARGS").contains("links"))
            System.out.println("testRowVarRels(): success!");
        else
            System.out.println("testRowVarRels(): failure");
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
        HashMap<String,HashSet<String>> rels = RowVars.getRowVarRelations(f);
        HashMap<String,Integer> rowVarMaxArities = RowVars.getRowVarMaxAritiesWithOtherArgs(rels, kb, f);
        int arity = kb.kbCache.valences.get("links").intValue();
        System.out.println("testLinks(): arity of 'links': " + arity);
        System.out.println("testLinks(): rels: " + rels);
        System.out.println("testLinks(): rowVarMaxArities: " + rowVarMaxArities);
        System.out.println("testLinks(): result: " + rowVarMaxArities.get("@ARGS").intValue());
        System.out.println("testLinks(): expected: " + 3);
        if (3 == rowVarMaxArities.get("@ARGS").intValue())
            System.out.println("testLinks(): success!");
        else
            System.out.println("testLinks(): failure");
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
        ArrayList<Formula> results = RowVars.expandRowVars(kb,f);
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
            System.out.println("testLinks2(): failure");
        assertEquals(expected,result);
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
        ArrayList<Formula> results = RowVars.expandRowVars(kb,f);
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