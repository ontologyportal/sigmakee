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

public class RulesAboutFirstLastCorpusTest {

    public static Interpreter interpreter;

    @BeforeClass
    public static void initInterpreter() {
        interpreter = new Interpreter();
        KBmanager.getMgr().initializeOnce();
        interpreter.loadRules();
    }

    @Test
    public void testSheWasTheFirstWomanToFly() {
        String input = "She was the first woman to fly.";

        String expectedKifString = "(exists (?she-1 ?fly-7)\n" +
                "  (and\n" +
                "  (instance ?she-1 Woman)\n" +
                "  (instance ?fly-7 Flying)\n" +
                "  (agent ?fly-7 ?she-1)\n" +
                "  (not\n" +
                "    (exists (?she-12 ?fly-72)\n" +
                "      (and\n" +
                "        (instance ?she-12 Woman)\n" +
                "        (instance ?fly-72 Flying)\n" +
                "        (agent ?fly-72 ?she-12)\n" +
                "        (earlier\n" +
                "          (WhenFn ?fly-72)\n" +
                "          (WhenFn ?fly-7)))))))";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

}
