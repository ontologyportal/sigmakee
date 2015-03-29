package com.articulate.sigma;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by qingqingcai on 3/9/15.
 */
public class FormulaPreprocessorAddTypeRestrictionsTest extends IntegrationTestBase {

    @Test
    public void testAddTypeRestrictions1() {

        FormulaPreprocessor fp = new FormulaPreprocessor();
        KB kb = SigmaTestBase.kb;
        String stmt = "(<=>\n" +
                "   (instance ?GRAPH PseudoGraph)\n" +
                "   (exists (?LOOP)\n" +
                "      (and\n" +
                "         (instance ?LOOP GraphLoop)\n" +
                "         (graphPart ?LOOP ?GRAPH))))";
        Formula f = new Formula();
        f.read(stmt);
        Formula actualF = fp.addTypeRestrictions(f, kb);
        String actualTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(actualF.theFormula, false);

        Formula expectedF = new Formula("(<=>\n" +
                "   (instance ?GRAPH PseudoGraph)\n" +
                "   (exists (?LOOP)\n" +
                "      (and\n" +
                "         (instance ?LOOP GraphLoop)\n" +
                "         (graphPart ?LOOP ?GRAPH))))");
        String expectedTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(expectedF.theFormula, false);

        assertEquals(expectedTPTP, actualTPTP);
    }

    @Test
    public void testAddTypeRestrictions2() {

        FormulaPreprocessor fp = new FormulaPreprocessor();
        KB kb = SigmaTestBase.kb;
        String stmt = "(=>\n" +
                "  (and\n" +
                "    (graphMeasure ?G ?M)\n" +
                "    (instance ?AN GraphNode)\n" +
                "    (instance ?AA GraphArc)\n" +
                "    (abstractCounterpart ?AN ?PN)\n" +
                "    (abstractCounterpart ?AA ?PA)\n" +
                "    (arcWeight ?AA (MeasureFn ?N ?M)))\n" +
                "  (measure ?PA (MeasureFn ?N ?M)))";
        Formula f = new Formula();
        f.read(stmt);
        Formula actualF = fp.addTypeRestrictions(f, kb);
        String actualTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(actualF.theFormula, false);

        Formula expectedF = new Formula("(=> \n" +
                "  (and \n" +
                "    (instance ?PA Object)\n" +
                "    (instance ?G Graph)\n" +
                "    (instance ?PN Physical)\n" +
                "    (instance ?M UnitOfMeasure)\n" +
                "    (instance ?N RealNumber) )\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (graphMeasure ?G ?M)\n" +
                "        (instance ?AN GraphNode)\n" +
                "        (instance ?AA GraphArc)\n" +
                "        (abstractCounterpart ?AN ?PN)\n" +
                "        (abstractCounterpart ?AA ?PA)\n" +
                "        (arcWeight ?AA (MeasureFn ?N ?M)) )\n" +
                "      (measure ?PA (MeasureFn ?N ?M)) ))");
        String expectedTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(expectedF.theFormula, false);

        assertEquals(expectedTPTP, actualTPTP);
    }

    @Test
    public void testAddTypeRestrictions3() {

        FormulaPreprocessor fp = new FormulaPreprocessor();
        KB kb = SigmaTestBase.kb;
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
        Formula actualF = fp.addTypeRestrictions(f, kb);
        String actualTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(actualF.theFormula, false);

        Formula expectedF = new Formula("(=> \n" +
                "  (and \n" +
                "    (instance ?MEASURE1 PhysicalQuantity)\n" +
                "    (instance ?MEASURE2 PhysicalQuantity) )\n" +
                "  (=>\n" +
                "    (instance ?CLOUD WaterCloud)\n" +
                "    (forall (?PART)\n" +
                "      (=>\n" +
                "        (and (instance ?PART Object))\n" +
                "        (=>\n" +
                "          (and\n" +
                "            (part ?PART ?CLOUD)\n" +
                "            (not (instance ?PART Water) ))\n" +
                "          (exists (?WATER)\n" +
                "            (and\n" +
                "              (instance ?WATER Water)\n" +
                "              (part ?WATER ?CLOUD)\n" +
                "              (measure ?WATER ?MEASURE1)\n" +
                "              (measure ?PART ?MEASURE2)\n" +
                "              (greaterThan ?MEASURE1 ?MEASURE2) )))))))");

        String expectedTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(expectedF.theFormula, false);

        assertEquals(expectedTPTP, actualTPTP);
    }

