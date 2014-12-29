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

import com.articulate.sigma.*;

/** *************************************************************
 * pred(arg1,arg2).  
 * arg1 and/or arg2 can be variables which are denoted by a 
 * leading '?'
 */
public class Clause {

    public boolean negated = false;
    public String pred;
    public String arg1;
    public String arg2;
    
    /** ***************************************************************
     */
    public String toString() {
        
        StringBuffer sb = new StringBuffer();
        sb.append(pred + "(" + arg1 + "," + arg2 + ")");
        return sb.toString();
    }
    
    /** ***************************************************************
     * The predicate must already have been read
     */
    public static Clause parse(Lexer lex, int startLine) {

        String errStart = "Parsing error in " + RuleSet.filename;
        String errStr;
        Clause cl = new Clause();
        try {
            System.out.println("INFO in Clause.parse(): " + lex.look());
            cl.pred = lex.next();
            System.out.println("INFO in Clause.parse(): " + lex.look());
            if (!lex.testTok(Lexer.OpenPar)) {
                errStr = (errStart + ": Invalid token '" + lex.look() + "' near line " + startLine);
                throw new ParseException(errStr, startLine);
            }
            lex.next();
            System.out.println("INFO in Clause.parse(): " + lex.look());
            cl.arg1 = lex.next();
            System.out.println("INFO in Clause.parse(): " + lex.look());
            if (!lex.testTok(Lexer.Comma)) {
                errStr = (errStart + ": Invalid token '" + lex.look() + "' near line " + startLine);
                throw new ParseException(errStr, startLine);
            }
            lex.next();
            System.out.println("INFO in Clause.parse(): " + lex.look());
            cl.arg2 = lex.next();
            System.out.println("INFO in Clause.parse(): " + lex.look());
            if (!lex.testTok(Lexer.ClosePar)) {
                errStr = (errStart + ": Invalid token '" + lex.look() + "' near line " + startLine);
                throw new ParseException(errStr, startLine);
            } 
            lex.next();
        }
        catch (Exception ex) {
            String message = ex.getMessage();
            RuleSet.warningSet.add("Error in KIF.parse() " + message);
            ex.printStackTrace();
        }	
        return cl;
    }
    
    /** *************************************************************
     * A test method
     */
    public static void main (String args[]) {
    }
}
