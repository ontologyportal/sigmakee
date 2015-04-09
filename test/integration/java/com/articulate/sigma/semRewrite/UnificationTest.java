package com.articulate.sigma.semRewrite;

import com.articulate.sigma.IntegrationTestBase;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Created by areed on 3/30/15.
 */
public class UnificationTest extends IntegrationTestBase {

    /** *************************************************************
     */
    @Test
    public void testUnifyWhatDoesMaryDo() {

        Interpreter interp = new Interpreter();
        interp.initialize();

        String input = "root(ROOT-0,do-4), dobj(do-4,what-1), aux(do-4,do-2), nsubj(do-4,Mary-3), sumo(IntentionalProcess,do-2), names(Mary-3,\"Mary\"), attribute(Mary-3,Female), sumo(Human,Mary-3), number(SINGULAR,Mary-3)";
        Lexer lex = new Lexer(input);
        CNF cnfInput = CNF.parseSimple(lex);
        ArrayList<CNF> inputs = new ArrayList<CNF>();
        inputs.add(cnfInput);

        String[] expected = {
                "(agent ?X Mary-3)",
                "(attribute Mary-3 Female)",
                "(names Mary-3 \"Mary\")",
                "(instance Mary-3 Human)"
        };

        List<String> results = interp.interpretCNF(inputs);
        assertThat(results, hasItems(expected));
    }

    /** *************************************************************
     */
    @Test
    public void testUnifyWhatDoesMaryKick() {

        Interpreter interp = new Interpreter();
        interp.initialize();

        String input = "root(ROOT-0,kick-2), nsubj(kick-2,Mary-1), det(cart-4,the-3), dobj(kick-2,cart-4), names(Mary-1,\"Mary\"), sumo(Wagon,cart-4), sumo(Kicking,kick-2), attribute(Mary-1,Female), sumo(Human,Mary-1), number(SINGULAR,Mary-1), tense(PAST,kick-2), number(SINGULAR,cart-4)";
        Lexer lex = new Lexer(input);
        CNF cnfInput = CNF.parseSimple(lex);
        ArrayList<CNF> inputs = new ArrayList<CNF>();
        inputs.add(cnfInput);

        // Mary kicks a cart.
        String[] expected = {
                "(attribute Mary-1 Female)",
                "(instance cart-4 Wagon)",
                "(names Mary-1 \"Mary\")",
                "(patient kick-2 cart-4)",
                "(instance kick-2 Kicking)",
                "(instance Mary-1 Human)",
                "(agent kick-2 Mary-1)"
        };

        List<String> results = interp.interpretCNF(inputs);
        assertThat(results, hasItems(expected));
    }

    /** *************************************************************
     */
    @Test
    public void testUnifyWasAmeliaMaryEarhartAFemale() {
        Interpreter interp = new Interpreter();
        interp.initialize();

        String input = "root(ROOT-0,female-6), cop(female-6,be-1), det(female-6,a-5), nsubj(female-6,AmeliaMaryEarhart-2), attribute(AmeliaMaryEarhart-2,Female), sumo(Female,female-6), sumo(Entity,be-1), names(AmeliaMaryEarhart-2,\"Amelia Mary Earhart\"), sumo(Human,AmeliaMaryEarhart-2), tense(PAST,be-1), number(SINGULAR,Amelia-2), number(SINGULAR,Mary-3), number(SINGULAR,Earhart-4), number(SINGULAR,female-6)";
        Lexer lex = new Lexer(input);
        CNF cnfInput = CNF.parseSimple(lex);
        ArrayList<CNF> inputs = new ArrayList<CNF>();
        inputs.add(cnfInput);

        // Was Amelia Mary Earhart a female?
        String[] expected = {
                "(attribute AmeliaMaryEarhart-2 Female)",
                "(names AmeliaMaryEarhart-2 \"Amelia Mary Earhart\")",
                "(instance AmeliaMaryEarhart-2 Human)"
        };

        List<String> results = interp.interpretCNF(inputs);
        assertThat(results, hasItems(expected));
    }

