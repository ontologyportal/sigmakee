package com.articulate.sigma.trans;

import com.articulate.sigma.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by apease on 7/23/18.
 */
public class SUMOtoTFAform {

    public static KB kb;

    private static boolean debug = false;

    private static HashMap<String,HashSet<String>> varmap = null;

    // a map of relation signatures (where function returns are index 0)
    // modified from the original by the constraints of the axiom
    private static HashMap<String,ArrayList<String>> signatures = null;

    public static boolean initialized = false;

    public static FormulaPreprocessor fp = new FormulaPreprocessor();

    // constraints on numeric types
    public static HashMap<String,String> numericConstraints = new HashMap();

    // variable names of constraints on numeric types
    public static HashMap<String,String> numericVars = new HashMap();

    /** *************************************************************
     */
    public static boolean isComparisonOperator(String s) {

        int under = s.lastIndexOf("__");
        if (under == -1)
            return Formula.isComparisonOperator(s);
        if (Formula.isComparisonOperator(s.substring(0,under)))
            return true;
        return false;
    }

    /** *************************************************************
     */
    public static boolean isMathFunction(String s) {

        int under = s.lastIndexOf("__");
        if (under == -1)
            return Formula.isMathFunction(s);
        if (Formula.isMathFunction(s.substring(0,under)))
            return true;
        return false;
    }

    /** *************************************************************
     * Set the cached information of automatically generated functions
     * and relations needed to cover the polymorphic type signatures
     * of build-in TFF terms
     */
    public static void setNumericFunctionInfo() {

        //System.out.println("setNumericFunctionInfo()");
        if (kb.containsTerm("AdditionFn_IntegerFn")) // this routine has already been run or cached via serialization
            return;
        for (String s : Formula.COMPARISON_OPERATORS) {
            kb.kbCache.extendInstance(s,"Integer");
            kb.kbCache.extendInstance(s,"Real");
        }
        for (String s : Formula.MATH_FUNCTIONS) {
            kb.kbCache.extendInstance(s,"IntegerFn");
            kb.kbCache.extendInstance(s,"RealFn");
        }
    }

    /** *************************************************************
     * Recurse through the formula giving numeric and comparison
     * operators a __Integer or __Real suffix if they operate on
     * numbers.  TODO check the return types of any enclosed functions
     * since this only works now for literal numbers
     */
    public static Formula convertNumericFunctions(Formula f) {

        //System.out.println("convertNumericFunctions(): " + f);
        if (f == null)
            return f;
        if (f.atom()) {
            int ttype = f.theFormula.charAt(0);
            if (Character.isDigit(ttype))
                ttype = StreamTokenizer_s.TT_NUMBER;
            return f;
        }
        Formula car = f.carAsFormula();
        ArrayList<String> args = f.complexArgumentsToArrayList(1);
        if (Formula.isMathFunction(car.theFormula) ||
                (Formula.isComparisonOperator(car.theFormula) && !car.theFormula.equals("equals"))) {
            StringBuffer argsStr = new StringBuffer();
            boolean isInt = false;
            boolean isReal = false;
            for (String s : args) {
                if (StringUtil.isInteger(s))
                    isReal = true; // isInt = true; it could be a real that just doesn't have a decimal
                if (!isInt && StringUtil.isNumeric(s))
                    isReal = true;
                if (!isInt && !isReal) {
                    Formula sf = new Formula(s);
                    String type = kb.kbCache.getRange(sf.car());
                    if (type != null && (type.equals("Integer") || kb.isSubclass(type,"Integer")))
                        isInt = true;
                    if (type != null && (type.equals("RealNumber") || kb.isSubclass(type,"RealNumber")))
                        isReal = true;
                    argsStr.append(convertNumericFunctions(sf) + " ");
                }
                else
                    argsStr.append(s + " ");
            }
            argsStr.deleteCharAt(argsStr.length()-1);
            String suffix = "";
            if (isInt) {
                if (Formula.isMathFunction(car.theFormula))
                    suffix = "__IntegerFn";
                else
                    suffix = "__Integer";
            }
            if (isReal) {
                if (Formula.isMathFunction(car.theFormula))
                    suffix = "__RealFn";
                else
                    suffix = "__Real";
            }
            f.theFormula = "(" + car.theFormula + suffix + " " + argsStr.toString() + ")";
        }
        else {
            StringBuffer argsStr = new StringBuffer();
            if (args != null) {
                for (String s : args) {
                    Formula sf = new Formula(s);
                    argsStr.append(convertNumericFunctions(sf) + " ");
                }
                argsStr.deleteCharAt(argsStr.length() - 1);
                f.theFormula = "(" + car.theFormula + " " + argsStr.toString() + ")";
            }
        }
        return f;
    }

