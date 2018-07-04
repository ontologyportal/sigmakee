/*
 * Copyright 2014-2015 IPsoft
 *
 * Author: Andrei Holub andrei.holub@ipsoft.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program ; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA  02111-1307 USA
 */
package com.articulate.sigma.wordNet;

import com.articulate.sigma.UnitTestBase;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MultiWordsTest extends UnitTestBase {

    /** ***************************************************************
     */
    @Test
    public void testVerbMultiWordKickBucket1() {

        List<String> input = Lists.newArrayList("kick", "the", "bucket");
        List<String> synset = Lists.newArrayList();
        String w = WordNet.wn.getMultiWords().findMultiWord(input);
        String s = WordNetUtilities.wordsToSynsets(w).iterator().next();
        assertEquals("200358431", s);
    }

    /** ***************************************************************
     */
    @Test
    public void testVerbMultiWordKickBucket2() {

        List<String> input = Lists.newArrayList("kick", "the", "bucket", "down", "the", "road");
        List<String> synset = Lists.newArrayList();
        String w = WordNet.wn.getMultiWords().findMultiWord(input);
        String s = WordNetUtilities.wordsToSynsets(w).iterator().next();
        assertEquals("200358431", s);
    }

    /** ***************************************************************
     */
    @Test
    public void testVerbMultiWordCatsAndDogs1() {

        List<String> input = Lists.newArrayList("many", "raining", "cats", "and", "dogs", "and", "sheep");
        List<String> synset = Lists.newArrayList();
        int endIndex = WordNet.wn.getMultiWords().findMultiWord(input, 0, synset);

        assertEquals(0, endIndex);
        assertEquals(0, synset.size());
    }

    /** ***************************************************************
     */
    @Test
    public void testVerbMultiWordCatsAndDogs2() {

        List<String> input = Lists.newArrayList("many", "raining", "cats", "and", "dogs", "and", "sheep");
        List<String> synset = Lists.newArrayList();
        int endIndex = WordNet.wn.getMultiWords().findMultiWord(input, 1, synset);

        assertEquals(5, endIndex);
        assertEquals(1, synset.size());
    }

    /** ***************************************************************
     */
    @Test
    public void testVerbMultiWordCatsAndDogs3() {

        ArrayList<String> input = Lists.newArrayList("cats", "and", "dogs", "and", "sheep");
        List<String> synset = Lists.newArrayList();
        int endIndex = WordNet.wn.getMultiWords().findMultiWord("rain", "rain", input, synset);

        assertEquals(4, endIndex);
        assertEquals(1, synset.size());
        assertEquals("202758033", synset.get(0));
    }

    /** ***************************************************************
     */
    @Test
    public void testVerbMultiWordCatsAndDogs4() {

        ArrayList<String> input = Lists.newArrayList("cats", "and", "dogs", "and", "sheep");
        List<String> synset = Lists.newArrayList();
        // Incorrect root form
        int endIndex = WordNet.wn.getMultiWords().findMultiWord("raining", "raining", input, synset);

        assertEquals(0, endIndex);
        assertEquals(0, synset.size());
    }

    /** ***************************************************************
     */
    @Test
    public void testVerbMultiWordCatsAndDogs5() {

        ArrayList<String> input = Lists.newArrayList("raining", "cats", "and", "dogs", "and", "sheep");
        List<String> synset = Lists.newArrayList();
        // Incorrect root form
        String result = WordNet.wn.getMultiWords().findMultiWord(input);

        assertEquals("raining_cats_and_dogs",result);
        //WordNet.wn.getSUMO
    }

    /** ***************************************************************
     */
    @Test
    public void testNounMultiWord1() {

        ArrayList<String> input = Lists.newArrayList("father");
        ArrayList<String> synset = Lists.newArrayList();
        // Incorrect root form
        int endIndex = WordNet.wn.getMultiWords().findMultiWord("found", "found", input, synset);

        assertEquals(0, endIndex);
        assertEquals(0, synset.size());
    }

    /** ***************************************************************
     */
    @Test
    public void testNounMultiWord2() {

        ArrayList<String> input = Lists.newArrayList("father");
        ArrayList<String> synset = Lists.newArrayList();
        int endIndex = WordNet.wn.getMultiWords().findMultiWord("founding", "founding", input, synset);

        assertEquals(2, endIndex);
        assertEquals(1, synset.size());
        assertEquals("110107303",synset.get(0));
    }
}