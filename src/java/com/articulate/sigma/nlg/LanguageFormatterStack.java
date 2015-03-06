package com.articulate.sigma.nlg;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * The stack which LanguageFormatter uses in its recursive operations.
 */
public class LanguageFormatterStack {
    private final List<StackElement> theStack = Lists.newArrayList();

    public enum StackState {PROCESSED, TRANSLATED, UNPROCESSED, QUANTIFIED_VARS}

    public class FormulaArg {
        final String argument;
        StackState state = StackState.UNPROCESSED;
        String translation = "";

        public FormulaArg(String arg, StackState stackState) {
            this.state = stackState;
            argument = arg;
        }
    }

    public class StackElement  {
        /**
         * Holds all the events being processed.
         */
        Map<String, SumoProcessCollector> sumoProcessMap = Maps.newHashMap();

        /**
         * Holds the arguments of the current clause. We use it to keep track of which arguments have been translated into informal NLG successfully.
         */
        final List<FormulaArg> formulaArgs = Lists.newArrayList();

        /**
         * Indicates whether we have translated this level into informal language.
         */
        private boolean translated = false;

        private String translation = "";

        /********************************************************************************
         * Instantiate a StackElement with a sumoProcess map and a list of formula args.
         *
         * @param spcMap
         * @param args
         */
        public StackElement(Map<String, SumoProcessCollector> spcMap, List<String> args)  {
            sumoProcessMap = Maps.newHashMap(spcMap);
            init(args);
        }

        /********************************************************************************
         * Init the formulaArgs and translated for this StackElement.
         * @param args
         */
        public void init(List<String> args) {
            translated = false;
            translation = "";
            formulaArgs.clear();
            for(String arg : args)  {
                formulaArgs.add(new FormulaArg(arg, StackState.UNPROCESSED));
            }
        }

        /********************************************************************************
         * Init the formulaArgs for this StackElement. The formula parameter is used to set valid
         * formulaArgs to states other than UNPROCESSED.
         * @param formula
         * @param args
         */
        public void argsInit(Formula formula, List<String> args) {
            init(args);

            // Set the formula args' state.
            String pred = formula.car();
            if (Formula.isQuantifier(pred))     {
                // See if it is a list of variables.

                String temp = args.get(0).replaceAll("[()]", "");
                String[] strings = temp.split(" ");
                boolean isVarList = true;
                for(String str : strings)  {
                    if (! Formula.isVariable(str)) {
                        isVarList = false;
                    }
                }
                if (isVarList) {
                    //formulaArgs.put(args.get(0), StackState.QUANTIFIED_VARS);
                    formulaArgs.get(0).state = StackState.QUANTIFIED_VARS;
                }

            }
        }

        /********************************************************************************
         * Mark this stack element as having been translated.
         * @param translation
         * @param translated
         */
        public void setTranslation(String translation, boolean translated) {
            this.translation = translation;
            this.translated = translated;
        }
    }


