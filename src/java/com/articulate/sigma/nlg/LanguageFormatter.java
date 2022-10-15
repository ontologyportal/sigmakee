/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. See also http://sigmakee.sourceforge.net

 This class expects the following to be in the ontology. Their absence won't cause an exception, but will prevent correct behavior.
   instance

 */

package com.articulate.sigma.nlg;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.StringUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.*;

/** ***************************************************************
 *  A class that handles the generation of natural language from logic.
 *
 *  @author Adam Pease - apease [at] articulatesoftware [dot] com, with thanks
 *  to Michal Sevcenko - sevcenko@vc.cvut.cz for development of the
 *  formatting language.
 */
public class LanguageFormatter {

    public static boolean debug = false;

    // a list of format parameters or words and the sentence words they match with
    public static HashMap<String,CoreLabel> outputMap = new HashMap<>();
    private final String statement;

    private final Map<String, String> phraseMap;

    // kb.getTermFormatMap() for this language
    private final Map<String, String> termMap;

    private static KB kb;
    private final String language;

    private final Map<String, HashSet<String>> variableTypes;

    private final Map<String, HashSet<String>> variableToInstanceMap;

    // Modifiable versions of variableTypes and variableToInstanceMap for informal NLG.
    private HashMap<String, Set<String>> variableTypesNLG;
    private  Map<String, Set<String>> variableToInstanceMapNLG;

    // FIXME: Verify that we handle all operators, then delete this field.
    private static final List<String> notReadyOperators = Lists.newArrayList();

    /**
     * "Informal" NLG refers to natural language generation in which the formal logic terms are expressions are
     * eliminated--e.g. "a man drives" instead of "there exist a process and an agent such that the process is an instance of driving and
     * the agent is an instance of human and the agent is an agent of the process".
     */
    private boolean doInformalNLG = false;

    public void setDoInformalNLG(boolean doIt) {
        doInformalNLG = doIt;
    }

    /**
     * The stack holds information on the formula being processed, so that successive recursive calls can refer to it.
     */
    private final LanguageFormatterStack theStack = new LanguageFormatterStack();

    // define the class that is used simply as a key in a CoreLabel
    // The value will be a variable that corresponds to the token by
    // prepending a '?' to the token plus its index, or by appending
    // a '*' to the token
    public static class VariableAnnotation implements CoreAnnotation<String> {
        public Class<String> getType() {
            return String.class;
        }
    }

    // define the class that is used simply as a key in a CoreLabel
    // The value is the number of the argument in a relation, corresponding
    // to this text token
    public static class RelationArgumentAnnotation implements CoreAnnotation<Integer> {
        public Class<Integer> getType() {
            return Integer.class;
        }
    }

    /*******************************************************************************
     * @param stmt The statement to be formatted.
     * @param phraseMap kb.getFormatMap() for this language
     * @param termMap kb.getTermFormatMap() for this language
     * @param kb
     * @param language
     */
    public LanguageFormatter(String stmt, Map<String, String> phraseMap, Map<String, String> termMap,
                             KB kb, String language) {

        outputMap = new HashMap<>();
        this.statement = stmt;
        this.phraseMap = phraseMap;
        this.termMap = termMap;
        this.kb = kb;
        this.language = language;

        Formula f = new Formula();
        f.read(statement);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        variableTypes = fp.computeVariableTypes(f, kb);
        variableToInstanceMap = fp.findExplicitTypes(kb,f);
        init();
    }

    /***********************************************************************************
     */
    private void init() {

        // Get special versions of variable maps.
        variableToInstanceMapNLG = new HashMap<>(variableToInstanceMap);
        variableTypesNLG = new HashMap<>();

        // variableToInstanceMapNLG should map variables to the correct surface form of the SUMO term
        for (Map.Entry<String, Set<String>> entry : variableToInstanceMapNLG.entrySet()) {
            String variable = entry.getKey();
            Set<String> origInstances = entry.getValue();
            Set<String> newInstances = Sets.newHashSet();
            for (String instance : origInstances) {
                String newStr = SumoProcessCollector.getProperFormOfEntity(instance, kb);
                newInstances.add(newStr);
            }
            variableToInstanceMapNLG.put(variable, newInstances);
        }

        // variableTypes should map variables to the correct surface form of the SUMO term, but we want it to contain
        // only those terms not in variableToInstanceMapNLG
        for (Map.Entry<String, HashSet<String>> entry : variableTypes.entrySet()) {
            String variable = entry.getKey();
            if (! variableToInstanceMapNLG.containsKey(variable)) {
                Set<String> origInstances = entry.getValue();
                Set<String> newInstances = Sets.newHashSet();
                for (String instance : origInstances) {
                    String newStr = SumoProcessCollector.getProperFormOfEntity(instance, kb);
                    newInstances.add(newStr);
                }
                variableTypesNLG.put(variable, newInstances);
            }
        }
    }

