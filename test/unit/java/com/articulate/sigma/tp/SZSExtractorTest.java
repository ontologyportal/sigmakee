/** This code is copyright Articulate Software (c) 2024.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
*/

package com.articulate.sigma.tp;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for SZSExtractor utility class.
 */
public class SZSExtractorTest {

    // === extractStatus tests ===

    @Test
    public void testExtractStatus_vampireTheorem() {
        List<String> output = Arrays.asList(
                "% Running Vampire",
                "% SZS status Theorem for SUMO",
                "% Proof found"
        );

        SZSStatus status = SZSExtractor.extractStatus(output);
        assertEquals(SZSStatus.THEOREM, status);
    }

    @Test
    public void testExtractStatus_vampireTimeout() {
        List<String> output = Arrays.asList(
                "% Running Vampire",
                "% SZS status Timeout for SUMO"
        );

        SZSStatus status = SZSExtractor.extractStatus(output);
        assertEquals(SZSStatus.TIMEOUT, status);
    }

    @Test
    public void testExtractStatus_vampireUnsatisfiable() {
        List<String> output = Arrays.asList(
                "% SZS status Unsatisfiable for problem"
        );

        SZSStatus status = SZSExtractor.extractStatus(output);
        assertEquals(SZSStatus.UNSATISFIABLE, status);
    }

    @Test
    public void testExtractStatus_vampireSatisfiable() {
        List<String> output = Arrays.asList(
                "% SZS status Satisfiable for problem"
        );

        SZSStatus status = SZSExtractor.extractStatus(output);
        assertEquals(SZSStatus.SATISFIABLE, status);
    }

    @Test
    public void testExtractStatus_vampireCounterSatisfiable() {
        List<String> output = Arrays.asList(
                "% SZS status CounterSatisfiable for problem"
        );

        SZSStatus status = SZSExtractor.extractStatus(output);
        assertEquals(SZSStatus.COUNTER_SATISFIABLE, status);
    }

    @Test
    public void testExtractStatus_eproverFormat() {
        List<String> output = Arrays.asList(
                "# Parsing problem",
                "# SZS status Theorem",
                "# Proof found"
        );

        SZSStatus status = SZSExtractor.extractStatus(output);
        assertEquals(SZSStatus.THEOREM, status);
    }

    @Test
    public void testExtractStatus_noPrefix() {
        List<String> output = Arrays.asList(
                "SZS status Theorem for problem"
        );

        SZSStatus status = SZSExtractor.extractStatus(output);
        assertEquals(SZSStatus.THEOREM, status);
    }

    @Test
    public void testExtractStatus_caseInsensitive() {
        List<String> output = Arrays.asList(
                "% szs STATUS theorem FOR problem"
        );

        SZSStatus status = SZSExtractor.extractStatus(output);
        assertEquals(SZSStatus.THEOREM, status);
    }

    @Test
    public void testExtractStatus_gaveUp() {
        List<String> output = Arrays.asList(
                "% SZS status GaveUp for problem"
        );

        SZSStatus status = SZSExtractor.extractStatus(output);
        assertEquals(SZSStatus.GAVE_UP, status);
    }

    @Test
    public void testExtractStatus_resourceOut() {
        List<String> output = Arrays.asList(
                "% SZS status ResourceOut for problem"
        );

        SZSStatus status = SZSExtractor.extractStatus(output);
        assertEquals(SZSStatus.RESOURCE_OUT, status);
    }

    @Test
    public void testExtractStatus_inputError() {
        List<String> output = Arrays.asList(
                "% SZS status InputError for problem"
        );

        SZSStatus status = SZSExtractor.extractStatus(output);
        assertEquals(SZSStatus.INPUT_ERROR, status);
    }

    @Test
    public void testExtractStatus_syntaxError() {
        List<String> output = Arrays.asList(
                "% SZS status SyntaxError for problem"
        );

        SZSStatus status = SZSExtractor.extractStatus(output);
        assertEquals(SZSStatus.SYNTAX_ERROR, status);
    }

    @Test
    public void testExtractStatus_typeError() {
        List<String> output = Arrays.asList(
                "% SZS status TypeError for problem"
        );

        SZSStatus status = SZSExtractor.extractStatus(output);
        assertEquals(SZSStatus.TYPE_ERROR, status);
    }

    @Test
    public void testExtractStatus_nullOutput() {
        SZSStatus status = SZSExtractor.extractStatus(null);
        assertNull(status);
    }

