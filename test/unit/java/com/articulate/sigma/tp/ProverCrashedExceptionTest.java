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
 * Unit tests for ProverCrashedException.
 */
public class ProverCrashedExceptionTest {

    // === Signal extraction tests ===

    @Test
    public void testSignalExtraction_SIGSEGV() {
        // Exit code 139 = 128 + 11 (SIGSEGV)
        ProverCrashedException ex = new ProverCrashedException("Vampire", 139);

        assertTrue(ex.wasKilledBySignal());
        assertEquals(11, ex.getSignalNumber());
        assertEquals("SIGSEGV", ex.getSignalName());
        assertTrue(ex.wasSegmentationFault());
        assertFalse(ex.wasAborted());
        assertFalse(ex.wasForciblyStopped());
    }

    @Test
    public void testSignalExtraction_SIGKILL() {
        // Exit code 137 = 128 + 9 (SIGKILL)
        ProverCrashedException ex = new ProverCrashedException("Vampire", 137);

        assertTrue(ex.wasKilledBySignal());
        assertEquals(9, ex.getSignalNumber());
        assertEquals("SIGKILL", ex.getSignalName());
        assertTrue(ex.wasForciblyStopped());
        assertFalse(ex.wasSegmentationFault());
    }

    @Test
    public void testSignalExtraction_SIGTERM() {
        // Exit code 143 = 128 + 15 (SIGTERM)
        ProverCrashedException ex = new ProverCrashedException("Vampire", 143);

        assertTrue(ex.wasKilledBySignal());
        assertEquals(15, ex.getSignalNumber());
        assertEquals("SIGTERM", ex.getSignalName());
        assertTrue(ex.wasForciblyStopped());
    }

    @Test
    public void testSignalExtraction_SIGABRT() {
        // Exit code 134 = 128 + 6 (SIGABRT)
        ProverCrashedException ex = new ProverCrashedException("Vampire", 134);

        assertTrue(ex.wasKilledBySignal());
        assertEquals(6, ex.getSignalNumber());
        assertEquals("SIGABRT", ex.getSignalName());
        assertTrue(ex.wasAborted());
        assertFalse(ex.wasSegmentationFault());
    }

    @Test
    public void testSignalExtraction_SIGHUP() {
        // Exit code 129 = 128 + 1 (SIGHUP)
        ProverCrashedException ex = new ProverCrashedException("Vampire", 129);

        assertTrue(ex.wasKilledBySignal());
        assertEquals(1, ex.getSignalNumber());
        assertEquals("SIGHUP", ex.getSignalName());
    }

    @Test
    public void testSignalExtraction_SIGINT() {
        // Exit code 130 = 128 + 2 (SIGINT)
        ProverCrashedException ex = new ProverCrashedException("Vampire", 130);

        assertTrue(ex.wasKilledBySignal());
        assertEquals(2, ex.getSignalNumber());
        assertEquals("SIGINT", ex.getSignalName());
    }

    @Test
    public void testSignalExtraction_SIGQUIT() {
        // Exit code 131 = 128 + 3 (SIGQUIT)
        ProverCrashedException ex = new ProverCrashedException("Vampire", 131);

        assertTrue(ex.wasKilledBySignal());
        assertEquals(3, ex.getSignalNumber());
        assertEquals("SIGQUIT", ex.getSignalName());
    }

    @Test
    public void testSignalExtraction_SIGXCPU() {
        // Exit code 152 = 128 + 24 (SIGXCPU)
        ProverCrashedException ex = new ProverCrashedException("Vampire", 152);

        assertTrue(ex.wasKilledBySignal());
        assertEquals(24, ex.getSignalNumber());
        assertEquals("SIGXCPU", ex.getSignalName());
        assertTrue(ex.wasCpuLimitExceeded());
    }

