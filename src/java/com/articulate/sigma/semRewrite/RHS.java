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
            System.out.println("Error in RHS.parse() " + message);
            ex.printStackTrace();
        }
        return rhs;
    }
 
    /** ***************************************************************
     * Apply variable substitutions to this set of clauses  
     * TODO: note that a replace for ?A will erroneously match ?AB
     */
    public RHS applyBindings(HashMap<String,String> bindings) {
        
        RHS rhs = new RHS();
        Iterator<String> it = bindings.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String value = bindings.get(key);
            form.theFormula = form.theFormula.replaceAll("\\"+key,value);
            //System.out.println("INFO in RHS.applyBindings(): " + form.theFormula);
        }
        rhs.form = form;
        return rhs;
    }
    
    /** *************************************************************
     * A test method
     */
    public static void main (String args[]) {
    }
}
