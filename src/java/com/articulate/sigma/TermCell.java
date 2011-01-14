/** This code is copyright Articulate Software (c) 20011.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/

package com.articulate.sigma;

import java.io.*;
import java.util.*;

/** ***************************************************************
 */
public class TermCell implements Comparable {

    /** Variables are negative, terms positive. Variables in
     * unprocessed formulas are odd.  Variables in processed
     * formulas are even. */
    public int term = 0;
    public TermCell[] argList = null;

    /** *************************************************************
     */
    public TermCell() {
    }

    /** ***************************************************************
    */
    public static void printArray(TermCell[] thisList) {

        if (thisList == null) 
            return;
        for (int i = 0; i < thisList.length; i++) 
            System.out.println(thisList[i]);        
    }

    /** ***************************************************************
     *  Compares this object with the specified object for order.
     * 
     *  @return a negative integer, zero, or a positive integer as
     *  this object is less than, equal to, or greater than the
     *  specified object.
    */
    public int compareTo(Object cc) throws ClassCastException {

        //System.out.println("INFO in TermCell.compareTo()");
        if (!cc.getClass().getName().equalsIgnoreCase("com.articulate.sigma.TermCell")) 
            throw new ClassCastException("Error in TermCell.compareTo(): "
                                         + "Class cast exception for argument of class: " 
                                         + cc.getClass().getName());
        TermCell cArg = (TermCell) cc;
        //System.out.println("INFO in TermCell.compareTo() Comparing \n" + this + "\n to \n" + cArg);
        if (term < cArg.term) 
            return -1;
        if (term > cArg.term) 
            return 1;
        if (term == cArg.term && cArg.argList == null) 
            return 0;
        if (cArg.argList == null) 
            return 1;
        if (argList.length <= cArg.argList.length) 
            return compareTo(argList,cArg.argList);
        else
            return -compareTo(cArg.argList,argList);                                 
    }

    /** ***************************************************************
     *  Compares this object with the specified object for order.
     * 
     *  @param thisList must be the smaller of the two arrays if
     *                  they are not of equal length
     *  @return a negative integer, zero, or a positive integer as
     *  this object is less than, equal to, or greater than the
     *  specified object.
    */
    public static int compareTo(TermCell[] thisList, TermCell[] argList) throws ClassCastException {

        //System.out.println("INFO in TermCell.compareTo([],[])");
        //System.out.println("INFO in TermCell.compareTo([],[]) Comparing \n");
        //printArray(thisList);
        //System.out.println("\n to \n");
        //printArray(argList);

        for (int i = 0; i < thisList.length; i++) {
            //if (!thisList[i].isVariable() || !argList[i].isVariable()) {  // if both variables then ignore
                if (thisList[i].isFunction() && argList[i].isFunction()) {
                    int result = 0;
                    if (thisList[i].argList.length <= argList[i].argList.length)
                        result = compareTo(thisList[i].argList,argList[i].argList);
                    else
                        result = - compareTo(argList[i].argList,thisList[i].argList);
                    if (result != 0) 
                        return result;
                }
                if (thisList[i].term < argList[i].term) 
                    return -1;
                if (thisList[i].term > argList[i].term) 
                    return 1;
            //}
        }
        if (thisList.length < argList.length)         
            return -1;                               
        else
            return 0;
    }

    /** *************************************************************
     */
    public TermCell deepCopy() {

        //System.out.println("INFO in TermCell.deepCopy() Copying \n" + this);
        TermCell newTC = new TermCell();
        newTC.term = term;
        if (argList != null) {
            newTC.argList = new TermCell[argList.length];
            for (int i = 0; i < argList.length; i++) 
                newTC.argList[i] = argList[i].deepCopy();        
        }
        return newTC;
    }