    @Test
    public void testExtractStatus_emptyOutput() {
        SZSStatus status = SZSExtractor.extractStatus(Collections.emptyList());
        assertNull(status);
    }

    @Test
    public void testExtractStatus_noStatusLine() {
        List<String> output = Arrays.asList(
                "% Running Vampire",
                "% Processing...",
                "% Done"
        );

        SZSStatus status = SZSExtractor.extractStatus(output);
        assertNull(status);
    }

    // === extractStatusLine tests ===

    @Test
    public void testExtractStatusLine_vampire() {
        List<String> output = Arrays.asList(
                "% Running",
                "% SZS status Theorem for SUMO",
                "% Proof"
        );

        String line = SZSExtractor.extractStatusLine(output);
        assertEquals("% SZS status Theorem for SUMO", line);
    }

    @Test
    public void testExtractStatusLine_null() {
        String line = SZSExtractor.extractStatusLine(null);
        assertNull(line);
    }

    @Test
    public void testExtractStatusLine_notFound() {
        List<String> output = Arrays.asList("No status here");
        String line = SZSExtractor.extractStatusLine(output);
        assertNull(line);
    }

    // === extractStatusRaw tests ===

    @Test
    public void testExtractStatusRaw() {
        List<String> output = Arrays.asList(
                "% SZS status Theorem for SUMO"
        );

        String raw = SZSExtractor.extractStatusRaw(output);
        assertEquals("Theorem", raw);
    }

    @Test
    public void testExtractStatusRaw_null() {
        String raw = SZSExtractor.extractStatusRaw(null);
        assertNull(raw);
    }

    // === extractDiagnostics tests ===

    @Test
    public void testExtractDiagnostics_withDiagnostics() {
        List<String> output = Arrays.asList(
                "% SZS status TypeError : line 42, bad type"
        );

        String diag = SZSExtractor.extractDiagnostics(output);
        assertEquals("line 42, bad type", diag);
    }

    @Test
    public void testExtractDiagnostics_noDiagnostics() {
        List<String> output = Arrays.asList(
                "% SZS status Theorem for SUMO"
        );

        String diag = SZSExtractor.extractDiagnostics(output);
        assertNull(diag);
    }

    @Test
    public void testExtractDiagnostics_null() {
        String diag = SZSExtractor.extractDiagnostics(null);
        assertNull(diag);
    }

    // === extractOutputType tests ===

    @Test
    public void testExtractOutputType_proof() {
        List<String> output = Arrays.asList(
                "% SZS status Theorem",
                "% SZS output start Proof",
                "fof(step1, ...)."
        );

        String type = SZSExtractor.extractOutputType(output);
        assertEquals("Proof", type);
    }

    @Test
    public void testExtractOutputType_refutation() {
        List<String> output = Arrays.asList(
                "% SZS output start CNFRefutation"
        );

        String type = SZSExtractor.extractOutputType(output);
        assertEquals("CNFRefutation", type);
    }

    @Test
    public void testExtractOutputType_model() {
        List<String> output = Arrays.asList(
                "% SZS output start Model"
        );

        String type = SZSExtractor.extractOutputType(output);
        assertEquals("Model", type);
    }

    @Test
    public void testExtractOutputType_eproverFormat() {
        List<String> output = Arrays.asList(
                "# SZS output start CNFRefutation"
        );

        String type = SZSExtractor.extractOutputType(output);
        assertEquals("CNFRefutation", type);
    }

    @Test
    public void testExtractOutputType_null() {
        String type = SZSExtractor.extractOutputType(null);
        assertNull(type);
    }

    @Test
    public void testExtractOutputType_notFound() {
        List<String> output = Arrays.asList("No output type");
        String type = SZSExtractor.extractOutputType(output);
        assertNull(type);
    }

    // === extractErrorLines tests ===

    @Test
    public void testExtractErrorLines_syntaxError() {
        List<String> output = Arrays.asList(
                "Processing formula...",
                "Syntax error at line 5",
                "Error: Missing parenthesis",
                "Done"
        );

        List<String> errors = SZSExtractor.extractErrorLines(output);
        assertEquals(2, errors.size());
        assertTrue(errors.get(0).contains("Syntax error"));
        assertTrue(errors.get(1).contains("Error"));
    }

