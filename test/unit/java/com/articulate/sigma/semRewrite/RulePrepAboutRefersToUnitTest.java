
package com.articulate.sigma.semRewrite;

import com.articulate.sigma.KBmanager;
import com.articulate.sigma.UnitTestBase;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

public class RulePrepAboutRefersToUnitTest extends UnitTestBase {

    public static Interpreter interpreter;

//    @BeforeClass
//    public static void initializeKbManager() {
//        KBmanager.getMgr().initializeOnce();
//    }

    @BeforeClass
    public static void initializeInterpreter() throws IOException {
        interpreter = new Interpreter();
        interpreter.initialize();
    }

    @Before
    public void deleteUserAssertions() {
        String base = System.getenv("SIGMA_HOME");
        String filePath = base + File.separator + "KBs" + File.separator + "SUMO_UserAssertions.kif";
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        filePath = base + File.separator + "KBs" + File.separator + "SUMO_UserAssertions.tptp";
        file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void testBoastAbout() {
        // Toyota officials boasted about the effectiveness.
        String input = "root(ROOT-0, boast-3), det(effectiveness-6, the-5), prep_about(boast-3, effectiveness-6), nsubj(boast-3, Toyotaofficials-1), sumo(SubjectiveAssessmentAttribute, effectiveness-6), sumo(Communication, boast-3), number(SINGULAR, Toyota-1), number(PLURAL, official-2), tense(PAST, boast-3), number(SINGULAR, effectiveness-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance boast-3 Communication) (refers boast-3 effectiveness-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testChatAbout() {
        // He chatted about small things.
        String input = "root(ROOT-0, chat-2), nsubj(chat-2, he-1), amod(thing-5, small-4), prep_about(chat-2, thing-5), sumo(Character, he-1), sumo(SubjectiveStrongNegativeAttribute, small-4), sumo(SubjectiveAssessmentAttribute, thing-5), sumo(Communication, chat-2), tense(PAST, chat-2), number(PLURAL, thing-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance chat-2 Communication) (refers chat-2 thing-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testDisagreeAbout() {
        // Stakeholders disagreed about climate change.
        String input = "root(ROOT-0, disagree-2), nsubj(disagree-2, stakeholder-1), prep_about(disagree-2, climate-4), sumo(Communication, disagree-2), sumo(QuantityChange, climate-4), number(PLURAL, stakeholder-1), tense(PAST, disagree-2), number(SINGULAR, climate-4), number(SINGULAR, change-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance disagree-2 Communication) (refers disagree-2 climate-4))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testFeelAbout() {
        // How do you feel about it?
        String input = "root(ROOT-0, feel-4), advmod(feel-4, how-1), aux(feel-4, do-2), nsubj(feel-4, you-3), prep_about(feel-4, it-6), sumo(EmotionalState, feel-4)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance feel-4 EmotionalState) (refers feel-4 it-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testTeachAbout() {
        // The course teaches about bioinformatics.
        String input = "root(ROOT-0, teach-3), det(course-2, the-1), nsubj(teach-3, course-2), prep_about(teach-3, bioinformatic-5), sumo(EducationalProcess, course-2), sumo(EducationalProcess, teach-3), number(SINGULAR, course-2), tense(PRESENT, teach-3), number(PLURAL, bioinformatic-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance teach-3 EducationalProcess) (refers teach-3 bioinformatic-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testYellAbout() {
        // The girls yelled about the accident.
        String input = "root(ROOT-0, yell-3), det(girl-2, the-1), nsubj(yell-3, girl-2), det(accident-6, the-5), prep_about(yell-3, accident-6), sumo(Female, girl-2), sumo(Process, accident-6), sumo(Vocalizing, yell-3), number(PLURAL, girl-2), tense(PAST, yell-3), number(SINGULAR, accident-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance yell-3 Vocalizing) (refers yell-3 accident-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testAdviseXAbout() {
        // The doctor advised the patient about the treatment.
        String input = "root(ROOT-0, advise-3), det(doctor-2, the-1), nsubj(advise-3, doctor-2), det(patient-5, the-4), dobj(advise-3, patient-5), det(treatment-8, the-7), prep_about(advise-3, treatment-8), sumo(Human, patient-5), sumo(Directing, advise-3), sumo(TherapeuticProcess, treatment-8), sumo(MedicalDoctor, doctor-2), number(SINGULAR, doctor-2), tense(PAST, advise-3), number(SINGULAR, patient-5), number(SINGULAR, treatment-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance advise-3 Directing) (refers advise-3 treatment-8))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testAlarmAbout() {
        // There was an alarm about the danger.
        String input = "root(ROOT-0, be-2), expl(be-2, there-1), det(alarm-4, a-3), nsubj(be-2, alarm-4), det(danger-7, the-6), prep_about(alarm-4, danger-7), sumo(EmotionalState, alarm-4), sumo(SubjectiveAssessmentAttribute, danger-7), sumo(SubjectiveAssessmentAttribute, there-1), tense(PAST, be-2), number(SINGULAR, alarm-4), number(SINGULAR, danger-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance alarm-4 EmotionalState) (refers alarm-4 danger-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testBeliefAbout() {
        // Different people have different beliefs about things.
        String input = "root(ROOT-0, have-3), amod(people-2, different-1), nsubj(have-3, people-2), amod(belief-5, different-4), dobj(have-3, belief-5), prep_about(belief-5, thing-7), sumo(GroupOfPeople, people-2), sumo(Proposition, belief-5), sumo(SubjectiveAssessmentAttribute, thing-7), sumo(equal, different-1), sumo(equal, different-4), number(PLURAL, people-2), tense(PRESENT, have-3), number(PLURAL, belief-5), number(PLURAL, thing-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance belief-5 Proposition) (refers belief-5 thing-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testTheChatAbout() {
        // The chat about the company led to a discussion.
        String input = "root(ROOT-0, lead-6), det(chat-2, the-1), nsubj(lead-6, chat-2), det(company-5, the-4), prep_about(chat-2, company-5), det(discussion-9, a-8), prep_to(lead-6, discussion-9), sumo(Communication, chat-2), sumo(Guiding, lead-6), sumo(Corporation, company-5), sumo(Text, discussion-9), number(SINGULAR, chat-2), number(SINGULAR, company-5), tense(PAST, lead-6), number(SINGULAR, discussion-9)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance chat-2 Communication) (refers chat-2 company-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testChoiceAbout() {
        // Students have choices about their majors.
        String input = "root(ROOT-0, have-2), nsubj(have-2, student-1), dobj(have-2, choice-3), poss(major-7, student-5), prep_about(choice-3, major-7), sumo(Student, student-5), sumo(Student, student-1), sumo(Learning, major-7), sumo(Selecting, choice-3), number(PLURAL, student-1), tense(PRESENT, have-2), number(PLURAL, choice-3), number(PLURAL, student-5), number(PLURAL, major-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance choice-3 Selecting) (refers choice-3 major-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testCommunicationAbout() {
        // Communication about the operation got interrupted.
        String input = "root(ROOT-0, interrupted-6), nsubj(interrupted-6, communication-1), det(operation-4, the-3), prep_about(communication-1, operation-4), dep(interrupted-6, get-5), sumo(Communication, communication-1), sumo(Obligation, operation-4), sumo(SubjectiveAssessmentAttribute, interrupted-6), sumo(Getting, get-5), number(SINGULAR, communication-1), number(SINGULAR, operation-4), tense(PAST, get-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance communication-1 Communication) (refers communication-1 operation-4))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testConclusionAbout() {
        // This study provides conclusions about human behavior.
        String input = "root(ROOT-0, provide-3), det(study-2, this-1), nsubj(provide-3, study-2), dobj(provide-3, conclusion-4), amod(behavior-7, human-6), prep_about(conclusion-4, behavior-7), sumo(BodyMotion, behavior-7), sumo(Investigating, study-2), sumo(Human, human-6), sumo(Putting, provide-3), sumo(conclusion, conclusion-4), number(SINGULAR, study-2), tense(PRESENT, provide-3), number(PLURAL, conclusion-4), number(SINGULAR, behavior-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance conclusion-4 conclusion) (refers conclusion-4 behavior-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testConsensusAbout() {
        // The committee has consensus about funding.
        String input = "root(ROOT-0, have-3), det(committee-2, the-1), nsubj(have-3, committee-2), dobj(have-3, consensus-4), prep_about(consensus-4, funding-6), sumo(FinancialTransaction, funding-6), sumo(Commission, committee-2), sumo(Cooperation, consensus-4), number(SINGULAR, committee-2), tense(PRESENT, have-3), number(SINGULAR, consensus-4), number(SINGULAR, funding-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance consensus-4 Cooperation) (refers consensus-4 funding-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testConspiracyAbout() {
        // He did not participate in the conspiracy about royal succession.
        String input = "root(ROOT-0, participate-4), nsubj(participate-4, he-1), aux(participate-4, do-2), neg(participate-4, not-3), det(conspiracy-7, the-6), prep_in(participate-4, conspiracy-7), amod(succession-10, royal-9), prep_about(conspiracy-7, succession-10), sumo(Character, he-1), sumo(Promise, conspiracy-7), sumo(SocialInteraction, participate-4), sumo(IntentionalProcess, do-2), sumo(Position, royal-9), sumo(TemporalRelation, succession-10), number(SINGULAR, conspiracy-7), number(SINGULAR, succession-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance conspiracy-7 Promise) (refers conspiracy-7 succession-10))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testCounselingAbout() {
        // They received counseling about their marriage.
        String input = "root(ROOT-0, receive-2), nsubj(receive-2, they-1), xcomp(receive-2, counsel-3), poss(marriage-7, they-5), prep_about(counsel-3, marriage-7), sumo(Directing, counsel-3), sumo(MarriageContract, marriage-7), sumo(Getting, receive-2), tense(PAST, receive-2), number(SINGULAR, marriage-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance counsel-3 Directing) (refers counsel-3 marriage-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testDecisionAbout() {
        // The decision about the matter is up to the committee.
        String input = "root(ROOT-0, be-6), det(decision-2, the-1), nsubj(be-6, decision-2), det(matter-5, the-4), prep_about(decision-2, matter-5), det(committee-10, the-9), advmod(be-6, up-7), prep_to(up-7, committee-10), sumo(StateOfMind, up-7), sumo(Deciding, decision-2), sumo(Commission, committee-10), sumo(Proposition, matter-5), number(SINGULAR, decision-2), number(SINGULAR, matter-5), tense(PRESENT, be-6), number(SINGULAR, committee-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance decision-2 Deciding) (refers decision-2 matter-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testDisagreementAbout() {
        // They had a disagreement about their plan.
        String input = "root(ROOT-0, have-2), nsubj(have-2, they-1), det(disagreement-4, a-3), dobj(have-2, disagreement-4), poss(plan-8, they-6), prep_about(disagreement-4, plan-8), sumo(Plan, plan-8), sumo(SubjectiveAssessmentAttribute, disagreement-4), tense(PAST, have-2), number(SINGULAR, disagreement-4), number(SINGULAR, plan-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance disagreement-4 SubjectiveAssessmentAttribute) (refers disagreement-4 plan-8))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testDiscoveryAbout() {
        // The discovery about ancient ruins made a headline.
        String input = "root(ROOT-0, make-6), det(discovery-2, the-1), nsubj(make-6, discovery-2), amod(ruin-5, ancient-4), prep_about(discovery-2, ruin-5), det(headline-8, a-7), dobj(make-6, headline-8), sumo(Destruction, ruin-5), sumo(Human, ancient-4), sumo(Learning, discovery-2), sumo(Text, headline-8), number(SINGULAR, discovery-2), number(PLURAL, ruin-5), tense(PAST, make-6), number(SINGULAR, headline-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance discovery-2 Learning) (refers discovery-2 ruin-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testDoubtAbout() {
        // There is a growing doubt about the cause of the accident.
        String input = "root(ROOT-0,be-2), expl(be-2,there-1), det(doubt-5,a-3), amod(doubt-5,grow-4), nsubj(be-2,doubt-5), det(cause-8,the-7), prep_about(doubt-5,cause-8), det(accident-11,the-10), prep_of(cause-8,accident-11), sumo(Process,grow-4), sumo(Process,cause-8), sumo(PsychologicalAttribute,doubt-5), sumo(SubjectiveAssessmentAttribute,there-1), sumo(Process,accident-11), tense(PRESENT,be-2), number(SINGULAR,doubt-5), number(SINGULAR,cause-8), number(SINGULAR,accident-11)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance doubt-5 PsychologicalAttribute) (refers doubt-5 cause-8))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testEducationAbout() {
        // Education about healthcare is long overdue.
        String input = "root(ROOT-0,overdue-6), nsubj(overdue-6,Education-1), prep_about(Education-1,healthcare-3), cop(overdue-6,be-4), advmod(overdue-6,long-5), sumo(SubjectiveAssessmentAttribute,long-5), sumo(EducationalProcess,Education-1), sumo(NormativeAttribute,overdue-6), sumo(Maintaining,healthcare-3), number(SINGULAR,Education-1), number(SINGULAR,healthcare-3), tense(PRESENT,be-4)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance Education-1 EducationalProcess) (refers Education-1 healthcare-3))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testEncouragementAbout() {
        // He gave me encouragement about my career.
        String input = "oot(ROOT-0,give-2), nsubj(give-2,he-1), iobj(give-2,I-3), dobj(give-2,encouragement-4), poss(career-8,I-6), prep_about(encouragement-4,career-8), sumo(Character,he-1), sumo(Process,give-2), sumo(SkilledOccupation,career-8), sumo(ExpressingApproval,encouragement-4), tense(PAST,give-2), number(SINGULAR,encouragement-4), number(SINGULAR,career-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance encouragement-4 ExpressingApproval) (refers encouragement-4 career-8))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testEvidenceAbout() {
        // The evidence about the subject is solid.
        String input = "root(ROOT-0,solid-7), det(evidence-2,the-1), nsubj(solid-7,evidence-2), det(suspect-5,the-4), prep_about(evidence-2,suspect-5), cop(solid-7,be-6), sumo(Reasoning,evidence-2), sumo(Solid,solid-7), sumo(IntentionalPsychologicalProcess,suspect-5), number(SINGULAR,evidence-2), number(SINGULAR,suspect-5), tense(PRESENT,be-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance evidence-2 Reasoning) (refers evidence-2 suspect-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testIdeaAbout() {
        // We had no idea about the problem.
        String input = "root(ROOT-0,have-2), nsubj(have-2,we-1), neg(idea-4,no-3), dobj(have-2,idea-4), det(problem-7,the-6), prep_about(idea-4,problem-7), sumo(SubjectiveAssessmentAttribute,problem-7), sumo(Proposition,idea-4), tense(PAST,have-2), number(SINGULAR,idea-4), number(SINGULAR,problem-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance idea-4 Proposition) (refers idea-4 problem-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testIndicationAbout() {
        // There are indications about economic recovery.
        String input = "root(ROOT-0,be-2), expl(be-2,there-1), nsubj(be-2,indication-3), amod(recovery-6,economic-5), prep_about(indication-3,recovery-6), sumo(SubjectiveAssessmentAttribute,recovery-6), sumo(SubjectiveAssessmentAttribute,there-1), sumo(FinancialTransaction,economic-5), sumo(ContentBearingObject,indication-3), tense(PRESENT,be-2), number(PLURAL,indication-3), number(SINGULAR,recovery-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance indication-3 ContentBearingObject) (refers indication-3 recovery-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testInferenceAbout() {
        // Inference about data is not totally accurate.
        String input = "root(ROOT-0,accurate-7), nsubj(accurate-7,inference-1), prep_about(inference-1,datum-3), cop(accurate-7,be-4), neg(accurate-7,not-5), advmod(accurate-7,totally-6), sumo(SubjectiveAssessmentAttribute,totally-6), sumo(DatumFn,datum-3), sumo(True,accurate-7), sumo(Reasoning,inference-1), number(SINGULAR,inference-1), number(PLURAL,datum-3), tense(PRESENT,be-4)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance inference-1 Reasoning) (refers inference-1 datum-3))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testInformationAbout() {
        // His information about the situation was not correct.
        String input = "root(ROOT-0,correct-8), poss(information-2,he-1), nsubj(correct-8,information-2), det(situation-5,the-4), prep_about(information-2,situation-5), aux(correct-8,be-6), neg(correct-8,not-7), sumo(SubjectiveAssessmentAttribute,situation-5), sumo(IntentionalProcess,correct-8), sumo(FactualText,information-2), number(SINGULAR,information-2), number(SINGULAR,situation-5), tense(PAST,correct-8), aspect(PROGRESSIVE,correct-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance information-2 FactualText) (refers information-2 situation-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testInsightAbout() {
        // You have good insights about the matter.
        String input = "root(ROOT-0,have-2), nsubj(have-2,you-1), amod(insight-4,good-3), dobj(have-2,insight-4), det(matter-7,the-6), prep_about(insight-4,matter-7), sumo(SubjectiveAssessmentAttribute,good-3), sumo(SubjectiveAssessmentAttribute,insight-4), sumo(Proposition,matter-7), tense(PRESENT,have-2), number(PLURAL,insight-4), number(SINGULAR,matter-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance insight-4 SubjectiveAssessmentAttribute) (refers insight-4 matter-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testIronyAbout() {
        // That's the biggest irony about the situation.
        String input = "root(ROOT-0,irony-5), nsubj(irony-5,that-1), cop(irony-5,be-2), det(irony-5,the-3), amod(irony-5,biggest-4), det(situation-8,the-7), prep_about(irony-5,situation-8), sumo(SubjectiveAssessmentAttribute,irony-5), sumo(SubjectiveAssessmentAttribute,situation-8), tense(PRESENT,be-2), number(SINGULAR,irony-5), number(SINGULAR,situation-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance irony-5 SubjectiveAssessmentAttribute) (refers irony-5 situation-8))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testIssueAbout() {
        // The issue about economic inequality continues to dominate the debate.
        String input = "root(ROOT-0,continue-6), det(issue-2,the-1), nsubj(continue-6,issue-2), amod(inequality-5,economic-4), prep_about(issue-2,inequality-5), aux(dominate-8,to-7), xcomp(continue-6,dominate-8), det(debate-10,the-9), dobj(dominate-8,debate-10), sumo(IntentionalProcess,continue-6), sumo(Proposition,issue-2), sumo(Attribute,inequality-5), sumo(Process,dominate-8), sumo(FinancialTransaction,economic-4), sumo(Debating,debate-10), number(SINGULAR,issue-2), number(SINGULAR,inequality-5), tense(PRESENT,continue-6), number(SINGULAR,debate-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance issue-2 Proposition) (refers issue-2 inequality-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testJokeAbout() {
        // The joke about his appearance made him angry.
        String input = "root(ROOT-0,make-6), det(joke-2,the-1), nsubj(make-6,joke-2), poss(appearance-5,he-4), prep_about(joke-2,appearance-5), poss(angry-8,he-7), dobj(make-6,angry-8), sumo(SubjectiveAssessmentAttribute,appearance-5), sumo(SubjectiveAssessmentAttribute,joke-2), sumo(Anger,angry-8), number(SINGULAR,joke-2), number(SINGULAR,appearance-5), tense(PAST,make-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance joke-2 SubjectiveAssessmentAttribute) (refers joke-2 appearance-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testJudgmentAbout() {
        // The judgment about the crime was announced today.
        String input = "root(ROOT-0,announce-7), det(judgment-2,the-1), nsubjpass(announce-7,judgment-2), det(crime-5,the-4), prep_about(judgment-2,crime-5), auxpass(announce-7,be-6), tmod(announce-7,today-8), sumo(NormativeAttribute,crime-5), sumo(Proposition,judgment-2), sumo(Disseminating,announce-7), sumo(SubjectiveAssessmentAttribute,today-8), number(SINGULAR,judgment-2), number(SINGULAR,crime-5), tense(PAST,be-6), number(SINGULAR,today-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance judgment-2 Proposition) (refers judgment-2 crime-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testKnowledgeAbout() {
        // He has extensive knowledge about the subject.
        String input = "root(ROOT-0,have-2), nsubj(have-2,he-1), amod(knowledge-4,extensive-3), dobj(have-2,knowledge-4), det(subject-7,the-6), prep_about(knowledge-4,subject-7), sumo(Character,he-1), sumo(PsychologicalAttribute,knowledge-4), sumo(SubjectiveWeakNegativeAttribute,extensive-3), sumo(Proposition,subject-7), tense(PRESENT,have-2), number(SINGULAR,knowledge-4), number(SINGULAR,subject-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance knowledge-4 PsychologicalAttribute) (refers knowledge-4 subject-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testLearningAbout() {
        // We acquired scientific learning about the ecosystem.
        String input = "root(ROOT-0,acquire-2), nsubj(acquire-2,we-1), dobj(acquire-2,scientific-3), vmod(scientific-3,learn-4), det(ecosystem-7,the-6), prep_about(learn-4,ecosystem-7), sumo(Ecosystem,ecosystem-7), sumo(Science,scientific-3), sumo(Learning,learn-4), sumo(Getting,acquire-2), tense(PAST,acquire-2), number(SINGULAR,ecosystem-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance learn-4 Learning) (refers learn-4 ecosystem-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testLessonAbout() {
        // The lesson about statistics was very helpful.
        String input = "oot(ROOT-0,helpful-7), det(lesson-2,the-1), nsubj(helpful-7,lesson-2), prep_about(lesson-2,statistics-4), cop(helpful-7,be-5), advmod(helpful-7,very-6), sumo(Quantity,statistics-4), sumo(EducationalProcess,lesson-2), sumo(SubjectiveWeakPositiveAttribute,helpful-7), number(SINGULAR,lesson-2), number(PLURAL,statistics-4), tense(PAST,be-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance lesson-2 EducationalProcess) (refers lesson-2 statistics-4))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testMetadataAbout() {
        // The system stores metadata about books.
        String input = "root(ROOT-0,metadata-4), prep_about(metadata-4,book-6), det(system-2,the-1), dep(metadata-4,system-2), sumo(Book,book-6), sumo(FactualText,metadata-4), number(SINGULAR,system-2), number(PLURAL,store-3), number(PLURAL,book-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance metadata-4 FactualText) (refers metadata-4 book-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testNotionAbout() {
        // We had no clear notion about the policy.
        String input = "root(ROOT-0,have-2), nsubj(have-2,we-1), neg(notion-5,no-3), amod(notion-5,clear-4), dobj(have-2,notion-5), det(policy-8,the-7), prep_about(notion-5,policy-8), sumo(Proposition,notion-5), sumo(Policy,policy-8), sumo(SubjectiveAssessmentAttribute,clear-4), tense(PAST,have-2), number(SINGULAR,notion-5), number(SINGULAR,policy-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance notion-5 Proposition) (refers notion-5 policy-8))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testOpinionAbout() {
        // People have different opinions about religion.
        String input = "root(ROOT-0,have-2), nsubj(have-2,people-1), amod(opinion-4,different-3), dobj(have-2,opinion-4), prep_about(opinion-4,religion-6), sumo(equal,different-3), sumo(GroupOfPeople,people-1), sumo(Proposition,religion-6), sumo(Proposition,opinion-4), number(PLURAL,people-1), tense(PRESENT,have-2), number(PLURAL,opinion-4), number(SINGULAR,religion-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance opinion-4 Proposition) (refers opinion-4 religion-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testPerspectiveAbout() {
        // He has a fresh perspective about the matter.
        String input = "root(ROOT-0,have-2), nsubj(have-2,he-1), det(perspective-5,a-3), amod(perspective-5,fresh-4), dobj(have-2,perspective-5), det(matter-8,the-7), prep_about(perspective-5,matter-8), sumo(Character,he-1), sumo(SubjectiveWeakPositiveAttribute,fresh-4), sumo(Proposition,perspective-5), sumo(Proposition,matter-8), tense(PRESENT,have-2), number(SINGULAR,perspective-5), number(SINGULAR,matter-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance perspective-5 Proposition) (refers perspective-5 matter-8))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testPresentationAbout() {
        // His presentation about his research was very interesting.
        String input = "root(ROOT-0,interesting-12), det(doomsday-2,the-1), poss(presentation-4,doomsday-2), nsubj(interesting-12,presentation-4), det(doomsday-7,the-6), poss(research-9,doomsday-7), prep_about(presentation-4,research-9), cop(interesting-12,be-10), advmod(interesting-12,very-11), sumo(Investigating,research-9), sumo(SubjectiveAssessmentAttribute,interesting-12), sumo(SubjectiveAssessmentAttribute,doomsday-2), sumo(IntentionalProcess,presentation-4), sumo(SubjectiveAssessmentAttribute,doomsday-7), number(SINGULAR,doomsday-2), number(SINGULAR,presentation-4), number(SINGULAR,doomsday-7), number(SINGULAR,research-9), tense(PAST,be-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance presentation-4 IntentionalProcess) (refers presentation-4 research-9))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testReflectionAbout() {
        // Reflections about the past are the foundations for building the future.
        String input = "root(ROOT-0,foundation-7), nsubj(foundation-7,reflection-1), det(past-4,the-3), prep_about(reflection-1,past-4), cop(foundation-7,be-5), det(foundation-7,the-6), prepc_for(foundation-7,build-9), det(future-11,the-10), dobj(build-9,future-11), sumo(Making,build-9), sumo(FutureFn,future-11), sumo(PastFn,past-4), sumo(SubjectiveAssessmentAttribute,foundation-7), sumo(PsychologicalProcess,reflection-1), number(PLURAL,reflection-1), number(SINGULAR,past-4), tense(PRESENT,be-5), number(PLURAL,foundation-7), number(SINGULAR,future-11)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance reflection-1 PsychologicalProcess) (refers reflection-1 past-4))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testResultAbout() {
        //
        String input = "";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testRevelationAbout() {
        // The revelation about the organization came as a surprise.
        String input = "root(ROOT-0,come-6), det(revelation-2,the-1), nsubj(come-6,revelation-2), det(organization-5,the-4), prep_about(revelation-2,organization-5), det(surprise-9,a-8), prep_as(come-6,surprise-9), sumo(BodyMotion,come-6), sumo(EmotionalState,surprise-9), sumo(Organization,organization-5), sumo(Communication,revelation-2), number(SINGULAR,revelation-2), number(SINGULAR,organization-5), tense(PAST,come-6), number(SINGULAR,surprise-9)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance revelation-2 Communication) (refers revelation-2 organization-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testSkepticismAbout() {
        // Skepticism about the procedure has been voiced.
        String input = "root(ROOT-0,voice-7), nsubjpass(voice-7,skepticism-1), det(procedure-4,the-3), prep_about(skepticism-1,procedure-4), aux(voice-7,have-5), auxpass(voice-7,be-6), sumo(PsychologicalAttribute,skepticism-1), sumo(Speaking,voice-7), sumo(Procedure,procedure-4), number(SINGULAR,skepticism-1), number(SINGULAR,procedure-4), tense(PRESENT,voice-7), aspect(PERFECT,voice-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance skepticism-1 PsychologicalAttribute) (refers skepticism-1 procedure-4))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testSpeculationAbout() {
        // There is growing speculation about the election result.
        String input = "root(ROOT-0,be-2), expl(be-2,there-1), amod(speculation-4,grow-3), nsubj(be-2,speculation-4), det(election-7,the-6), prep_about(speculation-4,election-7), sumo(Process,grow-3), sumo(Proposition,speculation-4), sumo(SubjectiveAssessmentAttribute,there-1), tense(PRESENT,be-2), number(SINGULAR,speculation-4), number(SINGULAR,election-7), number(SINGULAR,result-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance speculation-4 Proposition) (refers speculation-4 election-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testStatisticsAbout() {
        // The report contains statistics about the size of the market.
        String input = "root(ROOT-0,contain-3), det(report-2,the-1), nsubj(contain-3,report-2), dobj(contain-3,statistics-4), det(size-7,the-6), prep_about(statistics-4,size-7), det(market-10,the-9), prep_of(size-7,market-10), sumo(Attribute,contain-3), sumo(Quantity,statistics-4), sumo(FinancialTransaction,market-10), sumo(Stating,report-2), sumo(Attribute,size-7), number(SINGULAR,report-2), tense(PRESENT,contain-3), number(PLURAL,statistics-4), number(SINGULAR,size-7), number(SINGULAR,market-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance statistics-4 Quantity) (refers statistics-4 size-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testStorylineAbout() {
        // The storyline about the popular comic-strip character is very funny.
        String input = "root(ROOT-0,funny-10), det(storyline-2,the-1), nsubj(funny-10,storyline-2), det(character-7,the-4), amod(character-7,popular-5), amod(character-7,comic-strip-6), prep_about(storyline-2,character-7), cop(funny-10,be-8), advmod(funny-10,very-9), sumo(SubjectiveStrongPositiveAttribute,funny-10), sumo(Proposition,storyline-2), sumo(SubjectiveWeakPositiveAttribute,popular-5), sumo(SubjectiveStrongPositiveAttribute,comic-strip-6), sumo(Character,character-7), number(SINGULAR,storyline-2), number(SINGULAR,character-7), tense(PRESENT,be-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance storyline-2 Proposition) (refers storyline-2 character-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testSuspicionAbout() {
        // Our suspicion about the new drug has been confirmed.
        String input = "root(ROOT-0,confirm-10), poss(suspicion-3,we-1), nsubjpass(confirm-10,suspicion-3), det(drug-7,the-5), amod(drug-7,new-6), prep_about(suspicion-3,drug-7), aux(confirm-10,have-8), auxpass(confirm-10,be-9), sumo(Proposition,suspicion-3), sumo(Process,confirm-10), sumo(SubjectiveAssessmentAttribute,new-6), sumo(BiologicallyActiveSubstance,drug-7), number(SINGULAR,suspicion-3), number(SINGULAR,drug-7), tense(PRESENT,confirm-10), aspect(PERFECT,confirm-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance suspicion-3 Proposition) (refers suspicion-3 drug-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testUncertaintyAbout() {
        // The uncertainty about the economic prospect looms large.
        String input = "root(ROOT-0,loom-7), det(uncertainty-2,the-1), nsubj(loom-7,uncertainty-2), det(prospect-6,the-4), amod(prospect-6,economic-5), prep_about(uncertainty-2,prospect-6), acomp(loom-7,large-8), sumo(SubjectiveWeakPositiveAttribute,large-8), sumo(SubjectiveAssessmentAttribute,loom-7), sumo(SubjectiveAssessmentAttribute,prospect-6), sumo(ProbabilityRelation,uncertainty-2), sumo(FinancialTransaction,economic-5), number(SINGULAR,uncertainty-2), number(SINGULAR,prospect-6), tense(PRESENT,loom-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance uncertainty-2 ProbabilityRelation) (refers uncertainty-2 prospect-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testWondersAbout() {
        // His wonders about the natural world stayed with him all his life.
        String input = "root(ROOT-0,stay-8), nsubj(wonder-3,he-1), csubj(stay-8,wonder-3), det(world-7,the-5), amod(world-7,natural-6), prep_about(wonder-3,world-7), prep_with(stay-8,he-10), advmod(stay-8,all-11), dep(stay-8,he-12), dep(he-12,life-14), sumo(PsychologicalAttribute,wonder-3), sumo(SubjectiveWeakPositiveAttribute,natural-6), sumo(SubjectiveAssessmentAttribute,life-14), sumo(PastFn,stay-8), sumo(Object,world-7), tense(PRESENT,wonder-3), number(SINGULAR,world-7), tense(PAST,stay-8), number(SINGULAR,life-14)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance wonder-3 PsychologicalAttribute) (refers wonder-3 world-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    private Set<String> getCleanedOutput(ArrayList<String> kifClauses) {
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\n ", "")).collect(Collectors.toSet());
        return cleanedActual;
    }

}
