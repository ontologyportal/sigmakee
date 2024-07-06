package com.articulate.sigma.trans;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Modals {

    public static final ArrayList<String> formulaPreds = new ArrayList<String>(
            Arrays.asList("KappaFn", "ProbabilityFn", "believes",
                    "causesProposition", "conditionalProbability",
                    "confersNorm", "confersObligation", "confersRight",
                    "considers", "containsFormula", "decreasesLikelihood",
                    "deprivesNorm", "describes", "desires", "disapproves",
                    "doubts", "entails", "expects", "hasPurpose",
                    "hasPurposeForAgent", "holdsDuring", "holdsObligation",
                    "holdsRight", "increasesLikelihood",
                    "independentProbability", "knows", "modalAttribute",
                    "permits", "prefers", "prohibits", "rateDetail", "says",
                    "treatedPageDefinition", "visitorParameter"));

    public static final ArrayList<String> dualFormulaPreds = new ArrayList<String>(
            Arrays.asList("causesProposition", "conditionalProbability",
                    "decreasesLikelihood",
                    "entails",  "increasesLikelihood",
                    "independentProbability","prefers"));

    public static final ArrayList<String> regHOLpred = new ArrayList<String>(
            Arrays.asList("considers","sees","believes","knows","holdsDuring","desires"));

    /***************************************************************
     * Handle the predicates given in regHOLpred, which have a parameter
     * followed by a formula.
     */
    public static Formula handleHOLpred(Formula f, KB kb, Integer worldNum) {

        StringBuffer fstring = new StringBuffer();
        ArrayList<Formula> flist = f.complexArgumentsToArrayList(1);
        worldNum = worldNum + 1;
        fstring.append("(=> (accreln " + f.car() + " " +
                flist.get(0) + " ?W" + (worldNum - 1) + " ?W" + worldNum + ") ");
        fstring.append(" " + processRecurse(flist.get(1),kb,worldNum));
        fstring.append(")");
        Formula result = new Formula();
        result.read(fstring.toString());
        return result;
    }

    /***************************************************************
     * handle the predicate modalAttribute
     */
    public static Formula handleModalAttribute(Formula f, KB kb, Integer worldNum) {

        StringBuffer fstring = new StringBuffer();
        ArrayList<Formula> flist = f.complexArgumentsToArrayList(1);
        worldNum = worldNum + 1;
        fstring.append("(=> (accreln modalAttribute " +
                flist.get(1) + " ?W" + (worldNum - 1) + " ?W" + worldNum + ") ");
        fstring.append(processRecurse(flist.get(0),kb,worldNum));
        fstring.append(")");
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
            if (regHOLpred.contains(f.car()))
                return handleHOLpred(f,kb,worldNum);
            if (f.car().equals("modalAttribute"))
                return handleModalAttribute(f,kb,worldNum);
            //System.out.println("Modals.processRecurse(): " + f);
            int argStart = 1;
            if (Formula.isQuantifier(f.car()))
                argStart = 2;
            ArrayList<Formula> flist = f.complexArgumentsToArrayList(argStart);
            StringBuffer fstring = new StringBuffer();
            fstring.append("(" + f.car());
            if (argStart == 2) // append quantifier list without processing
                fstring.append(" " + f.getStringArgument(1));
            for (Formula arg : flist)
                fstring.append(" " + processRecurse(arg,kb,worldNum));
            if (Formula.isLogicalOperator(f.car()) || (f.car().equals(Formula.EQUAL)))
                fstring.append(")");
            else {
                fstring.append(" ?W" + worldNum + ")");
                List<String> sig = kb.kbCache.signatures.get(f.car()); // make sure to update the signature
                sig.add("World");
            }
            Formula result = new Formula();
            result.read(fstring.toString());
            return result;
        }
        return f;
    }

    /***************************************************************
     * Add the signature for the Kripke accessibility relation
     */
    public static void addAccrelnDef(KB kb) {

        ArrayList<String> sig = new ArrayList<>();
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
        int worldNum = 1;
        Formula result = new Formula();
        //if (!f.isHigherOrder(kb))
        //    return f;
        result = processRecurse(f,kb,worldNum);
        String fstring = result.getFormula();
        result.read(fstring);
        HashSet<String> types = new HashSet<String>();
        types.add("World");
        for (int i = 1; i <= worldNum; i++)
            result.varTypeCache.put("?W" + i,types);
        return result;
    }

    /***************************************************************
     */
    public static String getTFFHeader() {

        return "tff(worlds_tp,type,($w : $tType)).\n" +
                "tff(modals_tp,type,(m : $tType)).\n" +
                "tff(accreln_tp,type,(s__accreln : ($m * $i * $w * $w) > $o)).";
    }

    /***************************************************************
     */
    public static String getTHFHeader() {

        return "thf(worlds_tp,type,($w : $tType)).\n" +
                "thf(s__worlds_tp,type,(s__World : $w)).\n" +
                "thf(modals_tp,type,($m : $tType)).\n" +
                "thf(accreln_tp,type,(s__accreln : ($i > $i > $w > $w > $o))).";
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
