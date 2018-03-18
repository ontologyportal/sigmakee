package com.articulate.sigma;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class FormulaUtilTest {

    /** ***************************************************************
     */
    @Test
    public void testToProlog() {

        String stmt = "(birthplace ?animal ?LOC)";
        Formula f = new Formula(stmt);
        String result = FormulaUtil.toProlog(f);
        System.out.println("FormulaUtilTest.testToProlog(): "  + result);
        assertEquals("birthplace(?animal,?LOC)", result);
    }

}