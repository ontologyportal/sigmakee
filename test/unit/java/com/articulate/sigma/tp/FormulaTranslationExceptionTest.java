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
 * Unit tests for FormulaTranslationException.
 */
public class FormulaTranslationExceptionTest {

    // === Constructor tests ===

    @Test
    public void testConstructor_messageAndLanguage() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Unknown predicate 'foo'", "FOF");

        assertTrue(ex.getMessage().contains("FOF"));
        assertTrue(ex.getMessage().contains("failed"));
        assertTrue(ex.getMessage().contains("Unknown predicate"));
        assertEquals("FOF", ex.getTargetLanguage());
        assertEquals(-1, ex.getErrorLine());
        assertEquals(-1, ex.getErrorColumn());
    }

    @Test
    public void testConstructor_withPosition() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Syntax error", "TFF", 42, 10);

        assertEquals("TFF", ex.getTargetLanguage());
        assertEquals(42, ex.getErrorLine());
        assertEquals(10, ex.getErrorColumn());
        assertTrue(ex.hasPosition());
    }

    @Test
    public void testConstructor_withCause() {
        Throwable cause = new IllegalArgumentException("Invalid formula");
        FormulaTranslationException ex = new FormulaTranslationException(
                "Translation failed", "THF", cause);

        assertSame(cause, ex.getCause());
        assertEquals("THF", ex.getTargetLanguage());
    }

    @Test
    public void testConstructor_withLineAndOutput() {
        List<String> stdout = Arrays.asList("Processing formula...");
        List<String> stderr = Arrays.asList("Error at line 5");

        FormulaTranslationException ex = new FormulaTranslationException(
                "Parse error", "TFF", 5, stdout, stderr);

        assertEquals(5, ex.getErrorLine());
        assertEquals("TFF", ex.getTargetLanguage());
        assertTrue(ex.hasStdout());
        assertTrue(ex.hasStderr());
    }

    // === Message tests ===

    @Test
    public void testMessage_FOF() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Invalid arity", "FOF");

        String message = ex.getMessage();
        assertTrue(message.contains("FOF"));
        assertTrue(message.contains("failed"));
        assertTrue(message.contains("Invalid arity"));
    }

    @Test
    public void testMessage_TFF() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Type mismatch", "TFF");

        String message = ex.getMessage();
        assertTrue(message.contains("TFF"));
    }

    @Test
    public void testMessage_THF() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Higher-order construct not supported", "THF");

        String message = ex.getMessage();
        assertTrue(message.contains("THF"));
    }

    @Test
    public void testMessage_nullLanguage() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", null);

        String message = ex.getMessage();
        assertTrue(message.contains("TPTP")); // fallback
    }

    @Test
    public void testMessage_nullErrorMessage() {
        FormulaTranslationException ex = new FormulaTranslationException(
                null, "FOF");

        String message = ex.getMessage();
        assertTrue(message.contains("FOF"));
        assertTrue(message.contains("failed"));
    }

    @Test
    public void testMessage_emptyErrorMessage() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "", "FOF");

        String message = ex.getMessage();
        assertTrue(message.contains("FOF"));
        assertTrue(message.contains("failed"));
    }

    // === Position tests ===

    @Test
    public void testHasPosition_true() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", "FOF", 10, 5);

        assertTrue(ex.hasPosition());
    }

    @Test
    public void testHasPosition_false_noLine() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", "FOF");

        assertFalse(ex.hasPosition());
    }

    @Test
    public void testHasPosition_lineZero() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", "FOF", 0, 1);

        assertFalse(ex.hasPosition()); // line must be > 0
    }

    @Test
    public void testHasPosition_lineNegative() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", "FOF", -1, 1);

        assertFalse(ex.hasPosition());
    }

    @Test
    public void testGetErrorLine() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", "FOF", 42, 10);

        assertEquals(42, ex.getErrorLine());
    }

    @Test
    public void testGetErrorColumn() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", "FOF", 42, 10);

        assertEquals(10, ex.getErrorColumn());
    }

    @Test
    public void testGetErrorColumn_negative() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", "FOF", 42, -1);

        assertEquals(-1, ex.getErrorColumn());
    }

    // === Error detail tests ===

    @Test
    public void testGetErrorDetail() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Unknown predicate 'bar'", "FOF");

        assertEquals("Unknown predicate 'bar'", ex.getErrorDetail());
    }

    @Test
    public void testGetTargetLanguage() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", "TFF");

        assertEquals("TFF", ex.getTargetLanguage());
    }

    // === Suggestion tests ===

    @Test
    public void testGetSuggestion() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", "FOF");

        String suggestion = ex.getSuggestion();
        assertNotNull(suggestion);
        assertTrue(suggestion.contains("SUO-KIF"));
        assertTrue(suggestion.contains("FOF"));
    }

    @Test
    public void testGetSuggestion_containsCommonIssues() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", "TFF");

        String suggestion = ex.getSuggestion();

        // Should mention common issues
        assertTrue(suggestion.contains("parentheses"));
        assertTrue(suggestion.contains("predicates") || suggestion.contains("functions"));
        assertTrue(suggestion.contains("variable"));
    }

    @Test
    public void testGetSuggestion_withLineNumber() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", "FOF", 42, Collections.emptyList(), Collections.emptyList());

        String suggestion = ex.getSuggestion();
        assertTrue(suggestion.contains("SUMO"));
    }

    @Test
    public void testGetSuggestion_nullLanguage() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", null);

        String suggestion = ex.getSuggestion();
        assertNotNull(suggestion);
        // Should have a fallback
    }

    // === Detailed message tests ===

    @Test
    public void testGetDetailedMessage_noPosition() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Unknown predicate", "FOF");

        String detailed = ex.getDetailedMessage();
        assertTrue(detailed.contains("FOF"));
        assertTrue(detailed.contains("failed"));
    }

    @Test
    public void testGetDetailedMessage_withPosition() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Syntax error", "TFF", 42, 10);

        String detailed = ex.getDetailedMessage();
        assertTrue(detailed.contains("line 42"));
        assertTrue(detailed.contains("column 10"));
    }

    @Test
    public void testGetDetailedMessage_withLineOnlyNoColumn() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", "FOF", 42, -1);

        String detailed = ex.getDetailedMessage();
        assertTrue(detailed.contains("line 42"));
        // Should not mention column if -1
    }

    @Test
    public void testGetDetailedMessage_includesSuggestion() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", "FOF");

        String detailed = ex.getDetailedMessage();
        assertTrue(detailed.contains("SUO-KIF"));
    }

    // === Inheritance tests ===

    @Test
    public void testExtendsATPException() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", "FOF");

        assertTrue(ex instanceof ATPException);
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    public void testEngineNameNull() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", "FOF");

        // Engine name should be null since translation happens before prover runs
        assertNull(ex.getEngineName());
    }

    // === Language-specific tests ===

    @Test
    public void testFOFTranslation() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Row variables not supported in FOF", "FOF");

        assertTrue(ex.getMessage().contains("FOF"));
        assertEquals("FOF", ex.getTargetLanguage());
    }

    @Test
    public void testTFFTranslation() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Type declaration error", "TFF");

        assertTrue(ex.getMessage().contains("TFF"));
        assertEquals("TFF", ex.getTargetLanguage());
    }

    @Test
    public void testTHFTranslation() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Lambda expression error", "THF");

        assertTrue(ex.getMessage().contains("THF"));
        assertEquals("THF", ex.getTargetLanguage());
    }

    // === Edge cases ===

    @Test
    public void testEmptyStdoutStderr() {
        List<String> stdout = Collections.emptyList();
        List<String> stderr = Collections.emptyList();

        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", "FOF", 1, stdout, stderr);

        assertFalse(ex.hasStdout());
        assertFalse(ex.hasStderr());
    }

    @Test
    public void testNullStdoutStderr() {
        FormulaTranslationException ex = new FormulaTranslationException(
                "Error", "FOF", 1, null, null);

        assertFalse(ex.hasStdout());
        assertFalse(ex.hasStderr());
    }
}
