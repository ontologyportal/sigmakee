package com.articulate.sigma.parsing;

import com.articulate.sigma.IntegrationTestBase;

import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PredVarInstTest extends IntegrationTestBase {

    static PredVarInst pvi;
    @After
    public void afterClass() {
        pvi = null;
    }

    /** *************************************************************
     */
    private static int process(String input) {

        System.out.println("PredVarInstTest Input: " + input);
        SuokifVisitor.parseString(input);
        Map<Integer, FormulaAST> hm = SuokifVisitor.result;
        VarTypes vt = new VarTypes(hm.values(),kb);
        vt.findTypes();
        FormulaAST f = hm.values().iterator().next();
        f.printCaches();
        Sortals s = new Sortals(kb);
        System.out.println("PredVarInstTest.process(): varTypes:              " + f.varTypes);
        s.winnowAllTypes(f);
        System.out.println("PredVarInstTest.process(): varTypes after winnow: " + f.varTypes);
        String form = s.addSortals(f);
        f.setFormula(form);
        pvi = new PredVarInst(kb);
        Set<FormulaAST> result = pvi.processOne(f);

        //Formula resultf = new Formula(result);
        if (result.size() < 10)
            System.out.println("PredVarInstTest: Result: " + result);
        else {
            System.out.println("PredVarInstTest: Result too big to show ");
        }
        System.out.println("PredVarInstTest: # formulas : " + result.size());
        return result.size();
    }

    /** ***************************************************************
     */
    @Test
    public void test1() {

        System.out.println("===================== PredVarInstTest.test1() =====================");
        String input = "(=> (and (minValue ?R ?ARG ?N) (?R @ARGS) (equal ?VAL (ListOrderFn (ListFn @ARGS) ?ARG))) (greaterThan ?VAL ?N))";
        int result = process(input);
        System.out.println("PASSED: " + (result >= 555 && result <= 560));
        assert(result >= 555 && result <= 560);
    }

    /** ***************************************************************
     */
    @Test
    public void test2(){

        System.out.println("===================== PredVarInstTest.test2() =====================");
        String input = "(<=>\n" +
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
        int result = process(input);
        assertEquals(99,result);
    }

    /** ***************************************************************
     */
    @Test
    public void test3() {

        System.out.println("===================== PredVarInstTest.test3() =====================");
        String input = "\n" +
                "(=>\n" +
                "    (and\n" +
                "        (exhaustiveAttribute ?CLASS @ROW)\n" +
                "        (inList ?ATTR\n" +
                "            (ListFn @ROW)))\n" +
                "    (instance ?ATTR ?CLASS))";
        int result = process(input);
        assertEquals(1,result); // there should be no substitutions
    }

    /** ***************************************************************
     */
    @Test
    public void test4() {

        System.out.println("===================== PredVarInstTest.test4() =====================");
        String input = "(=>\n" +
                "  (and\n" +
                "    (maxValue ?REL ?ARG ?N)\n" +
                "    (?REL @ARGS)\n" +
                "    (equal ?VAL\n" +
                "      (ListOrderFn\n" +
                "        (ListFn @ARGS) ?ARG)))\n" +
                "  (greaterThan ?N ?VAL))";
        int result = process(input);
        System.out.println("PASSED: " + (result >= 555 && result <= 560));
        assert(result >= 555 && result <= 560);
    }


    /** ***************************************************************
     */
    @Test
    public void test5() {

        System.out.println("===================== PredVarInstTest.test5() =====================");
        String input = "\n" +
                "(=>\n" +
                "    (and\n" +
                "        (instance ?REL1 Predicate)\n" +
                "        (instance ?REL2 Predicate)\n" +
                "        (disjointRelation ?REL1 ?REL2)\n" +
                "        (?REL1 @ROW2))\n" +
                "    (not\n" +
                "        (?REL2 @ROW2)))";
//        String input = com.articulate.sigma.PredVarInst.DOUBLE_PREDICATE_AXIOM; // TODO: won't process (Error in Vartypes.findTypeOfTerm(): signature Class doesn't allow Predicate)
        int result = process(input);
        System.out.println("PASSED: " + (result > 308000 && result < 309000));
        assert(result > 308000 && result < 309000);
    }
}
