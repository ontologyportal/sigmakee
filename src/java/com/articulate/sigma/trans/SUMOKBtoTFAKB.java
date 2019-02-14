package com.articulate.sigma.trans;

import com.articulate.sigma.*;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SUMOKBtoTFAKB extends SUMOKBtoTPTPKB {

    public static String lang = "tff";

    public static boolean initialized = false;

    public static boolean debug = true;

    public static HashSet<String> qChildren = null;
    public static HashSet<String> iChildren = null;
    public static HashSet<String> rChildren = null;
    public static HashSet<String> lChildren = null;
    public static HashSet<String> qNotR = new HashSet<String>();
    public static HashSet<String> qNotI = new HashSet<String>();
    public static HashSet<String> qNotL = new HashSet<String>();

    public static final String INT_SUFFIX = "In";
    public static final String REAL_SUFFIX = "Re";
    public static final String RAT_SUFFIX = "Ra";

    /** *************************************************************
     */
    public void initOnce() {

        if (!initialized) {
            KBmanager.getMgr().initializeOnce();
            kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            qChildren = kb.kbCache.getChildClasses("Quantity");
            iChildren = kb.kbCache.getChildClasses("Integer");
            rChildren = kb.kbCache.getChildClasses("RationalNumber");
            lChildren = kb.kbCache.getChildClasses("RealNumber");
            qNotR.addAll(qChildren);
            qNotR.removeAll(rChildren);
            qNotR.add("RationalNumber");
            qNotI.addAll(qChildren);
            qNotI.removeAll(iChildren);
            qNotI.add("Integer");
            qNotL.addAll(qChildren);
            qNotL.removeAll(lChildren);
            qNotL.add("RealNumber");
            SUMOtoTFAform.initOnce();
        }
        initialized = true;
    }

    /** *************************************************************
     * Test whether the given relation has an argument that is a subclass
     * of Quantity
     */
    public boolean hasNumericArg(String t) {

        ArrayList<String> sig = kb.kbCache.signatures.get(t);
        boolean result = false;
        for (String s : sig) {
            if (kb.isSubclass(s,"Quantity"))
                return true;
        }
        return result;
    }

    /** *************************************************************
     * Test whether the term is a subclass of Quantity but not a
     * subclass of one of the three TFF built-in types of $int, $rat
     * and $real
     */
    public static boolean quantButNotBuiltInSubtype(String type, String s) {

        if (type.equals("RationalNumber"))
            return qNotR.contains(s);
        if (type.equals("Integer"))
            return qNotI.contains(s);
        if (type.equals("RealNumber"))
            return qNotL.contains(s);
        System.out.println("Error in quantButNotBuiltInType(): bad type: " + type);
        return false;
    }

    /** *************************************************************
     * Test whether the term is a subclass of Quantity but not a
     * one of the three TFF built-in types of $int, $rat
     * and $real or subclass
     */
    public static boolean quantButNotBuiltInType(String type, String s) {

        if (type.equals("RationalNumber"))
            return qNotR.contains(s) && !s.equals("RationalNumber");
        if (type.equals("Integer"))
            return qNotI.contains(s) && !s.equals("Integer");
        if (type.equals("RealNumber"))
            return qNotL.contains(s) && !s.equals("RealNumber");
        System.out.println("Error in quantButNotBuiltInType(): bad type: " + type);
        return false;
    }

    /** *************************************************************
     */
    public static String translateSort(KB kb, String s) {

        //System.out.println("translateSort(): s: '" + s + "'");
        if (StringUtil.emptyString(s))
            return "$i";
        if (s.equals("$i") || s.equals("$tType"))
            return s;
        if (s.equals("Integer"))
            return "$int";
        if (s.equals("RealNumber"))
            return "$real";
        if (s.equals("RationalNumber"))
            return "$rat";
        if (kb.isSubclass(s,"Integer"))
            return "$int";
        else if (kb.isSubclass(s,"RationalNumber"))
            return "$rat";
        else if (kb.isSubclass(s,"RealNumber"))
            return "$real";
        return "$i";
    }

    /** *************************************************************
     */
    public static String translateName(String s) {

        //System.out.println("% translateName(): " + s);
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

        System.out.println("writeRelationSort(): t: " + t);
        if (Formula.isLogicalOperator(t))
            return;
        if (Formula.isMathFunction(t))
            return;
        ArrayList<String> sig = kb.kbCache.signatures.get(t);
        if (debug) System.out.println("writeRelationSort(): sig: " + sig);
        if (sig == null || sig.size() == 0) {
            pw.println("% Error in writeRelationSort(): no sig for " + t);
            System.out.println("Error in writeRelationSort(): no sig for " + t);
            pw.flush();
            Thread.dumpStack();
            return;
        }
        StringBuffer sigBuf = new StringBuffer();
        //if (kb.isFunction(t))
        //    sigBuf.append(" " + translateSort(sig.get(0)) + " *");
        for (String s : sig.subList(1,sig.size()))
            sigBuf.append(" " + translateSort(kb,s) + " *");
        if (sigBuf.length() == 0) {
            pw.println("% Error in writeRelationSort(): " + t);
            pw.println("% Error in writeRelationSort(): signature: " + sig);
            pw.flush();
            return;
            //Thread.dumpStack();
        }
        String sigStr = sigBuf.toString().substring(0,sigBuf.length()-1);
        if (debug) System.out.println("writeRelationSort(): sigstr: " + sigStr);
        String relname = translateName(t);
        if (relname.endsWith("__m"))
            relname = relname.substring(0,relname.length()-3);
        if (kb.isFunction(t)) {
            String range = sig.get(0);
            pw.println("tff(" + StringUtil.initialLowerCase(t) + "_sig,type," + relname + " : ( " + sigStr + " ) > " + translateSort(kb,range) + " ).");
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
                        translateSort(kb,t) + ": $tType ).");
//                        translateSort(kb,t) + ": " + translateSort(c) + " ).");
                String cl = translateSort(kb,c);
                if (!c.equals("$tType"))
                    cl = translateSort(kb,c) + "_c";
                pw.println("tff(" + StringUtil.initialLowerCase(t) + "_type,type," +
                        translateSort(kb,t) + "_c : $tType ).");
                        //translateSort(t) + "_c : " + cl + " ).");
            }
            else
                pw.println("tff(" + StringUtil.initialLowerCase(t) + "_type,type," +
                        translateSort(kb,t) + ": $tType ).");
                        //translateSort(t) + ": " + translateSort(c) + " ).");
        }
    }

    /** *************************************************************
     */
    private static boolean alreadyExtended(String t) {

        String patternString = "__(\\d)(In|Re|Ra)+";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(t);
        if (matcher.find())
            return true;
        if (t.endsWith("__Integer") || t.endsWith("__RationalNumber") || t.endsWith("__RealNumber") ||
            t.endsWith("__IntegerFn") || t.endsWith("__RationalNumberFn") || t.endsWith("__RealNumberFn"))
            return true;
        return false;
    }

    /** *************************************************************
     * Create all possible combinations of argument types for Integer
     * RationalNumber and RealNumber for argument types that are
     * Quantity(s)
     * @param toExtend is a map of string relation names to the set
     *                 of suffixes that are its argument type variations
     * @param t is the relation name
     */
    private void processRelationSort(HashMap<String,HashSet<String>> toExtend, String t) {

        String suffix = "";
        if (kb.isFunction(t))
            suffix = "Fn";
        ArrayList<String> sig = kb.kbCache.signatures.get(t);
        if (debug) System.out.println("processRelationSort(): sig: " + sig);
        HashMap<String,HashSet<String>> modsig = new HashMap<>();
        for (int i = 0; i < sig.size(); i++) {
            String s = sig.get(i);
            String strnum = Integer.toString(i);
            if (debug) System.out.println("processRelationSort(): s,i: " + s + ", " + i);
            if ((kb.isSubclass("Integer",s) && kb.isSubclass(s,"Quantity")) ||
                    s.equals("Integer") || kb.isSubclass(s,"Integer"))
                FormulaPreprocessor.addToMap(modsig,strnum,strnum + INT_SUFFIX);
            if ((kb.isSubclass("RationalNumber",s) && kb.isSubclass(s,"Quantity")) ||
                    s.equals("RationalNumber") || kb.isSubclass(s,"RationalNumber"))
                FormulaPreprocessor.addToMap(modsig,strnum,strnum + RAT_SUFFIX);
            if ((kb.isSubclass("RealNumber",s) && kb.isSubclass(s,"Quantity")) ||
                    s.equals("RealNumber") || kb.isSubclass(s,"RealNumber"))
                FormulaPreprocessor.addToMap(modsig,strnum,strnum + REAL_SUFFIX);
        }
        if (debug) System.out.println("processRelationSort(): modsig: " + modsig);
        HashSet<String> allsig = new HashSet<>();
        allsig.add("");
        for (String s : modsig.keySet()) {  // number of the argument
            HashSet<String> sigElem = modsig.get(s);
            HashSet<String> newsig = new HashSet<>();
            for (String res : allsig) {  // all the suffixes for previous arguments
                for (String suf : sigElem) {  // suffixes for the new argument
                    newsig.add(res + suf);
                }
            }
            allsig.clear();
            allsig.addAll(newsig);
        }
        allsig.remove("");
        if (debug) System.out.println("processRelationSort(): adding " + t + " : " + allsig);
        if (toExtend.containsKey(t)) {
            allsig.addAll(toExtend.get(t));
        }
        toExtend.put(t,allsig);
    }

    /** *************************************************************
     * Copy an existing signature overwriting any argument types
     * as specified by the new suffix
     * @param e is the type suffix of a new relation built on the old one
     * @param t is the relation name
     */
    private void extendRelationSig(String t, String e) {

        String suffix = "";
        if (kb.isFunction(t))
            suffix = "Fn";
        if (debug) System.out.println("extendRelationSig(): t,e: " + t + ", " + e);
        ArrayList<String> sig = kb.kbCache.signatures.get(t);
        if (debug) System.out.println("extendRelationSig(): sig: " + sig);
        if (sig == null || sig.size() == 0) {
            System.out.println("Error in extendRelationSig(): t: " + t);
            Thread.dumpStack();
            return;
        }
        String newRel = t + "__" + e + suffix;
        System.out.println("extendRelationSig(): newrel: " + newRel);
        ArrayList<String> extsig = SUMOtoTFAform.relationExtractUpdateSig(newRel);
        ArrayList<String> combinedSig = new ArrayList<>();
        for (int i = 0; i < sig.size(); i++) {
            if (extsig != null && i < extsig.size() && !StringUtil.emptyString(extsig.get(i)))
                SUMOtoTFAform.safeSet(combinedSig,i,extsig.get(i));
            else
                SUMOtoTFAform.safeSet(combinedSig,i,sig.get(i));
        }
        if (debug) System.out.println("extendRelationSig(): combined sig: " +
                combinedSig);
        kb.kbCache.signatures.put(newRel,combinedSig);
    }

    /** *************************************************************
     * Create polymorphic comparison and math relations.
     * The result is a side effect on toExtend
     */
    private void handleMathAndComp(HashMap<String,HashSet<String>> toExtend) {

        for (String t : Formula.COMPARISON_OPERATORS) {
            FormulaPreprocessor.addToMap(toExtend, t, "1Re2Re");
            FormulaPreprocessor.addToMap(toExtend, t, "1Ra2Ra");
            FormulaPreprocessor.addToMap(toExtend, t, "1In2In");
        }
        for (String t : Formula.MATH_FUNCTIONS) {
            FormulaPreprocessor.addToMap(toExtend, t, "0Re1Re2Re");
            FormulaPreprocessor.addToMap(toExtend, t, "0Ra1Ra2Ra");
            FormulaPreprocessor.addToMap(toExtend, t, "0In1In2In");
        }
    }

    /** *************************************************************
     * VariableArityRelations are special cases since the different
     * versions only get expanded when axioms are processed but
     * we need to create the different sorts up front.  The current
     * set in SUMO as of 1/2019 are AssignmentFn, GreatestCommonDivisorFn,
     * LatitudeFn, LeastCommonMultipleFn, ListFn, LongitudeFn, contraryAttribute,
     * disjointDecomposition, exhaustiveAttribute, exhaustiveDecomposition,
     * partition and processList.  ListFn is a special special case
     */
    private void handleVariableArity(HashMap<String,HashSet<String>> toExtend) {

        if (debug) System.out.println("handleVariableArity():");
        HashSet<String> rels = kb.kbCache.getInstancesForType("VariableArityRelation");

        for (String r : rels) {
            StringBuffer inStr = new StringBuffer();
            StringBuffer reStr = new StringBuffer();
            StringBuffer raStr = new StringBuffer();
            if (debug) System.out.println("handleVariableArity(): r: " + r);
            if (hasNumericArg(r) || r.equals("ListFn")) {
                if (debug) System.out.println("handleVariableArity(): numeric or list r: " + r);
                for (int i = 1; i < 7; i++) {
                    inStr.append(Integer.toString(i) + "In");
                    reStr.append(Integer.toString(i) + "Re");
                    raStr.append(Integer.toString(i) + "Ra");
                    String newInStr = Integer.toString(i) + "__" + inStr.toString();
                    String newReStr = Integer.toString(i) + "__" + reStr.toString();
                    String newRaStr = Integer.toString(i) + "__" + raStr.toString();
                    if (debug) System.out.println("handleVariableArity(): adding term: " + newInStr);
                    FormulaPreprocessor.addToMap(toExtend, r, newInStr);
                    FormulaPreprocessor.addToMap(toExtend, r, newReStr);
                    FormulaPreprocessor.addToMap(toExtend, r, newRaStr);
                }
            }
            else {
                if (debug) System.out.println("handleVariableArity(): non numeric and non-list r: " + r);
                for (int i = 1; i < 7; i++) {
                    FormulaPreprocessor.addToMap(toExtend, r, Integer.toString(i));
                }
            }
        }
        if (debug) System.out.println("handleVariableArity(): toExtend: " + toExtend);
    }

    /** *************************************************************
     * Check if the relation has a numeric argument that isn't completely
     * specific, so that it needs special treatment to create versions
     * for integers, reals and rationals
     */
    public void writeSorts(PrintWriter pw, String sanitizedKBName) {

        SUMOtoTFAform.setNumericFunctionInfo();
        HashMap<String,HashSet<String>> toExtend = new HashMap<>();
        handleMathAndComp(toExtend);
        handleVariableArity(toExtend); // special case
        for (String t : kb.getTerms()) {
            pw.println("% writeSorts(): " + t);
            if (debug) System.out.println("writeSorts(): t: " + t);
            String fnSuffix = "";
            if (kb.isFunction(t))
                fnSuffix = "Fn";
            if (Formula.isLogicalOperator(t) || t.equals("equal"))
                continue;
            if (kb.isRelation(t) && !alreadyExtended(t) &&
                !(Formula.isComparisonOperator(t) || Formula.isMathFunction(t))) {
                if (hasNumericArg(t)) {
                    writeRelationSort(t,pw,sanitizedKBName);
                    processRelationSort(toExtend, t);
                }
                else
                    writeRelationSort(t,pw,sanitizedKBName);
            }
        }
        for (String k : toExtend.keySet()) {
            if (debug) System.out.println("writeSorts(): k: " + k);
            HashSet<String> vals = toExtend.get(k);
            String fnSuffix = "";
            if (kb.isFunction(k))
                fnSuffix = "Fn";
            for (String e : vals) {
                kb.kbCache.extendInstance(k, e + fnSuffix);
                String sep = "__";
                if (e.matches("\\d__.*"))  // variable arity has appended single underscore before arity
                    sep = "_";
                String newTerm = k + sep + e + fnSuffix;
                if (debug) System.out.println("writeSorts(): newTerm: " + newTerm);
                if (!StringUtil.emptyString(e)) {
                    extendRelationSig(k,e);
                    writeRelationSort(newTerm, pw, sanitizedKBName);
                }
            }
        }
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        debug = true;
        SUMOKBtoTFAKB skbtfakb = new SUMOKBtoTFAKB();
        skbtfakb.initOnce();
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
