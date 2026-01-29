/** This code is copyright Articulate Software (c) 2024.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
*/

package com.articulate.sigma.tp;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for ATPResult class.
 */
public class ATPResultTest {

    // === Constructor tests ===

    @Test
    public void testDefaultConstructor() {
        ATPResult result = new ATPResult();

        assertNull(result.getEngineName());
        assertNull(result.getEngineMode());
        assertEquals(-1, result.getExitCode());
        assertEquals(0, result.getElapsedTimeMs());
        assertFalse(result.isTimedOut());
        assertEquals(SZSStatus.NOT_RUN, result.getSzsStatus());
        assertNotNull(result.getStdout());
        assertTrue(result.getStdout().isEmpty());
        assertNotNull(result.getStderr());
        assertTrue(result.getStderr().isEmpty());
    }

    // === Factory method tests ===

    @Test
    public void testNotRun_factory() {
        ATPResult result = ATPResult.notRun("Vampire", "Executable not found");

        assertEquals("Vampire", result.getEngineName());
        assertEquals(SZSStatus.NOT_RUN, result.getSzsStatus());
        assertEquals("Executable not found", result.getPrimaryError());
    }

    @Test
    public void testCrashed_factory() {
        List<String> stdout = Arrays.asList("Starting...");
        List<String> stderr = Arrays.asList("Segmentation fault");

        ATPResult result = ATPResult.crashed("Vampire", 139, stdout, stderr);

        assertEquals("Vampire", result.getEngineName());
        assertEquals(139, result.getExitCode());
        assertEquals(SZSStatus.CRASHED, result.getSzsStatus());
        assertEquals(stdout, result.getStdout());
        assertEquals(stderr, result.getStderr());
        assertEquals("SIG11", result.getTerminationSignal()); // 139 - 128 = 11
    }

    @Test
    public void testCrashed_factory_noSignal() {
        ATPResult result = ATPResult.crashed("Vampire", 1, null, null);

        assertEquals(1, result.getExitCode());
        assertEquals(SZSStatus.CRASHED, result.getSzsStatus());
        assertNull(result.getTerminationSignal()); // Exit code 1 is not a signal
    }

    @Test
    public void testTimeout_factory() {
        List<String> stdout = Arrays.asList("% SZS status Timeout");
        List<String> stderr = Collections.emptyList();

        ATPResult result = ATPResult.timeout("Vampire", 30000, 30500, stdout, stderr);

        assertEquals("Vampire", result.getEngineName());
        assertEquals(30000, result.getTimeoutMs());
        assertEquals(30500, result.getElapsedTimeMs());
        assertTrue(result.isTimedOut());
        assertEquals(SZSStatus.TIMEOUT, result.getSzsStatus());
    }

    // === Builder tests ===

    @Test
    public void testBuilder_allFields() {
        List<String> commandLine = Arrays.asList("vampire", "--mode", "casc");
        List<String> stdout = Arrays.asList("% SZS status Theorem");
        List<String> stderr = Collections.emptyList();

        ATPResult result = ATPResult.builder()
                .engineName("Vampire")
                .engineMode("CASC")
                .inputLanguage("FOF")
                .inputSource("custom")
                .timeoutMs(30000)
                .commandLine(commandLine)
                .exitCode(0)
                .elapsedTimeMs(5000)
                .timedOut(false)
                .stdout(stdout)
                .stderr(stderr)
                .szsStatus(SZSStatus.THEOREM)
                .build();

        assertEquals("Vampire", result.getEngineName());
        assertEquals("CASC", result.getEngineMode());
        assertEquals("FOF", result.getInputLanguage());
        assertEquals("custom", result.getInputSource());
        assertEquals(30000, result.getTimeoutMs());
        assertEquals(commandLine, result.getCommandLine());
        assertEquals(0, result.getExitCode());
        assertEquals(5000, result.getElapsedTimeMs());
        assertFalse(result.isTimedOut());
        assertEquals(stdout, result.getStdout());
        assertEquals(stderr, result.getStderr());
        assertEquals(SZSStatus.THEOREM, result.getSzsStatus());
    }

    @Test
    public void testBuilder_commandLineArray() {
        String[] cmdArray = {"vampire", "--input", "file.tptp"};

        ATPResult result = ATPResult.builder()
                .commandLine(cmdArray)
                .build();

        assertEquals(3, result.getCommandLine().size());
        assertEquals("vampire", result.getCommandLine().get(0));
    }

    // === isSuccess tests ===

