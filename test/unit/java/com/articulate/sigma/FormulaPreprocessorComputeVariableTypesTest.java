package com.articulate.sigma;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * FormulaPreprocessor tests focused on computeVariableTypes().
 */
public class FormulaPreprocessorComputeVariableTypesTest extends UnitTestBase  {

    @Test
    public void testComputeVariableTypesNoVariables()     {
        String stmt = "(domain date 1 Physical)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String,HashSet<String>> actual = formulaPre.computeVariableTypes(f, SigmaTestBase.kb);

        HashMap<String,HashSet<String>> expected = new HashMap<>();

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
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

    /** ***************************************************************
     */
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
        HashSet<String> set1 = Sets.newHashSet("AutonomousAgent");
        expected.put("?H", set1);
        HashSet<String> set2 = Sets.newHashSet("Process");
        expected.put("?D", set2);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
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
        HashSet<String> set1 = Sets.newHashSet("AutonomousAgent");
        expected.put("?H", set1);
        HashSet<String> set2 = Sets.newHashSet("Process");
        expected.put("?D", set2);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testComputeVariableTypesInstanceAgentInstrument()     {
        String stmt = "(exists (?D ?H ?Car)\n" +
                "           (and\n" +
                "               (instance ?D Driving)\n" +
                "               (instance ?H Human)\n" +
                "               (names \"John\" ?H)\n" +
                "               (instance ?Car Automobile)\n" +
                "               (agent ?D ?H)\n" +
                "               (patient ?D ?Car)))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, SigmaTestBase.kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?H", Sets.newHashSet("AutonomousAgent"));
        expected.put("?D", Sets.newHashSet("Process"));
        expected.put("?Car", Sets.newHashSet("Entity"));

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
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
        HashSet<String> set1 = Sets.newHashSet("Set");
        expected.put("?SET1", set1);
        HashSet<String> set2 = Sets.newHashSet("Set");
        expected.put("?SET2", set2);
        HashSet<String> setElement = Sets.newHashSet("Entity");
        expected.put("?ELEMENT", setElement);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testComputeVariableTypesSubclass()     {
        String stmt =   "(subclass ?Cougar Feline)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, SigmaTestBase.kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Class");
        expected.put("?Cougar", set1);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
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

    /** ***************************************************************
     */
    @Test
    public void testComputeVariableTypesGovFn()     {
        String stmt =   "(=> " +
                "           (instance (GovernmentFn ?Place) StateGovernment) " +
                "           (instance ?Place StateOrProvince))) ";

        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, SigmaTestBase.kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("GeopoliticalArea");
        expected.put("?Place", set1);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
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
        HashSet<String> set1 = Sets.newHashSet("Class");
        expected.put("?Cougar", set1);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testComputeVariableTypesLowTerrain() {
        Map<String, HashSet<String>> expected = ImmutableMap.of("?ZONE", Sets.newHashSet("Object"),
                "?SLOPE", Sets.newHashSet("RealNumber"), "?AREA", Sets.newHashSet("Object"));

        String stmt = "(=> (and (attribute ?AREA LowTerrain) (part ?ZONE ?AREA)" +
                " (slopeGradient ?ZONE ?SLOPE)) (greaterThan 0.03 ?SLOPE))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actualMap = fp.computeVariableTypes(f, SigmaTestBase.kb);

        assertEquals(expected, actualMap);

    }

    /** ***************************************************************
     */
    @Test
    public void testComputeVariableTypesIfAndOnlyIfTransitiveRelation() {
        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?REL", Sets.newHashSet("Entity"));

        String stmt = "(<=> (instance ?REL TransitiveRelation) " +
                "(forall (?INST1 ?INST2 ?INST3) " +
                "(=> (and (?REL ?INST1 ?INST2) " +
                "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        System.out.println("Var types: " + fp.computeVariableTypes(f, SigmaTestBase.kb));

        HashMap<String, HashSet<String>> actualMap = fp.computeVariableTypes(f, SigmaTestBase.kb);

        assertEquals(expected, actualMap);
    }

    /** ***************************************************************
     */
    @Test
    public void testComputeVariableTypesForAllElementSet() {
        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?SET2", Sets.newHashSet("Set"));
        expected.put("?SET1", Sets.newHashSet("Set"));
        expected.put("?ELEMENT", Sets.newHashSet("Entity"));

        String stmt = "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) " +
                "(element ?ELEMENT ?SET2))) (equal ?SET1 ?SET2))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        System.out.println("Formula: " + f);
        System.out.println("Var types: " + fp.computeVariableTypes(f, SigmaTestBase.kb));

        HashMap<String, HashSet<String>> actualMap = fp.computeVariableTypes(f, SigmaTestBase.kb);

        assertEquals(expected, actualMap);
    }

    /** ***************************************************************
     */
    @Test
    public void testComputeVariableTypesAwake() {
        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?HUMAN", Sets.newHashSet("AutonomousAgent"));
        expected.put("?PROC", Sets.newHashSet("Process"));

        String stmt =   "(=>\n" +
                "           (and\n" +
                "               (instance ?PROC IntentionalProcess)\n" +
                "               (agent ?PROC ?HUMAN)\n" +
                "               (instance ?HUMAN Animal))\n" +
                "           (holdsDuring\n" +
                "               (WhenFn ?PROC)\n" +
                "               (attribute ?HUMAN Awake)))";
        Formula f = new Formula(stmt);

        FormulaPreprocessor fp = new FormulaPreprocessor();
        System.out.println("Formula: " + f);
        System.out.println("Var types: " + fp.computeVariableTypes(f, SigmaTestBase.kb));

        HashMap<String, HashSet<String>> actualMap = fp.computeVariableTypes(f, SigmaTestBase.kb);

        assertEquals(expected, actualMap);
    }

}