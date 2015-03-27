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

public class RulesAboutOnCorpusTest {

    public static Interpreter interpreter;

    @BeforeClass
    public static void initInterpreter() {
        interpreter = new Interpreter();
        KBmanager.getMgr().initializeOnce();
        interpreter.loadRules();
    }

    @Test
    public void testExplanationOfEarhartsAndNoonansFate() {
        String input = "This is the most widely accepted explanation of Earhart's and Noonan's fate.";

        String expectedKifString = "(exists (?accepted-6 ?most-4 ?Noonan-12 ?fate-14 ?explanation-7 ?This-1 ?Earhart-9 ?widely-5)\n" +
                "(and\n" +
                "  (and\n" +
                "  (refers ?explanation-7 ?fate-14)\n" +
                "  (refers ?fate-14 ?Earhart-9)\n" +
                "  (refers ?fate-14 ?Noonan-12))\n" +
                "  (agent ?explanation-7 ?This-1)\n" +
                "  (attribute ?explanation-7 believes)\n" +
                "  (instance ?widely-5 SubjectiveAssessmentAttribute)\n" +
                "  (names ?Noonan-12 \"Noonan\")\n" +
                "  (attribute ?accepted-6 ?most-4)\n" +
                "  (instance ?fate-14 SubjectiveAssessmentAttribute)\n" +
                "  (names ?Earhart-9 \"Earhart\")\n" +
                "  (attribute ?accepted-6 ?widely-5)\n" +
                "  (instance ?Noonan-12 Human)\n" +
                "  (instance ?Earhart-9 Human))\n" +
                ")";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

}
