/** This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the terms of the GNU
license.  This software is released under the GNU Public License
<http://www.gnu.org/copyleft/gpl.html>.  Users of this code also consent,
by use of this code, to credit Articulate Software and Teknowledge in any
writings, briefings, publications, presentations, or other representations
of any software which incorporates, builds on, or uses this code.  Please
cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. see also
http://sigmakee.sourceforge.net

Note that this class, and therefore, Sigma, depends upon several terms
being present in the ontology in order to function as intended.  They are:
  and or forall exists
  domain
  EnglishLanguage
  equal
  format
  instance
  inverse
  Predicate
  Relation
  Class
  subclass
  subrelation
  termFormat
  valence
  VariableArityRelation
*/

/*************************************************************************************************/
package com.articulate.sigma;

/*
Author: Adam Pease apease@articulatesoftware.com

some portions copyright Infosys, Teknowledge, IPsoft

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA

*/

import com.articulate.sigma.tp.Vampire;
import com.articulate.sigma.tp.EProver;
import com.articulate.sigma.tp.LEO;
import com.articulate.sigma.trans.*;
import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.Pair;
import com.articulate.sigma.utils.SetUtil;
import com.articulate.sigma.utils.StringUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import tptp_parser.TPTPFormula;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * ***************************************************************** Contains
 * methods for reading, writing knowledge bases and their configurations. Also
 * contains the inference engine process for the knowledge base.
 */
public class KB implements Serializable {

    private boolean isVisible = true;

    /** Eprover inference engine process for this KB. */
    public transient EProver eprover;

    /** LEO-III inference engine process for this KB. */
    public transient LEO leo;

    /** The name of the knowledge base. */
    public String name;

    /* An ArrayList of Strings that are the full canonical pathnames of the
     * files that comprise the KB.    */
    public List<String> constituents = new ArrayList<>();

    /** The natural language in which axiom paraphrases should be presented. */
    public String language = "EnglishLanguage";

    /* The location of preprocessed KIF files, suitable for loading into
     * EProver.     */
    public String kbDir = null;

    /** The instance of the CELT process. */
    public transient CELT celt = null;

    /** a cache built through lazy evaluation of the taxonomic depth of each term */
    public Map<String,Integer> termDepthCache = new HashMap<>();

    /* A SortedSet of Strings, which are all the terms in the KB.     */
    public Set<String> terms = new TreeSet<>();

    // A Map from all uppercase terms to their possibly mixed case original versions
    public Map<String,String> capterms = new HashMap<>();

    /** The String constant that is the suffix for file of user assertions. */
    public static final String _userAssertionsString = "_UserAssertions.kif";

    /** The String constant that is the suffix for TPTP file of user assertions. */
    public static final String _userAssertionsTPTP = "_UserAssertions.tptp";

    /** The String constant that is the suffix for TFF file of user assertions. */
    public static final String _userAssertionsTFF = "_UserAssertions.tff";

    /** The String constant that is the suffix for THF file of user assertions. */
    public static final String _userAssertionsTHF = "_UserAssertions.thf";

    /* The String constant that is the suffix for files of cached assertions.     */
    public static final String _cacheFileSuffix = "_Cache.kif";

    /* A Map of all the Formula objects in the KB. Each key is a String
     * representation of a Formula. Each value is the Formula object
     * corresponding to the key.     */
    public Map<String, Formula> formulaMap = new HashMap<>();

    /* A HashMap of ArrayLists of String formulae, containing all the formulae
     * in the KB. Keys are the formula itself, a formula ID, and term indexes
     * created in KIF.createKey(). The actual formula can be retrieved by using
     * the returned String as the key for the variable formulaMap     */
    public Map<String, List<String>> formulas = new HashMap<>();

    /* The natural language formatting strings for relations in the KB. It is a
     * HashMap of language keys and HashMap values. The interior HashMap is term
     * name keys and String values.     */
    private Map<String, Map<String, String>> formatMap = new HashMap<>();

    /* language keys and HashMap values. The interior HashMap is term name keys
     * and String values.     */
    private Map<String, Map<String, String>> termFormatMap = new HashMap<>();

    /** Errors found during loading of the KB constituents. */
    public Set<String> errors = new TreeSet<>();

    /** Warnings found during loading of the KB constituents. */
    public Set<String> warnings = new TreeSet<>();

    /* Future: If true, the contents of the KB have been modified without
     * updating the caches     */
    public boolean modifiedContents = false;

    /* If true, assertions of the form (predicate x x) will be included in the
     * relation cache tables.     */
//    private boolean cacheReflexiveAssertions = false;

    public KBcache kbCache = null;

    // maps TPTP axiom IDs to SUMO formulas
    public static Map<String,Formula> axiomKey = new HashMap<>();

    public Map<String, Integer> termFrequency = new HashMap<>();

    // force regeneration of TPTP file
    public static boolean force = false;

    public static boolean debug = false;

    /***************************************************************
     */
    public KB() {
    }

    /***************************************************************
     * Constructor which takes the name of the KB and the location where KBs preprocessed
     * for EProver should be placed.
     */
    public KB(String n, String dir) {

        name = n;
        kbDir = dir;
        try {
            KBmanager mgr = KBmanager.getMgr();
            if (mgr != null) {
                String loadCelt = mgr.getPref("loadCELT");
                if ((loadCelt != null) && loadCelt.equalsIgnoreCase("yes")) {
                    celt = new CELT();
                }
            }
        } catch (IOException ioe) {
            System.err.println("Error in KB(): " + ioe.getMessage());
            celt = null;
        }
    }

    /***************************************************************
     */
    public KB(String n, String dir, boolean visibility) {

        this(n, dir);
        isVisible = visibility;
    }

    /***************************************************************
     * Perform a deep copy of the kb input
     *
     * @param kbIn
     * @throws IOException
     */
    public KB(KB kbIn) throws IOException {

        this.isVisible = kbIn.isVisible;
        if (kbIn.eprover != null)
            this.eprover = kbIn.eprover;
        this.name = kbIn.name;
        if (kbIn.constituents != null)
            this.constituents = Lists.newArrayList(kbIn.constituents);
        this.language = kbIn.language;
        this.kbDir = kbIn.kbDir;

        if (kbIn.terms != null)
            this.terms = Collections.synchronizedSortedSet(new TreeSet<>(kbIn.terms));
        if (kbIn.capterms != null)
            this.capterms.putAll(kbIn.capterms);
        String key;
        Formula newFormula;
        if (kbIn.formulaMap != null) {
            for (Map.Entry<String, Formula> pair : kbIn.formulaMap.entrySet()) {
                key = pair.getKey();
                newFormula = new Formula(pair.getValue());
                this.formulaMap.put(key, newFormula);
            }
        }

        if (kbIn.formulas != null) {
            List<String> newList;
            for (Map.Entry<String, List<String>> pair : kbIn.formulas.entrySet()) {
                key = pair.getKey();
                newList = Lists.newArrayList(pair.getValue());
                this.formulas.put(key, newList);
            }
        }

        if (kbIn.formatMap != null)
            this.formatMap = Maps.newHashMap(kbIn.formatMap);
        if (kbIn.termFormatMap != null)
            this.termFormatMap = Maps.newHashMap(kbIn.termFormatMap);
        if (kbIn.errors != null)
            this.errors = Sets.newTreeSet(kbIn.errors);
        this.modifiedContents = kbIn.modifiedContents;
        this.kbCache = new KBcache(kbIn.kbCache, this);

        // Must be done after kb manager set.
        if (kbIn.celt != null)
            this.celt = new CELT();
    }

    /***************************************************************
     */
    public boolean isVisible() {
        return isVisible;
    }

    /***************************************************************
     * Constructor
     */
    public KB(String n) {

        this(n, KBmanager.getMgr().getPref("kbDir"));
    }

    /**************************************************************
     * Returns a SortedSet of Strings, which are all the terms in the KB.
     */
    public Set<String> getTerms() {

        return this.terms;
    }


    /***************************************************
     * Only called in
     * BrowseBody.jsp when a single match is found. Purpose is to simplify a
     * RegEx to its only matching term
     *
     * @param term
     *            a String
     * @return modified term a String
     */
    public String simplifyTerm(String term, boolean ignoreCaps) {

        if (getREMatch(term.intern(),ignoreCaps).size() == 1)
            return getREMatch(term.intern(),ignoreCaps).get(0);
        return term;
    }

    /****************************************************
     * Takes a term
     * (interpreted as a Regular Expression) and returns true if any term in the
     * KB has a match with the RE.
     *
     * @param term
     *            A String
     * @return true or false.
     */
    public boolean containsRE(String term, boolean ignoreCaps) {

        return !getREMatch(term,ignoreCaps).isEmpty();
    }

    /****************************************************
     * Takes a term
     * (interpreted as a Regular Expression) and returns an ArrayList containing
     * every term in the KB that has a match with the RE.
     *
     * @param term
     *            A String
     * @return An ArrayList of terms that have a match to term
     */
    public List<String> getREMatch(String term, boolean ignoreCaps) {

        try {
            Pattern p;
            if (ignoreCaps)
                p = Pattern.compile(term, Pattern.CASE_INSENSITIVE);
            else
                p = Pattern.compile(term);
            List<String> matchesList = new ArrayList<>();
            Matcher m;
            for (String t : getTerms()) {
                m = p.matcher(t);
                if (m.matches())
                    matchesList.add(t);
            }
            return matchesList;
        }
        catch (PatternSyntaxException ex) {
            List<String> err = new ArrayList<>();
            err.add("Invalid Input");
            return err;
        }
    }

    /**************************************************************
     * Sets the synchronized SortedSet of all the terms in the KB to be kbTerms.
     */
    public void setTerms(Set<String> newTerms) {

        getTerms().clear();
        this.terms = Collections.synchronizedSortedSet(new TreeSet<>(newTerms));
        capterms.clear();
        for (String t : terms)
            capterms.put(t.toUpperCase(),t);
    }

    /***************************************************************
     * Get an ArrayList of Strings containing the language identifiers of available
     * natural language formatting templates.
     *
     * @return an ArrayList of Strings containing the language identifiers
     */
    public List<String> availableLanguages() {

        List<String> al = new ArrayList<>();
        List<Formula> col = ask("arg", 0, "format");
        List<Formula> col2 = ask("arg", 0, "termFormat");
        if (col != null) {
            if (col2 != null)
                col.addAll(col2);

            Formula f;
            String lang;
            for (int i = 0; i < col.size(); i++) {
                f = (Formula) col.get(i);
                lang = f.getStringArgument(1);
                if (!al.contains(lang.intern()))
                    al.add(lang.intern());
            }
        }
        //al.addAll(OMWordnet.lnames);
        return al;
    }

    /***************************************************************
     * Remove from the given set any item which is a superclass of another item in the set.
     */
    public Set<String> removeSuperClasses(Set<String> set) {

        Set<String> returnSet = Sets.newHashSet(set);
        Set<String> removeSet = Sets.newHashSet();

        // Compare every element to every other.
        for (String first : returnSet) {
            for (String second : returnSet) {
                if (isSubclass(first, second) && !first.equals(second)) {
                    removeSet.add(second);
                }
            }
        }
        returnSet.removeAll(removeSet);
        return returnSet;
    }

    private int counter = 0;

    /***************************************************************
     * Arity errors should already have been trapped in addConstituent() unless a
     * relation is used before it is defined. This routine is a comprehensive
     * re-check.
     */
    public void checkArity() {

        long millis = System.currentTimeMillis();
        System.out.print("INFO in KB.checkArity(): Performing Arity Check");
        if (formulaMap != null && !formulaMap.isEmpty()) {
            Future<?> future;
            List<Future<?>> futures = new ArrayList<>();
            int total = formulaMap.values().size();
            for (Formula f : formulaMap.values()) {
                Runnable r = () -> {
                    if (counter++ % 10 == 0)
                        System.out.print(".");
                    if (counter % 400 == 0)
                        System.out.printf("%nINFO in KB.checkArity(): Still performing Arity Check. %d%% done%n", counter*100/total);
                    String term = PredVarInst.hasCorrectArity(f, this);
                    if (!StringUtil.emptyString(term)) {
                        errors.add("Formula in " + f.sourceFile + " rejected due to arity error of predicate " + term
                                + " in formula: \n" + f.getFormula());
                    }
                };
                future = KButilities.EXECUTOR_SERVICE.submit(r);
                futures.add(future);
            }
            for (Future<?> f : futures)
                try {
                    f.get(); // waits for task completion
                } catch (InterruptedException | ExecutionException ex) {
                    System.err.printf("Error in KB.checkArity(): %s", ex);
                } finally {
                    counter = 0; // reset
                }
            System.out.println();
        }
        System.out.println("KB.checkArity(): seconds: " + (System.currentTimeMillis() - millis) / 1000);
    }

    /***************************************************************
     * Returns the
     * type (SUO-KIF Class name) for any argument in argPos position of an
     * assertion formed with the SUO-KIF Relation reln. If no argument type
     * value is directly stated for reln, this method tries to find a value
     * inherited from one of reln's super-relations.
     *
     * @param reln   A String denoting a SUO-KIF Relation
     * @param argPos An int denoting an argument position, where 0 is the position
     *               of reln itself
     * @return A String denoting a SUO-KIF Class, or null if no value can
     * be obtained
     */
    public String getArgType(String reln, int argPos) {

        String className = null;
        String argType = FormulaPreprocessor.findType(argPos, reln, this);
        if (StringUtil.isNonEmptyString(argType)) {
            if (argType.endsWith("+"))
                argType = "Class";
            className = argType;
        }
        return className;
    }

    /***************************************************************
     * Returns the
     * type (SUO-KIF Class name) for any argument in argPos position of an
     * assertion formed with the SUO-KIF Relation reln. If no argument type
     * value is directly stated for reln, this method tries to find a value
     * inherited from one of reln's super-relations.
     *
     * @param reln   A String denoting a SUO-KIF Relation
     * @param argPos An int denoting an argument position, where 0 is the position
     *               of reln itself
     * @return A String denoting a SUO-KIF Class, or null if no value can
     * be obtained. A '+' is appended to the class name if the argument
     * is a subclass of the class, rather than an instance
     */
    public String getArgTypeClass(String reln, int argPos) {

        String className = null;
        String argType = FormulaPreprocessor.findType(argPos, reln, this);
        if (StringUtil.isNonEmptyString(argType))
            className = argType;
        return className;
    }

    /***************************************************************
     * Determine whether a particular term is an immediate instance, which has a statement
     * of the form (instance term otherTerm). Note that this does not count for
     * terms such as Attribute(s) and Relation(s), which may be defined as
     * subAttribute(s) or subrelation(s) of another instance. If the term is not
     * an instance, return an empty ArrayList. Otherwise, return an ArrayList of
     * the Formula(s) in which the given term is defined as an instance.
     * Note! This does not return instances of the given term, but rather the
     * terms of which the given term is an instance.
     */
    public List<Formula> instancesOf(String term) {

        return askWithRestriction(1, term, 0, "instance");
    }

    /***************************************************************
     * Get all instances of a given term
     */
    public Set<String> instances(String term) {

        Set<String> result = new HashSet<>();
        List<Formula> forms = askWithRestriction(2, term, 0, "instance");
        for (Formula f : forms) {
            result.add(f.getStringArgument(1));
        }
        return result;
    }

    /***************************************************************
     * Returns
     * true if i is an instance of c, else returns false.
     *
     * @param i A String denoting an instance.
     * @param c A String denoting a Class.
     * @return true or false.
     */
    public boolean isInstanceOf(String i, String c) {

        if (kbCache == null)
            return false;
        return kbCache.isInstanceOf(i, c);
    }

    /***************************************************************
     * Returns
     * true if i is an Attribute, else returns false.
     *
     * @param i A String denoting an possible instance of Attribute.
     * @return true or false.
     */
    public boolean isAttribute(String i) {

        if (kbCache == null)
            return false;
        return kbCache.isInstanceOf(i, "Attribute");
    }

    /***************************************************************
     * Returns
     * true if i is c, is an instance of c, or is subclass of c, or is
     * subAttribute of c, else returns false. Note that every class is
     * a child of itself
     *
     * @param i A String denoting a class or instance.
     * @param c A String denoting the parent Class.
     * @return true or false.
     */
    public boolean isChildOf(String i, String c) {

        return i.equals(c) || isInstanceOf(i, c) || isSubclass(i, c) || isSubAttribute(i, c);
    }

    /***************************************************************
     * Returns true if i is an instance of Function, else returns false.
     *
     * @param i A String denoting a constant.
     * @return true or false.
     */
    public boolean isFunction(String i) {

        if (kbCache != null && !StringUtil.emptyString(i)) {
            return kbCache.functions.contains(i);
        }
        return false;
    }

    /***************************************************************
     * Returns true if argument is functional expression, else returns
     * false.
     *
     * @param form is a possibly functional literal.
     * @return true or false.
     */
    public boolean isFunctional(Formula form) {

        if (form == null || form.empty()) {
            if (debug) System.out.println("Warning - KB.isFunctional(): empty");
            return false;
        }
        if (!form.listP()) {
            if (debug) System.out.println("Warning - KB.isFunctional(): not a list: " + form);
            //Thread.dumpStack();
            return false;
        }
        String pred = form.car();
        if (debug) System.out.println("KB.isFunctional(): pred: " + pred);
        if (debug) System.out.println("KB.isFunctional(): isFunction: " + isFunction(pred));
        if (Formula.isVariable(pred)) {
            Set<String> varTypes = form.getVarType(this,pred);
            if (varTypes != null) {
                for (String s : varTypes) {
                    if (debug) System.out.println("KB.isFunctional(): s: " + s);
                    if (debug) System.out.println("KB.isFunctional(): kbCache.subclassOf(s, \"Function\") " +
                            kbCache.subclassOf(s, "Function"));
                    if (s.equals("Function") || kbCache.subclassOf(s, "Function")) {
                        if (debug) System.out.println("KB.isFunctional(): returning true");
                        return true;
                    }
                }
            }
        }
        return isFunction(pred);
    }

