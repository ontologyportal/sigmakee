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

import java.util.*;

public class Disjunct {

    public ArrayList<Clause> disjuncts = new ArrayList<Clause>();
    
    /** *************************************************************
     */
    public String toString() {
        
        StringBuffer sb = new StringBuffer();
        if (disjuncts.size() > 1)
            sb.append("(");
        for (int i = 0; i < disjuncts.size(); i++) {
            Clause c = disjuncts.get(i);
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
    public Disjunct deepCopy() {
        
        Disjunct newd = new Disjunct();
        for (int i = 0; i < disjuncts.size(); i++) 
            newd.disjuncts.add(disjuncts.get(i).deepCopy());        
        return newd;
    }

    /** ***************************************************************
     */
    @Override
    public boolean equals(Object o) {
    
        if (!(o instanceof Disjunct))
            return false;
        Disjunct d = (Disjunct) o;
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
        
        for (Clause c: disjuncts)
            c.preProcessQuestionWords(qwords);
    }
    
    /** ***************************************************************
     */
    public void clearBound() {
        
        for (int i = 0; i < disjuncts.size(); i++) {
            Clause c = disjuncts.get(i);
            if (c.bound)
                c.bound = false;
        }
    }
    
    /** ***************************************************************
     */
    public void clearPreserve() {
        
        for (int i = 0; i < disjuncts.size(); i++) {
            Clause c = disjuncts.get(i);
            if (c.preserve)
                c.preserve = false;
        }
    }
    
    /** ***************************************************************
     * If clause is not marked "preserve" then remove it if bound and
     * then reset the preserve flag.  Note this should only be called
     * on inputs, not rules.
     */
    public void removeBound() {
        
        //System.out.println("INFO in Disjunct.removeBound(): before " + this);
        ArrayList<Clause> newdis = new ArrayList<Clause>();
        for (int i = 0; i < disjuncts.size(); i++) {
            Clause c = disjuncts.get(i);
            if (!c.bound || c.preserve) {
                c.bound = false;
                c.preserve = false;
                newdis.add(c);
            }
        }
        disjuncts = newdis;
        //System.out.println("INFO in Disjunct.removeBound(): after " + this);
    }
    
    /** ***************************************************************
     * Copy bound flags to this set of clauses  
     */
    public void copyBoundFlags(Disjunct d) {
     
        for (int i = 0; i < disjuncts.size(); i++) {
            if (d.disjuncts.get(i).bound)
                disjuncts.get(i).bound = true;
            if (d.disjuncts.get(i).preserve)
                disjuncts.get(i).preserve = true;
        }
    }
    
    /** *************************************************************
     */
    public Disjunct applyBindings(HashMap<String,String> bindings) {
        
        Disjunct d = new Disjunct();
        for (int i = 0; i < disjuncts.size(); i++) {
            Clause c = disjuncts.get(i);
            d.disjuncts.add(c.applyBindings(bindings));
        }            
        return d;
    }
        
    /** *************************************************************
     * argument is the rule and this is the sentence
     */
    public HashMap<String,String> unify(Disjunct d) {
        
        for (int i = 0; i < d.disjuncts.size(); i++) {
            Clause c1 = d.disjuncts.get(i);  // rule
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
                Clause c2 = disjuncts.get(j);
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
