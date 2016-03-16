package com.articulate.sigma;

import java.util.*;

import com.google.common.collect.Lists;

public class WSD {

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
     * if there's a reasonable amount of data.
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
            List<String> al = findWordSensePOS(word, words, i);
            if (al != null && al.size() > 0) {
                String synset = (String) al.get(0); // 9-digit
                String bestTotal = (String) al.get(1);
                int total = (new Integer(bestTotal)).intValue();
                if (total >= bestScore) {
                    bestScore = total;
                    bestSynset = synset;
                }
            }
        }
        //System.out.println("INFO in findWordSenseInContext(): best synset: " + bestSynset);
        //System.out.println("INFO in findWordSenseInContext(): best score: " + bestScore);
        if (bestScore > 5)
            return bestSynset;
        else
            return "";
    }

    /** ***************************************************************
     * Return the best guess at the synset for the given word in the
     * context of the sentence with the given POS.
     * @param word - word to disambiguate
     * @param words - words in context
     * @param pos - part of speech of @word
     * @return the 9-digit synset but only if there's a reasonable amount of data.
     */
    public static String findWordSendInContextWithPos(String word, List<String> words, int pos) {

        int bestScore = -1;
        String bestSynset = "";
        String newWord = "";
        if (pos == 1)
            newWord = WordNet.wn.nounRootForm(word,word.toLowerCase());
        if (pos == 2)
            newWord = WordNet.wn.verbRootForm(word,word.toLowerCase());
        if (newWord != null && newWord != "")
            word = newWord;
        List<String> al = findWordSensePOS(word, words, pos);
        if (al != null && al.size() > 0) {
            String synset = (String) al.get(0); // 9-digit
            String bestTotal = (String) al.get(1);
            int total = (new Integer(bestTotal)).intValue();
            if (total >= bestScore) {
                bestScore = total;
                bestSynset = synset;
            }
        }
        if (bestScore > 5)
            return bestSynset;
        else
            return "";
    }

    /** ***************************************************************
     * Check if a sense has been created from a domain ontology and give
     * it priority.
     */
    private static List<String> termFormatBypass(String word) {

        ArrayList<String> result = new ArrayList<String>();
        TreeSet<AVPair> senselist = WordNet.wn.wordFrequencies.get(word);
        if (senselist == null)
            return null;
        //System.out.println("INFO in WordNet.termFormatBypass(): senselist: " + senselist);
        for (AVPair avp : senselist) {
            if (avp.value.equals("99999")) {
                result.add(avp.attribute);
                result.add("99999");
                //System.out.println("INFO in WordNet.termFormatBypass(): word, avp: " +
                //        word + ", " + avp);
                return result;
            }
        }
        return null;
    }
        
    /** ***************************************************************
     * Return the best guess at the synset for the given word in the
     * context of the sentence.  Returns an ArrayList consisting of
     * a 9-digit WordNet synset, and the score
     * reflecting the quality of the guess the given synset is the right one.
     */
    public static List<String> findWordSensePOS(String word, List<String> words, int POS) {

        //System.out.println("INFO in WordNet.findWordSensePOS(): word, POS, text: " +
        //        word + ", " + POS + ", " + words);
        ArrayList<String> senses = WordNet.wn.wordsToSenses.get(word);
        List<String> termFormatBypass = termFormatBypass(word);
        if (termFormatBypass != null)
            return termFormatBypass;
        if (senses == null) {
            System.out.println("Info in WSD.findWordSensePOS(): Word: '" + word + 
                    "' not in lexicon as part of speech " + POS);
            return new ArrayList<String>();
        }
        int firstSense = -1;
        int bestSense = -1;
        int bestTotal = -1;
        for (int i = 0; i < senses.size(); i++) {
            String sense = (String) senses.get(i);
            if (WordNetUtilities.sensePOS(sense) == POS) {
                if (firstSense == -1)
                    firstSense = i;
                HashMap<String,Integer> senseAssoc = WordNet.wn.wordCoFrequencies.get(sense.intern());
                if (senseAssoc != null) {
                    int total = 0;
                    for (int j = 0; j < words.size(); j++) {
                        String lowercase = ((String) words.get(j)).toLowerCase().intern();
                        if (senseAssoc.containsKey(lowercase)) 
                            total = total + ((Integer) senseAssoc.get(lowercase)).intValue();                        
                    }
                    if (total > bestTotal) {
                        bestTotal = total;
                        bestSense = i;
                    }
                }
                else {
                    if (0 > bestTotal) { // no word frequency found, give lowest possible score
                        bestTotal = 0;
                        bestSense = i;
                    }
                }
            }
        }
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
                        String POS = WordNetUtilities.getPOSfromKey(avp.value);
                        String numPOS = WordNetUtilities.posLettersToNumber(POS);
                        int count = Integer.parseInt(avp.attribute.trim());
                        if (Integer.toString(pos).equals(numPOS) && count > bestScore) {     
                        	String baseSyn = WordNet.wn.senseIndex.get(avp.value);
                        	if (!StringUtil.emptyString(baseSyn)) {
                        		bestSense = numPOS + WordNet.wn.senseIndex.get(avp.value);
                        		bestScore = count;
                        	}
                        }
                    }        
                }
            }
            //System.out.println("INFO in WSD.getBestDefaultSense(): best sense: " + bestSense + " best score: " + bestScore);
        }
        if (bestSense == "") {
            //System.out.println("INFO in WSD.getBestDefaultSense(): no frequencies for " + word);
            ArrayList<String> al = WordNet.wn.wordsToSenses.get(word);
            if (al == null)
                al = WordNet.wn.wordsToSenses.get(word.toLowerCase());

            //System.out.println("INFO in WSD.getBestDefaultSense(): senses: " + al);
            if (al != null) {
                Iterator<String> it = al.iterator();
                while (it.hasNext()) {
                    String key = it.next();
                    String POS = WordNetUtilities.getPOSfromKey(key);
                    String numPOS = WordNetUtilities.posLettersToNumber(POS);
                    return numPOS + WordNet.wn.senseIndex.get(key);
                }  
            }
        }
        else
            return bestSense;
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
                    String POS = WordNetUtilities.getPOSfromKey(avp.value);
                    String numPOS = WordNetUtilities.posLettersToNumber(POS);
                    if (Integer.toString(pos).equals(numPOS))
                        return numPOS + WordNet.wn.senseIndex.get(avp.value);
                }        
            }
        }
        // if none of the sensekeys are the right pos, fall through to here
        ArrayList<String> al = WordNet.wn.wordsToSenses.get(newWord.toLowerCase());
        //System.out.println("WSD.getBestDefaultSense(): al: " + al);
        //System.out.println("WSD.getBestDefaultSense(): nouns: " + WordNet.wn.nounSynsetHash.get(newWord));
        if (al == null || al.size() == 0) {
            al = new ArrayList<String>();
            String synsets = "";
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
                al.addAll(Arrays.asList(synsets.split(" "))); 
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
     *  A method used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void testWordWSD() {
    
        try {
            KBmanager.getMgr().initializeOnce();
        } catch (Exception ex) {
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
        } catch (Exception ex) {
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
     *  A main method, used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void main (String[] args) {

        //testWordWSD();
        //testSentenceWSD();
        testSentenceWSD2();
    }
}
