package com.articulate.sigma;

//This software is released under the GNU Public License
//<http://www.gnu.org/copyleft/gpl.html>.
// Copyright 2019 Infosys
// adam.pease@infosys.com

import com.articulate.sigma.parsing.Expr;
import com.articulate.sigma.parsing.FormulaAST;
import com.articulate.sigma.trans.SUMOKBtoTFAKB;
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.google.common.collect.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import org.junit.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * FormulaPreprocessor tests not focused on findExplicitTypes( ) or computeVariableTypes( ).
 */
public class FormulaPreprocessorTest extends UnitTestBase  {

    @After
    public void tearDown() {
        SUMOformulaToTPTPformula.setLang("fof");
        SUMOKBtoTPTPKB.setLang("fof");
    }

    /** ***************************************************************
     */
    // TODO: Technically, this should to in the FormulaTest class, but the gatherRelationsWithArgTypes( ) method requires a KB
    // and none of the other tests in that class do. Maybe move the method to FormulaPreprocessor--it's the only Formula method
    // requiring a KB.
    @Test
    public void testGatherRelationships()   {

        System.out.println("\n============= testGatherRelationships ==================");
        String stmt = "(agent Leaving Human)";
        FormulaAST f = new FormulaAST();
        f.read(stmt);

        Map<String, List> actualMap = f.gatherRelationsWithArgTypes(SigmaTestBase.kb);

        List<String> expectedList = Lists.newArrayList(null, "Process", "AutonomousAgent", null, null, null, null, null);
        Map<String, List> expectedMap = Maps.newHashMap();
        expectedMap.put("agent", expectedList);

        System.out.println("testGatherRelationships(): actual: " + actualMap);
        System.out.println("testGatherRelationships(): expected: " + expectedMap);
        if (expectedMap.equals(actualMap))
            System.out.println("testGatherRelationships(): pass");
        else
            System.err.println("testGatherRelationships(): fail");
        assertEquals(expectedMap, actualMap);
    }

    /** ***************************************************************
     */
    // FIXME: test is waiting completion of Formula.logicallyEquals()
    @Ignore
    @Test
    public void testAddTypes1() {

        System.out.println("\n============= testAddTypes1 ==================");
        String stmt = "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) " +
                "(element ?ELEMENT ?SET2))) (equal ?SET1 ?SET2))";
        FormulaAST f = new FormulaAST();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        FormulaAST expected = new FormulaAST();
        String expectedString = "(=> (and (instance ?SET2 Set) (instance ?SET1 Set)) " +
                "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) (element ?ELEMENT ?SET2))) " +
                "(equal ?SET1 ?SET2)))";
        expected.read(expectedString);
        Map<String, Set<String>> varmap = fp.findTypeRestrictionsExpr(f.expr, kb);
        FormulaAST actual = new FormulaAST();
        actual.expr = fp.addTypeRestrictionsExpr(f.expr, varmap, SigmaTestBase.kb);
        System.out.println("testAddTypes1(): actual: " + actual);
        System.out.println("testAddTypes1(): expected: " + expected);
        if (expected.equals(actual))
            System.out.println("testAddTypes1(): pass");
        else
            System.err.println("testAddTypes1(): fail");
        assertEquals(expected, actual);
        if (expected.logicallyEquals(actual))
            System.out.println("testAddTypes1(): pass");
        else
            System.err.println("testAddTypes1(): fail");
        assertTrue(expected.logicallyEquals(actual));
    }

