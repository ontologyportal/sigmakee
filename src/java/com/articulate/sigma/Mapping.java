
package com.articulate.sigma;

import com.articulate.sigma.trans.OWLtranslator;
import com.articulate.sigma.utils.StringUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** This code is copyright Articulate Software (c) 2004.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. See also http://sigmakee.sourceforge.net

This class maps ontologies. It includes embedded subclasses that
implement specific mapping heuristics.

This class also includes utilities for converting other
ad-hoc formats to KIF
*/
public class Mapping {

    public static Map<String,Map<Integer,String>> mappings = new TreeMap<>();
    public static char termSeparator = '!';

    /** *************************************************************
     *  Write synonymousExternalConcept expressions for term pairs
     *  given in cbset.  They are strings of the form
     *  [checkbox|subcheckbox]_[T_]name1-name2
     *
     *  There's a known bug when ontology terms contain dashes.
     *
     *  @return error messages if necessary
     */
    public static String writeEquivalences(Set<String> cbset, String kbname1, String kbname2) throws IOException {

        System.out.println("INFO in Mapping.writeEquivalences(): size: " + cbset.size());
        String dir = KBmanager.getMgr().getPref("baseDir");
        String filename = dir + File.separator + kbname1 + "-" + kbname2 + "-links";

        if (mappings.keySet().size() < 1)
            return "Error: No mappings found";

        File f = new File(filename + ".kif");
        int fileCounter = 0;
        while (f.exists()) {
            fileCounter++;
            f = new File(filename + fileCounter + ".kif");
        }
        if (fileCounter == 0)
            filename = filename + ".kif";
        else
            filename = filename + fileCounter + ".kif";

        try (Writer fw = new FileWriter(filename);
             PrintWriter pw = new PrintWriter(fw)) {
            Iterator<String> it = cbset.iterator();
            String st, term1, term2;
            boolean subcheckbox;
            while (it.hasNext()) {
                st = it.next();
                subcheckbox = false;
                if (st.startsWith("sub_checkbox_")) {
                    st = st.substring(13);
                    subcheckbox = true;
                }
                else {
                    if (st.startsWith ("checkbox_"))
                        st = st.substring(9);
                    else
                        return "Error in Mapping.writeEquivalences(): malformed string " + st;
                }

                if (st.startsWith ("T_"))
                    st = st.substring(2);
                int i = st.indexOf(termSeparator);
                if (i < 0)
                    return "Error in Mapping.writeEquivalences(): malformed string (no '" + termSeparator + "') " + st;
                term1 = st.substring(0,i);
                term2 = st.substring(i+1);
                if (!subcheckbox)
                    pw.println("(synonymousExternalConcept \"" + term2 +
                               "\" " + term1 + " " + kbname2 + ")");
                else
                    pw.println("(subsumedExternalConcept \"" + term2 +
                               "\" " + term1 + " " + kbname2 + ")");

            }
        }
        catch (IOException e) {
            throw new IOException("Error writing file " + filename + "\n" + e.getMessage());
        }
        return "Wrote: " + filename;
    }

