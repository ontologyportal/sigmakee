

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
public class CNFClause2 implements Comparable {

    static boolean _UNIFY_DEBUG = false;

    public TermCell[] terms = null;
    public boolean negated = false;

    /** *************************************************************
     */
    public CNFClause2() {
    }

    /** ***************************************************************
     *  Get the next number for a term for the global term map.
     */
    public static int encodeTerm(String term) {

        if (CNFFormula2.termMap.keySet().contains(term)) 
            return ((Integer) CNFFormula2.termMap.get(term)).intValue();
        int termNum = CNFFormula2.termMap.size() + 1;
        CNFFormula2.termMap.put(term,new Integer(termNum));
        CNFFormula2.intToTermMap.put(new Integer(termNum),term);
        return termNum;
    }

    /** ***************************************************************
    */
    public static CNFClause2 createClause(Formula f, TreeMap<String, Integer> varMap) {

        boolean _CREATE_DEBUG = false;
        ArrayList<TermCell> argList = new ArrayList();

        if (_CREATE_DEBUG) System.out.println("INFO in CNFClause2.createClause(): formula: " + f);
        CNFClause2 newClause = new CNFClause2();
        Formula fnew = new Formula();
        fnew.read(f.theFormula);
        if (fnew.isSimpleNegatedClause()) {
            newClause.negated = true;
            fnew.read(fnew.cdr());  // read and discard the "(not"
            fnew.read(fnew.car());
        }
        while (!fnew.empty()) {
            String arg = fnew.car();
            if (_CREATE_DEBUG) System.out.println("INFO in CNFClause2.createClause(): arg: [" + arg + "]");
            fnew.read(fnew.cdr());

            if (argList.size() > 11) {
                if (_CREATE_DEBUG) System.out.println("Error in CNFClause2.createClause(): arg out of range converting formula: " + f);
                return null;
            }
            if (Formula.atom(arg) && !Formula.isVariable(arg)) {
                if (_CREATE_DEBUG) System.out.println("INFO in CNFClause2.createClause(): atom");
                int termNum = 0;
                if (CNFFormula2.termMap.keySet().contains(arg)) 
                    termNum = CNFFormula2.termMap.get(arg);
                else
                    termNum = CNFClause2.encodeTerm(arg);     
                TermCell tc = new TermCell();
                tc.term = termNum;
                tc.argList = null;
                argList.add(tc);
            }
            if (Formula.isVariable(arg)) {
                if (_CREATE_DEBUG) System.out.println("INFO in CNFClause2.createClause(): variable");
                if (varMap.keySet().contains(arg)) {
                    TermCell tc = new TermCell();
                    tc.term = ((Integer) varMap.get(arg)).intValue();
                    tc.argList = null;
                    argList.add(tc);
                }
                else {
                    int varNum = -((varMap.keySet().size() * 2) + 1);
                    varMap.put(arg,new Integer(varNum));
                    TermCell tc = new TermCell();
                    tc.term = varNum;
                    tc.argList = null;
                    argList.add(tc);
                }
            }
            if (Formula.isFunctionalTerm(arg)) {
                if (_CREATE_DEBUG) System.out.println("INFO in CNFClause2.createClause(): function");
                Formula func = new Formula();
                func.read(arg);
                CNFClause2 cnew = createClause(func,varMap);
                if (_CREATE_DEBUG) System.out.println("INFO in CNFClause2.createClause(): functions adding " + 
                                   cnew + ")");
                TermCell tc = new TermCell();
                tc.term = 0;
                tc.argList = cnew.terms;
                argList.add(tc);
            }
        }
        if (_CREATE_DEBUG) System.out.println("INFO in CNFClause.createClause(): Arguments: ");
        if (_CREATE_DEBUG) System.out.println("INFO in CNFClause.createClause(): Negated: " + newClause.negated);
        newClause.terms = new TermCell[argList.size()];
        for (int i = 0; i < argList.size(); i++) {
            TermCell tc = (TermCell) argList.get(i);
            newClause.terms[i] = tc;
        }
        return newClause;
    }

    /** *************************************************************
     */
    public CNFClause2 deepCopy() {

        CNFClause2 newCNF = new CNFClause2();
        newCNF.negated = negated;
        newCNF.terms = new TermCell[terms.length];
        for (int i = 0; i < terms.length; i++) 
            newCNF.terms[i] = terms[i].deepCopy();        
        return newCNF;
    }

