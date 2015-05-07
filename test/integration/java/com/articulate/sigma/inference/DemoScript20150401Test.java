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
        interpreter.inference = true;
        interpreter.initialize();

        IntegrationTestBase.resetAllForInference();
    }

    @Ignore
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
        String actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("EnglishLanguage.", actualAnswer);

        input = "Where did she live?";
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("UnitedStates.", actualAnswer);

        input = "Where did Amelia live?";
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("UnitedStates.", actualAnswer);

        input = "When was she born?";
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("DayFn(24,s__MonthFn(s__July,s__YearFn(1897))).", actualAnswer);

        input = "When did she die?";
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("DayFn(2,s__MonthFn(s__July,s__YearFn(1937))).", actualAnswer);

        input = "Was she interested in airplanes?";
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("Yes.", actualAnswer);

        input = "Where did she fly?";
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("AtlanticOcean.", actualAnswer);

        input = "What did she fly?";
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("An instance of Airplane.", actualAnswer);

        // We cannot answer "Was Amelia female?" because Amelia is an instance of DiseaseOrSyndrome
        input = "Was Amelia Mary Earhart female?";
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("Yes.", actualAnswer);

        input = "Where did she disappear?";
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("SouthPacificOcean.", actualAnswer);
        //assertEquals("PacificOcean.", actualAnswer);

        input = "When did she disappear?";
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("MonthFn(s__July,s__YearFn(1937)).", actualAnswer);
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

        input = "What was Amelia interested in?";
        String actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("An instance of Airplane.", actualAnswer);
    }

    @Ignore
    @Test
    public void test3() {
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
        String actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("UnitedStates.", actualAnswer);

        input = "Who was declared dead?";
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("I don't know", actualAnswer);
    }
}
