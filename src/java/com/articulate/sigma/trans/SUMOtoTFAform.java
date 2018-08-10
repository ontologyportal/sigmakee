package com.articulate.sigma.trans;

import com.articulate.sigma.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by apease on 7/23/18.
 */
public class SUMOtoTFAform {

    public static KB kb;

    private static boolean debug = false;

    private static HashMap<String,HashSet<String>> varmap = null;

    public static boolean initialized = false;

    public static FormulaPreprocessor fp = new FormulaPreprocessor();

    /** *************************************************************
     */
    public static String processRecurse(Formula f) {

        System.out.println("processRecurse(): " + f);
        if (f == null)
            return "";
        if (f.atom()) {
            int ttype = f.theFormula.charAt(0);
            if (Character.isDigit(ttype))
                ttype = StreamTokenizer_s.TT_NUMBER;
            return SUMOformulaToTPTPformula.translateWord(f.theFormula,ttype,false);
        }
        Formula car = f.carAsFormula();
        ArrayList<String> args = f.complexArgumentsToArrayList(1);
        if (car.listP() || car.listP()) {
            System.out.println("Error in processRecurse(): formula " + f);
            return "";
        }
        if (Formula.isLogicalOperator(car.theFormula)) {
            String op = car.theFormula;
            System.out.println("processRecurse(): op: " + op);
            System.out.println("processRecurse(): args: " + args);
            if (op.equals("equals")) {
                if (args.size() != 2) {
                    System.out.println("Error in processRecurse(): wrong number of arguments to " + op + " in " + f);
                    return "";
                }
                else
                    return processRecurse(new Formula(args.get(0))) + " = " +
                            processRecurse(new Formula(args.get(1)));
            }
            if (op.equals("and")) {
                if (args.size() < 2) {
                    System.out.println("Error in processRecurse(): wrong number of arguments to " + op + " in " + f);
                    return "";
                }
                else
                    return processRecurse(new Formula(args.get(0))) + " & " +
                            processRecurse(new Formula(args.get(1)));
            }
            if (op.equals("=>")) {
                if (args.size() < 2) {
                    System.out.println("Error in processRecurse(): wrong number of arguments to " + op + " in " + f);
                    return "";
                }
                else
                    return processRecurse(new Formula(args.get(0))) + " => " +
                            processRecurse(new Formula(args.get(1)));
            }
            if (op.equals("or")) {
                if (args.size() < 2) {
                    System.out.println("Error in processRecurse(): wrong number of arguments to " + op + " in " + f);
                    return "";
                }
                else
                    return processRecurse(new Formula(args.get(0))) + " | " +
                            processRecurse(new Formula(args.get(1)));
            }
            if (op.equals("forall") || op.equals("exists")) {
                System.out.println("processRecurse(): quantifier");
                if (args.size() < 2) {
                    System.out.println("Error in processRecurse(): wrong number of arguments to " + op + " in " + f);
                    return "";
                }
                else {
                    System.out.println("processRecurse(): correct # of args");
                    if (args.get(0) != null) {
                        System.out.println("processRecurse(): valid varlist: " + args.get(0));
                        Formula varlist = new Formula(args.get(0));
                        ArrayList<String> vars = varlist.argumentsToArrayList(0);
                        System.out.println("processRecurse(): valid vars: " + vars);
                        StringBuffer varStr = new StringBuffer();
                        for (String v : vars) {
                            String oneVar = SUMOformulaToTPTPformula.translateWord(v,v.charAt(0),false);
                            if (varmap.keySet().contains(v) && !StringUtil.emptyString(varmap.get(v)))
                                oneVar = oneVar + ":" + kb.mostSpecificTerm(varmap.get(v));
                            varStr.append(oneVar + ", ");
                        }
                        System.out.println("processRecurse(): valid vars: " + varStr);
                        String opStr = " ! ";
                        if (op.equals("exists"))
                            opStr = " ? ";
                        System.out.println("processRecurse(): quantified formula: " + args.get(1));
                        return opStr + "[" + varStr.toString().substring(0,varStr.length()-2) + "] : " +
                                processRecurse(new Formula(args.get(1)));
                    }
                }
            }
        }
        else {
            StringBuffer argStr = new StringBuffer();
            for (String s : args)
                argStr.append(processRecurse(new Formula(s)) + ", ");
            return("s__" + car.theFormula + "(" + argStr.substring(0,argStr.length()-2) + ")");
        }
        return "";
    }

    /** *************************************************************
     */
    public static String process(Formula f) {

        HashMap<String,HashSet<String>> varDomainTypes = fp.computeVariableTypes(f, kb);
        if (debug) System.out.println("process: varDomainTypes " + varDomainTypes);
        // get variable types which are explicitly defined in formula
        HashMap<String,HashSet<String>> varExplicitTypes = fp.findExplicitTypesClassesInAntecedent(kb,f);
        if (debug) System.out.println("process: varExplicitTypes " + varExplicitTypes);
        varmap = fp.findTypeRestrictions(f, kb);
        System.out.println("processRecurse(): varmap: " + varmap);
        if (f != null && f.listP()) {
            ArrayList<String> UqVars = f.collectUnquantifiedVariables();
            String result = processRecurse(f);
            StringBuffer qlist = new StringBuffer();
            for (String s : UqVars) {
                String t = "";
                String oneVar = SUMOformulaToTPTPformula.translateWord(s,s.charAt(0),false);
                if (varmap.keySet().contains(s) && !StringUtil.emptyString(varmap.get(s))) {
                    t = kb.mostSpecificTerm(varmap.get(s));
                    if (t != null)
                        qlist.append(oneVar + " : " + t + ",");
                }
            }
            if (qlist.length() > 1) {
                qlist.deleteCharAt(qlist.length() - 1);  // delete final comma
                result = "! [" + qlist + "] : (" + result + ")";
            }
            return result;
        }
        return ("");
    }

    /** *************************************************************
     */
    public static String process(String s) {

        if (StringUtil.emptyString(s))
            return "";
        Formula f = new Formula(s);
        return process(f);
    }

    /** *************************************************************
     */
    public static Collection<String> processList(Collection<Formula> l) {

        ArrayList<String> result = new ArrayList<>();
        for (Formula f : l)
            result.add(process(f));
        return result;
    }

    /** *************************************************************
     */
    public static void initOnce() {

        if (initialized)
            return;
        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB("SUMO");
        fp = new FormulaPreprocessor();
        fp.addTypes = false;
        initialized = true;
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        //debug = true;
        initOnce();
        Formula f = new Formula("(instance Foo Bar)");
        System.out.println("SUMOtoTFAform.main(): " + f);
        System.out.println("SUMOtoTFAform.main(): result: " + process(f));
    }
}
