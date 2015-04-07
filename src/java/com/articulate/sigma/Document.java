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

import com.articulate.sigma.nlp.CorefSubstitutor;
import com.google.common.collect.Lists;
import edu.stanford.nlp.util.StringUtils;

import java.util.List;


public class Document {

    //utterances from the user
    List<String> documentContent = Lists.newArrayList();

    /** *************************************************************
     * Inserts a new utterance or utterances into the document updating coreferencing of the document.
     * @param utterance input from the user (can be multiple sentences)
     * @return returns a coreference replaced version of the utterances
     */
    public List<String> addUtterance(String utterance) {

        List<String> toCoreference = Lists.newArrayList(documentContent);
        toCoreference.add(utterance);
        List<String> substitutedInputs = CorefSubstitutor.substitute(toCoreference);
        List<String> newSentences = substitutedInputs.subList(documentContent.size(), substitutedInputs.size());
        documentContent.addAll(newSentences);
        return newSentences;
    }

    /** *************************************************************
     * @return the number of lines of dialog in the Document
     */
    public int size() {

        return documentContent.size();
    }

    /** *************************************************************
     */
    public boolean contains(String s) {

        return documentContent.contains(s);
    }

    /** *************************************************************
     */
    @Override
    public String toString() {

        return documentContent.toString();
    }
}
