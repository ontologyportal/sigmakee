package com.articulate.sigma;

import com.google.common.collect.*;
import org.junit.*;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * FormulaPreprocessor tests not focused on findExplicitTypes( ) or computeVariableTypes( ).
 */
public class FormulaPreprocessorTest extends UnitTestBase  {

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
        assertEquals(expected, actual);
//        assertTrue(expected.logicallyEquals(actual));
    }

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
        //assertEquals(expected, actual);
        assertTrue(expected.logicallyEquals(actual));
    }

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

}