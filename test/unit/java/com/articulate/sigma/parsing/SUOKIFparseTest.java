package com.articulate.sigma.parsing;

import com.articulate.sigma.Formula;
import com.articulate.sigma.UnitTestBase;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SUOKIFparseTest extends UnitTestBase {

    public static Map<Integer,FormulaAST> process(String input) {

        System.out.println("process(): input: " + input);
        SuokifVisitor.parseString(input);
        //System.out.println("process(): visitor: " + visitor);
        //System.out.println("process(): visitor.result (before processing): " + visitor.result);
        Map<Integer,FormulaAST> hm = SuokifVisitor.result;
        System.out.println("process(): result: " + hm);
        return hm;
    }

    /** ***************************************************************
     */
    @Test
    public void test1() {

        System.out.println("===================== SUOKIFparseTest.test1() =====================");
        String input = "(likes John Mary)\n; and here's a comment\n(part Wheel1 Car2)\n";
        String expected = "(likes John Mary)\n(part Wheel1 Car2)\n";

        Map<Integer,FormulaAST> hm = process(input);
        StringBuilder sb = new StringBuilder();
        for (Formula f : hm.values()) {
            f.printCaches();
            sb.append(f.getFormula()).append("\n");
        }
        System.out.println("result: " + sb);
        if (expected.trim().equals(sb.toString().trim()))
            System.out.println("test1(): success!");
        else
            System.err.println("test1(): fail!");
        assertEquals(expected.trim(),sb.toString().trim());
        System.out.println();
    }

    /** ***************************************************************
     */
    @Test
    public void test2() {

        System.out.println("===================== SUOKIFparseTest.test2() =====================");
        String input = "(=> (and (minValue ?R ?ARG ?N) (?R @ARGS) (equal ?VAL (ListOrderFn (ListFn @ARGS) ?ARG))) (greaterThan ?VAL ?N))";
        Map<Integer,FormulaAST> hm = process(input);
        StringBuilder sb = new StringBuilder();
        for (Formula f : hm.values()) {
            f.printCaches();
            sb.append(f.getFormula()).append(" ");
        }
        System.out.println("result: " + sb);
        if (input.equals(sb.toString().trim()))
            System.out.println("test2(): success!");
        else
            System.err.println("test2(): fail!");
        assertEquals(input,sb.toString().trim());
        System.out.println();
    }

    /** ***************************************************************
     */
    @Test
    public void test3() {

        System.out.println("===================== SUOKIFparseTest.test3() =====================");
        String input = "(=> (and (exhaustiveAttribute ?CLASS @ROW) (inList NonFullyFormed (ListFn @ROW))) (instance NonFullyFormed Attribute))";
        Map<Integer,FormulaAST> hm = process(input);
        StringBuilder sb = new StringBuilder();
        for (Formula f : hm.values()) {
            //f.printCaches();
            sb.append(f.getFormula()).append(" ");
        }
        System.out.println("result: " + sb);
        if (input.equals(sb.toString().trim()))
            System.out.println("test3(): success!");
        else
            System.err.println("test3(): fail!");
        assertEquals(input,sb.toString().trim());
        System.out.println();
    }

    /** ***************************************************************
     */
    @Test
    public void predRowUnder() {

        System.out.println("===================== SUOKIFparseTest.predRowUnder() =====================");
        String input = "(=> (and (exhaustiveAttribute ?CLASS ?ROW1) (inList ?ATTR (ListFn_1Fn ?ROW1))) (instance ?ATTR Attribute))";
        Map<Integer,FormulaAST> hm = process(input);
        FormulaAST f = hm.values().iterator().next();

        f.printCaches();
        if (f.predVarCache.isEmpty())
            System.out.println("predRowUnder(): success!");
        else
            System.err.println("predRowUnder(): fail!");
        assertEquals(0,f.predVarCache.size());

        if (f.rowVarCache.isEmpty())
            System.out.println("predRowUnder(): success!");
        else
            System.err.println("predRowUnder(): fail!");
        assertEquals(0,f.rowVarCache.size());
        System.out.println();
    }

    /** ***************************************************************
     */
    @Test
    public void predRow() {

        System.out.println("===================== SUOKIFparseTest.predRow() =====================");
        String input = "(=> (and (exhaustiveAttribute ?CLASS @ROW) (inList ?ATTR (ListFn @ROW))) (instance ?ATTR Attribute))";
        Map<Integer,FormulaAST> hm = process(input);
        FormulaAST f = hm.values().iterator().next();

        f.printCaches();

        if (f.predVarCache.isEmpty())
            System.out.println("predRow(): success!");
        else
            System.err.println("predRow(): fail!");
        assertEquals(0,f.predVarCache.size());
        if (1 == f.rowVarCache.size())
            System.out.println("predRow(): success!");
        else
            System.err.println("predRow(): fail!");
        assertEquals(1,f.rowVarCache.size());
        System.out.println();
    }

    /** ***************************************************************
     */
    @Test
    public void withSortals() {

        System.out.println("===================== SUOKIFparseTest.withSortals() =====================");
        String input = "(=> (and (instance ?REL Predicate) (instance ?ARG PositiveInteger) (instance ?N Quantity) (instance ?VAL Quantity)) " +
                "(=> (and (maxValue subclass ?ARG ?N) (subclass @ARGS) (equal ?VAL (ListOrderFn (ListFn @ARGS) ?ARG))) (greaterThan ?N ?VAL)))";
        Map<Integer,FormulaAST> hm = process(input);
        FormulaAST f = hm.values().iterator().next();

        f.printCaches();
        if (f.predVarCache.isEmpty())
            System.out.println("withSortals(): success!");
        else
            System.err.println("withSortals(): fail!");
        assertEquals(0,f.predVarCache.size());
        if (1 == f.rowVarCache.size())
            System.out.println("withSortals(): success!");
        else
            System.err.println("withSortals(): fail!");
        assertEquals(1,f.rowVarCache.size());
        System.out.println();
    }

    /** ***************************************************************
     */
    @Test
    public void hasNumber() {

        System.out.println("===================== SUOKIFparseTest.hasNumber() =====================");
        String input = "(=>\n" +
                "  (instance ?MORNING Morning)\n" +
                "  (exists (?HOUR)\n" +
                "    (and\n" +
                "      (instance ?HOUR\n" +
                "        (HourFn 12 ?DAY))\n" +
                "      (finishes ?HOUR ?MORNING))))";
        Map<Integer,FormulaAST> hm = process(input);
        FormulaAST f = hm.values().iterator().next();
        if (f.containsNumber)
            System.out.println("hasNumber(): success!");
        else
            System.err.println("hasNumber(): fail!");
        assertTrue(f.containsNumber);
        System.out.println();
    }

    /** ***************************************************************
     */
    @Test
    public void testXor() {

        System.out.println("===================== SUOKIFparseTest.testXor() =====================");
        String input = "(=>\n" +
                        "  (connected ?OBJ1 ?OBJ2)\n" +
                        "  (xor\n" +
                        "    (overlapsSpatially ?OBJ1 ?OBJ2)))"; // incorrect # of args (only one here)
        Map<Integer,FormulaAST> hm = process(input);
//        FormulaAST f = hm.values().iterator().next();
        if (hm.isEmpty())
            System.out.println("success!");
        else
            System.err.println("fail!");
        assertTrue(hm.isEmpty());
        System.out.println();
    }
}