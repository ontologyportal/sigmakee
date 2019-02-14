package com.articulate.sigma.trans;

import com.articulate.sigma.FormulaPreprocessor;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.articulate.sigma.*;
import org.junit.Test;
import org.junit.Before;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;

/**
 */
public class SUMOtoTFATest extends UnitTestBase {

    /** ***************************************************************
     */
    @BeforeClass
    public static void init() {

        System.out.println("============ SUMOtoTFATest.init()");
        SUMOtoTFAform.initOnce();
        SUMOKBtoTFAKB skbtfakb = new SUMOKBtoTFAKB();
        skbtfakb.initOnce();
        String filename = KBmanager.getMgr().getPref("kbDir") + File.separator + "SUMO.tff";
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter(filename));
            skbtfakb.writeSorts(pw,filename);
            pw.flush();
            pw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        SUMOtoTFAform.setNumericFunctionInfo();
    }

    /** ***************************************************************
     */
    @Test
    public void test1() {

        System.out.println("\n========= test 1 ==========\n");
        String kifstring, expectedRes, actualRes;
        kifstring = "(instance Foo Bar)";
        expectedRes = "s__instance(s__Foo, s__Bar)";
        actualRes = SUMOtoTFAform.process(kifstring);
        System.out.println("actual:" + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void test2() {

        System.out.println("\n========= test 2 ==========\n");
        String kifstring, expectedRes, actualRes;
        kifstring = "(forall (?X) (=> (instance ?X Human) (attribute ?X Mortal)))";
        expectedRes = "! [V__X:$i] : (s__instance(V__X, s__Human) => s__attribute(V__X, s__Mortal))";
        actualRes = SUMOtoTFAform.process(kifstring);
        System.out.println("actual:" + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void test3() {

        System.out.println("\n========= test 3 ==========\n");
        String kifstring, expectedRes, actualRes;
        kifstring = "(=>\n" +
                "    (and\n" +
                "        (subProcess ?S1 ?P)\n" +
                "        (subProcess ?S2 ?P))\n" +
                "    (relatedEvent ?S1 ?S2))";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        if (forms == null || forms.size() == 0)
            return;
        expectedRes = "! [V__P : $i,V__S1 : $i,V__S2 : $i] : " +
                "(s__subProcess(V__S1, V__P) & s__subProcess(V__S2, V__P) => " +
                "s__relatedEvent(V__S1, V__S2))";
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        System.out.println("actual:" + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void test4() {

        System.out.println("\n========= test 4 ==========\n");
        String kifstring, expectedRes, actualRes;
        kifstring = "(=>\n" +
                "    (and\n" +
                "        (instance ?DEV ElectricDevice)\n" +
                "        (instance ?EV Process)\n" +
                "        (instrument ?EV ?DEV))\n" +
                "    (exists (?R)\n" +
                "        (and\n" +
                "            (instance ?R Electricity)\n" +
                "            (resource ?EV ?R))))";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        if (forms == null || forms.size() == 0)
            return;
        expectedRes = "! [V__EV : $i,V__DEV : $i] : " +
                "(s__instance(V__DEV, s__ElectricDevice) & s__instance(V__EV, s__Process) & s__instrument(V__EV, V__DEV) =>  " +
                "? [V__R:$i] : (s__instance(V__R, s__Electricity) & s__resource(V__EV, V__R)))";
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        System.out.println("actual:" + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void test5() {

        System.out.println("\n========= test 5 ==========\n");
        FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(=>\n" +
                "    (and\n" +
                "        (instance ?PROC Process)\n" +
                "        (eventLocated ?PROC ?LOC)\n" +
                "        (subProcess ?SUB ?PROC))\n" +
                "    (eventLocated ?SUB ?LOC))";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        if (forms == null || forms.size() == 0)
            return;
        expectedRes = "! [V__PROC : $i,V__LOC : $i,V__SUB : $i] : " +
                "(s__instance(V__PROC, s__Process) & s__eventLocated(V__PROC, V__LOC) & s__subProcess(V__SUB, V__PROC) => " +
                "s__eventLocated(V__SUB, V__LOC))";
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        System.out.println("actual:" + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void test6() {

        System.out.println("\n========= test 6 ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(=> (and (equal (PathWeightFn ?PATH) ?SUM) (graphPart ?ARC1 ?PATH) " +
                "(graphPart ?ARC2 ?PATH) (arcWeight ?ARC1 ?NUMBER1) (arcWeight ?ARC2 ?NUMBER2) " +
                "(forall (?ARC3) (=> (graphPart ?ARC3 ?PATH) (or (equal ?ARC3 ?ARC1) (equal ?ARC3 ?ARC2))))) " +
                "(equal (PathWeightFn ?PATH) (AdditionFn ?NUMBER1 ?NUMBER2)))\n";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        if (forms == null || forms.size() == 0)
            return;
        expectedRes = "! [V__SUM : $i,V__NUMBER1 : $i,V__NUMBER2 : $i,V__ARC1 : $i,V__PATH : $i,V__ARC2 : $i] : " +
                "(equal(s__PathWeightFn(V__PATH) ,V__SUM) & s__graphPart(V__ARC1, V__PATH) & " +
                "s__graphPart(V__ARC2, V__PATH) & s__arcWeight(V__ARC1, V__NUMBER1) & s__arcWeight(V__ARC2, V__NUMBER2) &  " +
                "! [V__ARC3:$i] : (s__graphPart(V__ARC3, V__PATH) => " +
                "equal(V__ARC3 ,V__ARC1) | equal(V__ARC3 ,V__ARC2)) => " +
                "equal(s__PathWeightFn(V__PATH) ,AdditionFn(V__NUMBER1 ,V__NUMBER2)))";
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        System.out.println("actual:" + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void test7() {

        System.out.println("\n========= test 7 ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(exists (?ARC1 ?ARC2 ?PATH) (and (graphPart ?ARC1 ?PATH) " +
                "(graphPart ?ARC2 ?PATH) (arcWeight ?ARC1 ?NUMBER1)))\n";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        if (forms == null || forms.size() == 0)
            return;
        expectedRes = "! [V__NUMBER1 : $i] : ( ? [V__ARC1:$i, V__ARC2:$i, V__PATH:$i] : " +
                "(s__graphPart(V__ARC1, V__PATH) & s__graphPart(V__ARC2, V__PATH) & s__arcWeight(V__ARC1, V__NUMBER1)))";
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        System.out.println("actual:" + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void test8() {

        System.out.println("\n========= test 8 ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(<=> \n" +
                "  (and \n" +
                "    (instance greaterThan TotalValuedRelation) \n" +
                "    (instance greaterThan Predicate)) \n" +
                "  (exists (?VALENCE) \n" +
                "    (and \n" +
                "      (instance greaterThan Relation) \n" +
                "      (valence greaterThan ?VALENCE) \n" +
                "      (=> \n" +
                "        (forall (?NUMBER ?ELEMENT ?CLASS) \n" +
                "          (=> \n" +
                "            (and \n" +
                "              (lessThan ?NUMBER ?VALENCE) \n" +
                "              (domain greaterThan ?NUMBER ?CLASS) \n" +
                "              (equal ?ELEMENT \n" +
                "                (ListOrderFn (ListFn @ROW) ?NUMBER))) \n" +
                "            (instance ?ELEMENT ?CLASS))) \n" +
                "        (exists (?ITEM) \n" +
                "          (greaterThan @ROW ?ITEM))))))";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        if (forms == null || forms.size() == 0)
            return;
        expectedRes = "! [V__NUMBER1 : $i] : ( ? [V__ARC1:$i, V__ARC2:$i, V__PATH:$i] : " +
                "(s__graphPart(V__ARC1, V__PATH) & s__graphPart(V__ARC2, V__PATH) & s__arcWeight(V__ARC1, V__NUMBER1)))";
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        System.out.println("actual:" + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }
}
