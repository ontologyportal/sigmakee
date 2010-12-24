
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
 *  Each clause is part of a conjunctive normal form formula.
 *  It may be negated or not.  Each element in the clause is
 *  either a variable, a numbered constant, or an index to a
 *  functional term, which is itself an array of constants and
 *  variables.  A function may itself contain a function.
 */
public class CNFClause implements Comparable {

    boolean _UNIFY_DEBUG = false;

    public boolean negated = false;

    /** Variables are numbers are 1 to 99. Function pointers are
     * 101-999. Constants are number 1001 to (2^31-1).
     * Limited to 6 arguments plus predicate. Constant values are
     * globally unique.  Variable and function indexes are only
     * locally unique within the scope of the formula. */
    public int[] args = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public static final int VARSTARTINDEX = 1;
    public static final int VARENDINDEX = 99;
    public static final int FUNCSTARTINDEX = 101;
    public static final int FUNCENDINDEX = 999;
    public static final int CONSTARTINDEX = 1001;
    public static final int CONENDINDEX = Integer.MAX_VALUE;

    public int numArgs = 0;
    // public TreeMap<Integer, CNFClause> functions = new TreeMap();  // keys start at 101 

    /** ***************************************************************
    */
    public CNFClause() {
    }

    /** ***************************************************************
     *  Create a CNFClause instance from a Formula that is a single
     *  clause.
     *  @param varMap is a temporary map, with the scope of just one
     *                Formula, that describes how to map variable
     *                names to integers from 1 to 99
    */
    public CNFClause(Formula f, TreeMap<String, Integer> varMap, 
                     TreeMap<Integer,CNFClause> functions) {

        //System.out.println("INFO in CNFClause(): here 1");
        CNFClause newClause = CNFClause.createClause(f,varMap,functions);
        //System.out.println("INFO in CNFClause(): here 2");
        //System.out.println("INFO in CNFClause(): functions size: " + functions.size());
        args = newClause.args;
        numArgs = newClause.numArgs;
        negated = newClause.negated;
    }

    /** ***************************************************************
     *  Get the next number for a term for the global term map.
     */
    public static int encodeTerm(String term) {

        if (CNFFormula.termMap.keySet().contains(term)) 
            return ((Integer) CNFFormula.termMap.get(term)).intValue();
        int termNum = CNFFormula.termMap.size() + CONSTARTINDEX;
        CNFFormula.termMap.put(term,new Integer(termNum));
        CNFFormula.intToTermMap.put(new Integer(termNum),term);
        return termNum;
    }

    /** ***************************************************************
    */
    private static CNFClause createClause(Formula f, TreeMap<String, Integer> varMap,
                                          TreeMap<Integer,CNFClause> functions) {

        boolean _CREATE_DEBUG = false;

        if (_CREATE_DEBUG) System.out.println("INFO in CNFClause.createClause(): formula: " + f);
        CNFClause newClause = new CNFClause();
        Formula fnew = new Formula();
        fnew.read(f.theFormula);
        if (fnew.isSimpleNegatedClause()) {
            newClause.negated = true;
            fnew.read(fnew.cdr());
            fnew.read(fnew.car());
        }
        while (!fnew.empty()) {
            String arg = fnew.car();
            if (_CREATE_DEBUG) System.out.println("INFO in CNFClause.createClause(): arg: [" + arg + "]");
            fnew.read(fnew.cdr());

            if (newClause.numArgs > 11) {
                if (_CREATE_DEBUG) System.out.println("Error in CNFClause.createClause(): arg out of range converting formula: " + f);
                return null;
            }
            if (Formula.atom(arg) && !Formula.isVariable(arg)) {
                if (_CREATE_DEBUG) System.out.println("INFO in CNFClause.createClause(): atom");
                int termNum = 0;
                if (CNFFormula.termMap.keySet().contains(arg)) 
                    termNum = CNFFormula.termMap.get(arg);
                else
                    termNum = CNFClause.encodeTerm(arg);     
                newClause.args[newClause.numArgs++] = termNum;
            }
            if (Formula.isVariable(arg)) {
                if (_CREATE_DEBUG) System.out.println("INFO in CNFClause.createClause(): variable");
                if (varMap.keySet().contains(arg)) 
                    newClause.args[newClause.numArgs++] = ((Integer) varMap.get(arg)).intValue();
                else {
                    int varNum = varMap.keySet().size() + 1;
                    varMap.put(arg,new Integer(varNum));
                    newClause.args[newClause.numArgs++] = varNum;
                }
            }
            if (Formula.isFunctionalTerm(arg)) {
                if (_CREATE_DEBUG) System.out.println("INFO in CNFClause.createClause(): function");
                int funcNum = functions.keySet().size() + FUNCSTARTINDEX;
                functions.put(new Integer(funcNum),null);
                if (_CREATE_DEBUG) System.out.println("INFO in CNFClause.createClause(): added dummy function number: " + funcNum);
                Formula func = new Formula();
                func.read(arg);
                CNFClause cnew = createClause(func,varMap,functions);
                if (containsFunction(cnew,functions,functions)) {
                    if (_CREATE_DEBUG) System.out.println("INFO in CNFClause.createClause(): removing dummy function number: " + funcNum);
                    functions.remove(new Integer(funcNum));
                    funcNum = cnew.functionFoundAtIndex(0,functions,functions);
                }
                functions.put(new Integer(funcNum),cnew);
                if (_CREATE_DEBUG) System.out.println("INFO in CNFClause.createClause(): functions adding " + 
                                   funcNum + "," + cnew.toString(functions) + ")");
                newClause.args[newClause.numArgs++] = funcNum;
            }
        }
        if (_CREATE_DEBUG) System.out.println("INFO in CNFClause.createClause(): Arguments: ");
        if (_CREATE_DEBUG) newClause.printArgs();
        if (_CREATE_DEBUG) System.out.println("INFO in CNFClause.createClause(): Negated: " + newClause.negated);
        if (_CREATE_DEBUG) System.out.println("INFO in CNFClause.createClause(): functions: " + CNFFormula.printFunctions(functions));
        return newClause;
    }

    /** ***************************************************************
     *  Compares this object with the specified object for order.
     *  Note that it does not consider functions, and therefore may
     *  say two equal clauses are equal if the same function is
     *  given two distinct indexes.
     * 
     *  @return a negative integer, zero, or a positive integer as
     *  this object is less than, equal to, or greater than the
     *  specified object.
    */
    public int compareTo(Object cc) throws ClassCastException {

        //System.out.println("INFO in CNFClause.compareTo()");
        if (!cc.getClass().getName().equalsIgnoreCase("com.articulate.sigma.CNFClause")) 
            throw new ClassCastException("Error in CNFClause.compareTo(): "
                                         + "Class cast exception for argument of class: " 
                                         + cc.getClass().getName());
        //System.out.println("INFO in CNFClause.compareTo() Comparing \n" + this.toString(null) + "\n to \n" + ((CNFClause) cc).toString(null));
        if (negated && !((CNFClause) cc).negated) 
            return 1;
        if (!negated && ((CNFClause) cc).negated) 
            return -1;
        if (args.equals(((CNFClause) cc).args)) {
            //System.out.println("INFO in CNFClause.compareTo(): equal!");
            return 0;       
        }
        else {
            if (numArgs == ((CNFClause) cc).numArgs)
                return compareTo(args,((CNFClause) cc).args,numArgs);            
            else {
                if (numArgs < ((CNFClause) cc).numArgs) 
                    return compareTo(args,((CNFClause) cc).args,numArgs);
                if (numArgs < ((CNFClause) cc).numArgs) {
                    int res = compareTo(args,((CNFClause) cc).args,numArgs);
                    if (res == 0) 
                        return -1;
                    else
                        return res;
                }
                else {
                    int res = compareTo(args,((CNFClause) cc).args,((CNFClause) cc).numArgs);
                    if (res == 0) 
                        return 1;
                    else
                        return res;
                }
            }
        }           
    }

