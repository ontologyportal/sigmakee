package com.articulate.sigma.semRewrite;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KBmanager;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RulesAboutAroundCorpusTest {

    public static Interpreter interpreter;

    @BeforeClass
    public static void initInterpreter() {
        interpreter = new Interpreter();
        KBmanager.getMgr().initializeOnce();
        interpreter.loadRules();
    }

    @Test
    public void testAroundTheWorld() {
        String input = "She was trying to fly around the world.";

        String expectedKifString = "((exists (?trying-3 ?Earhart-1 ?world-8 ?fly-5)\n" +
                "(and\n" +
                "  (traverses ?fly-5 ?world-8)\n" +
                "  (agent ?trying-3 ?Earhart-1)\n" +
                "  (instance ?trying-3 IntentionalProcess)\n" +
                "  (names ?Earhart-1 \"Earhart\")\n" +
                "  (instance ?Earhart-1 Human)\n" +
                "  (instance ?fly-5 Flying))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

}
