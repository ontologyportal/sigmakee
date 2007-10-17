package com.articulate.sigma;

/** This code is copyright Articulate Software (c) 2003.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

import java.util.*;
import java.io.*;
import java.text.ParseException;

/** *****************************************************************
 * A class that finds problems in WordNet to KB mappings.  It is not meant
 * to be instantiated.
 */
public class WNdiagnostics {

    /** *****************************************************************
     * @return an ArrayList of Strings which are WordNet synsets that don't
     * have a corresponding term in the knowledge base
     */
    public static ArrayList synsetsWithoutTerms() {

        ArrayList result = new ArrayList();
        Iterator it = WordNet.wn.synsetsToWords.keySet().iterator();
        while (it.hasNext()) {
            String synset = (String) it.next();
            String POS = synset.substring(0,1);
            synset = synset.substring(1);
            switch (POS.charAt(0)) {
            case '1': 
                if (WordNet.wn.nounSUMOHash.get(synset) == null) 
                    result.add(POS+synset);                
                break;
            case '2': 
                if (WordNet.wn.verbSUMOHash.get(synset) == null) 
                    result.add(POS+synset);                
                break;
            case '3': 
                if (WordNet.wn.adjectiveSUMOHash.get(synset) == null) 
                    result.add(POS+synset);                
                break;
            case '4': 
                if (WordNet.wn.adverbSUMOHash.get(synset) == null) 
                    result.add(POS+synset);                
                break;
            }
            if (result.size() > 50) {
                result.add("limited to 50 results.");
                return result;
            }
        }
        return result;
    }

    /** *****************************************************************
     * @return an ArrayList of Strings which are WordNet synsets that have
     * an identified term but that doesn't exist in the currently loaded
     * knowledge base
     */
    public static ArrayList synsetsWithoutFoundTerms(KB kb) {

        ArrayList result = new ArrayList();
        Iterator it = WordNet.wn.synsetsToWords.keySet().iterator();
        while (it.hasNext()) {
            String synset = (String) it.next();
            String POS = synset.substring(0,1);
            String term = "";
            synset = synset.substring(1);
            switch (POS.charAt(0)) {
            case '1': 
                term = (String) WordNet.wn.nounSUMOHash.get(synset);
                break;
            case '2': 
                term = (String) WordNet.wn.verbSUMOHash.get(synset);
                break;
            case '3': 
                term = (String) WordNet.wn.adjectiveSUMOHash.get(synset);
                break;
            case '4': 
                term = (String) WordNet.wn.adverbSUMOHash.get(synset);
                break;
            }
            if (term != null) {
                ArrayList termList = WordNetUtilities.convertTermList(term);
                for (int i = 0; i < termList.size(); i++) {
                    String newterm = (String) termList.get(i);
                    if (newterm.charAt(0) != '(') {
                        if (!kb.terms.contains(newterm)) 
                            result.add(POS+synset);                     
                    }
                }
            }
            if (result.size() > 50) {
                result.add("limited to 50 results.");
                return result;
            }
        }
        return result;
    }

