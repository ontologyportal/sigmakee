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

    public static FormulaMatchMemoMapKey createFormulaMatchMemoMapKey(String s1, String s2) {
        return new FormulaMatchMemoMapKey(s1,s2);
    }

    public static class FormulaMatchMemoMapKey {
        String f1;
        String f2;

        public FormulaMatchMemoMapKey(String f1, String f2) {
            this.f1 = f1;
            this.f2 = f2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FormulaMatchMemoMapKey that = (FormulaMatchMemoMapKey) o;

            if (f1 != null ? !f1.equals(that.f1) : that.f1 != null) return false;
            if (f2 != null ? !f2.equals(that.f2) : that.f2 != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = f1 != null ? f1.hashCode() : 0;
            result = 31 * result + (f2 != null ? f2.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "{" +
                    "f1='" + f1 + '\'' +
                    ", f2='" + f2 + '\'' +
                    '}';
        }
    }
}
