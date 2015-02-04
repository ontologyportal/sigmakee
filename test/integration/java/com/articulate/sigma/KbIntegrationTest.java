package com.articulate.sigma;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class KbIntegrationTest extends IntegrationTestBase {

    @Test
    public void testAskWithTwoRestrictionsDirect2() {
        ArrayList<Formula> actual = SigmaTestBase.kb.askWithTwoRestrictions(0, "subclass", 1, "Boy", 2, "Man");
        assertNotEquals(0, actual.size());
    }

    @Test
    public void testIsSubclass1()   {
        assertTrue(SigmaTestBase.kb.isSubclass("Boy", "Entity"));
    }

}