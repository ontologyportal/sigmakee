package com.articulate.sigma;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * LanguageFormatter tests specifically targeted toward the htmlParaphrase( ) method.
 */
public class LanguageFormatterHtmlParaphraseTest extends SigmaTestBase  {
    private static KB kb;

    @BeforeClass
    public static void setup()  {
        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        kb = KBmanager.getMgr().getKB("SUMO");
    }

    @Test
    public void testHtmlParaphraseDomainDatePhysical()     {
        String stmt = "(domain date 1 Physical)";

        String expectedResult = "the number 1 argument of date is an instance of physical";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
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

        String expectedResult = "if an entity is an instance of object attitude and the entity another entity and a third entity," +
                "then the third entity is an instance of physical";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseSubstanceAttributePhysicalState()     {
        String stmt = "(<=> (instance ?OBJ Substance) (exists (?ATTR) (and (instance ?ATTR PhysicalState) (attribute ?OBJ ?ATTR))))";

        String expectedResult = "an entity is an instance of substance if and only if there exists another entity such that " +
                "the other entity is an instance of physical state and the other entity is an attribute of the entity";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseBiologicallyActiveSubstance()     {
        String stmt = "(subclass BiologicallyActiveSubstance Substance)";

        String expectedResult = "biologically active substance is a subclass of substance";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphrasePureSubstanceMixture()     {
        String stmt = "(partition Substance PureSubstance Mixture)";

        String expectedResult = "substance is exhaustively partitioned into pure substance and mixture";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    /**
     * Ideal output: Should have a space after the comma.
     */
    @Test
    public void testHtmlParaphrasePartlyLocated()     {
        String stmt = "(=> (and (instance ?OBJ1 Object) (partlyLocated ?OBJ1 ?OBJ2)) (exists (?SUB) (and (part ?SUB ?OBJ1) (located ?SUB ?OBJ2))))";

        String expectedResult = "if an entity is an instance of object and the entity is partly located in another entity," +
                "then there exists a third entity such that the third entity is a part of the entity and the third entity is located at the other entity";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    /**
     * Ideal output: Shouldn't the output say something about time?
     */
    @Test
    public void testHtmlParaphraseDefinePhysical()     {
        String stmt = "(<=> (instance ?PHYS Physical) (exists (?LOC ?TIME) (and (located ?PHYS ?LOC) (time ?PHYS ?TIME))))";

        String expectedResult = "an entity is an instance of physical if and only if there exist another entity and a third entity such that " +
                "the entity is located at the other entity and the entity exists during the third entity";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphrasePatient()     {
        String stmt = "( patient Leaving ?ENTITY )";

        String expectedResult = "an entity is a patient of Leaving";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseNames()     {
        String stmt = "(names \"John\" ?H)";

        String expectedResult = "an entity has name \"John\"";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseNamesNot()     {
        String stmt = "(not (names \"John\" ?H))";

        String expectedResult = "an entity doesn't have name \"John\"";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDriving1()     {
        String stmt = "(exists (?D ?H)\n" +
                "   (and\n" +
                "       (instance ?D Driving)\n" +
                "       (instance ?H Human)\n" +
                "       (agent ?D ?H)))";

        String expectedResult = "there exist an entity and another entity such that the entity is an instance of driving and " +
                "the other entity is an instance of human and the other entity is an agent of the entity";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDriving2()     {
        String stmt = "(exists (?D ?H)\n" +
                "   (and\n" +
                "       (instance ?D Driving)\n" +
                "       (instance ?H Human)\n" +
                "       (names \"John\" ?H)\n" +
                "       (agent ?D ?H)))";

        String expectedResult = "there exist an entity and another entity such that the entity is an instance of driving and " +
                "the other entity is an instance of human and the other entity has name \"John\" and the other entity is an agent of the entity";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDriving3()     {
        String stmt = "(exists (?D ?H ?Car)\n" +
                "   (and\n" +
                "       (instance ?D Driving)\n" +
                "       (instance ?H Human)\n" +
                "       (names \"John\" ?H)\n" +
                "       (instance ?Car Automobile)\n" +
                "       (agent ?D ?H)\n" +
                "       (instrument ?D ?Car)))";

        String expectedResult = "there exist an entity, another entity and a third entity such that the entity is an instance of driving and "
                + "the other entity is an instance of human and the other entity has name \"John\" and " +
                "the third entity is an instance of Automobile and the other entity is an agent of the entity and the third entity is an instrument for the entity";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

}