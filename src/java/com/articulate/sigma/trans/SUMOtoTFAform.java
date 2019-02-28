package com.articulate.sigma.trans;

import com.articulate.sigma.*;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
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
     */
    public static String withoutSuffix(String s) {

        if (StringUtil.emptyString(s))
            return s;
        int under = s.indexOf("__");
        if (under == -1)
            return s;
        return s.substring(0,under);
    }

    /** *************************************************************
     * Set the cached information of automatically generated functions
     * and relations needed to cover the polymorphic type signatures
     * of build-in TFF terms
     */
    public static void setNumericFunctionInfo() {

        //System.out.println("setNumericFunctionInfo()");
        if (kb.containsTerm("AdditionFn__1In2InFn")) // this routine has already been run or cached via serialization
            return;
        for (String s : Formula.COMPARISON_OPERATORS) {
            kb.kbCache.extendInstance(s,"1In2In");
            kb.kbCache.extendInstance(s,"1Re2Re");
            kb.kbCache.extendInstance(s,"1Ra2Ra");
        }
        for (String s : Formula.MATH_FUNCTIONS) {
            kb.kbCache.extendInstance(s,"0In1In2InFn");
            kb.kbCache.extendInstance(s,"0Re1Re2ReFn");
            kb.kbCache.extendInstance(s,"0Ra1Ra2RaFn");
        }
    }

    /** *************************************************************
     * Fill the indicated elements with the empty string, starting at start and ending
     * at end-1
     */
    public static void fill (ArrayList<String> ar, int start, int end) {

        if (ar.size() <= end)
            for (int i = start; i < end; i++)
                ar.add("");
        else
            for (int i = start; i < end; i++)
                ar.set(i,"");
    }

    /** *************************************************************
     * If there's no such element index, fill the previous elements
     * with the empty string
     */
    public static void safeSet (ArrayList<String> ar, int index, String val) {

        if (index > ar.size()-1)
            fill(ar,ar.size(),index+1);
        ar.set(index,val);
    }

    /** *************************************************************
     * Extract modifications to the relation signature from annotations
     * embedded in the suffixes to its name.  Note that the first argument
     * to a relation is number 1, so getting the 0th argument of the
     * returned ArrayList would give an empty string result for a relation.
     */
    public static ArrayList<String> relationExtractSig(String rel) {

        if (StringUtil.emptyString(rel))
            return new ArrayList<String>();
        if (debug) System.out.println("SUMOtoTFAform.relationExtractSig(): " + rel);
        ArrayList<String> sig = new ArrayList();
        String patternString = "(\\d)(In|Re|Ra)";

        int under = rel.indexOf("__");
        if (under == -1)
            return sig;
        String text = rel.substring(under + 2, rel.length());
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
            String type = matcher.group(2);
            if (type.equals("In"))
                type = "Integer";
            else if (type.equals("Re"))
                type = "RealNumber";
            else if (type.equals("Ra"))
                type = "RationalNumber";
            else
                continue;
            int arg = Integer.parseInt(matcher.group(1));
            if (arg > sig.size()-1)
                fill(sig,sig.size(),arg+1);
            if (debug) System.out.println("SUMOtoTFAform.relationExtractSig(): matches: " +
                    arg + ", " + matcher.group(2));
            safeSet(sig,arg,type);
        }
        if (debug) System.out.println("SUMOtoTFAform.relationExtractSig(): for rel: " +
                rel + " set sig " + sig);
        return sig;
    }

    /** *************************************************************
     * Extract modifications to the relation signature from annotations
     * embedded in the suffixes to its name.  Note that the first argument
     * to a relation is number 1, so getting the 0th argument of the
     * returned ArrayList would give an empty string result for a relation.
     */
    public static ArrayList<String> relationExtractUpdateSig(String rel) {

        if (StringUtil.emptyString(rel))
            return new ArrayList<String>();
        if (debug) System.out.println("SUMOtoTFAform.relationExtractUpdateSig(): " + rel);
        ArrayList<String> origsig = kb.kbCache.getSignature(withoutSuffix(rel));
        if (debug) System.out.println("SUMOtoTFAform.relationExtractUpdateSig(): origsig: " + origsig);
        if (origsig == null) {
            System.out.println("Error in SUMOtoTFAform.relationExtractUpdateSig(): null signature for " + rel);
            return null;
        }
        ArrayList<String> sig = new ArrayList(origsig);
        ArrayList<String> newsig = relationExtractSig(rel);
        for (int i = 0; i < origsig.size(); i++)
            if (i < newsig.size() && !StringUtil.emptyString(newsig.get(i)))
                sig.set(i,newsig.get(i));
        if (debug) System.out.println("SUMOtoTFAform.relationExtractSig(): for rel: " +
                rel + " set sig " + sig);
        return sig;
    }

    /** *************************************************************
     * Embed the type signature for TFF numeric types into the name of
     * the relation.  This is used for when a relation's signature is
     * modified from its authored original
     */
    private static String relationEmbedSig(String rel, ArrayList<String> sig) {

        if (rel.equals("instance"))
            return "";
        String[] a = new String[] {"In","Re","Ra"};
        Collection typeChars = Arrays.asList(a);
        StringBuffer sb = new StringBuffer();
        sb.append(rel + "__");
        if (kb.isFunction(rel) && typeChars.contains(sig.get(0).substring(0,2))) {
            sb.append("0" + sig.get(0).substring(0, 2));
        }
        for (int i = 1; i < sig.size(); i++) {
            if (typeChars.contains(sig.get(i).substring(0,2)))
            sb.append(i + sig.get(i).substring(0, 2));
        }
        return sb.toString();
    }

    /** *************************************************************
     * Recurse through the formula giving numeric and comparison
     * operators a suffix if they operate on
     * numbers.
     */
    public static Formula convertNumericFunctions(Formula f) {

        if (debug) System.out.println("SUMOtoTFAform.convertNumericFunctions(): " + f);
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
        if (isMathFunction(car.theFormula) ||
                (isComparisonOperator(car.theFormula) && !car.theFormula.equals("equal"))) {
            StringBuffer argsStr = new StringBuffer();
            boolean isInt = false;
            boolean isReal = false;
            boolean isRat = false;
            for (String s : args) {
                if (debug) System.out.println("SUMOtoTFAform.convertNumericFunctions(): arg: " + s);
                if (Formula.isVariable(s)) {
                    if (varmap.containsKey(s)) {
                        Set<String> types = varmap.get(s);
                        String type = kb.mostSpecificTerm(types);
                        if (debug) System.out.println("SUMOtoTFAform.convertNumericFunctions(): type: " + type);
                        if (type != null && (type.equals("Integer") || kb.isSubclass(type,"Integer")))
                            isInt = true;
                        if (type != null && (type.equals("RationalNumber") || kb.isSubclass(type,"RationalNumber")))
                            isRat = true;
                        if (type != null && (type.equals("RealNumber") || kb.isSubclass(type,"RealNumber")))
                            isReal = true;
                    }
                }
                if (StringUtil.isInteger(s))
                    isReal = true; // isInt = true; it could be a real that just doesn't have a decimal
                if (!isInt && StringUtil.isNumeric(s))
                    isReal = true;
                if (!isInt && !isReal) {
                    Formula sf = new Formula(s);
                    String type = kb.kbCache.getRange(sf.car());
                    if (type != null && (type.equals("Integer") || kb.isSubclass(type,"Integer")))
                        isInt = true;
                    if (type != null && (type.equals("RationalNumber") || kb.isSubclass(type,"RationalNumber")))
                        isRat = true;
                    if (type != null && (type.equals("RealNumber") || kb.isSubclass(type,"RealNumber")))
                        isReal = true;
                    argsStr.append(convertNumericFunctions(sf) + " ");
                }
                else
                    argsStr.append(s + " ");
            }
            if (debug) System.out.println("SUMOtoTFAform.convertNumericFunctions(): argsStr: " + argsStr);
            argsStr.deleteCharAt(argsStr.length()-1);
            String suffix = "";
            if (isMathFunction(car.theFormula)) {
                if (isInt)
                    suffix = "__0In1In2In";
                else if (isRat)
                    suffix = "__0Ra1Ra2Ra";
                else if (isReal)
                    suffix = "__0Re1Re2Re";
            }
            else {
                if (isInt)
                    suffix = "__1In2In";
                else if (isRat)
                    suffix = "__1Ra2Ra";
                else if (isReal)
                    suffix = "__1Re2Re";
            }
            if (suffix != "" && isMathFunction(car.theFormula))
                suffix = suffix + "Fn";
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
        if (debug) System.out.println("SUMOtoTFAform.convertNumericFunctions(): result: " + f);
        return f;
    }

    /** *************************************************************
     */
    private static String processQuant(Formula f, Formula car, String op,
                                       ArrayList<String> args) {

        //if (debug) System.out.println("SUMOtoTFAform.processQuant(): quantifier");
        if (args.size() < 2) {
            System.out.println("Error in SUMOtoTFAform.processQuant(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        else {
            //if (debug) System.out.println("SUMOtoTFAform.processQuant(): correct # of args");
            if (args.get(0) != null) {
                //if (debug) System.out.println("SUMOtoTFAform.processQuant(): valid varlist: " + args.get(0));
                Formula varlist = new Formula(args.get(0));
                ArrayList<String> vars = varlist.argumentsToArrayList(0);
                //if (debug) System.out.println("SUMOtoTFAform.processRecurse(): valid vars: " + vars);
                StringBuffer varStr = new StringBuffer();
                for (String v : vars) {
                    String oneVar = SUMOformulaToTPTPformula.translateWord(v,v.charAt(0),false);
                    if (varmap.keySet().contains(v) && !StringUtil.emptyString(varmap.get(v))) {
                        String type = mostSpecificTerm(varmap.get(v));
                        oneVar = oneVar + ":" + SUMOKBtoTFAKB.translateSort(kb,type);
                    }
                    varStr.append(oneVar + ", ");
                }
                //if (debug) System.out.println("SUMOtoTFAform.processQuant(): valid vars: " + varStr);
                String opStr = " ! ";
                if (op.equals("exists"))
                    opStr = " ? ";
                //if (debug) System.out.println("SUMOtoTFAform.processQuant(): quantified formula: " + args.get(1));
                return "(" + opStr + "[" + varStr.toString().substring(0,varStr.length()-2) + "] : (" +
                        processRecurse(new Formula(args.get(1))) + "))";
            }
            else {
                System.out.println("Error in SUMOtoTFAform.processQuant(): null arguments to " + op + " in " + f);
                return "";
            }
        }
    }

    /** *************************************************************
     */
    private static String processConjDisj(Formula f, Formula car,
                                       ArrayList<String> args) {

        String op = car.theFormula;
        if (args.size() < 2) {
            System.out.println("Error in SUMOtoTFAform.processConjDisj(): wrong number of arguments to " + op + " in " + f);
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
    private static String processLogOp(Formula f, Formula car,
                                        ArrayList<String> args) {
        String op = car.theFormula;
        //System.out.println("processRecurse(): op: " + op);
        //System.out.println("processRecurse(): args: " + args);
        if (op.equals("and"))
            return processConjDisj(f,car,args);
        if (op.equals("=>")) {
            if (args.size() < 2) {
                System.out.println("Error in SUMOtoTFAform.processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return processRecurse(new Formula(args.get(0))) + " => " +
                        processRecurse(new Formula(args.get(1)));
        }
        if (op.equals("<=>")) {
            if (args.size() < 2) {
                System.out.println("Error in SUMOtoTFAform.processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return "(" + processRecurse(new Formula(args.get(0))) + " => " +
                        processRecurse(new Formula(args.get(1))) + ") & (" +
                        processRecurse(new Formula(args.get(1))) + " => " +
                        processRecurse(new Formula(args.get(0))) + ")";
        }
        if (op.equals("or"))
            return processConjDisj(f,car,args);
        if (op.equals("not")) {
            if (args.size() != 1) {
                System.out.println("Error in SUMOtoTFAform.processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return "~(" + processRecurse(new Formula(args.get(0))) + ")";
        }
        if (op.equals("forall") || op.equals("exists"))
            return processQuant(f,car,op,args);
        System.out.println("Error in SUMOtoTFAform.processLogOp(): bad logical operator " + op + " in " + f);
        return "";
    }

    /** *************************************************************
     * @return whether the constant is a TFF built in numeric
     * type or a subclass of one of those types
     */
    private static boolean builtInNumericType(String arg) {

        if (!SUMOKBtoTFAKB.iChildren.contains(arg) &&
            !SUMOKBtoTFAKB.rChildren.contains(arg) &&
            !SUMOKBtoTFAKB.lChildren.contains(arg) &&
            !arg.equals("RealNumber") &&
            !arg.equals("RationalNumber") &&
            !arg.equals("Integer"))
            return false;
        else
            return true;
    }

    /** *************************************************************
     */
    private static boolean allBuiltInNumericTypes(ArrayList<String> args) {

        ArrayList<String> argtypes = collectArgTypes(args);
        if (debug) System.out.println("SUMOtoTFAform.allBuiltInNumericTypes(): arg types: " + argtypes);
        for (String arg : argtypes) {
            if (!StringUtil.emptyString(arg) && !arg.equals("Integer") &&
                    !arg.equals("RationalNumber") && !arg.equals("RealNumber") &&
                    !builtInNumericType(arg)) {
                if (debug) System.out.println("SUMOtoTFAform.allBuiltInNumericTypes(): returning false ");
                return false;
            }
        }
        if (debug) System.out.println("SUMOtoTFAform.allBuiltInNumericTypes(): returning true ");
        return true;
    }

    /** *************************************************************
     * equal is a special case since it needs translation to '='
     * regardless of argument types since it's polymorphic on $i also.
     */
    private static String processCompOp(Formula f, Formula car,
                                       ArrayList<String> args) {

        String op = car.theFormula;
        if (args.size() != 2) {
            System.out.println("Error in SUMOtoTFAform.processCompOp(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        if (!allBuiltInNumericTypes(args) && !op.startsWith("equal__")) {
            int ttype = f.theFormula.charAt(0);
            if (Character.isDigit(ttype))
                ttype = StreamTokenizer_s.TT_NUMBER;
            return op + "(" + processRecurse(new Formula(args.get(0))) + " ," +
                    processRecurse(new Formula(args.get(1))) + ")";
        }
        if (debug) System.out.println("SUMOtoTFAform.processCompOp(): op: " + op);
        if (debug) System.out.println("SUMOtoTFAform.processCompOp(): args: " + args);
        if (op.startsWith("equal")) {
            return "(" + processRecurse(new Formula(args.get(0))) + " = " +
                    processRecurse(new Formula(args.get(1))) + ")";
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

        System.out.println("Error in SUMOtoTFAform.processCompOp(): bad comparison operator " + op + " in " + f);
        return "";
    }

    /** *************************************************************
     */
    private static String processMathOp(Formula f, Formula car,
                                        ArrayList<String> args) {

        String op = car.theFormula;
        if (debug) System.out.println("SUMOtoTFAform.processMathOp(): op: " + op);
        if (debug) System.out.println("SUMOtoTFAform.processMathOp(): args: " + args);
        if (args.size() != 2) {
            System.out.println("Error in SUMOtoTFAform.processMathOp(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        if (!allBuiltInNumericTypes(args)) {
            int ttype = f.theFormula.charAt(0);
            if (Character.isDigit(ttype))
                ttype = StreamTokenizer_s.TT_NUMBER;
            return "s__" + op + "(" + processRecurse(new Formula(args.get(0))) + " ," +
                    processRecurse(new Formula(args.get(1))) + ")";
        }
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
        System.out.println("Error in SUMOtoTFAform.processMathOp(): bad math operator " + op + " in " + f);
        return "";
    }

    /** *************************************************************
     */
    public static String processRecurse(Formula f) {

        if (debug) System.out.println("SUMOtoTFAform.processRecurse(): " + f);
        if (debug) System.out.println("SUMOtoTFAform.processRecurse(): varmap: " + varmap);
        if (f == null)
            return "";
        if (f.atom()) {
            int ttype = f.theFormula.charAt(0);
            if (Character.isDigit(ttype))
                ttype = StreamTokenizer_s.TT_NUMBER;
            return SUMOformulaToTPTPformula.translateWord(f.theFormula,ttype,false);
        }
        Formula car = f.carAsFormula();
        //System.out.println("SUMOtoTFAform.processRecurse(): car: " + car);
        //System.out.println("SUMOtoTFAform.processRecurse(): car: " + car.theFormula);
        ArrayList<String> args = f.complexArgumentsToArrayList(1);
        if (car.listP()) {
            System.out.println("Error in SUMOtoTFAform.processRecurse(): formula " + f);
            return "";
        }
        if (Formula.isLogicalOperator(car.theFormula))
            return processLogOp(f,car,args);
        else if (isComparisonOperator(car.theFormula))
            return processCompOp(f,car,args);
        else if (isMathFunction(car.theFormula))
            return processMathOp(f,car,args);
        else {
            if (debug) System.out.println("SUMOtoTFAform.processRecurse(): not math or comparison op: " + car);
            StringBuffer argStr = new StringBuffer();
            for (String s : args)
                argStr.append(processRecurse(new Formula(s)) + ", ");
            String result = "s__" + car.theFormula + "(" + argStr.substring(0,argStr.length()-2) + ")";
            //if (debug) System.out.println("SUMOtoTFAform.processRecurse(): result: " + result);
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
     * check if t is one of the fundamental types of $int, $rat, $real
     * or SUMO types that are subtypes of Integer, RationalNumber or
     * RealNumber
     */
    private static boolean fundamentalTypeOrSubtype(String t, String sigType) {

        if (sigType.equals("Integer") && kb.isSubclass(t,"Integer"))
            return true;
        if (sigType.equals("RealNumber") && kb.isSubclass(t,"RealNumber"))
            return true;
        if (sigType.equals("RationalNumber") && kb.isSubclass(t,"RationalNumber"))
            return true;
        return false;
    }

    /** *************************************************************
     * check if t is a subtype of Integer, RationalNumber or
     * RealNumber
     */
    private static boolean fundamentalSubtype(String t, String sigType) {

        if (sigType.equals("Integer") && kb.isSubclass(t,"Integer") && !t.equals("Integer"))
            return true;
        if (sigType.equals("RealNumber") && kb.isSubclass(t,"RealNumber") && !t.equals("RealNumber"))
            return true;
        if (sigType.equals("RationalNumber") && kb.isSubclass(t,"RationalNumber") && !t.equals("RationalNumber"))
            return true;
        return false;
    }

    /** *************************************************************
     * @param suf is the current suffix to add to
     * @param t is the type of the actual argument to op
     * @param sigType is the type required for this argument to op
     * @param op is the relation
     */
    private static String numberSuffix(String suf, int arg, String op, String t, String sigType) {

        if (debug) System.out.println("numberSuffix(): suf,op,t,type: " +
                suf + ", " + op + ", " + t + ", " + sigType);
        if (debug) System.out.println("numberSuffix(): kb.isSubclass(t, sigType): " +
                kb.isSubclass(t, sigType));
        if (debug) System.out.println("numberSuffix(): kb.isSubclass(t, \"Number\"): " +
                kb.isSubclass(t, "Number"));
        if (StringUtil.emptyString(t) || op.equals("instance")) // || t.equals(sigType))
            return suf;
        //if (fundamentalSubtype(t,sigType))
        //    return "";
        String suffix = "";
        if (kb.isSubclass(t, sigType) && kb.isSubclass(t, "Number")) {
            if (kb.isSubclass(t, "Integer") || t.equals("Integer"))
                suffix = "In";
            if (kb.isSubclass(t, "RealNumber") || t.equals("RealNumber"))
                suffix = "Re";
            if (kb.isSubclass(t, "RationalNumber") || t.equals("RationalNumber"))
                suffix = "In";
        }
        if (debug) System.out.println("numberSuffix(): suffix: " +
                suffix);
        if (suffix.equals(""))
            return suf;
        return suf = suf + Integer.toString(arg) + suffix;
    }

    /** *************************************************************
     * Find the types of each argument.  If a variable, look up in
     * varmap.  If a function, check its return type.
     */
    private static ArrayList<String> collectArgTypes(ArrayList<String> args) {

        if (debug) System.out.println("SUMOtoTFAform.collectArgTypes(): varmap: " + varmap);
        ArrayList<String> types = new ArrayList<String>();
        for (String s : args) {
            if (Formula.isVariable(s)) {
                String vtype = kb.mostSpecificTerm(varmap.get(s));
                if (!StringUtil.emptyString(vtype))
                    types.add(vtype);
            }
            else if (Formula.listP(s)) {
                if (kb.isFunctional(s)) {
                    String op = (new Formula(s)).car();
                    String range = kb.kbCache.getRange(op);
                    if (!StringUtil.emptyString(range))
                        types.add(range);
                }
            }
            else if (kb.isInstance(s)) {
                HashSet<String> p = kb.immediateParents(s);
                String most = kb.mostSpecificTerm(p);
                if (!StringUtil.emptyString(most))
                    types.add(most);
            }
        }
        return types;
    }

    /** *************************************************************
     */
    private static String getOpType(String op) {

        String type = "";
        int i = op.indexOf("__");
        if (i != -1) {
            type = op.substring(i+2,op.length());
            if (type.endsWith("Fn"))
                type = type.substring(0,type.length()-2);
            return type;
        }
        ArrayList<String> sig = kb.kbCache.getSignature(op);
        return kb.mostSpecificTerm(sig);
    }

    /** *************************************************************
     * Constrain a list of arguments to be the most specific type
     */
    private static void constrainVars(String type, ArrayList<String> args) {

        if (!kb.isSubclass(type,"Quantity"))
            return;
        if (debug) System.out.println("SUMOtoTFAform.constrainVars(): " + type);
        for (String t : args) {
            if (!Formula.isVariable(t))
                continue;
            HashSet<String> types = varmap.get(t);
            if (debug) System.out.println("SUMOtoTFAform.constrainVars(): checking var " + t + " with type " + types);
            String lowest = kb.mostSpecificTerm(types);
            if (debug) System.out.println("SUMOtoTFAform.constrainVars(): type " + type + " lowest " + lowest);
            if (lowest == null || kb.termDepth(type) > kb.termDepth(lowest)) {  // classes lower in hierarchy have a large number (of levels)
                if (lowest == null)
                    types = new HashSet<>();
                types.add(type);
                if (debug) System.out.println("SUMOtoTFAform.constrainVars(): constraining " + t + " to " + type);
            }
        }
    }

    /** *************************************************************
     * for numerical and comparison operators, ensure their arguments
     * are all the same type and that it's the most specific type.  If
     * no types are found in the suffix, return the empty string.
     */
    private static String leastNumericType(String op, String suffix) {

        ArrayList<String> args = relationExtractSig("dummy__" + suffix);
        String least = "";
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i) != "") {
                if (least == "")
                    least = args.get(i);
                else if (kb.isSubclass(least,args.get(i)))
                    least = args.get(i);
            }
        }
        if (StringUtil.emptyString(least))
            return "";
        StringBuffer result = new StringBuffer();
        int start = 0;
        if (!kb.isFunction(op))
            start = 1;
        for (int i = start; i < args.size(); i++)
            result.append(Integer.toString(i) + least.substring(0,2));
        if (debug) System.out.println("SUMOtoTFAform.leastNumericType(): op: " + op);
        if (debug) System.out.println("SUMOtoTFAform.leastNumericType(): suffix: " + suffix);
        if (debug) System.out.println("SUMOtoTFAform.leastNumericType(): result: " + result);
        return result.toString();
    }

    /** *************************************************************
     * if the operator already has a suffix, revise it with the new
     * suffix in the case where the new argument type is more
     * specific
     */
    private static String composeSuffix(String op, String suffix) {

        if (StringUtil.emptyString(op))
            return op;
        if (StringUtil.emptyString(suffix))
            return op;
        String patternString = "(\\d)(In|Re|Ra)";

        int under = op.indexOf("__");
        if (under == -1)
            return op + suffix;
        ArrayList<String> newTypes = relationExtractSig("dummy" + suffix);
        String lowestNew = kb.mostSpecificTerm(newTypes);
        ArrayList<String> oldTypes = relationExtractSig(op);
        String lowestOld = kb.mostSpecificTerm(oldTypes);

        int maxArg = newTypes.size();
        if (oldTypes.size() > maxArg)
            maxArg = oldTypes.size();
        for (int i = 0; i < maxArg; i++)
            if (StringUtil.emptyString(oldTypes.get(i)) ||
                    (i >= newTypes.size()-1) ||
                    kb.isSubclass(oldTypes.get(i),newTypes.get(i)))
                safeSet(newTypes,i,oldTypes.get(i));
        StringBuffer result = new StringBuffer();
        result.append(op.substring(0,under));
        result.append("__");
        for (int i = 0; i < newTypes.size(); i++)
            if (!StringUtil.emptyString(newTypes.get(i)))
                result.append(Integer.toString(i) + newTypes.get(i).substring(0,2));
        if (op.endsWith("Fn"))
            result.append("Fn");
        return result.toString();
    }

    /** *************************************************************
     * Create a specialized version of KB.mostSpecificTerm() that
     * biases the results for TFF.  Prefer a built-in numeric type
     * (equivalents to $int, $rat, $real) over any more specific
     * type in SUMO
     */
    public static String mostSpecificTerm(Collection<String> args) {

        if (args == null || args.size() < 1)
            return null;
        String result = "";
        for (String t : args) {
            if (debug) System.out.println("SUMOtoTFAform.mostSpecificTerm(): t: " + t);
            if (debug) System.out.println("SUMOtoTFAform.mostSpecificTerm(): builtInNumericType(t): " + builtInNumericType(t));
            if (debug) System.out.println("SUMOtoTFAform.mostSpecificTerm(): builtInNumericType(result): " + builtInNumericType(result));
            if (debug) System.out.println("SUMOtoTFAform.mostSpecificTerm(): depth(t): " + kb.termDepth(t));
            if (debug) System.out.println("SUMOtoTFAform.mostSpecificTerm(): depth(result): " + kb.termDepth(result));
            if (debug) System.out.println("SUMOtoTFAform.mostSpecificTerm(): compareTermDepth(t,result): " + kb.compareTermDepth(t,result));
            if (StringUtil.emptyString(t) || !kb.containsTerm(t)) {
                if (!StringUtil.emptyString(t))
                    System.out.println("Error in SUMOtoTFAform.mostSpecificTerm(): no such term: " + t);
                continue;
            }
            if (result == "")
                result = t;
            else if (kb.compareTermDepth(t,result) > 0) {
                if (builtInNumericType(result) && !builtInNumericType(t))
                    continue;
                result = t;
            }
        }
        if (debug) System.out.println("SUMOtoTFAform.mostSpecificTerm(): result: " + result);
        return result;
    }

    /** *************************************************************
     * @param f    is a formula that has an operator to constrain
     * @param args is a list of arguments of formula f, starting with
     *             an empty first argument for the relation
     * @param op   is the operator of formula f
     * @return the constrained formula
     */
    private static String constrainOp(Formula f, String op, ArrayList<String> args) {

        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): op: " + op);
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): f: " + f);
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): args: " + args);
        String suffix = "";
        ArrayList<String> sig = kb.kbCache.getSignature(op);

        ArrayList<String> typeFromName = relationExtractSig(op);
        String opType = mostSpecificTerm(typeFromName);
        ArrayList<String> argtypes = collectArgTypes(args);

        String lowest = mostSpecificTerm(argtypes);
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): op type: " + opType);
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): arg types: " + argtypes);
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): most specific arg type: " + lowest);
        if ((StringUtil.emptyString(opType) || kb.isSubclass(lowest,opType))  &&
                (isComparisonOperator(op) || isMathFunction(op)) || op.equals("equal)")) {
            constrainVars(lowest,args); // side effect on varmap
        }
        if (sig == null) {
            System.out.println("Error in SUMOtoTFAform.constrainOp(): null signature for " + op);
        }
        else {
            for (int i = 1; i < args.size(); i++) {
                String arg = args.get(i);
                if (debug) System.out.println("SUMOtoTFAform.constrainOp(): arg: " + arg);
                if (i >= sig.size()) {
                    System.out.println("Error in SUMOtoTFAform.constrainOp(): missing signature element for " +
                            op + "  in form " + f);
                    continue;
                }
                String type = sig.get(i);
                if (Formula.listP(arg)) {
                    args.set(i, constrainFunctVarsRecurse(new Formula(arg)));
                    arg = args.get(i);
                    String justArg = (new Formula(arg)).car();
                    if (kb.isFunction(justArg)) { // ||
                            // (justArg.indexOf('_') != -1 && kb.isFunction(justArg.substring(0, justArg.indexOf('_'))))) {
                        String t = kb.kbCache.getRange(justArg);
                        if (StringUtil.emptyString(t))
                            System.out.println("Error in SUMOtoTFAform.constrainOp(): empty function range for " + justArg);
                        suffix = numberSuffix(suffix, i,op, t, type);
                        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): suffix(1): " + suffix);
                    }
                    if (Formula.isVariable(justArg)) {
                        String t = mostSpecificTerm(varmap.get(justArg));
                        if (StringUtil.emptyString(t))
                            System.out.println("Error in SUMOtoTFAform.constrainOp(): empty variable type for " + justArg);
                        suffix = numberSuffix(suffix, i,op, t, type);
                    }

                }
                else {
                    if (Formula.isVariable(arg)) {
                        String t = mostSpecificTerm(varmap.get(arg));
                        if (StringUtil.emptyString(t))
                            System.out.println("Error in SUMOtoTFAform.constrainOp(): empty variable type for " + arg);
                        suffix = numberSuffix(suffix, i,op, t, type);
                    }
                    if (StringUtil.isNumeric(arg)) { // even without a decimal, we can't be sure it's Integer
                        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): numeric argument: " + arg);
                        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): isInteger(): " + StringUtil.isInteger(arg));
                        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): type.equals(\"RealNumber\"): " + type.equals("RealNumber"));
                        String t = "RealNumber";
                        if (StringUtil.isInteger(arg)) {
                            if (type.equals("RealNumber")) {
                                if (debug)
                                    System.out.println("SUMOtoTFAform.constrainOp(): converting number to real " + arg);
                                arg = arg + ".0";
                                args.set(i, arg);
                                t = "RealNumber";
                            }
                            else
                                t = "Integer";
                        }
                        suffix = numberSuffix(suffix, i,op, t, type);
                    }
                }
            }
        }
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): suffix: " + suffix);
        if (isComparisonOperator(op) || isMathFunction(op))
            suffix = leastNumericType(op,suffix);
        if (kb.isFunction(op) && !StringUtil.emptyString(suffix)) {
            String range = numberSuffix("",0,op,kb.kbCache.getRange(op),kb.kbCache.getRange(op));
            if (debug) System.out.println("SUMOtoTFAform.constrainOp(): range: " + range);
            suffix = range + suffix + "Fn";
        }
        if (!StringUtil.emptyString(suffix))
            suffix = "__" + suffix;
        ArrayList<String> newargs = new ArrayList<>();
        newargs.addAll(args);
        newargs.remove(0);
        String composed = composeSuffix(op, suffix);
        String result = "(" + composed + " " + StringUtil.arrayListToSpacedString(newargs) + ")";
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): result: " + result);
        return result;
    }

    /** *************************************************************
     */
    private static String constrainFunctVarsRecurse(Formula f) {

        if (debug) System.out.println("SUMOtoTFAform.constrainFunctVarsRecurse(): " + f);
        if (f == null) return "";
        if (f.atom()) return f.theFormula;
        Formula car = f.carAsFormula();
        if (car.isVariable())
            return f.theFormula;
        //System.out.println("SUMOtoTFAform.processRecurse(): car: " + car);
        //System.out.println("SUMOtoTFAform.processRecurse(): car: " + car.theFormula);
        ArrayList<String> args = f.complexArgumentsToArrayList(1);
        args.add(0,""); // empty argument for the relation or operator
        if (car.listP()) {
            System.out.println("Error in SUMOtoTFAform.constrainFunctVarsRecurse(): formula " + f);
            return "";
        }
        String op = car.theFormula;
        if (debug) System.out.println("SUMOtoTFAform.constrainFunctVarsRecurse(): op: " + op);
        //ArrayList<String> sig = kb.kbCache.getSignature(op);
//        if ((car.theFormula.equals(Formula.EQUAL)) || isComparisonOperator(car.theFormula) ||
  //              isMathFunction(car.theFormula))
        if (!Formula.isLogicalOperator(op) && !Formula.isVariable(op))
            return constrainOp(f,op,args);
        else {
            StringBuffer resultString = new StringBuffer();
            resultString.append("(" + op);
            if (debug) System.out.println("SUMOtoTFAform.constrainFunctVarsRecurse(): logical operator or variable: " + op);
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
            String newt = mostSpecificTerm(newvartypes);
            HashSet<String> oldvartypes = varmap.get(k);
            String oldt = mostSpecificTerm(oldvartypes);
            //System.out.println("SUMOtoTFAform.constrainTypeRestriction(): newt, oldt: " +
            //        newt + ", " + oldt);
            if (StringUtil.emptyString(newt) && StringUtil.emptyString(oldt)) {
                System.out.println("Error in SUMOtoTFAform.constrainTypeRestriction(): empty variables: " +
                        newt + " " + oldt);
                System.out.println("Error in SUMOtoTFAform.constrainTypeRestriction(): newvarmap: " +
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
     * result is a side effect on varmap and the formula
     */
    private static void constrainFunctVars(Formula f) {

        int counter = 0;
        if (debug) System.out.println();
        if (debug) System.out.println("SUMOtoTFAform.constrainFunctVars(): formula: " + f);
        HashMap<String,HashSet<String>> oldVarmap = null;
        do {
            counter++;
            oldVarmap = cloneVarmap();
            String newf = constrainFunctVarsRecurse(f);
            f.theFormula = newf;
            HashMap<String,HashSet<String>> types = fp.findAllTypeRestrictions(f, kb);
            if (debug) System.out.println("SUMOtoTFAform.constrainFunctVars(): found types: " + types);
            constrainTypeRestriction(types);
            //System.out.println("SUMOtoTFAform.constrainFunctVars(): new varmap: " + varmap);
            //System.out.println("SUMOtoTFAform.constrainFunctVars(): old varmap: " + oldVarmap);
        } while (!varmap.equals(oldVarmap) && counter < 5);
    }

    /** *************************************************************
     * Recursive routine to eliminate occurrences of '=>', 'and' and 'or' that
     * have only one or zero arguments
     * @return the corrected formula as a string
     */
    public static String elimUnitaryLogops(Formula f) {

        //System.out.println("SUMOtoTFAform.elimUnitaryLogops(): f: " + f);
        if (f.empty() || f.atom()) {
            //System.out.println("SUMOtoTFAform.elimUnitaryLogops(): atomic result: " + f.theFormula);
            return f.theFormula;
        }
        ArrayList<String> args = f.complexArgumentsToArrayList(0);
        //System.out.println("SUMOtoTFAform.elimUnitaryLogops(): args: " + args);
        //System.out.println("SUMOtoTFAform.elimUnitaryLogops(): size: " + args.size());
        //System.out.println("SUMOtoTFAform.elimUnitaryLogops(): car: " + f.car());
        if (f.car().equals("and") || f.car().equals("or") || f.car().equals("=>")) {
            if (args.size() == 1) {  // meaning that the one "argument" is the predicate
                //System.out.println("SUMOtoTFAform.elimUnitaryLogops(): empty result: ");
                return "";
            }
            if (args.size() == 2) {
                //System.out.println("SUMOtoTFAform.elimUnitaryLogops(): elimination: " + args.get(1));
                String result = elimUnitaryLogops(new Formula(args.get(1)));
                //System.out.println("SUMOtoTFAform.elimUnitaryLogops(): result: " + result);
                return result;
            }
        }
        //System.out.println("SUMOtoTFAform.elimUnitaryLogops(): not an elimination ");
        StringBuffer result = new StringBuffer();
        result = result.append("(" + args.get(0));
        for (int i = 1; i < args.size(); i++) {
            result.append(" " + elimUnitaryLogops(new Formula(args.get(i))));
            //System.out.println("SUMOtoTFAform.elimUnitaryLogops(): appending: " + result);
        }
        result.append(")");
        //System.out.println("SUMOtoTFAform.elimUnitaryLogops(): result: " + result);
        return result.toString();
    }

    /** *************************************************************
     * Substitute the values of numeric constants for their names.
     * Note that this is risky since it must be kept up to date
     * with the content of the knowledge base.  TODO: generalize this
     * Result is a side-effect on the formula
     */
    public static void instantiateNumericConstants(Formula f) {

        if (f.car().equals("instance"))
            return;
        List<String> constants = Arrays.asList("NumberE", "Pi");
        List<String> values = Arrays.asList("2.718282", "3.141592653589793");
        boolean found = false;
        for (int i = 0; i < constants.size(); i++) {
            String s = constants.get(i);
            String val = values.get(i);
            if (f.theFormula.indexOf(s) != -1) {
                f.theFormula = f.rename(s,val).theFormula;
            }
        }
    }

    /** *************************************************************
     * This is the primary method of the class.  It takes a SUO-KIF
     * formula and returns a TFF formula
     */
    public static String process(Formula f) {

        if (f.theFormula.startsWith("(instance equal"))
            return "";
        if (debug) System.out.println("\nSUMOtoTFAform.process(): =======================");
        instantiateNumericConstants(f);
        f.theFormula = modifyPrecond(f);
        f.theFormula = modifyTypesToConstraints(f);
        if (debug) System.out.println("SUMOtoTFAform.process(): formula before elimUnitaryLogops: " + f);
        String oldf = f.theFormula;
        int counter = 0;
        do {
            counter++;
            //System.out.println("SUMOtoTFAform.process(): counter: " + counter);
            oldf = f.theFormula;
            f.theFormula = elimUnitaryLogops(f); // remove empty (and... and (or... and =>...
        } while (!f.theFormula.equals(oldf) && counter < 5);
        if (debug) System.out.println("SUMOtoTFAform.process(): formula after elimUnitaryLogops: " + f);
        varmap = fp.findAllTypeRestrictions(f, kb);
        f.theFormula = convertNumericFunctions(f).theFormula;
        varmap = fp.findAllTypeRestrictions(f, kb);
        if (debug) System.out.println("SUMOtoTFAform.process(): formula: " + f);
        if (debug) System.out.println("SUMOtoTFAform.process(): varmap: " + varmap);
        oldf = f.theFormula;
        counter = 0;
        do {
            counter++;
            //System.out.println("SUMOtoTFAform.process(): counter: " + counter);
            oldf = f.theFormula;
            constrainFunctVars(f);
        } while (!f.theFormula.equals(oldf) && counter < 5);
        if (f != null && f.listP()) {
            ArrayList<String> UqVars = f.collectUnquantifiedVariables();
            if (debug) System.out.println("SUMOtoTFAform.process(): unquant: " + UqVars);
            String result = processRecurse(f);
            if (debug) System.out.println("SUMOtoTFAform.process(): result 1: " + result);
            StringBuffer qlist = new StringBuffer();
            for (String s : UqVars) {
                if (debug) System.out.println("process(): s: " + s);
                String t = "";
                String oneVar = SUMOformulaToTPTPformula.translateWord(s,s.charAt(0),false);
                if (varmap.keySet().contains(s) && !StringUtil.emptyString(varmap.get(s))) {
                    t = mostSpecificTerm(varmap.get(s));
                    if (debug) System.out.println("SUMOtoTFAform.process(): varmap.get(s): " + varmap.get(s));
                    if (debug) System.out.println("SUMOtoTFAform.process(): t: " + t);
                    if (t != null)
                        qlist.append(oneVar + " : " + SUMOKBtoTFAKB.translateSort(kb,t) + ",");
                }
            }
            if (qlist.length() > 1) {
                qlist.deleteCharAt(qlist.length() - 1);  // delete final comma
                result = "! [" + qlist + "] : (" + result + ")";
            }
            if (debug) System.out.println("SUMOtoTFAform.process(): result 2: " + result);
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
        //System.out.println("SUMOtoTFAform.matchingPrecond(): term: " + term);
        //System.out.println("SUMOtoTFAform.matchingPrecond(): ant: " + ant);
        if (ant == null)
            return null;
        Pattern p = Pattern.compile("\\(instance \\?(\\w+) " + term + "\\)");
        Matcher m = p.matcher(ant);
        if (m.find()) {
            //System.out.println("SUMOtoTFAform.matchingPrecond(): matches! ");
            String var = m.group(1);
            return var;
        }
        return null;
    }

    /** *************************************************************
     * if all or part of a consequent of a rule is of the form (instance ?X term)
     * @return the name of the type in the instance statement
     */
    private static String matchingInstanceTerm(Formula f) {

        //String cons = FormulaUtil.consequent(f);
        //System.out.println("SUMOtoTFAform.matchingPostcondTerm(): const: " + cons);
        if (f.theFormula == null)
            return null;
        Pattern p = Pattern.compile("\\(instance \\?\\w+ (\\w+)\\)");
        Matcher m = p.matcher(f.theFormula);
        if (m.find()) {
            if (debug) System.out.println("SUMOtoTFAform.matchingInstanceTerm(): matches! ");
            String type = m.group(1);
            if (debug) System.out.println("SUMOtoTFAform.matchingInstanceTerm(): type: " + type);
            return type;
        }
        return null;
    }

    /** *************************************************************
     * if all or part of a rule is of the form (instance ?X term)
     * @return the name of the type in the instance statement
     */
    private static String matchingInstance(Formula f) {

        if (f.theFormula == null)
            return null;
        HashSet<String> intChildren = kb.kbCache.getChildClasses("Integer");
        if (debug) System.out.println("SUMOtoTFAform.matchingInstance(): int: " + intChildren);
        HashSet<String> realChildren = new HashSet<String>();
        realChildren.addAll(kb.kbCache.getChildClasses("RealNumber"));
        if (realChildren.contains("Integer"))
            realChildren.remove("Integer");
        if (debug) System.out.println("SUMOtoTFAform.matchingInstance(): real: " + realChildren);
        Pattern p = Pattern.compile("\\(instance \\?\\w+ (\\w+)\\)");
        Matcher m = p.matcher(f.theFormula);
        while (m.find()) {
            if (debug) System.out.println("SUMOtoTFAform.matchingInstanceTerm(): matches! ");
            String type = m.group(1);
            if (debug) System.out.println("SUMOtoTFAform.matchingInstanceTerm(): type: " + type);
            if (intChildren.contains(type))
                return type;
            else if (realChildren.contains(type))
                return type;
        }
        return null;
    }

    /** *************************************************************
     * remove statements of the form (instance ?X term) if 'term' is
     * Integer or RealNumber and ?X is already of that type in the
     * quantifier list for the formula
     * @return the modified formula
     */
    protected static String modifyPrecond(Formula f) {

        if (f == null)
            return f.theFormula;
        String type = "Integer";
        Pattern p = Pattern.compile("\\(instance \\?(\\w+) " + type + "\\)");
        Matcher m = p.matcher(f.theFormula);
        if (m.find()) {
            String var = m.group(1);
            f.theFormula = m.replaceAll("");
        }

        type = "RealNumber";
        p = Pattern.compile("\\(instance \\?(\\w+) " + type + "\\)");
        m = p.matcher(f.theFormula);
        if (m.find()) {
            String var = m.group(1);
            f.theFormula = m.replaceAll("");
        }

        return f.theFormula;
    }

    /** *************************************************************
     * replace type statements of the form (instance ?X term), where
     * term is a subtype of Integer or RealNumber with a constraint
     * that defines that type
     * @return String version of the modified formula
     */
    protected static String modifyTypesToConstraints(Formula f) {

        if (debug) System.out.println("SUMOtoTFAform.modifyTypesToConstraints(): " + f);
        String type = null;
        boolean found = false;
        do {
            type = matchingInstance(f);
            if (debug) System.out.println("SUMOtoTFAform.modifyTypesToConstraints(): found type: " + type);
            if (type == null)
                return f.theFormula;
            Pattern p = Pattern.compile("\\(instance \\?(\\w+) " + type + "\\)");
            Matcher m = p.matcher(f.theFormula);
            if (m.find()) {
                found = true;
                String var = m.group(1);
                String toReplace = "(instance ?" + var + " " + type + ")";
                String cons = numericConstraints.get(type);
                if (StringUtil.emptyString(cons))
                    System.out.println("Error in SUMOtoTFAform.modifyTypesToConstraints(): no constraint for " + type);
                String origVar = numericVars.get(type);
                String newCons = cons.replace("?" + origVar, "?" + var);
                if (debug) System.out.println("SUMOtoTFAform.modifyTypesToConstraints(): replacing " +
                    toReplace + " with " + newCons);
                f.theFormula = f.theFormula.replace(toReplace, newCons);
            }
            else
                found = false;
        } while (type != null && found);
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
        //System.out.println("SUMOtoTFAform.buildNumericConstraints(): int: " + intChildren);
        HashSet<String> realChildren = new HashSet<String>();
        realChildren.addAll(kb.kbCache.getChildClasses("RealNumber"));
        if (realChildren.contains("Integer"))
            realChildren.remove("Integer");
        //System.out.println("SUMOtoTFAform.buildNumericConstraints(): real: " + realChildren);
        //HashSet<Formula> intForms = new HashSet<>();
        for (String t : intChildren) {
            //System.out.println("SUMOtoTFAform.buildNumericConstraints(): t: " + t);
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
            //System.out.println("SUMOtoTFAform.buildNumericConstraints(): t: " + t);
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
        System.out.println("SUMOtoTFAform.test5(): " + modifyTypesToConstraints(f));
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
                "=> $sum($product(s__FloorFn__1InFn($quotient_e(V__NUMBER1 ,V__NUMBER2)) ,V__NUMBER2) ,V__NUMBER) = " +
                "V__NUMBER1) & " +
                "($sum($product(s__FloorFn__1InFn($quotient_e(V__NUMBER1 ,V__NUMBER2)) ,V__NUMBER2) ,V__NUMBER) = " +
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
        System.out.println("test7() expected: ! [V__NUMBER1 : $real,V__NUMBER2 : $real] : " +
                "((s__AbsoluteValueFn__0Re1ReFn(V__NUMBER1) = V__NUMBER2 => s__SignumFn__0In1ReFn(V__NUMBER1) = 1 |" +
                " s__SignumFn__0In1ReFn(V__NUMBER1) = 0 & V__NUMBER1 = V__NUMBER2 | " +
                "s__SignumFn__0In1ReFn(V__NUMBER1) = -1 & V__NUMBER2 = $difference(0.0 ,V__NUMBER1)) & " +
                "(s__SignumFn__0In1ReFn(V__NUMBER1) = 1 | s__SignumFn__0In1ReFn(V__NUMBER1) = 0 & " +
                "V__NUMBER1 = V__NUMBER2 | s__SignumFn__0In1ReFn(V__NUMBER1) = -1 & " +
                "V__NUMBER2 = $difference(0.0 ,V__NUMBER1) => s__AbsoluteValueFn__0Re1ReFn(V__NUMBER1) = V__NUMBER2)).");
    }

    /** *************************************************************
     */
    public static void test8() {

        Formula f = new Formula("(<=> (equal (LastFn ?LIST) ?ITEM) (exists (?NUMBER) " +
                "(and (equal (ListLengthFn ?LIST) ?NUMBER) " +
                "(equal (ListOrderFn ?LIST ?NUMBER) ?ITEM))))");
        System.out.println("SUMOtoTFAform.test8(): " + process(f));
        System.out.println("test8() expected: tff(kb_SUMO_138,axiom,(! [V__LIST : $i,V__ITEM : $i] : " +
                "((s__LastFn(V__LIST) = V__ITEM =>  ? [V__NUMBER:$int] : " +
                "(s__ListLengthFn(V__LIST) = V__NUMBER & s__ListOrderFn(V__LIST, V__NUMBER) = V__ITEM)) & " +
                "( ? [V__NUMBER:$int] : " +
                "(s__ListLengthFn(V__LIST) = V__NUMBER & s__ListOrderFn(V__LIST, V__NUMBER) = V__ITEM) => " +
                "s__LastFn(V__LIST) = V__ITEM)))).");
    }

    /** *************************************************************
     */
    public static void test9() {

        Formula f = new Formula("(<=> (and (equal (AbsoluteValueFn ?NUMBER1) ?NUMBER2) " +
                "(instance ?NUMBER1 RealNumber) (instance ?NUMBER2 RealNumber)) " +
                "(or (and (instance ?NUMBER1 NonnegativeRealNumber) (equal ?NUMBER1 ?NUMBER2)) " +
                "(and (instance ?NUMBER1 NegativeRealNumber) (equal ?NUMBER2 (SubtractionFn 0 ?NUMBER1)))))");
        System.out.println("SUMOtoTFAform.test9(): " + process(f));
        System.out.println("test9() expected: tff(kb_SUMO_1,axiom,(! [V__NUMBER1 : $real,V__NUMBER2 : $real] : " +
                "((s__AbsoluteValueFn(V__NUMBER1) = V__NUMBER2 => " +
                "s__SignumFn(V__NUMBER1) = 1 | s__SignumFn(V__NUMBER1) = 0 & " +
                "V__NUMBER1 = V__NUMBER2 | s__SignumFn(V__NUMBER1) = -1 & " +
                "V__NUMBER2 = $difference(0.0 ,V__NUMBER1)) & (s__SignumFn(V__NUMBER1) = 1 | " +
                "s__SignumFn(V__NUMBER1) = 0 & V__NUMBER1 = V__NUMBER2 | " +
                "s__SignumFn(V__NUMBER1) = -1 & V__NUMBER2 = $difference(0.0 ,V__NUMBER1) => " +
                "s__AbsoluteValueFn(V__NUMBER1) = V__NUMBER2)))).");
    }

    /** *************************************************************
     */
    public static void test10() {

        Formula f = new Formula("(<=> (and (equal (AbsoluteValueFn ?NUMBER1) ?NUMBER2) " +
                "(instance ?NUMBER1 RealNumber) (instance ?NUMBER2 RealNumber)) " +
                "(or (and (instance ?NUMBER1 NonnegativeRealNumber) (equal ?NUMBER1 ?NUMBER2)) " +
                "(and (instance ?NUMBER1 NegativeRealNumber) (equal ?NUMBER2 (SubtractionFn 0 ?NUMBER1)))))");
        System.out.println("SUMOtoTFAform.test10(): " + process(f));
        System.out.println("test10() expected: tff(kb_SUMO_1,axiom,(! [V__NUMBER1 : $real,V__NUMBER2 : $real] : " +
                "((s__AbsoluteValueFn__0Re1ReFn(V__NUMBER1) = V__NUMBER2 => s__SignumFn__0In1ReFn(V__NUMBER1) = 1 | " +
                "s__SignumFn__0In1ReFn(V__NUMBER1) = 0 & V__NUMBER1 = V__NUMBER2 | " +
                "s__SignumFn__0In1ReFn(V__NUMBER1) = -1 & V__NUMBER2 = $difference(0.0 ,V__NUMBER1)) & " +
                "(s__SignumFn__0In1ReFn(V__NUMBER1) = 1 | s__SignumFn__0In1ReFn(V__NUMBER1) = 0 & " +
                "V__NUMBER1 = V__NUMBER2 | s__SignumFn__0In1ReFn(V__NUMBER1) = -1 & " +
                "V__NUMBER2 = $difference(0.0 ,V__NUMBER1) => s__AbsoluteValueFn__0Re1ReFn(V__NUMBER1) = V__NUMBER2)))).");
    }

    /** *************************************************************
     */
    public static void testRelEmbed() {

        String rel = "AbsoluteValueFn";
        ArrayList<String> sig = kb.kbCache.getSignature(rel);
        System.out.println("SUMOtoTFAform.testRlEmbed(): " + sig);
        System.out.println("SUMOtoTFAform.testRlEmbed(): new name: " + relationEmbedSig(rel,sig));
        kb.kbCache.extendInstance(rel,"1Re");
        kb.kbCache.signatures.put(rel + "__" + "1Re",sig);
    }

    /** *************************************************************
     */
    public static void testRelExtract() {

        String rel = "AbsoluteValueFn__1ReFn";
        System.out.println("SUMOtoTFAform.testRelExtract(): new name: " + relationExtractSig(rel));
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        debug = true;
        SUMOKBtoTFAKB skbtfakb = new SUMOKBtoTFAKB();
        skbtfakb.initOnce();
        String filename = KBmanager.getMgr().getPref("kbDir") + File.separator + "SUMO.tff";
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter(filename));
            skbtfakb.writeSorts(pw,filename);
            //skbtfakb.writeFile(filename, null, false, "", false, pw);
            pw.flush();
            pw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //debug = true;
        initOnce();
        setNumericFunctionInfo();
        System.out.println(numericConstraints);
        System.out.println(numericVars);
        //testRelEmbed();
        //testRelExtract();
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