    /***************************************************************
     * @param i A String denoting an instance.
     * @return true or false.
     */
    public boolean isRelation(String i) {

        //System.out.println("KB.isRelation(): term: " + i);
        //System.out.println("KB.isRelation(): kbCache != null: " + kbCache != null);
        //System.out.println("KB.isRelation(): kbCache .relations != nullL " + kbCache.relations != null);
        //System.out.println("KB.isRelation(): kbCache.relations.contains(i): " + kbCache.relations.contains(i));
        return kbCache != null && kbCache.relations != null && kbCache.relations.contains(i);
    }

    /***************************************************************
     * Returns
     * true if i is an instance of c in any loaded KB, else returns false.
     *
     * @param i A String denoting an instance.
     * @return true or false.
     */
    public static boolean isRelationInAnyKB(String i) {

        Map<String, KB> kbs = KBmanager.getMgr().kbs;
        if (!kbs.isEmpty()) {
            KB kb;
            Iterator<KB> it = kbs.values().iterator();
            while (it.hasNext()) {
                kb = it.next();
                if (kb.kbCache != null && kb.kbCache.relations != null && kb.kbCache.relations.contains(i))
                    return true;
            }
        }
        return false;
    }

    /***************************************************************
     */
    public boolean isInstance(String term) {

        List<Formula> al = askWithRestriction(0, "instance", 1, term);
        return (al != null && !al.isEmpty());
    }

    /***************************************************************
     * Determine
     * whether a particular class or instance "child" is a child of the given
     * "parent".
     *
     * @param child  A String, the name of a term.
     * @param parent A String, the name of a term.
     * @return true if child and parent constitute an actual or implied relation
     * in the current KB, else false.
     */
    public boolean childOf(String child, String parent) {

        if (child.equals(parent))
            return true;
        if (kbCache.transInstOf(child, parent))
            return true;
        return kbCache.childOfP("instance", parent, child) || kbCache.childOfP("subclass", parent, child)
                || kbCache.childOfP("subrelation", parent, child) || kbCache.childOfP("subAttribute", parent, child);
    }

    /***************************************************************
     * Returns
     * true if the subclass cache supports the conclusion that c1 is a subclass
     * of c2, else returns false.  Note that classes are also subclasses of
     * themselves
     *
     * @param c1 A String, the name of a Class.
     * @param parent A String, the name of a Class.
     * @return boolean
     */
    public boolean isSubclass(String c1, String parent) {

        if (StringUtil.emptyString(c1)) {
            //System.out.println("Error in KB.isSubclass(): empty c1");
            //Thread.dumpStack();
            return false;
        }
        if (StringUtil.emptyString(parent)) {
            //System.err.println("Error in KB.isSubclass(): empty parent");
            //Thread.dumpStack();
            return false;
        }
        if (c1.equals(parent))
            return true;
        if (StringUtil.isNonEmptyString(c1) && StringUtil.isNonEmptyString(parent))
            return kbCache.childOfP("subclass", parent, c1);
        return false;
    }

    /***************************************************************
     * Returns
     * true if the KB cache supports the conclusion that c1 is a subAttribute of
     * c2, else returns false.
     *
     * @param c1 A String, the name of a SetOrClass.
     * @param parent A String, the name of a SetOrClass.
     * @return boolean
     */
    public boolean isSubAttribute(String c1, String parent) {

        if (StringUtil.isNonEmptyString(c1) && StringUtil.isNonEmptyString(parent)) {
            return kbCache.childOfP("subAttribute", parent, c1);
        }
        return false;
    }

    /***************************************************************
     * Converts
     * all Formula objects in the input List to ArrayList tuples.
     *
     * @param formulaList A list of Formulas.
     * @return An ArrayList of formula tuples (ArrayLists), or an empty
     * ArrayList.
     */
    public static List<List<String>> formulasToArrayLists(List<Formula> formulaList) {

        List<List<String>> ans = new ArrayList<>();
        if (formulaList instanceof List) {
            Iterator<Formula> it = formulaList.iterator();
            Formula f;
            while (it.hasNext()) {
                f = (Formula) it.next();
                ans.add(f.literalToArrayList());
            }
        }
        return ans;
    }

    /* *************************************************************
     * TODO: Not used and causes issues with FormulaProcessorTest 2/6/25 tdn
     * Converts
     * all Strings in the input List to Formula objects.
     *
     * @param strings A list of Strings.
     * @return An ArrayList of Formulas, or an empty ArrayList.
     */
//    public static List<Formula> stringsToFormulas(List<String> strings) {
//
//        List<Formula> ans = new ArrayList<>();
//        if (strings instanceof List) {
//            Iterator<String> it = strings.iterator();
//            Formula f;
//            while (it.hasNext()) {
//                f = new Formula();
//                f.read(it.next());
//                ans.add(f);
//            }
//        }
//        return ans;
//    }

    /***************************************************************
     * Converts a
     * literal (List object) to a String.
     *
     * @param literal A List representing a SUO-KIF formula.
     * @return A String representing a SUO-KIF formula.
     */
    public static String literalListToString(List<String> literal) {

        StringBuilder b = new StringBuilder();
        if (literal instanceof List) {
            b.append("(");
            for (int i = 0; i < literal.size(); i++) {
                if (i > 0)
                    b.append(" ");
                b.append(literal.get(i));
            }
            b.append(")");
        }
        return b.toString();
    }

    /***************************************************************
     * Converts a
     * literal (List object) to a Formula.
     *
     * @param lit A List representing a SUO-KIF formula.
     * @return A SUO-KIF Formula object, or null if no Formula can be created.
     */
    public static Formula literalListToFormula(List<String> lit) {

        Formula f = null;
        String theFormula = literalListToString(lit);
        if (StringUtil.isNonEmptyString(theFormula)) {
            f = new Formula();
            f.read(theFormula);
        }
        return f;
    }

    /***************************************************************
     * Returns an
     * ArrayList containing the terms (Strings) that correspond to targetArgnum
     * in the Formulas obtained from the method call askWithRestriction(argnum1,
     * term1, argnum2, term2).
     *
     * @param predicatesUsed A Set to which will be added the predicates of the ground
     *                       assertions actually used to gather the terms returned
     * @return An ArrayList of terms, or an empty ArrayList if no terms can be
     * retrieved.
     */
    public List<String> getTermsViaAskWithRestriction(int argnum1, String term1, int argnum2, String term2,
                                                           int targetArgnum, Set<String> predicatesUsed) {

        List<String> result = new ArrayList<>();
        if (StringUtil.isNonEmptyString(term1) && !StringUtil.isQuotedString(term1)
                && StringUtil.isNonEmptyString(term2) && !StringUtil.isQuotedString(term2)) {
            List<Formula> formulae = askWithRestriction(argnum1, term1, argnum2, term2);
            Formula f;
            Iterator<Formula> it = formulae.iterator();
            while (it.hasNext()) {
                f = it.next();
                result.add(f.getStringArgument(targetArgnum));
            }
            if (predicatesUsed instanceof Set) {
                Iterator<Formula> it2 = formulae.iterator();
                while (it2.hasNext()) {
                    f = (Formula) it2.next();
                    predicatesUsed.add(f.car());
                }
            }
        }
        return result;
    }

    /***************************************************************
     * Returns an
     * ArrayList containing the terms (Strings) that correspond to targetArgnum
     * in the Formulas obtained from the method call askWithRestriction(argnum1,
     * term1, argnum2, term2).
     *
     * @return An ArrayList of terms, or an empty ArrayList if no terms can be
     * retrieved.
     */
    public List<String> getTermsViaAskWithRestriction(int argnum1, String term1, int argnum2, String term2,
                                                           int targetArgnum) {

        return getTermsViaAskWithRestriction(argnum1, term1, argnum2, term2, targetArgnum, null);
    }

    /***************************************************************
     * Returns the
     * first term found that corresponds to targetArgnum in the Formulas
     * obtained from the method call askWithRestriction(argnum1, term1, argnum2,
     * term2).
     *
     * @return A SUO-KIF term (String), or null is no answer can be retrieved.
     */
    public String getFirstTermViaAskWithRestriction(int argnum1, String term1, int argnum2, String term2,
                                                    int targetArgnum) {

        String result = null;
        List<String> terms = getTermsViaAskWithRestriction(argnum1, term1, argnum2, term2, targetArgnum);
        if (!terms.isEmpty())
            result = (String) terms.get(0);
        return result;
    }

    /***************************************************************
     * @return an ArrayList of Formulas in which the two terms provided appear
     * in the indicated argument positions. If there are no Formula(s)
     * matching the given terms and respective argument positions,
     * return an empty ArrayList. Iterate through the smallest list of
     * results.
     */
    public List<Formula> askWithRestriction(int argnum1, String term1, int argnum2, String term2) {

        //System.out.println("INFO in KB.askWithRestriction(): argnum1: " + argnum1);
        //System.out.println("INFO in KB.askWithRestriction(): term1: " + term1);
        //System.out.println("INFO in KB.askWithRestriction(): argnum2: " + argnum2);
        //System.out.println("INFO in KB.askWithRestriction(): term2: " + term2);
        List<Formula> result = new ArrayList<>();
        if (StringUtil.isNonEmptyString(term1) && StringUtil.isNonEmptyString(term2)) {
            List<Formula> partial1 = ask("arg", argnum1, term1);
            List<Formula> partial2 = ask("arg", argnum2, term2);
            //System.out.println("INFO in KB.askWithRestriction(): partial 1: " + partial1);
            //System.out.println("INFO in KB.askWithRestriction(): partial 2: " + partial2);
            List<Formula> partial = partial1;
            // partial.retainAll(partial2); - this should be faster than below
            int arg = argnum2;
            String term = term2;
            if (partial1.size() > partial2.size()) {
                partial = partial2;
                arg = argnum1;
                term = term1;
            }
            //System.out.println("INFO in KB.askWithRestriction(): partial: " + partial);
            //System.out.println("INFO in KB.askWithRestriction(): arg: " + arg);
            //System.out.println("INFO in KB.askWithRestriction(): term: " + term);
            String thisArg;
            for (Formula f : partial) {
                if (f == null)
                    System.err.println("Error in KB.askWithRestriction(): null formula searching on term: " + term);
                thisArg = f.getStringArgument(arg);
                //System.out.println("INFO in KB.askWithRestriction(): thisArg: " + thisArg);
                if (thisArg == null) {
                    System.err.println("Error in KB.askWithRestriction(): null argument: " + f);
                }
                else if (f.getStringArgument(arg).equals(term))
                    result.add(f);
            }
        }
        //System.out.println("INFO in KB.askWithRestriction(): result: " + result);
        return result;
    }

    /***************************************************************
     * Returns an
     * ArrayList of Formulas in which the two terms provided appear in the
     * indicated argument positions. If there are no Formula(s) matching the
     * given terms and respective argument positions, return an empty ArrayList.
     *
     * @return ArrayList
     */
    public List<Formula> askWithTwoRestrictions(int argnum1, String term1, int argnum2, String term2, int argnum3,
                                                     String term3) {

        String[] args = new String[6];
        args[0] = "argnum1 = " + argnum1;
        args[1] = "term1 = " + term1;
        args[0] = "argnum2 = " + argnum2;
        args[1] = "term2 = " + term2;
        args[0] = "argnum3 = " + argnum3;
        args[1] = "term3 = " + term3;

        List<Formula> result = new ArrayList<>();
        if (StringUtil.isNonEmptyString(term1) && StringUtil.isNonEmptyString(term2)
                && StringUtil.isNonEmptyString(term3)) {
            // a will get the smallest list then b then c
            List<Formula> partiala = new ArrayList<>();
            List<Formula> partial1 = ask("arg", argnum1, term1);
            List<Formula> partial2 = ask("arg", argnum2, term2);
            List<Formula> partial3 = ask("arg", argnum3, term3);
            int argb = -1;
            String termb = "";
            int argc = -1;
            String termc = "";
            if (partial1 == null || partial2 == null || partial3 == null)
                return result;
            if (partial1.size() > partial2.size() && partial1.size() > partial3.size()) {
                argc = argnum1;
                termc = term1;
                if (partial2.size() > partial3.size()) {
                    argb = argnum2;
                    termb = term2;
                    partiala = partial3;
                }
                else {
                    argb = argnum3;
                    termb = term3;
                    partiala = partial2;
                }
            }
            if (partial2.size() > partial1.size() && partial2.size() > partial3.size()) {
                argc = argnum2;
                termc = term2;
                if (partial1.size() > partial3.size()) {
                    argb = argnum1;
                    termb = term1;
                    partiala = partial3;
                }
                else {
                    argb = argnum3;
                    termb = term3;
                    partiala = partial1;
                }
            }
            if (partial3.size() > partial1.size() && partial3.size() > partial2.size()) {
                argc = argnum3;
                termc = term3;
                if (partial1.size() > partial2.size()) {
                    argb = argnum1;
                    termb = term1;
                    partiala = partial2;
                }
                else {
                    argb = argnum2;
                    termb = term2;
                    partiala = partial1;
                }
            }
            if (partiala != null) {
                Formula f, fargb, fargc;
                for (int i = 0; i < partiala.size(); i++) {
                    f = partiala.get(i);
                    fargb = f.getArgument(argb);
                    fargc = f.getArgument(argc);
                    if (f != null && fargb != null && f.getArgument(argb).equals(termb)) {
                        if (fargc != null && f.getArgument(argc).equals(termc))
                            result.add(f);
                    }
                }
            }
        }
        return result;
    }

    /***************************************************************
     * Returns an
     * ArrayList containing the SUO-KIF terms that match the request.
     *
     * @return An ArrayList of terms, or an empty ArrayList if no matches can be
     * found.
     */
    public List<String> getTermsViaAWTR(int argnum1, String term1, int argnum2, String term2, int argnum3,
                                             String term3, int targetArgnum) {

        List<String> ans = new ArrayList<>();
        List<Formula> formulae = askWithTwoRestrictions(argnum1, term1, argnum2, term2, argnum3, term3);
        Formula f;
        for (int i = 0; i < formulae.size(); i++) {
            f = formulae.get(i);
            ans.add(f.getStringArgument(targetArgnum));
        }
        return ans;
    }

    /***************************************************************
     * Returns the
     * first SUO-KIF terms that matches the request, or null.
     *
     * @return A term (String), or null.
     */
    public String getFirstTermViaAWTR(int argnum1, String term1, int argnum2, String term2, int argnum3, String term3,
                                      int targetArgnum) {

        String ans = null;
        List<String> terms = getTermsViaAWTR(argnum1, term1, argnum2, term2, argnum3, term3, targetArgnum);
        if (!terms.isEmpty())
            ans = (String) terms.get(0);
        return ans;
    }

    /***************************************************************
     * Returns an
     * ArrayList containing the terms (Strings) that correspond to targetArgnum
     * in the ground atomic Formulae in which knownArg is in the argument
     * position knownArgnum. The ArrayList returned will contain no duplicate
     * terms.
     *
     * @param knownArgnum  The argument position of knownArg
     * @param knownArg     The term that appears in the argument knownArgnum of the
     *                     ground atomic Formulae in the KB
     * @param targetArgnum The argument position of the terms being sought
     * @return An ArrayList of Strings, which will be empty if no match found.
     */
    public List<String> getTermsViaAsk(int knownArgnum, String knownArg, int targetArgnum) {

        List<String> result = new ArrayList<>();
        List<Formula> formulae = ask("arg", knownArgnum, knownArg);
        if (!formulae.isEmpty()) {
            Set<String> ts = new TreeSet<>();
            Formula f;
            Iterator<Formula> it = formulae.iterator();
            while (it.hasNext()) {
                f = it.next();
                ts.add(f.getStringArgument(targetArgnum));
            }
            result.addAll(ts);
        }
        return result;
    }

    /***************************************************************
     */
    private List<Formula> stringsToFormulas(List<String> strings) {

        List<Formula> result = new ArrayList<>();
        if (strings == null)
            return result;
        Formula f;
        for (String s : strings) {
            f = formulaMap.get(s);
            if (f != null)
                result.add(f);
            else
                System.err.println("Error in KB.stringsToFormulas(): null formula for key: " + s);
        }
        return result;
    }

    /***************************************************************
     * Returns an ArrayList containing the Formulas that match the request.
     *
     * @param kind   May be one of "ant", "cons", "stmt", or "arg"
     * @param term   The term that appears in the statements being requested.
     * @param argnum The argument position of the term being asked for. The first
     *               argument after the predicate is "1". This parameter is ignored
     *               if the kind is "ant", "cons" or "stmt".
     * @return An ArrayList of Formula(s), which will be empty if no match
     * found.
     * see KIF.createKey()
     */
    public List<Formula> ask(String kind, int argnum, String term) {

        List<Formula> result = new ArrayList<>();
        String msg;
        if (StringUtil.emptyString(term)) {
            msg = ("Error in KB.ask(\"" + kind + "\", " + argnum + ", \"" + term + "\"), "
                    + "search term is null, or an empty string");
            errors.add(msg);
        }
        if (term.length() > 1 && term.charAt(0) == '"' && term.charAt(term.length() - 1) == '"') {
            msg = ("Error in KB.ask(), Strings are not indexed.  No results for " + term);
            errors.add(msg);
        }
        List<Formula> tmp;
        String key;
        if (kind.equals("arg"))
            key = kind + "-" + argnum + "-" + term;
        else
            key = kind + "-" + term;
        List<String> alstr = formulas.get(key);

        tmp = stringsToFormulas(alstr);
        if (tmp != null)
            result.addAll(tmp);
        return result;
    }

