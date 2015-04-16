package com.articulate.sigma.semRewrite;

/*
Author: Adam Pease adam.pease@ipsoft.com
        Stephan Schulz 

A simple lexical analyser that converts a string into a sequence of
tokens.  Java's StreamTokenizer can't be used since it only can
"push back" one token.
     
This will convert a string into a sequence of
tokens that can be inspected and processed in-order. It is a bit
of an overkill for a simple application, but makes actual
parsing later much easier and more robust than a quicker hack.

Initialize the Lexer with a String or a filename then
iterate through the tokens with next() or testTok() to check
for expected token types and error if it's not an expected type.

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
import java.util.*;
import java.util.regex.*;
import java.text.*;

import com.articulate.sigma.KBmanager;

public class Lexer {
       
    public static final String NoToken        = "No Token";
    public static final String WhiteSpace     = "White Space";
    public static final String Newline        = "Newline";
    public static final String SemiComment    = "SemiComment";
    public static final String Directive      = "Directive";
    public static final String Ident          = "Identifier";
    public static final String Number         = "Positive or negative Integer or real";
    public static final String QuotedString   = "Quoted string";   
    public static final String FullStop       = ". (full stop)";
    public static final String OpenPar        = "(";
    public static final String ClosePar       = ")";
    public static final String OpenBracket    = "{";
    public static final String CloseBracket   = "}";

    public static final String Or             = "|";
    public static final String Plus           = "+";
    public static final String Comma          = ",";
    public static final String Implies        = "==>";   
    public static final String OptImplies     = "?=>";    
    public static final String Clause         = "/-";
    public static final String Var            = "Variable";
    public static final String Negation       = "-";
    public static final String Stop           = "stop";
    public static final String Zero           = "!";
    public static final String EOFToken       = "*EOF*";

    public String filename = "";
    public String type = "";
    public String literal = "";
    public String line = null;
    public String SZS = "";
    public int pos = 0;  // character position on the current line
    public LineNumberReader input = null;
    public ArrayDeque<String> tokenStack = new ArrayDeque<String>();

    /** This array contains all of the compiled Pattern objects that
     * will be used by methods in this file. */
    public static LinkedHashMap<String,Pattern> tokenDefs = new LinkedHashMap<String,Pattern>();
    
    public static ArrayList<String> andOr = new ArrayList<String>();
    public static ArrayList<String> binaryRel = new ArrayList<String>();
    public static ArrayList<String> quant = new ArrayList<String>(); 
    
    /** ***************************************************************
     */
    public Lexer() {
        init();
    }
    
    /** ***************************************************************
     */
    public Lexer(String s) {
        
        init();
        //source = s;
        input = new LineNumberReader(new StringReader(s));
        filename = "";
    }
  
    /** ***************************************************************
     * Read a text file into the "input" String variables.  Throws an
     * error on file not found.
     */
    public Lexer(File f) {
        
        init();
        //source = file2string(f);
        try {
            input = new LineNumberReader(new FileReader(f));
        }
        catch (FileNotFoundException fnf) {
            System.out.println("Error in Lexer(): File not found: " + f);
            System.out.println(fnf.getMessage());
            fnf.printStackTrace();
        }
    }
    
    /** ***************************************************************
     * Read the contents of a text file into a String.  Throws IOException
     */
    public String file2string(File f) {

        String result = null;
        DataInputStream in = null;

        try {
            byte[] buffer = new byte[(int) f.length()];
            in = new DataInputStream(new FileInputStream(f));
            in.readFully(buffer);
            result = new String(buffer);
        } 
        catch (IOException e) {
            throw new RuntimeException("IO problem in fileToString", e);
        } 
        finally {
            try {
                in.close();
            } 
            catch (IOException e) { /* ignore it */
            }
        }
        return result;
    }
    
    /** ***************************************************************
     * @return the line number of the token by counting all the
     * newlines in the position up to the current token.
     */
    public int linepos() {

        return input.getLineNumber();
        //return source.substring(0,pos).split(" ").length + 1;
    }        

    /** ***************************************************************
     * Set up the regular expressions to recognize each token type.
     */
    private static void init() {
        
        tokenDefs.put(FullStop,     Pattern.compile("\\."));                   
        tokenDefs.put(OpenPar,      Pattern.compile("\\("));                   
        tokenDefs.put(ClosePar,     Pattern.compile("\\)"));      
        tokenDefs.put(OpenBracket,  Pattern.compile("\\{"));                   
        tokenDefs.put(CloseBracket, Pattern.compile("\\}"));   
        tokenDefs.put(Comma,        Pattern.compile(","));                                   
        tokenDefs.put(Or,           Pattern.compile("\\|"));                                                 
        tokenDefs.put(Implies,      Pattern.compile("==>"));  
        tokenDefs.put(OptImplies,   Pattern.compile("\\?=>"));
        tokenDefs.put(Clause,       Pattern.compile("/-"));
        tokenDefs.put(Negation,     Pattern.compile("-"));    
        tokenDefs.put(Plus,         Pattern.compile("\\+"));
        tokenDefs.put(Var,          Pattern.compile("\\?[a-zA-Z][_a-z0-9_A-Z]*\\*?"));
        tokenDefs.put(Newline,      Pattern.compile("\\n"));
        tokenDefs.put(WhiteSpace,   Pattern.compile("\\s+"));
        tokenDefs.put(Ident,        Pattern.compile("\\\"?\\'?[0-9a-zA-Z][_\\-a-z0-9_A-Z ]+\\*?\\\"?"));
        tokenDefs.put(Number,       Pattern.compile("-?[0-9]?[0-9\\.]+\\:?E?-?[0-9]*-?[0-9]*"));
        tokenDefs.put(Zero,         Pattern.compile("\\!"));
        tokenDefs.put(Stop,         Pattern.compile("stop"));
        tokenDefs.put(SemiComment,  Pattern.compile(";[^\\n]*"));
        tokenDefs.put(Directive,    Pattern.compile("#[^\\n]*"));
        tokenDefs.put(QuotedString, Pattern.compile("'[^']*'"));
        
        andOr.add(Comma);
        andOr.add(Or);
        
        binaryRel.add(Implies);
        binaryRel.add(OptImplies);  
    }
    
    /** ***************************************************************
     * @return the next token type without consuming it.
     */
    public String lookType() throws ParseException {

        look();
        return type;
    }

    /** ***************************************************************
     * @return the next token without consuming it.
     */
    public String look() throws ParseException {

        String res = next();
        //System.out.println("INFO in Lexer.look(): " + res);
        tokenStack.push(res);
        return res;
    }

    /** ***************************************************************
     * @return the literal value of the next token, i.e. the string
     * generating the token.
     */
    public String lookLit() throws ParseException {

        look();
        return literal;
    }
            
    /** ***************************************************************
     * Take a list of expected token types. 
     * @return True if the next token is expected, False otherwise.
     */
    public boolean testTok(ArrayList<String> tokens) throws ParseException {

        look();
        for (int i = 0; i < tokens.size(); i++) {
            if (type.equals(tokens.get(i))) {
                //System.out.println("INFO in Lexer.testTok(): found token");
                return true;
            }
        }
        //System.out.println("INFO in Lexer.testTok(): didn't find tokens with type: " + type + " for list " + tokens);
        return false;
    }

    /** ***************************************************************
     * Convenience method
     */
    public boolean testTok(String tok) throws ParseException {

        ArrayList<String> tokens = new ArrayList<String>();
        tokens.add(tok);
        return testTok(tokens);
    }

    /** ***************************************************************
     * Take a list of expected token types. If the next token is
     * not among the expected ones, exit with an error. Otherwise do
     * nothing. 
     */
    public void checkTok(String tok) throws ParseException {

        ArrayList<String> tokens = new ArrayList<String>();
        tokens.add(tok);
        checkTok(tokens);
    }

    /** ***************************************************************
     * Take a list of expected token types. If the next token is
     * not among the expected ones, exit with an error. Otherwise do
     * nothing. 
     */
    public void checkTok(ArrayList<String> tokens) throws ParseException {

        look();
        for (int i = 0; i < tokens.size(); i++) {
            if (type.equals(tokens.get(i)))
                return;
        }
        throw new ParseException("Error in Lexer.checkTok(): Unexpected token '" + type + "'",linepos());
    }

    /** ***************************************************************
     * Take an expected token type. If the next token is
     * the same as the expected one, consume and return it. Otherwise, exit 
     * with an error. 
     * @return the token matching the type of the input
     */
    public String acceptTok(String token) throws ParseException {

        ArrayList<String> tokens = new ArrayList<String>();
        tokens.add(token);
        checkTok(tokens);
        return next();
    }

    /** ***************************************************************
     * Take a list of expected token types. If the next token is
     * among the expected ones, consume and return it. Otherwise, exit 
     * with an error. 
     * @return the token matching one of the types in the inputs
     */
    public String acceptTok(ArrayList<String> tokens) throws ParseException {

        checkTok(tokens);
        return next();
    }

    /** ***************************************************************
     * @param litval an expected literal string. 
     * @return True if the
     * next token's string value the same as the input, False otherwise.
     */
    public boolean testLit(String litval) throws ParseException {

        ArrayList<String> litvals = new ArrayList<String>();
        litvals.add(litval);
        return testLit(litvals);
    }
    
    /** ***************************************************************
     * @param litvals a list of expected literal strings
     * @return True if the next token's string value is among the input
     * string and false otherwise. 
     */
    public boolean testLit(ArrayList<String> litvals) throws ParseException {

        lookLit();
        for (int i = 0; i < litvals.size(); i++) {
            if (literal.equals(litvals.get(i)))
                return true;
        }
        return false;
    }
    
    /** ***************************************************************
     * Take an expected literal string. If the next token's
     * literal is not the expected one, exit with an
     * error. Otherwise do nothing. 
     */
    private void checkLit(String litval) throws ParseException {

        ArrayList<String> litvals = new ArrayList<String>();
        litvals.add(litval);
        checkLit(litvals);
    }

    /** ***************************************************************
     * Take a list of expected literal strings. If the next token's
     * literal is not among the expected ones, exit with an
     * error. Otherwise do nothing. 
     */
    private void checkLit(ArrayList<String> litvals) throws ParseException {

        if (!testLit(litvals)) {
            look();
            throw new ParseException("Error in Lexer.checkLit(): " + literal + " not in " + litvals, linepos());
        }
    }

    /** ***************************************************************
     * Take a list of expected literal strings. If the next token's
     * literal is among the expected ones, consume and return the
     * literal. Otherwise, exit with an error. 
     */
    public String acceptLit(ArrayList<String> litvals) throws ParseException {

        checkLit(litvals);
        return next();
    }
    
    /** ***************************************************************
     * Take a list of expected literal strings. If the next token's
     * literal is among the expected ones, consume and return the
     * literal. Otherwise, exit with an error. 
     */
    public String acceptLit(String litval) throws ParseException {

        ArrayList<String> litvals = new ArrayList<String>();
        litvals.add(litval);
        checkLit(litvals);
        return next();
    }

    /** ***************************************************************
     * @return next semantically relevant token (not whitespace, 
     * comments etc)
     */
    public String next() throws ParseException {

        String res = nextUnfiltered();
        while ((type.equals(WhiteSpace) || type.equals(SemiComment)) &&
                !res.equals(EOFToken)) {
            //System.out.println(type + ":" + line);
            res = nextUnfiltered();
        }
        //System.out.println("INFO in next(): returning token: " + res);
        return res;
    }
    
    /** ***************************************************************
     * @return next token, including tokens, such as whitespace and
     * comments, that are ignored by most languages. 
     */
    public String nextUnfiltered() throws ParseException {

        //System.out.println("INFO in Lexer.nextUnfiltered(): " + line);
        if (tokenStack.size() > 0)
            return tokenStack.pop();
        else {
            if (line == null || line.length() <= pos) {
                try {
                    do {
                        line = input.readLine();
                    } while (line != null && line.length() == 0);    
                    //System.out.println("INFO in Lexer.nextUnfiltered(): " + line);
                    pos = 0;
                }
                catch (IOException ioe) {
                    System.out.println("Error in Lexer.nextUnfiltered()");
                    System.out.println(ioe.getMessage());
                    ioe.printStackTrace();
                    return EOFToken;
                }
                if (line == null) {
                    //System.out.println("INFO in Lexer.nextUnfiltered(): returning eof");
                    type = EOFToken;
                    return EOFToken;
                }
            }
            Iterator<String> it = tokenDefs.keySet().iterator();
            while (it.hasNext()) {  // Go through all the token definitions and process the first one that matches
                String key = it.next();
                Pattern value = tokenDefs.get(key);
                Matcher m = value.matcher(line.substring(pos));
                //System.out.println("INFO in Lexer.nextUnfiltered(): checking: " + key + " against: " + line.substring(pos));
                if (m.lookingAt()) {
                    //System.out.println("INFO in Lexer.nextUnfiltered(): got token against source: " + line.substring(pos));
                    literal = line.substring(pos + m.start(),pos + m.end());
                    pos = pos + m.end();
                    type = key;
                    //System.out.println("INFO in Lexer.nextUnfiltered(): got token: " + literal + " type: " + type + 
                    //        " at pos: " + pos + " with regex: " + value);
                    return m.group();
                }
            }
            if (pos + 4 > line.length())
                if (pos - 4 < 0)
                    throw new ParseException("Error in Lexer.nextUnfiltered(): no matches in token list for " + 
                            line.substring(0,line.length()) + "... at line " + input.getLineNumber(),pos);
                else
                    throw new ParseException("Error in Lexer.nextUnfiltered(): no matches in token list for " + 
                            line.substring(pos - 4,line.length()) + "... at line " + input.getLineNumber(),pos);
            else
                throw new ParseException("Error in Lexer.nextUnfiltered(): no matches in token list for " + 
                        line.substring(pos,pos+4) + "... at line " + input.getLineNumber(),pos);
        }
    }

    /** ***************************************************************
     * Return a list of all tokens in the source. 
     */
    public ArrayList<String> lex() throws ParseException {

        ArrayList<String> res = new ArrayList<String>();
        while (!testTok(EOFToken)) {
            String tok = next();
            //System.out.println("INFO in Lexer.lex(): " + tok);
            res.add(tok);
        }
        return res;
    }

    /** ***************************************************************
     * Return a list of all tokens in the source. 
     */
    public ArrayList<String> lexTypes() throws ParseException {

        ArrayList<String> res = new ArrayList<String>();
        while (!testTok(EOFToken)) {
            String type = lookType();
            String tok = next();
            //System.out.println("INFO in Lexer.lex(): " + type);
            res.add(type);
        }
        return res;
    }
    
    /** ***************************************************************
     ** ***************************************************************
     */
    private static String example1 = "sense(212345678,?E), nsubj(?E,?X), dobj(?E,?Y) ==> " +
            "{(exists (?X ?E ?Y) " + 
              "(and " +
                "(instance ?X Organization) " +
                "(instance ?Y Human)}" +
                "(instance ?E Hiring)" +
                "(agent ?E ?X) " +
                "(patient ?E ?Y)))}.";
    
    private static String example2 = "bank2";
    private static String example3 = "at*";
    private static String example4 = "num(PM-6, 8:30-5)";
    private static String example5 = "name(John-6, \"John\")";
    
    /** ***************************************************************
     * Test that comments and whitespace are normally ignored. 
     */
    private static void testLex() {

        System.out.println("-------------------------------------------------");
        System.out.println("INFO in Lexer.testLex(): example2: " + example2);
        Lexer lex1 = new Lexer(example1);
        Lexer lex2 = new Lexer(example2);
        try {
            ArrayList<String> res1 = lex1.lex();
            System.out.println("INFO in Lexer.testLex(): completed parsing example 1: " + example1);
            ArrayList<String> res2 = lex2.lex();
            System.out.println("INFO in Lexer.testLex(): completed parsing example 1: " + example2);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    /** ***************************************************************
     * Test accepTok()
     */
    private static void testString() {

        System.out.println("-------------------------------------------------");
        System.out.println("INFO in Lexer.testString()");
        Lexer lex1 = new Lexer(example3);
        try {
            System.out.println(lex1.acceptTok(Ident)); 
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    /** ***************************************************************
     * Test that self.example 1 is split into the expected tokens. 
     */
    private static void testTerm() {

        System.out.println("-------------------------------------------------");
        System.out.println("INFO in Lexer.testTerm()");
        Lexer lex1 = new Lexer(example1);
        try {
            lex1.acceptTok(Ident); // sense
            lex1.acceptTok(OpenPar);    // (
            lex1.acceptTok(Number); // 212345678
            lex1.acceptTok(Comma);      // ,
            lex1.acceptTok(Var); // ?E
            lex1.acceptTok(ClosePar);    // )
            lex1.acceptTok(Comma);      // ,
            lex1.acceptTok(Ident); // nsubj
            lex1.acceptTok(OpenPar);   // (
            // ...
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     * Do a deep compare to two ArrayList<String> for equality.
     */
    private static boolean compareArrays(ArrayList<String> s1, ArrayList<String> s2) {
        
        if (s1.size() != s2.size())
            return false;
        for (int i = 0; i < s1.size(); i++) 
            if (!s1.get(i).equals(s2.get(i)))
                return false;
        return true;
    }
    
    /** ***************************************************************
     * Check the positive case of AcceptLit(). 
     */
    private static void testAcceptLit() {

        System.out.println("-------------------------------------------------");
        System.out.println("INFO in Lexer.testAcceptLit()");
        Lexer lex = new Lexer(example1);
        try {
            lex.acceptLit("sense");
            lex.acceptLit("(");
            lex.acceptLit("212345678");
            lex.acceptLit(",");
            lex.acceptLit("?E");
            lex.acceptLit(")");
            lex.acceptLit(",");
            lex.acceptLit("nsubj");
            lex.acceptLit("(");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    /** ***************************************************************
     * Provoke different errors. 
     */
    private static void testErrors() {

        System.out.println("-------------------------------------------------");
        System.out.println("INFO in Lexer.testErrors(): Should throw three errors");
        Lexer lex = null;
        try {
            lex = new Lexer(example1);
            lex.look(); 
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        try {
            lex = new Lexer(example1);
            lex.checkTok(Implies); 
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        try {
            lex = new Lexer(example1);
            lex.checkLit("abc");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    /** ***************************************************************
     * Check the positive case of AcceptLit(). 
     */
    private static void testAcceptClause() {

        System.out.println("-------------------------------------------------");
        System.out.println("INFO in Lexer.testAcceptClause()");
        Lexer lex = new Lexer(example4);
        try {
            
            Pattern value = tokenDefs.get(Number);
            Matcher m = value.matcher("8:30");
            if (m.lookingAt()) {
                System.out.println("parse ok");
            }
            System.out.println(lex.next());
            System.out.println(lex.next());
            System.out.println(lex.next());
            System.out.println(lex.next());
            System.out.println(lex.next());
            System.out.println(lex.next());
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     * Check the positive case of AcceptLit().
     */
    private static void testAcceptClause3() {

        System.out.println("-------------------------------------------------");
        System.out.println("INFO in Lexer.testAcceptClause3()");
        Lexer lex = new Lexer(example4);
        try {

            Pattern value = tokenDefs.get(Ident);
            Matcher m = value.matcher("5th-6");
            if (m.lookingAt()) {
                System.out.println("parse ok");
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     * Check the positive case of AcceptLit(). 
     */
    private static void testAcceptClause2() {

        System.out.println("-------------------------------------------------");
        System.out.println("INFO in Lexer.testAcceptClause()");
        Lexer lex = new Lexer(example5);
        try {
            System.out.println(lex.next());
            System.out.println(lex.next());
            System.out.println(lex.next());
            System.out.println(lex.next());
            System.out.println(lex.next());
            System.out.println(lex.next());
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    /** ***************************************************************
     */
    public static void main(String[] args) {
        
        System.out.println("INFO in Lexer.main()");
        Interpreter interp = new Interpreter();
        if (args != null && args.length > 1 && args[0].equals("-s")) {
            Lexer lex = new Lexer(args[1]);
            try {
                System.out.println(lex.lex());
                lex = new Lexer(args[1]);
                System.out.println(lex.lexTypes());
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }        
        }
        else if (args != null && args.length > 0 && args[0].equals("-h")) {
            System.out.println("Semantic Rewriting with SUMO, Sigma and E");
            System.out.println("  options:");
            System.out.println("  -h - show this help screen");
            System.out.println("  -s - runs one conversion of one quoted input");
            System.out.println("  with no options this falls through to some tests.");
        }
        else {
            //testString();
            //testLex();
            //testTerm();
            //testAcceptLit();
            //testErrors();
            testAcceptClause3();
        }
    }
}
