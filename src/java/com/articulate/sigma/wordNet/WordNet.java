/** This code is copyright Articulate Software (c) 2003-2007.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net

 Authors:
 Adam Pease
 Infosys LTD.
 */

package com.articulate.sigma.wordNet;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.AVPair;
import com.articulate.sigma.utils.MapUtils;
import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.articulate.sigma.wordNet.WordNetUtilities.isValidKey;

/** ***************************************************************
 *  This program finds and displays SUMO terms that are related in meaning to the English
 *  expressions that are entered as input.  Note that this program uses four WordNet data
 *  files, "NOUN.EXC", "VERB.EXC" etc, as well as four WordNet to SUMO
 *  mappings files called "WordNetMappings-nouns.txt", "WordNetMappings-verbs.txt" etc
 *  The main part of the program prompts the user for an English term and then
 *  returns associated SUMO concepts.  The two primary public methods are initOnce() and page().
 *  @author Ian Niles
 *  @author Adam Pease
 */
public class WordNet implements Serializable {

    public static boolean disable = false;
    public static boolean debug = false;
    public static WordNet wn  = new WordNet();

    /* A map of language name to wordnets */
    //public static HashMap<String,WordNet> wns = new HashMap<String,WordNet>();
    public static String baseDir = "";
    public static File baseDirFile = null;
    public static boolean initNeeded = true;

    private static HashMap<String,String> wnFilenames = new HashMap<>();

    /** This array contains all of the compiled Pattern objects that
     * will be used by methods in this file. */
    public static Pattern[] regexPatterns = null;

    public HashMap<String,HashSet<String>> nounSynsetHash = new HashMap<>();   // Words in root form are String keys,
    public HashMap<String,HashSet<String>> verbSynsetHash = new HashMap<>();   // String values are 8-digit synset lists.
    public HashMap<String,HashSet<String>> adjectiveSynsetHash = new HashMap<>();
    public HashMap<String,HashSet<String>> adverbSynsetHash = new HashMap<>();

    // @see caseMap
    public HashMap<String,HashSet<String>> ignoreCaseSynsetHash = new HashMap<>(); // uppercase keys to synsets covering all POS

    public Hashtable<String,String> verbDocumentationHash = new Hashtable<String,String>();       // Keys are synset Strings, values
    public Hashtable<String,String> adjectiveDocumentationHash = new Hashtable<String,String>();  // are documentation strings.
    public Hashtable<String,String> adverbDocumentationHash = new Hashtable<String,String>();
    public Hashtable<String,String> nounDocumentationHash = new Hashtable<String,String>();

    public Hashtable<String,String> nounSUMOHash = new Hashtable<String,String>();   // Keys are synset Strings, values are SUMO
    public Hashtable<String,String> verbSUMOHash = new Hashtable<String,String>();   // terms with the &% prefix and =, +, @ or [ suffix.
    public Hashtable<String,String> adjectiveSUMOHash = new Hashtable<String,String>();
    public Hashtable<String,String> adverbSUMOHash = new Hashtable<String,String>();

    public String maxNounSynsetID = "";
    public String maxVerbSynsetID = "";

    public String origMaxNounSynsetID = "";
    public String origMaxVerbSynsetID = "";
    
    /** Keys are SUMO terms, values are ArrayLists(s) of
     * POS-prefixed 9-digit synset String(s) meaning that the part of speech code is
     * prepended to the synset number. */
    public Hashtable<String,ArrayList<String>> SUMOHash = new Hashtable<String,ArrayList<String>>();

    /** Keys are String POS-prefixed synsets.  Values
     * are ArrayList(s) of String(s) which are words. Note
     * that the order of words in the file is preserved. */
    public Hashtable<String,ArrayList<String>> synsetsToWords = new Hashtable<String,ArrayList<String>>();

    // key is inflected form, value is root
    public HashMap<String,String> exceptionVerbHash = new HashMap<>();
    // key root, value is inflected (-en) form
    public HashMap<String,String> exceptionVerbPastProgHash = new HashMap<>();
    // key root, value  is inflected form
    public HashMap<String,String> exceptionVerbPastHash = new HashMap<String,String>();
    // key root, value  is inflected (-ing) form
    public HashMap<String,String> exceptVerbProgHash = new HashMap<String,String>();

    /** list of irregular plural forms where the key is the
     *  plural, singular is the value. */
    public HashMap<String,String> exceptionNounHash = new HashMap<String,String>();
    // The reverse index of the above
    public HashMap<String,String> exceptionNounPluralHash = new HashMap<String,String>();

    /** Keys are POS-prefixed synsets, values are ArrayList(s) of AVPair(s)
     * in which the attribute is a pointer type according to
     * http://wordnet.princeton.edu/man/wninput.5WN.html#sect3 and
     * the value is a POS-prefixed synset  @see WordNetUtilities.convertWordNetPointer */
    public Hashtable<String,ArrayList<AVPair>> relations = new Hashtable<String,ArrayList<AVPair>>();

    /** a HashMap of HashMaps where the key is a word sense of the
     * form word_POS_num signifying the word, part of speech and number
     * of the sense in WordNet.  The value is a HashMap of words and the
     * number of times that word cooccurs in sentences with the word sense
     * given in the key.  */
    public HashMap<String,HashMap<String,Integer>> wordCoFrequencies = new HashMap<String,HashMap<String,Integer>>();

    /** a HashMap of HashMaps where the key is a word and the value is a 
     * HashMap of 9-digit POS-prefixed senses which is the value of the AVPair,
     * and the number of times that sense occurs in the Brown corpus, which is
     * the key of the AVPair*/
    protected HashMap<String,TreeSet<AVPair>> wordFrequencies = new HashMap<String,TreeSet<AVPair>>();

    // A Map from all uppercase words to their possibly mixed case original versions
    public HashMap<String,String> caseMap = new HashMap<>();

    /** a HashMap where the key is a 9-digit POS-prefixed sense and the value is a
     *  the number of times that sense occurs in the Brown corpus.  */
    public HashMap<String,Integer> senseFrequencies = new HashMap<String,Integer>();

    /** English "stop words" such as "a", "at", "them", which have no or little
     * inherent meaning when taken alone. */
    public ArrayList<String> stopwords = new ArrayList<String>();

    /** A HashMap where the keys are of the form word_POS_sensenum (alpha POS like "VB")
     * and values are 8 digit WordNet synset byte offsets. Note that all words are
     * from index.sense, which reduces all words to lower case */
    public HashMap<String,String> senseIndex = new HashMap<String,String>();

    /** A HashMap where the keys are of the form word%POS:lex_filenum:lex_id (numeric POS)
     * and values are 8 digit WordNet synset byte offsets. Note that all words are
     * from index.sense, which reduces all words to lower case */
    public HashMap<String,String> senseKeys = new HashMap<String,String>();

    /** A HashMap where the keys are 9 digit POS prefixed WordNet synset byte offsets, 
     * and the values are of the form word_POS_sensenum (alpha POS like "VB"). Note
     * that all words are from index.sense, which reduces
     * all words to lower case */
    public HashMap<String,String> reverseSenseIndex = new HashMap<String,String>();
    
    /** A HashMap where keys are 8 digit
     * WordNet synset byte offsets or synsets appended with a dash and a specific
     * word such as "12345678-foo" or in the case where the frame applies to the entire
     * synset, it's just the synset number.  Values are ArrayList(s) of String
     * verb frame numbers. */
    public HashMap<String,ArrayList<String>> verbFrames = new HashMap<String,ArrayList<String>>();

    /** A HashMap with words as keys and ArrayList as values.  The
     * ArrayList contains word senses which are Strings of the form
     * word_POS_num (alpha POS like "VB") signifying the word, part of speech and number of
     * the sense in WordNet. Note that all words are from index.sense, which reduces
     * all words to lower case*/
    public HashMap<String,ArrayList<String>> wordsToSenseKeys = new HashMap<String,ArrayList<String>>();

    public MultiWords multiWords = new MultiWords();

    //private Pattern p;
    private transient Matcher m;

    public static final int NOUN                = 1;
    public static final int VERB                = 2;
    public static final int ADJECTIVE           = 3;
    public static final int ADVERB              = 4;
    public static final int ADJECTIVE_SATELLITE = 5;

    /** A HashMap with language name keys and HashMap<String,String> values.  The interior HashMap
     * has String keys which are PWN30 synsets with 8-digit synsets a dash and then a alphabetic
     * part of speech character.  Values are words in the target language. */
    public HashMap<String,HashMap<String,String>> OMW = new HashMap<String,HashMap<String,String>>();
    
    /**  This array contains all of the regular expression strings that
     * will be compiled to Pattern objects for use in the methods in
     * this file. */
    public static final String[] regexPatternStrings =
    {
        // 0: WordNet.processPointers()
        "^\\s*\\d\\d\\s\\S\\s\\d\\S\\s",

        // 1: WordNet.processPointers()
        "^([a-zA-Z0-9'._\\-]\\S*)\\s([0-9a-f])\\s",

        // 2: WordNet.processPointers()
        "^...\\s",

        // 3: WordNet.processPointers()
        "^(\\S\\S?)\\s([0-9]{8})\\s(.)\\s([0-9a-f]{4})\\s?",

        // 4: WordNet.processPointers()
        "^..\\s",

        // 5: WordNet.processPointers()
        "^\\+\\s(\\d\\d)\\s(\\d\\d)\\s?",

        // 6: WordNet.readNouns()
        "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$",

        // 7: WordNet.readNouns()
        "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$",

        // 8: WordNet.readNouns()
        "(\\S+)\\s+(\\S+)",

        // 9: WordNet.readNouns()
        "(\\S+)\\s+(\\S+)\\s+(\\S+)",

        // 10: WordNet.readVerbs()
        "^([0-9]{8})([^\\|]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$",

        // 11: WordNet.readVerbs()
        "^([0-9]{8})([^\\|]+)\\|\\s([\\S\\s]+)$",

        // 12: WordNet.readVerbs()
        "(\\S+)\\s+(\\S+).*",

        // 13: WordNet.readAdjectives()
        "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$",

        // 14: WordNet.readAdjectives()
        "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$",

        // 15: WordNet.readAdverbs()
        "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$",

        // 16: WordNet.readAdverbs()
        "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$",

        // 17: WordNet.readWordFrequencies()
        "^Word: ([^ ]+) Values: (.*)",

        // 18: WordNet.readSenseIndex()
        "([^%]+)%([^:]*):([^:]*):([^:]*)?:([^:]*)?:([^ ]*)? ([^ ]+)? ([^ ]+).*",

        // 19: WordNet.removePunctuation()
        "(\\w)\\'re",

        // 20: WordNet.removePunctuation()
        "(\\w)\\'m",

        // 21: WordNet.removePunctuation()
        "(\\w)n\\'t",

        // 22: WordNet.removePunctuation()
        "(\\w)\\'ll",

        // 23: WordNet.removePunctuation()
        "(\\w)\\'s",

        // 24: WordNet.removePunctuation()
        "(\\w)\\'d",

        // 25: WordNet.removePunctuation()
        "(\\w)\\'ve"
    };

    public static ArrayList<String> VerbFrames = new ArrayList<String>(Arrays.asList("", // empty 0 index
            "Something ----s",                                      // 1
            "Somebody ----s",
            "It is ----ing",
            "Something is ----ing PP",
            "Something ----s something Adjective/Noun",             // 5
            "Something ----s Adjective/Noun",
            "Somebody ----s Adjective",
            "Somebody ----s something",
            "Somebody ----s somebody",
            "Something ----s somebody",                             // 10
            "Something ----s something",
            "Something ----s to somebody",
            "Somebody ----s on something",
            "Somebody ----s somebody something",
            "Somebody ----s something to somebody",                 // 15
            "Somebody ----s something from somebody",
            "Somebody ----s somebody with something",
            "Somebody ----s somebody of something",
            "Somebody ----s something on somebody",
            "Somebody ----s somebody PP",                           // 20
            "Somebody ----s something PP",
            "Somebody ----s PP",
            "Somebody's (body part) ----s",
            "Somebody ----s somebody to INFINITIVE",
            "Somebody ----s somebody INFINITIVE",                   // 25
            "Somebody ----s that CLAUSE",
            "Somebody ----s to somebody",
            "Somebody ----s to INFINITIVE",
            "Somebody ----s whether INFINITIVE",
            "Somebody ----s somebody into V-ing something",         // 30
            "Somebody ----s something with something",
            "Somebody ----s INFINITIVE",
            "Somebody ----s VERB-ing",
            "It ----s that CLAUSE",
            "Something ----s INFINITIVE"));                         // 35

    public MultiWords getMultiWords() {

        return multiWords;
    }

    /** ***************************************************************
     */
    private void makeFileMap() {
    	
        wnFilenames.put("noun_mappings",    "WordNetMappings30-noun.txt" );
        wnFilenames.put("verb_mappings",    "WordNetMappings30-verb.txt" );
        wnFilenames.put("adj_mappings",     "WordNetMappings30-adj.txt" );
        wnFilenames.put("adv_mappings",     "WordNetMappings30-adv.txt" );
        wnFilenames.put("noun_exceptions",  "noun.exc" );
        wnFilenames.put("verb_exceptions",  "verb.exc" );
        wnFilenames.put("adj_exceptions",   "adj.exc" );
        wnFilenames.put("adv_exceptions",   "adv.exc" );
        wnFilenames.put("sense_indexes",    "index.sense" );
        wnFilenames.put("word_frequencies", "wordFrequencies.txt" );
        wnFilenames.put("cntlist",          "cntlist" );
        wnFilenames.put("stopwords",        "stopwords.txt" );
        wnFilenames.put("messages",         "messages.txt");
    }

    /** ***************************************************************
     * This method compiles all of the regular expression pattern
     * strings in regexPatternStrings and puts the resulting compiled
     * Pattern objects in the Pattern[] regexPatterns.
     */
    public void compileRegexPatterns() {
        
        System.out.println("INFO in WordNet.compileRegexPatterns(): compiling patterns");
        regexPatterns = new Pattern[regexPatternStrings.length];
        for (int i = 0; i < regexPatternStrings.length; i++) {
            regexPatterns[i] = Pattern.compile(regexPatternStrings[i]);
            if (!(regexPatterns[i] instanceof Pattern)) 
                System.out.println("ERROR in WordNet.compileRegexPatterns(): could not compile \""
                        + regexPatternStrings[i] + "\"");            
        }
        return;
    }