    /** *************************************************************
     */
    @Test
    public void testWhereDidAmeliaMaryEarhartLive() {

        Interpreter interp = new Interpreter();
        interp.initialize();

        String input = "root(ROOT-0, live-6), advmod(live-6, where-1), aux(live-6, do-2), nsubj(live-6, AmeliaMaryEarhart-3), attribute(AmeliaMaryEarhart-3, Female), sumo(IntentionalProcess, do-2), sumo(Living, live-6), names(AmeliaMaryEarhart-3, \"Amelia Mary Earhart\"), sumo(Human, AmeliaMaryEarhart-3), number(SINGULAR, Amelia-3), number(SINGULAR, Mary-4), number(SINGULAR, Earhart-5)";
        Lexer lex = new Lexer(input);
        CNF cnfInput = CNF.parseSimple(lex);
        ArrayList<CNF> inputs = new ArrayList<CNF>();
        inputs.add(cnfInput);

        // Where did Amelia Mary Earhart live?
        String[] expected = {
                "(attribute AmeliaMaryEarhart-3 Female)",
                "(inhabits AmeliaMaryEarhart-3 ?Y)",
                "(names AmeliaMaryEarhart-3 \"Amelia Mary Earhart\")"
        };

        List<String> results = interp.interpretCNF(inputs);
        assertThat(results, hasItems(expected));
    }

    /** *************************************************************
     * When was Amelia Mary Earhart born?
     * advmod(bear*,when-1), auxpass(bear*,be*), nsubjpass(bear*,?HUM), sumo(?C,bear*) ==> {(birthdate ?HUM ?WH)}.
     */
    @Test
    public void testWhenWasAmeliaMaryEarhartBorn() {

        Interpreter interp = new Interpreter();
        interp.initialize();

        String input = "root(ROOT-0,bear-6), advmod(bear-6,when-1), auxpass(bear-6,be-2), nsubjpass(bear-6,AmeliaMaryEarhart-3), attribute(AmeliaMaryEarhart-3,Female), names(AmeliaMaryEarhart-3,\"Amelia Mary Earhart\"), sumo(Human,AmeliaMaryEarhart-3), sumo(Attribute,bear-6), tense(PAST,be-2), number(SINGULAR,Amelia-3), number(SINGULAR,Mary-4), number(SINGULAR,Earhart-5)";
        Lexer lex = new Lexer(input);
        CNF cnfInput = CNF.parseSimple(lex);
        ArrayList<CNF> inputs = new ArrayList<CNF>();
        inputs.add(cnfInput);

        // When was Amelia Mary Earhart born?
        String[] expected = {
                "(birthdate AmeliaMaryEarhart-3 ?WH)",
                "(attribute AmeliaMaryEarhart-3 Female)",
                "(names AmeliaMaryEarhart-3 \"Amelia Mary Earhart\")",
                "(instance AmeliaMaryEarhart-3 Human)"
        };

        List<String> results = interp.interpretCNF(inputs);
        assertThat(results, hasItems(expected));
    }

    /** *************************************************************
     * When did Amelia Mary Earhart die?
     * advmod(die*,when-1), nsubj(die*,?HUM), sumo(?C,die*), sumo(?C2,do*) ==> {(deathdate ?HUM ?WH)}.
     */
    @Test
    public void testWhenDidAmeliaMaryEarhartDie() {

        Interpreter interp = new Interpreter();
        interp.initialize();

        String input = "root(ROOT-0,die-6), advmod(die-6,when-1), aux(die-6,do-2), nsubj(die-6,AmeliaMaryEarhart-3), attribute(AmeliaMaryEarhart-3,Female), sumo(IntentionalProcess,do-2), names(AmeliaMaryEarhart-3,\"Amelia Mary Earhart\"), sumo(Human,AmeliaMaryEarhart-3), sumo(Death,die-6), number(SINGULAR,Amelia-3), number(SINGULAR,Mary-4), number(SINGULAR,Earhart-5)";
        Lexer lex = new Lexer(input);
        CNF cnfInput = CNF.parseSimple(lex);
        ArrayList<CNF> inputs = new ArrayList<CNF>();
        inputs.add(cnfInput);

        // When did Amelia Mary Earhart die?
        String[] expected = {
                "(deathdate AmeliaMaryEarhart-3 ?WH)",
                "(attribute AmeliaMaryEarhart-3 Female)",
                "(names AmeliaMaryEarhart-3 \"Amelia Mary Earhart\")",
                "(instance AmeliaMaryEarhart-3 Human)"
        };

        List<String> results = interp.interpretCNF(inputs);
        assertThat(results, hasItems(expected));
    }

