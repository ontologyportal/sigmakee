package com.articulate.sigma.wordNet;

import com.articulate.sigma.UnitTestBase;
import com.articulate.sigma.wordNet.WordNet;
import com.articulate.sigma.wordNet.WordNetUtilities;
import org.junit.Test;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// TODO: Test the WordNet class more thoroughly. Start with the test methods called in main( ).

public class WordNetTest extends UnitTestBase {

    /****************************************************************
     */
    @Test
    public void testVerbRootFormGoing()  {

        String actual = WordNet.wn.verbRootForm("going", "going");
        String expected = "go";
        assertEquals(expected, actual);
    }

    /****************************************************************
     */
    @Test
    public void testVerbRootFormDriving()  {

        String actual = WordNet.wn.verbRootForm("driving", "driving");
        String expected = "drive";
        assertEquals(expected, actual);
    }

    /****************************************************************
     */
    @Test
    public void testGetSingularFormGo()  {

        String actual = WordNetUtilities.verbPlural("go");
        String expected = "goes";
        assertEquals(expected, actual);
    }

    /****************************************************************
     */
    @Test
    public void testGetSingularFormDrive()  {

        String actual = WordNetUtilities.verbPlural("drive");
        String expected = "drives";
        assertEquals(expected, actual);
    }

    /****************************************************************
     */
    @Test
    public void testIsValidKey()  {

        assertTrue(WordNetUtilities.isValidKey("stick_together_VB_1"));
    }

    /** ***************************************************************
     */
    @Test
    public void checkWordsToSenses() {

        List<String> runs = WordNet.wn.wordsToSenseKeys.get("run");
        System.out.println("run " + runs);
        assertTrue(runs.contains("run_NN_7"));
        System.out.println("TV " + WordNet.wn.wordsToSenseKeys.get("TV"));
        System.out.println("tv " + WordNet.wn.wordsToSenseKeys.get("tv"));
        System.out.println("106277280 " + WordNet.wn.synsetsToWords.get("106277280"));
        System.out.println("106277280 " + WordNet.wn.reverseSenseIndex.get("106277280"));
        System.out.println("court " + WordNet.wn.wordsToSenseKeys.get("court"));
        System.out.println("state " + WordNet.wn.wordsToSenseKeys.get("state"));
        System.out.println("labor " + WordNet.wn.wordsToSenseKeys.get("labor"));
        System.out.println("phase " + WordNet.wn.wordsToSenseKeys.get("phase"));
        System.out.println("craft " + WordNet.wn.wordsToSenseKeys.get("craft"));
    }
}