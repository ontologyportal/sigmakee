package com.articulate.sigma;

//This software is released under the GNU Public License
//<http://www.gnu.org/copyleft/gpl.html>.
// Copyright 2019 Infosys, 2020- Articulate Software
// apease@articulatesoftware.com

import com.google.common.collect.Sets;

import java.util.*;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

/**
 * These tests follow PredVarInst.test(), with the exception of that method's call to FormulaPreprocessor.
 * findExplicitTypesInAntecedent( ), which has been put into the FormulaPreprocessorTest class.
 * TODO: See how relevant the line "if (kb.kbCache.transInstOf("exhaustiveAttribute","VariableArityRelation"))"
 * at the start of the original PredVarInst.test( ) method is. Should these tests somehow reflect that?
 */
public class PredVarInstTest extends UnitTestBase  {

    private static final String AXIOM_1 = "(<=> (instance ?REL TransitiveRelation) " +
            "(forall (?INST1 ?INST2 ?INST3) " +
            "(=> (and (?REL ?INST1 ?INST2) " +
            "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";

    private static final String AXIOM_2 = "(=> " +
            "(instance ?JURY Jury) " +
            "(holdsRight " +
            "(exists (?DECISION) " +
            "(and " +
            "(instance ?DECISION LegalDecision) " +
            "(agent ?DECISION ?JURY))) ?JURY))";

    private static final String AXIOM_3 = "(=> (instance ?R TransitiveRelation) (=> (and (?R ?A ?B) (?R ?B ?C)) (?R ?A ?C)))";

