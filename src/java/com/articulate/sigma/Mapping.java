
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

Map ontologies using an approach based on:

John Li, "LOM: A Lexicon-based Ontology Mapping Tool",
Proceedings of the Performance Metrics for Intelligent
Systems  (PerMIS.'04), 2004.
*
*This is not yet fully implemented here
*
This class also includes utilities for converting other
ad-hoc formats to KIF
   */
public class Mapping {

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
    *   Normalize a string by replacing all non-letter, non-digit
    *   characters with spaces, adding spaces on capitalization
    *   boundaries, and then converting to lower case
     */
    private static String normalize(String s)  {

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
                String normTerm1 = normalize(term1);
                if (term1.length() > 2 && !Formula.isLogicalOperator(term1)) {
                    TreeMap tm = (TreeMap) result.get(term1);
                    if (tm == null) 
                        tm = new TreeMap();
                    Iterator it2 = kb2.terms.iterator();
                    while (it2.hasNext()) {
                        String term2 = (String) it2.next();
                        String normTerm2 = normalize(term2);
                        if (term2.length() > 2 && !Formula.isLogicalOperator(term2)) {
                            if (normTerm1.equals(normTerm2)) {
                                Integer score = new Integer(1);
                                tm.put(score,term2);
                            } 
                            else {
                                if (normTerm1.indexOf(normTerm2) > -1) {
                                    Integer score = new Integer(normTerm1.indexOf(normTerm2) + 
                                        (normTerm1.length() - normTerm2.length()));
                                    tm.put(score,term2);
                                }
                                else if (normTerm2.indexOf(normTerm1) > -1) {
                                    Integer score = new Integer(normTerm2.indexOf(normTerm1) + 
                                        (normTerm2.length() - normTerm1.length()));
                                    tm.put(score,term2);
                                }
                            }
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
        Mapping m = new Mapping();
        m.mapOntologies("SUMO","YAGO");


        //System.out.println(normalize("Philippe_Mex-s"));
        //System.out.println(normalize("AntiguaAndBarbuda"));
        //System.out.println(normalize("SUMO"));
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


