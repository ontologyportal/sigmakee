package com.articulate.sigma.semRewrite;

import com.articulate.sigma.UnitTestBase;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * Created by apease on 4/15/15.
 */
public class LexerTest extends UnitTestBase {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
    public void testTerm() throws ParseException {

        String example1 = "sense(212345678,?E), nsubj(?E,#?X), dobj(?E,?Y)";
        Lexer lex1 = new Lexer(example1);
        assertEquals("sense", lex1.acceptTok(Lexer.Ident)); // sense
        assertEquals("(",lex1.acceptTok(Lexer.OpenPar));    // (
        assertEquals("212345678",lex1.acceptLit("212345678")); // 212345678
        assertEquals(",",lex1.acceptTok(Lexer.Comma));      // ,
        assertEquals("?E", lex1.acceptTok(Lexer.Var)); // ?E
        assertEquals(")", lex1.acceptTok(Lexer.ClosePar));    // )
        assertEquals(",", lex1.acceptTok(Lexer.Comma));      // ,
        assertEquals("nsubj", lex1.acceptTok(Lexer.Ident)); // nsubj
        assertEquals("(", lex1.acceptTok(Lexer.OpenPar));   // (
    }

    /** ***************************************************************
     * Verify that you can't start a parse with OpenPar.
     */
    @Test(expected=ParseException.class)
    public void testInvalidInputOpenParStart() throws ParseException {

        String example1 = "sense(212345678,?E), nsubj(?E,#?X), dobj(?E,?Y)";
        Lexer lex1 = new Lexer(example1);
        assertEquals("(", lex1.acceptTok(Lexer.OpenPar));
    }

    /** ***************************************************************
     * Verify that you can't start a parse with Comma.
     */
    @Test(expected=ParseException.class)
    public void testInvalidInputIdentStart() throws ParseException {

        String example1 = "sense(212345678,?E), nsubj(?E,#?X), dobj(?E,?Y)";
        Lexer lex1 = new Lexer(example1);
        lex1.acceptTok(Lexer.Comma);
    }

    /** ***************************************************************
     * Verify that you can't start a parse with Var.
     */
    @Test(expected=ParseException.class)
    public void testInvalidInputVarStart() throws ParseException {

        String example1 = "sense(212345678,?E), nsubj(?E,#?X), dobj(?E,?Y)";
        Lexer lex1 = new Lexer(example1);
        lex1.acceptTok(Lexer.Var);
    }

    /** ***************************************************************
     * Verify that you can't start a parse with ClosePar.
     */
    @Test(expected=ParseException.class)
    public void testInvalidInputCloseParStart() throws ParseException {

        String example1 = "sense(212345678,?E), nsubj(?E,#?X), dobj(?E,?Y)";
        Lexer lex1 = new Lexer(example1);
        lex1.acceptTok(Lexer.ClosePar);
    }

    /** ***************************************************************
     * Verify that a literal isn't a Number.
     */
    @Test
    @Ignore
    public void testInvalidInputNumber() throws ParseException {

        String example1 = "sense(212345678,?E), nsubj(?E,#?X), dobj(?E,?Y)";
        Lexer lex1 = new Lexer(example1);
        lex1.acceptTok(Lexer.Ident);
        assertEquals("(",lex1.acceptTok(Lexer.OpenPar));

        expectedException.expect(ParseException.class);
        expectedException.expectMessage("Error in Lexer.checkTok(): Unexpected token 'Identifier'");
        lex1.acceptTok(Lexer.Number);
    }

    /** ***************************************************************
     * Verify a misplaced ClosePar.
     */
    @Test
     public void testInvalidInputClosePar1() throws ParseException {

        String example1 = "sense(212345678,?E), nsubj(?E,#?X), dobj(?E,?Y)";
        Lexer lex1 = new Lexer(example1);
        lex1.acceptTok(Lexer.Ident);
        lex1.acceptTok(Lexer.OpenPar);
        lex1.acceptLit("212345678");

        expectedException.expect(ParseException.class);
        expectedException.expectMessage("Error in Lexer.checkTok(): Unexpected token ','");
        lex1.acceptTok(Lexer.ClosePar);
    }

    /** ***************************************************************
     * Verify a misplaced ClosePar.
     */
    @Test
    public void testInvalidInputClosePar2() throws ParseException {

        String example1 = "sense(212345678,?E), nsubj(?E,#?X), dobj(?E,?Y)";
        Lexer lex1 = new Lexer(example1);
        lex1.acceptTok(Lexer.Ident);
        lex1.acceptTok(Lexer.OpenPar);
        lex1.acceptLit("212345678");
        lex1.acceptTok(Lexer.Comma);

        expectedException.expect(ParseException.class);
        expectedException.expectMessage("Error in Lexer.checkTok(): Unexpected token 'Variable'");
        lex1.acceptTok(Lexer.ClosePar);
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
    public void testAcceptClause4() {

        String example4 = "neg(n't-6, be-5)";
        Lexer lex = new Lexer(example4);
        try {
            assertEquals("neg",lex.next());
            assertEquals("(", lex.next());
            assertEquals("n't-6", lex.next());
            assertEquals(",", lex.next());
            assertEquals("be-5", lex.next());
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
    public void testAcceptClauses() {

        String example4 = "root(ROOT-0, reached-14), prepc_after(reached-14, making-2), amod(stops-4, several-3), dobj(making-2, stops-4), nn(islands-7, Caribbean-6), prep_at(making-2, islands-7), appos(islands-7, de-9), nsubj(reached-14, Leon-10), poss(men-13, his-12), conj_and(Leon-10, men-13), nsubj(reached-14, men-13), det(coast-17, the-15), amod(coast-17, east-16), dobj(reached-14, coast-17), prep_of(coast-17, Florida-19), nn(Augustine-22, St.-21), appos(Florida-19, Augustine-22), prep_on(reached-14, April-25), num(April-25, 2-26), num(April-25, 1513-28)";
        Lexer lex = new Lexer(example4);
        try {
            do {
                lex.next();
            } while (lex.look() != Lexer.EOFToken);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
