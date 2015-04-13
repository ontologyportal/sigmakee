package com.articulate.sigma.semRewrite;

import com.articulate.sigma.Formula;
import com.articulate.sigma.UnitTestBase;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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


}
