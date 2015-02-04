package com.articulate.sigma;

import com.google.common.collect.*;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * FormulaPreprocessor tests specifically targeted on the findExplicitTypes( ) and findExplicitTypesInAntecedent( ) methods.
 */
public class FormulaPreprocessorFindExplicitTypesTest extends UnitTestBase  {

    @Test
    public void testFindExplicitTypesDomainNotRule()     {
        String stmt = "(domain date 1 Physical)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypesInAntecedent(f);

        assertEquals(null, actual);
    }

    @Test
    public void testFindExplicitTypesDomainNotRule2()     {
        String stmt = "(domain date 1 Physical)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypes(f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();

        assertEquals(expected, actual);
    }

    @Test
    public void testFindExplicitTypesNames()     {
        String stmt = "(names \"John\" ?H)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypesInAntecedent(f);

        assertEquals(null, actual);
    }

    @Test
    public void testFindExplicitTypesNames2()     {
        String stmt = "(names \"John\" ?H)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypes(f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();

        assertEquals(expected, actual);
    }

    @Test
    public void testFindExplicitTypesSubclass()     {
        String stmt = "(subclass ?Cougar Feline)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypesInAntecedent(f);

        assertEquals(null, actual);
    }

    @Test
    public void testFindExplicitTypesSubclass2()     {
        String stmt = "(subclass ?Cougar Feline)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypes(f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Feline+");
        expected.put("?Cougar", set1);

        assertEquals(expected, actual);
    }


    @Test
    public void testFindExplicitTypesSubclassInRule()     {
        String stmt =   "(=> " +
                            "(subclass ?C Feline) "  +
                            "(subclass ?C Carnivore))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypesInAntecedent(f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Feline+");
        expected.put("?C", set1);

        assertEquals(expected, actual);
    }

    @Test
    public void testFindExplicitTypesSubclassInRule2()     {
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
        HashMap<String,HashSet<String>> actual = formulaPre.findExplicitTypesInAntecedent(f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("TransitiveRelation");
        expected.put("?REL", set1);

        assertEquals(expected, actual);
    }

    @Test
    public void testFindExplicitTransitiveRelation2()     {
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
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypesInAntecedent(f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Human");
        expected.put("?S", set1);
        HashSet<String> set2 = Sets.newHashSet("Human");
        expected.put("?J", set2);

        assertEquals(expected, actual);
    }

    @Test
    public void testFindExplicitTypesIfJohnLikesSue2()     {
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
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypesInAntecedent(f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Entity");
        expected.put("?PHYS", set1);

        assertEquals(expected, actual);
    }

    @Test
    public void testFindExplicitTypesIfAndOnlyIfEntity2()     {
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
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypesInAntecedent(f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Automobile");
        expected.put("?PHYS", set1);

        assertEquals(expected, actual);
    }

    @Test
    public void testFindExplicitTypesIfAndOnlyIfAutomobile2()     {
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

    @Test
    public void testTransitiveRelation()     {
        String stmt = "(<=> (instance ?REL TransitiveRelation) " +
                            "(forall (?INST1 ?INST2 ?INST3) " +
                                "(=> (and (?REL ?INST1 ?INST2) " +
                                    "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";

        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypesInAntecedent(f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?REL", Sets.newHashSet("TransitiveRelation"));
        assertEquals(expected, actual);
    }

    @Test
    public void testTransitiveRelation2()     {
        String stmt =   "(<=> (instance ?REL TransitiveRelation) " +
                            "(forall (?INST1 ?INST2 ?INST3) " +
                                "(=> (and (?REL ?INST1 ?INST2) " +
                                "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";

        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?REL", Sets.newHashSet("TransitiveRelation"));
        assertEquals(expected, actual);
    }

    @Test
    public void testFindExplicitTypesAgent()     {
        String stmt = "(agent Leaving Human)";

        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypesInAntecedent(f);

        assertEquals(null, actual);
    }

    @Test
    public void testFindExplicitTypesAgent2()     {
        String stmt = "(agent Leaving Human)";

        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        assertEquals(expected, actual);
    }

    @Test
    public void testFindExplicitTypesHumanDriving()     {
        String stmt =   "(exists (?D ?H)\n" +
                "           (and\n" +
                "               (instance ?D Driving)\n" +
                "               (instance ?H Human)\n" +
                "               (agent ?D ?H)))";

        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypesInAntecedent(f);

        assertEquals(null, actual);
    }

    @Test
    public void testFindExplicitTypesHumanDriving2() {
        String stmt = "(exists (?D ?H)\n" +
                "           (and\n" +
                "               (instance ?D Driving)\n" +
                "               (instance ?H Human)\n" +
                "               (agent ?D ?H)))";

        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?D", Sets.newHashSet("Driving"));
        expected.put("?H", Sets.newHashSet("Human"));
        assertEquals(expected, actual);

    }

}