    /** ***************************************************************
    */
    private int compareTo(ArrayList<CNFClause> a1, ArrayList<CNFClause> a2, int maxsize) {

        int arg = 0;
        while (arg < maxsize && ((CNFClause) a1.get(arg)).compareTo((CNFClause) a2.get(arg)) == 0) 
            arg++;
        if (arg < maxsize)
            return ((CNFClause) a1.get(arg)).compareTo((CNFClause) a2.get(arg));
        if (arg == maxsize && a1.size() == a2.size()) {
            return 0;
        }
        return a1.size() - a2.size();
    }

    /** ***************************************************************
    */
    private int compareTo(ArrayList<CNFClause> a1, ArrayList<CNFClause> a2) {

        int maxsize = 0;
        if (a1.size() < a2.size()) 
            maxsize = a1.size();
        else
            maxsize = a2.size();
        return compareTo(a1,a2,maxsize);
    }

    /** ***************************************************************
     *  Compare two arrays of int
    */
    private int compareTo(int[] a1, int[] a2, int size) {

        int arg = 0;
        while (arg < size && a1[arg] == a2[arg]) 
            arg++;
        if (arg < size)
            return a2[arg] - a1[arg];
        return 0;
    }

    /** ***************************************************************
     *  @param fromIndex allows checking for circular function
     *                   indexes
     *  @return a boolean indicating whether the clause or its
     *          referenced functions contains the given value.
     */
    private boolean containsInternal(Integer t, TreeMap<Integer, CNFClause> functions, int fromIndex) {

        //System.out.println("INFO in CNFClause.containsInternal(): Check for appearance of  " + t + " in " + this.toString(functions));
        int i = 0;
        while (i < numArgs) {
            //System.out.println(args[i]);
            if (t.intValue() == args[i]) 
                return true;
            if (fromIndex == args[i]) {
                System.out.println("Error in CNFClause.containsInternal(): circular function index: " + args[i]);
                CNFFormula.printFunctions(functions);
                return false;
            }
            if (isFunction(args[i])) {
                CNFClause c = (CNFClause) functions.get(args[i]);
                if (c == null) {
                    System.out.println("Error in CNFClause.containsInternal(): bad function index: " + args[i]);
                    CNFFormula.printFunctions(functions);
                    return false;
                }
                if (c.containsInternal(t,functions,args[i])) 
                    return true;
            }
            i++;
        }        
        return false;
    }

    /** ***************************************************************
     *  @return a boolean indicating whether the clause or its
     *          referenced functions contains the given value.
     */
    public boolean contains(Integer t, TreeMap<Integer, CNFClause> functions) {

        return containsInternal(t,functions,-1);
    }

    /** ***************************************************************
     */
    public boolean equals(CNFClause c, TreeMap<Integer, CNFClause> thisFunctions, TreeMap<Integer, CNFClause> cFunctions) {

        if (c.negated != negated || c.numArgs != numArgs || c.args != args) 
            return false;
        if ((cFunctions == null || cFunctions.size() < 1) && (thisFunctions == null || thisFunctions.size() < 1)) 
            return true;
        
        return true;
    }

    /** ***************************************************************
     * @return true if this is equal to the first argument, false
     * otherwise.  Check whether all functions are really equal, not
     * just sharing an index.
     */
    public boolean deepEquals(CNFClause c, TreeMap<Integer, CNFClause> thisFunctions, TreeMap<Integer, CNFClause> cFunctions) {

        //System.out.println("INFO in CNFClause.deepEquals(): Comparing : " + this.toString(thisFunctions) + 
        //                   " with " + c.toString(cFunctions));
        if (c == null) {
            System.out.println("Error in CNFClause.deepEquals(): c is null");
            return false;
        }
        if (c.negated != negated || c.numArgs != numArgs) 
            return false;
        for (int i = 0; i < numArgs; i++) {
            if (isFunction(args[i])) {
                if (isFunction(c.args[i])) {
                    CNFClause thisClause = (CNFClause) thisFunctions.get(new Integer(args[i]));
                    CNFClause argClause = (CNFClause) cFunctions.get(new Integer(c.args[i]));
                    if (thisClause == null) {
                        System.out.println("Error in CNFClause.deepEquals(): function index " + args[i] + 
                                           " is null for clause" + this.toString(thisFunctions));
                        return false;
                    }
                    if (argClause == null) {
                        System.out.println("Error in CNFClause.deepEquals(): function index " + c.args[i] + 
                                           " is null for clause" + c.toString(cFunctions));
                        return false;
                    }
                    if (!thisClause.deepEquals(argClause,thisFunctions,cFunctions)) 
                        return false;
                }
                else
                    return false;
            }
            else
                if (args[i] != c.args[i]) 
                    return false;
        }
        return true;
    }

    /** ***************************************************************
     */
    private int sizeRecurse(TreeMap<Integer, CNFClause> functions, boolean recurse) {

        int result = 0;
        result = result + numArgs;
        if (recurse) {
            Iterator it = functions.keySet().iterator();
            while (it.hasNext()) {
                Integer key = (Integer) it.next();
                CNFClause f = (CNFClause) functions.get(key);
                result = result + f.sizeRecurse(functions,false);
            }
        }
        return result;
    }

    /** ***************************************************************
     */
    public int size(TreeMap<Integer, CNFClause> functions) {
        
        return sizeRecurse(functions,true);
    }

    /** ***************************************************************
     * Copy
     */
    public CNFClause deepCopy() {

        CNFClause result = new CNFClause();
/*
        Iterator it = functions.keySet().iterator();
        while (it.hasNext()) {
            Integer key = (Integer) it.next();
            CNFClause c = (CNFClause) functions.get(key);;
            result.functions.add(c.deepCopy());
        }
        */
        result.numArgs = numArgs;
        result.negated = negated;
        for (int i = 0; i < numArgs; i++) 
            result.args[i] = args[i];        
        return result;
    }

    /** ***************************************************************
     */
    public boolean isGround(TreeMap<Integer, CNFClause> functions) {

        for (int i = 0; i < numArgs; i++) {
            if (isVariable(args[i])) 
                return false;
            if (isFunction(args[i])) {
                CNFClause c = (CNFClause) functions.get(new Integer(args[i]));;
                if (!c.isGround(functions)) 
                    return false;            
            }
        }
        return true;
    }

    /** ***************************************************************
     */
    public boolean isEmpty() {

        return numArgs == 0;
    }

    /** ***************************************************************
    */
    private String spaces(int num) {

        StringBuffer result = new StringBuffer();
        for (int space = 0; space < num; space++) 
            result.append(" ");
        return result.toString();
    }

    /** ***************************************************************
    */
    private void printArgs() {
        for (int i = 0; i < numArgs; i++) {
            System.out.print(args[i] + " ");
        }
        System.out.println();
    }

    /** ***************************************************************
     */
    public static boolean isVariable(int i) {

        if (i >= VARSTARTINDEX && i <= VARENDINDEX) 
            return true;
        else
            return false;
    }

