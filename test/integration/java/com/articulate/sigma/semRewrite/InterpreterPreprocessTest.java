/*
Copyright 2014-2015 IPsoft

Author: Adam Pease adam.pease@ipsoft.com

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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InterpreterPreprocessTest {


    /** *************************************************************
     */
    @Test
    public void testNoNullPointerException() {

        Interpreter i = new Interpreter();
        i.processInput("I have a car. It is green.");
        i.processInput("I have another car.");
    }

    /** *************************************************************
     */
    @Test
    public void testDocumentBuilder() {

        Interpreter i = new Interpreter();
        i.processInput("I have a car. It is green.");
        i.processInput("I have another car.");

        assertEquals(3, i.getUserInputs().size());

        String expected = "I have a car.";
        assertTrue("Should contain: " + expected + ", but was: " + i.getUserInputs(), i.getUserInputs().contains(expected));
        expected = "a car is green.";
        assertTrue("Should contain: " + expected + ", but was: " + i.getUserInputs(), i.getUserInputs().contains(expected));
        expected = "I have another car.";
        assertTrue("Should contain: " + expected + ", but was: " + i.getUserInputs(), i.getUserInputs().contains(expected));
    }

    /** *************************************************************
     */
    @Test
    public void testAmelia() {

        Interpreter i = new Interpreter();
        i.processInput("Amelia Mary Earhart (July 24, 1897 – July 2, 1937) was an American aviator. She was the first woman to fly a plane by herself across the Atlantic Ocean.");

        String expected = "Amelia Mary Earhart ( July 24, 1897 – July 2, 1937) was an American aviator.";

        assertEquals(2, i.getUserInputs().size());
        assertTrue("Should contain: " + expected + ", but was: " + i.getUserInputs(), i.getUserInputs().contains(expected));

        expected = "Amelia Mary Earhart was the first woman to fly a plane by herself across the Atlantic Ocean.";
        assertTrue("Should contain: " + expected + ", but was: " + i.getUserInputs(), i.getUserInputs().contains(expected));
    }

    /** *************************************************************
     */
    @Test
    public void testReflexive() {

        Interpreter i = new Interpreter();
        i.processInput("Aimee went to the store. She laughed to herself.");

        String expected = "Aimee laughed to herself.";

        assertTrue("Should contain: " + expected + ", but was: " + i.getUserInputs(), i.getUserInputs().contains(expected));
    }
}
