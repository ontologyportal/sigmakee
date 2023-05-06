package com.articulate.sigma;

import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.articulate.sigma.utils.StringUtil;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by qingqingcai on 3/9/15.
 *
 * requires
 *     <constituent filename="Merge.kif" />
 *     <constituent filename="Mid-level-ontology.kif" />
 *     <constituent filename="FinancialOntology.kif" />
 */
public class FormulaPreprocessorAddTypeRestrictionsTest extends IntegrationTestBase {

    /** ***************************************************************
     */
    public void test(String label, String stmt, String expected) {

        System.out.println("=============================");
        System.out.println("FormulaPreprocessorAddTypeRestrictionsTest: " + label);
        System.out.println();
        FormulaPreprocessor fp = new FormulaPreprocessor();
        KB kb = SigmaTestBase.kb;
        Formula f = new Formula(stmt);
        Formula actualF = fp.addTypeRestrictions(f, kb);
        String actualTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(actualF.getFormula(), false);

        Formula expectedF = new Formula(expected);
        String expectedTPTP = SUMOformulaToTPTPformula.tptpParseSUOKIFString(expectedF.getFormula(), false);

        System.out.println("actual: " + actualTPTP);
        System.out.println("expected: " + expectedTPTP);
        if (!StringUtil.emptyString(actualTPTP) && actualTPTP.equals(expectedTPTP))
            System.out.println(label + " : Success");
        else
            System.out.println(label + " : fail!");
        assertEquals(expectedTPTP, actualTPTP);
    }

    /** ***************************************************************
     */
    @Test
    public void testAddTypeRestrictions1() {

        String stmt = "(<=>\n" +
                "   (instance ?GRAPH PseudoGraph)\n" +
                "   (exists (?LOOP)\n" +
                "      (and\n" +
                "         (instance ?LOOP GraphLoop)\n" +
                "         (graphPart ?LOOP ?GRAPH))))";

        String expected = "(<=>\n" +
                "   (instance ?GRAPH PseudoGraph)\n" +
                "   (exists (?LOOP)\n" +
                "      (and\n" +
                "         (instance ?LOOP GraphLoop)\n" +
                "         (graphPart ?LOOP ?GRAPH))))";
        test("testAddTypeRestrictions1",stmt,expected);
    }

    /** ***************************************************************
     */
    @Test
    public void testAddTypeRestrictions2() {

        String stmt = "(=>\n" +
                "  (and\n" +
                "    (graphMeasure ?G ?M)\n" +
                "    (instance ?AN GraphNode)\n" +
                "    (instance ?AA GraphArc)\n" +
                "    (abstractCounterpart ?AN ?PN)\n" +
                "    (abstractCounterpart ?AA ?PA)\n" +
                "    (arcWeight ?AA (MeasureFn ?N ?M)))\n" +
                "  (measure ?PA (MeasureFn ?N ?M)))";

        String expected = "(=> \n" +
                "  (and \n" +
                "    (instance ?PA Physical)\n" +
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
                "      (measure ?PA (MeasureFn ?N ?M)) ))";

        test("testAddTypeRestrictions2",stmt,expected);
    }

    /** ***************************************************************
     */
    @Test
    public void testAddTypeRestrictions3() {

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
                "               (measure ?WATER (MeasureFn ?MEASURE1 ?UNIT))\n" +
                "               (measure ?PART (MeasureFn ?MEASURE2 ?UNIT))\n" +
                "               (greaterThan ?MEASURE1 ?MEASURE2))))))";

        String expected = "(=> \n" +
                "  (and \n" +
                "    (instance ?MEASURE1 RealNumber)\n" +
                "    (instance ?MEASURE2 RealNumber)\n" +
                "    (instance ?UNIT UnitOfMeasure) )\n" +
                "  (=>\n" +
                "    (instance ?CLOUD WaterCloud)\n" +
                "    (forall (?PART)\n" +
                "      (=>\n" +
                "        (instance ?PART Object)\n" +
                "        (=>\n" +
                "          (and\n" +
                "            (part ?PART ?CLOUD)\n" +
                "            (not (instance ?PART Water) ))\n" +
                "          (exists (?WATER)\n" +
                "            (and\n" +
                "              (instance ?WATER Water)\n" +
                "              (part ?WATER ?CLOUD)\n" +
                "              (measure ?WATER (MeasureFn ?MEASURE1 ?UNIT))\n" +
                "              (measure ?PART (MeasureFn ?MEASURE2 ?UNIT))\n" +
                "              (greaterThan ?MEASURE1 ?MEASURE2) )))))))";

        test("testAddTypeRestrictions3()",stmt,expected);
    }