    /** *************************************************************
     *  rename terms in KB kbname2 to conform to names in kbname1
     *  @return error messages if necessary
     */
    public static String merge(Set<String> cbset, String kbname1, String kbname2) {

        System.out.println("INFO in Mapping.merge()");
        if (mappings.keySet().size() < 1)
            return "Error: No mappings found";

        KB kb1 = KBmanager.getMgr().getKB(kbname1);
        KB kb2 = KBmanager.getMgr().getKB(kbname2);
        Map<Integer,String> value;
        Iterator<Integer> it2;
        Integer score;
        String term2, topScoreFlag, cbName, subName;
        int counter;
        for (String term1 : mappings.keySet()) {
            value = mappings.get(term1);
            // System.out.println("INFO in Mapping.merge(): outer loop, examining " + term1);
            it2 = value.keySet().iterator();
            counter = 0;
            while (it2.hasNext()) {
                counter++;
                score = it2.next();
                term2 = value.get(score);
                // System.out.println("INFO in Mapping.merge(): inner loop, examining " + term2);
                topScoreFlag = "";
                if (counter == 1)
                    topScoreFlag = "T_";
                cbName = "checkbox_" + topScoreFlag + term1 + termSeparator + term2;
                subName = "sub_checkbox_" + topScoreFlag + term1 + termSeparator + term2;
                if (cbset.contains(cbName) && !term2.equals(term1))
                    kb2.rename(term2,term1);
                if (cbset.contains(subName)) {
                    if (kb2.isInstance(term2)) {
                        kb2.tell("(instance " + term2 + " " + term1 + ")");
                        System.out.println("(instance " + term2 + " " + term1 + ")");
                    }
                    else {
                        kb2.tell("(subclass " + term2 + " " + term1 + ")");
                        System.out.println("(subclass " + term2 + " " + term1 + ")");
                    }
                }
            }
        }
        String dir = (String) KBmanager.getMgr().getPref("baseDir");
        String filename = dir + File.separator + kbname2 + "-merged-" + kbname1;
        try {
            File f = new File(filename + ".kif");
            counter = 0;
            while (f.exists()) {
                counter++;
                f = new File(filename + counter + ".kif");
            }
            if (counter == 0)
                filename = filename + ".kif";
            else
                filename = filename + counter + ".kif";
            kb2.writeFile(filename);
            kb1.addConstituent(filename);
            KBmanager.getMgr().removeKB(kbname2);
        }
        catch (IOException e) {
            return "Error writing file " + filename + "\n" + e.getMessage();
        }
        return "Successful renaming of terms in " + kbname2 + " to those in " + kbname1;
    }

    /** *************************************************************
     *  Convert a YAGO file into KIF
     */
    public static void convertYAGO(String file, String relName) throws IOException {

        File f = new File(file);
        if (f == null) {
            System.out.println( "INFO in convertYAGO(): "
                                + "The file " + file + " does not exist" );
            return;
        }
        try (Reader r = new FileReader(f);
            LineNumberReader lr = new LineNumberReader(r)) {
            String line, term1, term2;
            int tab1, tab2, tab3;
            while ((line = lr.readLine()) != null) {
                line = line.trim();
                if (line != null && line.length() > 0) {
                    tab1 = line.indexOf("\t");
                    tab2 = line.indexOf("\t",tab1+1);
                    tab3 = line.indexOf("\t",tab2+1);
                    term1 = line.substring(tab1+1,tab2);
                    term2 = line.substring(tab2+1,tab3);
                    term1 = StringUtil.stringToKIFid(term1);
                    term2 = StringUtil.stringToKIFid(term2);
                    System.out.println("(" + relName + " " + term1 + " " + term2 + ")");
                }
            }
        }
    }

    /** *************************************************************
    *   Get the termFormat label for a term.  Return only the first
    *   such label.  Return null if no label.
     */
    public static String getTermFormat(KB kb, String term) {

        if (kb != null) {
            List<Formula> al = kb.askWithRestriction(0,"termFormat",2,term);
            if (al != null && !al.isEmpty()) {
                Formula f = (Formula) al.get(0);
                String t = f.getStringArgument(3);
                t = OWLtranslator.removeQuotes(t);
                return t;
            }
        }
        return null;
    }

    /** *************************************************************
    *   @return the minimum of two ints
     */
    private static int min(int n1, int n2) {

        if (n1<n2)
            return n1;
        else
            return n2;
    }


    /** *************************************************************
     */
    private static int stringMatch(String t1, String t2, String matchMethod) {

        if (matchMethod.equals("JaroWinkler"))
            return getJaroWinklerDistance(t1, t2);
        if (matchMethod.equals("Levenshtein"))
            return getLevenshteinDistance(t1, t2);
        return getSubstringDistance(t1,t2);
    }

