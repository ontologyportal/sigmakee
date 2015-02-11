package com.articulate.sigma;

import com.google.common.collect.*;
import org.junit.Test;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

/**
 * FormulaPreprocessor tests not focused on findExplicitTypes( ), but requiring that the KBs be loaded.
 */
public class FormulaPreprocessorTest extends UnitTestBase  {

    @Test
    public void testComputeVariableTypesNoVariables()     {
        String stmt = "(domain date 1 Physical)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String,HashSet<String>> actual = formulaPre.computeVariableTypes(f, SigmaTestBase.kb);

        HashMap<String,HashSet<String>> expected = new HashMap<String,HashSet<String>>();

        assertEquals(expected, actual);
    }

    @Test
    public void testComputeVariableTypesNames()     {
        String stmt = "(names \"John\" ?H)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, SigmaTestBase.kb);

        HashSet<String> set = Sets.newHashSet("Entity");
        HashMap<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?H", set);

        assertEquals(expected, actual);
    }


    @Test
    public void testComputeVariableTypesInstance()     {
        String stmt =   "(exists (?D ?H)\n" +
                "           (and\n" +
                "               (instance ?D Driving)\n" +
                "               (instance ?H Human)\n" +
                "               (agent ?D ?H)))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, SigmaTestBase.kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Agent", "Entity");
        expected.put("?H", set1);
        HashSet<String> set2 = Sets.newHashSet("Entity", "Process");
        expected.put("?D", set2);

