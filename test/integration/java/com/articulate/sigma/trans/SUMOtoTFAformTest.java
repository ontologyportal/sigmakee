package com.articulate.sigma.trans;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.StringUtil;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static org.junit.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.assertEquals;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

//This software is released under the GNU Public License
//<http://www.gnu.org/copyleft/gpl.html>.
// Copyright 2019 Infosys
// adam.pease@infosys.com

public class SUMOtoTFAformTest extends IntegrationTestBase {

    /****************************************************************
     */
    @BeforeClass
    public static void init() {

        System.out.println("\n======================== SUMOtoTFAformTest.init(): ");
        SUMOKBtoTFAKB skbtfakb = new SUMOKBtoTFAKB();
        skbtfakb.initOnce();
        if (!kb.containsFile("Merge.kif") || !kb.containsFile("Mid-level-ontology.kif"))
            System.out.println("!!!!!!!! error in init(): missing KB files !!!!!!!!!!!!");
        SUMOtoTFAform.setNumericFunctionInfo();
        SUMOformulaToTPTPformula.lang = "tff";
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        String filename = KBmanager.getMgr().getPref("kbDir") + File.separator + kbName + ".tff";
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            skbtfakb.writeSorts(pw);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void postClass() {
        KBmanager.initialized = false;
        SUMOKBtoTFAKB.initialized = false;
        SUMOtoTFAform.initialized = false;
    }

    @After
    public void tearDown() {

        SUMOtoTFAform.debug = false;
        SUMOformulaToTPTPformula.debug = false;
        SUMOKBtoTFAKB.debug = false;
    }

    /** *************************************************************
     */
    @Test
    public void testExtractSig() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testExtractSig(): ");
        List<String> sorts = SUMOtoTFAform.relationExtractSigFromName("ListFn__6Fn__0Ra1Ra2Ra3Ra4Ra5Ra6Ra");
        System.out.println(sorts);
        String expectedRes = "[RationalNumber, RationalNumber, RationalNumber, RationalNumber, RationalNumber, RationalNumber, RationalNumber]";
        String result = sorts.toString();
        System.out.println("testExtractSig(): expected: " + expectedRes);
        System.out.println("testExtractSig(): actual:    " + sorts);
        if (expectedRes.equals(result))
            System.out.println("testExtractSig(): Success!");
        else
            System.err.println("testExtractSig(): fail");
        assertTrue(sorts.toString().equals("[RationalNumber, RationalNumber, RationalNumber, RationalNumber, RationalNumber, RationalNumber, RationalNumber]"));
    }