    @Test
    public void testSignalExtraction_unknownSignal() {
        // Exit code 155 = 128 + 27 (unknown signal)
        ProverCrashedException ex = new ProverCrashedException("Vampire", 155);

        assertTrue(ex.wasKilledBySignal());
        assertEquals(27, ex.getSignalNumber());
        assertEquals("SIG27", ex.getSignalName());
    }

    @Test
    public void testSignalExtraction_normalExit() {
        // Exit code 1 - normal error exit, not a signal
        ProverCrashedException ex = new ProverCrashedException("Vampire", 1);

        assertFalse(ex.wasKilledBySignal());
        assertEquals(-1, ex.getSignalNumber());
        assertNull(ex.getSignalName());
        assertFalse(ex.wasSegmentationFault());
        assertFalse(ex.wasAborted());
        assertFalse(ex.wasForciblyStopped());
    }

    @Test
    public void testSignalExtraction_normalExit128() {
        // Exit code 128 is the boundary - not a signal
        ProverCrashedException ex = new ProverCrashedException("Vampire", 128);

        assertFalse(ex.wasKilledBySignal());
        assertEquals(-1, ex.getSignalNumber());
    }

    @Test
    public void testSignalExtraction_exitCode160() {
        // Exit code 160+ is out of signal range
        ProverCrashedException ex = new ProverCrashedException("Vampire", 160);

        assertFalse(ex.wasKilledBySignal());
        assertEquals(-1, ex.getSignalNumber());
    }

    // === Message tests ===

    @Test
    public void testMessage_signalCrash() {
        ProverCrashedException ex = new ProverCrashedException("Vampire", 139);

        String message = ex.getMessage();
        assertTrue(message.contains("Vampire"));
        assertTrue(message.contains("crashed"));
        assertTrue(message.contains("SIGSEGV"));
        assertTrue(message.contains("139"));
    }

    @Test
    public void testMessage_normalError() {
        ProverCrashedException ex = new ProverCrashedException("EProver", 1);

        String message = ex.getMessage();
        assertTrue(message.contains("EProver"));
        assertTrue(message.contains("exited"));
        assertTrue(message.contains("error code"));
        assertTrue(message.contains("1"));
        assertFalse(message.contains("signal"));
    }

    // === Constructor with output tests ===

    @Test
    public void testConstructorWithOutput() {
        List<String> stdout = Arrays.asList("Starting prover...", "Processing formulas...");
        List<String> stderr = Arrays.asList("Segmentation fault (core dumped)");
        ATPResult result = new ATPResult();
        result.setSzsStatus(SZSStatus.CRASHED);

        ProverCrashedException ex = new ProverCrashedException("Vampire", 139, stdout, stderr, result);

        assertEquals(139, ex.getExitCode());
        assertTrue(ex.hasStdout());
        assertTrue(ex.hasStderr());
        assertNotNull(ex.getResult());
        assertEquals(SZSStatus.CRASHED, ex.getResult().getSzsStatus());
    }

    @Test
    public void testConstructorWithNullOutput() {
        ProverCrashedException ex = new ProverCrashedException("Vampire", 139, null, null, null);

        assertEquals(139, ex.getExitCode());
        assertTrue(ex.wasSegmentationFault());
    }

    // === Suggestion tests ===

    @Test
    public void testGetSuggestion_SIGSEGV() {
        ProverCrashedException ex = new ProverCrashedException("Vampire", 139);

        String suggestion = ex.getSuggestion();
        assertNotNull(suggestion);
        assertTrue(suggestion.contains("SIGSEGV"));
        assertTrue(suggestion.contains("malformed formula"));
        assertTrue(suggestion.contains("syntax"));
    }

    @Test
    public void testGetSuggestion_SIGABRT() {
        ProverCrashedException ex = new ProverCrashedException("Vampire", 134);

        String suggestion = ex.getSuggestion();
        assertNotNull(suggestion);
        assertTrue(suggestion.contains("SIGABRT"));
        assertTrue(suggestion.contains("assertion"));
    }

