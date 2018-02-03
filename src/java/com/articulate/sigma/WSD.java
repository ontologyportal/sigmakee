package com.articulate.sigma;

import java.io.*;
import java.util.*;

import com.google.common.collect.Lists;

import static com.articulate.sigma.WordNetUtilities.isValidKey;

public class WSD {

    public static int threshold = 5;
    public static int gap = 5;
    public static boolean debug = false;

    /** ***************************************************************
     * Collect all the SUMO terms that are found in the sentence.
     */
    public static ArrayList<String> collectSUMOFromWords(String sentence) {

        //System.out.println("INFO in WordNet.collectSUMOFromWords(): " + sentence);
        ArrayList<String> senses = collectWordSenses(sentence);
        ArrayList<String> result = new ArrayList<String>();
        for (int i = 0; i < senses.size(); i++) {
            String SUMO = WordNetUtilities.getBareSUMOTerm(WordNet.wn.getSUMOMapping(senses.get(i)));
            if (!StringUtil.emptyString(SUMO))
                result.add(SUMO);
        }
        return result;
    }

    /** ***************************************************************
     */
    public static boolean polysemous(String word) {

        ArrayList<String> values = WordNet.wn.wordsToSenseKeys.get(word);
        if (values == null || values.size() == 0)
            return false;
        if (values.size() == 1)
            return false;
        return true;
    }

    /** ***************************************************************
     */
    public static boolean polysemous(String word, int pos) {

        ArrayList<String> values = WordNet.wn.wordsToSenseKeys.get(word);
        if (values == null || values.size() == 0)
            return false;
        if (values.size() == 1)
            return false;
        boolean done = false;
        int count = 0;
        Iterator<String> it = values.iterator();
        while (it.hasNext()) {
            String s = it.next();
            if (!WordNetUtilities.isValidKey(s))
                continue;
            String POS = WordNetUtilities.getPOSfromKey(s);
            String num = WordNetUtilities.posLettersToNumber(POS);
            if (num.equals(Integer.toString(pos)))
                count++;
            if (count > 1)
                return true;
        }
        return false;
    }

    /** ***************************************************************
     * Collect all the synsets that represent the best guess at
     * meanings for all the words in a text given a larger linguistic
     * context.  
     * @return 9 digit synset IDs 
     */
    public static ArrayList<String> collectWordSenses(String text) {

        //System.out.println("INFO in WordNet.collectWordSenses(): " + text);
        String newtext = StringUtil.removeHTML(text);
        newtext = StringUtil.removePunctuation(newtext);
        String context = newtext;
        text = newtext;
        ArrayList<String> result = new ArrayList<String>();
        String wordResult = "";
        ArrayList<String> al = WordNet.splitToArrayList(text);
        ArrayList<String> alcon = WordNet.splitToArrayList(context);
        for (int i = 0; i < al.size(); i++) {
            String word = (String) al.get(i);
            ArrayList<String> multiWordResult = new ArrayList<String>();
            int wordIndex = WordNet.wn.getMultiWords().findMultiWord(al, i, multiWordResult);
            if (wordIndex != i) {
                //String theMultiWord = WordNet.wn.synsetsToWords.get(multiWordResult.get(0)).get(0);
                result.add(multiWordResult.get(0));
                //wordResult = wordResult + " " + theMultiWord;
                i = wordIndex;
            }
            else {
                if (!WordNet.wn.isStopWord(word)) {
                    String synset = findWordSenseInContext(word,alcon);
                    //System.out.println("INFO in WordNet.collectWordSenses(): sense in context: " + synset);
                    if (!StringUtil.emptyString(synset)) {
                        result.add(synset);
                    }
                    else {
                        synset = getBestDefaultSense(word);
                        //System.out.println("INFO in WordNet.collectWordSenses(): default sense: " + synset);
                        if (!StringUtil.emptyString(synset)) {
                            result.add(synset);
                        }
                    }
                }
            }
        }
        //System.out.println("INFO in WordNet.collectWordSenses(): result: " + result);
        return result;
    }
    
