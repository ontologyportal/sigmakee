package com.articulate.sigma.inference;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.semRewrite.Interpreter;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class DemoScript20150421_2Test extends IntegrationTestBase {

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

        String input = "Blessed Teresa of Calcutta, MC, commonly known as Mother Teresa (26 August 1910 – 5 September 1997), was a Roman Catholic religious sister and missionary who lived most of her life in India.";
        interpreter.interpret(input).get(0);
        input = "Mother Teresa was born in today's Macedonia, with her family being of Albanian descent originating in Kosovo.";
        interpreter.interpret(input).get(0);
        input = "Mother Teresa founded the Missionaries of Charity, a Roman Catholic religious congregation, which in 2012 consisted of over 4500 sisters and is active in 133 countries.";
        interpreter.interpret(input).get(0);
        input = "They run hospices and homes for people with HIV/AIDS, leprosy and tuberculosis; soup kitchens; dispensaries and mobile clinics; children's and family counselling programmes; orphanages; and schools.";
        interpreter.interpret(input).get(0);
        input = "Members must adhere to the vows of chastity, poverty and obedience as well as a fourth vow, to give \"wholehearted free service to the poorest of the poor\".";
        interpreter.interpret(input).get(0);
        input = "Mother Teresa was the recipient of numerous honours including the 1979 Nobel Peace Prize.";
        interpreter.interpret(input).get(0);
        input = "In 2003, she was beatified as \"\"Blessed Teresa of Calcutta\"\".";
        interpreter.interpret(input).get(0);
        input = "A second miracle credited to her intercession is required before she can be recognised as a saint by the Catholic Church.";
        interpreter.interpret(input).get(0);

        input = "Who founded the Missionaries of Charity?";
        String actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("'Mother Teresa'.", actualAnswer);

        input = "Who received the Nobel Peace Prize in 1979?";
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("'Mother Teresa'.", actualAnswer);

        input = "Where was Mother Teresa born?";
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("Macedonia.", actualAnswer);
    }
}
