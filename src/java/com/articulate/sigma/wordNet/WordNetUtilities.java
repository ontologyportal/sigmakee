/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
 */

package com.articulate.sigma.wordNet;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.AVPair;
import com.articulate.sigma.utils.StringUtil;
import com.google.common.collect.*;

import static com.articulate.sigma.wordNet.WSD.readFileIntoArray;

/** ***************************************************************
 *  @author Adam Pease
 */

public class WordNetUtilities {

    /** POS-prefixed mappings from a new synset number to the old
     *  one. */
    public static HashMap<String,String> mappings = new HashMap<String,String>();
    public static int TPTPidCounter = 1;
    public static int errorCount = 0;
    public static int patternNum = 18; // sense key patten

    /** ***************************************************************
     *  Get a SUMO term minus its &% prefix and one character mapping
     * suffix.
     */
    public static String getBareSUMOTerm (String term) {

        int start = 0;
        if (!StringUtil.emptyString(term)) {
            int finish = term.length();
            if (term.indexOf("&%") == 0)
                start = 2;
            if (!Character.isLetter(term.charAt(term.length()-1)) && !Character.isDigit(term.charAt(term.length()-1)))
                finish--;
            return term.substring(start,finish);
        }
        else
            return term;
    }

    /** ***************************************************************
     * Check whether a synset format is valid
     */
    public static boolean isValidSynset8(String synset) {

        if (!StringUtil.isInteger(synset) || synset.length() != 8) {
            //System.out.println("Error in WordNetUtilities.isValidSynset8(): bad synset: " + synset);
            return false;
        }
        else
            return true;
    }

    /** ***************************************************************
     * get the number of the verb frame
     */
    public static int verbFrameNum(String frame) {

        return WordNet.wn.VerbFrames.indexOf(frame);
    }

    /** ***************************************************************
     * Check whether a synset format is valid
     */
    public static boolean isValidSynset9(String synset) {

        if (!StringUtil.isInteger(synset) || synset.length() != 9) {
            //System.out.println("Error in WordNetUtilities.isValidSynset9(): bad synset: " + synset);
            return false;
        }
        else
            return true;
    }

    /** ***************************************************************
     * Check whether a sense key format is valid
     */
    public static boolean isValidKey(String senseKey) {

        String m = ".*_(NN|VB|JJ|RB|AS)_[\\d]+";
        return senseKey.matches(m);
    }

    /** ***************************************************************
     */
    public static String posAlphaKeyToWord(String alphaKey) {

        if (alphaKey.equals("NN")) return "noun";
        else if (alphaKey.equals("VB")) return "verb";
        else if (alphaKey.equals("JJ")) return "adjective";
        else if (alphaKey.equals("RB")) return "adverb";
        return "adjective_satellite";
    }

    /** ***************************************************************
     */
    public static String posWordToAlphaKey(String word) {

        if (word.equals("noun")) return "NN";
        else if (word.equals("verb")) return "VB";
        else if (word.equals("adjective")) return "JJ";
        else if (word.equals("adverb")) return "RB";
        return "AS";
    }

    /** ***************************************************************
     * Extract the POS from a word_POS_num sense key.  Should be an
     * alpha key, such as "VB".
     */
    public static String getPOSfromKey (String senseKey) {

        int lastUS = senseKey.lastIndexOf("_");
        if (lastUS < 0) {
            System.out.println("Info in WordNetUtilities.getPOSfromKey(): missing POS: " + senseKey);
            new Exception().printStackTrace();
            return "NN"; // default to noun
        }
        return senseKey.substring(lastUS - 2, lastUS);
    }

    /** ***************************************************************
     * Extract the word from a word_POS_num sense key.
     */
    public static String getWordFromKey (String senseKey) {

        int lastUS = senseKey.lastIndexOf("_");
        if (lastUS < 0) {
            System.out.println("Info in WordNetUtilities.getWordFromKey(): missing word: " + senseKey);
            new Exception().printStackTrace();
            return "";
        }
        return senseKey.substring(0, lastUS - 3);
    }

    /** ***************************************************************
     * Extract the sense number from a word_POS_num sense key.
     */
    public static String getNumFromKey (String senseKey) {

        int lastUS = senseKey.lastIndexOf("_");
        if (lastUS < 0) {
            System.out.println("Info in WordNetUtilities.getNumFromKey(): missing num: " + senseKey);
            new Exception().printStackTrace();
            return "";
        }
        return senseKey.substring(lastUS + 1,senseKey.length());
    }

    /** ***************************************************************
     * Extract the synset corresponding to a word_POS_num sense key.
     */
    public static String getSenseFromKey (String senseKey) {

        String POS = getPOSfromKey(senseKey);
        String POSnum = posLettersToNumber(POS);
        return POSnum + WordNet.wn.senseIndex.get(senseKey);
    }

    /** ***************************************************************
     * Extract the info in a word%num:num:num sense key.
     *     colonp = Pattern.compile("([^%]+)%([^:]*):([^:]*):([^:]*):([^:]*)");
     */
    public static ArrayList<String> parseColonKey (String colonKey) {

        ArrayList<String> result = new ArrayList<>();
        Pattern p = Pattern.compile("([^%]+)%([^:]*):([^:]*):([^:]*)");
        Matcher m = p.matcher(colonKey);
        if (m.matches()) {
            String word = m.group(1);
            String pos = m.group(2);
            String sensenum = m.group(3);
            result.add(word);
            result.add(pos);
            result.add(sensenum);
        }
        else
            System.out.println("Error in WordNetUtilities.parseColonKey(): improper key: " + colonKey);
        return result;
    }

    /** ***************************************************************
     * Extract the word from a word%num:num:num sense key.
     */
    public static String getWordFromColonKey (String key) {

        ArrayList<String> result = parseColonKey(key);
        if (result.size() < 3) {
            System.out.println("Info in WordNetUtilities.getWordFromColonKey(): missing word: " + key);
            new Exception().printStackTrace();
            return "";
        }
        return result.get(0);
    }

    /** ***************************************************************
     * Extract the sense number from a word%num:num:num sense key.
     */
    public static String getPOSNumFromColonKey (String key) {

        ArrayList<String> result = parseColonKey(key);
        if (result.size() < 3) {
            System.out.println("Info in WordNetUtilities.getPOSNumFromColonKey(): missing num: " + key);
            new Exception().printStackTrace();
            return "";
        }
        return result.get(1);
    }

    /** ***************************************************************
     * Extract the synset corresponding to a word%num:num:num sense key.
     */
    public static String getSenseFromColonKey (String key) {

        ArrayList<String> result = parseColonKey(key);
        if (result.size() < 3) {
            System.out.println("Info in WordNetUtilities.getSenseFromColonKey(): missing or bad key: " + key);
            new Exception().printStackTrace();
            return "";
        }
        String POSnum = result.get(1);
        String POSlet = posLettersToNumber(POSnum);
        String senseKey = result.get(0) + "_" + POSlet + "_" + result.get(2);
        return POSnum + WordNet.wn.senseIndex.get(senseKey);
    }

    /** ***************************************************************
     * Get the word_POS_num sense key corresponding to a 9 digit synset.
     * Note that some adjective keys are listed as "adjuncts" with id
     * '3' instead of '5' so we try that too in case of failure.
     */
    public static String getKeyFromSense(String synset) {

        String key = WordNet.wn.reverseSenseIndex.get(synset);
        if (StringUtil.emptyString(key)) {
            if (synset.charAt(0) == '3')
                key = WordNet.wn.reverseSenseIndex.get("5" + synset.substring(1));
            if (!StringUtil.emptyString(key))
                return key;
            if (errorCount < 20) {
                System.out.println("Error in WordNetUtilities.getKeyFromSense(): no result for " + synset);
                errorCount++;
                if (errorCount >= 20)
                    System.out.println("surpressing further errors...");
            }
            return null;
        }
        else
            return key;
    }

    /** ***************************************************************
     * Extract the nine digit synset ID corresponding to a word-POS.num sense key.
     * see nlp.corpora.OntoNotes
     */
    public static String synsetFromOntoNotes (String onKey) {

        int index1 = onKey.indexOf('-');
        if (index1 == -1) {
            System.out.println("Error in WordNetUtilities.synsetFromOntoNotes(): bad key format: " + onKey);
            return null;
        }
        int index2 = onKey.indexOf('.');
        if (index2 == -1) {
            System.out.println("Error in WordNetUtilities.synsetFromOntoNotes(): bad key format: " + onKey);
            return null;
        }
        String word = onKey.substring(0,index1);
        char POS = onKey.charAt(index1+1);
        char POSnum = posLetterToNumber(POS);
        String senseNum = onKey.substring(index2+1,onKey.length());
        String senseKey = word + "_" + posNumberToLetters(Character.toString(POSnum)) + "_" + senseNum;
        String eightDigit = WordNet.wn.senseIndex.get(senseKey);
        if (eightDigit == null) {
            if (errorCount < 20) {
                System.out.println("Error in WordNetUtilities.synsetFromOntoNotes(): no synset for sense key: " + senseKey);
                errorCount++;
                if (errorCount >= 20)
                    System.out.println("surpressing further errors...");
            }
            return null;
        }
        else
            return POSnum + WordNet.wn.senseIndex.get(senseKey);
    }

    /** ***************************************************************
     */
    public static String removeTermPrefixes (String formula) {

        return formula.replaceAll("&%", "");
    }

    /** ***************************************************************
     * Convert a list of Terms in the format "&%term1 &%term2" to an ArrayList
     * of bare term Strings
     */
    public static ArrayList<String> convertTermList (String termList) {

        ArrayList<String> result = new ArrayList<String>();
        String[] list = termList.split(" ");
        for (int i = 0; i < list.length; i++) {
            String t = getBareSUMOTerm(list[i]);
            if (!StringUtil.emptyString(t))
                result.add(t);
        }
        return result;
    }

    /** ***************************************************************
     *  Get a SUMO term mapping suffix.
     */
    public static char getSUMOMappingSuffix (String term) {

        if (!StringUtil.emptyString(term))
            return term.charAt(term.length()-1);
        else
            return ' ';
    }

    /** ***************************************************************
     */
    public static String convertWordNetPointer(String ptr) {

        if (ptr.equals("!"))    ptr =   "antonym";
        if (ptr.equals("@"))    ptr =   "hypernym";
        if (ptr.equals("@i"))   ptr =   "instance hypernym";
        if (ptr.equals("~"))    ptr =   "hyponym";
        if (ptr.equals("~i"))   ptr =   "instance hyponym";
        if (ptr.equals("#m"))   ptr =   "member holonym";
        if (ptr.equals("#s"))   ptr =   "substance holonym";
        if (ptr.equals("#p"))   ptr =   "part holonym";
        if (ptr.equals("%m"))   ptr =   "member meronym";
        if (ptr.equals("%s"))   ptr =   "substance meronym";
        if (ptr.equals("%p"))   ptr =   "part meronym";
        if (ptr.equals("="))    ptr =   "attribute";
        if (ptr.equals("+"))    ptr =   "derivationally related";
        if (ptr.equals(";c"))   ptr =   "domain topic";
        if (ptr.equals("-c"))   ptr =   "member topic";
        if (ptr.equals(";r"))   ptr =   "domain region";
        if (ptr.equals("-r"))   ptr =   "member region";
        if (ptr.equals(";u"))   ptr =   "domain usage";
        if (ptr.equals("-u"))   ptr =   "member usage";
        if (ptr.equals("*"))    ptr =   "entailment";
        if (ptr.equals(">"))    ptr =   "cause";
        if (ptr.equals("^"))    ptr =   "also see";
        if (ptr.equals("$"))    ptr =   "verb group";
        if (ptr.equals("&"))    ptr =   "similar to";
        if (ptr.equals("<"))    ptr =   "participle";
        if (ptr.equals("\\"))   ptr =   "pertainym";
        return ptr;
    }

    /** ***************************************************************
     */
    public static char posLetterToNumber(char POS) {

        switch (POS) {
        case 'n': return '1';
        case 'v': return '2';
        case 'a': return '3';
        case 'r': return '4';
        case 's': return '5';
        }
        System.out.println("Error in WordNetUtilities.posLetterToNumber(): bad letter: " + POS);
        return '1';
    }

    /** ***************************************************************
     */
    public static char posNumberToLetter(char POS) {

        switch (POS) {
        case '1': return 'n';
        case '2': return 'v';
        case '3': return 'a';
        case '4': return 'r';
        case '5': return 's';
        }
        System.out.println("Error in WordNetUtilities.posNumberToLetter(): bad number: " + POS);
        return 'n';
    }

