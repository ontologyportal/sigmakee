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
                if (term.charAt(0) != '(') {
                    term = term.substring(2,term.length()-1);
                    if (!kb.terms.contains(term)) 
                        result.add(POS+synset);                     
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
     * @return an ArrayList of Strings which are WordNet synsets that don't
     * have a matching taxonomic structure with their corresponding SUMO
     * terms.
     */
    public static ArrayList nonMatchingTaxonomy() {

        ArrayList result = new ArrayList();
        Iterator it = WordNet.wn.nounSUMOHash.keySet().iterator();
        while (it.hasNext()) {
            String synset = (String) it.next();                         // not a prefixed synset
            if (WordNet.wn.nounSUMOHash.get(synset) != null) {
                ArrayList rels = (ArrayList) WordNet.wn.relations.get("1"+synset);   // relations requires prefixes
                if (rels != null) {
                    Iterator it2 = rels.iterator();
                    while (it2.hasNext()) {
                        AVPair avp = (AVPair) it2.next();
                        if (avp.attribute.equals("hypernym") || avp.attribute.equals("hyponym")) {
                            String targetSynset = avp.value; 
                            String targetBareSynset = avp.value.substring(1);               
                            String targetSUMO = (String) WordNet.wn.nounSUMOHash.get(targetBareSynset);
                            String sumoTerm = (String) WordNet.wn.nounSUMOHash.get(synset);
                            if (sumoTerm != null) {
                                KB kb = KBmanager.getMgr().getKB("SUMO");
                                HashSet SUMOtaxonomy = new HashSet();
                                if (avp.attribute.equals("hypernym"))
                                    SUMOtaxonomy = (HashSet) kb.parents.get(sumoTerm);
                                if (avp.attribute.equals("hyponym"))
                                    SUMOtaxonomy = (HashSet) kb.children.get(sumoTerm);                                
                                if (SUMOtaxonomy != null && targetSUMO != null && !SUMOtaxonomy.contains(targetSUMO)) {
                                    result.add("1"+synset);                                
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
}

