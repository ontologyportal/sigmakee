package com.articulate.sigma.trans;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class THFnew {

    public static boolean debug = true;
    public static int axNum = 0;

    /** *************************************************************
     */
    private static String processQuant(Formula f, String op,
                                       ArrayList<String> args,
                                       HashMap<String, HashSet<String>> typeMap) {

        if (debug) System.out.println("THFnew.processQuant(): quantifier");
        if (debug) System.out.println("THFnew.processQuant(): typeMap: " + typeMap);
        if (args.size() < 2) {
            System.out.println("Error in THFnew.processQuant(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        else {
            if (debug) System.out.println("THFnew.processQuant(): correct # of args");
            if (args.get(0) != null) {
                if (debug) System.out.println("THFnew.processQuant(): valid varlist: " + args.get(0));
                Formula varlist = new Formula(args.get(0));
                ArrayList<String> vars = varlist.argumentsToArrayListString(0);
                //if (debug) System.out.println("THFnew.processRecurse(): valid vars: " + vars);
                StringBuffer varStr = new StringBuffer();
                varStr.append(generateQList(f,typeMap,new HashSet(vars)));
                if (debug) System.out.println("THFnew.processQuant(): valid vars: " + varStr);
                String opStr = " ! ";
                if (op.equals("exists"))
                    opStr = " ? ";
                if (debug) System.out.println("THFnew.processQuant(): quantified formula: " + args.get(1));
                return "(" + opStr + "[" + varStr + "] : (" +
                        processRecurse(new Formula(args.get(1)),typeMap) + "))";
            }
            else {
                System.out.println("Error in THFnew.processQuant(): null arguments to " + op + " in " + f);
                return "";
            }
        }
    }

    /** *************************************************************
     */
    private static String processConjDisj(Formula f, Formula car,
                                          ArrayList<String> args,
                                          HashMap<String, HashSet<String>> typeMap) {

        String op = car.getFormula();
        if (args.size() < 2) {
            System.out.println("Error in THFnew.processConjDisj(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        String tptpOp = "&";
        if (op.equals("or"))
            tptpOp = "|";
        StringBuffer sb = new StringBuffer();
        sb.append("(" + processRecurse(new Formula(args.get(0)),typeMap));
        for (int i = 1; i < args.size(); i++) {
            sb.append(" " + tptpOp + " " + processRecurse(new Formula(args.get(i)),typeMap));
        }
        sb.append(")");
        return sb.toString();
    }

    /** *************************************************************
     */
    public static String processLogOp(Formula f, Formula car, ArrayList<String> args,
                                      HashMap<String, HashSet<String>> typeMap) {

        String op = car.getFormula();
        if (debug) System.out.println("processLogOp(): op: " + op);
        if (debug) System.out.println("processLogOp(): args: " + args);
        if (debug) System.out.println("THFnew.processLogOp(): typeMap: " + typeMap);
        if (op.equals("and"))
            return processConjDisj(f,car,args,typeMap);
        if (op.equals("=>")) {
            if (args.size() < 2) {
                System.out.println("Error in THFnew.processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else {
                if (KBmanager.getMgr().prover == KBmanager.Prover.EPROVER)
                    return "(" + processRecurse(new Formula(args.get(0)),typeMap) + " => " +
                            "(" + processRecurse(new Formula(args.get(1)),typeMap) + "))";
                else
                    return "(" + processRecurse(new Formula(args.get(0)),typeMap) + " => " +
                            processRecurse(new Formula(args.get(1)),typeMap) + ")";
            }
        }
        if (op.equals("<=>")) {
            if (args.size() < 2) {
                System.out.println("Error in THFnew.processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return "((" + processRecurse(new Formula(args.get(0)),typeMap) + " => " +
                        processRecurse(new Formula(args.get(1)),typeMap) + ") & (" +
                        processRecurse(new Formula(args.get(1)),typeMap) + " => " +
                        processRecurse(new Formula(args.get(0)),typeMap) + "))";
        }
        if (op.equals("or"))
            return processConjDisj(f,car,args,typeMap);
        if (op.equals("not")) {
            if (args.size() != 1) {
                System.out.println("Error in THFnew.processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return "~(" + processRecurse(new Formula(args.get(0)),typeMap) + ")";
        }
        if (op.equals("forall") || op.equals("exists"))
            return processQuant(f,op,args,typeMap);
        System.out.println("Error in THFnew.processLogOp(): bad logical operator " + op + " in " + f);
        return "";
    }

    /** *************************************************************
     */
    public static String processEquals(Formula f, Formula car, ArrayList<String> args,
                                       HashMap<String, HashSet<String>> typeMap) {

        String op = car.getFormula();
        if (args.size() != 2) {
            System.out.println("Error in THFnew.processCompOp(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        if (op.startsWith("equal")) {
            return "(" + processRecurse(new Formula(args.get(0)),typeMap) + " = " +
                    processRecurse(new Formula(args.get(1)),typeMap) + ")";
        }
        System.out.println("Error in THFnew.processCompOp(): bad comparison operator " + op + " in " + f);
        return "";
    }

    /** *************************************************************
     */
    public static String processRecurse(Formula f, HashMap<String, HashSet<String>> typeMap) {

        if (debug) System.out.println("THFnew.processRecurse(): " + f);
        if (debug) System.out.println("THFnew.processRecurse(): typeMap: " + typeMap);
        if (f == null)
            return "";
        if (f.atom()) {
            int ttype = f.getFormula().charAt(0);
            if (Character.isDigit(ttype))
                ttype = StreamTokenizer_s.TT_NUMBER;
            return SUMOformulaToTPTPformula.translateWord(f.getFormula(),ttype,false);
        }
        Formula car = f.carAsFormula();
        //System.out.println("THFnew.processRecurse(): car: " + car);
        //System.out.println("THFnew.processRecurse(): car: " + car.theFormula);
        ArrayList<String> args = f.complexArgumentsToArrayListString(1);
        if (car.listP()) {
            System.out.println("Error in THFnew.processRecurse(): formula " + f);
            return "";
        }
        if (Formula.isLogicalOperator(car.getFormula()))
            return processLogOp(f,car,args,typeMap);
        else if (car.getFormula().equals("equal"))
            return processEquals(f,car,args,typeMap);
        else {
            if (debug) System.out.println("THFnew.processRecurse(): not math or comparison op: " + car);
            StringBuffer argStr = new StringBuffer();
            for (String s : args) {
                if (car.getFormula().equals("instance")) {
                    int ttype = f.getFormula().charAt(0);
                    if (Character.isDigit(ttype))
                        ttype = StreamTokenizer_s.TT_NUMBER;
                    if (Formula.atom(s))
                        argStr.append(SUMOformulaToTPTPformula.translateWord(s,ttype,false));
                    else
                        argStr.append(processRecurse(new Formula(s),typeMap));
                }
                else
                    argStr.append(processRecurse(new Formula(s),typeMap));
                argStr.append(" @ ");
            }
            argStr.delete(argStr.length()-2,argStr.length());  // remove final arg separator
            String result = "(" + SUMOformulaToTPTPformula.translateWord(car.getFormula(),
                    StreamTokenizer.TT_WORD,true) + " @ " + argStr.substring(0,argStr.length()-1) + ")";
            //if (debug) System.out.println("THFnew.processRecurse(): result: " + result);
            return result;
        }
    }

    /** *************************************************************
     */
    public static String getTHFtype(String v, HashMap<String, HashSet<String>> typeMap) {

        if (debug) System.out.println("THFnew.getTHFtype(): typeMap: "  + typeMap);
        if (debug) System.out.println("THFnew.getTHFtype(): typeMap(v): " + v + ":" + typeMap.get(v));
        if (typeMap.get(v) == null)
            return "$i";
        if (typeMap.get(v).contains("Formula"))
            return "($i > $o)";
        if (typeMap.get(v).contains("World"))
            return "$w";
        return "$i";
    }

    /** *************************************************************
     */
    public static String generateQList(Formula f, HashMap<String,
            HashSet<String>> typeMap, HashSet<String> vars) {

        if (debug) System.out.println("THFnew.generateQList(): typeMap: " + typeMap);
        StringBuffer qlist = new StringBuffer();
        for (String s : vars) {
            String thftype = getTHFtype(s,typeMap);
            String oneVar = SUMOformulaToTPTPformula.translateWord(s,s.charAt(0),false);
            qlist.append(oneVar + ":" + thftype + ",");
        }
        if (qlist.length() > 1)
            qlist.deleteCharAt(qlist.length() - 1);  // delete final comma
        return qlist.toString();
    }

    /** *************************************************************
     * This is the primary method of the class.  It takes a SUO-KIF
     * formula and returns a THF formula.
     */
    public static String process(Formula f, HashMap<String, HashSet<String>> typeMap, boolean query) {

        if (debug) System.out.println("THFnew.process(): typeMap: " + typeMap);
        if (f == null) {
            if (debug) System.out.println("Error in THFnew.process(): null formula: ");
            return "";
        }
        if (f.atom())
            return SUMOformulaToTPTPformula.translateWord(f.getFormula(),f.getFormula().charAt(0),false);
        if (f != null && f.listP()) {
            String result = processRecurse(f,typeMap);
            if (debug) System.out.println("THFnew.process(): result 1: " + result);
            HashSet<String> UqVars = f.collectUnquantifiedVariables();
            String qlist = generateQList(f,typeMap,UqVars);
            if (debug) System.out.println("THFnew.process(): typeMap: " + typeMap);
            if (debug) System.out.println("THFnew.process(): qlist: " + qlist);
            if (qlist.length() > 1) {
                String quantification = "! [";
                if (query)
                    quantification = "? [";
                result = "( " + quantification + qlist + "] : (" + result + " ) )";
            }
            if (debug) System.out.println("THFnew.process(): result 2: " + result);
            return result;
        }
        return (f.getFormula());
    }

    /** ***************************************************************
     */
    public static boolean variableArity(KB kb, String pred) {

        System.out.println("variableArity(): pred: " + pred); // eliminate prefix and suffix
        if (pred.length() > 4)
            System.out.println("variableArity(): sub: " + pred.substring(0,pred.length()-3));
        if (!pred.contains("_") || pred.length() < 4)
            return false;
        return kb.isInstanceOf(pred.substring(0,pred.length()-3),"VariableArityRelation");
    }

    /** ***************************************************************
     * Adding the world argument messes up pre-processing for variable
     * arity relations, so we have to decrement the numerical suffix
     * as a hack.
     * (s__partition__4 @ s__PsychologicalAttribute @ s__StateOfMind @ s__TraitAttribute @ V__W1) )
     * needs to be s__partition__3
     */
    public static Formula adjustArity(KB kb, Formula f) {

        System.out.println("adjustArity(): f: " + f);
        String fstr = f.cdr().substring(1);
        String pred = f.car();
        Pattern p = Pattern.compile("([\\w_]+__)(\\d)");
        Matcher m = p.matcher(pred);
        if (m.find()) {
            int num = Integer.parseInt(m.group(2));
            num--;
            Formula result = new Formula("(" + m.group(1) + num + " " + fstr);
            System.out.println("adjustArity(): result: " + result);
            return result;
        }
        return f;
    }

    /** ***************************************************************
     */
    public static void oneTrans(KB kb, Formula f, BufferedWriter bw) throws IOException {

        Formula res = Modals.processModals(f, kb);
        if (res != null) {
            FormulaPreprocessor fp = new FormulaPreprocessor();
            Set<Formula> processed = fp.preProcess(res, false, kb);
            if (debug) System.out.println("oneTrans(): processed: " + processed);
            HashMap<String, HashSet<String>> typeMap = fp.findAllTypeRestrictions(res, kb);
            typeMap.putAll(res.varTypeCache);
            HashSet<String> types = new HashSet<String>();
            types.add("World");
            typeMap.put("?W",types);
            if (debug) System.out.println("THFnew.oneTrans(): typeMap: " + typeMap);
            for (Formula fnew : processed) {
                if (debug) System.out.println("THFnew.oneTrans(): variableArity(kb,fnew.car()): " + variableArity(kb,fnew.car()));
                if (variableArity(kb,fnew.car()))
                    fnew = adjustArity(kb,fnew);   // hack to correct arity after adding world argument
                if (fnew.getFormula().startsWith("(instance ") &&
                        fnew.getFormula().endsWith("Class)")) {
                    fnew.read("(forall (?W) " +
                            fnew.getFormula().substring(0, fnew.getFormula().length() - 1) +
                            " ?W))");
                    types = new HashSet<String>();
                    types.add("World");
                    fnew.varTypeCache.put("?W",types);  // add the "World" type for the ?W since this is post-processModals()
                }
                if (bw == null)
                    System.out.println(process(new Formula(fnew), typeMap, false));
                else
                    bw.write("thf(ax" + axNum++ + ",axiom," +
                            process(new Formula(fnew), typeMap, false) + ").\n");
            }
        }
    }

    /** ***************************************************************
     */
    public static boolean exclude(Formula f, KB kb) {

        if (f.getFormula().contains(Formula.LOG_FALSE) || f.getFormula().contains(Formula.LOG_TRUE))
            return true;
        if (f.isGround())
            for (String s : f.argumentsToArrayListString(0))
                if (exclude(s))
                    return true;
        String pred = f.car();
        return exclude(pred);
    }

    /** ***************************************************************
     */
    public static boolean exclude(String pred) {

        if (pred.equals("documentation") ||
            pred.equals("termFormat") ||
            pred.equals("format") ||
            pred.equals("externalImage") ||
            pred.equals("comment") ||
            StringUtil.isNumeric(pred) ||
            pred.equals("equal") ||
            pred.equals("=") ||
            pred.equals(Formula.LOG_FALSE) ||
            pred.equals(Formula.LOG_TRUE) ||
            Formula.isLogicalOperator(pred))
            return true;
        else
            return false;
    }

    /** ***************************************************************
     */
    public static String sigString(List<String> sig, KB kb, boolean function) {

        System.out.println("sigString(): sig: " + sig);
        StringBuffer sb = new StringBuffer();
        boolean first = false;
        String range = "";
        if (function)
            first = true;
        for (String t : sig) {
            if (t.equals(""))
                continue;
            if (first) {
                range = t;
                first = false;
            }
            else if (kb.isInstanceOf(t,"Formula") || t.equals("Formula"))
                sb.append("$o > ");
            else if (kb.isInstanceOf(t,"World") || t.equals("World"))
                sb.append("$w > ");
            else
                sb.append("$i > ");
        }
        //if (sb.length() > 2)
        //    sb.delete(sb.length()-2,sb.length());  // remove final arg separator
        //else
        //    System.out.println("Error in sigString(): for sig: " + sig);
        //sb.append("w > ");
        if (function) {
            if (kb.isInstanceOf(range,"Formula") || range.equals("Formula"))
                sb.append("$o");
            else
                sb.append("$i");
        }
        else
            sb.append("$o");
        return sb.toString();
    }

    /** ***************************************************************
     */
    public static void writeIntegerTypes(KB kb, BufferedWriter out) throws IOException {

        for (int i = 0; i < 7; i++) {
            out.write("thf(n__" + i + "_tp,type,(n__" + i + " : $i)).\n");
        }
    }

    /** ***************************************************************
     */
    public static void writeTypes(KB kb, BufferedWriter out) throws IOException {

        writeIntegerTypes(kb,out);
        for (String t : kb.terms) {
            if (exclude(t))
                continue;
            if (kb.isInstanceOf(t,"Relation")) {
                List<String> sig = kb.kbCache.signatures.get(t);
                System.out.println("THFnew.writeTypes(): sig " + sig + " for " + t);
                if (!Formula.isLogicalOperator(t) && !t.equals("equals")) { // make sure to update the signature
                    sig.add("World");
                }
                if (t == null) {
                    System.out.println("Error in THFnew.writeTypes(): bad sig for " + t);
                    continue;
                }
                boolean isFunction = false;
                if (kb.isInstanceOf(t,"Function")) {
                    out.write("thf(" + t + "_tp,type,(" +
                            SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true) + " : ("); // write signature
                    isFunction = true;
                }
                else
                    out.write("thf(" + t + "_tp,type,(" +
                            SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true) + " : ("); // write signature
                String sigStr = sigString(sig,kb,isFunction);
                out.write(sigStr + "))).\n");
                out.write("thf(" + t + "_tp,type,(" +
                        SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),false) + " : $i)).\n"); // write relation constant
            }
            else
                out.write("thf(" + t + "_tp,type,(" +
                        SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true)+ " : $i)).\n");
        }
    }

    /** ***************************************************************
     */
    public static void transModalTHF(KB kb) {

        THF thf = new THF();
        Collection coll = Collections.EMPTY_LIST;
        Collection<Formula> result = new ArrayList<Formula>();
        String kbDir = KBmanager.getMgr().getPref("kbDir");
        String sep = File.separator;
        try {
            System.out.println("\n\nTHFnew.transModalTHF()");
            String filename = kbDir + sep + kb.name + ".thf";
            FileWriter fstream = new FileWriter(filename);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(Modals.getTHFHeader() + "\n");
            writeTypes(kb,out);
            for (Formula f : kb.formulaMap.values()) {
                if (debug) System.out.println("THFnew.transModalTHF(): " + f);
                if (!exclude(f,kb))
                    oneTrans(kb,f,out);
            }
            out.close();
            System.out.println("\n\nTHFnew.transModalTHF(): Result written to file " + filename);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** ***************************************************************
     */
    public static void test(KB kb) {

        debug = true;
        String fstr = "(=>\n" +
                "  (instance ?LAND1 LandArea)\n" +
                "  (exists (?LAND2)\n" +
                "    (and\n" +
                "      (part ?LAND1 ?LAND2)\n" +
                "      (or\n" +
                "        (instance ?LAND2 Continent)\n" +
                "        (instance ?LAND2 Island)))))\n";
        Formula f = new Formula(fstr);
        try {
            oneTrans(kb,f,null);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("THFnew");
        System.out.println("  options (with a leading '-'):");
        System.out.println("  m - THF translation with modals");
        System.out.println("  t - test");
        System.out.println("  h - show this help");
    }

    /** ***************************************************************
     */
    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("INFO in THFnew.main()");
        System.out.println("args:" + args.length + " : " + Arrays.toString(args));
        if (args == null) {
            System.out.println("no command given");
            showHelp();
        }
        else if (args != null && args.length > 0 && args[0].equals("-h"))
            showHelp();
        else {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            System.out.println("THFnew.main(): KB loaded");
            if (kb.errors.size() > 0) {
                System.out.println("Errors: " + kb.errors);
            }
            else if (args != null && args.length > 0 && args[0].equals("-t")) {
                System.out.println("THFnew.main(): translate to THF");
                test(kb);
            }
            else if (args != null && args.length > 0 && args[0].equals("-m")) {
                System.out.println("THFnew.main(): translate to THF with modals");
                transModalTHF(kb);
            }
            else
                showHelp();
        }
    }

}
