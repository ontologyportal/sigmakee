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
import java.text.SimpleDateFormat;


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
     *  Removes all balanced ASCII double-quote characters from each
     *  end of the String s, if any are present.
     *
     */
    public static String removeEnclosingQuotes(String s) {
        StringBuilder sb = new StringBuilder();
        if (isNonEmptyString(s)) {
            sb.append(s);
            int lastI = (sb.length() - 1);
            while ((lastI > 0)
                   && (sb.charAt(0) == '"')
                   && (sb.charAt(lastI) == '"')) {
                sb.deleteCharAt(lastI);
                sb.deleteCharAt(0);
                lastI = (sb.length() - 1);
            }
        }
        return sb.toString();
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
            // ans = ans.replaceAll("(?s)\\s", " ");
            ans = ans.replaceAll("\\s+", " ");
            // ans = ans.replaceAll("\\.\\s+", ".  ");
            // ans = ans.replaceAll("\\?\\s+", "?  ");
            // ans = ans.replaceAll("\\:\\s+", ":  ");
            // ans = ans.replaceAll("\\!\\s+", "!  ");
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
     * @return true if str contains any non-ASCII characters, else
     * false.
     */
    public static boolean containsNonAsciiChars(String str) {
        return isNonEmptyString(str) && str.matches(".*[^\\p{ASCII}].*");
    }

    /** ***************************************************************
     * @param str A String
     * @return A String with all non-ASCII characters replaced by "x".
     */
    public static String replaceNonAsciiChars(String str) {
        String ans = str;
        if (isNonEmptyString(ans)) {
            ans = ans.replaceAll("[^\\p{ASCII}]", "x");
        }
        return ans;
    }

    /** ***************************************************************
     *  Replace any character that isn't a valid KIF identifier
     *  character with a lower-case x.
     */
    public static String replaceNonIdChars(String st) {

        String ans = st;
        if (isNonEmptyString(ans)) {
            ans = ans.replaceAll("[\\W.]", "x");
            while (ans.matches(".+[^\\p{Alnum}]$")) {
                ans = ans.substring(0, ans.length() - 1);
            }
        }
        return ans;
    }

    /** ***************************************************************
     * Returns a date/time string corresponding to pattern.  The
     * date/time returned is the date/time of the method call.  The
     * locale is UTC (Greenwich).
     *
     * @param pattern Examples: yyyy, yyyyMMdd.
     *
     */
    public static String getDateTime(String pattern) {

        String dateTime = "";
        try {
            if (isNonEmptyString(pattern)) {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                sdf.setTimeZone(new SimpleTimeZone(0, "Greenwich"));
                dateTime = sdf.format(new Date());
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return dateTime;
    }

    /** ***************************************************************
     * Returns true if input appears to be a URI string, else returns
     * false.
     *
     * @param input A String
     *
     */
    public static boolean isUri(String input) {

        boolean ans = false;
        try {
            ans = (isNonEmptyString(input)
                   && (input.matches("^.?http://.+")
                       || input.matches("^.?file://.+")));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     * Returns true if input is a String containing some whitespace
     * chars, else returns false.
     *
     * @param input A String
     *
     * @return true or false
     */
    public static boolean isStringWithSpaces(String input) {

        return (isNonEmptyString(input) && input.matches(".*\\s+.*"));
    }

    /** ***************************************************************
     * Returns true if input appears to be a quoted String, else
     * returns false.
     *
     * @param input A String
     *
     */
    public static boolean isQuotedString(String input) {

        boolean ans = false;
        try {
            if (isNonEmptyString(input)) {
                int ilen = input.length();
                if (ilen > 2) {
                    char fc = input.charAt(0);
                    char lc = input.charAt(ilen - 1);
                    ans = (((fc == '"') && (lc == '"'))
                           || (((fc == '\'') || (fc == '`'))
                               && (lc == '\'')));
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     * Returns a new String formed by adding quoteChar to each end of
     * input.
     *
     * @param input A String
     *
     */
    public static String makeQuotedString(String input, char quoteChar) {
        String ans = input;
        try {
            if (isNonEmptyString(input)
                && !isQuotedString(input)
                && (input.charAt(0) != quoteChar)) {
                StringBuilder sb = new StringBuilder();
                sb.append(quoteChar);
                sb.append(input);
                sb.append(quoteChar);
                ans = sb.toString();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     * Returns true if every char in input is a digit char, else
     * returns false.
     *
     * @param input A String
     *
     * @return true or false
     */
    public static boolean isDigitString(String input) {
        return isNonEmptyString(input) && !input.matches(".*\\D+.*");
    }

    /** ***************************************************************
     * Performs a depth-first search of tree, replacing all terms
     * matching oldPattern with newTerm.
     *
     * @param oldPattern A regular expression pattern to be matched
     * against terms in tree
     *
     * @param newTerm A String to replace terms matching oldPattern
     *
     * @param tree A String representing a SUO-KIF Formula (list)
     * 
     * @return A new tree (String), with all occurrences of terms
     * matching oldPattern replaced by newTerm
     */
    public static String treeReplace(String oldPattern, String newTerm, String tree) {
        String result = tree;
        try {
            StringBuilder sb = new StringBuilder();
            String flist = tree;;
            if (flist.matches(oldPattern))
                sb.append(newTerm);
            else if (Formula.listP(flist)) {
                if (Formula.empty(flist)) {
                    sb.append(flist);
                }
                else {
                    Formula f = new Formula();
                    f.read(flist);
                    List tuple = f.literalToArrayList();
                    sb.append("(");
                    int i = 0;
                    for (Iterator it = tuple.iterator(); it.hasNext(); i++) {
                        if (i > 0) sb.append(" ");
                        sb.append(treeReplace(oldPattern,
                                              newTerm,
                                              (String) it.next()));
                    }
                    sb.append(")");
                }
            }
            else {
                sb.append(flist);
            }
            result = sb.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
     * Returns a new ArrayList formed by extracting in order the
     * top-level members of kifListAsString, which is assumed to be
     * the String representation of a SUO-KIF (LISP) list.
     *
     * @param kifListAsString A SUO-KIF list represented as a String
     *
     * @return ArrayList
     *
     */
    public static ArrayList kifListToArrayList(String kifListAsString) {
        ArrayList ans = new ArrayList();
        try {
            if (isNonEmptyString(kifListAsString)) {
                Formula f = new Formula();
                f.read(kifListAsString);
                ans = f.literalToArrayList();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

}
