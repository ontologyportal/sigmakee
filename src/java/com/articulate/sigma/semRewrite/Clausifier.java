package com.articulate.sigma.semRewrite;

import com.articulate.sigma.semRewrite.LHS.LHSop;

public class Clausifier {

    /** ***************************************************************
     */
    private static LHS moveNegationsInRecurse(LHS lhs, boolean flip) {  
        
        LHS newlhs = new LHS();
        if (flip) {
            switch(lhs.operator) {
            case AND : lhs.lhs1 = moveNegationsInRecurse(lhs.lhs1,true);
                       lhs.lhs2 = moveNegationsInRecurse(lhs.lhs1,true);
                       lhs.operator = LHS.LHSop.OR;
                       break; 
            case OR :  lhs.lhs1 = moveNegationsInRecurse(lhs.lhs1,true);
                       lhs.lhs2 = moveNegationsInRecurse(lhs.lhs1,true);
                       lhs.operator = LHS.LHSop.AND;
                       break; 
            case DELETE : break; 
            case PRESERVE : break; 
            case NOT : lhs = moveNegationsInRecurse(lhs.lhs1,true);
                       break; 
            case PROC : break; 
        }
        }
        else {
            switch(lhs.operator) {
                case AND : lhs.lhs1 = moveNegationsInRecurse(lhs.lhs1,false);
                           lhs.lhs2 = moveNegationsInRecurse(lhs.lhs1,false);
                           break; 
                case OR :  lhs.lhs1 = moveNegationsInRecurse(lhs.lhs1,false);
                           lhs.lhs2 = moveNegationsInRecurse(lhs.lhs1,false);
                           break; 
                case DELETE : break; 
                case PRESERVE : break; 
                case NOT : lhs.lhs1 = moveNegationsInRecurse(lhs.lhs1,true);
                           break; 
                case PROC : break; 
            }
        }
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
    * (a , b) | c becomes (a | c) , (b | c)
     */
    private static LHS distributeAndOverOr(LHS lhs) {  
        
        LHS newlhs = new LHS();
        return newlhs;
    }
    
    /** ***************************************************************
     */
    private static CNF separateConjunctions(LHS lhs) {  
        
        CNF cnf = new CNF();
        return cnf;
    }
    
    /** ***************************************************************
     */
    public static CNF clausify(LHS lhs) {  
        
        LHS result = moveNegationsIn(lhs);
        result = distributeAndOverOr(result);
        return separateConjunctions(result);
    }
    
    /** ***************************************************************
     */
    public static void main(String[] args) {  
        
        String rule = "-(sense(212345678,?E) | nsubj(?E,?X)) , dobj(?E,?Y) ==> " +
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
        System.out.println(Clausifier.clausify(r.lhs));
    }
}