    /** ***************************************************************
     * Return the best guess at the synset for the given word in the
     * context of the sentence.  @return the 9-digit synset but only
     * if there's a reasonable amount of data, otherwise return the most
     * frequent sense.
     */
    public static String findWordSenseInContext(String word, List<String> words) {

        //System.out.println("INFO in findWordSenseInContext(): word, words: " + word + ", " + words);
        int bestScore = -1;
        String bestSynset = "";
        for (int i = 1; i <= 4; i++) {
            String newWord = "";
            if (i == 1)
                newWord = WordNet.wn.nounRootForm(word,word.toLowerCase());
            if (i == 2)
                newWord = WordNet.wn.verbRootForm(word,word.toLowerCase());
            if (newWord != null && newWord != "")
                word = newWord;
            TreeSet<AVPair> al = findWordSensePOS(word, words, i);
            if (al != null && al.size() > 0) {
                AVPair avp1 = al.pollLast();
                bestScore = Integer.parseInt(avp1.attribute);
                bestSynset = avp1.value;
            }
        }
        //System.out.println("INFO in findWordSenseInContext(): best synset: " + bestSynset);
        //System.out.println("INFO in findWordSenseInContext(): best score: " + bestScore);
        if (bestScore > 5)
            return bestSynset;
        else
            return getBestDefaultSense(word);
    }

    /** ***************************************************************
     * Return the best guess at the synset for the given word in the
     * context of the sentence with the given POS.
     * @param word - word to disambiguate
     * @param words - words in context
     * @param pos - part of speech of @word
     * @return the 9-digit synset but only if there's a reasonable amount of data.
     */
    public static String findWordSenseInContextWithPos(String word, List<String> words, int pos, boolean lemma) {

        //System.out.println("INFO in WSD.findWordSenseInContextWithPos(): word, words: " +
        //        word + ", " + words);
        int bestScore = -1;
        int nextBestScore = -1;
        String bestSynset = "";
        String nextBestSynset = "";
        if (!lemma) {
            String newWord = "";
            if (pos == 1)
                newWord = WordNet.wn.nounRootForm(word, word.toLowerCase());
            if (pos == 2)
                newWord = WordNet.wn.verbRootForm(word, word.toLowerCase());
            if (!StringUtil.emptyString(newWord))
                word = newWord;
        }
        TreeSet<AVPair> al = findWordSensePOS(word, words, pos);
        if (al != null && al.size() > 0) {
            AVPair avp1 = al.pollLast();
            bestScore = Integer.parseInt(avp1.attribute);
            bestSynset = avp1.value;
            if (al != null && al.size() > 0) {
                AVPair avp2 = al.pollLast();
                nextBestScore = Integer.parseInt(avp2.attribute);
                nextBestSynset = avp2.value;
            }
        }
        //if (polysemous(word,pos) && bestScore != -1)
        //    System.out.println("INFO in WSD.findWordSenseInContextWithPos(): best, next " +
        //        bestSynset + ":" + bestScore + ", " + nextBestSynset  + ":" + nextBestScore +
        //        " for word " + word + " pos: " + pos + " words: " + words);
        if (bestScore > threshold && (bestScore - nextBestScore) > gap)
            return bestSynset;
        else
            return getBestDefaultSense(word,pos);
    }

