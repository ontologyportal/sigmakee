package com.articulate.sigma.semRewrite;

/*
Copyright 2014-2015 IPsoft

Author: Adam Pease adam.pease@ipsoft.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA 
*/

import java.text.ParseException;
import java.util.*;

import com.articulate.sigma.*;

/** *************************************************************
 * pred(arg1,arg2).  
 * arg1 and/or arg2 can be variables which are denoted by a 
 * leading '?'
 */
public class Clause {

    public boolean negated = false;
    public boolean preserve = false;
    public boolean bound = false; // bound clauses in a delete rule get deleted
    public String pred;
    public String arg1;
    public String arg2;
    
    /** ***************************************************************
     */
    public String toString() {
        
        StringBuffer sb = new StringBuffer();
        if (bound)
            sb.append("X");
        if (negated)
            sb.append("~");
        if (preserve)
            sb.append("+");
        sb.append(pred + "(" + arg1 + "," + arg2 + ")");
        return sb.toString();
    }
    
    /** ***************************************************************
     */
    public Clause deepCopy() {
        
        Clause newc = new Clause();
        newc.negated = negated;
        newc.preserve = preserve;
        newc.bound = bound;
        newc.pred = pred;
        newc.arg1 = arg1;
        newc.arg2 = arg2;
        return newc;
    }
    
    /** ***************************************************************
     */
    @Override
    public boolean equals(Object o) {
    
        if (!(o instanceof Clause))
            return false;
        Clause c = (Clause) o;
        if (negated != c.negated)
            return false;
        if (!pred.equals(c.pred))
            return false;
        if (!arg1.equals(c.arg1))
            return false;
        if (!arg2.equals(c.arg2))
            return false;
        return true;
    }
    
    /** ***************************************************************
     */
    public boolean isGround() {
        
        if (!arg1.startsWith("?") && !arg2.startsWith("?"))
            return true;
        else
            return false;
    }
    
    /** *************************************************************
     */
    public void preProcessQuestionWords(List<String> qwords) {
        
        for (String s: qwords) {
            System.out.println("INFO in Clause.preProcessQuestionWords(): " + s + " " + arg1 + " " + arg2);
            if (arg1.toLowerCase().matches(s.toLowerCase() + "-\\d+"))
                arg1 = "?" + arg1;
            if (arg2.toLowerCase().matches(s.toLowerCase() + "-\\d+"))
                arg2 = "?" + arg2;
        }
    }
    
    /** ***************************************************************
     * Apply variable substitutions to a clause  
     */
    public void applyBindingSelf(HashMap<String,String> bindings) {
        
        if (arg1.startsWith("?")) {
            if (bindings.containsKey(arg1))
                arg1 = bindings.get(arg1);
        }
        if (arg2.startsWith("?")) {
            if (bindings.containsKey(arg2))
                arg2 = bindings.get(arg2);
        }
    }
    
    /** ***************************************************************
     * Apply variable substitutions to a clause  
     */
    public Clause applyBindings(HashMap<String,String> bindings) {
        
        //System.out.println("INFO in Clause.applyBindings(): this: " + this);
        //System.out.println("INFO in Clause.applyBindings(): bindings: " + bindings);
        Clause c = new Clause();
        c.pred = pred;
        if (arg1.startsWith("?")) {
            if (bindings.containsKey(arg1))
                c.arg1 = bindings.get(arg1);
            else
                c.arg1 = arg1;
        }
        else
            c.arg1 = arg1;
        if (arg2.startsWith("?")) {
            if (bindings.containsKey(arg2))
                c.arg2 = bindings.get(arg2);
            else
                c.arg2 = arg2;
        }
        else
            c.arg2 = arg2;
        c.negated = negated;
        c.preserve = preserve;
        //System.out.println("INFO in Clause.applyBindings(): returning this: " + c);
        return c;
    }
    
    /** ***************************************************************
     * A degenerate case of an occurs check since we have no functions
     * and argument lists are always of length 2.
     */
    private static boolean occursCheck(String t, Clause c) {
        
        if (t.equals(c.arg1) || t.equals(c.arg2))
            return true;
        else
            return false;
    }
    
