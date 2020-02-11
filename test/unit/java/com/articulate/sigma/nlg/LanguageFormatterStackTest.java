package com.articulate.sigma.nlg;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.articulate.sigma.nlg.LanguageFormatterStack;
import org.junit.Test;

import java.util.List;
import java.util.Stack;

import static org.junit.Assert.*;

public class LanguageFormatterStackTest extends SigmaMockTestBase {
    private final KB kb = kbMock;


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

        List<StackElement.FormulaArg> formulaArgs = stack.getCurrStackElement().formulaArgs;
        assertEquals(2, formulaArgs.size());

        assertEquals(StackElement.StackState.QUANTIFIED_VARS, LanguageFormatterStack.getFormulaArg(formulaArgs, "(?D ?H)").state);

        String expectedKey = "(and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H))";
        assertEquals(StackElement.StackState.UNPROCESSED, LanguageFormatterStack.getFormulaArg(formulaArgs, expectedKey).state);
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

        List<StackElement.FormulaArg> formulaArgs = stack.getCurrStackElement().formulaArgs;
        assertEquals(3, formulaArgs.size());

        String expectedKey = "(instance ?D Driving)";
        assertEquals(StackElement.StackState.UNPROCESSED, LanguageFormatterStack.getFormulaArg(formulaArgs, expectedKey).state);

        expectedKey = "(instance ?H Human)";
        assertEquals(StackElement.StackState.UNPROCESSED, LanguageFormatterStack.getFormulaArg(formulaArgs, expectedKey).state);

        expectedKey = "(agent ?D ?H)";
        assertEquals(StackElement.StackState.UNPROCESSED, LanguageFormatterStack.getFormulaArg(formulaArgs, expectedKey).state);
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
        String string2 = formula1.complexArgumentsToArrayListString(1).get(1);
        Formula formula2 = new Formula(string2);
        stack.insertFormulaArgs(formula2);

        // Verify state of bottom element's arg.
        assertEquals(StackElement.StackState.UNPROCESSED, LanguageFormatterStack.getFormulaArg(stack.getPrevStackElement().formulaArgs, string2).state);

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
        String string2 = formula1.complexArgumentsToArrayListString(1).get(1);
        Formula formula2 = new Formula(string2);
        stack.insertFormulaArgs(formula2);

        // Verify state of bottom element's arg.
        assertEquals(StackElement.StackState.UNPROCESSED, LanguageFormatterStack.getFormulaArg(stack.getPrevStackElement().formulaArgs, string2).state);

        // Set top element's translated state.
        stack.getCurrStackElement().setTranslation("a human drives", true);

        // Call pushCurrTranslatedStateDown().
        stack.pushCurrTranslatedStateDown(string2);

        // Verify the state has changed.
        StackElement.FormulaArg formulaArg = LanguageFormatterStack.getFormulaArg(stack.getPrevStackElement().formulaArgs, string2);
        assertEquals("a human drives", formulaArg.translation);
        assertEquals(StackElement.StackState.TRANSLATED, formulaArg.state);
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
        String string2 = formula1.complexArgumentsToArrayListString(1).get(1);
        Formula formula2 = new Formula(string2);
        stack.insertFormulaArgs(formula2);

        // Verify state of bottom element's arg.
        assertEquals(StackElement.StackState.UNPROCESSED, LanguageFormatterStack.getFormulaArg(stack.getPrevStackElement().formulaArgs, string2).state);

        // Do not modify top element's translated state.
        //stack.getCurrStackElement().setTranslation(true);

        // Call pushCurrTranslatedStateDown().
        stack.pushCurrTranslatedStateDown(string2);

        // Verify the state has changed.
        assertEquals(StackElement.StackState.UNPROCESSED, LanguageFormatterStack.getFormulaArg(stack.getPrevStackElement().formulaArgs, string2).state);
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
        String string2 = formula1.complexArgumentsToArrayListString(1).get(1);
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


    @Test
    public void testPushCurrSumoProcessDown() {
        String string1 = "(and \n" +
                "               (instance John-1 Human) \n" +
                "               (instance ?event Seeing) \n" +
                "               (instance ?object SelfConnectedObject) \n" +
                "               (experiencer ?event John-1) \n" +
                "               (not \n" +
                "                   (patient ?event ?object)))";


        LanguageFormatterStack stack = new LanguageFormatterStack();

        // Push items onto the stack.
        stack.pushNew();
        Formula formula1 = new Formula(string1);
        stack.insertFormulaArgs(formula1);
        stack.pushNew();
        String string2 = formula1.complexArgumentsToArrayListString(1).get(1);
        Formula formula2 = new Formula(string2);
        stack.insertFormulaArgs(formula2);
        stack.pushNew();
        String string3 = formula2.complexArgumentsToArrayListString(1).get(1);
        Formula formula3 = new Formula(string3);
        stack.insertFormulaArgs(formula3);

        // Nothing marked as processed except for var quantifier in first element at bottom of stack.
        assertEquals(0, stack.getCurrStackElement().getSumoProcessMap().size());
        assertEquals(0, stack.getPrevStackElement().getSumoProcessMap().size());

        // Set the Sumo process maps for the top two elements.
        SumoProcessCollector processCollector = new SumoProcessCollector(kb, "experiencer", "Seeing", "John-1");
        stack.getPrevStackElement().getSumoProcessMap().put("?event", processCollector);

        processCollector = new SumoProcessCollector(kb, "patient", "Seeing", "?object");
        stack.getCurrStackElement().getSumoProcessMap().put("?event", processCollector);

        // Set top element's state to negative.
        stack.getCurrStackElement().setProcessPolarity("?event", VerbProperties.Polarity.NEGATIVE);

        // Now the stack elements should be populated.
        assertEquals(1, stack.getCurrStackElement().getSumoProcessMap().size());
        assertEquals(1, stack.getCurrStackElement().getSumoProcessMap().get("?event").getRolesAndEntities().size());
        assertEquals(VerbProperties.Polarity.NEGATIVE, stack.getCurrStackElement().getSumoProcessMap().get("?event").getPolarity());
        assertEquals(1, stack.getPrevStackElement().getSumoProcessMap().size());
        assertEquals(1, stack.getPrevStackElement().getSumoProcessMap().get("?event").getRolesAndEntities().size());
        assertEquals(VerbProperties.Polarity.AFFIRMATIVE, stack.getPrevStackElement().getSumoProcessMap().get("?event").getPolarity());

        // Call pushCurrSumoProcessDown().
        stack.pushCurrSumoProcessDown();

        // Now the second-from-the-top should contain the information in the topmost element. The topmost won't have changed.
        assertEquals(1, stack.getCurrStackElement().getSumoProcessMap().size());
        assertEquals(1, stack.getCurrStackElement().getSumoProcessMap().get("?event").getRolesAndEntities().size());
        assertEquals(VerbProperties.Polarity.NEGATIVE, stack.getCurrStackElement().getSumoProcessMap().get("?event").getPolarity());
        assertEquals(1, stack.getPrevStackElement().getSumoProcessMap().size());
        assertEquals(2, stack.getPrevStackElement().getSumoProcessMap().get("?event").getRolesAndEntities().size());
        assertEquals(VerbProperties.Polarity.NEGATIVE, stack.getPrevStackElement().getSumoProcessMap().get("?event").getPolarity());
    }

}