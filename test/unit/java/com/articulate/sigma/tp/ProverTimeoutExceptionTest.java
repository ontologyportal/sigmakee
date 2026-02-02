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
 * Unit tests for ProverTimeoutException.
 */
public class ProverTimeoutExceptionTest {

    // === Constructor tests ===

    @Test
    public void testConstructor_hardTimeout() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 30000, 30500, true);

        assertEquals("Vampire", ex.getEngineName());
        assertEquals(30000, ex.getTimeoutMs());
        assertEquals(30500, ex.getElapsedMs());
        assertTrue(ex.isHardTimeout());
        assertFalse(ex.isSoftTimeout());
    }

    @Test
    public void testConstructor_softTimeout() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 30000, 29800, false);

        assertEquals("Vampire", ex.getEngineName());
        assertEquals(30000, ex.getTimeoutMs());
        assertEquals(29800, ex.getElapsedMs());
        assertFalse(ex.isHardTimeout());
        assertTrue(ex.isSoftTimeout());
    }

    @Test
    public void testConstructorWithOutput() {
        List<String> stdout = Arrays.asList("% SZS status Timeout", "% Time limit reached");
        List<String> stderr = Collections.emptyList();
        ATPResult result = new ATPResult();
        result.setSzsStatus(SZSStatus.TIMEOUT);

        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 30000, 30100, true, stdout, stderr, result);

        assertEquals(30000, ex.getTimeoutMs());
        assertTrue(ex.hasStdout());
        assertNotNull(ex.getResult());
        assertEquals(SZSStatus.TIMEOUT, ex.getResult().getSzsStatus());
    }

    // === Message tests ===

    @Test
    public void testMessage_hardTimeout() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 30000, 30500, true);

        String message = ex.getMessage();
        assertTrue(message.contains("Hard timeout"));
        assertTrue(message.contains("Vampire"));
        assertTrue(message.contains("30s"));
    }

    @Test
    public void testMessage_softTimeout() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 60000, 59500, false);

        String message = ex.getMessage();
        assertTrue(message.contains("Timeout"));
        assertFalse(message.contains("Hard timeout"));
        assertTrue(message.contains("Vampire"));
        assertTrue(message.contains("60s"));
    }

    @Test
    public void testMessage_shortTimeout() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "EProver", 5000, 5100, true);

        String message = ex.getMessage();
        assertTrue(message.contains("EProver"));
        assertTrue(message.contains("5s"));
    }

    // === Time conversion tests ===

    @Test
    public void testGetTimeoutSeconds() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 45000, 45100, true);

        assertEquals(45, ex.getTimeoutSeconds());
    }

    @Test
    public void testGetElapsedSeconds() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 30000, 32500, true);

        assertEquals(32, ex.getElapsedSeconds());
    }

    @Test
    public void testTimeConversion_subSecond() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 500, 600, true);

        assertEquals(0, ex.getTimeoutSeconds());
        assertEquals(0, ex.getElapsedSeconds());
    }

    // === Suggestion tests ===

    @Test
    public void testGetSuggestion() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 30000, 30100, true);

        String suggestion = ex.getSuggestion();
        assertNotNull(suggestion);
        assertTrue(suggestion.contains("timeout"));
        assertTrue(suggestion.contains("30s")); // current timeout
        assertTrue(suggestion.contains("simplify") || suggestion.contains("Simplify"));
    }

    @Test
    public void testGetSuggestion_containsAllOptions() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 60000, 60100, true);

        String suggestion = ex.getSuggestion();

        // Should contain multiple suggestions
        assertTrue(suggestion.contains("timeout") || suggestion.contains("Timeout"));
        assertTrue(suggestion.contains("query") || suggestion.contains("Query"));
    }

    // === Detailed message tests ===

    @Test
    public void testGetDetailedMessage() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 30000, 30500, true);

        String detailed = ex.getDetailedMessage();

        assertTrue(detailed.contains("Vampire"));
        assertTrue(detailed.contains("30000"));  // timeout ms
        assertTrue(detailed.contains("30500"));  // elapsed ms
        assertTrue(detailed.contains("Hard") || detailed.contains("process killed"));
    }

    @Test
    public void testGetDetailedMessage_softTimeout() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 30000, 29500, false);

        String detailed = ex.getDetailedMessage();

        assertTrue(detailed.contains("Soft") || detailed.contains("self-reported"));
    }

    @Test
    public void testGetDetailedMessage_includesSuggestion() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 30000, 30100, true);

        String detailed = ex.getDetailedMessage();

        // Should include suggestion text
        assertTrue(detailed.contains("timeout") || detailed.contains("Timeout"));
    }

    // === Edge cases ===

    @Test
    public void testElapsedLessThanTimeout() {
        // This can happen when prover self-reports timeout slightly early
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 30000, 29500, false);

        assertEquals(29500, ex.getElapsedMs());
        assertFalse(ex.isHardTimeout());
    }

    @Test
    public void testElapsedMuchGreaterThanTimeout() {
        // This can happen when process cleanup takes time
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 30000, 35000, true);

        assertEquals(35000, ex.getElapsedMs());
        assertTrue(ex.isHardTimeout());
    }

    @Test
    public void testZeroTimeout() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 0, 100, true);

        assertEquals(0, ex.getTimeoutMs());
        assertEquals(0, ex.getTimeoutSeconds());
    }

    // === Inheritance tests ===

    @Test
    public void testExtendsATPException() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 30000, 30100, true);

        assertTrue(ex instanceof ATPException);
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    public void testEngineName_EProver() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "EProver", 30000, 30100, true);

        assertTrue(ex.getMessage().contains("EProver"));
    }

    @Test
    public void testEngineName_LEO() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "LEO-III", 30000, 30100, true);

        assertTrue(ex.getMessage().contains("LEO-III"));
    }

    // === Boolean accessor tests ===

    @Test
    public void testIsHardTimeout_true() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 30000, 30100, true);
        assertTrue(ex.isHardTimeout());
    }

    @Test
    public void testIsHardTimeout_false() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 30000, 30100, false);
        assertFalse(ex.isHardTimeout());
    }

    @Test
    public void testIsSoftTimeout_true() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 30000, 30100, false);
        assertTrue(ex.isSoftTimeout());
    }

    @Test
    public void testIsSoftTimeout_false() {
        ProverTimeoutException ex = new ProverTimeoutException(
                "Vampire", 30000, 30100, true);
        assertFalse(ex.isSoftTimeout());
    }
}
