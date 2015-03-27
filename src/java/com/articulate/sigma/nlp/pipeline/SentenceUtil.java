package com.articulate.sigma.nlp.pipeline;

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

import com.articulate.sigma.nlp.constants.LangLib;
import com.google.common.collect.Lists;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.KBestTreesAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.ScoredObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.*;

public class SentenceUtil {

    /** ***************************************************************
     * Print all the sentences in this document
     * CoreMap is essentially a Map that uses class objects as keys and 
     * has values with custom types
     */
    public static void printSentences(Annotation document) {

        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for(CoreMap sentence : sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            int count = 1;
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(TextAnnotation.class);
                // this is the POS tag of the token
                String pos = token.get(PartOfSpeechAnnotation.class);
                // this is the NER label of the token
                String ne = token.get(NamedEntityTagAnnotation.class);
                List<CoreMap> entity = token.get(MentionsAnnotation.class);
                System.out.println(word + "-" + count + "/" + pos + "/" + ne + "/" + entity);
                count++;
            }

            // this is the parse tree of the current sentence
            // Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);

            // this is the Stanford dependency graph of the current sentence
            SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
            System.out.println(dependencies.toList());
            //System.out.println(dependencies.toPOSList());
            System.out.println(getFullNamedEntities(sentence));

            List<ScoredObject<Tree>> scoredTrees = sentence.get(KBestTreesAnnotation.class);
            System.out.println("\nTree Scores:");
            for (ScoredObject<Tree> scoredTree : scoredTrees) {
                //SemanticGraph graph = SemanticGraphFactory.generateUncollapsedDependencies(scoredTree.object());
                System.out.println(scoredTree.score());
            }
        }
    }

    /** ***************************************************************
     * @return a List of Strings which are concatenated tokens forming
     * a single named entity, with the suffix of the number of the
     * head.  For example "I went to New York." would return
     * "NewYork-4".
     */
    public static ArrayList<String> getFullNamedEntities (CoreMap sentence) {

        ArrayList<String> nes = new ArrayList<String>();
        StringBuffer ne = new StringBuffer();
        String neType = "";
        int count = 1;
        int wordCount = 0; // number of words packed into a given ne
        for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
            // this is the text of the token
            String word = token.get(TextAnnotation.class);
            System.out.println(word);
            // this is the NER label of the token
            String type = token.get(NamedEntityTagAnnotation.class);
            if (neType == "") {
                neType = type;
                if (!type.equals("O"))
                    ne.append(word);
                wordCount = 1;
            }
            else if (!neType.equals(type)) {
                if (!neType.equals("O"))
                    nes.add(ne.toString() + "-" + (count-wordCount));
                ne = new StringBuffer();
                if (!type.equals("O")) {
                    ne.append(word);
                    wordCount = 1;
                }
                else
                    wordCount = 0;
                neType = type;
            }
            else {
                if (!type.equals("O"))
                    ne.append(word);
                wordCount++;
            }
            System.out.println(word + "-" + count + "/" + type + "/" + neType + "/" + ne + "/" + wordCount);
            count++;
        }
        return nes;
    }

    /** ***************************************************************
     *  Print the coreference link graph
     *  Each chain stores a set of mentions that link to each other,
     *  along with a method for getting the most representative mention
     *  Both sentence and token offsets start at 1!
     */
    public static void printCorefChain(Annotation document) {

        Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
        for (CorefChain cc : graph.values()) {
            List<CorefChain.CorefMention> mentions = cc.getMentionsInTextualOrder();
            if (mentions.size() > 1) {
                for (CorefChain.CorefMention ment : mentions) {
                    System.out.println(ment.sentNum + " : " + ment.headIndex + " : " + ment.mentionSpan);
                }
            }
        }
    }

    public static List<String> toDependenciesList(Annotation document) {
        ArrayList<String> results = Lists.newArrayList();
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
            results = Lists.newArrayList(dependencies.toList().split("\n"));
        }
        return results;
    }

    //TODO: I'm a monster! Refactor me
    /** *************************************************************
     * returns a list of strings that add tense, number, etc. information about words in input
     * ex.  tense(PAST, Verb)
     *      number(SINGULAR, Noun)
     */
    public static List<String> findPOSInformation(Annotation document, List<String> dependenciesList) {

        List<String> posInformation = Lists.newArrayList();
        List<CoreLabel> tokens = document.get(CoreAnnotations.TokensAnnotation.class);
        for (CoreLabel label : tokens) {
            Pattern auxPattern = Pattern.compile("aux\\(.*, " + label.toString() + "\\)");
            boolean isAux = false;
            for (String dep : dependenciesList) {
                if (auxPattern.matcher(dep).find()) {
                    isAux = true;
                    break;
                }
            }
            if (!isAux) {
                boolean progressive = false;
                boolean perfect = false;
                String pos = label.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                if (LangLib.POS_VBD.equals(pos)) {
                    posInformation.add(makeBinaryRelationship("tense", LangLib.TENSE_PAST, label.toString()));
                } else if (LangLib.POS_VBP.equals(pos) || LangLib.POS_VBZ.equals(pos)) {
                    posInformation.add(makeBinaryRelationship("tense", LangLib.TENSE_PRESENT, label.toString()));
                } else if (LangLib.POS_VBG.equals(pos) || LangLib.POS_VB.equals(pos) || LangLib.POS_VBN.equals(pos)) {
                    Pattern reverseAuxPattern = Pattern.compile("aux\\(" + label.toString() + ", .*-(\\d+)\\)");
                    for (String dep : dependenciesList) {
                        Matcher auxMatcher = reverseAuxPattern.matcher(dep);
                        if (auxMatcher.find()) {
                            int i = Integer.parseInt(auxMatcher.group(1));
                            CoreLabel t = tokens.get(i-1);
                            if (t.get(CoreAnnotations.LemmaAnnotation.class).equals("be")) {
                                if (t.get(CoreAnnotations.PartOfSpeechAnnotation.class).equals(LangLib.POS_VBP) || t.get(CoreAnnotations.PartOfSpeechAnnotation.class).equals(LangLib.POS_VBZ)) {
                                    posInformation.add(makeBinaryRelationship("tense", LangLib.TENSE_PRESENT, label.toString()));
                                } else if (t.get(CoreAnnotations.PartOfSpeechAnnotation.class).equals(LangLib.POS_VBD)) {
                                    posInformation.add(makeBinaryRelationship("tense", LangLib.TENSE_PAST, label.toString()));
                                }
                                progressive = true;
                            } else if (t.get(CoreAnnotations.LemmaAnnotation.class).equals("will")) {
                                posInformation.add(makeBinaryRelationship("tense", LangLib.TENSE_FUTURE, label.toString()));
                            } else if (t.get(CoreAnnotations.LemmaAnnotation.class).equals("have")) {
                                if (t.get(CoreAnnotations.PartOfSpeechAnnotation.class).equals(LangLib.POS_VBP) || t.get(CoreAnnotations.PartOfSpeechAnnotation.class).equals(LangLib.POS_VBZ)) {
                                    posInformation.add(makeBinaryRelationship("tense", LangLib.TENSE_PRESENT, label.toString()));
                                } else if (t.get(CoreAnnotations.PartOfSpeechAnnotation.class).equals(LangLib.POS_VBD)) {
                                    posInformation.add(makeBinaryRelationship("tense", LangLib.TENSE_PAST, label.toString()));
                                }
                                perfect = true;
                            }
                        }
                    }
                } else if (LangLib.POS_NN.equals(pos) || LangLib.POS_NNP.equals(pos)) {
                    posInformation.add(makeBinaryRelationship("number", LangLib.NUMBER_SINGULAR, label.toString()));
                } else if (LangLib.POS_NNS.equals(pos) || LangLib.POS_NNPS.equals(pos)) {
                    posInformation.add(makeBinaryRelationship("number", LangLib.NUMBER_PLURAL, label.toString()));
                }

                if (progressive && perfect) {
                    posInformation.add(makeBinaryRelationship("aspect", LangLib.ASPECT_PROGRESSIVE_PERFECT, label.toString()));
                } else if (progressive) {
                    posInformation.add(makeBinaryRelationship("aspect", LangLib.ASPECT_PROGRESSIVE, label.toString()));
                } else if (perfect) {
                    posInformation.add(makeBinaryRelationship("aspect", LangLib.ASPECT_PERFECT, label.toString()));
                }
            }
        }
        return posInformation;
    }

    //TODO: see if this exists somewhere else or move to utility class
    /** *************************************************************
     */
    public static String makeBinaryRelationship(String relationship, String argument1, String argument2) {

        StringBuilder sb = new StringBuilder(relationship);
        sb.append("(");
        sb.append(argument1);
        sb.append(", ");
        sb.append(argument2);
        sb.append(")");
        return sb.toString();
    }


    /** ***************************************************************
     */
    public static void main(String[] args) {
        
        Pipeline p = new Pipeline();
        Annotation a = p.annotate("I went to New York and had cookies and cream in the Empire State Building in January with Mary.");
        printCorefChain(a);
        printSentences(a);
    }
}
