package com.articulate.sigma;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

// Tests on SumoProcess require KBs be loaded.

public class SumoProcessTest extends UnitTestBase {

    protected KB knowledgeBase = SigmaTestBase.kb;

    @Test
    public void testNaturalLanguageDrivingPatient() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "Mark");
        process.addRole("patient", "Human");

        String actual = process.toNaturalLanguage();
        String expected = "Mark drives a human";
        assertEquals(expected, actual);
    }

    @Test
    public void testNaturalLanguageDrivingGoal() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "Mark");
        process.addRole("goal", "HospitalBuilding");

        String actual = process.toNaturalLanguage();
        // FIXME: String expected = "Mark drives to the hospital.";
        String expected = "Mark drives";
        assertEquals(expected, actual);
    }

    @Test
    public void testNaturalLanguagePerformsIntentionalProcess() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "IntentionalProcess", "Mark");

        String actual = process.toNaturalLanguage();
        String expected = "Mark performs an intentional process";
        assertEquals(expected, actual);
    }

}