    /** ***************************************************************
     */
    @Test
    public void testAddTypeRestrictions4() {

        String stmt = "(=>\n" +
                "   (and\n" +
                "      (instance ?MIXTURE Mixture)\n" +
                "      (part ?SUBSTANCE ?MIXTURE)\n" +
                "      (not (instance ?SUBSTANCE Mixture)))\n" +
                "   (instance ?SUBSTANCE PureSubstance))";

        String expected = "(=>\n" +
                "(instance ?SUBSTANCE Object)\n" +
                "(=>\n" +
                "  (and\n" +
                "    (instance ?MIXTURE Mixture)\n" +
                "    (part ?SUBSTANCE ?MIXTURE)\n" +
                "    (not (instance ?SUBSTANCE Mixture) ))\n" +
                "  (instance ?SUBSTANCE PureSubstance) ))";

        test("testAddTypeRestrictions4()",stmt,expected);
    }

    /** ***************************************************************
     */
    @Test
    public void testAddTypeRestrictions5() {

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

        String expected = "(=> \n" +
                "  (and \n" +
                "    (instance ?OBJ AutonomousAgent)\n" +
                "    (instance ?AXIS AutonomousAgent) )\n" +
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
                "              (experiencer ?R2 ?AXIS) )))))))";

        test("testAddTypeRestrictions5",stmt,expected);
    }

    /** ***************************************************************
     */
    @Ignore  // serviceFee is in Financial ontology not merge or MILO
    @Test
    public void testAddTypeRestrictions6() {

        String stmt = "(=>\n" +
                "    (serviceFee ?Bank ?Action ?Amount)\n" +
                "    (exists (?Fee)\n" +
                "        (and\n" +
                "            (instance ?Fee ChargingAFee)\n" +
                "            (agent ?Fee ?Bank)\n" +
                "            (causes ?Action ?Fee)\n" +
                "            (amountCharged ?Fee ?Amount))))";

        String expected = "(=> \n" +
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
                "        (amountCharged ?Fee ?Amount) ))))";

        test("testAddTypeRestrictions6",stmt,expected);
    }

    /** ***************************************************************
     */
    @Test
    public void testAddTypeRestrictions7() {

        String stmt = "(=>\n" +
                "    (forall (?ELEMENT)\n" +
                "        (<=>\n" +
                "            (element ?ELEMENT ?SET1)\n" +
                "            (element ?ELEMENT ?SET2)))\n" +
                "    (equal ?SET1 ?SET2))";

        String expected = "(=> \n" +
                "  (and \n" +
                "    (instance ?SET1 Set)\n" +
                "    (instance ?SET2 Set) )\n" +
                "  (=>\n" +
                "    (forall (?ELEMENT)\n" +
                "        (<=>\n" +
                "          (element ?ELEMENT ?SET1)\n" +
                "          (element ?ELEMENT ?SET2)) )\n" +
                "    (equal ?SET1 ?SET2) ))";

        test("testAddTypeRestrictions7",stmt,expected);
    }

    /** ***************************************************************
     */
    @Ignore
    @Test
    public void testAddTypeRestrictions8() {

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

        String expected = "(=> \n" +
                "  (and \n" +
                "    (subclass ?WHOLE Object)\n" +
                "    (instance ?NOTPARTPROB RealNumber)\n" +
                "    (instance ?PARTPROB RealNumber)\n" +
                "    (subclass ?PART Object)\n" +
                "    (instance ?PART Class))\n" +
                "  (=>\n" +
                "    (and\n" +
                "      (typicalPart ?PART ?WHOLE)\n" +
                "      (instance ?X ?PART)\n" +
                "      (equal ?PARTPROB\n" +
                "        (ProbabilityFn\n" +
                "          (exists (?Y)\n" +
                "            (and\n" +
                "              (instance ?Y Object)\n" +
                "              (instance ?Y ?WHOLE)\n" +
                "              (part ?X ?Y)))))\n" +
                "      (equal ?NOTPARTPROB\n" +
                "        (ProbabilityFn\n" +
                "          (not\n" +
                "            (exists (?Z)\n" +
                "              (and\n" +
                "                (instance ?Z Object)\n" +
                "                (instance ?Z ?WHOLE)\n" +
                "                (part ?X ?Z)))))))\n" +
                "    (greaterThan ?PARTPROB ?NOTPARTPROB)))";

        test("testAddTypeRestrictions8",stmt,expected);
    }

