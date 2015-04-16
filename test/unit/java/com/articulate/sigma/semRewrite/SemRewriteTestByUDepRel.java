package com.articulate.sigma.semRewrite;

import com.articulate.sigma.UnitTestBase;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

/*
Copyright 2014-2015 IPsoft

Author: Adam Pease adam.pease@ipsoft.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA
 */
public class SemRewriteTestByUDepRel extends UnitTestBase {

    /**
     * Tests the SemRewrite of a given parse.
     * In: Dependency parse as string.
     * Out: The right-hand-side of a SemRewrite rule that is triggered by the input.
     */
    private static Interpreter interpreter;

    @Before
    public void setUpInterpreter()  {
        interpreter = new Interpreter();
        interpreter.inference = false;
        interpreter.initialize();
    }

    /****************************************************************
     * John has a red car.
     * amod(?X,?Y), sumo(?C,?Y) ==> (attribute(?X,?C)).
     */
    @Test
    public void testAMod() {

        String input = "root(ROOT-0,have-2), nsubj(have-2,John-1), det(car-5,a-3), amod(car-5,red-4), dobj(have-2,car-5), names(John-1,\"John\"), sumo(Automobile,car-5), attribute(John-1,Male), sumo(Human,John-1), sumo(Red,red-4), number(SINGULAR,John-1), tense(PRESENT,have-2), number(SINGULAR,car-5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(attribute car-5 Red)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * London smells sweet.
     * acomp(?V,?A), sumo(?AC,?A), nsubj(?V,?O), sumo(?C,?O), isInstanceOf(?C,Object) ==> (attribute(?O,?AC)).
     */
    // Since only using Merge.kif we don't have London, or any other instance, available
    @Ignore
    @Test
    public void testAcomp1() {

        String input = "sumo(LondonUnitedKingdom,London-1), number(SINGULAR,London-1), root(ROOT-0,smell-2), nsubj(smell-2,London-1), sumo(Smelling,smell-2), tense(PRESENT,smell-2), acomp(smells-2,sweet-3), sumo(Sweetness,sweet-3)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(attribute London-1 Sweetness)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * The meat smells sweet.
     * acomp(?V,?A), sumo(?AC,?A), nsubj(?V,?O), sumo(?C,?O),  isSubclass(?C,Object) ==> (attribute(?O,?AC)).
     */
    @Test
    public void testAcomp2() {

        String input = "det(meat-2,the-1), det(meat-2,The-1), sumo(Meat,meat-2), number(SINGULAR,meat-2), root(ROOT-0,smell-3), nsubj(smell-3,meat-2), acomp(smell-3,sweet-4), sumo(Smelling,smell-3), tense(PRESENT,smell-3), acomp(smells-3,sweet-4), sumo(Sweetness,sweet-4)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(attribute meat-2 Sweetness)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * Mary, my sister, arrived.
     * appos(?X,?Y) ==> (equals(?X,?Y)).
     */
    @Test
    public void testAppos() {

        String input = "sumo(Woman,Mary-1), number(SINGULAR,London-1), tense(PRESENT,smells-2), number(SINGULAR,Sam-1), number(SINGULAR,brother-3), tense(PAST,arrived-4), number(SINGULAR,Mary-1), appos(Mary-1,sister-4), poss(sister-4,my-2), sumo(sister,sister-4), number(SINGULAR,sister-4), root(ROOT-0,arrive-6), nsubj(arrive-6,Mary-1), sumo(Arriving,arrive-6), tense(PAST,arrive-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(equals Mary-1 sister-4)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    // need test for vmod(?S,?V), sumo(?C,?S), isSubclass(?C,Human) ==> (agent(?V,?S)).

    /****************************************************************
     * Yesterday, Mary ran.
     * tmod(?V,?T) ==> (during(?V,?T)).
     */
    @Test
    public void testTmod() {

        String input = "names(Mary-3,\"Mary\"), attribute(Mary-3,Female), sumo(Human,Mary-3), number(SINGULAR,Mary-3), tmod(ran-4,yesterday-1), sumo(Day,yesterday-1), number(SINGULAR,yesterday-1), root(ROOT-0,run-4), tmod(run-4,Yesterday-1), nsubj(run-4,Mary-3), sumo(Running,run-4), tense(PAST,run-4)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(during run-4 Yesterday-1)"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }
}
