package com.articulate.sigma.trans;

import com.articulate.sigma.*;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

public class SUMOformulaToTPTPformula {

    public Formula _f = null;
    public static boolean debug = false;

    /** ***************************************************************
     * Encapsulates translateWord_1, which translates the logical
     * operators and inequalities in SUO-KIF to their TPTP
     * equivalents.
     *
     * @param st is the StreamTokenizer_s that contains the current token
     * @return the String that is the translated token
     */
    private static String translateWord(StreamTokenizer_s st, boolean hasArguments) {

        String result = null;
        try {
            result = translateWord_1(st, hasArguments);
            if (result.equals("$true__m") || result.equals("$false__m")) 
                result = "'" + result + "'";            
        }
        catch (Exception ex) {
            System.out.println("Error in SUMOformulaToTPTPformula.translateWord(): " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
     * Convert the logical operators and inequalities in SUO-KIF to
     * their TPTP equivalents
     * @param st is the StreamTokenizer_s that contains the current token
     * @return the String that is the translated token
     */
    private static String translateWord_1(StreamTokenizer_s st, boolean hasArguments) {      

        //System.out.println("INFO in SUMOformulaToTPTPformula.translateWord_1(): st: " + st.sval);
        int translateIndex;

        List<String> kifOps = Arrays.asList(Formula.UQUANT, Formula.EQUANT, 
                Formula.NOT, Formula.AND, Formula.OR, Formula.IF, Formula.IFF, 
                Formula.EQUAL);
        List<String> tptpOps = Arrays.asList("! ", "? ", "~ ", " & ", " | ", " => ", " <=> ", " = ");

        List<String> kifPredicates =
            Arrays.asList(Formula.LOG_TRUE, Formula.LOG_FALSE,
                          "<=","<",">",">=",
                          "lessThanOrEqualTo","lessThan","greaterThan","greaterThanOrEqualTo");

        List<String> tptpPredicates = Arrays.asList("$true","$false",
                                                    "lesseq","less","greater","greatereq",
                                                    "lesseq","less","greater","greatereq");

        List<String> kifFunctions = Arrays.asList(Formula.TIMESFN, Formula.DIVIDEFN, 
                Formula.PLUSFN, Formula.MINUSFN);
        List<String> tptpFunctions = Arrays.asList("times","divide","plus","minus");

        List<String> kifRelations = new ArrayList<String>();
        kifRelations.addAll(kifPredicates);
        kifRelations.addAll(kifFunctions);

        // Context creeps back in here whether we want it or not.  We
        // consult the KBmanager to determine if holds prefixing is
        // turned on, or not.  If it is on, then we do not want to add
        // the "mentions" suffix to relation names used as arguments
        // to other relations.
        KBmanager mgr = null;
        boolean holdsPrefixInUse = false;
        String mentionSuffix = Formula.termMentionSuffix;
        mgr = KBmanager.getMgr();
        holdsPrefixInUse = ((mgr != null) && mgr.getPref("holdsPrefix").equalsIgnoreCase("yes"));
        if (holdsPrefixInUse && !kifRelations.contains(st.sval))
            mentionSuffix = "";

        //----Places single quotes around strings, and replace \n by space
        if (st.ttype == 34)
            return("'" + st.sval.replaceAll("[\n\t\r\f]"," ").replaceAll("'","") + "'");
        //----Fix variables to have leading V_
        char ch0 = ((st.sval.length() > 0)
                    ? st.sval.charAt(0)
                    : 'x');
        char ch1 = ((st.sval.length() > 1)
                    ? st.sval.charAt(1)
                    : 'x');
        if (ch0 == '?' || ch0 == '@')
            return(Formula.termVariablePrefix + st.sval.substring(1).replace('-','_'));
        //----Translate special predicates
        translateIndex = 0;
        while (translateIndex < kifPredicates.size() && !st.sval.equals(kifPredicates.get(translateIndex)))
            translateIndex++;
        if (translateIndex < kifPredicates.size())
            // return((hasArguments ? "$" : "") + tptpPredicates[translateIndex]);
            return(tptpPredicates.get(translateIndex) + (hasArguments ? "" : mentionSuffix));
        //----Translate special functions
        translateIndex = 0;
        while (translateIndex < kifFunctions.size() && !st.sval.equals(kifFunctions.get(translateIndex)))
            translateIndex++;
        if (translateIndex < kifFunctions.size())
            // return((hasArguments ? "$" : "") + tptpFunctions[translateIndex]);
            return(tptpFunctions.get(translateIndex) + (hasArguments ? "" : mentionSuffix));
        //----Translate operators
        translateIndex = 0;
        while (translateIndex < kifOps.size() && !st.sval.equals(kifOps.get(translateIndex)))
            translateIndex++;
        if (translateIndex < kifOps.size())
            return(tptpOps.get(translateIndex));
        //----Do nothing to numbers
        if (st.ttype == StreamTokenizer.TT_NUMBER ||
            (st.sval != null && (Character.isDigit(ch0) ||
                                 (ch0 == '-' && Character.isDigit(ch1))))) {
            return(st.sval);
        }
        String term = st.sval;

        if (!hasArguments) {
            if (
                // The purely syntactic criteria for testing if a term
                // denotes a Relation are reliable only for "pure"
                // SUO-KIF.  They break down if the terms to be
                // translated contain namespace prefixes and other
                // non-SUO-KIF lexical conventions.
                (!term.endsWith(mentionSuffix)
                 && ((!term.contains(StringUtil.getKifNamespaceDelimiter())
                      && Character.isLowerCase(ch0))
                     || term.endsWith("Fn")
                     // The semantic test below works only if a KB is loaded.
                     || KB.isRelationInAnyKB(term)))) {
                term += mentionSuffix;
            }
        }
        return(Formula.termSymbolPrefix + term.replace('-','_'));
    }

    /** ***************************************************************
     * @param st is the StreamTokenizer_s that contains the current token
     * for which the arity is desired
     *
     * @return the integer arity of the given logical operator
     */
    private static int operatorArity(StreamTokenizer_s st) {

        int translateIndex;
        String kifOps[] = {Formula.UQUANT, Formula.EQUANT, Formula.NOT, 
                Formula.AND, Formula.OR, Formula.IF, Formula.IFF};

        translateIndex = 0;
        while (translateIndex < kifOps.length &&
               !st.sval.equals(kifOps[translateIndex])) 
            translateIndex++;        
        if (translateIndex <= 2) 
            return(1);        
        else {
            if (translateIndex < kifOps.length) 
                return(2);
            else 
                return(-1);            
        }
    }

    /** ***************************************************************
     */
    private static void incrementTOS(Stack<Integer> countStack) {

        countStack.push(Integer.valueOf((Integer) countStack.pop() + 1));
    }

    /** ***************************************************************
     * Add the current token, if a variable, to the list of variables
     * @param variables is the list of variables
     */
    private static void addVariable(StreamTokenizer_s st,Vector<String> variables) {

        if (st.sval.charAt(0) == '?' || st.sval.charAt(0) == '@') {
            String tptpVariable = translateWord(st,false);
            if (variables.indexOf(tptpVariable) == -1) 
                variables.add(tptpVariable);            
        }
    }

    /** ***************************************************************
     * Parse a single formula into TPTP format
     */
    public static String tptpParseSUOKIFString(String suoString, boolean query) {

        if (query || debug) System.out.println("INFO in SUMOformulaToTPTPformula.tptpParseSUOKIFString(): input: " + suoString);
        Formula tempF = new Formula();      // Special case to rename Foo for (instance Foo SetOrClass)
        tempF.read(suoString);              // so a symbol can't be both a class and an instance. However,
                                            // this may not be needed and we might just not allow a class 
                                            // to be an instance of itself
        if (tempF.getArgument(0).equals("instance") &&
            tempF.getArgument(2).equals("SetOrClass")) {
            String arg1 = tempF.getArgument(1);
            // suoString = "(instance " + arg1 + Formula.classSymbolSuffix + " SetOrClass)";
            suoString = "(instance " + arg1 + " SetOrClass)";
        }
        if (tempF.getArgument(0).equals("instance") &&
                tempF.getArgument(2).equals(tempF.getArgument(1))) {
            return null;
        }
        
        StreamTokenizer_s st = null;
        String translatedFormula = null;
        try {
            int parenLevel;
            boolean inQuantifierVars;
            boolean lastWasOpen;
            boolean inHOL;
            int inHOLCount;
            Stack<String> operatorStack = new Stack<String>();
            Stack<Integer> countStack = new Stack<Integer>();
            Vector<String> quantifiedVariables = new Vector<String>();
            Vector<String> allVariables = new Vector<String>();
            int index;
            int arity;
            String quantification;

            StringBuilder tptpFormula = new StringBuilder();

            parenLevel = 0;
            countStack.push(Integer.valueOf(0));
            lastWasOpen = false;
            inQuantifierVars = false;
            inHOL = false;
            inHOLCount = 0;

            st = new StreamTokenizer_s(new StringReader(suoString));
            KIF.setupStreamTokenizer(st);

            do {
                if (query || debug)
                    System.out.println("INFO in SUMOformulaToTPTPformula.tptpParseSUOKIFString(): looping: " +
                            st);

                st.nextToken();
                if (st.ttype==40) {         //----Open bracket
                    if (lastWasOpen)      //----Should not have ((in KIF
                        throw new ParseException("Parsing error in " + suoString + ". Doubled open parentheses.",0);                                       
                    if (inHOL)              //----Track nesting of ()s for hol__, so I know when to close the '
                        inHOLCount++;
                    lastWasOpen = true;
                    parenLevel++;                    
                } 
                else if (st.ttype == StreamTokenizer.TT_WORD &&  //----Operators
                           (((arity = operatorArity(st)) > 0) || st.sval.equals(Formula.EQUAL))) {                    
                    if (st.sval.equals(Formula.EQUAL))
                        arity = 2;
                    if (!lastWasOpen)              //----Operators must be preceded by a (
                        return(null);                    
                    //----This is the start of a new term - put in the infix operator if not the
                    //----first term for this operator
                    if ((Integer)(countStack.peek()) > 0) {
                        // System.out.println("  1 : countStack == " + countStack);
                        // System.out.println("  1 : operatorStack == " + operatorStack);
                        tptpFormula.append((String)operatorStack.peek());
                    }                    
                    if (inHOL && inHOLCount == 1)     //----If this is the start of a hol__ situation, quote it all
                        tptpFormula.append("'");
                    tptpFormula.append("(");          //----()s around all operator expressions
                    if (arity == 1) {                 //----Output unary as prefix
                        tptpFormula.append(translateWord(st,false));                        
                        countStack.push(Integer.valueOf(0));   //----Note the new operator (dummy) with 0 operands so far
                        operatorStack.push(",");
                        //----Check if the next thing will be the quantified variables
                        if (st.sval.equals("forall") || st.sval.equals("exists"))
                            inQuantifierVars = true;
                    } 
                    else if (arity == 2) {    //----Binary operator
                        //----Note the new operator with 0 operands so far
                        countStack.push(Integer.valueOf(0));
                        operatorStack.push(translateWord(st,false));
                    }
                    lastWasOpen = false;                    
                } 
                else if (st.ttype == 96) {   //----Back tick - token translation to TPTP. Everything gets ''ed                    
                    if (!inHOL) {              //----They may be nested - only start the situation at the outer one
                        inHOL = true;
                        inHOLCount = 0;
                    }
                } 
                else if (st.ttype == 34 ||  //----Quote - Term token translation to TPTP
                           st.ttype == StreamTokenizer.TT_NUMBER ||
                           (st.sval != null && (Character.isDigit(st.sval.charAt(0)))) ||
                           st.ttype == StreamTokenizer.TT_WORD) {
                    if (lastWasOpen) {          //----Start of a predicate or variable list
                        if (inQuantifierVars) { //----Variable list
                            tptpFormula.append("[");
                            tptpFormula.append(translateWord(st,false));
                            incrementTOS(countStack);
                        } 
                        else {                //----Predicate
                            //----This is the start of a new term - put in the infix operator if not the
                            //----first term for this operator
                            if ((Integer)(countStack.peek()) > 0)
                                tptpFormula.append((String)operatorStack.peek());
                            //----If this is the start of a hol__ situation, quote it all
                            if (inHOL && inHOLCount == 1)
                                tptpFormula.append("'");                            
                            tptpFormula.append(translateWord(st,true));   //----Predicate or function and (
                            tptpFormula.append("(");                            
                            countStack.push(Integer.valueOf(0));              //----Note the , for between arguments with 0 arguments so far
                            operatorStack.push(",");
                        }
                        //----Argument or quantified variable
                    } 
                    else {
                        //----This is the start of a new term - put in the infix operator if not the
                        //----first term for this operator
                        if ((Integer)(countStack.peek()) > 0)
                            tptpFormula.append((String)operatorStack.peek());
                        // TODO: may have to trap strings - AP
                        tptpFormula.append(translateWord(st,false));      //----Output the word               
                        incrementTOS(countStack);                         //----Increment counter for this level
                    }
                    //----Collect variables that are used and quantified
                    if (!StringUtil.emptyString(st.sval)
                        && (st.sval.charAt(0) == '?' || st.sval.charAt(0) == '@')) {
                        if (inQuantifierVars)
                            addVariable(st,quantifiedVariables);
                        else
                            addVariable(st,allVariables);
                    }
                    lastWasOpen = false;
                } 
                else if (st.ttype==41) {      //----Close bracket                    
                    if (inHOL)                  //----Track nesting of ()s for hol__, so I know when to close the '
                        inHOLCount--;
                    //----End of quantified variable list
                    if (inQuantifierVars) {
                        //----Fake restarting the argument list because the quantified variable list
                        //----does not use the operator from the surrounding expression
                        countStack.pop();
                        countStack.push(0);
                        tptpFormula.append("] : ");
                        inQuantifierVars = false;
                        //----End of predicate or operator list
                    } 
                    else {                        
                        countStack.pop();                //----Pop off the stacks to reveal the next outer layer
                        operatorStack.pop();                        
                        tptpFormula.append(")");         //----Close the expression               
                        if (inHOL && inHOLCount == 0) {  //----If this closes a HOL expression, close the '
                            tptpFormula.append("'");
                            inHOL = false;
                        }                        
                        incrementTOS(countStack);        //----Note that another expression has been completed
                    }
                    lastWasOpen = false;

                    parenLevel--;                    
                    if (parenLevel == 0) {   //----End of the statement being processed. Universally quantify free variables
                        //findFreeVariables(allVariables,quantifiedVariables);
                        allVariables.removeAll(quantifiedVariables);
                        if (allVariables.size() > 0) {
                            if (query)
                                quantification = "? [";
                            else
                                quantification = "! [";
                            for (index = 0; index < allVariables.size(); index++) {
                                if (index > 0)
                                    quantification += ",";
                                quantification += (String)allVariables.elementAt(index);
                            }
                            quantification += "] : ";
                            tptpFormula.insert(0,"( " + quantification);
                            tptpFormula.append(" )");
                        }
                        if (StringUtil.emptyString(translatedFormula))
                            translatedFormula = "( " + tptpFormula.toString() + " )";
                        else
                            translatedFormula += "& ( " + tptpFormula.toString() + " )";
                    } 
                    else if (parenLevel < 0) {
                        throw new ParseException("Parsing error in " + suoString + 
                                "Extra closing bracket at " + tptpFormula.toString(),0);
                    }
                } 
                else if (st.ttype != StreamTokenizer.TT_EOF && st.ttype != StreamTokenizer.TT_EOL) {
                	String error = null;
                	switch (st.ttype) {
                	case StreamTokenizer.TT_EOL : error = "End of line (which should not be an error)"; break;
                	case StreamTokenizer.TT_NUMBER : error = "\nIllegal token '" + st.nval + "'"; break;
                	case StreamTokenizer.TT_WORD : error = "\nIllegal token '" + st.sval + "'"; break;
                	default : error = "\nUnknown illegal token"; break;
                	}
                    throw new ParseException("Parsing error in\n" + suoString + "\n" +
                            error + " with TPTP so far:\n " + tptpFormula.toString(),0);
                }
            } while (st.ttype != StreamTokenizer.TT_EOF);

            //----Bare word like $false didn't get done by a closing)
            if (StringUtil.emptyString(translatedFormula))
                translatedFormula = tptpFormula.toString();
        }
        catch (Exception ex2) {
            System.out.println("Error in SUMOformulaToTPTPformula: " + ex2.getMessage());
            ex2.printStackTrace();
        }
        return translatedFormula;
    }

    /** ***************************************************************
     * Parse formulae into TPTP format
     * Result is returned in _f.theTptpFormulas
     */
    public void tptpParse(Formula input, boolean query, KB kb, List<Formula> preProcessedForms)
        throws ParseException, IOException {

        if (debug)
            System.out.println("INFO in SUMOformulaToTPTPformula.tptpParse(): input: " + input);
        if (debug)
            System.out.println("INFO in SUMOformulaToTPTPformula.tptpParse(): preprocessedForms: " + preProcessedForms);
        _f = input;
        try {
            KBmanager mgr = KBmanager.getMgr();
            if (kb == null)
                kb = new KB("",mgr.getPref("kbDir"));
            if (!_f.isBalancedList()) {
                String errStr = "Unbalanced parentheses or quotes in: " + _f.theFormula;
                _f.errors.add(errStr);
                return;
            }
            List<Formula> processed = preProcessedForms;
            if (processed == null) {
                FormulaPreprocessor fp = new FormulaPreprocessor();
                processed = fp.preProcess(_f,query, kb);
            }
            if (debug)
                System.out.println("INFO in SUMOformulaToTPTPformula.tptpParse(): preprocessed: " + processed);
            if (processed != null) {
                _f.clearTheTptpFormulas();
                //----Performs function on each current processed axiom
                Iterator<Formula> g = processed.iterator();
                while (g.hasNext()) {
                    Formula f = (Formula) g.next();
                    if (!f.theFormula.contains("@")) {
                        String tptpStr = tptpParseSUOKIFString(f.theFormula,query);
                        if (StringUtil.isNonEmptyString(tptpStr)) 
                            _f.getTheTptpFormulas().add(tptpStr);                        
                    }
                }
            }
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            if (ex instanceof ParseException)
                throw (ParseException) ex;
            if (ex instanceof IOException)
                throw (IOException) ex;
        }
        if (query || debug) System.out.println("INFO in SUMOformulaToTPTPformula.tptpParse(): result: " + _f.theTptpFormulas);
        return;
    }

    /** ***************************************************************
     * Parse formulae into TPTP format
     */
    public ArrayList<String> tptpParse(Formula input, boolean query, KB kb) throws ParseException, IOException {

        tptpParse(input,query, kb, null);
        return _f.theTptpFormulas;
    }
    
    /** ***************************************************************
     * A test method.
     */
    public static void testTptpParse() {
        
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB("SUMO");
        
        String teststr = "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) " +
                "(element ?ELEMENT ?SET2))) (equal ?SET1 ?SET2))";
        System.out.println(SUMOformulaToTPTPformula.tptpParseSUOKIFString(teststr,false));
        //note we should expect
        /*  fof(kb_SUMO_3489,axiom,(
        ! [V__SET2,V__SET1] :
            ( ( s__instance(V__SET1,s__Set)
              & s__instance(V__SET2,s__Set) )
               => ( ! [V__ELEMENT] :
                      ( s__element(V__ELEMENT,V__SET1)
                    <=> s__element(V__ELEMENT,V__SET2) )
                 => V__SET1 = V__SET2 ) ) )).
                 
                 and not
                 
                 fof(kb_SUMO_3515,axiom,(( (
      ! [V__SET2,V__SET1,V__ELEMENT] :
          ((! [V__ELEMENT] :
              ((s__instance(V__SET1,s__Set)
              & s__instance(V__SET2,s__Set))
               => (s__element(V__ELEMENT,V__SET1) <=>
                   s__element(V__ELEMENT,V__SET2))))
                   => (V__SET1 = V__SET2))) ))).
                   
        System.out.println(SUMOformulaToTPTPformula.tptpParseSUOKIFString("(agent ?VAR4 ?VAR1)",false));
        System.out.println(SUMOformulaToTPTPformula.tptpParseSUOKIFString("(equal ?VAR4 ?VAR1)",false));        
        System.out.println(SUMOformulaToTPTPformula.tptpParseSUOKIFString("(=> " +
                "  (and " +
                "    (attribute ?H Muslim) " +
                "    (equal " +
                "      (WealthFn ?H) ?W)) " +
                "(modalAttribute " +
                "  (exists (?Z ?T) " +
                "    (and " +
                "      (instance ?Z Zakat) " +
                "      (instance ?Y Year) " +
                "      (during ?Y " +
                "        (WhenFn ?H)) " +
                "      (holdsDuring ?Y " +
                "        (attribute ?H FullyFormed)) " +
                "      (agent ?Z ?H) " +
                "      (patient ?Z ?T) " +
                "      (monetaryValue ?T ?C) " +
                "      (greaterThan ?C " +
                "        (MultiplicationFn ?W 0.025)))) Obligation)) ",false));
                */
    }
    
    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {
        testTptpParse();
    }
}
