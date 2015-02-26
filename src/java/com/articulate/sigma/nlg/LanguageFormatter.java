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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.*;

/** ***************************************************************
 *  A class that handles the generation of natural language from logic.
 *
 *  @author Adam Pease - apease [at] articulatesoftware [dot] com, with thanks
 *  to Michal Sevcenko - sevcenko@vc.cvut.cz for development of the
 *  formatting language.
 */
public class LanguageFormatter {

    private final String statement;

    private final Map<String, String> phraseMap;

    // kb.getTermFormatMap() for this language
    private final Map<String, String> termMap;

    private final KB kb;
    private final String language;

    private final HashMap<String, HashSet<String>> variableTypes;

    private final Map<String, HashSet<String>> variableToInstanceMap;

    // FIXME: Temporarily disable Informal NLG for certain logical operators.
    private static final List<String> notReadyOperators = Lists.newArrayList("not");

    /**
     * "Informal" NLG refers to natural language generation in which the formal logic terms are expressions are
     * eliminated--e.g. "a man drives" instead of "there exist a process and an agent such that the process is an instance of driving and
     * the agent is an instance of human and the agent is an agent of the process".
     */
    private boolean doInformalNLG = true;

    void setDoInformalNLG(boolean doIt) {
        doInformalNLG = doIt;
    }


    /**
     * The stack holds information on the formula being processed, so that successive recursive calls can refer to it.
     */
    private final LanguageFormatterStack theStack = new LanguageFormatterStack();


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
            theStack.pushNew();
            LanguageFormatterStack.StackElement element = theStack.getCurrStackElement();

            String template = paraphraseStatement(statement, false, 1);

