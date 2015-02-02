package com.articulate.sigma;

import com.google.common.collect.Lists;
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
public class PredVarInstTest extends SigmaTestBase  {
    private static String stmt1 = "(<=> (instance ?REL TransitiveRelation) " +
            "(forall (?INST1 ?INST2 ?INST3) " +
            "(=> (and (?REL ?INST1 ?INST2) " +
            "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";

    private static String stmt2 = "(=> " +
            "(instance ?JURY Jury) " +
            "(holdsRight " +
            "(exists (?DECISION) " +
            "(and " +
            "(instance ?DECISION LegalDecision) " +
            "(agent ?DECISION ?JURY))) ?JURY))";


    @Test
    public void testGatherPredVars1()     {
        Formula f = new Formula();
        f.read(stmt1);

        Set<String> actual = PredVarInst.gatherPredVars(f);

        Set<String> expected = Sets.newHashSet("?REL");
        assertEquals(expected, actual);
    }

    @Test
    public void testGatherPredVars2()     {
        Formula f = new Formula();
        f.read(stmt2);

        Set<String> actual = PredVarInst.gatherPredVars(f);

        Set<String> expected = Sets.newHashSet();
        assertEquals(expected, actual);
    }

    @Test
    public void testFindPredVarTypes()     {
        Formula f = new Formula();
        f.read(stmt1);

        Map<String, HashSet<String>> actual = PredVarInst.findPredVarTypes(f, kb);

        Map<String, HashSet<String>> expected = Maps.newHashMap();
        expected.put("?REL", Sets.newHashSet("Entity"));
        assertEquals(expected, actual);
    }

