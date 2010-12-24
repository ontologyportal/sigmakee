
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

import java.io.*;
import java.util.*;

/** ***************************************************************
 * A single formula in conjunctive normal form (CNF), which is
 * actually a set of (possibly negated) clauses surrounded by an
 * "or".
 */
public class CNFFormula implements Comparable {

    public static TreeMap<String, Integer> termMap = new TreeMap();
    public static TreeMap<Integer, String> intToTermMap = new TreeMap();
    public Formula sourceFormula = null;
    public TreeSet<CNFClause> clauses = new TreeSet();

    /* All the functional terms that are in this formula.
       Key numbering starts at 101. */
    public TreeMap<Integer, CNFClause> functions = new TreeMap();

    /* The String representation of this formula */
    public String stringRep = null;

    /** *************************************************************
     */
    public CNFFormula() {
    }

    /** *************************************************************
     *  Create an instance of this class from a Formula in CNF
     */
    public CNFFormula(Formula f) {

        boolean _CREATE_DEBUG = false;

        if (_CREATE_DEBUG) System.out.println("INFO in CNFFormula(): creating formula from: \n" + f);
        Formula fnew = new Formula();
        fnew.read(f.theFormula);
        sourceFormula = f;
        TreeMap<String, Integer> varMap = new TreeMap();
        if (f.isSimpleClause() || f.isSimpleNegatedClause()) {
            CNFClause c = new CNFClause(f,varMap,functions);
            if (_CREATE_DEBUG) System.out.println("INFO in CNFFormula(): functions: " + printFunctions());
            clauses.add(c);
        }
        else {
            if (!fnew.car().equals("or")) {
                System.out.println("Error in CNFFormula(): formula not in CNF: " + f);
                return;
            }
            fnew.read(fnew.cdr());  // get rid of the enclosing "or"
            while (!fnew.empty() && fnew.car() != null) {
                Formula clause = new Formula();
                clause.read(fnew.car());
                if (_CREATE_DEBUG) System.out.println("INFO in CNFFormula(): creating clause from: \n" + clause);
                if (_CREATE_DEBUG) System.out.println("INFO in CNFFormula(): clauses size: " + clauses.size());
                clauses.add(new CNFClause(clause,varMap,this.functions));
                if (_CREATE_DEBUG) System.out.println("INFO in CNFFormula(): functions: " + printFunctions());
                fnew.read(fnew.cdr());
            }
        }

        if (_CREATE_DEBUG) System.out.println("INFO in CNFFormula(): Finished creating formula from: \n" + f +
                           "\n" + this.asIntegerList());
    }
    
    /** *************************************************************
     *  A convenience routine for creating a CNFFormula.  It does
     *  not convert a Formula into CNF
     */
    public void read(String s) {

        Formula f = new Formula();
        f.read(s);
        CNFFormula cnf = new CNFFormula(f);
        clauses = cnf.clauses;
        functions = cnf.functions;
        sourceFormula = cnf.sourceFormula;
        stringRep = null;
    }

    /** *************************************************************
     *  A convenience routine for creating a CNFFormula.  It assumes
     *  that a clausified version of the parameter yields a single
     *  formula.
     */
    public void readNonCNF(String s) {

        Formula f = new Formula();
        f.read(s);
        f.read(Clausifier.clausify(f).theFormula);
        CNFFormula cnf = new CNFFormula(f);
        clauses = cnf.clauses;
        functions = cnf.functions;
        sourceFormula = cnf.sourceFormula;
        stringRep = null;
    }