    /** ***************************************************************
     */
    // FIXME: test is waiting completion of Formula.logicallyEquals()
    @Ignore
    @Test
    public void testAddTypes2() {

        System.out.println("\n============= testAddTypes2 ==================");
        String stmt = "(=> (and (attribute ?AREA LowTerrain) (part ?ZONE ?AREA)" +
                " (slopeGradient ?ZONE ?SLOPE)) (greaterThan 0.03 ?SLOPE))";
        FormulaAST f = new FormulaAST();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        FormulaAST expected = new FormulaAST();
        String expectedString = "(=> (and (instance ?ZONE Object) (instance ?SLOPE Quantity) (instance ?AREA Object)) " +
                "(=> (and (attribute ?AREA LowTerrain) (part ?ZONE ?AREA) (slopeGradient ?ZONE ?SLOPE)) (greaterThan 0.03 ?SLOPE)))";
        expected.read(expectedString);
        Map<String, Set<String>> varmap = fp.findTypeRestrictionsExpr(f.expr, kb);
        FormulaAST actual = new FormulaAST();
        actual.expr = fp.addTypeRestrictionsExpr(f.expr, varmap, SigmaTestBase.kb);
        System.out.println("testAddTypes2(): actual: " + actual);
        System.out.println("testAddTypes2(): expected: " + expected);
        //assertEquals(expected, actual);
        if (expected.logicallyEquals(actual))
            System.out.println("testAddTypes2(): pass");
        else
            System.err.println("testAddTypes2(): fail");
        assertTrue(expected.logicallyEquals(actual));
    }

    /** ***************************************************************
     */
    @Test
    public void testMergeToMap1()   {

        System.out.println("\n============= testMergeToMap1 ==================");
//        Set<String> objectSet1 = Sets.newHashSet("Object", "CorpuscularObject");
//        Set<String> humanSet1 = Sets.newHashSet("Man", "Woman");
        Map<String, Set<String>> map1 = Maps.newHashMap();
        map1.put("?Obj", Sets.newHashSet("Object", "CorpuscularObject"));
        map1.put("?Hum", Sets.newHashSet("Man", "Woman"));
        map1.put("?Time", Sets.newHashSet("Month"));

//        Set<String> objectSet2 = Sets.newHashSet("Object");
//        Set<String> humanSet2 = Sets.newHashSet("Human");
        Map<String, Set<String>> map2 = Maps.newHashMap();
        map2.put("?Obj", Sets.newHashSet("Object"));
        map2.put("?Hum", Sets.newHashSet("Human"));

        Map<String, Set<String>> expectedMap = Maps.newHashMap();
        expectedMap.put("?Obj", Sets.newHashSet("CorpuscularObject"));
        expectedMap.put("?Hum", Sets.newHashSet("Man", "Woman"));
        expectedMap.put("?Time", Sets.newHashSet("Month"));

        Map<String, Set<String>> actualMap = KButilities.mergeToMap(map1, map2, SigmaTestBase.kb);

        System.out.println("testMergeToMap1(): actual: " + actualMap);
        System.out.println("testMergeToMap1(): expected: " + expectedMap);
        if (expectedMap.equals(actualMap))
            System.out.println("testMergeToMap1(): pass");
        else
            System.err.println("testMergeToMap1(): fail");
        assertEquals(expectedMap, actualMap);
    }

    /** ***************************************************************
     */
    @Test
    public void test4() {

        System.out.println("\n============= test4 ==================");
        FormulaPreprocessor fp = new FormulaPreprocessor();
        //FormulaPreprocessor.debug = true;
        String strf = "(forall (?NUMBER ?ELEMENT ?CLASS)\n" +
                "        (=>\n" +
                "          (equal ?ELEMENT\n" +
                "            (ListOrderFn\n" +
                "              (ListFn_1Fn ?FOO) ?NUMBER))\n" +
                "          (instance ?ELEMENT ?CLASS)))";
        FormulaAST f = new FormulaAST();
        f.read(strf);
        Map<String, Set<String>> varmap = fp.findTypeRestrictionsExpr(f.expr, kb);
        String actual = fp.addTypeRestrictionsExpr(f.expr, varmap, kb).toString();
        String expected = "(forall (?NUMBER ?ELEMENT ?CLASS)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (instance ?NUMBER PositiveInteger)\n" +
                "        (instance ?CLASS Class))\n" +
                "      (=>\n" +
                "        (equal ?ELEMENT\n" +
                "          (ListOrderFn\n" +
                "            (ListFn_1Fn ?FOO) ?NUMBER))\n" +
                "        (instance ?ELEMENT ?CLASS))))";
        System.out.println("test4(): actual: " + actual);
        System.out.println("test4(): expected: " + expected);
        FormulaAST fActual = new FormulaAST(actual);
        FormulaAST fExpected = new FormulaAST(expected);
        if (fExpected.deepEquals(fActual))
            System.out.println("test4(): pass");
        else
            System.err.println("test4(): fail");
        assertTrue(fExpected.deepEquals(fActual));
    }