    /** ***************************************************************
     *  Compares this object with the specified object for order.
     * 
     *  @return a negative integer, zero, or a positive integer as
     *  this object is less than, equal to, or greater than the
     *  specified object.
    */
    public int compareTo(Object cc) throws ClassCastException {

        //System.out.println("INFO in CNFClause2.compareTo()");
        if (!cc.getClass().getName().equalsIgnoreCase("com.articulate.sigma.CNFClause2")) 
            throw new ClassCastException("Error in CNFClause.compareTo(): "
                                         + "Class cast exception for argument of class: " 
                                         + cc.getClass().getName());
        //System.out.println("INFO in CNFClause2.compareTo() Comparing \n" + this + "\n to \n" + ((CNFClause2) cc));
        CNFClause2 cArg = (CNFClause2) cc;
        if (negated && !cArg.negated) 
            return 1;
        if (!negated && cArg.negated) 
            return -1;
        if (terms.length <= cArg.terms.length) 
            return TermCell.compareTo(terms,cArg.terms);
        else
            return - TermCell.compareTo(cArg.terms,terms);                                 
    }

    /** ***************************************************************
     * @return true if this is equal to the first argument, false
     * otherwise.  Check whether all functions are really equal, not
     * just sharing an index.
     */
    public boolean deepEquals(CNFClause2 c) {

        //System.out.println("INFO in CNFClause.deepEquals(): Comparing : " + this.toString(thisFunctions) + 
        //                   " with " + c.toString(cFunctions));
        if (c == null) {
            System.out.println("Error in CNFClause2.deepEquals(): c is null");
            return false;
        }
        if (c.negated != negated || c.terms.length != terms.length) 
            return false;
        for (int i = 0; i < terms.length; i++) 
            if (!c.terms[i].deepEquals(terms[i])) 
                return false;        
        return true;
    }

    /** ***************************************************************
     */
    public boolean equals(CNFClause2 c) {
        return deepEquals(c);
    }

    /** ***************************************************************
     *  @return a boolean indicating whether the clause contains the given value.
     */
    public static boolean containsVar(TermCell[] argTerms, TermCell t) {

        //System.out.println("INFO in CNFClause2.containsVar(): Check for appearance of  " + t + " in " + argTerms);
        if (!t.isVariable()) 
            return false;
        int i = 0;
        while (i < argTerms.length) {
            if (t.equals(argTerms[i])) 
                return true;
            if (argTerms[i].isFunction() && argTerms[i].containsVar(t)) 
                return true;
            i++;
        }        
        return false;
    }

    /** ***************************************************************
    */
    public static String spaces(int num) {

        StringBuffer result = new StringBuffer();
        for (int space = 0; space < num; space++) 
            result.append(" ");
        return result.toString();
    }

    /** ***************************************************************
     *  @param indent is the number of spaces to indent.  The sign
     *                of the integer is reversed if this is a
     *                function which shouldn't have a terminating
     *                carriage return.
    */
    public String toStringFormat(int indent, boolean format) {  

        if (indent > 100) {
            System.out.println("Error in CNFClause2.toStringFormat(): Error: excessive indent ");
            return "";
        }
        StringBuffer result = new StringBuffer();
        if (negated) {           
            result.append(spaces(indent) + "(not ");
            if (format) result.append("\n");
            indent = indent + 2;
        }
        if (format) result.append(spaces(indent));        
        result.append("(");
        for (int i = 0; i < terms.length; i++) {
            TermCell t = terms[i];
            result.append(t.toStringFormat(indent,format));
            if (i < terms.length - 1) 
                result.append(" ");                        
        }
        if (negated) result.append("))");
        else         result.append(")");
        if (indent == 0) 
            if (format) result.append("\n");
        return result.toString();
    }

    /** ***************************************************************
    */
    public String toString() {  
        return toStringFormat(0,true);
    }

    /** ***************************************************************
     *  Make variable numbers odd as a side effect.
    */
    public void makeVarsOdd() {

        for (int i = 0; i < terms.length; i++) 
            terms[i].makeVarsOdd();                    
    }

    /** ***************************************************************
     *  Make variable numbers even as a side effect.
    */
    public void makeVarsEven() {

        for (int i = 0; i < terms.length; i++) 
            terms[i].makeVarsEven();                    
    }

