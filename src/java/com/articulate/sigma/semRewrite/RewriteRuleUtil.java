package com.articulate.sigma.semRewrite;
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

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.google.common.base.Strings;
import javafx.beans.binding.ObjectExpression;

import java.io.File;
import java.util.*;

import static com.articulate.sigma.semRewrite.Interpreter.canon;

public final class RewriteRuleUtil extends RuleSet {

    static String fileLocation = "/Users/peigenyou/workspace/sigma/KBs/WordNetMappings/SemRewrite.txt";
    static KBmanager kbm;
    static KB kb;

    static List<String> ignorePreds = Arrays.asList(new String[]{"number", "tense", "root", "names"});

    static {
        kbm = KBmanager.getMgr();
        kbm.initializeOnce();
        kb = kbm.getKB("SUMO");
    }

    private RewriteRuleUtil() {

    }

    /**
     * **********************************************************
     * update a Ruleset's indexed rule's CNF
     */
    public static void updateCNF(int index, CNF f, RuleSet rs) {

        Rule r = rs.rules.get(index);
        r.cnf = f;
        System.out.println(r);
        Rule newr = Rule.parse(new Lexer(r.toString()));
        System.out.println(newr);
        rs.rules.set(index, newr);
    }

    /**
     * **********************************************************
     */
    private static void updateCNFTest(RuleSet rs) {

        rs = new RuleSet().parse(new Lexer("prep_about(?X,?Y) ==> {refers(?X,?Y)}.\n" + "prep_about(?H,?Y) ==> {refers(?H,?Y)}.\n"));
        rs = Clausifier.clausify(rs);
        System.out.println(rs);
        for (int i = 0; i < rs.rules.size(); ++i) {
            Rule r = rs.rules.get(i);
            CNF unifier = r.cnf;
            for (int j = i + 1; j < rs.rules.size(); ++j) {

                CNF unified = rs.rules.get(j).cnf;

                System.out.println("unified = " + unified + "   " + unifier);
                HashMap<String, String> map = unifier.unify(unified);
                if (map == null || map.size() < 1) {
                    continue;
                }
                System.out.printf("Unification found between index %d and %d map = %s \n", i, j, map);
                System.out.println("Before unification---------" + unified + "   " + unifier + "\n------------");
                unified = unified.applyBindings(map);
                System.out.println("After unification---------" + unified + "   " + unifier + "\n-----------");
                boolean m = SemRewriteRuleCheck.isCNFSubsumed(unified, unifier);
                System.out.println("After update---------" + m + "\n-----------");

            }
        }

    }

    /**
     * **********************************************************
     * load RuleSet from "SemRewrite.txt"
     */
    public static RuleSet loadRuleSet() {

        RuleSet rs;
        KBmanager kb = KBmanager.getMgr();
        kb.initializeOnce();

        String f = KBmanager.getMgr().getPref("kbDir") + File.separator + "WordNetMappings" + File.separator + "SemRewrite.txt";
        String pref = KBmanager.getMgr().getPref("SemRewrite");
        if (!Strings.isNullOrEmpty(pref))
            f = pref;
        if (f.indexOf(File.separator.toString(), 2) < 0)
            f = "/home/apease/SourceForge/KBs/WordNetMappings" + f;
        try {
            RuleSet rsin = RuleSet.readFile(f);
            rs = canon(rsin);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            return null;
        }
        rs = Clausifier.clausify(rs);
        return rs;
    }

    /**
     * **********************************************************
     */
    public static Map<Integer, CNF> generateCNFForStringSet(String[] input) {

        Map<Integer, CNF> res = new HashMap<Integer, CNF>();
        Interpreter inter = new Interpreter();
        KBmanager.getMgr().initializeOnce();
        inter.initialize();
        for (int i = 0; i < input.length; ++i) {
            CNF cnf = inter.interpretGenCNF(input[i]).get(0);
            System.out.println(cnf);
            cnf = preProcessCNF(cnf);
            res.put(i, cnf);
        }
        return res;
    }

    /**
     * **********************************************************
     */
    public static CNF preProcessCNF(CNF cnf) {

        Iterator<Clause> iterator = cnf.clauses.iterator();
        while (iterator.hasNext()) {
            Clause c = iterator.next();
            if (ignorePreds.contains(c.disjuncts.get(0).pred)) {
                iterator.remove();
                continue;
            }
            if (c.disjuncts.get(0).pred.equals("sumo")) {
                String sumoTerm = c.disjuncts.get(0).arg1;
                String word = c.disjuncts.get(0).arg2;
                for (Clause m : cnf.clauses) {
                    if (m.disjuncts.get(0).arg1.equals(word)) {
                        m.disjuncts.get(0).arg1 = sumoTerm;
                    }
                    if (m.disjuncts.get(0).arg2.equals(word)) {
                        m.disjuncts.get(0).arg2 = sumoTerm;
                    }
                }
                iterator.remove();
            }
        }
        return cnf;
    }

