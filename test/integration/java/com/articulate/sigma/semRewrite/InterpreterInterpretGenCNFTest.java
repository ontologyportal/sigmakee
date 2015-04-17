package com.articulate.sigma.semRewrite;

import com.articulate.sigma.IntegrationTestBase;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Tests Interpreter.interpretGenCNF( )
 * In: Natural language sentences.
 * Out: A CNF, aka the dependency parse.
 */
public class InterpreterInterpretGenCNFTest extends IntegrationTestBase {

    private static Interpreter interpreter;

    @Before
    public void setUpInterpreter() throws IOException {
        interpreter = new Interpreter();
        interpreter.inference = false;
        interpreter.initialize();

        IntegrationTestBase.resetAllForInference();
    }

    @Test
    public void testAmeliaLivesInMyComputer()   {
        String input = "Amelia lives in my computer.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "sumo(DiseaseOrSyndrome,Amelia-1)", "number(SINGULAR,Amelia-1)", "root(ROOT-0,live-2)",
                "nsubj(live-2,Amelia-1)", "sumo(Living,live-2)", "tense(PRESENT,live-2)", "number(SINGULAR,computer-5)",
                "prep_in(live-2,computer-5)", "poss(computer-5,my-4)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testWhereDoesAmeliaLive()   {
        String input = "Where does Amelia live?";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "sumo(Human,Amelia-3)", "names(Amelia-3,\"Amelia\")", "attribute(Amelia-3,Female)",
                "number(SINGULAR,Amelia-3)", "sumo(IntentionalProcess,do-2)", "root(ROOT-0,live-4)",
                "nsubj(live-4,Amelia-3)", "sumo(Living,live-4)", "advmod(live-4,where-1)", "aux(live-4,do-2)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testAmeliaLivedInUS()   {
        String input = "Amelia lived in the United States.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "nn(States-6,United-5)", "sumo(Human,Amelia-1)", "sumo(Cooperation,United-5)",
                "names(Amelia-1,\"Amelia\")", "sumo(StateOrProvince,States-6)", "attribute(Amelia-1,Female)",
                "number(SINGULAR,Amelia-1)", "tense(PAST,lived-2)", "number(SINGULAR,United-5)", "number(PLURAL,States-6)",
                "root(ROOT-0,live_in-2)", "nsubj(live_in-2,Amelia-1)", "prep_in(live_in-2,States-6)", "det(States-6,the-4)"
         );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testWhereDidAmeliaLive()   {
        String input = "Where did Amelia live?";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "sumo(Human,Amelia-3)", "names(Amelia-3,\"Amelia\")", "attribute(Amelia-3,Female)",
                "number(SINGULAR,Amelia-3)", "sumo(IntentionalProcess,do-2)", "root(ROOT-0,live-4)",
                "nsubj(live-4,Amelia-3)", "sumo(Living,live-4)", "advmod(live-4,where-1)", "aux(live-4,do-2)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testAmeliaFliesAirplanes()   {
        String input = "Amelia flies airplanes.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "sumo(Human,Amelia-1)", "names(Amelia-1,\"Amelia\")", "attribute(Amelia-1,Female)",
                 "number(SINGULAR,Amelia-1)", "root(ROOT-0,fly-2)", "nsubj(fly-2,Amelia-1)", "sumo(Flying,fly-2)",
                 "tense(PRESENT,fly-2)", "sumo(Airplane,airplane-3)", "number(PLURAL,airplane-3)", "dobj(fly-2,airplane-3)"
         );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testWhoFliesAirplanes()   {
        String input = "Who flies airplanes?";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "root(ROOT-0,fly-2)", "sumo(Flying,fly-2)", "tense(PRESENT,fly-2)", "nsubj(fly-2,who-1)",
                "sumo(Airplane,airplane-3)", "number(PLURAL,airplane-3)", "dobj(fly-2,airplane-3)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testMaryMadeAHouse()   {
        String input = "Mary made a house.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "names(Mary-1,\"Mary\")", "attribute(Mary-1,Female)", "sumo(Human,Mary-1)", "number(SINGULAR,Mary-1)",
                "root(ROOT-0,make-2)", "nsubj(make-2,Mary-1)", "tense(PAST,make-2)", "sumo(House,house-4)",
                "number(SINGULAR,house-4)", "dobj(make-2,house-4)", "det(house-4,a-3)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testMaryHasMadeAHouse()   {
        String input = "Mary has made a house.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "names(Mary-1,\"Mary\")", "attribute(Mary-1,Female)", "sumo(Human,Mary-1)", "number(SINGULAR,Mary-1)",
                "root(ROOT-0,make-3)", "nsubj(make-3,Mary-1)", "tense(PRESENT,make-3)", "aspect(PERFECT,make-3)",
                "aux(make-3,have-2)", "sumo(House,house-5)", "number(SINGULAR,house-5)", "dobj(make-3,house-5)", "det(house-5,a-4)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testMaryMadeUpAStory()   {
        String input = "Mary made up a story.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "names(Mary-1,\"Mary\")", "attribute(Mary-1,Female)", "sumo(Human,Mary-1)", "number(SINGULAR,Mary-1)", "tense(PAST,made-2)",
                "root(ROOT-0,make_up-2)", "nsubj(make_up-2,Mary-1)", "sumo(Stating,story-5)",
                "number(SINGULAR,story-5)", "dobj(make_up-2,story-5)", "det(story-5,a-4)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testMaryHasMadeUpAStory()   {
        String input = "Mary has made up a story.";
        CNF cnf = interpreter.interpretGenCNF(input);

//        String[] expected = {
//                "attribute(Mary-1,Female)", "sumo(Human,Mary-1)", "names(Mary-1,\"Mary\")", "number(SINGULAR,Mary-1)", "tense(PRESENT,made-3)",
//                "aspect(PERFECT,made-3)", "root(ROOT-0,make_up-3)", "nsubj(make_up-3,Mary-1)", "aux(make_up-3,have-2)", "sumo(Stating,story-6)",
//                "number(SINGULAR,story-6)", "dobj(make_up-3,story-6)", "det(story-6,a-5)"
//        };
//
//        assertThat(cnf.toListString(), containsInAnyOrder(expected));

        Set<String> expected = Sets.newHashSet(
                "names(Mary-1,\"Mary\")", "attribute(Mary-1,Female)", "sumo(Human,Mary-1)", "number(SINGULAR,Mary-1)", "tense(PRESENT,made-3)",
                "aspect(PERFECT,made-3)", "root(ROOT-0,make_up-3)", "nsubj(make_up-3,Mary-1)", "aux(make_up-3,have-2)", "sumo(Stating,story-6)",
                "number(SINGULAR,story-6)", "dobj(make_up-3,story-6)", "det(story-6,a-5)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

}
