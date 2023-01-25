package com.articulate.sigma.trans;

//This software is released under the GNU Public License
//<http://www.gnu.org/copyleft/gpl.html>.
// Copyright 2019-2020 Infosys, 2020- Articulate Software
// apease@articulatesoftware.com

import com.articulate.sigma.*;
import com.articulate.sigma.utils.MapUtils;
import com.articulate.sigma.utils.StringUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by apease on 7/23/18.
 */
public class SUMOtoTFAform {

    public static KB kb;

    public static boolean debug = false;

    // a Set of types for each variable key
    public static HashMap<String,HashSet<String>> varmap = null;

    // a map of relation signatures (where function returns are index 0)
    // modified from the original by the constraints of the axiom
    private static HashMap<String,ArrayList<String>> signatures = null;

    public static boolean initialized = false;

    public static FormulaPreprocessor fp = new FormulaPreprocessor();

    // constraints on numeric types
    public static HashMap<String,String> numericConstraints = new HashMap<>();

    // variable names of constraints on numeric types
    public static HashMap<String,String> numericVars = new HashMap<>();

    // numeric constraint axioms that need not be processed since
    // their constraints will be substituted into axioms directly
    public static HashSet<String> numConstAxioms = new HashSet<>();

    // types like E and Pi
    public static HashMap<String,String> numericConstantTypes = new HashMap<>();
    public static HashMap<String,String> numericConstantValues = new HashMap<>();
    public static int numericConstantCount = 0; // to compare with if another constant is found after initialization

    // storage for a message why the formula wasn't translated
    public static String filterMessage = "";

    // extra sorts determined just for this formula
    public HashSet<String> sorts = new HashSet<>();

    public static HashSet<String> errors = new HashSet<>();

    /** *************************************************************
     * comparison ops are EQUAL, GT, GTET, LT, LTET
     */
    public static boolean isComparisonOperator(String s) {

        if (StringUtil.emptyString(s))
            return false;
        int under = s.lastIndexOf("__");
        if (under == -1)
            return Formula.isComparisonOperator(s);
        if (Formula.isComparisonOperator(s.substring(0,under)))
            return true;
        return false;
    }

    /** *************************************************************
     * math ops are PLUSFN, MINUSFN, TIMESFN, DIVIDEFN, FLOORFN
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
     * math ops are PLUSFN, MINUSFN, TIMESFN, DIVIDEFN, FLOORFN
     */
    public static boolean isEqualTypeOp(String s) {

        if (StringUtil.emptyString(s))
            return false;
        return (isComparisonOperator(s) || isMathFunction(s)) && !s.equals(Formula.FLOORFN);
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
     * Fill the indicated elements with "Entity", starting at start and ending
     * at end-1
     */
    public static void fill (ArrayList<String> ar, int start, int end) {

        fill(ar,start,end,"Entity");
    }

    /** *************************************************************
     * Fill the indicated elements with the given string, starting at start and ending
     * at end-1
     */
    public static void fill (ArrayList<String> ar, int start, int end, String fillStr) {

        if (ar.size() <= end)
            for (int i = start; i < end; i++)
                ar.add(fillStr);
        else
            for (int i = start; i < end; i++)
                ar.set(i,fillStr);
    }

    /** *************************************************************
     * If there's no such element index, fill the previous elements
     * with Entity
     */
    public static void safeSet (ArrayList<String> ar, int index, String val) {

        if (index > ar.size()-1)
            fill(ar,ar.size(),index+1);
        ar.set(index,val);
    }

    /** *************************************************************
     * Return the full signature
     */
    protected static ArrayList<String> relationExtractNonNumericSig(String rel) {

        if (debug) System.out.println("relationExtractNonNumericSig(): rel " + rel);
        if (debug) System.out.println("relationExtractNonNumericSig(): sig " + kb.kbCache.signatures);
        String bareRel = rel.substring(0,rel.length() - 3);
        ArrayList<String> sig = new ArrayList();
        int size = Integer.parseInt(rel.substring(rel.length()-1,rel.length()));
        sig = kb.kbCache.signatures.get(bareRel);
        String type = kb.kbCache.variableArityType(bareRel);
        fill(sig,sig.size(),size,type);
        return sig;
    }

    /** *************************************************************
     */
    private static String suffixToType(String type) {

        if (type.equals("In"))
            return "Integer";
        else if (type.equals("Re"))
            return "RealNumber";
        else if (type.equals("Ra"))
            return "RationalNumber";
        else if (type.equals("En"))
            return "Entity";
        else {
            System.out.println("Error in SUMOtoTFAform.suffixToType(): unknown type " + type);
            return "Entity";
        }
    }

    /** *************************************************************
     */
    private static String typeToSuffix(String type) {

        if (type.equals("Integer"))
            return "In";
        else if (type.equals("RealNumber"))
            return "Re";
        else if (type.equals("RationalNumber"))
            return "Ra";
        else if (type.equals("Entity"))
            return "En";
        else {
            System.out.println("Error in SUMOtoTFAform.typeToSuffix(): unknown type " + type);
            return "En";
        }
    }

    /** *************************************************************
     * Extract modifications to the relation signature from annotations
     * embedded in the suffixes to its name.  Note that the first argument
     * to a relation is number 1, so getting the 0th argument of the
     * returned ArrayList would give an empty string result for a relation.
     */
    public static ArrayList<String> relationExtractSigFromName(String rel) {

        if (StringUtil.emptyString(rel))
            return new ArrayList<String>();
        ArrayList<String> sig = new ArrayList();
        String patternString = "(\\d)(In|Re|Ra|En)";

        int under = rel.indexOf("__");
        if (under == -1)
            return sig;
        if (rel.matches(".*__\\d$"))
            return relationExtractNonNumericSig(rel);
        String text = rel.substring(under + 2, rel.length());
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String type = matcher.group(2);
            type = suffixToType(type);
            int arg = Integer.parseInt(matcher.group(1));
            if (arg > sig.size()-1)
                fill(sig,sig.size(),arg+1);
            safeSet(sig,arg,type);
        }
        return sig;
    }

    /** *************************************************************
     * Extract modifications to the relation signature from annotations
     * embedded in the suffixes to its name.  Note that the first argument
     * to a relation is number 1, so getting the 0th argument of the
     * returned ArrayList would give an empty string result for a relation.
     */
    public static ArrayList<String> relationExtractUpdateSigFromName(String rel) {

        if (debug) System.out.println("SUMOtoTFAform.relationExtractUpdateSigFromName(): rel: " + rel);
        if (StringUtil.emptyString(rel))
            return new ArrayList<String>();
        ArrayList<String> origsig = kb.kbCache.getSignature(withoutSuffix(rel));
        if (origsig == null) {
            System.out.println("Error in SUMOtoTFAform.relationExtractUpdateSigFromName(): null signature for " + rel);
            return null;
        }
        ArrayList<String> sig = new ArrayList();
        sig.addAll(origsig);
        ArrayList<String> newsig = relationExtractSigFromName(rel);
        if (sig.size() < newsig.size())
            fill(sig,sig.size(),newsig.size());
        for (int i = 0; i < newsig.size(); i++)
            if (i < newsig.size() && !StringUtil.emptyString(newsig.get(i)))
                sig.set(i,newsig.get(i));
        if (debug) System.out.println("SUMOtoTFAform.relationExtractUpdateSigFromName(): sig: " + sig);
        return sig;
    }

