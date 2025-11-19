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
            // Non-modal mode: no special treatment for modal predicates.
//            if (Modals.regHOLpred.contains(f.getFormula()))
//                hasArguments = true;
            return SUMOformulaToTPTPformula.translateWord(f.getFormula(),ttype,hasArguments);
        }
        Formula car = f.carAsFormula();
        //System.out.println("THFnew.processRecurse(): car: " + car);
        //System.out.println("THFnew.processRecurse(): car: " + car.theFormula);
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
            argStr.delete(argStr.length()-2,argStr.length());  // remove final arg separator
            String result = Formula.LP + SUMOformulaToTPTPformula.translateWord(car.getFormula(),
                    StreamTokenizer.TT_WORD,true) + " @ " + argStr.substring(0,argStr.length()-1) + Formula.RP;
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

        // IMPORTANT: Formula variables should be $o, not $i.
        if (typeMap.get(v).contains("Formula"))
            return "$o";
//        if (typeMap.get(v).contains("Formula"))
//            return "(w > $o)";
//        if (typeMap.get(v).contains("World"))
//            return "w";
//        if (typeMap.get(v).contains("Modal"))
//            return "m";
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

    /** ***************************************************************
     */
        public static void oneTrans(KB kb, Formula f, Writer bw) throws IOException {

            bw.write("% original: " + f.getFormula() + "\n" +
                    "% from file " + f.sourceFile + " at line " + f.startLine + "\n");

            // *** NON-MODAL: do NOT call Modals.processModals ***
            Formula res = f;

            if (res != null) {
                FormulaPreprocessor fp = new FormulaPreprocessor();
                Set<Formula> processed = fp.preProcess(res, false, kb);

                if (debug) System.out.println("THFnew.oneTrans(): res.varTypeCache: " + res.varTypeCache);
                if (debug) System.out.println("THFnew.oneTrans(): processed: " + processed);

                res.varTypeCache.clear();
                Map<String, Set<String>> typeMap = fp.findAllTypeRestrictions(res, kb);
                if (debug) System.out.println("THFnew.oneTrans(): typeMap(1): " + typeMap);
                typeMap.putAll(res.varTypeCache);
                if (debug) System.out.println("THFnew.oneTrans(): typeMap(2): " + typeMap);

                // *** NON-MODAL: no worldVar, no World types added ***

                for (Formula fnew : processed) {
                    if (debug) System.out.println("THFnew.oneTrans(): variableArity(kb,fnew.car()): " +
                            variableArity(kb,fnew.car()));

                    // you can keep this hack or drop it; it’s harmless without worlds
                    if (variableArity(kb, fnew.car()))
                        fnew = adjustArity(kb, fnew);

                    // *** NON-MODAL: no special (instance ?X Class) wrappings ***

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
    public static boolean protectedRelation(String s) {

        return s.equals("domain") || s.equals("instance") || s.equals("subAttribute") || s.equals("contraryAttribute");
    }

    /** ***************************************************************
     * Formulas to exclude from the translation
     */
    public static boolean exclude(Formula f, KB kb, Writer out) throws IOException {

        String kif = f.getFormula();

        // --- 1) Drop modal / propositional-attitude schema axioms in non-modal mode ---

        // (domain knows 1 CognitiveAgent), (range knows 2 Formula), etc.
        List<String> ar0 = f.complexArgumentsToArrayListString(0);
        if (!ar0.isEmpty()) {
            String pred = ar0.get(0);

            boolean isDomainLike =
                    "domain".equals(pred) ||
                            "range".equals(pred) ||
                            "domainSubclass".equals(pred) ||
                            "rangeSubclass".equals(pred) ||
                            "domainDomainSubclass".equals(pred);

            if (isDomainLike && ar0.size() > 1) {
                String relName = ar0.get(1);   // the relation being typed, e.g. knows

                // Any schema about these “modal-ish” relations is trouble in THF.
                if ("knows".equals(relName) ||
                        "believes".equals(relName) ||
                        "desires".equals(relName) ||
                        "modalAttribute".equals(relName)) {

                    out.write("% exclude(): modal schema axiom: " + kif + "\n");
                    return true;
                }
            }
        }

        // TEMP: drop all modalAttribute axioms to fully disable modal logic
        if (f.getFormula().contains("modalAttribute")) {
            out.write("% exclude(): modalAttribute axiom (modal semantics disabled)\n");
            return true;
        }

        if (debug) System.out.println("exclude(): " + f);
        if (f.getFormula().contains("\"")) {
            out.write("% exclude(): quote" + "\n");
            return true;
        }
        if (f.getFormula().contains(Formula.LOG_FALSE) || f.getFormula().contains(Formula.LOG_TRUE)) {
            out.write("% exclude(): contains true or false constant" + "\n");
            return true;
        }
        if (f.isGround()) {
            if (debug) System.out.println("exclude(): is ground: " + f);
            List<String> ar = f.complexArgumentsToArrayListString(0);
            if (protectedRelation(ar.get(0)) && Modals.modalAttributes.contains(ar.get(1))) {
                out.write("% exclude(): modal attribute in protected relation: " + ar.get(0) +
                        Formula.SPACE + ar.get(1) + "\n");
                return true;  // defined as type "$m" directly in THF
            }
            for (String s : ar) {
                if (Formula.listP(s) && exclude(new Formula(s),kb,out)) {
                    String kif_var = f.getFormula().replace("\n", " ");
                    out.write("% excluded(): interior list: " + kif_var);
                    return true;
                }
                if (debug) System.out.println("exclude(): arguments: " + ar);
                if (debug) System.out.println("exclude(): testing: " + s);
                if (StringUtil.isNumeric(s)) {
                    out.write("% exclude(): is numeric(2): \n");
                    if (s.contains(".") || s.contains("-") || s.length() > 1)
                        return true;
                    if (s.charAt(0) < '1' || s.charAt(0) > '6')
                        return true;
                    if (debug) System.out.println("exclude(): not excluded");
                }
            }
        }
        String pred = f.car();
        return excludePred(pred,out);
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
//            pred.equals("knows") ||  // handled in header
//            pred.equals("believes") ||  //handled in header
//            pred.equals("desires") ||  //handled in header
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
        for (String t : sig) {
            if (t.equals(""))
                continue;
            if (first) {
                range = t;
                first = false;
            }
//            else if (kb.isInstanceOf(t,"Formula") || t.equals("Formula"))
//                sb.append("$o > ");
//            else if (kb.isInstanceOf(t,"World") || t.equals("World"))
//                sb.append("w > ");
            else{
                // Arguments: Formula → $o, everything else → $i
                if (kb.isInstanceOf(t,"Formula") || "Formula".equals(t))
                    sb.append("$o > ");
                else
                    sb.append("$i > ");
            }
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

    /** ***************************************************************
     */
    public static void writeTypes(KB kb, Writer out) throws IOException {

        writeIntegerTypes(kb,out);
        for (String t : kb.terms) {
            if (excludeForTypedef(t,out))
                continue;
            if (kb.isInstanceOf(t,"Relation")) {
                List<String> sig = kb.kbCache.signatures.get(t);
                if (sig == null) {
                    sig = new ArrayList<>();
                    sig.add("");                  // dummy 0th element
                    kb.kbCache.signatures.put(t, sig);
                }

                // REMOVE this block in non-modal mode:
//                if (!Formula.isLogicalOperator(t) && !t.equals("equals")) { // make sure to update the signature
//                    sig.add("World");
//                }

                // --- FIX: normalize signatures for __n variants ---
                // Pattern: base__3, base__4, base__6, ...
                Matcher m = Pattern.compile("(.+)__(\\d+)$").matcher(t);
                if (m.matches()) {
                    int suffix = Integer.parseInt(m.group(2));
                    int desiredSize = suffix + 1;

                    // pad if too short
                    while (sig.size() < desiredSize) {
                        sig.add("Entity");             // generic sort -> $i
                    }
                    // trim if too long (e.g., leftover "World")
                    if (sig.size() > desiredSize) {
                        sig = new ArrayList<>(sig.subList(0, desiredSize));
                        kb.kbCache.signatures.put(t, sig);
                    }
                }

                if (debug)
                    System.out.println("THFnew.writeTypes(): normalized sig " + sig + " for " + t);

                // (no world-arg injection in non-modal mode)
                if (t == null) {
                    System.err.println("Error in THFnew.writeTypes(): bad sig for " + t);
                    continue;
                }

                boolean isFunction = kb.isInstanceOf(t,"Function");
                out.write("thf(" +
                        SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true) +
                        "_tp,type,(" +
                        SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true) +
                        " : (");

                String sigStr = sigString(sig,kb,isFunction);
                out.write(sigStr + "))).\n");

                out.write("thf(" +
                        SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true) +
                        "_tp,type,(" +
                        SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),false) +
                        " : $i)).\n");
            }
//            else if (Modals.modalAttributes.contains(t))
//                out.write("thf(" + SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true) + "_tp,type,(" +
//                        SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),false) + " : $m)).\n"); // write relation constant
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
            // DISABLE MODALS
//            out.write(Modals.getTHFHeader() + "\n");
             out.write("% Non-modal THF translation (Modals disabled)\n");

            writeTypes(kb,out);
            for (Formula f : kb.formulaMap.values()) {
                if (debug) System.out.println("THFnew.transModalTHF(): " + f);
                if (!exclude(f,kb,out))
                    oneTrans(kb,f,out);
                else {
                    String kif = f.getFormula().replace("\n", " ");
                    out.write("% excluded: " + kif + "\n" +
                            "% from file " + f.sourceFile + " at line " + f.startLine + "\n");
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
