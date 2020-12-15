package com.articulate.sigma.nlg;

import com.articulate.sigma.KB;
import com.articulate.sigma.utils.StringUtil;
import org.junit.*;

import static org.junit.Assert.assertEquals;

/**
 * LanguageFormatter tests specifically targeted toward the htmlParaphrase( ) method.
 * Class uses a mock of the KB's so that they can be run very quickly without initialization.
 */
public class HtmlParaphraseMockTest extends SigmaMockTestBase {
    private final KB kb = kbMock;

    @Test
    public void testHtmlParaphraseDriving1()     {
        String stmt =       "(exists (?D ?H)\n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H)))";

        String expectedResult = "a human drives";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDrivingNot1()     {
        String stmt =       "(not \n" +
                "               (exists (?D ?H)\n" +
                "                   (and\n" +
                "                       (instance ?D Driving)\n" +
                "                       (instance ?H Human)\n" +
                "                       (agent ?D ?H))))";



        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there don't exist a process and an agent such that the process is an instance of Driving and the agent is an instance of human and the agent is an agent of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a human doesn't drive";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDrivingHarryReified()     {
        String stmt =       "(exists (?D)\n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance Harry Human)\n" +
                "                   (agent ?D Harry)))";

        String expectedResult = "Harry drives";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDrivingHarryReifiedNot()     {
        String stmt =       "(not\n" +
                "               (exists (?D)\n" +
                "                   (and\n" +
                "                       (instance ?D Driving)\n" +
                "                       (instance Harry Human)\n" +
                "                       (agent ?D Harry))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there doesn't exist a process such that the process is an instance of Driving and Harry is an instance of human and Harry is an agent of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "Harry doesn't drive";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDriving2()     {
        String stmt =   "(exists (?D ?H)\n" +
                "           (and\n" +
                "           (instance ?D Driving)\n" +
                "           (instance ?H Human)\n" +
                "           (names \"John\" ?H)\n" +
                "           (agent ?D ?H)))";

        //String expectedResult = "there exist a process and an agent such that the process is an instance of Driving and the agent is an instance of human and the agent has name \"John\" and the agent is an agent of the process";
        String expectedResult = "John drives";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDrivingNot2()     {
        String stmt =   "(not\n" +
                "           (exists (?D ?H)\n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (names \"John\" ?H)\n" +
                "                   (agent ?D ?H))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there don't exist a process and an agent such that the process is an instance of Driving and the agent is an instance of human and the agent has name \"John\" and the agent is an agent of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        //expectedResult = "there don't exist a process and an agent such that the process is an instance of Driving and the agent is an instance of human and the agent has name \"John\" and the agent is an agent of the process";
        expectedResult = "John doesn't drive";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "A human travels to (the) Sudan."
     */
    @Test
    public void testHumanTravels()     {
        String stmt =   "(exists (?he ?event)\n" +
                "                  (and\n" +
                "                    (instance ?event Transportation)\n" +
                "                    (instance ?he Human)\n" +
                "                    (agent ?event ?he)))";

        String expectedResult = "a human performs a transportation";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "A human travels to (the) Sudan."
     */
    @Test
    public void testHumanTravelsSudan()     {
        String stmt =   "(exists (?he ?event)\n" +
                "                  (and\n" +
                "                    (instance ?event Transportation)\n" +
                "                    (instance ?he Human)\n" +
                "                    (agent ?event ?he)\n" +
                "                    (destination ?event Sudan)))";

        String expectedResult = "a human performs a transportation to Sudan";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "He travels to (the) Sudan."
     */
    @Test
    public void testHeTravelsSudan()     {
        String stmt =   "(exists (?he ?event)\n" +
                "                  (and\n" +
                "                    (instance ?event Transportation)\n" +
                "                    (attribute ?he Male)\n" +
                "                    (instance ?he Human)\n" +
                "                    (agent ?event ?he)\n" +
                "                    (destination ?event Sudan)))";

        String expectedResult = "a male human performs a transportation to Sudan";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "Bell created the telephone."; also "The telephone was created by Bell."
     */
    @Test
    @Ignore
    public void testHtmlParaphraseBellCreateTelephone()     {
        String stmt =   "(exists \n" +
                "              (?event ?telephone) \n" +
                "              (and \n" +
                "                (instance Bell Human) \n" +
                "                (agent ?event Bell) \n" +
                "                (instance ?event Process) \n" +
                "                (instance ?telephone Telephone) \n" +
                "                (patient ?event ?telephone)))";

        String expectedResult = "Bell performs a process a telephone";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "Blankenship created the telephone."; also "The telephone was created by Blankenship."
     */
    @Test
    public void testHtmlParaphraseBlankenshipCreateTelephone()     {
        String stmt =   "(exists \n" +
                "              (?event ?telephone) \n" +
                "              (and \n" +
                "                (instance Blankenship Human) \n" +
                "                (agent ?event Blankenship) \n" +
                "                (instance ?event Process) \n" +
                "                (instance ?telephone Telephone) \n" +
                "                (patient ?event ?telephone)))";

        String expectedResult = "Blankenship performs a process on a telephone";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "Bob eats and drinks on the desk."
     */
    @Test
    public void testHtmlParaphraseBobEatsDrinksDesk()     {
        String stmt =   "(exists \n" +
                "              (?desk ?event1 ?event2) \n" +
                "              (and \n" +
                "                   (attribute Robert-1 Male) \n" +
                "                   (exists \n" +
                "                       (?location) \n" +
                "                       (and \n" +
                "                           (located ?event2 ?location) \n" +
                "                           (orientation ?location ?desk On))) \n" +
                "                   (instance Robert-1 Human)     \n" +
                "                   (instance ?desk Desk) \n" +
                "                   (agent ?event1 Robert-1) \n" +
                "                   (instance ?event1 Eating) \n" +
                "                   (agent ?event2 Robert-1) \n" +
                "                   (instance ?event2 Drinking)))";

        String expectedResult = "there exist an entity, a process and another process such that male is an attribute of Robert-1 and there exists another entity such that the other process is located at the other entity and the other entity is On to the entity and Robert-1 is an instance of human and the entity is an instance of Desk and Robert-1 is an agent of the process and the process is an instance of Eating and Robert-1 is an agent of the other process and the other process is an instance of Drinking";

        String actualResult = NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "If Mary gives John a book then he reads it."
     */
    @Test
    public void testHtmlParaphraseIfMaryGivesBookJohnThenHeReads()     {
        String stmt =   "(forall \n" +
                "              (?book ?event1) \n" +
                "              (=> \n" +
                "                (and \n" +
                "                  (attribute John-1 Male) \n" +
                "                  (attribute Mary-1 Female) \n" +
                "                  (instance John-1 Human) \n" +
                "                  (instance Mary-1 Human) \n" +
                "                  (instance ?book Book) \n" +
                "                  (agent ?event1 Mary-1) \n" +
                "                  (destination ?event1 John-1) \n" +
                "                  (instance ?event1 Giving) \n" +
                "                  (patient ?event1 ?book)) \n" +
                "                (exists \n" +
                "                  (?event2) \n" +
                "                  (and \n" +
                "                       (attribute John-1 Male) \n" +
                "                       (instance John-1 Human) \n" +
                "                       (instance ?book Object) \n" +
                "                       (agent ?event2 John-1) \n" +
                "                       (instance ?event2 Reading) \n" +
                "                       (patient ?event2 ?book)))))";

        String expectedResult = "if female Mary-1 gives a book to male John-1, then male John-1 reads the book";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "If Mary gives John a book then he doesn't read it."
     */
    @Test
    public void testHtmlParaphraseIfMaryGivesBookJohnThenHeReadsNot()     {
        String stmt =   "(forall \n" +
                "              (?book ?event1) \n" +
                "              (=> \n" +
                "                (and \n" +
                "                  (attribute John-1 Male) \n" +
                "                  (attribute Mary-1 Female) \n" +
                "                  (instance John-1 Human) \n" +
                "                  (instance Mary-1 Human) \n" +
                "                  (instance ?book Book) \n" +
                "                  (agent ?event1 Mary-1) \n" +
                "                  (destination ?event1 John-1) \n" +
                "                  (instance ?event1 Giving) \n" +
                "                  (patient ?event1 ?book)) \n" +
                "                (not \n" +
                "                   (exists \n" +
                "                       (?event2) \n" +
                "                       (and \n" +
                "                           (attribute John-1 Male) \n" +
                "                           (instance John-1 Human) \n" +
                "                           (instance ?book Object) \n" +
                "                           (agent ?event2 John-1) \n" +
                "                           (instance ?event2 Reading) \n" +
                "                           (patient ?event2 ?book))))))";


        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "for all an entity and a process if male is an attribute of John-1 and female is an attribute of Mary-1 and John-1 is an instance of human and Mary-1 is an instance of human and the entity is an instance of Book and Mary-1 is an agent of the process and the process ends at John-1 and the process is an instance of Giving and the entity is a patient of the process, then there doesn't exist another process such that male is an attribute of John-1 and John-1 is an instance of human and the entity is an instance of Object and John-1 is an agent of the other process and the other process is an instance of Reading and the entity is a patient of the other process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "if female Mary-1 doesn't give a book to male John-1, then male John-1 doesn't read the book";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDrivingThenSeeingIf()     {
        String stmt =       "(=> \n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H))\n" +
                "               (exists (?S)\n" +
                "                   (and\n" +
                "                       (instance ?S Seeing)\n" +
                "                       (agent ?S ?H))))";

        String expectedResult = "if a human drives, then the human performs a seeing";

        String actualResult = NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * We use this to test how much the antecedent and the consequent affect each other.
     */
    @Test
    public void testHtmlParaphraseDrivingThenTransportedIf()     {
        String stmt =       "(=> \n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H))\n" +
                "               (transported ?D ?H))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "if a process is an instance of Driving and an agent is an instance of human and the agent is an agent of the process, then the agent is transported during the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "if a process is an instance of Driving and an agent is an instance of human and the agent is an agent of the process, then the agent is transported during the process";

        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testDrivingToCleanCity()     {
        String stmt =   "(exists \n" +
                "              (?agent ?city ?event) \n" +
                "              (and \n" +
                "                (instance ?agent Agent) \n" +
                "                (instance ?city City) \n" +
                "                (instance ?event Driving) \n" +
                "                (agent ?event ?agent) \n" +
                "                (destination ?event ?city) \n" +
                "                (attribute ?city Clean)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an agent, an entity and a process such that the agent is an instance of agent and the entity is an instance of city and the process is an instance of Driving and the agent is an agent of the process and the process ends at the entity and clean is an attribute of the entity";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "an agent drives to a clean city";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "The waiter pours soup into the bowl."
     */
    @Test
    @Ignore
    public void testWaiterGivesTeaDoctor()     {
        String stmt =   "(exists \n" +
                "              (?doctor ?event ?tea ?waiter) \n" +
                "              (and \n" +
                "                (instance ?doctor MedicalDoctor) \n" +
                "                (instance ?event Giving) \n" +
                "                (instance ?tea Tea) \n" +
                "                (attribute ?waiter ServicePosition) \n" +
                "                (destination ?event ?doctor) \n" +
                "                (patient ?event ?tea) \n" +
                "                (agent ?event ?waiter)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an entity, a process, , , another entity and an agent such that the entity is an instance of medical doctor and the process is an instance of Giving and the other entity is an instance of tea and ServicePosition is an attribute of the agent and the process ends at the entity and the other entity is a patient of the process and the agent is an agent of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a kind of ServicePosition agent gives tea to a medical doctor";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }


}