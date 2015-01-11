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
     */
    public HashMap<String,String> unify(Disjunct d) {
        
        for (int i = 0; i < d.disjuncts.size(); i++) {
            Clause d1 = d.disjuncts.get(i);
            for (int j = 0; j < disjuncts.size(); j++) {
                Clause d2 = disjuncts.get(j);
                HashMap<String,String> bindings = d2.mguTermList(d1);
                //System.out.println("INFO in Disjunct.unify(): checking " + d1 + " against " + d2);
                if (bindings != null)
                    return bindings;
            }
        }
        return null;
    }
}