    /** ***************************************************************
     *  Compares this object with the specified object for order.
     *  @return a negative integer, zero, or a positive integer as
     *  this object is less than, equal to, or greater than the
     *  specified object.
    */
    public int compareTo(Object cc) throws ClassCastException {

        if (!cc.getClass().getName().equalsIgnoreCase("com.articulate.sigma.CNFFormula")) 
            throw new ClassCastException("Error in CNFFormula.compareTo(): "
                                         + "Class cast exception for argument of class: " 
                                         + cc.getClass().getName());
        Iterator it = clauses.iterator();
        CNFFormula argForm = (CNFFormula) cc;
        Iterator it2 = argForm.clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            if (it2.hasNext()) {
                CNFClause cArg = (CNFClause) it2.next();
                int res = c.compareTo(cArg);
                if (res != 0) 
                    return res;
            }
            else 
                return 1;
        }
        if (it2.hasNext()) 
            return -1;
        return 0;
    }

    /** *************************************************************
     *  A convenience routine that makes sure the cached string
     *  representation is erased when the formula changes.
     */
    public void removeClause(CNFClause c) {

        clauses.remove(c);
        stringRep = null;
    }

    /** *************************************************************
     *  A convenience routine that makes sure the cached string
     *  representation is erased when the formula changes.
     */
    public void addClause(CNFClause c, CNFFormula f) {

        clauses.add(c);
        functions.putAll(f.functions);
        stringRep = null;
    }

    /** *************************************************************
     *  A convenience routine that makes sure the cached string
     *  representation is erased when the formula changes.
     */
    public void addAllClauses(CNFFormula f) {

        clauses.addAll(f.clauses);
        functions.putAll(f.functions);
        stringRep = null;
    }

    /** *************************************************************
     *  A convenience routine that makes sure the cached string
     *  representation is erased when the formula changes.
     */
    public void addAllFunctions(TreeMap<Integer, CNFClause> functions) {

        functions.putAll(functions);
        stringRep = null;
    }

    /** ***************************************************************
     * If equals is overridedden, hashCode must use the same
     * "significant" fields.
     */
    public int hashCode() {

        return (clauses.hashCode());
    }
    
    /** ***************************************************************
     */
    public int size() {

        int result = 0;
        Iterator it = clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            result = result + c.size(functions);
        }
        return result;
    }
    
    /** ***************************************************************
     */
    public boolean equals(CNFFormula f) {

        boolean _EQUALS_DEBUG = false;

        if (_EQUALS_DEBUG) System.out.println("INFO in CNFFormula.equals(): this:\n" + this + "\n f:\n" + f);
        if (clauses.size() != f.clauses.size()) {
            if (_EQUALS_DEBUG) System.out.println("INFO in CNFFormula.equals(): not equal - non-equal number of clauses");
            return false;
        }
        CNFFormula f1 = this.deepCopy();
        CNFFormula f2 = f.deepCopy();

        if (_EQUALS_DEBUG) System.out.println("INFO in CNFFormula.equals(): first f1:\n" + f1 + "\n f2:\n" + f2);
        if (_EQUALS_DEBUG) System.out.println("INFO in CNFFormula.equals(): as lists:\n" + f1.asIntegerList() + "\n f2:\n" + f2.asIntegerList());
        f1 = f1.normalizeVariables();
        f2 = f2.normalizeVariables();
        f2 = f1.unifyFunctionScope(f2);
        if (_EQUALS_DEBUG) System.out.println("INFO in CNFFormula.equals(): after normalize f1:\n" + f1 + "\n f2:\n" + f2);
        if (_EQUALS_DEBUG) System.out.println("INFO in CNFFormula.equals(): as lists:\n" + f1.asIntegerList() + "\n f2:\n" + f2.asIntegerList());
        Iterator it = f1.clauses.iterator();
        while (it.hasNext()) {
            CNFClause clause = (CNFClause) it.next();
            if (_EQUALS_DEBUG) System.out.println("INFO in CNFFormula.equals(): checking clause \n" + clause.toString(f1.functions));
            boolean found = false;
            Iterator it2 = f2.clauses.iterator();
            while (it2.hasNext()) {
                CNFClause c2 = (CNFClause) it2.next();
                if (clause.deepEquals(c2,f1.functions,f2.functions)) 
                    found = true;
            }
            if (!found) {
                if (_EQUALS_DEBUG) System.out.println("INFO in CNFFormula.equals(): clause \n" + clause.toString(f1.functions) +
                                   "\nnot found in \n" + f2);
                return false;
            }
        }
        if (_EQUALS_DEBUG) System.out.println("INFO in CNFFormula.equals(): equal");
        return true;
    }

    /** ***************************************************************
     *  Utility debugging method to print all functions, whether
     *  used or not.
     */
    public static String printFunctions(TreeMap<Integer, CNFClause> functions) {

        if (functions == null) {
            System.out.println("Error in CNFFormula.printFunctions(): function list is null");
            return "";
        }
        StringBuffer result = new StringBuffer();
        Iterator it = functions.keySet().iterator();
        while (it.hasNext()) {
            Integer key = (Integer) it.next();
            CNFClause cnf = (CNFClause) functions.get(key);
            if (cnf == null) 
                result.append(key + ":null");
            else
                result.append(key + ":" + cnf.toString(functions));
        }
        return result.toString();
    }

    /** ***************************************************************
     *  Collect all the indices of functions actually referenced in
     *  the formula.
     */
    public ArrayList<Integer> collectFunctionIndices() {

        ArrayList<Integer> result = new ArrayList();
        Iterator it = clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            result.addAll(c.collectFunctionIndices());
        }

        Iterator it2 = functions.keySet().iterator();
        while (it2.hasNext()) {
            Integer key = (Integer) it2.next();
            CNFClause c = (CNFClause) functions.get(key);
            result.addAll(c.collectFunctionIndices());
        }
        return result;
    }

    /** ***************************************************************
     *  Remove any function whose index doesn't appear in the
     *  formula.
     */
    public void removeUnusedFunctions() {

        ArrayList<Integer> indices = collectFunctionIndices();
        TreeSet<Integer> keys = new TreeSet(functions.keySet());
        Iterator it = keys.iterator();
        while (it.hasNext()) {
            Integer i = (Integer) it.next();
            if (!indices.contains(i)) 
                functions.remove(i);            
        }
    }

    /** ***************************************************************
     *  Utility debugging method to print all functions, whether
     *  used or not.
     */
    public String printFunctions() {

        return CNFFormula.printFunctions(functions);
    }

    /** ***************************************************************
     * Convert to a String.
     */
    public String toStringFormat(boolean format) {

        //System.out.println("INFO in CNFFormula.toStringFormat(): format: " + format);
        //System.out.println("INFO in CNFFormula.toStringFormat(): functions: " + printFunctions());
        if (stringRep != null) 
            return stringRep;
        if (empty()) 
            return "()";
        int indent = 0;
        StringBuffer result = new StringBuffer();
        if (clauses.size() > 1) {
            result.append("(or");                     
            //if (format) 
            //    result.append("true");
            //else
            //    result.append("false");            
            if (format)             
                result.append("\n");
            else
                result.append(" ");
            indent = 2;
        }
        Iterator it = clauses.iterator();
        int clauseNum = 0;
        while (it.hasNext()) {
            if (clauseNum != 0) {
                if (format)             
                    result.append("\n");
                else
                    result.append(" ");
            }
            clauseNum++;
            CNFClause clause = (CNFClause) it.next();
            //System.out.println("INFO in CNFFormula.toStringFormat(): format(2): " + format);
            if (format)             
                result.append(clause.toStringFormat(true,indent,this.functions,0));
            else
                result.append(clause.toStringFormat(false,0,this.functions,0));
        }
        if (clauses.size() > 1) 
            result.append(")");        
        stringRep = result.toString();
        return stringRep;
    }

    /** ***************************************************************
     * Convert to a String of integer indexes.
     */
    public String asIntegerList() {

        //System.out.println("INFO in CNFFormula.toStringFormat(): format: " + format);
        //System.out.println("INFO in CNFFormula.toStringFormat(): functions: " + printFunctions());
        int indent = 0;
        StringBuffer result = new StringBuffer();
        if (clauses.size() > 1) {
            result.append("(or");                     
            //if (format) 
            //    result.append("true");
            //else
            //    result.append("false");                        
                result.append("\n");
            indent = 2;
        }
        Iterator it = clauses.iterator();
        int clauseNum = 0;
        while (it.hasNext()) {
            //if (clauseNum != 0) {            
            //    result.append("\n");
            //}
            clauseNum++;
            CNFClause clause = (CNFClause) it.next();
            //System.out.println("INFO in CNFFormula.toStringFormat(): format(2): " + format);             
            result.append(clause.asIntegerList(indent,this.functions));
        }
        if (clauses.size() > 1) 
            result.append(")");        
        return result.toString();
    }

    /** ***************************************************************
     * Convert to a String.
     */
    public String toString() {
        return toStringFormat(true);
    }

    /** ***************************************************************
     * Perform a copy as a side effect from the parameter to this.
     */
    public void copy(CNFFormula f) {

        //System.out.println("INFO in CNFFormula.copy(): argument: \n" + f);
        clauses = new TreeSet();
        Iterator it = f.clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            clauses.add(c.deepCopy());
        }
        Iterator it2 = f.functions.keySet().iterator();
        while (it2.hasNext()) {
            Integer key = (Integer) it2.next();
            CNFClause c = (CNFClause) f.functions.get(key);
            functions.put(key,c.deepCopy());
        }
        sourceFormula = f.sourceFormula;
        stringRep = f.stringRep;
        //System.out.println("INFO in CNFFormula.copy(): this: \n" + this);
    }

    /** ***************************************************************
     * Copy
     */
    public CNFFormula deepCopy() {

        return deepCopy(this);
    }

    /** ***************************************************************
     * Copy
     */
    public CNFFormula deepCopy(CNFFormula f) {

        CNFFormula result = new CNFFormula();

        Iterator it = f.clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            result.clauses.add(c.deepCopy());
        }
        if (f.functions != null) {
            Iterator it2 = f.functions.keySet().iterator();
            while (it2.hasNext()) {
                Integer key = (Integer) it2.next();
                CNFClause c = (CNFClause) f.functions.get(key);
                result.functions.put(key,c.deepCopy());
            }
        }
        result.sourceFormula = f.sourceFormula;
        result.stringRep = f.stringRep;
        return result;
    }

    /** ***************************************************************
     *  The empty list in this data structure is an empty clause
     *  list.
     */
    public boolean empty() {

        //System.out.println("INFO in CNFFormula.empty(): " + this);
        if (clauses != null && clauses.size() < 1) 
            return true;
        else
            return false;
    }

    /** ***************************************************************
     *  A convenience routine that gets the first clause in a
     *  formula
     *  @return the clause (not a copy of the clause)
     */
    public CNFClause firstClause() {

        if (clauses.size() < 1) {
            System.out.println("Error in CNFFormula.firstClause(): empty clause list");
            return null;
        }
        return (CNFClause) clauses.first();
    }

    /** ***************************************************************
     */
    public boolean isGround() {

        Iterator it = clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            if (!c.isGround(functions)) 
                return false;            
        }
        return true;
    }

    /** ***************************************************************
     */
    public static boolean isLogicalOperator(Integer t) {

        String s = (String) CNFFormula.intToTermMap.get(t);
        if (s == null) {
            System.out.println("Error in CNFFormula.isLogicalOperator(): term index " + t + " not found.");
            return false;
        }
        return Formula.isLogicalOperator(s);
    }

    /** ***************************************************************
     */
    public static boolean isVariable(Integer t) {

        return CNFClause.isVariable(t.intValue());
    }

    /** ***************************************************************
     * Get all the indexes of variables in the formula
     */
    public ArrayList<Integer> collectVariables() {

        //System.out.println("INFO in CNFFormula.collectVariables(): " + this);
        //System.out.println("INFO in CNFFormula.collectVariables(): functions: " + printFunctions());
        ArrayList<Integer> result = new ArrayList();
        Iterator it = clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            ArrayList<Integer> cRes = c.collectVariables();
            Iterator it2 = cRes.iterator();
            while (it2.hasNext()) {
                Integer num = (Integer) it2.next();
                if (!result.contains(num)) 
                    result.add(num);
            }
        }

        //System.out.println("INFO in CNFFormula.collectVariables(): collecting from functions" + printFunctions());
        Iterator it2 = functions.keySet().iterator();
        while (it2.hasNext()) {
            Integer key = (Integer) it2.next();
            CNFClause c = (CNFClause) functions.get(key);
            ArrayList<Integer> cRes = c.collectVariables();
            Iterator it3 = cRes.iterator();
            while (it3.hasNext()) {
                Integer num = (Integer) it3.next();
                if (!result.contains(num)) 
                    result.add(num);
            }
        }
        //System.out.println("INFO in CNFFormula.collectVariables(): var:" + result);
        return result;
    }

    /** ***************************************************************
     * Get all the indexes of terms in the formula
     */
    public ArrayList<Integer> collectTerms() {

        ArrayList<Integer> result = new ArrayList();
        Iterator it = clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            result.addAll(c.collectTerms(functions));
        }
        Iterator it2 = functions.keySet().iterator();
        while (it2.hasNext()) {
            Integer key = (Integer) it2.next();
            CNFClause c = (CNFClause) functions.get(key);
            result.addAll(c.collectTerms(functions));
        }
        return result;
    }

    /** ***************************************************************
     *  Set pointers as a side effect
     */
    public void setTermPointers(TreeMap<Integer, ArrayList<CNFFormula>> posTermPointers,
                                TreeMap<Integer, ArrayList<CNFFormula>> negTermPointers) {

        Iterator it = clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            c.setTermPointers(posTermPointers,negTermPointers,this,c.negated);
        }
    }

    /** **************************************************************
     *  Instantiate variables according to the parameter
     */
    public CNFFormula substituteVariables(TreeMap<Integer,Integer> varMap) {

        System.out.println("Error in CNFFormula.substituteVariables(): deprecated");

        System.out.println("INFO in CNFFormula.substituteVariables(): this as list:\n" + this.asIntegerList());
        System.out.println("INFO in CNFFormula.substituteVariables(): intToTermMap " + intToTermMap);
        System.out.println("INFO in CNFFormula.substituteVariables(): this " + this.toString() +
                           " with map " + varMap);
        CNFFormula result = new CNFFormula();
        Iterator it = clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            result.clauses.add(c.substituteVariables(varMap));
        }

        System.out.println(result.asIntegerList());
        TreeSet temp = new TreeSet(functions.keySet());
        Iterator it2 = temp.iterator();
        while (it2.hasNext()) {
            Integer key = (Integer) it2.next();
            CNFClause c = (CNFClause) functions.get(key);
            CNFClause cnew = c.substituteVariables(varMap);
            System.out.println("INFO in CNFFormula.substituteVariables(): Replaced\n" + c.asIntegerList(null) +
                               "\n with \n" + cnew.asIntegerList(null) + "\n at " + key);
            result.functions.put(key,cnew);
            System.out.println(result.asIntegerList());
        }
        result.sourceFormula = sourceFormula;
        result.stringRep = null; // string representation has changed

        System.out.println("INFO in CNFFormula.substituteVariables(): result\n" + result);
        System.out.println(result.asIntegerList());
        return result;
    }

    /** **************************************************************
     *  Normalize variables so they are numbered in order of
     *  appearance.
     */
    public CNFFormula normalizeVariables() {

        //System.out.println("INFO in CNFFormula.normalizeVariables(): " + this);
        int counter = 0;
        ArrayList<Integer> varList = collectVariables();
        //System.out.println("INFO in CNFFormula.normalizeVariables(): variables : " + varList);
        TreeMap<Integer,Integer> vars = new TreeMap<Integer,Integer>();
        boolean renumbered = false;
        for (int i = 0; i < varList.size(); i++) {
            Integer varNum = (Integer) varList.get(i);
            counter++;
            vars.put(varNum,new Integer(counter));
            if (varNum.intValue() != counter) 
                renumbered = true;
        }
        //System.out.println("INFO in CNFFormula.normalizeVariables(): Attempting to renumber : " + this + " with " + vars);
        if (renumbered) 
            return substitute(vars);
        else
            return this;
    }

    /** **************************************************************
     *  Create constants to fill variables.
     */
    public CNFFormula instantiateVariables() {

        ArrayList<Integer> varList = collectVariables();
        TreeMap<Integer,Integer> vars = new TreeMap<Integer,Integer>();
        for (int i = 0; i < varList.size(); i++) {
            Integer varNum = (Integer) varList.get(i);
            STP2._GENSYM_COUNTER++;
            String value = "GenSym" + String.valueOf(STP2._GENSYM_COUNTER);
            int termNum = CNFClause.encodeTerm(value);
            vars.put(varNum,new Integer(termNum));
        }
        return instantiateVariables(vars);
    }

    /** **************************************************************
     *  Makes the values of a map into the keys and vice versa
     */
    public static TreeMap<Integer,Integer> reverseMapping(TreeMap<Integer,Integer> m) {

        TreeMap<Integer,Integer> newM = new TreeMap();      // result to be returned
        Iterator it = m.keySet().iterator();
        while (it.hasNext()) {
            Integer key = (Integer) it.next();
            Integer value = (Integer) m.get(key);
            newM.put(value,key);
        }
        return newM;
    }

    /** ***************************************************************
     *  Alter clauses so that their function indexes have shared scope,
     *  which in effect means that they are different.  This will be
     *  largely unaltered, but will have the functions from the
     *  argument included (although not referenced) in its
     *  function list.
     */
    public CNFFormula unifyFunctionScope(CNFFormula f) {

        boolean _SCOPE_DEBUG = false;

        if (_SCOPE_DEBUG) System.out.println("INFO in CNFClause.unifyFunctionScope(): before: \n" + this + " and \n" + f); 
        if (_SCOPE_DEBUG) System.out.println("INFO in CNFClause.unifyFunctionScope(): before: \n" + this.asIntegerList() + 
                                             " and \n" + f.asIntegerList()); 
        CNFFormula result = new CNFFormula();        
        result.clauses.addAll(f.clauses);
        int c1FuncSize = this.functions.keySet().size();     // start renumber functions
        int c2FuncSize = f.functions.keySet().size();
        if (c1FuncSize > 0 && c2FuncSize > 0) {     
            TreeMap<Integer, Integer> funcMap = new TreeMap();
                // Since we can't remove the old {key,value} pairs while
                // iterating through them we add the new pairs to a new list
                // then clear the old one, and add them.
            TreeMap<Integer, CNFClause> tempFunctions = new TreeMap();
            Iterator it = f.functions.keySet().iterator();
            while (it.hasNext()) {
                Integer key = (Integer) it.next();
                CNFClause value = (CNFClause) f.functions.get(key);
                int newKey = CNFClause.FUNCSTARTINDEX;
                while (this.functions.keySet().contains(new Integer(newKey)) || 
                       f.functions.keySet().contains(new Integer(newKey)) || 
                       tempFunctions.keySet().contains(new Integer(newKey))) 
                    newKey++;                
                tempFunctions.put(new Integer(newKey),value);
                funcMap.put(key,new Integer(newKey));
                if (_SCOPE_DEBUG) System.out.println("INFO in CNFFormula.unifyFunctionScope(): renumbering key : " + 
                                                     key + " as " + newKey + " for " + value.toString(f.functions)); 
            }
            result.functions.clear();
            result.functions.putAll(tempFunctions);           
            result = result.substitute(funcMap);
        }
        else 
            result.functions.putAll(f.functions);        
        if (_SCOPE_DEBUG) System.out.println("INFO in CNFClause.unifyFunctionScope(): middle: \n" + this.asIntegerList() + 
                                             " and \n" + result.asIntegerList()); 
        if (c2FuncSize > 0)
            this.functions.putAll(result.functions);            
        if (c1FuncSize > 0)
            result.functions.putAll(this.functions);        // end renumber functions
        result.stringRep = null; // formula has changed
        if (_SCOPE_DEBUG) System.out.println("INFO in CNFClause.unifyFunctionScope(): after: \n" + this.asIntegerList() + 
                                             " and \n" + result.asIntegerList()); 
        result.stringRep = null; // formula has changed
        return result;
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
    public CNFFormula unifyVariableScope(CNFFormula c2) {

        boolean _SCOPE_DEBUG = false;

        if (_SCOPE_DEBUG) System.out.println("INFO in CNFClause.unifyVariableScope(): before: \n" + this + " and \n" + c2); 

        ArrayList<Integer> varList1 = this.collectVariables();
        if (_SCOPE_DEBUG) System.out.println("INFO in CNFClause.unifyVariableScope(): varlist1: " + varList1); 
        ArrayList<Integer> varList2 = c2.collectVariables();
        if (_SCOPE_DEBUG) System.out.println("INFO in CNFClause.unifyVariableScope(): varlist2: " + varList2); 
        TreeMap<Integer, Integer> varMap = new TreeMap();
        for (int i = 0; i < varList2.size(); i++) {                 // renumber variables
            Integer varNew = (Integer) varList2.get(i);
            while (varList1.contains(varNew) || varList2.contains(varNew)) 
                varNew = new Integer(varNew.intValue() + 1);            
            varMap.put((Integer) varList2.get(i),varNew);
            varList1.add(varNew);
        }
        CNFFormula c2new = c2.deepCopy();
        if (_SCOPE_DEBUG) System.out.println("INFO in CNFClause.unifyVariableScope(): varmap:\n" + varMap); 
        c2new = c2new.substitute(varMap);
        if (_SCOPE_DEBUG) System.out.println("INFO in CNFClause.unifyVariableScope(): middle : " + c2new + " and " + c2new); 
        if (_SCOPE_DEBUG) System.out.println("INFO in CNFClause.unifyVariableScope(): after:\n" + this + " and\n" + c2new); 

        c2new.stringRep = null; // formula has changed
        return c2new;
    }

    /** ***************************************************************
     *  Replace variables with a value as given by the map argument.
     */
    public CNFFormula instantiateVariables(TreeMap<Integer,Integer> m) {

        System.out.println("INFO in CNFFormula.instantiateVariables(): " + this);
        System.out.println(m);        
        TreeMap<Integer,Integer> newmap = m;
        CNFFormula newFormula = new CNFFormula();
        Iterator it = clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            System.out.println("INFO in CNFFormula.instantiateVariables(): before instantiating clause: " + c.toString(functions));
            newFormula.clauses.add(c.instantiateVariables(newmap,functions,true));
            System.out.println("INFO in CNFFormula.instantiateVariables(): after, newFormula " + newFormula);
        }
        newFormula.sourceFormula = sourceFormula;
        newFormula.stringRep = null;
        System.out.println("INFO in CNFFormula.instantiateVariables(): " + newFormula);
        return newFormula;
    }

    /** ************************************************************
     */
    private CNFFormula substituteInternal(TreeMap<Integer,Integer> m) {

        //System.out.println("INFO in CNFFormula.substituteInternal(): Replacing vars in " + this +
        //                   " as per " + m);
        //System.out.println(" with functions from " + printFunctions());       
        CNFFormula newFormula = new CNFFormula();
        Iterator it = clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            //System.out.println("INFO in CNFFormula.substituteInternal(): before instantiating clause: " + 
            //                   c.toString(functions));
            newFormula.clauses.add(c.instantiateVariables(m,functions,true));
            newFormula.functions.putAll(functions);
            //System.out.println("INFO in CNFFormula.substituteInternal(): after, newFormula " + 
            //                   newFormula);
        }

        TreeSet temp = new TreeSet(functions.keySet());
        Iterator it2 = temp.iterator();
        while (it2.hasNext()) {
            Integer key = (Integer) it2.next();
            CNFClause c = (CNFClause) functions.remove(key);
            //System.out.println("INFO in CNFFormula.substituteInternal(): before instantiating function: " + 
            //                   c.toString(functions));
            newFormula.functions.put(key,c.instantiateVariables(m,functions,true));
            //System.out.println("INFO in CNFFormula.substituteInternal(): after, newFormula " + 
            //                   newFormula);
        }
        newFormula.sourceFormula = sourceFormula;
        //System.out.println("INFO in CNFFormula.substituteInternal(): " + newFormula);
        return newFormula;
    }

    /** ************************************************************
     */
    public CNFFormula substitute(TreeMap<Integer,Integer> m) {

        boolean _SUB_DEBUG = false;
        if (_SUB_DEBUG) System.out.println("INFO in CNFFormula.substitute(): Replacing elements in \n" + this +
                           "\n as integer list \n" + this.asIntegerList() +
                           " as per " + m);
        if (_SUB_DEBUG) System.out.println(" with functions from " + printFunctions());       
        CNFFormula newFormula = new CNFFormula();
        Iterator it = clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            if (_SUB_DEBUG) System.out.println("INFO in CNFFormula.substitute(): before substituting clause: " + 
                               c.toString(functions) + "\n as integer list \n" + c.asIntegerList(null));
            newFormula.clauses.add(c.substitute(m));
            //newFormula.functions.putAll(functions);
            newFormula.stringRep = null;
            if (_SUB_DEBUG) System.out.println("INFO in CNFFormula.substitute(): after, newFormula \n" + 
                               newFormula + "\n as integer list \n" + newFormula.asIntegerList());
        }
        if (_SUB_DEBUG) System.out.println("INFO in CNFFormula.substitute(): done with clauses, before substituting functions");
        TreeSet temp = new TreeSet(functions.keySet());
        Iterator it2 = temp.iterator();
        while (it2.hasNext()) {
            Integer key = (Integer) it2.next();
            Integer newkey = null;
            if (m.keySet().contains(key)) 
                newkey = m.get(key);
            else
                newkey = key;
            //CNFClause c = (CNFClause) functions.remove(key);
            CNFClause c = (CNFClause) functions.get(key);
            if (_SUB_DEBUG) System.out.println("INFO in CNFFormula.substitute(): before instantiating function: " + 
                               c.toString(functions));
            newFormula.functions.put(newkey,c.substitute(m));
            newFormula.stringRep = null;
            if (_SUB_DEBUG) System.out.println("INFO in CNFFormula.substitute(): after, newFormula " + 
                              newFormula);
        }
        newFormula.stringRep = null;
        newFormula.sourceFormula = sourceFormula;
        if (_SUB_DEBUG) System.out.println("INFO in CNFFormula.substitute(): \n" + 
                               newFormula + "\n as integer list \n" + newFormula.asIntegerList());
        return newFormula;
    }

    /** ***************************************************************
     *  Use a TreeMap of [varname, value] to substitute value in for
     *  varname wherever it appears in the formula.  This is
     *  iterative, since values can themselves contain varnames.
     *  The substitution is a side effect on the formula, as well as
     *  the formula being returned.  This should contain all
     *  functions that need to be substituted, including those that
     *  do not appear in the formula before substitution.
     *  @return the formula with substitions (as well as in a side
     *          effect)
     *  @param m is the mapping from variables to values.  When the
     *           values are functions, they are numbered as per
     *           variable form, which should only be one clause.
     */
    public CNFFormula substituteOld(TreeMap<Integer,Integer> m) {

        //System.out.println("INFO in CNFFormula.substitute(): Replacing vars in " + this +
        //                   " as per " + m);
        //System.out.println(" with functions from " + printFunctions());
        CNFFormula newForm = null;
        CNFFormula result = null;
        TreeMap<Integer,Integer> newMap = m;
        //System.out.println("here 0 ");
        while (newForm == null || !newForm.equals(this)) {
            newForm = this.deepCopy();
            //System.out.println("INFO in CNFFormula.substitute(): newForm " + newForm);
            //System.out.println("INFO in CNFFormula.substitute(): newForm functions: " + newForm.printFunctions());
            //System.out.println("here 1 ");
            result = substitute(newMap);
            //System.out.println("INFO in CNFFormula.substitute(): Result " + result);
            //System.out.println("INFO in CNFFormula.substitute(): Result functions: " + result.printFunctions());
            clauses = result.deepCopy().clauses;
            //System.out.println("here 2 ");
            functions = result.functions;
            //System.out.println("here 3 ");
        }
        //System.out.println("here 4 ");
        stringRep = null;  // Formula has changed, so removed the cached string rep
        //System.out.println("here 5 ");
        //System.out.println("INFO in CNFFormula.substitute(): Returning " + this);
        return this;
    }

    /** ***************************************************************
     *  Check to see that either every key in the map is a variable
     *  from cArg, or the value is a variable from cArg
     */
    public boolean isSubsumingMap(TreeMap<Integer,Integer> m, CNFClause cArg, TreeMap<Integer, CNFClause> cFunctions) {

        //System.out.println("INFO in CNFFormula.subsumedBy(): checking this:\n" + this + "\nagainst form:\n" + cArg);
        ArrayList<Integer> al = cArg.collectVariables(cFunctions);
        Iterator it = m.keySet().iterator();
        while (it.hasNext()) {
            Integer key = (Integer) it.next();
            Integer value = (Integer) m.get(key);
            if ( ! ((isVariable(key) && al.contains(key)) ||
                    (isVariable(value) && al.contains(value)))) 
                return false;
        }
        return true;
    }

    /** ***************************************************************
     *  The criterion is that the argument can be instantiated so
     *  that all its literals are identical to a subset of the
     *  literals in this formula.  In that case, this formula is
     *  subsumed by the argument and is therefore redundant with it.
     *  @param form is the formula to test whether it subsumes this
     *              formula
     *  @return true if subsumes, false if not
     */
    public boolean subsumedBy(CNFFormula form) {

        boolean _SUB_DEBUG = false;

        if (_SUB_DEBUG) System.out.println("INFO in CNFFormula.subsumedBy(): checking this:\n" + this + "\nagainst form:\n" + form);
        if (_SUB_DEBUG) System.out.println("INFO in CNFFormula.subsumedBy(): checking this:\n" + this.asIntegerList() + 
                                           "\nagainst form:\n" + form.asIntegerList());
        if (form.clauses.size() < 1 || this.clauses.size() < 1) {
            System.out.println("Error in CNFFormula.subsumedBy() attempt to resolve with empty list");
            if (form.clauses.size() > 0) 
                System.out.println("argument: \n" + form);
            if (clauses.size() > 0) 
                System.out.println("this: \n" + this);            
            return false;
        }
        if (form.clauses.size() > this.clauses.size()) {
            if (_SUB_DEBUG) System.out.println("INFO in CNFFormula.subsumedBy() argument has more clauses than this");
            return false;
        }
        CNFFormula thisFormula = this.deepCopy();
        CNFFormula argFormula = form.deepCopy();
        argFormula = thisFormula.unifyVariableScope(argFormula);
        argFormula = thisFormula.unifyFunctionScope(argFormula);

        if (_SUB_DEBUG) System.out.println("INFO in CNFFormula.subsumedBy(): after, checking this:\n" + thisFormula + "\nagainst form:\n" + argFormula);
        if (_SUB_DEBUG) System.out.println("INFO in CNFFormula.subsumedBy(): after, checking this:\n" + thisFormula.asIntegerList() + 
                                           "\nagainst form:\n" + argFormula.asIntegerList());
        while (argFormula.clauses.size() > 0) {
            CNFClause cArg = argFormula.firstClause().deepCopy();
            if (_SUB_DEBUG) System.out.println("INFO in CNFFormula.subsumedBy(): cArg:\n" + cArg.toString(argFormula.functions));
            argFormula.removeClause(cArg);           
            boolean matched = false;
            while (thisFormula.clauses.size() > 0) {
                CNFClause cThis = thisFormula.firstClause().deepCopy();
                if (_SUB_DEBUG) System.out.println("INFO in CNFFormula.subsumedBy(): cThis:\n" + cThis.toString(thisFormula.functions));
                thisFormula.removeClause(cThis);           
                if (_SUB_DEBUG) System.out.println("INFO in CNFFormula.subsumedBy(): here 1");
                if (cThis.negated == cArg.negated) {
                    TreeMap<Integer,Integer> map = cArg.unify(cThis,argFormula.functions,thisFormula.functions);  // see whether arg subsumes this
                    if (_SUB_DEBUG) System.out.println("INFO in CNFFormula.subsumedBy(): here 2");
                    if (map != null && isSubsumingMap(map,cArg,argFormula.functions))
                        matched = true;
                    if (_SUB_DEBUG) System.out.println("INFO in CNFFormula.subsumedBy(): here 3");
                }
            }
            if (matched == false) 
                return false;
        }
        return true;
    }

    /** ***************************************************************
     *  A convenience routine that allows resolve() to be called
     *  with a clause argument, rather than a formula.
     */
    private TreeMap<Integer, Integer> resolve(CNFClause c, TreeMap<Integer, CNFClause> functions, CNFFormula result) {

        CNFFormula f = new CNFFormula();
        f.clauses.add(c);
        f.functions = functions;
        return resolve(f,result);
    }

    /** ***************************************************************
     *  @return a null mapping if resolution fails or if successful
     *  and complete resolution returns a non-null mapping 
     */
    private TreeMap<Integer, Integer> simpleResolve(CNFClause f, TreeMap<Integer, CNFClause> functions,
                                                    CNFFormula result) {

        boolean _RESOLVE_DEBUG = false;
        TreeMap<Integer, CNFClause> argFunctions = new TreeMap(functions);
        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.simpleResolve(): Attempting to resolve : \n" + 
                                               this + "\n with \n" + f.toString(functions));
        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.simpleResolve(): functions : \n" + 
                                               printFunctions(argFunctions));
        if (f.numArgs < 1 || this.clauses.size() < 1) {
            System.out.println("Error in CNFFormula.simpleResolve() attempt to resolve with empty list");
            if (f.numArgs > 0) 
                System.out.println("argument: \n" + f.toString(functions));
            if (clauses.size() > 0) 
                System.out.println("this: \n" + this);
            return null;
        }
        if (this.clauses.size() > 1) {
            System.out.println("Error in CNFFormula.simpleResolve() not a simple formula: \n" + this);
            return null;
        }
        
        if (!f.opposites(firstClause())) {
            System.out.println("Error in CNFFormula.simpleResolve() not opposites: \n" + firstClause() 
                               + "\n and \n" + f);
            return null;
        }
        
        CNFFormula thisFormula = new CNFFormula();
        thisFormula = this.deepCopy();
        TreeMap mapping = new TreeMap();

        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.simpleResolve(): both simple clauses");
        CNFClause thisClause = (CNFClause) this.clauses.first();
        CNFClause argClause = f.deepCopy();
        mapping = thisClause.unify(argClause,this.functions,argFunctions);

        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.simpleResolve(): functions (2): \n" + 
                                               printFunctions(argFunctions));
        if (mapping == null) 
            result = null;                      // failed resolution returns a null mapping, successful and complete resolution returns a non-null mapping
        else {
            result.functions = this.functions;  // any unification over functions must return the functions
        }
        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.simpleResolve(): result: \n" + result);
        if (_RESOLVE_DEBUG && result != null) System.out.println("INFO in CNFFormula.simpleResolve(): successful resolution");
        if (result != null && result.clauses.size() > 0 && result.firstClause() != null && result.functions != null) 
            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.simpleResolve(): number of functions: " + 
                                                   result.functions.size());
        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.simpleResolve(): mapping: " + mapping);            
        return mapping;
    }

    /** ***************************************************************
     *  @return a null mapping if resolution fails, or if successful
     *  and complete, resolution returns a non-null mapping and
     *  empty clause result.  A successful and incomplete resolution
     *  returns a non-null mappings and the remaining un-unified
     *  clauses.  For example (not (p a b)) and (or (p a b) (p b c))
     *  yields an empty but non null mapping and a result of the
     *  left over clause of (p b c).
     */
    private TreeMap<Integer, Integer> resolve(CNFFormula f, CNFFormula result) {

        boolean _RESOLVE_DEBUG = false;

        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve(): Attempting to resolve : \n" + this + "\n with \n" + f);
        if (f.clauses.size() < 1 || this.clauses.size() < 1) {
            System.out.println("Error in CNFFormula.resolve() attempt to resolve with empty list");
            if (f.clauses.size() > 0) 
                System.out.println("argument: \n" + f);
            if (clauses.size() > 0) 
                System.out.println("this: \n" + this);
            return null;
        }
        CNFFormula thisFormula = new CNFFormula();
        thisFormula = this.deepCopy();
        CNFFormula argFormula = new CNFFormula();
        argFormula = f.deepCopy();
        TreeMap mapping = new TreeMap();
        CNFFormula accumulator = new CNFFormula();

        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve(): this is not a simple clause\n" + thisFormula);
        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve(): arg is a simple clauses\n" + argFormula);
        while (thisFormula.clauses.size() > 0) {
            CNFClause clause = thisFormula.firstClause().deepCopy();
            thisFormula.removeClause(clause);
            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve(): checking clause: \n" + 
                                                   clause.toString(thisFormula.functions) +
                                                   " with \n" + argFormula);
            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() this: \n" + this);
            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() thisFormula: \n" + thisFormula +
                                                   "\n" + thisFormula.asIntegerList());
            // if (result != null && _RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve(): result so far: \n" + accumulator);
            // CNFFormula newResult = new CNFFormula();
            if (clause.opposites(argFormula.firstClause())) 
                mapping = argFormula.simpleResolve(clause,thisFormula.functions,result); 
            else
                mapping = null;

            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() this: \n" + this +
                                                   "\n" + this.asIntegerList());
            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() thisFormula: \n" + thisFormula +
                                                   "\n" + thisFormula.asIntegerList());
            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() 2: checked clause: \n" + clause.toString(thisFormula.functions) +
                                                   " with \n" + argFormula);
            if (mapping != null) {  //&& mapping.keySet().size() > 0
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve(): returning: \n" + result);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() argFormula: \n" + argFormula);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() accumulator: \n" + accumulator);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() result functions: \n" + result.printFunctions());
                if (thisFormula.clauses != null && _RESOLVE_DEBUG) 
                    System.out.println("INFO in CNFFormula.resolve() remaining clauses: \n" + thisFormula);
                accumulator.addAllClauses(thisFormula);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() accumulator after adding clauses: \n" + accumulator +
                                                       "\n" + accumulator.asIntegerList());

                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() this: \n" + this);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() 3: checked clause: \n" + 
                                                       clause.toString(thisFormula.functions) + "\n with \n" + argFormula);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() here");
                if (accumulator.firstClause() != null && accumulator.functions != null 
                    && _RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() accumulator functions: \n" + accumulator.printFunctions());
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() mapping: " + mapping);
                accumulator.addAllFunctions(result.functions);
                accumulator = accumulator.substitute(mapping);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() accumulator after substitution: \n" + accumulator);
                //accumulator.substitute(reverseMapping(mapping));
                accumulator = accumulator.deepCopy();
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() 4: checked clause: \n" + 
                                                       clause.toString(thisFormula.functions) + "\n with \n" + argFormula);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() accumulator after copy: \n" + accumulator);
                result.copy(accumulator);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve(): result: \n" + result);
                return mapping;
            }
            else
                accumulator.addClause(clause,thisFormula);      

            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() this: \n" + this);
            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() 5: checked clause: \n" + 
                                                   clause.toString(thisFormula.functions) + " with \n" + argFormula);
        }
        return mapping;
    }

    /** ***************************************************************
     *  Attempt to resolve one formula with another. @return a
     *  TreeMap of (possibly empty) variable substitutions if
     *  successful, null if not. Return CNFFormula "result" as a
     *  side effect, that is the result of the substition (which
     *  could be the empty list), null if not.  An empty list for
     *  this data structure is considered to be an empty TreeSet for
     *  the member variable "clauses".
     * 
     *  @return a null mapping if resolution fails, or if successful
     *  and complete resolution, returns a non-null mapping and
     *  empty clause result.  A successful and incomplete resolution
     *  returns a non-null mappings and the remaining un-unified
     *  clauses.  For example (not (p a b)) and (or (p a b) (p b c))
     *  yields an empty but non null mapping and a result of the
     *  left over clause of (p b c).
     */
    public TreeMap<Integer, Integer> hyperResolve(CNFFormula f, CNFFormula result) {

        boolean _RESOLVE_DEBUG = false;

        CNFFormula accumulator = new CNFFormula();
        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): Attempting to resolve, this: \n" + 
                                               this + "\n with argument f:\n" + f);
        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): Attempting to resolve, this: \n" + 
                                               this.asIntegerList() + "\n with argument f:\n" + f.asIntegerList());
        if (f.clauses.size() < 1 || this.clauses.size() < 1) {
            System.out.println("Error in CNFFormula.hyperResolve() attempt to resolve with empty list");
            return null;
        }
        CNFFormula thisFormula = new CNFFormula();
        CNFFormula argFormula = new CNFFormula();
        argFormula = f.deepCopy();
        thisFormula = this.deepCopy();
        TreeMap mapping = new TreeMap();
        boolean noMapping = true;
        argFormula = thisFormula.unifyVariableScope(argFormula);  // Ensure there are no variables clashes between this and f
        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): After unification of variable scope, thisFormula: \n" + 
                                               thisFormula + "\n with argFormula:\n" + argFormula);
        argFormula = thisFormula.unifyFunctionScope(argFormula);  // Ensure different functions have different indexes and same are same
        if (_RESOLVE_DEBUG) 
            System.out.println("INFO in CNFFormula.hyperResolve(): After unification of function scope, thisFormula: \n" + 
                               thisFormula + "\n with \n" + argFormula);
        if (_RESOLVE_DEBUG) 
            System.out.println("INFO in CNFFormula.hyperResolve(): After unification of function scope, argFormula: \n" + 
                               thisFormula.asIntegerList() + "\n with \n" + argFormula.asIntegerList());

        if ((thisFormula.clauses.size() == 1 && argFormula.clauses.size() == 1) &&
             thisFormula.firstClause().opposites(argFormula.firstClause())) {                       
            mapping = simpleResolve(argFormula.firstClause(),thisFormula.functions,result);
            if (result != null) 
                result.removeUnusedFunctions();
            return mapping;
        }
        else {
            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): not both simple clauses");
            if ((thisFormula.clauses.size() == 1 && argFormula.clauses.size() == 1) &&
                !thisFormula.firstClause().opposites(argFormula.firstClause())) {
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): not opposite clauses.");            
                return null;            
            }
            if (argFormula.clauses.size() == 1) {
                mapping = thisFormula.resolve(argFormula,result); 
                if (result != null) 
                    result.removeUnusedFunctions();
                return mapping;
            }
            else {
                if (clauses.size() == 1) {
                    mapping = argFormula.resolve(thisFormula,result);   
                    if (result != null) 
                        result.removeUnusedFunctions();
                    return mapping;
                }
                else {                                      // both formulas are not a simple clause
                    if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): neither is a simple clause");
                    CNFFormula newResult = new CNFFormula();
                    if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (before loop 3): looping through argFormula's clauses: \n" + argFormula);
                    accumulator.clauses = new TreeSet();
                    while (argFormula.clauses.size() > 0) {
                        CNFClause clause = argFormula.firstClause().deepCopy();
                        argFormula.removeClause(clause);
                        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (loop 3): here 1: ");
                        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (loop 3): checking clause: \n" + 
                                                               clause.toString(argFormula.functions) + "\n with \n" + thisFormula);
                        TreeMap newMapping = thisFormula.resolve(clause,argFormula.functions,newResult);
                        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (returning loop 3): mapping: " + newMapping);
                        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (returning loop 3): argFormula: \n" + argFormula);
                        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (returning loop 3): thisFormula: \n" + thisFormula);
                        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (returning loop 3): accumulator: \n" + accumulator);
                        if (newMapping != null) {  // && newMapping.keySet().size() > 0  // resolution succeeded
                            mapping.putAll(newMapping);  // could still be a problem if a mapping overwrites another...
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (returning loop 3): newResult: \n" + newResult);
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (returning loop 3): adding clauses to accumulator: \n" + 
                                                                   newResult.toString());
                            accumulator.addAllClauses(newResult);
                            accumulator.addAllFunctions(newResult.functions);                            
                            accumulator = accumulator.substitute(mapping);
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() argFormula (before function add): \n" + argFormula);
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() argFormula (before function add): \n" + argFormula.asIntegerList());
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() newResult function: \n" + printFunctions(newResult.functions));
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() newResult: \n" + newResult.asIntegerList());
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() mapping: \n" + mapping);
                            argFormula.addAllFunctions(newResult.functions);
                            argFormula = argFormula.substitute(mapping);
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (returning loop 3): argFormula (after function add): \n" + argFormula);
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (returning loop 3): argFormula (after function add): \n" + argFormula.asIntegerList());
                            thisFormula = newResult.deepCopy();
                            noMapping = false;
                        }
                        else {
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (loop 3): here 2: ");
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): accumulator: \n" + accumulator);
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): adding clause to accumulator: \n" + 
                                                                   clause.toString(argFormula.functions));
                            accumulator.addClause(clause,argFormula); 
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): accumulator after: \n" + accumulator);
                        }
                        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (loop 3): here 3: ");
                        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (loop 3): here 3: thisFormula\n" + thisFormula);
                        accumulator = accumulator.deepCopy();
                        result.copy(accumulator);
                        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (loop 3): here 4: ");
                    }
                    result.addAllClauses(argFormula);
                }
            }
        }
        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): result(4): \n" + result);
        if ((mapping != null && mapping.size() < 1 && !result.empty()) || noMapping) 
            mapping = null;
        if (result != null) 
            result.removeUnusedFunctions();
        return mapping;
    }

    /** ***************************************************************
     * A test method.
     */
    public static void subsumeTest1() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.subsumeTest1()");
        CNFFormula newResult = new CNFFormula();
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();

        cnf1.read("(or (not (p ?X)) (q ?X))");
        cnf2.read("(or (not (p ?X)))");

        System.out.println("INFO in CNFFormula.subsumeTest1(): cnf1: " + cnf1);
        System.out.println("INFO in CNFFormula.subsumeTest1(): cnf2: " + cnf2);
        System.out.println("INFO in CNFFormula.subsumeTest1(): result: " + cnf1.subsumedBy(cnf2));
        System.out.println("INFO in CNFFormula.subsumeTest1(): should be true");
    }

    /** ***************************************************************
     * A test method.
     */
    public static void subsumeTest2() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.subsumeTest2()");
        CNFFormula newResult = new CNFFormula();
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();

        cnf1.read("(or (not (p a)) (q a))");
        cnf2.read("(or (not (p ?X)))");

        System.out.println("INFO in CNFFormula.subsumeTest2(): cnf1: " + cnf1);
        System.out.println("INFO in CNFFormula.subsumeTest2(): cnf2: " + cnf2);
        System.out.println("INFO in CNFFormula.subsumeTest2(): result: " + cnf1.subsumedBy(cnf2));
        System.out.println("INFO in CNFFormula.subsumeTest2(): should be true");
    }

    /** ***************************************************************
     * A test method.
     */
    public static void subsumeTest3() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.subsumeTest3()");
        CNFFormula newResult = new CNFFormula();
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();

        cnf1.read("(or (not (p ?X)) (q a))");
        cnf2.read("(or (not (p a)))");

        System.out.println("INFO in CNFFormula.subsumeTest3(): cnf1: " + cnf1);
        System.out.println("INFO in CNFFormula.subsumeTest3(): cnf2: " + cnf2);
        System.out.println("INFO in CNFFormula.subsumeTest3(): result: " + cnf1.subsumedBy(cnf2));
        System.out.println("INFO in CNFFormula.subsumeTest3(): should be false");
    }

    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest1() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.resolveTest1()");
        Formula f1 = new Formula();
        f1.read("(s O C)");
        Formula f2 = new Formula();
        f2.read("(or (not (s ?X7 C)) (not (s O C)))");
        CNFFormula newResult = new CNFFormula();
        CNFFormula cnf1 = new CNFFormula(f1);
        CNFFormula cnf2 = new CNFFormula(f2);

        CNFFormula target = new CNFFormula();
        target.read("(or (not (s ?V1 C)))");

        System.out.println("INFO in CNFFormula.resolveTest1(): cnf1: " + cnf1.deepCopy());
        System.out.println("INFO in CNFFormula.resolveTest1(): cnf2: " + cnf2.deepCopy());
        System.out.println("INFO in CNFFormula.resolveTest1(): resolution result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula.resolveTest1(): resolution result: " + newResult);
        System.out.println("INFO in CNFFormula.resolveTest1(): target: " + target);

        if (newResult.equals(target)) 
            System.out.println("Successful test");
        else
            System.out.println("Result: \n " + newResult + "\n not equal to target result: \n" + target);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest2() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.resolveTest2()");
        CNFFormula newResult = new CNFFormula();
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();

        cnf1.read("(or (not (attribute ?VAR1 American)) (not (instance ?VAR2 Weapon)) (not (instance ?VAR3 Nation)) (not (attribute ?VAR3 Hostile)) " + 
                  "(not (instance ?VAR4 Selling)) (not (agent ?VAR4 ?VAR1)) (not (patient ?VAR4 ?VAR2)) (not (recipient ?VAR4 ?VAR3)))");
        cnf2.read("(or (agent ?X15 West) (not (possesses Nono ?X16)) (not (instance ?X16 Missile)))");

        System.out.println("INFO in CNFFormula.resolveTest2(): cnf1: " + cnf1);
        System.out.println("INFO in CNFFormula.resolveTest2(): cnf2: " + cnf2);
        System.out.println("INFO in CNFFormula.resolveTest2(): resolution result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula.resolveTest2(): resolution result: " + newResult);
        CNFFormula target = new CNFFormula();
        target.read("(or (not (recipient ?VAR4 ?VAR3)) (not (possesses Nono ?VAR1)) (not (instance ?VAR1 Missile)) " +
                    "(not (patient ?VAR4 ?VAR2)) (not (instance ?VAR4 Selling)) (not (instance ?VAR3 Nation)) " +
                    "(not (instance ?VAR2 Weapon)) (not (attribute ?VAR3 Hostile)) (not (attribute West American)))");

        if (newResult.equals(target)) 
            System.out.println("Successful test");
        else
            System.out.println("Result: \n " + newResult + "\n not equal to target result: \n" + target);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void unifyScopeTest() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.unifyScopeTest()");
        CNFFormula newResult = new CNFFormula();
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();

        cnf1.read("(or (not (attribute ?VAR1 American)) (not (instance ?VAR2 Weapon)) (not (instance ?VAR3 Nation)) (not (attribute ?VAR3 Hostile)) " + 
                  "(not (instance ?VAR4 Selling)) (not (agent ?VAR4 ?VAR1)) (not (patient ?VAR4 ?VAR2)) (not (recipient ?VAR4 ?VAR3)))");
        cnf2.read("(or (agent ?X15 West) (not (possesses Nono ?X16)) (not (instance ?X16 Missile)))");

        System.out.println("INFO in CNFFormula.unifyScopeTest(): cnf1: " + cnf1);
        System.out.println("INFO in CNFFormula.unifyScopeTest(): cnf2: " + cnf2);
        System.out.println("INFO in CNFFormula.unifyScopeTest(): cnf2 result: " + cnf1.unifyVariableScope(cnf2));

    }

    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest3() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.resolveTest3()");
        CNFFormula newResult = new CNFFormula();
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();

        cnf1.read("(or (attribute ?VAR2 Criminal) (not (recipient ?VAR1 ?VAR3)) (not (patient ?VAR1 ?VAR4)) (not (instance ?VAR4 Weapon)) " +
                  "(not (instance ?VAR3 Nation)) (not (instance ?VAR1 Selling)) (not (agent ?VAR1 ?VAR2)) (not (attribute ?VAR3 Hostile)) " +
                  "(not (attribute ?VAR2 American)))");
        cnf2.read("(or (not (recipient ?VAR5 ?VAR3)) (not (patient ?VAR5 ?VAR4)) (not (instance ?VAR5 Selling)) (not (instance ?VAR4 Weapon)) " +
                  "(not (instance ?VAR3 Nation)) (not (agent ?VAR5 ?VAR2)) (not (attribute ?VAR3 Hostile)) (not (attribute ?VAR2 American)))");

        TreeMap<Integer,Integer> valueMap = cnf1.hyperResolve(cnf2,newResult);
        System.out.println("INFO in CNFFormula.resolveTest3(): resolution result mapping: " + valueMap);
        System.out.println("INFO in CNFFormula.resolveTest3(): resolution result: " + newResult);

        if (valueMap == null) 
            System.out.println("Successful test");
        else
            System.out.println("Result not equal to target result");
    }

    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest4() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.resolveTest4()");
        CNFFormula newResult = new CNFFormula();
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();
        cnf1.read("(or (m (SkFn 1 ?VAR1) ?VAR1) (not (i ?VAR1 C)))");
        cnf2.read("(not (m ?VAR1 Org1-1))");

        System.out.println("INFO in CNFFormula.resolveTest4(): resolution result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula.resolveTest4(): resolution result: " + newResult);

        CNFFormula target = new CNFFormula();
        target.read("(not (i Org1-1 C))");

        if (newResult.equals(target)) 
            System.out.println("Successful test");
        else
            System.out.println("Result: \n " + newResult + "\n not equal to target result: \n" + target);
    }


    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest5() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.resolveTest5()");
        CNFFormula newResult = new CNFFormula();
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();
        cnf1.read("(or (i ?VAR2 SOC) (not (s ?VAR1 ?VAR2)))");
        cnf2.read("(or (not (s ?VAR1 C)) (not (i C SOC)) (not (i Org1-1 ?VAR1)) (not (i ?VAR1 SOC)))");
        System.out.println("INFO in CNFFormula.resolveTest5(): resolution result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula.resolveTest5(): resolution result: " + newResult);

        CNFFormula target = new CNFFormula();
        target.read("(or (not (i SOC SOC)) (not (i C SOC)) (not (s SOC C)) (not (s ?VAR2 Org1-1)))");

        if (newResult.equals(target)) 
            System.out.println("Successful test");
        else
            System.out.println("Result: \n " + newResult + "\n not equal to target result: \n" + target);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest6() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.resolveTest6()");
        CNFFormula newResult = new CNFFormula();
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();
        cnf1.read("(or (equal ?VAR2 ?VAR1) (not (equal ?VAR1 ?VAR2)))");
        cnf2.read("(or (not (rangeSubclass ?VAR1 Object)) (not (subclass Object SetOrClass)) " +
                  "(not (equal (AssignmentFn ?VAR1 ?VAR2 ?VAR3 ?VAR4 ?VAR5 ?VAR6 ?VAR7) Hole)) " +
                  "(not (instance Object SetOrClass)) (not (instance Hole SetOrClass)) " +
                  "(not (instance ?VAR1 Function)))");
        System.out.println("INFO in CNFFormula.resolveTest6(): resolution result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula.resolveTest6(): resolution result: " + newResult);

        CNFFormula target = new CNFFormula();
        target.read("(or (not (rangeSubclass ?VAR1 Object)) (not (subclass Object SetOrClass)) " +
                  "(not (equal Hole (AssignmentFn ?VAR1 ?VAR2 ?VAR3 ?VAR4 ?VAR5 ?VAR6 ?VAR7))) " +
                  "(not (instance Object SetOrClass)) (not (instance Hole SetOrClass)) " +
                  "(not (instance ?VAR1 Function)))");

        if (newResult.equals(target)) 
            System.out.println("Successful test");
        else
            System.out.println("Result: \n " + newResult + "\n not equal to target result: \n" + target);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest7() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.resolveTest7()");
        CNFFormula newResult = new CNFFormula();
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();
        cnf1.read("(equal (ListOrderFn (ListFn ?VAR1 ?VAR2) (ListLengthFn (ListFn ?VAR1 ?VAR2))) ?VAR2)");
        cnf2.read("(not (equal (ImmediateFamilyFn ?VAR1) Org1-1))");
        System.out.println("INFO in CNFFormula.resolveTest7(): resolution result mapping (should be null): " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula.resolveTest7(): resolution result: " + newResult);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest8() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.resolveTest8()");
        CNFFormula newResult = new CNFFormula();
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();
        System.out.println("INFO in CNFFormula.resolveTest8(): here 1");
        cnf1.read("(not (equal (ImmediateFamilyFn ?VAR1) Org1-1))");
        System.out.println("INFO in CNFFormula.resolveTest8(): here 2");
        cnf2.read("(or (equal ?VAR2 ?VAR3) (not (equal (SuccessorFn ?VAR2) (SuccessorFn ?VAR3))))");
        System.out.println("INFO in CNFFormula.resolveTest8(): here 3");
        System.out.println("INFO in CNFFormula.resolveTest8(): resolution result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula.resolveTest8(): resolution result: " + newResult);

        CNFFormula target = new CNFFormula();
        target.read("(not (equal (SuccessorFn (ImmediateFamilyFn ?VAR1)) (SuccessorFn Org1-1)))");

        if (newResult.equals(target)) 
            System.out.println("Successful test");
        else
            System.out.println("Result: \n " + newResult + "\n not equal to target result: \n" + target);       
    }

    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest9() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.resolveTest9()");
        CNFFormula newResult = new CNFFormula();
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();
        System.out.println("INFO in CNFFormula.resolveTest9(): here 1");
        cnf1.read("(or (not (graphPart Org1-1 ?VAR1)) (not (instance Org1-1 GraphArc)) (not (instance ?VAR1 GraphPath)) " +
                  "(not (equal (InitialNodeFn Org1-1) ?VAR2)))");
        System.out.println("INFO in CNFFormula.resolveTest9(): here 2");
        cnf2.read("(or (before NegativeInfinity ?VAR1) (equal ?VAR1 NegativeInfinity) " +
                  "(not (instance ?VAR1 TimePoint)))");
        System.out.println("INFO in CNFFormula.resolveTest9(): here 3");
        System.out.println("INFO in CNFFormula.resolveTest9(): resolution result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula.resolveTest9(): resolution result: " + newResult);

        CNFFormula target = new CNFFormula();
        target.read("(or (before NegativeInfinity (InitialNodeFn Org1-1)) " +
                    "(not (instance Org1-1 GraphArc)) (not (instance (InitialNodeFn Org1-1) TimePoint)) " +
                    "(not (instance ?VAR1 GraphPath)) (not (graphPart Org1-1 ?VAR1)))");        
        if (newResult.equals(target)) 
            System.out.println("Successful test");
        else
           System.out.println("Result: \n" + newResult + "\n not equal to target result: \n" + target);          
    }
   
    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest10() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.resolveTest10()");
        CNFFormula newResult = new CNFFormula();
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();
        cnf1.read("(or (not (subclass RealNumber Collection)) (not (subclass RealNumber NonnegativeRealNumber)) " +
                  "(not (equal Org1-1 ?VAR1)) (not (instance Org1-1 RealNumber)) " +
                  "(not (instance NonnegativeRealNumber SetOrClass)) (not (instance ?VAR1 NonnegativeRealNumber)))");
        cnf2.read("(or (equal ?VAR3 (SubtractionFn 0 ?VAR4)) (instance ?VAR4 NonnegativeRealNumber) " +
                  "(not (equal (AbsoluteValueFn ?VAR4) ?VAR3)) (not (instance ?VAR4 RealNumber)) " +
                  "(not (instance ?VAR3 RealNumber)) (not (instance ?VAR3 NonnegativeRealNumber)))");
        System.out.println("INFO in CNFFormula.resolveTest10(): resolution result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula.resolveTest10(): resolution result: " + newResult);
    }
   
    /** ***************************************************************
     * A test method.
     */
    public static void createTest() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.createTest()");
        CNFFormula cnf1 = new CNFFormula();
        System.out.println("INFO in CNFFormula.createTest(): here 1");
        cnf1.read("(or (not (graphPart Org1-1 ?VAR1)) (not (instance Org1-1 GraphArc)) (not (instance ?VAR1 GraphPath)) " +
                  "(not (equal (InitialNodeFn Org1-1) ?VAR2)))");
        System.out.println("INFO in CNFFormula.createTest(): here 2");
    }
   
    /** ***************************************************************
     * A test method.
     */
    public static void createTest2() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.createTest2()");
        CNFFormula cnf1 = new CNFFormula();
        System.out.println("INFO in CNFFormula.createTest2(): here 1");
        cnf1.read("(not (equal (SuccessorFn (ImmediateFamilyFn ?VAR1)) (SuccessorFn ?VAR3)))");
        System.out.println("INFO in CNFFormula.createTest(): here 2");
    }
   

    /** ***************************************************************
     * A test method.
     */
    public static void createTest3() {

        CNFFormula f = new CNFFormula();
        //f.read("(forall (?ROW1 ?ITEM) (equal (ListOrderFn (ListFn ?ROW1 ?ITEM) (ListLengthFn (ListFn ?ROW1 ?ITEM))) ?ITEM))");
        f.read("(equal (ListOrderFn (ListFn ?ROW1 ?ITEM) (ListLengthFn (ListFn ?ROW1 ?ITEM))) ?ITEM)");
        System.out.println("INFO in CNFFormula.createTest3(): formula: " + f);
        System.out.println("INFO in CNFFormula.createTest3(): formula: " + f.asIntegerList());
        System.out.println("INFO in CNFFormula.createTest3(): term map: " + CNFFormula.termMap);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void createTest4() {

        Formula f = new Formula();
        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.createTest4()");
        f.read("(=> (and (i ?X403 SOC) (i ?X404 SOC)) " +
               "(=> (and (s ?X403 ?X404) (i ?X405 ?X403)) (i ?X405 ?X404)))");
        System.out.println("INFO in CNFFormula.createTest4(): formula: \n" + f);
        CNFFormula cnf1 = new CNFFormula(Clausifier.clausify(f));
        System.out.println("INFO in CNFFormula.createTest4(): formula: \n" + cnf1);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void normVarTest() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.normVarTest()");
        CNFFormula cnf1 = new CNFFormula();
        cnf1.read("(or (attribute ?VAR10 Criminal) (attribute ?VAR8 Criminal) (not (instance ?VAR12 Nation)) (not (instance ?VAR9 Selling)) " +
                  "(not (instance ?VAR7 Selling)) (not (instance ?VAR4 Weapon)) (not (patient ?VAR9 ?VAR4)) (not (patient ?VAR7 ?VAR4)) " +
                  "(not (recipient ?VAR9 ?VAR12)) (not (recipient ?VAR7 ?VAR12)) (not (recipient ?VAR7 ?VAR11)) (not (recipient ?VAR7 ?VAR5)) " +
                  "(not (attribute ?VAR12 Hostile)) (not (attribute ?VAR11 Hostile)) (not (attribute ?VAR10 American)) (not (attribute ?VAR8 American)) " +
                  "(not (attribute ?VAR5 Hostile)))");
        System.out.println("INFO in CNFFormula.normVarTest(): " + cnf1);
        System.out.println("INFO in CNFFormula.normVarTest(): renumbered: " + cnf1.normalizeVariables());
    }

    /** ***************************************************************
     * A test method.
     */
    public static void testVarReplace() {

        System.out.println("---------------------------------");
        CNFFormula cnf1 = new CNFFormula();
        cnf1.read("(or (attribute ?VAR10 Criminal) (attribute ?VAR8 Criminal) (not (instance ?VAR12 Nation)) (not (instance ?VAR9 Selling)) " +
                  "(not (instance ?VAR7 Selling)) (not (instance ?VAR4 Weapon)) (not (patient ?VAR9 ?VAR4)) (not (patient ?VAR7 ?VAR4)) " +
                  "(not (recipient ?VAR9 ?VAR12)) (not (recipient ?VAR7 ?VAR12)) (not (recipient ?VAR7 ?VAR11)) (not (recipient ?VAR7 ?VAR5)) " +
                  "(not (attribute ?VAR12 Hostile)) (not (attribute ?VAR11 Hostile)) (not (attribute ?VAR10 American)) (not (attribute ?VAR8 American)) " +
                  "(not (attribute ?VAR5 Hostile)))");
        TreeMap<Integer,Integer> valueMap = new TreeMap();  // temporary mapping from old to new indexes, just changed ones
        valueMap.put(new Integer(1),new Integer(2));
        valueMap.put(new Integer(2),new Integer(4)); 
        valueMap.put(new Integer(3),new Integer(4));
        valueMap.put(new Integer(5),new Integer(7));
        valueMap.put(new Integer(6),new Integer(3));
        valueMap.put(new Integer(9),new Integer(4));
        System.out.println("INFO in CNFFormula.testVarReplace(): valueMap: " + valueMap);
        System.out.println("INFO in CNFFormula.testVarReplace(): reumbered: " + valueMap);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void equalityTest() {

        System.out.println("---------------------------------");
        CNFFormula cnf1 = new CNFFormula();
        cnf1.read("(or (before NegativeInfinity (InitialNodeFn Org1-1)) (not (instance Org1-1 GraphArc)) " +
                  "(not (instance (InitialNodeFn Org1-1) TimePoint)) (not (instance ?VAR1 GraphPath)) " +
                  "(not (graphPart Org1-1 ?VAR1)))");
        System.out.println("INFO in CNFFormula.equalityTest(): cnf1: " + cnf1);
        CNFFormula cnf2 = new CNFFormula();
        cnf2.read("(or (before NegativeInfinity (InitialNodeFn Org1-1)) (not (instance Org1-1 GraphArc)) " +
                  "(not (instance (InitialNodeFn Org1-1) TimePoint)) (not (instance ?VAR1 GraphPath)) " +
                  "(not (graphPart Org1-1 ?VAR1)))");
        TreeMap<Integer,Integer> valueMap = new TreeMap();  // temporary mapping from old to new indexes, just changed ones
        System.out.println("INFO in CNFFormula.equalityTest(): cnf2: " + cnf2);
        System.out.println("INFO in CNFFormula.equalityTest(): equal?: " + cnf2.equals(cnf1));
    }

    /** ***************************************************************
     * A test method.
     */
    public static void oppositesTest1() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.oppositesTest1()");
        CNFFormula newResult = new CNFFormula();
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();
        cnf1.read("(or (agent ?VAR4 West) (not (instance ?VAR6 Missile)) (not (possesses Nono ?VAR6)))");
        cnf2.read("(or (instance ?VAR1 Weapon) (not (instance ?VAR1 Missile)))");
        System.out.println("INFO in CNFFormula2.resolveTest10(): oppositesTest1 result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula2.resolveTest10(): oppositesTest1 result (should be null): " + newResult);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void speedTest() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula.speedTest()");
        long t_start = System.currentTimeMillis();
        long t_elapsed = (System.currentTimeMillis() - t_start) / 1000;
        for (int i = 0; i < 10000; i++) {
            resolveTest3();
            resolveTest4();
            resolveTest6();
            resolveTest7();
            resolveTest8();
            resolveTest9();
        }
        t_elapsed = (System.currentTimeMillis() - t_start) / 1000;
        System.out.println("INFO in CNFFormula.speedTest(): t_elapsed: " + t_elapsed);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {

        /* Formula f = new Formula();
        f.read("(possesses Nono Gogo)");
        System.out.println("Formula to encode: \n");
        System.out.println(f);
        CNFFormula cf = new CNFFormula(f);
        System.out.println("Formula: \n" + cf); 
        Formula f = new Formula();
        f.read("(not (possesses Nono Gogo))");
        System.out.println("Formula to encode: \n");
        System.out.println(f);
        CNFFormula cf = new CNFFormula(f);
        System.out.println("Formula: \n" + cf);
        cf.stringRep = null;
        System.out.println("unformatted: \n" + cf.toStringFormat(false));
        
        f = new Formula();
        f.read("(or (not (possesses Nono (SkFn A))) (not (instance (SkFn A) Missile)) (not (attribute West American)) (not (instance ?VAR2 Weapon)) (not (instance ?VAR3 Nation)) (not (attribute ?VAR3 Hostile)) (not (instance ?X15 Selling)) (not (patient ?X15 ?VAR2)) (not (recipient ?X15 ?VAR3))) ");
        System.out.println("Formula to encode: \n");
        System.out.println(f);
        cf = new CNFFormula(f);
        System.out.println("Formula: \n" + cf); */
        speedTest();
        //resolveTest1();
        //resolveTest2();
        //unifyScopeTest();
        //resolveTest3();
        //resolveTest4();
        //resolveTest5();
        //resolveTest6();
        //resolveTest7();
        //resolveTest8();
        //resolveTest9();
        //resolveTest10();
        //createTest();
        //createTest2();
        //createTest3();
        //createTest4();
        //testVarReplace();
        //normVarTest();
        //equalityTest();
        //subsumeTest1();
        //subsumeTest2();
        //subsumeTest3();
        //oppositesTest1();

    }
}