        assertEquals(expected, actual);
    }

    /**
     * Result shows that the method does not implicitly ID the agent of a Driving as a human or an entity.
     */
    @Test
    public void testComputeVariableTypesInstanceImplicitHuman()     {
        String stmt =   "(exists (?D ?H)\n" +
                "           (and\n" +
                "               (instance ?D Driving)\n" +
                "               (agent ?D ?H)))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, SigmaTestBase.kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Agent");
        expected.put("?H", set1);
        HashSet<String> set2 = Sets.newHashSet("Entity", "Process");
        expected.put("?D", set2);

        assertEquals(expected, actual);
    }


    @Test
    public void testComputeVariableTypesInstanceAgentInstrument()     {
        String stmt = "(exists (?D ?H ?Car)\n" +
                "           (and\n" +
                "               (instance ?D Driving)\n" +
                "               (instance ?H Human)\n" +
                "               (names \"John\" ?H)\n" +
                "               (instance ?Car Automobile)\n" +
                "               (agent ?D ?H)\n" +
                "               (instrument ?D ?Car)))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, SigmaTestBase.kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Agent", "Entity");
        expected.put("?H", set1);
        HashSet<String> set2 = Sets.newHashSet("Entity", "Process");
        expected.put("?D", set2);
        HashSet<String> set3 = Sets.newHashSet("Object", "Entity");
        expected.put("?Car", set3);

        assertEquals(expected, actual);
    }

    @Test
    public void testComputeVariableTypesElementSet()     {
        String stmt =   "(=> " +
                "           (forall (?ELEMENT) " +
                "               (<=> " +
                "                   (element ?ELEMENT ?SET1) " +
                "                   (element ?ELEMENT ?SET2))) " +
                "           (equal ?SET1 ?SET2))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, SigmaTestBase.kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Set", "Entity");
        expected.put("?SET1", set1);
        HashSet<String> set2 = Sets.newHashSet("Set", "Entity");
        expected.put("?SET2", set2);
        HashSet<String> setElement = Sets.newHashSet("Entity");
        expected.put("?ELEMENT", setElement);

        assertEquals(expected, actual);
    }


    @Test
    public void testComputeVariableTypesSubclass()     {
        String stmt =   "(subclass ?Cougar Feline)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, SigmaTestBase.kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("SetOrClass");
        expected.put("?Cougar", set1);

        assertEquals(expected, actual);
    }

    @Test
    public void testComputeVariableTypesMonthFn()     {
        String stmt =   "(exists (?M) " +
                "           (time JohnsBirth (MonthFn ?M (YearFn 2000))))";

        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, SigmaTestBase.kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Month+");
        expected.put("?M", set1);

        assertEquals(expected, actual);
    }

    @Test
    public void testComputeVariableTypesGovFn()     {
        String stmt =   "(exists (?Place) " +
                "   (=> " +
                "       (instance (GovernmentFn ?Place) StateGovernment) " +
                "       (instance ?Place StateOrProvince))) ";

        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, SigmaTestBase.kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("GeopoliticalArea", "Entity");
        expected.put("?Place", set1);

        assertEquals(expected, actual);
    }

    @Test
    public void testComputeVariableTypesSubclassIf()     {
        String stmt =   "(=> " +
                "           (subclass ?Cougar Feline) " +
                "           (subclass ?Cougar Carnivore))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, SigmaTestBase.kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("SetOrClass");
        expected.put("?Cougar", set1);

        assertEquals(expected, actual);
    }

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

    //FIXME: Rename me
    @Test
    public void testFindTypes1() {
        Map<String, HashSet<String>> expected = ImmutableMap.of("?ZONE", Sets.newHashSet("Object"), "?SLOPE", Sets.newHashSet("Quantity"), "?AREA", Sets.newHashSet("Object"));

        String strf = "(=> (and (attribute ?AREA LowTerrain) (part ?ZONE ?AREA)" +
                " (slopeGradient ?ZONE ?SLOPE)) (greaterThan 0.03 ?SLOPE))";
        Formula f = new Formula();
        f.read(strf);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actualMap = fp.computeVariableTypes(f, SigmaTestBase.kb);

        assertEquals(expected, actualMap);

    }

    //FIXME: Rename me
    @Test
    public void testFindTypes3() {
        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?REL", Sets.newHashSet("Entity"));

        String strf = "(<=> (instance ?REL TransitiveRelation) " +
                "(forall (?INST1 ?INST2 ?INST3) " +
                "(=> (and (?REL ?INST1 ?INST2) " +
                "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";
        Formula f = new Formula();
        f.read(strf);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        System.out.println("Var types: " + fp.computeVariableTypes(f, SigmaTestBase.kb));

        HashMap<String, HashSet<String>> actualMap = fp.computeVariableTypes(f, SigmaTestBase.kb);

        assertEquals(expected, actualMap);
    }


    //FIXME: Rename me
    @Test
    public void testFindTypes4() {
        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?SET2", Sets.newHashSet("Set", "Entity"));
        expected.put("?SET1", Sets.newHashSet("Set", "Entity"));
        expected.put("?ELEMENT", Sets.newHashSet("Entity"));

        String strf = "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) " +
                "(element ?ELEMENT ?SET2))) (equal ?SET1 ?SET2))";
        Formula f = new Formula();
        f.read(strf);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        System.out.println("Formula: " + f);
        System.out.println("Var types: " + fp.computeVariableTypes(f, SigmaTestBase.kb));

        HashMap<String, HashSet<String>> actualMap = fp.computeVariableTypes(f, SigmaTestBase.kb);

        assertEquals(expected, actualMap);
    }

    @Test
    public void testFindExplicit() {
        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?REL", Sets.newHashSet("TransitiveRelation"));

        String formStr = "(<=> (instance ?REL TransitiveRelation) " +
                "(forall (?INST1 ?INST2 ?INST3) " +
                "(=> (and (?REL ?INST1 ?INST2) " +
                "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";
        Formula f = new Formula(formStr);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Pattern p = Pattern.compile("\\(instance (\\?[a-zA-Z0-9]+) ([a-zA-Z0-9\\-_]+)");
        Matcher m = p.matcher(formStr);
        m.find();

        assertEquals(expected, fp.findExplicitTypesInAntecedent(f));
    }

    @Test
    public void testAddTypes1() {
        String strf = "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) " +
                "(element ?ELEMENT ?SET2))) (equal ?SET1 ?SET2))";
        Formula f = new Formula();
        f.read(strf);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Formula expected = new Formula();
        String expectedString = "(=> (and (instance ?SET2 Set) (instance ?ELEMENT Entity) (instance ?SET1 Set)) " +
                "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) (element ?ELEMENT ?SET2))) " +
                "(equal ?SET1 ?SET2)))";
        expected.read(expectedString);

        Formula actual = fp.addTypeRestrictionsNew(f, SigmaTestBase.kb);
        //assertTrue("expected: " + expected.toString() + ", but was: " + actual.toString(), expected.equals(actual));
        assertEquals(expected, actual);

    }

    @Test
    public void testAddTypes2() {
        String strf = "(=> (and (attribute ?AREA LowTerrain) (part ?ZONE ?AREA)" +
                " (slopeGradient ?ZONE ?SLOPE)) (greaterThan 0.03 ?SLOPE))";
        Formula f = new Formula();
        f.read(strf);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Formula expected = new Formula();
        String expectedString = "(=> (and (instance ?ZONE Object) (instance ?SLOPE Quantity) (instance ?AREA Object)) " +
                "(=> (and (attribute ?AREA LowTerrain) (part ?ZONE ?AREA) (slopeGradient ?ZONE ?SLOPE)) (greaterThan 0.03 ?SLOPE)))";
        expected.read(expectedString);

        Formula actual = fp.addTypeRestrictionsNew(f, SigmaTestBase.kb);
        //assertTrue("expected: " + expected.toString() + ", but was: " + actual.toString(), expected.equals(actual));
        assertEquals(expected, actual);
    }
}