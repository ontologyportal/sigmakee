package com.articulate.sigma.nlp;

import com.articulate.sigma.nlp.corpora.ShuZiInsQA;
import com.articulate.sigma.utils.ProgressPrinter;

import java.io.IOException;
import java.util.*;

/**

 */
public class TokenOverlap {

    TFIDF tfidf = null;
    public boolean debug = false;

    /** ***************************************************************
     */
    public TokenOverlap(TFIDF tf) throws IOException {

        //System.out.println("Info in TFIDF(): Initializing");
        tfidf = tf;
    }

    /** ***************************************************************
     * @return an integer score of the number of shared ngrams (minus
     * stopwords and punctuation)
     */
    public int nGramOverlap(String x, String y, int n) {

        //System.out.println("TokenOverlap.overlap(): testing: " + x + " \nand:\n" + y);
        int overlap = 0;
        String str1 = tfidf.removePunctuation(x);
        str1 = tfidf.removeStopWords(str1);
        ArrayList<String> s1 = new ArrayList<String>();
        String[] sspl = str1.split(" ");
        s1.addAll(Arrays.asList(sspl));
        String str2 = tfidf.removePunctuation(y);
        str2 = tfidf.removeStopWords(str2);
        ArrayList<String> s2 = new ArrayList<String>();
        s2.addAll(Arrays.asList(str2.split(" ")));

        for (int i = 0; i < s1.size() - n; i++) {
            StringBuffer s1tok = new StringBuffer();
            for (int z = 0; z < n; z++)
                s1tok.append(s1.get(i + z));
            for (int j = 0; j < s2.size() - n; j++) {
                StringBuffer s2tok = new StringBuffer();
                for (int z = 0; z < n; z++)
                    s2tok.append(s2.get(j+z));
                if (s1tok.equals(s2tok)) {
                    overlap++;
                    System.out.println("TokenOverlap.nGramOverlap(): match: " + s1tok);
                }
            }
        }
        //if (s1.size() > 0)
        //    System.out.println("TokenOverlap.overlap(): common tokens: " + s1);
        return overlap;
    }

    /** ***************************************************************
     * @return an integer score of the number of shared ngrams (minus
     * stopwords and punctuation)
     */
    public int cachedNGramOverlap(HashMap<Integer,HashSet<String>> questions,
                                  HashMap<Integer,HashSet<String>> answers, int n) {

        //System.out.println("TokenOverlap.cachedNGramOverlap():  " + questions + " \nand:\n" + answers);
        HashSet<String> qs = questions.get(n);
        if (qs == null) return 0;
        HashSet<String> as = answers.get(n);
        HashSet<String> result = new HashSet<>(qs);
        result.retainAll(as);
        int overlap = result.size();
        if (debug)
            System.out.println("TokenOverlap.cachedNGramOverlap(): common tokens: " + result);
        return overlap;
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
        String str2 = tfidf.removePunctuation(y);
        str2 = tfidf.removeStopWords(str2);
        Set<String> s2 = new HashSet<String>();
        s2.addAll(Arrays.asList(str2.split(" ")));
        s1.retainAll(s2);
        //if (s1.size() > 0)
        //    System.out.println("TokenOverlap.overlap(): common tokens: " + s1);
        return s1.size();
    }

    /** ***************************************************************
     */
    public static void testOverlap() {

        //String s1 = "do Medicare cover my spouse";
        //String s2 = "if your spouse have work and pay Medicare tax for the entire require 40 quarter or be eligible for Medicare by virtue of be disable or some other reason , your spouse can receive his / her own medicare benefit if your spouse have not meet those qualification , if you have meet them and if your spouse be age 65 he / she can receive Medicare based on your eligibility";
        String s1 = "can you borrow against globe Life Insurance";
        String s2 = "borrowing against a life insurance policy require cash value inside that policy term life insurance do not have cash value but whole life insurance policy may so you will need have a whole life policy with global Life Insurance in order to be able borrow against it call up your company and ask if you have any cash value inside your policy and what the borrowing option and cost be";

        TFIDF cb = null;
        TokenOverlap to = null;
        try {
            cb = new TFIDF("/home/apease/Sigma/KBs/stopwords.txt");
            to = new TokenOverlap(cb);
        }
        catch (IOException ioe) {
            System.out.println("Error in TokenOverlap.devsToInputs()");
            ioe.printStackTrace();
        }
        System.out.println(to.overlap(s1, s2));
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
    public TreeMap<Float,ArrayList<Integer>> computeNGramOverlap(String question) {

        ProgressPrinter pp = new ProgressPrinter(10);
        System.out.print("TokenOverlap.computerNGramOverlap(): ");
        TreeMap<Float,ArrayList<Integer>> result = new TreeMap<>();
        for (String line : tfidf.lines) {
            pp.tick();
            //if (tfidf.lines.indexOf(line) == 8362)
            //    System.out.println("TokenOverlap.computeOverlap(): " + line);
            int score = nGramOverlap(question, line, 2);
            if (score == 0)
                continue;
            float fscore = (float) score;
            ArrayList<Integer> al = new ArrayList<Integer>();
            if (result.containsKey(fscore))
                al = result.get(fscore);
            al.add(tfidf.lines.indexOf(line));
            result.put(fscore,al);
        }
        System.out.println();
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
     * @return a map of scores and the set of document IDs that have that
     * score, which is a count of token overlap with the question
     */
    public TreeMap<Float,ArrayList<Integer>> nGramRank(ShuZiInsQA.Dev dev,
                                                  List<String> toScoreIDs,
                                                  ArrayList<HashMap<Integer,HashSet<String>>> answerNgrams,
                                                  TreeMap<Float,ArrayList<Integer>> scoredIDs) {

        TreeMap<Float,ArrayList<Integer>> result = new TreeMap<>();
        result.putAll(scoredIDs);
        for (String id : toScoreIDs) {
            int intID = Integer.parseInt(id);
            int score = cachedNGramOverlap(dev.questionNgrams, answerNgrams.get(intID), 2);
            if (debug)
                System.out.println("TokenOverlap.nGramRank(): id: " + id + " as int: " + intID + " with score: " + score);
            if (score == 0)
                continue;
            float fscore = (float) score;
            ArrayList<Integer> al = new ArrayList<Integer>();
            if (result.containsKey(fscore))
                al = result.get(fscore);
            al.add(intID);
            result.put(fscore,al);
        }
        if (debug)
            System.out.println("TokenOverlap.nGramRank(): result " + result);
        return result;
    }

    /** ***************************************************************
     */
    public static void main(String[] args) {

        testOverlap();
    }
}