    /** ***************************************************************
     * Check if a sense has been created from a domain ontology and give
     * it priority.  Returns an ArrayList consisting of
     * a 9-digit WordNet synset, and the score
     * reflecting the quality of the guess the given synset is the right one.
     */
    private static List<String> termFormatBypass(String word) {

        //System.out.println("INFO in WSD.termFormatBypass(): word: " + word +
        //        " : " + WordNet.wn.caseMap.get(word.toUpperCase())
        //);
        ArrayList<String> result = new ArrayList<String>();
        TreeSet<AVPair> senselist = WordNet.wn.wordFrequencies.get(word);
        if (senselist == null) {
            if (WordNet.wn.caseMap.keySet().contains(word.toUpperCase())) {
                word = WordNet.wn.caseMap.get(word.toUpperCase());
                //System.out.println("INFO in WSD.termFormatBypass(): word: " + word);
                senselist = WordNet.wn.wordFrequencies.get(word);
                //System.out.println("INFO in WSD.termFormatBypass(): senselist: " + senselist);
                if (senselist == null)
                    return null;
            }
            else
                return null;
        }
        //System.out.println("INFO in WSD.termFormatBypass(): senselist: " + senselist);
        for (AVPair avp : senselist) {
            if (avp.attribute.equals("99999")) {
                String synset = avp.value;
                result.add(synset);
                result.add("99999");
                String SUMO = WordNet.wn.getSUMOMapping(synset);
                //System.out.println("INFO in WSD.termFormatBypass(): word, avp, synset, sumo: " +
                //        word + ", " + avp + ", " + synset + ", " + SUMO);
                return result;
            }
        }
        return null;
    }
        
    /** ***************************************************************
     * Return a list of scored guesses at the synset for the given word in the
     * context of the sentence.  Returns a TreeSet consisting AVPairs of
     * the key score reflecting the quality of the guess the given synset is the right one
     * and a value of a 9-digit WordNet synset
     */
    public static TreeSet<AVPair> findWordSensePOS(String word, List<String> words, int POS) {

        TreeSet<AVPair> result = new TreeSet<AVPair>();
        //System.out.println("INFO in WSD.findWordSensePOS(): word, POS, text: " +
        //        word + ", " + POS + ", " + words);
        ArrayList<String> senseKeys = WordNet.wn.wordsToSenseKeys.get(word);
        List<String> termFormatBypass = termFormatBypass(word);
        if (termFormatBypass != null) {
            AVPair score = new AVPair();
            score.attribute = "000" + termFormatBypass.get(1);
            score.value = termFormatBypass.get(0);
            result.add(score);
            return result;
        }
        if (senseKeys == null) {
            senseKeys = WordNet.wn.wordsToSenseKeys.get(word.toLowerCase());
            termFormatBypass = termFormatBypass(word.toLowerCase());
            if (termFormatBypass != null) {
                AVPair score = new AVPair();
                score.attribute = "000" + termFormatBypass.get(1);
                score.value = termFormatBypass.get(0);
                result.add(score);
                return result;
            }
            if (senseKeys == null) {
                if (debug)
                    System.out.println("Info in WSD.findWordSensePOS(): Word: '" + word +
                        "' not in lexicon as part of speech " + POS);
                return result;
            }
        }

        int bestSense = -1;
        int bestTotal = -1;
        for (int i = 0; i < senseKeys.size(); i++) {
            String senseKey = (String) senseKeys.get(i);
            String synset = WordNetUtilities.getSenseFromKey(senseKey);
            if (WordNetUtilities.sensePOS(senseKey) == POS) {
                HashMap<String,Integer> senseAssoc = WordNet.wn.wordCoFrequencies.get(senseKey.intern());
                if (senseAssoc != null) {
                    int total = 0;
                    for (int j = 0; j < words.size(); j++) {
                        String lowercase = ((String) words.get(j)).toLowerCase().intern();
                        if (senseAssoc.containsKey(lowercase)) 
                            total = total + ((Integer) senseAssoc.get(lowercase)).intValue();                        
                    }
                    AVPair score = new AVPair();
                    score.attribute = StringUtil.integerToPaddedString(total);
                    score.value = synset;
                    result.add(score);
                }
                else {
                    AVPair score = new AVPair();
                    score.attribute = StringUtil.integerToPaddedString(0);
                    score.value = synset;
                    result.add(score);
                }
            }
        }
        return result;
        /*
        if (bestTotal > 5) {
            String senseValue = (String) senses.get(bestSense);
            String synset = WordNet.wn.senseIndex.get(senseValue.intern());
            ArrayList<String> result = new ArrayList<String>();
            result.add((new Integer(POS)).toString() + synset);
            result.add(Integer.toString(bestTotal));
            //System.out.println("INFO in WordNet.findWordSensePOS(): result: " + result);
            return result;
        }
        else {
            return Lists.newArrayList();
        }
        */
    }
    
