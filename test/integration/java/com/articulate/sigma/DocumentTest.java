/*
Copyright 2014-2015 IPsoft

Author: Andrew Reed andrew.reed@ipsoft.com

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
package com.articulate.sigma;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class DocumentTest {

    /** *************************************************************
     */
    @Test
    public void testTwoSentence() {

        Document document = new Document();
        document.addUtterance("George Washington was a president.");
        List<String> coreffedSentences = document.addUtterance("He chopped down a tree.");

        assertEquals(1, coreffedSentences.size());
        assertEquals("George_Washington chopped down a tree.", coreffedSentences.get(0));
    }

    /** *************************************************************
     */
    @Test
    public void testTwoSentenceSecondMulti() {

        Document document = new Document();
        document.addUtterance("George Washington was a president.");
        List<String> coreffedSentences = document.addUtterance("He chopped down a tree. He bought a house.");

        assertEquals(2, coreffedSentences.size());
        assertEquals(ImmutableList.of("George_Washington chopped down a tree.", "George_Washington bought a house."), coreffedSentences);
    }

    /** *************************************************************
     */
    @Test
    public void testThreeSentence() {

        Document document = new Document();
        List<String> coreffedSentences1 = document.addUtterance("George Washington was a president.");
        List<String> coreffedSentences2 = document.addUtterance("He chopped down a tree.");
        List<String> coreffedSentences3 = document.addUtterance("It was large.");

        assertEquals(1, coreffedSentences1.size());
        assertEquals(ImmutableList.of("George Washington was a president."), coreffedSentences1);

        assertEquals(1, coreffedSentences2.size());
        assertEquals(ImmutableList.of("George_Washington chopped down a tree."), coreffedSentences2);

        assertEquals(1, coreffedSentences3.size());
        assertEquals(ImmutableList.of("a_tree was large."), coreffedSentences3);
    }


}
