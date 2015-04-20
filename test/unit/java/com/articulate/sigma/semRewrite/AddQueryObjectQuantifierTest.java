package com.articulate.sigma.semRewrite;

import com.articulate.sigma.Formula;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/*
Copyright 2014-2015 IPsoft

Author: Peigen You Peigen.You@ipsoft.com

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
public class AddQueryObjectQuantifierTest {

    String input;
    String output;
    Method findQueryObjects = null;
    Interpreter interpreter = null;

    public AddQueryObjectQuantifierTest() {

        interpreter = new Interpreter();
        interpreter.question = true;
    }

    /***********************************************************
     */
    @Test
    public void testAddQueryObjectQuantifierTest1() {

        input = "(and\n" +
                "    (instance ?WH Human)\n" +
                "    (agent ?moves-2 ?WH)\n" +
                "    (instance ?cart-4 Wagon)\n" +
                "    (patient ?moves-2 ?cart-4)\n" +
                "    (instance ?moves-2 Transportation))";
        output = "(exists (?WH)\n" +
                "  (forall (?DUMMY)\n" +
                "    (exists (?moves-2 ?cart-4)\n" +
                "    (and\n" +
                "      (instance ?WH Human)\n" +
                "      (agent ?moves-2 ?WH)\n" +
                "      (instance ?cart-4 Wagon)\n" +
                "      (patient ?moves-2 ?cart-4)\n" +
                "      (instance ?moves-2 Transportation)) )))";
        Assert.assertTrue(compareInputResultAndExpectedResult(input, output));
    }

    /***********************************************************
     */
    @Test
    public void testAddQueryObjectQuantifierTest2() {

        input = "(and\n" +
                "    (agent ?fly-4 ?Amelia-3)\n" +
                "    (instance ?fly-4 Flying)\n" +
                "    (names ?Amelia-3 \"Amelia\")\n" +
                "    (patient ?fly-4 ?What-1)\n" +
                "    (instance ?Amelia-3 Human)) ";
        output = "(exists (?What-1)\n" +
                "  (forall (?DUMMY)\n" +
                "  (exists (?Amelia-3 ?fly-4)\n" +
                "    (and\n" +
                "      (agent ?fly-4 ?Amelia-3)\n" +
                "      (instance ?fly-4 Flying)\n" +
                "      (names ?Amelia-3 \"Amelia\")\n" +
                "      (patient ?fly-4 ?What-1)\n" +
                "      (instance ?Amelia-3 Human)) )))";
        Assert.assertTrue(compareInputResultAndExpectedResult(input, output));
    }

    /***********************************************************
     */
    @Test
    public void testAddQueryObjectQuantifierTest3() {

        input = "  (and\n" +
                "    (agent ?go-4 ?Amelia-3)\n" +
                "    (attribute ?go-4 ?Where-1)\n" +
                "    (instance ?did-2 IntentionalProcess)\n" +
                "    (names ?Amelia-3 \"Amelia\")\n" +
                "    (instance ?Amelia-3 DiseaseOrSyndrome)) ";
        output = "(exists (?Where-1)\n" +
                "  (forall (?DUMMY)\n" +
                "  (exists (?go-4 ?did-2 ?Amelia-3)\n" +
                "    (and\n" +
                "      (agent ?go-4 ?Amelia-3)\n" +
                "      (attribute ?go-4 ?Where-1)\n" +
                "      (instance ?did-2 IntentionalProcess)\n" +
                "      (names ?Amelia-3 \"Amelia\")\n" +
                "      (instance ?Amelia-3 DiseaseOrSyndrome)) )))";
        Assert.assertTrue(compareInputResultAndExpectedResult(input, output));
    }

    /***********************************************************
     */
    @Test
    public void testAddQueryObjectQuantifierTest4() {

        input = "  (and\n" +
                "    (agent ?asked-2 ?John-1)\n" +
                "    (instance ?car-7 Automobile)\n" +
                "    (names ?John-1 \"John\")\n" +
                "    (patient ?asked-2 ?me-3)\n" +
                "    (agent ?had-5 ?who-4)\n" +
                "    (instance ?John-1 Bathroom)\n" +
                "    (patient ?had-5 ?car-7)\n" +
                "    (instance ?asked-2 Questioning)) ";
        output = "(exists (?who-4)\n" +
                "  (forall (?DUMMY)\n" +
                "  (exists (?John-1 ?me-3 ?asked-2 ?had-5 ?car-7)\n" +
                "    (and\n" +
                "      (agent ?asked-2 ?John-1)\n" +
                "      (instance ?car-7 Automobile)\n" +
                "      (names ?John-1 \"John\")\n" +
                "      (patient ?asked-2 ?me-3)\n" +
                "      (agent ?had-5 ?who-4)\n" +
                "      (instance ?John-1 Bathroom)\n" +
                "      (patient ?had-5 ?car-7)\n" +
                "      (instance ?asked-2 Questioning)) )))";
        Assert.assertTrue(compareInputResultAndExpectedResult(input, output));
    }

    /***********************************************************
     */
    @Test
    public void testAddQueryObjectQuantifierTest5() {

        input = "  (and\n" +
                "    (agent ?feel-4 ?you-3)\n" +
                "    (attribute ?feel-4 ?How-1)\n" +
                "    (instance ?feel-4 EmotionalState)\n" +
                "    (instance ?food-7 PreparedFood)) ";
        output = "(exists (?How-1)\n" +
                "  (forall (?DUMMY)\n" +
                "    (exists (?food-7 ?you-3 ?feel-4)\n" +
                "    (and\n" +
                "      (agent ?feel-4 ?you-3)\n" +
                "      (attribute ?feel-4 ?How-1)\n" +
                "      (instance ?feel-4 EmotionalState)\n" +
                "      (instance ?food-7 PreparedFood)) )))";
        Assert.assertTrue(compareInputResultAndExpectedResult(input, output));
    }

    /***********************************************************
     */
    @Test
    public void testAddQueryObjectSingleCharVariable() {

        input = "  (and\n" +
                "    (attribute ?John-3 Male)\n" +
                "    (inhabits ?John-3 ?Y)\n" +
                "    (names ?John-3 \"John\"))";
        output = "(exists (?Y)\n" +
                "  (forall (?DUMMY)\n" +
                "    (exists (?John-3)\n" +
                "      (and\n" +
                "        (attribute ?John-3 Male)\n" +
                "        (inhabits ?John-3 ?Y)\n" +
                "        (names ?John-3 \"John\")) )))";
        Assert.assertTrue(compareInputResultAndExpectedResult(input, output));
    }

    /***********************************************************
     */
    private boolean compareInputResultAndExpectedResult(String input, String output) {

        Formula f1 = new Formula();
        input = String.valueOf(interpreter.addQuantification(input));
        f1.read(input);
        Formula f2 = new Formula();
        f2.read(output);
        return f1.equals(f2);
    }
}
