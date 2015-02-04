package com.articulate.sigma;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertFalse;
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

    @Test
    public void testIsSimpleClauseWithFunctionalTerm() {
        Formula f1 = new Formula();
        f1.read("(part (MarialogicalSumFn ?X) ?Y)");

        assertTrue(f1.isSimpleClause());
    }

    @Test
    public void testIsSimpleClause1() {
        Formula f1 = new Formula();
        f1.read("(instance ?X Human)");

        assertTrue(f1.isSimpleClause());
    }

    @Test
    public void testIsSimpleClause2() {
        Formula f1 = new Formula();
        f1.read("(member (SkFn 1 ?X3) ?X3)");

        assertTrue(f1.isSimpleClause());
    }

    @Test
    public void testIsSimpleClause3() {
        Formula f1 = new Formula();
        f1.read("(member ?VAR1 Org1-1)");

        assertTrue(f1.isSimpleClause());
    }

    @Test
    public void testIsSimpleClause4() {
        Formula f1 = new Formula();
        f1.read("(capability (KappaFn ?HEAR (and (instance ?HEAR Hearing) (agent ?HEAR ?HUMAN) " +
                "(destination ?HEAR ?HUMAN) (origin ?HEAR ?OBJ))) agent ?HUMAN)");

        assertTrue(f1.isSimpleClause());
    }

    @Test
    public void testNotSimpleClause1() {
        Formula f1 = new Formula();
        f1.read("(=> (attribute ?Agent Investor) (exists (?Investing) (agent ?Investing ?Agent)))");

        assertFalse(f1.isSimpleClause());
    }

    @Test
    public void testNotSimpleClause2() {
        Formula f1 = new Formula();
        f1.read("(not (instance ?X Human))");

        assertFalse(f1.isSimpleClause());
    }

    @Test
    public void testCollectQuantifiedVariables() {
        List<String> expected = Lists.newArrayList("?T", "?Z");

        Formula f1 = new Formula();
        f1.read("(=> " +
                "  (and " +
                "    (attribute ?H Muslim) " +
                "    (equal " +
                "      (WealthFn ?H) ?W)) " +
                "(modalAttribute " +
                "  (exists (?Z ?T) " +
                "    (and " +
                "      (instance ?Z Zakat) " +
                "      (instance ?Y Year) " +
                "      (during ?Y " +
                "        (WhenFn ?H)) " +
                "      (holdsDuring ?Y " +
                "        (attribute ?H FullyFormed)) " +
                "      (agent ?Z ?H) " +
                "      (patient ?Z ?T) " +
                "      (monetaryValue ?T ?C) " +
                "      (greaterThan ?C " +
                "        (MultiplicationFn ?W 0.025)))) Obligation)) ");

        assertEquals(expected, f1.collectQuantifiedVariables());
    }

    @Test
    public void testCollectAllVariables() {
        List<String> expected = Lists.newArrayList("?C", "?T", "?H", "?W", "?Y", "?Z");

        Formula f1 = new Formula();
        f1.read("(=> " +
                "  (and " +
                "    (attribute ?H Muslim) " +
                "    (equal " +
                "      (WealthFn ?H) ?W)) " +
                "(modalAttribute " +
                "  (exists (?Z ?T) " +
                "    (and " +
                "      (instance ?Z Zakat) " +
                "      (instance ?Y Year) " +
                "      (during ?Y " +
                "        (WhenFn ?H)) " +
                "      (holdsDuring ?Y " +
                "        (attribute ?H FullyFormed)) " +
                "      (agent ?Z ?H) " +
                "      (patient ?Z ?T) " +
                "      (monetaryValue ?T ?C) " +
                "      (greaterThan ?C " +
                "        (MultiplicationFn ?W 0.025)))) Obligation)) ");

        assertEquals(expected, f1.collectAllVariables());
    }

    @Test
    public void testUnquantifiedVariables() {
        List<String> expected = Lists.newArrayList("?C", "?W", "?H", "?Y");

        Formula f1 = new Formula();
        f1.read("(=> " +
                "  (and " +
                "    (attribute ?H Muslim) " +
                "    (equal " +
                "      (WealthFn ?H) ?W)) " +
                "(modalAttribute " +
                "  (exists (?Z ?T) " +
                "    (and " +
                "      (instance ?Z Zakat) " +
                "      (instance ?Y Year) " +
                "      (during ?Y " +
                "        (WhenFn ?H)) " +
                "      (holdsDuring ?Y " +
                "        (attribute ?H FullyFormed)) " +
                "      (agent ?Z ?H) " +
                "      (patient ?Z ?T) " +
                "      (monetaryValue ?T ?C) " +
                "      (greaterThan ?C " +
                "        (MultiplicationFn ?W 0.025)))) Obligation)) ");

        assertEquals(expected, f1.collectUnquantifiedVariables());
    }

    @Test
    public void testTerms() {
        List<String> expected = Lists.newArrayList("holdsDuring", "MultiplicationFn", "WealthFn", "?T", "Muslim", "?W",
                "Obligation", "attribute", "?Y", "equal", "?Z", "agent", "and", "Year", "patient", "=>", "modalAttribute",
                "during", "?C", "monetaryValue", "FullyFormed", "greaterThan", "exists", "?H", "Zakat", "instance",
                "0.025", "WhenFn");

        Formula f1 = new Formula();
        f1.read("(=> " +
                "  (and " +
                "    (attribute ?H Muslim) " +
                "    (equal " +
                "      (WealthFn ?H) ?W)) " +
                "(modalAttribute " +
                "  (exists (?Z ?T) " +
                "    (and " +
                "      (instance ?Z Zakat) " +
                "      (instance ?Y Year) " +
                "      (during ?Y " +
                "        (WhenFn ?H)) " +
                "      (holdsDuring ?Y " +
                "        (attribute ?H FullyFormed)) " +
                "      (agent ?Z ?H) " +
                "      (patient ?Z ?T) " +
                "      (monetaryValue ?T ?C) " +
                "      (greaterThan ?C " +
                "        (MultiplicationFn ?W 0.025)))) Obligation)) ");

        assertEquals(expected, f1.collectTerms());
    }

    @Test
    public void testReplaceVar() {
        Formula expected = new Formula();
        expected.read("(<=> (instance part TransitiveRelation) (forall (?INST1 ?INST2 ?INST3) (=> (and (part ?INST1 ?INST2) (part ?INST2 ?INST3)) (part ?INST1 ?INST3))))");

        Formula f1 = new Formula();
        f1.read("(<=> (instance ?REL TransitiveRelation) (forall (?INST1 ?INST2 ?INST3) " +
                " (=> (and (?REL ?INST1 ?INST2) (?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))");

        assertEquals(expected, f1.replaceVar("?REL", "part"));
    }

    @Test
    public void testComplexVars() {
        List<String> expected = Lists.newArrayList("?Y", "(WhenFn ?H)");

        Formula f1 = new Formula();
        f1.read("(during ?Y (WhenFn ?H))");

        assertEquals(expected, f1.complexArgumentsToArrayList(1));
    }

    @Test
    public void testBigArgs() {
        String expected = "";

        Formula f1 = new Formula();
        f1.read("(=> (instance ?AT AutomobileTransmission) (hasPurpose ?AT (exists (?C ?D ?A ?R1 ?N1 ?R2 ?R3 ?R4 ?N2 ?N3)" +
                " (and (instance ?C Crankshaft) (instance ?D Driveshaft) (instance ?A Automobile) (part ?D ?A) (part ?AT ?A)" +
                " (part ?C ?A) (connectedEngineeringComponents ?C ?AT) (connectedEngineeringComponents ?D ?AT) (instance ?R1 Rotating)" +
                " (instance ?R2 Rotating) (instance ?R3 Rotating) (instance ?R4 Rotating) (patient ?R1 ?C) (patient ?R2 ?C) (patient ?R3 ?D)" +
                " (patient ?R4 ?D) (causes ?R1 ?R3) (causes ?R2 ?R4) (not (equal ?R1 ?R2)) (holdsDuring ?R1 (measure ?C (RotationFn ?N1 MinuteDuration)))" +
                " (holdsDuring ?R2 (measure ?C (RotationFn ?N1 MinuteDuration))) (holdsDuring ?R3 (measure ?D (RotationFn ?N2 MinuteDuration))) (holdsDuring ?R4" +
                " (measure ?D (RotationFn ?N3 MinuteDuration))) (not (equal ?N2 ?N3))))))");

        assertEquals(expected, f1.validArgs());
    }
}