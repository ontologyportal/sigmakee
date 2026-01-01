package com.articulate.sigma.nlg;

import com.articulate.sigma.Formula;
import com.articulate.sigma.SigmaTestBase;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.UnitTestBase;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * LanguageFormatter tests NOT targeted toward the htmlParaphrase( ) method.
 * See LanguageFormatterHtmlParaphraseTest for tests that invoke this method.
 */
public class LanguageFormatterTest extends UnitTestBase {

    @Before
    public void startUp() {
        LanguageFormatter.outputMap.clear();
    }

    private LanguageFormatter newLF() {
        return new LanguageFormatter("stmt",
                kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb,
                "EnglishLanguage");
    }

    private static int countOccurrences(String s, String sub) {
        int count = 0;
        int idx = 0;
        while (true) {
            idx = s.indexOf(sub, idx);
            if (idx < 0) break;
            count++;
            idx += sub.length();
        }
        return count;
    }

    @Test
    public void testStatementParse() {
        String input = "(exists (?D ?H) (and (instance ?D Driving) (instance ?H Human) (agent ?D ?H)))";
        LanguageFormatter lf = new LanguageFormatter(input, SigmaTestBase.kb.getFormatMap("EnglishLanguage"), SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        String actual = lf.paraphraseStatement(input, false, false, 0);
        String expected = "there exist ?D and ?H such that ?D is an &%instance$\"instance\" of &%Driving$\"driving\" and ?H is an &%instance$\"instance\" of &%Human$\"human\" and ?H is an &%agent$\"agent\" of ?D";
        assertEquals(expected, actual);
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

        assertEquals("Socrates is a man and ~{ Socrates is mortal }", actual);
    }

    @Test
    @Ignore
    // Deprecated Behavior (Replaced)
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

        assertEquals("~{ Socrates is a man } and ~{ Socrates is mortal }", actual);
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
        Map<String, Set<String>> classMap = Maps.newHashMap();

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
        Map<String, Set<String>> classMap = Maps.newHashMap();

        String expected = "if &%Human$\"a  human\" drives, then &%Human$\"the human\" sees";
        String variableReplaceOutput = LanguageFormatter.variableReplace(form, instanceMap, classMap, SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expected, variableReplaceOutput);

        // Verify resolveFormatSpecifiers( ).
        expected = "if a human drives, then the human sees";
        String resolveFormatSpecifiersOutput = NLGUtils.resolveFormatSpecifiers(variableReplaceOutput, "");
        assertEquals(expected, StringUtil.filterHtml(resolveFormatSpecifiersOutput));
    }

    @Test
    public void testLogicalOperator_knownOperators() {
        assertTrue(NLGUtils.logicalOperator("and"));
        assertTrue(NLGUtils.logicalOperator("or"));
        assertTrue(NLGUtils.logicalOperator("forall"));
        assertTrue(NLGUtils.logicalOperator("exists"));
        assertTrue(NLGUtils.logicalOperator("holds"));
        assertTrue(NLGUtils.logicalOperator("not"));
        assertTrue(NLGUtils.logicalOperator("=>"));
        assertTrue(NLGUtils.logicalOperator("<=>"));
    }

    @Test
    public void testLogicalOperator_falsePositives_substrings() {
        // These SHOULD be false, but the current implementation returns true.
        assertFalse("BUG: 'all' is not an operator; it's only a substring of 'forall'",
                NLGUtils.logicalOperator("all"));

        assertFalse("BUG: 'exist' is not an operator; it's only a substring of 'exists'",
                NLGUtils.logicalOperator("exist"));

        assertFalse("BUG: 'hold' is not an operator; it's only a substring of 'holds'",
                NLGUtils.logicalOperator("hold"));

        assertFalse("BUG: 'or,' is not a token/operator; it's a substring artifact",
                NLGUtils.logicalOperator("or,"));
    }

