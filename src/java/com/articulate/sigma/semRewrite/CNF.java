package com.articulate.sigma.semRewrite;

import java.util.*;

public class CNF {

    public ArrayList<Disjunct> clauses = new ArrayList<Disjunct>();
    
    /** ***************************************************************
     */
    public String toString() {
        
        StringBuffer sb = new StringBuffer();
        //sb.append("[");
        for (int i = 0; i < clauses.size(); i++) {
            Disjunct d = clauses.get(i);
            sb.append(d.toString());
            if (clauses.size() > 1 && i < clauses.size() - 1)
                sb.append(", ");
        }
        //sb.append("]");
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
    
    /** *************************************************************
     */
    public boolean empty() {
        
        return clauses.size() == 0;
    }

    /** ***************************************************************
     */
    public void clearBound() {
        
        for (int i = 0; i < clauses.size(); i++) {
            Disjunct d = clauses.get(i);
            d.clearBound();           
        }
    }
    
    /** ***************************************************************
     */
    public void removeBound() {
        
        ArrayList<Disjunct> newClauses = new ArrayList<Disjunct>();
        for (int i = 0; i < clauses.size(); i++) {
            Disjunct d = clauses.get(i);
            d.removeBound();
            if (!d.empty())
                newClauses.add(d);
        }
        clauses = newClauses;
    }
    
    /** ***************************************************************
     * Only positive clauses, no disjuncts, which is the output format
     * of the Stanford Dependency Parser
     */
    public static CNF parseSimple(Lexer lex) {
     
        CNF cnf = new CNF();
        String token = "";
        try {            
            ArrayList<String> tokens = new ArrayList<String>();
            tokens.add(Lexer.EOFToken);
            tokens.add(Lexer.ClosePar);
            tokens.add(Lexer.FullStop);
            while (!lex.testTok(tokens)) {
                Disjunct d = new Disjunct();
                Clause c = Clause.parse(lex, 0);
                d.disjuncts.add(c);
                cnf.clauses.add(d);
                if (lex.testTok(Lexer.Comma))
                    lex.next();
                else if (lex.testTok(Lexer.ClosePar)) {                    
                    lex.next();
                    System.out.println("INFO in CNF.parseSimple(): final token: " + lex.look());
                    if (!lex.testTok(Lexer.FullStop))
                        System.out.println("Error in CNF.parseSimple(): Bad token: " + lex.look());
                }
                else
                    System.out.println("Error in CNF.parseSimple(): Bad token: " + lex.look());
            }
        }
        catch (Exception ex) {
            String message = ex.getMessage();
            System.out.println("Error in CNF.parse(): " + message);
            ex.printStackTrace();
        }
        System.out.println("INFO in CNF.parseSimple(): returning: " + cnf);
        return cnf;
    }
    
    /** ***************************************************************
     * Apply variable substitutions to this set of clauses  
     */
    public CNF applyBindings(HashMap<String,String> bindings) {
        
        CNF cnf = new CNF();
        for (int i = 0; i < clauses.size(); i++) {
            Disjunct d = clauses.get(i);
            Disjunct dnew = new Disjunct();
            for (int j = 0; j < d.disjuncts.size(); j++) {
                Clause c = d.disjuncts.get(j);
                //System.out.println("INFO in CNF.applyBindings(): 1 " + c);
                Clause c2 = c.applyBindings(bindings);
                //System.out.println("INFO in CNF.applyBindings(): 1.5 " + c2);
                dnew.disjuncts.add(c2);
                //System.out.println("INFO in CNF.applyBindings(): 2 " + dnew);
            }
            //System.out.println("INFO in CNF.applyBindings(): 3 " + dnew);
            cnf.clauses.add(dnew);
        }
        return cnf;
    }
    
    /** ***************************************************************
     * Unify this CNF with the argument.  Note that the argument should
     * be a superset of clauses of (or equal to) this instance.  The argument
     * is the "sentence" and this is the "rule"
     */
    public HashMap<String,String> unify(CNF cnf) {
        
        CNF cnfnew = cnf.deepCopy();
        //System.out.println("INFO in CNF.unify(): cnf 1 " + cnf);
        //System.out.println("INFO in CNF.unify(): this " + this);
        HashMap<String,String> result = new HashMap<String,String>();
        for (int i = 0; i < cnf.clauses.size(); i++) {
            Disjunct d1 = cnf.clauses.get(i);
            for (int j = 0; j < clauses.size(); j++) {
                Disjunct d2 = clauses.get(j);
                //System.out.println("INFO in CNF.unify(): checking " + d1 + " against " + d2);
                HashMap<String,String> bindings = d1.unify(d2);
                if (bindings != null) {                   
                    //System.out.println("INFO in CNF.unify(): cnf 2 " + cnf);
                    cnfnew = this.applyBindings(bindings);
                    this.clauses = cnfnew.clauses;
                    result.putAll(bindings);
                    //System.out.println("INFO in CNF.unify(): bindings " + result);
                    //System.out.println("INFO in CNF.unify(): cnf3  " + this);
                }
            }
        }
        if (result.keySet().size() == 0)
            result = null;
        return result;
    }
}
