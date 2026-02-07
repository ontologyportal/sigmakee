package com.articulate.sigma.trans;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;

import java.util.*;

public class Modals {

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
                    "confersNorm",          // TODO: Move Formula to 3rd 
                    "confersObligation",    // TODO: Move Formula to 3rd 
                    "confersRight",         // TODO: Move Formula to 3rd 
                    "deprivesNorm",         // TODO: Move Formula to 3rd 
                    "hasPurposeForAgent"    // TODO: Move Formula to 3rd 
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
        Formula entity   = flist.get(flist.size() - 2);
        Formula cogAgent  = flist.get(flist.size() - 1);
        worldNum = worldNum + 1;
        fstring.append("(=> (accreln3 ").append(f.car()).append(Formula.SPACE);
        fstring.append(entity).append(Formula.SPACE).append(cogAgent).append(Formula.SPACE);
        // Accounts for Constant World (world 0)
        if (worldNum - 1 == 0) { 
            fstring.append(" CW");
        } else {
            fstring.append(" ?W").append(worldNum - 1);
        }
        fstring.append(" ?W").append(worldNum).append(") ");
        fstring.append(Formula.SPACE).append(processRecurse(flist.get(0),kb,worldNum));
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
        } else {
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
        worldNum = worldNum + 1;
        
        // a normal or dyadic deontic 
        if (flist.size() == 2) {
        // Monadic case: (modalAttribute F M)
        fstring.append("(=> (accreln1 ")
                .append(flist.get(1)); // modality
        // Accounts for Constant World (world 0)
        if (worldNum - 1 == 0) { 
            fstring.append(" CW");
        } else {
            fstring.append(" ?W").append(worldNum - 1);
        }
        fstring.append(" ?W").append(worldNum)
                .append(") ")
                .append(processRecurse(flist.get(0), kb, worldNum))
                .append(")");
        /* } else if (flist.size() == 3) {
        // Dyadic case: (modalAttribute F C M)
            Formula formula = flist.get(0);
            Formula condition = flist.get(1);
            Formula modality = flist.get(2);

            fstring.append("(=> (and (accrelnP ")
                    .append(modality);
            // Accounts for Constant World (world 0)
            if (worldNum - 1 == 0) { 
                fstring.append(" CW");
            } else {
                fstring.append(" ?W").append(worldNum - 1);
            }
            fstring.append(" ?W").append(worldNum)
                    .append(") ")
                    .append(processRecurse(condition, kb, worldNum))
                    .append(") ")
                    .append(processRecurse(formula, kb, worldNum))
                    .append(")"); */
        } else {
            throw new IllegalArgumentException("modalAttribute requires 2 or 3 arguments");
    }

        // Build antecedent: (accrelnP M ?W_{n-1} ?W_n)
        fstring.append("(=> (accreln1 ")
                .append(flist.get(1));           // the modal attribute constant, e.g. Necessity, Legal
        // Accounts for Constant World (world 0)
        if (worldNum - 1 == 0) { 
            fstring.append(" CW");
        } else {
            fstring.append(" ?W").append(worldNum - 1);
        }
        fstring.append(" ?W").append(worldNum)
                .append(") ");

        // Consequent: recursively process the embedded formula at the new world.
        fstring.append(processRecurse(flist.get(0), kb, worldNum));

        fstring.append(Formula.RP);

        Formula result = new Formula();
        result.read(fstring.toString());
        return result;
    }

    /***************************************************************
     */
    public static Formula processRecurse(Formula f, KB kb, Integer worldNum) {

        if (f.atom()) {
            return f;
        }
        if (f.empty()) {
            return f;
        }

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
                            } else {
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

    /**
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
        int worldNum = 0;
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
     */
    public static String getTHFHeader() {

        return 
                // CF: add these lines into getTHFHeader() result string
                "thf(obligation_tp,type,(s__Obligation : m)).\n" +
                "thf(permission_tp,type,(s__Permission : m)).\n" +
                "thf(prohibition_tp,type,(s__Prohibition : m)).\n" +
                
                "thf(worlds_tp,type,(w : $tType)).\n" +
                "thf(s__worlds_tp,type,(s__World : w)).\n" +
                "thf(modals_tp,type,(m : $tType)).\n" +
                "thf(accreln_tp,type,(s__accreln : (m > $i > w > w > $o))).\n" +
                "thf(accreln_tp,type, accreln1: m > $i > w > w > $o )." +
                "thf(accreln_tp,type, accreln2: m > $i > $i > w > w > $o )." + 
                //"thf(accrelnP_tp,type,(s__accrelnP : (m > w > w > $o))).\n" +     // CF: This is no longer needed, we are using accreln[ |2|3] 
//                "thf(knows_tp,type,(s__knows : m)).\n" +
//                "thf(believes_tp,type,(s__believes : m)).\n" +
//                "thf(desires_tp,type,(s__desires : m)).\n" +
                "thf(desires_accreln_refl,axiom,(! [W:w, P:$i] : (s__accreln @ s__desires @ P @ W @ W))).\n" +
                "thf(knows_accreln_refl,axiom,(! [W:w, P:$i] : (s__accreln @ s__knows @ P @ W @ W))).\n" +
                "thf(believes_accreln_refl,axiom,(! [W:w, P:$i] : (s__accreln @ s__believes @ P @ W @ W))).\n" +
                // ISSUE 6
                "thf(holdsDuring_tp,type,(s__holdsDuring : m)).\n";

    }
    
    /************************************************************************************
     * Tests based on ~/workspace/sumo/tests/TQM10.kif
     * Uses same KB instance as main method 
     */
    public static void doTQM10Tests(KB kb) {
        // "The US government obliges Agent Smith not to enter Area 51." 
        String fstr = 
        "(confersObligation " +
        "  (not " +
        "    (exists (?E)" +
        "      (and" +
        "        (instance ?E Entering)" +
        "        (agent ?E AgentSmith)" +
        "        (destination ?E Area51)))) " +
        "  USGovernment AgentSmith)";
        Formula f = new Formula(fstr);
        System.out.println(processModals(f,kb) + "\n\n");

        // "Agents that violate their obligations have a US government disciplinary hearing."
        // CF: Is this example correct? 
        fstr = 
        "(=>" +
        "  (and" +
        "    (confersObligation ?F USGovernment ?A)" +
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
        /*fstr = 
        "(=>" +
        "  (and" +
        "    (confersObligation ?F USGovernment ?A)" +
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
        System.out.println(processModals(f,kb) + "\n\n");*/
    }

    /***************************************************************
     */
    public static void main(String[] args) {
    
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        System.out.println("HOL.main(): completed init");
        
        // Examples: 
        
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
        
        // More tests: 
        doTQM10Tests(kb);
   
    }
}