    /** *************************************************************
     */
    @Test
    public void testExtractUpdateSig() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testExtractUpdateSig(): ");
        List<String> sorts = SUMOtoTFAform.relationExtractUpdateSigFromName("ListFn__6Fn__0Ra1Ra2Ra3Ra4Ra5Ra6Ra");
        System.out.println(sorts);
        String expectedRes = "[RationalNumber, RationalNumber, RationalNumber, RationalNumber, RationalNumber, RationalNumber, RationalNumber]";
        String result = sorts.toString();
        System.out.println("testExtractUpdateSig(): expected: " + expectedRes);
        System.out.println("testExtractUpdateSig(): actual:    " + sorts);
        if (expectedRes.equals(result))
            System.out.println("testExtractUpdateSig(): Success!");
        else
            System.err.println("testExtractUpdateSig(): fail");
        assertTrue(expectedRes.equals(result));
    }

    /** *************************************************************
     */
    @Test
    public void testExtractUpdateSig2() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testExtractUpdateSig2(): ");
        List<String> sorts = SUMOtoTFAform.relationExtractUpdateSigFromName("partition__5");
        System.out.println(sorts);
        String expectedRes = "[, Class, Class, Class, Class, Class]";
        System.out.println("testExtractUpdateSig2(): expected: " + expectedRes);
        System.out.println("testExtractUpdateSig2(): actual:    " + sorts);
        String result = sorts.toString();
        if (expectedRes.equals(result))
            System.out.println("testExtractUpdateSig2(): Success!");
        else
            System.err.println("testExtractUpdateSig2(): fail");
        assertTrue(expectedRes.equals(result));
    }

    /** *************************************************************
     */
    @Test
    public void testSorts() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testSorts(): ");
        List<String> actual = kb.kbCache.getSignature("AbsoluteValueFn__0Re1ReFn");
        String expectedRes = "[RealNumber, RealNumber]";
        System.out.println("testSorts(): expected: " + expectedRes);
        System.out.println("testSorts(): actual:    " + actual);
        String result = actual.toString();
        if (expectedRes.equals(result))
            System.out.println("testSorts(): Success!");
        else
            System.err.println("testSorts(): fail");
        assertTrue(expectedRes.equals(result));
    }

    /** *************************************************************
     */
    @Test
    public void testParents() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testParents(): ");
        Formula f = new Formula("(=> (instance ?X Human) (parents ?X (AdditionFn 1 1)))");
        ArrayList<String> sig = new ArrayList<>();
        sig.add("");
        sig.add("Human");
        sig.add("Integer");
        kb.terms.add("parents");
        kb.kbCache.relations.add("parents");
        kb.kbCache.signatures.put("parents",sig);
        String result = SUMOtoTFAform.process(f,false);
        System.out.println("SUMOtoTFAformTest.testParents(): result:   " + result);
        String expectedRes = "! [V__X : $i] : ((s__instance(V__X, s__Human) => s__parents(V__X, $sum(1 ,1))))";
        System.out.println("SUMOtoTFAformTest.testParents(): expected: " + expectedRes);
        if (expectedRes.equals(result))
            System.out.println("testParents(): Success!");
        else
            System.err.println("testParents(): fail");
        assertEquals(expectedRes,result);
    }

    /** *************************************************************
     */
    @Test
    public void test1() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.test1(): ");
        Formula f = new Formula("(equal ?X (AdditionFn 1 2))");
        String result = SUMOtoTFAform.process(f,false);
        System.out.println("SUMOtoTFAformTest.test1(): result:   " + result);
        String expectedRes = "! [V__X : $int] : (V__X = $sum(1 ,2))";
        System.out.println("SUMOtoTFAformTest.test1(): expected: " + expectedRes);
        if (expectedRes.equals(result))
            System.out.println("test1(): Success!");
        else
            System.err.println("test1(): fail");
        assertEquals(expectedRes,result);
    }

    /** *************************************************************
     */
    @Test
    public void test1_5() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.test1_5(): ");
        Formula f = new Formula("(equal ?X (SubtractionFn 2 1))");
        String result = SUMOtoTFAform.process(f,false);
        System.out.println("SUMOtoTFAformTest.test1_5(): result:   " + result);
        String expectedRes = "! [V__X : $int] : (V__X = $difference(2 ,1))";
        System.out.println("SUMOtoTFAformTest.test1_5(): expected: " + expectedRes);
        if (expectedRes.equals(result))
            System.out.println("test1_5(): Success!");
        else
            System.err.println("test1_5(): fail");
        assertEquals(expectedRes,result);
    }

    /** *************************************************************
     */
    @Test
    public void test2() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.test2(): ");
        Formula f = new Formula("(=> (and (equal (AbsoluteValueFn ?NUMBER1) ?NUMBER2) " +
                "(instance ?NUMBER1 RealNumber) (instance ?NUMBER2 RealNumber)) " +
                "(or (and (instance ?NUMBER1 NonnegativeRealNumber) (equal ?NUMBER1 ?NUMBER2)) " +
                "(and (instance ?NUMBER1 NegativeRealNumber) (equal ?NUMBER2 (SubtractionFn 0 ?NUMBER1)))))");
        String result = SUMOtoTFAform.process(f,false);
        System.out.println("SUMOtoTFAformTest.test2(): result:   " + result);
        String expected = "! [V__NUMBER1 : $int,V__NUMBER2 : $int] : " +
                "((s__AbsoluteValueFn__0In1InFn(V__NUMBER1) = V__NUMBER2 => " +
                "(((s__SignumFn__0In1InFn(V__NUMBER1) = 1 | " +
                "s__SignumFn__0In1InFn(V__NUMBER1) = 0) & V__NUMBER1 = V__NUMBER2) | " +
                "(s__SignumFn__0In1InFn(V__NUMBER1) = -1 & V__NUMBER2 = $difference(0 ,V__NUMBER1)))))";
        System.out.println("SUMOtoTFAformTest.test2(): expected: " + expected);
        if (expected.equals(result))
            System.out.println("test2(): Success!");
        else
            System.err.println("test2(): fail");
        assertEquals(expected,result);
    }

    /** *************************************************************
     */
    @Test
    public void test3() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.test3(): ");
        Formula f = new Formula("(<=> (equal (RemainderFn ?NUMBER1 ?NUMBER2) ?NUMBER) " +
                "(equal (AdditionFn (MultiplicationFn (FloorFn (DivisionFn ?NUMBER1 ?NUMBER2)) ?NUMBER2) ?NUMBER) ?NUMBER1))");
        String result = SUMOtoTFAform.process(f,false);
        System.out.println("SUMOtoTFAformTest.test3(): result:   " + result);
        String expected = "! [V__NUMBER1 : $int,V__NUMBER2 : $int,V__NUMBER : $int] : "
                + "((($remainder_t(V__NUMBER1 ,V__NUMBER2) = V__NUMBER => "
                + "$sum($product($floor($quotient_e(V__NUMBER1 ,V__NUMBER2)) ,V__NUMBER2) ,V__NUMBER) = V__NUMBER1) & "
                + "($sum($product($floor($quotient_e(V__NUMBER1 ,V__NUMBER2)) ,V__NUMBER2) ,V__NUMBER) = V__NUMBER1 => "
                + "$remainder_t(V__NUMBER1 ,V__NUMBER2) = V__NUMBER)))";
        System.out.println("SUMOtoTFAformTest.test3(): expected: " + expected);
        if (expected.equals(result))
            System.out.println("test3(): Success!");
        else
            System.err.println("test3(): fail");
        assertEquals(expected,result);
    }

    /** *************************************************************
     */
    @Test
    public void test4() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.test4(): ");
        Formula f = new Formula("(<=> (greaterThanOrEqualTo ?NUMBER1 ?NUMBER2) " +
                "(or (equal ?NUMBER1 ?NUMBER2) (greaterThan ?NUMBER1 ?NUMBER2)))");
        String result = SUMOtoTFAform.process(f,false);
        System.out.println("SUMOtoTFAformTest.test4(): result:   " + result);
        String expected = "! [V__NUMBER1 : $real,V__NUMBER2 : $real] : " +
                "(((($greatereq(V__NUMBER1,V__NUMBER2)) => " +
                "(V__NUMBER1 = V__NUMBER2 | ($greater(V__NUMBER1,V__NUMBER2)))) & " +
                "((V__NUMBER1 = V__NUMBER2 | ($greater(V__NUMBER1,V__NUMBER2))) => " +
                "($greatereq(V__NUMBER1,V__NUMBER2)))))";
        System.out.println("SUMOtoTFAformTest.test4(): expected: " + expected);
        if (expected.equals(result))
            System.out.println("test4(): Success!");
        else
            System.err.println("test4(): fail");
        assertEquals(expected,result);
    }

    /** *************************************************************
     */
    @Test
    public void test5() {

        //SUMOtoTFAform.debug = true;
        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.test5(): ");
        Formula f = new Formula("(=>\n" +
                "(measure ?QUAKE\n" +
                "(MeasureFn ?VALUE RichterMagnitude))\n" +
                "(instance ?VALUE PositiveRealNumber))");
        String result = SUMOtoTFAform.modifyTypesToConstraints(f);
        System.out.println("SUMOtoTFAformTest.test5(): result: " + result);
        String expected = "(=>\n" +
                "(measure ?QUAKE\n" +
                "(MeasureFn ?VALUE RichterMagnitude))\n" +
                "(equal (SignumFn ?VALUE) 1))";
        System.out.println("SUMOtoTFAformTest.test5(): expected: " + expected);
        if (expected.equals(result))
            System.out.println("test5(): Success!");
        else
            System.err.println("test5(): fail");
        assertEquals(expected,result);
    }

    /****************************************************************
     */
    @Test
    public void testFloorFn() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testFloorFn(): ");
        Formula f = new Formula("(<=> " +
                "(equal (RemainderFn ?NUMBER1 ?NUMBER2) ?NUMBER) " +
                "(equal (AdditionFn (MultiplicationFn (FloorFn (DivisionFn ?NUMBER1 ?NUMBER2)) ?NUMBER2) ?NUMBER) ?NUMBER1))");
        System.out.println("formula: " + f);
        String result = SUMOtoTFAform.process(f,false);
        System.out.println("SUMOtoTFAformTest.testFloorFn(): result: " + result);
        String expected = "! [V__NUMBER1 : $int,V__NUMBER2 : $int,V__NUMBER : $int] : "
                + "((($remainder_t(V__NUMBER1 ,V__NUMBER2) = V__NUMBER => "
                + "$sum($product($floor($quotient_e(V__NUMBER1 ,V__NUMBER2)) ,V__NUMBER2) ,V__NUMBER) = V__NUMBER1) & "
                + "($sum($product($floor($quotient_e(V__NUMBER1 ,V__NUMBER2)) ,V__NUMBER2) ,V__NUMBER) = V__NUMBER1 => "
                + "$remainder_t(V__NUMBER1 ,V__NUMBER2) = V__NUMBER)))";
        System.out.println("SUMOtoTFAformTest.testFloorFn(): expect: " + expected);
        System.out.println("equal: " + expected.equals(result));
        if (expected.equals(result))
            System.out.println("testFloorFn(): Success!");
        else
            System.err.println("testFloorFn(): fail");
        assertEquals(expected,result);
    }

    /****************************************************************
     */
    @Test
    public void testNumericSubclass() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testSubclass(): ");
        Formula f = new Formula("(<=> (and (equal (AbsoluteValueFn ?NUMBER1) ?NUMBER2) " +
                "(instance ?NUMBER1 RealNumber) (instance ?NUMBER2 RealNumber)) " +
                "(or (and (instance ?NUMBER1 NonnegativeRealNumber) (equal ?NUMBER1 ?NUMBER2)) " +
                "(and (instance ?NUMBER1 NegativeRealNumber) (equal ?NUMBER2 (SubtractionFn 0 ?NUMBER1)))))");
        String result = SUMOtoTFAform.process(f,false);
        System.out.println("SUMOtoTFAformTest.testNumericSubclass(): result:  " + result);
        String expected = "! [V__NUMBER1 : $int,V__NUMBER2 : $int] : (((s__AbsoluteValueFn__0In1InFn(V__NUMBER1) = "
                + "V__NUMBER2 => (((s__SignumFn__0In1InFn(V__NUMBER1) = 1 | s__SignumFn__0In1InFn(V__NUMBER1) = 0) & "
                + "V__NUMBER1 = V__NUMBER2) | (s__SignumFn__0In1InFn(V__NUMBER1) = -1 & V__NUMBER2 = $difference(0 ,V__NUMBER1)))) & "
                + "((((s__SignumFn__0In1InFn(V__NUMBER1) = 1 | s__SignumFn__0In1InFn(V__NUMBER1) = 0) & V__NUMBER1 = V__NUMBER2) | "
                + "(s__SignumFn__0In1InFn(V__NUMBER1) = -1 & V__NUMBER2 = $difference(0 ,V__NUMBER1))) => "
                + "s__AbsoluteValueFn__0In1InFn(V__NUMBER1) = V__NUMBER2)))";
        System.out.println("\"SUMOtoTFAformTest.testNumericSubclass(): expect: " + expected);
        if (expected.equals(result))
            System.out.println("testNumericSubclass(): Success!");
        else
            System.err.println("testNumericSubclass(): fail");
        assertEquals(expected,result);
    }

    /****************************************************************
     */
    @Test
    public void testElimUnitaryLogops() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testElimUnitaryLogops(): ");
        Formula f = new Formula("(<=> (and (equal (AbsoluteValueFn ?NUMBER1) ?NUMBER2)) " +
                "(or (and (instance ?NUMBER1 NonnegativeRealNumber) (equal ?NUMBER1 ?NUMBER2)) " +
                "(and (instance ?NUMBER1 NegativeRealNumber) (equal ?NUMBER2 (SubtractionFn 0 ?NUMBER1)))))");
        String result = SUMOtoTFAform.elimUnitaryLogops(f);
        System.out.println("SUMOtoTFAformTest.testNElimUnitaryLogops(): result: " + result);
        String expected = "(<=> (equal (AbsoluteValueFn ?NUMBER1) ?NUMBER2) " +
                "(or (and (instance ?NUMBER1 NonnegativeRealNumber) (equal ?NUMBER1 ?NUMBER2)) " +
                "(and (instance ?NUMBER1 NegativeRealNumber) (equal ?NUMBER2 (SubtractionFn 0 ?NUMBER1)))))";
        System.out.println("SUMOtoTFAformTest.testNElimUnitaryLogops(): expect: " + expected);
        if (expected.equals(result))
            System.out.println("testElimUnitaryLogops(): Success!");
        else
            System.err.println("testElimUnitaryLogops(): fail");
        assertEquals(expected,result);
    }

    /****************************************************************
     */
    @Test
    public void testVariableArity() {

        //SUMOtoTFAform.debug = true;
        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testVariableArity(): ");
        Formula f = new Formula("(<=> (and (instance ?REL TotalValuedRelation) " +
                "(instance ?REL Predicate)) (exists (?VALENCE) (and (instance ?REL Relation) " +
                "(valence ?REL ?VALENCE) (=> (forall (?NUMBER ?ELEMENT ?CLASS) " +
                "(=> (and (lessThan ?NUMBER ?VALENCE) (domain ?REL ?NUMBER ?CLASS) " +
                "(equal ?ELEMENT (ListOrderFn (ListFn @ROW) ?NUMBER))) " +
                "(instance ?ELEMENT ?CLASS))) (exists (?ITEM) (?REL @ROW ?ITEM))))))");
        System.out.println("formula: " + f);
        String result = SUMOtoTFAform.process(f,false);
        System.out.println("SUMOtoTFAformTest.testVariableArity(): result: " + result);
        String expected = "! [V__ROW : $i,V__REL : $i] : ((((s__instance(V__REL, s__TotalValuedRelation) & "
                + "s__instance(V__REL, s__Predicate)) => ( ? [V__VALENCE:$int] : ((s__instance(V__REL, s__Relation) & "
                + "s__valence(V__REL,V__VALENCE) & (( ! [V__NUMBER:$int, V__ELEMENT:$i, V__CLASS:$i] : "
                + "(((($less(V__NUMBER,V__VALENCE)) & s__domain(V__REL,V__NUMBER,V__CLASS) & V__ELEMENT = "
                + "s__ListOrderFn(s__ListFn(V__ROW),V__NUMBER)) => s__instance(V__ELEMENT, V__CLASS)))) => "
                + "( ? [V__ITEM] : (s__?REL(V__ROW, V__ITEM)))))))) & (( ? [V__VALENCE:$int] : "
                + "((s__instance(V__REL, s__Relation) & s__valence(V__REL,V__VALENCE) & "
                + "(( ! [V__NUMBER:$int, V__ELEMENT:$i, V__CLASS:$i] : (((($less(V__NUMBER,V__VALENCE)) & "
                + "s__domain(V__REL,V__NUMBER,V__CLASS) & V__ELEMENT = s__ListOrderFn(s__ListFn(V__ROW),V__NUMBER)) => "
                + "s__instance(V__ELEMENT, V__CLASS)))) => ( ? [V__ITEM] : (s__?REL(V__ROW, V__ITEM))))))) => "
                + "(s__instance(V__REL, s__TotalValuedRelation) & s__instance(V__REL, s__Predicate)))))";
        System.out.println("SUMOtoTFAformTest.testVariableArity(): expect: " + expected);
        System.out.println("equal: " + expected.equals(result));
        if (expected.equals(result))
            System.out.println("testVariableArity(): Success!");
        else
            System.err.println("testVariableArity(): fail");
        assertEquals(expected,result);
    }

    /****************************************************************
     */
    @Test
    public void testVariableArity2() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testVariableArity2(): ");
        Formula f = new Formula("(<=> (and (instance stringLength TotalValuedRelation) " +
                "(instance stringLength Predicate)) (exists (?VALENCE) (and (instance stringLength Relation) " +
                "(valence stringLength ?VALENCE) (=> (forall (?NUMBER ?ELEMENT ?CLASS) " +
                "(=> (and (lessThan ?NUMBER ?VALENCE) (domain stringLength ?NUMBER ?CLASS) " +
                "(equal ?ELEMENT (ListOrderFn (ListFn @ROW) ?NUMBER))) " +
                "(instance ?ELEMENT ?CLASS))) (exists (?ITEM) (stringLength @ROW ?ITEM))))))");
        System.out.println("formula: " + f);
        String result = SUMOtoTFAform.process(f,false);
        System.out.println("actual: " + result);
        String expected = "! [V__ROW : $int] : ((((s__instance(s__stringLength__m, s__TotalValuedRelation) & "
                + "s__instance(s__stringLength__m, s__Predicate)) => ( ? [V__VALENCE:$int] : "
                + "((s__instance(s__stringLength__m, s__Relation) & s__valence(s__stringLength__m,V__VALENCE) & "
                + "(( ! [V__NUMBER:$int, V__ELEMENT:$i, V__CLASS:$i] : (((($less(V__NUMBER,V__VALENCE)) & "
                + "s__domain(s__stringLength__m,V__NUMBER,V__CLASS) & V__ELEMENT = "
                + "s__ListOrderFn(s__ListFn__1Fn__0En1InFn(V__ROW),V__NUMBER)) => s__instance(V__ELEMENT, V__CLASS)))) => "
                + "( ? [V__ITEM:$i] : (s__stringLength(V__ROW,V__ITEM)))))))) & (( ? [V__VALENCE:$int] : "
                + "((s__instance(s__stringLength__m, s__Relation) & s__valence(s__stringLength__m,V__VALENCE) & "
                + "(( ! [V__NUMBER:$int, V__ELEMENT:$i, V__CLASS:$i] : (((($less(V__NUMBER,V__VALENCE)) & "
                + "s__domain(s__stringLength__m,V__NUMBER,V__CLASS) & V__ELEMENT = "
                + "s__ListOrderFn(s__ListFn__1Fn__0En1InFn(V__ROW),V__NUMBER)) => s__instance(V__ELEMENT, V__CLASS)))) => "
                + "( ? [V__ITEM:$i] : (s__stringLength(V__ROW,V__ITEM))))))) => "
                + "(s__instance(s__stringLength__m, s__TotalValuedRelation) & s__instance(s__stringLength__m, s__Predicate)))))";
        System.out.println("expect: " + expected);
        System.out.println("equal: " + expected.equals(result));
        if (expected.equals(result))
            System.out.println("testVariableArity2(): Success!");
        else
            System.err.println("testVariableArity2(): fail");
        assertEquals(expected,result);
    }

    /****************************************************************
     */
    @Test
    public void testPredVarArity() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testPredVarArity(): ");
        Formula f = new Formula("(<=>\n" +
                "  (and\n" +
                "    (instance greaterThan__1Ra2Ra TotalValuedRelation)\n" +
                "    (instance greaterThan__1Ra2Ra Predicate))\n" +
                "  (exists (?VALENCE)\n" +
                "    (and\n" +
                "      (greaterThan ?VALENCE 0)\n" +
                "      (and\n" +
                "        (instance greaterThan__1Ra2Ra Relation)\n" +
                "        (valence greaterThan__1Ra2Ra ?VALENCE)\n" +
                "        (=>\n" +
                "          (forall (?NUMBER ?ELEMENT ?CLASS)\n" +
                "            (=>\n" +
                "              (and\n" +
                "                (greaterThan ?NUMBER 0)\n" +
                "                (instance ?CLASS Class))\n" +
                "              (=>\n" +
                "                (and\n" +
                "                  (lessThan ?NUMBER ?VALENCE)\n" +
                "                  (domain greaterThan__1Ra2Ra ?NUMBER ?CLASS)\n" +
                "                  (equal ?ELEMENT\n" +
                "                    (ListOrderFn\n" +
                "                      (ListFn_1 ?ROW1) ?NUMBER)))\n" +
                "                (instance ?ELEMENT ?CLASS))))\n" +
                "          (exists (?ITEM)\n" +
                "            (and\n" +
                "              (instance ?ITEM RationalNumber)\n" +
                "              (greaterThan__1Ra2Ra ?ROW1 ?ITEM))))))))");
        System.out.println("formula: " + f);
        String result = SUMOtoTFAform.process(f,false);
        System.out.println("actual: " + result);
        String expected = "! [V__ROW1 : $rat] : ((((s__instance(s__greaterThan__1Ra2Ra__m, s__TotalValuedRelation) & "
                + "s__instance(s__greaterThan__1Ra2Ra__m, s__Predicate)) => ( ? [V__VALENCE:$int] : "
                + "((($greater(V__VALENCE,0)) & (s__instance(s__greaterThan__1Ra2Ra__m, s__Relation) & "
                + "s__valence(s__greaterThan__1Ra2Ra__m,V__VALENCE) & (( ! [V__NUMBER:$int, V__ELEMENT:$i, V__CLASS:$i] : "
                + "(((($greater(V__NUMBER,0)) & s__instance(V__CLASS, s__Class)) => ((($less(V__NUMBER,V__VALENCE)) & "
                + "s__domain(s__greaterThan__1Ra2Ra__m,V__NUMBER,V__CLASS) & V__ELEMENT = "
                + "s__ListOrderFn(s__ListFn_1__1Re(V__ROW1),V__NUMBER)) => s__instance(V__ELEMENT, V__CLASS))))) => "
                + "( ? [V__ITEM:$rat] : ((s__instance__1Re2En(V__ITEM, s__RationalNumber) & "
                + "($greater(V__ROW1,V__ITEM))))))))))) & (( ? [V__VALENCE:$int] : ((($greater(V__VALENCE,0)) & "
                + "(s__instance(s__greaterThan__1Ra2Ra__m, s__Relation) & s__valence(s__greaterThan__1Ra2Ra__m,V__VALENCE) & "
                + "(( ! [V__NUMBER:$int, V__ELEMENT:$i, V__CLASS:$i] : (((($greater(V__NUMBER,0)) & "
                + "s__instance(V__CLASS, s__Class)) => ((($less(V__NUMBER,V__VALENCE)) & "
                + "s__domain(s__greaterThan__1Ra2Ra__m,V__NUMBER,V__CLASS) & V__ELEMENT = "
                + "s__ListOrderFn(s__ListFn_1__1Re(V__ROW1),V__NUMBER)) => s__instance(V__ELEMENT, V__CLASS))))) => "
                + "( ? [V__ITEM:$rat] : ((s__instance__1Re2En(V__ITEM, s__RationalNumber) & "
                + "($greater(V__ROW1,V__ITEM)))))))))) => "
                + "(s__instance(s__greaterThan__1Ra2Ra__m, s__TotalValuedRelation) & "
                + "s__instance(s__greaterThan__1Ra2Ra__m, s__Predicate)))))";
        System.out.println("expect: " + expected);
        System.out.println("equal: " + expected.equals(result));
        if (expected.equals(result))
            System.out.println("testPredVarArity(): Success!");
        else
            System.err.println("testPredVarArity(): fail");
        assertEquals(expected,result);
    }


    /****************************************************************
     */
    @Test
    public void testRemoveNumInst() {

        //SUMOtoTFAform.debug = true;
        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testRemoveNumInst(): ");
        String sf = "\n" +
                "(<=>\n" +
                "    (and\n" +
                "        (instance initialList__2Ra TotalValuedRelation)\n" +
                "        (instance initialList__2Ra Predicate))\n" +
                "    (exists (?VALENCE)\n" +
                "        (and\n" +
                "            (instance initialList__2Ra Relation)\n" +
                "            (valence initialList__2Ra ?VALENCE)\n" +
                "            (=>\n" +
                "                (forall (?NUMBER ?ELEMENT ?CLASS)\n" +
                "                    (=>\n" +
                "                        (and\n" +
                "                            (lessThan ?NUMBER ?VALENCE)\n" +
                "                            (domain initialList__2Ra ?NUMBER ?CLASS)\n" +
                "                            (equal ?ELEMENT\n" +
                "                                (ListOrderFn\n" +
                "                                    (ListFn @ROW) ?NUMBER)))\n" +
                "                        (instance ?ELEMENT ?CLASS)))\n" +
                "                (exists (?ITEM)\n" +
                "                    (initialList__2Ra @ROW ?ITEM))))))";
        Formula f = new Formula(sf);
        System.out.println("formula: " + f);
        String result = SUMOtoTFAform.process(f,false);
        System.out.println("actual: " + result);
        String expected = "! [V__ROW : $i] : ((((s__instance(s__initialList__2Ra__m, s__TotalValuedRelation) & "
                + "s__instance(s__initialList__2Ra__m, s__Predicate)) => ( ? [V__VALENCE:$int] : "
                + "((s__instance(s__initialList__2Ra__m, s__Relation) & s__valence(s__initialList__2Ra__m,V__VALENCE) & "
                + "(( ! [V__NUMBER:$int, V__ELEMENT:$i, V__CLASS:$i] : (((($less(V__NUMBER,V__VALENCE)) & "
                + "s__domain(s__initialList__2Ra__m,V__NUMBER,V__CLASS) & V__ELEMENT = "
                + "s__ListOrderFn(s__ListFn(V__ROW),V__NUMBER)) => s__instance(V__ELEMENT, V__CLASS)))) => "
                + "( ? [V__ITEM:$rat] : (s__initialList__1En2Re(V__ROW, $to_real(V__ITEM))))))))) & "
                + "(( ? [V__VALENCE:$int] : ((s__instance(s__initialList__2Ra__m, s__Relation) & "
                + "s__valence(s__initialList__2Ra__m,V__VALENCE) & (( ! [V__NUMBER:$int, V__ELEMENT:$i, V__CLASS:$i] : "
                + "(((($less(V__NUMBER,V__VALENCE)) & s__domain(s__initialList__2Ra__m,V__NUMBER,V__CLASS) & "
                + "V__ELEMENT = s__ListOrderFn(s__ListFn(V__ROW),V__NUMBER)) => s__instance(V__ELEMENT, V__CLASS)))) => "
                + "( ? [V__ITEM:$rat] : (s__initialList__1En2Re(V__ROW, $to_real(V__ITEM)))))))) => "
                + "(s__instance(s__initialList__2Ra__m, s__TotalValuedRelation) & "
                + "s__instance(s__initialList__2Ra__m, s__Predicate)))))";
        System.out.println("expect: " + expected);
        System.out.println("equal: " + expected.equals(result));
        if (expected.equals(result))
            System.out.println("testRemoveNumInst(): Success!");
        else
            System.err.println("testRemoveNumInst(): fail");
        assertEquals(expected,result);
    }


    /****************************************************************
     */
    @Test
    public void testInstNum() {

        SUMOtoTFAform.debug = true;
        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testInstNum(): ");
        String sf = "(=> " +
                "(and " +
                "  (instance equal RelationExtendedToQuantities) " +
                "  (equal ?Q1 (MeasureFn ?I1 ?U)) " +
                "  (equal ?Q2 (MeasureFn ?I2 ?U)) " +
                "  (equal ?I1 ?I2)) " +
                "(equal ?Q1 ?Q2))";
        Formula f = new Formula(sf);
        System.out.println("formula: " + f);
        String result = SUMOtoTFAform.process(f,false);
        System.out.println("actual: " + result);
        String expected = "! [V__Q2 : $i,V__U : $i,V__I1 : $real,V__I2 : $real,V__Q1 : $i] : "
                + "(((s__instance(s__equal__m, s__RelationExtendedToQuantities) & V__Q1 = "
                + "s__MeasureFn(V__I1, V__U) & V__Q2 = s__MeasureFn(V__I2, V__U) & V__I1 = V__I2) => "
                + "V__Q1 = V__Q2))";
        System.out.println("expect: " + expected);
        System.out.println("equal: " + expected.equals(result));
        if (expected.equals(result))
            System.out.println("testInstNum(): Success!");
        else
            System.err.println("testInstNum(): fail");
        assertEquals(expected,result);
    }

    /****************************************************************
     */
    @Test
    public void testTypeConflict() {

        //SUMOtoTFAform.debug = true;
        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testTypeConflict(): ");
        String sf = "(=> (and (irreflexiveOn pathLength ?CLASS) (instance pathLength Predicate)) " +
                "(forall (?INST) (=> (instance ?INST ?CLASS) (not (pathLength ?INST ?INST)))))";
        Formula f = new Formula(sf);
        System.out.println("formula: " + f);
        String s = SUMOtoTFAform.process(f,false);
        //KBcache.debug = true;
        boolean inc = SUMOtoTFAform.inconsistentVarTypes();
        System.out.println("SUMOtoTFAformTest.testTypeConflict(): varmap: " + SUMOtoTFAform.varmap);
        if (inc)
            System.out.println("testTypeConflict(): Success!");
        else
            System.err.println("testTypeConflict(): fail");
        assertTrue(inc);
    }

    /****************************************************************
     */
    @Test
    public void testTypeConflict2() {

        //SUMOtoTFAform.debug = true;
        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testTypeConflict2(): ");
        String sf = "(=> (and (irreflexiveOn multiplicativeFactor ?CLASS) (instance multiplicativeFactor Predicate)) " +
                "(forall (?INST) (=> (instance ?INST ?CLASS) (not (multiplicativeFactor ?INST ?INST)))))";
        Formula f = new Formula(sf);
        System.out.println("formula: " + f);
        String result = SUMOtoTFAform.process(f,false);
        String expected = "! [V__CLASS : $i] : (((s__irreflexiveOn(s__multiplicativeFactor__m, V__CLASS) & "
                + "s__instance(s__multiplicativeFactor__m, s__Predicate)) => ( ! [V__INST:$int] : "
                + "((s__instance__1In2En(V__INST, V__CLASS) => ~(s__multiplicativeFactor(V__INST, V__INST)))))))";
        System.out.println("SUMOtoTFAformTest.testTypeConflict2(): result:   " + result);
        System.out.println("SUMOtoTFAformTest.testTypeConflict2(): expected: " + expected);
        if (result.equals(expected))
            System.out.println("testTypeConflict2(): Success!");
        else
            System.err.println("testTypeConflict2(): fail");
        assertEquals(expected,result);
    }

    /****************************************************************
     * nonsense axiom just to check types
     */
    @Test
    public void testTypeConflict3() {

        //SUMOtoTFAform.debug = true;
        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testTypeConflict3(): ");
        String sf = "(forall (?X) (=> (instance ?X Table) (agent ?Y ?X)))";
        Formula f = new Formula(sf);
        System.out.println("formula: " + f);
        String result = SUMOtoTFAform.process(f,false);
        assertTrue(result.isBlank());
        boolean inc = SUMOtoTFAform.inconsistentVarTypes();
        System.out.println("SUMOtoTFAformTest.testTypeConflict3(): varmap: " + SUMOtoTFAform.varmap);
        if (inc)
            System.out.println("testTypeConflict3(): Success!");
        else
            System.err.println("testTypeConflict3(): fail");
        assertTrue(inc);
    }

    /****************************************************************
     */
    @Test
    public void testTransNum() {

        SUMOtoTFAform.debug = true;
        //FormulaPreprocessor.debug = true;
        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testTransNum(): ");
        String sf = "(=> (instance ?X NegativeInteger) (greaterThan 0 ?X))";
        Formula f = new Formula(sf);
        System.out.println("formula: " + f);
        String result = SUMOtoTFAform.process(f,false);
        System.out.println("SUMOtoTFAformTest.testTransNum(): result: " + result);
        if (!StringUtil.emptyString(result))
            System.out.println("testTransNum(): Success!");
        else
            System.err.println("testTransNum(): fail");
        assertTrue(!StringUtil.emptyString(result));
    }

    /****************************************************************
     */
    @Test
    public void testPropertyFn() {

        SUMOtoTFAform.debug = true;
        SUMOformulaToTPTPformula.debug = true;
        //FormulaPreprocessor.debug = true;
        System.out.println();
        System.out.println("\n======================== SUMOtoTFAformTest.testPropertyFn(): ");
        String sf = "(<=> (instance ?OBJ (PropertyFn ?PERSON)) (possesses ?PERSON ?OBJ))";
        Formula f = new Formula(sf);
        System.out.println("formula: " + f);
        String result = SUMOtoTFAform.process(f,false);
        String expected = "! [V__OBJ : $i,V__PERSON : $i] : (((s__instance(V__OBJ, s__PropertyFn(V__PERSON)) => " +
                "s__possesses(V__PERSON, V__OBJ)) & (s__possesses(V__PERSON, V__OBJ) => s__instance(V__OBJ, s__PropertyFn(V__PERSON)))))";
        System.out.println("SUMOtoTFAformTest.testPropertyFn(): result:   " + result);
        System.out.println("SUMOtoTFAformTest.testPropertyFn(): expected: " + expected);
        if (expected.equals(result))
            System.out.println("testPropertyFn(): Success!");
        else
            System.err.println("testPropertyFn(): fail");
        assertTrue(expected.equals(result));
    }

    /** ***************************************************************
     */
    @Test
    public void testMemberTypeCount() {

        SUMOtoTFAform.debug = true;
        SUMOKBtoTFAKB.debug = true;
        System.out.println("\n========= testMemberTypeCount ==========\n");
        System.out.println("testMemberTypeCount(): memberTypeCount exists: " + kb.terms.contains("memberTypeCount"));
        System.out.println("testMemberTypeCount(): memberTypeCount signature: " + kb.kbCache.getSignature("memberTypeCount"));
        String input = "(=> (and (memberTypeCount ?GROUP ?TYPE ?NUMBER) (equal ?NUMBER 0)) " +
                "(not (exists (?ITEM) (and (instance ?ITEM ?TYPE) (member ?ITEM ?GROUP)))))";
        String actualRes = SUMOtoTFAform.process(input,false);
        String expectedRes = "! [V__GROUP : $i,V__TYPE : $i,V__NUMBER : $int] : "
                + "(((s__memberTypeCount(V__GROUP,V__TYPE,V__NUMBER) & V__NUMBER = 0) => "
                + "~(( ? [V__ITEM:$i] : ((s__instance(V__ITEM, V__TYPE) & "
                + "s__member(V__ITEM, V__GROUP)))))))";
        System.out.println("actual:  " + actualRes);
        System.out.println("expected:" + expectedRes);
        if (expectedRes.equals(actualRes.trim()))
            System.out.println("testMemberTypeCount(): Success!");
        else
            System.err.println("testMemberTypeCount(): fail");
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void testTypeConflict4() {

        // tff(kb_SUMO_283,axiom,(! [V__ROW1 : $i,V__ROW2 : $real,V__CLASS : $i,V__NUMBER : $int] : ((s__instance(V__ROW1, s__Human) & s__instance(V__CLASS, s__Class)) => (s__domain__2In(s__intelligenceQuotient__m, V__NUMBER, V__CLASS) & s__instance(s__intelligenceQuotient__m, s__Predicate) & s__intelligenceQuotient__2Re(V__ROW1, V__ROW2)) => s__instance(s__ListOrderFn__2InFn(s__ListFn__2ReFn(V__ROW1, V__ROW2), V__NUMBER), V__CLASS)))).

        SUMOtoTFAform.debug = true;
        SUMOtoTFAform stfa = new SUMOtoTFAform();
        SUMOKBtoTFAKB.debug = true;
        System.out.println("\n========= testTypeConflict4 ==========\n");
        String input = "(=> (and (domain intelligenceQuotient ?NUMBER ?CLASS) " +
                "(instance intelligenceQuotient Predicate) (intelligenceQuotient__2Re ?ROW1 ?ROW2)) " +
                "(instance (ListOrderFn__2InFn (ListFn__2Fn__2ReFn ?ROW1 ?ROW2) ?NUMBER) ?CLASS))";
        Formula f = new Formula(input);
        SUMOtoTFAform.varmap = SUMOtoTFAform.fp.findAllTypeRestrictions(f, kb);
        System.out.println("testTypeConflict4(): testing missing sorts");
        stfa.sorts = stfa.missingSorts(f);
        System.out.println("testTypeConflict4(): sorts: " + stfa.sorts);
        if (stfa.sorts != null && !stfa.sorts.isEmpty())
            f.tffSorts.addAll(stfa.sorts);
        boolean actual = SUMOtoTFAform.typeConflict(f);
        if (!actual)
            System.out.println("testTypeConflict4(): Success!");
        else
            System.err.println("testTypeConflict4(): fail");
        assertFalse(actual);
    }
}
