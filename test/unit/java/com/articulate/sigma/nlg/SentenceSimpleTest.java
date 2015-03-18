package com.articulate.sigma.nlg;

import com.articulate.sigma.KB;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

// Tests on SumoProcess that do not require KBs be loaded.

public class SentenceSimpleTest extends SigmaMockTestBase {

    private final KB knowledgeBase = this.kbMock;

    private final Multimap<String, SumoProcessEntityProperty> entityProperties = HashMultimap.create();

    @Test
    public void testFormulateNaturalDirectObject() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "patient", "Driving", "Automobile");

        String expected = "an automobile";
        Sentence sentence = new Sentence(process.createNewRoleScratchPad(), process.getSumoProcess(), knowledgeBase, entityProperties);
        sentence.formulateNaturalDirectObject();
        assertEquals(expected, sentence.getDirectObject().getSurfaceForm());

        process.addRole("patient", "Truck");
        sentence.setCaseRolesScratchpad(process.createNewRoleScratchPad());
        sentence.setDirectObject(new SVOElement(SVOElement.SVOGrammarPosition.DIRECT_OBJECT));
        expected = "an automobile and a truck";
        sentence.formulateNaturalDirectObject();
        assertEquals(expected, sentence.getDirectObject().getSurfaceForm());

        process.addRole("patient", "Taxi");
        sentence.setCaseRolesScratchpad(process.createNewRoleScratchPad());
        sentence.setDirectObject(new SVOElement(SVOElement.SVOGrammarPosition.DIRECT_OBJECT));
        expected = "an automobile, a taxi and a truck";
        sentence.formulateNaturalDirectObject();
        assertEquals(expected, sentence.getDirectObject().getSurfaceForm());
    }

    /**
     * Verifies that, at least for the time being, the case of "Entity" participants is not changed, and we do not precede
     * it with a definite article.
     */
    @Test
    public void testFormulateNaturalDirectObjectEntity() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "patient", "Driving", "Entity");

        String expected = "Entity";
        Sentence sentence = new Sentence(process.createNewRoleScratchPad(), process.getSumoProcess(), knowledgeBase, entityProperties);
        sentence.formulateNaturalDirectObject();
        assertEquals(expected, sentence.getDirectObject().getSurfaceForm());
    }

    @Test
    public void testFormulateNaturalSubject() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "Human");

        String expected = "a human";
        Sentence sentence = new Sentence(process.createNewRoleScratchPad(), process.getSumoProcess(), knowledgeBase, entityProperties);
        sentence.formulateNaturalSubject();
        assertEquals(expected, sentence.getSubject().getSurfaceForm());

        process.addRole("agent", "Coffee");
        expected = "coffee and a human";
        sentence.setCaseRolesScratchpad(process.createNewRoleScratchPad());
        sentence.formulateNaturalSubject();
        assertEquals(expected, sentence.getSubject().getSurfaceForm());

        process.addRole("agent", "MedicalDoctor");
        expected = "coffee, a human and a medical doctor";
        sentence.setCaseRolesScratchpad(process.createNewRoleScratchPad());
        sentence.formulateNaturalSubject();
        assertEquals(expected, sentence.getSubject().getSurfaceForm());
    }

    @Test
    public void testFormulateNaturalVerbNotInWordNet() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Transportation", "Maria");

        Sentence sentence = new Sentence(process.createNewRoleScratchPad(), process.getSumoProcess(), knowledgeBase, entityProperties);
        sentence.formulateNaturalSubject();
        sentence.formulateNaturalVerb();
        assertEquals("Maria", sentence.getSubject().getSurfaceForm());
        assertEquals("performs", sentence.getVerb().getSurfaceForm());
        assertEquals("a transportation", sentence.getDirectObject().getSurfaceForm());
    }

    @Test
    public void testToNaturalLanguageSimple() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "Maria");
        process.addRole("patient", "Automobile");
        process.addRole("instrument", "Telephone");
        Sentence sentence = new Sentence(process.createNewRoleScratchPad(), process.getSumoProcess(), knowledgeBase, entityProperties);

        String expected = "Maria drives an automobile with a telephone";
        String actual = sentence.toNaturalLanguage();
        assertEquals(expected, actual);
    }

    @Test
    public void testToNaturalLanguageSimpleDoubleIndirectObject() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "Maria");
        process.addRole("patient", "Automobile");
        process.addRole("instrument", "Telephone");
        process.addRole("destination", "Albany");
        Sentence sentence = new Sentence(process.createNewRoleScratchPad(), process.getSumoProcess(), knowledgeBase, entityProperties);

        String expected = "Maria drives an automobile to Albany with a telephone";
        String actual = sentence.toNaturalLanguage();
        assertEquals(expected, actual);
    }

    @Test
    public void testToNaturalLanguagePlurals() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "Maria");
        process.addRole("agent", "MedicalDoctor");
        process.addRole("patient", "Automobile");
        process.addRole("patient", "Truck");
        process.addRole("instrument", "Telephone");
        process.addRole("instrument", "Bell");
        process.addRole("destination", "Albany");
        Sentence sentence = new Sentence(process.createNewRoleScratchPad(), process.getSumoProcess(), knowledgeBase, entityProperties);

        String expected = "Maria and a medical doctor drive an automobile and a truck to Albany with a bell and a telephone";
        String actual = sentence.toNaturalLanguage();
        assertEquals(expected, actual);
    }

    @Test
    public void testToNaturalLanguageTriplePlurals() {
        SumoProcessCollector process = new SumoProcessCollector(knowledgeBase, "agent", "Driving", "Maria");
        process.addRole("agent", "MedicalDoctor");
        process.addRole("agent", "Suzy");
        process.addRole("patient", "Automobile");
        process.addRole("patient", "Taxi");
        process.addRole("patient", "Truck");
        process.addRole("instrument", "Telephone");
        process.addRole("instrument", "Bell");
        process.addRole("instrument", "Book");
        process.addRole("destination", "Albany");
        Sentence sentence = new Sentence(process.createNewRoleScratchPad(), process.getSumoProcess(), knowledgeBase, entityProperties);

        String expected = "Maria, a medical doctor and Suzy drive an automobile, a taxi and a truck to Albany with a bell, a book and a telephone";
        String actual = sentence.toNaturalLanguage();
        assertEquals(expected, actual);
    }

}