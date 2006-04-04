
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

import java.io.*;
import java.util.*;
import java.util.regex.*;

/** ***************************************************************
*  @author Adam Pease
*/

public class WordNetUtilities {

    /** ***************************************************************
    *  Get a SUMO term minus its &% prefix and one character mapping
    * suffix.
    */
    public static String getBareSUMOTerm (String term) {

        if (term != null && term != "") 
            return term.substring(2,term.length()-1);
        else
            return "";
    }

    /** *************************************************************** 
     */
    private static String removeTermPrefixes (String formula) {

        return formula.replaceAll("&%","");
    }

    /** ***************************************************************
    *  Get a SUMO term mapping suffix.
    */
    public static char getSUMOMappingSuffix (String term) {

        if (term != null && term != "") 
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
        }
        return '1';
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
    public static boolean substTest(String result, String match, String subst, Hashtable hash) {

        Pattern p = Pattern.compile(match);
        Matcher m = p.matcher(result);
        if (m.find()) {
            result = m.replaceFirst(subst);
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
    public static String formatWords(TreeMap words) {

        StringBuffer result = new StringBuffer();
        int count = 0;
        Iterator it = words.keySet().iterator();
        while (it.hasNext() && count < 50) {
            String word = (String) it.next();
            String synset = (String) words.get(word);
            result.append("<a href=\"WordNet.jsp?word=");
            result.append(word); 
            result.append("&POS=");
            result.append(synset.substring(0,1));
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
     * Routine called by mergeUpdates which does the bulk of the work.
     * Should not be called during normal interactive running of Sigma.
     */
    private static void processMergers (HashMap hm, String fileName, String pattern, String posNum) throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null; 
        try {
            KB kb = KBmanager.getMgr().getKB("SUMO");
            fw = new FileWriter(KBmanager.getMgr().getPref("kbDir") + File.separator + fileName + "-new.txt");
            pw = new PrintWriter(fw);

            FileReader r = new FileReader(KBmanager.getMgr().getPref("kbDir") + File.separator + fileName + ".txt");
            LineNumberReader lr = new LineNumberReader(r);
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
                    String newTerm = (String) hm.get(synset);
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
        }
    }

    /** ***************************************************************
     * Read in a file with a nine-digit synset number followed by a space
     * and a SUMO term.  If the term is more specific than the current
     * mapping for that synset, replace the old term. This is a utility
     * that is not normally called from the interactive Sigma system.
     */
    public static void mergeUpdates () throws IOException {

        HashMap hm = new HashMap();

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
     * This is a utility routine that should not be called during 
     * normal Sigma operation.  It does most of the actual work for 
     * deduceMissingLinks()
     */
    public static void processMissingLinks(String fileName, String pattern, String posNum) throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null;
        try {
            KB kb = KBmanager.getMgr().getKB("SUMO");
            fw = new FileWriter(KBmanager.getMgr().getPref("kbDir") + File.separator + fileName + "-new.txt");
            pw = new PrintWriter(fw);

            FileReader r = new FileReader(KBmanager.getMgr().getPref("kbDir") + File.separator + fileName + ".txt");
            LineNumberReader lr = new LineNumberReader(r);
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
                        String newTerm = "";
                        String mapType = "";
                        if (newTerm != null && newTerm != "") {              
                            pw.println(m.group(1) + m.group(2) + "| " + m.group(3) + " &%" + newTerm + mapType);
                            System.out.println("INFO in WordNet.processMergers(): synset, newterm: " + 
                                               synset + " " + " " + newTerm);
                        }
                        else
                            pw.println(line.trim());
                    }
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
        String pattern = "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s*$";
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
    *  A main method, used only for testing.  It should not be called
    *  during normal operation.
    */
    public static void main (String[] args) {

       try {
            KBmanager.getMgr().initializeOnce();
            WordNet.initOnce();
            WordNetUtilities.deduceMissingLinks();
        }
        catch (IOException ioe) {
            System.out.println("Error in WordNet.main(): IOException: " + ioe.getMessage());
        } 

    }
}