    /** *************************************************************
     * What was Amelia Mary Earhart interested in?
     * prep(interested*,in*), sumo(?C,interested*), nsubj(?R,?S), cop(interested*,be*) ==> {(inScopeOfInterest ?S ?O)}.
     */
    @Test
    public void testWhatWasAmeliaMaryEarhartInterestedIn() {

        Interpreter interp = new Interpreter();
        interp.initialize();

        String input = "root(ROOT-0,interested-6), pobj(interested-6,what-1), cop(interested-6,be-2), prep(interested-6,in-7), nsubj(interested-6,AmeliaMaryEarhart-3), attribute(AmeliaMaryEarhart-3,Female), sumo(inScopeOfInterest,interested-6), names(AmeliaMaryEarhart-3,\"Amelia Mary Earhart\"), sumo(Human,AmeliaMaryEarhart-3), tense(PAST,be-2), number(SINGULAR,Amelia-3), number(SINGULAR,Mary-4), number(SINGULAR,Earhart-5)";
        Lexer lex = new Lexer(input);
        CNF cnfInput = CNF.parseSimple(lex);
        ArrayList<CNF> inputs = new ArrayList<CNF>();
        inputs.add(cnfInput);

        // What was Amelia Mary Earhart interested in?
        String[] expected = {
                "(inScopeOfInterest AmeliaMaryEarhart-3 ?O)",
                "(attribute AmeliaMaryEarhart-3 Female)",
                "(names AmeliaMaryEarhart-3 \"Amelia Mary Earhart\")",
                "(instance AmeliaMaryEarhart-3 Human)"
        };

        List<String> results = interp.interpretCNF(inputs);
        assertThat(results, hasItems(expected));
    }

    /** *************************************************************
     * Was Amelia Mary Earhart interested in airplanes?
     * prep_in(?R,?O), sumo(inScopeOfInterest,interested*), nsubj(?R,?S), cop(interested*,be*) ==> {(inScopeOfInterest ?S ?O)}.
     */
    @Test
    public void testWasAmeliaMaryEarhartInterestedInAirplanes() {

        Interpreter interp = new Interpreter();
        interp.initialize();

        String input = "root(ROOT-0,interested-5), cop(interested-5,be-1), prep_in(interested-5,airplane-7), nsubj(interested-5,AmeliaMaryEarhart-2), sumo(Airplane,airplane-7), attribute(AmeliaMaryEarhart-2,Female), sumo(inScopeOfInterest,interested-5), sumo(Entity,be-1), names(AmeliaMaryEarhart-2,\"Amelia Mary Earhart\"), sumo(Human,AmeliaMaryEarhart-2), tense(PAST,be-1), number(SINGULAR,Amelia-2), number(SINGULAR,Mary-3), number(SINGULAR,Earhart-4), number(PLURAL,airplane-7)";
        Lexer lex = new Lexer(input);
        CNF cnfInput = CNF.parseSimple(lex);
        ArrayList<CNF> inputs = new ArrayList<CNF>();
        inputs.add(cnfInput);

        // Was Amelia Mary Earhart interested in airplanes?
        String[] expected = {
                "(inScopeOfInterest AmeliaMaryEarhart-2 airplane-7)",
                "(attribute AmeliaMaryEarhart-2 Female)",
                "(names AmeliaMaryEarhart-2 \"Amelia Mary Earhart\")",
                "(instance airplane-7 Airplane)",
                "(instance AmeliaMaryEarhart-2 Human)"
        };

        List<String> results = interp.interpretCNF(inputs);
        assertThat(results, hasItems(expected));
    }

