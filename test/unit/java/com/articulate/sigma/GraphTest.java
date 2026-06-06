package com.articulate.sigma;

import com.articulate.sigma.parsing.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=com.articulate.sigma.GraphTest">Terry Norbraten, NPS MOVES</a>
 */
public class GraphTest extends UnitTestBase {

    @Test
    public void testCreateDotGraph() {
        System.out.println("========================");
        String label = "testCreateDotGraph";
        System.out.println("TPTP3ProofProcTest: " + label);
        Graph g = new Graph();
        String term = "Transitway";
        String relation = "subclass";
        String fileRestrict = "";
        boolean fileExists = false;
        try {
            fileExists = g.createDotGraph(kb, term, relation, 1, 1, 100, "proof", fileRestrict);
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
        assertTrue("Image file does not exist", fileExists);
    }

    // -------------------------------------------------------------------------
    // CLI parsing tests — verify Graph.main()'s expected arg interpretation
    // via CLIMapParser directly (main() itself requires KB initialisation).
    // -------------------------------------------------------------------------

    @Test
    public void testHelpFlagParsed() {
        Map<String, List<String>> parsed = CLIMapParser.parse(new String[]{"-h"});
        assertTrue("'-h' should be recognised as the help flag", parsed.containsKey("h"));
    }

    @Test
    public void testGraphFlagExtractsTermAndRelation() {
        String[] args = {"-g", "Animal", "subclass"};
        Map<String, List<String>> parsed = CLIMapParser.parse(args);
        List<String> gArgs = parsed.get("g");
        assertNotNull("'-g' flag should be present", gArgs);
        assertEquals("First value should be the term", "Animal", gArgs.get(0));
        assertEquals("Second value should be the relation", "subclass", gArgs.get(1));
    }

    @Test
    public void testGraphFlagWithInsufficientArgsTreatedAsInvalid() {
        // Providing -g with only one value should not satisfy size >= 2
        Map<String, List<String>> parsed = CLIMapParser.parse(new String[]{"-g", "Animal"});
        List<String> gArgs = parsed.get("g");
        assertNotNull(gArgs);
        assertTrue("Only one argument to -g should fail the size >= 2 guard", gArgs.size() < 2);
    }

    @Test
    public void testNoArgsTreatedAsInvalid() {
        Map<String, List<String>> parsed = CLIMapParser.parse(new String[]{});
        assertFalse("Empty args should not contain '-g'", parsed.containsKey("g"));
        assertFalse("Empty args should not contain '-h'", parsed.containsKey("h"));
    }

} // end class file GraphTest.java