    /** ***************************************************************
     *  @return whether the given clause is the opposite sign from
     *          this clause
     */
    public boolean opposites(CNFClause2 c) {

        if ((negated && !c.negated) || (!negated && c.negated)) 
            return true;
        else
            return false;
    }

    /** ***************************************************************
     * Get all the Integer indexes of terms in the formula,
     * without duplication.
     */
    public TreeSet<Integer> collectTerms() {

        TreeSet<Integer> result = new TreeSet();
        for (int i = 0; i < terms.length; i++) {
            if (terms[i].isConstant()) 
                result.add(new Integer(terms[i].term));
            if (terms[i].isFunction())             
                result.addAll(terms[i].collectTerms());        
        }
        return result;
    }

    /** ***************************************************************
     * Get all the indexes of variables in the formula, without
     * duplication.
     */
    public TreeSet<TermCell> collectVariables() {

        TreeSet<TermCell> result = new TreeSet();
        for (int i = 0; i < terms.length; i++) {
            if (terms[i].isVariable()) 
                result.add(terms[i]);
            if (terms[i].isFunction())             
                result.addAll(terms[i].collectVariables());        
        }
        return result;
    }

    /** ***************************************************************
     */
    public boolean isGround() {
        return collectVariables().size() == 0;
    }

    /** ***************************************************************
     *  Set pointers as a side effect.
     *  @param negated is used in case this clause is a function
     *                 that is within a negated clause.
     */
    public void setTermPointers(TreeMap<Integer, ArrayList<CNFFormula2>> posTermPointers,
                                TreeMap<Integer, ArrayList<CNFFormula2>> negTermPointers,
                                CNFFormula2 f, boolean neg) {

        boolean isNegated = neg;
        if (!isNegated)             // a negated parameter overrides the local value
            isNegated = negated;            
        for (int i = 0; i < terms.length; i++) {
            if (terms[i].isVariable()) 
                continue;
            else if (terms[i].isFunction()) {
                TermCell tc = terms[i];
                tc.setTermPointers(posTermPointers,negTermPointers,f,isNegated);
                continue;
            }
            else {
                if (isNegated) 
                    TermCell.setTermPointer(negTermPointers,terms[i],f);
                else
                    TermCell.setTermPointer(posTermPointers,terms[i],f);
            }
        }
    }

    /** ***************************************************************
     *  Convenience routine to add values to a map where the value
     *  is an ArrayList of actual values.
     */
    public static void addToMap(TreeMap<Integer, ArrayList<CNFFormula2>> map, 
                                Integer key, CNFFormula2 value) {

        ArrayList<CNFFormula2> al = null;
        if (map.keySet().contains(key)) 
            al = (ArrayList) map.get(key);
        else
            al = new ArrayList();
        al.add(value);
        map.put(key,al);       
    }

    /** ***************************************************************
     *  Set pointers as a side effect.
     */
    public void setPredPointers(TreeMap<Integer, ArrayList<CNFFormula2>> posPreds,
                                TreeMap<Integer, ArrayList<CNFFormula2>> negPreds,
                                CNFFormula2 f) {

        if (terms[0].isConstant()) {
            if (negated) 
                addToMap(negPreds,new Integer(terms[0].term),f);
            else
                addToMap(posPreds,new Integer(terms[0].term),f);
        }
        else         
            System.out.println("Error in CNFClause2.setPredPointers(): formula \n" + f +
                               "\nhas a non-constant in predicate position.");
    }

    /** ***************************************************************
     *  Set pointers as a side effect.
     */
    public void getPredicates(TreeSet<Integer> posPreds,
                              TreeSet<Integer> negPreds) {

        if (terms[0].isConstant()) {
            if (negated) 
                negPreds.add(new Integer(terms[0].term));
            else
                posPreds.add(new Integer(terms[0].term));
        }
        else         
            System.out.println("Error in CNFClause2.setPredPointers(): formula \n" + this +
                               "\nhas a non-constant in predicate position.");
    }

