package com.articulate.sigma;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class KBTest extends UnitTestBase {

    @Test
    public void testAskWithTwoRestrictionsDirect1() {
        ArrayList<Formula> actual = SigmaTestBase.kb.askWithTwoRestrictions(0, "subclass", 1, "Driving", 2, "Guiding");
        assertNotEquals(0, actual.size());
    }

    /**
     * Fails because askWithTwoRestrictions does not go up the class hierarchy.
     */
    @Test
    public void testAskWithTwoRestrictionsIndirect1() {
        ArrayList<Formula> actual = SigmaTestBase.kb.askWithTwoRestrictions(0, "subclass", 1, "Driving", 2, "Process");
        assertEquals(0, actual.size());
    }

    /**
     * Fails because askWithTwoRestrictions does not go up the class hierarchy.
     */
    @Test
    public void testAskWithTwoRestrictionsIndirect2() {
        ArrayList<Formula> actual = SigmaTestBase.kb.askWithTwoRestrictions(0, "subclass", 1, "Boy", 2, "Entity");
        assertEquals(0, actual.size());
    }

    @Test
    public void testIsSubclass2()   {
        assertTrue(SigmaTestBase.kb.isSubclass("Driving", "Process"));
    }
}