    /***************************************************************
     * Returns an
     * ArrayList containing the Formulae retrieved, possibly via multiple asks
     * that recursively use relation and all of its subrelations. Note that the
     * Formulas might be formed with different predicates, but all of the
     * predicates will be subrelations of relation and will be related to each
     * other in a subsumption hierarchy.
     * <p>
     * FIXME: this routine only gets subrelations one level down
     *
     * @param relation  The name of a predicate, which is assumed to be the 0th
     *                  argument of one or more atomic formulae
     * @param idxArgnum The argument position occupied by idxTerm in each ground
     *                  Formula to be retrieved
     * @param idxTerm   A constant that occupied idxArgnum position in each ground
     *                  Formula to be retrieved
     * @return an ArrayList of Formulas that satisfy the query, or an empty
     * ArrayList if no Formulae are retrieved.
     */
    public List<Formula> askWithPredicateSubsumption(String relation, int idxArgnum, String idxTerm) {

        List<Formula> ans = new ArrayList<>();
        Set<Formula> accumulator = new HashSet<>();
        if (StringUtil.isNonEmptyString(relation) && StringUtil.isNonEmptyString(idxTerm) && (idxArgnum >= 0)) { // &&
            // (idxArgnum
            // <
            // 7)

            Set<String> relns = new HashSet<>(); // relation and
            // subrelations
            relns.add(relation);
            List<Formula> subrelForms = askWithRestriction(0, "subrelation", 2, relation);
            Formula f;
            String arg;
            for (int i = 0; i < subrelForms.size(); i++) {
                f = subrelForms.get(i);
                arg = f.getStringArgument(1);
                relns.add(arg);
            }
            List<Formula> forms = ask("arg", idxArgnum, idxTerm);
            for (int i = 0; i < forms.size(); i++) {
                f = forms.get(i);
                if (!accumulator.contains(f)) {
                    arg = f.getStringArgument(0);
                    if (relns.contains(arg))
                        accumulator.add(f);
                }
            }
            ans.addAll(accumulator);
        }
        return ans;
    }

    /***************************************************************
     * Returns an
     * ArrayList containing SUO-KIF constants, possibly retrieved via multiple
     * asks that recursively use relation and all of its subrelations.
     *
     * @param relation       The name of a predicate, which is assumed to be the 0th
     *                       argument of one or more atomic Formulae
     * @param idxArgnum      The argument position occupied by term in the ground atomic
     *                       Formulae that will be retrieved to gather the target (answer)
     *                       terms
     * @param idxTerm        A constant that occupies idxArgnum position in each of the
     *                       ground atomic Formulae that will be retrieved to gather the
     *                       target (answer) terms
     * @param targetArgnum   The argument position of the answer terms in the Formulae to
     *                       be retrieved
     * @param useInverses    If true, the inverses of relation and its subrelations will be
     *                       also be used to try to find answer terms
     * @param predicatesUsed A Set to which will be added the predicates of the ground
     *                       assertions actually used to gather the terms returned
     * @return an ArrayList of terms (SUO-KIF constants), or an empty ArrayList
     * if no terms can be retrieved
     */
    public List<String> getTermsViaPredicateSubsumption(String relation, int idxArgnum, String idxTerm,
                                                             int targetArgnum, boolean useInverses, Set predicatesUsed) {

        List<String> ans = new ArrayList<>();
        if (StringUtil.isNonEmptyString(relation) && StringUtil.isNonEmptyString(idxTerm) && (idxArgnum >= 0)
            // && (idxArgnum < 7)
                ) {
            Set<String> reduced = new TreeSet<>();
            List<String> inverseSyns = null;
            List<String> inverses = null;
            if (useInverses) {
                inverseSyns = getTermsViaAskWithRestriction(0, "subrelation", 2, "inverse", 1);
                inverseSyns.addAll(getTermsViaAskWithRestriction(0, "equal", 2, "inverse", 1));
                inverseSyns.addAll(getTermsViaAskWithRestriction(0, "equal", 1, "inverse", 2));
                inverseSyns.add("inverse");
                SetUtil.removeDuplicates(inverseSyns);
                inverses = new ArrayList<>();
            }
            List<String> accumulator = new ArrayList<>();
            List<String> predicates = new ArrayList<>();
            predicates.add(relation);
            while (!predicates.isEmpty()) {
                for (String pred : predicates) {
                    reduced.addAll(
                            getTermsViaAskWithRestriction(0, pred, idxArgnum, idxTerm, targetArgnum, predicatesUsed));
                    accumulator.addAll(getTermsViaAskWithRestriction(0, "subrelation", 2, pred, 1));
                    accumulator.addAll(getTermsViaAskWithRestriction(0, "equal", 2, "subrelation", 1));
                    accumulator.addAll(getTermsViaAskWithRestriction(0, "equal", 1, "subrelation", 2));
                    accumulator.remove(pred);
                    if (useInverses) {
                        for (String syn : inverseSyns) {
                            inverses.addAll(getTermsViaAskWithRestriction(0, syn, 1, pred, 2));
                            inverses.addAll(getTermsViaAskWithRestriction(0, syn, 2, pred, 1));
                        }
                    }
                }
                SetUtil.removeDuplicates(accumulator);
                predicates.clear();
                predicates.addAll(accumulator);
                accumulator.clear();
            }
            if (useInverses) {
                SetUtil.removeDuplicates(inverses);
                for (String inv : inverses)
                    reduced.addAll(getTermsViaPredicateSubsumption(inv, targetArgnum, idxTerm, idxArgnum, false,
                            predicatesUsed));
            }
            ans.addAll(reduced);
        }
        return ans;
    }

    /***************************************************************
     * Returns an
     * ArrayList containing SUO-KIF constants, possibly retrieved via multiple
     * asks that recursively use relation and all of its subrelations.
     *
     * @param relation     The name of a predicate, which is assumed to be the 0th
     *                     argument of one or more atomic Formulae
     * @param idxArgnum    The argument position occupied by term in the ground atomic
     *                     Formulae that will be retrieved to gather the target (answer)
     *                     terms
     * @param idxTerm      A constant that occupies idxArgnum position in each of the
     *                     ground atomic Formulae that will be retrieved to gather the
     *                     target (answer) terms
     * @param targetArgnum The argument position of the answer terms in the Formulae to
     *                     be retrieved
     * @param useInverses  If true, the inverses of relation and its subrelations will be
     *                     also be used to try to find answer terms
     * @return an ArrayList of terms (SUO-KIF constants), or an empty ArrayList
     * if no terms can be retrieved
     */
    public List<String> getTermsViaPredicateSubsumption(String relation, int idxArgnum, String idxTerm,
                                                             int targetArgnum, boolean useInverses) {

        return getTermsViaPredicateSubsumption(relation, idxArgnum, idxTerm, targetArgnum, useInverses, null);
    }

    /***************************************************************
     * Returns the
     * first SUO-KIF constant found via asks using relation and its
     * subrelations.
     *
     * @param relation     The name of a predicate, which is assumed to be the 0th
     *                     argument of one or more atomic Formulae.
     * @param idxArgnum    The argument position occupied by term in the ground atomic
     *                     Formulae that will be retrieved to gather the target (answer)
     *                     terms.
     * @param idxTerm      A constant that occupies idxArgnum position in each of the
     *                     ground atomic Formulae that will be retrieved to gather the
     *                     target (answer) terms.
     * @param targetArgnum The argument position of the answer terms in the Formulae to
     *                     be retrieved.
     * @param useInverses  If true, the inverses of relation and its subrelations will be
     *                     also be used to try to find answer terms.
     * @return A SUO-KIF constants (String), or null if no term can be
     * retrieved.
     */
    public String getFirstTermViaPredicateSubsumption(String relation, int idxArgnum, String idxTerm, int targetArgnum,
                                                      boolean useInverses) {

        String ans = null;
        if (StringUtil.isNonEmptyString(relation) && StringUtil.isNonEmptyString(idxTerm) && (idxArgnum >= 0)
            // && (idxArgnum < 7)
                ) {
            List<String> trms = getTermsViaPredicateSubsumption(relation, idxArgnum, idxTerm, targetArgnum,
                    useInverses);
            if (!trms.isEmpty())
                ans = (String) trms.get(0);
        }
        return ans;
    }

    /***************************************************************
     * Returns an
     * ArrayList containing the transitive closure of relation starting from
     * idxTerm in position idxArgnum. The result does not contain idxTerm.
     *
     * @param relation     The name of a predicate, which is assumed to be the 0th
     *                     argument of one or more atomic Formulae
     * @param idxArgnum    The argument position occupied by term in the ground atomic
     *                     Formulae that will be retrieved to gather the target (answer)
     *                     terms
     * @param idxTerm      A constant that occupies idxArgnum position in the first
     *                     "level" of ground atomic Formulae that will be retrieved to
     *                     gather the target (answer) terms
     * @param targetArgnum The argument position of the answer terms in the Formulae to
     *                     be retrieved
     * @param useInverses  If true, the inverses of relation and its subrelations will be
     *                     also be used to try to find answer terms
     * @return an ArrayList of terms (SUO-KIF constants), or an empty ArrayList
     * if no terms can be retrieved
     */
    public List<String> getTransitiveClosureViaPredicateSubsumption(String relation, int idxArgnum, String idxTerm,
                                                                         int targetArgnum, boolean useInverses) {

        List<String> ans = new ArrayList<>();
        Set<String> reduced = new TreeSet<>();
        Set<String> accumulator = new TreeSet<>(
                getTermsViaPredicateSubsumption(relation, idxArgnum, idxTerm, targetArgnum, useInverses));
        List<String> working = new ArrayList<>();
        while (!accumulator.isEmpty()) {
            reduced.addAll(accumulator);
            working.clear();
            working.addAll(accumulator);
            accumulator.clear();
            for (String term : working)
                accumulator
                        .addAll(getTermsViaPredicateSubsumption(relation, idxArgnum, term, targetArgnum, useInverses));
        }
        ans.addAll(reduced);
        return ans;
    }

    /***************************************************************
     * Add all members of one collection to another.  If the argument
     * is null, do nothing.
     */
    public void addAllSafe(Collection c1, Collection c2) {

        if (c1 != null && c2 != null)
            c1.addAll(c2);
    }

    /***************************************************************
     * Get all children of the given term following instance and
     * subclass relations as well as the indicated rel
     */
    @Deprecated // should use KBcache getChildren
    public Set<String> getAllSub(String term, String rel) {

        //System.out.println("KB.getAllSub(): "  + term + " : " + rel);
        List<String> temp = new ArrayList<>();
        Set<String> result = new HashSet<>();
        temp.add(term);
        Set<String> oldResult = new HashSet<>();
        while (!result.equals(oldResult)) {
            //System.out.println("KB.getAllSub(): "  + result);
            oldResult = new HashSet<>();
            oldResult.addAll(temp);
            for (String s : result) {
                addAllSafe(temp,kbCache.getChildTerms(s,"subclass"));
                addAllSafe(temp,kbCache.getChildTerms(s,"instance"));
                addAllSafe(temp,kbCache.getChildTerms(s,rel));
                addAllSafe(temp,kbCache.getInstancesForType(s));
            }
            result.addAll(temp);
            temp = new ArrayList<>();
            temp.addAll(result);
        }
        return result;
    }

    /***************************************************************
     * Merges a
     * KIF object containing a single formula into the current KB.
     *
     * @param kif      A KIF object.
     * @param pathname The full, canonical pathname string of the constituent file in
     *                 which the formula will be saved, if known.
     * @return If any of the formulas are already present, returns an ArrayList
     * containing the old (existing) formulas, else returns an empty
     * ArrayList.
     */
    public List<Formula> merge(KIF kif, String pathname) {

        List<Formula> formulasPresent = new ArrayList<>();
        // Add all the terms from the new formula into the KB's current list
        getTerms().addAll(kif.terms);
        for (String t : kif.terms)
            capterms.put(t.toUpperCase(),t);
        Set<String> keys = kif.formulas.keySet();
        List<String> newFormulas, oldFormulas;
        Formula newFormula, oldFormula, f;
        Iterator<String> it2;
        String newformulaStr;
        for (String key : keys) {
            newFormulas = new ArrayList<>(kif.formulas.get(key));
            if (formulas.containsKey(key)) {
                oldFormulas = formulas.get(key);
                for (int i = 0; i < newFormulas.size(); i++) {
                    newFormula = kif.formulaMap.get(newFormulas.get(i));
                    if (pathname != null)
                        newFormula.sourceFile = pathname;
                    boolean found = false;
                    for (int j = 0; j < oldFormulas.size(); j++) {
                        oldFormula = formulaMap.get(oldFormulas.get(j));
                        if (oldFormula != null && newFormula.getFormula().equals(oldFormula.getFormula())) {
                            found = true;
                            // no duplicate formulas are allowed in
                            // formulasPresent
                            if (formulasPresent != null && !formulasPresent.contains(oldFormula))
                                formulasPresent.add(oldFormula);
                        }
                    }
                    if (!found) {
                        oldFormulas.add(newFormula.getFormula());
                        formulaMap.put(newFormula.getFormula().intern(), newFormula);
                    }
                }
            }
            else {
                formulas.put(key, newFormulas);
                it2 = newFormulas.iterator();
                while (it2.hasNext()) {
                    newformulaStr = it2.next();
                    newFormula = kif.formulaMap.get(newformulaStr);
                    f = formulaMap.get(newformulaStr);
                    if (f == null) // If kb.formulaMap does not contain the new
                        // formula, should we add it into the kb?
                        formulaMap.put(newFormula.getFormula().intern(), newFormula);
                    else if (StringUtil.isNonEmptyString(f.getFormula()))
                        formulaMap.put(f.getFormula().intern(), f);
                }
            }
        }
        return formulasPresent;
    }

    /***************************************************************
     * Rename
     * term2 as term1 throughout the knowledge base. This is an operation with
     * side effects - the term names in the KB are changed.
     */
    public void rename(String term2, String term1) {

        Set<Formula> formulas = new HashSet<>();
        for (int i = 0; i < 7; i++)
            formulas.addAll(ask("arg", i, term2));
        formulas.addAll(ask("ant", 0, term2));
        formulas.addAll(ask("cons", 0, term2));
        formulas.addAll(ask("stmt", 0, term2));
        for (Formula f : formulas) {
            f.read(f.rename(term2, term1).getFormula());
        }
    }