    /** ***************************************************************
     *  Check to see that either every key in the map is a variable
     *  from this, or the value is a variable from this.
     */
    public boolean isSubsumingMap(TreeMap<TermCell,TermCell> m) {

        //System.out.println("INFO in CNFClause2.isSubsumingMap(): checking this:\n" + this + "\nagainst map:\n" + m);
        TreeSet<TermCell> al = collectVariables();
        //System.out.println("INFO in CNFClause2.isSubsumingMap(): variables: " + al);
        Iterator it = m.keySet().iterator();
        while (it.hasNext()) {
            TermCell key = (TermCell) it.next();
            TermCell value = (TermCell) m.get(key);
            //System.out.println("INFO in CNFClause2.isSubsumingMap(): key: " + key + " value: " + value);
            if ( ! ((key.isVariable() && al.contains(key)) ||
                    (value.isVariable() && al.contains(value)))) {
                //System.out.println("INFO in CNFClause2.isSubsumingMap(): key is variable: " + key.isVariable());
                //System.out.println("INFO in CNFClause2.isSubsumingMap(): al contains key: " + al.contains(key));
                return false;
            }
        }
        //System.out.println("INFO in CNFClause2.isSubsumingMap(): true");
        return true;
    }

    /** ***************************************************************
     *  Note that this routine assumes that the calling routine has
     *  copied the formula, since this routine has a side effect if
     *  substitution is successful.
    */
    public void substitute(TreeMap<TermCell, TermCell> m) {

        //System.out.println("INFO in CNFClause2.substitute(): map: " + m);
        for (int i = 0; i < terms.length; i++) {
            //System.out.println("INFO in CNFClause2.substitute(): checking term: " + terms[i]);
            if (m.containsKey(terms[i])) {
                //System.out.println("INFO in CNFClause2.substitute(): replacing: " + terms[i] + " with " + ((TermCell) m.get(terms[i])));
                terms[i] = (TermCell) m.get(terms[i]);
            }
            else 
                if (terms[i].isFunction())
                    terms[i].substitute(m);
        }
    }

    /** ***************************************************************
     *  Note that this routine assumes that the calling routine has
     *  copied the formula, since this routine has a side effect if
     *  substitution is successful.
    */
    public void substituteVar(TreeMap<TermCell, TermCell> m) {

        //System.out.println("INFO in CNFClause2.substituteVar(): map: " + m);
        for (int i = 0; i < terms.length; i++) {
            //System.out.println("INFO in CNFClause2.substituteVar(): checking term: " + terms[i]);
            if (terms[i].isVariable() && m.containsKey(terms[i])) {
                //System.out.println("INFO in CNFClause2.substituteVar(): replacing: " + terms[i] + " with " + ((TermCell) m.get(terms[i])));
                terms[i] = (TermCell) m.get(terms[i]);
            }
            else 
                if (terms[i].isFunction())
                    terms[i].substitute(m);
        }
    }

    /** **************************************************************
     *  Create constants to fill variables.
     */
    public void generateVariableValues() {

        TreeSet<TermCell> varList = collectVariables();
        TreeMap<TermCell,TermCell> vars = new TreeMap();
        Iterator it = varList.iterator();
        while (it.hasNext()) {
            TermCell v = (TermCell) it.next();
            STP3._GENSYM_COUNTER++;
            String value = "GenSym" + String.valueOf(STP3._GENSYM_COUNTER);
            int termNum = CNFClause2.encodeTerm(value);
            TermCell v2 = new TermCell();
            v2.term = termNum;
            vars.put(v,v2);
        }
        substituteVar(vars);
    }

