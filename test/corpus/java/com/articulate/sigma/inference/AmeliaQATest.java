package com.articulate.sigma.inference;

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.semRewrite.Interpreter;
import org.junit.BeforeClass;
import org.junit.Test;

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
        interpreter.loadRules();
    }

    @Test
    public void test0() {

        setKB();
        String assertion = "Amelia flies a plane.";
        String query = "What does Amelia fly?";

        interpreter.question = false;
        interpreter.interpretSingle(assertion);

        interpreter.question = true;
        String actualAnswer = interpreter.interpretSingle(query);
        System.out.println("actualAnswer = " + actualAnswer);
        assertNotEquals(actualAnswer,"No response.");
    }
}
