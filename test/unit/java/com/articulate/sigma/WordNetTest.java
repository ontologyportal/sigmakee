package com.articulate.sigma;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

// TODO: Test the WordNet class more thoroughly. Start with the test methods called in main( ).

public class WordNetTest extends UnitTestBase {


    @Test
    public void testVerbRootFormGoing()  {
        String actual = WordNet.wn.verbRootForm("going", "going");
        String expected = "go";
        assertEquals(expected, actual);
    }

    @Test
    public void testVerbRootFormDriving()  {
        String actual = WordNet.wn.verbRootForm("driving", "driving");
        String expected = "drive";
        assertEquals(expected, actual);
    }

    @Test
    public void testGetSingularFormGo()  {
        String actual = WordNetUtilities.verbPlural("go");
        String expected = "goes";
        assertEquals(expected, actual);
    }

    @Test
    public void testGetSingularFormDrive()  {
        String actual = WordNetUtilities.verbPlural("drive");
        String expected = "drives";
        assertEquals(expected, actual);
    }

}