    /** *************************************************************
     * Map ontologies through 4 methods:
     * (1) identical term names
     * (2) substrings of term names are equal
     * (3) terms align to words in the same WordNet synset
     * (4) extra "points" for having terms that align with the same
     * structural arrangement
     *
     */
    public static void mapOntologies(String kbName1, String kbName2, int threshold, String matchMethod) {

        System.out.println("INFO in Mapping.mapOntologies()");

        long t1 = System.currentTimeMillis();
        int mapCount = 0;
        if (!matchMethod.equals("JaroWinkler") &&
                !matchMethod.equals("Levenshtein") &&
                !matchMethod.equals("Substring")) {
            matchMethod = "Substring";
            System.out.println("Error in Mapping.mapOntologies(): Invalid match method " +
                    matchMethod + ". Defaulting to substring match.");
        }

        Map result = new TreeMap();
        int counter = 0;
        KB kb1,kb2;
        kb1 = KBmanager.getMgr().getKB(kbName1);
        kb2 = KBmanager.getMgr().getKB(kbName2);
        int totalCandidates = kb1.getTerms().size() * kb2.getTerms().size();
        System.out.println("Time estimate to map: " + totalCandidates / 100 + " seconds.");
        if (kb1 != null && kb2 != null) {
            synchronized (kb1.getTerms()) {
                synchronized (kb2.getTerms()) {
                    Iterator it1 = kb1.getTerms().iterator();
                    String term1, normTerm1, normLabel1, normTerm2, normLabel2;
                    Map tm;
                    int score;
                    while (it1.hasNext()) {
                        counter++;
                        if (counter > 100) {
                            System.out.print(".");
                            counter = 0;
                        }
                        term1 = (String) it1.next();
                        if (isValidTerm(term1)) {
                            normTerm1 = normalize(term1);
                            normLabel1 = normalize(getTermFormat(kb1,term1));
                            tm = (TreeMap) result.get(term1);
                            if (tm == null)
                                tm = new TreeMap();
                            for (String term2 : kb2.getTerms()) {
                                if (isValidTerm(term2)) {
                                    normTerm2 = normalize(term2);
                                    normLabel2 = normalize(getTermFormat(kb2,term2));
                                    score = Integer.MAX_VALUE;
                                    score = min(score,stringMatch(normTerm1, normTerm2,matchMethod));
                                    //System.out.println(normTerm1 + " " + normTerm2);
                                    if (normLabel1 != null && isValidTerm(normLabel1))
                                        score = min(score,stringMatch(normLabel1,normTerm2,matchMethod));
                                    if (normLabel2 != null && isValidTerm(normLabel2))
                                        score = min(score,stringMatch(normTerm1, normLabel2,matchMethod));
                                    if (normLabel1 != null && normLabel2 != null &&
                                            isValidTerm(normLabel1) && isValidTerm(normLabel2))
                                        score = min(score,stringMatch(normLabel1, normLabel2,matchMethod));
                                    if (score > 0 && score < Integer.MAX_VALUE) {
                                        if (score < threshold) {
                                            tm.put(score, term2);
                                            mapCount++;
                                        }
                                    }
                                }
                            }
                            if (!tm.keySet().isEmpty())
                                result.put(term1,tm);
                        }
                    }
                }
            }
        }
        else {
            if (kb1 == null)
                System.out.println(kbName1 + " not found<P>\n");
            if (kb2 == null)
                System.out.println(kbName2 + " not found<P>\n");
        }
        System.out.println();
        System.out.println(totalCandidates + " " + " possible mappings checked with " +
                mapCount + " mappings found in "
                + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");
        mappings = result;
    }

    /** *************************************************************
     *   check whether a term is valid (worthy of being compared)
     */
    public static boolean isValidTerm(String term) {

        return term.length() > 2 && !Formula.isLogicalOperator(term);
    }

    /** *************************************************************
     *   Normalize a string by replacing all non-letter, non-digit
     *   characters with spaces, adding spaces on capitalization
     *   boundaries, and then converting to lower case
     */
    public static String normalize(String s) {

        if (s == null || s.length() < 1)
            return null;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if ((Character.isLetter(s.charAt(i)) && Character.isLowerCase(s.charAt(i))) ||
                Character.isDigit(s.charAt(i)))
                result.append(s.charAt(i));
            else {
                if (Character.isLetter(s.charAt(i)) && Character.isUpperCase(s.charAt(i))) {
                    if (result.length() > 0 && result.charAt(result.length()-1) != ' ')
                        result.append(" ");
                    result.append(Character.toLowerCase(s.charAt(i)));
                }
                else
                    if (result.length() > 0 && result.charAt(result.length()-1) != ' ')
                        result.append(" ");
            }
        }
        return result.toString();
    }

