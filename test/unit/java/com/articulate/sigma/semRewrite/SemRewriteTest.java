package com.articulate.sigma.semRewrite;

import com.articulate.sigma.UnitTestBase;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

/**
 * Tests the SemRewrite of a given parse.
 * In: Dependency parse as string.
 * Out: The right-hand-side of a SemRewrite rule that is triggered by the input.
 */
public class SemRewriteTest extends UnitTestBase {

    private static Interpreter interpreter;

    @Before
    public void setUpInterpreter()  {
        interpreter = new Interpreter();
        interpreter.inference = false;
        interpreter.initialize();
    }

    /****************************************************************
     * Mary has walked close to the house.
     * prep_close_to(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> {(orientation ?X ?Y Near)}.
     */
    @Test
    public void testMaryHasWalkedCloseTo() {
        String input = "root(ROOT-0,walk-3), nsubj(walk-3,Mary-1), aux(walk-3,have-2), det(house-7,the-6), prep_close_to(walk-3,house-7), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Walking,walk-3), sumo(House,house-7), number(SINGULAR,Mary-1), tense(PRESENT,walk-3), aspect(PERFECT,walk-3), number(SINGULAR,house-7)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(orientation walk-3 house-7 Near)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * Mary walked next to the house.
     * prep_next_to(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> {(orientation ?X ?Y Near)}.
     */
    @Test
    public void testMaryWalkedNextTo() {
        String input = "root(ROOT-0,walk-2), nsubj(walk-2,Mary-1), det(house-6,the-5), prep_next_to(walk-2,house-6), sumo(House,house-6), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Walking,walk-2), number(SINGULAR,Mary-1), tense(PAST,walk-2), number(SINGULAR,house-6)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(orientation walk-2 house-6 Near)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * Mary is over the house.
     * prep_over(?V,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> {(orientation ?LOC ?Y Above) (located ?V ?LOC)}.
     */
    @Test
    public void testMaryIsOverHouse() {
        String input = "root(ROOT-0,be-2), nsubj(be-2,Mary-1), det(house-5,the-4), prep_over(be-2,house-5), sumo(House,house-5), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PRESENT,be-2), number(SINGULAR,house-5)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(orientation ?LOC house-5 Above) (located be-2 ?LOC)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * Mary went prior to eating.
     * prep_prior_to(?X,?Y), +sumo(?C,?Y), isSubclass(?C,Process) ==> {(earlier ?X (WhenFn ?Y))}.
     */
    @Test
    public void testMaryGoPriorToEating() {
        String input = "root(ROOT-0,go-2), nsubj(go-2,Mary-1), prep_prior_to(go-2,eating-5), sumo(Transportation,go-2), names(Mary-1,\"Mary\"), sumo(Eating,eating-5), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PAST,go-2), number(SINGULAR,eating-5)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(earlier go-2 (WhenFn eating-5))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * Mary went prior to Christmas Day.
     * prep_prior_to(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> {(earlier ?X ?Y)}.
     */
    @Test
    public void testMaryGoPriorToChristmas() {
        String input = "root(ROOT-0,go-2), nsubj(go-2,Mary-1), prep_prior_to(go-2,ChristmasDay-5), sumo(Transportation,go-2), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Day,ChristmasDay-5), number(SINGULAR,Mary-1), tense(PAST,go-2), number(SINGULAR,Christmas-5), number(SINGULAR,Day-6)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(earlier go-2 ChristmasDay-5)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * Mary walks in front of the house.
     * prep_in_front_of(?V,?Y), nsubj(?V,?Z) ==> {(and (equal ?F (FrontFn ?Y)) (orientation ?Z ?F Near))}.
     */
    @Test
    public void testMaryWalksInFrontOf() {
        String input = "det(house-7,the-6), root(ROOT-0,walk-2), nsubj(walk-2,Mary-1), prep_in_front_of(walk-2,house-7), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(House,house-7), number(SINGULAR,Mary-1), tense(PRESENT,walk-2), number(SINGULAR,front-4), number(SINGULAR,house-7)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(and (equal ?F (FrontFn house-7)) (orientation Mary-1 ?F Near))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * Mary is on top of the house.
     * prep_on_top_of(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Object) ==> {(orientation ?X ?Y On)}.
     */
    @Test
    public void testMaryIsOnTopOf() {
        String input = "det(house-7,the-6), root(ROOT-0,be-2), nsubj(be-2,Mary-1), prep_on_top_of(be-2,house-7), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(House,house-7), number(SINGULAR,Mary-1), tense(PRESENT,be-2), number(SINGULAR,top-4), number(SINGULAR,house-7)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(orientation be-2 house-7 On)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /** *************************************************************
     * Mary has walked since Christmas Day.
     * prep_since(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (starts(?Y,?X)).
     */
    @Test
    public void testMaryWalkSinceChristmasDay() {
        String input = "root(ROOT-0,walk-3), nsubj(walk-3,Mary-1), aux(walk-3,have-2), prep_since(walk-3,ChristmasDay-5), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Day,ChristmasDay-5), sumo(Walking,walk-3), number(SINGULAR,Mary-1), tense(PRESENT,walk-3), aspect(PERFECT,walk-3), number(SINGULAR,Christmas-5), number(SINGULAR,Day-6)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(starts ChristmasDay-5 walk-3)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /** *************************************************************
     * Mary goes on Christmas Day.
     * prep_on(?X,?Y), +sumo(?C,?Y), isCELTclass(?C,Time) ==> (time(?X,?Y)).
     */
    @Test
    public void testMaryGoOnChristmasDay() {
        String input = "root(ROOT-0,go-2), nsubj(go-2,Mary-1), prep_on(go-2,ChristmasDay-4), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Day,ChristmasDay-4), number(SINGULAR,Mary-1), tense(PRESENT,go-2), number(SINGULAR,Christmas-4), number(SINGULAR,Day-5)";

        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(time go-2 ChristmasDay-4)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }


}
