package com.articulate.sigma.trans;

/* A start at adding the (conservative) closed world assumption and unique names assumption to SUMO/Sigma.
 */

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.utils.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CWAUNA {

    public static int axiomIndex = 0;

    /** ***************************************************************
     */
    public static String buildDistinct(Set<String> allArgs) {

        StringBuilder sb = new StringBuilder();
        if (allArgs == null || allArgs.size() < 2)
            return "";
        String name = "distinct" + "_" + axiomIndex++;
        sb.append("fof(").append(name).append(",axiom,");
        sb.append("$distinct(");
        for (String arg : allArgs) {
            sb.append("s__").append(arg).append(",\n");
        }
        sb.delete(sb.length()-2,sb.length());
        sb.append(")).");
        return sb.toString();
    }

    /** ***************************************************************
     * build the big axiom that creates the closed word for eqch closed
     * relation
     */
    public static String buildExclusions(KB kb, String rel) {

        StringBuilder sb = new StringBuilder();
        if (kb.kbCache.getArity(rel) != 2)
            return sb.toString();
        String name = "cwa_ex_" + axiomIndex++;
        sb.append("fof(").append(name).append(",axiom,");
        sb.append("![X,Y] : (\ns__").append(rel).append("(X,Y) => (");
        List<Formula> forms = kb.ask("arg", 0, rel);
        System.out.println("buildExclusions(): # formulas: " + forms.size());
        if (forms == null || forms.isEmpty())
            return "";

        List<String> args;
        for (Formula f : forms) {
            args = f.argumentsToArrayListString(1);
            if (Formula.atom(args.get(0)) && Formula.atom(args.get(1))) {
                sb.append(" ( X = s__").append(args.get(0)).append(" & Y = s__").append(args.get(1)).append(" )");
                sb.append(" | \n");
            }
        }
        sb.delete(sb.length()-4,sb.length());
        sb.append(" ))).");
        return sb.toString();
    }

    /** ***************************************************************
     * Collect all constants used in ClosedWorldPredicate(s) and their
     * subrelation(s) and assert them as $distinct.
     */
    public static void runold (KB kb ) {

        System.out.println("CWAUNA.run()");
        List<String> result = new ArrayList<>();
        Set<String> rels = kb.kbCache.getInstancesForType("ClosedWorldPredicate");
        System.out.println("CWAUNA.run(): rels: " + rels);
        List<Formula> forms;
        Set<String> allArgs;
        List<String> args;
        for (String rel : rels) {
            forms = kb.ask("arg", 0, rel);
            System.out.println("CWAUNA.run(): forms: " + forms);
            allArgs = new HashSet<>();
            for (Formula form : forms) {
                if (form.isGround()) {
                    args = form.argumentsToArrayListString(1);
                    if (args != null)
                        allArgs.addAll(args);
                }
            }
            result.add(buildDistinct(allArgs));
        }
        System.out.println(StringUtil.arrayListToCRLFString(result));
    }

    /** ***************************************************************
     * Collect all constants for types (and subclasses) required for ClosedWorldPredicate(s) and their
     * subrelation(s) and assert them as $distinct.
     */
    public static List<String> run(KB kb) {

        System.out.println("CWAUNA.run()");
        List<String> result = new ArrayList<>();
        Set<String> rels = kb.kbCache.getInstancesForType("ClosedWorldPredicate");
        System.out.println("CWAUNA.run(): rels: " + rels);
        String e, d;
        Set<String> sig;
        List<String> arsig;
        Set<String> insts;
        for (String rel : rels) {
            e = buildExclusions(kb,rel);
            if (!StringUtil.emptyString(e))
                result.add(e);
            sig = new HashSet<>();
            arsig = kb.kbCache.getSignature(rel);
            System.out.println("CWAUNA.run(1): " + rel + " with sig: " + arsig);
            if (arsig != null)
                sig.addAll(arsig);
            System.out.println("CWAUNA.run(2): " + rel + " with sig: " + sig);
            for (String t : sig) {
                System.out.println("CWAUNA.run(): for type: " + t);
                insts = kb.kbCache.getInstancesForType(t);
                System.out.println("CWAUNA.run(): for type: " + t + " insts/types: " + insts);
                if (insts != null) {
                    d = buildDistinct(insts);
                    if (!StringUtil.emptyString(d))
                        result.add(d);
                }
            }
        }
        return result;
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("KB class");
        System.out.println("  options (with a leading '-'):");
        System.out.println("  h - show this help screen");
        System.out.println("  a - Add UNA/CWA axioms for KB");
    }

    /** ***************************************************************
     */
    public static void main(String[] args) throws IOException {

        System.out.println("INFO in CWAUNA.main()");
        if (args != null && args.length > 0 && args[0].equals("-h"))
            showHelp();
        else {
            KBmanager.getMgr().initializeOnce();
            String kbName = KBmanager.getMgr().getPref("sumokbname");
            KB kb = KBmanager.getMgr().getKB(kbName);
            if (args != null && args.length > 0 && args[0].contains("a"))
                System.out.println(StringUtil.arrayListToCRLFString(run(kb)));
        }
    }
}
