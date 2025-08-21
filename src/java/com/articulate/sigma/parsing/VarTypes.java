package com.articulate.sigma.parsing;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.articulate.sigma.utils.MapUtils;

import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;

/** Determine the types of variables by their appearance in relations,
 * as well as whether constants or functions are allowed given their types
 */
public class VarTypes {

    Collection<FormulaAST> formulas = null;
    KB kb = null;

    /** a map of variables and the set of types that constrain them */
    Map<String,Set<String>> varTypeMap = new HashMap<>();

    public boolean debug = false;

    /** ***************************************************************
     */
    public VarTypes(Collection<FormulaAST> set, KB kbinput) {
        formulas = set;
        kb = kbinput;
        if (set != null && debug)
            System.out.println("VarTypes(): created with # inputs: " + set.size());
    }

    /** ***************************************************************
     * funterm : '(' FUNWORD argument+ ')' ;
     * argument : (sentence | term) ;
     */
    public String findTypeOfFunterm(SuokifParser.FuntermContext input) {

        if (debug) System.out.println("VarTypes.findTypeOfFunterm(): input: " + input.getText());
        String type = "Entity";
        if (input.FUNWORD() != null) {
            String funword = input.FUNWORD().toString();
            type = kb.kbCache.getRange(funword);
        }
        return type;
    }

    /** ***************************************************************
     * term : (funterm | variable | string | number | FUNWORD | IDENTIFIER ) ;
     * Since types are not instances, always remove any trailing '+' denoting a
     * class
     */
    public String findTypeOfTerm(SuokifParser.TermContext input, String sigType) {

        if (debug) System.out.println("VarTypes.findTypeOfTerm(): input: " + input.getText());
        if (debug) System.out.println("VarTypes.findTypeOfTerm(): sigType: " + sigType);
        String type = null;
        if (input.IDENTIFIER() != null) {
            String ident = input.IDENTIFIER().getText();
            if (sigType.endsWith("+")) {
                if (kb.kbCache.subclassOf(ident,sigType.substring(0,sigType.length()-1)))
                    type = ident;
            }
            else if (sigType.equals("SetOrClass") && !kb.isInstance(ident))
                type = ident;
            else if (kb.kbCache.isInstanceOf(ident,sigType))
                type = kb.immediateParents(ident).iterator().next();
            else
                System.err.println("Error in Vartypes.findTypeOfTerm(): signature " + sigType + " doesn't allow " + ident);
        }
        for (ParseTree c : ((SuokifParser.TermContext) input).children) {
            switch (c.getClass().getName()) {
                case "com.articulate.sigma.parsing.SuokifParser$FuntermContext":
                    type = findTypeOfFunterm((SuokifParser.FuntermContext) c);
                    break;
                case "com.articulate.sigma.parsing.SuokifParser$VariableContext":
                    MapUtils.addToMap(varTypeMap, c.getText(),sigType);
                    break;
                case "com.articulate.sigma.parsing.SuokifParser$StringContext":
                    if (!sigType.equals("SymbolicString"))
                        System.err.println("Error in Vartypes.findTypeOfTerm(): signature doesn't allow string " + c.getText());
                    else
                        type = "SymbolicString";
                    break;
                case "com.articulate.sigma.parsing.SuokifParser$NumberContext":
                    if (!kb.kbCache.subclassOf(sigType,"Quantity") && !sigType.equals("Quantity"))
                        System.err.println("Error in Vartypes.findTypeOfTerm()(): signature doesn't allow number " + c.getText());
                    else {
                        if (c.getText().contains("."))
                            type = "RealNumber";
                        else
                            type = "Integer";
                        if (c.getText().startsWith("-"))
                            type = "Negative" + type;
                        else
                            type = "Positive" + type;
                    }
                    break;
                default:
                    break;
            }
        }
        return type;
    }