    @Test
    public void testAddTypeRestrictions4() {

        FormulaPreprocessor fp = new FormulaPreprocessor();
        KB kb = SigmaTestBase.kb;
        String stmt = "(=>\n" +
                "   (and\n" +
                "      (instance ?MIXTURE Mixture)\n" +
                "      (part ?SUBSTANCE ?MIXTURE)\n" +
                "      (not (instance ?SUBSTANCE Mixture)))\n" +
                "   (instance ?SUBSTANCE PureSubstance))";
        Formula f = new Formula();
        f.read(stmt);
        Formula actualF = fp.addTypeRestrictions(f, kb);
        String actualTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(actualF.theFormula, false);

        Formula expectedF = new Formula("(=>\n" +
                "(and (instance ?SUBSTANCE Object))\n" +
                "(=>\n" +
                "  (and\n" +
                "    (instance ?MIXTURE Mixture)\n" +
                "    (part ?SUBSTANCE ?MIXTURE)\n" +
                "    (not (instance ?SUBSTANCE Mixture) ))\n" +
                "  (instance ?SUBSTANCE PureSubstance) ))");

        String expectedTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(expectedF.theFormula, false);

        assertEquals(expectedTPTP, actualTPTP);
    }

    @Test
    public void testAddTypeRestrictions5() {

        FormulaPreprocessor fp = new FormulaPreprocessor();
        KB kb = SigmaTestBase.kb;
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
        Formula actualF = fp.addTypeRestrictions(f, kb);
        String actualTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(actualF.theFormula, false);

        Formula expectedF = new Formula("(=> \n" +
                "  (and \n" +
                "    (instance ?OBJ Agent)\n" +
                "    (instance ?AXIS Agent) )\n" +
                "  (=>\n" +
                "    (axis ?AXIS ?OBJ)\n" +
                "    (exists (?R)\n" +
                "      (and\n" +
                "        (instance ?R Rotating)\n" +
                "        (part ?AXIS ?OBJ)\n" +
                "        (experiencer ?R ?OBJ)\n" +
                "        (not\n" +
                "          (exists (?R2)\n" +
                "            (and\n" +
                "              (instance ?R2 Rotating)\n" +
                "              (subProcess ?R2 ?R)\n" +
                "              (experiencer ?R2 ?AXIS) )))))))");

        String expectedTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(expectedF.theFormula, false);

        assertEquals(expectedTPTP, actualTPTP);
    }

    @Test
    public void testAddTypeRestrictions6() {

        FormulaPreprocessor fp = new FormulaPreprocessor();
        KB kb = SigmaTestBase.kb;
        String stmt = "(=>\n" +
                "    (serviceFee ?Bank ?Action ?Amount)\n" +
                "    (exists (?Fee)\n" +
                "        (and\n" +
                "            (instance ?Fee ChargingAFee)\n" +
                "            (agent ?Fee ?Bank)\n" +
                "            (causes ?Action ?Fee)\n" +
                "            (amountCharged ?Fee ?Amount))))";
        Formula f = new Formula();
        f.read(stmt);
        Formula actualF = fp.addTypeRestrictions(f, kb);
        String actualTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(actualF.theFormula, false);

        Formula expectedF = new Formula("(=> \n" +
                "  (and \n" +
                "    (instance ?Amount CurrencyMeasure)\n" +
                "    (instance ?Action FinancialTransaction)\n" +
                "    (instance ?Bank FinancialOrganization) )\n" +
                "  (=>\n" +
                "    (serviceFee ?Bank ?Action ?Amount)\n" +
                "    (exists (?Fee)\n" +
                "      (and\n" +
                "        (instance ?Fee ChargingAFee)\n" +
                "        (agent ?Fee ?Bank)\n" +
                "        (causes ?Action ?Fee)\n" +
                "        (amountCharged ?Fee ?Amount) ))))");

        String expectedTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(expectedF.theFormula, false);

        assertEquals(expectedTPTP, actualTPTP);
    }

