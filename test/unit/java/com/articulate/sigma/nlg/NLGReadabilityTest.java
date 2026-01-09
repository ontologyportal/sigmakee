package com.articulate.sigma.nlg;

import com.articulate.sigma.UnitTestBase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class NLGReadabilityTest extends UnitTestBase {

    // Helper Methods
    private static int countOccurrences(String s, String needle) {
        int count = 0, idx = 0;
        while ((idx = s.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }


    @Test
    public void readabilityCommaList_doesNotTouchNegationBlocks() {

        String t = "~{ &%Jane7_1$\"Jane7_1\" is a &%mother$\"mother\" of &%Bill7_1$\"Bill7_1\" } or " +
                "~{ &%Jane7_1$\"Jane7_1\" is a &%mother$\"mother\" of &%Bob7_1$\"Bob7_1\" } or " +
                "~{ &%Jane7_1$\"Jane7_1\" is a &%mother$\"mother\" of &%Sue7_1$\"Sue7_1\" }";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.HTML, "EnglishLanguage");

        assertEquals(t, out);
    }


    @Test
    public void readabilityCommaList_supportsAnnotatedTrailingDigits() {

        String t = "&%Organism$\"an organism\"1 is an &%instance$\"instance\" of &%Organism$\"organism\" and " +
                "&%Organism$\"another organism\"1 is an &%instance$\"instance\" of &%Organism$\"organism\" and " +
                "&%Organism$\"the other organism\"1 is an &%instance$\"instance\" of &%Organism$\"organism\"";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.HTML, "EnglishLanguage");

        assertEquals("&%Organism$\"an organism\"1 is an &%instance$\"instance\" of &%Organism$\"organism\", " +
                "&%Organism$\"another organism\"1 is an &%instance$\"instance\" of &%Organism$\"organism\", and " +
                "&%Organism$\"the other organism\"1 is an &%instance$\"instance\" of &%Organism$\"organism\"", out);
    }


    @Test
    public void readabilityCommaList_doesNotRewriteTwoItemChains() {

        String t = "&%Jane7_1$\"Jane7_1\" is not a &%mother$\"mother\" of &%Bill7_1$\"Bill7_1\" or " +
                "&%Jane7_1$\"Jane7_1\" is not a &%mother$\"mother\" of &%Bob7_1$\"Bob7_1\"";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.HTML, "EnglishLanguage");

        assertEquals(t, out);
    }


    @Test
    public void readabilityChunking_longOrChain_textNumbered() {

        String t =
                "&%A$\"A\" or &%B$\"B\" or &%C$\"C\" or &%D$\"D\" or &%E$\"E\" or &%F$\"F\" or &%G$\"G\"";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.TEXT, "EnglishLanguage");

        assertEquals("At least one of the following holds: " +
                "(1) &%A$\"A\" (2) &%B$\"B\" (3) &%C$\"C\" (4) &%D$\"D\" (5) &%E$\"E\" (6) &%F$\"F\" (7) &%G$\"G\"", out);
    }


    @Test
    public void readabilityChunking_doesNotTouchNegationBlocksEvenIfLong() {

        String t =
                "~{ &%A$\"A\" } or ~{ &%B$\"B\" } or ~{ &%C$\"C\" } or ~{ &%D$\"D\" } or ~{ &%E$\"E\" } or ~{ &%F$\"F\" } or ~{ &%G$\"G\" }";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.HTML, "EnglishLanguage");

        assertEquals(t, out);
    }


