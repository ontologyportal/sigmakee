package com.articulate.sigma.trans;

/* A start at adding the (conservative) closed world assumption and unique names assumption to SUMO/Sigma.
 */

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.utils.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class CWAUNA {

    public static int axiomIndex = 0;
    /** ***************************************************************
     */
    public static String buildDistinct(HashSet<String> allArgs) {

        StringBuffer sb = new StringBuffer();
        if (allArgs == null || allArgs.size() < 2)
            return "";
        String name = "distinct" + "_" + axiomIndex++;
        sb.append("fof(" + name + ",axiom,");
        sb.append("$distinct(");
        for (String arg : allArgs)
            sb.append("s__" + arg + ",");
        sb.delete(sb.length()-1,sb.length());
        sb.append(")).");
        return sb.toString();
    }

    /** ***************************************************************
     * Collect all constants used in ClosedWorldPredicate(s) and their
     * subrelation(s) and assert them as $distinct.
     */
    public static void run1 (KB kb ) {

        System.out.println("CWAUNA.run()");
        ArrayList<String> result = new ArrayList<>();
        HashSet<String> rels = kb.kbCache.getInstancesForType("ClosedWorldPredicate");
        System.out.println("CWAUNA.run(): rels: " + rels);
        for (String rel : rels) {
            ArrayList<Formula> forms = kb.ask("arg", 0, rel);
            System.out.println("CWAUNA.run(): forms: " + forms);
            HashSet<String> allArgs = new HashSet<>();
            for (Formula form : forms) {
                if (form.isGround()) {
                    ArrayList<String> args = form.argumentsToArrayListString(1);
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
    public static ArrayList<String> run (KB kb ) {

        System.out.println("CWAUNA.run()");
        ArrayList<String> result = new ArrayList<>();
        HashSet<String> rels = kb.kbCache.getInstancesForType("ClosedWorldPredicate");
        System.out.println("CWAUNA.run(): rels: " + rels);
        for (String rel : rels) {
            HashSet<String> sig = new HashSet<>();
            List<String> arsig = kb.kbCache.getSignature(rel);
            System.out.println("CWAUNA.run(1): " + rel + " with sig: " + arsig);
            if (arsig != null)
                sig.addAll(arsig);
            System.out.println("CWAUNA.run(2): " + rel + " with sig: " + sig);
            for (String t : sig) {
                System.out.println("CWAUNA.run(): for type: " + t);
                HashSet<String> insts = kb.kbCache.getInstancesForType(t);
                System.out.println("CWAUNA.run(): for type: " + t + " insts/types: " + insts);
                if (insts != null) {
                    String d = buildDistinct(insts);
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