    /***************************************************************
     * Writes a
     * single user assertion (String) to the end of a file.
     *
     * @param formula A String representing a SUO-KIF Formula.
     * @param fname   A String denoting the pathname of the target file.
     * @return A long value indicating the number of bytes in the file after the
     * formula has been written. A value of 0L means that the file does
     * not exist, and so could not be written for some reason. A value
     * of -1 probably means that some error occurred.
     */
    private long writeUserAssertion(String formula, String fname) throws IOException {

        long flen = -1L;

        File file = new File(fname);
        try (Writer fr = new FileWriter(file, true)) {
            fr.write(formula);
            fr.write("\n");
            flen = file.length();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return flen;
    }

    /* *************************************************************
     * Writes all the terms in the knowledge base to a file
     */
    public void writeTerms() throws IOException {

       String fname = KBmanager.getMgr().getPref("kbDir") + File.separator + "terms.txt";

       File file = new File(fname);
       try (Writer fr = new FileWriter(file, true)) {
            for (String term : terms) {
                fr.write(term);
                fr.write("\n");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***************************************************************
     * Adds a formula to the knowledge base.
     *
     * @param input The String representation of a SUO-KIF Formula.
     * @return A String indicating the status of the tell operation.
     */
    public String tell(String input) {

        //System.out.println("KB.tell: eprover: " + eprover);
        //if (eprover == null) {
        //    System.err.println("Error in KB.tell: eprover not initialized");
        //    return null;
        //}

        String result = "The formula could not be added";
        KBmanager mgr = KBmanager.getMgr();
        KIF kif = new KIF(); // 1. Parse the input string.
        String msg = kif.parseStatement(input);
        if (msg != null) {
            result = "Error parsing \"" + input + "\" " + msg;
            return result;
        }
        if (kif.formulaMap.keySet().isEmpty()) {
            result = "The input could not be parsed";
            return result;
        }
        try { // Make the pathname of the user assertions file.
            String userAssertionKIF = this.name + _userAssertionsString;
            String userAssertionTFF = userAssertionKIF.substring(0, userAssertionKIF.indexOf(".kif")) + ".tff";
            String userAssertionTPTP = userAssertionKIF.substring(0, userAssertionKIF.indexOf(".kif")) + ".tptp";
            String userAssertionTHF = userAssertionKIF.substring(0, userAssertionKIF.indexOf(".kif")) + ".thf";
            File dir = new File(this.kbDir);
            File kiffile = new File(dir, (userAssertionKIF)); // create kb.name_UserAssertions.kif
            File tptpfile = null;  // kb.name_UserAssertions.tptp
            if (SUMOKBtoTPTPKB.lang.equals("fof"))
                tptpfile = new File(dir, (userAssertionTPTP));
            if (SUMOKBtoTPTPKB.lang.equals("tff"))
                tptpfile = new File(dir, (userAssertionTFF));
            if (SUMOKBtoTPTPKB.lang.equals("thf"))
                tptpfile = new File(dir, (userAssertionTHF));
            String filename = kiffile.getCanonicalPath();
            List<Formula> formulasAlreadyPresent = merge(kif, filename);
            // only check formulasAlreadyPresent when filterSimpleOnly = false;
            // otherwise, some user assertions/axioms will not be asserted for
            // inference, since these axioms do exist in formulasAlreadyPresent but not in
            // SUMO.tptp. In the future, when SUMO can completely run using whole KB, we
            // can remove SUMOKBtoTPTPKB.fitlerSimpleOnly==false;
            if (!SUMOKBtoTPTPKB.FILTER_SIMPLE_ONLY && !formulasAlreadyPresent.isEmpty()) {
                String sf = formulasAlreadyPresent.get(0).sourceFile;
                result = "The formula was already added from " + sf;
            }
            else {
                List<Formula> parsedFormulas = new ArrayList();
                String term;
                for (Formula parsedF : kif.formulaMap.values()) { // 2. Confirm that the input has been
                    // converted into at least one Formula object and stored in this.formulaMap.
                    if (debug) System.out.println("KB.tell(): " + parsedF.toString());
                    term = PredVarInst.hasCorrectArity(parsedF, this);
                    if (!StringUtil.emptyString(term)) {
                        result = result + "Formula in " + parsedF.sourceFile
                                + " rejected due to arity error of predicate " + term + " in formula: \n"
                                + parsedF.getFormula();
                    }
                    else
                        parsedFormulas.add(parsedF);
                }
                if (!parsedFormulas.isEmpty()) {
                    if (!constituents.contains(filename)) {
                        if (kiffile.exists()) // 3. If the assertions file exists, delete it.
                            kiffile.delete();
                        if (tptpfile.exists())
                            tptpfile.delete();
                        constituents.add(filename);
                    }
                    for (Formula parsedF : parsedFormulas) { // 4. Write the formula to the user assertions file.
                        parsedF.endFilePosition = writeUserAssertion(parsedF.getFormula(), filename);
                        parsedF.sourceFile = filename;
                    }
                    result = "The formula has been added for browsing";
                    // 5. Write the formula to the kb.name_UserAssertions.tptp/tff
                    if (null == KBmanager.getMgr().prover) result += " but not for local inference";
                    else
                        switch (KBmanager.getMgr().prover) {
                            case EPROVER:
                                if (debug) System.out.println("KB.tell: using eprover: " + eprover);
                                eprover.assertFormula(tptpfile.getCanonicalPath(), this, eprover, parsedFormulas,
                                        !mgr.getPref("TPTP").equalsIgnoreCase("no"));
                                EProver.addBatchConfig(tptpfile.getCanonicalPath(), 60); // 6. Add the new tptp file into EBatching.txt
                                eprover = new EProver(mgr.getPref("eprover")); // 7. Reload eprover
                                result += " and inference";
                                break;
                            case VAMPIRE:
                                if (debug) System.out.println("KB.tell: using vampire");
                                Vampire.assertFormula(tptpfile.getCanonicalPath(), this, parsedFormulas,
                                        !mgr.getPref("TPTP").equalsIgnoreCase("no"));
                                // nothing much to do since Vampire has to load it all at query time
                                // just create a single file
                                result += " and inference";
                                break;
                            case LEO:
                                if (debug) System.out.println("KB.tell: using leo");
                                LEO.assertFormula(tptpfile.getCanonicalPath(), this, parsedFormulas,
                                        !mgr.getPref("TPTP").equalsIgnoreCase("no"));
                                // nothing much to do since LEO has to load it all at query time
                                // just create a single file
                                result += " and inference";
                                break;
                            default:
                                result += " but not for local inference";
                                break;
                        }
                }
            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            System.err.println(ioe.getMessage());
            result = ioe.getMessage();
        }
        return result;
    }

    /***************************************************************
     * Submits a
     * query to the inference engine.
     *
     * @param suoKifFormula The String representation of the SUO-KIF query.
     * @param timeout       The number of seconds after which the inference engine should
     *                      give up.
     * @param maxAnswers    The maximum number of answers (binding sets) the inference
     *                      engine should return.
     * @return A String indicating the status of the ask operation.
     */
    public EProver askEProver(String suoKifFormula, int timeout, int maxAnswers) {

        try {
            if (eprover == null) {
                String lang = "tff";
                if (SUMOKBtoTPTPKB.lang.equals("fof"))
                    lang = "tptp";
                eprover = new EProver(KBmanager.getMgr().getPref("eprover"),
                        System.getenv("SIGMA_HOME") + "/KBs/" + name + "." + lang);
            }
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        if (StringUtil.isNonEmptyString(suoKifFormula)) {
            loadEProver();
            Formula query = new Formula();
            query.read(suoKifFormula);
            FormulaPreprocessor fp = new FormulaPreprocessor();
            Set<Formula> processedStmts = fp.preProcess(query, true, this);
            if (!processedStmts.isEmpty() && this.eprover != null) {
                // set timeout in EBatchConfig file and reload eprover
                try {
                    EProver.addBatchConfig(null, timeout);
                    eprover = new EProver(KBmanager.getMgr().getPref("eprover"), maxAnswers < 1 ? 1 : maxAnswers);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                String strQuery = processedStmts.iterator().next().getFormula();
                eprover.submitQuery(strQuery, this);
            }
        }
        return eprover;
    }

    /***************************************************************
     * Submits a
     * query to the inference engine.
     *
     * @param suoKifFormula The String representation of the SUO-KIF query.
     * @param timeout       The number of seconds after which the inference engine should
     *                      give up.
     * @param maxAnswers    The maximum number of answers (binding sets) the inference
     *                      engine should return.
     * @return A String indicating the status of the ask operation.
     */
    public LEO askLeo(String suoKifFormula, int timeout, int maxAnswers) {

        System.out.println("KB.askLeo(): query: " + suoKifFormula);
        try {
            if (leo == null) {
                leo = new LEO();
            }
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return null;
        }
        THF thf = new THF();
        if (StringUtil.isNonEmptyString(suoKifFormula)) {
            loadLeo();
            Formula query = new Formula();
            query.read(suoKifFormula);
            FormulaPreprocessor fp = new FormulaPreprocessor();
            Set<Formula> processedQuery = fp.preProcess(query, true, this);
            if (!processedQuery.isEmpty() && this.leo != null) {
                String dir = KBmanager.getMgr().getPref("kbDir") + File.separator;
                String kbName = name;
                File s = new File(dir + kbName + ".thf");
                if (!s.exists()) {
                    System.out.println("KB.askLeo(): no such file: " + s + ". Creating it.");
                    KB kb = KBmanager.getMgr().getKB(kbName);
                    KBmanager.getMgr().loadKBforInference(kb);
                }
                Set<String> thfquery = new HashSet<>();
                StringBuilder combined = new StringBuilder();
                if (processedQuery.size() > 1) {
                    combined.append("(or ");
                    for (Formula p : processedQuery) {
                        combined.append(p.getFormula()).append(" ");
                    }
                    combined.append(")");
                    String theTHFstatement =
                            thf.oneKIF2THF(new Formula(combined.toString()), true, this).trim(); // true - it's a query
                    thfquery.add(theTHFstatement);
                }
                else {
                    String theTPTPstatement =
                            thf.oneKIF2THF(processedQuery.iterator().next(), true, this).trim(); // true - it's a query
                    thfquery.add(theTPTPstatement);
                }
                try {
                    System.out.println("KB.askLeo(): calling with: " + s + ", " + timeout + ", " + thfquery);
                    System.out.println("KB.askLeo(): qlist: " + leo.qlist);
                    leo.run(this, s, timeout, thfquery);
                    return leo;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String strQuery = processedQuery.iterator().next().getFormula();
            }
            else
                System.err.println("Error in KB.askLeo(): no TPTP formula translation for query: " + query);
        }
        return leo;
    }

    /***************************************************************
     * Submits a
     * query to the inference engine.
     *
     * @param suoKifFormula The String representation of the SUO-KIF query.
     * @param timeout       The number of seconds after which the inference engine should
     *                      give up.
     * @param maxAnswers    The maximum number of answers (binding sets) the inference
     *                      engine should return.
     * @return A String indicating the status of the ask operation.
     */
    public Vampire askVampire(String suoKifFormula, int timeout, int maxAnswers) {

        if (StringUtil.isNonEmptyString(suoKifFormula)) {
            loadVampire();
            Formula query = new Formula();
            query.read(suoKifFormula);
            FormulaPreprocessor fp = new FormulaPreprocessor();
            Set<Formula> processedStmts = fp.preProcess(query, true, this);
            System.out.println("KB.askVampire(): processed query: " + processedStmts);
            if (!processedStmts.isEmpty()) {
                int axiomIndex = 0;
                String dir = KBmanager.getMgr().getPref("kbDir") + File.separator;
                String kbName = name;
                String lang = "tff";
                if (SUMOKBtoTPTPKB.lang.equals("fof"))
                    lang = "tptp";
                else
                    SUMOtoTFAform.initOnce();
                System.out.println("KB.askVampire(): lang: " + lang);
                File s = new File(dir + kbName + "." + lang);
                if (!s.exists()) {
                    System.out.println("Vampire.askVampire(): no such file: " + s + ". Creating it.");
                    KB kb = KBmanager.getMgr().getKB(kbName);
                    KBmanager.getMgr().loadKBforInference(kb);
                }
                else {
                    Set<String> tptpquery = new HashSet<>();
                    StringBuilder combined = new StringBuilder();
                    if (processedStmts.size() > 1) {
                        combined.append("(or ");
                        for (Formula p : processedStmts) {
                            combined.append(p.getFormula()).append(" ");
                        }
                        combined.append(")");
                        String theTPTPstatement = SUMOKBtoTPTPKB.lang + "(query" + "_" + axiomIndex++ +
                                ",conjecture,(" +
                                SUMOformulaToTPTPformula.tptpParseSUOKIFString(combined.toString(), true) // true - it's a query
                                + ")).";
                        tptpquery.add(theTPTPstatement);
                    }
                    else {
                        String theTPTPstatement = SUMOKBtoTPTPKB.lang + "(query" + "_" + axiomIndex++ +
                                ",conjecture,(" +
                                SUMOformulaToTPTPformula.tptpParseSUOKIFString(processedStmts.iterator().next().getFormula(), true) // true - it's a query
                                + ")).";
                        tptpquery.add(theTPTPstatement);
                    }
                    try {
                        System.out.println("KB.askVampire(): calling with: " + s + ", " + timeout + ", " + tptpquery);
                        System.out.println("KB.askVampire(): qlist: " + SUMOformulaToTPTPformula.qlist);
                        System.out.println("KB.askVampire(): mode before: " + Vampire.mode);
                        Vampire vampire = new Vampire();
                        if (Vampire.mode == null) {
                            if (!StringUtil.emptyString(System.getenv("VAMPIRE_OPTS")))
                                Vampire.mode = Vampire.ModeType.CUSTOM;
                            else
                                Vampire.mode = Vampire.ModeType.CASC;
                        }
                        System.out.println("KB.askVampire(): mode: " + Vampire.mode);
                        vampire.run(this, s, timeout, tptpquery);
                        vampire.qlist = SUMOformulaToTPTPformula.qlist;
                        return vampire;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    //vampire.terminate();
                }
            }
            else
                System.err.println("Error in KB.askVampire(): no TPTP formula translation for query: " + query);
        }
        return null;
    }

    /***************************************************************
     * Return a SUMO-formatted proof string
     */
    public String askVampireFormat(String suoKifFormula, int timeout, int maxAnswers) {

        StringBuilder sb = new StringBuilder();
        if (!StringUtil.emptyString(System.getenv("VAMPIRE_OPTS")))
            Vampire.mode = Vampire.ModeType.CUSTOM;
        else
            Vampire.mode = Vampire.ModeType.CASC;
        Vampire vampire = askVampire(suoKifFormula,30,1);
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        tpp.parseProofOutput(vampire.output, suoKifFormula, this, vampire.qlist);
        String result = tpp.proof.toString().trim();
        sb.append(result).append("\n");
        result = tpp.bindings.toString();
        sb.append("answers: ").append(result).append("\n");
        return sb.toString();
    }

    /***************************************************************
     * Submits a
     * query to the inference engine. Returns a list of answers from inference
     * engine. If no proof is found, return null;
     *
     * @param suoKifFormula The String representation of the SUO-KIF query.
     * @return A list of answers from inference engine; If no proof or answer is
     * found, return null;
     */
    public List<String> askNoProof(String suoKifFormula, int timeout, int maxAnswers) {

        List<String> answers = new ArrayList<>();
        if (StringUtil.isNonEmptyString(suoKifFormula)) {
            Formula query = new Formula();
            query.read(suoKifFormula);
            FormulaPreprocessor fp = new FormulaPreprocessor();
            Set<Formula> processedStmts = fp.preProcess(query, true, this);

            if (!processedStmts.isEmpty() && this.eprover != null) {
                // set timeout in EBatchConfig file and reload eprover
                try {
                    EProver.addBatchConfig(null, timeout);
                    eprover = new EProver(KBmanager.getMgr().getPref("eprover"));
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                String strQuery = processedStmts.iterator().next().getFormula();
                eprover.submitQuery(strQuery, this);
                if (eprover.output == null || eprover.output.isEmpty())
                    System.out.println("No response from EProver!");
                else
                    System.out.println("Get response from EProver, start for parsing ...");
                // System.out.println("Results returned from E = \n" + EResult);
                TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
                answers = tpp.parseAnswerTuples(eprover.output, strQuery, this,eprover.qlist);
                return answers;
            }
        }
        return null;
    }

    /**
     * *************************************************************
     * Submits a
     * query to specified InferenceEngine object. Returns an XML formatted
     * String that contains the response of the inference engine. It should be
     * in the form "<queryResponse>...</queryResponse>".
     *
     * @param suoKifFormula The String representation of the SUO-KIF query.
     * @param timeout       The number of seconds after which the underlying inference
     *                      engine should give up. (Time taken by axiom selection doesn't
     *                      count.)
     * @param maxAnswers    The maximum number of answers (binding sets) the inference
     *                      engine should return.
     * @param engine        InferenceEngine object that will be used for the inference.
     * @return A String indicating the status of the ask operation.
     */
    public String askEngine(String suoKifFormula, int timeout, int maxAnswers, InferenceEngine engine) {

        // Start by assuming that the ask is futile.
        String result = "<queryResponse>\n<answer result=\"no\" number=\"0\">\n</answer>\n<summary proofs=\"0\"/>\n</queryResponse>\n";
        if (!StringUtil.emptyString(suoKifFormula)) {
            Formula query = new Formula();
            query.read(suoKifFormula);
            FormulaPreprocessor fp = new FormulaPreprocessor();
            Set<Formula> processedStmts = fp.preProcess(query, true, this);
            try {
                if (!processedStmts.isEmpty()) {
                    String strQuery = processedStmts.iterator().next().getFormula();
                    result = engine.submitQuery(strQuery, timeout, maxAnswers);
                }
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
                String message = ioe.getMessage().replaceAll(":", "&#58;");
                errors.add(message);
                result = ioe.getMessage();
            }
        }
        result = result.replaceAll("&lt;", "<");
        result = result.replaceAll("&gt;", ">");
        return result;
    }

    /**************************************************************
     * Submits a query to the SInE inference engine. Returns an XML formatted String that
     * contains the response of the inference engine. It should be in the form
     * "<queryResponse>...</queryResponse>".
     *
     * @param suoKifFormula The String representation of the SUO-KIF query.
     * @param timeout       The number of seconds after which the underlying inference
     *                      engine should give up. (Time taken by axiom selection doesn't
     *                      count.)
     * @param maxAnswers    The maximum number of answers (binding sets) the inference
     *                      engine should return.
     * @return A String indicating the status of the ask operation.
     */
    public String askSInE(String suoKifFormula, int timeout, int maxAnswers) {

        InferenceEngine.EngineFactory factory = SInE.getFactory();
        InferenceEngine engine = createInferenceEngine(factory);
        String result = askEngine(suoKifFormula, timeout, maxAnswers, engine);
        try {
            if (engine != null)
                engine.terminate();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            String message = ioe.getMessage().replaceAll(":", "&#58;");
            errors.add(message);
            result = ioe.getMessage();
        }
        return result;
    }

    /**************************************************************
     * Submits a query to the LEO inference engine. Returns an XML formatted String that
     * contains the response of the inference engine. It should be in the form
     * "<queryResponse>...</queryResponse>".
     *
     * suoKifFormula The String representation of the SUO-KIF query.
     *  timeout       The number of seconds after which the underlying inference
     *                      engine should give up. (Time taken by axiom selection doesn't
     *                      count.)
     *  maxAnswers    The maximum number of answers (binding sets) the inference
     *                      engine should return.
     * @return A String indicating the status of the ask operation.

    public String askLEOOld(String suoKifFormula, int timeout, int maxAnswers, String flag) {

        String result = "";
        try {
            String LeoExecutable = KBmanager.getMgr().getPref("leoExecutable");
            String LeoInput = KBmanager.getMgr().getPref("inferenceTestDir") + "prob.p";
            String LeoProblem;
            String responseLine;
            String LeoOutput = "";
            File LeoExecutableFile = new File(LeoExecutable);
            File LeoInputFile = new File(LeoInput);
            FileWriter LeoInputFileW = new FileWriter(LeoInput);

            List<Formula> selectedQuery = new ArrayList<Formula>();
            Formula newQ = new Formula();
            newQ.read(suoKifFormula);
            selectedQuery.add(newQ);
            List<String> selFs = null;
            if (flag.equals("LeoSine")) {
                SInE sine = new SInE(this.formulaMap.keySet());
                selFs = new ArrayList<String>(sine.performSelection(suoKifFormula));
                sine.terminate();
            }
            else if (flag.equals("LeoLocal"))
                selFs = new ArrayList<String>();
            else if (flag.equals("LeoGlobal")) {
                selFs = new ArrayList<String>();
                Iterator<Formula> it = this.formulaMap.values().iterator();
                while (it.hasNext()) {
                    Formula entry = it.next();
                    selFs.add(entry.toString());
                }
            }
            try { // add user asserted formulas
                File dir = new File(this.kbDir);
                File file = new File(dir, (this.name + _userAssertionsString));
                String filename = file.getCanonicalPath();
                BufferedReader userAssertedInput = new BufferedReader(new FileReader(filename));

                try {
                    String line = null;
                    /
                     * readLine is a bit quirky : it returns the content of a
                     * line MINUS the newline. it returns null only for the END
                     * of the stream. it returns an empty String if two newlines
                     * appear in a row.

                    while ((line = userAssertedInput.readLine()) != null)
                        selFs.add(line);
                }
                finally {
                    userAssertedInput.close();
                }
            }
            catch (IOException ex) {
                System.err.println("Error in KB.askLEO(): " + ex.getMessage());
                ex.printStackTrace();
            }
            List<Formula> selectedFormulas = new ArrayList();
            Formula newF = new Formula();

            Iterator<String> it = selFs.iterator();
            while (it.hasNext()) {
                String entry = it.next();
                newF = new Formula();
                newF.read(entry);
                selectedFormulas.add(newF);
            }
            System.out.println(selFs.toString());
            THF thf = new THF();
            LeoProblem = thf.KIF2THF(selectedFormulas, selectedQuery, this);
            LeoInputFileW.write(LeoProblem);
            LeoInputFileW.close();

            String command = LeoExecutableFile.getCanonicalPath() + " -po 1 -t " + timeout + " "
                    + LeoInputFile.getCanonicalPath();

            Process leo = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(leo.getInputStream()));
            while ((responseLine = reader.readLine()) != null)
                LeoOutput += responseLine + "\n";
            reader.close();
            System.out.println(LeoOutput);

            if (LeoOutput.contains("SZS status Theorem")) {
                result = "Answer 1. yes" + "<br> <br>" + LeoProblem.replaceAll("\\n", "<br>") + "<br> <br>"
                        + LeoOutput.replaceAll("\\n", "<br>");
            }
            else {
                result = "Answer 1. don't know" + "<br> <br>" + LeoProblem.replaceAll("\\n", "<br>") + "<br> <br>"
                        + LeoOutput.replaceAll("\\n", "<br>");
            }
        }
        catch (Exception ex) {
            System.err.println("Error in KB.askLEO(): " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    /*****************************************************************
     * Count the number of "levels" deep the term is in taxonomic
     * relations from Entity.  Remove trailing '+' signs.  Also handle
     * a composite term like (UnionFn A B) with a warning
     */
    public int termDepth(String term) {

        //System.out.println("KB.termDepth(): " + term);
        if (term.endsWith("+"))
            term = term.substring(0,term.length()-1);
        if (term.startsWith("(")) {
            System.out.println("KB.termDepth(): warning - composite term: " + term);
            Formula f = new Formula(term);
            String arg1 = f.getStringArgument(1);
            String arg2 = f.getStringArgument(2);
            return Integer.max(termDepth(arg1), termDepth(arg2));
        }
        if (!terms.contains(term)) {
            if (!StringUtil.emptyString(term))
                System.out.println("KB.termDepth(): no such term " + term);
            //Thread.dumpStack();
            return 0;
        }
        if (term.equals("Entity") || StringUtil.isNumeric(term))
            return 0;
        if (!kbCache.subclassOf(term,"Entity") && !kbCache.transInstOf(term,"Entity"))
            return 0;
        if (termDepthCache.containsKey(term))
            return termDepthCache.get(term);
        Set<String> rents = immediateParents(term);
        for (String s : rents) {
            int depth = 1 + termDepth(s);
            termDepthCache.put(term,depth);
            return depth;
        }
        return 0;
    }

    /*****************************************************************
     */
    public Set<String> immediateParents(String term) {

        //System.out.println("KB.immediateParents(): " + term);
        Set<String> result = new HashSet<>();
        if (!terms.contains(term)) {
            System.out.println("KB.immediateParents(): no such term " + term);
            return result;
        }
        List<Formula> forms = askWithRestriction(0,"subclass",1,term);
        forms.addAll(askWithRestriction(0,"instance",1,term));
        forms.addAll(askWithRestriction(0,"subrelation",1,term));
        forms.addAll(askWithRestriction(0,"subAttribute",1,term));
        //System.out.println("KB.immediateParents(): forms: " + forms);
        for (Formula f : forms) {
            //System.out.println("KB.immediateParents(): f: " + f);
            if (!f.isCached())
                result.add(f.getStringArgument(2));
        }
        //System.out.println("KB.immediateParents(): result: " + result);
        return result;
    }

    /*****************************************************************
     * Analogous to compareTo(), return -1,0 or 1 depending on whether
     * the first term is "smaller", equal to or "greater" than the
     * second, respectively.  A term that is the parent of another
     * is "smaller".  If not a parent of the other, the smaller term
     * is that which is fewer "levels" from their common parent.
     * Therefore, terms that are not the same can still be "equal"
     * if they're at the same level of the taxonomy.
     */
    public int compareTermDepth(String t1, String t2) {

        if (debug) System.out.println("KB.compareTermDepth(): ");
        if (debug) System.out.println("KB.compareTermDepth(): subattribute: " + kbCache.subAttributeOf(t1,t2));
        if (debug) System.out.println("KB.compareTermDepth(): subclass: " + kbCache.subclassOf(t1,t2));
        if (t1.equals(t2))
            return 0;
        if (kbCache.subAttributeOf(t2,t1) || kbCache.subclassOf(t2,t1))
            return -1;
        if (kbCache.subAttributeOf(t1,t2) || kbCache.subclassOf(t1,t2))
            return 1;
        //String p = kbCache.getCommonParent(t1,t2);
        //boolean found = false;
        int depthT1 = termDepth(t1);
        int depthT2 = termDepth(t2);
        if (debug) System.out.println("KB.compareTermDepth(): term depth of " + t1 + " is " + depthT1);
        if (debug) System.out.println("KB.compareTermDepth(): term depth of " + t2 + " is " + depthT2);
        if (depthT1 == depthT2)
            return 0;
        if (depthT1 > depthT2)
            return 1;
        return -1;
    }

    /*****************************************************************
     * Find the most specific term in a collection using compareTermDepth()
     */
    public String mostGeneralType(Collection<String> terms) {

        if (terms == null || terms.size() < 1)
            return null;
        String result = "";
        for (String t : terms) {
            if (StringUtil.emptyString(t))
                continue;
            if (!containsTerm(t)) {
                System.err.println("Error in KB.mostSpecificType(): no such term: " + t);
                continue;
            }
            if ("".equals(result) || compareTermDepth(t,result) < 0)
                result = t;
        }
        return result;
    }

    /*****************************************************************
     * Find the most specific term in a collection using compareTermDepth()
     */
    public String mostSpecificType(Collection<String> terms) {

        if (terms == null || terms.size() < 1)
            return null;
        String result = "";
        for (String t : terms) {
            if (StringUtil.emptyString(t))
                continue;
            if (!containsTerm(t)) {
                System.err.println("Error in KB.mostSpecificType(): no such term: " + t);
                continue;
            }
            if ("".equals(result) || compareTermDepth(t,result) > 0)
                result = t;
        }
        return result;
    }

    /*****************************************************************
     * Find the most specific term in a collection using compareTermDepth()
     */
    public String mostSpecificTerm(Collection<String> terms) {

        if (terms == null || terms.size() < 1)
            return null;
        String result = "";
        for (String t : terms) {
            if (debug) System.out.println("mostSpecificTerm(): t: " + t);
            if (debug) System.out.println("mostSpecificTerm(): depth: " + termDepth(t));
            if (debug) System.out.println("mostSpecificTerm(): result: " + result);
            if (debug) System.out.println("mostSpecificTerm(): result depth: " + termDepth(result));
            if (debug) System.out.println("mostSpecificTerm(): compareTermDepth(t,result): " + compareTermDepth(t,result));
            if (StringUtil.emptyString(t))
                continue;
            if (t.endsWith("+"))
                t = t.substring(0,t.length()-1);
            if (!containsTerm(t)) {
                System.err.println("Error in KB.mostSpecificTerm(): no such term: " + t);
                continue;
            }
            if ("".equals(result) || compareTermDepth(t,result) > 0)
                result = t;
        }
        return result;
    }

    /*****************************************************************
     * Takes a term and returns true if the term occurs in the KB.
     *
     * @param term A String.
     * @return true or false.
     */
    public boolean containsTerm(String term) {

        if (StringUtil.emptyString(term))
            return false;
        //else if (getREMatch(term.intern()).size() >= 1)
        //    return true;
        return getTerms().contains(term.intern());
    }

    /*****************************************************************
     * Takes a filename without path and returns true if it occurs in the KB.
     *
     * @param fname A String.
     * @return true or false.
     */
    public boolean containsFile(String fname) {

        for (String path : constituents) {
            if (path.contains("/")) {
                path = FileUtil.noPath(path);
            }
            if (path.equals(fname))
                return true;
        }
        return false;
    }

    /*****************************************************************
     * Takes a formula string and returns true if the corresponding Formula occurs in
     * the KB.
     *
     * @param formula A String.
     * @return true or false.
     */
    public boolean containsFormula(String formula) {

        return formulaMap.containsKey(formula.intern());
    }

    /*****************************************************************
     * Count the number of terms in the knowledge base in order to
     * present statistics to the user.
     *
     * @return The int(eger) number of terms in the knowledge base.
     */
    public int getCountTerms() {

        return getTerms().size();
    }

    /*****************************************************************
     * Count the number of relations in the knowledge base in order to present statistics
     * to the user.
     *
     * @return The int(eger) number of relations in the knowledge base.
     */
    public int getCountRelations() {

        return kbCache.relations.size();
    }

    /*****************************************************************
     * Count the number of formulas in the knowledge base in order to present statistics
     * to the user.
     *
     * @return The int(eger) number of formulas in the knowledge base.
     */
    public int getCountAxioms() {

        return formulaMap.size();
    }

    /*****************************************************************
     * An accessor providing a TreeSet of un-preProcessed String representations of
     * Formulae.
     *
     * @return A TreeSet of Strings.
     */
    public TreeSet<String> getFormulas() {

        return new TreeSet<>(formulaMap.keySet());
    }

    /*****************************************************************
     * An accessor providing a Formula
     */
    public Formula getFormulaByKey(String key) {

        Formula f = null;
        List<String> al = formulas.get(key);
        if ((al != null) && !al.isEmpty())
            f = formulaMap.get(al.get(0));
        return f;
    }

    /*****************************************************************
     * Count the number of rules in the knowledge base in order to present statistics to
     * the user. Note that the number of rules is a subset of the number of
     * formulas.
     *
     * @return The int(eger) number of rules in the knowledge base.
     */
    public int getCountRules() {

        int count = 0;
        for (Formula f : formulaMap.values()) {
            if (f.isRule())
                count++;
        }
        return count;
    }

    /*****************************************************************
     * Create an ArrayList of the specific size, filled with empty strings.
     */
    private List<String> arrayListWithBlanks(int size) {

        List<String> al = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            al.add("");
        return al;
    }

    /** ***************************************************************
     * Get the alphabetically nearest terms to the given term, which is not in the KB.
     * Elements 0-(k-1) should be alphabetically lesser and k-(2*k-1)
     * alphabetically greater. If the term is at the beginning or end of the
     * alphabet, fill in blank items with the empty string: "".
     */
    private List<String> getNearestKTerms(String term, int k) {

        List<String> al;
        if (k == 0)
            al = arrayListWithBlanks(1);
        else
            al = arrayListWithBlanks(2 * k);
        Object[] t;
        t = getTerms().toArray();
        int i = 0;
        while (i < t.length - 1 && ((String) t[i]).compareTo(term) < 0)
            i++;
        if (k == 0) {
            al.set(0, (String) t[i]);
            return al;
        }
        int lower = i;
        while (i - lower < k && lower > 0) {
            lower--;
            al.set(k - (i - lower), (String) t[lower]);
        }
        int upper = i - 1;
        while (upper - i < (k - 1) && upper < t.length - 1) {
            upper++;
            al.set(k + (upper - i), (String) t[upper]);
        }
        return al;
    }

    /*****************************************************************
     * Get the alphabetically nearest terms to the given term, which is not in the KB.
     * Elements 0-14 should be alphabetically lesser and 15-29 alphabetically
     * greater. If the term is at the beginning or end of the alphabet, fill in
     * blank items with the empty string: "".
     */
    private List<String> getNearestTerms(String term) {

        return getNearestKTerms(term, 15);
    }

    /*****************************************************************
     * Get the neighbors of this initial uppercase term (class or function).
     */
    public List<String> getNearestRelations(String term) {

        term = Character.toLowerCase(term.charAt(0)) + term.substring(1, term.length());
        return getNearestTerms(term);
    }

    /*****************************************************************
     * Get the neighbors of this initial lowercase term (relation).
     */
    public List<String> getNearestNonRelations(String term) {

        term = Character.toUpperCase(term.charAt(0)) + term.substring(1, term.length());
        return getNearestTerms(term);
    }

    /*****************************************************************
     * Get the alphabetically num lower neighbor of this initial term, which must exist
     * in the current KB otherwise an empty string is returned.
     */
    public String getAlphaBefore(String term, int num) {

        if (!getTerms().contains(term)) {
            List<String> al = getNearestKTerms(term, 0);
            term = (String) al.get(0);
        }
        if (getTerms().size() < 1)
            return "";
        List<String> tal = new ArrayList<>(getTerms());
        int i = tal.indexOf(term.intern());
        if (i < 0)
            return "";
        i = i - num;
        if (i < 0)
            i = 0;
        return (String) tal.get(i);
    }

    /*****************************************************************
     * Get the alphabetically num higher neighbor of this initial term, which must exist
     * in the current KB otherwise an empty string is returned.
     */
    public String getAlphaAfter(String term, int num) {

        if (!getTerms().contains(term)) {
            List<String> al = getNearestKTerms(term, 0);
            term = (String) al.get(0);
        }
        if (getTerms().size() < 1)
            return "";
        List<String> tal = new ArrayList<>(getTerms());
        int i = tal.indexOf(term.intern());
        if (i < 0)
            return "";
        i = i + num;
        if (i >= tal.size())
            i = tal.size() - 1;
        return (String) tal.get(i);
    }

    /****************************************************************
     * This List is used to limit the number of warning messages logged by
     * loadFormatMaps(lang). If an attempt to load format or termFormat values
     * for lang is unsuccessful, the list is checked for the presence of lang.
     * If lang is not in the list, a warning message is logged and lang is added
     * to the list. The list is cleared whenever a constituent file is added or
     * removed for KB, since the latter might affect the availability of format
     * or termFormat values.
     */
    protected List<String> loadFormatMapsAttempted = new ArrayList<>();

    /****************************************************************
     * Populates the format maps for language lang.
     *
     * see termFormatMap is a HashMap of language keys and HashMap values. The
     *      interior HashMaps are term keys and format string values.
     *
     * see formatMap is the same but for relation format strings.
     */
    public void loadFormatMaps(String lang) {

        if (formatMap == null)
            formatMap = new HashMap<>();
        if (termFormatMap == null)
            termFormatMap = new HashMap<>();
        if (formatMap.get(lang) == null)
            formatMap.put(lang, new HashMap<>());
        if (termFormatMap.get(lang) == null)
            termFormatMap.put(lang, new HashMap<>());

        String key, format;
        if (!loadFormatMapsAttempted.contains(lang)) {
            List<Formula> col = askWithRestriction(0, "format", 1, lang);
            if ((col == null) || col.isEmpty())
                System.err.println("Error in KB.loadFormatMaps(): No relation format file loaded for language " + lang);
            else {
                Map<String, String> langFormatMap = formatMap.get(lang);
                for (Formula f : col) {
                    key = f.getStringArgument(2);
                    format = f.getStringArgument(3);
                    format = StringUtil.removeEnclosingQuotes(format);
                    langFormatMap.put(key, format);
                }
            }
            col = askWithRestriction(0, "termFormat", 1, lang);
            if ((col == null) || col.isEmpty())
                System.err.println("Error in KB.loadFormatMaps(): No term format file loaded for language: " + lang);
            else {
                Map<String, String> langTermFormatMap = termFormatMap.get(lang);
                for (Formula f : col) {
                    key = f.getStringArgument(2);
                    format = f.getStringArgument(3);
                    format = StringUtil.removeEnclosingQuotes(format);
                    langTermFormatMap.put(key, format);
                }
            }
            loadFormatMapsAttempted.add(lang);
        }
        language = lang;
    }

    /*****************************************************************
     * Clears all loaded format and termFormat maps, for all languages.
     */
    protected void clearFormatMaps() {

        if (formatMap != null) {
            for (Map<String, String> m : formatMap.values()) {
                if (m != null)
                    m.clear();
            }
            formatMap.clear();
        }
        if (termFormatMap != null) {
            for (Map<String, String> m : termFormatMap.values()) {
                if (m != null)
                    m.clear();
            }
            termFormatMap.clear();
        }
        loadFormatMapsAttempted.clear();
    }

    /*****************************************************************
     * This method creates a dictionary (Map) of SUO-KIF term symbols -- the keys --
     * and a natural language string for each key that is the preferred name for
     * the term -- the values -- in the context denoted by lang. If the Map has
     * already been built and the language hasn't changed, just return the
     * existing map. This is a case of "lazy evaluation".
     *
     * @return An instance of Map where the keys are terms and the values are
     *         format strings.
     */
    public Map<String, String> getTermFormatMap(String lang) {

        if (!StringUtil.isNonEmptyString(lang))
            lang = "EnglishLanguage";
        if ((termFormatMap == null) || termFormatMap.isEmpty())
            loadFormatMaps(lang);
        Map<String, String> langTermFormatMap = termFormatMap.get(lang);
        if ((langTermFormatMap == null) || langTermFormatMap.isEmpty())
            loadFormatMaps(lang);
        return (HashMap<String, String>) termFormatMap.get(lang);
    }

    /*****************************************************************
     * This method creates an association list (Map) of the natural language format
     * string and the relation name for which that format string applies. If the
     * map has already been built and the language hasn't changed, just return
     * the existing map. This is a case of "lazy evaluation".
     *
     * @return An instance of Map where the keys are relation names and the
     *         values are format strings.
     */
    public Map<String, String> getFormatMap(String lang) {

        if (!StringUtil.isNonEmptyString(lang))
            lang = "EnglishLanguage";
        if ((formatMap == null) || formatMap.isEmpty())
            loadFormatMaps(lang);
        Map<String, String> langFormatMap = formatMap.get(lang);
        if ((langFormatMap == null) || langFormatMap.isEmpty())
            loadFormatMaps(lang);
        return formatMap.get(lang);
    }

    /*****************************************************************
     * Get the termFormat entry for a given term and language
     */
    public String getTermFormat(String lang, String term) {

        Map<String, String> langFormatMap = getTermFormatMap(lang);
        return langFormatMap.get(term);
    }
    /** *************************************************************
     */
    public void deleteUserAssertionsForInference() {

        String userAssertionTPTP = this.name + KB._userAssertionsTPTP;
        if (SUMOKBtoTPTPKB.lang.equals("tff"))
            userAssertionTPTP = this.name + KB._userAssertionsTFF;
        File dir = new File(KBmanager.getMgr().getPref("kbDir"));
        String fname = dir + File.separator + userAssertionTPTP;
        File ufile = new File(fname);
        if (ufile.exists())
            FileUtil.delete(dir + File.separator + userAssertionTPTP);
    }

    /*****************************************************************
     * Deletes user assertions, both in the files and in the constituents list.
     */
    public void deleteUserAssertions() throws IOException {

        String toRemove = null;
        for (String nme : constituents) {
            if (nme.endsWith(_userAssertionsString)) {
                toRemove = nme;
                break;
            }
        }
        // Remove the string from the list.
        if (toRemove != null) {
            constituents.remove(toRemove);
        }
        deleteUserAssertionsForInference();
    }

    /*****************************************************************
     * Deletes the user assertions key in the constituents map, and then reloads the
     * KBs.
     */
    public void deleteUserAssertionsAndReload() {

        String cname;
        for (int i = 0; i < constituents.size(); i++) {
            cname = (String) constituents.get(i);
            if (cname.endsWith(_userAssertionsString)) {
                try {
                    constituents.remove(i);
                    KBmanager.getMgr().writeConfiguration();
                    reload();
                }
                catch (IOException ioe) {
                    System.err.println(
                            "Error in KB.deleteUserAssertionsAndReload(): writing configuration: " + ioe.getMessage());
                }
            }
        }
        deleteUserAssertionsForInference();
    }

    /***************************************************************
     */
    public KIF readConstituent(String filename) {

        String canonicalPath = null;
        KIF file = null;
        try {
            if (filename.endsWith(".owl") || filename.endsWith(".OWL") || filename.endsWith(".rdf")
                    || filename.endsWith(".RDF")) {
                OWLtranslator.read(filename);
                filename = filename + ".kif";
            }
            File constituent = new File(filename);

            canonicalPath = constituent.getCanonicalPath();
            if (constituents.contains(canonicalPath))
                errors.add("Error. " + canonicalPath + " already loaded.");
            file = new KIF(canonicalPath);
            file.readFile(canonicalPath);
            warnings.addAll(file.warningSet);
        }
        catch (Exception ex1) {
            StringBuilder error = new StringBuilder();
            error.append(ex1.getMessage());
            if (ex1 instanceof ParseException)
                error.append(" at line ").append(((ParseException) ex1).getErrorOffset());
            error.append(" in file ").append(canonicalPath);
            errors.add(error.toString());
            System.err.println("Error in KB.addConstituent(): " + error.toString());
            ex1.printStackTrace();
        }
        file.filename = filename;
        return file;
    }

    /***************************************************************
     * A a formula or formulas into the KB
     */
    public void addConstituentInfo(KIF file) {

        for (Map.Entry<String, Integer> entry : file.termFrequency.entrySet()) {
            if (!termFrequency.containsKey(entry.getKey())) {
                termFrequency.put(entry.getKey(), entry.getValue());
            }
            else {
                termFrequency.put(entry.getKey(), termFrequency.get(entry.getKey()) + entry.getValue());
            }
        }

        int count = 2;
        //System.out.println("INFO in KB.addConstituent(): add keys");
        List<String> newlist, list;
        int total = file.formulas.keySet().size();
        for (String key : file.formulas.keySet()) { // Iterate through keys.
            if ((count++ % 100) == 1)
                System.out.print(".");
            if ((count % 4000) == 1)
                System.out.printf("%nINFO in KB.addConstituent(): still adding keys. %d%% done.%n", count*100/total);
            newlist = file.formulas.get(key);
            list = formulas.get(key);
            if (list != null) {
                newlist.addAll(list);
            }
            formulas.put(key, newlist);
        }

        count = 2;
        Iterator<Formula> it2 = file.formulaMap.values().iterator();
        Formula f;
        String internedFormula;
        //System.out.println("INFO in KB.addConstituent(): add values");
        while (it2.hasNext()) { // Iterate through values
            f = (Formula) it2.next();
            internedFormula = f.getFormula().intern();
            if ((count++ % 100) == 1)
                System.out.print(".");
            if ((count % 4000) == 1)
                System.out.printf("\nINFO in KB.addConstituent(): still adding values. %d%% done.%n", count*100/total);
            if (!formulaMap.containsKey(internedFormula))
                formulaMap.put(internedFormula, f);
        }
        this.getTerms().addAll(file.terms);
        for (String t : file.terms)
            capterms.put(t.toUpperCase(),t);
        if (!constituents.contains(file.filename) && !file.filename.endsWith(_cacheFileSuffix)) // don't add auto-generated cache file
            constituents.add(file.filename);
    }

    /***************************************************************
     * Add a new KB constituent by reading in the file, and then merging the formulas with
     * the existing set of formulas.
     *
     * @param filename
     *            - The full path of the file being added
     */
    public void addConstituent(String filename) {

        long millis = System.currentTimeMillis();
        System.out.println("INFO in KB.addConstituent(): " + filename);
        KIF file = readConstituent(filename);
        addConstituentInfo(file);
        System.out.println("INFO in KB.addConstituent(): added " + file.formulaMap.values().size() + " formulas and "
                + file.terms.size() + " terms.");
        System.out.println("INFO in KB.addConstituent(): " + file.filename + " loaded in seconds: " + (System.currentTimeMillis() - millis) / 1000);

    }

    /*****************************************************************
     * Reload all the KB constituents.
     */
    public String reload() {

        List<String> newConstituents = new ArrayList<>();
        synchronized (this.getTerms()) {
            for (String cName : constituents) {
                if (!KButilities.isCacheFile(cName)) // Recompute cached data
                    newConstituents.add(cName);
            }
            constituents.clear();
            formulas.clear();
            formulaMap.clear();
            terms.clear();
            capterms.clear();
            clearFormatMaps();
            errors.clear();
            Iterator<String> nci = newConstituents.iterator();
            if (nci.hasNext())
                System.out.println("INFO in KB.reload()");

            String cName;
            while (nci.hasNext()) {
                cName = nci.next();
                addConstituent(cName);
                // addConstituent(cName, false, false, false);
            }
            // build kb cache when "cache" = "yes"
            //if (KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes")) {
                kbCache = new KBcache(this);
                kbCache.buildCaches();
                checkArity(); // Re-perform arity checks on everything
            //}
            //else {
            //    kbCache = new KBcache(this);
                // checkArity needs the cache, so don't call it.
            //}
            // At this point, we have reloaded all constituents, have
            // rebuilt the relation caches, and, if cache == yes, have
            // written out the _Cache.kif file. Now we reload the
            // inference engine.
            if (KBmanager.getMgr().prover == KBmanager.Prover.EPROVER)
                loadEProver();
            if (KBmanager.getMgr().prover == KBmanager.Prover.VAMPIRE)
                loadVampire();
        }
        return "";
    }

    /*****************************************************************
     * Write a KIF file consisting of all the formulas in the knowledge base.
     *
     * @param fname
     *            - the name of the file to write, including full path.
     */
    public void writeFile(String fname) throws IOException {

        Set<String> formulaSet = new HashSet<>();

        Iterator<String> it = formulas.keySet().iterator();
        String key;
        List<String> list;
        while (it.hasNext()) {
            key = (String) it.next();
            list = formulas.get(key);
            for (int i = 0; i < list.size(); i++) {
                String s = list.get(i);
                formulaSet.add(s);
            }
        }
        try (FileWriter fr = new FileWriter(fname);
            PrintWriter pr = new PrintWriter(fr)) {
            it = formulaMap.keySet().iterator();
            String s;
            while (it.hasNext()) {
                s = (String) it.next();
                pr.println(s);
                pr.println();
            }
        }
        catch (IOException e) {
            System.err.println("Error in KB.writeFile(): Error writing file " + fname);
            e.printStackTrace();
        }
    }

    /***************************************************************
     * Create the XML configuration element.
     */
    public SimpleElement writeConfiguration() {

        SimpleElement se = new SimpleElement("kb");
        se.setAttribute("name", name);
        SimpleElement constituent;
        String filename;
        for (int i = 0; i < constituents.size(); i++) {
            constituent = new SimpleElement("constituent");
            filename = (String) constituents.get(i);
            filename = KBmanager.escapeFilename(filename);
            constituent.setAttribute("filename", filename);
            se.addChildElement(constituent);
        }
        return se;
    }

    /***************************************************************
     * A HashMap for holding compiled regular expression patterns. The map is initialized
     * by calling compilePatterns().
     */
    private static Map<String, List> REGEX_PATTERNS = null;

    /*****************************************************************
     * This method returns a compiled regular expression Pattern object indexed by
     * key.
     *
     * @param key
     *            A String that is the retrieval key for a compiled regular
     *            expression Pattern.
     *
     * @return A compiled regular expression Pattern instance.
     */
    public static Pattern getCompiledPattern(String key) {

        if (StringUtil.isNonEmptyString(key) && (REGEX_PATTERNS != null)) {
            List al = REGEX_PATTERNS.get(key);
            if (al != null)
                return (Pattern) al.get(0);
        }
        return null;
    }

    /*****************************************************************
     * This method returns the int value that identifies the regular expression
     * binding group to be returned when there is a match.
     *
     * @param key
     *            A String that is the retrieval key for the binding group index
     *            associated with a compiled regular expression Pattern.
     *
     * @return An int that indexes a binding group.
     */
    public static int getPatternGroupIndex(String key) {

        if (StringUtil.isNonEmptyString(key) && (REGEX_PATTERNS != null)) {
            List al = REGEX_PATTERNS.get(key);
            if (al != null)
                return ((Integer) al.get(1));
        }
        return -1;
    }

    /*****************************************************************
     * This method compiles and stores regular expression Pattern objects and binding
     * group indexes as two cell ArrayList objects. Each ArrayList is indexed by
     * a String retrieval key.
     *
     * @return void
     */
    private static void compilePatterns() {

        if (REGEX_PATTERNS == null) {
            REGEX_PATTERNS = new HashMap<>();
            String[][] patternArray = { { "row_var", "\\@ROW\\d*", "0" },
                    // { "open_lit", "\\(\\w+\\s+\\?\\w+\\s+.\\w+\\s*\\)", "0"
                    // },
                    { "open_lit", "\\(\\w+\\s+\\?\\w+[a-zA-Z_0-9-?\\s]+\\)", "0" },
                    { "pred_var_1", "\\(holds\\s+(\\?\\w+)\\W", "1" }, { "pred_var_2", "\\((\\?\\w+)\\W", "1" },
                    { "var_with_digit_suffix", "(\\D+)\\d*", "1" } };
            String pName;
            Pattern p;
            Integer groupN;
            List pVal;
            for (String[] patternArray1 : patternArray) {
                pName = patternArray1[0];
                p = Pattern.compile(patternArray1[1]);
                groupN = Integer.valueOf(patternArray1[2]);
                pVal = new ArrayList();
                pVal.add(p);
                pVal.add(groupN);
                REGEX_PATTERNS.put(pName, pVal);
            }
        }
    }

    /*****************************************************************
     * This method finds regular expression matches in an input string using a
     * compiled Pattern and binding group index retrieved with patternKey. If
     * the ArrayList accumulator is provided, match results are added to it and
     * it is returned. If accumulator is not provided (is null), then a new
     * ArrayList is created and returned if matches are found.
     *
     * @param input
     *            The input String in which matches are sought.
     *
     * @param patternKey
     *            A String used as the retrieval key for a regular expression
     *            Pattern object, and an int index identifying a binding group.
     *
     * @param accumulator
     *            An optional ArrayList to which matches are added. Note that if
     *            accumulator is provided, it will be the return value even if
     *            no new matches are found in the input String.
     *
     * @return An ArrayList, or null if no matches are found and an accumulator
     *         is not provided.
     */
    public static List<String> getMatches(String input, String patternKey, ArrayList<String> accumulator) {

        List<String> ans = null;
        if (accumulator != null)
            ans = accumulator;
        if (REGEX_PATTERNS == null)
            KB.compilePatterns();
        if (StringUtil.isNonEmptyString(input) && StringUtil.isNonEmptyString(patternKey)) {
            Pattern p = KB.getCompiledPattern(patternKey);
            if (p != null) {
                Matcher m = p.matcher(input);
                int gidx = KB.getPatternGroupIndex(patternKey);
                if (gidx >= 0) {
                    String rv;
                    while (m.find()) {
                        rv = m.group(gidx);
                        if (StringUtil.isNonEmptyString(rv)) {
                            if (ans == null)
                                ans = new ArrayList<>();
                            if (!(ans.contains(rv)))
                                ans.add(rv);
                        }
                    }
                }
            }
        }
        return ans;
    }

    /*****************************************************************
     * This method finds regular expression matches in an input string using a
     * compiled Pattern and binding group index retrieved with patternKey, and
     * returns the results, if any, in an ArrayList.
     *
     * @param input
     *            The input String in which matches are sought.
     *
     * @param patternKey
     *            A String used as the retrieval key for a regular expression
     *            Pattern object, and an int index identifying a binding group.
     *
     * @return An ArrayList, or null if no matches are found.
     */
    public static List<String> getMatches(String input, String patternKey) {
        return KB.getMatches(input, patternKey, null);
    }

    /*****************************************************************
     * This method retrieves Formulas by asking the query expression queryLit, and
     * returns the results, if any, in an ArrayList.
     *
     * @param queryLit
     *            The query, which is assumed to be a List (atomic literal)
     *            consisting of a single predicate and its arguments. The
     *            arguments could be variables, constants, or a mix of the two,
     *            but only the first constant encountered in a left to right
     *            sweep over the literal will be used in the actual query.
     *
     * @return An ArrayList of Formula objects, or an empty ArrayList if no
     *         answers are retrieved.
     */
    public List<Formula> askWithLiteral(List<String> queryLit) {

        List<Formula> ans = new ArrayList<>();
        if ((queryLit instanceof List) && !(queryLit.isEmpty())) {
            String pred = (String) queryLit.get(0);
            if (pred.equals("instance") && isVariable(queryLit.get(1)) && !(isVariable(queryLit.get(2)))) {
                String className = queryLit.get(2);
                String inst;
                String fStr;
                Formula f;
                Set<String> ai = getAllInstances(className);
                Iterator<String> it = ai.iterator();
                while (it.hasNext()) {
                    inst = (String) it.next();
                    fStr = ("(instance " + inst + " " + className + ")");
                    f = new Formula();
                    f.read(fStr);
                    ans.add(f);
                }
            }
            else if (pred.equals("valence") && isVariable((String) queryLit.get(1))
                    && isVariable((String) queryLit.get(2))) {
                Set<String> ai = getAllInstances("Relation");
                Iterator<String> it = ai.iterator();
                int valence;
                String inst;
                while (it.hasNext()) {
                    inst = (String) it.next();
                    valence = kbCache.valences.get(inst);
                    if (valence > 0) {
                        String fStr = ("(valence " + inst + " " + valence + ")");
                        Formula f = new Formula();
                        f.read(fStr);
                        ans.add(f);
                    }
                }
            }
            else {
                String constant = null;
                int cidx = -1;
                int qlLen = queryLit.size();
                String term;
                for (int i = 1; i < qlLen; i++) {
                    term = (String) queryLit.get(i);
                    if (StringUtil.isNonEmptyString(term) && !isVariable(term)) {
                        constant = term;
                        cidx = i;
                        break;
                    }
                }
                if (constant != null)
                    ans = askWithRestriction(cidx, constant, 0, pred);
                else
                    ans = ask("arg", 0, pred);
            }
        }
        return ans;
    }

    /*****************************************************************
     * This method retrieves formulas by asking the query expression queryLit, and
     * returns the results, if any, in an ArrayList.
     *
     * @param queryLit
     *            The query, which is assumed to be an atomic literal consisting
     *            of a single predicate and its arguments. The arguments could
     *            be variables, constants, or a mix of the two, but only the
     *            first constant encountered in a left to right sweep over the
     *            literal will be used in the actual query.
     *
     * @return An ArrayList of Formula objects, or an empty ArrayList if no
     *         answers are retrieved.
     */
    public List<Formula> askWithLiteral(Formula queryLit) {

        List<String> input = queryLit.literalToArrayList();
        return askWithLiteral(input);
    }

    /*****************************************************************
     * This method retrieves the upward transitive closure of all Class names
     * contained in the input set. The members of the input set are not included
     * in the result set.
     *
     * @param classNames
     *            A Set object containing SUO-KIF class names (Strings).
     *
     * @return A Set of SUO-KIF class names, which could be empty.
     */
    public Set<String> getAllSuperClasses(Set<String> classNames) {

        Set<String> ans = new HashSet<>();
        for (String term : classNames) {
            ans.addAll(kbCache.getParentClasses(term));
        }
        return ans;
    }

    /*****************************************************************
     * This method retrieves all instances of the classes named in the input set.
     * TODO: Deprecated since it seems to do the opposite of what it should.
     * @param classNames
     *            A Set of String, containing SUO-KIF class names
     * @return A TreeSet, possibly empty, containing SUO-KIF constant names.
     */
    @Deprecated
    protected Set<String> getAllInstances(Set<String> classNames) {

        Set<String> ans = new TreeSet<>();
        if ((classNames instanceof TreeSet) && !classNames.isEmpty()) {
            for (String nme : classNames)
                ans.addAll(kbCache.getParentClassesOfInstance(nme));
        }
        return ans;
    }

    /*****************************************************************
     * This method retrieves all instances of the class named in the input String.
     *
     * @param className
     *            The name of a SUO-KIF Class.
     * @return A TreeSet, possibly empty, containing SUO-KIF constant names.
     */
    public Set<String> getAllInstances(String className) {

        if (StringUtil.isNonEmptyString(className)) {
            Set<String> input = new TreeSet<>();
            input.add(className);
            return getAllInstances(input);
        }
        return new TreeSet<>();
    }

    /*****************************************************************
     * This method tries to find or compute a valence for the input relation.
     *
     * @param relnName
     *            A String, the name of a SUO-KIF Relation.
     * @return An int value. -1 means that no valence value could be found. 0
     *         means that the relation is a VariableArityRelation. 1-5 are the
     *         standard SUO-KIF valence values.
     */
    public int getValence(String relnName) {

        if (kbCache.valences.get(relnName) == null) {
            if (Formula.isLogicalOperator(relnName)) // logical operator arity
                                                        // is checked in
                                                        // KIF.parse()
                return -1;
            System.err.println("Error in KB.getValence(): No valence found for " + relnName);
            return -1;
        }
        else
            return kbCache.valences.get(relnName);
    }

    /*****************************************************************
     *
     * @return an ArrayList containing all predicates in this KB.
     */
    public List<String> collectPredicates() {

        return new ArrayList<>(kbCache.instanceOf.get("Predicate"));
    }

    /*****************************************************************
     *
     * @param obj
     *            Any object
     *
     * @return true if obj is a String representation of a LISP empty list, else
     *         false.
     */
    public static boolean isEmptyList(Object obj) {
        return (StringUtil.isNonEmptyString(obj) && Formula.empty((String) obj));
    }

    /*****************************************************************
     * A static utility method.
     *
     * @param obj
     *            Presumably, a String.
     * @return true if obj is a SUO-KIF variable, else false.
     */
    public static boolean isVariable(String obj) {

        if (StringUtil.isNonEmptyString(obj)) {
            return (obj.startsWith("?") || obj.startsWith("@"));
        }
        return false;
    }

    /*****************************************************************
     * A static utility method.
     *
     * @param obj
     *            A String.
     * @return true if obj is a SUO-KIF logical quantifier, else false.
     */
    public static boolean isQuantifier(String obj) {

        return (StringUtil.isNonEmptyString(obj) && (obj.equals("forall") || obj.equals("exists")));
    }

    /*****************************************************************
     * A static utility method.
     *
     * @param obj
     *            Presumably, a String.
     * @return true if obj is a SUO-KIF commutative logical operator, else
     *         false.
     */
    public static boolean isCommutative(String obj) {

        return (StringUtil.isNonEmptyString(obj) && (obj.equals("and") || obj.equals("or")));
    }

    /***************************************************************
     * Hyperlink "[from Wikipedia]" if it occurs
     */
    public String formatWikipedia(String documentation) {

        if (!documentation.contains("[from Wikipedia]"))
            return documentation;
        int space1 = documentation.indexOf(" ");
        int space2 = documentation.indexOf(" ",space1);
        String term = documentation.substring(space1+1,space2);
        return documentation.replace("[from Wikipedia]","[<a href=\"https://en.wikipedia.org/wiki/" + term +
                "\">from Wikipedia]</a>");
    }

    /***************************************************************
     * Hyperlink terms identified with '&%' to the URL that brings up that term in the
     * browser. Handle (and ignore) suffixes on the term. For example
     * "&%Processes" would get properly linked to the term "Process", if present
     * in the knowledge base.
     */
    public String formatDocumentation(String href, String documentation, String language) {

        String formatted = documentation;
        if (StringUtil.isNonEmptyString(formatted)) {
            boolean isStaticFile = false;
            StringBuilder sb = new StringBuilder(formatted);
            String suffix = "";
            if (StringUtil.emptyString(href)) {
                href = "";
                suffix = ".html";
                isStaticFile = true;
            }
            else if (!href.endsWith("&term="))
                href += "&term=";
            int i;
            int j;
            int start = 0;
            String term = "";
            String formToPrint;
            StringBuilder hsb;
            while ((start < sb.length()) && ((i = sb.indexOf("&%", start)) != -1)) {
                sb.delete(i, (i + 2));
                j = i;
                while ((j < sb.length()) && !Character.isWhitespace(sb.charAt(j)) && sb.charAt(j) != '"')
                    j++;
                while (j > i) {
                    term = sb.substring(i, j);
                    if (containsTerm(term))
                        break;
                    j--;
                }
                if (j > i) {
                    // formToPrint =
                    // DocGen.getInstance(this.name).showTermName(this,term,language);
                    formToPrint = term;
                    hsb = new StringBuilder("<a href=\"");
                    hsb.append(href);
                    hsb.append(isStaticFile ? StringUtil.toSafeNamespaceDelimiter(term) : term);
                    hsb.append(suffix);
                    hsb.append("\">");
                    hsb.append(formToPrint);
                    hsb.append("</a>");
                    sb.replace(i, j, hsb.toString());
                    start = (i + hsb.length());
                }
            }
            formatted = sb.toString();
            //formatWikipedia(formatted);
        }
        return formatted;
    }

    /***************************************************************
     * Hyperlink terms identified with '&%' to the URL that brings up that term in the
     * ba static file Handle (and ignore) suffixes on the term. For example
     * "&%Processes" would get properly linked to the term "Process", if present
     * in the knowledge base.
     */
    public String formatStaticDocumentation(String documentation, String language, boolean onePage) {

        String formatted = documentation;
        if (StringUtil.isNonEmptyString(formatted)) {
            StringBuilder sb = new StringBuilder(formatted);
            int i, j, start = 0;
            String term = "";
            StringBuilder hsb;
            while ((start < sb.length()) && ((i = sb.indexOf("&%", start)) != -1)) {
                sb.delete(i, (i + 2));
                j = i;
                while ((j < sb.length()) && !Character.isWhitespace(sb.charAt(j)) && sb.charAt(j) != '"')
                    j++;
                while (j > i) {
                    term = sb.substring(i, j);
                    if (containsTerm(term))
                        break;
                    j--;
                }
                if (j > i) {
                    hsb = new StringBuilder("<a href=\"");
                    if (!onePage)
                        hsb.append(term.charAt(0) );
                    hsb.append("dict.html#");
                    hsb.append(StringUtil.toSafeNamespaceDelimiter(term));
                    hsb.append("\">").append(term).append("</a>");
                    sb.replace(i, j, hsb.toString());
                    start = (i + hsb.length());
                }
            }
            formatted = sb.toString();
        }
        return formatted;
    }

    /***************************************************************
     * Save the contents of the current KB to a file.
     */
    public String writeInferenceEngineFormulas(Set<String> forms) {

        String filename = null;
        try {
            String inferenceEngine = KBmanager.getMgr().getPref("eprover");
            if (StringUtil.isNonEmptyString(inferenceEngine)) {
                File executable = new File(inferenceEngine);
                if (executable.exists()) {
                    File dir = executable.getParentFile();
                    File file = new File(dir, (this.name + "-v.kif"));
                    filename = file.getCanonicalPath();
                    try (FileWriter fw = new FileWriter(filename); PrintWriter pw = new PrintWriter(fw)) {
                        Iterator<String> it = forms.iterator();
                        while (it.hasNext()) {
                            pw.println(it.next());
                            pw.println();
                        }
                    }
                } else {
                    System.err.println("Error in KB.writeInferenceEngineFormulas(): no executable " + inferenceEngine);
                }
            }
        } catch (IOException ioe) {
            System.err.println("Error in KB.writeInferenceEngineFormulas(): writing file: " + filename);
            System.err.println(ioe.getMessage());
            ioe.printStackTrace();
        }
        return filename;
    }

    /***************************************************************
     * Creates InferenceEngine and loads all of the constituents into it.
     *
     * @param factory
     *            Factory object used to create new InferenceEngine.
     * @return InferenceEngine object with all constituents loaded.
     */
    public InferenceEngine createInferenceEngine(InferenceEngine.EngineFactory factory) {

        InferenceEngine res = null;
        try {
            if (!formulaMap.isEmpty()) {
                Set<String> forms = preProcess((HashSet<String>) formulaMap.keySet());
                String filename = writeInferenceEngineFormulas(forms);
                boolean vFileSaved = !StringUtil.emptyString(filename);
                if (!vFileSaved)
                    System.err.println("Error in KB.createInterenceEngine(): new -v.kif file not written");
                if (vFileSaved && !factory.getClass().getName().equals("com.articulate.sigma.STP$STPEngineFactory"))
                    res = factory.createFromKBFile(filename);
                if (factory.getClass().getName().equals("com.articulate.sigma.STP$STPEngineFactory"))
                    res = factory.createWithFormulas(forms);
            }
        }
        catch (Exception e) {
            System.err.println("Error in KB.createInterenceEngine():" + e.getMessage());
            e.printStackTrace();
        }
        return res;
    }

    /***************************************************************
     * Checks for a Vampire executable, preprocesses all of the constituents
     */
    public void loadVampire() {

        System.out.println("INFO in KB.loadVampire()");
        String vampex = KBmanager.getMgr().getPref("vampire");
        KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
        if (StringUtil.emptyString(vampex)) {
            System.err.println("Error in KB.loadVampire(): no executable string in preferences");
            return;
        }
        File executable = new File(vampex);
        if (!executable.exists()) {
            System.err.println("Error in KB.loadVampire(): no executable " + vampex);
            return;
        }
        String lang = "tff";
        if (SUMOKBtoTPTPKB.lang.equals("fof"))
            lang = "tptp";
        String infFilename = KBmanager.getMgr().getPref("kbDir") + File.separator + this.name + "." + lang;
        String fileWritten = null;
        if (!(new File(infFilename).exists()) || KBmanager.getMgr().infFileOld() || force) {
            System.out.println("INFO in KB.loadVampire(): generating " + lang + " file " + infFilename);
            try (PrintWriter pw = new PrintWriter(new FileWriter(infFilename))) {
                if (!formulaMap.isEmpty()) {
                    long millis = System.currentTimeMillis();
                    if (lang.equals("tptp")) {
                        SUMOKBtoTPTPKB skb = new SUMOKBtoTPTPKB();
                        skb.kb = this;
                        fileWritten = skb.writeFile(infFilename, null, false, pw);
                    }
                    else {
                        SUMOKBtoTFAKB stff = new SUMOKBtoTFAKB();
                        stff.kb = this;
                        SUMOtoTFAform.initOnce();
                        stff.writeSorts(pw);
                        fileWritten = stff.writeFile(infFilename,null,false, pw);
                        System.out.println("INFO in KB.loadVampire(): CWA: " + SUMOKBtoTPTPKB.CWA);
                        if (SUMOKBtoTPTPKB.CWA)
                            pw.println(StringUtil.arrayListToCRLFString(CWAUNA.run(this)));
                        stff.printTFFNumericConstants(pw);
                    }
                    System.out.println("INFO in KB.loadVampire(): write " + lang + ", in seconds: " + (System.currentTimeMillis() - millis) / 1000);
                }
            }
            catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
            if (StringUtil.isNonEmptyString(fileWritten))
                System.out.println("File written: " + infFilename);
            else
                System.err.println("Could not write: " + infFilename);
        }
    }

    /***************************************************************
     * Checks for a Leo executable, preprocesses all of the constituents
     */
    public void loadLeo() {

        System.out.println("INFO in KB.loadLeo()");
        String leoex = KBmanager.getMgr().getPref("leoExecutable");
        KBmanager.getMgr().prover = KBmanager.Prover.LEO;
        if (StringUtil.emptyString(leoex)) {
            System.err.println("Error in loadLeo: no executable string in preferences");
            return;
        }
        File executable = new File(leoex);
        if (!executable.exists()) {
            System.err.println("Error in loadLeo: no executable " + leoex);
            return;
        }
        String lang = "thf";
        String infFilename = KBmanager.getMgr().getPref("kbDir") + File.separator + this.name + "." + lang;
        if (!(new File(infFilename).exists()) || KBmanager.getMgr().infFileOld()) {
            System.out.println("INFO in KB.loadLeo(): no need to generate " + lang + "file " + infFilename);
        }
    }

    /***************************************************************
     * Starts EProver and collects, preprocesses and loads all of the constituents into
     * it.
     */
    public void loadEProver() {

        System.out.println("INFO in KB.loadEProver(): Creating new process");
        KBmanager mgr = KBmanager.getMgr();
        KBmanager.getMgr().prover = KBmanager.Prover.EPROVER;
        String lang = "tff";
        if (SUMOKBtoTPTPKB.lang.equals("fof"))
            lang = "tptp";
        String infFilename = KBmanager.getMgr().getPref("kbDir") + File.separator + this.name + "." + lang;
        try (PrintWriter pw = new PrintWriter(new FileWriter(infFilename))) {
            if (!formulaMap.isEmpty()) {
//                HashSet<String> formulaStrings = new HashSet<String>();
//                formulaStrings.addAll(formulaMap.keySet());
                if (eprover != null) {
                    System.out.println("INFO in KB.loadEProver(): terminating old process first");
                    eprover.terminate();
                }
                eprover = null;
                SUMOKBtoTPTPKB skb = new SUMOKBtoTPTPKB();
                skb.kb = this;
                String tptpFilename = KBmanager.getMgr().getPref("kbDir") + File.separator + this.name + "" +
                        "" +
                        ".tptp";
                if (!(new File(tptpFilename).exists()) || KBmanager.getMgr().infFileOld()) {
                    System.out.println("INFO in KB.loadEProver(): generating TPTP file");
                    skb.writeFile(tptpFilename,null, false,pw);
                }
                if (StringUtil.isNonEmptyString(mgr.getPref("eprover")))
                    eprover = new EProver(mgr.getPref("eprover"), tptpFilename);
            }
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        if (eprover == null) {
            mgr.setError(mgr.getError() + "\n<br/>No local inference engine is available\n<br/>");
            System.err.println("Error in KB.loadEProver(): EProver not loaded");
        }
    }

    /*****************************************************************
     * Preprocess the knowledge base to TPTP. This includes "holds" prefixing,
     * ticking nested formulas, expanding row variables, and translating
     * mathematical relation operators. All the real work is done in
     * Formula.preProcess().
     *
     * @return a TreeSet of Strings.
     */
    public Set<String> preProcess(Set<String> forms) {

        System.out.println("INFO in KB.preProcess(): ");
        long millis = System.currentTimeMillis();
        Set<String> newTreeSet = new TreeSet<>();
        KBmanager mgr = KBmanager.getMgr();
        boolean tptpParseP = mgr.getPref("TPTP").equalsIgnoreCase("yes");
        kbCache.kb = this;
        kbCache.buildCaches();
        if (!tptpParseP)
            return newTreeSet;
        Iterator<String> it = forms.iterator();
        int counter = 0;
        FormulaPreprocessor fp = new FormulaPreprocessor();
        String form;
        Formula f;
        Set<Formula> processed;
        Set<String> tptp;
        while (it.hasNext()) {
            form = it.next();
            if ((counter++ % 100) == 1)
                System.out.print(".");
            if ((counter % 4000) == 1)
                System.out.println("\nINFO in KB.preProcess(): : still working");
            f = formulaMap.get(form);
            if (f == null) {
                String warn = "Warning in KB.preProcess(): No formula for : " + form;
                System.out.println(warn);
                warnings.add(warn);
                continue;
            }
            if (debug) System.out.println("INFO in KB.preProcess(): form : " + form);
            if (debug) System.out.println("INFO in KB.preProcess(): f : " + f);
            processed = fp.preProcess(f, false, this); // not queries
            tptp = new HashSet<>();
            if (tptpParseP) {
                for (Formula pform : processed) {
                    tptp.add(SUMOformulaToTPTPformula.tptpParseSUOKIFString(pform.getFormula(), false)); // not a query
                    errors.addAll(pform.getErrors());
                }
            }
            for (String p : tptp) {
                if (StringUtil.isNonEmptyString(p)) {
                    newTreeSet.add(p);
                }
                else {
                    String warn = "Warning in KB.preProcess(): empty formula: " + p;
                    System.out.println(warn);
                    warnings.add(warn);
                }
            }
        }
        System.out.println();
        // kbCache.clearSortalTypeCache();
        System.out.println("INFO in KB.preProcess(): completed in " +
                (System.currentTimeMillis() - millis) / 1000 + " seconds");
        return newTreeSet;
    }

    /*****************************************************************
     * @return a defensive copy of loadFormatMapsAttempted.
     */
    public List<String> getLoadFormatMapsAttempted() {

        return Lists.newArrayList(loadFormatMapsAttempted);
    }

    /*****************************************************************
     */
    public List<Pair> getSortedTermFrequency() {

        List<Pair> termFrequencies = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
            termFrequencies.add(new Pair(entry.getValue(), entry.getKey()));
        }
        Collections.sort(termFrequencies, Collections.reverseOrder());
        return termFrequencies;
    }

    /*****************************************************************
     */
    public TPTP3ProofProcessor runProver(String[] args, int timeout) {

        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        if (KBmanager.getMgr().prover == KBmanager.Prover.EPROVER) {
            loadEProver();
            EProver ep = askEProver(args[1], timeout, 1);
            System.out.println("KB.main(): completed Eprover query with result: " + StringUtil.arrayListToCRLFString(ep.output));
            tpp = new TPTP3ProofProcessor();
            tpp.parseProofOutput(ep.output, args[1], this, ep.qlist);
        }
        else if (KBmanager.getMgr().prover == KBmanager.Prover.VAMPIRE) {
            loadVampire();
            Vampire vamp = askVampire(args[1], timeout, 1);
            System.out.println("KB.main(): completed Vampire query with result: " + StringUtil.arrayListToCRLFString(vamp.output));
            tpp = new TPTP3ProofProcessor();
            tpp.parseProofOutput(vamp.output, args[1], this, vamp.qlist);
        }
        return tpp;
    }

    /*****************************************************************
     * Keep a count of axioms
     */
    public static void addToAxiomCount(Map<String,Integer> currentCount,
            Set<String> newAxioms) {

        Integer i;
        for (String s : newAxioms) {
            i = 0;
            if (currentCount.keySet().contains(s))
                i = currentCount.get(s);
            currentCount.put(s,i+1);
        }
    }

    /*****************************************************************
     * add to term format map
     * HashMap<String, HashMap<String, String>>();
     */
    public void addTermFormat(String lang, String term, String format) {

        Map<String, String> forLang;
        if (termFormatMap.containsKey(lang))
            forLang = termFormatMap.get(lang);
        else {
            forLang = new HashMap<>();
            termFormatMap.put(lang, forLang);
        }
        forLang.put(term,format);
    }

    /*****************************************************************
     * Attempt to provide guidance on the likely cause of a contradiction
     * by removing the axioms involved in a contradiction one-by-one and trying
     * again. @see contradictionHelp()
     */
    public static Map<String,Formula> collectSourceAxioms(KB kb, TPTP3ProofProcessor tpp) {

        Map<String,Formula> sourceAxioms = new HashMap<>();
        Formula f;
        for (TPTPFormula ps : tpp.proof) {
            System.out.println("KB.collectSourceAxioms(): " + ps.infRule);
            if (ps.infRule.startsWith("kb_") || ps.infRule.contains("conjecture")) {
                f = SUMOKBtoTPTPKB.axiomKey.get(ps.infRule);
                if (f != null && f.sourceFile != null && !f.sourceFile.endsWith(_cacheFileSuffix))
                    sourceAxioms.put(f.getFormula(),f);
            }
        }
        return sourceAxioms;
    }

    /*****************************************************************
     */
    private static void deletedOldInfFiles(String filename, String prefix) {

        System.out.println("KB.deletedOldInfFiles(): deleting old inference files");
        FileUtil.delete(filename);
        FileUtil.delete(prefix + "test.tptp");
        FileUtil.delete(prefix + "temp-comb.tptp");
        FileUtil.delete(prefix + "tempt-stmt.tptp");
    }

    /*****************************************************************
     * Attempt to provide guidance on the likely cause of a contradiction
     * by removing the axioms involved in a contradiction one-by-one and trying
     * again.
     */
    public static void contradictionHelp(KB kb, String[] args, int timeout) {

        Set<String> commonAxioms = new HashSet<>(); // axioms found in all contradictions
        Map<String,Integer> axiomCount= new HashMap<>(); // count axioms found in contradictions
        Set<Formula> removalSuccess = new HashSet<>(); // removing this axiom results in no contradiction
        TPTP3ProofProcessor tpp = kb.runProver(args,timeout);
        tpp.printProof(3);
        System.out.println();
        KBmanager.getMgr().removeKB(kb.name);
        String prefix = KBmanager.getMgr().getPref("kbDir") + File.separator;
        String filename = prefix + "SUMO_contra.kif";
        System.out.println("KB.contradictionHelp(): prefix: " + prefix);

        Map<String,Formula> sourceAxioms = collectSourceAxioms(kb,tpp);
        System.out.println("KB.contradictionHelp(): source axioms: " + sourceAxioms.keySet());
        commonAxioms.addAll(sourceAxioms.keySet());
        addToAxiomCount(axiomCount,sourceAxioms.keySet());
        Set<String> minusAxioms;
        List<String> display, display2, constituents;
        KB kb2;
        TPTP3ProofProcessor tpp2;
        Map<String, Formula> sourceAxioms2;
        for (String s : sourceAxioms.keySet()) {
            minusAxioms = new HashSet<>();
            minusAxioms.addAll(kb.getFormulas());
            minusAxioms.remove(s);
            System.out.println("KB.contradictionHelp(): removed axiom: " + s);
            display = new ArrayList<>();
            display.addAll(minusAxioms);
            display2 = new ArrayList<>();
            display2.addAll(display.subList(0,10));
            System.out.println("KB.contradictionHelp(): minusAxioms: " +
                    StringUtil.arrayListToCRLFString(display2) + "...");

            deletedOldInfFiles(filename,prefix);
            FileUtil.writeLines(filename, minusAxioms);
            constituents = new ArrayList<>();
            constituents.add(filename);
            KBmanager.getMgr().loadKB("test",constituents);
            kb2 = KBmanager.getMgr().getKB("test");
            tpp2 = kb2.runProver(args,timeout);
            if (!tpp2.noConjecture || tpp2.status.contains("GaveUp"))
                removalSuccess.add(kb.formulaMap.get(s));
            else {
                //System.out.println("KB.contradictionHelp(): axiomKey: " + SUMOKBtoTPTPKB.axiomKey);
                System.out.println("KB.contradictionHelp(): proof: ");
                tpp2.printProof(3);
                System.out.println();
                sourceAxioms2 = collectSourceAxioms(kb2, tpp2);
                addToAxiomCount(axiomCount, sourceAxioms2.keySet());
                commonAxioms.retainAll(sourceAxioms2.keySet());
            }
        }
        System.out.println("KB.contradictionHelp(): common axioms: " + commonAxioms);
        sourceAxioms.keySet().removeAll(commonAxioms);
        System.out.println("KB.contradictionHelp(): axiomCount: " + axiomCount);
        System.out.println("KB.contradictionHelp(): axioms not causing the contradiction: " +
                sourceAxioms.keySet());
        System.out.println("KB.contradictionHelp(): axioms that when any one is removed results in no contradiction: " +
                FormulaUtil.formatCollection(removalSuccess));
    }

    /***************************************************************
     */
    public static void test() {

        // generateTPTPTestAssertions();
        // testTPTP(args);
        KB kb;
        try {
            KBmanager.getMgr().initializeOnce();
            kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            System.out.println("KB.test(): " + kb.getAllSub("ColorAttribute","subAttribute"));

            String contents = "(subclass ?X Entity)";
            System.out.println("KB.test(): query Vampire with: " + contents);
            String dir = KBmanager.getMgr().getPref("kbDir") + File.separator;
            String type = "tptp";
            String outfile = dir + "temp-comb." + type;
            System.out.println("KB.test(): query Vampire on file: " + outfile);
            Vampire vamp = kb.askVampire(contents,30,1);
            //System.out.println("KB.test(): completed query with result: " + StringUtil.arrayListToCRLFString(vamp.output));
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            tpp.parseProofOutput(vamp.output,contents,kb,vamp.qlist);
            System.out.println("queryExp(): bindings: " + tpp.bindings);
            System.out.println("queryExp(): proof: " + tpp.proof);
            List<String> proofStepsStr = new ArrayList<>();
            for (TPTPFormula ps : tpp.proof)
                proofStepsStr.add(ps.toString());
            //kb.writeTerms();
            // System.out.println("KB.main(): " + kb.isChildOf("Africa",
            // "Region"));
            // kb.askEProver("(subclass ?X Object)",30,1);
        }
        catch (Exception ioe) {
            System.err.println(ioe.getMessage());
        }

        // kb.generateSemanticNetwork();
        // kb.generateRandomProof();
        // kb.instanceOfInstanceP();
        /*
        System.out.println("KB.main(): termDepth of Object: " + kb.termDepth("Object"));
        System.out.println("KB.main(): termDepth of Table: " + kb.termDepth("Table"));
        System.out.println("KB.main(): termDepth of immediateSubclass: " + kb.termDepth("immediateSubclass"));
        System.out.println("KB.main(): termDepth of Wagon: " + kb.termDepth("Wagon"));
        System.out.println("KB.main(): termDepth of Foo: " + kb.termDepth("Foo"));
*/
        /*
         * String foo = "(rel bar \"test\")"; Formula f = new Formula();
         * f.read(foo); System.out.println(f.getArgument(2).equals("\"test\""));
         */
    }

    /** ***************************************************************
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(" : ");
        if (formulaMap != null)
            sb.append(formulaMap.keySet().size()).append(" formulas");
        return sb.toString();
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("KB class");
        System.out.println("  options (with a leading '-'):");
        System.out.println("  h - show this help screen");
        System.out.println("  t - run test");
        System.out.println("  a \"<query>\"- ask query");
        System.out.println("  l - load KB files");
        System.out.println("  v - ask query of Vampire");
        System.out.println("  e - ask query of EProver");
        System.out.println("  L - ask query of LEO-IIIr");
        System.out.println("  1 - show full proof");
        System.out.println("  2 - remove single premise proof steps");
        System.out.println("  3 - show only KB axioms in proof");
        System.out.println("  x - contradiction help");
        System.out.println("  p - display TPTP proof");
        System.out.println("  f - use TFF language");
        System.out.println("  r - use (regular) FOF language");
        System.out.println("  o <seconds> - set the query timeout");
        System.out.println("  c <term1> <term2> - compare term depth");
        System.out.println("  s - show statistics");
        System.out.println("  R - rapid parsing of KB to TPTP");
    }

    /** ***************************************************************
     */
    public static void main(String[] args) throws IOException {

        System.out.println("INFO in KB.main()");
        if (args != null && args.length > 0 && args[0].equals("-h"))
            showHelp();
        else {

            // Check for "R" before initializing the KBmanager
            if (args != null && args.length > 1 && args[0].contains("R") || args[1].contains("R"))
                SUMOKBtoTPTPKB.rapidParsing = true;

            System.out.println("KB.main(): SUMOKBtoTPTPKB.rapidParsing==" + SUMOKBtoTPTPKB.rapidParsing);

            //KBmanager.prefOverride.put("loadLexicons","false");
            //System.out.println("KB.main(): Note! Not loading lexicons.");
            KBmanager.getMgr().initializeOnce();
            String kbName = KBmanager.getMgr().getPref("sumokbname");
            KB kb = KBmanager.getMgr().getKB(kbName);
            if (args != null)
                System.out.println("KB.main(): args[0]: " + args[0]);
            if (args != null && args.length > 2 && args[0].contains("c")) {
                if (!kb.containsTerm(args[1]))
                    System.err.println("Error in KB.main() no such term: " + args[1]);
                if (!kb.containsTerm(args[2]))
                    System.err.println("Error in KB.main() no such term: " + args[2]);
                int eqrel = kb.compareTermDepth(args[1], args[2]);
                String eqText = KButilities.eqNum2Text(eqrel);
                System.out.println("KB.main() term depth of " + args[1] + " : " + kb.termDepth(args[1]));
                System.out.println("KB.main() term depth of " + args[2] + " : " + kb.termDepth(args[2]));
                System.out.println("KB.main() eqrel " + eqrel);
                System.out.println("KB.main() " + args[1] + " " + eqText + " " + args[2]);
            }
            if (args != null && args.length > 0 && args[0].contains("t"))
                test();
            if (args != null && args.length > 1 && args[0].contains("v")) {
                KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
            }
            if (args != null && args.length > 1 && args[0].contains("e")) {
                KBmanager.getMgr().prover = KBmanager.Prover.EPROVER;
            }
            if (args != null && args.length > 1 && args[0].contains("L")) {
                KBmanager.getMgr().prover = KBmanager.Prover.LEO;
            }
            if (args != null && args.length > 0 && args[0].contains("l")) {
                System.out.println("KB.main(): Normal completion");
            }
            if (args != null && args.length > 0 && args[0].contains("f")) {
                System.out.println("KB.main(): set to TFF language");
                SUMOformulaToTPTPformula.lang = "tff";
                SUMOKBtoTPTPKB.lang = "tff";
            }
            if (args != null && args.length > 0 && args[0].contains("r")) {
                System.out.println("KB.main(): set to FOF language");
                SUMOformulaToTPTPformula.lang = "fof";
                SUMOKBtoTPTPKB.lang = "fof";
            }
            if (args != null && args.length > 0 && args[0].contains("s")) {
                System.out.println("KB.main(): show statistics");
                System.out.println(HTMLformatter.showStatistics(kb));
            }
            int timeout = 30;
            if (args != null && args.length > 2 && args[0].contains("o")) {
                try {
                    timeout = Integer.parseInt(args[1]);
                }
                catch(NumberFormatException nfe) {
                    timeout = Integer.parseInt(args[2]);
                }
                System.out.println("KB.main(): set timeout to: " + timeout);
            }
            if (args != null && args.length > 1 && args[0].contains("a")) {
                TPTP3ProofProcessor tpp = null;
                if (args[0].contains("p"))
                    TPTP3ProofProcessor.tptpProof = true;
                if (args[0].contains("x")) {
                    contradictionHelp(kb,args,timeout);
                }
                else if (KBmanager.getMgr().prover == KBmanager.Prover.EPROVER) {
                    kb.loadEProver();
                    EProver eprover = kb.askEProver(args[1], timeout, 1);
                    System.out.println("KB.main(): completed Eprover query with result: " + StringUtil.arrayListToCRLFString(eprover.output));
                    tpp = new TPTP3ProofProcessor();
                    tpp.parseProofOutput(eprover.output, args[1], kb, eprover.qlist);
                }
                else if (KBmanager.getMgr().prover == KBmanager.Prover.VAMPIRE) {
                    kb.loadVampire();
                    Vampire vamp = kb.askVampire(args[1], timeout, 1);
                    System.out.println("KB.main(): completed Vampire query with result: " + StringUtil.arrayListToCRLFString(vamp.output));
                    tpp = new TPTP3ProofProcessor();
                    tpp.parseProofOutput(vamp.output, args[1], kb, vamp.qlist);
                }
                else if (KBmanager.getMgr().prover == KBmanager.Prover.LEO) {
                    LEO leo = kb.askLeo(args[1], timeout, 1);
                    System.out.println("KB.main(): completed LEO query with result: " + StringUtil.arrayListToCRLFString(leo.output));
                    tpp = new TPTP3ProofProcessor();
                    tpp.parseProofOutput(leo.output, args[1], kb, leo.qlist);
                }
                if (tpp != null)
                    tpp.createProofDotGraph();
                if (!args[0].contains("x")) {
                    System.out.println("KB.main(): binding map: " + tpp.bindingMap);
                    int level = 1;
                    if (args[0].contains("2") || args[0].contains("3") ) {
                        if (args[0].contains("2"))
                            level = 2;
                        if (args[0].contains("3"))
                            level = 3;
                    }
                    System.out.println("KB.main(): proof with level " + level);
                    System.out.println("KB.main(): axiom key size " + SUMOKBtoTPTPKB.axiomKey.size());
                    tpp.printProof(level);
                }
            }
        }
    }
}
