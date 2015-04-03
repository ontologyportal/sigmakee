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

import static edu.stanford.nlp.util.StringUtils.capitalize;

public class IdiomSubstitutor extends SimpleSubstitutorStorage {

    public IdiomSubstitutor(List<CoreLabel> labels) {

         ArrayList<String> clauses = Lists.newArrayList(
                 labels.stream()
                         .map(label -> label.originalText()).toArray(String[]::new)
         );
        initialize(clauses);
    }

    private void initialize(ArrayList<String> labels) {
        int from = 0;
        Map<String, String> collectedIdioms = Maps.newHashMap();
        ArrayList<String> synset = Lists.newArrayList();
        while (from < labels.size()) {
            int to = WordNet.wn.collectMultiWord(labels, from, synset);
            if (to > from) {
                String idiomValue = buildIdiom(labels, from, to);
                for(int i = from; i < to; i++) {
                    String key = labels.get(i) + "-" + (i+1);
                    collectedIdioms.put(key, idiomValue);
                }
            }
            from = to + 1;
        }

        setGroups(collectedIdioms);
    }

    private String buildIdiom(List<String> labels, int startIndex, int endIndex) {
        StringBuilder idiom = new StringBuilder(labels.get(startIndex));
        for(int i = startIndex + 1; i < endIndex; i++) {
            String label = capitalize(labels.get(i));
            idiom.append(label);
        }
        idiom.append('-').append(startIndex+1);

        return idiom.toString();
    }
}
