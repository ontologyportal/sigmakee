package com.articulate.sigma;

//This software is released under the GNU Public License
//<http://www.gnu.org/copyleft/gpl.html>.
// Copyright 2019 Infosys
// adam.pease@infosys.com

import com.google.common.collect.*;
import org.junit.*;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * FormulaPreprocessor tests not focused on findExplicitTypes( ) or computeVariableTypes( ).
 */
public class FormulaPreprocessorTest extends UnitTestBase  {

    /** ***************************************************************
     */
    // TODO: Technically, this should to in the FormulaTest class, but the gatherRelationsWithArgTypes( ) method requires a KB
    // and none of the other tests in that class do. Maybe move the method to FormulaPreprocessor--it's the only Formula method
    // requiring a KB.
    @Test
    public void testGatherRelationships()   {

        String stmt = "(agent Leaving Human)";
        Formula f = new Formula();
        f.read(stmt);

        HashMap<String, ArrayList> actualMap = f.gatherRelationsWithArgTypes(SigmaTestBase.kb);

        ArrayList<String> expectedList = Lists.newArrayList(null, "Process", "Agent", null, null, null, null, null);
        HashMap<String, ArrayList> expectedMap = Maps.newHashMap();
        expectedMap.put("agent", expectedList);

        assertEquals(expectedMap, actualMap);
    }

    /** ***************************************************************
     */
    // FIXME: test is waiting completion of Formula.logicallyEquals()
    @Ignore
    @Test
    public void testAddTypes1() {

        String stmt = "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) " +
                "(element ?ELEMENT ?SET2))) (equal ?SET1 ?SET2))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Formula expected = new Formula();
        String expectedString = "(=> (and (instance ?SET2 Set) (instance ?SET1 Set)) " +
                "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) (element ?ELEMENT ?SET2))) " +
                "(equal ?SET1 ?SET2)))";
        expected.read(expectedString);

        Formula actual = fp.addTypeRestrictions(f, SigmaTestBase.kb);
        System.out.println("testAddTypes1(): actual: " + actual);
        System.out.println("testAddTypes1(): expected: " + expected);
        assertEquals(expected, actual);
        assertTrue(expected.logicallyEquals(actual));
    }

    /** ***************************************************************
     */
    // FIXME: test is waiting completion of Formula.logicallyEquals()
    @Ignore
    @Test
    public void testAddTypes2() {

        String stmt = "(=> (and (attribute ?AREA LowTerrain) (part ?ZONE ?AREA)" +
                " (slopeGradient ?ZONE ?SLOPE)) (greaterThan 0.03 ?SLOPE))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Formula expected = new Formula();
        String expectedString = "(=> (and (instance ?ZONE Object) (instance ?SLOPE Quantity) (instance ?AREA Object)) " +
                "(=> (and (attribute ?AREA LowTerrain) (part ?ZONE ?AREA) (slopeGradient ?ZONE ?SLOPE)) (greaterThan 0.03 ?SLOPE)))";
        expected.read(expectedString);

        Formula actual = fp.addTypeRestrictions(f, SigmaTestBase.kb);
        System.out.println("testAddTypes2(): actual: " + actual);
        System.out.println("testAddTypes2(): expected: " + expected);
        //assertEquals(expected, actual);
        assertTrue(expected.logicallyEquals(actual));
    }

    /** ***************************************************************
     */
    @Test
    public void testMergeToMap1()   {

//        Set<String> objectSet1 = Sets.newHashSet("Object", "CorpuscularObject");
//        Set<String> humanSet1 = Sets.newHashSet("Man", "Woman");
        HashMap<String, HashSet<String>> map1 = Maps.newHashMap();
        map1.put("?Obj", Sets.newHashSet("Object", "CorpuscularObject"));
        map1.put("?Hum", Sets.newHashSet("Man", "Woman"));
        map1.put("?Time", Sets.newHashSet("Month"));

//        Set<String> objectSet2 = Sets.newHashSet("Object");
//        Set<String> humanSet2 = Sets.newHashSet("Human");
        HashMap<String, HashSet<String>> map2 = Maps.newHashMap();
        map2.put("?Obj", Sets.newHashSet("Object"));
        map2.put("?Hum", Sets.newHashSet("Human"));

        HashMap<String, HashSet<String>> expectedMap = Maps.newHashMap();
        expectedMap.put("?Obj", Sets.newHashSet("CorpuscularObject"));
        expectedMap.put("?Hum", Sets.newHashSet("Man", "Woman"));
        expectedMap.put("?Time", Sets.newHashSet("Month"));

        HashMap<String, HashSet<String>> actualMap = FormulaPreprocessor.mergeToMap(map1, map2, SigmaTestBase.kb);

        assertEquals(expectedMap, actualMap);
    }

