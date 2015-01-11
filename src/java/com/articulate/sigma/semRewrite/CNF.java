package com.articulate.sigma.semRewrite;

import java.util.*;

public class CNF {

    public ArrayList<Disjunct> clauses = new ArrayList<Disjunct>();
    
    /** ***************************************************************
     */
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
    
    /** ***************************************************************
     */
    public CNF deepCopy() {
        
        CNF cnfnew = new CNF();
        for (int i = 0; i < clauses.size(); i++) {
            cnfnew.clauses.add(clauses.get(i).deepCopy());
        }
        return cnfnew;
    }
    
    /** ***************************************************************
     * Only positive clauses, no disjuncts, which is the output format
     * of the Stanford Dependency Parser
     */
    public static CNF parseSimple(Lexer lex) {
     
        CNF cnf = new CNF();
        try {            
            while (!lex.testTok(Lexer.EOFToken)) {
                Disjunct d = new Disjunct();
                Clause c = Clause.parse(lex, 0);
                d.disjuncts.add(c);
                cnf.clauses.add(d);
                if (lex.testTok(Lexer.Comma))
                    lex.next();
                else if (lex.testTok(Lexer.FullStop))
                    lex.next();
                else
                    System.out.println("Error in CNF.parseSimple(): Bad token: " + lex.look());
            }
        }
        catch (Exception ex) {
            String message = ex.getMessage();
            System.out.println("Error in CNF.parse(): " + message);
            ex.printStackTrace();
        }
        return cnf;
    }
    
    /** ***************************************************************
     * Apply variable substitutions to this set of clauses  
     */
    private CNF applyBindings(HashMap<String,String> bindings) {
        
        CNF cnf = new CNF();
        for (int i = 0; i < clauses.size(); i++) {
            Disjunct d = clauses.get(i);
            Disjunct dnew = new Disjunct();
            for (int j = 0; j < d.disjuncts.size(); j++) {
                Clause c = d.disjuncts.get(j);
                dnew.disjuncts.add(c.applyBindings(bindings));
            }
        }
        return cnf;
    }
    
    /** ***************************************************************
     * Unify this CNF with the argument.  Note that the argument should
     * be a superset of clauses of this instance.
     */
    public HashMap<String,String> unify(CNF cnf) {
        
        CNF cnfnew = cnf.deepCopy();
        HashMap<String,String> result = new HashMap<String,String>();
        for (int i = 0; i < cnf.clauses.size(); i++) {
            Disjunct d1 = cnf.clauses.get(i);
            for (int j = 0; j < clauses.size(); j++) {
                Disjunct d2 = clauses.get(j);
                //System.out.println("INFO in CNF.unify(): checking " + d1 + " against " + d2);
                HashMap<String,String> bindings = d1.unify(d2);
                if (bindings != null) {
                    cnfnew = cnfnew.applyBindings(bindings);
                    result.putAll(bindings);
                    //System.out.println("INFO in CNF.unify(): bindings " + bindings);
                }
            }
        }
        if (result.keySet().size() == 0)
            result = null;
        return result;
    }
}
