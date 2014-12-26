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

import java.text.ParseException;
import java.util.*;
import java.lang.reflect.*;
import com.articulate.sigma.*;

/** *************************************************************
LHS ::= ClausePattern        Match & delete atomic clause
        +ClausePattern       Match & preserve atomic clause
        LHS, LHS             Boolean conjunction 
        (LHS | LHS)          Boolean disjunction
        —LHS                 Boolean negation        
        {ProcedureCall}      Procedural attachment
 */
public class LHS {

    public enum LHSop {AND,OR,NOT,PRESERVE,DELETE,PROC}
    public Clause clause;
    public LHS lhs1;
    public LHS lhs2;
    public LHSop operator;
    public Method method;
    
    /** ***************************************************************
     */
    public String toString() {
        
        StringBuffer sb = new StringBuffer();
        if (operator == LHSop.AND) 
            sb.append(lhs1.toString() + ", " + lhs2.toString());
        else if (operator == LHSop.OR)
            sb.append("(" + lhs1.toString() + " | " + lhs2.toString() + ")");
        else if (operator == LHSop.DELETE)
            sb.append(clause.toString());
        else if (operator == LHSop.PRESERVE)
            sb.append("+" + clause.toString());
        else if (operator == LHSop.NOT)
            sb.append("-" + lhs1.toString());
        else if (operator == LHSop.PROC)
            sb.append("{" + method.toString() + "}");
        return sb.toString();
    }
    
    /** ***************************************************************
     */
    public static LHS parse(Lexer lex, int startLine) {
        
        String errStart = "Parsing error in " + RuleSet.filename;
        String errStr;
        LHS lhs = new LHS();
        try {
            System.out.println("INFO in LHS.parse(): " + lex.look());
            if (lex.testTok(Lexer.Plus)) {
                lex.next();
                lhs.operator = LHSop.PRESERVE;
                lhs.lhs1 = LHS.parse(lex,startLine);
                return lhs;
            }
            else if (lex.testTok(Lexer.OpenPar)) {
                lex.next();
                lhs.operator = LHSop.OR;
                lhs.lhs1 = LHS.parse(lex,startLine);    
                if (!lex.testTok(Lexer.Or)) {
                    errStr = (errStart + ": Invalid end token '" + lex.look() + "' near line " + startLine);
                    throw new ParseException(errStr, startLine);
                }
                lex.next();
                lhs.lhs2 = LHS.parse(lex,startLine); 
                if (!lex.testTok(Lexer.ClosePar)) {
                    errStr = (errStart + ": Invalid end token '" + lex.look() + "' near line " + startLine);
                    throw new ParseException(errStr, startLine);
                }
                lex.next();
                return lhs;
            }
            else if (lex.testTok(Lexer.Negation)) {
                lhs.operator = LHSop.NOT;
                lhs.lhs1 = LHS.parse(lex,startLine);
                return lhs;
            }
            else if (lex.testTok(Lexer.OpenBracket)) {
                lhs.operator = LHSop.PROC;
                errStr = (errStart + ": Procedural attachment not implemented, near line " + startLine);
                throw new ParseException(errStr, startLine);            
            }
            // Now it's either just a clause or a left hand side
            Clause c = Clause.parse(lex,startLine);
            System.out.println("INFO in LHS.parse(): " + lex.look());
            if (lex.testTok(Lexer.Comma)) {
                lex.next();
                lhs.lhs2 = LHS.parse(lex, startLine);
            }
            else
                lhs.clause = c;
        }
        catch (Exception ex) {
            String message = ex.getMessage();
            System.out.println("Error in LHS.parse(): " + message);
            ex.printStackTrace();
        }
        return lhs;
    }
    
/*
String aMethod = "myMethod";
Object iClass = thisClass.newInstance();   // get the method
Method thisMethod = thisClass.getDeclaredMethod(aMethod, params); // call the method
thisMethod.invoke(iClass, paramsObj);
*/
    
    /** *************************************************************
     * A test method
     */
    public static void main (String args[]) {
    }
}
