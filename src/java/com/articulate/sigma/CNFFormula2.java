
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
public class CNFFormula2 implements Comparable {

    public static TreeMap<String, Integer> termMap = new TreeMap();
    public static TreeMap<Integer, String> intToTermMap = new TreeMap();

    public Formula sourceFormula = null;
    public String stringRep = null;
    public String reason = null;  // whether stated by "user" or otherwise derived
    public int inferenceStepCount = 0;

    /* Routines must ensure these are sorted and do not contain duplicates. */
    public CNFClause2[] clauses = null;

    /** *************************************************************
     */
    public CNFFormula2() {
    }

    /** *************************************************************
     *  Create an instance of this class from a Formula in CNF
     */
    public CNFFormula2(Formula f) {

        boolean _CREATE_DEBUG = false;
        TreeSet<CNFClause2> tempClauses = new TreeSet();

        if (_CREATE_DEBUG) System.out.println("INFO in CNFFormula2(): creating formula from: \n" + f);
        Formula fnew = new Formula();
        fnew.read(f.theFormula);
        sourceFormula = f;
        TreeMap<String, Integer> varMap = new TreeMap();
        if (f.isSimpleClause() || f.isSimpleNegatedClause()) {
            CNFClause2 c = CNFClause2.createClause(f,varMap);
            tempClauses.add(c);
        }
        else {
            if (!fnew.car().equals("or")) {
                System.out.println("Error in CNFFormula2(): formula not in CNF: " + f);
                return;
            }
            fnew.read(fnew.cdr());  // get rid of the enclosing "or"
            while (!fnew.empty() && fnew.car() != null) {
                Formula clause = new Formula();
                clause.read(fnew.car());
                if (_CREATE_DEBUG) System.out.println("INFO in CNFFormula2(): creating clause from: \n" + clause);
                if (_CREATE_DEBUG) System.out.println("INFO in CNFFormula2(): clauses size before: " + tempClauses.size());
                tempClauses.add(CNFClause2.createClause(clause,varMap));
                if (_CREATE_DEBUG) System.out.println("INFO in CNFFormula2(): clauses size after: " + tempClauses.size());
                fnew.read(fnew.cdr());
            }
        }
        clauses = new CNFClause2[tempClauses.size()];
        int i = 0;
        Iterator it = tempClauses.iterator();
        while (it.hasNext()) {
            CNFClause2 c = (CNFClause2) it.next();
            clauses[i++] = c;
        }
        Arrays.sort(clauses);
        if (_CREATE_DEBUG) System.out.println("INFO in CNFFormula2(): Finished creating formula from: \n" + f);
    }
    
    /** *************************************************************
     */
    public CNFFormula2 deepCopy() {

        CNFFormula2 newCNF = new CNFFormula2();
        newCNF.sourceFormula = sourceFormula;
        newCNF.reason = reason;
        newCNF.inferenceStepCount = inferenceStepCount;
        if (clauses != null) {
            newCNF.clauses = new CNFClause2[clauses.length];
            for (int i = 0; i < clauses.length; i++) 
                newCNF.clauses[i] = clauses[i].deepCopy();        
        }
        return newCNF;
    }

    /** *************************************************************
     *  Removes the given clause as a side effect (and returns this
     *  for convenience).  Removing two identical clauses should be
     *  rare, so if it occurs the whole routine is just run again.
     *  @param removeAll controls whether to remove a clause even if
     *                   that results in no clauses being left in
     *                   the formula.
     */
    public void removeClause(CNFClause2 c, boolean removeAll) {

        //System.out.println("INFO in CNFFormula2.removeClause(): Attempting to remove \n" + c + "\n from: \n" + this +
        //                   "\n with length " + clauses.length);
        if (clauses == null || clauses.length < 1)
            return;        
        if (!removeAll && clauses.length < 2) 
            return;
        CNFClause2[] clausesNew = new CNFClause2[clauses.length - 1];
        //System.out.println("INFO in CNFFormula2.removeClause(): new clauses length " + clausesNew.length);
        int target = 0;
        boolean removed = false;
        boolean duplicate = false;
        for (int i = 0; i < clauses.length; i++) {
            //System.out.println("INFO in CNFFormula2.removeClause(): Checking \n" + c + "\n against: \n" + clauses[i]);
            if (!clauses[i].deepEquals(c)) {
                //System.out.println("INFO in CNFFormula2.removeClause(): target index \n" + target + " equals " + i);
                clausesNew[target++] = clauses[i];
                //System.out.println("INFO in CNFFormula2.removeClause(): new clauses \n" + clausesNew[target-1]);
            }
            else {
                //System.out.println("INFO in CNFFormula2.removeClause(): Removing \n" + c + "\n from: \n" + clauses[i]);
                if (removed) {
                    //System.out.println("INFO in CNFFormula2.removeClause(): duplicate clauses in \n" + this);  
                    clausesNew[target++] = clauses[i];
                    duplicate = true;
                }
                removed = true;
            }
        }
        clauses = clausesNew;
        if (duplicate) 
            removeClause(c,removeAll);        
        stringRep = null;
        //System.out.println("INFO in CNFFormula2.removeClause(): Removed \n" + c + "\n from: \n" + this);
    }

    /** *************************************************************
     */
    public CNFClause2 firstClause() {
        if (clauses == null || clauses.length == 0) 
            return null;
        else
            return clauses[0];
    }

    /** *************************************************************
     */
    public boolean containsClause(CNFClause2 c) {

        if (clauses == null || clauses.length == 0 || c == null) 
            return false;
        for (int i = 0; i < clauses.length; i++) 
            if (clauses[i].deepEquals(c)) 
                return true;
        return false;
    }

    /** *************************************************************
     *  Add the clause, if it is not already in this.  No side
     *  effect.
     */
    public CNFFormula2 addClause(CNFClause2 c) {

        if (c == null) {
            System.out.println("Error in CNFFormula2.addClause(): clause is null");
            return this;
        }
        if (containsClause(c)) 
            return this;
        CNFFormula2 result = new CNFFormula2();
        int thisSize = 0;
        if (clauses != null) 
            thisSize = clauses.length;
        else
            thisSize = 0;
        result.clauses = new CNFClause2[thisSize + 1];
        int target = 0;
        for (int i = 0; i < thisSize; i++) 
            result.clauses[i] = clauses[i];        
        result.clauses[thisSize] = c;
        result.sourceFormula = sourceFormula;
        result.reason = reason;
        Arrays.sort(result.clauses);
        result.stringRep = null;
        return result;
    }

