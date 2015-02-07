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
    @Override
    public boolean equals(Object o) {
    
        if (!(o instanceof CNF))
            return false;
        CNF cnf = (CNF) o;
        //System.out.println("INFO in CNF.equals(): Checking " + cnf + " against " + this);
        if (clauses.size() != cnf.clauses.size())
            return false;
        for (int i = 0; i < clauses.size(); i++) {
            //System.out.println("INFO in CNF.equals(): checking disjunct " + clauses.get(i) + 
            //        " " + cnf.clauses.get(i));
            if (!clauses.get(i).equals(cnf.clauses.get(i)))
                return false;
        }
        //System.out.println("INFO in CNF.equals(): true!");
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

    /** *************************************************************
     */
    public void preProcessQuestionWords(List<String> qwords) {
        
        for (Disjunct d: clauses)
            d.preProcessQuestionWords(qwords);
    }
    
    /** ***************************************************************
     */
    public void clearBound() {
        
        //System.out.println("INFO in CNF.clearBound(): before " + this);
        ArrayList<Disjunct> newclauses = new ArrayList<Disjunct>();
        for (int i = 0; i < clauses.size(); i++) {
            Disjunct d = clauses.get(i);
            d.clearBound();       
            if (!d.empty())
                newclauses.add(d);
        }
        //System.out.println("INFO in CNF.clearBound(): after " + this);
    }
    
    /** ***************************************************************
     */
    public void clearPreserve() {
        
        //System.out.println("INFO in CNF.clearBound(): before " + this);
        ArrayList<Disjunct> newclauses = new ArrayList<Disjunct>();
        for (int i = 0; i < clauses.size(); i++) {
            Disjunct d = clauses.get(i);
            d.clearPreserve();       
            if (!d.empty())
                newclauses.add(d);
        }
        //System.out.println("INFO in CNF.clearBound(): after " + this);
    }
    
    /** ***************************************************************
     */
    public void merge(CNF cnf) {
        
        //System.out.println("INFO in CNF.merge(): before " + this + "\narg: " + cnf);
        for (int i = 0; i < cnf.clauses.size(); i++)
            if (!clauses.contains(cnf.clauses.get(i)))
                clauses.add(cnf.clauses.get(i));
        //System.out.println("INFO in CNF.merge(): after " + this);
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
                    if (!lex.testTok(Lexer.FullStop))
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
     * Test a disjunct from a rule against a sentence.  It must succeed
     * for the rule to be bound.  If a binding is found, it can exit
     * without trying all the options.
     */
    private HashMap<String,String> unifyDisjunct(Disjunct d1, CNF cnf2, CNF cnf1, HashMap<String,String> bindings) {
        
        //System.out.println("INFO in CNF.unifyDisjunct(): checking " + d1 + " against " + cnf2);
        HashMap<String,String> result = new HashMap<String,String>();
        for (Disjunct d2 : cnf2.clauses) {  // sentence
            //System.out.println("INFO in CNF.unifyDisjunct(): checking " + d1 + " against " + d2);
            HashMap<String,String> bindings2 = d2.unify(d1);
            //System.out.println("INFO in CNF.unifyDisjunct(): d1 " + d1 + " d2 " + d2);
            //System.out.println("INFO in CNF.unifyDisjunct(): checked " + d1 + " against " + d2);
            //System.out.println("INFO in CNF.unifyDisjunct(): bindings " + bindings2);
            if (bindings2 != null) {  
                return bindings2;
            }
        }
        return null;
    }
    
    /** ***************************************************************
     * Unify this CNF with the argument.  Note that the argument should
     * be a superset of clauses of (or equal to) this instance.  The argument
     * is the "sentence" and this is the "rule"
     */
    public HashMap<String,String> unify(CNF cnf) {
        
        CNF cnfnew2 = cnf.deepCopy();  // sentence        
        CNF cnfnew1 = this.deepCopy(); // rule
        //System.out.println("INFO in CNF.unify(): cnf 1 " + cnf);
        //System.out.println("INFO in CNF.unify(): this " + this);
        HashMap<String,String> result = new HashMap<String,String>();
        for (int i = 0; i < cnfnew1.clauses.size(); i++) {  // rule
            Disjunct d1 = cnfnew1.clauses.get(i);
            HashMap<String,String> result2 = unifyDisjunct(d1,cnfnew2,cnfnew1,result);
            //System.out.println("INFO in CNF.unify(): results2 " + result2);
            //System.out.println("INFO in CNF.unify(): cnfnew1 " + cnfnew1);
            //System.out.println("INFO in CNF.unify(): cnfnew2 " + cnfnew2);
            if (result2 == null) { // every clause in the rule must match to succeed
                cnf.clearBound(); // if no success, wipe all the intermediate bindings.
                return null;
            }
            else {
                cnf.copyBoundFlags(cnfnew2);
                cnfnew1 = cnfnew1.applyBindings(result2);
                //System.out.println("INFO in CNF.unify(): cnf 1 " + cnfnew1);
                //System.out.println("INFO in CNF.unify(): cnf 2 " + cnfnew2);    
                cnfnew2 = cnfnew2.applyBindings(result2);
                result.putAll(result2);
                //System.out.println("INFO in CNF.unify(): bindings " + result); 
            }
        }
        if (result.keySet().size() == 0)
            result = null;
        //cnf.clearBound(); // if no success, wipe all the intermediate bindings.
        return result;
    }

    /** *************************************************************
     * A test method
     */
    public static void testMerge() {
        
        Lexer lex = new Lexer("sumo(BodyMotion,Bob-2), sumo(Human,John-1).");
        CNF cnf1 = CNF.parseSimple(lex);
        Lexer lex2 = new Lexer("foo(BodyMotion,Bob-2), bar(Human,John-1).");
        CNF cnf2 = CNF.parseSimple(lex2);        
        cnf1.merge(cnf2);
        System.out.println("INFO in CNF.testEquality(): should have four clauses: " + cnf1);
    }

    /** *************************************************************
     * A test method
     */
    public static void testParseSimple() {
        
        Lexer lex = new Lexer("num(?O,?N), +sumo(?C,?O).");
        CNF cnf1 = CNF.parseSimple(lex);    
        System.out.println("INFO in CNF.testParseSimple(): " + cnf1);
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
    public static void testContains() {
        
        Lexer lex = new Lexer("sumo(BodyMotion,Bob-2).");
        CNF cnf1 = CNF.parseSimple(lex);
        Lexer lex2 = new Lexer("sumo(BodyMotion,Bob-2).");
        CNF cnf2 = CNF.parseSimple(lex2);  
        ArrayList<CNF> al = new ArrayList<CNF>();
        al.add(cnf1);
        if (!al.contains(cnf2))
            al.add(cnf2);
        System.out.println("INFO in CNF.testEquality(): should be 1: " + al.size());
    }
    
    /** *************************************************************
     * A test method
     */
    public static void testUnify() {
        
        System.out.println("INFO in CNF.testUnify(): -------------------------------------");
        String rule = "sense(212345678,?E) ==> " +
                "(sumo(Foo,?E)).";
        Rule r = new Rule();
        r = Rule.parseString(rule);
        System.out.println(r.toString());
        CNF cnf1 = Clausifier.clausify(r.lhs);
        Lexer lex = new Lexer("sense(212345678,Foo).");
        CNF cnf = CNF.parseSimple(lex);
        System.out.println("INFO in CNF.testUnify(): cnf " + cnf);
        System.out.println("INFO in CNF.testUnify(): cnf1 " + cnf1);
        System.out.println("INFO in CNF.testUnify(): bindings: " + cnf1.unify(cnf));
        System.out.println("INFO in CNF.testUnify(): cnf " + cnf);
        System.out.println("INFO in CNF.testUnify(): expecting: Xsense(212345678,Foo).");
        
        System.out.println("INFO in CNF.testUnify(): -------------------------------------");
        rule = "sense(212345678,?E) ==> " +
                "(sumo(Foo,?E)).";
        r = new Rule();
        r = Rule.parseString(rule);
        System.out.println(r.toString());
        cnf1 = Clausifier.clausify(r.lhs);
        lex = new Lexer("sense(2123,Foo).");
        cnf = CNF.parseSimple(lex);
        System.out.println("INFO in CNF.testUnify(): cnf " + cnf);
        System.out.println("INFO in CNF.testUnify(): cnf1 " + cnf1);
        System.out.println("INFO in CNF.testUnify(): bindings  (should be null): " + cnf1.unify(cnf));
        
        System.out.println("INFO in CNF.testUnify(): -------------------------------------");
        String rule2 = "det(?X,What*), sumo(?O,?X).";
        lex = new Lexer(rule2);
        cnf1 = CNF.parseSimple(lex);
        String clauses = "nsubj(drives-2,John-1), root(ROOT-0,drives-2), sumo(Transportation,drives-2), sumo(Human,John-1).";
        lex = new Lexer(clauses);
        cnf = CNF.parseSimple(lex);
        System.out.println("INFO in CNF.testUnify(): cnf " + cnf);
        System.out.println("INFO in CNF.testUnify(): cnf1 " + cnf1);
        System.out.println("INFO in CNF.testUnify(): bindings (should be null): " + cnf1.unify(cnf));

        System.out.println("INFO in CNF.testUnify(): -------------------------------------");
        rule2 = "nsubj(?X,?Y), sumo(?O,?X).";
        lex = new Lexer(rule2);
        cnf1 = CNF.parseSimple(lex);
        clauses = "nsubj(drives-2,John-1), root(ROOT-0,drives-2), sumo(Transportation,drives-2), sumo(Human,John-1).";
        lex = new Lexer(clauses);
        cnf = CNF.parseSimple(lex);
        System.out.println("INFO in CNF.testUnify(): cnf " + cnf);
        System.out.println("INFO in CNF.testUnify(): cnf1 " + cnf1);
        System.out.println("INFO in CNF.testUnify(): bindings: " + cnf1.unify(cnf));
        System.out.println("INFO in CNF.testUnify(): expecting: Xnsubj(drives-2,John-1), root(ROOT-0,drives-2), Xsumo(Transportation,drives-2), sumo(Human,John-1).");
        System.out.println("INFO in CNF.testUnify(): cnf " + cnf);
        
        System.out.println("INFO in CNF.testUnify(): -------------------------------------");
        rule = "nsubj(?V,?Who*).";
        lex = new Lexer(rule);
        cnf1 = CNF.parseSimple(lex);
        String cnfstr = "nsubj(kicks-2,John-1), root(ROOT-0,kicks-2), det(cart-4,the-3), dobj(kicks-2,cart-4), sumo(Kicking,kicks-2), sumo(Human,John-1), sumo(Wagon,cart-4).";
        lex = new Lexer(cnfstr);
        cnf = CNF.parseSimple(lex);
        System.out.println("INFO in CNF.testUnify(): cnf " + cnf);
        System.out.println("INFO in CNF.testUnify(): cnf1 " + cnf1);
        System.out.println("INFO in CNF.testUnify(): bindings (should be null): " + cnf1.unify(cnf));
    }
    
    /** *************************************************************
     * A test method
     */
    public static void main (String args[]) {
        
        //testEquality();
        //testContains();
        //testMerge();
        testUnify();
        //testParseSimple();
    }
}
