package com.articulate.sigma.nlg;

import com.articulate.sigma.Formula;
import com.google.common.collect.*;

import java.util.List;
import java.util.Map;

/**
* One element of a LanguageFormatterStack.
*/
public class StackElement {

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

    /**
     * Holds all the events being processed.
     */
    private Map<String, SumoProcessCollector> sumoProcessMap = Maps.newHashMap();

    /**
     * Holds properties belonging to entities involved in the processes. Key is a string identifier (e.g. variable).
     */
    private Multimap<String, SumoProcessEntityProperty> entityProperties = TreeMultimap.create();

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
    void init(List<String> args) {
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

    /********************************************************************************
     * Set the polarity of current stack element's process.
     * @param predicate
     * @param polarity
     */
    public void setProcessPolarity(String predicate, VerbProperties.Polarity polarity) {
        SumoProcessCollector process = sumoProcessMap.get(predicate);
        if (process != null) {
            process.setPolarity(polarity);
        }
    }

    /********************************************************************************
     * Set polarity.
     * @param polarity
     */
    public void setProcessPolarity(VerbProperties.Polarity polarity) {
        if (sumoProcessMap.isEmpty())     {
            return;
        }
        String singleKey = sumoProcessMap.keySet().iterator().next();
        setProcessPolarity(singleKey, polarity);
    }

    /********************************************************************************
     * Getter and setter for translated field.
     * @return
     */
    public boolean getTranslated() {
        return translated;
    }

    public String getTranslation() {
        return translation;
    }

    /********************************************************************************
     * Getter and setter for translated field.
     * @return
     */
    public Multimap<String, SumoProcessEntityProperty> getEntityProperties() {
        return entityProperties;
    }

    public void setEntityProperties(Multimap<String, SumoProcessEntityProperty> entityProperties) {
        this.entityProperties = entityProperties;
    }

    /********************************************************************************
     *
     * @return
     */
    public Map<String, SumoProcessCollector> getSumoProcessMap() {
        return sumoProcessMap;
    }


}
