package com.articulate.sigma.nlg;

import com.articulate.sigma.UnitTestBase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class NLGReadabilityTest extends UnitTestBase {

    @Test
    public void readabilityCommaList_preservesAnnotatedTerms_andRewritesAndChain() {

        String t = "&%Jane7_1$\"Jane7_1\" is a &%mother$\"mother\" of &%Bill7_1$\"Bill7_1\" and " +
                "&%Jane7_1$\"Jane7_1\" is a &%mother$\"mother\" of &%Bob7_1$\"Bob7_1\" and " +
                "&%Jane7_1$\"Jane7_1\" is a &%mother$\"mother\" of &%Sue7_1$\"Sue7_1\"";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.HTML, "EnglishLanguage");

        assertEquals("&%Jane7_1$\"Jane7_1\" is a &%mother$\"mother\" of &%Bill7_1$\"Bill7_1\", " +
                "&%Jane7_1$\"Jane7_1\" is a &%mother$\"mother\" of &%Bob7_1$\"Bob7_1\", and " +
                "&%Jane7_1$\"Jane7_1\" is a &%mother$\"mother\" of &%Sue7_1$\"Sue7_1\"", out);
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
    public void readabilityChunking_longAndChain_htmlBullets() {

        String t =
                "&%A$\"A\" and &%B$\"B\" and &%C$\"C\" and &%D$\"D\" and &%E$\"E\" and &%F$\"F\" and &%G$\"G\"";

        String out = NLGReadability.improveTemplate(t, LanguageFormatter.RenderMode.HTML, "EnglishLanguage");

        assertEquals("All of the following hold:<ul>" +
                "<li>&%A$\"A\"</li>" +
                "<li>&%B$\"B\"</li>" +
                "<li>&%C$\"C\"</li>" +
                "<li>&%D$\"D\"</li>" +
                "<li>&%E$\"E\"</li>" +
                "<li>&%F$\"F\"</li>" +
                "<li>&%G$\"G\"</li>" +
                "</ul>", out);
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



}