    /** ***************************************************************
     * Get the SUMO term that represents the best guess at
     * meaning for a word.  This method attempts to convert to root form.
     */
    public static String getBestDefaultSUMOsense(String word, int pos) {

        //System.out.println("WSD.getBestDefaultSUMOSense(): " + word);
        String newWord = word;
        if (pos == 1)
            newWord = WordNet.wn.nounRootForm(word,word.toLowerCase());
        if (pos == 2)
            newWord = WordNet.wn.verbRootForm(word,word.toLowerCase());
        if (StringUtil.emptyString(newWord))
            newWord = word;
        String sense = getBestDefaultSense(newWord,pos);
        if (StringUtil.emptyString(sense))
            return "";
        if (pos == 1) {
            return WordNet.wn.nounSUMOHash.get(sense.substring(1));
        }
        else if (pos == 2) {
            return WordNet.wn.verbSUMOHash.get(sense.substring(1));           
        }
        else if (pos == 3) {
            return WordNet.wn.adjectiveSUMOHash.get(sense.substring(1));   
        }
        else if (pos == 4) {
            return WordNet.wn.adverbSUMOHash.get(sense.substring(1));  
        }
        return "";
    }

    /** ***************************************************************
     * Get the SUMO term that represents the best guess at
     * meaning for a word.
     */
    public static String getBestDefaultSUMO(String word) {

        //System.out.println("WSD.getBestDefaultSUMO(): " + word);
        String sense = getBestDefaultSense(word);
        if (StringUtil.emptyString(sense))
            return "";
        if (sense.charAt(0) == '1') {
            return WordNet.wn.nounSUMOHash.get(sense.substring(1));
        }
        else if (sense.charAt(0) == '2') {
            return WordNet.wn.verbSUMOHash.get(sense.substring(1));           
        }
        else if (sense.charAt(0) == '3') {
            return WordNet.wn.adjectiveSUMOHash.get(sense.substring(1));   
        }
        else if (sense.charAt(0) == '4') {
            return WordNet.wn.adverbSUMOHash.get(sense.substring(1));  
        }
        return "";
    }

    /** ***************************************************************
     * Get the POS-prefixed synset that represents the best guess at
     * meaning for a word.  If there is no wordFrequency entry for the
     * given word then it returns any sense. @return a 9 digit synset number
     */
    public static String getBestDefaultSense(String word) {

        //System.out.println("INFO in WSD.getBestDefaultSense(1): " + word);
        if (StringUtil.isDigitString(word))
            return null;
        String bestSense = "";
        int bestScore = -1;
        for (int pos = 1; pos <= 4; pos++) {
            //System.out.println("INFO in WSD.getBestDefaultSense(): pos: " + pos);
            String newWord = "";
            if (pos == 1)
                newWord = WordNet.wn.nounRootForm(word,word.toLowerCase());
            if (pos == 2)
                newWord = WordNet.wn.verbRootForm(word,word.toLowerCase());
            if (newWord == "")
                newWord = word;
            //System.out.println("INFO in WSD.getBestDefaultSense(): word: " + newWord + " POS: " + pos);
            if (newWord != null) {
                TreeSet<AVPair> senseKeys = WordNet.wn.wordFrequencies.get(newWord.toLowerCase());
                if (senseKeys != null) {
                    Iterator<AVPair> it = senseKeys.descendingIterator();
                    while (it.hasNext()) {
                        AVPair avp = it.next();
                        //System.out.println("INFO in WSD.getBestDefaultSense(): avp: " + avp);
                        //String POS = WordNetUtilities.getPOSfromKey(avp.value);
                        //String numPOS = WordNetUtilities.posLettersToNumber(POS);
                        String numPOS = avp.value.substring(0,1);
                        int count = Integer.parseInt(avp.attribute.trim());
                        if (Integer.toString(pos).equals(numPOS) && count > bestScore) {     
                        	//String baseSyn = WordNet.wn.senseIndex.get(avp.value);
                            //System.out.println("INFO in WSD.getBestDefaultSense(): baseSyn: " + baseSyn);
                        	//if (!StringUtil.emptyString(baseSyn)) {
                        	//	bestSense = numPOS + WordNet.wn.senseIndex.get(avp.value);
                            bestSense = avp.value;
                            bestScore = count;
                        	//}
                        }
                    }        
                }
            }
            //System.out.println("INFO in WSD.getBestDefaultSense(): best sense: " + bestSense + " best score: " + bestScore);
        }
        if (bestSense == "") {
            //System.out.println("INFO in WSD.getBestDefaultSense(): no frequencies for " + word);
            ArrayList<String> al = WordNet.wn.wordsToSenseKeys.get(word);
            if (al == null)
                al = WordNet.wn.wordsToSenseKeys.get(word.toLowerCase());

            //System.out.println("INFO in WSD.getBestDefaultSense(): senses: " + al);
            if (al != null) {
                Iterator<String> it = al.iterator();
                while (it.hasNext()) {
                    String key = it.next();
                    String POS = WordNetUtilities.getPOSfromKey(key);
                    String numPOS = WordNetUtilities.posLettersToNumber(POS);
                    String result = numPOS + WordNet.wn.senseIndex.get(key);
                    //System.out.println("INFO in WSD.getBestDefaultSense(): returning: " + result);
                    return result;
                }  
            }
        }
        else {
            //System.out.println("INFO in WSD.getBestDefaultSense(): returning: " + bestSense);
            return bestSense;
        }
        return "";
    }
    