    @Test
    public void testAddTypeRestrictions7() {

        FormulaPreprocessor fp = new FormulaPreprocessor();
        KB kb = SigmaTestBase.kb;
        String stmt = "(=>\n" +
                "    (forall (?ELEMENT)\n" +
                "        (<=>\n" +
                "            (element ?ELEMENT ?SET1)\n" +
                "            (element ?ELEMENT ?SET2)))\n" +
                "    (equal ?SET1 ?SET2))";
        Formula f = new Formula();
        f.read(stmt);
        Formula actualF = fp.addTypeRestrictions(f, kb);
        String actualTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(actualF.theFormula, false);

        Formula expectedF = new Formula("(=> \n" +
                "  (and \n" +
                "    (instance ?SET1 Set)\n" +
                "    (instance ?SET2 Set) )\n" +
                "  (=>\n" +
                "    (forall (?ELEMENT)\n" +
                "      (=> " +
                "        (and (instance ?ELEMENT Entity))" +
                "        (<=>\n" +
                "          (element ?ELEMENT ?SET1)\n" +
                "          (element ?ELEMENT ?SET2)) ))\n" +
                "    (equal ?SET1 ?SET2) ))");

        String expectedTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(expectedF.theFormula, false);

        assertEquals(expectedTPTP, actualTPTP);
    }

    @Ignore
    @Test
    public void testAddTypeRestrictions8() {

        FormulaPreprocessor fp = new FormulaPreprocessor();
        KB kb = SigmaTestBase.kb;
        String stmt = "(=>\n" +
                "    (and\n" +
                "        (typicalPart ?PART ?WHOLE)\n" +
                "        (instance ?X ?PART)\n" +
                "        (equal ?PARTPROB\n" +
                "            (ProbabilityFn\n" +
                "                (exists (?Y)\n" +
                "                    (and\n" +
                "                        (instance ?Y ?WHOLE)\n" +
                "                        (part ?X ?Y)))))\n" +
                "        (equal ?NOTPARTPROB\n" +
                "            (ProbabilityFn\n" +
                "                (not\n" +
                "                    (exists (?Z)\n" +
                "                        (and\n" +
                "                            (instance ?Z ?WHOLE)\n" +
                "                            (part ?X ?Z)))))))\n" +
                "    (greaterThan ?PARTPROB ?NOTPARTPROB))";
        Formula f = new Formula();
        f.read(stmt);
        Formula actualF = fp.addTypeRestrictions(f, kb);
        String actualTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(actualF.theFormula, false);

        Formula expectedF = new Formula("(=> \n" +
                "  (and \n" +
                "    (instance ?NOTPARTPROB Quantity)\n" +
                "    (instance ?X Object)\n" +
                "    (instance ?PARTPROB Quantity) )\n" +
                "  (=>\n" +
                "    (and\n" +
                "      (typicalPart ?PART ?WHOLE)\n" +
                "      (instance ?X ?PART)\n" +
                "      (equal ?PARTPROB\n" +
                "        (ProbabilityFn\n" +
                "          (exists (?Y)\n" +
                "            (and\n" +
                "              (instance ?Y ?WHOLE)\n" +
                "              (part ?X ?Y)))))\n" +
                "      (equal ?NOTPARTPROB\n" +
                "        (ProbabilityFn\n" +
                "          (not\n" +
                "            (exists (?Z)\n" +
                "              (and\n" +
                "                (instance ?Z ?WHOLE)\n" +
                "                (part ?X ?Z)))))) )\n" +
                "    (greaterThan ?PARTPROB ?NOTPARTPROB) ))");

        String expectedTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(expectedF.theFormula, false);

        assertEquals(expectedTPTP, actualTPTP);
    }

    /**
     * To pass testAddTypeRestrictions9, we need to add (domain located 1 Physical)
     * and (domain located 2 Object) in Merge.kif
     */
    @Ignore
    @Test
    public void testAddTypeRestrictions9() {

        FormulaPreprocessor fp = new FormulaPreprocessor();
        KB kb = SigmaTestBase.kb;
        String stmt = "(<=>\n" +
                "  (instance ?PHYS Physical)\n" +
                "  (exists (?LOC ?TIME)\n" +
                "    (and\n" +
                "      (located ?PHYS ?LOC)\n" +
                "      (time ?PHYS ?TIME))))";
        Formula f = new Formula();
        f.read(stmt);
        Formula actualF = fp.addTypeRestrictions(f, kb);
        String actualTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(actualF.theFormula, false);

        Formula expectedF = new Formula("(<=>\n" +
                "  (instance ?PHYS Physical)\n" +
                "  (exists (?LOC ?TIME)\n" +
                "    (and\n" +
                "      (instance ?TIME TimePosition)\n" +
                "      (instance ?LOC Object)\n" +
                "      (located ?PHYS ?LOC)\n" +
                "      (time ?PHYS ?TIME))))");

        String expectedTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(expectedF.theFormula, false);

        assertEquals(expectedTPTP, actualTPTP);
    }

