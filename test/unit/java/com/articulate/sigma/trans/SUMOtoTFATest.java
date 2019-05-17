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

import static org.junit.Assert.*;
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
        System.out.println("actual:  " + actualRes);
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
        expectedRes = "( ! [V__X:$i] : (s__instance(V__X, s__Human) => s__attribute(V__X, s__Mortal)))";
        actualRes = SUMOtoTFAform.process(kifstring);
        System.out.println("actual:  " + actualRes);
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
                "((s__subProcess(V__S1, V__P) & s__subProcess(V__S2, V__P)) => " +
                "s__relatedEvent(V__S1, V__S2))";
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        System.out.println("actual:  " + actualRes);
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
                "((s__instance(V__DEV, s__ElectricDevice) & s__instance(V__EV, s__Process) & " +
                "s__instrument(V__EV, V__DEV)) => " +
                "( ? [V__R:$i] : ((s__instance(V__R, s__Electricity) & s__resource(V__EV, V__R)))))";
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        System.out.println("actual:  " + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void test5() {

        System.out.println("\n========= test 5 ==========\n");
        //FormulaPreprocessor.debug = true;
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
                "((s__instance(V__PROC, s__Process) & s__eventLocated(V__PROC, V__LOC) & " +
                "s__subProcess(V__SUB, V__PROC)) => " +
                "s__eventLocated(V__SUB, V__LOC))";
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        System.out.println("actual:  " + actualRes);
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
                "((equal(s__PathWeightFn(V__PATH) ,V__SUM) & s__graphPart(V__ARC1, V__PATH) & " +
                "s__graphPart(V__ARC2, V__PATH) & s__arcWeight(V__ARC1, V__NUMBER1) & s__arcWeight(V__ARC2, V__NUMBER2) & " +
                "( ! [V__ARC3:$i] : (s__graphPart(V__ARC3, V__PATH) => " +
                "(equal(V__ARC3 ,V__ARC1) | equal(V__ARC3 ,V__ARC2))))) => " +
                "equal(s__PathWeightFn(V__PATH) ,s__AdditionFn(V__NUMBER1 ,V__NUMBER2)))";
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        System.out.println("actual:  " + actualRes);
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
        expectedRes = "! [V__NUMBER1 : $i] : (( ? [V__ARC1:$i, V__ARC2:$i, V__PATH:$i] : " +
                "((s__graphPart(V__ARC1, V__PATH) & s__graphPart(V__ARC2, V__PATH) & " +
                "s__arcWeight(V__ARC1, V__NUMBER1)))))";
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        System.out.println("actual:  " + actualRes);
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
        expectedRes = "! [V__ROW1 : $i] : (((s__instance(s__greaterThan__m, s__TotalValuedRelation) & " +
                "s__instance(s__greaterThan__m, s__Predicate)) => ( ? [V__VALENCE:$int] : " +
                "((s__instance(s__greaterThan__m, s__Relation) & " +
                "s__valence__2In(s__greaterThan__m, V__VALENCE) & " +
                "( ! [V__NUMBER:$int, V__ELEMENT:$i, V__CLASS:$i] : " +
                "(($less(V__NUMBER ,V__VALENCE) & s__domain__2In(s__greaterThan__m, V__NUMBER, V__CLASS) & " +
                "equal(V__ELEMENT ,s__ListOrderFn__2InFn(s__ListFn_1(V__ROW1), V__NUMBER))) => " +
                "s__instance(V__ELEMENT, V__CLASS))) => ( ? [V__ITEM:$i] : " +
                "(greaterThan(V__ROW1 ,V__ITEM))))))) & (( ? [V__VALENCE:$int] : " +
                "((s__instance(s__greaterThan__m, s__Relation) & s__valence__2In(s__greaterThan__m, V__VALENCE) & " +
                "( ! [V__NUMBER:$int, V__ELEMENT:$i, V__CLASS:$i] : (($less(V__NUMBER ,V__VALENCE) & " +
                "s__domain__2In(s__greaterThan__m, V__NUMBER, V__CLASS) & " +
                "equal(V__ELEMENT ,s__ListOrderFn__2InFn(s__ListFn_1(V__ROW1), V__NUMBER))) => " +
                "s__instance(V__ELEMENT, V__CLASS))) => ( ? [V__ITEM:$i] : " +
                "(greaterThan(V__ROW1 ,V__ITEM)))))) => (s__instance(s__greaterThan__m, s__TotalValuedRelation) & " +
                "s__instance(s__greaterThan__m, s__Predicate))))";
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        System.out.println("actual:  " + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void test9() {

        System.out.println("\n========= test 9 ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(=> (and (instance ?INT1 Integer) (instance ?INT2 Integer)) " +
                "(not (and (lessThan ?INT1 ?INT2) (lessThan ?INT2 (SuccessorFn ?INT1)))))\n";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        if (forms == null || forms.size() == 0)
            return;
        expectedRes = "! [V__INT2 : $int,V__INT1 : $int] : (~(($less(V__INT1 ,V__INT2) & " +
                "$less(V__INT2 ,s__SuccessorFn__0In1InFn(V__INT1)))))";
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        System.out.println("actual:  " + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void testElimLogops() {

        System.out.println("\n========= testElimLogops ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(=>\n" +
                "  (not\n" +
                "    (and\n" +
                "      (lessThan ?INT1 ?INT2)\n" +
                "      (lessThan ?INT2\n" +
                "        (SuccessorFn ?INT1)))))\n";
        actualRes = SUMOtoTFAform.elimUnitaryLogops(new Formula(kifstring));
        Formula fActual = new Formula(actualRes);

        expectedRes = "(not\n" +
                "    (and\n" +
                "      (lessThan ?INT1 ?INT2)\n" +
                "      (lessThan ?INT2\n" +
                "        (SuccessorFn ?INT1))))";
        Formula fExpected = new Formula(expectedRes);
        System.out.println("actual:  " + fActual);
        System.out.println("expected:" + fExpected);
        if (fExpected.deepEquals(fActual))
            System.out.println("testElimLogops: pass");
        else
            System.out.println("testElimLogops: fail");
        assertTrue(fExpected.deepEquals(fActual));
    }

    /** ***************************************************************
     */
    @Test
    public void testTemporalComp() {

        System.out.println("\n========= test testTemporalComp ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(=> (and (instance ?MONTH Month) (duration ?MONTH (MeasureFn ?NUMBER DayDuration))) " +
                "(equal (CardinalityFn (TemporalCompositionFn ?MONTH Day)) ?NUMBER))";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        expectedRes = "! [V__MONTH : $i,V__NUMBER : $int] : ((s__instance(V__MONTH, s__Month) & " +
                "s__duration(V__MONTH, s__MeasureFn__1InFn(V__NUMBER, s__DayDuration))) => " +
                "s__CardinalityFn(s__TemporalCompositionFn(V__MONTH, s__Day)) = V__NUMBER)";
        System.out.println("actual:  " + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void testBigNumber() {

        System.out.println("\n========= test testBigNumber ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(=> (and (instance ?UNIT UnitOfMeasure) (equal ?TERAUNIT (TeraFn ?UNIT))) " +
                "(equal (MeasureFn 1 ?TERAUNIT) (MeasureFn 1000000000 (KiloFn ?UNIT))))";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        expectedRes = "! [V__TERAUNIT : $i,V__UNIT : $i] : ((s__instance(V__UNIT, s__UnitOfMeasure) & " +
                "equal(V__TERAUNIT ,s__TeraFn(V__UNIT))) => " +
                "equal(s__MeasureFn__1ReFn(1.0, V__TERAUNIT) ,s__MeasureFn__1ReFn(1000000000.0, s__KiloFn(V__UNIT))))";
        System.out.println("actual:  " + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void testNumber() {

        System.out.println("\n========= test testNumber ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(=> (diameter ?CIRCLE ?LENGTH) (exists (?HALF) " +
                "(and (radius ?CIRCLE ?HALF) (equal (MultiplicationFn ?HALF 2) ?LENGTH))))";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        expectedRes = "! [V__CIRCLE : $i,V__LENGTH : $real] : (s__diameter(V__CIRCLE, V__LENGTH) => " +
                "( ? [V__HALF:$real] : ((s__radius(V__CIRCLE, V__HALF) & " +
                "s__MultiplicationFn__0Re1Re2ReFn(V__HALF ,2.0) = V__LENGTH))))";
        System.out.println("actual:  " + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void testMostSpecific() {

        System.out.println("\n========= test testMostSpecific ==========\n");
        assertEquals("RealNumber",SUMOtoTFAform.mostSpecificTerm(Arrays.asList("RealNumber", "LengthMeasure")));
    }

    /** ***************************************************************
     */
    @Test
    public void testTemporalComp2() {

        System.out.println("\n========= test testTemporalComp2 ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(=> (and (instance ?MONTH Month) (duration ?MONTH (MeasureFn ?NUMBER DayDuration))) " +
                "(equal (CardinalityFn (TemporalCompositionFn ?MONTH Day)) ?NUMBER))";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        expectedRes = "! [V__MONTH : $i,V__NUMBER : $int] : ((s__instance(V__MONTH, s__Month) & " +
                "s__duration(V__MONTH, s__MeasureFn__1InFn(V__NUMBER, s__DayDuration))) => " +
                "s__CardinalityFn(s__TemporalCompositionFn(V__MONTH, s__Day)) = V__NUMBER)";
        System.out.println("actual:  " + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void testCeiling() {

        System.out.println("\n========= test testTemporalComp ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(=> (equal (CeilingFn ?NUMBER) ?INT) (not (exists (?OTHERINT) " +
                "(and (instance ?OTHERINT Integer) " +
                "(greaterThanOrEqualTo ?OTHERINT ?NUMBER) (lessThan ?OTHERINT ?INT)))))";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        expectedRes = "! [V__NUMBER : $int,V__INT : $int] : " +
                "(s__CeilingFn__0In1ReFn(V__NUMBER) = V__INT => " +
                "~(( ? [V__OTHERINT:$int] : (($greatereq(V__OTHERINT ,V__NUMBER) & " +
                "$less(V__OTHERINT ,V__INT))))))";
        System.out.println("actual:  " + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void testInList() {

        System.out.println("\n========= test testInList ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(=> (equal (GreatestCommonDivisorFn @ROW) ?NUMBER) " +
                "(forall (?ELEMENT) (=> (inList ?ELEMENT (ListFn @ROW)) " +
                "(equal (RemainderFn ?ELEMENT ?NUMBER) 0))))";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        expectedRes = "! [V__ROW1 : $int,V__NUMBER : $int] : " +
                "(s__GreatestCommonDivisorFn_1__0In1InFn(V__ROW1) = V__NUMBER => " +
                "( ! [V__ELEMENT:$int] : (s__inList__1In(V__ELEMENT, s__ListFn_1__1InFn(V__ROW1)) => " +
                "s__RemainderFn__0In1In2InFn(V__ELEMENT, V__NUMBER) = 0)))";
        System.out.println("actual:  " + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void testLeastCommon() {

        System.out.println("\n========= test testLeastCommon ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(=> (equal (LeastCommonMultipleFn @ROW) ?NUMBER) " +
                "(=> (inList ?ELEMENT (ListFn @ROW)) (instance ?ELEMENT Number)))";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        expectedRes = "! [V__ELEMENT : $i,V__ROW1 : $int,V__NUMBER : $int] : " +
                "(s__LeastCommonMultipleFn_1__0In1InFn(V__ROW1) = V__NUMBER => " +
                "s__inList(V__ELEMENT, s__ListFn_1__1InFn(V__ROW1)) => s__instance(V__ELEMENT, s__Number))";
        System.out.println("actual:  " + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void testMult() {

        System.out.println("\n========= test testMult ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(=> (equal (SquareRootFn ?NUMBER1) ?NUMBER2) (equal (MultiplicationFn ?NUMBER2 ?NUMBER2) ?NUMBER1))";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        expectedRes = "! [V__NUMBER1 : $real,V__NUMBER2 : $real] : " +
                "(s__SquareRootFn__0Re1ReFn(V__NUMBER1) = V__NUMBER2 => " +
                "$product(V__NUMBER2 ,V__NUMBER2) = V__NUMBER1)";
        System.out.println("actual:  " + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void testDay() {

        System.out.println("\n========= test testDay ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(=> (instance ?DAY (DayFn ?NUMBER ?MONTH)) (lessThanOrEqualTo ?NUMBER 31))";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        expectedRes = "! [V__DAY : $i,V__MONTH : $i,V__NUMBER : $int] : " +
                "(s__instance(V__DAY, s__DayFn__1InFn(V__NUMBER, V__MONTH)) => $lesseq(V__NUMBER ,31))";
        System.out.println("actual:  " + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void testExponent() {

        System.out.println("\n========= test testExponent ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(=> (instance ?NUMBER Quantity) " +
                "(equal (ReciprocalFn ?NUMBER) (ExponentiationFn ?NUMBER -1)))";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        expectedRes = "! [V__NUMBER : $real] : (s__instance(V__NUMBER, s__Quantity) => " +
                "s__ReciprocalFn__0Re1ReFn(V__NUMBER) = s__ExponentiationFn__0Re1Re2InFn(V__NUMBER, -1))";
        System.out.println("actual:  " + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void testInstance() {

        System.out.println("\n========= test testInstance ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(=> (instance ?SET FiniteSet) " +
                "(exists (?NUMBER) (and (instance ?NUMBER NonnegativeInteger) " +
                "(equal ?NUMBER (CardinalityFn ?SET)))))";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        expectedRes = "! [V__SET : $i] : (s__instance(V__SET, s__FiniteSet) => " +
                "( ? [V__NUMBER:$int] : (($greater(V__NUMBER ,-1) & V__NUMBER = s__CardinalityFn(V__SET)))))";
        System.out.println("actual:  " + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void testRadian() {

        System.out.println("\n========= test testRadian ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(equal (MeasureFn ?NUMBER AngularDegree) " +
                "(MeasureFn (MultiplicationFn ?NUMBER (DivisionFn Pi 180)) Radian))";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        expectedRes = "! [V__NUMBER : $real] : " +
                "(equal(s__MeasureFn__1ReFn(V__NUMBER, s__AngularDegree) ," +
                "s__MeasureFn__1ReFn($product(V__NUMBER ,$quotient_e(3.141592653589793 ,180.0)), s__Radian)))";
        System.out.println("actual:  " + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void testFloor() {

        //SUMOtoTFAform.debug = true;
        System.out.println("\n========= test testFloor ==========\n");
        //FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(equal (MillionYearsAgoFn ?X) (BeginFn (YearFn (FloorFn " +
                "(AdditionFn 1950 (MultiplicationFn ?X -1000000))))))";
        Set<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        actualRes = SUMOtoTFAform.process(forms.iterator().next().toString());
        expectedRes = "! [V__X : $real] : " +
                "(equal(s__MillionYearsAgoFn(V__X) ," +
                "s__BeginFn(s__YearFn($to_int($sum(1950.0 ,$product(V__X ,-1000000.0)))))))";
        System.out.println("actual:  " + actualRes);
        System.out.println("expected:" + expectedRes);
        assertEquals(expectedRes, actualRes.trim());
    }
}
