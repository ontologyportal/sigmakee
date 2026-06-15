package com.articulate.sigma.trans;

import com.articulate.sigma.parsing.*;
import com.articulate.sigma.*;
import com.articulate.sigma.Formula;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.utils.LoggingUtils;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class THFnew {

    public static boolean debug = false;
    public static boolean writeKifComments = false; // write the original SUO-KIF as a comment in the THF output file
    public static int axNum = 0;
    public static Set<Formula> badUsageSymbols = new HashSet<>();
    public static Set<String> predicateTerms = new HashSet<>(); //Terms that return $o instead of $i
    private static final int PC_STR_LITERAL = 1;  // any StrLiteral node
    private static final int PC_TRUE_FALSE  = 2;  // Atom("True") or Atom("False")
    private static final int PC_FORMULA_ARG = 4;

    /*****************************************************************
     * Write the knowledge base to the file directory SUMO_plain.thf
     * @param kb the knowledge base
     */
    public static void transPlainTHF(KB kb) {

        String kbDir = KBmanager.getMgr().getPref("kbDir");
        String sep = File.separator;
        String filename = kbDir + sep + kb.name + "_plain.thf";
        try (Writer fstream = new FileWriter(filename);
            Writer out = new BufferedWriter(fstream)) {
            FormulaPreprocessor fp = new FormulaPreprocessor();
            for (Formula f : kb.formulaMap.values()) {
                if (f instanceof Formula fa && fa.expr != null) fp.preProcessExpr(fa, false, kb);
                else LoggingUtils.log("ERROR", "Error in parsing FormulaAST");
            }
            writeTypesNonModal(kb, out);
            analyzeBadUsages(kb);
            int i = 1;
            int total = kb.formulaMap.values().size();
            for (Formula f : kb.formulaMap.values()) {
                String flatFormula = f.getFormula().replace("\n", " ").replace("\r", " ");
                String stripped = flatFormula.replaceAll("[^\\p{ASCII}]", "");
                boolean excluded;
                if (writeKifComments) {
                    out.write("% original: " + stripped + "\n");
                    out.write("% from file " + f.sourceFile + " at line " + f.startLine + "\n");
                }
                if (f instanceof Formula fa && fa.expr != null) {
                    excluded = excludeNonModal(fa, kb, out);
                    if (!excluded) oneTransNonModalExpr(kb, fa, out);
                } 
                else {
                    excluded = excludeNonModal(f, kb, out);
                    LoggingUtils.log("ERROR", "Error in parsing FormulaAST");
                }
                if (excluded && writeKifComments) {
                    out.write("% excluded (non-modal): " + stripped + "\n");
                    out.write("% from file " + f.sourceFile + " at line " + f.startLine + "\n");
                }
                i++;
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /*****************************************************************
    */
    public static void transModalTHF(KB kb) {

        long start = System.nanoTime();
        String filename = KBmanager.getMgr().getPref("kbDir") + File.separator + kb.name + "_modals.thf";
        try (Writer fstream = new FileWriter(filename);
            PrintWriter out = new PrintWriter(new BufferedWriter(fstream))) {
            FormulaPreprocessor fp = new FormulaPreprocessor();
            for (Formula f : kb.formulaMap.values()) {
                if (f instanceof Formula fa && fa.expr != null) fp.preProcessExpr(fa, false, kb);
                else LoggingUtils.log("ERROR", "Error in parsing FormulaAST");
            }
            SUMOformulaToTPTPformula.setHideNumbers(true);
            Set<String> numbers = collectNumbers(kb);
            out.write(Modals.getTHFHeader(kb) + "\n");
            writeTypes(kb, out, numbers);
            int i = 1;
            int total = kb.formulaMap.values().size();
            for (Formula f : kb.formulaMap.values()) {
                String flatFormula = f.getFormula().replace("\n", " ").replace("\r", " ");
                String stripped = flatFormula.replaceAll("[^\\p{ASCII}]", "");
                boolean excluded;
                if (writeKifComments) {
                    out.write("% original: " + stripped + "\n");
                    out.write("% from file " + f.sourceFile + " at line " + f.startLine + "\n");
                }
                if (f instanceof Formula fa && fa.expr != null) {
                    excluded = exclude(fa, kb, out);
                    if (!excluded) oneTransExpr(kb, fa, out);
                } else {
                    LoggingUtils.log("ERROR", "Error in parsing FormulaAST");
                    excluded = exclude(f, kb, out);
                }
                if (excluded && writeKifComments) {
                    out.write("% excluded: " + stripped + "\n");
                    out.write("% from file " + f.sourceFile + " at line " + f.startLine + "\n");
                }
                i++;
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /*****************************************************************
     */
    public static void writeTypesNonModal(KB kb, Writer out) throws IOException {

        writeIntegerTypes(collectNumbers(kb), out);
        for (String pred : kb.kbCache.signatures.keySet()) {
            String base = pred;
            Matcher m = Pattern.compile("^(.+?)__(\\d+)(Fn)?$").matcher(pred);
            if (m.matches()) base = m.group(1) + (m.group(3) == null ? "" : m.group(3));
            if (!kb.isInstanceOf(base, "Relation")) continue;
            List<String> sig = new ArrayList<>(kb.kbCache.signatures.get(pred));
            boolean isFunction = kb.isInstanceOf(base, "Function");
            String functor = SUMOformulaToTPTPformula.translateWord(pred, pred.charAt(0), true);
            out.write("thf(" + functor + "_tp,type,(" + functor + " : (");
            String sigStr = sigStringNonModal(pred, sig, kb, isFunction);
            out.write(sigStr + "))).\n");
            String mentionedFunctor = SUMOformulaToTPTPformula.translateWord(pred, pred.charAt(0), false);
            out.write("thf(" + functor + "_m_tp,type,(" + mentionedFunctor + " : $i)).\n");
        }
        Set<String> alreadyDeclared = kb.kbCache.signatures.keySet();
        for (String t : kb.terms) {
            if (alreadyDeclared.contains(t)) continue;
            if (excludeForTypedef(t, out)) continue;
            if (kb.isInstanceOf(t, "Relation")) continue;
            if (StringUtil.isNumeric(t)) continue;
            String functor = SUMOformulaToTPTPformula.translateWord(t, t.charAt(0), true);
            out.write("thf(" + functor + "_tp,type,(" + functor + " : $i)).\n");
        }
    }

    /***************************************************************
     */
    private static String processQuant(Formula f, String op, List<String> args, Map<String, Set<String>> typeMap) {

        if (args.size() < 2) {
            System.err.println("Error in THFnew.processQuant(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        else {
            if (args.get(0) != null) {
                Formula varlist = new Formula(args.get(0));
                List<String> vars = varlist.argumentsToArrayListString(0);
                StringBuilder varStr = new StringBuilder();
                varStr.append(generateQList(f,typeMap,new HashSet(vars)));
                String opStr = " ! ";
                if (op.equals("exists")) opStr = " ? ";
                return Formula.LP + opStr + "[" + varStr + "] : (" +processRecurse(new Formula(args.get(1)),typeMap) + "))";
            }
            else {
                LoggingUtils.log("ERROR", "null arguments to " + op + " in " + f);
                return "";
            }
        }
    }

    /***************************************************************
     */
    private static String processConjDisj(Formula f, Formula car, List<String> args, Map<String, Set<String>> typeMap) {

        String op = car.getFormula();
        if (args.size() < 2) {
            System.err.println("Error in THFnew.processConjDisj(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        String tptpOp = "&";
        if (op.equals(Formula.OR)) tptpOp = "|";
        if (op.equals(Formula.XOR)) tptpOp = "<~>";
        StringBuilder sb = new StringBuilder();
        sb.append(Formula.LP).append(processRecurse(new Formula(args.get(0)),typeMap));
        for (int i = 1; i < args.size(); i++) sb.append(Formula.SPACE).append(tptpOp).append(Formula.SPACE).append(processRecurse(new Formula(args.get(i)),typeMap));
        sb.append(Formula.RP);
        return sb.toString();
    }

    /***************************************************************
     * Process Logical operators
     * @param f Formula to be processed
     * @param car
     * @param args
     * @param typeMap]
     */
    public static String processLogOp(Formula f, Formula car, List<String> args, Map<String, Set<String>> typeMap) {

        String op = car.getFormula();
        if (op.equals(Formula.AND)) return processConjDisj(f,car,args,typeMap);
        if (op.equals(Formula.IF)) {
            if (args.size() < 2) {
                LoggingUtils.log("ERROR", "Wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else {
                if (KBmanager.getMgr().prover == KBmanager.Prover.EPROVER) return Formula.LP + processRecurse(new Formula(args.get(0)),typeMap) + " => " + Formula.LP + processRecurse(new Formula(args.get(1)),typeMap) + "))";
                else return Formula.LP + processRecurse(new Formula(args.get(0)),typeMap) + " => " + processRecurse(new Formula(args.get(1)),typeMap) + Formula.RP;
            }
        }
        if (op.equals(Formula.IFF)) {
            if (args.size() < 2) {
                System.err.println("Error in THFnew.processLogOp(): wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else return "((" + processRecurse(new Formula(args.get(0)),typeMap) + " => " +
                processRecurse(new Formula(args.get(1)),typeMap) + ") & (" +
                processRecurse(new Formula(args.get(1)),typeMap) + " => " +
                processRecurse(new Formula(args.get(0)),typeMap) + "))";
        }
        if (op.equals(Formula.OR)) return processConjDisj(f,car,args,typeMap);
        if (op.equals(Formula.XOR)) return processConjDisj(f,car,args,typeMap);
        if (op.equals(Formula.NOT)) {
            if (args.size() != 1) {
                LoggingUtils.log("ERROR", "Wrong number of arguments to " + op + " in " + f);
                return "";
            }
            else return "~(" + processRecurse(new Formula(args.get(0)),typeMap) + Formula.RP;
        }
        if (op.equals(Formula.UQUANT) || op.equals(Formula.EQUANT)) return processQuant(f,op,args,typeMap);
        LoggingUtils.log("ERROR", "Bad logical operator " + op + " in " + f);
        return "";
    }

    /***************************************************************
     */
    public static String processEquals(Formula f, Formula car, List<String> args, Map<String, Set<String>> typeMap) {

        String op = car.getFormula();
        if (args.size() != 2) {
            System.err.println("Error in THFnew.processCompOp(): wrong number of arguments to " + op + " in " + f);
            return "";
        }
        if (op.startsWith(Formula.EQUAL)) return Formula.LP + processRecurse(new Formula(args.get(0)),typeMap) + " = " + processRecurse(new Formula(args.get(1)),typeMap) + Formula.RP;
        LoggingUtils.log("ERROR", "bad comparison operator " + op + " in " + f);
        return "";
    }

    /***************************************************************
     */
    public static String processRecurse(Formula f, Map<String, Set<String>> typeMap) {

        if (f == null) return "";
        if (f.atom()) {
            int ttype = f.getFormula().charAt(0);
            if (Character.isDigit(ttype)) ttype = StreamTokenizer_s.TT_NUMBER;
            boolean hasArguments = false; // if it's a modal op, don't add the __m suffix
            if (Modals.regHOLpred.contains(f.getFormula()) || Modals.regHOL3pred.contains(f.getFormula())) hasArguments = true;
            return SUMOformulaToTPTPformula.translateWord(f.getFormula(), ttype, hasArguments);
        }
        Formula car = f.carAsFormula();
        List<String> args = f.complexArgumentsToArrayListString(1);
        if (car.listP()) {
            LoggingUtils.log("ERROR", "Formula " + f);
            return "";
        }
        if (Formula.isLogicalOperator(car.getFormula())) return processLogOp(f, car, args, typeMap);
        else if (car.getFormula().equals(Formula.EQUAL)) return processEquals(f, car, args, typeMap);
        else {
            StringBuilder argStr = new StringBuilder();
            for (String s : args) {
                if (car.getFormula().equals("instance")) {
                    int ttype = f.getFormula().charAt(0);
                    if (Character.isDigit(ttype)) ttype = StreamTokenizer_s.TT_NUMBER;
                    if (Formula.atom(s)) argStr.append(SUMOformulaToTPTPformula.translateWord(s, ttype, false));
                    else argStr.append(processRecurse(new Formula(s), typeMap));
                }
                else argStr.append(processRecurse(new Formula(s), typeMap));
                argStr.append(" @ ");
            }
            argStr.delete(argStr.length() - 2, argStr.length()); 
            String functor = SUMOformulaToTPTPformula.translateWord(car.getFormula(), StreamTokenizer.TT_WORD, true);
            Matcher m = Pattern
                    .compile("^(s__[A-Za-z0-9]+__)(\\d+)$")
                    .matcher(functor);
            if (m.matches()) {
                int argCount = args.size();
                List<String> worldArgs = List.of("?W1", "?W2");
                if (argCount > 0 && (worldArgs.contains(args.get(argCount - 1)))) argCount--;
                int oldN = Integer.parseInt(m.group(2));
                if (argCount != oldN) functor = m.group(1) + argCount;
            }
            String result = Formula.LP + functor + " @ " + argStr.substring(0, argStr.length() - 1) + Formula.RP;
            return result;
        }
    }

    /***************************************************************
     * Map a KIF variable to a THF type, based on its inferred SUMO types.
     * - Formula variables become (w > $o) so they can be applied as F @ W.
     * - World variables become w.
     * - Modal variables become m.
     * - Everything else collapses to $i.
     */
    public static String getTHFtype(String v, Map<String, Set<String>> typeMap) {

        if (v.matches("\\?ROW\\d+") || v.matches("V__ROW\\d+")) return "$i";
        if (typeMap.get(v) == null) {
            if (v.matches("\\?W+\\d+") || v.matches("V__W+\\d+")) return "w";
            return "$i";
        }
        if (typeMap.get(v).contains("World")) return "w";
        if (typeMap.get(v).contains("Formula")) return "(w > $o)";
        if (typeMap.get(v).contains("Modal")) return "m";
        return "$i";
    }

    /*****************************************************************
     */
    private static String getTHFtypeNonModal(String v, Map<String, Set<String>> typeMap) {
        if (typeMap.get(v) == null) return "$i";
        if (typeMap.get(v).contains("Formula")) return "$o";
        return "$i";
    }

    /***************************************************************
     */
    public static String generateQList(Formula f, Map<String, Set<String>> typeMap, Set<String> vars) {

        StringBuilder qlist = new StringBuilder();
        String thftype, oneVar;
        for (String s : vars) {
            thftype = getTHFtype(s,typeMap);
            oneVar = SUMOformulaToTPTPformula.translateWord(s,s.charAt(0),false);
            qlist.append(oneVar).append(":").append(thftype).append(",");
        }
        if (qlist.length() > 1) qlist.deleteCharAt(qlist.length() - 1);  // delete final comma
        return qlist.toString();
    }

    /*****************************************************************
     */
    public static String generateQListNonModal(Formula f, Map<String, Set<String>> typeMap, Set<String> vars) {

        StringBuilder qlist = new StringBuilder();
        for (String s : vars) {
            String thftype = getTHFtypeNonModal(s, typeMap);
            String oneVar = SUMOformulaToTPTPformula.translateWord(s, s.charAt(0), false);
            qlist.append(oneVar).append(":").append(thftype).append(",");
        }
        if (qlist.length() > 1) qlist.deleteCharAt(qlist.length() - 1);
        return qlist.toString();
    }

    /***************************************************************
     * This is the primary method of the class.  It takes a SUO-KIF
     * formula and returns a THF formula.
     */
    public static String process(Formula f, Map<String, Set<String>> typeMap, boolean query) {

        if (f == null) {
            if (debug) System.err.println("Error in THFnew.process(): null formula: ");
            return "";
        }
        if (f.atom()) return SUMOformulaToTPTPformula.translateWord(f.getFormula(),f.getFormula().charAt(0),false);
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
                if (query) quantification = "? [";
                result = "( " + quantification + qlist + "] : (" + result + " ) )";
            }
            if (debug) System.out.println("THFnew.process(): result 2: " + result);
            return result;
        }
        return (f.getFormula());
    }

    /*****************************************************************
     */
    public static String processNonModal(Formula f, Map<String, Set<String>> typeMap, boolean query) {

        if (f == null) return "";
        if (f.atom()) return SUMOformulaToTPTPformula.translateWord(f.getFormula(), f.getFormula().charAt(0), false);
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

    /*****************************************************************
     */
    public static boolean variableArity(KB kb, String pred) {

        if (!pred.contains("_") || pred.length() < 4) return false;
        return kb.isInstanceOf(pred.substring(0,pred.length()-3),"VariableArityRelation");
    }

    /*****************************************************************
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

    /*****************************************************************
     */
    public static boolean protectedRelation(String s) {

        return s.equals("domain") || s.equals("instance") || s.equals("subAttribute") || s.equals("contraryAttribute");
    }

    // -----------------------------------------------------------------------
    // Private helpers for the Expr-based exclude overload
    // -----------------------------------------------------------------------

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
        if (found == (PC_STR_LITERAL | PC_TRUE_FALSE | PC_FORMULA_ARG)) return found;
        if (e instanceof Expr.StrLiteral) return found | PC_STR_LITERAL;
        if (e instanceof Expr.Atom a) {
            String name = a.name();
            if (name.equals(Formula.LOG_TRUE) || name.equals(Formula.LOG_FALSE)) return found | PC_TRUE_FALSE;
            return found;
        }
        if (!(e instanceof Expr.SExpr se)) return found;
        if (se.head() != null) found = preCheckExpr(se.head(), found);
        List<Expr> args = se.args();
        if ((found & PC_FORMULA_ARG) == 0 && !args.isEmpty() && args.get(args.size() - 1) instanceof Expr.Atom lastAtom && "Formula".equals(lastAtom.name()))
            found |= PC_FORMULA_ARG;
        for (Expr arg : args) {
            found = preCheckExpr(arg, found);
            if (found == (PC_STR_LITERAL | PC_TRUE_FALSE | PC_FORMULA_ARG)) return found;
        }
        return found;
    }

    /** Returns true if the Expr tree contains no {@link Expr.Var} or {@link Expr.RowVar} nodes. */
    private static boolean isGroundExpr(Expr e) {
        if (e instanceof Expr.Var || e instanceof Expr.RowVar) return false;
        if (e instanceof Expr.SExpr se) {
            if (se.head() != null && !isGroundExpr(se.head())) return false;
            for (Expr arg : se.args()) if (!isGroundExpr(arg)) return false;
        }
        return true;
    }

    /*****************************************************************
     * Expr-based overload of {@link #exclude(Formula, KB, Writer)}.
     *
     * <p>Walks the {@link Expr} tree directly — no {@code toKifString()}
     * round-trip, no {@code new Formula()} allocation.  Every check mirrors
     * its counterpart in the string-based overload.
     *
     * <p>Call sites should prefer this overload whenever a non-null {@code Expr}
     * is already available (e.g. in {@code oneTransExpr} and in the top-level
     * loop of {@code transModalTHF} for {@link Formula} formulas).
     */
    public static boolean exclude(Expr e, KB kb, Writer out) throws IOException {

        if (e == null) return false;
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
        if (!(e instanceof Expr.SExpr se)) return false;
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
            out.write("% exclude(): meta-logical axiom with Formula type\n");
            return true;
        }
        return excludeExprBody(se, kb, out);
    }

    /*****************************************************************
     * Fast-path overload for {@link Formula} formulas.
     *
     * <p>Uses {@code fa.getFormula().contains()} for the three pre-checks
     * (JVM-intrinsic / SIMD-backed string scans) instead of the recursive
     * {@link #preCheckExpr} tree walk, then delegates to
     * {@link #excludeExprBody} for the structural checks.
     * Called from the main loops in {@code transModalTHF} so that
     * {@code preCheckExpr} is not invoked for every one of the ~75 K formulas.
     */
    public static boolean exclude(Formula fa, KB kb, Writer out) throws IOException {

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
            out.write("% exclude(): meta-logical axiom with Formula type\n");
            return true;
        }
        if (!(fa.expr instanceof Expr.SExpr se)) return false;
        return excludeExprBody(se, kb, out);
    }

    /*****************************************************************
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
        if (headName != null && !Modals.allowedHeads.contains(headName)) {
            for (Expr arg : args) {
                if (arg instanceof Expr.Atom argAtom) {
                    String argName = argAtom.name();
                    if (Modals.MODAL_RELATIONS.contains(argName)
                        || Modals.modalAttributes.contains(argName)
                        || Modals.RESERVED_MODAL_SYMBOLS.contains(argName)
                        || Modals.regHOLpred.contains(argName)
                        || Modals.formulaPreds.contains(argName)) {
                        if (out != null)
                            out.write("% exclude(): modal/HOL symbol used as individual "
                                    + "argument of non-modal head, Symbol " + argName
                                    + " head: " + headName + "\n");
                        return true;
                    }
                }
            }
        }
        if (("domain".equals(headName) || "subrelation".equals(headName)) && !args.isEmpty()) {
            if (args.get(0) instanceof Expr.Atom firstArg) {
                String p = firstArg.name();
                if (Modals.formulaPreds.contains(p) || Modals.regHOLpred.contains(p)) {
                    if (out != null) out.write("% exclude(): domain axiom for formula/HOL predicate: " + p + "\n");
                    return true;
                }
            }
        }
        if ("=>".equals(headName) && args.size() >= 2 && args.get(1) instanceof Expr.Var conseqVar) {
            if (out != null) out.write("% exclude(): meta-logic (variable as consequent of =>): " + conseqVar.name() + "\n");
            return true;
        }
        if (("not".equals(headName) || "~".equals(headName))
                && !args.isEmpty() && args.get(0) instanceof Expr.Var notVar) {
            if (out != null)
                out.write("% exclude(): meta-logic (variable under not/~): " + notVar.name() + "\n");
            return true;
        }
        for (Expr arg : args) {
            if (arg instanceof Expr.SExpr argSe) {
                if (excludeExprBody(argSe, kb, out)) {
                    if (out != null)
                        out.write("% excluded(): interior list: " + se.toKifString() + "\n");
                    return true;
                }
            }
        }
        for (Expr arg : args) {
            if (arg instanceof Expr.Atom argAtom) {
                if (excludePred(argAtom.name(), out)) {
                    if (out != null)
                        out.write("% excluded(): term from excludePred: " + se.toKifString() + "\n");
                    return true;
                }
            }
        }
        if (isGroundExpr(se)) {
            if (debug) System.out.println("exclude(Expr): is ground: " + se.toKifString());
            if (headName != null && protectedRelation(headName)) {
                for (Expr arg : args) {
                    if (arg instanceof Expr.Atom argAtom && Modals.modalAttributes.contains(argAtom.name())) {
                        if (out != null) out.write("% exclude(): modal attribute in protected relation: " + headName + " " + argAtom.name() + "\n");
                        return true;
                    }
                }
            }
            if ("domain".equals(headName) && !args.isEmpty() && args.get(0) instanceof Expr.Atom firstArg && Modals.RESERVED_MODAL_SYMBOLS.contains(firstArg.name())) {
                if (out != null) out.write("% exclude(): modal operator in domain: " + firstArg.name() + "\n");
                return true;
            }
            for (Expr arg : args) {
                if (arg instanceof Expr.NumLiteral numLit) {
                    String s = numLit.value();
                    if (out != null) out.write("% exclude(): is numeric(2): \n");
                    if (s.contains(".") || s.contains("-") || s.length() > 1) return true;
                    if (s.charAt(0) < '1' || s.charAt(0) > '6') return true;
                }
            }
        }
        return headName != null && excludePred(headName, out);
    }

    /*****************************************************************
     * Expr-based overload of {@link #excludeNonModal(Formula, KB, Writer)}.
     * Used from inner loops where only an {@code Expr} is available (e.g.
     * preprocessed formulas in {@code oneTransNonModalExpr}).
     * Does NOT check {@code badUsageSymbols} — callers handle that.
     */
    public static boolean excludeNonModal(Expr e, KB kb, Writer out) throws IOException {

        if (e == null) return false;
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

    /*****************************************************************
     * Fast-path overload for {@link Formula} formulas in the plain-THF
     * main loop.
     *
     * <p>Handles {@code badUsageSymbols} (identity-based set lookup on the
     * original {@code FormulaAST} object), then uses
     * {@code fa.getFormula().contains()} for the three pre-checks instead of
     * the recursive {@link #preCheckExpr} tree walk, then delegates to
     * {@link #excludeNonModalExprBody}.
     */
    public static boolean excludeNonModal(Formula fa, KB kb, Writer out) throws IOException {

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

    /*****************************************************************
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
        if (("domain".equals(headName) || "subrelation".equals(headName)) && args.size() >= 2) {
            if (args.get(1) instanceof Expr.Atom p && kb.isInstanceOf(p.name(), "Relation")) {
                out.write("% excludeNonModal(): meta-logic domain/subrelation over relation: " + p.name() + "\n");
                return true;
            }
        }
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
        for (Expr arg : args) {
            if (arg instanceof Expr.SExpr argSe) {
                if (excludeNonModalExprBody(argSe, kb, out)) {
                    if (out != null) out.write("% excluded(): interior list: " + se.toKifString() + "\n");
                    return true;
                }
            }
        }
        for (Expr arg : args) {
            if (arg instanceof Expr.Atom argAtom) {
                if (excludePred(argAtom.name(), out)) {
                    if (out != null) out.write("% excluded(): term from excludePred: " + se.toKifString() + "\n");
                    return true;
                }
            }
        }
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
        return headName != null && excludePred(headName, out);
    }

    /*****************************************************************
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
        else return false;
    }

    /*****************************************************************
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
            pred.equals("holdsDuring") || 
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

    /*****************************************************************
     * Build a THF type string from a SUMO signature.
     * @param functor  the predicate / function symbol whose type we build
     * @param sig      the list of SUMO argument/result types (as strings)
     * @param kb       the KB, used for isInstanceOf checks
     * @param function true if this is a function symbol (first entry in sig is range)
     */
    public static String sigString(String functor, List<String> sig, KB kb, boolean function) {

        StringBuilder sb = new StringBuilder();
        boolean first = false;
        String range = "";
        if (function) first = true;
        boolean formulaAsWorldFunction = (Modals.formulaPreds.contains(functor) || Modals.regHOLpred.contains(functor));
        ArrayList<String> exceptionFormulas = new ArrayList<>(Arrays.asList(
            "KappaFn",
            "increasesLikelihood",
            "holdsRight",
            "ProbabilityFn",
            "hasPurpose",
            "containsFormula"
        ));
        if (exceptionFormulas.contains(functor)) formulaAsWorldFunction = false;
        for (String t : sig) {
            if (t.equals("")) continue;
            if (first) {
                range = t;
                first = false;
            }
            else if (kb.isInstanceOf(t, "Formula") || t.equals("Formula")) {
                if (formulaAsWorldFunction) sb.append("$o > ");
                else sb.append("$o > ");
            }
            else if (kb.isInstanceOf(t, "World") || t.equals("World")) sb.append("w > ");
            else if (kb.isInstanceOf(t, "ObjectiveNorm")) sb.append("m > ");
            else sb.append("$i > ");
        }
        if (function) {
            if (kb.isInstanceOf(range, "Formula") || range.equals("Formula")) sb.append("(w > $o)");
            else sb.append("$i");
        }
        else sb.append("$o");
        return sb.toString();
    }

    /*****************************************************************
     */
    public static String sigStringNonModal(String pred, List<String> sig, KB kb, boolean function) {

        Integer suffixNum = getSuffixNumber(pred);
        if (suffixNum != null && suffixNum > 0) {
            int arity = suffixNum;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arity; i++) sb.append("$i > ");
            if (function) sb.append("$i");
            else {
                sb.append("$o");
                predicateTerms.add(pred);
            }
            return sb.toString();
        }
        StringBuilder sb = new StringBuilder();
        boolean first = false;
        String range = "";
        if (function) first = true;
        for (String t : sig) {   // e.g. [, Organism, GeographicArea]
            if (t.equals("")) continue;

            if (first) {
                range = t;
                first = false;
            }
            else if (kb.isInstanceOf(t, "Formula") || t.equals("Formula")) sb.append("$o > ");
            else sb.append("$i > ");
        }

        if (function) {
            if (kb.isInstanceOf(range, "Formula") || range.equals("Formula")) {
                sb.append("$o");
                predicateTerms.add(pred);
            }
            else sb.append("$i");   // includes World-as-range => $i
        }
        else {
            sb.append("$o");
            predicateTerms.add(pred);
        }

        return sb.toString();
    }

    /*****************************************************************
     * Recursively collect all numeric literals from a formula string.
     * Collects integers, floats, and negatives (e.g. -1 → n___1).
     */
    private static void collectNumbersFromFormula(String fstr, Set<String> numbers) {

        Formula f = new Formula(fstr);
        if (f.atom()) {
            if (StringUtil.isNumeric(fstr)) numbers.add(fstr);
            return;
        }
        List<String> args = f.complexArgumentsToArrayListString(0);
        if (args != null) for (String arg : args) collectNumbersFromFormula(arg, numbers);
    }

    /*****************************************************************
     * Expr-tree equivalent of {@link #collectNumbersFromFormula}.
     * Walks the Expr tree directly — no {@code new Formula()} allocation,
     * no string re-scanning.  {@link Expr.NumLiteral} nodes are the only
     * leaves that can contribute numeric literals; all other leaf types
     * (Atom, Var, RowVar, StrLiteral) are skipped.
     */
    private static void collectNumbersFromExpr(Expr e, Set<String> numbers) {
        if (e instanceof Expr.NumLiteral num) numbers.add(num.value());
        else if (e instanceof Expr.SExpr se) {
            if (se.head() != null) collectNumbersFromExpr(se.head(), numbers);
            for (Expr arg : se.args()) collectNumbersFromExpr(arg, numbers);
        }
    }

    /*****************************************************************
     * Scan all KB formulas and return the set of every numeric literal
     * that appears (e.g. "24", "0.0", "360.0").  These will be declared
     * as thf(n__24_tp,type,(n__24 : $i)). so that hideNumbers=true
     * translations are well-typed.  Dots are normalised to underscores
     * to match the output of translateWord (0.0 -> n__0_0).
     *
     * <p>Uses the Expr tree directly for {@link Formula} instances
     * (no string re-scanning), and falls back to the string path for
     * plain {@link Formula} objects.
     */
    public static Set<String> collectNumbers(KB kb) {

        Set<String> numbers = new TreeSet<>();
        for (Formula f : kb.formulaMap.values()) {
            if (f instanceof Formula fa && fa.expr != null) collectNumbersFromExpr(fa.expr, numbers);
            else collectNumbersFromFormula(f.getFormula(), numbers);
        }
        return numbers;
    }

    /*****************************************************************
     * Emit a THF type declaration for every numeric literal in numbers.
     * Applies the same normalisation as translateWord: '.' -> '_'.
     */
    public static void writeIntegerTypes(Set<String> numbers, Writer out) throws IOException {

        for (String n : numbers) {
            String normalized = n.replace('.', '_').replace('-', '_');
            out.write("thf(n__" + normalized + "_tp,type,(n__" + normalized + " : $i)).\n");
        }
    }

    /*****************************************************************
     */
    private static Integer getSuffixNumber(String functor) {
        
        Matcher m = Pattern.compile("^.*__(\\d+)$").matcher(functor);
        if (m.matches()) return Integer.parseInt(m.group(1));
        return null;
    }

    /*****************************************************************
     */
    public static void writeTypes(KB kb, Writer out, Set<String> numbers) throws IOException {

        writeIntegerTypes(numbers, out);
        for (String t : kb.terms) {
            if (Modals.RESERVED_MODAL_SYMBOLS.contains(t) || Modals.regHOL3Modalpred.contains(t) ||
                Modals.regHOL3pred.contains(t) || Modals.regHOLpred.contains(t))
                continue;
            if (excludeForTypedef(t,out)) continue;
            if (kb.isInstanceOf(t,"Relation")) {
                List<String> baseSig = kb.kbCache.signatures.get(t);
                if (baseSig == null) {
                    System.err.println("Error in THFnew.writeTypes(): bad sig for " + t);
                    continue;
                }
                List<String> sig = new ArrayList<>(baseSig);
                if (variableArity(kb, t)) {
                    for (int _si = 1; _si < sig.size(); _si++) {
                        String _st = sig.get(_si);
                        if (_st == null || _st.isEmpty()) sig.set(_si, "Entity");
                    }
                }
                Integer suffixNum = getSuffixNumber(t);
                String baseHead = Modals.baseFunctor(t);
                if (suffixNum != null && !sig.isEmpty() && sig.size() > (suffixNum+1)) {
                    while (sig.size() > (suffixNum+1)) {
                        sig.remove(sig.size() - 1);   // remove from end until sizes match
                    }
                }

                if (!Formula.isLogicalOperator(t) && !t.equals("equals")) {
                    if (!Modals.RIGID_RELATIONS.contains(baseHead)
                            && !Modals.RESERVED_MODAL_SYMBOLS.contains(baseHead)
                            && !Modals.regHOLpred.contains(baseHead)) {
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
                    out.write("thf(" + SUMOtoTPTPformula + "_tp,type,(" + SUMOtoTPTPformula + " : "); // write signature
                    isFunction = true;
                }
                else out.write("thf(" + SUMOtoTPTPformula + "_tp,type,(" + SUMOtoTPTPformula + " : "); // write signature
                String sigStr;
                if (Modals.regHOLpred.contains(baseHead) || Modals.regHOL3pred.contains(baseHead)) {
                    sigStr = "m";
                    out.write( sigStr + ")).\n");
                }
                else {
                    sigStr = sigString(t, sig,kb,isFunction);
                    out.write("(" + sigStr + "))).\n");
                }
                String typeStr = "$i";
                if (Modals.MODAL_RELATIONS.contains(baseHead) && !Modals.regHOLpred.contains(baseHead)) typeStr = "m";
                out.write("thf(" + SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true) + "_m_tp,type,(" +
                        SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),false) + " : " + typeStr + ")).\n");
            }
            else if (Modals.modalAttributes.contains(t))
                out.write("thf(" + SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true) + "_tp,type,(" +
                    SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),false) + " : m)).\n");
            else
                out.write("thf(" + SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true) + "_tp,type,(" +
                    SUMOformulaToTPTPformula.translateWord(t,t.charAt(0),true) + " : $i)).\n");
        }
    }

    /*****************************************************************
     * Scans all KIF formulas once before THF translation to detect predicates whose 
     * arguments are used inconsistently with their declared signatures. 
     */
    public static void analyzeBadUsages(KB kb) {

        for (Formula f : kb.formulaMap.values()) if (f instanceof Formula fa && fa.expr != null) analyzeFormulaExpr(fa.expr, kb, fa);
    }

    /*****************************************************************
     * Expr-tree equivalent of {@link #analyzeFormula(Formula, KB)}.
     *
     * <p>Walks the Expr tree directly — no {@code new Formula()} allocation,
     * no string re-parsing.  Adds {@code topLevel} to
     * {@link #badUsageSymbols} if the given SExpr (or any sub-SExpr) has
     * an argument position where the declared signature expects a
     * non-Formula type but the argument is itself a compound expression
     * ({@link Expr.SExpr}) or a predicate term.
     *
     * @param e        the Expr node to analyze (may be any Expr subtype)
     * @param kb       knowledge base (used for signature look-up)
     * @param topLevel the {@link Formula} to add to badUsageSymbols on detection
     */
    private static void analyzeFormulaExpr(Expr e, KB kb, Formula topLevel) {

        if (!(e instanceof Expr.SExpr se)) return; 
        String headName = se.headName();
        if (headName == null) return;
        if (headName.equals("termFormat") || headName.equals("documentation") || headName.equals("format")) return;
        List<Expr> args = se.args();
        if (args.isEmpty()) return;
        List<String> sig = kb.kbCache.signatures.get(headName);
        if (debug) System.out.println("analyzeFormulaExpr(): head: " + headName + " sig: " + sig);
        if (sig != null && sig.size() > 1) {
            int maxArgs = Math.min(args.size(), sig.size() - 1);
            for (int i = 0; i < maxArgs; i++) {
                String expectedType = sig.get(i + 1);
                Expr arg = args.get(i);
                if (debug) System.out.println("analyzeFormulaExpr(): arg " + i + " | " + arg.toKifString());
                if (!"Formula".equals(expectedType) && (arg instanceof Expr.SExpr || (arg instanceof Expr.Atom atom && predicateTerms.contains(atom.name())))) {
                    THFnew.badUsageSymbols.add(topLevel);
                    break;
                }
            }
        }
        for (Expr arg : args) if (arg instanceof Expr.SExpr) analyzeFormulaExpr(arg, kb, topLevel);
    }

    /*****************************************************************
     * Expr-based equivalent of {@link #oneTrans}.
     * Used when the formula is a {@link Formula} with a non-null
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
    public static void oneTransExpr(KB kb, Formula fa, PrintWriter bw) throws IOException {

        bw.write("% original: " + fa.getFormula() + "\n" + "% from file " + fa.sourceFile + " at line " + fa.startLine + "\n");
        Map.Entry<Expr, Map<String, Set<String>>> modalResult = Modals.processModalsExpr(fa.expr, kb);
        Expr resExpr = modalResult.getKey();
        if (resExpr == null) return;
        FormulaPreprocessor fp = new FormulaPreprocessor();
        Set<Expr> processed = fp.preProcessExpr(fa, false, kb);
        if (processed == null || processed.isEmpty()) return;
        Map<String, Set<String>> typeMap = new HashMap<>();
        typeMap.putAll(fp.findTypeRestrictionsExpr(resExpr, kb));
        typeMap.putAll(modalResult.getValue()); // world var types (?W0, ?W1 → {"World"})
        Set<String> worldTypes = new HashSet<>(Collections.singleton("World"));
        String primaryWorldVar = Modals.makeWorldVarExpr(fa.expr);
        typeMap.put(primaryWorldVar, worldTypes);
        Modals.markModalAttributeFormulaVarsExpr(fa.expr, typeMap);
        for (Expr e : processed) {
            if (SUMOKBtoTPTPKB.hasUnresolvedPredVar(e)) continue;
            Modals.markModalAttributeFormulaVarsExpr(e, typeMap);
            Map.Entry<Expr, Map<String, Set<String>>> fmodalResult = Modals.processModalsExpr(e, kb, typeMap);
            typeMap.putAll(fmodalResult.getValue());
            Expr fmodal = fmodalResult.getKey();
            if (fmodal == null) continue;
            if (exclude(fmodal, kb, bw)) continue;
            if (hasFormulaDomainArgMismatch(fmodal, typeMap, kb)) {
                bw.println("% excluded (Formula-typed domain arg with untyped $i variable): " + e.toKifString().replace("\n", " ").replace("\r", " "));
                continue;
            }
            String thf = ExprToTHF.translate(fmodal, false, typeMap);
            bw.println("thf(ax" + axNum++ + ",axiom," + thf + ").\n");
        }
    }

    /**
     * Returns true when {@code e} contains a predicate application where a
     * Formula-typed domain argument is bound to a plain variable that is NOT
     * typed Formula in {@code typeMap} (i.e. would be quantified as {@code $i}).
     *
     * <p>This happens after pred-var expansion: the row-variable {@code @ARGS}
     * is split into fresh {@link Expr.Var} nodes ({@code ?ARGS1 ?ARGS2 ?ARGS3 …})
     * with no type annotation.  If the predicate substituted for {@code ?REL}
     * declares {@code Formula} as the domain type at position k, the variable
     * {@code ?ARGSk} would be emitted as {@code $i} — but the predicate's THF
     * type signature expects {@code $o} (or {@code w > $o} in modal mode).
     * The result is an ill-typed THF axiom rejected by LEO-III / Vampire HOL.
     *
     * <p>Predicates affected: {@code attitudeForFormula} (domain 3 = Formula),
     * {@code conclusion} (domain 2), {@code consistent} (domains 1,2),
     * {@code fears} (domain 2), {@code hopes} (domain 2), and others.
     */
    private static boolean hasFormulaDomainArgMismatch(Expr e,
                                                        Map<String, Set<String>> typeMap,
                                                        KB kb) {
        if (!(e instanceof Expr.SExpr se)) return false;
        String head = se.headName();
        if (head != null && !Formula.isLogicalOperator(head)
                && !head.equals(Formula.EQUAL)) {
            List<String> sig = kb.kbCache.signatures.get(head);
            if (sig != null && sig.size() > 1) {
                List<Expr> args = se.args();
                for (int i = 0; i < args.size() && (i + 1) < sig.size(); i++) {
                    String domType = sig.get(i + 1);
                    if ("Formula".equals(domType) || kb.isSubclass(domType, "Formula")) {
                        if (args.get(i) instanceof Expr.Var v) {
                            Set<String> varTypes = typeMap.get(v.name());
                            if (varTypes == null || !varTypes.contains("Formula"))
                                return true;
                        }
                    }
                }
            }
        }
        for (Expr arg : se.args()) if (hasFormulaDomainArgMismatch(arg, typeMap, kb)) return true;
        return false;
    }

    /*****************************************************************
     * Expr-based equivalent of {@link #oneTransNonModal}.
     * Used when the formula is a {@link Formula} with a non-null
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
    public static void oneTransNonModalExpr(KB kb, Formula fa, Writer bw) throws IOException {

        bw.write("% original: " + fa.getFormula() + "\n" + "% from file " + fa.sourceFile + " at line " + fa.startLine + "\n");
        FormulaPreprocessor fp = new FormulaPreprocessor();
        Set<Expr> processed = fp.preProcessExpr(fa, false, kb);
        if (processed == null || processed.isEmpty()) return;
        Map<String, Set<String>> typeMap = new HashMap<>();
        typeMap.putAll(fp.findTypeRestrictionsExpr(fa.expr, kb));
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

    /*****************************************************************
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
            oneTransExpr(kb,f,null);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /*****************************************************************
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

    /*****************************************************************
     * Wait for background TPTP generation to complete.
     * Only needed for full-KB export modes (-m, -r), not for
     * single-formula translation (--one).
     */
    public static void waitForBackgroundGeneration() {

        if (!TPTPGenerationManager.waitForTHFModal(600)) System.out.println("THFnew.main(): Background generation not ready, generating THF Modal synchronously");
        if (!TPTPGenerationManager.waitForTHFPlain(600)) System.out.println("THFnew.main(): Background generation not ready, generating THF Plain synchronously");
        if (!TPTPGenerationManager.waitForTFF(600)) System.out.println("THFnew.main(): Background generation not ready, generating TFF synchronously");
    }

    /*****************************************************************
     */
    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("INFO in THFnew.main()");
        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        System.out.println(argMap);
        if (argMap.containsKey("h") || argMap.isEmpty()) {
            showHelp();
            return;
        }
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        System.out.println("THFnew.main(): KB loaded");
        if (!kb.errors.isEmpty()) System.err.println("THFnew.main(): KB loaded with non-fatal errors: " + kb.errors);
        System.out.println("contains one : " + argMap.containsKey("one"));
        System.out.println("has one arg: " + (argMap.containsKey("one") && argMap.get("one").size() == 1));
        if (argMap.containsKey("one") && argMap.get("one").size() == 1) {
            System.out.println("THFnew.main(): translate to THF (with modals)");
            PrintWriter writer = new PrintWriter(System.out, true);
            try {
                String kifStr = argMap.get("one").get(0);
                SuokifVisitor visitor = SuokifVisitor.parseString(kifStr);
                Formula fa = visitor.result.isEmpty() ? null : visitor.result.values().iterator().next();
                if (fa != null && fa.expr != null) oneTransExpr(kb, fa, writer);
                else {
                    System.out.println("THFnew.main(): FormulaAST parse failed or expr is null — falling back to string-based translation");
                    oneTransExpr(kb, new Formula(kifStr), writer);
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