package com.articulate.sigma;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * LanguageFormatter tests NOT targeted toward the htmlParaphrase( ) method.
 * See LanguageFormatterHtmlParaphraseTest for tests that invokce this method.
 */
public class LanguageFormatterTest extends SigmaTestBase {
    @BeforeClass
    public static void setup()  {
        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        KBmanager.getMgr().getKB("SUMO");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testReadKeywordMapNull() {
        LanguageFormatter.readKeywordMap(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testReadKeywordMapEmpty() {
        LanguageFormatter.readKeywordMap("");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testReadKeywordMapNoExist() {
        LanguageFormatter.readKeywordMap("/somePathThatDoesntExist/SomeFileThatDoesntExist.txt");
    }

    /**
     * Verify no exception is thrown when the path is valid.
     */
    @Test
    public void testReadKeywordMapCorrectParameter()    {
        LanguageFormatter.readKeywordMap(KB_PATH);
    }

    @Test
    public void testFormatListNoList()   {
        String input = "";
        String actual = LanguageFormatter.formatList(input, "EnglishLanguage");
        String expected = "";
        assertEquals(expected, actual);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFormatListNoLanguage()   {
        String input = "?A ?B ?C";
        String actual = LanguageFormatter.formatList(input, "");
    }

    @Test
    public void testFormat1List()   {
        String input = "?A";
        String actual = LanguageFormatter.formatList(input, "EnglishLanguage");
        String expected = "?A";
        assertEquals(expected, actual);
    }

    @Test
    public void testFormat2ListWithNoAnd()   {
        String input = "?A ?B";
        String actual = LanguageFormatter.formatList(input, "EnglishLanguage");
        String expected = "?A and ?B";
        assertEquals(expected, actual);
    }

    @Test
    public void testFormat2ListWithAnd()   {
        String input = "?A and ?B";
        String actual = LanguageFormatter.formatList(input, "EnglishLanguage");
        String expected = "?A and ?B";
        assertEquals(expected, actual);
    }

    @Test
    public void testFormat3ListWithNoAnd()   {
        String input = "?A ?B ?C";
        String actual = LanguageFormatter.formatList(input, "EnglishLanguage");
        String expected = "?A, ?B and ?C";
        assertEquals(expected, actual);
    }

    @Test
    public void testFormat3ListWithAnd()   {
        String input = "?A ?B and ?C";
        String actual = LanguageFormatter.formatList(input, "EnglishLanguage");
        String expected = "?A, ?B and ?C";
        assertEquals(expected, actual);
    }

    @Test
    public void testFormatListWithNoAndFrench()   {
        String input = "?A ?B ?C";
        String actual = LanguageFormatter.formatList(input, "fr");
        String expected = "?A, ?B et ?C";
        assertEquals(expected, actual);
    }

    @Test
    public void testFormatListWithAndFrench()   {
        String input = "?A ?B et ?C";
        String actual = LanguageFormatter.formatList(input, "fr");
        String expected = "?A, ?B et ?C";
        assertEquals(expected, actual);
    }

}