    /** ***************************************************************
     * Get the POS-prefixed synset that represents the best guess at
     * meaning for a word with a given part of speech.  It picks the
     * most frequent sense for the word in the Brown Corpus.
     * @return a 9 digit synset number
     */
    public static String getBestDefaultSense(String word, int pos) {

        //System.out.println("WSD.getBestDefaultSense(2): " + word + " POS: " + pos);
        if (StringUtil.isDigitString(word))
            return null;
        String synset = "";
        String newWord = "";
        if (pos == 1)
            newWord = WordNet.wn.nounRootForm(word,word.toLowerCase());
        if (pos == 2)
            newWord = WordNet.wn.verbRootForm(word,word.toLowerCase());
        if (StringUtil.emptyString(newWord))
            newWord = word;
        //System.out.println("WSD.getBestDefaultSense(): word: " + newWord);
        if (newWord != null) {
            TreeSet<AVPair> senseKeys = WordNet.wn.wordFrequencies.get(newWord.toLowerCase());
            //System.out.println("WSD.getBestDefaultSense(): sensekeys: " + senseKeys);
            if (senseKeys != null) {
                Iterator<AVPair> it = senseKeys.descendingIterator();
                while (it.hasNext()) {
                    AVPair avp = it.next();
                    //String POS = WordNetUtilities.getPOSfromKey(avp.value);
                    String numPOS = avp.value.substring(0,1);
                    String POS = Character.toString(avp.value.charAt(0));
                    //if (Integer.toString(pos).equals(numPOS))
                    //    return numPOS + WordNet.wn.senseIndex.get(avp.value);
                    if (Integer.toString(pos).equals(numPOS))
                        return avp.value;
                }        
            }
        }
        // if none of the sensekeys are the right pos, fall through to here
        ArrayList<String> al = WordNet.wn.wordsToSenseKeys.get(newWord.toLowerCase());
        //System.out.println("WSD.getBestDefaultSense(): al: " + al);
        //System.out.println("WSD.getBestDefaultSense(): nouns: " + WordNet.wn.nounSynsetHash.get(newWord));
        if (al == null || al.size() == 0) {
            al = new ArrayList<String>();
            HashSet<String> synsets = null;
            switch (pos) {
                case 1: synsets = WordNet.wn.nounSynsetHash.get(newWord);                        
                        break;
                case 2: synsets = WordNet.wn.verbSynsetHash.get(newWord);  
                        break;
                case 3: synsets = WordNet.wn.adjectiveSynsetHash.get(newWord); 
                        break;
                case 4: synsets = WordNet.wn.adverbSynsetHash.get(newWord); 
                        break;
            }
            if (!StringUtil.emptyString(synsets)) 
                al.addAll(synsets);
            //System.out.println("WSD.getBestDefaultSense(): al: " + al);
            if (al == null || al.size() == 0)
                return "";
            else
                return Integer.toString(pos) + al.get(0);
        }
        Iterator<String> it = al.iterator();
        while (it.hasNext()) {
            String key = it.next();
            String POS = WordNetUtilities.getPOSfromKey(key);
            String numPOS = WordNetUtilities.posLettersToNumber(POS);
            if (Integer.toString(pos).equals(numPOS))
                return numPOS + WordNet.wn.senseIndex.get(key);
        }  
        return synset;
    }

