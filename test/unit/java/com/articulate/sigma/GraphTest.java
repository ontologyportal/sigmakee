package com.articulate.sigma;

import java.io.IOException;
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

} // end class file GraphTest.java