package com.articulate.sigma.nlg;

import com.articulate.sigma.Formula;
import com.articulate.sigma.SigmaTestBase;
import com.articulate.sigma.UnitTestBase;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class NLGUtilsTest extends UnitTestBase {
    @Test(expected=IllegalArgumentException.class)
    public void testReadKeywordMapNull() {
        NLGUtils.readKeywordMap(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testReadKeywordMapEmpty() {
        NLGUtils.readKeywordMap("");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testReadKeywordMapNoExist() {
        NLGUtils.readKeywordMap("/somePathThatDoesntExist/SomeFileThatDoesntExist.txt");
    }

    /**
     * Verify no exception is thrown when the path is valid.
     */
    @Test
    public void testReadKeywordMapCorrectParameter()    {
        NLGUtils.readKeywordMap(SigmaTestBase.KB_PATH);
    }

    @Test
    public void testFormatListNoList()   {
        String input = "";
        String actual = NLGUtils.formatList(input, "EnglishLanguage");
        String expected = "";
        assertEquals(expected, actual);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFormatListNoLanguage()   {
        String input = "?A ?B ?C";
        NLGUtils.formatList(input, "");
    }

    @Test
    public void testFormat1List()   {
        String input = "?A";
        String actual = NLGUtils.formatList(input, "EnglishLanguage");
        String expected = "?A";
        assertEquals(expected, actual);
    }

    @Test
    public void testFormat2ListWithNoAnd()   {
        String input = "?A ?B";
        String actual = NLGUtils.formatList(input, "EnglishLanguage");
        String expected = "?A and ?B";
        assertEquals(expected, actual);
    }

    @Test
    public void testFormat2ListWithAnd()   {
        String input = "?A and ?B";
        String actual = NLGUtils.formatList(input, "EnglishLanguage");
        String expected = "?A and ?B";
        assertEquals(expected, actual);
    }

    @Test
    public void testFormat3ListWithNoAnd()   {
        String input = "?A ?B ?C";
        String actual = NLGUtils.formatList(input, "EnglishLanguage");
        String expected = "?A, ?B and ?C";
        assertEquals(expected, actual);
    }

    @Test
    public void testFormat3ListWithAnd()   {
        String input = "?A ?B and ?C";
        String actual = NLGUtils.formatList(input, "EnglishLanguage");
        String expected = "?A, ?B and ?C";
        assertEquals(expected, actual);
    }

    @Test
    public void testFormatListWithNoAndFrench()   {
        String input = "?A ?B ?C";
        String actual = NLGUtils.formatList(input, "fr");
        String expected = "?A, ?B et ?C";
        assertEquals(expected, actual);
    }

    @Test
    public void testFormatListWithAndFrench()   {
        String input = "?A ?B et ?C";
        String actual = NLGUtils.formatList(input, "fr");
        String expected = "?A, ?B et ?C";
        assertEquals(expected, actual);
    }

    @Test
    public void testCollectOrderedVariablesWithFormula1()  {
        String stmt =   "(exists (?he ?event)\n" +
                "                  (and\n" +
                "                    (instance ?event Transportation)\n" +
                "                    (instance ?he Human)\n" +
                "                    (agent ?event ?he)))";
        Formula formula = new Formula(stmt);

        List<String> actual = NLGUtils.collectOrderedVariables(formula.getFormula());

        List<String> expected = Lists.newArrayList("?he", "?event");

        assertEquals(expected, actual);
    }

    @Test
    public void testCollectOrderedVariablesWithFormula2()  {
        String stmt =   "(agent ?event ?he)";
        Formula formula = new Formula(stmt);

        List<String> actual = NLGUtils.collectOrderedVariables(formula.getFormula());

        List<String> expected = Lists.newArrayList("?event", "?he");

        assertEquals(expected, actual);
    }

    @Test
    public void testCollectOrderedVariablesWithFormula3()  {
        String stmt =   "(names \"John\" ?H)";
        Formula formula = new Formula(stmt);

        List<String> actual = NLGUtils.collectOrderedVariables(formula.getFormula());

        List<String> expected = Lists.newArrayList("?H");

        assertEquals(expected, actual);
    }

    @Test
    public void testCollectOrderedVariablesWithString1()  {
        String stmt =   "(exists (?he ?event)\n" +
                "                  (and\n" +
                "                    (instance ?event Transportation)\n" +
                "                    (instance ?he Human)\n" +
                "                    (agent ?event ?he)))";
        List<String> actual = NLGUtils.collectOrderedVariables(stmt);

        List<String> expected = Lists.newArrayList("?he", "?event");

        assertEquals(expected, actual);
    }

    @Test
    public void testCollectOrderedVariablesWithString2()  {
        String stmt =   "agent ?event ?he";
        List<String> actual = NLGUtils.collectOrderedVariables(stmt);

        List<String> expected = Lists.newArrayList("?event", "?he");

        assertEquals(expected, actual);
    }

    @Test
    public void testCollectOrderedVariablesWithString3()  {
        String stmt =   "names \"John\" ?H";
        List<String> actual = NLGUtils.collectOrderedVariables(stmt);

        List<String> expected = Lists.newArrayList("?H");

        assertEquals(expected, actual);
    }

}