    @Test
    public void testExtractErrorLines_noErrors() {
        List<String> output = Arrays.asList(
                "Processing...",
                "Success",
                "Done"
        );

        List<String> errors = SZSExtractor.extractErrorLines(output);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testExtractErrorLines_variousIndicators() {
        List<String> output = Arrays.asList(
                "ERROR: something wrong",
                "Type error found",
                "Parse error at line 10",
                "Exception occurred",
                "FATAL: crash"
        );

        List<String> errors = SZSExtractor.extractErrorLines(output);
        assertEquals(5, errors.size());
    }

    @Test
    public void testExtractErrorLines_null() {
        List<String> errors = SZSExtractor.extractErrorLines((List<String>) null);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testExtractErrorLines_empty() {
        List<String> errors = SZSExtractor.extractErrorLines(Collections.emptyList());
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testExtractErrorLines_stdoutAndStderr() {
        List<String> stdout = Arrays.asList("Error in stdout");
        List<String> stderr = Arrays.asList("Error from stderr");

        List<String> errors = SZSExtractor.extractErrorLines(stdout, stderr);
        assertTrue(errors.size() >= 1);
        // Stderr takes precedence
        assertTrue(errors.get(0).contains("stderr"));
    }

    @Test
    public void testExtractErrorLines_stderrOnly() {
        List<String> stderr = Arrays.asList("stderr line 1", "stderr line 2");

        List<String> errors = SZSExtractor.extractErrorLines(null, stderr);
        assertEquals(2, errors.size());
    }

    @Test
    public void testExtractErrorLines_stdoutOnlyWhenNoStderr() {
        List<String> stdout = Arrays.asList("Error: something");

        List<String> errors = SZSExtractor.extractErrorLines(stdout, null);
        assertEquals(1, errors.size());
    }

    // === extractWarnings tests ===

    @Test
    public void testExtractWarnings() {
        List<String> output = Arrays.asList(
                "Processing...",
                "Warning: deprecated feature",
                "WARNING: old syntax",
                "Done"
        );

        List<String> warnings = SZSExtractor.extractWarnings(output);
        assertEquals(2, warnings.size());
    }

    @Test
    public void testExtractWarnings_noWarnings() {
        List<String> output = Arrays.asList("Processing complete");

        List<String> warnings = SZSExtractor.extractWarnings(output);
        assertTrue(warnings.isEmpty());
    }

    @Test
    public void testExtractWarnings_null() {
        List<String> warnings = SZSExtractor.extractWarnings(null);
        assertTrue(warnings.isEmpty());
    }

    // === indicatesTimeout tests ===

    @Test
    public void testIndicatesTimeout_szsStatus() {
        List<String> output = Arrays.asList(
                "% SZS status Timeout for problem"
        );

        assertTrue(SZSExtractor.indicatesTimeout(output));
    }

    @Test
    public void testIndicatesTimeout_timeLimitPattern() {
        List<String> output = Arrays.asList(
                "% Time limit reached"
        );

        assertTrue(SZSExtractor.indicatesTimeout(output));
    }

    @Test
    public void testIndicatesTimeout_timeoutPattern() {
        List<String> output = Arrays.asList(
                "Process timed out"
        );

        assertTrue(SZSExtractor.indicatesTimeout(output));
    }

    @Test
    public void testIndicatesTimeout_cpuTimeLimit() {
        List<String> output = Arrays.asList(
                "CPU time limit exceeded"
        );

        assertTrue(SZSExtractor.indicatesTimeout(output));
    }

    @Test
    public void testIndicatesTimeout_noTimeout() {
        List<String> output = Arrays.asList(
                "% SZS status Theorem"
        );

        assertFalse(SZSExtractor.indicatesTimeout(output));
    }

    @Test
    public void testIndicatesTimeout_null() {
        assertFalse(SZSExtractor.indicatesTimeout(null));
    }

    @Test
    public void testIndicatesTimeout_empty() {
        assertFalse(SZSExtractor.indicatesTimeout(Collections.emptyList()));
    }

    // === indicatesResourceOut tests ===

    @Test
    public void testIndicatesResourceOut_szsStatus() {
        List<String> output = Arrays.asList(
                "% SZS status ResourceOut"
        );

        assertTrue(SZSExtractor.indicatesResourceOut(output));
    }

    @Test
    public void testIndicatesResourceOut_memoryLimit() {
        List<String> output = Arrays.asList(
                "Memory limit exceeded"
        );

        assertTrue(SZSExtractor.indicatesResourceOut(output));
    }

    @Test
    public void testIndicatesResourceOut_outOfMemory() {
        List<String> output = Arrays.asList(
                "Out of memory error"
        );

        assertTrue(SZSExtractor.indicatesResourceOut(output));
    }

    @Test
    public void testIndicatesResourceOut_oom() {
        List<String> output = Arrays.asList(
                "Killed by OOM"
        );

        assertTrue(SZSExtractor.indicatesResourceOut(output));
    }

    @Test
    public void testIndicatesResourceOut_noResourceOut() {
        List<String> output = Arrays.asList(
                "% SZS status Theorem"
        );

        assertFalse(SZSExtractor.indicatesResourceOut(output));
    }

    @Test
    public void testIndicatesResourceOut_null() {
        assertFalse(SZSExtractor.indicatesResourceOut(null));
    }

    // === getPrimaryError tests ===

    @Test
    public void testGetPrimaryError_fromStderr() {
        List<String> stdout = Arrays.asList("Normal output");
        List<String> stderr = Arrays.asList("Primary error message");

        String error = SZSExtractor.getPrimaryError(stdout, stderr);
        assertEquals("Primary error message", error);
    }

    @Test
    public void testGetPrimaryError_skipsEmptyLines() {
        List<String> stderr = Arrays.asList("", "  ", "Actual error");

        String error = SZSExtractor.getPrimaryError(null, stderr);
        assertEquals("Actual error", error);
    }

    @Test
    public void testGetPrimaryError_skipsComments() {
        List<String> stderr = Arrays.asList("# comment", "% another comment", "Real error");

        String error = SZSExtractor.getPrimaryError(null, stderr);
        assertEquals("Real error", error);
    }

    @Test
    public void testGetPrimaryError_fallsBackToStdout() {
        List<String> stdout = Arrays.asList("Error: something went wrong");

        String error = SZSExtractor.getPrimaryError(stdout, null);
        assertEquals("Error: something went wrong", error);
    }

    @Test
    public void testGetPrimaryError_noErrors() {
        List<String> stdout = Arrays.asList("Normal output");

        String error = SZSExtractor.getPrimaryError(stdout, null);
        assertNull(error);
    }

    @Test
    public void testGetPrimaryError_bothNull() {
        String error = SZSExtractor.getPrimaryError(null, null);
        assertNull(error);
    }

    // === indicatesSuccess tests ===

    @Test
    public void testIndicatesSuccess_theorem() {
        List<String> output = Arrays.asList("% SZS status Theorem");
        assertTrue(SZSExtractor.indicatesSuccess(output));
    }

    @Test
    public void testIndicatesSuccess_unsatisfiable() {
        List<String> output = Arrays.asList("% SZS status Unsatisfiable");
        assertTrue(SZSExtractor.indicatesSuccess(output));
    }

    @Test
    public void testIndicatesSuccess_timeout() {
        List<String> output = Arrays.asList("% SZS status Timeout");
        assertFalse(SZSExtractor.indicatesSuccess(output));
    }

    @Test
    public void testIndicatesSuccess_null() {
        assertFalse(SZSExtractor.indicatesSuccess(null));
    }

    // === containsProof tests ===

    @Test
    public void testContainsProof_proofOutputType() {
        List<String> output = Arrays.asList(
                "% SZS status Theorem",
                "% SZS output start Proof",
                "fof(step1, plain, ...)."
        );

        assertTrue(SZSExtractor.containsProof(output));
    }

    @Test
    public void testContainsProof_refutationOutputType() {
        List<String> output = Arrays.asList(
                "% SZS output start CNFRefutation"
        );

        assertTrue(SZSExtractor.containsProof(output));
    }

    @Test
    public void testContainsProof_proofFoundMarker() {
        List<String> output = Arrays.asList(
                "Proof found!"
        );

        assertTrue(SZSExtractor.containsProof(output));
    }

    @Test
    public void testContainsProof_noProof() {
        List<String> output = Arrays.asList(
                "% SZS status Timeout"
        );

        assertFalse(SZSExtractor.containsProof(output));
    }

    @Test
    public void testContainsProof_null() {
        assertFalse(SZSExtractor.containsProof(null));
    }

    @Test
    public void testContainsProof_empty() {
        assertFalse(SZSExtractor.containsProof(Collections.emptyList()));
    }
}
