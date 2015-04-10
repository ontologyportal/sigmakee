package com.articulate.sigma.inference;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.semRewrite.Interpreter;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Verifies that when we call resetAllForInference( ), the state is untouched by any assertions previously made.
 * WARNING: Do not use these tests for examples of how to write tests for inference. Look for other users of resetAllForInference( ).
 */
public class InferenceInitTest extends IntegrationTestBase {

    private static Interpreter interpreter;

    private static void initInterpreter() throws IOException {
        interpreter = new Interpreter();
        interpreter.inference = true;
        interpreter.initialize();
    }

    @Before
    public void setupEachTest() throws IOException {
        initInterpreter();
    }

    @Test
    public void testReInit() throws IOException {
        interpreter.interpret("Mike hits a wagon.");

        initInterpreter();
        IntegrationTestBase.resetAllForInference();

        String actualAnswer = interpreter.interpret("What does Mike hit?").get(0);
        assertEquals("I don't know", actualAnswer);
    }
}
