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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.List;
import java.util.Optional;

public class CoreLabelSequence {

    public static final CoreLabelSequence EMPTY_SEQUENCE = CoreLabelSequence.from();
    public static final int IGNORE_SENTENCE = Integer.MIN_VALUE;

    final List<CoreLabel> labels;

    public CoreLabelSequence(List<CoreLabel> labels) {

        this.labels = ImmutableList.copyOf(labels);
    }

    public CoreLabelSequence(CoreLabel... labels) {

        this.labels = ImmutableList.copyOf(labels);
    }

    public static CoreLabelSequence from(CoreLabel... labels) {

        return labels.length > 0
                ? new CoreLabelSequence(labels)
                : EMPTY_SEQUENCE;
    }

    public List<CoreLabel> getLabels() {

        return labels;
    }

    public boolean containsLabel(CoreLabel label) {

        return labels.contains(label);
    }

    public boolean isEmpty() {

        return labels.isEmpty();
    }

    /** *************************************************************
     * Checks if label is part of current sequence
     * @param sentIndex can be CoreLabelSequence.IGNORE_SENTENCE
     */
    public boolean containsLabel(int sentIndex, String text, int index) {

        return labels.stream().anyMatch(label ->
                        (sentIndex == label.sentIndex() || sentIndex == IGNORE_SENTENCE)
                                && text.equals(label.originalText())
                                && index == label.index()
        );
    }

    /** *************************************************************
     * Converts the sequence to text representation using "_" as a separator
     */
    public String toText() {

        return Joiner.on("_").join(labels.stream().map(label -> label.originalText()).toArray());
    }

    /** *************************************************************
     * Returns the sequence in the String format like "United_States-3"
     */
    public Optional<String> toLabelString() {

        if (!labels.isEmpty()) {
            String combinedIndex = "-" + labels.get(0).index();
            return Optional.of(toText() +  combinedIndex);
        }
        else {
            return Optional.empty();
        }
    }
}