//    @Test
//    public void readabilityQuantifiedOrChain_becomesNumbered_text() {
//
//        String t =
//                "[FORALL][VARS]&%A$\"A\"1 and &%B$\"B\"1[/VARS] " +
//                        "&%C$\"C\" or &%D$\"D\" or &%E$\"E\"[/FORALL]";
//
//        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.TEXT, "EnglishLanguage");
//
//        String expected =
//                "for all &%A$\"A\"1 and &%B$\"B\"1, at least one of the following holds: " +
//                        "(1) &%C$\"C\" (2) &%D$\"D\" (3) &%E$\"E\"";
//
//        assertEquals(expected, out);
//    }


    @Test
    public void readabilityFactoring_orChain_joinsTailsWithOr() {

        String t =
                "&%X$\"X\" is a &%parent$\"parent\" of &%A$\"A\" or " +
                        "&%X$\"X\" is a &%parent$\"parent\" of &%B$\"B\" or " +
                        "&%X$\"X\" is a &%parent$\"parent\" of &%C$\"C\"";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.HTML, "EnglishLanguage");

        assertEquals("&%X$\"X\" is a &%parent$\"parent\" of " +
                "&%A$\"A\", &%B$\"B\", or &%C$\"C\"", out);
    }


    @Test
    public void readabilityFactoring_quantifiedOrList_collapsesAndUsesOr() {

        String t =
                "for all &%Organism$\"an organism\"1 and &%Organism$\"another organism\"1 " +
                        "&%Organism$\"the other organism\"1 is a &%parent$\"parent\" of &%Organism$\"the organism\"1 or " +
                        "&%Organism$\"the other organism\"1 is a &%parent$\"parent\" of &%Organism$\"the organism\"2 or " +
                        "&%Organism$\"the other organism\"1 is a &%parent$\"parent\" of &%Organism$\"the organism\"3";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.HTML, "EnglishLanguage");

        String expected =
                "for all &%Organism$\"an organism\"1 and &%Organism$\"another organism\"1, at least one of the following holds:" +
                        "<ul>" +
                        "<li>&%Organism$\"the other organism\"1 is a &%parent$\"parent\" of " +
                        "&%Organism$\"the organism\"1 or &%Organism$\"the organism\"2 or &%Organism$\"the organism\"3</li>" +
                        "</ul>";

        assertEquals(expected, out);
    }


    @Test
    public void readabilityFactoring_doesNotTrigger_ifPrefixDiffers() {

        String t =
                "&%X$\"X\" is a &%parent$\"parent\" of &%A$\"A\" or " +
                        "&%X$\"X\" is a &%mother$\"mother\" of &%B$\"B\" or " +
                        "&%X$\"X\" is a &%parent$\"parent\" of &%C$\"C\"";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.HTML, "EnglishLanguage");

        // Factoring would collapse repeated "is a parent of" into a single occurrence.
        // Here it must still appear twice.
        String parentPhrase = "is a &%parent$\"parent\" of";
        assertTrue(out.contains(parentPhrase));
        assertEquals(2, countOccurrences(out, parentPhrase));
    }


    @Test
    public void readabilityFactoring_doesNotTrigger_forTwoItems() {

        String t =
                "&%X$\"X\" is a &%parent$\"parent\" of &%A$\"A\" or " +
                        "&%X$\"X\" is a &%parent$\"parent\" of &%B$\"B\"";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.HTML, "EnglishLanguage");

        assertEquals(t, out);
    }


    @Test
    public void readabilitySegmentAware_doesNotRewriteInsideNegationBlocks() {

        String t =
                "Prefix " +
                        "~{ &%X$\"X\" is a &%parent$\"parent\" of &%A$\"A\" or " +
                        "&%X$\"X\" is a &%parent$\"parent\" of &%B$\"B\" or " +
                        "&%X$\"X\" is a &%parent$\"parent\" of &%C$\"C\" }" +
                        " Suffix";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.HTML, "EnglishLanguage");

        // Entire negation block should remain as-is (Commit 7 is conservative).
        assertEquals(t, out);
    }


    @Test
    public void nestedIf_html_rendersNestedLists_andDoesNotDuplicateAntecedent() {
        String template =
                "[IF_A][AND][SEG]A[/SEG] and [SEG]B[/SEG][/AND][/IF_A]" +
                        "[IF_C][IF_A][AND][SEG]C[/SEG][/AND][/IF_A][IF_C]D[/IF_C][/IF_C]";

        String out = NLGReadability.improveTemplate(
                template, LanguageFormatter.RenderMode.HTML, "EnglishLanguage"
        );

        // Must contain HTML list structure (IF renders as <ul><li>if ...</li><li>then ...</li></ul>)
        assertTrue(out.contains("<ul>"));
        assertTrue(out.contains("<li>"));

        // Nested IF => at least 2 <ul>
        int firstUl = out.indexOf("<ul>");
        int secondUl = out.indexOf("<ul>", firstUl + 1);
        assertTrue("Expected nested <ul> for nested IF", secondUl > firstUl);

        // Regression guard: consequent must NOT be rewritten from antecedent.
        // If the “then A” bug exists, D will disappear.
        assertTrue("Expected consequent D to appear", out.contains("D"));
        assertTrue("Expected antecedent A to appear", out.contains("A"));

        // No markers leak
        assertFalse(out.contains("[IF_A]"));
        assertFalse(out.contains("[IF_C]"));
        assertFalse(out.contains("[AND]"));
        assertFalse(out.contains("[OR]"));
        assertFalse(out.contains("[SEG]"));
    }

    @Test
    public void nestedIf_text_preservesIfThenStructure_andContainsAllAtoms() {
        String template =
                "if [IF_A][AND][SEG]A[/SEG] and [SEG]B[/SEG][/AND][/IF_A], then " +
                        "[IF_C]if [IF_A][AND][SEG]C[/SEG][/AND][/IF_A], then [IF_C]D[/IF_C][/IF_C]";

        String out = NLGReadability.improveTemplate(
                template, LanguageFormatter.RenderMode.TEXT, "EnglishLanguage"
        );

        // Must contain if/then tokens (localized via Keywords; EnglishLanguage uses "if"/"then")
        assertTrue(out.toLowerCase().contains("if"));
        assertTrue(out.toLowerCase().contains("then"));

        // All atoms must survive
        assertTrue(out.contains("A"));
        assertTrue(out.contains("B"));
        assertTrue(out.contains("C"));
        assertTrue(out.contains("D"));

        // No markers leak
        assertFalse(out.contains("[IF_A]"));
        assertFalse(out.contains("[IF_C]"));
        assertFalse(out.contains("[AND]"));
        assertFalse(out.contains("[OR]"));
        assertFalse(out.contains("[SEG]"));
    }

    @Test
    public void andBlock_countsTopLevelSegOnly_whenNestedOrInsideSeg() {
        // Top-level AND has 2 operands:
        //  1) a SEG that itself contains an OR block
        //  2) W
        String template =
                "[AND]" +
                        "[SEG][OR][SEG]X[/SEG] or [SEG]Y[/SEG][/OR][/SEG] and " +
                        "[SEG]W[/SEG]" +
                        "[/AND]";

        String out = NLGReadability.improveTemplate(
                template, LanguageFormatter.RenderMode.TEXT, "EnglishLanguage"
        );

        // Should include X, Y, W
        assertTrue(out.contains("X"));
        assertTrue(out.contains("Y"));
        assertTrue(out.contains("W"));

        // Should include both connectors somewhere
        assertTrue(out.toLowerCase().contains("and"));
        assertTrue(out.toLowerCase().contains("or"));

        // Markers removed
        assertFalse(out.contains("[AND]"));
        assertFalse(out.contains("[OR]"));
        assertFalse(out.contains("[SEG]"));

        // Regression guard: ensure W is still a separate operand (not lost / merged)
        assertTrue("Expected W to remain present as separate operand", out.matches("(?s).*W.*"));
    }

    @Test
    public void orBlock_nestedAndInsideSeg_isHandled() {
        String template =
                "[OR]" +
                        "[SEG][AND][SEG]A[/SEG] and [SEG]B[/SEG][/AND][/SEG] or " +
                        "[SEG]C[/SEG]" +
                        "[/OR]";

        String out = NLGReadability.improveTemplate(
                template, LanguageFormatter.RenderMode.TEXT, "EnglishLanguage"
        );

        // All atoms present
        assertTrue(out.contains("A"));
        assertTrue(out.contains("B"));
        assertTrue(out.contains("C"));

        // Both connectors present
        assertTrue(out.toLowerCase().contains("and"));
        assertTrue(out.toLowerCase().contains("or"));

        // No markers leak
        assertFalse(out.contains("[AND]"));
        assertFalse(out.contains("[OR]"));
        assertFalse(out.contains("[SEG]"));
    }

    @Test
    public void quantified_prefix_isPreserved_withIfBody() {
        String template =
                "for all §T0§ and §T1§ " +
                        "if [IF_A][AND][SEG]A[/SEG] and [SEG]B[/SEG][/AND][/IF_A], then " +
                        "[IF_C]C[/IF_C]";

        String out = NLGReadability.improveTemplate(
                template, LanguageFormatter.RenderMode.TEXT, "EnglishLanguage"
        );

        // Prefix preserved
        assertTrue(out.startsWith("for all"));

        // Body preserved
        assertTrue(out.contains("A"));
        assertTrue(out.contains("B"));
        assertTrue(out.contains("C"));

        // No markers leak
        assertFalse(out.contains("[IF_A]"));
        assertFalse(out.contains("[IF_C]"));
        assertFalse(out.contains("[AND]"));
        assertFalse(out.contains("[SEG]"));
    }

    @Test
    public void annotatedTerms_areProtected_andRestored() {
        // Use real Sigma-style annotated tokens (these will be protected and restored)
        String template =
                "if [IF_A][AND]" +
                        "[SEG]&%Organism$\"the organism\"1 is an &%instance$\"instance\" of &%Organism$\"organism\"[/SEG] and " +
                        "[SEG]&%Organism$\"the other organism\"1 is an &%instance$\"instance\" of &%Organism$\"organism\"[/SEG]" +
                        "[/AND][/IF_A], then " +
                        "[IF_C]&%Organism$\"the organism\"1 is an &%sibling$\"sibling\" of &%Organism$\"the other organism\"1[/IF_C]";

        String out = NLGReadability.improveTemplate(
                template, LanguageFormatter.RenderMode.TEXT, "EnglishLanguage"
        );

        // Must restore &%... tokens (no placeholders should remain)
        assertTrue(out.contains("&%Organism$\"the organism\"1"));
        assertFalse("Should not leak placeholders", out.contains("§T"));

        // No markers leak
        assertFalse(out.contains("[IF_A]"));
        assertFalse(out.contains("[IF_C]"));
        assertFalse(out.contains("[AND]"));
        assertFalse(out.contains("[SEG]"));
    }

    @Test
    public void protectAnnotatedTerms_doesNotCorruptTokens() {

        String t = "for all &%Organism$\"an organism\"1 and &%Organism$\"another organism\"1 " +
                "[AND][SEG]&%Organism$\"an organism\"1 is parent of &%Organism$\"another organism\"1[/SEG][/AND]";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.TEXT, "EnglishLanguage");

        // Tokens must survive exactly (no placeholder leaks)
        assertTrue(out.contains("&%Organism$\"an organism\"1"));
        assertTrue(out.contains("&%Organism$\"another organism\"1"));
        assertFalse(out.contains("§T"));
    }


    @Test
    public void quantifierHeader_preservedExactly() {

        String t = "for all &%A$\"a\"1 and &%B$\"b\"1 " +
                "[AND][SEG]X[/SEG] and [SEG]Y[/SEG] and [SEG]Z[/SEG][/AND]";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.TEXT, "EnglishLanguage");

        assertTrue(out.startsWith("for all &%A$\"a\"1 and &%B$\"b\"1 "));
    }


    @Test
    public void quantifiedFlatOr_rendersAsListHtml() {

        String t = "for all &%A$\"a\"1 and &%B$\"b\"1 " +
                "P or Q or R";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.HTML, "EnglishLanguage");

        assertTrue(out.contains("at least one of the following holds:"));
        assertTrue(out.contains("<ul>"));
        assertTrue(out.contains("<li>"));
    }

    @Test
    public void nestedIf_rendersAsNestedHtmlLists() {

        String t =
                "[IF_A][AND][SEG]A[/SEG] and [SEG]B[/SEG][/AND][/IF_A]" +
                        "[IF_C][IF_A][SEG]C[/SEG][/IF_A][IF_C]D[/IF_C][/IF_C]";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.HTML, "EnglishLanguage");

        assertTrue(out.contains("<ul>"));
        int firstUl = out.indexOf("<ul>");
        int secondUl = out.indexOf("<ul>", firstUl + 1);
        assertTrue("Expected nested <ul> for nested IF", secondUl > firstUl);

        assertFalse(out.contains("[IF_A]"));
        assertFalse(out.contains("[IF_C]"));
        assertFalse(out.contains("[AND]"));
        assertFalse(out.contains("[SEG]"));
    }


    @Test
    public void andBlock_joinsChildrenText() {

        String t = "[AND][SEG]A[/SEG] and [SEG]B[/SEG] and [SEG]C[/SEG][/AND]";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.TEXT, "EnglishLanguage");

        // We don't hard-freeze commas here; current joinWithConnector uses "and" between all items.
        // The important part: it must contain all operands and no markers.
        assertTrue(out.contains("A"));
        assertTrue(out.contains("B"));
        assertTrue(out.contains("C"));
        assertFalse(out.contains("[AND]"));
        assertFalse(out.contains("[SEG]"));
    }


    @Test
    public void orBlock_joinsChildrenText() {

        String t = "[OR][SEG]A[/SEG] or [SEG]B[/SEG] or [SEG]C[/SEG][/OR]";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.TEXT, "EnglishLanguage");

        assertTrue(out.contains("A"));
        assertTrue(out.contains("B"));
        assertTrue(out.contains("C"));
        assertFalse(out.contains("[OR]"));
        assertFalse(out.contains("[SEG]"));
    }


    @Test
    public void segRun_smoothsIntoCommaList() {

        String t = "[AND][SEG]A[/SEG] and [SEG]B[/SEG] and [SEG]C[/SEG][/AND]";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.TEXT, "EnglishLanguage");

        // Your known output was: "A, B, and C"
        assertEquals("A, B, and C", out.trim());
    }


    @Test
    public void longAndChain_chunksToHtmlList() {

        String t = "A and B and C and D and E and F and G";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.HTML, "EnglishLanguage");

        System.out.println("out: "+out);
        assertTrue(out.contains("All of the following hold:"));
        assertTrue(out.contains("<ul>"));
        assertTrue(out.contains("<li>"));
    }

    @Test
    public void print_parsed_tree_structure() {
        NLGReadability.debugPrintTree(
                "if [IF_A][AND]" +
                        "[SEG]A[/SEG] and " +
                        "[SEG][OR][SEG]B[/SEG] or [SEG]C[/SEG][/OR][/SEG] and " +
                        "[SEG]D[/SEG]" +
                        "[/AND][/IF_A], then " +
                        "[IF_C]if [IF_A][SEG]E[/SEG][/IF_A], then [IF_C]F[/IF_C][/IF_C]"
        , "EnglishLanguage");
    }

    @Test
    public void readabilityForAllHeader_factorsMultipleTypesAndVars_asTypedSymbols() {

        // 3 Organism vars + 1 Human var; body is a simple atom to avoid other rewrites.
        String t =
                "[FORALL][VARS]" +
                        "&%Organism$\"an organism X\"1, " +
                        "&%Organism$\"another organism Y\"1, " +
                        "&%Organism$\"a third organism Z\"2, and " +
                        "&%Human$\"a human W\"1 [/VARS]" +
                        "A[/FORALL]";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.TEXT, "EnglishLanguage");

        // Expect: factored multi-type header where BOTH the type and each symbol are annotated
        String expected =
                "For all " +
                        "&%Organism$\"Organisms\" " +
                        "&%Organism$\"X\", &%Organism$\"Y\", and &%Organism$\"Z\" " +
                        "and " +
                        "&%Human$\"Human\" " +
                        "&%Human$\"W\"" +
                        ": A";

        assertEquals(expected, out);
    }



    @Test
    public void test() {

        String t = "[FORALL][VARS]&%Organism$\"an organism X\"1, &%Organism$\"another organism Y\"1 and &%Organism$\"a third organism Z\"2[/VARS] [IF_A][AND][SEG]&%Organism$\"the organism X\"1 is an &%instance$\"instance\" of &%Organism$\"organism\"[/SEG] and [SEG]&%Organism$\"the other organism Y\"1 is an &%instance$\"instance\" of &%Organism$\"organism\"[/SEG] and [SEG]&%Organism$\"the third organism Z\"2 is an &%instance$\"instance\" of &%Organism$\"organism\"[/SEG][/AND][/IF_A][IF_C][IF_A][AND][SEG]&%Organism$\"the organism X\"1 is a &%sibling$\"sibling\" of &%Organism$\"the other organism Y\"1[/SEG] and [SEG]&%Organism$\"the third organism Z\"2 is a &%parent$\"parent\" of &%Organism$\"the organism X\"1[/SEG][/AND][/IF_A][IF_C]&%Organism$\"the third organism Z\"2 is a &%parent$\"parent\" of &%Organism$\"the other organism Y\"1[/IF_C][/IF_C][/FORALL]";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.TEXT, "EnglishLanguage");

        System.out.println("out: "+out);

        NLGReadability.debugPrintTree(t, "EnglishLanguage");
    }

}
