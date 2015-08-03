package com.articulate.sigma.nlp;

import com.articulate.sigma.*;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
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
 *
 * Adding a similarity method based on synset subsumption.
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

    // A bi-directional index of synsets to indexes which are in order of synset number
    private static HashBiMap<String,Integer> nounSynsetToIndex = HashBiMap.create();
    private static HashBiMap<String,Integer> verbSynsetToIndex = HashBiMap.create();
    private static HashBiMap<String,Integer> adjectiveSynsetToIndex = HashBiMap.create();
    private static HashBiMap<String,Integer> adverbSynsetToIndex = HashBiMap.create();

    // a bit is true if the synset (in order of byte offset number) is subsumed by the
    // synset in the key
    private static HashMap<String,BitSet> nouns = new HashMap<>();
    private static HashMap<String,BitSet> verbs = new HashMap<>();
    private static HashMap<String,BitSet> adjectives = new HashMap<>();
    private static HashMap<String,BitSet> adverbs = new HashMap<>();

    // default bitmaps
    private static BitSet nounBits = null;
    private static BitSet verbBits = null;
    private static BitSet adjectiveBits = null;
    private static BitSet adverbBits = null;

    private static boolean ResnikSim = true;
    private static boolean SubsumptionSim = false; // if both are true,
    // subsumption will be used by overwriting the result from Resnik

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
    public static float computeSynsetSim(String s1, String s2) {

        if (s1.charAt(0) != s2.charAt(0)) {
            System.out.println("Error in WNsim.computeSynsetSim(): incompatible synsets" + s1 + " " + s2);
            return 0;
        }
        char pos = s1.charAt(0);
        //System.out.println("Info in WNsim.computeSynsetSim(): pos: " + pos);
        int n = maxFreqs.get(Character.toString(pos));
        //System.out.println("Info in WNsim.computeSynsetSim(): n: " + n);
        String parent = WordNetUtilities.lowestCommonParent(s1, s2);
        if (parent == null)
            return (float) 0;
        //System.out.println("Info in WNsim.computeSynsetSim(): parent: " + parent);
        getSubsumingFreq();
        float parFreq = 0;
        if (getSubsumingFreq().containsKey(parent))
            parFreq= getSubsumingFreq().get(parent);
        //System.out.println("Info in WNsim.computeSynsetSim(): probability: " + parFreq / n);
        //System.out.println("Info in WNsim.computeSynsetSim(): score: " + (-Math.log(parFreq / n)));
        return (float) (-Math.log(parFreq / n));
        //return (float) (1 - (parFreq / n));
    }

    /** ***************************************************************
     * Compute similarity of two words as the maximum similarity among
     * their possible synsets. Note that currently only synsets of the
     * same part of speech have any similarity score.
     * @param allSynsets determines whether to take the best fit from
     *                   all possible synsets for the words, or just
     *                   to compare the most frequent synsets for each
     *                   word
     */
    public static float computeWordSim(String s1, String s2, boolean allSynsets) {

        //System.out.println("Info in WNsim.computeWordSim(): " + s1 + " " + s2);
        //if (subsumingFreq == null)
        //    subsumingFreq = computeSubsumingFreq(Sets.newHashSet("hyponym", "instance hyponym"),
        //            Sets.newHashSet("hypernym","instance hypernym"));
        if (cachedSims.containsKey(s1) && cachedSims.get(s1).containsKey(s2))
            return cachedSims.get(s1).get(s2);
        float result = (float) -100.0;
        HashSet<String> syns1 = WordNetUtilities.wordsToSynsets(s1);
        HashSet<String> syns2 = WordNetUtilities.wordsToSynsets(s2);
        if (!allSynsets) {
            syns1 = new HashSet<String>();
            String sense1 = WSD.getBestDefaultSense(s1);
            if (!sense1.isEmpty())
                syns1.add(sense1);
            syns2 = new HashSet<String>();
            String sense2 = WSD.getBestDefaultSense(s2);
            if (!sense2.isEmpty())
                syns2.add(sense2);
        }
        if (syns1 == null && syns2 == null || syns1.size() == 0 || syns2.size() == 0)
            return 0;
        String bestSyn1 = "";
        String bestSyn2 = "";
        if (syns1 == null || syns2 == null)
            return (float) -100.0;
        for (String syn1 : syns1) {
            for (String syn2 : syns2) {
                if (syn1.charAt(0) == syn2.charAt(0)) {
                    float score = 0;
                    if (ResnikSim)
                        score = computeSynsetSim(syn1,syn2);
                    if (SubsumptionSim)
                        score = maxCommonSynsetOverlap(syn1,syn2);
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
     * @return a set of all the immediate children of the given synset from
     * hyponym and instance hyponym links
     */
    public static HashSet<String> getChildList(String s) {

        HashSet<String> result = new HashSet<String>();
        ArrayList<AVPair> rels = WordNet.wn.relations.get(s);
        for (AVPair avp : rels) {
            if (avp.attribute.equals("hyponym") || avp.attribute.equals("instance hyponym"))
                result.add(avp.value);
        }
        return result;
    }

    /** ***************************************************************
     * @return an integer index into the bit vector
     * @param s is a POS-prefixed synset
     */
    public static int getIndex(String s) {

        int index = -1;
        switch (s.charAt(0)) {
            case '1' : index = nounSynsetToIndex.get(s);
                break;
            case '2' : index = verbSynsetToIndex.get(s);
                break;
            case '3' : index = adjectiveSynsetToIndex.get(s);
                break;
            case '4' : index = adverbSynsetToIndex.get(s);
                break;
        }
        return index;
    }

    /** ***************************************************************
     * @return a bit vector comprising set bits for all the children of
     * the given synset
     * @param s is a POS-prefixed synset
     */
    public static BitSet setBits(String s) {

        //System.out.println("INFO in WNsim.setBits(): " + s);
        HashSet<String> children = getChildList(s);
        BitSet posMap = null;
        int index = -1;
        switch (s.charAt(0)) {
            case '1' : posMap = (BitSet) nounBits.clone();
                index = nounSynsetToIndex.get(s);
                break;
            case '2' : posMap = (BitSet) verbBits.clone();
                index = verbSynsetToIndex.get(s);
                break;
            case '3' : posMap = (BitSet) adjectiveBits.clone();
                index = adjectiveSynsetToIndex.get(s);
                break;
            case '4' : posMap = (BitSet) adverbBits.clone();
                index = adverbSynsetToIndex.get(s);
                break;
        }
        for (String c : children) {
            posMap.or(setBits(c));
            posMap.set(getIndex(c));
        }
        //System.out.println("INFO in WNsim.setBits(): s " + posMap.cardinality());
        switch (s.charAt(0)) {
            case '1' : nouns.put(s, posMap);
                break;
            case '2' : verbs.put(s,posMap);
                break;
            case '3' : adjectives.put(s,posMap);
                break;
            case '4' : adverbs.put(s, posMap);
                break;
        }
        return posMap;
    }

    /** ***************************************************************
     */
    public static void createIndexes() {

        int i = 0;
        for (String s : WordNet.wn.nounDocumentationHash.keySet()) {
            nounSynsetToIndex.put("1" + s,i++);
        }
        i = 0;
        for (String s : WordNet.wn.verbDocumentationHash.keySet()) {
            verbSynsetToIndex.put("2" + s,i++);
        }
        i = 0;
        for (String s : WordNet.wn.adverbDocumentationHash.keySet()) {
            adverbSynsetToIndex.put("3" + s,i++);
        }
        i = 0;
        for (String s : WordNet.wn.adjectiveDocumentationHash.keySet()) {
            adjectiveSynsetToIndex.put("4" + s,i++);
        }

        HashSet<String> leaves = WordNetUtilities.findLeavesInTree(Sets.newHashSet("hyponym", "instance hyponym"));

        // a bit is true if the synset (in order of byte offset number) is subsumed by the
        // synset in the key
        nounBits = new BitSet(nounSynsetToIndex.size());
        verbBits = new BitSet(verbSynsetToIndex.size());
        adjectiveBits = new BitSet(adjectiveSynsetToIndex.size());
        adverbBits = new BitSet(adverbSynsetToIndex.size());

        HashSet<String> roots = WordNetUtilities.findLeavesInTree(Sets.newHashSet("hypernym", "instance hypernym"));
        for (String s : roots) {
            setBits(s);
        }
    }

    /** ***************************************************************
     * Compute the number of child synsets shared by the two synsets
     */
    public static int synsetOverlap(String s1, String s2) {

        int result = 0;
        if (s1.charAt(0) != s2.charAt(0))
            return 0;
        BitSet bs1 = null;
        BitSet bs2 = null;
        switch (s1.charAt(0)) {
            case '1' : bs1 = (BitSet) nouns.get(s1).clone();
                bs2 = (BitSet) nouns.get(s2).clone();
                break;
            case '2' : bs1 = (BitSet) verbs.get(s1).clone();
                bs2 = (BitSet) verbs.get(s2).clone();
                break;
            case '3' : bs1 = (BitSet) adjectives.get(s1).clone();
                bs2 = (BitSet) adjectives.get(s2).clone();
                break;
            case '4' : bs1 = (BitSet) adverbs.get(s1).clone();
                bs2 = (BitSet) adverbs.get(s2).clone();
                break;
        }
        //System.out.println("INFO in WNsim.synsetOverlap(): s1: " + s1 + " " + bs1.cardinality());
        //System.out.println("INFO in WNsim.synsetOverlap(): s2: " + s2 + " " + bs2.cardinality());
        bs1.and(bs2);
        return bs1.cardinality();
    }

    /** ***************************************************************
     * Compute the percentage of child synsets of the first synset
     * shared by the second synset
     */
    public static float synsetPercentOverlap(String s1, String s2) {

        int result = 0;
        if (s1.charAt(0) != s2.charAt(0))
            return 0;
        BitSet bs1 = null;
        BitSet bs2 = null;
        switch (s1.charAt(0)) {
            case '1' : bs1 = (BitSet) nouns.get(s1).clone();
                bs2 = (BitSet) nouns.get(s2).clone();
                break;
            case '2' : bs1 = (BitSet) verbs.get(s1).clone();
                bs2 = (BitSet) verbs.get(s2).clone();
                break;
            case '3' : bs1 = (BitSet) adjectives.get(s1).clone();
                bs2 = (BitSet) adjectives.get(s2).clone();
                break;
            case '4' : bs1 = (BitSet) adverbs.get(s1).clone();
                bs2 = (BitSet) adverbs.get(s2).clone();
                break;
        }
        if (bs1.cardinality() == 0)
            return 0;
        //System.out.println("INFO in WNsim.synsetOverlap(): s1: " + s1 + " " + bs1.cardinality());
        //System.out.println("INFO in WNsim.synsetOverlap(): s2: " + s2 + " " + bs2.cardinality());
        BitSet bs1copy = (BitSet) bs1.clone();
        bs1copy.and(bs2);
        return (float) Math.exp(((float) bs1copy.cardinality()) / ((float) bs1.cardinality()));
        //return (float) -Math.log(((float) bs1copy.cardinality()) / ((float) bs1.cardinality()));
        //return (float) ((float) bs1copy.cardinality()) / ((float) bs1.cardinality());
    }

    /** ***************************************************************
     */
    public static float maxCommonSynsetOverlap(String s1, String s2) {

        float result = 0;
        String parent = WordNetUtilities.lowestCommonParent(s1, s2);
        if (parent == null)
            return  0;
        result = synsetPercentOverlap(parent,s1);
        float newval = synsetPercentOverlap(parent, s2);
        if (newval > result)
            result = newval;
        return result;
    }

    /** ***************************************************************
     */
    public static void test() {

        //System.out.println("INFO in WNsim.test() ");
        HashMap<String,HashMap<String,Float>> hm = readTestFile("/home/apease/WordSim/scws.csv",
                "([^\\t]+)\\t([^\\t]+)\\t(.*)",false);
        //HashMap<String,HashMap<String,Float>> hm = readTestFile("/home/apease/WordSim/wordsim353/combined-noHead.tab",
        //        "([^\\t]+)\\t([^\\t]+)\\t(.*)",false);
        //HashMap<String,HashMap<String,Float>> hm = readTestFile("/home/apease/WordSim/rw/rw.txt",
        //        "([^\\t]+)\\t([^\\t]+)\\t([^\\t]+).*",false);
        //HashMap<String,HashMap<String,Float>> hm = readTestFile("/home/apease/WordSim/Resnik3.txt",
        //        "([^ ]+) ([^ ]+) (.*)",false);
        //HashMap<String,HashMap<String,Float>> hm = readTestFile("/home/apease/WordSim/MEN/MEN_dataset_lemma_form_full",
        //        "([^ ]+) ([^ ]+) (.*)",true);
        HashMap<String,HashMap<String,Float>> hm2 = new HashMap<>();
        HashMap<String,Float> results1 = new HashMap<>();
        HashMap<String,Float> results2 = new HashMap<>();
        for (String s1 : hm.keySet()) {
            HashMap<String,Float> m = hm.get(s1);
            if (m != null) {
                for (String s2 : m.keySet()) {
                    boolean allSynsets = false;
                    float factual = computeWordSim(s1,s2,allSynsets);
                    float fexpected = m.get(s2);
                    if (factual > -100) {
                        HashMap<String, Float> h = new HashMap<>();
                        if (hm2.containsKey(s1))
                            h = hm2.get(s1);
                        h.put(s2, factual);
                        h = new HashMap<>();
                        if (hm2.containsKey(s2))
                            h = hm2.get(s2);
                        h.put(s1, factual);
                        //System.out.println("INFO in WNsim.readTestFile(): s1: " + s1 + " s2: " +
                        //        s2 + " factual: " + factual + " fexpected: " + fexpected);
                        //System.out.println(s1 + ", " + s2 + ", " + factual + ", " + fexpected);
                        results1.put(s1 + "-" + s2, factual);
                        results2.put(s1 + "-" + s2, fexpected);
                    }
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
            createIndexes();
            System.out.println(synsetOverlap("104254777", "104199027"));
            System.out.println(maxCommonSynsetOverlap("104254777", "104199027"));
            //System.out.println(
            getSubsumingFreq(); //.toString().replace(",", "\n"));
            long start = System.currentTimeMillis();
            test();
            long finish = System.currentTimeMillis();
            System.out.println("Total time in milis: " + (finish - start));
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

