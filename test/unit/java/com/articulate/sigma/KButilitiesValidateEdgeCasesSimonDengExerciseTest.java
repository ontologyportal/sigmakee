package com.articulate.sigma;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Tests KButilities validation ("-v") on malformed/problematic formulas.
 * Each test is the Java equivalent of running a specific command in Terminal (MacOS).
 */
public class KButilitiesValidateEdgeCasesSimonDengExerciseTest {

    // Helper: run KButilities.main(args) and capture STDOUT as a String
    private static String runMain(String... args) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(baos, true, StandardCharsets.UTF_8));
            KButilities.main(args);
        } finally {
            System.setOut(originalOut);
        }
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    // Helper: accept common failure signals without depending on exact wording
    private static void assertIndicatesFailure(String out) {
        String s = out.toLowerCase();

        boolean mentionsProblem =
            s.contains("bad quantification") ||
            s.contains("ill-formed") ||
            s.contains("free var") ||
            s.contains("unbound var") ||
            s.contains("variable not quantified") ||
            s.contains("arity") ||
            s.contains("wrong number of arguments") ||
            s.contains("parse error");

        boolean printedFalse = s.contains("false") && !s.contains("true");

        assertTrue("Validator output didn't indicate a failure. Output was:\n" + out,
                   mentionsProblem || printedFalse);
    }
    
    /** ***************************************************************
     * Reasons for failure: arity problems: (instance ...), parse error
     */
    @Test
    public void validatesMalformedExistsListAndInstance() {
        // Keep the malformed structure intentional
        String formula = "(exists (?) (instance ? X))";
        String out = runMain("-v", formula);
        assertIndicatesFailure(out);
    }

    /** ***************************************************************
     * Reasons for failure: arity error: (instance ?X <Class>) requires two arguments.
     */
    @Test
    public void validatesInstanceMissingClassArg() {
        String formula = "(instance ?X)";
        String out = runMain("-v", formula);
        assertIndicatesFailure(out);
    }

    /** ***************************************************************
     * Reasons for failure: Free/unbound variables ?Z and ?Q inside (MereologicalSumFn ?Z ?Q).
     */
    @Test
    public void validatesExistsWithPartAndFreeVars() {
        String formula = "(exists (?X) (part ?X (MereologicalSumFn ?Z ?Q)))";
        String out = runMain("-v", formula);
        assertIndicatesFailure(out);
    }
    
} // end class file KButilitiesValidateEdgeCasesSimonDengExerciseTest.java
