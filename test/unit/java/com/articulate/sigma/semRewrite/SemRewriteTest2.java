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

/** ***********************************************************************************
 * Created by apease on 4/14/15.
 */
public class SemRewriteTest2  extends UnitTestBase {

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
     * Mary was born July 5, 1980. - Just testing the date
     * day(?T,?D), month(?T,?M), year(?T,?Y), time(?V,?T) ==> {(time ?V (DayFn ?D (MonthFn ?M (YearFn ?Y))))}.
     */
    @Test
    public void testMaryBornJuly51980() {

        String input = "num(July-5,5-6), num(July-5,1980-8), root(ROOT-0,be-2), nsubjpass(be-2,Mary-1), prep_on(be-2,July-5), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PAST,be-2), number(SINGULAR,July-5), month(time-1,July), year(time-1,1980), day(time-1,5), time(bear-3,time-1)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(time bear-3 (DayFn 5 (MonthFn July (YearFn 1980))))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * Mary was born July, 1980. - Just testing the date
     * month(?T,?M), year(?T,?Y), time(?V,?T) ==> {(time ?V (MonthFn ?M (YearFn ?Y)))}.
     */
    @Test
    public void testMaryBornJuly1980() {

        String input = "amod(July-4,1980-6), root(ROOT-0,be-2), nsubjpass(be-2,Mary-1), dobj(be-2,July-4), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PAST,be-2), number(SINGULAR,July-4), month(time-1,July), year(time-1,1980), time(bear-3,time-1)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(time bear-3 (MonthFn July (YearFn 1980)))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * Mary was born in 1980. - Just testing the date
     * year(?T,?Y), time(?V,?T) ==> {(time ?V (YearFn ?Y))}.
     */
    @Test
    public void testMaryBornIn1980() {

        String input = "root(ROOT-0,be-2), nsubjpass(be-2,Mary-1), prep_in(be-2,1980-5), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PAST,be-2), year(time-1,1980), time(bear-3,time-1)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(time bear-3 (YearFn 1980))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * Mary was born on July 5th. - Just testing the date
     * day(?T,?D), month(?T,?M), time(?V,?T) ==> {(time ?V (DayFn ?D (MonthFn ?M (YearFn ?Y))))}.
     */
    @Test
    public void testMaryBornOnJuly5th() {

        String input = "root(ROOT-0,be-2), nsubjpass(be-2,Mary-1), prep_on(be-2,July5th-5), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PAST,be-2), number(SINGULAR,July-5), number(SINGULAR,5), th-6(null,null), month(time-1,July), day(time-1,5), time(bear-3,time-1)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(time bear-3 (DayFn 5 (MonthFn July (YearFn ?Y))))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * Mary was born on the 5th. - Just testing the date
     day(?T,?D), time(?V,?T) ==> {(time ?V (DayFn ?D (MonthFn ?M (YearFn ?Y))))}.
     TODO: Test ignored since it appears Stanford NER doesn't recognize the date.
     */
    @Ignore
    @Test
    public void testMaryBornOnThe5th() {

        String input = "det(5th-6, the-5), root(ROOT-0, be-2), nsubjpass(be-2, Mary-1), prep_on(be-2, 5th-6), names(Mary-1, \"Mary\"), sumo(ListOrderFn, 5th-6), attribute(Mary-1, Female), sumo(Human, Mary-1), number(SINGULAR, Mary-1), tense(PAST, be-2), number(SINGULAR, 5th-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        String[] expected = {
                "(time bear-3 (DayFn 5 (MonthFn ?M (YearFn ?Y))))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * Mary ( July 5, 1980 - April 4, 2010) was a pilot. - Testing the date and birthdate
     * BirthDate(?V,?T) day(?T,?D), month(?T,?M), year(?T,?Y) ==> {(birthdate ?V (DayFn ?D (MonthFn ?M (YearFn ?Y))))}.
     */
    @Test
    public void testMaryJuly51980on() {

        String input = "root(ROOT-0,pilot-15), nsubj(pilot-15,Mary-1), dep(Mary-1,July-3), num(July-3,5-4), num(July-3,1980-6), dep(July-3,April-8), num(April-8,4-9), num(April-8,2010-11), cop(pilot-15,be-13), det(pilot-15,a-14), names(Mary-1,\"Mary\"), attribute(Mary-1,Female), sumo(Human,Mary-1), sumo(Pilot,pilot-15), number(SINGULAR,Mary-1), number(SINGULAR,July-3), number(SINGULAR,April-8), tense(PAST,be-13), number(SINGULAR,pilot-15), day(time-2,4), month(time-2,April), BirthDate(Mary-1,time-1), month(time-1,July), year(time-1,1980), day(time-1,5), year(time-2,2010), DeathDate(Mary-1,time-2)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);
        String[] expected = {
                "(birthdate Mary-1 (DayFn 5 (MonthFn July (YearFn 1980))))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * The celebration was from July 5, 1980 to August 4, 1980. - Testing StartTime
     * StartTime(?V,?T), day(?T,?D), month(?T,?M), year(?T,?Y) ==> {(equal (BeginFn (WhenFn ?V)) (DayFn ?D (MonthFn ?M (YearFn ?Y))))}.
    */
     @Test
     public void testCelebrationStartJuly51980() {

         String input = "root(ROOT-0,be-3), det(celebration-2,the-1), nsubj(be-3,celebration-2), prep_from(be-3,July-5), num(July-5,5-6), num(July-5,1980-8), prep_to(be-3,August-10), num(August-10,4-11), num(August-10,1980-13), sumo(SocialParty,celebration-2), number(SINGULAR,celebration-2), tense(PAST,be-3), number(SINGULAR,July-5), number(SINGULAR,August-10), day(time-2,4), StartTime(was-3,time-1), month(time-1,July), year(time-1,1980), month(time-2,August), EndTime(was-3,time-2), day(time-1,5), year(time-2,1980)";
         ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);
         String[] expected = {
                 "(equal (BeginFn (WhenFn was-3)) (DayFn 5 (MonthFn July (YearFn 1980))))"
         };

         ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
         Set<String> actual = Sets.newHashSet(kifClauses);
         Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

         assertThat(cleanedActual, hasItems(expected));
     }

    /****************************************************************
     * The celebration was from July 5, 1980 to August 4, 1980. - Testing EndTime
     * EndTime(?V,?T), day(?T,?D), month(?T,?M), year(?T,?Y) ==> {(equal (EndFn (WhenFn ?V)) (DayFn ?D (MonthFn ?M (YearFn ?Y))))}.
     */
    @Test
    public void testCelebrationEndAugust41980() {

        String input = "root(ROOT-0,be-3), det(celebration-2,the-1), nsubj(be-3,celebration-2), prep_from(be-3,July-5), num(July-5,5-6), num(July-5,1980-8), prep_to(be-3,August-10), num(August-10,4-11), num(August-10,1980-13), sumo(SocialParty,celebration-2), number(SINGULAR,celebration-2), tense(PAST,be-3), number(SINGULAR,July-5), number(SINGULAR,August-10), day(time-2,4), StartTime(was-3,time-1), month(time-1,July), year(time-1,1980), month(time-2,August), EndTime(was-3,time-2), day(time-1,5), year(time-2,1980)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);
        String[] expected = {
                "(equal (EndFn (WhenFn was-3)) (DayFn 4 (MonthFn August (YearFn 1980))))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * The celebration was from July 5 to August 4. - Testing StartTime
     * StartTime(?V,?T), day(?T,?D), month(?T,?M) ==> {(equal (BeginFn (WhenFn ?V)) (DayFn ?D (MonthFn ?M (YearFn ?Y))))}.
     */
    @Test
    public void testCelebrationStartJuly5() {

        String input = "root(ROOT-0,be-3), det(celebration-2,the-1), nsubj(be-3,celebration-2), prep_from(be-3,July-5), num(July-5,5-6), prep_to(be-3,August-8), num(August-8,4-9), sumo(SocialParty,celebration-2), number(SINGULAR,celebration-2), tense(PAST,be-3), number(SINGULAR,July-5), number(SINGULAR,August-8), day(time-2,4), StartTime(was-3,time-1), month(time-1,July), month(time-2,August), EndTime(was-3,time-2), day(time-1,5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);
        String[] expected = {
                "(equal (BeginFn (WhenFn was-3)) (DayFn 5 (MonthFn July (YearFn ?Y))))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * The celebration was from July 5 to August 4. - Testing EndTime
     * EndTime(?V,?T), day(?T,?D), month(?T,?M) ==> {(equal (EndFn (WhenFn ?V)) (DayFn ?D (MonthFn ?M (YearFn ?Y))))}.
     */
    @Test
    public void testCelebrationEndAugust4() {

        String input = "root(ROOT-0,be-3), det(celebration-2,the-1), nsubj(be-3,celebration-2), prep_from(be-3,July-5), num(July-5,5-6), prep_to(be-3,August-8), num(August-8,4-9), sumo(SocialParty,celebration-2), number(SINGULAR,celebration-2), tense(PAST,be-3), number(SINGULAR,July-5), number(SINGULAR,August-8), day(time-2,4), StartTime(was-3,time-1), month(time-1,July), month(time-2,August), EndTime(was-3,time-2), day(time-1,5)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);
        String[] expected = {
                "(equal (EndFn (WhenFn was-3)) (DayFn 4 (MonthFn August (YearFn ?Y))))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * The celebration was from July 1980 to August 1980. - Testing StartTime
     * StartTime(?V,?T), month(?T,?M), year(?T,?Y)  ==> {(equal (BeginFn (WhenFn ?V)) (MonthFn ?M (YearFn ?Y)))}.
     */
    @Test
    public void testCelebrationStartJuly1980() {

        String input = "root(ROOT-0,be-3), det(celebration-2,the-1), nsubj(be-3,celebration-2), prep_from(be-3,July-5), num(July-5,1980-6), prep_to(be-3,August-8), num(August-8,1980-9), sumo(SocialParty,celebration-2), number(SINGULAR,celebration-2), tense(PAST,be-3), number(SINGULAR,July-5), number(SINGULAR,August-8), StartTime(was-3,time-1), month(time-1,July), year(time-1,1980), month(time-2,August), EndTime(was-3,time-2), year(time-2,1980)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);
        String[] expected = {
                "(equal (BeginFn (WhenFn was-3)) (MonthFn July (YearFn 1980)))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * The celebration was from July 1980 to August 1980. - Testing EndTime
     * EndTime(?V,?T), month(?T,?M), year(?T,?Y) ==> {(equal (EndFn (WhenFn ?V)) (MonthFn ?M (YearFn ?Y)))}.
     */
    @Test
    public void testCelebrationEndAugust1980() {

        String input = "root(ROOT-0,be-3), det(celebration-2,the-1), nsubj(be-3,celebration-2), prep_from(be-3,July-5), num(July-5,1980-6), prep_to(be-3,August-8), num(August-8,1980-9), sumo(SocialParty,celebration-2), number(SINGULAR,celebration-2), tense(PAST,be-3), number(SINGULAR,July-5), number(SINGULAR,August-8), StartTime(was-3,time-1), month(time-1,July), year(time-1,1980), month(time-2,August), EndTime(was-3,time-2), year(time-2,1980)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);
        String[] expected = {
                "(equal (EndFn (WhenFn was-3)) (MonthFn August (YearFn 1980)))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * The celebration was from July 1980 to August 1980. - Testing StartTime
     * StartTime(?V,?T), year(?T,?Y) ==> {(equal (BeginFn (WhenFn ?V)) (YearFn ?Y))}.
     */
    @Test
    public void testCelebrationStart1980() {

        String input = "root(ROOT-0,be-3), det(celebration-2,the-1), nsubj(be-3,celebration-2), num(1986-7,1980-5), dep(1986-7,to-6), prep_from(be-3,1986-7), sumo(SocialParty,celebration-2), number(SINGULAR,celebration-2), tense(PAST,be-3), year(time-2,1986), StartTime(was-3,time-1), year(time-1,1980), EndTime(was-3,time-2)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);
        String[] expected = {
                "(equal (BeginFn (WhenFn was-3)) (YearFn 1980))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }

    /****************************************************************
     * The celebration was from July 1980 to August 1980. - Testing EndTime
     * EndTime(?V,?T), year(?T,?Y) ==> {(equal (EndFn (WhenFn ?V)) (YearFn ?Y))}.
     */
    @Test
    public void testCelebrationEnd1986() {

        String input = "root(ROOT-0,be-3), det(celebration-2,the-1), nsubj(be-3,celebration-2), num(1986-7,1980-5), dep(1986-7,to-6), prep_from(be-3,1986-7), sumo(SocialParty,celebration-2), number(SINGULAR,celebration-2), tense(PAST,be-3), year(time-2,1986), StartTime(was-3,time-1), year(time-1,1980), EndTime(was-3,time-2)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);
        String[] expected = {
                "(equal (EndFn (WhenFn was-3)) (YearFn 1986))"
        };

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());

        assertThat(cleanedActual, hasItems(expected));
    }
}
