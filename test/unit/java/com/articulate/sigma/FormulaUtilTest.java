package com.articulate.sigma;

import com.articulate.sigma.parsing.FormulaAST;
import org.junit.Test;

import static org.junit.Assert.*;

public class FormulaUtilTest {

    /** ***************************************************************
     */
    @Test
    public void testToProlog() {

        String stmt = "(birthplace ?animal ?LOC)";
        FormulaAST f = new FormulaAST(stmt);
        String result = FormulaUtil.toProlog(f);
        System.out.println("FormulaUtilTest.testToProlog(): "  + result);
        assertEquals("birthplace(?animal,?LOC)", result);
    }

}