    /**
     * **********************************************************
     */
    public static CNF unification(CNF c1, CNF c2) {

        CNF rescnf = new CNF();
        c2.clauses.sort(clauseComparator);
        c1.clauses.sort(clauseComparator);
        for (Clause m : c2.clauses) {
            for (Clause n : c1.clauses) {
                Clause h = m.deepCopy();
                n = n.deepCopy();
                if (isRelated(h, n)) {
                    rescnf.clauses.add(h);
                    break;
                }
            }
        }
        return rescnf;
    }

    /**
     * **********************************************************
     */
    private static class Pair<F, S> {

        F first;
        S second;

        public Pair(F f, S s) {

            first = f;
            second = s;
        }

        @Override
        public boolean equals(Object o) {

            if (!(o instanceof Pair))
                return false;
            Pair p = (Pair) o;
            return (p.first.equals(this.first) && p.second.equals(this.second)) ||
                    (p.first.equals(this.second) && p.second.equals(this.first));
        }

        @Override
        public int hashCode() {

            return first.hashCode() + second.hashCode();
        }

        @Override
        public String toString() {

            return '[' + first.toString() + ',' + second.toString() + ']';
        }
    }

    /**
     * **********************************************************
     */
    public static Map<CNF, Set<Pair<Integer, Integer>>> reverseMap(Map<Integer, Map<Integer, CNF>> input) {

        Map<CNF, Set<Pair<Integer, Integer>>> res = new HashMap<CNF, Set<Pair<Integer, Integer>>>();
        for (Integer i : input.keySet()) {
            Map<Integer, CNF> m = input.get(i);
            for (Integer j : m.keySet()) {
                CNF cnf = m.get(j);
                Set<Pair<Integer, Integer>> mid = res.get(cnf);
                if (mid == null)
                    mid = new HashSet<Pair<Integer, Integer>>();
                mid.add(new Pair<Integer, Integer>(i, j));
                res.put(cnf, mid);
            }
        }
        return res;
    }

    /**
     * **********************************************************
     * naive implementation
     */
    public static boolean isRelated(Clause m, Clause n) {

        if (!m.disjuncts.get(0).pred.equals(n.disjuncts.get(0).pred))
            return false;
        String ca = findCommonAncesstor(m.disjuncts.get(0).arg1, n.disjuncts.get(0).arg1);
        if (ca != null) {
            m.disjuncts.get(0).arg1 = ca;
            n.disjuncts.get(0).arg1 = ca;
        }
        String ca1 = findCommonAncesstor(m.disjuncts.get(0).arg2, n.disjuncts.get(0).arg2);
        if (ca1 != null) {
            m.disjuncts.get(0).arg2 = ca1;
            n.disjuncts.get(0).arg2 = ca1;
        }
        if (ca1 != null && ca != null) return true;
        return false;
    }

    /**
     * **********************************************************
    */
    public static boolean isRelatedVariable(String s1, String s2) {

        return true;
    }

