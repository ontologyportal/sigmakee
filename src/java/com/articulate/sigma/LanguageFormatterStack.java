package com.articulate.sigma;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The stack which LanguageFormatter uses in its recursive operations.
 */
public class LanguageFormatterStack {
    private final List<StackElement> theStack = Lists.newArrayList();

    public class StackElement  {
        /**
         * Holds all the events being processed.
         */
        Map<String, SumoProcessCollector> sumoProcessMap;

        /**
         * Holds the arguments of the current clause. We use it to keep track of which arguments have been processed successfully.
         */
        List<String> formulaArgs;

        public StackElement(Map<String, SumoProcessCollector> spm, List<String> args)  {
            sumoProcessMap = Maps.newHashMap(spm);
            formulaArgs = Lists.newArrayList(args);
        }
    }

    /********************************************************************************
     * Get the topmost StackElement.
     * @return
     */
    private StackElement getTop()   { return theStack.get(theStack.size() - 1);}

    /********************************************************************************
     * Pop the top element of the stack if it is inElement.
     * @param inElement
     * throws IllegalStateException if the topmost element is not inElement, or if the
     * stack cannot be popped
     */
    public void pop(StackElement inElement) {
        // Something is wrong if the top element of the stack isn't inElement.
        if(theStack.indexOf(inElement) != (theStack.size() - 1)) {
            throw new IllegalStateException("Current element of stack is not the top element.");
        }
        if(! theStack.remove(inElement))   {
            throw new IllegalStateException("Unable to pop the stack.");
        }
    }

    /********************************************************************************
     * Is the stack empty?
     * @return
     */
     public boolean isEmpty()    { return theStack.isEmpty(); }


    /********************************************************************************
     * Push a new element onto the stack.
     */
    public void pushNew() {
        Map<String, SumoProcessCollector> nextProcessMap = Maps.newHashMap();
        List<String> argsList = Lists.newArrayList();
        StackElement inElement = new StackElement(nextProcessMap, argsList);
        theStack.add(inElement);
    }

    /********************************************************************************
     * Return the topmost stack element
     * @return
     *  the topmost stack element; or null if the stack is empty
     */
    public StackElement getCurrStackElement() {
        if(! isEmpty()) {
            return getTop();
        }
        return null;
    }

    /********************************************************************************
     * Return the Map<String, SumoProcessCollector> for the top element of the stack.
     * @return
     */
    public Map<String, SumoProcessCollector> getCurrProcessMap() {
        Map<String, SumoProcessCollector> retVal = Maps.newHashMap();

        if (! isEmpty())    {
            StackElement element = getTop();
            retVal = element.sumoProcessMap;
        }

        return retVal;
    }

    /********************************************************************************
     * Insert the given formula arguments into the topmost element of the stack.
     * @param args
     */
    public void insertFormulaArgs(ArrayList<String> args) {
        if(! isEmpty()) {
            // Put the args list into the stack for later reference.
            StackElement element = getTop();
            element.formulaArgs = args;
        }
    }

    /********************************************************************************
     * Mark the given formula argument as having been processed. Note that this method
     * is called when the relevant args are not held at top of stack, but at top - 1.
     * @param theArg
     * @return
     */
    public boolean markFormulaArgAsProcessed(String theArg) {
        boolean retVal = false;

        if(theStack.size() >= 2) {
            // The relevant args are not held at top of stack, but at top - 1
            List<String> stackArgs = theStack.get(theStack.size() - 2).formulaArgs;
            if (stackArgs.contains(theArg)) {
                retVal = true;

                int idx = stackArgs.indexOf(theArg);
                String temp = "PROCESSED: " + stackArgs.remove(idx);
                stackArgs.add(idx, temp);
            }
        }

        return retVal;
    }

    /********************************************************************************
     * Are all the formula arguments marks as processed? Note that this method
     * is called when the relevant args are not held at top of stack, but at top - 1.
     * @return
     */
    public boolean areFormulaArgsProcessed() {
        boolean retVal = false;

        if(theStack.size() >= 2) {
            // The relevant args are not held at top of stack, but at top - 1
            StackElement stackElement = theStack.get(theStack.size() - 2);
            retVal = areFormulaArgsProcessed(stackElement);
        }

        return retVal;
    }

    /********************************************************************************
     * Have all the formula arguments for the given stack element been marked as Processed?
     * @param stackElement
     * @return
     *  return true if all the clause arguments have been marked as Processed;
     *  return false if they have not or no arguments exist
     */
    public boolean areFormulaArgsProcessed(StackElement stackElement) {
        boolean retVal = false;

        List<String> stackArgs = stackElement.formulaArgs;
        if (! stackArgs.isEmpty()) {
            boolean isComplete = true;
            for (String fArg : stackArgs) {
                if (! fArg.startsWith("PROCESSED: ")) {
                    isComplete = false;
                    break;
                }
            }
            retVal = isComplete;
        }

        return retVal;
    }

    /********************************************************************************
     * Is the current clause fully processed?
     * @return
     *  return true if all the clause arguments have been marked as Processed;
     *  return false if they have not or no arguments exist
     */
    public boolean isClauseFullyProcessed() {
        boolean retVal = false;

        if(! theStack.isEmpty()) {
            // The relevant args are not held at top of stack, but at top - 1
            StackElement stackElement = getTop();
            retVal = areFormulaArgsProcessed(stackElement);
        }

        return retVal;
    }

    /********************************************************************************
     * Generate natural language from the contents of the top element of the stack.
     * Returns empty string if the top element's formula arguments have not all been processed.
     * @return
     *  the NLG if top element can be processed; else empty string
     */
    public String doCurrNatlLanguageGeneration() {
        String output = "";
        if(areFormulaArgsProcessed()) {
            StringBuilder sb = new StringBuilder();

            for (SumoProcessCollector process : getCurrProcessMap().values()) {
                String naturalLanguage = process.toNaturalLanguage();
                if (!naturalLanguage.isEmpty()) {
                    sb.append(naturalLanguage).append(" and ");
                }
            }
            // Remove last "and" if it exists.
            output = sb.toString().replaceAll(" and $", "");
        }
        return output;
    }


}
