/* This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the terms of
the GNU license.  This software is released under the GNU Public
License <http://www.gnu.org/copyleft/gpl.html>.  Users of this code
also consent, by use of this code, to credit Articulate Software and
Teknowledge in any writings, briefings, publications, presentations,
or other representations of any software which incorporates, builds
on, or uses this code.  Please cite the following article in any
publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in
Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed
Systems, August 9, Acapulco, Mexico. See also http://sigmakee.sourceforge.net
*/

package com.articulate.sigma;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** ************************************************************
 * Handle operations on an individual formula.  This includes
 * formatting for presentation as well as pre-processing for sending
 * to the inference engine.
 */
public class Formula implements Comparable {

    protected static final String AND    = "and";
    protected static final String OR     = "or";
    protected static final String NOT    = "not";
    protected static final String IF     = "=>";
    protected static final String IFF    = "<=>";
    protected static final String UQUANT = "forall";
    protected static final String EQUANT = "exists";
    protected static final String EQUAL  = "equal";
    protected static final String GT     = "greaterThan";
    protected static final String GTET   = "greaterThanOrEqualTo";
    protected static final String LT     = "lessThan";
    protected static final String LTET   = "lessThanOrEqualTo";

    protected static final String KAPPAFN  = "KappaFn";
    protected static final String PLUSFN   = "AdditionFn";
    protected static final String MINUSFN  = "SubtractionFn";
    protected static final String TIMESFN  = "MultiplicationFn";
    protected static final String DIVIDEFN = "DivisionFn";
    protected static final String SKFN     = "SkFn";
    protected static final String SK_PREF = "Sk";
    protected static final String FN_SUFF = "Fn";
    protected static final String V_PREF  = "?";
    protected static final String R_PREF  = "@";
    protected static final String VX      = "?X";
    protected static final String VVAR    = "?VAR";
    protected static final String RVAR    = "@ROW";

    protected static final String LP = "(";
    protected static final String RP = ")";
    protected static final String SPACE = " ";

    protected static final String LOG_TRUE  = "True";
    protected static final String LOG_FALSE = "False";

    /** The SUO-KIF logical operators. */
    public static final List<String> LOGICAL_OPERATORS = Arrays.asList(UQUANT,
                                                                        EQUANT,
                                                                        AND,
                                                                        OR,
                                                                        NOT,
                                                                        IF,
                                                                        IFF);

    /** SUO-KIF mathematical comparison predicates. */
    private static final List<String> COMPARISON_OPERATORS = Arrays.asList(EQUAL,
                                                                           GT,
                                                                           GTET,
                                                                           LT,
                                                                           LTET);

    /** The SUO-KIF mathematical functions are implemented in Vampire. */
    private static final List<String> MATH_FUNCTIONS = Arrays.asList(PLUSFN,
                                                                     MINUSFN,
                                                                     TIMESFN,
                                                                     DIVIDEFN);

    public static final List<String> DOC_PREDICATES = Arrays.asList("documentation",
                                                                    "comment",
                                                                    "format" //,
                                                                    // "termFormat"
                                                                    );
    /** The source file in which the formula appears. */
    protected String sourceFile;

    /** The line in the file on which the formula starts. */
    public int startLine;

    /** The line in the file on which the formula ends. */
    public int endLine;

    /** The length of the file in bytes at the position immediately
     *  after the end of the formula.  This value is used only for
     *  formulas entered via KB.tell().  In general, you should not
     *  count on it being set to a value other than -1L.
     */
    public long endFilePosition = -1L;

    /** The formula. */
    public String theFormula;

	private static Logger logger = null;

    /** ***************************************************************
     *  Returns the platform-specific line separator String
     */
    public String getLineSeparator() {
        return System.getProperty("line.separator");
    }

    public String getSourceFile() {
        return this.sourceFile;
    }

    public void setSourceFile(String filename) {
        this.sourceFile = filename;
        return;
    }

    /** ***************************************************************
     * Should be false if this Formula occurs in and was loaded from
     * sourceFile.  Should be true if this Formula does not actually
     * occur in sourceFile but was computed (derived) from at least
     * some Formulae in sourceFile, possibly in combination with other
     * Formulae not is sourceFile.
     */
    private boolean isComputed = false;

    /** ***************************************************************
     * Should return false if this Formula occurs in and was loaded
     * from sourceFile.  Should return true if this Formula does not
     * actually occur in sourceFile but was computed (derived) from at
     * least some Formulae in sourceFile, possibly in combination with
     * other Formulae not is sourceFile.
     */
    public boolean getIsComputed() {
        return isComputed;
    }

    /** ***************************************************************
     * Sets the value of isComputed to val.
     */
    public void setIsComputed(boolean val) {
        isComputed = val;
        return;
    }

    /** ***************************************************************
     * A list of TPTP formulas (Strings) that together constitute the
     * translation of theFormula.  This member is a List, because
     * predicate variable instantiation and row variable expansion
     * might cause theFormula to expand to several TPTP formulas.
     */
    public ArrayList<String> theTptpFormulas = null;

    /** ***************************************************************
     * Returns an ArrayList of the TPTP formulas (Strings) that
     * together constitute the TPTP translation of theFormula.
     *
     * @return An ArrayList of Strings, or an empty ArrayList if no
     * translations have been created or entered.
     */
    public ArrayList<String> getTheTptpFormulas() {

        if (theTptpFormulas == null)
            theTptpFormulas = new ArrayList();
        return theTptpFormulas;
    }

    /** ***************************************************************
     * Clears theTptpFormulas if the ArrayList exists, else does
     * nothing.
     */
    public void clearTheTptpFormulas() {

        if (theTptpFormulas != null)
            theTptpFormulas.clear();
        return;
    }

    /** *****************************************************************
     * A list of clausal (resolution) forms generated from this
     * Formula.
     */
    private ArrayList theClausalForm = null;

