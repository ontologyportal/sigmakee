package com.articulate.sigma.semRewrite;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KBmanager;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RulesAboutToCorpusTest {

    public static Interpreter interpreter;

    @BeforeClass
    public static void initInterpreter() {
        interpreter = new Interpreter();
        KBmanager.getMgr().initializeOnce();
        interpreter.loadRules();
    }

    @Test
    public void testVanishedWhileTryingToFly() {
        String input = "Earhart vanished over the South Pacific Ocean in July 1937 while trying to fly around the world.";

        String expectedKifString = "(exists (?vanish-3 ?AmeliaEarhart-1 ?vanished-3 ?try-13 ?fly-15 ?world-18)\n" +
                "(and\n" +
                "  (traverses ?fly-15 ?world-18)\n" +
                "  (equal\n" +
                "  (WhenFn ?vanish-3)\n" +
                "  (WhenFn ?try-13))\n" +
                "  (and\n" +
                "  (instance ?try-13 IntentionalProcess)\n" +
                "  (hasPurpose ?try-13 ?fly-15))\n" +
                "  (time ?vanished-3\n" +
                "  (MonthFn July\n" +
                "    (YearFn 1937)))\n" +
                "  (agent ?vanish-3 ?AmeliaEarhart-1)\n" +
                "  (instance ?fly-15 Flying)\n" +
                "  (names ?AmeliaEarhart-1 \"Amelia Earhart\")\n" +
                "  (instance ?vanish-3 Disappearing)\n" +
                "  (instance ?AmeliaEarhart-1 Woman))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

}
