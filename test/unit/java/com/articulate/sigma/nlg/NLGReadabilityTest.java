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


}
