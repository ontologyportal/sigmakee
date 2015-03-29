package com.articulate.sigma.inference;

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.semRewrite.Interpreter;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by qingqingcai on 3/26/15.
 */
public class AmeliaQATest {

    private static KB kb;
    private static Interpreter interpreter;

    /** ***************************************************************
     */
    @BeforeClass
    public static void setKB() {

        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB("SUMO");
        interpreter = new Interpreter();

        interpreter.inference = true;
        interpreter.initialize();
    }

    @Test
    public void test0() {

        setKB();
        String assertion = "Amelia flies a plane.";
        String query = "What does Amelia fly?";
        String query2 = "Who flies a plane?";
        String query3 = "Does Amelia fly a plane?";

        interpreter.question = false;
        interpreter.interpretSingle(assertion);

        interpreter.question = true;
        String actualAnswer = null;

        actualAnswer = interpreter.interpretSingle(query);
        System.out.println("actualAnswer = " + actualAnswer);
    //    assertNotEquals(actualAnswer,"No response.");
    //    assertEquals(actualAnswer, "An instance of Airplane.");
        assertEquals(actualAnswer, "Flying.");

        actualAnswer = interpreter.interpretSingle(query2);
        System.out.println("actualAnswer = " + actualAnswer);
    //    assertNotEquals(actualAnswer,"No response.");
        assertEquals(actualAnswer, "'Amelia'.");

        actualAnswer = interpreter.interpretSingle(query3);
        System.out.println("actualAnswer = " + actualAnswer);
        assertNotEquals(actualAnswer,"No response.");
    //    assertEquals(actualAnswer, "Yes.");
    }

    @Test
    public void test1() {

        setKB();
        String assertion = "John was an American.";
        String query = "Where does John live?";

        kb.tell("(=>\n" +
                "  (citizen ?X ?Y)\n" +
                "  (inhabits ?X ?Y))");

        interpreter.question = false;
        interpreter.interpretSingle(assertion);

        interpreter.question = true;
        String actualAnswer = null;

        actualAnswer = interpreter.interpretSingle(query);
        System.out.println("actualAnswer = " + actualAnswer);
        assertEquals(actualAnswer,"UnitedStates.");
    }

    @Test
    public void test2() {

        setKB();
        String assertion = "John kicks a cart.";
        String query = "Who hits the cart?";

        interpreter.question = false;
        interpreter.interpretSingle(assertion);

        interpreter.question = true;
        String actualAnswer = null;

        actualAnswer = interpreter.interpretSingle(query);
        System.out.println("actualAnswer = " + actualAnswer);
        assertEquals(actualAnswer,"'John'.");
    }
}