    /** ***************************************************************
     */
    public static char posPennToNumber(String penn) {

        if (penn.equals("CC")) return '0';	// Coordinating conjunction
        else if (penn.equals("CD")) return '0';	//     CD	Cardinal number
        else if (penn.equals("DT")) return '0';	//     3.	DT	Determiner
        else if (penn.equals("EX")) return '0';	// 4.	EX	Existential there
        else if (penn.equals("FW")) return '0';	// 5.	FW	Foreign word
        else if (penn.equals("IN")) return '0';	// 6.	IN	Preposition or subordinating conjunction
        else if (penn.equals("JJ")) return '3';	// 7.	JJ	Adjective
        else if (penn.equals("JJR")) return '3';	// 8.	JJR	Adjective, comparative
        else if (penn.equals("JJS")) return '3';	// 9.	JJS	Adjective, superlative
        else if (penn.equals("LS")) return '0';	// 10.	LS	List item marker
        else if (penn.equals("MD")) return '0';	// 11.	MD	Modal
        else if (penn.equals("NN")) return '1';	// 12.	NN	Noun, singular or mass
        else if (penn.equals("NNS")) return '1';	// 13.	NNS	Noun, plural
        else if (penn.equals("NNP")) return '1';	// 14.	NNP	Proper noun, singular
        else if (penn.equals("NNPS")) return '1';	// 15.	NNPS	Proper noun, plural
        else if (penn.equals("PDT")) return '0';	// 16.	PDT	Predeterminer
        else if (penn.equals("POS")) return '0';	// 17.	POS	Possessive ending
        else if (penn.equals("PRP")) return '0';	// 18.	PRP	Personal pronoun
        else if (penn.equals("PRP$")) return '0';	// 19.	PRP$	Possessive pronoun
        else if (penn.equals("RB")) return '4';	// 20.	RB	Adverb
        else if (penn.equals("RBR")) return '4';	// 21.	RBR	Adverb, comparative
        else if (penn.equals("RBS")) return '4';	// 22.	RBS	Adverb, superlative
        else if (penn.equals("RP")) return '0';	// 23.	RP	Particle
        else if (penn.equals("SYM")) return '0';	// 24.	SYM	Symbol
        else if (penn.equals("TO")) return '0';	// 25.	TO	to
        else if (penn.equals("UH")) return '0';	// 26.	UH	Interjection
        else if (penn.equals("VB")) return '2';	// 27.	VB	Verb, base form
        else if (penn.equals("VBD")) return '2';	// 28.	VBD	Verb, past tense
        else if (penn.equals("VBG")) return '2';	// 29.	VBG	Verb, gerund or present participle
        else if (penn.equals("VBN")) return '2';	// 30.	VBN	Verb, past participle
        else if (penn.equals("VBP")) return '2';	// 31.	VBP	Verb, non-3rd person singular present
        else if (penn.equals("VBZ")) return '2';	// 32.	VBZ	Verb, 3rd person singular present
        else if (penn.equals("WDT")) return '0';	// 33.	WDT	Wh-determiner
        else if (penn.equals("WP")) return '0';	// 34.	WP	Wh-pronoun
        else if (penn.equals("WP$")) return '0';	// 35.	WP$	Possessive wh-pronoun
        else if (penn.equals("WRB")) return '0';	// 36.	WRB	Wh-adverb
        else if (penn.equals("``")) return '0';
        else if (penn.equals("''")) return '0';
        else { System.out.println("Error in WordNetUtilities.posPennToNumber(): bad tag: " + penn); new Throwable().printStackTrace(System.out); }
        return '0';
    }

    /** ***************************************************************
     * Convert a part of speech number to the two letter format used by
     * the WordNet sense index code.  Defaults to noun "NN".
     */
    public static String posNumberToLetters(String pos) {

        if (pos.equalsIgnoreCase("1")) return "NN";
        if (pos.equalsIgnoreCase("2")) return "VB";
        if (pos.equalsIgnoreCase("3")) return "JJ";
        if (pos.equalsIgnoreCase("4")) return "RB";
        if (pos.equalsIgnoreCase("5")) return "AS";
        System.out.println("Error in WordNetUtilities.posNumberToLetters(): bad number: " + pos);
        return "NN";
    }

    /** ***************************************************************
     * Convert a part of speech number to the two letter format used by
     * the WordNet sense index code.  Defaults to noun "NN".
     */
    public static String posLettersToNumber(String pos) {

        assert !StringUtil.emptyString(pos) : "Error in WordNetUtilities.posLettersToNumber(): empty string";
        if (pos.equalsIgnoreCase("NN")) return "1";
        if (pos.equalsIgnoreCase("VB")) return "2";
        if (pos.equalsIgnoreCase("JJ")) return "3";
        if (pos.equalsIgnoreCase("RB")) return "4";
        if (pos.equalsIgnoreCase("AS")) return "5";
        assert false : "Error in WordNetUtilities.posLettersToNumber(): bad letters: " + pos;
        return "1";
    }

    /** ***************************************************************
     * Take a WordNet sense identifier, and return the integer part of
     * speech code.
     */
    public static int sensePOS(String sense) {

        if (sense.indexOf("_NN_") != -1)
            return WordNet.NOUN;
        if (sense.indexOf("_VB_") != -1)
            return WordNet.VERB;
        if (sense.indexOf("_JJ_") != -1)
            return WordNet.ADJECTIVE;
        if (sense.indexOf("_RB_") != -1)
            return WordNet.ADVERB;
        if (sense.indexOf("_AS_") != -1)
            return WordNet.ADJECTIVE_SATELLITE;
        if (sense.indexOf("NNP") != -1)
            return WordNet.NOUN;
        if (sense.indexOf("JJ") != -1)
            return WordNet.ADJECTIVE;
        if (sense.indexOf("VBG") != -1)
            return WordNet.VERB;
        if (sense.indexOf("VBZ") != -1)
            return WordNet.VERB;
        if (sense.indexOf("RB") != -1)
            return WordNet.ADVERB;
        if (sense.indexOf("AS") != -1)
            return WordNet.ADJECTIVE_SATELLITE;
        if (sense.indexOf("NN") != -1)
            return WordNet.NOUN;
        System.out.println("Error in WordNetUtilities.sensePOS(): Unknown part of speech type in sense code: " + sense);
        return 0;
    }

    /** ***************************************************************
     */
    public static String mappingCharToName(char mappingType) {

        String mapping = "";
        switch (mappingType) {
        case '=': mapping = "equivalent";
        break;
        case ':': mapping = "anti-equivalent";
        break;
        case '+': mapping = "subsuming";
        break;
        case '[': mapping = "negated subsuming";
        break;
        case '@': mapping = "instance";
        break;
        case ']': mapping = "negated instance";
        break;
        }
        return mapping;
    }

    /** ***************************************************************
     *  A utility function that mimics the functionality of the perl
     *  substitution feature (s/match/replacement/).  Note that only
     *  one replacement is made, not a global replacement.
     *  @param result is the string on which the substitution is performed.
     *  @param match is the substring to be found and replaced.
     *  @param subst is the string replacement for match.
     *  @return is a String containing the result of the substitution.
     */
    public static String subst(String result, String match, String subst) {

        Pattern p = Pattern.compile(match);
        Matcher m = p.matcher(result);
        if (m.find()) {
            result = m.replaceFirst(subst);
        }
        return result;
    }

    /** ***************************************************************
     *  A utility function that mimics the functionality of the perl
     *  substitution feature (s/match/replacement/) but rather than
     *  returning the result of the substitution, just tests whether the
     *  result is a key in a hashtable.  Note that only
     *  one replacement is made, not a global replacement.
     *  @param result is the string on which the substitution is performed.
     *  @param match is the substring to be found and replaced.
     *  @param subst is the string replacement for match.
     *  @param hash is a hashtable to be checked against the result.
     *  @return is a boolean indicating whether the result of the substitution
     *  was found in the hashtable.
     */
    public static boolean substTest(String result, String match, String subst,
                                    HashMap<String,HashSet<String>> hash) {

        Pattern p = Pattern.compile(match);
        Matcher m = p.matcher(result);
        if (m.find()) {
            result = m.replaceFirst(subst);
            //System.out.println("Info in WordNetUtilities.substTest(): replacement result: " + result);
            if (hash.containsKey(result)) {
                return true;
            }
            return false;
        }
        else
            return false;
    }

    /** ***************************************************************
     */
    private static boolean isVowel(char c) {

        if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u')
            return true;
        else
            return false;
    }

    /** ***************************************************************
     * Return the plural form of the verb.  Handle multi-word phrases
     * to modify only the first word.
     */
    public static String verbPlural(String verb) {

        String word = verb;
        String remainder = "";
        if (verb.indexOf("_") > 0) {
            word = verb.substring(0,verb.indexOf("_"));
            remainder = verb.substring(verb.indexOf("_"),verb.length());
        }

        // if (exceptionVerbPluralHash.containsKey(word))                  Note that there appears to be no WordNet exception list for verb plurals, just tenses
        //    word = (String) exceptionVerbPluralHash.get(word);

        if (word.matches(".*y$") && !isVowel(word.charAt(word.length()-2)))
            word = WordNetUtilities.subst(word,"y$","ies");
        else {
            if (word.matches(".*s$") || word.matches(".*x$") || word.matches(".*ch$") ||
                    word.matches(".*sh$") || word.matches(".*z$") || word.equals("go"))
                word = word + "es";
            else
                if (word.equals("be"))
                    word = "are";
                else
                    word = word + "s";
        }
        return word + remainder;
    }

    /** ***************************************************************
     * Return the plural form of the noun.  Handle multi-word phrases
     * to modify only the last word.
     */
    public static String nounPlural(String noun) {

        String word = noun;
        if (WordNet.wn.exceptionNounPluralHash.containsKey(word))
            word = (String) WordNet.wn.exceptionNounPluralHash.get(word);
        if (word.matches(".*y$") && !isVowel(word.charAt(word.length()-2)))
            word = WordNetUtilities.subst(word,"y$","ies");
        else {
            if (word.matches(".*s$") || word.matches(".*x$") || word.matches(".*ch$") ||
                    word.matches(".*sh$") || word.matches(".*z$") || word.equals("go"))
                word = word + "es";
            else
            if (word.equals("be"))
                word = "are";
            else
                word = word + "s";
        }
        return word;
    }

    /** ***************************************************************
     * HTML format a TreeMap of word senses and their associated synset
     */
    public static String formatWords(TreeMap<String,String> words, String kbName) {

        StringBuffer result = new StringBuffer();
        int count = 0;
        Iterator<String> it = words.keySet().iterator();
        while (it.hasNext() && count < 50) {
            String word = it.next();
            String synset = words.get(word);
            result.append("<a href=\"WordNet.jsp?word=");
            result.append(word);
            result.append("&POS=");
            result.append(synset.substring(0,1));
            result.append("&kb=");
            result.append(kbName);
            result.append("&synset=");
            result.append(synset.substring(1,synset.length()));
            result.append("\">" + word + "</a>");
            count++;
            if (it.hasNext() && count < 50)
                result.append(", ");
        }
        if (it.hasNext() && count >= 50)
            result.append("...");
        return result.toString();
    }

    /** ***************************************************************
     * HTML format a TreeMap of ArrayLists word senses
     */
    public static String formatWordsList(TreeMap<String,ArrayList<String>> words, String kbName) {

        StringBuffer result = new StringBuffer();
        int count = 0;
        Iterator<String> it = words.keySet().iterator();
        while (it.hasNext() && count < 50) {
            String word = (String) it.next();
            ArrayList<String> synsetList = words.get(word);
            for (int i = 0; i < synsetList.size(); i++) {
                String synset = synsetList.get(i);
                result.append("<a href=\"WordNet.jsp?word=");
                result.append(word);
                result.append("&POS=");
                result.append(synset.substring(0,1));
                result.append("&kb=");
                result.append(kbName);
                result.append("&synset=");
                result.append(synset.substring(1,synset.length()));
                result.append("\">" + word + "</a>");
                count++;
                if (i < synsetList.size() - 1)
                    result.append(", ");
            }
            if (it.hasNext() && count < 50)
                result.append(", ");
        }
        if (it.hasNext() && count >= 50)
            result.append("...");
        return result.toString();
    }

