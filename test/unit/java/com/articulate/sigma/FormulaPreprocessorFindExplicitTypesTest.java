package com.articulate.sigma;

//This software is released under the GNU Public License
//<http://www.gnu.org/copyleft/gpl.html>.
// Copyright 2019 Infosys
// adam.pease@infosys.com

import com.google.common.collect.*;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * FormulaPreprocessor tests specifically targeted on the findExplicitTypes( ) and findExplicitTypesInAntecedent( ) methods.
 */
public class FormulaPreprocessorFindExplicitTypesTest extends UnitTestBase  {

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypesDomainNotRule() {

        String stmt = "(domain date 1 Physical)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypesInAntecedent(SigmaTestBase.kb,f);

        assertEquals(null, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypesDomainNotRule2() {

        String stmt = "(domain date 1 Physical)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypesNames() {

        String stmt = "(names \"John\" ?H)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypesInAntecedent(SigmaTestBase.kb,f);

        assertEquals(null, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypesNames2() {

        String stmt = "(names \"John\" ?H)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypesSubclass() {

        String stmt = "(subclass ?Cougar Feline)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypesInAntecedent(SigmaTestBase.kb,f);

        assertEquals(null, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypesSubclass2() {

        String stmt = "(subclass ?Cougar Feline)";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Feline+");
        expected.put("?Cougar", set1);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypesSubclassInRule() {

        String stmt =   "(=> " +
                            "(subclass ?C Feline) "  +
                            "(subclass ?C Carnivore))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypesInAntecedent(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Feline+");
        expected.put("?C", set1);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypesSubclassInRule2() {

        String stmt =   "(=> " +
                            "(subclass ?C Feline) "  +
                            "(subclass ?C Carnivore))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Feline+", "Carnivore+");
        expected.put("?C", set1);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTransitiveRelation() {

        String stmt =   "(<=> (instance ?REL TransitiveRelation) " +
                            "(forall (?INST1 ?INST2 ?INST3) " +
                                "(=> (and (?REL ?INST1 ?INST2) " +
                                "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String,HashSet<String>> actual = formulaPre.findExplicitTypesInAntecedent(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("TransitiveRelation");
        expected.put("?REL", set1);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTransitiveRelation2() {

        String stmt =   "(<=> (instance ?REL TransitiveRelation) " +
                            "(forall (?INST1 ?INST2 ?INST3) " +
                                "(=> (and (?REL ?INST1 ?INST2) " +
                                "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String,HashSet<String>> actual = formulaPre.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("TransitiveRelation");
        expected.put("?REL", set1);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypesIfJohnLikesSue() {

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
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypesInAntecedent(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Human");
        expected.put("?S", set1);
        HashSet<String> set2 = Sets.newHashSet("Human");
        expected.put("?J", set2);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
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
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Human");
        expected.put("?S", set1);
        HashSet<String> set2 = Sets.newHashSet("Human");
        expected.put("?J", set2);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypesIfAndOnlyIfEntity()     {
        String stmt = "(<=> (instance ?PHYS Entity) (exists (?LOC ?TIME) (and (located ?PHYS ?LOC) (time ?PHYS ?TIME))))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypesInAntecedent(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Entity");
        expected.put("?PHYS", set1);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypesIfAndOnlyIfEntity2()     {
        String stmt = "(<=> (instance ?PHYS Entity) (exists (?LOC ?TIME) (and (located ?PHYS ?LOC) (time ?PHYS ?TIME))))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Entity");
        expected.put("?PHYS", set1);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypesIfAndOnlyIfAutomobile()     {
        String stmt = "(<=> (instance ?PHYS Automobile) (exists (?LOC ?TIME) (and (located ?PHYS ?LOC) (time ?PHYS ?TIME))))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypesInAntecedent(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Automobile");
        expected.put("?PHYS", set1);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypesIfAndOnlyIfAutomobile2()     {
        String stmt = "(<=> (instance ?PHYS Automobile) (exists (?LOC ?TIME) (and (located ?PHYS ?LOC) (time ?PHYS ?TIME))))";
        Formula f = new Formula();
        f.read(stmt);

        FormulaPreprocessor formulaPre = new FormulaPreprocessor();
        HashMap<String, HashSet<String>> actual = formulaPre.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        HashSet<String> set1 = Sets.newHashSet("Automobile");
        expected.put("?PHYS", set1);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testTransitiveRelation()     {
        String stmt = "(<=> (instance ?REL TransitiveRelation) " +
                            "(forall (?INST1 ?INST2 ?INST3) " +
                                "(=> (and (?REL ?INST1 ?INST2) " +
                                    "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";

        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypesInAntecedent(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?REL", Sets.newHashSet("TransitiveRelation"));
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testTransitiveRelation2()     {
        String stmt =   "(<=> (instance ?REL TransitiveRelation) " +
                            "(forall (?INST1 ?INST2 ?INST3) " +
                                "(=> (and (?REL ?INST1 ?INST2) " +
                                "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";

        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?REL", Sets.newHashSet("TransitiveRelation"));
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypesAgent()     {
        String stmt = "(agent Leaving Human)";

        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypesInAntecedent(SigmaTestBase.kb,f);

        assertEquals(null, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypesAgent2()     {
        String stmt = "(agent Leaving Human)";

        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
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

        Map<String, HashSet<String>> actual = fp.findExplicitTypesInAntecedent(SigmaTestBase.kb,f);

        assertEquals(null, actual);
    }

    /** ***************************************************************
     */
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

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?D", Sets.newHashSet("Driving"));
        expected.put("?H", Sets.newHashSet("Human"));
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypes1() {
        String stmt = "(=>\n" +
                "    (subjectiveAttribute ?ENTITY ?ATTR ?AGENT)\n" +
                "    (forall (?RATE)\n" +
                "        (and\n" +
                "            (instance ?RATE Classifying)\n" +
                "            (agent ?RATE ?AGENT)\n" +
                "            (patient ?RATE ?ATTR)\n" +
                "            (destination ?RATE ?ENTITY))))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?RATE", Sets.newHashSet("Classifying"));
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypes2() {
        String stmt = "(=>\n" +
                "   (and\n" +
                "      (instance ?MIXTURE Mixture)\n" +
                "      (part ?SUBSTANCE ?MIXTURE)\n" +
                "      (not (instance ?SUBSTANCE Mixture)))\n" +
                "   (instance ?SUBSTANCE PureSubstance))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?MIXTURE", Sets.newHashSet("Mixture"));
        expected.put("?SUBSTANCE", Sets.newHashSet("PureSubstance"));
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypes3() {
        String stmt = "(=>\n" +
                "    (instance ?ANIMAL Animal)\n" +
                "    (or\n" +
                "        (exists (?MOTION)\n" +
                "            (and\n" +
                "                (instance ?MOTION BodyMotion)\n" +
                "                (agent ?MOTION ?ANIMAL)))\n" +
                "        (exists (?ATTR)\n" +
                "            (and\n" +
                "                (instance ?ATTR BodyPosition)\n" +
                "                (attribute ?ANIMAL ?ATTR)))))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?ANIMAL", Sets.newHashSet("Animal"));
        expected.put("?MOTION", Sets.newHashSet("BodyMotion"));
        expected.put("?ATTR", Sets.newHashSet("BodyPosition"));
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypes4() {
        String stmt = "(=>\n" +
                "   (and\n" +
                "      (graphPart ?PATH ?GRAPH)\n" +
                "      (not (instance ?GRAPH DirectedGraph)))\n" +
                "   (<=>\n" +
                "      (equal (GraphPathFn ?NODE1 ?NODE2) ?PATH)\n" +
                "      (equal (GraphPathFn ?NODE2 ?NODE1) ?PATH)))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypes5() {
        String stmt = "(=>\n" +
                "   (and\n" +
                "      (instance ?MONTH (MonthFn February ?YEAR))\n" +
                "      (instance ?Y ?YEAR)\n" +
                "      (not (instance ?Y LeapYear)))\n" +
                "   (duration ?MONTH (MeasureFn 28 DayDuration)))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        //expected.put("?Y", new HashSet<String>());
        // TODO: add explicit type "Month" for ?MONTH
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypes6() {
        String stmt = "(=>\n" +
                "   (instance ?CLOUD WaterCloud)\n" +
                "   (forall (?PART)\n" +
                "      (=>\n" +
                "         (and\n" +
                "            (part ?PART ?CLOUD)\n" +
                "            (not (instance ?PART Water)))\n" +
                "         (exists (?WATER)\n" +
                "            (and\n" +
                "               (instance ?WATER Water)\n" +
                "               (part ?WATER ?CLOUD)\n" +
                "               (measure ?WATER ?MEASURE1)\n" +
                "               (measure ?PART ?MEASURE2)\n" +
                "               (greaterThan ?MEASURE1 ?MEASURE2))))))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?CLOUD", Sets.newHashSet("WaterCloud"));
        expected.put("?WATER", Sets.newHashSet("Water"));
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypes7() {
        String stmt = "(=>\n" +
                "   (and\n" +
                "      (instance ?LANG AnimalLanguage)\n" +
                "      (agent ?PROC ?AGENT)\n" +
                "      (instrument ?PROC ?LANG))\n" +
                "   (and\n" +
                "      (instance ?AGENT Animal)\n" +
                "      (not (instance ?AGENT Human))))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?LANG", Sets.newHashSet("AnimalLanguage"));
        expected.put("?AGENT", Sets.newHashSet("Animal"));
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypes8() {
        String stmt = "(=>\n" +
                "   (instance ?LANG ConstructedLanguage)\n" +
                "   (exists (?PLAN)\n" +
                "      (and\n" +
                "         (instance ?PLAN Planning)\n" +
                "         (result ?PLAN ?LANG))))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?LANG", Sets.newHashSet("ConstructedLanguage"));
        expected.put("?PLAN", Sets.newHashSet("Planning"));
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypes9() {
        String stmt = "(=>\n" +
                "   (and\n" +
                "      (instance ?PUB Publication)\n" +
                "      (patient ?PUB ?TEXT))\n" +
                "   (subclass ?TEXT Text))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?PUB", Sets.newHashSet("Publication"));
        expected.put("?TEXT", Sets.newHashSet("Text+"));
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypes10() {
        String stmt = "(=>\n" +
                "  (and\n" +
                "    (instance ?X ?Y)\n" +
                "    (subclass ?Y PureSubstance)\n" +
                "    (barometricPressure ?X (MeasureFn ?PRES InchMercury))\n" +
                "    (greaterThan 29.92 ?PRES)\n" +
                "    (boilingPoint ?Y (MeasureFn ?BOIL KelvinDegree))\n" +
                "    (measure ?X (MeasureFn ?TEMP KelvinDegree))\n" +
                "    (greaterThan ?TEMP ?BOIL))\n" +
                "  (attribute ?X Gas))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        //expected.put("?X", new HashSet<String>());
        expected.put("?Y", Sets.newHashSet("PureSubstance+"));
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypes11() {
        String stmt = "(=>\n" +
                "   (and\n" +
                "      (instance ?BOILING Boiling)\n" +
                "      (boilingPoint ?TYPE (MeasureFn ?TEMP1 ?MEASURE))\n" +
                "      (instance ?SUBSTANCE ?TYPE)\n" +
                "      (patient ?BOILING ?SUBSTANCE)\n" +
                "      (holdsDuring (WhenFn ?BOILING) (measure ?SUBSTANCE (MeasureFn ?TEMP2 ?MEASURE)))\n" +
                "      (instance ?MEASURE UnitOfTemperature))\n" +
                "   (greaterThanOrEqualTo ?TEMP2 ?TEMP1))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        //expected.put("?SUBSTANCE", new HashSet<String>());
        expected.put("?BOILING", Sets.newHashSet("Boiling"));
        expected.put("?MEASURE", Sets.newHashSet("UnitOfTemperature"));
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypes12() {
        String stmt = "(=>\n" +
                "  (axis ?AXIS ?OBJ)\n" +
                "  (exists (?R)\n" +
                "    (and\n" +
                "      (instance ?R Rotating)\n" +
                "      (part ?AXIS ?OBJ)\n" +
                "      (experiencer ?R ?OBJ)\n" +
                "      (not\n" +
                "        (exists (?R2)\n" +
                "          (and\n" +
                "            (instance ?R2 Rotating)\n" +
                "            (subProcess ?R2 ?R)\n" +
                "            (experiencer ?R2 ?AXIS)))))))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?R", Sets.newHashSet("Rotating"));
        expected.put("?R2", Sets.newHashSet("Rotating"));
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFindExplicitTypes13() {
        String stmt = "(=>\n" +
                "  (instance ?X BabyMonitoringSystem)\n" +
                "  (exists (?TX ?RX)\n" +
                "    (and\n" +
                "      (instance ?RX RadioReceiver)\n" +
                "      (instance ?TX Device)\n" +
                "      (engineeringSubcomponent ?RX ?X)\n" +
                "      (engineeringSubcomponent ?TX ?X)\n" +
                "      (hasPurpose ?X\n" +
                "        (exists (?BABY ?CARER ?SOUND ?LOC1 ?LOC2 ?PROC ?RADIO)\n" +
                "          (and\n" +
                "            (instance ?BABY HumanBaby)\n" +
                "            (instance ?CARER Human)\n" +
                "            (located ?BABY ?LOC1)\n" +
                "            (located ?CARER ?LOC2)\n" +
                "            (not\n" +
                "              (equal ?LOC1 ?LOC2))\n" +
                "            (instance ?PROC Maintaining)\n" +
                "            (patient ?PROC ?BABY)\n" +
                "            (agent ?PROC ?CARER)\n" +
                "            (located ?TX ?LOC1)\n" +
                "            (located ?RX ?LOC2)\n" +
                "            (instance ?SOUND RadiatingSound)\n" +
                "            (eventLocated ?SOUND ?LOC1)\n" +
                "            (instance ?RADIO RadioEmission)\n" +
                "            (patient ?RADIO ?SOUND)\n" +
                "            (destination ?RADIO ?RX)\n" +
                "            (agent ?RADIO ?TX)))))))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Map<String, HashSet<String>> actual = fp.findExplicitTypes(SigmaTestBase.kb,f);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?X", Sets.newHashSet("BabyMonitoringSystem"));
        expected.put("?RX", Sets.newHashSet("RadioReceiver"));
        expected.put("?TX", Sets.newHashSet("Device"));
        expected.put("?BABY", Sets.newHashSet("HumanBaby"));
        expected.put("?CARER", Sets.newHashSet("Human"));
        expected.put("?PROC", Sets.newHashSet("Maintaining"));
        expected.put("?SOUND", Sets.newHashSet("RadiatingSound"));
        expected.put("?RADIO", Sets.newHashSet("RadioEmission"));
        assertEquals(expected, actual);
    }
}