    /** *************************************************************
     * Embed the type signature for TFF numeric types into the name of
     * the relation.  This is used for when a relation's signature is
     * modified from its authored original

    private static String relationEmbedSig(String rel, ArrayList<String> sig) {

        if (rel.equals("instance"))
            return "";
        String[] a = new String[] {"In","Re","Ra"};
        Collection typeChars = Arrays.asList(a);
        StringBuffer sb = new StringBuffer();
        sb.append(rel + "__");
        if (kb.isFunction(rel) && typeChars.contains(sig.get(0).substring(0,2))) {
            sb.append("0" + sig.get(0).substring(0,2));
        }
        for (int i = 1; i < sig.size(); i++) {
            if (typeChars.contains(sig.get(i).substring(0,2)))
                sb.append(i + sig.get(i).substring(0,2));
        }
        return sb.toString();
    }
*/
    /** *************************************************************

    private static void constrainVarsFromFunct(ArrayList<String> sig, String typeStr) {

        if (debug) System.out.println("SUMOtoTFAform.constrainVarsFromFunct(): sig,typeStr: " +
                sig + ", " + typeStr);
        String type = "";
        int start = 0;
        if (typeStr.equals("__0In")) {
            type = "Integer";
        }
        else if (typeStr.equals("__0Re")) {
            type = "RealNumber";
        }
        else if (typeStr.equals("__0Ra")) {
            type = "RationalNumber";
        }
        if (typeStr.equals("__1In")) {
            type = "Integer";
            start = 1;
        }
        else if (typeStr.equals("__1Re")) {
            type = "RealNumber";
            start = 1;
        }
        else if (typeStr.equals("__1Ra")) {
            type = "RationalNumber";
            start = 1;
        }
        for (int i = start; i <= 2; i++) {
            String arg = sig.get(i);
            if (Formula.isVariable(arg)) {
                HashSet<String> types = varmap.get(arg);
                types.add(type);
                varmap.put(arg,types);
            }
        }
    }
*/
    /** *************************************************************
     * Recurse through the formula giving numeric and comparison
     * operators a suffix if they operate on
     * numbers.

    public static Formula convertNumericFunctions(Formula f, String parentType) {

        if (f == null)
            return f;
        if (f.atom()) {
            int ttype = f.getFormula().charAt(0);
            if (Character.isDigit(ttype))
                ttype = StreamTokenizer_s.TT_NUMBER;
            return f;
        }
        Formula car = f.carAsFormula();
        ArrayList<String> args = f.complexArgumentsToArrayListString(1);
        if (isMathFunction(car.getFormula()) ||
                (isComparisonOperator(car.getFormula()) && !car.getFormula().equals("equal"))) {
            StringBuffer argsStr = new StringBuffer();
            boolean isInt = false;
            boolean isReal = false;
            boolean isRat = false;
            for (String s : args) {
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
                if (StringUtil.isInteger(s)) {
                    if (isMathFunction(car.getFormula()) && kb.isSubclass(parentType,"Integer"))
                        isInt = true;
                    else
                        isReal = true; // isInt = true; it could be a real that just doesn't have a decimal
                }
                if (!isInt && StringUtil.isNumeric(s))
                    isReal = true;
                if (!isInt && !isReal) {
                    Formula sf = new Formula(s);
                    Formula argResult = convertNumericFunctions(sf,"");
                    String type = kb.kbCache.getRange(argResult.car());
                    if (type != null && (type.equals("Integer") || kb.isSubclass(type,"Integer")))
                        isInt = true;
                    if (type != null && (type.equals("RationalNumber") || kb.isSubclass(type,"RationalNumber")))
                        isRat = true;
                    if (type != null && (type.equals("RealNumber") || kb.isSubclass(type,"RealNumber")))
                        isReal = true;
                    argsStr.append(argResult + " ");
                }
                else
                    argsStr.append(s + " ");
            }
            argsStr.deleteCharAt(argsStr.length()-1);
            String suffix = "";
            if (isMathFunction(car.getFormula())) {
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
            if (suffix != "" && isMathFunction(car.getFormula()))
                suffix = suffix + "Fn";
            String withSuffix = composeSuffix(car.getFormula(),suffix);
            f = new Formula("(" + withSuffix + " " + argsStr.toString() + ")");
            //if (isInt || isReal || isRat)
            //    constrainVarsFromFunct(args,suffix.substring(0,4));
        }
        else {
            StringBuffer argsStr = new StringBuffer();
            ArrayList<String> sig = kb.kbCache.signatures.get(car.toString());
            if (args != null) {
                for (int i = 0; i < args.size(); i++) {
                    String s = args.get(i);
                    String type = "";
                    if (sig != null && (i+1) < sig.size())
                        type = sig.get(i+1);
                    Formula sf = new Formula(s);
                    argsStr.append(convertNumericFunctions(sf,type) + " ");
                }
                argsStr.deleteCharAt(argsStr.length() - 1);
                f = new Formula("(" + car.getFormula() + " " + argsStr.toString() + ")");
            }
        }
        return f;
    }
*/
    /** *************************************************************
     */
    private static String processQuant(Formula f, Formula car, String parentType, String op,
                                       ArrayList<String> args) {

        //if (debug) System.out.println("SUMOtoTFAform.processQuant(): quantifier");
        if (args.size() != 3) { // quant + list + formula
            System.out.println("Error in SUMOtoTFAform.processQuant(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        else {
            //if (debug) System.out.println("SUMOtoTFAform.processQuant(): correct # of args");
            if (args.get(1) != null) {
                //if (debug) System.out.println("SUMOtoTFAform.processQuant(): valid varlist: " + args.get(0));
                Formula varlist = new Formula(args.get(1));
                ArrayList<String> vars = varlist.argumentsToArrayListString(0);
                //if (debug) System.out.println("SUMOtoTFAform.processRecurse(): valid vars: " + vars);
                StringBuffer varStr = new StringBuffer();
                for (String v : vars) {
                    String oneVar = SUMOformulaToTPTPformula.translateWord(v,v.charAt(0),false);
                    if (varmap.keySet().contains(v) && !StringUtil.emptyString(varmap.get(v))) {
                        String type = mostSpecificType(varmap.get(v));
                        oneVar = oneVar + ":" + SUMOKBtoTFAKB.translateSort(kb,type);
                    }
                    else
                        System.out.println("Error in SUMOtoTFAform.processQuant(): var type not found for " + v +
                                " in formula " + f);
                    varStr.append(oneVar + ", ");
                }
                //if (debug) System.out.println("SUMOtoTFAform.processQuant(): valid vars: " + varStr);
                String opStr = " ! ";
                if (op.equals("exists"))
                    opStr = " ? ";
                //if (debug) System.out.println("SUMOtoTFAform.processQuant(): quantified formula: " + args.get(1));
                return "(" + opStr + "[" + varStr.toString().substring(0,varStr.length()-2) + "] : (" +
                        processRecurse(new Formula(args.get(2)),parentType) + "))";
            }
            else {
                System.out.println("Error in SUMOtoTFAform.processQuant(): null arguments to " + op + " in " + f);
                return "";
            }
        }
    }

    /** *************************************************************
     */
    private static String processConjDisj(Formula f, Formula car, String parentType,
                                       ArrayList<String> args) {

        String op = car.getFormula();
        if (args.size() < 3) {
            System.out.println("Error in SUMOtoTFAform.processConjDisj(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        String tptpOp = "&";
        if (op.equals("or"))
            tptpOp = "|";
        StringBuffer sb = new StringBuffer();
        sb.append("(" + processRecurse(new Formula(args.get(1)),parentType));
        for (int i = 2; i < args.size(); i++) {
            sb.append(" " + tptpOp + " " + processRecurse(new Formula(args.get(i)),parentType));
        }
        sb.append(")");
        return sb.toString();
    }

    /** *************************************************************
     */
    private static String processLogOp(Formula f, Formula car, String parentType,
                                        ArrayList<String> args) {
        String op = car.getFormula();
        if (debug) System.out.println("processLogOp(): f: " + f);
        if (debug) System.out.println("processLogOp(): args: " + args);
        if (op.equals("and"))
            return processConjDisj(f,car,parentType,args);
        if (op.equals("=>")) {
            if (args.size() != 3) {  // op + 2 args
                System.out.println("Error in SUMOtoTFAform.processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return "(" + processRecurse(new Formula(args.get(1)),parentType) + " => " +
                        processRecurse(new Formula(args.get(2)),parentType) + ")";
        }
        if (op.equals("<=>")) {
            if (args.size() != 3) { // op + 2 args
                System.out.println("Error in SUMOtoTFAform.processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return "((" + processRecurse(new Formula(args.get(1)),parentType) + " => " +
                        processRecurse(new Formula(args.get(2)),parentType) + ") & (" +
                        processRecurse(new Formula(args.get(2)),parentType) + " => " +
                        processRecurse(new Formula(args.get(1)),parentType) + "))";
        }
        if (op.equals("or"))
            return processConjDisj(f,car,parentType,args);
        if (op.equals("not")) {
            if (args.size() != 2) { // op + 1 arg
                System.out.println("Error in SUMOtoTFAform.processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return "~(" + processRecurse(new Formula(args.get(1)),parentType) + ")";
        }
        if (op.equals("forall") || op.equals("exists"))
            return processQuant(f,car,parentType,op,args);
        System.out.println("Error in SUMOtoTFAform.processLogOp(): bad logical operator " + op + " in " + f);
        return "";
    }

    /** *************************************************************
     * @return whether the constant is a TFF built in numeric
     * type or a subclass of one of those types
     */
    private static boolean builtInOrSubNumericType(String arg) {

        if (StringUtil.emptyString(arg))
            return false;
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
     * @return whether the constant is a TFF built in numeric
     * type or a subclass of one of those types
     */
    private static boolean builtInNumericType(String arg) {

        if (StringUtil.emptyString(arg))
            return false;
        if (!arg.equals("RealNumber") &&
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
        for (String arg : argtypes) {
            if (!StringUtil.emptyString(arg) && !arg.equals("Integer") &&
                    !arg.equals("RationalNumber") && !arg.equals("RealNumber") &&
                    !builtInOrSubNumericType(arg)) {
                return false;
            }
        }
        return true;
    }

    /** *************************************************************
     */
    private static boolean allOfType(ArrayList<String> args, String type) {

        ArrayList<String> argtypes = collectArgTypes(args);
        for (String arg : argtypes) {
            if (!StringUtil.emptyString(arg) && !arg.equals(type)) {
                return false;
            }
        }
        return true;
    }

    /** *************************************************************
     * Specify the TFF types in the name of a predicate, as given by
     * the SUMO types listed in @param argTypeMap
     */
    private static String makePredFromArgTypes(Formula car, ArrayList<String> argTypeMap) {

        String pred = car.getFormula();
        String arityCountStr = null;
        if (debug) System.out.println("SUMOtoTFAform.makePredFromArgTypes(): car: " + car);
        if (!hasNumeric(argTypeMap))
            return car.getFormula();
        ArrayList<String> newArgTypeMap = new ArrayList<>();
        newArgTypeMap.addAll(argTypeMap);
        int index = 0;
        String suffix = "";
        if (kb.isFunction(car.getFormula())) {
            if (debug) System.out.println("SUMOtoTFAform.makePredFromArgTypes(): " + car + " is a Function: ");
            //newArgTypeMap.set(0, kb.kbCache.getRange(car.getFormula()));
            suffix = "Fn";
        }
        else {
            if (debug) System.out.println("SUMOtoTFAform.makePredFromArgTypes(): " + car + "is not a Function: ");
            index = 1;
            newArgTypeMap.set(0,"");
        }
        ArrayList<String> predTypes = kb.kbCache.getSignature(car.getFormula());
        ArrayList<String> types = mostSpecificSignature(predTypes,newArgTypeMap);
        String mostSpecific = mostSpecificType(types);
        if (debug) System.out.println("SUMOtoTFAform.makePredFromArgTypes(): isEqualTypeOp(pred): " + isEqualTypeOp(pred));
        if (debug) System.out.println("SUMOtoTFAform.makePredFromArgTypes(): mostSpecific: " + mostSpecific);
        if (isEqualTypeOp(pred))
            types = constrainArgs(mostSpecific,types);
        if (debug) System.out.println("SUMOtoTFAform.makePredFromArgTypes(): types: " + types);
        if (KButilities.isVariableArity(kb,pred)) {
            arityCountStr = Integer.toString(types.size()-1);
            if (kb.isFunction(pred))
                arityCountStr = arityCountStr + "Fn";
        }
        if (SUMOKBtoTFAKB.alreadyExtended(pred))
            return pred;
        StringBuffer result = new StringBuffer();
        result.append(car.getFormula() + "__");
        if (arityCountStr != null)
            result.append(arityCountStr + "__");
        for (int i = index; i < types.size(); i++) {
            String type = types.get(i);
            String twoType = "En";
            if (type.equals("Integer") || kb.isSubclass(type,"Integer"))
                twoType = "In";
            else if (type.equals("RealNumber") || kb.isSubclass(type,"RealNumber"))
                twoType = "Re";
            else if (type.equals("RationalNumber") || kb.isSubclass(type,"RationalNumber"))
                twoType = "Ra";
            result.append(i + twoType);
        }
        if (debug) System.out.println("SUMOtoTFAform.makePredFromArgTypes(): result: " + result.toString() + suffix);
        return result.toString() + suffix;
    }

    /** *************************************************************
     * Generate the TFF relation corresponding to an appearance of
     * any superclass of the TFF number types by naming the
     * type-specific relations using makePredFromArgTypes()
     */
    private static String processNumericSuperArgs(Formula f, Formula car,
                                                  String parentType,
                                                  ArrayList<String> args,
                                                  ArrayList<String> argTypes) {

        if (debug) System.out.println("SUMOtoTFAform.processNumericSuperArgs(): f: " + f);
        if (debug) System.out.println("SUMOtoTFAform.processNumericSuperArgs(): car: " + car);
        if (debug) System.out.println("SUMOtoTFAform.processNumericSuperArgs(): parentType: " + parentType);
        if (debug) System.out.println("SUMOtoTFAform.processNumericSuperArgs(): argTypes: " + argTypes);
        ArrayList<String> newArgTypes = new ArrayList<>();
        newArgTypes.addAll(argTypes);
        if (kb.isFunctional(f) && argTypes != null)
            newArgTypes.set(0,bestOfPair(parentType,argTypes.get(0)));
        ArrayList<String> predTypes = kb.kbCache.getSignature(car.getFormula());
        newArgTypes = bestSignature(predTypes,newArgTypes);
        if (debug) System.out.println("SUMOtoTFAform.processNumericSuperArgs(): newArgTypes: " + newArgTypes);
        String pred = car.getFormula();
        ArrayList<String> sig = kb.kbCache.getSignature(pred);
        if (!equalTFFsig(newArgTypes,sig,pred) || KButilities.isVariableArity(kb,pred))
            pred = makePredFromArgTypes(car,newArgTypes);
        if (debug) System.out.println("SUMOtoTFAform.processNumericSuperArgs(): pred: " + pred);
        ArrayList<String> processedArgs = new ArrayList<>();
        for (int i = 1; i < args.size(); i++) {
            String argType = "Entity";
            if (i < newArgTypes.size())
                argType = newArgTypes.get(i);
            else
                System.out.println("Error in SUMOtoTFAform.processNumericSuperArgs(): type list and arg list different size for " + f);
            processedArgs.add(processRecurse(new Formula(args.get(i)), argType));
        }
        StringBuffer result = new StringBuffer();
        result.append("s__" + pred + "(");
        for (String arg : processedArgs)
            result.append(arg + ",");
        result.deleteCharAt(result.length()-1);
        result.append(")");
        if (debug) System.out.println("SUMOtoTFAform.processNumericSuperArgs(): result: " + result);
        return result.toString();
    }

    /** *************************************************************
     * Generate the TFF function corresponding to an appearance of
     * SUMO's ListFn, naming the type-specific ListFn using
     * makePredFromArgTypes()
     * @param parentType the type that applies to this formula from any
     *                   enclosing formula content
     * @param args are all the arguments including ListFn, the arg 0
     * @param argTypes types of all arguments including arg 0
     */
    private static String processListFn(Formula f, Formula car,
                                        String parentType,
                                        ArrayList<String> args,
                                        ArrayList<String> argTypes) {

        if (debug) System.out.println("SUMOtoTFAform.processListFn(): f: " + f);
        String pred = car.getFormula();
        ArrayList<String> sig = kb.kbCache.getSignature(pred);
        if (!equalTFFsig(argTypes,sig,pred) || KButilities.isVariableArity(kb,pred))
            pred = makePredFromArgTypes(car,argTypes);
        ArrayList<String> processedArgs = new ArrayList<>();
        for (int i = 1; i < args.size(); i++) // arg 0 is ListFn
            processedArgs.add(processRecurse(new Formula(args.get(i)),parentType));
        StringBuffer result = new StringBuffer();
        result.append("s__" + pred + "(");
        for (String arg : processedArgs)
            result.append(arg + ",");
        result.deleteCharAt(result.length()-1);
        result.append(")");
        return result.toString();
    }

    /** *************************************************************
     * equal is a special case since it needs translation to '='
     * regardless of argument types since it's polymorphic on $i also.
     * @param args includes the relation as arg 0
     *
     */
    private static String processCompOp(Formula f, Formula car,
                                        ArrayList<String> args,
                                        ArrayList<String> argTypes) {

        String op = car.getFormula();
        if (debug) System.out.println("SUMOtoTFAform.processCompOp(): f: " + f);
        if (args.size() != 3) {
            System.out.println("Error in SUMOtoTFAform.processCompOp(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        Formula lhs = new Formula(args.get(1));
        Formula rhs = new Formula(args.get(2));
        String best = "Entity";
        if (argTypes != null && argTypes.size() > 2)
            best = bestOfPair(argTypes.get(1),argTypes.get(2));
        if (debug) System.out.println("SUMOtoTFAform.processCompOp(): best of argtypes: " + best);
        if (lhs.isVariable()) {
            if (debug) System.out.println("SUMOtoTFAform.processCompOp(): lhs: " + lhs);
            String bestVar = bestSpecificTerm(varmap.get(lhs.getFormula()));
            if (debug) System.out.println("SUMOtoTFAform.processCompOp(): bestVar: " + bestVar);
            best = bestOfPair(bestVar,best);
            if (debug) System.out.println("SUMOtoTFAform.processCompOp(): bestOfPair: " + best);
        }
        if (rhs.isVariable()) {
            String bestVar = bestSpecificTerm(varmap.get(rhs.getFormula()));
            best = bestOfPair(bestVar,best);
        }
        if (debug) System.out.println("SUMOtoTFAform.processCompOp(): builtInNumericType(best): " + builtInNumericType(best));
        if (debug) System.out.println("SUMOtoTFAform.processCompOp(): rhs is term: " + rhs + " : " + Formula.isTerm(rhs.getFormula()));
        if (Formula.isTerm(rhs.getFormula())) {
            if (builtInNumericType(best)) {
                if (debug) System.out.println("SUMOtoTFAform.processCompOp(): found constant: " + rhs + " with type " + best);
                numericConstantTypes.put(rhs.getFormula(), best);
            }
        }
        if (debug) System.out.println("SUMOtoTFAform.processCompOp(): lhs is term: " + lhs + " : " + Formula.isTerm(lhs.getFormula()));
        if (Formula.isTerm(lhs.getFormula())) {
            if (builtInNumericType(best)) {
                if (debug) System.out.println("SUMOtoTFAform.processCompOp(): found constant: " + lhs + " with type " + best);
                numericConstantTypes.put(lhs.getFormula(), best);
            }
        }
        if (debug) System.out.println("SUMOtoTFAform.processCompOp(): final best: " + best);
        if (debug) System.out.println("SUMOtoTFAform.processCompOp(): args: " + args);
        if (!op.startsWith("lessThan") && !op.startsWith("greaterThan") && !op.startsWith("equal")) {
            System.out.println("Error in SUMOtoTFAform.processCompOp(): bad comparison operator " + op + " in " + f);
            return "";
        }
        String lhsResult = processRecurse(lhs,best);
        if (debug) System.out.println("SUMOtoTFAform.processCompOp(): lhsResult: " + lhsResult);
        String rhsResult = processRecurse(rhs,best);
        if (debug) System.out.println("SUMOtoTFAform.processCompOp(): rhsResult: " + rhsResult);
        String comparator = "";
        String result = "";
        if (op.startsWith("equal")) {
            result = lhsResult + " = " + rhsResult;
            if (debug) System.out.println("SUMOtoTFAform.processCompOp(): result: " + result);
            return result;
        }
        if (op.startsWith("greaterThanOrEqualTo"))
            comparator = "$greatereq(" ;
        else if (op.startsWith("greaterThan"))
            comparator = "$greater(" ;
        else if (op.startsWith("lessThanOrEqualTo"))
            comparator = "$lesseq(";
        else if (op.startsWith("lessThan"))
            comparator = "$less(";
        result = comparator + lhsResult + "," + rhsResult + ")";
        if (debug) System.out.println("SUMOtoTFAform.processCompOp(): result: " + result);
        return result;
    }

    /** *************************************************************
     * When the arguments to quotient are mixed, promote the more
     * specific type to the more general type
     */
    private static String mixedQuotient(Formula f, String op,
                                        String parentType,
                                        ArrayList<String> args,
                                        ArrayList<String> argTypes) {

        if (debug) System.out.println("SUMOtoTFAform.mixedQuotient(): f: " + f);
        if (debug) System.out.println("SUMOtoTFAform.mixedQuotient(): argTypes: " + argTypes);
        String mgt = "Entity";
        if (argTypes != null && argTypes.size() > 2)
            mgt = bestOfPair(argTypes.get(1),argTypes.get(2));
        if (debug) System.out.println("SUMOtoTFAform.mixedQuotient(): most general: " + mgt);
        String promote = "";
        Formula lhs = new Formula(args.get(1));
        Formula rhs = new Formula(args.get(2));
        String eSuffix = "_e";
        if (mgt != null) {
            if (mgt.equals("RealNumber")) {
                promote = "$to_real";
                eSuffix = "";
            }
            if (mgt.equals("RationalNumber")) {
                promote = "$to_rat";
                eSuffix = "";
            }
        }
        if (promote == "") {
            System.out.println("Error in mixedQuotient() with arg " + args +
                    " and types " + argTypes);
            return "$quotient" + eSuffix + "(" + processRecurse(lhs,parentType) + " ," +
                    processRecurse(rhs,parentType) + ")";
        }
        else
            return "$quotient" + eSuffix + "(" + promote + "(" + processRecurse(lhs,parentType) + ") ," +
                    promote + "(" + processRecurse(rhs,parentType) + "))";
    }

    /** *************************************************************
     *  PLUSFN, MINUSFN, TIMESFN, DIVIDEFN, FLOORFN
     */
    private static String processMathOp(Formula f, Formula car,
                                        String parentType,
                                        ArrayList<String> args,
                                        ArrayList<String> argTypes) {

        String op = car.getFormula();
        ArrayList<String> predType = relationExtractSigFromName(op);
        ArrayList<String> sig = kb.kbCache.getSignature(op);
        String best = mostGeneralNumericType(sig);
        if (debug) System.out.println("SUMOtoTFAform.processMathOp(): f: " + f);
        if (debug) System.out.println("SUMOtoTFAform.processMathOp(): sig: " + sig);
        if (debug) System.out.println("SUMOtoTFAform.processMathOp(): best: " + best);
        if (!op.startsWith("FloorFn") && args.size() != 3) {
            System.out.println("Error in SUMOtoTFAform.processMathOp(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        if (op.startsWith("FloorFn") && args.size() != 2) {
            System.out.println("Error in SUMOtoTFAform.processMathOp(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        Formula arg1 = new Formula(args.get(1));
        String arg1Type = best;
        String promotion = numTypePromotion(f,parentType);
        String closeP = "";
        if (promotion != null)
            closeP = ")";
        else
            promotion = "";
        if (op.startsWith("FloorFn"))
            return promotion + "$floor(" + processRecurse(arg1, arg1Type) + ")" + closeP;
        Formula arg2 = new Formula(args.get(2));
        String arg2Type = best;
        if (debug) System.out.println("SUMOtoTFAform.processMathOp(): arg1Type: " + arg1Type);
        if (debug) System.out.println("SUMOtoTFAform.processMathOp(): arg2Type: " + arg2Type);
        if (op.startsWith("AdditionFn"))
            return promotion + "$sum(" + processRecurse(arg1,arg1Type) + " ," +
                    processRecurse(arg2,arg2Type) + ")" + closeP;
        if (op.startsWith("SubtractionFn"))
            return promotion + "$difference(" + processRecurse(arg1,arg1Type) + " ," +
                    processRecurse(arg2,arg2Type) + ")" + closeP;
        if (op.startsWith("MultiplicationFn"))
            return promotion + "$product(" + processRecurse(arg1,arg1Type) + " ," +
                    processRecurse(arg2,arg2Type) + ")" + closeP;
        if (op.startsWith("DivisionFn")) {
            if (allOfType(args,"Integer"))
                return promotion + "$quotient_e(" + processRecurse(arg1,"Integer") + " ," +
                        processRecurse(arg2,"Integer") + ")" + closeP;
            else {
                return mixedQuotient(f,op,parentType,args,argTypes);
            }
        }
        System.out.println("Error in SUMOtoTFAform.processMathOp(): bad math operator " + op + " in " + f);
        return "";
    }

    /** *************************************************************
     * @param f is the formula
     * @param car is the car of the formula, which is usually the predicate name
     * @param parentType is the type restriction that any enclosing formula imposes on this present formula
     * @param args are the arguments in this formula, not including the predicate position (arg 0)
     * @param argTypes are the types of the arguments
     */
    private static String processOtherRelation(Formula f, Formula car,
                                               String parentType,
                                               ArrayList<String> args,
                                               ArrayList<String> argTypes) {

        String op = car.getFormula();
        if (debug) System.out.println("SUMOtoTFAform.processOtherRelation(): f: " + f);
        if (debug) System.out.println("SUMOtoTFAform.processOtherRelation(): parentType: " + parentType);
        if (debug) System.out.println("SUMOtoTFAform.processOtherRelation(): argTypes: " + argTypes);
        ArrayList<String> newArgTypes = new ArrayList<>();
        ArrayList<String> sig = kb.kbCache.getSignature(op);
        if (debug) System.out.println("SUMOtoTFAform.processOtherRelation(): sig: " + sig);
        newArgTypes.addAll(argTypes);
        if (!SUMOKBtoTFAKB.alreadyExtended(op) && kb.isFunctional(f) && argTypes.size() > 0 &&
                kb.isSubclass("RealNumber",argTypes.get(0))) {
            String best = bestOfPair(argTypes.get(0),parentType);
            newArgTypes.set(0,best);
            if (!equalTFFsig(newArgTypes,sig,op) || KButilities.isVariableArity(kb,op)) // only add the suffix if arg types are different from the original sort of the predicate
                op = makePredFromArgTypes(car,newArgTypes);
        }
        ArrayList<String> predTypes = kb.kbCache.getSignature(car.getFormula());
        newArgTypes = bestSignature(predTypes,newArgTypes);
        if (debug) System.out.println("SUMOtoTFAform.processOtherRelation(): newArgTypes: " + newArgTypes);
        StringBuffer argStr = new StringBuffer();
        for (int i = 1; i < args.size(); i++) {  // skip the predicate (0th arg)
            String s = args.get(i);
            String type = "Entity";
            if (i < newArgTypes.size())
                type = newArgTypes.get(i);
            if (car.getFormula().startsWith("instance")) {
                if (Formula.isTerm(s) && i == 1) {
                    if (args.size() > 1 && builtInOrSubNumericType(args.get(2))) {
                        if (debug) System.out.println("SUMOtoTFAform.processOtherRelation(): found constant: " + s + " with type " + args.get(2));
                        numericConstantTypes.put(s, args.get(2));
                    }
                }
                int ttype = f.getFormula().charAt(0);
                if (Character.isDigit(ttype))
                    ttype = StreamTokenizer_s.TT_NUMBER;
                if (Formula.atom(s))
                    argStr.append(SUMOformulaToTPTPformula.translateWord(s,ttype,false) + ", ");
                else
                    argStr.append(processRecurse(new Formula(s),type) + ", ");
            }
            else
                argStr.append(processRecurse(new Formula(s),type) + ", ");
        }
        String promotion = numTypePromotion(f,parentType);
        String closeP = "";
        if (promotion != null)
            closeP = ")";
        else
            promotion = "";
        String result = promotion + "s__" + op + "(" + argStr.substring(0,argStr.length()-2) + ")" + closeP;
        if (debug) System.out.println("SUMOtoTFAform.processOtherRelation(): result: " + result);
        return result;
    }

    /** *************************************************************
     * The arg type list requires expansion since it contains a superclass
     * of Integer, and therefore could be used with more than one
     * TF) numerical type.
     */
    public static boolean hasNumericSuper(ArrayList<String> argTypes) {

        for (String s : argTypes)
            if (!kb.isSubclass("Integer",s) && !s.equals("Integer") && kb.isSubclass(s,"RealNumber"))
                return true;
        return false;
    }

    /** *************************************************************
     * @return the TF0-relevant type of the term whether it's a literal
     * number, variable with a numeric type or function
     */
    public static String findType(Formula f) {

        if (f == null)
            return "Entity";
        String form = f.getFormula();
        //if (debug) System.out.println("SUMOtoTFAform.findType(): f: " + f);
        //if (debug) System.out.println("SUMOtoTFAform.findType(): StringUtil.emptyString(form): " + StringUtil.emptyString(form));
        //if (debug) System.out.println("SUMOtoTFAform.findType(): StringUtil.isInteger(form): " + StringUtil.isInteger(form));
        //if (debug) System.out.println("SUMOtoTFAform.findType(): StringUtil.isNumeric(form): " + StringUtil.isNumeric(form));
        //if (debug) System.out.println("SUMOtoTFAform.findType(): f.isVariable(): " + f.isVariable());
        //if (debug) System.out.println("SUMOtoTFAform.findType(): kb.isFunction(f.getFormula()): " + kb.isFunction(f.getFormula()));
        //if (debug) System.out.println("SUMOtoTFAform.findType(): kb.isFunctional(f): " + kb.isFunctional(f));
        if (StringUtil.emptyString(form))
            return "Entity";
        if (StringUtil.isInteger(form))
            return "Integer";
        if (StringUtil.isNumeric(form)) // number but not an int
            return "RealNumber";
        if (f.isVariable()) {
            Set<String> vartypes = varmap.get(f.getFormula());
            return bestSpecificTerm(vartypes);
        }
        if (kb.isFunction(f.getFormula())) {
            String type = kb.kbCache.getRange(f.getFormula());
            return type;
        }
        if (kb.isFunctional(f)) {
            String type = kb.kbCache.getRange(f.carAsFormula().getFormula());
            //if (debug) System.out.println("SUMOtoTFAform.findType(): f: " + f);
            //if (debug) System.out.println("SUMOtoTFAform.findType(): type: " + type);
            return type;
        }
        return "Entity";
    }

    /** *************************************************************
     * a number, a variable with a numeric type or a function symbol
     * or function with a numeric type
     */
    public static boolean isNumeric(Formula f) {

        //if (debug) System.out.println("SUMOtoTFAform.isNumeric(): f: " + f);
        String type = findType(f);
        if (!StringUtil.emptyString(type) && (kb.isSubclass(type, "RealNumber") || type.equals("RealNumber")))
            return true;
        return false;
    }

    /** *************************************************************
     */
    public static boolean isNumericType(String s) {

        if (!StringUtil.emptyString(s) && (kb.isSubclass(s, "RealNumber") || s.equals("RealNumber")))
            return true;
        return false;
    }

    /** *************************************************************
     */
    public static boolean isBuiltInNumericType(String s) {

        if (!StringUtil.emptyString(s) && (s.equals("Integer") ||
                s.equals("RealNumber") || s.equals("RationalNumber")))
            return true;
        return false;
    }

    /** *************************************************************
     * Check if at least one of the types in the list is a numeric type
     */
    public static boolean hasNumeric(ArrayList<String> argTypes) {

        for (String s : argTypes)
            if (kb.isSubclass(s,"RealNumber") || s.equals("RealNumber"))
                return true;
        return false;
    }

    /** *************************************************************
     * Check if type signatures of SUMO would be equivalent TFF signatures.
     * @param pred is used just to give a meaningful error message
     */
    public static boolean equalTFFsig(ArrayList<String> argTypes1,
                                      ArrayList<String> argTypes2, String pred) {

        if (argTypes1 == null || argTypes2 == null ||
            argTypes1.size() != argTypes2.size()) {
            if ((argTypes1 == null || argTypes2 == null) && pred != null && !pred.startsWith("equal"))
                System.out.println("Error in equalTFFsig(): bad signatures " +
                    argTypes1 + ", " + argTypes2 + " for " + pred);
            return false;
        }
        for (int i = 0; i < argTypes1.size(); i++) {
            String arg1 = SUMOKBtoTFAKB.translateSort(kb,argTypes1.get(i));
            String arg2 = SUMOKBtoTFAKB.translateSort(kb,argTypes2.get(i));
            if (!arg1.equals(arg2))
                return false;
        }
        return true;
    }

    /** *************************************************************
     * if the formula is a numeric atom or variable or a function with
     * a number range and that type is more specific that the parentType,
     * promote it with $to_real or &to_rat
     * @return promotion function call with learning paren, or null otherwise
     */
    public static String numTypePromotion (Formula f, String parentType) {

        if (debug) System.out.println("SUMOtoTFAform.numTypePromotion(): f: " + f);
        if (debug) System.out.println("SUMOtoTFAform.numTypePromotion(): parentType: " + parentType);
        if (debug) System.out.println("SUMOtoTFAform.numTypePromotion(): isNumeric(f): " + isNumeric(f));
        if (debug) System.out.println("SUMOtoTFAform.numTypePromotion(): isNumericType(parentType): " + isNumericType(parentType));
        if (findType(f) != null)
            if (debug) System.out.println("SUMOtoTFAform.numTypePromotion(): kb.compareTermDepth(findType(f),parentType): " + kb.compareTermDepth(findType(f),parentType));
        if (isNumeric(f) && isNumericType(parentType) && findType(f) != null &&
                kb.compareTermDepth(findType(f),parentType) > 0) {
            if (debug) System.out.println("SUMOtoTFAform.numTypePromotion(): promoting");
            if (parentType.equals("RealNumber"))
                return "$to_real(";
            if (parentType.equals("RationalNumber"))
                return "$to_rat(";
        }
        return null;
    }

    /** *************************************************************
     * process a formula into TF0
     * @param f the formula to process
     * @param parentType is the type restriction that applies to this
     *                   formula, if it's part of a larger formula
     */
    public static String processRecurse(Formula f, String parentType) {

        if (debug) System.out.println("SUMOtoTFAform.processRecurse(): f: " + f);
        if (debug) System.out.println("SUMOtoTFAform.processRecurse(): varmap: " + varmap);
        if (debug) System.out.println("SUMOtoTFAform.processRecurse(): parentType: " + parentType);
        if (f == null)
            return "";
        if (f.atom()) {
            int ttype = f.getFormula().charAt(0);
            if (Character.isDigit(ttype))
                ttype = StreamTokenizer_s.TT_NUMBER;
            String promotion = numTypePromotion(f,parentType);
            if (promotion != null)
                return promotion + SUMOformulaToTPTPformula.translateWord(f.getFormula(),ttype,false) + ")";
            else
                return SUMOformulaToTPTPformula.translateWord(f.getFormula(),ttype,false);
        }
        Formula car = f.carAsFormula();
        String op = car.getFormula();
        if (debug) System.out.println("SUMOtoTFAform.processRecurse(): op: " + op);

        ArrayList<String> args = f.complexArgumentsToArrayListString(0);
          // in order to handle functions and their types, always include the relation in arg list
        ArrayList<String> argTypes = collectArgTypes(args);
        if (debug) System.out.println("SUMOtoTFAform.processRecurse(): argTypes: " + argTypes);
        ArrayList<String> argsFromSig = relationExtractSigFromName(op);
        if (debug) System.out.println("SUMOtoTFAform.processRecurse(): argsFromSig: " + argsFromSig);
        argTypes = bestSignature(argTypes,argsFromSig);
        if (Formula.listP(op)) {
            System.out.println("Error in SUMOtoTFAform.processRecurse(): formula " + f);
            return "";
        }
        if (Formula.isLogicalOperator(op)) // UQUANT, EQUANT, AND, OR, NOT, IF, IFF
            return processLogOp(f,car,parentType,args);
        else if (isComparisonOperator(op)) //EQUAL, GT, GTET, LT, LTET
            return processCompOp(f,car,args,argTypes);
        else if (isMathFunction(car.getFormula())) //  PLUSFN, MINUSFN, TIMESFN, DIVIDEFN, FLOORFN
            return processMathOp(f,car,parentType,args,argTypes);
        else if (car.getFormula().startsWith("ListFn"))
            return processListFn(f,car,parentType,args,argTypes);
        else if (hasNumericSuper(argTypes))
            return processNumericSuperArgs(f,car,parentType,args,argTypes);
        else
            return processOtherRelation(f,car,parentType,args,argTypes);
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
     * check if t is one of the fundamental types of $int, $rat, $real
     * or SUMO types that are subtypes of Integer, RationalNumber or
     * RealNumber and return the TFF type
     */
    private static String convertToTFFType(String t) {

        if (t.equals("Integer") && kb.isSubclass(t,"Integer"))
            return "$int";
        if (t.equals("RealNumber") && kb.isSubclass(t,"RealNumber"))
            return "$real";
        if (t.equals("RationalNumber") && kb.isSubclass(t,"RationalNumber"))
            return "$rat";
        return "$i";
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
     * @param op is the relation
     * @param t is the type of the actual argument to op
     * @param sigType is the type required for this argument to op
     */
    private static String numberSuffix(String suf, int arg, String op, String t, String sigType) {

        if (StringUtil.emptyString(t) || op.equals("instance")) // || t.equals(sigType))
            return suf;
        String suffix = "";
        if (kb.isSubclass(t, sigType) && kb.isSubclass(t, "Number")) {
            if (kb.isSubclass(t, "Integer") || t.equals("Integer"))
                suffix = "In";
            else if (kb.isSubclass(t, "RealNumber") || t.equals("RealNumber"))
                suffix = "Re";
            else if (kb.isSubclass(t, "RationalNumber") || t.equals("RationalNumber"))
                suffix = "Ra";
        }
        else
            suffix = "En";
        if (suffix.equals(""))
            return suf;
        return suf + Integer.toString(arg) + suffix;
    }

    /** *************************************************************
     * Find the types of each argument.  If a variable, look up in
     * varmap and if a numeric type, use the most specific type.
     * If a function, check its return type.
     */
    private static ArrayList<String> collectArgTypes(ArrayList<String> args) {

        if (debug) System.out.println("SUMOtoTFAform.collectArgTypes(): varmap: " + varmap);
        ArrayList<String> types = new ArrayList<String>();
        if (args == null)
            return types;
        for (String s : args) {
            if (Formula.isVariable(s)) {
                String vtype = kb.mostSpecificTerm(varmap.get(s));
                if (!StringUtil.emptyString(vtype))
                    types.add(vtype);
            }
            else if (StringUtil.isInteger(s))
                types.add("Integer");
            else if (StringUtil.isNumeric(s) && !StringUtil.isInteger(s))
                types.add("RealNumber");
            else if (Formula.listP(s)) {
                Formula newf = new Formula(s);
                if (kb.isFunctional(newf)) {
                    String op = newf.car();
                    String range = kb.kbCache.getRange(op);
                    if (!StringUtil.emptyString(range))
                        types.add(range);
                }
            }
            else if (kb.isInstance(s)) {
                if (kb.isRelation(s))
                    types.add("Entity");
                else if (kb.isFunction(s)) {
                    String range = "Entity";
                    if (kb.kbCache.getRange(s) != null)
                        range = kb.kbCache.getRange(s);
                    types.add(range);
                }
                else {
                    HashSet<String> p = kb.immediateParents(s);
                    String most = kb.mostSpecificTerm(p);
                    if (!StringUtil.emptyString(most))
                        types.add(most);
                }
            }
            else
                types.add("");
        }
        if (debug) System.out.println("SUMOtoTFAform.collectArgTypes(): return: " + types);
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
     * Constrain a list of arguments to be the best type
     */
    private static void constrainVars(ArrayList<String> argTypes, ArrayList<String> args) {

        if (debug) System.out.println("SUMOtoTFAform.constrainVars(): argTypes, args: " + argTypes + ", " + args);
        for (int i = 0; i < args.size(); i++) {
            String t = args.get(i);
            if (!Formula.isVariable(t) || i >= argTypes.size())
                continue;
            String type = argTypes.get(i);
            if (!kb.isSubclass(type,"Quantity"))
                System.out.println("Error in SUMOtoTFAform.constrainVars(): non numeric type: " + type);
            HashSet<String> types = varmap.get(t);
            if (debug) System.out.println("SUMOtoTFAform.constrainVars(): checking var " + t + " with type " + types);
            String best = bestSpecificTerm(types);
            if (debug) System.out.println("SUMOtoTFAform.constrainVars(): type " + type + " best " + best);
            if (best == null || bestOfPair(best,type).equals(type)) {
                if (best == null)
                    types = new HashSet<>();
                types.add(type);
                if (debug) System.out.println("SUMOtoTFAform.constrainVars(): constraining " + t + " to " + type);
            }
        }
    }

    /** *************************************************************
     * Constrain a list of arguments to be a given type
     */
    private static ArrayList<String> constrainArgs(String type, ArrayList<String> argTypes) {

        ArrayList<String> result = new ArrayList<>();

        if (debug) System.out.println("SUMOtoTFAform.constrainArgs(): " + type);
        for (int i = 0; i < argTypes.size(); i++)
            result.add(type);
        return result;
    }

    /** *************************************************************
     * for numerical and comparison operators, ensure their arguments
     * are all the same type and that it's the most specific type.  If
     * no types are found in the suffix, return the empty string.
     */
    private static String leastNumericType(String op, String suffix) {

        ArrayList<String> args = relationExtractSigFromName("dummy__" + suffix);
        String least = "";
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i) != "") {
                if (args.get(i).equals("Integer") || args.get(i).equals("RealNumber") || args.get(i).equals("RationalNumber")) {
                    if (least == "")
                        least = args.get(i);
                    else if (kb.isSubclass(least, args.get(i)))
                        least = args.get(i);
                }
            }
        }
        if (StringUtil.emptyString(least))
            return "";
        StringBuffer result = new StringBuffer();
        int start = 0;
        if (!kb.isFunction(op))
            start = 1;
        for (int i = start; i < args.size(); i++) {
            result.append(i + least.substring(0, 2));
        }
        return result.toString();
    }

    /** *************************************************************
     * for numerical and comparison operators, ensure their arguments
     * are all the same type and that it's the most general numeric type.  If
     * no types are found, return the empty string.  For
     * example equal__1En2Ra return equal__1Ra2Ra or for
     * equal__1Re2In return equal__1Re2Re
     * @return the most general numeric type
     */
    private static String mostGeneralNumericType(ArrayList<String> args) {

        if (args == null)
            return "Entity";
        String greatest = "";
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (isNumericType(arg) && !isBuiltInNumericType(arg))
                arg = promoteToBuiltIn(arg);
            if (arg != "") {
                if (isBuiltInNumericType(arg)) {
                    if (greatest == "" || greatest.equals("Entity"))
                        greatest = arg;
                    else if (kb.isSubclass(greatest, arg))
                        greatest = arg;
                }
            }
        }
        if (StringUtil.emptyString(greatest))
            return "";
        return greatest;
    }

    /** *************************************************************
     * if the operator already has a suffix, revise it with the new
     * suffix in the case where the new argument type is more
     * specific
     */
    protected static String composeSuffix(String op, String suffix) {

        if (StringUtil.emptyString(op) || StringUtil.emptyString(suffix) || SUMOKBtoTFAKB.alreadyExtended(op))
            return op;

        int under = op.indexOf("__");
        if (under == -1 || SUMOKBtoTFAKB.alreadyExtended(op))
            return op + suffix;
        String justOp = op.substring(0,under);
        ArrayList<String> newTypes = relationExtractSigFromName("dummy" + suffix);
        ArrayList<String> oldTypes = relationExtractSigFromName(op);
        ArrayList<String> bestSig = bestSignature(newTypes,oldTypes);
        ArrayList<String> sig = kb.kbCache.getSignature(justOp);
        if (!equalTFFsig(bestSig,sig,justOp) || KButilities.isVariableArity(kb,justOp))
            justOp = makePredFromArgTypes(new Formula(justOp),bestSig);
        String result = justOp;
        if (debug) System.out.println("SUMOtoTFAform.composeSuffix(): " + result);
        return result;
    }

    /** *************************************************************
     * Create a specialized version of KB.mostSpecificTerm() that
     * biases the results for TFF.  Prefer a built-in numeric type
     * (equivalents to $int, $rat, $real) over any more specific
     * type in SUMO
     */
    public static String mostSpecificType(Collection<String> args) {

        if (args == null || args.size() < 1)
            return null;
        String result = "";
        for (String t : args) {
            String term = t;
            if (t.endsWith("+"))
                term = t.substring(0,t.length()-1);
            if (StringUtil.emptyString(term) || !kb.containsTerm(term)) {
                if (!StringUtil.emptyString(term))
                    System.out.println("Error in SUMOtoTFAform.mostSpecificTerm(): no such term: " + term);
                continue;
            }
            if (!isBuiltInNumericType(term) && isNumericType(term))
                term = promoteToBuiltIn(term);
            if (result == "")
                result = term;
            else if (kb.compareTermDepth(term,result) > 0) {
                result = term;
            }
        }
        return result;
    }

    /** *************************************************************
     * Promote type to the most specific number that is a TFF type or superclass
     */
    public static String promoteToBuiltIn(String t) {

        if (kb.isSubclass(t,"Integer"))
            return "Integer";
        if (kb.isSubclass(t,"RationalNumber"))
            return "RationalNumber";
        if (kb.isSubclass(t,"RealNumber"))
            return "RealNumber";
        return t;
    }

    /** *************************************************************
     * Pick the most specific number type that is a TFF type or superclass
     */
    public static String constrainPair(String t1, String t2) {

        if (t1.endsWith("+"))
            t1 = t1.substring(0, t1.length() - 1);
        if (t2.endsWith("+"))
            t2 = t2.substring(0, t2.length() - 1);
        if (StringUtil.emptyString(t1) || !kb.containsTerm(t1))
            return t2;
        if (StringUtil.emptyString(t2) || !kb.containsTerm(t2))
            return t1;
        //if (debug) System.out.println("SUMOtoTFAform.constrainPair(): t1: " + t1);
        //if (debug) System.out.println("SUMOtoTFAform.constrainPair(): builtInOrSubNumericType(t1): " + builtInOrSubNumericType(t1));
        //if (debug) System.out.println("SUMOtoTFAform.constrainPair(): builtInNumericType(t1): " + builtInNumericType(t1));

        //if (debug) System.out.println("SUMOtoTFAform.constrainPair(): t2: " + t2);
        //if (debug) System.out.println("SUMOtoTFAform.constrainPair(): builtInOrSubNumericType(t2): " + builtInOrSubNumericType(t2));
        //if (debug) System.out.println("SUMOtoTFAform.constrainPair(): builtInNumericType(t2): " + builtInNumericType(t2));
        //if (debug) System.out.println("SUMOtoTFAform.constrainPair(): depth of t1: " + kb.termDepth(t1));
        //if (debug) System.out.println("SUMOtoTFAform.constrainPair(): depth of t2: " + kb.termDepth(t2));
        String newt1 = "Entity";
        String newt2 = "Entity";
        if (builtInOrSubNumericType(t1) && !builtInNumericType(t1)) {
            //if (debug) System.out.println("SUMOtoTFAform.constrainPair(): promoting t1: " + t1);
            newt1 = promoteToBuiltIn(t1);
        }
        else if (builtInNumericType(t1))
            return t1;
        if (builtInOrSubNumericType(t2) && !builtInNumericType(t2)) {
            //if (debug) System.out.println("SUMOtoTFAform.constrainPair(): promoting t2: " + t2);
            newt2 = promoteToBuiltIn(t2);
        }
        else if (builtInNumericType(t2))
            return t2;
        //if (debug) System.out.println("SUMOtoTFAform.constrainPair(): newt1: " + newt1);
        //if (debug) System.out.println("SUMOtoTFAform.constrainPair(): newt2: " + newt2);
        if (kb.compareTermDepth(newt1, newt2) > 0)
            return newt1;
        else
            return newt2;
    }

    /** *************************************************************
     * Pick the most general number among two numbers or the most
     * specific term otherwise
     */
    public static String bestOfPair(String t1, String t2) {

        if (t1.endsWith("+"))
            t1 = t1.substring(0, t1.length() - 1);
        if (t2.endsWith("+"))
            t2 = t2.substring(0, t2.length() - 1);
        if (StringUtil.emptyString(t1) || !kb.containsTerm(t1))
            return t2;
        if (StringUtil.emptyString(t2) || !kb.containsTerm(t2))
            return t1;
        //if (debug) System.out.println("SUMOtoTFAform.bestOfPair(): t1: " + t1);
        //if (debug) System.out.println("SUMOtoTFAform.bestOfPair(): t2: " + t2);
        //if (debug) System.out.println("SUMOtoTFAform.bestOfPair(): depth of t1: " + kb.termDepth(t1));
        //if (debug) System.out.println("SUMOtoTFAform.bestOfPair(): depth of t2: " + kb.termDepth(t2));
        if (builtInOrSubNumericType(t1) && builtInOrSubNumericType(t2)) {
            if (kb.compareTermDepth(t1, t2) < 0)
                return t1;
            else
                return t2;
        }
        else {
            //if (debug) System.out.println("SUMOtoTFAform.bestOfPair(): not both numeric");
            if (kb.compareTermDepth(t1, t2) > 0)
                return t1;
            else
                return t2;
        }
    }

    /** *************************************************************
     * Constrain a type based on a second type.  If the second type is
     * built-in type and first is not, pick the built-in type.  If the
     * second type and first type are both built-in types, pick the
     * more specific.  If the second type is more
     * general than the first, throw an error.
     */
    public static String constrainTerm(String t1, String t2) {

        if (t1.endsWith("+"))
            t1 = t1.substring(0, t1.length() - 1);
        if (t2.endsWith("+"))
            t2 = t2.substring(0, t2.length() - 1);
        if (StringUtil.emptyString(t1) || !kb.containsTerm(t1))
            return t2;
        if (StringUtil.emptyString(t2) || !kb.containsTerm(t2))
            return t1;
        //if (debug) System.out.println("SUMOtoTFAform.constrainTerm(): t1: " + t1);
        //if (debug) System.out.println("SUMOtoTFAform.constrainTerm(): t2: " + t2);
        //if (debug) System.out.println("SUMOtoTFAform.constrainTerm(): depth of t1: " + kb.termDepth(t1));
        //if (debug) System.out.println("SUMOtoTFAform.constrainTerm(): depth of t2: " + kb.termDepth(t2));
        if (isNumericType(t1) && !isBuiltInNumericType(t1))
            t1 = promoteToBuiltIn(t1);
        if (isNumericType(t2) && !isBuiltInNumericType(t2))
            t2 = promoteToBuiltIn(t2);
        if (kb.compareTermDepth(t1, t2) > 0) {
            if (debug) System.out.println("Error SUMOtoTFAform.constrainTerm(): second type more general than first: " +
                    t1 + ", " + t2);
            return t1;
        }
        else
            return t2;
    }

    /** *************************************************************
     * Find the most specific TFF type or superclass at every argument position
     */
    public static ArrayList<String> mostSpecificSignature(ArrayList<String> args1,
                                                  ArrayList<String> args2 ) {

        //if (debug) System.out.println("SUMOtoTFAform.mostSpecificSignature(): args1 args2: " + args1 + " " + args2);
        if (args1 == null || args2 == null || args1.size() != args2.size()) {
            //if (debug) System.out.println("Error in mostSpecificSignature(): bad arguments: " + args1 + " " + args2);
            if (args1 == null)
                return args2;
            if (args2 == null)
                return args1;
            if (args1.size() > args2.size())
                return args1;
            else
                return args2;
        }
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < args1.size(); i++) {
            String arg1 = args1.get(i);
            String arg2 = args2.get(i);
            String constrained = constrainTerm(arg1,arg2);
            result.add(constrained);
        }
        //if (debug) System.out.println("SUMOtoTFAform.mostSpecificSignature(): result: " + result);
        return result;
    }

    /** *************************************************************
     * Find the best type at every argument position
     */
    public static ArrayList<String> bestSignature(ArrayList<String> args1,
                                                        ArrayList<String> args2 ) {

        if (debug) System.out.println("SUMOtoTFAform.bestSignature(): args1 args2: " + args1 + " " + args2);
        if (args1 == null || args2 == null || args1.size() != args2.size()) {
            if (debug) System.out.println("Error in bestSignature(): bad arguments: " + args1 + " " + args2);
            if (args1 == null)
                return args2;
            if (args2 == null)
                return args1;
            if (args1.size() > args2.size())
                return args1;
            else
                return args2;
        }
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < args1.size(); i++) {
            String arg1 = args1.get(i);
            String arg2 = args2.get(i);
            String best = bestOfPair(arg1,arg2);
            result.add(best);
        }
        return result;
    }

    /** *************************************************************
     * Create a specialized version of KB.mostSpecificTerm() that
     * biases the results for TFF.  Prefer the most specific type but
     * no subclasses of a built-in numeric type
     * (equivalents to $int, $rat, $real)
     */
    public static String mostSpecificTFFTerm(Collection<String> args) {

        if (args == null || args.size() < 1)
            return null;
        String result = "";
        for (String t : args) {
            String term = t;
            if (t.endsWith("+"))
                term = t.substring(0, t.length() - 1);
            if (StringUtil.emptyString(term) || !kb.containsTerm(term)) {
                if (!StringUtil.emptyString(term))
                    System.out.println("Error in SUMOtoTFAform.mostSpecificTFFTerm(): no such term: " + term);
                continue;
            }
            if (result == "")
                result = term;
            else
                result = bestOfPair(result,term);
        }
        //if (debug) System.out.println("SUMOtoTFAform.mostSpecificTFFTerm(): result: " + result);
        return result;
    }

    /** *************************************************************
     * Create a specialized version of KB.mostSpecificTerm() that
     * biases the results for TFF.  Prefer a built-in numeric type
     * (equivalents to $int, $rat, $real) over any more specific
     * type in SUMO.  Prefer the most general numeric type.
     */
    public static String bestSpecificTerm(Collection<String> args) {

        if (args == null || args.size() < 1)
            return null;
        String result = "";
        for (String t : args) {
            String term = t;
            if (t.endsWith("+"))
                term = t.substring(0, t.length() - 1);
            if (StringUtil.emptyString(term) || !kb.containsTerm(term)) {
                if (!StringUtil.emptyString(term))
                    System.out.println("Error in SUMOtoTFAform.bestSpecificTerm(): no such term: " + term);
                continue;
            }
            if (result == "")
                result = term;
            else {
                if (builtInOrSubNumericType(result) && builtInOrSubNumericType(term)) {
                    if (kb.compareTermDepth(term, result) < 0)
                        result = term;
                }
                else if (kb.compareTermDepth(term, result) > 0)
                    result = term;
            }
        }
        //if (debug) System.out.println("SUMOtoTFAform.bestSpecificTerm(): result: " + result);
        return result;
    }

    /** *************************************************************
     */
    private static String getOpReturnType(String arg) {

        if (StringUtil.emptyString(arg) || !Formula.listP(arg))
            return "Entity";
        Formula f = new Formula(arg);
        String op = f.car();
        if (!kb.containsTerm(op)) {
            ArrayList<String> sig = SUMOtoTFAform.relationExtractSigFromName(op);
            String ret = sig.get(0);
            if (!StringUtil.emptyString(ret))
                return ret;
            System.out.println("Error in SUMOtoTFAform.getOpReturnType(): no term: " + op);
            Thread.dumpStack();
            System.exit(0);
        }
        if (kb.isFunctional(f)) {
            String range = kb.kbCache.getRange(op);
            if (range != null)
                return range;
        }
        return "Entity";
    }

    /** *************************************************************
     * Given two lists of types, return the bigger list, or create a
     * new list from two of equal size that has the most specific type
     * at each index.
     */
    private static ArrayList<String> mostSpecificArgType(ArrayList<String> args1, ArrayList<String> args2) {

        if (args1 == null || args1.size() < 1)
            return args2;
        if (args2 == null || args2.size() < 1)
            return args1;
        ArrayList<String> result = new ArrayList<String>();
        int size = args1.size();
        if (args2.size() > size)
            size = args2.size();
        for (int i = 0; i < size; i++) {
            String type1 = "";
            if (args1.size() > i)
                type1 = args1.get(i);
            String type2 = "";
            if (args2.size() > i)
                type2 = args2.get(i);
            result.add(constrainTerm(type1,type2));
        }
        return result;
    }

    /** *************************************************************
     */
    private static void setAll(ArrayList<String> sig, String best) {

        for (int i = 0; i < sig.size(); i++)
            sig.set(i,best);
    }

    /** *************************************************************
     * @param f    is a formula that has an operator to constrain
     * @param args is a list of arguments of formula f, starting with
     *             an empty first argument for the relation
     * @param op   is the operator of formula f
     * @return the constrained formula
     */
    private static String constrainOp(Formula f, String op, String parentType, ArrayList<String> args) {

        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): f: " + f);
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): op: " + op);
        if (f == null)
            return "";
        if (f.atom()) {
            if (f.isVariable())
                MapUtils.addToMap(varmap,f.getFormula(),parentType);
            return f.getFormula();
        }
        if (args == null) {
            System.out.println("Error in SUMOtoTFAform.constrainOp(): null args in : " + f);
            return "";
        }
        // three possible signatures 1. types from name, 2. types from op signature, 3. types from arg types
        // pick the 'best' from them - most specific down to TF0 types (so Integer is better than PositiveInteger)

        String suffix = "";
        ArrayList<String> sig = kb.kbCache.getSignature(op);
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): op sig: " + sig);
        ArrayList<String> typeFromName = relationExtractSigFromName(op); // extract type signature from the operator name
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): typeFromName: " + typeFromName);
        ArrayList<String> newsig = mostSpecificSignature(sig,typeFromName);
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(1): newsig: " + newsig);
        //if (!kb.kbCache.signatures.containsKey(op))
        //    if (typeFromName != null)
        //        sig = typeFromName;
        String opType = "Entity";
        if (sig != null && sig.get(0) !="")
            opType = sig.get(0);
        ArrayList<String> argtypes = collectArgTypes(args);
        newsig = mostSpecificSignature(newsig,argtypes);
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): argtypes: " + argtypes);
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(2): newsig: " + newsig);
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): args: " + args);
        String mostSpecific = mostSpecificType(newsig);
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): mostSpecific: " + mostSpecific);
        if (isEqualTypeOp(op))
            constrainArgs(mostSpecific,newsig);
        if (kb.isFunction(op)) {
            if (opType.equals("Entity"))
            newsig.set(0,constrainTerm(opType,newsig.get(0)));
            newsig.set(0,constrainTerm(parentType,newsig.get(0)));
        }
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(3): newsig: " + newsig);
        for (int i = 1; i < args.size(); i++) {
            String arg = args.get(i);
            Formula argForm = new Formula(arg);
            String type = "Entity";
            if (i <= newsig.size()-1)
                type = newsig.get(i);
            args.set(i, constrainOp(argForm, argForm.car(), type, argForm.complexArgumentsToArrayListString(0)));
        }

        ArrayList<String> newargs = new ArrayList<>();
        newargs.addAll(args);
        newargs.remove(0);
        String composed = op;

        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): newsig: " + newsig);
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): sig: " + sig);
        String makePred = op;
        if (!equalTFFsig(newsig,sig,op) || KButilities.isVariableArity(kb,op))  // only add the suffix if arg types are different from the original sort of the predicate
            makePred = makePredFromArgTypes(new Formula(withoutSuffix(op)),newsig);
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): makePred: " + makePred);
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): op: " + op);
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): suffix: " + suffix);
        //if (isComparisonOperator(op) || op.startsWith("ListFn")  || isMathFunction(op))
        //    composed = composeSuffix(op, suffix);
        //String result = "(" + composed + " " + StringUtil.arrayListToSpacedString(newargs) + ")";
        String result = "(" + makePred + " " + StringUtil.arrayListToSpacedString(newargs) + ")";
        if (debug) System.out.println("SUMOtoTFAform.constrainOp(): result: " + result);
        return result;
    }

    /** *************************************************************
     */
    private static String constrainFunctVarsRecurse(Formula f, String parentType) {

        //if (debug) System.out.println("SUMOtoTFAform.constrainFunctVarsRecurse(): " + f);
        if (f == null) return "";
        if (f.atom()) return f.getFormula();
        Formula car = f.carAsFormula();
        if (car.isVariable())
            return f.getFormula();
        ArrayList<String> args = f.complexArgumentsToArrayListString(0);
        //if (debug) System.out.println("SUMOtoTFAform.constrainFunctVarsRecurse(): args: " + args);
        if (args == null)
            args = new ArrayList<>();
        if (car.listP()) {
            System.out.println("Error in SUMOtoTFAform.constrainFunctVarsRecurse(): formula " + f);
            return "";
        }
        String op = car.getFormula();
        if (!Formula.isLogicalOperator(op) && !Formula.isVariable(op))
            return constrainOp(f,op,parentType,args);
        else {
            ArrayList<String> sig = kb.kbCache.getSignature(op);
            StringBuffer resultString = new StringBuffer();
            resultString.append("(" + op);
            ArrayList<String> newargs = new ArrayList<>();
            newargs.addAll(args);
            for (int i = 1; i < newargs.size(); i++) {
                String s = newargs.get(i);
                String t = "Entity";
                if (sig != null && i < sig.size())
                    t = sig.get(i);
                resultString.append(" " + constrainFunctVarsRecurse(new Formula(s),t));
            }
            resultString.append(")");
            //if (debug) System.out.println("SUMOtoTFAform.constrainFunctVarsRecurse(): returning: " + resultString);
            return resultString.toString();
        }
    }

    /** *************************************************************
     * Only constrain the element of the varmap if the new type is more specific
     * @result is the new varmap as a side effect
     */
    private static void constrainTypeRestriction(HashMap<String,HashSet<String>> newvarmap) {

        //if (debug) System.out.println("SUMOtoTFAform.constrainTypeRestriction(): varmap: " + varmap);
        for (String k : newvarmap.keySet()) {
            HashSet<String> newvartypes = newvarmap.get(k);
            String newt = bestSpecificTerm(newvartypes);
            HashSet<String> oldvartypes = varmap.get(k);
            String oldt = bestSpecificTerm(oldvartypes);
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
        //if (debug) System.out.println("SUMOtoTFAform.constrainTypeRestriction(): new varmap: " + varmap);
    }

    /** *************************************************************
     * result is a side effect on varmap and the formula
     */
    private static Formula constrainFunctVars(Formula f) {

        int counter = 0;
        if (debug) System.out.println("SUMOtoTFAform.constrainFunctVars(): formula: " + f);
        HashMap<String,HashSet<String>> oldVarmap = null;
        do {
            counter++;
            oldVarmap = cloneVarmap();
            String newf = constrainFunctVarsRecurse(f,"Entity");
            f = new Formula(newf);
            HashMap<String,HashSet<String>> types = fp.findAllTypeRestrictions(f, kb);
            constrainTypeRestriction(types);
        } while (!varmap.equals(oldVarmap) && counter < 5);
        return f;
    }

    /** *************************************************************
     * Recursive routine to eliminate occurrences of '=>', 'and' and 'or' that
     * have only one or zero arguments
     * @return the corrected formula as a string
     */
    public static String elimUnitaryLogops(Formula f) {

        if (f.empty() || f.atom())
            return f.getFormula();
        ArrayList<String> args = f.complexArgumentsToArrayListString(0);
        if (args == null) return "";
        if (f.car().equals("and") || f.car().equals("or") || f.car().equals("=>")) {
            if (args.size() == 1) {  // meaning that the one "argument" is the predicate
                return "";
            }
            if (args.size() == 2) {
                String result = elimUnitaryLogops(new Formula(args.get(1)));
                return result;
            }
        }
        StringBuffer result = new StringBuffer();
        result = result.append("(" + args.get(0));
        for (int i = 1; i < args.size(); i++) {
            result.append(" " + elimUnitaryLogops(new Formula(args.get(i))));
        }
        result.append(")");
        if (!result.toString().equals(f.getFormula()))
            return elimUnitaryLogops(new Formula(result.toString())); // loop again if it's changed
        return result.toString();
    }

    /** *************************************************************
     * Substitute the values of numeric constants for their names.
     * Note that this is risky since it must be kept up to date
     * with the content of the knowledge base.  TODO: generalize this
     */
    public static Formula instantiateNumericConstants(Formula f) {

        List<String> constants = Arrays.asList("NumberE", "Pi");
        List<String> values = Arrays.asList("2.718282", "3.141592653589793");
        Set<String> terms = f.collectTerms();
        if (f.car().equals("instance")) {
            if (terms.contains(constants.get(0)) ||
                terms.contains(constants.get(1)))
                return new Formula();
            else
            return f;
        }
        for (int i = 0; i < constants.size(); i++) {
            String s = constants.get(i);
            String val = values.get(i);
            if (terms.contains(s))
                f = new Formula(f.rename(s,val).getFormula());
        }
        return f;
    }

    /** *************************************************************
     * When predicate variable substitution occurs it can result
     * in an argument to the predicate being defined as a particular
     * type.  If that type is numeric, it will conflict with TFF's
     * built in types and need to be removed, since the type
     * will be specified in the quantifer list, albeit with a
     * constraint from the numericConstraints list if the type
     * is not a TFF fundamental type of $int, $rat, or $real
     */
    public static String removeNumericInstance(String s) {

        if (StringUtil.emptyString(s))
            return s;
        if (Formula.atom(s))
            return s;
        if (!s.contains("instance"))
            return s;
        Formula f = new Formula(s);
        if (Formula.listP(s)) {
            if (f.isSimpleClause(kb)) {
                if (f.car().equals("instance")) {
                    ArrayList<String> al = f.complexArgumentsToArrayListString(0);
                    if (al.size() < 3) {
                        System.out.println("Error SUMOtoTFAform.removeNumericInstance(): wrong # or args to: " + f);
                        return s;
                    }
                    String arg = al.get(2);
                    String var = al.get(1);
                    if (var.equals("NumberE") || var.equals("Pi"))
                        return "";
                    if (arg.equals("RealNumber") || arg.equals("RationalNumber") || arg.equals("Integer"))
                        return "";
                    if (varmap.get(var) != null)
                        if (varmap.get(var).contains("RealNumber") || varmap.get(var).contains("RationalNumber") || varmap.get(var).contains("Integer"))
                            return "";
                    if (builtInOrSubNumericType(arg)) { // meaning a subtype actually
                        String cons = numericConstraints.get(arg);
                        if (StringUtil.emptyString(cons))
                            System.out.println("Error in SUMOtoTFAform.removeNumericInstance(): no constraint for " +
                                    cons + " in formula:\n" + f);
                        else {
                            String origVar = numericVars.get(arg);
                            String newCons = cons.replace("?" + origVar, "?" + var);
                            return newCons;
                        }
                    }
                }
            }
            else {
                ArrayList<String> args = f.complexArgumentsToArrayListString(1);
                StringBuffer sb = new StringBuffer();
                sb.append("(" + f.car());
                for (String a : args)
                    sb.append(" " + removeNumericInstance(a));
                sb.append(")");
                return sb.toString();
            }
        }
        return s;
    }

    /** *************************************************************
     * Check whether variables have multiple mutually exclusive types
     */
    public static boolean inconsistentVarTypes() {

        for (String s : varmap.keySet()) {
            HashSet<String> types = varmap.get(s);
            for (String c1 : types) {
                for (String c2 : types) {
                    if (!c1.equals(c2)) {
                        if (kb.kbCache.checkDisjoint(kb, c1, c2)) {
                            String msg = "SUMOtoTFAform.inconsistentVarTypes(): rejected inconsistent variable types: " +
                                    c1 + ", " + c2 + " for var " + s;
                            System.out.println(msg);
                            errors.add(msg);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** *************************************************************
     */
    private static boolean typeConflict(Formula f, String type) {

        if (debug) System.out.println("SUMOtoTFAform.typeConflict(f,type): " + f + ", " + type);
        if (f == null)
            return false;
        if (f.atom())
            return false;
        if (!kb.isFunctional(f))
            return false;
        ArrayList<String> sig = kb.kbCache.getSignature(f.car());
        String rangeType = sig.get(0);
        if (kb.kbCache.checkDisjoint(kb,type,rangeType)) {
            if (debug) System.out.println("SUMOtoTFAform.typeConflict(2): found type conflict between " + type + " and " + rangeType);
            errors.addAll(kb.kbCache.errors);
            return true;
        }
        return false;
    }

    /** *************************************************************
     * Check for a type conflict between the given type and each type in a set
     * @param types the set of types to check
     * @param type the type to check against the set
     */
    private static boolean typeConflict(HashSet<String> types, String type) {

        if (debug) System.out.println("SUMOtoTFAform.typeConflict(types,type): " + types + ", " + type);
        if (types == null) {
            System.out.println("Error in SUMOtoTFAform.typeConflict(types,type): null types with type: " + type);
            return false;
        }
        for (String s : types) {
            if (kb.kbCache.checkDisjoint(kb, s, type)) {
                if (debug) System.out.println("SUMOtoTFAform.typeConflict(2): found type conflict between " + s + " and " + type);
                errors.addAll(kb.kbCache.errors);
                return true;
            }
        }
        return false;
    }

    /** *************************************************************
     * Reject formulas that wind up with type conflicts despite all
     * attempts to resolve them
     */
    public static boolean typeConflict(Formula f) {

        if (debug) System.out.println("SUMOtoTFAform.typeConflict(f): " + f);
        if (f == null)
            return false;
        if (f.atom())
            return false;
        String op = f.car();
        if (Formula.isQuantifier(op)) {
            ArrayList<String> args = f.complexArgumentsToArrayListString(1);
            for (String s : args) {
                Formula farg = new Formula(s);
                farg.sourceFile = f.sourceFile;
                if (farg.listP() && typeConflict(farg))
                    return true;
            }
        }
        else {
            ArrayList<String> sig = kb.kbCache.getSignature(op);
            ArrayList<String> args = f.complexArgumentsToArrayListString(0);
            for (int i = 1; i < args.size(); i++) {
                String s = args.get(i);
                String sigType = "";
                if (sig != null && i < sig.size())
                    sigType = sig.get(i);
                Formula farg = new Formula(s);
                farg.sourceFile = f.sourceFile;
                if (farg.listP() && kb.isFunctional(farg)) {
                    if (typeConflict(farg, sigType)) {
                        errors.add("error between " + farg + " and argument " + i + " of " + op +
                                " with type " + sigType + " in file " + f.sourceFile);
                        return true;
                    }
                    if (typeConflict(farg))
                        return true;
                }
                else if (farg.listP() && typeConflict(farg))
                    return true;
                else if (farg.isVariable()) {
                    HashSet<String> vars = varmap.get(farg.getFormula());
                    if (vars != null && typeConflict(vars, sigType)) {
                        errors.add("error between " + farg + " and argument " + i + " of " + op +
                                " with type " + sigType + " in file " + f.sourceFile);
                        return true;
                    }
                }
            }
        }
        if (debug) System.out.println("SUMOtoTFAform.typeConflict(f): no conflicts in " + f);
        return false;
    }

    /** *************************************************************
     * Create a sort spec from the relation name with embedded types
     */
    public static String sortFromRelation(String rel) {

        ArrayList<String> sig = SUMOtoTFAform.relationExtractSigFromName(rel);
        if (sig == null || sig.size() < 1)
            return "";
        StringBuffer sigBuf = new StringBuffer();
        for (String s : sig.subList(1,sig.size())) {
            if (StringUtil.emptyString((s)))
                sigBuf.append(" " + "$i" + " *");
            else
                sigBuf.append(" " + SUMOKBtoTFAKB.translateSort(kb, s) + " *");
        }
        String sigStr = sigBuf.toString().substring(0,sigBuf.length()-1);
        String relname = SUMOKBtoTFAKB.translateName(rel);
        if (relname.endsWith(Formula.termMentionSuffix))
            relname = relname.substring(0,relname.length()-3);
        String axname = rel;
        if (axname.startsWith("s__"))
            axname = axname.substring(3,axname.length());
        if (kb.isFunction(rel) || rel.endsWith("Fn")) {
            String range = sig.get(0);
            if (StringUtil.emptyString(range))
                range = "Entity";
            //return("tff(" + StringUtil.initialLowerCase(axname) + "_sig,type," + rel + " : ( " + sigStr + " ) > " + SUMOKBtoTFAKB.translateSort(kb,range) + " ).");
            return(rel + " : ( " + sigStr + " ) > " + SUMOKBtoTFAKB.translateSort(kb,range));
        }
        else
            return(rel + " : ( " + sigStr + " ) > $o ");
    }

    /** *************************************************************
     * @return a list of TFF relation sort definitions to cover
     * ListFn statements that have diverse sorts
     */
    public HashSet<String> missingSorts(Formula f) {

        HashSet<String> result = new HashSet<String>();
        Pattern p = Pattern.compile("(ListFn[^ ]+)");
        Matcher m = p.matcher(f.getFormula());
        while (m.find()) {
            String rel = m.group(1);
            String sort = sortFromRelation(rel);
            if (!StringUtil.emptyString(sort))
                result.add(sort);
        }
        return result;
    }

    /** *************************************************************
     * This is the primary method of the class.  It takes a SUO-KIF
     * formula and returns a TFF formula.
     */
    public static String process(Formula f, boolean query) {

        if (kb == null) {
            System.out.println("Error in SUMOtoTFAform.process(): null kb");
            Thread.dumpStack();
            return "";
        }
        if (f.getFormula().startsWith("(instance equal")) { // || f.theFormula.contains("ListFn"))
            System.out.println("SUMOtoTFAform.process(): rejected (instance equal: " + f);
            return "";
        }
        if (debug) System.out.println("\nSUMOtoTFAform.process(): =======================");
        if (debug) System.out.println("SUMOtoTFAform.process(): f: " + f);
        SUMOformulaToTPTPformula.generateQList(f);
        f = instantiateNumericConstants(f);
        f = new Formula(modifyPrecond(f));
        if (f == null || StringUtil.emptyString(f.getFormula()))
            return "";
        if (debug) System.out.println("SUMOtoTFAform.process(): f after modify precond: " + f);
        f = new Formula(modifyTypesToConstraints(f));
        if (debug) System.out.println("SUMOtoTFAform.process(): f after modify types: " + f);
        String oldf = null;
        int counter = 0;
        do {
            counter++;
            oldf = f.getFormula();
            f = new Formula(elimUnitaryLogops(f)); // remove empty (and... and (or... and =>...
        } while (!f.getFormula().equals(oldf) && counter < 5);
        //if (debug) System.out.println("SUMOtoTFAform.process(): f so far: " + f);
        varmap = fp.findAllTypeRestrictions(f, kb);
        if (inconsistentVarTypes()) {
            System.out.println("SUMOtoTFAform.process(): rejected inconsistent variables types: " + varmap + " in : " + f);
            return "";
        }
        counter = 0;
        do {
            counter++;
            oldf = f.getFormula();
            f = constrainFunctVars(f);
        } while (!f.getFormula().equals(oldf) && counter < 5);
        f = new Formula(removeNumericInstance(f.getFormula()));
        if (f.getFormula() == "")
            return "";
        f = new Formula(elimUnitaryLogops(f)); // remove empty (and... and (or... and =>...
        if (f != null && f.listP()) {
            HashSet<String> UqVars = f.collectUnquantifiedVariables();
            String result = processRecurse(f, "Entity"); // no enclosing required type
            StringBuffer qlist = new StringBuffer();
            for (String s : UqVars) {
                String t = "";
                String oneVar = SUMOformulaToTPTPformula.translateWord(s,s.charAt(0),false);
                if (varmap.keySet().contains(s) && !StringUtil.emptyString(varmap.get(s))) {
                    t = mostSpecificType(varmap.get(s));
                    if (t != null)
                        qlist.append(oneVar + " : " + SUMOKBtoTFAKB.translateSort(kb,t) + ",");
                }
            }
            if (qlist.length() > 1) {
                qlist.deleteCharAt(qlist.length() - 1);  // delete final comma
                String quant = "!";
                if (query)
                    quant = "?";
                result = quant + " [" + qlist + "] : (" + result + ")";
            }
            if (debug) System.out.println("SUMOtoTFAform.process(): result 2: " + result);
            return result;
        }
        return "";
    }

    /** *************************************************************
     */
    public static String process(String s, boolean query) {

        filterMessage = "";
        if (s.contains("ListFn"))
            filterMessage = "SUMOtoTFAform.process(): Formula contains a list operator";
        //if (StringUtil.emptyString(s) || numConstAxioms.contains(s))
        if (StringUtil.emptyString(s)) // || numConstAxioms.contains(s))
            return "";
        Formula f = new Formula(s);
        return process(f,query);
    }

    /** *************************************************************
     */
    public static Collection<String> processList(Collection<Formula> l) {

        ArrayList<String> result = new ArrayList<>();
        for (Formula f : l)
            result.add(process(f,false));
        return result;
    }

    /** *************************************************************
     * if the precondition of a rule is of the form (instance ?X term)
     * @return the name of the variable in the instance statement
     * (without the leading question mark)
     */
    private static String matchingPrecond(Formula f, String term) {

        String ant = FormulaUtil.antecedent(f);
        //if (debug) System.out.println("SUMOtoTFAform.matchingPrecond(): term: " + term);
        //if (debug) System.out.println("SUMOtoTFAform.matchingPrecond(): ant: " + ant);
        if (ant == null)
            return null;
        Pattern p = Pattern.compile("\\(instance \\?(\\w+) " + term + "\\)");
        Matcher m = p.matcher(ant);
        if (m.find()) {
            String var = m.group(1);
            String whole = m.group(0);
            //if (debug) System.out.println("SUMOtoTFAform.matchingPrecond(): matches! " + var);
            if (whole.equals(ant)) {
                //if (debug) System.out.println("SUMOtoTFAform.matchingPrecond(): really matches! " + var);
                return var;
            }
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
        if (f.getFormula() == null)
            return null;
        Pattern p = Pattern.compile("\\(instance \\?\\w+ (\\w+)\\)");
        Matcher m = p.matcher(f.getFormula());
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

        if (f.getFormula() == null) {
            System.out.println("Error in SUMOtoTFAform.matchingInstance(): null formula");
            return null;
        }
        if (kb == null || kb.kbCache == null) {
            System.out.println("Error in SUMOtoTFAform.matchingInstance(): null KB cache");
            System.out.println("formula: " + f);
            return null;
        }
        HashSet<String> intChildren = kb.kbCache.getChildClasses("Integer");
        HashSet<String> realChildren = new HashSet<String>();
        if (kb.kbCache.getChildClasses("RealNumber") != null)
            realChildren.addAll(kb.kbCache.getChildClasses("RealNumber"));
        if (realChildren.contains("Integer"))
            realChildren.remove("Integer");
        Pattern p = Pattern.compile("\\(instance \\?\\w+ (\\w+)\\)");
        Matcher m = p.matcher(f.getFormula());
        while (m.find()) {
            String type = m.group(1);
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

        if (f == null || StringUtil.emptyString(f.getFormula()))
            return f.getFormula();
        String type = "Integer";
        Pattern p = Pattern.compile("\\(instance \\?(\\w+) " + type + "\\)");
        Matcher m = p.matcher(f.getFormula());
        if (m.find()) {
            String var = m.group(1);
            f = new Formula(m.replaceAll(""));
        }

        type = "RealNumber";
        p = Pattern.compile("\\(instance \\?(\\w+) " + type + "\\)");
        m = p.matcher(f.getFormula());
        if (m.find()) {
            String var = m.group(1);
            f = new Formula(m.replaceAll(""));
        }
        return f.getFormula();
    }

    /** *************************************************************
     * replace type statements of the form (instance ?X term), where
     * term is a subtype of Integer or RealNumber with a constraint
     * that defines that type
     * @return String version of the modified formula
     */
    protected static String modifyTypesToConstraints(Formula f) {

        String type = null;
        boolean found = false;
        do {
            type = matchingInstance(f);
            if (type == null) {
                return f.getFormula();
            }
            Pattern p = Pattern.compile("\\(instance \\?(\\w+) " + type + "\\)");
            Matcher m = p.matcher(f.getFormula());
            if (m.find()) {
                found = true;
                String var = m.group(1);
                String toReplace = "(instance ?" + var + " " + type + ")";
                String cons = numericConstraints.get(type);
                //if (debug) System.out.println("modifyTypesToConstraints(): found matching constraint " + cons +
                //        " for type: " + type);
                if (StringUtil.emptyString(cons)) {
                    if (!type.equals("RealNumber") && !type.equals("RationalNumber") && !type.equals("Integer"))
                        System.out.println("Error in SUMOtoTFAform.modifyTypesToConstraints(): no constraint for " +
                            type + " in formula:\n" + f);
                    found = false;
                }
                else {
                    String origVar = numericVars.get(type);
                    String newCons = cons.replace("?" + origVar, "?" + var);
                    f = new Formula(f.getFormula().replace(toReplace, newCons));
                }
            }
            else
                found = false;
        } while (type != null && found);
        return f.getFormula();
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

        System.out.println("INFO in SUMOtoTFAform.buildNumericConstraints(): kbCache : " + kb.kbCache);
        HashSet<String> intChildren = kb.kbCache.getChildClasses("Integer");
        if (intChildren == null)
            return;
        HashSet<String> realChildren = new HashSet<String>();
        if (kb.kbCache.getChildClasses("RealNumber") != null)
            realChildren.addAll(kb.kbCache.getChildClasses("RealNumber"));
        if (realChildren.contains("Integer"))
            realChildren.remove("Integer");
        //if (debug) System.out.println("buildNumericConstraints(): intChildren " + intChildren);
        //if (debug) System.out.println("buildNumericConstraints(): realChildren " + realChildren);
        for (String t : intChildren) {
            ArrayList<Formula> intFormsTemp = kb.ask("ant",0,t);
            if (intFormsTemp != null) {
                for (Formula f : intFormsTemp) {
                    String var = matchingPrecond(f,t);
                    if (var != null) {
                        numericConstraints.put(t, FormulaUtil.consequent(f));
                        numericVars.put(t,var);
                        numConstAxioms.add(f.getFormula());
                    }
                }
            }
        }
        if (realChildren == null)
            return;
        for (String t : realChildren) {
            ArrayList<Formula> realFormsTemp = kb.ask("ant", 0, t);
            if (realFormsTemp != null) {
                for (Formula f : realFormsTemp) {
                    String var = matchingPrecond(f,t);
                    if (var != null) {
                        numericConstraints.put(t, FormulaUtil.consequent(f));
                        numericVars.put(t,var);
                        numConstAxioms.add(f.getFormula());
                    }
                }
            }
        }
    }

    /** *************************************************************
     */
    public static void initNumericConstantTypes() {

        numericConstantTypes.clear();
        numericConstantTypes.put("NumberE","RealNumber");
        numericConstantValues.put("NumberE","2.718282");
        numericConstantTypes.put("Pi","RealNumber");
        numericConstantValues.put("Pi","3.141592653589793");
    }
    /** *************************************************************
     */
    public static void initOnce() {

        if (initialized)
            return;
        KBmanager.getMgr().initializeOnce();
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        kb = KBmanager.getMgr().getKB(kbName);
        fp = new FormulaPreprocessor();
        fp.addOnlyNonNumericTypes = true;
        buildNumericConstraints();
        initNumericConstantTypes();
        numericConstantCount = numericConstantTypes.keySet().size();
        initialized = true;
    }

    /** *************************************************************
     */
    public static void test1() {

        Formula f = new Formula("(equal ?X (AdditionFn__IntegerFn 1 2))");
        System.out.println("SUMOtoTFAform.test1(): " + processRecurse(f,"Entity"));
        f = new Formula("(equal ?X (SubtractionFn__IntegerFn 2 1))");
        System.out.println("SUMOtoTFAform.test1(): " + processRecurse(f,"Entity"));
    }

    /** *************************************************************
     */
    public static void test2() {

        Formula f = new Formula("(=> (and (equal (AbsoluteValueFn ?NUMBER1) ?NUMBER2) " +
                "(instance ?NUMBER1 RealNumber) (instance ?NUMBER2 RealNumber)) " +
                "(or (and (instance ?NUMBER1 NonnegativeRealNumber) (equal ?NUMBER1 ?NUMBER2)) " +
                "(and (instance ?NUMBER1 NegativeRealNumber) (equal ?NUMBER2 (SubtractionFn 0 ?NUMBER1)))))");
        System.out.println("SUMOtoTFAform.test2(): " + process(f,false));
    }

    /** *************************************************************
     */
    public static void test3() {

        Formula f = new Formula("(<=> (equal (RemainderFn ?NUMBER1 ?NUMBER2) ?NUMBER) " +
                "(equal (AdditionFn (MultiplicationFn (FloorFn (DivisionFn ?NUMBER1 ?NUMBER2)) ?NUMBER2) ?NUMBER) ?NUMBER1))");
        System.out.println("SUMOtoTFAform.test3(): " + process(f,false));
    }

    /** *************************************************************
     */
    public static void test4() {

        Formula f = new Formula("(<=> (greaterThanOrEqualTo ?NUMBER1 ?NUMBER2) (or (equal ?NUMBER1 ?NUMBER2) (greaterThan ?NUMBER1 ?NUMBER2)))");
        System.out.println("SUMOtoTFAform.test4(): " + process(f,false));
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
        System.out.println("SUMOtoTFAform.test6(): " + process(f,false));
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
        System.out.println("SUMOtoTFAform.test7(): " + process(f,false));
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
        System.out.println("SUMOtoTFAform.test8(): " + process(f,false));
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
        System.out.println("SUMOtoTFAform.test9(): " + process(f,false));
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
        System.out.println("SUMOtoTFAform.test10(): " + process(f,false));
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
        System.out.println("SUMOtoTFAform.testRlEmbed(): new name: " + makePredFromArgTypes(new Formula(rel),sig));
        kb.kbCache.extendInstance(rel,"1Re");
        kb.kbCache.signatures.put(rel + "__" + "1Re",sig);
    }

    /** *************************************************************
     */
    public static void testRelExtract() {

        String rel = "AbsoluteValueFn__1ReFn";
        System.out.println("SUMOtoTFAform.testRelExtract(): new name: " + relationExtractSigFromName(rel));
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("KB class");
        System.out.println("  options (with a leading '-'):");
        System.out.println("  c <t1> <t2> - return 'best' term of two inputs");
        System.out.println("  t - run test");
        System.out.println("  h - show this help");
    }

    /** ***************************************************************
     */
    public static void main(String[] args) throws IOException {

        System.out.println("INFO in SUMOtoTFAform.main()");
        System.out.println("args:" + args.length + " : " + Arrays.toString(args));
        if (args == null) {
            System.out.println("no command given");
            showHelp();
        }
        else if (args != null && args.length > 0 && args[0].equals("-h"))
            showHelp();
        else {
            SUMOKBtoTFAKB skbtfakb = new SUMOKBtoTFAKB();
            skbtfakb.initOnce();
            initOnce();
            System.out.println("INFO in SUMOtoTFAform.main()");
            if (args != null && args.length > 2 && args[0].equals("-c")) {
                String t1 = args[1];
                String t2 = args[2];
                System.out.println("main() best of " + t1 + " and " + t2 + " : " + constrainTerm(t1,t2));
                ArrayList<String> argTypeMap = new ArrayList<>();
                argTypeMap.add("RealNumber");
                argTypeMap.add("RealNumber");
                ArrayList<String> predTypes = new ArrayList<>();
                predTypes.add("Integer");
                predTypes.add("RealNumber");
                System.out.println();
                ArrayList<String> best = bestSignature(argTypeMap,predTypes);
                System.out.println("main() best: " + best);
                System.out.println();
                ArrayList<String> constrained = mostSpecificSignature(argTypeMap,predTypes);
                System.out.println("main() most specific for (argTypeMap, predType) " +
                        argTypeMap + ", " + predTypes + " : " + constrained);
                System.out.println();
                constrained = mostSpecificSignature(predTypes,predTypes);
                System.out.println("main() most specific for (argTypeMap, predType) " +
                        predTypes + ", " + predTypes + " : " + constrained);
            }
            else if (args != null && args.length > 0 && args[0].equals("-t")) {
                if (debug) System.out.println("SUMOtoTFAform.main(): contains ListFn__1Fn: " + kb.terms.contains("ListFn__1Fn"));
                String kbName = KBmanager.getMgr().getPref("sumokbname");
                String filename = KBmanager.getMgr().getPref("kbDir") + File.separator + kbName + ".tff";
                PrintWriter pw = null;
                try {
                    pw = new PrintWriter(new FileWriter(filename));
                    skbtfakb.writeSorts(pw);
                    //skbtfakb.writeFile(filename, null, false, "", false, pw);
                    pw.flush();
                    pw.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                setNumericFunctionInfo();
                System.out.println(numericConstraints);
                System.out.println(numericVars);
                test7();
            }
            else
                showHelp();
        }
    }
}
