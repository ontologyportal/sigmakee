package com.articulate.sigma.parsing;

import com.articulate.sigma.IntegrationTestBase;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.After;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RowVarTest extends IntegrationTestBase {

    static SuokifVisitor visitor;
    static Sortals sortals;
    static VarTypes vt;
    static RowVar rv;

    @After
    public void afterClass() {
        visitor = null;
        sortals = null;
        vt = null;
        rv = null;
    }

    /***************************************************************
     * */
    public static Set<FormulaAST> process(String input) {

        System.out.println("RowVarTest.process(): " + input);
        visitor = SuokifVisitor.parseString(input);
        Map<Integer,FormulaAST> hm = SuokifVisitor.result;
        if (!visitor.hasPredVar.isEmpty()) {
            System.err.println("Error - can't have tests with pred vars in this routine.");
            return null;
        }
//        PredVarInst pvi = new PredVarInst(kb);
        sortals = new Sortals(kb);
        vt = new VarTypes(hm.values(),kb);
        vt.findTypes();
        for (FormulaAST f : visitor.rules) {
            //System.out.println("RowVarTest.process(): before winnow");
            //f.printCaches();
            sortals.winnowAllTypes(f);
            //System.out.println("RowVarTest.process(): after winnow");
            //f.printCaches();
        }
        PredVarInst.predVarInstDone = true; // because the test formulas already did it
        rv = new RowVar(kb);
        Set<FormulaAST> rvs = rv.expandRowVar(visitor.hasRowVar);
        //System.out.println("RowVarTest.process(): rvs" + rvs);
        return rvs;
    }

    /** ***************************************************************
     */
    @Test
    public void test1() {

        System.out.println("===================== RowVarTest.test1() =====================");
                String input = "\n" +
                "(=>\n" +
                "    (and\n" +
                "        (contraryAttribute @ROW1)\n" +
                "        (identicalListItems\n" +
                "            (ListFn @ROW1)\n" +
                "            (ListFn @ROW2)))\n" +
                "    (contraryAttribute @ROW2))";

        Set<FormulaAST> hm = process(input);
        System.out.println("RowVarTest.test1(): one result: " + hm.iterator().next());
        System.out.println("RowVarTest.test1(): result size: " + hm.size());
        System.out.println("RowVarTest.test1(): expected size: " + 49);
        assertEquals(49,hm.size());
        System.out.println();
    }

    /** ***************************************************************
     */
    @Test
    public void test2() {

        System.out.println("===================== RowVarTest.test2() =====================");
        String input = "(=> (and (minValue part ?ARG ?N) (part @ARGS) (equal ?VAL (ListOrderFn (ListFn @ARGS) ?ARG))) (greaterThan ?VAL ?N))";
        String expected = "(=> (and (minValue part ?ARG ?N) (part ?ARGS1 ?ARGS2) (equal ?VAL (ListOrderFn (ListFn_2Fn ?ARGS1 ?ARGS2) ?ARG))) (greaterThan ?VAL ?N))";
        Set<FormulaAST> hm = process(input);
        StringBuilder sb = new StringBuilder();
        for (FormulaAST f : hm) {
            f.printCaches();
            sb.append(f.getFormula()).append("\n");
        }
        System.out.println("RowVarTest.test2(): result: " + sb);
        assertEquals(expected,sb.toString().trim());
        System.out.println();
    }

    /** ***************************************************************
     */
    @Test
    public void test3() {

        System.out.println("===================== RowVarTest.test3() =====================");
        String input = "(forall (@ROW ?ITEM)\n" +
                "    (equal\n" +
                "        (ListLengthFn\n" +
                "            (ListFn @ROW ?ITEM))\n" +
                "        (SuccessorFn\n" +
                "            (ListLengthFn\n" +
                "                (ListFn @ROW)))))";

        Set<FormulaAST> hm = process(input);
        System.out.println("RowVarTest.test3(): result size: " + hm.size());
        System.out.println("RowVarTest.test3(): expected size: " + 7);
        System.out.println("RowVarTest.test3(): result size: " + hm);
        Set<String> results = new HashSet<>();
        for (FormulaAST f : hm) {
            results.add(f.toString());
            if (f.getFormula().contains("@")) {
                System.out.println("RowVarTest.test3(): shouldn't contain row variable " + f);
                assertTrue(false);
            }
        }

        String expected = "(forall (?ROW1 ?ROW2 ?ITEM)\n" +
                "  (equal\n" +
                "    (ListLengthFn\n" +
                "      (ListFn_3Fn ?ROW1 ?ROW2 ?ITEM))\n" +
                "    (SuccessorFn\n" +
                "      (ListLengthFn\n" +
                "        (ListFn_2Fn ?ROW1 ?ROW2)))))";
        assertTrue(results.contains(expected));
        System.out.println();
    }
}
