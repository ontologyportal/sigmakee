
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
 *  Each clause is part of a conjunctive normal form formula.
 *  It may be negated or not.  Each element in the clause is
 *  either a variable, a numbered constant, or an index to a
 *  functional term, which is itself an array of constants and
 *  variables.  A function may itself contain a function.
 */
public class CNFClause implements Comparable {

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
    public ArrayList<CNFClause> functions = new ArrayList();  // a function pointer of 101 points to the first element

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
     * Copy
     */
    public CNFClause deepCopy() {

        return deepCopy(this);
    }

    /** ***************************************************************
     */
    public int size() {

        int result = 0;
        result = result + numArgs;
        for (int i = 0; i < functions.size(); i++) {
            CNFClause f = (CNFClause) functions.get(i);
            result = result + f.size();
        }
        return result;
    }

    /** ***************************************************************
     * Copy
     */
    public CNFClause deepCopy(CNFClause f) {

        CNFClause result = new CNFClause();

        Iterator it = functions.iterator();
        while (it.hasNext()) {
            CNFClause c = (CNFClause) it.next();
            result.functions.add(c.deepCopy());
        }
        result.numArgs = f.numArgs;
        result.negated = f.negated;
        for (int i = 0; i < f.numArgs; i++) 
            result.args[i] = f.args[i];        
        return result;
    }

    /** ***************************************************************
     */
    public boolean isGround() {

        for (int i = 0; i < numArgs; i++) {
            if (isVariable(args[i])) 
                return false;
        }
        for (int i = 0; i < functions.size(); i++) {
            CNFClause c = (CNFClause) functions.get(i);
            if (!c.isGround()) 
                return false;            
        }
        return true;
    }

    /** ***************************************************************
     */
    public boolean isEmpty() {

        return numArgs == 0;
    }

