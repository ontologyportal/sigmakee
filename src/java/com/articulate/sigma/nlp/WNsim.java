package com.articulate.sigma.nlp;

import com.articulate.sigma.*;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * an implementation of
 * Resnik, P. (1995). Using information content to evaluate semantic similarity in a taxonomy.
 * In Proceedings of the 14th International Joint Conference on Artificial Intelligence, 448–453.
 */
public class WNsim {

    // frequency of a given synset
    private static HashMap<String,Integer> subsumingFreq = null;

    // for each synset key, the set of hyponyms and instance hyponyms
    // similarity is -log of
    private static HashMap<String,HashSet<String>> covered = new HashMap<>();

    // opportunistically cache similarities, keys are words, not synsets
    private static HashMap<String,HashMap<String,Float>> cachedSims = new HashMap<>();

    // find maximum values for each POS.  Keys are "1", "2", "3" and "4"
    private static HashMap<String,Integer> maxFreqs = new HashMap<>();

    // a bit is true if the synset (in order of byte offset number) is subsumed by the
    // synset in the key
    private static HashMap<String,BitSet> nouns = null;
    private static HashMap<String,BitSet> verbs = null;
    private static HashMap<String,BitSet> adjectives = null;
    private static HashMap<String,BitSet> adverbs = null;

    /** ***************************************************************
     */
    public static void calcMaxFreqs() {

        maxFreqs.put("1",0);
        maxFreqs.put("2",0);
        maxFreqs.put("3",0);
        maxFreqs.put("4",0);
        for (String s : subsumingFreq.keySet()) {
            if (subsumingFreq.get(s) > maxFreqs.get(s.substring(0,1)))
                maxFreqs.put(s.substring(0, 1), subsumingFreq.get(s));
        }
    }

    /** ***************************************************************
     */
    public static HashMap<String,Integer> getSubsumingFreq() {

        if (subsumingFreq == null) {
            subsumingFreq = new HashMap<String,Integer>();
            computeSubsumingFreq2(Sets.newHashSet("hyponym", "instance hyponym"),
                    Sets.newHashSet("hypernym", "instance hypernym"));
            calcMaxFreqs();
        }
        return subsumingFreq;
    }

    /** ***************************************************************
     */
    public static HashMap<String,Integer> computeSubsumingFreq(HashSet<String> childRels,
                                                               HashSet<String> parentRels)  {

        int safetyCounter = 0;
        HashMap<String,Integer> freqs = new HashMap<>();
        TreeSet<String> ts = new TreeSet<>();
        TreeSet<String> seen = new TreeSet<>();
        ts.addAll(WordNetUtilities.findLeavesInTree(childRels));

        int count = 0;
        //System.out.println();
        //System.out.println("====================================");
        for (String s: ts) {
           // System.out.print(WordNet.wn.getWordsFromSynset(s).get(0)+"-" + s + ", ");
            if (count++ > 6) {
                //System.out.println();
                count = 0;
            }
        }
        //System.out.println();
        //System.out.println("====================================");

        //System.out.println("Iterating:");
        while (!ts.isEmpty() && safetyCounter < 50) {
            TreeSet<String> tsnew = new TreeSet<>();
            for (String synset : ts) {
                if (seen.contains(synset))
                    continue;
                seen.add(synset);
                //System.out.println(WordNet.wn.getWordsFromSynset(synset).get(0) + "-" + synset);
                if (!freqs.keySet().contains(synset)) {
                    freqs.put(synset, new Integer(WordNet.wn.senseFrequencies.get(synset)));
                }
                ArrayList<AVPair> wnrels = WordNet.wn.relations.get(synset);
                for (AVPair avp : wnrels) {
                    if (parentRels.contains(avp.attribute)) {
                        tsnew.add(avp.value);
                        if (freqs.keySet().contains(avp.value))
                            freqs.put(avp.value, freqs.get(avp.value) + freqs.get(synset));
                        else
                            freqs.put(avp.value, freqs.get(synset));
                    }
                }
            }
            ts = new TreeSet<>();
            ts.addAll(tsnew);
            //System.out.println("====================================");
            //System.out.println(tsnew);
            //System.out.println("====================================");
            safetyCounter++;
        }
        //System.out.println("WordNet.wn.computeSubsumingFreq() " + safetyCounter);
        return freqs;
    }

    /** ***************************************************************
     */
    public static int computeSubsumingFreq2(HashSet<String> childRels,
                                            HashSet<String> parentRels) {

        int safetyCounter = 0;
        HashMap<String, Integer> freqs = new HashMap<>();
        TreeSet<String> ts = new TreeSet<>();
        TreeSet<String> seen = new TreeSet<>();
        ts.addAll(WordNetUtilities.findLeavesInTree(childRels));

        for (String s : ts) {
            // System.out.print(WordNet.wn.getWordsFromSynset(s).get(0)+"-" + s + ", ");
            freqs.put(s, new Integer(1));
        }
        return computeSubsumingFreq2("100001740", childRels, parentRels);
    }

