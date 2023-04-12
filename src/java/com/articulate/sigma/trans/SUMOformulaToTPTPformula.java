package com.articulate.sigma.trans;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.StringUtil;

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
    public static StringBuffer qlist;

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
    public static String translateWord(String st, int type, boolean hasArguments) {

        if (debug) System.out.println("SUMOformulaToTPTPformula.translateWord(): input: '" + st + "'");
        if (debug) System.out.println("SUMOformulaToTPTPformula.translateWord(): containsKey: " + SUMOtoTFAform.numericConstantValues.containsKey(st));
        if (debug) System.out.println("SUMOformulaToTPTPformula.translateWord(): lang: " + lang);
        if (debug) System.out.println("translateWord(): " + SUMOtoTFAform.numericConstantValues);
        String result = null;
        try {
            result = translateWord_1(st,type,hasArguments);
            if (debug) System.out.println("SUMOformulaToTPTPformula.translateWord(): result: " + result);
            if (result.equals("$true" + Formula.termMentionSuffix) || result.equals("$false" + Formula.termMentionSuffix))
                result = "'" + result + "'";
            if (StringUtil.isNumeric(result) && hideNumbers && !lang.equals("tff")) {
                if (result.indexOf(".") > -1)
                    result = result.replace('.','_');
                if (result.indexOf("-") > -1)
                    result = result.replace('-','_');
                result = "n__" + result;
            }
            if (!StringUtil.isNumeric(result)) {
                if (result.indexOf(".") > -1)
                    result = result.replace('.','_');
                if (result.indexOf("-") > -1)
                    result = result.replace('-','_');
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
            return(term);
        else
            return(Formula.termSymbolPrefix + term);
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

        if (debug) System.out.println("SUMOformulaToTPTPformula.processQuant(): quantifier");
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
        if (debug) System.out.println("processLogOp(): op: " + op);
        if (debug) System.out.println("processLogOp(): args: " + args);
        if (op.equals("and"))
            return processConjDisj(f,car,args);
        if (op.equals("=>")) {
            if (args.size() < 2) {
                System.out.println("Error in SUMOformulaToTPTPformula.processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else {
                if (KBmanager.getMgr().prover == KBmanager.Prover.EPROVER)
                    return "(" + processRecurse(new Formula(args.get(0))) + " => " +
                        "(" + processRecurse(new Formula(args.get(1))) + "))";
                else
                    return "(" + processRecurse(new Formula(args.get(0))) + " => " +
                            processRecurse(new Formula(args.get(1))) + ")";
            }
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
            String result = translateWord(car.getFormula(), StreamTokenizer.TT_WORD,true) + "(" + argStr.substring(0,argStr.length()-1) + ")";
            //if (debug) System.out.println("SUMOformulaToTPTPformula.processRecurse(): result: " + result);
            return result;
        }
    }

    /** *************************************************************
     * This is the primary method of the class.  It takes a SUO-KIF
     * formula and returns a TPTP formula.
     */
    public static void generateQList(Formula f) {

        HashSet<String> UqVars = f.collectUnquantifiedVariables();
        qlist = new StringBuffer();
        for (String s : UqVars) {
            String oneVar = SUMOformulaToTPTPformula.translateWord(s,s.charAt(0),false);
            qlist.append(oneVar + ",");
        }
        if (qlist.length() > 1)
            qlist.deleteCharAt(qlist.length() - 1);  // delete final comma
    }

    /** ***************************************************************
     * Parse a single formula into TPTP format
     */
    public static String tptpParseSUOKIFString(String suoString, boolean query) {

        if (debug) System.out.println("SUMOformulaToTPTPformula.process(): string,query,lang: " + suoString + ", " + query + ", " + SUMOKBtoTPTPKB.lang);
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        if (SUMOKBtoTPTPKB.lang.equals("tff"))
            return "( " + SUMOtoTFAform.process(suoString,query) + " )";
        if (SUMOKBtoTPTPKB.lang.equals("thf")) {
            THF thf = new THF();
            Collection<Formula> stmts = new ArrayList<Formula>();
            Collection<Formula> queries = new ArrayList<Formula>();
            if (query)
                queries.add(new Formula(suoString));
            else
                stmts.add(new Formula(suoString));
            return "( " + thf.KIF2THF(stmts,queries,kb) + " )";
        }
        if (SUMOKBtoTPTPKB.lang.equals("fof"))
            return "( " + process(new Formula(suoString),query) + " )";
        System.out.println("ERROR in tptpParseSUOKIFString(): unknown language type: " + SUMOKBtoTPTPKB.lang);
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
            String result = processRecurse(f);
            if (debug) System.out.println("SUMOformulaToTPTPformula.process(): result 1: " + result);
            generateQList(f);
            if (debug) System.out.println("SUMOformulaToTPTPformula.process(): qlist: " + qlist);
            if (qlist.length() > 1) {
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
                for (Formula f : processed) {
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