    /** *************************************************************
     * Where did she fly?
     * advmod(?X,where*), aux(?X,do*) ==> {(traverses ?X ?WH)}.
     */
    @Test
    public void testWhereDidSheFly() {

        Interpreter interp = new Interpreter();
        interp.initialize();

        String input = "root(ROOT-0,fly-6), advmod(fly-6,where-1), aux(fly-6,do-2), nsubj(fly-6,AmeliaMaryEarhart-3), attribute(AmeliaMaryEarhart-3,Female), sumo(IntentionalProcess,do-2), names(AmeliaMaryEarhart-3,\"Amelia Mary Earhart\"), sumo(Human,AmeliaMaryEarhart-3), sumo(Flying,fly-6), number(SINGULAR,Amelia-3), number(SINGULAR,Mary-4), number(SINGULAR,Earhart-5)";
        Lexer lex = new Lexer(input);
        CNF cnfInput = CNF.parseSimple(lex);
        ArrayList<CNF> inputs = new ArrayList<CNF>();
        inputs.add(cnfInput);

        // Where did she fly?
        String[] expected = {
                "(traverses fly-6 ?WH)",
                "(agent fly-6 AmeliaMaryEarhart-3)",
                "(attribute AmeliaMaryEarhart-3 Female)",
                "(names AmeliaMaryEarhart-3 \"Amelia Mary Earhart\")",
                "(instance AmeliaMaryEarhart-3 Human)",
                "(instance fly-6 Flying)"
        };

        List<String> results = interp.interpretCNF(inputs);
        assertThat(results, hasItems(expected));
    }

    /** *************************************************************
     * What language did Amelia speak?
     * det(?L,what*), dobj(?S,?L), sumo(?LT,?L), isSubclass(?LT,Physical), +sumo(?ST,?S), isSubclass(?ST,Process) ==> {(patient ?S ?WH)}.
     */
    @Test
    public void testWhatLanguageDidAmeliaSpeak() {

        Interpreter interpreter = new Interpreter();
        interpreter.initialize();

        String input = "root(ROOT-0,speak-7), det(language-2,what-1), dobj(speak-7,language-2), aux(speak-7,do-3), nsubj(speak-7,AmeliaMaryEarhart-4), sumo(Speaking,speak-7), sumo(Language,language-2), sumo(IntentionalProcess,do-3), sumo(Human,AmeliaMaryEarhart-4), names(AmeliaMaryEarhart-4,\"Amelia Mary Earhart\"), attribute(AmeliaMaryEarhart-4,Female), number(SINGULAR,language-2), number(SINGULAR,Amelia-4), number(SINGULAR,Mary-5), number(SINGULAR,Earhart-6)";
        ArrayList<CNF> cnfInput = interpreter.getCNFInput(input);

        // Where did she fly?
        Set<String> expected = Sets.newHashSet(
                "(patient speak-7 ?WH)",
                "(agent speak-7 AmeliaMaryEarhart-4)",
                "(attribute AmeliaMaryEarhart-4 Female)",
                "(names AmeliaMaryEarhart-4 \"Amelia Mary Earhart\")",
                "(instance AmeliaMaryEarhart-4 Human)",
                "(instance speak-7 Speaking)");

        ArrayList<String> kifClauses = interpreter.interpretCNF(cnfInput);
        Set<String> actual = Sets.newHashSet(kifClauses);
        Set<String> cleanedActual = actual.stream().map(str -> str.replaceAll("\\s+", " ")).collect(Collectors.toSet());
        assertEquals(expected, cleanedActual);
    }
}
