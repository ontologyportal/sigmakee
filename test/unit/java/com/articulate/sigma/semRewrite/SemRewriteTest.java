package com.articulate.sigma.semRewrite;

import com.articulate.sigma.UnitTestBase;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * Tests the SemRewrite of a given parse.
 * In: Dependency parse as string.
 * Out: Suo-Kif.
 */
public class SemRewriteTest extends UnitTestBase {

    /** *************************************************************
     * Mary is at the house.
     * prep_at(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> (located(?X,?Y))
     */
    @Test
    public void testMaryAtHouse() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0, be-2), nsubj(be-2, Mary-1), det(house-5, the-4), prep_at(be-2, house-5), sumo(House, house-5), names(Mary-1, \"Mary\"), attribute(Mary-1, Female), sumo(Human, Mary-1), number(SINGULAR, Mary-1), tense(PRESENT, be-2), number(SINGULAR, house-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(attribute Mary-1 Female)",
                "(located be-2 house-5)",
                "(names Mary-1 \"Mary\")",
                "(instance house-5 House)",
                "(instance Mary-1 Human)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary is in the house.
     * prep_in(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> {(orientation ?X ?Y Inside)}.
     */
    @Test
    public void testMaryInHouse() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0, be-2), nsubj(be-2, Mary-1), det(house-5, the-4), prep_in(be-2, house-5), sumo(House, house-5), names(Mary-1, \"Mary\"), attribute(Mary-1, Female), sumo(Human, Mary-1), number(SINGULAR, Mary-1), tense(PRESENT, be-2), number(SINGULAR, house-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(orientation be-2 house-5 Inside)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(instance house-5 House)",
                "(instance Mary-1 Human)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary is inside the house.
     * prep_inside(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> {(orientation ?X ?Y Inside)}.
     */
    @Test
    public void testMaryInsideHouse() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0, be-2), nsubj(be-2, Mary-1), det(house-5, the-4), prep_inside(be-2, house-5), sumo(House, house-5), names(Mary-1, \"Mary\"), attribute(Mary-1, Female), sumo(Human, Mary-1), number(SINGULAR, Mary-1), tense(PRESENT, be-2), number(SINGULAR, house-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(orientation be-2 house-5 Inside)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(instance house-5 House)",
                "(instance Mary-1 Human)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary is outside the house.
     * prep_outside(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> {(orientation ?X ?Y Outside)}.
     */
    @Test
    public void testMaryOutsideHouse() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0, be-2), nsubj(be-2, Mary-1), det(house-5, the-4), prep_outside(be-2, house-5), sumo(House, house-5), names(Mary-1, \"Mary\"), attribute(Mary-1, Female), sumo(Human, Mary-1), number(SINGULAR, Mary-1), tense(PRESENT, be-2), number(SINGULAR, house-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(orientation be-2 house-5 Outside)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(instance house-5 House)",
                "(instance Mary-1 Human)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary is on the house.
     * prep_on(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> (located(?X,?Y)).
     */
    @Test
    public void testMaryOnHouse() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "det(house-5, the-4), root(ROOT-0, be-2), nsubj(be-2, Mary-1), prep_on(be-2, house-5), sumo(House, house-5), names(Mary-1, \"Mary\"), attribute(Mary-1, Female), sumo(Human, Mary-1), number(SINGULAR, Mary-1), tense(PRESENT, be-2), number(SINGULAR, house-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(located be-2 house-5)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(instance house-5 House)",
                "(instance Mary-1 Human)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary goes to the house.
     * prep_to(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> (destination(?X,?Y)).
     */
    @Test
    public void testMaryGoToHouse() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "det(house-5, the-4), root(ROOT-0, go-2), nsubj(go-2, Mary-1), prep_to(go-2, house-5), sumo(House, house-5), names(Mary-1, \"Mary\"), attribute(Mary-1, Female), sumo(Human, Mary-1), number(SINGULAR, Mary-1), tense(PRESENT, go-2), number(SINGULAR, house-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(destination go-2 house-5)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(instance house-5 House)",
                "(instance Mary-1 Human)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary goes towards the house.
     * prep_towards(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> (destination(?X,?Y)).
     */
    @Test
    public void testMaryGoTowardsHouse() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0, go-2), nsubj(go-2, Mary-1), det(house-5, the-4), prep_toward(go-2, house-5), sumo(House, house-5), names(Mary-1, \"Mary\"), sumo(Transportation, go-2), attribute(Mary-1, Female), sumo(Human, Mary-1), number(SINGULAR, Mary-1), tense(PRESENT, go-2), number(SINGULAR, house-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(destination go-2 house-5)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(instance house-5 House)",
                "(instance Mary-1 Human)",
                "(agent go-2 Mary-1)",
                "(instance go-2 Transportation)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }


    /** *************************************************************
     * Mary goes in an hour.
     * prep_in(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (time(?X,?Y)).
     */
    @Test
    public void testMaryGoInHour() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "det(hour-5, a-4), root(ROOT-0, go-2), nsubj(go-2, Mary-1), prep_in(go-2, hour-5), names(Mary-1, \"Mary\"), sumo(Hour, hour-5), attribute(Mary-1, Female), sumo(Human, Mary-1), number(SINGULAR, Mary-1), tense(PRESENT, go-2), number(SINGULAR, hour-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(time go-2 hour-5)",
                "(instance Mary-1 Human)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary goes on Tuesday.
     * prep_on(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (time(?X,?Y)).
     */
    @Test
    public void testMaryGoOnTuesday() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0,go-2), nsubj(go-2,Mary-1), prep_on(go-2,Tuesday-4), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PRESENT,go-2), number(SINGULAR,Tuesday-4), day(time-1,Tuesday), time(goesOn-2,time-1)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(time goesOn-2 (DayFn Tuesday (MonthFn ?M (YearFn ?Y))))",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(instance Mary-1 Human)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertEquals(expected, cleanedActual);
    }

    /** *************************************************************
     * Mary walks for two hours.
     * prep_for(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Person) ==> (destination(?X,?Y)).
     */
    @Test
    public void testMaryWalkForTwoHours() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0,walk-2), nsubj(walk-2,Mary-1), num(hour-5,two-4), prep_for(walk-2,hour-5), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Hour,hour-5), sumo(Walking,walk-2), number(SINGULAR,Mary-1), tense(PRESENT,walk-2), number(PLURAL,hour-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(agent walk-2 Mary-1)",
                "(attribute Mary-1 Female)",
                "(duration walk-2 hour-5)",
                "(instance hour-5 Collection)",
                "(membersType hour-5 Hour)",
                "(membersCount hour-5 2)",
                "(names Mary-1 \"Mary\")",
                "(instance Mary-1 Human)",
                "(instance walk-2 Walking)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary has walked since Tuesday.
     * prep_since(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (starts(?Y,?X)).
     */
    @Test
    public void testMaryWalkSinceTuesday() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0,walk-3), nsubj(walk-3,Mary-1), aux(walk-3,have-2), prep_since(walk-3,Tuesday-5), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Walking,walk-3), number(SINGULAR,Mary-1), tense(PRESENT,walk-3), aspect(PERFECT,walk-3), number(SINGULAR,Tuesday-5), day(time-1,Tuesday), time(walked-3,time-1)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(time walked-3 (DayFn Tuesday (MonthFn ?M (YearFn ?Y))))",
                "(agent walk-3 Mary-1)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(instance Mary-1 Human)",
                "(instance walk-3 Walking)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertEquals(expected, cleanedActual);
    }

    /** *************************************************************
     * Mary walked through the day.
     * prep_through(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (duration(?X,?Y)).
     */
    @Test
    public void testMaryWalkThroughWinter() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "det(day-5,the-4), root(ROOT-0,walk-2), nsubj(walk-2,Mary-1), prep_through(walk-2,day-5), names(Mary-1,\"Mary\"), sumo(Day,day-5), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PAST,walk-2), number(SINGULAR,day-5)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(attribute Mary-1 Female)",
                "(duration walk-2 day-5)",
                "(names Mary-1 \"Mary\")",
                "(instance Mary-1 Human)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary walked through the house.
     * prep_through(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> (traverses(?X,?Y)).
     */
    @Test
    public void testMaryWalkThroughHouse() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "det(house-5,the-4), root(ROOT-0,walk-2), nsubj(walk-2,Mary-1), prep_through(walk-2,house-5), sumo(House,house-5), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PAST,walk-2), number(SINGULAR,house-5)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(traverses walk-2 house-5)",
                "(instance house-5 House)",
                "(instance Mary-1 Human)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary walks with John.
     * prep_with(?X,?Y), +sumo(Human,?Y) ==> (agent(?X,?Y)).
     */
    @Test
    public void testMaryWalkWithJohn() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0,walk-2), nsubj(walk-2,Mary-1), prep_with(walk-2,John-4), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), attribute(John-4,Male), sumo(Human,Mary-1), sumo(Human,John-4), names(John-4,\"John\"), sumo(Walking,walk-2), number(SINGULAR,Mary-1), tense(PRESENT,walk-2), number(SINGULAR,John-4)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(agent walk-2 John-4)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(instance Mary-1 Human)",
                "(agent walk-2 Mary-1)",
                "(attribute John-4 Male)",
                "(names John-4 \"John\")",
                "(instance John-4 Human)",
                "(instance walk-2 Walking)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary walks with the man.
     * prep_with(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Person) ==> (agent(?X,?Y)).
     */
    @Test
    public void testMaryWalkWithMan() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0,walk-2), nsubj(walk-2,Mary-1), det(man-5,the-4), prep_with(walk-2,man-5), sumo(Man,man-5), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Walking,walk-2), number(SINGULAR,Mary-1), tense(PRESENT,walk-2), number(SINGULAR,man-5)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(agent walk-2 man-5)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(instance man-5 Man)",
                "(agent walk-2 Mary-1)",
                "(instance Mary-1 Human)",
                "(instance walk-2 Walking)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary walks with the artifact.
     * prep_with(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Person) ==> (agent(?X,?Y)).
     */
    @Test
    public void testMaryWalkWithArtifact() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0,walk-2), nsubj(walk-2,Mary-1), det(artifact-5,the-4), prep_with(walk-2,artifact-5), sumo(Artifact,artifact-5), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Walking,walk-2), number(SINGULAR,Mary-1), tense(PRESENT,walk-2), number(SINGULAR,artifact-5)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(agent walk-2 Mary-1)",
                "(attribute Mary-1 Female)",
                "(instrument walk-2 artifact-5)",
                "(names Mary-1 \"Mary\")",
                "(instance artifact-5 Artifact)",
                "(instance Mary-1 Human)",
                "(instance walk-2 Walking)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary went across the room.
     * prep_across(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> (traverses(?X,?Y)).
     */
    @Test
    public void testMaryGoAcrossRoom() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "det(room-5,the-4), root(ROOT-0,go-2), nsubj(go-2,Mary-1), prep_across(go-2,room-5), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Room,room-5), number(SINGULAR,Mary-1), tense(PAST,go-2), number(SINGULAR,room-5)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(traverses go-2 room-5)",
                "(instance Mary-1 Human)",
                "(instance room-5 Room)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary walked within the room.
     * prep_within(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> (properlyFills(?X,?Y)).
     */
    @Test
    public void testMaryWalkWithinRoom() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0,walk-2), nsubj(walk-2,Mary-1), det(room-5,the-4), prep_within(walk-2,room-5), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Walking,walk-2), sumo(Room,room-5), number(SINGULAR,Mary-1), tense(PAST,walk-2), number(SINGULAR,room-5)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(agent walk-2 Mary-1)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(properlyFills walk-2 room-5)",
                "(instance Mary-1 Human)",
                "(instance walk-2 Walking)",
                "(instance room-5 Room)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary walked into the room.
     * prep_into(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> (properlyFills(?X,?Y)).
     */
    @Test
    public void testMaryWalkIntoRoom() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0,walk-2), nsubj(walk-2,Mary-1), det(room-5,the-4), prep_into(walk-2,room-5), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Walking,walk-2), sumo(Room,room-5), number(SINGULAR,Mary-1), tense(PAST,walk-2), number(SINGULAR,room-5)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(agent walk-2 Mary-1)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(properlyFills walk-2 room-5)",
                "(instance Mary-1 Human)",
                "(instance walk-2 Walking)",
                "(instance room-5 Room)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary will walk from the house.
     * prep_from(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> (origin(?X,?Y)).
     */
    @Test
    public void testMaryWillWalkFromHouse() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0,walk-3), nsubj(walk-3,Mary-1), aux(walk-3,will-2), det(house-6,the-5), prep_from(walk-3,house-6), sumo(House,house-6), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Walking,walk-3), number(SINGULAR,Mary-1), tense(FUTURE,walk-3), number(SINGULAR,house-6)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(agent walk-3 Mary-1)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(origin walk-3 house-6)",
                "(earlier Now (WhenFn walk-3))",
                "(instance house-6 House)",
                "(instance Mary-1 Human)",
                "(instance walk-3 Walking)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertEquals(expected, cleanedActual);
    }

    /** *************************************************************
     * Mary was walking from noon.
     * prep_from(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (BeginFn(?X,?Y)).
     * This rule is triggered by this test's input, though the BeginFn does not at this time appear in the output.
     */
    @Test
    public void testMaryWasWalkingFromNoon() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0,walk-3), nsubj(walk-3,Mary-1), aux(walk-3,be-2), prep_from(walk-3,noon-5), names(Mary-1,\"Mary\"), sumo(Walking,walk-3), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(TimePosition,noon-5), number(SINGULAR,Mary-1), tense(PAST,walk-3), aspect(PROGRESSIVE,walk-3), number(SINGULAR,noon-5), time(walking-3,time-1), hour(time-1,12-5), minute(time-1,00-5)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(agent walk-3 Mary-1)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(time walking-3 time-1)",
                "(instance walk-3 Walking)",
                "(instance Mary-1 Human)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertEquals(expected, cleanedActual);
    }

    /** *************************************************************
     * Mary was walking until midnight.
     * prep_until(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (EndFn(?X,?Y)).
     * This rule is triggered by this test's input, though the EndFn does not at this time appear in the output.
     */
    @Test
    public void testMaryWasWalkingUntilMidnight() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0,walk-3), nsubj(walk-3,Mary-1), aux(walk-3,be-2), prep_until(walk-3,midnight-5), names(Mary-1,\"Mary\"), sumo(Walking,walk-3), sumo(TimePoint,midnight-5), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PAST,walk-3), aspect(PROGRESSIVE,walk-3), number(SINGULAR,midnight-5), time(walking-3,time-1), hour(time-1,00-5), minute(time-1,00-5)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(agent walk-3 Mary-1)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(time walking-3 time-1)",
                "(instance walk-3 Walking)",
                "(instance Mary-1 Human)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertEquals(expected, cleanedActual);
    }

    /** *************************************************************
     * Mary was walking from noon until midnight.
     * prep_from(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (BeginFn(?X,?Y)).
     * prep_until(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (EndFn(?X,?Y)).
     * These two rules are triggered by this test's input, though the BeginFn and the EndFn do not at this time appear in the output.
     */
    @Test
    public void testMaryWasWalkingFromNoonUntilMidnight() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0,walk-3), nsubj(walk-3,Mary-1), aux(walk-3,be-2), prep_from(walk-3,noon-5), prep_until(walk-3,midnight-7), sumo(TimePoint,midnight-7), names(Mary-1,\"Mary\"), sumo(Walking,walk-3), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(TimePosition,noon-5), number(SINGULAR,Mary-1), tense(PAST,walk-3), aspect(PROGRESSIVE,walk-3), number(SINGULAR,noon-5), number(SINGULAR,midnight-7), time(walking-3,time-1), hour(time-1,12-5), minute(time-2,00-7), hour(time-2,00-7), time(walking-3,time-2), minute(time-1,00-5)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(agent walk-3 Mary-1)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(time walking-3 time-1)",
                "(time walking-3 time-2)",
                "(instance walk-3 Walking)",
                "(instance Mary-1 Human)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertEquals(expected, cleanedActual);
    }

    /** *************************************************************
     * Mary went after midnight.
     * prep_after(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (greaterThan(?X,?Y)).
     */
    @Test
    public void testMaryWentAfterMidnight() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0,go-2), nsubj(go-2,Mary-1), prep_after(go-2,midnight-4), names(Mary-1,\"Mary\"), sumo(TimePoint,midnight-4), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PAST,go-2), number(SINGULAR,midnight-4), hour(time-1,00-4), time(wentAfter-2,time-1), minute(time-1,00-4)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(attribute Mary-1 Female)",
                "(greaterThan go-2 midnight-4)",
                "(names Mary-1 \"Mary\")",
                "(time wentAfter-2 time-1)",
                "(instance Mary-1 Human)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertEquals(expected, cleanedActual);
    }

    /** *************************************************************
     * Mary went before midnight.
     * prep_before(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (lessThan(?X,?Y)).
     */
    @Test
    public void testMaryWentBeforeMidnight() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0,go-2), nsubj(go-2,Mary-1), prep_before(go-2,midnight-4), sumo(Transportation,go-2), names(Mary-1,\"Mary\"), sumo(TimePoint,midnight-4), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PAST,go-2), number(SINGULAR,midnight-4), hour(time-1,00-4), time(went-2,time-1), minute(time-1,00-4)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(agent go-2 Mary-1)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(lessThan go-2 midnight-4)",
                "(time went-2 time-1)",
                "(instance go-2 Transportation)",
                "(instance Mary-1 Human)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertEquals(expected, cleanedActual);
    }

    /** *************************************************************
     * Mary went along with John.
     * prep_along_with(?X,?Y), +sumo(Human,?Y) ==> (agent(?X,?Y)).
     */
    @Test
    public void testMaryWentAlongWithJohn() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0,go-2), nsubj(go-2,Mary-1), prep_along_with(go-2,John-5), attribute(John-5,Male), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Human,John-5), names(John-5,\"John\"), number(SINGULAR,Mary-1), tense(PAST,go-2), number(SINGULAR,John-5)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(agent go-2 John-5)",
                "(attribute John-5 Male)",
                "(names Mary-1 \"Mary\")",
                "(instance Mary-1 Human)",
                "(attribute Mary-1 Female)",
                "(names John-5 \"John\")",
                "(instance John-5 Human)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary went along with the man.
     * prep_along_with(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Person) ==> (agent(?X,?Y)).
     */
    @Test
    public void testMaryWentAlongWithMan() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "det(man-6,the-5), root(ROOT-0,go-2), nsubj(go-2,Mary-1), prep_along_with(go-2,man-6), sumo(Man,man-6), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PAST,go-2), number(SINGULAR,man-6)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(agent go-2 man-6)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(instance man-6 Man)",
                "(instance Mary-1 Human)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     * Mary has walked close by the house.
     * prep_close_by(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> {(orientation ?X ?Y Near)}.
     */
    @Test
    public void testMaryHasWalkedCloseBy() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0,walk-3), nsubj(walk-3,Mary-1), aux(walk-3,have-2), det(house-7,the-6), prep_close_by(walk-3,house-7), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Walking,walk-3), sumo(House,house-7), number(SINGULAR,Mary-1), tense(PRESENT,walk-3), aspect(PERFECT,walk-3), number(SINGULAR,house-7)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        Set<String> expected = Sets.newHashSet(
                "(orientation walk-3 house-7 Near)",
                "(agent walk-3 Mary-1)",
                "(attribute Mary-1 Female)",
                "(names Mary-1 \"Mary\")",
                "(instance Mary-1 Human)",
                "(instance walk-3 Walking)",
                "(instance house-7 House)"
        );

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        assertEquals(expected, actual);
    }


}
