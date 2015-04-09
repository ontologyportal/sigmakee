package com.articulate.sigma.inference;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.semRewrite.Interpreter;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class DemoScript20150401Test extends IntegrationTestBase {

    private static Interpreter interpreter;

    @BeforeClass
    public static void initInterpreter() throws IOException {
        interpreter = new Interpreter();
        Interpreter.inference = true;
        interpreter.initialize();

        IntegrationTestBase.resetAllForInference();
    }

    @Test
    public void test1() {
        String input = "Amelia Mary Earhart (July 24, 1897 – July 2, 1937) was an American aviator, one of the first women to fly a plane long distances.";
        interpreter.interpret(input).get(0);
        input = "She was the first woman to fly a plane by herself across the Atlantic Ocean.";
        interpreter.interpret(input).get(0);
        input = "Earhart vanished over the South Pacific Ocean in July 1937 while trying to fly around the world.";
        interpreter.interpret(input).get(0);
        input = "She was declared dead on January 5, 1939.";
        interpreter.interpret(input).get(0);

        input = "What language did Amelia speak?";
        interpreter.interpret(input).get(0);
        String actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("EnglishLanguage.", actualAnswer);

        input = "Where did she live?";
        interpreter.interpret(input).get(0);
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("UnitedStates.", actualAnswer);

        input = "Where did Amelia live?";
        interpreter.interpret(input).get(0);
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("UnitedStates.", actualAnswer);

        input = "When was she born?";
        interpreter.interpret(input).get(0);
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("DayFn(24,s__MonthFn(s__July,s__YearFn(1897))).", actualAnswer);

        input = "When did she die?";
        interpreter.interpret(input).get(0);
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("DayFn(2,s__MonthFn(s__July,s__YearFn(1937))).", actualAnswer);

        input = "What was she interested in?";
        interpreter.interpret(input).get(0);
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("An instance of Airplane.", actualAnswer);

        input = "Was she interested in airplanes?";
        interpreter.interpret(input).get(0);
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("Yes.", actualAnswer);

        input = "Where did she fly?";
        interpreter.interpret(input).get(0);
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("AtlanticOcean.", actualAnswer);

        input = "What did she fly?";
        interpreter.interpret(input).get(0);
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("An instance of Airplane.", actualAnswer);

        // We cannot answer "Was Amelia female?" because Amelia is an instance of DiseaseOrSyndrome
        input = "Was Amelia Mary Earhart female?";
        interpreter.interpret(input).get(0);
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("Yes.", actualAnswer);

        input = "Where did she disappear?";
        interpreter.interpret(input).get(0);
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("SouthPacificOcean.", actualAnswer);
    }

    @Ignore
    @Test
    public void test2() {
        String input = "Amelia Mary Earhart (July 24, 1897 – July 2, 1937) was an American aviator, one of the first women to fly a plane long distances.";
        interpreter.interpret(input).get(0);
        input = "She was the first woman to fly a plane by herself across the Atlantic Ocean.";
        interpreter.interpret(input).get(0);
        input = "Earhart vanished over the South Pacific Ocean in July 1937 while trying to fly around the world.";
        interpreter.interpret(input).get(0);
        input = "She was declared dead on January 5, 1939.";
        interpreter.interpret(input).get(0);

        // Incorrect, but this is the current output.
        input = "What was her nationality?";
        interpreter.interpret(input).get(0);
        String actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("UnitedStates.", actualAnswer);

        input = "Where did she disappear?";
        interpreter.interpret(input).get(0);
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("SouthPacificOcean.", actualAnswer);

        // Wait Rashmi to fix the vanished-4 (stemming) issue
        input = "When did she disappear?";
        interpreter.interpret(input).get(0);
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("I don't know", actualAnswer);

        input = "Who was declared dead?";
        interpreter.interpret(input).get(0);
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("I don't know", actualAnswer);
    }
}