    @Test
    public void testAddTypeRestrictions10() {

        FormulaPreprocessor fp = new FormulaPreprocessor();
        KB kb = SigmaTestBase.kb;
        String stmt = "(=>\n" +
                "  (instance ?GROUP BeliefGroup)\n" +
                "  (exists (?BELIEF)\n" +
                "    (forall (?MEMB)\n" +
                "      (=>\n" +
                "        (member ?MEMB ?GROUP)\n" +
                "        (believes ?MEMB ?BELIEF)))))";
        Formula f = new Formula();
        f.read(stmt);
        Formula actualF = fp.addTypeRestrictions(f, kb);
        String actualTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(actualF.theFormula, false);

        Formula expectedF = new Formula("(=>\n" +
                "  (instance ?GROUP BeliefGroup)\n" +
                "  (exists (?BELIEF)\n" +
                "    (and\n" +
                "      (instance ?BELIEF Formula)\n" +
                "      (forall (?MEMB)\n" +
                "        (=>\n" +
                "          (and " +
                "            (instance ?MEMB SelfConnectedObject)" +
                "            (instance ?MEMB CognitiveAgent))\n" +
                "          (=>\n" +
                "            (member ?MEMB ?GROUP)\n" +
                "            (believes ?MEMB ?BELIEF) ))))))");

        String expectedTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(expectedF.theFormula, false);

        assertEquals(expectedTPTP, actualTPTP);
    }

    @Test
    public void testAddTypeRestrictions11() {

        FormulaPreprocessor fp = new FormulaPreprocessor();
        KB kb = SigmaTestBase.kb;
        String stmt = "(<=>\n" +
                "  (instance ?OBJ SelfConnectedObject)\n" +
                "  (forall (?PART1 ?PART2)\n" +
                "  (=>\n" +
                "    (equal ?OBJ\n" +
                "      (MereologicalSumFn ?PART1 ?PART2))\n" +
                "    (connected ?PART1 ?PART2))))";
        Formula f = new Formula();
        f.read(stmt);
        Formula actualF = fp.addTypeRestrictions(f, kb);
        String actualTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(actualF.theFormula, false);

        Formula expectedF = new Formula("(<=>\n" +
                "  (instance ?OBJ SelfConnectedObject)\n" +
                "  (forall (?PART1 ?PART2)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (instance ?PART1 Object)\n" +
                "        (instance ?PART2 Object))\n" +
                "      (=>\n" +
                "        (equal ?OBJ\n" +
                "          (MereologicalSumFn ?PART1 ?PART2))\n" +
                "        (connected ?PART1 ?PART2) ))))");

        String expectedTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(expectedF.theFormula, false);

        assertEquals(expectedTPTP, actualTPTP);
    }

    @Test
    public void testAddTypeRestrictions12() {

        FormulaPreprocessor fp = new FormulaPreprocessor();
        KB kb = SigmaTestBase.kb;
        String stmt = "(=>\n" +
                "  (instance ?S Seafood)\n" +
                "  (exists (?X ?SEA)\n" +
                "    (and\n" +
                "      (meatOfAnimal ?S ?ANIMAL)\n" +
                "      (instance ?X ?ANIMAL)\n" +
                "      (instance ?SEA BodyOfWater)\n" +
                "      (inhabits ?X ?SEA))))";
        Formula f = new Formula();
        f.read(stmt);
        Formula actualF = fp.addTypeRestrictions(f, kb);
        String actualTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(actualF.theFormula, false);

        Formula expectedF = new Formula("(=>\n" +
                "  (and\n" +
                "    (subclass ?S Meat)\n" +
                "    (subclass ?ANIMAL Animal)\n" +
                "    (instance ?ANIMAL SetOrClass))\n" +
                "  (=>\n" +
                "    (instance ?S Seafood)\n" +
                "    (exists (?X ?SEA)\n" +
                "      (and\n" +
                "        (meatOfAnimal ?S ?ANIMAL)\n" +
                "        (instance ?X ?ANIMAL)\n" +
                "        (instance ?SEA BodyOfWater)\n" +
                "        (inhabits ?X ?SEA))))))");

        String expectedTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(expectedF.theFormula, false);

        assertEquals(expectedTPTP, actualTPTP);
    }
}
