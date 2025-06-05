package com.articulate.sigma.parsing;

import com.articulate.sigma.IntegrationTestBase;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Test;

public class TypeTest extends IntegrationTestBase {

    static FormulaAST f;
    static SuokifVisitor visitor;

    @After
    public void afterTest() {
        f = null;
        visitor = null;
    }

    /***************************************************************
     * */
    public static String process(String input, String expected) {

        String result = null;
        System.out.println("Input: " + input);
        visitor = SuokifVisitor.parseString(input);
        if (visitor.errors.isEmpty()) {
            Map<Integer,FormulaAST> hm = SuokifVisitor.result;
            VarTypes vt = new VarTypes(hm.values(),kb);
            vt.findTypes();
            f = hm.values().iterator().next();
            f.printCaches();
            result = f.varTypes.toString().trim();
            System.out.println("Result: " + result);
            System.out.println("expected: " + expected);
            if (result.equals(expected))
                System.out.println("Success");
            else
                System.err.println("FAIL");
        }
        return result;
    }

    /** ***************************************************************
     */
    @Test
    public void test1() {

        System.out.println("===================== TypeTest.test1() =====================");
        String input = "(=> (and (minValue ?R ?ARG ?N) (?R @ARGS) (equal ?VAL (ListOrderFn (ListFn @ARGS) ?ARG))) (greaterThan ?VAL ?N))";
        String expected = "{?R=[Predicate], ?ARG=[Integer, PositiveInteger], ?N=[RealNumber, Quantity], ?VAL=[RealNumber, Entity]}";
        String result = process(input,expected);
        assertEquals(expected,result);
    }

    /** ***************************************************************
     */
    @Test
    public void test2() {

        System.out.println("===================== TypeTest.test2() =====================");
        String input = "(<=>\n" +
                "  (instance ?OBJ SelfConnectedObject)\n" +
                "  (forall (?PART1 ?PART2)\n" +
                "    (=>\n" +
                "      (equal ?OBJ (MereologicalSumFn ?PART1 ?PART2))\n" +
                "      (connected ?PART1 ?PART2))))";
        String expected = "{?OBJ=[SelfConnectedObject, Entity, Object], ?PART2=[Object], ?PART1=[Object]}";
        String result = process(input,expected);
        assertEquals(expected,result);
    }

    /** ***************************************************************
     */
    @Test
    public void test3() {

        System.out.println("===================== TypeTest.test3() =====================");
        String input = "(=>\n" +
                "  (and\n" +
                "    (maxValue ?REL ?ARG ?N)\n" +
                "    (?REL @ARGS)\n" +
                "    (equal ?VAL\n" +
                "      (ListOrderFn\n" +
                "        (ListFn @ARGS) ?ARG)))\n" +
                "  (greaterThan ?N ?VAL))\n";
        String expected = "{?REL=[Predicate], ?ARG=[Integer, PositiveInteger], ?N=[RealNumber, Quantity], ?VAL=[RealNumber, Entity]}";
        String result = process(input,expected);
        assertEquals(expected,result);
    }

    /** ***************************************************************
     * Syntax violation
     */
    @Test
    public void syntaxViolation() {

        System.out.println("===================== TypeTest.syntaxViolation() =====================");
        String input = "(=>\n" +
                "  (and\n" +
                "    (maxValue ?REL ?ARG ?N)\n" +
                "    (?REL @ARGS)\n" +
                "    (equal ?VAL\n" +
                "      (ListOrderFn\n" +
                "        (ListFn (@ARGS)) ?ARG)))\n" + // arg in parens
                "  (greaterThan ?N ?VAL))\n";
        String expected = "{?REL=[Predicate], ?ARG=[Integer, PositiveInteger], ?N=[RealNumber, Quantity], ?VAL=[RealNumber, Entity]}";
        String result = process(input,expected);
        assertNull(result);
        assertFalse(visitor.errors.isEmpty());
    }

    /** ***************************************************************
     * Syntax violation
     */
    @Test
    public void syntaxViolation2() {

        System.out.println("===================== TypeTest.syntaxViolation2() =====================");
        String input = "(=>\n" +
                       "  (and\n" +
                       "    (instance ?STH2 Physical)\n" +
                       "    (instance ?FW Following)\n" +
                       "    (patient ?FW ?STH2))\n" +
                       "  (exists (?STH1 ?T1 ?T2)\n" +
                       "    (and\n" +
                       "      (instance ?STH1 Physical)\n" +
                       "      (holdsDuring ?T1 WhenFn (?STH1))\n" + // arg in parens
                       "      (holdsDuring ?T2 WhenFn (?STH2))\n" + // arg in parens
                       "      (earlier ?T1 ?T2))))\n";
        String expected = "{?FW=[Following], ?STH2=[Physical], ?STH1=[Physical, Entity]}";
        String result = process(input,expected);
        assertNull(result);
        assertFalse(visitor.errors.isEmpty());
    }
}
