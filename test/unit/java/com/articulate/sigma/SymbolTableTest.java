package com.articulate.sigma;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SymbolTable}.
 */
public class SymbolTableTest {

    @Test
    public void testInternAssignsSequentialIds() {
        SymbolTable st = new SymbolTable();
        assertEquals(0, st.intern("Dog"));
        assertEquals(1, st.intern("Cat"));
        assertEquals(2, st.intern("Animal"));
    }

    @Test
    public void testInternIsDeterministic() {
        SymbolTable st = new SymbolTable();
        int id1 = st.intern("Dog");
        int id2 = st.intern("Dog");
        assertEquals(id1, id2);
    }

    @Test
    public void testGetNameRoundTrip() {
        SymbolTable st = new SymbolTable();
        int id = st.intern("Elephant");
        assertEquals("Elephant", st.getName(id));
    }

    @Test
    public void testGetNameOutOfRange() {
        SymbolTable st = new SymbolTable();
        assertNull(st.getName(-1));
        assertNull(st.getName(0));
        assertNull(st.getName(99));
    }

    @Test
    public void testLookupKnown() {
        SymbolTable st = new SymbolTable();
        st.intern("Dog");
        assertEquals(0, st.lookup("Dog"));
    }

    @Test
    public void testLookupUnknownReturnsMinusOne() {
        SymbolTable st = new SymbolTable();
        assertEquals(-1, st.lookup("Unknown"));
    }

    @Test
    public void testLookupDoesNotRegister() {
        SymbolTable st = new SymbolTable();
        st.lookup("Dog");
        assertEquals(0, st.size());
        assertFalse(st.contains("Dog"));
    }

    @Test
    public void testSize() {
        SymbolTable st = new SymbolTable();
        assertEquals(0, st.size());
        st.intern("A");
        assertEquals(1, st.size());
        st.intern("B");
        assertEquals(2, st.size());
        st.intern("A"); // duplicate — no change
        assertEquals(2, st.size());
    }

    @Test
    public void testContains() {
        SymbolTable st = new SymbolTable();
        assertFalse(st.contains("Dog"));
        st.intern("Dog");
        assertTrue(st.contains("Dog"));
    }

    @Test
    public void testCapacityConstructor() {
        SymbolTable st = new SymbolTable(1000);
        assertEquals(0, st.size());
        int id = st.intern("Test");
        assertEquals(0, id);
        assertEquals("Test", st.getName(id));
    }
}