    /** *************************************************************
     */
    public boolean deepEquals(TermCell tc) {

        //System.out.println("INFO in TermCell.deepEquals() Comparing \n" + this + "\n to \n" + tc);
        if (isFunction() && tc.isFunction()) {
            for (int i = 0; i < argList.length; i++) 
                if (!tc.argList[i].deepEquals(argList[i]))        
                    return false;
            return true;
        }
        else 
            return term == tc.term;        
    }

    /** *************************************************************
     */
    public boolean equals(TermCell tc) {

        return deepEquals(tc);        
    }

    /** ***************************************************************
     *  Make variable numbers odd as a side effect.
    */
    public void makeVarsOdd() {  

        if (term == 0) 
            for (int i = 0; i < argList.length; i++) 
                argList[i].makeVarsOdd();                            
        else 
            if (term < 0 && (term % 2 == 0)) 
                term = term + 1;
    }

    /** ***************************************************************
     *  Make variable numbers even as a side effect.
    */
    public void makeVarsEven() {  

        if (term == 0)        
            for (int i = 0; i < argList.length; i++) 
                argList[i].makeVarsEven();  
        else
            if (term < 0 && (term % 2 == -1)) 
                term = term - 1;
    }

    /** ***************************************************************
     *  Note that this routine assumes that the invoking routine has
     *  created "this" as a copy, since it will be modified.
    */
    public TermCell substitute(TreeMap<TermCell, TermCell> m) {  

        //System.out.println("INFO in TermCell.substitute(): this:\n" + this);
        //System.out.println("INFO in TermCell.substitute(): m: " + m);
        for (int i = 0; i < argList.length; i++) {
            if (m.containsKey(argList[i])) {
                TermCell tc = m.get(argList[i]);
                if (tc.argList == null || tc.argList.length < 1 || (tc.argList[0].term != this.argList[0].term)) 
                    argList[i] = tc.deepCopy();
                else
                    System.out.println("INFO in TermCell.substitute(): Trapping possible recursive function application: " + this);
            }
            else 
                if (argList[i].isFunction()) 
                    argList[i].substitute(m);            
        }
        //System.out.println("INFO in TermCell.substitute(): result: " + this);
        return this;
    }

    /** ***************************************************************
     *  Note that this routine assumes that the invoking routine has
     *  created "this" as a copy, since it will be modified.
    */
    public TermCell substituteVar(TreeMap<TermCell, TermCell> m) {  

        for (int i = 0; i < argList.length; i++) {
            if (argList[i].isVariable() && m.containsKey(argList[i])) {
                TermCell tc = m.get(argList[i]);
                if (tc.argList == null || tc.argList.length < 1 || (tc.argList[0].term != this.argList[0].term)) 
                    argList[i] = tc.deepCopy();
                else
                    System.out.println("INFO in TermCell.substituteVar(): Trapping possible recursive function application: " + this);
            }
            else 
                if (argList[i].isFunction()) 
                    argList[i].substituteVar(m);            
        }
        return this;
    }

    /** ***************************************************************
     * Get all the Integer indexes of terms in the formula,
     * without duplication.
     */
    public TreeSet<Integer> collectTerms() {

        TreeSet<Integer> result = new TreeSet();
        for (int i = 0; i < argList.length; i++) {
            if (argList[i].isConstant()) 
                result.add(new Integer(argList[i].term));
            if (argList[i].isFunction())             
                result.addAll(argList[i].collectTerms());        
        }
        return result;
    }

    /** ***************************************************************
     * Get all the indexes of variables in the formula, without
     * duplication, but only if it is a function
     */
    public TreeSet<TermCell> collectVariables() {

        if (!isFunction()) 
            return null;
        TreeSet<TermCell> result = new TreeSet();
        for (int i = 0; i < argList.length; i++) {
            if (argList[i].isFunction()) 
                result.addAll(argList[i].collectVariables());
            if (argList[i].isVariable()) 
                result.add(argList[i]);
        }
        return result;
    }

