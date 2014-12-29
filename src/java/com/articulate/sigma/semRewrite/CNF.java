package com.articulate.sigma.semRewrite;

import java.util.*;

public class CNF {

    public ArrayList<Disjunct> clauses = new ArrayList<Disjunct>();
    
    public String toString() {
        
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = 0; i < clauses.size(); i++) {
            Disjunct d = clauses.get(i);
            sb.append(d.toString());
            if (clauses.size() > 1 && i < clauses.size() - 1)
                sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
    
}