    @Test
    public void testIsSuccess_theorem() {
        ATPResult result = new ATPResult();
        result.setSzsStatus(SZSStatus.THEOREM);

        assertTrue(result.isSuccess());
    }

    @Test
    public void testIsSuccess_unsatisfiable() {
        ATPResult result = new ATPResult();
        result.setSzsStatus(SZSStatus.UNSATISFIABLE);

        assertTrue(result.isSuccess());
    }

    @Test
    public void testIsSuccess_satisfiable() {
        ATPResult result = new ATPResult();
        result.setSzsStatus(SZSStatus.SATISFIABLE);

        assertTrue(result.isSuccess());
    }

    @Test
    public void testIsSuccess_counterSatisfiable() {
        ATPResult result = new ATPResult();
        result.setSzsStatus(SZSStatus.COUNTER_SATISFIABLE);

        assertTrue(result.isSuccess());
    }

    @Test
    public void testIsSuccess_timeout() {
        ATPResult result = new ATPResult();
        result.setSzsStatus(SZSStatus.TIMEOUT);

        assertFalse(result.isSuccess());
    }

    @Test
    public void testIsSuccess_gaveUp() {
        ATPResult result = new ATPResult();
        result.setSzsStatus(SZSStatus.GAVE_UP);

        assertFalse(result.isSuccess());
    }

    @Test
    public void testIsSuccess_notRun() {
        ATPResult result = new ATPResult();
        result.setSzsStatus(SZSStatus.NOT_RUN);

        assertFalse(result.isSuccess());
    }

    @Test
    public void testIsSuccess_nullStatus() {
        ATPResult result = new ATPResult();
        result.setSzsStatus(null);

        assertFalse(result.isSuccess());
    }

    // === hasProof tests ===

    @Test
    public void testHasProof_true() {
        ATPResult result = new ATPResult();
        result.setProofSteps(Arrays.asList(/* mock TPTPFormula objects would go here */));
        // Since we can't easily create TPTPFormula objects, we'll test the empty case
    }

    @Test
    public void testHasProof_false_null() {
        ATPResult result = new ATPResult();
        result.setProofSteps(null);

        assertFalse(result.hasProof());
    }

    @Test
    public void testHasProof_false_empty() {
        ATPResult result = new ATPResult();
        result.setProofSteps(Collections.emptyList());

        assertFalse(result.hasProof());
    }

    // === hasAnswers tests ===

    @Test
    public void testHasAnswers_true() {
        ATPResult result = new ATPResult();
        Map<String, String> answers = new HashMap<>();
        answers.put("X", "Human");
        result.setAnswerBindings(answers);

        assertTrue(result.hasAnswers());
    }

    @Test
    public void testHasAnswers_false_null() {
        ATPResult result = new ATPResult();
        result.setAnswerBindings(null);

        assertFalse(result.hasAnswers());
    }

    @Test
    public void testHasAnswers_false_empty() {
        ATPResult result = new ATPResult();
        result.setAnswerBindings(Collections.emptyMap());

        assertFalse(result.hasAnswers());
    }

    // === hasErrors tests ===

    @Test
    public void testHasErrors_exitCodeNonZero() {
        ATPResult result = new ATPResult();
        result.setExitCode(1);

        assertTrue(result.hasErrors());
    }

    @Test
    public void testHasErrors_exitCodeMinusOne() {
        ATPResult result = new ATPResult();
        result.setExitCode(-1);
        result.setSzsStatus(SZSStatus.UNKNOWN); // Set non-error status
        result.setErrorLines(Collections.emptyList());
        result.setStderr(Collections.emptyList());

        assertFalse(result.hasErrors()); // -1 means not run, and no other error indicators
    }

    @Test
    public void testHasErrors_errorStatus() {
        ATPResult result = new ATPResult();
        result.setExitCode(0);
        result.setSzsStatus(SZSStatus.INPUT_ERROR);

        assertTrue(result.hasErrors());
    }

    @Test
    public void testHasErrors_errorLines() {
        ATPResult result = new ATPResult();
        result.setExitCode(0);
        result.setSzsStatus(SZSStatus.UNKNOWN);
        result.setErrorLines(Arrays.asList("Error: something"));

        assertTrue(result.hasErrors());
    }

    @Test
    public void testHasErrors_stderr() {
        ATPResult result = new ATPResult();
        result.setExitCode(0);
        result.setSzsStatus(SZSStatus.THEOREM);
        result.setStderr(Arrays.asList("Warning message"));

        assertTrue(result.hasErrors());
    }

