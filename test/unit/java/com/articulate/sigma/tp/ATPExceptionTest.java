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
 * Unit tests for ATPException base class.
 */
public class ATPExceptionTest {

    @Test
    public void testSimpleConstructor() {
        ATPException ex = new ATPException("Test error message", "Vampire");

        assertEquals("Test error message", ex.getMessage());
        assertEquals("Vampire", ex.getEngineName());
        assertTrue(ex.getCommandLine().isEmpty());
        assertNull(ex.getWorkingDirectory());
        assertEquals(0, ex.getTimeoutMs());
        assertTrue(ex.getStdout().isEmpty());
        assertTrue(ex.getStderr().isEmpty());
    }

    @Test
    public void testConstructorWithStdoutStderr() {
        List<String> stdout = Arrays.asList("line1", "line2");
        List<String> stderr = Arrays.asList("error1");

        ATPException ex = new ATPException("Error with output", "EProver", stdout, stderr);

        assertEquals("Error with output", ex.getMessage());
        assertEquals("EProver", ex.getEngineName());
        assertEquals(stdout, ex.getStdout());
        assertEquals(stderr, ex.getStderr());
        assertTrue(ex.hasStdout());
        assertTrue(ex.hasStderr());
    }

    @Test
    public void testConstructorWithCause() {
        Throwable cause = new RuntimeException("root cause");
        ATPException ex = new ATPException("Wrapped error", "LEO-III", cause);

        assertEquals("Wrapped error", ex.getMessage());
        assertEquals("LEO-III", ex.getEngineName());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void testBuilder_allFields() {
        List<String> commandLine = Arrays.asList("vampire", "--mode", "casc", "input.tptp");
        List<String> stdout = Arrays.asList("% SZS status Theorem", "% Proof found");
        List<String> stderr = Arrays.asList("Warning: deprecated option");

        ATPException ex = ATPException.builder()
                .message("Test builder message")
                .engineName("Vampire")
                .commandLine(commandLine)
                .workingDirectory("/tmp/test")
                .timeoutMs(30000)
                .stdout(stdout)
                .stderr(stderr)
                .build();

        assertEquals("Test builder message", ex.getMessage());
        assertEquals("Vampire", ex.getEngineName());
        assertEquals(commandLine, ex.getCommandLine());
        assertEquals("/tmp/test", ex.getWorkingDirectory());
        assertEquals(30000, ex.getTimeoutMs());
        assertEquals(stdout, ex.getStdout());
        assertEquals(stderr, ex.getStderr());
    }

    @Test
    public void testBuilder_withCause() {
        Exception cause = new IllegalArgumentException("bad input");

        ATPException ex = ATPException.builder()
                .message("Error with cause")
                .engineName("Vampire")
                .cause(cause)
                .build();

        assertEquals("Error with cause", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void testBuilder_withStringArrayCommandLine() {
        String[] cmdArray = {"vampire", "--input", "file.tptp"};

        ATPException ex = ATPException.builder()
                .message("Test")
                .commandLine(cmdArray)
                .build();

        assertEquals(3, ex.getCommandLine().size());
        assertEquals("vampire", ex.getCommandLine().get(0));
    }

    @Test
    public void testGetCommandLineString() {
        List<String> commandLine = Arrays.asList("vampire", "--mode", "casc", "input.tptp");

        ATPException ex = ATPException.builder()
                .message("Test")
                .commandLine(commandLine)
                .build();

        assertEquals("vampire --mode casc input.tptp", ex.getCommandLineString());
    }

    @Test
    public void testGetCommandLineString_empty() {
        ATPException ex = new ATPException("Test", "Vampire");
        assertEquals("", ex.getCommandLineString());
    }

    @Test
    public void testHasStdout_true() {
        ATPException ex = new ATPException("Test", "Vampire",
                Arrays.asList("output line"), Collections.emptyList());
        assertTrue(ex.hasStdout());
    }

    @Test
    public void testHasStdout_false() {
        ATPException ex = new ATPException("Test", "Vampire");
        assertFalse(ex.hasStdout());
    }

    @Test
    public void testHasStderr_true() {
        ATPException ex = new ATPException("Test", "Vampire",
                Collections.emptyList(), Arrays.asList("error line"));
        assertTrue(ex.hasStderr());
    }

    @Test
    public void testHasStderr_false() {
        ATPException ex = new ATPException("Test", "Vampire");
        assertFalse(ex.hasStderr());
    }

    @Test
    public void testGetStdoutString() {
        List<String> stdout = Arrays.asList("line1", "line2", "line3");
        ATPException ex = new ATPException("Test", "Vampire", stdout, null);

        assertEquals("line1\nline2\nline3", ex.getStdoutString());
    }

    @Test
    public void testGetStdoutString_empty() {
        ATPException ex = new ATPException("Test", "Vampire");
        assertEquals("", ex.getStdoutString());
    }

    @Test
    public void testGetStderrString() {
        List<String> stderr = Arrays.asList("error1", "error2");
        ATPException ex = new ATPException("Test", "Vampire", null, stderr);

        assertEquals("error1\nerror2", ex.getStderrString());
    }

    @Test
    public void testGetStderrString_empty() {
        ATPException ex = new ATPException("Test", "Vampire");
        assertEquals("", ex.getStderrString());
    }

    @Test
    public void testGetSuggestion_default() {
        ATPException ex = new ATPException("Test", "Vampire");
        String suggestion = ex.getSuggestion();

        assertNotNull(suggestion);
        assertTrue(suggestion.contains("Check"));
    }

    @Test
    public void testGetDetailedMessage_simple() {
        ATPException ex = new ATPException("Test error", "Vampire");
        String detailed = ex.getDetailedMessage();

        assertTrue(detailed.contains("Test error"));
        assertTrue(detailed.contains("Vampire"));
    }

    @Test
    public void testGetDetailedMessage_withContext() {
        List<String> commandLine = Arrays.asList("vampire", "input.tptp");
        List<String> stderr = Arrays.asList("Error on line 5", "Missing parenthesis");

        ATPException ex = ATPException.builder()
                .message("Execution failed")
                .engineName("Vampire")
                .commandLine(commandLine)
                .workingDirectory("/home/user/kb")
                .timeoutMs(30000)
                .stderr(stderr)
                .build();

        String detailed = ex.getDetailedMessage();

        assertTrue(detailed.contains("Execution failed"));
        assertTrue(detailed.contains("Vampire"));
        assertTrue(detailed.contains("vampire input.tptp"));
        assertTrue(detailed.contains("/home/user/kb"));
        assertTrue(detailed.contains("30000"));
        assertTrue(detailed.contains("Error on line 5"));
        assertTrue(detailed.contains("Missing parenthesis"));
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

        ATPException ex = new ATPException("Test", "Vampire", null, stderr);
        String detailed = ex.getDetailedMessage();

        // Should show first 10 and indicate more
        assertTrue(detailed.contains("err1"));
        assertTrue(detailed.contains("err10"));
        assertTrue(detailed.contains("more lines"));
    }

    @Test
    public void testResult_getterSetter() {
        ATPException ex = new ATPException("Test", "Vampire");
        assertNull(ex.getResult());

        ATPResult result = new ATPResult();
        result.setSzsStatus(SZSStatus.THEOREM);
        ex.setResult(result);

        assertSame(result, ex.getResult());
        assertEquals(SZSStatus.THEOREM, ex.getResult().getSzsStatus());
    }

    @Test
    public void testBuilder_withResult() {
        ATPResult result = new ATPResult();
        result.setSzsStatus(SZSStatus.TIMEOUT);

        ATPException ex = ATPException.builder()
                .message("Timeout")
                .engineName("Vampire")
                .result(result)
                .build();

        assertNotNull(ex.getResult());
        assertEquals(SZSStatus.TIMEOUT, ex.getResult().getSzsStatus());
    }

    @Test
    public void testIsRuntimeException() {
        ATPException ex = new ATPException("Test", "Vampire");
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    public void testImmutableCommandLine() {
        List<String> commandLine = Arrays.asList("cmd1", "cmd2");
        ATPException ex = ATPException.builder()
                .message("Test")
                .commandLine(commandLine)
                .build();

        // The returned list should be unmodifiable
        try {
            ex.getCommandLine().add("cmd3");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }
}
