package com.articulate.sigma.nlp;

import com.articulate.sigma.KBmanager;
import com.articulate.sigma.WSD;
import com.articulate.sigma.WordNet;
import com.articulate.sigma.nlp.corpora.ShuZiInsQA;
import com.articulate.sigma.utils.ProgressPrinter;

import java.io.IOException;
import java.util.*;

/**
 * Created by apease on 10/12/15.
 */
public class SynsetOverlap {

    TFIDF tfidf = null;
    public boolean debug = false;

    /** ***************************************************************
     */
    public SynsetOverlap(TFIDF tf) throws IOException {

        //System.out.println("Info in TFIDF(): Initializing");
        tfidf = tf;
        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    /** ***************************************************************
     * @return an integer score of the number of shared tokens (minus
     * stopwords and punctuation)
     */
    public int overlap(String x, String y) {

        //System.out.println("TokenOverlap.overlap(): testing: " + x + " \nand:\n" + y);
        String str1 = tfidf.removePunctuation(x);
        str1 = tfidf.removeStopWords(str1);
        Set<String> s1 = new HashSet<String>();
        String[] sspl = str1.split(" ");
        s1.addAll(Arrays.asList(sspl));
        Set<String> s1synsets = new HashSet<>();
        for (String s : s1)
            s1synsets.add(WSD.getBestDefaultSense(s));
        String str2 = tfidf.removePunctuation(y);
        str2 = tfidf.removeStopWords(str2);
        Set<String> s2 = new HashSet<String>();
        s2.addAll(Arrays.asList(str2.split(" ")));
        Set<String> s2synsets = new HashSet<>();
        for (String s : s2)
            s2synsets.add(WSD.getBestDefaultSense(s));
        s1synsets.retainAll(s2synsets);
        //if (s1synsets.size() > 0)
        //    System.out.println("SynsetOverlap.overlap(): common tokens: " + s1synsets);
        return s1synsets.size();
    }

    /** ***************************************************************
     */
    public static void testOverlap() {

        //String s1 = "do Medicare cover my spouse";
        //String s2 = "if your spouse have work and pay Medicare tax for the entire require 40 quarter or be eligible for Medicare by virtue of be disable or some other reason , your spouse can receive his / her own medicare benefit if your spouse have not meet those qualification , if you have meet them and if your spouse be age 65 he / she can receive Medicare based on your eligibility";
        String s1 = "can you borrow against globe Life Insurance";
        String s2 = "borrowing against a life insurance policy require cash value inside that policy term life insurance do not have cash value but whole life insurance policy may so you will need have a whole life policy with global Life Insurance in order to be able borrow against it call up your company and ask if you have any cash value inside your policy and what the borrowing option and cost be";

        TFIDF cb = null;
        SynsetOverlap so = null;
        try {
            cb = new TFIDF("/home/apease/Sigma/KBs/stopwords.txt");
            so = new SynsetOverlap(cb);
        }
        catch (IOException ioe) {
            System.out.println("Error in TokenOverlap.devsToInputs()");
            ioe.printStackTrace();
        }
        System.out.println(so.overlap(s1, s2));
    }

    /** ***************************************************************
     * @return a map of scores and the set of document IDs that have that
     * score, which is a count of token overlap with the question
     */
    public TreeMap<Float,ArrayList<Integer>> computeOverlap(String question) {

        TreeMap<Float,ArrayList<Integer>> result = new TreeMap<>();
        for (String line : tfidf.lines) {
            //if (tfidf.lines.indexOf(line) == 8362)
            //    System.out.println("TokenOverlap.computeOverlap(): " + line);
            int score = overlap(question,line);
            if (score == 0)
                continue;
            float fscore = (float) score;
            ArrayList<Integer> al = new ArrayList<Integer>();
            if (result.containsKey(fscore))
                al = result.get(fscore);
            al.add(tfidf.lines.indexOf(line));
            result.put(fscore,al);
        }
        return result;
    }

    /** ***************************************************************
     * @return a map of scores and the set of document IDs that have that
     * score, which is a count of token overlap with the question
     */
    public TreeMap<Float,ArrayList<Integer>> rank(String question,
                                                  List<String> toScoreIDs,
                                                  TreeMap<Float,ArrayList<Integer>> scoredIDs) {

        TreeMap<Float,ArrayList<Integer>> result = new TreeMap<>();
        result.putAll(scoredIDs);
        for (String id : toScoreIDs) {
            int intID = Integer.parseInt(id);
            //System.out.println("TokenOverlap.rank(): id: " + id + " as int: " + intID);
            //if (tfidf.lines.indexOf(line) == 8362)
            //    System.out.println("TokenOverlap.rank(): " + line);
            int score = overlap(question, tfidf.lines.get(intID));
            if (score == 0)
                continue;
            float fscore = (float) score;
            ArrayList<Integer> al = new ArrayList<Integer>();
            if (result.containsKey(fscore))
                al = result.get(fscore);
            al.add(intID);
            result.put(fscore,al);
        }

        return result;
    }

    /** ***************************************************************
     */
    public static void main(String[] args) {

        testOverlap();
    }
}
