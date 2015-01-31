package com.articulate.sigma;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

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
        /*
        String result = "";
        ArrayList<String> al = WordNet.splitToArrayList(sentence);
        if (al == null || al.size() < 1)
            return "";
        for (int i = 0; i < al.size(); i++) {
            String word = (String) al.get(i);
            String SUMO = findSUMOFromWordInContext(word,al);
            if (!StringUtil.emptyString(SUMO)) {
                if (SUMO.indexOf("&%") >= 0) {
                    System.out.println("INFO in WordNet.collectSUMOFromWords(): found in context: " + SUMO);
                    SUMO = WordNetUtilities.getBareSUMOTerm(SUMO);
                    //System.out.println("INFO in WordNet.collectSUMOFromWords(): after: " + SUMO);                    
                }
                if (result == "")
                    result = SUMO;
                else
                    result = result + " " + SUMO;
            }
            else {                                    // assume it's a noun
                SUMO = getBestDefaultSUMO(word);
                if (!StringUtil.emptyString(SUMO)) {
                    if (SUMO.indexOf("&%") >= 0) {
                        System.out.println("INFO in WordNet.collectSUMOFromWords(): no context: " + SUMO);
                        SUMO = WordNetUtilities.getBareSUMOTerm(SUMO);
                        //System.out.println("INFO in WordNet.collectSUMOFromWords(): after: " + SUMO); 
                    }
                    if (result == "")
                        result = SUMO;
                    else
                        result = result + " " + SUMO;
                }
            }
            //if (SUMO == null || SUMO == "")
            //   System.out.println("INFO in findSUMOWordSense(): word not found: " + word);
           //    else
           //    System.out.println("INFO in findSUMOWordSense(): word, term: " + word + ", " + SUMO);
        }
        System.out.println("INFO in WordNet.collectSUMOFromWords(): result: " + result);
        */
        return result;
    }

    /** ***************************************************************
     * Collect all the synsets that represent the best guess at
     * meanings for all the words in a text given a larger linguistic
     * context.  
     * @return 9 digit synset IDs 
     */
    public static ArrayList<String> collectWordSenses(String text) {

        System.out.println("INFO in WordNet.collectWordSenses(): " + text);
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
            int wordIndex = WordNet.wn.collectMultiWord(al, i, multiWordResult);   
            if (wordIndex != i) {
                //String theMultiWord = WordNet.wn.synsetsToWords.get(multiWordResult.get(0)).get(0);
                result.add(multiWordResult.get(0));
                //wordResult = wordResult + " " + theMultiWord;
                i = wordIndex;
            }
            else {
                if (!WordNet.wn.isStopWord(word)) {
                    String synset = findWordSenseInContext(word,alcon);
                    if (!StringUtil.emptyString(synset)) {
                        result.add(synset);
                    }
                    else {
                        synset = getBestDefaultSense(word);
                        if (!StringUtil.emptyString(synset)) {
                            result.add(synset);
                        }
                    }
                }
            }
        }
        System.out.println("INFO in WordNet.collectWordSenses(): result: " + result);
        return result;
    }

    /** ***************************************************************
     * Return the best guess at the SUMO term for the given word in the
     * context of the sentence.  Returns a SUMO term.
     
    public static String findSUMOFromWordInContext(String word, ArrayList<String> words) {

        //System.out.println("INFO in findSUMOFromWordInContext(): word: " + word);
        int bestScore = -1;
        //int POS = 0;
        String bestTerm = "";
        for (int i = 1; i < 4; i++) {
            String newWord = "";
            if (i == 1)
                newWord = WordNet.wn.nounRootForm(word,word.toLowerCase());
            if (i == 2)
                newWord = WordNet.wn.verbRootForm(word,word.toLowerCase());
            //System.out.println("INFO in findSUMOFromWordInContext(): root form: " + newWord + " for POS " + i);
            if (!StringUtil.emptyString(newWord))
                word = newWord;
            ArrayList<String> al = findSUMOWordSenseArray(word, words, i);
            if (al != null && al.size() > 0) {
                //String synset = (String) al.get(0); // 9-digit
                String SUMOterm = (String) al.get(1);
                //System.out.println("INFO in findSUMOFromWordInContext(): SUMO candidate: " + SUMOterm);
                String bestTotal = (String) al.get(2);
                int total = (new Integer(bestTotal)).intValue();
                if (total > bestScore) {
                    bestScore = total;
                    //POS = i;
                    bestTerm = SUMOterm;
                }
            }
        }
        //System.out.println("INFO in findSUMOFromWordInContext(): result: " + bestTerm);
        return bestTerm;
    }
    */
    
    /** ***************************************************************
     * Return the best guess at the synset for the given word in the
     * context of the sentence.  @return the 9-digit synset.
     */
    public static String findWordSenseInContext(String word, ArrayList<String> words) {

        System.out.println("INFO in findWordSenseInContext(): word, words: " + 
                word + ", " + words);
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
            ArrayList<String> al = findWordSensePOS(word, words, i);
            if (al != null && al.size() > 0) {
                String synset = (String) al.get(0); // 9-digit
                String bestTotal = (String) al.get(1);
                int total = (new Integer(bestTotal)).intValue();
                if (total > bestScore) {
                    bestScore = total;
                    bestSynset = synset;
                }
            }
        }
        //System.out.println("INFO in findWordSenseInContext(): best synset: " + bestSynset);
        return bestSynset;
    }
        
    /** ***************************************************************
     * Return the best guess at the synset for the given word in the
     * context of the sentence.  Returns an ArrayList consisting of
     * a 9-digit WordNet synset, and the score
     * reflecting the quality of the guess the given synset is the right one.
     */
    private static ArrayList<String> findWordSensePOS(String word, ArrayList<String> words, int POS) {

        //System.out.println("INFO in WordNet.findWordSensePOS(): word, POS, text, " + 
        //        word + ", " + POS + ", " + words);
        ArrayList<String> senses = WordNet.wn.wordsToSenses.get(word);
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
        if (bestSense == -1) {             // if no word cooccurrances have been found
            if (firstSense == -1)         // if there were no words of the right part of speech
                return new ArrayList<String>();            
            bestSense = firstSense;
        }
        String senseValue = (String) senses.get(bestSense);
        String synset = WordNet.wn.senseIndex.get(senseValue.intern());
        ArrayList<String> result = new ArrayList<String>();
        result.add((new Integer(POS)).toString()+synset);
        result.add(Integer.toString(bestTotal));
        //System.out.println("INFO in WordNet.findWordSensePOS(): result: " + result);
        return result;
    }
    
    /** ***************************************************************
     * Return the best guess at the synset for the given word.  
     * Returns an ArrayList consisting of
     * a 9-digit WordNet synset, and the corresponding SUMO term.
     
    private static ArrayList<String> findSUMOWordSensePOS(String word, int POS) {

        //System.out.println("WordNet.findSUMOWordSensePOS(): word: " + word);
        String SUMOterm = null;
        ArrayList<String> senses = WordNet.wn.wordsToSenses.get(word.intern());
        if (senses == null) 
            return new ArrayList<String>();        
        int firstSense = -1;
        for (int i = 0; i < senses.size(); i++) {
            String sense = (String) senses.get(i);
            if (WordNetUtilities.sensePOS(sense) == POS) {
                if (firstSense == -1)
                    firstSense = i;
            }
        }
        if (firstSense == -1)         // if there were no words of the right part of speech
            return new ArrayList<String>();            
        
        String senseValue = (String) senses.get(firstSense);
        String synset = WordNet.wn.senseIndex.get(senseValue.intern());
        switch (POS) {
            case WordNet.NOUN : SUMOterm = WordNet.wn.nounSUMOHash.get(synset.intern()); break;
            case WordNet.VERB : SUMOterm = WordNet.wn.verbSUMOHash.get(synset.intern()); break;
            case WordNet.ADJECTIVE : SUMOterm = WordNet.wn.adjectiveSUMOHash.get(synset.intern()); break;
            case WordNet.ADVERB : SUMOterm = WordNet.wn.adverbSUMOHash.get(synset.intern()); break;
        }
        if (SUMOterm != null) {                                                // Remove SUMO-WordNet mapping characters
            SUMOterm = SUMOterm.replaceAll("&%","");
            SUMOterm = SUMOterm.replaceAll("[+=@]","");
        }
        ArrayList<String> result = new ArrayList<String>();
        result.add((new Integer(POS)).toString()+synset);
        result.add(SUMOterm);
        //System.out.println("WordNet.findSUMOWordSensePOS(): result: " + result);
        return result;
    }
*/

    /** ***************************************************************
     * Return the best guess at the synset for the given word.
     * @return a 9 digit synset number
     
    public static String findWordSense(String word) {

        //System.out.println("INFO in WordNet.findWordSense(): " + word);
        int bestScore = 0;
        //int POS = 0;
        String bestSynset = "";
        for (int i = 1; i <= 4; i++) {
            String newWord = "";
            if (i == 1)
                newWord = WordNet.wn.nounRootForm(word,word.toLowerCase());
            if (i == 2)
                newWord = WordNet.wn.verbRootForm(word,word.toLowerCase());
            if (newWord != null && newWord != "")
                word = newWord;
            //System.out.println("INFO in WordNet.findWordSense(): newWord: " + word);
            ArrayList<String> al = findSUMOWordSensePOS(word, i);
            if (al != null && al.size() > 0) {
                String synset = al.get(0); // 9-digit
                //String SUMOterm = al.get(1);
                String bestTotal = Integer.toString(4-i);  // give priority to nouns
                //System.out.println("INFO in WordNet.findWordSense(): synset,SUMO " + synset + "," + SUMOterm);
                int total = (new Integer(bestTotal)).intValue();
                if (total > bestScore) {
                    bestScore = total;
                    //POS = i;
                    bestSynset = synset;
                }
            }
        }
        return bestSynset;
    }
*/
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

        //System.out.println("WSD.getBestDefaultSense(1): " + word);
        if (StringUtil.isDigitString(word))
            return null;
        String bestSense = "";
        int bestScore = -1;
        for (int pos = 1; pos <= 4; pos++) {
            //System.out.println("WSD.getBestDefaultSense(): pos: " + pos);
            String newWord = "";
            if (pos == 1)
                newWord = WordNet.wn.nounRootForm(word,word.toLowerCase());
            if (pos == 2)
                newWord = WordNet.wn.verbRootForm(word,word.toLowerCase());
            if (newWord == "")
                newWord = word;
            TreeSet<AVPair> senseKeys = WordNet.wn.wordFrequencies.get(newWord);
            if (senseKeys != null) {
                Iterator<AVPair> it = senseKeys.descendingIterator();
                while (it.hasNext()) {
                    AVPair avp = it.next();
                    String POS = WordNetUtilities.getPOSfromKey(avp.value);
                    String numPOS = WordNetUtilities.posLettersToNumber(POS);
                    int count = Integer.parseInt(avp.attribute.trim());
                    if (Integer.toString(pos).equals(numPOS) && count > bestScore) {                        
                        bestSense = numPOS + WordNet.wn.senseIndex.get(avp.value);
                        bestScore = count;
                    }
                }        
            }
        }
        if (bestSense == "") {
            //System.out.println("WSD.getBestDefaultSense(): no frequencies for " + word);
            ArrayList<String> al = WordNet.wn.wordsToSenses.get(word);
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
        System.out.println("WSD.getBestDefaultSense(): word: " + newWord);
        TreeSet<AVPair> senseKeys = WordNet.wn.wordFrequencies.get(newWord);
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
        // if none of the sensekeys are the right pos, fall through to here
        ArrayList<String> al = WordNet.wn.wordsToSenses.get(newWord);
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
    public static void testWSD () {
        
        //System.out.println("INFO in WordNet.main(): " + WordNet.wn.multiWord.get("four"));
        //System.out.println((new ArrayList(WordNet.wn.senseIndex.keySet()).get(0)));
        //System.out.println(WordNet.wn.senseIndex.get("coffee_NN_1"));

        // 107827896 dill
        // 107811416 herb
        // 107816296 bay leaf
        // 100021265 food, nutrient
        
        //ArrayList<String> al = WordNet.wn.collectWordSenses("A computer is a general purpose device that can be programmed to carry out a finite set of arithmetic or logical operations.");
        //ArrayList<String> al = WordNet.wn.collectWordSenses("A four stroke engine is a beautiful thing.");
        ArrayList<String> al = collectWordSenses("Bob likes all living things.");
        System.out.println("in main(): " + al);
        String synString = al.get(1);
        String[] synsets = synString.split(" ");
        for (int i = 0; i < synsets.length; i++) {
            System.out.println("in main(): synset,words: " + 
                    synsets[i] + " " + WordNet.wn.synsetsToWords.get(synsets[i]));
        }        
    }

    /** ***************************************************************
     *  A method used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void oldTestWSD (String[] args) {

        StringBuffer textBuffer = new StringBuffer();
        try {
            FileInputStream fileStream = new FileInputStream(args[0]);
            DataInputStream dataStream = new DataInputStream(fileStream);
            BufferedReader reader = new BufferedReader(new InputStreamReader(dataStream));
            String line;
    
            while ((line = reader.readLine()) != null) {
                textBuffer.append(line + " ");
            }
            dataStream.close();
        }
        catch (IOException ioe) {
            System.out.println("Error in WSD.oldTestWSD(): " + ioe.getMessage());
            ioe.printStackTrace();
        }
        String text = textBuffer.toString();
        ArrayList<String> al = WSD.collectWordSenses(text);
           
        System.out.println("in main(): " + al);
        String synString = al.get(1);
        String[] synsets = synString.split(" ");
        for (int i = 0; i < synsets.length; i++) {
            System.out.println("in WSD.oldTestWSD(): synset,words,SUMO: " + 
                    synsets[i] + " " + WordNet.wn.synsetsToWords.get(synsets[i]) + " " + 
                    WordNetUtilities.getBareSUMOTerm(WordNet.wn.getSUMOMapping(synsets[i])));
        }     
        /*String sentence = "Bob likes all living things.";
        String params = "flang=KIF&lang=EnglishLanguage&kb=SUMO";
        WordNet.wn.sumoSentenceDisplay(sentence, sentence, params);
        */
    }
    
    /** ***************************************************************
     *  A main method, used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void main (String[] args) {

        try {
            KBmanager.getMgr().initializeOnce();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        WordNet.initOnce();
        System.out.println("INFO in WSD.main(): done initializing");

        //String sentence = "Bob runs around the track.";
        //ArrayList<String> al = WordNet.wn.collectWordSenses("A computer is a general purpose device that can be programmed to carry out a finite set of arithmetic or logical operations.");
        //ArrayList<String> al = WordNet.wn.collectWordSenses("A four stroke engine is a beautiful thing.");

        //ArrayList<String> al = WordNet.splitToArrayList(sentence);
        //String synset = findWordSenseInContext("runs",al);
        //System.out.println("INFO in WSD.main(): " + synset);
        //System.out.println("INFO in WSD.main(): sense for 'pin': " + WSD.getBestDefaultSUMOsense("pin",1));
        String sentence = "John walks.";
        //System.out.println("INFO in WSD.main(): " + WSD.collectWordSenses(sentence));
        System.out.println("INFO in WSD.main(): " + WordNet.wn.sumoSentenceDisplay(sentence,sentence,""));
    }
}
