package com.articulate.sigma.trans;

import com.articulate.sigma.*;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.text.ParseException;
import java.util.*;

public class SUMOformulaToTPTPformula {

    public Formula _f = null;
    public static boolean debug = false;
    public static boolean hideNumbers = true;
    public static String lang = "fof"; // or "tff"

    /** ***************************************************************
     */
    public SUMOformulaToTPTPformula () {

    }

    /** ***************************************************************
     */
    public SUMOformulaToTPTPformula (String l) {

        lang = l;
    }

    /** ***************************************************************
     * Encapsulates translateWord_1, which translates the logical
     * operators and inequalities in SUO-KIF to their TPTP
     * equivalents.
     *
     * @param st is the StreamTokenizer_s that contains the current token
     * @return the String that is the translated token
     */
    public static String translateWordNew(String st, int type, boolean hasArguments) {

        if (debug) System.out.println("SUMOformulaToTPTPformula.translateWordNew(): input: '" + st + "'");
        if (debug) System.out.println("SUMOformulaToTPTPformula.translateWordNew(): type: " + type);
        if (debug) System.out.println("SUMOformulaToTPTPformula.translateWordNew(): hasArguments: " + hasArguments);
        if (debug) System.out.println("SUMOformulaToTPTPformula.translateWordNew(): containsKey: " + SUMOtoTFAform.numericConstantValues.containsKey(st));
        if (debug) System.out.println("SUMOformulaToTPTPformula.translateWordNew(): lang: " + lang);
        if (debug) System.out.println("translateWordNew(): " + SUMOtoTFAform.numericConstantValues);
        int translateIndex;
        String result = null;
        List<String> kifOps = Arrays.asList(Formula.UQUANT, Formula.EQUANT,
                Formula.NOT, Formula.AND, Formula.OR, Formula.IF, Formula.IFF,
                Formula.EQUAL);
        List<String> tptpOps = Arrays.asList("! ", "? ", "~ ", " & ", " | ", " => ", " <=> ", " = ");

        List<String> kifPredicates =
                Arrays.asList("<=","<",">",">=",
                        "lessThanOrEqualTo","lessThan","greaterThan","greaterThanOrEqualTo");
        List<String> tptpPredicates = Arrays.asList("lesseq","less","greater","greatereq",
                "lesseq","less","greater","greatereq");

        List<String> kifConstants =
                Arrays.asList(Formula.LOG_TRUE, Formula.LOG_FALSE);
        List<String> tptpConstants = Arrays.asList("$true","$false");

        List<String> kifFunctions = Arrays.asList(Formula.TIMESFN, Formula.DIVIDEFN,
                Formula.PLUSFN, Formula.MINUSFN);
        List<String> tptpFunctions = Arrays.asList("product","quotient","sum","difference");

        List<String> kifRelations = new ArrayList<String>();
        kifRelations.addAll(kifPredicates);
        kifRelations.addAll(kifFunctions);
        String mentionSuffix = Formula.termMentionSuffix;
        //----Places single quotes around strings, and replace \n by space
        if (type == 34)
            return("'" + st.replaceAll("[\n\t\r\f]"," ").replaceAll("'","") + "'");
        //----Fix variables to have leading V_
        char ch0 = ((st.length() > 0)
                ? st.charAt(0)
                : 'x');
        char ch1 = ((st.length() > 1)
                ? st.charAt(1)
                : 'x');
        if (ch0 == '?' || ch0 == '@')
            return(Formula.termVariablePrefix + st.substring(1).replace('-','_'));
        //----Translate special constants
        translateIndex = kifConstants.indexOf(st);
        if (translateIndex != -1)
            return(tptpConstants.get(translateIndex) + (hasArguments ? "" : mentionSuffix));

        //----Translate operators
        translateIndex = kifOps.indexOf(st);
        if (translateIndex != -1 && hasArguments) {
            return (tptpOps.get(translateIndex));
        }

        String term = st;
        if (!hasArguments) {
            if ((!term.endsWith(mentionSuffix) && Character.isLowerCase(ch0))
                    || term.endsWith("Fn")
                    || KB.isRelationInAnyKB(term)) {
                term += mentionSuffix;
            }
        }
        if (debug) System.out.println("SUMOformulaToTPTPformula.translateWordNew(): almost done: " + term);
        if (StringUtil.isNumeric(term) && hideNumbers && !lang.equals("tff")) {
            if (term.indexOf(".") > -1)
                term = term.replace('.','_');
            if (term.indexOf("-") > -1)
                term = term.replace('-','_');
            term = "n__" + term;
        }
        if (kifOps.contains(term))
            return(term.replace('-','_')); // shouldn't be needed, no kifOps contain '-'
        else
            return(Formula.termSymbolPrefix + term.replace('-','_'));
    }