    /** *************************************************************
     */
    private static String processQuant(Formula f, Formula car, String op,
                                       ArrayList<String> args) {

        //if (debug) System.out.println("processQuant(): quantifier");
        if (args.size() < 2) {
            System.out.println("Error in processQuant(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        else {
            //if (debug) System.out.println("processQuant(): correct # of args");
            if (args.get(0) != null) {
                //if (debug) System.out.println("processQuant(): valid varlist: " + args.get(0));
                Formula varlist = new Formula(args.get(0));
                ArrayList<String> vars = varlist.argumentsToArrayList(0);
                //if (debug) System.out.println("processRecurse(): valid vars: " + vars);
                StringBuffer varStr = new StringBuffer();
                for (String v : vars) {
                    String oneVar = SUMOformulaToTPTPformula.translateWord(v,v.charAt(0),false);
                    if (varmap.keySet().contains(v) && !StringUtil.emptyString(varmap.get(v))) {
                        String type = kb.mostSpecificTerm(varmap.get(v));
                        oneVar = oneVar + ":" + SUMOKBtoTFAKB.translateSort(type);
                    }
                    varStr.append(oneVar + ", ");
                }
                //if (debug) System.out.println("processQuant(): valid vars: " + varStr);
                String opStr = " ! ";
                if (op.equals("exists"))
                    opStr = " ? ";
                //if (debug) System.out.println("processQuant(): quantified formula: " + args.get(1));
                return opStr + "[" + varStr.toString().substring(0,varStr.length()-2) + "] : (" +
                        processRecurse(new Formula(args.get(1))) + ")";
            }
            else {
                System.out.println("Error in processQuant(): null arguments to " + op + " in " + f);
                return "";
            }
        }
    }

    /** *************************************************************
     */
    private static String processLogOp(Formula f, Formula car,
                                        ArrayList<String> args) {
        String op = car.theFormula;
        //System.out.println("processRecurse(): op: " + op);
        //System.out.println("processRecurse(): args: " + args);
        if (op.equals("and")) {
            if (args.size() < 2) {
                System.out.println("Error in processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return processRecurse(new Formula(args.get(0))) + " & " +
                        processRecurse(new Formula(args.get(1)));
        }
        if (op.equals("=>")) {
            if (args.size() < 2) {
                System.out.println("Error in processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return processRecurse(new Formula(args.get(0))) + " => " +
                        processRecurse(new Formula(args.get(1)));
        }
        if (op.equals("<=>")) {
            if (args.size() < 2) {
                System.out.println("Error in processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return "(" + processRecurse(new Formula(args.get(0))) + " => " +
                        processRecurse(new Formula(args.get(1))) + ") & (" +
                        processRecurse(new Formula(args.get(1))) + " => " +
                        processRecurse(new Formula(args.get(0))) + ")";
        }
        if (op.equals("or")) {
            if (args.size() < 2) {
                System.out.println("Error in processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return processRecurse(new Formula(args.get(0))) + " | " +
                        processRecurse(new Formula(args.get(1)));
        }
        if (op.equals("not")) {
            if (args.size() != 1) {
                System.out.println("Error in processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return "~" + processRecurse(new Formula(args.get(0)));
        }
        if (op.equals("forall") || op.equals("exists"))
            return processQuant(f,car,op,args);
        System.out.println("Error in processLogOp(): bad logical operator " + op + " in " + f);
        return "";
    }

    /** *************************************************************
     */
    private static String processCompOp(Formula f, Formula car,
                                       ArrayList<String> args) {

        String op = car.theFormula;
        if (args.size() != 2) {
            System.out.println("Error in processCompOp(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        if (debug) System.out.println("processCompOp(): op: " + op);
        if (debug) System.out.println("processCompOp(): args: " + args);
        if (op.startsWith("equal")) {
            return processRecurse(new Formula(args.get(0))) + " = " +
                    processRecurse(new Formula(args.get(1)));
        }
        if (op.startsWith("greaterThanOrEqualTo"))
            return "$greatereq(" + processRecurse(new Formula(args.get(0))) + " ," +
                    processRecurse(new Formula(args.get(1))) + ")";
        if (op.startsWith("greaterThan"))
            return "$greater(" + processRecurse(new Formula(args.get(0))) + " ," +
                    processRecurse(new Formula(args.get(1))) + ")";
        if (op.startsWith("lessThanOrEqualTo"))
            return "$lesseq(" + processRecurse(new Formula(args.get(0))) + " ," +
                    processRecurse(new Formula(args.get(1))) + ")";
        if (op.startsWith("lessThan"))
            return "$less(" + processRecurse(new Formula(args.get(0))) + " ," +
                    processRecurse(new Formula(args.get(1))) + ")";

        System.out.println("Error in processCompOp(): bad comparison operator " + op + " in " + f);
        return "";
    }

    /** *************************************************************
     */
    private static String processMathOp(Formula f, Formula car,
                                        ArrayList<String> args) {

        String op = car.theFormula;
        if (args.size() != 2) {
            System.out.println("Error in processMathOp(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        //if (debug) System.out.println("processMathOp(): op: " + op);
        //if (debug) System.out.println("processMathOp(): args: " + args);
        if (op.startsWith("AdditionFn"))
            return "$sum(" + processRecurse(new Formula(args.get(0))) + " ," +
                    processRecurse(new Formula(args.get(1))) + ")";
        if (op.startsWith("SubtractionFn"))
            return "$difference(" + processRecurse(new Formula(args.get(0))) + " ," +
                    processRecurse(new Formula(args.get(1))) + ")";
        if (op.startsWith("MultiplicationFn"))
            return "$product(" + processRecurse(new Formula(args.get(0))) + " ," +
                    processRecurse(new Formula(args.get(1))) + ")";
        if (op.startsWith("DivisionFn"))
            return "$quotient_e(" + processRecurse(new Formula(args.get(0))) + " ," +
                    processRecurse(new Formula(args.get(1))) + ")";
        System.out.println("Error in processMathOp(): bad math operator " + op + " in " + f);
        return "";
    }

    /** *************************************************************
     */
    public static String processRecurse(Formula f) {

        if (debug) System.out.println("processRecurse(): " + f);
        if (debug) System.out.println("processRecurse(): varmap: " + varmap);
        if (f == null)
            return "";
        if (f.atom()) {
            int ttype = f.theFormula.charAt(0);
            if (Character.isDigit(ttype))
                ttype = StreamTokenizer_s.TT_NUMBER;
            return SUMOformulaToTPTPformula.translateWord(f.theFormula,ttype,false);
        }
        Formula car = f.carAsFormula();
        //System.out.println("processRecurse(): car: " + car);
        //System.out.println("processRecurse(): car: " + car.theFormula);
        ArrayList<String> args = f.complexArgumentsToArrayList(1);
        if (car.listP()) {
            System.out.println("Error in processRecurse(): formula " + f);
            return "";
        }
        if (Formula.isLogicalOperator(car.theFormula))
            return processLogOp(f,car,args);
        else if (isComparisonOperator(car.theFormula))
            return processCompOp(f,car,args);
        else if (isMathFunction(car.theFormula))
            return processMathOp(f,car,args);
        else {
            //if (debug) System.out.println("processRecurse(): not math or comparison op: " + car);
            StringBuffer argStr = new StringBuffer();
            for (String s : args)
                argStr.append(processRecurse(new Formula(s)) + ", ");
            String result = "s__" + car.theFormula + "(" + argStr.substring(0,argStr.length()-2) + ")";
            //if (debug) System.out.println("processRecurse(): result: " + result);
            return result;
        }
    }

    /** *************************************************************
     * result is a side effect on varmap
     */
    private static HashMap<String,HashSet<String>> cloneVarmap() {

        HashMap<String,HashSet<String>> newVarmap = new HashMap<>();
        for (String s : varmap.keySet()) {
            HashSet<String> newSet = new HashSet<>();
            newSet.addAll(varmap.get(s));
            newVarmap.put(s,newSet);
        }
        return newVarmap;
    }

    /** *************************************************************
     */
    private static String numberSuffix(String op, String t, String sigType) {

        String suffix = "";
        if (kb.isSubclass(t, sigType) && kb.isSubclass(t, "Number")) {
            if (kb.isSubclass(t, "RealNumber") || t.equals("RealNumber"))
                if (Formula.isMathFunction(op))
                    suffix = "__RealFn";
                else
                    suffix = "__Real";
            if (kb.isSubclass(t, "Integer") || t.equals("Integer"))
                if (Formula.isMathFunction(op))
                    suffix = "__IntegerFn";
                else
                    suffix = "__Integer";
        }
        return suffix;
    }

    /** *************************************************************
     * @param args is a list of arguments, starting with an empty first argument
     *             for the relation
     */
    private static String constrainOp(Formula f, String op, ArrayList<String> args) {

        if (debug) System.out.println("constrainOp(): op: " + op);
        if (debug) System.out.println("constrainOp(): f: " + f);
        if (debug) System.out.println("constrainOp(): args: " + args);
        String suffix = "";
        ArrayList<String> sig = kb.kbCache.getSignature(op);
        for (int i = 1; i < args.size(); i++) {
            String arg = args.get(i);
            String type = sig.get(i);
            if (Formula.listP(arg)) {
                String justArg = (new Formula(arg)).car();
                if (kb.isFunction(justArg) ||
                        (justArg.indexOf('_') != -1 && kb.isFunction(justArg.substring(0,justArg.indexOf('_'))))) {
                    String t = kb.kbCache.getRange(justArg);
                    suffix = numberSuffix(op,t,type);
                }
                if (Formula.isVariable(justArg)) {
                    String t = kb.mostSpecificTerm(varmap.get(justArg));
                    suffix = numberSuffix(op,t,type);
                }
                args.set(i,constrainFunctVarsRecurse(new Formula(arg)));
            }
        }
        if (op.indexOf("_") != -1)
            suffix = "";
        ArrayList<String> newargs = new ArrayList<>();
        newargs.addAll(args);
        newargs.remove(0);
        String result = "(" + op + suffix + " " + StringUtil.arrayListToSpacedString(newargs) + ")";
        //System.out.println("constrainOp(): result: " + result);
        return result;
    }

    /** *************************************************************
     */
    private static String constrainFunctVarsRecurse(Formula f) {

        if (debug) System.out.println("constrainFunctVarsRecurse(): " + f);
        if (f == null) return "";
        if (f.atom()) return f.theFormula;
        Formula car = f.carAsFormula();
        //System.out.println("processRecurse(): car: " + car);
        //System.out.println("processRecurse(): car: " + car.theFormula);
        ArrayList<String> args = f.complexArgumentsToArrayList(0);
        if (car.listP()) {
            System.out.println("Error in processRecurse(): formula " + f);
            return "";
        }
        String op = car.theFormula;
        ArrayList<String> sig = kb.kbCache.getSignature(op);
        if ((car.theFormula.equals(Formula.EQUAL)) || isComparisonOperator(car.theFormula) ||
                isMathFunction(car.theFormula))
            return constrainOp(f,op,args);
        else {
            StringBuffer resultString = new StringBuffer();
            resultString.append("(" + op);
            if (debug) System.out.println("processRecurse(): not math or comparison op: " + car);
            ArrayList<String> newargs = new ArrayList<>();
            newargs.addAll(args);
            newargs.remove(0);
            for (String s : newargs)
                resultString.append(" " + constrainFunctVarsRecurse(new Formula(s)));
            resultString.append(")");
            return resultString.toString();
        }
    }

    /** *************************************************************
     * Only constrain the element of the varmap if the new type is more specific
     * @result is the new varmap
     */
    private static void constrainTypeRestriction(HashMap<String,HashSet<String>> newvarmap) {

        for (String k : newvarmap.keySet()) {
            HashSet<String> newvartypes = newvarmap.get(k);
            String newt = kb.mostSpecificTerm(newvartypes);
            HashSet<String> oldvartypes = varmap.get(k);
            String oldt = kb.mostSpecificTerm(oldvartypes);
            if (StringUtil.emptyString(newt) && StringUtil.emptyString(oldt)) {
                System.out.println("Error in constrainTypeRestriction(): empty variables: " +
                        newt + " " + oldt);
                System.out.println("Error in constrainTypeRestriction(): newvarmap: " +
                        newvarmap);
                Thread.dumpStack();
                return;
            }
            if (StringUtil.emptyString(newt))
                varmap.put(k,oldvartypes);
            else if (StringUtil.emptyString(oldt))
                varmap.put(k,newvartypes);
            else if (kb.isSubclass(newt,oldt))
                varmap.put(k,newvartypes);
            else
                varmap.put(k,oldvartypes);
        }
    }

    /** *************************************************************
     * result is a side effect on varmap
     */
    private static void constrainFunctVars(Formula f) {

        if (debug) System.out.println("constrainFunctVars(): formula: " + f);
        HashMap<String,HashSet<String>> oldVarmap = null;
        do {
            oldVarmap = cloneVarmap();
            String newf = constrainFunctVarsRecurse(f);
            f.theFormula = newf;
            HashMap<String,HashSet<String>> types = fp.findAllTypeRestrictions(f, kb);
            if (debug) System.out.println("constrainFunctVars(): found types: " + types);
            constrainTypeRestriction(types);
            //System.out.println("constrainFunctVars(): new varmap: " + varmap);
            //System.out.println("constrainFunctVars(): old varmap: " + oldVarmap);
        } while (!varmap.equals(oldVarmap));
    }

    /** *************************************************************
     */
    public static String process(Formula f) {

        f.theFormula = modifyPostcond(f);
        f.theFormula = convertNumericFunctions(f).theFormula;
        HashMap<String,HashSet<String>> varDomainTypes = fp.computeVariableTypes(f, kb);
        if (debug) System.out.println("process: varDomainTypes " + varDomainTypes);
        // get variable types which are explicitly defined in formula
        HashMap<String,HashSet<String>> varExplicitTypes = fp.findExplicitTypesClassesInAntecedent(kb,f);
        if (debug) System.out.println("process: varExplicitTypes " + varExplicitTypes);
        varmap = fp.findTypeRestrictions(f, kb);
        if (debug) System.out.println("process(): varmap: " + varmap);
        constrainFunctVars(f);
        if (f != null && f.listP()) {
            ArrayList<String> UqVars = f.collectUnquantifiedVariables();
            if (debug) System.out.println("process(): unquant: " + UqVars);
            String result = processRecurse(f);
            if (debug) System.out.println("process(): result 1: " + result);
            StringBuffer qlist = new StringBuffer();
            for (String s : UqVars) {
                if (debug) System.out.println("process(): s: " + s);
                String t = "";
                String oneVar = SUMOformulaToTPTPformula.translateWord(s,s.charAt(0),false);
                if (varmap.keySet().contains(s) && !StringUtil.emptyString(varmap.get(s))) {
                    t = kb.mostSpecificTerm(varmap.get(s));
                    if (debug) System.out.println("process(): varmap.get(s): " + varmap.get(s));
                    if (debug) System.out.println("process(): t: " + t);
                    if (t != null)
                        qlist.append(oneVar + " : " + SUMOKBtoTFAKB.translateSort(t) + ",");
                }
            }
            if (qlist.length() > 1) {
                qlist.deleteCharAt(qlist.length() - 1);  // delete final comma
                result = "! [" + qlist + "] : (" + result + ")";
            }
            if (debug) System.out.println("process(): result 2: " + result);
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
     * if the precondition of a rule is of the form (instance ?X term)
     * @return the name of the variable in the instance statement
     * (without the leading question mark)
     */
    private static String matchingPrecond(Formula f, String term) {

        String ant = FormulaUtil.antecedent(f);
        //System.out.println("matchingPrecond(): term: " + term);
        //System.out.println("matchingPrecond(): ant: " + ant);
        if (ant == null)
            return null;
        Pattern p = Pattern.compile("\\(instance \\?(\\w+) " + term + "\\)");
        Matcher m = p.matcher(ant);
        if (m.find()) {
            //System.out.println("matchingPrecond(): matches! ");
            String var = m.group(1);
            return var;
        }
        return null;
    }

    /** *************************************************************
     * if all or part of a consequent of a rule is of the form (instance ?X term)
     * @return the name of the type in the instance statement
     */
    private static String matchingPostcondTerm(Formula f) {

        String cons = FormulaUtil.consequent(f);
        //System.out.println("matchingPostcondTerm(): const: " + cons);
        if (cons == null)
            return null;
        Pattern p = Pattern.compile("\\(instance \\?\\w+ (\\w+)\\)");
        Matcher m = p.matcher(cons);
        if (m.find()) {
            //System.out.println("matchingPostcondTerm(): matches! ");
            String type = m.group(1);
            return type;
        }
        return null;
    }

    /** *************************************************************
     * if all or part of a consequent of a rule is of the form (instance ?X term)
     * @return the name of the type in the instance statement
     */
    private static String matchingPostcond(Formula f) {

        HashSet<String> intChildren = kb.kbCache.getChildClasses("Integer");
        //System.out.println("buildNumericConstraints(): int: " + intChildren);
        HashSet<String> realChildren = kb.kbCache.getChildClasses("RealNumber");
        if (realChildren.contains("Integer"))
            realChildren.remove("Integer");
        //System.out.println("buildNumericConstraints(): real: " + realChildren);
        String type = matchingPostcondTerm(f);
        if (intChildren.contains(type))
            return type;
        else if (realChildren.contains(type))
            return type;
        else return null;
    }

    /** *************************************************************
     * remove statements of the form (instance ?X term) if 'term' is
     * Integer or RealNumber and ?X is already of that type in the
     * quantifier list for the formula
     * @return the modified formula
     * TODO: remove the statements but also remove enclosing conjunctions
     * and disjunctions if removal results in a single enclosed literal
     */
    protected static String modifyPrecond(Formula f) {

        if (f == null)
            return f.theFormula;
        String type = "Integer";
        Pattern p = Pattern.compile("\\(instance \\?(\\w+) " + type + "\\)");
        Matcher m = p.matcher(f.theFormula);
        if (m.find()) {
            String var = m.group(1);
        }

        type = "RealNumber";
        p = Pattern.compile("\\(instance \\?(\\w+) " + type + "\\)");
        m = p.matcher(f.theFormula);
        if (m.find()) {
            String var = m.group(1);
        }

        return f.theFormula; // do nothing until this is fully implemented
    }

    /** *************************************************************
     * if all or part of a consequent of a rule is of the form (instance ?X term)
     * @return the name of the type in the instance statement
     */
    protected static String modifyPostcond(Formula f) {

        String type = matchingPostcond(f);
        if (type == null)
            return f.theFormula;
        Pattern p = Pattern.compile("\\(instance \\?(\\w+) " + type + "\\)");
        Matcher m = p.matcher(f.theFormula);
        if (m.find()) {
            //if (debug) System.out.println("matchingPostcondTerm(): matches! ");
            String var = m.group(1);
            String toReplace = "(instance ?" + var + " " + type + ")";
            String cons = numericConstraints.get(type);
            String origVar = numericVars.get(type);
            String newCons = cons.replace("?" + origVar,"?" + var);
            return f.theFormula.replace(toReplace,newCons);
        }
        else
            return f.theFormula;
    }

    /** *************************************************************
     * Since SUMO has subtypes of numbers but TFF doesn't allow
     * subtypes, we need to capture all the rules that say things
     * like non negative integers are greater than 0 so they
     * can be added to axioms with NonNegativeInteger, replacing that
     * class with $int but adding the constraint that it must be
     * greater than 0
     */
    private static void buildNumericConstraints() {

        HashSet<String> intChildren = kb.kbCache.getChildClasses("Integer");
        //System.out.println("buildNumericConstraints(): int: " + intChildren);
        HashSet<String> realChildren = kb.kbCache.getChildClasses("RealNumber");
        if (realChildren.contains("Integer"))
            realChildren.remove("Integer");
        //System.out.println("buildNumericConstraints(): real: " + realChildren);
        //HashSet<Formula> intForms = new HashSet<>();
        for (String t : intChildren) {
            //System.out.println("buildNumericConstraints(): t: " + t);
            ArrayList<Formula> intFormsTemp = kb.ask("ant",0,t);
            if (intFormsTemp != null) {
                for (Formula f : intFormsTemp) {
                    String var = matchingPrecond(f,t);
                    if (var != null) {
                        numericConstraints.put(t, FormulaUtil.consequent(f));
                        numericVars.put(t,var);
                    }
                }
            }
        }
        //HashSet<Formula> realForms = new HashSet<>();
        for (String t : realChildren) {
            //System.out.println("buildNumericConstraints(): t: " + t);
            ArrayList<Formula> realFormsTemp = kb.ask("ant", 0, t);
            if (realFormsTemp != null) {
                for (Formula f : realFormsTemp) {
                    String var = matchingPrecond(f,t);
                    if (var != null) {
                        numericConstraints.put(t, FormulaUtil.consequent(f));
                        numericVars.put(t,var);
                    }
                }
            }
        }
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
        buildNumericConstraints();
        initialized = true;
    }

    /** *************************************************************
     */
    public static void test1() {

        Formula f = new Formula("(equal ?X (AdditionFn__IntegerFn 1 2))");
        System.out.println("SUMOtoTFAform.test1(): " + processRecurse(f));
        f = new Formula("(equal ?X (SubtractionFn__IntegerFn 2 1))");
        System.out.println("SUMOtoTFAform.test1(): " + processRecurse(f));
    }

    /** *************************************************************
     */
    public static void test2() {

        Formula f = new Formula("(=> (and (equal (AbsoluteValueFn ?NUMBER1) ?NUMBER2) " +
                "(instance ?NUMBER1 RealNumber) (instance ?NUMBER2 RealNumber)) " +
                "(or (and (instance ?NUMBER1 NonnegativeRealNumber) (equal ?NUMBER1 ?NUMBER2)) " +
                "(and (instance ?NUMBER1 NegativeRealNumber) (equal ?NUMBER2 (SubtractionFn 0 ?NUMBER1)))))");
        System.out.println("SUMOtoTFAform.test2(): " + process(f));
    }

    /** *************************************************************
     */
    public static void test3() {

        Formula f = new Formula("(<=> (equal (RemainderFn ?NUMBER1 ?NUMBER2) ?NUMBER) " +
                "(equal (AdditionFn (MultiplicationFn (FloorFn (DivisionFn ?NUMBER1 ?NUMBER2)) ?NUMBER2) ?NUMBER) ?NUMBER1))");
        System.out.println("SUMOtoTFAform.test3(): " + process(f));
    }

    /** *************************************************************
     */
    public static void test4() {

        Formula f = new Formula("(<=> (greaterThanOrEqualTo ?NUMBER1 ?NUMBER2) (or (equal ?NUMBER1 ?NUMBER2) (greaterThan ?NUMBER1 ?NUMBER2)))");
        System.out.println("SUMOtoTFAform.test4(): " + process(f));
    }

    /** *************************************************************
     */
    public static void test5() {

        Formula f = new Formula("(=>\n" +
                "(measure ?QUAKE\n" +
                "(MeasureFn ?VALUE RichterMagnitude))\n" +
                "(instance ?VALUE PositiveRealNumber))");
        System.out.println("SUMOtoTFAform.test5(): " + modifyPostcond(f));
    }

    /** *************************************************************
     */
    public static void test6() {

        Formula f = new Formula("(<=> " +
                "(equal (RemainderFn ?NUMBER1 ?NUMBER2) ?NUMBER) " +
                "(equal (AdditionFn (MultiplicationFn (FloorFn (DivisionFn ?NUMBER1 ?NUMBER2)) ?NUMBER2) ?NUMBER) ?NUMBER1))");
        System.out.println("SUMOtoTFAform.test6(): " + process(f));
        System.out.println("expect: ");
        System.out.println("tff(kb_SUMO_73,axiom,(! [V__NUMBER1 : $int,V__NUMBER2 : $int,V__NUMBER : $int] : " +
                "((s__RemainderFn(V__NUMBER1, V__NUMBER2) = V__NUMBER " +
                "=> $sum($product(s__FloorFn($quotient_e(V__NUMBER1 ,V__NUMBER2)) ,V__NUMBER2) ,V__NUMBER) = " +
                "V__NUMBER1) & " +
                "($sum($product(s__FloorFn($quotient_e(V__NUMBER1 ,V__NUMBER2)) ,V__NUMBER2) ,V__NUMBER) = " +
                "V__NUMBER1 => s__RemainderFn(V__NUMBER1, V__NUMBER2) = V__NUMBER)))).");
    }

    /** *************************************************************
     */
    public static void test7() {

        Formula f = new Formula("(<=> (and (equal (AbsoluteValueFn ?NUMBER1) ?NUMBER2) " +
                "(instance ?NUMBER1 RealNumber) (instance ?NUMBER2 RealNumber)) " +
                "(or (and (instance ?NUMBER1 NonnegativeRealNumber) (equal ?NUMBER1 ?NUMBER2)) " +
                "(and (instance ?NUMBER1 NegativeRealNumber) (equal ?NUMBER2 (SubtractionFn 0 ?NUMBER1)))))");
        System.out.println("SUMOtoTFAform.test7(): " + process(f));
        System.out.println("test7() expected: tff(kb_SUMO_1,axiom,(! [V__NUMBER1 : $int,V__NUMBER2 : $int] : " +
                "((s__AbsoluteValueFn__Integer(V__NUMBER1) = V__NUMBER2 => s__SignumFn__Integer(V__NUMBER1) = 1 " +
                "| s__SignumFn__Integer(V__NUMBER1) = 0 & V__NUMBER1 = V__NUMBER2 | $less(V__NUMBER1, 0) " +
                "& V__NUMBER2 = $difference(0 ,V__NUMBER1)) & (s__SignumFn__Integer(V__NUMBER1) = 1 | " +
                "s__SignumFn__Integer(V__NUMBER1) = 0 & V__NUMBER1 = V__NUMBER2 | $greater(V__NUMBER1, 0) & " +
                "V__NUMBER2 = $difference(0 ,V__NUMBER1) => s__AbsoluteValueFn__Integer(V__NUMBER1) = " +
                "V__NUMBER2 & $greater(V__NUMBER1, 0))))).");
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        //debug = true;
        initOnce();
        setNumericFunctionInfo();
        System.out.println(numericConstraints);
        System.out.println(numericVars);
        /*
        HashSet<String> realChildren = kb.kbCache.getChildClasses("RealNumber");
        System.out.println("main(): children of RealNumber: " + realChildren);

        realChildren = kb.kbCache.getChildClasses("PositiveRealNumber");
        System.out.println("main(): children of PositiveRealNumber: " + realChildren);

        realChildren = kb.kbCache.getChildClasses("NonnegativeRealNumber");
        System.out.println("main(): children of NonnegativeRealNumber: " + realChildren);
        */
        test7();
    }
}
