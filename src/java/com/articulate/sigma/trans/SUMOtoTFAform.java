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
        if (Formula.isComparisonOperator(s.substring(0,under-1)))
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

        if (kb.containsTerm("AdditionFn_IntegerFn")) // this routine has already been run or cached via serialization
            return;
        for (String s : Formula.COMPARISON_OPERATORS) {
            String relI = s + "__Integer";
            String relR = s + "__Real";
            if (!s.equals(Formula.EQUAL)) {
                kb.kbCache.extendInstance(s,"__Integer");
                kb.kbCache.relations.add(relI);
                kb.kbCache.valences.put(relI,2);
                kb.terms.add(relI);
                kb.kbCache.extendInstance(s,"__Real");
                kb.kbCache.relations.add(relR);
                kb.kbCache.valences.put(relR,2);
                kb.terms.add(relR);
                HashSet<String> p = kb.immediateParents(s);
                ArrayList<String> domainsI = new ArrayList<String>();
                domainsI.add("");
                domainsI.add("Integer");
                domainsI.add("Integer");
                kb.kbCache.signatures.put(relI,domainsI);
                ArrayList<String> domainsR = new ArrayList<String>();
                domainsR.add("");
                domainsR.add("RealNumber");
                domainsR.add("RealNumber");
                kb.kbCache.signatures.put(relR,domainsR);
            }
        }
        for (String s : Formula.MATH_FUNCTIONS) {
            HashSet<String> p = kb.immediateParents(s);
            kb.kbCache.extendInstance(s,"__IntegerFn");
            kb.kbCache.extendInstance(s,"__RealFn");
            String relI = s + "__IntegerFn";
            kb.kbCache.relations.add(relI);
            kb.kbCache.valences.put(relI,2);
            kb.terms.add(relI);
            String relR = s + "__RealFn";
            kb.kbCache.relations.add(relR);
            kb.kbCache.valences.put(relR,2);
            kb.terms.add(relR);
            ArrayList<String> domainsI = new ArrayList<String>();
            domainsI.add("Integer");
            domainsI.add("Integer");
            domainsI.add("Integer");
            kb.kbCache.signatures.put(relI,domainsI);
            ArrayList<String> domainsR = new ArrayList<String>();
            domainsR.add("RealNumber");
            domainsR.add("RealNumber");
            domainsR.add("RealNumber");
            kb.kbCache.signatures.put(relR,domainsR);
        }
    }

    /** *************************************************************
     * Recurse through the formula giving numeric and comparison
     * operators a __Integer or __Real suffix if they operate on
     * numbers.  TODO check the return types of any enclosed functions
     * since this only works now for literal numbers
     */
    public static Formula convertNumericFunctions(Formula f) {

        System.out.println("convertNumericFunctions(): " + f);
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
                    isInt = true;
                if (!isInt && StringUtil.isNumeric(s))
                    isReal = true;
                if (!isInt && !isReal) {
                    Formula sf = new Formula(s);
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
        System.out.println("processRecurse(): car: " + car);
        System.out.println("processRecurse(): car: " + car.theFormula);
        ArrayList<String> args = f.complexArgumentsToArrayList(1);
        if (car.listP()) {
            System.out.println("Error in processRecurse(): formula " + f);
            return "";
        }
        if (Formula.isLogicalOperator(car.theFormula)) {
            String op = car.theFormula;
            System.out.println("processRecurse(): op: " + op);
            System.out.println("processRecurse(): args: " + args);

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
            if (op.equals("<=>")) {
                if (args.size() < 2) {
                    System.out.println("Error in processRecurse(): wrong number of arguments to " + op + " in " + f);
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
                    System.out.println("Error in processRecurse(): wrong number of arguments to " + op + " in " + f);
                    return "";
                }
                else
                    return processRecurse(new Formula(args.get(0))) + " | " +
                            processRecurse(new Formula(args.get(1)));
            }
            if (op.equals("not")) {
                if (args.size() != 1) {
                    System.out.println("Error in processRecurse(): wrong number of arguments to " + op + " in " + f);
                    return "";
                }
                else
                    return "~" + processRecurse(new Formula(args.get(0)));
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
                            if (varmap.keySet().contains(v) && !StringUtil.emptyString(varmap.get(v))) {
                                String type = kb.mostSpecificTerm(varmap.get(v));
                                oneVar = oneVar + ":" + SUMOKBtoTFAKB.translateSort(type);
                            }
                            varStr.append(oneVar + ", ");
                        }
                        System.out.println("processRecurse(): valid vars: " + varStr);
                        String opStr = " ! ";
                        if (op.equals("exists"))
                            opStr = " ? ";
                        System.out.println("processRecurse(): quantified formula: " + args.get(1));
                        return opStr + "[" + varStr.toString().substring(0,varStr.length()-2) + "] : (" +
                                processRecurse(new Formula(args.get(1))) + ")";
                    }
                }
            }
        }
        else if (isComparisonOperator(car.theFormula)) {
            String op = car.theFormula;
            if (args.size() != 2) {
                System.out.println("Error in processRecurse(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            System.out.println("processRecurse(): op: " + op);
            System.out.println("processRecurse(): args: " + args);
            if (op.equals("equal")) {
                    return processRecurse(new Formula(args.get(0))) + " = " +
                            processRecurse(new Formula(args.get(1)));
            }
            if (op.equals("greaterThan"))
                return "$greater(" + processRecurse(new Formula(args.get(0))) + " ," +
                        processRecurse(new Formula(args.get(1))) + ")";
            if (op.equals("greaterThanOrEqualTo"))
                return "$greatereq(" + processRecurse(new Formula(args.get(0))) + " ," +
                        processRecurse(new Formula(args.get(1))) + ")";
            if (op.equals("lessThan"))
                return "$less(" + processRecurse(new Formula(args.get(0))) + " ," +
                        processRecurse(new Formula(args.get(1))) + ")";
            if (op.equals("lessThanOrEqualTo"))
                return "$lesseq(" + processRecurse(new Formula(args.get(0))) + " ," +
                        processRecurse(new Formula(args.get(1))) + ")";
        }
        else if (isMathFunction(car.theFormula)) {
            String op = car.theFormula;
            if (args.size() != 2) {
                System.out.println("Error in processRecurse(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            System.out.println("processRecurse(): op: " + op);
            System.out.println("processRecurse(): args: " + args);
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
        }
        else {
            System.out.println("processRecurse(): not math or comparison op: " + car);
            StringBuffer argStr = new StringBuffer();
            for (String s : args)
                argStr.append(processRecurse(new Formula(s)) + ", ");
            String result = "s__" + car.theFormula + "(" + argStr.substring(0,argStr.length()-2) + ")";
            System.out.println("processRecurse(): result: " + result);
            return result;
        }
        return "";
    }

    /** *************************************************************
     */
    public static String process(Formula f) {

        f.theFormula = convertNumericFunctions(f).theFormula;
        HashMap<String,HashSet<String>> varDomainTypes = fp.computeVariableTypes(f, kb);
        if (debug) System.out.println("process: varDomainTypes " + varDomainTypes);
        // get variable types which are explicitly defined in formula
        HashMap<String,HashSet<String>> varExplicitTypes = fp.findExplicitTypesClassesInAntecedent(kb,f);
        if (debug) System.out.println("process: varExplicitTypes " + varExplicitTypes);
        varmap = fp.findTypeRestrictions(f, kb);
        System.out.println("process(): varmap: " + varmap);
        if (f != null && f.listP()) {
            ArrayList<String> UqVars = f.collectUnquantifiedVariables();
            System.out.println("process(): unquant: " + UqVars);
            String result = processRecurse(f);
            System.out.println("process(): result 1: " + result);
            StringBuffer qlist = new StringBuffer();
            for (String s : UqVars) {
                System.out.println("process(): s: " + s);
                String t = "";
                String oneVar = SUMOformulaToTPTPformula.translateWord(s,s.charAt(0),false);
                if (varmap.keySet().contains(s) && !StringUtil.emptyString(varmap.get(s))) {
                    t = kb.mostSpecificTerm(varmap.get(s));
                    if (t != null)
                        qlist.append(oneVar + " : " + SUMOKBtoTFAKB.translateSort(t) + ",");
                }
            }
            if (qlist.length() > 1) {
                qlist.deleteCharAt(qlist.length() - 1);  // delete final comma
                result = "! [" + qlist + "] : (" + result + ")";
            }
            System.out.println("process(): result 2: " + result);
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
     */
    private static String matchingPrecond(Formula f, String term) {

        String ant = FormulaUtil.antecedent(f);
        System.out.println("matchingPrecond(): term: " + term);
        System.out.println("matchingPrecond(): ant: " + ant);
        if (ant == null)
            return null;
        Pattern p = Pattern.compile("\\(instance \\?(\\w+) " + term + "\\)");
        Matcher m = p.matcher(ant);
        if (m.find()) {
            System.out.println("matchingPrecond(): matches! ");
            String var = m.group(1);
            return var;
        }
        return null;
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
        System.out.println("buildNumericConstraints(): int: " + intChildren);
        HashSet<String> realChildren = kb.kbCache.getChildClasses("RealNumber");
        if (realChildren.contains("Integer"))
            realChildren.remove("Integer");
        System.out.println("buildNumericConstraints(): real: " + realChildren);
        //HashSet<Formula> intForms = new HashSet<>();
        for (String t : intChildren) {
            System.out.println("buildNumericConstraints(): t: " + t);
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
            System.out.println("buildNumericConstraints(): t: " + t);
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
        System.out.println("SUMOtoTFAform.test3(): " + process(f));
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        //debug = true;
        initOnce();
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
        test4();
    }
}
