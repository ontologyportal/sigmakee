/*
Copyright 2014-2015 IPsoft

Author: Andrei Holub andrei.holub@ipsoft.com

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
package com.articulate.sigma.semRewrite.substitutor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NounSubstitutor extends SimpleSubstitutorStorage {


    /** **************************************************************
     */
    public NounSubstitutor(List<CoreLabel> labels) {

        initialize(labels);
    }

    /** **************************************************************
     * Collects information about continuous noun sequences like "Garry Bloom", "Tim Buk Tu"
     */
    private void initialize(List<CoreLabel> labels) {

        Map<CoreLabelSequence, CoreLabelSequence> groupsFull = parseGroupsAndCollectRoots(labels);
        addGroups(groupsFull);
    }

    /** **************************************************************
     */
    private Map<CoreLabelSequence, CoreLabelSequence> parseGroupsAndCollectRoots(List<CoreLabel> labels) {

        System.out.println("Info in NounSubstitutor.parseGroupsAndCollectRoots(): " + labels);
        Map<CoreLabelSequence, CoreLabelSequence> sequences = Maps.newHashMap();
        CoreLabel firstLabel = null;
        List<CoreLabel> sequence = Lists.newArrayList();
        for (CoreLabel label : labels) {
            if (firstLabel != null
                    && ("NNP".equals(label.tag()) && Objects.equals(label.tag(), firstLabel.tag()))) {
                sequence.add(label);
            }
            else {
                if (sequence.size() > 1) {
                    CoreLabelSequence s = new CoreLabelSequence(sequence);
                    sequences.put(s, s);
                }
                firstLabel = label;
                sequence = Lists.newArrayList(firstLabel);
            }
        }
        if (sequence.size() > 1) {
            CoreLabelSequence s = new CoreLabelSequence(sequence);
            sequences.put(s, s);
        }

        return sequences;
    }

}
