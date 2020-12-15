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

import com.articulate.sigma.utils.StringUtil;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MultiWords implements Serializable {

    /** A Multimap of String keys and String values.
     * The String key is the first word of a multi-word WordNet "word", such as "table_tennis",
     * where words are separated by underscores.  The values are
     * the whole multi-word. The same head word can appear in many multi-words.*/
    public Multimap<String, String> multiWord = HashMultimap.create();

    public static boolean debug = false;

    /** ***************************************************************
     * Add a multi-word string to the multiWord member variable.  Convert
     * the wordDelimit to underscores
     */
    public void addMultiWord(String word, char wordDelimit) {

        if (debug) System.out.println("INFO in WordNet.addMultiWord(): word: " + word);
        if (StringUtil.emptyString(word)) {
            System.out.println("Error in MultiWords.addMultiWord(): word is null");
            return;
        }
        if (word.indexOf(wordDelimit) >= 0) {
            String firstWord = word.substring(0, word.indexOf(wordDelimit));
            String newWord = word.replace(wordDelimit,'_');
            multiWord.put(firstWord, newWord);
        }
        else {
            System.out.println("Error in MultiWords.addMultiWord(): Not a multi-word: " + word);
            Thread.dumpStack();
        }
    }

    /** ***************************************************************
     * Add a multi-word string to the multiWord member variable.
     */
    public void addMultiWord(String word) {

        addMultiWord(word,'_');
    }

    /** ***************************************************************
     */
    public String findMultiWord(List<String> text) {

        List<String> synset = new ArrayList<>();
        int endIndex = findMultiWord(text, 0, synset);
        StringBuffer sb = new StringBuffer();
        if (endIndex != 0) {
            for (int i = 0; i < endIndex; i++) {
                if (i != 0)
                    sb.append("_");
                sb.append(text.get(i));
            }
        }
        return sb.toString();
    }

    /** ***************************************************************
     * Find the synset for a multi-word string, if it exists.
     *
     * @param text is an array of String words.
     * @param startIndex is the first word in the array to look at
     * @param synset is an array of only one element, if a synset is found
     * and empty otherwise
     * @return the index into the next word to be checked, in text,
     * which could be the same as startIndex, if no multi-word was found
     */
    public int findMultiWord(List<String> text, int startIndex, List<String> synset) {

        //System.out.println("INFO in MultiWords.findMultiWord(): text: '" + text + "'");
        String rootWord = rootFormOf(text.get(startIndex));
        return startIndex + findMultiWord(rootWord, text.get(startIndex),
                text.subList(startIndex + 1, text.size()), synset);
    }

    /** ***************************************************************
     * @param nonRoot is the non root form of the potential multiword headword.
     *                We need to try both the root form and the original form,
     *                which includes capitalized and lower case versions.
     */
    public int findMultiWord(String multiWordKey, String nonRoot, List<String> multiWordTail, List<String> synset) {

        if (!multiWord.containsKey(multiWordKey))
            multiWordKey = nonRoot;
        int wordIndex = 0;
        if (multiWord.containsKey(multiWordKey) && !multiWordTail.isEmpty()) {
            String foundMultiWord = multiWordKey + "_" + multiWordTail.get(wordIndex);
            Collection<String> candidates = multiWord.get(multiWordKey);
            while (candidates.size() > 0) {
                ArrayList<String> newCandidates = new ArrayList<String>();
                for (String candidate : candidates) {
                    if (candidate.equals(foundMultiWord)) {
                        String sense = WSD.getBestDefaultSense(foundMultiWord);
                        if (!StringUtil.emptyString(sense)) { // only declare success if the multiword has a synset (trapping errors in the DB)
                            synset.add(sense);
                            return wordIndex + 2;
                        }
                    }
                    else if (candidate.startsWith(foundMultiWord)) {
                        newCandidates.add(candidate);
                    }
                }
                if (newCandidates.size() > 0) {
                    if (wordIndex > multiWordTail.size() - 1) {
                        candidates = new ArrayList<String>();  // ran out of words, trigger an exit
                    }
                    else {
                        candidates = newCandidates;
                        wordIndex++;
                        if (wordIndex < multiWordTail.size())
                            foundMultiWord = foundMultiWord + "_" + multiWordTail.get(wordIndex);
                    }
                }
                else {
                    candidates = new ArrayList<String>();
                }
            }
        }
        return 0;
    }

    /** ***************************************************************
     */
    public static String rootFormOf(String word) {

        String rootWord = word;
        String nounroot = WordNet.wn.nounRootForm(word, word.toLowerCase());
        String verbroot = WordNet.wn.verbRootForm(word, word.toLowerCase());
        if (!Strings.isNullOrEmpty(nounroot) && !nounroot.equals(word))
            rootWord = nounroot;
        else if (!Strings.isNullOrEmpty(verbroot) && !verbroot.equals(word)) {
            rootWord = verbroot;
        }
        return rootWord;
    }
}