    /** ***************************************************************
     * Go through the equation map of a formula.  If the argument is a variable,
     * add the type to the variable type map (varTypeMap) by finding the
     * type of the other argument
     *
     * eqsent : '(' 'equal' term term ')' ;
     * term : (funterm | variable | string | number | FUNWORD | IDENTIFIER ) ;
     */
    public void findEquationType(FormulaAST f) {

        if (debug) System.out.println("VarTypes.findEquationType(): input: " + f);
        String var = null, type = null, funterm;
        SuokifParser.TermContext arg1;
        SuokifParser.TermContext arg2;
        ParseTree child1;
        ParseTree child2;
        for (List<SuokifParser.TermContext> pair : f.eqList) {
            arg1 = pair.get(0);
            arg2 = pair.get(1);
            child1 = arg1.children.iterator().next();
            child2 = arg2.children.iterator().next();
            if (arg1.IDENTIFIER() != null)
                type = findTypeOfTerm(arg1,"Entity");
            else if (child1 instanceof SuokifParser.FuntermContext) {
                funterm = ((SuokifParser.FuntermContext) child1).FUNWORD().toString();
                type = kb.kbCache.getRange(funterm);
            }
            else if (child1 instanceof SuokifParser.VariableContext) {
                var = child1.getText();
            }
            else if (child1 instanceof SuokifParser.StringContext)
                type = "SymbolicString";
            else if (child1 instanceof SuokifParser.NumberContext)
                type = "Number";

            if (arg2.IDENTIFIER() != null)
                type = findTypeOfTerm(arg2,"Entity");
            else if (child2 instanceof SuokifParser.FuntermContext) {
                funterm = ((SuokifParser.FuntermContext) child2).FUNWORD().toString();
                type = kb.kbCache.getRange(funterm);
            }
            else if (child2 instanceof SuokifParser.VariableContext) {
                var = child2.getText();
            }
            else if (child2 instanceof SuokifParser.StringContext)
                type = "SymbolicString";
            else if (child2  instanceof SuokifParser.NumberContext)
                type = "Number";

            if (debug) System.out.println("findEquationType(): var&type: " + var + " : " + type);
            MapUtils.addToMap(f.varTypes,var, type);
        }
    }

    /** ***************************************************************
     * Constrain variables found in the argument list of a predicate variable
     * where the relation 'rel' will be substituted
     */
    public FormulaAST constrainVars(String rel, String var, FormulaAST f) {

        if (var.startsWith(Formula.R_PREF))
            return f;
        Map<Integer, Set<SuokifParser.ArgumentContext>> argsForIndex = f.argMap.get(var);
        List<String> sig = kb.kbCache.getSignature(rel);
        if (sig == null || argsForIndex == null || argsForIndex.keySet().size() != sig.size()-1) { // signatures have a 0 element for function return type
            StringBuilder sb = new StringBuilder();
            for (Integer i : argsForIndex.keySet()) {
                sb.append(i).append(" : ");
                for (SuokifParser.ArgumentContext arg : argsForIndex.get(i))
                    sb.append(arg.getText()).append(", ");
            }
            if (debug) System.out.println("VarTypes.constrainVars(): " + sb.toString());
            if (sb.toString().contains(Formula.R_PREF)) {
                if (debug) System.out.println("Arg mismatch caused by row variable " + argsForIndex.keySet());
            }
            else {
                System.out.println("VarTypes.constrainVars(): mismatched argument type lists:");
                System.out.println("VarTypes.constrainVars(): line and file: " + f.sourceFile + " " + f.startLine);
                System.out.println("When substituting " + rel + " for " + var);
                System.out.println("sig " + sig);
                System.out.print("argsForIndex: ");
                if (argsForIndex != null) {
                    for (Integer i : argsForIndex.keySet()) {
                        sb.append(i).append(" : ");
                        for (SuokifParser.ArgumentContext arg : argsForIndex.get(i))
                            sb.append(arg.getText()).append(", ");
                    }
                    System.out.println("VarTypes.constrainVars(): " + sb.toString());
                }
                else
                    System.out.println("is null");
            }
        }
        else {
            String sigTypeAtIndex, t;
            Set<SuokifParser.ArgumentContext> args;
            for (Integer i : argsForIndex.keySet()) {
                sigTypeAtIndex = sig.get(i);
                args = argsForIndex.get(i);
                for (SuokifParser.ArgumentContext ac : args) {
                    for (ParseTree c : ac.children) {
                        if (debug) System.out.println("child: " + c.getClass().getName());
                        if (c instanceof SuokifParser.SentenceContext) {
                            for (ParseTree c2 : ((SuokifParser.SentenceContext) c).children) {
                                if (c2 instanceof SuokifParser.RelsentContext ||
                                        c2 instanceof SuokifParser.LogsentContext ||
                                        c2 instanceof SuokifParser.QuantsentContext)
                                    f.higherOrder = true;
                                if (c2 instanceof SuokifParser.VariableContext &&
                                        ((SuokifParser.VariableContext) c2).REGVAR() != null) {
                                    MapUtils.addToMap(f.varTypes,c2.getText(), sigTypeAtIndex);
                                }
                            }
                        }
                        if (c instanceof SuokifParser.TermContext) {
                            t = findTypeOfTerm((SuokifParser.TermContext) c, sigTypeAtIndex);
                            if (!sigTypeAtIndex.equals(t) && !kb.isSubclass(t,sigTypeAtIndex))
                                System.err.println("Error in VarTypes.constrainVars(): arg " + c.getText() +
                                        " not allowed as argument " + i + " to relation " + rel + " in formula " + f);
                        }
                    }
                }
            }
        }
        return f;
    }

