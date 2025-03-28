package com.articulate.sigma;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=com.articulate.sigma.KButilitiesTest">Terry Norbraten, NPS MOVES</a>
 */
public class KButilitiesTest extends UnitTestBase {

    String stmt = "(=>\n" +
                  "(and\n" +
                  "(instance ?STH2 Physical)\n" +
                  "(instance ?FW Following)\n" +
                  "(patient ?FW ?STH2))\n" +
                  "(exists (?STH1 ?T1 ?T2)\n" +
                  "(and\n" +
                  "(instance ?STH1 Physical)\n" +
                  "(holdsDuring ?T1\n" +
                  "WhenFn(?STH1))\n" + // arg in parens
                  "(holdsDuring ?T2\n" +
                  "WhenFn(?STH2))\n" + // arg in parens
                  "(earlier ?T1 ?T2))))";

    String stmt2 = "(WhenFn(?STH2))"; // arg in parens

    String stmt3 = "(=> " +
                   "  (instance ?AW AmphibiousWarfare) " +
                   "  (exists (?O ?D ?WA ?LA) " +
                   "    (and " +
                   "      (origin ?AW ?O) " +
                   "      (destination ?AW ?D) " +
                   "      (subclass ?WA WaterArea) " + // type error
                   "      (instance ?LA LandArea) " +
                   "      (orientation ?O ?WA On) " +
                   "      (orientation ?D ?LA On))))";

    @Test // TODO: KIF won't complain about this syntax error
    @Ignore
    public void testIsValidFormula() {

        System.out.println("============= KButilitiesTest.testIsValidFormula ==================");
        assertFalse(KButilities.isValidFormula(kb, stmt));
    }

    @Test
    public void testIsValidFormula2() {

        System.out.println("============= KButilitiesTest.testIsValidFormula2 ==================");
        assertFalse(KButilities.isValidFormula(kb, stmt2));
    }

    @Test
    public void testIsValidFormula3() {

        System.out.println("============= KButilitiesTest.testIsValidFormula3 ==================");
        assertFalse(KButilities.isValidFormula(kb, stmt3));
    }

} // end class file KIFTest.java