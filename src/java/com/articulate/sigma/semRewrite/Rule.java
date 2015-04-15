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

import java.io.*;
import java.text.ParseException;
import java.util.*;

import com.articulate.sigma.*;

/** *************************************************************
 * Rule ::= LHS ==> RHS.     Obligatory rewrite
            LHS ?=> RHS.     Optional rewrite
            /­- Clause.        Permanent, unresourced fact
 *             
 * Values for variables on LHS are unified with variables on the 
 * RHS           
 */
public class Rule {

    public CNF cnf;
    public enum RuleOp {IMP, OPT, CLAUSE}
    public LHS lhs;
    public RuleOp operator;  
    public RHS rhs;
    public Literal clause;
    public int startLine = -1;
    
    /** ***************************************************************
     */
    public String toString() {
        
        StringBuffer sb = new StringBuffer();
        if (cnf != null) {
            sb.append(cnf.toString());
            if (operator == RuleOp.IMP)
                sb.append(" ==> ");
            if (operator == RuleOp.OPT)
                sb.append(" ?=> ");
            sb.append(rhs.toString() + ".");
        }
        else if (operator == RuleOp.CLAUSE) 
            sb.append("/- " + clause.toString() + ".");                    
        else {
            sb.append(lhs.toString());
            if (operator == RuleOp.IMP)
                sb.append(" ==> ");
            if (operator == RuleOp.OPT)
                sb.append(" ?=> ");
            sb.append(rhs.toString()+ ".");
        }
        sb.append(" ; line " + startLine);
        return sb.toString();
    }
    
    /** ***************************************************************
     */
    public Rule deepCopy() {
        
        Rule r = new Rule();
        if (cnf != null)
            r.cnf =  cnf.deepCopy();
        if (lhs != null)
            r.lhs = lhs.deepCopy();
        r.operator = operator;  
        if (rhs != null)
            r.rhs = rhs.deepCopy();
        if (clause != null)
            r.clause = clause.deepCopy();
        r.startLine = startLine;
        return r;
    }
    
    /** ***************************************************************
     * We won't know whether it's a fact or a rule until we read the 
     * first token, so the first token for LHS will always be already
     * in st.sval
     */
    public static Rule parse(Lexer lex) {
        
        Rule r = new Rule();
        r.startLine = lex.linepos(); 
        String errStart = "Parsing error in " + RuleSet.filename;
        String errStr = null;
        boolean isEOL = false;
        try {                                                               
            if (lex.testTok(Lexer.Clause)) {
                r.operator = RuleOp.CLAUSE;    
                r.clause = Literal.parse(lex,r.startLine);  
            }
            else {
                do {
                    r.lhs = LHS.parse(lex,r.startLine);     
                    if (lex.testTok(Lexer.Comma)) {
                        lex.next();
                        LHS newlhs = new LHS();
                        newlhs.lhs1 = r.lhs;
                        newlhs.operator = LHS.LHSop.AND;
                        newlhs.lhs2 = LHS.parse(lex, r.startLine);
                        r.lhs = newlhs;
                    }
                } while (lex.testTok(Lexer.Comma));
                    
                if (lex.testTok(Lexer.Implies))  
                    r.operator = RuleOp.IMP;
                else if (lex.testTok(Lexer.OptImplies)) 
                    r.operator = RuleOp.OPT;
                else {
                    errStr = (errStart + ": Invalid rule operator '" + lex.look() + "' near line " + r.startLine);
                    throw new ParseException(errStr, r.startLine); 
                }
                lex.next();
                //System.out.println("Info in Rule.parse(): " + lex.look());
                r.rhs = RHS.parse(lex,r.startLine);
                //System.out.println("Info in Rule.parse(): 2 " + lex.look());
                if (!lex.testTok(Lexer.FullStop))  {
                    errStr = (errStart + ": Invalid end token '" + lex.look() + "' near line " + r.startLine);
                    throw new ParseException(errStr, r.startLine); 
                }
                lex.next();
            }
        }
        catch (Exception ex) {
            String message = ex.getMessage();
            System.out.println("Error in RULE.parse(): " + message);
            ex.printStackTrace();
        }
        //System.out.println("Info in Rule.parse(): returning: " + r);
        return r;
    }
    
    /** ***************************************************************
     */
    public static Rule parseString(String s) {
        
        Lexer lex = new Lexer(s);
        return parse(lex);
    }

    /** *************************************************************
     * A test method
     */
    public static void testParse() {
        
        String s = "+num(?O,?N), +sumo(?C,?O) ==> (instance(?O,Collection), membersType(?O,?C), membersCount(?O,?N)).";
        //System.out.println("INFO in Rule.testParse(): " + parseString(s));
        //s = "+nsubj(?C2,?X), +amod(?C2,?C), cop(?C2,be*), det(?C2,?D), sumo(?Y,?C), sumo(Human,?X), isInstanceOf(?Y,Nation) ==> (citizen(?X,?Y)).";
        //System.out.println("INFO in Rule.testParse(): " + parseString(s));
        s = "day(?T,?D), month(?T,?M), year(?T,?Y), StartTime(?V,?T) ==> {(equal (BeginFn (WhenFn ?V)) (DayFn ?D (MonthFn ?M (YearFn ?Y))))}.";
        System.out.println("INFO in Rule.testParse(): " + parseString(s));
        s = "day(?T,?D), month(?T,?M), year(?T,?Y), StartTime(?V,?T).";
        Lexer lex4 = new Lexer(s);
        CNF cnf5 = CNF.parseSimple(lex4);
        System.out.println("INFO in Rule.testParse(): " + cnf5);
    }
    
    /** *************************************************************
     * A test method
     */
    public static void main (String args[]) {
        
        testParse();
    }
}
