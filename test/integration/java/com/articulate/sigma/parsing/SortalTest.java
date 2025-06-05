package com.articulate.sigma.parsing;

import com.articulate.sigma.Formula;
import com.articulate.sigma.IntegrationTestBase;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.After;

import static org.junit.Assert.assertEquals;

public class SortalTest extends IntegrationTestBase {

    static Sortals s;

    @After
    public void afterClass() {
        s = null;
    }

    /***************************************************************
     * */
    public static String process(String input, String expected) {

        System.out.println("Input: " + input);
        SuokifVisitor.parseString(input);
        Map<Integer,FormulaAST> hm = SuokifVisitor.result;
        VarTypes vt = new VarTypes(hm.values(),kb);
        vt.findTypes();
        FormulaAST f = hm.values().iterator().next();
        f.printCaches();
        s = new Sortals(kb);
        s.winnowAllTypes(f);
        String result = s.addSortals(f);
        Formula resultf = new Formula(result);
        System.out.println("Result: " + resultf);
        Formula expectedf = new Formula(expected);
        System.out.println("expected: " +expectedf);
        if (resultf.equals(expectedf))
            System.out.println("Success");
        else
            System.err.println("FAIL");
        return result;
    }

    /** ***************************************************************
     */
    @Test
    public void test1() {

        System.out.println("===================== SortalTest.test1() =====================");
        String input = "(=> (and (minValue ?R ?ARG ?N) (?R @ARGS) (equal ?VAL (ListOrderFn (ListFn @ARGS) ?ARG))) (greaterThan ?VAL ?N))";
        String expected = "(=> " +
                "(and " +
                  "(instance ?R Predicate)\n" +
                "    (instance ?ARG PositiveInteger)\n" +
                "    (instance ?N RealNumber)\n" +
                "    (instance ?VAL RealNumber)) " +
                  "(=> " +
                    "(and " +
                    "(minValue ?R ?ARG ?N) " +
                    "(?R @ARGS) " +
                    "(equal ?VAL (ListOrderFn (ListFn @ARGS) ?ARG))) " +
                    "(greaterThan ?VAL ?N)))";
        String result = process(input,expected);
        assertEquals(new Formula(expected),new Formula(result));
    }

    /** ***************************************************************
     */
    @Test
    public void test2() {

        System.out.println("===================== SortalTest.test2() =====================");
        String input = "(<=>\n" +
                "  (instance ?OBJ SelfConnectedObject)\n" +
                "  (forall (?PART1 ?PART2)\n" +
                "    (=>\n" +
                "      (equal ?OBJ (MereologicalSumFn ?PART1 ?PART2))\n" +
                "      (connected ?PART1 ?PART2))))";
        String expected = "(=> " +
                "(and " +
                  "(instance ?PART2 Object) " +
                  "(instance ?PART1 Object)) " +
                "(<=> " +
                  "(instance ?OBJ SelfConnectedObject) " +
                  "(forall (?PART1 ?PART2) " +
                    "(=> " +
                      "(equal ?OBJ (MereologicalSumFn ?PART1 ?PART2)) " +
                      "(connected ?PART1 ?PART2)))))";
        String result = process(input,expected);
        assertEquals(new Formula(expected),new Formula(result));

    }

    /** ***************************************************************
     */
    @Test
    public void test3() {

        System.out.println("===================== SortalTest.test3() =====================");
        String input = "(=>\n" +
                "  (and\n" +
                "    (valence identityElement ?NUMBER)\n" +
                "    (instance identityElement Predicate))\n" +
                "  (forall (?ROW1 ?ROW2)\n" +
                "    (=>\n" +
                "      (identityElement ?ROW1 ?ROW2)\n" +
                "      (equal\n" +
                "        (ListLengthFn\n" +
                "          (ListFn_2 ?ROW1 ?ROW2)) ?NUMBER))))";
        String expected = "(=>     " +
                "    (and" +
                "      (instance ?ROW1 BinaryFunction)\n" +
                "      (instance ?ROW2 Integer)" +
                "      (instance ?NUMBER PositiveInteger))" +
                "(=>\n" +
                "  (and\n" +
                "    (valence identityElement ?NUMBER)\n" +
                "    (instance identityElement Predicate))\n" +
                "  (forall (?ROW1 ?ROW2)\n" +
                "    (=>\n" +
                "      (identityElement ?ROW1 ?ROW2)\n" +
                "      (equal\n" +
                "        (ListLengthFn\n" +
                "          (ListFn_2 ?ROW1 ?ROW2)) ?NUMBER)))))";
        String result = process(input,expected);
        assertEquals(new Formula(expected),new Formula(result));

    }

    /** ***************************************************************
     */
    @Test
    public void elimTypes() {

        System.out.println("===================== SortalTest.elimTypes() =====================");
        String input = "\n" +
                "(<=>\n" +
                "    (and\n" +
                "        (instance ?REL TotalValuedRelation)\n" +
                "        (instance ?REL Predicate))\n" +
                "    (exists (?VALENCE)\n" +
                "        (and\n" +
                "            (instance ?REL Relation)\n" +
                "            (valence ?REL ?VALENCE)\n" +
                "            (=>\n" +
                "                (forall (?NUMBER ?ELEMENT ?CLASS)\n" +
                "                    (=>\n" +
                "                        (and\n" +
                "                            (lessThan ?NUMBER ?VALENCE)\n" +
                "                            (domain ?REL ?NUMBER ?CLASS)\n" +
                "                            (equal ?ELEMENT\n" +
                "                                (ListOrderFn\n" +
                "                                    (ListFn @ROW) ?NUMBER)))\n" +
                "                        (instance ?ELEMENT ?CLASS)))\n" +
                "                (exists (?ITEM)\n" +
                "                    (?REL @ROW ?ITEM))))))";

        System.out.println("Input: " + input);
        SuokifVisitor.parseString(input);
        Map<Integer,FormulaAST> hm = SuokifVisitor.result;
        VarTypes vt = new VarTypes(hm.values(),kb);
        vt.findTypes();
        FormulaAST f = hm.values().iterator().next();
        f.printCaches();
        s = new Sortals(kb);
        s.elimSubsumedTypes(f);
        Set<String> expected = new HashSet<>();
        expected.add("TotalValuedRelation");
        expected.add("Predicate");
        Set<String> actual = f.varTypes.get("?REL");
        System.out.println("SortalTest.elimTypes(): expected: " + expected);
        System.out.println("SortalTest.elimTypes(): actual: " + actual);
        if (expected.equals(actual))
            System.out.println("SortalTest.elimTypes(): success");
        else
            System.err.println("SortalTest.elimTypes(): fail");
        assertEquals(expected,actual);
    }
}
