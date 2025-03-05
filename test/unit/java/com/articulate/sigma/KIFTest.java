package com.articulate.sigma;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

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
                  "WhenFn(?STH!))\n" +
                  "(holdsDuring ?T2\n" +
                  "WhenFn(?STH2))\n" +
                  "(earlier ?T1 ?T2))))";
    KIF kif;

    @Before
    public void beforeTest() {

        kif = new KIF();
    }

    @After
    public void afterTest() {

        kif = null;
    }

    @Test
    public void testParseStatement() throws IOException {

        Set<String> set;
        try (Reader r = new StringReader(stmt)) {
            set = kif.parse(r);
        }
        assertFalse(set.isEmpty()); // ErrorList not empty (ParseException)
        for (String s : set)
            assertTrue(s.contains("Illegal character"));
    }

} // end class file KIFTest.java