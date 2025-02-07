package com.articulate.sigma.wordNet;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.AVPair;
import com.articulate.sigma.utils.StringUtil;

/** *****************************************************************
 * A class that finds problems in WordNet to KB mappings.  It is not meant
 * to be instantiated.
 */
public class WNdiagnostics {

    /** *****************************************************************
     * @return an ArrayList of Strings which are WordNet synsets that don't
     * have a corresponding term in the knowledge base
     */
    public static List<String> synsetsWithoutTerms() {

        List<String> result = new ArrayList<>();
        Iterator<String> it = WordNet.wn.synsetsToWords.keySet().iterator();
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
     * Distinguish between WordNet 3.0 synset ids and ones created from
     * SUMO termFormat expressions by use of origMaxNounSynsetID and
     * origMaxVerbSynsetID
     * @param term is the term that needs to be checked for whether it
     *             has WordNet mappings
     */
    private static boolean hasWordNetSynsetID(String term) {

        if (!WordNet.wn.SUMOHash.containsKey(term))
            return false;
        List<String> synsets = WordNet.wn.SUMOHash.get(term);
        for (String s : synsets) {
            if (s.charAt(0) == '1') { // noun
                String bareSynset = s.substring(1);
                if (bareSynset.compareTo(WordNet.wn.origMaxNounSynsetID) < 0)
                    return true;
            }
            else if (s.charAt(0) == '2') { // noun
                String bareSynset = s.substring(1);
                if (bareSynset.compareTo(WordNet.wn.origMaxVerbSynsetID) < 0)
                    return true;
            }
            else if (WordNetUtilities.isValidSynset9(s))
                return true;
        }
        return false;
    }

    /** *****************************************************************
     * @return an ArrayList of Strings which are terms that don't
     * have a corresponding synset
     */
    public static ArrayList<String> nonRelationTermsWithoutSynsets() {

        ArrayList<String> result = new ArrayList<>();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        for (String term : kb.terms) {
            if (!hasWordNetSynsetID(term) && !kb.isFunction(term) &&
                    Character.isUpperCase(term.charAt(0)))
                result.add(term);
        }
        return result;
    }

    /** *****************************************************************
     * @return an ArrayList of Strings which are WordNet synsets that have
     * an identified term but that doesn't exist in the currently loaded
     * knowledge base
     */
    public static List<String> synsetsWithoutFoundTerms(KB kb) {

        List<String> result = new ArrayList<>();
        for (String synset : WordNet.wn.synsetsToWords.keySet()) {
            String POS = synset.substring(0,1);
            String term = "";
            synset = synset.substring(1);
            //System.out.println("synsetsWithoutFoundTerms(): " + synset + " " + POS + " " + term + " ");
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
            if (!StringUtil.emptyString(term)) {
                List<String> termList = WordNetUtilities.convertTermList(term);
                for (int i = 0; i < termList.size(); i++) {
                    String newterm = (String) termList.get(i);
                    if (newterm.charAt(0) != '(') {
                        if (!kb.getTerms().contains(newterm))
                            result.add(POS + synset);
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
    public static ArrayList<String> nonMatchingTaxonomy(String kbName, String language) {

        String synsetHTML = "<a href=\"WordNet.jsp?";
        String termHTML = "<a href=\"Browse.jsp?kb=" + kbName + "&lang=" + language + "&";
        ArrayList<String> result = new ArrayList<>();
        for (String synset : WordNet.wn.nounSUMOHash.keySet()) {
            //System.out.println();
            // not a prefixed synset
            if (WordNet.wn.nounSUMOHash.get(synset) != null) {
                List<String> words = WordNet.wn.synsetsToWords.get("1"+synset);
                String sumoTerm = (String) WordNet.wn.nounSUMOHash.get(synset);
                String word = (String) words.get(0);
                //System.out.println("Source word: " + word);
                List<AVPair> rels = WordNet.wn.relations.get("1"+synset);   // relations requires prefixes
                if (rels != null) {
                    Iterator<AVPair> it2 = rels.iterator();
                    while (it2.hasNext()) {
                        AVPair avp = (AVPair) it2.next();
                        if (avp.attribute.equals("hypernym") || avp.attribute.equals("hyponym")) {
                            String targetSynset = avp.value;
                            List<String> targetWords = WordNet.wn.synsetsToWords.get(targetSynset);
                            String targetWord = (String) targetWords.get(0);
                            //System.out.println("Target word: " + targetWord);
                            String targetBareSynset = avp.value.substring(1);
                            String targetSUMO = (String) WordNet.wn.nounSUMOHash.get(targetBareSynset);
                            //System.out.println("SUMO source: " + sumoTerm);
                            //System.out.println("SUMO target: " + targetSUMO);
                            String bareSUMOterm = WordNetUtilities.getBareSUMOTerm(sumoTerm);
                            String bareTargetSUMO = WordNetUtilities.getBareSUMOTerm(targetSUMO);
                            if (sumoTerm != null) {
                                KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
                                Set<String> SUMOtaxonomy = new HashSet<>();
                                String arrow = "->";
                                if (avp.attribute.equals("hypernym"))
                                    SUMOtaxonomy = kb.kbCache.getParentClasses(bareSUMOterm);
                                if (avp.attribute.equals("hyponym")) {
                                    SUMOtaxonomy = kb.kbCache.getChildClasses(bareSUMOterm);
                                    arrow = "<-";
                                }
                                //System.out.println("taxonomy: " + SUMOtaxonomy);
                                if (SUMOtaxonomy != null && targetSUMO != null && !SUMOtaxonomy.contains(bareTargetSUMO) &&
                                        !bareSUMOterm.equals(bareTargetSUMO)) {
                                    StringBuilder resultString = new StringBuilder();
                                    resultString.append("(").append(synsetHTML).append("synset=1").append(synset).append("\">").append(word).append("</a>").append(arrow);
                                    resultString.append(synsetHTML).append("synset=").append(targetSynset).append("\">").append(targetWord).append("</a>) ");
                                    resultString.append("(").append(termHTML).append("term=").append(bareSUMOterm).append("\">").append(bareSUMOterm).append("</a>!").append(arrow);
                                    resultString.append(termHTML).append("term=").append(bareTargetSUMO).append("\">").append(bareTargetSUMO).append("</a>)<br>\n");
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
        Iterator<String> it = WordNet.wn.nounSUMOHash.keySet().iterator(); // Keys are synset Strings, values are SUMO
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

    /** ***************************************************************
     *  A main method, used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void main (String[] args) {

        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        WordNet.initOnce();
        System.out.println(nonRelationTermsWithoutSynsets());
    }

}

