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

package com.articulate.sigma;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

/** ***************************************************************
 *  A class that handles the generation of natural language from logic.
 *
 *  @author Adam Pease - apease [at] articulatesoftware [dot] com, with thanks
 *  to Michal Sevcenko - sevcenko@vc.cvut.cz for development of the
 *  formatting language.
 */
public class LanguageFormatter {
    private static final String SIGMA_HOME = System.getenv("SIGMA_HOME");
    private static final String KB_PATH = (new File(SIGMA_HOME, "KBs")).getAbsolutePath();

    private static HashMap<String,HashMap<String,String>> keywordMap = null;

    private static final String PHRASES_FILENAME = "language.txt";


    private String statement;

    private Map<String, String> phraseMap;

    // kb.getTermFormatMap() for this language
    private Map<String, String> termMap;

    private KB kb;
    private String language;

    HashMap<String, HashSet<String>> variableTypes;

    Map<String, HashSet<String>> variableToInstanceMap;

    // FIXME: Temporarily disable NLG for certain logical operators.
    static final List<String> notReadyOperators = Lists.newArrayList("not", "=>", "<=>");

    /**
     * Toggle on and off the latest NLG work.
     */
    boolean doNLG = true;

    /**
     * The stack holds information on the formula being processed, so that successive recursive calls can refer to it.
     * FIXME: This should probably be moved into a separate object.
     */
    private List<StackElement> theStack = Lists.newArrayList();

    private class StackElement  {
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


