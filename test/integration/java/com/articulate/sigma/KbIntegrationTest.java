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

    /** ***************************************************************
     */
    @Test
    public void testIsHigherOrder() {

        String stmt;
        stmt = "(=> (and (instance ?GUN Gun) (effectiveRange ?GUN ?LM) " +
                "(distance ?GUN ?O ?LM1) (instance ?O Organism) (not (exists (?O2) " +
                "(between ?O ?O2 ?GUN))) (lessThanOrEqualTo ?LM1 ?LM)) " +
                "(capability (KappaFn ?KILLING (and (instance ?KILLING Killing) " +
                "(patient ?KILLING ?O))) instrument ?GUN))";
        Formula f = new Formula(stmt);
        assertTrue(f.isHigherOrder(SigmaTestBase.kb));
    }

    /** ***************************************************************
     */
    @Test
    public void testIsHigherOrder2() {

        String stmt;
        stmt = "(and\n" +
                "  (instance Tunnel1 Tunnel)\n" +
                "  (equal ?P (AfternoonFn Tunnel))\n" +  // should be TransitFn but that's not in Merge.kif
                "  (holeMouth M1 Tunnel1)\n" +
                "  (holeMouth M2 Tunnel1)\n" +
                "  (not\n" +
                "    (equal M1 M2))\n" +
                "  (not\n" +
                "    (connected M1 M2))\n" +
                "  (located Jane M1)\n" +
                "  (origin ?P M1)\n" +
                "  (destination ?P M2)\n" +
                "  (agent ?P John)\n" +
                "  (length Tunnel1 L))";
        Formula f = new Formula(stmt);
        System.out.println("testIsHigherOrder2: " + f);
        System.out.println("isHigherOrder: " + f.isHigherOrder(SigmaTestBase.kb));
        assertFalse(f.isHigherOrder(SigmaTestBase.kb));
    }

    /** ***************************************************************
     */
    @Test
    public void testIsHigherOrder3() {

        String stmt = "(=>\n" +
                "  (and\n" +
                "    (instance ?F Function)\n" +
                "    (rangeSubclass ?F ?C)\n" +
                "    (instance ?I (?F ?X)))\n" +
                "  (instance ?I ?C))";
        Formula f = new Formula(stmt);
        System.out.println("testIsHigherOrder3: " + f);
        System.out.println("testIsHigherOrder3: results: " + f.isHigherOrder(SigmaTestBase.kb));
        System.out.println("testIsHigherOrder3(): expected: " + false);
        if (!f.isHigherOrder(SigmaTestBase.kb))
            System.out.println("testIsHigherOrder3(): success!");
        else
            System.out.println("testIsHigherOrder3(): failure");
        assertTrue(!f.isHigherOrder(SigmaTestBase.kb));
    }

    /****************************************************************
     */
    @Test
    public void testIsHigherOrder4() {

        String stmt = "(=> " +
                "(instance ?N SCNuclearSilo) " +
                "(hasPurpose ?N " +
                "  (exists (?M ?W) " +
                "    (and " +
                "      (instance ?M Manufacture) " +
                "      (located ?M ?N) " +
                "      (instance ?W NuclearWeapon) " +
                "      (result ?M ?W))))";
        Formula f = new Formula(stmt);
        System.out.println("testIsHigherOrder4: " + f);
        System.out.println("testIsHigherOrder4: results: " + f.isHigherOrder(SigmaTestBase.kb));
        System.out.println("testIsHigherOrder4(): expected: " + true);
        if (f.isHigherOrder(SigmaTestBase.kb))
            System.out.println("testIsHigherOrder4(): success!");
        else
            System.out.println("testIsHigherOrder4(): failure");
        assertTrue(f.isHigherOrder(SigmaTestBase.kb));
    }
}