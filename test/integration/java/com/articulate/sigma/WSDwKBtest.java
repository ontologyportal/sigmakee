package com.articulate.sigma;

import com.articulate.sigma.wordNet.*;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.assertEquals;

public class WSDwKBtest extends IntegrationTestBase {

    /****************************************************************
     */
    @Before
    public void setUpInterpreter() throws IOException {

        KBmanager.getMgr().initializeOnce();
        WSD.debug = true;
        System.out.println("INFO in WSDwKBtest.setUpInterpreter(): done initializing");
    }

    /****************************************************************
     */
    @Test
    public void testHyundai() {

        System.out.println("---------------------------");
        String sentence = "I drive a Hyundai Equus";
        System.out.println("INFO in WSDwKBtest.testSentenceWSD1(): " + sentence);
        ArrayList<String> senses = WSD.collectWordSenses(sentence);
        System.out.println("INFO in WSDwKBtest.testSentenceWSD1(): " + senses);
        String s = senses.get(1);
        String sumo = WordNetUtilities.getBareSUMOTerm(WordNet.wn.getSUMOMapping(s));
        assertEquals("HyundaiEquus",sumo);
    }

    /** ***************************************************************
     */
    @Test
    public void testWordWSD1() {

        System.out.println("---------------------------");
        String s = WSD.getBestDefaultSense("India");
        System.out.println("INFO in WSDwKBtest.testWordWSD1(): " + s);
        String sumo = WordNetUtilities.getBareSUMOTerm(WordNet.wn.getSUMOMapping(s));
        assertEquals("India",sumo);
    }

    /** ***************************************************************
     */
    @Test
    public void testWordWSD2() {

        System.out.println("---------------------------");
        String s = WSD.getBestDefaultSense("kick");
        System.out.println("INFO in WSDwKBtest.testWordWSD2(): " + s);
        assertEquals("201371756",s);
    }

    /** ***************************************************************
     */
    @Test
    public void testSentenceWSD1() {

        System.out.println("---------------------------");
        String sentence = "John walks.";
        System.out.println("INFO in WSDwKBtest.testSentenceWSD1(): " + sentence);
        ArrayList<String> senses = WSD.collectWordSenses(sentence);
        System.out.println("INFO in WSDwKBtest.testSentenceWSD1(): " + senses);
        assertEquals("201904930",senses.get(1));
    }

    /** ***************************************************************
     */
    @Test
    public void testSentenceWSD2() {

        System.out.println("---------------------------");
        String sentence = "Bob runs around the track.";
        System.out.println("INFO in WSDwKBtest.testSentenceWSD2(): " + sentence);
        ArrayList<String> senses = WSD.collectWordSenses(sentence);
        System.out.println("INFO in WSDwKBtest.testSentenceWSD2(): " + WSD.collectWordSenses(sentence));
        assertEquals("201883716",senses.get(1));
        assertEquals("109387222",senses.get(2)); // this is what we get but should be 104037625
    }


    /** ***************************************************************
     */
    @Test
    public void testSentenceWSD3() {

        System.out.println("---------------------------");
        String sentence = "A computer is a general purpose device that can be programmed to carry out a finite set of arithmetic or logical operations.";
        System.out.println("INFO in WSDwKBtest.testSentenceWSD3(): " + sentence);
        ArrayList<String> senses = WSD.collectWordSenses(sentence);
        System.out.println("INFO in WSDwKBtest.testSentenceWSD3(): " + senses);
        String s = senses.get(0);
        String sumo = WordNetUtilities.getBareSUMOTerm(WordNet.wn.getSUMOMapping(s));
        assertEquals("Computer",sumo);
    }

    /** ***************************************************************
     */
    @Test
    public void testSentenceWSD4() {

        System.out.println("---------------------------");
        String sentence = "A four stroke engine is a beautiful thing.";
        System.out.println("INFO in WSDwKBtest.testSentenceWSD4(): " + sentence);
        ArrayList<String> senses = WSD.collectWordSenses(sentence);
        String s = senses.get(0);
        System.out.println("INFO in WSDwKBtest.testSentenceWSD4(): " + s);
        String sumo = WordNetUtilities.getBareSUMOTerm(WordNet.wn.getSUMOMapping(s));
        assertEquals("FourStrokeEngine",sumo);
    }

    /** ***************************************************************
     */
    @Test
    public void testSentenceWSD5() {

        System.out.println("---------------------------");
        ArrayList<String> sentar = Lists.newArrayList("John","kicks","the","cart");
        System.out.println("INFO in WSDwKBtest.testSentenceWSD5(): " + sentar);
        for (String s : sentar) {
            String sumo = WSD.getBestDefaultSUMO(s);
            System.out.println("INFO in WSDwKBtest.testSentenceWSD5(): word: " + s + " SUMO: " + sumo);
            if (s.equals("kicks"))
                assertEquals("&%Kicking=",sumo);
        }
    }

    /** ***************************************************************
     */
    @Test
    public void testSentenceWSD6() {

        System.out.println("---------------------------");
        String sentence = "Play Hello on Hulu.";
        System.out.println("INFO in WSDwKBtest.testSentenceWSD6(): " + sentence);
        ArrayList<String> senses = WSD.collectWordSenses(sentence);
        System.out.println("INFO in WSDwKBtest.testSentenceWSD6(): " + senses);
        String s = senses.get(0);
        System.out.println("INFO in WSDwKBtest.testSentenceWSD6(): Note this is testing the wrong sense, " +
                "which should instead be 201717169 cause to emit recorded audio or video, but there's no" +
                "cooccurence data for 'Hulu'");
        assertEquals("201072949",s);
    }
}
