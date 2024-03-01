package com.articulate.sigma.trans;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.KIF;

import java.util.ArrayList;
import java.util.Arrays;

public class HOL {

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
        fstring.append("))");
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
        fstring.append("))");
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
            ArrayList<Formula> flist = f.complexArgumentsToArrayList(1);
            StringBuffer fstring = new StringBuffer();
            fstring.append("(" + f.car());
            for (Formula arg : flist)
                fstring.append(" " + processRecurse(arg,kb,worldNum));
            if (Formula.isLogicalOperator(f.car()))
                fstring.append(")");
            else
                fstring.append(" ?W" + worldNum + ")");
            Formula result = new Formula();
            result.read(fstring.toString());
            return result;
        }
        return f;
    }

    /***************************************************************
     */
    public static Formula processHigherOrder(Formula f, KB kb) {

        int worldNum = 1;
        Formula result = new Formula();
        //if (!f.isHigherOrder(kb))
        //    return f;
        result = processRecurse(f,kb,worldNum);
        String fstring = result.getFormula();
        result.read(fstring);
        return result;
    }

    /***************************************************************
     */
    public static void addTFFHeader(KB kb) {

        System.out.println("tff(worlds_tp,type,(w : $tType)).\n" +
                "tff(modals_tp,type,(m : $tType)).\n" +
                "tff(accreln_tp,type,(accreln : (m * $i * w * w) > $o)).");
    }

    /***************************************************************
     */
    public static void addTHFHeader(KB kb) {

        System.out.println("thf(worlds_tp,type,(w : $tType)).\n" +
                "thf(modals_tp,type,(m : $tType)).\n" +
                "thf(accreln_tp,type,(accreln : (m > $i > w > w > $o))).");
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
        System.out.println(processHigherOrder(f,kb) + "\n\n");

        fstr = "(=>\n" +
                "    (and\n" +
                "        (instance ?EXPRESS ExpressingApproval)\n" +
                "        (agent ?EXPRESS ?AGENT)\n" +
                "        (patient ?EXPRESS ?THING))\n" +
                "    (or\n" +
                "        (wants ?AGENT ?THING)\n" +
                "        (desires ?AGENT ?THING)))";
        f = new Formula(fstr);
        System.out.println(processHigherOrder(f,kb));

    }
}
