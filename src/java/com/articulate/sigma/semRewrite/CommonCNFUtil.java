package com.articulate.sigma.semRewrite;/*
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
import com.google.common.collect.Lists;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;

public class CommonCNFUtil {

    static List<String> ignorePreds = Arrays.asList(new String[]{"number", "tense"/**, "root", "names"**/});
    static KBmanager kbm;
    static KB kb;

    static {
        kbm = KBmanager.getMgr();
        kbm.initializeOnce();
        kb = kbm.getKB("SUMO");
    }

    private static Comparator<Clause> clauseComparator = new Comparator<Clause>() {

        @Override
        public int compare(Clause o1, Clause o2) {

            return o1.disjuncts.get(0).pred.compareTo(o2.disjuncts.get(0).pred);
        }
    };

    /***********************************************************
     * prevent instantiation
     */
    private CommonCNFUtil() {

    }

    /***********************************************************
     * save the intermediate parsing result to JSON file
     */
    public static String saveCNFMaptoFile(List<QAPair> list, String path) {

        File f = new File(path);
        if (!f.exists())
            try {
                f.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        System.out.println(f.getAbsolutePath());
        try (PrintWriter pw = new PrintWriter(f)) {
            JSONArray arr = new JSONArray();
            for (QAPair k : list) {
                JSONObject obj = new JSONObject();
                obj.put("file", k.file);
                obj.put("index", "" + k.index);
                obj.put("query", k.query);
                obj.put("queryCNF", k.queryCNF.toString());
                obj.put("answer", k.answer);
                obj.put("answerCNF", k.answerCNF.toString());
                arr.add(obj);
            }
            System.out.println(arr.toJSONString());
            pw.print(arr.toJSONString());
            pw.flush();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return f.getAbsolutePath();
    }

    /***********************************************************
     * load the intermediate parsing result to save time
     */
    public static List<QAPair> loadCNFMapfromFile(String path) {

        JSONParser jp = new JSONParser();
        List<QAPair> res = new ArrayList<QAPair>();
        try {
            JSONArray arr = (JSONArray) jp.parse(new FileReader(path));
            Iterator<JSONObject> iterator = arr.iterator();
            while (iterator.hasNext()) {
                JSONObject obj = iterator.next();
                Integer index = Integer.parseInt((String) obj.get("index"));
                String query = (String) obj.get("query");
                String answer = (String) obj.get("answer");
                String file = (String) obj.get("file");
                QAPair item = new QAPair(index, file, query, answer);
                String k = (String) obj.get("queryCNF");
                CNF cnf = CNF.parseSimple(new Lexer(k));
                k = (String) obj.get("answerCNF");
                CNF cnf2 = CNF.parseSimple(new Lexer(k));
                item.queryCNF = cnf;
                item.answerCNF = cnf2;
                res.add(item);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return res;
    }

    /***********************************************************
     * load sentences from file, one line one sentence
     */
    public static String[] loadSentencesFromTxt(String path) {

        ArrayList<String> res = new ArrayList<String>();
        try (Scanner in = new Scanner(new FileReader(path))) {
            while (in.hasNextLine()) {
                String line = in.nextLine();
                res.add(line);
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return res.toArray(new String[res.size()]);
    }

    /***********************************************************
     * load sentences from file, one line one sentence
     * return map
     */
    public static Map<Integer, String> loadSentencesMap(String path) {

        Map<Integer, String> res = new HashMap<Integer, String>();
        try (Scanner in = new Scanner(new FileReader(path))) {
            int index = 0;
            while (in.hasNextLine()) {
                String line = in.nextLine();
                res.put(index++, line);
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return res;
    }

    /***********************************************************
     * load sentences from "IRtest.json" like QA pair file
     */
    public static List<QAPair> loadSentencesFormJsonFile(String path) {

        JSONParser jp = new JSONParser();
        List<QAPair> res = new ArrayList<QAPair>();
        try {
            JSONArray arr = (JSONArray) jp.parse(new FileReader(path));
            Iterator<JSONObject> iterator = arr.iterator();
            int i = 0;
            while (iterator.hasNext()) {
                JSONObject obj = iterator.next();
                String filename = (String) obj.get("file");
                String query = (String) obj.get("query");
                String answer = (String) obj.get("answer");
                QAPair item = new QAPair(i++, filename, query, answer);
                res.add(item);
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    /***********************************************************
     * get rid of "sumo(X,Y)" and other terms defined in ignorePreds
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
                    if (m != null && m.disjuncts != null && m.disjuncts.get(0)
                            != null && m.disjuncts.get(0).arg1 != null && m.disjuncts.get(0).arg1.equals(word)) {
                        m.disjuncts.get(0).arg1 = sumoTerm;
                    }
                    if (m != null && m.disjuncts != null && m.disjuncts.get(0)
                            != null && m.disjuncts.get(0).arg2 != null && m.disjuncts.get(0).arg2.equals(word)) {
                        m.disjuncts.get(0).arg2 = sumoTerm;
                    }
                }
                // remove sumo() clauses
                iterator.remove();
            }
        }
        return cnf;
    }

    /***********************************************************
     */
    public static void generateCNFForQAPairs(List<QAPair> list) {

        Map<Integer, CNF> res = new HashMap<Integer, CNF>();
        Interpreter inter = new Interpreter();
        KBmanager.getMgr().initializeOnce();
        inter.initialize();
        for (QAPair q : list) {
            CNF cnf = inter.interpretGenCNF(q.query);
            cnf = preProcessCNF(cnf);
            System.out.println(cnf);
            q.queryCNF = cnf;
            cnf = inter.interpretGenCNF(q.answer);
            cnf = preProcessCNF(cnf);
            System.out.println(cnf);
            q.answerCNF = cnf;
        }
    }

    /***********************************************************
     */
    public static Map<Integer, CNF> generateCNFForStringSet(Map<Integer, String> sentences) {

        Map<Integer, CNF> res = new HashMap<Integer, CNF>();
        Interpreter inter = new Interpreter();
        KBmanager.getMgr().initializeOnce();
        inter.initialize();
        for (Integer index : sentences.keySet()) {
            String q = sentences.get(index);
            try {
                CNF cnf = inter.interpretGenCNF(q);
                cnf = preProcessCNF(cnf);
                System.out.println(cnf);
                res.put(index, cnf);
            }
            catch (Exception e) {
                System.out.println("Exception occurs in " + q);
                e.printStackTrace();
            }
        }
        return res;
    }

    /***********************************************************
     */
    public static CNF findOneCommonCNF(Collection<CNF> input) {

        CNF res = new CNF();
        boolean isFirst = true;
        System.out.println("Among the following CNF: \n");
        for (CNF c : input) {
            System.out.println(c);
            if (!isFirst) {
                res = unification(res, c);
            }
            else {
                res = c;
                isFirst = false;
            }
        }
        System.out.println("\n The common CNF is " + res);
        return res;
    }

    /***********************************************************
     */
    public static CNF unification(CNF unifier, CNF unified) {

        CNF rescnf = new CNF();
        unifier.clauses.sort(clauseComparator);
        unified.clauses.sort(clauseComparator);
        for (Clause m : unifier.clauses) {
            for (Clause n : unified.clauses) {
                Clause h = m.deepCopy();
                n = n.deepCopy();
                Clause c=isRelated(h, n);
                if (c!=null) {
                    rescnf.clauses.add(c);
                    break;
                }
            }
        }
        return rescnf;
    }

    /***********************************************************
     */
    public static Clause isRelated(Clause m, Clause n) {

        if (!m.disjuncts.get(0).pred.equals(n.disjuncts.get(0).pred))
            return null;
        String marg1 = m.disjuncts.get(0).arg1;
        String narg1 = n.disjuncts.get(0).arg1;
        String marg2 = m.disjuncts.get(0).arg2;
        String narg2 = n.disjuncts.get(0).arg2;
        String ca = findCommonAncesstor(marg1, narg1);
        if (ca != null) {
            marg1 = ca;
            narg1 = ca;
        }
        String ca1 = findCommonAncesstor(marg2, narg2);
        if (ca1 != null) {
            marg2 = ca1;
            narg2 = ca1;
        }
        if (ca != null && ca1 != null) {
            Literal l=new Literal();
            l.pred=m.disjuncts.get(0).pred;
            l.arg1=ca;
            l.arg2=ca1;
            Clause res=new Clause();
            res.disjuncts= Lists.newArrayList(l);
            return res;
        }
        return null;
    }

    /***********************************************************
     */
    public static void transformQAPairListtoCNFSet(List<QAPair> list, Map<Integer, String> sentences, Map<Integer, CNF> cnfs) {

        int index = 0;
        for (QAPair q : list) {
            sentences.put(index, q.query);
            cnfs.put(index, q.queryCNF);
            index++;
            sentences.put(index, q.answer);
            cnfs.put(index, q.answerCNF);
            index++;
        }
    }

    /***********************************************************
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

    /***********************************************************
     */
    public static String findCommonAncesstor(String s1, String s2) {

        if ((s1.contains("\"") || s1.contains("-")) || (s2.contains("-") || s2.contains("\"")) || (s1.startsWith("?") || (s2.startsWith("?"))))
            return "?X";
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

    /***********************************************************
     * get Common objects between two collections
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

    /***********************************************************
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

    /***********************************************************
     */
    public static class Pair<F, S> {

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

    /***********************************************************
     */
    public static class QAPair {

        Integer index;
        String file;
        String query;
        String answer;
        CNF queryCNF;
        CNF answerCNF;

        public QAPair(Integer index, String file, String query, String answer) {

            this.index = index;
            this.file = file;
            this.query = query;
            this.answer = answer;
        }

        public String toString() {

            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"index\":\"" + index + "\",\n");
            sb.append("  \"file\":\"" + file + "\",\n");
            sb.append("  \"query\":\"" + query + "\",\n");
            sb.append("  \"queryCNF\":\"" + queryCNF + "\",\n");
            sb.append("  \"answer\":\"" + answer + "\",\n");
            sb.append("  \"answerCNF\":\"" + answerCNF + "\"\n");
            sb.append("}\n");
            return sb.toString();
        }
    }

    /***********************************************************
     * function to load text file and generate one common CNF for all sentences,
     * one sentence one line.
     */
    public static CNF loadFileAndFindCommonCNF(String path) {

        Map<Integer, String> strs = loadSentencesMap(path);
        Map<Integer, CNF> cnfMap = CommonCNFUtil.generateCNFForStringSet(strs);
        System.out.println("\nSentences are:\n");
        for (Integer i : strs.keySet()) {
            System.out.println(strs.get(i));
            System.out.println(cnfMap.get(i));
        }
        CNF cnf = CommonCNFUtil.findOneCommonCNF(cnfMap.values());
        return cnf;
    }

    /***********************************************************
     */
    public static void testJSONQAPair() {

        List<QAPair> list = loadSentencesFormJsonFile("test/corpus/java/resources/IRtests.json");
        generateCNFForQAPairs(list);
        String path = saveCNFMaptoFile(list, "cache.json");
        list = loadCNFMapfromFile("cache.json");
        for (QAPair e : list)
            System.out.println(e);
        Map<Integer, CNF> cnfs = new HashMap<Integer, CNF>();
        Map<Integer, String> sentences = new HashMap<Integer, String>();
        transformQAPairListtoCNFSet(list, sentences, cnfs);
        Map<Integer, Map<Integer, CNF>> rr = getCommonCNF(cnfs);
        for (Map.Entry e : rr.entrySet()) {
            System.out.println(e);
        }
        Map<CNF, Set<Pair<Integer, Integer>>> re = reverseMap(rr);
        for (CNF cnf : re.keySet()) {
            System.out.println(cnf.toString() + re.get(cnf));
        }
    }

    public static void testFile() {

        String path = "/Users/peigenyou/workspace/test.txt";
        CNF cnf = loadFileAndFindCommonCNF(path);
    }

    /***********************************************************
     */
    public static void main(String[] args) {

//        String[] strings = new String[]{"Amelia flies.", "John walks."};
//        testJSONQAPair();
        testFile();
    }
}