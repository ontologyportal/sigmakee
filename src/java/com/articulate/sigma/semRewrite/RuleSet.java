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

import com.articulate.sigma.StreamTokenizer_s;

public class RuleSet {

    public ArrayList<Rule> rules = new ArrayList<Rule>();
    public ArrayList<String> warningSet = new ArrayList<String>();
    public static String filename = "";
    
    /** ***************************************************************
     */
    public String toString() {   
        
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < rules.size(); i++) 
            sb.append(rules.get(i).toString());
        return sb.toString();
    }

    /** ***************************************************************
     */
    public RuleSet parse(Lexer lex) {
        
        RuleSet rs = new RuleSet();
        String errStart = "Parsing error in " + filename;
        String errStr = null;
        boolean isEOL = false;
        try {
            do {
                Rule r = Rule.parse(lex);
                if (r != null)
                    rs.rules.add(r);
            } while (!lex.testTok(Lexer.EOFToken));
        }
        catch (Exception ex) {
            String message = ex.getMessage();
            warningSet.add("Error in RuleSet.parse() " + message);
            ex.printStackTrace();
        }
        return rs;
    }
    
    /** ***************************************************************
     */
    public static RuleSet readFile(String fname) throws Exception {

        filename = fname;
        FileReader fr = null;
        Exception exThr = null;
        try {
            File file = new File(fname);
            Lexer lex = new Lexer(file);
            RuleSet rs = new RuleSet();
            return rs.parse(lex);
        }
        catch (Exception ex) {
            exThr = ex;
            String er = ex.getMessage() + ((ex instanceof ParseException)
                                           ? " at line " + ((ParseException)ex).getErrorOffset()
                                           : "");
            System.out.println("Error in RuleSet.readFile(): " + er + " in file " + fname);
        }
        finally {
            if (fr != null) {
                try {
                    fr.close();
                }
                catch (Exception ex2) {
                }
            }
        }
        if (exThr != null) 
            throw exThr;        
        return null;
    }
    
    /** *************************************************************
     * A test method
     */
    public static CNF testRuleSet() {
        
        String rule = "sense(212345678,?E), nsubj(?E,?X), dobj(?E,?Y) ==> " +
                "{(exists (?X ?E ?Y) " + 
                  "(and " +
                    "(instance ?X Organization) " +
                    "(instance ?Y Human)" +
                    "(instance ?E Hiring)" +
                    "(agent ?E ?X) " +
                    "(patient ?E ?Y)))}.";
        Rule r = new Rule();
        r = Rule.parseString(rule);
        //System.out.println(r.toString());
        return Clausifier.clausify(r.lhs);
    }
    
    /** *************************************************************
     * A test method
     */
    public static void main (String args[]) {
        
        String rule = "sense(212345678,?E), nsubj(?E,?X), dobj(?E,?Y) ==> " +
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
        Clausifier.clausify(r.lhs);
    }
}
