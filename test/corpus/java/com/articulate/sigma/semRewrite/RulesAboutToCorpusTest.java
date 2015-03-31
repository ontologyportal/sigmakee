/*
Copyright 2014-2015 IPsoft

Author: Sofia Athenikos sofia.athenikos@ipsoft.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA
*/
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
