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
     */
    private String validArgsRecurse(Formula f) {

        //System.out.println("INFO in Formula.validArgsRecurse(): Formula: " + f.theFormula);
        if (f.theFormula == "" || !f.listP() || f.atom() || f.empty()) return "";
        String pred = f.car();
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
        return result;
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
     * Vampire requires.
     */
    private String translateInequalities(String s) {
        
        if (s.equalsIgnoreCase("greaterThan")) return ">";
        if (s.equalsIgnoreCase("greaterThanOrEqualTo")) return ">=";
        if (s.equalsIgnoreCase("lessThan")) return "<";
        if (s.equalsIgnoreCase("lessThanOrEqualTo")) return "<=";
        return "";
    }

    /** ***************************************************************
     * Makes implicit universal quantification explicit.  May be needed
     * in the future for other theorem provers.
     */
    public String makeQuantifiersExplicit() {
        
        ArrayList quantVariables = new ArrayList();
        ArrayList unquantVariables = new ArrayList();
        //System.out.println("Adding quantified variables.");
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

        //System.out.println("Adding unquantified variables.");
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
            if (!quantVariables.contains(theFormula.substring(startIndex,i).intern()) &&
                !unquantVariables.contains(theFormula.substring(startIndex,i).intern())) {
                unquantVariables.add(theFormula.substring(startIndex,i).intern());                
                //System.out.println(theFormula.substring(startIndex,i));
            }
            startIndex = i;
        }

       if (unquantVariables.size() > 0) {        
            StringBuffer quant = new StringBuffer("(forall (");  // Quantify all the unquantified variables
            for (int i = 0; i < unquantVariables.size(); i++) {
                quant = quant.append((String) unquantVariables.get(i));
                if (i < unquantVariables.size() - 1) 
                    quant = quant.append(" ");
            }
            System.out.println("INFO in Formula.makeQuantifiersExplicit(): result: " + 
                                quant.toString() + ") " + theFormula + ")");
            return quant.toString() + ") " + theFormula + ")";
        }
        else
            return theFormula;
    }

    /** ***************************************************************
     * Expand row variables, keeping the information about the original
     * source formula.  Each variable is treated like a macro that
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
     * 
     * @param rowVarMap is a HashMap with Strings as keys.  The keys are
     * row variable names without the '@'.  The values
     * are null, so it should really be a Set.
     * Note that this routine has a significant bug that appears when
     * row variable names are subsets of one another, for example
     * (foo @ROW @ROW2)
     * @return an ArrayList of Formulas
     */
    private ArrayList expandRowVars(String input, TreeSet rowVars) {

        //System.out.println("INFO in Formula.expandRowVars(): input: " + input);
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
            while (it.hasNext()) {                              // Iterate through the row variables
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
                //System.out.println("INFO in Formula.expandRowVars(): result: " + result);
            }
        }
        ArrayList al = parseList(result.toString());      // Copy the source file information for each expanded formula.
        //System.out.println("INFO in Formula.expandRowVars(): List : " + al);
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
     * Pre-process a formula before sending it to Vampire. This includes
     * ignoring meta-knowledge like documentation strings, translating
     * mathematical operators, quoting higher-order formulas, expanding
     * row variables and prepending the 'holds' predicate.
     * @return an ArrayList of Formula(s)
     */
    public ArrayList preProcess() {

        //String s = makeQuantifiersExplicit();
        String s = theFormula;
        s = s.replaceAll("greaterThan", ">");
        s = s.replaceAll("greaterThanOrEqualTo", ">=");
        s = s.replaceAll("lessThan", "<");
        s = s.replaceAll("lessThanOrEqualTo", "<=");

        Stack predicateStack = new Stack();
        TreeSet rowVars = new TreeSet();   // A list of row variables.
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
                        predicate = predicate.trim();
                        if (predicate.equalsIgnoreCase("documentation") ||
                            predicate.equalsIgnoreCase("format") ||
                            predicate.equalsIgnoreCase("termFormat"))
                            return new ArrayList(0);
                        if (mathOperators.contains(predicate)) {
                            // translate math operators (NYI)
                        }
                        if (lastPredicate != null &&
                            !logicalOperators.contains(lastPredicate.intern()) && 
                            predicate.length() > 1 &&
                            predicate.substring(predicate.length()-2).compareTo("Fn") != 0) {
                            result = result.append('`');
                            // tick the formula
                        }
                        result = result.append(ch); 
                        if (!logicalOperators.contains(predicate.intern()) && 
                            !mathOperators.contains(predicate.intern()) && 
                            !predicate.equalsIgnoreCase("holds")) {
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
                        if (s.charAt(i) == '?' && 
                            !lastPredicate.equalsIgnoreCase("forall") && 
                            !lastPredicate.equalsIgnoreCase("exists") ) {
                            result = result.append(ch);
                            result = result.append("holds ?");
                            lastPredicate = "holds";
                            predicateStack.push("holds");
                        }
                        else {
                            int predicateStart = i;
                            while (s.charAt(i) != ' ' && s.charAt(i) != ')')
                                i++;
                            String predicate = s.substring(predicateStart,i);
                            predicate = predicate.trim();
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
                        rowVars.add(var);
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
        return expandRowVars(result.toString(),rowVars);
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

        s = s.replaceAll("holds ","");
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
     * A test method.
     */
    public static void main(String[] args) {

        Formula f = new Formula();
        f.read("(subAttribute Foo GovernmentPerson)");
        System.out.println(f.toProlog());
        //f.read("(documentation Foo \"Blah, blah blah.\")");
        //System.out.println(f.getArgument(5));
        /* System.out.println(f.parseList("(=> (holds contraryAttribute ?ROW1) (holds foo ?ROW1)) " +
                                       "(=> (holds contraryAttribute ?ROW1 ?ROW2) (holds foo ?ROW1 ?ROW2)) " +
                                       "(=> (holds contraryAttribute ?ROW1 ?ROW2 ?ROW3) (holds foo ?ROW1 ?ROW2 ?ROW3)) " +
                                       "(=> (holds contraryAttribute ?ROW1 ?ROW2 ?ROW3 ?ROW4) (holds foo ?ROW1 ?ROW2 ?ROW3 ?ROW4)) " +
                                       "(=> (holds contraryAttribute ?ROW1 ?ROW2 ?ROW3 ?ROW4 ?ROW5) (holds foo ?ROW1 ?ROW2 ?ROW3 ?ROW4 ?ROW5)) " +
                                       "(=> (holds contraryAttribute ?ROW1 ?ROW2 ?ROW3 ?ROW4 ?ROW5 ?ROW6) (holds foo ?ROW1 ?ROW2 ?ROW3 ?ROW4 ?ROW5 ?ROW6)) " +
                                       "(=> (holds contraryAttribute ?ROW1 ?ROW2 ?ROW3 ?ROW4 ?ROW5 ?ROW6 ?ROW7) (holds foo ?ROW1 ?ROW2 ?ROW3 ?ROW4 ?ROW5 ?ROW6 ?ROW7))"));
                                       */
    }

}
