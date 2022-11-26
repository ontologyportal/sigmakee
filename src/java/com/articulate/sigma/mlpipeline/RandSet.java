package com.articulate.sigma.mlpipeline;

import com.articulate.sigma.utils.AVPair;
import com.articulate.sigma.wordNet.WordNet;
import com.articulate.sigma.wordNet.WordNetUtilities;

import java.util.*;

/** A set of terms, sorted by frequency, and duplicated by log of frequency.
 * The idea is that more frequent terms will be retrieved more frequently.
 */
public class RandSet {

    ArrayList<String> terms = new ArrayList<String>();
    HashSet<String> returned = new HashSet<>();

    public static boolean avoidDup = false;
    public Random rand = new Random();

    /** ***************************************************************
     */
    public int size() {
        return terms.size();
    }

    /** ***************************************************************
     */
    public void clearReturns() {
        returned.clear();
    }

    /** ***************************************************************
     */
    public String getNext() {

        if (this.terms.size() < 1) {
            System.out.println("RandSet.getNext(): empty set: " + terms);
            return "";
        }
        int index = 0;
        do {
            index = rand.nextInt(this.terms.size());
        } while (avoidDup && returned.contains(terms.get(index)));
        returned.add(terms.get(index));
        return terms.get(index);
    }

    /** ***************************************************************
     * Create a list, in the global variable terms, in which String terms
     * appear as many times as the log of their frequency + 1 in the input.
     * This list is used by the method getNext() to provide a random term
     * with a likelihood of return determined by that frequency.
     * @param freqs is a set of term keys and frequency (integers as Strings) values
     */
    public static RandSet create(Collection<AVPair> freqs) {

        RandSet r = new RandSet();
        TreeMap<Integer, Collection<String>> freqMap = new TreeMap<>();
        for (AVPair avp : freqs) {
            int count = 0;
            int freq = Integer.parseInt(avp.value);
            count = (int) Math.round(Math.log(freq) + 1.0);
            Collection<String> terms = null;
            if (freqMap.containsKey(count))
                terms = freqMap.get(count);
            else
                terms = new HashSet<>();
            terms.add(avp.attribute);
            freqMap.put(count,terms);
        }
        List<Integer> nums = new ArrayList(freqMap.keySet());
        Collections.reverse(nums);
        for (int num : nums) {
            Collection<String> terms = freqMap.get(num);
            for (String t : terms) {
                for (int i = 0; i < num; i++) {
                    r.terms.add(t);
                }
            }
        }
        return r;
    }

    /** ***************************************************************
     * Convert a list into a RandSet that assumes a frequency count of
     * one for each element
     */
    public static RandSet listToEqualPairs(Collection<String> input) {

        RandSet r = new RandSet();
        r.terms.addAll(input);
        return r;
    }
}