    /** *************************************************************
     *  Add all the clauses in f, if they are not already in this.
     *  No side effect.
     */
    public CNFFormula2 addAllClauses(CNFFormula2 f) {

        //System.out.println("INFO in CNFFormula2.addAllClauses(): Adding \n" + f + "\n to: \n" + this);
        CNFFormula2 result = new CNFFormula2();
        TreeSet<CNFClause2> resultClauses = new TreeSet();
        if (f.clauses != null) 
            resultClauses.addAll(Arrays.asList(f.clauses));
        if (clauses != null) 
            resultClauses.addAll(Arrays.asList(clauses));
        result.clauses = new CNFClause2[resultClauses.size()];
        Iterator it = resultClauses.iterator();
        int i = 0;
        while (it.hasNext())
            result.clauses[i++] = (CNFClause2) it.next();
        Arrays.sort(result.clauses);
        result.stringRep = null;
        //System.out.println("INFO in CNFFormula2.addAllClauses(): result \n" + result);
        return result;
    }

    /** *************************************************************
     *  Add all clauses as a side effect
     */
    public void addAllClausesSideEffect(CNFFormula2 f) {

        if (f != null && f.clauses != null) {
            //System.out.println("INFO in CNFFormula2.addAllClausesSideEffect(): Adding \n" + f + "\n to: \n" + this);
            TreeSet<CNFClause2> resultClauses = new TreeSet();
            if (f.clauses != null) 
                resultClauses.addAll(Arrays.asList(f.clauses));
            if (clauses != null) 
                resultClauses.addAll(Arrays.asList(clauses));
            clauses = new CNFClause2[resultClauses.size()];
            Iterator it = resultClauses.iterator();
            int i = 0;
            while (it.hasNext())
                clauses[i++] = (CNFClause2) it.next();
            Arrays.sort(clauses);
            stringRep = null;
            //System.out.println("INFO in CNFFormula2.addAllClausesSideEffect(): result \n" + this);
        }
    }

    /** *************************************************************
     *  ignore inferenceStepCount
     */
    public boolean deepEquals(CNFFormula2 arg) {

        if (clauses == null || arg.clauses == null) {
            if (clauses == null && arg.clauses == null) 
                return true;
            else
                return false;
        }
        if (arg.clauses.length != this.clauses.length) 
            return false;
        for (int i = 0; i < clauses.length; i++) 
             if (!clauses[i].deepEquals(arg.clauses[i]))
                 return false;
        return true;
    }

    /** *************************************************************
     */
    public boolean equals(CNFFormula2 arg) {
        return deepEquals(arg);
    }

    /** *************************************************************
     */
    public int size() {
        if (clauses == null) 
            return 0;
        return clauses.length;
    }

    /** ***************************************************************
     * Get all the Interger indexes of terms in the formula, without
     * duplication.
     */
    public TreeSet<Integer> collectTerms() {

        TreeSet<Integer> ts = new TreeSet();
        for (int i = 0; i < clauses.length; i++) 
            ts.addAll(clauses[i].collectTerms());                    
        return ts;
    }

    /** ***************************************************************
     * Get all the indexes of variables in the formula, without
     * duplication.
     */
    public TreeSet<TermCell> collectVariables() {

        TreeSet<TermCell> vars = new TreeSet();
        if (clauses != null) {
            for (int i = 0; i < clauses.length; i++) 
                vars.addAll(clauses[i].collectVariables());                    
        }
        return vars;
    }

    /** ***************************************************************
     */
    public boolean isGround() {
        return collectVariables().size() == 0;
    }

    /** ***************************************************************
     */
    public boolean empty() {
        return clauses == null;
    }

    /** ***************************************************************
     *  Set pointers as a side effect
     */
    public void setTermPointers(TreeMap<Integer, ArrayList<CNFFormula2>> posTermPointers,
                                TreeMap<Integer, ArrayList<CNFFormula2>> negTermPointers) {

        if (clauses != null)        
            for (int i = 0; i < clauses.length; i++) {
                CNFClause2 c = clauses[i];
                c.setTermPointers(posTermPointers,negTermPointers,this,c.negated);
            }
    }

    /** ***************************************************************
     *  Set pointers as a side effect
     */
    public void setPredPointers(TreeMap<Integer, ArrayList<CNFFormula2>> posPreds,
                                TreeMap<Integer, ArrayList<CNFFormula2>> negPreds) {

        if (clauses != null)        
            for (int i = 0; i < clauses.length; i++) {
                CNFClause2 c = clauses[i];
                c.setPredPointers(posPreds,negPreds,this);
            }
    }

    /** ***************************************************************
     *  Set pointers as a side effect
     */
    public void getPredicates(TreeSet<Integer> posPreds,
                              TreeSet<Integer> negPreds) {

        if (clauses != null)        
            for (int i = 0; i < clauses.length; i++) {
                CNFClause2 c = clauses[i];
                c.getPredicates(posPreds,negPreds);
            }
    }

    /** ***************************************************************
     *  @return a new formula with the substitutions made.  There is
     *          no side effect.
    */
    public CNFFormula2 substitute(TreeMap<TermCell, TermCell> m) {  

        //System.out.println("INFO in CNFFormula2.substitute(): this:\n" + this);
        //System.out.println("INFO in CNFFormula2.substitute(): m: " + m);
        CNFFormula2 result = this.deepCopy();
        CNFFormula2 oldResult = null;
        if (clauses != null) {
            do {
                oldResult = result.deepCopy();
                for (int i = 0; i < result.clauses.length; i++) 
                    result.clauses[i].substitute(m);                        
            } while (!result.deepEquals(oldResult));
            result.removeDuplicateClauses();
            Arrays.sort(result.clauses);
            result.stringRep = null;
        }
        return result;
    }