    /** ***************************************************************
     */
    public static void setTermPointer(TreeMap<Integer, ArrayList<CNFFormula2>> termPointers,
                                TermCell t, CNFFormula2 f) {

        ArrayList<CNFFormula2> al = null;
        if (termPointers.get(new Integer(t.term)) == null) {
            al = new ArrayList();
            termPointers.put(new Integer(t.term),al);
        }
        else
            al = (ArrayList) termPointers.get(new Integer(t.term));
        al.add(f);
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
        for (int i = 0; i < argList.length; i++) {
            if (argList[i].isVariable()) 
                continue;
            else if (argList[i].isFunction()) {
                TermCell tc = argList[i];
                tc.setTermPointers(posTermPointers,negTermPointers,f,isNegated);
                continue;
            }
            else {
                if (isNegated) 
                    setTermPointer(negTermPointers,argList[i],f);
                else
                    setTermPointer(posTermPointers,argList[i],f);
            }
        }
    }

    /** ***************************************************************
     * Get all the indexes of variables in the formula, without
     * duplication, but only if it is a function
     */
    public boolean containsVar(TermCell v) {

        if (!v.isVariable()) {
            System.out.println("Error in TermCell.containsVar(): not a variable: " + v);
            return false;
        }
        if (this.isVariable() && v.term == this.term) 
            return true;
        if (argList != null) 
            for (int i = 0; i < argList.length; i++) 
                if (argList[i].containsVar(v)) 
                    return true;                    
        return false;
    }

    /** *************************************************************
     */
    public boolean isVariable() {
        return term < 0;
    }

    /** *************************************************************
     */
    public boolean isConstant() {
        return term > 0;
    }

    /** *************************************************************
     */
    public boolean isFunction() {
        return term == 0;
    }

    /** *************************************************************
     */
    public static boolean isVariable(int term) {
        return term < 0;
    }

    /** *************************************************************
     */
    public static boolean isConstant(int term) {
        return term > 0;
    }

    /** *************************************************************
     */
    public static boolean isFunction(int term) {
        return term == 0;
    }

    /** ***************************************************************
     */
    public static boolean isLogicalOperator(Integer t) {

        String s = (String) CNFFormula2.intToTermMap.get(t);
        if (s == null) {
            System.out.println("Error in TermCell.isLogicalOperator(): term index " + t + " not found.");
            return false;
        }
        return Formula.isLogicalOperator(s);
    }

    /** ***************************************************************
     *  @param indent is the number of spaces to indent.  The sign
     *                of the integer is reversed if this is a
     *                function which shouldn't have a terminating
     *                carriage return.
    */
    public String toStringFormat(int indent, boolean format) {  

        if (indent > 100) {
            System.out.println("Error in TermCell.toStringFormat(): Error: excessive indent ");
            return "";
        }
        StringBuffer result = new StringBuffer();
        if (term < 0)                                             // variable
            result.append("?VAR" + String.valueOf(-term));
        if (term > 0) {                                         // term
            if (CNFFormula2.intToTermMap != null && CNFFormula2.intToTermMap.containsKey(new Integer(term))) 
                result.append(CNFFormula2.intToTermMap.get(new Integer(term)));                
            else {
                System.out.println("Error in CNFClause2.toString(): bad term index : " + term);
                System.out.println(CNFFormula2.intToTermMap.keySet().size());
            }
        }    
        if ((term == 0 && argList != null)) {             // function
            int newIndent = indent + 2;
            if (format) result.append("\n");
            if (format) result.append(CNFClause2.spaces(newIndent));                
            result.append("(");
            for (int i = 0; i < argList.length; i++) {
                TermCell t = argList[i];
                result.append(t.toStringFormat(newIndent,format));
                if (i < argList.length - 1) 
                    result.append(" ");                        
            }
            result.append(")");
        }
        return result.toString();
    }

    /** ***************************************************************
    */
    public String toString() {  
        return toStringFormat(0,true);
    }

    /** ***************************************************************
    */
    public static String printTerms(TermCell[] argList) {

        TermCell tc = new TermCell();
        tc.term = 0;
        tc.argList = argList;        
        return tc.toString();
    }

}