    @Test
    public void testIfHtmlMode_matchesLegacyMarkup() {
        LanguageFormatter lf = newLF();

        String out = lf.generateFormalNaturalLanguage(
                Arrays.asList("A", "B"),
                Formula.IF,
                false,
                LanguageFormatter.RenderMode.HTML
        );

        assertEquals("<ul><li>if A,</li><li>then B</li></ul>", out);
    }

    @Test
    public void testIfTextMode_plainSentence() {
        LanguageFormatter lf = newLF();

        String out = lf.generateFormalNaturalLanguage(
                Arrays.asList("A", "B"),
                Formula.IF,
                false,
                LanguageFormatter.RenderMode.TEXT
        );

        assertEquals("if A, then B", out);
    }

    @Test
    public void testIfArabicHtmlMode_wrapsRtlSpan() {
        LanguageFormatter lf = new LanguageFormatter("stmt", kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "ArabicLanguage");

        String out = lf.generateFormalNaturalLanguage(
                Arrays.asList("A", "B"),
                Formula.IF,
                false,
                LanguageFormatter.RenderMode.HTML
        );

        assertTrue(out.contains("<span dir=\"rtl\">"));
        assertTrue(out.startsWith("<ul><li>"));
        assertTrue(out.endsWith("</li></ul>"));
    }


    @Test
    public void testAndModeDoesNotMatter() {
        LanguageFormatter lf = newLF();

        String html = lf.generateFormalNaturalLanguage(
                Arrays.asList("A", "B"),
                Formula.AND,
                false,
                LanguageFormatter.RenderMode.HTML
        );
        String text = lf.generateFormalNaturalLanguage(
                Arrays.asList("A", "B"),
                Formula.AND,
                false,
                LanguageFormatter.RenderMode.TEXT
        );

        assertEquals(html, text);
    }

    @Test
    public void testAndNegationUsesNormalizedWrapperAndOrJoin() {
        LanguageFormatter lf = newLF();

        String out = lf.generateFormalNaturalLanguage(
                Arrays.asList("A", "B"),
                Formula.AND,
                true,
                LanguageFormatter.RenderMode.TEXT
        );

        // ¬(A ∧ B) rendered (current behavior) as "~{ A } or ~{ B }"
        assertEquals("~{ A } or ~{ B }", out);
        assertFalse(out.contains("~{A}"));     // no missing spaces
        assertFalse(out.contains("~{  "));     // no double spaces
    }

    // ¬(A → B) ≡ ¬(¬A ∨ B) ≡ A ∧ ¬B
    @Test
    public void testIfNegationUsesNormalizedWrapper() {
        LanguageFormatter lf = newLF();

        String out = lf.generateFormalNaturalLanguage(
                Arrays.asList("A", "B"),
                Formula.IF,
                true,
                LanguageFormatter.RenderMode.TEXT
        );

        assertTrue(out.contains("~{ B }"));
        assertFalse(out.contains("~{B}"));
        assertTrue(out.startsWith("A and"));
    }

    // ¬( A ↔ B ) ≡ A xor B
    @Test
    public void testIffNegationUsesNormalizedWrapper() {
        LanguageFormatter lf = newLF();

        String out_neg_iff = lf.generateFormalNaturalLanguage(
                Arrays.asList("A", "B"),
                Formula.IFF,
                true,
                LanguageFormatter.RenderMode.TEXT
        );

        String out_xor = lf.generateFormalNaturalLanguage(
                Arrays.asList("A", "B"),
                Formula.IFF,
                true,
                LanguageFormatter.RenderMode.TEXT
        );

        assertEquals(out_neg_iff,out_xor);
    }

    @Test
    public void testUquantNegationNoLeadingSpaceAndUsesBodyVerbatim() {
        LanguageFormatter lf = newLF();

        // Body includes spaces and negation wrapper; translateWord() must NOT try to remap it.
        String body = "some complex body ~{ X }";
        String out = lf.generateFormalNaturalLanguage(
                Arrays.asList("?X", body),
                Formula.UQUANT,
                true,
                LanguageFormatter.RenderMode.TEXT
        );

        // No leading whitespace.
        assertFalse("Output must not start with a space", out.startsWith(" "));

        // Begins with "not for all"
        assertTrue(out.startsWith("not for all"));

        // Body must appear unchanged.
        assertTrue("Body must be used verbatim (tArgs), not re-translated", out.contains(body));
    }

