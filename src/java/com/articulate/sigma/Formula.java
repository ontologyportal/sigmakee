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
Systems, August 9, Acapulco, Mexico. See also http://github.com/ontologyportal

Authors:
Adam Pease
Infosys LTD.

Formula is an important class that contains information and operations
about individual SUO-KIF formulas.
*/

package com.articulate.sigma;

import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.util.*;

/** ************************************************************
 * Handle operations on an individual formula.  This includes
 * formatting for presentation as well as pre-processing for sending
 * to the inference engine.
 */
public class Formula implements Comparable, Serializable {

    public static boolean debug = false;

    public static final String AND    = "and";
    public static final String OR     = "or";
    public static final String XOR    = "xor";
    public static final String NOT    = "not";
    public static final String IF     = "=>";
    public static final String IFF    = "<=>";
    public static final String UQUANT = "forall";
    public static final String EQUANT = "exists";
    public static final String EQUAL  = "equal";
    public static final String GT     = "greaterThan";
    public static final String GTET   = "greaterThanOrEqualTo";
    public static final String LT     = "lessThan";
    public static final String LTET   = "lessThanOrEqualTo";

    public static final String KAPPAFN  = "KappaFn";
    public static final String PLUSFN   = "AdditionFn";
    public static final String MINUSFN  = "SubtractionFn";
    public static final String TIMESFN  = "MultiplicationFn";
    public static final String DIVIDEFN = "DivisionFn";
    public static final String FLOORFN = "FloorFn";
    public static final String ROUNDFN = "RoundFn";
    public static final String CEILINGFN = "CeilingFn";
    public static final String REMAINDERFN = "RemainderFn";
    public static final String SKFN     = "SkFn";
    public static final String SK_PREF = "Sk";
    public static final String FN_SUFF = "Fn";
    public static final String V_PREF  = "?";
    public static final String R_PREF  = "@";
    public static final String VX      = V_PREF + "X";
    public static final String VVAR    = V_PREF + "VAR";
    public static final String RVAR    = R_PREF + "ROW";

    public static final String LP = "(";
    public static final String RP = ")";
    public static final String SPACE = " ";

    public static final String LOG_TRUE  = "True";
    public static final String LOG_FALSE = "False";

    public static final String TERM_MENTION_SUFFIX  = "__m";
    public static final String CLASS_SYMBOL_SUFFIX  = "__t";  // for the case when a class is used as an instance
    public static final String TERM_SYMBOL_PREFIX   = "s__";
    public static final String TERM_VARIABLE_PREFIX = "V__";

    /** The SUO-KIF logical operators. */
    public static final List<String> LOGICAL_OPERATORS = Arrays.asList(UQUANT,
                                                                       EQUANT,
                                                                       AND,
                                                                       OR,
                                                                       XOR,
                                                                       NOT,
                                                                       IF,
                                                                       IFF
    );

    /** SUO-KIF mathematical comparison predicates. */
    public static final List<String> COMPARISON_OPERATORS = Arrays.asList(EQUAL,
                                                                          GT,
                                                                          GTET,
                                                                          LT,
                                                                          LTET
    );

    /** SUO-KIF mathematical comparison predicates. */
    public static final List<String> INEQUALITIES = Arrays.asList(GT,
                                                                  GTET,
                                                                  LT,
                                                                  LTET
    );

    /** The SUO-KIF mathematical functions are implemented in Vampire, but not yet EProver. */
    public static final List<String> MATH_FUNCTIONS = Arrays.asList(PLUSFN,
                                                                    MINUSFN,
                                                                    TIMESFN,
                                                                    DIVIDEFN,
                                                                    FLOORFN,
                                                                    ROUNDFN,
                                                                    CEILINGFN,
                                                                    REMAINDERFN
    );

    public static final List<String> DOC_PREDICATES = Arrays.asList("documentation",
                                                                    "comment",
                                                                    "format",
                                                                    "termFormat",
                                                                    "lexicon",
                                                                    "externalImage",
                                                                    "synonymousExternalConcept"
    );

    public static final List<String> DEFN_PREDICATES = Arrays.asList("instance",
                                                                     "subclass",
                                                                     "domain",
                                                                     "domainSubclass",
                                                                     "range",
                                                                     "rangeSubclass",
                                                                     "subAttribute",
                                                                     "subrelation"
    );

    /** The source file in which the formula appears. */
    public String sourceFile;

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

    // An ArrayList of String messages with a message that has a reserved character of ':'
    // dividing the message from a formula or term that will be hyperlinked and formmated
    public Set<String> errors = new TreeSet<>();

    /** Warnings found during execution. */
    public Set<String> warnings = new TreeSet<>();

    /** The formula in textual forms. */
    private String theFormula;

    // if not a directly authored form, document how it was derived
    public Derivation derivation = new Derivation();

    public boolean higherOrder = false;
    public boolean simpleClause = false;
    public boolean comment = false;
    public boolean isFunctional = false;
    public boolean isGround = true; // assume true unless a variable is found during parsing
    public String relation = null;

    public List<String> stringArgs = new ArrayList<>(); // cached - only in the case of a simpleClause

    public List<Formula> args = new ArrayList<>();

    public String getSourceFile() {
        return this.sourceFile;
    }

    public void setSourceFile(String filename) { this.sourceFile = filename; }

    public Set<String> getErrors() {
        return this.errors;
    }

    // caches of frequently computed sets of variables in the formula
    public Set<String> allVarsCache = new HashSet<>();

    /* an ArrayList
     * containing a pair of ArrayLists.  The first contains all
     * explicitly quantified variables in the Formula.  The second
     * contains all variables in Formula that are not within the scope
     * of some explicit quantifier. */
    public List<Set<String>> allVarsPairCache = new ArrayList<>();

    public Set<String> quantVarsCache = new HashSet<>();
    public Set<String> unquantVarsCache = new HashSet<>();
    public Set<String> existVarsCache = new HashSet<>();
    public Set<String> univVarsCache = new HashSet<>();
    public Set<String> termCache = new HashSet<>();

    public Set<String> predVarCache = null; // null if not set, empty if no pred vars
    public Set<String> rowVarCache = null; // null if not set, empty if no row vars

    // includes the leading '?'.  Does not include row variables
    public Map<String,Set<String>> varTypeCache = new HashMap<>();

    /** ***************************************************************
     * A list of TPTP formulas (Strings) that together constitute the
     * translation of theFormula.  This member is a Set, because
     * predicate variable instantiation and row variable expansion
     * might cause theFormula to expand to several TPTP formulas.
     */
    public Set<String> theTptpFormulas = new HashSet<>();

    //any extra sort signatures not computed in advance
    public Set<String> tffSorts = new HashSet<>();

    /** *****************************************************************
     * A list of clausal (resolution) forms generated from this
     * Formula.
     */
    private List theClausalForm = null;

    /** *****************************************************************
     * Constructor to build a formula from an existing formula.  This isn't
     * a complete deepCopy() since it leaves out the errors and warnings
     * variables
     */
    public Formula(Formula f) {

        this.endLine = f.endLine;
        this.startLine = f.startLine;
        this.sourceFile = f.sourceFile;
        this.theFormula = f.theFormula;
        this.comment = f.comment;
        if (f.higherOrder)
            this.higherOrder = true;
        this.derivation = f.derivation;
        this.allVarsPairCache.addAll(f.allVarsPairCache);
        this.quantVarsCache.addAll(f.quantVarsCache);
        this.unquantVarsCache.addAll(f.unquantVarsCache);
        this.existVarsCache.addAll(f.existVarsCache);
        this.univVarsCache.addAll(f.univVarsCache);
        this.termCache.addAll(f.termCache);
        if (f.predVarCache != null) {
            this.predVarCache = new HashSet<>();
            this.predVarCache.addAll(f.predVarCache);
        }
        if (f.rowVarCache != null) {
            this.rowVarCache = new HashSet<>();
            this.rowVarCache.addAll(f.rowVarCache);
        }
        this.varTypeCache.putAll(f.varTypeCache);
        this.isGround = f.isGround;
    }

    /** *****************************************************************
     */
    public Formula() {
    }

    /** *****************************************************************
     * Just set the textual version of the formula
     */
    public Formula(String f) {
        theFormula = f;
    }

    /** *****************************************************************
     * the textual version of the formula
     */
    public String getFormula() {
        return theFormula;
    }

    /** *****************************************************************
     * the textual version of the formula
     */
    public void setFormula(String f) {
        theFormula = f;
    }

    /** *****************************************************************
     * the textual version of the formula
     */
    public static Formula createComment(String input) {

        Formula f = new Formula();
        f.theFormula = input;
        f.comment = true;
        return f;
    }

    /** *****************************************************************
     */
    public void printCaches() {

        System.out.println("Formula: " + this);
        System.out.println("all vars: " + allVarsCache);
        System.out.println("all vars pair: " + allVarsPairCache);
        System.out.println("quant vars: " + quantVarsCache);
        System.out.println("unquant vars: " + unquantVarsCache);
        System.out.println("exist vars: " + existVarsCache);
        System.out.println("univ vars: " + univVarsCache);
        System.out.println("terms: " + termCache);

        System.out.println("pred vars: " + predVarCache);
        System.out.println("row vars: " + rowVarCache);
    }

