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

package com.articulate.sigma.semRewrite.substitutor;

import com.articulate.sigma.WordNet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IdiomSubstitutor extends SimpleSubstitutorStorage {

    public IdiomSubstitutor(List<CoreLabel> labels) {

        initialize(labels);
    }

    /** ***************************************************************
     * Looking for idioms in the labels sequence
     * E.g. "raining cats and dogs"
     */
    private void initialize(List<CoreLabel> labels) {

        System.out.println("Info in IdiomSubstitutor.initialize(): " + labels);
        int from = 0;
        Map<CoreLabelSequence, CoreLabelSequence> collectedIdioms = Maps.newHashMap();
        ArrayList<String> labelsText = Lists.newArrayList(labels.stream()
                        .map(label -> label.originalText()).toArray(String[]::new)
        );
        ArrayList<String> synset = Lists.newArrayList();
        while (from < labels.size()) {
            List<String> tail = labelsText.subList(from + 1, labelsText.size());
            int to = WordNet.wn.getMultiWords().findMultiWord(labels.get(from).lemma(), labels.get(from).originalText(), tail, synset);
            if (to > 0) {
                CoreLabelSequence idiom = new CoreLabelSequence(labels.subList(from, from + to));
                collectedIdioms.put(idiom, idiom);
            }
            from++;
        }
        addGroups(collectedIdioms);
    }
}