    /** *************************************************************
     *  Substring Mapping Method: returns 1 if the two strings
     *  are identical, scores >1 if one string is a substring of
     *  the other, and Integer.MAX_VALUE if there is no substring
     *  match
     *
     *  This approach is based on:
     *  John Li, "LOM: A Lexicon-based Ontology Mapping Tool",
     *  Proceedings of the Performance Metrics for Intelligent
     *  Systems  (PerMIS.'04), 2004.
     *
     *  *** This is not yet fully implemented here ***
     */
    public static int getSubstringDistance(String term1, String term2) {

        if (term1.equals(term2))
            return 1;
        else if (term1.contains(term2))
            return term1.indexOf(term2) +
                    (term1.length() - term2.length());
        else if (term2.contains(term1))
            return term2.indexOf(term1) + (term2.length() - term1.length());
        else
            return Integer.MAX_VALUE;
    }


    /** *************************************************************
     */
    private static int minimum(int a, int b, int c) {
        int ans = a;
        if (b < ans) ans = b;
        if (c < ans) ans = c;
        return ans;
    }

    /** *************************************************************
     *  LevenshteinDistance(char s[1..m], char t[1..n])
     *  courtesy of Wikipedia
     *  http://en.wikipedia.org/wiki/Levenshtein_distance
     *  int LevenshteinDistance(char s[1..m], char t[1..n])
     */
    public static int getLevenshteinDistance(String s, String t) {

        int m = s.length();
        int n = t.length();
        // d is a table with m+1 rows and n+1 columns
        int[][] d = new int[m][n];

        for (int i = 0; i < m; i++)
            d[i][0] = i; // deletion
        for (int j = 0; j < n; j++)
            d[0][j] = j; // insertion

        for (int j = 1; j < n; j++) {
            for (int i = 1; i < m; i++) {
                if (s.charAt(i) == t.charAt(j))
                    d[i][j] = d[i-1][j-1];
                else
                    d[i][j] = minimum(d[i-1][j] + 1, d[i][j-1] + 1, d[i-1][j-1] + 1);
            }                      // deletion,      insertion,     substitution
        }
/**
        int result = 0;
        for (int j = 1; j < n; j++) {
            int min = Integer.MAX_VALUE;
            for (int i = 1; i < m; i++) {
                if (d[i][j] < min)
                    min = d[i][j];
            }
            result =+ min - 1;
        }
     *  */
        return d[m-1][n-1];
     }

    /** *************************************************************
     *  Jaro-Winkler Mapping Method
     *  implemented by Gerard de Melo
     */
    public static int getJaroWinklerDistance(String s1, String s2) {

        int SCALING_FACTOR = 100;
        int winklerMaxPrefixLen = 4;
        double winklerPrefixWeight = 0.1;

        int len1 = s1.length();
        int len2 = s2.length();
        if (len1 == 0 || len2 == 0)
            return SCALING_FACTOR;

        // without loss of generality assume s1 is longer
        if (len1 < len2) {
            String t = s1;
            s1 = s2;
            s2 = t;
            len1 = len2;
            len2 = s2.length();
        }

        // count and flag the matched pairs
        int maxDistance = (len1 >= 4) ? (int) Math.floor(len1 / 2) - 1 : 0;
        boolean[] s1Matches = new boolean[len1]; // initialized to false
        boolean[] s2Matches = new boolean[len2]; // initialized to false
        int nMatches = 0, jStart, jEnd;
        char c;

        for (int i = 0; i < len2; i++) { // index in s2
            c = s2.charAt(i);
            jStart = (i > maxDistance) ? i - maxDistance : 0;
            jEnd = i + maxDistance + 1;
            if (jEnd > len1)
                jEnd = len1;
            for (int j = jStart; j < jEnd; j++) { // possible matching positions within s1
                if (!s1Matches[j] && c == s1.charAt(j)) {
                    s1Matches[j] = true;
                    s2Matches[i] = true;
                    nMatches++;
                    break;
                }
            }
        }

        if (nMatches == 0)
            return SCALING_FACTOR;

        // count transpositions
        int nTranspositions = 0;
        int k = 0;
        for (int i = 0; i < len2; i++) // index in s2
            if (s2Matches[i]) {
                int j;
                for (j = k; j < len1; j++)
                    if (s1Matches[j]) {
                        k = j + 1;
                        break;
                    }
                if (s2.charAt(i) != s1.charAt(j))
                    nTranspositions++;
            }
        int halfTranspositions = nTranspositions / 2;

        double jaroScore = ((double) nMatches / len1
                           +(double) nMatches / len2
                           +(double) (nMatches - halfTranspositions) / nMatches)
                           / 3.0;

        // Winkler bias
        int cMaxPrefixLen = winklerMaxPrefixLen;
        if (len1 < cMaxPrefixLen)
            cMaxPrefixLen = len1;
        if (len2 < cMaxPrefixLen)
            cMaxPrefixLen = len2;
        int l = 0;
        while (l < cMaxPrefixLen)
            if (s1.charAt(l) == s2.charAt(l))
                l++;
            else
                break;
        double jaroWinklerScore = jaroScore + l * winklerPrefixWeight * (1.0 - jaroScore);

        // return as a distance value such that larger
        // values indicate greater distances
        return (int) (SCALING_FACTOR * (1.0 - jaroWinklerScore));
    }

