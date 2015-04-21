package com.articulate.sigma.inference;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.semRewrite.Interpreter;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class DemoScript20150421_3Test extends IntegrationTestBase {

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

        String input = "Penicillin (sometimes abbreviated PCN or pen) is a group of antibiotics derived from Penicillium fungi, including penicillin G (intravenous use), penicillin V (oral use), procaine penicillin, and benzathine penicillin(intramuscular use).";
        interpreter.interpret(input).get(0);
        input = "Penicillin antibiotics were among the first drugs to be effective against many previously serious diseases, such as bacterial infections caused by staphylococci and streptococci.";
        interpreter.interpret(input).get(0);
        input = "Penicillins are still widely used today, though misuse has now made many types of bacteria resistant.";
        interpreter.interpret(input).get(0);
        input = "All penicillins are β lactam antibiotics and are used in the treatment of bacterial infections caused by susceptible, usually Gram positive, organisms.";
        interpreter.interpret(input).get(0);
        input = "Several enhanced penicillin families also exist, effective against additional bacteria: these include the antistaphylococcal penicillins, aminopenicillins and the more-powerful antipseudomonal penicillins.";
        interpreter.interpret(input).get(0);
        input = "Procaine penicillin and benzathine penicillin have the same antibacterial activity as benzylpenicillin but act for a longer period of time.";
        interpreter.interpret(input).get(0);
        input = "Phenoxymethylpenicillin is less active against gram-negative bacteria than benzylpenicillin.";
        interpreter.interpret(input).get(0);
        input = "Benzylpenicillin, procaine penicillin and benzathine penicillin are given by injection (parenterally), but phenoxymethylpenicillin is given orally.";
        interpreter.interpret(input).get(0);

        input = "Which antibiotic is given parenterally?";
        String actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("Penicillin.", actualAnswer);

        input = "Which antibiotic is given orally?";
        actualAnswer = interpreter.interpret(input).get(0);
        assertEquals("Phenoxymethylpenicillin.", actualAnswer);
    }
}
