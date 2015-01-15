package com.articulate.sigma;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * LanguageFormatter tests specifically targeted toward the htmlParaphrase( ) method.
 */
public class FormulaPreprocessorTest extends SigmaTestBase  {
    private static KB kb;

    @BeforeClass
    public static void setup()  {
        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        kb = KBmanager.getMgr().getKB("SUMO");
    }


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

    @Ignore
    @Test
    public void testComputeVariableTypesMonthFn()     {
        String stmt =   "(exists (?M) " +
                "           (time JohnsBirth (MonthFn ?M (YearFn 2000))))";

        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("SetOrClass");
        expected.put("?Cougar", set1);

        assertEquals(expected, actual);
    }

    @Ignore
    @Test
    public void testComputeVariableTypesGovFn()     {
        String stmt =   "(exists (?Place) " +
                "           (subclass (GovernmentFn ?Place) StateGovernment))";

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
    public void testComputeVariableTypesTypicalPart()     {
        String stmt =   "(=> " +
                            "(typicalPart ?X ?Y) " +
                            "(subclass ?Y Object))";

        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.computeVariableTypes(f, kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("SetOrClass");
        expected.put("?Y", set1);

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
    public void testFindExplicitTypesDomainNotRule()     {
        String stmt = "(domain date 1 Physical)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String,HashSet<String>> actual = formulaPre.findExplicitTypes(f);

        assertEquals(null, actual);
    }

    @Test
    public void testFindExplicitTypesNames()     {
        String stmt = "(names \"John\" ?H)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypes(f);

        assertEquals(null, actual);
    }

    @Test
    public void testFindExplicitTypesSubclass()     {
        String stmt = "(subclass ?Cougar Feline)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypes(f);

        assertEquals(null, actual);
    }


    @Test
    public void testFindExplicitTypesSubclassInRule()     {
        String stmt =   "(=> " +
                            "(subclass ?C Feline) "  +
                            "(subclass ?C Carnivore))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypes(f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Feline+", "Carnivore+");
        expected.put("?C", set1);

        assertEquals(expected, actual);
    }


    @Test
    public void testFindExplicitTransitiveRelation()     {
        String stmt =   "(<=> (instance ?REL TransitiveRelation) " +
                "(forall (?INST1 ?INST2 ?INST3) " +
                "(=> (and (?REL ?INST1 ?INST2) " +
                "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String,HashSet<String>> actual = formulaPre.findExplicitTypes(f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("TransitiveRelation");
        expected.put("?REL", set1);

        assertEquals(expected, actual);
    }

    @Test
    public void testFindExplicitTypesIfJohnLikesSue()     {
        String stmt =   "(=>\n" +
                "           (and\n" +
                "               (instance ?J Human)\n" +
                "               (instance ?S Human)\n" +
                "               (names \"John\" ?J)\n" +
                "               (names \"Sue\" ?S)\n" +
                "               (likes ?J ?S))\n" +
                "            (like ?S ?J))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypes(f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Human");
        expected.put("?S", set1);
        HashSet<String> set2 = Sets.newHashSet("Human");
        expected.put("?J", set2);

        assertEquals(expected, actual);
    }

    @Test
    public void testFindExplicitTypesIfAndOnlyIfEntity()     {
        String stmt = "(<=> (instance ?PHYS Entity) (exists (?LOC ?TIME) (and (located ?PHYS ?LOC) (time ?PHYS ?TIME))))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypes(f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Entity");
        expected.put("?PHYS", set1);

        assertEquals(expected, actual);
    }

    @Test
    public void testFindExplicitTypesIfAndOnlyIfAutomobile()     {
        String stmt = "(<=> (instance ?PHYS Automobile) (exists (?LOC ?TIME) (and (located ?PHYS ?LOC) (time ?PHYS ?TIME))))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypes(f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Automobile");
        expected.put("?PHYS", set1);

        assertEquals(expected, actual);
    }

}