    /** ***************************************************************
     * @return false if there are wildcards and they don't match (or there's an error)
     * and true if there are no wildcards.  Only
     */
    private static boolean wildcardMatch(String t1, String t2) {
        
        String s1 = t1;
        String s2 = t2;
        if (!t1.contains("*") && !t2.contains("*")) // no wildcards case should fall through
            return true;
        if (t1.contains("*") && t2.contains("*")) {
            System.out.println("Error in Clause.wildcardMatch(): both arguments have wildcards: " + t1 + " " + t2);
            return false;
        }
        if (t2.contains("*")) {
            s1 = t2;
            s2 = t1;
        }
        if (s1.indexOf('*') > -1 && s2.indexOf('-') > -1) {  // when wildcard, both have to be matching variables
                                                             // except for suffix
            if (!s1.substring(0,s1.lastIndexOf('*')).equals(s2.substring(0,s2.lastIndexOf('-'))))
                return false;
        }
        return true;
    }
        
    /** ***************************************************************
     * Unify all terms in term1 with the corresponding terms in term2 with a
     * common substitution. Note that unlike general unification, we have
     * a fixed argument list of 2.   
     */
    public HashMap<String,String> mguTermList(Clause l2) {

        //System.out.println("INFO in Clause.mguTermList(): attempting to unify " + this + " and " + l2);
        HashMap<String,String> subst = new HashMap<String,String>();
        
        if (!pred.equals(l2.pred)) 
            return null;        
        for (int arg = 1; arg < 3; arg++) {           
            String t1 = arg1; // Pop the first term pair to unify off the lists            
            String t2 = l2.arg1; // (removes and returns the denoted elements).
            if (arg == 2) {
                t1 = arg2;            
                t2 = l2.arg2;
            }
            //System.out.println("INFO in Clause.mguTermList(): attempting to unify arguments " + t1 + " and " + t2); 
            if (t1.startsWith("?")) {
                //System.out.println("INFO in Clause.mguTermList(): here 1");
                if (t1.equals(t2))
                    // We could always test this upfront, but that would
                    // require an expensive check every time. 
                    // We descend recursively anyway, so we only check this on
                    // the terminal case.  
                    continue;
                if (occursCheck(t1,l2))
                    return null;
                // We now create a new substitution that binds t2 to t1, and
                // apply it to the remaining unification problem. We know
                // that every variable will only ever be bound once, because
                // we eliminate all occurrences of it in this step - remember
                // that by the failed occurs-check, t2 cannot contain t1.
                HashMap<String,String> newBinding = new HashMap<String,String>();
                if (!wildcardMatch(t1,t2)) 
                    return null;
                newBinding.put(t1,t2);                
                applyBindingSelf(newBinding);
                l2 = l2.applyBindings(newBinding);
                subst.put(t1, t2);
            }
            else if (t2.startsWith("?")) {
                //System.out.println("INFO in Clause.mguTermList(): here 2");
                // Symmetric case - We know that t1!=t2, so we can drop this check
                if (occursCheck(t2, this))
                    return null;
                HashMap<String,String> newBinding = new HashMap<String,String>();
                if (!wildcardMatch(t1,t2)) 
                    return null;
                newBinding.put(t2, t1);          
                applyBindingSelf(newBinding);
                l2 = l2.applyBindings(newBinding);
                subst.put(t2, t1);
            }
            else {
                //System.out.println("INFO in Clause.mguTermList(): t1 " + t1 + " t2 " + t2);
                if (!t1.equals(t2)) {
                    if (t1.indexOf('*') > -1 && t2.indexOf('-') > -1) {
                        if (!t1.substring(0,t1.lastIndexOf('*')).equals(t2.substring(0,t2.lastIndexOf('-'))))
                            return null;
                    }
                    else if (t2.indexOf('*') > -1 && t1.indexOf('-') > -1) {
                        if (!t2.substring(0,t2.lastIndexOf('*')).equals(t1.substring(0,t1.lastIndexOf('-'))))
                            return null;
                    }
                    else
                        return null;
                }
            }
        }
        //System.out.println("INFO in Clause.mguTermList(): subst on exit: " + subst);
        return subst;
    }
    
