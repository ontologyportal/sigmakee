package com.articulate.sigma;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * These tests follow PredVarInst.test( ), with the exception of that method's call to FormulaPreprocessor.
 * findExplicitTypesInAntecedent( ), which has been put into the FormulaPreprocessorTest class.
 * TODO: See how relevant the line "if (kb.kbCache.transInstOf("exhaustiveAttribute","VariableArityRelation"))"
 * at the start of the original PredVarInst.test( ) method is. Should these tests somehow reflect that?
 */
public class PredVarInstTest extends UnitTestBase  {
    private static final String stmt1 = "(<=> (instance ?REL TransitiveRelation) " +
            "(forall (?INST1 ?INST2 ?INST3) " +
            "(=> (and (?REL ?INST1 ?INST2) " +
            "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";

    private static final String stmt2 = "(=> " +
            "(instance ?JURY Jury) " +
            "(holdsRight " +
            "(exists (?DECISION) " +
            "(and " +
            "(instance ?DECISION LegalDecision) " +
            "(agent ?DECISION ?JURY))) ?JURY))";


    @Test
    public void testGatherPredVarsStmt1()     {
        Formula f = new Formula();
        f.read(PredVarInstTest.stmt1);

        Set<String> actual = PredVarInst.gatherPredVars(SigmaTestBase.kb,f);

        Set<String> expected = Sets.newHashSet("?REL");
        assertEquals(expected, actual);
    }

    @Test
    public void testGatherPredVarsStmt2()     {
        Formula f = new Formula();
        f.read(PredVarInstTest.stmt2);
        Set<String> actual = PredVarInst.gatherPredVars(SigmaTestBase.kb,f);

        Set<String> expected = Sets.newHashSet();
        assertEquals(expected, actual);
    }

    @Test
    public void testFindPredVarTypesStmt1()     {
        Formula f = new Formula();
        f.read(PredVarInstTest.stmt1);

        Map<String, HashSet<String>> actual = PredVarInst.findPredVarTypes(f, SigmaTestBase.kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?REL", Sets.newHashSet("Entity"));
        assertEquals(expected, actual);
    }

    @Test
    public void testInstantiatePredStmt2()     {
        Formula f = new Formula();
        f.read(PredVarInstTest.stmt2);

        Set<Formula> actual = PredVarInst.instantiatePredVars(f, SigmaTestBase.kb);

        Set<Formula> expected = Sets.newHashSet();
        assertEquals(expected, actual);
    }
}