    /** ***************************************************************
     */
    public static int computeSubsumingFreq2(String synset,
            HashSet<String> childRels,
            HashSet<String> parentRels)  {

        ArrayList<AVPair> wnrels = WordNet.wn.relations.get(synset);
        int total = 1;
        for (AVPair avp : wnrels) {
            if (childRels.contains(avp.attribute))
                total += computeSubsumingFreq2(avp.value, childRels,parentRels);
        }
        subsumingFreq.put(synset,total);
        return total;
    }

    /** ***************************************************************
     * Compute similarity of two synsets. Note that currently only synsets of the
     * same part of speech have any similarity score.
     */
    public static float computeSynsetSim (String s1, String s2) {

        if (s1.charAt(0) != s2.charAt(0)) {
            System.out.println("Error in WNsim.computeSynsetSim(): incompatible synsets" + s1 + " " + s2);
            return 0;
        }
        char pos = s1.charAt(0);
        //System.out.println("Info in WNsim.computeSynsetSim(): pos: " + pos);
        int n = maxFreqs.get(Character.toString(pos));
        //System.out.println("Info in WNsim.computeSynsetSim(): n: " + n);
        String parent = WordNetUtilities.lowestCommonParent(s1, s2);
        //System.out.println("Info in WNsim.computeSynsetSim(): parent: " + parent);
        //System.out.println("Info in WNsim.computeSynsetSim(): parent freq: " + getSubsumingFreq().get(parent));
        float parFreq = getSubsumingFreq().get(parent);
        //System.out.println("Info in WNsim.computeSynsetSim(): probability: " + parFreq / n);
        //System.out.println("Info in WNsim.computeSynsetSim(): score: " + (-Math.log(parFreq / n)));
        return (float) (-Math.log(parFreq / n));
        //return (float) (1 - (parFreq / n));
    }

    /** ***************************************************************
     * Compute similarity of two words as the maximum similarity among
     * their possible synsets. Note that currently only synsets of the
     * same part of speech have any similarity score.
     */
    public static float computeWordSim (String s1, String s2) {

        //System.out.println("Info in WNsim.computeWordSim(): " + s1 + " " + s2);
        //if (subsumingFreq == null)
        //    subsumingFreq = computeSubsumingFreq(Sets.newHashSet("hyponym", "instance hyponym"),
        //            Sets.newHashSet("hypernym","instance hypernym"));
        if (cachedSims.containsKey(s1) && cachedSims.get(s1).containsKey(s2))
            return cachedSims.get(s1).get(s2);
        float result = (float) -100.0;
        HashSet<String> syns1 = WordNetUtilities.wordsToSynsets(s1);
        HashSet<String> syns2 = WordNetUtilities.wordsToSynsets(s2);
        String bestSyn1 = "";
        String bestSyn2 = "";
        if (syns1 == null || syns2 == null)
            return (float) -100.0;
        for (String syn1 : syns1) {
            for (String syn2 : syns2) {
                if (syn1.charAt(0) == syn2.charAt(0)) {
                    float score = computeSynsetSim(syn1,syn2);
                    //System.out.println("Info in WNsim.computeWordSim(): " + syn1 + " " + syn2 + " " + score);
                    if (score > result) {
                        result = score;
                        bestSyn1 = syn1;
                        bestSyn2 = syn2;
                    }
                }
                else {
                    //System.out.println("Info in WNsim.computeWordSim(): diff. types: " + syn1 + " " + syn2);
                }
            }
        }
        return result;
    }

    /** ***************************************************************
     * Read a space-delimited file of word pairs and their similarity
     * rating.
     */
    public static HashMap<String,HashMap<String,Float>> readTestFile(String filename,
                                                                     String regex, boolean removePOS) {

        HashMap<String,HashMap<String,Float>> sims = new HashMap<>();
        //System.out.println("INFO in WNsim.readTestFile()");
        LineNumberReader lr = null;
        try {
            String line;
            //File testFile = new File("/home/apease/WordSim/Resnik3.txt");
            //File testFile = new File("/home/apease/WordSim/MEN/MEN_dataset_lemma_form_full");
            File testFile = new File(filename);
            FileReader r = new FileReader( testFile );
            lr = new LineNumberReader(r);
            while ((line = lr.readLine()) != null) {
                //System.out.println("INFO in WNsim.readTestFile(): " + line);
                Pattern p = Pattern.compile(regex);
                // Pattern p = Pattern.compile("([^ ]+) ([^ ]+) (.*)");
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String w1 = m.group(1);
                    String w2 = m.group(2);
                    if (removePOS) {
                        w1 = w1.substring(0, w1.lastIndexOf('-'));
                        w2 = w2.substring(0, w2.lastIndexOf('-'));
                    }
                    String n1 = m.group(3);
                    //System.out.println("INFO in WNsim.readTestFile(): results: " + w1 + " " + w2 + " " + n1);
                    float f = Float.parseFloat(n1);
                    HashMap<String,Float> hm = new HashMap<>();
                    if (sims.containsKey(w1))
                        hm = sims.get(w1);
                    hm.put(w2,f);
                    sims.put(w1,hm);
                    hm = new HashMap<>();
                    if (sims.containsKey(w2))
                        hm = sims.get(w2);
                    hm.put(w1,f);
                    sims.put(w2,hm);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (lr != null)
                    lr.close();
            }
            catch (Exception ex) {
            }
        }
        return sims;
    }

