package com.articulate.sigma.semRewrite;

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
    public boolean equals(Disjunct d) {
    
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
    public void removeBound() {
        
        ArrayList<Clause> newdis = new ArrayList<Clause>();
        for (int i = 0; i < disjuncts.size(); i++) {
            Clause c = disjuncts.get(i);
            if (!c.bound)
                newdis.add(c);
        }
        disjuncts = newdis;
    }
    
    /** ***************************************************************
     * Copy bound flags to this set of clauses  
     */
    public void copyBoundFlags(Disjunct d) {
     
        for (int i = 0; i < disjuncts.size(); i++)
            disjuncts.get(i).bound = d.disjuncts.get(i).bound;
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
            Clause c1 = d.disjuncts.get(i);
            for (int j = 0; j < disjuncts.size(); j++) {
                Clause c2 = disjuncts.get(j);
                HashMap<String,String> bindings = c2.mguTermList(c1);
                //System.out.println("INFO in Disjunct.unify(): checking " + c1 + " against " + c2);
                if (bindings != null) {
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
