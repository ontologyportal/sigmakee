
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

import com.sun.org.apache.bcel.internal.generic.NEW;

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
    /* The String representation of this formula */
    public String stringRep = null;

    /** *************************************************************
     *  Create an instance of this class from a Formula in CNF
     */
    public CNFFormula(Formula f) {

        Formula fnew = new Formula();
        fnew.read(f.theFormula);
        sourceFormula = f;
        TreeMap varMap = new TreeMap();
        if (f.isSimpleClause() || f.isSimpleNegatedClause()) {
            CNFClause c = new CNFClause(f,varMap);
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
                clauses.add(new CNFClause(clause,varMap));
                fnew.read(fnew.cdr());
            }
        }
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
        sourceFormula = cnf.sourceFormula;
        stringRep = null;
    }

    /** *************************************************************
     *  A convenience routine for creating a CNFFormula.  It assumes
     *  the a clausified version of the parameter yields a single
     *  formula.
     */
    public void readNonCNF(String s) {

        Formula f = new Formula();
        f.read(s);
        f.read(f.clausify().theFormula);
        CNFFormula cnf = new CNFFormula(f);
        clauses = cnf.clauses;
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
    public void addClause(CNFClause c) {

        clauses.add(c);
        stringRep = null;
    }

    /** *************************************************************
     *  A convenience routine that makes sure the cached string
     *  representation is erased when the formula changes.
     */
    public void addAllClauses(TreeSet<CNFClause> al) {

        clauses.addAll(al);
        stringRep = null;
    }

    /** *************************************************************
     */
    public CNFFormula() {
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
            result = result + c.size();
        }
        return result;
    }
    
    /** ***************************************************************
     */
    public boolean equals(CNFFormula f) {

        if (clauses.size() != f.clauses.size()) 
            return false;
        CNFFormula f1 = this.deepCopy();
        CNFFormula f2 = f.deepCopy();
        f1 = f1.normalizeVariables();
        f2 = f2.normalizeVariables();
        //System.out.println("INFO in CNFFormula.equals(): f1:\n" + f1 + "\n f2:\n" + f2);
        Iterator it = f1.clauses.iterator();
        while (it.hasNext()) {
            CNFClause clause = (CNFClause) it.next();
            if (!f2.clauses.contains(clause)) 
                return false;
        }
        return true;
    }

    /** ***************************************************************
     * Convert to a String.
     */
    public String toStringFormat(boolean format) {

        //System.out.println("INFO in CNFFormula.toStringFormat(): format: " + format);
        if (stringRep != null) 
            return stringRep;
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
                result.append(clause.toStringFormat(true,indent));
            else
                result.append(clause.toStringFormat(false,0));
        }
        if (clauses.size() > 1) 
            result.append(")");        
        stringRep = result.toString();
        return stringRep;
    }

    /** ***************************************************************
     * Convert to a String.
     */
    public String toString() {
        return toStringFormat(true);
    }

    /** ***************************************************************
     * Perform a copy as a side effect
     */
    public void copy(CNFFormula f) {

        //System.out.println("INFO in CNFFormula.copy(): argument: \n" + f);
        clauses = new TreeSet();
        Iterator it = f.clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            clauses.add(c.deepCopy());
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
     *  The empty list in this data structure is a clause list with
     *  a non-null first clause that has an argument list of 0
     *  length.
     */
    public boolean empty() {

        if (clauses == null || clauses.size() < 1 || firstClause() == null) 
            return false;
        return firstClause().numArgs == 0;
    }

    /** ***************************************************************
     * Copy
     */
    public CNFFormula deepCopy(CNFFormula f) {

        CNFFormula result = new CNFFormula();

        Iterator it = clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            result.clauses.add(c.deepCopy());
        }
        result.sourceFormula = f.sourceFormula;
        result.stringRep = f.stringRep;
        return result;
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
            if (!c.isGround()) 
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
            result.addAll(c.collectTerms());
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
     *  Renumber variables according to the parameter
     */
    public CNFFormula renumberVariables(TreeMap<Integer,Integer> varMap) {

        CNFFormula result = new CNFFormula();
        Iterator it = clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            result.clauses.add(c.renumberVariables(varMap));
        }
        result.sourceFormula = sourceFormula;
        return result;
    }

    /** **************************************************************
     *  Normalize variables so they are numbered in order of
     *  appearance.
     */
    public CNFFormula normalizeVariables() {

        int counter = 0;
        ArrayList<Integer> varList = collectVariables();
        //System.out.println("INFO in CNFFormula.normalizeVariables(): varaibles : " + varList);
        TreeMap<Integer,Integer> vars = new TreeMap<Integer,Integer>();
        for (int i = 0; i < varList.size(); i++) {
            Integer varNum = (Integer) varList.get(i);
            counter++;
            vars.put(varNum,new Integer(counter));
        }
        //System.out.println("INFO in CNFFormula.normalizeVariables(): Attempting to renumber : " + this + " with " + vars);
        return renumberVariables(vars);
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
    public TreeMap<Integer,Integer> reverseMapping(TreeMap<Integer,Integer> m) {

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
     *  Alter formulas so that their variable indexes have shared
     *  scope, which in effect means that they are different.  For
     *  example, given (p ?V1 a) and (p c ?V1) the second formula
     *  will be changed to (p c ?V2) because the ?V1 in the original
     *  formulas do not refer to the same variable.  There is no
     *  side effect to this method
     *  @return the argument with its variable numbering altered if
     *          necessary
     */
    public CNFFormula unifyScope(CNFFormula c2) {

        //System.out.println("INFO in CNFClause.unifyScope(): before: \n" + this + " and \n" + c2); 
        //System.out.println("c1 func size: " + c1.functions.size());
        //System.out.println("c2 func size: " + c2.functions.size());

        ArrayList<Integer> varList1 = this.collectVariables();
        ArrayList<Integer> varList2 = c2.collectVariables();
        TreeMap<Integer, Integer> varMap = new TreeMap();
        for (int i = 0; i < varList2.size(); i++) {                 // renumber variables
            Integer varNew = (Integer) varList2.get(i);
            while (varList1.contains(varNew)) 
                varNew = new Integer(varNew.intValue() + 1);            
            varMap.put((Integer) varList2.get(i),varNew);
            varList1.add(varNew);
        }
        CNFFormula c2new = c2.deepCopy();
        //System.out.println("INFO in CNFClause.unifyScope(): varmap:\n" + varMap); 
        c2new = c2new.renumberVariables(varMap);
        //System.out.println("INFO in CNFClause.unifyScope(): middle : " + c1new + " and " + c2new); 
        //System.out.println("INFO in CNFClause.unifyScope(): middle : " + c1new.asIntegerList() + " and " + c2new.asIntegerList()); 
        //System.out.println("INFO in CNFClause.unifyScope(): after:\n" + this + " and\n" + c2new); 
        //System.out.println("INFO in CNFClause.unifyScope(): after : " + c1new.asIntegerList() + " and " + c2new.asIntegerList()); 

        //System.out.println("c1new func size: " + c1new.functions.size());
        //System.out.println("c2new func size: " + c2new.functions.size());
        return c2new;
    }

    /** ***************************************************************
     *  Variable indexes have a scope local to their formulas.  When
     *  a unification occurs, if one variable unifies with another,
     *  we need to ensure the formula doesn't already have another
     *  variable with the same index.  For example, say we have
     *  (or (p ?X1 m) (r ?X1 ?X2)) and (not (p ?X2 m)).  If we then
     *  map ?X1 to ?X2 we'll wind up with (r ?X2 ?X2) after
     *  resolution, which is not correct.
     */
    public TreeMap<Integer,Integer> renumberVariableReplacements(TreeMap<Integer,Integer> m) {

        return m;
        /*
        //System.out.println("INFO in CNFFormula.renumberVariableReplacements(): " + m);
        ArrayList<Integer> varList = collectVariables();
        TreeMap<Integer,Integer> newM = new TreeMap();      // result to be returned
        TreeMap<Integer,Integer> valueMap = new TreeMap();  // temporary mapping from old to new indexes, just changed ones
        Iterator it = m.keySet().iterator();
        while (it.hasNext()) {
            Integer key = (Integer) it.next();
            Integer oldValue = (Integer) m.get(key);
            if (valueMap.keySet().contains(oldValue))       // A revised mapping already exists
                newM.put(key,(Integer) valueMap.get(oldValue));
            else {
                if (varList.contains(oldValue)) {           // There's a clash, renumbering needed.
                    Integer newValue = new Integer(oldValue.intValue());
                    while (varList.contains(newValue) || m.keySet().contains(newValue)) 
                        newValue = new Integer(newValue.intValue() + 1);
                    varList.add(newValue);
                    valueMap.put(oldValue,newValue);
                    newM.put(key,newValue);
                }
                else {                                      // There's no clash, just transfer to the new map
                    newM.put(key,oldValue);
                }
            }                            
        }
        //System.out.println("INFO in CNFFormula.renumberVariableReplacements(): new: " + newM);
        return newM;

        */
    }

    /** ***************************************************************
     *  Replace variables with a value as given by the map argument.
     *  If the replacement value is also a variable, make sure it's
     *  one that doesn't clash with an existing variable index.
     */
    public CNFFormula instantiateVariables(TreeMap<Integer,Integer> m) {

        //System.out.println("INFO in CNFFormula.instantiateVariables(): " + this);
        //System.out.println(m);        
        TreeMap<Integer,Integer> newmap = renumberVariableReplacements(m);
        CNFFormula newFormula = new CNFFormula();
        Iterator it = clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            //System.out.println("INFO in CNFFormula.instantiateVariables(): before instantiating clause: " + c);
            newFormula.clauses.add(c.instantiateVariables(newmap,null));
            //System.out.println("INFO in CNFFormula.instantiateVariables(): after, newFormula " + newFormula);
        }
        newFormula.sourceFormula = sourceFormula;
        //System.out.println("INFO in CNFFormula.instantiateVariables(): " + newFormula);
        return newFormula;
    }

    /** ***************************************************************
     *  @param result is a formula with a single clause that may
     *                contain functions that need to be substituted.
     */
    public CNFFormula substituteInternal(TreeMap<Integer,Integer> m, CNFFormula form) {

        //System.out.println("INFO in CNFFormula.instantiateVariables(): " + this);
        //System.out.println(m);        
        CNFFormula newFormula = new CNFFormula();
        Iterator it = clauses.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            //System.out.println("INFO in CNFFormula.substituteInternal(): before instantiating clause: " + c);
            if (form != null) 
                newFormula.clauses.add(c.instantiateVariables(m,form.firstClause()));
            else
                newFormula.clauses.add(c.instantiateVariables(m,null));
            //System.out.println("INFO in CNFFormula.instantiateVariables(): after, newFormula " + newFormula);
        }
        newFormula.sourceFormula = sourceFormula;
        //System.out.println("INFO in CNFFormula.instantiateVariables(): " + newFormula);
        return newFormula;
    }

    /** ***************************************************************
     *  Use a TreeMap of [varname, value] to substitute value in for
     *  varname wherever it appears in the formula.  This is
     *  iterative, since values can themselves contain varnames.
     *  The substitution is a side effect on the formula, as well as
     *  the formula being returned.
     *  @return the formula with substitions (as well as in a side
     *          effect)
     *  @param form is a formula with a single clause that may
     *                contain functions that need to be substituted.
     */
    public CNFFormula substitute(TreeMap<Integer,Integer> m, CNFFormula form) {

        //System.out.println("INFO in CNFFormula.substitute(): Replacing vars in " + this +
        //                   " as per " + m);
        CNFFormula newForm = null;
        CNFFormula result = null;
        TreeMap<Integer,Integer> newMap = renumberVariableReplacements(m);
        while (newForm == null || !newForm.equals(this)) {
            newForm = this.deepCopy();
            result = substituteInternal(newMap,form);
            //System.out.println("INFO in CNFFormula.substitute(): Result " + result);
            clauses = result.deepCopy().clauses;
        }
        stringRep = null;  // Formula has changed, so removed the cached string rep
        return this;
    }

    /** ***************************************************************
     *  Check to see that either every key in the map is a variable
     *  from cArg, or the value is a variable from cArg
     */
    public boolean isSubsumingMap(TreeMap<Integer,Integer> m, CNFClause cArg) {

        ArrayList<Integer> al = cArg.collectVariables();
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

        if (form.clauses.size() < 1 || this.clauses.size() < 1) {
            System.out.println("Error in CNFFormula.subsumes() attempt to resolve with empty list");
            if (form.clauses.size() > 0) 
                System.out.println("argument: \n" + form);
            if (clauses.size() > 0) 
                System.out.println("this: \n" + this);            
            return false;
        }
        if (form.clauses.size() > this.clauses.size()) {
            //System.out.println("INFO in CNFFormula.subsumes() argument has more clauses than this");
            return false;
        }
        CNFFormula thisFormula = this.deepCopy();
        CNFFormula argFormula = form.deepCopy();
        argFormula = thisFormula.unifyScope(argFormula);

        while (argFormula.clauses.size() > 0) {
            CNFClause cArg = argFormula.firstClause().deepCopy();
            argFormula.removeClause(cArg);           
            boolean matched = false;
            while (thisFormula.clauses.size() > 0) {
                CNFClause cThis = thisFormula.firstClause().deepCopy();
                thisFormula.removeClause(cThis);           
                if (cThis.negated == cArg.negated) {
                    TreeMap<Integer,Integer> map = cArg.unify(cThis);  // see whether arg subsumes this
                    if (map != null && isSubsumingMap(map,cArg))
                        matched = true;
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
    private TreeMap<Integer, Integer> resolve(CNFClause c, CNFFormula result) {

        CNFFormula f = new CNFFormula();
        f.clauses.add(c);
        return resolve(f,result);
    }

    /** ***************************************************************
     *  @return a null mapping if resolution fails or if successful
     *  and complete resolution returns a non-null mapping 
     */
    private TreeMap<Integer, Integer> simpleResolve(CNFClause f, CNFFormula result) {

        boolean _RESOLVE_DEBUG = false;

        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.simpleResolve(): Attempting to resolve : \n" + this + "\n with \n" + f);
        if (f.numArgs < 1 || this.clauses.size() < 1) {
            System.out.println("Error in CNFFormula.simpleResolve() attempt to resolve with empty list");
            if (f.numArgs > 0) 
                System.out.println("argument: \n" + f);
            if (clauses.size() > 0) 
                System.out.println("this: \n" + this);            
            return null;
        }
        if (this.clauses.size() > 1) {
            System.out.println("Error in CNFFormula.simpleResolve() not a simple formula: \n" + this);
            return null;
        }
        
        if (!f.opposites(firstClause())) {
            System.out.println("Error in CNFFormula.simpleResolve() not opposites: \n" + firstClause() + "\n and \n" + f);
            return null;
        }
        
        CNFFormula thisFormula = new CNFFormula();
        thisFormula = this.deepCopy();
        TreeMap mapping = new TreeMap();

        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.simpleResolve(): both simple clauses");
        CNFClause thisClause = (CNFClause) this.clauses.first();
        CNFClause argClause = f.deepCopy();
        mapping = thisClause.unify(argClause);
        if (mapping == null) 
            result = null;                      // failed resolution returns a null mapping, successful and complete resolution returns a non-null mapping
        else {
            CNFClause c = new CNFClause();
            c.functions = thisClause.functions;  // any unification over functions must return the functions
            result.clauses = new TreeSet();
            result.clauses.add(c);
        }
        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.simpleResolve(): result: \n" + result);
        if (_RESOLVE_DEBUG && result != null) System.out.println("INFO in CNFFormula.simpleResolve(): successful resolution");
        if (result != null && result.firstClause() != null && result.firstClause().functions != null) 
            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.simpleResolve(): number of functions: " + result.firstClause().functions.size());
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
            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve(): checking clause: \n" + clause +
                                                   " with \n" + argFormula);
            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() this: \n" + this);
            // if (result != null && _RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve(): result so far: \n" + accumulator);
            // CNFFormula newResult = new CNFFormula();
            if (clause.opposites(argFormula.firstClause())) 
                mapping = argFormula.simpleResolve(clause,result); 
            else
                mapping = null;

            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() this: \n" + this);
            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() 2: checked clause: \n" + clause +
                                                   " with \n" + argFormula);
            if (mapping != null) {  //&& mapping.keySet().size() > 0
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve(): returning: \n" + result);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() argFormula: \n" + argFormula);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() accumulator: \n" + accumulator);
                if (result.firstClause() != null && result.firstClause().functions != null 
                    && _RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() result functions: \n" + result.firstClause().functions);
                if (thisFormula.clauses != null && _RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() remaining clauses: \n" + thisFormula.clauses);
                accumulator.addAllClauses(thisFormula.clauses);

                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() this: \n" + this);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() 3: checked clause: \n" + clause +
                                                       " with \n" + argFormula);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() accumulator after adding clauses: \n" + accumulator);
                if (accumulator.firstClause() != null && accumulator.firstClause().functions != null 
                    && _RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() accumulator functions: \n" + accumulator.firstClause().functions);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() mapping: " + mapping);
                accumulator.substitute(mapping,result);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() accumulator after substitution: \n" + accumulator);
                //accumulator.substitute(reverseMapping(mapping));
                accumulator = accumulator.deepCopy();

                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() 4: checked clause: \n" + clause +
                                                       " with \n" + argFormula);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() accumulator after copy: \n" + accumulator);
                result.copy(accumulator);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve(): result: \n" + result);
                return mapping;
            }
            else
                accumulator.addClause(clause);      

            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() this: \n" + this);
            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.resolve() 5: checked clause: \n" + clause +
                                                   " with \n" + argFormula);
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
        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): Attempting to resolve : \n" + this + "\n with \n" + f);
        if (f.clauses.size() < 1 || this.clauses.size() < 1) {
            System.out.println("Error in CNFFormula.hyperResolve() attempt to resolve with empty list");
            return null;
        }
        CNFFormula thisFormula = new CNFFormula();
        thisFormula = this.deepCopy();
        CNFFormula argFormula = new CNFFormula();
        //argFormula = f.deepCopy();
        argFormula = this.unifyScope(f);  // ensure there are no variables clashes between this and f

        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): After unification of scope: \n" + thisFormula + "\n with \n" + argFormula);
        TreeMap mapping = new TreeMap();
        if ((thisFormula.clauses.size() == 1 && argFormula.clauses.size() == 1) &&
             thisFormula.firstClause().opposites(argFormula.firstClause())) {                       
            return simpleResolve(argFormula.firstClause(),result);
        }
        else {
            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): not both simple clauses");
            if ((thisFormula.clauses.size() == 1 && argFormula.clauses.size() == 1) &&
                !thisFormula.firstClause().opposites(argFormula.firstClause())) {
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): not opposite clauses.");            
                return null;            
            }
            if (argFormula.clauses.size() == 1) 
                return thisFormula.resolve(argFormula,result);            
            else {
                if (clauses.size() == 1) 
                    return argFormula.resolve(thisFormula,result);                
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
                                                               clause + " with \n" + thisFormula);
                        TreeMap newMapping = thisFormula.resolve(clause,newResult);
                        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (returning loop 3): mapping: " + newMapping);
                        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (returning loop 3): argFormula: \n" + argFormula);
                        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (returning loop 3): thisFormula: \n" + thisFormula);
                        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (returning loop 3): accumulator: \n" + accumulator);
                        if (newMapping != null) {  // && newMapping.keySet().size() > 0  // resolution succeeded
                            mapping.putAll(newMapping);  // could still be a problem if a mapping overwrites another...
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (returning loop 3): newResult: \n" + newResult);
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (returning loop 3): adding clauses to accumulator: \n" + newResult.clauses);
                            //accumulator = accumulator.appendClauseInCNF(argFormula);
                            accumulator.addAllClauses(newResult.clauses);
                            accumulator.substitute(mapping,null);
                            argFormula.substitute(mapping,null);
                            thisFormula = newResult.deepCopy();
                        }
                        else {
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (loop 3): here 2: ");
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): accumulator: \n" + accumulator);
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): adding clause to accumulator: \n" + clause);
                            accumulator.addClause(clause); 
                            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): accumulator after: \n" + accumulator);
                        }
                        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (loop 3): here 3: ");
                        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (loop 3): here 3: thisFormula\n" + thisFormula);
                        accumulator = accumulator.deepCopy();
                        result.copy(accumulator);
                        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() (loop 3): here 4: ");
                    }
                    result.addAllClauses(argFormula.clauses);
                }
            }
        }
        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): result(4): \n" + result);
        if (mapping != null && mapping.size() < 1 && !result.empty()) {
            mapping = null;
            result = null;
        }
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
        System.out.println("INFO in CNFFormula.unifyScopeTest(): cnf2 result: " + cnf1.unifyScope(cnf2));

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
        System.out.println("INFO in CNFFormula.testVarReplace(): reumbered: " + cnf1.renumberVariableReplacements(valueMap));
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

        //resolveTest1();
        //resolveTest2();
        //unifyScopeTest();
        //resolveTest3();
        //resolveTest4();
        //resolveTest5();
        resolveTest6();
        //testVarReplace();
        //normVarTest();

        //subsumeTest1();
        //subsumeTest2();
        //subsumeTest3();

    }
}

