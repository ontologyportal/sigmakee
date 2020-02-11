package com.articulate.sigma.nlg;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.articulate.sigma.nlg.StackElement.*;

/**
 * The stack which LanguageFormatter uses in its recursive operations.
 */
public class LanguageFormatterStack {
    private final List<StackElement> theStack = Lists.newArrayList();

    private VerbProperties.Polarity polarity = VerbProperties.Polarity.AFFIRMATIVE;

    /********************************************************************************
     * Getter and setter for polarity field.
     * @return
     */
    public VerbProperties.Polarity getPolarity() {
        return polarity;
    }

    public void setPolarity(VerbProperties.Polarity polarity) {
        this.polarity = polarity;
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
        if (theStack.indexOf(inElement) != (theStack.size() - 1)) {
            throw new IllegalStateException("Current element of stack is not the top element.");
        }
        if (! theStack.remove(inElement))   {
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

        if (! isEmpty()) {
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

        if (theStack.size() >= 2) {
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
            retVal = element.getSumoProcessMap();
        }
        return retVal;
    }

    /********************************************************************************
     * Insert the given formula arguments into the topmost element of the stack.
     * @param formula
     */
    public void insertFormulaArgs(Formula formula) {

        List<String> args = formula.complexArgumentsToArrayListString(1);
        if (! isEmpty() && args != null) {
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
    public void markFormulaArgAsProcessed(String theArg) {

        if (theStack.size() >= 2) {
            // The relevant args are not held at top of stack, but at top - 1
            List<FormulaArg> stackArgs = theStack.get(theStack.size() - 2).formulaArgs;
            setFormulaArgState(stackArgs, theArg, StackState.PROCESSED);
        }
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

        if (theStack.size() >= 2) {
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
                // "Inherit" polarity from top-level if it is negative.
                if (this.polarity.equals(VerbProperties.Polarity.NEGATIVE)) {
                    process.setPolarity(VerbProperties.Polarity.NEGATIVE);
                }

                // Pass any entity properties, aka "attributes" on.
                process.setEntityProperties(getCurrStackElement().getEntityProperties());

                // Generate the language.
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
            output = getCurrStackElement().getTranslation();
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

        if (curr.getTranslated())  {
            if (curr.getTranslation() == null || curr.getTranslation().isEmpty())  {
                throw new IllegalStateException("Current stack element state is Translated, but translation is null or empty.");
            }

            prevFormulaArg.state = StackState.TRANSLATED;
            prevFormulaArg.translation = curr.getTranslation();
        }
        else if (prevFormulaArg.state.equals(StackState.QUANTIFIED_VARS))   {
            // This is set in initialization of the formula, and we don't want to change it.
            return;
        }
        else    {
            if (! curr.getTranslation().isEmpty())  {
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
            // FIXME: Also allow processed?
//            if (! formula.state.equals(StackState.TRANSLATED) )   {
//                if (! formula.state.equals(StackState.PROCESSED)) {
//                    return Lists.newArrayList();
//                }
//            }
//            else {
//                translations.add(formula.translation);
//            }

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

        if (isQuantifiedClauseProcessed())    {
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
     * If possible, translate the process instantiation and insert the translation into the topmost
     * stack element.
     * @param kb
     * @param formula
     *   a formula for the instantiation of a process, e.g. (instance ?event Classifying)
     */
    public void translateCurrProcessInstantiation(KB kb, Formula formula) {

        // Expecting the instantiation of a process, e.g. (instance ?FLY FlyingAircraft)
        String process = formula.complexArgumentsToArrayListString(2).get(0);
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

    /********************************************************************************
     * Push the current element's sumoProcessMap down into the previous element.
     * If the sumoProcessCollector already exists in the lower element of the stack,
     * merge the current element's sumo process elements into the lower element's.
     */
    public void pushCurrSumoProcessDown() {

        StackElement currElement = getCurrStackElement();
        StackElement prevElement = getPrevStackElement();
        Map<String, SumoProcessCollector> prevSumoMap = prevElement.getSumoProcessMap();
        for (Map.Entry<String, SumoProcessCollector> currProcessEntry : currElement.getSumoProcessMap().entrySet())   {
            String key = currProcessEntry.getKey();
            if (prevSumoMap.containsKey(key))    {
                // Merge.
                SumoProcessCollector prevCollector = prevSumoMap.get(key);
                prevCollector.merge(currProcessEntry.getValue());
            }
            else    {
                // Insert.
                prevSumoMap.put(key, currProcessEntry.getValue());
            }
        }
    }


    /********************************************************************************
     * Handle pushing the translation down into the stack for "not" clauses.
     * @param statement
     */
    public void pushTranslationDownToNotLevel(String statement) {
        String translation = getCurrStackElement().getTranslation();

        if (translation.isEmpty())  {
            return;
        }

        // Iterate through the stack from top down, looking for a "(not ..." formula arg.
        ListIterator<StackElement> iterator = theStack.listIterator(theStack.size());
        while(iterator.hasPrevious())   {
            StackElement element = iterator.previous();

            for (FormulaArg formulaArg : element.formulaArgs)   {
                if (formulaArg.argument.equals(statement))   {
                    // Set the formula arg.
                    formulaArg.translation = translation;
                    formulaArg.state = StackState.TRANSLATED;

                    // Set the element.
                    element.setTranslation(translation, true);

                    return;
                }
            }
        }

        // If the original formula begins with "(not" + existential quantifier, it won't be in the formula args.
        // In this case, iterate through the stack from top down, looking for the quantifier. For example, if the
        // original begins "(not  (exists (?D ?H) ...", we will find "(exists (?D ?H) ..." in the formula args.
        Formula formula = new Formula(statement);
        statement = formula.cdrAsFormula().car();
        iterator = theStack.listIterator(theStack.size());
        while(iterator.hasPrevious())   {
            StackElement element = iterator.previous();

            for (FormulaArg formulaArg : element.formulaArgs)   {
                if (formulaArg.argument.equals(statement))   {
                    // Set the formula arg.
                    formulaArg.translation = translation;
                    formulaArg.state = StackState.TRANSLATED;

                    // Set the element.
                    element.setTranslation(translation, true);

                    return;
                }
            }
        }
    }

    /********************************************************************************
     * Add the given key - property pair to the properties of the current stack element.
     * @param key
     * @param property
     */
    public void addToCurrProperties(String key, SumoProcessEntityProperty property) {
        StackElement element = getCurrStackElement();
        element.getEntityProperties().put(key, property);
    }
}
