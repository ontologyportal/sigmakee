/** This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the termsof the GNU
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
*/

/*************************************************************************************************/
package com.articulate.sigma;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.regex.*;
import com.articulate.sigma.InferenceEngine;

/** *****************************************************************
 *  Contains methods for reading, writing knowledge bases and their
 *  configurations.  Also contains the inference engine process for
 *  the knowledge base.
 */
public class KB {

    private boolean isVisible = true;
    
    /** The inference engine process for this KB. */
    public EProver eprover;

    /** The name of the knowledge base. */
    public String name;

    /** An ArrayList of Strings that are the full
     * canonical pathnames of the files that comprise the KB. */
    public ArrayList<String> constituents = new ArrayList<String>();

    /** The natural language in which axiom paraphrases should be presented. */
    public String language = "EnglishLanguage";

    /** The location of preprocessed KIF files, suitable for loading into EProver. */
    public String kbDir = null;

    /** The instance of the CELT process. */
    public CELT celt = null;

    /** A synchronized SortedSet of Strings, which are all the terms in the KB. */
    public SortedSet<String> terms = Collections.synchronizedSortedSet(new TreeSet<String>());

    /** The String constant that is the suffix for files of user assertions. */
    public static final String _userAssertionsString = "_UserAssertions.kif";

    /** The String constant that is the suffix for files of cached assertions. */
    public static final String _cacheFileSuffix      = "_Cache.kif";

    /** A Map of all the Formula objects in the KB.  Each key is a
     * String representation of a Formula.  Each value is the Formula
     * object corresponding to the key. */
    public HashMap<String, Formula> formulaMap = new HashMap<String, Formula>();

    /** A HashMap of ArrayLists of String formulae, containing all the
     * formulae in the KB.  Keys are the formula itself, a formula ID, and term
     * indexes created in KIF.createKey().  The actual formula can be retrieved
     * by using the returned String as the key for the variable formulaMap */
    public HashMap<String, ArrayList<String>> formulas = new HashMap<String, ArrayList<String>>();

    /** The natural language formatting strings for relations in the
     *  KB. It is a HashMap of language keys and HashMap values.
     *  The interior HashMap is term name keys and String values. */
    private HashMap<String,HashMap<String,String>> formatMap = new HashMap<String,HashMap<String,String>>();

    /** The natural language strings for terms in the KB. It is a
     *  HashMap of language keys and HashMap values. The interior
     *  HashMap is term name keys and String values. */
    private HashMap<String,HashMap<String,String>> termFormatMap = new HashMap<String,HashMap<String,String>>();

    /** Errors and warnings found during loading of the KB constituents. */
    public TreeSet<String> errors = new TreeSet<String>();

    /** Future: If true, the contents of the KB have been modified without updating the caches */
    public boolean modifiedContents = false;

    /** If true, assertions of the form (predicate x x) will be
     * included in the relation cache tables. */
    private boolean cacheReflexiveAssertions = false;

    public KBcache kbCache = null;
    
    /** *************************************************************
     * Constructor which takes the name of the KB and the location
     * where KBs preprocessed for EProver should be placed.
     */
    public KB(String n, String dir) {
        
        name = n;
        kbDir = dir;
        try {
            KBmanager mgr = KBmanager.getMgr();
            if (mgr != null) {
                String loadCelt = mgr.getPref("loadCELT");
                if ((loadCelt != null) && loadCelt.equalsIgnoreCase("yes")) 
                    celt = new CELT();                
            }            
        }
        catch (IOException ioe) {
            System.out.println("Error in KB(): " + ioe.getMessage());
            celt = null;
        }                
    }
    
    public KB(String n, String dir, boolean visibility) {
    	
        this(n, dir);
        isVisible = visibility;        
    }
    
    public boolean isVisible() {
        return isVisible;
    }
    
    /** *************************************************************
     * Constructor
     */
    public KB(String n) {

        name = n;
        try {
            KBmanager mgr = KBmanager.getMgr();
            kbDir = mgr.getPref("kbDir");
            if (mgr != null) {
                String loadCelt = mgr.getPref("loadCELT");
                if ((loadCelt != null) && loadCelt.equalsIgnoreCase("yes")) 
                    celt = new CELT();                
            }                       
        }
        catch (IOException ioe) {
        	System.out.println("Error in KB(): " + ioe.getMessage());
            celt = null;
        }
    }

    /** ************************************************************
     * Returns a SortedSet of Strings, which are all
     * the terms in the KB. */
    public SortedSet<String> getTerms() {
        
        return this.terms;
    }
    
    /** **************************************************
     * REswitch determines if a String is a RegEx or not based on its use of RE metacharacters. "1"=nonRE, "2"=RE
     * 
     * @param term A String
     * @return "1" or "2" 
     */
    public String REswitch(String term) {
        
        if (term.contains("(")||term.contains("[")||term.contains("{")||term.contains("\\")||term.contains("^")||term.contains("$")||
                term.contains("|")||term.contains("}")||term.contains("]")||term.contains(")")||term.contains("?")||term.contains("*")||
                term.contains("+")) 
            return "2";
        return "1";
    }
    
    /** *************************************************
     * Only called in BrowseBody.jsp when a single match is found. Purpose is to simplify a RegEx to its only matching term
     * 
     * @param term a String
     * @return modified term a String
     */
    public String simplifyTerm(String term) {
        
        if (getREMatch(term.intern()).size()==1) 
            return getREMatch(term.intern()).get(0);
        return term;
    }

    /** **************************************************
    * Takes a term (interpreted as a Regular Expression) and returns true
    * if any term in the KB has a match with the RE.
    *
    * @param term A String
    * @return true or false.
    */
    public boolean containsRE(String term) {
        
        return (getREMatch(term).size()>0 ? true : false);  
    }  

    /** **************************************************
    * Takes a term (interpreted as a Regular Expression) and returns an ArrayList
    * containing every term in the KB that has a match with the RE.
    *
    * @param term A String
    * @return An ArrayList of terms that have a match to term
    */
    public ArrayList<String> getREMatch(String term) {
        
        try {
            Pattern p = Pattern.compile(term);
            ArrayList<String> matchesList = new ArrayList<String>();
            Iterator<String> itr = getTerms().iterator();
            while(itr.hasNext()) {
                String t = itr.next();
                Matcher m = p.matcher(t);
                if (m.matches()) 
                    matchesList.add(t);
            }
            return matchesList;
        } 
        catch (PatternSyntaxException ex) {
            ArrayList<String> err = new ArrayList<String>();
            err.add("Invalid Input");
            return err;
        }
    }

    /** ************************************************************
     *  Sets the synchronized SortedSet of all the terms in the
     *  KB to be kbTerms. */
    public void setTerms(SortedSet<String> newTerms) {
        
        synchronized (getTerms()) {
            getTerms().clear();
            this.terms = Collections.synchronizedSortedSet(newTerms);
        }
        return;
    }

    /** *************************************************************
     * Get an ArrayList of Strings containing the language identifiers
     * of available natural language formatting templates.
     *
     * @return an ArrayList of Strings containing the language identifiers
     */
    public ArrayList<String> availableLanguages() {

        ArrayList<String> al = new ArrayList<String>();
        ArrayList<Formula> col = ask("arg", 0, "format");
        ArrayList<Formula> col2 = ask("arg", 0, "termFormat");
        if (col != null) {
            if (col2 != null)
                col.addAll(col2);
            for (int i = 0; i < col.size(); i++) {
                Formula f = (Formula) col.get(i);
                String lang = f.getArgument(1);
                if (!al.contains(lang.intern()))
                    al.add(lang.intern());
            }
        }
        al.addAll(OMWordnet.lnames);
        return al;
    }

    /** *************************************************************
     * Arity errors should already have been trapped in addConstituent()
     * unless a relation is used before it is defined.  This routine
     * is a comprehensive re-check.
     */
    public void checkArity() {
        
        ArrayList<String> toRemove = new ArrayList<String>();
        System.out.print("INFO in KB.checkArity(): Performing Arity Check");
        if (formulaMap != null && formulaMap.size() > 0) {
        	int counter = 0;
            Iterator<String> formulas = formulaMap.keySet().iterator();
            while (formulas.hasNext()) {
                Formula f = (Formula) formulaMap.get(formulas.next());
                if (counter == 100) {
                    System.out.print(".");
                    counter = 0;
                }
                String term = PredVarInst.hasCorrectArity(f,this);
                if (!StringUtil.emptyString(term)) {
                    errors.add("Formula in " + f.sourceFile + 
                            " rejected due to arity error of predicate " + term +
                            " in formula: \n" + f.theFormula);
                    toRemove.add(f.theFormula);
                }                
            }
            System.out.println();
        }
        //for (int i = 0; i < toRemove.size(); i++) 
        //    formulaMap.remove(toRemove.get(i));        
    }

    /** *************************************************************
     * Returns the type (SUO-KIF SetOrClass name) for any argument in
     * argPos position of an assertion formed with the SUO-KIF
     * Relation reln.  If no argument type value is directly stated
     * for reln, this method tries to find a value inherited from one
     * of reln's super-relations.
     *
     * @param reln A String denoting a SUO-KIF Relation     
     * @param argPos An int denoting an argument position, where 0 is
     * the position of reln itself     
     * @return A String denoting a SUO-KIF SetOrClass, or null if no
     * value can be obtained
     */
    public String getArgType(String reln, int argPos) {

        String className = null;
        String argType = FormulaPreprocessor.findType(argPos, reln, this);
        if (StringUtil.isNonEmptyString(argType)) {
            if (argType.endsWith("+"))
                argType = "SetOrClass";
            className = argType;
        }
        return className;
    }
    
    /** *************************************************************
     * Returns the type (SUO-KIF SetOrClass name) for any argument in
     * argPos position of an assertion formed with the SUO-KIF
     * Relation reln.  If no argument type value is directly stated
     * for reln, this method tries to find a value inherited from one
     * of reln's super-relations.
     *
     * @param reln A String denoting a SUO-KIF Relation
     *
     * @param argPos An int denoting an argument position, where 0 is
     * the position of reln itself
     *
     * @return A String denoting a SUO-KIF SetOrClass, or null if no
     * value can be obtained.  A '+' is appended to the class name
     * if the argument is a subclass of the class, rather than an instance
     */
    public String getArgTypeClass(String reln, int argPos) {

        String className = null;        
        String argType = FormulaPreprocessor.findType(argPos, reln, this);
        if (StringUtil.isNonEmptyString(argType))
            className = argType;
        return className;
    }

    /** *************************************************************
     * This list contains the names of SUMO Relations known to be
     * instances of VariableArityRelation in at least some domain.  It
     * is used only for TPTP generation, and should
     * <strong>not</strong> be relied upon for any other purpose,
     * since it is not automatically generated and might be out of
     * date.
     
    public static final List<String> VA_RELNS = Arrays.asList("AssignmentFn",
                                                      "GreatestCommonDivisorFn",
                                                      "LatitudeFn",
                                                      "LeastCommonMultipleFn",
                                                      "ListFn",
                                                      "LongitudeFn",
                                                      "contraryAttribute",
                                                      "disjointDecomposition",
                                                      "exhaustiveAttribute",
                                                      "exhaustiveDecomposition",
                                                      "partition",
                                                      "processList");
*/
    /** *************************************************************
     * Returns true if relnName is the name of a relation that is
     * known to be, or computed to be, a variable arity relation.
     *
     * @param relnName A String that names a SUMO Relation (Predicate
     * or Function).
     *
     * @return boolean
     
    public boolean isVariableArityRelation(String relnName) {
        
        boolean ans = false;
        ans = (VA_RELNS.contains(relnName)
               || (getValence(relnName) == 0)
               || isInstanceOf(relnName, "VariableArityRelation"));
        return ans;
    }
*/
    /** *************************************************************
     
    protected ArrayList listRelnsWithRelnArgs() {
        
        if (kbCache.relnsWithRelnArgs != null)
            return new ArrayList(kbCache.relnsWithRelnArgs.keySet());
        return null;
    }

    /** *************************************************************
     *
    protected boolean containsRelnWithRelnArg(String input) {
        
        if (StringUtil.isNonEmptyString(input)) {
            List relns = listRelnsWithRelnArgs();
            if (relns != null) {
                int len = relns.size();
                String reln = null;
                for (int i = 0 ; i < len ; i++) {
                    reln = (String) relns.get(i);
                    if (input.indexOf(reln) >= 0)
                        return true;
                }
            }
        }
        return false;
    }
*/
    /** *************************************************************
     * debugging utility
     
    private void printParents() {

        logger.finer("Printing parents");
        Iterator it = parents.keySet().iterator();
        while (it.hasNext()) {
            String parent = (String) it.next();
            logger.finer(parent + " " + 
                    (HashSet) parents.get(parent));
        }
        
    }
     */
    /** *************************************************************
     * debugging utility
     
    private void printChildren() {

        logger.finer("Printing children.");
        Iterator it = children.keySet().iterator();
        while (it.hasNext()) {
            String child = (String) it.next();
            logger.finer(child + " " +
                    (HashSet) children.get(child));
        }
    }
    */
    /** *************************************************************
     * debugging utility
     
    private void printDisjointness() {

        logger.finer("Printing disjoint.");
        Iterator it = disjoint.keySet().iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            logger.finer(term + " is disjoint with " + 
                    (Set) disjoint.get(term));
        }
    }
    */
    /** *************************************************************
     * Determine whether a particular term is an immediate instance,
     * which has a statement of the form (instance term otherTerm).
     * Note that this does not count for terms such as Attribute(s)
     * and Relation(s), which may be defined as subAttribute(s) or
     * subrelation(s) of another instance.  If the term is not an
     * instance, return an empty ArrayList.  Otherwise, return an
     * ArrayList of the Formula(s) in which the given term is
     * defined as an instance.
     */
    public ArrayList<Formula> instancesOf(String term) {

        return askWithRestriction(1,term,0,"instance");
    }

    /** *************************************************************
     * Returns true if i is an instance of c, else returns false.
     *
     * @param i A String denoting an instance.
     * @param c A String denoting a Class.
     * @return true or false.
     */
    public boolean isInstanceOf(String i, String c) {
        
        ArrayList<Formula> al = askWithRestriction(0,"instance",1,i);
        HashSet<String> classes = KBcache.collectArgFromFormulas(2,al);
        Iterator<String> it = classes.iterator();
        while (it.hasNext()) {
            String cl = it.next();
            HashSet<String> parents = kbCache.getParentClasses(cl);
            if (parents == null) {
                System.out.println("Error in KB.isInstanceOf(): no parents for " + cl);
            }
            else if (parents.contains(c))
                return true;
        }
        return false;
    }

