package com.articulate.sigma;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * LanguageFormatter tests NOT targeted toward the htmlParaphrase( ) method.
 * See LanguageFormatterHtmlParaphraseTest for tests that invokce this method.
 */
public class LanguageFormatterTest extends SigmaTestBase {

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

    @Test
    public void testFilterHtml()   {
        String input = "<ul><li>if for all <a href=\"&term=Entity\">an entity</a> <a href=\"&term=Entity\">the entity</a> is an <a href=\"&term=element\">element</a> of <a href=\"&term=Set\">a  set</a> if and only if <a href=\"&term=Entity\">the entity</a> is an <a href=\"&term=element\">element</a> of <a href=\"&term=Set\">another set</a>,</li><li>then <a href=\"&term=Set\">the set</a> is <a href=\"&term=equal\">equal</a> to <a href=\"&term=Set\">the other set</a></li></ul>";
        String actual = LanguageFormatter.filterHtml(input);
        String expected = "if for all an entity the entity is an element of a set if and only if the entity is an element of another set, " +
                "then the set is equal to the other set";
        assertEquals(expected, actual);
    }

    @Test
    public void testStatementParse() {
        String input = "(exists (?D ?H) (and (instance ?D Driving) (instance ?H Human) (agent ?D ?H)))";
        LanguageFormatter lf = new LanguageFormatter(input, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage");
        String actual = lf.nlStmtPara(input, false, 0);
        assertEquals("", actual);
    }

    @Test
    public void testVariableReplace() {
        String form = "there exist ?D and ?H such that ?D is an &%instance$\"instance\" of &%Driving$\"driving\" and ?H is an &%instance$\"instance\" of &%Human$\"human\" and ?H is an &%agent$\"agent\" of ?D";
        HashMap<String, HashSet<String>> instanceMap = Maps.newHashMap();
        instanceMap.put("?D", Sets.newHashSet("Entity", "Process"));
        instanceMap.put("?H", Sets.newHashSet("Entity", "Agent"));
        HashMap<String, HashSet<String>> classMap = Maps.newHashMap();

        String expected = "there exist &%Process$\"a  process\" and &%Agent$\"an agent\" such that &%Process$\"the process\" is an &%instance$\"instance\" of &%Driving$\"driving\" and &%Agent$\"the agent\" is an &%instance$\"instance\" of &%Human$\"human\" and &%Agent$\"the agent\" is an &%agent$\"agent\" of &%Process$\"the process\"";

        String actual = LanguageFormatter.variableReplace(form, instanceMap, classMap, kb, "EnglishLanguage");

        assertEquals(expected, actual);
    }

}