    /** ***************************************************************
     */
    public static boolean isConstant(int i) {

        if (i >= CONSTARTINDEX && i <= CONENDINDEX) 
            return true;
        else
            return false;
    }

    /** ***************************************************************
     */
    public static boolean isFunction(int i) {

        if (i >= FUNCSTARTINDEX && i <= FUNCENDINDEX) 
            return true;
        else
            return false;
    }

    /** ***************************************************************
     * Get all the indexes of functions in the formula
     */
    public ArrayList<Integer> collectFunctionIndices() {

        ArrayList<Integer> result = new ArrayList();
        for (int i = 0; i < numArgs; i++) {
            if (isFunction(args[i])) 
                result.add(new Integer(args[i]));
        }
        return result;
    }

    /** ***************************************************************
     * Get all the indexes of terms in the formula
     */
    private ArrayList<Integer> collectTermsRecurse(TreeMap<Integer, CNFClause> functions, boolean recurse) {

        ArrayList<Integer> result = new ArrayList();
        for (int i = 0; i < numArgs; i++) {
            if (isConstant(args[i])) 
                result.add(new Integer(args[i]));
        }
        if (recurse) {
            Iterator it = functions.keySet().iterator();
            while (it.hasNext()) {
                Integer key = (Integer) it.next();
                CNFClause c = (CNFClause) functions.get(key);;
                result.addAll(c.collectTermsRecurse(functions,false));
            }
        }
        return result;
    }

    /** ***************************************************************
     * Get all the indexes of terms in the formula
     */
    public ArrayList<Integer> collectTerms(TreeMap<Integer, CNFClause> functions) {

        return collectTermsRecurse(functions,true);
    }

    /** ***************************************************************
     * Renumber variables according to the supplied Map
     */
    public CNFClause substituteVariables(TreeMap<Integer,Integer> varMap) {

        CNFClause result = new CNFClause();
        result.numArgs = numArgs;
        result.negated = negated;
        //System.out.println("INFO in CNFClause.substituteVariables(): Attempting to renumber : " + this.toString(functions) + " with " + varMap);
        for (int i = 0; i < numArgs; i++) {
            if (args[i] >= CNFClause.VARSTARTINDEX && args[i] <= CNFClause.VARENDINDEX && varMap.containsKey(new Integer(args[i]))) {
                System.out.println("Substituting " + args[i] + " as " + ((Integer) varMap.get(new Integer(args[i]))).intValue());
                result.args[i] = ((Integer) varMap.get(new Integer(args[i]))).intValue();
            }
            else {
                result.args[i] = args[i];
            }
        }
        //System.out.println("INFO in CNFClause.substituteVariables(): result : " + this.toString(functions));
        return result;
    }

    /** ***************************************************************
     * Renumber variables according to the supplied Map
     */
    public CNFClause substitute(TreeMap<Integer,Integer> varMap) {

        CNFClause result = new CNFClause();
        result.numArgs = numArgs;
        result.negated = negated;
        //System.out.println("INFO in CNFClause.substitute(): Attempting to renumber : " + this.toString(null) + " with " + varMap);
        for (int i = 0; i < numArgs; i++) {
            if (varMap.containsKey(new Integer(args[i]))) {
                //System.out.println("Substituting " + args[i] + " as " + ((Integer) varMap.get(new Integer(args[i]))).intValue());
                result.args[i] = ((Integer) varMap.get(new Integer(args[i]))).intValue();
            }
            else {
                result.args[i] = args[i];
            }
        }
        //System.out.println("INFO in CNFClause.substitute(): result : " + this.toString(null));
        return result;
    }

    /** ***************************************************************
     * Get all the indexes of variables in the formula, without
     * duplication.
     */
    public ArrayList<Integer> collectVariables() {

        //System.out.println("INFO in CNFClause.collectVariables(): " + this.toString(functions));
        //System.out.println("INFO in CNFClause.collectVariables(): " + this.asIntegerList(functions));
        //System.out.println("INFO in CNFClause.collectVariables(): numArgs : " + numArgs);
        ArrayList<Integer> result = new ArrayList();
        for (int i = 0; i < numArgs; i++) {
            //System.out.println(args[i]);
            if (args[i] >= CNFClause.VARSTARTINDEX && args[i] <= CNFClause.VARENDINDEX && !result.contains(new Integer(args[i]))) 
                result.add(new Integer(args[i]));
            /*
            else if (args[i] >= CNFClause.FUNCSTARTINDEX && args[i] <= CNFClause.FUNCENDINDEX) {
                if (functions == null || !functions.keySet().contains(new Integer(args[i]))) {
                    System.out.println("Error in CNFClause.collectVariables(): Bad function index: " + args[i] +
                                       " for clause: " + this.toString(functions) + 
                                       " and function map: " + CNFFormula.printFunctions(functions));
                    System.out.println(this.asIntegerList(functions));
                }
                else {
                    CNFClause c = (CNFClause) functions.get(new Integer(args[i]));
                    result.addAll(c.collectVariables(functions));
                }
            }
            */
        }
        //System.out.println("INFO in CNFClause.collectVariables(): result : " + result);
        return result;
    }

    /** ***************************************************************
     * Get all the indexes of variables in the formula, without
     * duplication.
     * @param fromIndex provides the index of this clause, if it is
     *                  a function, in order to trap cycles
     */
    private ArrayList<Integer> collectVariables(TreeMap<Integer, CNFClause> functions, int fromIndex) {

        //System.out.println("INFO in CNFClause.collectVariables(): " + this.toString(functions));
        //System.out.println("INFO in CNFClause.collectVariables(): " + this.asIntegerList(functions));
        //System.out.println("INFO in CNFClause.collectVariables(): numArgs : " + numArgs);
        ArrayList<Integer> result = new ArrayList();
        for (int i = 0; i < numArgs; i++) {
            //System.out.println(args[i]);
            if (args[i] >= CNFClause.VARSTARTINDEX && args[i] <= CNFClause.VARENDINDEX && !result.contains(new Integer(args[i]))) 
                result.add(new Integer(args[i]));
            
            else if (args[i] >= CNFClause.FUNCSTARTINDEX && args[i] <= CNFClause.FUNCENDINDEX) {
                if (functions == null || !functions.keySet().contains(new Integer(args[i]))) {
                    System.out.println("Error in CNFClause.collectVariables(): Bad function index: " + args[i] +
                                       " for clause: " + this.toString(functions) + 
                                       " and function map: " + CNFFormula.printFunctions(functions));
                    System.out.println(this.asIntegerList(functions));
                }
                else {
                    if (args[i] == fromIndex) {
                        System.out.println("Error in CNFClause.collectVariables(): Circular function index: " + args[i] +
                                           " for clause: " + this.toString(functions) + 
                                           " and function map: " + CNFFormula.printFunctions(functions));
                    }
                    else {
                        CNFClause c = (CNFClause) functions.get(new Integer(args[i]));
                        result.addAll(c.collectVariables(functions,args[i]));
                    }
                }
            }            
        }
        //System.out.println("INFO in CNFClause.collectVariables(): result : " + result);
        return result;
    }

    /** ***************************************************************
     * Get all the indexes of variables in the formula, without
     * duplication.
     * @param fromIndex provides the index of this clause, if it is
     *                  a function, in order to trap cycles
     */
    public ArrayList<Integer> collectVariables(TreeMap<Integer, CNFClause> functions) {
        return collectVariables(functions,-1);
    }