    /** *************************************************************
     * Returns true if i is c, is an instance of c, or is subclass of c,
     * else returns false.
    *
    * @param i A String denoting an instance.
    * @param c A String denoting a Class.
    * @return true or false.
    */
   public boolean isChildOf(String i, String c) {

       return i.equals(c) || isInstanceOf(i,c) || isSubclass(i,c);
   }

    /** *************************************************************
     * Returns true if i is an instance of c in any loaded KB, else
     * returns false.
     *
     * @param i A String denoting an instance.
     * @param c A String denoting a Class.
     * @return true or false.
     */
    public static boolean isRelationInAnyKB(String i) {
        
        HashMap<String,KB> kbs = KBmanager.getMgr().kbs;
        if (!kbs.isEmpty()) {
            KB kb = null;
            Iterator<KB> it = kbs.values().iterator();
            while (it.hasNext()) {
                kb = it.next();
                if (kb.kbCache.relations.contains(i)) 
                    return true;
            }
        }
        return false;
    }

    /** *************************************************************
     */
    public boolean isInstance(String term) {

        ArrayList<Formula> al = askWithRestriction(0,"instance",1,term);
        return (al != null && al.size() > 0);
    }

    /** *************************************************************
     * Determine whether a particular class or instance "child" is a
     * child of the given "parent".
     *
     * @param child A String, the name of a term.
     * @param parent A String, the name of a term.
     * @return true if child and parent constitute an actual or
     * implied relation in the current KB, else false.
     */
    public boolean childOf(String child, String parent) {
        
        if (child.equals(parent))
            return true;
        if (kbCache.transInstOf(parent,child))
            return true;
        if (kbCache.childOfP("instance",parent,child) ||
            kbCache.childOfP("subclass",parent,child) || 
            kbCache.childOfP("subrelation",parent,child))
            return true;
        return false;
    }

    /** *************************************************************
     * Returns true if the subclass cache supports the conclusion that
     * c1 is a subclass of c2, else returns false.
     *
     * @param c1 A String, the name of a SetOrClass.
     * @param c2 A String, the name of a SetOrClass.
     * @return boolean
     */
    public boolean isSubclass(String c1, String c2) {
        
        if (StringUtil.isNonEmptyString(c1) && StringUtil.isNonEmptyString(c2)) 
            return kbCache.childOfP("subclass",c2,c1);
        return false;
    }
    
    /** *************************************************************
     * Converts all Formula objects in the input List to ArrayList
     * tuples.
     *
     * @param formulaList A list of Formulas.
     *
     * @return An ArrayList of formula tuples (ArrayLists), or an
     * empty ArrayList.
     */
    public static ArrayList<ArrayList<String>> formulasToArrayLists(List<Formula> formulaList) {

        ArrayList<ArrayList<String>> ans = new ArrayList<ArrayList<String>>();
        if (formulaList instanceof List) {
            Iterator<Formula> it = formulaList.iterator();
            Formula f = null;
            while (it.hasNext()) {
                f = (Formula) it.next();
                ans.add(f.literalToArrayList());
            }
        }
        return ans;
    }

    /** *************************************************************
     * Converts all Strings in the input List to Formula objects.
     *
     * @param strings A list of Strings.
     *
     * @return An ArrayList of Formulas, or an empty ArrayList.
     */
    public static ArrayList<Formula> stringsToFormulas(List<String> strings) {

        ArrayList<Formula> ans = new ArrayList<Formula>();
        if (strings instanceof List) {
            Iterator<String> it = strings.iterator();
            while (it.hasNext()) {
                Formula f = new Formula();
                f.read(it.next());
                ans.add(f);
            }
        }
        return ans;
    }