    /** ***************************************************************
     * Routine called by mergeUpdates which does the bulk of the work.
     * Should not be called during normal interactive running of Sigma.
     */
    private static void processMergers (HashMap<String,String> hm, String fileName,
                                        String pattern, String posNum) throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null;
        LineNumberReader lr = null;
        try {
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            fw = new FileWriter(KBmanager.getMgr().getPref("kbDir") + File.separator + fileName + "-new.txt");
            pw = new PrintWriter(fw);

            FileReader r = new FileReader(KBmanager.getMgr().getPref("kbDir") + File.separator + fileName + ".txt");
            lr = new LineNumberReader(r);
            String line;
            while ((line = lr.readLine()) != null) {
                if (lr.getLineNumber() % 1000 == 0)
                    System.out.print('.');
                Pattern p = Pattern.compile(pattern);
                line = line.trim();
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String oldTerm = m.group(4);
                    String bareOldTerm = getBareSUMOTerm(oldTerm);
                    String mapType = oldTerm.substring(oldTerm.length()-1);
                    String synset = posNum + m.group(1);
                    String newTerm = hm.get(synset);
                    if (bareOldTerm.indexOf("&%") < 0 && newTerm != null && newTerm != "" && !newTerm.equals(bareOldTerm) && kb.childOf(newTerm,bareOldTerm)) {
                        pw.println(m.group(1) + m.group(2) + "| " + m.group(3) + " &%" + newTerm + mapType);
                        System.out.println("INFO in WordNet.processMergers(): synset, oldTerm, newterm: " +
                                synset + " " + oldTerm + " " + newTerm);
                    }
                    else
                        pw.println(m.group(1) + m.group(2) + "| " + m.group(3) + " " + m.group(4));
                }
                else
                    pw.println(line.trim());
            }
        }
        catch (java.io.IOException e) {
            throw new IOException("Error writing file " + fileName + "\n" + e.getMessage());
        }
        finally {
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                fw.close();
            }
            if (lr != null) {
                lr.close();
            }
        }
    }

    /** ***************************************************************
     * Read in a file with a nine-digit synset number followed by a space
     * and a SUMO term.  If the term is more specific than the current
     * mapping for that synset, replace the old term. This is a utility
     * that is not normally called from the interactive Sigma system.
     */
    public static void mergeUpdates() throws IOException {

        HashMap<String,String> hm = new HashMap<String,String>();

        String dir = "/Program Files/Apache Software Foundation/Tomcat 5.5/KBs";
        FileReader r = new FileReader(dir + File.separator + "newMappings20.dat");
        LineNumberReader lr = new LineNumberReader(r);
        String line;
        while ((line = lr.readLine()) != null) {
            if (line.length() > 11) {
                String synset = line.substring(0,9);
                String SUMOterm = line.substring (10);
                hm.put(synset,SUMOterm);
            }
        }

        String fileName = "WordNetMappings-nouns";
        String pattern = "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\&\\%\\S+[\\S\\s]+)$";
        String posNum = "1";
        processMergers(hm,fileName,pattern,posNum);
        fileName = "WordNetMappings-verbs";
        pattern = "^([0-9]{8})([^\\|]+)\\|\\s([\\S\\s]+?)\\s(\\&\\%\\S+[\\S\\s]+)$";
        posNum = "2";
        processMergers(hm,fileName,pattern,posNum);
        fileName = "WordNetMappings-adj";
        pattern = "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\&\\%\\S+[\\S\\s]+)$";
        posNum = "3";
        processMergers(hm,fileName,pattern,posNum);
        fileName = "WordNetMappings-adv";
        pattern = "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)\\s(\\&\\%\\S+[\\S\\s]+)$";
        posNum = "4";
        processMergers(hm,fileName,pattern,posNum);
    }

    /** ***************************************************************
     * Given a POS-prefixed synset that is not mapped to SUMO, go up the hypernym
     * links to try to find a synset that is linked.  Return the SUMO term with its
     * mapping type suffix and &% prefix. Note that in cases where there are
     * multiple hpernyms, When the first hypernym doesn't yield a good SUMO term,
     * the routine does a depth first search (although going "up"
     * the tree of hypernyms) to find a good term.
     */
    private static String findMappingFromHypernym(String synset) {

        ArrayList<AVPair> rels = WordNet.wn.relations.get(synset);   // relations requires prefixes
        if (rels != null) {
            Iterator<AVPair> it2 = rels.iterator();
            while (it2.hasNext()) {
                AVPair avp = it2.next();
                if (avp.attribute.equals("hypernym") || avp.attribute.equals("instance hypernym")) {
                    String mappingChar = "";
                    if (avp.attribute.equals("instance hypernym"))
                        mappingChar = "@";
                    else
                        mappingChar = "+";
                    String targetSynset = avp.value;
                    String targetSUMO = (String) WordNet.wn.getSUMOMapping(targetSynset);
                    if (targetSUMO != null && targetSUMO != "") {
                        if (targetSUMO.charAt(targetSUMO.length()-1) == '[')
                            mappingChar = "[";
                        if (Character.isUpperCase(targetSUMO.charAt(2)))     // char 2 is start of actual term after &%
                            return "&%" + getBareSUMOTerm(targetSUMO) + mappingChar;
                        else {
                            String candidate = findMappingFromHypernym(targetSynset);
                            if (candidate != null && candidate != "")
                                return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** ***************************************************************
     * This is a utility routine that should not be called during
     * normal Sigma operation.  It does most of the actual work for
     * deduceMissingLinks()
     */
    public static void processMissingLinks(String fileName, String pattern, String posNum) throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null;
        LineNumberReader lr = null;
        try {
            fw = new FileWriter(KBmanager.getMgr().getPref("kbDir") + File.separator + fileName + "-new.txt");
            pw = new PrintWriter(fw);

            FileReader r = new FileReader(KBmanager.getMgr().getPref("kbDir") + File.separator + fileName + ".txt");
            lr = new LineNumberReader(r);
            String line;
            while ((line = lr.readLine()) != null) {
                if (lr.getLineNumber() % 1000 == 0)
                    System.out.print('.');
                Pattern p = Pattern.compile(pattern);
                line = line.trim();
                Matcher m = p.matcher(line);
                if (line.indexOf("&%") > -1)
                    pw.println(line.trim());
                else {
                    if (m.matches()) {
                        String synset = posNum + m.group(1);
                        String newTerm = findMappingFromHypernym(synset);
                        if (newTerm != null && newTerm != "") {
                            pw.println(m.group(1) + m.group(2) + "| " + m.group(3) + " " + newTerm);
                            //                            System.out.println("INFO in WordNet.processMissingLinks(): synset, newterm: " +
                            //                                               synset + " " + " " + newTerm);
                        }
                        else {
                            pw.println(line.trim());
                            System.out.println("INFO in WordNet.processMissingLinks(): No term found for synset" +
                                    synset);
                        }
                    }
                    else
                        pw.println(line.trim());
                }
                m = p.matcher(line);
            }
        }
        catch (java.io.IOException e) {
            throw new IOException("Error writing file " + fileName + "\n" + e.getMessage());
        }
        finally {
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                fw.close();
            }
            if (lr != null) {
                lr.close();
            }
        }
    }

    /** ***************************************************************
     * Use the WordNet hyper-/hypo-nym links to deduce a likely link
     * for a SUMO term that has not yet been manually linked.
     * This is a utility routine that should not be called during
     * normal Sigma operation.
     */
    public static void deduceMissingLinks() throws IOException {

        String fileName = "WordNetMappings-nouns";
        String pattern = "^([0-9]{8})([\\S\\s_]+)\\|\\s([\\S\\s]+?)\\s*$";
        String posNum = "1";
        processMissingLinks(fileName,pattern,posNum);
        fileName = "WordNetMappings-verbs";
        pattern = "^([0-9]{8})([^\\|]+)\\|\\s([\\S\\s]+?)\\s*$";
        posNum = "2";
        processMissingLinks(fileName,pattern,posNum);
        fileName = "WordNetMappings-adj";
        pattern = "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s*$";
        posNum = "3";
        processMissingLinks(fileName,pattern,posNum);
        fileName = "WordNetMappings-adv";
        pattern = "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)\\s*$";
        posNum = "4";
        processMissingLinks(fileName,pattern,posNum);
    }

    /** ***************************************************************
     * This is a utility routine that should not be called during
     * normal Sigma operation.  It does most of the actual work for
     * updateWNversion().  The output is a set of WordNet data files
     * with a "-new" suffix.
     */
    public static void updateWNversionProcess(String fileName, String pattern, String posNum) throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null;
        LineNumberReader lr = null;
        try {
            fw = new FileWriter(KBmanager.getMgr().getPref("kbDir") + File.separator + fileName + "-new");
            pw = new PrintWriter(fw);

            FileReader r = new FileReader(KBmanager.getMgr().getPref("kbDir") + File.separator + fileName);
            lr = new LineNumberReader(r);
            String line;
            while ((line = lr.readLine()) != null) {
                if (lr.getLineNumber() % 1000 == 0)
                    System.out.print('.');
                Pattern p = Pattern.compile(pattern);
                line = line.trim();
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String newsynset = posNum + m.group(1);
                    String oldsynset = (String) mappings.get(newsynset);
                    if (oldsynset != null && oldsynset != "") {
                        String term = "";
                        oldsynset = oldsynset.substring(1);
                        switch (posNum.charAt(0)) {
                        case '1': term = (String) WordNet.wn.nounSUMOHash.get(oldsynset); break;
                        case '2': term = (String) WordNet.wn.verbSUMOHash.get(oldsynset); break;
                        case '3': term = (String) WordNet.wn.adjectiveSUMOHash.get(oldsynset); break;
                        case '4': term = (String) WordNet.wn.adverbSUMOHash.get(oldsynset); break;
                        }
                        if (term == null) {
                            pw.println(line.trim());
                            System.out.println("Error in WordNetUtilities.updateWNversionProcess(): No term for synsets (old, new): " +
                                    posNum + oldsynset + " " + posNum + newsynset);
                        }
                        else
                            pw.println(line + " " + term);
                    }
                    else {
                        pw.println(line.trim());
                        System.out.println("Error in WordNetUtilities.updateWNversionProcess(): No mapping for synset: " + newsynset);
                    }
                }
                else
                    pw.println(line.trim());
            }
        }
        catch (java.io.IOException e) {
            throw new IOException("Error writing file " + fileName + "\n" + e.getMessage());
        }
        finally {
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                fw.close();
            }
            if (lr != null) {
                lr.close();
            }
        }
    }

    /** ***************************************************************
     * Read the version mapping files and store in the HashMap
     * called "mappings".
     * Note that the "old" synset should be the second element of each line
     */
    public static void readWNversionMap(String fileName, String pattern, String posNum) throws IOException {

        LineNumberReader lr = null;
        try {
            FileReader r = new FileReader(fileName);
            lr = new LineNumberReader(r);
            String line;
            while ((line = lr.readLine()) != null) {
                if (lr.getLineNumber() % 1000 == 0)
                    System.out.print('.');
                Pattern p = Pattern.compile(pattern);
                line = line.trim();
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String newsynset = posNum + m.group(1);
                    String oldsynset = posNum + m.group(2);
                    mappings.put(newsynset,oldsynset);
                }
                else
                    System.out.println("INFO in WordNetUtilities.updateWNversionReading(): no match for line: " + line);
            }
        }
        catch (java.io.IOException e) {
            throw new IOException("Error writing file " + fileName + "\n" + e.getMessage());
        }
        finally {
            if (lr != null) {
                lr.close();
            }
        }
    }

    /** ***************************************************************
     * Note that the "old" synset should be the second element of each line
     */
    public static void updateWNversionReading(String path, String versionPair) throws IOException {

        String fileName = path + "wn" + versionPair + ".noun";
        String pattern = "^(\\d+) (\\d+) .*$";
        String posNum = "1";
        readWNversionMap(fileName,pattern,posNum);
        fileName = path + "wn" + versionPair + ".verb";
        pattern = "^(\\d+) (\\d+) .*$";
        posNum = "2";
        readWNversionMap(fileName,pattern,posNum);
        fileName = path + "wn" + versionPair + ".adj";
        pattern = "^(\\d+) (\\d+) .*$";
        posNum = "3";
        readWNversionMap(fileName,pattern,posNum);
        fileName = path + "wn" + versionPair + ".adv";
        pattern = "^(\\d+) (\\d+) .*$";
        posNum = "4";
        readWNversionMap(fileName,pattern,posNum);
    }

    /** ***************************************************************
     * Port the mappings from one version of WordNet to another. It
     * calls updateWNversionReading to do most of the work. It assumes
     * that the mapping file has the new synset first and the old one
     * second.  File names are for the new WordNet version, which will
     * need to have different names from the old version that WordNet.java
     * needs to read in order to get the existing mappings.
     * This is a utility which should not be called during normal Sigma
     * operation.  Mapping files are in a simple format produced by
     * University of Catalonia and available at
     * http://www.lsi.upc.edu/~nlp/web/index.php?option=com_content&task=view&id=21&Itemid=57
     * If that address changes you may also start at
     * http://www.lsi.upc.edu/~nlp/web/ and go to Resources and then an
     * item on WordNet mappings.
     */
    public static void updateWNversion(String path, String versionPair) throws IOException {

        // versionPair="30-21" to map version numbers from 2.1 to 3.0
        String fileName = "wn" + versionPair + ".noun";
        String pattern = "^(\\d+) (\\d+) .*$";
        String posNum = "1";
        updateWNversionReading(path, versionPair);

        fileName = "data3.noun";
        pattern = "^([0-9]{8}) .+$";
        posNum = "1";
        updateWNversionProcess(fileName,pattern,posNum);
        fileName = "data3.verb";
        pattern = "^([0-9]{8}) .+$";
        posNum = "2";
        updateWNversionProcess(fileName,pattern,posNum);
        fileName = "data3.adj";
        pattern = "^([0-9]{8}) .+$";
        posNum = "3";
        updateWNversionProcess(fileName,pattern,posNum);
        fileName = "data3.adv";
        pattern = "^([0-9]{8}) .+$";
        posNum = "4";
        updateWNversionProcess(fileName,pattern,posNum);
    }

    /** ***************************************************************
     * @return the number of synsets in WordNet for the given part of
     * speech
     */
    public static int numSynsets(char pos) {

        switch (pos) {
            case '1': return WordNet.wn.nounDocumentationHash.keySet().size();
            case '2': return WordNet.wn.verbDocumentationHash.keySet().size();
            case '3': return WordNet.wn.adjectiveDocumentationHash.keySet().size();
            case '4': return WordNet.wn.adverbDocumentationHash.keySet().size();
        }
        System.out.println("Error in WordNetUtilities.numSynsets(): bad pos: " + pos);
        return 0;
    }

    /** ***************************************************************
     */
    public static String printStatistics() {

        HashSet<String> mappedSUMOterms = new HashSet<String>();
        int totalInstanceMappings = 0;
        int totalSubsumingMappings = 0;
        int totalEquivalenceMappings = 0;
        int instanceMappings = 0;
        int subsumingMappings = 0;
        int equivalenceMappings = 0;
        StringBuffer result = new StringBuffer();
        result.append("<table><tr><td></td><td>instance</td><td>equivalence</td><td>subsuming</td><td></td></tr>\n");
        Iterator<String> it = WordNet.wn.nounSUMOHash.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String value = (String) WordNet.wn.nounSUMOHash.get(key);
            if (value.endsWith("="))
                equivalenceMappings++;
            if (value.endsWith("+"))
                subsumingMappings++;
            if (value.endsWith("@"))
                instanceMappings++;
            mappedSUMOterms.add(value.substring(0,value.length()-1));
        }
        result.append("<tr><td>noun</td><td>" + instanceMappings + "</td><td>" +
                equivalenceMappings + "</td><td>" + subsumingMappings + "</td><td></td></tr>\n");

        totalInstanceMappings = totalInstanceMappings + instanceMappings;
        totalSubsumingMappings = totalSubsumingMappings + subsumingMappings;
        totalEquivalenceMappings = totalEquivalenceMappings + equivalenceMappings;
        instanceMappings = 0;
        subsumingMappings = 0;
        equivalenceMappings = 0;
        it = WordNet.wn.verbSUMOHash.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            String value = (String) WordNet.wn.verbSUMOHash.get(key);
            if (value.endsWith("="))
                equivalenceMappings++;
            if (value.endsWith("+"))
                subsumingMappings++;
            if (value.endsWith("@"))
                instanceMappings++;
            mappedSUMOterms.add(value.substring(0,value.length()-1));
        }
        result.append("<tr><td>verb</td><td>" + instanceMappings + "</td><td>" +
                equivalenceMappings + "</td><td>" + subsumingMappings + "</td><td></td></tr>\n");

        totalInstanceMappings = totalInstanceMappings + instanceMappings;
        totalSubsumingMappings = totalSubsumingMappings + subsumingMappings;
        totalEquivalenceMappings = totalEquivalenceMappings + equivalenceMappings;
        instanceMappings = 0;
        subsumingMappings = 0;
        equivalenceMappings = 0;
        it = WordNet.wn.adjectiveSUMOHash.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            String value = (String) WordNet.wn.adjectiveSUMOHash.get(key);
            if (value.endsWith("="))
                equivalenceMappings++;
            if (value.endsWith("+"))
                subsumingMappings++;
            if (value.endsWith("@"))
                instanceMappings++;
            mappedSUMOterms.add(value.substring(0,value.length()-1));
        }
        result.append("<tr><td>adjective</td><td>" + instanceMappings + "</td><td>" +
                equivalenceMappings + "</td><td>" + subsumingMappings + "</td><td></td></tr>\n");

        totalInstanceMappings = totalInstanceMappings + instanceMappings;
        totalSubsumingMappings = totalSubsumingMappings + subsumingMappings;
        totalEquivalenceMappings = totalEquivalenceMappings + equivalenceMappings;
        instanceMappings = 0;
        subsumingMappings = 0;
        equivalenceMappings = 0;
        it = WordNet.wn.adverbSUMOHash.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            String value = (String) WordNet.wn.adverbSUMOHash.get(key);
            if (value.endsWith("="))
                equivalenceMappings++;
            if (value.endsWith("+"))
                subsumingMappings++;
            if (value.endsWith("@"))
                instanceMappings++;
            mappedSUMOterms.add(value.substring(0,value.length()-1));
        }
        result.append("<tr><td>adverb</td><td>" + instanceMappings + "</td><td>" +
                equivalenceMappings + "</td><td>" + subsumingMappings + "</td><td></td></tr>\n");

        totalInstanceMappings = totalInstanceMappings + instanceMappings;
        totalSubsumingMappings = totalSubsumingMappings + subsumingMappings;
        totalEquivalenceMappings = totalEquivalenceMappings + equivalenceMappings;
        int grandTotal =  totalInstanceMappings +  totalSubsumingMappings + totalEquivalenceMappings;
        result.append("<tr><td><b>total</b></td><td>" + totalInstanceMappings + "</td><td>" +
                totalEquivalenceMappings + "</td><td>" + totalSubsumingMappings + "</td><td><b>" +
                grandTotal + "</b></td></tr>\n");
        result.append("</table><P>\n");
        result.append("Mapped unique SUMO terms: " + mappedSUMOterms.size() + "<p>\n");
        return result.toString();
    }

    /** ***************************************************************
     *  Import links from www.image-net.org that are linked to
     *  WordNet and links them to SUMO terms when the synset has a
     *  directly equivalent SUMO term
     */
    public void imageNetLinks() throws IOException {

        String filename = "nounLinks.txt";
        LineNumberReader lr = null;
        System.out.println("In WordNetUtilities.imageNetLinks()");
        try {
            FileReader r = new FileReader(filename);
            lr = new LineNumberReader(r);
            String l;
            while ((l = lr.readLine()) != null) {
                //System.out.println(";; " + l);
                String synset = l.substring(1,9);
                String url = l.substring(10);
                String term = (String) WordNet.wn.nounSUMOHash.get(synset);
                //System.out.println(synset);
                //System.out.println(term);
                //if (term.endsWith("=")) {
                term = term.substring(2,term.length()-1);
                System.out.println("(externalImage " + term + " \"" + url + "\")");
                //}
            }
        }
        catch (java.io.IOException e) {
            throw new IOException("Error writing file " + filename + "\n" + e.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (lr != null) {
                lr.close();
            }
        }
    }

    /** ***************************************************************
     */
    private static boolean excludedStringsForMeronymy(String s1, String s2) {

        if (s1.indexOf("genus_") > -1 ||
            s2.indexOf("genus_") > -1 ||
            s1.indexOf("order_") > -1 ||
            s2.indexOf("order_") > -1 ||
            s1.indexOf("family_") > -1 ||
            s2.indexOf("family_") > -1 ||
            s1.indexOf("_family") > -1 ||
            s2.indexOf("_family") > -1 ||
            s1.indexOf("division_") > -1 ||
            s2.indexOf("division_") > -1)
            return true;
        else
            return false;
    }

    /** ***************************************************************
     *  A utility to extract meronym relations as relations between
     *  SUMO terms.  Filter out relations between genus and species,
     *  which shouldn't be meronyms
     */
    public static void extractMeronyms() {

        System.out.println("; All meronym relations from WordNet other than genus membership is filtered out");
        Iterator<String> it = WordNet.wn.relations.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            ArrayList<AVPair> al = WordNet.wn.relations.get(key);
            for (int i = 0; i < al.size(); i++) {
                AVPair avp = (AVPair) al.get(i);
                if (avp.attribute.equals("member meronym") ||
                        avp.attribute.equals("substance meronym") ||
                        avp.attribute.equals("part meronym")) {
                    avp.attribute = avp.attribute.replaceAll(" ", "_");
                    String value = avp.value;
                    String SUMO1 = WordNet.wn.getSUMOMapping(key);
                    String SUMO2 = WordNet.wn.getSUMOMapping(value);
                    String keywordlist = WordNet.wn.synsetsToWords.get(key).toString();
                    String valuewordlist = WordNet.wn.synsetsToWords.get(value).toString();
                    if (!excludedStringsForMeronymy(keywordlist,valuewordlist)) {
                        System.out.println("; " + WordNet.wn.synsetsToWords.get(key)); //ArrayList<String>
                        System.out.println("; " + WordNet.wn.synsetsToWords.get(value));
                        if (SUMO1 != null && SUMO2 != null)
                            System.out.println("(" + avp.attribute + " " + SUMO2.substring(2,SUMO2.length()-1) +
                                    " " + SUMO1.substring(2,SUMO1.length()-1) + ")");
                    }
                }
            }
        }
    }

    /** *************************************************************
     * Take a file of <id>tab<timestamp>tab<string> and calculate
     * the average Levenshtein distance for each ID.
     */
    public static void searchCoherence(String fileWithPath) {

        String line;
        String lastT = "";
        String id = "";
        int count = 0;
        int total = 0;
        try {
            File f = new File(fileWithPath);
            FileReader r = new FileReader(f);
            LineNumberReader lr = new LineNumberReader(r);
            while ((line = lr.readLine()) != null) {
                //System.out.println(line);
                int tabIndex = line.indexOf("\t");
                if (tabIndex > -1) {
                    String uid = line.substring(0,tabIndex);
                    tabIndex = line.indexOf("\t",tabIndex+1);
                    String t = line.substring(tabIndex + 1, line.length());
                    //System.out.println("Found tab: t, uid, id, lastT: " + t + " " + uid
                    //                    + " " + id+ " " + lastT);
                    if (!id.equals(uid)) {
                        if (id != "" && count != 0)
                            System.out.println("***** Total for " + id + " is " + total/count);
                        count = 0;
                        total = 0;
                        id = uid;
                    }
                    if (lastT != "") {
                        int l = Mapping.getLevenshteinDistance(lastT,t);
                        if (l != 0) {  // exclude searches with no changes
                            total = total + l;
                            count++;
                        }
                    }
                    lastT = t;
                }
            }
            if (id != "" && count != 0)
                System.out.println("***** Total for " + id + " is " + total/count);
        }
        catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            ioe.printStackTrace();
        }
    }

    /** *************************************************************
     */
    public static void commentSentiment(String fileWithPath) {

        String line;
        try {
            File f = new File(fileWithPath);
            FileReader r = new FileReader(f);
            LineNumberReader lr = new LineNumberReader(r);
            while ((line = lr.readLine()) != null) {
                //System.out.println(line);
                int tabIndex = line.indexOf("\t");
                if (tabIndex > -1) {
                    String comment = line.substring(0,tabIndex);
                    String uid = line.substring(tabIndex + 1, line.length());
                    System.out.println("UID: " + uid + " Sentiment: " + DB.computeSentiment(comment));
                }
            }
            lr.close();
        }
        catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            ioe.printStackTrace();
        }
    }

    /** ***************************************************************
     */
    private static void writeTPTPWordNetClassDefinitions(PrintWriter pw) throws IOException {

        ArrayList<String> WordNetClasses =
            new ArrayList<String>(Arrays.asList("s__Synset","s__NounSynset","s__VerbSynset","s__AdjectiveSynset","s__AdverbSynset"));
        Iterator<String> it = WordNetClasses.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            if (!term.equals("s__Synset")) {
                pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__subclass(" + term + ",s__Synset))).");
                String POS = term.substring(0,term.indexOf("Synset"));
                pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                        ",axiom,(s__documentation(" + term + ",s__EnglishLanguage,\"A group of " + POS +
                        "s having the same meaning.\"))).");
            }
        }
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__documentation(s__WordSense,s__EnglishLanguage,\"A particular sense of a word.\"))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__documentation(s__Word,s__EnglishLanguage,\"A particular word.\"))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__documentation(s__VerbFrame,s__EnglishLanguage,\"A string template showing allowed form of use of a verb.\"))).");
    }

    /** ***************************************************************
     */
    private static void writeTPTPVerbFrames(PrintWriter pw) throws IOException {

        for (int i = 0; i < WordNet.VerbFrames.size(); i ++) {
            String frame = WordNet.VerbFrames.get(i);
            String numString = String.valueOf(i);
            if (numString.length() == 1)
                numString = "0" + numString;
            pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                    ",axiom,(s__documentation(s__WN30VerbFrame_" + numString + ",s__EnglishLanguage,\"" + frame + "\"))).");
        }
    }

    protected static ArrayList<String> WordNetRelations = new ArrayList<String>(Arrays.asList("antonym",
            "hypernym", "instance_hypernym", "hyponym", "instance_hyponym",
            "member_holonym", "substance_holonym", "part_holonym", "member_meronym",
            "substance_meronym", "part_meronym", "attribute", "derivationally_related",
            "domain_topic", "member_topic", "domain_region", "member_region",
            "domain_usage", "member_usage", "entailment", "cause", "also_see",
            "verb_group", "similar_to", "participle", "pertainym"));

    /** ***************************************************************
     */
    private static void writeTPTPWordNetRelationDefinitions(PrintWriter pw) throws IOException {

        Iterator<String> it = WordNetRelations.iterator();
        while (it.hasNext()) {
            String rel = (String) it.next();
            String tag = null;
            if (rel.equals("antonym") || rel.equals("similar-to") ||
                rel.equals("verb-group") || rel.equals("derivationally-related"))
                pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__instance(s__" + rel + "__m,s__SymmetricRelation))).");
            else
                pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__instance(s__" + rel + "__m,s__BinaryRelation))).");
            pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__domain(s__" + rel + "__m,1,s__Synset))).");
            pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__domain(s__" + rel + "__m,2,s__Synset))).");
        }

        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__instance(s__word__m,s__BinaryRelation))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__domain(s__word__m,1,s__Synset))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__domain(s__word__m,2,s__Literal))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__documentation(s__word__m,s__EnglishLanguage,\"A relation between a WordNet synset and a word " +
                   "which is a member of the synset\"))).");

        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__instance(s__singular__m,s__BinaryRelation))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__domain(s__singular__m,1,s__Word))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__domain(s__singular__m,2,s__Literal))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__documentation(s__singular__m,s__EnglishLanguage,\"A relation between a WordNet synset and a word " +
                   "which is a member of the synset.\"))).");

        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__instance(s__infinitive__m,s__BinaryRelation))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__domain(s__infinitive__m,1,s__Word))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__domain(s__infinitive__m,2,s__Literal))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__documentation(s__infinitive__m,s__EnglishLanguage,\"A relation between a word " +
                   " in its past tense and infinitive form.\"))).");

        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__instance(s__senseKey__m,s__BinaryRelation))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__domain(s__senseKey__m,1,s__Word))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__domain(s__senseKey__m,2,s__WordSense))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__documentation(s__senseKey__m,s__EnglishLanguage,\"A relation between a word " +
                   "and a particular sense of the word.\"))).");

        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__instance(s__synset__m,s__BinaryRelation))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__domain(s__synset__m,1,s__WordSense))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__domain(s__synset__m,2,s__Synset))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__documentation(s__synset__m,s__EnglishLanguage,\"A relation between a sense of a particular word " +
                   "and the synset in which it appears.\"))).");

        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__instance(s__verbFrame__m,s__BinaryRelation))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__domain(s__verbFrame__m,1,s__WordSense))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__domain(s__verbFrame__m,2,s__VerbFrame))).");
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ +
                ",axiom,(s__documentation(s__verbFrame__m,s__EnglishLanguage,\"A relation between a verb word sense and a template that "+
                   "describes the use of the verb in a sentence.\"))).");
    }

    /** ***************************************************************
     * Write TPTP format for SUMO-WordNet mappings.
     * @param synset is a POS prefixed synset number
     */
    private static void writeTPTPWordNetSynset(PrintWriter pw, String synset) {

        //if (synset.startsWith("WN30-")) 
        //    synset = synset.substring(5);
        ArrayList<String> al = WordNet.wn.synsetsToWords.get(synset);
        if (al != null) {
            String parent = "Noun";
            switch (synset.charAt(0)) {
              case '1': parent = "NounSynset"; break;
              case '2': parent = "VerbSynset"; break;
              case '3': parent = "AdjectiveSynset"; break;
              case '4': parent = "AdverbSynset"; break;
            }
            pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__instance(s__WN30_" +
                    synset + ",s__" + parent + "))).\n");
            for (int i = 0; i < al.size(); i++) {
                String word = al.get(i);
                String wordAsID = StringUtil.StringToPrologID(word);
                pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__word(s__WN30_" +
                        synset + ",s__WN30Word_" + wordAsID + "))).\n");
            }
            String doc = null;
            switch (synset.charAt(0)) {
              case '1': doc = (String) WordNet.wn.nounDocumentationHash.get(synset.substring(1)); break;
              case '2': doc = (String) WordNet.wn.verbDocumentationHash.get(synset.substring(1)); break;
              case '3': doc = (String) WordNet.wn.adjectiveDocumentationHash.get(synset.substring(1)); break;
              case '4': doc = (String) WordNet.wn.adverbDocumentationHash.get(synset.substring(1)); break;
            }

            //pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__documentation(s__WN30_" + 
            //        synset + ",s__EnglishLanguage,\"" + StringUtil.escapeQuoteChars(doc) + "\")))."); 
            ArrayList<AVPair> al2 = WordNet.wn.relations.get(synset);
            if (al2 != null) {
                for (int i = 0; i < al2.size(); i++) {
                    AVPair avp = al2.get(i);
                    String rel = StringUtil.StringToPrologID(avp.attribute);
                    pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__" + rel + "(s__WN30_" +
                            synset + ",s__WN30_" + avp.value + "))).\n");
                }
            }
        }
    }

    /** ***************************************************************
     */
    private static void writeTPTPWordNetExceptions(PrintWriter pw) throws IOException {

        Iterator<String> it = WordNet.wn.exceptionNounHash.keySet().iterator();
        while (it.hasNext()) {
            String plural = it.next();
            String singular = WordNet.wn.exceptionNounHash.get(plural);
            pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__instance(s__" +
                    StringUtil.StringToPrologID(singular) + ",s__Word))).\n");
            pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__singular(s__" +
                    StringUtil.StringToPrologID(singular) + ",s__" + StringUtil.StringToPrologID(plural) + "))).\n");
            //pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__documentation(s__" + 
            //        StringUtil.StringToPrologID(singular) + ",s__EnglishLanguage,\"'" + 
            //        singular + "', is the singular form" +
            //           " of the irregular plural '" + plural + "'\"))).\n");
        }
        it = WordNet.wn.exceptionVerbHash.keySet().iterator();
        while (it.hasNext()) {
            String past = it.next();
            String infinitive = (String) WordNet.wn.exceptionVerbHash.get(past);
            pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__instance(s__" +
                    StringUtil.StringToPrologID(infinitive) + ",s__Word))).\n");
            pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__past(s__" +
                    StringUtil.StringToPrologID(infinitive) + ",s__" + StringUtil.StringToPrologID(past) + "))).\n");
            //pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__documentation(s__" + 
            //        StringUtil.StringToPrologID(past) + ",s__EnglishLanguage,\"'" + 
            //        past + "', is the irregular past tense form" +
            //           " of the infinitive '" + infinitive + "'\"))).\n");
        }
    }

    /** ***************************************************************
     */
    private static void writeTPTPOneWordToSenses(PrintWriter pw, String word) {

        String wordAsID = StringUtil.StringToPrologID(word);
        pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__instance(s__WN30Word_" + wordAsID + ",s__Word))).\n");
        String wordOrPhrase = "word";
        if (word.indexOf("_") != -1)
            wordOrPhrase = "phrase";
        //pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__documentation(s__WN30Word_" + 
        //        wordAsID + ",s__EnglishLanguage,\"The English " + wordOrPhrase + " '" + word + "'\"))).\n");
        ArrayList<String> senses = WordNet.wn.wordsToSenseKeys.get(word);
        if (senses != null) {
            for (int i = 0; i < senses.size(); i++) {
                String sense = StringUtil.StringToPrologID(senses.get(i));
                pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__senseKey(s__WN30Word_" +
                        wordAsID + ",s__WN30WordSense_" + sense + "))).\n");
            }
        }
        else
            System.out.println("Error in WordNetUtilities.writeTPTPOneWordToSenses(): no senses for word: " + word);
    }

    /** ***************************************************************
     */
    private static void writeTPTPWordsToSenses(PrintWriter pw) throws IOException {

        Iterator<String> it = WordNet.wn.wordsToSenseKeys.keySet().iterator();
        while (it.hasNext()) {
            String word = (String) it.next();
            writeTPTPOneWordToSenses(pw, word);
        }
    }

    /** ***************************************************************
     */
    private static void writeTPTPSenseIndex(PrintWriter pw) throws IOException {

        Iterator<String> it = WordNet.wn.senseIndex.keySet().iterator();
        while (it.hasNext()) {
            String sense = it.next();
            String synset = StringUtil.StringToPrologID(WordNet.wn.senseIndex.get(sense));
            pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__instance(s__" +
                    StringUtil.StringToPrologID(sense) + ",s__WordSense))).\n");
            //pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__documentation(s__" + 
            //        StringUtil.StringToPrologID(sense) + ",s__EnglishLanguage,\"The WordNet word sense '" + 
            //       sense + "'\"))).\n");
            String pos = WordNetUtilities.getPOSfromKey(sense);
            String word = WordNetUtilities.getWordFromKey(sense);
            String posNum = WordNetUtilities.posLettersToNumber(pos);
            pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__synset(s__" +
                    StringUtil.StringToPrologID(sense) + ",s__WN30_" + posNum + synset + "))).\n");
            if (posNum.equals("2")) {
                ArrayList<String> frames = WordNet.wn.verbFrames.get(synset + "-" + word);
                if (frames != null) {
                    for (int i = 0; i < frames.size(); i++) {
                        String frame = frames.get(i);
                        pw.println("fof(kb_WordNet_" + TPTPidCounter++ + ",axiom,(s__verbFrame(s__" +
                                StringUtil.StringToPrologID(sense) + ",\"" + frame + "\"))).\n");
                    }
                }
            }
        }
    }

    /** ***************************************************************
     */
    private static void writeTPTPWordNetHeader(PrintWriter pw) {

        pw.println("# An expression of the Princeton WordNet " +
                "( http://wordnet.princeton.edu ) " +
                "in TPTP.  Use is subject to the Princeton WordNet license at " +
                "http://wordnet.princeton.edu/wordnet/license/");
        Date d = new Date();
        pw.println("#Produced on date: " + d.toString());
    }

    /** ***************************************************************
     * Write TPTP format for WordNet
     */
    public static void writeTPTPWordNet(PrintWriter pw) throws IOException {

        System.out.println("INFO in WordNetUtilities.writeTPTPWordNet()");

        writeTPTPWordNetHeader(pw);
        writeTPTPWordNetRelationDefinitions(pw);
        writeTPTPWordNetClassDefinitions(pw);
          // Get POS-prefixed synsets.
        Iterator<String> it = WordNet.wn.synsetsToWords.keySet().iterator();
        while (it.hasNext()) {
            String synset = it.next();
            writeTPTPWordNetSynset(pw,synset);
        }
        //writeTPTPWordNetExceptions(pw);
        //writeTPTPVerbFrames(pw);
        writeTPTPWordsToSenses(pw);
        writeTPTPSenseIndex(pw);
    }

    /** ***************************************************************
     * Find all the leaf nodes for a particular relation in WordNet.
     * Note that the leaf must have a link from another node to be a
     * leaf.  No isolated nodes can be considered leaves.
     * @return a list of POS-prefixed synsets
     */
    public static HashSet<String> findLeavesInTree(HashSet<String> rels)  {

        // first find all valid nodes that are pointed to
        HashSet<String> valid = new HashSet<>();
        for (String s : WordNet.wn.relations.keySet()) {
            ArrayList<AVPair> avpList = WordNet.wn.relations.get(s);
            Iterator<AVPair> it = avpList.iterator();
            while (it.hasNext()) {
                AVPair avp = it.next();
                if (rels.contains(avp.attribute))
                    valid.add(avp.value);
            }
        }

        HashSet<String> result = new HashSet<>();
        for (String s : WordNet.wn.relations.keySet()) {
            ArrayList<AVPair> avpList = WordNet.wn.relations.get(s);
            boolean found = false;
            Iterator<AVPair> it = avpList.iterator();
            while (it.hasNext() && !found) {
                AVPair avp = it.next();
                if (rels.contains(avp.attribute))
                    found = true;
            }
            if (!found && valid.contains(s))
                result.add(s);
        }
        return result;
    }

    /** ***************************************************************
     * Find the complete path from a given synset.  If multiple
     * inheritance results in multiple paths, return them all.
     */
    public static ArrayList<ArrayList<String>> findPathsToRoot(ArrayList<String> base, String synset) {

        //System.out.println("WordNetUtilities.findPathsToRoot(): base: " + base);
        //System.out.println("WordNetUtilities.findPathsToRoot(): synset: " +
        //        WordNet.wn.getWordsFromSynset(synset).get(0) + "-" + synset);
        ArrayList<ArrayList<String>> result = new ArrayList<>();
        if (base.contains(synset) || synset.equals("100001740")) { // catch cycles, stop at "entity"
            ArrayList<String> path = new ArrayList<>();
            path.addAll(base);
            path.add(synset);
            result.add(path);
            return result;
        }
        ArrayList<AVPair> links = WordNet.wn.relations.get(synset);
        if (links != null) {
            for (AVPair link : links) {
                if (link == null)
                    System.out.println("Error in WordNetUtilities.findPathsToRoot(): null link");
                else if (link.attribute.equals("hypernym") || link.attribute.equals("instance hypernym")) {
                    //System.out.println("WordNetUtilities.findPathsToRoot(): link: " + link);
                    ArrayList<String> path = new ArrayList<>();
                    path.addAll(base);
                    path.add(synset);
                    result.addAll(findPathsToRoot(path,link.value));
                }
            }
        }
        return result;
    }

    /** ***************************************************************
     */
    private static String lowestCommonParentInner(ArrayList<String> path,
                                                  ArrayList<ArrayList<String>> paths, int cursor) {

        Iterator<ArrayList<String>> it = paths.iterator();
        while (it.hasNext()) {
            ArrayList<String> path2 = it.next();
            int index1 = path.size() - cursor - 1;
            int index2 = path2.size() - cursor - 1;
            if (index1 < 0 || index2 < 0)
                return null;
            //System.out.println("lowestCommonParentInner(): index: " + index1);
            //System.out.println("lowestCommonParentInner(): index: " + index2);
            //System.out.println("lowestCommonParentInner() 1: " + path.get(index1));
            //System.out.println("lowestCommonParentInner() 2: " + path2.get(index2));
            if (path2.get(index2).equals(path.get(index1)))
                return path.get(index1);
        }
        return null;
    }

    /** ***************************************************************
     */
    private static String lowestCommonParent(ArrayList<ArrayList<String>> paths1,
                                            ArrayList<ArrayList<String>> paths2, int cursor) {

        String bestSyn = null;
        Iterator<ArrayList<String>> it = paths1.iterator();
        while (it.hasNext()) {
            String result = lowestCommonParentInner(it.next(), paths2, cursor);
            if (result != null)
                bestSyn = result;
        }
        return bestSyn;
    }

    /** ***************************************************************
     */
    public static String lowestCommonParent(String s1, String s2) {

        ArrayList<String> base1 = new ArrayList<String>();
        ArrayList<String> base2 = new ArrayList<String>();
        ArrayList<ArrayList<String>> paths1 = findPathsToRoot(base1, s1);
        ArrayList<ArrayList<String>> paths2 = findPathsToRoot(base2,s2);
        int cursor = 0;
        String bestSyn = "100001740"; // entity
        String result = bestSyn;
        while (result != null) {
            result = lowestCommonParent(paths1,paths2,cursor);
            cursor++;
            if (result != null)
                bestSyn = result;
        }
        return bestSyn;
    }

    /** ***************************************************************
     * Find all the leaf nodes for a particular relation in WordNet.
     * Note that a node may be a leaf simply because it has no such
     * link to another node.
     * @return a list of POS-prefixed synsets
     */
    public static HashSet<String> findLeaves(String rel)  {

        HashSet<String> result = new HashSet<>();
        for (String s : WordNet.wn.relations.keySet()) {
            ArrayList<AVPair> avpList = WordNet.wn.relations.get(s);
            boolean found = false;
            Iterator<AVPair> it = avpList.iterator();
            while (it.hasNext() && !found) {
                AVPair avp = it.next();
                if (avp.attribute.equals(rel))
                    found = true;
            }
            if (!found)
                result.add(s);
        }
        return result;
    }

    /** ***************************************************************
     */
    public static void showAllLeaves() {

        try {
            KBmanager.getMgr().initializeOnce();
            HashSet<String> hs = findLeavesInTree(Sets.newHashSet("hyponym","instance hyponym"));
            int count = 0;
            System.out.println();
            System.out.println("====================================");
            for (String s: hs) {
                System.out.print(WordNet.wn.getWordsFromSynset(s).get(0)+"-" + s + ", ");
                if (count++ > 6) {
                    System.out.println();
                    count = 0;
                }
            }
            System.out.println();
            System.out.println("====================================");
        }
        catch (Exception e) {
            System.out.println("Error in WordNetUtilities.main(): Exception: " + e.getMessage());
        }
    }

    /** ***************************************************************
     */
    public static void showAllRoots() {

        try {
            KBmanager.getMgr().initializeOnce();
            HashSet<String> hs = findLeavesInTree(Sets.newHashSet("hypernym","instance hypernym"));
            int count = 0;
            System.out.println();
            System.out.println("====================================");
            for (String s: hs) {
                System.out.print(WordNet.wn.getWordsFromSynset(s).get(0)+"-" + s + ", ");
                if (count++ > 6) {
                    System.out.println();
                    count = 0;
                }
            }
            System.out.println();
            System.out.println("====================================");
        }
        catch (Exception e) {
            System.out.println("Error in WordNetUtilities.main(): Exception: " + e.getMessage());
        }
    }

    /** ***************************************************************
     * @return POS-prefixed synsets
     */
    public static HashSet<String> wordsToSynsets(String word) {

        HashSet<String> result = new HashSet<String>();
        ArrayList<String> sensekeys = WordNet.wn.wordsToSenseKeys.get(word);
        if (sensekeys == null) {
            //System.out.println("Error in WordNetUtilities.wordsToSynsets(): no synset for : " + word);
            return null;
        }
        for (String s : sensekeys) {
            //System.out.println("Info in WordNetUtilities.wordsToSynsets(): s: " + s);
            String synset =  WordNet.wn.senseIndex.get(s);
            String posnum = WordNetUtilities.getPOSfromKey(s);
            //System.out.println("Info in WordNetUtilities.wordsToSynsets(): pos: " + posnum);
            String posnumint = WordNetUtilities.posLettersToNumber(posnum);
            result.add(posnumint + synset);
        }
        return result;
    }

    /** ***************************************************************
     */
    public static String synsetToOneWord(String s) {

        return WordNet.wn.getWordsFromSynset(s).get(0);
    }


    /** ***************************************************************
     * Is the given 9 digit sysnset one constructed from SUMO termFormat
     * expressions?
     */
    public static boolean nonWNsynset(String s) {

        if (s.charAt(0) == '1') {
            if (s.substring(1).compareTo(WordNet.wn.origMaxNounSynsetID) > 0)
                return true;
            else
                return false;
        }
        else if (s.charAt(0) == '2') {
            if (s.substring(1).compareTo(WordNet.wn.origMaxVerbSynsetID) > 0)
                return true;
            else
                return false;
        }
        else
            return false;
    }

    /** ***************************************************************
     */
    private static void addSenseSet(Hashtable<String,String> SUMOhash,
                                   String prefix, HashMap<String,HashSet<String>> SUMOs) {

        for (String synset : SUMOhash.keySet()) {
            String SUMO = SUMOhash.get(synset);
            String bareSUMO = getBareSUMOTerm(SUMO);
            String posSynset = prefix + synset;
            HashSet<String> theSet = null;
            if (!SUMOs.containsKey(bareSUMO)) {
                theSet = new HashSet<String>();
                SUMOs.put(bareSUMO, theSet);
            }
            theSet = SUMOs.get(bareSUMO);
            theSet.add(posSynset);
        }
    }

    /** ***************************************************************
     * @return a Set of Sets where each interior Set consists of
     * WordNet word senses that all map to a single SUMO term.
     * The goal is to provide a way to collapse WordNet synsets that
     * embody overly fine grained distinctions.
     */
    public static HashMap<String,HashSet<String>> collapseSenses() {

        HashMap<String,HashSet<String>> SUMOs = new HashMap<>();
        //nounSUMOHash = new Hashtable<String,String>();   // Keys are synset Strings, values are SUMO
        //verbSUMOHash = new Hashtable<String,String>();   // terms with the &% prefix and =, +, @ or [ suffix.
        //djectiveSUMOHash = new Hashtable<String,String>();
        //adverbSUMOHash = new Hashtable<String,String>();
        addSenseSet(WordNet.wn.nounSUMOHash,"1",SUMOs);
        addSenseSet(WordNet.wn.verbSUMOHash,"2",SUMOs);
        addSenseSet(WordNet.wn.adjectiveSUMOHash,"3",SUMOs);
        addSenseSet(WordNet.wn.adverbSUMOHash, "4", SUMOs);
        return SUMOs;
    }

    /** ***************************************************************
     * @return all the hyponyms of a given POS-prefixed synset
     */
    public static HashSet<String> getAllHyponyms(String s) {

        //System.out.println("getAllHyponyms(): " + s);
        HashSet<String> result = new HashSet<String>();
        ArrayList<AVPair> rels = WordNet.wn.relations.get(s);
        // System.out.println("getAllHyponyms() rels: " + rels);
        if (rels == null)
            return result;
        for (AVPair avp : rels) {
            if (avp.attribute.equals("hyponym") || avp.attribute.equals("instance hyponym"))
                result.addAll(getAllHyponyms(avp.value));
        }
        result.add(s);
        return result;
    }

    /** ***************************************************************
     * @return all the hyponyms of a given POS-prefixed synset
     */
    public static HashSet<String> getAllHyponymsTransitive(String s) {

        //System.out.println("getAllHyponymsTransitive(): input: " + s);
        HashSet<String> result = new HashSet<String>();
        result.addAll(getAllHyponyms(s));
        boolean changed = true;
        while (changed) {
            HashSet<String> newresult = new HashSet<String>();
            newresult.addAll(result);
            for (String str : result)
                newresult.addAll(getAllHyponyms(str));
            //System.out.println("getAllHyponymsTransitive(): " + newresult);
            if (newresult.size() == result.size())
                changed = false;
            else
                changed = true;
            result = newresult;
        }
        return result;
    }

    /** ***************************************************************
     * @return whether the word is a possible hyponym of a given POS-prefixed synset
     */
    public static boolean isHyponymousWord(String word, HashSet<String> synsets) {

        if (StringUtil.emptyString(word) || synsets == null || synsets.isEmpty()) {
            System.out.println("WordNetUtilities.isHyponymousWord(): bad inputs");
            System.out.println("word: '" + word + "' synsets: " + synsets);
            return false;
        }
        for (String synset : synsets) {
            HashSet<String> hypo = getAllHyponymsTransitive(synset);
            HashSet<String> words = new HashSet<>();
            for (String s : hypo) {
                if (WordNet.wn.synsetsToWords.keySet().contains(s)) {
                    ArrayList<String> newWords = WordNet.wn.synsetsToWords.get(s);
                    //System.out.println("WordNetUtilities.isHyponymousWord(): words: "  + newWords);
                    words.addAll(newWords);
                }
            }
            if (words.contains(word))
                return true;
        }
        return false;
    }

    /** ***************************************************************
     * Generate sets of all hyponymous words for each synset in a file
     */
    public static void generateHyponymSets (String filename) {

        List<String> lines = new ArrayList<String>();
        System.out.println("INFO in WordNetUtilities.generateHyponymSets(): Reading files");
        LineNumberReader lr = null;
        try {
            String line;
            StringBuffer doc = new StringBuffer();
            File nounFile = new File(filename);
            if (nounFile == null) {
                System.out.println("Error in WordNetUtilities.generateHyponymSets(): The file does not exist ");
                return;
            }
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(nounFile);
            lr = new LineNumberReader(r);
            while ((line = lr.readLine()) != null) {
                if (lr.getLineNumber() % 1000 == 0)
                    System.out.print('.');
                System.out.println("==============");
                System.out.println(line.trim());
                ArrayList<String> words = WordNet.wn.synsetsToWords.get(line.trim());
                if (words != null)
                    System.out.println(words);
                HashSet<String> hyps = getAllHyponymsTransitive(line.trim());
                for (String s : hyps) {
                    words = WordNet.wn.synsetsToWords.get(s);
                    System.out.println(words);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (lr != null) {
                    lr.close();
                }
            }
            catch (Exception ex) {
            }
        }
    }

    /** ***************************************************************
     * Generate notional SUMO terms from WordNet
     */
    public static void generateSUMOfromWNsubtree(String synset, String sumo) {

    }

    /** ***************************************************************
     * Generate notional SUMO terms from WordNet
     */
    public static void generateSUMOfromWN(String synset, String sumo) {

    }

    /** ***************************************************************
     * Generate notional SUMO terms from WordNet.  Start with an equivalence
     * Make each synset a notional SUMO term with its parent either the
     * synset parent or the equivalence.
     */
    public static void generateSUMOfromWN() {

        for (String syn : WordNet.wn.nounSUMOHash.keySet()) { // Keys are synset Strings, values are SUMO
            String sumo = WordNet.wn.nounSUMOHash.get(syn);   // terms with the &% prefix and =, +, @ or [ suffix.
            if (sumo.endsWith("="))
                generateSUMOfromWNsubtree("1"+syn,sumo);
        }
        for (String syn : WordNet.wn.verbSUMOHash.keySet()) {
            String sumo = WordNet.wn.verbSUMOHash.get(syn);
            if (sumo.endsWith("="))
                generateSUMOfromWNsubtree("2"+syn,sumo);
        }
        for (String syn : WordNet.wn.adjectiveSUMOHash.keySet()) {
            String sumo = WordNet.wn.adjectiveSUMOHash.get(syn);
            if (sumo.endsWith("="))
                generateSUMOfromWNsubtree("3"+syn,sumo);
        }
        for (String syn : WordNet.wn.adverbSUMOHash.keySet()) {
            String sumo = WordNet.wn.adverbSUMOHash.get(syn);
            if (sumo.endsWith("="))
                generateSUMOfromWNsubtree("4"+syn,sumo);

        }
    }

    /** ***************************************************************
     * get all synsets corresponding to a SUMO term
     */
    public static ArrayList<String> getSynsetsFromSUMO(String sumo) {

        return WordNet.wn.SUMOHash.get(sumo);
    }

    /** ***************************************************************
     * Convert verb frame indexes as Strings into actual vrb frame strings.
     * For example "1" becomes "Something ----s"
     */
    public static ArrayList<String> convertVerbFrameNumbersToFrames(ArrayList<String> numbers) {

        ArrayList<String> res = new ArrayList<>();
        if (numbers == null)
            return res;
        for (String s : numbers)
            res.add(WordNet.VerbFrames.get(Integer.parseInt(s)));
        return res;
    }

    /** ***************************************************************
     * get all verb frames corresponding to a synset.
     * @param synset is a 9-digit synset
     * Note! The verb frame key takes an 8-digit synset
     */
    public static ArrayList<String> getVerbFramesForSynset(String synset) {

        ArrayList<String> res = new ArrayList<>();
        if (synset.length() == 8) {
            System.out.println("Error in WordNetUtilities.getVerbFramesForSynset(): 8 digit synset");
            return res;
        }
        ArrayList<String> numbers = WordNet.wn.verbFrames.get(synset.substring(1));
        return convertVerbFrameNumbersToFrames(numbers);
    }

    /** ***************************************************************
     * get all verb frames corresponding to a word in a synset.  Include
     * verb frames common to all words in the synset.
     * @param synset is a 9-digit synset
     * Note! The verb frame key takes an 8-digit synset
     */
    public static ArrayList<String> getVerbFramesForWord(String synset, String word) {

        ArrayList<String> res = new ArrayList<>();
        if (synset.length() < 9) {
            System.out.println("Error in WordNetUtilities.getVerbFramesForWord(): 8 digit synset: " + synset);
            return res;
        }
        if (WordNet.wn.verbFrames.containsKey(synset.substring(1)))
            res.addAll(WordNet.wn.verbFrames.get(synset.substring(1)));
        if (WordNet.wn.verbFrames.containsKey(synset.substring(1) + "-" + word))
            res.addAll(WordNet.wn.verbFrames.get(synset.substring(1) + "-" + word));
        return convertVerbFrameNumbersToFrames(res);
    }

    /** ***************************************************************
     * get all verb frames corresponding to a word in a synset.
     * @param map is a set of word keys and the values are the verb frames
     * @param words are all the words in a given synset
     */
    public static ArrayList<String> doVerbFrameSubstitution(HashMap<String,ArrayList<String>> map,
                                                            ArrayList<String> words) {

        ArrayList<String> result = new ArrayList<>();
        for (String w : map.keySet()) {
            if (w.equals("all")) {
                for (String word : words) {
                    for (String f : map.get(w)) {
                        if (!StringUtil.isNumeric(f))
                            continue;
                        int index = Integer.parseInt(f);
                        String frame = WordNet.wn.VerbFrames.get(index);
                        frame = frame.replace("----",word);
                        result.add(frame);
                    }
                }
            }
            else {
                for (String f : map.get(w)) {
                    int index = Integer.parseInt(f);
                    String frame = WordNet.wn.VerbFrames.get(index);
                    frame = frame.replace("----", w);
                    result.add(frame);
                }
            }
        }
        return result;
    }

    /** ***************************************************************
     * get all verb frames corresponding to a synset.
     * @param synset is a 9-digit synset
     * Note! The verb frame key takes an 8-digit synset
     */
    public static HashMap<String,ArrayList<String>> getAllVerbFrames(String synset,
                                                                     ArrayList<String> words) {

        HashMap<String,ArrayList<String>> res = new HashMap<>();
        res.put("all",getVerbFramesForSynset(synset));
        System.out.println("showVerbFrames(1): res: " + res);
        if (res.get("all") == null)
            res.put("all",new ArrayList<>());
        for (String w : words) {
            if (WordNet.wn.verbFrames.containsKey(synset.substring(1) + "-" + w)) {
                res.put(w,getVerbFramesForWord(synset,w));
                System.out.println("showVerbFrames(2): res: " + res);
            }
        }
        return res;
    }

    /** ***************************************************************
     * get all verb frames corresponding to a synset.
     * @param synset is a 9-digit synset
     * Note! The verb frame key takes an 8-digit synset
     */
    public static String showVerbFrames(String synset) {

        System.out.println("showVerbFrames(): synset: " + synset);
        StringBuffer sb = new StringBuffer();
        ArrayList<String> words = WordNet.wn.getWordsFromSynset(synset);
        System.out.println("showVerbFrames(): words: " + words);
        HashMap<String,ArrayList<String>> res = new HashMap<>();
        res.put("all",getVerbFramesForSynset(synset));
        System.out.println("showVerbFrames(1): res: " + res);
        if (res.get("all") == null)
            res.put("all",new ArrayList<>());
        for (String w : words) {
            if (WordNet.wn.verbFrames.containsKey(synset.substring(1) + "-" + w)) {
                res.put(w,getVerbFramesForWord(synset,w));
                System.out.println("showVerbFrames(2): res: " + res);
            }
        }
        if (res.size() > 0) {
            sb.append("<b>Verb Frames</b><P>\n");
            ArrayList<String> stringList = doVerbFrameSubstitution(res,words);
            for (String s : stringList)
                sb.append(s + "<br>\n");
            sb.append("<P>\n");
        }
        return sb.toString();
    }

    /** ***************************************************************
     * get all verb synsets corresponding to a SUMO term that are equivalence links
     */
    public static ArrayList<String> getEquivalentVerbSynsetsFromSUMO(String sumo) {

        ArrayList<String> result = new ArrayList<>();
        if (WordNet.wn == null || WordNet.wn.SUMOHash == null) {
            System.out.println("Error in getEquivalentVerbSynsetsFromSUMO(): WordNet not loaded");
            return null;
        }
        if (sumo == null || sumo == "") {
            System.out.println("Error in getEquivalentVerbSynsetsFromSUMO(): null input");
            return null;
        }
        ArrayList<String> synlist = WordNet.wn.SUMOHash.get(sumo);
        if (synlist == null) return result;
        //System.out.println("getEquivalentVerbSynsetsFromSUMO(): synlist: " + synlist);
        for (String s : synlist) {
            //System.out.println("getEquivalentVerbSynsetsFromSUMO(): synset: " + s);
            if (s.charAt(0) != '2')  // get only verb mappings
                continue;
            String SUMO = WordNet.wn.getSUMOMapping(s);
            //System.out.println("getEquivalentVerbSynsetsFromSUMO(): mapping: " + SUMO);
            char suffix = WordNetUtilities.getSUMOMappingSuffix(SUMO);
            if (suffix == '=')
                result.add(s);
        }
        return result;
    }

    /** ***************************************************************
     * get all verb synsets corresponding to a SUMO term
     */
    public static ArrayList<String> getVerbSynsetsFromSUMO(String sumo) {

        ArrayList<String> result = new ArrayList<>();
        if (WordNet.wn == null || WordNet.wn.SUMOHash == null) {
            System.out.println("Error in getVerbSynsetsFromSUMO(): WordNet not loaded");
            return null;
        }
        if (sumo == null || sumo == "") {
            System.out.println("Error in getVerbSynsetsFromSUMO(): null input");
            return null;
        }
        ArrayList<String> synlist = WordNet.wn.SUMOHash.get(sumo);
        //System.out.println("getVerbSynsetsFromSUMO(): synlist: " + synlist);
        if (synlist == null) return result;
        for (String s : synlist) {
            if (s.charAt(0) != '2')  // get only verb mappings
                continue;
            result.add(s);
        }
        return result;
    }

    /** ***************************************************************
     * get all synsets corresponding to a SUMO term that are equivalence links
     */
    public static ArrayList<String> getEquivalentSynsetsFromSUMO(String sumo) {

        ArrayList<String> result = new ArrayList<>();
        if (WordNet.wn == null || WordNet.wn.SUMOHash == null) {
            System.out.println("Error in getEquivalentSynsetsFromSUMO(): WordNet not loaded");
            return null;
        }
        if (sumo == null || sumo == "") {
            System.out.println("Error in getEquivalentSynsetsFromSUMO(): null input");
            return null;
        }
        ArrayList<String> synlist = WordNet.wn.SUMOHash.get(sumo);
        if (synlist == null) return result;
        for (String s : synlist) {
            String SUMO = WordNet.wn.getSUMOMapping(s);
            char suffix = WordNetUtilities.getSUMOMappingSuffix(SUMO);
            if (suffix == '=')
                result.add(s);
        }
        return result;
    }

    /** ***************************************************************
     * get all synsets corresponding to a list of SUMO terms
     */
    public static HashSet<String> getSynsetsFromSUMOList(Collection<String> sumo) {

        HashSet<String> result = new HashSet<>();
        for (String s : sumo) {
            ArrayList<String> synsets = WordNet.wn.SUMOHash.get(s);
            if (synsets != null)
                result.addAll(synsets);
        }
        return result;
    }

    /** ***************************************************************
     * get all words corresponding to a list of synsets
     */
    public static HashSet<String> getWordsFromSynsetList(Collection<String> synsets) {

        HashSet<String> result = new HashSet<>();
        for (String s : synsets) {
            ArrayList<String> words = WordNet.wn.getWordsFromSynset(s);
            if (words != null)
                result.addAll(words);
        }
        return result;
    }

    /** ***************************************************************
     */
    public static String rootFormOf(String word) {

        String rootWord = word;

        String nounroot = WordNet.wn.nounRootForm(word, word.toLowerCase());
        String verbroot = WordNet.wn.verbRootForm(word, word.toLowerCase());

        if (!StringUtil.emptyString(nounroot) && !nounroot.equals(word))
            rootWord = nounroot;
        else if (!StringUtil.emptyString(verbroot) && !verbroot.equals(word)) {
            rootWord = verbroot;
        }

        return rootWord;
    }

    /** ***************************************************************
     * Find all words which are more than one list of words pertaining
     * to a given human sense (as well as a list of emotions and a list
     * of thought words)
     */
    private static HashSet<String> findWordIntersections(HashMap<String,HashSet<String>> words) {

        HashSet<String> synesthesiaWords = new HashSet<>();
        HashSet<String> inverses = new HashSet<>();
        for (String s1 : words.keySet()) {
            for (String s2 : words.keySet()) {
                if (!s1.equals(s2) && !inverses.contains(s2 + "_" + s1)) {
                    HashSet<String> temp = new HashSet<>();
                    temp.addAll(words.get(s1));
                    temp.retainAll(words.get(s2));
                    if (temp.size() > 0) {
                        System.out.println(s1 + " : " + s2 + "\n" + temp + "\n\n");
                        synesthesiaWords.addAll(temp);
                    }
                    inverses.add(s1 + "_" + s2);
                }
            }
        }
        return synesthesiaWords;
    }

    /** ***************************************************************
     * result is a side effect on "orig"
     */
    private static void addTermAndSubs(KB kb, HashSet<String> orig, String term) {

        HashSet<String> newterms = null;
        if (kb.isAttribute(term))
            newterms = (HashSet) kb.getAllSub(term,"subAttribute");
        else
            newterms = kb.kbCache.getChildClasses(term);
        if (newterms != null)
            orig.addAll(newterms);
        orig.add(term);
    }

    public static boolean withThoughtEmotion = false;

    /** ***************************************************************
     * Find all words associated with sensory, psychological and
     * emotional concepts.  Return a set of words with String keys
     * as to the human sense plus "emotion" and "thought"
     */
    public static HashMap<String,HashSet<String>> sensoryWords() {

        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        KBcache cache = kb.kbCache;
        System.out.println();
        System.out.println("INFO in WordNetUtilities.sensorySynsets(): ");
        // Hearing, Seeing, Smelling, TactilePerception, Tasting
        // OlfactoryAttribute, SoundAttribute, TasteAttribute, TextureAttribute, VisualAttribute
        HashSet<String> tasteTerms = new HashSet<>();
        HashSet<String> smellTerms = new HashSet<>();
        HashSet<String> sightTerms = new HashSet<>();
        HashSet<String> touchTerms = new HashSet<>();
        HashSet<String> soundTerms = new HashSet<>();

        HashSet<String> perceptionTerms = new HashSet<>();

        HashSet<String> emotionTerms = new HashSet<>();
        HashSet<String> thoughtTerms = new HashSet<>();

        addTermAndSubs(kb, tasteTerms, "Tasting");
        addTermAndSubs(kb, tasteTerms, "TasteAttribute");

        addTermAndSubs(kb, soundTerms, "Hearing");
        addTermAndSubs(kb, soundTerms, "RadiatingSound");
        addTermAndSubs(kb, soundTerms, "Music");
        addTermAndSubs(kb, soundTerms, "MusicalInstrument");
        addTermAndSubs(kb, soundTerms, "MusicGenre");
        addTermAndSubs(kb, soundTerms, "MusicalGroup");
        addTermAndSubs(kb, soundTerms, "SoundAttribute");

        addTermAndSubs(kb, smellTerms, "Smelling");
        addTermAndSubs(kb, smellTerms, "OlfactoryAttribute");

        addTermAndSubs(kb, sightTerms, "Seeing");
        addTermAndSubs(kb, sightTerms, "RadiatingLight");
        addTermAndSubs(kb, sightTerms, "VisualAttribute");

        addTermAndSubs(kb, touchTerms, "Touching");
        addTermAndSubs(kb, touchTerms, "TextureAttribute");
        addTermAndSubs(kb, touchTerms, "TemperatureAttribute");
        addTermAndSubs(kb, touchTerms, "ShapeAttribute");

        addTermAndSubs(kb, perceptionTerms, "PerceptualAttribute");

        addTermAndSubs(kb, thoughtTerms, "PsychologicalProcess");
        addTermAndSubs(kb, thoughtTerms, "PsychologicalAttribute");

        addTermAndSubs(kb, emotionTerms, "EmotionalState");

        HashSet<String> tasteSynsets = getSynsetsFromSUMOList(tasteTerms);
        HashSet<String> smellSynsets = getSynsetsFromSUMOList(smellTerms);
        HashSet<String> sightSynsets = getSynsetsFromSUMOList(sightTerms);
        HashSet<String> touchSynsets = getSynsetsFromSUMOList(touchTerms);
        HashSet<String> soundSynsets = getSynsetsFromSUMOList(soundTerms);
        HashSet<String> perceptionSynsets = getSynsetsFromSUMOList(perceptionTerms);
        HashSet<String> emotionSynsets = getSynsetsFromSUMOList(emotionTerms);
        HashSet<String> thoughtSynsets = getSynsetsFromSUMOList(thoughtTerms);

        HashMap<String,HashSet<String>> words = new HashMap<>();
        words.put("taste",getWordsFromSynsetList(tasteSynsets));
        words.put("smell",getWordsFromSynsetList(smellSynsets));
        words.put("sight",getWordsFromSynsetList(sightSynsets));
        words.put("touch",getWordsFromSynsetList(touchSynsets));
        words.put("sound",getWordsFromSynsetList(soundSynsets));

        if (withThoughtEmotion) {
            words.put("perception",getWordsFromSynsetList(perceptionSynsets));
            words.put("emotion", getWordsFromSynsetList(emotionSynsets));
            words.put("thought", getWordsFromSynsetList(thoughtSynsets));
        }
        return words;
    }

    /** ***************************************************************
     * Find all the words that exhibit links to multiple sensory modes
     * in SUMO
     */
    public static HashSet<String> synestheticSynsets(HashMap<String,HashSet<String>> words) {

        HashSet<String> synWords = findWordIntersections(words);
        System.out.println();
        System.out.println("synesthesiaCompare(): synWords list: " + synWords);
        System.out.println("count of synWords list: " + synWords.size());
        return synWords;
    }

    /** ***************************************************************
     */
    private static String removeIsh(String s) {

        if (s.endsWith("ish")) {
            String result = s.substring(0, s.length() - 3);
            System.out.println("WordNetUtilities.removeIsh(): " + s + " : " + result);
            return result;
        }
        else
            return s;
    }

    /** ***************************************************************
     * Compare Lievers list of synesthetic words with those derived from
     * SUMO-WordNet
     */
    public static void synesthesiaCompare(HashMap<String,HashSet<String>> words,
                                          HashSet<String> synwords) {

        System.out.println();
        ArrayList<ArrayList<String>> lieversFile = readFileIntoArray(System.getenv("CORPORA") +
                File.separator + "LieversLexiconList.txt");
        TreeSet<String> lievers = new TreeSet<>();
        TreeSet<String> lieversRoots = new TreeSet<>();
        for (ArrayList<String> al : lieversFile) {
            lievers.addAll(al);
        }
        for (String s : lievers) {
            String root = MultiWords.rootFormOf(s);
            if (WordNet.wn.containsWord(removeIsh(root)))
                root = removeIsh(root);
            if (root != null && !lievers.contains(root) && WordNet.wn.containsWord(root))
                lieversRoots.add(root);
            else
                lieversRoots.add(s);
        }
        lievers = new TreeSet<>();
        lievers.addAll(lieversRoots);

        TreeSet<String> sumo = new TreeSet<>();
        for (HashSet<String> hs : words.values()) {
            sumo.addAll(hs);
        }
        System.out.println("synesthesiaCompare(): lievers list: " + lievers);
        System.out.println("count of lievers list: " + lievers.size());
        System.out.println();
        System.out.println("synesthesiaCompare(): sumo list: " + sumo);
        System.out.println("count of sumo list: " + sumo.size());

        TreeSet<String> lieversOverlap = new TreeSet<>();
        lieversOverlap.addAll(lievers);
        System.out.println();
        System.out.println("synesthesiaCompare(): lievers list overlap: " + lieversOverlap.retainAll(sumo));
        System.out.println("count: " + lieversOverlap.size());

        TreeSet<String> sumoDiff = new TreeSet<>();
        sumoDiff.addAll(sumo);
        sumoDiff.removeAll(lievers);
        System.out.println();
        System.out.println("synesthesiaCompare(): sumo not in lievers: " + sumoDiff);
        System.out.println("count: " + sumoDiff.size());

        TreeSet<String> lieversSynNonOverlap = new TreeSet<>();
        lieversSynNonOverlap.addAll(lievers);
        lieversSynNonOverlap.removeAll(synwords);
        System.out.println();
        System.out.println("synesthesiaCompare(): lievers not in sumo syn: " + lieversSynNonOverlap);
        System.out.println("count: " + lieversSynNonOverlap.size());

        lievers.removeAll(sumo);
        TreeSet<String> newlievers = new TreeSet<>();
        for (String s : lievers)
            if (!sensoryOrMentalWord(s))
                newlievers.add(s);
        System.out.println();
        System.out.println("synesthesiaCompare(): lievers not in sumo: " + newlievers);
        System.out.println("count: " + lievers.size());
    }

    /** ***************************************************************
     *  A method used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void testCommonParent() {

        // showAllLeaves();
        // showAllRoots();
        try {
            KBmanager.getMgr().initializeOnce();
            ArrayList<String> base = new ArrayList<String>();
            ArrayList<ArrayList<String>> result = findPathsToRoot(base, "102858304");
            for (ArrayList<String> path : result) {
                int count = 0;
                for (String s: path) {
                    System.out.print(WordNet.wn.getWordsFromSynset(s).get(0) + "-" + s + ", ");
                    if (count++ > 6) {
                        System.out.println();
                        count = 0;
                    }
                }
            }
            System.out.println();
            ArrayList<String> base2 = new ArrayList<String>();
            ArrayList<ArrayList<String>> result2 = findPathsToRoot(base2, "102958343");
            for (ArrayList<String> path : result2) {
                int count = 0;
                for (String s: path) {
                    System.out.print(WordNet.wn.getWordsFromSynset(s).get(0) + "-" + s + ", ");
                    if (count++ > 6) {
                        System.out.println();
                        count = 0;
                    }
                }
            }
            System.out.println("\nparent: " + lowestCommonParent("102858304", "102958343"));
            //extractMeronyms();
            //FileWriter fw = new FileWriter("WNout.tptp");
            //PrintWriter pw = new PrintWriter(fw);
            //pw.flush();
            //writeTPTPWordNet(pw);
            //pw.flush();
        }
        catch (Exception e) {
            System.out.println("Error in WordNetUtilities.main(): Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     * test if a word is sensory or mental and return true if so
     */
    public static boolean sensoryOrMentalWord(String word) {

        String[] parents = {"Tasting", "TasteAttribute", "Hearing", "RadiatingSound",
                "Music", "MusicalInstrument", "MusicGenre", "MusicalGroup", "SoundAttribute", "Smelling",
                "OlfactoryAttribute", "Seeing", "RadiatingLight", "VisualAttribute",
                "Touching", "TextureAttribute", "PerceptualAttribute", "TemperatureAttribute",
                "ShapeAttribute", "PsychologicalProcess","PsychologicalAttribute", "EmotionalState"};

        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        if (testWordDebug) System.out.println("WordNetUtilities.testWord(): word: " + word);
        HashSet<String> synsets = wordsToSynsets(word);
        if (synsets == null)
            return false;
        String root = MultiWords.rootFormOf(word);
        HashSet<String> rootSynsets = wordsToSynsets(root);
        if (rootSynsets != null)
            synsets.addAll(rootSynsets);
        if (testWordDebug) System.out.println("WordNetUtilities.testWord(): synsets: " + synsets);
        HashSet<String> terms = new HashSet<>();
        for (String s : synsets) {
            terms.add(WordNet.wn.getSUMOMapping(s));
        }
        if (testWordDebug) System.out.println("WordNetUtilities.testWord(): terms: " + terms);
        String parent = null;
        String child = null;
        for (String s : parents) {
            for (String t : terms) {
                String bare = getBareSUMOTerm(t);
                if (bare != null && s != null && kb.childOf(bare,s)) {
                    parent = s;
                    child = bare;
                }
            }
        }
        if (parent != null && child != null) {
            if (testWordDebug)
                System.out.println("WordNetUtilities.testWord(): word: " + word +
                    " mapping to term " + child + " has parent sensory or mental term " + parent);
            return true;
        }
        return false;
    }

    /** ***************************************************************
     */
    private static HashSet<String> readDomain(String domain) {

        HashSet<String> result = new HashSet<>();
        String filename = System.getenv("CORPORA") + File.separator +
                "WordNet-3.0" + File.separator + "wn-domains-3.0.txt";
        ArrayList<ArrayList<String>> lines = DB.readSpreadsheet(filename, null, false, ' ');
        for (ArrayList<String> line : lines) {
            if (line == null && line.size() < 3)
                continue;
            if (line.contains(domain))
                result.add(line.get(0));
        }
        return result;
    }

    /** ***************************************************************
     * print out legal synsets and SUMO mappings
     */
    private static void lawDomainInfo() {

        HashSet<String> synsets = readDomain("law");
        for (String synset : synsets) {
            String id = synset.substring(0,8);
            char pos = synset.charAt(synset.length()-1);
            char posnum = posLetterToNumber(pos);
            String synset9 = posnum + id;
            Collection<String> words = WordNet.wn.getWordsFromSynset(synset9);
            String doc = WordNet.wn.getDocumentation(synset9);
            String sumo = WordNet.wn.getSUMOMapping(synset9);
            System.out.println(synset9 + "\n" + words + "\n" + doc  + "\n" + sumo + "\n\n");
        }
    }

    /** ***************************************************************
     */
    private static boolean testWordDebug = false;

    /** ***************************************************************
     */
    public static void testWord() {

        testWordDebug = true;
        sensoryOrMentalWord("burning");
        sensoryOrMentalWord("audible");
        sensoryOrMentalWord("chug");
        sensoryOrMentalWord("pianola");
    }

    /** ***************************************************************
     */
    public static void testSynesthesia() {

        HashMap<String,HashSet<String>> sensoryWords = sensoryWords();
        HashSet<String> synwords = synestheticSynsets(sensoryWords);
        synesthesiaCompare(sensoryWords,synwords);
    }

    /** ***************************************************************
     */
    public static void testGetPOS() {

        String s = "four-lane_AS_1";
        String POS = WordNetUtilities.getPOSfromKey(s);
        String posWord = WordNetUtilities.posAlphaKeyToWord(POS);
        System.out.println("testGetPOS(): "  + s + " " + POS + " " + posWord);
    }

    /** ***************************************************************
     *  A method used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void testIsValidKey() {

        System.out.println("INFO in WordNetUtilities.testIsValidKey(): " + isValidKey("morale_NN_1"));
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("KB class");
        System.out.println("  options (with a leading '-'):");
        System.out.println("  h - show this help screen");
        System.out.println("  w \"word\" - show WordNet display page");
        System.out.println("  t \"term\" - get words from SUMO Term");
    }

    /** ***************************************************************
     *  A main method, used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void main (String[] args) {

        System.out.println("INFO in WordNet.main()");
        if (args == null || args.length == 0 || args[0].equals("-h"))
            showHelp();
        else {
            KBmanager.getMgr().initializeOnce();
            String kbName = KBmanager.getMgr().getPref("sumokbname");
            if (args != null && args.length > 1 && args[0].equals("-w")) {
                String result = WordNet.wn.page(StringUtil.removeEnclosingQuotes(args[1]), 0, kbName, "", "");
                System.out.println(StringUtil.removeHTML(result));
            }
            else if (args != null && args.length > 1 && args[0].equals("-t")) {
                String term = StringUtil.removeEnclosingQuotes(args[1]);
                System.out.println("term: " +  term);
                System.out.println(WordNet.wn.getWordsFromTerm(term).keySet());
            }
            else {
                System.out.println("no such option or no option : " + args);
                showHelp();
            }
        }
    }
}

