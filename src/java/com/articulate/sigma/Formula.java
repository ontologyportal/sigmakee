package com.articulate.sigma;

import java.util.*;
import java.io.*;

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

/** Handle operations on an individual formula.  This includes formatting
 *  for presentation as well as pre-processing for sending to the 
 *  inference engine.
 */
public class Formula implements Comparable {

     /** The source file in which the formula appears. */
    public String sourceFile;   
     /** The line in the file on which the formula starts. */
    public int startLine;       
     /** The line in the file on which the formula ends. */
    public int endLine;         
     /** The formula. */
    public String theFormula;   

    /******************************************************************
     * Read a String into the variable 'theFormula'.
     */
    public void read(String s) {
        theFormula = s;
    }
    
    /******************************************************************
     * Implement the Comparable interface by defining the compareTo
     * method.  Formulas are equal if their formula strings are equal.
     */
    public int compareTo(Object f) throws ClassCastException {
        if (!f.getClass().getName().equalsIgnoreCase("com.articulate.sigma.Formula")) 
            throw new ClassCastException("Error in Formula.compareTo(): Class cast exception for argument of class: " + f.getClass().getName());
        return theFormula.compareTo(((Formula) f).theFormula);
    }

    /******************************************************************
     * Test if the contents of the formula are equal to the String argument.
     * Normalize all variables.
     */
    public boolean equals(String s) {

        String f = theFormula;
        Formula form = new Formula();
        Formula sform = new Formula();
        
        form.theFormula = f;
        s = normalizeVariables(s).intern();
        sform.read(s);
        s = sform.textFormat().trim().intern();

        form.theFormula = normalizeVariables(theFormula);
        f = form.textFormat().trim().intern();
        // System.out.println("INFO in Formula.equals(): Comparing " + s + " to " + f);
        return (f == s);
    }
    
    /******************************************************************
     * Test if the contents of the formula are equal to the argument.
     */
    public boolean deepEquals(Formula f) {

        return (f.theFormula.intern() == theFormula.intern()) &&
               (f.sourceFile.intern() == sourceFile.intern());
    }

    /******************************************************************
     * Return the numbered argument of the given formula.  The first
     * element of a formula is number 0.
     */
    public String getArgument(int argnum) {

        int start = -1;
        int end = -1;
        int parenLevel = 0;
        int tokenNumber = -1;
        int i = -1;
        boolean marker;
        boolean oldMarker = false;
        boolean newMarker = false;
        while (i < theFormula.length() - 1) {
            i++;
            char ch = theFormula.charAt(i);
            switch (ch) {
                case '(': {  
                    parenLevel++;
                    if (parenLevel == 1)
                        tokenNumber++;
                    if (parenLevel == 2) {
                        start = i;
                    }
                    break;
                }
                case ')': {      
                    parenLevel--;
                    break;
                }
                case ' ': {
                    if (parenLevel == 1)
                        tokenNumber++;
                    // i++;
                    break;
                }
            }
            if (ch != '(' && ch != ')' && ch != ' ') {
                marker = false;
            }
            else {
                marker = true;
            }
            if (i > 0 && tokenNumber == argnum && 
                (theFormula.charAt(i-1) == '(' || theFormula.charAt(i-1) == ' ') &&
                !marker && parenLevel == 1) 
                start = i;
            if (tokenNumber == argnum + 1 || parenLevel == 0) {
                end = i;
                i = theFormula.length();
            }
        }
        return theFormula.substring(start,end);
    }