    /** ***************************************************************
     * The type specialization of MeasureFn is a special case
     * funterm : '(' FUNWORD argument+ ')' ;
     * argument : (sentence | term) ;
     * term : (funterm | variable | string | number | FUNWORD | IDENTIFIER ) ;
     */
    public boolean functionSpecializationAllowed(ParseTree c, String sigTypeNoSuffix) {

        if (debug) System.out.println("VarTypes.functionSpecializationAllowed(): Checking " +
                c.getText() + " for fit with sig " + sigTypeNoSuffix);
        if (debug) System.out.println("VarTypes.functionSpecializationAllowed(): expression type: " + c.getClass().getName());
        if (!(c instanceof SuokifParser.TermContext)) {
            return false;
        }
        else {
            SuokifParser.TermContext tc = (SuokifParser.TermContext) c;
            SuokifParser.ArgumentContext ac;
            SuokifParser.TermContext othertc;
            int argnum;
            String type = null;
            for (ParseTree firsttc : tc.children) {
                if (debug) System.out.println("VarTypes.functionSpecializationAllowed(): firsttc expression type: " + firsttc.getClass().getName());
                if (!(firsttc instanceof SuokifParser.FuntermContext))
                    return false;
                else {
                    SuokifParser.FuntermContext fc = (SuokifParser.FuntermContext) firsttc;
                    if (fc.FUNWORD() != null) {
                        String funword = fc.FUNWORD().getText();
                        if (debug)
                            System.out.println("VarTypes.functionSpecializationAllowed(): funword: " + funword);
                        if (funword.equals("MeasureFn")) {
                            argnum = 1;
                            for (ParseTree ptc : fc.children) {
                                if (debug) System.out.println("VarTypes.functionSpecializationAllowed(): argnum: " + argnum);
                                if (debug) System.out.println("VarTypes.functionSpecializationAllowed(): ptc expression type: " + ptc.getClass().getName());
                                if (debug) System.out.println("VarTypes.functionSpecializationAllowed(): ptc: " + ptc.getText());
                                if (ptc instanceof SuokifParser.ArgumentContext) {
                                    ac = (SuokifParser.ArgumentContext) ptc;
                                    for (ParseTree ptc2 : ac.children) {
                                        if (debug) System.out.println("VarTypes.functionSpecializationAllowed(): ptc2 expression type: " + ptc2.getClass().getName());
                                        if (debug) System.out.println("VarTypes.functionSpecializationAllowed(): ptc2: " + ptc2.getText());
                                        if (argnum == 4 && ptc2 instanceof SuokifParser.TermContext) {
                                            othertc = (SuokifParser.TermContext) ptc2;
                                            if (othertc.IDENTIFIER() != null) {
                                                type = othertc.IDENTIFIER().getText();
                                                if (debug)
                                                    System.out.println("VarTypes.functionSpecializationAllowed(): found type: " + type);
                                            }
                                        }
                                    }
                                }
                                argnum++;
                            }
                            if (type == null)
                                if (debug)
                                    System.out.println("VarTypes.functionSpecializationAllowed(): no type found");
                            if (type != null && type.endsWith("+") &&
                                    (kb.kbCache.subclassOf(type.substring(0,type.length()-1), sigTypeNoSuffix) ||
                                            type.substring(0,type.length()-1).equals(sigTypeNoSuffix))) {
                                if (debug)
                                    System.out.println("VarTypes.functionSpecializationAllowed(): type fits! (and is a class)");
                                return true;
                            }
                            else if (type != null && !type.endsWith("+") && kb.kbCache.isInstanceOf(type, sigTypeNoSuffix)) {
                                if (debug)
                                    System.out.println("VarTypes.functionSpecializationAllowed(): type fits! (and is an instance)");
                                return true;
                            }
                            else {
                                if (debug)
                                    System.out.println("VarTypes.functionSpecializationAllowed(): type " + type + " not an instance of " + sigTypeNoSuffix);
                            }
                        }
                        else
                            if (debug)
                                System.out.println("VarTypes.functionSpecializationAllowed(): not a MeasureFn");
                    }
                }
            }
        }
        return false;
    }

