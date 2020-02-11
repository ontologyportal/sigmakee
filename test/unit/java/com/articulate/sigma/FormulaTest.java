package com.articulate.sigma;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class FormulaTest {

    /** ***************************************************************
     */
    @Test
    public void testFormulaRead() {

        String stmt = "(domain date 1 Physical)";
        Formula f = new Formula(stmt);
        assertEquals(stmt, f.getFormula());

        stmt = "(=> (and (instance ?REL ObjectAttitude) (?REL ?AGENT ?THING)) (instance ?THING Physical))";
        f = new Formula();
        f.read(stmt);
        assertEquals(stmt, f.getFormula());

        stmt = "aabbc";
        f = new Formula();
        f.read(stmt);
        assertEquals(stmt, f.getFormula());

    }

    /** ***************************************************************
     */
    @Test
    public void testRecursiveCdrSimple() {

        String stmt = "(exists (?M))";
        Formula f = new Formula(stmt);

        String car = f.car();
        assertEquals("exists", car);
        Formula cdrF = f.cdrAsFormula();
        assertEquals("((?M))", cdrF.getFormula());

        car = cdrF.car();
        assertEquals("(?M)", car);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("()", cdrF.getFormula());

        car = cdrF.car();
        assertEquals("", car);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("()", cdrF.getFormula());

        car = cdrF.car();
        assertEquals("", car);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("()", cdrF.getFormula());
    }

    /** ***************************************************************
     */
    @Test
    public void testRecursiveCdrComplex() {

        System.out.println("============= testRecursiveCdrComplex ==================");
        String stmt = "(time JohnsBirth (MonthFn ?M (YearFn 2000)))";
        Formula f = new Formula(stmt);

        String car = f.car();
        assertEquals("time", car);
        Formula cdrF = f.cdrAsFormula();
        assertEquals("(JohnsBirth (MonthFn ?M (YearFn 2000)))", cdrF.getFormula());

        car = cdrF.car();
        assertEquals("JohnsBirth", car);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("((MonthFn ?M (YearFn 2000)))", cdrF.getFormula());

        String functionStr = cdrF.car();
        assertEquals("(MonthFn ?M (YearFn 2000))", functionStr);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("()", cdrF.getFormula());

        System.out.println("testRecursiveCdrComplex(): functionStr: " + functionStr);
        //assertTrue(Formula.isFunctionalTerm(functionStr));

        f = new Formula();
        f.read(functionStr);

        car = f.car();
        assertEquals("MonthFn", car);
        cdrF = f.cdrAsFormula();
        assertEquals("(?M (YearFn 2000))", cdrF.getFormula());

        car = cdrF.car();
        assertEquals("?M", car);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("((YearFn 2000))", cdrF.getFormula());

        functionStr = cdrF.car();
        assertEquals("(YearFn 2000)", functionStr);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("()", cdrF.getFormula());

        //assertTrue(Formula.isFunctionalTerm(functionStr));

        f = new Formula();
        f.read(functionStr);

        car = f.car();
        assertEquals("YearFn", car);
        cdrF = f.cdrAsFormula();
        assertEquals("(2000)", cdrF.getFormula());

        car = cdrF.car();
        assertEquals("2000", car);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("()", cdrF.getFormula());

        car = cdrF.car();
        assertEquals("", car);
        cdrF = cdrF.cdrAsFormula();
        assertEquals("()", cdrF.getFormula());
    }

    /*
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
*/
    /** ***************************************************************
     */
    @Test
    public void testCollectQuantifiedVariables() {

        HashSet<String> expected = new HashSet<>(Arrays.asList("?T", "?Z"));
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
        System.out.println("testCollectQuantifiedVariables(): f1: " + f1);
        System.out.println("testCollectQuantifiedVariables(): expected: " + expected);
        Set<String> result = f1.collectQuantifiedVariables();
        System.out.println("testCollectQuantifiedVariables(): result: " + result);
        assertEquals(expected, result);
    }

    /** ***************************************************************
     */
    @Test
    public void testCollectAllVariables() {

        Set<String> expected = Sets.newHashSet("?C", "?T", "?H", "?W", "?Y", "?Z");

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

    /** ***************************************************************
     */
    @Test
    public void testUnquantifiedVariables() {

        HashSet<String> expected = new HashSet<>(Arrays.asList("?C", "?W", "?H", "?Y"));
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

    /** ***************************************************************
     */
    @Test
    public void testTerms() {

        Set<String> expected = Sets.newHashSet("holdsDuring", "MultiplicationFn", "WealthFn", "?T", "Muslim", "?W",
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

    /** ***************************************************************
     */
    @Test
    public void testReplaceVar() {

        Formula expected = new Formula();
        expected.read("(<=> (instance part TransitiveRelation) (forall (?INST1 ?INST2 ?INST3) (=> (and (part ?INST1 ?INST2) (part ?INST2 ?INST3)) (part ?INST1 ?INST3))))");

        Formula f1 = new Formula();
        f1.read("(<=> (instance ?REL TransitiveRelation) (forall (?INST1 ?INST2 ?INST3) " +
                " (=> (and (?REL ?INST1 ?INST2) (?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))");

        assertEquals(expected, f1.replaceVar("?REL", "part"));
    }

    /** ***************************************************************
     */
    @Test
    public void testComplexVarsDuringWhenFn1() {

        List<String> expected = Lists.newArrayList("?Y", "(WhenFn ?H)");

        Formula f1 = new Formula();
        f1.read("(during ?Y (WhenFn ?H))");

        assertEquals(expected, f1.complexArgumentsToArrayListString(1));
    }

    /** ***************************************************************
     */
    @Test
    public void testComplexVarsDuringWhenFn2() {

        List<String> expected = Lists.newArrayList("(WhenFn ?H)");

        Formula f1 = new Formula();
        f1.read("(during ?Y (WhenFn ?H))");

        assertEquals(expected, f1.complexArgumentsToArrayListString(2));
    }

    /** ***************************************************************
     */
    @Test
    public void testComplexVarsInstance1() {

        List<String> expected = Lists.newArrayList("?DRIVE", "Driving");

        Formula f1 = new Formula();
        f1.read("(instance ?DRIVE Driving)");

        assertEquals(expected, f1.complexArgumentsToArrayListString(1));
    }

    /** ***************************************************************
     */
    @Test
    public void testComplexVarsInstance2() {

        List<String> expected = Lists.newArrayList("Driving");

        Formula f1 = new Formula();
        f1.read("(instance ?DRIVE Driving)");

        assertEquals(expected, f1.complexArgumentsToArrayListString(2));
    }

    /** ***************************************************************
     */
    @Test
    public void testComplexVarsInstanceGovernmentFn1() {

        List<String> expected = Lists.newArrayList("(GovernmentFn ?Place)", "StateGovernment)");

        Formula f1 = new Formula();
        f1.read("(instance (GovernmentFn ?Place) StateGovernment))");

        assertEquals(expected, f1.complexArgumentsToArrayListString(1));
    }

    /** ***************************************************************
     */
    @Test
    public void testComplexVarsInstanceGovernmentFn2() {

        List<String> expected = Lists.newArrayList("StateGovernment)");

        Formula f1 = new Formula();
        f1.read("(instance (GovernmentFn ?Place) StateGovernment))");

        assertEquals(expected, f1.complexArgumentsToArrayListString(2));
    }

    /** ***************************************************************
     */
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

    /** ***************************************************************
     */
    @Test
    public void testArgumentsToArrayListGivenComplex0() {

        String stmt = "(exists (?D ?H)\n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H)))";

        Formula f = new Formula(stmt);

        ArrayList<String> actual = f.argumentsToArrayListString(0);

        assertEquals(null, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testArgumentsToArrayListGivenComplex1() {

        String stmt = "(exists (?D ?H)\n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H)))";

        Formula f = new Formula(stmt);

        ArrayList<String> actual = f.argumentsToArrayListString(1);

        assertEquals(null, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testArgumentsToArrayListAnd0() {

        String stmt = "(and\n" +
                "(instance ?D Driving)\n" +
                "(instance ?H Human)\n" +
                "(agent ?D ?H)))";

        Formula f = new Formula(stmt);

        ArrayList<String> actual = f.argumentsToArrayListString(0);

        assertEquals(null, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testArgumentsToArrayInstance0() {

        String stmt = "(instance ?D Driving)";

        Formula f = new Formula(stmt);

        ArrayList<String> actual = f.argumentsToArrayListString(0);
        ArrayList<String> expected = Lists.newArrayList("instance", "?D", "Driving");

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testComplexArgumentsToArrayListDriving0() {

        String stmt = "(exists (?D ?H)\n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H)))";

        Formula f = new Formula(stmt);

        ArrayList<String> actual = f.complexArgumentsToArrayListString(0);
        String temp = "(and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H))";
        ArrayList<String> expected = Lists.newArrayList("exists", "(?D ?H)", temp);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testComplexArgumentsToArrayListDriving1() {

        String stmt = "(exists (?D ?H)\n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H)))";

        Formula f = new Formula(stmt);

        ArrayList<String> actual = f.complexArgumentsToArrayListString(1);
        String temp = "(and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H))";
        ArrayList<String> expected = Lists.newArrayList("(?D ?H)", temp);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testComplexArgumentsToArrayListDriving2() {

        String stmt = "(exists (?D ?H)\n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H)))";

        Formula f = new Formula(stmt);

        ArrayList<String> actual = f.complexArgumentsToArrayListString(2);
        String temp = "(and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H))";
        ArrayList<String> expected = Lists.newArrayList(temp);

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testComplexArgumentsToArrayListDriving3() {

        String stmt = "(exists (?D ?H)\n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H)))";

        Formula f = new Formula(stmt);

        ArrayList<String> actual = f.complexArgumentsToArrayListString(3);

        assertEquals(null, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testComplexArgumentsToArrayListAnd0() {

        String stmt = "(and\n" +
                "           (instance ?D Driving)\n" +
                "           (instance ?H Human)\n" +
                "           (agent ?D ?H))";

        Formula f = new Formula(stmt);

        ArrayList<String> actual = f.complexArgumentsToArrayListString(0);
        ArrayList<String> expected = Lists.newArrayList("and", "(instance ?D Driving)", "(instance ?H Human)", "(agent ?D ?H)");

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testComplexArgumentsToArrayListAnd1() {

        String stmt = "(and\n" +
                "           (instance ?D Driving)\n" +
                "           (instance ?H Human)\n" +
                "           (agent ?D ?H))";

        Formula f = new Formula(stmt);

        ArrayList<String> actual = f.complexArgumentsToArrayListString(1);
        ArrayList<String> expected = Lists.newArrayList("(instance ?D Driving)", "(instance ?H Human)", "(agent ?D ?H)");

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testComplexArgumentsToArrayListAnd2() {

        String stmt = "(and\n" +
                "           (instance ?D Driving)\n" +
                "           (instance ?H Human)\n" +
                "           (agent ?D ?H))";

        Formula f = new Formula(stmt);

        ArrayList<String> actual = f.complexArgumentsToArrayListString(2);
        ArrayList<String> expected = Lists.newArrayList("(instance ?H Human)", "(agent ?D ?H)");

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testComplexArgumentsToArrayListAnd3() {

        String stmt = "(and\n" +
                "           (instance ?D Driving)\n" +
                "           (instance ?H Human)\n" +
                "           (agent ?D ?H))";

        Formula f = new Formula(stmt);

        ArrayList<String> actual = f.complexArgumentsToArrayListString(3);
        ArrayList<String> expected = Lists.newArrayList("(agent ?D ?H)");

        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testComplexArgumentsToArrayListAnd4() {

        String stmt = "(and\n" +
                "           (instance ?D Driving)\n" +
                "           (instance ?H Human)\n" +
                "           (agent ?D ?H))";

        Formula f = new Formula(stmt);

        ArrayList<String> actual = f.complexArgumentsToArrayListString(4);

        assertEquals(null, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testComplexArgumentsToArrayListAbsolute() {

        String stmt = "(equal\n" +
                "  (AbsoluteValueFn ?NUMBER1) ?NUMBER2)";
        Formula f = new Formula(stmt);
        String expected = "[(AbsoluteValueFn ?NUMBER1), ?NUMBER2]";
        ArrayList<String> actual = f.complexArgumentsToArrayListString(1);
        System.out.println("testComplexArgumentsToArrayListAbsolute(): actual: " + actual);
        System.out.println("testComplexArgumentsToArrayListAbsolute(): expected: " + expected);
        assertEquals(expected, actual.toString());
    }

    /** ***************************************************************
     */
    @Test
    public void testComplexArgumentsToArrayList2() {

        String stmt = "(termFormat EnglishLanguage WestMakianLanguage \"west makian language\")";
        Formula f = new Formula(stmt);
        String expected = "";
        ArrayList<Formula> l = f.complexArgumentsToArrayList(1);
        System.out.println("testComplexArgumentsToArrayList2(): actual: " + l.size());
        System.out.println("testComplexArgumentsToArrayList2(): expected: " + 3);
        assertEquals(l.size(),3);
    }

    /** ***************************************************************
     */
    @Test
    public void testGetArg() {

        List<String> expected = Lists.newArrayList("during","?Y", "(WhenFn ?H)");
        ArrayList<String> actual = new ArrayList<>();
        Formula f1 = new Formula();
        f1.read("(during ?Y (WhenFn ?H))");
        for (int i = 0; i < 3; i++) {
            String arg = f1.getArgument(i).getFormula();
            System.out.println("testGetArg(): adding: " + arg);
            actual.add(arg);
        }
        System.out.println("testGetArg(): actual: " + actual);
        System.out.println("testGetArg(): expected: " + expected);
        Formula a = f1.getArgument(1);  // test caching of argument list
        String e = "?Y";
        System.out.println("testGetArg(): a: " + a.getFormula());
        System.out.println("testGetArg(): e: " + e);
        assertEquals(expected, actual);
        assertEquals(e, a.toString());
    }

    /** ***************************************************************
     */
    @Test
    public void testGetArg2() {

        Formula expected = null;
        Formula f1 = new Formula();
        f1.read("(during ?Y (WhenFn ?H))");
        Formula actual = f1.getArgument(3);
        System.out.println("testGetArg(): actual: " + actual);
        System.out.println("testGetArg(): expected: " + expected);
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testGetArgString() {

        List<String> expected = Lists.newArrayList("during","?Y", "(WhenFn ?H)");
        ArrayList<String> actual = new ArrayList<>();
        Formula f1 = new Formula();
        f1.read("(during ?Y (WhenFn ?H))");
        for (int i = 0; i < 3; i++) {
            String arg = f1.getStringArgument(i);
            System.out.println("testGetArgString(): adding: " + arg);
            actual.add(arg);
        }
        System.out.println("testGetArgString(): actual: " + actual);
        System.out.println("testGetArgString(): expected: " + expected);
        assertEquals(expected, actual);
        String a = f1.getStringArgument(1); // test caching of argument list
        String e = "?Y";
        System.out.println("testGetArgString(): a: " + a);
        System.out.println("testGetArgString(): e: " + e);
        assertEquals(e, a);
    }

    /** ***************************************************************
     */
    @Test
    public void testGetArgString2() {

        String expected = "";
        Formula f1 = new Formula();
        f1.read("(during ?Y (WhenFn ?H))");
        String actual = f1.getStringArgument(3);
        System.out.println("testGetArg(): actual: " + actual);
        System.out.println("testGetArg(): expected: " + expected);
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testReplaceQuantifierVars() throws Exception {

        String stmt = "(exists (?X)\n" +
                "        (and\n" +
                "                (instance ?X Organism)\n" +
                "        (part Bio18-1 ?X)))";

        String expected = "(exists (Drosophila)\n" +
                "        (and\n" +
                "                (instance Drosophila Organism)\n" +
                "        (part Bio18-1 Drosophila)))";
        Formula f = new Formula(stmt);
        Formula exp = new Formula(expected);

        List<String> vars = new ArrayList<>();
        vars.add("Drosophila");
        Formula actual = f.replaceQuantifierVars(Formula.EQUANT, vars);
        assertTrue(actual.logicallyEquals(exp));

        stmt = "(exists (?JOHN ?KICKS ?CART)\n" +
                "  (and\n" +
                "    (instance ?JOHN Human)\n" +
                "    (instance ?KICKS Kicking)\n" +
                "    (instance ?CART Wagon)\n" +
                "    (patient ?KICKS ?CART)\n" +
                "    (agent ?KICKS ?JOHN)))\n";

        expected = "(exists (Doyle Kick_2 Cart_1)\n" +
                "  (and\n" +
                "    (instance Doyle Human)\n" +
                "    (instance Kick_2 Kicking)\n" +
                "    (instance Cart_1 Wagon)\n" +
                "    (patient Kick_2 Cart_1)\n" +
                "    (agent Kick_2 Doyle)))\n";
        f = new Formula(stmt);
        exp = new Formula(expected);

        vars = new ArrayList<>();
        vars.add("Doyle");
        vars.add("Kick_2");
        vars.add("Cart_1");
        actual = f.replaceQuantifierVars(Formula.EQUANT, vars);
        assertTrue(actual.logicallyEquals(exp));

        stmt = "(exists (?ENTITY)\n" +
                "         (and \n" +
                "           (subclass ?ENTITY Animal) \n" +
                "           (subclass ?ENTITY CognitiveAgent)\n" +
                "           (equal ?ENTITY Human)))";

        expected = "(exists (Ent_1)\n" +
                "         (and \n" +
                "           (subclass Ent_1 Animal) \n" +
                "           (subclass Ent_1 CognitiveAgent)\n" +
                "           (equal Ent_1 Human)))";
        f = new Formula(stmt);
        exp = new Formula(expected);

        vars = new ArrayList<>();
        vars.add("Ent_1");
        actual = f.replaceQuantifierVars(Formula.EQUANT, vars);
        assertTrue(actual.logicallyEquals(exp));

        stmt = "(exists (?ENTITY)\n" +
                "         (and \n" +
                "           (subclass ?ENTITY ?TEST) \n" +
                "           (subclass ?ENTITY CognitiveAgent)\n" +
                "           (equal ?ENTITY Human)))";

        expected = "(exists (Ent_1)\n" +
                "         (and \n" +
                "           (subclass Ent_1 Ent_1) \n" +
                "           (subclass Ent_1 CognitiveAgent)\n" +
                "           (equal Ent_1 Human)))";
        f = new Formula(stmt);
        exp = new Formula(expected);

        vars = new ArrayList<>();
        vars.add("Ent_1");
        actual = f.replaceQuantifierVars(Formula.EQUANT, vars);
        assertFalse(actual.toString() + "\n should not be logically equal to \n" + expected, actual.logicallyEquals(exp));
    }
}