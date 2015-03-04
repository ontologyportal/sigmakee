package com.articulate.sigma.nlg;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class NLGStringUtilsTest {

    @Test
    public void testConcatenateNoInput()   {
        String expected = "";
        String actual = NLGStringUtils.concatenateWithCommas(Lists.<String>newArrayList());
        assertEquals(expected, actual);

        expected = "";
        actual = NLGStringUtils.concatenateWithCommas(null);
        assertEquals(expected, actual);
    }

    @Test
    public void testOneItem()   {
        String expected = "one";
        String actual = NLGStringUtils.concatenateWithCommas(Lists.newArrayList("one"));
        assertEquals(expected, actual);
    }

    @Test
    public void testTwoItems()   {
        String expected = "one and two";
        String actual = NLGStringUtils.concatenateWithCommas(Lists.newArrayList("one", "two"));
        assertEquals(expected, actual);
    }

    @Test
    public void testThreeItems()   {
        String expected = "one, two and three";
        String actual = NLGStringUtils.concatenateWithCommas(Lists.newArrayList("one", "two", "three"));
        assertEquals(expected, actual);
    }

    @Test
    public void testSixItems()   {
        String expected = "one, two, three, four, five and six";
        String actual = NLGStringUtils.concatenateWithCommas(Lists.newArrayList("one", "two", "three", "four", "five", "six"));
        assertEquals(expected, actual);
    }
}
