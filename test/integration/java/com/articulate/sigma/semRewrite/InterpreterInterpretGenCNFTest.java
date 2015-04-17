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

    @Test
    public void testMaryWentAfterMidnight()   {
        String input = "Mary went after midnight.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "root(ROOT-0,go_after-2)", "names(Mary-1,\"Mary\")", "hour(time-1,00-4)", "attribute(Mary-1,Female)",
                "sumo(Human,Mary-1)", "tense(PAST,went-2)", "time(go-2,time-1)", "minute(time-1,00-4)", "number(SINGULAR,midnight-4)",
                "nsubj(go_after-2,Mary-1)", "sumo(TimePoint,midnight-4)", "number(SINGULAR,Mary-1)", "prep_after(go_after-2,midnight-4)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testMaryWalksWithTheMan()   {
        String input = "Mary walks with the man.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "det(man-5,the-4)", "sumo(Man,man-5)", "names(Mary-1,\"Mary\")", "root(ROOT-0,walk-2)",
                "nsubj(walk-2,Mary-1)", "tense(PRESENT,walk-2)", "attribute(Mary-1,Female)", "sumo(Human,Mary-1)",
                "sumo(Walking,walk-2)", "prep_with(walk-2,man-5)", "number(SINGULAR,man-5)", "number(SINGULAR,Mary-1)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testMaryWalksForTwoHours()   {
        String input = "Mary walks for two hours.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "names(Mary-1,\"Mary\")", "sumo(Hour,hour-5)", "root(ROOT-0,walk-2)", "nsubj(walk-2,Mary-1)",
                "tense(PRESENT,walk-2)", "attribute(Mary-1,Female)", "sumo(Human,Mary-1)", "sumo(Walking,walk-2)",
                "prep_for(walk-2,hour-5)", "num(hour-5,two-4)", "number(SINGULAR,Mary-1)", "number(PLURAL,hour-5)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testMaryWalkedForTwoHours()   {
        String input = "Mary walked for two hours.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "names(Mary-1,\"Mary\")", "sumo(Hour,hour-5)", "root(ROOT-0,walk-2)", "nsubj(walk-2,Mary-1)",
                "attribute(Mary-1,Female)", "tense(PAST,walk-2)", "sumo(Human,Mary-1)", "sumo(Walking,walk-2)",
                "prep_for(walk-2,hour-5)", "num(hour-5,two-4)", "number(SINGULAR,Mary-1)", "number(PLURAL,hour-5)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testMaryHasWalkedForTwoHours()   {
        String input = "Mary has walked for two hours.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "nsubj(walk-3,Mary-1)", "names(Mary-1,\"Mary\")", "sumo(Hour,hour-6)", "root(ROOT-0,walk-3)", 
                "aspect(PERFECT,walk-3)", "attribute(Mary-1,Female)", "tense(PRESENT,walk-3)", "sumo(Human,Mary-1)", 
                "sumo(Walking,walk-3)", "num(hour-6,two-5)", "aux(walk-3,have-2)", "prep_for(walk-3,hour-6)", 
                "number(SINGULAR,Mary-1)", "number(PLURAL,hour-6)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testMaryHasBeenWalkingForTwoHours()   {
        String input = "Mary has been walking for two hours.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "names(Mary-1,\"Mary\")", "attribute(Mary-1,Female)", "root(ROOT-0,walk-4)", "aux(walk-4,be-3)",
                "sumo(Human,Mary-1)", "sumo(Hour,hour-7)", "sumo(Walking,walk-4)", "aux(walk-4,have-2)", "num(hour-7,two-6)",
                "prep_for(walk-4,hour-7)", "tense(PRESENT,walk-4)", "number(PLURAL,hour-7)", "number(SINGULAR,Mary-1)",
                "nsubj(walk-4,Mary-1)", "aspect(PROGRESSIVEPERFECT,walk-4)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testMaryWasWalkingForTwoHours()   {
        String input = "Mary was walking for two hours.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "aux(walk-3,be-2)", "nsubj(walk-3,Mary-1)", "names(Mary-1,\"Mary\")", "sumo(Hour,hour-6)",
                "root(ROOT-0,walk-3)", "attribute(Mary-1,Female)", "aspect(PROGRESSIVE,walk-3)", "sumo(Human,Mary-1)",
                "sumo(Walking,walk-3)", "tense(PAST,walk-3)", "num(hour-6,two-5)", "prep_for(walk-3,hour-6)",
                "number(SINGULAR,Mary-1)", "number(PLURAL,hour-6)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testMaryHadWalkedForTwoHours()   {
        String input = "Mary had walked for two hours.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "nsubj(walk-3,Mary-1)", "names(Mary-1,\"Mary\")", "sumo(Hour,hour-6)", "root(ROOT-0,walk-3)",
                "aspect(PERFECT,walk-3)", "attribute(Mary-1,Female)", "sumo(Human,Mary-1)", "sumo(Walking,walk-3)",
                "tense(PAST,walk-3)", "num(hour-6,two-5)", "aux(walk-3,have-2)", "prep_for(walk-3,hour-6)",
                "number(SINGULAR,Mary-1)", "number(PLURAL,hour-6)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testMaryHadBeenWalkingForTwoHours()   {
        String input = "Mary had been walking for two hours.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "names(Mary-1,\"Mary\")", "attribute(Mary-1,Female)", "root(ROOT-0,walk-4)", "aux(walk-4,be-3)", 
                "sumo(Human,Mary-1)", "tense(PAST,walk-4)", "sumo(Hour,hour-7)", "sumo(Walking,walk-4)", "aux(walk-4,have-2)", 
                "num(hour-7,two-6)", "prep_for(walk-4,hour-7)", "number(PLURAL,hour-7)", "number(SINGULAR,Mary-1)", 
                "nsubj(walk-4,Mary-1)", "aspect(PROGRESSIVEPERFECT,walk-4)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testMaryWillWalkForTwoHours()   {
        String input = "Mary will walk for two hours.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "nsubj(walk-3,Mary-1)", "names(Mary-1,\"Mary\")", "sumo(Hour,hour-6)", "root(ROOT-0,walk-3)",
                "attribute(Mary-1,Female)", "sumo(Human,Mary-1)", "sumo(Walking,walk-3)", "num(hour-6,two-5)",
                "tense(FUTURE,walk-3)", "prep_for(walk-3,hour-6)", "aux(walk-3,will-2)", "number(SINGULAR,Mary-1)",
                "number(PLURAL,hour-6)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testMaryWillBeWalkingForTwoHours()   {
        String input = "Mary will be walking for two hours.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "names(Mary-1,\"Mary\")", "aspect(PROGRESSIVE,walk-4)", "attribute(Mary-1,Female)",
                "root(ROOT-0,walk-4)", "aux(walk-4,be-3)", "sumo(Human,Mary-1)", "sumo(Hour,hour-7)", "sumo(Walking,walk-4)",
                "aux(walk-4,will-2)", "tense(FUTURE,walk-4)", "num(hour-7,two-6)", "prep_for(walk-4,hour-7)", "number(PLURAL,hour-7)",
                "number(SINGULAR,Mary-1)", "nsubj(walk-4,Mary-1)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testMaryWillHaveWalkedForTwoHours()   {
        String input = "Mary will have walked for two hours.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "names(Mary-1,\"Mary\")", "attribute(Mary-1,Female)", "root(ROOT-0,walk-4)", "aspect(PERFECT,walk-4)",
                "sumo(Human,Mary-1)", "sumo(Hour,hour-7)", "sumo(Walking,walk-4)", "aux(walk-4,will-2)", "tense(FUTURE,walk-4)",
                "num(hour-7,two-6)", "aux(walk-4,have-3)", "prep_for(walk-4,hour-7)", "number(PLURAL,hour-7)",
                "number(SINGULAR,Mary-1)", "nsubj(walk-4,Mary-1)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

    @Test
    public void testMaryWillHaveBeenWalkingForTwoHours()   {
        String input = "Mary will have been walking for two hours.";
        CNF cnf = interpreter.interpretGenCNF(input);

        Set<String> expected = Sets.newHashSet(
                "names(Mary-1,\"Mary\")", "attribute(Mary-1,Female)", "root(ROOT-0,walk-5)", "sumo(Walking,walk-5)",
                "sumo(Human,Mary-1)", "num(hour-8,two-7)", "sumo(Hour,hour-8)", "aux(walk-5,will-2)", "tense(FUTURE,walk-5)",
                "nsubj(walk-5,Mary-1)", "number(SINGULAR,Mary-1)", "number(PLURAL,hour-8)", "prep_for(walk-5,hour-8)",
                "aux(walk-5,be-4)", "aspect(PROGRESSIVEPERFECT,walk-5)", "aux(walk-5,have-3)"
        );

        Set<String> cnfSets = Sets.newHashSet(cnf.toListString());
        assertEquals(expected, cnfSets);
    }

}
