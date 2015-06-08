/*
Copyright 2014-2015 IPsoft

Author: Andrew Reed andrew.reed@ipsoft.com

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
package com.articulate.sigma;

import com.articulate.sigma.nlp.pipeline.Pipeline;
import com.articulate.sigma.nlp.pipeline.SentenceBuilder;
import com.articulate.sigma.semRewrite.substitutor.CorefSubstitutor;
import com.google.common.collect.Lists;
import edu.stanford.nlp.pipeline.Annotation;

import java.util.ArrayList;
import java.util.List;

public class Document extends ArrayList<String> {

    /** *************************************************************
     */
    public Annotation annotateDocument(String extraSentence) {

        List sentences = new ArrayList<>(this);
        sentences.add(extraSentence);
        Annotation document = Pipeline.toAnnotation(sentences);
        return document;
    }

    /** *************************************************************
     * Inserts a new utterance or utterances into the document updating coreferencing of the document.
     * @param utterance input from the user (can be multiple sentences)
     * @return returns a coreference replaced version of the utterances
     */
    @Deprecated
    public List<String> addUtterance(String utterance) {

        List<String> toCoreference = Lists.newArrayList(this);
        toCoreference.add(utterance);

        Annotation document = Pipeline.toAnnotation(toCoreference);
        CorefSubstitutor ps = new CorefSubstitutor(document);

        List<String> substitutedInputs = new SentenceBuilder(document).asStrings(ps);
        List<String> newSentences = substitutedInputs.subList(this.size(), substitutedInputs.size());
        addAll(newSentences);

        return newSentences;
    }
}