    /** ***************************************************************
     *  Compares this object with the specified object for order.
     *  @return a negative integer, zero, or a positive integer as
     *  this object is less than, equal to, or greater than the
     *  specified object.
    */
    public int compareTo(Object cc) throws ClassCastException {

        if (!cc.getClass().getName().equalsIgnoreCase("com.articulate.sigma.CNFClause")) 
            throw new ClassCastException("Error in CNFClause.compareTo(): "
                                         + "Class cast exception for argument of class: " 
                                         + cc.getClass().getName());
        if (negated && !((CNFClause) cc).negated) 
            return 1;
        if (!negated && ((CNFClause) cc).negated) 
            return -1;
        if (args.equals(((CNFClause) cc).args))
            return compareTo(functions,((CNFClause) cc).functions);       
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
    public CNFClause() {
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
     * Get all the indexes of terms in the formula
     */
    public ArrayList<Integer> collectTerms() {

        ArrayList<Integer> result = new ArrayList();
        for (int i = 0; i < numArgs; i++) {
            if (isConstant(args[i])) 
                result.add(new Integer(args[i]));
        }
        Iterator it2 = functions.iterator();
        while (it2.hasNext()) {
            CNFClause c = (CNFClause) it2.next();  
            result.addAll(c.collectTerms());
        }
        return result;
    }

    /** ***************************************************************
     * Renumber variables according to the supplied Map
     */
    public CNFClause renumberVariables(TreeMap<Integer,Integer> varMap) {

        CNFClause result = new CNFClause();
        result.numArgs = numArgs;
        result.negated = negated;
        //System.out.println("INFO in CNFClause.renumberVariables(): Attempting to renumber : " + this + " with " + varMap);
        for (int i = 0; i < numArgs; i++) {
            if (args[i] >= CNFClause.VARSTARTINDEX && args[i] <= CNFClause.VARENDINDEX && varMap.containsKey(new Integer(args[i]))) {
                //System.out.println("Renumbering " + args[i] + " as " + ((Integer) varMap.get(new Integer(args[i]))).intValue());
                result.args[i] = ((Integer) varMap.get(new Integer(args[i]))).intValue();
            }
            else {
                result.args[i] = args[i];
            }
        }
        Iterator it2 = functions.iterator();
        while (it2.hasNext()) {
            CNFClause c = (CNFClause) it2.next();  
            result.functions.add(c.renumberVariables(varMap));
        }
        //System.out.println("INFO in CNFClause.renumberVariables(): result : " + this);
        return result;
    }

    /** ***************************************************************
     * Get all the indexes of variables in the formula, without
     * duplication.
     */
    public ArrayList<Integer> collectVariables() {

        ArrayList<Integer> result = new ArrayList();
        for (int i = 0; i < numArgs; i++) {
            if (args[i] >= CNFClause.VARSTARTINDEX && args[i] <= CNFClause.VARENDINDEX && !result.contains(new Integer(args[i]))) 
                result.add(new Integer(args[i]));
        }
        Iterator it2 = functions.iterator();
        while (it2.hasNext()) {
            CNFClause c = (CNFClause) it2.next();  
            result.addAll(c.collectVariables());
        }
        return result;
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
                CNFClause c = functions.get(args[i] - FUNCSTARTINDEX);
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

    /** ***************************************************************
     *  @return a boolean indicating whether the clause contains the
     *          given term.  Checking functional terms or variables
     *          returns an error.
     */
    public boolean containsTerm(Integer t) {

        if (t.intValue() < CONSTARTINDEX || t.intValue() > CONENDINDEX) {
            System.out.println("Error in CNFClause.containsTerm(): index " + t + " out of range for " + this);
            return false;
        }
        int i = 0;
        while (i <= numArgs && args[i] != t.intValue())
            i++;        
        if (i > numArgs) 
            return false;
        else
            return true;
    }

    /** **************************************************************
     *  Convenience routine that creates constants to fill variables
     *  and returns a formula.
     */
    public CNFClause instantiateVariables() {

        ArrayList<Integer> varList = collectVariables();
        TreeMap<Integer,Integer> vars = new TreeMap<Integer,Integer>();
        for (int i = 0; i < varList.size(); i++) {
            Integer varNum = (Integer) varList.get(i);
            STP2._GENSYM_COUNTER++;
            String value = "GenSym" + String.valueOf(STP2._GENSYM_COUNTER);
            int termNum = CNFClause.encodeTerm(value);
            vars.put(varNum,new Integer(termNum));
        }
        return instantiateVariables(vars,null);
    }


    /** ***************************************************************
     *  Replace existing values with a value as given by the map
     *  argument
     */
    public CNFClause replaceValues(Map<Integer,Integer> m) {

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
        Iterator it2 = functions.iterator();
        while (it2.hasNext()) {
            CNFClause c2 = (CNFClause) it2.next();  
            c.functions.add(c2.replaceValues(m));
        }
        //System.out.println("INFO in CNFClause.replaceValues(): result : " + c);
        return c;
    }
    /** ***************************************************************
     *  Replace variables with a value as given by the map argument
     *  @param funcs is a clause that may contain functions that
     *               need to be substituted
     */
    public CNFClause instantiateVariables(Map<Integer,Integer> m, CNFClause funcs) {

        //System.out.println("INFO in CNFClause.instantiateVariables(): Attempting to instantiate : " + this + " with " + m);
        CNFClause c = new CNFClause();
        c.numArgs = numArgs;
        c.negated = negated;
        for (int i = 0; i < numArgs; i++) {
            if ((args[i] >= CNFClause.VARSTARTINDEX && args[i] <= CNFClause.VARENDINDEX) &&
                (m.keySet().contains(new Integer(args[i])))) {
                Integer substitution = (Integer) m.get(new Integer(args[i]));
                if (funcs != null && isFunction(substitution.intValue())) {
                    c.functions.addAll(funcs.functions);
                }
                c.args[i] = substitution.intValue();
            }
            else
                c.args[i] = args[i];
        }
        Iterator it2 = functions.iterator();
        while (it2.hasNext()) {
            CNFClause c2 = (CNFClause) it2.next();  
            c.functions.add(c2.instantiateVariables(m,null));
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
     *  @param f1 is the variable
     *  @see unify()
     */
    private TreeMap<Integer, Integer> unifyVar(int f1, int f2, CNFClause c1, CNFClause c2, TreeMap<Integer, Integer> m) {

        //System.out.println("INFO in CNFClause.unifyVar(): Attempting to unify : " + c1 + 
        //                   " with " + c2);
        //System.out.println("INFO in CNFClause.unifyVar(): and terms : " + f1 + 
        //                   " with " + f2 + "(" + CNFFormula.intToTermMap.get(f2) + ")");
        if (!isVariable(f1)) {
            System.out.println("Error in CNFClause.unifyVar(): Attempting to unify : " + c1 + 
                               " with " + c2);
            System.out.println(": and terms : " + f1 + " with " + f2);
            System.out.println(f1 + " is not a variable");
        }
        if (m.keySet().contains(new Integer(f1))) 
            return unifyInternal(((Integer) m.get(f1)).intValue(),f2,c1,c2,m);
        else if (m.keySet().contains(new Integer(f2)))         
            return unifyInternal(((Integer) m.get(f2)).intValue(),f1,c2,c1,m);
        else if (isConstant(f1) && c2.containsTerm(new Integer(f1))) 
            return null;
        else {
            if (isFunction(f2)) {
                //System.out.println("INFO in CNFClause.unifyVar(): is function : " + f2);
                m.put(f1,f2);
                //System.out.println("INFO in CNFClause.unifyVar(): map : " + m);
                return m;
            }
            else {
                m.put(f1,f2);
                return m;
            }
        }
    }

    /** ***************************************************************
     *  @see unify()
     */
    private TreeMap<Integer, Integer> unifyInternal(int i1, int i2, CNFClause c1, CNFClause c2, TreeMap<Integer, Integer> m) {

        //System.out.println("INFO in CNFClause.unifyInternal(): term map : " + CNFFormula.intToTermMap);
        //System.out.println("INFO in CNFClause.unifyInternal(): Attempting to unify : " + i1 + "(" + CNFFormula.intToTermMap.get(i1) + ")" +
        //                   " with " + i2  + "(" + CNFFormula.intToTermMap.get(i2) + ")" + " in " + c1 + " and " + c2);
        //for (int i = 0; i < 1000000; i++) {
        //}
        if (m == null) 
            return null;
        else if (isConstant(i1) && i1 == i2) 
            return m;
        else if (isVariable(i1)) 
            return unifyVar(i1,i2,c1,c2,m);
        else if (isVariable(i2)) 
            return unifyVar(i2,i1,c2,c1,m);
        else if (isFunction(i1) && isFunction(i2)) {
            CNFClause form1 = new CNFClause();
            form1.deepCopy(c1);
            CNFClause form2 = new CNFClause();
            form2.deepCopy(c2);
            return form1.unifyClausesInternal(form2,m);
        }
        else {
            //System.out.println("INFO in CNFClause.unifyInternal(): failed to unify");
            return null;
        }
    }

    /** ***************************************************************
     *  @see unify()
     */
    public TreeMap<Integer, Integer> unifyClausesInternal(CNFClause c, TreeMap<Integer, Integer> m) {

        if (m == null) 
            return null;
        //System.out.println("INFO in CNFClause.unifyClausesInternal(): Attempting to unify : " + this + 
        //                   " with " + c);
        //System.out.println("this func size: " + this.functions.size());
        //System.out.println("c func size: " + c.functions.size());
        TreeMap<Integer, Integer> result = new TreeMap();
        int thisPointer = 0;
        while (result != null && thisPointer < numArgs) {
            result = unifyInternal(args[thisPointer],c.args[thisPointer],this,c,m);
            if (result == null) 
                return null;
            thisPointer++;
        }
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
    public ArrayList<CNFClause> unifyScope(CNFClause c1, CNFClause c2) {

        //System.out.println("INFO in CNFClause.unifyScope(): before : " + c1 + " and " + c2); 
        //System.out.println("c1 func size: " + c1.functions.size());
        //System.out.println("c2 func size: " + c2.functions.size());
        ArrayList<CNFClause> result = new ArrayList();        
        ArrayList<Integer> varList1 = c1.collectVariables();
        ArrayList<Integer> varList2 = c2.collectVariables();
        TreeMap<Integer, Integer> varMap = new TreeMap();
        for (int i = 0; i < varList2.size(); i++) {                 // renumber variables
            Integer varNew = (Integer) varList2.get(i);
            while (varList1.contains(varNew)) 
                varNew = new Integer(varNew.intValue() + 1);            
            varMap.put((Integer) varList2.get(i),varNew);
        }
        CNFClause c1new = c1.deepCopy();
        CNFClause c2new = c2.deepCopy();
        c2new = c2new.instantiateVariables(varMap,null);
        //System.out.println("INFO in CNFClause.unifyScope(): middle : " + c1new + " and " + c2new); 
        //System.out.println("INFO in CNFClause.unifyScope(): middle : " + c1new.asIntegerList() + " and " + c2new.asIntegerList()); 
        if (c1new.functions.size() > 0 || c2new.functions.size() > 0) {     // renumber functions
            ArrayList<CNFClause> newFuncs = new ArrayList();
            int c1FuncSize = c1new.functions.size();
            int c2FuncSize = c2new.functions.size();
            newFuncs.addAll(c1new.functions);
            newFuncs.addAll(c2new.functions);
            if (c2FuncSize > 0)
                c1new.functions.addAll(c2new.functions);            
            if (c1FuncSize > 0) {
                //System.out.println("INFO in CNFClause.unifyScope(): renumbering functions"); 
                c2new.functions = newFuncs;
                TreeMap<Integer, Integer> funcMap = new TreeMap();
                for (int i = 0; i < c2FuncSize; i++) 
                    funcMap.put(new Integer(101+i),new Integer(101+i+c1FuncSize-1));
                c2new = c2new.replaceValues(funcMap);
            }
        }
        result.add(c1new);
        result.add(c2new);
        //System.out.println("INFO in CNFClause.unifyScope(): after : " + c1new + " and " + c2new); 
        //System.out.println("INFO in CNFClause.unifyScope(): after : " + c1new.asIntegerList() + " and " + c2new.asIntegerList()); 
        //System.out.println("c1new func size: " + c1new.functions.size());
        //System.out.println("c2new func size: " + c2new.functions.size());
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
     *  Note that this method has a side effect on "this", which is
     *  to add functions from the argument.
     */
    public TreeMap<Integer, Integer> unify(CNFClause c) {

        ArrayList<CNFClause> clausePair = unifyScope(this,c);
        CNFClause thisClause = (CNFClause) clausePair.get(0);
        CNFClause c2 = (CNFClause) clausePair.get(1);
        //System.out.println("INFO in CNFClause.unify(): Attempting to unify : " + thisClause + 
        //                  " with " + c2);
        //System.out.println("thisClause func size: " + thisClause.functions.size());
        //System.out.println("c2 func size: " + c2.functions.size());
        TreeMap<Integer, Integer> result = new TreeMap();
        this.functions = thisClause.functions;
        return thisClause.unifyClausesInternal(c2,result);
    }

    /** ***************************************************************
     */
    public static int encodeTerm(String term) {

        int termNum = CNFFormula.termMap.size() + CONSTARTINDEX;
        CNFFormula.termMap.put(term,new Integer(termNum));
        CNFFormula.intToTermMap.put(new Integer(termNum),term);
        return termNum;
    }

    /** ***************************************************************
    */
    private static CNFClause createClause(Formula f, TreeMap<String, Integer> varMap) {
        
        //System.out.println("INFO in CNFClause.createClause(): formula: " + f);
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
            //System.out.println("INFO in CNFClause.createClause(): arg: [" + arg + "]");
            fnew.read(fnew.cdr());

            if (newClause.numArgs > 11) {
                System.out.println("Error in CNFClause.createClause(): arg out of range converting formula: " + f);
                return null;
            }
            if (Formula.atom(arg) && !Formula.isVariable(arg)) {
                //System.out.println("INFO in CNFClause.createClause(): atom");
                int termNum = 0;
                if (CNFFormula.termMap.keySet().contains(arg)) 
                    termNum = CNFFormula.termMap.get(arg);
                else
                    termNum = CNFClause.encodeTerm(arg);     
                newClause.args[newClause.numArgs++] = termNum;
            }
            if (Formula.isVariable(arg)) {
                //System.out.println("INFO in CNFClause.createClause(): variable");
                if (varMap.keySet().contains(arg)) 
                    newClause.args[newClause.numArgs++] = ((Integer) varMap.get(arg)).intValue();
                else {
                    int varNum = varMap.keySet().size() + 1;
                    varMap.put(arg,new Integer(varNum));
                    newClause.args[newClause.numArgs++] = varNum;
                }
            }
            if (Formula.isFunctionalTerm(arg)) {
                //System.out.println("INFO in CNFClause.createClause(): function");
                int funcNum = newClause.functions.size() + FUNCSTARTINDEX;
                Formula func = new Formula();
                func.read(arg);
                CNFClause cnew = createClause(func,varMap);
                newClause.functions.add(cnew);
                newClause.args[newClause.numArgs++] = funcNum;
            }
        }
        //System.out.println("INFO in CNFClause.createClause(): ");
        //System.out.print("Arguments: ");
        //newClause.printArgs();
        //System.out.println("Negated: " + newClause.negated);
        return newClause;
    }

    /** ***************************************************************
     *  Create a CNFClause instance from a Formula that is a single
     *  clause.
     *  @param varMap is a temporary map, with the scope of just one
     *                Formula, that describes how to map variable
     *                names to integers from 1 to 99
    */
    public CNFClause(Formula f, TreeMap<String, Integer> varMap) {

        CNFClause newClause = CNFClause.createClause(f,varMap);
        args = newClause.args;
        numArgs = newClause.numArgs;
        functions = newClause.functions;
        negated = newClause.negated;
    }

    /** ***************************************************************
     *  @param indent is the number of spaces to indent.  The sign
     *                of the integer is reversed if this is a
     *                function which shouldn't have a terminating
     *                carriage return.
    */
    public String toStringFormat(boolean format, int indent) {  

        //System.out.println("INFO in CNFClause.toString()");
        //System.out.println("INFO in CNFClause.toStringFormat(): format: " + format);
        //System.out.println("Functions: " + functions);
        //System.out.println("Term map: " + CNFFormula.intToTermMap);
        //System.out.println("Num args: " + numArgs);
        //System.out.println("negated: " + negated);
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
                if (arg - FUNCSTARTINDEX < functions.size()) 
                    result.append(((CNFClause) functions.get(arg - FUNCSTARTINDEX)).toString(newIndent));                    
                else {
                    System.out.println("Error in CNFClause.toString(): bad function index : " + arg);
                    System.out.println("Number of functions : " + functions.size());
                    return this.asIntegerList();
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
    */
    public String toString(int indent) {  
        return toStringFormat(true,indent);
    }

    /** ***************************************************************
     *  @param indent is the number of spaces to indent.  The sign
     *                of the integer is reversed if this is a
     *                function which shouldn't have a terminating
     *                carriage return.
    */
    public String asIntegerList(int indent) {  

        //System.out.println("INFO in CNFClause.toString()");
        //System.out.println("Functions: " + functions);
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
                if (arg - FUNCSTARTINDEX < functions.size()) 
                    result.append(arg + "->" + ((CNFClause) functions.get(arg - FUNCSTARTINDEX)).asIntegerList(newIndent));                    
                else {
                    System.out.println("Error in CNFClause.toString(): bad function index : " + arg);
                    System.out.println("Number of functions : " + functions.size());
                    return null;
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
        return result.toString();
    }

    /** ***************************************************************
    */
    public String toString() {  // number of spaces to indent

        return toString(0);
    }

    /** ***************************************************************
    */
    public String asIntegerList() {  // number of spaces to indent

        return asIntegerList(0);
    }


    /** ***************************************************************
     * A test method.
     */
    public static void test1() {

        Formula f2 = new Formula();
        f2.read("(m (SkFn 1 ?VAR1) ?VAR1)");
        CNFFormula cnf2 = new CNFFormula(f2);
        Formula f3 = new Formula();
        f3.read("(m ?VAR1 Org1-1)");
        CNFFormula cnf3 = new CNFFormula(f3);
        TreeMap tm = cnf3.firstClause().unify(cnf2.firstClause());
        System.out.println("INFO in CNFClause.main(): Map result: " + tm);
        if (tm != null) 
            System.out.println("INFO in CNFClause.main(): Substitution result: " + cnf3.substitute(tm,null));
        System.out.println("INFO in CNFClause.main(): cnf2: " + cnf2.deepCopy());
        System.out.println("INFO in CNFClause.main(): cnf3: " + cnf3.deepCopy());
    }

    /** ***************************************************************
     * A test method.
     */
    public static void test2() {

        Formula f2 = new Formula();
        f2.read("(attribute (SkFn Jane) Investor)");
        CNFFormula cnf2 = new CNFFormula(f2);
        Formula f3 = new Formula();
        f3.read("(attribute ?X Investor)");
        CNFFormula cnf3 = new CNFFormula(f3);
        TreeMap tm = cnf3.firstClause().unify(cnf2.firstClause());
        System.out.println(tm);
        if (tm != null) 
            System.out.println(cnf3.substitute(tm,null));
        System.out.println("INFO in CNFClause.main(): cnf2: " + cnf2.deepCopy());
        System.out.println("INFO in CNFClause.main(): cnf3: " + cnf3.deepCopy());
    }

    /** ***************************************************************
     * A test method.
     */
    public static void test3() {

        Formula f2 = new Formula();
        f2.read("(not (agent ?VAR4 ?VAR1))");
        CNFFormula cnf2 = new CNFFormula(f2);
        Formula f3 = new Formula();
        f3.read("(agent ?VAR5 West)");
        CNFFormula cnf3 = new CNFFormula(f3);
        cnf3 = cnf2.unifyScope(cnf3);
        System.out.println("INFO in CNFClause.main(): cnf2: " + cnf2);
        System.out.println("INFO in CNFClause.main(): cnf3: " + cnf3);
        TreeMap tm = cnf3.firstClause().unify(cnf2.firstClause());
        System.out.println(tm);
        if (tm != null) 
            System.out.println(cnf3.substitute(tm,null));
        System.out.println("INFO in CNFClause.main(): cnf2: " + cnf2);
        System.out.println("INFO in CNFClause.main(): cnf3: " + cnf3);
    } 

    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {

        test3();
    }

}

