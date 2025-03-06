package com.articulate.sigma;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Set;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;

/**
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=com.articulate.sigma.KIFTest">Terry Norbraten, NPS MOVES</a>
 */
public class KIFTest extends UnitTestBase {

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

    String stmt2 = "(WhenFn(?STH2))"; // args in parens

    KIF kif;


    Set<String> errorSet;

    @Before
    public void beforeTest() {

        kif = new KIF();
    }

    @After
    public void afterTest() {

        kif = null;

        if (errorSet != null)
            errorSet.clear();
    }

    @Test
    public void testParseStatement() throws IOException {

        System.out.println("============= KIFTest.testParseStatement ==================");
        try (Reader r = new StringReader(stmt)) {
            errorSet = kif.parse(r);
        }
        assertFalse(errorSet.isEmpty()); // ErrorList not empty (ParseException)
        for (String e : errorSet)
            assertTrue(e.contains("Illegal character"));
    }

    @Test // TODO: KIF won't complain about this syntax error
    @Ignore
    public void testParseStatement2() throws IOException {

        System.out.println("============= KIFTest.testParseStatement2 ==================");
        try (Reader r = new StringReader(stmt2)) {
            errorSet = kif.parse(r);
        }
        assertFalse(errorSet.isEmpty()); // ErrorList not empty (ParseException)
    }

} // end class file KIFTest.java