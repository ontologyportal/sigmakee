package com.articulate.sigma.nlp;

import com.articulate.sigma.*;
import java.util.*;

/**
 * an implementation of
 * Resnik, P. (1995). Using information content to evaluate semantic similarity in a taxonomy.
 * In Proceedings of the 14th International Joint Conference on Artificial Intelligence, 448–453.
 */
public class WNsim {

    /** ***************************************************************
     */
    public static HashMap<String,Integer> computeSubsumingFreq(String rel)  {

        int safetyCounter = 0;
        HashMap<String,Integer> freqs = new HashMap<>();
        TreeSet<String> ts = new TreeSet<>();
        ts.addAll(WordNetUtilities.findLeavesInTree(rel));
        while (!ts.isEmpty() && safetyCounter < 50) {
            String synset = ts.first();
            if (!freqs.keySet().contains(synset))
                freqs.put(synset,1);
            ArrayList<AVPair> rels = WordNet.wn.relations.get(synset);
            for (AVPair avp : rels) {
                if (avp.attribute.equals(rel)) {
                    ts.add(avp.value);
                    if (freqs.keySet().contains(avp.value))
                        freqs.put(avp.value,freqs.get(avp.value) + freqs.get(synset));
                    else
                        freqs.put(avp.value,freqs.get(synset));
                }
            }
            safetyCounter++;
        }
        return freqs;
    }

    /** ***************************************************************
     *  A main method, used only for testing.  It should not be called
     *  during normal operation.
     */
    public static void main (String[] args) {

        try {
            KBmanager.getMgr().initializeOnce();
            HashMap<String,Integer> hm = computeSubsumingFreq("hyponym");
            for (String s: hm.keySet()) {
                System.out.print(WordNet.wn.getWordsFromSynset(s).get(0) + ":" + hm.get(s) + ", ");
            }
        }
        catch (Exception e) {
            System.out.println("Error in WordNetUtilities.main(): Exception: " + e.getMessage());
        }
    }
}