    /******************************************************************
     * Normalize all variables, so that the first variable in a formula is
     * ?VAR1, the second is ?VAR2 etc.  This is necessary so that two 
     * formulas can be found equal even if they have different variable
     * names. Variables must be normalized so that (foo ?A ?B) is 
     * equal to (foo ?X ?Y) - they both are converted to (foo ?VAR1 ?VAR2)
     */
    private static String normalizeVariables(String s) {

        int i = 0;
        int varCount = 0;
        int rowVarCount = 0;
        int varstart = 0;
        
        while (varstart != -1) {
            varstart = s.indexOf('?',i + 1);
            if (varstart != -1) {
                int varend = varstart+1;
                while (Character.isJavaIdentifierPart(s.charAt(varend)) && varend < s.length())
                    varend++;
                String varname = s.substring(varstart+1,varend);
                s = s.replaceAll("\\?" + varname,"?VAR" + (new Integer(varCount++)).toString());
                i = varstart;
            }
        }

        i = 0;
        while (varstart != -1) {
            varstart = s.indexOf('@',i + 1);
            if (varstart != -1) {
                int varend = varstart+1;
                while (Character.isJavaIdentifierPart(s.charAt(varend)) && varend < s.length())
                    varend++;
                String varname = s.substring(varstart+1,varend);
                s = s.replaceAll("\\@" + varname,"@ROWVAR" + (new Integer(varCount++)).toString());
                i = varstart;
            }
        }

        return s;
    }

    /******************************************************************
     * Translate SUMO inequalities to the typical inequality symbols that 
     * Vampire requires.
     */
    private String translateInequalities(String s) {
        
        if (s.equalsIgnoreCase("greaterThan")) return ">";
        if (s.equalsIgnoreCase("greaterThanOrEqualTo")) return ">=";
        if (s.equalsIgnoreCase("lessThan")) return "<";
        if (s.equalsIgnoreCase("lessThanOrEqualTo")) return "<=";
        return "";
    }

    /******************************************************************
     * Makes implicit universal quantification explicit.  May be needed
     * in the future for other theorem provers.
     */
    private String makeQuantifiersExplicit() {
        
        ArrayList quantVariables = new ArrayList();
        ArrayList unquantVariables = new ArrayList();
        System.out.println("Adding quantified variables.");
        int startIndex = -1;                        // Collect all quantified variables
        while (theFormula.indexOf("(forall (?",startIndex) != -1 ||
               theFormula.indexOf("(exists (?",startIndex) != -1) {
            int forallIndex = theFormula.indexOf("(forall (?",startIndex);
            int existsIndex = theFormula.indexOf("(exists (?",startIndex);
            if ((forallIndex < existsIndex && forallIndex != -1) || existsIndex == -1) 
                startIndex = forallIndex + 9;
            else
                startIndex = existsIndex + 9;
            int i = startIndex;
            while (theFormula.charAt(i) != ')' && i < theFormula.length()) {
                i++;
                if (theFormula.charAt(i) == ' ') { 
                    quantVariables.add(theFormula.substring(startIndex,i));
                    System.out.println(theFormula.substring(startIndex,i));
                    startIndex = i+1;
                }
            }
            System.out.println(startIndex);
            System.out.println(i);
            if (i < theFormula.length()) {
                quantVariables.add(theFormula.substring(startIndex,i).intern());
                System.out.println(theFormula.substring(startIndex,i));
                startIndex = i+1;
            }
            else
                startIndex = theFormula.length();
        }

        System.out.println("Adding unquantified variables.");
        startIndex = 0;                        // Collect all unquantified variables
        while (theFormula.indexOf("?",startIndex) != -1) {
            startIndex = theFormula.indexOf("?",startIndex);
            int spaceIndex = theFormula.indexOf(" ",startIndex);
            int parenIndex = theFormula.indexOf(")",startIndex);
            int i;
            if ((spaceIndex < parenIndex && spaceIndex != -1) || parenIndex == -1) 
                i = spaceIndex;
            else
                i = parenIndex;
            if (!quantVariables.contains(theFormula.substring(startIndex,i).intern())) {
                unquantVariables.add(theFormula.substring(startIndex,i).intern());                
                System.out.println(theFormula.substring(startIndex,i));
            }
            startIndex = i;
        }

        StringBuffer quant = new StringBuffer("(forall (");  // Quantify all the unquantified variables
        for (int i = 0; i < unquantVariables.size(); i++) {
            quant = quant.append((String) unquantVariables.get(i));
            if (i < unquantVariables.size() - 1) 
                quant = quant.append(" ");
        }
        return quant.toString() + ") " + theFormula + ")";
    }