    /** ***************************************************************
     *  Read the SICK data set
     *  http://clic.cimec.unitn.it/composes/sick.html
     */
    public static ArrayList<ArrayList<String>> readSick() {

        System.out.println("In WSD.readSick(): Reading SICK file ");
        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
        LineNumberReader lr = null;
        try {
            // pair_ID sentence_A sentence_B entailment_label relatedness_score entailment_AB entailment_BA sentence_A_original
            // sentence_B_original sentence_A_dataset sentence_B_dataset SemEval_set
            String line;
            String f = System.getProperty("user.home") + "/ontology/SICK/SICK.txt";
            File sickFile = new File(f);
            if (sickFile == null) {
                System.out.println("Error in WSD.readSick(): The file does not exist in " + f );
                return null;
            }
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(sickFile);
            lr = new LineNumberReader(r);
            while ((line = lr.readLine()) != null) {
                System.out.println(line);
                //if (lr.getLineNumber() % 1000 == 0)
                //    System.out.print('.');
                String[] ls = line.split("\t");
                ArrayList<String> al = new ArrayList<String>(Arrays.asList(ls));
                result.add(al);
            }
        }
        catch (IOException ex) {
            System.out.println("Error in WSD.readSick()");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
     *  Extract SUMO terms from the SICK data set
     *  http://clic.cimec.unitn.it/composes/sick.html
     */
    public static void collectSUMOFromSICK() {

        FileWriter fw = null;
        PrintWriter pw = null;
        String fname = System.getProperty("user.home") + "/ontology/SICK/SickOut.txt";

        try {
            fw = new FileWriter(fname);
            pw = new PrintWriter(fw);

            KBmanager.getMgr().initializeOnce();

            WordNet.initOnce();
            ArrayList<ArrayList<String>> sickAr = readSick();
            System.out.println("collectSUMOFromSICK: size of array" + sickAr.size());
            for (int i = 0; i < sickAr.size(); i++) {
                ArrayList<String> al = sickAr.get(i);
                System.out.println("WSD.collectSUMOFromSICK():  line " + i);
                if (al.size() > 2) {
                    System.out.println("WSD.collectSUMOFromSICK(): at line " + al.get(0));
                    String sentA = al.get(1);
                    String sumoA = collectSUMOFromWords(sentA).toString();
                    String sentB = al.get(2);
                    String sumoB = collectSUMOFromWords(sentB).toString();
                    pw.println(sentA);
                    pw.println(sumoA);
                    pw.println(sentB);
                    pw.println(sumoB);
                }
            }
        }
        catch (Exception ex) {
            System.out.println("Error in WSD.collectSUMOFromSICK()");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            try {
                if (pw != null) {
                    pw.close();
                }
                if (fw != null) {
                    fw.close();
                }
            }
            catch (Exception ex) {
                System.out.println("Error in WSD.collectSUMOFromSICK()");
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    /** ***************************************************************
     *  @return each line of a file into an array.  The first element of
     *  each interior array is the whole line, and subsequent elements
     *  are the individual words.
     */
    public static ArrayList<ArrayList<String>> readFileIntoArray(String filename) {

        System.out.println("In WSD.readFileIntoArray(): Reading file " + filename);
        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
        LineNumberReader lr = null;
        try {
            String line;
            File file = new File(filename);
            if (file == null) {
                System.out.println("Error in WSD.collectSUMOFromFile(): The file does not exist in " + filename);
                return null;
            }
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(file);
            lr = new LineNumberReader(r);
            while ((line = lr.readLine()) != null) {
                System.out.println(line);
                String[] ls = line.split(" ");
                ArrayList<String> al = new ArrayList<String>();
                al.add(line);
                al.addAll(Arrays.asList(ls));
                result.add(al);
            }
        }
        catch (IOException ex) {
            System.out.println("Error in WSD.readSick()");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
     *  Extract SUMO terms from a file assuming one sentence per line
     *  @return a Map of SUMO term keys and integer counts of their
     *  appearance
     */
    public static Map<String,Integer> collectSUMOFromFile(String filename) {

        HashMap<String,Integer> result = new HashMap<>();
        ArrayList<ArrayList<String>> far = readFileIntoArray(filename);
        for (ArrayList<String> line : far) {
            String fullsent = line.get(0);
            for (int i = 1; i < line.size(); i++) {
                String synset = findWordSenseInContext(line.get(i), line);
                if (synset == "")
                    synset = WSD.getBestDefaultSense(line.get(i));
                if (synset != null && synset != "") {
                    if (!result.containsKey(synset)) {
                        result.put(synset, 1);
                        System.out.println("new synset: " + synset);
                    }
                    else {
                        Integer val = result.get(synset);
                        //System.out.println("adding to synset: " + synset + " : " + (val + 1));
                        result.put(synset, val.intValue() + 1);
                    }
                }
            }
        }
        Iterator<String> it = result.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String SUMO = WordNetUtilities.getBareSUMOTerm(WordNet.wn.getSUMOMapping(key));
            ArrayList<String> words = WordNet.wn.synsetsToWords.get(key);
            String wordstr = "";
            if (words != null)
                wordstr = words.toString();
            System.out.println(key + "\t" + result.get(key) + "\t" + SUMO + "\t" + wordstr);
        }
        return result;
    }

    /** ***************************************************************
     *  Extract SUMO terms from a file assuming one sentence per line
     *  @return a Map of SUMO term keys and integer counts of their
     *  appearance
     */
    public static Map<String,Integer> collectSUMOFromString(String lineStr) {

        ArrayList<String> line = new ArrayList<String>();
        line.addAll(Arrays.asList(lineStr.split(" ")));
        HashMap<String,Integer> result = new HashMap<>();
        for (int i = 1; i < line.size(); i++) {
            String synset = findWordSenseInContext(line.get(i), line);
            if (synset == "")
                synset = WSD.getBestDefaultSense(line.get(i));
            if (synset != null && synset != "") {
                if (!result.containsKey(synset)) {
                    result.put(synset, 1);
                    System.out.println("new synset: " + synset);
                }
                else {
                    Integer val = result.get(synset);
                    //System.out.println("adding to synset: " + synset + " : " + (val + 1));
                    result.put(synset, val.intValue() + 1);
                }
            }
        }
        Iterator<String> it = result.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String SUMO = WordNetUtilities.getBareSUMOTerm(WordNet.wn.getSUMOMapping(key));
            ArrayList<String> words = WordNet.wn.synsetsToWords.get(key);
            String wordstr = "";
            if (words != null)
                wordstr = words.toString();
            System.out.println(key + "\t" + result.get(key) + "\t" + SUMO + "\t" + wordstr);
        }
        return result;
    }

    /** ***************************************************************
     *  A method used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void testWordWSD() {
    
        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        WordNet.initOnce();
        System.out.println("INFO in WSD.testWordWSD(): " + WSD.getBestDefaultSense("India"));
        System.out.println("INFO in WSD.testWordWSD(): " + WSD.getBestDefaultSense("kick"));
    }
    
    /** ***************************************************************
     *  A method used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void testSentenceWSD() {
        
        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        WordNet.initOnce();
        System.out.println("INFO in WSD.testSentenceWSD(): done initializing");

        String sentence = "John walks.";
        System.out.println("INFO in WSD.testSentenceWSD(): " + sentence);
        System.out.println("INFO in WSD.testSentenceWSD(): " + WSD.collectWordSenses(sentence));
        sentence = "Bob runs around the track.";
        System.out.println("INFO in WSD.testSentenceWSD(): " + sentence);
        System.out.println("INFO in WSD.testSentenceWSD(): " + WSD.collectWordSenses(sentence));
        sentence = "A computer is a general purpose device that can be programmed to carry out a finite set of arithmetic or logical operations.";
        System.out.println("INFO in WSD.testSentenceWSD(): " + sentence);
        System.out.println("INFO in WSD.testSentenceWSD(): " + WSD.collectWordSenses(sentence));
        sentence = "A four stroke engine is a beautiful thing.";
        System.out.println("INFO in WSD.testSentenceWSD(): " + sentence);
        System.out.println("INFO in WSD.testSentenceWSD(): " + WSD.collectWordSenses(sentence));     
        System.out.println("INFO in WSD.testSentenceWSD(): " + WordNet.wn.sumoSentenceDisplay(sentence,sentence,""));
        ArrayList<String> sentar = Lists.newArrayList("John","kicks","the","cart");
        System.out.println("INFO in WSD.testSentenceWSD(): " + sentar);
        for (String s : sentar)
            System.out.println("INFO in WSD.testSentenceWSD(): word: " + s + " SUMO: " + WSD.getBestDefaultSUMO(s));
    }

    /** ***************************************************************
     *  A method used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void testSentenceWSD2() {

        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        WordNet.initOnce();
        System.out.println("INFO in WSD.testSentenceWSD(): done initializing");
        String sentence = "Play Hello on Hulu.";
        System.out.println("INFO in WSD.testSentenceWSD(): " + sentence);
        System.out.println("INFO in WSD.testSentenceWSD(): " + WSD.collectWordSenses(sentence));
        ArrayList<String> sentar = Lists.newArrayList("Play", "Hello", "on", "Hulu");
        System.out.println("INFO in WSD.testSentenceWSD(): " + sentar);
        for (String s : sentar)
            System.out.println("INFO in WSD.testSentenceWSD(): word: " + s + " SUMO: " + WSD.getBestDefaultSUMO(s));
    }

    /** ***************************************************************
     */
    public static void interactive() {

        BufferedReader d = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("type 'quit' (without the quotes) on its own line to quit");
        String line = "";
        try {
            while (!line.equals("quit")) {
                System.out.print("> ");
                line = d.readLine();
                if (!line.equals("quit"))
                    System.out.println(collectSUMOFromString(line));
            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            System.out.println("error in TimeBank.interactive()");
        }
    }

    /** ***************************************************************
     *  A main method, used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void main (String[] args) {

        //testWordWSD();
        //testSentenceWSD();
        //testSentenceWSD2();
        //collectSUMOFromSICK();
        if (args != null && args.length > 0 && (args[0].equals("-f"))) {
            KBmanager.getMgr().initializeOnce();
            collectSUMOFromFile(args[1]);
        }
        else if (args != null && args.length > 0 && (args[0].equals("-p"))) {
            KBmanager.getMgr().initializeOnce();
            collectSUMOFromString(StringUtil.removeEnclosingQuotes(args[1]));
        }
        else if (args != null && args.length > 0 && (args[0].equals("-i"))) {
            KBmanager.getMgr().initializeOnce();
            interactive();
        }
        else if (args != null || args.length == 0 || (args.length > 0 && args[0].equals("-h"))) {
            System.out.println("Word Sense Disambiguation");
            System.out.println("  options:");
            System.out.println("  -h - show this help screen");
            System.out.println("  -f <file> - find all SUMO terms in a file");
            System.out.println("  -p one quoted sentence with space delimited tokens into SUMO ");
            System.out.println("  -i interactive mode ");
        }
    }
}