    @Test
    public void testJoinSpacingIsConsistent() {
        LanguageFormatter lf = newLF();

        String out = lf.generateFormalNaturalLanguage(
                Arrays.asList("A", "B", "C"),
                Formula.AND,
                false,
                LanguageFormatter.RenderMode.TEXT
        );

        assertEquals("A and B and C", out);
        assertFalse(out.contains("  "));
    }

    /**
     * BUG: ¬(A -> B) is rendered as "B and ~{ A }"
     * Correct would be: "A and ~{ B }"
     */
    @Test
    public void testBug_notImplicationRenderedBackwards() {
        LanguageFormatter lf = newLF();

        String out = lf.generateFormalNaturalLanguage(
                Arrays.asList("A", "B"),
                Formula.IF,
                true,
                LanguageFormatter.RenderMode.TEXT
        );

        assertNotEquals("B and ~{ A }", out); // Previous Behavior
        assertEquals("A and ~{ B }", out);
    }

    /**
     * BUG: ¬(A or B) is rendered as "A and B" (operands are not negated)
     * Correct would be: "~{ A } and ~{ B }"
     */
    @Test
    public void testBug_notOrMissingOperandNegation() {
        LanguageFormatter lf = newLF();

        String out = lf.generateFormalNaturalLanguage(
                Arrays.asList("A", "B"),
                Formula.OR,
                true,
                LanguageFormatter.RenderMode.TEXT
        );

        assertNotEquals("A and B", out); // Previous Behavior
        assertEquals("~{ A } and ~{ B }", out);
    }

    /**
     * BUG: XOR is rendered using OR (currently join with OR keyword).
     * Correct would use the XOR keyword: "A xor B"
     */
    @Test
    public void testBug_xorRenderedAsOr() {
        LanguageFormatter lf = newLF();

        String out = lf.generateFormalNaturalLanguage(
                Arrays.asList("A", "B"),
                Formula.XOR,
                false,
                LanguageFormatter.RenderMode.TEXT
        );

        assertNotEquals("A or B", out); // Previous Behavior
    }

    /**
     * BUG: ¬(A iff B) is rendered as a 4-disjunct OR expression:
     *    B or ~{ A } or A or ~{ B }
     * Correct would be XOR: "A xor B"
     */
    @Test
    public void testBug_notIffRenderedAsFourWayOr() {
        LanguageFormatter lf = newLF();

        String out = lf.generateFormalNaturalLanguage(
                Arrays.asList("A", "B"),
                Formula.IFF,
                true,
                LanguageFormatter.RenderMode.TEXT
        );

        assertNotEquals("B or ~{ A } or A or ~{ B }", out); //Previous Behavior
    }

    @Test
    public void testXorTextMode_userFriendlyBinary() {
        LanguageFormatter lf = newLF();

        // Use the fallback key "xor" explicitly to avoid dependence on Formula.XOR.
        String out = lf.generateFormalNaturalLanguage(
                Arrays.asList("A", "B"),
                Formula.XOR,
                false,
                LanguageFormatter.RenderMode.TEXT
        );

        assertEquals("either A or B, but not both", out);
        assertFalse(out.toLowerCase().contains(" xor ")); // should not expose token
    }

    @Test
    public void testNegatedIffRendersAsUserFriendlyXor() {
        LanguageFormatter lf = newLF();

        String out = lf.generateFormalNaturalLanguage(
                Arrays.asList("A", "B"),
                Formula.IFF,
                true,
                LanguageFormatter.RenderMode.TEXT
        );

        assertEquals("either A or B, but not both", out);
        assertFalse(out.toLowerCase().contains(" xor "));
    }

