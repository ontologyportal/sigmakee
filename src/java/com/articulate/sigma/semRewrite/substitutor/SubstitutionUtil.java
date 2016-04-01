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

import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubstitutionUtil {

    // predicate(word-1, word-2)
    // predicate(word, word-1)
    // predicate(word, word)
    // predicate(word-1, word)
    public static final Pattern CLAUSE_SPLITTER = Pattern.compile("([^\\(]+)\\((.+?(-\\d+)?),\\s*(.+?(-\\d+)?)\\)");
    public static final Pattern CLAUSE_PARAM = Pattern.compile("(.+)-(\\d+)");

    /** **************************************************************
     * Takes the list of clauses and substitutes them using provided substitutor.
     * If both arguments are substituted to the same group, this entry will be
     * removed from the original input.
     */
    public static List<String> groupClauses(ClauseSubstitutor substitutor, List<String> clauses) {

        //System.out.println("INFO in SubstitutionUtil.groupClauses(): clauses: " + clauses);
        //System.out.println("INFO in SubstitutionUtil.groupClauses(): substitutor: " + substitutor);
        Iterator<String> clauseIterator = clauses.iterator();
        List<String> modifiedClauses = Lists.newArrayList();
        while (clauseIterator.hasNext()) {
            String clause = clauseIterator.next();
            //System.out.println("INFO in SubstitutionUtil.groupClauses(): clause: " + clause);
            Matcher m = CLAUSE_SPLITTER.matcher(clause);
            if (m.matches()) {
                // FIXME LOW: Still waiting for optimization
                String attr1 = m.group(2);
                String attr2 = m.group(4);
                //System.out.println("INFO in SubstitutionUtil.groupClauses(): attr1: " + attr1);
                //System.out.println("INFO in SubstitutionUtil.groupClauses(): attr2: " + attr2);
                if ((m.group(3) != null && substitutor.containsKey(attr1.toUpperCase()))
                        || (m.group(5) != null && substitutor.containsKey(attr2.toUpperCase()))) {
                    CoreLabelSequence attr1Grouped = substitutor.getGrouped(attr1.toUpperCase());
                    CoreLabelSequence attr2Grouped = substitutor.getGrouped(attr2.toUpperCase());
                    //System.out.println("INFO in SubstitutionUtil.groupClauses(): attr1Grouped: " + attr1Grouped);
                    //System.out.println("INFO in SubstitutionUtil.groupClauses(): attr2Grouped: " + attr2Grouped);
                   // if (attr1Grouped.toString() != null && attr2Grouped != null &&
                    if (!Objects.equals(attr1Grouped, attr2Grouped)) {
                            // delete clauses like amod() and nn() that have parts of a compound noun as args
                        clauseIterator.remove();
                        String label = m.group(1);
                        String arg1 = attr1;
                        if (attr1Grouped != null)
                            arg1 = attr1Grouped.toStringWithNumToken();
                        String arg2 = attr2;
                        if (attr2Grouped != null)
                            arg2 = attr2Grouped.toStringWithNumToken();
                        String modClause = label + "(" + arg1  + "," + arg2 + ")";
                        //System.out.println("INFO in SubstitutionUtil.groupClauses(): modClause: " + modClause);
                        modifiedClauses.add(modClause);
                    }
                }
                else
                    modifiedClauses.add(clause);
            }
            else
                System.out.println("Error in SubstitutionUtil.groupClauses(): unmatched clause: " + clause);
        }

        //System.out.println("INFO in SubstitutionUtil.groupClauses(2): " + modifiedClauses);
        clauses.addAll(modifiedClauses);
        return modifiedClauses;
    }
}