    /** *************************************************************
     * A test method.
     */
    private static void timingTest() {

        String s1 = normalize("sitting");
        String s2 = normalize("kitten");
        String s3 = normalize("arm");
        String s4 = normalize("Arm");
        String s5 = normalize("alarm");
        String s6 = normalize("Arm");
        String s7 = normalize("farm");
        String s8 = normalize("Armory");
        String s9 = normalize("hiccup");
        String s10 = normalize("Armory");
        String s11 = normalize("isSubclassOf");
        String s12 = normalize("subclass");
        String s13 = normalize("subclassOf");
        String s14 = normalize("subclass");

        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            getJaroWinklerDistance(s1,s2);
            getJaroWinklerDistance(s3,s4);
            getJaroWinklerDistance(s5,s6);
            getJaroWinklerDistance(s7,s8);
            getJaroWinklerDistance(s9,s10);
            getJaroWinklerDistance(s11,s12);
            getJaroWinklerDistance(s13,s14);
        }
        System.out.println("Jaro-Winkler: " + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");

        t1 = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            getLevenshteinDistance(s1,s2);
            getLevenshteinDistance(s3,s4);
            getLevenshteinDistance(s5,s6);
            getLevenshteinDistance(s7,s8);
            getLevenshteinDistance(s9,s10);
            getLevenshteinDistance(s11,s12);
            getLevenshteinDistance(s13,s14);
        }
        System.out.println("Levenshtein: " + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");

        t1 = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            getSubstringDistance(s1,s2);
            getSubstringDistance(s3,s4);
            getSubstringDistance(s5,s6);
            getSubstringDistance(s7,s8);
            getSubstringDistance(s9,s10);
            getSubstringDistance(s11,s12);
            getSubstringDistance(s13,s14);
        }
        System.out.println("Substring: " + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");
    }

    /** *************************************************************
     * A test method.
     */
    private static void printTest(String s1, String s2) {

        System.out.println("\"" + s1 + "\" \"" + s2 + "\"");
        s1 = normalize(s1);
        s2 = normalize(s2);
        System.out.println("\"" + s1 + "\" \"" + s2 + "\"");
        System.out.print(getJaroWinklerDistance(s1,s2));
        System.out.print(" ");
        System.out.print(getLevenshteinDistance(s1,s2));
        System.out.print(" ");
        System.out.println(getSubstringDistance(s1,s2));
        System.out.println();
    }

    /** *************************************************************
     * A test method.
     */
    public static void main(String args[]) {
        // read(args[0]);

        printTest("sitting","kitten");
        printTest("arm","Arm");
        printTest("alarm","Arm");
        printTest("farm","Armory");
        printTest("hiccup","Armory");
        printTest("isSubclassOf","subclass");
        printTest("subclassOf","subclass");
        printTest("supercalafragalisticexpialadotious","subclass");
        printTest("subclass","supercalafragalisticexpialadotious");
        printTest("fix","arm");

        timingTest();

        /**
        try {
          KBmanager.getMgr().initializeOnce();
        }
        catch (Exception e ) {
          System.out.println(e.getMessage());
        }
        Mapping.mapOntologies("SUMO","OBO",10);
        **/

        //System.out.println(m.normalize("Philippe_Mex-s"));
        //System.out.println(m.normalize("AntiguaAndBarbuda"));
        //System.out.println(m.normalize("SUMO"));
/***
        try {
            convertYAGO("TypeExtractor.txt","citizen");
        }
        catch (Exception e ) {
          System.out.println(e.getMessage());
          e.printStackTrace();
        }
        */
    }

}


