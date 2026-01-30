package com.articulate.sigma.tp;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for SZSStatus enum
 */
public class SZSStatusTest {

    @Test
    public void testFromString_theorem() {
        assertEquals(SZSStatus.THEOREM, SZSStatus.fromString("Theorem"));
        assertEquals(SZSStatus.THEOREM, SZSStatus.fromString("THEOREM"));
        assertEquals(SZSStatus.THEOREM, SZSStatus.fromString("theorem"));
        assertEquals(SZSStatus.THEOREM, SZSStatus.fromString("THM"));
    }

    @Test
    public void testFromString_timeout() {
        assertEquals(SZSStatus.TIMEOUT, SZSStatus.fromString("Timeout"));
        assertEquals(SZSStatus.TIMEOUT, SZSStatus.fromString("TIMEOUT"));
        assertEquals(SZSStatus.TIMEOUT, SZSStatus.fromString("TMO"));
        assertEquals(SZSStatus.TIMEOUT, SZSStatus.fromString("TimeLIMIT"));
    }

    @Test
    public void testFromString_unsatisfiable() {
        assertEquals(SZSStatus.UNSATISFIABLE, SZSStatus.fromString("Unsatisfiable"));
        assertEquals(SZSStatus.UNSATISFIABLE, SZSStatus.fromString("UNSAT"));
    }

    @Test
    public void testFromString_satisfiable() {
        assertEquals(SZSStatus.SATISFIABLE, SZSStatus.fromString("Satisfiable"));
        assertEquals(SZSStatus.SATISFIABLE, SZSStatus.fromString("SAT"));
    }

    @Test
    public void testFromString_counterSatisfiable() {
        assertEquals(SZSStatus.COUNTER_SATISFIABLE, SZSStatus.fromString("CounterSatisfiable"));
        assertEquals(SZSStatus.COUNTER_SATISFIABLE, SZSStatus.fromString("CSA"));
    }

    @Test
    public void testFromString_gaveUp() {
        assertEquals(SZSStatus.GAVE_UP, SZSStatus.fromString("GaveUp"));
        assertEquals(SZSStatus.GAVE_UP, SZSStatus.fromString("GUP"));
    }

    @Test
    public void testFromString_resourceOut() {
        assertEquals(SZSStatus.RESOURCE_OUT, SZSStatus.fromString("ResourceOut"));
        assertEquals(SZSStatus.RESOURCE_OUT, SZSStatus.fromString("RSO"));
        assertEquals(SZSStatus.RESOURCE_OUT, SZSStatus.fromString("MemoryLimit"));
    }

    @Test
    public void testFromString_inputError() {
        assertEquals(SZSStatus.INPUT_ERROR, SZSStatus.fromString("InputError"));
        assertEquals(SZSStatus.INPUT_ERROR, SZSStatus.fromString("INE"));
    }

    @Test
    public void testFromString_syntaxError() {
        assertEquals(SZSStatus.SYNTAX_ERROR, SZSStatus.fromString("SyntaxError"));
        assertEquals(SZSStatus.SYNTAX_ERROR, SZSStatus.fromString("SYE"));
    }

    @Test
    public void testFromString_typeError() {
        assertEquals(SZSStatus.TYPE_ERROR, SZSStatus.fromString("TypeError"));
        assertEquals(SZSStatus.TYPE_ERROR, SZSStatus.fromString("TYE"));
    }

    @Test
    public void testFromString_unknown() {
        assertEquals(SZSStatus.UNKNOWN, SZSStatus.fromString("Unknown"));
        assertEquals(SZSStatus.UNKNOWN, SZSStatus.fromString("UNK"));
    }

    @Test
    public void testFromString_nullReturnsUnknown() {
        assertEquals(SZSStatus.UNKNOWN, SZSStatus.fromString(null));
    }

    @Test
    public void testFromString_emptyReturnsUnknown() {
        assertEquals(SZSStatus.UNKNOWN, SZSStatus.fromString(""));
    }

    @Test
    public void testFromString_unrecognizedReturnsUnknown() {
        assertEquals(SZSStatus.UNKNOWN, SZSStatus.fromString("SomethingElse"));
    }

    @Test
    public void testFromString_withSzsPrefix() {
        assertEquals(SZSStatus.THEOREM, SZSStatus.fromString("SZS status Theorem"));
    }

    @Test
    public void testFromString_withForSuffix() {
        assertEquals(SZSStatus.THEOREM, SZSStatus.fromString("Theorem for SUMO"));
    }

