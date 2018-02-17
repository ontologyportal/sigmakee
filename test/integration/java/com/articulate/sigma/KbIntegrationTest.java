package com.articulate.sigma;

import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.Assert.*;

public class KbIntegrationTest extends IntegrationTestBase {

    /** *************************************************************
     */
    @Test
    public void testIsChildOf3() {

        KBcache cache = SigmaTestBase.kb.kbCache;
        System.out.println("parents of Shirt (as instance): " + cache.getParentClassesOfInstance("Shirt"));
        System.out.println("parents of Shirt: " + cache.parents.get("subclass").get("Shirt"));
        System.out.println("KBcache.childOfP(subclass, WearableItem, Shirt): " + cache.childOfP("subclass", "WearableItem","Shirt"));
        System.out.println("SigmaTestBase.kb.isChildOf(Shirt, WearableItem): " + SigmaTestBase.kb.isChildOf("Shirt", "WearableItem"));
        System.out.println("SigmaTestBase.kb.childOf(Shirt, WearableItem): " + SigmaTestBase.kb.childOf("Shirt", "WearableItem"));
        assertTrue(SigmaTestBase.kb.isSubclass("Shirt", "WearableItem"));
    }

    /** *************************************************************
     */
    @Test
    public void testAskWithTwoRestrictionsDirect2() {
        ArrayList<Formula> actual = SigmaTestBase.kb.askWithTwoRestrictions(0, "subclass", 1, "Boy", 2, "Man");
        assertNotEquals(0, actual.size());
    }

    /** *************************************************************
     */
    @Test
    public void testIsSubclass1()   {

        KBcache cache = SigmaTestBase.kb.kbCache;
        System.out.println("parents of Boy (as instance): " + cache.getParentClassesOfInstance("Boy"));
        System.out.println("parents of Boy: " + cache.parents.get("Boy"));
        System.out.println("childOfP(\"Boy\", \"Entity\"): " + cache.childOfP("subclass", "Entity","Boy"));
        System.out.println("SigmaTestBase.kb.isChildOf(\"Boy\", \"Entity\"): " + SigmaTestBase.kb.isChildOf("Boy", "Entity"));
        assertTrue(SigmaTestBase.kb.isSubclass("Boy", "Entity"));
    }

}