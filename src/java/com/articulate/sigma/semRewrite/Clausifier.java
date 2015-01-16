package com.articulate.sigma.semRewrite;

import java.lang.reflect.Method;
import com.articulate.sigma.semRewrite.*;

// Note that because the language is so simple we only have to 
// handle moving negations and disjunctions.
public class Clausifier {

    public static boolean changed = false;
    
    /** ***************************************************************
     */
    private static LHS moveNegationsInRecurse(LHS lhs, boolean flip) {  
        
        //System.out.println("Info in Clausifier.moveNegationsInRecurse(): flip: " + flip + "\n" + lhs);
        LHS newlhs = new LHS();
        newlhs.clause = lhs.clause;
        newlhs.lhs1 = lhs.lhs1;
        newlhs.lhs2 = lhs.lhs2;
        newlhs.operator = lhs.operator;
        newlhs.method = lhs.method;
        if (flip) {            
            switch(lhs.operator) {
                case AND : newlhs.lhs1 = moveNegationsInRecurse(lhs.lhs1,true);
                           newlhs.lhs2 = moveNegationsInRecurse(lhs.lhs2,true);
                           newlhs.operator = LHS.LHSop.OR;
                           break; 
                case OR :  newlhs.lhs1 = moveNegationsInRecurse(lhs.lhs1,true);
                           newlhs.lhs2 = moveNegationsInRecurse(lhs.lhs2,true);
                           newlhs.operator = LHS.LHSop.AND;
                           break; 
                case DELETE : newlhs.clause.negated = !newlhs.clause.negated;
                           break; 
                case PRESERVE : newlhs.clause.negated = !newlhs.clause.negated;
                           break; 
                case NOT : newlhs = moveNegationsInRecurse(lhs.lhs1,false);
                           break; 
                case PROC : break; 
            }
        }
        else {
            switch(lhs.operator) {
                case AND : newlhs.lhs1 = moveNegationsInRecurse(lhs.lhs1,false);
                           newlhs.lhs2 = moveNegationsInRecurse(lhs.lhs2,false);
                           break; 
                case OR :  newlhs.lhs1 = moveNegationsInRecurse(lhs.lhs1,false);
                           newlhs.lhs2 = moveNegationsInRecurse(lhs.lhs2,false);
                           break; 
                case DELETE : break; 
                case PRESERVE : break; 
                case NOT : newlhs = moveNegationsInRecurse(lhs.lhs1,true);
                           break; 
                case PROC : break; 
            }
        }
        //System.out.println("Info in Clausifier.moveNegationsInRecurse(): returning: " + newlhs);
        return newlhs;
    }
    
    /** ***************************************************************
     * -(p | q) becomes -p , -q
     * -(p , q) becomes -p | -q
     */
    private static LHS moveNegationsIn(LHS lhs) {  
         
        return moveNegationsInRecurse(lhs,false);
    }
    
    /** ***************************************************************
     * (a & b) | c becomes (a | c) & (b | c)
     */
    private static LHS distributeAndOverOrRecurse(LHS form) {
    
        //System.out.println("INFO in Clausifier.distributeAndOverOrRecurse(): " + form);
        LHS result = form.deepCopy();
        if (form.lhs1 != null)
            result.lhs1 = distributeAndOverOr(form.lhs1);
        if (form.lhs2 != null)
            result.lhs2 = distributeAndOverOr(form.lhs2);
        //System.out.println("INFO in Clausifier.distributeAndOverOrRecurse(): (2): " + KIF.format(form.toKIFString()));
        if (form.operator.equals(LHS.LHSop.OR)) {
            //System.out.println("INFO in Clausifier.distributeAndOverOrRecurse(): top level or: " + form);
            if (result.lhs1 != null && result.lhs1.operator.equals(LHS.LHSop.AND)) {
                //System.out.println("INFO in Clausifier.distributeAndOverOrRecurse(): lhs1 and: " + form);
                LHS newParent = new LHS();
                newParent.operator = LHS.LHSop.AND;
                LHS newChild1 = new LHS();
                newChild1.operator = LHS.LHSop.OR;
                if (result.lhs1.lhs1 != null)
                    newChild1.lhs1 = result.lhs1.lhs1;
                if (result.lhs1.clause != null)
                    newChild1.clause = result.lhs1.clause;
                if (result.lhs2 != null)
                    newChild1.lhs2 = result.lhs2;
                LHS newChild2 = new LHS();
                newChild2.operator = LHS.LHSop.OR;
                if (result.lhs1.lhs2 != null)
                    newChild2.lhs1 = result.lhs1.lhs2;
                if (result.lhs1.clause != null)
                    newChild2.clause = result.lhs1.clause;
                if (result.lhs2 != null)
                    newChild2.lhs2 = result.lhs2;
                if (result.clause != null)
                    newChild2.clause = result.clause;
                newParent.lhs1 = newChild1;
                newParent.lhs2 = newChild2;
                changed = true;
                //System.out.println("INFO in Clausifier.distributeAndOverOrRecurse(): result: " + form);
                return newParent;
            }
            else if (result.lhs2 != null && result.lhs2.operator.equals(LHS.LHSop.AND)) {
                //System.out.println("INFO in Clausifier.distributeAndOverOrRecurse(): lhs2 and: " + form);
                LHS newParent = new LHS();
                newParent.operator = LHS.LHSop.AND;
                LHS newChild1 = new LHS();
                newChild1.operator = LHS.LHSop.OR;
                if (result.lhs2.lhs1 != null)
                    newChild1.lhs1 = result.lhs2.lhs1;
                if (result.lhs2.clause != null)
                    newChild1.clause = result.lhs2.clause;
                if (result.lhs1 != null)
                    newChild1.lhs2 = result.lhs1;
                if (result.clause != null)
                    newChild1.clause = result.clause;
                LHS newChild2 = new LHS();
                newChild2.operator = LHS.LHSop.OR;
                if (result.lhs2.lhs2 != null)
                    newChild2.lhs1 = result.lhs2.lhs2;
                if (result.lhs2.clause != null)
                    newChild2.clause = result.lhs2.clause;
                if (result.lhs1 != null)
                    newChild2.lhs2 = result.lhs1;
                if (result.clause != null)
                    newChild2.clause = result.clause;
                newParent.lhs1 = newChild1;
                newParent.lhs2 = newChild2;
                changed = true;
                //System.out.println("INFO in Clausifier.distributeAndOverOrRecurse(): result: " + form);
                return newParent;
            }   
        }
        return result;
    } 