    @Test
    public void testFromString_withDiagnostics() {
        assertEquals(SZSStatus.TYPE_ERROR, SZSStatus.fromString("TypeError : line 42"));
    }

    @Test
    public void testIsSuccess() {
        assertTrue(SZSStatus.THEOREM.isSuccess());
        assertTrue(SZSStatus.UNSATISFIABLE.isSuccess());
        assertTrue(SZSStatus.SATISFIABLE.isSuccess());
        assertTrue(SZSStatus.COUNTER_SATISFIABLE.isSuccess());

        assertFalse(SZSStatus.TIMEOUT.isSuccess());
        assertFalse(SZSStatus.UNKNOWN.isSuccess());
        assertFalse(SZSStatus.OS_ERROR.isSuccess());
    }

    @Test
    public void testIsFailure() {
        assertTrue(SZSStatus.TIMEOUT.isFailure());
        assertTrue(SZSStatus.RESOURCE_OUT.isFailure());

        assertFalse(SZSStatus.THEOREM.isFailure());
        assertFalse(SZSStatus.UNKNOWN.isFailure());
        assertFalse(SZSStatus.OS_ERROR.isFailure());
    }

    @Test
    public void testIsError() {
        assertTrue(SZSStatus.INPUT_ERROR.isError());
        assertTrue(SZSStatus.SYNTAX_ERROR.isError());
        assertTrue(SZSStatus.TYPE_ERROR.isError());
        assertTrue(SZSStatus.OS_ERROR.isError());
        assertTrue(SZSStatus.CRASHED.isError());
        assertTrue(SZSStatus.NOT_RUN.isError());

        assertFalse(SZSStatus.THEOREM.isError());
        assertFalse(SZSStatus.TIMEOUT.isError());
        assertFalse(SZSStatus.UNKNOWN.isError());
    }

    @Test
    public void testIsUnknown() {
        assertTrue(SZSStatus.UNKNOWN.isUnknown());
        assertTrue(SZSStatus.GAVE_UP.isUnknown());
        assertTrue(SZSStatus.INCOMPLETE.isUnknown());

        assertFalse(SZSStatus.THEOREM.isUnknown());
        assertFalse(SZSStatus.TIMEOUT.isUnknown());
    }

    @Test
    public void testFromExitCode_zero() {
        assertEquals(SZSStatus.UNKNOWN, SZSStatus.fromExitCode(0, false));
    }

    @Test
    public void testFromExitCode_timeout() {
        assertEquals(SZSStatus.TIMEOUT, SZSStatus.fromExitCode(0, true));
        assertEquals(SZSStatus.TIMEOUT, SZSStatus.fromExitCode(1, true));
    }

    @Test
    public void testFromExitCode_signal() {
        // 128 + 11 (SIGSEGV) = 139
        assertEquals(SZSStatus.CRASHED, SZSStatus.fromExitCode(139, false));
        // 128 + 9 (SIGKILL) = 137
        assertEquals(SZSStatus.CRASHED, SZSStatus.fromExitCode(137, false));
    }

    @Test
    public void testFromExitCode_nonZero() {
        assertEquals(SZSStatus.ERROR, SZSStatus.fromExitCode(1, false));
        assertEquals(SZSStatus.ERROR, SZSStatus.fromExitCode(2, false));
    }

    @Test
    public void testGetCssClass() {
        assertEquals("szs-success", SZSStatus.THEOREM.getCssClass());
        assertEquals("szs-failure", SZSStatus.TIMEOUT.getCssClass());
        assertEquals("szs-error", SZSStatus.OS_ERROR.getCssClass());
        assertEquals("szs-unknown", SZSStatus.UNKNOWN.getCssClass());
    }

    @Test
    public void testGetTptpName() {
        assertEquals("Theorem", SZSStatus.THEOREM.getTptpName());
        assertEquals("Timeout", SZSStatus.TIMEOUT.getTptpName());
        assertEquals("OSError", SZSStatus.OS_ERROR.getTptpName());
    }

    @Test
    public void testGetDescription() {
        assertNotNull(SZSStatus.THEOREM.getDescription());
        assertTrue(SZSStatus.THEOREM.getDescription().length() > 0);
    }

    @Test
    public void testToString() {
        assertEquals("Theorem", SZSStatus.THEOREM.toString());
        assertEquals("Timeout", SZSStatus.TIMEOUT.toString());
    }
}