    /** ***************************************************************
     */
    private void setTermPointer(TreeMap<Integer, ArrayList<CNFFormula>> termPointers,
                                int term, CNFFormula f) {

        ArrayList<CNFFormula> al = null;
        if (termPointers.get(new Integer(term)) == null) {
            al = new ArrayList();
            termPointers.put(new Integer(term),al);
        }
        else
            al = (ArrayList) termPointers.get(new Integer(term));
        al.add(f);
    }

    /** ***************************************************************
     *  Set pointers as a side effect.
     *  @param negated is used in case this clause is a function
     *                 that is within a negated clause.
     */
    public void setTermPointers(TreeMap<Integer, ArrayList<CNFFormula>> posTermPointers,
                                TreeMap<Integer, ArrayList<CNFFormula>> negTermPointers,
                                CNFFormula f, boolean neg) {

        boolean isNegated = neg;
        if (!isNegated)             // a negated parameter overrides the local value
            isNegated = negated;            
        for (int i = 0; i < numArgs; i++) {
            if (isVariable(args[i])) 
                continue;
            else if (isFunction(args[i])) {
                CNFClause c = (CNFClause) f.functions.get(args[i]);
                c.setTermPointers(posTermPointers,negTermPointers,f,isNegated);
                continue;
            }
            else {
                if (isNegated) 
                    setTermPointer(negTermPointers,args[i],f);
                else
                    setTermPointer(posTermPointers,args[i],f);
            }
        }
    }

    /** **************************************************************
     *  Convenience routine that creates constants to fill variables
     *  and returns a formula.
     */
    public CNFClause generateVariableValues(TreeMap<Integer, CNFClause> functions) {

        ArrayList<Integer> varList = collectVariables(functions);
        TreeMap<Integer,Integer> vars = new TreeMap<Integer,Integer>();
        for (int i = 0; i < varList.size(); i++) {
            Integer varNum = (Integer) varList.get(i);
            STP2._GENSYM_COUNTER++;
            String value = "GenSym" + String.valueOf(STP2._GENSYM_COUNTER);
            int termNum = CNFClause.encodeTerm(value);
            vars.put(varNum,new Integer(termNum));
        }
        return instantiateVariables(vars,null,true);
    }


    /** ***************************************************************
     *  Replace existing values with a value as given by the map
     *  argument.  Changes to argument "functions" are a side
     *  effect.
     */
    public CNFClause replaceValues(Map<Integer,Integer> m, TreeMap<Integer, CNFClause> functions,
                                   boolean recurse) {

        //System.out.println("INFO in CNFClause.replaceValues(): Attempting to instantiate : " + this + " with " + m);
        CNFClause c = new CNFClause();
        c.numArgs = numArgs;
        c.negated = negated;
        for (int i = 0; i < numArgs; i++) {
            if (m.keySet().contains(new Integer(args[i])))
                c.args[i] = ((Integer) m.get(new Integer(args[i]))).intValue();            
            else
                c.args[i] = args[i];
        }
        if (functions != null && recurse) {
            TreeMap<Integer, CNFClause> tempFunctions = new TreeMap();
            Iterator it = functions.keySet().iterator();
            while (it.hasNext()) {
                Integer key = (Integer) it.next();
                CNFClause c2 = (CNFClause) functions.get(key);
                tempFunctions.put(key,c2.replaceValues(m,functions,false));
            }
            functions.clear();
            functions.putAll(tempFunctions);
        }
        //System.out.println("INFO in CNFClause.replaceValues(): result : " + c);
        return c;
    }

    /** ***************************************************************
     *  Replace variables with a value as given by the map argument.
     *  Changes to functions are a side effect.
     *  @param funcs is a clause that may contain functions that
     *               need to be substituted
     *  @param recurse specifies whether to instantiate the
     *                 functions
     */
    public CNFClause instantiateVariables(Map<Integer,Integer> m, 
                                          TreeMap<Integer, CNFClause> functions,
                                          boolean recurse) {

        //System.out.println("INFO in CNFClause.instantiateVariables(): Attempting to instantiate : " + this + 
        //                   " with " + m);
        //if (funcs != null) System.out.println(" and functions " + funcs.printFunctions() + " from " + funcs);
        CNFClause c = new CNFClause();
        c.numArgs = numArgs;
        c.negated = negated;
        for (int i = 0; i < numArgs; i++) {
            if ((args[i] >= CNFClause.VARSTARTINDEX && args[i] <= CNFClause.VARENDINDEX) &&
                (m.keySet().contains(new Integer(args[i])))) {
                Integer substitution = (Integer) m.get(new Integer(args[i]));
                c.args[i] = substitution.intValue();
            }
            else
                c.args[i] = args[i];
        }
        if (functions != null && recurse) {
            Iterator it = functions.keySet().iterator();
            while (it.hasNext()) {
                Integer key = (Integer) it.next();
                CNFClause c2 = (CNFClause) functions.get(key);
                functions.put(key,c2.instantiateVariables(m,functions,false));
            }
        }
        //System.out.println("INFO in CNFClause.instantiateVariables(): result : " + c);
        return c;
    }

    /** ***************************************************************
     *  @return whether the given clause is the opposite sign from
     *          this clause
     */
    public boolean opposites(CNFClause c) {

        if ((negated && !c.negated) || (!negated && c.negated)) 
            return true;
        else
            return false;
    }

    /** ***************************************************************
     *  If this clause, which gets its embedded functions from the
     *  first argument, is equal to a function in the second
     *  argument, return the intValue of the key to that function in
     *  the second argmment.
     */
    public int functionFoundAtIndex(int key, TreeMap<Integer, CNFClause> functions1, 
                                    TreeMap<Integer, CNFClause> functions2) {

        //System.out.println("INFO in CNFClause.functionFoundAtIndex(): this : " + this.toString(functions1));
        Iterator it = functions2.keySet().iterator();
        while (it.hasNext()) {
            Integer funckey = (Integer) it.next();
            CNFClause value = (CNFClause) functions2.get(funckey);
            if (value != null) {
                //System.out.println("INFO in CNFClause.functionFoundAtIndex(): key: " + funckey);
                //System.out.println("INFO in CNFClause.functionFoundAtIndex(): value:\n" + value.toString(functions2));
                if (this.deepEquals(value,functions1,functions2)) 
                    return funckey.intValue();
            }
        }
        return key + functions1.size();
    }

    /** ***************************************************************
     */
    public static boolean containsFunction(CNFClause c, TreeMap<Integer, CNFClause> cfunctions,
                                           TreeMap<Integer, CNFClause> functions) {

        //System.out.println("INFO in CNFClause.containsFunction(): c : " + c.toString(cfunctions));
        Iterator it = functions.keySet().iterator();
        while (it.hasNext()) {
            Integer funckey = (Integer) it.next();
            CNFClause value = (CNFClause) functions.get(funckey);
            //System.out.println("INFO in CNFClause.containsFunction(): key : " + funckey);
            if (value == null) {
                //System.out.println("INFO in CNFClause.containsFunction(): null value for key " + funckey +
                //                   " (likely the dummy value from CNFClause.createClause() )");
            }
            else {
                //System.out.println("INFO in CNFClause.containsFunction(): value: " + value.toString(functions));
                if (c.deepEquals(value,cfunctions,functions)) 
                    return true;
            }
        }
        return false;
    }