	public Formula() {
		if (logger == null)
			logger = Logger.getLogger(this.getClass().getName());
	}
    /** ***************************************************************
     * Returns a List of the clauses that together constitute the
     * resolution form of this Formula.  The list could be empty if
     * the clausal form has not yet been computed.
     *
     * @return ArrayList
     */
    public ArrayList getTheClausalForm() {

		logger.entering("Formula", "getTheClausalForm");
        try {
            if (theClausalForm == null) {
                if (isNonEmptyString(theFormula))
                    theClausalForm = Clausifier.toNegAndPosLitsWithRenameInfo(this);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
			logger.warning(ex.getMessage());
        }

		logger.exiting("Formula", "getTheClausalForm", theClausalForm);
        return theClausalForm;
    }

    /** ***************************************************************
     * This method clears the list of clauses that together constitute
     * the resolution form of this Formula, and can be used in
     * preparation for recomputing the clauses.
     */
    public void clearTheClausalForm() {

        if (theClausalForm != null)
            theClausalForm.clear();
        theClausalForm = null;
    }

    /** ***************************************************************
     * Returns a List of List objects.  Each such object contains, in
     * turn, a pair of List objects.  Each List object in a pair
     * contains Formula objects.  The Formula objects contained in the
     * first List object (0) of a pair represent negative literals
     * (antecedent conjuncts).  The Formula objects contained in the
     * second List object (1) of a pair represent positive literals
     * (consequent conjuncts).  Taken together, all of the clauses
     * constitute the resolution form of this Formula.
     *
     * @return A List of Lists.
     */
    public ArrayList getClauses() {

        ArrayList clausesWithVarMap = getTheClausalForm();
        if ((clausesWithVarMap == null) || clausesWithVarMap.isEmpty())
            return null;
        return (ArrayList) clausesWithVarMap.get(0);
    }

    /** ***************************************************************
     * Returns a map of the variable renames that occurred during the
     * translation of this Formula into the clausal (resolution) form
     * accessible via this.getClauses().
     *
     * @return A Map of String (SUO-KIF variable) key-value pairs.
     */
    public HashMap getVarMap() {

        ArrayList clausesWithVarMap = getTheClausalForm();
        if ((clausesWithVarMap == null) || (clausesWithVarMap.size() < 3))
            return null;
        return (HashMap) clausesWithVarMap.get(2);
    }

    /** ***************************************************************
     * Returns the variable in this Formula that corresponds to the
     * clausal form variable passed as input.
     *
     * @return A SUO-KIF variable (String), which may be just the
     * input variable.
     */
    public String getOriginalVar(String var) {

        Map varmap = getVarMap();
        if (varmap == null)
            return var;
        return Clausifier.getOriginalVar(var, varmap);
    }

    /** ***************************************************************
     * For any given formula, stop generating new pred var
     * instantiations and row var expansions if this threshold value
     * has been exceeded.  The default value is 2000.
     */
    private static final int AXIOM_EXPANSION_LIMIT = 2000;

    /** ***************************************************************
     * This constant indicates the maximum predicate arity supported
     * by the current implementation of Sigma.
     */
    protected static final int MAX_PREDICATE_ARITY = 7;

    /** ***************************************************************
     * Read a String into the variable 'theFormula'.
     */
    public void read(String s) {
        theFormula = s;
    }

    /** ***************************************************************
     */
    public static String integerToPaddedString(int i, int digits) {

        String result = Integer.toString(i);
        while (result.length() < digits) {
            result = "0" + result;
        }
        return result;
    }

    /** ***************************************************************
    *  @return a unique ID by appending the hashCode() of the
    *  formula String to the file name in which it appears
     */
    public String createID() {

        String fname = sourceFile;
        if (StringUtil.isNonEmptyString(fname) && fname.lastIndexOf(File.separator) > -1)
            fname = fname.substring(fname.lastIndexOf(File.separator)+1);
        int hc = theFormula.hashCode();
        String result = null;
        if (hc < 0)
            result = "N" + (new Integer(hc)).toString().substring(1) + fname;
        else
            result = (new Integer(hc)).toString() + fname;

		// logger.finest("ID Created: " + result + "; For the formula: " +
		// theFormula);

        return result;
    }

    /** ***************************************************************
     * Copy the Formula.  This is in effect a deep copy.
     */
    private Formula copy() {

        Formula result = new Formula();
        if (sourceFile != null)
            result.sourceFile = sourceFile.intern();
        result.startLine = startLine;
        result.endLine = endLine;
        if (theFormula != null)
            result.theFormula = theFormula.intern();
        return result;
    }

    /** ***************************************************************
     */
    private Formula deepCopy() {
        return copy();
    }

    /** ***************************************************************
     * Implement the Comparable interface by defining the compareTo
     * method.  Formulas are equal if their formula strings are equal.
     */
    public int compareTo(Object f) throws ClassCastException {
    	
        if (!f.getClass().getName().equalsIgnoreCase("com.articulate.sigma.Formula"))
            throw new ClassCastException("Error in Formula.compareTo(): "
                                         + "Class cast exception for argument of class: "
                                         + f.getClass().getName());
        return theFormula.compareTo(((Formula) f).theFormula);
    }

    /** ***************************************************************
     * Returns true if the Formula contains no unbalanced parentheses
     * or unbalanced quote characters, otherwise returns false.
     *
     * @return boolean
     */
    public boolean isBalancedList() {
        boolean ans = false;
        try {
            if (this.listP()) {
                if (this.empty())
                    ans = true;
                else {
                    String input = this.theFormula.trim();
                    List quoteChars = Arrays.asList('"', '\'');
                    int i = 0;
                    int len = input.length();
                    int end = len - 1;
                    int pLevel = 0;
                    int qLevel = 0;
                    char prev = '0';
                    char ch = prev;
                    boolean insideQuote = false;
                    char quoteCharInForce = '0';
                    while (i < len) {
                        ch = input.charAt(i);
                        if (!insideQuote) {
                            if (ch == '(')
                                pLevel++;
                            else if (ch == ')')
                                pLevel--;
                            else if (quoteChars.contains(ch) && (prev != '\\')) {
                                insideQuote = true;
                                quoteCharInForce = ch;
                                qLevel++;
                            }
                        }
                        else if (quoteChars.contains(ch)
                                 && (ch == quoteCharInForce)
                                 && (prev != '\\')) {
                            insideQuote = false;
                            quoteCharInForce = '0';
                            qLevel--;
                        }
                        prev = ch;
                        i++;
                    }
                    ans = ((pLevel == 0) && (qLevel == 0));
					// logger.finest("qLevel == " + qLevel);
					// logger.finest("pLevel == " + pLevel);
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return ans;
    }


    /** ***************************************************************
     * @return the LISP 'car' of the formula as a String - the first
     * element of the list. Note that this operation has no side
     * effect on the Formula.
     *
     * Currently (10/24/2007) this method returns the empty string
     * ("") when invoked on an empty list.  Technically, this is
     * wrong.  In most LISPS, the car of the empty list is the empty
     * list (or nil).  But some parts of the Sigma code apparently
     * expect this method to return the empty string when invoked on
     * an empty list.
     */
    public String car() {
		logger.entering("Formula", "car");
        String ans = null;
        try {
            if (this.listP()) {
                if (this.empty()) {
                    // NS: Clean this up someday.
                    ans = "";  // this.theFormula;
                }
                else {
                    String input = this.theFormula.trim();
                    StringBuilder sb = new StringBuilder();
                    List quoteChars = Arrays.asList('"', '\'');
                    int i = 1;
                    int len = input.length();
                    int end = len - 1;
                    int level = 0;
                    char prev = '0';
                    char ch = prev;
                    boolean insideQuote = false;
                    char quoteCharInForce = '0';
                    while (i < end) {
                        ch = input.charAt(i);
                        if (!insideQuote) {
                            if (ch == '(') {
                                sb.append(ch);
                                level++;
                            }
                            else if (ch == ')') {
                                sb.append(ch);
                                level--;
                                if (level <= 0) {
                                    break;
                                }
                            }
                            else if (Character.isWhitespace(ch) && (level <= 0)) {
                                if (sb.length() > 0) {
                                    break;
                                }
                            }
                            else if (quoteChars.contains(ch) && (prev != '\\')) {
                                sb.append(ch);
                                insideQuote = true;
                                quoteCharInForce = ch;
                            }
                            else {
                                sb.append(ch);
                            }
                        }
                        else if (quoteChars.contains(ch)
                                 && (ch == quoteCharInForce)
                                 && (prev != '\\')) {
                            sb.append(ch);
                            insideQuote = false;
                            quoteCharInForce = '0';
                            if (level <= 0) {
                                break;
                            }
                        }
                        else {
                            sb.append(ch);
                        }
                        prev = ch;
                        i++;
                    }
                    ans = sb.toString();
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            return null;
            // ex.printStackTrace();
        }
		logger.exiting("Formula", "car", ans);
        return ans;
    }

    /** ***************************************************************
     * Return the LISP 'cdr' of the formula - the rest of a list minus its
     * first element.
     * Note that this operation has no side effect on the Formula.
     */
    public String cdr() {

		logger.entering("Formula", "cdr");
        String ans = null;
        try {
            if (this.listP()) {
                if (this.empty()) {
                    ans = this.theFormula;
                }
                else {
                    String input = theFormula.trim();
                    List quoteChars = Arrays.asList('"', '\'');
                    int i = 1;
                    int len = input.length();
                    int end = len - 1;
                    int level = 0;
                    char prev = '0';
                    char ch = prev;
                    boolean insideQuote = false;
                    char quoteCharInForce = '0';
                    int carCount = 0;
                    while (i < end) {
                        ch = input.charAt(i);
                        if (!insideQuote) {
                            if (ch == '(') {
                                carCount++;
                                level++;
                            }
                            else if (ch == ')') {
                                carCount++;
                                level--;
                                if (level <= 0) {
                                    break;
                                }
                            }
                            else if (Character.isWhitespace(ch) && (level <= 0)) {
                                if (carCount > 0) {
                                    break;
                                }
                            }
                            else if (quoteChars.contains(ch) && (prev != '\\')) {
                                carCount++;
                                insideQuote = true;
                                quoteCharInForce = ch;
                            }
                            else {
                                carCount++;
                            }
                        }
                        else if (quoteChars.contains(ch)
                                 && (ch == quoteCharInForce)
                                 && (prev != '\\')) {
                            carCount++;
                            insideQuote = false;
                            quoteCharInForce = '0';
                            if (level <= 0) {
                                break;
                            }
                        }
                        else {
                            carCount++;
                        }
                        prev = ch;
                        i++;
                    }
                    if (carCount > 0) {
                        int j = i + 1;
                        if (j < end) {
                            ans = "(" + input.substring(j, end).trim() + ")";
                        }
                        else {
                            ans = "()";
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            return null;
            // ex.printStackTrace();
        }
		logger.exiting("Formula", "cdr", ans);
        return ans;
    }

    /** ***************************************************************
     * Returns a new Formula which is the result of 'consing' a String
     * into this Formula, similar to the LISP procedure of the same
     * name.  This procedure is a little bit of a kluge, since this
     * Formula is treated simply as a LISP object (presumably, a LISP
     * list), and could be degenerate or malformed as a Formula.
     *
     * Note that this operation has no side effect on the original Formula.
     *
     * @param obj The String object that will become the 'car' (or
     * head) of the resulting Formula (list).
     *
     * @return a new Formula, or the original Formula if the cons fails.
     */
    public Formula cons(String obj) {

		logger.entering("Formula", "cons", obj);
        Formula ans = this;
        try {
            String fStr = this.theFormula;
            if (isNonEmptyString(obj) && isNonEmptyString(fStr)) {
                String theNewFormula = null;
                if (this.listP()) {
                    if (this.empty())
                        theNewFormula = ("(" + obj + ")");
                    else
                        theNewFormula = ("(" + obj + " " + fStr.substring(1, (fStr.length() - 1)) + ")");
                }
                else
                    // This should never happen during clausification, but
                    // we include it to make this procedure behave
                    // (almost) like its LISP namesake.
                    theNewFormula = ("(" + obj + " . " + fStr + ")");
                if (theNewFormula != null) {
                    ans = new Formula();
                    ans.read(theNewFormula);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
			logger.warning(ex.getMessage());
        }
		logger.exiting("Formula", "cons", ans);
        return ans;
    }

    /** ***************************************************************
     * @return a new Formula, or the original Formula if the cons fails.
     */
    public Formula cons(Formula f) {

        return cons(f.theFormula);
    }

    /** ***************************************************************
     * Returns the LISP 'cdr' of the formula as a new Formula, if
     * possible, else returns null.
     *
     * Note that this operation has no side effect on the Formula.
     * @return a Formula, or null.
     */
    public Formula cdrAsFormula() {
    	
        String thisCdr = this.cdr();
        if (listP(thisCdr)) {
            Formula f = new Formula();
            f.read(thisCdr);
            return f;
        }
        return null;
    }

    /** ***************************************************************
     * Returns the LISP 'cadr' (the second list element) of the
     * formula.
     *
     * Note that this operation has no side effect on the Formula.
     * @return a String, or the empty string if the is no cadr.
     */
    public String cadr() {
    	
        return this.getArgument(1);
    }

    /** ***************************************************************
     * Returns the LISP 'cddr' of the formula - the rest of the rest,
     * or the list minus its first two elements.
     *
     * Note that this operation has no side effect on the Formula.
     * @return a String, or null.
     */
    public String cddr() {
    	
        Formula fCdr = this.cdrAsFormula();
        if (fCdr != null) {
            return fCdr.cdr();
        }
        return null;
    }

    /** ***************************************************************
     * Returns the LISP 'cddr' of the formula as a new Formula, if
     * possible, else returns null.
     *
     * Note that this operation has no side effect on the Formula.
     *
     * @return a Formula, or null.
     */
    public Formula cddrAsFormula() {
        String thisCddr = this.cddr();
        if (listP(thisCddr)) {
            Formula f = new Formula();
            f.read(thisCddr);
            return f;
        }
        return null;
    }

    /** ***************************************************************
     * Returns the LISP 'caddr' of the formula, which is the third
     * list element of the formula.
     *
     * Note that this operation has no side effect on the Formula.
     *
     * @return a String, or the empty string if there is no caddr.
     *
     */
    public String caddr() {
        return this.getArgument(2);
    }

    /** ***************************************************************
     * Returns the LISP 'append' of the formulas
     * Note that this operation has no side effect on the Formula.
     * @return a Formula
     */
    public Formula append(Formula f) {

        Formula newFormula = new Formula();
        newFormula.read(theFormula);
        if (newFormula.equals("") || newFormula.atom()) {
            System.out.println("Error in KB.append(): attempt to append to non-list: " + theFormula);
            return this;
        }
        if (f == null || f.theFormula == null || f.theFormula == "" || f.theFormula.equals("()"))
            return newFormula;
        f.theFormula = f.theFormula.trim();
        if (!f.atom())
            f.theFormula = f.theFormula.substring(1,f.theFormula.length()-1);
        int lastParen = theFormula.lastIndexOf(")");
        String sep = "";
        if (lastParen > 1)
            sep = " ";
        newFormula.theFormula = newFormula.theFormula.substring(0,lastParen) + sep + f.theFormula + ")";
        return newFormula;
    }

    /** ***************************************************************
     * Test whether the String is a LISP atom.
     */
    public static boolean atom(String s) {

        boolean ans = false;
        if (isNonEmptyString(s)) {
            String str = s.trim();
            ans = (StringUtil.isQuotedString(s) ||
                  (!str.contains(")") && !str.matches(".*\\s.*")) );
        }
        return ans;
    }

    /** ***************************************************************
     * Test whether the Formula is a LISP atom.
     */
    public boolean atom() {

        return Formula.atom(theFormula);
    }

    /** ***************************************************************
     * Test whether the Formula is an empty list.
     */
    public boolean empty() {

        return Formula.empty(theFormula);
    }

    /** ***************************************************************
     * Test whether the String is an empty formula.  Not to be
     * confused with a null string or empty string.  There must be
     * parentheses with nothing or whitespace in the middle.
     */
    public static boolean empty(String s) {
        return (listP(s) && s.matches("\\(\\s*\\)"));
    }

    /** ***************************************************************
     * Test whether the Formula is a list.
     */
    public boolean listP() {

        return Formula.listP(theFormula);
    }

    /** ***************************************************************
     * Test whether the String is a list.
     */
    public static boolean listP(String s) {

        boolean ans = false;
        if (isNonEmptyString(s)) {
            String str = s.trim();
            ans = (str.startsWith("(") && str.endsWith(")"));
        }
        return ans;
    }

    /** ***************************************************************
     * @see #validArgs() validArgs below for documentation
     */
    private String validArgsRecurse(Formula f, String filename, Integer lineNo) {

		// logger.finest("Formula: " + f.theFormula);

		if (f.theFormula == "" || !f.listP() || f.atom() || f.empty())
			return "";
        String pred = f.car();
        String rest = f.cdr();
        Formula restF = new Formula();
        restF.read(rest);
        int argCount = 0;
        while (!restF.empty()) {
            argCount++;
            String arg = restF.car();
            Formula argF = new Formula();
            argF.read(arg);
            String result = validArgsRecurse(argF, filename, lineNo);
            if (result != "")
                return result;
            restF.theFormula = restF.cdr();
        }
        if (pred.equals(AND) || pred.equals(OR)) {
            if (argCount < 2)
                return "Too few arguments for 'and' or 'or' in formula: \n"
                    + f.toString() + "\n";
        }
        else if (pred.equals(UQUANT) || pred.equals(EQUANT)) {
            if (argCount != 2)
                return "Wrong number of arguments for 'exists' or 'forall' in formula: \n"
                    + f.toString() + "\n";
            else {
                Formula quantF = new Formula();
                quantF.read(rest);
                if (!listP(quantF.car()))
                    return ("No parenthesized variable list for 'exists' or 'forall' "
                            + "in formula: \n" + f.toString() + "\n");
            }
        }
        else if (pred.equals(IFF) || pred.equals(IF)) {
            if (argCount != 2)
                return "Wrong number of arguments for '<=>' or '=>' in formula: \n"
                    + f.toString() + "\n";
        }
        else if (pred.equals(EQUAL)) {
            if (argCount != 2)
                return "Wrong number of arguments for 'equals' in formula: \n"
                    + f.toString() + "\n";
        }
        else if (// !(isVariable(pred))
                 // &&
                 (KBmanager.getMgr().getPref("holdsPrefix").equalsIgnoreCase("yes")
                  && (argCount > (MAX_PREDICATE_ARITY + 1)))
                 ||
                 (!KBmanager.getMgr().getPref("holdsPrefix").equalsIgnoreCase("yes")
                  && (argCount > MAX_PREDICATE_ARITY))) {
            String location = "";
            if ((filename != null) && (lineNo != null)) {
                location = (" near line " + lineNo + " in " + filename);
            }
            KBmanager.getMgr().setError(KBmanager.getMgr().getError() + "\n<br/>Maybe too many arguments"
                                        + location + ": " + f.toString() + "\n<br/>");
        }
        return "";
    }

    /** ***************************************************************
     * Test whether the Formula uses logical operators and predicates
     * with the correct number of arguments.  "equals", "<=>", and
     * "=>" are strictly binary.  "or", and "and" are binary or
     * greater. "not" is unary.  "forall" and "exists" are unary with
     * an argument list.  Warn if we encounter a formula that has more
     * arguments than MAX_PREDICATE_ARITY.
     *
     * @param filename If not null, denotes the name of the file being
     * parsed.
     *
     * @param lineNo If not null, indicates the location of the
     * expression (formula) being parsed in the file being read.
     *
     * @return an empty String if there are no problems or an error message
     * if there are.
     */
    public String validArgs(String filename, Integer lineNo) {

        if (theFormula == null || theFormula == "")
            return "";
        Formula f = new Formula();
        f.read(theFormula);
        String result = validArgsRecurse(f, filename, lineNo);

		// logger.finest("Result: " + result);

        return result;
    }

    /** ***************************************************************
     * Test whether the Formula uses logical operators and predicates
     * with the correct number of arguments.  "equals", "<=>", and
     * "=>" are strictly binary.  "or", and "and" are binary or
     * greater. "not" is unary.  "forall" and "exists" are unary with
     * an argument list.  Warn if we encounter a formula that has more
     * arguments than MAX_PREDICATE_ARITY.
     *
     * @return an empty String if there are no problems or an error message
     * if there are.
     */
    public String validArgs() {
        return this.validArgs(null, null);
    }

    /** ***************************************************************
     * Not yet implemented!  Test whether the Formula has variables that are not properly
     * quantified.  The case tested for is whether a quantified variable
     * in the antecedent appears in the consequent or vice versa.
     *
     *  @return an empty String if there are no problems or an error message
     *  if there are.
     */
    public String badQuantification() {
        return "";
    }

    /** ***************************************************************
     * Parse a String into an ArrayList of Formulas. The String must be
     * a LISP-style list.
     * @return an ArrayList of Formulas
     */
    private ArrayList parseList(String s) {

        //System.out.println("INFO in Formula.parseList(): s " + s);
        ArrayList result = new ArrayList();
        Formula f = new Formula();
        f.read("(" + s + ")");
        if (f.empty())
            return result;
        while (!f.empty()) {
            //System.out.println("INFO in Formula.parseList(): f " + f.theFormula);
            String car = f.car();
            f.read(f.cdr());
            Formula newForm = new Formula();
            newForm.read(car);
            result.add(newForm);
        }
        return result;
    }

    /** ***************************************************************
     * Compare two lists of formulas, testing whether they are equal,
     * without regard to order.  (B A C) will be equal to (C B A). The
     * method iterates through one list, trying to find a match in the other
     * and removing it if a match is found.  If the lists are equal, the
     * second list should be empty once the iteration is complete.
     * Note that the formulas being compared must be lists, not atoms, and
     * not a set of formulas unenclosed by parentheses.  So, "(A B C)"
     * and "(A)" are valid, but "A" is not, nor is "A B C".
     */
    private boolean compareFormulaSets(String s) {
    	
        // an ArrayList of Formulas
        ArrayList thisList = parseList(this.theFormula.substring(1,this.theFormula.length()-1));
        ArrayList sList = parseList(s.substring(1,s.length()-1));
        if (thisList.size() != sList.size())
            return false;

        for (int i = 0; i < thisList.size(); i++) {
            for (int j = 0; j < sList.size(); j++) {
                if (((Formula) thisList.get(i)).logicallyEquals(((Formula) sList.get(j)).theFormula)) {
                    // System.out.println("INFO in Formula.compareFormulaSets(): " +
                    //       ((Formula) thisList.get(i)).toString() + " equal to " +
                    //       ((Formula) sList.get(j)).theFormula);
                    sList.remove(j);
                    j = sList.size();
                }
            }
        }
        return sList.size() == 0;
    }

    /** ***************************************************************
     * Test if the contents of the formula are equal to the argument
     * at a deeper level than a simple string equals.  The only logical
     * manipulation is to treat conjunctions and disjunctions as unordered
     * bags of clauses. So (and A B C) will be logicallyEqual(s) for example,
     * to (and B A C).  Note that this is a fairly time-consuming operation
     * and should not generally be used for comparing large sets of formulas.
     */
    public boolean logicallyEquals(String s) {

        if (this.equals(s))
            return true;
        if (Formula.atom(s) && s.compareTo(theFormula) != 0)
            return false;

        Formula form = new Formula();
        form.read(this.theFormula);
        Formula sform = new Formula();
        sform.read(s);

        if (form.car().intern() == "and" || form.car().intern() == "or") {
            if (sform.car().intern() != sform.car().intern())
                return false;
            form.read(form.cdr());
            sform.read(sform.cdr());
            return form.compareFormulaSets(sform.theFormula);
        }
        else {
            Formula newForm = new Formula();
            newForm.read(form.car());
            Formula newSform = new Formula();
            newSform.read(sform.cdr());
            return newForm.logicallyEquals(sform.car()) &&
                newSform.logicallyEquals(form.cdr());
        }
    }

    /** ***************************************************************
     * If equals is overridedden, hashCode must use the same
     * "significant" fields.
     */
    public int hashCode() {

        String thisString = Clausifier.normalizeVariables(this.theFormula).trim();
        return (thisString.hashCode());
    }

    /** ***************************************************************
     * Test if the contents of the formula are equal to the
     * argument. Normalize all variables.
     */
    public boolean equals(Formula f) {

        String thisString = Clausifier.normalizeVariables(this.theFormula).trim();
        String argString = Clausifier.normalizeVariables(f.theFormula).trim();
        return (thisString.equals(argString));
    }

    /** ***************************************************************
     * Test if the contents of the formula are equal to the String argument.
     * Normalize all variables.
     */
    public boolean equals(String s) {

        String f = theFormula;
        Formula form = new Formula();
        Formula sform = new Formula();

        form.theFormula = f;
        s = Clausifier.normalizeVariables(s).intern();
        sform.read(s);
        s = sform.toString().trim().intern();

        form.theFormula = Clausifier.normalizeVariables(theFormula);
        f = form.toString().trim().intern();

		// logger.finest("Comparing " + s + " to " + f);

        return (f == s);
    }

    /** ***************************************************************
     * Test if the contents of the formula are equal to the argument.
     */
    public boolean deepEquals(Formula f) {

        return (f.theFormula.intern() == theFormula.intern()) &&
            (f.sourceFile.intern() == sourceFile.intern());
    }

    /** ***************************************************************
     * Return the numbered argument of the given formula.  The first
     * element of a formula (i.e. the predicate position) is number 0.
     * Returns the empty string if there is no such argument position.
     */
    public String getArgument(int argnum) {

        String ans = "";
        try {
            Formula form = new Formula();
            form.read(theFormula);
            for (int i = 0 ; form.listP() ; i++) {
                ans = form.car();
                if (i == argnum) { break; }
                form.read(form.cdr());
            }
            if (ans == null) { ans = ""; }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     * Returns a non-negative int value indicating the top-level list
     * length of this Formula if it is a proper listP(), else returns
     * -1.  One caveat: This method assumes that neither null nor the
     * empty string are legitimate list members in a wff.  The return
     * value is likely to be wrong if this assumption is mistaken.
     *
     * @return A non-negative int, or -1.
     */
    public int listLength() {
    	
        int ans = -1;
        try {
            if (this.listP()) {
                ans = 0;
                while (isNonEmptyString(this.getArgument(ans)))
                    ++ans;
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     * Return all the arguments in a simple formula as a list, starting
     * at the given argument.  If formula is complex (i.e. an argument
     * is a function or sentence), then return null.  If the starting
     * argument is greater than the number of arguments, also return
     * null.
     */
    public ArrayList<String> argumentsToArrayList(int start) {

        if (theFormula.indexOf('(',1) != -1)
            return null;
        int index = start;
        ArrayList result = new ArrayList();
        String arg = getArgument(index);
        while (arg != null && arg != "" && arg.length() > 0) {
            result.add(arg.intern());
            index++;
            arg = getArgument(index);
        }
        if (index == start)
            return null;
        return result;
    }

    /** ***************************************************************
     * Translate SUMO inequalities to the typical inequality symbols that
     * the theorem prover requires.
     */
    private static String translateInequalities(String s) {

        if (s.equalsIgnoreCase("greaterThan")) return ">";
        if (s.equalsIgnoreCase("greaterThanOrEqualTo")) return ">=";
        if (s.equalsIgnoreCase("lessThan")) return "<";
        if (s.equalsIgnoreCase("lessThanOrEqualTo")) return "<=";
        return "";
    }

    /** ***************************************************************
     *  Alter formulas so that their variable indexes have shared
     *  scope, which in effect means that they are different.  For
     *  example, given (p ?V1 a) and (p c ?V1) the second formula
     *  will be changed to (p c ?V2) because the ?V1 in the original
     *  formulas do not refer to the same variable.  There is no
     *  side effect to this method
     *  @return the argument with its variable numbering altered if
     *          necessary
     */
    public Formula separateVariableScope(Formula c2) {

		// logger.finest("before: \n" + this + "\n and " + c2);

        ArrayList<String> varList1 = this.simpleCollectVariables();
        ArrayList<String> varList2 = c2.simpleCollectVariables();
        TreeMap<String, String> varMap = new TreeMap();
        for (int i = 0; i < varList2.size(); i++) {                 // renumber variables
            int index = 1;
            while (varList1.contains("?VAR" + Integer.toString(index)))
                index++;
            String varNew = "?VAR" + Integer.toString(index);
            varMap.put((String) varList2.get(i),varNew);
            varList1.add(varNew);
        }
        Formula c2new = c2.deepCopy();

		// logger.finest("varmap: " + varMap);

        c2new = c2new.substituteVariables(varMap);

		// logger.finest("middle: \n" + c2new + "\n and " + c2new);
		// logger.finest("after: \n" + this + "\n and " + c2new);

        return c2new;
    }

    /** ***************************************************************
     *  @see unify()
     */
    private TreeMap<String, String> unifyVar(String f1, String f2, TreeMap m) {

        //System.out.println("INFO in Formula.unifyVar(): Attempting to unify : " + f1 +
        //                   " with " + f2);
        if (m.keySet().contains(f1))
            return unifyInternal((String) m.get(f1),f2,m);
        else if (m.keySet().contains(f2))
            return unifyInternal((String) m.get(f2),f1,m);
        else if (f2.indexOf(f1) > -1)
            return null;
        else {
            m.put(f1,f2);
            return m;
        }
    }

    /** ***************************************************************
     *  @see unify()
     */
    private TreeMap<String, String> unifyInternal(String f1, String f2, TreeMap m) {

        //System.out.println("INFO in Formula.unifyInternal(): Attempting to unify : " + f1 + " with " + f2);
        if (m == null)
            return null;
        else if (f1.equals(f2))
            return m;
        else if (isVariable(f1))
            return unifyVar(f1,f2,m);
        else if (isVariable(f2))
            return unifyVar(f2,f1,m);
        else if (listP(f1) && listP(f2)) {
            Formula form1 = new Formula();
            form1.read(f1);
            Formula form2 = new Formula();
            form2.read(f2);
            TreeMap<String, String> res = unifyInternal(form1.car(),form2.car(),m);
            if (res == null)
                return null;
            else
                return unifyInternal(form1.cdr(),form2.cdr(),res);
        }
        else {
            //System.out.println("failed to unify");
            return null;
        }
    }

    /** ***************************************************************
     *  Attempt to unify one formula with another. @return a Map of
     *  variable substitutions if successful, null if not. If two
     *  formulas are identical the result will be an empty (but not
     *  null) TreeMap. Algorithm is after Russell and Norvig's AI: A
     *  Modern Approach p303. But R&N's algorithm assumes that
     *  variables are within the same scope, which is not the case
     *  when unifying clauses in resolution.  This needs to be
     *  corrected by renaming variables so each clause does not
     *  duplicate names from the other.
     */
    public TreeMap<String, String> unify(Formula f) {

        //System.out.println("INFO in Formula.unify(): Attempting to unify : " + theFormula + " with " + f.theFormula);
        TreeMap result = new TreeMap();
        result = unifyInternal(f.theFormula,this.theFormula,result);
        return result;
    }

    /** ***************************************************************
     *  Append a clause to a (potentially empty) list of clauses.
     *  append () and (foo A B) yields (foo A B)
     *  append (foo A B) and (bar B C) yields
     *  (or (foo A B) (bar B C))
     *  append (or (foo A B) (bar B C)) and
     *    (baz D E) yields (or (foo A B) (bar B C) (baz D E))
     */
    public Formula appendClauseInCNF(Formula f) {

		// logger.finest("Appending \n" + this + "\n and \n" + f);

		if (f == null) {
			logger.warning("Error in Formula.appendClauseInCNF(): null formula");
            return null;
        }
        if (f.empty())
            return this;
        if (this.empty())
            return f;
        if (this.atom()) {
			logger.warning("Error in Formula.appendClauseInCNF(): formula to append to not in CNF: "
					+ this);
            return null;
        }
        if (f.atom()) {
			logger.warning("Error in Formula.appendClauseInCNF(): formula to append not in CNF: "
					+ f);
            return null;
        }
        Formula thisNew = new Formula();
        Formula fnew = new Formula();
        if (isSimpleClause() || isSimpleNegatedClause())
            thisNew.read("(" + this.theFormula + ")");
        else {
            if (!this.car().equals("or")) {
				logger.warning("Error in Formula.appendClauseInCNF(): formula to append to not in CNF: "
						+ this);
                return null;
            }
            thisNew.read(this.cdr());   // remove the "or"
        }
        if (f.isSimpleClause() || f.isSimpleNegatedClause())
            fnew.read("(" + f.theFormula + ")");
        else {
            if (!f.car().equals("or")) {
				logger.warning("Error in Formula.appendClauseInCNF(): formula to append not in CNF: "
						+ f);
                return null;
            }
            fnew.read(f.cdr());   // remove the "or"
        }
        fnew.theFormula = fnew.theFormula;
        thisNew = thisNew.append(fnew);
        thisNew.theFormula = "(or " + thisNew.theFormula.substring(1);
        return thisNew;
    }

    /** ***************************************************************
     *  Attempt to resolve one formula with another. @return a
     *  TreeMap of (possibly empty) variable substitutions if
     *  successful, null if not. Return Formula "result" as a side
     *  effect, that is the result of the substition (which could be
     *  the empty list), null if not.
     */
    public TreeMap<String, String> resolve(Formula f, Formula result) {

		Formula accumulator = new Formula();

		// logger.finest("Attempting to resolve: \n" + this + "\n with \n" + f);

        if (f.empty() || this.empty()) {
            System.out.println("Error in Formula.resolve() attempt to resolve with empty list");
            return null;
        }

        Formula thisFormula = new Formula();
        thisFormula.read(theFormula);
        Formula argFormula = new Formula();
        argFormula.read(f.theFormula);
        TreeMap mapping = new TreeMap();
        if ((this.isSimpleClause() && f.isSimpleNegatedClause()) ||
            (this.isSimpleNegatedClause() && f.isSimpleClause())) {
			// logger.finest("Both simple clauses");
            if (this.isSimpleNegatedClause()) {
				// logger.finest("This (or fnew).isSimpleNegatedClause(): \n" +
				// this);
                thisFormula.read(thisFormula.cdr());
                thisFormula.read(thisFormula.car());
				// logger.finest("thisFormula: \n" + thisFormula);
            }
            if (f.isSimpleNegatedClause()) {
				// logger.finest("f (or f2).isSimpleNegatedClause(): \n" + f);
                argFormula.read(argFormula.cdr());
                argFormula.read(argFormula.car());
            }
            mapping = thisFormula.unify(argFormula);
            if (mapping == null)
                result.theFormula = null;
            else
                result.theFormula = "()";
			// logger.finest("result: " + result);
			// logger.finest("mapping: " + mapping);
            if (result.theFormula != null)
                result.theFormula = Clausifier.toCanonicalClausalForm(result).theFormula;
            return mapping;
        }
        else {
            if ((this.isSimpleClause() && f.isSimpleClause()) ||
                (this.isSimpleNegatedClause() && f.isSimpleNegatedClause())) {
                return null;
            }
            if (this.isSimpleClause() || this.isSimpleNegatedClause()) {
                if (!argFormula.car().equals("or")) {
					// logger.finest("(this is simple): The non-simple clause not in CNF: \n"
					// + argFormula);
                    return null;
                }
                argFormula.read(argFormula.cdr());  // remove the initial "or"
                accumulator.theFormula = "()";     // holds clauses that have been tried and not unified
                while (!argFormula.empty()) {
                    Formula clause = new Formula();
                    clause.read(argFormula.car());
					// logger.finest("(loop 1): checking clause: \n" + clause);
                    if (result != null)
						// logger.finest("result so far: \n" + accumulator);
                    argFormula.read(argFormula.cdr());
                    Formula newResult = new Formula();
                    mapping = thisFormula.resolve(clause,newResult); // if it succeeds, newResult will be () so ignore
                    if (mapping != null ) {  //&& mapping.keySet().size() > 0
						// logger.finest("(returning loop 1): \n" + newResult);
						// logger.finest("argFormula: \n" + argFormula);
                        if (!argFormula.empty() && !argFormula.isSimpleClause() && !argFormula.isSimpleNegatedClause()) {
                             if (!empty(argFormula.cdr()))
                                 argFormula.read("(or " + argFormula.theFormula.substring(1));
                             else
                                 argFormula.read(argFormula.car());
                        }
                        accumulator = accumulator.appendClauseInCNF(argFormula);
                        accumulator.theFormula = (accumulator.substitute(mapping)).theFormula;
                        result.theFormula = accumulator.theFormula;

						// logger.finest("Result: " + result);
                        if (result.theFormula != null)
                            result.theFormula = Clausifier.toCanonicalClausalForm(result).theFormula;
                        return mapping;
                    }
                    else
                        accumulator = accumulator.appendClauseInCNF(clause);
                }
            }
            else {
                if (argFormula.isSimpleClause() || argFormula.isSimpleNegatedClause()) {
                    if (!this.car().equals("or")) {
						logger.warning("Error in Formula.resolve() (f2 is simple): The non-simple clause not in CNF: "
								+ this);
                        return null;
                    }
                    thisFormula.read(thisFormula.cdr());// remove the initial "or"
                    accumulator.theFormula = "()";
                    while (!thisFormula.empty()) {
                        Formula clause = new Formula();
                        clause.read(thisFormula.car());
						// logger.finest("(loop 2): checking clause: \n" + clause);
                        thisFormula.read(thisFormula.cdr());
                        Formula newResult = new Formula();
                        mapping = argFormula.resolve(clause,newResult);
						// logger.finest("(loop 2): return mapping: \n" + mapping);
                        if (mapping != null) {   //  && mapping.keySet().size() > 0
							// logger.finest("(returning loop 2): newResult: \n"
							// + newResult);
                            if (!thisFormula.empty() && !thisFormula.isSimpleClause() && !thisFormula.isSimpleNegatedClause()) {
                                if (!empty(thisFormula.cdr()))
                                    thisFormula.read("(or " + thisFormula.theFormula.substring(1));
                                else
                                    thisFormula.read(thisFormula.car());
                            }
                            accumulator = accumulator.appendClauseInCNF(thisFormula);
                            accumulator.theFormula = (accumulator.substitute(mapping)).theFormula;
                            result.theFormula = accumulator.theFormula;
							// logger.finest("result: " + result);
                            if (result.theFormula != null)
                                result.theFormula = Clausifier.toCanonicalClausalForm(result).theFormula;
                            return mapping;
                        }
                        else
                            accumulator = accumulator.appendClauseInCNF(clause);
                    }
                }
                else {                                      // both formulas are not a simple clause
                    Formula newResult = new Formula();
					// logger.finest("(before loop 3): looping through argFormula's clauses: \n"
					// argFormula);
                    argFormula.read(argFormula.cdr());    // remove the initial "or"
                    accumulator.theFormula = "()";
                    while (!argFormula.empty()) {
						// logger.finest("(loop 3): here 1: ");
                        Formula clause = new Formula();
                        clause.read(argFormula.car());
						// logger.finest("(loop 3): checking clause: \n" + clause);
                        argFormula.read(argFormula.cdr());
                        TreeMap newMapping = thisFormula.resolve(clause,newResult);
						// logger.finest("(returning loop 3): mapping: " +
						// newMapping);
						// logger.finest("(returning loop 3): argFormula: \n" +
						// argFormula);
						// logger.finest("(returning loop 3): accumulator: \n"+
						// accumulator);
                        if (newMapping != null) {  // && newMapping.keySet().size() > 0
                            mapping.putAll(newMapping);  // could still be a problem if a mapping overwrites another...
							// logger.finest("(returning loop 3): newResult: \n"
							// + newResult);
                            //accumulator = accumulator.appendClauseInCNF(argFormula);
                            accumulator.theFormula = (accumulator.substitute(mapping)).theFormula;
                            argFormula.theFormula = (argFormula.substitute(mapping)).theFormula;
                            thisFormula.theFormula = new String(newResult.theFormula);
                        }
                        else {
							// logger.finest("(loop 3): here 2: ");
							// logger.finest("accumulator: \n" + accumulator);
							// logger.finest("clause: \n" + clause);
                            accumulator = accumulator.appendClauseInCNF(clause);
							// logger.finest("accumulator after: \n" + accumulator);
                        }
						// logger.finest("(loop 3): here 3: ");
                        result.theFormula = accumulator.theFormula;
						// logger.finest("(loop 3): here 4: ");
                    }
                    result.theFormula = new String(result.appendClauseInCNF(thisFormula).theFormula);
                }
            }
        }
		// logger.finest("Result: " + result);
        if (result.theFormula != null)
            result.theFormula = Clausifier.toCanonicalClausalForm(result).theFormula;
        return mapping;
    }

    /** ***************************************************************
     *  Use a TreeMap of [varname, value] to substitute value in for
     *  varname wherever it appears in the formula.  This is
     *  iterative, since values can themselves contain varnames.
     */
    public Formula substitute(TreeMap<String,String> m) {

		// logger.finest("Replacing vars in " + this + " as per " + m);

		String newForm = null;
        Formula result = null;
        while (newForm == null || !newForm.equals(theFormula)) {
            newForm = theFormula;
            result = substituteVariables(m);
            theFormula = result.theFormula;
        }
        return this;
    }

    /** ***************************************************************
     *  A convenience method that collects all variables and returns
     *  a simple ArrayList of variables whether quantified or not.
     *
     * @see Formula.collectVariables()
     *
     * @return An ArrayList of String
     */
    public ArrayList<String> simpleCollectVariables() {

        ArrayList<String> result = new ArrayList();
        ArrayList<ArrayList<String>> ans = collectVariables();
        if (ans == null || ans.size() < 1) return null;
        ArrayList<String> ans1 = (ArrayList<String>) ans.get(0);
        if (ans1 == null) return null;
        result.addAll(ans1);
        if (ans.size() < 2) return result;
        ArrayList<String> ans2 = (ArrayList<String>) ans.get(1);
        if (ans2 == null)
            return result;
        result.addAll(ans2);
        return result;
    }

    /** ***************************************************************
     * Collects all variables in this Formula.  Returns an ArrayList
     * containing a pair of ArrayList.  The first contains all
     * explicitly quantified variables in the Formula.  The second
     * contains all variables in Formula that are not within the scope
     * of some explicit quantifier.
     *
     * @see Formula.collectVariables_1()
     *
     * @return An ArrayList containing two ArrayLists, each of which
     * could be empty
     */
    public ArrayList<ArrayList<String>> collectVariables() {
        /*
        long t1 = System.currentTimeMillis();
        */
		logger.entering("Formula", "collectVariables");
        ArrayList<ArrayList<String>> ans = new ArrayList<ArrayList<String>>();
        ans.add(new ArrayList());
        ans.add(new ArrayList());
        try {
            Set quantified = new HashSet();
            Set unquantified = new HashSet();
            collectVariables_1(this, quantified, unquantified);
            ans.get(0).addAll(quantified);
            ans.get(1).addAll(unquantified);
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        //System.out.println("EXIT Formula.collectVariables()");
        // System.out.println("  elapsed time == " + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");
		logger.exiting("Formula", "collectVariables", ans);
        return ans;
    }

    /** ***************************************************************
     * A utility method used internally by Formula.collectVariables().
     *
     * @see Formula.collectVariables()
     *
     * @param originalF The original top-level Formula
     *
     * @param quantified A Set for collecting all explicitly
     * quantified variables in originalF
     *
     * @param unquantified A Set for collecting all variables in
     * originalF that are not within the scope of an explicit
     * quantifier
     */
    private void collectVariables_1(Formula originalF, Set quantified, Set unquantified) {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "originalF = " + originalF.theFormula,
					"quantified = " + quantified,
					"unquantified = " + unquantified };
			logger.entering("Formula", "collectVariables_1", params);
		}

		try {
            if (this.theFormula.contains(V_PREF) || this.theFormula.contains(R_PREF)) {
                KBmanager mgr = KBmanager.getMgr();
                List quantifiers = Arrays.asList(UQUANT, EQUANT, KAPPAFN);
                Formula f = this;
                if (isVariable(f.theFormula) && !quantified.contains(f.theFormula)) {
                    unquantified.add(f.theFormula);
                }
                else if (f.listP() && !f.empty()) {
                    String arg0 = f.car();
                    if (quantifiers.contains(arg0)) {
                        String arg1 = f.getArgument(1);
                        if (arg0.equals(KAPPAFN) && isVariable(arg1)) {
                            arg1 = "(" + arg1 + ")";
                        }
                        Formula arg1F = new Formula();
                        arg1F.read(arg1);
                        if (arg1F.listP() && !arg1F.empty()) {
                            for (Iterator it = arg1F.literalToArrayList().iterator();
                                 it.hasNext();) {
                                String var = (String) it.next();
                                if (isVariable(var)
                                    && (quantified.contains(var) || unquantified.contains(var))) {
                                    String err = ("Possible quantification error for " + var + " in ");
									logger.warning(err + " \n" + originalF);

                                    mgr.setError(mgr.getError() + "\n<br/>" + err + "\n<br/>" + originalF + "\n<br/>");
                                }
                                quantified.add(var);
                            }
                            Formula arg2F = new Formula();
                            arg2F.read(f.getArgument(2));
                            arg2F.collectVariables_1(originalF, quantified, unquantified);
                        }
                    }
                    else {
                        Formula arg0F = new Formula();
                        arg0F.read(arg0);
                        arg0F.collectVariables_1(originalF, quantified, unquantified);
                        Formula cdrF = f.cdrAsFormula();
                        if (cdrF != null) {
                            cdrF.collectVariables_1(originalF, quantified, unquantified);
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
			logger.warning(ex.getMessage());
        }

		logger.exiting("Formula", "collectVariables_1");

        return;
    }

    /** ***************************************************************
     * Collect all of the quantified variables in the input, which is
     * the String representation of a Formula.
     *
     * @return An ArrayList of variables (Strings).
     */
    private ArrayList collectQuantifiedVariables() {
        return collectVariables().get(0);
    }

    /** ***************************************************************
     * Collect all the unquantified variables in a formula
     */
    private ArrayList collectUnquantifiedVariables() {
        return collectVariables().get(1);
    }

    /** ***************************************************************
     * Collect all the terms in a formula
     */
    public ArrayList<String> collectTerms() {

        ArrayList<String> result = new ArrayList();
        if (this.theFormula == null || this.theFormula == "") {
			logger.warning("No formula to collect terms from: " + this);
            return null;
        }
        if (this.empty())
            return result;
        if (this.atom())
            result.add(theFormula);
        else {
            Formula f = new Formula();
            f.read(theFormula);
            while (!f.empty() && f.theFormula != null && f.theFormula != "") {
                Formula f2 = new Formula();
                f2.read(f.car());
                result.addAll(f2.collectTerms());
                f.read(f.cdr());
            }
        }
        return result;
    }

    /** ***************************************************************
     * Makes implicit quantification explicit.
     *
     * @param query controls whether to add universal or existential
     * quantification.  If true, add existential.
     *
     * @result the formula as a String, with explicit quantification
     */
    public String makeQuantifiersExplicit(boolean query) {
        String result = this.theFormula;
        try {
            String arg0 = this.car();
            ArrayList<ArrayList<String>> vpair = collectVariables();
            ArrayList<String> quantVariables = vpair.get(0);
            ArrayList<String> unquantVariables = vpair.get(1);

            if (!unquantVariables.isEmpty()) {   // Quantify all the unquantified variables
                StringBuilder sb = new StringBuilder();
                sb.append((query ? "(exists (" : "(forall ("));
                boolean afterTheFirst = false;
                for (Iterator itu = unquantVariables.iterator(); itu.hasNext();) {
                    if (afterTheFirst) sb.append(" ");
                    sb.append(itu.next().toString());
                    afterTheFirst = true;
                }
                //System.out.println("INFO in Formula.makeQuantifiersExplicit(): result: " +
                //    quant.toString() + ") " + theFormula + ")");
                sb.append(") ");
                sb.append(this.theFormula);
                sb.append(")");
                result = sb.toString();
				logger.exiting("Formula", "makeQuantifiersExplicit", result);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
			logger.warning(ex.getMessage());
        }
        return result;
    }

    /** ***************************************************************
     * @param kb - The KB used to compute variable arity relations.
     *
     * @return Returns true if this Formula contains any variable
     * arity relations, else returns false.
     */
    protected boolean containsVariableArityRelation(KB kb) {

        boolean ans = false;
        try {
            Set relns = kb.getCachedRelationValues("instance", "VariableArityRelation", 2, 1);
            if (relns == null)
                relns = new HashSet();
            relns.addAll(KB.VA_RELNS);
            String r = null;
            Iterator it = relns.iterator();
            while (it.hasNext()) {
                r = (String) it.next();
                ans = (this.theFormula.indexOf(r) != -1);
                if (ans) { break; }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
			logger.warning(ex.getMessage());
        }
        return ans;
    }

    /** ***************************************************************
     * @param kb - The KB used to compute variable arity relations.
     * @param relationMap is a Map of String keys and values where
     *                    the key is the renamed relation and the
     *                    value is the original name.  This is set
     *                    as a side effect of this method.
     * @return A new version of the Formula in which every
     * VariableArityRelation has been renamed to include a numeric
     * suffix corresponding to the actual number of arguments in the
     * Formula.
     */
    protected Formula renameVariableArityRelations(KB kb, TreeMap<String,String> relationMap) {

        Formula result = this;
        try {
            if (this.listP()) {
                StringBuilder sb = new StringBuilder();
                Formula f = new Formula();
                f.read(this.theFormula);
                int flen = f.listLength();
                String suffix = ("_" + (flen - 1));
                String arg = null;
                sb.append("(");
                for (int i = 0 ; i < flen ; i++) {
                    arg = f.getArgument(i);
                    if (i > 0)
                        sb.append(" ");
                    if ((i == 0) && kb.isVariableArityRelation(arg) && !arg.endsWith(suffix)) {
                        relationMap.put(arg + suffix, arg);
                        arg += suffix;
                    }
                    else if (listP(arg)) {
                        Formula argF = new Formula();
                        argF.read(arg);
                        arg = argF.renameVariableArityRelations(kb,relationMap).theFormula;
                    }
                    sb.append(arg);
                }
                sb.append(")");
                f = new Formula();
                f.read(sb.toString());
                result = f;
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
     * Gathers the row variable names in this.theFormula and returns
     * them in a TreeSet.
     *
     * @return a TreeSet, possibly empty, containing row variable
     * names, each of which will start with the row variable
     * designator '@'.
     */
    private TreeSet findRowVars() {

        TreeSet result = new TreeSet();
        try {
            if (isNonEmptyString(this.theFormula)
                && this.theFormula.contains(R_PREF)) {
                Formula f = new Formula();
                f.read(this.theFormula);
                while (f.listP() && !f.empty()) {
                    String arg = f.getArgument(0);
                    if (arg.startsWith(R_PREF))
                        result.add(arg);
                    else {
                        Formula argF = new Formula();
                        argF.read(arg);
                        if (argF.listP())
                            result.addAll(argF.findRowVars());
                    }
                    f.read(f.cdr());
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
     * Expand row variables, keeping the information about the original
     * source formula.  Each variable is treated like a macro that
     * expands to up to seven regular variables.  For example
     *
     * (=>
     *    (and
     *       (subrelation ?REL1 ?REL2)
     *       (holds__ ?REL1 @ROW))
     *    (holds__ ?REL2 @ROW))
     *
     * would become
     *
     * (=>
     *    (and
     *       (subrelation ?REL1 ?REL2)
     *       (holds__ ?REL1 ?ARG1))
     *    (holds__ ?REL2 ?ARG1))
     *
     * (=>
     *    (and
     *       (subrelation ?REL1 ?REL2)
     *       (holds__ ?REL1 ?ARG1 ?ARG2))
     *    (holds__ ?REL2 ?ARG1 ?ARG2))
     * etc.
     *
     * @return an ArrayList of Formulas, or an empty ArrayList.
     */
    public ArrayList expandRowVars(KB kb) {       
    	
		logger.entering("Formula", "expandRowVars", kb.name);

		ArrayList resultList = new ArrayList();
        try {
            TreeSet rowVars = (this.theFormula.contains(R_PREF)
                               ? this.findRowVars()
                               : null);
            // If this Formula contains no row vars to expand, we just
            // add it to resultList and quit.
            if ((rowVars == null) || rowVars.isEmpty()) {
                resultList.add(this);
            }
            else {
                Formula f = new Formula();
                f.read(this.theFormula);
                Set accumulator = new LinkedHashSet();
                accumulator.add(f);
                List working = new ArrayList();
                long t1 = 0L;

                // Iterate through the row variables
                String rowvar = null;
                for (Iterator irv = rowVars.iterator(); irv.hasNext();) {
                    rowvar = (String) irv.next();

					// logger.finest("rowvar ==" + rowvar);
					// logger.finest("accumulator == " + accumulator);

                    working.clear();
                    working.addAll(accumulator);
                    accumulator.clear();

                    String fstr = null;
                    for (Iterator itw = working.iterator(); itw.hasNext();) {
                        f = (Formula) itw.next();
                        fstr = f.theFormula;
                        if (!fstr.contains(R_PREF)
                            || (fstr.indexOf("\"") > -1)) {
                            f.sourceFile = this.sourceFile;
                            resultList.add(f);
                        }
                        else {
                            t1 = System.currentTimeMillis();
                            int[] range = f.getRowVarExpansionRange(kb, rowvar);
                            // Increment the timer for getRowVarExpansionRange().
                            KB.ppTimers[3] += (System.currentTimeMillis() - t1);
                            boolean hasVariableArityRelation = (range[0] == 0);
                            t1 = System.currentTimeMillis();
                            range[1] = adjustExpansionCount(hasVariableArityRelation, range[1], rowvar);
                            // Increment the timer for adjustExpansionCount().
                            KB.ppTimers[5] += (System.currentTimeMillis() - t1);
                            Formula newF = null;
                            StringBuilder varRepl = new StringBuilder();

                            for (int j = 1 ; j < range[1] ; j++) {
                                if (varRepl.length() > 0)
                                    varRepl.append(" ");
                                varRepl.append("?");
                                varRepl.append(rowvar.substring(1));
                                // varRepl.append("_");
                                varRepl.append(Integer.toString(j));
                                if (hasVariableArityRelation) {
                                    newF = new Formula();
                                    newF.read(fstr.replaceAll(rowvar, varRepl.toString()));
                                    // Copy the source file information for each expanded formula.
                                    newF.sourceFile = this.sourceFile;
                                    if (newF.theFormula.contains(R_PREF)
                                        && (newF.theFormula.indexOf("\"") == -1)) {
                                        accumulator.add(newF);
                                    }
                                    else
                                        resultList.add(newF);
                                }
                            }
                            if (!hasVariableArityRelation) {
                                newF = new Formula();
                                newF.read(fstr.replaceAll(rowvar, varRepl.toString()));
                                // Copy the source file information for each expanded formula.
                                newF.sourceFile = this.sourceFile;
                                if (newF.theFormula.contains(R_PREF)
                                    && (newF.theFormula.indexOf('"') == -1)) {
                                    accumulator.add(newF);
                                }
                                else
                                    resultList.add(newF);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }        

		logger.exiting("Formula", "expandRowVars", resultList);

        return resultList;
    }

    /** ***************************************************************
     * This method attempts to revise the number of row var expansions
     * to be done, based on the occurrence of forms such as (<pred>
     * @ROW1 ?ITEM).  Note that variables such as ?ITEM throw off the
     * default expected expansion count, and so must be dealt with to
     * prevent unnecessary expansions.
     *
     * @param variableArity Indicates whether the overall expansion
     * count for the Formula is governed by a variable arity relation,
     * or not.
     *
     * @param count The default expected expansion count, possibly to
     * be revised.
     *
     * @param var The row variable to be expanded.
     *
     * @return An int value, the revised expansion count.  In most
     * cases, the count will not change.    
     */
    private int adjustExpansionCount(boolean variableArity, int count, String var) {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "variableArity = " + variableArity,
					"count = " + count, "var = " + var };
			logger.entering("Formula", "adjustExpansionCount", params);
		}

        int revisedCount = count;
        try {
            if (isNonEmptyString(var)) {
                String rowVar = var;
                if (!var.startsWith("@"))
                    rowVar = ("@" + var);
                List accumulator = new ArrayList();
                List working = new ArrayList();
                if (this.listP() && !this.empty())
                    accumulator.add(this);
                while (!accumulator.isEmpty()) {
                    working.clear();
                    working.addAll(accumulator);
                    accumulator.clear();
                    for (int i = 0 ; i < working.size() ; i++) {
                        Formula f = (Formula) working.get(i);
                        List literal = f.literalToArrayList();
						// logger.finest("Literal: " + literal);

                        int len = literal.size();
                        if (literal.contains(rowVar) && !isVariable(f.car())) {
                            if (!variableArity && (len > 2))
                                revisedCount = (count - (len - 2));
                            else if (variableArity)
                                revisedCount = (10 - len);
                        }
                        if (revisedCount < 2) {
                            revisedCount = 2;
                        }
                        while (!f.empty()) {
                            String arg = f.car();
                            Formula argF = new Formula();
                            argF.read(arg);
                            if (argF.listP() && !argF.empty())
                                accumulator.add(argF);
                            f = f.cdrAsFormula();
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
			logger.warning(ex.getMessage());
        }

		logger.exiting("Formula", "adjustExpansionCount", revisedCount);
        return revisedCount;
    }

    /** ***************************************************************
     * Returns a two-place int[] indicating the low and high points of
     * the expansion range (number of row var instances) for the input
     * row var.
     *
     * @param kb A KB required for processing.
     *
     * @param rowVar The row var (String) to be expanded.
     *
     * @return A two-place int[] object.  The int[] indicates a
     * numeric range.  int[0] holds the start (lowest number) in the
     * range, and int[1] holds the highest number.  The default is
     * [1,8].  If the Formula does not contain     
     */
    private int[] getRowVarExpansionRange(KB kb, String rowVar) {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "kb = " + kb.name, "rowVar = " + rowVar };
			logger.entering("Formula", "getRowVarExpansionRange", params);
		}

        int[] ans = new int[2];
        ans[0] = 1;
        ans[1] = 8;
        try {
            if (isNonEmptyString(rowVar)) {
                String var = rowVar;
                if (!var.startsWith("@"))
                    var = "@" + var;
                Map minMaxMap = this.getRowVarsMinMax(kb);
                int[] newArr = (int[]) minMaxMap.get(var);
                if (newArr != null)
                    ans = newArr;
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
			logger.warning(ex.getMessage());
        }

		logger.exiting("Formula", "getRowVarExpansionRange", ans);

        return ans;
    }

    /** ***************************************************************
     * Applied to a SUO-KIF Formula with row variables, this method
     * returns a Map containing an int[] of length 2 for each row var
     * that indicates the minimum and maximum number of row var
     * expansions to perform.
     *
     * @param kb A KB required for processing.
     *
     * @return A Map in which the keys are distinct row variables and
     * the values are two-place int[] objects.  The int[] indicates a
     * numeric range.  int[0] is the start (lowest number) in the
     * range, and int[1] is the end.  If the Formula contains no row
     * vars, the Map is empty.
     */
    private Map getRowVarsMinMax(KB kb) {

		logger.entering("Formula", "getRowVarsMinMax", kb.name);

		Map ans = new HashMap();
        try {
            long t1 = System.currentTimeMillis();
            ArrayList clauseData = this.getTheClausalForm();
            // if (trace) System.out.println("  getTheClausalForm() == " + clauseData);
            // Increment the timer for toNegAndPosLitsWithRenameInfo().
            KB.ppTimers[4] += (System.currentTimeMillis() - t1);
            if (!((clauseData instanceof ArrayList) && (clauseData.size() > 2)))
                return ans;
            // System.out.println("\nclauseData == " + clauseData + "\n");
            ArrayList clauses = (ArrayList) clauseData.get(0);
            // System.out.println("\nclauses == " + clauses + "clauses.size() == " + clauses.size() + "\n");
            if (!(clauses instanceof ArrayList) || clauses.isEmpty())
                return ans;
            Map varMap = (Map) clauseData.get(2);
            Map rowVarRelns = new HashMap();
            for (int i = 0 ; i < clauses.size() ; i++) {
                ArrayList clause = (ArrayList) clauses.get(i);
                // if (trace) System.out.println("  clause == " + clause);
                if ((clause != null) && !clause.isEmpty()) {
                    // First we get the neg lits.  It may be that
                    // we should use *only* the neg lits for this
                    // task, but we will start by combining the neg
                    // lits and pos lits into one list of literals
                    // and see how that works.
                    ArrayList literals = (ArrayList) clause.get(0);
                    ArrayList posLits = (ArrayList) clause.get(1);
                    literals.addAll(posLits);
                    // if (trace) System.out.println("  literals == " + literals);
                    for (Iterator itl = literals.iterator(); itl.hasNext();) {
                        Formula litF = (Formula) itl.next();;
                        litF.computeRowVarsWithRelations(rowVarRelns, varMap);
                    }
                }
				// logger.finest("rowVarRelns == " + rowVarRelns);
                if (!rowVarRelns.isEmpty()) {
                    for (Iterator kit = rowVarRelns.keySet().iterator(); kit.hasNext();) {
                        String rowVar = (String) kit.next();
                        String origRowVar = Clausifier.getOriginalVar(rowVar, varMap);
                        int[] minMax = (int[]) ans.get(origRowVar);
                        if (minMax == null) {
                            minMax = new int[2];
                            minMax[0] = 0;
                            minMax[1] = 8;
                            ans.put(origRowVar, minMax);
                        }
                        TreeSet val = (TreeSet) rowVarRelns.get(rowVar);
                        for (Iterator vit = val.iterator(); vit.hasNext();) {
                            String reln = (String) vit.next();
                            int arity = kb.getValence(reln);
                            if (arity < 1) {
                                // It's a VariableArityRelation or we
                                // can't find an arity, so do nothing.
                                ;
                            }
                            else {
                                minMax[0] = 1;
                                int arityPlusOne = (arity + 1);
                                if (arityPlusOne < minMax[1])
                                    minMax[1] = arityPlusOne;
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
			logger.warning(ex.getMessage());
        }

		logger.exiting("Formula", "getRowVarsMinMax", ans);

        return ans;
    }

    /** ***************************************************************
     * Finds all the relations in this Formula that are applied to row
     * variables, and for which a specific arity might be computed.
     * Note that results are accumulated in varsToRelns, and the
     * variable correspondences (if any) in varsToVars are used to
     * compute the results.
     *
     * @param varsToRelns A Map for accumulating row var data for one
     * Formula literal.  The keys are row variables (Strings) and the
     * values are TreeSets containing relations (Strings) that might
     * help to constrain the row var during row var expansion.
     *
     * @param varsToVars A Map of variable correspondences, the leaves
     * of which might include row variables
     *
     * @return void
     * */
    protected void computeRowVarsWithRelations(Map varsToRelns, Map varsToVars) {

        try {
            Formula f = this;
            if (f.listP() && !f.empty()) {
                String relation = f.car();
                if (!isVariable(relation) && !relation.equals(SKFN)) {
                    Formula newF = f.cdrAsFormula();
                    while (newF.listP() && !newF.empty()) {
                        String term = newF.car();
                        String rowVar = term;
                        if (isVariable(rowVar)) {
                            if (rowVar.startsWith(V_PREF) && (varsToVars != null))
                                rowVar = Clausifier.getOriginalVar(term, varsToVars);
                        }
                        if (rowVar.startsWith(R_PREF)) {
                            TreeSet relns = (TreeSet) varsToRelns.get(term);
                            if (relns == null) {
                                relns = new TreeSet();
                                varsToRelns.put(term, relns);
                                varsToRelns.put(rowVar, relns);
                            }
                            relns.add(relation);
                        }
                        else {
                            Formula termF = new Formula();
                            termF.read(term);
                            termF.computeRowVarsWithRelations(varsToRelns, varsToVars);
                        }
                        newF = newF.cdrAsFormula();
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
			logger.warning(ex.getMessage());
        }
        return;
    }

    /** ***************************************************************
     * Returns a HashMap in which the keys are the Relation constants
     * gathered from this Formula, and the values are ArrayLists in
     * which the ordinal positions 0 - n are occupied by the names of
     * the corresponding argument types.  n should never be greater
     * than the value of Formula.MAX_PREDICATE_ARITY.  For each
     * Predicate key, the length of its ArrayList should be equal to
     * the predicate's valence + 1.  For each Function, the length of
     * its ArrayList should be equal to its valence.  Only Functions
     * will have argument types in the 0th position of the ArrayList,
     * since this position contains a function's range type.  This
     * means that all Predicate ArrayLists will contain at least one
     * null value.  A null value will also be added to the nth
     * position of an ArrayList when no value can be obtained for that
     * position.
     *
     * @return A HashMap that maps every Relation occurring in this
     * Formula to an ArrayList indicating the Relation's argument
     * types.  Some HashMap keys may map to null values or empty
     * ArrayLists, and most ArrayLists will contain some null values.
     */
    public HashMap<String, ArrayList> gatherRelationsWithArgTypes(KB kb) {

        HashMap<String, ArrayList> argtypemap = new HashMap<String, ArrayList>();
        try {
            Set<String> relations = gatherRelationConstants();
            for (String r : relations) {
                int atlen = (Formula.MAX_PREDICATE_ARITY + 1);
                ArrayList argtypes = new ArrayList();
                for (int i = 0; i < atlen; i++) {
                    argtypes.add(kb.getArgType(r, i));
                }
                argtypemap.put(r, argtypes);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
			logger.warning(ex.getMessage());
        }
        return argtypemap;
    }

    /** ***************************************************************
     * Returns a HashSet of all atomic KIF Relation constants that
     * occur as Predicates or Functions (argument 0 terms) in this
     * Formula.
     *
     * @return a HashSet containing the String constants that denote
     * KIF Relations in this Formula, or an empty HashSet.
     */
    public HashSet<String> gatherRelationConstants() {

        HashSet<String> relations = new HashSet<String>();
        try {
            Set<String> accumulator = new HashSet<String>();
            if (this.listP() && !this.empty())
                accumulator.add(this.theFormula);
            List<String> kifLists = new ArrayList<String>();
            Formula f = null;
            while (!accumulator.isEmpty()) {
                kifLists.clear();
                kifLists.addAll(accumulator);
                accumulator.clear();
                String klist = null;
                for (Iterator it = kifLists.iterator(); it.hasNext();) {
                    klist = (String) it.next();
                    if (listP(klist)) {
                        f = new Formula();
                        f.read(klist);
                        for (int i = 0; !f.empty(); i++) {
                            String arg = f.car();
                            if (listP(arg)) {
                                if (!empty(arg)) accumulator.add(arg);
                            }
                            else if (isQuantifier(arg)) {
                                accumulator.add(f.getArgument(2));
                                break;
                            }
                            else if ((i == 0)
                                     && !isVariable(arg)
                                     && !isLogicalOperator(arg)
                                     && !arg.equals(SKFN)
                                     && !StringUtil.isQuotedString(arg)
                                     && !arg.matches(".*\\s.*")) {
                                relations.add(arg);
                            }
                            f = f.cdrAsFormula();
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return relations;
    }

    /** ***************************************************************
     * Convert an ArrayList of Formulas to an ArrayList of Strings.
     */
    private ArrayList formulasToStrings(ArrayList list) {

        ArrayList result = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            result.add(((Formula) list.get(i)).theFormula);
        }
        return result;
    }

    /** ***************************************************************
     * Test whether a Formula is a functional term
     */
    public boolean isFunctionalTerm() {

        boolean ans = false;
        if (this.listP()) {
            String pred = this.car();
            ans = ((pred.length() > 2) && pred.endsWith(FN_SUFF));
        }
        return ans;
    }

    /** ***************************************************************
     * Test whether a Formula is a functional term
     */
    public static boolean isFunctionalTerm(String s) {

        Formula f = new Formula();
        f.read(s);
        return f.isFunctionalTerm();
    }

    /** ***************************************************************
     * Test whether a Formula contains a Formula as an argument to
     * other than a logical operator.
     */
    public boolean isHigherOrder() {

        if (this.listP()) {
            String pred = this.car();
            boolean logop = isLogicalOperator(pred);
            ArrayList al = literalToArrayList();
            for (int i = 1; i < al.size(); i++) {
                String arg = (String) al.get(i);
                Formula f = new Formula();
                f.read(arg);
                if (!atom(arg) && !f.isFunctionalTerm()) {
                    if (logop) {
                        if (f.isHigherOrder())
                            return true;
                    }
                    else
                        return true;
                }
            }
        }
        return false;
    }

    /** ***************************************************************
     * Test whether an Object is a variable
     */
    public static boolean isVariable(Object term) {

        return (isNonEmptyString(term)
                && (((String)term).startsWith(V_PREF)
                    || ((String)term).startsWith(R_PREF)));
    }

    /** ***************************************************************
     * Test whether the formula is a variable
     */
    public  boolean isVariable() {
        return isVariable(theFormula);
    }

    /** ***************************************************************
     * Returns true only if this Formula, explicitly quantified or
     * not, starts with "=>" or "<=>", else returns false.  It would
     * be better to test for the occurrence of at least one positive
     * literal with one or more negative literals, but this test would
     * require converting the Formula to clausal form.
     */
    public boolean isRule() {
    	
        boolean ans = false;
        try {
            if (this.listP()) {
                String arg0 = this.car();
                if (isQuantifier(arg0)) {
                    String arg2 = this.getArgument(2);
                    if (Formula.listP(arg2)) {
                        Formula newF = new Formula();
                        newF.read(arg2);
                        ans = newF.isRule();
                    }
                }
                else {
                    ans = Arrays.asList(IF, IFF).contains(arg0);
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     * Test whether a list with a predicate is a quantifier list
     */
    private static boolean isQuantifierList(String listPred, String previousPred) {

        return ((previousPred.equals(EQUANT) || previousPred.equals(UQUANT)) &&
                (listPred.startsWith(R_PREF) || listPred.startsWith(V_PREF)));
    }

    /** ***************************************************************
     * Test whether a Formula is a simple list of terms (including
     * functional terms).
     */
    public boolean isSimpleClause() {

		logger.entering("Formula", "isSimpleClause");
        Formula f = new Formula();
        f.read(theFormula);
        while (!f.empty()) {
            if (listP(f.car())) {
                Formula f2 = new Formula();
                f2.read(f.car());
                if (!Formula.isFunction(f2.car()))
                    return false;
                else if (!f2.isSimpleClause())
                    return false;
            }
            f.read(f.cdr());
        }
        return true;
    }

    /** ***************************************************************
     * Test whether a Formula is a simple clause wrapped in a
     * negation.
     */
    public boolean isSimpleNegatedClause() {


        //System.out.println("INFO in Formula.isSimpleNegatedClause(): " + this);
        Formula f = new Formula();
        f.read(theFormula);
        if (f == null || f.empty() || f.atom())
            return false;
        if (f.car().equals("not")) {
            f.read(f.cdr());
            if (empty(f.cdr())) {
                f.read(f.car());
                return f.isSimpleClause();
            }
            else
                return false;
        }
        else
            return false;
    }

    /** ***************************************************************
     * Test whether a predicate is a logical quantifier
     */
    public static boolean isQuantifier(String pred) {

        return (isNonEmptyString(pred)
                && (pred.equals(EQUANT)
                    || pred.equals(UQUANT)));
    }

    /** ***************************************************************
     * A static utility method.
     * @param obj Any object, but should be a String.
     * @return true if obj is a SUO-KIF commutative logical operator,
     * else false.
     */
    public static boolean isCommutative(String obj) {

        return (isNonEmptyString(obj)
                && (obj.equals(AND)
                    || obj.equals(OR)));
    }

    /** ***************************************************************
     * Returns the dual logical operator of op, or null if op is not
     * an operator or has no dual.
     *
     * @param term A String, assumed to be a SUO-KIF logical operator
     *
     * @return A String, the dual operator of op, or null.
     */
    protected static String getDualOperator(String op) {
    	
        String ans = null;
        if (op instanceof String) {
            String[][] duals = { { UQUANT, EQUANT },
                                 { EQUANT, UQUANT },
                                 { AND,    OR     },
                                 { OR,     AND    },
                                 { NOT,    ""     },
                                 { "",     NOT    },
                                 { LOG_TRUE,  LOG_FALSE  },
                                 { LOG_FALSE, LOG_TRUE   }
            };
            for (int i = 0; i < duals.length; i++) {
                if (op.equals(duals[i][0])) ans = duals[i][1];
            }
        }
        return ans;
    }

    /** ***************************************************************
     * Returns true if term is a standard FOL logical operator, else
     * returns false.
     *
     * @param term A String, assumed to be an atomic SUO-KIF term.
     */
    public static boolean isLogicalOperator(String term) {

        return (isNonEmptyString(term) && LOGICAL_OPERATORS.contains(term));
    }

    /** ***************************************************************
     * Returns true if term is a valid SUO-KIF term, else
     * returns false.
     *
     * @param term A String, assumed to be an atomic SUO-KIF term.
     */
    public static boolean isTerm(String term) {

        if (isNonEmptyString(term) && !listP(term) &&
                Character.isJavaIdentifierStart(term.charAt(0))) {
            for (int i = 0; i < term.length(); i++) {
                if (!Character.isJavaIdentifierPart(term.charAt(i)))
                    return false;
            }
            return true;
        }
        else
            return false;
    }

    /** ***************************************************************
     * Returns true if term is a SUO-KIF predicate for comparing two
     * (typically numeric) terms, else returns false.
     *
     * @param term A String.
     */
    public static boolean isComparisonOperator(String term) {

        return (isNonEmptyString(term) && COMPARISON_OPERATORS.contains(term));
    }

    /** ***************************************************************
     * Returns true if term is a SUO-KIF mathematical function, else
     * returns false.
     *
     * @param term A String.
     */
    public static boolean isMathFunction(String term) {

        return (isNonEmptyString(term) && MATH_FUNCTIONS.contains(term));
    }

    /** ***************************************************************
     * Returns true if formula is a valid formula with no variables,
     * else returns false.
     */
    public static boolean isGround(String form) {

        if (form == null || form == "")
            return false;
        if (form.indexOf("\"") < 0)
            return (form.indexOf("?") < 0 && form.indexOf("@") < 0);
        boolean inQuote = false;
        for (int i = 0; i < form.length(); i++) {
            if (form.charAt(i) == '"')
                inQuote = !inQuote;
            if ((form.charAt(i) == '?' || form.charAt(i) == '@') && !inQuote)
                return false;
        }
        return true;
    }

    /** ***************************************************************
     * Returns true if formula has variable, else returns false.
     */
    public boolean isGround() {
        return isGround(theFormula);
    }

    /** ***************************************************************
     * Returns true if term is a SUO-KIF function, else returns false.
     * Note that this test is purely syntactic, and could fail for
     * functions that do not adhere to the convention of ending all
     * functions with "Fn".
     *
     * @param term A String.
     */
    public static boolean isFunction(String term) {
        return (isNonEmptyString(term) && term.endsWith(FN_SUFF));
    }

    /** ***************************************************************
     * Returns true if term is a SUO-KIF Skolem term, else returns false.
     *
     * @param term A String.
     *
     * @return true or false
     */
    public static boolean isSkolemTerm(String term) {
        return (isNonEmptyString(term)
                && term.trim().matches("^.?" + SK_PREF + "\\S*\\s*\\d+"));
    }

    /** ***************************************************************
     * @param obj Any object
     * @return true if obj is a non-empty String, else false.
     */
    public static boolean isNonEmptyString(Object obj) {
        return StringUtil.isNonEmptyString(obj);
    }

    /** ***************************************************************
     * @return An ArrayList (ordered tuple) representation of the
     * Formula, in which each top-level element of the Formula is
     * either an atom (String) or another list.
     */
    public ArrayList literalToArrayList() {
        ArrayList tuple = new ArrayList();
        try {
            Formula f = this;
            if (f.listP()) {
                while (!f.empty()) {
                    tuple.add(f.car());
                    f = f.cdrAsFormula();
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return tuple;
    }

    /** ***************************************************************
     * A + is appended to the type if the parameter must be a class
     *
     * @return the type for each argument to the given predicate, where
     * ArrayList element 0 is the result, if a function, 1 is the
     * first argument, 2 is the second etc.
     */
    private ArrayList getTypeList(String pred, KB kb) {

        //System.out.println("INFO in Formula.getTypeList(): pred: " + pred);
        ArrayList result = null;
        try {
            // build the sortalTypeCache key.
            StringBuilder sb = new StringBuilder("gtl");
            sb.append(pred);
            sb.append(kb.name);
            String key = sb.toString();
            Map stc = kb.getSortalTypeCache();
            result = (ArrayList) (stc.get(key));
            if (result == null) {
                result = new ArrayList();
                int valence = kb.getValence(pred);
                int len = MAX_PREDICATE_ARITY + 1;
                if (valence == 0) {
                    len = 2;
                }
                else if (valence > 0) {
                    len = valence + 1;
                }
                String[] r = new String[len];

                ArrayList al = kb.askWithRestriction(0,"domain",1,pred);
                ArrayList al2 = kb.askWithRestriction(0,"domainSubclass",1,pred);
                ArrayList al3 = kb.askWithRestriction(0,"range",1,pred);
                ArrayList al4 = kb.askWithRestriction(0,"rangeSubclass",1,pred);
                r = addToTypeList(pred,al,r,false);
                r = addToTypeList(pred,al2,r,true);
                r = addToTypeList(pred,al3,r,false);
                r = addToTypeList(pred,al4,r,true);
                for (int i = 0; i < r.length; i++)
                    result.add(r[i]);
                stc.put(key, result);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
     * A utility helper method for computing predicate data types.
     */
    private String[] addToTypeList(String pred, ArrayList al, String[] result, boolean classP) {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "pred = " + pred, "al = " + al,
					"result = " + result, "classP = " + classP };
			logger.entering("Formula", "addToTypeList", params);
		}
    	
        try {
            Formula f = null;
            // If the relations in al start with "(range", argnum will
            // be 0, and the arg position of the desired classnames
            // will be 2.
            int argnum = 0;
            int clPos = 2;
            for (int i = 0; i < al.size(); i++) {
                f = (Formula) al.get(i);
				// logger.finest("theFormula: " + f.theFormula);
                if (f.theFormula.startsWith("(domain")) {
                    argnum = Integer.parseInt(f.getArgument(2));
                    clPos = 3;
                }
                String cl = f.getArgument(clPos);
                String errStr = null;
                String mgrErrStr = null;
                if ((argnum < 0) || (argnum >= result.length)) {
                    errStr = "Possible arity confusion for " + pred;
                    mgrErrStr = KBmanager.getMgr().getError();
					logger.warning(errStr);
                    if (mgrErrStr.equals("") || (mgrErrStr.indexOf(errStr) == -1)) {
                        KBmanager.getMgr().setError(mgrErrStr + "\n<br/>" + errStr + "\n<br/>");
                    }
                }
                else if (StringUtil.emptyString(result[argnum])) {
                    if (classP) { cl += "+"; }
                    result[argnum] = cl;
                }
                else {
                    if (!cl.equals(result[argnum])) {
                        errStr = ("Multiple types asserted for argument " + argnum
                                  + " of " + pred + ": " + cl + ", " + result[argnum]);
                        mgrErrStr = KBmanager.getMgr().getError();
						logger.warning(errStr);
                        if (mgrErrStr.equals("") || (mgrErrStr.indexOf(errStr) == -1)) {
                            KBmanager.getMgr().setError(KBmanager.getMgr().getError()
                                                        + "\n<br/>" + errStr + "\n<br/>");
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
     * Find the argument type restriction for a given predicate and
     * argument number that is inherited from one of its
     * super-relations.  A "+" is appended to the type if the
     * parameter must be a class.  Argument number 0 is used for the
     * return type of a Function.
     */
   public static String findType(int numarg, String pred, KB kb) {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "numarg = " + numarg, "pred = " + pred,
					"kb = " + kb.name };
			logger.entering("Formula", "findType", params);
		}
        String result = null;
        boolean isCached = false;
        boolean cacheResult = false;
        try {
            // build the sortalTypeCache key.
            StringBuilder sb = new StringBuilder("ft");
            sb.append(numarg);
            sb.append(pred);
            sb.append(kb.name);
            String key = sb.toString();
            Map stc = kb.getSortalTypeCache();
            result = (String) (stc.get(key));
            isCached = (result != null);
            cacheResult = !isCached;

            if (result == null) {
                boolean found = false;
                Set<String> accumulator = new HashSet<String>();
                accumulator.add(pred);
                List<String> relations = new ArrayList<String>();

                while (!found && !accumulator.isEmpty()) {
                    relations.clear();
                    relations.addAll(accumulator);
                    accumulator.clear();
                    Iterator it = relations.iterator();

                    while (!found && it.hasNext()) {
                        String r = (String) it.next();
                        List<Formula> formulas = null;
                        if (numarg > 0) {
                            formulas = kb.askWithRestriction(0,"domain",1,r);
                            for (Formula f : formulas) {
                                int argnum = Integer.parseInt(f.getArgument(2));
                                if (argnum == numarg) {
                                    result = f.getArgument(3);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                formulas = kb.askWithRestriction(0,"domainSubclass",1,r);
                                for (Formula f : formulas) {
                                    int argnum = Integer.parseInt(f.getArgument(2));
                                    if (argnum == numarg) {
                                        result = (f.getArgument(3) + "+");
                                        found = true;
                                        break;
                                    }
                                }
                            }
                        }
                        else if (numarg == 0) {
                            formulas = kb.askWithRestriction(0,"range",1,r);
                            if (!formulas.isEmpty()) {
                                Formula f = (Formula) formulas.get(0);
                                result = f.getArgument(2);
                                found = true;
                            }
                            if (!found) {
                                formulas = kb.askWithRestriction(0,"rangeSubclass",1,r);
                                if (!formulas.isEmpty()) {
                                    Formula f = (Formula) formulas.get(0);
                                    result = (f.getArgument(2) + "+");
                                    found = true;
                                }
                            }
                        }
                    }
                    if (!found) {
                        for (String r : relations)
                            accumulator.addAll(kb.getTermsViaAskWithRestriction(1,r,0,"subrelation",2));
                    }
                }
                if (cacheResult && (result != null))
                    stc.put(key, result);
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }

		logger.exiting("Formula", "findType", result);
        return result;
    }

    /** ***************************************************************
     * This method tries to remove all but the most specific relevant
     * classes from a List of sortal classes.
     *
     * @param types A List of classes (class name Strings) that
     * constrain the value of a SUO-KIF variable.
     *
     * @param kb The KB used to determine if any of the classes in the
     * List types are redundant.
     *
     * @return void
     */
    private void winnowTypeList(List types, KB kb) {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "types = " + types, "kb = " + kb.name };
			logger.entering("Formula", "winnowTypeList", params);
		}

		long t1 = 0L;
        try {
            if ((types instanceof List) && (types.size() > 1)) {
                Object[] valArr = types.toArray();
                String clX = null;
                String clY = null;
                for (int i = 0; i < valArr.length; i++) {
                    boolean stop = false;
                    for (int j = 0; j < valArr.length; j++) {
                        if (i != j) {
                            clX = (String) valArr[i];
                            clY = (String) valArr[j];
                            if (kb.isSubclass(clX, clY)) {
                                types.remove(clY);
                                if (types.size() < 2) {
                                    stop = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (stop) break;
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }

		logger.exiting("Formula", "winnowTypeList");
        return;
    }

    /** ***************************************************************
     * Does much of the real work for addTypeRestrictions() by
     * recursing through the Formula and collecting type constraint
     * information for the variable var.
     *
     * @param ios A List of classes (class name Strings) of which any
     * binding for var must be an instance.
     *
     * @param scs A List of classes (class name Strings) of which any
     * binding for var must be a subclass.
     *
     * @param var A SUO-KIF variable.
     *
     * @param kb The KB used to determine predicate and variable arg
     * types.
     *
     * @return void
     */
    private void computeTypeRestrictions(List ios, List scs, String var, KB kb) {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "ios = " + ios, "scs = " + scs, "var = " + var,
					"kb = " + kb.name };
			logger.entering("Formula", "computeTypeRestrictions", params);
		}

		String pred = null;
        try {
            if (!this.listP() || !this.theFormula.contains(var))
                return;
            Formula f = new Formula();
            f.read(this.theFormula);
            pred = f.car();
            if (isQuantifier(pred)) {
                String arg2 = f.getArgument(2);
                if (arg2.contains(var)) {
                    Formula nextF = new Formula();
                    nextF.read(arg2);
                    nextF.computeTypeRestrictions(ios, scs, var, kb);
                }
            }
            else if (isLogicalOperator(pred)) {
                int len = f.listLength();
                for (int i = 1; i < len; i++) {
                    String argI = f.getArgument(i);
                    if (argI.contains(var)) {
                        Formula nextF = new Formula();
                        nextF.read(argI);
                        nextF.computeTypeRestrictions(ios, scs, var, kb);
                    }
                }
            }
            else {
                int len = f.listLength();
                int valence = kb.getValence(pred);
                List types = getTypeList(pred,kb);
                int numarg = 0;
                for (int i = 1; i < len; i++) {
                    numarg = i;
                    if (valence == 0) // pred is a VariableArityRelation
                        numarg = 1;                   
                    String arg = f.getArgument(i);
                    if (arg.contains(var)) {
                        if (listP(arg)) {
                            Formula nextF = new Formula();
                            nextF.read(arg);
                            nextF.computeTypeRestrictions(ios, scs, var, kb);
                        }
                        else if (var.equals(arg)) {
                            String type = null;
                            if (numarg < types.size()) 
                                type = (String) types.get(numarg);                            
                            if (type == null) 
                                type = findType(numarg,pred,kb);                            
                            if (StringUtil.isNonEmptyString(type) && !type.startsWith("Entity")) {
                                boolean sc = false;
                                while (type.endsWith("+")) {
                                    sc = true;
                                    type = type.substring(0, type.length() - 1);
                                }
                                if (sc) {
                                    if (!scs.contains(type)) 
                                        scs.add(type);                                    
                                }
                                else if (!ios.contains(type)) 
                                    ios.add(type);                                
                            }
                        }
                    }
                }

                String arg1 = null;
                String arg2 = null;
                String term = null;
                String cl = null;

                // Special treatment for equal
                if (pred.equals("equal")) {
                    arg1 = f.getArgument(1);
                    arg2 = f.getArgument(2);
                    if (var.equals(arg1)) { term = arg2; }
                    else if (var.equals(arg2)) { term = arg1; }
                    if (isNonEmptyString(term)) {
                        if (listP(term)) {
                            Formula nextF = new Formula();
                            nextF.read(term);
                            if (nextF.isFunctionalTerm()) {
                                String fn = nextF.car();
                                List classes = getTypeList(fn, kb);
                                if (!classes.isEmpty()) 
                                    cl = (String) classes.get(0);                                
                                if (cl == null) 
                                    cl = findType(0, fn, kb);                                
                                if (StringUtil.isNonEmptyString(cl) && !cl.startsWith("Entity")) {
                                    boolean sc = false;
                                    while (cl.endsWith("+")) {
                                        sc = true;
                                        cl = cl.substring(0, cl.length() - 1);
                                    }
                                    if (sc) {
                                        if (!scs.contains(cl)) 
                                            scs.add(cl);                                        
                                    }
                                    else if (!ios.contains(cl)) 
                                        ios.add(cl);                                    
                                }
                            }
                        }
                        else {
                            Set instanceOfs = kb.getCachedRelationValues("instance", term, 1, 2);
                            if ((instanceOfs != null) && !instanceOfs.isEmpty()) {
                                Iterator it = instanceOfs.iterator();
                                String io = null;
                                while (it.hasNext()) {
                                    io = (String) it.next();
                                    if (!io.equals("Entity") && !ios.contains(io)) 
                                        ios.add(io);                                    
                                }
                            }
                        }
                    }
                }
                // Special treatment for instance or subclass, only if var.equals(arg1)
                // and arg2 is a functional term.
                else if (Arrays.asList("instance", "subclass").contains(pred)) {
                    arg1 = f.getArgument(1);
                    arg2 = f.getArgument(2);
                    if (var.equals(arg1) && listP(arg2)) {
                        Formula nextF = new Formula();
                        nextF.read(arg2);
                        if (nextF.isFunctionalTerm()) {
                            String fn = nextF.car();
                            List classes = getTypeList(fn, kb);
                            if (!classes.isEmpty()) 
                                cl = (String) classes.get(0);                            
                            if (cl == null) 
                                cl = findType(0, fn, kb);                            
                            if (StringUtil.isNonEmptyString(cl) && !cl.startsWith("Entity")) {
                                while (cl.endsWith("+")) 
                                    cl = cl.substring(0, cl.length() - 1);                                
                                if (pred.equals("subclass")) {
                                    if (!scs.contains(cl)) 
                                        scs.add(cl);                                    
                                }
                                else if (!ios.contains(cl)) 
                                    ios.add(cl);                                
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }

		logger.exiting("Formula", "computeTypeRestrictions");
        return;
    }

    /** ***************************************************************
     * When invoked on a Formula that begins with explicit universal
     * quantification, this method returns a String representation of
     * the Formula with type constraints added for the top level
     * quantified variables, if possible.  Otherwise, a String
     * representation of the original Formula is returned.
     *
     * @param shelf A List of quaternary ArrayLists, each of which
     * contains type information about a variable
     *
     * @param kb The KB used to determine predicate and variable arg
     * types.
     *
     * @return A String representation of a Formula, with type
     * restrictions added.
     */
    private String insertTypeRestrictionsU(List shelf, KB kb) {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "shelf = " + shelf, "kb = " + kb.name };
			logger.entering("Formula", "insertTypeRestrictionsU", params);
		}

        String result = this.theFormula;
        try {
            String varlist = this.getArgument(1);
            Formula varlistF = new Formula();
            varlistF.read(varlist);
            List newShelf = makeNewShelf(shelf);
            int vlen = varlistF.listLength();
            for (int i = 0; i < vlen; i++) 
                addVarDataQuad(varlistF.getArgument(i), "U", newShelf);            

            String arg2 = this.getArgument(2);
            Formula nextF = new Formula();
            nextF.read(arg2);
            String processedArg2 = nextF.insertTypeRestrictionsR(newShelf, kb);
            Set constraints = new LinkedHashSet();
            List quad = null;
            String var = null;
            String token = null;
            List ios = null;
            List scs = null;
            Iterator it2 = null;
            String constraint = null;
            for (Iterator it = newShelf.iterator(); it.hasNext();) {
                quad = (List) it.next();
                var = (String) quad.get(0);
                token = (String) quad.get(1);
                if (token.equals("U")) {
                    ios = (List) quad.get(2);
                    scs = (List) quad.get(3);
                    if (!scs.isEmpty()) {
                        winnowTypeList(scs, kb);
                        if (!scs.isEmpty()) {
                            if (!ios.contains("SetOrClass"))
                                ios.add("SetOrClass");
                            for (it2 = scs.iterator(); it2.hasNext();) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("(subclass " + var + " " + it2.next().toString() + ")");
                                constraint = sb.toString();
                                if (!processedArg2.contains(constraint)) 
                                    constraints.add(constraint);                                
                            }
                        }
                    }
                    if (!ios.isEmpty()) {
                        winnowTypeList(ios, kb);
                        for (it2 = ios.iterator(); it2.hasNext();) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("(instance " + var + " " + it2.next().toString() + ")");
                            constraint = sb.toString();
                            if (!processedArg2.contains(constraint)) 
                                constraints.add(constraint);                            
                        }
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("(forall ");
            sb.append(varlistF.theFormula);
            if (constraints.isEmpty()) {
                sb.append(" ");
                sb.append(processedArg2);
            }
            else {
                sb.append(" (=>");
                int clen = constraints.size();
                if (clen > 1) 
                    sb.append(" (and");                
                for (it2 = constraints.iterator(); it2.hasNext();) {
                    sb.append(" ");
                    sb.append(it2.next().toString());
                }
                if (clen > 1) 
                    sb.append(")");                
                sb.append(" ");
                sb.append(processedArg2);
                sb.append(")");
            }
            sb.append(")");
            result = sb.toString();
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
            result = this.theFormula;
        }

		logger.exiting("Formula", "insertTypeRestrictionsU", result);
        return result;
    }

    /** ***************************************************************
     * When invoked on a Formula that begins with explicit existential
     * quantification, this method returns a String representation of
     * the Formula with type constraints added for the top level
     * quantified variables, if possible.  Otherwise, a String
     * representation of the original Formula is returned.
     *
     * @param shelf A List of quaternary ArrayLists, each of which
     * contains type information about a variable
     *
     * @param kb The KB used to determine predicate and variable arg
     * types.
     *
     * @return A String representation of a Formula, with type
     * restrictions added.
     */
    private String insertTypeRestrictionsE(List shelf, KB kb) {

		if (logger.isLoggable(Level.FINER)) {
    		String[] params = {"shelf = " + shelf, "kb = " + kb.name};
			logger.entering("Formula", "insertTypeRestrictionsE", params);
    	}
    	
        String result = this.theFormula;
        try {
            String varlist = this.getArgument(1);
            Formula varlistF = new Formula();
            varlistF.read(varlist);
            List newShelf = makeNewShelf(shelf);
            int vlen = varlistF.listLength();
            for (int i = 0; i < vlen; i++) 
                addVarDataQuad(varlistF.getArgument(i), "E", newShelf);            

            String arg2 = this.getArgument(2);
            Formula nextF = new Formula();
            nextF.read(arg2);

            String processedArg2 = nextF.insertTypeRestrictionsR(newShelf, kb);
            nextF.read(processedArg2);

            Set constraints = new LinkedHashSet();
            StringBuilder sb = new StringBuilder();

            List quad = null;
            String var = null;
            String token = null;
            List ios = null;
            List scs = null;
            Iterator it2 = null;
            String constraint = null;
            for (Iterator it = newShelf.iterator(); it.hasNext();) {
                quad = (List) it.next();
                var = (String) quad.get(0);
                token = (String) quad.get(1);
                if (token.equals("E")) {
                    ios = (List) quad.get(2);
                    scs = (List) quad.get(3);
                    if (!ios.isEmpty()) {
                        winnowTypeList(ios, kb);
                        for (it2 = ios.iterator(); it2.hasNext();) {
                            sb.setLength(0);
                            sb.append("(instance " + var + " " + it2.next().toString() + ")");
                            constraint = sb.toString();
                            if (!processedArg2.contains(constraint)) {
                                constraints.add(constraint);
                            }
                        }
                    }
                    if (!scs.isEmpty()) {
                        winnowTypeList(scs, kb);
                        for (it2 = scs.iterator(); it2.hasNext();) {
                            sb.setLength(0);
                            sb.append("(subclass " + var + " " + it2.next().toString() + ")");
                            constraint = sb.toString();
                            if (!processedArg2.contains(constraint)) {
                                constraints.add(constraint);
                            }
                        }
                    }
                }
            }
            sb.setLength(0);
            sb.append("(exists ");
            sb.append(varlistF.theFormula);
            if (constraints.isEmpty()) {
                sb.append(" ");
                sb.append(processedArg2);
            }
            else {
                sb.append(" (and");
                int clen = constraints.size();
                for (it2 = constraints.iterator(); it2.hasNext();) {
                    sb.append(" ");
                    sb.append(it2.next().toString());
                }
                if (nextF.car().equals("and")) {
                    int nextFLen = nextF.listLength();
                    for (int k = 1; k < nextFLen; k++) {
                        sb.append(" ");
                        sb.append(nextF.getArgument(k));
                    }
                }
                else {
                    sb.append(" ");
                    sb.append(nextF.theFormula);
                }
                sb.append(")");
            }
            sb.append(")");
            result = sb.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            result = this.theFormula;
        }

		logger.exiting("Formula", "insertTypeRestrictionsE", result);
        return result;
    }

    /** ***************************************************************
     * When invoked on a Formula, this method returns a String
     * representation of the Formula with type constraints added for
     * all explicitly quantified variables, if possible.  Otherwise, a
     * String representation of the original Formula is returned.
     *
     * @param shelf A List, each element of which is a quaternary List
     * containing a SUO-KIF variable String, a token "U" or "E"
     * indicating how the variable is quantified, a List of instance
     * classes, and a List of subclass classes
     *
     * @param kb The KB used to determine predicate and variable arg
     * types.
     *
     * @return A String representation of a Formula, with type
     * restrictions added.
     */
    private String insertTypeRestrictionsR(List shelf, KB kb) {


		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "shelf = " + shelf, "kb = " + kb.name };
			logger.entering("Formula", "insertTypeRestrictionsR", params);
		}

        String result = this.theFormula;
        try {
            if (listP(this.theFormula)
                && !empty(this.theFormula)
                && this.theFormula.matches(".*\\?\\w+.*")) {
                StringBuilder sb = new StringBuilder();
                Formula f = new Formula();
                f.read(this.theFormula);
                int len = f.listLength();
                String arg0 = f.car();
                if (isQuantifier(arg0) && (len == 3)) {
                    if (arg0.equals("forall")) 
                        sb.append(f.insertTypeRestrictionsU(shelf, kb));                    
                    else 
                        sb.append(f.insertTypeRestrictionsE(shelf, kb));                    
                }
                else {
                    sb.append("(");
                    String argI = null;
                    for (int i = 0; i < len; i++) {
                        argI = f.getArgument(i);
                        if (i > 0) {
                            sb.append(" ");
                            if (isVariable(argI)) {
                                String type = findType(i, arg0, kb);
                                if (StringUtil.isNonEmptyString(type)
                                    && !type.startsWith("Entity")) {
                                    boolean sc = false;
                                    while (type.endsWith("+")) {
                                        sc = true;
                                        type = type.substring(0, type.length() - 1);
                                    }
                                    if (sc) 
                                        addScForVar(argI, type, shelf);                                    
                                    else 
                                        addIoForVar(argI, type, shelf);                                    
                                }
                            }
                        }
                        Formula nextF = new Formula();
                        nextF.read(argI);
                        sb.append(nextF.insertTypeRestrictionsR(shelf, kb));
                    }
                    sb.append(")");
                }
                result = sb.toString();
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
            result = this.theFormula;
        }

		logger.exiting("Formula", "insertTypeRestrictionsR", result);

        return result;
    }
    
    /** ***************************************************************
     */
    private void addVarDataQuad(String var, String quantToken, List shelf) {
        try {
            ArrayList quad = new ArrayList();
            quad.add(var);              // e.g., "?X"
            quad.add(quantToken);       // "U" or "E"
            quad.add(new ArrayList());  // ios
            quad.add(new ArrayList());  // scs
            shelf.add(0, quad);
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return;
    }
    
    /** ***************************************************************
     */
    private ArrayList getIosForVar(String var, List shelf) {
        ArrayList result = null;
        try {
            ArrayList quad = null;
            for (Iterator si = shelf.iterator(); si.hasNext();) {
                quad = (ArrayList) si.next();
                if (var.equals((String) (quad.get(0)))) {
                    result = (ArrayList) (quad.get(2));
                    break;
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
     */
    private ArrayList getScsForVar(String var, List shelf) {
        ArrayList result = null;
        try {
            ArrayList quad = null;
            for (Iterator si = shelf.iterator(); si.hasNext();) {
                quad = (ArrayList) si.next();
                if (var.equals((String) (quad.get(0)))) {
                    result = (ArrayList) (quad.get(3));
                    break;
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
     */
    private void addIoForVar(String var, String io, List shelf) {
        try {
            if (StringUtil.isNonEmptyString(io)) {
                ArrayList ios = getIosForVar(var, shelf);
                if ((ios != null) && !ios.contains(io)) {
                    ios.add(io);
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return;
    }

    /** ***************************************************************
     */
    private void addScForVar(String var, String sc, List shelf) {
        try {
            if (StringUtil.isNonEmptyString(sc)) {
                ArrayList scs = getScsForVar(var, shelf);
                if ((scs != null) && !scs.contains(sc)) {
                    scs.add(sc);
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return;
    }

    /** ***************************************************************
     */
    private ArrayList makeNewShelf(List shelf) {
        return new ArrayList(shelf);
    }

    /** ***************************************************************
     * Add clauses for every variable in the antecedent to restrict its
     * type to the type restrictions defined on every relation in which
     * it appears.  For example
     * (=>
     *   (foo ?A B)
     *   (bar B ?A))
     *
     * (domain foo 1 Z)
     *
     * would result in
     *
     * (=>
     *   (instance ?A Z)
     *   (=>
     *     (foo ?A B)
     *     (bar B ?A)))
     */
    private String addTypeRestrictions(KB kb) {

		logger.entering("Formula", "addTypeRestrictions", kb.name);

    	String result = this.theFormula;
        try {
            Formula f = new Formula();
            long t1 = System.currentTimeMillis();
            f.read(this.makeQuantifiersExplicit(false));
			// logger.finest("f == " + f);
            long t2 = System.currentTimeMillis();
            result = f.insertTypeRestrictionsR(new ArrayList(), kb);
            long t3 = System.currentTimeMillis();
            // Time makeQuantifiersExplicit
            KB.ppTimers[7] += (t2 - t1);
            // Time insertTypeRestrictions
            KB.ppTimers[8] += (t3 - t2);
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }

		logger.exiting("Formula", "addTypeRestrictions", result);

        return result;
    }

    /** ***************************************************************
     * This method returns a HashMap that maps each variable in this
     * Formula to an ArrayList that contains a pair of ArrayLists.
     * The first ArrayList of the pair contains the names of types
     * (classes) of which the variable must be an instance.  The
     * second ArrayList of the pair contains the names of types of
     * which the variable must be a subclass.  Either list in the pair
     * could be empty.  If the only instance or subclass sortal that
     * can be computed for a variable is Entity, the lists will be
     * empty.
     *
     * @param kb The KB used to compute the sortal constraints for
     * each variable.
     *
     * @return A HashMap
     */
    public HashMap computeVariableTypes(KB kb) {

		logger.entering("Formula", "computeVariableTypes", kb.name);

        HashMap result = new HashMap();
        try {
            Formula f = new Formula();
            f.read(this.makeQuantifiersExplicit(false));
            f.computeVariableTypesR(result, kb);
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }

		logger.exiting("Formula", "computeVariableTypes", result);
        return result;
    }

    /** ***************************************************************
     * A recursive utility method used to collect type information for
     * the variables in this Formula.
     *
     * @param map A HashMap used to store type information for the
     * variables in this Formula.
     *
     * @param kb The KB used to compute the sortal constraints for
     * each variable.
     *
     * @return void
     */
    private void computeVariableTypesR(HashMap map, KB kb) {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "map = " + map, "kb = " + kb.name };
			logger.entering("Formula", "computeVariableTypesR", params);
		}

		try {
            if (this.listP() && !this.empty()) {
                int len = this.listLength();
                String arg0 = this.car();
                if (isQuantifier(arg0) && (len == 3)) 
                    this.computeVariableTypesQ(map, kb);                
                else {
                    for (int i = 0; i < len; i++) {
                        Formula nextF = new Formula();
                        nextF.read(this.getArgument(i));
                        nextF.computeVariableTypesR(map, kb);
                    }
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
			ex.printStackTrace();
        }

		logger.exiting("Formula", "computeVariableTypesR");
        return;
    }

    /** ***************************************************************
     * A recursive utility method used to collect type information for
     * the variables in this Formula, which is assumed to have forall
     * or exists as its arg0.
     *
     * @param map A HashMap used to store type information for the
     * variables in this Formula.
     *
     * @param kb The KB used to compute the sortal constraints for
     * each variable.
     *
     * @return void
     */
    private void computeVariableTypesQ(HashMap map, KB kb) {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "map = " + map, "kb = " + kb.name };
			logger.entering("Formula", "computeVariableTypesQ", params);
		}

        try {
            Formula varlistF = new Formula();
            varlistF.read(this.getArgument(1));
            // System.out.println("varlistF == " + varlistF);
            int vlen = varlistF.listLength();
            // System.out.println("vlen == " + vlen);
            Formula nextF = new Formula();
            nextF.read(this.getArgument(2));
            // System.out.println("nextF == " + nextF);
            String var = null;
            for (int i = 0; i < vlen; i++) {
                ArrayList types = new ArrayList();
                ArrayList ios = new ArrayList();
                ArrayList scs = new ArrayList();
                var = varlistF.getArgument(i);
                // System.out.println("i == " + i + ", var == " + var);
                nextF.computeTypeRestrictions(ios, scs, var, kb);
                if (!scs.isEmpty()) {
                    winnowTypeList(scs, kb);
                    if (!scs.isEmpty() && !ios.contains("SetOrClass"))
                        ios.add("SetOrClass");
                }

                if (!ios.isEmpty()) 
                    winnowTypeList(ios, kb);                
                types.add(ios);
                types.add(scs);
                map.put(var, types);
            }
            nextF.computeVariableTypesR(map, kb);
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }

		logger.exiting("Formula", "computeVariableTypesQ");
        return;
    }

    /** ***************************************************************
     * Pre-process a formula before sending it to the theorem prover. This includes
     * ignoring meta-knowledge like documentation strings, translating
     * mathematical operators, quoting higher-order formulas, expanding
     * row variables and prepending the 'holds__' predicate.
     * @return an ArrayList of Formula(s)
     */
    private String preProcessRecurse(Formula f, String previousPred, boolean ignoreStrings,
                                     boolean translateIneq, boolean translateMath) {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "f = " + f, "previousPred = " + previousPred,
					"ignoreStrings = " + ignoreStrings,
					"translateIneq = " + translateIneq,
					"translateMath = " + translateMath };
			logger.entering("Formula", "preProcessRecurse", params);
		}

		StringBuilder result = new StringBuilder();
        try {
            if (f.listP() && !f.empty()) {
                String prefix = "";
                String pred = f.car();
                // Formula predF = new Formula();
                // predF.read(pred);
                if (isQuantifier(pred)) {
                    // The list of quantified variables.
                    result.append(" ");
                    result.append(f.cadr());
                    // The formula following the list of variables.
                    String next = f.caddr();
                    Formula nextF = new Formula();
                    nextF.read(next);
                    result.append(" ");
                    result.append(preProcessRecurse(nextF,"",ignoreStrings,translateIneq,translateMath));
                }
                else {
                    Formula restF = f.cdrAsFormula();
                    int argCount = 1;
                    while (!restF.empty()) {
                        argCount++;
                        String arg = restF.car();
                        //System.out.println("INFO in preProcessRecurse(): arg: " + arg);
                        Formula argF = new Formula();
                        argF.read(arg);
                        if (argF.listP()) {
                            String res = preProcessRecurse(argF,pred,ignoreStrings,translateIneq,translateMath);
                            result.append(" ");
                            if (!isLogicalOperator(pred) &&
                                !isComparisonOperator(pred) &&
                                !isMathFunction(pred) &&
                                !argF.isFunctionalTerm()) {
                                result.append("`");
                            }
                            result.append(res);
                        }
                        else
                            result.append(" " + arg);
                        restF.theFormula = restF.cdr();
                    }
                    if (KBmanager.getMgr().getPref("holdsPrefix").equals("yes")) {
                        if (!isLogicalOperator(pred) && !isQuantifierList(pred,previousPred))
                            prefix = "holds_";
                        if (f.isFunctionalTerm())
                            prefix = "apply_";
                        if (pred.equals("holds")) {
                            pred = "";
                            argCount--;
                            prefix = prefix + argCount + "__ ";
                        }
                        else {
                            if (!isLogicalOperator(pred) &&
                                !isQuantifierList(pred,previousPred) &&
                                !isMathFunction(pred) &&
                                !isComparisonOperator(pred)) {
                                prefix = prefix + argCount + "__ ";
                            }
                            else
                                prefix = "";
                        }
                    }
                }
                result.insert(0, pred);
                result.insert(0, prefix);
                result.insert(0, "(");
                result.append(")");
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }

		logger.exiting("Formula", "preProcessRecurse", result.toString());
        return result.toString();
    }

    /** ***************************************************************
     * Pre-process a formula before sending it to the theorem
     * prover. This includes ignoring meta-knowledge like
     * documentation strings, translating mathematical operators,
     * quoting higher-order formulas, expanding row variables and
     * prepending the 'holds__' predicate.
     *
     * @param isQuery If true the Formula is a query and should be
     *                existentially quantified, else the Formula is a
     *                statement and should be universally quantified
     *
     * @param kb The KB to be used for processing this Formula
     *
     * @return an ArrayList of Formula(s), which could be empty.
     *
     */
    public ArrayList preProcess(boolean isQuery, KB kb) {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "isQuery = " + isQuery, "kb = " + kb.name };
			logger.entering("Formula", "preProcess", params);
		}

        ArrayList results = new ArrayList();
        try {
            if (isNonEmptyString(this.theFormula)) {
                KBmanager mgr = KBmanager.getMgr();
                if (!this.isBalancedList()) {
                    String errStr = "Unbalanced parentheses or quotes";
					logger.warning(errStr + " for formula = " + this.theFormula);
                    mgr.setError(mgr.getError() + "\n<br/>" + errStr + " in "
                                 + this.theFormula + "\n<br/>");
                    return results;
                }
                boolean ignoreStrings = false;
                boolean translateIneq = true;
                boolean translateMath = true;
                Formula f = new Formula();
                f.read(this.theFormula);
                if (StringUtil.containsNonAsciiChars(f.theFormula))
                    f.theFormula = StringUtil.replaceNonAsciiChars(f.theFormula);

                boolean addHoldsPrefix = mgr.getPref("holdsPrefix").equalsIgnoreCase("yes");
                ArrayList variableReplacements = f.replacePredVarsAndRowVars(kb, addHoldsPrefix);

                ArrayList accumulator = addInstancesOfSetOrClass(kb, isQuery, variableReplacements);
                // Iterate over the formulae resulting from predicate
                // variable instantiation and row variable expansion,
                // passing each to preProcessRecurse for further
                // processing.
                if (!accumulator.isEmpty()) {
                    boolean addSortals = mgr.getPref("typePrefix").equalsIgnoreCase("yes");
                    Formula fnew = null;
                    String theNewFormula = null;
                    long t1 = 0L;
                    for (Iterator it = accumulator.iterator(); it.hasNext();) {
                        fnew = (Formula) it.next();
                        t1 = System.currentTimeMillis();
                        // arg0 = this.getArgument(0);
                        if (addSortals
                            && !isQuery
                            // isLogicalOperator(arg0) ||
                            && fnew.theFormula.matches(".*\\?\\w+.*")) {
                            fnew.read(fnew.addTypeRestrictions(kb));
                        }
                        // Increment the timer for adding type restrictions.
                        KB.ppTimers[0] += (System.currentTimeMillis() - t1);

                        t1 = System.currentTimeMillis();
                        theNewFormula = fnew.preProcessRecurse(fnew,"",ignoreStrings,translateIneq,translateMath);
                        fnew.read(theNewFormula);
                        // Increment the timer for preProcessRecurse().
                        KB.ppTimers[6] += (System.currentTimeMillis() - t1);

                        if (fnew.isOkForInference(isQuery, kb)) {
                            fnew.sourceFile = this.sourceFile;
                            results.add(fnew);
                        }
                        else {
							logger.warning("Following formula rejected for inference: "
									+ theNewFormula);
                            mgr.setError(mgr.getError() + "\n<br/>Formula rejected for inference:<br/>"
                                         + fnew.htmlFormat(kb) + "<br/>\n");
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
			logger.warning(ex.getMessage());
        }

		logger.exiting("Formula", "preProcess", results);
        return results;
    }

    /** ***************************************************************
     *  Replace v with term
     */
    public Formula replaceVar(String v, String term) {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "v = " + v, "term = " + term };
			logger.entering("Formula", "replaceVar", params);
		}

        Formula newFormula = new Formula();
        newFormula.read("()");
        if (this.isVariable()) {
            if (theFormula.equals(v))
                theFormula = term;
            return this;
        }
        if (!this.empty()) {
            Formula f1 = new Formula();
            f1.read(this.car());
			// logger.finest("car: " + f1.theFormula);
            if (f1.listP())
                newFormula = newFormula.cons(f1.replaceVar(v,term));
            else
                newFormula = newFormula.append(f1.replaceVar(v,term));
            Formula f2 = new Formula();
            f2.read(this.cdr());
			// logger.finest("cdr: " + f2);
            newFormula = newFormula.append(f2.replaceVar(v,term));
        }

		logger.exiting("Formula", "replaceVar", newFormula);

        return newFormula;
    }

    /** ***************************************************************
     * Tries to successively instantiate predicate variables and then
     * expand row variables in this Formula, looping until no new
     * Formulae are generated.
     *
     * @param kb The KB to be used for processing this Formula
     *
     * @param addHoldsPrefix If true, predicate variables are not
     * instantiated
     *
     * @return an ArrayList of Formula(s), which could be empty.
     */
    private ArrayList replacePredVarsAndRowVars(KB kb, boolean addHoldsPrefix) {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "kb = " + kb.name,
					"addHoldsPrefix = " + addHoldsPrefix };
			logger.entering("Formula", "replacePredVarsAndRowVars", params);
		}

		ArrayList result = new ArrayList();
        try {
            Formula startF = new Formula();
            startF.read(this.theFormula);
            long t1 = 0L;
            Set accumulator = new LinkedHashSet();
            accumulator.add(startF);
            List working = new ArrayList();
            int prevAccumulatorSize = 0;
            Iterator it = null;
            Formula f = null;
            while (accumulator.size() != prevAccumulatorSize) {
                prevAccumulatorSize = accumulator.size();
                // Do pred var instantiations if we are not adding
                // holds prefixes.
                if (!addHoldsPrefix) {
                    working.clear();
                    working.addAll(accumulator);
                    accumulator.clear();
                    long tnaplwriVal = KB.ppTimers[4];
                    t1 = System.currentTimeMillis();
                    for (it = working.iterator(); it.hasNext();) {
                        f = (Formula) it.next();
                        List instantiations = f.instantiatePredVars(kb);
						// logger.finest("instantiations == " + instantiations);
                        if (instantiations.isEmpty()) {
                            // If the accumulator is empty -- no pred
                            // var instantiations were possible -- add
                            // the original formula to the accumulator
                            // for possible row var expansion below.
                            accumulator.add(f);
                        }
                        else {
                            // If the formula can't be instantiated at
                            // all and so has been marked "reject",
                            // don't add anything.
                            KBmanager mgr = KBmanager.getMgr();
                            Object obj0 = instantiations.get(0);
                            String errStr = "No predicate instantiations for";
                            if (isNonEmptyString(obj0)
                                && ((String) obj0).equalsIgnoreCase("reject")) {
								logger.warning(errStr + " " + f);
                                errStr += ("\n<br/> " + f.htmlFormat(kb));
                                mgr.setError(mgr.getError() + "\n<br/>" + errStr + "\n<br/>");
                            }
                            else {
                                // It might not be possible to
                                // instantiate all pred vars until
                                // after row vars have been expanded,
                                // so we loop until no new formulae
                                // are being generated.
                                accumulator.addAll(instantiations);
                            }
                        }
                    }

                    // Increment the timer for pred var instantiation.
                    KB.ppTimers[1] += (System.currentTimeMillis() - t1);

                    // We do this to avoid adding up time spent in
                    // Formula.toNegAndPosLitsWtihRenameInfo() while
                    // doing pred var instantiation.  What we really
                    // want to know is how much time this method
                    // contributes to the total time for row var
                    // expansion.
                    KB.ppTimers[4] = tnaplwriVal;
                }
                // System.out.println("  1. pvi accumulator.size() == " + accumulator.size());
                // Row var expansion.
                // Iterate over the instantiated predicate formulas,
                // doing row var expansion on each.  If no predicate
                // instantiations can be generated, the accumulator
                // will contain just the original input formula.
                if (!accumulator.isEmpty() && (accumulator.size() < AXIOM_EXPANSION_LIMIT)) {
                    t1 = System.currentTimeMillis();
                    working.clear();
                    working.addAll(accumulator);
                    accumulator.clear();
                    for (it = working.iterator(); it.hasNext();) {
                        f = (Formula) it.next();
                        // System.out.println("f == " + f);
                        accumulator.addAll(f.expandRowVars(kb));
						// logger.finest("f == " + f);
						// logger.finest("accumulator == " + accumulator);
                        if (accumulator.size() > AXIOM_EXPANSION_LIMIT) {
                            System.out.println("  AXIOM_EXPANSION_LIMIT EXCEEDED: "
                                               + AXIOM_EXPANSION_LIMIT);
                            break;
                        }
                    }
                    // Increment the timer for row var expansion.
                    KB.ppTimers[2] += (System.currentTimeMillis() - t1);
                }
                // System.out.println("  3. rve accumulator.size() == " + accumulator.size());
            }
            result.addAll(accumulator);
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        
		logger.exiting("Formula", "replacePredVarsAndRowVars", result);

        return result;
    }

    /** ***************************************************************
     * Adds statements of the form (instance <Entity> <SetOrClass>) if
     * they are not already in the KB.
     *
     * @param kb The KB to be used for processing the input Formulae
     * in variableReplacements
     *
     * @param isQuery If true, this method just returns the initial
     * input List, variableReplacements, with no additions
     *
     * @param variableReplacements A List of Formulae in which
     * predicate variables and row variables have already been
     * replaced, and to which (instance <Entity> <SetOrClass>)
     * Formulae might be added
     *
     * @return an ArrayList of Formula(s), which could be larger than
     * the input List, variableReplacements, or could be empty.
     */
    private ArrayList addInstancesOfSetOrClass(KB kb,boolean isQuery, List variableReplacements) {

        ArrayList result = new ArrayList();
        try {
            if ((variableReplacements != null) && !variableReplacements.isEmpty()) {
                if (isQuery)
                    result.addAll(variableReplacements);
                else {
                    Set formulae = new LinkedHashSet();
                    String arg0 = null;
                    Formula f = null;
                    for (Iterator it = variableReplacements.iterator(); it.hasNext();) {
                        f = (Formula) it.next();
                        formulae.add(f);

                        // Make sure every SetOrClass is stated to be such.
                        if (f.listP() && !f.empty()) {
                            arg0 = f.car();
                            int start = -1;
                            if (arg0.equals("subclass")) start = 0;
                            else if (arg0.equals("instance")) start = 1;
                            if (start > -1) {
                                List args = Arrays.asList(f.getArgument(1),f.getArgument(2));
                                int argslen = args.size();
                                String ioStr = null;
                                Formula ioF = null;
                                String arg = null;
                                for (int i = start; i < argslen; i++) {
                                    arg = (String) args.get(i);
                                    if (!isVariable(arg) && !arg.equals("SetOrClass") && atom(arg)) {
                                        StringBuilder sb = new StringBuilder();
                                        sb.setLength(0);
                                        sb.append("(instance ");
                                        sb.append(arg);
                                        sb.append(" SetOrClass)");
                                        ioF = new Formula();
                                        ioStr = sb.toString().intern();
                                        ioF.read(ioStr);
                                        ioF.sourceFile = this.sourceFile;
                                        if (!kb.formulaMap.containsKey(ioStr)) {
                                            Map stc = kb.getSortalTypeCache();
                                            if (stc.get(ioStr) == null) {
                                                stc.put(ioStr, ioStr);
                                                formulae.add(ioF);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    result.addAll(formulae);
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
     * Returns true if this Formula appears not to have any of the
     * characteristics that would cause it to be rejected during
     * translation to TPTP form, or cause problems during inference.
     * Otherwise, returns false.
     *
     * @param query true if this Formula represents a query, else
     * false.
     *
     * @param kb The KB object to be used for evaluating the
     * suitability of this Formula.
     *
     * @return boolean
     */
    private boolean isOkForInference(boolean query, KB kb) {

        boolean pass = false;
        // kb isn't used yet, because the checks below are purely
        // syntactic.  But it probably will be used in the future.
        try {
            pass = !(// (equal ?X ?Y ?Z ...) - equal is strictly binary.
                     // No longer necessary?  NS: 2009-06-12
                     // this.theFormula.matches(".*\\(\\s*equal\\s+\\?*\\w+\\s+\\?*\\w+\\s+\\?*\\w+.*")

                     // The formula contains non-ASCII characters.
                     // was: this.theFormula.matches(".*[\\x7F-\\xFF].*")
                     // ||
                     StringUtil.containsNonAsciiChars(this.theFormula)

                     // (<relation> ?X ...) - no free variables in an
                     // atomic formula that doesn't contain a string
                     // unless the formula is a query.
                     || (!query
                         && !isLogicalOperator(this.car())
                         // The formula does not contain a string.
                         && (this.theFormula.indexOf('"') == -1)
                         // The formula contains a free variable.
                         && this.theFormula.matches(".*\\?\\w+.*"))

                     // ... add more patterns here, as needed.
                     || false
                     );
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return pass;
    }

    /** ***************************************************************
     * Compare the given formula to the query and return whether
     * they are the same.
     */
    public static boolean isQuery(String query, String formula) {

        boolean result = false;

        Formula f = new Formula();
        f.read(formula);
        result = f.equals(query);
        return result;
    }

    /** ***************************************************************
     * Compare the given formula to the negated query and return whether
     * they are the same (minus the negation).
     */
    public static boolean isNegatedQuery(String query, String formulaString) {

        boolean result = false;
        try {
			if (logger.isLoggable(Level.FINER)) {
				String[] params = { "query = " + query,
						"formulaString = " + formulaString };
				logger.entering("Formula", "isNegatedQuery", params);
			}

            String fstr = formulaString.trim();
            if (fstr.startsWith("(not")) {
                Formula f = new Formula();
                f.read(fstr);
                result = query.equals(f.getArgument(1));
                //System.out.print("INFO in Formula.isNegatedQuery(): ");
                //System.out.println(result);
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }

		logger.exiting("Formula", "isNegatedQuery", result);
        return result;
    }

    /** ***************************************************************
     * Remove the 'holds' prefix wherever it appears.
     */
    public static String postProcess(String s) {

        s = s.replaceAll("holds_\\d+__ ","");
        s = s.replaceAll("apply_\\d+__ ","");
        return s;
    }

    /** ***************************************************************
     * Format a formula for either text or HTML presentation by inserting
     * the proper hyperlink code, characters for indentation and end of line.
     * A standard LISP-style pretty printing is employed where an open
     * parenthesis triggers a new line and added indentation.
     *
     * @param hyperlink - the URL to be referenced to a hyperlinked term.
     * @param indentChars - the proper characters for indenting text.
     * @param eolChars - the proper character for end of line.
     */
    public String format(String hyperlink, String indentChars, String eolChars) {

        if (this.theFormula == null)
            return "";
        String result = this.theFormula;
        try {
            if (isNonEmptyString(this.theFormula))
                this.theFormula = this.theFormula.trim();
            String legalTermChars = "-:";
            String varStartChars = "?@";
            String quantifiers = "forall|exists";
            StringBuilder token = new StringBuilder();
            StringBuilder formatted = new StringBuilder();
            int indentLevel = 0;
            boolean inQuantifier = false;
            boolean inToken = false;
            boolean inVariable = false;
            boolean inVarlist = false;
            boolean inComment = false;

            int flen = this.theFormula.length();
            char pch = '0';  // char at (i-1)
            char ch = '0';   // char at i
            for (int i = 0; i < flen; i++) {
				// logger.finest("formatted string = " + formatted.toString());
                ch = this.theFormula.charAt(i);
                if (inComment) {     // In a comment
                    formatted.append(ch);
                    if ((i > 70) && (ch == '/')) // add spaces to long URL strings
                        formatted.append(" ");
                    if (ch == '"')
                        inComment = false;
                }
                else {
                    if ((ch == '(')
                        && !inQuantifier
                        && ((indentLevel != 0) || (i > 1))) {
                        if ((i > 0) && Character.isWhitespace(pch)) {
                            formatted = formatted.deleteCharAt(formatted.length()-1);
                        }
                        formatted.append(eolChars);
                        for (int j = 0; j < indentLevel; j++)
                            formatted.append(indentChars);
                    }
                    if ((i == 0) && (indentLevel == 0) && (ch == '('))
                        formatted.append(ch);
                    if (!inToken && !inVariable && Character.isJavaIdentifierStart(ch)) {
                        token = new StringBuilder(ch);
                        inToken = true;
                    }
                    if (inToken && (Character.isJavaIdentifierPart(ch)
                                    || (legalTermChars.indexOf(ch) > -1)))
                        token.append(ch);
                    if (ch == '(') {
                        if (inQuantifier) {
                            inQuantifier = false;
                            inVarlist = true;
                            token = new StringBuilder();
                        }
                        else
                            indentLevel++;
                    }
                    if (ch == '"')
                        inComment = true;
                    if (ch == ')') {
                        if (!inVarlist)
                            indentLevel--;
                        else
                            inVarlist = false;
                    }
                    if ((token.indexOf("forall") > -1) || (token.indexOf("exists") > -1))
                        inQuantifier = true;
                    if (inVariable
                        && !Character.isJavaIdentifierPart(ch)
                        && (legalTermChars.indexOf(ch) == -1))
                        inVariable = false;
                    if (varStartChars.indexOf(ch) > -1)
                        inVariable = true;
                    if (inToken
                        && !Character.isJavaIdentifierPart(ch)
                        && (legalTermChars.indexOf(ch) == -1)) {
                        inToken = false;
                        if (StringUtil.isNonEmptyString(hyperlink)) {
                            formatted.append("<a href=\"");
                            formatted.append(hyperlink);
                            formatted.append("&term=");
                            formatted.append(token);
                            formatted.append("\">");
                            formatted.append(token);
                            formatted.append("</a>");
                        }
                        else
                            formatted.append(token);
                        token = new StringBuilder();
                    }
                    if ((i > 0) && !inToken && !(Character.isWhitespace(ch) && (pch == '('))) {
                        if (Character.isWhitespace(ch)) {
                            if (!Character.isWhitespace(pch))
                                formatted.append(" ");
                        }
                        else
                            formatted.append(ch);
                    }
                }
                pch = ch;
            }
            if (inToken) {    // A term which is outside of parenthesis, typically, a binding.
                if (StringUtil.isNonEmptyString(hyperlink)) {
                    formatted.append("<a href=\"");
                    formatted.append(hyperlink);
                    formatted.append("&term=");
                    formatted.append(token);
                    formatted.append("\">");
                    formatted.append(token);
                    formatted.append("</a>");
                }
                else
                    formatted.append(token);
            }
            result = formatted.toString();
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
     * Format a formula for text presentation.
     * @deprecated
     */
    public String textFormat() {

        return format("","  ",new Character((char) 10).toString());
    }

    /** ***************************************************************
     * Format a formula for text presentation.
     */
    public String toString() {

        return format("","  ",new Character((char) 10).toString());
    }

    /** ***************************************************************
     * Format a formula for HTML presentation.
     */
    public String htmlFormat(String html) {

        return format(html,"&nbsp;&nbsp;&nbsp;&nbsp;","<br>\n");
    }

    /** ***************************************************************
     * Format a formula for HTML presentation.
     */
    public String htmlFormat(KB kb) {

        String fKbHref = "";
        try {
            KBmanager mgr = KBmanager.getMgr();
            String hostname = mgr.getPref("hostname");
            if (!isNonEmptyString(hostname))
                hostname = "localhost";
            String port = mgr.getPref("port");
            if (!isNonEmptyString(port))
                port = "8080";
            String kbHref = ("http://" + hostname + ":" + port + "/sigma/Browse.jsp?kb=" + kb.name);
            fKbHref = format(kbHref,"&nbsp;&nbsp;&nbsp;&nbsp;","<br>\n");
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return fKbHref;
    }

    /** ***************************************************************
     * Format a formula as a prolog statement.  Note that only tuples
     * are converted properly at this time.  Statements with any embedded
     * formulas or functions will be rejected with a null return.
     */
    public String toProlog() {

        if (!listP()) {
			logger.warning("Not a formula: " + theFormula);
            return "";
        }
        if (empty()) {
			logger.warning("Empty formula: " + theFormula);
            return "";
        }
        StringBuilder result = new StringBuilder();
        String relation = car();
        Formula f = new Formula();
        f.theFormula = cdr();
        if (!Formula.atom(relation)) {
			logger.warning("Relation not an atom: " + relation);
            return "";
        }
        result.append(relation + "('");
        while (!f.empty()) {
            String arg = f.car();
            f.theFormula = f.cdr();
            if (!Formula.atom(arg)) {
				logger.warning("Argument not an atom: " + arg);
                return "";
            }
            result.append(arg + "'");
            if (!f.empty())
                result.append(",'");
            else
                result.append(").");
        }
        return result.toString();
    }

    public static final String termMentionSuffix  = "__m";
    public static final String classSymbolSuffix  = "__t";  // for the case when a class is used as an instance
    public static final String termSymbolPrefix   = "s__";
    public static final String termVariablePrefix = "V__";

    /** ***************************************************************
     * Encapsulates translateWord_1, which translates the logical
     * operators and inequalities in SUO-KIF to their TPTP
     * equivalents.
     *
     * @param st is the StreamTokenizer_s that contains the current token
     * @return the String that is the translated token
     */
    private static String translateWord(StreamTokenizer_s st, boolean hasArguments) {

        String result = null;
        try {

			// logger.finest("st.ttype == " + st.ttype);
			// logger.finest("st.sval == " + st.sval);
			// logger.finest("st.nval == " + st.nval);

            result = translateWord_1(st, hasArguments);
            if (result.equals("$true__m") || result.equals("$false__m")) {
                result = "'" + result + "'";
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }

		logger.exiting("Formula", "translateWord", result);
        return result;
    }

    /** ***************************************************************
     * Convert the logical operators and inequalities in SUO-KIF to
     * their TPTP equivalents
     * @param st is the StreamTokenizer_s that contains the current token
     * @return the String that is the translated token
     */
    private static String translateWord_1(StreamTokenizer_s st, boolean hasArguments) {

        int translateIndex;

        List<String> kifOps = Arrays.asList(UQUANT, EQUANT, NOT, AND, OR, IF, IFF);
        List<String> tptpOps = Arrays.asList("! ", "? ", "~ ", " & ", " | ", " => ", " <=> ");

        List<String> kifPredicates =
            Arrays.asList(LOG_TRUE, LOG_FALSE,
                          EQUAL,
                          "<=","<",">",">=",
                          "lessThanOrEqualTo","lessThan","greaterThan","greaterThanOrEqualTo");

        List<String> tptpPredicates = Arrays.asList("$true","$false",
                                                    "equal",
                                                    "lesseq","less","greater","greatereq",
                                                    "lesseq","less","greater","greatereq");

        List<String> kifFunctions = Arrays.asList(TIMESFN, DIVIDEFN, PLUSFN, MINUSFN);
        List<String> tptpFunctions = Arrays.asList("times","divide","plus","minus");

        List<String> kifRelations = new ArrayList<String>();
        kifRelations.addAll(kifPredicates);
        kifRelations.addAll(kifFunctions);

        // DEBUG System.out.println("Translating word " + st.sval + " with hasArguments " + hasArguments);

        // Context creeps back in here whether we want it or not.  We
        // consult the KBmanager to determine if holds prefixing is
        // turned on, or not.  If it is on, then we do not want to add
        // the "mentions" suffix to relation names used as arguments
        // to other relations.
        KBmanager mgr = null;
        boolean holdsPrefixInUse = false;
        String mentionSuffix = Formula.termMentionSuffix;
        String symbolPrefix = Formula.termSymbolPrefix;
        String variablePrefix = Formula.termVariablePrefix;
        try {
            mgr = KBmanager.getMgr();
            holdsPrefixInUse = ((mgr != null) && mgr.getPref("holdsPrefix").equalsIgnoreCase("yes"));
            if (holdsPrefixInUse && !kifRelations.contains(st.sval))
                mentionSuffix = "";
        }
        catch (Exception ex) {
            //---Be silent if there is a problem getting the KBmanager.
			logger.warning(ex.getMessage());
        }

        //----Places single quotes around strings, and replace \n by space
        if (st.ttype == 34)
            return("'" + st.sval.replaceAll("[\n\t\r\f]"," ").replaceAll("'","") + "'");
        //----Fix variables to have leading V_
        char ch0 = ((st.sval.length() > 0)
                    ? st.sval.charAt(0)
                    : 'x');
        char ch1 = ((st.sval.length() > 1)
                    ? st.sval.charAt(1)
                    : 'x');
        if (ch0 == '?' || ch0 == '@')
            return(termVariablePrefix + st.sval.substring(1).replace('-','_'));
        //----Translate special predicates
        translateIndex = 0;
        while (translateIndex < kifPredicates.size() && !st.sval.equals(kifPredicates.get(translateIndex)))
            translateIndex++;
        if (translateIndex < kifPredicates.size())
            // return((hasArguments ? "$" : "") + tptpPredicates[translateIndex]);
            return(tptpPredicates.get(translateIndex) + (hasArguments ? "" : mentionSuffix));
        //----Translate special functions
        translateIndex = 0;
        while (translateIndex < kifFunctions.size() && !st.sval.equals(kifFunctions.get(translateIndex)))
            translateIndex++;
        if (translateIndex < kifFunctions.size())
            // return((hasArguments ? "$" : "") + tptpFunctions[translateIndex]);
            return(tptpFunctions.get(translateIndex) + (hasArguments ? "" : mentionSuffix));
        //----Translate operators
        translateIndex = 0;
        while (translateIndex < kifOps.size() && !st.sval.equals(kifOps.get(translateIndex)))
            translateIndex++;
        if (translateIndex < kifOps.size())
            return(tptpOps.get(translateIndex));
        //----Do nothing to numbers
        if (st.ttype == StreamTokenizer.TT_NUMBER ||
            (st.sval != null && (Character.isDigit(ch0) ||
                                 (ch0 == '-' && Character.isDigit(ch1))))) {
            return(st.sval);
            //SANITIZE return("n" + st.sval.replace('-','n').replaceAll("[.]","dot"));
        }

        //----Fix other symbols to have leading s_
        // return("s_" + st.sval.substring(1).replace('-','_'));
        String term = st.sval;

        //----Add a "mention" suffix to relation names that occur as arguments
        //----to other relations.
        if (!hasArguments) {
            if (
                // The purely syntactic criteria for testing if a term
                // denotes a Relation are reliable only for "pure"
                // SUO-KIF.  They break down if the terms to be
                // translated contain namespace prefixes and other
                // non-SUO-KIF lexical conventions.
                (!term.endsWith(mentionSuffix)

                 && ((!term.contains(StringUtil.getKifNamespaceDelimiter())
                      && Character.isLowerCase(ch0))

                     || term.endsWith("Fn")

                     // The semantic test below works only if a KB is
                     // loaded.
                     || KB.isInstanceOfInAnyKB(term, "Relation")))) {

                term += mentionSuffix;
            }
        }
        //return("s_" + term.replace('-','_'));
        return(termSymbolPrefix + term.replace('-','_'));
    }

    /** ***************************************************************
     * @param st is the StreamTokenizer_s that contains the current token
     * for which the arity is desired
     *
     * @return the integer arity of the given logical operator
     */
    private static int operatorArity(StreamTokenizer_s st) {

        int translateIndex;
        String kifOps[] = {UQUANT, EQUANT, NOT, AND, OR, IF, IFF};

        translateIndex = 0;
        while (translateIndex < kifOps.length &&
               !st.sval.equals(kifOps[translateIndex])) {
            translateIndex++;
        }
        if (translateIndex <= 2) {
            return(1);
        } else {
            if (translateIndex < kifOps.length) {
                return(2);
            } else {
                return(-1);
            }
        }
    }

    /** ***************************************************************
     */
    private static void incrementTOS(Stack countStack) {

        countStack.push(new Integer((Integer)countStack.pop()+1));
    }

    /** ***************************************************************
     * Add the current token, if a variable, to the list of variables
     * @param variables is the list of variables
     */
    private static void addVariable(StreamTokenizer_s st,Vector variables) {

        String tptpVariable;

        if (st.sval.charAt(0) == '?' || st.sval.charAt(0) == '@') {
            tptpVariable = translateWord(st,false);
            if (variables.indexOf(tptpVariable) == -1) {
                variables.add(tptpVariable);
            }
        }
    }

    /** ***************************************************************
     * Parse a single formula into TPTP format
     */
    public static String tptpParseSUOKIFString(String suoString) {

        //  System.out.println("INFO in Formula.tptpParseSUOKIFString(\"" + suoString + "\")");
        Formula tempF = new Formula();      // Special case to renam Foo for (instance Foo SetOrClass)
        tempF.read(suoString);              // so a symbol can't be both a class and an instance.
        if (tempF.getArgument(0).equals("instance") &&
            tempF.getArgument(2).equals("SetOrClass")) {
            String arg1 = tempF.getArgument(1);
            suoString = "(instance " + arg1 + classSymbolSuffix + " SetOrClass)";
        }

        StreamTokenizer_s st = null;
        String translatedFormula = null;
        try {
            int parenLevel;
            boolean inQuantifierVars;
            boolean lastWasOpen;
            boolean inHOL;
            int inHOLCount;
            Stack operatorStack = new Stack();
            Stack countStack = new Stack();
            Vector quantifiedVariables = new Vector();
            Vector allVariables = new Vector();
            int index;
            int arity;
            String quantification;

            StringBuilder tptpFormula = new StringBuilder();

            parenLevel = 0;
            countStack.push(0);
            lastWasOpen = false;
            inQuantifierVars = false;
            inHOL = false;
            inHOLCount = 0;

            st = new StreamTokenizer_s(new StringReader(suoString));
            KIF.setupStreamTokenizer(st);

            do {
                st.nextToken();
                if (st.ttype==40) {         //----Open bracket
                    if (lastWasOpen) {      //----Should not have ((in KIF
						logger.warning("Double open bracket at " + tptpFormula);
                        throw new ParseException("Parsing error in " + suoString,0);
                    }
                    //----Track nesting of ()s for hol__, so I know when to close the '
                    if (inHOL)
                        inHOLCount++;
                    lastWasOpen = true;
                    parenLevel++;
                    //----Operators
                } else if (st.ttype == StreamTokenizer.TT_WORD &&
                           (arity = operatorArity(st)) > 0) {
                    //----Operators must be preceded by a (
                    if (!lastWasOpen) {
						logger.warning("Missing ( before " +
                                           st.sval + " at " + tptpFormula);
                        return(null);
                    }
                    //----This is the start of a new term - put in the infix operator if not the
                    //----first term for this operator
                    if ((Integer)(countStack.peek()) > 0) {

                        // System.out.println("  1 : countStack == " + countStack);
                        // System.out.println("  1 : operatorStack == " + operatorStack);

                        tptpFormula.append((String)operatorStack.peek());
                    }
                    //----If this is the start of a hol__ situation, quote it all
                    if (inHOL && inHOLCount == 1)
                        tptpFormula.append("'");
                    tptpFormula.append("(");          //----()s around all operator expressions
                    if (arity == 1) {                 //----Output unary as prefix
                        tptpFormula.append(translateWord(st,false));
                        //----Note the new operator (dummy) with 0 operands so far
                        countStack.push(new Integer(0));
                        operatorStack.push(",");
                        //----Check if the next thing will be the quantified variables
                        if (st.sval.equals("forall") || st.sval.equals("exists"))
                            inQuantifierVars = true;
                    } else if (arity == 2) {    //----Binary operator
                        //----Note the new operator with 0 operands so far
                        countStack.push(new Integer(0));
                        operatorStack.push(translateWord(st,false));
                    }
                    lastWasOpen = false;
                    //----Back tick - token translation to TPTP. Everything gets ''ed
                } else if (st.ttype == 96) {
                    //----They may be nested - only start the situation at the outer one
                    if (!inHOL) {
                        inHOL = true;
                        inHOLCount = 0;
                    }
                } else if (st.ttype == 34 ||  //----Quote - Term token translation to TPTP
                           st.ttype == StreamTokenizer.TT_NUMBER ||
                           (st.sval != null && (Character.isDigit(st.sval.charAt(0)))) ||
                           st.ttype == StreamTokenizer.TT_WORD) {
                    if (lastWasOpen) {          //----Start of a predicate or variable list
                        if (inQuantifierVars) { //----Variable list
                            tptpFormula.append("[");
                            tptpFormula.append(translateWord(st,false));
                            incrementTOS(countStack);
                        } else {                //----Predicate
                            //----This is the start of a new term - put in the infix operator if not the
                            //----first term for this operator
                            if ((Integer)(countStack.peek()) > 0)
                                tptpFormula.append((String)operatorStack.peek());
                            //----If this is the start of a hol__ situation, quote it all
                            if (inHOL && inHOLCount == 1)
                                tptpFormula.append("'");
                            //----Predicate or function and (
                            tptpFormula.append(translateWord(st,true));
                            tptpFormula.append("(");
                            //----Note the , for between arguments with 0 arguments so far
                            countStack.push(new Integer(0));
                            operatorStack.push(",");
                        }
                        //----Argument or quantified variable
                    } else {
                        //----This is the start of a new term - put in the infix operator if not the
                        //----first term for this operator
                        if ((Integer)(countStack.peek()) > 0)
                            tptpFormula.append((String)operatorStack.peek());
                        //----Output the word
                        tptpFormula.append(translateWord(st,false));
                        //----Increment counter for this level
                        incrementTOS(countStack);
                    }
                    //----Collect variables that are used and quantified
                    if (isNonEmptyString(st.sval)
                        && (st.sval.charAt(0) == '?' || st.sval.charAt(0) == '@')) {
                        if (inQuantifierVars)
                            addVariable(st,quantifiedVariables);
                        else
                            addVariable(st,allVariables);
                    }
                    lastWasOpen = false;
                } else if (st.ttype==41) {      //----Close bracket
                    //----Track nesting of ()s for hol__, so I know when to close the '
                    if (inHOL)
                        inHOLCount--;
                    //----End of quantified variable list
                    if (inQuantifierVars) {
                        //----Fake restarting the argument list because the quantified variable list
                        //----does not use the operator from the surrounding expression
                        countStack.pop();
                        countStack.push(0);
                        tptpFormula.append("] : ");
                        inQuantifierVars = false;
                        //----End of predicate or operator list
                    } else {
                        //----Pop off the stacks to reveal the next outer layer
                        countStack.pop();
                        operatorStack.pop();
                        //----Close the expression
                        tptpFormula.append(")");
                        //----If this closes a HOL expression, close the '
                        if (inHOL && inHOLCount == 0) {
                            tptpFormula.append("'");
                            inHOL = false;
                        }
                        //----Note that another expression has been completed
                        incrementTOS(countStack);
                    }
                    lastWasOpen = false;

                    parenLevel--;
                    //----End of the statement being processed. Universally quantify free variables
                    if (parenLevel == 0) {
                        //findFreeVariables(allVariables,quantifiedVariables);
                        allVariables.removeAll(quantifiedVariables);
                        if (allVariables.size() > 0) {
                            quantification = "! [";
                            for (index = 0; index < allVariables.size(); index++) {
                                if (index > 0)
                                    quantification += ",";
                                quantification += (String)allVariables.elementAt(index);
                            }
                            quantification += "] : ";
                            tptpFormula.insert(0,"( " + quantification);
                            tptpFormula.append(" )");
                        }
                        if (StringUtil.emptyString(translatedFormula))
                            translatedFormula = "( " + tptpFormula.toString() + " )";
                        else
                            translatedFormula += "& ( " + tptpFormula.toString() + " )";

                        if ((Integer)(countStack.pop()) != 1)
							logger.warning("Not one formula");
                    } else if (parenLevel < 0) {
						logger.warning("Extra closing bracket at "
								+ tptpFormula.toString());
                        throw new ParseException("Parsing error in " + suoString,0);
                    }
                } else if (st.ttype != StreamTokenizer.TT_EOF) {
					logger.warning("Illegal character '" +
                                       (char)st.ttype + "' at " + tptpFormula.toString());
                    throw new ParseException("Parsing error in " + suoString,0);
                }
            } while (st.ttype != StreamTokenizer.TT_EOF);

            //----Bare word like $false didn't get done by a closing)
            if (StringUtil.emptyString(translatedFormula))
                translatedFormula = tptpFormula.toString();
        }
        catch (Exception ex2) {
			logger.warning(ex2.getMessage());
			ex2.printStackTrace();
        }
          //System.out.println("  ==> " + translatedFormula);
          //System.out.println("EXIT Formula.tptpParseSUOKIFString(\"" + suoString + "\")");
        return translatedFormula;
    }

    /** ***************************************************************
     * Parse formulae into TPTP format
     */
    public void tptpParse(boolean query, KB kb, List<Formula> preProcessedForms)
        throws ParseException, IOException {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "query = " + query, "kb = " + kb.name,
					"preProcessForms = " + preProcessedForms };
			logger.entering("Formula", "tptpParse", params);
		}

		try {
            KBmanager mgr = KBmanager.getMgr();
            if (kb == null)
                kb = new KB("",mgr.getPref("kbDir"));

            if (!this.isBalancedList()) {
                String errStr = "Unbalanced parentheses or quotes";
				logger.warning(errStr);
                mgr.setError(mgr.getError() + ("\n<br/>" + errStr + " in " + this.theFormula + "\n<br/>"));
                return;
            }
            List<Formula> processed = preProcessedForms;
            if (processed == null)
                processed = this.preProcess(query, kb);

			// logger.finest("processed = " + processed);

            if (processed != null) {
                this.clearTheTptpFormulas();
                //----Performs function on each current processed axiom
                Formula f = null;
                for (Iterator<Formula> g = processed.iterator(); g.hasNext();) {
                    f = (Formula) g.next();
					logger.finer("f = " + f);
                    String tptpStr = tptpParseSUOKIFString(f.theFormula);
                    if (StringUtil.isNonEmptyString(tptpStr)) {
                        this.getTheTptpFormulas().add(tptpStr);
                    }
                }

				// logger.finest("theTptpFormulas = " + this.getTheTptpFormulas());
            }
        }
        catch (Exception ex) {
			logger.severe(ex.getMessage());
            ex.printStackTrace();
            if (ex instanceof ParseException)
                throw (ParseException) ex;
            if (ex instanceof IOException)
                throw (IOException) ex;
        }

		logger.exiting("Formula", "tptpParse");
        return;
    }

    /** ***************************************************************
     * Parse formulae into TPTP format
     */
    public void tptpParse(boolean query, KB kb) throws ParseException, IOException {

        this.tptpParse(query, kb, null);
        return;
    }

    ///////////////////////////////////////////////////////
    /*
      START of instantiatePredVars(KB kb) implementation.
    */
    ///////////////////////////////////////////////////////

    /** ***************************************************************
     * Returns an ArrayList of the Formulae that result from replacing
     * all arg0 predicate variables in the input Formula with
     * predicate names.
     *
     * @param kb A KB that is used for processing the Formula.
     *
     * @return An ArrayList of Formulas, or an empty ArrayList if no
     * instantiations can be generated.
     */
    public ArrayList instantiatePredVars(KB kb) {

        ArrayList ans = new ArrayList();
        try {
            if (this.listP()) {
                String arg0 = this.getArgument(0);

                // First we do some checks to see if it is worth
                // processing the formula.
                if (isLogicalOperator(arg0)
                    && this.theFormula.matches(".*\\(\\s*\\?\\w+.*")) {

                    // Get all pred vars, and then compute query lits
                    // for the pred vars, indexed by var.
                    Map varsWithTypes = gatherPredVars(kb);

                    if (!varsWithTypes.containsKey("arg0")) {
                        // The formula has no predicate variables in
                        // arg0 position, so just return it.
                        ans.add(this);
                    }
                    else {
                        List indexedQueryLits = prepareIndexedQueryLiterals(kb, varsWithTypes);
                        List substForms = new ArrayList();
                        List varQueryTuples = null;
                        List substTuples = null;
                        List litsToRemove = null;

                        // First, gather all substitutions.
                        Iterator it1 = indexedQueryLits.iterator();
                        while (it1.hasNext()) {
                            varQueryTuples = (List) it1.next();
                            substTuples = computeSubstitutionTuples(kb, varQueryTuples);
                            if ((substTuples instanceof List) && !substTuples.isEmpty()) {
                                if (substForms.isEmpty())
                                    substForms.add(substTuples);
                                else {
                                    int stSize = substTuples.size();
                                    int iSize = -1;
                                    int sfSize = substForms.size();
                                    int sfLast = (sfSize - 1);
                                    for (int i = 0 ; i < sfSize ; i++) {
                                        iSize = ((List) substForms.get(i)).size();
                                        if (stSize < iSize) {
                                            substForms.add(i, substTuples);
                                            break;
                                        }
                                        if (i == sfLast)
                                            substForms.add(substTuples);
                                    }
                                }
                            }
                        }

                        if (!substForms.isEmpty()) {
                            // Try to simplify the Formula.
                            Formula f = this;
                            it1 = substForms.iterator();
                            Iterator it2 = null;
                            while (it1.hasNext()) {
                                substTuples = (List) it1.next();
                                litsToRemove = (List) substTuples.get(0);
                                it2 = litsToRemove.iterator();
                                while (it2.hasNext()) {
                                    List lit = (List) it2.next();
                                    f = f.maybeRemoveMatchingLits(lit);
                                }
                            }

                            // Now generate pred var instantiations from the
                            // possibly simplified formula.
                            List templates = new ArrayList();
                            templates.add(f.theFormula);
                            Set accumulator = new HashSet();

                            String template = null;
                            String var = null;
                            String term = null;
                            ArrayList quantVars = null;
                            int i = 0;

                            // Iterate over all var plus query lits forms, getting
                            // a list of substitution literals.
                            it1 = substForms.iterator();
                            while (it1.hasNext()) {
                                substTuples = (List) it1.next();
                                if ((substTuples instanceof List) && !substTuples.isEmpty()) {
                                    // Iterate over all ground lits ...

                                    // Remove litsToRemove, which we have
                                    // already used above.
                                    litsToRemove = (List) substTuples.remove(0);

                                    // Remove and hold the tuple that
                                    // indicates the variable substitution
                                    // pattern.
                                    List varTuple = (List) substTuples.remove(0);

                                    it2 = substTuples.iterator();
                                    while (it2.hasNext()) {
                                        List groundLit = (List) it2.next();

                                        // Iterate over all formula templates,
                                        // substituting terms from each ground lit
                                        // for vars in the template.
                                        Iterator it3 = templates.iterator();
                                        Formula templateF = new Formula();
                                        while (it3.hasNext()) {
                                            template = (String) it3.next();
                                            templateF.read(template);
                                            quantVars = templateF.collectQuantifiedVariables();
                                            for (i = 0 ; i < varTuple.size() ; i++) {
                                                var = (String) varTuple.get(i);
                                                if (isVariable(var)) {
                                                    term = (String) groundLit.get(i);
                                                    // Don't replace variables that
                                                    // are explicitly quantified.
                                                    if (!quantVars.contains(var)) {
                                                        List patternStrings =
                                                            Arrays.asList("(\\W*\\()(\\s*holds\\s+\\" + var + ")(\\W+)",
                                                                          // "(\\W*\\()(\\s*\\" + var + ")(\\W+)",
                                                                          "(\\W*)(\\" + var + ")(\\W+)"
                                                                          );
                                                        int pslen = patternStrings.size();
                                                        List patterns = new ArrayList();
                                                        for (int j = 0; j < pslen; j++)
                                                            patterns.add(Pattern.compile((String)(patternStrings.get(j))));
                                                        int plen = patterns.size();
                                                        Pattern p = null;
                                                        Matcher m = null;
                                                        for (int j = 0 ; j < plen; j++) {
                                                            p = (Pattern) patterns.get(j);
                                                            m = p.matcher(template);
                                                            template =
                                                                m.replaceAll("$1" + term + "$3");
                                                        }
                                                    }
                                                }
                                            }
                                            accumulator.add(template);
                                        }
                                    }
                                    templates.clear();
                                    templates.addAll(accumulator);
                                    accumulator.clear();
                                }
                            }
                            ans.addAll(KB.stringsToFormulas(templates));
                        }
                        if (ans.isEmpty()) {
                            ans.add("reject");
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     * Returns the number of SUO-KIF variables (only ? variables, not
     * @ROW variables) in the input query literal.
     *
     * @param queryLiteral A List representing a Formula.
     *
     * @return An int.
     */
    private static int getVarCount(List queryLiteral) {

        int ans = 0;
        if (queryLiteral instanceof List) {
            String term = null;
            for (Iterator it = queryLiteral.iterator(); it.hasNext();) {
                term = (String) it.next();
                if (term.startsWith("?")) ans++;
            }
        }
        return ans;
    }

    /** ***************************************************************
     * This method returns an ArrayList of query answer literals.  The
     * first element is an ArrayList of query literals that might be
     * used to simplify the Formula to be instantiated.  The second
     * element is the query literal (ArrayList) that will be used as a
     * template for doing the variable substitutions.  All subsequent
     * elements are ground literals (ArrayLists).
     *
     * @param kb A KB to query for answers.
     *
     * @param queryLits A List of query literals.  The first item in
     * the list will be a SUO-KIF variable (String), which indexes the
     * list.  Each subsequent item is a query literal (List).
     *
     * @return An ArrayList of literals, or an empty ArrayList if no
     * query answers can be found.
     */
    private static ArrayList computeSubstitutionTuples(KB kb, List queryLits) {

        // System.out.println("ENTER computeSubstitutionTuples(" + kb + ", " + queryLits + ")");
        ArrayList result = new ArrayList();
        try {
            if ((kb instanceof KB)
                && (queryLits instanceof List)
                && !queryLits.isEmpty()) {

                String idxVar = (String) queryLits.get(0);
                int i = 0;
                int j = 0;

                // Sort the query lits by number of variables.
                ArrayList sortedQLits = new ArrayList(queryLits);
                sortedQLits.remove(0);
                if (sortedQLits.size() > 1) {
                    Comparator comp = new Comparator() {
                            public int compare(Object o1, Object o2) {
                                Integer c1 = Integer.valueOf(getVarCount((List) o1));
                                Integer c2 = Integer.valueOf(getVarCount((List) o2));
                                return c1.compareTo(c2);
                            }
                        };
                    Collections.sort(sortedQLits, Collections.reverseOrder(comp));
                }

                // Put instance literals last.
                List tmplist = new ArrayList(sortedQLits);
                List ioLits = new ArrayList();
                sortedQLits.clear();
                List ql = null;
                for (Iterator iql = tmplist.iterator(); iql.hasNext();) {
                    ql = (List) iql.next();
                    if (((String)(ql.get(0))).equals("instance"))
                        ioLits.add(ql);
                    else
                        sortedQLits.add(ql);
                }
                sortedQLits.addAll(ioLits);

                // Literals that will be used to try to simplify the
                // formula before pred var instantiation.
                ArrayList simplificationLits = new ArrayList();

                // The literal that will serve as the pattern for
                // extracting var replacement terms from answer
                // literals.
                List keyLit = null;

                // The list of answer literals retrieved using the
                // query lits, possibly built up via a sequence of
                // multiple queries.
                ArrayList answers = null;

                Set working = new HashSet();
                ArrayList accumulator = null;

                boolean satisfiable = true;
                boolean tryNextQueryLiteral = true;

                // The first query lit for which we get an answer is
                // the key lit.
                for (i = 0;
                     (i < sortedQLits.size()) && tryNextQueryLiteral;
                     i++) {
                    ql = (List) sortedQLits.get(i);
                    accumulator = kb.askWithLiteral(ql);
                    satisfiable = ((accumulator != null) && !accumulator.isEmpty());
                    tryNextQueryLiteral =
                        (satisfiable
                         ||
                         (getVarCount(ql) > 1)
                         // !((String)(ql.get(0))).equals("instance")
                         );

                    // System.out.println(ql + " accumulator == " + accumulator);

                    if (satisfiable) {
                        simplificationLits.add(ql);
                        if (keyLit == null) {
                            keyLit = ql;
                            answers = KB.formulasToArrayLists(accumulator);
                        }
                        else {  // if (accumulator.size() < answers.size()) {
                            accumulator = KB.formulasToArrayLists(accumulator);

                            // Winnow the answers list.
                            working.clear();
                            List ql2 = null;
                            int varPos = ql.indexOf(idxVar);
                            String term = null;
                            for (j = 0; j < accumulator.size(); j++) {
                                ql2 = (List) accumulator.get(j);
                                term = (String) (ql2.get(varPos));
                                // if (!term.endsWith("Fn")) {
                                working.add(term);
                                // }
                            }
                            accumulator.clear();
                            accumulator.addAll(answers);
                            answers.clear();
                            varPos = keyLit.indexOf(idxVar);
                            for (j = 0; j < accumulator.size(); j++) {
                                ql2 = (List) accumulator.get(j);
                                term = (String) (ql2.get(varPos));
                                if (working.contains(term)) {
                                    answers.add(ql2);
                                }
                            }
                        }
                    }
                }
                if (satisfiable && (keyLit != null)) {
                    result.add(simplificationLits);
                    result.add(keyLit);
                    result.addAll(answers);
                }
                else {
                    result.clear();
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }

        return result;
    }

    /** ***************************************************************
     * This method returns an ArrayList in which each element is
     * another ArrayList.  The head of each element is a variable.
     * The subsequent objects in each element are query literals
     * (ArrayLists).
     *
     * @param kb The KB to use for computing variable type signatures.
     *
     * @return An ArrayList, or null if the input formula contains no
     * predicate variables.
     */
    private ArrayList prepareIndexedQueryLiterals(KB kb) {
        return prepareIndexedQueryLiterals(kb, null);
    }

    /** ***************************************************************
     * This method returns an ArrayList in which each element is
     * another ArrayList.  The head of each element is a variable.
     * The subsequent objects in each element are query literals
     * (ArrayLists).
     *
     * @param kb The KB to use for computing variable type signatures.
     *
     * @param varTypeMap A Map from variables to their types, as
     * explained in the javadoc entry for gatherPredVars(kb)
     *
     * @see Formula.gatherPredVars(KB kb)
     *
     * @return An ArrayList, or null if the input formula contains no
     * predicate variables.
     */
    private ArrayList prepareIndexedQueryLiterals(KB kb, Map varTypeMap) {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "kb = " + kb.name, "varTypeMap = " + varTypeMap };
			logger.entering("Formula", "prepareIndexedQueryLiterals", params);
		}

		ArrayList ans = new ArrayList();
        try {
            Map varsWithTypes = ((varTypeMap instanceof Map)
                                 ? varTypeMap
                                 : this.gatherPredVars(kb));

			// logger.finest("varsWithTypes = " + varsWithTypes);

            if (!varsWithTypes.isEmpty()) {
                String yOrN = (String) varsWithTypes.get("arg0");
                // If the formula doesn't contain any arg0 pred vars, do
                // nothing.
                if (isNonEmptyString(yOrN) && yOrN.equalsIgnoreCase("yes")) {
                    // Try to simplify the formula.
                    ArrayList varWithTypes = null;
                    ArrayList indexedQueryLits = null;

                    String var = null;
                    for (Iterator it = varsWithTypes.keySet().iterator(); it.hasNext();) {
                        var = (String) it.next();
                        if (isVariable(var)) {
                            varWithTypes = (ArrayList) varsWithTypes.get(var);
                            indexedQueryLits = gatherPredVarQueryLits(kb, varWithTypes);
                            if (!indexedQueryLits.isEmpty()) {
                                ans.add(indexedQueryLits);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }

		logger.exiting("Formula", "prepareIndexedQueryLiterals", ans);

        return ans;
    }

    /** ***************************************************************
     * This method collects and returns all predicate variables that
     * occur in the Formula.
     *
     * @param kb The KB to be used for computations involving
     * assertions.
     *
     * @return a HashMap in which the keys are predicate variables,
     * and the values are ArrayLists containing one or more class
     * names that indicate the type constraints tha apply to the
     * variable.  If no predicate variables can be gathered from the
     * Formula, the HashMap will be empty.  The first element in each
     * ArrayList is the variable itself.  Subsequent elements are the
     * types of the variable.  If no types for the variable can be
     * determined, the ArrayList will contain just the variable.
     *
     */
    protected HashMap gatherPredVars(KB kb) {

		logger.entering("Formula", "gatherPredVars", kb.name);
        HashMap ans = new HashMap();
        try {
            if (isNonEmptyString(this.theFormula)) {
                List accumulator = new ArrayList();
                List working = new ArrayList();
                if (this.listP() && !this.empty())
                    accumulator.add(this);
                Iterator it = null;
                while (!accumulator.isEmpty()) {
                    working.clear();
                    working.addAll(accumulator);
                    accumulator.clear();
                    Formula f = null;
                    String arg0 = null;
                    String arg2 = null;
                    ArrayList vals = null;
                    int len = -1;
                    for (it = working.iterator(); it.hasNext();) {
                        f = (Formula) it.next();
                        len = f.listLength();
                        arg0 = f.getArgument(0);
                        if (isQuantifier(arg0)
                            || arg0.equals("holdsDuring")
                            || arg0.equals("KappaFn")) {
                            if (len > 2) {
                                arg2 = f.getArgument(2);

                                Formula newF = new Formula();
                                newF.read(arg2);
                                if (f.listP() && !f.empty())
                                    accumulator.add(newF);
                            }
                            else {
								logger.warning("Is this malformed? "
										+ f.theFormula);
                            }
                        }
                        else if (arg0.equals("holds"))
                            accumulator.add(f.cdrAsFormula());
                        else if (isVariable(arg0)) {
                            vals = (ArrayList) ans.get(arg0);
                            if (vals == null) {
                                vals = new ArrayList();
                                ans.put(arg0, vals);
                                vals.add(arg0);
                            }
                            // Record the fact that we found at least
                            // one variable in the arg0 position.
                            ans.put("arg0", "yes");
                        }
                        else {
                            String argN = null;
                            Formula argF = null;
                            String argType = null;
                            boolean[] signature = kb.getRelnArgSignature(arg0);
                            for (int j = 1; j < len; j++) {
                                argN = f.getArgument(j);
                                if ((signature != null)
                                    && (signature.length > j)
                                    && signature[j]
                                    && isVariable(argN)) {
                                    vals = (ArrayList) ans.get(argN);
                                    if (vals == null) {
                                        vals = new ArrayList();
                                        ans.put(argN, vals);
                                        vals.add(argN);
                                    }
                                    argType = kb.getArgType(arg0, j);
                                    if (!((argType == null) || vals.contains(argType)))
                                        vals.add(argType);
                                }
                                else {
                                    argF = new Formula();
                                    argF.read(argN);
                                    if (argF.listP() && !argF.empty())
                                        accumulator.add(argF);
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }

		logger.exiting("Formula", "gatherPredVars", ans);

        return ans;
    }

    /** ***************************************************************
     * This method tries to remove literals from the Formula that
     * match litArr.  It is intended for use in simplification of this
     * Formula during predicate variable instantiation, and so only
     * attempts removals that are likely to be safe in that context.
     *
     * @param litArr A List object representing a SUO-KIF atomic
     * formula.
     *
     * @return A new Formula with at least some occurrences of litF
     * removed, or the original Formula if no removals are possible.
     */
    private Formula maybeRemoveMatchingLits(List litArr) {
        Formula f = KB.literalListToFormula(litArr);
        return maybeRemoveMatchingLits(f);
    }

    /** ***************************************************************
     * This method tries to remove literals from the Formula that
     * match litF.  It is intended for use in simplification of this
     * Formula during predicate variable instantiation, and so only
     * attempts removals that are likely to be safe in that context.
     *
     * @param litF A SUO-KIF literal (atomic Formula).
     *
     * @return A new Formula with at least some occurrences of litF
     * removed, or the original Formula if no removals are possible.
     */
    private Formula maybeRemoveMatchingLits(Formula litF) {

		logger.entering("Formula", "maybeRemoveMatchingLits", litF);

		Formula result = null;
        try {
            Formula f = this;
            if (f.listP() && !f.empty()) {
                StringBuilder litBuf = new StringBuilder();
                String arg0 = f.car();
                if (Arrays.asList(IF, IFF).contains(arg0)) {
                    String arg1 = f.getArgument(1);
                    String arg2 = f.getArgument(2);
                    if (arg1.equals(litF.theFormula)) {
                        Formula arg2F = new Formula();
                        arg2F.read(arg2);
                        litBuf.append(arg2F.maybeRemoveMatchingLits(litF).theFormula);
                    }
                    else if (arg2.equals(litF.theFormula)) {
                        Formula arg1F = new Formula();
                        arg1F.read(arg1);
                        litBuf.append(arg1F.maybeRemoveMatchingLits(litF).theFormula);
                    }
                    else {
                        Formula arg1F = new Formula();
                        arg1F.read(arg1);
                        Formula arg2F = new Formula();
                        arg2F.read(arg2);
                        litBuf.append("(" + arg0 + " "
                                      + arg1F.maybeRemoveMatchingLits(litF).theFormula + " "
                                      + arg2F.maybeRemoveMatchingLits(litF).theFormula + ")");
                    }
                }
                else if (isQuantifier(arg0)
                         || arg0.equals("holdsDuring")
                         || arg0.equals("KappaFn")) {
                    Formula arg2F = new Formula();
                    arg2F.read(f.caddr());
                    litBuf.append("(" + arg0 + " " + f.cadr() + " "
                                  + arg2F.maybeRemoveMatchingLits(litF).theFormula + ")");
                }
                else if (isCommutative(arg0)) {
                    List litArr = f.literalToArrayList();
                    if (litArr.contains(litF.theFormula))
                        litArr.remove(litF.theFormula);
                    String args = "";
                    int len = litArr.size();
                    for (int i = 1 ; i < len ; i++) {
                        Formula argF = new Formula();
                        argF.read((String) litArr.get(i));
                        args += (" " + argF.maybeRemoveMatchingLits(litF).theFormula);
                    }
                    if (len > 2)
                        args = ("(" + arg0 + args + ")");
                    else
                        args = args.trim();
                    litBuf.append(args);
                }
                else {
                    litBuf.append(f.theFormula);
                }
                Formula newF = new Formula();
                newF.read(litBuf.toString());
                result = newF;
            }
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        if (result == null)
            result = this;

		logger.exiting("Formula", "maybeRemoveMatchingLits", result);
        return result;
    }

    /** ***************************************************************
     * Return true if the input predicate can take relation names a
     * arguments, else returns false.
     */
    private boolean isPossibleRelnArgQueryPred (KB kb, String predicate) {

        return (isNonEmptyString(predicate)
                && ((kb.getRelnArgSignature(predicate) != null)
                    ||
                    predicate.equals("instance")
                    )
                );
    }

    /** ***************************************************************
     * This method collects and returns literals likely to be of use
     * as templates for retrieving predicates to be substituted for
     * var.
     *
     * @param varWithTypes A List containing a variable followed,
     * optionally, by class names indicating the type of the variable.
     *
     * @return An ArrayList of literals (Lists) with var at the head.
     * The first element of the ArrayList is the variable (String).
     * Subsequent elements are Lists corresponding to SUO-KIF
     * formulas, which will be used as query templates.
     *
     */
    private ArrayList gatherPredVarQueryLits(KB kb, List varWithTypes) {

        //System.out.println("ENTER Formula.gatherPredVarQueryLits(" + this + ", " + kb.name + ", " + varWithTypes + ")");
        ArrayList ans = new ArrayList();
        try {
            String var = (String) varWithTypes.get(0);
            Set added = new HashSet();

            // Get the clauses for this Formula.
            StringBuilder litBuf = new StringBuilder();
            List clauses = getClauses();
            Map varMap = getVarMap();
            String qlString = null;
            ArrayList queryLit = null;

            if (clauses != null) {
                Iterator it2 = null;
                Formula f = null;
                Iterator it1 = clauses.iterator();
                while (it1.hasNext()) {
                    List clause = (List) it1.next();
                    List negLits = (List) clause.get(0);
                    // List poslits = (List) clause.get(1);
                    if (!negLits.isEmpty()) {
                        int flen = -1;
                        String arg = null;
                        String arg0 = null;
                        String term = null;
                        String origVar = null;
                        List lit = null;
                        boolean working = true;
                        for (int ci = 0;
                             ci < 1;
                             // (ci < clause.size()) && ans.isEmpty();
                             ci++) {
                            // Try the neglits first.  Then try the poslits only if there still are no resuls.
                            lit = (List)(clause.get(ci));
                            it2 = lit.iterator();
                            // System.out.println("  lit == " + lit);
                            while (it2.hasNext()) {
                                f = (Formula) it2.next();
                                if (f.theFormula.matches(".*SkFn\\s+\\d+.*")
                                    || f.theFormula.matches(".*Sk\\d+.*"))
                                    continue;
                                flen = f.listLength();
                                arg0 = f.getArgument(0);
                                // System.out.println("  var == " + var + "\n  f.theFormula == " + f.theFormula + "\n  arg0 == " + arg0);
                                if (isNonEmptyString(arg0)) {
                                    // If arg0 corresponds to var, then var has to be of type Predicate, not of
                                    // types Function or List.
                                    if (isVariable(arg0)) {
                                        origVar = Clausifier.getOriginalVar(arg0, varMap);
                                        if (origVar.equals(var)
                                            && !varWithTypes.contains("Predicate")) {
                                            varWithTypes.add("Predicate");
                                        }
                                    }
                                    else {
                                        queryLit = new ArrayList();
                                        queryLit.add(arg0);
                                        boolean foundVar = false;
                                        for (int i = 1; i < flen; i++) {
                                            arg = f.getArgument(i);
                                            if (!listP(arg)) {
                                                if (isVariable(arg)) {
                                                    arg = Clausifier.getOriginalVar(arg, varMap);
                                                    if (arg.equals(var))
                                                        foundVar = true;
                                                }
                                                queryLit.add(arg);
                                            }
                                        }
                                        // System.out.println("  arg0 == " + arg0 + "\n  queryLit == " + queryLit);
                                        if (queryLit.size() != flen)
                                            continue;
                                        // If the literal does not start with a variable or with "holds" and does not
                                        // contain Skolem terms, but does contain the variable in which we're interested,
                                        // it is probably suitable as a query template, or might serve as a starting
                                        // place.  Use it, or a literal obtained with it.
                                        if (isPossibleRelnArgQueryPred(kb, arg0) && foundVar) {
                                            // || arg0.equals("disjoint"))
                                            term = "";
                                            if (queryLit.size() > 2)
                                                term = (String) queryLit.get(2);
                                            if (!(arg0.equals("instance")
                                                  && term.equals("Relation"))) {
                                                String queryLitStr = queryLit.toString().intern();
                                                if (!added.contains(queryLitStr)) {
                                                    ans.add(queryLit);
                                                    // System.out.println("  queryLitStr == " + queryLitStr);
                                                    added.add(queryLitStr);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // If we have previously collected type info for the variable,
            // convert that info query lits now.
            String argType = null;
            int vtLen = varWithTypes.size();
            if (vtLen > 1) {
                for (int j = 1 ; j < vtLen ; j++) {
                    argType = (String) varWithTypes.get(j);
                    if (!argType.equals("Relation")) {
                        queryLit = new ArrayList();
                        queryLit.add("instance");
                        queryLit.add(var);
                        queryLit.add(argType);
                        qlString = queryLit.toString().intern();
                        if (!added.contains(qlString)) {
                            ans.add(queryLit);
                            added.add(qlString);
                        }
                    }
                }
            }

            // Add the variable to the front of the answer list, if it contains
            // any query literals.
            if (!ans.isEmpty())
                ans.add(0, var);
            // System.out.println("EXIT Formula.gatherPredVarQueryLits(" + this + ", " + kb.name + ", " + varWithTypes + ")");
            // System.out.println("  ==> " + ans);
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        return ans;
    }

    ///////////////////////////////////////////////////////
    /*
      END of instantiatePredVars(KB kb) implementation.
    */
    ///////////////////////////////////////////////////////

    /** ***************************************************************
     *  Replace variables with a value as given by the map argument
     */
    public Formula substituteVariables(Map<String,String> m) {

		logger.entering("Formula", "substituteVariables", m);

        Formula newFormula = new Formula();
        newFormula.read("()");
        if (atom()) {
            if (m.keySet().contains(theFormula)) {
                theFormula = (String) m.get(theFormula);
                if (this.listP())
                    theFormula = "(" + theFormula + ")";
            }
            return this;
        }
        if (!empty()) {
            Formula f1 = new Formula();
            f1.read(this.car());
            if (f1.listP()) {
                newFormula = newFormula.cons(f1.substituteVariables(m));
            }
            else
                newFormula = newFormula.append(f1.substituteVariables(m));
            Formula f2 = new Formula();
            f2.read(this.cdr());
            newFormula = newFormula.append(f2.substituteVariables(m));
        }

		logger.exiting("Formula", "substituteVariables", newFormula);
        return newFormula;
    }

    /** **************************************************************
     *  Counter for instantiateVariables() to make sure generated
     *  symbols are unique.
     */
    private static int _GENSYM_COUNTER = 0;

    /** **************************************************************
     *  Create constants to fill variables.
     */
    public Formula instantiateVariables() {

        Formula f = Clausifier.renameVariables(this);
        ArrayList<ArrayList<String>> varList = f.collectVariables();
        TreeMap<String,String> vars = new TreeMap<String,String>();
        ArrayList<String> al = (ArrayList<String>) varList.get(0);
        al.addAll((ArrayList<String>) varList.get(1));
        for (int i = 0; i < al.size(); i++) {
            String s = (String) al.get(i);
            _GENSYM_COUNTER++;
            String value = "GenSym" + String.valueOf(_GENSYM_COUNTER);
            vars.put(s,value);
        }
        return f.substituteVariables(vars);
    }


    /** ***************************************************************
     *  Replace term2 with term1
     */
    public Formula rename(String term2, String term1) {

		if (logger.isLoggable(Level.FINER)) {
			String[] params = { "term2 = " + term2, "term1 = " + term1 };
			logger.entering("Formula", "rename", params);
		}

        Formula newFormula = new Formula();
        newFormula.read("()");
        if (this.atom()) {
            if (theFormula.equals(term2))
                theFormula = term1;
            return this;
        }
        if (!this.empty()) {
            Formula f1 = new Formula();
            f1.read(this.car());
			// logger.finest("car: " + f1.theFormula);

            if (f1.listP())
                newFormula = newFormula.cons(f1.rename(term2,term1));
            else
                newFormula = newFormula.append(f1.rename(term2,term1));
            Formula f2 = new Formula();
            f2.read(this.cdr());

			// logger.finest("cdr: " + f2);
            newFormula = newFormula.append(f2.rename(term2,term1));
        }

		logger.exiting("Formula", "rename", newFormula.theFormula);

        return newFormula;
    }

    /** ***************************************************************
     * A test method.
     */
    public static void testClausifier(String[] args) {

        BufferedWriter bw = null;
        try {
            long t1 = System.currentTimeMillis();
            int count = 0;
            String inpath = args[0];
            String outpath = args[1];
            if (isNonEmptyString(inpath) && isNonEmptyString(outpath)) {
                File infile = new File(inpath);
                if (infile.exists()) {
                    KIF kif = new KIF();
                    kif.setParseMode(KIF.RELAXED_PARSE_MODE);
                    kif.readFile(infile.getCanonicalPath());
                    if (! kif.formulas.isEmpty()) {
                        File outfile = new File(outpath);
                        if (outfile.exists()) { outfile.delete(); }
                        bw = new BufferedWriter(new FileWriter(outfile, true));
                        Iterator it = kif.formulas.values().iterator();
                        Iterator it2 = null;
                        Formula f = null;
                        Formula clausalForm = null;
                        while (it.hasNext()) {
                            it2 = ((List) it.next()).iterator();
                            while (it2.hasNext()) {
                                f = (Formula) it2.next();
                                clausalForm = Clausifier.clausify(f);
                                if (clausalForm != null) {
                                    bw.write(clausalForm.theFormula);
                                    bw.newLine();
                                    count++;
                                }
                            }
                        }
                        try {
                            bw.flush();
                            bw.close();
                            bw = null;
                        }
                        catch (Exception bwe) {
                            bwe.printStackTrace();
                        }
                    }
                }
            }
            long dur = (System.currentTimeMillis() - t1);
			logger.info(count + " clausal forms written in " + (dur / 1000.0)
					+ " seconds");
        }
        catch (Exception ex) {
			logger.warning(ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            if (bw != null) {
                try {
                    bw.close();
                }
                catch (Exception e2) {
					logger.warning(e2.getMessage());
                    e2.printStackTrace();
                }
            }
        }
        return;
    }

    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest3() {

		// logger.finest("--------------------INFO in Formula.resolveTest3()--------------");
        Formula newResult = new Formula();
        Formula f1 = new Formula();
        Formula f2 = new Formula();

        f1.read("(or (attribute ?VAR2 Criminal) (not (recipient ?VAR1 ?VAR3)) (not (patient ?VAR1 ?VAR4)) (not (instance ?VAR4 Weapon)) " +
                  "(not (instance ?VAR3 Nation)) (not (instance ?VAR1 Selling)) (not (agent ?VAR1 ?VAR2)) (not (attribute ?VAR3 Hostile)) " +
                  "(not (attribute ?VAR2 American)))");
        f2.read("(or (not (recipient ?VAR5 ?VAR3)) (not (patient ?VAR5 ?VAR4)) (not (instance ?VAR5 Selling)) (not (instance ?VAR4 Weapon)) " +
                  "(not (instance ?VAR3 Nation)) (not (agent ?VAR5 ?VAR2)) (not (attribute ?VAR3 Hostile)) (not (attribute ?VAR2 American)))");

		// logger.finest("f1: " + f1);
		// logger.finest("f2: " + f2);
		// logger.finest("resolution result mapping: " + f1.resolve(f2, newResult));
		// logger.finest("resolution result: " + newResult);
    }


    /** ***************************************************************
     * A test method.
     */
    public static void unifyTest1() {

		// logger.finest("--------------------INFO in Formula.unifyTest1()--------------");
        Formula f2 = new Formula();
        f2.read("(m (SkFn 1 ?VAR1) ?VAR1)");
        f2 = Clausifier.clausify(f2);
        Formula f3 = new Formula();
        f3.read("(m ?VAR1 Org1-1)");

		// logger.finest("f2: " + f2);
		// logger.finest("f3: " + f3);
        f3 = Clausifier.clausify(f3);
        TreeMap tm = f3.unify(f2);
		// logger.finest("Map result: " + tm);
        if (tm != null)
            f3 = f3.substitute(tm);
		// logger.finest("f2: " + f2);
		// logger.finest("f3: " + f3);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void unifyTest2() {

		// logger.finest("--------------------INFO in Formula.unifyTest2()--------------");
        Formula f2 = new Formula();
        f2.read("(attribute (SkFn Jane) Investor)");
        f2 = Clausifier.clausify(f2);
        Formula f3 = new Formula();
        f3.read("(attribute ?X Investor)");
        f3 = Clausifier.clausify(f3);

		// logger.finest("f2: " + f2);
		// logger.finest("f3: " + f3);

        TreeMap tm = f3.unify(f2);
		// logger.finest("mapping: " + tm);
        if (tm != null)
            f3 = f3.substitute(tm);
		// logger.finest("f2: " + f2);
		// logger.finest("f3: " + f3);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void unifyTest3() {

		// logger.finest("--------------------INFO in Formula.unifyTest3()--------------");
        Formula f2 = new Formula();
        f2.read("(agent ?VAR4 ?VAR1)");
        f2 = Clausifier.clausify(f2);
        Formula f3 = new Formula();
        f3.read("(agent ?VAR5 West)");
        f3 = Clausifier.clausify(f3);
        f3 = f2.separateVariableScope(f3);
		// logger.finest("f2: " + f2);
		// logger.finest("f3: " + f3);
        TreeMap tm = f3.unify(f2);
		// logger.finest("mapping: " + tm);
        if (tm != null)
            f3 = f3.substitute(tm);
		// logger.finest("f2: " + f2);
		// logger.finest("f3: " + f3);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void unifyTest4() {

		// logger.finest("--------------------INFO in Formula.unifyTest4()--------------");
        Formula f1 = new Formula();
        Formula f2 = new Formula();
        //cnf1.read("(equal (ListOrderFn (ListFn ?VAR1 ?VAR2) (ListLengthFn (ListFn ?VAR1 ?VAR2))) ?VAR2)");
        f1.read("(equal (ListOrderFn (ListFn ?VAR1 ?VAR2)) ?VAR2)");
        f2.read("(equal (ImmediateFamilyFn ?VAR1) Org1-1)");
		// logger.finest("f1: " + f1);
		// logger.finest("f2: " + f2);
        TreeMap tm = f1.unify(f2);
		// logger.finest("mapping: " + tm);
        if (tm != null)
            f1 = f1.substitute(tm);
		// logger.finest("f1: " + f1);
		// logger.finest("f2: " + f2);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void unifyTest5() {

		// logger.finest("--------------------INFO in Formula.unifyTest5()--------------");
        Formula f1 = new Formula();
        Formula f2 = new Formula();
        //cnf1.read("(equal (ListOrderFn (ListFn ?VAR1 ?VAR2) (ListLengthFn (ListFn ?VAR1 ?VAR2))) ?VAR2)");
        f1.read("(equal ?VAR1 ?VAR2)");
        f2.read("(equal (ImmediateFamilyFn ?VAR3) Org1-1)");
		// logger.finest("f1: " + f1);
		// logger.finest("f2: " + f2);
        TreeMap tm = f1.unify(f2);
		// logger.finest("mapping: " + tm);
        if (tm != null)
            f1 = f1.substitute(tm);
		// logger.finest("f1: " + f1);
		// logger.finest("f2: " + f2);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {

        //Formula.resolveTest3();
        //Formula.unifyTest1();
        //Formula.unifyTest2();
        //Formula.unifyTest3();
        //Formula.unifyTest4();
        //Formula.unifyTest5();
        //Formula f1 = new Formula();
        //Formula f2 = new Formula();
        //Formula f3 = new Formula();
        /**
        f1.read("(=> (attribute ?Agent Investor) (exists (?Investing) (agent ?Investing ?Agent)))");
        System.out.println(f1);
        System.out.println(f1.clausify());
        f2.read("(attribute Bob Investor)");
        TreeMap tm = f1.unify(f2);
        if (tm != null)
            System.out.println(f1.substitute(tm));
        f3.read("(attribute ?X Investor)");
        tm = f3.unify(f2);
        System.out.println(tm);
        if (tm != null)
            System.out.println(f3.substitute(tm));
        f1.read("(=> (and (instance ?CITY AmericanCity) (part ?CITY California) " +
                "(not (equal ?CITY LosAngelesCalifornia))) (greaterThan (CardinalityFn " +
                "(ResidentFn LosAngelesCalifornia)) (CardinalityFn (ResidentFn ?CITY))))");
        System.out.println(f1);
        System.out.println(f1.clausify());
        f1.read("(not (instance ?X Human))");
        System.out.println(f1);
        System.out.println(f1.clausify());
        f1.read("(not (and (instance ?X Human) (attribute ?X Male)))");
        System.out.println(f1);
        System.out.println(f1.clausify());
        f1.read("(not (instance ?X2 Human))");
        System.out.println(f1.isSimpleNegatedClause());

        System.out.println(f1.append(f3));
 * */

        //f1.read("(not (and (exists (?MEMBER) (member ?MEMBER Org1-1)) (instance Org1-1 Foo))");
        //f1.read("(not (attribute ?VAR1 Criminal))");
        //f2.read("(or (not (attribute ?X5 American)) (not (instance ?X6 Weapon)) (not (instance ?X7 Nation)) " +
        //        "(not (attribute ?X7 Hostile)) (not (instance ?X8 Selling)) (not (agent ?X8 ?X5)) (not (patient ?X8 ?X6)) " +
        //        "(not (recipient ?X8 ?X7)) (attribute ?X5 Criminal))");
        //f1.read("(or (not (attribute ?VAR1 American)) (not (instance ?VAR2 Weapon)) (not (instance ?VAR3 Nation)) (not (attribute ?VAR3 Hostile)) (not (instance ?VAR4 Selling)) (not (agent ?VAR4 ?VAR1)) (not (patient ?VAR4 ?VAR2)) (not (recipient ?VAR4 ?VAR3)))");
        // (or
        //   (not
        //     (attribute ?VAR1 American))
        //   (not
        //     (instance ?VAR2 Weapon))
        //   (not
        //     (instance ?VAR3 Nation))
        //   (not
        //     (attribute ?VAR3 Hostile))
        //   (not
        //     (instance ?VAR4 Selling))
        //   (not
        //     (agent ?VAR4 ?VAR1))
        //   (not
        //     (patient ?VAR4 ?VAR2))
        //   (not
        //     (recipient ?VAR4 ?VAR3)))
        //f2.read("(or (agent ?X15 West) (not (possesses Nono ?X16)) (not (instance ?X16 Missile)))");
        //f1 = f1.clausify();
        //f2.read("(=> (instance ?X290 Collection) (exists (?X12) (and (instance ?X290 Foo) (member ?X12 ?X290))))");
        //System.out.println(f2.toNegAndPosLitsWithRenameInfo());
        //f2 = f2.clausify();
        //System.out.println(f2);
        //f3.read("(member (SkFn 1 ?X290) ?X290))");
        //System.out.println(f3.isSimpleClause());
        //Formula result = new Formula();
        //TreeMap mappings = f1.resolve(f2,result);
        //System.out.println(mappings);
        //System.out.println(result);
        //f1.read("(not (p a))");
        //f2.read("(not (p a))");
        //System.out.println(f1.unify(f2));
        //f1.read("(not (q a))");
        //f2.read("(not (p a))");
        //System.out.println(f1.unify(f2));

        //f1.read("(s O C)");
        //f2.read("(or (not (s ?X7 C)) (not (s O C)))");
        //Formula newResult = new Formula();
        //System.out.println(f1.resolve(f2,newResult));
        //System.out.println(newResult);

        //f1.read("(or (not (possesses Nono ?X16)) (not (instance ?X16 Missile)) (not (attribute West American)) (not (instance ?VAR2 Weapon)) (not (instance ?VAR3 Nation)) (not (attribute ?VAR3 Hostile)) (not (instance ?X15 Selling)) (not (patient ?X15 ?VAR2)) (not (recipient ?X15 ?VAR3))) ");
        //System.out.println(f1.toCanonicalClausalForm());

        /*
        f1.read("(not (enemies Nono America))");
        f2.read("(enemies Nono America)");
        System.out.println(f1.resolve(f2,f3));
        System.out.println(f3);
        f1.read("(or (not (agent ?VAR1 West)) (not (enemies Nono America)) (not (instance ?VAR1 Selling)) " +
                "(not (instance ?VAR2 Weapon)) (not (patient ?VAR1 ?VAR2)) (not (recipient ?VAR1 Nono)))");
        System.out.println(f1.resolve(f2,f3));
        System.out.println(f3); */

/*
        f1.read("()");
        f2.read("()");
        System.out.println(f1.appendClauseInCNF(f2));
        f2.read("(foo A B)");
        System.out.println(f1.appendClauseInCNF(f2));   // yields (foo A B)
        f1.read("(foo A B)");
        f2.read("(bar B C)");
        System.out.println(f1.appendClauseInCNF(f2));   // yields (or (foo A B) (bar B C))
        f1.read("(or (foo A B) (not (bar B C)))");
        f2.read("(not (baz D E))");
        System.out.println(f2.appendClauseInCNF(f1));   // yields (or (not (baz D E)) (foo A B) (not (bar B C)))
        System.out.println(f1.appendClauseInCNF(f2));   // yields (or (foo A B) (not (bar B C)) (not baz D E)))
        f1.read("(or (foo A B) (bar B C))");
        f2.read("(or (baz D E) (bop F G))");
        System.out.println(f1.appendClauseInCNF(f2));   // yields (or (foo A B) (bar B C) (baz D E) (bop F G))
*/
        /**
        f1.read("(member (SkFn 1 ?X3) ?X3)");
        f3.read("(member ?VAR1 Org1-1)");
        tm = f1.unify(f3);
        System.out.println(tm);
        System.out.println(f1.substitute(tm));
        System.out.println(f3.substitute(tm));

        f1.read("(attribute West American)");
        f3.read("(attribute ?VAR1 Criminal)");
        System.out.println(f1);
        System.out.println(f3);
        System.out.println(f1.unify(f3));
         * */

    }

}  // Formula.java