    /** ***************************************************************
     * Returns the WordNet File object corresponding to key.
     *
     * @param key A descriptive literal String that maps to a regular
     * expression pattern used to obtain a WordNet file.
     *
     * @return A File object
     */
    public File getWnFile(String key, String override) {
        
        File theFile = null;
        try {
            if (override != null)
                theFile = new File(override);
            else if ((key != null) && (baseDirFile != null))
                theFile = new File(baseDirFile + File.separator + wnFilenames.get(key));
            if (theFile == null || !theFile.exists())
                System.out.println("Error in WordNet.getWnFile(): no such file: " + theFile.getAbsolutePath());
        }
        catch (Exception ex) {
            System.out.println("Error in WordNet.getWnFile(): key: " + key);
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return theFile;
    }

    /** ***************************************************************
     * Add a synset (with part of speech number prefix) and the SUMO
     * term that maps to it.
     */
    private void addSUMOHash(String term, String synset) {

        //System.out.println("INFO in WordNet.addSUMOHash(): SUMO term: " + key);
        //System.out.println("INFO in WordNet.addSUMOHash(): synset: " + value);
        term = term.substring(2,term.length()-1);
        ArrayList<String> synsets = SUMOHash.get(term);
        if (synsets == null) {
            synsets = new ArrayList<String>();
            SUMOHash.put(term,synsets);
        }
        synsets.add(synset);
    }

    /** ***************************************************************
     * Return an ArrayList of the string split by spaces.
     */
    public static ArrayList<String> splitToArrayList(String st) {

        if (StringUtil.emptyString(st)) {
            System.out.println("Error in WordNet.splitToArrayList(): empty string input");
            return null;
        }
        String[] sentar = st.split(" ");
        ArrayList<String> words = new ArrayList<String>(Arrays.asList(sentar));
        for (int i = 0; i < words.size(); i++) {
            if (words.get(i).equals("") || words.get(i) == null || words.get(i).matches("\\s*"))
                words.remove(i);
        }
        return words;
    }
    
    /** ***************************************************************
     * Return an ArrayList of the string split by periods.
     */
    public static ArrayList<String> splitToArrayListSentence(String st) {

        if (st.equals("") || st == null) {
            System.out.println("Error in WordNet.splitToArrayList(): empty string input");
            return null;
        }
        String[] sentar = st.split("\\.\\s");
        ArrayList<String> words = new ArrayList<String>(Arrays.asList(sentar));
        for (int i = 0; i < words.size(); i++) {
            if (words.get(i).equals("") || words.get(i) == null || words.get(i).matches("\\s*"))
                words.remove(i);
        }
        return words;
    }

    /** ***************************************************************
     * Add a synset and its corresponding word to the synsetsToWords
     * variable.  Prefix the synset with its part of speech before adding.
     */
    private void addToSynsetsToWords(String word, String synsetStr, String POS) {

        if (word.indexOf('_') > 0)
            multiWords.addMultiWord(word);
        ArrayList<String> al = synsetsToWords.get(POS + synsetStr);
        if (al == null) {
            al = new ArrayList<String>();
            synsetsToWords.put(POS + synsetStr, al);
        }
        al.add(word);

        HashSet<String> synsets = null;
        switch (POS.charAt(0)) {
        case '1':
            MapUtils.addToMap(nounSynsetHash,word,synsetStr);
            break;
        case '2':
            MapUtils.addToMap(verbSynsetHash,word,synsetStr);
            break;
        case '3':
            MapUtils.addToMap(adjectiveSynsetHash,word,synsetStr);
            break;
        case '4':
            MapUtils.addToMap(adverbSynsetHash,word,synsetStr);
            break;
        }
        //System.out.println("WordNet.addToSynsetsToWords(): " + word.toUpperCase()  + "," + synsetStr);
        MapUtils.addToMap(ignoreCaseSynsetHash,word.toUpperCase(),synsetStr);
    }

    /** ***************************************************************
     * Process some of the fields in a WordNet .DAT file as described at
     * http://wordnet.princeton.edu/man/wndb.5WN . synset must include
     * the POS-prefix.  Input should be of the form
     * lex_filenum  ss_type  w_cnt  word  lex_id  [word  lex_id...]  p_cnt  [ptr...]  [frames...]
     */
    private void processPointers(String synset, String pointers) {

        //System.out.println("INFO in WordNet.processPointers(): " + pointers);
        // 0: p = Pattern.compile("^\\s*\\d\\d\\s\\S\\s\\d\\S\\s");
        m = regexPatterns[0].matcher(pointers);
        pointers = m.replaceFirst("");
        //System.out.println("INFO in WordNet.processPointers(): removed prefix: " + pointers);

        // Should be left with:
        // word  lex_id  [word  lex_id...]  p_cnt  [ptr...]  [frames...]
        // 1: p = Pattern.compile("^([a-zA-Z0-9'._\\-]\\S*)\\s([0-9a-f])\\s");
        m = regexPatterns[1].matcher(pointers);
        while (m.lookingAt()) {
            String word = m.group(1);
            //if (word.equals("roll"))
            //    System.out.println("INFO in WordNet.processPointers(): word: " + word);
            if (word.length() > 3 && (word.substring(word.length()-3,word.length()).equals("(a)") ||
                    word.substring(word.length()-3,word.length()).equals("(p)")))
                word = word.substring(0,word.length()-3);
            if (word.length() > 4 && word.substring(word.length()-4,word.length()).equals("(ip)"))
                word = word.substring(0,word.length()-4);
            //String count = m.group(2);
            addToSynsetsToWords(word,synset.substring(1),synset.substring(0,1));
            pointers = m.replaceFirst("");
            m = regexPatterns[1].matcher(pointers);
        }
        //System.out.println("INFO in WordNet.processPointers(): removed words: " + pointers);

        // Should be left with:
        // p_cnt  [ptr...]  [frames...]
        // 2: p = Pattern.compile("^...\\s");
        m = regexPatterns[2].matcher(pointers);
        pointers = m.replaceFirst("");

        // Should be left with:
        // [ptr...]  [frames...]
        // where ptr is
        // pointer_symbol  synset_offset  pos  source/target
        // 3: p = Pattern.compile("^(\\S\\S?)\\s([0-9]{8})\\s(.)\\s([0-9a-f]{4})\\s?");
        m = regexPatterns[3].matcher(pointers);
        while (m.lookingAt()) {
            String ptr = m.group(1);
            String targetSynset = m.group(2);
            String targetPOS = m.group(3);
            //String sourceTarget = m.group(4);
            targetPOS = (Character.valueOf(WordNetUtilities.posLetterToNumber(targetPOS.charAt(0)))).toString();
            pointers = m.replaceFirst("");
            m = regexPatterns[3].matcher(pointers);
            ptr = WordNetUtilities.convertWordNetPointer(ptr);
            AVPair avp = new AVPair();
            avp.attribute = ptr;
            avp.value = targetPOS + targetSynset;
            ArrayList<AVPair> al = new ArrayList<AVPair>();
            if (relations.keySet().contains(synset))
                al = relations.get(synset);
            else {
                relations.put(synset,al);
            }
            //System.out.println("INFO in WordNet.processPointers(): (" + avp.attribute +
            //                   " " + synset + " " + avp.value);
            al.add(avp);
        }
        if ( (pointers != null)
                && (pointers != "")
                && (pointers.length() > 0)
                && !pointers.equals(" ") ) {
            // Only for verbs may we have the following leftover
            // f_cnt + f_num  w_num  [ +  f_num  w_num...]
            if (synset.charAt(0) == '2') {
                // 4: p = Pattern.compile("^..\\s");
                m = regexPatterns[4].matcher(pointers);
                pointers = m.replaceFirst("");
                // 5: p = Pattern.compile("^\\+\\s(\\d\\d)\\s(\\d\\d)\\s?");
                m = regexPatterns[5].matcher(pointers);
                while (m.lookingAt()) {
                    String frameNum = m.group(1);
                    String wordNum = m.group(2);
                    String key = null;
                    if (wordNum.equals("00")) // frame num applies to all words in the synset
                        key = synset.substring(1);
                    else {
                        int num = Integer.valueOf(wordNum).intValue();
                        ArrayList<String> al = synsetsToWords.get(synset);
                        if (al == null)
                            System.out.println("Error in WordNet.processPointers(): " + synset
                                    + " has no words for pointers: \"" + pointers + "\"");
                        String word = (String) al.get(num-1);
                        key = synset.substring(1) + "-" + word;
                    }
                    ArrayList<String> frames = new ArrayList<String>();
                    if (!verbFrames.keySet().contains(key))
                        verbFrames.put(key,frames);
                    else
                        frames = verbFrames.get(key);
                    frames.add(frameNum);
                    pointers = m.replaceFirst("");
                    m = regexPatterns[5].matcher(pointers);
                }
            }
            else {
                System.out.println("Error in WordNet.processPointers(): " +
                        synset.charAt(0) + " leftover pointers: \"" + pointers + "\"");
            }
        }
        return;
    }

    /** ***************************************************************
     */
    private void addSUMOMapping(String SUMO, String synset) {

        SUMO = SUMO.trim();
        switch (synset.charAt(0)) {
        case '1': nounSUMOHash.put(synset.substring(1),SUMO);
        break;
        case '2': verbSUMOHash.put(synset.substring(1),SUMO);
        break;
        case '3': adjectiveSUMOHash.put(synset.substring(1),SUMO);
        break;
        case '4': adverbSUMOHash.put(synset.substring(1),SUMO);
        break;
        }
        addSUMOHash(SUMO, synset);
    }

    /** ***************************************************************
     * Get the SUMO mapping for a POS-prefixed synset
     */
    public String getSUMOMapping(String synset) {

        if (StringUtil.emptyString(synset)) {
            System.out.println("Error in WordNet.getSUMOMapping: null synset " + synset);
            Thread.dumpStack();
            return null;
        }
        switch (synset.charAt(0)) {
            case '1': return (String) nounSUMOHash.get(synset.substring(1));
            case '2': return (String) verbSUMOHash.get(synset.substring(1));
            case '3': return (String) adjectiveSUMOHash.get(synset.substring(1));
            case '4': return (String) adverbSUMOHash.get(synset.substring(1));
            case '5': return (String) adjectiveSUMOHash.get(synset.substring(1));
        }
        System.out.println("Error in WordNet.getSUMOMapping: improper first character for synset: " + synset);
        Thread.dumpStack();
        return null;
    }

    /** ***************************************************************
     *  Create the hashtables nounSynsetHash, nounDocumentationHash,
     *  nounSUMOhash and exceptionNounHash that contain the WordNet
     *  noun synsets, word definitions, mappings to SUMO, and plural
     *  exception forms, respectively.
     *  Throws an IOException if the files are not found.
     *  Use a default filename and path unless a non-null string is
     *  provided, in which case assume it is a full path.
     */
    private void readNouns() throws java.io.IOException {
    	
        System.out.println("INFO in WordNet.readNouns(): Reading WordNet noun files");
        LineNumberReader lr = null;
        try {
            // synset_offset  lex_filenum  ss_type  w_cnt  word  lex_id  [word  lex_id...]  p_cnt  [ptr...]  [frames...]  |   gloss
            String line;
            File nounFile = getWnFile("noun_mappings",null);
            if (nounFile == null) {
                System.out.println("Error in WordNet.readNouns(): The noun mappings file does not exist in " + baseDir );
                return;
            }
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(nounFile);
            // System.out.println( "INFO in WordNet.readNouns(): Reading file " + nounFile.getCanonicalPath() );
            lr = new LineNumberReader(new BufferedReader(r));
            while ((line = lr.readLine()) != null) {
                if (lr.getLineNumber() % 1000 == 0)
                    System.out.print('.');
                line = line.trim();
                if (!processNounLine(line)) {
                    System.out.println();
                    System.out.println( "Error in WordNet.readNouns(): No match in "
                            + nounFile.getCanonicalPath() + " for line " + line );
                }
            }
            System.out.println("x");
            System.out.println("  " + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process " + nounFile.getCanonicalPath() + 
                    " with " + lr.getLineNumber() + " lines");
            // System.out.println("INFO in WordNet.readNouns(): Reading WordNet noun exceptions");
            nounFile = getWnFile("noun_exceptions",null);
            if (nounFile == null) {
                System.out.println("ERROR in WordNet.readNouns(): "
                        + "The noun mapping exceptions file does not exist in " + baseDir);
                return;
            }

            t1 = System.currentTimeMillis();
            r = new FileReader( nounFile );
            lr = new LineNumberReader(r);
            while ((line = lr.readLine()) != null) {
                // 8: p = Pattern.compile("(\\S+)\\s+(\\S+)");
                m = regexPatterns[8].matcher(line);
                if (m.matches()) {
                    exceptionNounHash.put(m.group(1),m.group(2));      // 1-plural, 2-singular
                    exceptionNounPluralHash.put(m.group(2),m.group(1));
                }
                else {
                    // 9: p = Pattern.compile("(\\S+)\\s+(\\S+)\\s+(\\S+)");
                    m = regexPatterns[9].matcher(line);
                    if (m.matches()) {
                        exceptionNounHash.put(m.group(1),m.group(2));      // 1-plural, 2-singular 3-alternate singular
                        exceptionNounPluralHash.put(m.group(2),m.group(1));
                        exceptionNounPluralHash.put(m.group(3),m.group(1));
                    }
                    else
                        if (line != null && line.length() > 0 && line.charAt(0) != ';') {
                            System.out.println("Error in WordNet.readNouns(): No match in "
                                    + nounFile.getCanonicalPath() + " for line " + line );
                        }
                }
            }
            System.out.println("  " + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process " + nounFile.getCanonicalPath() );
        }
        catch (Exception ex) {
            System.out.println("Error in WordNet.readNouns(): " + ex.getMessage());
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
        return;
    }

    /** ***************************************************************
     */
    protected void setMaxNounSynsetID(String synset) {

        //System.out.println("WordNet.setMaxNounSynsetID(): " + synset);
        if (WordNetUtilities.isValidSynset8(synset))
            maxNounSynsetID = synset;
    }

    /** ***************************************************************
     */
    protected void setMaxVerbSynsetID(String synset) {

        if (WordNetUtilities.isValidSynset8(synset))
            maxVerbSynsetID = synset;
    }

    /** ***************************************************************
     */
    protected boolean processNounLine(String line) {

        // 6: p = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$");
        m = regexPatterns[6].matcher(line);
        boolean anyAreNull = false;
        if (m.matches()) {
            for (int i = 1 ; i < 5 ; i++) {
                anyAreNull = (m.group(i) == null);
                if (anyAreNull) {
                    break;
                }
            }
            if (!anyAreNull ) {
                addSUMOMapping(m.group(4),"1" + m.group(1));
                setMaxNounSynsetID(m.group(1));
                nounDocumentationHash.put(m.group(1),m.group(3)); // 1-synset, 2-pointers, 3-docu, 4-SUMO term
                processPointers("1" + m.group(1),m.group(2));
            }
        }
        else {
            // 7: p = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$");  // no SUMO mapping
            m = regexPatterns[7].matcher(line);
            if (m.matches()) {
                nounDocumentationHash.put(m.group(1),m.group(3));
                setMaxNounSynsetID(m.group(1));
                processPointers("1" + m.group(1),m.group(2));
            }
            else {
                //System.out.println("line: " + line);
                if (line.length() > 0 && line.charAt(0) != ';') {
                    return false;
                }
            }
        }
        return true;
    }

    /** ***************************************************************
     *  Create the hashtables verbSynsetHash (by calling processPointers which calls
     *  addSynsetsToWords), verbDocumentationHash,
     *  verbSUMOhash and exceptionVerbHash that contain the WordNet
     *  verb synsets, word definitions, mappings to SUMO, and plural
     *  exception forms, respectively.
     *  Throws an IOException if the files are not found.
     */
    private void readVerbs() throws java.io.IOException {

        System.out.println("INFO in WordNet.readVerbs(): Reading WordNet verb files");
        LineNumberReader lr = null;
        try {
            String line;
            File verbFile = getWnFile("verb_mappings",null);
            if (verbFile == null) {
                System.out.println("Error in WordNet.readVerbs(): The verb mappings file does not exist in " + baseDir);
                return;
            }
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(verbFile);
            lr = new LineNumberReader(new BufferedReader(r));
            while ((line = lr.readLine()) != null) {
                if (lr.getLineNumber() % 1000 == 0)
                    System.out.print('.');
                line = line.trim();
                // 10: p = Pattern.compile("^([0-9]{8})([^\\|]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$");
                m = regexPatterns[10].matcher(line);
                if (m.matches()) {
                    verbDocumentationHash.put(m.group(1),m.group(3));
                    setMaxVerbSynsetID(m.group(1));
                    addSUMOMapping(m.group(4),"2" + m.group(1));
                    processPointers("2" + m.group(1),m.group(2));
                }
                else {
                    // 11: p = Pattern.compile("^([0-9]{8})([^\\|]+)\\|\\s([\\S\\s]+)$");   // no SUMO mapping
                    m = regexPatterns[11].matcher(line);
                    if (m.matches()) {
                        verbDocumentationHash.put(m.group(1),m.group(3));
                        setMaxVerbSynsetID(m.group(1));
                        processPointers("2" + m.group(1),m.group(2));
                    }
                    else {
                        //System.out.println("line: " + line);
                        if (line != null && line.length() > 0 && line.charAt(0) != ';') {
                            System.out.println();
                            System.out.println("Error in WordNet.readVerbs(): No match in "
                                    + verbFile.getCanonicalPath() + " for line " + line );
                        }
                    }
                }
            }
            System.out.println("x");
            System.out.println("  " + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process " + verbFile.getCanonicalPath()  + 
                    " with " + lr.getLineNumber() + " lines");
            // System.out.println("INFO in WordNet.readVerbs(): Reading WordNet verb exceptions");
            verbFile = getWnFile("verb_exceptions",null);
            if (verbFile == null) {
                System.out.println("Error in WordNet.readVerbs(): The verb mapping exceptions file does not exist in " + baseDir);
                return;
            }
            t1 = System.currentTimeMillis();
            r = new FileReader( verbFile );
            lr = new LineNumberReader(r);
            while ((line = lr.readLine()) != null) {
                // 12: p = Pattern.compile("(\\S+)\\s+(\\S+).*");  
                m = regexPatterns[12].matcher(line);  // TODO: Note we ignore more then one base form for a given tense
                if (m.matches()) {
                    exceptionVerbHash.put(m.group(1),m.group(2));          // 1-past/progressive, 2-root
                    if (m.group(1).endsWith("ing"))
                        exceptVerbProgHash.put(m.group(2),m.group(1));
                    else if ((m.group(1).endsWith("en") && !m.group(1).equals("been")) || m.group(1).endsWith("wn")
                            || m.group(1).endsWith("ne"))
                        exceptionVerbPastProgHash.put(m.group(2), m.group(1));
                    else
                        exceptionVerbPastHash.put(m.group(2), m.group(1));

                }
                else
                    if (line != null && line.length() > 0 && line.charAt(0) != ';')
                        System.out.println( "Error in WordNet.readVerbs(): No match in "
                                + verbFile.getCanonicalPath() + " for line " + line );
            }
            System.out.println("  " + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process " + verbFile.getCanonicalPath() );
        }
        catch (Exception ex) {
            System.out.println("Error in WordNet.readVerbs(): " + ex.getMessage());
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
        return;
    }

    /** ***************************************************************
     *  Create the hashtables adjectiveSynsetHash, adjectiveDocumentationHash,
     *  and adjectiveSUMOhash that contain the WordNet
     *  adjective synsets, word definitions, and mappings to SUMO, respectively.
     *  Throws an IOException if the files are not found.
     */
    private void readAdjectives() throws java.io.IOException {

        System.out.println("INFO in WordNet.readAdjectives(): Reading WordNet adjective files");
        LineNumberReader lr = null;
        try {
            String line;
            File adjFile = getWnFile("adj_mappings",null);
            if (adjFile == null) {
                System.out.println("Error in WordNet.readAdjectives(): The adjective mappings file does not exist in " + baseDir);
                return;
            }
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(adjFile);
            lr = new LineNumberReader(new BufferedReader(r));
            while ((line = lr.readLine()) != null) {
                if (lr.getLineNumber() % 1000 == 0)
                    System.out.print('.');
                line = line.trim();
                // 13: p = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$");
                m = regexPatterns[13].matcher(line);
                if (m.matches()) {
                    adjectiveDocumentationHash.put(m.group(1),m.group(3));
                    addSUMOMapping(m.group(4), "3" + m.group(1));
                    processPointers("3" + m.group(1), m.group(2));
                }
                else {
                    // 14: p = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$");     // no SUMO mapping
                    m = regexPatterns[14].matcher(line);
                    if (m.matches()) {
                        adjectiveDocumentationHash.put(m.group(1),m.group(3));
                        processPointers("3" + m.group(1),m.group(2));
                    }
                    else {
                        //System.out.println("line: " + line);
                        if (line != null && line.length() > 0 && line.charAt(0) != ';') {
                            System.out.println();
                            System.out.println( "Error in WordNet.readAdjectives(): No match in "
                                    + adjFile.getCanonicalPath() + " for line " + line );
                        }
                    }
                }
            }
            System.out.println("x");
            System.out.println("  " + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process " + adjFile.getCanonicalPath() + 
                    " with " + lr.getLineNumber() + " lines");
        }
        catch (Exception ex) {
            System.out.println("Error in WordNet.readAdjectives(): " + ex.getMessage());
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
        return;
    }

    /** ***************************************************************
     *  Create the hashtables adverbSynsetHash, adverbDocumentationHash,
     *  and adverbSUMOhash that contain the WordNet
     *  adverb synsets, word definitions, and mappings to SUMO, respectively.
     *  Throws an IOException if the files are not found.
     */
    private void readAdverbs() throws java.io.IOException {

        System.out.println("INFO in WordNet.readAdverbs(): Reading WordNet adverb files");
        LineNumberReader lr = null;
        try {
            String line;
            File advFile = getWnFile("adv_mappings",null);
            if (advFile == null) {
                System.out.println("Error in WordNet.readAdverbs(): The adverb mappings file does not exist in " + baseDir);
                return;
            }
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(advFile);
            lr = new LineNumberReader(new BufferedReader(r));
            while ((line = lr.readLine()) != null) {
                if (lr.getLineNumber() % 1000 == 0)
                    System.out.print('.');
                line = line.trim();
                // 15: p = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$");
                m = regexPatterns[15].matcher(line);
                if (m.matches()) {
                    adverbDocumentationHash.put(m.group(1),m.group(3));
                    addSUMOMapping(m.group(4),"4" + m.group(1));
                    processPointers("4" + m.group(1),m.group(2));
                }
                else {
                    // 16: p = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$");   // no SUMO mapping
                    m = regexPatterns[16].matcher(line);
                    if (m.matches()) {
                        adverbDocumentationHash.put(m.group(1),m.group(3));
                        processPointers("4" + m.group(1),m.group(2));
                    }
                    else {
                        //System.out.println("line: " + line);
                        if (line != null && line.length() > 0 && line.charAt(0) != ';') {
                            System.out.println();
                            System.out.println("Error in WordNet.readAdverbs(): No match in "
                                    + advFile.getCanonicalPath() + " for line " + line);
                        }
                    }
                }
            }
            System.out.println("x");
            System.out.println("  " + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process " + advFile.getCanonicalPath() + 
                    " with " + lr.getLineNumber() + " lines");
        }
        catch (Exception ex) {
            System.out.println("Error in WordNet.readAdverbs(): " + ex.getMessage());
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
     * Merge a new set of word co-occurrence statistics into the existing
     * set.
     */
    public void mergeWordCoFrequencies(HashMap<String,HashMap<String,Integer>> senses) {

        System.out.println("INFO in WordNet.mergeWordCoFrequencies(): before size: " +
                wordCoFrequencies.keySet().size());
        for (String s : senses.keySet()) {
            if (wordCoFrequencies.containsKey(s)) {
                HashMap<String,Integer> newValues = senses.get(s);
                HashMap<String,Integer> oldValues = wordCoFrequencies.get(s);
                for (String w : newValues.keySet()) {
                    if (oldValues.containsKey(w)) {
                        oldValues.put(w,newValues.get(w) + oldValues.get(w));
                    }
                    else {
                        oldValues.put(w,newValues.get(w));
                    }
                }
            }
            else
                wordCoFrequencies.put(s,senses.get(s));
        }
        System.out.println("INFO in WordNet.mergeWordCoFrequencies(): after size: " +
                wordCoFrequencies.keySet().size());
    }

    /** ***************************************************************
     * Write a HashMap of HashMaps where the key is a word sense of the
     * form word_POS_num signifying the word, part of speech and number
     * of the sense in WordNet.  The value is a HashMap of words and the
     * number of times that word cooccurs in sentences with the word sense
     * given in the key.
     */
    public static void writeWordCoFrequencies(String fname, HashMap<String,HashMap<String,Integer>> senses) {

        FileWriter fw = null;
        PrintWriter pw = null;
        String dir = WordNet.baseDir;

        try {
            fw = new FileWriter(dir + File.separator + fname);
            pw = new PrintWriter(fw);
            System.out.println("INFO in WordNet.writeWordFrequencies(): Writing WordNet word frequencies");
            for (String s : senses.keySet()) {
                HashMap<String,Integer> values = senses.get(s);
                if (values.size() > 0) {
                    pw.print("Word: " + s + " Values: ");
                    for (String v : values.keySet())
                        pw.print(v + "_" + values.get(v) + " ");
                    pw.println();
                }
            }
        }
        catch (Exception e) {
            System.out.println("Error in WordNet.writeProlog(): " + e.getMessage());
            System.out.println("Error writing file " + dir + File.separator + fname + "\n" + e.getMessage());
            e.printStackTrace();
        }
        finally {
            try {
                if (pw != null)
                    pw.close();
                if (fw != null)
                    fw.close();
            }
            catch (Exception ex) {
            }
        }
    }

    /** ***************************************************************
     * Return a HashMap of HashMaps where the key is a word sense of the
     * form word_POS_num signifying the word, part of speech and number
     * of the sense in WordNet.  The value is a HashMap of words and the
     * number of times that word cooccurs in sentences with the word sense
     * given in the key.
     */
    public void readWordCoFrequencies() {

        System.out.println("INFO in WordNet.readWordFrequencies(): Reading WordNet word frequencies");
        wordCoFrequencies = new HashMap<String,HashMap<String,Integer>>();
        LineNumberReader lr = null;
        int counter = 0;
        File wfFile = null;
        String canonicalPath = "";
        try {
            wfFile = getWnFile("word_frequencies",null);
            if (wfFile == null) {
                System.out.println("Error in WordNet.readWordFrequencies(): The word frequencies file does not exist in " + baseDir);
                return;
            }
            canonicalPath = wfFile.getCanonicalPath();
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(wfFile);
            lr = new LineNumberReader(r);
            String line = null;
            while ((line = lr.readLine()) != null) {
                line = line.trim();
                // 17: Pattern p = Pattern.compile("^Word: ([^ ]+) Values: (.*)");
                Matcher m = regexPatterns[17].matcher(line);
                if (m.matches()) {
                    String key = m.group(1);
                    String values = m.group(2);
                    String[] words = values.split(" ");
                    HashMap<String,Integer> frequencies = new HashMap<String,Integer>();
                    for (int i = 0; i < words.length-3; i++) {
                        if (words[i].equals("SUMOterm:")) {
                            i = words.length;
                        }
                        else {
                            if (words[i].indexOf("_") == -1) {
                                //System.out.println("INFO in WordNet.readWordFrequencies().  word: " + words[i]);
                                //System.out.println("INFO in WordNet.readWordFrequencies().  line: " + line);
                            }
                            else {
                                String word = words[i].substring(0,words[i].indexOf("_"));
                                String freq = words[i].substring(words[i].lastIndexOf("_") + 1, words[i].length());
                                frequencies.put(word.intern(),Integer.decode(freq));
                            }
                        }
                    }
                    wordCoFrequencies.put(key.intern(),frequencies);
                    counter++;
                    if (counter == 1000) {
                        System.out.print(".");
                        counter = 0;
                    }
                }
            }
            System.out.println("x");
            System.out.println("  " + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process " + canonicalPath);
        }
        catch (Exception i) {
            System.out.println();
            System.out.println("Error in WordNet.readWordFrequencies() reading file "
                    + canonicalPath + ": " + i.getMessage());
            i.printStackTrace();
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
        return;
    }

    /** ***************************************************************
     */
    public void readStopWords() {

        System.out.println("INFO in WordNet.readStopWords(): Reading stop words");
        LineNumberReader lr = null;
        File swFile = null;
        String canonicalPath = "";
        try {
            swFile = getWnFile("stopwords",null);
            if (swFile == null) {
                System.out.println("Error in WordNet.readStopWords(): The stopwords file does not exist in " + baseDir);
                return;
            }
            canonicalPath = swFile.getCanonicalPath();
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(swFile);
            lr = new LineNumberReader(r);
            String line;
            while ((line = lr.readLine()) != null)
                stopwords.add(line.intern());
            System.out.println("  " + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process " + canonicalPath );
        }
        catch (Exception i) {
            System.out.println("Error in WordNet.readStopWords() reading file "
                    + canonicalPath + ": " + i.getMessage());
            i.printStackTrace();
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
        return;
    }

    /** ***************************************************************
     * Note that WordNet forces all these words to lowercase in the index.xxx files
     */
    public void readSenseIndex(String filename) {

        System.out.println("INFO in WordNet.readSenseIndex(): Reading WordNet sense index");
        LineNumberReader lr = null;
        int counter = 0;
        int totalcount = 0;
        File siFile = null;
        String canonicalPath = "";
        try {
            siFile = getWnFile("sense_indexes",filename);
            if (siFile == null) {
                System.out.println("Error in WordNet.readSenseIndex(): The sense indexes file does not exist in " + baseDir);
                return;
            }
            canonicalPath = siFile.getCanonicalPath();
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader( siFile );
            lr = new LineNumberReader(r);
            //System.out.println("INFO in WordNet.readSenseIndex().  Opened file.");
            Matcher m = null;
            String line;
            while ((line = lr.readLine()) != null) {
                // 18: Pattern p = Pattern.compile("([^%]+)%([^:]*):([^:]*):([^:]*):([^:]*):([^ ]*) ([^ ]+) ([^ ]+) .*");
                m = regexPatterns[18].matcher(line);
                if (m.matches()) {
                    String word = m.group(1);
                    String pos = m.group(2);  // WN's ss_type
                    String lexFilenum = m.group(3);
                    String lexID = m.group(4);
                    String headword = m.group(5);
                    String headID = m.group(6);
                    String synset = m.group(7);
                    String sensenum = m.group(8);
                    String posString = WordNetUtilities.posNumberToLetters(pos); // alpha POS - NN,VB etc
                    String key = word + "_" + posString + "_" + sensenum;
                    String sensekey = word + "%" + pos + ":" + lexFilenum + ":" + lexID;
                    senseKeys.put(sensekey,synset);
                    //System.out.println("WordNet.readSenseIndex(): " + sensekey + " " + synset);
                    ArrayList<String> al = wordsToSenseKeys.get(word);
                    if (al == null) {
                        al = new ArrayList<String>();
                        wordsToSenseKeys.put(word, al);
                    }
                    al.add(key);
                    senseIndex.put(key, synset);
                    reverseSenseIndex.put(pos + synset, key);
                    counter++;
                    if (counter == 1000) {
                        //System.out.println("INFO in WordNet.readSenseIndex().  Read word sense: " + key);
                        //System.out.println(word + " " + pos  + " " + synset  + " "  + sensenum);
                        System.out.print('.');
                        totalcount = totalcount + counter;
                        counter = 0;
                    }
                }
            }
            System.out.println("x");
            System.out.println("  " + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process " + canonicalPath + " with " + totalcount + " senses.");
        }
        catch (Exception i) {
            System.out.println();
            System.out.println("Error in WordNet.readSenseIndex() reading file "
                    + canonicalPath + ": " + i.getMessage());
            i.printStackTrace();
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
        return;
    }

    /** ***************************************************************
     * Read word sense frequencies into a HashMap of PriorityQueues 
     * containing AVPairs where the value is a word and the attribute 
     * (on which PriorityQueue is sorted) is an 8 digit String 
     * representation of an integer count.
     */
    public void readSenseCount() {

        System.out.println("INFO in WordNet.readSenseCount(): Reading WordNet sense counts");
        LineNumberReader lr = null;
        int counter = 0;
        int missingSenses = 0;
        int totalcount = 0;
        File siFile = null;
        String canonicalPath = "";
        try {
            siFile = getWnFile("cntlist",null);
            if (siFile == null) {
                System.out.println("Error in WordNet.readSenseCount(): The sense count file does not exist in " + 
                        baseDir + File.separator + "cntlist");
                return;
            }
            canonicalPath = siFile.getCanonicalPath();
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader( siFile );
            lr = new LineNumberReader(r);
            //System.out.println("INFO in WordNet.readSenseIndex().  Opened file.");
            Matcher m = null;
            String line;
            while ((line = lr.readLine()) != null) {
                //System.out.println("Info in WordNet.readSenseCount(): line " + line);
                Pattern p = Pattern.compile("([^ ]+) ([^%]+)%([^:]*):[^:]*:[^:]*:[^:]*:[^ ]* ([^ ]+)");
                m = p.matcher(line);
                if (m.matches()) {
                    String count = m.group(1);
                    String word = m.group(2);
                    caseMap.put(word.toUpperCase(),word);
                    String POS = m.group(3);
                    String posString = WordNetUtilities.posNumberToLetters(POS);
                    String sensenum = m.group(4);
                    String key = word + "_" + posString + "_" + sensenum;  // word_POS_sensenum
                    AVPair avp = new AVPair();
                    avp.attribute = StringUtil.fillString(count, ' ', 8, true);
                    String synset8 = senseIndex.get(key);
                    if (synset8 == null && missingSenses < 101) {
                        //System.out.println("Info in WordNet.readSenseCount(): no synset for key: " + key);
                        if (missingSenses == 100)
                            System.out.println("Info in WordNet.readSenseCount(): > 100 missing senses, suppressing messages ");
                        missingSenses++;
                    }
                    else {
                        String synset = WordNetUtilities.getSenseFromKey(key);
                        avp.value = synset;
                        addToWordFreq(word, avp);
                        int freq = Integer.parseInt(count);
                        senseFrequencies.put(synset, freq);
                    }
                }
                counter++;
                if (counter == 1000) {
                    //System.out.println("INFO in WordNet.readSenseIndex().  Read word sense: " + key);
                    //System.out.println(word + " " + pos  + " " + synset  + " "  + sensenum);
                    System.out.print('.');
                    totalcount = totalcount + counter;
                    counter = 0;
                }
            }
            System.out.println("x");
            System.out.println("  " + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process " + canonicalPath + " with " + totalcount + " senses.");
        }
        catch (Exception i) {
            System.out.println();
            System.out.println( "Error in WordNet.readSenseCount() reading file "
                    + canonicalPath + ": " + i.getMessage());
            i.printStackTrace();
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
        return;
    }

    /** ***************************************************************
     * Add an entry to the wordFrequencies list, checking whether it
     * has a valid count and synset pair.
     */
    public void addToWordFreq(String word, AVPair avp) {

        if (avp == null) {
            System.out.println("Error in WordNet.addToWordFreq(): null AVPair for word " + word);
            return;
        }
        if (!StringUtil.isInteger(avp.attribute)) {
            System.out.println("Error in WordNet.addToWordFreq(): bad sense count: " + avp +
            " for word " + word);
            return;
        }
        if (!WordNetUtilities.isValidSynset9(avp.value)) {
            //System.out.println("Error in WordNet.addToWordFreq(): bad synset: " + avp +
            //        " for word " + word);
            return;
        }

        TreeSet<AVPair> pq = new TreeSet<AVPair>();
        if (wordFrequencies.containsKey(word))
            pq = wordFrequencies.get(word);
        pq.add(avp);
        wordFrequencies.put(word,pq);
    }

    /** ***************************************************************
     *  A routine which looks up a given list of words in the hashtables
     *  to find the relevant word definitions and SUMO mappings.
     *  @param input is the target sentence to be parsed. See WordSenseBody.jsp for usage.
     *  @param context is the larger context of the sentence. Can mean more accurate results. 
     *  @param params is the set of html parameters
     *  @returns a String that is the sentence taken apart and displayed in HTML
     */
    public String sumoSentenceDisplay(String input, String context, String params) {

        if (StringUtil.emptyString(input))
            return "Empty input";
        try {
            ArrayList<String> sentenceList = splitToArrayListSentence(input);
            StringBuffer result = new StringBuffer();

            for (String sentence : sentenceList) {
                ArrayList<String> synsetList = WSD.collectWordSenses(sentence); //returns an ArrayList of synsets
                System.out.println("INFO in WordNet.sumoSentenceDisplay(): " + synsetList);
                result.append("<b>Target Sentence: </b>" + sentence + "<br>\n");
                //if (wordSynsetList == null || wordSynsetList.size() < 2)
                //    continue;
                //String wordColl = wordSynsetList.get(0);
                //String synsetColl = wordSynsetList.get(1);
                //ArrayList<String> synsetList = splitToArrayList(synsetColl); //removes all punctuation and whitespace
                //ArrayList<String> wordList = splitToArrayList(wordColl);
            
                result.append(sumoSentimentDisplay(sentence)); //attaches sentiment display already in HTML format
                HashMap<String,Integer> conceptMap = (HashMap<String,Integer>) DB.computeConceptSentiment(sentence); //returns Hashmap of key=SUMO concept, value=sentiment score
                System.out.println("INFO in WordNet.sumoSentenceDisplay(): map " + conceptMap);
                
                if (synsetList == null) {
                    result.append("<b> No words could be mapped for Sense Analysis. </b>\n");
                    return result.toString();
                }
            
                ArrayList<String> SUMOtermList = new ArrayList<String>();
                int listLength = synsetList.size();
                for (int i = 0; i < listLength; i++) {
                    String s = synsetList.get(i);
                    String trm = getSUMOMapping(s); //returns a SUMO term in the form "&%term=" that matches the synset
                    SUMOtermList.add(trm);
                }
            
                String synset = new String();
                String word = new String();
                String SUMOterm = new String();
                String documentation = new String();

                for (int j = 0; j < listLength; j++) {        
                    synset = synsetList.get(j);
                    ArrayList<String> words = synsetsToWords.get(synset);
                    if (words != null && words.size() > 0)
                        word = words.get(0);
                    else
                        word = "";
                    SUMOterm = SUMOtermList.get(j); 
                    if (nounDocumentationHash.containsKey(synset.substring(1)) && synset.substring(0,1).equals("1")) {
                        documentation = (String) nounDocumentationHash.get(synset.substring(1));
                        result.append("<a href=\"WordNet.jsp?synset=" + synset + "&" + params + "\">" + synset + "</a> ");
                        result.append(" " + "<a href=\"WordNet.jsp?simple=no" + "&" + params + "&word=" + word + "&POS=1" + 
                                "\">" + word + "</a>" + "-  " + documentation + ".\n");
                    }
                    else if (verbDocumentationHash.containsKey(synset.substring(1)) && synset.substring(0,1).equals("2")) {
                        documentation = (String) verbDocumentationHash.get(synset.substring(1));
                        result.append("<a href=\"WordNet.jsp?synset=" + synset + "&" + params + "\">" + synset + "</a> ");
                        result.append(" " + "<a href=\"WordNet.jsp?simple=no" + "&" + params + "&word=" + word + "&POS=2" + 
                                "\">" + word + "</a>" + "-  " + documentation + ".\n");
                    }
                    else if (adjectiveDocumentationHash.containsKey(synset.substring(1)) && synset.substring(0,1).equals("3")) {
                        documentation = (String) adjectiveDocumentationHash.get(synset.substring(1));
                        result.append("<a href=\"WordNet.jsp?synset=" + synset + "&" + params + "\">" + synset + "</a> ");
                        result.append(" " + "<a href=\"WordNet.jsp?simple=no" + "&" + params + "&word=" + word + "&POS=3" + 
                                "\">" + word + "</a>" + "-  " + documentation + ".\n");
                    }
                    else if (adverbDocumentationHash.containsKey(synset.substring(1)) && synset.substring(0,1).equals("4")) {
                        documentation = (String) adverbDocumentationHash.get(synset.substring(1));
                        result.append("<a href=\"WordNet.jsp?synset=" + synset + "&" + params + "\">" + synset + "</a> ");
                        result.append(" " + "<a href=\"WordNet.jsp?simple=no" + "&" + params + "&word=" + word + "&POS=4" + 
                                "\">1" + word + "</a>" + "-  " + documentation + ".\n");
                    }
                                
                    if (SUMOterm == null) 
                        result.append("<P>" + word + " not yet mapped to SUMO<P>"); 
                    else {
                        result.append(HTMLformatter.termMappingsList(SUMOterm,"<a href=\"Browse.jsp?" + params + "&term="));
                        if (conceptMap.containsKey(SUMOterm.substring(2,SUMOterm.length()-1))) { //SUMOterm is in form &%term=, so must remove &% and = to match to a key in conceptMap
                            Integer conceptScore = (Integer) conceptMap.get(SUMOterm.substring(2,SUMOterm.length()-1));
                            result.append("<p><ul><li>\tSentiment Score in Context:  " + conceptScore.toString() + "</li></ul>");
                        }
                    }
                }
                result.append("<br><br>");
            }      
            return result.toString();
        }
        catch (NullPointerException ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            return "<b>Error</b>";
        }        
    }
    
    /** ***************************************************************
     *  A routine that uses computeSentiment in DB.java to display a sentiment score for a single sentence
     *  as well as the individual scores of scored descriptors.
     *  @param sentence is the target sentence to be scored. See WordSenseBody.jsp for usage.
     *  @returns a String that is the sentence scored and displayed in HTML
     */
    public String sumoSentimentDisplay(String sentence) {
        
        try {
            StringBuffer result = new StringBuffer();
            Integer overallSentiment = DB.computeSentiment(sentence); //returns a single Integer of total sentiment
            
            String newSentence = WordNet.wn.removeStopWords(sentence.trim());
            newSentence = StringUtil.removePunctuation(newSentence);
            ArrayList<String> words = splitToArrayList(newSentence);
            HashMap<String,Integer> scoreMap = new HashMap<String,Integer>();
            for (String w : words) { //must gather individual scores that went into created the overall sentiment
                Integer score = DB.computeSentimentForWord(w);
                if (score != 0) 
                    scoreMap.put(w,score);
            }
            result.append("<b>Overall Sentence Sentiment Score: </b>" + overallSentiment.toString() + "<br>");
            if (scoreMap.size()!=0)
                result.append("<b>Individual (un-disambiguated) Word Sentiment Scores: </b>" + scoreMap.toString() + "<br><br>");
            else
                result.append("<b>All Words Are Neutral</b><br><br>");
            return result.toString();
        }
        catch (NullPointerException E) {
            return "<b> NullPointerException </b>";
        }
    }
        
    /** ***************************************************************
     *  A routine which takes a full pathname as input and returns a sentence by sentence display of sense
     *  and sentiment analysis
     *  @param pathname
     *  @param counter is used to keep track of which sentence is being displayed
     *  @param params is the set of html parameters
     *  @returns a String that is the file split into sentences, which are taken apart one by one
     *   and displayed in HTML
     */
    public String sumoFileDisplay(String pathname, String counter, String params) {
        
        try {
            File file = new File(pathname);
            if (file.exists() && file.length() == 0)
                return null;
            FileInputStream fileStream = new FileInputStream(pathname);
            DataInputStream dataStream = new DataInputStream(fileStream);
            BufferedReader reader = new BufferedReader(new InputStreamReader(dataStream));
            String line;
            StringBuffer textBuffer = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                textBuffer.append(line + " ");
            }
            dataStream.close();
            String text = textBuffer.toString();
            Integer overallSentiment = DB.computeSentiment(text); //returns an Integer that is the overall sentiment of the entire file
            ArrayList<String> sentenceList = (ArrayList<String>) splitToArrayListSentence(text); //splits by periods. Runs into trouble with titles such as Mr. Test. 
            StringBuffer result = new StringBuffer();
            result.append("<b>Overall File Sentiment Score: </b>" + overallSentiment.toString() + "<br><br>");
            if (Integer.parseInt(counter) > 0) {
                String prevCount = Integer.toString(Integer.parseInt(counter) - 1);
                result.append("<i><a href=\"WordSense.jsp?" + params + "&sentence=" + pathname + "&sentCounter=" + prevCount + "\">" + "previous sentence (" + counter + " more)</a></i><br>");            
            }
            String targetSentence = sentenceList.get(Integer.parseInt(counter));
            result.append(sumoSentenceDisplay(targetSentence, text, params)); //sumoSentenceDisplay does all the heavy lifting in sentence parsing and sense gathering
            if (sentenceList.size() > (Integer.parseInt(counter)+1)) {
                String nextCount = Integer.toString(Integer.parseInt(counter) + 1);
                String remainingCount = Integer.toString(sentenceList.size() - Integer.parseInt(counter) - 1);
                result.append("<i><a href=\"WordSense.jsp?" + params + "&sentence=" + pathname + "&sentCounter=" + nextCount + "\">" + "next sentence (" + remainingCount + " more)</a></i>");
            }
            
            return result.toString();
        }
        catch (FileNotFoundException e) {
            return "<b> No such File " + pathname + "</b>";
        }
        catch (IOException i) {
            return "<b> IO error </b>";
        }
    }
    
    /** ***************************************************************
     * @return true if the input String is a file pathname. Determined by whether
     * the string contains a forward or backward slash. This is only used in WordSense.jsp and will fail if 
     * a sentence that is not a file contains a forward or back slash.
     */
    public boolean isFile(String s) {
        
        return (s.contains("\\")||s.contains("/") ? true : false);
    }

    /** ***************************************************************
     * @return true if the first POS-prefixed synset is a hyponym of the
     * second POS-prefixed synset.
     * This is a recursive method.
     */
    public boolean isHyponymRecurse(String synset, String hypo, ArrayList<String> visited) {

        //System.out.println("INFO in WordNet.isHyponym(): synset, hypo: " + synset + "," + hypo);
        //System.out.println("INFO in WordNet.isHyponym(): synset, hypo: " + synsetsToWords.get(synset) + "," + synsetsToWords.get(hypo));

        // hypernym 100021265 - food, nutrient
        //    public Hashtable<String,ArrayList<AVPair>> relations = new Hashtable();
        // ~ is hyponym
        if (StringUtil.emptyString(synset) || StringUtil.emptyString(hypo))
            return false;
        if (visited.contains(synset))  // catch cycles
            return false;
        ArrayList<AVPair> links = relations.get(synset);
        if (links != null) {
            for (int i = 0; i < links.size(); i++) {
                AVPair link = links.get(i);
                if (link == null)
                    System.out.println("Error in WordNet.isHyponym(): null link");
                else if (link.attribute.equals("hypernym")) {
                    if (link.value.equals(hypo))
                        return true;
                    else {
                        ArrayList<String> newVisited = new ArrayList<String>();
                        newVisited.addAll(visited);
                        newVisited.add(synset);
                        if (isHyponymRecurse(link.value,hypo,newVisited))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    /** ***************************************************************
     * @return true if the first POS-prefixed synset is a hyponym of the
     * second POS-prefixed synset.
     * This is a recursive method.
     */
    public boolean isHyponym(String synset, String hypo) {

        ArrayList<String> visited = new ArrayList<String>();
        return isHyponymRecurse(synset,hypo,visited);
    }

    /** ***************************************************************
     * Remove stop words from a sentence.
     */
    public String removeStopWords(String sentence) {

        if (StringUtil.emptyString(sentence))
            return "";
        String result = "";
        ArrayList<String> al = splitToArrayList(sentence);
        if (al == null)
            return "";
        for (int i = 0; i < al.size(); i++) {
            String word = (String) al.get(i);
            if (!isStopWord(word)) {
                if (result == "")
                    result = word;
                else
                    result = result + " " + word;
            }
        }
        return result;
    }

    /** ***************************************************************
     * Remove stop words from a sentence.
     */
    public ArrayList<String> removeStopWords(ArrayList<String> sentence) {

        ArrayList<String> result = new ArrayList<String>();
        if (sentence == null)
            return result;
        for (int i = 0; i < sentence.size(); i++) {
            String word = (String) sentence.get(i);
            if (!isStopWord(word)) {
                result.add(word);
            }
        }
        return result;
    }

    /** ***************************************************************
     * Check whether the word is a stop word
     */
    public boolean isStopWord(String word) {

        if (debug) System.out.println("WordNet.isStopWord(): word: " + word);
        if (stopwords.size() < 1) {
            System.out.println("Error in WordNet.isStopWord(): stopwords list not loaded");
            return false;
        }
        if (StringUtil.emptyString(word)) {
            System.out.println("Error in WordNet.isStopWord(): empty input");
            return false;
        }
        if (stopwords.contains(word.trim().toLowerCase())) {
            if (debug) System.out.println("isStopWord(): contains: "  + stopwords.contains(word));
            return true;
        }
        return false;
    }

    /** ***************************************************************
     * Collect all the synsets that represent the best guess at
     * meanings for all the words in a sentence.  Keep track of how many
     * times each sense appears.
     */
    public HashMap<String,Integer> collectCountedWordSenses(String sentence) {

        if (StringUtil.emptyString(sentence))
            System.out.println("Error in collectCountedWordSenses(): empty string");
        HashMap<String,Integer> result = new HashMap<String,Integer>();
        //System.out.println("INFO in collectSUMOWordSenses(): unprocessed sentence: " + sentence);
        String newSentence = StringUtil.removeHTML(sentence);
        newSentence = StringUtil.removePunctuation(sentence);
        newSentence = removeStopWords(newSentence);
        //System.out.println("INFO in collectSUMOWordSenses(): processed sentence: " + newSentence);
        ArrayList<String> al = splitToArrayList(newSentence);
        if (al == null) 
            return result;
        for (int i = 0; i < al.size(); i++) {
            String word = al.get(i);
            String synset = WSD.findWordSenseInContext(word,al);
            if (synset != null && synset != "") {
                if (result.get(synset) == null)
                    result.put(synset,Integer.valueOf(1));
                else
                    result.put(synset,Integer.valueOf(result.get(synset).intValue() + 1));
            }
            else {
                synset = WSD.getBestDefaultSense(word);
                if (!StringUtil.emptyString(synset)) {
                    if (result.get(synset) == null)
                        result.put(synset,Integer.valueOf(1));
                    else
                        result.put(synset,Integer.valueOf(result.get(synset).intValue() + 1));
                }
            }
            /**if (SUMO == null || SUMO == "")
               System.out.println("INFO in findSUMOWordSense(): word not found: " + word);
               else
               System.out.println("INFO in findSUMOWordSense(): word, term: " + word + ", " + SUMO);*/
        }
        return result;
    }

    /** ***************************************************************
     */
    public static boolean serializedExists() {

        File serfile = new File(baseDir + File.separator + "wn.ser");
        return serfile.exists();
    }

    /** ***************************************************************
     *  Check whether sources are newer than serialized version.
     */
    public static boolean serializedOld() {

        File serfile = new File(baseDir + File.separator + "wn.ser");
        Date saveDate = new Date(serfile.lastModified());
        for (String f : wnFilenames.values()) {
            File file = new File(f);
            Date fileDate = new Date(file.lastModified());
            if (saveDate.compareTo(fileDate) < 0) {
                return true;
            }
        }
        return false;
    }

    /** ***************************************************************
     *  Load the most recently save serialized version.
     */
    public static void loadSerialized() {

        wn = null;
        try {
            // Reading the object from a file
            FileInputStream file = new FileInputStream(baseDir + File.separator + "wn.ser");
            ObjectInputStream in = new ObjectInputStream(file);
            // Method for deserialization of object
            wn = (WordNet) in.readObject();
            if (serializedOld()) {
                wn = null;
                System.out.println("WordNet.loadSerialized(): serialized file is older than sources, " +
                        "reloding from sources.");
                return;
            }
            in.close();
            file.close();
            System.out.println("WordNet.loadSerialized(): WN has been deserialized ");
            initNeeded = false;
            System.out.println("INFO in WordNet.loadSerialized(): origMaxNounSynsetID: " +
                    wn.origMaxNounSynsetID + " maxNounSynsetID: " +
                    wn.maxNounSynsetID);
        }
        catch(IOException ex) {
            System.out.println("Error in WordNet.loadSerialized(): IOException is caught");
            ex.printStackTrace();
        }
        catch(ClassNotFoundException ex) {
            System.out.println("Error in WordNet.loadSerialized(): ClassNotFoundException is caught");
            ex.printStackTrace();
        }
    }

    /** ***************************************************************
     *  save serialized version.
     */
    public static void serialize() {

        System.out.println("INFO in WordNet.serialize(): origMaxNounSynsetID: " +
                wn.origMaxNounSynsetID + " and: " + wn.maxNounSynsetID);
        if (StringUtil.emptyString(wn.origMaxNounSynsetID))
            System.out.println("Error in WordNet.serialize(): empty max synset id");
        try {
            // Reading the object from a file
            FileOutputStream file = new FileOutputStream(baseDir + File.separator + "wn.ser");
            ObjectOutputStream out = new ObjectOutputStream(file);
            // Method for deserialization of object
            out.writeObject(wn);
            out.close();
            file.close();
            System.out.println("WordNet.serialize(): WN has been serialized ");
            initNeeded = false;
        }
        catch(IOException ex) {
            System.out.println("Error in WordNet.serialize(): IOException is caught");
            ex.printStackTrace();
        }
    }

    /** ***************************************************************
     *  create a map of upper case versions of all words
     */
    private void createIgnoreCaseMap() {

        for (String s : wn.nounSynsetHash.keySet())
            ignoreCaseSynsetHash.put(s.toUpperCase(),wn.nounSynsetHash.get(s));
        for (String s : wn.verbSynsetHash.keySet())
            ignoreCaseSynsetHash.put(s.toUpperCase(),wn.verbSynsetHash.get(s));
        for (String s : wn.adjectiveSynsetHash.keySet())
            ignoreCaseSynsetHash.put(s.toUpperCase(),wn.adjectiveSynsetHash.get(s));
        for (String s : wn.adverbSynsetHash.keySet())
            ignoreCaseSynsetHash.put(s.toUpperCase(),wn.adverbSynsetHash.get(s));
    }

    /** ***************************************************************
     *  Read the WordNet files only on initialization of the class.
     */
    private static void loadFresh() {

        if (disable) return;
        System.out.println("WordNet.loadFresh(): ");
        try {
            wn = new WordNet();
            wn.makeFileMap();
            wn.compileRegexPatterns();
            
            wn.readNouns();
            wn.readVerbs();
            wn.readAdjectives();
            wn.readAdverbs();
            wn.createIgnoreCaseMap();
            wn.origMaxNounSynsetID = wn.maxNounSynsetID;
            wn.origMaxVerbSynsetID = wn.maxVerbSynsetID;
            wn.readWordCoFrequencies();
            wn.readStopWords();
            wn.readSenseIndex(null);
            wn.readSenseCount();
            serialize(); // always create a serialized version of the latest load from source
        }
        catch (Exception ex) {
            System.out.println("Error in WordNet.loadFresh(): ");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

    /** ***************************************************************
     *  Read the WordNet files only on initialization of the class.
     */
    public static void initOnce() {

        if (KBmanager.getMgr().getPref("loadLexicons").equals("false"))
            disable = true;
        System.out.println("WordNet.initOnce(): 'disable' is: " + disable);
        if (disable) return;
        try {
            if (initNeeded == true) {
                if ((WordNet.baseDir == "") || (WordNet.baseDir == null))
                    WordNet.baseDir = KBmanager.getMgr().getPref("kbDir") + File.separator + "WordNetMappings";
                System.out.println("WordNet.initOnce(): using baseDir = " + WordNet.baseDir);
                System.out.println("WordNet.initOnce(): disable: " + disable);
                baseDirFile = new File(WordNet.baseDir);
                if (KBmanager.getMgr().getPref("loadFresh").equals("true") || !serializedExists()) {
                    System.out.println("WordNet.initOnce(): loading WordNet source files ");
                    loadFresh();
                    initNeeded = false;
                }
                else {
                    loadSerialized();
                    if (wn == null)
                        loadFresh();
                }
                DB.readSentimentArray();                
            }
        }
        catch (Exception ex) {
            System.out.println("Error in WordNet.initOnce(): ");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        System.out.println("WordNet.initOnce(): " + wn.reverseSenseIndex.keySet().size() + " senses loaded");
    }

    /** ***************************************************************
     * Split apart the block of synsets, and return the separated values
     * as an array.
     */
    private static String[] splitSynsets(String synsetBlock) {

        String[] synsetList = null;
        if (synsetBlock != null)
            synsetList = synsetBlock.split("\\s+");
        return synsetList;
    }

    /** ***************************************************************
     *  The main routine which looks up the search word in the hashtables
     *  to find the relevant word definitions and SUMO mappings.
     *  @param word is the word the user is asking to search for.
     *  @param type is whether the word is a noun or verb (we need to add capability for adjectives and adverbs.
     *  @param params is the set of html parameters
     */
    private String sumoDisplay(HashSet<String> synsetBlock, String word, String type,
            String sumokbname, String synsetNum, String params) {

        StringBuffer result = new StringBuffer();
        String synset;
        String documentation = new String();
        String sumoEquivalent = new String();
        String kbString = "&kb=" + sumokbname;

        if (synsetBlock != null) {
            System.out.println("sumoDisplay(): origMaxNounSynsetID: " + origMaxNounSynsetID);
            System.out.println("sumoDisplay(): maxNounSynsetID: " + maxNounSynsetID);
            result.append("<i>According to WordNet, the " + type + " \"" + word + "\" has ");
            result.append(synsetBlock.size() + " sense(s).</i><P>\n\n");
            Iterator<String> it = synsetBlock.iterator();

            while (it.hasNext()) {         // Split apart the SUMO concepts, and store them as an associative array.
                synset = it.next();
                synset = synset.trim();
                String ital = "";
                String italEnd = "";
                if (synset.equals(synsetNum))
                    result.append("<b>");
                if (type.compareTo("noun") == 0) {
                    documentation = (String) nounDocumentationHash.get(synset);
                    if (documentation == null)
                        documentation = "";
                    if (synset.compareTo(origMaxNounSynsetID) > 0) {
                        ital = "<i>";
                        italEnd = "</i> <small>(entry from SUMO termFormat and documentation)</small> ";
                    }
                    result.append(ital + "<a href=\"WordNet.jsp?synset=1" + synset + kbString + "&" + params + "\">1" + synset + "</a> ");
                    result.append(" " + documentation + italEnd + ".\n");
                    sumoEquivalent = (String) nounSUMOHash.get(synset);
                }
                else {
                    if (type.compareTo("verb") == 0) {
                        if (synset.compareTo(origMaxVerbSynsetID) > 0) {
                            ital = "<i>";
                            italEnd = "</i> <small>(entry from SUMO termFormat and documentation)</small> ";
                        }
                        documentation = (String) verbDocumentationHash.get(synset);
                        if (documentation == null)
                            documentation = "";
                        result.append(ital + "<a href=\"WordNet.jsp?synset=2" + synset + kbString + "&" + params + "\">2" + synset + "</a> ");
                        result.append(" " + documentation + italEnd + ".\n");
                        sumoEquivalent = (String) verbSUMOHash.get(synset);
                    }
                    else {
                        if (type.compareTo("adjective") == 0) {
                            documentation = (String) adjectiveDocumentationHash.get(synset);
                            result.append("<a href=\"WordNet.jsp?synset=3" + synset + kbString + "&" + params + "\">3" + synset + "</a> ");
                            result.append(" " + documentation + ".\n");
                            sumoEquivalent = (String) adjectiveSUMOHash.get(synset);
                        }
                        else {
                            if (type.compareTo("adverb") == 0) {
                                documentation = (String) adverbDocumentationHash.get(synset);
                                result.append("<a href=\"WordNet.jsp?synset=4" + synset + kbString + "&" + params + "\">4" + synset + "</a> ");
                                result.append(" " + documentation + ".\n");
                                sumoEquivalent = (String) adverbSUMOHash.get(synset);
                            }
                        }
                    }
                }
                if (synset.equals(synsetNum)) {
                    result.append("</b>");
                }
                if (sumoEquivalent == null) {
                    result.append("<P><ul><li>" + word + " not yet mapped to SUMO</ul><P>");
                }
                else {
                    result.append(HTMLformatter.termMappingsList(sumoEquivalent, "<a href=\"Browse.jsp?" + params + "&term="));
                }
            }
        }
        else
            result.append("<P>No " + type + " synsets\n");
        String searchTerm = word.replaceAll("_+", "+");
        searchTerm = searchTerm.replaceAll("\\s+", "+");
        result.append("<hr>Explore the word <a href=\"http://wordnetweb.princeton.edu/perl/webwn/webwn?s=");
        result.append(searchTerm + "\">"+ word + "</a> on the WordNet web site.<P>\n");
        return result.toString();
    }

    /** ***************************************************************
     * Return the root form of the noun, or null if it's not in the lexicon.
     */
    public String nounRootForm(String mixedCase, String input) {

        String result = null;

        //System.out.println("INFO in WordNet.nounRootForm: Checking word : " + mixedCase + " and " + input);
        if ((exceptionNounHash.containsKey(mixedCase)) ||
                (exceptionNounHash.containsKey(input))) {
            if (exceptionNounHash.containsKey(mixedCase))
                result = (String) exceptionNounHash.get(mixedCase);
            else
                result = (String) exceptionNounHash.get(input);
        }
        else {
            // Test all regular plural forms, and correct to singular.
            if (WordNetUtilities.substTest(input,"s$","",nounSynsetHash))
                result = WordNetUtilities.subst(input,"s$","");
            else {
                if (WordNetUtilities.substTest(input,"ses$","s", nounSynsetHash))
                    result = WordNetUtilities.subst(input,"ses$","s");
                else {
                    if (WordNetUtilities.substTest(input,"xes$","x", nounSynsetHash))
                        result = WordNetUtilities.subst(input,"xes$","x");
                    else {
                        if (WordNetUtilities.substTest(input,"zes$","z", nounSynsetHash))
                            result = WordNetUtilities.subst(input,"zes$","z");
                        else {
                            if (WordNetUtilities.substTest(input,"ches$","ch", nounSynsetHash))
                                result = WordNetUtilities.subst(input,"ches$","ch");
                            else {
                                if (WordNetUtilities.substTest(input,"shes$","sh", nounSynsetHash))
                                    result = WordNetUtilities.subst(input,"shes$","sh");
                                else {
                                    if (WordNetUtilities.substTest(input,"ies$","y", nounSynsetHash)) // but ties and series will be false
                                        result = WordNetUtilities.subst(input,"ies$","y");
                                    else {
                                        if (nounSynsetHash.containsKey(mixedCase))
                                            result = mixedCase;
                                        else {
                                            if (nounSynsetHash.containsKey(input))
                                                result = input;
                                        }
                                     }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /** ***************************************************************
     *  This routine converts a noun to its singular form and gets the synsets for it,
     *  then passes those synsets to sumoDisplay() for processing.
     *  First check to see if the input value or its lower-case version are entered in the
     *  WordNet exception list (NOUN.EXC).  If so, then use the regular form in the exception
     *  list to find the synsets in the NOUN.DAT file.
     *  If the word is not in the exception list, check to see if the lower case version of
     *  the input value is a plural and search over NOUN.DAT in the singular form if it is.
     *  Note that multi-word synsets must have underscores in place of spaces.
     */
    private String processNoun (String sumokbname, String mixedCase, String input,
            String synset, String params) {

        String regular = null;
        HashSet<String> synsetBlock = null;

        regular = nounRootForm(mixedCase,input);
        //System.out.println("Info in WordNet.processNoun(): root form: " + regular);
        if (regular != null) {
            synsetBlock = nounSynsetHash.get(regular);
            return sumoDisplay(synsetBlock,mixedCase,"noun",sumokbname,synset,params);
        }
        else
            return "<P>There are no associated SUMO terms for the noun \"" + mixedCase + "\".<P>\n";
    }

    /** ***************************************************************
     * Return the present tense singular form of the verb, or null if
     * it's not in the lexicon.
     */
    public String verbRootForm(String mixedCase, String input) {

        String result = null;
        //System.out.println("INFO in verbRootForm(): word: " + mixedCase + " " + input);
        if ((exceptionVerbHash.containsKey(mixedCase)) ||
                (exceptionVerbHash.containsKey(input))) {
            //System.out.println("INFO in verbRootForm(): exception ");
            if (exceptionVerbHash.containsKey(mixedCase))
                result = (String) exceptionVerbHash.get(mixedCase);
            else
                result = (String) exceptionVerbHash.get(input);
        }
        else {
            //System.out.println("INFO in verbRootForm(): not an exception: " + mixedCase);
            // Test all regular forms and convert to present tense singular.
            if (WordNetUtilities.substTest(input,"s$","",verbSynsetHash)) {
                result = WordNetUtilities.subst(input,"s$","");
                //System.out.println("INFO in verbRootForm(): word: " + result);
            }
            else {
                if (WordNetUtilities.substTest(input,"es$","",verbSynsetHash))
                    result = WordNetUtilities.subst(input,"es$","");
                else {
                    if (WordNetUtilities.substTest(input,"ies$","y",verbSynsetHash))
                        result = WordNetUtilities.subst(input,"ies$","y");
                    //else if (WordNetUtilities.substTest(input,"ied$","y",verbSynsetHash)) // new 10/29/2014
                    //    result = WordNetUtilities.subst(input,"ied$","y");
                    else if (WordNetUtilities.substTest(input,"ed$","",verbSynsetHash))
                        result = WordNetUtilities.subst(input,"ed$","");
                    else if (WordNetUtilities.substTest(input,"ed$","e",verbSynsetHash))
                        result = WordNetUtilities.subst(input,"ed$","e");
                    else if (WordNetUtilities.substTest(input,"ing$","e",verbSynsetHash))
                        result = WordNetUtilities.subst(input,"ing$","e");
                    else if (WordNetUtilities.substTest(input,"ing$","",verbSynsetHash))
                        result = WordNetUtilities.subst(input,"ing$","");
                    else if (verbSynsetHash.containsKey(mixedCase))
                        result = mixedCase;
                    else if (verbSynsetHash.containsKey(input))
                        result = input;                    
                }
            }
        }
        //System.out.println("INFO in verbRootForm(): result: " + result);
        return result;
    }

    /** ***************************************************************
     *  This routine converts a verb to its present tense singular form and gets the synsets for it,
     *  then passes those synsets to sumoDisplay() for processing.
     *  First check to see if the input value or its lower-case version are entered in the
     *  WordNet exception list (VERB.EXC).  If so, then use the regular form in the exception
     *  list to find the synsets in the VERB.DAT file.
     *  If the word is not in the exception list, check to see if the lower case version of the
     *  input value is a singular form and search over VERB.DAT with the infinitive form if it is.
     *  Note that multi-word synsets must have underscores in place of spaces.
     */
    private String processVerb(String sumokbname, String mixedCase, String input,
            String synset, String params) {

        String regular = null;
        HashSet<String> synsetBlock = null;

        regular = verbRootForm(mixedCase,input);
        System.out.println("INFO in processVerb(): word: " + regular);
        if (regular != null) {
            synsetBlock = verbSynsetHash.get(regular);
            return sumoDisplay(synsetBlock, mixedCase, "verb", sumokbname,synset,params);
        }
        else
            return "<P>There are no associated SUMO terms for the verb \"" + mixedCase + "\".<P>\n";
    }

    /** ***************************************************************
     * This routine gets the synsets for an adverb, then passes those
     * synsets to sumoDisplay() for processing.
     * Note that multi-word synsets must have underscores in place of spaces.
     */
    private String processAdverb(String sumokbname, String mixedCase, String input,
            String synset, String params) {

        StringBuffer result = new StringBuffer();
        HashSet<String> synsetBlock = null;

        synsetBlock = adverbSynsetHash.get(input);
        result.append(sumoDisplay(synsetBlock, mixedCase, "adverb", sumokbname,synset,params));

        return (result.toString());
    }

    /** ***************************************************************
     * This routine gets the synsets for an adjective, then passes those
     * synsets to sumoDisplay() for processing.
     * Note that multi-word synsets must have underscores in place of spaces.
     */
    private String processAdjective(String sumokbname, String mixedCase,
            String input, String synset, String params) {

        StringBuffer result = new StringBuffer();
        HashSet<String> synsetBlock = null;

        synsetBlock = adjectiveSynsetHash.get(input);
        result.append(sumoDisplay(synsetBlock, mixedCase, "adjective", sumokbname,synset,params));

        return (result.toString());
    }

    /** ***************************************************************
     * Prepend a POS number to a set of 8 digit synsets
     *
     * @return an ArrayList of 9 digit synset Strings
     */
    public HashSet<String> prependPOS(HashSet<String> synsets, String POS) {

        HashSet<String> result = new HashSet<>();
        for (String s : synsets)
            result.add(POS + s);
        return result;
    }

    /** ***************************************************************
     * Get all the synsets for a given word. Print an error if this
     * routine gives a result and getSenseKeysFromWord() doesn't
     *
     * @return an ArrayList of 9 digit synset Strings
     */
    public HashSet<String> getSynsetsFromWord(String word) {

        HashSet<String> result = new HashSet<>();
        HashSet<String> nouns = nounSynsetHash.get(word);
        if (nouns != null)
            result.addAll(prependPOS(nouns,"1"));
        HashSet<String> verbs = verbSynsetHash.get(word);
        if (verbs != null)
            result.addAll(prependPOS(verbs,"1"));
        HashSet<String> adj = adjectiveSynsetHash.get(word);
        if (adj != null)
            result.addAll(prependPOS(adj,"1"));
        HashSet<String> adv = adverbSynsetHash.get(word);
        if (adv != null)
            result.addAll(prependPOS(adv,"1"));
        if (result.size() > 0 && getSenseKeysFromWord(word).keySet().size() == 0)
            System.out.println("Error in WordNet.getSynsetsFromWord(): synset but no sense key for word: " + word);
        return result;
    }

    /** ***************************************************************
     * Get all the synsets for a given word.
     * @return a TreeMap of sense keys in the form of word_POS_num
     * and values that are ArrayLists of synset Strings
     */
    public TreeMap<String,ArrayList<String>> getSenseKeysFromWord(String word) {

        TreeMap<String,ArrayList<String>> result = new TreeMap<String,ArrayList<String>>();
        String verbRoot = verbRootForm(word,word.toLowerCase());
        String nounRoot = nounRootForm(word,word.toLowerCase());
        ArrayList<String> senseKeys = wordsToSenseKeys.get(verbRoot);
        if (senseKeys != null) {
            for (int i = 0; i < senseKeys.size(); i++) {
                String senseKey = (String) senseKeys.get(i);                // returns a word_POS_num
                if (!isValidKey(senseKey)) {
                    System.out.println("Error in WordNet.getSenseKeysFromWord: invalid key: " + senseKey);
                    senseKey = null;
                }
                if (senseKey != null) {
                    String POS = WordNetUtilities.getPOSfromKey(senseKey);
                    if (POS != null) {
                        String synset = WordNetUtilities.posLettersToNumber(POS) + ((String) senseIndex.get(senseKey));
                        if (synset != null) {
                            ArrayList<String> words = synsetsToWords.get(synset);
                            if (words != null) {
                                Iterator<String> it2 = words.iterator();
                                while (it2.hasNext()) {
                                    String newword = (String) it2.next();
                                    ArrayList<String> al = result.get(newword);
                                    if (al == null) {
                                        al = new ArrayList<String>();
                                        result.put(newword,al);
                                    }
                                    al.add(synset);
                                }
                            }
                        }
                    }
                }
            }
        }
        senseKeys = wordsToSenseKeys.get(nounRoot);
        if (senseKeys != null) {
            for (int i = 0; i < senseKeys.size(); i++) {
                String senseKey = (String) senseKeys.get(i);                // returns a word_POS_num
                if (!isValidKey(senseKey)) {
                    System.out.println("Error in WordNet.getSenseKeysFromWord: invalid key: " + senseKey);
                    senseKey = null;
                }
                if (senseKey != null) {
                    String POS = WordNetUtilities.getPOSfromKey(senseKey);
                    if (POS != null) {
                        String synset = WordNetUtilities.posLettersToNumber(POS) + ((String) senseIndex.get(senseKey));
                        if (synset != null) {
                            ArrayList<String> words = synsetsToWords.get(synset);
                            if (words != null) {
                                Iterator<String> it2 = words.iterator();
                                while (it2.hasNext()) {
                                    String newword = (String) it2.next();
                                    ArrayList<String> al = result.get(newword);
                                    if (al == null) {
                                        al = new ArrayList<String>();
                                        result.put(newword,al);
                                    }
                                    al.add(synset);
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /** ***************************************************************
     * Get the words and synsets corresponding to a SUMO term. The
     * return is a Map of words with their corresponding synset number.
     */
    public TreeMap<String,String> getWordsFromTerm(String SUMOterm) {

        TreeMap<String,String> result = new TreeMap<String,String>();
        ArrayList<String> synsets = SUMOHash.get(SUMOterm);
        if (synsets == null) {
            System.out.println("INFO in WordNet.getWordsFromTerm(): No synsets for term : " + SUMOterm);
            return null;
        }
        Iterator<String> it = synsets.iterator();
        while (it.hasNext()) {
            String synset = (String) it.next();
            ArrayList<String> words = synsetsToWords.get(synset);
            if (words == null) {
                System.out.println("INFO in WordNet.getWordsFromTerm(): No words for synset: " + synset);
                return null;
            }
            Iterator<String> it2 = words.iterator();
            while (it2.hasNext()) {
                String word = (String) it2.next();
                result.put(word,synset);
            }
        }
        return result;
    }
    /** ***************************************************************
     */
    public ArrayList<String> getWordsFromSynset(String synset) {

        return WordNet.wn.synsetsToWords.get(synset);
    }

    /** ***************************************************************
     * Get the SUMO term for the given root form word and part of speech.

    public String getSUMOterm(String word, int pos) {

        if (StringUtil.emptyString(word))
            return null;
        HashSet<String> synsetBlock = null;  // A String of synsets, which are 8 digit numbers, separated by spaces.

        //System.out.println("INFO in WordNet.getSUMOterm: Checking word : " + word);
        if (pos == NOUN)
            synsetBlock = nounSynsetHash.get(word);
        if (pos == VERB)
            synsetBlock = verbSynsetHash.get(word);
        if (pos == ADJECTIVE)
            synsetBlock = adjectiveSynsetHash.get(word);
        if (pos == ADVERB)
            synsetBlock = adverbSynsetHash.get(word);

        //int listLength;
        String synset;
        String[] synsetList = null;
        if (synsetBlock != null)
            synsetList = synsetBlock.split("\\s+");
        String term = null;

        if (synsetList != null) {
            synset = synsetList[0];   // Just get the first synset.  This needs to be changed to a word sense disambiguation algorithm.
            synset = synset.trim();
            if (pos == NOUN)
                term =  (String) nounSUMOHash.get(synset);
            if (pos == VERB)
                term =  (String) verbSUMOHash.get(synset);
            if (pos == ADJECTIVE)
                term =  (String) adjectiveSUMOHash.get(synset);
            if (pos == ADVERB)
                term =  (String) adverbSUMOHash.get(synset);
        }
        if (term != null)
            return term.trim().substring(2,term.trim().length() - 1);
        else
            return null;
    }
*/
    /** ***************************************************************
     * Does WordNet contain the given word.
     */
    public boolean containsWord(String word, int pos) {

        if (debug) System.out.println("INFO in WordNet.containsWord: Checking word : " + word);
        if (pos == NOUN && nounSynsetHash.containsKey(word))
            return true;
        if (pos == VERB && verbSynsetHash.containsKey(word))
            return true;
        if (pos == ADJECTIVE && adjectiveSynsetHash.containsKey(word))
            return true;
        if (pos == ADVERB && adverbSynsetHash.containsKey(word))
            return true;
        if (pos == ADJECTIVE_SATELLITE && adjectiveSynsetHash.containsKey(word))
            return true;
        else
            return false;
    }

    /** ***************************************************************
     * Does WordNet contain the given word.
     */
    public boolean containsWord(String word) {

        for (int i = 0; i <= 5; i++) {
            if (containsWord(word,i))
                return true;
        }
        return false;
    }

    /** ***************************************************************
     * Does WordNet contain the given word, ignoring case.
     */
    public boolean containsWordIgnoreCase(String word) {

        //System.out.println("containsWordIgnoreCase(): " + word);
        //System.out.println("containsWordIgnoreCase(): " + word.toUpperCase());
        if (ignoreCaseSynsetHash.containsKey(word.toUpperCase()))
            return true;
        return false;
    }

    /** ***************************************************************
     * This is the regular point of entry for this class.  It takes the
     * word the user is searching for, and the part of speech index, does
     * the search, and returns the string with HTML formatting codes to
     * present to the user.  The part of speech codes must be the same as in
     * the menu options in WordNet.jsp and Browse.jsp
     *
     *  @param inp The string the user is searching for.
     *  @param pos The part of speech of the word 1=noun, 2=verb, 3=adjective, 4=adverb
     *  @return A string contained the HTML formatted search result.
     */
    public String page (String inp, int pos, String kbname, String synset, String params) {

        String input = inp;
        StringBuffer buf = new StringBuffer();
        
        String mixedCase = input;
        String str = input;
        if (input.contains(" "))
            str = StringUtil.spacesToUnderlines(input);
        if (input.matches(".*[a-z][A-Z].*"))
            str = StringUtil.camelCaseToUnderlines(input);

        mixedCase = str;
        input = str.toLowerCase();
        boolean found = false;
        if (pos == NOUN || pos == 0) {
            String res = processNoun(kbname, mixedCase, input, synset, params);
            if (!res.startsWith("<P>There are no"))
                found = true;
            if (found == true || pos != 0)
                buf.append(res);
        }
        if (pos == VERB || pos == 0) {
            String res = processVerb(kbname, mixedCase, input, synset, params);
            if (!res.startsWith("<P>There are no"))
                found = true;
            if (found == true || pos != 0)
                buf.append(res);
        }
        if (pos == ADJECTIVE || pos == 0) {
            String res = processAdjective(kbname, mixedCase, input, synset, params);
            if (!res.isEmpty())
                found = true;
            if (found == true || pos != 0)
                buf.append(res);
        }
        if (pos == ADVERB || pos == 0) {
            String res = processAdverb(kbname, mixedCase, input, synset, params);
            if (!res.isEmpty())
                found = true;
            if (found == true || pos != 0)
                buf.append(res);
        }
        buf.append("\n");

        return buf.toString();
    }

    /** ***************************************************************
     * @param synset is a synset with POS-prefix
     */
    public String getDocumentation (String synset) {

        char POS = synset.charAt(0);
        String bareSynset = synset.substring(1);
        switch (POS) {
        case '1': return (String) nounDocumentationHash.get(bareSynset);
        case '2': return (String) verbDocumentationHash.get(bareSynset);
        case '3': return (String) adjectiveDocumentationHash.get(bareSynset);
        case '4': return (String) adverbDocumentationHash.get(bareSynset);
        }
        return null;
    }

    /** ***************************************************************
     * @param synset is a synset with POS-prefix
     */
    public String displaySynset (String sumokbname, String synset, String params) {

        StringBuffer buf = new StringBuffer();
        char POS = synset.charAt(0);
        String gloss = "";
        String SUMOterm = "";
        String POSstring = "";
        String bareSynset = synset.substring(1);
        switch (POS) {
            case '1': gloss = (String) nounDocumentationHash.get(bareSynset);
                SUMOterm = (String) nounSUMOHash.get(bareSynset);
                POSstring = "Noun";
                break;
            case '2': gloss = (String) verbDocumentationHash.get(bareSynset);
                SUMOterm = (String) verbSUMOHash.get(bareSynset);
                POSstring = "Verb";
                break;
            case '3': gloss = (String) adjectiveDocumentationHash.get(bareSynset);
                SUMOterm = (String) adjectiveSUMOHash.get(bareSynset);
                POSstring = "Adjective";
                break;
            case '4': gloss = (String) adverbDocumentationHash.get(bareSynset);
                SUMOterm = (String) adverbSUMOHash.get(bareSynset);
                POSstring = "Adverb";
                break;
        }
        if (gloss == null) {
            return (synset + " is not a valid synset number.<P>\n");
        }
        buf.append("<b>" + POSstring + " Synset:</b> " + synset);
        if (SUMOterm != null && SUMOterm != "")
            buf.append(HTMLformatter.termMappingsList(SUMOterm,"<a href=\"Browse.jsp?" + params + "&term="));
        TreeSet<String> words = new TreeSet<String>();
        ArrayList<String> al = synsetsToWords.get(synset);
        if (al != null)
            words.addAll(al);
        buf.append(" <b>Words:</b> ");
        Iterator<String> it = words.iterator();
        while (it.hasNext()) {
            String word = it.next();
            buf.append(word);
            if (it.hasNext())
                buf.append(", ");
        }
        buf.append("<P>\n <b>Gloss:</b> " + gloss);
        buf.append("<P>\n");
        ArrayList<AVPair> al3 = relations.get(synset);
        if (al3 != null) {
            Iterator<AVPair> it3 = al3.iterator();
            while (it3.hasNext()) {
                AVPair avp = it3.next();
                buf.append(avp.attribute + " ");
                buf.append("<a href=\"WordNet.jsp?synset=" + avp.value + "&" + params + "\">" + avp.value + "</a> - ");
                words = new TreeSet<String>();
                ArrayList<String> al2 = synsetsToWords.get(avp.value);
                if (al2 != null)
                    words.addAll(al2);
                Iterator<String> it2 = words.iterator();
                while (it2.hasNext()) {
                    String word = it2.next();
                    buf.append(word);
                    if (it2.hasNext())
                        buf.append(", ");
                }
                buf.append("<br>\n");
            }
            buf.append("<P>\n");
        }
        return buf.toString();
    }

    /** ***************************************************************
     * @param key is a WordNet sense key
     * @return 9-digit POS-prefix and synset number
     */
    public String displayByKey (String sumokbname, String key, String params) {

        String synset = (String) senseIndex.get(key);
        String POS = WordNetUtilities.getPOSfromKey(key);
        POS = POS.substring(0,1);
        return displaySynset(sumokbname,POS+synset,params);
    }

    /** ***************************************************************
     
    private String fromXML(SimpleElement configuration) {

        StringBuffer result = new StringBuffer();
        if (!configuration.getTagName().equals("wordnet"))
            System.out.println("Error in KBmanager.fromXML(): Bad tag: " + configuration.getTagName());
        else {
        }
        return result.toString();
    }
*/
    /** ***************************************************************
     */
    private SimpleElement toXML() {

        SimpleElement top = new SimpleElement("wordnet");
        Iterator<String> it = synsetsToWords.keySet().iterator();
        while (it.hasNext()) {
            String synset = (String) it.next();
            String gloss = "";
            String name = "";
            String SUMO = "";
            switch (synset.charAt(0)) {
            case '1': gloss = (String) nounDocumentationHash.get(synset.substring(1));
            SUMO = (String) nounSUMOHash.get(synset.substring(1));
            break;
            case '2': gloss = (String) verbDocumentationHash.get(synset.substring(1));
            SUMO = (String) verbSUMOHash.get(synset.substring(1));
            break;
            case '3': gloss = (String) adjectiveDocumentationHash.get(synset.substring(1));
            SUMO = (String) adjectiveSUMOHash.get(synset.substring(1));
            break;
            case '4': gloss = (String) adverbDocumentationHash.get(synset.substring(1));
            SUMO = (String) adverbSUMOHash.get(synset.substring(1));
            break;
            }
            if (gloss != null)
                gloss = gloss.replaceAll("\"","&quote;");
            ArrayList<String> al2 = synsetsToWords.get(synset);
            Iterator<String> itts = al2.iterator();
            if (itts.hasNext())
                name = (String) itts.next();
            SimpleElement item = new SimpleElement("item");
            item.setAttribute("id",synset);
            item.setAttribute("offset",synset);
            item.setAttribute("type","synset");
            item.setAttribute("name",name);
            item.setAttribute("source","WordNet 2.0");
            item.setAttribute("gloss",gloss);
            top.addChildElement(item);
            ArrayList<AVPair> al = relations.get(synset);
            if (SUMO != "") {
                String bareTerm = WordNetUtilities.getBareSUMOTerm(SUMO);
                char mapping = WordNetUtilities.getSUMOMappingSuffix(SUMO);
                SimpleElement link = new SimpleElement("link");
                link.setAttribute("id1",synset);
                link.setAttribute("id2",bareTerm);
                String mapName = WordNetUtilities.mappingCharToName(mapping);
                link.setAttribute("type",mapName);
                top.addChildElement(link);
            }
            if (al != null) {
                Iterator<AVPair> it2 = al.iterator();
                while (it2.hasNext()) {
                    SimpleElement link = new SimpleElement("link");
                    AVPair avp = it2.next();
                    link.setAttribute("type",avp.attribute);
                    link.setAttribute("id1",synset);
                    link.setAttribute("id2",avp.value);
                    top.addChildElement(link);
                }
            }
        }
        return top;
    }

    /** ***************************************************************
     */
    public void writeXML() {

        FileWriter fw = null;
        PrintWriter pw = null;
        String dir = WordNet.baseDir;
        String fname = "WordNet.xml";

        try {
            fw = new FileWriter(dir + File.separator + fname);
            pw = new PrintWriter(fw);
            SimpleElement se = toXML();
            pw.println(se.toFileString());
        }
        catch (Exception e) {
            System.out.println("Error in WordNet.writeXML(): " + e.getMessage());
            System.out.println("Error writing file " + dir + File.separator + fname + "\n" + e.getMessage());
            e.printStackTrace();
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
            }
        }
    }

    /** *************************************************************
     */
    private static boolean arrayContains(int[] ar, int value) {

        //System.out.println("INFO in WordNet.arrayContains: value: " + value);
        for (int i = 0; i < ar.length; i++) {
            if (ar[i] == value)
                return true;
        }
        return false;
    }

    /** *************************************************************
     * Frame transitivity
     *   intransitive - 1,2,3,4,7,23,35
     *   transitive - everything else
     *   ditransitive - 15,16,17,18,19
     */
    public String getTransitivity(String synset, String word) {

        //System.out.println("INFO in WordNet.getTransitivity: synset, word: " + synset + " " + word);
        int[] intrans = {1,2,3,4,7,23,35};
        int[] ditrans = {15,16,17,18,19};
        String intransitive = "no";
        String transitive = "no";
        String ditransitive = "no";
        ArrayList<String> frames = new ArrayList<String>();
        ArrayList<String> res = verbFrames.get(synset);
        if (res != null)
            frames.addAll(res);
        res = verbFrames.get(synset + "-" + word);
        if (res != null)
            frames.addAll(res);
        for (int i = 0; i < frames.size(); i++) {
            int value = (Integer.valueOf((String) frames.get(i))).intValue();
            if (arrayContains(intrans,value))
                intransitive = "intransitive";
            else
                if (arrayContains(ditrans,value))
                    ditransitive = "ditransitive";
                else
                    transitive = "transitive";
        }

        return "[" + intransitive + "," + transitive + "," + ditransitive + "]";
    }

    /** *************************************************************
     * Replace underscores with commas, wrap hyphenatid and apostrophed words in single
     * quotes, and wrap the whole phrase in brackets.  Used only in conversion
     * to Prolog.
     */
    private static String processMultiWord(String word) {

        word = word.replace('_',',');
        word = word.replace("'","\\'");
        String words[] = word.split(",");
        word = "";
        for (int i = 0; i < words.length; i++) {
            if (words[i].length() > 0 &&
                    (words[i].indexOf("-") > -1 || (words[i].indexOf(".") > -1) ||
                            (words[i].indexOf("\\'") > -1) || Character.isUpperCase(words[i].charAt(0)) || Character.isDigit(words[i].charAt(0))))
                words[i] = "'" + words[i] + "'";
            word = word + words[i];
            if (i < words.length-1)
                word = word + ",";
        }
        return "[" + word + "]";
    }

    /** *************************************************************
     */
    private static String multipleMappingToProlog(String term) {

        if (term.indexOf("&%") < 0)
            return term;
        else {
            term = "[" + term + "]";
            term = term.replaceAll("[\\=\\+] \\&\\%","','");
        }
        return term;
    }

    /** *************************************************************
     * verb_in_lexicon(Verb for singular mode, Verb for plural mode, {transitive,
     * intransitive, [intransitive, transitive, ditransitive], [no, no,
     * ditransitive], [no, transitive, no], [intransitive, no, no], [no,
     * transitive, ditransitive], [intransitive, transitive, no], [no, no, no],
     * [intransitive, no, ditransitive]}, singular,  {simple, prepositional,
     * compound, phrasal}, {event, state}, SUMOMapping., Synset_ID).
     */
    private void writeVerbsProlog(PrintWriter pw, KB kb) throws IOException {

        Iterator<String> it = verbSynsetHash.keySet().iterator();
        while (it.hasNext()) {
            String word = (String) it.next();
            String compound = "simple";
            if (word.indexOf("_") > -1)
                compound = "compound";

            HashSet<String> stringSynsets = verbSynsetHash.get(word);
            String plural = WordNetUtilities.verbPlural(word);
            if (word.indexOf("_") > -1) {
                word = processMultiWord(word);
                plural = processMultiWord(plural);

            }
            else {
                word = word.replace("'","\\'");
                if (word.indexOf("-") > -1 || (word.indexOf(".") > -1) ||
                        (word.indexOf("\\'") > -1) || Character.isUpperCase(word.charAt(0)) || Character.isDigit(word.charAt(0))) {
                    word = "'" + word + "'";
                    plural = "'" + plural + "'";
                }
            }
            Iterator<String> it2 = stringSynsets.iterator();
            while (it2.hasNext()) {
                String synset = it2.next();
                String sumoTerm = (String) verbSUMOHash.get(synset);
                if (sumoTerm != null && sumoTerm != "") {
                    String bareSumoTerm = WordNetUtilities.getBareSUMOTerm(sumoTerm);
                    String transitivity = getTransitivity(synset,word);
                    String eventstate = "state";
                    if (kb.childOf(bareSumoTerm,"Process"))
                        eventstate = "event";
                    bareSumoTerm = multipleMappingToProlog(bareSumoTerm);
                    pw.println("verb_in_lexicon(" + plural + "," + word + "," + transitivity +
                            ", singular, " + compound + ", " + eventstate + ", '" + bareSumoTerm + "',2" +
                            synset + ").");
                }
            }
        }
    }

    /** *************************************************************
     * adjective_in_lexicon(Adj, CELT_form, {normal, two_place}, {positive,
     * ungraded, comparative, superlative}, SUMOMapping).
     */
    private void writeAdjectivesProlog(PrintWriter pw, KB kb) throws IOException {

        Iterator<String> it = adjectiveSynsetHash.keySet().iterator();
        while (it.hasNext()) {
            String word = (String) it.next();
            HashSet<String> stringSynsets = adjectiveSynsetHash.get(word);
            if (word.indexOf("_") > -1)
                word = processMultiWord(word);
            else {
                word = word.replace("'","\\'");
                if (word.indexOf("-") > -1 || (word.indexOf(".") > -1) ||
                        (word.indexOf("\\'") > -1) || Character.isUpperCase(word.charAt(0)) || Character.isDigit(word.charAt(0))) {
                    word = "'" + word + "'";
                }
            }
            Iterator<String> it2 = stringSynsets.iterator();
            while (it2.hasNext()) {
                String synset = it2.next();
                String sumoTerm = (String) adjectiveSUMOHash.get(synset);
                if (sumoTerm != null && sumoTerm != "") {
                    String bareSumoTerm = WordNetUtilities.getBareSUMOTerm(sumoTerm);
                    bareSumoTerm = multipleMappingToProlog(bareSumoTerm);
                    pw.println("adjective_in_lexicon(" + word + "," + word + ",normal,positive," +
                            bareSumoTerm + ").");
                }
            }
        }
    }

    /** *************************************************************
     * adverb_in_lexicon(Adv, {location, direction, time, duration, frequency,
     * manner}, SUMOMapping).
     */
    private void writeAdverbsProlog(PrintWriter pw, KB kb) throws IOException {

        Iterator<String> it = verbSynsetHash.keySet().iterator();
        while (it.hasNext()) {
            String word = (String) it.next();
            HashSet<String> stringSynsets = verbSynsetHash.get(word);
            if (word.indexOf("_") > -1)
                word = processMultiWord(word);
            else {
                word = word.replace("'","\\'");
                if (word.indexOf("-") > -1 || (word.indexOf(".") > -1) ||
                        (word.indexOf("\\'") > -1) || Character.isUpperCase(word.charAt(0)) || Character.isDigit(word.charAt(0))) {
                    word = "'" + word + "'";
                }
            }
            Iterator<String> it2 = stringSynsets.iterator();
            while (it2.hasNext()) {
                String synset = it2.next();
                String sumoTerm = (String) verbSUMOHash.get(synset);
                if (sumoTerm != null && sumoTerm != "") {
                    String bareSumoTerm = WordNetUtilities.getBareSUMOTerm(sumoTerm);
                    bareSumoTerm = multipleMappingToProlog(bareSumoTerm);
                    pw.println("adverb_in_lexicon(" + word + ",null," + bareSumoTerm + ").");
                }
            }
        }
    }

    /** *************************************************************
     *  noun_in_lexicon(Noun,{object, person, time}, neuter, {count, mass}, singular, SUMOMapping, Synset_ID).
     */
    private void writeNounsProlog(PrintWriter pw, KB kb) throws IOException {

        Iterator<String> it = nounSynsetHash.keySet().iterator();
        while (it.hasNext()) {
            String word = (String) it.next();
            HashSet<String> stringSynsets = nounSynsetHash.get(word);
            boolean uppercase = false;
            if (Character.isUpperCase(word.charAt(0)))
                uppercase = true;
            if (word.indexOf("_") > -1)
                word = processMultiWord(word);
            else {
                word = word.replace("'","\\'");
                if ((word.indexOf("-") > -1) || (word.indexOf(".") > -1) || (word.indexOf("/") > -1)
                        || (word.indexOf("\\'") > -1) || uppercase || Character.isDigit(word.charAt(0)))
                    word = "'" + word + "'";
            }
            Iterator<String> it2 = stringSynsets.iterator();
            while (it2.hasNext()) {
                String synset = it2.next();
                String sumoTerm = (String) nounSUMOHash.get(synset);
                if (sumoTerm != null && sumoTerm != "") {
                    String bareSumoTerm = WordNetUtilities.getBareSUMOTerm(sumoTerm);
                    char mapping = WordNetUtilities.getSUMOMappingSuffix(sumoTerm);
                    String type = "object";
                    if (kb.childOf(bareSumoTerm,"Human") || kb.childOf(bareSumoTerm,"SocialRole"))
                        type = "person";
                    if (kb.childOf(bareSumoTerm,"TimePosition") || kb.childOf(bareSumoTerm,"Process"))
                        type = "time";
                    String countOrMass = "count";
                    if (kb.childOf(bareSumoTerm,"Substance"))
                        countOrMass = "mass";
                    boolean instance = false;
                    if (uppercase && mapping == '@')
                        instance = true;
                    if (mapping == '=') {
                        ArrayList<Formula> al = kb.instancesOf(bareSumoTerm);
                        if (al.size() > 0)
                            instance = true;
                    }
                    if (instance && uppercase) {
                        ArrayList<Formula> al = kb.askWithRestriction(1,bareSumoTerm,0,"instance");
                        String parentTerm = "";
                        if (al != null && al.size() > 0) {
                            parentTerm = al.get(0).getStringArgument(2);
                        }
                        else
                            parentTerm = bareSumoTerm;
                        bareSumoTerm = multipleMappingToProlog(bareSumoTerm);
                        parentTerm = multipleMappingToProlog(parentTerm);
                        pw.println("proper_noun_in_lexicon(" + word + "," + type + ", neuter, singular, '" +
                                parentTerm + "','" + bareSumoTerm + "',1" + synset + ").");
                    }
                    else {
                        bareSumoTerm = multipleMappingToProlog(bareSumoTerm);
                        pw.println("noun_in_lexicon(" + word + "," + type + ", neuter, " +
                                countOrMass + ", singular, '" + bareSumoTerm + "',1" +
                                synset + ").");
                    }
                }
            }
        }
    }

    /** ***************************************************************
     */
    public void writeProlog(KB kb) {

        FileWriter fw = null;
        PrintWriter pw = null;
        String dir = WordNet.baseDir;
        String fname = "WordNet.pl";

        try {
            fw = new FileWriter(dir + File.separator + fname);
            pw = new PrintWriter(fw);
            writeNounsProlog(pw,kb);
            writeVerbsProlog(pw,kb);
            writeAdjectivesProlog(pw,kb);
            writeAdverbsProlog(pw,kb);
        }
        catch (Exception e) {
            System.out.println("Error in WordNet.writeProlog(): " + e.getMessage());
            System.out.println("Error writing file " + dir + File.separator + fname + "\n" + e.getMessage());
            e.printStackTrace();
        }
        finally {
            try {
                if (pw != null)
                    pw.close();
                if (fw != null)
                    fw.close();
            }
            catch (Exception ex) {
            }
        }
    }

    /** ***************************************************************
     */
    public String senseKeyPOS (String senseKey) {

        if (StringUtil.emptyString(senseKey)) 
            return "";
        int underscore2 = senseKey.lastIndexOf("_");
        if (underscore2 < 0) return "";
        int underscore1 = senseKey.lastIndexOf("_",underscore2-1);
        if (underscore1 < 0) return "";
        return senseKey.substring(underscore1+1, underscore2);
    }

    /** ***************************************************************
     */
    private String senseKeySenseNum (String senseKey) {

        if (senseKey == null) return "";

        int underscore2 = senseKey.lastIndexOf("_");
        if (underscore2 < 0) return "";
        int underscore1 = senseKey.lastIndexOf("_",underscore2-1);
        if (underscore1 < 0) return "";
        return senseKey.substring(underscore2+1,senseKey.length());
    }

    /** ***************************************************************
     * Find the "word number" of a word and synset, which is its place
     * in the list of words belonging to a given synset.  Return -1 if
     * not found.
     */
    private int findWordNum(String POS, String synset, String word) {

        ArrayList<String> al = synsetsToWords.get(POS+synset);
        if (al == null || al.size() < 1) {
            System.out.println("Error in WordNet.findWordNum(): No words found for synset: " + POS + synset + " and word " + word);
            return -1;
        }
        for (int i = 0; i < al.size(); i++) {
            String storedWord = (String) al.get(i);
            if (word.equalsIgnoreCase(storedWord)) {
                return i+1;
            }
        }
        System.out.println("Error in WordNet.findWordNum(): No match found for synset: " + POS + synset + " and word " + word);
        System.out.println(al);
        return -1;
    }

    /** ***************************************************************
     */
    private String processWordForProlog(String word) {

        String result = new String(word);
        int start = 0;
        while (result.indexOf("'",start) > -1) {
            int i = 0;
            i = result.indexOf("'",start);
            //System.out.println("INFO in WordNet.processPrologString(): index: " + i + " string: " + doc);
            if (i == 0)
                result = "''" + result.substring(i+1);
            else
                result = result.substring(0,i) + "\\'" + result.substring(i+1);
            start = i+2;
        }
        return result;
    }

    /** ***************************************************************
     * Write WordNet data to a prolog file with a single kind of clause
     * in the following format:
     * s(Synset_ID, Word_No_in_the_Synset, Word, SS_Type,
     * Synset_Rank_By_the_Word,Tag_Count)
     */
    public void writeWordNetS() {

        FileWriter fw = null;
        PrintWriter pw = null;
        String dir = WordNet.baseDir;
        String fname = "Wn_s.pl";

        try {
            fw = new FileWriter(dir + File.separator + fname);
            pw = new PrintWriter(fw);
            if (wordsToSenseKeys.keySet().size() < 1)
                System.out.println("Error in WordNet.writeWordNetS(): No contents in sense index");
            Iterator<String> it = wordsToSenseKeys.keySet().iterator();
            while (it.hasNext()) {
                String word = (String) it.next();
                String processedWord = processWordForProlog(word);
                ArrayList<String> keys = wordsToSenseKeys.get(word);
                Iterator<String> it2 = keys.iterator();
                if (keys.size() < 1)
                    System.out.println("Error in WordNet.writeWordNetS(): No synsets for word: " + word);
                while (it2.hasNext()) {
                    String senseKey = (String) it2.next();
                    //System.out.println("INFO in WordNet.writeWordNetS(): Sense key: " + senseKey);
                    String POS = senseKeyPOS(senseKey);
                    String senseNum = senseKeySenseNum(senseKey);
                    if (POS == "" || senseNum == "")
                        System.out.println("Error in WordNet.writeWordNetS(): Bad sense key: " + senseKey);
                    POS = WordNetUtilities.posLettersToNumber(POS);
                    String POSchar = Character.toString(WordNetUtilities.posNumberToLetter(POS.charAt(0)));
                    String synset = (String) senseIndex.get(senseKey);
                    int wordNum = findWordNum(POS,synset,word);
                    pw.println("s(" + POS + synset + "," + wordNum + ",'" + processedWord + "'," + POSchar + "," + senseNum + ",1).");
                }
            }
        }
        catch (Exception e) {
            System.out.println("Error in WordNet.writeWordNetS(): ");
            System.out.println("Error writing file " + dir + File.separator + fname + "\n" + e.getMessage());
            e.printStackTrace();
        }
        finally {
            try {
                if (pw != null)
                    pw.close();
                if (fw != null)
                    fw.close();
            }
            catch (Exception ex) {
            }
        }
    }

    /** ***************************************************************
     */
    public void writeWordNetHyp() {

        FileWriter fw = null;
        PrintWriter pw = null;
        String dir = WordNet.baseDir;
        String fname = "Wn_hyp.pl";

        try {
            fw = new FileWriter(dir + File.separator + fname);
            pw = new PrintWriter(fw);

            if (relations.keySet().size() < 1)
                System.out.println("Error in WordNet.writeWordNetHyp(): No contents in relations");
            Iterator<String> it = relations.keySet().iterator();
            while (it.hasNext()) {
                String synset = (String) it.next();
                //System.out.println("INFO in WordNet.writeWordNetHyp(): synset: " + synset);
                ArrayList<AVPair> rels = relations.get(synset);
                if (rels == null || rels.size() < 1)
                    System.out.println("Error in WordNet.writeWordNetHyp(): No contents in rels for synset: " + synset);
                if (rels != null) {
                    Iterator<AVPair> it2 = rels.iterator();
                    while (it2.hasNext()) {
                        AVPair rel = it2.next();
                        if (rel.attribute.equals("hypernym"))
                            pw.println("hyp(" + synset + "," + rel.value + ").");
                    }
                }
            }
        }
        catch (Exception e) {
            System.out.println("Error in WordNet.writeWordNetHyp(): ");
            System.out.println("Error writing file " + dir + File.separator + fname + "\n" + e.getMessage());
            e.printStackTrace();
        }
        finally {
            try {
                if (pw != null)
                    pw.close();
                if (fw != null)
                    fw.close();
            }
            catch (Exception ex) {
            }
        }
    }

    /** ***************************************************************
     * Double any single quotes that appear.
     */
    public String processPrologString (String doc) {

        int start = 0;
        while (doc.indexOf("'",start) > -1) {
            int i = 0;
            i = doc.indexOf("'",start);
            //System.out.println("INFO in WordNet.processPrologString(): index: " + i + " string: " + doc);
            if (i == 0)
                doc = "''" + doc.substring(i+1);
            else
                doc = doc.substring(0,i) + "''" + doc.substring(i+1);
            start = i+2;
        }
        return doc;
    }

    /** ***************************************************************
     */
    public void writeWordNetG() {

        FileWriter fw = null;
        PrintWriter pw = null;
        String dir = WordNet.baseDir;
        String fname = "Wn_g.pl";

        try {
            fw = new FileWriter(dir + File.separator + fname);
            pw = new PrintWriter(fw);
            Iterator<String> it = nounDocumentationHash.keySet().iterator();
            while (it.hasNext()) {
                String synset = (String) it.next();
                String doc = (String) nounDocumentationHash.get(synset);
                doc = processPrologString(doc);
                pw.println("g(" + "1" + synset + ",'(" + doc + ")').");
            }
            it = verbDocumentationHash.keySet().iterator();
            while (it.hasNext()) {
                String synset = (String) it.next();
                String doc = (String) verbDocumentationHash.get(synset);
                doc = processPrologString(doc);
                pw.println("g(" + "2" + synset + ",'(" + doc + ")').");
            }
            it = adjectiveDocumentationHash.keySet().iterator();
            while (it.hasNext()) {
                String synset = (String) it.next();
                String doc = (String) adjectiveDocumentationHash.get(synset);
                doc = processPrologString(doc);
                pw.println("g(" + "3" + synset + ",'(" + doc + ")').");
            }
            it = adverbDocumentationHash.keySet().iterator();
            while (it.hasNext()) {
                String synset = (String) it.next();
                String doc = (String) adverbDocumentationHash.get(synset);
                doc = processPrologString(doc);
                pw.println("g(" + "4" + synset + ",'(" + doc + ")').");
            }
        }
        catch (Exception e) {
            System.out.println("Error in WordNet.writeWordNetG(): ");
            System.out.println("Error writing file " + dir + File.separator + fname + "\n" + e.getMessage());
            e.printStackTrace();
        }
        finally {
            try {
                if (pw != null)
                    pw.close();
                if (fw != null)
                    fw.close();
            }
            catch (Exception ex) {
            }
        }
    }

    /** ***************************************************************
     */
    public void writeWordNetProlog () throws IOException {

        writeWordNetS();
        writeWordNetHyp();
        writeWordNetG();
    }

    /** ***************************************************************
     * Generate a new 8 digit synset ID that doesn't have an existing hash
     */
    public String generateSynsetID(String l) {

        Integer intval = Integer.parseInt(l);
        intval++;
        return StringUtil.integerToPaddedString(intval);
    }

    /** ***************************************************************
     * Generate a new eight digit noun synset ID that doesn't have an existing hash
     */
    public String generateNounSynsetID() {

        maxNounSynsetID = generateSynsetID(maxNounSynsetID);
        return maxNounSynsetID;
    }

    /** ***************************************************************
     * Generate a new eight digit verb synset ID that doesn't have an existing hash
     */
    public String generateVerbSynsetID() {

        maxVerbSynsetID = generateSynsetID(maxVerbSynsetID);
        return maxVerbSynsetID;
    }
    
    /** ***************************************************************
     * Generate a new noun synset from a termFormat
     */
    public String nounSynsetFromTermFormat(String tf, String SUMOterm, KB kb) {
        
        String synsetID = generateNounSynsetID();
        ArrayList<Formula> forms = kb.askWithRestriction(0, "documentation", 1, SUMOterm);
        if (forms.size() > 0) {
            Formula f = forms.get(0);
            String doc = f.getStringArgument(3);
            if (StringUtil.emptyString(doc))
                doc = "no gloss";
            nounDocumentationHash.put(synsetID, doc);
        }
        nounSUMOHash.put(synsetID, SUMOterm + "=");
        HashSet<String> synsets = new HashSet<>();
        if (nounSynsetHash.containsKey(tf))
            synsets = nounSynsetHash.get(tf);
        synsets.add(synsetID);
        nounSynsetHash.put(tf,synsets);
        return synsetID;
    }

    /** ***************************************************************
     * Generate a new verb synset from a termFormat
     */
    public String verbSynsetFromTermFormat(String tf, String SUMOterm, KB kb) {
        
        String synsetID = generateVerbSynsetID();
        ArrayList<Formula> forms = kb.askWithRestriction(0, "documentation", 1, SUMOterm);
        if (forms.size() > 0) {
            Formula f = forms.get(0);
            String doc = f.getStringArgument(3);
            if (StringUtil.emptyString(doc))
                doc = "no gloss";
            verbDocumentationHash.put(synsetID, doc);
        }
        verbSUMOHash.put(synsetID, SUMOterm + "=");
        HashSet<String> synsets = new HashSet<>();
        if (verbSynsetHash.containsKey(tf))
            synsets = verbSynsetHash.get(tf);
        synsets.add(synsetID);
        verbSynsetHash.put(tf,synsets);
        return synsetID;
    }

    /** ***************************************************************
     */
    private String createNewSenseIndexKey(String base) {

        char num = '1';
        String key = base + num;
        while (senseIndex.keySet().contains(key) || reverseSenseIndex.values().contains(key)) {
            num++;
            key = base + num;
        }
        return key;
    }

    /** ***************************************************************
     * Generate a new synset from a termFormat statement
     * @param form is the entire termFormat statement
     * @param tf is the lexical item (word).  note that in the case of a multi-word
     *           lexical item it should already have had spaces replaced by
     *           underscores
     * @param SUMOterm is the SUMO term that the lexical item is mapped to
     */
    public void synsetFromTermFormat(Formula form, String tf, String SUMOterm, KB kb) {

        long millis = System.currentTimeMillis();
        //System.out.println("INFO in WordNet.synsetFromTermFormat(): " + tf);
        String synsetID = null;
        String pos = null;
        if (kb.kbCache.subclassOf(SUMOterm,"Process")) {  // like a verb
            pos = "2";
            synsetID = "2" + verbSynsetFromTermFormat(tf,SUMOterm,kb);
        }
        else { // like a noun
            pos = "1";
            synsetID = "1" + nounSynsetFromTermFormat(tf,SUMOterm,kb);
        }
        MapUtils.addToMap(ignoreCaseSynsetHash,tf.toUpperCase(),synsetID);
        //System.out.println("INFO in WordNet.synsetFromTermFormat(): " + tf.toUpperCase() + "," + synsetID);

        ArrayList<String> al = SUMOHash.get(SUMOterm);
        if (al == null)
            al = new ArrayList<String>();
        al.add(synsetID);
        SUMOHash.put(SUMOterm, al);
        
        ArrayList<String> words = synsetsToWords.get(synsetID); 
        if (words == null)
            words = new ArrayList<String>();
        words.add(tf);
        synsetsToWords.put(synsetID,words);
        
        String letterPOS = WordNetUtilities.posNumberToLetters(pos);

        String key = createNewSenseIndexKey(tf + "_" + letterPOS + "_");
        senseIndex.put(key,synsetID.substring(1)); // senseIndex requires un-prefixed synset #
        reverseSenseIndex.put(synsetID,key);
        ArrayList<String> keys = new ArrayList<String>();
        if (wordsToSenseKeys.containsKey(tf))
            keys = wordsToSenseKeys.get(tf);
        keys.add(key);
        //System.out.println("INFO in WordNet.synsetFromTermFormat(): add to wordsToSenseKeys: " + tf + ", " + keys);
        wordsToSenseKeys.put(tf,keys);

        // TODO: kind of a hack to give priority to any domain term, maybe make this a switchable option
        if (!form.sourceFile.equals("Merge.kif") && !form.sourceFile.equals("Mid-level-ontology.kif")) {
            //TreeSet<AVPair> senselist = wordFrequencies.get(tf);
            //if (senselist == null) {
                //senselist = new TreeSet<AVPair>();
                //wordFrequencies.put(tf,senselist);
                caseMap.put(tf.toUpperCase(),tf);
                //System.out.println("INFO in WordNet.synsetFromTermFormat(): " +
                //        tf.toUpperCase() + " : " + tf);
            //}
            AVPair avp = new AVPair();
            avp.value = synsetID;
            avp.attribute = "99999"; // bigger than any word frequency in Brown
            //senselist.add(avp);
            addToWordFreq(tf,avp);
        }
        //System.out.println("INFO in WordNet.synsetFromTermFormat(): term, sensekey, synset, SUMOterm: " +
        //        tf + ", " + key + ", " + synsetID + ", " + SUMOterm);
        //System.out.println("WordNet.synsetFromTermFormat(): millis: " + (System.currentTimeMillis() - millis));
    }
    
    /** ***************************************************************
     * Generate a new synset from a termFormat
     */
    public void termFormatsToSynsets(KB kb) {

        long millis = System.currentTimeMillis();
        if (kb == null) {
            System.out.println("INFO in WordNet.termFormatsToSynsets(): KB is null");
            return;
        }
        System.out.println("INFO in WordNet.termFormatsToSynsets(): changing origMaxNounSynsetID from: " +
                origMaxNounSynsetID + " to: " + maxNounSynsetID);
        if (!StringUtil.emptyString(maxNounSynsetID))
            origMaxNounSynsetID = maxNounSynsetID;
        if (!StringUtil.emptyString(maxVerbSynsetID))
            origMaxVerbSynsetID = maxVerbSynsetID;

        int counter = 0;
        int totalcount = 0;
        //System.out.println("INFO in WordNet.termFormatsToSynsets()");
        long millis2 = System.currentTimeMillis();
        ArrayList<Formula> forms = kb.ask("arg", 0, "termFormat");
        System.out.println("WordNet.termFormatsToSynsets(): just the ask in seconds: " + (System.currentTimeMillis() - millis) / 1000);
        System.out.println("WordNet.termFormatsToSynsets(): termFormats: " + forms.size());
        for (Formula form : forms) {
            //System.out.println("WordNet.termFormatsToSynsets(): form: " + form);
            ArrayList<String> args = form.argumentsToArrayListString(0);
            //System.out.println("WordNet.termFormatsToSynsets(): args: " + args);
            if (args == null || args.size() < 2)
                continue;
            counter++;
            if (counter == 1000) {
                System.out.print('.');
                totalcount = totalcount + counter;
                counter = 0;
            }
            if (args.size() != 4) {
                String errStr = "Error in WordNet.termFormatsToSynsets(): wrong number of arguments: " +
                    form.toString();
                kb.errors.add(errStr);
                continue;
            }
            String SUMOterm = args.get(2);
            //if (SUMOterm.equals("FourStrokeEngine"))
            //    System.out.println("INFO in WordNet.termFormatsToSynsets(): formula: " + form);
            String tf = StringUtil.removeEnclosingQuotes(args.get(3));
            tf = tf.replaceAll(" ", "_");
            if (tf.indexOf("_") > 0) {
                //System.out.println("INFO in WordNet.termFormatsToSynsets() multiword:" + tf + " SUMO: " + SUMOterm);
                multiWords.addMultiWord(tf);
            }
            synsetFromTermFormat(form,tf,SUMOterm,kb);
        }
        System.out.println("\nINFO in WordNet.termFormatsToSynsets(): result (orig,max): " +
                origMaxNounSynsetID + " and: " + maxNounSynsetID);
        System.out.println("WordNet.termFormatsToSynsets(): seconds: " + (System.currentTimeMillis() - millis) / 1000);
    }
    
    /** ***************************************************************
     *  A method used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void testWordFreq () {

        String word = "run";
        //String word = "be";
        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ex) {
            System.out.println("Error in WordNet.testWordFreq(): ");
            System.out.println(ex.getMessage());
        }
        System.out.println("INFO in WordNet.testWordFreq(): done initializing");
        WordNet.initOnce();
        System.out.println("Word frequencies: " + WordNet.wn.wordFrequencies.get(word));
        System.out.println("Best word frequency: " + WordNet.wn.wordFrequencies.get(word).last());
        System.out.println("Best word frequency value: " + WordNet.wn.wordFrequencies.get(word).last().value);
        // HashMap<String,TreeSet<AVPair>> wordFrequencies
        System.out.println("Best word frequency value sense index: " + 
                WordNet.wn.senseIndex.get(WordNet.wn.wordFrequencies.get(word).last().value));
        System.out.println("Best default sense w/POS: " + WSD.getBestDefaultSense(word,2));
        System.out.println("Best default sense w/o POS: " + WSD.getBestDefaultSense(word));
        System.out.println("Best default sense w/o POS sense index: " + 
                WordNet.wn.reverseSenseIndex.get(WSD.getBestDefaultSense(word,2)));
    }
    
    /** ***************************************************************
     *  A method used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void testProcessPointers() {

        String line = "01522276 35 v 04 wind 6 wrap 2 roll 0 twine 3 013 @ 01850315 v 0000 + 03150232 n 0303 + 07441619 n 0303 ^ 00435688 v 0301 ^ 00435688 v 0202 + 04586421 n 0101 + 10781984 n 0101 ! 01523654 v 0101 ~ 01522878 v 0000 ~ 01523105 v 0000 ~ 01523270 v 0000 ~ 01523401 v 0000 ~ 01523986 v 0000 01 + 21 00 | arrange or or coil around; \"roll your hair around your finger\"; \"Twine the thread around the spool\"; \"She wrapped her arms around the child\" &%Motion+";
        // 10: p = Pattern.compile("^([0-9]{8})([^\\|]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$");
        WordNet.wn.compileRegexPatterns();
        Matcher m = regexPatterns[10].matcher(line);
        if (m.matches()) {
            //verbDocumentationHash.put(m.group(1),m.group(3));
            //addSUMOMapping(m.group(4),"2" + m.group(1));
            WordNet.wn.processPointers("2" + m.group(1),m.group(2));
        }
        System.out.println("Info in WordNet.testProcessPointers(): synset: " + WordNet.wn.verbSynsetHash.get("roll"));
    }

    /** ***************************************************************
     */
    public static void checkWordsToSenses() {

        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ioe ) {
            System.out.println("Error in WordNet.checkWordsToSenses(): ");
            System.out.println(ioe.getMessage());
        }
        System.out.println("run " + wn.wordsToSenseKeys.get("run"));
        System.out.println("TV " + wn.wordsToSenseKeys.get("TV"));
        System.out.println("tv " + wn.wordsToSenseKeys.get("tv"));
        System.out.println("106277280 " + wn.synsetsToWords.get("106277280"));
        System.out.println("106277280 " + wn.reverseSenseIndex.get("106277280"));
        System.out.println("court " + wn.wordsToSenseKeys.get("court"));
        System.out.println("state " + wn.wordsToSenseKeys.get("state"));
        System.out.println("labor " + wn.wordsToSenseKeys.get("labor"));
        System.out.println("phase " + wn.wordsToSenseKeys.get("phase"));
        System.out.println("craft " + wn.wordsToSenseKeys.get("craft"));
    }

    /** ***************************************************************
     */
    public static void getEntailments() {

        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ioe ) {
            System.out.println("Error in WordNet.getEntailments(): ");
            System.out.println(ioe.getMessage());
        }
        for (String s : wn.relations.keySet()) {
            Collection<AVPair> values = wn.relations.get(s);
            for (AVPair avp : values) {
                if (avp.attribute.equals("entailment")) {
                    String s1 = wn.synsetsToWords.get(s).toString();
                    String s2 = wn.synsetsToWords.get(avp.value).toString();
                    String sumo1 = wn.getSUMOMapping(s);
                    String sumo2 = wn.getSUMOMapping(avp.value);
                    System.out.println(s + " " + s1 + " " + sumo1 + " " +
                            " entails " + avp.value + " " + s2 + " " + sumo2);
                }
            }
        }
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("Semantic Rewriting with SUMO, Sigma and E");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -w \"...\" - find a word in WordNet");
    }

    /** ***************************************************************
     *  A main method, used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void main (String[] args) {
        
        //testWordFreq();    
        //checkWordsToSenses();
        //getEntailments();
        System.out.println("INFO in WordNet.main()");
        KBmanager.getMgr().initializeOnce();
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        if (args != null && args.length > 1 && args[0].equals("-w")) {
            String result = wn.page(StringUtil.removeEnclosingQuotes(args[1]),0,kbName,"","");
            System.out.println(StringUtil.removeHTML(result));
        }
        else
            showHelp();
    }
}