    /** *****************************************************************
     * @return an ArrayList of Strings which are HTML-formatted presentations 
     * of SUMO terms, and WordNet synsets and that don't
     * have a matching taxonomic structure with their corresponding SUMO
     * terms. Currently, this just examines nouns and needs to be expanded
     * to examine verbs too.
     */
    public static ArrayList nonMatchingTaxonomy(String kbName, String language) {

        String synsetHTML = "<a href=\"WordNet.jsp?";
        String termHTML = "<a href=\"Browse.jsp?kb=" + kbName + "&lang=" + language + "&";
        ArrayList result = new ArrayList();
        Iterator it = WordNet.wn.nounSUMOHash.keySet().iterator();
        while (it.hasNext()) {
            //System.out.println();
            String synset = (String) it.next();                         // not a prefixed synset
            if (WordNet.wn.nounSUMOHash.get(synset) != null) {
                ArrayList words = (ArrayList) WordNet.wn.synsetsToWords.get("1"+synset);
                String sumoTerm = (String) WordNet.wn.nounSUMOHash.get(synset);
                String word = (String) words.get(0);
                //System.out.println("Source word: " + word);
                ArrayList rels = (ArrayList) WordNet.wn.relations.get("1"+synset);   // relations requires prefixes
                if (rels != null) {
                    Iterator it2 = rels.iterator();
                    while (it2.hasNext()) {
                        AVPair avp = (AVPair) it2.next();
                        if (avp.attribute.equals("hypernym") || avp.attribute.equals("hyponym")) {
                            String targetSynset = avp.value; 
                            ArrayList targetWords = (ArrayList) WordNet.wn.synsetsToWords.get(targetSynset);
                            String targetWord = (String) targetWords.get(0);
                            //System.out.println("Target word: " + targetWord);                            
                            String targetBareSynset = avp.value.substring(1);               
                            String targetSUMO = (String) WordNet.wn.nounSUMOHash.get(targetBareSynset);
                            //System.out.println("SUMO source: " + sumoTerm);
                            //System.out.println("SUMO target: " + targetSUMO);
                            String bareSUMOterm = WordNetUtilities.getBareSUMOTerm(sumoTerm);
                            String bareTargetSUMO = WordNetUtilities.getBareSUMOTerm(targetSUMO);
                            if (sumoTerm != null) {
                                KB kb = KBmanager.getMgr().getKB("SUMO");
                                HashSet SUMOtaxonomy = new HashSet();
                                String arrow = "->";
                                if (avp.attribute.equals("hypernym")) 
                                    SUMOtaxonomy = (HashSet) kb.parents.get(bareSUMOterm);                                                                  
                                if (avp.attribute.equals("hyponym")) {
                                    SUMOtaxonomy = (HashSet) kb.children.get(bareSUMOterm);                                
                                    arrow = "<-";
                                }
                                //System.out.println("taxonomy: " + SUMOtaxonomy);
                                if (SUMOtaxonomy != null && targetSUMO != null && !SUMOtaxonomy.contains(bareTargetSUMO) &&
                                    !bareSUMOterm.equals(bareTargetSUMO)) {
                                    StringBuffer resultString = new StringBuffer();
                                    resultString.append("(" + synsetHTML + "synset=1" + synset + "\">" + word + "</a>" + arrow);
                                    resultString.append(synsetHTML + "synset=" + targetSynset + "\">" + targetWord + "</a>) ");
                                    resultString.append("(" + termHTML + "term=" + bareSUMOterm + "\">" + bareSUMOterm + "</a>!" + arrow);
                                    resultString.append(termHTML + "term=" + bareTargetSUMO + "\">" + bareTargetSUMO + "</a>)<br>\n");
                                    result.add(resultString.toString());
                                    if (result.size() > 50) {
                                        result.add("limited to 50 results.");
                                        return result;
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
     * Create an HTML-formatted table that counts WordNet-SUMO mapping
     * types.
     */
    public static String countMappings()  {

        int equals = 0;
        int plus = 0;
        int ampersand = 0;
        int leftbr = 0;
        int rightbr = 0;
        int colon = 0;
        Iterator it = WordNet.wn.nounSUMOHash.keySet().iterator(); // Keys are synset Strings, values are SUMO 
                                                      // terms with the &% prefix and =, +, @ or [ suffix. 
        while (it.hasNext()) {
            String key = (String) it.next();
            String mapping = (String) WordNet.wn.nounSUMOHash.get(key);
            switch (mapping.charAt(mapping.length()-1)) {
              case '=': equals++; break;
              case '+': plus++; break;
              case '@': ampersand++; break;
              case '[': leftbr++; break;
              case ']': rightbr++; break;
              case ':': colon++; break;
            }
        }
        it = WordNet.wn.verbSUMOHash.keySet().iterator(); // Keys are synset Strings, values are SUMO 
                                                      // terms with the &% prefix and =, +, @ or [ suffix. 
        while (it.hasNext()) {
            String key = (String) it.next();
            String mapping = (String) WordNet.wn.verbSUMOHash.get(key);

            switch (mapping.charAt(mapping.length()-1)) {
              case '=': equals++; break;
              case '+': plus++; break;
              case '@': ampersand++; break;
              case '[': leftbr++; break;
              case ']': rightbr++; break;
              case ':': colon++; break;
            }
        }
        it = WordNet.wn.adjectiveSUMOHash.keySet().iterator(); // Keys are synset Strings, values are SUMO 
                                                      // terms with the &% prefix and =, +, @ or [ suffix. 
        while (it.hasNext()) {
            String key = (String) it.next();
            String mapping = (String) WordNet.wn.adjectiveSUMOHash.get(key);
            switch (mapping.charAt(mapping.length()-1)) {
              case '=': equals++; break;
              case '+': plus++; break;
              case '@': ampersand++; break;
              case '[': leftbr++; break;
              case ']': rightbr++; break;
              case ':': colon++; break;
            }
        }
        it = WordNet.wn.adverbSUMOHash.keySet().iterator(); // Keys are synset Strings, values are SUMO 
                                                      // terms with the &% prefix and =, +, @ or [ suffix. 
        while (it.hasNext()) {
            String key = (String) it.next();
            String mapping = (String) WordNet.wn.adverbSUMOHash.get(key);
            switch (mapping.charAt(mapping.length()-1)) {
              case '=': equals++; break;
              case '+': plus++; break;
              case '@': ampersand++; break;
              case '[': leftbr++; break;
              case ']': rightbr++; break;
              case ':': colon++; break;
            }
        }
        String result = "<table><tr bgcolor=#DDDDDD><td>equivalent</td><td>subsuming</td><td>instance</td>" +
                        "<td>anti-subsuming</td><td>anti-instance</td><td>anti-equivalent</td></tr>\n" +
                        "<td>" + equals + "</td><td>" + plus + "</td><td>" + ampersand + "</td>" + 
                        "<td>" + leftbr + "</td><td>" + rightbr + "</td><td>" + colon + "</td></tr></table>\n" +
                        "<table><tr bgcolor=#DDDDDD><td>nouns</td><td>verbs</td>" + 
                        "<td>adjectives</td><td>adverbs</td></tr>\n" + 
                        "<tr><td>" + WordNet.wn.nounSUMOHash.keySet().size() + 
                        "</td><td>" + WordNet.wn.verbSUMOHash.keySet().size() + 
                        "</td><td>" + WordNet.wn.adjectiveSUMOHash.keySet().size() + 
                        "</td><td>" + WordNet.wn.adverbSUMOHash.keySet().size() + 
                        "</td></tr></table>\n"; 
        return result;
    }       
             
}

