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

import com.google.common.base.Joiner;
import com.google.common.collect.*;

import java.util.*;
import java.util.regex.Matcher;

public class NounSubstitutor extends SimpleSubstitutorStorage {


    /** ***************************************************************
     */
    public NounSubstitutor(List<String> clauses) {
        initialize(clauses);
    }

    /** ***************************************************************
     */
    private void initialize(List<String> clauses) {

        Multimap<String, String> groupsFull = parseGroupsAndCollectRoots(clauses);
        initGroupForAttributes(groupsFull);
        setGroups(
                mergeAttributes(groupsFull)
        );
    }

    /** ***************************************************************
     */
    private Multimap<String, String> parseGroupsAndCollectRoots(List<String> clauses) {
        
        Multimap<String, String> groupsFull = HashMultimap.create();
        for (String clause : clauses) {
            Matcher m = SubstitutionUtil.CLAUSE_SPLITTER.matcher(clause);
            if (m.matches()) {
                String label = m.group(1);
                if ("nn".equals(label)) {
                    String attr1 = m.group(2);
                    String attr2 = m.group(3);
                    groupsFull.put(attr1, attr1);
                    groupsFull.put(attr1, attr2);
                }
            } 
            else {
                System.out.println("ERROR: wrong clause " + clause);
            }
        }

        return groupsFull;
    }

    /** ***************************************************************
     */
    private void initGroupForAttributes(Multimap<String, String> groups) {
        
        Multimap<String, String> crossGroups = HashMultimap.create();
        for (String root: groups.keySet()) {
            Collection<String> rootGroup = groups.get(root);
            rootGroup.stream()
                    .filter(attr -> !attr.equals(root))
                    .forEach(attr -> crossGroups.putAll(attr, rootGroup));
        }
        groups.putAll(crossGroups);
    }

    /** ***************************************************************
     */
    private Map<String, String> mergeAttributes(Multimap<String, String> groupsFull) {
        
        Map<String, String> groupsWithMergedAttributes = Maps.newTreeMap();
        for (Map.Entry<String, Collection<String>> entry : groupsFull.asMap().entrySet()) {
            SortedMap<Integer, String> attributesMerged = Maps.newTreeMap();
            Integer firstIndex = null;
            for (String attr : entry.getValue()) {
                Matcher m = SubstitutionUtil.CLAUSE_PARAM.matcher(attr);
                if (m.matches()) {
                    String value = m.group(1);
                    Integer idx = Integer.valueOf(m.group(2));
                    if (firstIndex == null || idx.compareTo(firstIndex) < 0) {
                        firstIndex = idx;
                    }
                    attributesMerged.put(idx, value);
                } 
                else {
                    System.out.println("ERROR: wrong clause parameter " + m);
                }
            }
            groupsWithMergedAttributes.put(entry.getKey(), Joiner.on("").join(attributesMerged.values()) + "-" + firstIndex);
        }

        return groupsWithMergedAttributes;
    }

}