    /** ***************************************************************
     */
    @Test
    public void test5() {

        System.out.println("\n============= test5 ==================");
        FormulaPreprocessor fp = new FormulaPreprocessor();
        String strf = "(<=>\n" +
                "   (equal (RemainderFn ?NUMBER1 ?NUMBER2) ?NUMBER)\n" +
                "   (equal (AdditionFn (MultiplicationFn (FloorFn (DivisionFn ?NUMBER1 ?NUMBER2)) ?NUMBER2) ?NUMBER) ?NUMBER1))";
        FormulaAST f = new FormulaAST();
        f.read(strf);
        //FormulaPreprocessor.debug = true;
        Map<String, Set<String>> varmap = fp.findTypeRestrictionsExpr(f.expr, kb);
        String actual = fp.addTypeRestrictionsExpr(f.expr,varmap, kb).toString();
        String expected = "(=>\n" +
                "    (and\n" +
                "      (instance ?NUMBER1 Integer)\n" +
                "      (instance ?NUMBER2 Integer)\n" +
                "      (instance ?NUMBER Integer) )\n" +
                "    (<=>\n" +
                "   (equal " +
                "     (RemainderFn ?NUMBER1 ?NUMBER2) ?NUMBER)\n" +
                "   (equal \n" +
                "     (AdditionFn \n" +
                "       (MultiplicationFn \n" +
                "         (FloorFn \n" +
                "           (DivisionFn ?NUMBER1 ?NUMBER2)) ?NUMBER2) ?NUMBER) ?NUMBER1)) )";
        System.out.println("test5(): actual: " + actual);
        System.out.println("test5(): expected: " + expected);
        FormulaAST fActual = new FormulaAST(actual);
        FormulaAST fExpected = new FormulaAST(expected);
        if (fExpected.deepEquals(fActual))
            System.out.println("test5(): pass");
        else
            System.err.println("test5(): fail");
        assertTrue(fExpected.deepEquals(fActual));
    }

    /** ***************************************************************
     */
    @Test
    public void test6() {

        System.out.println("\n============= test6 ==================");
        FormulaPreprocessor fp = new FormulaPreprocessor();
        String strf = "(<=>\n" +
                "  (temporalPart ?POS\n" +
                "    (WhenFn ?THING))\n" +
                "  (time ?THING ?POS))";
        FormulaAST f = new FormulaAST();
        f.read(strf);
        //FormulaPreprocessor.debug = true;
        Map<String, Set<String>> varmap = fp.findTypeRestrictionsExpr(f.expr, kb);
        Expr actualExpr = fp.addTypeRestrictionsExpr(f.expr, varmap, kb);
        String expected = "(=>\n" +
                "  (and\n" +
                "    (instance ?POS TimePosition)\n" +
                "    (instance ?THING Physical))\n" +
                "  (<=>\n" +
                "    (temporalPart ?POS\n" +
                "      (WhenFn ?THING))\n" +
                "    (time ?THING ?POS)))";
        FormulaAST fActual = new FormulaAST(actualExpr.toKifString());
        FormulaAST fExpected = new FormulaAST(expected);
        System.out.println("test6(): actual: " + actualExpr);
        System.out.println("test6(): expected: " + expected);
        if (fExpected.deepEquals(fActual))
            System.out.println("test6(): pass");
        else
            System.err.println("test6(): fail");
        assertTrue(fExpected.deepEquals(fActual));
    }

