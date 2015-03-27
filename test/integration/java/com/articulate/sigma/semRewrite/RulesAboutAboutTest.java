package com.articulate.sigma.semRewrite;

import com.articulate.sigma.*;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author sathenikos
 */
@Ignore
public class RulesAboutAboutTest extends IntegrationTestBase {

    public static Interpreter interpreter;

    @BeforeClass
    public static void initInterpreter() {
        interpreter = new Interpreter();
        KBmanager.getMgr().initializeOnce();
        interpreter.initialize();
    }

    @Test
    public void testAdviceAbout() {
        String input = "He gave me an advice about exercise.";

        String expectedKifString = "(exists (?gave-2 ?advice-5 ?me-3 ?He-1 ?exercise-7)\n" +
                "(and\n" +
                "  (agent ?gave-2 ?He-1)\n" +
                "  (instance ?advice-5 Requesting)\n" +
                "  (patient ?gave-2 ?advice-5)\n" +
                "  (refers ?advice-5 ?exercise-7)\n" +
                "  (instance ?gave-2 causes)\n" +
                "  (patient ?X ?me-3)\n" +
                "  (instance ?exercise-7 RecreationOrExercise)\n" +
                "  (instance ?He-1 Character))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testAgreementAbout() {
        String input = "We have an agreement about the policy.";

        String expectedKifString = "(exists (?We-1 ?have-2 ?agreement-4 ?policy-7)\n" +
                "(and\n" +
                "  (agent ?have-2 ?We-1)\n" +
                "  (instance ?agreement-4 Agreement)\n" +
                "  (patient ?have-2 ?agreement-4)\n" +
                "  (refers ?agreement-4 ?policy-7)\n" +
                "  (instance ?policy-7 Policy))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testAlarmAbout() {
        String input = "There was an alarm about the danger.";

        String expectedKifString = "(exists (?was-2 ?danger-7 ?alarm-4 ?There-1)\n" +
                "(and\n" +
                "  (agent ?was-2 ?alarm-4)\n" +
                "  (instance ?There-1 SubjectiveAssessmentAttribute)\n" +
                "  (refers ?alarm-4 ?danger-7)\n" +
                "  (instance ?alarm-4 EmotionalState)\n" +
                "  (instance ?danger-7 SubjectiveAssessmentAttribute))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testAnxietyAbout() {
        String input = "I have an anxiety about height.";

        String expectedKifString = "(exists (?have-2 ?I-1 ?anxiety-4 ?height-6)\n" +
                "(and\n" +
                "  (agent ?have-2 ?I-1)\n" +
                "  (instance ?anxiety-4 EmotionalState)\n" +
                "  (patient ?have-2 ?anxiety-4)\n" +
                "  (refers ?anxiety-4 ?height-6)\n" +
                "  (instance ?I-1 AlphabeticCharacter)\n" +
                "  (instance ?height-6 LengthMeasure))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testArgumentAbout() {
        String input = "We had arguments about the health care cost.";

        String expectedKifString = "(exists (?We-1 ?health-6 ?arguments-3 ?had-2 ?cost-8 ?care-7)\n" +
                "(and\n" +
                "  (agent ?had-2 ?We-1)\n" +
                "  (instance ?health-6 BiologicalAttribute)\n" +
                "  (patient ?had-2 ?arguments-3)\n" +
                "  (refers ?arguments-3 ?cost-8)\n" +
                "  (instance ?arguments-3 ContentBearingObject)\n" +
                "  (instance ?care-7 Maintaining))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testArticleAbout() {
        String input = "He gave me an article about the current economy.";

        String expectedKifString = "(exists (?current-8 ?article-5 ?gave-2 ?economy-9 ?me-3 ?He-1)\n" +
                "(and\n" +
                "  (agent ?gave-2 ?He-1)\n" +
                "  (attribute ?economy-9 ?current-8)\n" +
                "  (instance ?current-8 TimePosition)\n" +
                "  (patient ?gave-2 ?article-5)\n" +
                "  (refers ?article-5 ?economy-9)\n" +
                "  (instance ?gave-2 causes)\n" +
                "  (patient ?X ?me-3)\n" +
                "  (instance ?economy-9 EconomicAttribute)\n" +
                "  (instance ?He-1 Character)\n" +
                "  (instance ?article-5 Report))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testAssumptionAbout() {
        String input = "The nurses have assumptions about patient care.";

        String expectedKifString = "(exists (?have-3 ?nurses-2 ?patient-6 ?assumptions-4 ?care-7)\n" +
                "(and\n" +
                "  (agent ?have-3 ?nurses-2)\n" +
                "  (instance ?patient-6 Human)\n" +
                "  (patient ?have-3 ?assumptions-4)\n" +
                "  (refers ?assumptions-4 ?care-7)\n" +
                "  (instance ?nurses-2 TherapeuticProcess)\n" +
                "  (instance ?assumptions-4 Supposition))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testBeliefAbout() {
        String input = "Different cultures have different beliefs about social role expectations.";

        String expectedKifString = "(exists (?social-7 ?beliefs-5 ?have-3 ?different-4 ?expectations-9 ?cultures-2 ?Different-1 ?role-8)\n" +
                "(and\n" +
                "  (agent ?have-3 ?cultures-2)\n" +
                "  (attribute ?cultures-2 ?Different-1)\n" +
                "  (instance ?role-8 Position)\n" +
                "  (patient ?have-3 ?beliefs-5)\n" +
                "  (refers ?beliefs-5 ?expectations-9)\n" +
                "  (attribute ?beliefs-5 ?different-4)\n" +
                "  (instance ?social-7 SocialInteraction)\n" +
                "  (attribute ?expectations-9 ?social-7)\n" +
                "  (instance ?cultures-2 SubjectiveAssessmentAttribute)\n" +
                "  (instance ?expectations-9 Proposition)\n" +
                "  (instance ?beliefs-5 Proposition)\n" +
                "  (instance ?Different-1 equal))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testBookAbout() {
        String input = "Peggy gave me two books about the North.";

        String expectedKifString = "(exists (?gave-2 ?books-5 ?North-8 ?me-3 ?Peggy-1)\n" +
                "(and\n" +
                "  (agent ?gave-2 ?Peggy-1)\n" +
                "  (attribute ?Peggy-1 Female)\n" +
                "  (instance ?books-5 Collection)\n" +
                "  (membersType ?books-5 Book)\n" +
                "  (membersCount ?books-5 2)\n" +
                "  (patient ?gave-2 ?books-5)\n" +
                "  (refers ?books-5 ?North-8)\n" +
                "  (instance ?Peggy-1 Human)\n" +
                "  (patient ?X ?me-3)\n" +
                "  (instance ?books-5 Book)\n" +
                "  (instance ?gave-2 causes)\n" +
                "  (instance ?North-8 LandArea))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testChatAbout() {
        String input = "The chat about the company evolved into a discussion.";

        String expectedKifString = "(exists (?chat-2 ?discussion-9 ?company-5 ?evolved-6)\n" +
                "(and\n" +
                "  (agent ?evolved-6 ?chat-2)\n" +
                "  (instance ?chat-2 Communication)\n" +
                "  (properlyFills ?evolved-6 ?discussion-9)\n" +
                "  (refers ?chat-2 ?company-5)\n" +
                "  (instance ?evolved-6 ContentDevelopment)\n" +
                "  (instance ?company-5 Corporation)\n" +
                "  (instance ?discussion-9 Text))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testChoiceAbout() {
        String input = "Patients have choices about future treatments.";

        String expectedKifString = "(exists (?have-2 ?treatments-6 ?future-5 ?choices-3 ?Patients-1)\n" +
                "(and\n" +
                "  (agent ?have-2 ?Patients-1)\n" +
                "  (attribute ?treatments-6 ?future-5)\n" +
                "  (instance ?choices-3 Selecting)\n" +
                "  (patient ?have-2 ?choices-3)\n" +
                "  (refers ?choices-3 ?treatments-6)\n" +
                "  (instance ?Patients-1 Human)\n" +
                "  (instance ?treatments-6 TherapeuticProcess))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testCommunicationAbout() {
        String input = "He wants effective communication about influenza vaccine.";

        String expectedKifString = "(exists (?wants-2 ?vaccine-7 ?effective-3 ?influenza-6 ?communication-4 ?He-1)\n" +
                "(and\n" +
                "  (agent ?wants-2 ?He-1)\n" +
                "  (attribute ?communication-4 ?effective-3)\n" +
                "  (instance ?influenza-6 Influenza)\n" +
                "  (patient ?wants-2 ?communication-4)\n" +
                "  (refers ?communication-4 ?vaccine-7)\n" +
                "  (instance ?effective-3 SubjectiveWeakPositiveAttribute)\n" +
                "  (instance ?vaccine-7 BiologicallyActiveSubstance)\n" +
                "  (instance ?wants-2 wants)\n" +
                "  (instance ?communication-4 Communication)\n" +
                "  (instance ?He-1 Character))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testConclusionAbout() {
        String input = "This study makes conclusions about people's behavior.";

        String expectedKifString = "(exists (?people-6 ?makes-3 ?study-2 ?conclusions-4 ?behavior-8)\n" +
                "(and\n" +
                "  (agent ?makes-3 ?study-2)\n" +
                "  (instance ?study-2 Investigating)\n" +
                "  (patient ?makes-3 ?conclusions-4)\n" +
                "  (refers ?conclusions-4 ?behavior-8)\n" +
                "  (instance ?conclusions-4 Learning)\n" +
                "  (instance ?behavior-8 BodyMotion)\n" +
                "  (instance ?people-6 GroupOfPeople))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testConsensusAbout() {
        String input = "The committee has consensus about the funding.";

        String expectedKifString = "(exists (?committee-2 ?funding-7 ?has-3 ?consensus-4)\n" +
                "(and\n" +
                "  (agent ?has-3 ?committee-2)\n" +
                "  (instance ?consensus-4 Cooperation)\n" +
                "  (patient ?has-3 ?consensus-4)\n" +
                "  (refers ?consensus-4 ?funding-7)\n" +
                "  (instance ?funding-7 FinancialTransaction)\n" +
                "  (instance ?committee-2 Commission))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testConspiracyAbout() {
        String input = "He was charged with the conspiracy about the successor to the throne.";

        String expectedKifString = "(exists (?conspiracy-6 ?charged-3 ?successor-9 ?He-1 ?throne-12)\n" +
                "(and\n" +
                "  (destination ?charged-3 ?throne-12)\n" +
                "  (instance ?conspiracy-6 Promise)\n" +
                "  (patient ?charged-3 ?He-1)\n" +
                "  (refers ?conspiracy-6 ?successor-9)\n" +
                "  (instance ?successor-9 SocialRole)\n" +
                "  (instance ?throne-12 Chair)\n" +
                "  (instance ?charged-3 Attack)\n" +
                "  (instance ?He-1 Character))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testConversationAbout() {
        String input = "In the middle of a conversation about schoolwork, she suddenly left.";

        String expectedKifString = "(exists (?middle-3 ?she-10 ?schoolwork-8 ?conversation-6 ?left-12 ?suddenly-11)\n" +
                "(and\n" +
                "  (orientation ?left-12 ?middle-3 Inside)\n" +
                "  (agent ?left-12 ?she-10)\n" +
                "  (attribute ?left-12 ?suddenly-11)\n" +
                "  (instance ?middle-3 Region)\n" +
                "  (refers ?conversation-6 ?schoolwork-8)\n" +
                "  (instance ?schoolwork-8 EducationalProcess)\n" +
                "  (instance ?left-12 Leaving)\n" +
                "  (instance ?suddenly-11 FinancialTransaction)\n" +
                "  (instance ?conversation-6 Speaking))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testCounselingAbout() {
        String input = "Is office-based counseling about timeouts effective?";

        String expectedKifString = "(exists (?counseling-3 ?effective-6 ?timeouts-5)\n" +
                "(and\n" +
                "  (attribute ?counseling-3 office-?based-2)\n" +
                "  (instance ?effective-6 SubjectiveWeakPositiveAttribute)\n" +
                "  (refers ?counseling-3 ?timeouts-5)\n" +
                "  (instance ?counseling-3 Directing)\n" +
                "  (instance office-?based-2 OfficeBuilding))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testDebateAbout() {
        String input = "There is an ongoing debate about the amount of weight that women should gain during pregnancy.";

        String expectedKifString = "(exists (?debate-5 ?ongoing-4 ?that-11 ?women-12 ?is-2 ?pregnancy-16 ?There-1 ?amount-8 ?weight-10 ?gain-14)\n" +
                "(and\n" +
                "  (agent ?is-2 ?debate-5)\n" +
                "  (attribute ?debate-5 ?ongoing-4)\n" +
                "  (instance ?pregnancy-16 BiologicalAttribute)\n" +
                "  (patient ?gain-14 ?that-11)\n" +
                "  (refers ?debate-5 ?amount-8)\n" +
                "  (agent ?gain-14 ?women-12)\n" +
                "  (instance ?weight-10 MassMeasure)\n" +
                "  (instance ?gain-14 experiencer)\n" +
                "  (instance ?debate-5 Debating)\n" +
                "  (instance ?ongoing-4 TimePosition)\n" +
                "  (instance ?There-1 SubjectiveAssessmentAttribute))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testDecisionAbout() {
        String input = "We have made a decision about the matter.";

        String expectedKifString = "(exists (?decision-5 ?We-1 ?matter-8)\n" +
                "(and\n" +
                "  (agent ?decision-5 ?We-1)\n" +
                "  (instance ?matter-8 Proposition)\n" +
                "  (refers ?decision-5 ?matter-8)\n" +
                "  (earlier\n" +
                "  (WhenFn ?decision-5) Now)\n";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testDetailAbout() {
        String input = "He did not give me any details about specific hardware.";

        String expectedKifString = "(exists (?details-7 ?me-5 ?hardware-10 ?He-1 ?specific-9 ?give-4)\n" +
                "(and\n" +
                "  (agent ?give-4 ?He-1)\n" +
                "  (attribute ?hardware-10 ?specific-9)\n" +
                "  (instance ?details-7 Fact)\n" +
                "  (patient ?give-4 ?details-7)\n" +
                "  (refers ?details-7 ?hardware-10)\n" +
                "  (instance ?specific-9 SubjectiveWeakNegativeAttribute)\n" +
                "  (patient ?X ?me-5)\n" +
                "  (instance ?He-1 Character)\n" +
                "  (instance ?hardware-10 Weapon))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testDisagreementAbout() {
        String input = "The meeting was deadlocked because of disagreement about the importance of the problem.";

        String expectedKifString = "(exists (?deadlocked-4 ?importance-10 ?problem-13 ?disagreement-7 ?meeting-2)\n" +
                "(and\n" +
                "  (instance ?problem-13 SubjectiveAssessmentAttribute)\n" +
                "  (patient ?deadlocked-4 ?meeting-2)\n" +
                "  (refers ?disagreement-7 ?importance-10)\n" +
                "  (instance ?disagreement-7 SubjectiveAssessmentAttribute)\n" +
                "  (instance ?importance-10 SubjectiveAssessmentAttribute)\n" +
                "  (instance ?deadlocked-4 ContestAttribute)\n" +
                "  (instance ?meeting-2 Meeting))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testDiscoveryAbout() {
        String input = "His discovery about the peptide earned him a Nobel prize.";

        String expectedKifString = "(exists (?discovery-2 ?peptide-5 ?prize-10 ?Nobel-9 ?earned-6)\n" +
                "(and\n" +
                "  (agent ?earned-6 ?discovery-2)\n" +
                "  (instance ?earned-6 Getting)\n" +
                "  (patient ?earned-6 ?prize-10)\n" +
                "  (refers ?discovery-2 ?peptide-5)\n" +
                "  (instance ?peptide-5 CompoundSubstance)\n" +
                "  (instance ?discovery-2 Learning)\n" +
                "  (instance ?Nobel-9 Man)\n" +
                "  (instance ?prize-10 UnilateralGiving))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testDiscussionAbout() {
        String input = "The discussion about the best option led to no conclusion.";

        String expectedKifString = "(exists (?best-5 ?led-7 ?discussion-2 ?option-6 ?conclusion-10)\n" +
                "(and\n" +
                "  (EndFn ?led-7 ?conclusion-10)\n" +
                "  (agent ?led-7 ?discussion-2)\n" +
                "  (attribute ?option-6 ?best-5)\n" +
                "  (instance ?conclusion-10 Learning)\n" +
                "  (refers ?discussion-2 ?option-6)\n" +
                "  (instance ?led-7 Guiding)\n" +
                "  (instance ?best-5 SubjectiveAssessmentAttribute)\n" +
                "  (instance ?discussion-2 Text)\n" +
                "  (instance ?option-6 Selecting))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testDocumentaryAbout() {
        String input = "The winner of the DVD documentary about the Headless Chicken welcomed the award.";

        String expectedKifString = "(exists (?Headless-9 ?documentary-6 ?winner-2 ?welcomed-11 ?award-13 ?Chicken-10 ?DVD-5)\n" +
                "(and\n" +
                "  (agent ?welcomed-11 ?winner-2)\n" +
                "  (attribute ?Chicken-10 ?Headless-9)\n" +
                "  (instance ?welcomed-11 Getting)\n" +
                "  (patient ?welcomed-11 ?award-13)\n" +
                "  (refers ?documentary-6 ?Chicken-10)\n" +
                "  (instance ?winner-2 SocialRole)\n" +
                "  (instance ?DVD-5 DVD)\n" +
                "  (instance ?award-13 UnilateralGiving)\n" +
                "  (instance ?Chicken-10 ChickenMeat)\n" +
                "  (instance ?Headless-9 BodyPart)\n" +
                "  (instance ?documentary-6 Documentary))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testDoubtAbout() {
        String input = "Doubts about the existence of the condition grew as time went by.";

        String expectedKifString = "(exists (?grew-8 ?went-11 ?Doubts-1 ?time-10 ?existence-4 ?condition-7)\n" +
                "(and\n" +
                "  (agent ?grew-8 ?Doubts-1)\n" +
                "  (instance ?condition-7 manner)\n" +
                "  (refers ?Doubts-1 ?existence-4)\n" +
                "  (agent ?went-11 ?time-10)\n" +
                "  (instance ?went-11 Transportation)\n" +
                "  (instance ?existence-4 exists)\n" +
                "  (instance ?Doubts-1 Psych ologicalAttribute)\n" +
                "  (instance ?time-10 IntentionalProcess)\n" +
                "  (instance ?grew-8 Process))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testEducationAbout() {
        String input = "Education about healthcare is long overdue.";

        String expectedKifString = "(exists (?long-5 ?healthcare-3 ?overdue-6 ?Education-1)\n" +
                "(and\n" +
                "  (attribute ?overdue-6 ?long-5)\n" +
                "  (instance ?overdue-6 NormativeAttribute)\n" +
                "  (refers ?Education-1 ?healthcare-3)\n" +
                "  (instance ?Education-1 EducationalProcess)\n" +
                "  (instance ?long-5 SubjectiveAssessmentAttribute)\n" +
                "  (instance ?healthcare-3 Maintaining))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testEncouragementAbout() {
        String input = "The report gave me encouragement about future economic prospects.";

        String expectedKifString = "(exists (?me-4 ?gave-3 ?report-2 ?encouragement-5 ?future-7 ?economic-8 ?prospects-9)\n" +
                "(and\n" +
                "  (agent ?gave-3 ?report-2)\n" +
                "  (attribute ?prospects-9 ?future-7)\n" +
                "  (instance ?encouragement-5 ExpressingApproval)\n" +
                "  (patient ?gave-3 ?encouragement-5)\n" +
                "  (refers ?encouragement-5 ?prospects-9)\n" +
                "  (attribute ?prospects-9 ?economic-8)\n" +
                "  (instance ?prospects-9 SubjectiveAssessmentAttribute)\n" +
                "  (patient ?X ?me-4)\n" +
                "  (instance ?future-7 FutureFn)\n" +
                "  (instance ?report-2 Stating)\n" +
                "  (instance ?gave-3 causes)\n" +
                "  (instance ?economic-8 FinancialTransaction))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testEvidenceAbout() {
        String input = "There is ample evidence about the cause of the accident.";

        String expectedKifString = "(exists (?evidence-4 ?is-2 ?ample-3 ?accident-10 ?There-1 ?cause-7)\n" +
                "(and\n" +
                "  (agent ?is-2 ?evidence-4)\n" +
                "  (attribute ?evidence-4 ?ample-3)\n" +
                "  (instance ?There-1 SubjectiveAssessmentAttribute)\n" +
                "  (refers ?evidence-4 ?cause-7)\n" +
                "  (instance ?evidence-4 Reasoning)\n" +
                "  (instance ?ample-3 SubjectiveWeakPositiveAttribute)\n" +
                "  (instance ?cause-7 causes)\n" +
                "  (instance ?accident-10 Process))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testFactAbout() {
        String input = "This article outlines the facts about malnutrition.";

        String expectedKifString = "(exists (?article-2 ?facts-5 ?malnutrition-7 ?outlines-3)\n" +
                "(and\n" +
                "  (agent ?outlines-3 ?article-2)\n" +
                "  (instance ?outlines-3 Communication)\n" +
                "  (patient ?outlines-3 ?facts-5)\n" +
                "  (refers ?facts-5 ?malnutrition-7)\n" +
                "  (instance ?malnutrition-7 DiseaseOrSyndrome)\n" +
                "  (instance ?facts-5 Fact)\n" +
                "  (instance ?article-2 Report))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

//    @Test
//    public void testAssumptionAbout() {
//        String input = "";
//
//        String expectedKifString = "";
//
//        String actualKifString = interpreter.interpretSingle(input);

//    Formula expectedKifFormula = new Formula(expectedKifString);
//    Formula actualKifFormula = new Formula(actualKifString);
//
//    assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
//    }


    private String processString(String str) {
        return str.replaceAll(" ", "").replaceAll("\\s", "").trim();
    }


}
