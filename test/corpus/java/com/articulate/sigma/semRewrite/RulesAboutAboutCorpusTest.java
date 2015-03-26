package com.articulate.sigma.semRewrite;

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

import com.articulate.sigma.Formula;
import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.KBmanager;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author sathenikos
 */
public class RulesAboutAboutCorpusTest extends IntegrationTestBase {

    public static Interpreter interpreter;

    @BeforeClass
    public static void initInterpreter() {
        interpreter = new Interpreter();
        KBmanager.getMgr().initializeOnce();
        interpreter.loadRules();
    }

    @Test
    public void testAnnouncedAbout() {
        String input = "John announced about his plan.";

        String expectedKifString = "(exists (?John-1 ?plan-5 ?announced-2)\n" +
                "  (and\n" +
                "    (agent ?announced-2 ?John-1)\n" +
                "    (attribute ?John-1 Male)\n" +
                "    (instance ?announced-2 Disseminating)\n" +
                "    (refers ?announced-2 ?plan-5)\n" +
                "    (instance ?John-1 Human)\n" +
                "    (instance ?plan-5 Plan)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testAskAbout() {
        String input = "John asked about my opinion.";

        String expectedKifString = "(exists (?John-1 ?asked-2 ?opinion-5)\n" +
                "  (and\n" +
                "    (agent ?asked-2 ?John-1)\n" +
                "    (attribute ?John-1 Male)\n" +
                "    (instance ?opinion-5 Proposition)\n" +
                "    (refers ?asked-2 ?opinion-5)\n" +
                "    (instance ?John-1 Human)\n" +
                "    (instance ?asked-2 Questioning)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testBeAbout() {
        String input = "Section Five is about the impact on student learning.";

        String expectedKifString = "(exists (?impact-6 ?studentlearning-8 ?Section-1 ?Five-2)\n" +
                "  (and\n" +
                "    (instance ?Section-1 Collection)\n" +
                "    (membersType ?Section-1 Text)\n" +
                "    (membersCount ?Section-1 ?Five-2)\n" +
                "    (refers ?Section-1 ?impact-6)\n" +
                "    (instance ?Section-1 Text)\n" +
                "    (instance ?studentlearning-8 student)\n" +
                "    (instance ?studentlearning-8 Learning)\n" +
                "    (instance ?impact-6 Impacting)\n" +
                "    (instance ?Five-2 Integer)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testBoastAbout() {
        String input = "Toyota officials boasted about the effectiveness of the new measures.";

        String expectedKifString = "(exists (?new-9 ?Toyotaofficials-1 ?measures-10 ?boasted-3 ?effectiveness-6 ?officials-2)\n" +
                "  (and\n" +
                "    (agent ?boasted-3 ?officials-2)\n" +
                "    (attribute ?measures-10 ?new-9)\n" +
                "    (instance ?effectiveness-6 SubjectiveAssessmentAttribute)\n" +
                "    (refers ?boasted-3 ?effectiveness-6)\n" +
                "    (instance ?boasted-3 Communication)\n" +
                "    (instance ?new-9 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?Toyotaofficials-1 City)\n" +
                "    (instance ?Toyotaofficials-1 Human)\n" +
                "    (instance ?measures-10 Measuring)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testChatWithXAbout() {
        String input = "He chatted with her about his promotion.";

        String expectedKifString = "(exists (?chatted-2 ?promotion-7 ?He-1)\n" +
                "  (and\n" +
                "    (agent ?chatted-2 ?He-1)\n" +
                "    (instance ?He-1 Character)\n" +
                "    (refers ?chatted-2 ?promotion-7)\n" +
                "    (instance ?promotion-7 Text)\n" +
                "    (instance ?chatted-2 Communication)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testComplainAbout() {
        String input = "They seldom complain about unsafe work environments because of fear of losing their jobs.";

        String expectedKifString = "(exists (?They-1 ?losing-12 ?complain-3 ?fear-10 ?jobs-14 ?environments-7 ?workenvironments-6 ?selcom-2 ?unsafe-5)\n" +
                "  (and\n" +
                "    (agent ?selcom-2 ?They-1)\n" +
                "    (attribute ?environments-7 ?unsafe-5)\n" +
                "    (instance ?fear-10 EmotionalState)\n" +
                "    (patient ?losing-12 ?jobs-14)\n" +
                "    (refers ?complain-3 ?environments-7)\n" +
                "    (instance ?complain-3 Expressing)\n" +
                "    (instance ?unsafe-5 SubjectiveWeakNegativeAttribute)\n" +
                "    (instance ?losing-12 not)\n" +
                "    (instance ?workenvironments-6 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?jobs-14 Position)\n" +
                "    (instance ?workenvironments-6 IntentionalProcess)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testDisagreeAbout() {
        String input = "Stakeholders disagreed about the relative importance of climate change.";

        String expectedKifString = "(exists (?Stakeholders-1 ?climatechange-8 ?importance-6 ?disagreed-2 ?relative-5)\n" +
                "  (and\n" +
                "    (agent ?disagreed-2 ?Stakeholders-1)\n" +
                "    (attribute ?importance-6 ?relative-5)\n" +
                "    (instance ?climatechange-8 causes)\n" +
                "    (refers ?disagreed-2 ?importance-6)\n" +
                "    (instance ?climatechange-8 Attribute)\n" +
                "    (instance ?importance-6 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?disagreed-2 Communication)\n" +
                "    (instance ?relative-5 SubjectiveAssessmentAttribute)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testExplainAbout() {
        String input = "He explained about the reason for his sudden absence.";

        String expectedKifString = "(exists (?explained-2 ?sudden-8 ?He-1 ?reason-5 ?absence-9)\n" +
                "  (and\n" +
                "    (agent ?explained-2 ?He-1)\n" +
                "    (attribute ?absence-9 ?sudden-8)\n" +
                "    (instance ?absence-9 located)\n" +
                "    (refers ?explained-2 ?reason-5)\n" +
                "    (instance ?He-1 Character)\n" +
                "    (instance ?reason-5 Proposition)\n" +
                "    (instance ?explained-2 ContentDevelopment)\n" +
                "    (instance ?sudden-8 SubjectiveAssessmentAttribute)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testFeelAbout() {
        String input = "How do you feel about the current state of economy?";

        String expectedKifString = "(exists (?current-7 ?economy-10 ?you-3 ?state-8 ?feel-4 ?How-1)\n" +
                "  (and\n" +
                "    (agent ?feel-4 ?you-3)\n" +
                "    (attribute ?feel-4 ?How-1)\n" +
                "    (instance ?state-8 Attribute)\n" +
                "    (refers ?feel-4 ?state-8)\n" +
                "    (attribute ?state-8 ?current-7)\n" +
                "    (instance ?feel-4 EmotionalState)\n" +
                "    (instance ?economy-10 EconomicAttribute)\n" +
                "    (instance ?current-7 TimePosition)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testForgetAbout() {
        String input = "Thus IP forgets about a packet and recycles its buffer immediately after forwarding.";

        String expectedKifString = "(exists (?IP-2 ?buffer-10 ?forgets-3 ?Thus-1 ?immediately-11 ?recycles-8 ?packet-6 ?forwarding-13)\n" +
                "  (and\n" +
                "    (agent ?forgets-3 ?IP-2)\n" +
                "    (attribute ?forgets-3 ?Thus-1)\n" +
                "    (greaterThan ?recycles-8 ?forwarding-13)\n" +
                "    (instance ?packet-6 Collection)\n" +
                "    (patient ?recycles-8 ?buffer-10)\n" +
                "    (refers ?forgets-3 ?packet-6)\n" +
                "    (agent ?recycles-8 ?IP-2)\n" +
                "    (attribute ?recycles-8 ?immediately-11)\n" +
                "    (instance ?forwarding-13 Transfer)\n" +
                "    (instance ?immediately-11 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?forgets-3 Remembering)\n" +
                "    (instance ?buffer-10 CompoundSubstance)\n" +
                "    (instance ?Thus-1 resource)\n" +
                "    (instance ?IP-2 FieldOfStudy)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testGeneralizeAbout() {
        String input = "We often attempt to generalize about the varied and often conflicting viewpoints of many millions.";

        String expectedKifString = "(exists (?We-1 ?often-2 ?viewpoints-12 ?varied-8 ?conflicting-11 ?millions-15 ?generalize-5 ?often-10 ?attempt-3 ?many-14)\n" +
                "  (and\n" +
                "    (agent ?attempt-3 ?We-1)\n" +
                "    (attribute ?attempt-3 ?often-2)\n" +
                "    (instance ?viewpoints-12 SubjectiveAssessmentAttribute)\n" +
                "    (refers ?generalize-5 ?viewpoints-12)\n" +
                "    (attribute ?conflicting-11 ?often-10)\n" +
                "    (instance ?millions-15 PositiveInteger)\n" +
                "    (attribute ?viewpoints-12 ?varied-8)\n" +
                "    (instance ?varied-8 Process)\n" +
                "    (attribute ?viewpoints-12 ?conflicting-11)\n" +
                "    (instance ?conflicting-11 SubjectiveAssessmentAttribute)\n" +
                "    (attribute ?millions-15 ?many-14)\n" +
                "    (instance ?attempt-3 IntentionalProcess)\n" +
                "    (instance ?generalize-5 IntentionalPsychologicalProcess)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testGossipAbout() {
        String input = "It was so we could gossip about everyone who was not there.";

        String expectedKifString = "(exists (?gossip-6 ?It-1 ?who-9 ?everyone-8 ?was-10 ?we-4 ?so-3 ?there-12)\n" +
                "  (and\n" +
                "    (agent ?so-3 ?It-1)\n" +
                "    (attribute ?was-10 ?there-12)\n" +
                "    (instance ?It-1 FieldOfStudy)\n" +
                "    (refers ?gossip-6 ?everyone-8)\n" +
                "    (agent ?gossip-6 ?we-4)\n" +
                "    (instance ?gossip-6 LinguisticCommunication)\n" +
                "    (agent ?was-10 ?who-9)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testHearAbout() {
        String input = "We hear about this type of transportation very little because it is the one method that least impacts our environment.";

        String expectedKifString = "(exists (?method-15 ?We-1 ?least-17 ?impacts-18 ?transportation-7 ?environment-20 ?very-8 ?type-5 ?little-9 ?hear-2)\n" +
                "  (and\n" +
                "    (agent ?hear-2 ?We-1)\n" +
                "    (attribute ?little-9 ?very-8)\n" +
                "    (instance ?method-15 Collection)\n" +
                "    (membersType ?method-15 Procedure)\n" +
                "    (membersCount ?method-15 1)\n" +
                "    (refers ?hear-2 ?type-5)\n" +
                "    (attribute ?hear-2 ?little-9)\n" +
                "    (instance ?environment-20 SubjectiveAssessmentAttribute)\n" +
                "    (attribute ?impacts-18 ?least-17)\n" +
                "    (instance ?method-15 Procedure)\n" +
                "    (instance ?impacts-18 Impacting)\n" +
                "    (instance ?transportation-7 TransportationDevice)\n" +
                "    (instance ?hear-2 Hearing)\n" +
                "    (instance ?type-5 Entity)\n" +
                "    (instance ?little-9 SubjectiveStrongNegativeAttribute)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testInquireAbout() {
        String input = "After years of inquiring about a girlfriend, Jose Dominguez's dad finally stopped.";

        String expectedKifString = "(exists (?stopped-14 ?dad-12 ?finally-13 ?girlfriend-7 ?inquiring-4)\n" +
                "  (and\n" +
                "    (agent ?stopped-14 ?dad-12)\n" +
                "    (attribute ?stopped-14 ?finally-13)\n" +
                "    (instance ?girlfriend-7 Female)\n" +
                "    (refers ?inquiring-4 ?girlfriend-7)\n" +
                "    (instance ?finally-13 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?stopped-14 IntentionalProcess)\n" +
                "    (instance ?dad-12 Male)\n" +
                "    (instance ?inquiring-4 Questioning)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testLaughAbout() {
        String input = "Maybe, when she is in her 60's, we can sit and laugh about this.";

        String expectedKifString = "(exists (?sit-13 ?this-17 ?is-5 ?Maybe-1 ?laugh-15 ?we-11 ?when-3 ?she-4)\n" +
                "  (and\n" +
                "    (agent ?is-5 ?she-4)\n" +
                "    (attribute ?sit-13 ?Maybe-1)\n" +
                "    (instance ?Maybe-1 SubjectiveAssessmentAttribute)\n" +
                "    (refers ?laugh-15 ?this-17)\n" +
                "    (agent ?sit-13 ?we-11)\n" +
                "    (attribute ?is-5 ?when-3)\n" +
                "    (instance ?sit-13 SittingDown)\n" +
                "    (instance ?laugh-15 Laughing)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testMutterAbout() {
        String input = "Some passengers say the captain muttered about al-Qaeda.";

        String expectedKifString = "(exists (?say-3 ?muttered-6 ?passengers-2 ?captain-5 ?Some-1)\n" +
                "  (and\n" +
                "    (agent ?say-3 ?passengers-2)\n" +
                "    (instance ?muttered-6 Speaking)\n" +
                "    (refers ?muttered-6 al-?Qaeda-8)\n" +
                "    (agent ?muttered-6 ?captain-5)\n" +
                "    (instance ?captain-5 USMilitaryRankO3)\n" +
                "    (instance ?passengers-2 SocialRole)\n" +
                "    (instance ?Some-1 exists) (instance ?say-3 Stating)\n" +
                "    (instance al-?Qaeda-8 Alabama)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testProclaimAbout() {
        String input = "Here I kiss what those waters of baptism proclaimed about the world.";

        String expectedKifString = "(exists (?waters-6 ?I-2 ?world-12 ?Here-1 ?what-4 ?proclaimed-9 ?baptism-8 ?kiss-3)\n" +
                "  (and\n" +
                "    (agent ?proclaimed-9 ?waters-6)\n" +
                "    (instance ?baptism-8 ReligiousProcess)\n" +
                "    (patient ?proclaimed-9 ?what-4)\n" +
                "    (refers ?proclaimed-9 ?world-12)\n" +
                "    (instance ?I-2 AlphabeticCharacter)\n" +
                "    (instance ?Here-1 TemporalRelation)\n" +
                "    (instance ?waters-6 Water)\n" +
                "    (instance ?kiss-3 Kissing)\n" +
                "    (instance ?proclaimed-9 Declaring)\n" +
                "    (instance ?world-12 Object)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testReadAbout() {
        String input = "Young and Townsend don't know why anyone would want to read about them.";

        String expectedKifString = "(exists (?know-6 ?them-14 ?want-10 ?anyone-8 ?read-12 ?Townsend-3 ?why-7 ?Young-1)\n" +
                "  (and\n" +
                "    (agent ?know-6 ?Young-1)\n" +
                "    (attribute ?want-10 ?why-7)\n" +
                "    (instance ?Young-1 HumanYouth)\n" +
                "    (refers ?read-12 ?them-14)\n" +
                "    (agent ?know-6 ?Townsend-3)\n" +
                "    (instance ?Townsend-3 Man)\n" +
                "    (agent ?want-10 ?anyone-8)\n" +
                "    (authors ?X ?read-12)\n" +
                "    (instance ?read-12 Reading)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testRememberAbout() {
        String input = "Then it started to rain and then he remembered about his children and he went home as fast as he could and they were fine.";

        String expectedKifString = "(exists (?rain-5 ?then-7 ?it-2 ?he-20 ?Then-1 ?fast-18 ?could-21 ?he-8 ?as-17 ?went-15 ?fine-26 ?started-3 ?children-12 ?children-24 ?remembered-9 ?he-14 ?home-16)\n" +
                "  (and\n" +
                "    (EndFn ?started-3 ?rain-5)\n" +
                "    (agent ?started-3 ?it-2)\n" +
                "    (attribute ?started-3 ?Then-1)\n" +
                "    (instance ?fast-18 SubjectiveWeakNegativeAttribute)\n" +
                "    (refers ?remembered-9 ?children-12)\n" +
                "    (agent ?remembered-9 ?he-8)\n" +
                "    (attribute ?remembered-9 ?then-7)\n" +
                "    (instance ?remembered-9 Remembering)\n" +
                "    (agent ?went-15 ?he-14)\n" +
                "    (attribute ?fast-18 ?as-17)\n" +
                "    (instance ?started-3 Process)\n" +
                "    (agent ?could-21 ?he-20)\n" +
                "    (instance ?children-24 HumanChild)\n" +
                "    (instance ?fine-26 SubjectiveWeakPositiveAttribute)\n" +
                "    (instance ?went-15 Transportation)\n" +
                "    (instance ?home-16 Home)\n" +
                "    (instance ?rain-5 Raining)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testReportAbout() {
        String input = "She reports about the benefits and detriments of the proprietary Royal Scottish Geographical Society's Images.";

        String expectedKifString = "(exists (?Images-16 ?proprietary-10 ?reports-2 ?She-1 ?RoyalScottishGeographicalSociety-11 ?benefits-5 ?detriments-7)\n" +
                "  (and\n" +
                "    (agent ?reports-2 ?She-1)\n" +
                "    (attribute ?Images-16 ?proprietary-10)\n" +
                "    (instance ?RoyalScottishGeographicalSociety-11 SubjectiveAssessmentAttribute)\n" +
                "    (refers ?reports-2 ?benefits-5)\n" +
                "    (instance ?RoyalScottishGeographicalSociety-11 Position)\n" +
                "    (refers ?reports-2 ?detriments-7)\n" +
                "    (instance ?benefits-5 Funding)\n" +
                "    (instance ?reports-2 Stating)\n" +
                "    (instance ?Images-16 Icon)\n" +
                "    (instance ?RoyalScottishGeographicalSociety-11 FieldOfStudy)\n" +
                "    (instance ?RoyalScottishGeographicalSociety-11 EnglishLanguage)\n" +
                "    (instance ?proprietary-10 Proprietorship)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testSmileAbout() {
        String input = "Proctor smiled about the encounter Wednesday afternoon.";

        String expectedKifString = "(exists (?afternoon-7 ?smiled-2 ?Proctor-1 ?encounter-5 ?Wednesday-6)\n" +
                "  (and\n" +
                "    (time ?smiled-2\n" +
                "      (DayFn ?Wednesday-6\n" +
                "        (MonthFn ?M\n" +
                "          (YearFn ?Y))))\n" +
                "    (agent ?smiled-2 ?Proctor-1)\n" +
                "    (instance ?smiled-2 Smiling)\n" +
                "    (refers ?smiled-2 ?encounter-5)\n" +
                "    (instance ?Proctor-1 SocialRole)\n" +
                "    (instance ?encounter-5 Meeting)\n" +
                "    (instance ?afternoon-7 Afternoon)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testSpeakAbout() {
        String input = "They often spoke about events from a long past.";

        String expectedKifString = "(exists (?spoke-3 ?often-2 ?They-1 ?long-8 ?past-9 ?events-5)\n" +
                "  (and\n" +
                "    (agent ?spoke-3 ?They-1)\n" +
                "    (attribute ?spoke-3 ?often-2)\n" +
                "    (instance ?spoke-3 Speaking)\n" +
                "    (refers ?spoke-3 ?events-5)\n" +
                "    (attribute ?past-9 ?long-8)\n" +
                "    (instance ?past-9 PastFn)\n" +
                "    (instance ?long-8 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?events-5 Process)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testSpeakUpAbout() {
        String input = "They should speak up about their concerns.";

        String expectedKifString = "(exists (?They-1 ?concerns-7 ?speak-3)\n" +
                "  (and\n" +
                "    (agent ?speak-3 ?They-1)\n" +
                "    (instance ?speak-3 Speaking)\n" +
                "    (refers ?speak-3 ?concerns-7)\n" +
                "    (instance ?concerns-7 refers)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testSpeculateAbout() {
        String input = "Policy-makers have speculated about the cause of this division.";

        String expectedKifString = "(exists (?division-9 ?speculated-3 ?cause-6)\n" +
                "  (and\n" +
                "    (agent ?speculated-3 Policy-?makers-1)\n" +
                "    (instance ?speculated-3 IntentionalPsychologicalProcess)\n" +
                "    (refers ?speculated-3 ?cause-6)\n" +
                "    (instance Policy-?makers-1 Policy)\n" +
                "    (instance ?division-9 MilitaryOrganization)\n" +
                "    (instance ?cause-6 Stating)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testStudyAbout() {
        String input = "They study about human biology in this course.";

        String expectedKifString = "(exists (?They-1 ?human-4 ?study-2 ?course-8 ?biology-5)\n" +
                "  (and\n" +
                "    (agent ?study-2 ?They-1)\n" +
                "    (attribute ?biology-5 ?human-4)\n" +
                "    (instance ?biology-5 Biology)\n" +
                "    (refers ?study-2 ?biology-5)\n" +
                "    (time ?biology-5 ?course-8)\n" +
                "    (instance ?human-4 Human)\n" +
                "    (instance ?study-2 Investigating)\n" +
                "    (instance ?course-8 EducationalProcess)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testTalkAbout() {
        String input = "Patients talk about shared decision-making.";

        String expectedKifString = "(exists (?talk-2 ?shared-4 ?Patients-1)\n" +
                "  (and\n" +
                "    (agent ?talk-2 ?Patients-1)\n" +
                "    (attribute decision-?making-5 ?shared-4)\n" +
                "    (instance decision-?making-5 Deciding)\n" +
                "    (refers ?talk-2 decision-?making-5)\n" +
                "    (instance ?shared-4 Sharing)\n" +
                "    (instance ?Patients-1 Human)\n" +
                "    (instance ?talk-2 Speaking)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testTeachAbout() {
        String input = "As a field that teaches about technological changes, it is important for educators to keep up with those technological changes.";

        String expectedKifString = "(exists (?important-12 ?educators-14 ?technological-20 ?field-3 ?changes-21 ?technological-7 ?teaches-5 ?that-4 ?changes-8)\n" +
                "  (and\n" +
                "    (agent ?teaches-5 ?that-4)\n" +
                "    (attribute ?changes-8 ?technological-7)\n" +
                "    (instance ?teaches-5 EducationalProcess)\n" +
                "    (refers ?teaches-5 ?changes-8)\n" +
                "    (attribute ?changes-21 ?technological-20)\n" +
                "    (instance ?important-12 SubjectiveWeakPositiveAttribute)\n" +
                "    (instance ?educators-14 Teacher)\n" +
                "    (instance ?changes-21 causes)\n" +
                "    (instance ?field-3 Field)\n" +
                "    (instance ?technological-20 NormativeAttribute)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testUnderstandAbout() {
        String input = "The more you understand about a mechanism, the more you can do to alter it.";

        String expectedKifString = "(exists (?do-13 ?mechanism-7 ?understand-4 ?you-3 ?you-11 ?alter-15 ?mechanism-17)\n" +
                "  (and\n" +
                "    (agent ?understand-4 ?you-3)\n" +
                "    (instance ?alter-15 causes)\n" +
                "    (patient ?alter-15 ?mechanism-17)\n" +
                "    (refers ?understand-4 ?mechanism-7)\n" +
                "    (agent ?do-13 ?you-11)\n" +
                "    (instance ?understand-4 Interpreting)\n" +
                "    (instance ?mechanism-17 ChemicalProcess)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testWriteAbout() {
        String input = "He has written about environmental contaminants, hazards, and technology for solving environmental problems.";

        String expectedKifString = "exists (?environmental-5 ?environmental-14 ?contaminants-6 ?written-3 ?problems-15 ?solving-13 ?hazards-8 ?He-1 ?technology-11)\n" +
                "  (and\n" +
                "    (agent ?written-3 ?He-1)\n" +
                "    (attribute ?contaminants-6 ?environmental-5)\n" +
                "    (instance ?He-1 Character)\n" +
                "    (patient ?solving-13 ?problems-15)\n" +
                "    (refers ?written-3 ?contaminants-6)\n" +
                "    (earlier\n" +
                "      (WhenFn ?written-3) Now)\n" +
                "    (attribute ?problems-15 ?environmental-14)\n" +
                "    (instance ?solving-13 Reasoning)\n" +
                "    (refers ?written-3 ?hazards-8)\n" +
                "    (instance ?technology-11 Engineering)\n" +
                "    (refers ?written-3 ?technology-11)\n" +
                "    (instance ?problems-15 SubjectiveAssessmentAttribute)\n" +
                "    (authors ?X ?written-3)\n" +
                "    (instance ?written-3 Writing)\n" +
                "    (instance ?environmental-14 causes)\n" +
                "    (instance ?hazards-8 SubjectiveAssessmentAttribute)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testYellAbout() {
        String input = "Two girls yelled about the traffic accident.";

        String expectedKifString = "(exists (?accident-7 ?yelled-3 ?Two-1 ?trafficaccident-6 ?girls-2)\n" +
                "  (and\n" +
                "    (agent ?yelled-3 ?girls-2)\n" +
                "    (instance ?girls-2 Collection)\n" +
                "    (membersType ?girls-2 Female)\n" +
                "    (membersCount ?girls-2 ?Two-1)\n" +
                "    (refers ?yelled-3 ?accident-7)\n" +
                "    (instance ?girls-2 Female)\n" +
                "    (instance ?trafficaccident-6 Process)\n" +
                "    (instance ?yelled-3 Vocalizing)\n" +
                "    (instance ?trafficaccident-6 Group)\n" +
                "    (instance ?Two-1 Integer)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testAdviseXAbout() {
        String input = "The airline did not advise the passengers about the carry-on luggage.";

        String expectedKifString = "(exists (?luggage-11 ?passengers-7 ?airline-2 ?advise-5)\n" +
                "  (and\n" +
                "    (agent ?advise-5 ?airline-2)\n" +
                "    (attribute ?luggage-11 carry-?on-10)\n" +
                "    (instance ?passengers-7 SocialRole)\n" +
                "    (patient ?advise-5 ?passengers-7)\n" +
                "    (refers ?advise-5 ?luggage-11)\n" +
                "    (instance ?advise-5 Directing)\n" +
                "    (instance ?airline-2 Airline)\n" +
                "    (instance carry-?on-10 Transportation)\n" +
                "    (instance ?luggage-11 TravelContainer)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testExciteXAbout() {
        String input = "Great teachers excite students about learning.";

        String expectedKifString = "(exists (?students-4 ?teachers-2 ?excite-3 ?Great-1 ?learning-6)\n" +
                "  (and\n" +
                "    (agent ?excite-3 ?teachers-2)\n" +
                "    (attribute ?teachers-2 ?Great-1)\n" +
                "    (instance ?teachers-2 Teacher)\n" +
                "    (patient ?excite-3 ?students-4)\n" +
                "    (refers ?excite-3 ?learning-6)\n" +
                "    (instance ?students-4 student)\n" +
                "    (instance ?Great-1 SubjectiveStrongPositiveAttribute)\n" +
                "    (instance ?learning-6 Learning)\n" +
                "    (instance ?excite-3 IntentionalPsychologicalProcess)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testHearAnythingAbout() {
        String input = "I did not hear anything about this before.";

        String expectedKifString = "(exists (?this-7 ?I-1 ?before-8 ?hear-4 ?anything-5)\n" +
                "  (and\n" +
                "    (agent ?hear-4 ?I-1)\n" +
                "    (attribute ?this-7 ?before-8)\n" +
                "    (instance ?hear-4 Hearing)\n" +
                "    (patient ?hear-4 ?anything-5)\n" +
                "    (refers ?hear-4 ?this-7)\n" +
                "    (instance ?I-1 AlphabeticCharacter)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testKnowAnythingAbout() {
        String input = "Jenkins was asked by the OHA if he knew anything about cognitive behavioural therapy.";

        String expectedKifString = "(exists (?Jenkins-8 ?cognitive-12 ?behavioural-13 ?therapy-14 ?asked-3 ?anything-10 ?Jenkins-1 ?knew-9 ?OHA-6)\n" +
                "  (and\n" +
                "    (agent ?asked-3 ?OHA-6)\n" +
                "    (attribute ?therapy-14 ?cognitive-12)\n" +
                "    (instance ?asked-3 Questioning)\n" +
                "    (patient ?asked-3 ?Jenkins-1)\n" +
                "    (refers ?knew-9 ?therapy-14)\n" +
                "    (agent ?knew-9 ?Jenkins-8)\n" +
                "    (attribute ?therapy-14 ?behavioural-13)\n" +
                "    (instance ?behavioural-13 IntentionalProcess)\n" +
                "    (patient ?knew-9 ?anything-10)\n" +
                "    (instance ?cognitive-12 IntentionalPsychologicalProcess)\n" +
                "    (instance ?therapy-14 TherapeuticProcess)\n" +
                "    (instance ?knew-9 knows)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testMakeNotesAbout() {
        String input = "Listen to their feedback and make notes about the areas for improvement.";

        String expectedKifString = "(exists (?areas-10 ?Listen-1 ?improvement-12 ?feedback-4 ?notes-7)\n" +
                "  (and\n" +
                "    (EndFn ?Listen-1 ?feedback-4)\n" +
                "    (instance ?notes-7 Stating)\n" +
                "    (refers ?notes-7 ?areas-10)\n" +
                "    (instance ?Listen-1 Listening)\n" +
                "    (instance ?improvement-12 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?feedback-4 Radiating)\n" +
                "    (instance ?areas-10 GeographicArea)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testRememberSomethingAbout() {
        String input = "He remembered something about that family with the names.";

        String expectedKifString = "(exists (?names-9 ?He-1 ?family-6 ?remembered-2 ?something-3)\n" +
                "  (and\n" +
                "    (agent ?remembered-2 ?He-1)\n" +
                "    (instance ?names-9 ContentBearingObject)\n" +
                "    (patient ?remembered-2 ?something-3)\n" +
                "    (refers ?remembered-2 ?family-6)\n" +
                "    (instance ?He-1 Character)\n" +
                "    (instance ?family-6 FamilyGroup)\n" +
                "    (instance ?remembered-2 Remembering)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
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

    @Test
    public void testHypothesisAbout() {
        String input = "The goal of the present study was to test a hypothesis about the contribution of the lexicon.";

        String expectedKifString = "(exists (?goal-2 ?contribution-14 ?lexicon-17 ?was-7 ?hypothesis-11 ?study-6 ?present-5 ?test-9)\n" +
                "  (and\n" +
                "    (agent ?was-7 ?goal-2)\n" +
                "    (attribute ?study-6 ?present-5)\n" +
                "    (instance ?study-6 Investigating)\n" +
                "    (refers ?hypothesis-11 ?contribution-14)\n" +
                "    (instance ?hypothesis-11 Stating)\n" +
                "    (instance ?goal-2 Entity)\n" +
                "    (instance ?present-5 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?lexicon-17 Proposition)\n" +
                "    (instance ?contribution-14 Entity)\n" +
                "    (instance ?test-9 Investigating)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testIdeaAbout() {
        String input = "We had no idea about the fact that her family did not care.";

        String expectedKifString = "(exists (?We-1 ?care-13 ?idea-4 ?had-2 ?fact-7 ?family-10)\n" +
                "  (and\n" +
                "    (agent ?had-2 ?We-1)\n" +
                "    (instance ?family-10 FamilyGroup)\n" +
                "    (patient ?had-2 ?idea-4)\n" +
                "    (refers ?idea-4 ?fact-7)\n" +
                "    (agent ?care-13 ?family-10)\n" +
                "    (instance ?idea-4 Proposition)\n" +
                "    (instance ?fact-7 Fact)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testIndicationAbout() {
        String input = "There are indications about economic recovery.";

        String expectedKifString = "(exists (?are-2 ?recovery-6 ?indications-3 ?There-1 ?economic-5)\n" +
                "  (and\n" +
                "    (agent ?are-2 ?indications-3)\n" +
                "    (attribute ?recovery-6 ?economic-5)\n" +
                "    (instance ?recovery-6 SubjectiveAssessmentAttribute)\n" +
                "    (refers ?indications-3 ?recovery-6)\n" +
                "    (instance ?There-1 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?indications-3 ContentBearingObject)\n" +
                "    (instance ?economic-5 FinancialTransaction)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testInferenceAbout() {
        String input = "Inferences about language typology based on optional verbs are not always accurate.";

        String expectedKifString = "(exists (?optional-7 ?always-11 ?languagetypology-3 ?verbs-8 ?typology-4 ?based-5 ?Inferences-1 ?are-9 ?accurate-12)\n" +
                "  (and\n" +
                "    (attribute ?accurate-12 ?always-11)\n" +
                "    (instance ?languagetypology-3 Classifying)\n" +
                "    (refers ?Inferences-1 ?typology-4)\n" +
                "    (attribute ?verbs-8 ?optional-7)\n" +
                "    (instance ?verbs-8 Verb)\n" +
                "    (attribute ?accurate-12 ?are-9)\n" +
                "    (instance ?languagetypology-3 Language)\n" +
                "    (instance ?based-5 Reasoning)\n" +
                "    (instance ?accurate-12 True)\n" +
                "    (instance ?Inferences-1 Reasoning)\n" +
                "    (instance ?optional-7 NormativeAttribute)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testInformationAbout() {
        String input = "This asymmetric information about sellers and their goods leads buyers to dramatically lower what they are willing to pay.";

        String expectedKifString = "(exists (?information-3 ?lower-13 ?goods-8 ?sellers-5 ?willing-17 ?buyers-15 ?buyers-10 ?asymmetric-2 ?leads-9 ?dramatically-12 ?what-14)\n" +
                "  (and\n" +
                "    (agent ?leads-9 ?information-3)\n" +
                "    (attribute ?lower-13 ?dramatically-12)\n" +
                "    (instance ?dramatically-12 SubjectiveAssessmentAttribute)\n" +
                "    (patient ?leads-9 ?buyers-10)\n" +
                "    (refers ?information-3 ?sellers-5)\n" +
                "    (attribute ?information-3 ?asymmetric-2)\n" +
                "    (instance ?willing-17 desires)\n" +
                "    (patient ?willing-17 ?what-14)\n" +
                "    (refers ?information-3 ?goods-8)\n" +
                "    (instance ?sellers-5 Man)\n" +
                "    (instance ?goods-8 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?leads-9 Guiding)\n" +
                "    (instance ?asymmetric-2 ShapeAttribute)\n" +
                "    (instance ?information-3 FactualText)\n" +
                "    (instance ?buyers-15 Human)\n" +
                "    (instance ?lower-13 Transfer)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testInsightAbout() {
        String input = "The book conveys insights about the complexities, contradictions, and multiplicities of culture.";

        String expectedKifString = "(exists (?complexities-7 ?culture-14 ?contradictions-9 ?insights-4 ?multiplicities-12 ?book-2 ?conveys-3)\n" +
                "  (and\n" +
                "    (agent ?conveys-3 ?book-2)\n" +
                "    (instance ?contradictions-9 Entity)\n" +
                "    (patient ?conveys-3 ?insights-4)\n" +
                "    (refers ?insights-4 ?complexities-7)\n" +
                "    (instance ?book-2 Book)\n" +
                "    (refers ?insights-4 ?contradictions-9)\n" +
                "    (instance ?conveys-3 LinguisticCommunication)\n" +
                "    (refers ?insights-4 ?multiplicities-12)\n" +
                "    (instance ?insights-4 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?culture-14 SubjectiveAssessmentAttribute)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testInstructionAbout() {
        String input = "They did not provide instructions about the assembly process.";

        String expectedKifString = "(exists (?assemblyprocess-8 ?They-1 ?provide-4 ?instructions-5 ?process-9)\n" +
                "  (and\n" +
                "    (agent ?provide-4 ?They-1)\n" +
                "    (instance ?assemblyprocess-8 Collection)\n" +
                "    (patient ?provide-4 ?instructions-5)\n" +
                "    (refers ?provide-4 ?process-9)\n" +
                "    (instance ?instructions-5 Procedure)\n" +
                "    (instance ?provide-4 Declaring)\n" +
                "    (instance ?assemblyprocess-8 IntentionalProcess)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testIronyAbout() {
        String input = "There is a huge irony about the impact of Internet porn.";

        String expectedKifString = "(exists (?huge-4 ?Internetporn-10 ?is-2 ?irony-5 ?There-1 ?impact-8)\n" +
                "  (and\n" +
                "    (agent ?is-2 ?irony-5)\n" +
                "    (attribute ?irony-5 ?huge-4)\n" +
                "    (instance ?irony-5 SubjectiveAssessmentAttribute)\n" +
                "    (refers ?irony-5 ?impact-8)\n" +
                "    (instance ?huge-4 SubjectiveWeakNegativeAttribute)\n" +
                "    (instance ?Internetporn-10 WiredInternetConnection)\n" +
                "    (instance ?There-1 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?impact-8 Impacting)\n" +
                "    (instance ?Internetporn-10 SubjectiveAssessmentAttribute)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testIssueAbout() {
        String input = "The issue about economic inequality is going to continue to resonate.";

        String expectedKifString = "(exists (?continue-9 ?going-7 ?resonate-11 ?inequality-5 ?economic-4 ?issue-2)\n" +
                "  (and\n" +
                "    (agent ?going-7 ?issue-2)\n" +
                "    (attribute ?inequality-5 ?economic-4)\n" +
                "    (instance ?inequality-5 Attribute)\n" +
                "    (refers ?issue-2 ?inequality-5)\n" +
                "    (instance ?issue-2 Proposition)\n" +
                "    (instance ?continue-9 IntentionalProcess)\n" +
                "    (instance ?resonate-11 IntentionalPsychologicalProcess)\n" +
                "    (instance ?going-7 Transportation)\n" +
                "    (instance ?economic-4 FinancialTransaction)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testJokeAbout() {
        String input = "I have heard the joke about it before.";

        String expectedKifString = "(exists (?I-1 ?before-8 ?heard-3 ?it-7 ?joke-5)\n" +
                "  (and\n" +
                "    (agent ?heard-3 ?I-1)\n" +
                "    (attribute ?heard-3 ?before-8)\n" +
                "    (instance ?heard-3 Hearing)\n" +
                "    (patient ?heard-3 ?joke-5)\n" +
                "    (refers ?joke-5 ?it-7)\n" +
                "    (instance ?I-1 AlphabeticCharacter)\n" +
                "    (instance ?joke-5 SubjectiveAssessmentAttribute)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testJudgmentAbout() {
        String input = "His judgment about the matter is flawed.";

        String expectedKifString = "(exists (?matter-5 ?judgment-2 ?flawed-7)\n" +
                "  (and\n" +
                "    (instance ?judgment-2 Proposition)\n" +
                "    (patient ?flawed-7 ?judgment-2)\n" +
                "    (refers ?judgment-2 ?matter-5)\n" +
                "    (instance ?flawed-7 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?matter-5 Proposition)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testKnowledgeAbout() {
        String input = "Teachers will be developing students' knowledge about medical technologies in this way.";

        String expectedKifString = "(exists (?students-5 ?medical-9 ?way-13 ?technologies-10 ?knowledge-7 ?Teachers-1 ?developing-4)\n" +
                "  (and\n" +
                "    (agent ?developing-4 ?Teachers-1)\n" +
                "    (attribute ?technologies-10 ?medical-9)\n" +
                "    (instance ?knowledge-7 PsychologicalAttribute)\n" +
                "    (patient ?developing-4 ?knowledge-7)\n" +
                "    (refers ?knowledge-7 ?technologies-10)\n" +
                "    (earlier Now\n" +
                "      (WhenFn ?developing-4))\n" +
                "    (instance ?developing-4 ContentDevelopment)\n" +
                "    (instance ?students-5 student)\n" +
                "    (instance ?way-13 SubjectiveAssessmentAttribute)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testLanguageAbout() {
        String input = "His use of plain language about issues such as Social Security earned His rave reviews.";

        String expectedKifString = "(exists (?rave-14 ?earned-12 ?plain-4 ?reviews-15 ?use-2 ?language-5 ?issues-7 ?His-13 ?SocialSecurity-10)\n" +
                "  (and\n" +
                "    (agent ?earned-12 ?use-2)\n" +
                "    (attribute ?language-5 ?plain-4)\n" +
                "    (instance ?language-5 Language)\n" +
                "    (patient ?rave-14 ?reviews-15)\n" +
                "    (refers ?language-5 ?issues-7)\n" +
                "    (agent ?rave-14 ?His-13)\n" +
                "    (instance ?issues-7 Proposition)\n" +
                "    (instance ?reviews-15 Looking)\n" +
                "    (instance ?rave-14 Text)\n" +
                "    (instance ?earned-12 Getting)\n" +
                "    (instance ?SocialSecurity-10 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?SocialSecurity-10 SocialInteraction)\n" +
                "    (instance ?plain-4 SubjectiveStrongPositiveAttribute)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testLearningAbout() {
        String input = "We have acquired improved scientific learning about implementing promotion programs.";

        String expectedKifString = "(exists (?acquired-3 ?We-1 ?learning-6 ?promotionprograms-9 ?implementing-8 ?scientific-5 ?improved-4 ?programs-10)\n" +
                "  (and\n" +
                "    (agent ?acquired-3 ?We-1)\n" +
                "    (attribute ?learning-6 ?improved-4)\n" +
                "    (instance ?improved-4 Increasing)\n" +
                "    (patient ?acquired-3 ?learning-6)\n" +
                "    (refers ?acquired-3 ?implementing-8)\n" +
                "    (attribute ?learning-6 ?scientific-5)\n" +
                "    (instance ?scientific-5 Science)\n" +
                "    (patient ?implementing-8 ?programs-10)\n" +
                "    (instance ?promotionprograms-9 Plan)\n" +
                "    (instance ?implementing-8 IntentionalProcess)\n" +
                "    (instance ?learning-6 Learning)\n" +
                "    (instance ?promotionprograms-9 Text)\n" +
                "    (instance ?acquired-3 Getting)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testLessonAbout() {
        String input = "Important lessons about shared responsibility should be imparted to every student.";

        String expectedKifString = "(exists (?lessons-2 ?imparted-8 ?responsibility-5 ?shared-4 ?student-11 ?Important-1)\n" +
                "  (and\n" +
                "    (attribute ?lessons-2 ?Important-1)\n" +
                "    (instance ?student-11 student)\n" +
                "    (patient ?imparted-8 ?lessons-2)\n" +
                "    (refers ?lessons-2 ?responsibility-5)\n" +
                "    (attribute ?responsibility-5 ?shared-4)\n" +
                "    (instance ?Important-1 SubjectiveWeakPositiveAttribute)\n" +
                "    (instance ?lessons-2 EducationalProcess)\n" +
                "    (instance ?imparted-8 LinguisticCommunication)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testMeetingAbout() {
        String input = "The meeting about publicity and elections was of no interest to me.";

        String expectedKifString = "(exists (?elections-6 ?was-7 ?publicity-4 ?meeting-2)\n" +
                "  (and\n" +
                "    (agent ?was-7 ?meeting-2)\n" +
                "    (instance ?publicity-4 Text)\n" +
                "    (refers ?meeting-2 ?publicity-4)\n" +
                "    (instance ?meeting-2 Meeting)\n" +
                "    (refers ?meeting-2 ?elections-6)\n" +
                "    (instance ?elections-6 Election)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testMessageAbout() {
        String input = "Email messages about various scams inundate our mailboxes.";

        String expectedKifString = "(exists (?scams-5 ?messages-2 ?Email-1 ?inundate-6 ?various-4 ?mailboxes-8)\n" +
                "  (and\n" +
                "    (agent ?inundate-6 ?messages-2)\n" +
                "    (attribute ?scams-5 ?various-4)\n" +
                "    (instance ?various-4 SubjectiveAssessmentAttribute)\n" +
                "    (patient ?inundate-6 ?mailboxes-8)\n" +
                "    (refers ?messages-2 ?scams-5)\n" +
                "    (instance ?Email-1 Device)\n" +
                "    (instance ?mailboxes-8 Mailbox)\n" +
                "    (instance ?inundate-6 Wetting)\n" +
                "    (instance ?messages-2 Text)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testMetadataAbout() {
        String input = "You can find metadata about books and authors in a library information system.";

        String expectedKifString = "(exists (?books-6 ?authors-8 ?metadata-4 ?find-3 ?libraryinformationsystem-11 ?You-1)\n" +
                "  (and\n" +
                "    (agent ?find-3 ?You-1)\n" +
                "    (instance ?books-6 Book)\n" +
                "    (refers ?metadata-4 ?books-6)\n" +
                "    (instance ?metadata-4 FactualText)\n" +
                "    (refers ?metadata-4 ?authors-8)\n" +
                "    (instance ?libraryinformationsystem-11 Room)\n" +
                "    (instance ?authors-8 Profession)\n" +
                "    (instance ?libraryinformationsystem-11 FactualText)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testMovieAbout() {
        String input = "The movie about the famous battle is soon to hit the theaters.";

        String expectedKifString = "(exists (?soon-8 ?hit-10 ?theaters-12 ?is-7 ?famous-5 ?movie-2 ?battle-6)\n" +
                "  (and\n" +
                "    (agent ?is-7 ?movie-2)\n" +
                "    (attribute ?is-7 ?soon-8)\n" +
                "    (instance ?famous-5 SubjectiveStrongPositiveAttribute)\n" +
                "    (patient ?hit-10 ?theaters-12)\n" +
                "    (refers ?movie-2 ?battle-6)\n" +
                "    (attribute ?battle-6 ?famous-5)\n" +
                "    (instance ?movie-2 MotionPicture)\n" +
                "    (instance ?soon-8 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?theaters-12 Auditorium)\n" +
                "    (instance ?hit-10 Impelling)\n" +
                "    (instance ?battle-6 Battle)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testMythAbout() {
        String input = "Many urban myths about the famous incident are merely products of imagination.";

        String expectedKifString = "(exists (?incident-7 ?merely-9 ?products-10 ?myths-3 ?urban-2 ?Many-1 ?imagination-12 ?famous-6)\n" +
                "  (and\n" +
                "    (attribute ?products-10 ?merely-9)\n" +
                "    (instance ?products-10 Product)\n" +
                "    (refers ?myths-3 ?incident-7)\n" +
                "    (attribute ?myths-3 ?Many-1)\n" +
                "    (instance ?famous-6 SubjectiveStrongPositiveAttribute)\n" +
                "    (attribute ?myths-3 ?urban-2)\n" +
                "    (instance ?imagination-12 PsychologicalAttribute)\n" +
                "    (attribute ?incident-7 ?famous-6)\n" +
                "    (instance ?merely-9 Entity)\n" +
                "    (instance ?urban-2 Urban)\n" +
                "    (instance ?myths-3 NarrativeText)\n" +
                "    (instance ?incident-7 Process)\n" +
                "    (instance ?Many-1 SubjectiveAssessmentAttribute)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testNoteAbout() {
        String input = "Listen to their feedback and make notes about possible improvements.";

        String expectedKifString = "(exists (?possible-9 ?Listen-1 ?improvements-10 ?feedback-4 ?notes-7)\n" +
                "  (and\n" +
                "    (EndFn ?Listen-1 ?feedback-4)\n" +
                "    (attribute ?improvements-10 ?possible-9)\n" +
                "    (instance ?notes-7 Stating)\n" +
                "    (refers ?notes-7 ?improvements-10)\n" +
                "    (instance ?Listen-1 Listening)\n" +
                "    (instance ?improvements-10 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?feedback-4 Radiating)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testNotionAbout() {
        String input = "We had clear notions about the acceptability of the intervention package.";

        String expectedKifString = "(exists (?We-1 ?acceptability-7 ?interventionpackage-10 ?had-2 ?notions-4 ?clear-3)\n" +
                "  (and\n" +
                "    (agent ?had-2 ?We-1)\n" +
                "    (attribute ?notions-4 ?clear-3)\n" +
                "    (instance ?acceptability-7 SubjectiveAssessmentAttribute)\n" +
                "    (patient ?had-2 ?notions-4)\n" +
                "    (refers ?notions-4 ?acceptability-7)\n" +
                "    (instance ?clear-3 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?interventionpackage-10 Collection)\n" +
                "    (instance ?notions-4 Proposition)\n" +
                "    (instance ?interventionpackage-10 Comparing)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testObservationAbout() {
        String input = "The assessment and examination considered issues such as initial observations about Jenkins and his environment";

        String expectedKifString = "(exists (?observations-10 ?assessment-2 ?Jenkins-12 ?initial-9 ?examination-4 ?considered-5 ?issues-6 ?environment-15)\n" +
                "  (and\n" +
                "    (agent ?considered-5 ?assessment-2)\n" +
                "    (attribute ?observations-10 ?initial-9)\n" +
                "    (instance ?assessment-2 Comparing)\n" +
                "    (patient ?considered-5 ?issues-6)\n" +
                "    (refers ?observations-10 ?Jenkins-12)\n" +
                "    (agent ?considered-5 ?examination-4)\n" +
                "    (instance ?initial-9 starts)\n" +
                "    (refers ?observations-10 ?environment-15)\n" +
                "    (instance ?examination-4 Pursuing)\n" +
                "    (instance ?observations-10 Measuring)\n" +
                "    (instance ?environment-15 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?issues-6 Proposition)\n" +
                "    (instance ?considered-5 believes)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testOpinionAbout() {
        String input = "People have different opinions about same-sex marriage.";

        String expectedKifString = "(exists (?opinions-4 ?have-2 ?People-1 ?marriage-7 ?different-3)\n" +
                "  (and\n" +
                "    (agent ?have-2 ?People-1)\n" +
                "    (attribute ?opinions-4 ?different-3)\n" +
                "    (instance ?marriage-7 MarriageContract)\n" +
                "    (patient ?have-2 ?opinions-4)\n" +
                "    (refers ?opinions-4 ?marriage-7)\n" +
                "    (attribute ?marriage-7 same-?sex-6)\n" +
                "    (instance ?People-1 GroupOfPeople)\n" +
                "    (instance ?opinions-4 Proposition)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testPerceptionAbout() {
        String input = "Different people have different perceptions about the same situation.";

        String expectedKifString = "(exists (?situation-9 ?have-3 ?people-2 ?perceptions-5 ?different-4 ?Different-1 ?same-8)\n" +
                "  (and\n" +
                "    (agent ?have-3 ?people-2)\n" +
                "    (attribute ?people-2 ?Different-1)\n" +
                "    (instance ?people-2 GroupOfPeople)\n" +
                "    (patient ?have-3 ?perceptions-5)\n" +
                "    (refers ?perceptions-5 ?situation-9)\n" +
                "    (attribute ?perceptions-5 ?different-4)\n" +
                "    (instance ?perceptions-5 Perception)\n" +
                "    (attribute ?situation-9 ?same-8)\n" +
                "    (instance ?Different-1 equal)\n" +
                "    (instance ?situation-9 SubjectiveAssessmentAttribute)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testPerspectiveAbout() {
        String input = "What is your perspective about the risks and hazards associated with orchard work?";

        String expectedKifString = "(exists (?perspective-4 ?What-1 ?orchardwork-12 ?risks-7 ?associated-10 ?benefits-9)\n" +
                "  (and\n" +
                "    (instance ?associated-10 Comparing)\n" +
                "    (patient ?associated-10 ?perspective-4)\n" +
                "    (refers ?perspective-4 ?risks-7)\n" +
                "    (instance ?risks-7 SubjectiveAssessmentAttribute)\n" +
                "    (patient ?associated-10 ?What-1)\n" +
                "    (refers ?perspective-4 ?benefits-9)\n" +
                "    (instance ?perspective-4 Proposition)\n" +
                "    (instance ?orchardwork-12 IntentionalProcess)\n" +
                "    (instance ?benefits-9 Funding)\n" +
                "    (instance ?orchardwork-12 Region)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testPredictionAbout() {
        String input = "These categories differ in terms of predictions about specific effects of lexical development.";

        String expectedKifString = "(exists (?terms-5 ?differ-3 ?effects-10 ?categories-2 ?specific-9 ?development-13 ?predictions-7 ?lexical-12)\n" +
                "  (and\n" +
                "    (agent ?differ-3 ?categories-2)\n" +
                "    (attribute ?effects-10 ?specific-9)\n" +
                "    (instance ?development-13 Creation)\n" +
                "    (refers ?predictions-7 ?effects-10)\n" +
                "    (attribute ?development-13 ?lexical-12)\n" +
                "    (instance ?terms-5 NounPhrase)\n" +
                "    (instance ?predictions-7 Predicting)\n" +
                "    (instance ?specific-9 SubjectiveWeakNegativeAttribute)\n" +
                "    (instance ?effects-10 Process)\n" +
                "    (instance ?differ-3 equal)\n" +
                "    (instance ?lexical-12 Word)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testPresentationAbout() {
        String input = "The location and frequency of presentations about college shifted after the students entered high school.";

        String expectedKifString = "(exists (?presentations-6 ?high-14 ?location-2 ?shifted-9 ?entered-13 ?school-15 ?college-8 ?frequency-4 ?students-12)\n" +
                "  (and\n" +
                "    (agent ?entered-13 ?location-2)\n" +
                "    (attribute ?school-15 ?high-14)\n" +
                "    (instance ?shifted-9 SocialInteraction)\n" +
                "    (patient ?entered-13 ?school-15)\n" +
                "    (refers ?presentations-6 ?college-8)\n" +
                "    (agent ?entered-13 ?frequency-4)\n" +
                "    (instance ?location-2 Region)\n" +
                "    (instance ?frequency-4 TimeDependentQuantity)\n" +
                "    (instance ?students-12 student)\n" +
                "    (instance ?college-8 College)\n" +
                "    (instance ?high-14 Motion)\n" +
                "    (instance ?school-15 School)\n" +
                "    (instance ?entered-13 located)\n" +
                "    (instance ?presentations-6 IntentionalProcess)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testProgramAbout() {
        String input = "Despite the fierce competition, he was easily accepted into the prestigious program about economics.";

        String expectedKifString = "(exists (?program-13 ?prestigious-12 ?economics-15 ?easily-8 ?competition-4 ?fierce-3 ?he-6 ?accepted-9)\n" +
                "  (and\n" +
                "    (attribute ?accepted-9 ?easily-8)\n" +
                "    (instance ?fierce-3 SubjectiveStrongNegativeAttribute)\n" +
                "    (patient ?accepted-9 ?he-6)\n" +
                "    (refers ?program-13 ?economics-15)\n" +
                "    (attribute ?competition-4 ?fierce-3)\n" +
                "    (instance ?program-13 PublicProgram)\n" +
                "    (attribute ?program-13 ?prestigious-12)\n" +
                "    (instance ?accepted-9 ExpressingApproval)\n" +
                "    (instance ?economics-15 Economics)\n" +
                "    (instance ?easily-8 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?competition-4 BusinessCompetition)\n" +
                "    (instance ?prestigious-12 SubjectiveAssessmentAttribute)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testQualmsAbout() {
        String input = "She has no qualms about insects.";

        String expectedKifString = "(exists (?insects-6 ?She-1 ?has-2 ?qualms-4)\n" +
                "  (and\n" +
                "    (agent ?has-2 ?She-1)\n" +
                "    (instance ?insects-6 Insect)\n" +
                "    (patient ?has-2 ?qualms-4)\n" +
                "    (refers ?qualms-4 ?insects-6)\n" +
                "    (instance ?qualms-4 EmotionalState)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testQueryAbout() {
        String input = "The system can handle queries about general topics.";

        String expectedKifString = "(exists (?topics-8 ?handle-4 ?queries-5 ?system-2 ?general-7)\n" +
                "  (and\n" +
                "    (agent ?handle-4 ?system-2)\n" +
                "    (attribute ?topics-8 ?general-7)\n" +
                "    (instance ?general-7 forall) (refers ?queries-5 ?topics-8)\n" +
                "    (instance ?handle-4 capability)\n" +
                "    (instance ?queries-5 Requesting)\n" +
                "    (instance ?topics-8 Proposition)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testQuestionAbout() {
        String input = "Any questions about possible treatment options should be directed to the doctor.";

        String expectedKifString = "(exists (?questions-2 ?directed-9 ?Any-1 ?options-6 ?possible-4)\n" +
                "  (and\n" +
                "    (attribute ?options-6 ?possible-4)\n" +
                "    (instance ?directed-9 SubjectiveWeakNegativeAttribute)\n" +
                "    (patient ?directed-9 ?questions-2)\n" +
                "    (refers ?questions-2 ?options-6)\n" +
                "    (instance ?Any-1 exists) (instance ?questions-2 Questioning)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testRecommendationAbout() {
        String input = "The committee made public its recommendations about the amount of buffering.";

        String expectedKifString = "(exists (?committee-2 ?recommendations-6 ?public-4 ?amount-9 ?buffering-11)\n" +
                "  (and\n" +
                "    (agent ?recommendations-6 ?committee-2)\n" +
                "    (attribute ?recommendations-6 ?public-4)\n" +
                "    (instance ?committee-2 Commission)\n" +
                "    (refers ?recommendations-6 ?amount-9)\n" +
                "    (earlier\n" +
                "      (WhenFn ?recommendations-6) Now)\n" +
                "    (instance ?buffering-11 Combining)\n" +
                "    (instance ?public-4 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?recommendations-6 Requesting)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testReflectionAbout() {
        String input = "Reflections about the past, present, and future of the organization led to a new plan.";

        String expectedKifString = "(exists (?led-13 ?new-16 ?plan-17 ?organization-12 ?present-6 ?Reflections-1 ?past-4 ?future-9)\n" +
                "  (and\n" +
                "    (agent ?led-13 ?Reflections-1)\n" +
                "    (attribute ?plan-17 ?new-16)\n" +
                "    (instance ?plan-17 Plan)\n" +
                "    (refers ?Reflections-1 ?past-4)\n" +
                "    (instance ?organization-12 Organization)\n" +
                "    (refers ?Reflections-1 ?present-6)\n" +
                "    (instance ?led-13 Guiding)\n" +
                "    (refers ?Reflections-1 ?future-9)\n" +
                "    (instance ?present-6 TimeInterval)\n" +
                "    (instance ?new-16 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?Reflections-1 PsychologicalProcess)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testResultAbout() {
        String input = "The poll result about people's interests in politics was not surprising.";

        String expectedKifString = "(exists (?result-3 ?people-5 ?interests-7 ?poll-2 ?politics-9 ?surprising-12)\n" +
                "  (and\n" +
                "    (agent ?result-3 ?poll-2)\n" +
                "    (instance ?result-3 Process)\n" +
                "    (refers ?result-3 ?interests-7)\n" +
                "    (instance ?surprising-12 EmotionalState)\n" +
                "    (instance ?politics-9 SocialInteraction)\n" +
                "    (instance ?poll-2 Polling)\n" +
                "    (instance ?people-5 GroupOfPeople)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testRevelationAbout() {
        String input = "Despite the revelations about his pornographic postings, she still belives in him as a donor.";

        String expectedKifString = "(exists (?postings-7 ?believes-11 ?still-10 ?revelations-3 ?pornographic-6 ?she-9 ?donor-16)\n" +
                "  (and\n" +
                "    (agent ?believes-11 ?she-9)\n" +
                "    (attribute ?believes-11 ?still-10)\n" +
                "    (instance ?believes-11 believes)\n" +
                "    (refers ?revelations-3 ?postings-7)\n" +
                "    (attribute ?postings-7 ?pornographic-6)\n" +
                "    (instance ?revelations-3 Communication)\n" +
                "    (instance ?pornographic-6 ContentBearingObject)\n" +
                "    (instance ?donor-16 SocialRole)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testRumorAbout() {
        String input = "Rumors about imminent terrorist attacks abound.";

        String expectedKifString = "(exists (?attakcs-5 ?imminent-3 ?terrorist-4 ?Rumors-1 ?abound-6)\n" +
                "  (and\n" +
                "    (agent ?abound-6 ?Rumors-1)\n" +
                "    (attribute ?attakcs-5 ?imminent-3)\n" +
                "    (instance ?imminent-3 ImmediateFutureFn)\n" +
                "    (refers ?Rumors-1 ?attakcs-5)\n" +
                "    (attribute ?attakcs-5 ?terrorist-4)\n" +
                "    (instance ?abound-6 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?terrorist-4 Terrorist)\n" +
                "    (instance ?Rumors-1 HistoricalAccount)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testScenarioAbout() {
        String input = "Hypothetical scenarios about commonplace problematic situations are the topics of the exercise.";

        String expectedKifString = "(exists (?Hypothetical-1 ?commonplace-4 ?scenarios-2 ?situations-6 ?problematic-5 ?topics-9 ?exercise-12)\n" +
                "  (and\n" +
                "    (attribute ?scenarios-2 ?Hypothetical-1)\n" +
                "    (instance ?topics-9 Proposition)\n" +
                "    (refers ?scenarios-2 ?situations-6)\n" +
                "    (attribute ?situations-6 ?commonplace-4)\n" +
                "    (instance ?scenarios-2 Text)\n" +
                "    (attribute ?situations-6 ?problematic-5)\n" +
                "    (instance ?problematic-5 SubjectiveStrongNegativeAttribute)\n" +
                "    (instance ?exercise-12 RecreationOrExercise)\n" +
                "    (instance ?Hypothetical-1 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?situations-6 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?commonplace-4 SubjectiveWeakNegativeAttribute)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testShowAbout() {
        String input = "Maybe he was too rational for all the television shows about mediums and psychic children?";

        String expectedKifString = "(exists (?rational-5 ?mediums-12 ?too-4 ?shows-10 ?television-9 ?Maybe-1 ?children-15 ?pscyhic-14)\n" +
                "  (and\n" +
                "    (agent ?shows-10 ?television-9)\n" +
                "    (attribute ?rational-5 ?Maybe-1)\n" +
                "    (instance ?rational-5 Reasoning)\n" +
                "    (refers ?shows-10 ?mediums-12)\n" +
                "    (attribute ?rational-5 ?too-4)\n" +
                "    (instance ?television-9 TelevisionReceiver)\n" +
                "    (refers ?shows-10 ?children-15)\n" +
                "    (attribute ?children-15 ?pscyhic-14)\n" +
                "    (instance ?children-15 HumanChild)\n" +
                "    (instance ?mediums-12 Region)\n" +
                "    (instance ?Maybe-1 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?shows-10 Demonstrating)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testSignalAbout() {
        String input = "We are receiving signals about climate change.";

        String expectedKifString = "(exists (?We-1 ?change-7 ?receiving-3 ?climatechange-6 ?signals-4)\n" +
                "  (and\n" +
                "    (agent ?receiving-3 ?We-1)\n" +
                "    (instance ?climatechange-6 Attribute)\n" +
                "    (patient ?receiving-3 ?signals-4)\n" +
                "    (refers ?signals-4 ?change-7)\n" +
                "    (instance ?receiving-3 Getting)\n" +
                "    (instance ?signals-4 Icon)\n" +
                "    (instance ?climatechange-6 causes)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testSkepticismAbout() {
        String input = "Skepticism about scoliosis screening has been voiced at numerous points over the last several decades.";

        String expectedKifString = "(exists (?last-13 ?scoliosisscreening-3 ?several-14 ?Skepticism-1 ?points-10 ?numerous-9 ?decades-15 ?voiced-7 ?screening-4)\n" +
                "  (and\n" +
                "    (attribute ?points-10 ?numerous-9)\n" +
                "    (instance ?numerous-9 SubjectiveAssessmentAttribute)\n" +
                "    (patient ?voiced-7 ?Skepticism-1)\n" +
                "    (refers ?Skepticism-1 ?screening-4)\n" +
                "    (earlier\n" +
                "      (WhenFn ?voiced-7) Now)\n" +
                "    (attribute ?decades-15 ?last-13)\n" +
                "    (instance ?Skepticism-1 PsychologicalAttribute)\n" +
                "    (attribute ?decades-15 ?several-14)\n" +
                "    (instance ?voiced-7 Speaking)\n" +
                "    (instance ?scoliosisscreening-3 DiseaseOrSyndrome)\n" +
                "    (instance ?decades-15 Decade)\n" +
                "    (instance ?scoliosisscreening-3 Pursuing)\n" +
                "    (instance ?points-10 GeometricPoint)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testSnobberyAbout() {
        String input = "An indie-rock snobbery about such trappings from rock's monolithic arena era remains.";

        String expectedKifString = "(exists (?monolithic-10 ?rock-8 ?trappings-6 ?such-5 ?era-12 ?remains-13 ?snobbery-3 ?arenaera-11)\n" +
                "  (and\n" +
                "    (agent ?remains-13 ?snobbery-3)\n" +
                "    (attribute ?snobbery-3 indie-?rock-2)\n" +
                "    (instance indie-?rock-2 MusicalGroup)\n" +
                "    (refers ?snobbery-3 ?trappings-6)\n" +
                "    (attribute ?trappings-6 ?such-5)\n" +
                "    (instance ?monolithic-10 SubjectiveWeakNegativeAttribute)\n" +
                "    (attribute ?era-12 ?monolithic-10)\n" +
                "    (instance ?arenaera-11 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?rock-8 Rock)\n" +
                "    (instance ?snobbery-3 TraitAttribute)\n" +
                "    (instance ?arenaera-11 TimeInterval)\n" +
                "    (instance ?trappings-6 Artifact)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testSpeculationAbout() {
        String input = "There is growing speculation about the potential contribution of climate change to violent conflicts.";

        String expectedKifString = "(exists (?contribution-8 ?speculation-4 ?potential-7 ?growing-3 ?violent-13 ?climatechange-10 ?There-1 ?conflicts-14)\n" +
                "  (and\n" +
                "    (EndFn ?growing-3 ?conflicts-14)\n" +
                "    (attribute ?contribution-8 ?potential-7)\n" +
                "    (instance ?growing-3 Process)\n" +
                "    (patient ?growing-3 ?speculation-4)\n" +
                "    (refers ?speculation-4 ?contribution-8)\n" +
                "    (attribute ?conflicts-14 ?violent-13)\n" +
                "    (instance ?violent-13 SubjectiveWeakNegativeAttribute)\n" +
                "    (instance ?speculation-4 Proposition)\n" +
                "    (instance ?contribution-8 Entity)\n" +
                "    (instance ?potential-7 capability)\n" +
                "    (instance ?climatechange-10 Attribute)\n" +
                "    (instance ?There-1 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?conflicts-14 Contest)\n" +
                "    (instance ?climatechange-10 causes)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testStatementAbout() {
        String input = "A statement about the government's position on the issue is long overdue.";

        String expectedKifString = "(exists (?long-12 ?overdue-13 ?issue-10 ?position-7 ?government-5 ?A-1 ?statement-2)\n" +
                "  (and\n" +
                "    (attribute ?overdue-13 ?long-12)\n" +
                "    (instance ?overdue-13 NormativeAttribute)\n" +
                "    (refers ?statement-2 ?position-7)\n" +
                "    (instance ?statement-2 Statement)\n" +
                "    (instance ?long-12 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?A-1 Angstrom)\n" +
                "    (instance ?issue-10 Proposition)\n" +
                "    (instance ?position-7 Entity)\n" +
                "    (instance ?government-5 Government)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testStatisticsAbout() {
        String input = "Luther includes statistics about the size of the market.";

        String expectedKifString = "(exists (?includes-2 ?Luther-1 ?market-9 ?statistics-3 ?size-6)\n" +
                "  (and\n" +
                "    (agent ?includes-2 ?Luther-1)\n" +
                "    (instance ?Luther-1 Man)\n" +
                "    (patient ?includes-2 ?statistics-3)\n" +
                "    (refers ?statistics-3 ?size-6)\n" +
                "    (instance ?includes-2 part)\n" +
                "    (instance ?statistics-3 Quantity)\n" +
                "    (instance ?market-9 FinancialTransaction)\n" +
                "    (instance ?size-6 Attribute)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testStoryAbout() {
        String input = "Pinocchio is a story about a wooden puppet who wanted to become a boy.";

        String expectedKifString = "(exists (?puppet-8 ?who-9 ?boy-14 ?story-4 ?wooden-7 ?wanted-10)\n" +
                "  (and\n" +
                "    (agent ?wanted-10 ?who-9)\n" +
                "    (attribute ?puppet-8 ?wooden-7)\n" +
                "    (instance ?story-4 Stating)\n" +
                "    (refers ?story-4 ?puppet-8)\n" +
                "    (instance ?puppet-8 Artifact)\n" +
                "    (instance ?boy-14 Male)\n" +
                "    (instance ?wooden-7 Wood)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testStorylineAbout() {
        String input = "The storyline about the popular comic-strip character is very funny.";

        String expectedKifString = "(exists (?funny-10 ?popular-5 ?character-7 ?very-9 ?storyline-2)\n" +
                "  (and\n" +
                "    (attribute ?funny-10 ?very-9)\n" +
                "    (instance ?funny-10 SubjectiveStrongPositiveAttribute)\n" +
                "    (refers ?storyline-2 ?character-7)\n" +
                "    (attribute ?character-7 ?popular-5)\n" +
                "    (instance ?character-7 CognitiveAgent)\n" +
                "    (attribute ?character-7 comic-?strip-6)\n" +
                "    (instance ?storyline-2 Proposition)\n" +
                "    (instance ?popular-5 SubjectiveWeakPositiveAttribute)\n" +
                "    (instance comic-?strip-6 SubjectiveStrongPositiveAttribute)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testSuggestionAbout() {
        String input = "The author's suggestions about embarking on a new enterprise are highly useful.";

        String expectedKifString = "(exists (?new-9 ?enterprise-10 ?useful-13 ?author-2 ?highly-12 ?suggestions-4 ?embarking-6)\n" +
                "  (and\n" +
                "    (attribute ?useful-13 ?highly-12)\n" +
                "    (instance ?suggestions-4 Requesting)\n" +
                "    (refers ?suggestions-4 ?embarking-6)\n" +
                "    (time ?embarking-6 ?enterprise-10)\n" +
                "    (attribute ?enterprise-10 ?new-9)\n" +
                "    (instance ?new-9 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?embarking-6 Motion)\n" +
                "    (instance ?useful-13 SubjectiveWeakPositiveAttribute)\n" +
                "    (instance ?highly-12 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?author-2 Profession)\n" +
                "    (instance ?enterprise-10 IntentionalProcess)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testSuspicionAbout() {
        String input = "We had our suspicions about the new drug.";

        String expectedKifString = "(exists (?drug-8 ?We-1 ?new-7 ?had-2 ?suspicions-4)\n" +
                "  (and\n" +
                "    (agent ?had-2 ?We-1)\n" +
                "    (attribute ?drug-8 ?new-7)\n" +
                "    (instance ?suspicions-4 Proposition)\n" +
                "    (patient ?had-2 ?suspicions-4)\n" +
                "    (refers ?suspicions-4 ?drug-8)\n" +
                "    (instance ?new-7 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?drug-8 BiologicallyActiveSubstance)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testTestimonyAbout() {
        String input = "In his public testimony about the service probe, Sullivan sometimes gave conflicting numbers.";

        String expectedKifString = "(exists (?numbers-14 ?serviceprobe-7 ?gave-12 ?conflicting-13 ?probe-8 ?testimony-4 ?public-3 ?Sullivan-10 ?sometimes-11)\n" +
                "  (and\n" +
                "    (agent ?gave-12 ?Sullivan-10)\n" +
                "    (attribute ?gave-12 ?sometimes-11)\n" +
                "    (instance ?serviceprobe-7 Questioning)\n" +
                "    (patient ?gave-12 ?numbers-14)\n" +
                "    (refers ?testimony-4 ?probe-8)\n" +
                "    (time ?gave-12 ?testimony-4)\n" +
                "    (attribute ?testimony-4 ?public-3)\n" +
                "    (instance ?public-3 SubjectiveAssessmentAttribute)\n" +
                "    (attribute ?numbers-14 ?conflicting-13)\n" +
                "    (instance ?numbers-14 Number)\n" +
                "    (instance ?conflicting-13 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?serviceprobe-7 FinancialTransaction)\n" +
                "    (instance ?gave-12 causes)\n" +
                "    (instance ?Sullivan-10 Man)\n" +
                "    (instance ?testimony-4 Testifying)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testTransparencyAbout() {
        String input = "There is often a lack of transparency about governmental decision-making.";

        String expectedKifString = "(exists (?often-3 ?is-2 ?transparency-7 ?governmental-9 ?There-1 ?lack-5)\n" +
                "  (and\n" +
                "    (agent ?is-2 ?lack-5)\n" +
                "    (attribute ?is-2 ?often-3)\n" +
                "    (instance decision-?making-10 Deciding)\n" +
                "    (refers ?transparency-7 decision-?making-10)\n" +
                "    (attribute decision-?making-10 ?governmental-9)\n" +
                "    (instance ?There-1 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?lack-5 needs)\n" +
                "    (instance ?transparency-7 Icon)\n" +
                "    (instance ?governmental-9 Government)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testTrialAbout() {
        String input = "The trial about the murder case became a global sensation.";

        String expectedKifString = "(exists (?global-9 ?trial-2 ?became-7 ?case-6 ?sensation-10 ?murdercase-5)\n" +
                "  (and\n" +
                "    (agent ?became-7 ?trial-2)\n" +
                "    (attribute ?sensation-10 ?global-9)\n" +
                "    (instance ?global-9 SubjectiveAssessmentAttribute)\n" +
                "    (refers ?trial-2 ?case-6)\n" +
                "    (instance ?murdercase-5 Killing)\n" +
                "    (instance ?sensation-10 Perception)\n" +
                "    (instance ?trial-2 Experimenting)\n" +
                "    (instance ?murdercase-5 CartridgeCase)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testUncertaintyAbout() {
        String input = "The uncertainty about the economic prospect looms large.";

        String expectedKifString = "(exists (?large-8 ?uncertainty-2 ?looms-7 ?prospect-6 ?economic-5)\n" +
                "  (and\n" +
                "    (agent ?looms-7 ?uncertainty-2)\n" +
                "    (attribute ?prospect-6 ?economic-5)\n" +
                "    (instance ?large-8 SubjectiveWeakPositiveAttribute)\n" +
                "    (refers ?uncertainty-2 ?prospect-6)\n" +
                "    (instance ?looms-7 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?prospect-6 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?uncertainty-2 ProbabilityRelation)\n" +
                "    (instance ?economic-5 FinancialTransaction)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testViewAbout() {
        String input = "The senator's views about the responsibility of decision makers clash with those of many other senators.";

        String expectedKifString = "(exists (?clash-11 ?decisionmakers-9 ?other-16 ?senator-2 ?responsibility-7 ?views-4 ?senators-17 ?many-15)\n" +
                "  (and\n" +
                "    (agent ?clash-11 ?views-4)\n" +
                "    (attribute ?senators-17 ?many-15)\n" +
                "    (instance ?views-4 View)\n" +
                "    (refers ?views-4 ?responsibility-7)\n" +
                "    (attribute ?senators-17 ?other-16)\n" +
                "    (instance ?senator-2 Position)\n" +
                "    (instance ?decisionmakers-9 Deciding)\n" +
                "    (instance ?senators-17 Position)\n" +
                "    (instance ?decisionmakers-9 Human)\n" +
                "    (instance ?clash-11 RadiatingSound)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testWondersAbout() {
        String input = "His wonders about the natural world stayed with him all his life.";

        String expectedKifString = "(exists (?life-12 ?wonders-2 ?natural-5 ?stayed-7 ?world-6)\n" +
                "  (and\n" +
                "    (agent ?stayed-7 ?wonders-2)\n" +
                "    (attribute ?world-6 ?natural-5)\n" +
                "    (instance ?life-12 PsychologicalProcess)\n" +
                "    (refers ?wonders-2 ?world-6)\n" +
                "    (instance ?natural-5 SubjectiveWeakPositiveAttribute)\n" +
                "    (instance ?stayed-7 PastFn)\n" +
                "    (instance ?world-6 Human)\n" +
                "    (instance ?wonders-2 desires)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testWordAbout() {
        String input = "The police chief did not give the reporters any word about the case.";

        String expectedKifString = "(exists (?give-6 ?reporters-8 ?case-13 ?word-10 ?chief-3 ?policychief-2)\n" +
                "  (and\n" +
                "    (agent ?give-6 ?chief-3)\n" +
                "    (instance ?case-13 LegalAction)\n" +
                "    (patient ?give-6 ?word-10)\n" +
                "    (refers ?word-10 ?case-13)\n" +
                "    (instance ?reporters-8 NewsReporter)\n" +
                "    (patient ?X ?reporters-8)\n" +
                "    (instance ?policychief-2 SubjectiveAssessmentAttribute)\n" +
                "    (instance ?policychief-2 Argument)\n" +
                "    (instance ?word-10 Word)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testWritingAbout() {
        String input = "I have a student writing about a zombie.";

        String expectedKifString = "(exists (?zombie-8 ?writing-5 ?have-2 ?I-1 ?student-4)\n" +
                "  (and\n" +
                "    (agent ?have-2 ?I-1)\n" +
                "    (authors ?X ?writing-5)\n" +
                "    (instance ?writing-5 Writing)\n" +
                "    (patient ?have-2 ?student-4)\n" +
                "    (refers ?writing-5 ?zombie-8)\n" +
                "    (instance ?I-1 AlphabeticCharacter)\n" +
                "    (instance ?zombie-8 Human)\n" +
                "    (instance ?student-4 student)) )";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

}