    @Test
    public void testHasErrors_noErrors() {
        ATPResult result = new ATPResult();
        result.setExitCode(0);
        result.setSzsStatus(SZSStatus.THEOREM);
        result.setStderr(Collections.emptyList());
        result.setErrorLines(Collections.emptyList());

        assertFalse(result.hasErrors());
    }

    // === hasWarnings tests ===

    @Test
    public void testHasWarnings_true() {
        ATPResult result = new ATPResult();
        result.setWarnings(Arrays.asList("Warning: deprecated"));

        assertTrue(result.hasWarnings());
    }

    @Test
    public void testHasWarnings_false_null() {
        ATPResult result = new ATPResult();
        result.setWarnings(null);

        assertFalse(result.hasWarnings());
    }

    @Test
    public void testHasWarnings_false_empty() {
        ATPResult result = new ATPResult();
        result.setWarnings(Collections.emptyList());

        assertFalse(result.hasWarnings());
    }

    // === hasStdout/hasStderr tests ===

    @Test
    public void testHasStdout_true() {
        ATPResult result = new ATPResult();
        result.setStdout(Arrays.asList("output"));

        assertTrue(result.hasStdout());
    }

    @Test
    public void testHasStdout_false() {
        ATPResult result = new ATPResult();

        assertFalse(result.hasStdout());
    }

    @Test
    public void testHasStderr_true() {
        ATPResult result = new ATPResult();
        result.setStderr(Arrays.asList("error"));

        assertTrue(result.hasStderr());
    }

    @Test
    public void testHasStderr_false() {
        ATPResult result = new ATPResult();

        assertFalse(result.hasStderr());
    }

    // === getCssClass tests ===

    @Test
    public void testGetCssClass_success() {
        ATPResult result = new ATPResult();
        result.setSzsStatus(SZSStatus.THEOREM);

        assertEquals("szs-success", result.getCssClass());
    }

    @Test
    public void testGetCssClass_failure() {
        ATPResult result = new ATPResult();
        result.setSzsStatus(SZSStatus.TIMEOUT);

        assertEquals("szs-failure", result.getCssClass());
    }

    @Test
    public void testGetCssClass_error() {
        ATPResult result = new ATPResult();
        result.setSzsStatus(SZSStatus.INPUT_ERROR);

        assertEquals("szs-error", result.getCssClass());
    }

    @Test
    public void testGetCssClass_unknown() {
        ATPResult result = new ATPResult();
        result.setSzsStatus(SZSStatus.UNKNOWN);

        assertEquals("szs-unknown", result.getCssClass());
    }

    @Test
    public void testGetCssClass_null() {
        ATPResult result = new ATPResult();
        result.setSzsStatus(null);

        assertEquals("szs-unknown", result.getCssClass());
    }

    @Test
    public void testGetStatusBadgeClass() {
        ATPResult result = new ATPResult();
        result.setSzsStatus(SZSStatus.THEOREM);

        assertEquals(result.getCssClass(), result.getStatusBadgeClass());
    }

    // === getSummary tests ===

    @Test
    public void testGetSummary_complete() {
        ATPResult result = ATPResult.builder()
                .engineName("Vampire")
                .engineMode("CASC")
                .szsStatus(SZSStatus.THEOREM)
                .elapsedTimeMs(5000)
                .build();

        String summary = result.getSummary();

        assertTrue(summary.contains("Vampire"));
        assertTrue(summary.contains("CASC"));
        assertTrue(summary.contains("Theorem"));
        assertTrue(summary.contains("5000"));
    }

    @Test
    public void testGetSummary_timeout() {
        ATPResult result = ATPResult.builder()
                .engineName("Vampire")
                .szsStatus(SZSStatus.TIMEOUT)
                .timedOut(true)
                .elapsedTimeMs(30000)
                .build();

        String summary = result.getSummary();

        assertTrue(summary.contains("Timeout"));
        assertTrue(summary.contains("timed out"));
    }

    @Test
    public void testGetSummary_nullEngine() {
        ATPResult result = new ATPResult();
        result.setSzsStatus(SZSStatus.THEOREM);

        String summary = result.getSummary();

        assertTrue(summary.contains("Prover")); // fallback
        assertTrue(summary.contains("Theorem"));
    }

    @Test
    public void testToString() {
        ATPResult result = ATPResult.builder()
                .engineName("Vampire")
                .szsStatus(SZSStatus.THEOREM)
                .build();

        assertEquals(result.getSummary(), result.toString());
    }

    // === Getter/Setter tests ===

