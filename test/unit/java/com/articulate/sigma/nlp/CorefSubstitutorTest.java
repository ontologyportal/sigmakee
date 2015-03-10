package com.articulate.sigma.nlp;

import junit.framework.TestCase;

/**
 * Created by aholub on 3/9/15.
 */
public class CorefSubstitutorTest extends TestCase {

    public void testSubstitute() throws Exception {
        String input = "George Washington, sat on a wall. He had a great fall.";
        String output = CorefSubstitutor.substitute(input);
        assertEquals("George Washington, sat on a wall. George Washington had a great fall.", output);
    }
}