            // Check for successful informal NLG of the entire statement.
            if (doInformalNLG)  {
                String informalNLG = theStack.doStatementLevelNatlLanguageGeneration();
                if (! informalNLG.isEmpty())    {
                    // Resolve variables.
                    informalNLG = LanguageFormatter.variableReplace(informalNLG, variableToInstanceMap, Maps.newHashMap(), kb, language);
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
                HashMap<String, HashSet<String>> instanceMap = new HashMap<>();
                HashMap<String, HashSet<String>> classMap = new HashMap<>();
                HashMap<String, HashSet<String>> types = fp.computeVariableTypes(f, kb);
                Iterator<String> it = types.keySet().iterator();
                while (it.hasNext()) {
                    String var = it.next();
                    HashSet<String> typeList = types.get(var);
                    Iterator<String> it2 = typeList.iterator();
                    while (it2.hasNext()) {
                        String t = it2.next();
                        if (t.endsWith("+")) {
                            HashSet<String> values = new HashSet<>();
                            if (classMap.containsKey(var))
                                values = classMap.get(var);
                            values.add(t.substring(0, t.length()-1));
                            classMap.put(var, values);
                        }
                        else {
                            HashSet<String> values = new HashSet<>();
                            if (instanceMap.containsKey(var))
                                values = instanceMap.get(var);
                            values.add(t);
                            instanceMap.put(var, values);
                        }
                    }
                }

                if ( !instanceMap.isEmpty() ||  !classMap.isEmpty() ) {
                    //if ((instanceMap != null && !instanceMap.isEmpty()) || (classMap != null && !classMap.isEmpty()))
                    template = variableReplace(template, instanceMap, classMap, kb, language);
                }

                // Get rid of the percentage signs.
                nlFormat = NLGUtils.resolveFormatSpecifiers(template, href);
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
    String paraphraseStatement(String stmt, boolean isNegMode, int depth) {

        //System.out.println("INFO in LanguageFormatter.paraphraseStatement(): stmt: " + stmt);
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

        if (kb.kbCache.isInstanceOf(pred, "CaseRole") && SumoProcessCollector.isKnownRole(pred)) {
            handleCaseRole(f, pred);
        }

        // Mark the predicate as processed.
        // FIXME: handle other predicates here: located, orientation, etc.
        if (pred.equals("instance")) {
            theStack.markFormulaArgAsProcessed(stmt);
        }

        if (!Formula.atom(pred)) {
            System.out.println("Error in LanguageFormatter.paraphraseStatement(): statement "
                    + stmt
                    + " has a formula in the predicate position.");
            return stmt;
        }

        if (NLGUtils.logicalOperator(pred)) {
            ans = paraphraseLogicalOperator(stmt, isNegMode, depth+1);

            // "Concatenate" the informal NLG with the operator
            List<String> translations = theStack.getCurrStackFormulaArgs();

            String translation = generateFormalNaturalLanguage(translations, pred, isNegMode);
            if (! translation.isEmpty()) {
                theStack.getCurrStackElement().setTranslation(translation, true);
            }

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
                result.append(processAtom(pred, termMap));
            }
            f.read(f.cdr());
            while (!f.empty()) {
                /*
                System.out.println("INFO in LanguageFormatter.paraphraseStatement(): stmt: " + f);
                System.out.println("length: " + f.listLength());
                System.out.println("result: " + result);
                 */
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
     * Insert the give case role into the current element's process map; or, if the SumoProcess already exists
     * there, update it with the new information.
     * @param formula
     * @param caseRole
     */
    private void handleCaseRole(Formula formula, String caseRole) {
        if(kb.kbCache.isInstanceOf(caseRole, "CaseRole") && SumoProcessCollector.isKnownRole(caseRole)) {
            try {
                if (!theStack.isEmpty()) {
                    String caseArgument = formula.cadr() + " " + formula.caddr();
                    String[] caseArgs = caseArgument.trim().split(" ");
                    String processInstanceName = caseArgs[0];
                    String processParticipant = caseArgs[1];

                    if (variableTypes.containsKey(processInstanceName)) {

                        HashSet<String> vals = variableTypes.get(processInstanceName);
                        // FIXME: instead of adding new processes to the if-statement, you need to iterate through all the vals elements
                        // and see if each one is either Process or a subclass of Process
                        if (vals.contains("Process") || vals.contains("LegalAction")) {
                            // Mark the argument as PROCESSED.
                            theStack.markFormulaArgAsProcessed(formula.theFormula);

                            // Get the data in the stack's topmost element.
                            Map<String, SumoProcessCollector> thisProcessMap = theStack.getCurrProcessMap();

                            if (thisProcessMap.containsKey(processInstanceName)) {
                                // Already there. Add new information if any exists.
                                SumoProcessCollector sProcess = thisProcessMap.get(processInstanceName);
                                //String resolvedParticipant = resolveVariable(processParticipant);
                                sProcess.addRole(caseRole, processParticipant);
                            } else {
                                // Insert into stack.
                                String resolvedProcessName = resolveVariable(processInstanceName);
                                //String resolvedParticipant = resolveVariable(processParticipant);
                                SumoProcessCollector sProcess = new SumoProcessCollector(kb, caseRole, resolvedProcessName, processParticipant);
                                thisProcessMap.put(processInstanceName, sProcess);
                            }
                        }
                    }

                }
            } catch (IllegalArgumentException e) {
                // Recover from exception by turning off full NLG.
                this.doInformalNLG = false;
                String temp = statement.replaceAll("\\s+", " ");    // clean up, reducing consecutive whitespace to single space
                String msg = "Handled IllegalArgumentException after finding case role.\n   Exception message:\n      " + e.getMessage() + "\n" +
                        "   Formula:\n       " + temp + "\n";
                System.out.println(msg);
            }
        }
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
            if (NLGUtils.keywordMap == null) {
                System.out.println("Error in LanguageFormatter.paraphraseLogicalOperator(): " + "keywordMap is null");
                return null;
            }
            ArrayList<String> args = new ArrayList<>();
            Formula f = new Formula();
            f.read(stmt);
            String pred = f.getArgument(0);
            f.read(f.cdr());

            String ans = null;

            if (LanguageFormatter.notReadyOperators.contains(pred))   {
                doInformalNLG = false;
            }

            if (pred.equals("not")) {
                ans = paraphraseStatement(f.car(), true, depth + 1);
                return ans;
            }

            // Push new element onto the stack.
            theStack.pushNew();
            LanguageFormatterStack.StackElement inElement = theStack.getCurrStackElement();

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

                    if(thisProcessMap != null) {

                        if(! thisProcessMap.isEmpty()) {
                            // Only perform NLG if the args list has been fully processed.
                            boolean doIt = theStack.areFormulaArgsProcessed();

                            if (doIt) {
                                // Try to do natural language generation on top element of stack.
                                String output = theStack.doProcessLevelNatlLanguageGeneration();
                                if (!output.isEmpty()) {
                                    theStack.getPrevStackElement().setTranslation(output, true);
                                }
                            }
                        }

                    }
                }

            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            } finally {
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

    String generateFormalNaturalLanguage(List<String> args, String pred, boolean isNegMode) {
        if (args.isEmpty())     {
            return "";
        }

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

        //System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): Statement: " + stmt);
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
            //System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): Statement: " + f.theFormula);
            //System.out.println("arg: " + f.getArgument(num));
            //System.out.println("num: " + num);
            //System.out.println("str: " + strFormat);
            arg = f.getArgument(num);
            if (Formula.isVariable(arg))
                para = arg;
            else
                para = paraphraseStatement(arg, isNegMode, 1);

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
    private static String translateWord(Map<String,String> termMap, String word) {

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

        String result = form;
        boolean isArabic = (language.matches(".*(?i)arabic.*")
                || language.equalsIgnoreCase("ar"));
        if (StringUtil.emptyString(varPretty)) 
            varPretty = varType;        
        boolean found = true;
        int occurrenceCounter = 1;
        if (typeMap.keySet().contains(varType)) {
            occurrenceCounter = typeMap.get(varType);
            occurrenceCounter++;
            typeMap.put(varType,new Integer(occurrenceCounter));
        }
        else
            typeMap.put(varType,new Integer(1));
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
                                ("\\&\\%" + varType + "\\$\"" + replacement + "\""));
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
                        ("\\&\\%" + varType + "\\$\"" + replacement + "\""));
            }
            else
                found = false;
            count++;
        }
        return result;
    }

    /** **************************************************************
     * Replace variables in a formula with paraphrases expressing their
     * type.
     */
    public static String variableReplace(String form, Map<String, HashSet<String>> instMap,
            HashMap<String, HashSet<String>> classMap, KB kb, String language) {

        String result = form;
        HashMap<String,Integer> typeMap = new HashMap<>();
        ArrayList<String> varList = NLGUtils.collectOrderedVariables(form);
        Iterator<String> it = varList.iterator();
        while (it.hasNext()) {
            String varString = it.next();
            if (StringUtil.isNonEmptyString(varString)) {
                HashSet<String> instanceArray = instMap.get(varString);
                HashSet<String> subclassArray = classMap.get(varString);

                if (subclassArray != null && ! subclassArray.isEmpty()) {
                    String varType = (String) subclassArray.toArray()[0];
                    String varPretty = kb.getTermFormatMap(language).get(varType);
                    result = incrementalVarReplace(result, varString, varType, varPretty, language, true, typeMap);
                }
                else {
                    if (instanceArray != null && ! instanceArray.isEmpty()) {
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
        return result;
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
        System.out.println("result: " + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
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
        System.out.println("result: " + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
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
                "       (instrument ?D ?C)))";
        f = new Formula();
        f.read(stmt);
        System.out.println("Formula: " + f.theFormula);
        System.out.println("result: " + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")));
        System.out.println();

    }

}

