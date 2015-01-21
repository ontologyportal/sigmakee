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
    public Clause clause;
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
        return sb.toString();
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
                r.clause = Clause.parse(lex,r.startLine);  
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
    public static void main (String args[]) {
    }
}