    /** ***************************************************************
     * (a , b) | c becomes (a | c) , (b | c)
     */
    private static LHS distributeAndOverOr(LHS form) {
        
        LHS result = form.deepCopy();
        changed = true;
        while (changed) {
            changed = false;
            result = distributeAndOverOrRecurse(result);
        }
        return result;
    }
    
    /** ***************************************************************
     */
    private static CNF separateConjunctions(LHS lhs) {  
        
        CNF cnf = new CNF();
        if (lhs.operator.equals(LHS.LHSop.AND)) {
            if (lhs.lhs1.clause != null) {
                Disjunct d = new Disjunct();
                d.disjuncts.add(lhs.lhs1.clause);
                cnf.clauses.add(d);
            }
            else
                cnf.clauses.addAll(separateConjunctions(lhs.lhs1).clauses);
            if (lhs.lhs2.clause != null){
                Disjunct d = new Disjunct();
                d.disjuncts.add(lhs.lhs2.clause);
                cnf.clauses.add(d);
            }
            else
                cnf.clauses.addAll(separateConjunctions(lhs.lhs2).clauses);
        }
        else if (lhs.operator.equals(LHS.LHSop.OR)) {
            Disjunct d = new Disjunct();
            d.disjuncts.add(lhs.lhs1.clause);
            d.disjuncts.add(lhs.lhs2.clause);
            cnf.clauses.add(d);
        }
        else if (lhs.clause != null) {
            Disjunct d = new Disjunct();
            d.disjuncts.add(lhs.clause);
            cnf.clauses.add(d);
        }
        else {
            System.out.println("Error in Clausifier.separateConjunctions(): bad operator: " + lhs.operator);
        }    
        return cnf;
    }
    
    /** ***************************************************************
     */
    public static CNF clausify(LHS lhs) {  
        
        System.out.println("INFO in RuleSet.clausify(): (lhs) " + lhs);
        LHS result = moveNegationsIn(lhs);
        result = distributeAndOverOr(result);
        return separateConjunctions(result);
    }
    
    /** ***************************************************************
     */
    public static RuleSet clausify(RuleSet rs) {  
        
        RuleSet newrs = new RuleSet();
        for (int i = 0; i < rs.rules.size(); i++) {
            Rule r = rs.rules.get(i);
            System.out.println("INFO in RuleSet.clausify(): " + r.lhs);
            LHS result = moveNegationsIn(r.lhs);
            result = distributeAndOverOr(result);
            r.cnf = separateConjunctions(result);
            newrs.rules.add(r);
        }
        return newrs;
    }
    
    /** ***************************************************************
     */
    public static void main(String[] args) {  
        
        String rule = "(sense(212345678,?E) , nsubj(?E,?X) | dobj(?E,?Y) , a(?E,?Y)) ==> " +
                "{(exists (?X ?E ?Y) " + 
                  "(and " +
                    "(instance ?X Organization) " +
                    "(instance ?Y Human)" +
                    "(instance ?E Hiring)" +
                    "(agent ?E ?X) " +
                    "(patient ?E ?Y)))}.";
        Rule r = new Rule();
        r = Rule.parseString(rule);
        System.out.println(r.toString());
        r.lhs = Clausifier.moveNegationsIn(r.lhs);
        System.out.println("moved negations in: \n" + r.lhs);
        LHS lhs = Clausifier.distributeAndOverOr(r.lhs);
        System.out.println("Distributed OR: \n" + lhs);
        CNF cnf = separateConjunctions(lhs);
        System.out.println("Flattened: \n" + cnf);
    }
}
