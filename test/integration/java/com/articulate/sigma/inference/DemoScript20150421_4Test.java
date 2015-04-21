package com.articulate.sigma.inference;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.semRewrite.Interpreter;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class DemoScript20150421_4Test extends IntegrationTestBase {

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

        String input = "Adolf Hitler was an Austrian-born German politician who was the leader of the Nazi Party (German: National sozialistische Deutsche Arbeiter partei (NSDAP); National Socialist German Workers Party).";
        interpreter.interpret(input).get(0);
        input = "He was chancellor of Germany from 1933 to 1945 and Führer (leader) of Nazi Germany from 1934 to 1945.";
        interpreter.interpret(input).get(0);
        input = "As effective dictator of Nazi Germany, Hitler was at the centre of World War II in Europe, and the Holocaust.";
        interpreter.interpret(input).get(0);
        input = "Hitler was a decorated veteran of World War I.";
        interpreter.interpret(input).get(0);
        input = "He joined the German Workers' Party (precursor of the NSDAP) in 1919, and became leader of the NSDAP in 1921.";
        interpreter.interpret(input).get(0);
        input = "In 1923, he attempted a coup in Munich to seize power.";
        interpreter.interpret(input).get(0);
        input = "The failed coup resulted in Hitler's imprisonment, during which time he wrote his autobiography and political manifesto Mein Kampf (\"My Struggle\").";
        interpreter.interpret(input).get(0);

        input = "Who was the leader of the Nazi Party?";
        String actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("'Adolf Hitler'.", actualAnswer);

        input = "Who was the effective dictator of Nazi Germany?";
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("'Adolf Hitler'.", actualAnswer);
    }
}