    /** ***************************************************************
     */
    @Test
    public void testGatherPredVarsStmt1() {

        Formula f = new Formula();
        f.read(PredVarInstTest.AXIOM_1);
        Set<String> actual = PredVarInst.gatherPredVars(kb,f);
        Set<String> expected = Sets.newHashSet("?REL");
        System.out.println("\n--------------------");
        System.out.println("testGatherPredVarsStmt1() actual: " + actual);
        System.out.println("testGatherPredVarsStmt1() expected: " + expected);
        if (expected.equals(actual))
            System.out.println("testGatherPredVarsStmt1(): success!");
        else
            System.err.println("testGatherPredVarsStmt1(): failure");
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testGatherPredVarsStmt2() {

        Formula f = new Formula();
        f.read(PredVarInstTest.AXIOM_2);
        Set<String> actual = PredVarInst.gatherPredVars(kb,f);
        Set<String> expected = Sets.newHashSet();
        System.out.println("\n--------------------");
        System.out.println("testGatherPredVarsStmt2() actual: " + actual);
        System.out.println("testGatherPredVarsStmt2() expected: " + expected);
        if (expected.equals(actual))
            System.out.println("testGatherPredVarsStmt2(): success!");
        else
            System.err.println("testGatherPredVarsStmt2(): failure");
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testGatherPredVarsStmt3() {

        Formula f = new Formula();
        f.read(PredVarInstTest.AXIOM_3);
        Set<String> actual = PredVarInst.gatherPredVars(kb,f);
        Set<String> expected = Sets.newHashSet("?R");
        System.out.println("\n--------------------");
        System.out.println("testGatherPredVarsStmt3() actual: " + actual);
        System.out.println("testGatherPredVarsStmt3() expected: " + expected);
        if (expected.equals(actual))
            System.out.println("testGatherPredVarsStmt3(): success!");
        else
            System.err.println("testGatherPredVarsStmt3(): failure");
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testInstantiatePredStmt2() {

        Formula f = new Formula();
        f.read(PredVarInstTest.AXIOM_2);
        Set<Formula> actual = PredVarInst.instantiatePredVars(f, kb);
        Set<Formula> expected = Sets.newHashSet();
        System.out.println("\n--------------------");
        System.out.println("testInstantiatePredStmt2() actual: " + actual);
        System.out.println("testInstantiatePredStmt2() expected: " + expected);
        if (expected.equals(actual))
            System.out.println("testInstantiatePredStmt2(): success!");
        else
            System.err.println("testInstantiatePredStmt2(): failure");
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testInstantiatePredStmt3() {

        String stmt = "(=> " +
                "(and " +
                  "(minValue ?R ?ARG ?N) " +
                  "(?R @ARGS) " +
                  "(equal ?VAL (ListOrderFn (ListFn @ARGS) ?ARG))) " +
                "(greaterThan ?VAL ?N))";
        Formula f = new Formula();
        f.read(stmt);

        System.out.println("\n--------------------");
        Set<Formula> actual = PredVarInst.instantiatePredVars(f, kb);
        Set<Formula> expected = Sets.newHashSet();
        System.out.println("testInstantiatePredStmt3() actual: " + actual);
        System.out.println("testInstantiatePredStmt3() expected: " + expected);
        if (actual.size() > 100)
            System.out.println("testInstantiatePredStmt3(): success!");
        else
            System.err.println("testInstantiatePredStmt3(): failure");
        assertTrue(actual.size() > 100);
    }

    /** ***************************************************************
     */
    @Test
    public void testPredVarArity() {

        String stmt = "(=> (and (instance ?REL CaseRole) (instance ?OBJ Object) " +
                "(?REL ?PROCESS ?OBJ)) (exists (?TIME) (overlapsSpatially (WhereFn ?PROCESS ?TIME) ?OBJ)))";
        Formula f = new Formula();
        f.read(stmt);
        System.out.println("\n--------------------");
        Set<String>  actual = PredVarInst.gatherPredVarRecurse(kb,f);

        Set<String> expected = new HashSet<>();
        expected.add("?REL");
        System.out.println("testPredVarArity() actual: " + actual);
        System.out.println("testPredVarArity() expected: " + expected);
        if (expected.equals(actual))
            System.out.println("testPredVarArity(): success!");
        else
            System.out.println("testPredVarArity(): failure");
        assertEquals(expected, actual);

        System.out.println("PredVarInstTest.testPredVarArity(): actual arity: " + PredVarInst.predVarArity.get("?REL"));
        System.out.println("PredVarInstTest.testPredVarArity(): expected arity: " + 2);
        if (PredVarInst.predVarArity.get("?REL") == 2)
            System.out.println("testPredVarArity(): success!");
        else
            System.err.println("testPredVarArity(): failure");
        assertEquals(2,PredVarInst.predVarArity.get("?REL").intValue());
    }

    /** ***************************************************************
     */
    @Test
    public void testPredVarArity2() {

        String stmt = "(=>\n" +
                "  (and\n" +
                "    (instance ?REL CaseRole)\n" +
                "    (instance ?OBJ Object)\n" +
                "    (?REL ?PROCESS ?OBJ))\n" +
                "  (exists (?TIME)\n" +
                "    (overlapsSpatially\n" +
                "      (WhereFn ?PROCESS ?TIME) ?OBJ)))";
        Formula f = new Formula();
        f.read(stmt);
        System.out.println("\n--------------------");
        String var = "?REL";
        System.out.println("PredVarInstTest.testPredVarArity2(): formula: " + f);
        Set<String> actual = PredVarInst.gatherPredVarRecurse(kb,f);
        System.out.println("PredVarInstTest.testPredVarArity2(): actual pred vars: " + actual);
        int arity = PredVarInst.predVarArity.get(var);
        int expectedArity = 2;
        System.out.println("PredVarInstTest.testPredVarArity2(): actual arity: " + arity);
        System.out.println("PredVarInstTest.testPredVarArity2(): expectedArity: " + expectedArity);
        Set<String> expected = new HashSet<>();
        expected.add(var);
        System.out.println("PredVarInstTest.testPredVarArity2(): expected pred vars: " + expected);
        assertEquals(expected, actual);
        assertEquals(expectedArity,arity);
    }

    /** ***************************************************************
     */
    @Test
    public void testTVRPredVars() {

        String stmt = "(<=>\n" +
                "    (and\n" +
                "        (instance ?REL TotalValuedRelation)\n" +
                "        (instance ?REL Predicate))\n" +
                "    (exists (?VALENCE)\n" +
                "        (and\n" +
                "            (instance ?REL Relation)\n" +
                "            (valence ?REL ?VALENCE)\n" +
                "            (=>\n" +
                "                (forall (?NUMBER ?ELEMENT ?CLASS)\n" +
                "                    (=>\n" +
                "                        (and\n" +
                "                            (lessThan ?NUMBER ?VALENCE)\n" +
                "                            (domain ?REL ?NUMBER ?CLASS)\n" +
                "                            (equal ?ELEMENT\n" +
                "                                (ListOrderFn\n" +
                "                                    (ListFn @ROW) ?NUMBER)))\n" +
                "                        (instance ?ELEMENT ?CLASS)))\n" +
                "                (exists (?ITEM)\n" +
                "                    (?REL @ROW ?ITEM))))))";
        Formula f = new Formula();
        f.read(stmt);
        System.out.println("\n--------------------");
        System.out.println("PredVarInstTest.testTVRPredVars(): formula: " + f);
        Set<String> actual = PredVarInst.gatherPredVars(kb, f);
        System.out.println("PredVarInstTest.testTVRPredVars(): actual: " + actual);
        Set<String> expected = new HashSet<>();
        expected.add("?REL");
        System.out.println("PredVarInstTest.testTVRPredVars(): expected: " + expected);
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testTVRArity() {

        String stmt = "(<=>\n" +
                "    (and\n" +
                "        (instance ?REL TotalValuedRelation)\n" +
                "        (instance ?REL Predicate))\n" +
                "    (exists (?VALENCE)\n" +
                "        (and\n" +
                "            (instance ?REL Relation)\n" +
                "            (valence ?REL ?VALENCE)\n" +
                "            (=>\n" +
                "                (forall (?NUMBER ?ELEMENT ?CLASS)\n" +
                "                    (=>\n" +
                "                        (and\n" +
                "                            (lessThan ?NUMBER ?VALENCE)\n" +
                "                            (domain ?REL ?NUMBER ?CLASS)\n" +
                "                            (equal ?ELEMENT\n" +
                "                                (ListOrderFn\n" +
                "                                    (ListFn @ROW) ?NUMBER)))\n" +
                "                        (instance ?ELEMENT ?CLASS)))\n" +
                "                (exists (?ITEM)\n" +
                "                    (?REL @ROW ?ITEM))))))";
        Formula f = new Formula();
        f.read(stmt);
        System.out.println("\n--------------------");
        String var = "?REL";
        System.out.println("PredVarInstTest.testTVRArity(): formula: " + f);
        System.out.println("PredVarInstTest.testTVRArity(): variable: " + var);
        Set<String> actual = PredVarInst.gatherPredVars(kb, f);
        int arity = PredVarInst.predVarArity.get(var);
        int expected = 0; // variable arity is given as "0"
        System.out.println("PredVarInstTest.testTVRArity(): actual arity: " + arity);
        System.out.println("PredVarInstTest.testTVRArity(): expected arity: " + expected);
        assertEquals(expected, arity);
    }

    /** ***************************************************************
     */
    @Test
    public void testTVRTypes() {

        String stmt = "(<=>\n" +
                "    (and\n" +
                "        (instance ?REL TotalValuedRelation)\n" +
                "        (instance ?REL Predicate))\n" +
                "    (exists (?VALENCE)\n" +
                "        (and\n" +
                "            (instance ?REL Relation)\n" +
                "            (valence ?REL ?VALENCE)\n" +
                "            (=>\n" +
                "                (forall (?NUMBER ?ELEMENT ?CLASS)\n" +
                "                    (=>\n" +
                "                        (and\n" +
                "                            (lessThan ?NUMBER ?VALENCE)\n" +
                "                            (domain ?REL ?NUMBER ?CLASS)\n" +
                "                            (equal ?ELEMENT\n" +
                "                                (ListOrderFn\n" +
                "                                    (ListFn @ROW) ?NUMBER)))\n" +
                "                        (instance ?ELEMENT ?CLASS)))\n" +
                "                (exists (?ITEM)\n" +
                "                    (?REL @ROW ?ITEM))))))";
        Formula f = new Formula();
        f.read(stmt);
        System.out.println("\n--------------------");
        System.out.println("PredVarInstTest.testTVRTypes(): formula: " + f);
        Map<String,Set<String>> varTypes = PredVarInst.findPredVarTypes(f,kb);
        System.out.println("PredVarInstTest.testTVRTypes(): types from domains: " + varTypes);
        varTypes = PredVarInst.addExplicitTypes(kb,f,varTypes);
        System.out.println("PredVarInstTest.testTVRTypes(): with explicit types: " + varTypes);
        Set<String> types = varTypes.get("?REL");
        System.out.println("PredVarInstTest.testTVRTypes(): types: " + types);
        System.out.println("PredVarInstTest.testTVRTypes(): expected: TotalValuedRelation and Predicate");
        if (types.contains("TotalValuedRelation") && types.contains("Predicate"))
            System.out.println("PredVarInstTest.testTVRTypes(): pass");
        else
            System.err.println("PredVarInstTest.testTVRTypes(): fail");
        assertTrue(types.contains("TotalValuedRelation"));
        assertTrue(types.contains("Predicate"));
    }


    /** ***************************************************************
     */
    @Test
    public void testPredVarCount() {

        String stmt = PredVarInst.DOUBLE_PREDICATE_AXIOM;
        Formula f = new Formula();
        f.read(stmt);
        System.out.println("\n--------------------");
        System.out.println("PredVarInstTest.testPredVarCount(): formula: " + f);
        Set<String> predVars = PredVarInst.gatherPredVars(kb,f);
        System.out.println("PredVarInstTest.testPredVarCount(): predVars: " + predVars);
        System.out.println("PredVarInstTest.testPredVarCount(): expected: ?REL1 ?REL2");
        if (predVars.contains("?REL1") && predVars.contains("?REL2") && predVars.size() == 2)
            System.out.println("PredVarInstTest.testPredVarCount(): pass");
        else
            System.err.println("PredVarInstTest.testPredVarCount(): fail");
        assertTrue(predVars.contains("?REL1") && predVars.contains("?REL2") && predVars.size() == 2);
    }

    /** ***************************************************************
     * TODO: Experimental
     */
    // @Ignore
    // @Test
    // public void testReportDisjointErrors() {

    //     KBmanager.getMgr().setPref("cacheDisjoint","true"); // ensure disjoint maps are built

    //     System.out.printf("%n%s%n", "===================== PredVarInstTest.testReportDisjointErrors =====================");
    //     List<Formula> errors = new ArrayList<>(), errorsAll = new ArrayList<>();;
    //     String[] classes;
    //     Set<String> args1 = null, args2 = null;
    //     String rel1, rel2, car;
    //     Map<String, Set<String>> varCoccurrences;
    //     StringBuilder sb = new StringBuilder();
    //     for (String s : kb.kbCache.disjointRelations) {
    //         classes = s.split("\t");
    //         rel1 = classes[0];
    //         rel2 = classes[1];
    //         for (Formula form : kb.formulaMap.values()) {
    //             if (form.isRule() && form.getFormula().contains(rel1) && form.getFormula().contains(rel2)) {
    //                 // varCoccurrences = Diagnostics.extractVariables(form, kb);
    //                 for (String key : varCoccurrences.keySet()) {
    //                     sb.append(key);
    //                     car = sb.substring(1, sb.indexOf(" ")); // the car w/o leading '('
    //                     sb.setLength(0); // reset
    //                     if (car.equals(rel1))
    //                         args1 = varCoccurrences.get(key);
    //                     if (car.equals(rel2))
    //                         args2 = varCoccurrences.get(key);
    //                     if (args1 != null && !args1.isEmpty() && args2 != null && !args2.isEmpty() && args1.containsAll(args2)) {
    //                         System.out.printf("%nrel1: %s : rel2: %s%n", rel1, rel2);
    //                         System.out.printf("varCoccurrences: %s%n", varCoccurrences);

    //                         errors.add(form);
    //                         System.out.printf("%n%s%n", "Disjoint Error set contents:");
    //                         for (Formula f1 : errors)
    //                             System.out.printf("%n%s%n", f1);

    //                         errorsAll.addAll(errors);
    //                         errors.clear();
    //                         args1.clear();
    //                         args2.clear();
    //                     }
    //                 }
    //             }
    //         }
    //     }
    //     System.out.printf("%nDisjoint Error set size: %d%n", errorsAll.size());
    //     assertTrue(errorsAll.isEmpty());
    // }

    /** ***************************************************************
     */
    @Test
    public void testArity() {

        String stmt = "(termFormat EnglishLanguage WestMakianLanguage \"west makian language\")";
        Formula f = new Formula();
        f.read(stmt);
        System.out.println("\n--------------------");
        System.out.println("PredVarInstTest.testArity(): formula: " + f);
        String hasCorrectArity = PredVarInst.hasCorrectArity(f,kb);
        if (hasCorrectArity == null)
            System.out.println("PredVarInstTest.testPredVarCount(): pass");
        else
            System.err.println("PredVarInstTest.testPredVarCount(): fail");
        assertTrue(hasCorrectArity == null);
    }
}