package com.articulate.sigma;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class FormulaTest {

   @Test
    public void testFormulaRead()   {
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

}