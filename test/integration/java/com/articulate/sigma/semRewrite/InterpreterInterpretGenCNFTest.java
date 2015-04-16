package com.articulate.sigma.semRewrite;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.UnitTestBase;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests Interpreter.interpretGenCNF( )
 * In: Natural language sentences.
 * Out: An array of CNF.
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
        ArrayList<CNF> cnf = interpreter.interpretGenCNF(input);

        String expected = "[sumo(DiseaseOrSyndrome,Amelia-1), number(SINGULAR,Amelia-1), root(ROOT-0,live-2), nsubj(live-2,Amelia-1), sumo(Living,live-2), tense(PRESENT,live-2), " +
                "number(SINGULAR,computer-5), prep_in(live-2,computer-5), poss(computer-5,my-4)]";

        assertEquals(expected, cnf.toString());
    }

    @Test
    public void testWhereDoesAmeliaLive()   {
        String input = "Where does Amelia live?";
        ArrayList<CNF> cnf = interpreter.interpretGenCNF(input);

        String expected = "[sumo(Human,Amelia-3), names(Amelia-3,\"Amelia\"), attribute(Amelia-3,Female), " +
                "number(SINGULAR,Amelia-3), sumo(IntentionalProcess,do-2), root(ROOT-0,live-4), nsubj(live-4,Amelia-3), sumo(Living,live-4), advmod(live-4,where-1), aux(live-4,do-2)]";

        assertEquals(expected, cnf.toString());
    }

    @Test
    public void testAmeliaLivedInUS()   {
        String input = "Amelia lived in the United States.";
        ArrayList<CNF> cnf = interpreter.interpretGenCNF(input);

        String expected = "[nn(States-6,United-5), sumo(Human,Amelia-1), sumo(Cooperation,United-5), names(Amelia-1,\"Amelia\"), sumo(StateOrProvince,States-6), attribute(Amelia-1,Female), number(SINGULAR,Amelia-1), tense(PAST,lived-2), number(SINGULAR,United-5), number(PLURAL,States-6), " +
                "root(ROOT-0,live_in-2), nsubj(live_in-2,Amelia-1), prep_in(live_in-2,States-6), det(States-6,the-4)]";

        assertEquals(expected, cnf.toString());
    }

    @Test
    public void testWhereDidAmeliaLive()   {
        String input = "Where did Amelia live?";
        ArrayList<CNF> cnf = interpreter.interpretGenCNF(input);

        String expected = "[sumo(Human,Amelia-3), names(Amelia-3,\"Amelia\"), attribute(Amelia-3,Female), " +
                "number(SINGULAR,Amelia-3), sumo(IntentionalProcess,do-2), root(ROOT-0,live-4), nsubj(live-4,Amelia-3), sumo(Living,live-4), advmod(live-4,where-1), aux(live-4,do-2)]";

        assertEquals(expected, cnf.toString());
    }

    @Test
    public void testAmeliaFliesAirplanes()   {
        String input = "Amelia flies airplanes.";
        ArrayList<CNF> cnf = interpreter.interpretGenCNF(input);

        String expected = "[sumo(Human,Amelia-1), names(Amelia-1,\"Amelia\"), attribute(Amelia-1,Female), number(SINGULAR,Amelia-1), root(ROOT-0,fly-2), nsubj(fly-2,Amelia-1), sumo(Flying,fly-2), tense(PRESENT,fly-2), " +
            "sumo(Airplane,airplane-3), number(PLURAL,airplane-3), dobj(fly-2,airplane-3)]";

        assertEquals(expected, cnf.toString());
    }

    @Test
    public void testWhoFliesAirplanes()   {
        String input = "Who flies airplanes?";
        ArrayList<CNF> cnf = interpreter.interpretGenCNF(input);

        String expected = "[root(ROOT-0,fly-2), sumo(Flying,fly-2), tense(PRESENT,fly-2), nsubj(fly-2,who-1), sumo(Airplane,airplane-3), number(PLURAL,airplane-3), dobj(fly-2,airplane-3)]";

        assertEquals(expected, cnf.toString());
    }

}
