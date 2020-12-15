package com.articulate.sigma.utils;

import com.articulate.sigma.KB;
import com.google.common.collect.Sets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MapUtils {

    /** ***************************************************************
     */
    public static void addToMapMap(HashMap<String, HashMap<String, HashSet<String>>> mapmap, String superkey, String key, String element) {

        HashMap<String, HashSet<String>> map = mapmap.get(superkey);
        if (map == null)
            map = new HashMap<String, HashSet<String>>();
        mapmap.put(superkey,map);
        HashSet<String> set = map.get(key);
        if (set == null)
            set = new HashSet<String>();
        set.add(element);
        map.put(key,set);
    }

    /** ***************************************************************
     * utility method to add a String element to a HashMap of String
     * keys and a value of an HashSet of Strings
     */
    public static void addToMap(HashMap<String, HashSet<String>> map, String key, String element) {

        HashSet<String> al = map.get(key);
        if (al == null)
            al = new HashSet<String>();
        al.add(element);
        map.put(key, al);
    }

    /** ***************************************************************
     * utility method to merge two HashMaps of String keys and a values
     * of an HashSet of Strings.  Note that parent classes in the set of
     * classes will be removed
     */
    public static HashMap<String, HashSet<String>> mergeToMap(HashMap<String, HashSet<String>> map1,
                                                              HashMap<String, HashSet<String>> map2, KB kb) {

        HashMap<String, HashSet<String>> result = new HashMap<String,HashSet<String>>(map1);

        for (String key : map2.keySet()) {
            Set<String> value = new HashSet<String>();
            if (result.containsKey(key)) {
                value = result.get(key);
            }
            value.addAll(map2.get(key));
            value = kb.removeSuperClasses(value);
            result.put(key, Sets.newHashSet(value));
        }
        return result;
    }
}
