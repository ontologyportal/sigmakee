package com.articulate.sigma.semRewrite;

import java.util.*;

/*
Copyright 2014-2015 IPsoft

Author: Peigen You Peigen.You@ipsoft.com

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
public class SemRewriteRuleCheck {

    public static boolean isIgnoreCNFOnRight = true;

    /***********************************************************
     * Check the whole Ruleset. Print all possible subsumptions.
     * Change isIgnoreCNFOnRight value to control if CNF on right rule conflict should be printed.
     */
    public static Map<Integer, HashSet<Integer>> checkRuleSet(RuleSet rs) {

        HashMap<Integer, HashSet<Integer>> subsumMap = new HashMap<Integer, HashSet<Integer>>();
        int len = rs.rules.size();
        for (int i = 0; i < len; ++i) {
            Rule r = rs.rules.get(i);

            ArrayList<Integer> getSubsumedRules = new ArrayList<Integer>();
            ArrayList<Integer> subsumerRules = new ArrayList<Integer>();

            isRuleSubsumedByRuleSet(r, rs, getSubsumedRules, subsumerRules);

            if (subsumerRules.size() != 0) {
                HashSet<Integer> l = subsumMap.get(i);
                if (l == null)
                    l = new HashSet<Integer>();
                for (int k : subsumerRules) {
                    if (k != i)
                        l.add(k);
                }
                if (l.size() != 0)
                    subsumMap.put(i, l);
            }

            if (getSubsumedRules.size() != 0) {
                HashSet<Integer> l;
                for (int h : getSubsumedRules) {
                    if (h == i)
                        continue;
                    l = subsumMap.get(h);
                    if (l == null)
                        l = new HashSet<Integer>();
                    l.add(i);
                    subsumMap.put(h, l);
                }
            }
        }
        int count = 0;
        for (Integer r : subsumMap.keySet()) {
            for (Integer k : subsumMap.get(r)) {
                if (k > r) {
                    if (isIgnoreCNFOnRight)
                        if (rs.rules.get(r).rhs.cnf != null)
                            continue;
                    count++;
                    System.out.println("\nLine " + rs.rules.get(r).startLine + " and  " + rs.rules.get(k).startLine);
                    System.out.println("rules = \n" + rs.rules.get(r) + "\n" + rs.rules.get(k));
                }
            }
        }
        System.out.println("There are total " + count + " need to be check.");
        return subsumMap;
    }

    /***********************************************************
     * get all the Rules in Ruleset that would be subsumed or subsums the rule r
     * should pass in the arraylist as result space
     */
    public static void isRuleSubsumedByRuleSet(Rule r, RuleSet rs, ArrayList<Integer> getSubsumedRules, ArrayList<Integer> subsumerRules) {

        getSubsumedRules.clear();
        subsumerRules.clear();
        CNF lc = Clausifier.clausify(r.lhs.deepCopy());
        for (int i = 0; i < rs.rules.size(); ++i) {
            CNF rc = rs.rules.get(i).cnf.deepCopy();
            if (isCNFSubsumedNaive(lc, rc)) {
                subsumerRules.add(i);
            }
            if (isCNFSubsumedNaive(rc, lc)) {
                getSubsumedRules.add(i);
            }
        }
    }

    /***********************************************************
     * check if CNF subsumed, naive implementation
     */
    public static boolean isCNFSubsumedNaive(CNF subsumer, CNF subsumed) {

        HashMap<String, String> binding = subsumer.unify(subsumed);
        if (binding != null) {
            subsumed = subsumer.applyBindings(binding);
        }
        subsumed.clearBound();
        subsumer.clearBound();
        if (subsumed.clauses.size() < subsumer.clauses.size()) return false;
        //check if subsumed has all the clauses that subsumer has.
        if (subsumed.clauses.containsAll(subsumer.clauses))
            return true;
        return false;
    }

    /***********************************************************
     * //TODO
     */
    public static boolean isCNFSubsumed(CNF subsumer, CNF subsumed) {

        return false;
    }


}
