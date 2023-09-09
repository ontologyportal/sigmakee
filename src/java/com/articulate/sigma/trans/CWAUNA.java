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
            sb.append("s__" + arg + ",\n");
        sb.delete(sb.length()-2,sb.length());
        sb.append(")).");
        return sb.toString();
    }

    /** ***************************************************************
     * build the big axiom that creates the closed word for eqch closed
     * relation
     */
    public static String buildExclusions(KB kb, String rel) {

        StringBuffer sb = new StringBuffer();
        if (kb.kbCache.getArity(rel) != 2)
            return sb.toString();
        String name = "cwa_ex_" + axiomIndex++;
        sb.append("fof(" + name + ",axiom,");
        sb.append("![X,Y] : (\n" + "s__" + rel + "(X,Y) => (");
        ArrayList<Formula> forms = kb.ask("arg", 0, rel);
        System.out.println("buildExclusions(): # formulas: " + forms.size());
        if (forms == null || forms.size() == 0)
            return "";
        for (Formula f : forms) {
            ArrayList<String> args = f.argumentsToArrayListString(1);
            sb.append(" ( X = s__" + args.get(0) + " & Y = s__" + args.get(1) + " )");
            sb.append (" | \n");
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
    public static ArrayList<String> run(KB kb) {

        System.out.println("CWAUNA.run()");
        ArrayList<String> result = new ArrayList<>();
        HashSet<String> rels = kb.kbCache.getInstancesForType("ClosedWorldPredicate");
        System.out.println("CWAUNA.run(): rels: " + rels);
        for (String rel : rels) {
            String e = buildExclusions(kb,rel);
            if (!StringUtil.emptyString(e))
                result.add(e);
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
