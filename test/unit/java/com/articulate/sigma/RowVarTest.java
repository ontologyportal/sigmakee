package com.articulate.sigma;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * These tests follow PredVarInst.test( ), with the exception of that method's call to FormulaPreprocessor.
 * findExplicitTypesInAntecedent( ), which has been put into the FormulaPreprocessorTest class.
 * TODO: See how relevant the line "if (kb.kbCache.transInstOf("exhaustiveAttribute","VariableArityRelation"))"
 * at the start of the original PredVarInst.test( ) method is. Should these tests somehow reflect that?
 */
public class RowVarTest extends UnitTestBase  {

    private static final String stmt1 = "(=>\n" +
            "  (and\n" +
            "    (minValue links ?ARG ?N)\n" +
            "    (links @ARGS)\n" +
            "    (equal ?VAL\n" +
            "      (ListOrderFn\n" +
            "        (ListFn @ARGS) ?ARG)))\n" +
            "  (greaterThan ?VAL ?N))";

    /** ***************************************************************
     */
    @Test
    public void testLinks() {

        Formula f = new Formula();
        f.read(RowVarTest.stmt1);

        RowVars.DEBUG = true;
        HashMap<String,HashSet<String>> rels = RowVars.getRowVarRelations(f);
        HashMap<String,Integer> rowVarMaxArities = RowVars.getRowVarMaxAritiesWithOtherArgs(rels, kb, f);
        int arity = kb.kbCache.valences.get("links").intValue();
        System.out.println("testLinks(): arity of 'links': " + arity);
        System.out.println("testLinks(): rels: " + rels);
        System.out.println("testLinks(): rowVarMaxArities: " + rowVarMaxArities);
        assertEquals(3, rowVarMaxArities.get("@ARGS").intValue());
    }
}