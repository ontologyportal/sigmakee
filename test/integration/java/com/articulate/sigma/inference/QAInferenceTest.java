package com.articulate.sigma.inference;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.semRewrite.Interpreter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by qingqingcai on 3/26/15.
 */
public class QAInferenceTest extends IntegrationTestBase {

    private static Interpreter interpreter;

    @Before
    public void initInterpreter()   {
        interpreter = new Interpreter();
        interpreter.inference = true;
        interpreter.initialize();
    }

    @Test
    public void test0() {
        initInterpreter();
        String assertion = "Amelia flies a plane.";

        interpreter.question = false;
        interpreter.interpret(assertion);

        interpreter.question = true;
        String actualAnswer = null;

        String query = "What does Amelia fly?";
        actualAnswer = interpreter.interpret(query).get(0);
        System.out.println("actualAnswer = " + actualAnswer);
        assertEquals("An instance of Airplane.", actualAnswer);

        String query2 = "Who flies a plane?";
        actualAnswer = interpreter.interpret(query2).get(0);
        System.out.println("actualAnswer = " + actualAnswer);
        assertEquals("Amelia flies a plane.", actualAnswer);

        String query3 = "Does Amelia fly a plane?";
        actualAnswer = interpreter.interpret(query3).get(0);
        System.out.println("actualAnswer = " + actualAnswer);
        assertNotEquals("No response.", actualAnswer);
    }

    @Test
    public void test1() {
        initInterpreter();
        String assertion = "Amelia (July 24, 1897 – July 2, 1937) was an American aviator.";
        String query = "Where does Amelia live?";

        kb.tell("(=>\n" +
                "  (citizen ?X ?Y)\n" +
                "  (inhabits ?X ?Y))");

        interpreter.question = false;
        interpreter.interpret(assertion);

        interpreter.question = true;
        String actualAnswer = null;

        actualAnswer = interpreter.interpret(query).get(0);
        System.out.println("actualAnswer = " + actualAnswer);
        assertEquals("UnitedStates.", actualAnswer);
    }

    @Test
    public void test2() {
        initInterpreter();
        String assertion = "John kicks a cart.";
        String query = "Who hits the cart?";

        interpreter.question = false;
        interpreter.interpret(assertion);

        interpreter.question = true;
        String actualAnswer = null;

        actualAnswer = interpreter.interpret(query).get(0);
        System.out.println("actualAnswer = " + actualAnswer);
        assertEquals("I don't know", actualAnswer);
    }
}
