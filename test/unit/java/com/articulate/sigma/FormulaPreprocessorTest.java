package com.articulate.sigma;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * FormulaPreprocessor tests not focused on findExplicitTypes( ), but requiring that the KBs be loaded.
 */
public class FormulaPreprocessorTest extends SigmaTestBase  {

    @Test
    public void testComputeVariableTypesNoVariables()     {
        String stmt = "(domain date 1 Physical)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String,HashSet<String>> actual = formulaPre.computeVariableTypes(f, kb);

        HashMap<String,HashSet<String>> expected = new HashMap<String,HashSet<String>>();

        assertEquals(expected, actual);
    }

    @Test
    public void testComputeVariableTypesNames()     {
        String stmt = "(names \"John\" ?H)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, kb);

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
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, kb);

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
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, kb);

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
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, kb);

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
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, kb);

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
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, kb);

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
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, kb);

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
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, kb);

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
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("SetOrClass");
        expected.put("?Cougar", set1);

        assertEquals(expected, actual);
    }

    @Test
    public void testFindCaseRolesComplex()   {
        String stmt =   "(exists (?D ?H)\n" +
                "           (and\n" +
                "               (instance ?D Driving)\n" +
                "               (instance ?H Human)\n" +
                "               (agent ?D ?H)))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor formulaPre = new FormulaPreprocessor();

        List<SumoProcess> actualResult = formulaPre.findCaseRoles(f, kb);

        SumoProcess expected = new SumoProcess(kb, "agent", "Driving Human");

        assertEquals(1, actualResult.size());
        assertEquals(expected.toString(), actualResult.get(0).toString());

        String expectNLG = "A human drives.";
        assertEquals(expectNLG, actualResult.get(0).toNaturalLanguage());
    }

    // TODO: Technically, this should to in the FormulaTest class, but the gatherRelationsWithArgTypes( ) method requires a KB
    // and none of the other tests in that class do. Maybe move the method to FormulaPreprocessor--it's the only Formula method
    // requiring a KB.
    @Test
    public void testGatherRelationships()   {
        String stmt = "(agent Leaving Human)";
        Formula f = new Formula();
        f.read(stmt);

        HashMap<String, ArrayList> actualMap = f.gatherRelationsWithArgTypes(kb);

        List<String> expectedList = Lists.newArrayList(null, "Process", "Agent", null, null, null, null, null);
        HashMap<String, List<String>> expectedMap = Maps.newHashMap();
        expectedMap.put("agent", expectedList);

        assertEquals(expectedMap, actualMap);
    }
}