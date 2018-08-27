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
    public static String translateSort(String s) {

        //System.out.println("translateSort(): s: '" + s + "'");
        if (s.equals("$i") || s.equals("$tType"))
            return s;
        if (s.equals("Integer"))
            return "$int";
        if (s.equals("RealNumber"))
            return "$real";
        return "$i";
    }

    /** *************************************************************
     */
    public static String translateName(String s) {

        System.out.println("% translateName(): " + s);
        int ttype = s.charAt(0);
        if (Character.isDigit(ttype))
            ttype = StreamTokenizer_s.TT_NUMBER;
        String result = SUMOformulaToTPTPformula.translateWord(s,ttype,false);
        if (result.endsWith("+"))
            result = result.replace("+","_c");
        return result;
    }

    /** *************************************************************
     */
    public void writeRelationSort(String t, PrintWriter pw, String sanitizedKBName) {

        if (Formula.isLogicalOperator(t))
            return;
        if (Formula.isMathFunction(t))
            return;
        ArrayList<String> sig = kb.kbCache.signatures.get(t);
        if (sig == null || sig.size() == 0) {
            pw.println("% Error in writeSorts(): " + t);
            pw.flush();
            Thread.dumpStack();
        }
        StringBuffer sigBuf = new StringBuffer();
        //if (kb.isFunction(t))
        //    sigBuf.append(" " + translateSort(sig.get(0)) + " *");
        for (String s : sig.subList(1,sig.size()))
            sigBuf.append(" " + translateSort(s) + " *");
        if (sigBuf.length() == 0) {
            pw.println("% Error in writeSorts(): " + t);
            pw.println("% Error in writeSorts(): signature: " + sig);
            pw.flush();
            Thread.dumpStack();
        }
        String sigStr = sigBuf.toString().substring(0,sigBuf.length()-1);
        String relname = translateName(t);
        if (relname.endsWith("__m"))
            relname = relname.substring(0,relname.length()-3);
        if (kb.isFunction(t)) {
            String range = sig.get(0);
            pw.println("tff(" + StringUtil.initialLowerCase(t) + "_sig,type," + relname + " : ( " + sigStr + " ) > " + translateSort(range) + " ).");
        }
        else
            pw.println("tff(" + StringUtil.initialLowerCase(t) + "_sig,type," + relname + " : ( " + sigStr + " ) > $o ).");

    }

    /** *************************************************************
     */
    private void writeTermSort(PrintWriter pw, String t) {

        HashSet<String> parents = kb.immediateParents(t);
        if (parents == null || parents.size() == 0) {
            parents = new HashSet<>();
            if (!kb.isInstance(t))
                parents.add("$tType");
            else
                parents.add("$i");
        }
        for (String c : parents) {
            if (!kb.isInstance(t)) {
                pw.println("tff(" + StringUtil.initialLowerCase(t) + "_type,type," +
                        translateSort(t) + ": $tType ).");
//                        translateSort(t) + ": " + translateSort(c) + " ).");
                String cl = translateSort(c);
                if (!c.equals("$tType"))
                    cl = translateSort(c) + "_c";
                pw.println("tff(" + StringUtil.initialLowerCase(t) + "_type,type," +
                        translateSort(t) + "_c : $tType ).");
                        //translateSort(t) + "_c : " + cl + " ).");
            }
            else
                pw.println("tff(" + StringUtil.initialLowerCase(t) + "_type,type," +
                        translateSort(t) + ": $tType ).");
                        //translateSort(t) + ": " + translateSort(c) + " ).");
        }
    }

    /** *************************************************************
     */
    public void writeSorts(PrintWriter pw, String sanitizedKBName) {

        SUMOtoTFAform.setNumericFunctionInfo();
        for (String t : kb.getTerms()) {
            pw.println("% writeSorts(): " + t);
            if (Formula.isLogicalOperator(t) || t.equals("equal"))
                continue;
            if (kb.isRelation(t))
                writeRelationSort(t,pw,sanitizedKBName);
            //else
                //writeTermSort(pw,t);
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