    /** ***************************************************************
     */
    @Test
    public void test7() {

        System.out.println("\n============= test7 ==================");
        FormulaPreprocessor fp = new FormulaPreprocessor();
        String strf = "(<=>\n" +
                "  (temporalPart ?POS\n" +
                "    (WhenFn ?THING))\n" +
                "  (time ?THING ?POS))";
        FormulaAST f = new FormulaAST();
        f.read(strf);
        //FormulaPreprocessor.debug = true;
        Set<Expr> actual = fp.preProcessExpr(f.expr, false, kb);
        String expected = "(=>\n" +
                "  (and\n" +
                "    (instance ?POS TimePosition)\n" +
                "    (instance ?THING Physical))\n" +
                "  (<=>\n" +
                "    (temporalPart ?POS\n" +
                "      (WhenFn ?THING))\n" +
                "    (time ?THING ?POS)))";
        FormulaAST fActual = new FormulaAST(actual.iterator().next().toKifString());
        FormulaAST fExpected = new FormulaAST(expected);
        System.out.println("test7(): actual: " + fActual.expr);
        System.out.println("test7(): expected: " + expected);
        if (fExpected.deepEquals(fActual))
            System.out.println("test7(): pass");
        else
            System.err.println("test7(): fail");
        assertTrue(fExpected.deepEquals(fActual));
    }

    /** ***************************************************************
     */
    @Test
    public void testAbsolute() {

        System.out.println("\n============= testAbsolute ==================");
        FormulaPreprocessor fp = new FormulaPreprocessor();
        String strf = "(equal\n" +
                "  (AbsoluteValueFn ?NUMBER1) ?NUMBER2)";
        FormulaAST f = new FormulaAST();
        f.read(strf);
        //FormulaPreprocessor.debug = true;
        Map<String,Set<String>> actual = fp.findAllTypeRestrictions(f, kb);
        String expected = "{?NUMBER1=[RealNumber], ?NUMBER2=[NonnegativeRealNumber]}";
        System.out.println("testAbsolute(): actual: " + actual);
        System.out.println("testAbsolute(): expected: " + expected);
        if (expected.equals(actual.toString()))
            System.out.println("testAbsolute(): pass");
        else
            System.err.println("testAbsolute(): fail");
        assertEquals(expected,actual.toString());
    }

    /** ***************************************************************
     */
    @Test
    public void testMinValuePreprocess() {

        System.out.println("\n============= testMinValuePreprocess ==================");
        String stmt = "(=> " +
                "(and " +
                "(minValue ?R ?ARG ?N) " +
                "(?R @ARGS) " +
                "(equal ?VAL (ListOrderFn (ListFn @ARGS) ?ARG))) " +
                "(greaterThan ?VAL ?N))";
        FormulaAST f = new FormulaAST();
        f.read(stmt);

        FormulaPreprocessor fp = new FormulaPreprocessor();
        //PredVarInst.debug = true;
        //FormulaPreprocessor.debug = true;
        //RowVars.DEBUG = true;
        System.out.println("testMinValuePreprocess: greaterThanOrEqualTo valence: " +
                kb.kbCache.valences.get("greaterThanOrEqualTo"));
        assertEquals(2, (int) kb.kbCache.valences.get("greaterThanOrEqualTo"));
        Set<Expr> actual = fp.preProcessExpr(f.expr, false, kb);
        System.out.println("testMinValuePreprocess(): actual: " + actual);
        Set<Formula> expected = Sets.newHashSet();
        int expectedSize = 100;
        System.out.println("testMinValuePreprocess(): expected: " + expectedSize);
        if (actual.size() > expectedSize)
            System.out.println("testMinValuePreprocess(): pass");
        else
            System.err.println("testMinValuePreprocess(): fail");
        assertTrue(actual.size() > expectedSize);
    }