    /**
     * **********************************************************
     */
    public static Map<Integer, Map<Integer, CNF>> getCommonCNF(Map<Integer, CNF> map) {

        Map<Integer, Map<Integer, CNF>> res = new HashMap<Integer, Map<Integer, CNF>>();
        HashMap<String, String> bindmap;
        for (Integer i = 0; i < map.keySet().size(); i++) {
            CNF cnfOut = map.get(i);
            Map<Integer, CNF> mapfori = new HashMap<Integer, CNF>();
            for (Integer j = i + 1; j < map.keySet().size(); ++j) {
                CNF cnfIn = map.get(j);
                CNF cnfnew = unification(cnfIn, cnfOut);
                if (cnfnew.clauses.size() > 0)
                    mapfori.put(j, cnfnew);
            }
            res.put(i, mapfori);
        }
        Iterator<Map.Entry<Integer, Map<Integer, CNF>>> iterator = res.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry e = iterator.next();
            if (e.getValue() == null)
                iterator.remove();
        }
        return res;
    }

    /**
     * **********************************************************
     */
    public static String findCommonAncesstor(String s1, String s2) {

        HashSet<String> p1 = kb.kbCache.parents.get("subclass").get(s1);
        if (p1 == null) p1 = new HashSet<String>();
        HashSet<String> m = kb.kbCache.parents.get("subrelation").get(s1);
        if (m != null)
            p1.addAll(m);
        m = kb.kbCache.parents.get("subAttribute").get(s1);
        if (m != null)
            p1.addAll(m);
        p1.add(s1);
        HashSet<String> p2 = kb.kbCache.parents.get("subclass").get(s2);
        if (p2 == null) p2 = new HashSet<String>();
        m = kb.kbCache.parents.get("subrelation").get(s2);
        if (m != null)
            p2.addAll(m);
        p2.add(s2);
        m = kb.kbCache.parents.get("subAttribute").get(s2);
        if (m != null)
            p2.addAll(m);
        Collection<String> common = getCommon(p1, p2);
        if (common.size() < 1)
            return null;
        for (String k : common) {
            HashSet<String> children = kb.kbCache.children.get("subrelation").get(k);
            if (children == null) children = new HashSet<>();
            m = kb.kbCache.children.get("subAttribute").get(k);
            if (m != null)
                children.addAll(m);
            m = kb.kbCache.children.get("subclass").get(k);
            if (m != null)
                children.addAll(m);
            boolean isClosest = true;
            for (String n : common) {
                if (children.contains(n)) {
                    isClosest = false;
                    break;
                }
            }
            if (isClosest) return k;
        }
        return null;
    }

    /**
     * **********************************************************
     */
    public static Collection getCommon(Collection c1, Collection c2) {

        Iterator iterator = c1.iterator();
        while (iterator.hasNext()) {
            Object o1 = iterator.next();
            if (!c2.contains(o1))
                iterator.remove();
        }
        return c1;
    }

    /**
     * **********************************************************
     */
    private static Comparator<Clause> clauseComparator = new Comparator<Clause>() {

        @Override
        public int compare(Clause o1, Clause o2) {

            return o1.disjuncts.get(0).pred.compareTo(o2.disjuncts.get(0).pred);
        }
    };

    /**
     * **********************************************************
     */
//    public static void main(String[] args) {
//
//        ArrayList<Integer> getSubsumed = new ArrayList<Integer>();
//        ArrayList<Integer> subsumer = new ArrayList<Integer>();
//        RuleSet rs = loadRuleSet();
//        String input = "";
//        System.out.println("SemRewrite.txt loaded. There are " + rs.rules.size() + " rules.");
//        SemRewriteRuleCheck.checkRuleSet(rs);
//        System.out.println("Will check rules entered. Please enter rule.\n");
//        Scanner scanner = new Scanner(System.in);
//        do {
//            try {
//                System.out.print("\nEnter rule: ");
//                input = scanner.nextLine().trim();
//                if (!Strings.isNullOrEmpty(input) &&  !input.equals("exit") && !input.equals("quit")) {
//                    if(input.equals("reload")){
//                        rs=loadRuleSet();
//                        SemRewriteRuleCheck.checkRuleSet(rs);
//                        continue;
//                    }
//                    Rule r = Rule.parseString(input);
//                    System.out.println("The rule entered is :: " + r + "\n");
//                    SemRewriteRuleCheck.isRuleSubsumedByRuleSet(r, rs, getSubsumed, subsumer);
//
//                    System.out.println("Following " + getSubsumed.size() + " rules would subsume the rule entered: \n");
//                    for (int k : getSubsumed) {
//                        System.out.println("Line Number:" + rs.rules.get(k).startLine + " : " + rs.rules.get(k));
//                    }
//                    System.out.println("---------------------------------------------------------------");
//                    System.out.println("Following " + subsumer.size() + " rules would be subsumed by the rule entered: \n");
//                    for (int k : subsumer) {
//                        System.out.println("Line Number:" + rs.rules.get(k).startLine + " : " + rs.rules.get(k));
//                    }
//                    System.out.println("\n");
//                }
//            }
//            catch (Throwable e) {
//                continue;
//            }
//
//        } while (!input.equals("exit") && !input.equals("quit"));
//    }
    public static void main(String[] args) {

        String[] strings = new String[]{"Amelia flies.", "John walks."};
        Map<Integer, CNF> res = generateCNFForStringSet(strings);
        for (Map.Entry e : res.entrySet()) {
            System.out.println(e);
        }
        Map<Integer, Map<Integer, CNF>> rr = getCommonCNF(res);
        for (Map.Entry e : rr.entrySet()) {
            System.out.println(e);
        }
        Map<CNF, Set<Pair<Integer, Integer>>> re = reverseMap(rr);
        System.out.println(re);
    }

}