    /** ***************************************************************
     */
    public static float mean(HashMap<String,Float> x) {

        float total = 0;
        for (String s : x.keySet()) {
            total += x.get(s);
        }
        return total / (float) x.keySet().size();
    }

    /** ***************************************************************
     */
    public static float deviation(HashMap<String,Float> x, HashMap<String,Float> y,
                                  float meanx, float meany) {

        float dev = 0;
        for (String s : x.keySet()) {
            dev += (x.get(s) - meanx) * (y.get(s) - meany);
        }
        return dev;
    }

    /** ***************************************************************
     */
    public static float sqDeviation(HashMap<String,Float> x, float mean) {

        float dev = 0;
        for (String s : x.keySet()) {
            dev += (x.get(s) - mean) * (x.get(s) - mean);
        }
        return dev;
    }

    /** ***************************************************************
     * determine the sample correlation coefficient
     */
    public static double correlation(HashMap<String,Float> x, HashMap<String,Float> y) {

        float result = 0;
        // find means
        float meanx = mean(x);
        float meany = mean(y);
        float deviation = deviation(x, y, meanx, meany);
        float sqdeviationX = sqDeviation(x, meanx);
        float sqdeviationY = sqDeviation(y, meany);
        return deviation / (Math.sqrt(sqdeviationX * sqdeviationY));
    }

    /** ***************************************************************
     */
    public static void test() {

        //System.out.println("INFO in WNsim.test() ");
        HashMap<String,HashMap<String,Float>> hm = readTestFile("/home/apease/WordSim/rw/rw.txt",
                "([^\\t]+)\\t([^\\t]+)\\t([^\\t]+).*",false);
        //HashMap<String,HashMap<String,Float>> hm = readTestFile("/home/apease/WordSim/MEN/MEN_dataset_lemma_form_full",
        //        "([^ ]+) ([^ ]+) (.*)",true);
        HashMap<String,HashMap<String,Float>> hm2 = new HashMap<>();
        HashMap<String,Float> results1 = new HashMap<>();
        HashMap<String,Float> results2 = new HashMap<>();
        for (String s1 : hm.keySet()) {
            HashMap<String,Float> m = hm.get(s1);
            if (m != null) {
                for (String s2 : m.keySet()) {
                    float factual = computeWordSim(s1,s2);
                    float fexpected = m.get(s2);
                    HashMap<String,Float> h = new HashMap<>();
                    if (hm2.containsKey(s1))
                        h = hm2.get(s1);
                    h.put(s2,factual);
                    h = new HashMap<>();
                    if (hm2.containsKey(s2))
                        h = hm2.get(s2);
                    h.put(s1,factual);
                    System.out.println("INFO in WNsim.readTestFile(): s1: " + s1 + " s2: " +
                            s2 + " factual: " + factual + " fexpected: " + fexpected);
                    results1.put(s1 + "-" + s2, factual);
                    results2.put(s1 + "-" + s2, fexpected);
                }
            }
        }
        System.out.println("Correlation: " + correlation(results1,results2));
    }

    /** ***************************************************************
     *  A main method, used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void main (String[] args) {

        /*
        String line = "explosion stencil 3.000000";
        System.out.println("INFO in WNsim.main(): " + line);
        Pattern p = Pattern.compile("([^ ]+) ([^ ]+) (.*)");
        Matcher m = p.matcher(line);
        if (m.matches()) {
            String w1 = m.group(1);
            String w2 = m.group(2);
            String n1 = m.group(3);
            System.out.println("results: " + w1 + " " + w2 + " " + n1);
        }
*/
        try {
            KBmanager.getMgr().initializeOnce();
            //System.out.println(
            getSubsumingFreq(); //.toString().replace(",", "\n"));
            test();
            //System.out.println("Similarity: " + computeSynsetSim("102858304", "102958343"));
            //System.out.println("Similarity of car and boat: " + computeWordSim("car", "boat"));
            //String parent = WordNetUtilities.lowestCommonParent("102858304", "102958343");

            //System.out.println("frequency of " + parent + " : " + getSubsumingFreq().get(parent));
            //System.out.println("Similarity: " +
            //        (-Math.log(getSubsumingFreq().get(parent) / WordNetUtilities.numSynsets('1'))));
            //for (String s: hm.keySet()) {
            //    System.out.print(WordNet.wn.getWordsFromSynset(s).get(0) + "-" + s + ":" + hm.get(s) + ", ");
            //    if (count++ > 6) {
            //        System.out.println();
            //        count = 0;
            //    }
            //}
        }
        catch (Exception e) {
            System.out.println("Error in WordNetUtilities.main(): Exception: " + e.getMessage());
            e.printStackTrace();
        }

    }
}

