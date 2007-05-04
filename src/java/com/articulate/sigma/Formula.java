package com.articulate.sigma;

import java.util.*;
import java.io.*;
import java.text.ParseException;

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
    public String theTPTPFormula;

    /** ***************************************************************
     * For any given formula, stop generating new pred var
     * instantiations and row var expansions if this threshold value
     * has been exceeded.  The default value is 2000. 
     */
    private static int AXIOM_EXPANSION_LIMIT = 2000;

    /** ***************************************************************
     * Read a String into the variable 'theFormula'.
     */
    public void read(String s) {
        theFormula = s;
    }
    
    /** ***************************************************************
     * Copy the Formula.
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
     * Implement the Comparable interface by defining the compareTo
     * method.  Formulas are equal if their formula strings are equal.
     */
    public int compareTo(Object f) throws ClassCastException {
        if (!f.getClass().getName().equalsIgnoreCase("com.articulate.sigma.Formula")) 
            throw new ClassCastException("Error in Formula.compareTo(): Class cast exception for argument of class: " + f.getClass().getName());
        return theFormula.compareTo(((Formula) f).theFormula);
    }

    /** ***************************************************************
     * Return the LISP 'car' of the formula - the first element of the list.
     * Note that this operation has no side effect on the Formula.
     */
    public String car() {

        //System.out.println("INFO in formula.car(): theFormula: " + theFormula);
        if (theFormula == null) {
            System.out.println("Error in Formula.car(): Null string");
            return "";
        }
        if (!listP()) return null;       
        //System.out.println("INFO in Formula.car: theformula: " + theFormula);
        int i = 0;
        while (theFormula.charAt(i) != '(') i++;
        i++;
        while (Character.isWhitespace(theFormula.charAt(i))) i++;
        int start = i;
        if (theFormula.charAt(i) == '(') {
            int level = 0;
            i++;
            while (theFormula.charAt(i) != ')' || level > 0) {
                // System.out.print(theFormula.charAt(i));
                if (theFormula.charAt(i) == ')') level--;
                if (theFormula.charAt(i) == '(') level++;
                i++;            
            }
            i++;
        }
        else {        
            if (theFormula.charAt(i) == '"' || theFormula.charAt(i) == '\'') {
                char quoteChar = theFormula.charAt(i);
                i++;
                while (((theFormula.charAt(i) != quoteChar || 
                         (theFormula.charAt(i) == quoteChar && theFormula.charAt(i-1) == '\\')) &&
                        i < theFormula.length() - 1)) {                  
                    i++;
                }
                i++;
            }
            else {
                while (!Character.isWhitespace(theFormula.charAt(i)) && i < theFormula.length() - 1) i++;
            }            
        }
        //System.out.println("INFO in formula.car() end: theFormula: " + theFormula.substring(start,i));
        return theFormula.substring(start,i);    
    }

    /** ***************************************************************
     * Return the LISP 'cdr' of the formula - the rest of a list minus its
     * first element.
     * Note that this operation has no side effect on the Formula.
     */
    public String cdr() {

        //System.out.println("INFO in formula.cdr(): theFormula: " + theFormula);
        if (theFormula == null) {
            System.out.println("Error in Formula.cdr(): Null string");
            return "";
        }
        if (!listP()) return null;       
        int i = 0;
        while (theFormula.charAt(i) != '(') i++;
        i++;
        while (Character.isWhitespace(theFormula.charAt(i))) i++;
        int start = i;
        if (theFormula.charAt(i) == '(') {
            int level = 0;
            i++;
            while (theFormula.charAt(i) != ')' || level > 0) {
                //System.out.print(theFormula.charAt(i));
                if (theFormula.charAt(i) == ')') level--;
                if (theFormula.charAt(i) == '(') level++;
                i++;            
            }
            i++;
        }
        else {
            if (theFormula.charAt(i) == '"' || theFormula.charAt(i) == '\'') {
                char quoteChar = theFormula.charAt(i);
                i++;
                while (((theFormula.charAt(i) != quoteChar || 
                         (theFormula.charAt(i) == quoteChar && theFormula.charAt(i-1) == '\\')) &&
                        i < theFormula.length() - 1)) {                  
                    i++;
                }
                i++;
            }
            else {
                while (!Character.isWhitespace(theFormula.charAt(i)) && i < theFormula.length() - 1) i++;
            }
        }
        while (Character.isWhitespace(theFormula.charAt(i))) i++;
        int end = theFormula.lastIndexOf(')');
        return "(" + theFormula.substring(i,end) + ")";
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
    private Formula cons(String obj) {
	Formula ans = this;
	try {
	    String fStr = this.theFormula;
	    if ( isNonEmptyString(obj) && isNonEmptyString(fStr) ) {
		String theNewFormula = null;
		if ( this.listP() ) {
		    if ( this.empty() ) {
			theNewFormula = ( "(" + obj + ")" );
		    }
		    else {
			theNewFormula = ( "("
					  + obj
					  + " "
					  + fStr.substring( 1, (fStr.length() - 1) )
					  + ")" );
		    }
		}
		else {
		    // This should never happen during clausification, but
		    // we include it to make this procedure behave
		    // (almost) like its LISP namesake.
		    theNewFormula = ( "(" + obj + " . " + fStr + ")" );
		}
		if ( theNewFormula != null ) {
		    ans = new Formula();
		    ans.read( theNewFormula );
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return ans;
    }

    /** ***************************************************************
     * Returns the LISP 'cdr' of the formula as a new Formula, if
     * possible, else returns null.
     *
     * Note that this operation has no side effect on the Formula.
     *
     * @return a Formula, or null.
     */
    public Formula cdrAsFormula() {
	String thisCdr = this.cdr();
	if ( isNonEmptyString(thisCdr) && listP(thisCdr) ) {
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
     *
     * @return a String, or the empty string if the is no cadr.
     *
     */
    public String cadr() {
	return this.getArgument( 1 );
    }

    /** ***************************************************************
     * Returns the LISP 'cddr' of the formula - the rest of the rest,
     * or the list minus its first two elements.
     * 
     * Note that this operation has no side effect on the Formula.
     *
     * @return a String, or null.
     *
     */
    public String cddr() {
	Formula fCdr = this.cdrAsFormula();
	if ( fCdr != null ) {
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
	if ( isNonEmptyString(thisCddr) && listP(thisCddr) ) {
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
	return this.getArgument( 2 );
    }

    /** ***************************************************************
     * Test whether the String is a LISP atom.
     */
    public static boolean atom(String s) {

        if (s == null || s == "" || s.length() < 1) {
            System.out.println("Error in Formula.atom(): Null string");

            Thread.dumpStack();
            throw new NullPointerException();
            
        }
        if (s.charAt(0) == '"' && s.charAt(s.length()-1) == '"') 
            return true;
        if (s.indexOf(')') == -1 &&
            s.indexOf('\n') == -1 &&
            s.indexOf(' ') == -1 &&
            s.indexOf('\t') == -1) return true;
        else return false;
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
     * Test whether the String is an empty formula.
     */
    public static boolean empty(String s) {

        if (s == null || s == "" || s.length() < 1) {
            System.out.println("Error in Formula.empty(): Null string");
            Thread.dumpStack();
            throw new NullPointerException();
        }
        s.trim();
        if (s.matches("\\(\\s*\\)")) return true;
        else return false;
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
    private static boolean listP(String s) {

        if (s == null || s == "" || s.length() < 1) {
            System.out.println("Error in Formula.listP(): Null string");
            Thread.dumpStack();
            throw new NullPointerException();
        }
        s.trim();
        if (s.charAt(0) == '(' && s.charAt(s.length()-1) == ')') return true;
        else return false;
    }

    /** ***************************************************************
     * @see #validArgs() validArgs below for documentation
     */
    private String validArgsRecurse(Formula f) {

        //System.out.println("INFO in Formula.validArgsRecurse(): Formula: " + f.theFormula);
        if (f.theFormula == "" || !f.listP() || f.atom() || f.empty()) return "";
        String pred = f.car();

	// AB: predF is created here, but never used. (?)
        Formula predF = new Formula();
        predF.read(pred);

        String rest = f.cdr();
        Formula restF = new Formula();
        restF.read(rest);
        int argCount = 0;
        while (!restF.empty()) {
            argCount++;
            String arg = restF.car();
            Formula argF = new Formula();
            argF.read(arg);
            String result = validArgsRecurse(argF);
            if (result != "") 
                return result;
            restF.theFormula = restF.cdr();
        }
        if (pred.equals("and") || pred.equals("or")) {
            if (argCount < 2) 
                return "Too few arguments for 'and' or 'or' in formula: \n" + f.toString() + "\n";
        }
        else {        
            if (pred.equals("forall") || pred.equals("exists")) {
                 if (argCount != 2) 
                     return "Wrong number of arguments for 'exists' or 'forall' in formula: \n" + f.toString() + "\n";
                 else {
                     Formula quantF = new Formula();
                     quantF.read(rest);
                     if (!listP(quantF.car())) 
                         return "No parenthesized variable list for 'exists' or 'forall' in formula: \n" + f.toString() + "\n";
                 }
            }
            else {            
                if (pred.equals("<=>") || pred.equals("=>")) {
                    if (argCount != 2) 
                        return "Wrong number of arguments for '<=>' or '=>' in formula: \n" + f.toString() + "\n";
                }
                else {                
                    if (pred.equals("equals")) {
                         if (argCount != 2) 
                             return "Wrong number of arguments for 'equals' in formula: \n" + f.toString() + "\n";
                    }
                }
            }
        }
        return "";
    }

    /** ***************************************************************
     * Test whether the Formula uses logical operators with the standard
     * number of arguments.  "equals", "<=>", and "=>" are strictly binary. 
     * "or", and "and" are binary or greater. "not" is unary.  "forall" and
     * "exists" are unary with an argument list.
     *  @return an empty String if there are no problems or an error message
     *  if there are.
     */
    public String validArgs() {

        if (theFormula == null || theFormula == "") 
            return "";
        Formula f = new Formula();
        f.read(theFormula);
        String result = validArgsRecurse(f);
        //System.out.println("INFO in Formula.validArgs(): result: " + result);
        return result;
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

        ArrayList thisList = parseList(this.theFormula.substring(1,this.theFormula.length()-1));  // an ArrayList of Formulas
        ArrayList sList = parseList(s.substring(1,s.length()-1));
        if (thisList.size() != sList.size()) 
            return false;

        for (int i = 0; i < thisList.size(); i++) {
            for (int j = 0; j < sList.size(); j++) {
                if (((Formula) thisList.get(i)).logicallyEquals(((Formula) sList.get(j)).theFormula)) {
                    // System.out.println("INFO in Formula.compareFormulaSets(): " + 
                    //                   ((Formula) thisList.get(i)).toString() + " equal to " +
                    //                   ((Formula) sList.get(j)).theFormula);
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
        s = sform.toString().trim().intern();

        form.theFormula = normalizeVariables(theFormula);
        f = form.toString().trim().intern();
        // System.out.println("INFO in Formula.equals(): Comparing " + s + " to " + f);
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

        Formula form = new Formula();
        form.read(theFormula);
        if (argnum == 0) 
            return form.car();
        for (int i = 0; i < argnum; i++) {
            form.read(form.cdr());
        }
        return form.car();
    }

    /** ***************************************************************
     * Return all the arguments in a simple formula as a list, starting
     * at the given argument.  If formula is complex (i.e. an argument
     * is a function or sentence), then return null.  If the starting
     * argument is greater than the number of arguments, also return
     * null.
     */
    public ArrayList argumentsToArrayList(int start) {

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
     * Normalize all variables, so that the first variable in a formula is
     * ?VAR1, the second is ?VAR2 etc.  This is necessary so that two 
     * formulas can be found equal even if they have different variable
     * names. Variables must be normalized so that (foo ?A ?B) is 
     * equal to (foo ?X ?Y) - they both are converted to (foo ?VAR1 ?VAR2)
     * Note that this routine has a significant known bug that variables
     * whose names are a subset of one another will cause problems, for
     * example (foo ?VAR ?VAR1)
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

    /** ***************************************************************
     * Translate SUMO inequalities to the typical inequality symbols that 
     * the theorem prover requires.
     */
    private String translateInequalities(String s) {
        
        if (s.equalsIgnoreCase("greaterThan")) return ">";
        if (s.equalsIgnoreCase("greaterThanOrEqualTo")) return ">=";
        if (s.equalsIgnoreCase("lessThan")) return "<";
        if (s.equalsIgnoreCase("lessThanOrEqualTo")) return "<=";
        return "";
    }

    /** ***************************************************************
     * Collect all the quantified variables in a formula
     */
    private ArrayList collectQuantifiedVariables(String theFormula) {

        int startIndex = -1;                        
        ArrayList quantVariables = new ArrayList();
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
                    if (!quantVariables.contains(theFormula.substring(startIndex,i).intern()))
                        quantVariables.add(theFormula.substring(startIndex,i));
                    //System.out.println(theFormula.substring(startIndex,i));
                    startIndex = i+1;
                }
            }
            //System.out.println(startIndex);
            //System.out.println(i);
            if (i < theFormula.length()) {
                if (!quantVariables.contains(theFormula.substring(startIndex,i).intern()))
                    quantVariables.add(theFormula.substring(startIndex,i).intern());
                //System.out.println(theFormula.substring(startIndex,i));
                startIndex = i+1;
            }
            else
                startIndex = theFormula.length();
        }
        return quantVariables;
    }

    /** ***************************************************************
     * Collect all the quantified variables in a formula
     */
    private ArrayList collectQuantifiedVariables() {

	return this.collectQuantifiedVariables(this.theFormula);
    }

    /** ***************************************************************
     * Collect all the unquantified variables in a formula
     */
    private ArrayList collectUnquantifiedVariables(String theFormula, ArrayList quantVariables) {

        int startIndex = 0;                        
        ArrayList unquantVariables = new ArrayList();

        while (theFormula.indexOf("?",startIndex) != -1) {
            startIndex = theFormula.indexOf("?",startIndex);
            int spaceIndex = theFormula.indexOf(" ",startIndex);
            int parenIndex = theFormula.indexOf(")",startIndex);
            int i;
            if ((spaceIndex < parenIndex && spaceIndex != -1) || parenIndex == -1) 
                i = spaceIndex;
            else
                i = parenIndex;
            if (!quantVariables.contains(theFormula.substring(startIndex,i).intern()) &&
                !unquantVariables.contains(theFormula.substring(startIndex,i).intern())) {
                unquantVariables.add(theFormula.substring(startIndex,i).intern());                
                //System.out.println(theFormula.substring(startIndex,i));
            }
            startIndex = i;
        }
        return unquantVariables;
    }

    /** ***************************************************************
     * Makes implicit quantification explicit.  
     * @param query controls whether to add universal or existential
     * quantification.  If true, add existential.
     * @result the formula as a String, with explicit quantification
     */
    public String makeQuantifiersExplicit(boolean query) {

        if (theFormula.indexOf("(documentation") == 0 ) 
            return theFormula;
        ArrayList quantVariables = collectQuantifiedVariables(theFormula);
        ArrayList unquantVariables = collectUnquantifiedVariables(theFormula,quantVariables);

        if (unquantVariables.size() > 0) {           // Quantify all the unquantified variables
            StringBuffer quant = new StringBuffer("(forall (");  
            if (query) 
                quant = new StringBuffer("(exists (");             
            for (int i = 0; i < unquantVariables.size(); i++) {
                quant = quant.append((String) unquantVariables.get(i));
                if (i < unquantVariables.size() - 1) 
                    quant = quant.append(" ");
            }
            //System.out.println("INFO in Formula.makeQuantifiersExplicit(): result: " + 
            //                    quant.toString() + ") " + theFormula + ")");
            return quant.toString() + ") " + theFormula + ")";
        }
        else
            return theFormula;
    }

    /** ***************************************************************
     * Find all the row variables in an input String.
     * @return a TreeSet of row variable names, without their '@' designator
     * */
    private TreeSet findRowVars(String input) {

        Formula f = new Formula();
        f.read(input);
	return f.findRowVars();
    }

    /** ***************************************************************
     * Find all the row variables in this Formula.  
     * @return a TreeSet of row variable names, without their '@' designator
     * */
    private TreeSet findRowVars() {

        TreeSet result = new TreeSet();
        int i = 0;
        Formula f = new Formula();
        f.read( this.theFormula );

        while (f.listP() && !f.empty()) {
            String arg = f.car();
            if (arg.charAt(0) == '@') 
                result.add(arg.substring(1));
            else {
                Formula argF = new Formula();
                argF.read(arg);
                if (argF.listP()) 
                    result.addAll(argF.findRowVars());
            }
            f.read(f.cdr());
        }
        return result;
    }

    /** ***************************************************************
     * A TreeSet of the relations Vampire enforces to be strictly
     * binary.
     */
    private static TreeSet BINARIES = null;

    /** ***************************************************************
     * An array of the relations Vampire enforces to be strictly
     * binary.
     */
    private static String[] STRICT_BINARIES =
    { "equal", 
      "MultiplicationFn", "DivisionFn", "AdditionFn", "SubtractionFn",
      "greaterThan", "greaterThanOrEqualTo", "lessThan", "lessThanOrEqualTo" };

    /** ***************************************************************
     * @return Returns true if theFormula contains one of the
     * relations that Vampire expects to be strictly binary, else
     * returns false.
     */
    private boolean containsStrictBinary() {
	boolean ans = false;
	try {
	    String fStr = this.theFormula;
	    if ( isNonEmptyString(fStr) ) {
		for ( int i = 0 ; i < STRICT_BINARIES.length; i++ ) {
		    if ( fStr.indexOf(STRICT_BINARIES[i]) >= 0 ) {
			return true;
		    }
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return ans;
    }

    /** ***************************************************************
     *
     * @return true if this Formula contains a literal of length != 3
     * and the predicate is a strict binary, else false.
     */
    private boolean hasMalformedBinary() {
	boolean ans = false;
	try {

	    if ( this.containsStrictBinary() ) {

		if ( BINARIES == null ) {
		    BINARIES = new TreeSet( Arrays.asList(STRICT_BINARIES) );
		}

		ArrayList clauseData = this.toNegAndPosLitsWithRenameInfo();
		if ( !( (clauseData instanceof ArrayList) 
			&& (clauseData.size() > 2) ) ) {
		    return ans;
		}

		ArrayList clauses = (ArrayList) clauseData.get( 0 );
		if ( !(clauses instanceof ArrayList) || clauses.isEmpty() ) {
		    return ans;
		}

		Iterator it = clauses.iterator();
		while ( it.hasNext() ) {
		    ArrayList clause = (ArrayList) it.next();

		    if ( (clause != null) && !(clause.isEmpty()) ) {
 
			ArrayList literals = (ArrayList) clause.get( 0 );
			// Add the pos lits to the neg lits.
			literals.addAll((ArrayList)clause.get(1));
			Iterator it2 = literals.iterator();
			while ( it2.hasNext() ) {
			    Formula litF = (Formula) it2.next();
			    ArrayList litArr = litF.literalToArrayList();
			    if ( !(litArr.isEmpty())
				 && BINARIES.contains((String)litArr.get(0))
				 && (litArr.size() != 3) ) {
				return true;
			    }
			}
		    }
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return ans;
    }

    /** ***************************************************************
     * This method filters a list of formulas, removing those that
     * contain literals Vampire would consider to be malformed because
     * they have the wrong number of arguments for strictly enforced
     * binary relations.  NOTE: this really filters out only malformed
     * predicate literals, not malformed function terms.
     *
     * @param formulas A List of Formula objects.
     *
     * @return true if this Formula contains a literal of length != 3
     * and the predicate is a strict binary, else false.
     */
    private static ArrayList filterForMalformedBinaries(List formulas) {
	ArrayList ans = new ArrayList();
	try {
	    if ( formulas instanceof List ) {
		Iterator it = formulas.iterator();
		while ( it.hasNext() ) {
		    Formula f = (Formula) it.next();
		    if ( ! f.hasMalformedBinary() ) {
			ans.add( f );
		    }
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return ans;
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
     * @return an ArrayList of Formulas
     */
    protected ArrayList expandRowVars(KB kb) {

	String input = this.theFormula;
        TreeSet rowVars = this.findRowVars();
        StringBuffer result = new StringBuffer(input);
        ArrayList resultList = new ArrayList();
        Iterator it = rowVars.iterator();
        if (!it.hasNext()) {
            Formula f = new Formula();
            f.read(input);
            resultList.add(f);
            return resultList;
        }
        else {
	    // Iterate through the row variables
            while (it.hasNext()) {
                String row = (String) it.next();
		int[] range = this.getRowVarExpansionRange(kb, row);
		boolean hasVariableArityRelation = (range[0] == 0);
		range[1] = adjustExpansionCount( hasVariableArityRelation, range[1], row );
                StringBuffer rowResult = new StringBuffer();
                StringBuffer rowReplace = new StringBuffer();
                for ( int j = 1 ; j < range[1] ; j++ ) {
                    if (rowReplace.toString().length() > 0) {
                        rowReplace = rowReplace.append(" ");
                    }
                    rowReplace = rowReplace.append("\\?" + row + (new Integer(j)).toString());
		    if ( hasVariableArityRelation ) {
			rowResult = rowResult.append(result.toString().replaceAll("\\@" + row, rowReplace.toString()) + "\n");
		    }
                }
		if ( ! hasVariableArityRelation ) {
		    rowResult = rowResult.append(result.toString().replaceAll("\\@" + row, rowReplace.toString()) + "\n");
		}
                result = new StringBuffer(rowResult.toString());
            }
        }
	// Copy the source file information for each expanded formula.
        ArrayList al = parseList(result.toString());
        ArrayList newList = new ArrayList();
        for (int i = 0; i < al.size(); i++) {
            Formula f = this.copy();
            f.theFormula = ((Formula) al.get(i)).theFormula;
            newList.add(f);
            //System.out.println("INFO in Formula.expandRowVars(): Adding formula : " + f);
        }
        return newList;
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
     *
     */
    private int adjustExpansionCount(boolean variableArity, int count, String var) {
	// System.out.println( "INFO in adjustExpansionCount( " + variableArity + ", " + count + ", " + var + " )" );
	int revisedCount = count;
	try {
	    if ( isNonEmptyString(var) ) {
		String rowVar = var;
		if ( ! var.startsWith("@") ) {
		    rowVar = ( "@" + var );
		}
		List accumulator = new ArrayList();
		List working = new ArrayList();
		if ( this.listP() && !this.empty() ) {
		    accumulator.add( this );
		}
		while ( !(accumulator.isEmpty()) ) {
		    working.clear();
		    working.addAll( accumulator );
		    accumulator.clear();
		    for ( int i = 0 ; i < working.size() ; i++ ) {
			Formula f = (Formula) working.get( i );
			List literal = f.literalToArrayList();

			// System.out.println( literal );

			int len = literal.size();
			if ( literal.contains(rowVar) && !(isVariable(f.car())) ) {
			    if ( !variableArity && (len > 2) ) {
				revisedCount = ( count - (len - 2) );
			    }
			    else if ( variableArity ) {
				revisedCount = (10 - len);
			    }
			}
			if ( revisedCount < 2 ) {
			    revisedCount = 2;
			}
			while ( !(f.empty()) ) {
			    String arg = f.car();
			    Formula argF = new Formula();
			    argF.read( arg );
			    if ( argF.listP() && !(argF.empty()) ) {
				accumulator.add( argF );
			    }
			    f = f.cdrAsFormula();
			}
		    }
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	// System.out.println( "        => " + revisedCount );
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
     *
     */
    private int[] getRowVarExpansionRange(KB kb, String rowVar) {
	int[] ans = new int[2];
	ans[0] = 1;
	ans[1] = 8;
	try {
	    if ( isNonEmptyString(rowVar) ) {
		String var = rowVar;
		if ( ! var.startsWith("@") ) {
		    var = "@" + var;
		}
		Map minMaxMap = this.getRowVarsMinMax( kb );
		int[] newArr = (int[]) minMaxMap.get( var );
		if ( newArr != null ) {
		    ans = newArr;
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	// System.out.print( "INFO in getRowVarExpansionRange( " + this + ", " + kb + ", " + rowVar + " )" );
	// System.out.println( " => [" + ans[0] + "," + ans[1] + "]" );
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
     *
     */
    private Map getRowVarsMinMax(KB kb) {
	Map ans = new HashMap();
	try {
	    ArrayList clauseData = this.toNegAndPosLitsWithRenameInfo();
	    if ( !( (clauseData instanceof ArrayList) 
		    && (clauseData.size() > 2) ) ) {
		return ans;
	    }
	    /*
	      System.out.println();
	      System.out.println( "clauseData == " + clauseData );
	      System.out.println();
	    */
	    ArrayList clauses = (ArrayList) clauseData.get( 0 );
	    /*
	      System.out.println();
	      System.out.println( "clauses == " + clauses );
	      System.out.println( "clauses.size() == " + clauses.size() );
	      System.out.println();
	    */
	    if ( !(clauses instanceof ArrayList) || clauses.isEmpty() ) {
		return ans;
	    }

	    Map varMap = (Map) clauseData.get( 2 );
	    Map rowVarRelns = new HashMap();
	    for ( int i = 0 ; i < clauses.size() ; i++ ) {
		ArrayList clause = (ArrayList) clauses.get( i );

		// System.out.println( "clause == " + clause );

		if ( (clause != null) && !(clause.isEmpty()) ) {
 
		    // First we get the neg lits.  It may be that
		    // we should use *only* the neg lits for this
		    // task, but we will start by combining the neg
		    // lits and pos lits into one list of literals
		    // and see how that works.
		    ArrayList literals = (ArrayList) clause.get( 0 );
		    ArrayList posLits = (ArrayList) clause.get( 1 );
		    literals.addAll( posLits );
		    for ( int j = 0 ; j < literals.size() ; j++ ) {
			Formula litF = (Formula) literals.get( j );
			litF.getRowVarsWithRelations_1( rowVarRelns );
		    }
		}

		// System.out.println( "rowVarRelns == " + rowVarRelns );

		if ( !(rowVarRelns.isEmpty()) ) {
		    Iterator kit = rowVarRelns.keySet().iterator();
		    while ( kit.hasNext() ) {
			String rowVar = (String) kit.next();
			String origRowVar = getOriginalVar( rowVar, varMap );
			int[] minMax = (int[]) ans.get( origRowVar );
			if ( minMax == null ) {
			    minMax = new int[ 2 ];
			    minMax[0] = 0;
			    minMax[1] = 8;
			    ans.put( origRowVar, minMax );
			}
			TreeSet val = (TreeSet) rowVarRelns.get( rowVar );
			Iterator vit = val.iterator();
			while ( vit.hasNext() ) {
			    String reln = (String) vit.next();
			    int arity = kb.getValence( reln );
			    if ( arity < 1 ) {
				// It's a VariableArityRelation or we
				// can't find an arity, do nothing.
				;
			    }
			    else {
				minMax[0] = 1;
				if ( (arity + 1) < minMax[1] ) {
				    minMax[1] = (arity + 1);
				}
			    }
			    /*
			      System.out.print( "minMax == [ " );
			      for ( int j = 0 ; j < minMax.length ; j++ ) {
			      if ( j > 0 ) {
			      System.out.print( ", " );
			      }
			      System.out.print( minMax[j] );
			      }
			      System.out.println( " ]" );
			    */
			}
		    }
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	// System.out.println( "INFO in getRowVarsMinMax( " + kb + " ) => " + ans );
	return ans;
    }

    /** ***************************************************************
     * Finds all the row variables in a literal that occur with a relation
     * that might have a specific arity.
     *
     * @return A Map containing row var data for this literal.  The keys
     * are row variables (Strings) and the values are TreeSets containing
     * relations (Strings) that might help to constrain the row var during
     * row var expansion.
     */
    protected Map getRowVarsWithRelations() {
	Map varsToRelns = new HashMap();
	getRowVarsWithRelations_1( varsToRelns );
	return varsToRelns;
    }

    /** ***************************************************************
     * Finds all the row variables in a literal that occur with a relation
     * that might have a specific arity.
     *
     * @see getRowVarsWithRelations()
     *
     * @param varsToRelns A Map for accumulating row var data for one
     * literal.  The keys are row variables (Strings) and the values are
     * TreeSets containing relations (Strings) that might help to constrain
     * the row var during row var expansion.
     * 
     * @return void
     * */
    protected void getRowVarsWithRelations_1(Map varsToRelns) {
	try {
	    Formula f = this;
	    if ( f.listP() && !(f.empty()) ) {
		String relation = f.car();
		if ( ! (isVariable(relation) || relation.equals("SkFn")) ) {
		    Formula newF = f.cdrAsFormula();
		    while ( newF.listP() && !(newF.empty()) ) {
			String term = newF.car();
			if ( term.startsWith("@") ) {
			    TreeSet relns = (TreeSet) varsToRelns.get( term );
			    if ( relns == null ) {
				relns = new TreeSet();
				varsToRelns.put( term, relns );
			    }
			    relns.add( relation );
			}
			else {
			    Formula termF = new Formula();
			    termF.read( term );
			    termF.getRowVarsWithRelations_1( varsToRelns );
			}
			newF = newF.cdrAsFormula();
		    }
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return;
    }

    /** ***************************************************************
     * This method finds the original variable that corresponds to a new
     * variable.  Note that the clausification algorithm has two variable
     * renaming steps, and that after variables are standardized apart an
     * original variable might correspond to multiple clause variables.
     *
     * @param var A SUO-KIF variable (String)
     *
     * @param varMap A Map (graph) of successive new to old variable
     * correspondences.
     * 
     * @return The original SUO-KIF variable corresponding to the input.
     *
     **/
    private static String getOriginalVar(String var, Map varMap) {

	// System.out.println( "INFO in getOriginalVar( " + var + ", " + varMap + " )" );
	String ans = null;
	try {
	    if ( isNonEmptyString(var) && (varMap instanceof Map) ) {
		ans = var;
		String next = (String) varMap.get( ans );
		while ( (next != null) && !(next.equals(ans)) ) {
		    ans = next;
		    next = (String) varMap.get( ans );	    
		}
		if ( ans == null ) {
		    ans = var;
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	// System.out.println( "-> " + ans );
	return ans;
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
    private boolean isFunctionalTerm() {

        if (!listP()) 
            return false;
        String pred = car();
        if (pred.length() >= 2 && pred.substring(pred.length()-2).compareTo("Fn") == 0) 
            return true;
        return false;
    }

    /** ***************************************************************
     * Test whether a String is a variable
     */
    private boolean isVariable(String term) {

        if (listP(term)) 
            return false;
        if (term.charAt(0) == '@' || term.charAt(0) == '?') 
            return true;        
        return false;
    }

    /** ***************************************************************
     * Test whether this Formula is a rule
     */
    private boolean isRule() {

        if ( car().equals("=>") )
            return true;        
        return false;
    }

    /** ***************************************************************
     * Test whether a list with a predicate is a quantifier list
     */
    private boolean isQuantifierList(String listPred, String previousPred) {

        if ((previousPred.equals("exists") || previousPred.equals("forall")) &&
             (listPred.charAt(0) == '@' || listPred.charAt(0) == '?'))
            return true;        
        return false;
    }

    /** ***************************************************************
     * Test whether a predicate is a logical quantifier
     */
    private boolean isQuantifier(String pred) {

	return ( isNonEmptyString(pred)
		 && ( pred.equals("exists") 
		      || pred.equals("forall") ) );
    }

    /** ***************************************************************
     *
     * A static utility method.
     *
     * @param obj Any object, but should be a String.
     *
     * @return true if obj is a SUO-KIF commutative logical operator,
     * else false.
     *
     */
    public static boolean isCommutative(String obj) {

	return ( isNonEmptyString(obj)
		 && ( obj.equals("and") 
		      || obj.equals("or") ) );
    }

    /** ***************************************************************
     * Test whether a predicate is a logical operator
     */
    private boolean isLogicalOperator(String pred) {

        String[] logOps = {"and", "or", "not", "=>", "<=>", "forall", "exists", "holds"};
        ArrayList logicalOperators = new ArrayList(Arrays.asList(logOps));
        if (logicalOperators.contains(pred))
            return true;        
        return false;
    }

    /** ***************************************************************
     *
     * @param obj Any object
     *
     * @return true if obj is a non-empty String, else false.
     *
     */
    public static boolean isNonEmptyString(Object obj) {
	return ( (obj instanceof String) && (((String) obj).length() > 0) );
    }

    /** ***************************************************************
     *
     * @return An ArrayList (ordered tuple) representation of the
     * Formula, in which each top-level element of the Formula is a
     * String occupying one cell.  Elements can be lists or atoms.
     *
     */
    public ArrayList literalToArrayList() {
	ArrayList tuple = new ArrayList();
	Formula f = this;
	if ( f.listP() ) {
	    while ( !(f.empty()) ) {
		tuple.add( f.car() );
		f = f.cdrAsFormula();
	    }
	}
	return tuple;
    }

    /** ***************************************************************
     * This method returns all SUO-KIF variables that occur in the
     * Formula.
     *
     * @see gatherVariables_1(TreeSet accumulator)
     * 
     * @return A TreeSet containing variables (Strings), or an empty
     * TreeSet if no variables can be found.
     */
    private TreeSet gatherVariables() {
	TreeSet accumulator = new TreeSet();
	this.gatherVariables_1( accumulator );
	return accumulator;
    }

    /** ***************************************************************
     * @see gatherVariables()
     *
     * @param accumulator A TreeSet used for storing variables
     * (Strings).
     * 
     * @return void
     */
    private void gatherVariables_1(TreeSet accumulator) {
	try {
	    if ( isNonEmptyString(this.theFormula) ) {
		if ( this.listP() && !(this.empty()) ) {
		    String arg0 = this.car();
		    Formula arg0F = new Formula();
		    arg0F.read( arg0 );
		    arg0F.gatherVariables_1( accumulator );
		    this.cdrAsFormula().gatherVariables_1( accumulator );
		}
		if ( isVariable(this.theFormula) ) {
		    accumulator.add( this.theFormula );
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return;
    } 

    /** ***************************************************************
     */
    private String[] addToTypeList(String pred, ArrayList al, String[] result, String classP) {

	/*
        System.out.print( "INFO in addToTypeList( " + this + ", " + pred + ", " + al + ", [" );
	if ( (result != null) && (result.length > 0) ) {
	    for ( int j = 0 ; j < result.length ; j++ ) {
		if ( j > 0 ) { System.out.print(","); }
		System.out.print( result[j] );
	    }
	}
	System.out.println( "], \"" + classP + "\" )" );
	*/

        for (int i = 0; i < al.size(); i++) {

            Formula f = (Formula) al.get(i);
            //System.out.println("INFO in addToTypeList(): formula: " + f.theFormula);
            String cl = f.getArgument(3);
            String argnum;
            if (f.theFormula.startsWith("(range"))
                argnum = "0";
            else
                argnum = f.getArgument(2);
            int num;
            try {                
                num = Integer.valueOf(argnum).intValue();
            } catch (NumberFormatException nfe) {
                KBmanager.getMgr().setError(KBmanager.getMgr().getError() + 
                                            "\nBad argument type declaration in formula " + f.theFormula +
                                            " in file " + f.sourceFile + " at line " + f.startLine + "\n<br>");
                return result;
            }
            if (result[num] == null || result[num] == "")
                result[num] = cl + classP;
            else {
                KBmanager.getMgr().setError(KBmanager.getMgr().getError() + 
                                            "\nConflicting type declarations found for predicate " + pred +
                                            " at argument " + argnum + " with class " + cl + " and existing argument " +
                                            result[num] + "\n<br>");
                //System.out.println(KBmanager.getMgr().getError() + 
                //                            "\nConflicting type declarations found for predicate " + pred +
                //                            " at argument " + argnum + " with class " + cl + " and existing argument " +
                //                            result[num]);
            }
        }
        return result;
    }

    /** ***************************************************************
     * A + is appended to the type if the parameter must be a class
     * @return the type for each argument to the given predicate, where
     * ArrayList element 0 is the result, if a function, 1 is the 
     * first argument, 2 is the second etc.
     */
    private ArrayList getTypeList(String pred, KB kb) {

        //System.out.println("INFO in Formula.getTypeList(): pred: " + pred);
        String[] r = new String[20];
        ArrayList result = new ArrayList();
        ArrayList al = kb.askWithRestriction(0,"domain",1,pred);
        ArrayList al2 = kb.askWithRestriction(0,"domainSubclass",1,pred);
        ArrayList al3 = kb.askWithRestriction(0,"range",1,pred);
        ArrayList al4 = kb.askWithRestriction(0,"rangeSubclass",1,pred);
        r = addToTypeList(pred,al,r,"");
        r = addToTypeList(pred,al2,r,"+");
        r = addToTypeList(pred,al3,r,"");
        r = addToTypeList(pred,al4,r,"+");
        for (int i = 0; i < r.length; i++) 
            result.add(r[i]);
        return result;
    }

    /** ***************************************************************
     * Find the argument type restriction for a given predicate and 
     * argument number that is inherited from one of its super-relations.
     * A "+" is appended to the type if the parameter must be a class.
     * argument number 0 is used for the return type of a Function.
     * Functions are not currently handled however.
     */
    private String findType(ArrayList types, int numarg, String pred, KB kb) {

        //System.out.println("INFO in Formula.findType(): pred: " + pred);
        boolean found = false;
        String newPred = pred;
        while (!found) {
            ArrayList parents = kb.askWithRestriction(0,"subrelation",1,newPred);
            if (parents == null || parents.size() == 0) 
                return "";            
            String parent = newPred;
            for (int i = 0; i < parents.size(); i++) {
                Formula f = (Formula) parents.get(i);
                parent = f.getArgument(2);
                ArrayList axioms = kb.askWithRestriction(0,"domain",1,parent);
                for (int j = 0; j < axioms.size(); j++) {
                    f = (Formula) axioms.get(j);
                    int argnum = Integer.valueOf(f.getArgument(2)).intValue();
                    if (argnum == numarg) {
                        found = true;
                        return f.getArgument(3);
                    }
                }
                axioms = kb.askWithRestriction(0,"domainSubclass",1,parent);
                for (int j = 0; j < axioms.size(); j++) {
                    f = (Formula) axioms.get(j);
                    int argnum = Integer.valueOf(f.getArgument(2)).intValue();
                    if (argnum == numarg) {
                        found = true;
                        return f.getArgument(3) + "+";
                    }
                }
            }
            newPred = parent;
        }
        return "";
    }


    /** ***************************************************************
     * Does most of the real work for addTypeRestrictions() by
     * recursing through the formula, collecting variables, finding
     * their type restrictions, and adding that information to the
     * HashMap result
     *
     * @return a HashMap keys are var names values are types. A "+" 
     * is appended to the type if the parameter must be a class
     * Note this routine does not yet properly handle function return types.
     */
    private HashMap addTypeRestrictionsRecurse(String theFormula, KB kb) {

        //System.out.println("INFO in Formula.addTypeRestrictionsRecurse(): formula: " + theFormula);
        HashMap varmap = new HashMap(); 
        Formula f = new Formula();
        f.read(theFormula);
        if (!f.listP()) 
            return varmap;
        String pred = f.car();
        ArrayList types = new ArrayList();
        if (!isLogicalOperator(pred)) 
            types = getTypeList(pred,kb);
        f.read(f.cdr());
        int numarg = 0;
        while (!f.empty()) {
            numarg++;
            String arg = f.car();
            if (listP(arg)) 
                varmap.putAll(addTypeRestrictionsRecurse(arg,kb));
            else {
                if (isVariable(arg) && !isLogicalOperator(pred)) {
                    String type;
                    if (numarg > types.size()) 
                        type = findType(types,numarg,pred,kb);
                    type = (String) types.get(numarg);
                    if (type == null) 
                        type = findType(types,numarg,pred,kb);
                    if (type != null && type != "") 
                        varmap.put(arg,type);
                }                
            }
            f.read(f.cdr());
        }
	/*
	if ( theFormula.startsWith("(=>")
	     || theFormula.startsWith("(<=>") ) {
	    System.out.println( "INFO in addTypeRestrictionsRecurse( " + theFormula + " )" );
	    System.out.println( "varmap == " + varmap );
	}
	*/

        return varmap;
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

        Formula f = new Formula();
        //System.out.println("INFO in Formula.addTypeRestrictions(): original formula: " + toString());
        f.read(theFormula);
        f.read(f.cdr());
        String antecedent = f.car();
        f.read(antecedent);

        HashMap varmap = new HashMap(); // keys are var names values are types
                                        // a + is appended to the type if the parameter must be a class
        varmap = addTypeRestrictionsRecurse(f.theFormula,kb);
        StringBuffer form = new StringBuffer();
        if (varmap.keySet().size() > 0) {
            form.append("(=>");
            if (varmap.keySet().size() > 1) 
                form.append(" (and");
            Iterator it = varmap.keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                String value = (String) varmap.get(key);
                String relation = "instance";
                if (value.endsWith("+")) { 
                    relation = "subclass";
                    value = value.substring(0,value.length()-1);
                }
                form.append(" (" + relation + " " + key + " " + value + ")");
            }
            if (varmap.keySet().size() > 1) 
                form.append(")");
            form.append( " " + theFormula);
            form.append(")");

            f.read(form.toString());

	    /*
	    if ( this.theFormula.startsWith("(=>")
		 || this.theFormula.startsWith("(<=>") ) {
		System.out.println( "INFO in addTypeRestrictions( " + this + " )" );
		System.out.println( "-> " + form.toString() );
	    }
	    */

            return form.toString();
        }

	/*
	if ( this.theFormula.startsWith("(=>")
	     || this.theFormula.startsWith("(<=>") ) {
	    System.out.println( "INFO in addTypeRestrictions( " + this + " )" );
	    System.out.println( "-> " + theFormula );
	}
	*/

        return theFormula;
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
	/*
	System.out.println( "INFO in preProcessRecurse( "
			    + f
			    + ", " 
			    + previousPred
			    + ", "
			    + ignoreStrings
			    + ", "
			    + translateIneq
			    + ", "
			    + translateMath
			    + " )" );
	*/

	boolean addHoldsPrefix = KBmanager.getMgr().getPref("holdsPrefix").equals("yes");

        String[] logOps = {"and", "or", "not", "=>", "<=>", "forall", "exists"};
        String[] matOps = {"equal", "AdditionFn", "SubtractionFn", "MultiplicationFn", "DivisionFn"};
        String[] compOps = {"greaterThan", "greaterThanOrEqualTo", "lessThan", "lessThanOrEqualTo"};
        ArrayList logicalOperators = new ArrayList(Arrays.asList(logOps));
        ArrayList mathOperators = new ArrayList(Arrays.asList(matOps));
        ArrayList comparisonOperators = new ArrayList(Arrays.asList(compOps));

        StringBuffer result = new StringBuffer();
        if (f.theFormula == "" || !f.listP() || f.atom() || f.empty()) return "";
        String pred = f.car();
        Formula predF = new Formula();
        predF.read(pred);
        String prefix = "";

	if ( addHoldsPrefix ) {
	    if ( !logicalOperators.contains(pred) && !isQuantifierList(pred,previousPred) ) {
		prefix = "holds_";    
	    }    
	    if ( f.isFunctionalTerm() ) {
		prefix = "apply_";  
	    }
	}

        String rest = f.cdr();
        Formula restF = new Formula();
        restF.read(rest);
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
                if (!logicalOperators.contains(pred) &&
                    !comparisonOperators.contains(pred) &&
                    !mathOperators.contains(pred) &&
                    !argF.isFunctionalTerm()) {
                    result.append("`");                     
                }
		result.append(res);
            }
            else
                result.append(" " + arg);
            restF.theFormula = restF.cdr();
        }

        if ( addHoldsPrefix ) {
	    if (pred.equals("holds")) {
		pred = "";
		argCount--;
		prefix = prefix + argCount + "__ ";
	    }
	    else
		if (!logicalOperators.contains(pred) && !isQuantifierList(pred,previousPred) && 
		    !mathOperators.contains(pred) && 
		    !comparisonOperators.contains(pred)) 
		    prefix = prefix + argCount + "__ ";
		else 
		    prefix = "";
	}
        
        return "(" + prefix + pred + result + ")";
    }

    /** ***************************************************************
     * Pre-process a formula before sending it to the theorem prover. This includes
     * ignoring meta-knowledge like documentation strings, translating
     * mathematical operators, quoting higher-order formulas, expanding
     * row variables and prepending the 'holds__' predicate.
     * @param query controls whether to add universal or existential
     * quantification.  If true, add existential.
     * @return an ArrayList of Formula(s)
     */
    public ArrayList preProcess(boolean query, KB kb) {

        ArrayList allResults = new ArrayList();

        boolean ignoreStrings = false;
        boolean translateIneq = true;
        boolean translateMath = true;

        if (theFormula == null || theFormula == "") 
            return allResults;

        Formula f = new Formula();
        f.read(theFormula);
        if (f.isRule() && KBmanager.getMgr().getPref("typePrefix").equals("yes"))
            f.read(addTypeRestrictions(kb));

	boolean addHoldsPrefix = KBmanager.getMgr().getPref("holdsPrefix").equals("yes");

	// Do pred var instantiations if we are not adding holds
	// prefixes.
	ArrayList predVarInstantiations = new ArrayList();
	if ( ! addHoldsPrefix ) {
	    predVarInstantiations.addAll( f.instantiatePredVars(kb) );
	}

	// If the list of pred var instatiations is empty, add the
	// original formula to the list for further processing below.
	if ( predVarInstantiations.isEmpty() ) {
	    predVarInstantiations.add( f );
	}
	// If the formula contains a pred var that can't be
	// instantiated, return an empty results list.
	else if ( predVarInstantiations.size() == 1 ) {
	    Object obj = predVarInstantiations.get( 0 );
	    if ( isNonEmptyString(obj) && ((String) obj).equalsIgnoreCase("reject") ) {
		return allResults;
	    }
	}

	// AB: I'm not sure what the call to makeQuantifiersExplicit,
	// below, is for.  The return value is not used, and it is
	// being invoked on this, not on f.  I'm wondering if the call
	// was meant to be f.makeQuantifiersExplicit(query), and if it
	// should now be called on every member of allResults.
	makeQuantifiersExplicit(query);

	// Iterate over the instantiated predicate formulas, doing row
	// var expansion on each.  If no predicate instantiations can
	// be generated, the ArrayList predVarInstantiations will
	// contain just f (the original input formula).
	int pviN = predVarInstantiations.size();
	if ( pviN < AXIOM_EXPANSION_LIMIT ) {
	    for ( int i = 0 ; i < pviN ; i++ ) {
		Formula ipvF = (Formula) predVarInstantiations.get( i );
		ArrayList rowVarExpansions = ipvF.expandRowVars(kb);
		allResults.addAll( rowVarExpansions );
		if ( allResults.size() > AXIOM_EXPANSION_LIMIT ) {
		    break;
		}
	    }
	}

	// If we are not adding the holds prefixes, filter out
	// literals Vampire will reject.
	if ( ! addHoldsPrefix ) {
	    allResults = filterForMalformedBinaries( allResults );
	}

	// Iterate over the formulas resulting from row var expansion,
	// passing each to preProcessRecurse for further processing.
	Iterator it = allResults.iterator();
	while ( it.hasNext() ) {
	    Formula fnew = (Formula) it.next();
	    String theNewFormula = preProcessRecurse( fnew,
						      new String(),
						      ignoreStrings,
						      translateIneq,
						      translateMath );
	    fnew.read( theNewFormula );
	}

	/*
	int totalN = allResults.size();
	if ( totalN > 1 ) {
	    pviN = predVarInstantiations.size();
	    System.out.println( "INFO in Formula.preProcess( " + this + ", " + query + ", " + kb + " )" );
	    System.out.println( "  " 
				+ pviN
				+ " formula"
				+ ( pviN == 1 ? "" : "s" )
				+ " after pred var instantiation" );
	    System.out.println( "  " + totalN + " formulas after row var expansion" );
	    if ( totalN > AXIOM_EXPANSION_LIMIT ) {
		System.out.println( "AXIOM_EXPANSION_LIMIT, " + AXIOM_EXPANSION_LIMIT + ", exceeded" );
	    }
	}
	*/

        return allResults;
    }

    /** ***************************************************************
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

    /** ***************************************************************
     * Remove the 'holds' prefix wherever it appears.
     */
    public static String postProcess(String s) {

        s = s.replaceAll("holds_\\d__ ","");
        s = s.replaceAll("apply_\\d__ ","");        
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

        boolean inQuantifier = false;
        StringBuffer token = new StringBuffer();
        StringBuffer formatted = new StringBuffer();
        int indentLevel = 0;
        boolean inToken = false;
        boolean inVariable = false;
        boolean inVarlist = false;
        boolean inComment = false;

	if ( isNonEmptyString(theFormula) ) {
	    theFormula = theFormula.trim();
	}

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
     * Format a formula as a prolog statement.  Note that only tuples
     * are converted properly at this time.  Statements with any embedded
     * formulas or functions will be rejected with a null return.
     */
    public String toProlog() {

        if (!listP()) { 
            System.out.println("INFO in Fomula.toProlog(): Not a formula: " + theFormula);
            return "";
        }
        if (empty()) { 
            System.out.println("INFO in Fomula.toProlog(): Empty formula: " + theFormula);
            return "";
        }
        StringBuffer result = new StringBuffer();
        String relation = car();
        Formula f = new Formula();
        f.theFormula = cdr();
        if (!Formula.atom(relation)) { 
            System.out.println("INFO in Fomula.toProlog(): Relation not an atom: " + relation);
            return "";
        }
        result.append(relation + "('");
        while (!f.empty()) {
            String arg = f.car();
            f.theFormula = f.cdr();
            if (!Formula.atom(arg)) { 
                System.out.println("INFO in Fomula.toProlog(): Argument not an atom: " + arg);
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

  /** ***************************************************************
   * Convert the logical operators and inequalities in SUO-KIF to 
   * their TPTP equivalents
   * @param st is the StreamTokenizer_s that contains the current token
   * @return the String that is the translated token
   */
  private String translateWord(StreamTokenizer_s st,boolean hasArguments) {

      int translateIndex;

      String kifOps[] = {"forall", "exists", "not", "and", "or", "=>", "<=>"};
      String tptpOps[] = {"! ", "? ", "~ ", " & ", " | " ," => " , " <=> "};

      String kifPredicates[] = {"equal","<=","<",">",">=",
          "lessThanOrEqualTo","lessThan","greaterThan","greaterThanOrEqualTo"};
      String tptpPredicates[] = {"equal","lesseq","less","greater","greatereq",
          "lesseq","less","greater","greatereq"};

      String kifFunctions[] = {"MultiplicationFn", "DivisionFn", "AdditionFn",
          "SubtractionFn" };
      String tptpFunctions[] = {"times","divide","plus","minus"};

      //DEBUG System.out.println("Translating word " + st.sval + " with hasArguments " + hasArguments);

      //----Places double quotes around strings, and replace \n by space
        if (st.ttype == 34) {
            return("\"" + st.sval.replaceAll("[\n\t\r\f]"," ") + "\"");
        }
        //----Fix variables
      if (st.sval.charAt(0) == '?' || st.sval.charAt(0) == '@') {
          return((Character.toUpperCase(st.sval.charAt(1)) +
                  st.sval.substring(2)).replace('-','_'));
      }
      //----Translate special predicates
      translateIndex = 0; 
      while (translateIndex < kifPredicates.length && 
             !st.sval.equals(kifPredicates[translateIndex])) {
          translateIndex++;
      }
      if (translateIndex < kifPredicates.length) {
          return((hasArguments ? "$" : "") + tptpPredicates[translateIndex]);
      }
      //----Translate special functions
      translateIndex = 0; 
      while (translateIndex < kifFunctions.length && 
             !st.sval.equals(kifFunctions[translateIndex])) {
          translateIndex++;
      }
      if (translateIndex < kifFunctions.length) {
          return((hasArguments ? "$" : "") + tptpFunctions[translateIndex]);
      }
      //----Translate operators
      translateIndex = 0; 
      while (translateIndex < kifOps.length && 
             !st.sval.equals(kifOps[translateIndex])) {
          translateIndex++;
      }
      if (translateIndex < kifOps.length) {
          return(tptpOps[translateIndex]);
      }
      //----Do nothing to numbers 
      if (st.ttype == StreamTokenizer.TT_NUMBER || 
          (st.sval != null && (Character.isDigit(st.sval.charAt(0)) ||
                               (st.sval.charAt(0) == '-' && Character.isDigit(st.sval.charAt(1)))))) {
          return(st.sval);
          //SANITIZE return("n" + st.sval.replace('-','n').replaceAll("[.]","dot"));
      }
      //----Converts leading uppercase to lower case
      return(Character.toLowerCase(st.sval.charAt(0)) + 
             st.sval.substring(1).replace('-','_'));
  }

  /** ***************************************************************
   * @param st is the StreamTokenizer_s that contains the current token
   * for which the arity is desired
   * @return the integer arity of the given logical operator
   */
  private int operatorArity(StreamTokenizer_s st) {

      int translateIndex;
      String kifOps[] = {"forall", "exists", "not", "and", "or", "=>", "<=>"};

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
  private void incrementTOS(Stack countStack) {

      countStack.push(new Integer((Integer)countStack.pop()+1));
  }

  /** ***************************************************************
   * Add the current token, if a variable, to the list of variables
   * @param variables is the list of variables
   */
  private void addVariable(StreamTokenizer_s st,Vector variables) {

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
  public void tptpParse(boolean query, KB kb) throws ParseException, IOException {

      if (kb == null) 
          kb = new KB("",KBmanager.getMgr().getPref("kbDir"));
      
      //System.out.println("INFO in KIF.tptpParse(): formula: " + f.theFormula);
      StreamTokenizer_s st;
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

      theTPTPFormula = null;
      ArrayList parsedForm = null;
      parsedForm = preProcess(query,kb);
      Iterator g = parsedForm.iterator();

      //----Peforms function on each current processed axiom
      while (g.hasNext()) {
          Object next = g.next();
          Formula p = (Formula) next;
          //System.out.println("INFO in KIF.tptpParse(): ##############");
          //System.out.println("INFO in KIF.tptpParse(): " + f.theFormula);
          //System.out.println("INFO in KIF.tptpParse(): --------------");
          //System.out.println("INFO in KIF.tptpParse(): " + p.theFormula);
 
          st = new StreamTokenizer_s(new StringReader(p.theFormula));
          KIF.setupStreamTokenizer(st);

          StringBuffer tptpFormula = new StringBuffer(p.theFormula.length());
          parenLevel = 0;
          countStack.push(0);
          lastWasOpen = false;
          inQuantifierVars = false;
          inHOL = false;
          inHOLCount = 0;

          do {
              st.nextToken();
              //----Open bracket
              if (st.ttype==40) {
                  if (lastWasOpen) {    //----Should not have (( in KIF
                      System.out.println("ERROR: Double open bracket at " + 
                                         tptpFormula);
                      throw new ParseException("Parsing error in " + 
                                               p.theFormula,startLine);
                  }
                  //----Track nesting of ()s for hol__, so I know when to close the '
                  if (inHOL) {
                      inHOLCount++;
                  }
                  lastWasOpen = true;
                  parenLevel++;
                  //----Operators
              } else if (st.ttype == StreamTokenizer.TT_WORD && 
                         (arity = operatorArity(st)) > 0) {
                  //----Operators must be preceded by a (
                  if (!lastWasOpen) {   
                      System.out.println("ERROR: Missing ( before " + 
                                         st.sval + " at " + tptpFormula);
                      return;
                  }
                  //----This is the start of a new term - put in the infix operator if not the
                  //----first term for this operator
                  if ((Integer)(countStack.peek()) > 0) 
                      tptpFormula.append((String)operatorStack.peek()); 
                  //----If this is the start of a hol__ situation, quote it all
                  if (inHOL && inHOLCount == 1) 
                      tptpFormula.append("'");
                  //----()s around all operator expressions
                  tptpFormula.append("(");      
                  //----Output unary as prefix
                  if (arity == 1) {
                      tptpFormula.append(translateWord(st,false));
                      //----Note the new operator (dummy) with 0 operands so far
                      countStack.push(new Integer(0));
                      operatorStack.push(",");
                      //----Check if the next thing will be the quantified variables
                      if (st.sval.equals("forall") || 
                          st.sval.equals("exists")) {
                          inQuantifierVars = true;
                      }
                      //----Binary operator
                  } else if (arity == 2) {
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
                  //----Quote - Term token translation to TPTP
              } else if (st.ttype == 34 ||
                         st.ttype == StreamTokenizer.TT_NUMBER || 
                         (st.sval != null && (Character.isDigit(st.sval.charAt(0)))) ||
                         st.ttype == StreamTokenizer.TT_WORD) {                              
                  //----Start of a predicate or variable list
                  if (lastWasOpen) {
                      //----Variable list
                      if (inQuantifierVars) {
                          tptpFormula.append("[");
                          tptpFormula.append(translateWord(st,false));
                          incrementTOS(countStack);
                          //----Predicate
                      } else {
                          //----This is the start of a new term - put in the infix operator if not the
                          //----first term for this operator
                          if ((Integer)(countStack.peek()) > 0) 
                              tptpFormula.append((String)operatorStack.peek());
                          //----If this is the start of a hol__ situation, quote it all
                          if (inHOL && inHOLCount == 1) 
                              tptpFormula.append("'");
                          //----Predicate of function and (
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
                      if ((Integer)(countStack.peek()) > 0) {
                          tptpFormula.append((String)operatorStack.peek());
                      }
                      //----Output the word
                      tptpFormula.append(translateWord(st,false));
                      //----Increment counter for this level
                      incrementTOS(countStack);
                  }
                  //----Collect variables that are used and quantified
                  if (st.sval.charAt(0) == '?' || st.sval.charAt(0) == '@') {
                      if (inQuantifierVars) {
                          addVariable(st,quantifiedVariables);
                      } else {
                          addVariable(st,allVariables);
                      }
                  }
                  lastWasOpen = false; 
                  //----Close bracket.
              } else if (st.ttype==41) {
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
                          for (index = 0;index < allVariables.size();index++) {
                              if (index > 0) {
                                  quantification += ",";
                              }
                              quantification += 
                                  (String)allVariables.elementAt(index);
                          }
                          quantification += "] : ";
                          tptpFormula.insert(0,"( " + quantification);
                          tptpFormula.append(" )");
                      }
                      //----Appends the final TPTP formula into the formula object and resets 
                      //----variables
                      if (theTPTPFormula == null) {
                          theTPTPFormula = "( " + tptpFormula.toString() + " )";
                      } else {
                          theTPTPFormula += " & ( " + tptpFormula.toString() +
                              " )";
                      }
                      if ((Integer)(countStack.pop()) != 1) {
                          System.out.println(
                              "Error in KIF.tptpParse(): Not one formula");
                      }
                  } else if (parenLevel < 0) {
                      System.out.print("ERROR: Extra closing bracket at " + 
                                       tptpFormula.toString());
                      throw new ParseException("Parsing error in " + 
                                               p.theFormula,startLine);
                  }                  
              } else if (st.ttype != StreamTokenizer.TT_EOF) {
                  System.out.println("ERROR: Illegal character '" +
                                     (char)st.ttype + "' at " + tptpFormula.toString());
                  throw new ParseException("Parsing error in " + 
                                           p.theFormula,startLine);
              }              
          } while (st.ttype != StreamTokenizer.TT_EOF);
      }
  }

    ///////////////////////////////////////////////////////
    /*
      START of instantiatePredVars(KB kb) implementation.
    */
    ///////////////////////////////////////////////////////

    /** ***************************************************************
     * Returns an ArrayList of the Formulas that result from replacing
     * all predicate variables in the input Formula with predicate
     * names.
     *
     * @param kb A KB that is used for processing the Formula.
     * 
     * @return An ArrayList of Formulas, or an empty ArrayList if no
     * instantiations can be generated.
     */
    private ArrayList instantiatePredVars(KB kb) {

	/*
	if ( this.theFormula.startsWith("(=>")
	     || this.theFormula.startsWith("(<=>") ) {
	    System.out.println( "ENTER instantiatePredVars( " + this + " )" );
	}
	*/

	ArrayList ans = new ArrayList();

	try {

	    String fStr = this.theFormula;

	    // First we do a string existence check to see if it is
	    // worth processing the formula.
	    if ( isNonEmptyString(fStr) 
		 && ( (fStr.indexOf("holds") >= 0)
		      || (fStr.indexOf("(?") >= 0)
		      || (fStr.indexOf("@ROW") >= 0)) ) {
		
		// Get all query lits for all pred vars, indexed by
		// var.
		List indexedQueryLits = this.prepareIndexedQueryLiterals();
		// System.out.println( "indexedQueryLits == " + indexedQueryLits );

		if ( indexedQueryLits == null ) {
		    ans.add( this );
		}
		else {

		    List substForms = new ArrayList();
		    List varQueryTuples = null;
		    List substTuples = null;
		    List litsToRemove = null;

		    // First, gather all substitutions.
		    Iterator it1 = indexedQueryLits.iterator();
		    while ( it1.hasNext() ) {
			varQueryTuples = (List) it1.next();
			substTuples = computeSubstitutionTuples( kb, varQueryTuples );
			if ( (substTuples instanceof List) && !(substTuples.isEmpty()) ) {
			    substForms.add( substTuples );
			}
		    }
		    // System.out.println( "substForms == " + substForms );

		    if ( !(substForms.isEmpty()) ) {

			// Try to simplify the Formula.
			Formula f = this;
			it1 = substForms.iterator();
			Iterator it2 = null;
			while ( it1.hasNext() ) {
			    substTuples = (List) it1.next();
			    litsToRemove = (List) substTuples.get( 0 );
			    it2 = litsToRemove.iterator();
			    while ( it2.hasNext() ) {
				List lit = (List) it2.next();
				f = f.maybeRemoveMatchingLits( lit );
			    }
			}

			// System.out.println();
			// System.out.println();
			// System.out.println( "SIMPLIFIED FORMULA: " + f );
			// System.out.println();

			// Now generate pred var instantions from the
			// possibly simplified formula.
			List templates = new ArrayList();
			templates.add( f.theFormula );
			TreeSet accumulator = new TreeSet();

			String template = null;
			String var = null;
			String term = null;
			ArrayList quantVars = null;
			int i = 0;

			// Iterate over all var plus query lits forms, getting
			// a list of substitution literals.
			it1 = substForms.iterator();
			while ( it1.hasNext() ) {
			    substTuples = (List) it1.next();

			    // System.out.println( "substTuples == " + substTuples );

			    if ( (substTuples instanceof List) && !(substTuples.isEmpty()) ) {

				// Iterate over all ground lits ...

				// Remove litsToRemove, which we have
				// already used above.
				litsToRemove = (List) substTuples.remove( 0 );
				// System.out.println( "litsToRemove == " + litsToRemove );

				// Remove and hold the tuple that
				// indicates the variable substitution
				// pattern.
				List varTuple = (List) substTuples.remove( 0 );
				// System.out.println( "varTuple == " + varTuple );

				it2 = substTuples.iterator();
				while ( it2.hasNext() ) {
				    List groundLit = (List) it2.next();
				    // System.out.println( "groundLit == " + groundLit );

				    // Iterate over all formula templates,
				    // substituting terms from each ground lit
				    // for vars in the template.
				    Iterator it3 = templates.iterator();
				    while ( it3.hasNext() ) {
					template = (String) it3.next();
					quantVars = collectQuantifiedVariables( template );
					// System.out.println( "quantVars == " + quantVars );
					for ( i = 0 ; i < varTuple.size() ; i++ ) {
					    var = (String) varTuple.get( i );
					    if ( isVariable(var) ) {
						term = (String) groundLit.get( i );
						// System.out.println( "term == " + term );

						// Don't relace variables that are
						// explicitly quantified.  This check could
						// be avoided by using the clausal
						// form.
						if ( ! quantVars.contains(var) ) {

						    template = template.replace( ("holds " + var), term );
						    template = template.replace( var, term );
						    // System.out.println( "template == " + template );
						}
					    }
					}
					accumulator.add( template );
					// System.out.println( "accumulator == " + accumulator );
				    }
				}
				templates.clear();
				templates.addAll( accumulator );
				// System.out.println( "templates == " + templates );
				accumulator.clear();
			    }
			}
			ans.addAll( KB.stringsToFormulas(templates) );
			if ( ans.isEmpty() ) {
			    ans.add( "reject" );
			}
			// System.out.println( "ans == " + ans );
		    }
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}

	/*
	if ( this.theFormula.startsWith("(=>")
	     || this.theFormula.startsWith("(<=>") ) {

	    System.out.println( "EXIT instantiatePredVars( " + this + " )" );
	    if ( ans.isEmpty() ) {
		System.out.println( "-> " + ans );
	    }
	    else {
		System.out.println( "-> " );
		Iterator it = ans.iterator();
		for ( int i = 1 ; it.hasNext() ; i++ ) {
		    System.out.println( i + ": " + ((Formula) it.next()) );
		}
	    }
	}
 	*/

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
	if ( queryLiteral instanceof List ) {
	    String term = null;
	    for ( int i = 0 ; i < queryLiteral.size() ; i++ ) {
		term = (String) queryLiteral.get( i );
		if ( term.startsWith("?") ) {
		    ans++;
		}
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
     * @return An ArrayList of literals, or an empty ArrayList of no
     * query answers can be found.
     */
    private static ArrayList computeSubstitutionTuples(KB kb, List queryLits) {

	// System.out.println( "ENTER computeSubstitutionTuples( " + kb + ", " + queryLits + " )" );
	ArrayList result = new ArrayList();
	try {
	    if ( (kb instanceof KB) 
		 && (queryLits instanceof List)
		 && !(queryLits.isEmpty()) ) {

		String idxVar = (String) queryLits.get( 0 );

		int i = 0;
		int j = 0;

		// Sort the query lits by number of variables.
		ArrayList sortedQLits = new ArrayList();
		for ( i = 1 ; i < queryLits.size() ; i++ ) {
		    ArrayList ql = (ArrayList) queryLits.get( i );
		    int varCount = getVarCount( ql );
		    boolean added = false;
		    for ( j = 0 ; j < sortedQLits.size() ; j++ ) {
			ArrayList ql2 = (ArrayList) sortedQLits.get( j );
			if ( varCount > getVarCount(ql2) ) {
			    sortedQLits.add( j, ql );
			    added = true;
			    break;
			}		    
		    }
		    if ( ! added ) {
			sortedQLits.add( ql );
		    }
		}

		// Literals that will be used to try to simplify the
		// formula before pred var instantiation.
		ArrayList simplificationLits = new ArrayList();

		// The literal that will serve as the pattern for
		// extracting var replacement terms from answer
		// literals.
		ArrayList keyLit = null;

		// The list of answer literals retrieved using the
		// query lits, possibly built up via a sequence of
		// multiple queries.
		ArrayList answers = null;

		TreeSet working = new TreeSet();
			    
		// The first query lit for which we get an answer is
		// the key lit.
		for ( i = 0 ; i < sortedQLits.size() ; i++ ) {
		    ArrayList ql = (ArrayList) sortedQLits.get( i );
		    ArrayList accumulator = kb.askWithLiteral( ql );

		    // System.out.println( ql + " accumulator == " + accumulator );

		    if ( (accumulator != null) && !(accumulator.isEmpty()) ) {

			simplificationLits.add( ql );

			if ( keyLit == null ) {
			    keyLit = ql;
			    answers = KB.formulasToArrayLists( accumulator );
			}
			else {  // if ( accumulator.size() < answers.size() ) {
			    accumulator = KB.formulasToArrayLists( accumulator );

			    // Winnow the answers list.
			    working.clear();
			    ArrayList ql2 = null;
			    int varPos = ql.indexOf( idxVar );
			    for ( j = 0 ; j < accumulator.size() ; j++ ) {
				ql2 = (ArrayList) accumulator.get( j );
				working.add((String)ql2.get(varPos));
			    }
			    accumulator.clear();
			    accumulator.addAll( answers );
			    answers.clear();
			    varPos = keyLit.indexOf( idxVar );
			    for ( j = 0 ; j < accumulator.size() ; j++ ) {
				ql2 = (ArrayList) accumulator.get( j );
				if ( working.contains((String)ql2.get(varPos)) ) {
				    answers.add( ql2 );
				}
			    }
			}
		    }
		}
		if ( keyLit != null ) {
		    result.add( simplificationLits );
		    result.add( keyLit );
		    result.addAll( answers );
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}

	// System.out.println( "EXIT computeSubstitutionTuples( " + kb + ", " + queryLits + " )" );
	// System.out.println( "-> " + result );

	return result;
    }

    /** ***************************************************************
     * This method returns an ArrayList in which each element is
     * another ArrayList.  The head of each element is a variable.
     * The subsequent objects in each element are query literals
     * (ArrayLists).
     *
     * @return An ArrayList, or null if the input formula contains no
     * predicate variables.
     */
    private ArrayList prepareIndexedQueryLiterals() {

	// System.out.println( "ENTER prepareIndexedQueryLiterals( " + this + " )" );
	ArrayList ans = null;
	TreeSet vars = this.gatherPredVars();
	// System.out.println( "vars == " + vars );

	if ( !(vars.isEmpty()) ) {

	    ans = new ArrayList();
		    
	    // Try to simplify the formula.
	    String var = null;
	    ArrayList indexedQueryLits = null;

	    Iterator it = vars.iterator();
	    while ( it.hasNext() ) {
		var = (String) it.next();
		indexedQueryLits = gatherPredVarQueryLits( var );
		if ( !(indexedQueryLits.isEmpty()) ) {
		    ans.add( indexedQueryLits );
		}
	    }
	}

	// System.out.println( "EXIT prepareIndexedQueryLiterals( " + this + " )" );
	// System.out.println( "-> " + ans );

	return ans;
    }

    /** ***************************************************************
     * This method collects and returns all predicate variables that
     * occur in the Formula.
     *
     * @return a TreeSet of predicate variables (Strings).
     *
     */
    protected TreeSet gatherPredVars() {

	// System.out.println( "ENTER gatherPredVars( " +  this + " )" );
	TreeSet ans = new TreeSet();
	try {
	    if ( isNonEmptyString(this.theFormula) ) {
		List accumulator = new ArrayList();
		List working = new ArrayList();
		if ( this.listP() && !(this.empty()) ) {
		    accumulator.add( this );
		}
		int listCount = 0;
		while ( !(accumulator.isEmpty()) ) {
		    working.clear();
		    working.addAll( accumulator );
		    accumulator.clear();
		    for ( int i = 0 ; i < working.size() ; i++ ) {
			Formula f = (Formula) working.get( i );
			List literal = f.literalToArrayList();

			listCount += 1;
			// System.out.println( listCount + ": " + literal );

			int len = literal.size();
			if ( len > 0 ) {
			    String arg0 = (String) literal.get( 0 );
			    if ( isQuantifier(arg0)
				 || arg0.equals("holdsDuring")
				 || arg0.equals("KappaFn") ) {
				if ( len > 2 ) {
				    String arg2 = (String) literal.get( 2 );
				    Formula arg2F = new Formula();
				    arg2F.read( arg2 );
				    accumulator.add( arg2F );
				}
				else {
				    System.out.println( "INFO in Formula.gatherPredVars( " + this + " )" );
				    System.out.println( "Is this malformed? " + literal );
				}
			    }
			    else {
				int j = 0;
				if ( arg0.equals("holds") ) {
				    ans.add( literal.get(1) );
				    j = 2;
				}
				else if ( isVariable(arg0) ) {
				    ans.add( literal.get(0) );
				    j = 1;
				}
				for ( int k = j ; k < len ; k++ ) {
				    String arg = (String) literal.get( k );
				    Formula argF = new Formula();
				    argF.read( arg );
				    if ( argF.listP() && !(argF.empty()) ) {
					accumulator.add( argF );
				    }
				}
			    }
			}
		    }
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}

	// System.out.println( "EXIT gatherPredVars( " +  this + " )" );
	// System.out.println( "-> " + ans );

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
	String litStr = KB.literalListToString( litArr );
	Formula f = new Formula();
	f.read( litStr );
	return maybeRemoveMatchingLits( f );
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

	// System.out.println( "ENTER maybeRemoveMatchingLits( " + litF + " ) " );
	Formula result = null;
	try {
	    Formula f = this;
	    if ( f.listP() && !(f.empty()) ) {
		StringBuffer litBuf = new StringBuffer();
		String arg0 = f.car();
		if ( // arg0.equals("<=>") || 
		     arg0.equals("=>") ) {
		    String arg1 = f.cadr();
		    if ( arg1.equals(litF.theFormula) ) {
			Formula arg2F = new Formula();
			arg2F.read( f.caddr() );
			litBuf.append( arg2F.maybeRemoveMatchingLits(litF).theFormula );
		    }
		    else {
			Formula arg1F = new Formula();
			arg1F.read( f.cadr() );
			Formula arg2F = new Formula();
			arg2F.read( f.caddr() );
			litBuf.append( "(" + arg0 + " " 
				       + arg1F.maybeRemoveMatchingLits(litF).theFormula + " "
				       + arg2F.maybeRemoveMatchingLits(litF).theFormula + ")" );
		    }
		}
		else if ( isQuantifier(arg0)
			  || arg0.equals("holdsDuring") 
			  || arg0.equals("KappaFn") ) {
		    Formula arg2F = new Formula();
		    arg2F.read( f.caddr() );
		    litBuf.append( "(" + arg0 + " " + f.cadr() + " " 
				   + arg2F.maybeRemoveMatchingLits(litF).theFormula + ")" );
		}
		else if ( isCommutative(arg0) ) {
		    List litArr = f.literalToArrayList();
		    if ( litArr.contains(litF.theFormula) ) {
			litArr.remove(litF.theFormula);
		    }
		    String args = "";
		    int len = litArr.size();
		    for ( int i = 1 ; i < len ; i++ ) {
			Formula argF = new Formula();
			argF.read((String) litArr.get(i));
			args += (" " + argF.maybeRemoveMatchingLits(litF).theFormula );
		    }
		    if ( len > 2 ) {
			args = ( "(" + arg0 + args + ")" );
		    }
		    else {
			args = args.trim();
		    }
		    litBuf.append( args );
		}
		else {
		    litBuf.append( f.theFormula );
		}
		Formula newF = new Formula();
		newF.read( litBuf.toString() );
		result = newF;
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	if ( result == null ) {
	    result = this;
	}

	// System.out.println( "EXIT maybeRemoveMatchingLits( " + litF + " )" );
	// System.out.println( "-> " + result );

	return result;
    }

    /** ***************************************************************
     * This method collects and returns literals likely to be of use
     * as templates for retrieving predicates to be substituted for
     * var.
     *
     * @param var A variable (String) that occurs in the Formula.
     *
     * @return An ArrayList of literals (Lists) with var at the head.
     * The first element of the ArrayList is the variable (String).
     * Subsequent elements are Lists corresponding to Formulas.
     *
     */
    private ArrayList gatherPredVarQueryLits(String var) {

	// System.out.println( "ENTER gatherPredVarQueryLits( " +  this + ", " + var + " )" );
	ArrayList ans = new ArrayList();
	try {
	    TreeSet accumulator = new TreeSet();

	    // Get the clauses for this Formula.
	    StringBuffer litBuf = new StringBuffer();
	    List clauseData = (List) this.toNegAndPosLitsWithRenameInfo();
	    List clauses = (List) clauseData.get( 0 );

	    // System.out.println( "clauses == " + clauses );

	    Map varMap = (Map) clauseData.get( 2 );
	    if ( clauses != null ) {
		Iterator it1 = clauses.iterator();
		while ( it1.hasNext() ) {
		    List clause = (List) it1.next();

		    // Get the neg lits for this clause.
		    List negLits = (List) clause.get( 0 );
		    if ( !(negLits.isEmpty()) ) {
			Iterator it2 = negLits.iterator();
			while ( it2.hasNext() ) {
			    boolean hasSkolem = false;
			    Formula lit = (Formula) it2.next();
			    List litArr = lit.literalToArrayList();
			    Iterator it3 = litArr.iterator();
			    String term = null;
			    while ( it3.hasNext() ) {
				term = (String) it3.next();
				if ( term.matches("\\(SkFn\\s+\\d+") || term.matches("Sk\\d+") ) {
				    hasSkolem = true;
				    break;
				}
			    }
			    if ( hasSkolem ) { break; }

			    // If the literal does not start with a
			    // variable or with "holds" and does not
			    // contain Skolem terms, but does contain
			    // the variable in which we're interested,
			    // it is probably suitable as a query
			    // template.  Add it to accumulator.
			    String arg0 = (String)litArr.get(0);
			    if ( !(isVariable(arg0)) && !(arg0.equals("holds")) ) {
				boolean addToAccumulator = false;
				for ( int i = 1 ; i < litArr.size() ; i++ ) {
				    term = (String) litArr.get( i );
				    if ( isVariable(term) ) {
					String origVar = getOriginalVar( term, varMap );
					if ( origVar.equals(var) ) {
					    addToAccumulator = true;
					}
					litArr.set( i, origVar );
				    }
				}
				if ( addToAccumulator ) {
				    litBuf.setLength( 0 );
				    litBuf.append( KB.literalListToString(litArr) );
				    Formula newF = new Formula();
				    newF.read( litBuf.toString() );
				    accumulator.add( newF );
				}
			    }
			}
		    }
		}
	    }

	    // As a fallback, add a very general query literal.  This will be ignored
	    // if we have answers for a more specific query.  Unfortunately,
	    // sometimes the best thing to do is to quit if all of the more specific
	    // queries fail, but I haven't figured out an easy general way to
	    // determine when this is true.
	    String fallback = ( "(instance " + var + " Predicate)" );
	    Formula fallbackF = new Formula();
	    fallbackF.read( fallback );
	    accumulator.add( fallbackF );

	    // Convert the Set of Formulas to an indexed ArrayList of
	    // tuples (ArrayLists).
	    if ( !(accumulator.isEmpty()) ) {
		ArrayList tmp = new ArrayList( accumulator );
		tmp = KB.formulasToArrayLists( tmp );
		ans.add( var );
		ans.addAll( tmp );
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}

	// System.out.println( "EXIT gatherPredVarQueryLits( " +  this + ", " + var + " )" );
	// System.out.println( "-> " + ans );
	return ans;
    }

    ///////////////////////////////////////////////////////
    /*
      END of instantiatePredVars(KB kb) implementation.
    */
    ///////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////
    /*
      START of clausify() implementation.

      The code in the section below implements an algorithm for
      translating SUO-KIF expressions to clausal form.  The public
      methods are:

          public Formula clausify()
          public ArrayList clausifyWithRenameInfo()
          public ArrayList toNegAndPosLitsWithRenameInfo()
    */
    ///////////////////////////////////////////////////////

    /** ***************************************************************
     * This method converts the SUO-KIF Formula to a version of
     * clausal (resolution, conjunctive normal) form with Skolem
     * functions, following the procedure described in Logical
     * Foundations of Artificial Intelligence, by Michael Genesereth
     * and Nils Nilsson, 1987, pp. 63-66.
     *
     * <P>A literal is an atomic formula.  (However, because SUO-KIF
     * allows higher-order formulas, not all SUO-KIF literals are
     * really atomic.)  A clause is a disjunction of literals that
     * share no variable names with literals in any other clause in
     * the KB.  Note that even a relatively simple SUO-KIF formula
     * might generate multiple clauses.  In such cases, the Formula
     * returned will be a conjunction of clauses.  (A KB is understood
     * to be a conjunction of clauses.)  In all cases, the Formula
     * returned by this method should be a well-formed SUO-KIF Formula
     * if the input (original formula) is a well-formed SUO-KIF
     * Formula.  Rendering the output in true (LISPy) clausal form
     * would require an additional step, the removal of all
     * commutative logical operators, and the result would not be
     * well-formed SUO-KIF.</P>
     *
     * @see clausifyWithRenameInfo()
     @ @see toNegAndPosLitsWithRenameInfo()
     *
     * @return A SUO-KIF Formula in clausal form, or null if a clausal
     * form cannot be generated.
     */
    public Formula clausify() {
	Formula ans = null;
	try {
	    ans = this.equivalencesOut();
	    ans = ans.implicationsOut();
	    ans = ans.negationsIn();
	    ans = ans.renameVariables();
	    ans = ans.existentialsOut();
	    ans = ans.universalsOut();
	    ans = ans.disjunctionsIn();
	    ans = ans.standardizeApart();
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return ans;
    }

    /** ***************************************************************
     * This method converts the SUO-KIF Formula to a version of
     * clausal (resolution, conjunctive normal) form with Skolem
     * functions, following the procedure described in Logical
     * Foundations of Artificial Intelligence, by Michael Genesereth
     * and Nils Nilsson, 1987, pp. 63-66.
     *
     * <P>It returns an ArrayList that contains three items: The new
     * clausal-form Formula, the old (original input) Formula, and a
     * Map containing a graph of all the variable substitions done
     * during the conversion to clausal form.  This Map makes it
     * possible to retrieve the correspondence between the variables
     * in the clausal form and the variables in the original
     * Formula.</P>
     *
     * @see clausify()
     @ @see toNegAndPosLitsWithRenameInfo()
     *
     * @return A three-element ArrayList, [<Formula>, <Formula>,
     * <Map>], in which some elements might be null if a clausal form
     * cannot be generated.
     */
    public ArrayList clausifyWithRenameInfo() {
	ArrayList result = new ArrayList();
	Formula ans = null;
	try {
	    HashMap topLevelVars  = new HashMap();
	    HashMap scopedRenames = new HashMap();
	    HashMap allRenames    = new HashMap();
	    HashMap standardizedRenames = new HashMap();
	    ans = this.equivalencesOut();
	    ans = ans.implicationsOut();
	    ans = ans.negationsIn();
	    ans = ans.renameVariables( topLevelVars, scopedRenames, allRenames );
	    ans = ans.existentialsOut();
	    ans = ans.universalsOut();
	    ans = ans.disjunctionsIn();
	    ans = ans.standardizeApart( standardizedRenames );
	    allRenames.putAll( standardizedRenames );
	    result.add( ans );
	    result.add( this );
	    result.add( allRenames );
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return result;
    }

    /** ***************************************************************
     * This method converts the SUO-KIF Formula to an ArrayList of
     * clauses.  Each clause is an ArrayList containing an ArrayList
     * of negative literals, and an ArrayList of positive literals.
     * Either the neg lits list or the pos lits list could be empty.
     * Each literal is a Formula object.
     *
     * The first object in the returned ArrayList is an ArrayList of
     * clauses.
     *
     * The second object in the returned ArrayList is the original
     * (input) Formula object (this).
     *
     * The third object in the returned ArrayList is a Map that
     * contains a graph of all the variable substitions done during
     * the conversion of this Formula to clausal form.  This Map makes
     * it possible to retrieve the correspondences between the
     * variables in the clausal form and the variables in the original
     * Formula.
     *
     * @see clausify()
     * @see clausifyWithRenameInfo()
     *
     * @return A three-element ArrayList, 
     *
     * [ 
     *   // 1. clauses
     *   [ 
     *     // a clause
     *     [ 
     *       // negative literals
     *       [ Formula1, Formula2, ..., FormulaN ],
     *       // positive literals
     *       [ Formula1, Formula2, ..., FormulaN ] 
     *     ],
     *
     *     // another clause
     *     [ 
     *       // negative literals
     *       [ Formula1, Formula2, ..., FormulaN ],
     *       // positive literals
     *       [ Formula1, Formula2, ..., FormulaN ] 
     *     ],
     *
     *     ...,
     *   ],
     *
     *   // 2.
     *   <the-original-Formula>,
     *
     *   // 3.
     *   {a-Map-of-variable-renamings},
     *
     * ]
     *
     */
    public ArrayList toNegAndPosLitsWithRenameInfo() {
	ArrayList ans = new ArrayList();
	try {
	    List clausesWithRenameInfo = this.clausifyWithRenameInfo();
	    if ( clausesWithRenameInfo.size() == 3 ) {
		Formula clausalForm = (Formula) clausesWithRenameInfo.get( 0 );
		ArrayList clauses = clausalForm.operatorsOut();
		if ( (clauses != null) && !(clauses.isEmpty()) ) {
		    ArrayList newClauses = new ArrayList();
		    for ( int i = 0 ; i < clauses.size() ; i++ ) {
			ArrayList negLits = new ArrayList();
			ArrayList posLits = new ArrayList();
			ArrayList literals = new ArrayList();
			literals.add( negLits );
			literals.add( posLits );
			Formula clause = (Formula) clauses.get( i );
			if ( clause.listP() ) {
			    while ( !(clause.empty()) ) {
				boolean isNegLit = false;
				String lit = clause.car();
				Formula litF = new Formula();
				litF.read( lit );
				if ( litF.listP() && litF.car().equals("not") ) {
				    litF.read( litF.cadr() );
				    isNegLit = true;
				}
				if ( litF.theFormula.equals("FALSE") ) {
				    isNegLit = true;
				}
				if ( isNegLit ) {
				    negLits.add( litF );
				}
				else {
				    posLits.add( litF );
				}
				clause = clause.cdrAsFormula();
			    }
			}
			else if ( clause.theFormula.equals("FALSE") ) {
			    negLits.add( clause );
			}
			else {
			    posLits.add( clause );
			}
			newClauses.add( literals );
		    }
		    ans.add( newClauses );
		}
		if ( ans.size() == 1 ) {
		    for ( int j = 1 ; j < clausesWithRenameInfo.size() ; j++ ) {		    
			ans.add( clausesWithRenameInfo.get(j) );
		    }
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return ans;
    }

    /** ***************************************************************
     * This method converts every occurrence of '<=>' in the Formula
     * to a conjunct with two occurrences of '=>'.
     * 
     * @return A Formula with no occurrences of '<=>'.
     *
     */
    private Formula equivalencesOut() {
	Formula ans = this;
	try {
	    String theNewFormula = null;
	    if ( this.listP() && !(this.empty()) ) {
		String head = this.car();
		if ( isNonEmptyString(head) && listP(head) ) {
		    Formula headF = new Formula();
		    headF.read( head );
		    String newHead = headF.equivalencesOut().theFormula;
		    theNewFormula = this.cdrAsFormula().equivalencesOut().cons(newHead).theFormula;
		}
		else if ( head.equals("<=>") ) {
		    String second = this.cadr();
		    Formula secondF = new Formula();
		    secondF.read( second );
		    String newSecond = secondF.equivalencesOut().theFormula;
		    String third = this.caddr();
		    Formula thirdF = new Formula();
		    thirdF.read( third );
		    String newThird = thirdF.equivalencesOut().theFormula;
		    
		    theNewFormula = ( "(and (=> "
				      + newSecond
				      + " "
				      + newThird
				      + ") (=> "
				      + newThird
				      + " "
				      + newSecond
				      + "))" );
		}
		else {
		    theNewFormula = this.cdrAsFormula().equivalencesOut().cons(head).theFormula;
		}
		if ( theNewFormula != null ) {
		    ans = new Formula();
		    ans.read( theNewFormula );
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return ans;
    }

    /** ***************************************************************
     * This method converts every occurrence of '(=> LHS RHS' in the
     * Formula to a disjunct of the form '(or (not LHS) RHS)'.
     * 
     * @return A Formula with no occurrences of '=>'.
     *
     */
    private Formula implicationsOut() {
	Formula ans = this;
	try {
	    String theNewFormula = null;
	    if ( this.listP() && !(this.empty()) ) {
		String head = this.car();
		if ( isNonEmptyString(head) && listP(head) ) {
		    Formula headF = new Formula();
		    headF.read( head );
		    String newHead = headF.implicationsOut().theFormula;
		    theNewFormula = this.cdrAsFormula().implicationsOut().cons(newHead).theFormula;
		}
		else if ( head.equals("=>") ) {
		    String second = this.cadr();
		    Formula secondF = new Formula();
		    secondF.read( second );
		    String newSecond = secondF.implicationsOut().theFormula;
		    String third = this.caddr();
		    Formula thirdF = new Formula();
		    thirdF.read( third );
		    String newThird = thirdF.implicationsOut().theFormula;
		    theNewFormula = ( "(or (not " + newSecond + ") " + newThird + ")" );
		}
		else {
		    theNewFormula = this.cdrAsFormula().implicationsOut().cons(head).theFormula;
		}
		if ( theNewFormula != null ) {
		    ans = new Formula();
		    ans.read( theNewFormula );
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return ans;
    }

    /** ***************************************************************
     * This method 'pushes in' all occurrences of 'not', so that each
     * occurrence has the narrowest possible scope, and also removes
     * from the Formula all occurrences of '(not (not ... ))'.
     *
     * @see negationsIn_1().
     *
     * @return A Formula with all occurrences of 'not' accorded
     * narrowest scope, and no occurrences of '(not (not ... ))'.
     */
    private Formula negationsIn() {
	Formula f = this;
	Formula ans = negationsIn_1();

	// Here we repeatedly apply negationsIn_1() until there are no
	// more changes.
	while ( ! f.theFormula.equals(ans.theFormula) ) {	    

	    /*
	    System.out.println();
	    System.out.println( "f.theFormula == " + f.theFormula );
	    System.out.println( "ans.theFormula == " + ans.theFormula );
	    System.out.println();
	    */

	    f = ans;
	    ans = f.negationsIn_1();
	}
	return ans;
    }

    /** ***************************************************************
     * This method is used in negationsIn().  It recursively 'pushes
     * in' all occurrences of 'not', so that each occurrence has the
     * narrowest possible scope, and also removes from the Formula all
     * occurrences of '(not (not ... ))'.
     *
     * @see negationsIn().
     *
     * @return A Formula with all occurrences of 'not' accorded
     * narrowest scope, and no occurrences of '(not (not ... ))'.
     */
    private Formula negationsIn_1() {	
	// System.out.println( "INFO in negationsIn_1( " + theFormula + " )" );
	try {
	    if ( isNonEmptyString(this.theFormula) ) {
		if ( this.listP() ) {
		    if ( this.empty() ) { return this; }
		    String arg0 = this.car();
		    String arg1 = this.cadr();
		    if ( arg0.equals("not") && listP(arg1) ) {
			Formula arg1F = new Formula();
			arg1F.read( arg1 );
			String arg0_of_arg1 = arg1F.car();
			if ( arg0_of_arg1.equals("not") ) {
			    String arg1_of_arg1 = arg1F.cadr();
			    Formula arg1_of_arg1F = new Formula();
			    arg1_of_arg1F.read( arg1_of_arg1 );
			    return arg1_of_arg1F;
			}
			if ( isCommutative(arg0_of_arg1) ) {
			    String newOp = ( arg0_of_arg1.equals("and") ? "or" : "and" );
			    return arg1F.cdrAsFormula().listAll( "(not ", ")" ).cons(newOp);
			}
			if ( isQuantifier(arg0_of_arg1) ) {
			    String vars = arg1F.cadr();
			    String arg2_of_arg1 = arg1F.caddr();
			    String quant = ( arg0_of_arg1.equals("forall") ? "exists" : "forall" );
			    arg2_of_arg1 = ( "(not " + arg2_of_arg1 + ")" );
			    Formula arg2_of_arg1F = new Formula();
			    arg2_of_arg1F.read( arg2_of_arg1 );
			    String theNewFormula = ( "(" + quant + " " + vars + " " 
						     + arg2_of_arg1F.negationsIn_1().theFormula + ")" );
			    Formula newF = new Formula();
			    newF.read( theNewFormula );
			    return newF;
			}
			String theNewFormula = ( "(not " + arg1F.negationsIn_1().theFormula + ")" );
			Formula newF = new Formula();
			newF.read( theNewFormula );
			return newF;
		    }
		    if ( isQuantifier(arg0) ) {
			String arg2 = this.caddr();
			Formula arg2F = new Formula();
			arg2F.read( arg2 );
			String newArg2 = arg2F.negationsIn_1().theFormula;
			String theNewFormula = ( "(" + arg0 + " " + arg1 + " " + newArg2 + ")" );
			Formula newF = new Formula();
			newF.read( theNewFormula );
			return newF;
		    }
		    if ( listP(arg0) ) {
			Formula arg0F = new Formula();
			arg0F.read( arg0 );
			return this.cdrAsFormula().negationsIn_1().cons(arg0F.negationsIn_1().theFormula);
		    }
		    return this.cdrAsFormula().negationsIn_1().cons(arg0);
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return this;
    }

    /** ***************************************************************
     * This method augments each element of the Formula by
     * concatenating optional Strings before and after the element.
     *
     * Note that in most cases the input Formula will be simply a
     * list, not a well-formed SUO-KIF Formula, and that the output
     * will therefore not necessarily be a well-formed Formula.
     *
     * @param before A String that, if present, is prepended to every
     * element of the Formula.
     *
     * @param after A String that, if present, is postpended to every
     * element of the Formula.
     * 
     * @return A Formula, or, more likely, simply a list, with the
     * String values corresponding to before and after added to each
     * element.
     *
     */
    private Formula listAll(String before, String after) {
	Formula ans = this;
	String theNewFormula = null;
	if ( this.listP() ) {
	    theNewFormula = "";
	    Formula f = this;
	    while ( !(f.empty()) ) {
		String element = f.car();
		if ( isNonEmptyString(before) ) {
		    element = ( before + element );
		}
		if ( isNonEmptyString(after) ) {
		    element += after;
		}
		theNewFormula += ( " " + element );
		f = f.cdrAsFormula();
	    }
	    theNewFormula = ( "(" + theNewFormula.trim() + ")" );
	    if ( isNonEmptyString(theNewFormula) ) {
		ans = new Formula();
		ans.read( theNewFormula );
	    }
	}
	return ans;
    }

    /** ***************************************************************
     * This static variable holds the long value that is used to
     * generate unique variable names and Skolem terms.  This should
     * probably be just an int, after all.
     */
    private static long VAR_INDEX = 0;

    /** ***************************************************************
     * This method increments VAR_INDEX and then returns the new long
     * value.  If VAR_INDEX is already at Long.MAX_VALUE, then
     * VAR_INDEX is reset to 0.
     * 
     * @return A long value between 0 and Long.MAX_VALUE inclusive.
     */
    private static long incVarIndex() {
	long oldVal = VAR_INDEX;
	if ( oldVal == Long.MAX_VALUE ) {
	    VAR_INDEX = 0;
	}
	else {
	    VAR_INDEX += 1;
	}
	return VAR_INDEX;
    }

    /** ***************************************************************
     * This method returns a new SUO-KIF variable String, modifying
     * any digit suffix to ensure that the variable will be unique.
     *
     * @param prefix An optional variable prefix string.
     * 
     * @return A new SUO-KIF variable.
     */
    private static String newVar(String prefix) {
	String base = "?X";
	String varIdx = Long.toString( incVarIndex() );
	if ( isNonEmptyString(prefix) ) {
	    List woDigitSuffix = KB.getMatches( prefix, "var_with_digit_suffix" );
	    if ( woDigitSuffix != null ) {
		base = (String) woDigitSuffix.get( 0 );
	    }
	    else if ( prefix.startsWith("@ROW") ) {
		base = "@ROW";
	    }
	    else if ( prefix.startsWith("?X") ) {
		base = "?X";
	    }
	    else {
		base = prefix;
	    }
	    if ( ! (base.startsWith("?") || base.startsWith("@")) ) {
		base = ( "?" + base );
	    }
	}
	return ( base + varIdx );
    }

    /** ***************************************************************
     * This method returns a new SUO-KIF variable String, modifying
     * any digit suffix to ensure that the variable will be unique.
     *
     * @return A new SUO-KIF variable.
     */
    private static String newVar() {
	return newVar( null );
    }

    /** ***************************************************************
     * This method returns a new SUO-KIF row variable String,
     * modifying any digit suffix to ensure that the variable will be
     * unique.
     *
     * @return A new SUO-KIF row variable.
     */
    private static String newRowVar() {
	return newVar( "@ROW" );
    }

    /** ***************************************************************
     * This method returns a new Formula in which all variables have
     * been renamed to ensure uniqueness.
     *
     * @see clausify()
     * @see renameVariables(Map topLevelVars, Map scopedRenames)
     *
     * @return A new SUO-KIF Formula with all variables renamed.
     */
    private Formula renameVariables() {
	HashMap topLevelVars = new HashMap();
	HashMap scopedRenames = new HashMap();
	HashMap allRenames = new HashMap();
	return renameVariables( topLevelVars, scopedRenames, allRenames );
    }

    /** ***************************************************************
     * This method returns a new Formula in which all variables have
     * been renamed to ensure uniqueness.
     *
     * @see renameVariables().
     *
     * @param topLevelVars A Map that is used to track renames of
     * implicitly universally quantified variables.
     *
     * @param scopedRenames A Map that is used to track renames of
     * explicitly quantified variables.
     *
     * @param allRenames A Map from all new vars in the Formula to
     * their old counterparts.
     *
     * @return A new SUO-KIF Formula with all variables renamed.
     */
    private Formula renameVariables(Map topLevelVars, Map scopedRenames, Map allRenames) {

	try {
	    if ( isNonEmptyString(this.theFormula) ) {
		if ( this.listP() ) {
		    if ( this.empty() ) { return this; }
		    String arg0 = this.car();
		    if ( isQuantifier(arg0) ) {
			
			// Copy the scopedRenames map to protect
			// variable scope as we descend below this
			// quantifier.
			Map newScopedRenames = new HashMap( scopedRenames );

			String oldVars = this.cadr();
			Formula oldVarsF = new Formula();
			oldVarsF.read( oldVars );
			String newVars = "";
			while ( !(oldVarsF.empty()) ) {
			    String oldVar = oldVarsF.car();
			    String newVar = newVar( oldVar );
			    newScopedRenames.put( oldVar, newVar );
			    allRenames.put( newVar, oldVar );
			    newVars += ( " " + newVar );
			    oldVarsF = oldVarsF.cdrAsFormula();
			}
			newVars = ( "(" + newVars.trim() + ")" );
			String arg2 = this.caddr();
			Formula arg2F = new Formula();
			arg2F.read( arg2 );
			String newArg2 = arg2F.renameVariables(topLevelVars,newScopedRenames,allRenames).theFormula;
			String theNewFormula = ( "(" + arg0 + " " + newVars + " " + newArg2 + ")" );
			Formula newF = new Formula();
			newF.read( theNewFormula );
			return newF;
		    }
		    Formula arg0F = new Formula();
		    arg0F.read( arg0 );
		    String newArg0 = arg0F.renameVariables(topLevelVars,scopedRenames,allRenames).theFormula;
		    String newRest = 
			this.cdrAsFormula().renameVariables(topLevelVars,scopedRenames,allRenames).theFormula;
		    Formula newRestF = new Formula();
		    newRestF.read( newRest );
		    String theNewFormula = newRestF.cons(newArg0).theFormula;
		    Formula newF = new Formula();
		    newF.read( theNewFormula );
		    return newF;
		}
		if ( isVariable(this.theFormula) ) {
		    String rnv = (String) scopedRenames.get( this.theFormula );
		    if ( !(isNonEmptyString(rnv)) ) {
			rnv = (String) topLevelVars.get( this.theFormula );
			if ( !(isNonEmptyString(rnv)) ) {
			    rnv = newVar( this.theFormula );
			    topLevelVars.put( this.theFormula, rnv );
			    allRenames.put( rnv, this.theFormula );
			}
		    }
		    Formula newF = new Formula();
		    newF.read( rnv );
		    return newF;
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return this;
    }

    /** ***************************************************************
     * This method returns a new, unique skolem term with each
     * invocation.
     *
     * @param vars A sorted TreeSet of the universally quantified
     * variables that potentially define the skolem term.  The set may
     * be empty.
     *
     * @return A String.  The string will be a skolem functional term
     * (a list) if vars cotains variables.  Otherwise, it will be an
     * atomic constant.
     */
    private String newSkolemTerm(TreeSet vars) {
	String ans = "Sk";
	long idx = incVarIndex();
	if ( (vars != null) && !(vars.isEmpty()) ) {
	    ans += ( "Fn " + idx );
	    Iterator it = vars.iterator();
	    while ( it.hasNext() ) {
		String var = (String) it.next();
		ans += ( " " + var );
	    }
	    ans = ( "(" + ans + ")" );
	}
	else {
	    ans += idx;
	}
	return ans;
    }	    

    /** ***************************************************************
     * This method returns a new Formula in which all existentially
     * quantified variables have been replaced by Skolem terms.
     *
     * @see existentialsOut(Map evSubs, TreeSet iUQVs, TreeSet scopedUQVs)
     * @see collectIUQVars(TreeSet iuqvs, TreeSet scopedVars)
     *
     * @return A new SUO-KIF Formula without existentially quantified
     * variables.
     */
    private Formula existentialsOut() {

	// Existentially quantified variable substitution pairs:
	// var -> skolem term.
	Map evSubs = new HashMap();

	// Implicitly universally quantified variables.
	TreeSet iUQVs = new TreeSet();

	// Explicitly quantified variables.
	TreeSet scopedVars = new TreeSet();

	// Explicitly universally quantified variables.
	TreeSet scopedUQVs = new TreeSet();

	// Collect the implicitly universally qualified variables from
	// the Formula.
	collectIUQVars( iUQVs, scopedVars );

	// Do the recursive term replacement, and return the results.
	return existentialsOut( evSubs, iUQVs, scopedUQVs );
    }

    /** ***************************************************************
     * This method returns a new Formula in which all existentially
     * quantified variables have been replaced by Skolem terms.
     *
     * @see existentialsOut()
     *
     * @param evSubs A Map of variable - skolem term substitution
     * pairs.
     *
     * @param iUQVs A TreeSet of implicitly universally quantified
     * variables.
     *
     * @param scopedUQVs A TreeSet of explicitly universally
     * quantified variables.
     *
     * @return A new SUO-KIF Formula without existentially quantified
     * variables.
     */
    private Formula existentialsOut(Map evSubs, TreeSet iUQVs, TreeSet scopedUQVs) {
	// System.out.println( "INFO in existentialsOut( " + this.theFormula + ", " + evSubs + ", " + iUQVs + ", " + scopedUQVs + " )" );
	try {
	    if ( isNonEmptyString(this.theFormula) ) {
		if ( this.listP() ) {
		    if ( this.empty() ) { return this; }
		    String arg0 = this.car();
		    if ( arg0.equals("forall") ) {
			
			// Copy the scoped variables set to protect
			// variable scope as we descend below this
			// quantifier.
			TreeSet newScopedUQVs = new TreeSet( scopedUQVs );

			String varList = this.cadr();			
			Formula varListF = new Formula();
			varListF.read( varList );
			while ( !(varListF.empty()) ) {
			    String var = varListF.car();
			    newScopedUQVs.add( var );
			    varListF.read( varListF.cdr() );
			}
			String arg2 = this.caddr();
			Formula arg2F = new Formula();
			arg2F.read( arg2 );
			String theNewFormula = ( "(forall " 
						 + varList 
						 + " " 
						 + arg2F.existentialsOut(evSubs, iUQVs, newScopedUQVs).theFormula + ")" );
			this.read( theNewFormula );
			return this;
		    }
		    if ( arg0.equals("exists") ) {

			// Collect the relevant universally quantified
			// variables.
			TreeSet uQVs = new TreeSet( iUQVs );
			uQVs.addAll( scopedUQVs );

			// Collect the existentially quantified
			// variables.
			ArrayList eQVs = new ArrayList();
			String varList = this.cadr();			
			Formula varListF = new Formula();
			varListF.read( varList );
			while ( !(varListF.empty()) ) {
			    String var = varListF.car();
			    eQVs.add( var );
			    varListF.read( varListF.cdr() );
			}
			
			// For each existentially quantified variable,
			// create a corresponding skolem term, and
			// store the pair in the evSubs map.
			for ( int i = 0 ; i < eQVs.size() ; i++ ) {
			    String var = (String) eQVs.get( i );
			    String skTerm = newSkolemTerm( uQVs );
			    evSubs.put( var, skTerm );
			}
			String arg2 = this.caddr();
			Formula arg2F = new Formula();
			arg2F.read( arg2 );
			return arg2F.existentialsOut(evSubs, iUQVs, scopedUQVs);
		    }
		    Formula arg0F = new Formula();
		    arg0F.read( arg0 );
		    String newArg0 = arg0F.existentialsOut(evSubs, iUQVs, scopedUQVs).theFormula;
		    return this.cdrAsFormula().existentialsOut(evSubs, iUQVs, scopedUQVs).cons(newArg0);
		}
		if ( isVariable(this.theFormula) ) {
		    String newTerm = (String) evSubs.get( this.theFormula );
		    if ( isNonEmptyString(newTerm) ) {
			this.read(newTerm);
		    }
		    return this;
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return this;
    }

    /** ***************************************************************
     * This method collects all variables in Formula that appear to be
     * only implicitly universally quantified and adds them to the
     * TreeSet iuqvs.  Note the iuqvs must be passed in.
     *
     * @param iuqvs A TreeSet for accumulating variables that appear
     * to be implicitly universally quantified.
     *
     * @param scopedVars A TreeSet containing explicitly quantified
     * variables.
     *
     * @return void
     */
    private void collectIUQVars(TreeSet iuqvs, TreeSet scopedVars) {

	// System.out.println( "INFO in collectIUQVars( " + this.theFormula + ", " + iuqvs + ", " + scopedVars + " )" );
	try {
	    if ( isNonEmptyString(this.theFormula) ) {
		if ( this.listP() && !(this.empty()) ) {
		    String arg0 = this.car();
		    if ( isQuantifier(arg0) ) {
			
			// Copy the scopedVars set to protect variable
			// scope as we descend below this quantifier.
			TreeSet newScopedVars = new TreeSet( scopedVars );

			String varList = this.cadr();
			Formula varListF = new Formula();
			varListF.read( varList );
			while ( !(varListF.empty()) ) {
			    String var = varListF.car();
			    newScopedVars.add( var );
			    varListF = varListF.cdrAsFormula();
			}
			String arg2 = this.caddr();
			Formula arg2F = new Formula();
			arg2F.read( arg2 );
			arg2F.collectIUQVars( iuqvs, newScopedVars );
		    }
		    else {
			Formula arg0F = new Formula();
			arg0F.read( arg0 );
			arg0F.collectIUQVars( iuqvs, scopedVars );
			this.cdrAsFormula().collectIUQVars( iuqvs, scopedVars );
		    }
		}
		else if ( isVariable(this.theFormula) 
			  && !(scopedVars.contains(this.theFormula)) ) {
		    iuqvs.add( this.theFormula );
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return;
    }

    /** ***************************************************************
     * This method returns a new Formula in which explicit univeral
     * quantifiers have been removed.
     *
     * @see clausify()
     *
     * @return A new SUO-KIF Formula without explicit universal
     * quantifiers.
     */
    private Formula universalsOut() {
	// System.out.println( "INFO in universalsOut( " + this.theFormula + " )" );
	try {
	    if ( isNonEmptyString(this.theFormula) ) {
		if ( this.listP() ) {
		    if ( this.empty() ) { return this; }
		    String arg0 = this.car();
		    if ( arg0.equals("forall") ) {
			String arg2 = this.caddr();
			this.read( arg2 );
			return this.universalsOut();
		    }
		    Formula arg0F = new Formula();
		    arg0F.read( arg0 );
		    String newArg0 = arg0F.universalsOut().theFormula;
		    return this.cdrAsFormula().universalsOut().cons(newArg0);
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return this;
    }

    /** ***************************************************************
     * This method returns a new Formula in which nested 'and', 'or',
     * and 'not' operators have been unnested:
     *
     * (not (not <literal> ... )) -> <literal>
     *
     * (and (and <literal-sequence> ... )) -> (and <literal-sequence> ...)
     *
     * (or (or <literal-sequence> ...)) -> (or <literal-sequence> ...)
     *
     * @see clausify()
     * @see nestedOperatorsOut_1()
     *
     * @return A new SUO-KIF Formula in which nested commutative
     * operators and 'not' have been unnested.
     */
    private Formula nestedOperatorsOut() {
	Formula f = this;
	Formula ans = nestedOperatorsOut_1();

	// Here we repeatedly apply nestedOperatorsOut_1() until there are no
	// more changes.
	while ( ! f.theFormula.equals(ans.theFormula) ) {	    

	    /*
	    System.out.println();
	    System.out.println( "f.theFormula == " + f.theFormula );
	    System.out.println( "ans.theFormula == " + ans.theFormula );
	    System.out.println();
	    */

	    f = ans;
	    ans = f.nestedOperatorsOut_1();
	}
	return ans;
    }

    /** ***************************************************************
     *
     * @see clausify()
     * @see nestedOperatorsOut_1()
     *
     * @return A new SUO-KIF Formula in which nested commutative
     * operators and 'not' have been unnested.
     */
    private Formula nestedOperatorsOut_1() {

	// System.out.println( "INFO in nestedOperatorsOut_1( " + this.theFormula + " )" );
	try {
	    if ( isNonEmptyString(this.theFormula) ) {
		if ( this.listP() ) {
		    if ( this.empty() ) { return this; }
		    String arg0 = this.car();
		    if ( isCommutative(arg0) || arg0.equals("not") ) {
			ArrayList literals = new ArrayList();
			Formula restF = this.cdrAsFormula();
			while ( !(restF.empty()) ) {
			    String lit = restF.car();
			    Formula litF = new Formula();
			    litF.read ( lit );
			    if ( litF.listP() ) {
				String litFarg0 = litF.car();
				if ( litFarg0.equals(arg0) ) {
				    if ( arg0.equals("not") ) {
					String theNewFormula = litF.cadr();
					Formula newF = new Formula();
					newF.read( theNewFormula );
					return newF.nestedOperatorsOut_1();
				    }
				    Formula rest2F = litF.cdrAsFormula();
				    while ( !(rest2F.empty()) ) {
					String rest2arg0 = rest2F.car();
					Formula rest2arg0F = new Formula();
					rest2arg0F.read( rest2arg0 );
					literals.add( rest2arg0F.nestedOperatorsOut_1().theFormula );
					rest2F = rest2F.cdrAsFormula();
				    }
				}
				else {
				    literals.add( litF.nestedOperatorsOut_1().theFormula );
				}
			    }
			    else {
				literals.add( lit );
			    }
			    restF = restF.cdrAsFormula();
			}
			String theNewFormula = ( "(" + arg0 );
			for ( int i = 0 ; i < literals.size() ; i++ ) {
			    theNewFormula += ( " " + (String)literals.get(i) );
			}
			theNewFormula += ")";
			Formula newF = new Formula();
			newF.read( theNewFormula );
			return newF;
		    }
		    Formula arg0F = new Formula();
		    arg0F.read( arg0 );
		    String newArg0 = arg0F.nestedOperatorsOut_1().theFormula;
		    return this.cdrAsFormula().nestedOperatorsOut_1().cons(newArg0);
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return this;
    }

    /** ***************************************************************
     * This method returns a new Formula in which all occurrences of
     * 'or' have been accorded the least possible scope.
     *
     * (or P (and Q R)) -> (and (or P Q) (or P R))
     *
     * @see clausify()
     * @see disjunctionsIn_1()
     *
     * @return A new SUO-KIF Formula in which occurrences of 'or' have
     * been 'moved in' as far as possible.
     */
    private Formula disjunctionsIn() {
	Formula f = this;
	Formula ans = nestedOperatorsOut().disjunctionsIn_1();

	// Here we repeatedly apply disjunctionIn_1() until there are no
	// more changes.
	while ( ! f.theFormula.equals(ans.theFormula) ) {	    

	    /* 
	    System.out.println();
	    System.out.println( "f.theFormula == " + f.theFormula );
	    System.out.println( "ans.theFormula == " + ans.theFormula );
	    System.out.println();
	    */

	    f = ans;
	    ans = f.nestedOperatorsOut().disjunctionsIn_1();
	}
	return ans;
    }
 
    /** ***************************************************************
     *
     * @see clausify()
     * @see disjunctionsIn()
     *
     * @return A new SUO-KIF Formula in which occurrences of 'or' have
     * been 'moved in' as far as possible.
     */
    private Formula disjunctionsIn_1() {

	// System.out.println( "INFO in disjunctionsIn_1( " + this.theFormula + " )" );
	try {
	    if ( isNonEmptyString(this.theFormula) ) {
		if ( this.listP() ) {
		    if ( this.empty() ) { return this; }
		    String arg0 = this.car();
		    if ( arg0.equals("or") ) {
			List disjuncts = new ArrayList();
			List conjuncts = new ArrayList();
			Formula restF = this.cdrAsFormula();
			while ( !(restF.empty()) ) {
			    String disjunct = restF.car();
			    Formula disjunctF = new Formula();
			    disjunctF.read( disjunct );
			    if ( disjunctF.listP() 
				 && disjunctF.car().equals("and") 
				 && conjuncts.isEmpty() ) {
				Formula rest2F = disjunctF.cdrAsFormula().disjunctionsIn_1();
				while ( !(rest2F.empty()) ) {
				    conjuncts.add( rest2F.car() );
				    rest2F = rest2F.cdrAsFormula();
				}
			    }
			    else {
				disjuncts.add( disjunct );
			    }
			    restF = restF.cdrAsFormula();
			}

			if ( conjuncts.isEmpty() ) { return this; }

			Formula resultF = new Formula();
			resultF.read("()");
			String disjunctsString = "";
			for ( int i = 0 ; i < disjuncts.size() ; i++ ) {
			    disjunctsString += ( " " + (String)disjuncts.get(i) );
			}
			disjunctsString = ( "(" + disjunctsString.trim() + ")" );
			Formula disjunctsF = new Formula();
			disjunctsF.read( disjunctsString );
			for ( int ci = 0 ; ci < conjuncts.size() ; ci++ ) {
			    String newDisjuncts = 
				disjunctsF.cons((String)conjuncts.get(ci)).cons("or").disjunctionsIn_1().theFormula;
			    resultF = resultF.cons( newDisjuncts );
			}
			resultF = resultF.cons("and");
			return resultF;
		    }
		    Formula arg0F = new Formula();
		    arg0F.read( arg0 );
		    String newArg0 = arg0F.disjunctionsIn_1().theFormula;
		    return this.cdrAsFormula().disjunctionsIn_1().cons(newArg0);
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return this;
    }

    /** ***************************************************************
     * This method returns an ArrayList of clauses.  Each clause is a
     * LISP list (really, a Formula) containing one or more Formulas.
     * The LISP list is assumed to be a disjunction, but there is no
     * 'or' at the head.
     *
     * @see clausify()
     *
     * @return An ArrayList of LISP lists, each of which contains one
     * or more Formulas.
     */
    private ArrayList operatorsOut() {
	// System.out.println( "INFO in operatorsOut( " + this.theFormula + " )" );
	ArrayList result = new ArrayList();
	try {
	    ArrayList clauses = new ArrayList();
	    if ( isNonEmptyString(this.theFormula) ) {
		if ( this.listP() ) {
		    String arg0 = this.car();
		    if ( arg0.equals("and") ) {
			Formula restF = this.cdrAsFormula();
			while ( !(restF.empty()) ) {
			    String fStr = restF.car();
			    Formula newF = new Formula();
			    newF.read( fStr );
			    clauses.add( newF );
			    restF = restF.cdrAsFormula();
			}
		    }
		}
		if ( clauses.isEmpty() ) {
		    clauses.add( this );
		}
		for ( int i = 0 ; i < clauses.size() ; i++ ) {
		    Formula clauseF = new Formula();
		    clauseF.read( "()" );
		    Formula f = (Formula) clauses.get( i );
		    if ( f.listP() ) {
			if ( f.car().equals("or") ) {
			    f = f.cdrAsFormula();
			    while ( !(f.empty()) ) {
				String lit = f.car();
				clauseF = clauseF.cons( lit );
				f = f.cdrAsFormula();
			    }
			}
		    }
		    if ( clauseF.empty() ) {
			clauseF = clauseF.cons( f.theFormula );
		    }
		    result.add( clauseF );
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return result;
    }

    /** ***************************************************************
     * This method returns a Formula in which variables for separate
     * clauses have been 'standardized apart'.
     *
     * @see clausify()
     * @see standardizeApart(Map renameMap)
     * @see standardizeApart_1(Map renames, Map reverseRenames)
     *
     * @return A Formula.
     */
    private Formula standardizeApart() {
	HashMap reverseRenames = new HashMap();
	return this.standardizeApart( reverseRenames );
    }

    /** ***************************************************************
     * This method returns a Formula in which variables for separate
     * clauses have been 'standardized apart'.
     *
     * @see clausify()
     * @see standardizeApart()
     * @see standardizeApart_1(Map renames, Map reverseRenames)
     *
     * @param renameMap A Map for capturing one-to-one variable rename
     * correspondences.  Keys are new variables.  Values are old
     * variables.
     *
     * @return A Formula.
     */
    private Formula standardizeApart(Map renameMap) {
	    
	Formula result = this;
	try {
	    Map reverseRenames = null;
	    if ( renameMap instanceof Map ) {
		reverseRenames = renameMap;
	    }
	    else {
		reverseRenames = new HashMap();
	    }

	    // First, break the Formula into separate clauses, if
	    // necessary.
	    ArrayList clauses = new ArrayList();
	    if ( isNonEmptyString(this.theFormula) ) {
		if ( this.listP() ) {
		    String arg0 = this.car();
		    if ( arg0.equals("and") ) {
			Formula restF = this.cdrAsFormula();
			while ( !(restF.empty()) ) {
			    String fStr = restF.car();
			    Formula newF = new Formula();
			    newF.read( fStr );
			    clauses.add( newF );
			    restF = restF.cdrAsFormula();
			}
		    }
		}
		if ( clauses.isEmpty() ) {
		    clauses.add( this );
		}

		// 'Standardize apart' by renaming the variables in
		// each clause.
		int n = clauses.size();
		for ( int i = 0 ; i < n ; i++ ) {
		    HashMap renames = new HashMap();
		    Formula oldClause = (Formula) clauses.remove( 0 );
		    clauses.add( oldClause.standardizeApart_1(renames,reverseRenames) );
		}

		// Construct the new Formula to return.
		if ( n > 1 ) {
		    String theNewFormula = "(and";
		    for ( int i = 0 ; i < n ; i++ ) {
			Formula f = (Formula) clauses.get( i );
			theNewFormula += ( " " + f.theFormula );
		    }
		    theNewFormula += ")";
		    Formula newF = new Formula();
		    newF.read( theNewFormula );
		    result = newF;
		}
		else {
		    result = (Formula) clauses.get( 0 );
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return result;
    }

    /** ***************************************************************
     * This is a helper method for standardizeApart(renameMap).  It
     * assumes that the Formula will be a single clause.
     *
     * @see clausify()
     * @see standardizeApart()
     * @see standardizeApart(Map renameMap)
     *
     * @param renames A Map of correspondences between old variables
     * and new variables.
     *
     * @param reverseRenames A Map of correspondences between new
     * variables and old variables.
     *
     * @return A Formula
     */
    private Formula standardizeApart_1(Map renames, Map reverseRenames) {

	try {
	    if ( isNonEmptyString(this.theFormula) ) {
		if ( this.listP() && !(this.empty()) ) {
		    String arg0 = this.car();
		    Formula arg0F = new Formula();
		    arg0F.read( arg0 );
		    arg0F = arg0F.standardizeApart_1( renames, reverseRenames );
		    return this.cdrAsFormula().standardizeApart_1(renames,reverseRenames).cons(arg0F.theFormula);
		}
		if ( isVariable(this.theFormula) ) {
		    String rnv = (String) renames.get( this.theFormula );
		    if ( !(isNonEmptyString(rnv)) ) {
			rnv = newVar( this.theFormula );
			renames.put( this.theFormula, rnv );
			reverseRenames.put( rnv, this.theFormula );
		    }
		    Formula rnvF = new Formula();
		    rnvF.read( rnv );
		    return rnvF;
		}
	    }
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return this;
    }

    ///////////////////////////////////////////////////////
    /*
      END of clausify() implementation.
    */
    ///////////////////////////////////////////////////////

    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {

	String[] strArr = 
	    { "(forall (?X) (=> (forall (?Y) (P ?X ?Y)) (not (forall (?Y) (=> (Q ?X ?Y) (R ?X ?Y))))))",

	      "TRUE",

	      "FALSE",

	      "(or (instance ?X ?Y) (instance ?X ?Z) (not (instance ?Y Class)) (not (instance ?Z Class)))",

	      "(acquainted GeorgeWBush DickCheney)",

	      "(instance a b)",

	      "(or a b (and c d) (and e f))",

	      "(exists (?X ?Y) (and (instance ?X Man) (instance ?Y Canine) (possesses ?X ?Y)))",

	      "(=> (instance ?X Animal) (not (exists (?Y) (and (instance ?Y Crankshaft) (part ?Y ?X)))))",

	      "(=> (inverse ?REL1 ?REL2) (forall (?INST1 ?INST2) (<=> (holds ?REL1 ?INST1 ?INST2) (holds ?REL2 ?INST2 ?INST1))))",

	      "(<=> (instance ?PHYS Physical) (exists (?LOC ?TIME) (and (located ?PHYS ?LOC) (time ?PHYS ?TIME))))",

	      "(=> (and (disjointRelation ?REL1 ?REL2) (not (equal ?REL1 ?REL2)) (holds ?REL1 @ROW2)) (not (holds ?REL2 @ROW2)))",

	      "(=> (inverse ?REL1 ?REL2) (forall (?INST1 ?INST2) (<=> (?REL1 ?INST1 ?INST2) (?REL2 ?INST2 ?INST1))))",

	      "(<=> (instance ?REL TotalValuedRelation) (exists (?VALENCE) (and (instance ?REL Relation) (valence ?REL ?VALENCE) (=> (forall (?NUMBER ?ELEMENT ?CLASS) (=> (and (lessThan ?NUMBER ?VALENCE) (domain ?REL ?NUMBER ?CLASS) (equal ?ELEMENT (ListOrderFn (ListFn @ROW) ?NUMBER))) (instance ?ELEMENT ?CLASS))) (exists (?ITEM) (holds ?REL @ROW ?ITEM))))))"
	    };
		
	for ( int i = 0 ; i < strArr.length ; i++ ) {
	    System.out.println();
	    System.out.println( "OLD " + i + ": " + strArr[i] );
	    System.out.println();
	    Formula f = new Formula();
	    f.read( strArr[i] );
	    System.out.println();
	    // System.out.println( "NEW " + i + ": " + f.clausifyWithRenameInfo() );
	    // System.out.println( "NEW " + i + ": " + f.clausify() );
	    // System.out.println( "NEW " + i + ": " + f.clausify().operatorsOut() );
	    System.out.println();
	    System.out.println( "PREP " + i + ": " + f.prepareIndexedQueryLiterals() );
	    System.out.println();
	}

	/*
	for ( int i = 0 ; i < strArr.length ; i++ ) {
	    Formula f = new Formula();
	    f.read( strArr[i] );
	    System.out.println();
	    f.gatherPredVars();
	    System.out.println();
	}
	*/

	/*
        KB kb = new KB("C:\\SourceForge\\KBs\\Merge.kif","");
        Formula f = new Formula();
        f.read("(=> (and (wears ?A ?C) (part ?P ?C)) (wears ?A ?P))");
        System.out.println(f.addTypeRestrictions(kb));
        */

        //f.read("(documentation Foo \"Blah, blah blah.\")");
        //System.out.println(f.getArgument(5));
        /* System.out.println(f.parseList("(=> (holds__ contraryAttribute ?ROW1) (holds__ foo ?ROW1)) " +
                                       "(=> (holds__ contraryAttribute ?ROW1 ?ROW2) (holds__ foo ?ROW1 ?ROW2)) " +
                                       "(=> (holds__ contraryAttribute ?ROW1 ?ROW2 ?ROW3) (holds__ foo ?ROW1 ?ROW2 ?ROW3)) " +
                                       "(=> (holds__ contraryAttribute ?ROW1 ?ROW2 ?ROW3 ?ROW4) (holds__ foo ?ROW1 ?ROW2 ?ROW3 ?ROW4)) " +
                                       "(=> (holds__ contraryAttribute ?ROW1 ?ROW2 ?ROW3 ?ROW4 ?ROW5) (holds__ foo ?ROW1 ?ROW2 ?ROW3 ?ROW4 ?ROW5)) " +
                                       "(=> (holds__ contraryAttribute ?ROW1 ?ROW2 ?ROW3 ?ROW4 ?ROW5 ?ROW6) (holds__ foo ?ROW1 ?ROW2 ?ROW3 ?ROW4 ?ROW5 ?ROW6)) " +
                                       "(=> (holds__ contraryAttribute ?ROW1 ?ROW2 ?ROW3 ?ROW4 ?ROW5 ?ROW6 ?ROW7) (holds__ foo ?ROW1 ?ROW2 ?ROW3 ?ROW4 ?ROW5 ?ROW6 ?ROW7))"));
                                       */
    }

}