    /** ***************************************************************
     * Returns a List of the clauses that together constitute the
     * resolution form of this Formula.  The list could be empty if
     * the clausal form has not yet been computed.
     *
     * @return ArrayList
     */
    public List getTheClausalForm() {

        if (theClausalForm == null) {
            if (!StringUtil.emptyString(theFormula))
                theClausalForm = Clausifier.toNegAndPosLitsWithRenameInfo(this);
        }
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
    public List getClauses() {

        List clausesWithVarMap = getTheClausalForm();
        if ((clausesWithVarMap == null) || clausesWithVarMap.isEmpty())
            return null;
        return (List) clausesWithVarMap.get(0);
    }

    /** ***************************************************************
     * Returns a map of the variable renames that occurred during the
     * translation of this Formula into the clausal (resolution) form
     * accessible via this.getClauses().
     *
     * @return A Map of String (SUO-KIF variable) key-value pairs.
     */
    public Map getVarMap() {

        List clausesWithVarMap = getTheClausalForm();
        if ((clausesWithVarMap == null) || (clausesWithVarMap.size() < 3))
            return null;
        return (Map) clausesWithVarMap.get(2);
    }

    /** ***************************************************************
     * Returns a map of the variable types.
     *
     * @return A Map of String (SUO-KIF variable) key-value pairs.
     */
    public Map<String,Set<String>> getVarTypes(KB kb) {

        if (varTypeCache != null)
            return varTypeCache;
        FormulaPreprocessor fp = new FormulaPreprocessor();
        varTypeCache = fp.computeVariableTypes(this,kb);
        return varTypeCache;
    }

    /** ***************************************************************
     * Returns the type of a variable or null if it doesn't exist.
     */
    public Set<String> getVarType(KB kb, String var) {

        Map<String,Set<String>> varTypes = getVarTypes(kb);
        if (varTypes.containsKey(var))
            return varTypes.get(var);
        else
            return null;
    }

    /** ***************************************************************
     * This constant indicates the maximum predicate arity supported
     * by the current implementation of Sigma.
     */
    public static final int MAX_PREDICATE_ARITY = 7;

    /** ***************************************************************
     * Read a String into the variable 'theFormula'.
     */
    public void read(String s) {

        theFormula = s;
        allVarsCache = new HashSet<>();
        allVarsPairCache = new ArrayList<>();
        quantVarsCache = new HashSet<>();
        unquantVarsCache = new HashSet<>();
        existVarsCache = new HashSet<>();
        univVarsCache = new HashSet<>();
        termCache = new HashSet<>();
        args = new ArrayList<>();
        stringArgs = new ArrayList<>();
    }

    /** ***************************************************************
    *  @return a unique ID by appending the hashCode() of the
    *  formula String to the file name in which it appears
     */
    public String createID() {

        String fname = sourceFile;
        if (!StringUtil.emptyString(fname) && fname.lastIndexOf(File.separator) > -1)
            fname = fname.substring(fname.lastIndexOf(File.separator) + 1);
        int hc = theFormula.hashCode();
        String result;
        if (hc < 0)
            result = "N" + (Integer.valueOf(hc)).toString().substring(1) + fname;
        else
            result = (Integer.valueOf(hc)).toString() + fname;
        return result;
    }

    /** ***************************************************************
     * Copy the Formula.  This is in effect a deep copy although it ignores
     * the errors and warnings variables.
     */
    public Formula copy() {

        return new Formula(this);
    }

    /** ***************************************************************
     */
    public Formula deepCopy() {
        return copy();
    }

    /** ***************************************************************
     * Implement the Comparable interface by defining the compareTo
     * method.  Formulas are equal if their formula strings are equal.
     */
    @Override
    public int compareTo(Object f) throws ClassCastException {

    	if (f == null) {
            System.err.println("Error in Formula.compareTo(): null formula");
            throw new ClassCastException("Error in Formula.compareTo(): null formula");
    	}
        if (!(f instanceof Formula))
            throw new ClassCastException("Error in Formula.compareTo(): "
                                         + "Class cast exception for argument of class: "
                                         + f.getClass().getName());
        return theFormula.compareTo(((Formula) f).theFormula);
    }

    /** ***************************************************************
     */
    static class SortByLine implements Comparator<Formula> {

        @Override
        public int compare(Formula a, Formula b) {
            return a.startLine - b.startLine;
        }
    }

    /** ***************************************************************
     * Returns true if the Formula contains no unbalanced parentheses
     * or unbalanced quote characters, otherwise returns false.
     *
     * @return boolean
     */
    public boolean isBalancedList() {

        boolean ans = false;
        if (this.listP()) {
            if (this.empty())
                ans = true;
            else {
                String input = this.theFormula.trim();
                List<Character> quoteChars = Arrays.asList('"', '\'');
                int i = 0;
                int len = input.length();
                int end = len - 1;
                int pLevel = 0;
                int qLevel = 0;
                char prev = '0';
                char ch;
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
            }
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

        String ans = null;
        if (this.listP()) {
            if (this.empty())
                // NS: Clean this up someday.
                ans = "";  // this.theFormula;
            else {
                String input = this.theFormula.trim();
                StringBuilder sb = new StringBuilder();
                List quoteChars = Arrays.asList('"', '\'');
                int i = 1;
                int len = input.length();
                int end = len - 1;
                int level = 0;
                char prev = '0';
                char ch;
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
                            if (level <= 0)
                                break;
                        }
                        else if (Character.isWhitespace(ch) && (level <= 0)) {
                            if (sb.length() > 0)
                                break;
                        }
                        else if (quoteChars.contains(ch) && (prev != '\\')) {
                            sb.append(ch);
                            insideQuote = true;
                            quoteCharInForce = ch;
                        }
                        else
                            sb.append(ch);
                    }
                    else if (quoteChars.contains(ch)
                             && (ch == quoteCharInForce)
                             && (prev != '\\')) {
                        sb.append(ch);
                        insideQuote = false;
                        quoteCharInForce = '0';
                        if (level <= 0)
                            break;
                    }
                    else
                        sb.append(ch);
                    prev = ch;
                    i++;
                }
                ans = sb.toString();
            }
        }
        return ans;
    }

    /** ***************************************************************
     * Return the LISP 'cdr' of the formula - the rest of a list minus its
     * first element.
     * Note that this operation has no side effect on the Formula.
     */
    public String cdr() {

        String ans = null;
        if (this.listP()) {
            if (this.empty())
                ans = this.theFormula;
            else {
                String input = theFormula.trim();
                List<Character> quoteChars = Arrays.asList('"', '\'');
                int i = 1;
                int len = input.length();
                int end = len - 1;
                int level = 0;
                char prev = '0';
                char ch;
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
                            if (level <= 0)
                                break;
                        }
                        else if (Character.isWhitespace(ch) && (level <= 0)) {
                            if (carCount > 0)
                                break;
                        }
                        else if (quoteChars.contains(ch) && (prev != '\\')) {
                            carCount++;
                            insideQuote = true;
                            quoteCharInForce = ch;
                        }
                        else
                            carCount++;
                    }
                    else if (quoteChars.contains(ch)
                             && (ch == quoteCharInForce)
                             && (prev != '\\')) {
                        carCount++;
                        insideQuote = false;
                        quoteCharInForce = '0';
                        if (level <= 0)
                            break;
                    }
                    else
                        carCount++;
                    prev = ch;
                    i++;
                }
                if (carCount > 0) {
                    int j = i + 1;
                    if (j < end)
                        ans = LP + input.substring(j, end).trim() + RP;
                    else
                        ans = LP+RP;
                }
            }
        }
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

        Formula ans = this;
        String fStr = this.theFormula;
        if (!StringUtil.emptyString(obj) && !StringUtil.emptyString(fStr)) {
            String theNewFormula;
            if (this.listP()) {
                if (this.empty())
                    theNewFormula = (LP + obj + RP);
                else
                    theNewFormula = (LP + obj + SPACE + fStr.substring(1, (fStr.length() - 1)) + RP);
            }
            else
                // This should never happen during clausification, but
                // we include it to make this procedure behave
                // (almost) like its LISP namesake.
                theNewFormula = (LP + obj + " . " + fStr + RP);
            if (theNewFormula != null) {
                ans = new Formula();
                ans.read(theNewFormula);
            }
        }
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
     * Returns the LISP 'car' of the formula as a new Formula, if
     * possible, else returns null.
     *
     * Note that this operation has no side effect on the Formula.
     * @return a Formula, or null.
     */
    public Formula carAsFormula() {

        String thisCar = this.car();
        //if (listP(thisCar)) {
            Formula f = new Formula();
            f.read(thisCar);
            return f;
       // }
        //return null;
    }

    /** ***************************************************************
     * Returns the LISP 'cadr' (the second list element) of the
     * formula.
     *
     * Note that this operation has no side effect on the Formula.
     * @return a String, or the empty string if the is no cadr.
     */
    public String cadr() {

        return this.getStringArgument(1);
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
        if (fCdr != null)
            return fCdr.cdr();
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
        return this.getStringArgument(2);
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
            System.err.println("Error in Formula.append(): attempt to append to non-list: " + theFormula);
            return this;
        }
        if (f == null || f.theFormula == null || "".equals(f.theFormula) || f.theFormula.equals("()"))
            return newFormula;
        f.theFormula = f.theFormula.trim();
        if (!f.atom())
            f.theFormula = f.theFormula.substring(1,f.theFormula.length()-1);
        int lastParen = theFormula.lastIndexOf(RP);
        String sep = "";
        if (lastParen > 1)
            sep = SPACE;
        newFormula.theFormula = newFormula.theFormula.substring(0,lastParen) + sep + f.theFormula + RP;
        return newFormula;
    }

    /** ***************************************************************
     * Test whether the String is a LISP atom.
     */
    public static boolean atom(String s) {

        boolean ans = false;
        if (!StringUtil.emptyString(s)) {
            String str = s.trim();
            ans = (StringUtil.isQuotedString(s) ||
                  (!str.contains(RP) && !str.matches(".*\\s.*")) );
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
        if (!StringUtil.emptyString(s)) {
            String str = s.trim();
            ans = (str.startsWith(LP) && str.endsWith(RP));
        }
        return ans;
    }

    /** ***************************************************************
     * @see #validArgs() validArgs below for documentation
     */
    private String validArgsRecurse(Formula f, String filename, Integer lineNo) {

		if ("".equals(f.theFormula) || !f.listP() || f.atom() || f.empty())
			return "";
        String pred = f.car();
        String rest = f.cdr();
        Formula restF = new Formula();
        restF.read(rest);
        int argCount = 0;
        String arg, result, errString;
        Formula argF;
        while (!restF.empty()) {
            argCount++;
            arg = restF.car();
            argF = new Formula();
            argF.read(arg);
            result = validArgsRecurse(argF, filename, lineNo);
            if (!"".equals(result))
                return result;
            restF.theFormula = restF.cdr();
        }
        String location = "";
        if ((filename != null) && (lineNo != null))
            location = "near line " + lineNo + " in " + filename;
        if (pred.equals(AND) || pred.equals(OR) || pred.equals(XOR)) {
            if (argCount < 2) {
                errString = "Too few arguments for 'and' or 'or' " + location + ": " + f.toString();
                errors.add(errString);
                return errString;
            }
        }
        else if (pred.equals(UQUANT) || pred.equals(EQUANT)) {
            if (argCount != 2) {
                errString = "Wrong number of arguments for quantifer " + location + ": " + f.toString();
                errors.add(errString);
                return errString;
            }
            else {
                Formula quantF = new Formula();
                quantF.read(rest);
                if (!listP(quantF.car())) {
                    errString = "No var list for quantifier " + location + ": " + f.toString();
                    errors.add(errString);
                    return errString;
                }
            }
        }
        else if (pred.equals(IFF) || pred.equals(IF)) {
            if (argCount != 2) {
                errString = "Wrong number of arguments for '<=>' or '=>' " + location + ": " + f.toString();
                errors.add(errString);
                return errString;
            }
        }
        else if (pred.equals(EQUAL)) {
            if (argCount != 2) {
                errString = "Wrong number of arguments for 'equals' " + location + ": " + f.toString();
                errors.add(errString);
                return errString;
            }
        }
        else if (!(isVariable(pred)) && (argCount > (MAX_PREDICATE_ARITY + 1))) {
            //System.out.println("info in KIF.parse(): pred: " + pred);
            //System.out.println("info in KIF.parse(): " + this);
            errString = "Maybe too many arguments " + location + ": " + f.toString();
            errors.add(errString);
            return errString;
        }
        return "";
    }

    /** ***************************************************************
     * Test whether the Formula uses logical operators and predicates
     * with the correct number of arguments.  "equals", "<=>", and
     * "=>" are strictly binary.  "or", "xor" and "and" are binary or
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

        if (theFormula == null || "".equals(theFormula))
            return "";
        Formula f = new Formula();
        f.read(theFormula);
        String result = validArgsRecurse(f, filename, lineNo);
        return result;
    }

    /** ***************************************************************
     * Test whether the Formula uses logical operators and predicates
     * with the correct number of arguments.  "equals", "<=>", and
     * "=>" are strictly binary.  "or", "xor" and "and" are binary or
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
     * TODO: Not yet implemented!  Test whether the Formula has variables that are not properly
     * quantified.  The case tested for is whether a quantified variable
     * in the antecedent appears in the consequent or vice versa.
     *
     *  @return an empty String if there are no problems or an error message
     *  if there are.
     */
    @Deprecated
    public String badQuantification() {
        return "";
    }

    /** ***************************************************************
     * Parse a String into an ArrayList of Formulas. The String must be
     * a LISP-style list.
     */
    private List<Formula> parseList(String s) {

        List<Formula> result = new ArrayList<>();
        Formula f = new Formula();
        f.read(LP + s + RP);
        if (f.empty())
            return result;
        String car;
        Formula newForm;
        while (!f.empty()) {
            car = f.car();
            f.read(f.cdr());
            newForm = new Formula();
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

        List<Formula> thisList = parseList(this.theFormula.substring(1,this.theFormula.length()-1));
        List<Formula> sList = parseList(s.substring(1,s.length()-1));
        if (thisList.size() != sList.size())
            return false;
        for (int i = 0; i < thisList.size(); i++) {
            for (int j = 0; j < sList.size(); j++) {
                if ((thisList.get(i)).logicallyEquals((sList.get(j)).theFormula)) {
                    sList.remove(j);
                    j = sList.size();
                }
            }
        }
        return sList.isEmpty();
    }

    /** ***************************************************************
     * Test if the contents of the formula are equal to the argument
     * at a deeper level than a simple string equals.  The only logical
     * manipulation is to treat conjunctions and disjunctions as unordered
     * bags of clauses. So (and A B C) will be logicallyEqual(s) for example,
     * to (and B A C).  Note that this is a fairly time-consuming operation
     * and should not generally be used for comparing large sets of formulas.
     */
    @Deprecated
    public boolean logicallyEquals(String s) {

        if (this.equals(s))
            return true;
        if (Formula.atom(s) && s.compareTo(theFormula) != 0)
            return false;

        Formula form = new Formula();
        form.read(this.theFormula);
        Formula sform = new Formula();
        sform.read(s);

        if (Formula.AND.equals(form.car().intern()) || Formula.OR.equals(form.car().intern()) || Formula.XOR.equals(form.car().intern())) {
            if (sform.car().intern() == null ? sform.car().intern() != null : !sform.car().intern().equals(sform.car().intern()))
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
     * If equals is overridden, hashCode must use the same
     * "significant" fields.
     */
    @Override
    public int hashCode() {

        String thisString = Clausifier.normalizeVariables(this.theFormula).trim();
        return (thisString.hashCode());
    }

    /** ***************************************************************
     * Test if the contents of the formula are equal to the
     * argument. Normalize all variables so that formulas can be equal
     * independent of their variable names, which have no semantics.
     */
    @Override
    public boolean equals(Object o) {

        if (o == null ) {
            return false;
        }

        if (!(o instanceof Formula))
            return false;
        Formula f = (Formula) o;
        if (f.theFormula == null) {
            return (this.theFormula == null);
        }
        String thisString = Clausifier.normalizeVariables(this.theFormula).trim().replaceAll("\\s+", SPACE);
        String argString = Clausifier.normalizeVariables(f.theFormula).trim().replaceAll("\\s+", SPACE);
        return (thisString.equals(argString));
    }

    /** ***************************************************************
     * Test if the contents of the formula are equal to the String argument.
     * Normalize all variables.
     */
    public boolean equals(String s) {

        if (s == null) {
            return false;
        }

        String f = theFormula;
        Formula form = new Formula();
        Formula sform = new Formula();

        form.theFormula = f;
        s = Clausifier.normalizeVariables(s).intern();
        sform.read(s);
        s = sform.toString().trim().intern();

        form.theFormula = Clausifier.normalizeVariables(theFormula);
        f = form.toString().trim().intern();
        return (f.equals(s));
    }

    /** ***************************************************************
     * Tests if this is logically equal with the parameter formula. It
     * employs three equality tests starting with the
     * fastest and finishing with the slowest:
     *
     *  - string comparisons: if the strings of the two formulae are
     *  equal return true as the formulae are also equal,
     *  otherwise try comparing them by more complex means
     *
     *  - compare the predicate structure of the formulae (deepEquals(...)):
     *  this comparison only checks if the two formulae
     *  have an equal structure of predicates disregarding variable
     *  equivalence. Example:
     *     (and (instance ?A Human) (instance ?A Mushroom)) according
     *     to deepEquals(...) would be equal to
     *     (and (instance ?A Human) (instance ?B Mushroom)) even though
     *     the first formula uses only one variable
     *  but the second one uses two, and as such they are not logically
     *  equal. This method generates false positives, but
     *  only true negatives. If the result of the comparison is false,
     *  we return false, otherwise keep trying.
     *
     *  - try to logically unify the formulae by matching the predicates
     *  and the variables
     *
     * @param f
     * @return
     */
    public boolean logicallyEquals(Formula f) {

        boolean equalStrings = this.equals(f);
        if (equalStrings) {
            return true;
        }
        else if (!this.deepEquals(f)) {
            return false;
        }
        else {
            return this.unifyWith(f);
        }
    }

    /** *****************************************************************
     *  Compares this formula with the parameter by trying to compare the
     *  predicate structure of the two and logically
     *  unify their variables. The helper method mapFormulaVariables(....)
     *  returns a logical mapping between the variables
     *  of two formulae of one exists.
     */
    @Deprecated
    public boolean unifyWith(Formula f) {

        if (debug) System.out.println("Formula.unifyWith(): input f : " + f);
        if (debug) System.out.println("Formula.unifyWith(): input this : " + this);
        Formula f1 = Clausifier.clausify(this);
        Formula f2 = Clausifier.clausify(f);

        if (debug) System.out.println("Formula.unifyWith(): after clausify f : " + f2);
        if (debug) System.out.println("Formula.unifyWith(): after clausify  this : " + f1);

        //the normalizeParameterOrder method should be moved to Clausifier
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        Map<FormulaUtil.FormulaMatchMemoMapKey, List<Set<VariableMapping>>> memoMap =
                new HashMap<>();
        List<Set<VariableMapping>> result = mapFormulaVariables(f1, f2, kb, memoMap);

        if (debug) System.out.println("Formula.unifyWith(): variable mapping : " + result);
        return result != null;
    }

    /** *****************************************************************
     * Compares two formulae by recursively traversing its predicate
     * structure and by building possible variable maps
     * between the variables of the two formulae. If a complete mapping
     * is possible, it is returned.
     * Each recursive call returns a list of sets of variable pairs.
     * Each pair is a variable from the first formula and
     * its potential corresponding variable in the second formula. Each
     * set is a potential complete mapping between all
     * the variables in the first formula and the ones in the second. The
     * returned list contains all possible sets, so
     * in essence all possible valid mappings of variables between the two
     * formulas. The method will reconcile the list
     * returned by all one level deeper recursive calls and return the list
     * of sets which offer no contradictions.
     *
     * Note: for clauses with commutative
     *
     * @param memoMap a memo-ization mechanism designed to reduce the number
     *                of recursive calls in "dynamic programming"
     *                fashion
     * @return
     *  null - if the formulas cannot be equals (due to having different predicates for example)
     *  empty list- formulas are equal, but there are no variables to map
     *  list 0f variable mapping sets the list of possible variable mapping sets which will make formulas equal
     *
     */
    public static List<Set<VariableMapping>> mapFormulaVariables(Formula f1, Formula f2, KB kb,
                                     Map<FormulaUtil.FormulaMatchMemoMapKey, List<Set<VariableMapping>>> memoMap) {

        //reading the memo map first
        FormulaUtil.FormulaMatchMemoMapKey key = FormulaUtil.createFormulaMatchMemoMapKey(f1.theFormula, f2.theFormula);
        if (memoMap.containsKey(key)) {
           return memoMap.get(key);
        }

        //not memo hit, carrying on the actual computations
        //null tests first
        if (f1 == null && f2 == null) {
            List<Set<VariableMapping>> result = new ArrayList<>();
            result.add(new HashSet<>());
            return result;
        }
        else if (f1 == null || f2 == null) {
            return null;
        }

        //checking formulas are simple tokens
        if (f1.atom() && f2.atom()) {
            if ((f1.isVariable() && f2.isVariable()) || (Formula.isSkolemTerm(f1.theFormula) && Formula.isSkolemTerm(f2.theFormula))) {
                List<Set<VariableMapping>> result = new ArrayList<>();
                Set<VariableMapping> set = new HashSet<>();
                set.add(new VariableMapping(f1.theFormula, f2.theFormula));
                result.add(set);
                return result;
            }
            else {
                if (f1.theFormula.equals(f2.theFormula)) {
                    List<Set<VariableMapping>> result = new ArrayList<>();
                    result.add(new HashSet<>());
                    return result;
                } else {
                    return null;
                }
            }
        }
        else if (f1.atom() || f2.atom()) {
            return null;
        }

        //if we got here, formulas are lists
        //comparing heads
        Formula head1 = new Formula();
        head1.read(f1.car());
        Formula head2 = new Formula();
        head2.read(f2.car());
        List<Set<VariableMapping>> headMaps = mapFormulaVariables(head1, head2, kb, memoMap);
        memoMap.put(FormulaUtil.createFormulaMatchMemoMapKey(head1.theFormula, head2.theFormula), headMaps);
        if (headMaps == null) {
            //heads don't match; no point of going further
            return null;
        }

        //comparing arguments
        List<String> args1 = f1.complexArgumentsToArrayListString(1);
        List<String> args2 = f2.complexArgumentsToArrayListString(1);
        if (args1.size() != args2.size()) {
            return null;
        }

        if (!Formula.isCommutative(head1.theFormula) && !(kb != null && kb.isInstanceOf(head1.theFormula, "SymmetricRelation"))) {
            //non commutative relation; comparing parameters in order
            List<Set<VariableMapping>> runningMaps = headMaps;
            List<Set<VariableMapping>> parameterMaps;
            Formula parameter1, parameter2;
            for (int i = 0; i < args1.size(); i++) {
                parameter1 = new Formula();
                parameter1.read(args1.get(i));
                parameter2 = new Formula();
                parameter2.read(args2.get(i));
                parameterMaps = mapFormulaVariables(parameter1, parameter2, kb, memoMap);
                memoMap.put(FormulaUtil.createFormulaMatchMemoMapKey(parameter1.theFormula, parameter2.theFormula), parameterMaps);
                runningMaps = VariableMapping.intersect(runningMaps, parameterMaps);
                if (runningMaps == null) {
                    return null;
                }
            }
            return runningMaps;
        }
        else {
            //commutative relation; going through all possible parameter permutations and comparing
            List<Set<VariableMapping>> unionMaps = new ArrayList<>();
            List<int[]> permutations = FormulaUtil.getPermutations(args1.size(),
                    (a,b)-> mapFormulaVariables(new Formula(args1.get(a)), new Formula(args2.get(b)), kb, memoMap) != null);
            List<Set<VariableMapping>> currentMaps, parameterMaps;
            boolean currentPairingValid;
            Formula parameter1, parameter2;
            for (int[] perm:permutations) {
                currentMaps = headMaps;
                currentPairingValid = true;
                for (int i = 0; i < args1.size(); i++) {
                    parameter1 = new Formula();
                    parameter1.read(args1.get(i));
                    parameter2 = new Formula();
                    parameter2.read(args2.get(perm[i]));
                    parameterMaps = mapFormulaVariables(parameter1, parameter2, kb, memoMap);
                    memoMap.put(FormulaUtil.createFormulaMatchMemoMapKey(parameter1.theFormula, parameter2.theFormula), parameterMaps);
                    currentMaps = VariableMapping.intersect(currentMaps, parameterMaps);
                    if (currentMaps == null) {
                        currentPairingValid = false;
                        break;
                    }
                }
                if (currentPairingValid) {
                    unionMaps = VariableMapping.union(unionMaps, currentMaps);
                }
            }
            if (unionMaps.isEmpty()) {
                //keeping the convention of null list when matching is impossible
                unionMaps = null;
            }
            return unionMaps;
        }
    }

    /** ***************************************************************
     * Test if the contents of the formula are equal to the argument.
     */
    public boolean deepEquals(Formula f) {

        if (debug)
            System.out.println("deepEquals(): this: " + this + " arg: " + f);
        //null and simple string equality tests
        if (f == null) {
            return false;
        }
        // if the strings are equal or any of the formula strings are null, there is no point on comparing deep
        boolean stringsEqual = Objects.equals(this.theFormula, f.theFormula);
        if (stringsEqual || (this.theFormula == null || f.theFormula == null)) {
            return stringsEqual;
        }

        Formula f1 = Clausifier.clausify(this);
        Formula f2 = Clausifier.clausify(f);

        if (debug)
            System.out.println("deepEquals(): clausified this: " + f1 + " arg: " + f2);

        //the normalizeParameterOrder method should be moved to Clausifier
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        String normalized1 = Formula.normalizeParameterOrder(f1.theFormula, kb, true);
        String normalized2 = Formula.normalizeParameterOrder(f2.theFormula, kb, true);

        f1 = new Formula(normalized1);
        f2 = new Formula(normalized2);

        if (debug)
            System.out.println("deepEquals(): normalized this: \n" + f1.format("","  ","\n") + "\n arg: \n" + f2.format("","  ","\n"));

        normalized1 = Clausifier.normalizeVariables(f1.theFormula,true); // renumber skolems too
        normalized2 = Clausifier.normalizeVariables(f2.theFormula,true);

        if (debug)
            System.out.println("deepEquals(2): normalized this: \n" + f1.format("","  ","\n") + "\n arg: \n" + f2.format("","  ","\n"));
        //if (debug)
        //    System.out.println("difference: \n" + StringUtils.difference(f1.getFormula(),f2.getFormula()));
        return normalized1.equals(normalized2);
    }

    /** *****************************************************************
     * @param formula
     * @param kb
     * @param varPlaceholders
     * @return
     */
    private static String normalizeParameterOrder(String formula,KB kb, boolean varPlaceholders) {

        //null test first
        if (formula == null) {
            return null;
        }

        //checking formula is a simple tokens
        if (!Formula.listP(formula)) {
            if (varPlaceholders && isVariable(formula)) {
                return "?XYZ";
            }
            else {
                return formula;
            }
        }

        //if we got here, the formulas is a list
        Formula f = new Formula();
        f.read(formula);

        //normalizing parameters
        List<String> args = f.complexArgumentsToArrayListString(1);
        if (args == null || args.isEmpty()) {
            return formula;
        }
        List<String> orderedArgs = new ArrayList<>();
        for (String arg:args) {
            orderedArgs.add(Formula.normalizeParameterOrder(arg, kb, varPlaceholders));
        }

        //sorting arguments if the predicate permits
        String head = f.car();
        if (Formula.isCommutative(head) || (kb != null && kb.isInstanceOf(head, "SymmetricRelation"))) {
            Collections.sort(orderedArgs);
        }

        //building result
        StringBuilder result = new StringBuilder(LP);
        if (varPlaceholders && isSkolemTerm(head)) {
            head = "?SknFn";
        }
        result.append(head);
        result.append(SPACE);
        for (String arg:orderedArgs) {
            result.append(arg);
            result.append(SPACE);
        }
        result.deleteCharAt(result.length() - 1);
        result.append(RP);

        return result.toString();
    }

    /** ***************************************************************
     * Loads all of the arguments into both args and stringArgs
     * data structures.
     */
    private void loadArguments() {

        if (debug) System.out.println("Formula.loadArgument(): formula to load" + this.theFormula);
        args = new ArrayList<>();
        stringArgs = new ArrayList<>();
        String nextarg = "";
        Formula form = this;
        while (form.listP() && !form.empty()) {
            nextarg = form.car();
            stringArgs.add(nextarg);
            args.add(new Formula(nextarg));
            form = new Formula(form.cdr());
        }
        if (debug) System.out.println("Formula.loadArgument(): args loaded: " + args);
    }

    /** ***************************************************************
     * Return the numbered argument of the given formula.  The first
     * element of a formula (i.e. the predicate position) is number 0.
     * Returns the empty string if there is no such argument position.
     */
    public String getStringArgument(int argnum) {

        if (stringArgs == null || args == null || stringArgs.isEmpty() || args.isEmpty()) {
            loadArguments();
        }
        if (stringArgs.size() > argnum)
            return stringArgs.get(argnum);
        return "";
    }

    /** ***************************************************************
     * Return the numbered argument of the given formula.  The first
     * element of a formula (i.e. the predicate position) is number 0.
     * Returns null if there is no such argument position.
     */
    public Formula getArgument(int argnum) {

        if (stringArgs == null || args == null || stringArgs.isEmpty() || args.isEmpty()) {
            loadArguments();
        }
        if (args.size() > argnum)
            return args.get(argnum);
        return null;
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
        if (this.listP()) {
            ans = 0;
            while (!StringUtil.emptyString(this.getStringArgument(ans)))
                ++ans;
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
    public List<String> argumentsToArrayListString(int start) {

        //if (start > 1)
        //    System.out.println("Error in Formula.argumentsToArrayList() start greater than 1 : " + start);
        if (debug) System.out.println("Formula.argumentsToArrayListString(): formula: " + getFormula());
        if (debug) System.out.println("Formula.argumentsToArrayListString(): stringArgs: " + stringArgs);
        List<String> result = new ArrayList<>();
        if (stringArgs != null && !stringArgs.isEmpty()) {
            if (start == 0)
                return stringArgs;
            if (start > stringArgs.size()) {
                System.err.println("Error in Formula.argumentsToArrayListString() start " +
                        start + " greater than end : " + stringArgs.size() + " in formula " + getFormula());
                return result;
            }
            result.addAll(stringArgs.subList(start, stringArgs.size()));
            return result;
        }
        if (theFormula.indexOf('(',1) != -1) {
            List<String> erList = complexArgumentsToArrayListString(0);
            for (String s : erList) {
                if (s.indexOf('(') != -1 && !StringUtil.quoted(s)) {
                    //String err = "Error in Formula.argumentsToArrayListString() complex formula: " + this.toString();
                    //errors.add(err);
                    //System.out.println(err);
                    return null;
                }
            }
        }
        int index = start;

        String arg = getStringArgument(index);
        while (arg != null && !"".equals(arg) && arg.length() > 0) {
            result.add(arg.intern());
            index++;
            arg = getStringArgument(index);
        }
        if (index == start)
            return null;
        return result;
    }

    /** ***************************************************************
     * Return all the arguments in a simple formula as a list, starting
     * at the given argument.  If formula is complex (i.e. an argument
     * is a function or sentence), then return null.  If the starting
     * argument is greater than the number of arguments, also return
     * null.
     *
     * Deprecated - use complexArgumentsToArrayList()
     */
    @Deprecated
    public List<Formula> argumentsToArrayList(int start) {

        List<Formula> result = new ArrayList<>();
        System.err.println("Error not implemented Formula.argumentsToArrayList()");
        return result;
    }

    /** ***************************************************************
     * Return all the arguments in a formula as a list, starting
     * at the given argument.  If the starting
     * argument is greater than the number of arguments, return null.
     */
    public List<Formula> complexArgumentsToArrayList(int start) {

        int index = start;
        List<Formula> result = new ArrayList<>();
        Formula arg = getArgument(index);
        while (arg != null && !arg.empty() && index < 20) {
            result.add(arg);
            index++;
            arg = getArgument(index);
            if (debug) System.out.println("Formula.complexArgumentsToArrayList(): arg: " + arg);
            if (debug && arg != null)
                System.out.println("Formula.complexArgumentsToArrayList(): empty?: " + arg.empty());
        }
        if (index == start)
            return null;
        return result;
    }

    /** ***************************************************************
     * Return all the arguments in a formula as a list, starting
     * at the given argument.  If the starting
     * argument is greater than the number of arguments, return null.
     */
    public List<String> complexArgumentsToArrayListString(int start) {

        int index = start;
        List<String> result = new ArrayList<>();
        String arg = getStringArgument(index);
        while (!StringUtil.emptyString(arg)) {
            result.add(arg);
            index++;
            arg = getStringArgument(index);
        }
        if (index == start)
            return null;
        return result;
    }

    /** ***************************************************************
     * Translate SUMO inequalities to the typical inequality symbols that
     * some theorem provers require.
     */
    private static String translateInequalities(String s) {

        if (s.equalsIgnoreCase(GT)) return ">";
        if (s.equalsIgnoreCase(GTET)) return ">=";
        if (s.equalsIgnoreCase(LT)) return "<";
        if (s.equalsIgnoreCase(LTET)) return "<=";
        return "";
    }

    /** ***************************************************************
     * Collects all variables in this Formula.  Returns an ArrayList
     * containing a pair of ArrayLists.  The first contains all
     * explicitly quantified variables in the Formula.  The second
     * contains all variables in Formula that are not within the scope
     * of some explicit quantifier.
     *
     * @return An ArrayList containing two ArrayLists, each of which
     * could be empty
     */
    public List<Set<String>> collectVariables() {

        if (!allVarsPairCache.isEmpty() && KBmanager.initialized)
            return allVarsPairCache;
        List<Set<String>> ans = new ArrayList<>();
        ans.add(new HashSet());
        ans.add(new HashSet());
        allVarsPairCache.add(new HashSet());
        allVarsPairCache.add(new HashSet());
    	Set<String> quantified = new HashSet<>();
    	Set<String> unquantified = new HashSet<>();
        unquantified.addAll(collectAllVariables());
        quantified.addAll(collectQuantifiedVariables());
        unquantified.removeAll(quantified);
        ans.get(0).addAll(quantified);
        ans.get(1).addAll(unquantified);
        allVarsPairCache.get(0).addAll(quantified);
        allVarsPairCache.get(1).addAll(unquantified);
        return ans;
    }

    /** ***************************************************************
     * Collect quantified and unquantified variables recursively
     */
    public void collectQuantifiedUnquantifiedVariablesRecurse(Formula f, Map<String, Boolean> varFlag,
                  Set<String> unquantifiedVariables, Set<String> quantifiedVariables) {

        if (f == null || StringUtil.emptyString(f.theFormula) || f.empty())
            return;

        String carstr = f.car();
        if (Formula.atom(carstr) && Formula.isLogicalOperator(carstr)) {
            if (isQuantifier(carstr)) {
                if (true) System.out.println("Formula.collectQuantifiedUnquantifiedVariablesRecurse(): carstr" + carstr);
                String varString = f.getStringArgument(1);
                String[] varArray = (varString.substring(1, varString.length()-1)).split(SPACE);
                quantifiedVariables.addAll(Arrays.asList(varArray));

                for (int i = 2 ; i < f.listLength(); i++) {
                    collectQuantifiedUnquantifiedVariablesRecurse(new Formula(f.getArgument(i)),
                            varFlag, unquantifiedVariables, quantifiedVariables);
                }
            }
            else {
                for (int i = 1; i < f.listLength(); i++) {
                    collectQuantifiedUnquantifiedVariablesRecurse(new Formula(f.getArgument(i)),
                            varFlag, unquantifiedVariables, quantifiedVariables);
                }
            }

        }
        else {
            String arg;
            for (int i = 0; i < f.listLength(); i++) {
                arg = f.getStringArgument(i);
                if (arg.startsWith(V_PREF) || arg.startsWith(R_PREF)) {
                    if (!varFlag.containsKey(arg) && !quantifiedVariables.contains(arg)) {
                        unquantifiedVariables.add(arg);
                        varFlag.put(arg, false);
                    }
                }
                else {
                    collectQuantifiedUnquantifiedVariablesRecurse(new Formula(arg),
                            varFlag, unquantifiedVariables, quantifiedVariables);
                }
            }
        }
    }

    /** ***************************************************************
     * Collects all String terms from one Collection and adds them
     * to another, without duplication
     */
    public void addAllNoDup (Collection<String> thisCol, Collection<String> arg) {

        for (String s : arg)
            if (!thisCol.contains(s))
                thisCol.add(s);
    }

    /** ***************************************************************
     * Collects all variables in this Formula.  Returns an ArrayList
     * of String variable names (with initial '?').
     *
     * @return An ArrayList of String variable names

    public List<String> collectOrderedVariables() {

        List<String> result = new ArrayList<>();
        if (listLength() < 1)
            return result;
        Formula fcar = new Formula();
        fcar.read(this.car());
        if (fcar.isVariable() && !result.contains(fcar.theFormula))
            result.add(fcar.theFormula);
        else {
            if (fcar.listP())
                addAllNoDup(result,fcar.collectAllVariables());
        }
        Formula fcdr = new Formula();
        fcdr.read(this.cdr());
        if (fcdr.isVariable() && !result.contains(fcar.theFormula))
            result.add(fcdr.theFormula);
        else {
            if (fcdr.listP())
                addAllNoDup(result,fcdr.collectAllVariables());
        }
        return result;
    }
*/
    /** ***************************************************************
     * Collects all variables in this Formula.  Returns an Set
     * of String variable names (with initial '?').
     *
     * @return A Set of String variable names
     */
    public Set<String> collectAllVariables() {

        if (!allVarsCache.isEmpty())
            return allVarsCache;

    	//ArrayList<String> result = new ArrayList<String>();
    	Set<String> resultSet = new HashSet<>();

    	if (listLength() < 1)
    	    return resultSet;
    	Formula fcar = new Formula();
    	fcar.read(this.car());
    	if (fcar.isVariable())
            resultSet.add(fcar.theFormula);
    	else {
            if (fcar.listP())
                resultSet.addAll(fcar.collectAllVariables());
    	}
    	Formula fcdr = new Formula();
    	fcdr.read(this.cdr());
    	if (fcdr.isVariable())
            resultSet.add(fcdr.theFormula);
    	else {
            if (fcdr.listP())
    		resultSet.addAll(fcdr.collectAllVariables());
    	}
    	//result.addAll(resultSet);
        allVarsCache.addAll(resultSet);
    	return resultSet;
    }

    /** ***************************************************************
     * Collects all variables in this Formula in lexical order.  Returns an ArrayList
     * of String variable names (with initial '?'). Note that unlike
     * collectAllVariables() this is not cached and therefore potentially much
     * slower, although it does fill the cache of variable names so using
     * this method first, will make calls to collectAllVariables() fast
     * if that method is called on the same formula after this method is called.
     *
     * @return An ArrayList of String variable names
     */
    public List<String> collectAllVariablesOrdered() {

        List<String> result = new ArrayList<>();
        if (listLength() < 1)
            return result;
        Formula fcar = new Formula();
        fcar.read(this.car());
        if (fcar.isVariable())
            result.add(fcar.theFormula);
        else {
            if (fcar.listP())
                result.addAll(fcar.collectAllVariablesOrdered());
        }
        Formula fcdr = new Formula();
        fcdr.read(this.cdr());
        if (fcdr.isVariable())
            result.add(fcdr.theFormula);
        else {
            if (fcdr.listP())
                result.addAll(fcdr.collectAllVariablesOrdered());
        }
        allVarsCache.addAll(result);
        return result;
    }

    /** ***************************************************************
     * Collects all quantified variables in this Formula.  Returns an ArrayList
     * of String variable names (with initial '?').  Note that
     * duplicates are not removed.
     *
     * @return An ArrayList of String variable names

    public List<String> collectExistentiallyQuantifiedVariables() {

    	List<String> result = new ArrayList<String>();
    	Set<String> resultSet = new HashSet<String>();
    	if (listLength() < 1)
    		return result;
    	Formula fcar = new Formula();
    	fcar.read(this.car());
    	if (fcar.theFormula.equals(EQUANT)) {
        	Formula remainder = new Formula();
        	remainder.read(this.cdr());
        	if (!remainder.listP()) {
        		System.out.println("Error in Formula.collectQuantifiedVariables(): incorrect quantification: " + this.toString());
        		return result;
        	}
        	Formula varlist = new Formula();
        	varlist.read(remainder.car());
        	resultSet.addAll(varlist.collectAllVariables());
    		resultSet.addAll(remainder.cdrAsFormula().collectExistentiallyQuantifiedVariables());
    	}
    	else {
    		if (fcar.listP())
    			resultSet.addAll(fcar.collectExistentiallyQuantifiedVariables());
    		resultSet.addAll(this.cdrAsFormula().collectExistentiallyQuantifiedVariables());
    	}
    	result.addAll(resultSet);
    	return result;
    }
*/
    /** ***************************************************************
     * Collects all quantified variables in this Formula.  Returns an ArrayList
     * of String variable names (with initial '?').  Note that
     * duplicates are not removed.
     *
     * @return An ArrayList of String variable names
     */
    public Set<String> collectQuantifiedVariables() {

        if (!quantVarsCache.isEmpty())
            return quantVarsCache;
    	Set<String> resultSet = new HashSet<>();
    	if (empty())
    	    return resultSet;
    	Formula fcar = new Formula();
    	fcar.read(this.car());
        if (fcar.empty())
            return resultSet;
        if (debug) System.out.println("Formula.collectQuantifiedVariables(): car: " + fcar);
    	// if (isQuantifier(fcar.theFormula)) {
        if (fcar.theFormula.equals(UQUANT) || fcar.theFormula.equals(EQUANT) || fcar.theFormula.equals(KAPPAFN)) {
            Formula remainder = new Formula();
            remainder.read(this.cdr());
            if (debug) System.out.println("Formula.collectQuantifiedVariables(): remainder \n" + remainder.toString());
            if (!remainder.listP()) {
                System.err.println("Error in Formula.collectQuantifiedVariables(): incorrect quantification: " + this.toString());
                return resultSet;
            }
            if (fcar.theFormula.equals(KAPPAFN)) {
                String bindingVar = remainder.car();
                if (bindingVar.startsWith("?")) {
                    resultSet.add(bindingVar);
                }
            }
        else {
            Formula varlist = new Formula();
            varlist.read(remainder.car());
            resultSet.addAll(varlist.collectAllVariables());
            resultSet.addAll(remainder.cdrAsFormula().collectQuantifiedVariables());
        }
            Formula varlist = new Formula();
            varlist.read(remainder.car());
            resultSet.addAll(varlist.collectAllVariables());
            resultSet.addAll(remainder.cdrAsFormula().collectQuantifiedVariables());
    	}
    	else {
            if (fcar.listP())
                    resultSet.addAll(fcar.collectQuantifiedVariables());
            resultSet.addAll(this.cdrAsFormula().collectQuantifiedVariables());
    	}
        quantVarsCache.addAll(resultSet);
        if (debug) System.out.println("Formula.collectQuantifiedVariables(): " + "resultSet \n" + resultSet);
            
    	return resultSet;
    }

    /** ***************************************************************
     * Collect all the unquantified variables in a formula
     */
    public Set<String> collectUnquantifiedVariables() {
        return collectVariables().get(1);
    }

    /** ***************************************************************
     * Collect all the terms in a formula
     */
    public Set<String> collectTerms() {

        Set<String> resultSet = new HashSet<>();

        if (this.theFormula == null || "".equals(this.theFormula)) {
			System.out.println("Error in Formula.collectTerms(): " +
					"No formula to collect terms from: " + this);
            return null;
        }
        if (this.empty())
            return resultSet;
        if (this.atom()) {
            termCache.add(theFormula);
            resultSet.add(theFormula);
        }
        else {
            if (!termCache.isEmpty())
                return termCache;
            Formula f = new Formula();
            f.read(theFormula);
            Formula f2;
            while (!f.empty() && f.theFormula != null && !"".equals(f.theFormula)) {
                f2 = new Formula();
                f2.read(f.car());
                resultSet.addAll(f2.collectTerms());
                f.read(f.cdr());
            }
        }
        //List<String> result = new ArrayList(resultSet);
        termCache.addAll(resultSet);
        return resultSet;
    }

    /** ***************************************************************
     *  Replace variables with a value as given by the map argument
     */
    public Formula substituteVariables(Map<String,String> m) {

        Formula newFormula = new Formula();
        newFormula.read("()");
        if (atom()) {
            if (m.keySet().contains(theFormula)) {
                theFormula = (String) m.get(theFormula);
                if (this.listP())
                    theFormula = LP + theFormula + RP;
            }
            return this;
        }
        if (!empty()) {
            Formula f1 = new Formula();
            f1.read(this.car());
            if (f1.listP())
                newFormula = newFormula.cons(f1.substituteVariables(m));
            else
                newFormula = newFormula.append(f1.substituteVariables(m));
            Formula f2 = new Formula();
            f2.read(this.cdr());
            newFormula = newFormula.append(f2.substituteVariables(m));
        }
        return newFormula;
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
        String arg0 = this.car();
        List<Set<String>> vpair = collectVariables();
        Set<String> quantVariables = vpair.get(0);
        Set<String> unquantVariables = vpair.get(1);

        if (!unquantVariables.isEmpty()) {   // Quantify all the unquantified variables
            StringBuilder sb = new StringBuilder();
            sb.append((query ? "(exists (" : "(forall ("));
            boolean afterTheFirst = false;
            Iterator<String> itu = unquantVariables.iterator();
            while (itu.hasNext()) {
                if (afterTheFirst) sb.append(SPACE);
                sb.append(itu.next());
                afterTheFirst = true;
            }
            sb.append(") ");
            sb.append(this.theFormula);
            sb.append(RP);
            result = sb.toString();
        }
        return result;
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
    public Formula renameVariableArityRelations(KB kb, Map<String,String> relationMap) {

        Formula result = this;
        if (result.listP()) {
            StringBuilder sb = new StringBuilder();
            Formula f = new Formula();
            f.read(this.theFormula);
            int flen = f.listLength();
            String suffix = ("__" + (flen - 1));
            String arg, func;
            Formula argF;
            sb.append(LP);
            for (int i = 0 ; i < flen ; i++) {
                arg = f.getStringArgument(i);
                if (i > 0)
                    sb.append(SPACE);
                func = "";
                if (kb.kbCache.isInstanceOf(arg,"Function"))
                    func = FN_SUFF;
                if ((i == 0) && kb.kbCache.transInstOf(arg,"VariableArityRelation") && !arg.endsWith(suffix + func)) {
                    relationMap.put(arg + suffix + func, arg);
                    arg += suffix + func;
                }
                else if (listP(arg)) {
                    argF = new Formula();
                    argF.read(arg);
                    arg = argF.renameVariableArityRelations(kb,relationMap).theFormula;
                }
                sb.append(arg);
            }
            sb.append(RP);
            f = new Formula();
            f.read(sb.toString());
            result = f;
        }
        return result;
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
    public Map<String, List> gatherRelationsWithArgTypes(KB kb) {

        Map<String, List> argtypemap = new HashMap<>();
        Set<String> relations = gatherRelationConstants();
        int atlen;
        List argtypes;
        for (String r : relations) {
            atlen = (Formula.MAX_PREDICATE_ARITY + 1);
            argtypes = new ArrayList();
            for (int i = 0; i < atlen; i++)
                argtypes.add(kb.getArgType(r, i));
            argtypemap.put(r, argtypes);
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
    public Set<String> gatherRelationConstants() {

        Set<String> relations = new HashSet<>();
        Set<String> accumulator = new HashSet<>();
        if (this.listP() && !this.empty())
            accumulator.add(this.theFormula);
        List<String> kifLists = new ArrayList<>();
        Formula f;
        String klist, arg;
        while (!accumulator.isEmpty()) {
            kifLists.clear();
            kifLists.addAll(accumulator);
            accumulator.clear();
            for (Iterator<String> it = kifLists.iterator(); it.hasNext();) {
                klist = it.next();
                if (listP(klist)) {
                    f = new Formula();
                    f.read(klist);
                    for (int i = 0; !f.empty(); i++) {
                        arg = f.car();
                        if (listP(arg)) {
                            if (!empty(arg)) accumulator.add(arg);
                        }
                        else if (isQuantifier(arg)) {
                            accumulator.add(f.getStringArgument(2));
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
        return relations;
    }

    /** ***************************************************************
     * Test whether a Formula is a functional term.  Note this assumes
     * the textual convention of all functions ending with "Fn".
     */
    @Deprecated
    public boolean isFunctionalTerm() {

        System.err.println("Error in Formula.isFunctionalTerm(): must use KB.isFunction() instead");
        Thread.dumpStack();
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
    @Deprecated
    public static boolean isFunctionalTerm(String s) {

        Formula f = new Formula();
        f.read(s);
        return f.isFunctionalTerm();
    }

    /** ***************************************************************
     * Test whether a Formula contains a Formula as an argument to
     * other than a logical operator.
     * TODO: get var types in case there is a function variable, and
     * copy that var type list down to the arguments
     */
    public boolean isHigherOrder(KB kb) {

        if (debug) System.out.println("Formula.isHigherOrder(): " + this);
        if (varTypeCache == null || varTypeCache.keySet().isEmpty()) {
            FormulaPreprocessor fp = new FormulaPreprocessor();
            varTypeCache = fp.findAllTypeRestrictions(this,kb);
        }
        if (!KBmanager.initialized)
            return false;
        //if (higherOrder) {
        //    if (debug) if (debug) System.out.println("Formula.isHigherOrder(): cached as higher order: " + this);
        //    return true;
        //}
        if (this.listP()) {
            String pred = this.car();
            if (debug) System.out.println("Formula.isHigherOrder(): pred: " + pred);
            List<String> sig = kb.kbCache.getSignature(pred);
            if (sig != null && !Formula.isVariable(pred) && sig.contains("Formula"))
                return true;
            boolean logop = isLogicalOperator(pred);
            if (debug) System.out.println("Formula.isHigherOrder(): logop: " + logop);
            List<String> al = literalToArrayList();
            Formula f;
            for (String arg : al) {
                f = new Formula();
                f.read(arg);
                f.varTypeCache = this.varTypeCache;
                if (debug) System.out.println("Formula.isHigherOrder(): varTypeCache: " + varTypeCache);
                if (debug) System.out.println("Formula.isHigherOrder(): arg: " + arg);
                if (debug) System.out.println("Formula.isHigherOrder(): atom: " + atom(arg));
                if (debug) System.out.println("Formula.isHigherOrder(): isFunctional: " + kb.isFunctional(f));
                if (!atom(arg) && !kb.isFunctional(f)) {
                    if (logop) {
                        if (f.isHigherOrder(kb)) {
                            higherOrder = true;
                            return true;
                        }
                    }
                    else {
                        higherOrder = true;
                        return true;
                    }
                }
                else
                    if (f.isHigherOrder(kb)) {
                        higherOrder = true;
                        return true;
                    }
            }
        }
        return false;
    }

    /** ***************************************************************
     * Test whether a String formula is a variable
     */
    public static boolean isVariable(String term) {

        return (!StringUtil.emptyString(term)
                && (term.startsWith(V_PREF)
                    || term.startsWith(R_PREF)));
    }

    /** ***************************************************************
     * Test whether the Formula is a variable
     */
    public boolean isVariable() {
        return isVariable(theFormula);
    }

    /** ***************************************************************
     * Test whether the Formula is a regular '?' variable
     */
    public boolean isRegularVariable() {
        return !empty() && getFormula().startsWith(V_PREF);
    }

    /** ***************************************************************
     * Test whether the Formula is a row variable
     */
    public boolean isRowVar() {
        return !empty() && getFormula().startsWith(R_PREF);
    }

    /** ***************************************************************
     * Test whether the Formula is automatically created by caching
     */
    public boolean isCached() {
        return sourceFile != null && KButilities.isCacheFile(sourceFile);
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
        if (this.listP()) {
            String arg0 = this.car();
            if (isQuantifier(arg0)) {
                String arg2 = this.getStringArgument(2);
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
        return ans;
    }

    /** ***************************************************************
     * Returns true only if this Formula is a horn clause or is simply
     * modified to be horn by breaking out a conjunctive conclusion.
     */
    public boolean isHorn(KB kb) {

        if (!isRule()) {
            System.out.println("Error in Formula.isHorn(): Formula is not a rule: " + this);
            return false;
        }
        if (isHigherOrder(kb))
            return false;
        if (theFormula.contains(Formula.EQUANT) || theFormula.contains(Formula.UQUANT))
            return false;

        Formula antecedent = cdrAsFormula().carAsFormula();
        if (!antecedent.isSimpleClause(kb) && !antecedent.car().equals(Formula.AND))
            return false;
        Formula consequent = cdrAsFormula().cdrAsFormula().carAsFormula();
        return !(!consequent.isSimpleClause(kb) && !consequent.car().equals(Formula.AND));
    }

    /** ***************************************************************
     * Test whether a list with a predicate is a quantifier list
     */
    public static boolean isQuantifierList(String listPred, String previousPred) {

        return ((previousPred.equals(EQUANT) || previousPred.equals(UQUANT)) &&
                (listPred.startsWith(R_PREF) || listPred.startsWith(V_PREF)));
    }

    /** ***************************************************************
     * Test whether a Formula is a simple list of terms (including
     * functional terms).
     *
     * @param kb the current knowledge base
     * @return true if a Formula is a simple list of terms (including
     * functional terms)
     */
    public boolean isSimpleClause(KB kb) {

        if (!listP(this.theFormula))
        	return false;
        if (!atom(this.car()))
        	return false;
        String arg;
        int argnum = 1;
        Formula f;
        do {
            arg = this.getStringArgument(argnum);
            argnum++;
            if (listP(arg)) {
            	f = new Formula(arg);
                if (kb != null && !kb.isFunction(f.car()))
                    return false;
                if (kb == null && !f.car().endsWith(FN_SUFF)) // in case just testing without a kb
                    return false;
            }
        } while (!StringUtil.emptyString(arg));
        return true;
    }

    /** ***************************************************************
     * Test whether a Formula is a simple clause wrapped in a
     * negation.
     */
    public boolean isSimpleNegatedClause(KB kb) {

        if (!listP(this.theFormula))
        	return false;
        Formula f = new Formula();
        f.read(theFormula);
        if (f.car().equals(NOT)) {
            f.read(f.cdr());
            if (empty(f.cdr())) {
                f.read(f.car());
                return f.isSimpleClause(kb);
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

        return (!StringUtil.emptyString(pred) 
            && (pred.equals(EQUANT) 
            || pred.equals(UQUANT) 
            || pred.equals(KAPPAFN)));
    }

    /** *****************************************************************
     * Tests if this formula is an existentially quantified formula
     *
     * @return
     */
    public boolean isExistentiallyQuantified() {

        return EQUANT.equals(this.car());
    }

    /** *****************************************************************
     * Tests if this formula is an universally quantified formula
     *
     * @return
     */
    public boolean isUniversallyQuantified() {

        return UQUANT.equals(this.car());
    }

    /** ***************************************************************
     * A static utility method.
     * @param obj Any object, but should be a String.
     * @return true if obj is a SUO-KIF commutative logical operator,
     * else false.
     */
    public static boolean isCommutative(String obj) {

        return (!StringUtil.emptyString(obj)
                && (obj.equals(AND) || obj.equals(EQUAL)
                    || obj.equals(OR)));
    }

    /** *****************************************************************
     * @return a String with the type(s) of the formula
     */
    public String findType(KB kb) {

        StringBuilder sb = new StringBuilder();
        if (this.isBinary()) sb.append ("binary, ");
        if (this.isExistentiallyQuantified()) sb.append ("existential, ");
        if (this.isFunctionalTerm()) sb.append ("functional, ");
        if (this.isHorn(kb)) sb.append ("horn, ");
        if (this.isRule()) sb.append ("rule, ");
        if (this.isSimpleClause(kb)) sb.append ("simple clause, ");
        if (this.isSimpleNegatedClause(kb)) sb.append ("simple negated clause, ");
        if (this.isUniversallyQuantified()) sb.append ("universal, ");
        if (this.isVariable()) sb.append ("variable, ");
        if (this.isHigherOrder(kb)) sb.append ("hol, ");
        return sb.toString();
    }

    /** ***************************************************************
     * Returns the dual logical operator of op, or null if op is not
     * an operator or has no dual.
     *
     * @param op A String, assumed to be a SUO-KIF logical operator
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
            for (String[] dual : duals) {
                if (op.equals(dual[0])) {
                    ans = dual[1];
                }
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
    public static boolean isTrueFalse(String term) {

        return (!term.isEmpty() && (term.equals("true") || term.equals("false")));
    }

    /** ***************************************************************
     * Returns true if term is a constant true or constant false, else
     * returns false.
     *
     * @param term A String, assumed to be an atomic SUO-KIF term.
     */
    public static boolean isLogicalOperator(String term) {

        return (!StringUtil.emptyString(term) && LOGICAL_OPERATORS.contains(term));
    }

    /** ***************************************************************
     * Returns true if term is a valid SUO-KIF term, else
     * returns false.
     *
     * @param term A String, assumed to be an atomic SUO-KIF term.
     */
    public static boolean isTerm(String term) {

        if (!StringUtil.emptyString(term) && !listP(term) &&
                Character.isJavaIdentifierStart(term.charAt(0))) {
            for (int i = 0; i < term.length(); i++) {
                if (!Character.isJavaIdentifierPart(term.charAt(i)) && term.charAt(i) != '-')
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

        return (!StringUtil.emptyString(term) && COMPARISON_OPERATORS.contains(term));
    }

    /** ***************************************************************
     * Returns true if term is a SUO-KIF inequality, else returns false.
     *
     * @param term A String.
     */
    public static boolean isInequality(String term) {

        return (!StringUtil.emptyString(term) && INEQUALITIES.contains(term));
    }

    /** ***************************************************************
     * Returns true if term is a SUO-KIF mathematical function, else
     * returns false.
     * @param term A String.
     */
    public static boolean isMathFunction(String term) {

        return (!StringUtil.emptyString(term) && MATH_FUNCTIONS.contains(term));
    }

    /** ***************************************************************
     */
    public boolean isModal(KB kb) {

        return (this.isHigherOrder(kb) && this.getFormula().contains("modalAttribute"));
    }

    /** ***************************************************************
     */
    public boolean isEpistemic(KB kb) {

        return (this.isHigherOrder(kb) &&
                (this.getFormula().contains("knows") || this.getFormula().contains("believes")));
    }

    /** ***************************************************************
     */
    public boolean isTemporal(KB kb) {

        return (this.isHigherOrder(kb) && this.getFormula().contains("holdsDuring"));
    }

    /** *****************************************************************
        The purpose is to take simple HOL formula with temporal aspects
        and remove temporal aspects of it in an attempt to take it to FOL.

        Note: This may change the semantic meaning of the formal logic.
     */
    public static String removeTemporalRelations(String p_f, KB kb) {
        Formula f = new Formula(p_f);
        String newFormula = "";
        String nextCar = f.car();

        String nextCdr, subFormula, formulaArg;
        Formula subF;
        while (nextCar != null && !nextCar.equals("")) {
            nextCdr = f.cdr();
            if (kb.isChildOf(nextCar, "TemporalRelation")) {
                return "";
            }
            else if(nextCar.matches("^\\s*\\(\\s*and.*")) {
                subFormula = removeTemporalRelations(nextCar, kb);
                subF = new Formula(LP+subFormula+RP);
                if (subF.cddr() == null || subF.cddr().isEmpty() || subF.cddr().equals("()")) {
                    subFormula = subFormula.replaceFirst("^\\s*and\\s+", "");
                }
                else {
                    subFormula = LP + subFormula + RP;
                }
                return newFormula + SPACE + subFormula;
            }
            else if (nextCar.startsWith(LP)) {
                subFormula = removeTemporalRelations(f.car(), kb);
                if (!subFormula.equals("")) {
                    newFormula += LP + subFormula + RP;
                    if (newFormula.endsWith(" )")) {
                        newFormula = newFormula.substring(0, newFormula.length() - 2) + RP;
                    }
                }
            }
            else if(nextCar.equals("holdsDuring")) {
                formulaArg = f.cddr();
                if (formulaArg != null && formulaArg.length() >= 2 && formulaArg.startsWith(LP) && formulaArg.endsWith(RP)) {
                    return removeTemporalRelations(formulaArg.substring(1, formulaArg.length()-1), kb);
                }
                return removeTemporalRelations(formulaArg, kb);
            }
            else {
                newFormula += nextCar + SPACE + removeTemporalRelations(nextCar, kb);
            }
            f = new Formula(nextCdr);
            nextCar = f.car();
        }
        return newFormula;
    }

    /** ***************************************************************
     */
    public boolean isOtherHOL(KB kb) {

        return (this.isHigherOrder(kb) && !this.isTemporal(kb) &&
                !this.isEpistemic(kb) && !this.isModal(kb));
    }

    /** ***************************************************************
     * Returns true if formula is a valid formula with no variables,
     * else returns false.
     */
    public static boolean isGround(String form) {

        if (StringUtil.emptyString(form))
            return false;
        if (!form.contains("\""))
            return (!form.contains(V_PREF) && !form.contains(R_PREF));
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
     * Returns true if formula does not have variables, else returns false.
     */
    public boolean isGround() {
        return isGround(theFormula);
    }

    /** ***************************************************************
     * Returns true if formula is a simple binary relation (note
     * that because the argument list includes the predicate, which is
     * argument 0, there will be three elements)
     */
    public boolean isBinary() {

        List<String> l = argumentsToArrayListString(0);
        if (l == null)
            return false;
        return complexArgumentsToArrayList(0).size() == 3;
    }

    /** ***************************************************************
     * Returns true if term is a SUO-KIF function, else returns false.
     * Note that this test is purely syntactic, and could fail for
     * functions that do not adhere to the convention of ending all
     * functions with "Fn".
     * @param term A String.
     */
    @Deprecated
    public static boolean isFunction(String term) {

        System.err.println("Error in Formula.isFuction(): must use KB.isFunction() instead");
        return (!StringUtil.emptyString(term) && (term.endsWith(FN_SUFF)));
    }

    /** ***************************************************************
     * Returns true if term is a SUO-KIF Skolem term, else returns false
     * @param term A String.
     * @return true or false
     */
    public static boolean isSkolemTerm(String term) {
        return (!StringUtil.emptyString(term)
                && term.trim().matches("^.?" + SK_PREF + "\\S*\\s*\\d+"));
    }

    /** ***************************************************************
     * @return An ArrayList (ordered tuple) representation of the
     * Formula, in which each top-level element of the Formula is
     * either an atom (String) or another list.
     */
    public List<String> literalToArrayList() {

        List<String> tuple = new ArrayList<>();
        Formula f = this;
        if (f.listP()) {
            while (!f.empty()) {
                tuple.add(f.car());
                f = f.cdrAsFormula();
            }
        }
        return tuple;
    }

    /** ***************************************************************
     *  Replace v with term.
     *  TODO: See if a regex replace is faster (commented out buggy code below)
     */
    public Formula replaceVar(String v, String term) {

        //Pattern p = Pattern.compile("\\" + var + "([^a-zA-Z0-9])");
        //Matcher m = p.matcher(input.theFormula);
        //String fstr = m.replaceAll(rel + "$1");

    	//System.out.println("INFO in Formula.replaceVar(): formula: " + theFormula);
    	if (StringUtil.emptyString(this.theFormula) || this.empty())
    		return this;
        Formula newFormula = new Formula();
        newFormula.read("()");
        if (this.isVariable()) {
            if (theFormula.equals(v))
                theFormula = term;
            return this;
        }
        if (this.atom())
        	return this;
        if (!this.empty()) {
            Formula f1 = new Formula();
            f1.read(this.car());
            //System.out.println("INFO in Formula.replaceVar(): car: " + f1.theFormula);
            if (f1.listP())
                newFormula = newFormula.cons(f1.replaceVar(v,term));
            else
                newFormula = newFormula.append(f1.replaceVar(v,term));
            Formula f2 = new Formula();
            f2.read(this.cdr());
			//System.out.println("INFO in Formula.replaceVar(): cdr: " + f2);
            newFormula = newFormula.append(f2.replaceVar(v,term));
        }
        return newFormula;
    }

    /** *****************************************************************
     * @param quantifier
     * @param vars
     * @return
     * @throws Exception
     */
    public Formula replaceQuantifierVars(String quantifier, List<String> vars) throws Exception {

        if (!quantifier.equals(this.car())) {
            throw new Exception("The formula is not properly quantified: " + this);
        }

        Formula param = new Formula();
        param.read(this.cadr());
        List<String> existVars = param.complexArgumentsToArrayListString(0);

        if (existVars.size() != vars.size()) {
            throw new Exception("Wrong number of variables: " + vars + " to substitute in existentially quantified formula: " + this);
        }

        Formula result = this;
        for (int i = 0; i < existVars.size(); i++) {
            result = result.replaceVar(existVars.get(i), vars.get(i));
        }

        return result;
    }

    /** ***************************************************************
     * Compare the given formula to the query and return whether
     * they are the same.
     */
    public static boolean isQuery(String query, String formula) {

        boolean result;

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
        String fstr = formulaString.trim();
        if (fstr.startsWith("(not")) {
            Formula f = new Formula();
            f.read(fstr);
            result = query.equals(f.getArgument(1));
        }
        return result;
    }

    /** ***************************************************************
     * Remove the 'holds' prefix wherever it appears.
     */
    public static String postProcess(String s) {

        if (StringUtil.emptyString(s))
            return s;
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

        if (debug) System.out.println("Formula.format(): "  + this.theFormula);
        if (this.theFormula == null)
            return "";
        if (!StringUtil.emptyString(this.theFormula))
            this.theFormula = this.theFormula.trim();
        if (atom())
            return theFormula;
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
        char ch;         // char at i
        for (int i = 0; i < flen; i++) {
			// logger.finest("formatted string = " + formatted.toString());
            ch = this.theFormula.charAt(i);
            if (inComment) {     // In a comment
                formatted.append(ch);
                if ((i > 70) && (ch == '/')) // add spaces to long URL strings
                    formatted.append(SPACE);
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
                    token = new StringBuilder();
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
                if ((token.indexOf(UQUANT) > -1) || (token.indexOf(EQUANT) > -1))
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
                            formatted.append(SPACE);
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
        return formatted.toString();
    }

    /** ***************************************************************
     * Format a formula for text presentation.
     */
    public static String textFormat(String input) {

        Formula f = new Formula(input);
        return f.format("", "  ", Character.valueOf((char) 10).toString());
    }

    /** ***************************************************************
     * Format a formula for text presentation.
     */
    @Override
    public String toString() {

        return format("", "  ", Character.valueOf((char) 10).toString());
    }

    /** ***************************************************************
     * Format a formula for text presentation include file and line#.
     */
    public String toStringMeta() {

        return format("", "  ", Character.valueOf((char) 10).toString()) +
                "[" + sourceFile + SPACE + startLine + "-" + endLine + "]";
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
    public String htmlFormat(KB kb, String href) {

        String fKbHref;
        String kbHref = (href + "/sigma/Browse.jsp?kb=" + kb.name);
        fKbHref = format(kbHref,"&nbsp;&nbsp;&nbsp;&nbsp;","<br>\n");
        return fKbHref;
    }

    /** ***************************************************************
     * Format a formula as a prolog statement.  Note that only tuples
     * are converted properly at this time.  Statements with any embedded
     * formulas or functions will be rejected with a null return.
     */
    public String toProlog() {

        System.out.println("INFO in Formula.toProlog(): formula: " + theFormula);
        if (!listP()) {
			System.out.println("Error in Formula.toProlog(): Not a formula: " + theFormula);
            return null;
        }
        if (empty()) {
        	System.out.println("Error in Formula.toProlog(): Empty formula: " + theFormula);
            return null;
        }
        StringBuilder result = new StringBuilder();
        String relation = car();
        Formula f = new Formula();
        f.theFormula = cdr();
        if (!Formula.atom(relation)) {
        	System.out.println("Error in Formula.toProlog(): Relation not an atom: " + relation);
            return null;
        }
        result.append(relation).append(LP);
        System.out.println("INFO in Formula.toProlog(): result so far: " + result.toString());
        System.out.println("INFO in Formula.toProlog(): remaining formula: " + f);
        String arg, newVar;
        while (!f.empty()) {
            arg = f.car();
            System.out.println("INFO in Formula.toProlog(): argForm: " + arg);
            f.theFormula = f.cdr();
            if (!Formula.atom(arg)) {
            	System.err.println("Error in Formula.toProlog(): Argument not an atom: " + arg);
                return null;
            }
            if (Formula.isVariable(arg)) {
                newVar = Character.toUpperCase(arg.charAt(1)) + arg.substring(2);
                result.append(newVar);
            }
            else if (StringUtil.isQuotedString(arg))
                result.append(arg);
            else
                result.append("'").append(arg).append("'");
            if (!f.empty())
                result.append(",");
            else
                result.append(RP);
        }
        return result.toString();
    }

    /** ***************************************************************
     *  Replace term2 with term1
     */
    public Formula rename(String term2, String term1) {

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
            if (f1.listP())
                newFormula = newFormula.cons(f1.rename(term2,term1));
            else
                newFormula = newFormula.append(f1.rename(term2,term1));
            Formula f2 = new Formula();
            f2.read(this.cdr());
            newFormula = newFormula.append(f2.rename(term2,term1));
        }
        return newFormula;
    }

    /** ***************************************************************
     * A test method.
     */
    public static void testCollectVariables() {

    	Formula f = new Formula();
    	f.read("(=> " +
                "  (and " +
                "    (attribute ?H Muslim) " +
                "    (equal " +
                "      (WealthFn ?H) ?W)) " +
                "(modalAttribute " +
                "  (exists (?Z ?T) " +
                "    (and " +
                "      (instance ?Z Zakat) " +
                "      (instance ?Y Year) " +
                "      (during ?Y " +
                "        (WhenFn ?H)) " +
                "      (holdsDuring ?Y " +
                "        (attribute ?H FullyFormed)) " +
                "      (agent ?Z ?H) " +
                "      (patient ?Z ?T) " +
                "      (monetaryValue ?T ?C) " +
                "      (greaterThan ?C " +
                "        (MultiplicationFn ?W 0.025)))) Obligation)) ");
    	System.out.println("Quantified variables: " + f.collectQuantifiedVariables());
    	System.out.println("All variables: " + f.collectAllVariables());
    	System.out.println("Unquantified variables: " + f.collectUnquantifiedVariables());
    	System.out.println("Terms: " + f.collectTerms());
    }

    /** ***************************************************************
     * A test method.
     */
    public static void testIsSimpleClause() {

        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
    	Formula f1 = new Formula();
    	f1.read("(not (instance ?X Human))");
    	System.out.println("Simple clause? : " + f1.isSimpleClause(kb) + "\n" + f1 + "\n");
    	f1.read("(instance ?X Human)");
    	System.out.println("Simple clause? : " + f1.isSimpleClause(kb) + "\n" + f1 + "\n");
        f1.read("(=> (attribute ?Agent Investor) (exists (?Investing) (agent ?Investing ?Agent)))");
    	System.out.println("Simple clause? : " + f1.isSimpleClause(kb) + "\n" + f1 + "\n");
        f1.read("(member (SkFn 1 ?X3) ?X3)");
    	System.out.println("Simple clause? : " + f1.isSimpleClause(kb) + "\n" + f1 + "\n");
        f1.read("(member ?VAR1 Org1-1)");
    	System.out.println("Simple clause? : " + f1.isSimpleClause(kb) + "\n" + f1 + "\n");
    	f1.read("(capability (KappaFn ?HEAR (and (instance ?HEAR Hearing) (agent ?HEAR ?HUMAN) " +
                "(destination ?HEAR ?HUMAN) (origin ?HEAR ?OBJ))) agent ?HUMAN)");
    	System.out.println("Simple clause? : " + f1.isSimpleClause(kb) + "\n" + f1 + "\n");
    }

    /** ***************************************************************
     * A test method.
     */
    public static void testReplaceVar() {

        Formula f1 = new Formula();
        f1.read("(<=> (instance ?REL TransitiveRelation) (forall (?INST1 ?INST2 ?INST3) " +
                " (=> (and (?REL ?INST1 ?INST2) (?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))");
        System.out.println("Input: " + f1);
        System.out.println(f1.replaceVar("?REL", "part"));
    }

    /** ***************************************************************
     * A test method.
     */
    public static void testComplexArgs() {

        Formula f1 = new Formula();
        f1.read("(during ?Y (WhenFn ?H))");
        System.out.println("Input: " + f1);
        System.out.println(f1.complexArgumentsToArrayList(1));
    }

    /** ***************************************************************
     * A test method.
     */
    public static void testBigArgs() {

        Formula f1 = new Formula();
        f1.read("(=>   (instance ?AT AutomobileTransmission)  (hasPurpose ?AT    (exists (?C ?D ?A ?R1 ?N1 ?R2 ?R3 ?R4 ?N2 ?N3)      (and        (instance ?C Crankshaft)        (instance ?D Driveshaft)        (instance ?A Automobile)        (part ?D ?A)        (part ?AT ?A)        (part ?C ?A)        (connectedEngineeringComponents ?C ?AT)        (connectedEngineeringComponents ?D ?AT)        (instance ?R1 Rotating)        (instance ?R2 Rotating)               (instance ?R3 Rotating)        (instance ?R4 Rotating)        (patient ?R1 ?C)        (patient ?R2 ?C)        (patient ?R3 ?D)        (patient ?R4 ?D)        (causes ?R1 ?R3)        (causes ?R2 ?R4)        (not          (equal ?R1 ?R2))        (holdsDuring ?R1          (measure ?C (RotationFn ?N1 MinuteDuration)))        (holdsDuring ?R2          (measure ?C (RotationFn ?N1 MinuteDuration)))        (holdsDuring ?R3          (measure ?D (RotationFn ?N2 MinuteDuration)))        (holdsDuring ?R4          (measure ?D (RotationFn ?N3 MinuteDuration)))        (not          (equal ?N2 ?N3))))))");
        System.out.println("Input: " + f1);
        System.out.println(f1.validArgs());
    }

    /** ***************************************************************
     * A test method.
     */
    public static void testCar1() {

        Formula f1 = new Formula();
        f1.read("(=>   (instance ?AT AutomobileTransmission)  (hasPurpose ?AT    (exists (?C ?D ?A ?R1 ?N1 ?R2 ?R3 ?R4 ?N2 ?N3)      (and        (instance ?C Crankshaft)        (instance ?D Driveshaft)        (instance ?A Automobile)        (part ?D ?A)        (part ?AT ?A)        (part ?C ?A)        (connectedEngineeringComponents ?C ?AT)        (connectedEngineeringComponents ?D ?AT)        (instance ?R1 Rotating)        (instance ?R2 Rotating)               (instance ?R3 Rotating)        (instance ?R4 Rotating)        (patient ?R1 ?C)        (patient ?R2 ?C)        (patient ?R3 ?D)        (patient ?R4 ?D)        (causes ?R1 ?R3)        (causes ?R2 ?R4)        (not          (equal ?R1 ?R2))        (holdsDuring ?R1          (measure ?C (RotationFn ?N1 MinuteDuration)))        (holdsDuring ?R2          (measure ?C (RotationFn ?N1 MinuteDuration)))        (holdsDuring ?R3          (measure ?D (RotationFn ?N2 MinuteDuration)))        (holdsDuring ?R4          (measure ?D (RotationFn ?N3 MinuteDuration)))        (not          (equal ?N2 ?N3))))))");
        System.out.println("Input: " + f1);
        System.out.println(f1.validArgs());
    }

    /** ***************************************************************
     * A test method.
     */
    public static void testArg2ArrayList() {

        System.out.println("testArg2ArrayList(): ");
        String f = "(termFormat EnglishLanguage experimentalControlProcess \"experimental control (process)\")";
        Formula form = new Formula(f);
        System.out.println(form.complexArgumentsToArrayList(0));
    }

    /** ***************************************************************
     */
    public Formula negate() {

        return new Formula(Formula.LP + Formula.NOT + SPACE + theFormula + RP);
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("KB class");
        System.out.println("  options (with a leading '-'):");
        System.out.println("  h - show this help screen");
        System.out.println("  t \"<formula\" - formula type");
        System.out.println("  x \"<formula\" - format a formula");
        System.out.println("  r \"<formula\" - remove temporal relations on formulas");
    }

    /** ***************************************************************
     */
    public static void main(String[] args) throws IOException {

        System.out.println("INFO in Formula.main()");
        if (args == null)
            System.out.println("no command given");
        else
            System.out.println(args.length + " : " + Arrays.toString(args));
        if (args != null && args.length > 0 && args[0].equals("-h"))
            showHelp();
        else {
            KBmanager.getMgr().initializeOnce();
            String kbName = KBmanager.getMgr().getPref("sumokbname");
            KB kb = KBmanager.getMgr().getKB(kbName);
            if (args != null && args.length > 1 && args[0].contains("t")) {
                Formula f = new Formula(args[1]);
                System.out.println("Formula.main() formula type of " + args[1] + " : " + f.findType(kb));
            }
            else if (args != null && args.length > 1 && args[0].contains("x")) {
                System.out.println(Formula.textFormat(args[1]));
            }
            else if (args != null && args.length > 1 && args[0].contains("r")) {
                // The opening and closing parentheses are because it expects a lisp list.
                System.out.println(Formula.removeTemporalRelations(LP+args[1]+RP, kb));
            }
            else
                showHelp();
        }
    }
}
