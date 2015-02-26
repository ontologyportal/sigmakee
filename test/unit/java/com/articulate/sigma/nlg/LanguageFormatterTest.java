package com.articulate.sigma.nlg;

import com.articulate.sigma.Formula;
import com.articulate.sigma.SigmaTestBase;
import com.articulate.sigma.UnitTestBase;
import com.articulate.sigma.nlg.LanguageFormatter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * LanguageFormatter tests NOT targeted toward the htmlParaphrase( ) method.
 * See LanguageFormatterHtmlParaphraseTest for tests that invoke this method.
 */
public class LanguageFormatterTest extends UnitTestBase {

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
        LanguageFormatter.readKeywordMap(SigmaTestBase.KB_PATH);
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
        LanguageFormatter.formatList(input, "");
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

    @Test
    public void testFilterHtml()   {
        String input = "<ul><li>if for all <a href=\"&term=Entity\">an entity</a> <a href=\"&term=Entity\">the entity</a> is an <a href=\"&term=element\">element</a> of <a href=\"&term=Set\">a  set</a> if and only if <a href=\"&term=Entity\">the entity</a> is an <a href=\"&term=element\">element</a> of <a href=\"&term=Set\">another set</a>,</li><li>then <a href=\"&term=Set\">the set</a> is <a href=\"&term=equal\">equal</a> to <a href=\"&term=Set\">the other set</a></li></ul>";
        String actual = LanguageFormatter.filterHtml(input);
        String expected = "if for all an entity the entity is an element of a set if and only if the entity is an element of another set, " +
                "then the set is equal to the other set";
        assertEquals(expected, actual);
    }

