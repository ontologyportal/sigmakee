package com.articulate.sigma;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class KbIntegrationTest extends SigmaTestBase {

    @Test
    public void testAskWithTwoRestrictionsDirect2() {
        ArrayList<Formula> actual = kb.askWithTwoRestrictions(0, "subclass", 1, "Boy", 2, "Man");
        assertNotEquals(0, actual.size());
    }

    @Test
    public void testIsSubclass1()   {
        assertTrue(kb.isSubclass("Boy", "Entity"));
    }

}