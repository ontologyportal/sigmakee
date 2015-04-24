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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

import static com.articulate.sigma.semRewrite.substitutor.CoreLabelSequence.EMPTY_SEQUENCE;
import static com.articulate.sigma.semRewrite.substitutor.CoreLabelSequence.IGNORE_SENTENCE;
import static com.articulate.sigma.semRewrite.substitutor.SubstitutionUtil.CLAUSE_PARAM;

/** **************************************************************
 * Basic implementation of ClauseSubstitutor storage. This implementation store
 * the data as a map of CoreLabelSequences.
 */
public class SimpleSubstitutorStorage implements ClauseSubstitutor {

    Map<CoreLabelSequence, CoreLabelSequence> groups = Maps.newHashMap();

    public void addGroups(Map<CoreLabelSequence, CoreLabelSequence> groups) {

        this.groups.putAll(groups);
    }

    /** ***************************************************************
     */
    @Override
    public boolean containsKey(CoreLabel keyLabel) {

        Preconditions.checkNotNull(groups);
        return groups.keySet().stream().anyMatch(key -> key.containsLabel(keyLabel));
    }

    @Override
    public boolean containsKey(String keyLabel) {

        Preconditions.checkNotNull(groups);
        Matcher m = CLAUSE_PARAM.matcher(keyLabel);
        if(m.find()) {
            String text = m.group(1);
            Integer index = Integer.valueOf(m.group(2));
            return groups.keySet().stream()
                    .anyMatch(key -> key.containsLabel(IGNORE_SENTENCE, text, index));
        } else {
            return false;
        }
    }

    @Override
    public Optional<CoreLabelSequence> getGroupedByFirstLabel(CoreLabel label) {

        Preconditions.checkNotNull(groups);
        return groups.entrySet().stream()
                .filter(entry -> {
                    List<CoreLabel> labels = entry.getKey().getLabels();
                    return !labels.isEmpty() && labels.get(0).equals(label);
                })
                .map(entry -> entry.getValue())
                .findFirst();
    }

    /** ***************************************************************
     */
    @Override
    public CoreLabelSequence getGrouped(CoreLabel key) {

        return groups.entrySet().stream()
                .filter(e -> e.getKey().containsLabel(key))
                .map(e -> e.getValue())
                .findFirst().orElse(EMPTY_SEQUENCE);
    }

    @Override
    public CoreLabelSequence getGrouped(String keyLabel) {
        Matcher m = CLAUSE_PARAM.matcher(keyLabel);
        if(m.find()) {
            String text = m.group(1);
            Integer index = Integer.valueOf(m.group(2));
            return groups.entrySet().stream()
                    .filter(e -> e.getKey().containsLabel(IGNORE_SENTENCE, text, index))
                    .map(e -> e.getValue())
                    .findFirst().orElse(EMPTY_SEQUENCE);
        } else {
            return EMPTY_SEQUENCE;
        }
    }

}