    @Test
    public void testInstantiatePredVars1()     {
        Formula f = new Formula();
        f.read(stmt1);

        ArrayList<Formula> actual = PredVarInst.instantiatePredVars(f, kb);

        String expectedStr = "(<=>\n" +
                "  (instance lessThanOrEqualTo TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (lessThanOrEqualTo ?INST1 ?INST2)\n" +
                "        (lessThanOrEqualTo ?INST2 ?INST3))\n" +
                "      (lessThanOrEqualTo ?INST1 ?INST3)))), (<=>\n" +
                "  (instance equivalentContentInstance TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (equivalentContentInstance ?INST1 ?INST2)\n" +
                "        (equivalentContentInstance ?INST2 ?INST3))\n" +
                "      (equivalentContentInstance ?INST1 ?INST3)))), (<=>\n" +
                "  (instance subProcess TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (subProcess ?INST1 ?INST2)\n" +
                "        (subProcess ?INST2 ?INST3))\n" +
                "      (subProcess ?INST1 ?INST3)))), (<=>\n" +
                "  (instance abstractPart TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (abstractPart ?INST1 ?INST2)\n" +
                "        (abstractPart ?INST2 ?INST3))\n" +
                "      (abstractPart ?INST1 ?INST3)))), (<=>\n" +
                "  (instance subrelation TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (subrelation ?INST1 ?INST2)\n" +
                "        (subrelation ?INST2 ?INST3))\n" +
                "      (subrelation ?INST1 ?INST3)))), (<=>\n" +
                "  (instance subsumesContentClass TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (subsumesContentClass ?INST1 ?INST2)\n" +
                "        (subsumesContentClass ?INST2 ?INST3))\n" +
                "      (subsumesContentClass ?INST1 ?INST3)))), (<=>\n" +
                "  (instance equal TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (equal ?INST1 ?INST2)\n" +
                "        (equal ?INST2 ?INST3))\n" +
                "      (equal ?INST1 ?INST3)))), (<=>\n" +
                "  (instance beforeOrEqual TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (beforeOrEqual ?INST1 ?INST2)\n" +
                "        (beforeOrEqual ?INST2 ?INST3))\n" +
                "      (beforeOrEqual ?INST1 ?INST3)))), (<=>\n" +
                "  (instance subList TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (subList ?INST1 ?INST2)\n" +
                "        (subList ?INST2 ?INST3))\n" +
                "      (subList ?INST1 ?INST3)))), (<=>\n" +
                "  (instance initialList TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (initialList ?INST1 ?INST2)\n" +
                "        (initialList ?INST2 ?INST3))\n" +
                "      (initialList ?INST1 ?INST3)))), (<=>\n" +
                "  (instance temporalPart TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (temporalPart ?INST1 ?INST2)\n" +
                "        (temporalPart ?INST2 ?INST3))\n" +
                "      (temporalPart ?INST1 ?INST3)))), (<=>\n" +
                "  (instance copy TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (copy ?INST1 ?INST2)\n" +
                "        (copy ?INST2 ?INST3))\n" +
                "      (copy ?INST1 ?INST3)))), (<=>\n" +
                "  (instance subAttribute TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (subAttribute ?INST1 ?INST2)\n" +
                "        (subAttribute ?INST2 ?INST3))\n" +
                "      (subAttribute ?INST1 ?INST3)))), (<=>\n" +
                "  (instance equivalentContentClass TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (equivalentContentClass ?INST1 ?INST2)\n" +
                "        (equivalentContentClass ?INST2 ?INST3))\n" +
                "      (equivalentContentClass ?INST1 ?INST3)))), (<=>\n" +
                "  (instance part TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (part ?INST1 ?INST2)\n" +
                "        (part ?INST2 ?INST3))\n" +
                "      (part ?INST1 ?INST3)))), (<=>\n" +
                "  (instance subOrganization TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (subOrganization ?INST1 ?INST2)\n" +
                "        (subOrganization ?INST2 ?INST3))\n" +
                "      (subOrganization ?INST1 ?INST3)))), (<=>\n" +
                "  (instance greaterThanOrEqualTo TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (greaterThanOrEqualTo ?INST1 ?INST2)\n" +
                "        (greaterThanOrEqualTo ?INST2 ?INST3))\n" +
                "      (greaterThanOrEqualTo ?INST1 ?INST3)))), (<=>\n" +
                "  (instance subsumesContentInstance TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (subsumesContentInstance ?INST1 ?INST2)\n" +
                "        (subsumesContentInstance ?INST2 ?INST3))\n" +
                "      (subsumesContentInstance ?INST1 ?INST3)))), (<=>\n" +
                "  (instance relatedInternalConcept TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (relatedInternalConcept ?INST1 ?INST2)\n" +
                "        (relatedInternalConcept ?INST2 ?INST3))\n" +
                "      (relatedInternalConcept ?INST1 ?INST3)))), (<=>\n" +
                "  (instance identicalListItems TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (identicalListItems ?INST1 ?INST2)\n" +
                "        (identicalListItems ?INST2 ?INST3))\n" +
                "      (identicalListItems ?INST1 ?INST3)))), (<=>\n" +
                "  (instance subCollection TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (subCollection ?INST1 ?INST2)\n" +
                "        (subCollection ?INST2 ?INST3))\n" +
                "      (subCollection ?INST1 ?INST3)))), (<=>\n" +
                "  (instance geometricPart TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (geometricPart ?INST1 ?INST2)\n" +
                "        (geometricPart ?INST2 ?INST3))\n" +
                "      (geometricPart ?INST1 ?INST3)))), (<=>\n" +
                "  (instance cooccur TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (cooccur ?INST1 ?INST2)\n" +
                "        (cooccur ?INST2 ?INST3))\n" +
                "      (cooccur ?INST1 ?INST3)))), (<=>\n" +
                "  (instance subclass TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (subclass ?INST1 ?INST2)\n" +
                "        (subclass ?INST2 ?INST3))\n" +
                "      (subclass ?INST1 ?INST3)))), (<=>\n" +
                "  (instance familyRelation TransitiveRelation)\n" +
                "  (forall (?INST1 ?INST2 ?INST3)\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (familyRelation ?INST1 ?INST2)\n" +
                "        (familyRelation ?INST2 ?INST3))\n" +
                "      (familyRelation ?INST1 ?INST3))))";


        List<String> expectedList = Lists.newArrayList(expectedStr);

        assertEquals(25, actual.size());
        assertEquals(expectedList.toString(), actual.toString());
    }

    @Test
    public void testInstantiatePredVars2()     {
        Formula f = new Formula();
        f.read(stmt2);

        ArrayList<Formula> actual = PredVarInst.instantiatePredVars(f, kb);

        List<String> expected = Lists.newArrayList();
        assertEquals(expected, actual);
    }


}