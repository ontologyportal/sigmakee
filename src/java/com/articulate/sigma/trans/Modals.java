package com.articulate.sigma.trans;

import com.articulate.sigma.CLIMapParser;
import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.parsing.Expr;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;

public class Modals {
    
    public static boolean debug = true; // Mainly for deontic sentences 

    // these are predicates that take a formula as (one of) their arguments in SUMO/KIF
    // e.g. (holdsDuring ?T ?FORMULA)
    public static final List<String> formulaPreds = new ArrayList<>(
            Arrays.asList(
                    Formula.KAPPAFN,
                    "believes",
                    "causesProposition",
                    "conditionalProbability", 
                    "confersNorm", 
                    "confersObligation",
                    "confersRight", 
                    "considers",
                    "containsFormula",
                    "decreasesLikelihood", 
                    "deprivesNorm", 
                    "describes",
                    "desires",
                    "disapproves",
                    "doubts",
                    "entails",  // Not modal 
                    "expects",
                    "hasPurpose",
                    "hasPurposeForAgent", // Needs to switch formula with agent 
                    "holdsDuring",
                    "holdsObligation",
                    "holdsRight",
                    "increasesLikelihood",  // Not modal
                    "independentProbability",   // Not modal
                    "knows",
                    "modalAttribute",
                    "permits",
                    "prefers",
                    "prohibits",
                    "ProbabilityFn",    // Not modal 
                    "rateDetail",
                    "says",
                    "treatedPageDefinition",
                    "visitorParameter"
            ));

    // a subset of formulaPreds where two arguments are formulas (e.g. entails(φ, ψ)).
    // (<dualFormulaPreds> ?FORMULA1 ?FORMULA2)
    public static final List<String> dualFormulaPreds = new ArrayList<>(
            Arrays.asList(
                    "causesProposition",
                    "conditionalProbability",
                    "decreasesLikelihood",
                    "entails",
                    "increasesLikelihood",
                    "independentProbability",
                    "prefers"
            ));

    // these are the ones you want to handle with the special HOL rewrite
    // Modal operators that take an agent and a formula as arguments are
    // (<regHOLpred> ?AGENT ?FORMULA)
    public static final List<String> regHOL3pred = new ArrayList<>(
            Arrays.asList(
                    "confersNorm",
                    "confersObligation",
                    "confersRight",
                    "deprivesNorm",
                    "hasPurposeForAgent"
            ));

    // these take a modal as their second argument
    public static final List<String> regHOL3Modalpred = new ArrayList<>(
            Arrays.asList(
                    "confersNorm",
                    "deprivesNorm"
            ));

    public static final List<String> deontics = new ArrayList<>(
            Arrays.asList(
                    "confersNorm",
                    "confersObligation",
                    "confersRight",
                    "deprivesNorm",
                    "hasPurposeForAgent",
                    "Obligation",
                    "Permission",
                    "Prohibition",
                    "holdsObligation",
                    "holdsRight"
            ));

    public static final List<String> regHOLpred = new ArrayList<>(
            Arrays.asList("permits","prohibits","considers","sees","believes",
                    "knows","holdsDuring","desires","hasPurpose","describes",
                    "disapproves","doubts","expects","holdsObligation",
                    "holdsRight","says"));

    // TODO: Instead of Hard Typing check that is a subclass of NormativeAttribute
    // these are the attribute constants you can pass to modalAttribute
    // (modalAttribute ?FORMULA <modalAttributes>)
    public static final Set<String> modalAttributes = new HashSet<>(Arrays.asList(
            "Possibility",
            "Necessity",
            "Permission",
            "Obligation",
            "Prohibition",
            // ISSUE 5
            "Likely",
            // ISSUE 7
            "Unlikely",
            // ISSUE 9
            "Legal",
            "Law",
            "Illegal",
            "Promise"
    ));

