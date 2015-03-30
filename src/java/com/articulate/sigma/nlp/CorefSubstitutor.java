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
package com.articulate.sigma.nlp;

import com.articulate.sigma.nlp.pipeline.Pipeline;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefClusterIdAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CorefSubstitutor {

    final Annotation document;

    public static final Set<String> ignorablePronouns = ImmutableSet.of("himself", "herself", "Noonan");

    public CorefSubstitutor(Annotation document) {

        this.document = document;
    }

    public CorefSubstitutor(String input) {

        Pipeline pipeline = new Pipeline();
        this.document = pipeline.annotate(input);
    }

    /** *************************************************************
     */
    private boolean needSpaceBefore(CoreLabel label) {

        String text = label.originalText();
        boolean skipSpace = text.length() == 1 && (
                ",".equals(text)
                        || ".".equals(text)
                        || ")".equals(text)
                        || "!".equals(text)
                        || "?".equals(text)
        );
        return !skipSpace;
    }

    /** *************************************************************
     */
    private String replaceCoref(final CoreLabel label, Map<Integer, CorefChain> corefs) {

        String text = label.originalText();
        Integer corefClusterId = label.get(CorefClusterIdAnnotation.class);
        if (corefClusterId != null) {
            List<CorefMention> mentions = corefs.get(corefClusterId).getMentionsInTextualOrder();
            if (mentions.size() > 1) {
                int index = label.index();
                int sentence = 1 + label.sentIndex();
                CorefMention firstMention = mentions.get(0);
                if (sentence != firstMention.sentNum || index < firstMention.startIndex || index >= firstMention.endIndex) {
                    String masterTag = label.tag();
                    if (isSubstitutablePronoun(label)) {
                        masterTag = "";
                    }
                    String candidateText = extractTextWithSameTag(firstMention, masterTag);
                    if (!Strings.isNullOrEmpty(candidateText)) {
                        if ("PRP$".equals(label.tag())) {
                            candidateText += candidateText.endsWith("s") ? "'" : "'s";
                        }

                        text = candidateText;
                    }
                }
            }
        }

        return text;
    }

    /** *************************************************************
     */
    private boolean isSubstitutablePronoun(CoreLabel label) {

        String text =  label.originalText();
        String tag = label.tag();
        return ("PRP".equals(tag) || "PRP$".equals(tag)) && !ignorablePronouns.contains(text);
    }

    /** *************************************************************
     */
    private String extractTextWithSameTag(CorefMention mention, String masterTag) {

        List<String> out = Lists.newArrayListWithCapacity(mention.endIndex - mention.startIndex);
        List<CoreLabel> tokens = getSentenceTokens(mention.sentNum - 1);
        String tag = masterTag;
        for (int i = mention.startIndex; i < mention.endIndex; i++) {
            if (Strings.isNullOrEmpty(tag) || "DT".equals(tag)) {
                tag = tokens.get(i - 1).tag();
            }
            CoreLabel coreLabel = tokens.get(i - 1);
            if (tag.equals(coreLabel.tag())) {
                out.add(coreLabel.originalText());
            }
            else {
                break;
            }
        }
        return String.join(" ", out);
    }

    /** *************************************************************
     */
    private List<CoreLabel> getSentenceTokens(int sentenceNumber) {

        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        CoreMap sentence = sentences.get(sentenceNumber);
        List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
        return tokens;
    }

    /** *************************************************************
     */
    public static String substitute(String input) {

        CorefSubstitutor substitutor = new CorefSubstitutor(input);
        return StringUtils.join(substitutor.substitute(), " ");
    }

    /** *************************************************************
     */
    public static List<String> substitute(List<String> input) {

        CorefSubstitutor substitutor = new CorefSubstitutor(StringUtils.join(input, " "));
        return substitutor.substitute();
    }

    /** *************************************************************
     * Substitutes coreferences in document and returns each sentence as a List.
     * @return returns a list of Strings with coref substitutions
     */
    public List<String> substitute() {

        Map<Integer, CorefChain> corefs = document.get(CorefChainAnnotation.class);
        List<String> sentences = Lists.newArrayList();
        for (CoreMap sentence : document.get(SentencesAnnotation.class)) {
            StringBuilder builder = new StringBuilder();
            for (CoreLabel label : sentence.get(TokensAnnotation.class)) {
                if (builder.length() != 0 && needSpaceBefore(label)) {
                    builder.append(" ");
                }
                builder.append(replaceCoref(label, corefs));
            }
            sentences.add(builder.toString());
        }
        return sentences;
    }
}