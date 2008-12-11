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

import java.util.*;
import java.io.*;


  /** ***************************************************************
  *  A utility class that defines static methods for common string
  *  manipulation operations.
  */
public class StringUtil {

    private StringUtil() {
        // This class should not have any instances.
    }

    /** ***************************************************************
     * @param obj Any object
     * @return true if obj is a non-empty String, else false.
     */
    public static boolean isNonEmptyString(Object obj) {
        return ((obj instanceof String) && !obj.equals(""));
    }

    /** ***************************************************************
     * @param s An input Object, expected to be a String.
     * @return true if s == null or s is an empty String, else false.
     */
    public static boolean emptyString(Object s) {
        return ((s == null) 
                || ((s instanceof String)
                    && s.equals("")));
    }

    /** ***************************************************************
     *  Remove the quotes in the first and last character of a
     *  String, if present.
     */
    public static String removeEnclosingQuotes(String s) {
        if (isNonEmptyString(s) 
            && (s.charAt(0) == '"')
            && (s.charAt(s.length()-1) == '"')) {
            return s.substring(1,s.length()-1);
        }
        return s;
    }

    /** ***************************************************************
     * @param str A String
     * @return A String with space characters normalized to match the
     * conventions for written English text.  All linefeeds and
     * carriage returns are replaced with spaces.
     */
    public static String normalizeSpaceChars(String str) {
        String ans = str;
        if (isNonEmptyString(ans)) {
            ans = ans.replaceAll("\\\\n", " ");
            ans = ans.replaceAll("\\s\\s*", " ");
            ans = ans.replaceAll("\\.\\s+", ".  ");
        }
        return ans;
    }

    /** ***************************************************************
     * @param str A String
     * @return A String with all double quote characters properly
     * escaped with a left slash character.
     */
    public static String escapeQuoteChars(String str) {
        String ans = str;
        if (isNonEmptyString(str)) {
            StringBuffer sb = new StringBuffer();
            char prevCh = 'x';
            char ch = 'x';
            for (int i = 0; i < str.length(); i++) {
                ch = str.charAt(i);
                if ((ch == '"') && (prevCh != '\\')) {
                    sb.append('\\');
                }
                sb.append(ch);
                prevCh = ch;
            }
            ans = sb.toString();
        }
        return ans;
    }

    /** ***************************************************************
     * @param str A String
     * @return A String with all escape characters properly
     * escaped with a left slash character.
     */
    public static String escapeEscapeChars(String str) {
        String ans = str;
        if (isNonEmptyString(str)) {
            StringBuffer sb = new StringBuffer();
            char prevCh = 'x';
            char ch = 'x';
            for (int i = 0; i < str.length(); i++) {
                ch = str.charAt(i);
                if ((ch == '\\') && (prevCh != '\\')) {
                    sb.append('\\');
                }
                sb.append(ch);
                prevCh = ch;
            }
            ans = sb.toString();
        }
        return ans;
    }

    /** ***************************************************************
     * @param str A String
     * @return A String with all double quote characters properly
     * escaped with a left slash character.
     */
    public static String removeQuoteEscapes(String str) {
        String ans = str;
        if (isNonEmptyString(str)) {
            StringBuffer sb = new StringBuffer();
            char prevCh = 'x';
            char ch = 'x';
            for (int i = 0; i < str.length(); i++) {
                ch = str.charAt(i);
                if ((ch == '"') && (prevCh == '\\')) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                sb.append(ch);
                prevCh = ch;
            }
            ans = sb.toString();
        }
        return ans;
    }

    /** ***************************************************************
     * @param str A String
     * @return A String with all sequences of two double quote
     * characters have been replaced by a left slash character
     * followed by a double quote character.
     */
    public static String replaceRepeatedDoubleQuotes(String str) {
        String ans = str;
        if (isNonEmptyString(str)) {
            StringBuffer sb = new StringBuffer();
            char prevCh = 'x';
            char ch = 'x';
            for (int i = 0; i < str.length(); i++) {
                ch = str.charAt(i);
                if ((ch == '"') && (prevCh == '"')) {
                    sb.setCharAt(sb.length() - 1, '\\');
                }
                sb.append(ch);
                prevCh = ch;
            }
            ans = sb.toString();
        }
        return ans;
    }

    /** ***************************************************************
     * @param str A String
     * @return A String with all non-ASCII characters replaced by "-".
     */
    public static String replaceNonAsciiChars(String str) {
        String ans = str;
        if (isNonEmptyString(ans)) {
            ans = ans.replaceAll("[^\\p{ASCII}]", "-");
        }
        return ans;
    }

    /** ***************************************************************
     *  Replace any character that isn't a valid KIF identifier
     *  character with a hyphen.
     */
    public static String replaceNonIdChars(String st) {

        String ans = st;
        if (isNonEmptyString(ans)) {
            ans = ans.replaceAll("[\\W.]", "-");
            while (ans.matches(".+[^\\p{Alnum}]$")) {
                ans = ans.substring(0, ans.length() - 1);
            }
        }
        return ans;
    }

}
