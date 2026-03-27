package com.articulate.sigma.trans;

import com.articulate.sigma.CLIMapParser;
import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;

import java.io.IOException;
import java.util.*;

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
        K,D,T,B,S4,S5}

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
    public static Formula handleHOL3pred(Formula f, KB kb, Integer worldNum) {

        StringBuilder fstring = new StringBuilder();
        List<Formula> flist = f.complexArgumentsToArrayList(1);
        Formula arg1   = flist.get(0);
        Formula arg2  = flist.get(1);
        worldNum = worldNum + 1;
        fstring.append("(=> (accreln3 ").append(f.car()).append(Formula.SPACE);
        fstring.append(arg1).append(Formula.SPACE).append(arg2).append(Formula.SPACE);
        // Accounts for Constant World (world 0)
        if (worldNum - 1 == 0) { 
            fstring.append(" CW");
        }
        else {
            fstring.append(" ?W").append(worldNum - 1);
        }
        fstring.append(" ?W").append(worldNum).append(") ");
        fstring.append(Formula.SPACE).append(processRecurse(flist.get(2),kb,worldNum));
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
    public static Formula handleHOLpred(Formula f, KB kb, Integer worldNum) {

        List<Formula> flist = f.complexArgumentsToArrayList(1); // args after the head
        // Expect: flist.get(0) = "agent"/parameter, flist.get(1) = formula argument
        worldNum = worldNum + 1;

        // Recursively process the “parameter” term as well
        Formula param = processRecurse(flist.get(0), kb, worldNum - 1);
        Formula embedded = processRecurse(flist.get(1), kb, worldNum);

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
            fstring.append(" ?W").append(worldNum - 1);
        }
        fstring.append(" ?W").append(worldNum)
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
    public static Formula handleModalAttribute(Formula f, KB kb, Integer worldNum) {

        StringBuilder fstring = new StringBuilder();
        List<Formula> flist = f.complexArgumentsToArrayList(1); // [F, M]
        if (flist == null || flist.size() < 2) {
            throw new IllegalArgumentException("modalAttribute requires at least 2 arguments");
        }
        int prevWorld = worldNum;
        int currWorld  = worldNum + 1;
        Formula modality = flist.get(1); // modality is the second complex arg
        Formula formula  = flist.get(0);
        fstring.append("(=> (accreln1 ").append(modality).append(Formula.SPACE);
        // Account for CW (constant world):
        if (prevWorld == 0) {
            fstring.append("CW");
        }
        else {
            fstring.append("?W").append(prevWorld);
        }
        fstring.append(Formula.SPACE).append("?W").append(currWorld).append(") ");
        // Recurse once on the embedded formula at the new current world:
        fstring.append(processRecurse(formula, kb, currWorld));
        fstring.append(Formula.RP);
        Formula result = new Formula();
        result.read(fstring.toString());
        return result;
    }

    /***************************************************************
     */
    public static Formula processRecurse(Formula f, KB kb, Integer worldNum) {

        if (f.atom())
            return f;
        if (f.empty())
            return f;

        if (f.listP()) {
            if (regHOL3pred.contains(f.car()))
                return handleHOL3pred(f,kb,worldNum);
            if (regHOLpred.contains(f.car())) {
                return handleHOLpred(f, kb, worldNum);
            }
            if (f.car().equals("modalAttribute")) {
                return handleModalAttribute(f, kb, worldNum);
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
                fstring.append(Formula.SPACE).append(processRecurse(arg, kb, worldNum));
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
                                fstring.append(" ?W").append(worldNum);
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
        sig.add("Entity");
        sig.add("World");
        sig.add("World");
        kb.kbCache.signatures.put("accreln",sig);
    }

    /***************************************************************
     */
    public static Formula processModals(Formula f, KB kb) {

        addAccrelnDef(kb);
        //addAccrelnDefP(kb);
        // Start at index 0 for constant world (W0 = CW) 
        int worldNum = 1;
        //if (!f.isHigherOrder(kb))
        //    return f;
        Formula result = processRecurse(f,kb,worldNum);
        String fstring = result.getFormula();
        result.read(fstring);
        Set<String> types = new HashSet<>();
        types.add("World");
        for (int i = 0; i <= worldNum; i++) {
            result.varTypeCache.put("?W" + i,types);
        } 
        return result;
    }

    /***************************************************************
     */
    public static String getTFFHeader() {

        return "tff(worlds_tp,type,(w : $tType)).\n" +
                "tff(modals_tp,type,(m : $tType)).\n" +
                "tff(accreln_tp,type,(s__accreln : (m * $i * w * w) > $o)).";
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
            quantArgs = ", P1:$i, P2:$i";
            args = " @ P1 @ P2";
            accreln = "s__accreln3";
        }
        switch (frameAx) {
            case REFLEXIVE:
                return "thf(" + modalOp + "_refl" + ",axiom,(! [W:w" + quantArgs +
                        "] : (" + accreln + " @ s__" + modalOp + args + " @ W @ W))).\n";
            case SYMMETRIC:
                return "thf(" + modalOp + "_symm" + ",axiom,(! [W1:w, W2:w" + quantArgs +
                        "] : ((" + accreln + " @ s__" + modalOp + args + " @ W1 @ W2) => " +
                        "(" + accreln + " @ s__" + modalOp + " @ P @ W2 @ W1)))).\n";
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
    public static String getTHFHeader() {

        return 
                // CF: add these lines into getTHFHeader() result string
                "thf(modals_tp,type,(m : $tType)).\n" +
                "thf(obligation_tp,type,(s__Obligation : m)).\n" +
                "thf(permission_tp,type,(s__Permission : m)).\n" +
                "thf(prohibition_tp,type,(s__Prohibition : m)).\n" +
                
                "thf(worlds_tp,type,(w : $tType)).\n" +
                "thf(cworld_tp,type,(s__CW : w)).\n" +
                "thf(s__worlds_tp,type,(s__World : w)).\n" +

                "thf(accreln1_tp,type,s__accreln1 : (m > w > w > $o)).\n" +
                "thf(accreln2_tp,type, s__accreln2: m > $i > w > w > $o ).\n" +
                "thf(accreln3_tp,type, s__accreln3: m > $i > $i > w > w > $o ).\n" +
                //"thf(accrelnP_tp,type,(s__accrelnP : (m > w > w > $o))).\n" +     // CF: This is no longer needed, we are using accreln[ |2|3] 
                "thf(knows_tp,type,(s__knows : m)).\n" +
                "thf(believes_tp,type,(s__believes : m)).\n" +
                "thf(desires_tp,type,(s__desires : m)).\n" +
                "thf(desires_accreln_refl,axiom,(! [W:w, P:$i] : (s__accreln2 @ s__desires @ P @ W @ W))).\n" +
                "thf(knows_accreln_refl,axiom,(! [W:w, P:$i] : (s__accreln2 @ s__knows @ P @ W @ W))).\n" +
                "thf(believes_accreln_refl,axiom,(! [W:w, P:$i] : (s__accreln2 @ s__believes @ P @ W @ W))).\n" +
                // ISSUE 6
                "thf(holdsDuring_tp,type,(s__holdsDuring : m)).\n" +
                genAllModalSystems();
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
        System.out.println(processModals(f,kb) + "\n\n");
  
        fstr = "(=>\n" +
                "    (and\n" +
                "        (instance ?EXPRESS ExpressingApproval)\n" +
                "        (agent ?EXPRESS ?AGENT)\n" +
                "        (patient ?EXPRESS ?THING))\n" +
                "    (or\n" +
                "        (wants ?AGENT ?THING)\n" +
                "        (desires ?AGENT ?THING)))";
        f = new Formula(fstr);
        System.out.println(processModals(f,kb)+ "\n\n");
        
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
        System.out.println(processModals(f,kb)+ "\n\n");

        // this one is wrong due to the two relations not conforming to argument order
        fstr = "(=>\n" +
                "    (confersRight ?FORMULA ?AGENT1 ?AGENT2)\n" +
                "    (holdsRight ?FORMULA ?AGENT2))";
        f = new Formula(fstr);
        System.out.println(processModals(f,kb)+ "\n\n");

        fstr = "(holdsDuring (YearFn 2025)\n" +
                "  (knows John \n" +
                "    (believes Sue \n" +
                "      (acquaintance Bill Jane))))";
        f = new Formula(fstr);
        System.out.println(processModals(f,kb)+ "\n\n");

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
        System.out.println(processModals(f,kb) + "\n\n");
    }
    
    /************************************************************************************
     * Tests based on ~/workspace/sumo/tests/TQM10.kif
     * Uses same KB instance as main method 
     */
    public static void doTQM10Tests(KB kb) {
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
        System.out.println(processModals(f,kb) + "\n\n");

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
        System.out.println(processModals(f,kb) + "\n\n");
        
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
        System.out.println(processModals(f,kb) + "\n\n");
    }

    /***************************************************************
    /* CFeener
     * Easy and Medium Deontic examples   
     */ 
    public static void deonticTests(KB kb) {
                
        /* This section is Easy problems (use modalAttribute)
         */
         
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
        System.out.println(processModals(f, kb) + "\n\n");

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
        System.out.println(processModals(f, kb) + "\n\n");

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
        System.out.println(processModals(f, kb) + "\n\n");

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
        System.out.println(processModals(f, kb) + "\n\n");
        
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
        System.out.println(processModals(f, kb) + "\n\n");
        
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
        System.out.println(processModals(f, kb) + "\n\n");
        
        
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
        System.out.println(processModals(f, kb) + "\n\n");
        
        
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
        System.out.println(processModals(f, kb) + "\n\n");
        
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
        System.out.println(processModals(f, kb) + "\n\n");
        
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
        System.out.println(processModals(f, kb) + "\n\n");

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
        System.out.println(processModals(f, kb) + "\n\n");

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
        System.out.println(processModals(f, kb) + "\n\n");

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
        System.out.println(processModals(f, kb) + "\n\n");

        // MEDIUM: deprivesNorm -> confersNorm
        if (debug) {
            System.out.println("MEDIUM: deprivesNorm implies confersNorm (Prohibition -> Permission)");
        }
        fstr =
            "(=>" +
            "  (deprivesNorm ?AGENT Prohibition ?F)" +
            "  (confersNorm ?AGENT Permission ?F))";
        f = new Formula(fstr);
        System.out.println(processModals(f, kb) + "\n\n");
        
        
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
        System.out.println(processModals(f, kb) + "\n\n"); 
        
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
        System.out.println(processModals(f, kb) + "\n\n");

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
                System.out.println(genAllModalSystems());
            }
        }
    }
}
