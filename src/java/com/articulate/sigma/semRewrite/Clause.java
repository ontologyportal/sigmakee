package com.articulate.sigma.semRewrite;

/*
Copyright 2014-2015 IPsoft

Author: Adam Pease adam.pease@ipsoft.com

In conjunctive normal form (CNF) a formula is a conjunct of 
disjuncts.  This is the list of disjuncts.

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

import java.util.*;

public class Clause {

    public ArrayList<Literal> disjuncts = new ArrayList<Literal>();
    
    /** *************************************************************
     */
    public String toString() {
        
        StringBuffer sb = new StringBuffer();
        if (disjuncts.size() > 1)
            sb.append("(");
        for (int i = 0; i < disjuncts.size(); i++) {
            Literal c = disjuncts.get(i);
            sb.append(c.toString());
            if (disjuncts.size() > 1 && i < disjuncts.size() - 1)
                sb.append(" | ");
        }
        if (disjuncts.size() > 1)
            sb.append(")");
        return sb.toString();
    }
    
    /** *************************************************************
     */
    public Clause deepCopy() {
        
        Clause newd = new Clause();
        for (int i = 0; i < disjuncts.size(); i++) 
            newd.disjuncts.add(disjuncts.get(i).deepCopy());        
        return newd;
    }

    /** ***************************************************************
     */
    @Override
    public boolean equals(Object o) {
    
        if (!(o instanceof Clause))
            return false;
        Clause d = (Clause) o;
        if (disjuncts.size() != d.disjuncts.size())
            return false;
        for (int i = 0; i < disjuncts.size(); i++)
            if (!disjuncts.get(i).equals(d.disjuncts.get(i)))
                return false;
        return true;
    }
    
    /** ***************************************************************
     */
    public boolean empty() {
        
        if (disjuncts.size() == 0)
            return true;
        else
            return false;
    }
  
    /** *************************************************************
     */
    public void preProcessQuestionWords(List<String> qwords) {
        
        for (Literal c: disjuncts)
            c.preProcessQuestionWords(qwords);
    }
    
    /** ***************************************************************
     * Clear the bound flags on each Literal
     */
    public void clearBound() {
        
        for (int i = 0; i < disjuncts.size(); i++) {
            Literal l = disjuncts.get(i);
            if (l.bound)
                l.bound = false;
        }
    }
    
    /** ***************************************************************
     * Clear the preserve flags on each Literal
     */
    public void clearPreserve() {
        
        for (int i = 0; i < disjuncts.size(); i++) {
            Literal l = disjuncts.get(i);
            if (l.preserve)
                l.preserve = false;
        }
    }
    
    /** ***************************************************************
     * If literal is not marked "preserve" then remove it if bound and
     * then reset the preserve flag.  Note this should only be called
     * on inputs, not rules.
     */
    public void removeBound() {
        
        //System.out.println("INFO in Disjunct.removeBound(): before " + this);
        ArrayList<Literal> newdis = new ArrayList<Literal>();
        for (int i = 0; i < disjuncts.size(); i++) {
            Literal l = disjuncts.get(i);
            if (!l.bound || l.preserve) {
                l.bound = false;
                l.preserve = false;
                newdis.add(l);
            }
        }
        disjuncts = newdis;
        //System.out.println("INFO in Disjunct.removeBound(): after " + this);
    }
    
    /** ***************************************************************
     * Copy bound flags to this set of clauses  
     */
    public void copyBoundFlags(Clause d) {
     
        for (int i = 0; i < disjuncts.size(); i++) {
            if (d.disjuncts.get(i).bound)
                disjuncts.get(i).bound = true;
            if (d.disjuncts.get(i).preserve)
                disjuncts.get(i).preserve = true;
        }
    }
    
    /** *************************************************************
     * @return a clause that results from applying a binding list
     * to this clause.
     */
    public Clause applyBindings(HashMap<String,String> bindings) {
        
        Clause c = new Clause();
        for (int i = 0; i < disjuncts.size(); i++) {
            Literal l = disjuncts.get(i);
            c.disjuncts.add(l.applyBindings(bindings));
        }            
        return c;
    }
        
    /** *************************************************************
     * The argument to this method is the rule and this is the sentence
     * @return the set of variable bindings.  The key is the variable
     * and the value is the binding.
     */
    public HashMap<String,String> unify(Clause d) {
        
        for (int i = 0; i < d.disjuncts.size(); i++) {
            Literal c1 = d.disjuncts.get(i);  // rule
            //System.out.println("INFO in Disjunct.unify(): checking " + c1);
            if (c1.pred.equals("isCELTclass") && c1.isGround())
                if (Procedures.isCELTclass(c1).equals("true"))
                    return new HashMap<String,String>();
            if (c1.pred.equals("isSubclass") && c1.isGround())
                if (Procedures.isSubclass(c1).equals("true"))
                    return new HashMap<String,String>();
            if (c1.pred.equals("isInstanceOf") && c1.isGround())
                if (Procedures.isInstanceOf(c1).equals("true"))
                    return new HashMap<String,String>();
            for (int j = 0; j < disjuncts.size(); j++) {
                Literal c2 = disjuncts.get(j);
                HashMap<String,String> bindings = c2.mguTermList(c1);
                //System.out.println("INFO in Disjunct.unify(): checking " + c1 + " against " + c2);
                if (bindings != null) {
                    if (c1.preserve)
                        c2.preserve = true;
                    c2.bound = true; // mark as bound in case the rule consumes the clauses ( a ==> rule not a ?=>)
                    //System.out.println("INFO in Disjunct.unify(): bound: " + c2);
                    return bindings;
                }
            }
        }
        //System.out.println("INFO in Disjunct.unify(): this: " + this);
        //System.out.println("INFO in Disjunct.unify(): d: " + d);
        return null;
    }
}
