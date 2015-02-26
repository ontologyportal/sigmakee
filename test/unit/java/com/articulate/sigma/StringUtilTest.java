package com.articulate.sigma;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class StringUtilTest {
    @Test
    public void testFilterHtml()   {
        String input = "<ul><li>if for all <a href=\"&term=Entity\">an entity</a> <a href=\"&term=Entity\">the entity</a> is an <a href=\"&term=element\">element</a> of <a href=\"&term=Set\">a  set</a> if and only if <a href=\"&term=Entity\">the entity</a> is an <a href=\"&term=element\">element</a> of <a href=\"&term=Set\">another set</a>,</li><li>then <a href=\"&term=Set\">the set</a> is <a href=\"&term=equal\">equal</a> to <a href=\"&term=Set\">the other set</a></li></ul>";
        String actual = StringUtil.filterHtml(input);
        String expected = "if for all an entity the entity is an element of a set if and only if the entity is an element of another set, " +
                "then the set is equal to the other set";
        assertEquals(expected, actual);
    }

}