    /******************************************************************
     * Expand row variables.  Each variable is treated like a macro that
     * expands to up to seven regular variables.  For example
     *
     * (=>
     *    (and
     *       (subrelation ?REL1 ?REL2)
     *       (holds ?REL1 @ROW))
     *    (holds ?REL2 @ROW))
     *
     * would become 
     *
     * (=>
     *    (and
     *       (subrelation ?REL1 ?REL2)
     *       (holds ?REL1 ?ARG1))
     *    (holds ?REL2 ?ARG1))
     * 
     * (=>
     *    (and
     *       (subrelation ?REL1 ?REL2)
     *       (holds ?REL1 ?ARG1 ?ARG2))
     *    (holds ?REL2 ?ARG1 ?ARG2))
     * etc. 
     */
    private String expandRowVars(String input, HashMap rowVarMap) {

        StringBuffer result = new StringBuffer(input);
        Iterator it = rowVarMap.keySet().iterator();
        while (it.hasNext()) {
            String row = (String) it.next();
            StringBuffer rowResult = new StringBuffer();
            StringBuffer rowReplace = new StringBuffer();
            for (int j = 1; j < 8; j++) {
                if (rowReplace.toString().length() > 0) {
                    rowReplace = rowReplace.append(" ");
                }
                rowReplace = rowReplace.append("\\?" + row + (new Integer(j)).toString());
                rowResult = rowResult.append(result.toString().replaceAll("\\@" + row, rowReplace.toString()) + "\n");
            }
            result = new StringBuffer(rowResult.toString());
        }
        return result.toString();
    }    

