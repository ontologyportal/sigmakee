package com.articulate.sigma.nlg;

import com.articulate.sigma.SigmaTestBase;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.UnitTestBase;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * LanguageFormatter tests NOT targeted toward the htmlParaphrase( ) method.
 * See LanguageFormatterHtmlParaphraseTest for tests that invoke this method.
 */
public class LanguageFormatterTest extends UnitTestBase {

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
        HashMap<String, Set<String>> instanceMap = Maps.newHashMap();
        instanceMap.put("?D", Sets.newHashSet("Process"));
        instanceMap.put("?H", Sets.newHashSet("AutonomousAgent"));
        HashMap<String, Set<String>> classMap = Maps.newHashMap();

        String expected = "there exist &%Process$\"a  process\" and &%AutonomousAgent$\"an agent\" such that &%Process$\"the process\" is an &%instance$\"instance\" of &%Driving$\"driving\" and &%AutonomousAgent$\"the agent\" is an &%instance$\"instance\" of &%Human$\"human\" and &%AutonomousAgent$\"the agent\" is an &%agent$\"agent\" of &%Process$\"the process\"";

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
        actual = StringUtil.filterHtml(actual);

        assertEquals("if Socrates is a man, then Socrates is mortal", actual);

        actual = formatter.generateFormalNaturalLanguage(translations, "=>", true);
        actual = StringUtil.filterHtml(actual);

        assertEquals("Socrates is mortal and ~{Socrates is a man}", actual);
    }

    @Test
    public void testGenerateFormalNaturalLanguageIfAndOnlyIf() {
        LanguageFormatter formatter = new LanguageFormatter("", SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        List<String> translations = Lists.newArrayList("Socrates is a man", "Socrates is mortal");
        String actual = formatter.generateFormalNaturalLanguage(translations, "<=>", false);
        actual = StringUtil.filterHtml(actual);

        assertEquals("Socrates is a man if and only if Socrates is mortal", actual);

        actual = formatter.generateFormalNaturalLanguage(translations, "<=>", true);
        actual = StringUtil.filterHtml(actual);

        assertEquals("Socrates is mortal or ~{ Socrates is a man } or Socrates is a man or ~{ Socrates is mortal }", actual);
    }

    @Test
    public void testGenerateFormalNaturalLanguageAnd() {
        LanguageFormatter formatter = new LanguageFormatter("", SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        List<String> translations = Lists.newArrayList("Socrates is a man", "Socrates is mortal");
        String actual = formatter.generateFormalNaturalLanguage(translations, "and", false);
        actual = StringUtil.filterHtml(actual);

        assertEquals("Socrates is a man and Socrates is mortal", actual);

        actual = formatter.generateFormalNaturalLanguage(translations, "and", true);
        actual = StringUtil.filterHtml(actual);

        assertEquals("~{ Socrates is a man } or ~{ Socrates is mortal }", actual);
    }

    @Test
    public void testGenerateFormalNaturalLanguageOr() {
        LanguageFormatter formatter = new LanguageFormatter("", SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");

        List<String> translations = Lists.newArrayList("Socrates is a man", "Socrates is mortal");
        String actual = formatter.generateFormalNaturalLanguage(translations, "or", false);
        actual = StringUtil.filterHtml(actual);

        assertEquals("Socrates is a man or Socrates is mortal", actual);

        actual = formatter.generateFormalNaturalLanguage(translations, "or", true);
        actual = StringUtil.filterHtml(actual);

        assertEquals("Socrates is a man and Socrates is mortal", actual);
    }

    /**
     * Test various LanguageFormatter methods that are employed in "informal" NLG.
     */
    @Test
    public void testInformalNLGWithHtml() {
        String form = "<ul><li>if ?H drives,</li><li>then ?H sees</li></ul>";

        // Verify variableReplace( ).
        Map<String, Set<String>> instanceMap = Maps.newHashMap(ImmutableMap.of("?S", Sets.newHashSet("Seeing"),
                "?H", Sets.newHashSet("Human"), "?D", Sets.newHashSet("Driving")));
        HashMap<String, Set<String>> classMap = Maps.newHashMap();

        String expected = "<ul><li>if &%Human$\"a  human\" drives,</li><li>then &%Human$\"the human\" sees</li></ul>";
        String variableReplaceOutput = LanguageFormatter.variableReplace(form, instanceMap, classMap, SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expected, variableReplaceOutput);

        // Verify resolveFormatSpecifiers( ).
        expected = "<ul><li>if <a href=\"&term=Human\">a  human</a> drives,</li><li>then <a href=\"&term=Human\">the human</a> sees</li></ul>";
        String resolveFormatSpecifiersOutput = NLGUtils.resolveFormatSpecifiers(variableReplaceOutput, "");
        assertEquals(expected, resolveFormatSpecifiersOutput);
    }

    /**
     * Test various LanguageFormatter methods that are employed in "informal" NLG.
     */
    @Test
    public void testInformalNLGWithoutHtml() {
        String form = "if ?H drives, then ?H sees";

        // Verify variableReplace( ).
        Map<String, Set<String>> instanceMap = Maps.newHashMap(ImmutableMap.of("?S", Sets.newHashSet("Seeing"),
                "?H", Sets.newHashSet("Human"), "?D", Sets.newHashSet("Driving")));
        HashMap<String, Set<String>> classMap = Maps.newHashMap();

        String expected = "if &%Human$\"a  human\" drives, then &%Human$\"the human\" sees";
        String variableReplaceOutput = LanguageFormatter.variableReplace(form, instanceMap, classMap, SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expected, variableReplaceOutput);

        // Verify resolveFormatSpecifiers( ).
        expected = "if a human drives, then the human sees";
        String resolveFormatSpecifiersOutput = NLGUtils.resolveFormatSpecifiers(variableReplaceOutput, "");
        assertEquals(expected, StringUtil.filterHtml(resolveFormatSpecifiersOutput));
    }

}