    @Test
    public void testGetSuggestion_SIGKILL() {
        ProverCrashedException ex = new ProverCrashedException("Vampire", 137);

        String suggestion = ex.getSuggestion();
        assertNotNull(suggestion);
        assertTrue(suggestion.contains("killed"));
        assertTrue(suggestion.contains("memory") || suggestion.contains("Memory"));
    }

    @Test
    public void testGetSuggestion_SIGXCPU() {
        ProverCrashedException ex = new ProverCrashedException("Vampire", 152);

        String suggestion = ex.getSuggestion();
        assertNotNull(suggestion);
        assertTrue(suggestion.contains("CPU time"));
        assertTrue(suggestion.contains("timeout"));
    }

    @Test
    public void testGetSuggestion_exitCode1() {
        ProverCrashedException ex = new ProverCrashedException("Vampire", 1);

        String suggestion = ex.getSuggestion();
        assertNotNull(suggestion);
        assertTrue(suggestion.contains("Exit code 1"));
        assertTrue(suggestion.contains("prover output"));
    }

    @Test
    public void testGetSuggestion_genericError() {
        ProverCrashedException ex = new ProverCrashedException("Vampire", 2);

        String suggestion = ex.getSuggestion();
        assertNotNull(suggestion);
        assertTrue(suggestion.contains("prover output"));
    }

    // === Detailed message tests ===

    @Test
    public void testGetDetailedMessage_withSignal() {
        ProverCrashedException ex = new ProverCrashedException("Vampire", 139);

        String detailed = ex.getDetailedMessage();
        assertTrue(detailed.contains("Vampire"));
        assertTrue(detailed.contains("SIGSEGV"));
        assertTrue(detailed.contains("11")); // signal number
        assertTrue(detailed.contains("Suggestion") || detailed.contains("segmentation"));
    }

    @Test
    public void testGetDetailedMessage_withStderr() {
        List<String> stderr = Arrays.asList(
                "Error: unknown predicate 'foo'",
                "At line 42, column 10"
        );
        ProverCrashedException ex = new ProverCrashedException("Vampire", 1, null, stderr, null);

        String detailed = ex.getDetailedMessage();
        assertTrue(detailed.contains("unknown predicate"));
        assertTrue(detailed.contains("line 42"));
    }

    @Test
    public void testGetDetailedMessage_truncatesLongStderr() {
        // Create 20 stderr lines
        List<String> stderr = Arrays.asList(
                "err1", "err2", "err3", "err4", "err5",
                "err6", "err7", "err8", "err9", "err10",
                "err11", "err12", "err13", "err14", "err15",
                "err16", "err17", "err18", "err19", "err20"
        );
        ProverCrashedException ex = new ProverCrashedException("Vampire", 1, null, stderr, null);

        String detailed = ex.getDetailedMessage();
        assertTrue(detailed.contains("err1"));
        assertTrue(detailed.contains("err15")); // first 15 shown
        assertTrue(detailed.contains("more lines"));
    }

    // === Inheritance tests ===

    @Test
    public void testExtendsATPException() {
        ProverCrashedException ex = new ProverCrashedException("Vampire", 139);

        assertTrue(ex instanceof ATPException);
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    public void testGetExitCode() {
        ProverCrashedException ex = new ProverCrashedException("Vampire", 42);
        assertEquals(42, ex.getExitCode());
    }

    // === Signal constant tests ===

    @Test
    public void testSignalConstants() {
        assertEquals(1, ProverCrashedException.SIGHUP);
        assertEquals(2, ProverCrashedException.SIGINT);
        assertEquals(3, ProverCrashedException.SIGQUIT);
        assertEquals(6, ProverCrashedException.SIGABRT);
        assertEquals(9, ProverCrashedException.SIGKILL);
        assertEquals(11, ProverCrashedException.SIGSEGV);
        assertEquals(15, ProverCrashedException.SIGTERM);
        assertEquals(24, ProverCrashedException.SIGXCPU);
    }
}