    /******************************************************************
     * Pre-process a formula before sending it to Vampire. This includes
     * ignoring meta-knowledge like documentation strings, translating
     * mathematical operators, quoting higher-order formulas, expanding
     * row variables and prepending the 'holds' predicate.
     */
    public String preProcess() {

        String s = theFormula;
        Stack predicateStack = new Stack();
        HashMap varMap = new HashMap();      // A list of variable names and their normalized names.
        HashMap rowVarMap = new HashMap();   // A list of normalized variable names for row variables.
                                             // Variables must be normalized so that (foo ?A ?B) is 
                                             // equal to (foo ?X ?Y) - they both are converted to (foo ?VAR1 ?VAR2)
        StringBuffer result = new StringBuffer();
        String[] logOps = {"and", "or", "not", "=>", "<=>", "forall", "exists"};
        String[] matOps = {"equal", "greaterThan", "greaterThanOrEqualTo", "lessThan", "lessThanOrEqualTo", 
                           "AdditionFn", "SubtractionFn", "MultiplicationFn", "DivisionFn"};
        String[] compOps = {"greaterThan", "greaterThanOrEqualTo", "lessThan", "lessThanOrEqualTo"};
        ArrayList logicalOperators = new ArrayList(Arrays.asList(logOps));
        ArrayList mathOperators = new ArrayList(Arrays.asList(matOps));
        ArrayList comparisonOperators = new ArrayList(Arrays.asList(compOps));
        String lastPredicate = null;
        predicateStack.push("dummy");
        
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '(': {
                    if (Character.isJavaIdentifierStart(s.charAt(i+1))) {
                        i++;
                        int predicateStart = i;
                        while (Character.isJavaIdentifierPart(s.charAt(i)))
                            i++;
                        String predicate = s.substring(predicateStart,i);
                        if (predicate.equalsIgnoreCase("documentation") ||
                            predicate.equalsIgnoreCase("format") ||
                            predicate.equalsIgnoreCase("termFormat")) 
                            return "";
                        if (mathOperators.contains(predicate)) {
                            // translate math operators
                        }
                        if (lastPredicate != null &&
                            !logicalOperators.contains(lastPredicate.intern()) && 
                            predicate.substring(predicate.length()-2,predicate.length()).compareTo("Fn") != 0) {
                            result = result.append('`');
                            // tick the formula
                        }
                        result = result.append(ch); 
                        if (!logicalOperators.contains(predicate.intern()) && !mathOperators.contains(predicate.intern()) && !predicate.equalsIgnoreCase("holds")) {
                            result = result.append("holds " + predicate);
                        }
                        else {
                            if (comparisonOperators.contains(predicate.intern())) 
                                predicate = translateInequalities(predicate);
                            result = result.append(predicate);
                        }
                        i--;                       
                        lastPredicate = predicate;
                        predicateStack.push(predicate);
                    }
                    else {
                        i++;
                        if (s.charAt(i) == '?' && !lastPredicate.equalsIgnoreCase("forall") && !lastPredicate.equalsIgnoreCase("exists") ) {
                            result = result.append(ch);
                            result = result.append("holds ");
                            lastPredicate = "holds";
                            predicateStack.push("holds");
                        }
                        else {
                            int predicateStart = i;
                            while (s.charAt(i) != ' ' && s.charAt(i) != ')')
                                i++;
                            String predicate = s.substring(predicateStart,i);
                            result = result.append(ch);
                            result = result.append(predicate);
                            lastPredicate = predicate;
                            predicateStack.push(predicate);
                            i--;
                        }
                    }
                    break;
                }
                case '@': {
                    if (Character.isJavaIdentifierStart(s.charAt(i+1))) {
                        i++;
                        int varStart = i;
                        while (Character.isJavaIdentifierPart(s.charAt(i)))
                            i++;
                        String var = s.substring(varStart,i);
                        if (!rowVarMap.keySet().contains(var)) {
                            rowVarMap.put(var,null);
                        }
                        result = result.append("@" + var);
                        i--;
                    }
                    break;
                }
                case ')': {
                    predicateStack.pop();                  
                    lastPredicate = (String) predicateStack.peek();
                    break;
                }
                case '"': {
                    result = result.append(ch);
                    i++;
                    while (s.charAt(i) != '"') {
                        result.append(s.charAt(i));                        
                        i++;
                    }
                    break;
                }
            }
            if (ch != '(' && ch != '@') {
                result = result.append(ch);
            }
        }
        result = new StringBuffer(expandRowVars(result.toString(),rowVarMap));
        return result.toString();
    }

    /******************************************************************
     * Compare the given formula to the negated query and return whether
     * they are the same (minus the negation).
     */
    public static boolean isNegatedQuery(String query, String formula) {

        boolean result = false;

        //System.out.println("INFO in Formula.isNegatedQuery(): Comparing |" + query + "| to |" + formula + "|");

        formula = formula.trim();
        if (formula.substring(0,4).compareTo("(not") != 0) 
            return false;
        formula = formula.substring(5,formula.length()-1);
        Formula f = new Formula();
        f.read(formula);
        result = f.equals(query);
        //System.out.print("INFO in Formula.isNegatedQuery(): ");
        //System.out.println(result);
        return result;
    }

    /******************************************************************
     * Remove the 'holds' prefix wherever it appears.
     */
    public static String postProcess(String s) {

        s = s.replaceAll("holds ","");
        return s;
    }

    /******************************************************************
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

        boolean inQuantifier = false;
        StringBuffer token = new StringBuffer();
        StringBuffer formatted = new StringBuffer();
        int indentLevel = 0;
        boolean inToken = false;
        boolean inVariable = false;
        boolean inVarlist = false;
        boolean inComment = false;

        theFormula = theFormula.trim();

        for (int i = 0; i < theFormula.length(); i++) {
            // System.out.println("INFO in format(): " + formatted.toString());
            if (!inComment) {
                if (theFormula.charAt(i) == '(' && !inQuantifier && (indentLevel != 0 || i > 1)) {
                    if (i > 0 && Character.isWhitespace(theFormula.charAt(i-1))) { 
                        //System.out.println("INFO in format(): Deleting at end of : |" + formatted.toString() + "|");
                        formatted = formatted.deleteCharAt(formatted.length()-1);
                    }
                    formatted = formatted.append(eolChars);
                    for (int j = 0; j < indentLevel; j++) {
                        formatted = formatted.append(indentChars);
                    }
                }
                if (theFormula.charAt(i) == '(' && indentLevel == 0 && i == 0) 
                    formatted = formatted.append(theFormula.charAt(i));
                if (Character.isJavaIdentifierStart(theFormula.charAt(i)) && !inToken && !inVariable) {
                    token = new StringBuffer(theFormula.charAt(i));
                    inToken = true;
                }
                if ((Character.isJavaIdentifierPart(theFormula.charAt(i)) || theFormula.charAt(i) == '-') && inToken)
                    token = token.append(theFormula.charAt(i));
                if (theFormula.charAt(i) == '(') {
                    if (inQuantifier) {
                        inQuantifier = false;
                        inVarlist = true;
                        token = new StringBuffer();
                    }
                    else
                        indentLevel++;
                }
                if (theFormula.charAt(i) == '"') 
                    inComment = true;                // The next character will be handled in the "else" clause of this primary "if"
                if (theFormula.charAt(i) == ')') {
                    if (!inVarlist)
                        indentLevel--;
                    else
                        inVarlist = false;
                }
                if (token.toString().compareTo("exists") == 0 || token.toString().compareTo("forall") == 0)
                    inQuantifier = true;
                if (!Character.isJavaIdentifierPart(theFormula.charAt(i)) && inVariable) 
                    inVariable = false;
                if (theFormula.charAt(i) == '?' || theFormula.charAt(i) == '@')
                    inVariable = true;
                if (!(Character.isJavaIdentifierPart(theFormula.charAt(i)) || theFormula.charAt(i) == '-') && inToken) {
                    inToken = false;
                    if (hyperlink != "")
                        formatted = formatted.append("<a href=\"" + hyperlink + "&term=" + token + "\">" + token + "</a>");
                    else
                        formatted = formatted.append(token);
                    token = new StringBuffer();
                }
                if (!inToken && i>0 && !(Character.isWhitespace(theFormula.charAt(i)) && theFormula.charAt(i-1) == '(')) {
                    if (Character.isWhitespace(theFormula.charAt(i))) { 
                        if (!Character.isWhitespace(theFormula.charAt(i-1)))
                            formatted = formatted.append(" ");
                    }
                    else
                        formatted = formatted.append(theFormula.charAt(i));
                }
            }
            else {                                                     // In a comment
                formatted = formatted.append(theFormula.charAt(i));
                if (theFormula.charAt(i) == '"') 
                    inComment = false;
            }
        }
        if (inToken) {                                // A term which is outside of parenthesis, typically, a binding.
            if (hyperlink != "")
                formatted = formatted.append("<a href=\"" + hyperlink + "&term=" + token + "\">" + token + "</a>");
            else
                formatted = formatted.append(token);
        }
        return formatted.toString();
    }

    /******************************************************************
     * Format a formula for text presentation.
     */
    public String textFormat() {

        return format("","  ",new Character((char) 10).toString());
    }

    /******************************************************************
     * Format a formula for HTML presentation.
     */
    public String htmlFormat(String html) {

        return format(html,"&nbsp;&nbsp;&nbsp;&nbsp;","<br>\n");
    }

    /******************************************************************
     * A test method.
     */
    public static void main(String[] args) {

        Formula f = new Formula();
        f.theFormula = "(=> (equal ?REL1 ?REL2) (forall (@ROW1 @ @ROW2) (<=> (holds ?REL1 @ROW1) (holds ?REL2 @ROW2))))";
        System.out.println(f.preProcess());
    }

}
