package com.articulate.sigma.nlp;

import com.articulate.sigma.nlp.corpora.ShuZiInsQA;
import com.articulate.sigma.utils.ProgressPrinter;

import java.io.IOException;
import java.util.*;

/**
 * Created by apease on 10/1/15.
 */
public class NGramOverlap {

    TFIDF tfidf = null;
    public boolean debug = false;

    /** ***************************************************************
     */
    public NGramOverlap(TFIDF tf) throws IOException {

        //System.out.println("Info in TFIDF(): Initializing");
        tfidf = tf;
    }

    /** ***************************************************************
     * @return an integer score of the number of shared ngrams (minus
     * punctuation)
     */
    public int nGramOverlap(String x, String y, int n) {

        //System.out.println("TokenOverlap.overlap(): testing: " + x + " \nand:\n" + y);
        int overlap = 0;
        HashSet<String> common = new HashSet<>();
        String str1 = tfidf.removePunctuation(x);
        //str1 = tfidf.removeStopWords(str1);
        ArrayList<String> s1 = new ArrayList<String>();
        String[] sspl = str1.split(" ");
        s1.addAll(Arrays.asList(sspl));

        String str2 = tfidf.removePunctuation(y);
        //str2 = tfidf.removeStopWords(str2);
        ArrayList<String> s2 = new ArrayList<String>();
        s2.addAll(Arrays.asList(str2.split(" ")));

        for (int i = 0; i < s1.size()+1 - n; i++) {
            StringBuffer s1tok = new StringBuffer();
            for (int z = 0; z < n; z++)
                s1tok.append(s1.get(i + z));
            for (int j = 0; j < s2.size()+1 - n; j++) {
                StringBuffer s2tok = new StringBuffer();
                for (int z = 0; z < n; z++)
                    s2tok.append(s2.get(j+z));
                //System.out.println("'" + s1tok + "' '" + s2tok + "'");
                if (s1tok.toString().equals(s2tok.toString())) {
                    overlap++;
                    common.add(s1tok.toString());
                    //System.out.println("TokenOverlap.nGramOverlap(): match: " + s1tok);
                }
            }
        }
        if (common.size() > 0)
            System.out.println("TokenOverlap.overlap(): common tokens: " + common);
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
     * @return a map of scores and the set of document IDs that have that
     * score, which is a count of token overlap with the question

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
*/
    /** ***************************************************************
     * @return a map of scores and the set of document IDs that have that
     * score, which is a count of token overlap with the question
     */
    public TreeMap<Float,ArrayList<Integer>> nGramRank(ShuZiInsQA.Dev dev,
                                                       List<String> toScoreIDs,
                                                       ArrayList<HashMap<Integer,HashSet<String>>> answerNgrams,
                                                       TreeMap<Float,ArrayList<Integer>> scoredIDs, int n) {

        TreeMap<Float,ArrayList<Integer>> result = new TreeMap<>();
        result.putAll(scoredIDs);
        for (String id : toScoreIDs) {
            int intID = Integer.parseInt(id);
            int score = cachedNGramOverlap(dev.questionNgrams, answerNgrams.get(intID), n);
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

        TFIDF cb = null;
        TokenOverlap to = null;
        NGramOverlap ng = null;
        try {
            cb = new TFIDF("/home/apease/Sigma/KBs/stopwords.txt");
            ng = new NGramOverlap(cb);
        }
        catch (IOException ioe) {
            System.out.println("Error in ShuZiInsQA.devsToInputs()");
            ioe.printStackTrace();
        }
        System.out.println(ng.nGramOverlap("John likes big trees in the night", "John likes small leaves in the night", 3));
    }
}
