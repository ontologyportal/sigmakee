package com.articulate.sigma;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=com.articulate.sigma.KIFTest">Terry Norbraten, NPS MOVES</a>
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
                  "WhenFn(?STH!))\n" + // Illegal character
                  "(holdsDuring ?T2\n" +
                  "WhenFn(?STH2))\n" +
                  "(earlier ?T1 ?T2))))";

    String stmt2 = "(WhenFn(?STH2))"; // arg in parens

    @Test
    public void testIsValidFormula() {

        System.out.println("============= KButilitiesTest.testIsValidFormula ==================");
        assertFalse(KButilities.isValidFormula(kb, stmt));
    }

    @Test
    public void testIsValidFormula2() {

        System.out.println("============= KButilitiesTest.testIsValidFormula2 ==================");
        assertFalse(KButilities.isValidFormula(kb, stmt2));
    }

} // end class file KIFTest.java