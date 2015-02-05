
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

package com.articulate.sigma;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.articulate.sigma.KB;

/** ***************************************************************
 *  @author Adam Pease
 */

public class WordNetUtilities {

    /** POS-prefixed mappings from a new synset number to the old
     *  one. */
    HashMap<String,String> mappings = new HashMap<String,String>();

    public static int TPTPidCounter = 1;
    
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
     * Extract the POS from a word_POS_num sense key.  Should be an
     * alpha key, such as "VB".
     */
    public static String getPOSfromKey (String senseKey) {

        int lastUS = senseKey.lastIndexOf("_");
        return senseKey.substring(lastUS-2,lastUS);
    }

    /** ***************************************************************
     * Extract the POS from a word_POS_num sense key
     */
    public static String getWordFromKey (String senseKey) {

        int lastUS = senseKey.lastIndexOf("_");
        return senseKey.substring(0,lastUS-3);
    }

    /** ***************************************************************
     */
    public static String removeTermPrefixes (String formula) {

        return formula.replaceAll("&%","");
    }

    /** ***************************************************************
     * Convert a list of Terms in the format "&%term1 &%term2" to an ArrayList
     * of bare term Strings
     */
    public static ArrayList<String> convertTermList (String termList) {

        ArrayList<String> result = new ArrayList<String>();
        String[] list = termList.split(" ");
        for (int i = 0; i < list.length; i++)
            result.add(getBareSUMOTerm(list[i]));
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
     * Convert a part of speech number to the two letter format used by
     * the WordNet sense index code.  Defaults to noun "NN".
     */
    public static String posNumberToLetters(String pos) {

        if (pos.equalsIgnoreCase("1")) return "NN";
        if (pos.equalsIgnoreCase("2")) return "VB";
        if (pos.equalsIgnoreCase("3")) return "JJ";
        if (pos.equalsIgnoreCase("4")) return "RB";
        if (pos.equalsIgnoreCase("5")) return "JJ";
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
    public static boolean substTest(String result, String match, String subst, Hashtable<String,String> hash) {

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
    private static void processMergers (HashMap<String,String> hm, String fileName, String pattern, String posNum) throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null;
        LineNumberReader lr = null;
        try {
            KB kb = KBmanager.getMgr().getKB("SUMO");
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
    public static void mergeUpdates () throws IOException {

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
    public void updateWNversionProcess(String fileName, String pattern, String posNum) throws IOException {

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
     */
    public void updateWNversionReading(String fileName, String pattern, String posNum) throws IOException {

        LineNumberReader lr = null;
        try {
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
    public void updateWNversion() throws IOException {

        String fileName = "wn30-21.noun";
        String pattern = "^(\\d+) (\\d+) .*$";
        String posNum = "1";
        updateWNversionReading(fileName,pattern,posNum);
        fileName = "wn30-21.verb";
        pattern = "^(\\d+) (\\d+) .*$";
        posNum = "2";
        updateWNversionReading(fileName,pattern,posNum);
        fileName = "wn30-21.adj";
        pattern = "^(\\d+) (\\d+) .*$";
        posNum = "3";
        updateWNversionReading(fileName,pattern,posNum);
        fileName = "wn30-21.adv";
        pattern = "^(\\d+) (\\d+) .*$";
        posNum = "4";
        updateWNversionReading(fileName,pattern,posNum);

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
     * Take a file of <id>tab<timestamp>tab<string> and calculate
     * the average Levenshtein distance for each ID.
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

        ArrayList<String> VerbFrames = new ArrayList<String>(Arrays.asList("Something ----s",
          "Somebody ----s",
          "It is ----ing",
          "Something is ----ing PP",
          "Something ----s something Adjective/Noun",
          "Something ----s Adjective/Noun",
          "Somebody ----s Adjective",
          "Somebody ----s something",
          "Somebody ----s somebody",
          "Something ----s somebody",
          "Something ----s something",
          "Something ----s to somebody",
          "Somebody ----s on something",
          "Somebody ----s somebody something",
          "Somebody ----s something to somebody",
          "Somebody ----s something from somebody",
          "Somebody ----s somebody with something",
          "Somebody ----s somebody of something",
          "Somebody ----s something on somebody",
          "Somebody ----s somebody PP",
          "Somebody ----s something PP",
          "Somebody ----s PP",
          "Somebody's (body part) ----s",
          "Somebody ----s somebody to INFINITIVE",
          "Somebody ----s somebody INFINITIVE",
          "Somebody ----s that CLAUSE",
          "Somebody ----s to somebody",
          "Somebody ----s to INFINITIVE",
          "Somebody ----s whether INFINITIVE",
          "Somebody ----s somebody into V-ing something",
          "Somebody ----s something with something",
          "Somebody ----s INFINITIVE",
          "Somebody ----s VERB-ing",
          "It ----s that CLAUSE",
          "Something ----s INFINITIVE"));

        for (int i = 0; i < VerbFrames.size(); i ++) {
            String frame = VerbFrames.get(i);
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
     * Write OWL format for SUMO-WordNet mappings.
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
        ArrayList<String> senses = WordNet.wn.wordsToSenses.get(word);
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

        Iterator<String> it = WordNet.wn.wordsToSenses.keySet().iterator();
        while (it.hasNext()) {
            String word = (String) it.next();
            writeTPTPOneWordToSenses(pw,word);
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
     *  A main method, used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void main (String[] args) {

        try {
	        KBmanager.getMgr().initializeOnce();
	        //extractMeronyms();
            FileWriter fw = new FileWriter("WNout.tptp");
            PrintWriter pw = new PrintWriter(fw);
            pw.flush();
	        writeTPTPWordNet(pw);
            pw.flush();
        }
        catch (Exception e) {
            System.out.println("Error in WordNetUtilities.main(): Exception: " + e.getMessage());
        }

    }
}

