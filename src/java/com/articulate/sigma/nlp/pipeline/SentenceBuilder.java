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

package com.articulate.sigma.nlp.pipeline;

import com.articulate.sigma.semRewrite.substitutor.ClauseSubstitutor;
import com.articulate.sigma.semRewrite.substitutor.CoreLabelSequence;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class SentenceBuilder {

    public static Function<CoreLabel, String> NO_MUTATION = label -> label.originalText();

    private final List<CoreMap> sentences;

    public SentenceBuilder(Annotation document) {

        this.sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
    }

    public SentenceBuilder(CoreMap sentence) {

        this.sentences = ImmutableList.of(sentence);
    }

    /** ************************************************************
     */
    private boolean needSpaceBefore(CoreLabel label) {

        return needSpaceBefore(label.originalText());
    }

    private boolean needSpaceBefore(String text) {

        boolean skipSpace = text.length() == 1 && (
                ",".equals(text)
                        || ".".equals(text)
                        || ")".equals(text)
                        || "!".equals(text)
                        || "?".equals(text)
        );
        skipSpace |= text.isEmpty();
        return !skipSpace;
    }

    public List<String> asStrings() {

        return asStrings(NO_MUTATION);
    }

    public List<String> asStrings(ClauseSubstitutor substitutor) {
        return asStrings(label -> {
            if(substitutor.containsKey(label)) {
                // Replace only first element for complex keys
                Optional<CoreLabelSequence> grouped = substitutor.getGroupedByFirstLabel(label);
                return grouped.isPresent() ? grouped.get().toText() : "";
            }
            return label.originalText();
        });
    }

    public List<String> asStrings(Function<CoreLabel, String> onLabel) {

        List<String> sentences = Lists.newArrayList();
        for (CoreMap sentence : this.sentences) {
            StringBuilder builder = new StringBuilder();
            for (CoreLabel label : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String text = onLabel.apply(label);
                if (builder.length() != 0 && needSpaceBefore(text)) {
                    builder.append(" ");
                }
                builder.append(text);
                if ("PRP$".equals(label.tag())) {
                    builder.append(text.endsWith("s") ? "'" : "'s");
                }
            }
            sentences.add(builder.toString());
        }
        return sentences;

    }
}
