package com.articulate.sigma.semRewrite;

import com.articulate.sigma.UnitTestBase;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by apease on 4/15/15.
 */
public class LexerTest extends UnitTestBase {

    /****************************************************************
     * Test that comments and whitespace are normally ignored.
     */
    @Test
    public void testLex() {

        String example1 = "sense(212345678,?E), nsubj(?E,?X), dobj(?E,?Y) ==> " +
                "{(exists (?X ?E ?Y) " +
                "(and " +
                "(instance ?X Organization) " +
                "(instance ?Y Human)}" +
                "(instance ?E Hiring)" +
                "(agent ?E ?X) " +
                "(patient ?E ?Y)))}.";

        String example2 = "bank2";
        String example3 = "at*";
        String example4 = "num(PM-6, 8:30-5)";
        String example5 = "name(John-6, \"John\")";

        Lexer lex1 = new Lexer(example1);
        Lexer lex2 = new Lexer(example2);
        try {
            ArrayList<String> res1 = lex1.lex();
            ArrayList<String> res2 = lex2.lex();
        }
        catch (ParseException e) {
            e.printStackTrace();
            assertFalse(false);
        }
        //assertEquals(example2, example2);
    }

    /****************************************************************
     * Test that comments and whitespace are normally ignored.
     */
    @Test
    public void testLex2() {

        String example1 = "sense(212345678,?E), nsubj(?E,#?X), dobj(?E,?Y) ==> " +
                "{(exists (?X ?E ?Y) " +
                "(and " +
                "(instance ?X Organization) " +
                "(instance ?Y Human)}" +
                "(instance ?E Hiring)" +
                "(agent ?E ?X) " +
                "(patient ?E ?Y)))}.";

        String example2 = "bank2";
        String example4 = "num(PM-6, 8:30-5)";
        String example5 = "name(John-6, \"John\")";

        Lexer lex1 = new Lexer(example1);
        Lexer lex2 = new Lexer(example2);
        try {
            ArrayList<String> res1 = lex1.lexTypes();
            ArrayList<String> res2 = lex2.lexTypes();
            System.out.println("INFO in Lexer.testLex(): completed parsing example 1: " + res1);
            System.out.println("INFO in Lexer.testLex(): completed parsing example 1: " + res2);
            assertThat(res1, hasItems(Lexer.Directive));
            assertThat(res1,hasItems(Lexer.Ident));
            assertThat(res1,hasItems(Lexer.Var));
        }
        catch (ParseException e) {
            e.printStackTrace();
            assertFalse(false);
        }
        //assertEquals(example2, example2);
    }

    /** ***************************************************************
     * Test accepTok()
     */
    @Test
    public void testString() {

        String example3 = "at*";
        Lexer lex1 = new Lexer(example3);
        try {
            assertEquals("at*", lex1.acceptTok(Lexer.Ident));
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     * Test that self.example 1 is split into the expected tokens.
     */
    @Test
    public void testTerm() {

        String example1 = "sense(212345678,?E), nsubj(?E,#?X), dobj(?E,?Y)";
        Lexer lex1 = new Lexer(example1);
        try {
            assertEquals("sense", lex1.acceptTok(Lexer.Ident)); // sense
            assertEquals("(",lex1.acceptTok(Lexer.OpenPar));    // (
            assertEquals("212345678",lex1.acceptTok(Lexer.Number)); // 212345678
            assertEquals(",",lex1.acceptTok(Lexer.Comma));      // ,
            assertEquals("?E", lex1.acceptTok(Lexer.Var)); // ?E
            assertEquals(")", lex1.acceptTok(Lexer.ClosePar));    // )
            assertEquals(",", lex1.acceptTok(Lexer.Comma));      // ,
            assertEquals("nsubj", lex1.acceptTok(Lexer.Ident)); // nsubj
            assertEquals("(", lex1.acceptTok(Lexer.OpenPar));   // (
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     * Check the positive case of AcceptLit().
     */
    @Test
    public void testAcceptLit() {

        String example1 = "sense(212345678,?E), nsubj(?E,#?X), dobj(?E,?Y)";
        Lexer lex = new Lexer(example1);
        try {
            assertEquals("sense",lex.acceptLit("sense"));
            assertEquals("(",lex.acceptLit("("));
            assertEquals("212345678",lex.acceptLit("212345678"));
            assertEquals(",",lex.acceptLit(","));
            assertEquals("?E",lex.acceptLit("?E"));
            assertEquals(")",lex.acceptLit(")"));
            assertEquals(",",lex.acceptLit(","));
            assertEquals("nsubj",lex.acceptLit("nsubj"));
            assertEquals("(",lex.acceptLit("("));
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     * Check the positive case of AcceptLit().
     */
    @Test
    public void testAcceptClause() {

        String example4 = "num(PM-6, 8:30-5)";
        Lexer lex = new Lexer(example4);
        try {
            Pattern value = Lexer.tokenDefs.get(Lexer.Number);
            Matcher m = value.matcher("8:30");
            assertTrue(m.lookingAt());
            assertEquals("num", lex.next());
            assertEquals("(", lex.next());
            assertEquals("PM-6", lex.next());
            assertEquals(",", lex.next());
            assertEquals("8:30-5", lex.next());
            assertEquals(")", lex.next());
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     * Check the positive case of AcceptLit().
     */
    @Test
    public void testAcceptClause3() {

        String example4 = "num(PM-6, 8:30-5)";
        Lexer lex = new Lexer(example4);
        try {
            Pattern value = Lexer.tokenDefs.get(Lexer.Ident);
            Matcher m = value.matcher("5th-6");
            assertTrue(m.lookingAt());
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     * Check the positive case of AcceptLit().
     */
    @Test
    public void testAcceptClause2() {

        String example5 = "name(John-6, \"John\")";
        Lexer lex = new Lexer(example5);
        try {
            assertEquals("name",lex.next());
            assertEquals("(", lex.next());
            assertEquals("John-6", lex.next());
            assertEquals(",", lex.next());
            assertEquals("\"John\"", lex.next());
            assertEquals(")", lex.next());
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

}