    /** ***************************************************************
     *  @param f1 is the variable
     *  @see unify()
     */
    private static TreeMap<TermCell, TermCell> unifyVar(TermCell f1, TermCell f2, TermCell[] thisTerms, TermCell[] argTerms, 
                                               TreeMap<TermCell, TermCell> m) {

        if (f1.isVariable() && f1.deepEquals(f2)) 
            return m;
        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause2.unifyVar(): Attempting to unify : " + TermCell.printTerms(thisTerms) + 
                           " with " + TermCell.printTerms(argTerms));
        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause2.unifyVar(): and terms : " + f1 + 
                           " with " + f2);
        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause2.unifyVar(): map : " + m);
        if (!f1.isVariable()) {
            System.out.println("Error in CNFClause2.unifyVar(): Attempting to unify : " + 
                               TermCell.printTerms(thisTerms) + " with " + TermCell.printTerms(argTerms));
            System.out.println(": and terms : " + f1 + " with " + f2);
            System.out.println(f1 + " is not a variable");
            return null;
        }
        if (m.keySet().contains(f1)) 
            return unifyInternal(((TermCell) m.get(f1)),f2,thisTerms,argTerms,m);
        else if (m.keySet().contains(f2))         
            return unifyInternal(((TermCell) m.get(f2)),f1,argTerms,thisTerms,m);
        else if (containsVar(argTerms,f1))  {  // occurs-check
            if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause2.unifyVar(): Occurs check");
            if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause2.unifyVar(): argTerms: " + TermCell.printTerms(argTerms) + 
                                                 " contains " + f1);
            return null;
        }
        else {
            if (f1 != f2)                 
                m.put(f1.deepCopy(),f2.deepCopy());
            return m;            
        }
    }

    /** ***************************************************************
     *  @see unify()
     */
    private static TreeMap<TermCell, TermCell> unifyInternal(TermCell i1, TermCell i2, TermCell[] thisTerms, TermCell[] argTerms, 
                                                    TreeMap<TermCell, TermCell> m) {

        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause2.unifyInternal(): Attempting to unify : " + i1 + 
                                             " with " + i2 + 
                                             " in " + TermCell.printTerms(thisTerms) + " and " + TermCell.printTerms(argTerms));
        if (m == null) 
            return null;
        else if (i1.isConstant() && i1.term == i2.term) 
            return m;
        else if (i1.isVariable()) 
            return unifyVar(i1,i2,thisTerms,argTerms,m);
        else if (i2.isVariable()) 
            return unifyVar(i2,i1,argTerms,thisTerms,m);
        else if (i1.isFunction() && i2.isFunction()) {
            return unifyClausesInternal(i1.argList,i2.argList,m);
        }
        else {
            if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause2.unifyInternal(): failed to unify");
            return null;
        }
    }

    /** ***************************************************************
     *  @see unify()
     */
    private static TreeMap<TermCell, TermCell> unifyClausesInternal(TermCell[] thisTerms, TermCell[] argTerms, TreeMap<TermCell, TermCell> m) {

        if (m == null || thisTerms == null || argTerms == null || thisTerms.length != argTerms.length) 
            return null;
        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause2.unifyClausesInternal(): Attempting to unify : " + 
                                             TermCell.printTerms(thisTerms) + " with " + TermCell.printTerms(argTerms));
        TreeMap<TermCell, TermCell> result = new TreeMap();
        int thisPointer = 0;
        while (result != null && thisPointer < thisTerms.length) {
            result = unifyInternal(thisTerms[thisPointer],argTerms[thisPointer],
                                   thisTerms,argTerms,m);
            if (result == null) 
                return null;
            thisPointer++;
        }
        return result;
    }

    /** ***************************************************************
     *  Attempt to unify one formula with another. @return a Map of
     *  variable substitutions if successful, null if not. If two
     *  formulas are identical the result will be an empty (but not
     *  null) TreeMap. Algorithm is after Russell and Norvig's AI: A
     *  Modern Approach p303.  But R&N's algorithm assumes that
     *  variables are within the same scope, which is not the case
     *  when unifying clauses in resolution.  This needs to be
     *  corrected by duplicating function indexes so they are the
     *  same in each clause, and renaming variables so each clause
     *  does not duplicate names from the other.
     *  There is no side effect.
     */
    public TreeMap<TermCell, TermCell> unify(CNFClause2 arg) {

        if (this.terms.length != arg.terms.length) 
            return null;
        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause2.unify(): Attempting to unify : " + this + 
                          " with " + arg);
        TreeMap<TermCell, TermCell> result = new TreeMap();
        return unifyClausesInternal(this.terms,arg.terms,result);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void test1() {

        System.out.println("--------------------INFO in CNFClause1.test1()--------------");
        Formula f2 = new Formula();
        f2.read("(m (SkFn 1 ?VAR1) ?VAR1)");
        CNFFormula2 cnf2 = new CNFFormula2(f2);
        cnf2.makeVarsEven();
        Formula f3 = new Formula();
        f3.read("(m ?VAR2 Org1-1)");
        CNFFormula2 cnf3 = new CNFFormula2(f3);
        cnf3.makeVarsOdd();
        System.out.println("INFO in CNFClause2.test1(): cnf2: " + cnf2);
        System.out.println("INFO in CNFClause2.test1(): cnf3: " + cnf3);
        TreeMap tm = cnf3.clauses[0].unify(cnf2.clauses[0]);
        System.out.println("INFO in CNFClause2.test1(): Map result: " + tm);
        if (tm != null) {
            cnf3 = cnf3.substitute(tm);
            cnf2 = cnf2.substitute(tm);
            System.out.println("INFO in CNFClause2.test1(): Substitution result: " + 
                               cnf3);
        }
        System.out.println("INFO in CNFClause2.test1(): cnf2: " + cnf2);
        System.out.println("INFO in CNFClause2.test1(): cnf3: " + cnf3);

        CNFFormula2 target = new CNFFormula2();
        target.read("(m (SkFn 1 Org1-1) Org1-1)");

        if (cnf2.deepEquals(target)) 
            System.out.println("Successful test");
        else
           System.out.println("Result: \n " + cnf3 + "\n not equal to target result: \n" + target);          
    }

    /** ***************************************************************
     * A test method.
     */
    public static void test2() {

        System.out.println("--------------------INFO in CNFClause2.test2()--------------");
        Formula f2 = new Formula();
        f2.read("(attribute (SkFn Jane) Investor)");
        CNFFormula2 cnf2 = new CNFFormula2(f2);
        cnf2.makeVarsEven();
        Formula f3 = new Formula();
        f3.read("(attribute ?X Investor)");
        CNFFormula2 cnf3 = new CNFFormula2(f3);
        cnf3.makeVarsOdd();
        System.out.println("INFO in CNFClause2.test2(): cnf2: " + cnf2);
        System.out.println("INFO in CNFClause2.test2(): cnf3: " + cnf3);
        TreeMap tm = cnf3.clauses[0].unify(cnf2.clauses[0]);
        System.out.println("INFO in CNFClause2.test2(): Map result: " + tm);
        if (tm != null) {
            cnf3 = cnf3.substitute(tm);
            System.out.println("INFO in CNFClause2.test2(): Substitution result: " + 
                               cnf3);
        }
        System.out.println("INFO in CNFClause2.test2(): cnf2: " + cnf2);
        System.out.println("INFO in CNFClause2.test2(): cnf3: " + cnf3);

        CNFFormula2 target = new CNFFormula2();
        target.read("(attribute (SkFn Jane) Investor)");

        if (cnf3.deepEquals(target)) 
            System.out.println("Successful test");
        else
           System.out.println("Result: \n " + cnf3 + "\n not equal to target result: \n" + target);          
    }

    /** ***************************************************************
     * A test method.
     */
    public static void test3() {

        System.out.println("--------------------INFO in CNFClause2.test3()--------------");
        Formula f2 = new Formula();
        f2.read("(not (agent ?VAR4 ?VAR1))");
        CNFFormula2 cnf2 = new CNFFormula2(f2);
        cnf2.makeVarsEven();
        Formula f3 = new Formula();
        f3.read("(agent ?VAR5 West)");
        CNFFormula2 cnf3 = new CNFFormula2(f3);
        cnf3.makeVarsOdd();
        System.out.println("INFO in CNFClause2.test3(): cnf2: " + cnf2);
        System.out.println("INFO in CNFClause2.test3(): cnf3: " + cnf3);
        TreeMap tm = cnf3.clauses[0].unify(cnf2.clauses[0]);
        System.out.println("INFO in CNFClause2.test3(): Map result: " + tm);
        if (tm != null) {
            cnf3 = cnf3.substitute(tm);
            System.out.println("INFO in CNFClause2.test3(): Substitution result: " + 
                               cnf3);
        }
        System.out.println("INFO in CNFClause2.test3(): cnf2: " + cnf2);
        System.out.println("INFO in CNFClause2.test3(): cnf3: " + cnf3);
    } 

    /** ***************************************************************
     * A test method.
     */
    public static void test4() {

        System.out.println("--------------------INFO in CNFClause2.test4()--------------");
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();
        cnf1.read("(equal (ListOrderFn (ListFn ?VAR1 ?VAR2)) ?VAR2)");

        cnf1.makeVarsEven();
        cnf2.read("(not (equal (ImmediateFamilyFn ?VAR3) Org1-1))");
        cnf2.makeVarsOdd();
        System.out.println("INFO in CNFClause2.test4(): cnf1: " + cnf1);
        System.out.println("INFO in CNFClause2.test4(): cnf2: " + cnf2);
        TreeMap tm = cnf1.clauses[0].unify(cnf2.clauses[0]);
        System.out.println("INFO in CNFClause2.test4(): mapping: " + tm);
        if (tm != null) 
            System.out.println(cnf1.substitute(tm));
        System.out.println("INFO in CNFClause2.test4(): cnf1: " + cnf1);
        System.out.println("INFO in CNFClause2.test4(): cnf2: " + cnf2);
        System.out.println("INFO in CNFClause2.test4(): should fail to unify ");
    } 

    /** ***************************************************************
     * A test method.
     */
    public static void test5() {

        System.out.println("--------------------INFO in CNFClause2.test5()--------------");
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();
        cnf1.read("(equal ?VAR1 ?VAR2)");
        cnf1.makeVarsEven();
        cnf2.read("(not (equal (ImmediateFamilyFn ?VAR3) Org1-1))");
        cnf2.makeVarsOdd();

        System.out.println("INFO in CNFClause2.test5(): cnf1: " + cnf1);
        System.out.println("INFO in CNFClause2.test5(): cnf2: " + cnf2);
        TreeMap tm = cnf1.clauses[0].unify(cnf2.clauses[0]);
        System.out.println(tm);
        CNFFormula2 result = null;
        if (tm != null) {
            result = cnf1.substitute(tm);
            System.out.println();
        }
        System.out.println("INFO in CNFClause2.test5(): cnf1: " + cnf1);
        System.out.println("INFO in CNFClause2.test5(): cnf2: " + cnf2);

        CNFFormula2 target = new CNFFormula2();
        target.read("(equal (ImmediateFamilyFn ?VAR1) Org1-1)");

        if (result.deepEquals(target)) 
            System.out.println("Successful test");
        else
           System.out.println("Result: \n " + result + "\n not equal to target result: \n" + target);                  
    } 

    /** ***************************************************************
     * A test method.
     */
    public static void test6() {

        System.out.println("--------------------INFO in CNFClause2.test6()--------------");
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();
        cnf1.read("(equal (ListOrderFn (SkFn 158 ?VAR11 ?VAR10 ?VAR5 ?VAR9) (SuccessorFn ?VAR10)) (SkFn 159 ?VAR11 ?VAR10 ?VAR5 ?VAR9))");
        cnf1.makeVarsEven();
        cnf2.read("(equal (ListOrderFn (SkFn 158 ?VAR8 ?VAR7 Org1-1 ?VAR6) (SuccessorFn ?VAR7)) (SkFn 159 ?VAR8 ?VAR7 Org1-1 ?VAR6))");
        cnf2.makeVarsOdd();

        System.out.println("INFO in CNFClause2.test6(): cnf1: " + cnf1);
        System.out.println("INFO in CNFClause2.test6(): cnf2: " + cnf2);
        TreeMap tm = cnf1.clauses[0].unify(cnf2.clauses[0]);
        System.out.println(tm);
        CNFFormula2 result = null;
        if (tm != null) {
            result = cnf1.substitute(tm);
            System.out.println();

            System.out.println("INFO in CNFClause2.test6(): cnf1: " + cnf1);
            System.out.println("INFO in CNFClause2.test6(): cnf2: " + cnf2);

            CNFFormula2 target = new CNFFormula2();
            target.read("(equal (ListOrderFn (SkFn 158 ?VAR8 ?VAR7 Org1-1 ?VAR6) (SuccessorFn ?VAR7)) (SkFn 159 ?VAR8 ?VAR7 Org1-1 ?VAR6))");

            if (result.equals(target)) 
                System.out.println("Successful test");
            else
               System.out.println("Result: \n " + result + "\n not equal to target result: \n" + target);                  
        }
        else
            System.out.println("Failed to unify.");                  
    } 

    /** ***************************************************************
     * A test method.
     */
    public static void occursTest() {

        System.out.println("--------------------INFO in CNFClause2.occursTest()--------------");
        CNFFormula2 cnf1 = new CNFFormula2();
        CNFFormula2 cnf2 = new CNFFormula2();
        cnf1.read("(instance (SubtractionFn 0 ?VAR4) NonnegativeRealNumber)");
        cnf2.read("(instance ?VAR4 NonnegativeRealNumber)");

        System.out.println("INFO in CNFClause2.occursTest(): cnf1: " + cnf1);
        System.out.println("INFO in CNFClause2.occursTest(): cnf2: " + cnf2);
        TreeMap<TermCell,TermCell> tm = cnf1.clauses[0].unify(cnf2.clauses[0]);
        System.out.println(tm);
    } 

    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {

        test1();
        test2();
        test3();
        test4();
        test5();
        test6();
        //occursTest();
    }
}