    /** ***************************************************************
     */
    @Ignore
    @Test
    public void testAddTypeRestrictions9() {

        String stmt = "(<=>\n" +
                "  (instance ?PHYS Physical)\n" +
                "  (exists (?LOC ?TIME)\n" +
                "    (and\n" +
                "      (located ?PHYS ?LOC)\n" +
                "      (time ?PHYS ?TIME))))";

        String expected = "(<=>\n" +
                "  (instance ?PHYS Physical)\n" +
                "  (exists (?LOC ?TIME)\n" +
                "    (and\n" +
                "      (instance ?LOC Object)\n" +
                "      (instance ?TIME TimePosition)\n" +
                "      (located ?PHYS ?LOC)\n" +
                "      (time ?PHYS ?TIME))))";

        test("testAddTypeRestrictions9",stmt,expected);
    }

    /** ***************************************************************
     */
    @Test
    public void testAddTypeRestrictions10() {

        String stmt = "(=>\n" +
                "  (instance ?GROUP BeliefGroup)\n" +
                "  (exists (?BELIEF)\n" +
                "    (forall (?MEMB)\n" +
                "      (=>\n" +
                "        (member ?MEMB ?GROUP)\n" +
                "        (believes ?MEMB ?BELIEF)))))";

        String expected = "(=>\n" +
                "  (instance ?GROUP BeliefGroup)\n" +
                "  (exists (?BELIEF)\n" +
                "    (and\n" +
                "      (instance ?BELIEF Formula)\n" +
                "      (forall (?MEMB)\n" +
                "        (=>\n" +
                "          (instance ?MEMB CognitiveAgent)\n" +
                "          (=>\n" +
                "            (member ?MEMB ?GROUP)\n" +
                "            (believes ?MEMB ?BELIEF) ))))))";

        test("testAddTypeRestrictions10",stmt,expected);
    }

    /** ***************************************************************
     */
    @Test
    public void testAddTypeRestrictions11() {

        String stmt = "(<=>\n" +
                "  (instance ?OBJ SelfConnectedObject)\n" +
                "  (forall (?PART1 ?PART2)\n" +
                "  (=>\n" +
                "    (equal ?OBJ\n" +
                "      (MereologicalSumFn ?PART1 ?PART2))\n" +
                "    (connected ?PART1 ?PART2))))";

        String expected = "(<=>\n" +
                "  (instance ?OBJ SelfConnectedObject)\n" +
                "  (forall (?PART1 ?PART2)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (instance ?PART1 Object)\n" +
                "        (instance ?PART2 Object))\n" +
                "      (=>\n" +
                "        (equal ?OBJ\n" +
                "          (MereologicalSumFn ?PART1 ?PART2))\n" +
                "        (connected ?PART1 ?PART2) ))))";

        test("testAddTypeRestrictions11",stmt,expected);
    }

    /** ***************************************************************
     */
    @Ignore
    @Test
    public void testAddTypeRestrictions12() {

        SUMOformulaToTPTPformula.debug = true;
        String stmt = "(=>\n" +
                "  (and\n" +
                "    (instance ?S ?C)\n" +
                "    (subclass ?C Seafood))\n" +
                "  (exists (?X ?SEA)\n" +
                "    (and\n" +
                "      (meatOfAnimal ?C ?ANIMAL)\n" +
                "      (instance ?X ?ANIMAL)\n" +
                "      (instance ?SEA BodyOfWater)\n" +
                "      (inhabits ?X ?SEA))))";

        String expected = "(=>\n" +
                "  (and\n" +
                "    (instance ?S Meat)\n" +
                "    (subclass ?ANIMAL Animal)\n" +
                "    (subclass ?C Meat)\n" +
                "    (instance ?ANIMAL Class))\n" +
                "  (=>\n" +
                "  (and\n" +
                "    (instance ?S ?C)\n" +
                "    (subclass ?C Seafood))\n" +
                "  (exists (?X ?SEA)\n" +
                "    (and\n" +
                "      (meatOfAnimal ?C ?ANIMAL)\n" +
                "      (instance ?X ?ANIMAL)\n" +
                "      (instance ?SEA BodyOfWater)\n" +
                "      (inhabits ?X ?SEA)))))";

        test("testAddTypeRestrictions12",stmt,expected);
    }
}
