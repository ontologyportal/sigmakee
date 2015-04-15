package com.articulate.sigma.inference;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.semRewrite.Interpreter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by qingqingcai on 3/26/15.
 */
@Ignore
public class QAInferenceTest extends IntegrationTestBase {

    private static Interpreter interpreter;

    @Before
    public void init() throws IOException {
        interpreter = new Interpreter();
        interpreter.inference = true;
        interpreter.initialize();

        IntegrationTestBase.resetAllForInference();
    }

    @Test
    public void test0() throws IOException {
        String assertion = "Amelia flies a plane.";

        interpreter.interpret(assertion);

        String actualAnswer = null;

        String query = "What does Amelia fly?";
        actualAnswer = interpreter.interpret(query).get(0);
        System.out.println("actualAnswer = " + actualAnswer);
        assertEquals("An instance of Airplane.", actualAnswer);

        String query2 = "Who flies a plane?";
        actualAnswer = interpreter.interpret(query2).get(0);
        System.out.println("actualAnswer = " + actualAnswer);
        assertEquals("'Amelia'.", actualAnswer);

        String query3 = "Does Amelia fly a plane?";
        actualAnswer = interpreter.interpret(query3).get(0);
        System.out.println("actualAnswer = " + actualAnswer);
        assertNotEquals("No response.", actualAnswer);
    }

    @Test
    public void test1() throws IOException {

        kb.tell("(=>\n" +
                "  (citizen ?X ?Y)\n" +
                "  (inhabits ?X ?Y))");

        interpreter.interpret("Amelia (July 24, 1897 – July 2, 1937) was an American aviator.");

        String actualAnswer = interpreter.interpret("Where does Amelia live?").get(0);
        assertEquals("UnitedStates.", actualAnswer);
    }

    @Test
    public void test2() throws IOException {

        interpreter.interpret("John kicks a cart.");

        String actualAnswer = interpreter.interpret("Who hits the cart?").get(0);
        assertEquals("'John'.", actualAnswer);
    }
}
