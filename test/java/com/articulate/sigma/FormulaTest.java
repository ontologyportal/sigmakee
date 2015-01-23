package com.articulate.sigma;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FormulaTest {

    @Test
    public void testFormulaRead() {
        String stmt = "(domain date 1 Physical)";
        Formula f = new Formula();
        f.read(stmt);
        assertEquals(stmt, f.theFormula);

        stmt = "(=> (and (instance ?REL ObjectAttitude) (?REL ?AGENT ?THING)) (instance ?THING Physical))";
        f = new Formula();
        f.read(stmt);
        assertEquals(stmt, f.theFormula);

        stmt = "aabbc";
        f = new Formula();
        f.read(stmt);
        assertEquals(stmt, f.theFormula);

    }

    @Test
    public void testRecursiveCdrSimple() {
        String stmt = "(exists (?M))";
        Formula f = new Formula();
        f.read(stmt);

        String car = f.car();
        assertEquals("exists", car);
        Formula cdrF = f.cdrAsFormula();
        assertEquals("((?M))", cdrF.theFormula);

        car = cdrF.car();
        assertEquals("(?M)", car);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("()", cdrF.theFormula);

        car = cdrF.car();
        assertEquals("", car);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("()", cdrF.theFormula);

        car = cdrF.car();
        assertEquals("", car);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("()", cdrF.theFormula);
    }

    @Test
    public void testRecursiveCdrComplex() {
        String stmt = "(time JohnsBirth (MonthFn ?M (YearFn 2000)))";
        Formula f = new Formula();
        f.read(stmt);

        String car = f.car();
        assertEquals("time", car);
        Formula cdrF = f.cdrAsFormula();
        assertEquals("(JohnsBirth (MonthFn ?M (YearFn 2000)))", cdrF.theFormula);

        car = cdrF.car();
        assertEquals("JohnsBirth", car);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("((MonthFn ?M (YearFn 2000)))", cdrF.theFormula);

        String functionStr = cdrF.car();
        assertEquals("(MonthFn ?M (YearFn 2000))", functionStr);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("()", cdrF.theFormula);

        assertTrue(Formula.isFunctionalTerm(functionStr));

        f = new Formula();
        f.read(functionStr);

        car = f.car();
        assertEquals("MonthFn", car);
        cdrF = f.cdrAsFormula();
        assertEquals("(?M (YearFn 2000))", cdrF.theFormula);

        car = cdrF.car();
        assertEquals("?M", car);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("((YearFn 2000))", cdrF.theFormula);

        functionStr = cdrF.car();
        assertEquals("(YearFn 2000)", functionStr);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("()", cdrF.theFormula);

        assertTrue(Formula.isFunctionalTerm(functionStr));

        f = new Formula();
        f.read(functionStr);

        car = f.car();
        assertEquals("YearFn", car);
        cdrF = f.cdrAsFormula();
        assertEquals("(2000)", cdrF.theFormula);

        car = cdrF.car();
        assertEquals("2000", car);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("()", cdrF.theFormula);

        car = cdrF.car();
        assertEquals("", car);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("()", cdrF.theFormula);
    }
}