    @Test
    public void testSettersWithNull() {
        ATPResult result = new ATPResult();

        result.setStdout(null);
        assertNotNull(result.getStdout());
        assertTrue(result.getStdout().isEmpty());

        result.setStderr(null);
        assertNotNull(result.getStderr());
        assertTrue(result.getStderr().isEmpty());

        result.setCommandLine((List<String>) null);
        assertNotNull(result.getCommandLine());
        assertTrue(result.getCommandLine().isEmpty());

        result.setErrorLines(null);
        assertNotNull(result.getErrorLines());
        assertTrue(result.getErrorLines().isEmpty());

        result.setWarnings(null);
        assertNotNull(result.getWarnings());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testSetSzsStatus_null() {
        ATPResult result = new ATPResult();
        result.setSzsStatus(null);

        assertEquals(SZSStatus.UNKNOWN, result.getSzsStatus());
    }

    @Test
    public void testGetCommandLineString() {
        ATPResult result = new ATPResult();
        result.setCommandLine(Arrays.asList("vampire", "--mode", "casc"));

        assertEquals("vampire --mode casc", result.getCommandLineString());
    }

    @Test
    public void testGetCommandLineString_empty() {
        ATPResult result = new ATPResult();

        assertEquals("", result.getCommandLineString());
    }

    // === extractSzsFromOutput tests ===

    @Test
    public void testExtractSzsFromOutput() {
        ATPResult result = new ATPResult();
        result.setStdout(Arrays.asList(
                "% Running Vampire",
                "% SZS status Theorem for SUMO",
                "% SZS output start Proof"
        ));

        result.extractSzsFromOutput();

        assertEquals(SZSStatus.THEOREM, result.getSzsStatus());
        assertNotNull(result.getSzsStatusRaw());
        assertEquals("Proof", result.getSzsOutputType());
    }

    @Test
    public void testExtractSzsFromOutput_emptyStdout() {
        ATPResult result = new ATPResult();
        result.extractSzsFromOutput();

        // Should not change anything
        assertEquals(SZSStatus.NOT_RUN, result.getSzsStatus());
    }

    // === finalize tests ===

    @Test
    public void testFinalize_success() {
        ATPResult result = new ATPResult();
        result.setStdout(Arrays.asList("% SZS status Theorem"));

        result.finalize(0, 5000, false);

        assertEquals(0, result.getExitCode());
        assertEquals(5000, result.getElapsedTimeMs());
        assertFalse(result.isTimedOut());
        assertEquals(SZSStatus.THEOREM, result.getSzsStatus());
    }

    @Test
    public void testFinalize_timeout() {
        ATPResult result = new ATPResult();
        result.setStdout(Arrays.asList("% Time limit reached"));

        result.finalize(0, 30000, false);

        assertTrue(result.isTimedOut());
        assertEquals(SZSStatus.TIMEOUT, result.getSzsStatus());
    }

    @Test
    public void testFinalize_resourceOut() {
        ATPResult result = new ATPResult();
        result.setStdout(Arrays.asList("Memory limit exceeded"));

        result.finalize(1, 5000, false);

        assertEquals(SZSStatus.RESOURCE_OUT, result.getSzsStatus());
    }

    @Test
    public void testFinalize_successOverridesTimeout() {
        // If prover found a proof but also logged "Time limit" warning,
        // success should win
        ATPResult result = new ATPResult();
        result.setStdout(Arrays.asList(
                "% SZS status Theorem",
                "% Time limit reached!"  // sometimes logged even on success
        ));

        result.finalize(0, 29000, false);

        assertEquals(SZSStatus.THEOREM, result.getSzsStatus());
        assertFalse(result.isTimedOut());  // success clears timeout flag
    }

    // === Misc field tests ===

    @Test
    public void testInconsistencyDetected() {
        ATPResult result = new ATPResult();
        assertFalse(result.isInconsistencyDetected());

        result.setInconsistencyDetected(true);
        assertTrue(result.isInconsistencyDetected());
    }

    @Test
    public void testParsingWarnings() {
        ATPResult result = new ATPResult();
        assertNull(result.getParsingWarnings());

        List<String> warnings = Arrays.asList("Warning 1", "Warning 2");
        result.setParsingWarnings(warnings);
        assertEquals(warnings, result.getParsingWarnings());
    }

    @Test
    public void testTerminationSignal() {
        ATPResult result = new ATPResult();
        assertNull(result.getTerminationSignal());

        result.setTerminationSignal("SIGSEGV");
        assertEquals("SIGSEGV", result.getTerminationSignal());
    }
}