    /** ***************************************************************
     *  @param f1 is the variable
     *  @see unify()
     */
    private TreeMap<Integer, Integer> unifyVar(int f1, int f2, CNFClause c1, CNFClause c2, 
                                               TreeMap<Integer, Integer> m,
                                               TreeMap<Integer, CNFClause> thisFunctions,
                                               TreeMap<Integer, CNFClause> cFunctions) {

        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyVar(): Attempting to unify : " + c1.toString(thisFunctions) + 
                           " with " + c2.toString(cFunctions));
        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyVar(): and terms : " + f1 + 
                           " with " + f2 + "(" + CNFFormula.intToTermMap.get(f2) + ")");
        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyVar(): map : " + m);
        if (!isVariable(f1)) {
            System.out.println("Error in CNFClause.unifyVar(): Attempting to unify : " + 
                               c1.toString(thisFunctions) + " with " + c2.toString(cFunctions));
            System.out.println(": and terms : " + f1 + " with " + f2);
            System.out.println(f1 + " is not a variable");
            return null;
        }
        if (m.keySet().contains(new Integer(f1))) 
            return unifyInternal(((Integer) m.get(f1)).intValue(),f2,c1,c2,m,thisFunctions,cFunctions);
        else if (m.keySet().contains(new Integer(f2)))         
            return unifyInternal(((Integer) m.get(f2)).intValue(),f1,c2,c1,m,thisFunctions,cFunctions);
        else if (c2.contains(f1,cFunctions))  {  // occurs-check
            if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyVar(): Occurs check");
            return null;
        }
        else {
            if (isFunction(f2)) {
                if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyVar(): is function : " + f2);
                m.put(f1,f2);
                if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyVar(): map : " + m);
                return m;
            }
            else {
                if (f1 != f2)                 
                    m.put(f1,f2);
                return m;
            }
        }
    }

    /** ***************************************************************
     *  @see unify()
     */
    private TreeMap<Integer, Integer> unifyInternal(int i1, int i2, CNFClause c1, CNFClause c2, 
                                                    TreeMap<Integer, Integer> m, 
                                                    TreeMap<Integer, CNFClause> thisFunctions,
                                                    TreeMap<Integer, CNFClause> cFunctions) {

        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyInternal(): term map : " + CNFFormula.intToTermMap);
        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyInternal(): Attempting to unify : " + i1 + "(" + CNFFormula.intToTermMap.get(i1) + ")" +
                           " with " + i2  + "(" + CNFFormula.intToTermMap.get(i2) + ")" + 
                                             " in " + c1.toString(thisFunctions) + " and " + c2.toString(cFunctions));
        //for (int i = 0; i < 1000000; i++) {
        //}
        if (m == null) 
            return null;
        else if (isConstant(i1) && i1 == i2) 
            return m;
        else if (isVariable(i1)) 
            return unifyVar(i1,i2,c1,c2,m,thisFunctions,cFunctions);
        else if (isVariable(i2)) 
            return unifyVar(i2,i1,c2,c1,m,cFunctions,thisFunctions);
        else if (isFunction(i1) && isFunction(i2)) {
            CNFClause form1 = (CNFClause) thisFunctions.get(new Integer(i1));
            CNFClause form2 = (CNFClause) cFunctions.get(new Integer(i2));
            return form1.unifyClausesInternal(form2,m,thisFunctions,cFunctions);
        }
        else {
            if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyInternal(): failed to unify");
            return null;
        }
    }

    /** ***************************************************************
     *  @see unify()
     */
    public TreeMap<Integer, Integer> unifyClausesInternal(CNFClause c, TreeMap<Integer, Integer> m,
                                                          TreeMap<Integer, CNFClause> thisFunctions,
                                                          TreeMap<Integer, CNFClause> cFunctions) {

        if (m == null || c == null) 
            return null;
        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyClausesInternal(): Attempting to unify : " + 
                                             this.toString(thisFunctions) + " with " + c.toString(cFunctions));
        if (_UNIFY_DEBUG) System.out.println("this func size: " + thisFunctions.size());
        if (_UNIFY_DEBUG) System.out.println("c func size: " + cFunctions.size());
        TreeMap<Integer, Integer> result = new TreeMap();
        int thisPointer = 0;
        while (result != null && thisPointer < numArgs) {
            result = unifyInternal(args[thisPointer],c.args[thisPointer],
                                   this,c,m,thisFunctions,cFunctions);
            if (result == null) 
                return null;
            thisPointer++;
        }
        return result;
    }

    /** ***************************************************************
     *  Alter clauses so that their variable
     *  indexes have shared scope, which in effect means that they
     *  are different.  For example, given (p ?V1 a) and (p c ?V1)
     *  the second clause will be changed to (p c ?V2) because the
     *  ?V1 in the original clauses do not refer to the same
     *  variable.  The first argument will be unaltered.
     */
    public ArrayList<CNFClause> unifyVariableScope(CNFClause c1, CNFClause c2, 
                                           TreeMap<Integer, CNFClause> functions1,
                                           TreeMap<Integer, CNFClause> functions2) {

        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyVariableScope(): begin"); 
        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyVariableScope(): c1: " + c1.toString(functions1) + " and c2: " + c2.toString(functions2)); 
        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyVariableScope(): c1: " + c1.asIntegerList(functions1) + " and c2: " + c2.asIntegerList(functions2)); 
        if (_UNIFY_DEBUG) System.out.println("c1 func size: " + functions1.size());
        if (_UNIFY_DEBUG) System.out.println("c2 func size: " + functions2.size());
        ArrayList<CNFClause> result = new ArrayList();        
        ArrayList<Integer> varList1 = c1.collectVariables(functions1);
        ArrayList<Integer> varList2 = c2.collectVariables(functions2);
        TreeMap<Integer, Integer> varMap = new TreeMap();
        for (int i = 0; i < varList2.size(); i++) {                 // renumber variables
            Integer varNew = (Integer) varList2.get(i);
            while (varList1.contains(varNew)) 
                varNew = new Integer(varNew.intValue() + 1);            
            varMap.put((Integer) varList2.get(i),varNew);
        }
        CNFClause c1new = c1.deepCopy();
        CNFClause c2new = c2.deepCopy();
        c2new = c2new.instantiateVariables(varMap,functions2,true);
        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyVariableScope(): middle : " + c1new.toString(functions1) + " and " + c2new.toString(functions2)); 
        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyVariableScope(): middle : " + c1new.asIntegerList(functions1) + " and " + c2new.asIntegerList(functions2)); 
        result.add(c1);
        result.add(c2);
        return result;
    }

    /** ***************************************************************
     *  Alter clauses so that their function indexes have shared scope,
     *  which in effect means that they are different.  The first
     *  argument will be largely unaltered, but will have the
     *  functions from the second argument included (although not
     *  referenced) in its function list.
     */
    public ArrayList<CNFClause> unifyFunctionScope(CNFClause c1, CNFClause c2, 
                                           TreeMap<Integer, CNFClause> functions1,
                                           TreeMap<Integer, CNFClause> functions2) {

        ArrayList<CNFClause> result = new ArrayList();        
        CNFClause c1new = c1.deepCopy();
        CNFClause c2new = c2.deepCopy();

        int c1FuncSize = functions1.keySet().size();     // start renumber functions
        int c2FuncSize = functions2.keySet().size();
        if (c1FuncSize > 0 && c2FuncSize > 0) {     
            TreeMap<Integer, Integer> funcMap = new TreeMap();
                // Since we can't remove the old {key,value} pairs while
                // iterating through them we add the new pairs to a new list
                // then clear the old one, and add them.
            TreeMap<Integer, CNFClause> tempFunctions = new TreeMap();
            Iterator it = functions2.keySet().iterator();
            while (it.hasNext()) {
                Integer key = (Integer) it.next();
                CNFClause value = (CNFClause) functions2.get(key);
                int newKey = value.functionFoundAtIndex(key.intValue(),functions2,functions1);
                // int newKey = key.intValue() + c1FuncSize;
                tempFunctions.put(new Integer(newKey),value);
                funcMap.put(key,new Integer(newKey));
                if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyFunctionScope(): renumbering key : " + 
                                                     key + " as " + newKey + " for " + value); 
            }
            functions2.clear();
            functions2.putAll(tempFunctions);           
            c2new = c2new.replaceValues(funcMap,functions2,true);
        }
        if (c2FuncSize > 0)
            functions1.putAll(functions2);            
        if (c1FuncSize > 0)
            functions2.putAll(functions1);        // end renumber functions

        result.add(c1new);
        result.add(c2new);
        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyFunctionScope(): after : " + c1new.toString(functions1) + " and " + c2new.toString(functions2)); 
        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unifyFunctionScope(): after : " + c1new.asIntegerList(functions1) + " and " + c2new.asIntegerList(functions2)); 
        if (_UNIFY_DEBUG) System.out.println("c1new func size: " + functions1.size());
        if (_UNIFY_DEBUG) System.out.println("c2new func size: " + functions2.size());
        return result;
    }

    /** ***************************************************************
     *  Alter clauses so that their function indexes and variable
     *  indexes have shared scope, which in effect means that they
     *  are different.  For example, given (p ?V1 a) and (p c ?V1)
     *  the second clause will be changed to (p c ?V2) because the
     *  ?V1 in the original clauses do not refer to the same
     *  variable.  The first argument will be largely unaltered, but
     *  will have the functions from the second argument included
     *  (although not referenced) in its function list.
     */
    public ArrayList<CNFClause> unifyScope(CNFClause c1, CNFClause c2, 
                                           TreeMap<Integer, CNFClause> functions1,
                                           TreeMap<Integer, CNFClause> functions2) {

        ArrayList<CNFClause> result = new ArrayList();        
        result = unifyVariableScope(c1,c2,functions1,functions2);
        CNFClause c1new = (CNFClause) result.get(0);
        CNFClause c2new = (CNFClause) result.get(1);
        result.clear();

        return unifyFunctionScope(c1new,c2new,functions1,functions2);
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
     *  Note that this method has a side effect on "this", which is
     *  to add functions from the argument.
     */
    public TreeMap<Integer, Integer> unify(CNFClause c, TreeMap<Integer, CNFClause> thisFunctions, 
                                           TreeMap<Integer, CNFClause> cFunctions) {

        //ArrayList<CNFClause> clausePair = unifyScope(this,c,thisFunctions,cFunctions);
            // both function lists should be the same at this point
        //CNFClause thisClause = (CNFClause) clausePair.get(0);
        //CNFClause c2 = (CNFClause) clausePair.get(1);
        if (this.numArgs != c.numArgs) 
            return null;
        CNFClause thisClause = this.deepCopy();
        CNFClause c2 = c.deepCopy();
        if (_UNIFY_DEBUG) System.out.println("INFO in CNFClause.unify(): Attempting to unify : " + thisClause + 
                          " with " + c2);
        if (_UNIFY_DEBUG) System.out.println("thisClause func size: " + thisFunctions.size());
        if (_UNIFY_DEBUG) System.out.println("c2 func size: " + cFunctions.size());
        TreeMap<Integer, Integer> result = new TreeMap();
        return thisClause.unifyClausesInternal(c2,result,thisFunctions,cFunctions);
    }

    /** ***************************************************************
     *  @param indent is the number of spaces to indent.  The sign
     *                of the integer is reversed if this is a
     *                function which shouldn't have a terminating
     *                carriage return.
     *  @param fromFunction is a safety parameter in case there is a
     *                      spurious cycle
    */
    public String toStringFormat(boolean format, int indent, TreeMap<Integer,CNFClause> functions, int fromFunction) {  

        //System.out.println("INFO in CNFClause.toString()");
        //System.out.println("INFO in CNFClause.toStringFormat(): format: " + format);
        //System.out.println("Functions: " + functions);
        //System.out.println("Term map: " + CNFFormula.intToTermMap);
        //System.out.println("Num args: " + numArgs);
        //System.out.println("negated: " + negated);
        if (indent > 100 || indent < -100) {
            System.out.println("Error in CNFClause.toStringFormat(): Error: excessive indent ");
            return "";
        }
        StringBuffer result = new StringBuffer();
        if (negated) {
            if (format)             
                result.append(spaces(indent) + "(not \n");
            else 
                result.append("(not ");
            //if (format) 
            //    result.append("true");
            //else
            //    result.append("false");            
            indent = indent + 2;
        }
        if (format) {
            if (indent < 0) 
                result.append("\n" + spaces(-indent));
            else
                result.append(spaces(indent));
        }
        result.append("(");
        for (int i = 0; i < numArgs; i++) {
            int arg = args[i];
            if (arg <= VARENDINDEX)                                             // variable
                result.append("?VAR" + String.valueOf(arg));
            if ((arg >= FUNCSTARTINDEX) && (arg <= FUNCENDINDEX)) {             // function
                int newIndent = indent;
                if (newIndent < 0)
                    newIndent = newIndent - 2;  
                else
                    newIndent = -newIndent - 2;
                if (functions != null && functions.keySet().contains(new Integer(arg))) {
                    if (arg == fromFunction) {
                        System.out.println("Error in CNFClause.toStringFormat(): Cycle from argument: " + arg);
                        result.append("\n");
                        return result.toString();
                    }
                    result.append(((CNFClause) functions.get(new Integer(arg))).toStringFormat(format,newIndent,functions,arg));                    
                }
                else {
                    System.out.println("Error in CNFClause.toString(): bad function index : " + arg);
                    if (functions != null) 
                        System.out.println("Number of functions : " + functions.size());
                    else
                        System.out.println("Functions = null");
                    result.append("F" + arg + "?");
                }
            }
            if (arg >= CONSTARTINDEX) {                                         // term
                if (CNFFormula.intToTermMap != null && (arg - (CONSTARTINDEX -1)) <= CNFFormula.intToTermMap.keySet().size() && 
                    CNFFormula.intToTermMap.get(new Integer(arg)) != null) {
                    result.append(CNFFormula.intToTermMap.get(new Integer(arg)));
                }
                else {
                    System.out.println("Error in CNFClause.toString(): bad term index : " + arg);
                    System.out.println(CNFFormula.intToTermMap.keySet().size());
                    return null;
                }
            }            
            if (i < numArgs - 1) 
                result.append(" ");                        
        }
        if (negated) result.append("))");
        else         result.append(")");
        if (indent == 0 && format) 
            result.append("\n");
        return result.toString();
    }

    /** ***************************************************************
     *  A debugging routine to print all functions, whether used or
     *  not.
    */
    public String printFunctions(TreeMap<Integer,CNFClause> functions) { 

        StringBuffer result = new StringBuffer();
        for (int i = 0; i < functions.size(); i++) {
            CNFClause cnf = (CNFClause) functions.get(i);
            result.append(cnf.toString() + "\n");
        }
        return result.toString();
    }

    /** ***************************************************************
    */
    public String toString(int indent,TreeMap<Integer,CNFClause> functions) {  
        return toStringFormat(true,indent,functions,0);
    }

    /** ***************************************************************
     *  @param indent is the number of spaces to indent.  The sign
     *                of the integer is reversed if this is a
     *                function which shouldn't have a terminating
     *                carriage return.
     * 
     *  @param fromFunction is a safety parameter in case there is a
     *                      spurious cycle
    */
    public String asIntegerList(int indent,TreeMap<Integer,CNFClause> functions, int fromFunction) {  

        //System.out.println("INFO in CNFClause.toString()");
        //if (functions != null)         
        //    System.out.println("Functions: " + CNFFormula.printFunctions(functions));
        //System.out.println("Term map: " + CNFFormula.intToTermMap);
        //System.out.println("Num args: " + numArgs);
        //System.out.println("negated: " + negated);
        StringBuffer result = new StringBuffer();
        if (negated) {
            result.append(spaces(indent) + "(not \n");
            indent = indent + 2;
        }
        if (indent < 0) 
            result.append("\n" + spaces(-indent));
        else
            result.append(spaces(indent));
        result.append("(");
        for (int i = 0; i < numArgs; i++) {
            int arg = args[i];
            if (arg <= VARENDINDEX)                                             // variable
                result.append(arg);
            if ((arg >= FUNCSTARTINDEX) && (arg <= FUNCENDINDEX)) {             // function
                int newIndent = indent;
                if (newIndent < 0)
                    newIndent = newIndent - 2;  
                else
                    newIndent = -newIndent - 2;
                if (functions != null && functions.containsKey(new Integer(arg))) {
                    if (arg == fromFunction) {
                        System.out.println("Error in CNFClause.toStringFormat(): Cycle from argument: " + arg);
                        result.append("\n");
                        return result.toString();
                    }
                    result.append(arg + "->" + ((CNFClause) functions.get(new Integer(arg))).asIntegerList(newIndent,functions,arg));
                }
                else {
                    //System.out.println("Error in CNFClause.toString(): bad function index : " + arg);
                    //if (functions != null)                     
                    //    System.out.println("Number of functions : " + functions.size());
                    result.append("F" + arg + "?");
                }
            }
            if (arg >= CONSTARTINDEX) {                                         // term
                if (CNFFormula.intToTermMap != null && (arg - (CONSTARTINDEX -1)) <= CNFFormula.intToTermMap.keySet().size() && 
                    CNFFormula.intToTermMap.get(new Integer(arg)) != null) {
                    result.append(arg);
                }
                else {
                    System.out.println("Error in CNFClause.toString(): bad term index : " + arg);
                    System.out.println(CNFFormula.intToTermMap.keySet().size());
                    return null;
                }
            }            
            if (i < numArgs - 1) 
                result.append(" ");                        
        }
        if (negated) result.append("))");
        else         result.append(")");
        if (indent >= 0) 
            result.append("\n");
        /*
        if (functions != null && functions.size() > 0) {
            // result.append("showing (possibly unused) functions: \n");
            Iterator it = functions.keySet().iterator();
            while (it.hasNext()) {
                Integer key = (Integer) it.next();
                CNFClause cnf = (CNFClause) functions.get(key);
                result.append(key + " ");
            }
        }
        */
        return result.toString();
    }

    /** ***************************************************************
    */
    public String toString(TreeMap<Integer,CNFClause> functions) {  

        return toString(0,functions);
    }

    /** ***************************************************************
    */
    public String asIntegerList(TreeMap<Integer,CNFClause> functions) {  

        return asIntegerList(0,functions,0);
    }

    /** ***************************************************************
    */
    public String asIntegerList(int indent, TreeMap<Integer,CNFClause> functions) {  

        return asIntegerList(indent,functions,0);
    }

    /** ***************************************************************
     * A test method.
     */
    public static void test1() {

        System.out.println("--------------------INFO in CNFClause.test1()--------------");
        Formula f2 = new Formula();
        f2.read("(m (SkFn 1 ?VAR1) ?VAR1)");
        CNFFormula cnf2 = new CNFFormula(f2);
        Formula f3 = new Formula();
        f3.read("(m ?VAR1 Org1-1)");
        CNFFormula cnf3 = new CNFFormula(f3);
        TreeMap tm = cnf3.firstClause().unify(cnf2.firstClause(),cnf3.functions,cnf2.functions);
        System.out.println("INFO in CNFClause.test1(): Map result: " + tm);
        if (tm != null) {
            cnf3 = cnf3.substitute(tm);
            System.out.println("INFO in CNFClause.test1(): Substitution result: " + 
                               cnf3);
        }
        System.out.println("INFO in CNFClause.test1(): cnf2: " + cnf2.deepCopy());
        System.out.println("INFO in CNFClause.test1(): cnf3: " + cnf3.deepCopy());

        CNFFormula target = new CNFFormula();
        target.read("(m (SkFn 1 Org1-1) Org1-1)");

        if (cnf3.equals(target)) 
            System.out.println("Successful test");
        else
           System.out.println("Result: \n " + cnf3 + "\n not equal to target result: \n" + target);          
    }

    /** ***************************************************************
     * A test method.
     */
    public static void test2() {

        System.out.println("--------------------INFO in CNFClause.test2()--------------");
        Formula f2 = new Formula();
        f2.read("(attribute (SkFn Jane) Investor)");
        CNFFormula cnf2 = new CNFFormula(f2);
        Formula f3 = new Formula();
        f3.read("(attribute ?X Investor)");
        CNFFormula cnf3 = new CNFFormula(f3);
        TreeMap tm = cnf3.firstClause().unify(cnf2.firstClause(),cnf3.functions,cnf2.functions);
        System.out.println("INFO in CNFClause.test2(): Map result: " + tm);
        if (tm != null) {
            cnf3 = cnf3.substitute(tm);
            System.out.println("INFO in CNFClause.test2(): Substitution result: " + 
                               cnf3);
        }
        System.out.println("INFO in CNFClause.test2(): cnf2: " + cnf2.deepCopy());
        System.out.println("INFO in CNFClause.test2(): cnf3: " + cnf3.deepCopy());

        CNFFormula target = new CNFFormula();
        target.read("(attribute (SkFn Jane) Investor)");

        if (cnf3.equals(target)) 
            System.out.println("Successful test");
        else
           System.out.println("Result: \n " + cnf3 + "\n not equal to target result: \n" + target);          
    }

    /** ***************************************************************
     * A test method.
     */
    public static void test3() {

        System.out.println("--------------------INFO in CNFClause.test3()--------------");
        Formula f2 = new Formula();
        f2.read("(not (agent ?VAR4 ?VAR1))");
        CNFFormula cnf2 = new CNFFormula(f2);
        Formula f3 = new Formula();
        f3.read("(agent ?VAR5 West)");
        CNFFormula cnf3 = new CNFFormula(f3);
        cnf3 = cnf2.unifyVariableScope(cnf3);
        System.out.println("INFO in CNFClause.test3(): cnf2: " + cnf2);
        System.out.println("INFO in CNFClause.test3(): cnf3: " + cnf3);
        TreeMap tm = cnf3.firstClause().unify(cnf2.firstClause(),cnf3.functions,cnf2.functions);
        System.out.println("INFO in CNFClause.test3(): Map result: " + tm);
        if (tm != null) {
            cnf3 = cnf3.substitute(tm);
            System.out.println("INFO in CNFClause.test3(): Substitution result: " + 
                               cnf3);
        }
        System.out.println("INFO in CNFClause.test3(): cnf2: " + cnf2);
        System.out.println("INFO in CNFClause.test3(): cnf3: " + cnf3);
    } 

    /** ***************************************************************
     * A test method.
     */
    public static void test4() {

        System.out.println("--------------------INFO in CNFClause.test4()--------------");
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();
        //cnf1.read("(equal (ListOrderFn (ListFn ?VAR1 ?VAR2) (ListLengthFn (ListFn ?VAR1 ?VAR2))) ?VAR2)");
        cnf1.read("(equal (ListOrderFn (ListFn ?VAR1 ?VAR2)) ?VAR2)");

        //System.out.println(cnf1.firstClause().asIntegerList(cnf1.functions));
        cnf2.read("(not (equal (ImmediateFamilyFn ?VAR1) Org1-1))");
        System.out.println("INFO in CNFClause.test4(): cnf1: " + cnf1);
        System.out.println("INFO in CNFClause.test4(): cnf2: " + cnf2);
        TreeMap tm = cnf1.firstClause().unify(cnf2.firstClause(),cnf1.functions,cnf2.functions);
        System.out.println("INFO in CNFClause.test4(): mapping: " + tm);
        if (tm != null) 
            System.out.println(cnf1.substitute(tm));
        System.out.println("INFO in CNFClause.test4(): cnf1: " + cnf1);
        System.out.println("INFO in CNFClause.test4(): cnf2: " + cnf2);
        System.out.println("INFO in CNFClause.test4(): should fail to unify ");
    } 

    /** ***************************************************************
     * A test method.
     */
    public static void test5() {

        System.out.println("--------------------INFO in CNFClause.test5()--------------");
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();
        //cnf1.read("(equal (ListOrderFn (ListFn ?VAR1 ?VAR2) (ListLengthFn (ListFn ?VAR1 ?VAR2))) ?VAR2)");
        cnf1.read("(equal ?VAR1 ?VAR2)");
        cnf2.read("(not (equal (ImmediateFamilyFn ?VAR1) Org1-1))");

        //System.out.println("INFO in CNFClause.test5(): cnf2: num functions: " + cnf2.functions.keySet().size());
        System.out.println("INFO in CNFClause.test5(): cnf1: " + cnf1);
        System.out.println("INFO in CNFClause.test5(): cnf2: " + cnf2);
        //System.out.println("INFO in CNFClause.test5(): cnf1 functions: " + 
        //                   CNFFormula.printFunctions(cnf1.functions));
        //System.out.println("INFO in CNFClause.test5(): cnf2 functions: " + 
        //                   CNFFormula.printFunctions(cnf2.functions));
        TreeMap tm = cnf1.firstClause().unify(cnf2.firstClause(),cnf1.functions,cnf2.functions);
        System.out.println("INFO in CNFClause.test5(): after unify cnf1 functions: " + 
                           CNFFormula.printFunctions(cnf1.functions));
        System.out.println("INFO in CNFClause.test5(): after unify cnf2 functions: " + 
                           CNFFormula.printFunctions(cnf2.functions));
        System.out.println(tm);
        CNFFormula result = null;
        if (tm != null) {
            result = cnf1.substitute(tm);
            System.out.println();
        }
        System.out.println("INFO in CNFClause.test5(): cnf1: " + cnf1);
        System.out.println("INFO in CNFClause.test5(): cnf2: " + cnf2);

        CNFFormula target = new CNFFormula();
        target.read("(equal (ImmediateFamilyFn ?VAR1) Org1-1)");

        if (result.equals(target)) 
            System.out.println("Successful test");
        else
           System.out.println("Result: \n " + result + "\n not equal to target result: \n" + target);                  
    } 

    /** ***************************************************************
     * A test method.
     */
    public static void test6() {

        System.out.println("--------------------INFO in CNFClause.test5()--------------");
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();
        //cnf1.read("(equal (ListOrderFn (ListFn ?VAR1 ?VAR2) (ListLengthFn (ListFn ?VAR1 ?VAR2))) ?VAR2)");
        cnf1.read("(equal (ListOrderFn (SkFn 158 ?VAR11 ?VAR10 ?VAR5 ?VAR9) (SuccessorFn ?VAR10)) (SkFn 159 ?VAR11 ?VAR10 ?VAR5 ?VAR9))");
        cnf2.read("(equal (ListOrderFn (SkFn 158 ?VAR8 ?VAR7 Org1-1 ?VAR6) (SuccessorFn ?VAR7)) (SkFn 159 ?VAR8 ?VAR7 Org1-1 ?VAR6))");

        //System.out.println("INFO in CNFClause.test6(): cnf2: num functions: " + cnf2.functions.keySet().size());
        System.out.println("INFO in CNFClause.test6(): cnf1: " + cnf1);
        System.out.println("INFO in CNFClause.test6(): cnf2: " + cnf2);
        //System.out.println("INFO in CNFClause.test6(): cnf1 functions: " + 
        //                   CNFFormula.printFunctions(cnf1.functions));
        //System.out.println("INFO in CNFClause.test6(): cnf2 functions: " + 
        //                   CNFFormula.printFunctions(cnf2.functions));
        TreeMap tm = cnf1.firstClause().unify(cnf2.firstClause(),cnf1.functions,cnf2.functions);
        System.out.println("INFO in CNFClause.test6(): after unify cnf1 functions: " + 
                           CNFFormula.printFunctions(cnf1.functions));
        System.out.println("INFO in CNFClause.test6(): after unify cnf2 functions: " + 
                           CNFFormula.printFunctions(cnf2.functions));
        System.out.println(tm);
        CNFFormula result = null;
        if (tm != null) {
            result = cnf1.substitute(tm);
            System.out.println();
        }
        System.out.println("INFO in CNFClause.test6(): cnf1: " + cnf1);
        System.out.println("INFO in CNFClause.test6(): cnf2: " + cnf2);

        CNFFormula target = new CNFFormula();
        target.read("(equal (ListOrderFn (SkFn 158 ?VAR8 ?VAR7 Org1-1 ?VAR6) (SuccessorFn ?VAR7)) (SkFn 159 ?VAR8 ?VAR7 Org1-1 ?VAR6))");

        if (result.equals(target)) 
            System.out.println("Successful test");
        else
           System.out.println("Result: \n " + result + "\n not equal to target result: \n" + target);                  
    } 

    /** ***************************************************************
     * A test method.
     */
    public static void occursTest() {

        System.out.println("--------------------INFO in CNFClause.occursTest()--------------");
        CNFFormula cnf1 = new CNFFormula();
        CNFFormula cnf2 = new CNFFormula();
        cnf1.read("(instance (SubtractionFn 0 ?VAR4) NonnegativeRealNumber)");
        cnf2.read("(instance ?VAR4 NonnegativeRealNumber)");

        //System.out.println("INFO in CNFClause.test6(): cnf2: num functions: " + cnf2.functions.keySet().size());
        System.out.println("INFO in CNFClause.occursTest(): cnf1: " + cnf1);
        System.out.println("INFO in CNFClause.occursTest(): cnf2: " + cnf2);
        //System.out.println("INFO in CNFClause.test6(): cnf1 functions: " + 
        //                   CNFFormula.printFunctions(cnf1.functions));
        //System.out.println("INFO in CNFClause.test6(): cnf2 functions: " + 
        //                   CNFFormula.printFunctions(cnf2.functions));
        TreeMap tm = cnf1.firstClause().unify(cnf2.firstClause(),cnf1.functions,cnf2.functions);
        System.out.println(tm);
    } 

    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {

        //test1();
        //test2();
        //test3();
        //test4();
        //test5();
        test6();
        //occursTest();
    }

}