    /** ***************************************************************
     */
    @Test
    public void test4() {

        System.out.println("============= test4 ==================");
        FormulaPreprocessor fp = new FormulaPreprocessor();
        String strf = "(forall (?NUMBER ?ELEMENT ?CLASS)\n" +
                "        (=>\n" +
                "          (equal ?ELEMENT\n" +
                "            (ListOrderFn\n" +
                "              (ListFn_1 ?FOO) ?NUMBER))\n" +
                "          (instance ?ELEMENT ?CLASS)))";
        Formula f = new Formula();
        f.read(strf);
        fp = new FormulaPreprocessor();
        String actual = fp.addTypeRestrictions(f, kb).toString();
        String expected = "(=>\n" +
                "  (and\n" +
                "    (instance ?FOO Entity) )\n" +
                "  (forall (?NUMBER ?ELEMENT ?CLASS)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (instance ?NUMBER PositiveInteger)\n" +
                "        (instance ?ELEMENT Entity)\n" +
                "        (instance ?CLASS SetOrClass) )\n" +
                "      (=>\n" +
                "        (equal ?ELEMENT\n" +
                "          (ListOrderFn\n" +
                "            (ListFn_1 ?FOO) ?NUMBER) )\n" +
                "        (instance ?ELEMENT ?CLASS) ))))";
        System.out.println("test4(): actual: " + actual);
        System.out.println("test4(): expected: " + expected);
        Formula fActual = new Formula(actual);
        Formula fExpected = new Formula(expected);
        assertTrue(fExpected.deepEquals(fActual));
    }

    /** ***************************************************************
     */
    @Test
    public void test5() {

        FormulaPreprocessor fp = new FormulaPreprocessor();
        String strf = "(<=>\n" +
                "   (equal (RemainderFn ?NUMBER1 ?NUMBER2) ?NUMBER)\n" +
                "   (equal (AdditionFn (MultiplicationFn (FloorFn (DivisionFn ?NUMBER1 ?NUMBER2)) ?NUMBER2) ?NUMBER) ?NUMBER1))";
        Formula f = new Formula();
        f.read(strf);
        fp = new FormulaPreprocessor();
        FormulaPreprocessor.debug = true;
        String actual = fp.addTypeRestrictions(f, kb).toString();
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
        Formula fActual = new Formula(actual);
        Formula fExpected = new Formula(expected);
        assertTrue(fExpected.deepEquals(fActual));
    }

    /** ***************************************************************
     */
    @Test
    public void test6() {

        FormulaPreprocessor fp = new FormulaPreprocessor();
        String strf = "(<=>\n" +
                "  (temporalPart ?POS\n" +
                "    (WhenFn ?THING))\n" +
                "  (time ?THING ?POS))";
        Formula f = new Formula();
        f.read(strf);
        fp = new FormulaPreprocessor();
        FormulaPreprocessor.debug = true;
        String actual = fp.addTypeRestrictions(f, kb).toString();
        String expected = "(=>\n" +
                "  (and\n" +
                "    (instance ?POS TimePosition)\n" +
                "    (instance ?THING Physical) )\n" +
                "  (<=>\n" +
                "    (temporalPart ?POS\n" +
                "      (WhenFn ?THING))\n" +
                "    (time ?THING ?POS) ))";
        System.out.println("test6(): actual: " + actual);
        System.out.println("test6(): expected: " + expected);
        assertEquals(expected,actual);
    }

    /** ***************************************************************
     */
    @Test
    public void test7() {

        FormulaPreprocessor fp = new FormulaPreprocessor();
        String strf = "(<=>\n" +
                "  (temporalPart ?POS\n" +
                "    (WhenFn ?THING))\n" +
                "  (time ?THING ?POS))";
        Formula f = new Formula();
        f.read(strf);
        fp = new FormulaPreprocessor();
        FormulaPreprocessor.debug = true;
        Set<Formula> actual = fp.preProcess(f, false, kb);
        String expected = "(=>\n" +
                "  (and\n" +
                "    (instance ?POS TimePosition)\n" +
                "    (instance ?THING Physical) )\n" +
                "  (<=>\n" +
                "    (temporalPart ?POS\n" +
                "      (WhenFn ?THING))\n" +
                "    (time ?THING ?POS) ))";
        System.out.println("test7(): actual: " + actual);
        System.out.println("test7(): expected: " + expected);
        assertEquals(expected,actual.iterator().next().toString());
    }

    /** ***************************************************************
     */
    @Test
    public void testAbsolute() {

        FormulaPreprocessor fp = new FormulaPreprocessor();
        String strf = "(equal\n" +
                "  (AbsoluteValueFn ?NUMBER1) ?NUMBER2)";
        Formula f = new Formula();
        f.read(strf);
        fp = new FormulaPreprocessor();
        FormulaPreprocessor.debug = true;
        HashMap<String,HashSet<String>> actual = fp.findAllTypeRestrictions(f, kb);
        String expected = "{?NUMBER1=[RealNumber], ?NUMBER2=[Entity, NonnegativeRealNumber]}";
        System.out.println("testAbsolute(): actual: " + actual);
        System.out.println("testAbsolute(): expected: " + expected);
        assertEquals(expected,actual.toString());
    }
}