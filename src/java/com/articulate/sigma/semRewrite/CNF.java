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
    public boolean equals(CNF cnf) {
    
        if (clauses.size() != cnf.clauses.size())
            return false;
        for (int i = 0; i < clauses.size(); i++) {
            System.out.println("INFO in CNF.equals(): checking disjunct " + clauses.get(i) + 
                    " " + cnf.clauses.get(i));
            if (!clauses.get(i).equals(cnf.clauses.get(i)))
                return false;
        }
        return true;
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
        
        //System.out.println("INFO in CNF.clearBound(): before " + this);
        for (int i = 0; i < clauses.size(); i++) {
            Disjunct d = clauses.get(i);
            d.clearBound();           
        }
        //System.out.println("INFO in CNF.clearBound(): after " + this);
    }
    
    /** ***************************************************************
     */
    public CNF removeBound() {
        
        //System.out.println("INFO in CNF.removeBound(): before " + this);
        CNF newCNF = new CNF();
        for (int i = 0; i < clauses.size(); i++) {
            Disjunct d = clauses.get(i);
            d.removeBound();
            if (!d.empty())
                newCNF.clauses.add(d);
        }
        //System.out.println("INFO in CNF.removeBound(): after " + newCNF);
        return newCNF;
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
                    //System.out.println("INFO in CNF.parseSimple(): final token: " + lex.look());
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
        //System.out.println("INFO in CNF.parseSimple(): returning: " + cnf);
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
     * Copy bound flags to this set of clauses  
     */
    public void copyBoundFlags(CNF cnf) {
     
        for (int i = 0; i < clauses.size(); i++)
            clauses.get(i).copyBoundFlags(cnf.clauses.get(i));
    }
    
    /** ***************************************************************
     * Unify this CNF with the argument.  Note that the argument should
     * be a superset of clauses of (or equal to) this instance.  The argument
     * is the "sentence" and this is the "rule"
     */
    public HashMap<String,String> unify(CNF cnf) {
        
        CNF cnfnew1 = cnf.deepCopy();  // sentence        
        CNF cnfnew2 = this.deepCopy(); // rule
        //System.out.println("INFO in CNF.unify(): cnf 1 " + cnf);
        //System.out.println("INFO in CNF.unify(): this " + this);
        HashMap<String,String> result = new HashMap<String,String>();
        for (int i = 0; i < cnfnew1.clauses.size(); i++) {  // sentence
            Disjunct d1 = cnfnew1.clauses.get(i);
            for (int j = 0; j < cnfnew2.clauses.size(); j++) {  // rule
                Disjunct d2 = cnfnew2.clauses.get(j);
                //System.out.println("INFO in CNF.unify(): checking " + d1 + " against " + d2);
                HashMap<String,String> bindings = d1.unify(d2);
                //System.out.println("INFO in CNF.unify(): d1 " + d1 + " d2 " + d2);
                if (bindings != null) {        
                    cnf.copyBoundFlags(cnfnew1);
                    cnfnew1 = cnfnew1.applyBindings(bindings);
                    //System.out.println("INFO in CNF.unify(): cnf 1 " + cnfnew1);
                    //System.out.println("INFO in CNF.unify(): cnf 2 " + cnfnew2);
                    
                    cnfnew2 = cnfnew2.applyBindings(bindings);
                    result.putAll(bindings);
                    //System.out.println("INFO in CNF.unify(): bindings " + result);
                    //System.out.println("INFO in CNF.unify(): this  " + this);
                }
            }
        }
        if (result.keySet().size() == 0)
            result = null;
        return result;
    }

    /** *************************************************************
     * A test method
     */
    public static void testEquality() {
        
        Lexer lex = new Lexer("sumo(BodyMotion,Bob-2), sumo(Human,John-1).");
        CNF cnf1 = CNF.parseSimple(lex);
        Lexer lex2 = new Lexer("sumo(BodyMotion,Bob-2), sumo(Human,John-1).");
        CNF cnf2 = CNF.parseSimple(lex2);        
        System.out.println("INFO in CNF.testEquality(): should be true: " + cnf1.equals(cnf2));
    }
    
    /** *************************************************************
     * A test method
     */
    public static void testUnify() {
        
        String rule = "sense(212345678,?E) ==> " +
                "(sumo(Foo,?E)).";
        Rule r = new Rule();
        r = Rule.parseString(rule);
        System.out.println(r.toString());
        CNF cnf1 = Clausifier.clausify(r.lhs);
        Lexer lex = new Lexer("sense(212345678,Foo).");
        CNF cnf = CNF.parseSimple(lex);
        System.out.println("INFO in CNF.main(): " + cnf1.unify(cnf));
        System.out.println("INFO in CNF.main(): cnf " + cnf);
        System.out.println("INFO in CNF.main(): cnf1 " + cnf1);
    }
    
    /** *************************************************************
     * A test method
     */
    public static void main (String args[]) {
        
        testEquality();
    }
}
