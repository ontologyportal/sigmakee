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
                } else
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
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("^(s__[A-Za-z0-9]+__)(\\d+)$")
                    .matcher(functor);
            if (m.matches()) {
                int argCount = args.size();

                // TODO: Fix the hard typed values
                List<String> worldArgs = List.of("?W1", "?W2");
                // Don't increase the term's ArityValue if it contains a World argument.
                if (argCount > 0 && (worldArgs.contains(args.get(argCount - 1)))) {
                    argCount--;
                }
                int oldN = Integer.parseInt(m.group(2));
                if (argCount != oldN) {
                    functor = m.group(1) + argCount;
                }
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

        // TODO: Implement a more generic solution for variable types
        // TODO: Identify why the typeMap.get(v) most of the times is Null
        if (v.equals("?W1") || v.equals("?W2") ) {
            return "w";
        }

        if (typeMap.get(v) == null) {
            return "$i";
        }

        if (typeMap.get(v).contains("Formula")) {
            // Treat "Formula" variables as functions from worlds to booleans,
            // as per Alex Steen / TQM10: F : w > $o, used as F @ W.
            return "(w > $o)";
        }

        if (typeMap.get(v).contains("World"))
            return "w";

        if (typeMap.get(v).contains("Modal"))
            return "m";

        return "$i";
    }

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
            oneVar = SUMOformulaToTPTPformula.translateWord(s,s.charAt(0),false);
            qlist.append(oneVar).append(":").append(thftype).append(",");
        }
        if (qlist.length() > 1)
            qlist.deleteCharAt(qlist.length() - 1);  // delete final comma
        return qlist.toString();
    }

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


    public static void oneTrans(KB kb, Formula f, Writer bw) throws IOException {

        bw.write("% original: " + f.getFormula() + "\n" +
                "% from file " + f.sourceFile + " at line " + f.startLine + "\n");

        // 1) Modal pass on the original f, for TYPE INFO ONLY
        Formula res = Modals.processModals(f, kb);
        if (res != null) {

            FormulaPreprocessor fp = new FormulaPreprocessor();

            // 2) IMPORTANT: preprocess ORIGINAL f, not res.
            //    So processed formulas do NOT yet contain worlds.
            Set<Formula> processed = fp.preProcess(f, false, kb);

            // 3) Build typeMap from res (modalised original) as before
            res.varTypeCache.clear();
            Map<String, Set<String>> typeMap = new HashMap<>();
            typeMap.putAll(fp.findAllTypeRestrictions(res, kb));
            typeMap.putAll(res.varTypeCache);

            Set<String> types = new HashSet<>();
            types.add("World");
            String worldVar = makeWorldVar(kb, f);
            typeMap.put(worldVar, types);

            markModalAttributeFormulaVars(f, typeMap);

            // 4) For each processed formula, NOW apply Modals and then THFnew
            for (Formula fnew : processed) {

                // Single, correct modal/world pass per processed formula
                Formula fmodal = Modals.processModals(fnew, kb);
                if (fmodal == null) {
                    continue;
                }

                if (exclude(fmodal, kb, bw)) {
                    continue;
                }

                if (bw == null) {
                    System.out.println(process(new Formula(fmodal), typeMap, false));
                } else {
                    bw.write("thf(ax" + axNum++ + ",axiom," +
                            process(new Formula(fmodal), typeMap, false) + ").\n");
                }
            }
        }
    }


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

    /**
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

        // ISSUE 17
        // ISSUE 21
        // ISSUE 22
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

                        out.write("% exclude(): modal/HOL symbol used as individual " +
                                "argument of non-modal head: " + a + "\n");
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

        // TODO: Fix that in SUMO
        // Known problematic terms unrelated to the modal embedding.
        List<String> problematic_terms = Arrays.asList("airTemperature", "ListFn", "AssignmentFn", "Organism");
        for (String a : args) {
            if (problematic_terms.contains(a)) {
                out.write("% exclude(): Problematic Term encountered: "+a+"\n");
                return true;
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
                        Modals.RESERVED_MODAL_SYMBOLS.contains(args.get(1))) {
                    out.write("% exclude(): modal operator in domain: " + args.get(1) + "\n");
                    return true;
                }

                // Ground numeric filtering – unrelated to the modal embedding.
                // This can be revisited later if we want more general numerals.
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

    /**
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

        if (exceptionFormulas.contains(functor)) {
            formulaAsWorldFunction = false;
        }

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
            else if (kb.isInstanceOf(t, "World") || t.equals("World")) {
                // World arguments are typed as w.
                sb.append("w > ");
            }
            else {
                // Everything else is an individual.
                sb.append("$i > ");
            }
        }

        // Result type: for functions we respect the range, for relations we return $o.
        if (function) {
            if (kb.isInstanceOf(range, "Formula") || range.equals("Formula")) {
                // Functions returning Formula yield (w > $o).
                sb.append("(w > $o)");
            }
            else {
                sb.append("$i");
            }
        }
        else {
            sb.append("$o");
        }

        return sb.toString();
    }


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
            }else
                sb.append("$i");   // includes World-as-range => $i
        }
        else {
            sb.append("$o");
            predicateTerms.add(pred);
        }

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
            if (Modals.RESERVED_MODAL_SYMBOLS.contains(t)) {
                continue;
            }

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

                // ISSUE 14
                // Check that the sigStr alligns with the __NUM of the term:
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
                            && !Modals.regHOLpred.contains(baseHead)
                    ) {
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
                }else
                    out.write("thf(" + SUMOtoTPTPformula + "_tp,type,(" + SUMOtoTPTPformula + " : "); // write signature

                if (suffixNum != null && !sig.isEmpty() && sig.size() > (suffixNum+1)) {
                    while (sig.size() > (suffixNum+1)) {
                        sig.remove(sig.size() - 1);   // remove from end until sizes match
                    }
                }

                String sigStr;
                if (Modals.regHOLpred.contains(baseHead)){
                    sigStr = "m";
                    out.write( sigStr + ")).\n");
                }else{
                    sigStr = sigString(t, sig,kb,isFunction);
                    out.write("(" + sigStr + "))).\n");
                }


                // End Of first Signature

                // Start of Second Signature
                String typeStr = "$i";
                if (Modals.MODAL_RELATIONS.contains(baseHead) && !Modals.regHOLpred.contains(baseHead)) {
                    typeStr = "m";
                }

                out.write("thf(" + SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true) + "_tp,type,(" +
                        SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),false) + " : "+typeStr+")).\n"); // write relation constant
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

    public static void writeTypesNonModal(KB kb, Writer out) throws IOException {

        writeIntegerTypes(kb, out);

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


    // Scans all KIF formulas once before THF translation to detect predicates whose
    // arguments are used inconsistently with their declared signatures. In particular,
    // if a predicate expects an $i argument but receives a formula (a list), it is
    // flagged as a badUsageSymbol and will later be excluded from translation.
    public static void analyzeBadUsages(KB kb) {

        for (Formula f : kb.formulaMap.values()) {
            analyzeFormula(f, kb);
        }
    }

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
             Writer out = new BufferedWriter(fstream)) {

            // Warm Up
            FormulaPreprocessor fp = new FormulaPreprocessor();
            for (Formula f : kb.formulaMap.values()) {
                // We ignore the results; we just want preProcessRecurse()
                // to run and call copyNewPredFromVariableArity(...)
                fp.preProcess(f, false, kb);
            }

            writeTypes(kb,out);

            // Write at the end of the header the hard coded types because they use some from the auto-generated ones.
            out.write(Modals.getTHFHeader() + "\n");
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

    public static void transPlainTHF(KB kb) {

        String kbDir = KBmanager.getMgr().getPref("kbDir");
        String sep = File.separator;
        String filename = kbDir + sep + kb.name + "_plain.thf";

        if (debug) System.out.println("\n\nTHFnew.transPlainTHF()");
        try (Writer fstream = new FileWriter(filename);
             Writer out = new BufferedWriter(fstream)) {

            FormulaPreprocessor fp = new FormulaPreprocessor();
            for (Formula f : kb.formulaMap.values()) {
                // We ignore the results; we just want preProcessRecurse()
                // to run and call copyNewPredFromVariableArity(...)
                fp.preProcess(f, false, kb);
            }

            // For pure { $i, $o } we probably don't need a big header.
            // Optionally: out.write(getPlainTHFHeader() + "\n");
            writeTypesNonModal(kb, out);

            analyzeBadUsages(kb);
            if (debug) System.out.println("Predicate Terms: " + predicateTerms);

            for (Formula f : kb.formulaMap.values()) {
                if (debug) System.out.println("THFnew.transPlainTHF(): " + f);
                if (!excludeNonModal(f, kb, out))
                    oneTransNonModal(kb, f, out);
                else {
                    String flatFormula = f.getFormula()
                            .replace("\n", " ").replace("\r", " ");
                    out.write("% excluded (non-modal): " + flatFormula + "\n");
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
        System.out.println("  (no option) - plain THF (no modals, only $i and $o)");
    }

    /** ***************************************************************
     */
    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("INFO in THFnew.main()");
        System.out.println("args:" + (args == null ? 0 : args.length) + " : " +
                Arrays.toString(args));

        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        System.out.println("THFnew.main(): KB loaded");
        if (!kb.errors.isEmpty()) {
            System.err.println("Errors: " + kb.errors);
            return;
        }

        if (args == null || args.length == 0) {
            // DEFAULT: plain (non-modal) THF
            System.out.println("THFnew.main(): translate to plain THF (no modals)");
            transPlainTHF(kb);
        }
        else if ("-h".equals(args[0])) {
            showHelp();
        }
        else if ("-t".equals(args[0])) {
            System.out.println("THFnew.main(): test");
            test(kb);
        }
        else if ("-m".equals(args[0])) {
            System.out.println("THFnew.main(): translate to THF with modals");
            transModalTHF(kb);
        }
        else {
            showHelp();
        }
    }


}