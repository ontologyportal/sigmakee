/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

package com.articulate.sigma;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/** *************************************************************** 
 * A utility class that defines static methods for working with Sets
 * and other Collections.
 */
public class SetUtil {

    private SetUtil() {
        // This class should not have any instances.
    }

    /** ***************************************************************
     * Removes duplicates from collection based on its natural
     * comparator or equality operator.
     *
     * @param collection The collection from which duplicate elements
     *                   are to be removed.
     *
     */
    public static void removeDuplicates(Collection collection) {
    	
        try {
            HashSet hs = new HashSet();
            Object obj = null;
            for (Iterator it = collection.iterator(); it.hasNext();) {
                obj = (Object) it.next();
                if (hs.contains(obj)) 
                    it.remove();                
                else 
                    hs.add(obj);                
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** ***************************************************************
     * Returns true if obj is not a Collection (including if obj ==
     * null) or if obj is an empty Collection.  Returns false if obj
     * is a non-empty Collection.
     *
     * @param obj The Object to be tested, presumably with the
     * expectation that it could be a Collection
     *
     * @return true or false
     */
    public static boolean isEmpty(Object obj) {
        return (!(obj instanceof Collection) || ((Collection) obj).isEmpty());
    }

    /** ***************************************************************
     * Returns the first String value indexed by a String regex matching
     * key in the property list plist, else returns an empty String
     * if no regex in plist matches key.
     *
     * @param plist A property list (cf. LISP) containing an even
     * number of Strings, which are understood to be consecutive
     * regex/value pairs
     *
     * @param key A String that will be matched against the regular
     * expressions in plist.
     *
     * @return A String
     */
    public static String plistMatch(List<String> plist, String key) {
    	
        String ans = "";
        try {
            int plistSize = plist.size();
            if ((plistSize > 1) && ((plistSize % 2) == 0)) {
                int valIdx = -1;
                String str = null;
                for (int i = 0; i < plistSize; i++) {
                    str = plist.get(i);
                    if (i == valIdx) {
                        ans = str;
                        break;
                    }
                    if ((i % 2) != 0) 
                        continue;                    
                    if (key.matches(str)) 
                        valIdx = (i + 1);                    
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** **************************************************************
     * Filters the List of Strings seq, removing all items that match
     * the regular expression pattern regex.
     *
     * @param seq A List of Strings
     *
     * @param regex A regular expression pattern String that will be
     * matched against the Strings in seq
     *
     */
    public static void removeByPattern(List seq, String regex) {
    	
        try {
            if ((seq instanceof List) && StringUtil.isNonEmptyString(regex)) {
                Object obj = null;
                for (ListIterator it = seq.listIterator(); it.hasNext();) {
                    obj = (Object) it.next();
                    if (obj.toString().matches(regex)) 
                        it.remove();                    
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** **************************************************************
     * Filters the List of Strings seq, retaining only items that match
     * the regular expression pattern regex.
     *
     * @param seq A List of Strings
     *
     * @param regex A regular expression pattern String that will be
     * matched against the Strings in seq
     *
     */
    public static void retainByPattern(List seq, String regex) {
    	
        try {
            if ((seq instanceof List) && StringUtil.isNonEmptyString(regex)) {
                Object obj = null;
                for (ListIterator it = seq.listIterator(); it.hasNext();) {
                    obj = (Object) it.next();
                    if (!obj.toString().matches(regex)) {
                        it.remove();
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

}  // SetUtil
