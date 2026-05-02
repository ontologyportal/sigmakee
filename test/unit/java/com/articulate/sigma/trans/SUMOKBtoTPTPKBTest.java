package com.articulate.sigma.trans;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for utility methods in {@link SUMOKBtoTPTPKB}.
 */
public class SUMOKBtoTPTPKBTest {

    // -----------------------------------------------------------------------
    // isTptpArithmeticLiteral — integers
    // -----------------------------------------------------------------------

    @Test
    public void arithmeticLiteralInteger() {
        assertTrue(SUMOKBtoTPTPKB.isTptpArithmeticLiteral("0"));
        assertTrue(SUMOKBtoTPTPKB.isTptpArithmeticLiteral("42"));
        assertTrue(SUMOKBtoTPTPKB.isTptpArithmeticLiteral("-7"));
        assertTrue(SUMOKBtoTPTPKB.isTptpArithmeticLiteral("1000000"));
    }

    @Test
    public void arithmeticLiteralRational() {
        assertTrue(SUMOKBtoTPTPKB.isTptpArithmeticLiteral("1/3"));
        assertTrue(SUMOKBtoTPTPKB.isTptpArithmeticLiteral("-2/7"));
        assertTrue(SUMOKBtoTPTPKB.isTptpArithmeticLiteral("0/1"));
    }

    @Test
    public void arithmeticLiteralReal() {
        assertTrue(SUMOKBtoTPTPKB.isTptpArithmeticLiteral("3.14"));
        assertTrue(SUMOKBtoTPTPKB.isTptpArithmeticLiteral("-0.5"));
        assertTrue(SUMOKBtoTPTPKB.isTptpArithmeticLiteral("1."));    // trailing dot
        assertTrue(SUMOKBtoTPTPKB.isTptpArithmeticLiteral(".5"));    // leading dot
    }

    // -----------------------------------------------------------------------
    // isTptpArithmeticLiteral — non-literals (SUMO constants / edge cases)
    // -----------------------------------------------------------------------

    @Test
    public void nonLiteralSymbols() {
        assertFalse(SUMOKBtoTPTPKB.isTptpArithmeticLiteral("NumberE"));
        assertFalse(SUMOKBtoTPTPKB.isTptpArithmeticLiteral("Pi"));
        assertFalse(SUMOKBtoTPTPKB.isTptpArithmeticLiteral("RealNumber"));
        assertFalse(SUMOKBtoTPTPKB.isTptpArithmeticLiteral("s__42"));
        assertFalse(SUMOKBtoTPTPKB.isTptpArithmeticLiteral("abstractCounterpart"));
    }

    @Test
    public void nullAndEmpty() {
        assertFalse(SUMOKBtoTPTPKB.isTptpArithmeticLiteral(null));
        assertFalse(SUMOKBtoTPTPKB.isTptpArithmeticLiteral(""));
    }
}
