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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class LocationSubstitutor extends SimpleSubstitutorStorage {

    /** **************************************************************
     * Connects "there" to the nearest coreference that is a location.
     */
    public LocationSubstitutor(Annotation document) {
        addGroups(collectGroups(document));
    }

    private Map<CoreLabelSequence, CoreLabelSequence> collectGroups(Annotation document) {
        Map<CoreLabelSequence, CoreLabelSequence> collectedGroups = Maps.newHashMap();
        List<CoreLabel> labels = document.get(CoreAnnotations.TokensAnnotation.class);

        ListIterator<CoreLabel> rit = labels.listIterator(labels.size());
        while(rit.hasPrevious()) {
            CoreLabel label = rit.previous();

            // We have to distinguish the case of "there" being used as a different part of speech, so we should only
            // seek to make "there" a coreference when it has POS tag VB and not "EX" from the Stanford POS tagger.
            if("there".equals(label.lemma()) && ("RB".equals(label.tag()) || "VB".equals(label.tag()))) {
                if(rit.hasPrevious()) {
                    CoreLabelSequence location = closestLocation(labels, rit.previousIndex());
                    if (!location.isEmpty()) {
                        collectedGroups.put(CoreLabelSequence.from(label), location);
                    }
                }
            }
        }

        return collectedGroups;
    }

    private CoreLabelSequence closestLocation(List<CoreLabel> labels, int therePosition) {
        ListIterator<CoreLabel> rit = labels.listIterator(therePosition);
        LinkedList<CoreLabel> location = Lists.newLinkedList();
        while(rit.hasPrevious()) {
            CoreLabel label = rit.previous();
            if("LOCATION".equals(label.ner()) || "ORGANIZATION".equals(label.ner())) {
                if(Iterables.getFirst(location, label).ner().equals(label.ner())) {
                    location.addFirst(label);
                }
            } else if(!location.isEmpty()){
                if("IN".equals(label.tag())) {
                    location.addFirst(label);
                }
                break;
            }
        }

        return CoreLabelSequence.from(location.toArray(new CoreLabel[0]));
    }
}
