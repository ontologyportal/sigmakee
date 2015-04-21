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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.stanford.nlp.dcoref.CorefCoreAnnotations.*;

public class CorefSubstitutor extends SimpleSubstitutorStorage {

    public static final Set<String> ignorablePronouns = ImmutableSet.of("himself", "herself", "Noonan");

    public CorefSubstitutor(Annotation document) {

        initialize(document);
    }

    private void initialize(Annotation document) {
        List<CoreLabel> labels = document.get(TokensAnnotation.class);
        Map<Integer, CorefChain> corefChains = document.get(CorefChainAnnotation.class);

        Map<CoreLabelSequence, CoreLabelSequence> collectedGroups = Maps.newHashMap();

        for(CoreLabel label : labels) {
            List<CorefMention> mentions = getMentions(label, corefChains);
            if (mentions.size() > 1) {
                if (!ignorablePronouns.contains(label.originalText())) {
                    int index = label.index();
                    int sentenceIdx = 1 + label.sentIndex();

                    CorefMention firstMention = findRootMention(mentions);
                    if (sentenceIdx != firstMention.sentNum || index < firstMention.startIndex || index >= firstMention.endIndex) {
                        String masterTag = label.tag();
                        if (isSubstitutablePronoun(label)) {
                            masterTag = "";
                        }
                        List<CoreLabel> singleSentence =  getSentenceTokens(document, firstMention.sentNum - 1);
                        CoreLabelSequence key = extractTextWithSameTag(singleSentence, firstMention, masterTag);
                        if(!key.isEmpty()) {
                            collectedGroups.put(new CoreLabelSequence(label), key);
                        }
                    }
                }

            }
        }

        addGroups(collectedGroups);
    }

    private CorefMention findRootMention(List<CorefMention> mentions) {
        // Assuming first mention is the replacement
        CorefMention rootMention = mentions.get(0);

        return rootMention;
    }

    /** *************************************************************
     */
    private List<CorefMention> getMentions(final CoreLabel label, Map<Integer, CorefChain> corefs) {

        List<CorefMention> mentions = ImmutableList.of();
        Integer corefClusterId = label.get(CorefClusterIdAnnotation.class);
        while(mentions.size() <= 1 && corefClusterId != null && corefClusterId.compareTo(0) > 0) {
            if(corefs.containsKey(corefClusterId)) {
                List<CorefMention> candidateMentions = corefs.get(corefClusterId).getMentionsInTextualOrder();
                boolean areMentionsContainLabel = candidateMentions.stream().anyMatch(mention ->
                                mention.sentNum == label.sentIndex() + 1
                                        && mention.startIndex == label.index()
                );
                if(areMentionsContainLabel) {
                    mentions = candidateMentions;
                }
            }
            corefClusterId = corefClusterId - 1;
        }

        return mentions;
    }

    private boolean isSubstitutablePronoun(CoreLabel label) {

        String tag = label.tag();
        return "PRP".equals(tag) || "PRP$".equals(tag);
    }

    /** *************************************************************
     */
    private List<CoreLabel> getSentenceTokens(Annotation document, int sentenceNumber) {

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        CoreMap sentence = sentences.get(sentenceNumber);
        List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
        return tokens;
    }

    /** *************************************************************
     */
    private CoreLabelSequence extractTextWithSameTag(List<CoreLabel> tokens, CorefMention mention, /* @Nullable */String masterTag) {

        List<CoreLabel> out = Lists.newArrayListWithCapacity(mention.endIndex - mention.startIndex);
        String tag = Strings.nullToEmpty(masterTag);
        for (int i = mention.startIndex; i < mention.endIndex; i++) {
            if (Strings.isNullOrEmpty(tag) || "DT".equals(tag)) {
                tag = tokens.get(i - 1).tag();
            }
            CoreLabel coreLabel = tokens.get(i - 1);
            if (tag.equals(coreLabel.tag())) {
                out.add(coreLabel);
            }
            else {
                break;
            }
        }
        return new CoreLabelSequence(out);
    }
}
