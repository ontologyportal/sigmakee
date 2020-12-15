package com.articulate.sigma.nlg;

import com.articulate.sigma.SigmaTestBase;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.UnitTestBase;
import org.junit.*;

import static org.junit.Assert.assertEquals;

/**
 * LanguageFormatter tests specifically targeted toward the htmlParaphrase( ) method.
 */
public class HtmlParaphraseTest extends UnitTestBase {

    @Test
    public void testHtmlParaphraseDomainDatePhysical()     {
        String stmt = "(domain date 1 Physical)";

        String expectedResult = "the number 1 argument of date is an instance of physical";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Currently the output is not very good.
     * Better output: If RELATION is an instance of object attitude and the RELATION of another entity to a third entity, then the third
     * entity is an instance of physical.
     * Ideal output: Relations that are instances of object attitude have as their second argument instances that are physical things.
     */
    @Test
    public void testHtmlParaphraseInstanceRELObjectAttitude()     {
        String stmt = "(=> (and (instance ?REL ObjectAttitude) (?REL ?AGENT ?THING)) (instance ?THING Physical))";

        String expectedResult = "if an entity is an instance of object attitude and the entity another entity and a third entity, " +
                "then the third entity is an instance of physical";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseSubstanceAttributePhysicalState()     {
        String stmt = "(<=> (instance ?OBJ Substance) (exists (?ATTR) (and (instance ?ATTR PhysicalState) (attribute ?OBJ ?ATTR))))";

        String expectedResult = "an object is an instance of substance if and only if there exists an entity such that " +
                "the entity is an instance of physical state and the entity is an attribute of the object";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseBiologicallyActiveSubstance()     {
        String stmt = "(subclass BiologicallyActiveSubstance Substance)";

        String expectedResult = "biologically active substance is a subclass of substance";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphrasePureSubstanceMixture()     {
        String stmt = "(partition Substance PureSubstance Mixture)";

        String expectedResult = "substance is exhaustively partitioned into pure substance and mixture";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal output: Should have a space after the comma.
     */
    @Test
    public void testHtmlParaphrasePartlyLocated()     {
        String stmt = "(=> (and (instance ?OBJ1 Object) (partlyLocated ?OBJ1 ?OBJ2)) (exists (?SUB) (and (part ?SUB ?OBJ1) (located ?SUB ?OBJ2))))";

        String expectedResult = "if an object is an instance of object and the object is partly located in another object, then there exists a third object such that the third object is a part of the object and the third object is located at the other object";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal output: Correct the spacing.
     */
    @Test
    public void testHtmlParaphraseDefinePhysical()     {
        String stmt = "(<=> (instance ?PHYS Physical) (exists (?LOC ?TIME) (and (located ?PHYS ?LOC) (time ?PHYS ?TIME))))";

        String expectedResult = "a physical is an instance of physical if and only if there exist an object and " +
                "a time position such that the physical is located at the object and the physical exists during the time position";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    // It seems this is not a correct SUO-Kif expression since only instances can be arguments in a case role.
    // Should this test therefore be removed?
    @Test
    public void testHtmlParaphrasePatient()     {
        String stmt = "( patient Leaving ?ENTITY )";

        String expectedResult = "an entity is a patient of leaving";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseNames()     {
        String stmt = "(names \"John\" ?H)";

        String expectedResult = "an entity has name \"John\"";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseSubclassNot()     {
        String stmt = "(not (subclass ?X Animal))";

        String expectedResult = "a set or class is not a subclass of animal";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseNamesNot()     {
        String stmt = "(not (names \"John\" ?H))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "an entity doesn't have name \"John\"";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "an entity doesn't have name \"John\"";
        actualResult = languageFormatter.htmlParaphrase("");
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

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there don't exist a process and an agent such that the process is an instance of driving and the agent is an instance of human and the agent is an agent of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a human doesn't drive";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDrivingNot2()     {
        String stmt =       "(not\n" +
                "               (exists (?D)\n" +
                "                   (and\n" +
                "                       (instance ?D Driving)\n" +
                "                       (agent ?D John))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there doesn't exist a process such that the process is an instance of driving and John is an agent of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "John doesn't drive";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDriving1()     {
        String stmt =       "(exists (?D ?H)\n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist a process and an agent such that the process is an instance of driving and " +
                "the agent is an instance of human and the agent is an agent of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a human drives";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDriving1If()     {
        String stmt =       "(=> \n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H))\n" +
                "               (exists (?B)\n" +
                "                   (and\n" +
                "                       (instance ?B Breathing)\n" +
                "                       (agent ?B ?H))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "if a process is an instance of driving and an agent is an instance of human and the agent is an agent of the process, then there exists another process such that the other process is an instance of breathing and the agent is an agent of the other process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "if a human drives, then the human breathes";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDriving1IfAndOnlyIf()     {
        String stmt =       "(<=> \n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H))\n" +
                "               (exists (?B)\n" +
                "                   (and\n" +
                "                       (instance ?B Breathing)\n" +
                "                       (agent ?B ?H))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "a process is an instance of driving and an agent is an instance of human and the agent is an agent of the process if and only if there exists another process such that the other process is an instance of breathing and the agent is an agent of the other process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a human drives if and only if the human breathes";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseJohnDriving()     {
        String stmt =   "(exists (?D ?H)\n" +
                "           (and\n" +
                "           (instance ?D Driving)\n" +
                "           (instance ?H Human)\n" +
                "           (names \"John\" ?H)\n" +
                "           (agent ?D ?H)))";


        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist a process and an agent such that the process is an instance of driving and " +
                "the agent is an instance of human and the agent has name \"John\" and the agent is an agent of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "John drives";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseJohnDrivingNot1()     {
        String stmt =   "(not\n" +
                "           (exists (?D ?H)\n" +
                "               (and\n" +
                "               (instance ?D Driving)\n" +
                "               (instance ?H Human)\n" +
                "               (names \"John\" ?H)\n" +
                "               (agent ?D ?H))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there don't exist a process and an agent such that the process is an instance of driving and the agent is an instance of human and the agent has name \"John\" and the agent is an agent of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        // FIXME: perhaps this is a better rendering of the formula:
        //expectedResult = "No one named John drives.";
        expectedResult = "John doesn't drive";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseJohnDrivingCar()     {
        String stmt =   "(exists (?D ?H ?Car)\n" +
                "           (and\n" +
                "           (instance ?D Driving)\n" +
                "           (instance ?H Human)\n" +
                "           (names \"John\" ?H)\n" +
                "           (instance ?Car Automobile)\n" +
                "           (agent ?D ?H)\n" +
                "           (patient ?D ?Car)))";


        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist a process, an agent and an entity such that the process is an instance of driving and " +
                "the agent is an instance of human and the agent has name \"John\" and " +
                "the entity is an instance of automobile and the agent is an agent of the process and " +
                "the entity is a patient of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "John drives an automobile";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseHumanDrivingCar()     {
        String stmt =   "(exists (?D ?H ?Car)\n" +
                "           (and\n" +
                "           (instance ?D Driving)\n" +
                "           (instance ?H Human)\n" +
                "           (instance ?Car Automobile)\n" +
                "           (agent ?D ?H)\n" +
                "           (patient ?D ?Car)))";

        String expectedResult = "a human drives an automobile";

        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseSubclassIf()     {
        String stmt =   "(=> " +
                "           (subclass ?Cougar Feline) " +
                "           (subclass ?Cougar Carnivore))";

        String expectedResult = "if a set or class is a subclass of feline, then the set or class is a subclass of carnivore";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseSubclassMonthFn()     {
        String stmt =   "(exists (?M) " +
                "           (time JohnsBirth (MonthFn ?M (YearFn 2000))))";

        String expectedResult = "there exists a kind of month such that JohnsBirth exists during the month a kind of month";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * See what happens when you call htmlParaphrase( ) with a syntactically incorrect statement.
     * TODO: Perhaps this test should expect an exception, but currently the exception is being swallowed--see the output
     * in the console when you run this test.
     */
    @Test
    public void testWrongNbrParens()     {
        System.out.println("\nAbout to perform unit test that throws an IndexOutOfBoundsException.");
        System.out.flush();
        String stmt =   "(=> " +
                                // The next line has too many right parens.
                "               (instance (GovernmentFn ?Place) StateGovernment)) " +
                "               (instance ?Place StateOrProvince)) ";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        String expectedResult = "";
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
        System.out.println("Finished performing unit test that throws an IndexOutOfBoundsException.");
        System.out.flush();
    }

    @Test
    public void testHtmlParaphraseTypesGovFnIf()     {
        String stmt =   "(=> " +
                "           (instance (GovernmentFn ?Place) StateGovernment) " +
                "           (instance ?Place StateOrProvince)) ";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        String expectedResult = "if the government of a geopolitical area is an instance of state government, then the geopolitical area is an instance of state or province";
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseElementSetIf()     {
        String stmt =   "(=> " +
                "           (forall (?ELEMENT) " +
                "               (<=> " +
                "                   (element ?ELEMENT ?SET1) " +
                "                   (element ?ELEMENT ?SET2))) " +
                "           (equal ?SET1 ?SET2))";

        String expectedResult = "if for all an entity the entity is an element of a set if and only if the entity is an element of another set, " +
                "then the set is equal to the other set";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "The document was not classified as top secret before 2001."
     */
    @Test
    public void testHtmlParaphraseNotClassifiedBefore()     {
        String stmt =   "(not \n" +
                "              (exists \n" +
                "                (?agent ?document ?event) \n" +
                "                (and \n" +
                "                  (holdsDuring \n" +
                "                  (EndFn \n" +
                "                    (WhenFn ?event)) \n" +
                "                  (attribute ?document USTopSecret)) \n" +
                "                  (lessThan \n" +
                "                  (BeginFn \n" +
                "                    (WhenFn \n" +
                "                      (attribute ?document USTopSecret))) \n" +
                "                  (BeginFn \n" +
                "                    (YearFn 2001))) \n" +
                "                  (instance ?agent Agent) \n" +
                "                  (instance ?document FactualText) \n" +
                "                  (instance ?event Classifying) \n" +
                "                  (agent ?event ?agent) \n" +
                "                  (patient ?event ?document))))";

        String expectedResult = "there don't exist an agent, an object and a process such that USTopSecret is an attribute of the object holds during the end of the time of existence of the process and the beginning of the time of existence of USTopSecret is an attribute of the object is less than the beginning of the year 2001 and the agent is an instance of agent and the object is an instance of factual text and the process is an instance of classifying and the agent is an agent of the process and the object is a patient of the process";

        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "The document was classified as top secret before 2001."
     */
    @Test
    public void testHtmlParaphraseClassifiedBefore()     {
        String stmt =   "(exists \n" +
                "              (?agent ?document ?event) \n" +
                "              (and \n" +
                "                (holdsDuring \n" +
                "                  (EndFn \n" +
                "                  (WhenFn ?event)) \n" +
                "                  (attribute ?document USTopSecret)) \n" +
                "                (lessThan \n" +
                "                  (BeginFn \n" +
                "                  (WhenFn \n" +
                "                    (attribute ?document USTopSecret))) \n" +
                "                  (BeginFn \n" +
                "                  (YearFn 2001))) \n" +
                "                (instance ?agent Agent) \n" +
                "                (instance ?document FactualText) \n" +
                "                (instance ?event Classifying) \n" +
                "                (agent ?event ?agent) \n" +
                "                (patient ?event ?document)))";

        String expectedResult = "there exist an agent, an object and a process such that USTopSecret is an attribute of the object holds during the end of the time of existence of the process and the beginning of the time of existence of USTopSecret is an attribute of the object is less than the beginning of the year 2001 and the agent is an instance of agent and the object is an instance of factual text and the process is an instance of classifying and the agent is an agent of the process and the object is a patient of the process";

        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "Bob sent a card."; also "A card was sent by Bob."
     */
    @Test
    public void testHtmlParaphraseBobSendCard()     {
        String stmt =   "(exists\n" +
                "              (?card ?event)\n" +
                "              (and\n" +
                "                (instance Mary-1 Human)\n" +
                "                (instance Robert-1 Human)\n" +
                "                (instance ?card BankCard)\n" +
                "                (agent ?event Robert-1)\n" +
                "                (instance ?event Directing)\n" +
                "                (patient ?event ?card)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an entity and a process such that Mary-1 is an instance of human and Robert-1 is an instance of human and the entity is an instance of bank card and Robert-1 is an agent of the process and the process is an instance of directing and the entity is a patient of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "Robert-1 directs a bank card";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "Bob sent a card to Mary."; also "A card was sent to Mary by Bob."
     */
    @Test
    public void testHtmlParaphraseBobSendCardMary()     {
        String stmt =   "(exists\n" +
                "              (?card ?event)\n" +
                "              (and\n" +
                "                (instance Mary-1 Human)\n" +
                "                (instance Robert-1 Human)\n" +
                "                (instance ?card BankCard)\n" +
                "                (agent ?event Robert-1)\n" +
                "                (destination ?event Mary-1)\n" +
                "                (instance ?event Directing)\n" +
                "                (patient ?event ?card)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an entity and a process such that Mary-1 is an instance of human and Robert-1 is an instance of human and the entity is an instance of bank card and Robert-1 is an agent of the process and the process ends at Mary-1 and the process is an instance of directing and the entity is a patient of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "Robert-1 directs a bank card to Mary-1";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * ideal: Bob sends a bank card to Mary with a/by pigeon.
     */
    @Test
    public void testHtmlParaphraseBobSendCardMaryWithPigeon()     {
        String stmt =   "(exists\n" +
                "              (?card ?event)\n" +
                "              (and\n" +
                "                (instance Mary-1 Human)\n" +
                "                (instance Robert-1 Human)\n" +
                "                (instance ?card BankCard)\n" +
                "                (instance ?bird Pigeon)\n" +
                "                (agent ?event Robert-1)\n" +
                "                (destination ?event Mary-1)\n" +
                "                (instance ?event Directing)\n" +
                "                (instrument ?event ?bird)\n" +
                "                (patient ?event ?card)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an entity and a process such that Mary-1 is an instance of human and Robert-1 is an instance of human and the entity is an instance of bank card and an object is an instance of pigeon and Robert-1 is an agent of the process and the process ends at Mary-1 and the process is an instance of directing and the object is an instrument for the process and the entity is a patient of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "Robert-1 directs a bank card to Mary-1 with a pigeon";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "The man Bob sent a card to the woman Mary."; also "A card was sent to Mary by Bob."
     */
    @Test
    public void testHtmlParaphraseManBobSendCardWomanMary()     {
        String stmt =   "(exists\n" +
                "              (?card ?event)\n" +
                "              (and\n" +
                "                (attribute Mary-1 Female)\n" +
                "                (attribute Robert-1 Male)\n" +
                "                (instance Mary-1 Human)\n" +
                "                (instance Robert-1 Human)\n" +
                "                (instance ?card BankCard)\n" +
                "                (agent ?event Robert-1)\n" +
                "                (destination ?event Mary-1)\n" +
                "                (instance ?event Directing)\n" +
                "                (patient ?event ?card)))";


        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an entity and a process such that female is an attribute of Mary-1 and male is an attribute of Robert-1 and Mary-1 is an instance of human and Robert-1 is an instance of human and the entity is an instance of bank card and Robert-1 is an agent of the process and the process ends at Mary-1 and the process is an instance of directing and the entity is a patient of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "male Robert-1 directs a bank card to female Mary-1";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "A city was built."
     */
    @Test
    public void testHtmlParaphraseCityBeBuilt()     {
        String stmt =   "(exists \n" +
                "              (?agent ?city ?event) \n" +
                "              (and \n" +
                "                (instance ?agent Agent) \n" +
                "                (instance ?city City) \n" +
                "                (instance ?event Making) \n" +
                "                (agent ?event ?agent) \n" +
                "                (patient ?event ?city)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an agent, an entity and a process such that the agent is an instance of agent and the entity is an instance of city and the process is an instance of making and the agent is an agent of the process and the entity is a patient of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "an agent makes a city";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "The city was built."
     */
    @Test
    public void testHtmlParaphraseCityBeBuiltWithMachine()     {
        String stmt =   "(exists \n" +
                "              (?agent ?city ?event) \n" +
                "              (and \n" +
                "                (instance ?agent Agent) \n" +
                "                (instance ?city City) \n" +
                "                (instance ?event Making) \n" +
                "                (agent ?event ?agent) \n" +
                "                (patient ?event ?city)\n" +
                "                (instance Machine-1 Machine)\n" +
                "                (instrument ?event Machine-1)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an agent, an entity and a process such that the agent is an instance of agent and the entity is an instance of city and the process is an instance of making and the agent is an agent of the process and the entity is a patient of the process and Machine-1 is an instance of machine and Machine-1 is an instrument for the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "an agent makes a city with Machine-1";
        actualResult = languageFormatter.htmlParaphrase("");
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
                "                (attribute Robert-1 Male) \n" +
                "                (exists \n" +
                "                  (?location) \n" +
                "                  (and \n" +
                "                  (located ?event2 ?location) \n" +
                "                  (orientation ?location ?desk On))) \n" +
                "                (instance Robert-1 Human)     \n" +
                "                (instance ?desk Desk) \n" +
                "                (agent ?event1 Robert-1) \n" +
                "                (instance ?event1 Eating) \n" +
                "                (agent ?event2 Robert-1) \n" +
                "                (instance ?event2 Drinking)))";

        String expectedResult = "there exist an object, a process and another process such that male is an attribute of Robert-1 and there exists another object such that the other process is located at the other object and the other object is on to the object and Robert-1 is an instance of human and the object is an instance of desk and Robert-1 is an agent of the process and the process is an instance of eating and Robert-1 is an agent of the other process and the other process is an instance of drinking";

        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "If John sees a hamburger then he wants it."
     */
    @Test
    public void testHtmlParaphraseIfJohnSeeHamburgerThenWants()     {
        String stmt =   "(forall \n" +
                "              (?event ?hamburger) \n" +
                "              (=> \n" +
                "                (and \n" +
                "                  (attribute John-1 Male) \n" +
                "                  (instance John-1 Human) \n" +
                "                  (experiencer ?event John-1) \n" +
                "                  (instance ?event Seeing) \n" +
                "                  (instance ?hamburger Food) \n" +
                "                  (patient ?event ?hamburger)) \n" +
                "                (and \n" +
                "                  (attribute John-1 Male) \n" +
                "                  (instance John-1 Human) \n" +
                "                  (instance ?hamburger Object) \n" +
                "                  (wants John-1 ?hamburger))))";

        String expectedResult = "for all a process and a physical if male is an attribute of John-1 and John-1 is an instance of human and John-1 experiences the process and the process is an instance of seeing and the physical is an instance of Food and the physical is a patient of the process, then male is an attribute of John-1 and John-1 is an instance of human and the physical is an instance of object and John-1 wants the physical";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "John owns a dog."
     */
    @Test
    public void testJohnOwnsDog()     {
        String stmt =   "(exists (?dog)\n" +
                "              (and\n" +
                "               (instance ?dog Canine)\n" +
                "               (instance John-1 Human)\n" +
                "               (attribute John-1 Male)\n" +
                "               (possesses John-1 ?dog)))";

        String expectedResult = "there exists an object such that the object is an instance of canine and John-1 is an instance of human and male is an attribute of John-1 and John-1 possesses the object";
        String actualResult = NLGUtils.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "John gives the bank card to Mary."
     */
    @Test
    public void testJohnGivesCardMary()     {
        String stmt =   "(exists (?card ?event)\n" +
                "              (and\n" +
                "               (instance ?event Giving)\n" +
                "               (instance John-1 Human)\n" +
                "               (agent ?event John-1)\n" +
                "               (instance ?card BankCard)\n" +
                "               (patient ?event ?card)\n" +
                "               (instance Mary-1 Human)\n" +
                "               (destination ?event Mary-1)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an entity and a process such that the process is an instance of giving and John-1 is an instance of human and John-1 is an agent of the process and the entity is an instance of bank card and the entity is a patient of the process and Mary-1 is an instance of human and the process ends at Mary-1";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "John-1 gives a bank card to Mary-1";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "The man John gives the card to Mary."
     */
    @Test
    public void testManJohnGivesCardWomanMary()     {
        String stmt =   "(exists (?card ?event)\n" +
                "              (and\n" +
                "               (instance ?event Giving)\n" +
                "               (attribute John-1 Male)\n" +
                "               (instance John-1 Human)\n" +
                "               (agent ?event John-1)\n" +
                "               (instance ?card BankCard)\n" +
                "               (patient ?event ?card)\n" +
                "               (attribute Mary-1 Female)\n" +
                "               (instance Mary-1 Human)\n" +
                "               (destination ?event Mary-1)))";


        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an entity and a process such that the process is an instance of giving and male is an attribute of John-1 and John-1 is an instance of human and John-1 is an agent of the process and the entity is an instance of bank card and the entity is a patient of the process and female is an attribute of Mary-1 and Mary-1 is an instance of human and the process ends at Mary-1";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "male John-1 gives a bank card to female Mary-1";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "The oldest dog enters the bank."
     */
    @Test
    public void testOldestDogEntersBank()     {
        String stmt =   "(exists \n" +
                "                  (?bank ?dog ?event) \n" +
                "                  (and \n" +
                "                    (forall \n" +
                "                      (?X) \n" +
                "                      (=> \n" +
                "                        (and \n" +
                "                          (instance ?X Canine) \n" +
                "                          (not \n" +
                "                            (equal ?X ?dog))) \n" +
                "                        (and \n" +
                "                          (greaterThan ?val1 ?val2) \n" +
                "                          (age ?dog ?val1) \n" +
                "                          (age ?X ?val2)))) \n" +
                "                    (instance ?bank Bank-FinancialOrganization) \n" +
                "                    (instance ?dog Canine) \n" +
                "                    (instance ?event Motion) \n" +
                "                    (patient ?event ?bank) \n" +
                "                    (agent ?event ?dog)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an entity, an agent and a process such that for all an object if the object is an instance of canine and the object is not equal to the agent, then a time duration is greater than another time duration and the age of the agent is the time duration and the age of the object is the other time duration and the entity is an instance of bank- financial organization and the agent is an instance of canine and the process is an instance of motion and the entity is a patient of the process and the agent is an agent of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "there exist an entity, an agent and a process such that for all an object if the object is an instance of canine and the object is not equal to the agent, then a time duration is greater than another time duration and the age of the agent is the time duration and the age of the object is the other time duration and the entity is an instance of bank- financial organization and the agent is an instance of canine and the process is an instance of motion and the entity is a patient of the process and the agent is an agent of the process";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "Mr Miller enters the bank."
     */
    @Test
    public void testFullyFormedMaleEntersBank()     {
        String stmt =   "(exists \n" +
                "                (?bank ?event) \n" +
                "                (and \n" +
                "                  (attribute MrMiller FullyFormed) \n" +
                "                  (attribute MrMiller Male) \n" +
                "                  (instance MrMiller Human) \n" +
                "                  (instance ?bank Bank-FinancialOrganization) \n" +
                "                  (agent ?event MrMiller) \n" +
                "                  (instance ?event Motion) \n" +
                "                  (destination ?event ?bank)))";


        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an entity and a process such that fully formed is an attribute of MrMiller and male is an attribute of MrMiller and MrMiller is an instance of human and the entity is an instance of bank- financial organization and MrMiller is an agent of the process and the process is an instance of motion and the process ends at the entity";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "male fully formed MrMiller motions to a bank- financial organization";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "Mary walks to the bank."
     */
    @Test
    public void testNamesMaryWalksToBank()     {
        String stmt =   "(exists \n" +
                "                (?woman ?bank ?event) \n" +
                "                (and \n" +
                "                  (attribute ?woman Female) \n" +
                "                  (instance ?woman Human) \n" +
                "                  (names \"Mary\" ?woman) \n" +
                "                  (instance ?bank Bank-FinancialOrganization) \n" +
                "                  (agent ?event ?woman) \n" +
                "                  (instance ?event Walking) \n" +
                "                  (destination ?event ?bank)))";


        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an agent, an entity and a process such that female is an attribute of the agent and the agent is an instance of human and the agent has name \"Mary\" and the entity is an instance of bank- financial organization and the agent is an agent of the process and the process is an instance of walking and the process ends at the entity";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        // TODO: or should it be "a woman named Mary walks to a bank- financial organization"?
        expectedResult = "Mary walks to a bank- financial organization";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * ? Ideal: "Mr Miller walks to the bank."
     */
    @Test
    public void testNamesMrMillerWalksToBank()     {
        String stmt =   "(exists \n" +
                "                (?bank ?event) \n" +
                "                (and \n" +
                "                  (attribute MrMiller FullyFormed) \n" +
                "                  (attribute MrMiller Male) \n" +
                "                  (instance MrMiller Human) \n" +
                "                  (names \"MrMiller\" MrMiller) \n" +
                "                  (instance ?bank Bank-FinancialOrganization) \n" +
                "                  (agent ?event MrMiller) \n" +
                "                  (instance ?event Walking) \n" +
                "                  (destination ?event ?bank)))";


        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an entity and a process such that fully formed is an attribute of MrMiller and male is an attribute of MrMiller and MrMiller is an instance of human and MrMiller has name \"MrMiller\" and the entity is an instance of bank- financial organization and MrMiller is an agent of the process and the process is an instance of walking and the process ends at the entity";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "male fully formed MrMiller walks to a bank- financial organization";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     *
     */
    @Test
    public void testHumanTravels()     {
        String stmt =   "(exists (?he ?event)\n" +
                "                  (and\n" +
                "                    (instance ?event Transportation)\n" +
                "                    (instance ?he Human)\n" +
                "                    (agent ?event ?he)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an agent and a process such that the process is an instance of transportation and the agent is an instance of human and the agent is an agent of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a human performs a transportation";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: "He travels to (the) Sudan."
     * Note that currently "Sudan" is NOT capitalized.
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

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an agent and a process such that the process is an instance of transportation and male is an attribute of the agent and the agent is an instance of human and the agent is an agent of the process and the process ends at sudan";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a male human performs a transportation to Sudan";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: ? "If an animal performs an intentional process, then the animal is awake."
     * This test may fail, but should be fixed by #17181: Modify LanguageFormatter.computeVariableTypes( )
     * so that the HashSet consists of only a single element which is the least general--the most specific.
     */
    @Test
    public void testAwakeIf()     {
        String stmt =   "(=>\n" +
                "           (and\n" +
                "               (instance ?PROC IntentionalProcess)\n" +
                "               (agent ?PROC ?HUMAN)\n" +
                "               (instance ?HUMAN Animal))\n" +
                "           (holdsDuring\n" +
                "               (WhenFn ?PROC)\n" +
                "               (attribute ?HUMAN Awake)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);

        // Do "formal" NLG.
        String expectedResult = "if a process is an instance of intentional process and an agent is an agent of the process and the agent is an instance of animal, " +
                "then awake is an attribute of the agent holds during the time of existence of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        // Do "informal" NLG.
        languageFormatter.setDoInformalNLG(true);

//        expectedResult = "if an animal performs an intentional process, then awake is an attribute of the animal holds during the time of existence of the process";
        expectedResult = "if a process is an instance of intentional process and an agent is an agent of the process and the agent is an instance of animal, " +
                "then awake is an attribute of the agent holds during the time of existence of the process";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * NOTE: Currently this test verifies that LanguageFormatter recovers from an IllegalArgumentException thrown by SumoProcessCollector. The console will display
     * a message to that effect ("Process parameter is not a Process: role = agent; process = ?PROC; entity = Human.").
     * FIXME: We need to find a better way to verify that LanguageFormatter is recovering from the exception. This must be done whenever we become
     * able to correctly translate this input into natural language.
     */
    @Test
    public void testAnimalLanguage()     {
        String stmt =   "(=>\n" +
                "           (and\n" +
                "               (instance ?LANG AnimalLanguage)\n" +
                "               (agent ?PROC ?AGENT)\n" +
                "               (instrument ?PROC ?LANG))\n" +
                "           (and\n" +
                "               (instance ?AGENT Animal)\n" +
                "               (not\n" +
                "                   (instance ?AGENT Human))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "if an object is an instance of animal language and an agent is an agent of a process and the object is an instrument for the process, " +
                "then the agent is an instance of animal and the agent is not an instance of human";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        //expectedResult = "if an animal is speaking an animal language, then the animal is not human";
        expectedResult = "if an object is an instance of animal language and an agent is an agent of a process and the object is an instrument for the process, then the agent is an instance of animal and the agent is not an instance of human";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Ideal: if an entity is an object, then the entity is a collection or the entity is a self connected object
     */
    @Test
    public void testObjectSubclassesIf()     {
        String stmt =   "(=>\n" +
                "           (instance ?A Object)\n" +
                "           (or \n" +
                "               (instance ?A Collection)\n" +
                "               (instance ?A SelfConnectedObject)))";


        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "if an entity is an instance of object, then the entity is an instance of collection or the entity is an instance of self connected object";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "if an entity is an instance of object, then the entity is an instance of collection or the entity is an instance of self connected object";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testSymmetricRelationIff()     {
        String stmt =   "(<=>\n" +
                "           (instance ?REL SymmetricRelation)\n" +
                "           (forall (?INST1 ?INST2)\n" +
                "               (=>\n" +
                "                   (?REL ?INST1 ?INST2)\n" +
                "                   (?REL ?INST2 ?INST1))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "an entity is an instance of symmetric relation if and only if for all another entity and a third entity if the entity the other entity and the third entity, then the entity the third entity and the other entity";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "an entity is an instance of symmetric relation if and only if for all another entity and a third entity if the entity the other entity and the third entity, then the entity the third entity and the other entity";
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

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "if a process is an instance of driving and an agent is an instance of human and the agent is an agent of the process, then there exists another process such that the other process is an instance of seeing and the agent is an agent of the other process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "if a human drives, then the human sees";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * This assertion is not valid, but we use to test how much the antecedent and the consequent affect each other.
     */
    @Test
    public void testHtmlParaphraseDrivingThenSeeingWithGlassesIf()     {
        String stmt =       "(=> \n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H))\n" +
                "               (exists (?S ?G)\n" +
                "                   (and\n" +
                "                       (instance ?G EyeGlass)\n" +
                "                       (instance ?S Seeing)\n" +
                "                       (instrument ?S ?G)\n" +
                "                       (agent ?S ?H))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "if a process is an instance of driving and an agent is an instance of human and the agent is an agent of the process, then there exist another process and an object such that the object is an instance of eye glass and the other process is an instance of seeing and the object is an instrument for the other process and the agent is an agent of the other process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "if a human drives, then the human sees with an eye glass";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * This assertion is not valid, but we use to test how much the antecedent and the consequent affect each other.
     */
    @Test
    public void testHtmlParaphraseDrivingThenControllingCarIf()     {
        String stmt =       "(=> \n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H))\n" +
                "               (exists (?C)\n" +
                "                   (and\n" +
                "                       (instance ?C Automobile)\n" +
                "                       (controlled ?D ?C))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "if a process is an instance of driving and an agent is an instance of human and the agent is an agent of the process, then there exists an entity such that the entity is an instance of automobile and the entity comes to be physically controlled by an agent during the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "if a process is an instance of driving and an agent is an instance of human and the agent is an agent of the process, then there exists an entity such that the entity is an instance of automobile and the entity comes to be physically controlled by an agent during the process";
        actualResult = languageFormatter.htmlParaphrase("");
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

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "if a process is an instance of driving and an agent is an instance of human and the agent is an agent of the process, then the agent is transported during the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "if a process is an instance of driving and an agent is an instance of human and the agent is an agent of the process, then the agent is transported during the process";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * As of 2/25/2015, this formula was being translated differently on the online version of Sigma and our local versions of Sigma.
     * This test now expects what our local versions return. When the problem is fixed, this test will fail and will then
     * need to be changed.
     */
    @Test
    public void testHtmlParaphraseBodyMotionBodyPositionIf()     {
        String stmt =       "(=>\n" +
                "               (instance ?ANIMAL Animal)\n" +
                "               (or\n" +
                "                   (exists (?MOTION)\n" +
                "                       (and\n" +
                "                           (instance ?MOTION BodyMotion)\n" +
                "                           (agent ?MOTION ?ANIMAL)))\n" +
                "                   (exists (?ATTR)\n" +
                "                       (and\n" +
                "                           (instance ?ATTR BodyPosition)\n" +
                "                           (attribute ?ANIMAL ?ATTR)))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);

        String expectedResult = "if an agent is an instance of animal, " +
                "then there exists a process such that the process is an instance of body motion and the agent is an agent of the process " +
                "or there exists an entity such that the entity is an instance of body position and the entity is an attribute of the agent";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "if an agent is an instance of animal, " +
                "then there exists a process such that the process is an instance of body motion and the agent is an agent of the process " +
                "or there exists an entity such that the entity is an instance of body position and the entity is an attribute of the agent";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testJohnSeesSelfConnectedObject()     {
        String stmt =   "(exists (?event ?object)\n" +
                "           (and \n" +
                "               (instance John-1 Human) \n" +
                "               (instance ?event Seeing) \n" +
                "               (instance ?object SelfConnectedObject) \n" +
                "               (experiencer ?event John-1) \n" +
                "               (patient ?event ?object)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);

        String expectedResult = "there exist a process and an entity such that John-1 is an instance of human and the process is an instance of seeing and the entity is an instance of self connected object and John-1 experiences the process and the entity is a patient of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "John-1 sees a self connected object";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testJohnSeesSelfConnectedObjectNot1()     {
        String stmt =   "(exists (?event ?object)\n" +
                "           (and \n" +
                "               (instance John-1 Human) \n" +
                "               (instance ?event Seeing) \n" +
                "               (instance ?object SelfConnectedObject) \n" +
                "               (experiencer ?event John-1) \n" +
                "               (not \n" +
                "                   (patient ?event ?object))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);

        String expectedResult = "there exist a process and an entity such that John-1 is an instance of human and the process is an instance of seeing and the entity is an instance of self connected object and John-1 experiences the process and the entity is not a patient of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "John-1 doesn't see a self connected object";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testJohnSeesSelfConnectedObjectNot2()     {
        String stmt =   "(exists (?event ?object)\n" +
                "           (and \n" +
                "               (instance John-1 Human) \n" +
                "               (instance ?event Seeing) \n" +
                "               (instance ?object SelfConnectedObject) \n" +
                "               (not \n" +
                "                   (and \n" +
                "                       (experiencer ?event John-1) \n" +
                "                       (patient ?event ?object)))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);

        String expectedResult = "there exist a process and an entity such that John-1 is an instance of human and the process is an instance of seeing and the entity is an instance of self connected object and ~{ John-1 experiences the process } or ~{ the entity is a patient of the process }";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "John-1 doesn't see a self connected object";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * This formula has no agent/experiencer. Eventually we will want to translate it into passive voice--currently we fall back to formal NLG.
     */
    @Test
    public void testSelfConnectedObjectIsSeen()     {
        String stmt =   "(exists (?event ?object)\n" +
                "           (and \n" +
                "               (instance ?event Seeing) \n" +
                "               (instance ?object SelfConnectedObject) \n" +
                "               (patient ?event ?object)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);

        String expectedResult = "there exist a process and an entity such that the process is an instance of seeing and the entity is an instance of self connected object and the entity is a patient of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a self connected object experiences a seeing";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testJohnSeesHamburger()     {
        String stmt =   "(exists (?event ?hamburger)\n" +
                "           (and \n" +
                "               (instance John-1 Human) \n" +
                "               (instance ?event Seeing) \n" +
                "               (instance ?hamburger (FoodForFn Human)) \n" +
                "               (experiencer ?event John-1) \n" +
                "               (patient ?event ?hamburger)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);

        String expectedResult = "there exist a process and an entity such that John-1 is an instance of human and the process is an instance of seeing and the entity is an instance of FoodForFn human and John-1 experiences the process and the entity is a patient of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "there exist a process and an entity such that John-1 is an instance of human and the process is an instance of seeing and the entity is an instance of FoodForFn human and John-1 experiences the process and the entity is a patient of the process";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testArtifactMoves()     {
        String stmt =   "(exists (?event ?thing)\n" +
                "           (and \n" +
                "               (instance ?event BodyMotion) \n" +
                "               (instance ?thing Artifact) \n" +
                "               (moves ?event ?thing)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);

        String expectedResult = "there exist a motion and an object such that the motion is an instance of body motion and the object is an instance of artifact and moves the motion and the object";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        // FIXME: the CaseRole has influence on the verb: expectedResult = "an artifact moves";
        expectedResult = "an artifact experiences a body motion";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testArtifactMovesNot1()     {
        String stmt =   "(not\n" +
                "           (exists (?event)\n" +
                "               (and \n" +
                "                   (instance ?event BodyMotion) \n" +
                "                   (instance Thing1 Artifact) \n" +
                "                   (moves ?event Thing1))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);

        String expectedResult = "there doesn't exist a motion such that the motion is an instance of body motion and Thing1 is an instance of artifact and moves the motion and Thing1";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        // FIXME: the CaseRole has influence on the verb: expectedResult = "an artifact moves";
        expectedResult = "Thing1 doesn't experience a body motion";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testArtifactMovesNot2()     {
        String stmt =   "(exists (?event ?thing)\n" +
                "           (and \n" +
                "               (instance ?event BodyMotion) \n" +
                "               (instance ?thing Artifact) \n" +
                "               (not \n" +
                "                   (moves ?event ?thing))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);

        String expectedResult = "there exist a motion and an object such that the motion is an instance of body motion and the object is an instance of artifact and moves the motion and the object";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        // FIXME: the CaseRole has influence on the verb: expectedResult = "an artifact moves";
        expectedResult = "an artifact doesn't experience a body motion";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testGirlDrivesFromLondonOnHighway()     {
        String stmt =   "(exists (?she ?event ?route)\n" +
                "                  (and\n" +
                "                    (instance ?event Driving) \n" +
                "                    (instance ?she Girl) \n" +
                "                    (instance ?route Expressway) \n" +
                "                    (agent ?event ?she) \n" +
                "                    (path ?event ?route) \n" +
                "                    (origin ?event LondonUnitedKingdom)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an agent, a motion and an object such that the motion is an instance of driving and the agent is an instance of girl and the object is an instance of expressway and the agent is an agent of the motion and the object is path along which the motion occurs and the motion originates at London";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a girl drives along an expressway from LondonUnitedKingdom";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testGirlDrivesTowardsLeedsFromLondonOnHighway()     {
        String stmt =   "(exists (?she ?event ?route)\n" +
                "                  (and\n" +
                "                    (instance ?event Driving) \n" +
                "                    (instance ?she Girl) \n" +
                "                    (instance ?route Expressway) \n" +
                "                    (agent ?event ?she) \n" +
                "                    (path ?event ?route) \n" +
                "                    (direction ?event Leeds) \n" +
                "                    (origin ?event LondonUnitedKingdom)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an agent, a motion and an object such that the motion is an instance of driving and the agent is an instance of girl and the object is an instance of expressway and the agent is an agent of the motion and the object is path along which the motion occurs and entities in the process the motion are moving Leeds and the motion originates at London";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a girl drives toward Leeds along an expressway from LondonUnitedKingdom";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testGirlDrivesNorthFromLondonOnHighway()     {
        String stmt =   "(exists (?she ?event ?route)\n" +
                "                  (and\n" +
                "                    (instance ?event Driving) \n" +
                "                    (instance ?she Girl) \n" +
                "                    (instance ?route Expressway) \n" +
                "                    (agent ?event ?she) \n" +
                "                    (path ?event ?route) \n" +
                "                    (direction ?event North) \n" +
                "                    (origin ?event LondonUnitedKingdom)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an agent, a motion and an object such that the motion is an instance of driving and the agent is an instance of girl and the object is an instance of expressway and the agent is an agent of the motion and the object is path along which the motion occurs and entities in the process the motion are moving north and the motion originates at London";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a girl drives toward North along an expressway from LondonUnitedKingdom";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testGirlDrivesNorthThroughLeedsFromLondonOnHighway()     {
        String stmt =   "(exists (?she ?event ?route ?midpoint)\n" +
                "                  (and\n" +
                "                    (instance ?event Driving) \n" +
                "                    (instance ?she Girl) \n" +
                "                    (instance ?route Expressway) \n" +
                "                    (instance Leeds City) \n" +
                "                    (agent ?event ?she) \n" +
                "                    (path ?event ?route) \n" +
                "                    (direction ?event North) \n" +
                "                    (eventPartlyLocated ?event Leeds) \n" +
                "                    (origin ?event LondonUnitedKingdom)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an agent, a motion, , , an object and an entity such that the motion is an instance of driving and the agent is an instance of girl and the object is an instance of expressway and Leeds is an instance of city and the agent is an agent of the motion and the object is path along which the motion occurs and entities in the process the motion are moving north and event partly located the motion and Leeds and the motion originates at London";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a girl drives toward North along an expressway from LondonUnitedKingdom in Leeds";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testGirlDrivesThroughEngland()     {
        String stmt =   "(exists (?she ?event)\n" +
                "                  (and\n" +
                "                    (instance ?event Driving) \n" +
                "                    (instance ?she Girl) \n" +
                "                    (agent ?event ?she) \n" +
                "                    (eventLocated ?event England)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an agent and a process such that the process is an instance of driving and the agent is an instance of girl and the agent is an agent of the process and event located the process and England";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a girl drives in England";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testGirlCarveWeaponOutOfSoap()     {
        String stmt =   "(exists (?she ?event ?gun ?soap)\n" +
                "                  (and\n" +
                "                    (instance ?event Making) \n" +
                "                    (instance ?she Woman) \n" +
                "                    (instance ?gun Weapon) \n" +
                "                    (instance ?soap Artifact) \n" +
                "                    (agent ?event ?she) \n" +
                "                    (patient ?event ?gun) \n" +
                "                    (resource ?event ?soap)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an agent, a process, , , an entity and an object such that the process is an instance of making and the agent is an instance of woman and the entity is an instance of weapon and the object is an instance of artifact and the agent is an agent of the process and the entity is a patient of the process and the object is a resource for the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a woman makes a weapon out of an artifact";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    /**
     * Like the previous test, but gun is a result--a more specific patient.
     */
    @Test
    public void testGirlCarveWeaponOutOfSoap2()     {
        String stmt =   "(exists (?she ?event ?gun ?soap)\n" +
                "                  (and\n" +
                "                    (instance ?event Making) \n" +
                "                    (instance ?she Woman) \n" +
                "                    (instance ?gun Weapon) \n" +
                "                    (instance ?soap Artifact) \n" +
                "                    (agent ?event ?she) \n" +
                "                    (result ?event ?gun) \n" +
                "                    (resource ?event ?soap)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an agent, a process, , , an entity and an object such that the process is an instance of making and the agent is an instance of woman and the entity is an instance of weapon and the object is an instance of artifact and the agent is an agent of the process and the entity is a result of the process and the object is a resource for the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a woman makes a weapon out of an artifact";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testBoyAttendsDemonstration()     {
        String stmt =   "(exists (?he ?event)\n" +
                "                  (and\n" +
                "                    (instance ?event Demonstrating) \n" +
                "                    (instance ?he Boy) \n" +
                "                    (attends ?event ?he)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist a human and a demonstrating such that the demonstrating is an instance of demonstrating and the human is an instance of boy and attends the demonstrating and the human";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        // FIXME: the CaseRole has influence on the verb:
        //expectedResult = "a boy attends a demonstration";
        expectedResult = "a boy experiences a demonstrating";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testConnectedObjectBurnsPatient()     {
        String stmt =   "(exists (?object ?event)\n" +
                "                  (and\n" +
                "                    (instance ?event Combustion) \n" +
                "                    (instance ?object SelfConnectedObject) \n" +
                "                    (patient ?event ?object)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an entity and a process such that the process is an instance of combustion and the entity is an instance of self connected object and the entity is a patient of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a self connected object experiences a combustion";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testConnectedObjectBurnsResource()     {
        String stmt =   "(exists (?object ?event)\n" +
                "                  (and\n" +
                "                    (instance ?event Combustion) \n" +
                "                    (instance ?object SelfConnectedObject) \n" +
                "                    (resource ?event ?object)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an object and a process such that the process is an instance of combustion and the object is an instance of self connected object and the object is a resource for the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a self connected object experiences a combustion";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testBoyBurnsCombustibleObject()     {
        String stmt =   "(exists (?he ?event ?object)\n" +
                "                  (and\n" +
                "                    (instance ?event Combustion) \n" +
                "                    (instance ?he Boy) \n" +
                "                    (instance ?object SelfConnectedObject) \n" +
                "                    (patient ?event ?object) \n" +
                "                    (agent ?event ?he)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an agent, a process and an entity such that the process is an instance of combustion and the agent is an instance of boy and the entity is an instance of self connected object and the entity is a patient of the process and the agent is an agent of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "a boy performs a combustion on a self connected object";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testDrivingVehicle()     {
        String stmt =   "(=>\n" +
                "           (instance ?DRIVE Driving)\n" +
                "           (exists (?VEHICLE)\n" +
                "               (and\n" +
                "                   (instance ?VEHICLE Vehicle)\n" +
                "                   (patient ?DRIVE ?VEHICLE))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "if a process is an instance of driving, then there exists an entity such that the entity is an instance of vehicle and the entity is a patient of the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "if someone drives, then a vehicle experiences a driving";
        // expectedResult = "if a process is an instance of driving, then there exists an entity such that the entity is an instance of vehicle and the entity is a patient of the process";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testSwimmingAgentLocatedWater()     {
        String stmt =   "(=>\n" +
                "           (and\n" +
                "               (instance ?SWIM Swimming)\n" +
                "               (agent ?SWIM ?AGENT))\n" +
                "           (exists (?AREA)\n" +
                "               (and\n" +
                "                   (instance ?AREA WaterArea)\n" +
                "                   (located ?AGENT ?AREA))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "if a process is an instance of swimming and an agent is an agent of the process, then there exists an object such that the object is an instance of water area and the agent is located at the object";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        //expectedResult = "if an agent swims, then the agent is in a body of water";
        expectedResult = "if a process is an instance of swimming and an agent is an agent of the process, then there exists an object such that the object is an instance of water area and the agent is located at the object";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testSwimmingProcessLocatedWater()     {
        String stmt =   "(=>\n" +
                "           (and\n" +
                "               (instance ?SWIM Swimming)\n" +
                "               (agent ?SWIM ?AGENT))\n" +
                "           (exists (?AREA)\n" +
                "               (and\n" +
                "                   (instance ?AREA WaterArea)\n" +
                "                   (eventLocated ?SWIM ?AREA))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "if a process is an instance of swimming and an agent is an agent of the process, then there exists an entity such that the entity is an instance of water area and event located the process and the entity";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        //expectedResult = "if an agent swims, then the agent is in a body of water";
        expectedResult = "if a process is an instance of swimming and an agent is an agent of the process, then there exists an entity such that the entity is an instance of water area and event located the process and the entity";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testObjectFillsHole()     {
        String stmt =   "(exists (?OBJ ?HOLE)\n" +
                "           (and\n" +
                "               (instance ?OBJ Object)\n" +
                "               (instance ?HOLE Hole)\n" +
                "               (fills ?OBJ ?HOLE)))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an object and a hole such that the object is an instance of object and the hole is an instance of hole and the object fills the hole";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "there exist an object and a hole such that the object is an instance of object and the hole is an instance of hole and the object fills the hole";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }

    @Test
    public void testObjectFillsHoleNot()     {
        String stmt =   "(exists (?OBJ ?HOLE)\n" +
                "           (and\n" +
                "               (instance ?OBJ Object)\n" +
                "               (instance ?HOLE Hole)\n" +
                "               (not\n" +
                "                   (fills ?OBJ ?HOLE))))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "there exist an object and a hole such that the object is an instance of object and the hole is an instance of hole and the object doesn't fill the hole";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        //expectedResult = "some object doesn't fill a hole";
        expectedResult = "there exist an object and a hole such that the object is an instance of object and the hole is an instance of hole and the object doesn't fill the hole";
        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, StringUtil.filterHtml(actualResult));
    }
}