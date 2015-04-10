/*
Copyright 2014-2015 IPsoft

Author: Sofia Athenikos sofia.athenikos@ipsoft.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA
*/
package com.articulate.sigma.semRewrite;

import com.articulate.sigma.KBmanager;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

public class RulePrepAboutRefersToIntegrationTest {

    public static Interpreter interpreter;

    @BeforeClass
    public static void initializeKbManager() {
        KBmanager.getMgr().initializeOnce();
    }

    @BeforeClass
    public static void initializeInterpreter() {
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
    public void testAnnounceAbout() {
        // John announced about his plan.
        String input = "root(ROOT-0, announce-2), nsubj(announce-2, John-1), poss(plan-6, John-4), prep_about(announce-2, plan-6), names(John-1, \"John\"), sumo(Disseminating, announce-2), attribute(John-4, Male), attribute(John-1, Male), sumo(Human, John-1), sumo(Human, John-4), names(John-4, \"John\"), sumo(Plan, plan-6), number(SINGULAR, John-1), tense(PAST, announce-2), number(SINGULAR, John-4), number(SINGULAR, plan-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance announce-2 Disseminating) (refers announce-2 plan-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testAskAbout() {
        // John asked about my opinion.
        String input = "root(ROOT-0, ask-2), nsubj(ask-2, John-1), poss(opinion-5, my-4), prep_about(ask-2, opinion-5), names(John-1, \"John\"), sumo(Proposition, opinion-5), attribute(John-1, Male), sumo(Human, John-1), sumo(Questioning, ask-2), number(SINGULAR, John-1), tense(PAST, ask-2), number(SINGULAR, opinion-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance ask-2 Questioning) (refers ask-2 opinion-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testBeAbout() {
        // Section Five is about the impact.
        String input = "root(ROOT-0, be-3), nsubj(be-3, Section-1), num(Section-1, five-2), det(impact-6, the-5), prep_about(be-3, impact-6), sumo(Text, Section-1), sumo(Impacting, impact-6), number(SINGULAR, Section-1), tense(PRESENT, be-3), number(SINGULAR, impact-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance impact-6 Impacting) (refers Section-1 impact-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testComplainAbout() {
        // They seldom complain about unsafe work environments.
        String input = "root(ROOT-0, selcom-2), nsubj(selcom-2, they-1), xcomp(selcom-2, complain-3), amod(work-6, unsafe-5), prep_about(complain-3, work-6), sumo(SubjectiveWeakNegativeAttribute, unsafe-5), sumo(Expressing, complain-3), tense(PRESENT, selcom-2), number(SINGULAR, work-6), number(PLURAL, environment-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance complain-3 Expressing) (refers complain-3 work-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testExplainAbout() {
        // He explained about the reason.
        String input = "root(ROOT-0, explain-2), nsubj(explain-2, he-1), det(reason-5, the-4), prep_about(explain-2, reason-5), sumo(Character, he-1), sumo(Proposition, reason-5), sumo(ContentDevelopment, explain-2), tense(PAST, explain-2), number(SINGULAR, reason-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance explain-2 ContentDevelopment) (refers explain-2 reason-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testForgetAbout() {
        // She forgot about the appointment.
        String input = "root(ROOT-0, forget-2), nsubj(forget-2, she-1), det(appointment-5, the-4), prep_about(forget-2, appointment-5), sumo(Stating, appointment-5), sumo(Remembering, forget-2), tense(PAST, forget-2), number(SINGULAR, appointment-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance forget-2 Remembering) (refers forget-2 appointment-5))",
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testGeneralizeAbout() {
        // We tend to generalize about varied viewpoints.
        String input = "root(ROOT-0, tend-2), nsubj(tend-2, we-1), aux(generalize-4, to-3), xcomp(tend-2, generalize-4), amod(viewpoint-7, varied-6), prep_about(generalize-4, viewpoint-7), sumo(Process, varied-6), sumo(TraitAttribute, tend-2), sumo(SubjectiveAssessmentAttribute, viewpoint-7), sumo(IntentionalPsychologicalProcess, generalize-4), tense(PRESENT, tend-2), number(PLURAL, viewpoint-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance generalize-4 IntentionalPsychologicalProcess) (refers generalize-4 viewpoint-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testGossipAbout() {
        // It was so we could gossip about everyone who was not there.
        String input = "root(ROOT-0, so-3), nsubj(so-3, it-1), cop(so-3, be-2), nsubj(gossip-6, we-4), aux(gossip-6, could-5), ccomp(so-3, gossip-6), prep_about(gossip-6, everyone-8), nsubj(be-10, who-9), rcmod(everyone-8, be-10), neg(be-10, not-11), advmod(be-10, there-12), sumo(FieldOfStudy, it-1), sumo(LinguisticCommunication, gossip-6), tense(PAST, be-2), number(SINGULAR, gossip-6), number(SINGULAR, everyone-8), tense(PAST, be-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance gossip-6 LinguisticCommunication) (refers gossip-6 everyone-8))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testHearAbout() {
        // We hear about the issue all the time.
        String input = "root(ROOT-0, hear-2), nsubj(hear-2, we-1), det(issue-5, the-4), prep_about(hear-2, issue-5), dobj(hear-2, all-6), sumo(Proposition, issue-5), sumo(SubjectiveAssessmentAttribute, all-6), sumo(Hearing, hear-2), tense(PRESENT, hear-2), number(SINGULAR, issue-5), number(SINGULAR, time-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance hear-2 Hearing) (refers hear-2 issue-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testInquireAbout() {
        // Many people are inquiring about the new policy.
        String input = "root(ROOT-0, inquire-4), amod(people-2, many-1), nsubj(inquire-4, people-2), aux(inquire-4, be-3), det(policy-8, the-6), amod(policy-8, new-7), prep_about(inquire-4, policy-8), sumo(GroupOfPeople, people-2), sumo(Policy, policy-8), sumo(SubjectiveAssessmentAttribute, new-7), sumo(Questioning, inquire-4), sumo(SubjectiveAssessmentAttribute, many-1), number(PLURAL, people-2), tense(PRESENT, inquire-4), aspect(PROGRESSIVE, inquire-4), number(SINGULAR, policy-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance inquire-4 Questioning) (refers inquire-4 policy-8))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testLaughAbout() {
        // People are laughing about the incident.
        String input = "root(ROOT-0, laugh-3), nsubj(laugh-3, people-1), aux(laugh-3, be-2), det(incident-6, the-5), prep_about(laugh-3, incident-6), sumo(GroupOfPeople, people-1), sumo(Process, incident-6), sumo(Laughing, laugh-3), number(PLURAL, people-1), tense(PRESENT, laugh-3), aspect(PROGRESSIVE, laugh-3), number(SINGULAR, incident-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance laugh-3 Laughing) (refers laugh-3 incident-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testMutterAbout() {
        // He muttered about al-Qaeda.
        String input = "root(ROOT-0, mutter-2), nsubj(mutter-2, he-1), prep_about(mutter-2, al-Qaeda-4), sumo(Character, he-1), sumo(Alabama, al-Qaeda-4), sumo(Speaking, mutter-2), tense(PAST, mutter-2), number(SINGULAR, al-Qaeda-4)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance mutter-2 Speaking) (refers mutter-2 al-Qaeda-4))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testProclaimAbout() {
        // The Messiah proclaimed about a new world.
        String input = "root(ROOT-0, proclaim-3), det(Messiah-2, the-1), nsubj(proclaim-3, Messiah-2), det(world-7, a-5), amod(world-7, new-6), prep_about(proclaim-3, world-7), sumo(Declaring, proclaim-3), sumo(CognitiveAgent, Messiah-2), sumo(Object, world-7), sumo(SubjectiveAssessmentAttribute, new-6), number(SINGULAR, Messiah-2), tense(PAST, proclaim-3), number(SINGULAR, world-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance proclaim-3 Declaring) (refers proclaim-3 world-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testReadAbout() {
        // He read about the accident in the newspaper.
        String input = "root(ROOT-0, read-2), nsubj(read-2, he-1), det(accident-5, the-4), prep_about(read-2, accident-5), det(newspaper-8, the-7), prep_in(accident-5, newspaper-8), sumo(Character, he-1), sumo(Process, accident-5), sumo(Newspaper, newspaper-8), sumo(Reading, read-2), tense(PAST, read-2), number(SINGULAR, accident-5), number(SINGULAR, newspaper-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance read-2 Reading) (refers read-2 accident-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testRememberAbout() {
        // He did not remember anything about the accident.
        String input = "oot(ROOT-0, remember-4), nsubj(remember-4, he-1), aux(remember-4, do-2), neg(remember-4, not-3), dobj(remember-4, anything-5), det(accident-8, the-7), prep_about(remember-4, accident-8), sumo(Character, he-1), sumo(IntentionalProcess, do-2), sumo(Process, accident-8), sumo(Remembering, remember-4), number(SINGULAR, anything-5), number(SINGULAR, accident-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance remember-4 Remembering) (refers remember-4 accident-8))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testReportAbout() {
        // The journalist reported about the war.
        String input = "root(ROOT-0, report-3), det(journalist-2, the-1), nsubj(report-3, journalist-2), det(war-6, the-5), prep_about(report-3, war-6), sumo(Journalist, journalist-2), sumo(War, war-6), sumo(Stating, report-3), number(SINGULAR, journalist-2), tense(PAST, report-3), number(SINGULAR, war-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance report-3 Stating) (refers report-3 war-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testSmileAbout() {
        // Proctor smiled about the encounter.
        String input = "root(ROOT-0, smile-2), nsubj(smile-2, Proctor-1), det(encounter-5, the-4), prep_about(smile-2, encounter-5), sumo(Human, Proctor-1), sumo(Smiling, smile-2), sumo(Meeting, encounter-5), names(Proctor-1, \"Proctor\"), number(SINGULAR, Proctor-1), tense(PAST, smile-2), number(SINGULAR, encounter-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance smile-2 Smiling) (refers smile-2 encounter-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testSpeakAbout() {
        // They often spoke about the trip.
        String input = "root(ROOT-0, speak-3), nsubj(speak-3, they-1), advmod(speak-3, often-2), det(trip-6, the-5), prep_about(speak-3, trip-6), sumo(Speaking, speak-3), sumo(Translocation, trip-6), tense(PAST, speak-3), number(SINGULAR, trip-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance speak-3 Speaking) (refers speak-3 trip-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testSpeakUpAbout() {
        // They should speak up about the problem.
        String input = "det(problem-7, the-6), root(ROOT-0, speak-3), nsubj(speak-3, they-1), aux(speak-3, should-2), prep_about(speak-3, problem-7), sumo(SubjectiveAssessmentAttribute, problem-7), sumo(Communication, speak-3), number(SINGULAR, problem-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance speak-3 Communication) (refers speak-3 problem-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testSpeculateAbout() {
        // Policy-makers have speculated about the cause.
        String input = "root(ROOT-0, speculate-3), nsubj(speculate-3, policy-maker-1), aux(speculate-3, have-2), det(cause-6, the-5), prep_about(speculate-3, cause-6), sumo(IntentionalPsychologicalProcess, speculate-3), sumo(Policy, policy-maker-1), sumo(Process, cause-6), number(PLURAL, policy-maker-1), tense(PRESENT, speculate-3), aspect(PERFECT, speculate-3), number(SINGULAR, cause-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance speculate-3 IntentionalPsychologicalProcess) (refers speculate-3 cause-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testStudyAbout() {
        // They study about biology.
        String input = "root(ROOT-0, study-2), nsubj(study-2, they-1), prep_about(study-2, biology-4), sumo(Investigating, study-2), sumo(Biology, biology-4), tense(PRESENT, study-2), number(SINGULAR, biology-4)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance study-2 Investigating) (refers study-2 biology-4))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testTalkAbout() {
        // Doctors talk about the epidemic.
        String input = "det(epidemic-5, the-4), root(ROOT-0, talk-2), nsubj(talk-2, doctor-1), prep_about(talk-2, epidemic-5), sumo(DiseaseOrSyndrome, epidemic-5), sumo(MedicalDoctor, doctor-1), sumo(ExpressingInLanguage, talk-2), number(PLURAL, doctor-1), tense(PRESENT, talk-2)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance talk-2 ExpressingInLanguage) (refers talk-2 epidemic-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testUnderstandAbout() {
        // You should understand about the principles.
        String input = "root(ROOT-0, understand-3), nsubj(understand-3, you-1), aux(understand-3, should-2), det(principle-6, the-5), prep_about(understand-3, principle-6), sumo(Interpreting, understand-3), sumo(Proposition, principle-6), number(PLURAL, principle-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance understand-3 Interpreting) (refers understand-3 principle-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testExciteXAbout() {
        // Great teachers excite students about learning.
        String input = "root(ROOT-0, excite-3), amod(student-2, great-1), nsubj(excite-3, student-2), dobj(excite-3, student-4), prepc_about(excite-3, learn-6), sumo(student, student-2), sumo(student, student-4), sumo(SubjectiveStrongPositiveAttribute, great-1), sumo(Learning, learn-6), sumo(IntentionalPsychologicalProcess, excite-3), number(PLURAL, student-2), tense(PRESENT, excite-3), number(PLURAL, student-4)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance excite-3 IntentionalPsychologicalProcess) (refers excite-3 learn-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testHearAnythingAbout() {
        // I did not hear anything about this before.
        String input = "root(ROOT-0, hear-4), nsubj(hear-4, I-1), aux(hear-4, do-2), neg(hear-4, not-3), dobj(hear-4, anything-5), prep_about(hear-4, this-7), sumo(Hearing, hear-4), sumo(IntentionalProcess, do-2), sumo(AlphabeticCharacter, I-1), number(SINGULAR, anything-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance hear-4 Hearing) (refers hear-4 this-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testKnowAnythingAbout() {
        // He did not know anything about the therapy.
        String input = "root(ROOT-0, know-4), nsubj(know-4, he-1), aux(know-4, do-2), neg(know-4, not-3), det(therapy-7, the-6), prep_about(know-4, therapy-7), sumo(Character, he-1), sumo(TherapeuticProcess, therapy-7), sumo(Interpreting, know-4), sumo(IntentionalProcess, do-2), number(SINGULAR, therapy-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance know-4 Interpreting) (refers know-4 therapy-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testRememberSomethingAbout() {
        // He remembered something about that family.
        String input = "root(ROOT-0, remember-2), nsubj(remember-2, he-1), dobj(remember-2, something-3), det(family-6, that-5), prep_about(remember-2, family-6), sumo(Character, he-1), sumo(FamilyGroup, family-6), sumo(Remembering, remember-2), tense(PAST, remember-2), number(SINGULAR, something-3), number(SINGULAR, family-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance remember-2 Remembering) (refers remember-2 family-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testAdviceAbout() {
        // He gave me an advice about exercise.
        String input = "root(ROOT-0, give-2), nsubj(give-2, he-1), iobj(give-2, I-3), det(advice-5, a-4), dobj(give-2, advice-5), prep_about(advice-5, exercise-7), sumo(Character, he-1), sumo(Requesting, advice-5), sumo(Process, give-2), sumo(RecreationOrExercise, exercise-7), tense(PAST, give-2), number(SINGULAR, advice-5), number(SINGULAR, exercise-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance advice-5 Requesting) (refers advice-5 exercise-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testAgreementAbout() {
        // We have an agreement about the policy.
        String input = "root(ROOT-0, have-2), nsubj(have-2, we-1), det(agreement-4, a-3), dobj(have-2, agreement-4), det(policy-7, the-6), prep_about(agreement-4, policy-7), sumo(Policy, policy-7), sumo(Agreement, agreement-4), tense(PRESENT, have-2), number(SINGULAR, agreement-4), number(SINGULAR, policy-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance agreement-4 Agreement) (refers agreement-4 policy-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testAnxietyAbout() {
        // He has an anxiety about height.
        String input = "root(ROOT-0, have-2), nsubj(have-2, he-1), det(anxiety-4, a-3), dobj(have-2, anxiety-4), prep_about(anxiety-4, height-6), sumo(Character, he-1), sumo(Anxiety, anxiety-4), sumo(height, height-6), tense(PRESENT, have-2), number(SINGULAR, anxiety-4), number(SINGULAR, height-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance anxiety-4 Anxiety) (refers anxiety-4 height-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testArgumentAbout() {
        // The couple had arguments about insurance.
        String input = "root(ROOT-0, have-3), det(couple-2, the-1), nsubj(have-3, couple-2), dobj(have-3, argument-4), prep_about(argument-4, insurance-6), sumo(Obligation, insurance-6), sumo(Argument, argument-4), sumo(GroupOfPeople, couple-2), number(SINGULAR, couple-2), tense(PAST, have-3), number(PLURAL, argument-4), number(SINGULAR, insurance-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance argument-4 Argument) (refers argument-4 insurance-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testArticleAbout() {
        // He gave me an article about the experiment.
        String input = "root(ROOT-0, give-2), nsubj(give-2, he-1), iobj(give-2, I-3), det(article-5, a-4), dobj(give-2, article-5), det(experiment-8, the-7), prep_about(article-5, experiment-8), sumo(Character, he-1), sumo(Process, give-2), sumo(Article, article-5), sumo(Experimenting, experiment-8), tense(PAST, give-2), number(SINGULAR, article-5), number(SINGULAR, experiment-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance article-5 Article) (refers article-5 experiment-8))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testAssumptionAbout() {
        // The nurses have assumptions about doctors.
        String input = "root(ROOT-0, have-3), det(nurse-2, the-1), nsubj(have-3, nurse-2), dobj(have-3, assumption-4), prep_about(assumption-4, doctor-6), sumo(Supposition, assumption-4), sumo(TherapeuticProcess, nurse-2), sumo(MedicalDoctor, doctor-6), number(PLURAL, nurse-2), tense(PRESENT, have-3), number(PLURAL, assumption-4), number(PLURAL, doctor-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance assumption-4 Supposition) (refers assumption-4 doctor-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testBookAbout() {
        // He gave me a book about the famous painter.
        String input = "root(ROOT-0, give-2), nsubj(give-2, he-1), iobj(give-2, I-3), det(book-5, a-4), dobj(give-2, book-5), det(painter-9, the-7), amod(painter-9, famous-8), prep_about(book-5, painter-9), sumo(Character, he-1), sumo(Process, give-2), sumo(Artist, painter-9), sumo(Book, book-5), sumo(SubjectiveStrongPositiveAttribute, famous-8), tense(PAST, give-2), number(SINGULAR, book-5), number(SINGULAR, painter-9)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance book-5 Book) (refers book-5 painter-9))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testConversationAbout() {
        // We had a conversation about the trip.
        String input = "root(ROOT-0, have-2), nsubj(have-2, we-1), det(conversation-4, a-3), dobj(have-2, conversation-4), det(trip-7, the-6), prep_about(conversation-4, trip-7), sumo(Speaking, conversation-4), sumo(TripFn, trip-7), tense(PAST, have-2), number(SINGULAR, conversation-4), number(SINGULAR, trip-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance conversation-4 Speaking) (refers conversation-4 trip-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testDebateAbout() {
        // There is an ongoing debate about the issue.
        String input = "root(ROOT-0, be-2), expl(be-2, there-1), det(debate-5, a-3), amod(debate-5, ongoing-4), nsubj(be-2, debate-5), det(issue-8, the-7), prep_about(debate-5, issue-8), sumo(Debating, debate-5), sumo(SubjectiveAssessmentAttribute, there-1), sumo(TimePosition, ongoing-4), sumo(Proposition, issue-8), tense(PRESENT, be-2), number(SINGULAR, debate-5), number(SINGULAR, issue-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance debate-5 Debating) (refers debate-5 issue-8))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testDetailAbout() {
        // He did not give me any details about the requirements.
        String input = "root(ROOT-0, give-4), nsubj(give-4, he-1), aux(give-4, do-2), neg(give-4, not-3), iobj(give-4, I-5), det(detail-7, any-6), dobj(give-4, detail-7), det(requirement-10, the-9), prep_about(detail-7, requirement-10), sumo(Character, he-1), sumo(Fact, detail-7), sumo(IntentionalProcess, do-2), sumo(Attribute, requirement-10), number(PLURAL, detail-7), number(PLURAL, requirement-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance detail-7 Fact) (refers detail-7 requirement-10))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testDiscussionAbout() {
        // The discussion about the best option led to no conclusion.
        String input = "root(ROOT-0, lead-7), det(discussion-2, the-1), nsubj(lead-7, discussion-2), det(option-6, the-4), amod(option-6, best-5), prep_about(discussion-2, option-6), neg(conclusion-10, no-9), prep_to(lead-7, conclusion-10), sumo(Text, discussion-2), sumo(Selecting, option-6), sumo(SubjectiveAssessmentAttribute, best-5), sumo(conclusion, conclusion-10), sumo(Guiding, lead-7), number(SINGULAR, discussion-2), number(SINGULAR, option-6), tense(PAST, lead-7), number(SINGULAR, conclusion-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance discussion-2 Text) (refers discussion-2 option-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testDocumentaryAbout() {
        // The documentary about Egypt was good.
        String input = "root(ROOT-0,good-6), det(documentary-2,the-1), nsubj(good-6,documentary-2), prep_about(documentary-2,Egypt-4), cop(good-6,be-5), sumo(Egypt,Egypt-4), sumo(SubjectiveAssessmentAttribute,good-6), sumo(Documentary,documentary-2), number(SINGULAR,documentary-2), number(SINGULAR,Egypt-4), tense(PAST,be-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance documentary-2 Documentary) (refers documentary-2 Egypt-4))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testFactAbout() {
        // The article outlines facts about malnutrition.
        String input = "root(ROOT-0,outline-3), det(article-2,the-1), nsubj(outline-3,article-2), dobj(outline-3,fact-4), prep_about(fact-4,malnutrition-6), sumo(DiseaseOrSyndrome,malnutrition-6), sumo(Article,article-2), sumo(Fact,fact-4), sumo(Communication,outline-3), number(SINGULAR,article-2), tense(PRESENT,outline-3), number(PLURAL,fact-4), number(SINGULAR,malnutrition-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance fact-4 Fact) (refers fact-4 malnutrition-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testHypothesisAbout() {
        // There are various hypotheses about the phenomenon.
        String input = "root(ROOT-0,be-2), expl(be-2,there-1), amod(hypothesis-4,various-3), nsubj(be-2,hypothesis-4), det(phenomenon-7,the-6), prep_about(hypothesis-4,phenomenon-7), sumo(Stating,hypothesis-4), sumo(SubjectiveAssessmentAttribute,there-1), sumo(SubjectiveAssessmentAttribute,various-3), sumo(Physical,phenomenon-7), tense(PRESENT,be-2), number(PLURAL,hypothesis-4), number(SINGULAR,phenomenon-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance hypothesis-4 Stating) (refers hypothesis-4 phenomenon-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testInstructionAbout() {
        // Instructions about evacuation were not clear.
        String input = "root(ROOT-0,clear-6), nsubj(clear-6,instruction-1), prep_about(instruction-1,evauation-3), cop(clear-6,be-4), neg(clear-6,not-5), sumo(Procedure,instruction-1), sumo(SubjectiveAssessmentAttribute,clear-6), number(PLURAL,instruction-1), number(SINGULAR,evauation-3), tense(PAST,be-4)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance instruction-1 Procedure) (refers instruction-1 evauation-3))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testLanguageAbout() {
        // The painting represents a visual language about the topic.
        String input = "root(ROOT-0,represent-3), det(painting-2,the-1), nsubj(represent-3,painting-2), det(language-6,a-4), amod(language-6,visual-5), dobj(represent-3,language-6), det(topic-9,the-8), prep_about(language-6,topic-9), sumo(Language,language-6), sumo(Seeing,visual-5), sumo(Proposition,topic-9), sumo(Process,represent-3), sumo(Painting,painting-2), number(SINGULAR,painting-2), tense(PRESENT,represent-3), number(SINGULAR,language-6), number(SINGULAR,topic-9)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance language-6 Language) (refers language-6 topic-9))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testMeetingAbout() {
        // The meeting about the election was boring.
        String input = "root(ROOT-0,boring-7), det(meeting-2,the-1), nsubj(boring-7,meeting-2), det(election-5,the-4), prep_about(meeting-2,election-5), cop(boring-7,be-6), sumo(IntentionalPsychologicalProcess,boring-7), sumo(Meeting,meeting-2), sumo(ElectionFn,election-5), number(SINGULAR,meeting-2), number(SINGULAR,election-5), tense(PAST,be-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance meeting-2 Meeting) (refers meeting-2 election-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testMessageAbout() {
        // Email messages about various scams inundate our mailboxes.
        String input = "root(ROOT-0,email-1), nsubj(inundate-6,message-2), amod(scam-5,various-4), prep_about(message-2,scam-5), ccomp(email-1,inundate-6), poss(mailbox-9,we-7), dobj(inundate-6,mailbox-9), sumo(Mailbox,mailbox-9), sumo(Message,message-2), sumo(SubjectiveAssessmentAttribute,various-4), sumo(Device,email-1), sumo(Wetting,inundate-6), number(PLURAL,message-2), number(PLURAL,scam-5), number(PLURAL,mailbox-9)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance message-2 Message) (refers message-2 scam-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testMovieAbout() {
        // The movie about the Civil War was pretty good.
        String input = "root(ROOT-0,good-9), det(movie-2,the-1), nsubj(good-9,movie-2), det(war-6,the-4), amod(war-6,civil-5), prep_about(movie-2,war-6), cop(good-9,be-7), advmod(good-9,pretty-8), sumo(MotionPicture,movie-2), sumo(NormativeAttribute,civil-5), sumo(SubjectiveAssessmentAttribute,pretty-8), sumo(War,war-6), sumo(SubjectiveAssessmentAttribute,good-9), number(SINGULAR,movie-2), number(SINGULAR,war-6), tense(PAST,be-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance movie-2 MotionPicture) (refers movie-2 war-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testMythAbout() {
        // The myth about the war is only growing.
        String input = "root(ROOT-0,grow-8), det(myth-2,the-1), nsubj(grow-8,myth-2), det(war-5,the-4), prep_about(myth-2,war-5), aux(grow-8,be-6), advmod(grow-8,only-7), sumo(War,war-5), sumo(Process,grow-8), sumo(NarrativeText,myth-2), number(SINGULAR,myth-2), number(SINGULAR,war-5), tense(PRESENT,grow-8), aspect(PROGRESSIVE,grow-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance myth-2 NarrativeText) (refers myth-2 war-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testNoteAbout() {
        // He made notes about possible improvements.
        String input = "root(ROOT-0,make-2), nsubj(make-2,he-1), dobj(make-2,note-3), amod(improvement-6,possible-5), prep_about(make-2,improvement-6), sumo(Character,he-1), sumo(Stating,note-3), sumo(SubjectiveAssessmentAttribute,improvement-6), sumo(Possibility,possible-5), tense(PAST,make-2), number(PLURAL,note-3), number(PLURAL,improvement-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance note-3 Stating) (refers note-3 improvement-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testObservationAbout() {
        // His observation about her behavior only led to more confusion.
        String input = "root(ROOT-0,observation-3), nsubj(observation-3,he-1), cop(observation-3,be-2), poss(behavior-6,she-5), prep_about(observation-3,behavior-6), advmod(lead-8,only-7), vmod(behavior-6,lead-8), amod(confusion-11,more-10), prep_to(lead-8,confusion-11), sumo(Character,he-1), sumo(Measuring,observation-3), sumo(SubjectiveAssessmentAttribute,confusion-11), sumo(BodyMotion,behavior-6), sumo(Guiding,lead-8), tense(PRESENT,be-2), number(SINGULAR,observation-3), number(SINGULAR,behavior-6), tense(PAST,lead-8), number(SINGULAR,confusion-11)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance observation-3 Measuring) (refers observation-3 behavior-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testPerceptionAbout() {
        // Different people have different perceptions about the same situation.
        String input = "root(ROOT-0,have-3), amod(people-2,different-1), nsubj(have-3,people-2), amod(perception-5,different-4), dobj(have-3,perception-5), det(situation-9,the-7), amod(situation-9,same-8), prep_about(perception-5,situation-9), sumo(GroupOfPeople,people-2), sumo(Perception,perception-5), sumo(equal,different-1), sumo(equal,different-4), sumo(SubjectiveAssessmentAttribute,situation-9), number(PLURAL,people-2), tense(PRESENT,have-3), number(PLURAL,perception-5), number(SINGULAR,situation-9)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance perception-5 Perception) (refers perception-5 situation-9))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testPredictionAbout() {
        // The ancient prediction about the doomsday is well known.
        String input = "root(ROOT-0,know-9), det(prediction-3,the-1), amod(prediction-3,ancient-2), nsubjpass(know-9,prediction-3), det(doomsday-6,the-5), prep_about(prediction-3,doomsday-6), auxpass(know-9,be-7), advmod(know-9,be-7), sumo(Interpreting,know-9), sumo(SubjectiveAssessmentAttribute,doomsday-6), sumo(Human,ancient-2), sumo(Predicting,prediction-3), number(SINGULAR,prediction-3), number(SINGULAR,doomsday-6), tense(PRESENT,be-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance prediction-3 Predicting) (refers prediction-3 doomsday-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testProgramAbout() {
        // He got his degree from a program about literature.
        String input = "oot(ROOT-0,get-3), det(doomsday-2,the-1), nsubj(get-3,doomsday-2), det(doomsday-5,the-4), poss(degree-7,doomsday-5), dobj(get-3,degree-7), det(program-10,a-9), prep_from(get-3,program-10), prep_about(program-10,literature-12), sumo(Plan,program-10), sumo(SubjectiveAssessmentAttribute,doomsday-2), sumo(Literature,literature-12), sumo(SubjectiveAssessmentAttribute,doomsday-5), sumo(ConstantQuantity,degree-7), sumo(Getting,get-3), number(SINGULAR,doomsday-2), tense(PAST,get-3), number(SINGULAR,doomsday-5), number(SINGULAR,degree-7), number(SINGULAR,program-10), number(SINGULAR,literature-12)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance program-10 Plan) (refers program-10 literature-12))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testQualmsAbout() {
        // He has no qualms about manhunt.
        String input = "root(ROOT-0,have-3), det(doomsday-2,the-1), nsubj(have-3,doomsday-2), neg(qualm-5,no-4), dobj(have-3,qualm-5), prep_about(qualm-5,manhunt-7), sumo(EmotionalState,qualm-5), sumo(SubjectiveAssessmentAttribute,doomsday-2), sumo(Pursuing,manhunt-7), number(SINGULAR,doomsday-2), tense(PRESENT,have-3), number(PLURAL,qualm-5), number(SINGULAR,manhunt-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance qualm-5 EmotionalState) (refers qualm-5 manhunt-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testQueryAbout() {
        // The system can handle queries about general topics.
        String input = "root(ROOT-0,handle-4), det(system-2,the-1), nsubj(handle-4,system-2), aux(handle-4,can-3), dobj(handle-4,query-5), amod(topic-8,general-7), prep_about(handle-4,topic-8), sumo(Attribute,handle-4), sumo(forall,general-7), sumo(Requesting,query-5), sumo(Proposition,topic-8), number(SINGULAR,system-2), number(PLURAL,query-5), number(PLURAL,topic-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance query-5 Requesting) (refers query-5 topic-8))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testQuestionAbout() {
        // Any questions about the procedure should be directed to the doctor.
        String input = "root(ROOT-0,direct-8), det(question-2,any-1), nsubjpass(direct-8,question-2), det(procedure-5,the-4), prep_about(question-2,procedure-5), aux(direct-8,should-6), auxpass(direct-8,be-7), det(doctor-11,the-10), prep_to(direct-8,doctor-11), sumo(Question,question-2), sumo(exists,any-1), sumo(Procedure,procedure-5), sumo(Ordering,direct-8), sumo(MedicalDoctor,doctor-11), number(PLURAL,question-2), number(SINGULAR,procedure-5), number(SINGULAR,doctor-11)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance question-2 Question) (refers question-2 procedure-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testRecommendationAbout() {
        // He gave me recommendations about good restaurants.
        String input = "oot(ROOT-0,give-3), det(doctor-2,the-1), nsubj(give-3,doctor-2), iobj(give-3,I-4), dobj(give-3,recommendation-5), amod(restaurant-8,good-7), prep_about(recommendation-5,restaurant-8), sumo(Restaurant,restaurant-8), sumo(Process,give-3), sumo(Requesting,recommendation-5), sumo(SubjectiveAssessmentAttribute,good-7), sumo(MedicalDoctor,doctor-2), number(SINGULAR,doctor-2), tense(PAST,give-3), number(PLURAL,recommendation-5), number(PLURAL,restaurant-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance recommendation-5 Requesting) (refers recommendation-5 restaurant-8))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testRumorAbout() {
        // Rumors about imminent terrorist attacks abound.
        String input = "root(ROOT-0,abound-6), nsubj(abound-6,rumor-1), amod(attack-5,imminent-3), amod(attack-5,terrorist-4), prep_about(rumor-1,attack-5), sumo(ImmediateFutureFn,imminent-3), sumo(SubjectiveAssessmentAttribute,abound-6), sumo(Attack,attack-5), sumo(Terrorist,terrorist-4), sumo(HistoricalAccount,rumor-1), number(PLURAL,rumor-1), number(PLURAL,attack-5), tense(PRESENT,abound-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance rumor-1 HistoricalAccount) (refers rumor-1 attack-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testScenarioAbout() {
        // A hypothetical scenario about fire in the building is the topic of the exercise.
        String input = "oot(ROOT-0,topic-11), det(scenario-3,a-1), amod(scenario-3,hypothetical-2), nsubj(topic-11,scenario-3), prep_about(scenario-3,fire-5), det(building-8,the-7), prep_in(fire-5,building-8), cop(topic-11,be-9), det(topic-11,the-10), det(exercise-14,the-13), prep_of(topic-11,exercise-14), sumo(RecreationOrExercise,exercise-14), sumo(Angstrom,a-1), sumo(SubjectiveAssessmentAttribute,hypothetical-2), sumo(Making,building-8), sumo(Text,scenario-3), sumo(Proposition,topic-11), number(SINGULAR,scenario-3), number(SINGULAR,fire-5), number(SINGULAR,building-8), tense(PRESENT,be-9), number(SINGULAR,topic-11), number(SINGULAR,exercise-14)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance scenario-3 Text) (refers scenario-3 fire-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testSignalAbout() {
        // We are receiving signals about climate change.
        String input = "root(ROOT-0,receive-3), nsubj(receive-3,we-1), aux(receive-3,be-2), dobj(receive-3,signal-4), prep_about(signal-4,climate-6), sumo(Getting,receive-3), sumo(Icon,signal-4), sumo(QuantityChange,climate-6), tense(PRESENT,receive-3), aspect(PROGRESSIVE,receive-3), number(PLURAL,signal-4), number(SINGULAR,climate-6), number(SINGULAR,change-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance signal-4 Icon) (refers signal-4 climate-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testSnobberyAbout() {
        // Snobbery about modern art is often observed.
        String input = "root(ROOT-0,observe-7), nsubjpass(observe-7,snobbery-1), amod(art-4,modern-3), prep_about(snobbery-1,art-4), auxpass(observe-7,be-5), advmod(observe-7,often-6), sumo(Discovering,observe-7), sumo(TraitAttribute,snobbery-1), sumo(SubjectiveWeakPositiveAttribute,modern-3), sumo(ArtWork,art-4), number(SINGULAR,snobbery-1), number(SINGULAR,art-4), tense(PRESENT,be-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance snobbery-1 TraitAttribute) (refers snobbery-1 art-4))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testStatementAbout() {
        // A statement about the government's position on the issue is long overdue.
        String input = "root(ROOT-0,overdue-13), det(statement-2,a-1), nsubj(overdue-13,statement-2), det(government-5,the-4), poss(position-7,government-5), prep_about(statement-2,position-7), det(issue-10,the-9), prep_on(position-7,issue-10), cop(overdue-13,be-11), advmod(overdue-13,long-12), sumo(Statement,statement-2), sumo(SubjectiveAssessmentAttribute,long-12), sumo(Position,position-7), sumo(Angstrom,a-1), sumo(Proposition,issue-10), sumo(GovernmentFn,government-5), sumo(NormativeAttribute,overdue-13), number(SINGULAR,statement-2), number(SINGULAR,government-5), number(SINGULAR,position-7), number(SINGULAR,issue-10), tense(PRESENT,be-11)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance statement-2 Statement) (refers statement-2 position-7))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testStoryAbout() {
        // Pinocchio is a story about a wooden puppet who wanted to become a boy.
        String input = "oot(ROOT-0,story-4), nsubj(story-4,Pinocchio-1), cop(story-4,be-2), det(story-4,a-3), det(puppet-8,a-6), amod(puppet-8,wodden-7), prep_about(story-4,puppet-8), nsubj(want-10,who-9), rcmod(puppet-8,want-10), aux(become-12,to-11), xcomp(want-10,become-12), det(boy-14,a-13), xcomp(become-12,boy-14), sumo(Artifact,puppet-8), sumo(Boy,boy-14), sumo(PsychologicalAttribute,want-10), sumo(Stating,story-4), number(SINGULAR,Pinocchio-1), tense(PRESENT,be-2), number(SINGULAR,story-4), number(SINGULAR,puppet-8), tense(PAST,want-10), number(SINGULAR,boy-14)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance story-4 Stating) (refers story-4 puppet-8))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testSuggestionAbout() {
        // The author's suggestions about marketing are highly useful.
        String input = "root(ROOT-0,useful-9), det(author-2,the-1), poss(suggestion-4,author-2), nsubj(useful-9,suggestion-4), prep_about(suggestion-4,marketing-6), cop(useful-9,be-7), advmod(useful-9,highly-8), sumo(Selling,marketing-6), sumo(SubjectiveAssessmentAttribute,highly-8), sumo(Requesting,suggestion-4), sumo(Writer,author-2), sumo(SubjectiveWeakPositiveAttribute,useful-9), number(SINGULAR,author-2), number(PLURAL,suggestion-4), number(SINGULAR,marketing-6), tense(PRESENT,be-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance suggestion-4 Requesting) (refers suggestion-4 marketing-6))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testTestimonyAbout() {
        // In his public testimony about the accident, Sullivan gave conflicting accounts.
        String input = "root(ROOT-0,give-12), det(author-3,the-2), poss(testimony-6,author-3), amod(testimony-6,public-5), prep_in(give-12,testimony-6), det(accident-9,the-8), prep_about(testimony-6,accident-9), nsubj(give-12,Suillivan-11), amod(account-14,conflict-13), dobj(give-12,account-14), sumo(Writer,author-3), sumo(Human,Suillivan-11), sumo(SubjectiveAssessmentAttribute,conflict-13), sumo(Testifying,testimony-6), sumo(Process,give-12), sumo(AccountFn,account-14), sumo(SubjectiveAssessmentAttribute,public-5), names(Suillivan-11,\"Suillivan\"), sumo(Process,accident-9), number(SINGULAR,author-3), number(SINGULAR,testimony-6), number(SINGULAR,accident-9), number(SINGULAR,Suillivan-11), tense(PAST,give-12), number(PLURAL,account-14)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance testimony-6 Testifying) (refers testimony-6 accident-9))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testTransparencyAbout() {
        // There is often a lack of transparency about governmental decision-making.
        String input = "root(ROOT-0,be-2), expl(be-2,there-1), advmod(be-2,often-3), det(lack-5,a-4), nsubj(be-2,lack-5), prep_of(lack-5,transparency-7), amod(decision-making-10,governmental-9), prep_about(transparency-7,decision-making-10), sumo(Deciding,decision-making-10), sumo(Attribute,lack-5), sumo(SubjectiveAssessmentAttribute,there-1), sumo(Icon,transparency-7), sumo(Government,governmental-9), tense(PRESENT,be-2), number(SINGULAR,lack-5), number(SINGULAR,transparency-7), number(SINGULAR,decision-making-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance transparency-7 Icon) (refers transparency-7 decision-making-10))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testTrialAbout() {
        // The trial about the murder case became a global sensation.
        String input = "root(ROOT-0,become-7), det(trial-2,the-1), nsubj(become-7,trial-2), det(sensation-10,a-8), amod(sensation-10,global-9), xcomp(become-7,sensation-10), det(murder-5,the-4), prep_about(trial-2,murder-5), sumo(SubjectiveAssessmentAttribute,global-9), sumo(Perception,sensation-10), sumo(Experimenting,trial-2), number(SINGULAR,trial-2), number(SINGULAR,murder-5), number(SINGULAR,case-6), tense(PAST,become-7), number(SINGULAR,sensation-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance trial-2 Experimenting) (refers trial-2 murder-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testViewAbout() {
        // His view about the matter clashes with those of many other senators.
        String input = "root(ROOT-0,view-2), poss(view-2,he-1), prep_with(view-2,those-8), amod(senator-12,many-10), amod(senator-12,other-11), prep_of(those-8,senator-12), det(matter-5,the-4), prep_about(view-2,matter-5), sumo(Position,senator-12), sumo(View,view-2), number(SINGULAR,view-2), number(SINGULAR,matter-5), number(PLURAL,clash-6), number(PLURAL,senator-12)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance view-2 View) (refers view-2 matter-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testWordAbout() {
        // The police chief did not give the reporters any word about the case.
        String input = "oot(ROOT-0,give-6), aux(give-6,do-4), neg(give-6,not-5), det(reporter-8,the-7), iobj(give-6,reporter-8), det(word-10,any-9), dobj(give-6,word-10), det(case-13,the-12), prep_about(word-10,case-13), det(police-2,the-1), nsubj(give-6,police-2), sumo(NewsReporter,reporter-8), sumo(PoliceCaptain,police-2), sumo(Word,word-10), sumo(IntentionalProcess,do-4), sumo(CartridgeCase,case-13), number(SINGULAR,police-2), number(SINGULAR,chief-3), number(PLURAL,reporter-8), number(SINGULAR,word-10), number(SINGULAR,case-13)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance word-10 Word) (refers word-10 case-13))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> cleanedActual = getCleanedOutput(kifClauses);
        assertThat(cleanedActual, hasItems(expected));
    }

    @Test
    public void testWritingAbout() {
        // His writings about medieval legends became popular.
        String input = "root(ROOT-0,become-6), poss(writings-2,he-1), nsubj(become-6,writings-2), amod(legend-5,medieval-4), prep_about(writings-2,legend-5), acomp(become-6,popular-7), sumo(Writing,writings-2), sumo(TimeInterval,medieval-4), sumo(FictionalText,legend-5), sumo(SubjectiveWeakPositiveAttribute,popular-7), number(PLURAL,writings-2), number(PLURAL,legend-5), tense(PAST,become-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (instance writings-2 Writing) (refers writings-2 legend-5))"
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