    /*******************************************************************************
     *
     * @param stmt The statement to be formatted.
     * @param phraseMap kb.getFormatMap() for this language
     * @param termMap kb.getTermFormatMap() for this language
     * @param kb
     * @param language
     */
    public LanguageFormatter(String stmt, Map<String, String> phraseMap, Map<String, String> termMap,
                             KB kb, String language) {
        this.statement = stmt;
        this.phraseMap = phraseMap;
        this.termMap = termMap;
        this.kb = kb;
        this.language = language;

        Formula f = new Formula();
        f.read(statement);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        variableTypes = fp.computeVariableTypes(f, kb);

        variableToInstanceMap = fp.findExplicitTypes(f);
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
    public String htmlParaphrase(String href)     {

        String nlFormat = "";
        try {
            String template = nlStmtPara(statement, false, 1);

            if (StringUtil.isNonEmptyString(template)) {
                String anchorStart = ("<a href=\"" + href + "&term=");
                Formula f = new Formula();
                f.read(statement);
                FormulaPreprocessor fp = new FormulaPreprocessor();
                //HashMap varMap = fp.computeVariableTypes(kb);
                HashMap<String, HashSet<String>> instanceMap = new HashMap<String, HashSet<String>>();
                HashMap<String, HashSet<String>> classMap = new HashMap<String, HashSet<String>>();
                HashMap<String, HashSet<String>> types = fp.computeVariableTypes(f, kb);
                Iterator<String> it = types.keySet().iterator();
                while (it.hasNext()) {
                    String var = it.next();
                    HashSet<String> typeList = types.get(var);
                    Iterator<String> it2 = typeList.iterator();
                    while (it2.hasNext()) {
                        String t = it2.next();
                        if (t.endsWith("+")) {
                            HashSet<String> values = new HashSet<String>();
                            if (classMap.containsKey(var))
                                values = classMap.get(var);
                            values.add(t.substring(0, t.length()-1));
                            classMap.put(var, values);
                        }
                        else {
                            HashSet<String> values = new HashSet<String>();
                            if (instanceMap.containsKey(var))
                                values = instanceMap.get(var);
                            values.add(t);
                            instanceMap.put(var, values);
                        }
                    }
                }
                if ((instanceMap != null && !instanceMap.isEmpty()) || (classMap != null && !classMap.isEmpty()))
                    template = variableReplace(template, instanceMap, classMap, kb, language);
                StringBuilder sb = new StringBuilder(template);
                int sblen = sb.length();
                String titok = "&%";
                int titoklen = titok.length();
                String ditok = "$\"";
                int ditoklen = ditok.length();
                String dktok = "\"";
                int dktoklen = dktok.length();
                int prevti = -1;
                int ti = 0;
                int tj = -1;
                int di = -1;
                int dj = -1;
                int dk = -1;
                int dl = -1;
                // The indexed positions: &%termNameString$"termDisplayString"  ti tj   di dj  dk dl
                while (((ti = sb.indexOf(titok, ti)) != -1) && (prevti != ti)) {
                    prevti = ti;
                    tj = (ti + titoklen);
                    if (tj >= sblen)
                        break;
                    di = sb.indexOf(ditok, tj);
                    if (di == -1)
                        break;
                    String termName = sb.substring(tj, di);
                    dj = (di + ditoklen);
                    if (dj >= sblen)
                        break;
                    dk = sb.indexOf(dktok, dj);
                    if (dk == -1)
                        break;
                    String displayName = sb.substring(dj, dk);
                    if (StringUtil.emptyString(displayName))
                        displayName = termName;
                    StringBuilder rsb = new StringBuilder();
                    rsb.append(anchorStart);
                    rsb.append(termName);
                    rsb.append("\">");
                    rsb.append(displayName);
                    rsb.append("</a>");
                    dl = (dk + dktoklen);
                    if (dl > sblen)
                        break;
                    int rsblen = rsb.length();
                    sb = sb.replace(ti, dl, rsb.toString());
                    sblen = sb.length();
                    ti = (ti + rsblen);
                    if (ti >= sblen)
                        break;
                    tj = -1;
                    di = -1;
                    dj = -1;
                    dk = -1;
                    dl = -1;
                }
                nlFormat = sb.toString();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return nlFormat;
    }

    /** ***************************************************************
     * Create a natural language paraphrase of a logical statement.
     *  @param stmt The statement to be paraphrased.
     *  @param isNegMode Whether the statement is negated.
     *  @param depth An int indicating the level of nesting, for control of indentation.
     *  @return A String, which is the paraphrased statement.
     */
    protected String nlStmtPara(String stmt, boolean isNegMode, int depth) {

        //System.out.println("INFO in LanguageFormatter.nlStmtPara(): stmt: " + stmt);
        if (Formula.empty(stmt)) {
            System.out.println("Error in LanguageFormatter.nlStmtPara(): stmt is empty");
            return "";
        }
        boolean alreadyTried = kb.loadFormatMapsAttempted.contains(language);
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
        Formula f = new Formula();
        f.read(stmt);

        if (f.atom()) {
            ans = processAtom(stmt,termMap,language);
            return ans;
        }
        else {
            if (!f.listP()) {
                System.out.println("Error in LanguageFormatter.nlStmtPara(): "
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

        // FIXME: test for case role here
        if(kb.kbCache.isInstanceOf(pred, "CaseRole") && SumoProcessCollector.isKnownRole(pred))    {

            try {
                if(! theStack.isEmpty())  {
                    String caseArgument = f.cadr() + " " + f.caddr();
                    String[] caseArgs = caseArgument.trim().split(" ");
                    String processInstanceName = caseArgs[0];
                    String processParticipant = caseArgs[1];

                    if(variableTypes.containsKey(processInstanceName))    {

                        // FIXME: candidate for separate (static?) method that can be unit tested.
                        HashSet<String> vals = variableTypes.get(processInstanceName);
                        if(vals.contains("Process")) {
                            // Does this process already exist on the stack?
                            StackElement element = theStack.get(theStack.size() - 1);
                            Map<String, SumoProcessCollector> thisProcessMap = element.sumoProcessMap;

                            // Mark the argument as PROCESSED.
                            // The relevant args are not held at top of stack, but at top - 1
                            List<String> stackArgs = theStack.get(theStack.size() - 2).formulaArgs;
                            if (stackArgs.contains(f.theFormula)) {
                                int idx = stackArgs.indexOf(f.theFormula);
                                String temp = "PROCESSED: " + stackArgs.remove(idx);
                                stackArgs.add(idx, temp);
                            }

                            if (thisProcessMap.containsKey(processInstanceName)) {
                                // Already there. Add new information if any exists.
                                SumoProcessCollector sProcess = thisProcessMap.get(processInstanceName);
                                String resolvedParticipant = resolveVariable(processParticipant);
                                sProcess.addRole(pred, resolvedParticipant);
                            } else {
                                // Insert into stack.
                                String resolvedProcessName = resolveVariable(processInstanceName);
                                String resolvedParticipant = resolveVariable(processParticipant);
                                SumoProcessCollector sProcess = new SumoProcessCollector(kb, pred, resolvedProcessName, resolvedParticipant);
                                thisProcessMap.put(processInstanceName, sProcess);
                            }
                        }
                    }

                }
            } catch (IllegalArgumentException e) {
                // Recover from exception by turning off full NLG.
                this.doNLG = false;
                //theStack.clear();
                String msg = "Handled IllegalArgumentException after finding case role.\n   Exception message:\n      " + e.getMessage() + "\n" +
                        "   Formula:\n       " + statement + "\n";
                System.out.println(msg);
            }
        }

        // Mark the predicate as processed.
        // FIXME: handle other predicates here: located, orientation, etc.
        if(pred.equals("instance")) {
            if(theStack.size() >= 2) {
                // The relevant args are not held at top of stack, but at top - 1
                List<String> stackArgs = theStack.get(theStack.size() - 2).formulaArgs;
                if (stackArgs.contains(stmt)) {
                    int idx = stackArgs.indexOf(stmt);
                    String temp = "PROCESSED: " + stackArgs.remove(idx);
                    stackArgs.add(idx, temp);
                }
            }
        }

        if (!Formula.atom(pred)) {
            System.out.println("Error in LanguageFormatter.nlStmtPara(): statement "
                    + stmt
                    + " has a formula in the predicate position.");
            return stmt;
        }

        if (logicalOperator(pred)) {
            if(! theStack.isEmpty()) {
                // Put the args list into the stack for later reference.
                List<String> args = f.complexArgumentsToArrayList(1);
                StackElement element = this.theStack.get(theStack.size() - 1);
                element.formulaArgs = args;
            }

            ans = paraphraseLogicalOperator(stmt, isNegMode, depth+1);

            return ans;
        }

        if (phraseMap.containsKey(pred)) {
            ans = paraphraseWithFormat(stmt, isNegMode);
            return ans;
        }
        else {                              // predicate has no paraphrase
            if (Formula.isVariable(pred))
                result.append(pred);
            else {
                result.append(processAtom(pred, termMap, language));
            }
            f.read(f.cdr());
            while (!f.empty()) {
                /*
                System.out.println("INFO in LanguageFormatter.nlStmtPara(): stmt: " + f);
                System.out.println("length: " + f.listLength());
                System.out.println("result: " + result);
                 */
                String arg = f.car();
                f.read(f.cdr());
                result.append(" ");
                if (Formula.atom(arg))
                    result.append(processAtom(arg,termMap,language));
                else
                    result.append(nlStmtPara(arg, isNegMode, depth+1));
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

    /** ***************************************************************
     * Uses variableToInstanceMap to resolve a given variable.
     * If more than one entry is in the map, currently takes the first one returned by the iterator.
     * @param input
     * @return
     */
    private String resolveVariable(String input) {
        String retVal = input;

        if(variableToInstanceMap.containsKey(input))    {
            Set<String> values = variableToInstanceMap.get(input);
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
            if (keywordMap == null) {
                System.out.println("Error in LanguageFormatter.paraphraseLogicalOperator(): " + "keywordMap is null");
                return null;
            }
            ArrayList<String> args = new ArrayList<String>();
            Formula f = new Formula();
            f.read(stmt);
            String pred = f.getArgument(0);
            f.read(f.cdr());

            String ans = null;

            if (LanguageFormatter.notReadyOperators.contains(pred))   {
                doNLG = false;
            }

            if (pred.equals("not")) {
                ans = nlStmtPara(f.car(), true, depth+1);
                return ans;
            }

            // Push new element onto the stack.
            Map<String, SumoProcessCollector> nextProcessMap = Maps.newHashMap();
            List<String> argsList = Lists.newArrayList();
            StackElement inElement = new StackElement(nextProcessMap, argsList);
            theStack.add(inElement);

            try {

                String arg = null;
                while (!f.empty()) {
                    arg = f.car();
                    String result = nlStmtPara(arg, false, depth + 1);
                    if (StringUtil.isNonEmptyString(result))
                        args.add(result);
                    else {
                        System.out.println("INFO in LanguageFormatter.paraphraseLogicalOperators(): "
                                + "bad result for \"" + arg + "\": " + result);
                        // TODO: We should probably return empty string at this point, like nlStmtPara( ), instead of continuing processing.
                        return "";
                    }
                    f.read(f.cdr());
                }

                // Perform natural language output.
                if (doNLG) {
                    StackElement outElement = theStack.get(theStack.size() - 1);
                    Map<String, SumoProcessCollector> thisProcessMap = outElement.sumoProcessMap;

                    if(thisProcessMap != null) {

                        if(! thisProcessMap.isEmpty()) {
                            // Only perform NLG if the args list has been fully processed.
                            boolean doIt = false;   // TODO: not sure if this should be true or false
                            if(theStack.size() >= 2) {
                                StackElement previousElement = theStack.get(theStack.size() - 2);
                                doIt = allArgsProcessed(previousElement.formulaArgs);
                            }

                            if (doIt) {
                                // Try to do natural language generation on top element of stack.
                                StringBuilder sb = new StringBuilder();
                                for (SumoProcessCollector process : thisProcessMap.values()) {
                                    //SumoProcess process = thisProcessMap.get(thisProcessMap.keySet().iterator().next());
                                    String naturalLanguage = process.toNaturalLanguage();
                                    if (!naturalLanguage.isEmpty()) {
                                        sb.append(naturalLanguage).append(" and ");
                                    }
                                }
                                // Remove last "and" if it exists.
                                String output = sb.toString().replaceAll(" and $", "");
                                if (!output.isEmpty()) {

                                    // FIXME: clear the arguments
//                                    if(theStack.size() >= 2) {
//                                        StackElement previousElement = theStack.get(theStack.size() - 2);
//                                        previousElement.formulaArgs.clear();
//                                    }
//                                    if (theStack.contains(inElement)) {
//                                        StackElement thisElement = theStack.get(theStack.indexOf(inElement));
//                                        thisElement.formulaArgs.clear();
//                                    }

                                    return output;
                                }
                            }
                        }
                        else    {
                            // See if the last recursive call processed all the clause arguments.
                            if (allArgsProcessed(outElement.formulaArgs)) {
                                // FIXME: clear the arguments
//                                if (theStack.size() >= 2) {
//                                if (theStack.contains(inElement)) {
//                                    StackElement thisElement = theStack.get(theStack.indexOf(inElement));
//                                    thisElement.formulaArgs.clear();
//                                }

                                return args.get(args.size() - 1);
                            }
                        }
                    }
                }

            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            } finally {
                // Clear the stack.
                // Something's wrong if the top element of the stack isn't inElement.
                if(theStack.indexOf(inElement) != theStack.size() - 1) {
                    throw new IllegalStateException("Current element of stack is not the top element.");
                }
                if(! theStack.remove(inElement))   {
                    throw new IllegalStateException("Unable to pop the stack.");
                }
            }

            // FIXME: put the below into a separate method
            String COMMA = getKeyword(",",language);
            //String QUESTION = getKeyword("?",language);
            String IF = getKeyword("if",language);
            String THEN = getKeyword("then",language);
            String AND = getKeyword("and",language);
            String OR = getKeyword("or",language);
            String IFANDONLYIF = getKeyword("if and only if",language);
            String NOT = getKeyword("not",language);
            String FORALL = getKeyword("for all",language);
            String EXISTS = getKeyword("there exists",language);
            String EXIST = getKeyword("there exist",language);
            String NOTEXIST = getKeyword("there don't exist",language);
            String NOTEXISTS = getKeyword("there doesn't exist",language);
            String HOLDS = getKeyword("holds",language);
            String SOTHAT = getKeyword("so that",language);
            String SUCHTHAT = getKeyword("such that",language);
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
                ans = sb.toString();
                return ans;
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
                        sb.append(translateWord(termMap,(String) args.get(i),language));
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
                        sb.append(translateWord(termMap,(String) args.get(i),language));
                    }
                }
                ans = sb.toString();
                return ans;
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
                    sb.append(translateWord(termMap,(String) args.get(i),language));
                }
                ans = sb.toString();
                return ans;
            }
            if (pred.equalsIgnoreCase("or")) {
                for (int i = 0; i < args.size(); i++) {
                    if (i > 0) {
                        sb.append(" ");
                        sb.append(isNegMode ? AND : OR);
                        sb.append(" ");
                    }
                    sb.append(translateWord(termMap,(String) args.get(i),language));
                }
                ans = sb.toString();
                return ans;
            }
            if (pred.equals("<=>")) {
                if (isNegMode) {
                    sb.append(translateWord(termMap,(String) args.get(1),language));
                    sb.append(" ");
                    sb.append(OR);
                    sb.append(" ");
                    sb.append("~{ ");
                    sb.append(translateWord(termMap,(String) args.get(0),language));
                    sb.append(" }");
                    sb.append(" ");
                    sb.append(OR);
                    sb.append(" ");
                    sb.append(translateWord(termMap,(String) args.get(0),language));
                    sb.append(" ");
                    sb.append(OR);
                    sb.append(" ");
                    sb.append("~{ ");
                    sb.append(translateWord(termMap,(String) args.get(1),language));
                    sb.append(" }");
                }
                else {
                    sb.append(translateWord(termMap,(String) args.get(0),language));
                    sb.append(" ");
                    sb.append(IFANDONLYIF);
                    sb.append(" ");
                    sb.append(translateWord(termMap,(String) args.get(1),language));
                }
                ans = sb.toString();
                return ans;
            }
            if (pred.equalsIgnoreCase("forall")) {
                if (isNegMode) {
                    sb.append(" ");
                    sb.append(NOT);
                    sb.append(" ");
                }
                sb.append(FORALL);
                sb.append(" ");
                if (((String) args.get(0)).contains(" ")) {
                    // If more than one variable ...
                    sb.append(translateWord(termMap,formatList((String) args.get(0), language), language));
                }
                else {
                    // If just one variable ...
                    sb.append(translateWord(termMap,(String) args.get(0),language));
                }
                sb.append(" ");
                sb.append(translateWord(termMap,(String) args.get(1),language));
                ans = sb.toString();
                return ans;
            }
            if (pred.equalsIgnoreCase("exists")) {
                if (((String) args.get(0)).contains(" ")) {
                    // If more than one variable ...
                    sb.append(isNegMode ? NOTEXIST : EXIST);
                    sb.append(" ");
                    sb.append(translateWord(termMap, formatList((String) args.get(0), language), language));
                }
                else {
                    // If just one variable ...
                    sb.append(isNegMode ? NOTEXISTS : EXISTS);
                    sb.append(" ");
                    sb.append(translateWord(termMap,(String) args.get(0),language));
                }
                sb.append(" ");
                sb.append(SUCHTHAT);
                sb.append(" ");
                sb.append(translateWord(termMap,(String) args.get(1),language));
                ans = sb.toString();
                return ans;
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }



    /** ***************************************************************
     * Check all the arguments for the clause being processed to see if they have all been processed.
     * FIXME: this should be moved to a separate object with the stack, etc.
     * @param formulaArgs
     * @return
     */
    private boolean allArgsProcessed(List<String> formulaArgs) {
        boolean retVal = false;

        if (! formulaArgs.isEmpty()) {
            boolean isComplete = true;
            for (String fArg : formulaArgs) {
                if (! fArg.startsWith(("PROCESSED: "))) {
                    isComplete = false;
                    break;
                }
            }
            retVal = isComplete;
        }

        return retVal;
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

        //System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): Statement: " + stmt);
        //System.out.println("neg mode: " + isNegMode);
        Formula f = new Formula();
        f.read(stmt);
        String pred = f.car();
        String strFormat = (String) phraseMap.get(pred);
        //System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): 1 format: " + strFormat);
        // System.out.println("str format: " + strFormat);
        //int index;

        if (strFormat.contains("&%"))                    // setup the term hyperlink
            strFormat = strFormat.replaceAll("&%(\\w+)","&%" + pred + "\\$\"$1\"");

        //System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): 2 format: " + strFormat);
        if (isNegMode) {                                    // handle negation
            if (!strFormat.contains("%n")) {
                strFormat = getKeyword("not",language) + " " + strFormat;
            }
            else {
                if (!strFormat.contains("%n{")) {
                    strFormat = strFormat.replace("%n",getKeyword("not",language));
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
            strFormat = expandStar(f, strFormat, language);

        //System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): 3.5 format: " + strFormat);
        int num = 1;                                          // handle arguments
        String argPointer = ("%" + num);
        String arg = "";
        String para = "";
        while (strFormat.contains(argPointer)) {
            //System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): Statement: " + f.theFormula);
            //System.out.println("arg: " + f.getArgument(num));
            //System.out.println("num: " + num);
            //System.out.println("str: " + strFormat);
            arg = f.getArgument(num);
            if (Formula.isVariable(arg))
                para = arg;
            else
                para = nlStmtPara(arg, isNegMode, 1);

            //System.out.println("para: " + para);
            if (!Formula.atom(para)) {
                // Add the hyperlink placeholder for arg.
                if (Formula.isVariable(arg))
                    strFormat = strFormat.replace(argPointer, para);
                else {
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
                    strFormat = strFormat.replace(argPointer, para);
                }
            }
            else
                strFormat = strFormat.replace(argPointer,para);
            //System.out.println("strFormat == " + strFormat);
            num++;
            argPointer = ("%" + num);
        }
        return strFormat.toString();
    }

    /** *************************************************************
     */
    private String prettyPrint(String term) {

        if (term.endsWith("Fn"))
            term = term.substring(0,term.length()-2);
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < term.length(); i++) {
            if (Character.isLowerCase(term.charAt(i)) || !Character.isLetter(term.charAt(i)))
                result.append(term.charAt(i));
            else {
                if (i + 1 < term.length() && Character.isUpperCase(term.charAt(i+1)))
                    result.append(term.charAt(i));
                else {
                    if (i != 0)
                        result.append(" ");
                    result.append(Character.toLowerCase(term.charAt(i)));
                }
            }
        }
        return result.toString();
    }

    /** *************************************************************
     *  @return a string with termFormat expressions created for all
     *  the terms in the knowledge base
     */
    private String allTerms(KB kb) {

        StringBuilder result = new StringBuilder();
        for (Iterator<String> it = kb.getTerms().iterator(); it.hasNext();) {
            String term = (String) it.next();
            result.append("(termFormat EnglishLanguage ");
            result.append(term);
            result.append(" \"");
            result.append(prettyPrint(term));
            result.append("\")\n");
        }
        return result.toString();
    }

    /** *************************************************************
     */
    private String functionFormat(String term, int i) {

        switch (i) {
            case 1: return "the &%" + prettyPrint(term) + " of %1";
            case 2: return "the &%" + prettyPrint(term) + " of %1 and %2";
            case 3: return "the &%" + prettyPrint(term) + " of %1, %2 and %3";
            case 4: return "the &%" + prettyPrint(term) + " of %1, %2, %3 and %4";
        }
        return "";
    }

    /** *************************************************************
     */
    private String allFunctionsOfArity(KB kb, int i) {

        String parent = "";
        switch (i) {
            case 1: parent = "UnaryFunction"; break;
            case 2: parent = "BinaryFunction"; break;
            case 3: parent = "TernaryFunction"; break;
            case 4: parent = "QuaternaryFunction"; break;
        }
        if (parent == "")
            return "";
        StringBuffer result = new StringBuffer();
        for (Iterator<String> it = kb.getTerms().iterator(); it.hasNext();) {
            String term = it.next();
            if (kb.kbCache.transInstOf(term,parent))
                result.append("(format EnglishLanguage " + term + " \"" +
                        functionFormat(term,i) + "\")\n");
        }
        return result.toString();
    }

    /** *************************************************************
     */
    private String relationFormat(String term, int i) {

        switch (i) {
            case 2: return ("%2 is %n "
                    + LanguageFormatter.getArticle(term,1,1,"EnglishLanguage")
                    + "&%" + prettyPrint(term) + " of %1");
            case 3: return "%1 %n{doesn't} &%" + prettyPrint(term) + " %2 for %3";
            case 4: return "%1 %n{doesn't} &%" + prettyPrint(term) + " %2 for %3 with %4";
            case 5: return "%1 %n{doesn't} &%" + prettyPrint(term) + " %2 for %3 with %4 and %5";
        }
        return "";
    }

    /** *************************************************************
     */
    private String allRelationsOfArity(KB kb, int i) {

        String parent = "";
        switch (i) {
            case 2: parent = "BinaryPredicate"; break;
            case 3: parent = "TernaryPredicate"; break;
            case 4: parent = "QuaternaryPredicate"; break;
            case 5: parent = "QuintaryPredicate"; break;
        }
        if (parent == "")
            return "";
        StringBuffer result = new StringBuffer();
        for (Iterator<String> it = kb.getTerms().iterator(); it.hasNext();) {
            String term = (String) it.next();
            if (kb.kbCache.transInstOf(term,parent))
                result.append("(format EnglishLanguage " + term + " \"" + relationFormat(term,i) + "\")\n");
        }
        return result.toString();
    }

    /** *************************************************************
     * Print out set of all format and termFormat expressions
     */
    private void generateAllLanguageFormats(KB kb) {

        System.out.println(";;-------------- Terms ---------------");
        System.out.println(allTerms(kb));
        for (int i = 1; i < 5; i++) {
            System.out.println(";;-------------- Arity " + i + " Functions ---------------");
            System.out.println(allFunctionsOfArity(kb,i));
            System.out.println(";;-------------- Arity " + i + " Relations ---------------");
            System.out.println(allRelationsOfArity(kb,i+1));
        }
    }

    /** ***************************************************************
     */
    private static String getKeyword(String englishWord, String language) {

        String ans = "";
        if (keywordMap == null) {
            System.out.println("Error in LanguageFormatter.getKeyword(): keyword map is null");
            return ans;
        }
        HashMap<String,String> hm = keywordMap.get(englishWord);
        if (hm != null) {
            String tmp = hm.get(language);
            if (tmp != null)
                ans = tmp;
        }
        return ans;
    }

    /** ***************************************************************
     * Format a list of variables which are not enclosed by parens.
     * Formatting includes inserting the appropriate separator between the elements (usually a comma), as well as
     * inserting the conjunction ("and" or its equivalent in another language) if the conjunction doesn't already exist.
     * @param strseq
     *  the list of variables
     * @param language
     *  the target language (used for the conjunction "and")
     * @return
     *  the formatted string
     */
    public static String formatList(String strseq, String language) {
    	
        if (language == null || language.isEmpty())    {
            throw new IllegalArgumentException("Parameter language is empty or null.");
        }

        StringBuilder result = new StringBuilder();
        String comma = getKeyword(",", language);
        String space = " ";
        String[] arr = strseq.split(space);
        int lastIdx = (arr.length - 1);
        for (int i = 0; i < arr.length; i++) {
            String val = arr[i];
            if (i > 0) {
                if(val.equals(getKeyword("and", language))) {
                    // Make behavior for lists that include "and" the same as for those that don't.
                    continue;
                }
                if (i == lastIdx) {
                    result.append(space);
                    result.append(getKeyword("and", language));
                }
                else {
                    result.append(comma);
                }
                result.append(space);
            }
            result.append(val);
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private static boolean logicalOperator(String word) {

        String logops = "if,then,=>,and,or,<=>,not,forall,exists,holds";
        return logops.contains(word);
    }

    /** ***************************************************************
     *  Read a set of standard words and phrases in several languages.
     *  Each phrase must appear on a new line with alternatives separated by '|'.
     *  The first entry should be a set of two letter language identifiers.
     *
     *  @return a HashMap of HashMaps where the first HashMap has a key of the
     *  English phrase, and the interior HashMap has a key of the two letter
     *  language identifier.
     */
    public static HashMap<String,HashMap<String,String>> readKeywordMap(String dir) {
        System.out.println("INFO in LanguageFormatter.readKeywordMap(" + dir + "/" + 
                PHRASES_FILENAME + ")");

        if(dir == null || dir.isEmpty())    {
            throw new IllegalArgumentException("Parameter dir is null or empty.");
        }

        File dirFile = new File(dir);
        if(! dirFile.exists())  {
            throw new IllegalArgumentException("Parameter dir points to non-existent path: " + dir);
        }

        if (keywordMap == null)
            keywordMap = new HashMap<String,HashMap<String,String>>();
        int lc = 0;
        BufferedReader br = null;
        File phrasesFile = null;
        try {
            if (keywordMap.isEmpty()) {
                System.out.println("Filling keywordMap");

                phrasesFile = new File(dirFile, PHRASES_FILENAME);
                if (!phrasesFile.canRead())
                    throw new Exception("Cannot read \"" + phrasesFile.getCanonicalPath() + "\"");
                br = new BufferedReader(new InputStreamReader(new FileInputStream(phrasesFile),"UTF-8"));
                HashMap<String,String> phrasesByLang = null;
                List<String> phraseList = null;
                List<String> languageKeys = null;
                String delim = "|";
                String key = null;
                String line = null;
                while ((line = br.readLine()) != null) {
                    lc++;
                    line = line.trim();
                    if (line.startsWith(";") || line.equals("")) {
                        continue;
                    }
                    if (line.contains(delim)) {
                        if (line.startsWith("EnglishLanguage|")) // The language key line.
                            languageKeys = Arrays.asList(line.split("\\" + delim));
                        else {
                            phraseList = Arrays.asList(line.split("\\" + delim));
                            phrasesByLang = new HashMap<String,String>();
                            key = (String) phraseList.get(0);
                            int plLen = phraseList.size();
                            for (int i = 0; i < plLen; i++)
                                phrasesByLang.put(languageKeys.get(i), phraseList.get(i));
                            keywordMap.put(key.intern(), phrasesByLang);
                        }
                    }
                    else {
                        System.out.println("WARNING in LanguageFormatter.readKeywordMap(): "
                                + "Unrecognized line");
                        System.out.println(lc + ": \"" + line + "\"");
                    }
                }
            }
        }
        catch (Exception ex) {
            System.out.println("ERROR loading " + PHRASES_FILENAME + " at line " + lc + ":");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            try {
                if (br != null) { br.close(); }
            }
            catch (Exception ex2) {
                ex2.printStackTrace();
            }
            System.out.println("EXIT LanguageFormatter.readKeywordMap(" + dir + ")");
            return keywordMap;
        }
    }

    /** ***************************************************************
     * Process an atom into an appropriate NL string.  If a URL, add
     * spaces for readability.  Return variable unaltered.  Add
     * term format string to all other atoms.
     */
    private static String processAtom(String atom, Map<String,String> termMap, String language) {

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
            String formattedString = (String) termMap.get(atom);
            result = ("&%" + atom + "$\"" + formattedString + "\"");
        }
        else
            result = ("&%" + atom + "$\"" + atom + "\"");
        return result;
    }

    /** ***************************************************************
     * For debugging ...
     */
    private static void printSpaces(int depth) {
        
        for (int i = 0 ; i <= depth ; i++)
            System.out.print("  ");
        System.out.print(depth + ":");
        return;
    }

    /** ***************************************************************
     * Return the NL format of an individual word.
     */
    private static String translateWord(Map<String,String> termMap, String word, String language) {

        String ans = word;
        try {
            if (!Formula.isVariable(word) && (termMap != null)) {
                String pph = (String) termMap.get(word);
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
     * This method expands all "star" (asterisk) directives in the input
     * format string, and returns a new format string with individually
     * numbered argument pointers.
     *
     * @param f The Formula being paraphrased.
     *
     * @param strFormat The format string that contains the patterns and
     * directives for paraphrasing f.
     *
     * @param lang A two-character string indicating the language into
     * which f should be paraphrased.
     *
     * @return A format string with all relevant argument pointers
     * expanded.
     */
    private static String expandStar(Formula f, String strFormat, String lang) {

        String result = strFormat;
        ArrayList<String> problems = new ArrayList<String>();
        try {
            int flen = f.listLength();
            if (StringUtil.isNonEmptyString(strFormat) && (flen > 1)) {
                int p1 = 0;
                int p2 = strFormat.indexOf("%*");
                if (p2 != -1) {
                    int slen = strFormat.length();
                    String lb = null;
                    String rb = null;
                    int lbi = -1;
                    int rbi = -1;
                    String ss = null;
                    String range = null;
                    String[] rangeArr = null;
                    String[] rangeArr2 = null;
                    String lowStr = null;
                    String highStr = null;
                    int low = -1;
                    int high = -1;
                    String delim = " ";
                    boolean isRange = false;
                    boolean[] argsToPrint = new boolean[ flen ];
                    int nArgsSet = -1;
                    StringBuilder sb = new StringBuilder();
                    while ((p1 < slen) && (p2 >= 0) && (p2 < slen)) {
                        sb.append(strFormat.substring(p1, p2));
                        p1 = (p2 + 2);
                        for (int k = 0 ; k < argsToPrint.length ; k++) {
                            argsToPrint[k] = false;
                        }
                        lowStr = null;
                        highStr = null;
                        low = -1;
                        high = -1;
                        delim = " ";
                        nArgsSet = 0;
                        lb = null;
                        lbi = p1;
                        if (lbi < slen) { lb = strFormat.substring(lbi, (lbi + 1)); }
                        while ((lb != null) && (lb.equals("{") || lb.equals("["))) {
                            rb = "]";
                            if (lb.equals("{")) { rb = "}"; }
                            rbi = strFormat.indexOf(rb, lbi);
                            if (rbi == -1) {
                                problems.add("Error in format \"" + strFormat + "\": missing \"" + rb + "\"");
                                break;
                            }
                            p1 = (rbi + 1);
                            ss = strFormat.substring((lbi + 1), rbi);
                            if (lb.equals("{")) {
                                range = ss.trim();
                                rangeArr = range.split(",");
                                for (int i = 0 ; i < rangeArr.length ; i++) {
                                    if (StringUtil.isNonEmptyString(rangeArr[i])) {
                                        isRange = (rangeArr[i].indexOf("-") != -1);
                                        rangeArr2 = rangeArr[i].split("-");
                                        lowStr = rangeArr2[0].trim();
                                        try {
                                            low = Integer.parseInt(lowStr);
                                        }
                                        catch (Exception e1) {
                                            problems.add("Error in format \"" + strFormat + "\": bad value in \"" + ss + "\"");
                                            low = 1;
                                        }
                                        high = low;
                                        if (isRange) {
                                            if (rangeArr2.length == 2) {
                                                highStr = rangeArr2[1].trim();
                                                try {
                                                    high = Integer.parseInt(highStr);
                                                }
                                                catch (Exception e2) {
                                                    problems.add("Error in format \"" + strFormat + "\": bad value in \"" + ss + "\"");
                                                    high = (flen - 1);
                                                }
                                            }
                                            else 
                                                high = (flen - 1);                                            
                                        }
                                        for (int j = low; (j <= high) && (j < argsToPrint.length); j++) {
                                            argsToPrint[j] = true;
                                            nArgsSet++;
                                        }
                                    }
                                }
                            }
                            else 
                                delim = ss;                            
                            lb = null;
                            lbi = p1;
                            if (lbi < slen) { lb = strFormat.substring(lbi, (lbi + 1)); }
                        }
                        String AND = getKeyword("and",lang);
                        if (StringUtil.emptyString(AND))
                            AND = "+";
                        int nAdded = 0;
                        boolean addAll = (nArgsSet == 0);
                        int nToAdd = (addAll ? (argsToPrint.length - 1) : nArgsSet);
                        for (int i = 1 ; i < argsToPrint.length ; i++) {
                            if (addAll || (argsToPrint[i] == true)) {
                                if (nAdded >= 1) {
                                    if (nToAdd == 2) {
                                        sb.append(" ");
                                        sb.append(AND);
                                        sb.append(" ");
                                    }
                                    else {
                                        sb.append(delim);
                                        sb.append(" ");
                                    }
                                    if ((nToAdd > 2) && ((nAdded + 1) == nToAdd)) {
                                        sb.append(AND);
                                        sb.append(" ");
                                    }
                                }
                                sb.append("%" + i);
                                nAdded++;
                            }
                        }
                        if (p1 < slen) {
                            p2 = strFormat.indexOf("%*", p1);
                            if (p2 == -1) {
                                sb.append(strFormat.substring(p1, slen));
                                break;
                            }
                        }
                    }
                    if (sb.length() > 0) {
                        result = sb.toString();
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        if (! problems.isEmpty()) {
            String errStr = KBmanager.getMgr().getError();
            String str = null;
            if (errStr == null) { errStr = ""; }
            Iterator<String> it = problems.iterator();
            while (it.hasNext()) {
                str = (String) it.next();
                System.out.println("Error in LanguageFormatter.expandStar(): ");
                System.out.println("  " + str);
                errStr += ("\n<br/>" + str + "\n<br/>");
            }
            KBmanager.getMgr().setError(errStr);
        }
        return result;
    }

    /** **************************************************************
     * Hyperlink terms in a natural language format string.  This assumes that
     * terms to be hyperlinked are in the form &%termName$termString , where
     * termName is the name of the term to be browsed in the knowledge base and
     * termString is the text that should be displayed hyperlinked.
     *
     * @param href the anchor string up to the term= parameter, which this method
     *               will fill in.
     * @param stmt the KIF statement that will be passed to nlStmtPara for formatting.
     * @param phraseMap the set of NL formatting statements that will be passed to nlStmtPara.
     * @param termMap the set of NL statements for terms that will be passed to nlStmtPara.
     * @param language the natural language in which the paraphrase should be generated.
     */
    public static String htmlParaphrase(String href, String stmt, Map<String,String> phraseMap,
                                        Map<String,String> termMap, KB kb, String language) {

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, phraseMap, termMap, kb, language);
        return languageFormatter.htmlParaphrase(href);
    }


    /** **************************************************************
     * Generate a linguistic article appropriate to how many times in a
     * paraphrase a particular type has already occurred.
     * @param occurrence is the number of times a variables of a
     *                   given type have appeared
     * @param count is the number of times a given variable has
     *              appeared
     */
    public static String getArticle(String s, int count, int occurrence, String language) {

        String ordinal = "";
        switch (occurrence) {
        case 3: ordinal = getKeyword("third",language); break;
        case 4: ordinal = getKeyword("fourth",language); break;
        case 5: ordinal = getKeyword("fifth",language); break;
        case 6: ordinal = getKeyword("sixth",language); break;
        case 7: ordinal = getKeyword("seventh",language); break;
        case 8: ordinal = getKeyword("eighth",language); break;
        case 9: ordinal = getKeyword("ninth",language); break;
        case 10: ordinal = getKeyword("tenth",language); break;
        case 11: ordinal = getKeyword("eleventh",language); break;
        case 12: ordinal = getKeyword("twelfth",language); break;
        }
        boolean isArabic = (language.matches(".*(?i)arabic.*")
                || language.equalsIgnoreCase("ar"));
        if (count == 1 && occurrence == 2)
            return getKeyword("another",language);
        if (count > 1) {
            if (occurrence == 1) {
                if (isArabic)
                    return ordinal;
                else
                    return (getKeyword("the",language));
            }
            else if (occurrence > 2) {
                if (isArabic)
                    return ordinal;
                else
                    return (getKeyword("the",language) + " " + ordinal);
            }
            else {
                if (isArabic)
                    return (getKeyword("the",language) + " " + getKeyword("other",language));
                else
                    return (getKeyword("the",language) + " " + getKeyword("other",language));
            }
        }
        // count = 1 (first occurrence of a type)
        if (language.equalsIgnoreCase("EnglishLanguage")) {
            String vowels = "AEIOUaeiou";
            if ((vowels.indexOf(s.charAt(0)) != -1) && (occurrence == 1))
                return "an";
            else
                return "a " + ordinal;
        }
        else if (isArabic) {
            String defArt = getKeyword("the",language);
            if (ordinal.startsWith(defArt)) {
                // remove the definite article
                ordinal = ordinal.substring(defArt.length());
                // remove shadda
                ordinal = ordinal.replaceFirst("\\&\\#\\x651\\;","");
            }
            return ordinal;
        }
        else 
            return ordinal;        
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

        String result = new String(form);
        boolean isArabic = (language.matches(".*(?i)arabic.*")
                || language.equalsIgnoreCase("ar"));
        if (StringUtil.emptyString(varPretty)) 
            varPretty = varType;        
        boolean found = true;
        int occurrenceCounter = 1;
        if (typeMap.keySet().contains(varType)) {
            occurrenceCounter = (Integer) typeMap.get(varType);
            occurrenceCounter++;
            typeMap.put(varType,new Integer(occurrenceCounter));
        }
        else
            typeMap.put(varType,new Integer(1));
        int count = 1;
        while (found) {
            if (result.indexOf(varString) > -1 && count < 20) {
                String article = "";
                String replacement = "";
                if (isClass) {
                    article = getArticle("kind",count,occurrenceCounter,language);
                    replacement = (article + " " + getKeyword("kind of",language)
                            + " " + varPretty);
                    if (isArabic)
                        replacement = (getKeyword("kind of",language) + " " + varPretty);
                    result =
                        result.replaceFirst(("\\?" + varString.substring(1)),
                                ("\\&\\%" + varType + "\\$\"" + replacement + "\""));
                }
                else {
                    article = getArticle(varPretty,count,occurrenceCounter,language);
                    replacement = (article + " " + varPretty);
                    if (isArabic) {
                        String defArt = getKeyword("the",language);
                        if (article.startsWith(defArt) && !varPretty.startsWith(defArt)) {
                            // This has to be refined to insert shadda for sun letters.
                            varPretty = (defArt + varPretty);
                        }
                        replacement = (varPretty + " " + article);
                    }
                }
                result = result.replaceFirst(("\\?" + varString.substring(1)),
                        ("\\&\\%" + varType + "\\$\"" + replacement + "\""));
            }
            else
                found = false;
            count++;
        }
        return result;
    }

    /** **************************************************************
     * Collect all the variables occurring in a formula in order.  Return
     * an ArrayList of Strings.
     */
    private static ArrayList<String> collectOrderedVariables(String form) {

        boolean inString = false;
        boolean inVar = false;
        String var = "";
        ArrayList<String> result = new ArrayList<String>();
        for (int i = 0; i < form.length(); i++) {
            char ch = form.charAt(i);
            switch (ch) {
            case '"': inString = !inString; break;
            case '?': if (!inString) inVar = true; break;
            case '@': if (!inString) inVar = true; break;
            }
            if (inVar && !Character.isLetterOrDigit(ch) && ch != '?' && ch != '@') {
                if (!result.contains(var))
                    result.add(var);
                inVar = false;
                var = "";
            }
            if (inVar)
                var = var + ch;
        }
        return result;
    }

    /** **************************************************************
     * Replace variables in a formula with paraphrases expressing their
     * type.
     */
    public static String variableReplace(String form, HashMap<String, HashSet<String>> instMap, 
    		HashMap<String, HashSet<String>> classMap, KB kb, String language) {

        String result = form;
        HashMap<String,Integer> typeMap = new HashMap<String,Integer>();
        ArrayList<String> varList = collectOrderedVariables(form);
        Iterator<String> it = varList.iterator();
        while (it.hasNext()) {
            String varString = (String) it.next();
            if (StringUtil.isNonEmptyString(varString)) {
                HashSet<String> instanceArray = instMap.get(varString);
                HashSet<String> subclassArray = classMap.get(varString);

                if (subclassArray != null && subclassArray.size() > 0) {
                    String varType = (String) subclassArray.toArray()[0];
                    String varPretty = (String) kb.getTermFormatMap(language).get(varType);
                    result = incrementalVarReplace(result,varString,varType,varPretty,language,true,typeMap);
                }
                else {
                    if (instanceArray != null && instanceArray.size() > 0) {
                        String varType = (String) instanceArray.toArray()[0];
                        String varPretty = (String) kb.getTermFormatMap(language).get(varType);
                        result = incrementalVarReplace(result,varString,varType,varPretty,language,false,typeMap);
                    }
                    else {
                        String varPretty = (String) kb.getTermFormatMap(language).get("Entity");
                        if (StringUtil.emptyString(varPretty))
                            varPretty = "entity";
                        result = incrementalVarReplace(result,varString,"Entity",varPretty,language,false,typeMap);
                    }
                }         
            }
        }
        return result;
    }

    /** **************************************************************
     * Capitalizes the first visible char of htmlParaphrase, if
     * possible, and adds the full stop symbol for language at a
     * workable place near the end of htmlParaphrase if addFullStop is
     * true.
     *
     * @param htmlParaphrase Any String, but assumed to be a Formula
     * paraphrase with HTML markup
     *
     * @param addFullStop If true, this method will try to add a full
     * stop symbol to the result String.
     *
     * @param language The language of the paraphrase String.
     *
     * @return String
     */
    protected static String upcaseFirstVisibleChar(String htmlParaphrase,
                                                   boolean addFullStop,
                                                   String language) {

        String ans = htmlParaphrase;
        try {
            if (StringUtil.isNonEmptyString(htmlParaphrase)) {
                StringBuilder sb = new StringBuilder(htmlParaphrase.trim());
                String termKey = "term=";
                int sbLen = sb.length();
                if (sbLen > 0) {
                    int codePoint = -1;
                    int termCodePoint = -1;
                    int termPos = -1;
                    String uc = null;
                    int i = 0;
                    while ((i > -1) && (i < sbLen)) {
                        // System.out.println("x");
                        codePoint = Character.codePointAt(sb, i);
                        if (Character.isLetter(codePoint)) {
                            if (Character.isLowerCase(codePoint)) {
                                boolean isKifTermCapitalized = true;
                                termPos = sb.indexOf(termKey);
                                if (termPos > -1) {
                                    termPos = (termPos + termKey.length());
                                    if (termPos < i) {
                                        termCodePoint = Character.codePointAt(sb, termPos);
                                        isKifTermCapitalized = Character.isUpperCase(termCodePoint);
                                    }
                                }
                                if (isKifTermCapitalized) {
                                    uc = sb.substring(i, i + 1).toUpperCase();
                                    sb = sb.replace(i, i + 1, uc);
                                }
                            }
                            break;
                        }
                        i = sb.indexOf(">", i);
                        if (i > -1) {
                            i++;
                            while ((i < sbLen) && sb.substring(i, i + 1).matches("\\s")) 
                                i++;                            
                        }
                    }
                    if (addFullStop) {
                        String fs = getKeyword(".", language);
                        if (StringUtil.isNonEmptyString(fs)) {
                            String ss = "";
                            sbLen = sb.length();
                            i = (sbLen - 1);
                            while ((i > 0) && (i < sbLen)) {
                                // System.out.println("m");
                                ss = sb.substring(i, i + 1);
                                if (ss.matches("\\s")) {
                                    i--;
                                    continue;
                                }
                                if (ss.matches("[\\w;]")) {
                                    sb = sb.insert(i + 1, fs);
                                    break;
                                }
                                while ((i > 0) && !ss.equals("<")) {
                                    i--;
                                    ss = sb.substring(i, i + 1);
                                }
                                i--;
                            }
                        }
                    }
                    ans = sb.toString();
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     * Remove HTML from input string.
     * @param input
     * @return
     */
    public static String filterHtml(String input)  {
        // Note use of non-greedy matching.
        String out = input.replaceAll("<.*?>", "");

        // Clean up.
        out = out.replaceAll(" +", " ");
        // Insert a space anywhere a comma isn't followed by a space.
        out = out.replaceAll(",(\\S)", ", $1");

        return out;
    }

    /** **************************************************************
     */
    public static void main(String[] args) {

        try {
            KBmanager.getMgr().initializeOnce();
        } 
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB("SUMO");

        String stmt = "(exists (?D ?H)\n" +
                "   (and\n" +
                "       (instance ?D Driving)\n" +
                "       (instance ?H Human)\n" +
                "       (agent ?D ?H)))";
        Formula f = new Formula(stmt);
        System.out.println("Formula: " + f.theFormula);
        System.out.println("result: " + filterHtml(htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        System.out.println();

        stmt = "(exists (?D ?H)\n" +
                "   (and\n" +
                "       (instance ?D Driving)\n" +
                "       (instance ?H Human)\n" +
                "       (names ?H \"John\")\n" +
                "       (agent ?D ?H)))";
        f = new Formula();
        f.read(stmt);
        System.out.println("Formula: " + f.theFormula);
        System.out.println("result: " + filterHtml(htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        System.out.println();

        stmt = "(exists (?D ?H ?Car)\n" +
                "   (and\n" +
                "       (instance ?D Driving)\n" +
                "       (instance ?H Human)\n" +
                "       (names ?H \"John\")\n" +
                "       (instance ?Car Automobile)\n" +
                "       (agent ?D ?H)\n" +
                "       (instrument ?D ?Car)))";
        f = new Formula();
        f.read(stmt);
        System.out.println("Formula: " + f.theFormula);
        System.out.println("result: " + filterHtml(htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
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
                "       (instrument ?D ?C)))";
        f = new Formula();
        f.read(stmt);
        System.out.println("Formula: " + f.theFormula);
        System.out.println("result: " + filterHtml(htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        System.out.println();

    }
}

