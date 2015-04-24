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
    public static void groupClauses(ClauseSubstitutor substitutor, List<String> clauses) {

        Iterator<String> clauseIterator = clauses.iterator();
        List<String> modifiedClauses = Lists.newArrayList();
        while (clauseIterator.hasNext()) {
            String clause = clauseIterator.next();
            Matcher m = CLAUSE_SPLITTER.matcher(clause);
            if (m.matches()) {
                // FIXME LOW: Still waiting for optimization
                String attr1 = m.group(2);
                String attr2 = m.group(4);
                if ((m.group(3) != null && substitutor.containsKey(attr1))
                        || (m.group(5) != null && substitutor.containsKey(attr2))) {
                    CoreLabelSequence attr1Grouped = substitutor.getGrouped(attr1);
                    CoreLabelSequence attr2Grouped = substitutor.getGrouped(attr2);
                    clauseIterator.remove();
                    if (!Objects.equals(attr1Grouped, attr2Grouped)) {
                        String label = m.group(1);
                        String arg1 = attr1Grouped.toLabelString().orElse(attr1);
                        String arg2 = attr2Grouped.toLabelString().orElse(attr2);
                        modifiedClauses.add(label + "(" + arg1  + "," + arg2 + ")");
                    }
                }
            }
        }

        clauses.addAll(modifiedClauses);
    }
}