    /** *************************************************************
     * Converts a literal (List object) to a String.
     *
     * @param literal A List representing a SUO-KIF formula.     
     * @return A String representing a SUO-KIF formula.
     */
    public static String literalListToString(List<String> literal) {
        
        StringBuffer b = new StringBuffer();
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

    /** *************************************************************
     * Converts a literal (List object) to a Formula.
     *
     * @param literal A List representing a SUO-KIF formula.     
     * @return A SUO-KIF Formula object, or null if no Formula can be
     * created.
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

    /** *************************************************************
     * Returns an ArrayList containing the terms (Strings) that
     * correspond to targetArgnum in the Formulas obtained from the
     * method call askWithRestriction(argnum1, term1, argnum2, term2).
     *
     * @param predicatesUsed A Set to which will be added the
     *                       predicates of the ground assertions
     *                       actually used to gather the terms
     *                       returned
     *
     * @return An ArrayList of terms, or an empty ArrayList if no
     *         terms can be retrieved.
     */
    public ArrayList<String> getTermsViaAskWithRestriction(int argnum1,
                                                           String term1,
                                                           int argnum2,
                                                           String term2,
                                                           int targetArgnum,
                                                           Set<String> predicatesUsed) {

        ArrayList<String> result = new ArrayList<String>();
        if (StringUtil.isNonEmptyString(term1)
            && !StringUtil.isQuotedString(term1)
            && StringUtil.isNonEmptyString(term2)
            && !StringUtil.isQuotedString(term2)) {
            ArrayList<Formula> formulae = askWithRestriction(argnum1, term1, argnum2, term2);
            Formula f = null;
            Iterator<Formula> it = formulae.iterator();
            while (it.hasNext()) {
                f = it.next();
                result.add(f.getArgument(targetArgnum));
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

    /** *************************************************************
     * Returns an ArrayList containing the terms (Strings) that
     * correspond to targetArgnum in the Formulas obtained from the
     * method call askWithRestriction(argnum1, term1, argnum2, term2).
     *
     * @return An ArrayList of terms, or an empty ArrayList if no
     *         terms can be retrieved.
     */
    public ArrayList<String> getTermsViaAskWithRestriction(int argnum1,
                                                           String term1,
                                                           int argnum2,
                                                           String term2,
                                                           int targetArgnum) {
        
        return getTermsViaAskWithRestriction(argnum1, term1, argnum2,
                                             term2, targetArgnum, null);
    }

    /** *************************************************************
     * Returns the first term found that corresponds to targetArgnum
     * in the Formulas obtained from the method call
     * askWithRestriction(argnum1, term1, argnum2, term2).
     *
     * @return A SUO-KIF term (String), or null is no answer can be
     *         retrieved.
     */
    public String getFirstTermViaAskWithRestriction(int argnum1, String term1,
                                           int argnum2, String term2, int targetArgnum) {

        String result = null;
        ArrayList<String> terms = getTermsViaAskWithRestriction(argnum1, term1, argnum2,
                                                   term2, targetArgnum);
        if (!terms.isEmpty()) 
            result = (String) terms.get(0);            
        return result;
    }

    /** *************************************************************
     * @return an ArrayList of Formulas in which the two terms
     * provided appear in the indicated argument positions.  If there
     * are no Formula(s) matching the given terms and respective
     * argument positions, return an empty ArrayList.  Iterate
     * through the smallest list of results.
     */
    public ArrayList<Formula> askWithRestriction(int argnum1, String term1, int argnum2, String term2) {

       ArrayList<Formula> result = new ArrayList<Formula>();
       if (StringUtil.isNonEmptyString(term1) && StringUtil.isNonEmptyString(term2)) {
           ArrayList<Formula> partial1 = ask("arg", argnum1, term1);
           ArrayList<Formula> partial2 = ask("arg", argnum2, term2);
           //System.out.println("INFO in KB.askWithRestriction(): partial 2: " + partial2);
           ArrayList<Formula> partial = partial1;
           // partial.retainAll(partial2); - this should be faster than below
           int arg = argnum2;
           String term = term2;
           if (partial1.size() > partial2.size()) {
               partial = partial2;
               arg = argnum1;
               term = term1;
           }
           Formula f = null;
           int plen = partial.size();
           for (int i = 0; i < plen; i++) {
               f = (Formula) partial.get(i);
               if (f == null) 
                   System.out.println("Error in KB.askWithRestriction(): null formula searching on term: " + term);
               String thisArg = f.getArgument(arg);
               if (thisArg == null) {
                   System.out.println("Error in KB.askWithRestriction(): null argument: " + f);
               }
               else if (f.getArgument(arg).equals(term))
                   result.add(f);
           }
       }
       return result;
    }

    /** *************************************************************
     * Returns an ArrayList of Formulas in which the two terms
     * provided appear in the indicated argument positions.  If there
     * are no Formula(s) matching the given terms and respective
     * argument positions, return an empty ArrayList.
     *
     * @return ArrayList
     */
    public ArrayList<Formula> askWithTwoRestrictions(int argnum1, String term1,
                                            int argnum2, String term2,
                                            int argnum3, String term3) {

        String[] args = new String[6];
        args[0] = "argnum1 = " + argnum1;
        args[1] = "term1 = " + term1;
        args[0] = "argnum2 = " + argnum2;
        args[1] = "term2 = " + term2;
        args[0] = "argnum3 = " + argnum3;
        args[1] = "term3 = " + term3;
        
        ArrayList<Formula> result = new ArrayList<Formula>();
        if (StringUtil.isNonEmptyString(term1)
            && StringUtil.isNonEmptyString(term2)
            && StringUtil.isNonEmptyString(term3)) {
              // a will get the smallest list then b then c
            ArrayList<Formula> partiala = new ArrayList<Formula>();   
            ArrayList<Formula> partial1 = ask("arg",argnum1,term1);
            ArrayList<Formula> partial2 = ask("arg",argnum2,term2);
            ArrayList<Formula> partial3 = ask("arg",argnum3,term3);
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
                for (int i = 0; i < partiala.size(); i++) {
                    Formula f = partiala.get(i);
                    if (f.getArgument(argb).equals(termb)) {
                        if (f.getArgument(argc).equals(termc))
                            result.add(f);
                    }
                }
            }
        }
        return result;
    }

    /** *************************************************************
     * Returns an ArrayList containing the SUO-KIF terms that match
     * the request.
     *
     * @return An ArrayList of terms, or an empty ArrayList if no
     *         matches can be found.
     */
    public ArrayList<String> getTermsViaAWTR(int argnum1, String term1,
                                             int argnum2, String term2,
                                             int argnum3, String term3,
                                             int targetArgnum) {
        ArrayList<String> ans = new ArrayList<String>();
        List<Formula> formulae = askWithTwoRestrictions(argnum1, term1,
                                               argnum2, term2,
                                               argnum3, term3);
        Formula f = null;
        for (int i = 0; i < formulae.size(); i++) {
            f = formulae.get(i);
            ans.add(f.getArgument(targetArgnum));
        }
        return ans;
    }

    /** *************************************************************
     * Returns the first SUO-KIF terms that matches the request, or
     * null.
     *
     * @return A term (String), or null.
     */
    public String getFirstTermViaAWTR(int argnum1, String term1,
                                      int argnum2, String term2,
                                      int argnum3, String term3,
                                      int targetArgnum) {
        String ans = null;
        List<String> terms = getTermsViaAWTR(argnum1, term1,
                                             argnum2, term2,
                                             argnum3, term3,
                                             targetArgnum);
        if (!terms.isEmpty()) 
            ans = (String) terms.get(0);        
        return ans;
    }

    /** *************************************************************
     * Returns an ArrayList containing the terms (Strings) that
     * correspond to targetArgnum in the ground atomic Formulae in
     * which knownArg is in the argument position knownArgnum.  The
     * ArrayList returned will contain no duplicate terms.
     *
     * @param knownArgnum The argument position of knownArg
     *
     * @param knownArg The term that appears in the argument
     *                 knownArgnum of the ground atomic Formulae in
     *                 the KB
     *
     * @param targetArgnum The argument position of the terms being sought
     *
     * @return An ArrayList of Strings, which will be empty if no
     *         match found.
     */
    public ArrayList<String> getTermsViaAsk(int knownArgnum, String knownArg, int targetArgnum) {

        ArrayList<String> result = new ArrayList<String>();
        List<Formula> formulae = ask("arg", knownArgnum, knownArg);
        if (!formulae.isEmpty()) {
            TreeSet<String> ts = new TreeSet<String>();
            Formula f = null;
            Iterator<Formula> it = formulae.iterator(); 
            while (it.hasNext()) {
                f = it.next();
                ts.add(f.getArgument(targetArgnum));
            }
            result.addAll(ts);
        }
        return result;
    }

    /** *************************************************************
     */
    private ArrayList<Formula> stringsToFormulas(ArrayList<String> strings) {
    
        ArrayList<Formula> result = new ArrayList<Formula>();
        if (strings == null) return result;
        for (int i = 0; i < strings.size(); i++) {
        	String s = strings.get(i);
        	Formula f = formulaMap.get(s);
        	if (f != null)
        		result.add(f);
        	else
        		System.out.println("Error in KB.stringsToFormulas(): null formula for key: " + s);
        }
        return result;
    }
    
    /** *************************************************************
     * Returns an ArrayList containing the Formulas that match the
     * request.
     *
     * @param kind May be one of "ant", "cons", "stmt", or "arg"
     *             @see KIF.createKey()
     * @param term The term that appears in the statements being
     *             requested.
     * @param argnum The argument position of the term being asked
     *               for.  The first argument after the predicate
     *               is "1". This parameter is ignored if the kind
     *               is "ant", "cons" or "stmt".
     * @return An ArrayList of Formula(s), which will be empty if no
     *         match found.
     */
    public ArrayList<Formula> ask(String kind, int argnum, String term) {
        
        ArrayList<Formula> result = new ArrayList<Formula>();
        String msg = null;
        if (StringUtil.emptyString(term)) {
            msg = ("Error in KB.ask(\"" + kind + "\", " + argnum + ", \"" + term + "\"), "
                   + "search term is null, or an empty string");
            errors.add(msg);
        }
        if (term.length() > 1
            && term.charAt(0) == '"'
            && term.charAt(term.length()-1) == '"') {
            msg = ("Error in KB.ask(), Strings are not indexed.  No results for " + term);
            errors.add(msg);
        }
        ArrayList<Formula> tmp = null;
        String key = null;
        if (kind.equals("arg"))                
            key = kind + "-" + argnum + "-" + term;            
        else 
            key = kind + "-" + term;            
        ArrayList<String> alstr = formulas.get(key);
        
        tmp = stringsToFormulas(alstr);
        if (tmp != null) 
            result.addAll(tmp);                        
        return result;
    }

    /** *************************************************************
     * Returns an ArrayList containing the Formulae retrieved,
     * possibly via multiple asks that recursively use relation and
     * all of its subrelations.  Note that the Formulas might be
     * formed with different predicates, but all of the predicates
     * will be subrelations of relation and will be related to each
     * other in a subsumption hierarchy.
     *
     * @param relation The name of a predicate, which is assumed to be
     *                 the 0th argument of one or more atomic
     *                 formulae  
     * @param idxArgnum The argument position occupied by idxTerm in
     *                  each ground Formula to be retrieved     
     * @param idxTerm A constant that occupied idxArgnum position in
     *                each ground Formula to be retrieved     
     * @return an ArrayList of Formulas that satisfy the query, or an
     *         empty ArrayList if no Formulae are retrieved.
     
    public ArrayList<Formula> askWithPredicateSubsumption(String relation, int idxArgnum, String idxTerm) {

        ArrayList<Formula> ans = new ArrayList<Formula>();
        if (StringUtil.isNonEmptyString(relation) && StringUtil.isNonEmptyString(idxTerm)
            && (idxArgnum >= 0) ) {   // && (idxArgnum < 7)
            HashSet<String> done = new HashSet<String>();
            HashSet<Formula> accumulator = new HashSet<Formula>();
            ArrayList<String> relns = new ArrayList<String>();
            relns.add(relation);
            ArrayList<Formula> formulae = null;
            Formula f = null;
            String arg = null;
            while (!relns.isEmpty()) {
                Iterator<String> it = relns.iterator();
                while (it.hasNext()) {
                    String reln = it.next();
                    formulae = askWithRestriction(0, reln, idxArgnum, idxTerm);
                    ans.addAll(formulae);
                    formulae = askWithRestriction(0, "subrelation", 2, reln);
                    for (int i = 0; i < formulae.size(); i++) {
                        f = (Formula) formulae.get(i);
                        if (!done.contains(f.theFormula)) {
                            arg = f.getArgument(1);
                            if (!reln.equals(arg)) {
                                accumulator.add(arg);
                                done.add(f.theFormula);
                            }
                        }
                    }
                }
                relns.clear();
                relns.addAll(accumulator);
                accumulator.clear();
            }
            // Remove duplicates; perhaps not necessary.
            accumulator.clear();
            accumulator.addAll(ans);
            ans.clear();
            ans.addAll(accumulator);
        }
        return ans;
    }
*/
    public ArrayList<Formula> askWithPredicateSubsumption(String relation, int idxArgnum, String idxTerm) {

        ArrayList<Formula> ans = new ArrayList<Formula>();
        HashSet<Formula> accumulator = new HashSet<Formula>();
        if (StringUtil.isNonEmptyString(relation) && StringUtil.isNonEmptyString(idxTerm)
            && (idxArgnum >= 0) ) {   // && (idxArgnum < 7)

            HashSet<String> relns = new HashSet<String>();  // relation and subrelations
            relns.add(relation);
            ArrayList<Formula> subrelForms = askWithRestriction(0, "subrelation", 2, relation);
            for (int i = 0; i < subrelForms.size(); i++) {
                Formula f = subrelForms.get(i);
                String arg = f.getArgument(1);
                relns.add(arg);
            }
            ArrayList<Formula> forms = ask("arg",idxArgnum,idxTerm);            
            for (int i = 0; i < forms.size(); i++) {
                Formula f = forms.get(i);
                if (!accumulator.contains(f)) {
                    String arg = f.getArgument(0);
                    if (relns.contains(arg)) 
                        accumulator.add(f);                           
                }
            }
            ans.addAll(accumulator);
        }
        return ans;
    }
    
    /** *************************************************************
     * Returns an ArrayList containing SUO-KIF constants, possibly
     * retrieved via multiple asks that recursively use relation and
     * all of its subrelations.
     *
     * @param relation The name of a predicate, which is assumed to be
     *                 the 0th argument of one or more atomic
     *                 Formulae
     *
     * @param idxArgnum The argument position occupied by term in the
     *                  ground atomic Formulae that will be retrieved
     *                  to gather the target (answer) terms
     *
     * @param idxTerm A constant that occupies idxArgnum position in
     *                each of the ground atomic Formulae that will be
     *                retrieved to gather the target (answer) terms
     *
     * @param targetArgnum The argument position of the answer terms
     *                     in the Formulae to be retrieved
     *
     * @param useInverses If true, the inverses of relation and its
     *                    subrelations will be also be used to try to
     *                    find answer terms
     *
     * @param predicatesUsed A Set to which will be added the
     *                       predicates of the ground assertions
     *                       actually used to gather the terms
     *                       returned
     *
     * @return an ArrayList of terms (SUO-KIF constants), or an
     * empy ArrayList if no terms can be retrieved
     */
    public ArrayList<String> getTermsViaPredicateSubsumption(String relation,
                                                             int idxArgnum,
                                                             String idxTerm,
                                                             int targetArgnum,
                                                             boolean useInverses,
                                                             Set predicatesUsed) {

        ArrayList<String> ans = new ArrayList<String>();
        if (StringUtil.isNonEmptyString(relation)
            && StringUtil.isNonEmptyString(idxTerm)
            && (idxArgnum >= 0)
            // && (idxArgnum < 7)
            ) {
            TreeSet<String> reduced = new TreeSet<String>();
            List<String> inverseSyns = null;
            List<String> inverses = null;
            if (useInverses) {
                inverseSyns = getTermsViaAskWithRestriction(0,"subrelation",2,
                                                            "inverse",1);
                inverseSyns.addAll(getTermsViaAskWithRestriction(0,"equal",2,
                                                                 "inverse",1));
                inverseSyns.addAll(getTermsViaAskWithRestriction(0,"equal",
                                                                 1,"inverse",2));
                inverseSyns.add("inverse");
                SetUtil.removeDuplicates(inverseSyns);
                inverses = new ArrayList<String>();
            }
            List<String> accumulator = new ArrayList<String>();
            List<String> predicates = new ArrayList<String>();
            predicates.add(relation);
            while (!predicates.isEmpty()) {
                for (String pred : predicates) {
                    reduced.addAll(getTermsViaAskWithRestriction(0,pred,idxArgnum,
                                                                 idxTerm,targetArgnum,predicatesUsed));
                    accumulator.addAll(getTermsViaAskWithRestriction(0,"subrelation",2,
                                                                     pred,1));
                    accumulator.addAll(getTermsViaAskWithRestriction(0,"equal",2,
                                                                     "subrelation",1));
                    accumulator.addAll(getTermsViaAskWithRestriction(0,"equal",1,
                                                                     "subrelation",2));
                    accumulator.remove(pred);
                    if (useInverses) {
                        for (String syn : inverseSyns) {
                            inverses.addAll(getTermsViaAskWithRestriction(0,syn,1,pred,2));
                            inverses.addAll(getTermsViaAskWithRestriction(0,syn,2,pred,1));
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
                    reduced.addAll(getTermsViaPredicateSubsumption(inv,targetArgnum,idxTerm,
                                                                   idxArgnum,false,predicatesUsed));                
            }
            ans.addAll(reduced);
        }
        return ans;
    }

    /** *************************************************************
     * Returns an ArrayList containing SUO-KIF constants, possibly
     * retrieved via multiple asks that recursively use relation and
     * all of its subrelations.
     *
     * @param relation The name of a predicate, which is assumed to be
     *                 the 0th argument of one or more atomic
     *                 Formulae
     *
     * @param idxArgnum The argument position occupied by term in the
     *                  ground atomic Formulae that will be retrieved
     *                  to gather the target (answer) terms
     *
     * @param idxTerm A constant that occupies idxArgnum position in
     *                each of the ground atomic Formulae that will be
     *                retrieved to gather the target (answer) terms
     *
     * @param targetArgnum The argument position of the answer terms
     *                     in the Formulae to be retrieved
     *
     * @param useInverses If true, the inverses of relation and its
     *                    subrelations will be also be used to try to
     *                    find answer terms
     *
     * @return an ArrayList of terms (SUO-KIF constants), or an
     * empy ArrayList if no terms can be retrieved
     */
    public ArrayList<String> getTermsViaPredicateSubsumption(String relation,
                                                             int idxArgnum,
                                                             String idxTerm,
                                                             int targetArgnum,
                                                             boolean useInverses) {
        return getTermsViaPredicateSubsumption(relation,
                                               idxArgnum,
                                               idxTerm,
                                               targetArgnum,
                                               useInverses,
                                               null);
    }

    /** *************************************************************
     * Returns the first SUO-KIF constant found via asks using
     * relation and its subrelations.
     *
     * @param relation The name of a predicate, which is assumed to be
     *                 the 0th argument of one or more atomic
     *                 Formulae.
     *
     * @param idxArgnum The argument position occupied by term in the
     *                  ground atomic Formulae that will be retrieved
     *                  to gather the target (answer) terms.
     *
     * @param idxTerm A constant that occupies idxArgnum position in
     *                each of the ground atomic Formulae that will be
     *                retrieved to gather the target (answer) terms.
     *
     * @param targetArgnum The argument position of the answer terms
     *                     in the Formulae to be retrieved.
     *
     * @param useInverses If true, the inverses of relation and its
     *                    subrelations will be also be used to try to
     *                    find answer terms.
     *
     * @return A SUO-KIF constants (String), or null if no term can be
     *         retrieved.
     */
    public String getFirstTermViaPredicateSubsumption(String relation,
                                                      int idxArgnum,
                                                      String idxTerm,
                                                      int targetArgnum,
                                                      boolean useInverses) {

        String ans = null;
        if (StringUtil.isNonEmptyString(relation)
            && StringUtil.isNonEmptyString(idxTerm)
            && (idxArgnum >= 0)
            // && (idxArgnum < 7)
            ) {
            ArrayList<String> terms = getTermsViaPredicateSubsumption(relation,
                                               idxArgnum,idxTerm,targetArgnum,useInverses);
            if (!terms.isEmpty()) 
                ans = (String) terms.get(0);            
        }
        return ans;
    }

    /** *************************************************************
     * Returns an ArrayList containing the transitive closure of
     * relation starting from idxTerm in position idxArgnum.  The
     * result does not contain idxTerm.
     *
     * @param relation The name of a predicate, which is assumed to be
     *                 the 0th argument of one or more atomic
     *                 Formulae
     *
     * @param idxArgnum The argument position occupied by term in the
     *                  ground atomic Formulae that will be retrieved
     *                  to gather the target (answer) terms
     *
     * @param idxTerm A constant that occupies idxArgnum position in
     *                the first "level" of ground atomic Formulae that
     *                will be retrieved to gather the target (answer)
     *                terms
     *
     * @param targetArgnum The argument position of the answer terms
     *                     in the Formulae to be retrieved
     *
     * @param useInverses If true, the inverses of relation and its
     *                    subrelations will be also be used to try to
     *                    find answer terms
     *
     * @return an ArrayList of terms (SUO-KIF constants), or an
     * empy ArrayList if no terms can be retrieved
     */
    public ArrayList<String> getTransitiveClosureViaPredicateSubsumption(String relation, int idxArgnum,
                                                                         String idxTerm, int targetArgnum,
                                                                         boolean useInverses) {
        ArrayList<String> ans = new ArrayList<String>();
        Set<String> reduced = new TreeSet<String>();
        Set<String> accumulator =
            new TreeSet<String>(getTermsViaPredicateSubsumption(relation, idxArgnum, idxTerm,
                                                                targetArgnum, useInverses));
        ArrayList<String> working = new ArrayList<String>();
        while (!accumulator.isEmpty()) {
            reduced.addAll(accumulator);
            working.clear();
            working.addAll(accumulator);
            accumulator.clear();
            for (String term : working)
                accumulator.addAll(getTermsViaPredicateSubsumption(relation,
                		idxArgnum,term,targetArgnum,useInverses));
        }
        ans.addAll(reduced);
        return ans;
    }

    /** *************************************************************
     * Merges a KIF object containing a single formula into the current KB.
     *
     * @param kif A KIF object.
     *
     * @param pathname The full, canonical pathname string of the
     * constituent file in which the formula will be saved, if known.
     *
     * @return If any of the formulas are already present, returns an
     * ArrayList containing the old (existing) formulas, else returns
     * an empty ArrayList.
     */
    private ArrayList<Formula> merge(KIF kif, String pathname) {
             
        ArrayList<Formula> formulasPresent = new ArrayList<Formula>();
        // Add all the terms from the new formula into the KB's current list
        getTerms().addAll(kif.terms);
        Set<String> keys = kif.formulas.keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String key = it.next();
            ArrayList<String> newFormulas = new ArrayList<String>(kif.formulas.get(key));
            if (formulas.containsKey(key)) {
                ArrayList<String> oldFormulas = formulas.get(key);
                for (int i = 0; i < newFormulas.size(); i++) {
                    Formula newFormula = formulaMap.get(newFormulas.get(i));
                    if (pathname != null) 
                        newFormula.sourceFile = pathname;                    
                    boolean found = false;
                    for (int j = 0; j < oldFormulas.size(); j++) {
                        Formula oldFormula = formulaMap.get(oldFormulas.get(j));
                        if (newFormula.theFormula.equals(oldFormula.theFormula)) {
                            found = true;
                            formulasPresent.add(oldFormula);
                        }
                    }
                    if (!found) {
                        oldFormulas.add(newFormula.theFormula);
                        formulaMap.put(newFormula.theFormula.intern(), newFormula);
                    }
                }
            }
            else {
                formulas.put(key,newFormulas);
                Iterator<String> it2 = newFormulas.iterator();
                Formula f = null;
                while (it2.hasNext()) {
                    f = formulaMap.get(it2.next());
                    if (StringUtil.isNonEmptyString(f.theFormula))
                        formulaMap.put(f.theFormula.intern(), f);                        
                }
            }
        }       
        return formulasPresent;
    }

    /** *************************************************************
     *  Rename term2 as term1 throughout the knowledge base.  This
     *  is an operation with side effects - the term names in the KB
     *  are changed.
     */
    public void rename(String term2, String term1) {

        HashSet<Formula> formulas = new HashSet<Formula>();
        for (int i = 0; i < 7; i++)
            formulas.addAll(ask("arg",i,term2));
        formulas.addAll(ask("ant",0,term2));
        formulas.addAll(ask("cons",0,term2));
        formulas.addAll(ask("stmt",0,term2));
        Iterator<Formula> it = formulas.iterator();
        while (it.hasNext()) {
            Formula f = it.next();
            f.theFormula = f.rename(term2,term1).theFormula;
        }
    }

    /** *************************************************************
     *  Writes a list of Formulas to a file.
     * @param formulas an ArrayList of Strings.
     * @param fname The fully qualified file name.
     * @return void
     
    private void writeFormulas(ArrayList<String> formulas, String fname) {

        FileWriter fr = null;
        try {
            fr = new FileWriter(fname,true);
            String ls = System.getProperty("line.separator");
            Iterator<String> it = formulas.iterator();
            while (it.hasNext()) {
                fr.write(it.next());
                fr.write(ls);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (fr != null)
                    fr.close();
            }
            catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return;
    }
*/
    /** *************************************************************
     *  Writes a single user assertion (String) to the end of a file.
     *
     * @param formula A String representing a SUO-KIF Formula.
     * @param fname A String denoting the pathname of the target file.
     * @return A long value indicating the number of bytes in the file
     * after the formula has been written.  A value of 0L means that
     * the file does not exist, and so could not be written for some
     * reason.  A value of -1 probably means that some error occurred.
     */
    private long writeUserAssertion(String formula, String fname) throws IOException {

        long flen = -1L;
        FileWriter fr = null;
        try {
            File file = new File(fname);
            fr = new FileWriter(file,true);
            fr.write(formula);
            fr.write("\n");
            flen = file.length();
        }
        catch (java.io.IOException e) {
            e.printStackTrace();
        }
        finally {
            if (fr != null) fr.close();
        }
        return flen;
    }

    /** *************************************************************
     *  Writes all the terms in the knowledge base to a file
     */
    public void writeTerms() throws IOException {

        String fname = KBmanager.getMgr().getPref("kbDir") + File.separator + "terms.txt";
        FileWriter fr = null;
        try {
            File file = new File(fname);
            fr = new FileWriter(file,true);
            Iterator<String> it = terms.iterator();
            while (it.hasNext()) {
                String term = it.next();
                fr.write(term);
                fr.write("\n");
            }
        }
        catch (java.io.IOException e) {
            e.printStackTrace();
        }
        finally {
            if (fr != null) fr.close();
        }        
    }
    
    /** *************************************************************
     * Adds a formula to the knowledge base.  Returns an XML formatted
     * String that contains the response of the inference engine.  It
     * should be of the form "<assertionResponse>...</assertionResponse>"
     * where the body should be " Formula has been added to the session
     * database" if all went well.
     *
     * @param input The String representation of a SUO-KIF Formula.
     *
     * @return A String indicating the status of the tell operation.
     */
    public String tell(String input) {

        String result = "The formula could not be added";
        KBmanager mgr = KBmanager.getMgr();            
        KIF kif = new KIF();    // 1. Parse the input string.
        String msg = kif.parseStatement(input);
        if (msg != null) {
            result = "Error parsing \"" + input + "\" " + msg;
            return result;
        }
        if (kif.formulaMap.keySet().isEmpty()) { 
            result = "The input could not be parsed";
            return result;
        }
        try {  // Make the pathname of the user assertions file.
            File dir = new File(this.kbDir);
            File file = new File(dir, (this.name + _userAssertionsString));
            String filename = file.getCanonicalPath();
            ArrayList<Formula> formulasAlreadyPresent = merge(kif, filename);
            if (!formulasAlreadyPresent.isEmpty()) {
                String sf = ((Formula)formulasAlreadyPresent.get(0)).sourceFile;
                result = "The formula was already added from " + sf;
            }
            else {
                ArrayList<Formula> parsedFormulas = new ArrayList<Formula>();
                Iterator<Formula> it = kif.formulaMap.values().iterator();
                while (it.hasNext()) { // 2. Confirm that the input has been converted into
                                       // at least one Formula object and stored in this.formulaMap.
                    Formula parsedF = it.next();   
                    String term = PredVarInst.hasCorrectArity(parsedF,this);
                    if (!StringUtil.emptyString(term)) {
                    	result = result + "Formula in " + parsedF.sourceFile + 
                                " rejected due to arity error of predicate " + term +
                                " in formula: \n" + parsedF.theFormula;
                    }                       
                    else 
                        parsedFormulas.add(parsedF);                        
                }
                if (!parsedFormulas.isEmpty()) {
                    if (!constituents.contains(filename)) {                            
                        if (file.exists())  // 3. If the assertions file exists, delete it.
                            file.delete();
                        constituents.add(filename);
                        //mgr.writeConfiguration();
                    }
                    Iterator<Formula> pfit = parsedFormulas.iterator();
                    while (pfit.hasNext()) {
                        Formula parsedF = pfit.next();
                        // 4. Write the formula to the user assertions file.
                        parsedF.endFilePosition = writeUserAssertion(parsedF.theFormula,filename);
                        parsedF.sourceFile = filename;
                    }
                    result = "The formula has been added for browsing";
                    boolean allAdded = (eprover != null);
                    ArrayList<Formula> processedFormulas = new ArrayList<Formula>();
                    Iterator<Formula> it2 = parsedFormulas.iterator();
                    while (it2.hasNext()) {
                        processedFormulas.clear();
                        Formula parsedF = it2.next();          // 5. Preproccess the formula.   
                        FormulaPreprocessor fp = new FormulaPreprocessor();
                        processedFormulas.addAll(fp.preProcess(parsedF,false, this));
                        errors.addAll(parsedF.getErrors());
                        if (processedFormulas.isEmpty()) 
                            allAdded = false;                            
                        else {   // 6. Translate to TPTP.                            
                            if (!mgr.getPref("TPTP").equalsIgnoreCase("no")) {
                            	SUMOformulaToTPTPformula stptp = new SUMOformulaToTPTPformula();
                            	stptp._f = parsedF;
                            	stptp.tptpParse(parsedF,false, this, processedFormulas);
                            }
                            // 7. If there is an inference engine, assert the formula to the
                            // inference engine's database.
                            if (eprover != null) {
                                String ieResult = null;
                                Formula processedF = null;
                                Iterator<Formula> it3 = processedFormulas.iterator();
                                while (it3.hasNext()) {
                                    processedF = it3.next();
                                    ieResult = eprover.assertFormula(processedF.theFormula);
                                    if (ieResult.indexOf("Formula has been added") < 0) 
                                        allAdded = false;                                        
                                }
                            }
                        }
                    }
                    result += (allAdded ? " and inference" : " but not for local inference");
                }
            }
        }        
        catch (IOException ioe) {
            ioe.printStackTrace();
            System.out.println(ioe.getMessage());
            result = ioe.getMessage();
        }
        catch (ParseException pe) {
            pe.printStackTrace();
            System.out.println(pe.getMessage());
            result = pe.getMessage();
        }
        return result;
    }

    /** *************************************************************
     * Submits a query to the inference engine.  Returns an XML
     * formatted String that contains the response of the inference
     * engine.  It should be in the form
     * "<queryResponse>...</queryResponse>".
     *
     * @param suoKifFormula The String representation of the SUO-KIF
     * query.
     *
     * @param timeout The number of seconds after which the inference
     * engine should give up.
     *
     * @param maxAnswers The maximum number of answers (binding sets)
     * the inference engine should return.
     *
     * @return A String indicating the status of the ask operation.
     */
    public String ask(String suoKifFormula, int timeout, int maxAnswers) {

    	String result = "";
    	// Start by assuming that the ask is futile.
    	result = ("<queryResponse>" + System.getProperty("line.separator")
    			+ "  <answer result=\"no\" number=\"0\"> </answer>" + System.getProperty("line.separator")
    			+ "  <summary proofs=\"0\"/>" + System.getProperty("line.separator")
    			+ "</queryResponse>" + System.getProperty("line.separator"));
    	if (StringUtil.isNonEmptyString(suoKifFormula)) {
    		Formula query = new Formula();
    		query.read(suoKifFormula);
    		FormulaPreprocessor fp = new FormulaPreprocessor();
    		ArrayList<Formula> processedStmts = fp.preProcess(query,true, this);

    		if (!processedStmts.isEmpty() && this.eprover != null) {
    			String strQuery = processedStmts.get(0).theFormula;                
    			result = this.eprover.submitQuery(strQuery,this);
    		}
    	}
    	return result;
    }

    /** *************************************************************
     * Submits a query to specified InferenceEngine object.  Returns an XML
     * formatted String that contains the response of the inference
     * engine.  It should be in the form
     * "<queryResponse>...</queryResponse>".
     *
     * @param suoKifFormula The String representation of the SUO-KIF
     * query.
     *
     * @param timeout The number of seconds after which the underlying inference
     * engine should give up. (Time taken by axiom selection doesn't count.)
     *
     * @param maxAnswers The maximum number of answers (binding sets)
     * the inference engine should return.
     *
     * @param engine InferenceEngine object that will be used for the inference.
     *
     * @return A String indicating the status of the ask operation.
     */
    public String askEngine(String suoKifFormula, int timeout, int maxAnswers, InferenceEngine engine) {

        String result = "";
        // Start by assuming that the ask is futile.
        result = "<queryResponse>\n<answer result=\"no\" number=\"0\">\n</answer>\n<summary proofs=\"0\"/>\n</queryResponse>\n";
        if (!StringUtil.emptyString(suoKifFormula)) {
            Formula query = new Formula();
            query.read(suoKifFormula);
            FormulaPreprocessor fp = new FormulaPreprocessor();
            ArrayList<Formula> processedStmts = fp.preProcess(query,true, this);
            try {
                if (!processedStmts.isEmpty()) {
                    String strQuery = processedStmts.get(0).theFormula;                
                    result = engine.submitQuery(strQuery,timeout,maxAnswers);
                }
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
                String message = ioe.getMessage().replaceAll(":","&58;");
                errors.add(message);
                result = ioe.getMessage();
            }
        }
        result = result.replaceAll("&lt;","<");
        result = result.replaceAll("&gt;",">");
        return result;
    }

    /** *************************************************************
     * Submits a query to the SInE inference engine.  Returns an XML
     * formatted String that contains the response of the inference
     * engine.  It should be in the form
     * "<queryResponse>...</queryResponse>".
     *
     * @param suoKifFormula The String representation of the SUO-KIF
     * query.
     *
     * @param timeout The number of seconds after which the underlying inference
     * engine should give up. (Time taken by axiom selection doesn't count.)
     *
     * @param maxAnswers The maximum number of answers (binding sets)
     * the inference engine should return.
     *
     * @return A String indicating the status of the ask operation.
     */
    public String askSInE(String suoKifFormula, int timeout, int maxAnswers) {

        String result = "";
        InferenceEngine.EngineFactory factory = SInE.getFactory();
        InferenceEngine engine = createInferenceEngine(factory);
        result = askEngine(suoKifFormula, timeout, maxAnswers, engine);
        try {
            if (engine != null)
                engine.terminate();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            String message = ioe.getMessage().replaceAll(":","&58;");
            errors.add(message);
            result = ioe.getMessage();
        }
        return result;
    }

    /** *************************************************************
     * Submits a query to the LEO inference engine.  Returns an XML
     * formatted String that contains the response of the inference
     * engine.  It should be in the form
     * "<queryResponse>...</queryResponse>".
     *
     * @param suoKifFormula The String representation of the SUO-KIF
     * query.
     *
     * @param timeout The number of seconds after which the underlying inference
     * engine should give up. (Time taken by axiom selection doesn't count.)
     *
     * @param maxAnswers The maximum number of answers (binding sets)
     * the inference engine should return.
     *
     * @return A String indicating the status of the ask operation.
     */
    public String askLEO(String suoKifFormula, int timeout, int maxAnswers, String flag) {

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
            try {  // add user asserted formulas
                File dir = new File(this.kbDir);
                File file = new File(dir, (this.name + _userAssertionsString));
                String filename = file.getCanonicalPath();
                BufferedReader userAssertedInput =  new BufferedReader(new FileReader(filename));

                try {
                    String line = null;
                    /* readLine is a bit quirky :
                     * it returns the content of a line MINUS the newline.
                     * it returns null only for the END of the stream.
                     * it returns an empty String if two newlines appear in a row.
                     */
                    while ((line = userAssertedInput.readLine()) != null)
                        selFs.add(line);                    
                }
                finally {
                    userAssertedInput.close();
                }
            }
            catch (IOException ex){
            	System.out.println("Error in KB.askLEO(): " + ex.getMessage());
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
            LeoProblem = thf.KIF2THF(selectedFormulas,selectedQuery,this);
            LeoInputFileW.write(LeoProblem);
            LeoInputFileW.close();

            String command = LeoExecutableFile.getCanonicalPath() + " -po 1 -t " + timeout + " " + LeoInputFile.getCanonicalPath();

            Process leo = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(leo.getInputStream()));
            while ((responseLine = reader.readLine()) != null) 
                LeoOutput += responseLine + "\n";            
            reader.close();
            System.out.println(LeoOutput);

            if (LeoOutput.contains("SZS status Theorem")) {
                result = "Answer 1. yes"
                        + "<br> <br>" + LeoProblem.replaceAll("\\n","<br>")
                        + "<br> <br>" + LeoOutput.replaceAll("\\n","<br>");
            }
            else {
                result = "Answer 1. don't know"
                        + "<br> <br>" + LeoProblem.replaceAll("\\n","<br>")
                        + "<br> <br>" + LeoOutput.replaceAll("\\n","<br>");
            }
        }
        catch (Exception ex) {
        	System.out.println("Error in KB.askLEO(): " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
     * Takes a term and returns true if the term occurs in the KB.
     *
     * @param term A String.
     * @return true or false.
     */
    public boolean containsTerm(String term) {
        
        if (getTerms().contains(term.intern())) 
            return true;        
        else if (getREMatch(term.intern()).size()==1) 
            return true;       
        return false;
    }

    /** ***************************************************************
     * Takes a formula string and returns true if the corresponding
     * Formula occurs in the KB.
     *
     * @param formula A String.
     * @return true or false.
     */
    public boolean containsFormula(String formula) {

        return formulaMap.containsKey(formula.intern());
    }

    /** ***************************************************************
     * Count the number of terms in the knowledge base in order to
     * present statistics to the user.
     *
     * @return The int(eger) number of terms in the knowledge base.
     */
    public int getCountTerms() {

        return getTerms().size();
    }

    /** ***************************************************************
     *  Count the number of relations in the knowledge base in order to
     *  present statistics to the user.
     *
     *  @return The int(eger) number of relations in the knowledge base.
     */
    public int getCountRelations() {

        return kbCache.relations.size();
    }

    /** ***************************************************************
     *  Count the number of formulas in the knowledge base in order to
     *  present statistics to the user.
     *
     *  @return The int(eger) number of formulas in the knowledge base.
     */
    public int getCountAxioms() {

        return formulaMap.size();
    }

    /** ***************************************************************
     *  An accessor providing a TreeSet of un-preProcessed String
     *  representations of Formulae.
     *
     *  @return A TreeSet of Strings.
     */
    public TreeSet<String> getFormulas() {

        return new TreeSet<String>(formulaMap.keySet());
    }

    /** ***************************************************************
     *  An accessor providing a Formula
     */
    public Formula getFormulaByKey(String key) {
    	
        Formula f = null;
        ArrayList<String> al = formulas.get(key);
        if ((al != null) && !al.isEmpty())
            f = formulaMap.get(al.get(0));
        return f;
    }

    /** ***************************************************************
     
    public void rehashFormula(Formula f, String formID) {

        ArrayList<String> al = formulas.get(formID);
        if ((al != null) && !al.isEmpty()) {
            if (al.size() == 1) {
                String newID = f.createID();
                formulas.remove(formID);
                al = new ArrayList();
                al.add(f);
                if (!formulas.keySet().contains(newID))
                    formulas.put(newID,al);
            }
            else
                logger.info("Formula hash collision for: " + formID + " and formula " + f.theFormula);
        }
        else
            logger.info("No formula for hash: " + formID + " and formula " + f.theFormula);
    }
*/
    /** ***************************************************************
     *  Count the number of rules in the knowledge base in order to
     *  present statistics to the user. Note that the number of rules
     *  is a subset of the number of formulas.
     *
     *  @return The int(eger) number of rules in the knowledge base.
     */
    public int getCountRules() {

        int count = 0;
        Iterator<Formula> it = formulaMap.values().iterator();
        while (it.hasNext()) {
            Formula f = it.next();
            if (f.isRule()) 
                count++;            
        }
        return count;
    }

    /** ***************************************************************
     * Create an ArrayList of the specific size, filled with empty strings.
     */
    private ArrayList<String> arrayListWithBlanks(int size) {

        ArrayList<String> al = new ArrayList<String>(size);
        for (int i = 0; i < size; i++)
            al.add("");
        return al;
    }

    /** ***************************************************************
     * Get the alphabetically nearest terms to the given term, which
     * is not in the KB.  Elements 0-(k-1) should be alphabetically
     * lesser and k-(2*k-1) alphabetically greater.  If the term is
     * at the beginning or end of the alphabet, fill in blank items
     * with the empty string: "".
     */
    private ArrayList<String> getNearestKTerms(String term, int k) {

        ArrayList<String> al;
        if (k == 0)
            al = arrayListWithBlanks(1);
        else
            al = arrayListWithBlanks(2*k);
        Object[] t;
        t = getTerms().toArray();
        int i = 0;
        while (i < t.length-1 && ((String) t[i]).compareTo(term) < 0)
            i++;
        if (k == 0) {
            al.set(0,(String) t[i]);
            return al;
        }
        int lower = i;
        while (i - lower < k && lower > 0) {
            lower--;
            al.set(k - (i - lower),(String) t[lower]);
        }
        int upper = i-1;
        while (upper - i < (k-1) && upper < t.length-1) {
            upper++;
            al.set(k + (upper - i),(String) t[upper]);
        }
        return al;
    }

    /** ***************************************************************
     * Get the alphabetically nearest terms to the given term, which
     * is not in the KB.  Elements 0-14 should be alphabetically lesser and
     * 15-29 alphabetically greater.  If the term is at the beginning or end
     * of the alphabet, fill in blank items with the empty string: "".
     */
    private ArrayList<String> getNearestTerms(String term) {

        return getNearestKTerms(term,15);
    }

    /** ***************************************************************
     * Get the neighbors of this initial uppercase term (class or function).
     */
    public ArrayList<String> getNearestRelations(String term) {

        term = Character.toUpperCase(term.charAt(0)) + term.substring(1,term.length());
        return getNearestTerms(term);
    }

    /** ***************************************************************
     * Get the neighbors of this initial lowercase term (relation).
     */
    public ArrayList<String> getNearestNonRelations(String term) {

        term = Character.toLowerCase(term.charAt(0)) + term.substring(1,term.length());
        return getNearestTerms(term);
    }

    /** ***************************************************************
     * Get the alphabetically num lower neighbor of this initial term, which
     * must exist in the current KB otherwise an empty string is returned.
     */
    public String getAlphaBefore(String term, int num) {

        if (!getTerms().contains(term)) {
            ArrayList<String> al = getNearestKTerms(term,0);
            term = (String) al.get(0);
        }
        if (getTerms().size() < 1)
            return "";
        ArrayList<String> tal = new ArrayList<String>(getTerms());
        int i = tal.indexOf(term.intern());
        if (i < 0)
            return "";
        i = i - num;
        if (i < 0)
            i = 0;
        return (String) tal.get(i);
    }
    
    /** ***************************************************************
     * Get the alphabetically num higher neighbor of this initial term, which
     * must exist in the current KB otherwise an empty string is returned.
     */
    public String getAlphaAfter(String term, int num) {

        if (!getTerms().contains(term)) {
            ArrayList<String> al = getNearestKTerms(term,0);
            term = (String) al.get(0);
        }
        if (getTerms().size() < 1)
            return "";
        ArrayList<String> tal = new ArrayList<String>(getTerms());
        int i = tal.indexOf(term.intern());
        if (i < 0)
            return "";
        i = i + num;
        if (i >= tal.size())
            i = tal.size() - 1;
        return (String) tal.get(i);
    }

    /** ***************************************************************
     * This List is used to limit the number of warning messages
     * logged by loadFormatMaps(lang).  If an attempt to load format
     * or termFormat values for lang is unsuccessful, the list is
     * checked for the presence of lang.  If lang is not in the list,
     * a warning message is logged and lang is added to the list.  The
     * list is cleared whenever a constituent file is added or removed
     * for KB, since the latter might affect the availability of
     * format or termFormat values.
     */
    protected ArrayList<String> loadFormatMapsAttempted = new ArrayList<String>();

    /** ***************************************************************
     * Populates the format maps for language lang.
     *
     * @see termFormatMap is a HashMap of language keys and HashMap
     * values.  The interior HashMaps are term keys and format
     * string values.
     *
     * @see formatMap is the same but for relation format strings.
     */
    protected void loadFormatMaps(String lang) {

        if (formatMap == null)
            formatMap = new HashMap<String,HashMap<String,String>>();
        if (termFormatMap == null)
            termFormatMap = new HashMap<String,HashMap<String,String>>();
        if (formatMap.get(lang) == null)
            formatMap.put(lang, new HashMap<String,String>());
        if (termFormatMap.get(lang) == null)
            termFormatMap.put(lang, new HashMap<String,String>());

        if (!loadFormatMapsAttempted.contains(lang)) {
            ArrayList<Formula> col = askWithRestriction(0,"format",1,lang);
            if ((col == null) || col.isEmpty()) 
            	System.out.println("Error in KB.loadFormatMaps(): No relation format file loaded for language " + lang);                
            else {
                HashMap<String,String> langFormatMap = formatMap.get(lang);
                Iterator<Formula> ite = col.iterator();
                while (ite.hasNext()) {
                    Formula f = (Formula) ite.next();
                    String key = f.getArgument(2);
                    String format = f.getArgument(3);
                    format = StringUtil.removeEnclosingQuotes(format);
                    langFormatMap.put(key, format);
                }
            }
            col = askWithRestriction(0,"termFormat",1,lang);
            if ((col == null) || col.isEmpty()) 
            	System.out.println("Error in KB.loadFormatMaps(): No term format file loaded for language: " + lang);                
            else {
                HashMap<String,String> langTermFormatMap = termFormatMap.get(lang);
                Iterator<Formula> ite = col.iterator();
                while (ite.hasNext()) {
                    Formula f = (Formula) ite.next();
                    String key = f.getArgument(2);
                    String format = f.getArgument(3);
                    format = StringUtil.removeEnclosingQuotes(format);
                    langTermFormatMap.put(key,format);
                }
            }
            loadFormatMapsAttempted.add(lang);
        }
        language = lang;
    }

    /** ***************************************************************
     * Clears all loaded format and termFormat maps, for all
     * languages.     
     */
    protected void clearFormatMaps() {

        if (formatMap != null) {
            Iterator<HashMap<String,String>> itf = formatMap.values().iterator();
            while (itf.hasNext()) {
                HashMap<String,String> m = itf.next();
                if (m != null) m.clear();
            }
            formatMap.clear();
        }
        if (termFormatMap != null) {
            Iterator<HashMap<String,String>> itf = termFormatMap.values().iterator();
            while (itf.hasNext()) {
                HashMap<String,String> m = itf.next();
                if (m != null) m.clear();
            }
            termFormatMap.clear();
        }
        loadFormatMapsAttempted.clear();
        return;
    }

    /** ***************************************************************
     * This method creates a dictionary (Map) of SUO-KIF term symbols
     * -- the keys -- and a natural language string for each key that
     * is the preferred name for the term -- the values -- in the
     * context denoted by lang.  If the Map has already been built and
     * the language hasn't changed, just return the existing map.
     * This is a case of "lazy evaluation".
     *
     *  @return An instance of Map where the keys are terms and the
     *  values are format strings.
     */
    public HashMap<String,String> getTermFormatMap(String lang) {
        
        if (!StringUtil.isNonEmptyString(lang)) 
            lang = "EnglishLanguage";        
        if ((termFormatMap == null) || termFormatMap.isEmpty()) 
            loadFormatMaps(lang);        
        HashMap<String,String> langTermFormatMap = termFormatMap.get(lang);
        if ((langTermFormatMap == null) || langTermFormatMap.isEmpty()) 
            loadFormatMaps(lang);        
        return (HashMap<String,String> ) termFormatMap.get(lang);
    }

    /** ***************************************************************
     *  This method creates an association list (Map) of the natural
     *  language format string and the relation name for which that
     *  format string applies.  If the map has already been built and
     *  the language hasn't changed, just return the existing map.
     *  This is a case of "lazy evaluation".
     *
     *  @return An instance of Map where the keys are relation names
     *  and the values are format strings.
     */
    public HashMap<String,String> getFormatMap(String lang) {

        if (!StringUtil.isNonEmptyString(lang)) 
            lang = "EnglishLanguage";        
        if ((formatMap == null) || formatMap.isEmpty()) 
            loadFormatMaps(lang);        
        HashMap<String,String> langFormatMap = formatMap.get(lang);
        if ((langFormatMap == null) || langFormatMap.isEmpty()) 
            loadFormatMaps(lang);        
        return formatMap.get(lang);
    }

    /** ***************************************************************
     * Deletes the user assertions file, and then reloads the KB.
     */
    public void deleteUserAssertions() {

        String cname = null;
        for (int i = 0 ; i < constituents.size() ; i++) {
            cname = (String) constituents.get(i);
            if (cname.endsWith(_userAssertionsString)) {
                try {
                    constituents.remove(i);
                    KBmanager.getMgr().writeConfiguration();
                    reload();
                }
                catch (IOException ioe) {
                    System.out.println("Error in KB.deleteUserAssertions(): writing configuration: " + ioe.getMessage());
                }
            }
        }
        return;
    }

    /** *************************************************************
     
    public void addNewConstituent(String filename) throws IOException {
        addConstituent(filename, true, false, true);
    }
    
    ** *************************************************************
     * Add a new KB constituent by reading in the file, and then
     * merging the formulas with the existing set of formulas.  All
     * assertion caches are rebuilt, the current EProver process is
     * destroyed, and a new one is created.
     *
     * @param filename - the full path of the file being added.
     
    public void addConstituent(String filename) throws IOException {
        addConstituent(filename, true, true, false);
    }
*/
    /** *************************************************************
     * Add a new KB constituent by reading in the file, and then merging
     * the formulas with the existing set of formulas.
     *
     * @param filename - The full path of the file being added
     * @param buildCachesP - If true, forces the assertion caches to be rebuilt - deprecated
     * @param performArity - Perform arity check - deprecated since this can't be dont until all relations are defined
     * @param loadEProverP - If true, destroys the old EProver process and
     * starts a new one     - deprecated
     */    
    public void addConstituent(String filename) {
    //, boolean buildCachesP, boolean loadEProverP, boolean performArity) {

    	String canonicalPath = null;
    	KIF file = new KIF();
    	try {
	        if (filename.endsWith(".owl") || filename.endsWith(".OWL") ||
	            filename.endsWith(".rdf") || filename.endsWith(".RDF")) {
	            OWLtranslator.read(filename);
	            filename = filename + ".kif";
	        }
	        File constituent = new File(filename);
	        
	        canonicalPath = constituent.getCanonicalPath();
	        if (constituents.contains(canonicalPath))
	            errors.add("Error. " + canonicalPath + " already loaded.");        
            file.readFile(canonicalPath);
            errors.addAll(file.warningSet);
        }
        catch (Exception ex1) {
            StringBuilder error = new StringBuilder();
            error.append(ex1.getMessage());
            if (ex1 instanceof ParseException)
                error.append(" at line " + ((ParseException)ex1).getErrorOffset());
            error.append(" in file " + canonicalPath);
            errors.add(error.toString());
        }

        Iterator<String> it = file.formulas.keySet().iterator();
        int count = 0;
        while (it.hasNext()) { // Iterate through keys.
            String key = it.next();                 
            if ((count++ % 100) == 1)  
                System.out.print("."); 
            ArrayList<String> newlist = file.formulas.get(key);
            
         // temporary debug test to find nulls
            for (int i = 0; i < newlist.size(); i++) {
            	String form = newlist.get(i);
            	if (StringUtil.emptyString(form))
            		System.out.println("Error in KB.addConstituent() 1: formula is null ");
            }
            ArrayList<String> list = formulas.get(key);   
            
            if (list != null) {
                // temporary debug test to find nulls
                for (int i = 0; i < list.size(); i++) {
                	String form = list.get(i);
                	if (StringUtil.emptyString(form))
                		System.out.println("Error in KB.addConstituent() 2: formula is null ");
                }
                newlist.addAll(list);
            }                 
            formulas.put(key, newlist);            
        }
          
        count = 0;
        Iterator<Formula> it2 = file.formulaMap.values().iterator();
        while (it2.hasNext()) { // Iterate through values
            Formula f = (Formula) it2.next();
            String internedFormula = f.theFormula.intern();
            if ((count++ % 100) == 1)  
                System.out.print(".");
            if (!formulaMap.containsKey(internedFormula))
                formulaMap.put(internedFormula, f);
        }

        this.getTerms().addAll(file.terms);
        if (!constituents.contains(canonicalPath))
            constituents.add(canonicalPath);            
        //clearFormatMaps(); // Clear formatMap and termFormatMap for this KB.
        //if (buildCachesP && !canonicalPath.endsWith(_cacheFileSuffix)) {
        //    kbCache = new KBcache(this);
        //    kbCache.buildCaches();
        //}
        //if (loadEProverP)
        //    loadEProver();
    }

    /** ***************************************************************
     * deprecated code
     
    private void oldArityCheck(KIF file) {
        
        Iterator<Formula> it2 = file.formulaMap.values().iterator();
        while (it2.hasNext()) { // Iterate through values
            Formula f = (Formula) it2.next();
            boolean correctArity = true;
            if (!PredVarInst.hasCorrectArity(f, this)) {
                errors.add("Incorrect arity:\n" + f.htmlFormat(this));
                System.out.println("Incorrect arity: " + f.theFormula);
                correctArity = false;
            }
            if (correctArity) {
                String internedFormula = f.theFormula.intern();
                if (!formulaMap.containsKey(internedFormula))
                    formulaMap.put(internedFormula, f);
                else {
                    StringBuilder error = new StringBuilder();
                    error.append("Warning: Duplicate axiom in ");
                    error.append(f.sourceFile + " at line " + f.startLine
                            + "<br />");
                    error.append(f.theFormula + "<p>");
                    Formula existingFormula = (Formula) formulaMap
                            .get(internedFormula);
                    error.append("Warning: Existing formula appears in ");
                    error.append(existingFormula.sourceFile + " at line ");
                    error.append(existingFormula.startLine + "<br />");
                    error.append("<p>");
                    System.out.println("Duplicate detected.");
                    errors.add(error.toString());
                }
            }
        }
    }
    */
    /** ***************************************************************
     * Reload all the KB constituents.
     */
    public String reload() {
        
        ArrayList<String> newConstituents = new ArrayList<String>();
        synchronized (this.getTerms()) {
            Iterator<String> ci = constituents.iterator();
            while (ci.hasNext()) {
                String cName = ci.next();                     
                if (!cName.endsWith(_cacheFileSuffix))  // Recompute cached data
                    newConstituents.add(cName);
            }
            constituents.clear();
            formulas.clear();
            formulaMap.clear();
            terms.clear();
            clearFormatMaps();
            errors.clear();
            Iterator<String> nci = newConstituents.iterator();
            if (nci.hasNext()) System.out.println("INFO in KB.reload()");
            while (nci.hasNext()) {
                String cName = (String) nci.next();                
                addConstituent(cName);
                //addConstituent(cName, false, false, false);
            }            
            checkArity();  // Reperform arity checks on everything            
            kbCache = new KBcache(this);
            kbCache.buildCaches();
            // If cache == yes, write the cache file.
            if (KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes")) {
                kbCache.writeCacheFile();
            }
            // At this point, we have reloaded all constituents, have
            // rebuilt the relation caches, and, if cache == yes, have
            // written out the _Cache.kif file.  Now we reload the
            // inference engine.
            loadEProver();
        }
        return "";
    }

    /** ***************************************************************
     * Write a KIF file consisting of all the formulas in the
     * knowledge base.
     * @param fname - the name of the file to write, including full path.
     */
    public void writeFile(String fname) throws IOException {

        FileWriter fr = null;
        PrintWriter pr = null;
        HashSet<String> formulaSet = new HashSet<String>();

        Iterator<String> it = formulas.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            ArrayList<String> list = formulas.get(key);
            for (int i = 0; i < list.size(); i++) {
                String s = list.get(i);
                formulaSet.add(s);
            }
        }
        try {
            fr = new FileWriter(fname);
            pr = new PrintWriter(fr);
            it = formulaMap.keySet().iterator();
            while (it.hasNext()) {
                String s = (String) it.next();
                pr.println(s);
                pr.println();
            }
        }
        catch (java.io.IOException e) {
        	System.out.println("Error in KB.writeFile(): Error writing file " + fname);
        	e.printStackTrace();
        }
        finally {
            if (pr != null) { pr.close(); }
            if (fr != null) { fr.close(); }
        }
    }

    /** *************************************************************
     * Create the XML configuration element.
     */
    public SimpleElement writeConfiguration() {

        SimpleElement se = new SimpleElement("kb");
        se.setAttribute("name",name);
        for (int i = 0; i < constituents.size(); i++) {
            SimpleElement constituent = new SimpleElement("constituent");
            String filename = (String) constituents.get(i);
            filename = KBmanager.escapeFilename(filename);
            constituent.setAttribute("filename",filename);
            se.addChildElement(constituent);
        }
        return se;
    }

    /** *************************************************************
     * A HashMap for holding compiled regular expression patterns.
     * The map is initialized by calling compilePatterns().
     */
    private static HashMap<String,ArrayList> REGEX_PATTERNS = null;

    /** ***************************************************************
     * This method returns a compiled regular expression Pattern
     * object indexed by key.
     *
     * @param key A String that is the retrieval key for a compiled
     * regular expression Pattern.
     *
     * @return A compiled regular expression Pattern instance.
     */
    public static Pattern getCompiledPattern(String key) {

        if (StringUtil.isNonEmptyString(key) && (REGEX_PATTERNS != null)) {
            ArrayList al = (ArrayList) REGEX_PATTERNS.get(key);
            if (al != null)
                return (Pattern) al.get(0);
        }
        return null;
    }

    /** ***************************************************************
     * This method returns the int value that identifies the regular
     * expression binding group to be returned when there is a match.
     *
     * @param key A String that is the retrieval key for the binding
     * group index associated with a compiled regular expression
     * Pattern.
     *
     * @return An int that indexes a binding group.
     */
    public static int getPatternGroupIndex(String key) {

        if (StringUtil.isNonEmptyString(key) && (REGEX_PATTERNS != null)) {
            ArrayList al = (ArrayList) REGEX_PATTERNS.get(key);
            if (al != null) 
                return ((Integer)al.get(1)).intValue();            
        }
        return -1;
    }

    /** ***************************************************************
     * This method compiles and stores regular expression Pattern
     * objects and binding group indexes as two cell ArrayList
     * objects.  Each ArrayList is indexed by a String retrieval key.
     *
     * @return void
     */
    private static void compilePatterns() {

        if (REGEX_PATTERNS == null) {
            REGEX_PATTERNS = new HashMap<String,ArrayList>();
            String[][] patternArray =
                { { "row_var", "\\@ROW\\d*", "0" },
                  // { "open_lit", "\\(\\w+\\s+\\?\\w+\\s+.\\w+\\s*\\)", "0" },
                  { "open_lit", "\\(\\w+\\s+\\?\\w+[a-zA-Z_0-9-?\\s]+\\)", "0" },
                  { "pred_var_1", "\\(holds\\s+(\\?\\w+)\\W", "1" },
                  { "pred_var_2", "\\((\\?\\w+)\\W", "1" },
                  { "var_with_digit_suffix", "(\\D+)\\d*", "1" }
                };
            String pName   = null;
            Pattern p      = null;
            Integer groupN = null;
            ArrayList pVal = null;
            for (int i = 0 ; i < patternArray.length ; i++) {
                pName  = patternArray[i][0];
                p      = Pattern.compile(patternArray[i][1]);
                groupN = new Integer(patternArray[i][2]);
                pVal   = new ArrayList();
                pVal.add(p);
                pVal.add(groupN);
                REGEX_PATTERNS.put(pName, pVal);
            }
        }
        return;
    }

    /** ***************************************************************
     * This method finds regular expression matches in an input string
     * using a compiled Pattern and binding group index retrieved with
     * patternKey.  If the ArrayList accumulator is provided, match
     * results are added to it and it is returned.  If accumulator is
     * not provided (is null), then a new ArrayList is created and
     * returned if matches are found.
     *
     * @param input The input String in which matches are sought.
     *
     * @param patternKey A String used as the retrieval key for a
     * regular expression Pattern object, and an int index identifying
     * a binding group.
     *
     * @param accumulator An optional ArrayList to which matches are
     * added.  Note that if accumulator is provided, it will be the
     * return value even if no new matches are found in the input
     * String.
     *
     * @return An ArrayList, or null if no matches are found and an
     * accumulator is not provided.
     */
    public static ArrayList<String> getMatches(String input, String patternKey, ArrayList<String> accumulator) {

        ArrayList<String> ans = null;
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
                    while (m.find()) {
                        String rv = m.group(gidx);
                        if (StringUtil.isNonEmptyString(rv)) {
                            if (ans == null) 
                                ans = new ArrayList<String>();                            
                            if (!(ans.contains(rv))) 
                                ans.add(rv);                            
                        }
                    }
                }
            }
        }
        return ans;
    }

    /** ***************************************************************
     * This method finds regular expression matches in an input string
     * using a compiled Pattern and binding group index retrieved with
     * patternKey, and returns the results, if any, in an ArrayList.
     *
     * @param input The input String in which matches are sought.
     *
     * @param patternKey A String used as the retrieval key for a
     * regular expression Pattern object, and an int index identifying
     * a binding group.
     *
     * @return An ArrayList, or null if no matches are found.
     */
    public static ArrayList<String> getMatches(String input, String patternKey) {
        return KB.getMatches(input, patternKey, null);
    }

    /** ***************************************************************
     * This method retrieves Formulas by asking the query expression
     * queryLit, and returns the results, if any, in an ArrayList.
     *
     * @param queryLit The query, which is assumed to be a List
     * (atomic literal) consisting of a single predicate and its
     * arguments.  The arguments could be variables, constants, or a
     * mix of the two, but only the first constant encountered in a
     * left to right sweep over the literal will be used in the actual
     * query.
     *
     * @return An ArrayList of Formula objects, or an empty ArrayList
     * if no answers are retrieved.
     */
    public ArrayList<Formula> askWithLiteral(List<String> queryLit) {

        ArrayList<Formula> ans = new ArrayList<Formula>();
        if ((queryLit instanceof List) && !(queryLit.isEmpty())) {
            String pred = (String) queryLit.get(0);
            if (pred.equals("instance")
                && isVariable(queryLit.get(1))
                && !(isVariable(queryLit.get(2)))) {
                String className = queryLit.get(2);
                String inst = null;
                String fStr = null;
                Formula f = null;
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
            else if (pred.equals("valence")
                     && isVariable((String)queryLit.get(1))
                     && isVariable((String)queryLit.get(2))) {
                TreeSet<String> ai = getAllInstances("Relation");
                Iterator<String> it = ai.iterator();
                int valence = 0;
                while (it.hasNext()) {
                	String inst = (String) it.next();
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
                String term = null;
                for (int i = 1 ; i < qlLen ; i++) {
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

    /** ***************************************************************
     * This method retrieves formulas by asking the query expression
     * queryLit, and returns the results, if any, in an ArrayList.
     *
     * @param queryLit The query, which is assumed to be an atomic
     * literal consisting of a single predicate and its arguments.
     * The arguments could be variables, constants, or a mix of the
     * two, but only the first constant encountered in a left to right
     * sweep over the literal will be used in the actual query.
     *
     * @return An ArrayList of Formula objects, or an empty ArrayList
     * if no answers are retrieved.
     */
    public ArrayList<Formula> askWithLiteral(Formula queryLit) {
    	
        List<String> input = queryLit.literalToArrayList();
        return askWithLiteral(input);
    }

    /** ***************************************************************
     * This method retrieves the upward transitive closure of all Class
     * names contained in the input set.  The members of the input set are
     * not included in the result set.
     *
     * @param classNames A Set object containing SUO-KIF class names
     * (Strings).
     *
     * @return A Set of SUO-KIF class names, which could be empty.
     
    public Set getAllSuperClasses(Set classNames) {

        Set ans = new HashSet();
        try {
            if ((classNames instanceof Set) && !(classNames.isEmpty())) {
                List accumulator = new ArrayList();
                List working = new ArrayList();
                String arg2 = null;
                working.addAll(classNames);
                while (!(working.isEmpty())) {
                    for (int i = 0 ; i < working.size() ; i++) {
                        List nextLits = askWithRestriction(1,(String) working.get(i),0,"subclass");
                        if (nextLits != null) {
                            for (int j = 0 ; j < nextLits.size() ; j++) {
                                Formula f = (Formula) nextLits.get(j);
                                arg2 = f.getArgument(2);
                                if (! working.contains(arg2)) {
                                    accumulator.add(arg2);
                                }
                            }
                        }
                    }
                    ans.addAll(accumulator);
                    working.clear();
                    working.addAll(accumulator);
                    accumulator.clear();
                }
            }
        }
        catch (Exception ex) {
            logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return ans;
    } */

    /** ***************************************************************
     * This method retrieves the upward transitive closure of all Class
     * names contained in the input set.  The members of the input set are
     * not included in the result set.
     *
     * @param classNames A Set object containing SUO-KIF class names
     * (Strings).
     *
     * @return A Set of SUO-KIF class names, which could be empty.
     */
    public Set<String> getAllSuperClasses(Set<String> classNames) {

        Set<String> ans = new HashSet<String>();
        Iterator<String> it = classNames.iterator();
        while (it.hasNext()) {
            String term = it.next();
            ans.addAll(kbCache.getParentClasses(term));
        }
        return ans;    
    }
    
    /** ***************************************************************
     * This method retrieves all subclasses of className, using both
     * class and predicate (subrelation) subsumption.
     *
     * @param className The name of a Class.
     *
     * @return A Set of String terms, which could be empty.
     
    public TreeSet<String> getAllSubClassesWithPredicateSubsumption(String className) {

        TreeSet<String> ans = new TreeSet<String>();
        if (StringUtil.isNonEmptyString(className)) {
            // Get all subrelations of subrelation.
            Set<String> metarelations = kbCache.getCachedRelationValues("subrelation", "subrelation", 2, 1);
            metarelations.add("subrelation");
            HashSet<String> relations = new HashSet<String>();
            Iterator<String> it = metarelations.iterator();        
            while (it.hasNext()) {   // Get all subrelations of subclass.
                String pred = it.next();
                relations.addAll(kbCache.getCachedRelationValues(pred, "subclass", 2, 1));
            }
            relations.add("subclass");
            // Get all subclasses of className.
            Iterator<String> it2 = relations.iterator();
            while (it2.hasNext()) {
                String pred = (String) it2.next();
                ans.addAll(kbCache.getCachedRelationValues(pred, className, 2, 1));
            }
        }
        return ans;
    }
*/
    /** ***************************************************************
     * This method retrieves all superclasses of className, using both
     * class and predicate (subrelation) subsumption.
     *
     * @param className The name of a Class.
     *
     * @return A Set of String terms, which could be empty.
     
    public TreeSet<String> getAllSuperClassesWithPredicateSubsumption(String className) {

    	TreeSet<String> ans = new TreeSet<String>();
        if (StringUtil.isNonEmptyString(className)) {
            // Get all subrelations of subrelation.
            HashSet<String> metarelations = kbCache.getCachedRelationValues("subrelation", "subrelation", 2, 1);
            metarelations.add("subrelation");
            HashSet<String> relations = new HashSet<String>();
            Iterator<String> it = metarelations.iterator();
            String pred = null;            
            while (it.hasNext()) {   // Get all subrelations of subclass
                pred = it.next();
                relations.addAll(kbCache.getCachedRelationValues(pred, "subclass", 2, 1));
            }
            relations.add("subclass");            
            it = relations.iterator();   
            while (it.hasNext()) {   // Get all superclasses of className
                pred = it.next();
                ans.addAll(kbCache.getCachedRelationValues(pred, className, 1, 2));
            }
        }
        return ans;
    }
*/
    /** ***************************************************************
     * This method retrieves all instances of className, using both
     * predicate (subrelation) and class subsumption.
     *
     * @param className The name of a Class
     *
     * @return A Set of String terms, which could be empty
     
    public Set<String> getAllInstancesWithPredicateSubsumption(String className) {
        return getAllInstancesWithPredicateSubsumption(className, true);
    }

    /** ***************************************************************
     * This method retrieves all instances of className, using
     * predicate (subrelation) subsumption if gatherSubclasses is
     * false, and using both predicate and subclass subsumption if
     * gatherSubclasses is true.
     *
     * @param className The name of a Class
     *
     * @param gatherSubclasses If true, all subclasses of className
     * are gathered and their local instances are added to the set of
     * returned terms
     *
     * @return A Set of String terms, which could be empty
     
    public TreeSet<String> getAllInstancesWithPredicateSubsumption(String className,
                                                       boolean gatherSubclasses) {
    	TreeSet<String> ans = new TreeSet<String>();
        if (StringUtil.isNonEmptyString(className)) {
            // Get all subrelations of subrelation.
        	HashSet<String> metarelations = kbCache.getCachedRelationValues("subrelation", "subrelation", 2, 1);
            metarelations.add("subrelation");
            TreeSet<String> relations = new TreeSet<String>();
            Iterator<String> it = metarelations.iterator();
            // Get all subrelations of instance.
            while (it.hasNext()) {
                String pred = it.next();
                relations.addAll(kbCache.getCachedRelationValues(pred, "instance", 2, 1));
            }
            relations.add("instance");
            // Get all "local" or "immediate" instances of className, using instance and 
            // all gathered subrelations of instance.
            Iterator<String> itr = relations.iterator();;
            while (itr.hasNext()) {
                String pred = itr.next();
                ans.addAll(kbCache.getCachedRelationValues(pred, className, 2, 1));
            }
            if (gatherSubclasses) {
                Set<String> subclasses = getAllSubClassesWithPredicateSubsumption(className);
                // subclasses.add(className);
                Iterator<String> its = subclasses.iterator();
                while (its.hasNext()) {
                    String cl = its.next();
                    itr = relations.iterator();
                    while (itr.hasNext()) {
                        String pred = itr.next();
                        ans.addAll(getTermsViaAskWithRestriction(0,pred,2,cl,1));
                    }
                }
            }
        }
        return ans;
    }
*/
    /** ***************************************************************
     * This method retrieves all classes of which term is an instance,
     * using both class and predicate (subrelation) subsumption.
     *
     * @param term The name of a SUO-KIF term.     
     * @return A Set of terms (class names), which could be empty.
     
    public Set<String> getAllInstanceOfsWithPredicateSubsumption(String term) {

        TreeSet<String> ans = new TreeSet<String>();
        if (StringUtil.isNonEmptyString(term)) {
            // Get all subrelations of subrelation.
            Set<String> metarelations = kbCache.getCachedRelationValues("subrelation", "subrelation", 2, 1);
            metarelations.add("subrelation");
            Set<String> relations = new HashSet<String>();
            Iterator<String> it = metarelations.iterator();
            // Get all subrelations of instance.
            while (it.hasNext()) {
            	String pred = it.next();
                relations.addAll(kbCache.getCachedRelationValues(pred, "instance", 2, 1));
            }
            relations.add("instance");
            // Get all classes of which term is an instance.
            Set<String> classes = new HashSet<String>();
            it = relations.iterator();
            while (it.hasNext()) {
            	String pred = it.next();
                classes.addAll(kbCache.getCachedRelationValues(pred, term, 1, 2));
            }
            ans.addAll(classes);
            // Get all superclasses of classes.
            it = classes.iterator();
            while (it.hasNext()) {
            	String cl = it.next();
                ans.addAll(getAllSuperClassesWithPredicateSubsumption(cl));
            }
        }
        return ans;
    }
*/
    /** ***************************************************************
     * This method retrieves the downward transitive closure of all Class
     * names contained in the input set.  The members of the input set are
     * not included in the result set.
     *
     * @param classNames A Set object containing SUO-KIF class names
     * (Strings).     
     * @return A Set of SUO-KIF class names, which could be empty.     
    private Set getAllSubClasses(Set classNames) {

        Set ans = new HashSet();
        try {
            if ((classNames instanceof Set) && !(classNames.isEmpty())) {
                List accumulator = new ArrayList();
                List working = new ArrayList();
                String arg1 = null;
                working.addAll(classNames);
                while (!(working.isEmpty())) {
                    for (int i = 0 ; i < working.size() ; i++) {
                        List nextLits = askWithRestriction(2,(String) working.get(i),0,"subclass");
                        if (nextLits != null) {
                            for (int j = 0 ; j < nextLits.size() ; j++) {
                                Formula f = (Formula) nextLits.get(j);
                                arg1 = f.getArgument(1);
                                if (! working.contains(arg1))
                                    accumulator.add(arg1);
                            }
                        }
                    }
                    ans.addAll(accumulator);
                    working.clear();
                    working.addAll(accumulator);
                    accumulator.clear();
                }
            }
        }
        catch (Exception ex) {
            logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return ans;
    } */
    
    /** ***************************************************************
     * This method retrieves the downward transitive closure of all Class
     * names contained in the input set.  The members of the input set are
     * not included in the result set.
     *
     * @param classNames A Set object containing SUO-KIF class names
     * (Strings).     
     * @return A Set of SUO-KIF class names, which could be empty.
     
    private Set<String> getAllSubClasses(Set<String> classNames) {

        Set<String> ans = new HashSet<String>();
        Iterator<String> it = classNames.iterator();
        while (it.hasNext()) {
            String term = it.next();
            ans.addAll(kbCache.getChildClasses(term));
        }
        return ans;
    }
*/
    /** ***************************************************************
     * This method retrieves all instances of the classes named in the
     * input set.
     *
     * @param classNames A Set of String, containing SUO-KIF class names    
     * @return A TreeSet, possibly empty, containing SUO-KIF constant names.
     */
    protected TreeSet<String> getAllInstances(TreeSet<String> classNames) {

        TreeSet<String> ans = new TreeSet<String>();
        if ((classNames instanceof TreeSet) && !classNames.isEmpty()) {
            String name = null;
            Iterator<String> it = classNames.iterator();
            while (it.hasNext()) {
                name = it.next();
                ans.addAll(kbCache.getInstances(name));
            }
        }
        return ans;
    }

    /** ***************************************************************
     * This method retrieves all instances of the class named in the
     * input String.
     *
     * @param className The name of a SUO-KIF Class.     
     * @return A TreeSet, possibly empty, containing SUO-KIF constant names.
     */
    public TreeSet<String> getAllInstances(String className) {
        
        if (StringUtil.isNonEmptyString(className)) {
            TreeSet<String> input = new TreeSet<String>();
            input.add(className);
            return getAllInstances(input);
        }
        return new TreeSet<String>();
    }

    /** ***************************************************************
     * This method tries to find or compute a valence for the input
     * relation.
     *
     * @param relnName A String, the name of a SUO-KIF Relation.     
     * @return An int value. -1 means that no valence value could be
     * found.  0 means that the relation is a VariableArityRelation.
     * 1-5 are the standard SUO-KIF valence values.
     */
    public int getValence(String relnName) {

        if (kbCache.valences.get(relnName) == null) {
            if (Formula.isLogicalOperator(relnName)) // logical operator arity is checked in KIF.parse()
                return -1;
            System.out.println("Error in KB.getValence(): No valence found for " + relnName);
            return -1;
        }
        else
            return kbCache.valences.get(relnName);
    }

    /** ***************************************************************     
     * @return an ArrayList containing all predicates in this KB.     
     */
    public ArrayList<String> collectPredicates() {
        return new ArrayList<String>(kbCache.instances.get("Predicate"));
    }

    /** ***************************************************************     
     * @param obj Any object
     *
     * @return true if obj is a String representation of a LISP empty
     * list, else false.     
     */
    public static boolean isEmptyList(Object obj) {
        return (StringUtil.isNonEmptyString(obj) && Formula.empty((String) obj));
    }

    /** ***************************************************************     
     * A utility method.
     *
     * @param objList A list of anything.     
     * @param label An optional label (String), or null.     
     * @return void     
     
    public static void printAll(List objList, String label) {
        
        if (objList instanceof List) {
            Iterator it = objList.iterator();
            while (it.hasNext()) {
                if (StringUtil.isNonEmptyString(label)) 
                    logger.fine(label + ": " + it.next());                
                else 
                    logger.fine(it.next().toString());                
            }
        }
        return;
    }
*/
    /** ***************************************************************     
     * A static utility method.
     *
     * @param obj Presumably, a String.     
     * @return true if obj is a SUO-KIF variable, else false.     
     */
    public static boolean isVariable(String obj) {
        
        if (StringUtil.isNonEmptyString(obj)) {
            return (obj.startsWith("?") || obj.startsWith("@"));
        }
        return false;
    }

    /** ***************************************************************     
     * A static utility method.
     *
     * @param obj A String.     
     * @return true if obj is a SUO-KIF logical quantifier, else
     * false.     
     */
    public static boolean isQuantifier(String obj) {

        return (StringUtil.isNonEmptyString(obj)
                && (obj.equals("forall") || obj.equals("exists")));
    }

    /** ***************************************************************     
     * A static utility method.
     *
     * @param obj Presumably, a String.     
     * @return true if obj is a SUO-KIF commutative logical operator,
     * else false.     
     */
    public static boolean isCommutative(String obj) {

        return (StringUtil.isNonEmptyString(obj)
                && (obj.equals("and") || obj.equals("or")));
    }

    /** *************************************************************
     * Hyperlink terms identified with '&%' to the URL that brings up
     * that term in the browser.  Handle (and ignore) suffixes on the
     * term.  For example "&%Processes" would get properly linked to
     * the term "Process", if present in the knowledge base.
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
            int i = -1;
            int j = -1;
            int start = 0;
            String term = "";
            String formToPrint = "";
            while ((start < sb.length()) && ((i = sb.indexOf("&%", start)) != -1)) {
                sb.delete(i, (i + 2));
                j = i;
                while ((j < sb.length())
                        && !Character.isWhitespace(sb.charAt(j))
                        && sb.charAt(j) != '"')
                    j++;
                while (j > i) {
                    term = sb.substring(i,j);
                    if (containsTerm(term))
                        break;
                    j--;
                }
                if (j > i) {
                    formToPrint = DocGen.getInstance(this.name).showTermName(this,term,language);
                    StringBuilder hsb = new StringBuilder("<a href=\"");
                    hsb.append(href);
                    hsb.append(isStaticFile
                               ? StringUtil.toSafeNamespaceDelimiter(term)
                               : term);
                    hsb.append(suffix);
                    hsb.append("\">");
                    hsb.append(formToPrint);
                    hsb.append("</a>");
                    sb.replace(i, j, hsb.toString());
                    start = (i + hsb.length());
                }
            }
            formatted = sb.toString();
        }
        return formatted;
    }

    /** *************************************************************
     *  Pull all the formulas into one TreeSet of Strings.
     
    private TreeSet<String> collectAllFormulas(HashMap<String,ArrayList<Formula>> forms) {

        TreeSet<String> ts = new TreeSet<String>();
        ArrayList<ArrayList<Formula>> al = new ArrayList<ArrayList<Formula>>(forms.values());

        Iterator<ArrayList<Formula>> it = al.iterator();
        while (it.hasNext()) {
            ArrayList<Formula> al2 = (ArrayList<Formula>) it.next();
            Iterator<Formula> it2 = al2.iterator();
            while (it2.hasNext())
                ts.add(it2.next().theFormula);
        }
        return ts;
    }
*/
    /** *************************************************************
     *  Pull all the formulas in an ArrayList into one TreeSet of Strings.
     
    private TreeSet collectFormulasFromList(ArrayList forms) {

        TreeSet ts = new TreeSet();
        for (Iterator it = forms.iterator(); it.hasNext();)
            ts.add(((Formula) it.next()).theFormula);
        return ts;
    }
*/
    /** *************************************************************
     * Save the contents of the current KB to a file.
     */
    public String writeInferenceEngineFormulas(TreeSet<String> forms) {

        FileWriter fw = null;
        PrintWriter pw = null;
        String filename = null;
        try {
            String inferenceEngine = KBmanager.getMgr().getPref("inferenceEngine");
            if (StringUtil.isNonEmptyString(inferenceEngine)) {
            	File executable = new File(inferenceEngine);
                if (executable.exists()) {
                    File dir = executable.getParentFile();
                    File file = new File(dir, (this.name + "-v.kif"));
                    filename = file.getCanonicalPath();
                    fw = new FileWriter(filename);
                    pw = new PrintWriter(fw);
                    Iterator<String> it = forms.iterator();
                    while (it.hasNext()) {
                        pw.println(it.next());
                        pw.println();
                    }
                }
                else
                	System.out.println("Error in KB.writeInferenceEngineFormulas(): no executable " + inferenceEngine);
            }
        }
        catch (IOException ioe) {
        	System.out.println("Error in KB.writeInferenceEngineFormulas(): writing file: " + filename);
        	System.out.println(ioe.getMessage());
            ioe.printStackTrace();
        }
        finally {
        	try {
	            if (pw != null) { pw.close(); }
	            if (fw != null) { fw.close(); }
        	}
            catch (Exception ex) {
            }
        }
        return filename;
    }

    /** *************************************************************
     * Creates InferenceEngine and loads all of the constituents into it.
     *
     * @param factory Factory object used to create new InferenceEngine.
     * @return InferenceEngine object with all constituents loaded.
     */
    public InferenceEngine createInferenceEngine(InferenceEngine.EngineFactory factory) {

        InferenceEngine res = null;
        try {
            if (!formulaMap.isEmpty()) {
                TreeSet<String> forms = preProcess((HashSet<String>) formulaMap.keySet());
                String filename = writeInferenceEngineFormulas(forms);
                boolean vFileSaved = !StringUtil.emptyString(filename);
                if (!vFileSaved)
                    System.out.println("Error in KB.createInterenceEngine(): new -v.kif file not written");
                if (vFileSaved && !factory.getClass().getName().equals("com.articulate.sigma.STP$STPEngineFactory")) 
                    res = factory.createFromKBFile(filename);                
                if (factory.getClass().getName().equals("com.articulate.sigma.STP$STPEngineFactory")) 
                    res = factory.createWithFormulas(forms);                
            }
        }
        catch (Exception e) {
        	System.out.println("Error in KB.createInterenceEngine():" + e.getMessage());
            e.printStackTrace();
        }
        return res;
    }

    /** *************************************************************
     *  Starts EProver and collects, preprocesses and loads all of the
     *  constituents into it.
     */
    public void loadEProver() {

        KBmanager mgr = KBmanager.getMgr();
        try {
            if (!formulaMap.isEmpty()) {
            	HashSet<String> formulaStrings = new HashSet<String>();
            	formulaStrings.addAll(formulaMap.keySet());
                TreeSet<String> forms = preProcess(formulaStrings);
                String filename = writeInferenceEngineFormulas(forms);
                boolean vFileSaved = StringUtil.isNonEmptyString(filename);
                if (eprover != null) 
                	eprover.terminate();                
                eprover = null;
                SUMOKBtoTPTPKB skb = new SUMOKBtoTPTPKB();
                skb.kb = this;
                String tptpFilename = KBmanager.getMgr().getPref("kbDir") + File.separator + this.name + ".tptp";
                skb.writeTPTPFile(tptpFilename, true);
                if (StringUtil.isNonEmptyString(mgr.getPref("inferenceEngine")) && vFileSaved) 
                	eprover = new EProver(mgr.getPref("inferenceEngine"),tptpFilename);          
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        if (eprover == null)
            mgr.setError(mgr.getError() + "\n<br/>No local inference engine is available\n<br/>");
        return;
    }

    /** ***************************************************************
     * Preprocess the knowledge base to TPTP.  This includes "holds"
     * prefixing, ticking nested formulas, expanding row variables, and
     * translating mathematical relation operators.  All the real work
     * is done in Formula.preProcess().
     * @return a TreeSet of Strings.
     */
    public TreeSet<String> preProcess(HashSet<String> forms) {

        TreeSet<String> newTreeSet = new TreeSet<String>();
        KBmanager mgr = KBmanager.getMgr();
        kbCache.kb = this;
        kbCache.buildCaches();
        boolean tptpParseP = mgr.getPref("TPTP").equalsIgnoreCase("yes");
        Iterator<String> it = forms.iterator(); 
        while (it.hasNext()) {
            String form = it.next();
            Formula f = formulaMap.get(form);
            if (f == null) {
            	System.out.println("Error in KB.preProcess(): No formula for : " + form);
            	continue;
            }
            //System.out.println("INFO in KB.preProcess(): form : " + form);
            //System.out.println("INFO in KB.preProcess(): f : " + f);
            FormulaPreprocessor fp = new FormulaPreprocessor();
            ArrayList<Formula> processed = fp.preProcess(f,false, this);   // not queries
            if (tptpParseP) {
                try {
                	SUMOformulaToTPTPformula stptp = new SUMOformulaToTPTPformula();
                	stptp._f = f;
                	stptp.tptpParse(f,false, this, processed);   // not a query
                }
                catch (ParseException pe) {
                    String er = ("Error in KB.preProcess() " + pe.getMessage() + " at line "
                                 + f.startLine + " in file " + f.sourceFile);
                    mgr.setError(mgr.getError() + "\n<br/>" + er + "\n<br/>");
                }
                catch (IOException ioe) {
                    System.out.println("Error in KB.preProcess(): " + ioe.getMessage());
                }
            }
            errors.addAll(f.getErrors());
            Formula p = null;
            Iterator<Formula> itp = processed.iterator();
            while (itp.hasNext()) {
                p = itp.next();
                if (StringUtil.isNonEmptyString(p.theFormula)) {
                    newTreeSet.add(p.theFormula);
                    errors.addAll(p.getErrors());
                }
                else {
                	System.out.println("Error in KB.preProcess(): empty formula: " + p);
                }
            }
        }
        //kbCache.clearSortalTypeCache();
        return newTreeSet;
    }

    /** *************************************************************
     * List all terms that don't have an externalImage link
     
    private void termsWithNoPictureLinks() {

       synchronized (this.getTerms()) {
            for (Iterator it = getTerms().iterator(); it.hasNext();) {
                String term = (String) it.next();
                ArrayList al = askWithRestriction(0,"externalImage",1,term);
                if (al == null || al.size() < 1)
                	System.out.println(term + " has no picture links.");
            }
        }
    }
*/
    /** *************************************************************
     *  Turn SUMO into a semantic network by extracting all ground
     *  binary relations, turning all higher arity relations into a
     *  set of binary relations, and making all term co-occurring in
     *  an axiom to be related with a general "link" relation. Also
     *  use the subclass hierarchy to relate all parents of terms in
     *  domain statements, through the relation itself but with a
     *  suffix designating it as a separate relation. Convert SUMO
     *  terms to WordNet synsets.
     */
    private void generateSemanticNetwork() {

        TreeSet resultSet = new TreeSet();
        Iterator it = formulaMap.keySet().iterator();
        while (it.hasNext()) {          // look at all formulas in the KB
            String formula = (String) it.next();
            Formula f = new Formula();
            f.read(formula);
            if (f.isRule() || f.car().equals("instance") || f.car().equals("subclass")) 
                continue;            
            StreamTokenizer_s st = new StreamTokenizer_s(new StringReader(formula));
            KIF.setupStreamTokenizer(st);
            ArrayList al = new ArrayList();
            boolean firstToken = true;
            String predicate = "link";
            try {
                while (st.nextToken() != StreamTokenizer_s.TT_EOF) {
                    if (st.ttype == StreamTokenizer_s.TT_WORD) {
                        String token = st.sval;
                        if (firstToken && !Formula.isLogicalOperator(token))
                            predicate = token;
                        if (Formula.isTerm(token) && !Formula.isLogicalOperator(token) && !firstToken)
                            al.add(token);
                        if (firstToken)
                            firstToken = false;
                    }
                }
            } 
            catch (IOException ioe) {
            	System.out.println("INFO in KB.generateSemanticNetwork(): Error parsing: " + formula);
            }
            if (al != null && al.size() > 1 && !predicate.equals("link") &&
                !predicate.equals("instance") && !predicate.equals("subclass") &&
                !predicate.equals("domain")) {
                for (int i = 0; i < al.size(); i++) {
                    String firstTerm = (String) al.get(i);
                    for (int j = i; j < al.size(); j++) {
                        String otherTerm = (String) al.get(j);
                        if (!firstTerm.equals(otherTerm)) {
                            ArrayList synsets1 = (ArrayList) WordNet.wn.SUMOHash.get(firstTerm);
                            ArrayList synsets2 = (ArrayList) WordNet.wn.SUMOHash.get(otherTerm);
                            if (synsets1 != null & synsets2 != null) {
                                for (int k = 0; k < synsets1.size(); k++) {
                                    String firstSynset = (String) synsets1.get(k);
                                    if (firstSynset.endsWith("=")) {
                                        for (int l = 0; l < synsets2.size(); l++) {
                                            String secondSynset = (String) synsets2.get(l);
                                            if (secondSynset.endsWith("=")) {
                                                char firstLetter = WordNetUtilities.posNumberToLetter(firstSynset.charAt(0));
                                                char secondLetter = WordNetUtilities.posNumberToLetter(secondSynset.charAt(0));                                                
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** *************************************************************
     *  Find all cases of where (instance A B) (instance B C) as
     *  well as all cases of where (instance A B) (instance B C)
     *  (instance C D).  Report true if any such cases are found,
     *  false otherwise.
     */
    public boolean instanceOfInstanceP() {

        boolean result = false;
        Iterator it = terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            ArrayList<Formula> al = askWithRestriction(0,"instance",1,term);
            for (int i = 0; i < al.size(); i++) {
                Formula f = (Formula) al.get(i);
                String term2 = f.getArgument(2);
                if (Formula.atom(term2)) {
                    ArrayList<Formula> al2 = askWithRestriction(0,"instance",1,term2);
                    if (al2.size() > 0)
                        result = true;
                    for (int j = 0; j < al2.size(); j++) {
                        Formula f2 = (Formula) al2.get(j);
                        String term3 = f2.getArgument(2);
                        if (Formula.atom(term3)) {
                            ArrayList<Formula> al3 = askWithRestriction(0,"instance",1,term3);
                            for (int k = 0; k < al3.size(); k++) {
                                Formula f3 = (Formula) al3.get(k);
                                String term4 = f3.getArgument(2);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /** *************************************************************
     */
    public void writeDisplayText(String displayFormatPredicate, String displayTermPredicate, 
            String language, String fname) throws IOException {
        
        PrintWriter pr = null;
        try {
            pr = new PrintWriter(new FileWriter(fname, false));            
            //get all formulas that have the display predicate as the predicate           
            ArrayList<Formula> formats = this.askWithRestriction(0, displayFormatPredicate, 1, language);
            ArrayList<Formula> terms = this.askWithRestriction(0, displayTermPredicate, 1, language);            
            HashMap<String,String> termMap = new HashMap<String,String>();            
            for (int i = 0; i < terms.size(); i++) {
                Formula term = terms.get(i);                
                String key = term.getArgument(2);
                String value = term.getArgument(3);                
                if (key != "" && value != "") 
                    termMap.put(key, value);                
            }            
            for (int i = 0; i < formats.size(); i++) {
                Formula format = formats.get(i);                
                // This is the current predicate whose format we are keeping track of. 
                String key = format.getArgument(2);
                String value = format.getArgument(3);                
                if (key != "" && value != "") {                
                    // This basically gets all statements that use the current predicate in the 0 position
                    ArrayList<Formula> predInstances = this.ask("arg", 0, key);                    
                    for(int j=0; j < predInstances.size(); j++) {
                        StringBuilder sb = new StringBuilder();
                        String displayText = String.copyValueOf(value.toCharArray());                        
                        Formula f = predInstances.get(j);
                        ArrayList arguments = f.argumentsToArrayList(0);      
                        sb.append(key);
                        sb.append(",");           
                        // check if each of the arguments for the statements is to be replaced in its
                        // format statement.
                        for (int k = 1; k < arguments.size(); k++) {
                            String argName = f.getArgument(k);
                            String term = (String) termMap.get(argName);
                            term = StringUtil.removeEnclosingQuotes(term);
                            String argNum = "%" + String.valueOf(k);
                        
                            // also, add the SUMO Concept that is replaced in the format
                            if (displayText.contains(argNum)) {
                                sb.append(argName);
                                sb.append(",");
                                displayText = displayText.replace(argNum, term);                                
                            }                                                                
                        }                                             
                        sb.append(displayText);                                               
                        // resulting line will be something like:
                        // <predicate>, <argument_0>, ..., <argument_n>, <display_text>
                        // note: argument_0 to argument_n is only placed there if their 
                        // termFormat is used in the display_text.
                        pr.println(sb.toString());                        
                    }                    
                }
            }            
            
        }
        catch (java.io.IOException e) {
        	System.out.println(e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e) {
        	System.out.println(e.getMessage());
            e.printStackTrace();
        }
        finally {
            if (pr != null) 
                pr.close();            
        }
    }

    /** *************************************************************
     */
    public static void generateTPTPTestAssertions() {
        
        try {
            int counter = 0;
            System.out.println("INFO in KB.main()");
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB("SUMO");
            System.out.println("INFO in KB.main(): printing predicates");
            Iterator<String> it = kb.terms.iterator();
            while (it.hasNext()) {
                String term = it.next();
                if (Character.isLowerCase(term.charAt(0)) && kb.kbCache.valences.get(term) <= 2) {
                    /*
                    ArrayList<Formula> forms = kb.askWithRestriction(0,"domain",1,term);
                    for (int i = 0; i < forms.size(); i++) {
                        String argnum = forms.get(i).getArgument(2);
                        String type = forms.get(i).getArgument(3);
                        if (argnum.equals("1"))
                            System.out.print("(instance Foo " + type + "),");
                        if (argnum.equals("2"))
                            System.out.print("(instance Bar " + type + ")");
                    }
                    */
                    String argType1 = kb.getArgType(term,1);
                    String argType2 = kb.getArgType(term,2);
                    if (argType1 != null && argType2 != null) {
                        System.out.print("fof(local_" + counter++ + ",axiom,(s__" + term + "(s__Foo,s__Bar))).|");
                        System.out.print("fof(local_" + counter++ + ",axiom,(s__instance(s__Foo,s__" + argType1 + "))).|");
                        System.out.println("fof(local_" + counter++ + ",axiom,(s__instance(s__Bar,s__" + argType2 + "))).");
                    }                    
                }
            }
        } 
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    /** *************************************************************
     * Note this simply assumes that initial lower case terms are relations.
     */
    public static void generateRelationList() {
        
        try {
            System.out.println("INFO in KB.main()");
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB("SUMO");
            System.out.println("INFO in KB.main(): printing predicates");
            Iterator<String> it = kb.terms.iterator();
            while (it.hasNext()) {
                String term = it.next();
                if (Character.isLowerCase(term.charAt(0)))
                    System.out.println(term);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    /** *************************************************************
     */
    public static void main(String[] args) {

        //generateTPTPTestAssertions();       
        // testTPTP(args);
        
          try {
              KBmanager.getMgr().initializeOnce();
              KB kb = KBmanager.getMgr().getKB("SUMO");
              kb.writeTerms();
          } 
          catch (IOException ioe ) {
              System.out.println(ioe.getMessage());
          }

        
        
        //kb.generateSemanticNetwork();
        //kb.generateRandomProof();
        //kb.instanceOfInstanceP();

        /*
        String foo = "(rel bar \"test\")";
        Formula f = new Formula();
        f.read(foo);
        System.out.println(f.getArgument(2).equals("\"test\""));
    */
    }
}
