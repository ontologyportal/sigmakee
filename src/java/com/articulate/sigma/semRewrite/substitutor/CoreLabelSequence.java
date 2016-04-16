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

import com.articulate.sigma.StringUtil;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CoreLabelSequence {

    public static final CoreLabelSequence EMPTY_SEQUENCE = CoreLabelSequence.from();
    public static final int IGNORE_SENTENCE = Integer.MIN_VALUE;

    final List<CoreLabel> labels;

    /** ***************************************************************
     */
    public CoreLabelSequence(List<CoreLabel> labels) {

        this.labels = ImmutableList.copyOf(labels);
    }

    /** ***************************************************************
     */
    public CoreLabelSequence(CoreLabel... labels) {

        this.labels = ImmutableList.copyOf(labels);
    }

    /** ***************************************************************
     */
    public static CoreLabelSequence from(CoreLabel... labels) {

        return labels.length > 0
                ? new CoreLabelSequence(labels)
                : EMPTY_SEQUENCE;
    }

    /** ***************************************************************
     */
    public String toListString() {

        return labels.toString();
    }

    /** ***************************************************************
     * Generate a String where the CoreLabel values are separated by
     * spaces and do not have a token number suffix
     */
    public String toString() {

        StringBuffer sb = new StringBuffer();
        for (CoreLabel cl : labels) {
            if (!StringUtil.emptyString(sb.toString()))
                sb.append(" ");
            sb.append(cl.value());
        }
        return labels.toString();
    }

    /** ***************************************************************
     */
    public List<CoreLabel> getLabels() {

        return labels;
    }

    /** ***************************************************************
     */
    public boolean containsLabel(CoreLabel label) {

        return labels.contains(label);
    }

    /** ***************************************************************
     */
    public boolean isEmpty() {

        return labels.isEmpty();
    }

    /** *************************************************************
     * Checks if label is part of current sequence
     * @param text the label to be checked for in the sequence
     * @param sentIndex can be CoreLabelSequence.IGNORE_SENTENCE
     */
    public boolean containsLabel(int sentIndex, String text, int index) {

        //System.out.println("CoreLabelSequence.containsLabel(): sentIndex: " + sentIndex);
        //System.out.println("CoreLabelSequence.containsLabel(): text: " + text);
        //System.out.println("CoreLabelSequence.containsLabel(): index: " + index);
        //System.out.println("CoreLabelSequence.containsLabel(): labels: " + labels);

        for (CoreLabel label : labels) {
            //System.out.println("CoreLabelSequence.containsLabel(): value: " + label.value());
            //System.out.println("CoreLabelSequence.containsLabel():index: " + label.index());
            if ((sentIndex == label.sentIndex() || sentIndex == IGNORE_SENTENCE)
                    && text.equals(label.value())
                    // && index == label.index() FIXME: total hack!
                    ) {
                //System.out.println("CoreLabelSequence.containsLabel(): success ");
                return true;
            }
        }
        //return labels.stream().anyMatch(label ->
        //                (sentIndex == label.sentIndex() || sentIndex == IGNORE_SENTENCE)
        //                        && text.equals(label.originalText())
        //                        && index == label.index()
        //);
        //System.out.println("CoreLabelSequence.containsLabel(): failure - label not in sequence ");
        return false;
    }

    /** *************************************************************
     * Converts the sequence to text representation using "_" as a separator
     */
    public String toText() {

        return Joiner.on("_").join(labels.stream().map(label -> label.value()).toArray());
    }

    /** *************************************************************
     * Returns the sequence in the String format like "United_States-3"
     */
    public Optional<String> toLabelString() {

        if (!labels.isEmpty()) {
            String combinedIndex = "-" + labels.get(0).index();
            return Optional.of(toText() + combinedIndex);
        }
        else {
            return Optional.empty();
        }
    }

    /** *************************************************************
     * Returns the sequence in the String format like "United_States-3"
     */
    public String toStringWithNumToken() {

        //System.out.println("CoreLabelSequence.toStringWithNumToken(): labels: " + labels);
        if (!labels.isEmpty()) {
            String combinedIndex = "-" + labels.get(0).index();
            return toText() + combinedIndex;
        }
        else {
            return "";
        }
    }

    /** *************************************************************
     */
    public int size() {
        return labels.size();
    }

    /** *************************************************************
     * Change the value() of each CoreLabel to be all caps
     */
    public CoreLabelSequence toUpperCase() {

        //System.out.println("CoreLabelSequence.toUpperCase(): labels: " + labels);
        List<CoreLabel> lcl = new ArrayList<>();
        for (CoreLabel cl : labels) {
            CoreLabel newcl = new CoreLabel();
            newcl.setValue(cl.value().toUpperCase());
            newcl.setIndex(cl.index());
            lcl.add(newcl);
        }
        CoreLabelSequence cls = new CoreLabelSequence(lcl);
        //System.out.println("CoreLabelSequence.toUpperCase(): cls: " + cls);
        return cls;
    }

    /** *************************************************************
     */
    public CoreLabelSequence removePunctuation() {

        //System.out.println("CoreLabelSequence.toUpperCase(): removePunctuation: " + labels);
        CoreLabelSequence cls = new CoreLabelSequence(labels);
        for (CoreLabel cl : labels) {
            String puncRE = "[\\.\\,\\;\\:\\[\\]\\{\\}\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)\\-\\=\\_\\+\\`\\~\\<\\>\\/\\?]";
            if (cl.value().matches(puncRE))
                cl.setValue(cl.value().replace(puncRE,""));
        }
        //System.out.println("CoreLabelSequence.toUpperCase(): cls: " + cls);
        return cls;
    }

    /** *************************************************************
     */
    public String toWordNetID() {

        //System.out.println("CoreLabelSequence.toUptoWordNetIDperCase(): labels: " + labels);
        StringBuffer sb = new StringBuffer();
        for (CoreLabel cl : labels) {
            if (!StringUtil.emptyString(sb.toString()))
                sb.append("_");
            sb.append(cl.value().replace(" ","_"));
        }
        //System.out.println("CoreLabelSequence.toUptoWordNetIDperCase(): sb: " + sb);
        return sb.toString();
    }
}