    /** **************************************************************
     *  Normalize variables so they are numbered in order of
     *  appearance.  There is no side effect.
     */
    public CNFFormula2 normalizeVariables(boolean even) {

        //System.out.println("INFO in CNFFormula2.normalizeVariables(): " + this);
        //System.out.println("INFO in CNFFormula2.normalizeVariables(): even: " + even);
        int counter = -1;
        TermCell counterNum = null;
        if (even) 
            counter--;
        TreeSet<TermCell> varList = collectVariables();
        //System.out.println("INFO in CNFFormula2.normalizeVariables(): variables : " + varList);
        TreeMap<TermCell,TermCell> vars = new TreeMap();
        boolean renumbered = false;
        Iterator it = varList.iterator();
        while (it.hasNext()) {
            TermCell v = (TermCell) it.next();
            do {  // ensure there are no cyclical variable assignments
                counter = counter - 2;
                counterNum = new TermCell();
                counterNum.term = counter;
            } while (vars.keySet() != null && vars.keySet().contains(counterNum));
            if (!v.deepEquals(counterNum)) {
                vars.put(v,counterNum);
                renumbered = true;
            }
        }
        //System.out.println("INFO in CNFFormula2.normalizeVariables(): Attempting to renumber : " + this + " with " + vars);
        if (renumbered) 
            return substitute(vars);
        else
            return this;
    }

