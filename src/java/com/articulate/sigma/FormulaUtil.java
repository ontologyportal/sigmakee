package com.articulate.sigma;

import com.google.common.collect.ObjectArrays;

import java.io.Serializable;
import java.util.*;

/**
 * Created by sserban on 2/17/15.
 */
public class FormulaUtil {

    public static List<int[]> getPermutations(int size) {
        int[] array = new int[size];
        for( int i = 0; i< size; i++) {
            array[i] = i;
        }
        List<int[]> result = new LinkedList<int[]>();
        permutation(new int[0], array, result);
        return result;
    }

    private static void permutation(int[]  prefix, int[] array, List<int[]> permutations) {
        int n = array.length;
        if (n == 0) {
            permutations.add(prefix);
            return;
        } else {
            for (int i = 0; i < n; i++) {
                int[] newPrefix = Arrays.copyOf(prefix, prefix.length + 1);
                newPrefix[prefix.length] = array[i];
                int[] leftovers = new int[n - 1];
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
