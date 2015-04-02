package com.articulate.sigma.semRewrite;

import com.articulate.sigma.KBmanager;

import java.util.*;

import static com.articulate.sigma.semRewrite.Interpreter.canon;

/**
 * Created by peigenyou on 3/31/15.
 */
public class SemRewriteRuleCheck {

    /**
     * check the ruleset if there is any subsumes happens inside
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
                    if(rs.rules.get(r).rhs.cnf!=null) continue;
                    count++;
                    System.out.println("\nLine "+rs.rules.get(r).startLine + " and  " + rs.rules.get(k).startLine);
                    System.out.println("rules = \n" + rs.rules.get(r) + "\n" + rs.rules.get(k));
                }
            }
        }
        System.out.println("There are total " + count + " need to be check.");
        return subsumMap;
    }

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

    public static boolean isCNFSubsumed(CNF subsumer, CNF subsumed) {

        //TODO


        return false;
    }



}
