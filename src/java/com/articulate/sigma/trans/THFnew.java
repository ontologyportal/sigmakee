package com.articulate.sigma.trans;

import com.articulate.sigma.*;
import com.articulate.sigma.parsing.Expr;
import com.articulate.sigma.parsing.ExprToTHF;
import com.articulate.sigma.parsing.FormulaAST;
import com.articulate.sigma.parsing.SuokifVisitor;
import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class THFnew {

    public static boolean debug = false;
    public static int axNum = 0;
    public static Set<Formula> badUsageSymbols = new HashSet<>();
    public static Set<String> predicateTerms = new HashSet<>(); //Terms that return $o instead of $i

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
                if (debug) System.out.println("THFnew.processRecurse(): vars: " + vars);
                StringBuilder varStr = new StringBuilder();
                varStr.append(generateQList(f,typeMap,new HashSet(vars)));
                if (debug) System.out.println("THFnew.processQuant(): quantifier vars: " + varStr);
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
            if (Modals.regHOLpred.contains(f.getFormula()) || Modals.regHOL3pred.contains(f.getFormula()))
                hasArguments = true;
            return SUMOformulaToTPTPformula.translateWord(f.getFormula(), ttype, hasArguments);
        }

        Formula car = f.carAsFormula();
        List<String> args = f.complexArgumentsToArrayListString(1);
        if (car.listP()) {
            System.err.println("Error in THFnew.processRecurse(): formula " + f);
            return "";
        }

        if (Formula.isLogicalOperator(car.getFormula()))
            return processLogOp(f, car, args, typeMap);
        else if (car.getFormula().equals(Formula.EQUAL))
            return processEquals(f, car, args, typeMap);
        else {
            if (debug) System.out.println("THFnew.processRecurse(): not math or comparison op: " + car);
            StringBuilder argStr = new StringBuilder();
            for (String s : args) {
                if (car.getFormula().equals("instance")) {
                    int ttype = f.getFormula().charAt(0);
                    if (Character.isDigit(ttype))
                        ttype = StreamTokenizer_s.TT_NUMBER;
                    if (Formula.atom(s))
                        argStr.append(SUMOformulaToTPTPformula.translateWord(s, ttype, false));
                    else
                        argStr.append(processRecurse(new Formula(s), typeMap));
                }
                else
                    argStr.append(processRecurse(new Formula(s), typeMap));
                argStr.append(" @ ");
            }

            // ISSUE 13
            argStr.delete(argStr.length() - 2, argStr.length());  // remove final arg separator

            // Translate predicate name to TPTP
            String functor = SUMOformulaToTPTPformula.translateWord(
                    car.getFormula(), StreamTokenizer.TT_WORD, true);

            // FIX: ensure variable-arity predicates use the right numeric suffix
            // e.g. s__partition__4 with 5 args -> s__partition__5
            Matcher m = Pattern
                    .compile("^(s__[A-Za-z0-9]+__)(\\d+)$")
                    .matcher(functor);
            if (m.matches()) {
                int argCount = args.size();

                // TODO: Fix the hard typed values
                List<String> worldArgs = List.of("?W1", "?W2");
                // Don't increase the term's ArityValue if it contains a World argument.
                if (argCount > 0 && (worldArgs.contains(args.get(argCount - 1))))
                    argCount--;
                int oldN = Integer.parseInt(m.group(2));
                if (argCount != oldN)
                    functor = m.group(1) + argCount;
            }
            String result = Formula.LP + functor + " @ " +
                    argStr.substring(0, argStr.length() - 1) + Formula.RP;
//            String result = Formula.LP + SUMOformulaToTPTPformula.translateWord(car.getFormula(),
//                    StreamTokenizer.TT_WORD,true) + " @ " + argStr.substring(0,argStr.length()-1) + Formula.RP;
            //if (debug) System.out.println("THFnew.processRecurse(): result: " + result);
            return result;
        }
    }

    /** *************************************************************
     * Map a KIF variable to a THF type, based on its inferred SUMO types.
     * - Formula variables become (w > $o) so they can be applied as F @ W.
     * - World variables become w.
     * - Modal variables become m.
     * - Everything else collapses to $i.
     */
    public static String getTHFtype(String v, Map<String, Set<String>> typeMap) {

        if (debug) System.out.println("THFnew.getTHFtype(): typeMap: "  + typeMap);
        if (debug) System.out.println("THFnew.getTHFtype(): typeMap(v): " + v + ":" + typeMap.get(v));
//        if (v.startsWith("?W")) {
//            System.out.println("V= "+v);
//            System.out.println("typeMap(v)= "+typeMap.get(v));
//        }
        if (typeMap.get(v) == null) {
            // Fallback: ?W<n> variables introduced deep in modal recursion may
            // not reach the typeMap (worldNum is a local int, recursive calls
            // cannot update the caller's counter).  Treat them as world type.
            if (v.matches("\\?W+\\d+")) return "w";
            return "$i";
        }
        if (typeMap.get(v).contains("World"))
            return "w";
        if (typeMap.get(v).contains("Formula")) {
            // Treat "Formula" variables as functions from worlds to booleans,
            // as per Alex Steen / TQM10: F : w > $o, used as F @ W.
            return "(w > $o)";
        }
        if (typeMap.get(v).contains("Modal"))
            return "m";
        return "$i";
    }

    /** ***************************************************************
     */
    private static String getTHFtypeNonModal(String v, Map<String, Set<String>> typeMap) {
        if (typeMap.get(v) == null)
            return "$i";
        if (typeMap.get(v).contains("Formula"))
            return "$o";
        // Collapse World/Modal to $i in the non-modal embedding
        // (we don't introduce Kripke worlds or modal types)
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
            if (debug) System.out.println("THFnew.generateQList(): thftype for  " + s + " : " + thftype);
            oneVar = SUMOformulaToTPTPformula.translateWord(s,s.charAt(0),false);
            qlist.append(oneVar).append(":").append(thftype).append(",");
        }
        if (qlist.length() > 1)
            qlist.deleteCharAt(qlist.length() - 1);  // delete final comma
        return qlist.toString();
    }

    /** ***************************************************************
     */
    public static String generateQListNonModal(Formula f,
                                               Map<String, Set<String>> typeMap,
                                               Set<String> vars) {

        StringBuilder qlist = new StringBuilder();
        for (String s : vars) {
            String thftype = getTHFtypeNonModal(s, typeMap);
            String oneVar = SUMOformulaToTPTPformula.translateWord(
                    s, s.charAt(0), false);
            qlist.append(oneVar).append(":").append(thftype).append(",");
        }
        if (qlist.length() > 1)
            qlist.deleteCharAt(qlist.length() - 1);
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
    public static String processNonModal(Formula f,
                                         Map<String, Set<String>> typeMap,
                                         boolean query) {

        if (f == null)
            return "";
        if (f.atom())
            return SUMOformulaToTPTPformula.translateWord(
                    f.getFormula(), f.getFormula().charAt(0), false);

        if (f.listP()) {
            String result = processRecurse(f, typeMap);
            Set<String> UqVars = f.collectUnquantifiedVariables();
            String qlist = generateQListNonModal(f, typeMap, UqVars);

            if (qlist.length() > 1) {
                String quantification = query ? "? [" : "! [";
                result = "( " + quantification + qlist + "] : (" +
                        result + " ) )";
            }
            return result;
        }
        return f.getFormula();
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

    /** ***************************************************************
     */
    // ISSUE 4, ISSUE 12
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
    public static void oneTrans(KB kb, Formula f, PrintWriter bw) throws IOException {

        if (bw == null)
            if (debug) System.out.println("% original: " + f.getFormula() + "\n" +
                    "% from file " + f.sourceFile + " at line " + f.startLine + "\n");
        else
            bw.write("% original: " + f.getFormula() + "\n" +
                "% from file " + f.sourceFile + " at line " + f.startLine + "\n");

        // 1) Modal pass on the original f, for TYPE INFO ONLY
        Map<String, Set<String>> typeMap = new HashMap<>();
        Formula res = Modals.processModals(f, kb, typeMap);
        if (res != null) {
            FormulaPreprocessor fp = new FormulaPreprocessor();

            // 2) IMPORTANT: preprocess ORIGINAL f, not res.
            //    So processed formulas do NOT yet contain worlds.
            Set<Formula> processed = fp.preProcess(f, false, kb);
            if (debug) System.out.println("oneTrans(): preprocessed: " + processed);
            // 3) Build typeMap from res (modalised original) as before
            res.varTypeCache.clear();

            typeMap.putAll(fp.findAllTypeRestrictions(res, kb));
            typeMap.putAll(res.varTypeCache);

            Set<String> types = new HashSet<>();
            types.add("World");
            String worldVar = makeWorldVar(kb, f);
            typeMap.put(worldVar, types);
            if (debug) System.out.println("oneTrans(): typemap: " + typeMap);
            markModalAttributeFormulaVars(f, typeMap);
            if (debug) System.out.println("oneTrans(): formula: " + f);

            // 4) For each processed formula, NOW apply Modals and then THFnew
            for (Formula fnew : processed) {
                // Single, correct modal/world pass per processed formula
                Formula fmodal = Modals.processModals(fnew, kb,typeMap);
                if (debug) System.out.println("oneTrans(): after modal processing: " + fmodal);
                if (fmodal == null)
                    continue;
                if (exclude(fmodal, kb, bw))
                    continue;
                if (bw == null) {
                    if (debug) System.out.println("oneTrans(): processed: " +
                            process(new Formula(fmodal), typeMap, false));
                }
                else {
                    String s = "thf(ax" + axNum++ + ",axiom," +
                            process(new Formula(fmodal), typeMap, false) + ").\n";
                    if (debug) System.out.println("oneTrans(): " + s);
                    bw.println(s);
                }
            }
        }
    }

    /** ***************************************************************
     */
    public static void oneTransNonModal(KB kb, Formula f, Writer bw)
            throws IOException {

        bw.write("% original: " + f.getFormula() + "\n" +
                "% from file " + f.sourceFile + " at line " + f.startLine + "\n");

        Formula res = f;  // no Modals.processModals()

        FormulaPreprocessor fp = new FormulaPreprocessor();
        Set<Formula> processed = fp.preProcess(res, false, kb);

        res.varTypeCache.clear();

        Map<String, Set<String>> typeMap = new HashMap<>();
        typeMap.putAll(fp.findAllTypeRestrictions(res, kb));
        typeMap.putAll(res.varTypeCache);

        for (Formula fnew : processed) {

            if (excludeNonModal(fnew, kb, bw)) {
                String flatFormula = f.getFormula()
                        .replace("\n", " ").replace("\r", " ");
                bw.write("% excluded processed formula (non-modal): " +
                        flatFormula + "\n");
                bw.write("% from file " + f.sourceFile + " at line " +
                        f.startLine + "\n");
                continue;
            }

            if (bw == null) {
                System.out.println(processNonModal(new Formula(fnew),
                        typeMap, false));
            }
            else {
                bw.write("thf(ax" + axNum++ + ",axiom," +
                        processNonModal(new Formula(fnew),
                                typeMap, false) + ").\n");
            }
        }
    }

    /** ***************************************************************
     */
    public static boolean protectedRelation(String s) {

        return s.equals("domain") || s.equals("instance") || s.equals("subAttribute") || s.equals("contraryAttribute");
    }

    /** ***************************************************************
     * Decide whether to exclude a formula from THF export.
     *
     * This filter:
     *  - removes unsupported syntactic constructs (quotes, $true/$false),
     *  - removes meta-logical axioms about the class Formula and bare
     *    variables in formula position,
     *  - enforces constraints on how modal/HOL symbols may be used
     *    (never as ordinary individuals in non-modal heads),
     *  - drops a small set of known-bad SUMO terms (problematic_terms).
     *
     * NOTE: Pure arithmetic functions such as AdditionFn, MultiplicationFn,
     * LogFn, SquareRootFn, etc. are NOT excluded here. They are treated as
     * world-free functions and handled via THFnew's type system.
     */
    public static boolean exclude(Formula f, KB kb, Writer out) throws IOException {

        if (debug) System.out.println("exclude(): " + f);

        // Exclude strings (quotes) – THF translation does not support them.
        if (f.getFormula().contains("\"")) {
            out.write("% exclude(): quote\n");
            return true;
        }

        // Exclude formulas containing true/false – we do not yet rely on
        // TPTP's $true / $false constants in this pipeline.
        if (f.getFormula().contains(Formula.LOG_FALSE) ||
                f.getFormula().contains(Formula.LOG_TRUE)) {
            out.write("% exclude(): contains true or false constant\n");
            return true;
        }

        // ISSUE 16
        // Exclude axioms that mention 'Formula' as a type/class. These are
        // meta-logical axioms about the class Formula, not about particular
        // formula-valued arguments, which we handle as (w > $o).
        if (f.getFormula().contains(" Formula)")) {
            if (debug) {
                System.out.println("exclude(): meta-logical axiom with Formula type: "
                        + f.getFormula());
            }
            out.write("% exclude(): meta-logical axiom with Formula type\n");
            return true;
        }

        // Generic: modal operators must not appear as non-head arguments.
        // If the head itself is not a modal relation, any occurrence of a
        // modal relation name in argument position is rejected.
        List<String> args = f.complexArgumentsToArrayListString(0);
        String head = args.get(0);
//        if (!Modals.MODAL_RELATIONS.contains(head)) {
//            for (String a : args) {
//                if (Modals.MODAL_RELATIONS.contains(a)) {
//                    out.write("% exclude(): modal operator used as individual: " + a + "\n");
//                    return true;
//                }
//            }
//        }

        // Generic rule: if the head is NOT in allowedHeads, then none of the
        // arguments may be modal/HOL/formula predicates or modal attributes.
        if (args != null && !args.isEmpty()) {
            head = args.get(0);
            if (!Modals.allowedHeads.contains(head)) {
                for (int i = 1; i < args.size(); i++) {   // skip head
                    String a = args.get(i);
                    if (Modals.MODAL_RELATIONS.contains(a)
                            || Modals.modalAttributes.contains(a)
                            || Modals.RESERVED_MODAL_SYMBOLS.contains(a)
                            || Modals.regHOLpred.contains(a)
                            || Modals.formulaPreds.contains(a)) {
                        if (debug) {
                            System.out.println("% exclude(): modal/HOL symbol used as individual " +
                                    "argument of non-modal head, Symbol " + a + " head: " + head + "\n");
                        }
                        if (out != null)
                            out.write("% exclude(): modal/HOL symbol used as individual " +
                                "argument of non-modal head, Symbol " + a + " head: " + head + "\n");
                        return true;
                    }
                }
            }
        }

        // Exclude domain/subrelation axioms for formula/HOL predicates.
        // These are meta-level typing constraints; we set the types of
        // such predicates explicitly in Modals/THFnew instead.
        if (args.size() >= 2 &&
                (args.get(0).equals("domain")) || (args.get(0).equals("subrelation")))  {
            String p = args.get(1);
            if (Modals.formulaPreds.contains(p) ||
                    Modals.regHOLpred.contains(p)) {
                if (out != null)
                    out.write("% exclude(): domain axiom for formula/HOL predicate: " + p + "\n");
                return true;
            }
        }

//        head = args.get(0);
//        if (Modals.regHOL3pred.contains(head)) {
//            for (String a : args) {
//                if (Modals.modalAttributes.contains(a)) {
//                    if (out != null)
//                        out.write("% exclude(): modal operator used as individual in " + head + ": " + a + "\n");
//                    return true;
//                }
//            }
//        }

        // TODO: Fix that in SUMO
        // Known problematic terms unrelated to the modal embedding.
        List<String> problematic_terms = Arrays.asList("airTemperature", "ListFn", "AssignmentFn", "Organism");
        for (String a : args) {
            if (problematic_terms.contains(a)) {
                if (out != null)
                    out.write("% exclude(): Problematic Term encountered: "+a+"\n");
                return true;
            }
        }
        // META-LOGIC FILTER
        // Exclude any formula where a bare variable is used directly
        // in formula position:
        //   (=> ... ?VAR)
        //   (not ?VAR)   or  (~ ?VAR)
        // Because variables are $i, not $o.
        // This catches the PROP / FORMULA / SITUATION issues in one shot.
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
                        if (out != null)
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
                        if (out != null)
                            out.write("% exclude(): meta-logic (variable under not/~): "
                                + arg0 + "\n");
                        return true;
                    }
                }
            }
        }
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
                        if (out != null)
                            out.write("% excluded(): interior list: " + flat + "\n");
                        return true;
                    }
                }
            }

            // Existing predicate-based exclusion (documentation, format, etc.)
            for (String sub : args) {
                if (excludePred(sub, out)) {
                    String flat = f.toString().replace("\n", " ").replace("\r", " ");
                    if (out != null)
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
                            if (out != null)
                                out.write("% exclude(): modal attribute in protected relation: " +
                                    args.get(0) + " " + args.get(i) + "\n");
                            return true;
                        }
                    }
                }

                // ISSUE 10: exclude domain axioms with modal symbols
                if (args.get(0).equals("domain") &&
                        args.size() > 1 &&
                        Modals.RESERVED_MODAL_SYMBOLS.contains(args.get(1))) {
                    if (out != null)
                        out.write("% exclude(): modal operator in domain: " + args.get(1) + "\n");
                    return true;
                }

                // Ground numeric filtering – unrelated to the modal embedding.
                // This can be revisited later if we want more general numerals.
                for (String s : args) {
                    if (StringUtil.isNumeric(s)) {
                        if (out != null)
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

    // -----------------------------------------------------------------------
    // Private helpers for the Expr-based exclude overload
    // -----------------------------------------------------------------------

    // Bitmask flags for preCheckExpr
    private static final int PC_STR_LITERAL = 1;  // any StrLiteral node
    private static final int PC_TRUE_FALSE  = 2;  // Atom("True") or Atom("False")
    private static final int PC_FORMULA_ARG = 4;  // Atom("Formula") as last arg of some SExpr

    /**
     * Single-pass pre-check: walks the Expr tree <em>once</em> and returns a
     * bitmask of the three conditions checked at the top of
     * {@link #exclude(Expr, KB, Writer)}.
     *
     * <p>Replacing three separate recursive helpers with one traversal avoids
     * redundant pointer-chasing through the Java heap for every formula.
     * An early-exit guard short-circuits as soon as all three flags are set.
     *
     * <p>Bit meanings: {@link #PC_STR_LITERAL}, {@link #PC_TRUE_FALSE},
     * {@link #PC_FORMULA_ARG}.
     */
    private static int preCheckExpr(Expr e, int found) {
        if (found == (PC_STR_LITERAL | PC_TRUE_FALSE | PC_FORMULA_ARG))
            return found;                          // all flags set — early exit
        if (e instanceof Expr.StrLiteral)
            return found | PC_STR_LITERAL;
        if (e instanceof Expr.Atom a) {
            String name = a.name();
            if (name.equals(Formula.LOG_TRUE) || name.equals(Formula.LOG_FALSE))
                return found | PC_TRUE_FALSE;
            return found;
        }
        if (!(e instanceof Expr.SExpr se)) return found;
        if (se.head() != null)
            found = preCheckExpr(se.head(), found);
        List<Expr> args = se.args();
        // Check: Atom("Formula") as the last argument of this SExpr
        if ((found & PC_FORMULA_ARG) == 0 && !args.isEmpty()
                && args.get(args.size() - 1) instanceof Expr.Atom lastAtom
                && "Formula".equals(lastAtom.name()))
            found |= PC_FORMULA_ARG;
        for (Expr arg : args) {
            found = preCheckExpr(arg, found);
            if (found == (PC_STR_LITERAL | PC_TRUE_FALSE | PC_FORMULA_ARG))
                return found;                      // all flags set — early exit
        }
        return found;
    }

    /** Returns true if the Expr tree contains no {@link Expr.Var} or {@link Expr.RowVar} nodes. */
    private static boolean isGroundExpr(Expr e) {
        if (e instanceof Expr.Var || e instanceof Expr.RowVar) return false;
        if (e instanceof Expr.SExpr se) {
            if (se.head() != null && !isGroundExpr(se.head())) return false;
            for (Expr arg : se.args())
                if (!isGroundExpr(arg)) return false;
        }
        return true;
    }

    /** ***************************************************************
     * Expr-based overload of {@link #exclude(Formula, KB, Writer)}.
     *
     * <p>Walks the {@link Expr} tree directly — no {@code toKifString()}
     * round-trip, no {@code new Formula()} allocation.  Every check mirrors
     * its counterpart in the string-based overload.
     *
     * <p>Call sites should prefer this overload whenever a non-null {@code Expr}
     * is already available (e.g. in {@code oneTransExpr} and in the top-level
     * loop of {@code transModalTHF} for {@link FormulaAST} formulas).
     */
    public static boolean exclude(Expr e, KB kb, Writer out) throws IOException {

        if (e == null) return false;
        if (debug) System.out.println("exclude(Expr): " + e.toKifString());

        // Non-SExpr top-level nodes — handle the simple cases and return
        if (e instanceof Expr.StrLiteral) {
            out.write("% exclude(): quote\n");
            return true;
        }
        if (e instanceof Expr.Atom a) {
            String name = a.name();
            if (name.equals(Formula.LOG_TRUE) || name.equals(Formula.LOG_FALSE)) {
                out.write("% exclude(): contains true or false constant\n");
                return true;
            }
            return excludePred(name, out);
        }
        if (!(e instanceof Expr.SExpr se))
            return false;   // Var, RowVar, NumLiteral — not excluded at top level

        // 1–3. Single-pass pre-check for: StrLiteral, true/false, Formula-as-last-arg.
        //      One tree walk instead of three separate recursive calls.
        int flags = preCheckExpr(se, 0);
        if ((flags & PC_STR_LITERAL) != 0) {
            out.write("% exclude(): quote\n");
            return true;
        }
        if ((flags & PC_TRUE_FALSE) != 0) {
            out.write("% exclude(): contains true or false constant\n");
            return true;
        }
        if ((flags & PC_FORMULA_ARG) != 0) {
            if (debug)
                System.out.println("exclude(Expr): meta-logical axiom with Formula type: "
                        + se.toKifString());
            out.write("% exclude(): meta-logical axiom with Formula type\n");
            return true;
        }

        // Pre-checks passed — run the structural checks.
        // excludeExprBody recurses into children via itself, not via exclude(), so
        // preCheckExpr is never re-run on already-verified subtrees.
        return excludeExprBody(se, kb, out);
    }

    /** ***************************************************************
     * Fast-path overload for {@link FormulaAST} formulas.
     *
     * <p>Uses {@code fa.getFormula().contains()} for the three pre-checks
     * (JVM-intrinsic / SIMD-backed string scans) instead of the recursive
     * {@link #preCheckExpr} tree walk, then delegates to
     * {@link #excludeExprBody} for the structural checks.
     * Called from the main loops in {@code transModalTHF} so that
     * {@code preCheckExpr} is not invoked for every one of the ~75 K formulas.
     */
    public static boolean exclude(FormulaAST fa, KB kb, Writer out) throws IOException {

        String s = fa.getFormula();
        if (s.contains("\"")) {
            out.write("% exclude(): quote\n");
            return true;
        }
        if (s.contains(Formula.LOG_FALSE) || s.contains(Formula.LOG_TRUE)) {
            out.write("% exclude(): contains true or false constant\n");
            return true;
        }
        if (s.contains(" Formula)")) {
            if (debug)
                System.out.println("exclude(FormulaAST): meta-logical axiom with Formula type: " + s);
            out.write("% exclude(): meta-logical axiom with Formula type\n");
            return true;
        }
        if (!(fa.expr instanceof Expr.SExpr se)) return false;
        return excludeExprBody(se, kb, out);
    }

    /** ***************************************************************
     * Shared structural body for both {@code exclude(Expr, …)} and
     * {@code exclude(FormulaAST, …)}.
     *
     * <p>Called <em>after</em> the three pre-checks (StrLiteral /
     * LOG_TRUE/FALSE / Formula-arg) have already passed.  Recursion into
     * child {@code SExpr} nodes uses this method directly, so
     * {@link #preCheckExpr} is never re-run on already-verified subtrees
     * (eliminates the O(n²) redundancy of calling {@code exclude(Expr)}
     * recursively).
     */
    private static boolean excludeExprBody(Expr.SExpr se, KB kb, Writer out) throws IOException {

        String headName = se.headName();
        List<Expr> args = se.args();

        // 4. Generic rule: if head is NOT in allowedHeads, no direct arg may be a
        //    modal/HOL/formula atom symbol
        if (headName != null && !Modals.allowedHeads.contains(headName)) {
            for (Expr arg : args) {
                if (arg instanceof Expr.Atom argAtom) {
                    String argName = argAtom.name();
                    if (Modals.MODAL_RELATIONS.contains(argName)
                            || Modals.modalAttributes.contains(argName)
                            || Modals.RESERVED_MODAL_SYMBOLS.contains(argName)
                            || Modals.regHOLpred.contains(argName)
                            || Modals.formulaPreds.contains(argName)) {
                        if (debug)
                            System.out.println("% exclude(Expr): modal/HOL symbol used as individual "
                                    + "argument of non-modal head, Symbol " + argName
                                    + " head: " + headName);
                        if (out != null)
                            out.write("% exclude(): modal/HOL symbol used as individual "
                                    + "argument of non-modal head, Symbol " + argName
                                    + " head: " + headName + "\n");
                        return true;
                    }
                }
            }
        }

        // 5. Exclude domain / subrelation axioms for formula / HOL predicates
        if (("domain".equals(headName) || "subrelation".equals(headName)) && !args.isEmpty()) {
            if (args.get(0) instanceof Expr.Atom firstArg) {
                String p = firstArg.name();
                if (Modals.formulaPreds.contains(p) || Modals.regHOLpred.contains(p)) {
                    if (out != null)
                        out.write("% exclude(): domain axiom for formula/HOL predicate: " + p + "\n");
                    return true;
                }
            }
        }

        // 6. Known problematic terms (head or direct atom args)
        List<String> problematic_terms = Arrays.asList(
                "airTemperature", "ListFn", "AssignmentFn", "Organism");
        if (headName != null && problematic_terms.contains(headName)) {
            if (out != null)
                out.write("% exclude(): Problematic Term encountered: " + headName + "\n");
            return true;
        }
        for (Expr arg : args) {
            if (arg instanceof Expr.Atom argAtom && problematic_terms.contains(argAtom.name())) {
                if (out != null)
                    out.write("% exclude(): Problematic Term encountered: " + argAtom.name() + "\n");
                return true;
            }
        }

        // 7. META-LOGIC FILTER: bare variable in formula position
        //    Case 1: (=> antecedent ?VAR)
        if ("=>".equals(headName) && args.size() >= 2 && args.get(1) instanceof Expr.Var conseqVar) {
            if (debug)
                System.out.println("exclude(Expr): META-LOGIC pattern: variable as consequent of => : "
                        + conseqVar.name() + " in " + se.toKifString());
            if (out != null)
                out.write("% exclude(): meta-logic (variable as consequent of =>): "
                        + conseqVar.name() + "\n");
            return true;
        }
        //    Case 2: (not ?VAR) or (~ ?VAR)
        if (("not".equals(headName) || "~".equals(headName))
                && !args.isEmpty() && args.get(0) instanceof Expr.Var notVar) {
            if (debug)
                System.out.println("exclude(Expr): META-LOGIC pattern: variable under not/~ : "
                        + notVar.name() + " in " + se.toKifString());
            if (out != null)
                out.write("% exclude(): meta-logic (variable under not/~): " + notVar.name() + "\n");
            return true;
        }

        // 8. Recurse into SExpr children — call this method directly so preCheckExpr is
        //    not re-run on subtrees that were already validated at the top level.
        for (Expr arg : args) {
            if (arg instanceof Expr.SExpr argSe) {
                if (excludeExprBody(argSe, kb, out)) {
                    if (out != null)
                        out.write("% excluded(): interior list: " + se.toKifString() + "\n");
                    return true;
                }
            }
        }

        // 9. excludePred check on each direct atom arg
        for (Expr arg : args) {
            if (arg instanceof Expr.Atom argAtom) {
                if (excludePred(argAtom.name(), out)) {
                    if (out != null)
                        out.write("% excluded(): term from excludePred: " + se.toKifString() + "\n");
                    return true;
                }
            }
        }

        // 10. Additional checks when the formula is ground (no variables)
        if (isGroundExpr(se)) {
            if (debug) System.out.println("exclude(Expr): is ground: " + se.toKifString());

            // ISSUE 11: modal attributes must not appear as args of protected relations
            if (headName != null && protectedRelation(headName)) {
                for (Expr arg : args) {
                    if (arg instanceof Expr.Atom argAtom
                            && Modals.modalAttributes.contains(argAtom.name())) {
                        if (out != null)
                            out.write("% exclude(): modal attribute in protected relation: "
                                    + headName + " " + argAtom.name() + "\n");
                        return true;
                    }
                }
            }

            // ISSUE 10: domain axioms where the first arg is a reserved modal symbol
            if ("domain".equals(headName) && !args.isEmpty()
                    && args.get(0) instanceof Expr.Atom firstArg
                    && Modals.RESERVED_MODAL_SYMBOLS.contains(firstArg.name())) {
                if (out != null)
                    out.write("% exclude(): modal operator in domain: " + firstArg.name() + "\n");
                return true;
            }

            // Ground numeric filtering
            for (Expr arg : args) {
                if (arg instanceof Expr.NumLiteral numLit) {
                    String s = numLit.value();
                    if (out != null)
                        out.write("% exclude(): is numeric(2): \n");
                    if (s.contains(".") || s.contains("-") || s.length() > 1)
                        return true;
                    if (s.charAt(0) < '1' || s.charAt(0) > '6')
                        return true;
                    if (debug) System.out.println("exclude(Expr): numeric arg not excluded: " + s);
                }
            }
        }

        // 11. Top-level predicate check
        return headName != null && excludePred(headName, out);
    }

    /** ***************************************************************
     * Expr-based overload of {@link #excludeNonModal(Formula, KB, Writer)}.
     * Used from inner loops where only an {@code Expr} is available (e.g.
     * preprocessed formulas in {@code oneTransNonModalExpr}).
     * Does NOT check {@code badUsageSymbols} — callers handle that.
     */
    public static boolean excludeNonModal(Expr e, KB kb, Writer out) throws IOException {

        if (e == null) return false;
        if (debug) System.out.println("excludeNonModal(Expr): " + e.toKifString());

        // 1. Non-SExpr top-level nodes
        if (e instanceof Expr.StrLiteral) {
            out.write("% exclude(): quote (String Literal)\n");
            return true;
        }
        if (e instanceof Expr.Atom a) {
            String name = a.name();
            if (name.equals(Formula.LOG_TRUE) || name.equals(Formula.LOG_FALSE)) {
                out.write("% exclude(): contains true or false constant\n");
                return true;
            }
            return excludePred(name, out);
        }
        if (!(e instanceof Expr.SExpr se)) return false;

        // 2. Batch pre-check: StrLiteral, LOG_TRUE/FALSE, or "Formula" as arg anywhere in tree
        int flags = preCheckExpr(se, 0);
        if ((flags & PC_STR_LITERAL) != 0) {
            out.write("% exclude(): quote (String Literal)\n");
            return true;
        }
        if ((flags & PC_TRUE_FALSE) != 0) {
            out.write("% exclude(): contains true or false constant\n");
            return true;
        }
        if ((flags & PC_FORMULA_ARG) != 0) {
            if (debug) System.out.println("excludeNonModal(Expr): meta-logical axiom with Formula type: " + se.toKifString());
            out.write("% exclude(): meta-logical axiom with Formula type\n");
            return true;
        }

        return excludeNonModalExprBody(se, kb, out);
    }

    /** ***************************************************************
     * Fast-path overload for {@link FormulaAST} formulas in the plain-THF
     * main loop.
     *
     * <p>Handles {@code badUsageSymbols} (identity-based set lookup on the
     * original {@code FormulaAST} object), then uses
     * {@code fa.getFormula().contains()} for the three pre-checks instead of
     * the recursive {@link #preCheckExpr} tree walk, then delegates to
     * {@link #excludeNonModalExprBody}.
     */
    public static boolean excludeNonModal(FormulaAST fa, KB kb, Writer out) throws IOException {

        // badUsageSymbols stores the original FormulaAST objects; must check here.
        if (THFnew.badUsageSymbols.contains(fa)) {
            String flat = fa.getFormula().replace("\n", " ").replace("\r", " ");
            out.write("% exclude(): bad usage symbol: " + flat + "\n");
            return true;
        }
        String s = fa.getFormula();
        if (s.contains("\"")) {
            out.write("% exclude(): quote (String Literal)\n");
            return true;
        }
        if (s.contains(Formula.LOG_FALSE) || s.contains(Formula.LOG_TRUE)) {
            out.write("% exclude(): contains true or false constant\n");
            return true;
        }
        if (s.contains(" Formula)")) {
            if (debug) System.out.println("excludeNonModal(FormulaAST): meta-logical axiom with Formula type: " + s);
            out.write("% exclude(): meta-logical axiom with Formula type\n");
            return true;
        }
        if (!(fa.expr instanceof Expr.SExpr se)) return false;
        return excludeNonModalExprBody(se, kb, out);
    }

    /** ***************************************************************
     * Shared structural body for both {@code excludeNonModal(Expr, …)} and
     * {@code excludeNonModal(FormulaAST, …)}.
     *
     * <p>Called after the three pre-checks have already passed.
     * Recursion uses this method directly so {@link #preCheckExpr} is never
     * re-run on already-verified subtrees.
     */
    private static boolean excludeNonModalExprBody(Expr.SExpr se, KB kb, Writer out) throws IOException {

        List<Expr> args = se.args();
        String headName = se.head() instanceof Expr.Atom ha ? ha.name() : null;

        // 3. Problematic terms
        List<String> problematic_terms = Arrays.asList("airTemperature", "ListFn", "AssignmentFn", "Organism");
        if (headName != null && problematic_terms.contains(headName)) {
            if (out != null) out.write("% exclude(): Problematic Term encountered: " + headName + "\n");
            return true;
        }
        for (Expr arg : args) {
            if (arg instanceof Expr.Atom argAtom && problematic_terms.contains(argAtom.name())) {
                if (out != null) out.write("% exclude(): Problematic Term encountered: " + argAtom.name() + "\n");
                return true;
            }
        }

        // 4. Non-modal: domain/subrelation axiom whose 2nd arg is itself a Relation — drop it
        if (("domain".equals(headName) || "subrelation".equals(headName)) && args.size() >= 2) {
            if (args.get(1) instanceof Expr.Atom p && kb.isInstanceOf(p.name(), "Relation")) {
                out.write("% excludeNonModal(): meta-logic domain/subrelation over relation: " + p.name() + "\n");
                return true;
            }
        }

        // 5. Meta-logic: variable as consequent of =>, or variable directly under not/~
        if ("=>".equals(headName) && args.size() >= 2 && args.get(1) instanceof Expr.Var conseqVar) {
            if (debug) System.out.println("excludeNonModal(Expr): META-LOGIC: variable as consequent of =>: " + conseqVar.name());
            if (out != null) out.write("% exclude(): meta-logic (variable as consequent of =>): " + conseqVar.name() + "\n");
            return true;
        }
        if (("not".equals(headName) || "~".equals(headName)) && !args.isEmpty() && args.get(0) instanceof Expr.Var notVar) {
            if (debug) System.out.println("excludeNonModal(Expr): META-LOGIC: variable under not/~: " + notVar.name());
            if (out != null) out.write("% exclude(): meta-logic (variable under not/~): " + notVar.name() + "\n");
            return true;
        }

        // 6. Recurse into SExpr sub-args — call body directly, not excludeNonModal(),
        //    to avoid re-running preCheckExpr on already-verified subtrees.
        for (Expr arg : args) {
            if (arg instanceof Expr.SExpr argSe) {
                if (excludeNonModalExprBody(argSe, kb, out)) {
                    if (out != null) out.write("% excluded(): interior list: " + se.toKifString() + "\n");
                    return true;
                }
            }
        }

        // 7. excludePred on each direct Atom arg
        for (Expr arg : args) {
            if (arg instanceof Expr.Atom argAtom) {
                if (excludePred(argAtom.name(), out)) {
                    if (out != null) out.write("% excluded(): term from excludePred: " + se.toKifString() + "\n");
                    return true;
                }
            }
        }

        // 8. Ground checks — numeric args
        if (isGroundExpr(se)) {
            if (debug) System.out.println("excludeNonModal(Expr): is ground: " + se.toKifString());
            for (Expr arg : args) {
                if (arg instanceof Expr.NumLiteral numLit) {
                    String s = numLit.value();
                    if (out != null) out.write("% exclude(): is numeric(2): \n");
                    if (s.contains(".") || s.contains("-") || s.length() > 1) return true;
                    if (s.charAt(0) < '1' || s.charAt(0) > '6') return true;
                    if (debug) System.out.println("excludeNonModal(Expr): numeric arg not excluded: " + s);
                }
            }
        }

        // 9. Top-level predicate check
        return headName != null && excludePred(headName, out);
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
                pred.equals("codeMapping") ||
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
//                pred.equals("knows") ||  // handled in header
//                pred.equals("believes") ||  //handled in header
//                pred.equals("desires") || //handled in header
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
     * Build a THF type string from a SUMO signature.
     *
     * @param functor  the predicate / function symbol whose type we build
     * @param sig      the list of SUMO argument/result types (as strings)
     * @param kb       the KB, used for isInstanceOf checks
     * @param function true if this is a function symbol (first entry in sig is range)
     *
     * NOTE:
     *   - For "modal" predicates in Modals.formulaPreds / regHOLpred we treat
     *     Formula arguments as (w > $o) (e.g. confersObligation, desires, ...).
     *   - KappaFn is a special case: although it appears in formulaPreds,
     *     in the existing SUMO axioms its Formula argument is used as a *plain*
     *     proposition (already world-instantiated), so we type that argument
     *     as $o instead.
     */
    public static String sigString(String functor, List<String> sig, KB kb, boolean function) {

        if (debug) System.out.println("sigString(): functor: " + functor + " sig: " + sig);
        StringBuilder sb = new StringBuilder();
        boolean first = false;
        String range = "";
        if (function)
            first = true;
        // By default, modal/HOL predicates treat Formula args as (w > $o).
        boolean formulaAsWorldFunction =
                (Modals.formulaPreds.contains(functor) ||
                        Modals.regHOLpred.contains(functor));
        // EXCEPTION: KappaFn – we override and treat its Formula argument as $o,
        // because in the current axioms (e.g. ax478) it is applied to a fully
        // evaluated sentence like (sideOfFigure S POL W1), not a function F: w>$o.
        ArrayList<String> exceptionFormulas =
                new ArrayList<>(Arrays.asList(
                        "KappaFn",
                        "increasesLikelihood",
                        "holdsRight",
                        "ProbabilityFn",
                        "hasPurpose",
                        "containsFormula"
                ));

        if (exceptionFormulas.contains(functor))
            formulaAsWorldFunction = false;

        for (String t : sig) {
            if (t.equals(""))
                continue;

            if (first) {
                // First entry is the result type (for functions).
                range = t;
                first = false;
            }
            else if (kb.isInstanceOf(t, "Formula") || t.equals("Formula")) {
                if (formulaAsWorldFunction) {
                    // For deontic/epistemic predicates etc. we treat Formula args as
                    // functions from worlds to booleans: F : w > $o.
//                    sb.append("(w > $o) > ");
                    sb.append("$o > ");
                }
                else {
                    // For KappaFn and any other non-modal functor with a Formula arg
                    // we treat the argument as a plain proposition: $o.
                    sb.append("$o > ");
                }
            }
            else if (kb.isInstanceOf(t, "World") || t.equals("World"))
                // World arguments are typed as w.
                sb.append("w > ");
            else
                // Everything else is an individual.
                sb.append("$i > ");
        }

        // Result type: for functions we respect the range, for relations we return $o.
        if (function) {
            if (kb.isInstanceOf(range, "Formula") || range.equals("Formula")) {
                // Functions returning Formula yield (w > $o).
                sb.append("(w > $o)");
            }
            else
                sb.append("$i");
        }
        else {
            sb.append("$o");
        }
        return sb.toString();
    }

    /** ***************************************************************
     */
    // Non-modal version: only $i and $o.
    // - If pred has a __N suffix, use N as arity: all args $i, result $o / $i.
    // - Otherwise, reuse the old sigString logic, but map World -> $i (no 'w').
    public static String sigStringNonModal(String pred,
                                           List<String> sig,
                                           KB kb,
                                           boolean function) {

        // Try to read a numeric suffix, e.g. partition__4 -> 4
        Integer suffixNum = getSuffixNumber(pred);

        if (suffixNum != null && suffixNum > 0) {
            int arity = suffixNum;

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arity; i++) {
                sb.append("$i > ");
            }

            if (function)
                sb.append("$i");   // functions return an individual
            else {
                sb.append("$o");   // relations return a boolean
                predicateTerms.add(pred);
            }

            return sb.toString();
        }

        // ---------- NO SUFFIX: fall back to signature-based logic ----------
        if (debug) System.out.println("sigStringNonModal(): sig: " + sig);

        StringBuilder sb = new StringBuilder();
        boolean first = false;
        String range = "";

        if (function)
            first = true;

        for (String t : sig) {   // e.g. [, Organism, GeographicArea]
            if (t.equals(""))
                continue;

            if (first) {
                range = t;
                first = false;
            }
            else if (kb.isInstanceOf(t, "Formula") || t.equals("Formula")) {
                sb.append("$o > ");
            }
            else {
                // Non-modal: treat World arguments as ordinary individuals too.
                // We deliberately DO NOT emit 'w > ' here.
                sb.append("$i > ");
            }
        }

        if (function) {
            if (kb.isInstanceOf(range, "Formula") || range.equals("Formula")) {
                sb.append("$o");
                predicateTerms.add(pred);
            }
            else
                sb.append("$i");   // includes World-as-range => $i
        }
        else {
            sb.append("$o");
            predicateTerms.add(pred);
        }

        return sb.toString();
    }

    /** ***************************************************************
     * Recursively collect all numeric literals from a formula string.
     * Collects integers, floats, and negatives (e.g. -1 → n___1).
     */
    private static void collectNumbersFromFormula(String fstr, Set<String> numbers) {

        Formula f = new Formula(fstr);
        if (f.atom()) {
            if (StringUtil.isNumeric(fstr))
                numbers.add(fstr);
            return;
        }
        List<String> args = f.complexArgumentsToArrayListString(0);
        if (args != null) {
            for (String arg : args)
                collectNumbersFromFormula(arg, numbers);
        }
    }

    /** ***************************************************************
     * Scan all KB formulas and return the set of every numeric literal
     * that appears (e.g. "24", "0.0", "360.0").  These will be declared
     * as thf(n__24_tp,type,(n__24 : $i)). so that hideNumbers=true
     * translations are well-typed.  Dots are normalised to underscores
     * to match the output of translateWord (0.0 -> n__0_0).
     */
    public static Set<String> collectNumbers(KB kb) {

        Set<String> numbers = new TreeSet<>();
        for (Formula f : kb.formulaMap.values())
            collectNumbersFromFormula(f.getFormula(), numbers);
        return numbers;
    }

    /** ***************************************************************
     * Emit a THF type declaration for every numeric literal in numbers.
     * Applies the same normalisation as translateWord: '.' -> '_'.
     */
    public static void writeIntegerTypes(Set<String> numbers, Writer out) throws IOException {

        for (String n : numbers) {
            String normalized = n.replace('.', '_').replace('-', '_');
            out.write("thf(n__" + normalized + "_tp,type,(n__" + normalized + " : $i)).\n");
        }
    }

    /** ***************************************************************
     */
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
    public static void writeTypes(KB kb, Writer out, Set<String> numbers) throws IOException {

        writeIntegerTypes(numbers, out);
        for (String t : kb.terms) {
            // ISSUE 2
            // 1. Skip modal helper symbols – they already have correct types in the header.
            if (Modals.RESERVED_MODAL_SYMBOLS.contains(t))
                continue;
            // 2. Skip HOL-rewrite predicates (regHOLpred / regHOL3pred) – their types are
            //    declared in the header by getTHFHeader(). Writing them again here
            //    (possibly with a conflicting type like $i) causes a duplicate-declaration
            //    parse error in LEO-III / Vampire. modalAttributes are NOT in the header
            //    and are handled by the else-if branch below, so do NOT skip them here.
            if (Modals.regHOLpred.contains(t) || Modals.regHOL3pred.contains(t))
                continue;
            if (excludeForTypedef(t,out))
                continue;
            if (kb.isInstanceOf(t,"Relation")) {
                // Start Of first Signature
                List<String> baseSig = kb.kbCache.signatures.get(t);
                if (baseSig == null) {
                    System.err.println("Error in THFnew.writeTypes(): bad sig for " + t);
                    continue;
                }
                // Work on a local copy to build the THF type
                List<String> sig = new ArrayList<>(baseSig);

                // VariableArityRelations with no domain declarations get an empty string
                // as their only sig entry (the range placeholder). copyNewPredFromVariableArity
                // fills expanded variants with the same empty string, so sigString() skips all
                // arg positions and produces "(w > $o)" instead of "($i > ... > w > $o)".
                // Normalise: replace empty arg entries (index 1+) with "Entity".
                if (variableArity(kb, t)) {
                    for (int _si = 1; _si < sig.size(); _si++) {
                        String _st = sig.get(_si);
                        if (_st == null || _st.isEmpty()) sig.set(_si, "Entity");
                    }
                }

                // ISSUE 14
                // Check that the sigStr aligns with the __NUM of the term:
                Integer suffixNum = getSuffixNumber(t);
                String baseHead = Modals.baseFunctor(t);
                // For relations:
                //  - skip logical operators and equality
                //  - skip rigid relations (instance, subclass, etc.)
                //  - skip symbols with explicitly defined modal types (reserved header)
                //  - skip modal relations
                //  - every other relation gets a trailing "World" argument
                if (!Formula.isLogicalOperator(t) && !t.equals("equals")) {
                    if (!Modals.RIGID_RELATIONS.contains(baseHead)
                            && !Modals.RESERVED_MODAL_SYMBOLS.contains(baseHead)
                            && !Modals.regHOLpred.contains(baseHead)) {
                        sig.add("World");
                        if (suffixNum != null) suffixNum+=1;
                    }
                }

                if (t == null) {
                    System.err.println("Error in THFnew.writeTypes(): bad sig for " + t);
                    continue;
                }

                boolean isFunction = false;
                String SUMOtoTPTPformula = SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true);
                if (kb.isInstanceOf(t,"Function")) {
                    out.write("thf(" + SUMOtoTPTPformula + "_tp,type,(" + SUMOtoTPTPformula + " : "); // write signature
                    isFunction = true;
                }
                else
                    out.write("thf(" + SUMOtoTPTPformula + "_tp,type,(" + SUMOtoTPTPformula + " : "); // write signature

                if (suffixNum != null && !sig.isEmpty() && sig.size() > (suffixNum+1)) {
                    while (sig.size() > (suffixNum+1)) {
                        sig.remove(sig.size() - 1);   // remove from end until sizes match
                    }
                }

                String sigStr;
                if (Modals.regHOLpred.contains(baseHead) || Modals.regHOL3pred.contains(baseHead)) {
                    sigStr = "m";
                    out.write( sigStr + ")).\n");
                }
                else {
                    sigStr = sigString(t, sig,kb,isFunction);
                    out.write("(" + sigStr + "))).\n");
                }
                // End Of first Signature
                // Start of Second Signature
                String typeStr = "$i";
                if (Modals.MODAL_RELATIONS.contains(baseHead) && !Modals.regHOLpred.contains(baseHead)) {
                    typeStr = "m";
                }
                out.write("thf(" + SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true) + "_m_tp,type,(" +
                        SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),false) + " : " + typeStr + ")).\n"); // write relation constant
                // End of Second Signature
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
    public static void writeTypesNonModal(KB kb, Writer out) throws IOException {

        writeIntegerTypes(collectNumbers(kb), out);
        for (String pred : kb.kbCache.signatures.keySet()) {
            // Derive base predicate name (strip __N or __NFn if present)
            String base = pred;
            Matcher m = Pattern.compile("^(.+?)__(\\d+)(Fn)?$").matcher(pred);
            if (m.matches())
                base = m.group(1) + (m.group(3) == null ? "" : m.group(3));

            if (!kb.isInstanceOf(base, "Relation"))
                continue;   // skip non-relations / junk

            List<String> sig = new ArrayList<>(kb.kbCache.signatures.get(pred));
            boolean isFunction = kb.isInstanceOf(base, "Function");

            String functor = SUMOformulaToTPTPformula.translateWord(pred, pred.charAt(0), true);

            out.write("thf(" + functor + "_tp,type,(" + functor + " : (");
            String sigStr = sigStringNonModal(pred, sig, kb, isFunction);
            out.write(sigStr + "))).\n");

            // Also treat the predicate symbol itself as an individual
//            out.write("thf(" + functor + "_tp_ind,type,(" + functor + " : $i)).\n");
        }
    }

    /** ***************************************************************
     */
    // Scans all KIF formulas once before THF translation to detect predicates whose
    // arguments are used inconsistently with their declared signatures. In particular,
    // if a predicate expects an $i argument but receives a formula (a list), it is
    // flagged as a badUsageSymbol and will later be excluded from translation.
    public static void analyzeBadUsages(KB kb) {

        for (Formula f : kb.formulaMap.values()) {
            analyzeFormula(f, kb);
        }
    }

    /** ***************************************************************
     */
    // Recursively analyzes a single formula for typing mismatches: if a predicate's
    // argument position expects a non-Formula type (e.g., Entity/$i) but the argument
    // is itself a formula (list), the predicate is marked as badly used.
    private static void analyzeFormula(Formula f, KB kb) {

        if (f == null)
            return;

        // We only care about list formulas of the form (head arg1 arg2 ...)
        if (f.atom() || !f.listP())
            return;

        String head = f.car();
        if (head == null)
            return;

        if (head.equals("termFormat") || head.equals("documentation") || head.equals("format"))
            return;

        // Get argument strings; this may legitimately return null in some cases.
        List<String> args = f.complexArgumentsToArrayListString(1);
        if (args == null || args.isEmpty())
            return;

        // Check against the declared signature, if any
        List<String> sig = kb.kbCache.signatures.get(head);
        if (debug) System.out.println("analyzeFormula(): head: " + head + " sig: " + sig);
        if (sig != null && sig.size() > 1) {
            // sig[0] is "", last is range; arguments are 1..sig.size()-2
            int maxArgs = Math.min(args.size(), sig.size() - 1);
            for (int i = 0; i < maxArgs; i++) {
                String expectedType = sig.get(i + 1);   // KIF type: Entity, Formula, etc.
                String arg = args.get(i);
                if (debug) System.out.println("analyzeFormula(): arg " + i + " | " + arg);
                // If we expect a non-Formula type but the argument is itself a formula (list),
                // this symbol is being used as if that position were a formula.
                if ((!"Formula".equals(expectedType) && (Formula.listP(arg) || (predicateTerms.contains(arg))))){
                    THFnew.badUsageSymbols.add(f);
                    break;
                }
            }
        }

        // Recurse on subformulas in the arguments
        for (String s : args) {
            if (Formula.listP(s))
                analyzeFormula(new Formula(s), kb);
        }
    }

    // =======================================================================
    // Expr-based per-formula translation (FormulaAST path)
    // These mirror oneTrans / oneTransNonModal but operate on Expr trees.
    // =======================================================================

    /** ***************************************************************
     * Expr-based equivalent of {@link #oneTrans}.
     * Used when the formula is a {@link FormulaAST} with a non-null
     * {@code expr} field.
     *
     * <p>Pipeline:
     * <ol>
     *   <li>Run {@link Modals#processModalsExpr} on the original expr for
     *       type-info only.</li>
     *   <li>Preprocess the original expr with
     *       {@link FormulaPreprocessor#preProcessExpr}.</li>
     *   <li>Build the typeMap from domain/range restrictions plus world-var
     *       entries from step 1.</li>
     *   <li>For each preprocessed Expr, apply modal processing again and
     *       translate to THF via {@link ExprToTHF#translate}.</li>
     * </ol>
     */
    public static void oneTransExpr(KB kb, FormulaAST fa, PrintWriter bw)
            throws IOException {

        bw.write("% original: " + fa.getFormula() + "\n" +
                "% from file " + fa.sourceFile + " at line " + fa.startLine + "\n");

        // Step 1: modal pass on the original expr for TYPE INFO only
        Map.Entry<Expr, Map<String, Set<String>>> modalResult =
                Modals.processModalsExpr(fa.expr, kb);
        Expr resExpr = modalResult.getKey();

        if (resExpr == null) return;

        FormulaPreprocessor fp = new FormulaPreprocessor();

        // Step 2: preprocess ORIGINAL fa (not the modalized one)
        Set<Expr> processed = fp.preProcessExpr(fa, false, kb);
        if (processed == null || processed.isEmpty()) return;

        // Step 3: build typeMap from modalized expr
        Map<String, Set<String>> typeMap = new HashMap<>();
        typeMap.putAll(fp.findTypeRestrictionsExpr(resExpr, kb));
        typeMap.putAll(modalResult.getValue()); // world var types (?W0, ?W1 → {"World"})

        // Add primary world var explicitly (mirrors oneTrans's makeWorldVar call)
        Set<String> worldTypes = new HashSet<>(Collections.singleton("World"));
        String primaryWorldVar = Modals.makeWorldVarExpr(fa.expr);
        typeMap.put(primaryWorldVar, worldTypes);

        // Mark formula-typed variables from modalAttribute forms
        Modals.markModalAttributeFormulaVarsExpr(fa.expr, typeMap);

        // Step 4: for each preprocessed Expr, apply modals and translate to THF
        for (Expr e : processed) {
            // Skip formulas where a predicate variable was not expanded by preProcessExpr
            // (no KB instances found for the constraining type).  Applying a $i-typed
            // variable as a function causes a Vampire SIGSEGV during THF parsing.
            // The string-based oneTrans() implicitly drops these via exclude() when
            // SUMOformulaToTPTPformula.process() returns empty for pred-var formulas.
            if (SUMOKBtoTPTPKB.hasUnresolvedPredVar(e)) continue;

            Map.Entry<Expr, Map<String, Set<String>>> fmodalResult =
                    Modals.processModalsExpr(e, kb);
            Expr fmodal = fmodalResult.getKey();
            if (fmodal == null) continue;

            if (exclude(fmodal, kb, bw)) continue;

            String thf = ExprToTHF.translate(fmodal, false, typeMap);
            bw.println("thf(ax" + axNum++ + ",axiom," + thf + ").\n");
        }
    }

    /** ***************************************************************
     * Expr-based equivalent of {@link #oneTransNonModal}.
     * Used when the formula is a {@link FormulaAST} with a non-null
     * {@code expr} field (plain/non-modal THF generation).
     *
     * <p>Pipeline:
     * <ol>
     *   <li>Preprocess the expr with
     *       {@link FormulaPreprocessor#preProcessExpr}.</li>
     *   <li>Build the typeMap from domain/range restrictions.</li>
     *   <li>For each preprocessed Expr, translate to THF (non-modal) via
     *       {@link ExprToTHF#translateNonModal}.</li>
     * </ol>
     */
    public static void oneTransNonModalExpr(KB kb, FormulaAST fa, Writer bw)
            throws IOException {

        bw.write("% original: " + fa.getFormula() + "\n" +
                "% from file " + fa.sourceFile + " at line " + fa.startLine + "\n");

        FormulaPreprocessor fp = new FormulaPreprocessor();

        // Step 1: preprocess original expr
        Set<Expr> processed = fp.preProcessExpr(fa, false, kb);
        if (processed == null || processed.isEmpty()) return;

        // Step 2: build typeMap from the original (no modal processing)
        Map<String, Set<String>> typeMap = new HashMap<>();
        typeMap.putAll(fp.findTypeRestrictionsExpr(fa.expr, kb));

        // Step 3: translate each preprocessed Expr
        for (Expr e : processed) {
            if (SUMOKBtoTPTPKB.hasUnresolvedPredVar(e)) continue;
            if (excludeNonModal(e, kb, bw)) {
                String flat = fa.getFormula().replace("\n", " ").replace("\r", " ");
                bw.write("% excluded processed formula (non-modal): " + flat + "\n");
                bw.write("% from file " + fa.sourceFile + " at line " + fa.startLine + "\n");
                continue;
            }
            String thf = ExprToTHF.translateNonModal(e, false, typeMap);
            bw.write("thf(ax" + axNum++ + ",axiom," + thf + ").\n");
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
        String filename = kbDir + sep + kb.name + "_modals.thf";
        try (Writer fstream = new FileWriter(filename);
             PrintWriter out = new PrintWriter(new BufferedWriter(fstream))) {
            // Warm Up
            // Use the Expr-based path for FormulaAST formulas so that row-var expansion
            // goes up to arity 7 (matching oneTransExpr's behaviour) and all __N predicate
            // variants are registered in kb.terms/signatures BEFORE writeTypes() runs.
            // The string-based preProcess() only expands to RowVars.MAX_ARITY=5, so any
            // arity-6/7 variant created later by oneTransExpr would be missing a type
            // declaration (Vampire SIGSEGV due to undeclared predicate).
            FormulaPreprocessor fp = new FormulaPreprocessor();
            for (Formula f : kb.formulaMap.values()) {
                // We ignore the results; we just want preProcessRecurse()
                // to run and call copyNewPredFromVariableArity(...)
                if (f instanceof FormulaAST fa && fa.expr != null)
                    fp.preProcessExpr(fa, false, kb);
                else
                    fp.preProcess(f, false, kb);
            }
            // Pre-collect all integer literals so every n__N constant gets a type declaration.
            SUMOformulaToTPTPformula.setHideNumbers(true);
            Set<String> numbers = collectNumbers(kb);
            // Write at the end of the header the hard coded types because they use some from the auto-generated ones.
            out.write(Modals.getTHFHeader(kb) + "\n");
            writeTypes(kb, out, numbers);
            for (Formula f : kb.formulaMap.values()) {
                if (debug) System.out.println("THFnew.transModalTHF(): " + f);
                boolean excluded;
                if (f instanceof FormulaAST fa && fa.expr != null) {
                    excluded = exclude(fa, kb, out);   // FormulaAST overload: fast string pre-checks
                    if (!excluded)
                        oneTransExpr(kb, fa, out);
                } else {
                    excluded = exclude(f, kb, out);
                    if (!excluded) {
                        System.out.println("THFnew.transModalTHF(): fallback to string-based translation for: "
                                + f.sourceFile + " line " + f.startLine + ": " + f.getFormula());
                        oneTrans(kb, f, out);
                    }
                }
                if (excluded) {
                    String flatFormula = f.getFormula().replace("\n", " ").replace("\r", " ");
                    String stripped = flatFormula.replaceAll("[^\\p{ASCII}]", "");
                    out.write("% excluded: " + stripped + "\n");
                    out.write("% from file " + f.sourceFile + " at line " + f.startLine + "\n");
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
    public static void transPlainTHF(KB kb) {

        String kbDir = KBmanager.getMgr().getPref("kbDir");
        String sep = File.separator;
        String filename = kbDir + sep + kb.name + "_plain.thf";

        if (debug) System.out.println("\n\nTHFnew.transPlainTHF()");
        try (Writer fstream = new FileWriter(filename);
             Writer out = new BufferedWriter(fstream)) {

            // Use Expr-based expansion in the warm-up so __N predicates up to arity 7
            // are registered before writeTypesNonModal() runs (same fix as transModalTHF).
            FormulaPreprocessor fp = new FormulaPreprocessor();
            for (Formula f : kb.formulaMap.values()) {
                // We ignore the results; we just want preProcessRecurse()
                // to run and call copyNewPredFromVariableArity(...)
                if (f instanceof FormulaAST fa && fa.expr != null)
                    fp.preProcessExpr(fa, false, kb);
                else
                    fp.preProcess(f, false, kb);
            }

            // For pure { $i, $o } we probably don't need a big header.
            // Optionally: out.write(getPlainTHFHeader() + "\n");
            writeTypesNonModal(kb, out);

            analyzeBadUsages(kb);
            if (debug) System.out.println("Predicate Terms: " + predicateTerms);

            for (Formula f : kb.formulaMap.values()) {
                if (debug) System.out.println("THFnew.transPlainTHF(): " + f);
                boolean excluded;
                if (f instanceof FormulaAST fa && fa.expr != null) {
                    excluded = excludeNonModal(fa, kb, out);  // FormulaAST overload: badUsage + fast string pre-checks
                    if (!excluded)
                        oneTransNonModalExpr(kb, fa, out);
                } else {
                    excluded = excludeNonModal(f, kb, out);
                    if (!excluded) {
                        System.out.println("THFnew.transPlainTHF(): fallback to string-based translation for: "
                                + f.sourceFile + " line " + f.startLine + ": " + f.getFormula());
                        oneTransNonModal(kb, f, out);
                    }
                }
                if (excluded) {
                    String flatFormula = f.getFormula()
                            .replace("\n", " ").replace("\r", " ");
                    String stripped = flatFormula.replaceAll("[^\\p{ASCII}]", "");
                    out.write("% excluded (non-modal): " + stripped + "\n");
                    out.write("% from file " + f.sourceFile + " at line " +
                            f.startLine + "\n");
                }
            }
            System.out.println("\n\nTHFnew.transPlainTHF(): Result written to file " + filename);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** ***************************************************************
     */
    public static boolean excludeNonModal(Formula f, KB kb, Writer out) throws IOException {

        if (debug) System.out.println("exclude(): " + f);

        // Excludes any formula whose main predicate has been flagged as a mixed-result
        // symbol or a bad-usage symbol. This prevents the generation of THF axioms that
        // Vampire would reject due to type inconsistencies discovered during analysis.
        if (THFnew.badUsageSymbols.contains(f)) {
            String flat = f.toString().replace("\n", " ").replace("\r", " ");
            out.write("% exclude(): bad usage symbol: " + flat + "\n");
            return true;
        }

        // Exclude strings (quotes)
        if (f.getFormula().contains("\"")) {
            out.write("% exclude(): quote (String Literal)\n");
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
        List<String> args = f.complexArgumentsToArrayListString(0);

        // TODO: Fix that in SUMO
        // Problematic Terms
        List<String> problematic_terms = Arrays.asList("airTemperature", "ListFn", "AssignmentFn", "Organism");
        for (String a : args) {
            if (problematic_terms.contains(a)) {
                out.write("% exclude(): Problematic Term encountered: \n");
                return true;
            }
        }

        // META-LOGIC FILTER
        // Exclude any formula where a bare variable is used directly
        // in formula position:
        //   (=> ... ?VAR)
        //   (not ?VAR)   or  (~ ?VAR)
        // Because variables are $i, not $o.
        // This catches the PROP / FORMULA / SITUATION issues in one shot.
        if (f.listP()) {
            String op = f.car();
            // arguments starting at position 1 (operator is at 0)
            List<String> opArgs = f.complexArgumentsToArrayListString(1);

            if (args != null && !args.isEmpty()) {
                String head = args.get(0);

                // Non-modal: we cannot treat relations as individuals,
                // so drop domain/subrelation axioms whose 2nd arg is a relation.
                if ((head.equals("domain") || head.equals("subrelation"))
                        && args.size() >= 2) {
                    String p = args.get(1);
                    if (kb.isInstanceOf(p, "Relation")) {
                        out.write("% excludeNonModal(): meta-logic domain/subrelation over relation: "
                                + p + "\n");
                        return true;
                    }
                }
            }

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
        // ALWAYS recurse into interior lists (this catches nested cases,
        // since exclude() is called on all sub-formulas)
        if (args != null) {
            if (debug) {
                System.out.println("exclude(): Formula: " + f.getFormula());
                System.out.println("exclude(): complexArgumentsToArrayListString(0): " + args);
            }
            for (String s : args) {
                if (Formula.listP(s)) {
                    if (excludeNonModal(new Formula(s), kb, out)) {
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
     //   try {
            //oneTrans(kb,f,null);
     //   }
      //  catch (IOException ex) {
      //      ex.printStackTrace();
      //  }
        fstr = "(=>\n" +
                "    (instance ?J TransitwayJunction)\n" +
                "    (exists (?W1 ?W2)\n" +
                "        (and\n" +
                "            (instance ?W1 Transitway)\n" +
                "            (instance ?W2 Transitway)\n" +
                "            (connects ?J ?W1 ?W2)\n" +
                "            (not\n" +
                "                (equal ?W1 ?W2)))))";
        f = new Formula(fstr);
        //try {
        //    oneTrans(kb,f,null);
        //}
        //catch (IOException ex) {
        //    ex.printStackTrace();
        //}
        fstr = "(=> " +
                "(and " +
                  "(instance ?POLICY NoChildrenPolicy) " +
                  "(policyLocationCoverage ?POLICY ?LOC) " +
                  "(policyOwner ?AGENT ?POLICY)) " +
                "(deprivesNorm ?AGENT Permission " +
                  "(exists (?CHILD) " +
                    "(and " +
                      "(instance ?CHILD HumanChild) " +
                      "(located ?CHILD ?LOC)))))";
        f = new Formula(fstr);
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
        System.out.println("  r - THF tRanslation without modals");
        System.out.println("  --one \"formula\" - THF translate One statement");
        System.out.println("  t - test");
        System.out.println("  h - show this help");
        System.out.println("  (no option) - plain THF (no modals, only $i and $o)");
    }

    /** ***************************************************************
     * Wait for background TPTP generation to complete.
     * Only needed for full-KB export modes (-m, -r), not for
     * single-formula translation (--one).
     */
    public static void waitForBackgroundGeneration() {

        if (!TPTPGenerationManager.waitForTHFModal(600)) {
            System.out.println("THFnew.main(): Background generation not ready, generating THF Modal synchronously");
        }
        if (!TPTPGenerationManager.waitForTHFPlain(600)) {
            System.out.println("THFnew.main(): Background generation not ready, generating THF Plain synchronously");
        }
        if (!TPTPGenerationManager.waitForTFF(600)) {
            System.out.println("THFnew.main(): Background generation not ready, generating TFF synchronously");
        }
    }

    /** ***************************************************************
     */
    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("INFO in THFnew.main()");
        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        System.out.println(argMap);
        if (argMap.containsKey("h") || argMap.isEmpty()) {
            showHelp();
        }
        else {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            System.out.println("THFnew.main(): KB loaded");
            if (!kb.errors.isEmpty())
                System.err.println("THFnew.main(): KB loaded with non-fatal errors: " + kb.errors);
            System.out.println("contains one : " + argMap.containsKey("one"));
            System.out.println("has one arg: " + (argMap.containsKey("one") && argMap.get("one").size() == 1));
            if (argMap.containsKey("one") && argMap.get("one").size() == 1) {
                // Single formula translation - no need to wait for background TPTP generation
                System.out.println("THFnew.main(): translate to THF (with modals)");
                PrintWriter writer = new PrintWriter(System.out, true);
                try {
                    String kifStr = argMap.get("one").get(0);
                    SuokifVisitor visitor = SuokifVisitor.parseString(kifStr);
                    FormulaAST fa = visitor.result.isEmpty() ? null
                            : visitor.result.values().iterator().next();
                    if (fa != null && fa.expr != null) {
                        oneTransExpr(kb, fa, writer);
                    } else {
                        System.out.println("THFnew.main(): FormulaAST parse failed or expr is null — falling back to string-based translation");
                        oneTrans(kb, new Formula(kifStr), writer);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    writer.flush();
                }
            }
            else if (argMap.containsKey("r")) {
                waitForBackgroundGeneration();
                System.out.println("THFnew.main(): translate to plain THF (no modals)");
                transPlainTHF(kb);
            }
            else if (argMap.containsKey("t")) {
                System.out.println("THFnew.main(): test");
                test(kb);
            }
            else if (argMap.containsKey("m")) {
                waitForBackgroundGeneration();
                System.out.println("THFnew.main(): translate to THF with modals");
                transModalTHF(kb);
            }
            else {
                showHelp();
            }
        }
    }
}