package com.articulate.sigma.dataProc;

import com.articulate.sigma.HTMLformatter;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBcache;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.utils.StringUtil;

public class Diagnosis {

    /** ***************************************************************
     */
    public static String doDiagnosis(KB kb) {

        String diag = "";
        return diag;
    }

    /** ***************************************************************
     */
    public static String doTreatment(KB kb, String diag) {

        String treat = "";
        return treat;
    }

    /** ***************************************************************
     */
    public static int incTime(KB kb, String treat) {

        int inc = 0;
        return inc;
    }

    /** ***************************************************************
     */
    public static void diagnose(KB kb) {

        String diag = "";
        String treat = "";
        int time = 0;
        do {
            diag = doDiagnosis(kb);
            treat = doTreatment(kb,diag);
            time = time + incTime(kb,treat);
        } while (!StringUtil.emptyString(diag));

    }

    /** ***************************************************************
     */
    public static void printHelp() {

        System.out.println("Sigma Knowledge Engineering Environment: KBcache");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -d - perform diagnosis");
    }

    /** ***************************************************************
     */
    public static void main(String[] args) {

        if (args == null) {
            printHelp();
        }
        else {
            try {
                KBmanager.getMgr().initializeOnce();
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            System.out.println("**** Finished loading KB ***");
            if (args != null && args.length > 0 && args[0].equals("-d")) {
                diagnose(kb);
            }
            else {
                printHelp();
            }
        }
    }
}

