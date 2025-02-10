package com.articulate.sigma.trans;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.MapUtils;
import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//This software is released under the GNU Public License
//<http://www.gnu.org/copyleft/gpl.html>.
// Copyright 2019 Infosys, 2020- Articulate Software
// apease@articulatesoftware.com

public class SUMOKBtoTFAKB extends SUMOKBtoTPTPKB {

    public static String lang = "tff";

    public static boolean initialized = false;

    public static boolean debug = false;

    public static Set<String> qChildren = new HashSet<>();
    public static Set<String> iChildren = new HashSet<>();
    public static Set<String> rChildren = new HashSet<>();
    public static Set<String> lChildren = new HashSet<>();
    public static Set<String> qNotR = new HashSet<>();
    public static Set<String> qNotI = new HashSet<>();
    public static Set<String> qNotL = new HashSet<>();

    public static final String INT_SUFFIX = "In";
    public static final String REAL_SUFFIX = "Re";
    public static final String RAT_SUFFIX = "Ra";
    public static final String ENTITY_SUFFIX = "En";

    public static final String TFF_INT = "$int";
    public static final String TFF_REAL = "$real";
    public static final String TFF_RAT = "$rat";
    public static final String TFF_ENTITY = "$i";

    public Set<String> sortLabels = new HashSet<>();

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
            if (qChildren != null)
                qNotR.addAll(qChildren);
            else
                qChildren = new HashSet<>();
            if (rChildren != null)
                qNotR.removeAll(rChildren);
            else
                rChildren = new HashSet<>();
            qNotR.add("RationalNumber");
            if (qChildren != null) qNotI.addAll(qChildren);
            if (iChildren != null)
                qNotI.removeAll(iChildren);
            else
                iChildren = new HashSet<>();
            qNotI.add("Integer");
            if (qChildren != null) qNotL.addAll(qChildren);
            if (lChildren != null)
                qNotL.removeAll(lChildren);
            else
                lChildren = new HashSet<>();
            qNotL.add("RealNumber");
            String tLang = KBmanager.getMgr().getPref("TPTPlang");
            if (!StringUtil.emptyString(tLang))
                SUMOKBtoTFAKB.lang = tLang;
            SUMOtoTFAform.initOnce();
        }
        initialized = true;
    }

    /** *************************************************************
     * Test whether the given relation has an argument that is a subclass
     * of Quantity
     */
    public boolean hasNumericArg(String t) {

        List<String> sig = kb.kbCache.signatures.get(t);
        boolean result = false;
        for (String s : sig) {
            if (kb.isSubclass(s,"Quantity"))
                return true;
        }
        return result;
    }

    /** *************************************************************
     * Test whether the given relation has an argument that could be
     * a number
     */
    public boolean hasNumericSuperArg(String t) {

        List<String> sig = kb.kbCache.signatures.get(t);
        boolean result = false;
        for (String s : sig) {
            if (kb.isSubclass("RealNumber",s))
                return true;
        }
        return result;
    }

    /** *************************************************************
     * Test whether the given relation has an argument that is a subclass
     * of Quantity or the special case kludge of AssignmentFn
     */
    private boolean listOperator(String t) {

        if (t.startsWith("AssignmentFn"))
            return true;
        List<String> sig = kb.kbCache.signatures.get(t);
        boolean result = false;
        for (String s : sig) {
            if (s.equals("List"))
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
        System.err.println("Error in SUMOKBtoTFAKB.quantButNotBuiltInType(): bad type: " + type);
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
        System.out.println("Error in SUMOKBtoTFAKB.quantButNotBuiltInType(): bad type: " + type);
        return false;
    }

    /** *************************************************************
     * Test whether the term
     * one of the three TFF built-in types of $int, $rat
     * and $real or subclass
     */
    public boolean builtInOrSubType(String s) {

        if (StringUtil.emptyString(s))
            return false;
        return s.equals("RationalNumber") || s.equals("Integer") || s.equals("RealNumber") ||
                kb.isSubclass(s,"Integer") || kb.isSubclass(s,"RationalNumber") || kb.isSubclass(s,"RealNumber");
    }

    /** *************************************************************
     * Translate SUMO class names to their appropriate TFF sort
     */
    public static String translateSort(KB kb, String s) {

        if (debug) System.out.println("translateSort(): s: '" + s + "'");
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
     * Write sort for a term
     */
    public void writeSort(String t, PrintWriter pw) {

        String label;
        label = translateName(t) + "_sig";
        pw.println("% writeSort(): term: " + t);
        String bareTerm = SUMOtoTFAform.getBareTerm(t);
        pw.println("% bare term: " + bareTerm);
        if (sortLabels.contains(label)) {
            pw.println("% duplicate label " + label + " for " + t);
            return;
        }
        else {
            pw.println("% add label (writeSort) " + label);
            sortLabels.add(label);
        }
        String output;
        if (Formula.isInequality(bareTerm))
            t = translateName(t);
        if (!t.startsWith(Formula.termSymbolPrefix))
            t = Formula.termSymbolPrefix + t;
        output = "tff(" + label + ",type," + t;
        //if (Formula.isLogicalOperator(bareTerm) ||
        if (kb.isRelation(bareTerm) ||
                bareTerm.equals("equal")) {
          //  ||
        //        (kb.isRelation(bareTerm) && !Formula.isMathFunction(bareTerm))) {
//                       && !Formula.isInequality(bareTerm))) {
            pw.println("% logop: " + Formula.isLogicalOperator(bareTerm));
            pw.println("% is relation: " + kb.isRelation(bareTerm));
            pw.println("% is inequality: " + Formula.isInequality(bareTerm));
            pw.println("% is math: " + Formula.isMathFunction(bareTerm));
            if (!output.endsWith(Formula.termMentionSuffix))
                output = output + Formula.termMentionSuffix;
        }
        output = output + " : $i  ).";
        pw.println(output);
    }

    /** *************************************************************
     * Get the numerical suffix on a variable arity predicate for how
     * many arguments it has (in a particular case of use)
     */
    public int getVariableAritySuffix(String t) {

        if (t == null || t.isEmpty())
            return 0;
        int index = t.indexOf("__");
        if (index == -1)
            return 0;
        return Integer.parseInt(t.substring(index+2,index+3));
    }

    /** *************************************************************
     * Write signatures for relations
     */
    public void writeRelationSort(String t, PrintWriter pw) {

        pw.println("% SUMOKBtoTFAKB.writeRelationSort(): " + t);
        if (t.endsWith("Fn") != kb.isFunction(t))
            System.out.println("ERROR in writeRelationSort(): is function mismatch with term name : " + t + ", " + kb.isFunction(t));
        if (Formula.isLogicalOperator(t) || Formula.isMathFunction(t)  || Formula.isComparisonOperator(t)) {
            String label = translateName(t);
            String output = "tff(" + label + ",type," + label +
                    " : $i ).";
            pw.println(output);
            return;
        }
        List<String> sig = kb.kbCache.signatures.get(t);
        int endIndex = sig.size();
        if (KButilities.isVariableArity(kb,SUMOtoTFAform.withoutSuffix(t)))
            endIndex = getVariableAritySuffix(t) + 1;
        if (endIndex > 6)
            return;
        if (endIndex < 1) {
            System.err.println("Error SUMOKBtoTFAKB.writeRelationSort(): " + t + " variable arity relation without suffix");
            endIndex = sig.size();
        }
        if (endIndex > sig.size())
            endIndex = sig.size();
        if (sig == null || sig.isEmpty()) {
            pw.println("% Error in SUMOKBtoTFAKB.writeRelationSort(): no sig for " + t);
            System.err.println("Error in SUMOKBtoTFAKB.writeRelationSort(): no sig for " + t);
            pw.flush();
            Thread.dumpStack();
            return;
        }
        StringBuilder sigBuf = new StringBuilder();
        //if (kb.isFunction(t))
        //    sigBuf.append(" " + translateSort(sig.get(0)) + " *");
        for (String s : sig.subList(1,endIndex))
            sigBuf.append(" ").append(translateSort(kb,s)).append(" *");
        if (sigBuf.length() == 0) {
            pw.println("% Error in SUMOKBtoTFAKB.writeRelationSort(): " + t);
            pw.println("% Error in SUMOKBtoTFAKB.writeRelationSort(): signature: " + sig);
            String label = translateName(t);
            String output = "tff(" + label + ",type," + label +
                    " : $i ).";
            pw.println(output);
            pw.flush();
            return;
            //Thread.dumpStack();
        }
        String sigStr = sigBuf.toString().substring(0,sigBuf.length()-1);
        String relname = translateName(t);
        if (relname.endsWith(Formula.termMentionSuffix))
            relname = relname.substring(0,relname.length()-3);
        String range = sig.get(0);
        String label = translateName(t);
        if (label.endsWith(Formula.termMentionSuffix))
            label = label.substring(0,label.length()-2);
        label = label + "_sig_rel";
        if (sortLabels.contains(label)) {
            pw.println("% duplicate label " + label + " for " + relname);
            return;
        }
        else {
            pw.println("% add label (relation sort) " + label);
            sortLabels.add(label);
        }
        if (kb.isFunction(t)) {
            String output = "tff(" + label + ",type," + relname +
                    " : ( " + sigStr + " ) > " + translateSort(kb,range) + " ).";
            pw.println(output);
        }
        else {
            String output = "tff(" + label + ",type," + relname +
                    " : ( " + sigStr + " ) > $o ).";
            pw.println(output);
        }
        String output = "tff(" + label + Formula.termMentionSuffix + ",type," + relname + Formula.termMentionSuffix +
                " : $i ).";
        pw.println(output);
    }

    /** *************************************************************
     */
    public static boolean alreadyExtended(String t) {

        String patternString = "__(\\d)(In|Re|Ra|En)+";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(t);
        return matcher.find();
    }

    /** *************************************************************
     * Create all possible combinations of argument types for Integer
     * RationalNumber and RealNumber for argument types that are
     * Entity(s) (which is everything)
     * @param toExtend is a map of string relation names to the set
     *                 of suffixes that are its argument type variations
     * @param t is the relation name
     */
    private void processRelationSort(Map<String,Set<String>> toExtend, String t) {

        if (debug) System.out.println("SUMOKBtoTFAKB.processRelationSort(): t: " + t);
        int index = 1;
        if (kb.isFunction(t)) {
            index = 0;
        }
        List<String> sig = kb.kbCache.signatures.get(t);
        Map<String,Set<String>> modsig = new HashMap<>();
        String s, strnum;
        for (int i = index; i < sig.size(); i++) {
            s = sig.get(i);
            strnum = Integer.toString(i);
            if ((kb.isSubclass("RealNumber",s) && kb.isSubclass(s,"Entity")) ||
                    s.equals("RealNumber") || kb.isSubclass(s,"RealNumber")) {
                MapUtils.addToMap(modsig, strnum, strnum + REAL_SUFFIX);
                MapUtils.addToMap(modsig, strnum, strnum + INT_SUFFIX);
                MapUtils.addToMap(modsig, strnum, strnum + RAT_SUFFIX);
            }
            else if ((kb.isSubclass("RationalNumber",s) && kb.isSubclass(s,"Entity")) ||
                    s.equals("RationalNumber") || kb.isSubclass(s,"RationalNumber")) {
                MapUtils.addToMap(modsig, strnum, strnum + RAT_SUFFIX);
                MapUtils.addToMap(modsig, strnum, strnum + INT_SUFFIX);
            }
            else if ((kb.isSubclass("Integer",s) && kb.isSubclass(s,"Entity")) ||
                    s.equals("Integer") || kb.isSubclass(s,"Integer"))
                MapUtils.addToMap(modsig,strnum,strnum + INT_SUFFIX);
            MapUtils.addToMap(modsig, strnum, strnum + ENTITY_SUFFIX); // Entity (for $i) suffix
            if (listOperator(t) && s.equals("Entity")) {
                MapUtils.addToMap(modsig, strnum, strnum + INT_SUFFIX);
                MapUtils.addToMap(modsig, strnum, strnum + RAT_SUFFIX);
                MapUtils.addToMap(modsig, strnum, strnum + REAL_SUFFIX);
                MapUtils.addToMap(modsig, strnum, strnum + ENTITY_SUFFIX);
            }
        }
        Set<String> allsig = new HashSet<>();
        allsig.add("");
        Set<String> sigElem, newsig;
        for (String str : modsig.keySet()) {  // number of the argument
            sigElem = modsig.get(str);
            newsig = new HashSet<>();
            for (String res : allsig) {  // all the suffixes for previous arguments
                for (String suf : sigElem) {  // suffixes for the new argument
                    newsig.add(res + suf);
                }
            }
            allsig.clear();
            allsig.addAll(newsig);
        }
        allsig.remove("");
        if (toExtend.containsKey(t)) {
            allsig.addAll(toExtend.get(t));
        }
        toExtend.put(t,allsig);
        if (debug) System.out.println("SUMOKBtoTFAKB.processRelationSort(): t, allsig: " + t + ", " + allsig);
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
        String newRel = t + "__" + e + suffix;
        if (kb.terms.contains(newRel))
            return;
        List<String> sig = kb.kbCache.signatures.get(t);
        if (sig == null || sig.isEmpty()) {
            System.err.println("Error in SUMOKBtoTFAKB.extendRelationSig(): t: " + t);
            Thread.dumpStack();
            return;
        }
        List<String> extsig = SUMOtoTFAform.relationExtractUpdateSigFromName(newRel);
        List<String> combinedSig = new ArrayList<>();
        int sigmax = sig.size();
        if (extsig.size() > sigmax)
            sigmax = extsig.size();
        for (int i = 0; i < sigmax; i++) {
            if (i < extsig.size() && !StringUtil.emptyString(extsig.get(i)))
                SUMOtoTFAform.safeSet(combinedSig,i,extsig.get(i));
            else
                SUMOtoTFAform.safeSet(combinedSig,i,sig.get(i));
        }
        kb.kbCache.signatures.put(newRel,combinedSig);
    }

    /** *************************************************************
     * Create polymorphic comparison and math relations.
     * The result is a side effect on toExtend
     */
    private void handleMathAndComp(Map<String,Set<String>> toExtend) {

        if (debug) System.out.println("SUMOKBtoTFAKB.handleMathAndComp():");
        for (String t : Formula.COMPARISON_OPERATORS) {                 // EQUAL,GT,GTET,LT,LTET
            MapUtils.addToMap(toExtend, t, "1Re2Re");
            MapUtils.addToMap(toExtend, t, "1Ra2Ra");
            MapUtils.addToMap(toExtend, t, "1In2In");
        }
        for (String t : Formula.MATH_FUNCTIONS) {  // PLUSFN,MINUSFN,TIMESFN,DIVIDEFN,FLOORFN,CEILINGFN,ROUNDFN
            if (t.equals(Formula.FLOORFN) || t.equals(Formula.CEILINGFN) || t.equals(Formula.ROUNDFN))
                MapUtils.addToMap(toExtend, t, "0In1Re");
            else {
                MapUtils.addToMap(toExtend, t, "0Re1Re2Re");
                MapUtils.addToMap(toExtend, t, "0Ra1Ra2Ra");
                MapUtils.addToMap(toExtend, t, "0In1In2In");
            }
        }
    }

    /** *************************************************************
     */
    private boolean expandableArg(String rel, int argnum, List<String> sig) {

        String type;
        if (argnum < sig.size())
            type = sig.get(argnum);
        else
            type = kb.kbCache.variableArityType(rel);
        if (rel.equals("ListFn") && argnum > 0)
            return true;
        if (rel.equals("AssignmentFn") && argnum == 0)
            return false;
        if (argnum == 0)
            return true;
        if (listOperator(rel) && type.equals("Entity"))
            return true;
        return hasNumericArg(rel) && kb.isSubclass(type,"Quantity");
    }

    /** *************************************************************
     *  ListFn is a special special case.  Lists can hold any type and
     *  ListFn is a VariableArityPredicate, so we need to provide all
     *  permutations of TFF types in a list as signatures.
     */
    private static void handleListFn(Map<String,Set<String>> toExtend) {

        List<String> types = new ArrayList<>();
        types.add("In");
        types.add("Re");
        types.add("Ra");
        types.add("En");
        List<String> suffixes = new ArrayList<>();
        List<String> finalsuffixes = new ArrayList<>();
        String ext;
        for (String t : types) {
            ext = 1 + "Fn__0En" + 1 + t;
            suffixes.add(ext);
        }
        finalsuffixes.addAll(suffixes);
        List<String> newsuffixes;
        for (int i = 2; i <= RowVars.MAX_ARITY+1; i++) {
            newsuffixes = new ArrayList<>();
            for (String suffix : suffixes) {
                for (String t : types) {
                    ext = i + "Fn__" + suffix.substring(5) + i + t;
                    newsuffixes.add(ext);
                }
            }
            suffixes = newsuffixes;
            finalsuffixes.addAll(suffixes);
        }
        for (String suffix : finalsuffixes)
            MapUtils.addToMap(toExtend, "ListFn", suffix);
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
    private void handleVariableArity(Map<String,Set<String>> toExtend) {

        Set<String> rels = kb.kbCache.getInstancesForType("VariableArityRelation");
        List<String> sig;
        StringBuilder inStr, reStr, raStr, enStr;
        int size;
        String fnSuffix, newInStr, newReStr, newRaStr, newEnStr;
        for (String r : rels) {  // loop through all variable arity relations
            if (r.equals("ListFn"))
                continue;
            sig = kb.kbCache.getSignature(r);
            size = sig.size();
            System.out.println("SUMOKBtoTFAKB.handleVariableArity(): r,sig,size: " + r + " " + sig + " size " + size);
            if (size > 1)
                size = size - 1;  // first sig element is range, some sig elements before variable arity element may be fixed and explicit
            inStr = new StringBuilder();
            reStr = new StringBuilder();
            raStr = new StringBuilder();
            enStr = new StringBuilder();
            if (expandableArg(r,0,sig)) {
                inStr.append("0In");
                reStr.append("0Re");
                raStr.append("0Ra");
                enStr.append("0En");
            }
            fnSuffix = "";
            if (kb.isFunction(r))
                fnSuffix = "Fn";
            if (hasNumericArg(r) || listOperator(r)) {
                for (int i = size; i <= 7; i++) {
                    //if (expandableArg(r,i,sig)) {
                    inStr.append(Integer.toString(i)).append("In");
                        reStr.append(Integer.toString(i)).append("Re");
                        raStr.append(Integer.toString(i)).append("Ra");
                        enStr.append(Integer.toString(i)).append("En");
                    //}
                    newInStr = Integer.toString(i) + fnSuffix + "__" + inStr.toString();
                    newReStr = Integer.toString(i) + fnSuffix + "__" + reStr.toString();
                    newRaStr = Integer.toString(i) + fnSuffix + "__" + raStr.toString();
                    newEnStr = Integer.toString(i) + fnSuffix + "__" + enStr.toString();
                    MapUtils.addToMap(toExtend, r, newInStr);
                    MapUtils.addToMap(toExtend, r, newReStr);
                    MapUtils.addToMap(toExtend, r, newRaStr);
                    MapUtils.addToMap(toExtend, r, newEnStr);
                }
            }
            else {
                for (int i = size; i < 7; i++) {
                    MapUtils.addToMap(toExtend, r, Integer.toString(i));
                }
            }
        }
    }

    /** *************************************************************
     * Check if the relation has a numeric argument that isn't completely
     * specific, so that it needs special treatment to create versions
     * for integers, reals and rationals
     */
    public void writeSorts(PrintWriter pw) {

        Map<String, Set<String>> toExtend = new HashMap<>();
        handleMathAndComp(toExtend); // needed within processing to determine types even though they don't appear in result
        handleVariableArity(toExtend); // special case
        handleListFn(toExtend);
        String fnSuffix;
        String bareTerm;
        for (String t : kb.getTerms()) {
            bareTerm = SUMOtoTFAform.getBareTerm(t);
            pw.println("% SUMOKBtoTFAKB.writeSorts(): " + t);
            if (debug) System.out.println("SUMOKBtoTFAKB.writeSorts(): t: " + t);
            if (debug) System.out.println("SUMOKBtoTFAKB.writeSorts(): bareTerm: " + bareTerm);
            if (debug) System.out.println("kb.isRelation(t) " + kb.isRelation(t));
            if (debug) System.out.println("!alreadyExtended(t): " + !alreadyExtended(t));
            if (debug) System.out.println("!Formula.isComparisonOperator(t)): " + !Formula.isComparisonOperator(t));
            if (debug) System.out.println("!Formula.isMathFunction(t)): " + !Formula.isMathFunction(t));
            if (!Character.isLetter(t.charAt(0)) || Formula.isTrueFalse(t))
                continue;
            if (kb.isFunction(bareTerm)) {
                if (Formula.isLogicalOperator(bareTerm) || t.equals("equal") ) {
                    continue;
                }
                else {
                    writeRelationSort(t, pw);
                    if (!alreadyExtended(t))
                        processRelationSort(toExtend, t);
                }
            }
            else if (kb.isRelation(bareTerm) && !alreadyExtended(t) && !bareTerm.equals("ListFn")
                    && !Formula.isComparisonOperator(bareTerm) && !Formula.isMathFunction(bareTerm)) {
                if (hasNumericSuperArg(bareTerm) || listOperator(bareTerm)) {
                    writeRelationSort(t, pw);
                    processRelationSort(toExtend, t);
                }
                else
                    writeRelationSort(t, pw);
            }
            else
                writeSort(t,pw);
        }
        Set<String> vals;
        String sep, newTerm;
        pw.println("% SUMOKBtoTFAKB.writeSorts(): starting on toExtend sorts");
        for (String k : toExtend.keySet()) {
            bareTerm = SUMOtoTFAform.getBareTerm(k);
            vals = toExtend.get(bareTerm);
            fnSuffix = "";
            if (kb.isFunction(bareTerm) || bareTerm.endsWith("Fn"))  // variable arity functions with numerical suffixes not in kb yet
                fnSuffix = "Fn";
            for (String e : vals) {
                kb.kbCache.extendInstance(k, e + fnSuffix);
                sep = "__";
                newTerm = k + sep + e + fnSuffix;
                if (!StringUtil.emptyString(e)) {
                    extendRelationSig(k,e);
                    writeRelationSort(newTerm, pw);
                }
            }
        }
        pw.flush();
        pw.println("% SUMOKBtoTFAKB.writeSorts(): finished");
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        System.out.println("SUMOKBtoTFAKB.main():");
        SUMOKBtoTFAKB skbtfakb = new SUMOKBtoTFAKB();
        skbtfakb.initOnce();
        System.out.println("SUMOKBtoTFAKB.main(): completed init");
        SUMOformulaToTPTPformula.lang = "tff"; // this setting has to be *after* initialization, otherwise init
        // tries to write a TPTP file and then sees that tff is set and tries to write tff, but then sorts etc
        // haven't been set
        SUMOKBtoTPTPKB.lang = "tff";
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        String filename = KBmanager.getMgr().getPref("kbDir") + File.separator + kbName + "." + SUMOKBtoTPTPKB.lang;
        System.out.println("SUMOKBtoTFAKB.main(): " + skbtfakb.kb.kbCache.getSignature("ListOrderFn"));
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            skbtfakb.writeSorts(pw);
            System.out.println("---------------------------");
            System.out.println("SUMOKBtoTFAKB.main(): completed writing sorts");
            skbtfakb.writeFile(filename, null, false, pw);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