    @Test
    public void testXorWithSingleArgument_degeneratesToArgument() {
        LanguageFormatter lf = newLF();

        String out = lf.generateFormalNaturalLanguage(
                Arrays.asList("A"),
                Formula.XOR,
                false,
                LanguageFormatter.RenderMode.TEXT
        );

        assertEquals("A", out);
    }

    @Test
    public void testXorThreeArgs_expandsToParityDNF_expectedForm() {
        LanguageFormatter lf = newLF();

        String out = lf.generateFormalNaturalLanguage(
                Arrays.asList("A", "B", "C"),
                Formula.XOR,
                false,
                LanguageFormatter.RenderMode.TEXT
        );

        // Exact expected DNF (order depends on mask iteration: 001,010,100,111)
        // Must contain all odd-parity minterms
        assertTrue(out.contains("A and ~{ B } and ~{ C }"));     // 100
        assertTrue(out.contains("~{ A } and B and ~{ C }"));     // 010
        assertTrue(out.contains("~{ A } and ~{ B } and C"));     // 001
        assertTrue(out.contains("A and B and C"));               // 111

        // Must have exactly 4 disjuncts (3 separators)
        assertEquals(3, countOccurrences(out, " or "));

        // Should not expose raw xor token
        assertFalse(out.toLowerCase().contains(" xor "));

        // Optional: ensure an even-parity minterm isn't present
        assertFalse(out.contains("~{ A } and ~{ B } and ~{ C }"));  // 000
    }

    @Test
    public void testFormalNLG_doesNotTranslateComposedArgs() {
        LanguageFormatter lf = newLF();

        // This string must remain unchanged; it is already rendered.
        String clause = "A and ~{ B }";

        String out = lf.generateFormalNaturalLanguage(
                Arrays.asList(clause, "C"),
                Formula.OR,
                false,
                LanguageFormatter.RenderMode.TEXT
        );

        assertTrue(out.contains(clause));
    }

    @Test
    public void testFormalNLG_doubleTranslation_canCorruptAlreadyParaphrasedArg() {

        // Start from the real maps so the formatter has normal behavior.
        Map<String,String> phraseMap = new HashMap<>(kb.getFormatMap("EnglishLanguage"));
        Map<String,String> termMap   = new HashMap<>(kb.getTermFormatMap("EnglishLanguage"));

        // This is the "already paraphrased" clause we will pass as an argument to OR.
        String paraphrasedClause = "A and ~{ B }";

        // Inject a malicious/accidental entry to show why translating again is dangerous.
        termMap.put(paraphrasedClause, "CORRUPTED");

        LanguageFormatter lf = new LanguageFormatter(
                "stmt",
                phraseMap,
                termMap,
                kb,
                "EnglishLanguage"
        );

        String out = lf.generateFormalNaturalLanguage(
                Arrays.asList(paraphrasedClause, "C"),
                Formula.OR,
                false,
                LanguageFormatter.RenderMode.TEXT
        );

        // Current buggy behavior (double-translation) will remap the whole clause.
        assertNotEquals("CORRUPTED or C", out); // previous behavior
        assertEquals("A and ~{ B } or C", out); // current behavior
    }

    @Test
    public void testBug_processAtom_doubleWrapsAlreadyAnnotatedTerm() {

        Map<String,String> termMap = new HashMap<>();
        termMap.put("Human", "human");

        // First call produces an annotated term.
        String once = LanguageFormatter.processAtom("Human", termMap);
        assertEquals("&%Human$\"human\"", once);

        // Old buggy behavior: calling processAtom() again on the already-annotated string
        // treated it like a normal atom and wrapped it again.
        String twice = LanguageFormatter.processAtom(once, termMap);

        // This is the *bug*: double-annotation.
        assertNotEquals("&%&%Human$\"human\"$\"&%Human$\"human\"\"", twice); // Previous Behavior
        assertEquals("&%Human$\"human\"", twice); // Current Behavior
    }


}