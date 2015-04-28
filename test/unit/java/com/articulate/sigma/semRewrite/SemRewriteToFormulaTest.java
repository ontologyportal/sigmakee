package com.articulate.sigma.semRewrite;

import com.articulate.sigma.Formula;
import com.articulate.sigma.UnitTestBase;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the SemRewrite of a given parse, plus its manipulation into a formula string.
 * In: Dependency parse as string.
 * Out: a formula string.
 * FIXME: Generally we prefer testing SemRewrite rules without relying on the formula string. (See SemRewriteTest.java.) We should use the
 * approach in this test class only when the target rule is being triggered, but the RHS does not appear in the interpretCNF result.
 */
public class SemRewriteToFormulaTest extends UnitTestBase {

    private static Interpreter interpreter;

    @Before
    public void setUpInterpreter() throws IOException {
        interpreter = new Interpreter();
        interpreter.inference = false;
        interpreter.initialize();
    }

    private void doTest(String input, String expectedOutput, boolean isQuestion) {

        interpreter.question = isQuestion;
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);
        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        String actual = interpreter.fromKIFClauses(kifClauses);

        Formula actualFormula = new Formula(actual);

        assertEquals(expectedOutput.replaceAll("\\s+", " ").trim(), actual.replaceAll("\\s+", " ").trim());
        assertTrue(actualFormula.logicallyEquals(expectedOutput));
    }

    /***************************************************************
     * Mary is at the house.
     * prep_at(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> (located(?X,?Y))
     */
    @Test
    public void testMaryAtHouse() {
        String input = "root(ROOT-0, be-2), nsubj(be-2, Mary-1), det(house-5, the-4), prep_at(be-2, house-5), sumo(House, house-5), names(Mary-1, \"Mary\"), attribute(Mary-1, Female), sumo(Human, Mary-1), number(SINGULAR, Mary-1), tense(PRESENT, be-2), number(SINGULAR, house-5)";
        String expected =
                "(exists (?be-2 ?Mary-1 ?house-5) \n" +
                "(and \n" +
                "  (attribute ?Mary-1 Female)\n" +
                "  (located ?be-2 ?house-5)\n" +
                "  (names ?Mary-1 \"Mary\")\n" +
                "  (instance ?house-5 House)\n" +
                "  (instance ?Mary-1 Human))\n" +
                ")";

        doTest(input, expected, false);
    }

    /** *************************************************************
     * Mary is in the house.
     * prep_in(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> {(orientation ?X ?Y Inside)}.
     */
    @Test
    public void testMaryInHouse() {
        String input = "root(ROOT-0, be-2), nsubj(be-2, Mary-1), det(house-5, the-4), prep_in(be-2, house-5), sumo(House, house-5), names(Mary-1, \"Mary\"), attribute(Mary-1, Female), sumo(Human, Mary-1), number(SINGULAR, Mary-1), tense(PRESENT, be-2), number(SINGULAR, house-5)";
        String expected =
                "(exists (?be-2 ?Mary-1 ?house-5) \n" +
                        "(and \n" +
                        "  (orientation ?be-2 ?house-5 Inside)\n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (instance ?house-5 House)\n" +
                        "  (instance ?Mary-1 Human))\n" +
                        ")";

        doTest(input, expected, false);
    }

    /** *************************************************************
     * Mary is inside the house.
     * prep_inside(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> {(orientation ?X ?Y Inside)}.
     */
    @Test
    public void testMaryInsideHouse() {
        String input = "root(ROOT-0, be-2), nsubj(be-2, Mary-1), det(house-5, the-4), prep_inside(be-2, house-5), sumo(House, house-5), names(Mary-1, \"Mary\"), attribute(Mary-1, Female), sumo(Human, Mary-1), number(SINGULAR, Mary-1), tense(PRESENT, be-2), number(SINGULAR, house-5)";
        String expected =
                "(exists (?be-2 ?Mary-1 ?house-5) \n" +
                        "(and \n" +
                        "  (orientation ?be-2 ?house-5 Inside)\n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (instance ?house-5 House)\n" +
                        "  (instance ?Mary-1 Human))\n" +
                        ")";

        doTest(input, expected, false);
    }

    /** *************************************************************
     * Mary is outside the house.
     * prep_outside(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> {(orientation ?X ?Y Outside)}.
     */
    @Test
    public void testMaryOutsideHouse() {
        String input = "root(ROOT-0, be-2), nsubj(be-2, Mary-1), det(house-5, the-4), prep_outside(be-2, house-5), sumo(House, house-5), names(Mary-1, \"Mary\"), attribute(Mary-1, Female), sumo(Human, Mary-1), number(SINGULAR, Mary-1), tense(PRESENT, be-2), number(SINGULAR, house-5)";
        String expected =
                "(exists (?be-2 ?Mary-1 ?house-5) \n" +
                        "(and \n" +
                        "  (orientation ?be-2 ?house-5 Outside)\n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (instance ?house-5 House)\n" +
                        "  (instance ?Mary-1 Human))\n" +
                        ")";

        doTest(input, expected, false);
    }

    /** *************************************************************
     * Mary is on the house.
     * prep_on(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> (located(?X,?Y)).
     */
    @Test
    public void testMaryOnHouse() {
        String input = "det(house-5, the-4), root(ROOT-0, be-2), nsubj(be-2, Mary-1), prep_on(be-2, house-5), sumo(House, house-5), names(Mary-1, \"Mary\"), attribute(Mary-1, Female), sumo(Human, Mary-1), number(SINGULAR, Mary-1), tense(PRESENT, be-2), number(SINGULAR, house-5)";
        String expected =
                "(exists (?be-2 ?Mary-1 ?house-5) \n" +
                        "(and \n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (located ?be-2 ?house-5)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (instance ?house-5 House)\n" +
                        "  (instance ?Mary-1 Human))\n" +
                        ")";

        doTest(input, expected, false);
    }

    /** *************************************************************
     * Mary goes to the house.
     * prep_to(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> (destination(?X,?Y)).
     */
    @Test
    public void testMaryGoToHouse() {
        String input = "det(house-5, the-4), root(ROOT-0, go-2), nsubj(go-2, Mary-1), prep_to(go-2, house-5), sumo(House, house-5), names(Mary-1, \"Mary\"), attribute(Mary-1, Female), sumo(Human, Mary-1), number(SINGULAR, Mary-1), tense(PRESENT, go-2), number(SINGULAR, house-5)";
        String expected =
                "(exists (?Mary-1 ?go-2 ?house-5) \n" +
                        "(and \n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (destination ?go-2 ?house-5)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (instance ?house-5 House)\n" +
                        "  (instance ?Mary-1 Human))\n" +
                        ")";

        doTest(input, expected, false);
    }

    /** *************************************************************
     * Mary goes towards the house.
     * prep_towards(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> (destination(?X,?Y)).
     */
    @Test
    public void testMaryGoTowardsHouse() {
        String input = "root(ROOT-0, go-2), nsubj(go-2, Mary-1), det(house-5, the-4), prep_toward(go-2, house-5), sumo(House, house-5), names(Mary-1, \"Mary\"), sumo(Transportation, go-2), attribute(Mary-1, Female), sumo(Human, Mary-1), number(SINGULAR, Mary-1), tense(PRESENT, go-2), number(SINGULAR, house-5)";
        String expected =
                "(exists (?go-2 ?Mary-1 ?house-5) \n" +
                        "(and \n" +
                        "  (agent ?go-2 ?Mary-1)\n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (destination ?go-2 ?house-5)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (instance ?house-5 House)\n" +
                        "  (instance ?go-2 Transportation)\n" +
                        "  (instance ?Mary-1 Human))\n" +
                        ")";

        doTest(input, expected, false);
    }


    /** *************************************************************
     * Mary goes in an hour.
     * prep_in(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (time(?X,?Y)).
     */
    @Test
    public void testMaryGoInHour() {
        String input = "det(hour-5, a-4), root(ROOT-0, go-2), nsubj(go-2, Mary-1), prep_in(go-2, hour-5), names(Mary-1, \"Mary\"), sumo(Hour, hour-5), attribute(Mary-1, Female), sumo(Human, Mary-1), number(SINGULAR, Mary-1), tense(PRESENT, go-2), number(SINGULAR, hour-5)";
        String expected =
                "(exists (?Mary-1 ?go-2 ?hour-5) \n" +
                        "(and \n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (time ?go-2 ?hour-5)\n" +
                        "  (instance ?Mary-1 Human))\n" +
                        ")";

        doTest(input, expected, false);
    }

    /** *************************************************************
     * Mary walks for two hours.
     * prep_for(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Person) ==> (destination(?X,?Y)).
     */
    @Test
    public void testMaryWalkForTwoHours() {
        String input = "root(ROOT-0,walk-2), nsubj(walk-2,Mary-1), num(hour-5,two-4), prep_for(walk-2,hour-5), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Hour,hour-5), sumo(Walking,walk-2), number(SINGULAR,Mary-1), tense(PRESENT,walk-2), number(PLURAL,hour-5)";
        String expected =
                "(exists (?Mary-1 ?hour-5 ?walk-2) \n" +
                        "(and \n" +
                        "  (agent ?walk-2 ?Mary-1)\n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (duration ?walk-2 ?hour-5)\n" +
                        "  (instance ?hour-5 Collection)\n" +
                        "  (membersType ?hour-5 Hour)\n" +
                        "  (membersCount ?hour-5 2)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (instance ?Mary-1 Human)\n" +
                        "  (instance ?walk-2 Walking))\n" +
                        ")";

        doTest(input, expected, false);
    }

    /** *************************************************************
     * Mary walked through the day.
     * prep_through(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (duration(?X,?Y)).
     */
    @Test
    public void testMaryWalkedThroughDay() {
        String input = "det(day-5,the-4), root(ROOT-0,walk-2), nsubj(walk-2,Mary-1), prep_through(walk-2,day-5), names(Mary-1,\"Mary\"), sumo(Day,day-5), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PAST,walk-2), number(SINGULAR,day-5)";
        String expected =
                "(exists (?Mary-1 ?day-5 ?walk-2) \n" +
                        "(and \n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (duration ?walk-2 ?day-5)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (earlier (WhenFn ?walk-2) Now)\n" +
                        "  (instance ?Mary-1 Human))\n" +
                        ")";

        doTest(input, expected, false);
    }

