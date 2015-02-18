package com.articulate.sigma;

import com.google.common.collect.ObjectArrays;

import java.io.Serializable;
import java.util.*;

/**
 * Created by sserban on 2/17/15.
 */
public class FormulaUtil {
    public static List<String[]> setPermutations(Set<String> set) {
        List<String[]> result = new LinkedList<String[]>();
        String[] prefix = new String[0];
        permutation(prefix, (String[]) set.toArray(new String[set.size()]), result);
        return result;
    }

    private static void permutation(String[]  prefix, String[] array, List<String[]> permutations) {
        int n = array.length;
        if (n == 0) {
            permutations.add(prefix);
            return;
        } else {
            for (int i = 0; i < n; i++) {
                String[] newPrefix = Arrays.copyOf(prefix, prefix.length + 1);
                newPrefix[prefix.length] = array[i];
                String[] leftovers = new String[n - 1];
                for(int j = 0; j < i; j++) {
                    leftovers[j] = array[j];
                }
                for(int j = i + 1; j < n; j++) {
                    leftovers[j - 1] = array[j];
                }
                permutation(newPrefix, leftovers, permutations);
            }
        }
    }
}