    /***********************************************************************************
     * Hyperlink terms in a natural language format string.  This assumes that
     * terms to be hyperlinked are in the form &%termName$termString , where
     * termName is the name of the term to be browsed in the knowledge base and
     * termString is the text that should be displayed hyperlinked.
     *
     * @param href the anchor string up to the term= parameter, which this method
     *               will fill in.
     * @return
     */
    public String htmlParaphrase(String href) {

        init();
        String nlFormat = "";
        try {
            theStack.pushNew();
            StackElement element = theStack.getCurrStackElement();

            String template = paraphraseStatement(statement, false, 1);

            // Check for successful informal NLG of the entire statement.
            if (doInformalNLG)  {
                String informalNLG = theStack.doStatementLevelNatlLanguageGeneration();
                if (! informalNLG.isEmpty())    {
                    // Resolve variables.
                    informalNLG = LanguageFormatter.variableReplace(informalNLG, variableToInstanceMapNLG, variableTypesNLG, kb, language);
                    if (debug) System.out.println("LanguageFormatter.htmlParaphrase():  " + informalNLG);
                    template = informalNLG;
                }
            }
            theStack.pop(element);

            // Replace any variables in the template.
            if (StringUtil.isNonEmptyString(template)) {
                Formula f = new Formula();
                f.read(statement);
                FormulaPreprocessor fp = new FormulaPreprocessor();
                //HashMap varMap = fp.computeVariableTypes(kb);
                HashMap<String, Set<String>> instanceMap = new HashMap<>();
                HashMap<String, Set<String>> classMap = new HashMap<>();
                HashMap<String, HashSet<String>> types = fp.computeVariableTypes(f, kb);
                Iterator<String> it = types.keySet().iterator();
                while (it.hasNext()) {
                    String var = it.next();
                    HashSet<String> typeList = types.get(var);
                    Iterator<String> it2 = typeList.iterator();
                    while (it2.hasNext()) {
                        String t = it2.next();
                        if (t.endsWith("+")) {
                            Set<String> values = new HashSet<>();
                            if (classMap.containsKey(var))
                                values = classMap.get(var);
                            values.add(t.substring(0, t.length()-1));
                            classMap.put(var, values);
                        }
                        else {
                            Set<String> values = new HashSet<>();
                            if (instanceMap.containsKey(var))
                                values = instanceMap.get(var);
                            values.add(t);
                            instanceMap.put(var, values);
                        }
                    }
                }
                if (!instanceMap.isEmpty() || !classMap.isEmpty() ) {
                    //if ((instanceMap != null && !instanceMap.isEmpty()) || (classMap != null && !classMap.isEmpty()))
                    template = variableReplace(template, instanceMap, classMap, kb, language);
                    if (debug) System.out.println("LanguageFormatter.htmlParaphrase(): template: " + template);
                }
                // Get rid of the percentage signs.
                nlFormat = NLGUtils.resolveFormatSpecifiers(template, href);
                if (debug) System.out.println("LanguageFormatter.htmlParaphrase(): nlFormat: " + nlFormat);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        if (debug) System.out.println("htmlParaphrase(): " + outputMap);
        return nlFormat;
    }

    /******************************************************************
     * Create a natural language paraphrase of a logical statement.
     *  @param stmt The statement to be paraphrased.
     *  @param isNegMode Whether the statement is negated.
     *  @param depth An int indicating the level of nesting, for control of indentation.
     *  @return A String, which is the paraphrased statement.
     */
    public String paraphraseStatement(String stmt, boolean isNegMode, int depth) {

        if (debug) System.out.println("INFO in LanguageFormatter.paraphraseStatement(): stmt: " + stmt);
        if (Formula.empty(stmt)) {
            System.out.println("Error in LanguageFormatter.paraphraseStatement(): stmt is empty");
            return "";
        }
        boolean alreadyTried = kb.getLoadFormatMapsAttempted().contains(language);
        if ((phraseMap == null) || phraseMap.isEmpty()) {
            if (!alreadyTried) { kb.loadFormatMaps(language); }
            return "";
        }
        if ((termMap == null) || termMap.isEmpty()) {
            if (!alreadyTried) { kb.loadFormatMaps(language); }
            return "";
        }
        StringBuilder result = new StringBuilder();
        String ans = null;
        Formula f = new Formula(stmt);

        theStack.insertFormulaArgs(f);

        if (f.atom()) {
            ans = processAtom(stmt, termMap);
            return ans;
        }
        else {
            if (!f.listP()) {
                if (!StringUtil.emptyString(stmt))
                    System.out.println("Error in LanguageFormatter.paraphraseStatement(): "
                        + " Statement is not an atom or a list: "
                        + stmt);
                return "";
            }
        }
        // The test immediately below should be changed to check that
        // the car (predicate) is either an atomic constant, or a
        // non-atomic (list) term formed with the function
        // PredicateFn.
        String pred = f.car();
        handleCaseRole(f, pred, isNegMode);

        // Mark the predicate as processed.
        // FIXME: handle other predicates here: located, orientation, etc.
        if (pred.equals("instance")) {
            // Do not mark as processed "complicated" clauses containing functions. They will have to be handled later in the process.
            // FIXME: The check below handles "(instance ?hamburger (FoodForFn Human))", but not "(instance (GovernmentFn ?Place) StateGovernment))".
            if (!kb.isFunction(new Formula(f.complexArgumentsToArrayList(2).get(0)).car())) {
                theStack.translateCurrProcessInstantiation(kb, f);
                theStack.markFormulaArgAsProcessed(stmt);
            }
        }

        if (!Formula.atom(pred)) {
            System.out.println("Error in LanguageFormatter.paraphraseStatement(): statement "
                    + stmt
                    + " has a formula in the predicate position.");
            return stmt;
        }

        if (NLGUtils.logicalOperator(pred)) {
            ans = paraphraseLogicalOperator(stmt, isNegMode, depth+1);
            List<String> translations = theStack.getCurrStackFormulaArgs();
            String translation = "";
            if (translations.size() > 1) {
                // "Plug" the operator into the informal language, e.g. "=> A B" becomes "if A, then B".
                translation = generateFormalNaturalLanguage(translations, pred, isNegMode);
            }
            else if (translations.size() == 1)
                translation = translations.get(0);
            if (! translation.isEmpty())
                theStack.getCurrStackElement().setTranslation(translation, true);
            return ans;
        }

        if (phraseMap.containsKey(pred)) {
            if (pred.equals("attribute"))    {
                theStack.markFormulaArgAsProcessed(stmt);
                // Add the property to the current stack element.
                SumoProcessEntityProperty property = new SumoProcessEntityProperty(f);
                String key = f.cdrAsFormula().car();
                theStack.addToCurrProperties(key, property);
                // Modify the variable maps so that the variables are mapped to a surface form for the entity which includes
                // the attribute.
                updateVariables(variableToInstanceMapNLG, key, property);
                updateVariables(variableTypesNLG, key, property);
            }
            else if (pred.equals("names")) {
                theStack.markFormulaArgAsProcessed(stmt);
                // Get the name, with the quotation marks which surround it.
                Formula forumulaCdr = f.cdrAsFormula();
                String name = forumulaCdr.car();
                // Get the variable.
                String var = forumulaCdr.cdr().substring(1, forumulaCdr.cdr().length() - 1);
                // Replace all variables with the name.
                variableToInstanceMapNLG.put(var, Sets.newHashSet(name));
                // TODO: not needed? variableTypesNLG.put(var, Sets.newHashSet(name));
            }
            ans = paraphraseWithFormat(stmt, isNegMode);
            return ans;
        }
        else {                              // predicate has no paraphrase
            if (Formula.isVariable(pred))
                result.append(pred);
            else
                result.append(processAtom(pred, termMap));
            f.read(f.cdr());
            while (!f.empty()) {
                if (debug) System.out.println("INFO in LanguageFormatter.paraphraseStatement(): stmt: " + f);
                if (debug) System.out.println("length: " + f.listLength());
                if (debug) System.out.println("result: " + result);
                String arg = f.car();
                f.read(f.cdr());
                result.append(" ");
                if (Formula.atom(arg))
                    result.append(processAtom(arg, termMap));
                else
                    result.append(paraphraseStatement(arg, isNegMode, depth + 1));
                if (!f.empty()) {
                    if (f.listLength() > 1)
                        result.append(", ");
                    else
                        result.append(" and");
                }
            }
        }
        ans = result.toString();
        return ans;
    }

    /*****************************************************************
     * Modify the given variable map so that given key is mapped to a surface form for the entity which includes
     * the given property.
     * @param variableMap
     * @param key
     * @param property
     */
    private void updateVariables(Map<String, Set<String>> variableMap, String key, SumoProcessEntityProperty property) {

        if (variableMap.containsKey(key))     {
            Set<String> oldSet = variableMap.get(key);
            Set<String> newSet = Sets.newHashSet();
            for (String oldStr : oldSet)   {
                // modify each noun accordingly
                String newStr = property.getSurfaceFormForNoun(oldStr, kb);
                newSet.add(newStr);
            }
            variableMap.put(key, newSet);
        }
    }

    /*****************************************************************
     * Insert the give case role into the current element's process map; or, if the SumoProcess already exists
     * there, update it with the new information.
     * @param formula
     * @param caseRole
     * @param isNegMode
     */
    private void handleCaseRole(Formula formula, String caseRole, boolean isNegMode) {

        if (! doInformalNLG)
            return;
        if (kb.kbCache.isInstanceOf(caseRole, "CaseRole")) {
            try {
                if (!theStack.isEmpty()) {
                    String caseArgument = formula.cadr() + " " + formula.caddr();
                    String[] caseArgs = caseArgument.trim().split(" ");
                    String processInstanceName = caseArgs[0];
                    String processParticipant = caseArgs[1];

                    if (variableTypes.containsKey(processInstanceName)) {

                        HashSet<String> vals = variableTypes.get(processInstanceName);
                        if (NLGUtils.containsProcess(vals, kb))  {
                            // Mark the argument as PROCESSED.
                            theStack.markFormulaArgAsProcessed(formula.getFormula());
                            // Get the data in the stack's topmost element.
                            Map<String, SumoProcessCollector> thisProcessMap = theStack.getCurrProcessMap();
                            if (thisProcessMap.containsKey(processInstanceName)) {
                                // Already there. Add new information if any exists.
                                SumoProcessCollector sProcess = thisProcessMap.get(processInstanceName);
                                //String resolvedParticipant = resolveVariable(processParticipant);
                                sProcess.addRole(caseRole, processParticipant);
                                if (isNegMode)
                                    sProcess.setPolarity(VerbProperties.Polarity.NEGATIVE);
                            }
                            else {
                                // Insert into stack.
                                String resolvedProcessName = resolveVariable(processInstanceName);
                                //String resolvedParticipant = resolveVariable(processParticipant);
                                SumoProcessCollector sProcess = new SumoProcessCollector(kb, caseRole, resolvedProcessName, processParticipant);
                                thisProcessMap.put(processInstanceName, sProcess);
                                if (isNegMode)
                                    sProcess.setPolarity(VerbProperties.Polarity.NEGATIVE);
                            }
                        }
                    }

                }
            }
            catch (IllegalArgumentException e) {
                // Recover from exception by turning off full NLG.
                this.doInformalNLG = false;
                String temp = statement.replaceAll("\\s+", " ");    // clean up, reducing consecutive whitespace to single space
                String msg = "Handled IllegalArgumentException after finding case role.\n   Exception message:\n      " + e.getMessage() + "\n" +
                        "   Formula:\n       " + temp + "\n";
                System.out.println("LanguageFormatter " + msg);
            }
        }
    }

    /** ***************************************************************
     * Uses variableToInstanceMap and variableTypes to resolve a given variable.
     * If more than one entry is in the map, currently takes the first one returned by the iterator.
     * @param input
     * @return
     */
    private String resolveVariable(String input) {

        String retVal = input;
        if (variableToInstanceMap.containsKey(input))    {
            Set<String> values = variableToInstanceMap.get(input);
            // If more than one entry, take the first one returned by the iterator.
            retVal = values.iterator().next();
        }
        else if (variableTypes.containsKey(input)) {
            Set<String> values = variableTypes.get(input);
            // If more than one entry, take the first one returned by the iterator.
            retVal = values.iterator().next();
        }
        return retVal;
    }

    /** ***************************************************************
     * Create a natural language paraphrase for statements involving the logical operators.
     * @param stmt The logical statement for which we want to paraphrase the operator, arg 0.
     * @param isNegMode Is the expression negated?
     * @param depth The nested operator depth, for controlling indentation.
     * @return The natural language paraphrase as a String, or null if the predicate was not a logical operator.
     */
    private String paraphraseLogicalOperator(String stmt, boolean isNegMode, int depth) {

        try {
            if (NLGUtils.getKeywordMap() == null) {
                if (debug) System.out.println("Error in LanguageFormatter.paraphraseLogicalOperator(): " + "keywordMap is null");
                return null;
            }
            ArrayList<String> args = new ArrayList<>();
            Formula f = new Formula();
            f.read(stmt);
            String pred = f.getStringArgument(0);
            f.read(f.cdr());

            String ans = null;

            if (LanguageFormatter.notReadyOperators.contains(pred))   {
                doInformalNLG = false;
            }

            // Push new element onto the stack.
            theStack.pushNew();
            StackElement inElement = theStack.getCurrStackElement();

            if (pred.equals("not")) {
                theStack.setPolarity(VerbProperties.Polarity.NEGATIVE);
                ans = paraphraseStatement(f.car(), true, depth + 1);
                inElement.setProcessPolarity(VerbProperties.Polarity.NEGATIVE);
                theStack.pushCurrSumoProcessDown();

                if (theStack.getCurrStackElement().getTranslated()) {
                    theStack.pushTranslationDownToNotLevel(stmt);
                    theStack.pop(inElement);
                }
                else    {
                    theStack.pop(inElement);
                    theStack.markFormulaArgAsProcessed(stmt);
                }
                return ans;
            }
            try {
                String arg = null;
                while (!f.empty()) {
                    arg = f.car();
                    String result = paraphraseStatement(arg, false, depth + 1);
                    if (StringUtil.isNonEmptyString(result))    {
                        args.add(result);
                        // Let the next element down in the stack know whether we've performed informal NLG.
                        theStack.pushCurrTranslatedStateDown(arg);
                    }
                    else {
                        System.out.println("INFO in LanguageFormatter.paraphraseLogicalOperator(): "
                                + "bad result for \"" + arg + "\": " + result);
                        return "";
                    }
                    f.read(f.cdr());
                }
                // Perform natural language output.
                if (doInformalNLG) {
                    Map<String, SumoProcessCollector> thisProcessMap = theStack.getCurrProcessMap();
                    if (thisProcessMap != null) {
                        if (! thisProcessMap.isEmpty()) {
                            // Only perform NLG if the args list has been fully processed.
                            boolean doIt = theStack.areFormulaArgsProcessed();
                            if (doIt) {
                                // Try to do natural language generation on top element of stack.
                                String output = theStack.doProcessLevelNatlLanguageGeneration();
                                if (!output.isEmpty())
                                    theStack.getPrevStackElement().setTranslation(output, true);
                            }
                        }
                    }
                }
            }
            catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            }
            finally {
                // Pop the stack.
                theStack.pop(inElement);
            }
            theStack.pushCurrTranslatedStateDown(stmt);
            // If this clause has successfully been generated into informal language, then we can drop any remaining quantifiers and mark
            // the entire clause as translated.
            theStack.setCurrTranslatedIfQuantified();
            // Do formal NLG in case informal NLG fails.
            return generateFormalNaturalLanguage(args, pred, isNegMode);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    /** ***************************************************************
     */
    public String generateFormalNaturalLanguage(List<String> args, String pred, boolean isNegMode) {

        if (args.isEmpty())
            return "";
        String COMMA = NLGUtils.getKeyword(",", language);
        //String QUESTION = getKeyword("?",language);
        String IF = NLGUtils.getKeyword("if", language);
        String THEN = NLGUtils.getKeyword("then", language);
        String AND = NLGUtils.getKeyword("and", language);
        String OR = NLGUtils.getKeyword("or", language);
        String IFANDONLYIF = NLGUtils.getKeyword("if and only if", language);
        String NOT = NLGUtils.getKeyword("not", language);
        String FORALL = NLGUtils.getKeyword("for all", language);
        String EXISTS = NLGUtils.getKeyword("there exists", language);
        String EXIST = NLGUtils.getKeyword("there exist", language);
        String NOTEXIST = NLGUtils.getKeyword("there don't exist", language);
        String NOTEXISTS = NLGUtils.getKeyword("there doesn't exist", language);
        String HOLDS = NLGUtils.getKeyword("holds", language);
        String SOTHAT = NLGUtils.getKeyword("so that", language);
        String SUCHTHAT = NLGUtils.getKeyword("such that", language);
        if (StringUtil.emptyString(SUCHTHAT)) { SUCHTHAT = SOTHAT; }

        StringBuilder sb = new StringBuilder();

        if (pred.equals("=>")) {
            if (isNegMode) {
                sb.append(args.get(1));
                sb.append(" ");
                sb.append(AND);
                sb.append(" ");
                sb.append("~{");
                sb.append(args.get(0));
                sb.append("}");
            }
            else {
                // Special handling for Arabic.
                boolean isArabic = (language.matches(".*(?i)arabic.*")
                        || language.equalsIgnoreCase("ar"));
                sb.append("<ul><li>");
                sb.append(isArabic ? "<span dir=\"rtl\">" : "");
                sb.append(IF);
                sb.append(" ");
                sb.append(args.get(0));
                sb.append(COMMA);
                sb.append(isArabic ? "</span>" : "");
                sb.append("</li><li>");
                sb.append(isArabic ? "<span dir=\"rtl\">" : "");
                sb.append(THEN);
                sb.append(" ");
                sb.append(args.get(1));
                sb.append(isArabic ? "</span>" : "");
                sb.append("</li></ul>");
            }

            return sb.toString();
        }
        if (pred.equalsIgnoreCase("and")) {
            if (isNegMode) {
                for (int i = 0; i < args.size(); i++) {
                    if (i > 0) {
                        sb.append(" ");
                        sb.append(OR);
                        sb.append(" ");
                    }
                    sb.append("~{ ");
                    sb.append(translateWord(termMap, args.get(i)));
                    sb.append(" }");
                }
            }
            else {
                for (int i = 0; i < args.size(); i++) {
                    if (i > 0) {
                        sb.append(" ");
                        sb.append(AND);
                        sb.append(" ");
                    }
                    sb.append(translateWord(termMap, args.get(i)));
                }
            }

            return sb.toString();
        }
        if (pred.equalsIgnoreCase("holds")) {
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) {
                    if (isNegMode) {
                        sb.append(" ");
                        sb.append(NOT);
                    }
                    sb.append(" ");
                    sb.append(HOLDS);
                    sb.append(" ");
                }
                sb.append(translateWord(termMap, args.get(i)));
            }

            return sb.toString();
        }
        if (pred.equalsIgnoreCase("or")) {
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) {
                    sb.append(" ");
                    sb.append(isNegMode ? AND : OR);
                    sb.append(" ");
                }
                sb.append(translateWord(termMap, args.get(i)));
            }

            return sb.toString();
        }
        if (pred.equals("<=>")) {
            if (isNegMode) {
                sb.append(translateWord(termMap, args.get(1)));
                sb.append(" ");
                sb.append(OR);
                sb.append(" ");
                sb.append("~{ ");
                sb.append(translateWord(termMap, args.get(0)));
                sb.append(" }");
                sb.append(" ");
                sb.append(OR);
                sb.append(" ");
                sb.append(translateWord(termMap, args.get(0)));
                sb.append(" ");
                sb.append(OR);
                sb.append(" ");
                sb.append("~{ ");
                sb.append(translateWord(termMap, args.get(1)));
                sb.append(" }");
            }
            else {
                sb.append(translateWord(termMap, args.get(0)));
                sb.append(" ");
                sb.append(IFANDONLYIF);
                sb.append(" ");
                sb.append(translateWord(termMap, args.get(1)));
            }

            return sb.toString();
        }
        if (pred.equalsIgnoreCase("forall")) {
            if (isNegMode) {
                sb.append(" ");
                sb.append(NOT);
                sb.append(" ");
            }
            sb.append(FORALL);
            sb.append(" ");
            if (args.get(0).contains(" ")) {
                // If more than one variable ...
                sb.append(translateWord(termMap, NLGUtils.formatList(args.get(0), language)));
            }
            else {
                // If just one variable ...
                sb.append(translateWord(termMap, args.get(0)));
            }
            sb.append(" ");
            sb.append(translateWord(termMap, args.get(1)));

            return sb.toString();
        }
        if (pred.equalsIgnoreCase("exists")) {
            if (args.get(0).contains(" ")) {
                // If more than one variable ...
                sb.append(isNegMode ? NOTEXIST : EXIST);
                sb.append(" ");
                sb.append(translateWord(termMap, NLGUtils.formatList(args.get(0), language)));
            }
            else {
                // If just one variable ...
                sb.append(isNegMode ? NOTEXISTS : EXISTS);
                sb.append(" ");
                sb.append(translateWord(termMap, args.get(0)));
            }
            sb.append(" ");
            sb.append(SUCHTHAT);
            sb.append(" ");
            sb.append(translateWord(termMap, args.get(1)));

            return sb.toString();
        }
        return "";
    }

    /** ***************************************************************
     * Create a natural language paraphrase of a logical statement, where the
     * predicate is not a logical operator.  Use a printf-like format string to generate
     * the paraphrase.
     * @param stmt the statement to format
     * @param isNegMode whether the statement is negated, and therefore requiring special formatting.
     * @return the paraphrased statement.
     */
    private String paraphraseWithFormat(String stmt, boolean isNegMode) {

        if (debug) System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): Statement: " + stmt);
        //System.out.println("neg mode: " + isNegMode);
        Formula f = new Formula();
        f.read(stmt);
        String pred = f.car();
        String strFormat = phraseMap.get(pred);
        //System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): 1 format: " + strFormat);
        // System.out.println("str format: " + strFormat);
        //int index;

        if (strFormat.contains("&%"))                    // setup the term hyperlink
            strFormat = strFormat.replaceAll("&%(\\w+)","&%" + pred + "\\$\"$1\"");

        //System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): 2 format: " + strFormat);
        if (isNegMode) {                                    // handle negation
            if (!strFormat.contains("%n")) {
                strFormat = NLGUtils.getKeyword("not", language) + " " + strFormat;
            }
            else {
                if (!strFormat.contains("%n{")) {
                    strFormat = strFormat.replace("%n", NLGUtils.getKeyword("not", language));
                }
                else {
                    int start = strFormat.indexOf("%n{") + 3;
                    int end = strFormat.indexOf("}",start);
                    strFormat = (strFormat.substring(0,start-3)
                            + strFormat.substring(start,end)
                            + strFormat.substring(end+1,strFormat.length()));
                }
            }
            // delete all the unused positive commands
            isNegMode = false;
            // strFormat = strFormat.replace("%p ","");
            // strFormat = strFormat.replaceAll(" %p\\{[\\w\\']+\\} "," ");
            // strFormat = strFormat.replaceAll("%p\\{[\\w\\']+\\} "," ");
            strFormat = strFormat.replaceAll(" %p\\{.+?\\} "," ");
            strFormat = strFormat.replaceAll("%p\\{.+?\\} "," ");
        }
        else {
            // delete all the unused negative commands
            strFormat = strFormat.replace(" %n "," ");
            strFormat = strFormat.replace("%n "," ");
            // strFormat = strFormat.replaceAll(" %n\\{[\\w\\']+\\} "," ");
            // strFormat = strFormat.replaceAll("%n\\{[\\w\\']+\\} "," ");
            strFormat = strFormat.replaceAll(" %n\\{.+?\\} "," ");
            strFormat = strFormat.replaceAll("%n\\{.+?\\} "," ");

            if (strFormat.contains("%p{")) {
                int start = strFormat.indexOf("%p{") + 3;
                int end = strFormat.indexOf("}", start);
                strFormat = (strFormat.substring(0, start-3)
                        + strFormat.substring(start, end)
                        + strFormat.substring(end+1, strFormat.length()));
            }
        }

        //System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): 3 format: " + strFormat);
        if (strFormat.contains("%*"))
            strFormat = NLGUtils.expandStar(f, strFormat, language);

        //System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): 3.5 format: " + strFormat);
        int num = 1;                                          // handle arguments
        String argPointer = ("%" + num);
        String arg = "";
        String para = "";
        while (strFormat.contains(argPointer)) {
            if (debug) System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): Statement: " + f.getFormula());
            if (debug) System.out.println("arg: " + f.getArgument(num));
            if (debug) System.out.println("num: " + num);
            if (debug) System.out.println("str: " + strFormat);
            arg = f.getStringArgument(num);
            if (Formula.isVariable(arg))
                para = arg;
            else
                para = paraphraseStatement(arg, isNegMode, 1);
            if (debug) System.out.println("para: " + para);
            //outputMap.add(new AVPair(pred + "-" + argPointer, para));
            //System.out.println("para: " + para);
            //if (!Formula.atom(para)) {
                // Add the hyperlink placeholder for arg.
           //     if (Formula.isVariable(argPointer))
           //         strFormat = strFormat.replace(argPointer, para);
            //    else {
                    /**
                     List splitPara = Arrays.asList(para.split("\\s+"));
                     System.out.println("splitPara == " + splitPara);
                     StringBuilder pb = new StringBuilder();
                     int spLen = splitPara.size();
                     for (int i = 0; i < spLen; i++) {
                     if (i > 0) {
                     pb.append(" ");
                     }
                     pb.append("&%");
                     pb.append(arg);
                     pb.append("$");
                     pb.append((String) splitPara.get(i));
                     }
                     */
            //        strFormat = strFormat.replace(argPointer, para);
            //    }
            //}
            //else
            strFormat = strFormat.replace(argPointer,para);
            CoreLabel cl = new CoreLabel();
            cl.setValue(para);
            cl.set(RelationArgumentAnnotation.class,num);
            outputMap.put(para,cl);
            num++;
            argPointer = ("%" + num);
        }
        return strFormat;
    }

    /** ***************************************************************
     * Process an atom into an appropriate NL string.  If a URL, add
     * spaces for readability.  Return variable unaltered.  Add
     * term format string to all other atoms.
     */
    private static String processAtom(String atom, Map<String, String> termMap) {

        String result = atom;
        String unquoted = StringUtil.removeEnclosingQuotes(atom);
        boolean isNumber = false;
        try {
            Double dbl = Double.valueOf(unquoted);
            isNumber = !dbl.isNaN();
        }
        catch (Exception nex) {
            isNumber = false;
        }
        if (isNumber)
            ; // do nothing
        else if (StringUtil.isQuotedString(atom)) {
            if (unquoted.startsWith("http")) {
                StringBuilder formatted = new StringBuilder(atom);
                if (formatted.length() > 50) {
                    for (int i = 50; i < formatted.length(); i++) {
                        if (i > 50 && formatted.charAt(i) == '/')
                            // add spaces to long URL strings
                            formatted = formatted.insert(i+1,' ');
                    }
                    result = formatted.toString();
                }
            }
        }
        else if (Formula.isVariable(atom))
            ; // do nothing
        else if (StringUtil.isDigitString(unquoted))
            ; // do nothing
        else if (termMap.containsKey(atom)) {
            String formattedString = termMap.get(atom);
            result = ("&%" + atom + "$\"" + formattedString + "\"");
        }
        else
            result = ("&%" + atom + "$\"" + atom + "\"");
        return result;
    }

    /** ***************************************************************
     * Return the NL format of an individual word.
     */
    public static String translateWord(Map<String,String> termMap, String word) {

        String ans = word;
        try {
            if (!Formula.isVariable(word) && (termMap != null)) {
                String pph = termMap.get(word);
                if (StringUtil.isNonEmptyString(pph)) 
                    ans = pph;                
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     * Remove the type declarations from the text
     */
    public static String removePreamble(String input) {

        if (input.contains("such that"))
            input = input.substring(input.indexOf("such that") + 10,input.length());
        return input;
    }

    /** ***************************************************************
     * create a map from numbered tokens to their types
     */
    private static void createObjectMap(String form) {

        form = removePreamble(form);
        form = form + " ";
        if (debug) System.out.println("LanguageFormatter.createObjectMap(): input " + form);
        int tokenNum = 1;
        boolean inQuote = false;
        int index = 0;
        StringBuffer termBuffer = new StringBuffer();
        while (index < form.length()) {
            //System.out.println(index);
            if (form.charAt(index) == '"') {
                System.out.println("Error in LanguageFormatter.createObjectMap(): premature quote at index " +
                        index + " in " + form);
                return;
            }
            else if (form.charAt(index) == ' ') {
                CoreLabel cl = new CoreLabel();
                cl.setOriginalText(termBuffer.toString());
                cl.setValue(termBuffer.toString());
                cl.setIndex(tokenNum);
                if (debug) System.out.println("LanguageFormatter.createObjectMap(): CoreLabel: " + cl);
                if (debug) System.out.println("LanguageFormatter.createObjectMap(): termBuffer: " + termBuffer);
                outputMap.put(termBuffer.toString(),cl);
                termBuffer = new StringBuffer();
                tokenNum++;
                index++;
            }
            else if (form.charAt(index) == '&') {
                index++;
                if (form.charAt(index) != '%') {
                    System.out.println("Error in LanguageFormatter.createObjectMap(): bad symbol " +
                            form.charAt(index) + " in " + form);
                    return;
                }
                index++;
                int termEnd = form.indexOf("$",index);
                if (termEnd == -1) {
                    System.out.println("Error in LanguageFormatter.createObjectMap(): missing $ in " +
                            form + " after " + index);
                    return;
                }
                String term = form.substring(index,termEnd);
                int quote1 = termEnd + 1;
                if (form.charAt(quote1) != '"') {
                    System.out.println("Error in LanguageFormatter.createObjectMap(): bad symbol " +
                            form.charAt(quote1) +" at index " + quote1 + " in " + form);
                    return;
                }
                int phraseStart = quote1 + 1;
                int phraseEnd = form.indexOf("\"",phraseStart);
                String phrase = form.substring(phraseStart,phraseEnd);
                phrase = StringUtil.removeDoubleSpaces(phrase);
                int argnum = -1;
                if (Character.isDigit(form.charAt(phraseEnd+1)))
                    argnum = Integer.parseInt(Character.toString(form.charAt(phraseEnd+1)));
                String[] phraseWords = phrase.split(" ");
                for (String s : phraseWords) {
                    CoreLabel cl = new CoreLabel();
                    cl.setOriginalText(s);
                    cl.setValue(s);
                    cl.setIndex(tokenNum);
                    cl.set(RelationArgumentAnnotation.class,argnum);
                    if (!kb.isInstanceOf(term,"Relation"))
                        cl.setCategory(term);
                    else
                        if (debug) System.out.println("LanguageFormatter.createObjectMap(): " + term + " is a relation");
                    if (debug) System.out.println("LanguageFormatter.createObjectMap(): argnum: " +  cl.get(RelationArgumentAnnotation.class));
                    if (debug) System.out.println("LanguageFormatter.createObjectMap(): category: " + term);
                    outputMap.put(cl.toString(),cl);
                    if (debug) System.out.println("LanguageFormatter.createObjectMap(): CoreLabel: " + cl.toString());
                    //outputMap.put(s + "-" + Integer.toString(tokenNum),term);
                    tokenNum++;
                }
                index = phraseEnd + 1;
                if (argnum != -1)
                    index++;
                if (index < form.length()-1 && form.charAt(index) == ' ')
                    index++;
            }
            else {
                termBuffer.append(form.charAt(index));
                index++;
            }
        }
        if (debug) System.out.println("LanguageFormatter.createObjectMap(): result " + outputMap);
    }

    /** **************************************************************
     * Replace variables with types, and articles appropriate to how many times
     * they have occurred.
     *
     * There is a known bug where variables that are a substring of each other
     * causes problems.
     * @param typeMap is a map with variable type keys that counts
     *                how many times variables of the given type
     *                appears in the paraphrase.  This is done in
     *                order to state "an entity, another entity, a
     *                third entity" etc.
     */
    private static String incrementalVarReplace(String form, String varString, String varType,
                                                String varPretty, String language,
                                                boolean isClass, HashMap<String,Integer> typeMap) {

        String argNumStr = "";
        if (debug) System.out.println("LanguageFormatter.incrementalVarReplace(): form " + form);
        if (debug) System.out.println("LanguageFormatter.incrementalVarReplace(): varString " + varString);
        if (debug) System.out.println("LanguageFormatter.incrementalVarReplace(): varType " + varType);
        if (debug) System.out.println("LanguageFormatter.incrementalVarReplace(): varPretty " + varPretty);
        if (outputMap.keySet().contains(varString)) {
            CoreLabel cl = outputMap.get(varString);
            cl.setOriginalText(varPretty);
            cl.setValue(varPretty);
            if (debug) System.out.println("LanguageFormatter.incrementalVarReplace(): arg num " +
                    cl.get(RelationArgumentAnnotation.class));
            argNumStr = Integer.toString(cl.get(RelationArgumentAnnotation.class));
            outputMap.put(varString, cl); // create a "dummy" CoreLabel to hold the variable value
        }
        if (form.indexOf("?") < 0) // if there are variables, the replacements are not done yet
            createObjectMap(form);
        String result = form;
        // Make necessary changes if the variable is a quoted string, i.e. a name.

        if (varPretty == null && varType.matches("\".*\""))    {
            String name = varType.substring(1, varType.length() - 1);
            result = result.replaceAll("\\?" + varString.substring(1), name);
            if (debug) System.out.println("LanguageFormatter.incrementalVarReplace(): result " + result);
            return result;
        }

        boolean isArabic = (language.matches(".*(?i)arabic.*")
                || language.equalsIgnoreCase("ar"));
        if (StringUtil.emptyString(varPretty)) 
            varPretty = varType;        
        boolean found = true;
        int occurrenceCounter = 1;
        if (typeMap.keySet().contains(varType)) {
            occurrenceCounter = typeMap.get(varType);
            occurrenceCounter++;
            typeMap.put(varType,Integer.valueOf(occurrenceCounter));
        }
        else
            typeMap.put(varType,Integer.valueOf(1));
        int count = 1;
        while (found) {
            if (result.contains(varString) && count < 20) {
                String article = "";
                String replacement = "";
                if (isClass) {
                    article = NLGUtils.getArticle("kind", count, occurrenceCounter, language);
                    replacement = (article + " " + NLGUtils.getKeyword("kind of", language)
                            + " " + varPretty);
                    if (isArabic)
                        replacement = (NLGUtils.getKeyword("kind of", language) + " " + varPretty);
                    result =
                        result.replaceFirst(("\\?" + varString.substring(1)),
                                ("\\&\\%" + varType + "\\$\"" + replacement + "\"" + argNumStr));
                }
                else {
                    article = NLGUtils.getArticle(varPretty, count, occurrenceCounter, language);
                    replacement = (article + " " + varPretty);
                    if (isArabic) {
                        String defArt = NLGUtils.getKeyword("the", language);
                        if (article.startsWith(defArt) && !varPretty.startsWith(defArt)) {
                            // This has to be refined to insert shadda for sun letters.
                            varPretty = (defArt + varPretty);
                        }
                        replacement = (varPretty + " " + article);
                    }
                }
                result = result.replaceFirst(("\\?" + varString.substring(1)),
                        ("\\&\\%" + varType + "\\$\"" + replacement + "\"" + argNumStr));
            }
            else
                found = false;
            count++;
        }
        if (debug) System.out.println("LanguageFormatter.incrementalVarReplace(): result (2) " + result);
        return result;
    }

    /** **************************************************************
     * Replace variables in a formula with paraphrases expressing their
     * type.
     */
    public static String variableReplace(String form, Map<String, Set<String>> instMap,
            HashMap<String, Set<String>> classMap, KB kb, String language) {

        String result = form;
        HashMap<String,Integer> typeMap = new HashMap<>();
        ArrayList<String> varList = NLGUtils.collectOrderedVariables(form);
        Iterator<String> it = varList.iterator();
        while (it.hasNext()) {
            String varString = it.next();
            if (StringUtil.isNonEmptyString(varString)) {
                Set<String> instanceArray = instMap.get(varString);
                Set<String> subclassArray = classMap.get(varString);

                if (subclassArray != null && !subclassArray.isEmpty()) {
                    String varType = (String) subclassArray.toArray()[0];
                    String varPretty = kb.getTermFormatMap(language).get(varType);
                    result = incrementalVarReplace(result, varString, varType, varPretty, language, true, typeMap);
                }
                else {
                    if (instanceArray != null && !instanceArray.isEmpty()) {
                        String varType = (String) instanceArray.toArray()[0];
                        String varPretty = kb.getTermFormatMap(language).get(varType);
                        result = incrementalVarReplace(result, varString, varType, varPretty, language, false, typeMap);
                    }
                    else {
                        String varPretty = kb.getTermFormatMap(language).get("Entity");
                        if (StringUtil.emptyString(varPretty))
                            varPretty = "entity";
                        result = incrementalVarReplace(result, varString, "Entity", varPretty, language, false, typeMap);
                    }
                }         
            }
        }
        if (debug) System.out.println("LanguageFormatter.variableReplace(): " + result);
        return result;
    }

    /** **************************************************************
     */
    public static void test1() {

        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        // INFO in LanguageFormatter.paraphraseLogicalOperator(): bad result for
        String stmt =  "(and (instance ?GUIE1 GUIElement) (hasGUEState ?GUIE1 GUE_ActiveState)" +
                " (properPart ?GUIE1 ?GUIE2) (instance ?GUIE2 GUIElement))";
        Formula f = new Formula(stmt);
        System.out.println("Formula: " + f.getFormula());
        System.out.println("result: " + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        System.out.println();
    }

    /** **************************************************************
     */
    public static void test2() {

        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        String stmt = "(exists (?D ?H)\n" +
                "   (and\n" +
                "       (instance ?D Driving)\n" +
                "       (instance ?H Human)\n" +
                "       (agent ?D ?H)))";
        Formula f = new Formula(stmt);
        System.out.println("Formula: " + f.getFormula());
        System.out.println("result: " + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        //LanguageFormatter lf = new LanguageFormatter(stmt, kb.getFormatMap("EnglishLanguage"),
        //        kb.getTermFormatMap("EnglishLanguage"),kb,"EnglishLanguage");
        //System.out.println(lf.paraphraseStatement(stmt,false,0));
        System.out.println();

        stmt = "(exists (?D ?H)\n" +
                "   (and\n" +
                "       (instance ?D Driving)\n" +
                "       (instance ?H Human)\n" +
                "       (names ?H \"John\")\n" +
                "       (agent ?D ?H)))";
        f = new Formula();
        f.read(stmt);
        System.out.println("Formula: " + f.getFormula());
        System.out.println("result: " + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        System.out.println();

        stmt = "(exists (?D ?H ?Car)\n" +
                "   (and\n" +
                "       (instance ?D Driving)\n" +
                "       (instance ?H Human)\n" +
                "       (names ?H \"John\")\n" +
                "       (instance ?Car Automobile)\n" +
                "       (agent ?D ?H)\n" +
                "       (patient ?D ?Car)))";
        f = new Formula();
        f.read(stmt);
        System.out.println("Formula: " + f.getFormula());
        System.out.println("result: " + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        System.out.println();

        stmt = "(exists (?D ?H ?C ?A)\n" +
                "   (and\n" +
                "       (instance ?D Driving)\n" +
                "       (instance ?H Human)\n" +
                "       (names ?H \"John\")\n" +
                "       (instance ?C Automobile)\n" +
                "       (instance ?A Airport)\n" +
                "       (agent ?D ?H)\n" +
                "       (destination ?D ?A)\n" +
                "       (patient ?D ?C)))";
        f = new Formula();
        f.read(stmt);
        System.out.println("Formula: " + f.getFormula());
        System.out.println("result: " + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        System.out.println();
    }

    /** **************************************************************
     */
    public static void test3() {

        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        debug = true;
        // INFO in LanguageFormatter.paraphraseLogicalOperator(): bad result for
        String stmt =  "(exists (?FINANCIALTRANSACTION1 ?AGENT2 ) (broker ?FINANCIALTRANSACTION1 ?AGENT2 ))";
        Formula f = new Formula(stmt);
        System.out.println("Formula: " + f.getFormula());
        System.out.println("result: " + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        System.out.println(NLGUtils.outputMap);
        System.out.println();

        stmt =  "(exists (?MOTION1 ?OBJECT2 ) (moves ?MOTION1 ?OBJECT2 ))";
        f = new Formula(stmt);
        System.out.println("Formula: " + f.getFormula());
        System.out.println("result: " + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        System.out.println(NLGUtils.outputMap);
        System.out.println();
    }

    /** ***************************************************************
     * generate English paraphrase
     */
    public static String toEnglish(String form) {

        return NLGUtils.htmlParaphrase("", form, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage") + "\n";
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("Language generation ");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -t - run test");
        System.out.println("  -g \"<formula>\" - generate English from formula");
    }

    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {

        System.out.println("INFO in Graph.main()");
        if (args != null && args.length > 1 && args[0].equals("-h")) {
            showHelp();
        }
        else if (args.length > 1 && args[0].equals("-g")) {
            KBmanager.getMgr().initializeOnce();
            Formula f = new Formula(args[1]);
            kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            System.out.println("translation for\n" + f);
            String actual = toEnglish(StringUtil.removeEnclosingQuotes(args[1]));
            System.out.println(StringUtil.filterHtml(actual));
        }
        else {
            test3();
        }
    }
}

