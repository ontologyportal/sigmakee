package com.articulate.sigma;

//This software is released under the GNU Public License
//<http://www.gnu.org/copyleft/gpl.html>.
// Copyright 2019 Infosys
// adam.pease@infosys.com

import com.articulate.sigma.utils.StringUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;

public class StringUtilTest {

    /** *****************************************************************
     */
    @Test
    public void testFilterHtml() {

        String input = "<ul><li>if for all <a href=\"&term=Entity\">an entity</a> <a href=\"&term=Entity\">the entity</a> is an <a href=\"&term=element\">element</a> of <a href=\"&term=Set\">a  set</a> if and only if <a href=\"&term=Entity\">the entity</a> is an <a href=\"&term=element\">element</a> of <a href=\"&term=Set\">another set</a>,</li><li>then <a href=\"&term=Set\">the set</a> is <a href=\"&term=equal\">equal</a> to <a href=\"&term=Set\">the other set</a></li></ul>";
        String actual = StringUtil.filterHtml(input);
        String expected = "if for all an entity the entity is an element of a set if and only if the entity is an element of another set, " +
                "then the set is equal to the other set";
        assertEquals(expected, actual);
    }

    /** *****************************************************************
     */
    @Test
    public void testIsInteger() {

        assertTrue(StringUtil.isInteger("53"));
        assertFalse(StringUtil.isInteger("53.0"));
    }

    /** *****************************************************************
     */
    @Test
    public void testIsNumeric() {

        assertTrue(StringUtil.isNumeric("53"));
        assertTrue(StringUtil.isNumeric("53.0"));
        assertFalse(StringUtil.isNumeric("Hello!"));
        assertTrue(StringUtil.isNumeric("0.000001"));
        assertTrue(StringUtil.isNumeric("1000000000"));
    }

    /** *****************************************************************
     */
    @Ignore
    @Test
    public void testRemoveEscapes() {

        assertEquals("  ",StringUtil.removeEscapes("\u0641\u0646\u062F\u0642 \u0648\u064A\u0633\u062A\u064A\u0646 "));
    }

    /** *****************************************************************
     */
    @Test
    public void testCamelCase() {

        assertEquals("HowToMarketToPeopleNotLikeYou",StringUtil.toCamelCase("\\n          \\n          How to Market to People Not Like You: \\"));

    }
}
