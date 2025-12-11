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
                    "entails",
                    "expects",
                    "hasPurpose",
                    "hasPurposeForAgent",
                    "holdsDuring",
                    "holdsObligation",
                    "holdsRight",
                    "increasesLikelihood",
                    "independentProbability",
                    "knows",
                    "modalAttribute",
                    "permits",
                    "prefers",
                    "prohibits",
                    "ProbabilityFn",
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
    public static final List<String> regHOLpred = new ArrayList<>(
            Arrays.asList(
                    "considers",
                    "sees",
                    "believes",
                    "knows",
                    "holdsDuring",
                    "desires"
            ));

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
                    // Arithmetic Op
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
                    "accrelnP",
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

        StringBuilder fstring = new StringBuilder();
        List<Formula> flist = f.complexArgumentsToArrayList(1); // args after the head
        // Expect: flist.get(0) = "agent"/parameter, flist.get(1) = formula argument
        worldNum = worldNum + 1;

        fstring.append("(=> (accreln ")
                .append(f.car())                // modal kind / operator, e.g. confersObligation, knows, desires
                .append(Formula.SPACE)
                .append(flist.get(0))           // parameter (e.g. USGovernment, agent)
                .append(" ?W").append(worldNum - 1)
                .append(" ?W").append(worldNum)
                .append(") ");

        // Recursively process the embedded formula under the *new* world index.
        fstring.append(Formula.SPACE)
                .append(processRecurse(flist.get(1), kb, worldNum));

        fstring.append(Formula.RP);

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

        // Build antecedent: (accrelnP M ?W_{n-1} ?W_n)
        fstring.append("(=> (accrelnP ")
                .append(flist.get(1))           // the modal attribute constant, e.g. Necessity, Legal
                .append(" ?W").append(worldNum - 1)
                .append(" ?W").append(worldNum)
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
            for (Formula arg : flist)
                fstring.append(Formula.SPACE).append(processRecurse(arg,kb,worldNum));

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

                    fstring.append(" ?W").append(worldNum);
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
     * Add the signature for the Kripke accessibility relation
     */
    public static void addAccrelnDefP(KB kb) {

        List<String> sig = new ArrayList<>();
        sig.add(""); // empty 0th argument for relations
        sig.add("Modal");
        sig.add("World");
        sig.add("World");
        kb.kbCache.signatures.put("accrelnP",sig);
    }

    /***************************************************************
     */
    public static Formula processModals(Formula f, KB kb) {

        addAccrelnDef(kb);
        addAccrelnDefP(kb);
        int worldNum = 1;
        //if (!f.isHigherOrder(kb))
        //    return f;
        Formula result = processRecurse(f,kb,worldNum);
        String fstring = result.getFormula();
        result.read(fstring);
        Set<String> types = new HashSet<>();
        types.add("World");
        for (int i = 1; i <= worldNum; i++)
            result.varTypeCache.put("?W" + i,types);
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

        return "thf(worlds_tp,type,(w : $tType)).\n" +
                "thf(s__worlds_tp,type,(s__World : w)).\n" +
                "thf(modals_tp,type,(m : $tType)).\n" +
                "thf(accreln_tp,type,(s__accreln : (m > $i > w > w > $o))).\n" +
                "thf(accrelnP_tp,type,(s__accrelnP : (m > w > w > $o))).\n" +
//                "thf(knows_tp,type,(s__knows : m)).\n" +
//                "thf(believes_tp,type,(s__believes : m)).\n" +
//                "thf(desires_tp,type,(s__desires : m)).\n" +
                "thf(desires_accreln_refl,axiom,(! [W:w, P:$i] : (s__accreln @ s__desires__m @ P @ W @ W))).\n" +
                "thf(knows_accreln_refl,axiom,(! [W:w, P:$i] : (s__accreln @ s__knows__m @ P @ W @ W))).\n" +
                "thf(believes_accreln_refl,axiom,(! [W:w, P:$i] : (s__accreln @ s__believes__m @ P @ W @ W))).\n";
                // ISSUE 6
//                "thf(holdsDuring_tp,type,(s__holdsDuring : m)).\n";

    }

    /***************************************************************
     */
    public static void main(String[] args) {

        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        System.out.println("HOL.main(): completed init");
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
    }
}