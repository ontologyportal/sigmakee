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

public class RHS {

    public Formula form;
    boolean stop;
    
    /** ***************************************************************
     */
    public String toString() {
        
        StringBuffer sb = new StringBuffer();
        if (stop)
            sb.append("stop");
        else if (form == null)
            sb.append("0");
        else 
            sb.append(form.toString());
        return sb.toString();
    }
    
    /** ***************************************************************
     * The predicate must already have been read
     */
    public static RHS parse(Lexer lex, int startLine) {

        String errStart = "Parsing error in " + RuleSet.filename;
        String errStr;
        RHS rhs = new RHS();
        try {
            if (lex.testTok(Lexer.Stop)) {
                rhs.stop = true;
                lex.next();
                //System.out.println("Info in RHS.parse(): " + lex.look());
                if (!lex.testTok(Lexer.Stop)) {
                    errStr = (errStart + ": Invalid end token '" + lex.next() + "' near line " + startLine);
                    throw new ParseException(errStr, startLine);
                }
            }
            else if (lex.testTok(Lexer.Zero)) {
                lex.next();
            }
            else if (lex.testTok(Lexer.OpenBracket)) {
                StringBuffer sb = new StringBuffer();
                String st = lex.nextUnfiltered();
                while (!st.equals("}")) {
                    st = lex.nextUnfiltered();
                    if (!st.equals("}"))
                        sb.append(st);
                }
                rhs.form = new Formula(sb.toString()); 
                //System.out.println("Info in RHS.parse(): SUMO: " + sb.toString());
            }
            //System.out.println("Info in RHS.parse(): " + lex.look());
        }
        catch (Exception ex) {
            String message = ex.getMessage();
            RuleSet.warningSet.add("Error in KIF.parse() " + message);
            ex.printStackTrace();
        }
        return rhs;
    }
    
    /** *************************************************************
     * A test method
     */
    public static void main (String args[]) {
    }
}