    // LanguageFormatterStack methods begin here.


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
     * Return the stack element that is second from the top
     * @return
     *  the 2nd from the top stack element; or null if the stack has fewer than 2 elements
     */
    public StackElement getPrevStackElement() {
        if(theStack.size() >= 2) {
            return theStack.get(theStack.size() - 2);
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
     * @param formula
     */
    public void insertFormulaArgs(Formula formula) {
        List<String> args = formula.complexArgumentsToArrayList(1);
        if(! isEmpty() && args != null) {
            // Put the args list into the stack for later reference.
            StackElement element = getTop();
            element.argsInit(formula, args);
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
            List<FormulaArg> stackArgs = theStack.get(theStack.size() - 2).formulaArgs;
            retVal = setFormulaArgState(stackArgs, theArg, StackState.PROCESSED);
        }

        return retVal;
    }

    /********************************************************************************
     * Set the state for the formula arg of the given argument.
     * @param theArg
     * @param state
     * @return
     *  true if successful; else false
     */
    private boolean setFormulaArgState(List<FormulaArg> stackArgs, String theArg, StackState state) {
        boolean retVal = false;
        if (stackArgs != null && ! stackArgs.isEmpty()) {
            FormulaArg fArg = LanguageFormatterStack.getFormulaArg(stackArgs, theArg);
            if (fArg != null) {
                retVal = true;
                fArg.state = state;
            }
        }
        return retVal;
    }

    /********************************************************************************
     * Read the given list of FormulaArgs for a given argument.
     * @param formulaArgs
     * @param theArg
     * @return
     *  the formula arg corresponding to the given argument; or null if not found
     */
    public static FormulaArg getFormulaArg(List<FormulaArg> formulaArgs, String theArg) {
        for (FormulaArg fArg : formulaArgs)   {
            if (fArg.argument.equals(theArg))   {
                return fArg;
            }
        }
        return null;
    }

    /********************************************************************************
     * Are all the formula arguments processed in some way? Note that this method
     * is called when the relevant args are not held at top of stack, but at top - 1.
     * @return
     */
    public boolean areFormulaArgsProcessed() {
        boolean retVal = false;

        if(theStack.size() >= 2) {
            // The relevant args are not held at top of stack, but at top - 1
            StackElement stackElement = theStack.get(theStack.size() - 2);
            retVal = LanguageFormatterStack.areFormulaArgsProcessed(stackElement);
        }

        return retVal;
    }

    /********************************************************************************
     * Have all the formula arguments for the given stack element been processed in some way?
     * @param stackElement
     * @return
     *  return true if all the clause arguments have been marked as Processed;
     *  return false if they have not or no arguments exist
     */
    public static boolean areFormulaArgsProcessed(StackElement stackElement) {
        boolean retVal = false;

        List<FormulaArg> stackArgs = stackElement.formulaArgs;
        if (stackArgs != null && ! stackArgs.isEmpty()) {
            boolean isComplete = true;
            for (FormulaArg fArg : stackArgs) {
                EnumSet enumSet = EnumSet.of(StackState.PROCESSED, StackState.TRANSLATED, StackState.QUANTIFIED_VARS);
                if (! enumSet.contains(fArg.state)) {
                    isComplete = false;
                    break;
                }
            }
            retVal = isComplete;
        }


        return retVal;
    }

    /********************************************************************************
     * Generate natural language from the contents of the top element of the stack.
     * Returns empty string if the top element's formula arguments have not all been processed.
     * @return
     *  the NLG if top element can be processed; else empty string
     */
    public String doProcessLevelNatlLanguageGeneration() {
        String output = "";
        if (areFormulaArgsProcessed()) {
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

    /********************************************************************************
     * Top-level method call for informal NLG after all the elements of the formula have been processed.
     */
    public String doStatementLevelNatlLanguageGeneration() {
        String output = "";
        // Stack should have only one element

        if (theStack.size() == 1) {
            output = getCurrStackElement().translation;
        }
        return output;
    }

    /********************************************************************************
     * If the top stack element has been translated, then find the given arg in the previous stack element
     * and mark the corresponding formula argument as translated.
     * @param arg
     */
    public void pushCurrTranslatedStateDown(String arg) {
        StackElement curr = getCurrStackElement();

        StackElement prev = getPrevStackElement();
        if (prev == null)  {
            return;
        }

        FormulaArg prevFormulaArg = LanguageFormatterStack.getFormulaArg(prev.formulaArgs, arg);
        if (prevFormulaArg == null)  {
            return;
        }

        if (curr.translated)  {
            if (curr.translation == null || curr.translation.isEmpty())  {
                throw new IllegalStateException("Current stack element state is Translated, but translation is null or empty.");
            }

            prevFormulaArg.state = StackState.TRANSLATED;
            prevFormulaArg.translation = curr.translation;
        }
        else if (prevFormulaArg.state.equals(StackState.QUANTIFIED_VARS))   {
            // This is set in initialization of the formula, and we don't want to change it.
            return;
        }
        else    {
            if (! curr.translation.isEmpty())  {
                throw new IllegalStateException("Current stack element state is not Translated, but translation is not empty.");
            }

            return;
        }
    }

    /********************************************************************************
     * For the current stack element, iterate through the List<FormulaArgs> and collect their translations into a single list.
     * @return
     *  the curr element's formula args, or empty list if any formula arg has not been translated into informal NLG
     */
    public List<String> getCurrStackFormulaArgs() {
        StackElement element = getCurrStackElement();

        // Collect the translations.
        List<String> translations = Lists.newArrayList();
        for (FormulaArg formula : element.formulaArgs)   {
            // If any element has not been translated, do nothing.
            if (! formula.state.equals(StackState.TRANSLATED))   {
                return Lists.newArrayList();
            }
            translations.add(formula.translation);
        }

        return translations;
    }

    /********************************************************************************
     * If the curr stack element has just two formula args--the first having a state of QUANTIFIED_VARS, the
     * second having a state of TRANSLATED--mark the curr stack element as Translated.
     */
    public void setCurrTranslatedIfQuantified() {
        if(isQuantifiedClauseProcessed())    {
            String translation = getCurrStackElement().formulaArgs.get(1).translation;
            getCurrStackElement().setTranslation(translation, true);
        }
    }

    /********************************************************************************
     * Has the current clause been processed? This tells us whether we can eliminate quantifier
     * variable lists from informal NLG. We can do so when the stack has only two elements whose states
     * are QUANTIFIED_VARS and TRANSLATED.
     * @return
     */
    boolean isQuantifiedClauseProcessed() {
        boolean retVal = false;

        StackElement element = getCurrStackElement();
        boolean foundQuantified = false;
        boolean foundTranslated = false;
        if (element.formulaArgs.size() == 2) {
            for (FormulaArg arg : element.formulaArgs)   {
                if (arg.state.equals(StackState.QUANTIFIED_VARS))    {
                    foundQuantified = true;
                }
                else if (arg.state.equals(StackState.TRANSLATED))  {
                    foundTranslated = true;
                }
            }
            if (foundQuantified && foundTranslated)  {
                retVal = true;
            }
        }
        return retVal;
    }

    /********************************************************************************
     * If possible, translate the process instantiation and insert the translatation into the topmost
     * stack element.
     * @param kb
     * @param formula
     *   a formula for the instantiation of a process, e.g. (instance ?event Classifying)
     */
    public void translateCurrProcessInstantiation(KB kb, Formula formula) {
        // Expecting the instantiation of a process, e.g. (instance ?FLY FlyingAircraft)
        String process = formula.complexArgumentsToArrayList(2).get(0);
        if (kb.isSubclass(process, "IntentionalProcess"))  {
            String kbForm = kb.getTermFormatMap("EnglishLanguage").get(process);
            if(kbForm == null)  {
                // For some reason the formatted version of the term is not in the KBs. Maybe we'll have luck with the process name in the original formula.
                kbForm = process;
            }
            String simpleForm = SumoProcess.getVerbRootForm(kbForm);
            if (simpleForm != null && ! simpleForm.isEmpty())  {
                simpleForm = SumoProcess.verbRootToThirdPersonSingular(simpleForm);
                getCurrStackElement().setTranslation("someone " + simpleForm, true);
            }
        }
    }


}
