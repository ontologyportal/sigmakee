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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

public class RulePrepAboutUnitTest {

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
    }

    @Test
    public void testAnnounceAbout() {
        // John announced about his plan.
        String input = "root(ROOT-0, announce-2), nsubj(announce-2, John-1), poss(plan-6, John-4), prep_about(announce-2, plan-6), names(John-1, \"John\"), sumo(Disseminating, announce-2), attribute(John-4, Male), attribute(John-1, Male), sumo(Human, John-1), sumo(Human, John-4), names(John-4, \"John\"), sumo(Plan, plan-6), number(SINGULAR, John-1), tense(PAST, announce-2), number(SINGULAR, John-4), number(SINGULAR, plan-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance announce-2 Disseminating)",
                "(refers announce-2 plan-6)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testAskAbout() {
        // John asked about my opinion.
        String input = "root(ROOT-0, ask-2), nsubj(ask-2, John-1), poss(opinion-5, my-4), prep_about(ask-2, opinion-5), names(John-1, \"John\"), sumo(Proposition, opinion-5), attribute(John-1, Male), sumo(Human, John-1), sumo(Questioning, ask-2), number(SINGULAR, John-1), tense(PAST, ask-2), number(SINGULAR, opinion-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance ask-2 Questioning)",
                "(refers ask-2 opinion-5)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testBeAbout() {
        // Section Five is about the impact.
        String input = "root(ROOT-0, be-3), nsubj(be-3, Section-1), num(Section-1, five-2), det(impact-6, the-5), prep_about(be-3, impact-6), sumo(Text, Section-1), sumo(Impacting, impact-6), number(SINGULAR, Section-1), tense(PRESENT, be-3), number(SINGULAR, impact-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers Section-1 impact-6)",
                "(instance impact-6 Impacting)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testBoastAbout() {
        // Toyota officials boasted about the effectiveness.
        String input = "root(ROOT-0, boast-3), det(effectiveness-6, the-5), prep_about(boast-3, effectiveness-6), nsubj(boast-3, Toyotaofficials-1), sumo(SubjectiveAssessmentAttribute, effectiveness-6), sumo(Communication, boast-3), number(SINGULAR, Toyota-1), number(PLURAL, official-2), tense(PAST, boast-3), number(SINGULAR, effectiveness-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers boast-3 effectiveness-6)",
                "(instance boast-3 Communication)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testChatAbout() {
        // He chatted about small things.
        String input = "root(ROOT-0, chat-2), nsubj(chat-2, he-1), amod(thing-5, small-4), prep_about(chat-2, thing-5), sumo(Character, he-1), sumo(SubjectiveStrongNegativeAttribute, small-4), sumo(SubjectiveAssessmentAttribute, thing-5), sumo(Communication, chat-2), tense(PAST, chat-2), number(PLURAL, thing-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers chat-2 thing-5)",
                "(instance chat-2 Communication)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testComplainAbout() {
        // They seldom complain about unsafe work environments.
        String input = "root(ROOT-0, selcom-2), nsubj(selcom-2, they-1), xcomp(selcom-2, complain-3), amod(work-6, unsafe-5), prep_about(complain-3, work-6), sumo(SubjectiveWeakNegativeAttribute, unsafe-5), sumo(Expressing, complain-3), tense(PRESENT, selcom-2), number(SINGULAR, work-6), number(PLURAL, environment-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers complain-3 work-6)",
                "(instance complain-3 Expressing)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testDisagreeAbout() {
        // Stakeholders disagreed about climate change.
        String input = "root(ROOT-0, disagree-2), nsubj(disagree-2, stakeholder-1), prep_about(disagree-2, climate-4), sumo(Communication, disagree-2), sumo(QuantityChange, climate-4), number(PLURAL, stakeholder-1), tense(PAST, disagree-2), number(SINGULAR, climate-4), number(SINGULAR, change-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance disagree-2 Communication)",
                "(refers disagree-2 climate-4)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testExplainAbout() {
        // He explained about the reason.
        String input = "root(ROOT-0, explain-2), nsubj(explain-2, he-1), det(reason-5, the-4), prep_about(explain-2, reason-5), sumo(Character, he-1), sumo(Proposition, reason-5), sumo(ContentDevelopment, explain-2), tense(PAST, explain-2), number(SINGULAR, reason-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers explain-2 reason-5)",
                "(instance explain-2 ContentDevelopment)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testFeelAbout() {
        // How do you feel about it?
        String input = "root(ROOT-0, feel-4), advmod(feel-4, how-1), aux(feel-4, do-2), nsubj(feel-4, you-3), prep_about(feel-4, it-6), sumo(EmotionalState, feel-4)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance feel-4 EmotionalState)",
                "(refers feel-4 it-6)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testForgetAbout() {
        // She forgot about the appointment.
        String input = "root(ROOT-0, forget-2), nsubj(forget-2, she-1), det(appointment-5, the-4), prep_about(forget-2, appointment-5), sumo(Stating, appointment-5), sumo(Remembering, forget-2), tense(PAST, forget-2), number(SINGULAR, appointment-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers forget-2 appointment-5)",
                "(instance forget-2 Remembering)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testGeneralizeAbout() {
        // We tend to generalize about varied viewpoints.
        String input = "root(ROOT-0, tend-2), nsubj(tend-2, we-1), aux(generalize-4, to-3), xcomp(tend-2, generalize-4), amod(viewpoint-7, varied-6), prep_about(generalize-4, viewpoint-7), sumo(Process, varied-6), sumo(TraitAttribute, tend-2), sumo(SubjectiveAssessmentAttribute, viewpoint-7), sumo(IntentionalPsychologicalProcess, generalize-4), tense(PRESENT, tend-2), number(PLURAL, viewpoint-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers generalize-4 viewpoint-7)",
                "(instance generalize-4 IntentionalPsychologicalProcess)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testGossipAbout() {
        // It was so we could gossip about everyone who was not there.
        String input = "root(ROOT-0, so-3), nsubj(so-3, it-1), cop(so-3, be-2), nsubj(gossip-6, we-4), aux(gossip-6, could-5), ccomp(so-3, gossip-6), prep_about(gossip-6, everyone-8), nsubj(be-10, who-9), rcmod(everyone-8, be-10), neg(be-10, not-11), advmod(be-10, there-12), sumo(FieldOfStudy, it-1), sumo(LinguisticCommunication, gossip-6), tense(PAST, be-2), number(SINGULAR, gossip-6), number(SINGULAR, everyone-8), tense(PAST, be-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers gossip-6 everyone-8)",
                "(instance gossip-6 LinguisticCommunication)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testHearAbout() {
        // We hear about the issue all the time.
        String input = "root(ROOT-0, hear-2), nsubj(hear-2, we-1), det(issue-5, the-4), prep_about(hear-2, issue-5), dobj(hear-2, all-6), sumo(Proposition, issue-5), sumo(SubjectiveAssessmentAttribute, all-6), sumo(Hearing, hear-2), tense(PRESENT, hear-2), number(SINGULAR, issue-5), number(SINGULAR, time-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers hear-2 issue-5)",
                "(instance hear-2 Hearing)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testInquireAbout() {
        // Many people are inquiring about the new policy.
        String input = "root(ROOT-0, inquire-4), amod(people-2, many-1), nsubj(inquire-4, people-2), aux(inquire-4, be-3), det(policy-8, the-6), amod(policy-8, new-7), prep_about(inquire-4, policy-8), sumo(GroupOfPeople, people-2), sumo(Policy, policy-8), sumo(SubjectiveAssessmentAttribute, new-7), sumo(Questioning, inquire-4), sumo(SubjectiveAssessmentAttribute, many-1), number(PLURAL, people-2), tense(PRESENT, inquire-4), aspect(PROGRESSIVE, inquire-4), number(SINGULAR, policy-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers inquire-4 policy-8)",
                "(instance inquire-4 Questioning)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testLaughAbout() {
        // People are laughing about the incident.
        String input = "root(ROOT-0, laugh-3), nsubj(laugh-3, people-1), aux(laugh-3, be-2), det(incident-6, the-5), prep_about(laugh-3, incident-6), sumo(GroupOfPeople, people-1), sumo(Process, incident-6), sumo(Laughing, laugh-3), number(PLURAL, people-1), tense(PRESENT, laugh-3), aspect(PROGRESSIVE, laugh-3), number(SINGULAR, incident-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers laugh-3 incident-6)",
                "(instance laugh-3 Laughing)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testMutterAbout() {
        // He muttered about al-Qaeda.
        String input = "root(ROOT-0, mutter-2), nsubj(mutter-2, he-1), prep_about(mutter-2, al-Qaeda-4), sumo(Character, he-1), sumo(Alabama, al-Qaeda-4), sumo(Speaking, mutter-2), tense(PAST, mutter-2), number(SINGULAR, al-Qaeda-4)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers mutter-2 al-Qaeda-4)",
                "(instance mutter-2 Speaking)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testProclaimAbout() {
        // The Messiah proclaimed about a new world.
        String input = "root(ROOT-0, proclaim-3), det(Messiah-2, the-1), nsubj(proclaim-3, Messiah-2), det(world-7, a-5), amod(world-7, new-6), prep_about(proclaim-3, world-7), sumo(Declaring, proclaim-3), sumo(CognitiveAgent, Messiah-2), sumo(Object, world-7), sumo(SubjectiveAssessmentAttribute, new-6), number(SINGULAR, Messiah-2), tense(PAST, proclaim-3), number(SINGULAR, world-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance proclaim-3 Declaring)",
                "(refers proclaim-3 world-7)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testReadAbout() {
        // He read about the accident in the newspaper.
        String input = "root(ROOT-0, read-2), nsubj(read-2, he-1), det(accident-5, the-4), prep_about(read-2, accident-5), det(newspaper-8, the-7), prep_in(accident-5, newspaper-8), sumo(Character, he-1), sumo(Process, accident-5), sumo(Newspaper, newspaper-8), sumo(Reading, read-2), tense(PAST, read-2), number(SINGULAR, accident-5), number(SINGULAR, newspaper-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers read-2 accident-5)",
                "(instance read-2 Reading)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testRememberAbout() {
        // He did not remember anything about the accident.
        String input = "oot(ROOT-0, remember-4), nsubj(remember-4, he-1), aux(remember-4, do-2), neg(remember-4, not-3), dobj(remember-4, anything-5), det(accident-8, the-7), prep_about(remember-4, accident-8), sumo(Character, he-1), sumo(IntentionalProcess, do-2), sumo(Process, accident-8), sumo(Remembering, remember-4), number(SINGULAR, anything-5), number(SINGULAR, accident-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers remember-4 accident-8)",
                "(instance remember-4 Remembering)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testReportAbout() {
        // The journalist reported about the war.
        String input = "root(ROOT-0, report-3), det(journalist-2, the-1), nsubj(report-3, journalist-2), det(war-6, the-5), prep_about(report-3, war-6), sumo(Journalist, journalist-2), sumo(War, war-6), sumo(Stating, report-3), number(SINGULAR, journalist-2), tense(PAST, report-3), number(SINGULAR, war-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers report-3 war-6)",
                "(instance report-3 Stating)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testSmileAbout() {
        // Proctor smiled about the encounter.
        String input = "root(ROOT-0, smile-2), nsubj(smile-2, Proctor-1), det(encounter-5, the-4), prep_about(smile-2, encounter-5), sumo(Human, Proctor-1), sumo(Smiling, smile-2), sumo(Meeting, encounter-5), names(Proctor-1, \"Proctor\"), number(SINGULAR, Proctor-1), tense(PAST, smile-2), number(SINGULAR, encounter-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers smile-2 encounter-5)",
                "(instance smile-2 Smiling)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testSpeakAbout() {
        // They often spoke about the trip.
        String input = "root(ROOT-0, speak-3), nsubj(speak-3, they-1), advmod(speak-3, often-2), det(trip-6, the-5), prep_about(speak-3, trip-6), sumo(Speaking, speak-3), sumo(Translocation, trip-6), tense(PAST, speak-3), number(SINGULAR, trip-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance speak-3 Speaking)",
                "(refers speak-3 trip-6)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testSpeakUpAbout() {
        // They should speak up about the problem.
        String input = "det(problem-7, the-6), root(ROOT-0, speak-3), nsubj(speak-3, they-1), aux(speak-3, should-2), prep_about(speak-3, problem-7), sumo(SubjectiveAssessmentAttribute, problem-7), sumo(Communication, speak-3), number(SINGULAR, problem-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers speak-3 problem-7)",
                "(instance speak-3 Communication)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testSpeculateAbout() {
        // Policy-makers have speculated about the cause.
        String input = "root(ROOT-0, speculate-3), nsubj(speculate-3, policy-maker-1), aux(speculate-3, have-2), det(cause-6, the-5), prep_about(speculate-3, cause-6), sumo(IntentionalPsychologicalProcess, speculate-3), sumo(Policy, policy-maker-1), sumo(Process, cause-6), number(PLURAL, policy-maker-1), tense(PRESENT, speculate-3), aspect(PERFECT, speculate-3), number(SINGULAR, cause-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance speculate-3 IntentionalPsychologicalProcess)",
                "(refers speculate-3 cause-6)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testStudyAbout() {
        // They study about biology.
        String input = "root(ROOT-0, study-2), nsubj(study-2, they-1), prep_about(study-2, biology-4), sumo(Investigating, study-2), sumo(Biology, biology-4), tense(PRESENT, study-2), number(SINGULAR, biology-4)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance study-2 Investigating)",
                "(refers study-2 biology-4)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testTalkAbout() {
        // Doctors talk about the epidemic.
        String input = "det(epidemic-5, the-4), root(ROOT-0, talk-2), nsubj(talk-2, doctor-1), prep_about(talk-2, epidemic-5), sumo(DiseaseOrSyndrome, epidemic-5), sumo(MedicalDoctor, doctor-1), sumo(ExpressingInLanguage, talk-2), number(PLURAL, doctor-1), tense(PRESENT, talk-2)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers talk-2 epidemic-5)",
                "(instance talk-2 ExpressingInLanguage)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testTeachAbout() {
        // The course teaches about bioinformatics.
        String input = "root(ROOT-0, teach-3), det(course-2, the-1), nsubj(teach-3, course-2), prep_about(teach-3, bioinformatic-5), sumo(EducationalProcess, course-2), sumo(EducationalProcess, teach-3), number(SINGULAR, course-2), tense(PRESENT, teach-3), number(PLURAL, bioinformatic-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance course-2 EducationalProcess)",
                "(refers teach-3 bioinformatic-5)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testUnderstandAbout() {
        // You should understand about the principles.
        String input = "root(ROOT-0, understand-3), nsubj(understand-3, you-1), aux(understand-3, should-2), det(principle-6, the-5), prep_about(understand-3, principle-6), sumo(Interpreting, understand-3), sumo(Proposition, principle-6), number(PLURAL, principle-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance understand-3 Interpreting)",
                "(refers understand-3 principle-6)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testYellAbout() {
        // The girls yelled about the accident.
        String input = "root(ROOT-0, yell-3), det(girl-2, the-1), nsubj(yell-3, girl-2), det(accident-6, the-5), prep_about(yell-3, accident-6), sumo(Female, girl-2), sumo(Process, accident-6), sumo(Vocalizing, yell-3), number(PLURAL, girl-2), tense(PAST, yell-3), number(SINGULAR, accident-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers yell-3 accident-6)",
                "(instance yell-3 Vocalizing)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testAdviseXAbout() {
        // The doctor advised the patient about the treatment.
        String input = "root(ROOT-0, advise-3), det(doctor-2, the-1), nsubj(advise-3, doctor-2), det(patient-5, the-4), dobj(advise-3, patient-5), det(treatment-8, the-7), prep_about(advise-3, treatment-8), sumo(Human, patient-5), sumo(Directing, advise-3), sumo(TherapeuticProcess, treatment-8), sumo(MedicalDoctor, doctor-2), number(SINGULAR, doctor-2), tense(PAST, advise-3), number(SINGULAR, patient-5), number(SINGULAR, treatment-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers advise-3 treatment-8)",
                "(instance advise-3 Directing)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testExciteXAbout() {
        // Great teachers excite students about learning.
        String input = "root(ROOT-0, excite-3), amod(student-2, great-1), nsubj(excite-3, student-2), dobj(excite-3, student-4), prepc_about(excite-3, learn-6), sumo(student, student-2), sumo(student, student-4), sumo(SubjectiveStrongPositiveAttribute, great-1), sumo(Learning, learn-6), sumo(IntentionalPsychologicalProcess, excite-3), number(PLURAL, student-2), tense(PRESENT, excite-3), number(PLURAL, student-4)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers excite-3 learn-6)",
                "(instance excite-3 IntentionalPsychologicalProcess)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testHearAnythingAbout() {
        // I did not hear anything about this before.
        String input = "root(ROOT-0, hear-4), nsubj(hear-4, I-1), aux(hear-4, do-2), neg(hear-4, not-3), dobj(hear-4, anything-5), prep_about(hear-4, this-7), sumo(Hearing, hear-4), sumo(IntentionalProcess, do-2), sumo(AlphabeticCharacter, I-1), number(SINGULAR, anything-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance hear-4 Hearing)",
                "(refers hear-4 this-7)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testKnowAnythingAbout() {
        // He did not know anything about the therapy.
        String input = "root(ROOT-0, know-4), nsubj(know-4, he-1), aux(know-4, do-2), neg(know-4, not-3), det(therapy-7, the-6), prep_about(know-4, therapy-7), sumo(Character, he-1), sumo(TherapeuticProcess, therapy-7), sumo(Interpreting, know-4), sumo(IntentionalProcess, do-2), number(SINGULAR, therapy-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers know-4 therapy-7)",
                "(instance know-4 Interpreting)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testRememberSomethingAbout() {
        // He remembered something about that family.
        String input = "root(ROOT-0, remember-2), nsubj(remember-2, he-1), dobj(remember-2, something-3), det(family-6, that-5), prep_about(remember-2, family-6), sumo(Character, he-1), sumo(FamilyGroup, family-6), sumo(Remembering, remember-2), tense(PAST, remember-2), number(SINGULAR, something-3), number(SINGULAR, family-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers remember-2 family-6)",
                "(instance remember-2 Remembering)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testAdviceAbout() {
        // He gave me an advice about exercise.
        String input = "oot(ROOT-0, give-2), nsubj(give-2, he-1), iobj(give-2, I-3), det(advice-5, a-4), dobj(give-2, advice-5), prep_about(advice-5, exercise-7), sumo(Character, he-1), sumo(Requesting, advice-5), sumo(Process, give-2), sumo(RecreationOrExercise, exercise-7), tense(PAST, give-2), number(SINGULAR, advice-5), number(SINGULAR, exercise-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers advice-5 exercise-7)",
                "(instance advice-5 Requesting)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testAgreementAbout() {
        // We have an agreement about the policy.
        String input = "root(ROOT-0, have-2), nsubj(have-2, we-1), det(agreement-4, a-3), dobj(have-2, agreement-4), det(policy-7, the-6), prep_about(agreement-4, policy-7), sumo(Policy, policy-7), sumo(Agreement, agreement-4), tense(PRESENT, have-2), number(SINGULAR, agreement-4), number(SINGULAR, policy-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers agreement-4 policy-7)",
                "(instance agreement-4 Agreement)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testAlarmAbout() {
        // There was an alarm about the danger.
        String input = "root(ROOT-0, be-2), expl(be-2, there-1), det(alarm-4, a-3), nsubj(be-2, alarm-4), det(danger-7, the-6), prep_about(alarm-4, danger-7), sumo(EmotionalState, alarm-4), sumo(SubjectiveAssessmentAttribute, danger-7), sumo(SubjectiveAssessmentAttribute, there-1), tense(PAST, be-2), number(SINGULAR, alarm-4), number(SINGULAR, danger-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance alarm-4 EmotionalState)",
                "(refers alarm-4 danger-7)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testAnxietyAbout() {
        // I have an anxiety about height.
        String input = "root(ROOT-0, have-2), nsubj(have-2, I-1), det(anxiety-4, a-3), dobj(have-2, anxiety-4), prep_about(anxiety-4, height-6), sumo(EmotionalState, anxiety-4), sumo(LengthMeasure, height-6), tense(PRESENT, have-2), number(SINGULAR, anxiety-4), number(SINGULAR, height-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance anxiety-4 EmotionalState)",
                "(refers anxiety-4 height-6)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testArgumentAbout() {
        // The couple had arguments about insurance.
        String input = "root(ROOT-0, have-3), det(couple-2, the-1), nsubj(have-3, couple-2), dobj(have-3, argument-4), prep_about(argument-4, insurance-6), sumo(ContentBearingObject, argument-4), sumo(Obligation, insurance-6), sumo(GroupOfPeople, couple-2), number(SINGULAR, couple-2), tense(PAST, have-3), number(PLURAL, argument-4), number(SINGULAR, insurance-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance argument-4 ContentBearingObject)",
                "(refers argument-4 insurance-6)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testArticleAbout() {
        // He gave me an article about the experiment.
        String input = "root(ROOT-0, give-3), det(couple-2, the-1), nsubj(give-3, couple-2), iobj(give-3, I-4), det(article-6, a-5), dobj(give-3, article-6), det(experiment-9, the-8), prep_about(article-6, experiment-9), sumo(Process, give-3), sumo(Report, article-6), sumo(Experimenting, experiment-9), sumo(GroupOfPeople, couple-2), number(SINGULAR, couple-2), tense(PAST, give-3), number(SINGULAR, article-6), number(SINGULAR, experiment-9)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers article-6 experiment-9)",
                "(instance article-6 Report)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testAssumptionAbout() {
        // The nurses have assumptions about patient care.
        String input = "root(ROOT-0, have-3), det(nurse-2, the-1), nsubj(have-3, nurse-2), dobj(have-3, assumption-4), prep_about(assumption-4, patient-6), sumo(Supposition, assumption-4), sumo(TherapeuticProcess, nurse-2), number(PLURAL, nurse-2), tense(PRESENT, have-3), number(PLURAL, assumption-4), number(SINGULAR, patient-6), number(SINGULAR, care-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance assumption-4 Supposition)",
                "(refers assumption-4 patient-6)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testBeliefAbout() {
        // Different people have different beliefs about things.
        String input = "root(ROOT-0, have-3), amod(people-2, different-1), nsubj(have-3, people-2), amod(belief-5, different-4), dobj(have-3, belief-5), prep_about(belief-5, thing-7), sumo(GroupOfPeople, people-2), sumo(Proposition, belief-5), sumo(SubjectiveAssessmentAttribute, thing-7), sumo(equal, different-1), sumo(equal, different-4), number(PLURAL, people-2), tense(PRESENT, have-3), number(PLURAL, belief-5), number(PLURAL, thing-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers belief-5 thing-7)",
                "(instance belief-5 Proposition)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testBookAbout() {
        // He gave me a book about the famous painter.
        String input = "root(ROOT-0, give-2), nsubj(give-2, he-1), iobj(give-2, I-3), det(book-5, a-4), dobj(give-2, book-5), det(painter-9, the-7), amod(painter-9, famous-8), prep_about(book-5, painter-9), sumo(Character, he-1), sumo(Process, give-2), sumo(Artist, painter-9), sumo(Book, book-5), sumo(SubjectiveStrongPositiveAttribute, famous-8), tense(PAST, give-2), number(SINGULAR, book-5), number(SINGULAR, painter-9)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers book-5 painter-9)",
                "(instance book-5 Book)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testTheChatAbout() {
        // The chat about the company led to a discussion.
        String input = "root(ROOT-0, lead-6), det(chat-2, the-1), nsubj(lead-6, chat-2), det(company-5, the-4), prep_about(chat-2, company-5), det(discussion-9, a-8), prep_to(lead-6, discussion-9), sumo(Communication, chat-2), sumo(Guiding, lead-6), sumo(Corporation, company-5), sumo(Text, discussion-9), number(SINGULAR, chat-2), number(SINGULAR, company-5), tense(PAST, lead-6), number(SINGULAR, discussion-9)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance chat-2 Communication)",
                "(refers chat-2 company-5)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testChoiceAbout() {
        // Students have choices about their majors.
        String input = "root(ROOT-0, have-2), nsubj(have-2, student-1), dobj(have-2, choice-3), poss(major-7, student-5), prep_about(choice-3, major-7), sumo(student, student-1), sumo(student, student-5), sumo(Learning, major-7), sumo(Selecting, choice-3), number(PLURAL, student-1), tense(PRESENT, have-2), number(PLURAL, choice-3), number(PLURAL, student-5), number(PLURAL, major-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers choice-3 major-7)",
                "(instance choice-3 Selecting)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testCommunicationAbout() {
        // Communication about the operation got interrupted.
        String input = "root(ROOT-0, interrupted-6), nsubj(interrupted-6, communication-1), det(operation-4, the-3), prep_about(communication-1, operation-4), dep(interrupted-6, get-5), sumo(Communication, communication-1), sumo(Obligation, operation-4), sumo(SubjectiveAssessmentAttribute, interrupted-6), sumo(Getting, get-5), number(SINGULAR, communication-1), number(SINGULAR, operation-4), tense(PAST, get-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance communication-1 Communication)",
                "(refers communication-1 operation-4)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testConclusionAbout() {
        // This study draws conclusions about human behavior.
        String input = "root(ROOT-0, draw-3), det(study-2, this-1), nsubj(draw-3, study-2), dobj(draw-3, conclusion-4), amod(behavior-7, human-6), prep_about(conclusion-4, behavior-7), sumo(Learning, conclusion-4), sumo(BodyMotion, behavior-7), sumo(Investigating, study-2), sumo(Human, human-6), sumo(Pulling, draw-3), number(SINGULAR, study-2), tense(PRESENT, draw-3), number(PLURAL, conclusion-4), number(SINGULAR, behavior-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance conclusion-4 Learning)",
                "(refers conclusion-4 behavior-7)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testConsensusAbout() {
        // The committee has consensus about funding.
        String input = "root(ROOT-0, have-3), det(committee-2, the-1), nsubj(have-3, committee-2), dobj(have-3, consensus-4), prep_about(consensus-4, funding-6), sumo(FinancialTransaction, funding-6), sumo(Commission, committee-2), sumo(Cooperation, consensus-4), number(SINGULAR, committee-2), tense(PRESENT, have-3), number(SINGULAR, consensus-4), number(SINGULAR, funding-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers consensus-4 funding-6)",
                "(instance consensus-4 Cooperation)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testConspiracyAbout() {
        // He did not participate in the conspiracy about royal succession.
        String input = "root(ROOT-0, participate-4), nsubj(participate-4, he-1), aux(participate-4, do-2), neg(participate-4, not-3), det(conspiracy-7, the-6), prep_in(participate-4, conspiracy-7), amod(succession-10, royal-9), prep_about(conspiracy-7, succession-10), sumo(Character, he-1), sumo(Promise, conspiracy-7), sumo(SocialInteraction, participate-4), sumo(IntentionalProcess, do-2), sumo(Position, royal-9), sumo(TemporalRelation, succession-10), number(SINGULAR, conspiracy-7), number(SINGULAR, succession-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers conspiracy-7 succession-10)",
                "(instance conspiracy-7 Promise)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testConversationAbout() {
        // They had a conversation about the trip.
        String input = "root(ROOT-0, have-2), nsubj(have-2, they-1), det(conversation-4, a-3), dobj(have-2, conversation-4), det(trip-7, the-6), prep_about(conversation-4, trip-7), sumo(Speaking, conversation-4), sumo(Translocation, trip-7), tense(PAST, have-2), number(SINGULAR, conversation-4), number(SINGULAR, trip-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance conversation-4 Speaking)",
                "(refers conversation-4 trip-7)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testCounselingAbout() {
        // They received counseling about their marriage.
        String input = "root(ROOT-0, receive-3), det(couple-2, the-1), nsubj(receive-3, couple-2), dobj(receive-3, counseling-4), poss(marriage-8, they-6), prep_about(receive-3, marriage-8), sumo(Directing, counseling-4), sumo(MarriageContract, marriage-8), sumo(Getting, receive-3), sumo(GroupOfPeople, couple-2), number(SINGULAR, couple-2), tense(PAST, receive-3), number(SINGULAR, counseling-4), number(SINGULAR, marriage-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance counseling-4 Directing)",
                "(refers counseling-4 marriage-8)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testDebateAbout() {
        // There is an ongoing debate about the issue.
        String input = "root(ROOT-0, be-2), expl(be-2, there-1), det(debate-5, a-3), amod(debate-5, ongoing-4), nsubj(be-2, debate-5), det(issue-8, the-7), prep_about(debate-5, issue-8), sumo(Debating, debate-5), sumo(SubjectiveAssessmentAttribute, there-1), sumo(TimePosition, ongoing-4), sumo(Proposition, issue-8), tense(PRESENT, be-2), number(SINGULAR, debate-5), number(SINGULAR, issue-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance debate-5 Debating)",
                "(refers debate-5 issue-8)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testDecisionAbout() {
        // The decision about the matter is up to the committee.
        String input = "root(ROOT-0, be-6), det(decision-2, the-1), nsubj(be-6, decision-2), det(matter-5, the-4), prep_about(decision-2, matter-5), det(committee-10, the-9), advmod(be-6, up-7), prep_to(up-7, committee-10), sumo(StateOfMind, up-7), sumo(Deciding, decision-2), sumo(Commission, committee-10), sumo(Proposition, matter-5), number(SINGULAR, decision-2), number(SINGULAR, matter-5), tense(PRESENT, be-6), number(SINGULAR, committee-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers decision-2 matter-5)",
                "(instance decision-2 Deciding)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testDetailAbout() {
        // He did not give me any details about the requirements.
        String input = "root(ROOT-0, give-4), nsubj(give-4, he-1), aux(give-4, do-2), neg(give-4, not-3), iobj(give-4, I-5), det(detail-7, any-6), dobj(give-4, detail-7), det(requirement-10, the-9), prep_about(detail-7, requirement-10), sumo(Character, he-1), sumo(Fact, detail-7), sumo(IntentionalProcess, do-2), sumo(Attribute, requirement-10), number(PLURAL, detail-7), number(PLURAL, requirement-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers detail-7 requirement-10)",
                "(instance detail-7 Fact)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testDisagreementAbout() {
        // They had a disagreement about their plan.
        String input = "root(ROOT-0, have-2), nsubj(have-2, they-1), det(disagreement-4, a-3), dobj(have-2, disagreement-4), poss(plan-8, they-6), prep_about(disagreement-4, plan-8), sumo(Plan, plan-8), sumo(SubjectiveAssessmentAttribute, disagreement-4), tense(PAST, have-2), number(SINGULAR, disagreement-4), number(SINGULAR, plan-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers disagreement-4 plan-8)",
                "(instance disagreement-4 SubjectiveAssessmentAttribute)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testDiscoveryAbout() {
        // The discovery about ancient ruins made a headline.
        String input = "root(ROOT-0, make-6), det(discovery-2, the-1), nsubj(make-6, discovery-2), amod(ruin-5, ancient-4), prep_about(discovery-2, ruin-5), det(headline-8, a-7), dobj(make-6, headline-8), sumo(Destruction, ruin-5), sumo(Human, ancient-4), sumo(Learning, discovery-2), sumo(Text, headline-8), number(SINGULAR, discovery-2), number(PLURAL, ruin-5), tense(PAST, make-6), number(SINGULAR, headline-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers discovery-2 ruin-5)",
                "(instance discovery-2 Learning)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testDiscussionAbout() {
        // The discussion about the best option led to no conclusion.
        String input = "root(ROOT-0, lead-7), det(discussion-2, the-1), nsubj(lead-7, discussion-2), det(option-6, the-4), amod(option-6, best-5), prep_about(discussion-2, option-6), neg(conclusion-10, no-9), prep_to(lead-7, conclusion-10), sumo(Text, discussion-2), sumo(Learning, conclusion-10), sumo(Selecting, option-6), sumo(SubjectiveAssessmentAttribute, best-5), sumo(Guiding, lead-7), number(SINGULAR, discussion-2), number(SINGULAR, option-6), tense(PAST, lead-7), number(SINGULAR, conclusion-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance discussion-2 Text)",
                "(refers discussion-2 option-6)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testDocumentaryAbout() {
        // I enjoyed the documentary about Egypt.
        String input = "root(ROOT-0, enjoy-2), nsubj(enjoy-2, I-1), det(documentary-4, the-3), dobj(enjoy-2, documentary-4), prep_about(enjoy-2, Egypt-6), sumo(Egypt, Egypt-6), sumo(Documentary, documentary-4), sumo(IntentionalPsychologicalProcess, enjoy-2), tense(PAST, enjoy-2), number(SINGULAR, documentary-4), number(SINGULAR, Egypt-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers enjoy-2 Egypt-6)",
                "(instance documentary-4 Documentary)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testDoubtAbout() {
        // There is a growing doubt about the cause of the accident.
        String input = "root(ROOT-0, be-2), expl(be-2, there-1), det(doubt-5, a-3), amod(doubt-5, grow-4), nsubj(be-2, doubt-5), det(cause-8, the-7), prep_about(doubt-5, cause-8), det(accident-11, the-10), prep_of(cause-8, accident-11), sumo(Process, grow-4), sumo(Process, cause-8), sumo(PsychologicalAttribute, doubt-5), sumo(SubjectiveAssessmentAttribute, there-1), sumo(Process, accident-11), tense(PRESENT, be-2), number(SINGULAR, doubt-5), number(SINGULAR, cause-8), number(SINGULAR, accident-11)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers doubt-5 cause-8)",
                "(instance doubt-5 PsychologicalAttribute)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testEducationAbout() {
        // Education about healthcare is long overdue.
        String input = "root(ROOT-0, overdue-6), nsubj(overdue-6, Education-1), prep_about(Education-1, healthcare-3), cop(overdue-6, be-4), advmod(overdue-6, long-5), sumo(SubjectiveAssessmentAttribute, long-5), sumo(EducationalProcess, Education-1), sumo(NormativeAttribute, overdue-6), sumo(Maintaining, healthcare-3), number(SINGULAR, Education-1), number(SINGULAR, healthcare-3), tense(PRESENT, be-4)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers Education-1 healthcare-3)",
                "(instance Education-1 EducationalProcess)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testEncouragementAbout() {
        // He gave me encouragement about my career.
        String input = "root(ROOT-0, give-2), nsubj(give-2, he-1), iobj(give-2, I-3), dobj(give-2, encouragement-4), poss(career-8, I-6), prep_about(give-2, career-8), sumo(Character, he-1), sumo(Process, give-2), sumo(SkilledOccupation, career-8), sumo(ExpressingApproval, encouragement-4), tense(PAST, give-2), number(SINGULAR, encouragement-4), number(SINGULAR, career-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers encouragement-4 career-8)",
                "(instance encouragement-4 ExpressingApproval)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testEvidenceAbout() {
        // There is sufficient evidence about his guilt.
        String input = "root(ROOT-0, be-2), expl(be-2, there-1), amod(evidence-4, sufficient-3), nsubj(be-2, evidence-4), det(police-7, the-6), poss(guilt-9, police-7), prep_about(evidence-4, guilt-9), sumo(PoliceOrganization, police-7), sumo(SubjectiveWeakPositiveAttribute, sufficient-3), sumo(Reasoning, evidence-4), sumo(SubjectiveAssessmentAttribute, there-1), sumo(SubjectiveAssessmentAttribute, guilt-9), tense(PRESENT, be-2), number(SINGULAR, evidence-4), number(SINGULAR, police-7), number(SINGULAR, guilt-9)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers evidence-4 guilt-9)",
                "(instance evidence-4 Reasoning)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testFactAbout() {
        // The article outlines facts about malnutrition.
        String input = "root(ROOT-0, outline-3), det(article-2, the-1), nsubj(outline-3, article-2), dobj(outline-3, fact-4), prep_about(fact-4, malnutrition-6), sumo(DiseaseOrSyndrome, malnutrition-6), sumo(Report, article-2), sumo(Fact, fact-4), sumo(Communication, outline-3), number(SINGULAR, article-2), tense(PRESENT, outline-3), number(PLURAL, fact-4), number(SINGULAR, malnutrition-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers fact-4 malnutrition-6)",
                "(instance article-2 Report)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testHypothesisAbout() {
        // There are various hypotheses about the phenomenon.
        String input = "root(ROOT-0, be-2), expl(be-2, there-1), amod(hypothesis-4, various-3), nsubj(be-2, hypothesis-4), det(phenomenon-7, the-6), prep_about(hypothesis-4, phenomenon-7), sumo(Stating, hypothesis-4), sumo(SubjectiveAssessmentAttribute, there-1), sumo(SubjectiveAssessmentAttribute, various-3), sumo(Physical, phenomenon-7), tense(PRESENT, be-2), number(PLURAL, hypothesis-4), number(SINGULAR, phenomenon-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance hypothesis-4 Stating)",
                "(refers hypothesis-4 phenomenon-7)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testIdeaAbout() {
        // We had no idea about the problem.
        String input = "root(ROOT-0, have-2), nsubj(have-2, we-1), neg(idea-4, no-3), dobj(have-2, idea-4), det(problem-7, the-6), prep_about(idea-4, problem-7), sumo(SubjectiveAssessmentAttribute, problem-7), sumo(Proposition, idea-4), tense(PAST, have-2), number(SINGULAR, idea-4), number(SINGULAR, problem-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers idea-4 problem-7)",
                "(instance idea-4 Proposition)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testIndicationAbout() {
        // There are indications about economic recovery.
        String input = "root(ROOT-0, be-2), expl(be-2, there-1), nsubj(be-2, indication-3), amod(recovery-6, economic-5), prep_about(indication-3, recovery-6), sumo(SubjectiveAssessmentAttribute, recovery-6), sumo(SubjectiveAssessmentAttribute, there-1), sumo(FinancialTransaction, economic-5), sumo(ContentBearingObject, indication-3), tense(PRESENT, be-2), number(PLURAL, indication-3), number(SINGULAR, recovery-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers indication-3 recovery-6)",
                "(instance indication-3 ContentBearingObject)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testInferenceAbout() {
        // Inference about data is not totally accurate.
        String input = "root(ROOT-0, accurate-7), nsubj(accurate-7, inference-1), prep_about(inference-1, datum-3), cop(accurate-7, be-4), neg(accurate-7, not-5), advmod(accurate-7, totally-6), sumo(SubjectiveAssessmentAttribute, totally-6), sumo(True, accurate-7), sumo(True, datum-3), sumo(Reasoning, inference-1), number(SINGULAR, inference-1), number(PLURAL, datum-3), tense(PRESENT, be-4)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers inference-1 datum-3)",
                "(instance inference-1 Reasoning)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testInformationAbout() {
        // His information about the situation was not correct.
        String input = "root(ROOT-0, information-3), nsubj(information-3, he-1), cop(information-3, be-2), det(situation-6, the-5), prep_about(information-3, situation-6), vmod(situation-6, be-7), neg(be-7, not-8), ccomp(be-7, correct-9), sumo(Character, he-1), sumo(SubjectiveAssessmentAttribute, situation-6), sumo(IntentionalProcess, correct-9), sumo(FactualText, information-3), tense(PRESENT, be-2), number(SINGULAR, information-3), number(SINGULAR, situation-6), tense(PAST, be-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance information-3 FactualText)",
                "(refers information-3 situation-6)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testInsightAbout() {
        // You have good insights about the matter.
        String input = "root(ROOT-0, have-2), nsubj(have-2, you-1), amod(insight-4, good-3), dobj(have-2, insight-4), det(matter-7, the-6), prep_about(insight-4, matter-7), sumo(SubjectiveAssessmentAttribute, good-3), sumo(SubjectiveAssessmentAttribute, insight-4), sumo(Proposition, matter-7), tense(PRESENT, have-2), number(PLURAL, insight-4), number(SINGULAR, matter-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers insight-4 matter-7)",
                "(instance insight-4 SubjectiveAssessmentAttribute)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testInstructionAbout() {
        // Instructions about evacuation were not clear.
        String input = "root(ROOT-0, clear-6), nsubj(clear-6, instruction-1), prep_about(instruction-1, evacuation-3), cop(clear-6, be-4), neg(clear-6, not-5), sumo(Removing, evacuation-3), sumo(Procedure, instruction-1), sumo(SubjectiveAssessmentAttribute, clear-6), number(PLURAL, instruction-1), number(SINGULAR, evacuation-3), tense(PAST, be-4)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers instruction-1 evacuation-3)",
                "(instance instruction-1 Procedure)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testIronyAbout() {
        // That's the biggest irony about the situation.
        String input = "root(ROOT-0, irony-5), nsubj(irony-5, that-1), cop(irony-5, be-2), det(irony-5, the-3), amod(irony-5, biggest-4), det(situation-8, the-7), prep_about(irony-5, situation-8), sumo(SubjectiveAssessmentAttribute, irony-5), sumo(SubjectiveAssessmentAttribute, situation-8), tense(PRESENT, be-2), number(SINGULAR, irony-5), number(SINGULAR, situation-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance irony-5 SubjectiveAssessmentAttribute)",
                "(refers irony-5 situation-8)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testIssueAbout() {
        // The issue about economic inequality continues to dominate the debate.
        String input = "root(ROOT-0, continue-6), det(issue-2, the-1), nsubj(continue-6, issue-2), amod(inequality-5, economic-4), prep_about(issue-2, inequality-5), aux(dominate-8, to-7), xcomp(continue-6, dominate-8), det(debate-10, the-9), dobj(dominate-8, debate-10), sumo(IntentionalProcess, continue-6), sumo(Proposition, issue-2), sumo(Attribute, inequality-5), sumo(Process, dominate-8), sumo(FinancialTransaction, economic-4), sumo(Debating, debate-10), number(SINGULAR, issue-2), number(SINGULAR, inequality-5), tense(PRESENT, continue-6), number(SINGULAR, debate-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance issue-2 Proposition)",
                "(refers issue-2 inequality-5)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testJokeAbout() {
        // The joke about his appearance made him angry.
        String input = "root(ROOT-0, make-6), det(joke-2, the-1), nsubj(make-6, joke-2), poss(appearance-5, he-4), prep_about(joke-2, appearance-5), poss(angry-8, he-7), dobj(make-6, angry-8), sumo(SubjectiveAssessmentAttribute, appearance-5), sumo(SubjectiveAssessmentAttribute, joke-2), sumo(Anger, angry-8), number(SINGULAR, joke-2), number(SINGULAR, appearance-5), tense(PAST, make-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers joke-2 appearance-5)",
                "(instance joke-2 SubjectiveAssessmentAttribute)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testJudgmentAbout() {
        // The judgment about the crime was announced today.
        String input = "root(ROOT-0, announce-7), det(judgment-2, the-1), nsubjpass(announce-7, judgment-2), det(crime-5, the-4), prep_about(judgment-2, crime-5), auxpass(announce-7, be-6), tmod(announce-7, today-8), sumo(NormativeAttribute, crime-5), sumo(Proposition, judgment-2), sumo(Disseminating, announce-7), sumo(SubjectiveAssessmentAttribute, today-8), number(SINGULAR, judgment-2), number(SINGULAR, crime-5), tense(PAST, be-6), number(SINGULAR, today-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers judgment-2 crime-5)",
                "(instance judgment-2 Proposition)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testKnowledgeAbout() {
        // He has extensive knowledge about the subject.
        String input = "root(ROOT-0, have-2), nsubj(have-2, he-1), amod(knowledge-4, extensive-3), dobj(have-2, knowledge-4), det(subject-7, the-6), prep_about(knowledge-4, subject-7), sumo(Character, he-1), sumo(PsychologicalAttribute, knowledge-4), sumo(SubjectiveWeakNegativeAttribute, extensive-3), sumo(Proposition, subject-7), tense(PRESENT, have-2), number(SINGULAR, knowledge-4), number(SINGULAR, subject-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers knowledge-4 subject-7)",
                "(instance knowledge-4 PsychologicalAttribute)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testLanguageAbout() {
        // The painting represents a visual language about the topic.
        String input = "root(ROOT-0, represent-3), det(painting-2, the-1), nsubj(represent-3, painting-2), det(language-6, a-4), amod(language-6, visual-5), dobj(represent-3, language-6), det(topic-9, the-8), prep_about(language-6, topic-9), sumo(Language, language-6), sumo(Seeing, visual-5), sumo(Proposition, topic-9), sumo(Process, represent-3), sumo(Painting, painting-2), number(SINGULAR, painting-2), tense(PRESENT, represent-3), number(SINGULAR, language-6), number(SINGULAR, topic-9)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance language-6 Language)",
                "(refers language-6 topic-9)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testLearningAbout() {
        // We acquired scientific learning about the ecosystem.
        String input = "root(ROOT-0, acquire-2), nsubj(acquire-2, we-1), dobj(acquire-2, scientific-3), vmod(scientific-3, learn-4), det(ecosystem-7, the-6), prep_about(learn-4, ecosystem-7), sumo(Science, scientific-3), sumo(Learning, learn-4), sumo(SocialInteraction, ecosystem-7), sumo(Getting, acquire-2), tense(PAST, acquire-2), number(SINGULAR, ecosystem-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers learn-4 ecosystem-7)",
                "(instance learn-4 Learning)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testLessonAbout() {
        // The lesson about statistics was very helpful.
        String input = "root(ROOT-0, helpful-7), det(lesson-2, the-1), nsubj(helpful-7, lesson-2), prep_about(lesson-2, statistics-4), cop(helpful-7, be-5), advmod(helpful-7, very-6), sumo(Quantity, statistics-4), sumo(EducationalProcess, lesson-2), sumo(SubjectiveWeakPositiveAttribute, helpful-7), number(SINGULAR, lesson-2), number(PLURAL, statistics-4), tense(PAST, be-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers lesson-2 statistics-4)",
                "(instance lesson-2 EducationalProcess)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testMeetingAbout() {
        // The meeting about the election was boring.
        String input = "root(ROOT-0, boring-7), det(meeting-2, the-1), nsubj(boring-7, meeting-2), det(election-5, the-4), prep_about(meeting-2, election-5), cop(boring-7, be-6), sumo(Election, election-5), sumo(IntentionalPsychologicalProcess, boring-7), sumo(Meeting, meeting-2), number(SINGULAR, meeting-2), number(SINGULAR, election-5), tense(PAST, be-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers meeting-2 election-5)",
                "(instance meeting-2 Meeting)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testMessageAbout() {
        // Email messages about various scams inundate our mailboxes.
        String input = "root(ROOT-0, email-1), nsubj(inundate-6, message-2), amod(scam-5, various-4), prep_about(message-2, scam-5), ccomp(email-1, inundate-6), poss(mailbox-9, we-7), dobj(inundate-6, mailbox-9), sumo(Mailbox, mailbox-9), sumo(SubjectiveAssessmentAttribute, various-4), sumo(Device, email-1), sumo(Wetting, inundate-6), sumo(Text, message-2), number(PLURAL, message-2), number(PLURAL, scam-5), number(PLURAL, mailbox-9)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers message-2 scam-5)",
                "(instance message-2 Text)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testMetadataAbout() {
        // The system stores metadata about books.
        String input = "root(ROOT-0, metadata-4), prep_about(metadata-4, book-6), det(system-2, the-1), dep(metadata-4, system-2), sumo(Book, book-6), sumo(FactualText, metadata-4), number(SINGULAR, system-2), number(PLURAL, store-3), number(PLURAL, book-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers metadata-4 book-6)",
                "(instance metadata-4 FactualText)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testMovieAbout() {
        // The movie about the Civil War was pretty good.
        String input = "root(ROOT-0, good-9), det(movie-2, the-1), nsubj(good-9, movie-2), det(war-6, the-4), amod(war-6, civil-5), prep_about(movie-2, war-6), cop(good-9, be-7), advmod(good-9, pretty-8), sumo(MotionPicture, movie-2), sumo(NormativeAttribute, civil-5), sumo(SubjectiveAssessmentAttribute, pretty-8), sumo(War, war-6), sumo(SubjectiveAssessmentAttribute, good-9), number(SINGULAR, movie-2), number(SINGULAR, war-6), tense(PAST, be-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers movie-2 war-6)",
                "(instance movie-2 MotionPicture)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testMythAbout() {
        // The myth about the war is only growing.
        String input = "root(ROOT-0, grow-8), det(myth-2, the-1), nsubj(grow-8, myth-2), det(war-5, the-4), prep_about(myth-2, war-5), aux(grow-8, be-6), advmod(grow-8, only-7), sumo(War, war-5), sumo(Process, grow-8), sumo(NarrativeText, myth-2), number(SINGULAR, myth-2), number(SINGULAR, war-5), tense(PRESENT, grow-8), aspect(PROGRESSIVE, grow-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers myth-2 war-5)",
                "(instance myth-2 NarrativeText)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testNoteAbout() {
        // He made notes about possible improvements.
        String input = "root(ROOT-0, make-2), nsubj(make-2, he-1), dobj(make-2, note-3), amod(improvement-6, possible-5), prep_about(make-2, improvement-6), sumo(Character, he-1), sumo(Stating, note-3), sumo(SubjectiveAssessmentAttribute, improvement-6), sumo(Possibility, possible-5), tense(PAST, make-2), number(PLURAL, note-3), number(PLURAL, improvement-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers note-3 improvement-6)",
                "(instance note-3 Stating)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testNotionAbout() {
        // We had no clear notion about the policy.
        String input = "root(ROOT-0, have-2), nsubj(have-2, we-1), neg(notion-5, no-3), amod(notion-5, clear-4), dobj(have-2, notion-5), det(policy-9, the-7), amod(policy-9, new-8), prep_about(notion-5, policy-9), sumo(Proposition, notion-5), sumo(Policy, policy-9), sumo(SubjectiveAssessmentAttribute, clear-4), sumo(SubjectiveAssessmentAttribute, new-8), tense(PAST, have-2), number(SINGULAR, notion-5), number(SINGULAR, policy-9)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance notion-5 Proposition)",
                "(refers notion-5 policy-9)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testObservationAbout() {
        // His observation about her behavior only led to more confusion.
        String input = "root(ROOT-0, observation-3), nsubj(observation-3, he-1), cop(observation-3, be-2), poss(behavior-6, she-5), prep_about(observation-3, behavior-6), advmod(lead-8, only-7), vmod(behavior-6, lead-8), amod(confusion-11, more-10), prep_to(lead-8, confusion-11), sumo(Character, he-1), sumo(Measuring, observation-3), sumo(SubjectiveAssessmentAttribute, confusion-11), sumo(BodyMotion, behavior-6), sumo(Guiding, lead-8), tense(PRESENT, be-2), number(SINGULAR, observation-3), number(SINGULAR, behavior-6), tense(PAST, lead-8), number(SINGULAR, confusion-11)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance observation-3 Measuring)",
                "(refers observation-3 behavior-6)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testOpinionAbout() {
        // People have different opinions about religion.
        String input = "root(ROOT-0, have-2), nsubj(have-2, people-1), amod(opinion-4, different-3), dobj(have-2, opinion-4), prep_about(opinion-4, religion-6), sumo(equal, different-3), sumo(GroupOfPeople, people-1), sumo(Proposition, religion-6), sumo(Proposition, opinion-4), number(PLURAL, people-1), tense(PRESENT, have-2), number(PLURAL, opinion-4), number(SINGULAR, religion-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers opinion-4 religion-6)",
                "(instance opinion-4 Proposition)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testPerceptionAbout() {
        // Different people have different perceptions about the same situation.
        String input = "root(ROOT-0, have-3), amod(people-2, different-1), nsubj(have-3, people-2), amod(perception-5, different-4), dobj(have-3, perception-5), det(situation-9, the-7), amod(situation-9, same-8), prep_about(perception-5, situation-9), sumo(GroupOfPeople, people-2), sumo(Perception, perception-5), sumo(equal, different-1), sumo(equal, different-4), sumo(SubjectiveAssessmentAttribute, situation-9), number(PLURAL, people-2), tense(PRESENT, have-3), number(PLURAL, perception-5), number(SINGULAR, situation-9)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers perception-5 situation-9)",
                "(instance perception-5 Perception)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testPerspectiveAbout() {
        // He has a fresh perspective about the matter.
        String input = "root(ROOT-0, have-2), nsubj(have-2, he-1), det(perspective-5, a-3), amod(perspective-5, fresh-4), dobj(have-2, perspective-5), det(matter-8, the-7), prep_about(perspective-5, matter-8), sumo(Character, he-1), sumo(SubjectiveWeakPositiveAttribute, fresh-4), sumo(Proposition, perspective-5), sumo(Proposition, matter-8), tense(PRESENT, have-2), number(SINGULAR, perspective-5), number(SINGULAR, matter-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers perspective-5 matter-8)",
                "(instance perspective-5 Proposition)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testPredictionAbout() {
        // The ancient prediction about the doomsday is well known.
        String input = "root(ROOT-0, know-9), det(prediction-3, the-1), amod(prediction-3, ancient-2), nsubjpass(know-9, prediction-3), det(doomsday-6, the-5), prep_about(prediction-3, doomsday-6), auxpass(know-9, be-7), advmod(know-9, be-7), sumo(Interpreting, know-9), sumo(SubjectiveAssessmentAttribute, doomsday-6), sumo(Human, ancient-2), sumo(Predicting, prediction-3), number(SINGULAR, prediction-3), number(SINGULAR, doomsday-6), tense(PRESENT, be-7)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers prediction-3 doomsday-6)",
                "(instance prediction-3 Predicting)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testPresentationAbout() {
        // His presentation about his research was very interesting.
        String input = "root(ROOT-0, interesting-12), det(doomsday-2, the-1), poss(presentation-4, doomsday-2), nsubj(interesting-12, presentation-4), det(doomsday-7, the-6), poss(research-9, doomsday-7), prep_about(presentation-4, research-9), cop(interesting-12, be-10), advmod(interesting-12, very-11), sumo(Investigating, research-9), sumo(SubjectiveAssessmentAttribute, interesting-12), sumo(SubjectiveAssessmentAttribute, doomsday-2), sumo(IntentionalProcess, presentation-4), sumo(SubjectiveAssessmentAttribute, doomsday-7), number(SINGULAR, doomsday-2), number(SINGULAR, presentation-4), number(SINGULAR, doomsday-7), number(SINGULAR, research-9), tense(PAST, be-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(instance presentation-4 IntentionalProcess)",
                "(refers presentation-4 research-9)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testProgramAbout() {
        // He got his degree from a program about literature.
        String input = "root(ROOT-0, get-2), nsubj(get-2, he-1), nsubj(degree-5, he-3), cop(degree-5, be-4), ccomp(get-2, degree-5), det(program-8, a-7), prep_from(degree-5, program-8), prep_about(program-8, literature-10), sumo(Character, he-1), sumo(Plan, program-8), sumo(ConstantQuantity, degree-5), sumo(Getting, get-2), sumo(Character, he-3), sumo(SubjectiveAssessmentAttribute, literature-10), tense(PAST, get-2), tense(PRESENT, be-4), number(SINGULAR, degree-5), number(SINGULAR, program-8), number(SINGULAR, literature-10)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers program-8 literature-10)",
                "(instance program-8 Plan)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testQualmsAbout() {
        // He has no qualms about manhunt.
        String input = "root(ROOT-0, have-2), nsubj(have-2, he-1), neg(qualm-4, no-3), dobj(have-2, qualm-4), prep_about(qualm-4, manhunt-6), sumo(Character, he-1), sumo(Pursuing, manhunt-6), sumo(EmotionalState, qualm-4), tense(PRESENT, have-2), number(PLURAL, qualm-4), number(SINGULAR, manhunt-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers qualm-4 manhunt-6)",
                "(instance qualm-4 EmotionalState)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testQueryAbout() {
        // The system can handle queries about general topics.
        String input = "root(ROOT-0, handle-4), det(system-2, the-1), nsubj(handle-4, system-2), aux(handle-4, can-3), dobj(handle-4, query-5), amod(topic-8, general-7), prep_about(handle-4, topic-8), sumo(Attribute, handle-4), sumo(forall, general-7), sumo(Requesting, query-5), sumo(Proposition, topic-8), number(SINGULAR, system-2), number(PLURAL, query-5), number(PLURAL, topic-8)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers query-5 topic-8)",
                "(instance query-5 Requesting)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

    @Test
    public void testQuestionAbout() {
        // Any questions about the procedure should be directed to the doctor.
        String input = "root(ROOT-0, direct-8), det(question-2, any-1), nsubjpass(direct-8, question-2), det(procedure-5, the-4), prep_about(question-2, procedure-5), aux(direct-8, should-6), auxpass(direct-8, be-7), det(doctor-11, the-10), prep_to(direct-8, doctor-11), sumo(IntentionalProcess, procedure-5), sumo(exists, any-1), sumo(Ordering, direct-8), sumo(Questioning, question-2), sumo(MedicalDoctor, doctor-11), number(PLURAL, question-2), number(SINGULAR, procedure-5), number(SINGULAR, doctor-11)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(refers question-2 procedure-5)",
                "(instance question-2 Questioning)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        assertThat(kifClauses, hasItems(expected));
    }

//    @Test
//    public void testAbout() {
//        //
//        String input = "";
//        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);
//
//        String[] expected = {
//        };
//
//        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
//        assertThat(kifClauses, hasItems(expected));
//    }

//    @Test
//    public void testAbout() {
//        //
//        String input = "";
//        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);
//
//        String[] expected = {
//        };
//
//        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
//        assertThat(kifClauses, hasItems(expected));
//    }

}
