package com.articulate.sigma.nlg;

import com.articulate.sigma.Formula;
import com.articulate.sigma.nlg.LanguageFormatterStack;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class LanguageFormatterStackTest {

    @Test
    public void testInsertQuantifier() {
        String stmt = "(exists (?D ?H)\n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H)))";
        Formula formula = new Formula(stmt);

        LanguageFormatterStack stack = new LanguageFormatterStack();
        stack.pushNew();
        stack.insertFormulaArgs(formula);

        List<LanguageFormatterStack.FormulaArg> formulaArgs = stack.getCurrStackElement().formulaArgs;
        assertEquals(2, formulaArgs.size());

        assertEquals(LanguageFormatterStack.StackState.QUANTIFIED_VARS, LanguageFormatterStack.getFormulaArg(formulaArgs, "(?D ?H)").state);

        String expectedKey = "(and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H))";
        assertEquals(LanguageFormatterStack.StackState.UNPROCESSED, LanguageFormatterStack.getFormulaArg(formulaArgs, expectedKey).state);
    }


    @Test
    public void testInsertAnd() {
        String stmt = "(and\n" +
                "           (instance ?D Driving)\n" +
                "           (instance ?H Human)\n" +
                "           (agent ?D ?H))";
        Formula formula = new Formula(stmt);

        LanguageFormatterStack stack = new LanguageFormatterStack();
        stack.pushNew();
        stack.insertFormulaArgs(formula);

        List<LanguageFormatterStack.FormulaArg> formulaArgs = stack.getCurrStackElement().formulaArgs;
        assertEquals(3, formulaArgs.size());

        String expectedKey = "(instance ?D Driving)";
        assertEquals(LanguageFormatterStack.StackState.UNPROCESSED, LanguageFormatterStack.getFormulaArg(formulaArgs, expectedKey).state);

        expectedKey = "(instance ?H Human)";
        assertEquals(LanguageFormatterStack.StackState.UNPROCESSED, LanguageFormatterStack.getFormulaArg(formulaArgs, expectedKey).state);

        expectedKey = "(agent ?D ?H)";
        assertEquals(LanguageFormatterStack.StackState.UNPROCESSED, LanguageFormatterStack.getFormulaArg(formulaArgs, expectedKey).state);
    }


    @Test(expected=IllegalStateException.class)
    public void testIllegalTranslatedState() {
        String string1 = "(exists (?D ?H)\n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H)))";


        LanguageFormatterStack stack = new LanguageFormatterStack();

        // Push two items onto the stack.
        stack.pushNew();
        Formula formula1 = new Formula(string1);
        stack.insertFormulaArgs(formula1);
        stack.pushNew();
        String string2 = formula1.complexArgumentsToArrayList(1).get(1);
        Formula formula2 = new Formula(string2);
        stack.insertFormulaArgs(formula2);

        // Verify state of bottom element's arg.
        assertEquals(LanguageFormatterStack.StackState.UNPROCESSED, LanguageFormatterStack.getFormulaArg(stack.getPrevStackElement().formulaArgs, string2).state);

        // Set top element's translated state, creating the illegal state.
        stack.getCurrStackElement().setTranslation("", true);

        // Call pushCurrTranslatedStateDown().
        stack.pushCurrTranslatedStateDown(string2);
    }

    @Test
    public void testPushTranslatedYes() {
        String string1 = "(exists (?D ?H)\n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H)))";


        LanguageFormatterStack stack = new LanguageFormatterStack();

        // Push two items onto the stack.
        stack.pushNew();
        Formula formula1 = new Formula(string1);
        stack.insertFormulaArgs(formula1);
        stack.pushNew();
        String string2 = formula1.complexArgumentsToArrayList(1).get(1);
        Formula formula2 = new Formula(string2);
        stack.insertFormulaArgs(formula2);

        // Verify state of bottom element's arg.
        //assertEquals(LanguageFormatterStack.StackState.UNPROCESSED, stack.getPrevStackElement().formulaArgs.get(string2));
        assertEquals(LanguageFormatterStack.StackState.UNPROCESSED, LanguageFormatterStack.getFormulaArg(stack.getPrevStackElement().formulaArgs, string2).state);

        // Set top element's translated state.
        stack.getCurrStackElement().setTranslation("a human drives", true);

        // Call pushCurrTranslatedStateDown().
        stack.pushCurrTranslatedStateDown(string2);

        // Verify the state has changed.
        LanguageFormatterStack.FormulaArg formulaArg = LanguageFormatterStack.getFormulaArg(stack.getPrevStackElement().formulaArgs, string2);
        assertEquals("a human drives", formulaArg.translation);
        assertEquals(LanguageFormatterStack.StackState.TRANSLATED, formulaArg.state);
    }

    @Test
    public void testPushTranslatedNo() {
        String string1 = "(exists (?D ?H)\n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H)))";


        LanguageFormatterStack stack = new LanguageFormatterStack();

        // Push two items onto the stack.
        stack.pushNew();
        Formula formula1 = new Formula(string1);
        stack.insertFormulaArgs(formula1);
        stack.pushNew();
        String string2 = formula1.complexArgumentsToArrayList(1).get(1);
        Formula formula2 = new Formula(string2);
        stack.insertFormulaArgs(formula2);

        // Verify state of bottom element's arg.
        assertEquals(LanguageFormatterStack.StackState.UNPROCESSED, LanguageFormatterStack.getFormulaArg(stack.getPrevStackElement().formulaArgs, string2).state);

        // Do not modify top element's translated state.
        //stack.getCurrStackElement().setTranslation(true);

        // Call pushCurrTranslatedStateDown().
        stack.pushCurrTranslatedStateDown(string2);

        // Verify the state has changed.
        assertEquals(LanguageFormatterStack.StackState.UNPROCESSED, LanguageFormatterStack.getFormulaArg(stack.getPrevStackElement().formulaArgs, string2).state);
    }


    @Test
    public void testAreFormulaArgsProcessed() {
        String string1 = "(exists (?D ?H)\n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H)))";


        LanguageFormatterStack stack = new LanguageFormatterStack();

        // Push two items onto the stack.
        stack.pushNew();
        Formula formula1 = new Formula(string1);
        stack.insertFormulaArgs(formula1);
        stack.pushNew();
        String string2 = formula1.complexArgumentsToArrayList(1).get(1);
        Formula formula2 = new Formula(string2);
        stack.insertFormulaArgs(formula2);

        // Nothing marked as processed except for var quantifier in first element at bottom of stack.
        assertFalse(LanguageFormatterStack.areFormulaArgsProcessed(stack.getCurrStackElement()));
        assertFalse(LanguageFormatterStack.areFormulaArgsProcessed(stack.getPrevStackElement()));


        // Set top element's translated state.
        stack.getCurrStackElement().setTranslation("a human drives", true);

        // Call pushCurrTranslatedStateDown().
        stack.pushCurrTranslatedStateDown(string2);

        // Pushing down from curr to bottom of stack has set the state of the second element at bottom of stack.
        // Now both are in a "processed" state.
        assertFalse(LanguageFormatterStack.areFormulaArgsProcessed(stack.getCurrStackElement()));
        assertTrue(LanguageFormatterStack.areFormulaArgsProcessed(stack.getPrevStackElement()));
    }

}