//    /** *************************************************************
//     * Mary walked through the house.
//     * prep_through(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> (traverses(?X,?Y)).
//     */
//    @Test
//    public void testMaryWalkedThroughHouse() {
//        String input = "det(house-5,the-4), root(ROOT-0,walk-2), nsubj(walk-2,Mary-1), prep_through(walk-2,house-5), sumo(House,house-5), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PAST,walk-2), number(SINGULAR,house-5)";
//        String expected =
//                "(exists (?Mary-1 ?house-5 ?walk-2) \n" +
//                        "(and \n" +
//                        "  (attribute ?Mary-1 Female)\n" +
//                        "  (names ?Mary-1 \"Mary\")\n" +
//                        "  (traverses ?walk-2 ?house-5)\n" +
//                        "  (earlier (WhenFn ?walk-2) Now)\n" +
//                        "  (instance ?house-5 House)\n" +
//                        "  (instance ?Mary-1 Human))\n" +
//                        ")";
//
//        doTest(input, expected, false);
//    }

    /** *************************************************************
     * Mary walks with the artifact.
     * prep_with(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Person) ==> (agent(?X,?Y)).
     */
    @Test
    public void testMaryWalkWithArtifact() {
        String input = "root(ROOT-0,walk-2), nsubj(walk-2,Mary-1), det(artifact-5,the-4), prep_with(walk-2,artifact-5), sumo(Artifact,artifact-5), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Walking,walk-2), number(SINGULAR,Mary-1), tense(PRESENT,walk-2), number(SINGULAR,artifact-5)";
        String expected =
                "(exists (?Mary-1 ?artifact-5 ?walk-2) \n" +
                        "(and \n" +
                        "  (agent ?walk-2 ?Mary-1)\n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (instrument ?walk-2 ?artifact-5)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (instance ?artifact-5 Artifact)\n" +
                        "  (instance ?Mary-1 Human)\n" +
                        "  (instance ?walk-2 Walking))\n" +
                        ")";

        doTest(input, expected, false);
    }

    /** *************************************************************
     * Mary was walking from noon.
     * prep_from(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (BeginFn(?X,?Y)).
     * This rule is triggered by this test's input, though the BeginFn does not at this time appear in the output.
     */
    @Test
    public void testMaryWasWalkingFromNoon() {
        String input = "root(ROOT-0,walk-3), nsubj(walk-3,Mary-1), aux(walk-3,be-2), prep_from(walk-3,noon-5), names(Mary-1,\"Mary\"), sumo(Walking,walk-3), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(TimePosition,noon-5), number(SINGULAR,Mary-1), tense(PAST,walk-3), aspect(PROGRESSIVE,walk-3), number(SINGULAR,noon-5), time(walk-3,time-1), hour(time-1,12-5), minute(time-1,00-5)";
        String expected =
                "(exists (?Mary-1 ?time-1 ?walk-3) \n" +
                        "(and \n" +
                        "  (agent ?walk-3 ?Mary-1)\n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (time ?walk-3 ?time-1)\n" +
                        "  (earlier (WhenFn ?walk-3) Now)\n" +
                        "  (instance ?walk-3 Walking)\n" +
                        "  (instance ?Mary-1 Human))\n" +
                        ")";

        doTest(input, expected, false);
    }

    /** *************************************************************
     * Mary was walking until midnight.
     * prep_until(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (EndFn(?X,?Y)).
     * This rule is triggered by this test's input, though the EndFn does not at this time appear in the output.
     */
    @Test
    public void testMaryWasWalkingUntilMidnight() {
        String input = "root(ROOT-0,walk-3), nsubj(walk-3,Mary-1), aux(walk-3,be-2), prep_until(walk-3,midnight-5), names(Mary-1,\"Mary\"), sumo(Walking,walk-3), sumo(TimePoint,midnight-5), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PAST,walk-3), aspect(PROGRESSIVE,walk-3), number(SINGULAR,midnight-5), time(walk-3,time-1), hour(time-1,00-5), minute(time-1,00-5)";
        String expected =
                "(exists (?Mary-1 ?time-1 ?walk-3) \n" +
                        "(and \n" +
                        "  (agent ?walk-3 ?Mary-1)\n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (time ?walk-3 ?time-1)\n" +
                        "  (earlier (WhenFn ?walk-3) Now)\n" +
                        "  (instance ?walk-3 Walking)\n" +
                        "  (instance ?Mary-1 Human))\n" +
                        ")";

        doTest(input, expected, false);
    }

    /** *************************************************************
     * Mary was walking from noon until midnight.
     * prep_from(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (BeginFn(?X,?Y)).
     * prep_until(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (EndFn(?X,?Y)).
     * These two rules are triggered by this test's input, though the BeginFn and the EndFn do not at this time appear in the output.
     */
    @Test
    public void testMaryWasWalkingFromNoonUntilMidnight() {
        String input = "root(ROOT-0,walk-3), nsubj(walk-3,Mary-1), aux(walk-3,be-2), prep_from(walk-3,noon-5), prep_until(walk-3,midnight-7), sumo(TimePoint,midnight-7), names(Mary-1,\"Mary\"), sumo(Walking,walk-3), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(TimePosition,noon-5), number(SINGULAR,Mary-1), tense(PAST,walk-3), aspect(PROGRESSIVE,walk-3), number(SINGULAR,noon-5), number(SINGULAR,midnight-7), time(walk-3,time-1), time(walk-3,time-2), hour(time-1,12-5), minute(time-2,00-7), hour(time-2,00-7), minute(time-1,00-5)";
        String expected =
                "(exists (?Mary-1 ?time-2 ?time-1 ?walk-3) \n" +
                        "(and \n" +
                        "  (agent ?walk-3 ?Mary-1)\n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (time ?walk-3 ?time-1)\n" +
                        "  (earlier (WhenFn ?walk-3) Now)\n" +
                        "  (instance ?walk-3 Walking)\n" +
                        "  (time ?walk-3 ?time-2)\n" +
                        "  (instance ?Mary-1 Human))\n" +
                        ")";

        doTest(input, expected, false);
    }

    /** *************************************************************
     * Mary went after midnight.
     * prep_after(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (greaterThan(?X,?Y)).
     */
    @Test
    // FIXME: Currently the system outputs the expected string below, though the "go-2" part is wrong.
    public void testMaryWentAfterMidnight() {
        String input = "names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), root(ROOT-0,go_after-2), nsubj(go_after-2,Mary-1), tense(PAST,go_after-2), sumo(TimePoint,midnight-4), number(SINGULAR,midnight-4), prep_after(go_after-2,midnight-4), hour(time-1,00-4), time(go-2,time-1), minute(time-1,00-4)";
        String expected =
                "(exists (?Mary-1 ?go_after-2 ?go-2 ?time-1 ?midnight-4) \n" +
                        "(and \n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (greaterThan ?go_after-2 ?midnight-4)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (time ?go-2 ?time-1)\n" +
                        "  (earlier (WhenFn ?go_after-2) Now)\n" +
                        "  (instance ?Mary-1 Human))\n" +
                        ")";

        doTest(input, expected, false);
    }

    /** *************************************************************
     * Mary went along with John.
     * prep_along_with(?X,?Y), +sumo(Human,?Y) ==> (agent(?X,?Y)).
     */
    @Test
    public void testMaryWentAlongWithJohn() {
        String input = "root(ROOT-0,go-2), nsubj(go-2,Mary-1), prep_along_with(go-2,John-5), attribute(John-5,Male), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Human,John-5), names(John-5,\"John\"), number(SINGULAR,Mary-1), tense(PAST,go-2), number(SINGULAR,John-5)";
        String expected =
                "(exists (?go-2 ?Mary-1 ?John-5) \n" +
                        "(and \n" +
                        "  (agent ?go-2 ?John-5)\n" +
                        "  (attribute ?John-5 Male)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (earlier (WhenFn ?go-2) Now)\n" +
                        "  (instance ?Mary-1 Human)\n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (names ?John-5 \"John\")\n" +
                        "  (instance ?John-5 Human))\n" +
                        ")";

        doTest(input, expected, false);
    }

     /** *************************************************************
     * That is Mary's house.
     * poss(?R,?S), nsubj(?O,?R), cop(?O,?IS), sumo(?C,?R) ==> (sumo(?C,?R), nsubj(?R,?S), cop(?R,?IS), prep_of(?R,?O)).
     */
    @Test
    public void testThatIsMarysHouse() {
        String input = "names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PRESENT,be-2), root(ROOT-0,house-5), poss(house-5,Mary-1), sumo(House,house-5), number(SINGULAR,house-5), nsubj(house-5,a_house-4), cop(house-5,be-2)";
        String expected =
                "(exists (?Mary-1 ?house-5) \n" +
                        "(and \n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (instance ?house-5 House)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (instance ?Mary-1 Human))\n" +
                        ")"
        ;

        doTest(input, expected, false);
    }


    /** *************************************************************
     * Mary has made up the story.
     * nsubj(make_up*,?S), +dobj(make_up*,?V), sumo(?C,?V), isSubclass(?C,Process) ==> (nsubj(?V,?S), sumo(?C,?V)).
     */
    @Test
    public void testMaryHasMadeUpStory() {
        String input = "names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), root(ROOT-0,make_up-3), nsubj(make_up-3,Mary-1), tense(PRESENT,make_up-3), aspect(PERFECT,make_up-3), aux(make_up-3,have-2), sumo(Stating,story-6), number(SINGULAR,story-6), dobj(make_up-3,story-6), det(story-6,the-5)";
        String expected =
                "(exists (?Mary-1 ?story-6 ?make_up-3) \n" +
                        "(and \n" +
                        "  (agent ?story-6 ?Mary-1)\n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (patient ?make_up-3 ?story-6)\n" +
                        "  (earlier\n" +
                        "       (WhenFn ?make_up-3) Now)\n" +
                        "  (instance ?Mary-1 Human)\n" +
                        "  (instance ?story-6 Stating))\n" +
                        ")"
                ;

        doTest(input, expected, false);
    }

    /** *************************************************************
     * Mary will make up the story.
     * nsubj(make_up*,?S), +dobj(make_up*,?V), sumo(?C,?V), isSubclass(?C,Process) ==> (nsubj(?V,?S), sumo(?C,?V)).
     */
    @Test
    public void testMaryWillMakeUpStory() {
        String input = "names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), root(ROOT-0,make_up-3), nsubj(make_up-3,Mary-1), sumo(Attribute,make_up-3), tense(FUTURE,make_up-3), aux(make_up-3,will-2), sumo(Stating,story-6), number(SINGULAR,story-6), dobj(make_up-3,story-6), det(story-6,the-5)";

        String expected =
                "(exists (?Mary-1 ?story-6 ?make_up-3) \n" +
                        "(and \n" +
                        "  (agent ?story-6 ?Mary-1)\n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (patient ?make_up-3 ?story-6)\n" +
                        "  (earlier Now\n" +
                        "       (WhenFn ?make_up-3))\n" +
                        "  (instance ?Mary-1 Human)\n" +
                        "  (instance ?story-6 Stating))\n" +
                        ")"
                ;

        doTest(input, expected, false);
    }

    /** *************************************************************
     * Mary makes up the story.
     * nsubj(make_up*,?S), +dobj(make_up*,?V), sumo(?C,?V), isSubclass(?C,Process) ==> (nsubj(?V,?S), sumo(?C,?V)).
     */
    @Test
    public void testMaryMakesUpStory() {
        String input = "names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), root(ROOT-0,make_up-2), nsubj(make_up-2,Mary-1), tense(PRESENT,make_up-2), sumo(Stating,story-5), number(SINGULAR,story-5), dobj(make_up-2,story-5), det(story-5,the-4)";

        String expected =
                "(exists (?story-5 ?Mary-1 ?make_up-2) \n" +
                        "(and \n" +
                        "  (agent ?story-5 ?Mary-1)\n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (patient ?make_up-2 ?story-5)\n" +
                        "  (instance ?Mary-1 Human)\n" +
                        "  (instance ?story-5 Stating))\n" +
                        ")"
                ;

        doTest(input, expected, false);
    }

    /** *************************************************************
     * Mary made up the story.
     * nsubj(make_up*,?S), +dobj(make_up*,?V), sumo(?C,?V), isSubclass(?C,Process) ==> (nsubj(?V,?S), sumo(?C,?V)).
     */
    @Test
    public void testMaryMadeUpStory() {
        String input = "names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), root(ROOT-0,make_up-2), nsubj(make_up-2,Mary-1), tense(PAST,make_up-2), sumo(Stating,story-5), number(SINGULAR,story-5), dobj(make_up-2,story-5), det(story-5,the-4)";

        String expected =
                "(exists (?story-5 ?Mary-1 ?make_up-2) \n" +
                        "(and \n" +
                        "  (agent ?story-5 ?Mary-1)\n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (patient ?make_up-2 ?story-5)\n" +
                        "  (earlier\n" +
                        "  (WhenFn ?make_up-2) Now)\n" +
                        "  (instance ?Mary-1 Human)\n" +
                        "  (instance ?story-5 Stating))\n" +
                        ")"
                ;

        doTest(input, expected, false);
    }

    /** *************************************************************
     * May Mary walk?
     * FIXME: Output is faulty because we detect "May" as a month.
     * We would like to trigger: aux(?V,may*) ==> (possible(?V,?DUMMY)).
     */
    @Test
    public void testMayMaryWalk() {
        String input = "number(SINGULAR,May_Mary-1), number(SINGULAR,May_Mary-1), root(ROOT-0,walk-3), nsubj(walk-3,May_Mary-1), sumo(Walking,walk-3), time(walk-3,time-1), month(time-1,May)";

        String expected =
                "(exists (?time-1 ?walk-3) \n" +
                        "(and \n" +
                        "  (time ?walk-3 ?time-1)\n" +
                        "  (instance ?walk-3 Walking))\n" +
                        ")"
                ;

        doTest(input, expected, true);
    }

    /** *************************************************************
     * Might Mary walk?
     * FIXME: Output is faulty because we detect "Might Mary" as a single entity.
     * We would like to trigger: aux(?V,might*) ==> (possible(?V,?DUMMY)).
     */
    @Test
    public void testMightMaryWalk() {
        String input = "number(SINGULAR,Might_Mary-1), number(SINGULAR,Might_Mary-1), root(ROOT-0,walk-3), nsubj(walk-3,Might_Mary-1), sumo(Walking,walk-3)";

        String expected =
                "(exists (?walk-3) \n" +
                        "  (instance ?walk-3 Walking))"
                ;

        doTest(input, expected, true);
    }


    /** *************************************************************
     * Must Mary walk?
     * Currently we detect "Must" as a subjective assessment. But that doesn't seem to affect the final output.
     * aux(?V,must*) ==> (necessary(?V,?DUMMY)).
     */
    @Test
    public void testMustMaryWalk() {
        String input = "names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), sumo(SubjectiveAssessmentAttribute,must-1), root(ROOT-0,walk-3), nsubj(walk-3,Mary-1), sumo(Walking,walk-3), aux(walk-3,must-1)";

        String expected =
                "(exists (?Y) \n" +
                        "(forall (?DUMMY) \n" +
                        "(exists (?Mary-1 ?walk-3) \n" +
                        "(and \n" +
                        "  (agent ?walk-3 ?Mary-1)\n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (necessary ?walk-3 ?Y)\n" +
                        "  (instance ?Mary-1 Human)\n" +
                        "  (instance ?walk-3 Walking))\n" +
                        ") \n" +
                        "))"
                ;

        doTest(input, expected, true);
    }


    /** *************************************************************
     * TODO: We trigger neg(?V,?Y) ==> (not(?V,?DUMMY))., but the negative doesn't currently appear in the output.
     */
    @Test
    public void testMaryDoesntBuildHouses() {
        String input = "names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), sumo(IntentionalProcess,do-2), root(ROOT-0,build-4), nsubj(build-4,Mary-1), sumo(Making,build-4), aux(build-4,do-2), neg(build-4,not-3), sumo(House,house-5), number(PLURAL,house-5), dobj(build-4,house-5)";

        String expected =
                "(exists (?Mary-1 ?build-4 ?house-5)\n" +
                        "  (and\n" +
                        "    (agent ?build-4 ?Mary-1)\n" +
                        "    (attribute ?Mary-1 Female)\n" +
                        "    (names ?Mary-1 \"Mary\")\n" +
                        "    (patient ?build-4 ?house-5)\n" +
                        "    (instance ?Mary-1 Human)\n" +
                        "    (instance ?build-4 Making)\n" +
                        "    (instance ?house-5 House)) )"
                ;

        doTest(input, expected, false);
    }

    /** *************************************************************
     * predet(?X,?Y) ==> (det(?X,?Y)).
     */
    @Test
    public void testMaryMadeAllTheHouses() {
        String input = "names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), root(ROOT-0,make-2), nsubj(make-2,Mary-1), tense(PAST,make-2), sumo(House,house-5), number(PLURAL,house-5), dobj(make-2,house-5), predet(house-5,all-3), det(house-5,the-4)";

        String expected =
                "(exists (?make-2 ?Mary-1 ?house-5)\n" +
                        "  (and\n" +
                        "    (attribute ?Mary-1 Female)\n" +
                        "    (names ?Mary-1 \"Mary\")\n" +
                        "    (patient ?make-2 ?house-5)\n" +
                        "    (earlier\n" +
                        "      (WhenFn ?make-2) Now)\n" +
                        "    (instance ?Mary-1 Human)\n" +
                        "    (instance ?house-5 House)) )"
                ;

        doTest(input, expected, false);
    }

    /** *************************************************************
     * TODO: JERRY: We trigger prepc_without(?X,?Y) ==> (not(?Y,?DUMMY))., but the negative doesn't currently appear in the output.
     */
    @Test
    public void testMaryMadeTheHousesWithoutWalking() {
        String input = "names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), root(ROOT-0,make-2), nsubj(make-2,Mary-1), tense(PAST,make-2), sumo(House,house-4), number(SINGULAR,house-4), dobj(make-2,house-4), det(house-4,the-3), sumo(Walking,walk-6), prepc_without(make-2,walk-6)";

        String expected =
                "(exists (?make-2 ?walk-6 ?Mary-1 ?house-4) \n" +
                        "(and \n" +
                        "  (attribute ?Mary-1 Female)\n" +
                        "  (names ?Mary-1 \"Mary\")\n" +
                        "  (patient ?make-2 ?house-4)\n" +
                        "  (earlier\n" +
                        "  (WhenFn ?make-2) Now)\n" +
                        "  (instance ?Mary-1 Human)\n" +
                        "  (instance ?house-4 House)\n" +
                        "  (instance ?walk-6 Walking))\n" +
                        ")"
                ;

        doTest(input, expected, false);
    }


}
