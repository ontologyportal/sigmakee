/** This code is copyright Articulate Software (c) 2024.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
*/

package com.articulate.sigma.tp;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for ExecutableNotFoundException.
 */
public class ExecutableNotFoundExceptionTest {

    @Test
    public void testConstructor_allFields() {
        ExecutableNotFoundException ex = new ExecutableNotFoundException(
                "Vampire", "/usr/local/bin/vampire", "vampire");

        assertEquals("Vampire", ex.getEngineName());
        assertEquals("/usr/local/bin/vampire", ex.getExecutablePath());
        assertEquals("vampire", ex.getConfigKey());
    }

    @Test
    public void testMessage_containsAllInfo() {
        ExecutableNotFoundException ex = new ExecutableNotFoundException(
                "Vampire", "/opt/vampire/bin/vampire", "vampire");

        String message = ex.getMessage();
        assertTrue(message.contains("Vampire"));
        assertTrue(message.contains("/opt/vampire/bin/vampire"));
        assertTrue(message.contains("vampire"));
        assertTrue(message.contains("not found"));
    }

    @Test
    public void testMessage_nullEngineName() {
        ExecutableNotFoundException ex = new ExecutableNotFoundException(
                null, "/path/to/prover", "prover");

        String message = ex.getMessage();
        assertTrue(message.contains("Prover"));
        assertTrue(message.contains("not found"));
    }

    @Test
    public void testMessage_nullPath() {
        ExecutableNotFoundException ex = new ExecutableNotFoundException(
                "EProver", null, "eprover");

        String message = ex.getMessage();
        assertTrue(message.contains("EProver"));
        // Should not contain "at:" if path is null
        assertFalse(message.contains("at: null"));
    }

    @Test
    public void testMessage_emptyPath() {
        ExecutableNotFoundException ex = new ExecutableNotFoundException(
                "LEO-III", "", "leoExecutable");

        String message = ex.getMessage();
        assertTrue(message.contains("LEO-III"));
        // Should not contain "at:" for empty path
    }

    @Test
    public void testMessage_nullConfigKey() {
        ExecutableNotFoundException ex = new ExecutableNotFoundException(
                "Vampire", "/path/to/vampire", null);

        String message = ex.getMessage();
        assertTrue(message.contains("Vampire"));
        // Should not contain "(config key: null)"
        assertFalse(message.contains("config key: null"));
    }

    @Test
    public void testConstructorWithCause() {
        Throwable cause = new SecurityException("Permission denied");
        ExecutableNotFoundException ex = new ExecutableNotFoundException(
                "Vampire", "/path/to/vampire", "vampire", cause);

        assertSame(cause, ex.getCause());
        assertEquals("Vampire", ex.getEngineName());
        assertEquals("/path/to/vampire", ex.getExecutablePath());
    }

    @Test
    public void testGetSuggestion() {
        ExecutableNotFoundException ex = new ExecutableNotFoundException(
                "Vampire", "/usr/bin/vampire", "vampire");

        String suggestion = ex.getSuggestion();

        assertNotNull(suggestion);
        assertTrue(suggestion.contains("config.xml"));
        assertTrue(suggestion.contains("vampire"));
        assertTrue(suggestion.contains("/usr/bin/vampire"));
        assertTrue(suggestion.contains("execute permissions"));
        assertTrue(suggestion.contains("chmod"));
    }

    @Test
    public void testGetSuggestion_nullConfigKey() {
        ExecutableNotFoundException ex = new ExecutableNotFoundException(
                "Vampire", "/path", null);

        String suggestion = ex.getSuggestion();
        assertNotNull(suggestion);
        assertTrue(suggestion.contains("prover")); // fallback name
    }

    @Test
    public void testGetDetailedMessage() {
        ExecutableNotFoundException ex = new ExecutableNotFoundException(
                "Vampire", "/usr/local/bin/vampire", "vampire");

        String detailed = ex.getDetailedMessage();

        // Should contain the message
        assertTrue(detailed.contains("not found"));
        assertTrue(detailed.contains("Vampire"));

        // Should contain the suggestion
        assertTrue(detailed.contains("config.xml"));
        assertTrue(detailed.contains("chmod"));
    }

    @Test
    public void testExtendsATPException() {
        ExecutableNotFoundException ex = new ExecutableNotFoundException(
                "Vampire", "/path", "vampire");

        assertTrue(ex instanceof ATPException);
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    public void testGetExecutablePath() {
        ExecutableNotFoundException ex = new ExecutableNotFoundException(
                "EProver", "/home/user/TPTP/eprover", "eprover");

        assertEquals("/home/user/TPTP/eprover", ex.getExecutablePath());
    }

    @Test
    public void testGetConfigKey() {
        ExecutableNotFoundException ex = new ExecutableNotFoundException(
                "LEO-III", "/opt/leo/leo3", "leoExecutable");

        assertEquals("leoExecutable", ex.getConfigKey());
    }

    @Test
    public void testForVampire() {
        ExecutableNotFoundException ex = new ExecutableNotFoundException(
                "Vampire", "/opt/vampire/vampire", "vampire");

        assertTrue(ex.getMessage().contains("Vampire"));
        assertTrue(ex.getSuggestion().contains("vampire"));
    }

    @Test
    public void testForEProver() {
        ExecutableNotFoundException ex = new ExecutableNotFoundException(
                "EProver", "/usr/local/bin/eprover", "eprover");

        assertTrue(ex.getMessage().contains("EProver"));
        assertTrue(ex.getSuggestion().contains("eprover"));
    }

    @Test
    public void testForLEO() {
        ExecutableNotFoundException ex = new ExecutableNotFoundException(
                "LEO-III", "/opt/leo-iii/leo3", "leoExecutable");

        assertTrue(ex.getMessage().contains("LEO-III"));
        assertTrue(ex.getSuggestion().contains("leoExecutable"));
    }
}