    /** ***************************************************************
     * if a relation is used as an argument, add a suffix to that constant
     * in the literal for the constant list.  It will be used later in
     * conversion to TPTP.
     */
    public void findRelationsAsArgs(FormulaAST f) {

        FormulaAST.ArgStruct as;
        String newc;
        for (String c : f.constants.keySet()) {
            as = f.constants.get(c);
            if (kb.kbCache.relations.contains(c)) {
                newc = c + Formula.TERM_MENTION_SUFFIX;
                as.literal = as.literal.replace(Formula.SPACE +  c, " " + newc);
            }
        }
    }

    /** ***************************************************************
     * Go through the argument map of a formula, which consists of all
     * predicates and their arguments in each position, within this formula,
     * and find the type of that argument.  If the argument is a variable,
     * add the type to the variable type map (varTypeMap).  If the argument
     * is not a variable, make sure it is allowed as a argument, given the
     * signature of the predicate.  If not, report an error.  Handling
     * functions that are arguments requires a special case to call
     * findTypeOfFunterm.  MeasureFn is a special case of function arguments
     * since the type of the measure indicates the return type of the function.
     *
     * argument : (sentence | term) ;
     * sentence : (relsent | logsent | quantsent | variable) ;
     * term : (funterm | variable | string | number | FUNWORD | IDENTIFIER ) ;
     */
    public void findType(FormulaAST f) {

        if (debug) System.out.println("VarTypes.findType(): " + f);
        Map<Integer, Set<SuokifParser.ArgumentContext>> argsForIndex;
        List<String> sig;
        StringBuilder sb = new StringBuilder();
        String sigTypeAtIndex, t, sigTypeNoSuffix, tNoSuffix;
        Set<SuokifParser.ArgumentContext> args;
        for (String pred : f.argMap.keySet()) {
            if (debug) System.out.println("VarTypes.findType():relation: " + pred);
            argsForIndex = f.argMap.get(pred);
            if (debug) {
                for (Integer key : argsForIndex.keySet()) {
                    System.out.println("VarTypes.findType(): argsForIndex: key " + key);
                    for (SuokifParser.ArgumentContext ac : argsForIndex.get(key)) {
                        System.out.print(ac.getText() + ", ");
                    }
                    System.out.println();
                }
            }
            if (argsForIndex == null || Formula.isVariable(pred))
                continue;
            if (debug) {
                System.out.println("VarTypes.findType(): ");
                printContexts(argsForIndex);
            }
            sig = kb.kbCache.getSignature(pred);
            if (sig == null) {
                System.err.println("Error in VarTypes.findType(): null signature in formula " + f + " for pred: " + pred);
                continue;
            }
            if (sig.contains("Formula")) {
                f.higherOrder = true;
                return;  // no point in continuing since all preprocessing is just to translate to TPTP
            }
            if (debug) System.out.println("VarTypes.findType():signature: " + sig);
            if (kb.kbCache.getArity(pred) != -1 && argsForIndex.keySet().size() != sig.size()-1) { // signatures have a 0 element for function return type
                sb.setLength(0); // reset
                for (Integer i : argsForIndex.keySet()) {
                    sb.append(i).append(" : ");
                    for (SuokifParser.ArgumentContext arg : argsForIndex.get(i))
                        sb.append(arg.getText()).append(", ");
                }
                if (debug) System.out.println("VarTypes.findType(): " + sb.toString());
                if (sb.toString().contains(Formula.R_PREF)) {
                    if (debug) System.out.println("Arg mismatch caused by row variable " + argsForIndex.keySet());
                }
                else {
                    System.err.println("Error in VarTypes.findType(): mismatched argument type lists:");
                    System.err.println("VarTypes.findType(): relation: " + pred);
                    System.err.println("VarTypes.findType(): argsForIndex: " + sb.toString());
                    System.err.println("VarTypes.findType(): sig: " + sig);
                    System.err.println("VarTypes.findType(): line and file: " + f.sourceFile + " " + f.startLine);
                }
            }
            else {
                for (Integer i : argsForIndex.keySet()) {
                    if (sig.size() <= i && kb.kbCache.getArity(pred) != -1) {
                        System.err.println("Error in VarTypes.findType() no signature element " + i + " for " + pred + " in " + sig);
                        continue;
                    }
                    if (kb.getValence(pred) == -1)
                        sigTypeAtIndex = kb.kbCache.variableArityType(pred);
                    else
                        sigTypeAtIndex = sig.get(i);
                    args = argsForIndex.get(i);
                    for (SuokifParser.ArgumentContext ac : args) {
                        for (ParseTree c : ac.children) {
                            if (debug) System.out.println("child: " + c.getClass().getName());
                            if (c instanceof SuokifParser.SentenceContext) {
                                for (ParseTree c2 : ((SuokifParser.SentenceContext) c).children) {
                                    if (c2 instanceof SuokifParser.RelsentContext ||
                                            c2 instanceof SuokifParser.LogsentContext ||
                                            c2 instanceof SuokifParser.QuantsentContext)
                                        f.higherOrder = true;
                                    if (c2 instanceof SuokifParser.VariableContext &&
                                            ((SuokifParser.VariableContext) c2).REGVAR() != null) {
                                        MapUtils.addToMap(f.varTypes,c2.getText(), sigTypeAtIndex);
                                    }
                                }
                            }
                            if (c instanceof SuokifParser.TermContext) {
                                t = findTypeOfTerm((SuokifParser.TermContext) c, sigTypeAtIndex); // the type of the argument
                                if (kb.isInstance(c.getText())) {
                                    if (!kb.kbCache.isInstanceOf(c.getText(),sigTypeAtIndex))
                                        System.out.println("Warning in VarTypes.findType(): instance arg " + c.getText() + " of type " + t +
                                                " may not be allowed as argument " + i + " to relation " + pred + " in formula " + f +
                                                "\nthat requires " + sigTypeAtIndex);
                                }
                                else {
                                    sigTypeNoSuffix = sigTypeAtIndex;
                                    if (sigTypeAtIndex.endsWith("+"))
                                        sigTypeNoSuffix = sigTypeAtIndex.substring(0,sigTypeAtIndex.length()-1); // remove the trailing '+'
                                    tNoSuffix = t;
                                    if (t != null && t.endsWith("+"))
                                        tNoSuffix = t.substring(0,t.length()-1); // remove the trailing '+'
                                    if (!sigTypeNoSuffix.equals(c.getText()) && !kb.isSubclass(tNoSuffix, sigTypeNoSuffix) && !sigTypeNoSuffix.equals("SetOrClass") )
                                        if (!functionSpecializationAllowed(c,sigTypeNoSuffix))
                                            System.out.println("Warning in VarTypes.findType(): arg " + c.getText() + " of type " + tNoSuffix +
                                                " may not be allowed as argument " + i + " to relation " + pred + " in formula " + f +
                                                "\nthat requires " + sigTypeNoSuffix);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** ***************************************************************
     */
    public void printContexts(Map<Integer, Set<SuokifParser.ArgumentContext>> args) {

        System.out.println("VarTypes.printContexts(): args: ");
        Set<SuokifParser.ArgumentContext> argTypes;
        for (Integer i : args.keySet()) {
            System.out.print(i + ": {");
            argTypes = args.get(i);
            for (SuokifParser.ArgumentContext ac : argTypes) {
                System.out.print(ac.getText() + ", ");
            }
            System.out.println("}");
        }
    }

    /** ***************************************************************
     */
    public void findTypes() {

        for (FormulaAST f : formulas) {
            if (!f.higherOrder) // could have been found while parsing
                findType(f);
            if (!f.higherOrder) { // or could have been found when looking at relation signatures
                findEquationType(f);
                findRelationsAsArgs(f);
            }
        }
    }
}
