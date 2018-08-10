package com.articulate.sigma.trans;

import com.articulate.sigma.*;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;

import java.io.*;
import java.util.*;

public class SUMOKBtoTFAKB extends SUMOKBtoTPTPKB {

    public static String lang = "tff";

    public static boolean initialized = false;

    /** *************************************************************
     */
    public static void initOnce() {

        if (!initialized)
            SUMOtoTFAform.initOnce();
        initialized = true;
    }

    /** *************************************************************
     */
    public String translateSort(String s) {

        int ttype = s.charAt(0);
        if (Character.isDigit(ttype))
            ttype = StreamTokenizer_s.TT_NUMBER;
        return SUMOformulaToTPTPformula.translateWord(s,ttype,false);
    }

    /** *************************************************************
     */
    public void writeRelationSort(String t, PrintWriter pw, String sanitizedKBName) {

        ArrayList<String> sig = kb.kbCache.signatures.get(t);
        if (sig == null || sig.size() == 0) {
            pw.println("% Error in writeSorts(): " + t);
            pw.flush();
            Thread.dumpStack();
        }
        StringBuffer sigBuf = new StringBuffer();
        for (String s : sig.subList(1,sig.size()))
            sigBuf.append(" " + translateSort(s) + " *");
        if (sigBuf.length() == 0) {
            pw.println("% Error in writeSorts(): " + t);
            pw.println("% Error in writeSorts(): signature: " + sig);
            pw.flush();
            Thread.dumpStack();
        }
        String sigStr = sigBuf.toString().substring(0,sigBuf.length()-1);
        if (kb.isFunction(t)) {
            String range = sig.get(0);
            pw.println("tff(" + StringUtil.initialLowerCase(t) + ".sig,type," + translateSort(t) + " : ( " + sigStr + " ) > " + translateSort(range) + " ).");
        }
        else
            pw.println("tff(" + StringUtil.initialLowerCase(t) + ".sig,type," + translateSort(t) + " : ( " + sigStr + " ) > $o ).");

    }

    /** *************************************************************
     */
    public void writeSorts(PrintWriter pw, String sanitizedKBName) {

        for (String t : kb.getTerms()) {
            pw.println("% writeSorts(): " + t);
            if (Formula.isLogicalOperator(t) || t.equals("equal"))
                continue;
            if (kb.isRelation(t))
                writeRelationSort(t,pw,sanitizedKBName);
            else if (!kb.isInstance(t))
                pw.println("tff(" + StringUtil.initialLowerCase(t) + "_type,type," + translateSort(t) + ": $tType ).");
            else
                pw.println("tff(" + StringUtil.initialLowerCase(t) + "_type,type," + translateSort(t) + ": $i ).");
        }
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        debug = true;
        initOnce();
        SUMOKBtoTFAKB skbtfakb = new SUMOKBtoTFAKB();
        skbtfakb.kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        String filename = KBmanager.getMgr().getPref("kbDir") + File.separator + "SUMO.tff";
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter(filename));
            skbtfakb.writeSorts(pw,filename);
            skbtfakb.writeFile(filename, null, false, "", false, pw);
            pw.flush();
            pw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}