    /** ***************************************************************
     */
    @Test
    public void testArgNumsPreprocess() {

        System.out.println("\n============= testArgNumsPreprocess ==================");
        String stmt = "(=>\n" +
                "    (and\n" +
                "        (exactCardinality patient ?ARG 1)\n" +
                "        (instance patient Predicate))\n" +
                "    (exists (?X @ARGS)\n" +
                "        (and\n" +
                "            (patient @ARGS)\n" +
                "            (equal ?X\n" +
                "                (ListOrderFn\n" +
                "                    (ListFn @ARGS) ?ARG))\n" +
                "            (not\n" +
                "                (exists (?Y)\n" +
                "                    (and\n" +
                "                        (equal ?Y\n" +
                "                            (ListOrderFn\n" +
                "                                (ListFn @ARGS) ?ARG))\n" +
                "                        (not\n" +
                "                            (equal ?X ?Y))))))))";
        FormulaAST f = new FormulaAST();
        f.read(stmt);

        FormulaPreprocessor fp = new FormulaPreprocessor();
        Set<Expr> preProcessSet = fp.preProcessExpr(f, false, kb);
        assertTrue(fp.computeVariableTypesExpr(preProcessSet.iterator().next(), kb).size() == 5);
        //PredVarInst.debug = true;
        //FormulaPreprocessor.debug = true;
        // RowVars.DEBUG = true;
        System.out.println("testArgNumsPreprocess: patient valence: " +
                kb.kbCache.valences.get("patient"));
        assertTrue(kb.kbCache.valences.get("patient") == 2);

        List<Formula> forms = RowVars.expandRowVars(kb, new Formula(f.getFormula()));
        System.out.println("testArgNumsPreprocess: forms: " + forms);
    }

    /** ***************************************************************
     */
    @Test
    public void testTVRPreprocess() {

        SUMOformulaToTPTPformula.setLang("tff");
        SUMOKBtoTPTPKB.setLang("tff");
        SUMOKBtoTFAKB skbtfakb = new SUMOKBtoTFAKB();
        skbtfakb.initOnce();
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        String filename = KBmanager.getMgr().getPref("kbDir") + File.separator + kbName + ".tff";
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            skbtfakb.writeSorts(pw);
            //skbtfakb.writeFile(filename, null, false, "", false, pw);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("\n============= testTVRPreprocess ==================");
        String stmt = "\n" +
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
        FormulaAST f = new FormulaAST();
        f.read(stmt);

        FormulaPreprocessor fp = new FormulaPreprocessor();
        //PredVarInst.debug = true;
        //FormulaPreprocessor.debug = true;
        //RowVars.DEBUG = true;
        System.out.println("testTVRPreprocess: greaterThanOrEqualTo valence: " +
                kb.kbCache.valences.get("greaterThanOrEqualTo"));
        Set<Expr> actual = fp.preProcessExpr(f, false, kb);
        System.out.println("testTVRPreprocess(): actual: " + actual);
        Set<Formula> expected = Sets.newHashSet();
        int expectedSize = 30;
        System.out.println("testTVRPreprocess(): actual size: " + actual.size());
        System.out.println("testTVRPreprocess(): expected: " + expectedSize);
        if (actual.size() > expectedSize)
            System.out.println("testTVRPreprocess(): pass");
        else
            System.err.println("testTVRPreprocess(): fail");
        assertTrue(actual.size() > expectedSize);
    }

    /** ***************************************************************
     */
    @Test
    public void testFunctionVariable() {

        System.out.println("\n============= testFunctionVariable ==================");
        String stmt = "(and\n" +
                "  (instance ?F Function)\n" +
                "  (instance ?I (?F ?X)))";
        FormulaAST f = new FormulaAST();
        f.read(stmt);

        FormulaPreprocessor fp = new FormulaPreprocessor();
        Set<Expr> actual = fp.preProcessExpr(f, false, kb);
        System.out.println("testFunctionVariable(): actual: " + actual);
        int expectedSize = 1;
        System.out.println("testFunctionVariable(): expected: " + expectedSize);
        if (actual.size() > expectedSize)
            System.out.println("testFunctionVariable(): pass");
        else
            System.out.println("testFunctionVariable(): fail");
        assertTrue(actual.size() > expectedSize);
    }
}