    // Relations that are treated as *rigid* in the Kripke semantics:
    // they do NOT get a world argument in their THF type. These are
    // mostly taxonomic / structural relations (types, orders, etc.).
    public static final Set<String> RIGID_RELATIONS =
            new HashSet<>(Arrays.asList(
                    "instance",
                    "subclass",
                    "domain",
                    "domainSubclass",
                    "range",
                    "rangeSubclass",
                    "immediateInstance",
                    "immediateSubclass",
                    "disjoint",
                    "partition",
                    "exhaustiveDecomposition",
                    "successorClass",
                    "partialOrderingOn",
                    "trichotomizingOn",
                    "totalOrderingOn",
                    "disjointDecomposition", // New Entry (Angelos)
                    // CF: TODO: Include all temporals other than the SEVEN: 
                    // weddingAnniversary, typicallyContainsTemporalPart, typicalTemporalPart
                    // time, cooccur, anniversary, WhenFn 
                    "AfternoonFn",
                    "MorningFn",
                    "EveningFn", 
                    // Arithmetic Op
                    "AbsoluteValueFn", 
                    "AdditionFn",
                    "MultiplicationFn",
                    "ArcCosineFn",
                    "ArcSineFn",
                    "arcTangentFn",
                    "AverageFn",
                    "CosineFn",
                    "DivisionFn",
                    "ExponentiationFn",
                    "ListSumFn",
                    "LogFn",
                    "MultiplicationFn",
                    "ReciprocalFn",
                    "RoundFn",
                    "SineFn",
                    "SquareRootFn",
                    "SubtractionFn",
                    "TangentFn"
            ));

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
                    "accreln1",
                    "accreln2",
                    "accreln3",
                    "accreln3norm",
//                    "knows",
//                    "believes",
//                    "desires",
                    "holdsDuring" // ISSUE 6
            ));

    // list that contains the allowed head predicates for the modal predicates
    public static final List<String> allowedHeads;
    static {
        List<String> tmp = new ArrayList<>();
        tmp.addAll(MODAL_RELATIONS);
        tmp.addAll(modalAttributes);
        tmp.addAll(RESERVED_MODAL_SYMBOLS);
        tmp.addAll(regHOLpred);
        tmp.addAll(formulaPreds);
        allowedHeads = Collections.unmodifiableList(tmp);
    }

    public enum FrameAx { // frame axioms
        REFLEXIVE, SYMMETRIC, TRANSITIVE, SERIAL, EUCLIDEAN}

    public enum ModalSystem {
        K,D,T,B,S4,S5,D4}

    public static final Set<String> noWorld = new HashSet<>(Arrays.asList(
            "instance","subclass","domain","domainSubclass","range","rangeSubclass",
            "immediateInstance","immediateSubclass","disjoint","partition",
            "exhaustiveDecomposition","successorClass","partialOrderingOn",
            "trichotomizingOn","totalOrderingOn","disjointDecomposition",
            "AdditionFn","MultiplicationFn","ArcCosineFn","ArcSineFn",
            "arcTangentFn","AverageFn","CosineFn","DivisionFn","ExponentiationFn",
            "ListSumFn","LogFn","MultiplicationFn","ReciprocalFn","RoundFn",
            "SineFn","SquareRootFn","SubtractionFn","TangentFn"));

    /***************************************************************
     * Handle the predicates given in regHOL3pred, which have a parameter
     * followed by a formula.
     */
    public static Formula handleHOL3pred(Formula f, KB kb, Map<String, Set<String>> typeMap,
                                         String worldvar, Integer worldNum) {

        Set<String> types = new HashSet<>();
        types.add("World");
        StringBuilder fstring = new StringBuilder();
        List<Formula> flist = f.complexArgumentsToArrayList(1);
        Formula arg1 = flist.get(0);
        Formula arg2 = flist.get(1);
        worldNum = worldNum + 1;
        if (regHOL3Modalpred.contains(f.car()))
            fstring.append("(=> (accreln3norm ").append(f.car()).append(Formula.SPACE);
        else
            fstring.append("(=> (accreln3 ").append(f.car()).append(Formula.SPACE);
        fstring.append(arg1).append(Formula.SPACE).append(arg2).append(Formula.SPACE);
        // Accounts for Constant World (world 0)
        if (worldNum - 1 == 0) { 
            fstring.append(" CW");
        }
        else {
            String worldStr = " ?" + worldvar + (worldNum - 1);
            fstring.append(worldStr);
            if (!typeMap.containsKey(worldStr))
                typeMap.put(worldStr.trim(),types);
        }
        String worldStr = " ?" + worldvar + (worldNum);
        if (!typeMap.containsKey(worldStr))
            typeMap.put(worldStr.trim(),types);
        fstring.append(worldStr).append(") ");
        fstring.append(Formula.SPACE).append(processRecurse(flist.get(2),kb,typeMap,worldvar,worldNum));
        fstring.append(Formula.RP);
        Formula result = new Formula();
        result.read(fstring.toString());
        return result;
    }

    /***************************************************************
     * Handle predicates in regHOLpred that take an individual and a
     * formula argument, e.g. (confersObligation USGovernment ?A F).
     * We rewrite them into a Kripke-style implication using accreln:
     *
     *   (P A F)  ==>  (=> (accreln P A ?W_{n-1} ?W_n) F')
     *
     * where F' is recursively processed and world-indexed, and we
     * introduce a fresh world variable ?Wn.
     */
    public static Formula handleHOLpred(Formula f, KB kb, Map<String, Set<String>> typeMap,
                                        String worldvar, Integer worldNum) {


        Set<String> types = new HashSet<>();
        types.add("World");
        List<Formula> flist = f.complexArgumentsToArrayList(1); // args after the head
        if (flist.size() < 2) {
            System.out.println("Error in Modals.handleHOLpred(): too few arguments in : " + f);
        }
        // Expect: flist.get(0) = "agent"/parameter, flist.get(1) = formula argument
        worldNum = worldNum + 1;

        // Recursively process the “parameter” term as well
        Formula param = processRecurse(flist.get(0), kb, typeMap, worldvar, worldNum - 1);
        Formula embedded = processRecurse(flist.get(1), kb, typeMap, worldvar, worldNum);

        StringBuilder fstring = new StringBuilder();
        fstring.append("(=> (accreln2 ")
                .append(f.car())          // modal operator
                .append(Formula.SPACE)
                .append(param.toString()); // now world-annotated
        // Accounts for Constant World (world 0)
        if (worldNum - 1 == 0) { 
            fstring.append(" CW");
        }
        else {
            String worldStr = " ?" + worldvar + (worldNum-1);
            if (!typeMap.containsKey(worldStr))
                typeMap.put(worldStr.trim(),types);
            fstring.append(" ?" + worldvar).append(worldNum - 1);
        }
        String worldStr = " ?" + worldvar + (worldNum);
        if (!typeMap.containsKey(worldStr))
            typeMap.put(worldStr.trim(),types);
        fstring.append(" ?" + worldvar).append(worldNum)
                .append(") ")
                .append(embedded.toString())
                .append(Formula.RP);

        Formula result = new Formula();
        result.read(fstring.toString());
        return result;
    }

    /***************************************************************
     * Handle the predicate modalAttribute:
     *   (modalAttribute F M)
     * is read as: "F holds in all worlds accessible via modality M".
     *
     * We rewrite:
     *   (modalAttribute F M)
     * into
     *   (=> (accrelnP M ?W_{n-1} ?W_n) F')
     *
     * where F' is recursively processed and world-indexed, and
     * we introduce a fresh world variable ?Wn.
     */
    public static Formula handleModalAttribute(Formula f, KB kb, Map<String, Set<String>> typeMap,
                                               String worldvar, Integer worldNum) {

        Set<String> types = new HashSet<>();
        types.add("World");
        StringBuilder fstring = new StringBuilder();
        List<Formula> flist = f.complexArgumentsToArrayList(1); // [F, M]
        if (flist == null || flist.size() < 2) {
            System.out.println("Error in Modals.handleModalAttribute(): " + f + " at " +
                f.getSourceFile() + ":" + f.startLine);
            throw new IllegalArgumentException("modalAttribute requires at least 2 arguments: " + f);
        }
        int prevWorld = worldNum;
        int currWorld = worldNum + 1;
        Formula modality = flist.get(1); // modality is the second complex arg
        Formula formula  = flist.get(0);
        fstring.append("(=> (accreln1 ").append(modality).append(Formula.SPACE);
        // Account for CW (constant world):
        if (prevWorld == 0) {
            fstring.append("CW");
        }
        else {
            String worldStr = " ?" + worldvar + (prevWorld);
            if (!typeMap.containsKey(worldStr))
                typeMap.put(worldStr.trim(),types);
            fstring.append("?" + worldvar).append(prevWorld);
        }
        String worldStr = " ?" + worldvar + (currWorld);
        if (!typeMap.containsKey(worldStr))
            typeMap.put(worldStr.trim(),types);
        fstring.append(Formula.SPACE).append("?" + worldvar).append(currWorld).append(") ");
        // Recurse once on the embedded formula at the new current world:
        fstring.append(processRecurse(formula, kb, typeMap, worldvar, currWorld));
        fstring.append(Formula.RP);
        Formula result = new Formula();
        result.read(fstring.toString());
        return result;
    }

    /***************************************************************
     */
    public static Formula processRecurse(Formula f, KB kb, Map<String, Set<String>> typeMap,
                                         String worldvar, Integer worldNum) {

        if (f.atom())
            return f;
        if (f.empty())
            return f;

        if (f.listP()) {
            if (regHOL3pred.contains(f.car()))
                return handleHOL3pred(f,kb,typeMap,worldvar,worldNum);
            if (regHOLpred.contains(f.car())) {
                return handleHOLpred(f, kb, typeMap,worldvar,worldNum);
            }
            if (f.car().equals("modalAttribute")) {
                return handleModalAttribute(f, kb, typeMap,worldvar, worldNum);
            }

            int argStart = 1;
            if (Formula.isQuantifier(f.car()))
                argStart = 2;

            List<Formula> flist = f.complexArgumentsToArrayList(argStart);
            StringBuilder fstring = new StringBuilder();
            fstring.append(Formula.LP).append(f.car());
            // Append quantifier variable list as-is
            if (argStart == 2)
                fstring.append(Formula.SPACE).append(f.getStringArgument(1));

            // Recursively process arguments
            for (Formula arg : flist) {
                fstring.append(Formula.SPACE).append(processRecurse(arg, kb, typeMap, worldvar, worldNum));
            }
            // Close the term / formula
            if (Formula.isLogicalOperator(f.car()) || (f.car().equals(Formula.EQUAL))) {
                // Pure logical symbols: no world argument (and, or, =>, <=>, =, etc.)
                fstring.append(Formula.RP);
            }
            else {
                // For non-logical heads, add a world argument to ALL non-rigid,
                // non-reserved predicates. This matches the idea that almost
                // every factual predicate is world-sensitive, except:
                //  - rigid taxonomy / structural relations (instance, subclass, ...)
                //  - reserved modal machinery (accreln, accrelnP, knows, believes, ...)
                //  - modal attribute constants themselves.
                String head = f.car();
                String baseHead = baseFunctor(head);   // <-- normalize "partition__7" -> "partition"

                if (worldNum != null
                        && !Formula.isVariable(baseHead)
                        && !RIGID_RELATIONS.contains(baseHead)
                        && !Modals.RESERVED_MODAL_SYMBOLS.contains(baseHead)
                        && !modalAttributes.contains(baseHead)) {
                            // Account for Constant-world (world 0)
                            if (worldNum == 0) {
                                fstring.append(" CW");
                            }
                            else {
                                fstring.append(" ?" + worldvar).append(worldNum);
                            }
                }
                fstring.append(Formula.RP);

                // Signatures are read-only here; no mutation. We only log missing ones.
                List<String> sig = kb.kbCache.signatures.get(head);
                if (sig == null && !Formula.isVariable(head)) {
                    System.err.println("Error in processRecurse(): null signature for " + head);
                }
            }

            Formula result = new Formula();
            result.read(fstring.toString());
            return result;
        }
        return f;
    }

    /***************************************************************
     * Return the base functor name by stripping a trailing "__<digits>" suffix.
     * E.g. "partition__7" -> "partition", "disjointDecomposition__4" -> "disjointDecomposition".
     * If there is no such suffix, returns the input unchanged.
     */
    public static String baseFunctor(String head) {

        if (head == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^(.*)__\\d+$")
                .matcher(head);
        if (m.matches()) {
            return m.group(1);
        }
        return head;
    }

    /***************************************************************
     * Add the signature for the Kripke accessibility relation
     */
    public static void addAccrelnDef(KB kb) {

        List<String> sig = new ArrayList<>();
        sig.add(""); // empty 0th argument for relations
        sig.add("Entity");
        sig.add("World");
        sig.add("World");
        kb.kbCache.signatures.put("accreln1",sig);
        sig.add(0,"Entity");
        kb.kbCache.signatures.put("accreln2",sig);
        sig.add(0,"Entity");
        kb.kbCache.signatures.put("accreln3",sig);
        sig.set(1,"Modal");
        kb.kbCache.signatures.put("accreln3norm",sig);
    }

    /***************************************************************
     */
    public static Formula processModals(Formula f, KB kb, Map<String, Set<String>> typeMap) {

        int worldNum = 1;
        String worldvar = "W" ;
        //System.out.println("processModals(): vars: " + f.collectAllVariables());
        //System.out.println("processModals(): world var: " + worldvar + worldNum);
        while (f.collectAllVariables().contains("?" + worldvar + worldNum))  // f might have ?W1 already
            worldvar = worldvar + "W";
        //addAccrelnDefP(kb);
        // Start at index 0 for constant world (W0 = CW)
        //if (!f.isHigherOrder(kb))
        //    return f;
        Formula result = processRecurse(f,kb,typeMap, worldvar,worldNum);
        String fstring = result.getFormula();
        result.read(fstring);
        Set<String> types = new HashSet<>();
        types.add("World");
        for (int i = 1; i <= worldNum; i++) {
            result.varTypeCache.put("?" + worldvar + i,types);
            typeMap.put("?" + worldvar + i,types);
        } 
        return result;
    }

    // =======================================================================
    // Expr-based modal processing (FormulaAST path)
    // Replicates processModals / processRecurse / handle* using Expr trees
    // instead of Formula string manipulation.
    // =======================================================================

    /***************************************************************
     * Collect all variable names (Var and RowVar) from an Expr tree.
     * Used to find a world-variable prefix that doesn't conflict with
     * existing variables.
     */
    private static void collectAllVarsExprRecurse(Expr expr, Set<String> vars) {
        switch (expr) {
            case Expr.Var v      -> vars.add(v.name());
            case Expr.RowVar rv  -> vars.add(rv.name());
            case Expr.SExpr se   -> {
                for (Expr child : se.args())
                    collectAllVarsExprRecurse(child, vars);
            }
            default -> { /* Atom, NumLiteral, StrLiteral — no variables */ }
        }
    }

    private static Set<String> collectAllVarsExpr(Expr expr) {
        Set<String> vars = new HashSet<>();
        collectAllVarsExprRecurse(expr, vars);
        return vars;
    }

    /***************************************************************
     * Expr-based equivalent of {@link #makeWorldVar(KB, Formula)}.
     * Returns the first {@code ?W<N>} variable name not already
     * present in the expression.
     */
    public static String makeWorldVarExpr(Expr expr) {
        Set<String> vars = collectAllVarsExpr(expr);
        int num = 0;
        while (vars.contains("?W" + num))
            num++;
        return "?W" + num;
    }

    /***************************************************************
     * Expr-based equivalent of {@link #markModalAttributeFormulaVars}.
     * Scans the Expr tree for {@code (modalAttribute ?VAR ...)} forms and
     * marks the first argument as having type {@code "Formula"} in the
     * supplied typeMap.
     */
    public static void markModalAttributeFormulaVarsExpr(Expr expr,
                                                          Map<String, Set<String>> typeMap) {
        if (expr == null) return;
        switch (expr) {
            case Expr.SExpr se -> {
                if ("modalAttribute".equals(se.headName()) && !se.args().isEmpty()) {
                    Expr first = se.args().get(0);
                    if (first instanceof Expr.Var v) {
                        typeMap.computeIfAbsent(v.name(), k -> new HashSet<>()).add("Formula");
                    }
                }
                for (Expr child : se.args())
                    markModalAttributeFormulaVarsExpr(child, typeMap);
            }
            default -> { /* leaf nodes have no subformulas */ }
        }
    }

    /***************************************************************
     * Expr-based equivalent of {@link #handleHOL3pred}.
     * Rewrites {@code (pred a1 a2 form)} into the Kripke implication
     * {@code (=> (accreln3[norm] pred a1 a2 prevWorld currWorld) form')}.
     * Uses {@code accreln3norm} for {@link #regHOL3Modalpred} predicates
     * (confersNorm, deprivesNorm) where the second extra argument is Modal-typed.
     */
    private static Expr handleHOL3predExpr(Expr.SExpr se, KB kb,
                                            String worldvar, int worldNum) {
        List<Expr> flist = se.args(); // args after head (but se.args() has them already)
        // flist[0]=a1, flist[1]=a2, flist[2]=form
        int prevWorld = worldNum;
        int currWorld = worldNum + 1;

        Expr a1   = processRecurseExpr(flist.get(0), kb, worldvar, prevWorld);
        Expr a2   = processRecurseExpr(flist.get(1), kb, worldvar, prevWorld);
        Expr form = processRecurseExpr(flist.get(2), kb, worldvar, currWorld);

        Expr prevWorldArg = (prevWorld == 0)
                ? new Expr.Atom("CW")
                : new Expr.Var("?" + worldvar + prevWorld);
        Expr currWorldArg = new Expr.Var("?" + worldvar + currWorld);

        // Use accreln3norm for Modal-typed predicates (confersNorm, deprivesNorm),
        // accreln3 for the rest — mirrors the string-based handleHOL3pred() distinction.
        String accrelnName = regHOL3Modalpred.contains(se.head().toKifString())
                ? "accreln3norm" : "accreln3";
        Expr accreln = new Expr.SExpr(
                new Expr.Atom(accrelnName),
                List.of(se.head(), a1, a2, prevWorldArg, currWorldArg));

        // (=> accreln form)
        return new Expr.SExpr(new Expr.Atom("=>"), List.of(accreln, form));
    }

    /***************************************************************
     * Expr-based equivalent of {@link #handleHOLpred}.
     * Rewrites {@code (pred agent form)} into
     * {@code (=> (accreln2 pred agent prevWorld currWorld) form')}.
     */
    private static Expr handleHOLpredExpr(Expr.SExpr se, KB kb,
                                           String worldvar, int worldNum) {
        List<Expr> flist = se.args(); // flist[0]=agent, flist[1]=form
        int prevWorld = worldNum;
        int currWorld = worldNum + 1;

        Expr param    = processRecurseExpr(flist.get(0), kb, worldvar, prevWorld);
        Expr embedded = processRecurseExpr(flist.get(1), kb, worldvar, currWorld);

        Expr prevWorldArg = (prevWorld == 0)
                ? new Expr.Atom("CW")
                : new Expr.Var("?" + worldvar + prevWorld);
        Expr currWorldArg = new Expr.Var("?" + worldvar + currWorld);

        // (accreln2 pred agent prevWorld currWorld)
        Expr accreln = new Expr.SExpr(
                new Expr.Atom("accreln2"),
                List.of(se.head(), param, prevWorldArg, currWorldArg));

        // (=> accreln embedded)
        return new Expr.SExpr(new Expr.Atom("=>"), List.of(accreln, embedded));
    }

    /***************************************************************
     * Expr-based equivalent of {@link #handleModalAttribute}.
     * Rewrites {@code (modalAttribute form modality)} into
     * {@code (=> (accreln1 modality prevWorld currWorld) form')}.
     */
    private static Expr handleModalAttributeExpr(Expr.SExpr se, KB kb,
                                                   String worldvar, int worldNum) {
        List<Expr> flist = se.args(); // flist[0]=form, flist[1]=modality
        int prevWorld = worldNum;
        int currWorld = worldNum + 1;

        Expr modality = flist.get(1);
        Expr form     = processRecurseExpr(flist.get(0), kb, worldvar, currWorld);

        Expr prevWorldArg = (prevWorld == 0)
                ? new Expr.Atom("CW")
                : new Expr.Var("?" + worldvar + prevWorld);
        Expr currWorldArg = new Expr.Var("?" + worldvar + currWorld);

        // (accreln1 modality prevWorld currWorld)
        Expr accreln = new Expr.SExpr(
                new Expr.Atom("accreln1"),
                List.of(modality, prevWorldArg, currWorldArg));

        // (=> accreln form)
        return new Expr.SExpr(new Expr.Atom("=>"), List.of(accreln, form));
    }

    /***************************************************************
     * Expr-based equivalent of {@link #processRecurse(Formula, KB, String, Integer)}.
     * Recursively transforms an Expr tree by:
     * <ul>
     *   <li>Rewriting modal predicates (regHOLpred, regHOL3pred, modalAttribute)
     *       into Kripke-style accessibility-relation implications.</li>
     *   <li>Adding a world argument to all non-rigid, non-reserved predicates.</li>
     *   <li>Leaving logical operators and quantifier bodies intact (recursing into them).</li>
     * </ul>
     */
    public static Expr processRecurseExpr(Expr expr, KB kb,
                                           String worldvar, int worldNum) {
        return switch (expr) {
            case Expr.Atom a       -> a;
            case Expr.Var v        -> v;
            case Expr.RowVar rv    -> rv;
            case Expr.NumLiteral n -> n;
            case Expr.StrLiteral s -> s;
            case Expr.SExpr se     -> processRecurseSExprExpr(se, kb, worldvar, worldNum);
        };
    }

    private static Expr processRecurseSExprExpr(Expr.SExpr se, KB kb,
                                                  String worldvar, int worldNum) {
        String headName = se.headName();
        if (headName == null) return se; // null-head var list inside quantifier

        // Modal predicate rewrites
        if (regHOL3pred.contains(headName))
            return handleHOL3predExpr(se, kb, worldvar, worldNum);
        if (regHOLpred.contains(headName))
            return handleHOLpredExpr(se, kb, worldvar, worldNum);
        if ("modalAttribute".equals(headName))
            return handleModalAttributeExpr(se, kb, worldvar, worldNum);

        boolean isQuantifier = Formula.isQuantifier(headName);
        boolean isLogical    = Formula.isLogicalOperator(headName)
                               || Formula.EQUAL.equals(headName);

        // Build new arg list by recursing; for quantifiers, skip the variable list
        List<Expr> args    = se.args();
        List<Expr> newArgs = new ArrayList<>(args.size());

        for (int i = 0; i < args.size(); i++) {
            if (isQuantifier && i == 0) {
                // Keep variable list verbatim — do not add world args to bound variables
                newArgs.add(args.get(i));
            } else {
                newArgs.add(processRecurseExpr(args.get(i), kb, worldvar, worldNum));
            }
        }

        Expr.SExpr rebuilt = new Expr.SExpr(se.head(), newArgs);

        // Add world argument to non-rigid, non-reserved, non-logical predicates
        if (!isLogical && !isQuantifier) {
            String baseHead = baseFunctor(headName);
            if (!Formula.isVariable(baseHead)
                    && !RIGID_RELATIONS.contains(baseHead)
                    && !RESERVED_MODAL_SYMBOLS.contains(baseHead)
                    && !modalAttributes.contains(baseHead)) {
                Expr worldArg = (worldNum == 0)
                        ? new Expr.Atom("CW")
                        : new Expr.Var("?" + worldvar + worldNum);
                List<Expr> argsWithWorld = new ArrayList<>(rebuilt.args());
                argsWithWorld.add(worldArg);
                return new Expr.SExpr(rebuilt.head(), argsWithWorld);
            }
        }
        return rebuilt;
    }

    /***************************************************************
     * Expr-based equivalent of {@link #processModals(Formula, KB)}.
     *
     * <p>Transforms the Expr tree to Kripke modal form:
     * non-rigid predicates receive a world argument and modal predicates
     * are rewritten into accessibility-relation implications.
     *
     * @param expr the formula Expr tree
     * @param kb   the knowledge base (used for signature look-ups)
     * @return a {@link Map.Entry} whose key is the transformed Expr and
     *         whose value is the world-variable type map
     *         ({@code "?W<n>" → {"World"}} for the primary world var)
     */
    public static Map.Entry<Expr, Map<String, Set<String>>> processModalsExpr(
            Expr expr, KB kb) {

        // Find a world-variable prefix that doesn't conflict with existing vars
        Set<String> existingVars = collectAllVarsExpr(expr);
        String worldvar  = "W";
        int    worldNum  = 1;
        while (existingVars.contains("?" + worldvar + worldNum))
            worldvar = worldvar + "W";

        Expr result = processRecurseExpr(expr, kb, worldvar, worldNum);

        // Collect all world variables actually introduced during recursive processing.
        // The fixed-range loop (0..worldNum) misses ?W2, ?W3, etc. because worldNum
        // is a local int that recursive calls cannot update.  Scanning the result tree
        // for vars that share the worldvar prefix is the reliable alternative.
        Map<String, Set<String>> worldTypes = new HashMap<>();
        Set<String> wType = new HashSet<>(Collections.singleton("World"));
        // World variables are exactly "?<worldvar><digits>" — use matches() not startsWith()
        // to avoid falsely classifying user variables like ?WHOLE or ?WORLD as world-typed.
        final String wPattern = "\\?" + worldvar + "\\d+";
        for (String v : collectAllVarsExpr(result)) {
            if (v.matches(wPattern))
                worldTypes.put(v, wType);
        }

        return new SimpleEntry<>(result, worldTypes);
    }

    /***************************************************************
     */
    public static String getTFFHeader() {

        return "tff(worlds_tp,type,(w : $tType)).\n" +
                "tff(modals_tp,type,(m : $tType)).\n" +
                "tff(accreln_tp,type,(s__accreln : (m * $i * w * w) > $o)).";
    }

    /***************************************************************
     * Generates the appropriate modal type for every operator
     */
    public static String genModalTypes(HashSet<String> allModals) {

        StringBuffer result = new StringBuffer();
        for (String s : allModals)
            result.append("thf(" + Character.toLowerCase(s.charAt(0)) + s.substring(1) +
                    "_tp,type,(s__" + s + " : m)).\n");
        return result.toString();
    }

    /***************************************************************
     */
    public static String genDistinctModals() {

        StringBuffer result = new StringBuffer();
        HashSet<String> allModals = new HashSet<>();
        allModals.addAll(regHOLpred);
        allModals.addAll(regHOL3pred);
        result.append("thf(tdistinct,type,$distinct(");
        for (String s : allModals) {
            result.append("s__" + s + ",");
        }
        result.deleteCharAt(result.length()-1);
        result.append(")).");
        return result.toString();
    }

    /***************************************************************
     * Generates the appropriate modal system for every operator
     * Use system D for deontics and T for everything else
     */
    public static String genAllModalSystems() {

        StringBuffer result = new StringBuffer();
        HashSet<String> allModals = new HashSet<>();
        allModals.addAll(regHOLpred);
        allModals.addAll(regHOL3pred);
        result.append(genModalTypes(allModals));
        //System.out.println("Modals.genAllModalSystems(): allModals size: " + allModals.size());
        for (String s : allModals) {
            if (deontics.contains(s))
                result.append(genModalSystem(s, ModalSystem.D));
            else
                result.append(genModalSystem(s, ModalSystem.T));
        }
        return result.toString();
    }

    /***************************************************************
     * Generates the appropriate modal system
     * See https://en.wikipedia.org/wiki/Modal_logic
     * K := no conditions
     * D := serial
     * T := reflexive
     * B := reflexive and symmetric
     * S4 := reflexive and transitive
     * S5 := reflexive and Euclidean
     */
    public static String genModalSystem(String modalOp, ModalSystem modalsys) {

        //System.out.println("Modals.genModalSystem(): modalOp: " + modalOp);
        //System.out.println("Modals.genModalSystem(): modalsys: " + modalsys);
        String result = "";
        switch (modalsys) {
            case K: return "";
            case D: return genFrameAxiom(modalOp,FrameAx.SERIAL);
            case T: return genFrameAxiom(modalOp,FrameAx.REFLEXIVE);
            case B: return genFrameAxiom(modalOp,FrameAx.REFLEXIVE) +
                    genFrameAxiom(modalOp,FrameAx.SYMMETRIC);
            case S4: return genFrameAxiom(modalOp,FrameAx.REFLEXIVE) +
                    genFrameAxiom(modalOp,FrameAx.TRANSITIVE);
            case S5: return genFrameAxiom(modalOp,FrameAx.REFLEXIVE) +
                    genFrameAxiom(modalOp,FrameAx.EUCLIDEAN);
            case D4: return genFrameAxiom(modalOp,FrameAx.TRANSITIVE) +
                    genFrameAxiom(modalOp,FrameAx.SERIAL);
        }
        return result;
    }

    /***************************************************************
     * Generates the appropriate frame axioms for each modal.  Requires
     * regHOLpred or regHOL3pred to have the right modal relations.
     * See https://en.wikipedia.org/wiki/Modal_logic
     * reflexive if w R w, for every w in G
     * symmetric if w R u implies u R w, for all w and u in G
     * transitive if w R u and u R q together imply w R q, for all w, u, q in G.
     * serial if, for every w in G there is some u in G such that w R u.
     * Euclidean if, for every u, t, and w, w R u and w R t implies u R t (by symmetry, it also implies t R u, as well as t R t and u R u)
     */
    public static String genFrameAxiom(String modalOp, FrameAx frameAx) {

        //System.out.println("Modals.genFrameAxiom(): modalOp: " + modalOp);
        //System.out.println("Modals.genFrameAxiom(): frameAx: " + frameAx);
        String quantArgs = "";
        String args = "";
        String accreln = "s__accreln1";
        if (regHOLpred.contains(modalOp)) {
            quantArgs = ", P1:$i";
            args = " @ P1";
            accreln = "s__accreln2";
        }
        if (regHOL3pred.contains(modalOp)) {
            if (regHOL3Modalpred.contains(modalOp)) {
                quantArgs = ", P1:$i, P2:m";
                args = " @ P1 @ P2";
                accreln = "s__accreln3norm";
            }
            else {
                quantArgs = ", P1:$i, P2:$i";
                args = " @ P1 @ P2";
                accreln = "s__accreln3";
            }
        }
        switch (frameAx) {
            case REFLEXIVE:
                return "thf(" + modalOp + "_refl" + ",axiom,(! [W:w" + quantArgs +
                        "] : (" + accreln + " @ s__" + modalOp + args + " @ W @ W))).\n";
            case SYMMETRIC:
                return "thf(" + modalOp + "_symm" + ",axiom,(! [W1:w, W2:w" + quantArgs +
                        "] : ((" + accreln + " @ s__" + modalOp + args + " @ W1 @ W2) => " +
                        "(" + accreln + " @ s__" + modalOp + args + " @ W2 @ W1)))).\n";
            case TRANSITIVE:
                return "thf(" + modalOp + "_trans" + ",axiom,(! [W1:w, W2:w, W3:w" + quantArgs +
                        "] : (((" + accreln + " @ s__" + modalOp + args + " @ W1 @ W2) & " +
                        "(" + accreln + " @ s__" + modalOp + args + " @ W2 @ W3)) => " +
                        "(" + accreln + " @ s__" + modalOp + args + " @ W1 @ W3)))).\n";
            case SERIAL:
                return "thf(" + modalOp + "_ser" + ",axiom,(! [W:w" + quantArgs +
                        "] : (?[U:w] : (" + accreln + " @ s__" + modalOp + args + " @ W @ U)))).\n";
            case EUCLIDEAN:
                return "thf(" + modalOp + "_eucl" + ",axiom,(! [W1:w,W2:2,W3:w" + quantArgs +
                        "] : (((" + accreln + " @ s__" + modalOp + args + " @ W1 @ W2) & " +
                        "(" + accreln + " @ s__" + modalOp + args + " @ W1 @ W3)) => " +
                        "(" + accreln + " @ s__" + modalOp + args + " @ W2 @ W3))).\n";
        }
        System.out.println("Error in genFrameAxiom() invalid frame: " + frameAx);
        return "";
    }

    /***************************************************************
     */
    public static String getTHFHeader(KB kb) {

        addAccrelnDef(kb);
        return 
                // CF: add these lines into getTHFHeader() result string
                "thf(modals_tp,type,(m : $tType)).\n" +
                //"thf(obligation_tp,type,(s__Obligation : m)).\n" +
                //"thf(permission_tp,type,(s__Permission : m)).\n" +
                //"thf(prohibition_tp,type,(s__Prohibition : m)).\n" +
                
                "thf(worlds_tp,type,(w : $tType)).\n" +
                "thf(cworld_tp,type,(s__CW : w)).\n" +
                "thf(s__worlds_tp,type,(s__World : w)).\n" +

                "thf(accreln1_tp,type,s__accreln1 : (m > w > w > $o)).\n" +
                "thf(accreln2_tp,type, s__accreln2: (m > $i > w > w > $o) ).\n" +
                "thf(accreln3_tp,type, s__accreln3: (m > $i > $i > w > w > $o) ).\n" +
                "thf(accreln3norm_tp,type, s__accreln3norm: (m > $i > m > w > w > $o) ).\n" +
                //"thf(accrelnP_tp,type,(s__accrelnP : (m > w > w > $o))).\n" +     // CF: This is no longer needed, we are using accreln[ |2|3] 
                //"thf(knows_tp,type,(s__knows : m)).\n" +
                //"thf(believes_tp,type,(s__believes : m)).\n" +
                //"thf(desires_tp,type,(s__desires : m)).\n" +
                //"thf(desires_accreln_refl,axiom,(! [W:w, P:$i] : (s__accreln2 @ s__desires @ P @ W @ W))).\n" +
                //"thf(knows_accreln_refl,axiom,(! [W:w, P:$i] : (s__accreln2 @ s__knows @ P @ W @ W))).\n" +
                //"thf(believes_accreln_refl,axiom,(! [W:w, P:$i] : (s__accreln2 @ s__believes @ P @ W @ W))).\n" +
                //"thf(holdsDuring_tp,type,(s__holdsDuring : m)).\n" +

                genAllModalSystems();
    }

    /***************************************************************
     */
    public static void worldVarTest1(KB kb) {

        String fstr = "(=> " +
                "(and " +
                  "(instance ?POLICY NoChildrenPolicy) " +
                  "(policyLocationCoverage ?POLICY ?LOC) " +
                  "(policyOwner ?AGENT ?POLICY)) " +
                "(deprivesNorm ?AGENT Permission " +
                  "(exists (?CHILD) " +
                    "(and " +
                      "(instance ?CHILD HumanChild) " +
                        "(located ?CHILD ?LOC)))))";
        Formula f = new Formula(fstr);
        System.out.println("Modals.worldVarTest1()");
        Map<String, Set<String>> typeMap = new HashMap<>();
        System.out.println(processModals(f, kb,typeMap) + "\n\n");
    }

    /***************************************************************
     */
    public static void worldVarTest2(KB kb) {

        String fstr = "(=>\n" +
                "    (instance ?J TransitwayJunction)\n" +
                "    (exists (?W1 ?W2)\n" +
                "        (and\n" +
                "            (instance ?W1 Transitway)\n" +
                "            (instance ?W2 Transitway)\n" +
                "            (connects ?J ?W1 ?W2)\n" +
                "            (not\n" +
                "                (equal ?W1 ?W2)))))";
        Formula f = new Formula(fstr);
        System.out.println("Modals.worldVarTest2()");
        Map<String, Set<String>> typeMap = new HashMap<>();
        System.out.println(processModals(f, kb,typeMap) + "\n\n");
    }

    /***************************************************************
    /* These are the original tests from this file 
     * A combination of different modalities (deontic to temporal) 
     */ 
    public static void someInitialTests(KB kb) {
        
        String fstr = "(<=>\n" +
                "    (modalAttribute ?FORMULA Prohibition)\n" +
                "    (not\n" +
                "        (modalAttribute ?FORMULA Permission)))";
        Formula f = new Formula(fstr);
        Map<String, Set<String>> typeMap = new HashMap<>();
        System.out.println(processModals(f,kb,typeMap) + "\n\n");
  
        fstr = "(=>\n" +
                "    (and\n" +
                "        (instance ?EXPRESS ExpressingApproval)\n" +
                "        (agent ?EXPRESS ?AGENT)\n" +
                "        (patient ?EXPRESS ?THING))\n" +
                "    (or\n" +
                "        (wants ?AGENT ?THING)\n" +
                "        (desires ?AGENT ?THING)))";
        f = new Formula(fstr);
        System.out.println(processModals(f,kb,typeMap)+ "\n\n");
        
        fstr = "(=> " +
                "  (instance ?ARGUMENT Argument ?W1) " +
                "  (exists (?PREMISES ?CONCLUSION) " +
                "    (and " +
                "      (instance ?PREMISES Formula) " +
                "      (instance ?CONCLUSION Argument) " +
                "      (and " +
                "        (equal (PremisesFn ?ARGUMENT ?W1) ?PREMISES) " +
                "        (conclusion ?CONCLUSION ?ARGUMENT ?W1)))))";
        f = new Formula(fstr);
        System.out.println(processModals(f,kb,typeMap)+ "\n\n");

        // this one is wrong due to the two relations not conforming to argument order
        fstr = "(=>\n" +
                "    (confersRight ?FORMULA ?AGENT1 ?AGENT2)\n" +
                "    (holdsRight ?FORMULA ?AGENT2))";
        f = new Formula(fstr);
        System.out.println(processModals(f,kb,typeMap)+ "\n\n");

        fstr = "(holdsDuring (YearFn 2025)\n" +
                "  (knows John \n" +
                "    (believes Sue \n" +
                "      (acquaintance Bill Jane))))";
        f = new Formula(fstr);
        System.out.println(processModals(f,kb,typeMap)+ "\n\n");

        fstr = 
        "(holdsDuring ?T " +
        "   (knows John " + 
        "       (believes Mary " + 
        "           (knows Bill " +
        "               (believes Sue " +
        "                   (=> " +
        "                       (acquaintance Bill Sue) " +
        "                       (acquaintance Bill Jane)))))))";
        f = new Formula(fstr);
        System.out.println(processModals(f,kb,typeMap) + "\n\n");
    }
    
    /************************************************************************************
     * Tests based on ~/workspace/sumo/tests/TQM10.kif
     * Uses same KB instance as main method 
     */
    public static void doTQM10Tests(KB kb) {

        Map<String, Set<String>> typeMap = new HashMap<>();
        // "The US government obliges Agent Smith not to enter Area 51." 
        String fstr = 
        "(confersObligation USGovernment AgentSmith" +
        "  (not " +
        "    (exists (?E)" +
        "      (and" +
        "        (instance ?E Entering)" +
        "        (agent ?E AgentSmith)" +
        "        (destination ?E Area51)))))";
        Formula f = new Formula(fstr);
        System.out.println(processModals(f,kb,typeMap) + "\n\n");

        // "Agents that violate their obligations have a US government disciplinary hearing."
        // CF: Is this example correct? 
        fstr = 
        "(=>" +
        "  (and" +
        "    (confersObligation USGovernment ?A ?F) " +
        "    (not ?F))" +
        "  (exists (?H)" +
        "    (and" +
        "      (instance ?H LegalAction)" +
        "      (plaintiff ?H USGovernment)" +
        "      (defendant ?H ?A))))";
        f = new Formula(fstr);
        System.out.println(processModals(f,kb,typeMap) + "\n\n");
        
        // "Agents that violate their obligations are fired after a US government disciplinary hearing."
        // CF: Is this example correct? 
        fstr = 
        "(=>" +
        "  (and" +
        "    (confersObligation USGovernment ?A ?F)" +
        "    (not ?F)" +
        "    (instance ?H LegalAction)" +
        "    (plaintiff ?H USGovernment)" +
        "    (defendant ?H ?A))" +
        "  (exists (?FIRE)" +
        "    (and" +
        "      (instance ?FIRE TerminatingEmployment)" +
        "      (earlier " +
        "        (WhenFn ?H) " +
        "        (WhenFn ?FIRE))" +
        "      (patient ?FIRE ?A))))";
        f = new Formula(fstr);
        System.out.println(processModals(f,kb,typeMap) + "\n\n");
    }

    /***************************************************************
    /* CFeener
     * Easy and Medium Deontic examples   
     */ 
    public static void deonticTests(KB kb) {
                
        /* This section is Easy problems (use modalAttribute)
         */
        Map<String, Set<String>> typeMap = new HashMap<>();
        if (debug) {
            System.out.println("EASY: Permission - Constitution grants permission pattern");
        }
        String fstr =
            "(=>" +
            "    (instance ?CONST Constitution)" +
            "    (exists (?FORMULA ?PART)" +
            "        (and" +
            "            (instance ?FORMULA Formula)" +
            "            (containsInformation ?FORMULA ?PART)" +
            "            (instance ?PART Proposition)" +
            "            (subProposition ?PART ?CONST)" +
            "            (modalAttribute ?FORMULA Permission))))";
        Formula f = new Formula(fstr);
        System.out.println(processModals(f, kb,typeMap) + "\n\n");

        if (debug) {
            System.out.println("EASY: Obligation - If entering, must enter on path");
        }
        fstr =
            "(=>" +
            "  (and" +
            "    (instance ?E Entering)" +
            "    (destination ?E ?F)" +
            "    (attribute ?F PhysicallyRestrictedRegion))" +
            "  (modalAttribute" +
            "    (exists (?R)" +
            "      (and" +
            "        (entrance ?R ?F)" +
            "        (path ?E ?R))) Obligation))";
        f = new Formula(fstr);
        System.out.println(processModals(f, kb,typeMap) + "\n\n");

        if (debug) {
            System.out.println("EASY: Prohibition, It is prohibited for Bill to walk to the store");
        }
        fstr =
            "(modalAttribute\n" +
            "  (exists (?W ?B)\n" +
            "    (and\n" +
            "      (instance ?W Walking)\n" +
            "      (instance ?B Human)\n" +
            "      (instance ?GS GroceryStore)\n" +
            "      (names \"Bill\" ?B)\n" +
            "      (agent ?W ?B)\n" +
            "      (destination ?W ?GS))) Prohibition)";
        f = new Formula(fstr);
        System.out.println(processModals(f, kb,typeMap) + "\n\n");

        if (debug) {
            System.out.println("EASY: Law - Immigration and Nationality Act");
        }
        fstr =
            "(exists (?FORMULA)"+
            "  (and"+
            "    (instance ?FORMULA Formula)"+
            "    (containsInformation ?FORMULA ImmigrationAndNationalityAct_US)"+
            "    (modalAttribute ?FORMULA Law)))";
        f = new Formula(fstr);
        System.out.println(processModals(f, kb,typeMap) + "\n\n");
        
        if (debug) {
            System.out.println("EASY-MEDIUM: LegislativeBill");
        }
        fstr =
            "(=>" +
            "  (holdsDuring ?TIME1" +
            "    (modalAttribute ?TEXT Law))" +
            "  (exists (?TIME2)" +
            "    (and" +
            "      (holdsDuring ?TIME2" +
            "        (attribute ?TEXT LegislativeBill))" +
            "      (earlier ?TIME2 ?TIME1))))";
        f = new Formula(fstr);
        System.out.println(processModals(f, kb,typeMap) + "\n\n");
        
        // EASY: InternationalLaw -> Law
        if (debug) {
            System.out.println("EASY: InternationalLaw");
        }
        fstr =
            "(=>" +
            "  (and" +
            "    (instance ?UNCLOS FORMULA)" +
            "    (modalAttribute ?UNCLOS InternationalLaw))" +
            "  (modalAttribute ?UNCLOS Law))";
        f = new Formula(fstr);
        System.out.println(processModals(f, kb,typeMap) + "\n\n");
        
        
        // TODO: Illegal, Legal, and Ally (all under EASY)
        
        // EASY-MEDIUM: Nested problem from Hotel.kif 
        if (debug) {
            System.out.println("EASY-MEDIUM: Nested problem");
        }
        fstr =    
            "(=>" +
            "  (and" +
            "    (instance ?POLICY ChildrenPolicy)" +
            "    (policyOwner ?AGENT ?POLICY)" +
            "    (policyLocationCoverage ?POLICY ?LOC))" +
            "  (or" +
            "    (containsInformation ?POLICY" +
            "      (modalAttribute" +
            "        (exists (?CUST1)" +
            "          (and" +
            "            (customer ?CUST1 ?AGENT)" +
            "            (instance ?CUST1 HumanChild))) Possibility))" +
            "    (containsInformation ?POLICY" +
            "      (not" +
            "        (modalAttribute" +
            "          (exists (?CUST2)" +
            "            (and" +
            "              (customer ?CUST2 ?AGENT)" +
            "              (instance ?CUST2 HumanChild))) Possibility)))))";
        f = new Formula(fstr);
        System.out.println(processModals(f, kb,typeMap) + "\n\n");
        
        
        /* This half is Medium examples (the "Confers" family) 
         */
        
        if (debug) {
            System.out.println("MEDIUM: confersNorm, permission vs prohibition");
        }
        fstr =
            "(=>" +
            "  (and" +
            "    (instance ?USG GovernmentOrganization)" +
            "    (confersNorm ?USG Permission ?FORMULA))" +
            "  (not" +
            "    (confersNorm ?USG Prohibition ?F)))";
        f = new Formula(fstr);
        System.out.println(processModals(f, kb,typeMap) + "\n\n");
        
        if (debug) {
            System.out.println("MEDIUM: confersNorm - Must enter restricted region through entrance");
        }
        fstr =
            "(=>" +
            "  (and" +
            "    (instance ?E Entering)" +
            "    (instance ?G GovernmentOrganization)" +
            "    (destination ?E ?F)" +
            "    (attribute ?F PhysicallyRestrictedRegion)" +
            "    (located ?G ?F))" +
            "  (confersNorm ?G Obligation" +
            "    (exists (?R)" +
            "      (and" +
            "        (entrance ?R ?F)" +
            "        (path ?E ?R)))))";
        f = new Formula(fstr);
        System.out.println(processModals(f, kb,typeMap) + "\n\n");
        
        if (debug) {
            System.out.println("MEDIUM: confersObligation - USG obliges Bill to use the entrance to get in");
        }
        fstr =
            "(=>" +
            "  (and" +
            "    (instance ?E Entering)" +
            "    (instance ?G GovernmentOrganization)" +
            "    (instance ?BILL Human)" +
            "    (destination ?E ?F)" +
            "    (attribute ?F PhysicallyRestrictedRegion)" +
            "    (located ?G ?F))" +
            "  (confersObligation ?G ?BILL" +
            "    (exists (?R)" +
            "      (and" +
            "        (entrance ?R ?F)" +
            "        (path ?E ?R))) ))";
        f = new Formula(fstr);
        System.out.println(processModals(f, kb,typeMap) + "\n\n");

        if (debug) {
            System.out.println("MEDIUM: confersObligation - The US government obliges Agent Smith not to enter Area 51");
        }
        fstr =
            "(confersObligation USGovernment AgentSmith" +
            "  (not" +
            "    (exists (?E)" +
            "      (and" +
            "        (instance ?E Entering)" +
            "        (agent ?E AgentSmith)" +
            "        (destination ?E Area51)))) )";
        f = new Formula(fstr);
        System.out.println(processModals(f, kb,typeMap) + "\n\n");

        if (debug) {
            System.out.println("MEDIUM: Agents that violate their obligations have a US government disciplinary hearing");
        }
        fstr =
            "(=>" +
            "  (and" +
            "    (confersObligation USGovernment ?A ?F)" +
            "    (not ?F))" +
            "  (exists (?H)" +
            "    (and" +
            "      (instance ?H LegalAction)" +
            "      (plaintiff ?H USGovernment)" +
            "      (defendant ?H ?A))))";
        f = new Formula(fstr);
        System.out.println(processModals(f, kb,typeMap) + "\n\n");

        if (debug) {
            System.out.println("MEDIUM: Agents that violate their obligations are fired after a US government disciplinary hearing");
        }
        fstr =
            "(=>" +
            "  (and" +
            "    (confersObligation USGovernment ?A ?F)" +
            "    (not ?F)" +
            "    (instance ?H LegalAction)" +
            "    (plaintiff ?H USGovernment)" +
            "    (defendant ?H ?A))" +
            "  (exists (?FIRE)" +
            "    (and" +
            "      (instance ?FIRE TerminatingEmployment)" +
            "      (earlier" +
            "        (WhenFn ?H)" +
            "        (WhenFn ?FIRE))" +
            "      (patient ?FIRE ?A))))";
        f = new Formula(fstr);
        System.out.println(processModals(f, kb,typeMap) + "\n\n");

        // MEDIUM: deprivesNorm -> confersNorm
        if (debug) {
            System.out.println("MEDIUM: deprivesNorm implies confersNorm (Prohibition -> Permission)");
        }
        fstr =
            "(=>" +
            "  (deprivesNorm ?AGENT Prohibition ?F)" +
            "  (confersNorm ?AGENT Permission ?F))";
        f = new Formula(fstr);
        System.out.println(processModals(f, kb,typeMap) + "\n\n");
        
        
        if (debug) {
            System.out.println("MEDIUM to HARD: confersRight");
        }
        fstr =
            "(=>" +
            "    (and" +
            "        (facility ?AGENT ?OBJ)" +
            "        (customer ?CUST ?AGENT)" +
            "        (instance ?X ?OBJ)" +
            "        (desires ?CUST" +
            "            (exists (?PROC)" +
            "                (and" +
            "                    (instance ?PROC IntentionalProcess)" +
            "                    (patient ?PROC ?X)" +
            "                    (agent ?PROC ?CUST)))))" +
            "    (modalAttribute" +
            "        (confersRight ?AGENT ?CUST" +
            "            (uses ?X ?CUST)) Possibility))";
        f = new Formula(fstr);
        System.out.println(processModals(f, kb,typeMap) + "\n\n");
        
        if (debug) { 
            System.out.println("MEDIUM: confers norm example");
        }
        fstr =
            "(=>\n" +
            "  (and\n" +
            "    (instance ?POLICY PetsAllowedPolicy)" +
            "    (policyLocationCoverage ?POLICY ?LOC)" +
            "    (policyOwner ?AGENT ?POLICY))" +
            "  (confersNorm ?AGENT Permission" +
            "    (exists (?PET)" +
            "      (and" +
            "        (instance ?PET DomesticAnimal)" +
            "        (located ?PET ?LOC))) ))";
        f = new Formula(fstr);
        System.out.println(processModals(f, kb,typeMap) + "\n\n");

        // MEDIUM-HARD (nested): holdsDuring / Sally example
        /*if (debug) { 
            System.out.println("HARD: holdsDuring - Sally is aware of the deadline");
        } 
        fstr =
            "(exists (?S)" +
            "  (and" +
            "    (instance ?S Human)" +
            "    (knows ?S" +
            "      (exists (?D ?P ?A)" +
            "        (holdsDuring" +
            "          (and" +
            "            (instance ?P Process)" +
            "            (agent ?P ?A)" +
            "            (finishes ?D (WhenFn ?P))))))))";
        f = new Formula(fstr);
        System.out.println(processModals(f, kb) + "\n\n");
        */
    }

    /** ***************************************************************
     */
    public static void testFrameAx() {

        System.out.println("testFrameAx():");
        System.out.println(genModalSystem("Possibility",ModalSystem.S5));
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("Modals");
        System.out.println("  h - show this help screen");
        System.out.println("  t - run tests");
        System.out.println("  r - tRanslate to KB to THF with modals");
        System.out.println("  --form \"<fomula>\" - translate one formula with modals");
    }

    /***************************************************************
     */
    public static void main(String[] args) {

        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        if (argMap.isEmpty() || argMap.containsKey("h"))
            showHelp();
        else {
            if (argMap.containsKey("r")) {
                SUMOformulaToTPTPformula.setHideNumbers(false);
                KBmanager.getMgr().initializeOnce();
                KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
                System.out.println("Modals.main(): completed init");
                System.out.println("Modals.main(): KB loaded");
                THFnew.waitForBackgroundGeneration();
                System.out.println("Modals.main(): translate to THF with modals");
                THFnew.transModalTHF(kb);
            }
            else if (argMap.containsKey("t")) {
                System.out.println("Modals: run tests");
                //someInitialTests(kb);
                //doTQM10Tests(kb);
                //deonticTests(kb);
                //System.out.println(genAllModalSystems());
                KBmanager.getMgr().initializeOnce();
                KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
                System.out.println("Modals,main(): init complete");
                worldVarTest1(kb);
                worldVarTest2(kb);
            }
            else if (argMap.containsKey("form")) {
                SUMOformulaToTPTPformula.setHideNumbers(false);
                KBmanager.getMgr().initializeOnce();
                KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
                Map<String, Set<String>> typeMap = new HashMap<>();
                System.out.println(processModals(new Formula(argMap.get("form").get(0)),kb,typeMap));
            }
        }
    }
}
