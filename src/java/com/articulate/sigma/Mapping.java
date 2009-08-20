
package com.articulate.sigma;

import java.io.*;
import java.util.*;

import com.sun.source.tree.Tree;

/** This code is copyright Articulate Software (c) 2004.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.See also http://sigmakee.sourceforge.net

This class maps ontologies. It includes embedded subclasses that
implement specific mapping heuristics.

This class also includes utilities for converting other
ad-hoc formats to KIF
   */
abstract public class Mapping {

    /** *************************************************************
     *  Write synonymousExternalConcept expressions for cases where
     *  the match score is above a certain threshold.
     */
    public static String writeEquivalences(TreeMap mappings, String kbname1, 
                                           String kbname2) throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null; 
        String filename = kbname1 + "-links.kif";

        try {
            fw = new FileWriter(filename);
            pw = new PrintWriter(fw);
            int threshold = 2;  // lower number is more strict
        
            Iterator it = mappings.keySet().iterator();
            while (it.hasNext()) {
                String term1 = (String) it.next();
                TreeMap value = (TreeMap) mappings.get(term1);
                Iterator it2 = value.keySet().iterator();
                while (it2.hasNext()) {
                    Integer score = (Integer) it2.next();
                    String term2 = (String) value.get(score);
                    if (score.intValue() < threshold) {
                        pw.println("(synonymousExternalConcept \"" + term2 + 
                                   "\" " + term1 + " " + kbname2 + ")");
                    }
                }
            }
        }
        catch (java.io.IOException e) {
            throw new IOException("Error writing file " + filename + "\n" + e.getMessage());
        }
        finally {
            if (pw != null) {
                pw.flush();
                pw.close();
            }
            if (fw != null) {
                fw.close();
            }
        }
        return filename + " written to ";
    }

    /** *************************************************************
    *   Convert a YAGO file into KIF
     */
    public static void convertYAGO(String file, String relName) throws IOException {

        File f = new File(file);
        if (f == null) {
            System.out.println( "INFO in convertYAGO(): " 
                                + "The file " + file + " does not exist" );
            return;
        }
        FileReader r = new FileReader(f);
        LineNumberReader lr = new LineNumberReader(r);
        String line = null;
        while ((line = lr.readLine()) != null) {
            line = line.trim();
            if (line != null && line.length() > 0) {
                int tab1 = line.indexOf("\t");
                int tab2 = line.indexOf("\t",tab1+1);
                int tab3 = line.indexOf("\t",tab2+1);
                String term1 = line.substring(tab1+1,tab2);
                String term2 = line.substring(tab2+1,tab3);
                term1 = OWLtranslator.StringToKIFid(term1);
                term2 = OWLtranslator.StringToKIFid(term2);
                System.out.println("(" + relName + " " + term1 + " " + term2 + ")");
            }
        }
    }

    /** *************************************************************
     * Map ontologies through 4 methods:
     * (1) identical term names
     * (2) substrings of term names are equal
     * (3) terms align to words in the same WordNet synset
     * (4) extra "points" for having terms that align with the same
     * structural arrangement
     * 
     * @return a TreeMap where the key is a term from the first
     *         ontology and the value is another TreeMap.  The
     *         internal TreeMap has keys that are an integer mapping
     *         score and the values are terms from the second
     *         ontology.
     */
    public TreeMap mapOntologies(String kbName1, String kbName2) {

        TreeMap result = new TreeMap();
        int counter = 0;
        KB kb1,kb2;
        kb1 = KBmanager.getMgr().getKB(kbName1);
        kb2 = KBmanager.getMgr().getKB(kbName2);
        if (kb1 != null && kb2 != null) {
            Iterator it1 = kb1.terms.iterator();
            while (it1.hasNext()) {
                counter++;
                if (counter == 100) {
                    System.out.print(".");
                    counter = 0;
                }
                String term1 = (String) it1.next();
                if (isValidTerm(term1)) {
                    String normTerm1 = normalize(term1);
                    TreeMap tm = (TreeMap) result.get(term1);
                    if (tm == null) 
                        tm = new TreeMap();
                    Iterator it2 = kb2.terms.iterator();
                    while (it2.hasNext()) {
                        String term2 = (String) it2.next();
                        if (isValidTerm(term2)) {
                            String normTerm2 = normalize(term2);
                            int score = getDistance(normTerm1, kb1, normTerm2, kb2);
                            if (score < Integer.MAX_VALUE)
                                tm.put(new Integer(score), term2);
                        }
                    }
                    if (tm.keySet().size() > 0)
                        result.put(term1,tm);
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
        return result;
    }

    /** *************************************************************
     *   get distance between two terms
     */
    abstract public int getDistance(String term1, KB kb1, String term2, KB kb2);

    /** *************************************************************
     *   check whether a term is valid (worthy of being compared)
     */
    public boolean isValidTerm(String term) {
        return term.length() > 2 && !Formula.isLogicalOperator(term);
    }

    /** *************************************************************
     *   Normalize a string by replacing all non-letter, non-digit
     *   characters with spaces, adding spaces on capitalization
     *   boundaries, and then converting to lower case
     */
    public String normalize(String s) {
        if (s == null || s.length() < 1)
            return null;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            if ((Character.isLetter(s.charAt(i)) && Character.isLowerCase(s.charAt(i))) ||
                Character.isDigit(s.charAt(i))) 
                result.append(s.charAt(i));
            else {
                if (Character.isLetter(s.charAt(i)) && Character.isUpperCase(s.charAt(i))) {
                    if (result.length() < 1 || result.charAt(result.length()-1) != ' ') 
                        result.append(" ");
                    result.append(Character.toLowerCase(s.charAt(i)));
                }
                else 
                    if (result.length() < 1 || result.charAt(result.length()-1) != ' ') 
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
     *
     */
    public static class SubstringMapping extends Mapping {
        public int getDistance(String term1, KB kb1, String term2, KB kb2) {
            if (term1.equals(term2))
                return 1;
            else if (term1.indexOf(term2) > -1)
                return term1.indexOf(term2) + 
                        (term1.length() - term2.length());
            else if (term2.indexOf(term1) > -1)
                return term2.indexOf(term1) + (term2.length() - term1.length());
            else
                return Integer.MAX_VALUE;
        }
    }

    /** *************************************************************
     *  Jaro-Winkler Mapping Method
     *  implemented by Gerard de Melo
     */
    public static class JaroWinklerMapping extends Mapping {
        private static final int SCALING_FACTOR = 10000;
        private int winklerMaxPrefixLen = 4;
        private double winklerPrefixWeight = 0.1;

        public int getDistance(String s1, KB kb1, String s2, KB kb2) {
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
            int nMatches = 0;
            for (int i = 0; i < len2; i++) { // index in s2
                char c = s2.charAt(i);
                int jStart = (i > maxDistance) ? i - maxDistance : 0;
                int jEnd = i + maxDistance + 1;
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
    }

    /** *************************************************************
     * A test method.
     */
    public static void main(String args[]) {
        // read(args[0]);

        try {
          KBmanager.getMgr().initializeOnce();
        } 
        catch (Exception e ) {
          System.out.println(e.getMessage());
        }
        Mapping m = new SubstringMapping();
        m.mapOntologies("SUMO","YAGO");

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