    /** ***************************************************************
     * The predicate must already have been read
     */
    public static Clause parse(Lexer lex, int startLine) {

        String errStart = "Parsing error in " + RuleSet.filename;
        String errStr;
        Clause cl = new Clause();
        try {
            //System.out.println("INFO in Clause.parse(): " + lex.look());
            if (lex.testTok(Lexer.Plus)) {
                cl.preserve = true;
                lex.next();
            }
            //System.out.println("INFO in Clause.parse(): " + lex.look());
            cl.pred = lex.next();
            //System.out.println("INFO in Clause.parse(): " + lex.look());
            if (!lex.testTok(Lexer.OpenPar)) {
                errStr = (errStart + ": Invalid token '" + lex.look() + "' near line " + startLine);
                throw new ParseException(errStr, startLine);
            }
            lex.next();
            //System.out.println("INFO in Clause.parse(): " + lex.look());
            cl.arg1 = lex.next();
            //System.out.println("INFO in Clause.parse(): " + lex.look());
            if (!lex.testTok(Lexer.Comma)) {
                errStr = (errStart + ": Invalid token '" + lex.look() + "' near line " + startLine);
                throw new ParseException(errStr, startLine);
            }
            lex.next();
            //System.out.println("INFO in Clause.parse(): " + lex.look());
            cl.arg2 = lex.next();
            //System.out.println("INFO in Clause.parse(): " + lex.look());
            if (!lex.testTok(Lexer.ClosePar)) {
                errStr = (errStart + ": Invalid token '" + lex.look() + "' near line " + startLine);
                throw new ParseException(errStr, startLine);
            } 
            lex.next();
        }
        catch (Exception ex) {
            String message = ex.getMessage();
            System.out.println("Error in Clause.parse() " + message);
            ex.printStackTrace();
        }	
        //System.out.println("INFO in Clause.parse(): returning " + cl);
        return cl;
    }
    
    /** *************************************************************
     * A test method
     */
    public static void testUnify() {
        
        String s1 = "sumo(Human,Mary-1)";
        String s2 = "sumo(?O,Mary-1)";
        Clause c1 = null;
        Clause c2 = null;
        try {
            Lexer lex = new Lexer(s1);
            lex.look();
            c1 = Clause.parse(lex, 0);
            lex.look();
            lex = new Lexer(s2);
            c2 = Clause.parse(lex, 0);
        }
        catch (Exception ex) {
            String message = ex.getMessage();
            System.out.println("Error in Clause.parse() " + message);
            ex.printStackTrace();
        }   
        System.out.println("INFO in Clause.testUnify(): " + c1.mguTermList(c2));
        System.out.println("INFO in Clause.testUnify(): " + c2.mguTermList(c1));
    }
    
    /** *************************************************************
     * A test method
     */
    public static void testRegexUnify() {
        
        String s1 = "pobj(at-1,Mary-1).";
        String s2 = "pobj(at*,?M).";
        String s3 = "pobj(boo-3,?M).";
        System.out.println("INFO in Clause.testRegexUnify(): attempting parses ");
        Clause c1 = null;
        Clause c2 = null;
        Clause c3 = null;
        try {
            Lexer lex = new Lexer(s1);
            lex.look();
            c1 = Clause.parse(lex, 0);
            System.out.println("INFO in Clause.testRegexUnify(): parsed " + c1);
            lex.look();
            lex = new Lexer(s2);
            c2 = Clause.parse(lex, 0);
            System.out.println("INFO in Clause.testRegexUnify(): parsed " + c2);
            lex = new Lexer(s3);
            c3 = Clause.parse(lex, 0);
            System.out.println("INFO in Clause.testRegexUnify(): parsed " + c3);
        }
        catch (Exception ex) {
            String message = ex.getMessage();
            System.out.println("Error in Clause.parse() " + message);
            ex.printStackTrace();
        }   
        System.out.println("INFO in Clause.testRegexUnify(): " + c1.mguTermList(c2));
        System.out.println("INFO in Clause.testRegexUnify(): " + c2.mguTermList(c1));
        System.out.println("INFO in Clause.testRegexUnify(): should fail: " + c2.mguTermList(c3));
    }
    
    /** *************************************************************
     * A test method
     */
    public static void testParse() {
        
        try {
            String input = "+det(bank-2, The-1).";
            Lexer lex = new Lexer(input);
            lex.look();
            System.out.println(Clause.parse(lex, 0));
        }
        catch (Exception ex) {
            String message = ex.getMessage();
            System.out.println("Error in Clause.parse() " + message);
            ex.printStackTrace();
        }   
    }
    
    /** *************************************************************
     * A test method
     */
    public static void main (String args[]) {
        
        testParse();
        //testUnify();
        //testRegexUnify();
    }
}
