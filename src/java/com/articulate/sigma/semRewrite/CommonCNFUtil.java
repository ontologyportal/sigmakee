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
    private CommonCNFUtil(){}


    public static String saveCNFMaptoFile(Map<Integer,CNF> map, String[] input, String path){
        File f=new File(path);
        if(!f.exists())
            try {
                f.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        System.out.println(f.getAbsolutePath());
        try(PrintWriter pw=new PrintWriter(f)){
            JSONArray arr=new JSONArray();
            for(Integer k:map.keySet()){
                JSONObject obj=new JSONObject();
                obj.put("index",k.toString());
                obj.put("sentence",input[k]);
                obj.put("CNF",""+map.get(k)+"");
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
    public static void loadCNFMapfromFile(String path,Map<Integer,CNF> map,ArrayList<String> strs){
        JSONParser jp=new JSONParser();
        try {
            JSONArray arr= (JSONArray) jp.parse(new FileReader(path));
            Iterator<JSONObject> iterator=arr.iterator();
            while(iterator.hasNext()){
                JSONObject obj=iterator.next();
                String k=(String)obj.get("CNF");
                CNF cnf=CNF.parseSimple(new Lexer(k));
                map.put(Integer.parseInt((String)obj.get("index")),cnf);
                strs.add((String)obj.get("sentence"));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static String[] loadSentencesFromTxt(String path){

        ArrayList<String> res=new ArrayList<String>();
        try(Scanner in=new Scanner(new FileReader(path))){
            while(in.hasNextLine()){
                String line=in.nextLine();
                res.add(line);
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return res.toArray(new String[res.size()]);
    }

    public static String[] loadSentencesFormJsonFile(String path){
        JSONParser jp=new JSONParser();
        ArrayList<String> res=new ArrayList<String>();
        try{
            JSONArray arr= (JSONArray) jp.parse(new FileReader(path));
            Iterator<JSONObject> iterator=arr.iterator();
            while(iterator.hasNext()){
                JSONObject obj=iterator.next();
                String sen=(String)obj.get("query");
                res.add(sen);
                sen= (String) obj.get("answer");
                res.add(sen);
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
        return res.toArray(new String[res.size()]);
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
                // remove sumo() clauses
                iterator.remove();
            }
        }
        return cnf;
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
            System.out.println(input[i]);
            CNF cnf = inter.interpretGenCNF(input[i]).get(0);
            System.out.println(cnf);
            cnf = preProcessCNF(cnf);
            res.put(i, cnf);
        }
        return res;
    }

    public static CNF findOneCommonCNF(Collection<CNF> input){
        CNF res=new CNF();
        boolean isFirst=true;
        for(CNF c:input){
            if(!isFirst){
                res=unification(res,c);
            }
            else{
                res=c;
                isFirst=false;
            }
        }
        return res;
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
        String marg1=m.disjuncts.get(0).arg1;
        String narg1=n.disjuncts.get(0).arg1;
        String marg2=m.disjuncts.get(0).arg2;
        String narg2=n.disjuncts.get(0).arg2;
        if (ca != null) {
            marg1= ca;
            narg1= ca;
        }
        String ca1 = findCommonAncesstor(m.disjuncts.get(0).arg2, n.disjuncts.get(0).arg2);
        if (ca1 != null) {
            marg2 = ca1;
            narg2 = ca1;
        }
        if (ca != null){
            if(ca1!=null)
                return true;
            if((marg2.matches("\".*\"")|| marg2.matches(".*-.*"))&&(narg2.matches("\".*\"")|| narg2.matches(".*-.*")))
                return true;
        }
        else if(ca1!=null){
            if((marg1.matches("\".*\"")|| marg1.matches(".*-.*"))&&(narg1.matches("\".*\"")|| narg1.matches(".*-.*")))
                return true;
        }
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
}