    /** ***************************************************************
     * Encapsulates translateWord_1, which translates the logical
     * operators and inequalities in SUO-KIF to their TPTP
     * equivalents.
     *
     * @param st is the StreamTokenizer_s that contains the current token
     * @return the String that is the translated token
     */
    public static String translateWord(String st, int type, boolean hasArguments) {

        if (debug) System.out.println("SUMOformulaToTPTPformula.translateWord(): input: '" + st + "'");
        if (debug) System.out.println("SUMOformulaToTPTPformula.translateWord(): containsKey: " + SUMOtoTFAform.numericConstantValues.containsKey(st));
        if (debug) System.out.println("SUMOformulaToTPTPformula.translateWord(): lang: " + lang);
        if (debug) System.out.println("translateWord(): " + SUMOtoTFAform.numericConstantValues);
        String result = null;
        try {
            //if (lang.equals("tff")) {
            //    if (SUMOtoTFAform.numericConstantValues.containsKey(st)) {
            //        if (debug)
            //            System.out.println("SUMOformulaToTPTPformula.translateWord(): constant " + SUMOtoTFAform.numericConstantValues.get(st));
            //        return (SUMOtoTFAform.numericConstantValues.get(st));
            //    }
           // }
            result = translateWord_1(st,type,hasArguments);
            if (debug) System.out.println("SUMOformulaToTPTPformula.translateWord(): result: " + result);
            if (result.equals("$true__m") || result.equals("$false__m")) 
                result = "'" + result + "'";
            if (StringUtil.isNumeric(result) && hideNumbers && !lang.equals("tff")) {
                if (result.indexOf(".") > -1)
                    result = result.replace('.','_');
                if (result.indexOf("-") > -1)
                    result = result.replace('-','_');
                result = "n__" + result;
            }
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
     * @param hasArguments is whether the given word, st, has arguments or
     *                     is itself and argument.  If it's a term that is
     *                     an argument, and it's a relation, it will get a suffix
     *                     "__m"
     * @return the String that is the translated token
     */
    private static String translateWord_1(String st, int type, boolean hasArguments) {

        if (debug) System.out.println("INFO in SUMOformulaToTPTPformula.translateWord_1(): st: " + st);
        if (debug) System.out.println("INFO in SUMOformulaToTPTPformula.translateWord_1(): hasArguments: " + hasArguments);
        int translateIndex;

        List<String> kifOps = Arrays.asList(Formula.UQUANT, Formula.EQUANT, 
                Formula.NOT, Formula.AND, Formula.OR, Formula.IF, Formula.IFF, 
                Formula.EQUAL);
        List<String> tptpOps = Arrays.asList("! ", "? ", "~ ", " & ", " | ", " => ", " <=> ", " = ");

        List<String> kifPredicates =
                Arrays.asList("<=","<",">",">=",
                        "lessThanOrEqualTo","lessThan","greaterThan","greaterThanOrEqualTo");
        List<String> tptpPredicates = Arrays.asList("lesseq","less","greater","greatereq",
                "lesseq","less","greater","greatereq");

        List<String> kifConstants =
                Arrays.asList(Formula.LOG_TRUE, Formula.LOG_FALSE);
        List<String> tptpConstants = Arrays.asList("$true","$false");

        List<String> kifFunctions = Arrays.asList(Formula.TIMESFN, Formula.DIVIDEFN, 
                Formula.PLUSFN, Formula.MINUSFN);
        List<String> tptpFunctions = Arrays.asList("product","quotient","sum","difference");

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
        if (holdsPrefixInUse && !kifRelations.contains(st))
            mentionSuffix = "";

        //----Places single quotes around strings, and replace \n by space
        if (type == 34)
            return("'" + st.replaceAll("[\n\t\r\f]"," ").replaceAll("'","") + "'");
        //----Fix variables to have leading V_
        char ch0 = ((st.length() > 0)
                    ? st.charAt(0)
                    : 'x');
        char ch1 = ((st.length() > 1)
                    ? st.charAt(1)
                    : 'x');
        if (ch0 == '?' || ch0 == '@')
            return(Formula.termVariablePrefix + st.substring(1).replace('-','_'));

        //----Translate special predicates
        if (lang.equals("tff")) {
            translateIndex = kifPredicates.indexOf(st);
            if (translateIndex != -1)
                return (tptpPredicates.get(translateIndex) + (hasArguments ? "" : mentionSuffix));
        }
        //----Translate special constants
        translateIndex = kifConstants.indexOf(st);
        if (translateIndex != -1)
            return(tptpConstants.get(translateIndex) + (hasArguments ? "" : mentionSuffix));
        //----Translate special functions
        if (lang.equals("tff")) {
            translateIndex = kifFunctions.indexOf(st);
            if (translateIndex != -1)
                return (tptpFunctions.get(translateIndex) + (hasArguments ? "" : mentionSuffix));
        }
        //----Translate operators
        translateIndex = kifOps.indexOf(st);
        if (translateIndex != -1 && hasArguments) {
            return (tptpOps.get(translateIndex));
        }
        //----Do nothing to numbers
        if (type == StreamTokenizer.TT_NUMBER ||
            (st != null && (Character.isDigit(ch0) ||
                                 (ch0 == '-' && Character.isDigit(ch1))))) {
            return(st);
        }
        String term = st;

        if (!hasArguments) {
            if ((!term.endsWith(mentionSuffix) && Character.isLowerCase(ch0))
                     || term.endsWith("Fn")
                     || KB.isRelationInAnyKB(term)) {
                term += mentionSuffix;
            }
        }
        if (kifOps.contains(term))
            return(term.replace('-','_'));  // shouldn't be needed, no kifOps contain '-'
        else
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
            String tptpVariable = translateWord(st.sval,st.ttype,false);
            if (variables.indexOf(tptpVariable) == -1) 
                variables.add(tptpVariable);            
        }
    }

    /** *************************************************************
     */
    private static String processQuant(Formula f, Formula car, String op,
                                       ArrayList<String> args) {

        //if (debug) System.out.println("SUMOformulaToTPTPformula.processQuant(): quantifier");
        if (args.size() < 2) {
            System.out.println("Error in SUMOformulaToTPTPformula.processQuant(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        else {
            //if (debug) System.out.println("SUMOformulaToTPTPformula.processQuant(): correct # of args");
            if (args.get(0) != null) {
                //if (debug) System.out.println("SUMOtoTFAform.processQuant(): valid varlist: " + args.get(0));
                Formula varlist = new Formula(args.get(0));
                ArrayList<String> vars = varlist.argumentsToArrayListString(0);
                //if (debug) System.out.println("SUMOformulaToTPTPformula.processRecurse(): valid vars: " + vars);
                StringBuffer varStr = new StringBuffer();
                for (String v : vars) {
                    String oneVar = SUMOformulaToTPTPformula.translateWord(v,v.charAt(0),false);
                    varStr.append(oneVar + ", ");
                }
                //if (debug) System.out.println("SUMOformulaToTPTPformula.processQuant(): valid vars: " + varStr);
                String opStr = " ! ";
                if (op.equals("exists"))
                    opStr = " ? ";
                //if (debug) System.out.println("SUMOformulaToTPTPformula.processQuant(): quantified formula: " + args.get(1));
                return "(" + opStr + "[" + varStr.toString().substring(0,varStr.length()-2) + "] : (" +
                        processRecurse(new Formula(args.get(1))) + "))";
            }
            else {
                System.out.println("Error in SUMOformulaToTPTPformula.processQuant(): null arguments to " + op + " in " + f);
                return "";
            }
        }
    }

    /** *************************************************************
     */
    private static String processConjDisj(Formula f, Formula car,
                                          ArrayList<String> args) {

        String op = car.getFormula();
        if (args.size() < 2) {
            System.out.println("Error in SUMOformulaToTPTPformula.processConjDisj(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        String tptpOp = "&";
        if (op.equals("or"))
            tptpOp = "|";
        StringBuffer sb = new StringBuffer();
        sb.append("(" + processRecurse(new Formula(args.get(0))));
        for (int i = 1; i < args.size(); i++) {
            sb.append(" " + tptpOp + " " + processRecurse(new Formula(args.get(i))));
        }
        sb.append(")");
        return sb.toString();
    }

    /** *************************************************************
     */
    public static String processLogOp(Formula f, Formula car, ArrayList<String> args) {

        String op = car.getFormula();
        //System.out.println("processRecurse(): op: " + op);
        //System.out.println("processRecurse(): args: " + args);
        if (op.equals("and"))
            return processConjDisj(f,car,args);
        if (op.equals("=>")) {
            if (args.size() < 2) {
                System.out.println("Error in SUMOformulaToTPTPformula.processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return "(" + processRecurse(new Formula(args.get(0))) + " => " +
                        "(" + processRecurse(new Formula(args.get(1))) + "))";
        }
        if (op.equals("<=>")) {
            if (args.size() < 2) {
                System.out.println("Error in SUMOformulaToTPTPformula.processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return "((" + processRecurse(new Formula(args.get(0))) + " => " +
                        processRecurse(new Formula(args.get(1))) + ") & (" +
                        processRecurse(new Formula(args.get(1))) + " => " +
                        processRecurse(new Formula(args.get(0))) + "))";
        }
        if (op.equals("or"))
            return processConjDisj(f,car,args);
        if (op.equals("not")) {
            if (args.size() != 1) {
                System.out.println("Error in SUMOformulaToTPTPformula.processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return "~(" + processRecurse(new Formula(args.get(0))) + ")";
        }
        if (op.equals("forall") || op.equals("exists"))
            return processQuant(f,car,op,args);
        System.out.println("Error in SUMOformulaToTPTPformula.processLogOp(): bad logical operator " + op + " in " + f);
        return "";
    }

    /** *************************************************************
     */
    public static String processEquals(Formula f, Formula car, ArrayList<String> args) {

        String op = car.getFormula();
        if (args.size() != 2) {
            System.out.println("Error in SUMOformulaToTPTPformula.processCompOp(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        if (op.startsWith("equal")) {
            return "(" + processRecurse(new Formula(args.get(0))) + " = " +
                    processRecurse(new Formula(args.get(1))) + ")";
        }
        System.out.println("Error in SUMOformulaToTPTPformula.processCompOp(): bad comparison operator " + op + " in " + f);
        return "";
    }

    /** *************************************************************
     */
    public static String processRecurse(Formula f) {

        if (debug) System.out.println("SUMOformulaToTPTPformula.processRecurse(): " + f);
        if (f == null)
            return "";
        if (f.atom()) {
            int ttype = f.getFormula().charAt(0);
            if (Character.isDigit(ttype))
                ttype = StreamTokenizer_s.TT_NUMBER;
            return SUMOformulaToTPTPformula.translateWord(f.getFormula(),ttype,false);
        }
        Formula car = f.carAsFormula();
        //System.out.println("SUMOformulaToTPTPformula.processRecurse(): car: " + car);
        //System.out.println("SUMOformulaToTPTPformula.processRecurse(): car: " + car.theFormula);
        ArrayList<String> args = f.complexArgumentsToArrayListString(1);
        if (car.listP()) {
            System.out.println("Error in SUMOformulaToTPTPformula.processRecurse(): formula " + f);
            return "";
        }
        if (Formula.isLogicalOperator(car.getFormula()))
            return processLogOp(f,car,args);
        else if (car.getFormula().equals("equal"))
            return processEquals(f,car,args);
        else {
            if (debug) System.out.println("SUMOformulaToTPTPformula.processRecurse(): not math or comparison op: " + car);
            StringBuffer argStr = new StringBuffer();
            for (String s : args) {
                if (car.getFormula().equals("instance")) {
                    int ttype = f.getFormula().charAt(0);
                    if (Character.isDigit(ttype))
                        ttype = StreamTokenizer_s.TT_NUMBER;
                    if (Formula.atom(s))
                        argStr.append(SUMOformulaToTPTPformula.translateWord(s,ttype,false) + ",");
                    else
                        argStr.append(processRecurse(new Formula(s)) + ",");
                }
                else
                    argStr.append(processRecurse(new Formula(s)) + ",");
            }
            String result = "s__" + car.getFormula() + "(" + argStr.substring(0,argStr.length()-1) + ")";
            //if (debug) System.out.println("SUMOformulaToTPTPformula.processRecurse(): result: " + result);
            return result;
        }
    }

    /** ***************************************************************
     * Parse a single formula into TPTP format
     */
    public static String tptpParseSUOKIFString(String suoString, boolean query) {

        return "( " + process(new Formula(suoString),query) + " )";
    }

    /** *************************************************************
     * This is the primary method of the class.  It takes a SUO-KIF
     * formula and returns a TPTP formula.
     */
    public static String process(Formula f, boolean query) {

        if (f == null) {
            if (debug) System.out.println("Error in SUMOformulaToTPTPformula.process(): null formula: ");
            return "";
        }
        if (f.atom())
            return SUMOformulaToTPTPformula.translateWord(f.getFormula(),f.getFormula().charAt(0),false);
        if (f != null && f.listP()) {
            HashSet<String> UqVars = f.collectUnquantifiedVariables();
            if (debug) System.out.println("SUMOformulaToTPTPformula.process(): unquant: " + UqVars);
            String result = processRecurse(f);
            if (debug) System.out.println("SUMOformulaToTPTPformula.process(): result 1: " + result);
            StringBuffer qlist = new StringBuffer();
            for (String s : UqVars) {
                if (debug) System.out.println("process(): s: " + s);
                String t = "";
                String oneVar = SUMOformulaToTPTPformula.translateWord(s,s.charAt(0),false);
                qlist.append(oneVar + ",");
            }
            if (qlist.length() > 1) {
                qlist.deleteCharAt(qlist.length() - 1);  // delete final comma
                String quantification = "! [";
                if (query)
                    quantification = "? [";
                result = "( " + quantification + qlist + "] : (" + result + " ) )";
            }
            if (debug) System.out.println("SUMOformulaToTPTPformula.process(): result 2: " + result);
            return result;
        }
        return (f.getFormula());
    }

    /** ***************************************************************
     * Parse a single formula into TPTP format
     */
    public static String tptpParseSUOKIFStringOld(String suoString, boolean query) {

        if (debug) System.out.println("INFO in SUMOformulaToTPTPformula.tptpParseSUOKIFString(): input: " + suoString);
        if (query || debug) System.out.println("INFO in SUMOformulaToTPTPformula.tptpParseSUOKIFString(): input: " + suoString);
        Formula tempF = new Formula();      // Special case to rename Foo for (instance Foo SetOrClass)
        tempF.read(suoString);              // so a symbol can't be both a class and an instance. However,
                                            // this may not be needed and we might just not allow a class 
                                            // to be an instance of itself
        if (tempF.getArgument(0).equals("instance") &&
            tempF.getArgument(2).equals("SetOrClass")) {
            String arg1 = tempF.getStringArgument(1);
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
                if (debug) System.out.println("INFO in SUMOformulaToTPTPformula.tptpParseSUOKIFString(): looping: " + st);
                if (debug) System.out.println("tptpParseSUOKIFString(): lastWasOpen 1: " + lastWasOpen);
                if (debug) System.out.println("tptpParseSUOKIFString(): so far: " + tptpFormula);
                st.nextToken();
                if (st.ttype == 40) {         //----Open paren
                    if (debug) System.out.println("tptpParseSUOKIFString(): open bracket: " + st);
                    if (lastWasOpen)      //----Should not have ((in KIF
                        throw new ParseException("Parsing error in " + suoString + ". Doubled open parentheses.",0);                                       
                    if (inHOL)              //----Track nesting of ()s for hol__, so I know when to close the '
                        inHOLCount++;
                    lastWasOpen = true;
                    parenLevel++;
                    if (debug) System.out.println("tptpParseSUOKIFString(): lastWasOpen 2: " + lastWasOpen);
                } 
                else if (st.ttype == StreamTokenizer.TT_WORD &&  //----Operators
                           (((arity = operatorArity(st)) > 0) || st.sval.equals(Formula.EQUAL))) {
                    if (debug) System.out.println("tptpParseSUOKIFString(): operator: " + st);
                    if (st.sval.equals(Formula.EQUAL))
                        arity = 2;
                    if (debug) System.out.println("tptpParseSUOKIFString(): lastWasOpen 3: " + lastWasOpen);
                    if (!lastWasOpen) {             //----Operators must be preceded by a ( or else they are arguments
                        if (countStack.peek() > 0) {
                            if (operatorStack.peek() != ",") tptpFormula.append(" ");
                            tptpFormula.append(operatorStack.peek());
                            if (operatorStack.peek() != ",") tptpFormula.append(" ");
                        }
                        // TODO: may have to trap strings - AP
                        tptpFormula.append(translateWordNew(st.sval, st.ttype, false));      //----Output the word
                        incrementTOS(countStack);                         //----Increment counter for this level
                    }
                    else {
                        //----This is the start of a new term - put in the infix operator if not the
                        //----first term for this operator
                        if (countStack.peek() > 0) {
                            // System.out.println("  1 : countStack == " + countStack);
                            // System.out.println("  1 : operatorStack == " + operatorStack);
                            tptpFormula.append(operatorStack.peek());
                        }
                        if (inHOL && inHOLCount == 1)     //----If this is the start of a hol__ situation, quote it all
                            tptpFormula.append("'");
                        tptpFormula.append("(");          //----()s around all operator expressions
                        if (arity == 1) {                 //----Output unary as prefix
                            tptpFormula.append(translateWordNew(st.sval, st.ttype, true));
                            countStack.push(Integer.valueOf(0));   //----Note the new operator (dummy) with 0 operands so far
                            operatorStack.push(",");
                            //----Check if the next thing will be the quantified variables
                            if (st.sval.equals("forall") || st.sval.equals("exists"))
                                inQuantifierVars = true;
                        }
                        else if (arity == 2) {    //----Binary operator
                            //----Note the new operator with 0 operands so far
                            countStack.push(Integer.valueOf(0));
                            operatorStack.push(translateWordNew(st.sval, st.ttype, true));
                        }
                        lastWasOpen = false;
                    }
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
                    if (debug) System.out.println("tptpParseSUOKIFString(): lastWasOpen 4: " + lastWasOpen);
                    if (lastWasOpen) {          //----Start of a predicate or variable list
                        if (inQuantifierVars) { //----Variable list
                            tptpFormula.append("[");
                            tptpFormula.append(translateWordNew(st.sval,st.ttype,false));
                            incrementTOS(countStack);
                        } 
                        else {                //----Predicate
                            //----This is the start of a new term - put in the infix operator if not the
                            //----first term for this operator
                            if (countStack.peek() > 0) {
                                if (operatorStack.peek() != ",") tptpFormula.append(" ");
                                tptpFormula.append(operatorStack.peek());
                                if (operatorStack.peek() != ",") tptpFormula.append(" ");
                            }
                            //----If this is the start of a hol__ situation, quote it all
                            if (inHOL && inHOLCount == 1)
                                tptpFormula.append("'");                            
                            tptpFormula.append(translateWordNew(st.sval,st.ttype,true));   //----Predicate or function and (
                            tptpFormula.append("(");                            
                            countStack.push(Integer.valueOf(0));              //----Note the , for between arguments with 0 arguments so far
                            operatorStack.push(",");
                        }
                        //----Argument or quantified variable
                    } 
                    else {
                        if (debug) System.out.println("tptpParseSUOKIFString(): lastWasOpen 5: " + lastWasOpen);
                        //----This is the start of a new term - put in the infix operator if not the
                        //----first term for this operator
                        if (countStack.peek() > 0) {
                            if (operatorStack.peek() != ",") tptpFormula.append(" ");
                            tptpFormula.append(operatorStack.peek());
                            if (operatorStack.peek() != ",") tptpFormula.append(" ");
                        }
                        // TODO: may have to trap strings - AP
                        tptpFormula.append(translateWordNew(st.sval,st.ttype,false));      //----Output the word
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
                else if (st.ttype == 41) {      //----Close parentheses
                    if (debug) System.out.println("tptpParseSUOKIFString(): close bracket: " + st);
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
                                quantification += (String) allVariables.elementAt(index);
                            }
                            quantification += "] : (";
                            tptpFormula.insert(0,"( " + quantification);
                            tptpFormula.append(" ))");
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
                    if (debug) System.out.println("tptpParseSUOKIFString(): eol: " + st);
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
            System.out.println("Error in SUMOformulaToTPTPformula.tptpParseSUOKIFString(): ");
            System.out.println(ex2.getMessage());
            ex2.printStackTrace();
        }
        if (translatedFormula != null)
            translatedFormula = translatedFormula.replaceAll("  "," ");
        return translatedFormula;
    }

    /** ***************************************************************
     * Parse formulae into TPTP format
     * Result is returned in _f.theTptpFormulas
     */
    @Deprecated  // call tptpParseSUOKIFString() directly
    public void tptpParse(Formula input, boolean query, KB kb, Set<Formula> preProcessedForms)
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
                String errStr = "Unbalanced parentheses or quotes in: " + _f.getFormula();
                _f.errors.add(errStr);
                return;
            }
            Set<Formula> processed = preProcessedForms;
            if (processed == null) {
                FormulaPreprocessor fp = new FormulaPreprocessor();
                processed = fp.preProcess(_f,query, kb);
            }
            if (debug)
                System.out.println("INFO in SUMOformulaToTPTPformula.tptpParse(): preprocessed: " + processed);
            if (processed != null) {
                _f.theTptpFormulas = new HashSet<String>();
                //----Performs function on each current processed axiom
                Iterator<Formula> g = processed.iterator();
                while (g.hasNext()) {
                    Formula f = (Formula) g.next();
                    if (!f.getFormula().contains("@") && !f.higherOrder) {
                        String tptpStr = tptpParseSUOKIFString(f.getFormula(),query);
                        if (StringUtil.isNonEmptyString(tptpStr)) 
                            _f.theTptpFormulas.add(tptpStr);
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
    @Deprecated
    public Set<String> tptpParse(Formula input, boolean query, KB kb) throws ParseException, IOException {

        tptpParse(input,query, kb, null);
        return _f.theTptpFormulas;
    }
    
    /** ***************************************************************
     * A test method.
     */
    public static void testTptpParse() {
        
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        
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
    public static void testTptpParse2() {

        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        String teststr = "\n" +
                "(<=>\n" +
                "    (instance ?NUMBER NegativeRealNumber)\n" +
                "    (and\n" +
                "        (lessThan ?NUMBER 0)\n" +
                "        (instance ?NUMBER RealNumber)))";
        System.out.println(SUMOformulaToTPTPformula.tptpParseSUOKIFString(teststr, false));
    }

    /** ***************************************************************
     * A test method.
     */
    public static void testTptpParse3() {

        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        String teststr = "(instance equal BinaryPredicate)";
        System.out.println(SUMOformulaToTPTPformula.tptpParseSUOKIFString(teststr, false));
    }

    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {
        testTptpParse3();
    }
}
