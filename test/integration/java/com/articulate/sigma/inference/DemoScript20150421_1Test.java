package com.articulate.sigma.inference;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.semRewrite.Interpreter;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class DemoScript20150421_1Test extends IntegrationTestBase {

    private static Interpreter interpreter;

    @BeforeClass
    public static void initInterpreter() throws IOException {
        interpreter = new Interpreter();
        interpreter.inference = true;
        interpreter.initialize();

        IntegrationTestBase.resetAllForInference();
    }

    @Test
    public void test1() {

        String input = "The Liberty Bell, located in Pennsylvania, changed the world on July 8, 1776.";
        interpreter.interpret(input).get(0);
        input = "It rang out from the tower of Independence Hall.";
        interpreter.interpret(input).get(0);
        input = "It was used to summon the citizens to hear the first reading of the Declaration of Independence by Colonel John Nixon.";
        interpreter.interpret(input).get(0);
        input = "In the 1800s, the Liberty Bell became a symbol for ending slavery in America.";
        interpreter.interpret(input).get(0);
        input = "The Liberty Bell is famous for its large crack.";
        interpreter.interpret(input).get(0);
        input = "The crack got worse over time and people did not want the bell to break apart.";
        interpreter.interpret(input).get(0);
        input = "In order to preserve it, it was decided that the Liberty Bell should never again be used.";
        interpreter.interpret(input).get(0);
        input = "The bell has not been rung since Washington’s birthday in 1842.";
        interpreter.interpret(input).get(0);

        input = "When did the Liberty Bell change the world?";
        String actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("DayFn(8,s__MonthFn(s__July,s__YearFn(1776))).", actualAnswer);

        input = "When was Washington’s birthday?";
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("YearFn(1842).", actualAnswer);

        input = "When was the Liberty Bell last rung?";
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("YearFn(1842).", actualAnswer);

//        input = "Where is Liberty Bell located?";
//        actualAnswer = interpreter.interpret(input).get(0);
//        assertEquals("An instance of Pennsylvania.", actualAnswer);
    }
}