    @Ignore
    @Test
    public void testStatementParse() {
        String input = "(exists (?D ?H) (and (instance ?D Driving) (instance ?H Human) (agent ?D ?H)))";
        LanguageFormatter lf = new LanguageFormatter(input, SigmaTestBase.kb.getFormatMap("EnglishLanguage"), SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        String actual = lf.paraphraseStatement(input, false, 0);
        assertEquals("", actual);
    }

    @Test
    public void testVariableReplaceBasic() {
        String form = "there exist ?D and ?H such that ?D is an &%instance$\"instance\" of &%Driving$\"driving\" and ?H is an &%instance$\"instance\" of &%Human$\"human\" and ?H is an &%agent$\"agent\" of ?D";
        HashMap<String, HashSet<String>> instanceMap = Maps.newHashMap();
        instanceMap.put("?D", Sets.newHashSet("Process"));
        instanceMap.put("?H", Sets.newHashSet("Agent"));
        HashMap<String, HashSet<String>> classMap = Maps.newHashMap();

        String expected = "there exist &%Process$\"a  process\" and &%Agent$\"an agent\" such that &%Process$\"the process\" is an &%instance$\"instance\" of &%Driving$\"driving\" and &%Agent$\"the agent\" is an &%instance$\"instance\" of &%Human$\"human\" and &%Agent$\"the agent\" is an &%agent$\"agent\" of &%Process$\"the process\"";

        String actual = LanguageFormatter.variableReplace(form, instanceMap, classMap, SigmaTestBase.kb, "EnglishLanguage");

        assertEquals(expected, actual);
    }

    @Test
    public void testGenerateFormalNaturalLanguageIf() {
        LanguageFormatter formatter = new LanguageFormatter("", SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        List<String> translations = Lists.newArrayList("Socrates is a man", "Socrates is mortal");
        String actual = formatter.generateFormalNaturalLanguage(translations, "=>", false);
        actual = LanguageFormatter.filterHtml(actual);

        assertEquals("if Socrates is a man, then Socrates is mortal", actual);

        actual = formatter.generateFormalNaturalLanguage(translations, "=>", true);
        actual = LanguageFormatter.filterHtml(actual);

        assertEquals("Socrates is mortal and ~{Socrates is a man}", actual);
    }

    @Test
    public void testGenerateFormalNaturalLanguageIfAndOnlyIf() {
        LanguageFormatter formatter = new LanguageFormatter("", SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        List<String> translations = Lists.newArrayList("Socrates is a man", "Socrates is mortal");
        String actual = formatter.generateFormalNaturalLanguage(translations, "<=>", false);
        actual = LanguageFormatter.filterHtml(actual);

        assertEquals("Socrates is a man if and only if Socrates is mortal", actual);

        actual = formatter.generateFormalNaturalLanguage(translations, "<=>", true);
        actual = LanguageFormatter.filterHtml(actual);

        assertEquals("Socrates is mortal or ~{ Socrates is a man } or Socrates is a man or ~{ Socrates is mortal }", actual);
    }

    @Test
    public void testGenerateFormalNaturalLanguageAnd() {
        LanguageFormatter formatter = new LanguageFormatter("", SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        List<String> translations = Lists.newArrayList("Socrates is a man", "Socrates is mortal");
        String actual = formatter.generateFormalNaturalLanguage(translations, "and", false);
        actual = LanguageFormatter.filterHtml(actual);

        assertEquals("Socrates is a man and Socrates is mortal", actual);

        actual = formatter.generateFormalNaturalLanguage(translations, "and", true);
        actual = LanguageFormatter.filterHtml(actual);

        assertEquals("~{ Socrates is a man } or ~{ Socrates is mortal }", actual);
    }

    @Test
    public void testGenerateFormalNaturalLanguageOr() {
        LanguageFormatter formatter = new LanguageFormatter("", SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        List<String> translations = Lists.newArrayList("Socrates is a man", "Socrates is mortal");
        String actual = formatter.generateFormalNaturalLanguage(translations, "or", false);
        actual = LanguageFormatter.filterHtml(actual);

        assertEquals("Socrates is a man or Socrates is mortal", actual);

        actual = formatter.generateFormalNaturalLanguage(translations, "or", true);
        actual = LanguageFormatter.filterHtml(actual);

        assertEquals("Socrates is a man and Socrates is mortal", actual);
    }

    /**
     * Test various LanguageFormatter methods that are employed in "informal" NLG.
     */
    @Test
    public void testInformalNLGWithHtml() {
        String form = "<ul><li>if ?H drives,</li><li>then ?H sees</li></ul>";

        // Verify variableReplace( ).
        Map<String, HashSet<String>> instanceMap = Maps.newHashMap(ImmutableMap.of("?S", Sets.newHashSet("Seeing"),
                "?H", Sets.newHashSet("Human"), "?D", Sets.newHashSet("Driving")));
        HashMap<String, HashSet<String>> classMap = Maps.newHashMap();

        String expected = "<ul><li>if &%Human$\"a  human\" drives,</li><li>then &%Human$\"the human\" sees</li></ul>";
        String variableReplaceOutput = LanguageFormatter.variableReplace(form, instanceMap, classMap, SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expected, variableReplaceOutput);

        // Verify resolveFormatSpecifiers( ).
        expected = "<ul><li>if <a href=\"&term=Human\">a  human</a> drives,</li><li>then <a href=\"&term=Human\">the human</a> sees</li></ul>";
        String resolveFormatSpecifiersOutput = LanguageFormatter.resolveFormatSpecifiers(variableReplaceOutput, "");
        assertEquals(expected, resolveFormatSpecifiersOutput);
    }

    /**
     * Test various LanguageFormatter methods that are employed in "informal" NLG.
     */
    @Test
    public void testInformalNLGWithoutHtml() {
        String form = "if ?H drives, then ?H sees";

        // Verify variableReplace( ).
        Map<String, HashSet<String>> instanceMap = Maps.newHashMap(ImmutableMap.of("?S", Sets.newHashSet("Seeing"),
                "?H", Sets.newHashSet("Human"), "?D", Sets.newHashSet("Driving")));
        HashMap<String, HashSet<String>> classMap = Maps.newHashMap();

        String expected = "if &%Human$\"a  human\" drives, then &%Human$\"the human\" sees";
        String variableReplaceOutput = LanguageFormatter.variableReplace(form, instanceMap, classMap, SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expected, variableReplaceOutput);

        // Verify resolveFormatSpecifiers( ).
        expected = "if a human drives, then the human sees";
        String resolveFormatSpecifiersOutput = LanguageFormatter.resolveFormatSpecifiers(variableReplaceOutput, "");
        assertEquals(expected, LanguageFormatter.filterHtml(resolveFormatSpecifiersOutput));
    }

    @Test
    public void testCollectOrderedVariablesWithFormula1()  {
        String stmt =   "(exists (?he ?event)\n" +
                "                  (and\n" +
                "                    (instance ?event Transportation)\n" +
                "                    (instance ?he Human)\n" +
                "                    (agent ?event ?he)))";
        Formula formula = new Formula(stmt);

        List<String> actual = LanguageFormatter.collectOrderedVariables(formula.theFormula);

        List<String> expected = Lists.newArrayList("?he", "?event");

        assertEquals(expected, actual);
    }

    @Test
    public void testCollectOrderedVariablesWithFormula2()  {
        String stmt =   "(agent ?event ?he)";
        Formula formula = new Formula(stmt);

        List<String> actual = LanguageFormatter.collectOrderedVariables(formula.theFormula);

        List<String> expected = Lists.newArrayList("?event", "?he");

        assertEquals(expected, actual);
    }

    @Test
    public void testCollectOrderedVariablesWithFormula3()  {
        String stmt =   "(names \"John\" ?H)";
        Formula formula = new Formula(stmt);

        List<String> actual = LanguageFormatter.collectOrderedVariables(formula.theFormula);

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
        List<String> actual = LanguageFormatter.collectOrderedVariables(stmt);

        List<String> expected = Lists.newArrayList("?he", "?event");

        assertEquals(expected, actual);
    }

    @Test
    public void testCollectOrderedVariablesWithString2()  {
        String stmt =   "agent ?event ?he";
        List<String> actual = LanguageFormatter.collectOrderedVariables(stmt);

        List<String> expected = Lists.newArrayList("?event", "?he");

        assertEquals(expected, actual);
    }

    @Test
    public void testCollectOrderedVariablesWithString3()  {
        String stmt =   "names \"John\" ?H";
        List<String> actual = LanguageFormatter.collectOrderedVariables(stmt);

        List<String> expected = Lists.newArrayList("?H");

        assertEquals(expected, actual);
    }
}