    /** **************************************************************
     */
    public void removeDuplicateClauses() {

        //System.out.println("INFO in CNFFormula2.removeDuplicateClauses(): " + this);
        if (clauses == null || clauses.length < 2) 
            return;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < clauses.length; i++) {
                if (!changed) {
                    CNFClause2 c = clauses[i];
                    for (int j = i + 1; j < clauses.length; j++) {
                        if (c.deepEquals(clauses[j]) && !changed) {
                            //System.out.println("INFO in CNFFormula2.removeDuplicateClauses(): removing: " + c);
                            removeClause(c,false);
                            changed = true;
                        }
                    }
                }
            }
        }
    }

    /** **************************************************************
     *  Create constants to fill variables.  Results are side
     *  effects on the formula.
     */
    public void generateVariableValues() {

        for (int i = 0; i < clauses.length; i++) 
            clauses[i].generateVariableValues();        
    }

    /** ***************************************************************
     *  @return a new formula with the substitutions made.  There is
     *          no side effect.
    */
    public CNFFormula2 substituteVar(TreeMap<TermCell, TermCell> m) {  

        CNFFormula2 result = this.deepCopy();
        for (int i = 0; i < result.clauses.length; i++) 
            result.clauses[i].substituteVar(m);  
        result.removeDuplicateClauses();
        Arrays.sort(result.clauses);
        return result;
    }

    /** ***************************************************************
     *  Make variable numbers odd as a side effect.
    */
    public void makeVarsOdd() {  

        for (int i = 0; i < clauses.length; i++) 
            clauses[i].makeVarsOdd();                    
    }

    /** ***************************************************************
     *  Make variable numbers even as a side effect.
    */
    public void makeVarsEven() {  

        for (int i = 0; i < clauses.length; i++) 
            clauses[i].makeVarsEven();                    
    }

    /** ***************************************************************
     *  Compares this object with the specified object for order.
     *  @return a negative integer, zero, or a positive integer as
     *  this object is less than, equal to, or greater than the
     *  specified object.
    */
    public int compareTo(Object cc) throws ClassCastException {

        if (!cc.getClass().getName().equalsIgnoreCase("com.articulate.sigma.CNFFormula2")) 
            throw new ClassCastException("Error in CNFFormula2.compareTo(): "
                                         + "Class cast exception for argument of class: " 
                                         + cc.getClass().getName());
        CNFFormula2 argForm = (CNFFormula2) cc;
        for (int i = 0; i < clauses.length; i++) {
            CNFClause2 c = clauses[i];
            if (i < argForm.clauses.length) {
                CNFClause2 cArg = argForm.clauses[i];
                int res = c.compareTo(cArg);
                if (res != 0) 
                    return res;
            }
            else 
                return 1;
        }
        if (argForm.clauses.length > this.clauses.length) 
            return -1;
        return 0;
    }

    /** *************************************************************
     *  A convenience routine for creating a CNFFormula.  It does
     *  not convert a Formula into CNF
     */
    public void read(String s) {

        Formula f = new Formula();
        f.read(s);
        CNFFormula2 cnf = new CNFFormula2(f);
        clauses = cnf.clauses;
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
        CNFFormula2 cnf = new CNFFormula2(f);
        clauses = cnf.clauses;
        sourceFormula = cnf.sourceFormula;
        stringRep = null;
    }

    /** ***************************************************************
     * Convert to a String.
     */
    public String toStringFormat(boolean format) {

        if (stringRep != null) 
            return stringRep;        
        if (clauses == null || clauses.length == 0) 
            return "()";
        int indent = 0;
        StringBuffer result = new StringBuffer();
        if (clauses.length > 1) {
            result.append("(or");                     
            if (format) result.append("\n");
            indent = 2;
        }
        for (int clauseNum = 0; clauseNum < clauses.length; clauseNum++) {
            if (clauseNum != 0) 
                if (format) result.append("\n");            
            CNFClause2 clause = clauses[clauseNum];
            if (clause == null) 
                System.out.println("Error in CNFFormula2.toStringFormat(): clause is null at index: " + clauseNum);
            else
                result.append(clause.toStringFormat(indent,format));
        }
        if (clauses.length > 1) 
            result.append(")");  
        stringRep = result.toString();
        return result.toString();
    }

    /** ***************************************************************
     * Convert to a String.
     */
    public String toString() {

        return toStringFormat(true);
    }

    /** ***************************************************************
     *  Check for clauses in a formula that subsume one another, and
     *  can therefore be removed.
     *  @return a formula with the subsumed clauses removed.  There
     *          is no side effect.
     */
    public CNFFormula2 subsumedClauses() {

        TreeSet<CNFClause2> removals = new TreeSet();
        for (int i = 0; i < clauses.length; i++) {
            for (int j = i + 1; j < clauses.length; j++) {
                if (clauses[i].negated == clauses[j].negated && clauses[i].terms[0].deepEquals(clauses[j].terms[0])) {  // same predicate and negation
                    TreeMap<TermCell,TermCell> map = clauses[i].unify(clauses[j]);  
                    if (map != null && clauses[i].isSubsumingMap(map)) {
                        removals.add(clauses[i]);
                        //System.out.println("INFO in CNFFormula2.subsumedClauses(): Removing: " + clauses[i]);
                    }
                }
            }
        }
        CNFFormula2 result = new CNFFormula2();
        result.sourceFormula = sourceFormula;
        result.reason = reason;
        result.inferenceStepCount = inferenceStepCount;
        result.clauses = new CNFClause2[clauses.length - removals.size()];
        int index = 0;
        for (int i = 0; i < clauses.length; i++) {
            if (!removals.contains(clauses[i])) {
                result.clauses[index] = clauses[i].deepCopy();
                index++;
            }
        }
        if (removals.size() > 0) 
            STP3.subsumptions++;
        //System.out.println("INFO in CNFFormula2.subsumedClauses(): result: " + result);
        return result;
    }

    /** ***************************************************************
     *  The criterion is that the argument can be instantiated so
     *  that all its literals are identical to a subset of the
     *  literals in this formula.  In that case, this formula is
     *  subsumed by the argument and is therefore redundant with it.
     *  This and form should have odd and even variables numberings,
     *  respectively, or vice versa.
     *  @param form is the formula to test whether it subsumes this
     *              formula
     *  @return true if subsumes, false if not
     */
    public boolean subsumedBy(CNFFormula2 form) {

        boolean _SUB_DEBUG = false;

        if (_SUB_DEBUG) System.out.println("INFO in CNFFormula2.subsumedBy(): checking this:\n" + this + "\nagainst form:\n" + form);
        if (form.clauses.length < 1 || this.clauses.length < 1) {
            System.out.println("Error in CNFFormula2.subsumedBy() attempt to resolve with empty list");
            if (form.clauses.length > 0) 
                System.out.println("argument: \n" + form);
            if (clauses.length > 0) 
                System.out.println("this: \n" + this);            
            return false;
        }
        if (form.clauses.length > this.clauses.length) {
            if (_SUB_DEBUG) System.out.println("INFO in CNFFormula2.subsumedBy() argument has more clauses than this");
            return false;
        }
        CNFFormula2 thisFormula = this.deepCopy();
        CNFFormula2 argFormula = form.deepCopy();

        if (_SUB_DEBUG) System.out.println("INFO in CNFFormula2.subsumedBy(): after, checking this:\n" + thisFormula + "\nagainst form:\n" + argFormula);
        while (argFormula.clauses.length > 0) {
            CNFClause2 cArg = argFormula.clauses[0].deepCopy();
            if (_SUB_DEBUG) System.out.println("INFO in CNFFormula2.subsumedBy(): cArg:\n" + cArg);
            argFormula.removeClause(cArg,true);           
            boolean matched = false;
            while (thisFormula.clauses.length > 0) {
                CNFClause2 cThis = thisFormula.clauses[0].deepCopy();
                if (_SUB_DEBUG) System.out.println("INFO in CNFFormula2.subsumedBy(): cThis:\n" + cThis);
                thisFormula.removeClause(cThis,true);           
                if (_SUB_DEBUG) System.out.println("INFO in CNFFormula2.subsumedBy(): here 1");
                if (cThis.negated == cArg.negated) {
                    TreeMap<TermCell,TermCell> map = cArg.unify(cThis);  // see whether arg subsumes this
                    if (_SUB_DEBUG) System.out.println("INFO in CNFFormula2.subsumedBy(): here 2");
                    if (map != null && cArg.isSubsumingMap(map))
                        matched = true;
                    if (_SUB_DEBUG) System.out.println("INFO in CNFFormula2.subsumedBy(): here 3");
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
    private TreeMap<TermCell, TermCell> resolve(CNFClause2 c, CNFFormula2 result) {

        CNFFormula2 f = new CNFFormula2();
        f.clauses = new CNFClause2[1];
        f.clauses[0] = c;
        return resolve(f,result);
    }

    /** ***************************************************************
     *  @return a null mapping if resolution fails or if successful
     *  and complete resolution returns a non-null mapping.
     *  There is no side effect on this or f, but result will be an
     *  empty clause if resolution is successful.
     */
    private TreeMap<TermCell, TermCell> simpleResolve(CNFClause2 f, CNFFormula2 result) {

        boolean _RESOLVE_DEBUG = false;

        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula2.simpleResolve(): Attempting to resolve : \n" + 
                                               this + "\n with \n" + f);
        if (f.terms.length < 1 || this.clauses.length < 1) {
            System.out.println("Error in CNFFormula2.simpleResolve() attempt to resolve with empty list");
            if (f.terms.length > 0) 
                System.out.println("argument: \n" + f);
            if (clauses.length > 0) 
                System.out.println("this: \n" + this);
            return null;
        }
        if (this.clauses.length > 1) {
            System.out.println("Error in CNFFormula2.simpleResolve() not a simple formula: this:\n" + this);
            return null;
        }        
        if (!f.opposites(clauses[0])) {
            System.out.println("Error in CNFFormula2.simpleResolve() not opposites: \n" + clauses[0] 
                               + "\n and \n" + f);
            return null;
        }        
        TreeMap mapping = new TreeMap();
        mapping = this.clauses[0].unify(f);

        if (mapping != null) 
            result.clauses = null;
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
     *  @param f must be a formula with a single clause.
     *  @param result will contain all unmatched clauses if there is
     *                a successful unification, but the substitution
     *                will not have been performed.  Value is
     *                undefined if no resolution is found (and
     *                therefore the return value is null).
     *  There is no side effect on this or f.
     */
    private TreeMap<TermCell, TermCell> resolve(CNFFormula2 f, CNFFormula2 result) {

        boolean _RESOLVE_DEBUG = false;

        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula2.resolve(): Attempting to resolve : \n" + this + "\n with \n" + f);
        if (f.clauses.length < 1 || this.clauses.length < 1) {
            System.out.println("Error in CNFFormula2.resolve() attempt to resolve with empty list");
            if (f.clauses.length > 0) 
                System.out.println("argument: \n" + f);
            if (clauses.length > 0) 
                System.out.println("this: \n" + this);
            return null;
        }
        if (f.clauses.length > 1) {
            System.out.println("Error in CNFFormula2.resolve() argument is not a simple clause");
            if (f.clauses.length > 0) 
                System.out.println("argument: \n" + f);
            if (clauses.length > 0) 
                System.out.println("this: \n" + this);
            return null;
        }
        CNFFormula2 thisFormula = this.deepCopy();
        TreeMap mapping = new TreeMap();
        CNFFormula2 accumulator = new CNFFormula2();

        while (thisFormula.clauses.length > 0) {
            CNFClause2 clause = thisFormula.clauses[0].deepCopy();
            thisFormula.removeClause(clause,true);
            if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula2.resolve(): checking clause: \n" + 
                                                   clause + " with \n" + f);
            if (clause.opposites(f.clauses[0])) 
                mapping = f.simpleResolve(clause,result); 
            else
                mapping = null;
            if (mapping != null) { 
                accumulator.addAllClausesSideEffect(thisFormula.deepCopy());
                accumulator.addAllClausesSideEffect(accumulator.deepCopy());
                accumulator = accumulator.substitute(mapping);
                result.clauses = null;
                result.addAllClausesSideEffect(accumulator);
                if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula2.resolve(): successful result: \n" + 
                                                       result + " with mapping\n" + mapping);
                return mapping;
            }
            else
                accumulator = accumulator.addClause(clause);      
        }
        return mapping;
    }

    /** ***************************************************************
     *  Attempt to resolve one formula with another. @return a
     *  TreeMap of (possibly empty) variable substitutions if
     *  successful, null if not. Return CNFFormula "result" as a
     *  side effect, that is the result of the substition (which
     *  could be the empty list) on the argument, null if not.  An
     *  empty list for this data structure is considered to be a
     *  null array for the member variable "clauses".
     * 
     *  @return a null mapping if resolution fails, or if successful
     *  and complete resolution, returns a non-null mapping and
     *  empty clause result.  A successful and incomplete resolution
     *  returns a non-null mappings and the remaining un-unified
     *  clauses.  For example (not (p a b)) and (or (p a b) (p b c))
     *  yields an empty but non null mapping and a result of the
     *  left over clause of (p b c).
     * 
     *  There is no side effect on this or f.
     */
    public TreeMap<TermCell, TermCell> hyperResolve(CNFFormula2 f, CNFFormula2 result) {

        boolean _RESOLVE_DEBUG = false;

        result.inferenceStepCount = Math.max(this.inferenceStepCount,f.inferenceStepCount) + 1;
        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve() 1: Attempting to resolve, this: \n" + 
                                               this + "\n with argument f:\n" + f);
        if (f.clauses.length < 1 || this.clauses.length < 1) {
            System.out.println("Error in CNFFormula.hyperResolve() attempt to resolve with empty list");
            return null;
        }
        CNFFormula2 argFormula = f.deepCopy();
        CNFFormula2 thisFormula = this.deepCopy();
        CNFFormula2 accumulator = new CNFFormula2();
        TreeMap mapping = new TreeMap();
        boolean noMapping = true;
        if ((thisFormula.clauses.length == 1 && argFormula.clauses.length == 1) &&
             thisFormula.clauses[0].opposites(argFormula.clauses[0])) {                       
            mapping = simpleResolve(argFormula.clauses[0],result);
            return mapping;
        }
        else {
            if ((this.clauses.length == 1 && argFormula.clauses.length == 1) &&
                !this.clauses[0].opposites(argFormula.clauses[0])) 
                return null;                        
            if (argFormula.clauses.length == 1) 
                mapping = this.resolve(argFormula,result);                            
            else {
                if (clauses.length == 1) 
                    return argFormula.resolve(this,result);                   
                else {                                      // both formulas are not a simple clause
                    CNFFormula2 newResult = new CNFFormula2();
                    noMapping = true;
                    while (argFormula.clauses.length > 0) {
                        CNFClause2 clause = argFormula.clauses[0].deepCopy();
                        argFormula.removeClause(clause,true);
                        TreeMap<TermCell, TermCell> newMapping = thisFormula.resolve(clause,newResult);
                        if (newMapping != null) {  
                            mapping.putAll(newMapping);
                            argFormula = argFormula.substitute(mapping);
                            thisFormula = newResult.substitute(mapping);
                            noMapping = false;
                        }
                        else
                            accumulator = accumulator.addClause(clause);
                    }
                    result.addAllClausesSideEffect(accumulator);
                    result.addAllClausesSideEffect(thisFormula);
                }
            }
        }
        if (_RESOLVE_DEBUG) System.out.println("INFO in CNFFormula.hyperResolve(): result: \n" + result);
        if ((mapping != null && mapping.keySet().size() < 1 && result.clauses.length != 0) || noMapping) 
            mapping = null;        
        return mapping;
    }

    /** ***************************************************************
     * A test method.
     */
    public static void createTest() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.createTest()");
        CNFFormula2 cnf1 = new CNFFormula2();
        cnf1.read("(or (not (graphPart Org1-1 ?VAR1)) (not (instance Org1-1 GraphArc)) (not (instance ?VAR1 GraphPath)) " +
                  "(not (equal (InitialNodeFn Org1-1) ?VAR2)))");
        System.out.println("INFO in CNFFormula2.createTest(): formula:\n" + cnf1);
    }
   
    /** ***************************************************************
     * A test method.
     */
    public static void createTest2() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.createTest2()");
        CNFFormula2 cnf1 = new CNFFormula2();
        cnf1.read("(not (equal (SuccessorFn (ImmediateFamilyFn ?VAR1)) (SuccessorFn ?VAR3)))");
        System.out.println("INFO in CNFFormula2.createTest2(): formula:\n" + cnf1);
    }
   

    /** ***************************************************************
     * A test method.
     */
    public static void createTest3() {

        CNFFormula2 f = new CNFFormula2();
        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.createTest3()");
        //f.read("(forall (?ROW1 ?ITEM) (equal (ListOrderFn (ListFn ?ROW1 ?ITEM) (ListLengthFn (ListFn ?ROW1 ?ITEM))) ?ITEM))");
        f.read("(equal (ListOrderFn (ListFn ?ROW1 ?ITEM) (ListLengthFn (ListFn ?ROW1 ?ITEM))) ?ITEM)");
        System.out.println("INFO in CNFFormula2.createTest3(): formula: \n" + f);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void createTest4() {

        Formula f = new Formula();
        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.createTest4()");
        f.read("(=> (and (i ?X403 SOC) (i ?X404 SOC)) " +
               "(=> (and (s ?X403 ?X404) (i ?X405 ?X403)) (i ?X405 ?X404)))");
        System.out.println("INFO in CNFFormula2.createTest4(): formula: \n" + f);
        CNFFormula2 cnf1 = new CNFFormula2(Clausifier.clausify(f));
        System.out.println("INFO in CNFFormula2.createTest4(): formula: \n" + cnf1);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void subsumeTest1() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.subsumeTest1()");
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();

        cnf1.read("(or (not (p ?X)) (q ?X))");
        cnf2.read("(or (not (p ?X)))");

        System.out.println("INFO in CNFFormula2.subsumeTest1(): cnf1: " + cnf1);
        System.out.println("INFO in CNFFormula2.subsumeTest1(): cnf2: " + cnf2);
        System.out.println("INFO in CNFFormula2.subsumeTest1(): result: " + cnf1.subsumedBy(cnf2));
        System.out.println("INFO in CNFFormula2.subsumeTest1(): should be true");
    }

    /** ***************************************************************
     * A test method.
     */
    public static void subsumeTest2() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.subsumeTest2()");
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();

        cnf1.read("(or (not (p a)) (q a))");
        cnf2.read("(or (not (p ?X)))");

        System.out.println("INFO in CNFFormula2.subsumeTest2(): cnf1: " + cnf1);
        System.out.println("INFO in CNFFormula2.subsumeTest2(): cnf2: " + cnf2);
        System.out.println("INFO in CNFFormula2.subsumeTest2(): result: " + cnf1.subsumedBy(cnf2));
        System.out.println("INFO in CNFFormula2.subsumeTest2(): should be true");
    }

    /** ***************************************************************
     * A test method.
     */
    public static void subsumeTest3() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.subsumeTest3()");
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();

        cnf1.read("(or (not (p ?X)) (q a))");
        cnf2.read("(or (not (p a)))");

        System.out.println("INFO in CNFFormula2.subsumeTest3(): cnf1: " + cnf1);
        System.out.println("INFO in CNFFormula2.subsumeTest3(): cnf2: " + cnf2);
        System.out.println("INFO in CNFFormula2.subsumeTest3(): result: " + cnf1.subsumedBy(cnf2));
        System.out.println("INFO in CNFFormula2.subsumeTest3(): should be false");
    }

    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest1() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.resolveTest1()");
        Formula f1 = new Formula();
        f1.read("(s O C)");
        Formula f2 = new Formula();
        f2.read("(or (not (s ?X7 C)) (not (s O C)))");
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2(f1);
        CNFFormula2 cnf2 = new CNFFormula2(f2);

        CNFFormula2 target = new CNFFormula2();
        target.read("(not (s O C))");

        System.out.println("INFO in CNFFormula2.resolveTest1(): cnf1: " + cnf1.deepCopy());
        System.out.println("INFO in CNFFormula2.resolveTest1(): cnf2: " + cnf2.deepCopy());
        System.out.println("INFO in CNFFormula2.resolveTest1(): resolution result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula2.resolveTest1(): resolution result: " + newResult);
        System.out.println("INFO in CNFFormula2.resolveTest1(): target: " + target);

        newResult = newResult.normalizeVariables(true);
        target = target.normalizeVariables(true);
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
        System.out.println("INFO in CNFFormula2.resolveTest2()");
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();

        cnf1.read("(or (not (attribute ?VAR1 American)) (not (instance ?VAR2 Weapon)) (not (instance ?VAR3 Nation)) (not (attribute ?VAR3 Hostile)) " + 
                  "(not (instance ?VAR4 Selling)) (not (agent ?VAR4 ?VAR1)) (not (patient ?VAR4 ?VAR2)) (not (recipient ?VAR4 ?VAR3)))");
        cnf2.read("(or (agent ?X15 West) (not (possesses Nono ?X16)) (not (instance ?X16 Missile)))");
        cnf1.makeVarsEven();
        cnf2.makeVarsOdd();
        System.out.println("INFO in CNFFormula2.resolveTest2(): cnf1: " + cnf1);
        System.out.println("INFO in CNFFormula2.resolveTest2(): cnf2: " + cnf2);
        System.out.println("INFO in CNFFormula2.resolveTest2(): resolution result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula2.resolveTest2(): resolution result: " + newResult);
        CNFFormula2 target = new CNFFormula2();
        target.read("(or (not (recipient ?VAR4 ?VAR3)) (not (possesses Nono ?VAR1)) (not (instance ?VAR1 Missile)) " +
                    "(not (patient ?VAR4 ?VAR2)) (not (instance ?VAR4 Selling)) (not (instance ?VAR3 Nation)) " +
                    "(not (instance ?VAR2 Weapon)) (not (attribute ?VAR3 Hostile)) (not (attribute West American)))");

        newResult = newResult.normalizeVariables(true);
        target = target.normalizeVariables(true);
        if (newResult.equals(target)) 
            System.out.println("Successful test");
        else
            System.out.println("Result: \n " + newResult + "\n not equal to target result: \n" + target);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest3() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.resolveTest3()");
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();

        cnf1.read("(or (attribute ?VAR2 Criminal) (not (recipient ?VAR1 ?VAR3)) (not (patient ?VAR1 ?VAR4)) (not (instance ?VAR4 Weapon)) " +
                  "(not (instance ?VAR3 Nation)) (not (instance ?VAR1 Selling)) (not (agent ?VAR1 ?VAR2)) (not (attribute ?VAR3 Hostile)) " +
                  "(not (attribute ?VAR2 American)))");
        cnf2.read("(or (not (recipient ?VAR5 ?VAR3)) (not (patient ?VAR5 ?VAR4)) (not (instance ?VAR5 Selling)) (not (instance ?VAR4 Weapon)) " +
                  "(not (instance ?VAR3 Nation)) (not (agent ?VAR5 ?VAR2)) (not (attribute ?VAR3 Hostile)) (not (attribute ?VAR2 American)))");

        cnf1.makeVarsEven();
        cnf2.makeVarsOdd();
        TreeMap<TermCell,TermCell> valueMap = cnf1.hyperResolve(cnf2,newResult);
        System.out.println("INFO in CNFFormula2.resolveTest3(): resolution result mapping: " + valueMap);
        System.out.println("INFO in CNFFormula2.resolveTest3(): resolution result: " + newResult);

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
        System.out.println("INFO in CNFFormula2.resolveTest4()");
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();
        cnf1.read("(or (m (SkFn 1 ?VAR1) ?VAR1) (not (i ?VAR1 C)))");
        cnf2.read("(not (m ?VAR1 Org1-1))");

        cnf1.makeVarsEven();
        cnf2.makeVarsOdd();
        System.out.println("INFO in CNFFormula2.resolveTest4(): resolution result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula2.resolveTest4(): resolution result: " + newResult);

        CNFFormula2 target = new CNFFormula2();
        target.read("(not (i Org1-1 C))");

        newResult = newResult.normalizeVariables(true);
        target = target.normalizeVariables(true);
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
        System.out.println("INFO in CNFFormula2.resolveTest5()");
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();
        cnf1.read("(or (i ?VAR2 SOC) (not (s ?VAR1 ?VAR2)))");
        cnf2.read("(or (not (s ?VAR1 C)) (not (i C SOC)) (not (i Org1-1 ?VAR1)) (not (i ?VAR1 SOC)))");
        cnf1.makeVarsEven();
        cnf2.makeVarsOdd();
        System.out.println("INFO in CNFFormula2.resolveTest5(): resolution result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula2.resolveTest5(): resolution result: " + newResult);

        CNFFormula2 target = new CNFFormula2();
        target.read("(or (not (i SOC SOC)) (not (i C SOC)) (not (s SOC C)) (not (s ?VAR2 Org1-1)))");

        newResult = newResult.normalizeVariables(true);
        System.out.println("INFO in CNFFormula2.resolveTest5(): resolution result after normalization: " + newResult);
        target = target.normalizeVariables(true);
        if (newResult.equals(target)) 
            System.out.println("Successful test");
        else
            System.out.println("Result: \n " + newResult + "\n not equal to target result: \n" + target);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest5new() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.resolveTest5new()");
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();
        cnf1.read("(or (i ?VAR2 SOC))");
        cnf2.read("(or (not (s ?VAR1 C)) (not (i C SOC)) (not (i Org1-1 ?VAR1)) (not (i ?VAR1 SOC)))");
        cnf1.makeVarsEven();
        cnf2.makeVarsOdd();
        System.out.println("INFO in CNFFormula2.resolveTest5new(): cnf1: \n" + cnf1);
        System.out.println("INFO in CNFFormula2.resolveTest5new(): cnf2: \n" + cnf2);
        System.out.println("INFO in CNFFormula2.resolveTest5new(): resolution result mapping: " + cnf2.resolve(cnf1,newResult));
        System.out.println("INFO in CNFFormula2.resolveTest5new(): resolution result: " + newResult);

    }

    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest5hyper() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.resolveTest5hyper()");
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();
        cnf1.read("(or (i ?VAR2 SOC) (s M ?VAR4))");
        cnf2.read("(or (not (s ?VAR1 C)) (not (i C SOC)) (not (i Org1-1 ?VAR1)) (not (i ?VAR1 SOC)))");
        cnf1.makeVarsEven();
        cnf2.makeVarsOdd();
        System.out.println("INFO in CNFFormula2.resolveTest5hyper(): cnf1: \n" + cnf1);
        System.out.println("INFO in CNFFormula2.resolveTest5hyper(): cnf2: \n" + cnf2);
        System.out.println("INFO in CNFFormula2.resolveTest5hyper(): resolution result mapping: " + cnf2.hyperResolve(cnf1,newResult));
        System.out.println("INFO in CNFFormula2.resolveTest5hyper(): resolution result: " + newResult);

    }

    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest6() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.resolveTest6()");
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();
        cnf1.read("(or (equal ?VAR2 ?VAR1) (not (equal ?VAR1 ?VAR2)))");
        cnf2.read("(or (not (rangeSubclass ?VAR1 Object)) (not (subclass Object SetOrClass)) " +
                  "(not (equal (AssignmentFn ?VAR1 ?VAR2 ?VAR3 ?VAR4 ?VAR5 ?VAR6 ?VAR7) Hole)) " +
                  "(not (instance Object SetOrClass)) (not (instance Hole SetOrClass)) " +
                  "(not (instance ?VAR1 Function)))");
        cnf1.makeVarsEven();
        cnf2.makeVarsOdd();
        System.out.println("INFO in CNFFormula2.resolveTest6(): resolution result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula2.resolveTest6(): resolution result: " + newResult);

        CNFFormula2 target = new CNFFormula2();
        target.read("(or (not (rangeSubclass ?VAR1 Object)) (not (subclass Object SetOrClass)) " +
                  "(not (equal Hole (AssignmentFn ?VAR1 ?VAR2 ?VAR3 ?VAR4 ?VAR5 ?VAR6 ?VAR7))) " +
                  "(not (instance Object SetOrClass)) (not (instance Hole SetOrClass)) " +
                  "(not (instance ?VAR1 Function)))");

        newResult = newResult.normalizeVariables(true);
        target = target.normalizeVariables(true);
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
        System.out.println("INFO in CNFFormula2.resolveTest7()");
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();
        cnf1.read("(equal (ListOrderFn (ListFn ?VAR1 ?VAR2) (ListLengthFn (ListFn ?VAR1 ?VAR2))) ?VAR2)");
        cnf2.read("(not (equal (ImmediateFamilyFn ?VAR1) Org1-1))");
        cnf1.makeVarsEven();
        cnf2.makeVarsOdd();
        System.out.println("INFO in CNFFormula2.resolveTest7(): resolution result mapping (should be null): " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula2.resolveTest7(): resolution result: " + newResult);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest8() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.resolveTest8()");
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();
        cnf1.read("(not (equal (ImmediateFamilyFn ?VAR1) Org1-1))");
        cnf2.read("(or (equal ?VAR2 ?VAR3) (not (equal (SuccessorFn ?VAR2) (SuccessorFn ?VAR3))))");
        cnf1.makeVarsEven();
        cnf2.makeVarsOdd();
        System.out.println("INFO in CNFFormula2.resolveTest8(): resolution result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula2.resolveTest8(): resolution result: " + newResult);

        CNFFormula2 target = new CNFFormula2();
        target.read("(not (equal (SuccessorFn (ImmediateFamilyFn ?VAR1)) (SuccessorFn Org1-1)))");

        newResult = newResult.normalizeVariables(true);
        target = target.normalizeVariables(true);
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
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();
        cnf1.read("(or (not (graphPart Org1-1 ?VAR1)) (not (instance Org1-1 GraphArc)) (not (instance ?VAR1 GraphPath)) " +
                  "(not (equal (InitialNodeFn Org1-1) ?VAR2)))");
        cnf2.read("(or (before NegativeInfinity ?VAR1) (equal ?VAR1 NegativeInfinity) " +
                  "(not (instance ?VAR1 TimePoint)))");
        cnf1.makeVarsEven();
        cnf2.makeVarsOdd();
        System.out.println("INFO in CNFFormula2.resolveTest9(): resolution result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula2.resolveTest9(): resolution result: " + newResult);

        CNFFormula2 target = new CNFFormula2();
        target.read("(or (before NegativeInfinity (InitialNodeFn Org1-1)) " +
                    "(not (instance Org1-1 GraphArc)) (not (instance (InitialNodeFn Org1-1) TimePoint)) " +
                    "(not (instance ?VAR1 GraphPath)) (not (graphPart Org1-1 ?VAR1)))");        
        newResult = newResult.normalizeVariables(true);
        target = target.normalizeVariables(true);
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
        System.out.println("INFO in CNFFormula2.resolveTest10()");
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();
        cnf1.read("(or (not (subclass RealNumber Collection)) (not (subclass RealNumber NonnegativeRealNumber)) " +
                  "(not (equal Org1-1 ?VAR1)) (not (instance Org1-1 RealNumber)) " +
                  "(not (instance NonnegativeRealNumber SetOrClass)) (not (instance ?VAR1 NonnegativeRealNumber)))");
        cnf2.read("(or (equal ?VAR3 (SubtractionFn 0 ?VAR4)) (instance ?VAR4 NonnegativeRealNumber) " +
                  "(not (equal (AbsoluteValueFn ?VAR4) ?VAR3)) (not (instance ?VAR4 RealNumber)) " +
                  "(not (instance ?VAR3 RealNumber)) (not (instance ?VAR3 NonnegativeRealNumber)))");
        cnf1.makeVarsEven();
        cnf2.makeVarsOdd();
        System.out.println("INFO in CNFFormula2.resolveTest10(): resolution result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula2.resolveTest10(): resolution result: " + newResult);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void resolveTest11() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.resolveTest11()");
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();
        cnf1.read("(or (c ?VAR1) (not (a ?VAR1)) (not (b ?VAR1)))");
        cnf2.read("(or (a foo) (b foo))");
        System.out.println("INFO in CNFFormula2.resolveTest11(): resolution result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula2.resolveTest11(): resolution result: " + newResult);

        CNFFormula2 target = new CNFFormula2();
        target.read("(c foo)");        
        if (newResult.equals(target)) 
            System.out.println("Successful test");
        else
           System.out.println("Result: \n" + newResult + "\n not equal to target result: \n" + target);          
    }
 
    /** ***************************************************************
     * A test method.
     */
    public static void oppositesTest1() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.oppositesTest1()");
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();
        cnf1.read("(or (agent ?VAR4 West) (not (instance ?VAR6 Missile)) (not (possesses Nono ?VAR6)))");
        cnf2.read("(or (instance ?VAR1 Weapon) (not (instance ?VAR1 Missile)))");
        cnf1.makeVarsEven();
        cnf2.makeVarsOdd();
        System.out.println("INFO in CNFFormula2.oppositesTest1(): result mapping: " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula2.oppositesTest1(): result: " + newResult);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void oppositesTest2() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.oppositesTest2()");
        CNFFormula2 newResult = new CNFFormula2();
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();
        cnf1.read("(or (agent ?VAR4 West) (not (instance ?VAR6 Missile)) (not (possesses Nono ?VAR6)))");
        cnf2.read("(not (instance ?VAR1 Missile))");
        cnf1.makeVarsEven();
        cnf2.makeVarsOdd();
        System.out.println("INFO in CNFFormula2.oppositesTest2(): result mapping (should be null): " + cnf1.hyperResolve(cnf2,newResult));
        System.out.println("INFO in CNFFormula2.oppositesTest2(): result: " + newResult);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void speedTest() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.speedTest()");
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
        System.out.println("INFO in CNFFormula2.speedTest(): t_elapsed: " + t_elapsed);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void subsumptionTest1() {

        System.out.println("---------------------------------");
        System.out.println("INFO in CNFFormula2.subsumptionTest1()");
        CNFFormula2 cnf1 = new CNFFormula2();
        cnf1.read("(or (agent ?VAR4 ?VAR2) (not (agent Flying1 Missile)) (agent Nono ?VAR6))");
        System.out.println("INFO in CNFFormula2.subsumptionTest1(): Result: " + cnf1.subsumedClauses());
    }

    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {

        //speedTest();
        //subsumptionTest1();
        //createTest();
        //createTest2();
        //createTest3();
        //createTest4();
        //subsumeTest1();
        //subsumeTest2();
        //subsumeTest3();
        //resolveTest1();
        //resolveTest2();
        //resolveTest3();
        //resolveTest4();
        resolveTest5new();
        //resolveTest5();
        //resolveTest6();
        //resolveTest7();
        //resolveTest8();
        //resolveTest9();
        //resolveTest10();
        //resolveTest11();
        //oppositesTest1();
        //oppositesTest2();
        
    }
}
