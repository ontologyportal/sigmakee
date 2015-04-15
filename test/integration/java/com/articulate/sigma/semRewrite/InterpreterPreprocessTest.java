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

import com.articulate.sigma.IntegrationTestBase;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class InterpreterPreprocessTest extends IntegrationTestBase {

    Interpreter i;

    @Before
    public void init() {
        i = new Interpreter();
        i.initialize();
    }

    /** *************************************************************
     */
    @Test
    public void testNoNullPointerException() {

        i.interpret("I have a car. It is green.");
        i.interpret("I have another car.");
    }

    /** *************************************************************
     */
    @Test
    public void testDocumentBuilder() {

        i.interpret("I have a car. Who has a car? It is green.");
        i.interpret("I have another car.");
        i.interpret("What is the color of the car?");

        assertEquals(3, i.getUserInputs().size());

        assertThat(i.getUserInputs(), hasItem("I have a car."));
        assertThat(i.getUserInputs(), hasItem("It is green."));
        assertThat(i.getUserInputs(), hasItem("I have another car."));
    }

    /** *************************************************************
     */
    @Test
    public void testAmelia() {

        i.interpret("Amelia Mary Earhart (July 24, 1897 – July 2, 1937) was an American aviator. She was the first woman to fly a plane by herself across the Atlantic Ocean.");

        assertEquals(2, i.getUserInputs().size());

        String expected = "Amelia Mary Earhart (July 24, 1897 – July 2, 1937) was an American aviator.";
        assertThat(i.getUserInputs(), hasItem(expected));

        expected = "She was the first woman to fly a plane by herself across the Atlantic Ocean.";
        assertThat(i.getUserInputs(), hasItem(expected));
    }

    /** *************************************************************
     */
    @Test
    public void testReflexive() {

        i.interpret("Aimee went to the store. She laughed to herself.");

        String expected = "She laughed to herself.";
        assertThat(i.getUserInputs(), hasItem(expected));
    }
}
