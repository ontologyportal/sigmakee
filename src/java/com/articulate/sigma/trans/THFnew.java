package com.articulate.sigma.trans;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class THFnew {

    public static boolean debug = false;
    public static int axNum = 0;

    // ISSUE 1
    // relations that are treated as modal in the THF embedding, i.e. they get an extra world argument and special THF types.
    // MODAL_RELATIONS = “when this symbol is in head position, treat it as a Kripke-style modal predicate and give it a world argument.
    public static final Set<String> MODAL_RELATIONS = new HashSet<>(Arrays.asList(
            "believes",
            "knows",
            "desires",
            "modalAttribute",
            "holdsDuring"
            // (you can add more here if you *explicitly* design them as modal)
    ));

    // ISSUE 2
    // Symbols whose types are defined explicitly in Modals.getTHFHeader().
    public static final Set<String> RESERVED_MODAL_SYMBOLS =
            new HashSet<>(Arrays.asList(
                    "accreln",
                    "accrelnP",
                    "knows",
                    "believes",
                    "desires",
                    "holdsDuring" // ISSUE 6
            ));

    /** *************************************************************
     */
    private static String processQuant(Formula f, String op,
                                       List<String> args,
                                       Map<String, Set<String>> typeMap) {

        if (debug) System.out.println("THFnew.processQuant(): quantifier");
        if (debug) System.out.println("THFnew.processQuant(): typeMap: " + typeMap);
        if (args.size() < 2) {
            System.err.println("Error in THFnew.processQuant(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        else {
            if (debug) System.out.println("THFnew.processQuant(): correct # of args");
            if (args.get(0) != null) {
                if (debug) System.out.println("THFnew.processQuant(): valid varlist: " + args.get(0));
                Formula varlist = new Formula(args.get(0));
                List<String> vars = varlist.argumentsToArrayListString(0);
                //if (debug) System.out.println("THFnew.processRecurse(): valid vars: " + vars);
                StringBuilder varStr = new StringBuilder();
                varStr.append(generateQList(f,typeMap,new HashSet(vars)));
                if (debug) System.out.println("THFnew.processQuant(): valid vars: " + varStr);
                String opStr = " ! ";
                if (op.equals("exists"))
                    opStr = " ? ";
                if (debug) System.out.println("THFnew.processQuant(): quantified formula: " + args.get(1));
                return Formula.LP + opStr + "[" + varStr + "] : (" +
                        processRecurse(new Formula(args.get(1)),typeMap) + "))";
            }
            else {
                System.err.println("Error in THFnew.processQuant(): null arguments to " + op + " in " + f);
                return "";
            }
        }
    }

    /** *************************************************************
     */
    private static String processConjDisj(Formula f, Formula car,
                                          List<String> args,
                                          Map<String, Set<String>> typeMap) {

        String op = car.getFormula();
        if (args.size() < 2) {
            System.err.println("Error in THFnew.processConjDisj(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        String tptpOp = "&";
        if (op.equals(Formula.OR))
            tptpOp = "|";
        if (op.equals(Formula.XOR))
            tptpOp = "<~>";
        StringBuilder sb = new StringBuilder();
        sb.append(Formula.LP).append(processRecurse(new Formula(args.get(0)),typeMap));
        for (int i = 1; i < args.size(); i++) {
            sb.append(Formula.SPACE).append(tptpOp).append(Formula.SPACE).append(processRecurse(new Formula(args.get(i)),typeMap));
        }
        sb.append(Formula.RP);
        return sb.toString();
    }

    /** *************************************************************
     */
    public static String processLogOp(Formula f, Formula car, List<String> args,
                                      Map<String, Set<String>> typeMap) {

        String op = car.getFormula();
        if (debug) System.out.println("processLogOp(): op: " + op);
        if (debug) System.out.println("processLogOp(): args: " + args);
        if (debug) System.out.println("THFnew.processLogOp(): typeMap: " + typeMap);
        if (op.equals(Formula.AND))
            return processConjDisj(f,car,args,typeMap);
        if (op.equals(Formula.IF)) {
            if (args.size() < 2) {
                System.err.println("Error in THFnew.processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else {
                if (KBmanager.getMgr().prover == KBmanager.Prover.EPROVER)
                    return Formula.LP + processRecurse(new Formula(args.get(0)),typeMap) + " => " +
                            Formula.LP + processRecurse(new Formula(args.get(1)),typeMap) + "))";
                else
                    return Formula.LP + processRecurse(new Formula(args.get(0)),typeMap) + " => " +
                            processRecurse(new Formula(args.get(1)),typeMap) + Formula.RP;
            }
        }
        if (op.equals(Formula.IFF)) {
            if (args.size() < 2) {
                System.err.println("Error in THFnew.processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return "((" + processRecurse(new Formula(args.get(0)),typeMap) + " => " +
                        processRecurse(new Formula(args.get(1)),typeMap) + ") & (" +
                        processRecurse(new Formula(args.get(1)),typeMap) + " => " +
                        processRecurse(new Formula(args.get(0)),typeMap) + "))";
        }
        if (op.equals(Formula.OR))
            return processConjDisj(f,car,args,typeMap);
        if (op.equals(Formula.XOR))
            return processConjDisj(f,car,args,typeMap);
        if (op.equals(Formula.NOT)) {
            if (args.size() != 1) {
                System.err.println("Error in THFnew.processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else
                return "~(" + processRecurse(new Formula(args.get(0)),typeMap) + Formula.RP;
        }
        if (op.equals(Formula.UQUANT) || op.equals(Formula.EQUANT))
            return processQuant(f,op,args,typeMap);
        System.err.println("Error in THFnew.processLogOp(): bad logical operator " + op + " in " + f);
        return "";
    }

    /** *************************************************************
     */
    public static String processEquals(Formula f, Formula car, List<String> args,
                                       Map<String, Set<String>> typeMap) {

        String op = car.getFormula();
        if (args.size() != 2) {
            System.err.println("Error in THFnew.processCompOp(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        if (op.startsWith(Formula.EQUAL)) {
            return Formula.LP + processRecurse(new Formula(args.get(0)),typeMap) + " = " +
                    processRecurse(new Formula(args.get(1)),typeMap) + Formula.RP;
        }
        System.err.println("Error in THFnew.processCompOp(): bad comparison operator " + op + " in " + f);
        return "";
    }

    /** *************************************************************
     */
    public static String processRecurse(Formula f, Map<String, Set<String>> typeMap) {

        if (debug) System.out.println("THFnew.processRecurse(): " + f);
        if (debug) System.out.println("THFnew.processRecurse(): typeMap: " + typeMap);
        if (f == null)
            return "";

        if (f.atom()) {
            int ttype = f.getFormula().charAt(0);
            if (Character.isDigit(ttype))
                ttype = StreamTokenizer_s.TT_NUMBER;
            boolean hasArguments = false; // if it's a modal op, don't add the __m suffix
            if (Modals.regHOLpred.contains(f.getFormula()))
                hasArguments = true;
            return SUMOformulaToTPTPformula.translateWord(f.getFormula(),ttype,hasArguments);
        }

        Formula car = f.carAsFormula();
        List<String> args = f.complexArgumentsToArrayListString(1);
        if (car.listP()) {
            System.err.println("Error in THFnew.processRecurse(): formula " + f);
            return "";
        }

        if (Formula.isLogicalOperator(car.getFormula()))
            return processLogOp(f,car,args,typeMap);
        else if (car.getFormula().equals(Formula.EQUAL))
            return processEquals(f,car,args,typeMap);
        else {
            if (debug) System.out.println("THFnew.processRecurse(): not math or comparison op: " + car);
            StringBuilder argStr = new StringBuilder();
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

            // ISSUE 13
            argStr.delete(argStr.length()-2,argStr.length());  // remove final arg separator

            // Translate predicate name to TPTP
            String functor = SUMOformulaToTPTPformula.translateWord(
                    car.getFormula(), StreamTokenizer.TT_WORD, true);

            // FIX: ensure variable-arity predicates use the right numeric suffix
            // e.g. s__partition__4 with 5 args -> s__partition__5
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("^(s__[A-Za-z0-9]+__)(\\d+)$")
                    .matcher(functor);
            if (m.matches()) {
                int argCount = args.size();
                int oldN = Integer.parseInt(m.group(2));
                if (argCount != oldN) {
                    functor = m.group(1) + argCount;
                }
            }

            String result = Formula.LP + functor + " @ " +
                    argStr.substring(0, argStr.length()-1) + Formula.RP;

//            String result = Formula.LP + SUMOformulaToTPTPformula.translateWord(car.getFormula(),
//                    StreamTokenizer.TT_WORD,true) + " @ " + argStr.substring(0,argStr.length()-1) + Formula.RP;
            //if (debug) System.out.println("THFnew.processRecurse(): result: " + result);
            return result;
        }
    }

    /** *************************************************************
     */
    public static String getTHFtype(String v, Map<String, Set<String>> typeMap) {

        if (debug) System.out.println("THFnew.getTHFtype(): typeMap: "  + typeMap);
        if (debug) System.out.println("THFnew.getTHFtype(): typeMap(v): " + v + ":" + typeMap.get(v));
        if (typeMap.get(v) == null)
            return "$i";
        if (typeMap.get(v).contains("Formula"))
//            return "(w > $o)";
            // ISSUE 4
            return "$o";
        if (typeMap.get(v).contains("World"))
            return "w";
        if (typeMap.get(v).contains("Modal"))
            return "m";
        return "$i";
    }

    /** *************************************************************
     */
    public static String generateQList(Formula f, Map<String,
            Set<String>> typeMap, Set<String> vars) {

        if (debug) System.out.println("THFnew.generateQList(): typeMap: " + typeMap);
        StringBuilder qlist = new StringBuilder();
        String thftype, oneVar;
        for (String s : vars) {
            thftype = getTHFtype(s,typeMap);
            oneVar = SUMOformulaToTPTPformula.translateWord(s,s.charAt(0),false);
            qlist.append(oneVar).append(":").append(thftype).append(",");
        }
        if (qlist.length() > 1)
            qlist.deleteCharAt(qlist.length() - 1);  // delete final comma
        return qlist.toString();
    }

    /** *************************************************************
     * This is the primary method of the class.  It takes a SUO-KIF
     * formula and returns a THF formula.
     */
    public static String process(Formula f, Map<String, Set<String>> typeMap, boolean query) {

        if (debug) System.out.println("THFnew.process(): typeMap: " + typeMap);
        if (f == null) {
            if (debug) System.err.println("Error in THFnew.process(): null formula: ");
            return "";
        }
        if (f.atom())
            return SUMOformulaToTPTPformula.translateWord(f.getFormula(),f.getFormula().charAt(0),false);
        if (f != null && f.listP()) {
            String result = processRecurse(f,typeMap);
            if (debug) System.out.println("THFnew.process(): result 1: " + result);
            Set<String> UqVars = f.collectUnquantifiedVariables();
            Set<String> types = new HashSet<>();
            //types.add("World");
            //typeMap.put("?W1",types);
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

        if (debug) System.out.println("variableArity(): pred: " + pred); // eliminate prefix and suffix
        if (debug)
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

        if (debug) System.out.println("adjustArity(): f: " + f);
        String fstr = f.cdr().substring(1);
        String pred = f.car();
        Pattern p = Pattern.compile("([\\w_]+__)(\\d)");
        Matcher m = p.matcher(pred);
        if (m.find()) {
            int num = Integer.parseInt(m.group(2));
            num--;
            Formula result = new Formula(Formula.LP + m.group(1) + num + Formula.SPACE + fstr);
            if (debug) System.out.println("adjustArity(): result: " + result);
            return result;
        }
        return f;
    }

    /** ***************************************************************
     */
    public static String makeWorldVar(KB kb, Formula f) {

        Set<String> vars = f.collectAllVariables();
        int num = 0;
        while (vars.contains("?W" + num))
            num++;
        return "?W" + num;
    }

    // ISSUE 4
    // ISSUE 12
    // Mark variables that occur as the first argument of modalAttribute as Formula-valued
    private static void markModalAttributeFormulaVars(Formula f, Map<String, Set<String>> typeMap) {

        if (f == null)
            return;

        // If this is directly a (modalAttribute X Y) form
        if ("modalAttribute".equals(f.car())) {
            List<Formula> args = f.complexArgumentsToArrayList(1);
            if (!args.isEmpty()) {
                Formula first = args.get(0);
                String firstStr = first.getFormula();
                if (Formula.isVariable(firstStr)) {
                    Set<String> ts = typeMap.get(firstStr);
                    if (ts == null) {
                        ts = new HashSet<>();
                        typeMap.put(firstStr, ts);
                    }
                    ts.add("Formula");
                }
            }
        }

        // Recurse into sub-formulas
        List<Formula> subs = f.complexArgumentsToArrayList(0);
        if (subs != null) {
            for (Formula sub : subs) {
                markModalAttributeFormulaVars(sub, typeMap);
            }
        }
    }

    /** ***************************************************************
     */
    public static void oneTrans(KB kb, Formula f, Writer bw) throws IOException {

        bw.write("% original: " + f.getFormula() + "\n" +
                "% from file " + f.sourceFile + " at line " + f.startLine + "\n");
        Formula res = Modals.processModals(f, kb);
        if (res != null) {
            FormulaPreprocessor fp = new FormulaPreprocessor();
            Set<Formula> processed = fp.preProcess(res, false, kb);
            if (debug) System.out.println("THFnew.oneTrans(): res.varTypeCache: " + res.varTypeCache);
            if (debug) System.out.println("THFnew.oneTrans(): processed: " + processed);
            if (debug) System.out.println("THFnew.oneTrans(): accreln sig: " + kb.kbCache.getSignature("accreln"));

            res.varTypeCache.clear(); // clear so it really computes the types instead of just returning the type cache

            // Start with an empty typeMap
            Map<String, Set<String>> typeMap = new HashMap<>();

            // Existing typing information from preprocessing
            typeMap.putAll(fp.findAllTypeRestrictions(res, kb));
            if (debug) System.out.println("THFnew.oneTrans(): typeMap(1): " + typeMap);
            typeMap.putAll(res.varTypeCache);
            if (debug) System.out.println("THFnew.oneTrans(): typeMap(2): " + typeMap);

            // Existing code: add World, FORMULA-name hack, etc.
            Set<String> types = new HashSet<>();
            types.add("World");
            String worldVar = makeWorldVar(kb,f);
            typeMap.put(worldVar,types);
            if (debug) System.out.println("THFnew.oneTrans(): typeMap(3): " + typeMap);

            // APPLY structural modalAttribute rule (after typeMap is built)
            // ISSUE 4
            // ISSUE 12
            markModalAttributeFormulaVars(f, typeMap);   // use the original KIF formula


            // ISSUE 4
            // NEW: force “FORMULA” variables to be of type Formula
//            Set<String> allVars = res.collectAllVariables();
//            for (String v : allVars) {
//                if (v == null)
//                    continue;
//                String bare = v.startsWith("?") ? v.substring(1) : v;
//                if (bare.toUpperCase().contains("FORMULA") ||
//                        bare.equalsIgnoreCase("TEXT")) {
//                    Set<String> ts = typeMap.get(v);
//                    if (ts == null) {
//                        ts = new HashSet<>();
//                        typeMap.put(v, ts);
//                    }
//                    ts.add("Formula");
//                }
//            }
//            if (debug) System.out.println("THFnew.oneTrans(): typeMap(4): " + typeMap);


            for (Formula fnew : processed) {

                // ISSUE 15
                if (exclude(fnew,kb,bw)){
                    String flatFormula = f.getFormula().replace("\n", " ").replace("\r", " ");
                    bw.write("% excluded processed formula: " + flatFormula + "\n");
                    bw.write("% from file " + f.sourceFile + " at line " + f.startLine + "\n");
                    continue;
                }

                if (debug) System.out.println("THFnew.oneTrans(): variableArity(kb,fnew.car()): " + variableArity(kb,fnew.car()));
                if (variableArity(kb,fnew.car()))
                    fnew = adjustArity(kb,fnew);   // hack to correct arity after adding world argument
                //ISSUE 1
//                if (fnew.getFormula().startsWith("(instance ") &&
//                        fnew.getFormula().endsWith("Class)")) {
//                    fnew.read("(forall (" + worldVar + ") " +
//                            fnew.getFormula().substring(0, fnew.getFormula().length() - 1) +
//                            " " + worldVar + "))");
//                    types = new HashSet<>();
//                    types.add("World");
//                    fnew.varTypeCache.put(worldVar,types);  // add the "World" type for the ?W since this is post-processModals()
//                }
                if (bw == null)
                    System.out.println(process(new Formula(fnew), typeMap, false));
                else {
                    bw.write("thf(ax" + axNum++ + ",axiom," +
                            process(new Formula(fnew), typeMap, false) + ").\n");
                }
            }
        }
    }

    /** ***************************************************************
     */
    public static boolean protectedRelation(String s) {

        return s.equals("domain") || s.equals("instance") || s.equals("subAttribute") || s.equals("contraryAttribute");
    }

    /** ***************************************************************
     * Formulas to exclude from the translation
     */
    public static boolean exclude(Formula f, KB kb, Writer out) throws IOException {

        if (debug) System.out.println("exclude(): " + f);

        // Exclude strings (quotes)
        if (f.getFormula().contains("\"")) {
            out.write("% exclude(): quote\n");
            return true;
        }

        // Exclude formulas containing true/false
        if (f.getFormula().contains(Formula.LOG_FALSE) ||
                f.getFormula().contains(Formula.LOG_TRUE)) {
            out.write("% exclude(): contains true or false constant\n");
            return true;
        }

        // ISSUE 16
        // Exclude formulas mentioning 'Formula' as a type (SUMO meta-logic)
        if (f.getFormula().contains(" Formula)")) {
            if (debug) {
                System.out.println("exclude(): meta-logical axiom with Formula type: "
                        + f.getFormula());
            }
            out.write("% exclude(): meta-logical axiom with Formula type\n");
            return true;
        }

        // ISSUE 17
        // ISSUE 21
        // ISSUE 22
        // Generic: modal operators must not appear as non-head arguments
        List<String> args = f.complexArgumentsToArrayListString(0);
        String head = args.get(0);
        if (!MODAL_RELATIONS.contains(head)) {
            for (String a : args) {
                if (MODAL_RELATIONS.contains(a)) {
                    out.write("% exclude(): modal operator used as individual: " + a + "\n");
                    return true;
                }
            }
        }

        // Generic rule: if the head is NOT in allowedHeads, then none of the
        // arguments may be modal/HOL/formula predicates or modal attributes.
        if (args != null && !args.isEmpty()) {
            head = args.get(0);

            if (!Modals.allowedHeads.contains(head)) {
                for (int i = 1; i < args.size(); i++) {   // skip head
                    String a = args.get(i);

                    if (THFnew.MODAL_RELATIONS.contains(a)
                            || Modals.modalAttributes.contains(a)
                            || THFnew.RESERVED_MODAL_SYMBOLS.contains(a)
                            || Modals.regHOLpred.contains(a)
                            || Modals.formulaPreds.contains(a)) {

                        out.write("% exclude(): modal/HOL symbol used as individual " +
                                "argument of non-modal head: " + a + "\n");
                        return true;
                    }
                }
            }
        }

        // Exclude domain axioms for formula / HOL predicates
        if (args.size() >= 2 &&
                (args.get(0).equals("domain")) || (args.get(0).equals("subrelation")))  {
            String p = args.get(1);
            if (Modals.formulaPreds.contains(p) ||
                    Modals.regHOLpred.contains(p)) {
                out.write("% exclude(): domain axiom for formula/HOL predicate: " + p + "\n");
                return true;
            }
        }

        head = args.get(0);
        if ("confersNorm".equals(head)) {
            for (String a : args) {
                if (Modals.modalAttributes.contains(a)) {
                    out.write("% exclude(): modal operator used as individual in confersNorm: " + a + "\n");
                    return true;
                }
            }
        }


        // ============================================================
        // META-LOGIC FILTER
        // Exclude any formula where a bare variable is used directly
        // in formula position:
        //   (=> ... ?VAR)
        //   (not ?VAR)   or  (~ ?VAR)
        // Because variables are $i, not $o.
        // This catches the PROP / FORMULA / SITUATION issues in one shot.
        // ============================================================
        if (f.listP()) {
            String op = f.car();
            // arguments starting at position 1 (operator is at 0)
            List<String> opArgs = f.complexArgumentsToArrayListString(1);

            if (opArgs != null) {
                // Case 1: implication with bare variable consequent
                if (op.equals("=>") && opArgs.size() >= 2) {
                    String conseq = opArgs.get(1).trim();
                    if (Formula.isVariable(conseq)) {
                        if (debug) {
                            System.out.println("exclude(): META-LOGIC pattern: variable as consequent of => : "
                                    + conseq + " in " + f.getFormula());
                        }
                        out.write("% exclude(): meta-logic (variable as consequent of =>): "
                                + conseq + "\n");
                        return true;
                    }
                }

                // Case 2: negation of bare variable
                if ((op.equals("not") || op.equals("~")) && opArgs.size() >= 1) {
                    String arg0 = opArgs.get(0).trim();
                    if (Formula.isVariable(arg0)) {
                        if (debug) {
                            System.out.println("exclude(): META-LOGIC pattern: variable under not/~ : "
                                    + arg0 + " in " + f.getFormula());
                        }
                        out.write("% exclude(): meta-logic (variable under not/~): "
                                + arg0 + "\n");
                        return true;
                    }
                }
            }
        }
        // ============================================================

        // ALWAYS recurse into interior lists (this catches nested cases,
        // since exclude() is called on all sub-formulas)
        if (args != null) {
            if (debug) {
                System.out.println("exclude(): Formula: " + f.getFormula());
                System.out.println("exclude(): complexArgumentsToArrayListString(0): " + args);
            }
            for (String s : args) {
                if (Formula.listP(s)) {
                    if (exclude(new Formula(s), kb, out)) {
                        String flat = f.toString().replace("\n", " ").replace("\r", " ");
                        out.write("% excluded(): interior list: " + flat + "\n");
                        return true;
                    }
                }
            }

            // Existing predicate-based exclusion (documentation, format, etc.)
            for (String sub : args) {
                if (excludePred(sub, out)) {
                    String flat = f.toString().replace("\n", " ").replace("\r", " ");
                    out.write("% excluded(): term from excludePred: " + flat + "\n");
                    return true;
                }
            }
        }

        // Additional checks only when ground
        if (f.isGround()) {

            if (debug) System.out.println("exclude(): is ground: " + f);

            // Reuse args (we already computed it above)
            if (args == null)
                args = f.complexArgumentsToArrayListString(0);

            if (args != null && !args.isEmpty()) {

                // ISSUE 11: modal protected relations
                if (protectedRelation(args.get(0))) {
                    for (int i = 1; i < args.size(); i++) {
                        if (Modals.modalAttributes.contains(args.get(i))) {
                            out.write("% exclude(): modal attribute in protected relation: " +
                                    args.get(0) + " " + args.get(i) + "\n");
                            return true;
                        }
                    }
                }

                // ISSUE 10: exclude domain axioms with modal symbols
                if (args.get(0).equals("domain") &&
                        args.size() > 1 &&
                        RESERVED_MODAL_SYMBOLS.contains(args.get(1))) {
                    out.write("% exclude(): modal operator in domain: " + args.get(1) + "\n");
                    return true;
                }

                // Ground numeric filtering
                for (String s : args) {
                    if (StringUtil.isNumeric(s)) {
                        out.write("% exclude(): is numeric(2): \n");
                        if (s.contains(".") || s.contains("-") || s.length() > 1)
                            return true;
                        if (s.charAt(0) < '1' || s.charAt(0) > '6')
                            return true;
                        if (debug) System.out.println("exclude(): numeric arg not excluded: " + s);
                    }
                }
            }
        }

        // TOP-LEVEL predicate check (documentation, format, etc.)
        return excludePred(f.car(), out);
    }



    /** ***************************************************************
     * Predicates that denote formulas that shouldn't be included in
     * the translation.
     */
    public static boolean excludePred(String pred, Writer out) throws IOException {

        if (pred.equals("documentation") ||
                pred.equals("termFormat") ||
                pred.equals("conventionalShortName") ||
                pred.equals("externalImage") ||
                pred.equals("abbreviation") ||
                pred.equals("format") ||
                pred.equals("externalImage") ||
                pred.equals("comment")) {
            out.write("% excludePred(): " + "\n");
            return true;
        }
        else
            return false;
    }

    /** ***************************************************************
     * Predicates that denote formulas that shouldn't be included in
     * type definitions of the translation.
     */
    public static boolean excludeForTypedef(String pred, Writer out) throws IOException {

        if (pred.equals("documentation") ||
                pred.equals("termFormat") ||
                pred.equals("conventionalShortName") ||
                pred.equals("externalImage") ||
                pred.equals("abbreviation") ||
                pred.equals("format") ||
                pred.equals("externalImage") ||
                pred.equals("comment") ||
                pred.equals("knows") ||  // handled in header
                pred.equals("believes") ||  //handled in header
                pred.equals("desires") || //handled in header
                pred.equals("holdsDuring") || // handled in header | ISSUE 6
                //StringUtil.isNumeric(pred) ||
                pred.equals(Formula.EQUAL) ||
                pred.equals("=") ||
                pred.equals(Formula.LOG_FALSE) ||
                pred.equals(Formula.LOG_TRUE) ||
                Formula.isLogicalOperator(pred)) {
            out.write("% excludeForTypedef(): " + pred + "\n");
            return true;
        }
        else
            return false;
    }

    /** ***************************************************************
     */
    public static String sigString(List<String> sig, KB kb, boolean function) {

        if (debug) System.out.println("sigString(): sig: " + sig);
        StringBuilder sb = new StringBuilder();
        boolean first = false;
        String range = "";
        if (function)
            first = true;
        for (String t : sig) { //[, Organism, GeographicArea]
            if (t.equals(""))
                continue;
            if (first) {
                range = t;
                first = false;
            }else if (kb.isInstanceOf(t,"Formula") || t.equals("Formula"))
                sb.append("$o > ");
            else if (kb.isInstanceOf(t,"World") || t.equals("World"))
                sb.append("w > ");
            else
                sb.append("$i > ");
        }
        //if (sb.length() > 2)
        //    sb.delete(sb.length()-2,sb.length());  // remove final arg separator
        //else
        //    System.err.println("Error in sigString(): for sig: " + sig);
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
    public static void writeIntegerTypes(KB kb, Writer out) throws IOException {

        for (int i = 0; i < 7; i++) {
            out.write("thf(n__" + i + "_tp,type,(n__" + i + " : $i)).\n");
        }
    }

    private static Integer getSuffixNumber(String functor) {
        // Match: anything, then "__", then digits at the end
        Matcher m = Pattern.compile("^.*__(\\d+)$").matcher(functor);
        if (m.matches()) {
            return Integer.parseInt(m.group(1));
        }
        return null;   // no suffix found
    }


    /** ***************************************************************
     */
    public static void writeTypes(KB kb, Writer out) throws IOException {

        writeIntegerTypes(kb,out);
        for (String t : kb.terms) {

            // ISSUE 2
            // 1. Skip modal helper symbols – they already have correct types in the header.
            if (RESERVED_MODAL_SYMBOLS.contains(t)) {
                continue;
            }

            if (excludeForTypedef(t,out))
                continue;
            if (kb.isInstanceOf(t,"Relation")) {

//                List<String> sig = kb.kbCache.signatures.get(t);
//                if (debug) System.out.println("THFnew.writeTypes(): sig " + sig + " for " + t);
//                if (!Formula.isLogicalOperator(t) && !t.equals("equals")) { // make sure to update the signature
//                    sig.add("World");
//                }

                List<String> baseSig = kb.kbCache.signatures.get(t);
                if (baseSig == null) {
                    System.err.println("Error in THFnew.writeTypes(): bad sig for " + t);
                    continue;
                }

                // Work on a local copy to build the THF type
                List<String> sig = new ArrayList<>(baseSig);


                if (!Formula.isLogicalOperator(t) && !t.equals("equals")) {
                    // ISSUE 1
                    if (MODAL_RELATIONS.contains(t)) {
                        sig.add("World");
                    }
                }

                if (t == null) {
                    System.err.println("Error in THFnew.writeTypes(): bad sig for " + t);
                    continue;
                }
                boolean isFunction = false;
                String SUMOtoTPTPformula = SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true);
                if (kb.isInstanceOf(t,"Function")) {
                    out.write("thf(" + SUMOtoTPTPformula + "_tp,type,(" + SUMOtoTPTPformula + " : ("); // write signature
                    isFunction = true;
                }else
                    out.write("thf(" + SUMOtoTPTPformula + "_tp,type,(" + SUMOtoTPTPformula + " : ("); // write signature

                // ISSUE 14
                // Check that the sigStr alligns with the __NUM of the term:
                Integer suffixNum = getSuffixNumber(SUMOtoTPTPformula);

                if (suffixNum != null && !sig.isEmpty() && sig.size() > (suffixNum+1)) {
                    while (sig.size() > (suffixNum+1)) {
                        sig.remove(sig.size() - 1);   // remove from end until sizes match
                    }
                }

                String sigStr = sigString(sig,kb,isFunction);

                out.write(sigStr + "))).\n");

                out.write("thf(" + SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true) + "_tp,type,(" +
                        SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),false) + " : $i)).\n"); // write relation constant
            }
            // ISSUE 3
            else if (Modals.modalAttributes.contains(t))
                out.write("thf(" + SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true) + "_tp,type,(" +
                        SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),false) + " : m)).\n"); // write relation constant
            else
                out.write("thf(" + SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true) + "_tp,type,(" +
                        SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true)+ " : $i)).\n");
        }
    }

    /** ***************************************************************
     */
    public static void transModalTHF(KB kb) {

//        THF thf = new THF();
//        Collection coll = Collections.EMPTY_LIST;
//        Collection<Formula> result = new ArrayList<>();
        String kbDir = KBmanager.getMgr().getPref("kbDir");
        String sep = File.separator;

        if (debug) System.out.println("\n\nTHFnew.transModalTHF()");
        String filename = kbDir + sep + kb.name + ".thf";
        try (Writer fstream = new FileWriter(filename);
             Writer out = new BufferedWriter(fstream)) {
            out.write(Modals.getTHFHeader() + "\n");
            writeTypes(kb,out);
            for (Formula f : kb.formulaMap.values()) {
                if (debug) System.out.println("THFnew.transModalTHF(): " + f);
                if (!exclude(f,kb,out))
                    oneTrans(kb,f,out);
                else {
                    // ISSUE 8
                    String flatFormula = f.getFormula().replace("\n", " ").replace("\r", " ");
                    out.write("% excluded: " + flatFormula + "\n");
                    out.write("% from file " + f.sourceFile + " at line " + f.startLine + "\n");

//                    out.write("% excluded: " + f.getFormula() + "\n" +
//                            "% from file " + f.sourceFile + " at line " + f.startLine + "\n");
                }
            }
            System.out.println("\n\nTHFnew.transModalTHF(): Result written to file " + filename);
        }
        catch (IOException ex) {
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
        catch (IOException ex) {
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
            if (!kb.errors.isEmpty()) {
                System.err.println("Errors: " + kb.errors);
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