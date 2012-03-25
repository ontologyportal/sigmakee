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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** *****************************************************************
 *  Contains methods for reading, writing knowledge bases and their
 *  configurations.  Also contains the inference engine process for
 *  the knowledge base.
 */
public class KB {

    private static boolean DEBUG = false;
	private static Logger logger;

	private boolean isVisible = true;
	
    /** The inference engine process for this KB. Deprecated.   */
    public InferenceEngine inferenceEngine;

    /** The collection of inference engines for this KB. */
    public TreeMap<String, InferenceEngine> engineMap = new TreeMap<String, InferenceEngine>();

    /** The name of the knowledge base. */
    public String name;

    /** An ArrayList of Strings that are the full
     * canonical pathnames of the files that comprise the KB. */
    public ArrayList<String> constituents = new ArrayList<String>();

    /** The natural language in which axiom paraphrases should be presented. */
    public String language = "EnglishLanguage";

    /** The location of preprocessed KIF files, suitable for loading into Vampire. */
    public String kbDir = null;

    /** A HashMap of HashSets, which contain all the parent classes of a given class. */
    public HashMap<String,HashSet<String>> parents = new HashMap<String,HashSet<String>>();

    /** A HashMap of HashSets, which contain all the child classes of a given class. */
    public HashMap<String,HashSet<String>> children = new HashMap<String,HashSet<String>>();

    /** A HashMap of HashSets, which contain all the disjoint classes of a given class. */
    public HashMap<String,HashSet<String>> disjoint = new HashMap<String,HashSet<String>>();

    /** A threshold limiting the number of values that will be added to
     * a single relation cache table. */
    private static final long MAX_CACHE_SIZE = 1000000;

    /** A List of the names of cached transitive relations. */
    private List<String> cachedTransitiveRelationNames = Arrays.asList("subclass",
                                                               "subset",
                                                               "subrelation",
                                                               "subAttribute",
                                                               "subOrganization",
                                                               "subCollection",
                                                               "subProcess",
                                                               "geographicSubregion",
                                                               "geopoliticalSubdivision");

    /** A List of the names of cached reflexive relations. */
    private List<String> cachedReflexiveRelationNames = Arrays.asList("subclass",
                                                              "subset",
                                                              "subrelation",
                                                              "subAttribute",
                                                              "subOrganization",
                                                              "subCollection",
                                                              "subProcess");

    /** A List of the names of cached relations. */
    private List<String> cachedRelationNames = Arrays.asList("instance", "disjoint");

    /** An ArrayList of RelationCache objects. */
    private ArrayList relationCaches = new ArrayList();

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
    public Map<String, Formula> formulaMap = new LinkedHashMap<String, Formula>();

    /** A HashMap of ArrayLists of Formulae, containing all the
     * formulae in the KB.  Keys are the formula itself, a formula ID, and term
     * indexes created in KIF.createKey().  */
    public Map<String, ArrayList<String>> formulas = new HashMap<String, ArrayList<String>>();

    /** The natural language formatting strings for relations in the
     *  KB. It is a HashMap of language keys and HashMap values.
     *  The interior HashMap is term name keys and String values. */
    private HashMap formatMap = new HashMap();

    /** The natural language strings for terms in the KB. It is a
     *  HashMap of language keys and HashMap values. The interior
     *  HashMap is term name keys and String values. */
    private HashMap termFormatMap = new HashMap();

    /** Errors and warnings found during loading of the KB constituents. */
    public TreeSet errors = new TreeSet();

    /** Future: If true, the contents of the KB have been modified without updating the caches */
    public boolean modifiedContents = false;

    /** If true, assertions of the form (predicate x x) will be
     * included in the relation cache tables. */
    private boolean cacheReflexiveAssertions = false;

    /** A global counter used to ensure that constants created by
     *  instantiateFormula() are unique. */
    private int gensym = 0;

    /** *************************************************************
     * Constructor which takes the name of the KB and the location
     * where KBs preprocessed for Vampire should be placed.
     */
    public KB(String n, String dir) {
        name = n;
        kbDir = dir;
        try {
            KBmanager mgr = KBmanager.getMgr();
            if (mgr != null) {
                // initRelationCaches();
                String loadCelt = mgr.getPref("loadCELT");
                if ((loadCelt != null) && loadCelt.equalsIgnoreCase("yes")) {
                    celt = new CELT();
                }
            }
            
            logger = Logger.getLogger(this.getClass().getName());
        }
        catch (IOException ioe) {
            logger.warning("Error in KB(): " + ioe.getMessage());
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
                // initRelationCaches();
                String loadCelt = mgr.getPref("loadCELT");
                if ((loadCelt != null) && loadCelt.equalsIgnoreCase("yes")) {
                    celt = new CELT();
                }
            }
            
            logger = Logger.getLogger(this.getClass().getName());
            
        }
        catch (IOException ioe) {
            logger.warning("Error in KB(): " + ioe.getMessage());
            celt = null;
        }
    }

    /** ************************************************************
     * Returns a synchronized SortedSet of Strings, which are all
     * the terms in the KB. */
    public SortedSet<String> getTerms() {
        return this.terms;
    }
    
    /** ***************************************************
    *Return ArrayList of all nonrelTerms in an ArrayList
    *
    *@return An ArrayList of nonrelTerms
    */
    public ArrayList<String> getAllNonRelTerms(ArrayList list) {
    	
    	ArrayList<String> nonRelTerms = new ArrayList();
    	Iterator<String> itr = list.iterator();
        while(itr.hasNext()) {
        	String t = itr.next();
        	if (Character.isUpperCase(t.charAt(0))) 
       			nonRelTerms.add(t);
        }
       	return nonRelTerms;
    }
    
    /** ******************************************************
    *Return ArrayList of all relTerms in an ArrayList
    *
    *@return An ArrayList of relTerms
    */
    public ArrayList<String> getAllRelTerms(ArrayList list) {
    	
    	ArrayList<String> relTerms = new ArrayList();
    	Iterator<String> itr = list.iterator();
        while(itr.hasNext()) {
        	String t = itr.next();
        	if (Character.isLowerCase(t.charAt(0))) 
        		relTerms.add(t);
        }
        return relTerms;
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
    *Takes a term (interpreted as a Regular Expression) and returns true
    *if any term in the KB has a match with the RE.
    *
    *@param term A String
    *@return true or false.
    */
    public boolean containsRE(String term) {
    	
    	return (getREMatch(term).size()>0 ? true : false);  
    }  

    /** **************************************************
    *Takes a term (interpreted as a Regular Expression) and returns an ArrayList
    *containing every term in the KB that has a match with the RE.
    *
    *@param term A String
    *@return An ArrayList of terms that have a match to term
    */
    public ArrayList<String> getREMatch(String term) {
    	try {
    		Pattern p = Pattern.compile(term);
    		ArrayList<String> matchesList = new ArrayList();
    		Iterator<String> itr = getTerms().iterator();
    		while(itr.hasNext()) {
    			String t = itr.next();
    			Matcher m = p.matcher(t);
    			if (m.matches()) 
    				matchesList.add(t);
    		}
    		return matchesList;
    	} catch (PatternSyntaxException ex) {
    		ArrayList<String> err = new ArrayList();
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
     * Sets the private instance variable cacheReflexiveAssertions to
     * val.
     * @param val true or false
     * @return void
     */
    public void setCacheReflexiveAssertions(boolean val) {
        cacheReflexiveAssertions = val;
        return;
    }

    /** *************************************************************
     * If this method returns true, then reflexive assertions will be
     * included in the relation caches built when Sigma starts up.
     *
     * @return true or false
     */
    public boolean getCacheReflexiveAssertions() {
        return cacheReflexiveAssertions;
    }

    /** *************************************************************
     * @return An ArrayList of relation names (Strings).
     */
    private ArrayList getCachedRelationNames() {
        ArrayList relationNames = new ArrayList();
        try {
        	LinkedHashSet<String> reduced = new LinkedHashSet<String>(cachedRelationNames);
            reduced.addAll(getCachedTransitiveRelationNames());
            reduced.addAll(getCachedSymmetricRelationNames());
            relationNames.addAll(reduced);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return relationNames;
    }

    /** *************************************************************
     * Returns a list of the names of cached transitive relations.
     *
     * @return An ArrayList of relation names (Strings).
     */
    private ArrayList getCachedTransitiveRelationNames() {
        ArrayList ans = new ArrayList(cachedTransitiveRelationNames);
        try {
            Set trset = getAllInstancesWithPredicateSubsumption("TransitiveRelation");
            String name = null;
            for (Iterator it = trset.iterator(); it.hasNext();) {
                name = (String) it.next();
                if (!ans.contains(name)) ans.add(name);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Returns a list of the names of cached symmetric relations.
     *
     * @return An ArrayList of relation names (Strings).
     */
    private ArrayList getCachedSymmetricRelationNames() {
        ArrayList ans = new ArrayList();
        try {
            Set symmset = getAllInstancesWithPredicateSubsumption("SymmetricRelation");
            // symmset.addAll(getTermsViaPredicateSubsumption("subrelation",2,"inverse",1,true));
            symmset.add("inverse");
            ans.addAll(symmset);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * @return An ArrayList of relation names (Strings).
     */
    private ArrayList getCachedReflexiveRelationNames() {
        ArrayList ans = new ArrayList();
        try {
            List allcached = getCachedRelationNames();
            List reflexives = new ArrayList(cachedReflexiveRelationNames);
            String name = null;
            Iterator it = null;
            for (it = getAllInstancesWithPredicateSubsumption("ReflexiveRelation").iterator();
                 it.hasNext();) {
                name = (String) it.next();
                if (!reflexives.contains(name)) reflexives.add(name);
            }
            for (it = reflexives.iterator(); it.hasNext();) {
                name = (String) it.next();
                if (allcached.contains(name)) ans.add(name);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * @return An ArrayList of RelationCache objects.
     */
    protected ArrayList getRelationCaches() {
        return this.relationCaches;
    }

    /** *************************************************************
     * Returns the platform-specific line separator String.
     */
    protected String getLineSeparator() {
        return System.getProperty("line.separator");
    }

    /** ***************************************************************
     * This Map is used to cache sortal predicate argument type data
     * whenever Formula.findType() or Formula.getTypeList() will be
     * called hundreds of times inside KB.preProcess(), or to
     * accomplish another expensive computation tasks.  The Map is
     * cleared after each use in KB.preProcess(), but may retain its
     * contents when used in other contexts.
     */
    private HashMap<String, Object> sortalTypeCache = null;

    /** ***************************************************************
     * Returns the Map is used to cache sortal predicate argument type
     * data whenever Formula.findType() or Formula.getTypeList() will
     * be called hundreds of times inside KB.preProcess(), or to
     * accomplish another expensive computation tasks.  The Map is
     * cleared after each use in KB.preProcess(), but may retain its
     * contents when used in other contexts.
     */
    public HashMap<String, Object> getSortalTypeCache() {
        if (sortalTypeCache == null) {
            sortalTypeCache = new HashMap<String, Object>();
        }
        return sortalTypeCache;
    }

    /** ***************************************************************
     * Clears the Map returned by KB.getSortalTypeCache().
     *
     * @return void
     */
    protected void clearSortalTypeCache() {
        try {
            logger.info("Clearing " + getSortalTypeCache().size() + " entries");
            Object obj = null;
            for (Iterator it = getSortalTypeCache().values().iterator(); it.hasNext();) {
                obj = it.next();
                if (obj instanceof Collection) {
                    ((Collection) obj).clear();
                }
            }
            getSortalTypeCache().clear();

        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return;
    }

    /** ***************************************************************
     * Clears the Map returned by KB.getSortalTypeCache(), or creates
     * it if it does not already exist.
     *
     * @return void
     */
    protected void resetSortalTypeCache() {
        if (getSortalTypeCache().isEmpty()) {
        	logger.info("KB.getSortalTypeCache() == " + getSortalTypeCache());
        }
        else {
            clearSortalTypeCache();
        }
        return;
    }

    /** *************************************************************
     * Get an ArrayList of Strings containing the language identifiers
     * of available natural language formatting templates.
     *
     * @return an ArrayList of Strings containing the language identifiers
     */
    public ArrayList availableLanguages() {

        ArrayList al = new ArrayList();
        ArrayList col = ask("arg", 0, "format");
        ArrayList col2 = ask("arg", 0, "termFormat");
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
        return al;
    }

    /** *************************************************************
      * Initializes all RelationCaches.  Creates the RelationCache
     * objects if they do not yet exist, and clears all existing
     * RelationCache objects if clearExistingCaches is true.
     *
     * @param clearExistingCaches If true, all existing RelationCache
     * maps are cleared and the List of RelatonCaches is cleared, else
     * all existing RelationCache objects and their contents are
     * reused
     *
     * @return void
     */
    protected void initRelationCaches(boolean clearExistingCaches) {

    	logger.entering("KB", "initRelationCaches", "clearExistingCaches = " + clearExistingCaches);

        Iterator it = null;
        if (clearExistingCaches) {
            // Clear all cache maps.
            for (it = getRelationCaches().iterator(); it.hasNext();) {
                RelationCache rc = (RelationCache) it.next();
                rc.clear();
            }

            // Discard all cache maps.
            getRelationCaches().clear();
        }
        List symmetric = getCachedSymmetricRelationNames();
        it = getCachedRelationNames().iterator();
        String relname = null;
        while (it.hasNext()) {
            relname = (String) it.next();
            getRelationCache(relname, 1, 2);

            System.out.println("  " + relname);

            // We put each symmetric relation -- disjoint and a
            // few others -- into just one RelationCache table
            // apiece.  All transitive binary relations are cached
            // in two RelationCaches, one that looks "upward" from
            // the keys, and another that looks "downward" from
            // the keys.
            if (!symmetric.contains(relname)) {
                getRelationCache(relname, 2, 1);
            }
        }

        // We still set these legacy variables.  Eventually, they
        // should be removed.
        parents  = getRelationCache("subclass", 1, 2);
        children = getRelationCache("subclass", 2, 1);
        disjoint = getRelationCache("disjoint", 1, 2);

        logger.exiting("KB", "initRelationCaches");

        return;
    }

    /** *************************************************************
     * Returns the RelationCache object identified by the input
     * arguments: relation name, key argument position, and value
     * argument position.
     *
     * @param relName The name of the cached relation.
     *
     * @param keyArg An int value that indicates the argument position
     * of the cache keys.
     *
     * @param valueArg An int value that indicates the argument
     * position of the cache values.
     *
     * @return a RelationCache object, or null if there is no cache
     * corresponding to the input arguments.
     */
    private RelationCache getRelationCache(String relName, int keyArg, int valueArg) {
        RelationCache result = null;
        try {
            if (StringUtil.isNonEmptyString(relName)) {
                RelationCache cache = null;
                for (Iterator it = getRelationCaches().iterator(); it.hasNext();) {
                    cache = (RelationCache) it.next();
                    if (cache.getRelationName().equals(relName)
                        && (cache.getKeyArgument() == keyArg)
                        && (cache.getValueArgument() == valueArg)) {
                        result = cache;
                        break;
                    }
                }
                /*                 */
                if (result == null) {
                    cache = new RelationCache(relName, keyArg, valueArg);
                    getRelationCaches().add(cache);
                    result = cache;
                }
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Writes the cache .kif file, and then calls addConstituent() so
     * that the file can be processed and loaded by the inference
     * engine.
     *
     * @return a String indicating any errors, or the empty string if
     * there were no errors.
     */
	public void cache() {

    	logger.entering("KB", "cache");
        String result = "";
        FileWriter fr = null;
        try {
            boolean isClosureComputed = false;
            List caches = getRelationCaches();
            Iterator it = null;
            Iterator it2 = null;
            Iterator it3 = null;
            String relation = null;
            String arg1 = null;
            String arg2 = null;
            Set valSet = null;
            String tuple = null;
            RelationCache rc = null;
            if (caches != null) {
                it = caches.iterator();
                while (it.hasNext()) {
                    rc = (RelationCache) it.next();
                    if (rc.getIsClosureComputed()) {
                        isClosureComputed = true;
                        break;
                    }
                }

                // Don't bother writing the cache file if we have not
                // at least partially computed the closure of the
                // various cached relations.
                if (isClosureComputed) {
                    File dir = new File(kbDir);
                    File f = new File(dir, (this.name + _cacheFileSuffix));
                    logger.finer("User cache file == " + f.getCanonicalPath());
                    if (f.exists()) {
                        logger.finer("Deleting " + f.getCanonicalPath());
                        f.delete();
                        if (f.exists()) {
                            logger.finer("Could not delete " + f.getCanonicalPath());
                        }
                    }
                    String filename = f.getCanonicalPath();
                    fr = new FileWriter(f, true);
                    logger.finer("Appending statements to " + f.getCanonicalPath());
                    it = caches.iterator();
                    while (it.hasNext()) {
                        rc = (RelationCache) it.next();
                        if (rc.getKeyArgument() == 1) {
                            relation = rc.getRelationName();

                            // Unfortunately, there are just too many
                            // disjoint classes to consider writing
                            // them to a file, or to consider having
                            // Vampire try to load the assertions.
                            if (!relation.equals("disjoint")) {
                                it2 = rc.keySet().iterator();
                                while (it2.hasNext()) {
                                    arg1 = (String) it2.next();
                                    valSet = (Set) rc.get(arg1);
                                    it3 = valSet.iterator();
                                    while (it3.hasNext()) {
                                        arg2 = (String) it3.next();
                                        StringBuilder sb = new StringBuilder("(");
                                        sb.append(relation);
                                        sb.append(" ");
                                        sb.append(arg1);
                                        sb.append(" ");
                                        sb.append(arg2);
                                        sb.append(")");
                                        tuple = sb.toString();
                                        if (!formulaMap.containsKey(tuple.intern())
                                            && (getCacheReflexiveAssertions()
                                                || !arg1.equals(arg2))) {
                                            fr.write(tuple);
                                            fr.write(getLineSeparator());
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (fr != null) {
                        fr.close();
                        fr = null;
                    }
                    constituents.remove(filename);
                    logger.fine("Adding " + filename);
					addConstituent(filename, false, false, true);
                    KBmanager.getMgr().writeConfiguration();
                }
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        finally {
            try {
                if (fr != null) {
                    fr.close();
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        logger.exiting("KB", "cache", result);
    }

    /** *************************************************************
     * Adds one value to the cache, indexed under keyTerm.
     *
     * @param cache The RelationCache object to be updated.
     * @param keyTerm The String that is the key for this entry.
     * @param valueTerm The String that is the value for this entry.
     * @return The int value 1 if a new entry is added, else 0.
     */
    private int addRelationCacheEntry(RelationCache cache, String keyTerm, String valueTerm) {
        int count = 0;
        if ((cache != null)
            && StringUtil.isNonEmptyString(keyTerm)
            && StringUtil.isNonEmptyString(valueTerm)) {
            Set valueSet = (Set) cache.get(keyTerm);
            if (valueSet == null) {
                valueSet = new HashSet();
                cache.put(keyTerm, valueSet);
            }
            if (valueSet.add(valueTerm)) {
                count++;
            }
        }
        return count;
    }

    /** *************************************************************
     * Returns the HashSet indexed by term in the RelationCache
     * identified by relation, keyArg, and valueArg.
     *
     * @param relation A String, the name of a relation
     * @param term A String (key) that indexes a HashSet
     * @param keyArg An int value that, with relation and valueArg,
     * identifies a RelationCache
     * @param valueArg An int value that, with relation and keyArg,
     * identifies a RelationCache
     *
     * @return A HashSet, which could be empty
     */
    public HashSet getCachedRelationValues(String relation,
                                           String term,
                                           int keyArg,
                                           int valueArg) {
        HashSet ans = new HashSet();
        try {
            RelationCache cache = getRelationCache(relation, keyArg, valueArg);
            if (cache != null) {
                HashSet values = (HashSet) cache.get(term);
                if (values != null) ans.addAll(values);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

	public void checkArity() {
		ArrayList<String> toRemove = new ArrayList<String>();

		System.out.println("Performing Arity Check");
		if (formulaMap != null && formulaMap.size() > 0) {
			Iterator<String> formulas = formulaMap.keySet().iterator();

			while (formulas.hasNext()) {
				Formula f = (Formula) formulaMap.get(formulas.next());

				if (!f.hasCorrectArity(this)) {
					errors.add("Formula in " + f.sourceFile
							+ " rejected due to arity error: <br/>"
							+ f.theFormula);
					toRemove.add(f.theFormula);
				}
			}
		}

		for (int i = 0; i < toRemove.size(); i++) {
			formulaMap.remove(toRemove.get(i));
		}

	}

    /** *************************************************************
     * This method computes the transitive closure for the relation
     * identified by relationName.  The results are stored in the
     * RelationCache object for the relation and "direction" (looking
     * from the arg1 keys toward arg2 parents, or looking from the
     * arg2 keys toward arg1 children).
     *
     * @param relationName The name of a relation
     *
     * @return void
     */
    private void computeTransitiveCacheClosure(String relationName) {

        long t1 = System.currentTimeMillis();
        logger.entering("KB", "computerTransitiveCacheClosure", "relationName = " + relationName);
        long count = 0L;
        try {
            if (getCachedTransitiveRelationNames().contains(relationName)) {
                RelationCache c1 = getRelationCache(relationName, 1, 2);
                RelationCache c2 = getRelationCache(relationName, 2, 1);
                if ((c1 != null) && (c2 != null)) {
                    RelationCache inst1 = null;
                    RelationCache inst2 = null;
                    boolean isSubrelationCache = relationName.equals("subrelation");
                    if (isSubrelationCache) {
                        inst1 = getRelationCache("instance", 1, 2);
                        inst2 = getRelationCache("instance", 2, 1);
                    }
                    Set c1Keys = c1.keySet();
                    Iterator it1 = null;
                    Iterator it2 = null;
                    String keyTerm = null;
                    String valTerm = null;
                    Set valSet = null;
                    Set valSet2 = null;
                    Object[] valArr = null;
                    boolean changed = true;
                    while (changed) {
                        changed = false;
                        it1 = c1Keys.iterator();
                        while (it1.hasNext()) {
                            keyTerm = (String) it1.next();
                            if (StringUtil.emptyString(keyTerm)) {
                                logger.warning("Error in KB.computeTransitiveCacheClosure("
                                                   + relationName + ") \n   keyTerm == "
                                                   + ((keyTerm == null) ? null : "\""
                                                      + keyTerm + "\""));
                            }
                            else {
                                valSet = (Set) c1.get(keyTerm);
                                valArr = valSet.toArray();
                                for (int i = 0 ; i < valArr.length ; i++) {
                                    valTerm = (String) valArr[i];

                                    valSet2 = (Set) c1.get(valTerm);
                                    if (valSet2 != null) {
                                        it2 = valSet2.iterator();
                                        while (it2.hasNext() && (count < MAX_CACHE_SIZE)) {
                                            if (valSet.add(it2.next())) {
                                                changed = true;
                                                count++;
                                            }
                                        }
                                    }

                                    if (count < MAX_CACHE_SIZE) {
                                        valSet2 = (Set) c2.get(valTerm);
                                        if (valSet2 == null) {
                                            valSet2 = new HashSet();
                                            c2.put(valTerm, valSet2);
                                        }
                                        if (valSet2.add(keyTerm)) {
                                            changed = true;
                                            count++;
                                        }
                                    }
                                }
                                // Here we try to ensure that instances of
                                // Relation have at least some entry in the
                                // "instance" caches, since this
                                // information is sometimes considered
                                // redundant and so could be left out of
                                // .kif files.
                                if (isSubrelationCache) {
                                    valTerm = "Relation";
                                    if (keyTerm.endsWith("Fn")) {
                                        valTerm = "Function";
                                    }
                                    else {
                                        String nsdelim = StringUtil.getKifNamespaceDelimiter();
                                        int ndidx = keyTerm.indexOf(nsdelim);
                                        String stripped = keyTerm;
                                        if (ndidx > -1) {
                                            stripped = keyTerm.substring(nsdelim.length() + ndidx);
                                        }
                                        if (Character.isLowerCase(stripped.charAt(0))
                                            && !keyTerm.contains("(")) {
                                            valTerm = "Predicate";
                                        }
                                    }
                                    addRelationCacheEntry(inst1, keyTerm, valTerm);
                                    addRelationCacheEntry(inst2, valTerm, keyTerm);
                                }
                            }
                        }
                        if (changed) {
                            c1.setIsClosureComputed(true);
                            c2.setIsClosureComputed(true);
                        }
                    }
                }
            }

            /*
              if (relationName.equals("subclass")) {
              printParents();
              printChildren();
              }
            */
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        if (count > 0) {
            logger.fine(count
                      + " "
                      + relationName
                      + " entries computed in "
                      + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");
        }
        
        logger.exiting("KB", "computeTransitiveCacheClosure");
        
        return;
    }

    /** *************************************************************
     * This method computes the closure for the cache of the instance
     * relation, in both directions.
     *
     * @return void
     */
    private void computeInstanceCacheClosure() {

        long t1 = System.currentTimeMillis();
        logger.entering("KB", "computeInstanceCacheClosure");
        long count = 0L;
        try {
            RelationCache ic1 = getRelationCache("instance", 1, 2);
            RelationCache ic2 = getRelationCache("instance", 2, 1);
            RelationCache sc1 = getRelationCache("subclass", 1, 2);
            Set ic1KeySet = ic1.keySet();
            Iterator it1 = ic1KeySet.iterator();
            Iterator it2 = null;
            String ic1KeyTerm = null;
            Set ic1ValSet = null;
            Object[] ic1ValArr = null;
            String ic1ValTerm = null;
            Set sc1ValSet = null;
            Set ic2ValSet = null;

            while (it1.hasNext()) {
                ic1KeyTerm = (String) it1.next();
                ic1ValSet = (Set) ic1.get(ic1KeyTerm);
                ic1ValArr = ic1ValSet.toArray();
                for (int i = 0 ; i < ic1ValArr.length ; i++) {
                    ic1ValTerm = (String) ic1ValArr[i];
                    if (ic1ValTerm != null) {
                        sc1ValSet = (Set) sc1.get(ic1ValTerm);
                        if (sc1ValSet != null) {
                            it2 = sc1ValSet.iterator();
                            while (it2.hasNext() && (count < MAX_CACHE_SIZE)) {
                                if (ic1ValSet.add(it2.next())) {
                                    count++;
                                }
                            }
                        }
                    }
                }
                if (count < MAX_CACHE_SIZE) {
                    it2 = ic1ValSet.iterator();
                    while (it2.hasNext()) {
                        ic1ValTerm = (String) it2.next();
                        ic2ValSet = (Set) ic2.get(ic1ValTerm);
                        if (ic2ValSet == null) {
                            ic2ValSet = new HashSet();
                            ic2.put(ic1ValTerm, ic2ValSet);
                        }
                        if (ic2ValSet.add(ic1KeyTerm)) {
                            count++;
                        }
                    }
                }
            }

            ic1.setIsClosureComputed(true);
            ic2.setIsClosureComputed(true);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        if (count > 0) 
        	logger.info(count + " instance entries computed in " + (System.currentTimeMillis() - t1) / 1000.0 + " seconds");
        
        logger.exiting("KB", "computeInstanceCacheClosure");
        
        return;
    }

    /** *************************************************************
     * This method computes the closure for the caches of symmetric
     * relations.  As currently implemented, it really applies to only
     * disjoint.
     */
    private void computeSymmetricCacheClosure(String relationName) {

        long t1 = System.currentTimeMillis();
        logger.entering("KB", "computeSymmetricCacheClosure", "relationName = " + relationName);

        long count = 0L;
        try {
            RelationCache dc1 = getRelationCache(relationName, 1, 2);
            RelationCache sc2 = (relationName.equals("disjoint")
                                 ? getRelationCache("subclass", 2, 1)
                                 : null);
            if (sc2 != null) {
                Set dc1KeySet      = null;
                Object[] dc1KeyArr = null;
                String dc1KeyTerm  = null;
                Set dc1ValSet      = null;
                Object[] dc1ValArr = null;
                String dc1ValTerm  = null;
                Set sc2ValSet      = null;
                Iterator it        = null;
                String sc2ValTerm  = null;
                Set dc1ValSet2     = null;
                // int passes = 0;
                // One pass is sufficient.
                boolean changed = true;
                while (changed) {
                    changed = false;
                    dc1KeySet = dc1.keySet();
                    dc1KeyArr = dc1KeySet.toArray();
                    for (int i = 0; (i < dc1KeyArr.length) && (count < MAX_CACHE_SIZE); i++) {
                        dc1KeyTerm = (String) dc1KeyArr[i];
                        dc1ValSet = (Set) dc1.get(dc1KeyTerm);
                        dc1ValArr = dc1ValSet.toArray();
                        for (int j = 0 ; j < dc1ValArr.length ; j++) {
                            dc1ValTerm = (String) dc1ValArr[j];
                            sc2ValSet = (Set) sc2.get(dc1ValTerm);
                            if (sc2ValSet != null) {
                                if (dc1ValSet.addAll(sc2ValSet)) {
                                    changed = true;
                                }
                            }
                        }

                        sc2ValSet = (Set) sc2.get(dc1KeyTerm);
                        if (sc2ValSet != null) {
                            it = sc2ValSet.iterator();
                            while (it.hasNext()) {
                                sc2ValTerm = (String) it.next();
                                dc1ValSet2 = (Set) dc1.get(sc2ValTerm);
                                if (dc1ValSet2 == null) {
                                    dc1ValSet2 = new HashSet();
                                    dc1.put(sc2ValTerm, dc1ValSet2);
                                }
                                if (dc1ValSet2.addAll(dc1ValSet)) {
                                    changed = true;
                                }
                            }
                        }
                        it = dc1.values().iterator();
                        count = 0;
                        while (it.hasNext()) {
                            dc1ValSet = (HashSet) it.next();
                            count += dc1ValSet.size();
                        }
                    }

                    if (changed)
                        dc1.setIsClosureComputed(true);
                    // System.out.println("  " + count + " disjoint entries after pass " + ++passes);
                }
            }
            // printDisjointness();
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        
        if (count > 0L)
            logger.info(count + " " + relationName + " entries computed in "
                               + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");
        
        logger.exiting("KB", "computeSymmetricCacheClosure");
        return;
    }

    private HashMap relnsWithRelnArgs = null;

    /** *************************************************************
     * This method builds a cache of all Relations in the current KB
     * for which at least one argument must be filled by a relation
     * name (or a variable denoting a relation name).  This method
     * should be called only after the subclass cache has been built.
     */
    private void cacheRelnsWithRelnArgs() {

        long t1 = System.currentTimeMillis();
        logger.entering("KB", "cacheRelnsWithRelnArgs");

        try {
            if (relnsWithRelnArgs == null) {
                relnsWithRelnArgs = new HashMap();
            }
            relnsWithRelnArgs.clear();
            Set relnClasses = getCachedRelationValues("subclass", "Relation", 2, 1);
            if (relnClasses != null)
                relnClasses.add("Relation");

            // System.out.println("  relnClasses == " + relnClasses);

            if (relnClasses != null) {
                ArrayList formulas = null;
                Iterator it = relnClasses.iterator();
                Iterator it2 = null;
                String relnClass = null;
                Formula f = null;
                String reln = null;
                int argPos = -1;
                int valence = -1;
                boolean[] signature = null;
                while (it.hasNext()) {
                    relnClass = (String) it.next();
                    formulas = askWithRestriction(3, relnClass, 0, "domain");

                    // System.out.println("  formulas == " + formulas);

                    if (formulas != null) {
                        it2 = formulas.iterator();
                        while (it2.hasNext()) {
                            f = (Formula) it2.next();
                            reln = f.getArgument(1);
                            valence = getValence(reln);
                            if (valence < 1) {
                                valence = Formula.MAX_PREDICATE_ARITY;
                            }
                            signature = (boolean[]) relnsWithRelnArgs.get(reln);
                            if (signature == null) {
                                signature = new boolean[ valence + 1 ];
                                for (int j = 0 ; j < signature.length ; j++) {
                                    signature[j] = false;
                                }
                                relnsWithRelnArgs.put(reln, signature);
                            }
                            argPos = Integer.parseInt(f.getArgument(2));
                            try {
                                signature[argPos] = true;
                            }
                            catch (Exception e1) {
                            	logger.warning( "Error in KB.cacheRelnsWithRelnArgs(): reln == " 
                            					   + reln
                                                   + ", argPos == " + argPos
                                                   + ", signature == " + signature);
                                throw e1;
                            }
                        }
                    }
                }
                // This is a kluge.  "format" (and "termFormat", which
                // is not directly relevant here) should be defined as
                // predicates (meta-predicates) in Merge.kif, or in
                // some language-independent paraphrase scaffolding
                // .kif file.
                signature = (boolean[]) relnsWithRelnArgs.get("format");
                if (signature == null) {
                    signature = new boolean[4];
                    // signature = { false, false, true, false };
                    for (int i = 0 ; i < signature.length ; i++) {
                        signature[i] = (i == 2);
                    }
                    relnsWithRelnArgs.put("format", signature);
                }
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        int rwraSize = relnsWithRelnArgs.size();
        if (rwraSize > 0) {
            logger.info(rwraSize +
            			" relation argument entries computed in "
            			+ (System.currentTimeMillis() - t1) / 1000.0
             			+ " seconds");
        }
        logger.exiting("KB", "cacheRelnsWithRelnArgs");
        return;
    }

    /** *************************************************************
     * Returns a boolean[] if the input relation has at least one
     * argument that must be filled by a relation name.
     */
    protected boolean[] getRelnArgSignature(String relation) {

        if (relnsWithRelnArgs != null) {
            return (boolean[]) relnsWithRelnArgs.get(relation);
        }
        return null;
    }

    /** *************************************************************
     *  */
    private HashMap relationValences = new HashMap();

    /** *************************************************************
     *  */
    private void cacheRelationValences() {

        long t1 = System.currentTimeMillis();
        logger.entering("KB", "cacheRelationValences");

        try {
            Set relations = getCachedRelationValues("instance", "Relation", 2, 1);
            if (relations != null) {
                List<String> namePrefixes = Arrays.asList("VariableArity",
                                                          "Unary",
                                                          "Binary",
                                                          "Ternary",
                                                          "Quaternary",
                                                          "Quintary");
                int nplen = namePrefixes.size();
                RelationCache ic1 = getRelationCache("instance", 1, 2);
                RelationCache ic2 = getRelationCache("instance", 2, 1);
                String reln = null;
                String className = null;
                int valence = -1;
                StringBuilder sb = null;
                for (Iterator it = relations.iterator(); it.hasNext();) {
                    reln = (String) it.next();

                    // Here we evaluate getValence() to build the
                    // relationValences cache, and use its return
                    // value to fill in any info that might be missing
                    // from the "instance" cache.
                    valence = getValence(reln);
                    className = null;
                    if ((valence > -1) && (valence < nplen)) {
                        sb = new StringBuilder();
                        if (reln.endsWith("Fn")) {
                            if ((valence > 0) && (valence < 5)) {
                                sb.append(namePrefixes.get(valence));
                                sb.append("Function");
                            }
                        }
                        else {
                            sb.append(namePrefixes.get(valence));
                            sb.append("Relation");
                        }
                        className = sb.toString();
                        if (StringUtil.isNonEmptyString(className)) {
                            addRelationCacheEntry(ic1, reln, className);
                            addRelationCacheEntry(ic2, className, reln);
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        
        logger.info("RelationValences == " + relationValences.size() + " entries" +
        			"  " + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds elapsed time");
        
        logger.exiting("KB", "cacheRelationValences");
        return;
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
     * value can be obtained
     */
    public String getArgType(String reln, int argPos) {

        // long t1 = System.currentTimeMillis();
        // System.out.println("ENTER KB.getArgType(" + reln + ", " + argPos + ")");
        String className = null;
        try {
            String argType = Formula.findType(argPos, reln, this);
            if (StringUtil.isNonEmptyString(argType)) {
                if (argType.endsWith("+"))
                    argType = "SetOrClass";
                className = argType;
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        // System.out.println("EXIT KB.getArgType(" + reln + ", " + argPos + ")");
        // System.out.println("  className == " + className);
        // System.out.println("  "  + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds elapsed time");
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
        try {
            String argType = Formula.findType(argPos, reln, this);
            if (StringUtil.isNonEmptyString(argType))
                className = argType;
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return className;
    }

    /** *************************************************************
     * This list contains the names of SUMO Relations known to be
     * instances of VariableArityRelation in at least some domain.  It
     * is used only for TPTP generation, and should
     * <strong>not</strong> be relied upon for any other purpose,
     * since it is not automatically generated and might be out of
     * date.
     */
    public static final List VA_RELNS = Arrays.asList("AssignmentFn",
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

    /** *************************************************************
     * Returns true if relnName is the name of a relation that is
     * known to be, or computed to be, a variable arity relation.
     *
     * @param relnName A String that names a SUMO Relation (Predicate
     * or Function).
     *
     * @return boolean
     */
    public boolean isVariableArityRelation(String relnName) {
        boolean ans = false;
        try {
            ans = (VA_RELNS.contains(relnName)
                   || (getValence(relnName) == 0)
                   || isInstanceOf(relnName, "VariableArityRelation"));
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return ans;
    }

    protected ArrayList listRelnsWithRelnArgs() {
        if (relnsWithRelnArgs != null)
            return new ArrayList(relnsWithRelnArgs.keySet());
        return null;
    }

    protected boolean containsRelnWithRelnArg(String input) {
        try {
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
        }
        catch (Exception ex) { 
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return false;
    }

    /** *************************************************************
     * debugging utility
     */
    private void printParents() {

    	logger.finer("Printing parents");
        Iterator it = parents.keySet().iterator();
        while (it.hasNext()) {
            String parent = (String) it.next();
            logger.finer(parent + " " + 
            		(HashSet) parents.get(parent));
        }
        
    }

    /** *************************************************************
     * debugging utility
     */
    private void printChildren() {

        logger.finer("Printing children.");
        Iterator it = children.keySet().iterator();
        while (it.hasNext()) {
            String child = (String) it.next();
            logger.finer(child + " " +
            		(HashSet) children.get(child));
        }
    }

    /** *************************************************************
     * debugging utility
     */
    private void printDisjointness() {

        logger.finer("Printing disjoint.");
        Iterator it = disjoint.keySet().iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            logger.finer(term + " is disjoint with " + 
            		(Set) disjoint.get(term));
        }
    }

    /** *************************************************************
     * Determine whether a particular term is an immediate instance,
     * which has a statement of the form (instance term otherTerm).
     * Note that this does not count for terms such as Attribute(s)
     * and Relation(s), which may be defined as subAttribute(s) or
     * subrelation(s) of another instance.  If the term is not an
     * instance, return an empty ArrayList.  Otherwise, return an
     * ArrayList of the Formula(s) in which the given term is
     * defined as an instance.
     *
     * @param term A String.
     * @return An ArrayList.
     */
    public ArrayList instancesOf(String term) {

        //System.out.println("INFO in KB.instancesOf()");
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
        boolean ans = false;
        try {
            ans = getCachedRelationValues("instance", i, 1, 2).contains(c);
            // was: getAllInstancesWithPredicateSubsumption(c);
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return ans;
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
    public static boolean isInstanceOfInAnyKB(String i, String c) {
        boolean ans = false;
        try {
            Map kbs = KBmanager.getMgr().kbs;
            if (!kbs.isEmpty()) {
                KB kb = null;
                for (Iterator it = kbs.values().iterator(); it.hasNext();) {
                    kb = (KB) it.next();
                    ans = kb.isInstanceOf(i, c);
                    if (ans) break;
                }
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     */
    public boolean isInstance(String term) {

        ArrayList al = askWithRestriction(0,"instance",1,term);
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
        boolean ans = child.equals(parent);
        try {
            if (!ans) {
                List<String> preds = Arrays.asList("instance", "subclass", "subrelation");
                Set parents = null;
                for (String pred : preds) {
                    parents = getCachedRelationValues(pred, child, 1, 2);
                    ans = parents.contains(parent);
                    if (ans) break;
                }
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return ans;
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
        boolean ans = false;
        try {
            if (StringUtil.isNonEmptyString(c1) && StringUtil.isNonEmptyString(c2)) {
                ans = getCachedRelationValues("subclass", c1, 1, 2).contains(c2);
                // was: getAllSubClassesWithPredicateSubsumption(c2);
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Builds all of the relation caches for the current KB.  If
     * RelationCache Map objects already exist, they are cleared and
     * discarded.  New RelationCache Maps are created, and all caches
     * are rebuilt.
     *
     * @param clearExistingCaches If true, all existing caches are
     * cleared and discarded and completely new caches are created,
     * else if false, any existing caches are used and augmented
     *
     * @return void
     */
    public void buildRelationCaches(boolean clearExistingCaches) {

    	logger.entering("KB", "buildRelationCaches", "clearExistingCaches = " + clearExistingCaches);

        long t1 = System.currentTimeMillis();
        long totalCacheEntries = 0L;
        int i = -1;
        try {
            Iterator it = null;
            Iterator itv = null;
            String relationName = null;
            Map relationCache = null;
            for (i = 1; true; i++) {
                initRelationCaches(clearExistingCaches);
                clearExistingCaches = false;

                cacheGroundAssertionsAndPredSubsumptionEntailments();
                it = getCachedTransitiveRelationNames().iterator();
                relationName = null;
                while (it.hasNext()) {
                    relationName = (String) it.next();
                    computeTransitiveCacheClosure(relationName);
                }

                computeInstanceCacheClosure();

                // "disjoint"
                for (it = getCachedSymmetricRelationNames().iterator(); it.hasNext();) {
                    relationName = (String) it.next();
                    if (Arrays.asList("disjoint").contains(relationName))
                        computeSymmetricCacheClosure(relationName);
                }

                cacheRelnsWithRelnArgs();
                cacheRelationValences();

                long entriesAfterThisIteration = 0L;
                for (it = getRelationCaches().iterator(); it.hasNext();) {
                    relationCache = (RelationCache) it.next();
                    if (!relationCache.isEmpty()) {
                        for (itv = relationCache.values().iterator(); itv.hasNext();) {
                            entriesAfterThisIteration += ((Set) itv.next()).size();
                        }
                    }
                }
                if (entriesAfterThisIteration > totalCacheEntries)
                    totalCacheEntries = entriesAfterThisIteration;
                else
                    break;
                if (i > 4) break;
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        logger.info("Caching cycles == " + i
        			+ "\n Cache entries == " + totalCacheEntries
        			+ "\n Total time to build caches: " + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");
        logger.exiting("KB", "buildRelationCaches");
        return;
    }

    /** *************************************************************
     * Builds all of the relation caches for the current KB.  If
     * RelationCache Map objects already exist, they are cleared and
     * discarded.  New RelationCache Maps are created, and all caches
     * are rebuilt.
     *
     * @return void
     */
    public void buildRelationCaches() {

        buildRelationCaches(true);
        return;
    }

    /** *************************************************************
     * Populates all caches with ground assertions, from which
     * closures can be computed.
     */
    private void cacheGroundAssertions() {

    	logger.entering("KB", "cacheGroundAssertions");
        // System.out.println("formulas == " + formulas.toString());
        try {
            long t1 = System.currentTimeMillis();
            List symmetric = getCachedSymmetricRelationNames();
            List reflexive = getCachedReflexiveRelationNames();
            Set subInverses = new HashSet(getTermsViaPredicateSubsumption("subrelation",
                                                                          2, "inverse", 1,true));
            subInverses.add("inverse");
            logger.finer("subInverses == " + subInverses);

            String relation = null;
            String arg1 = null;
            String arg2 = null;
            List forms = null;
            Formula formula = null;
            RelationCache c1 = null;
            RelationCache c2 = null;
            RelationCache inv1 = null;
            Iterator formsIt = null;
            Iterator it = getCachedRelationNames().iterator();
            int total = 0;
            int count = -1;
            while (it.hasNext()) {
                count = 0;
                relation = (String) it.next();
                forms = ask("arg", 0, relation);
                // System.out.println(forms.size() + " " + relation + " assertions retrieved");
                if (forms != null) {
                    // System.out.print(relation);
                    c1 = getRelationCache(relation, 1, 2);
                    c2 = getRelationCache(relation, 2, 1);
                    inv1 = (subInverses.contains(relation)
                            ? getRelationCache("inverse", 1, 2)
                            : null);
                    boolean isSubInverse = (inv1 != null);
                    /*
                      if (relation.equals("inverse")) {
                      System.out.println("");
                      System.out.println("  relation == " + relation);
                      System.out.println("  forms == " + forms);
                      System.out.println("  c1 == " + c1);
                      System.out.println("  c2 == " + c2);
                      System.out.println("  inv1 == " + inv1);
                      }
                    */
                    formsIt = forms.iterator();
                    while (formsIt.hasNext()) {
                        formula = (Formula) formsIt.next();
                        if ((formula.theFormula.indexOf("(",2) == -1)
                            && !formula.sourceFile.endsWith(_cacheFileSuffix)) {

                            arg1 = formula.getArgument(1).intern();
                            arg2 = formula.getArgument(2).intern();

                            if (StringUtil.isNonEmptyString(arg1)
                                && StringUtil.isNonEmptyString(arg2)) {
                                count += addRelationCacheEntry(c1, arg1, arg2);
                                count += addRelationCacheEntry(c2, arg2, arg1);
                                // Special cases.
                                if (getCacheReflexiveAssertions()
                                    && reflexive.contains(relation)) {
                                    count += addRelationCacheEntry(c1, arg1, arg1);
                                    count += addRelationCacheEntry(c1, arg2, arg2);
                                    count += addRelationCacheEntry(c2, arg1, arg1);
                                    count += addRelationCacheEntry(c2, arg2, arg2);
                                }
                                if (symmetric.contains(relation))
                                    count += addRelationCacheEntry(c1, arg2, arg1);
                                if (isSubInverse) {
                                    count += addRelationCacheEntry(c1, arg2, arg1);
                                    count += addRelationCacheEntry(inv1, arg1, arg2);
                                    count += addRelationCacheEntry(inv1, arg2, arg1);
                                }
                            }
                        }
                    }
                }

                // More ways of collecting implied disjointness
                // assertions.
                if (relation.equals("disjoint")) {
                    List partitions = ask("arg", 0, "partition");
                    List decompositions = ask("arg", 0, "disjointDecomposition");
                    forms = new ArrayList();
                    if (partitions != null)
                        forms.addAll(partitions);
                    if (decompositions != null)
                        forms.addAll(decompositions);
                    c1 = getRelationCache(relation, 1, 2);
                    List arglist = null;
                    formsIt = forms.iterator();
                    while (formsIt.hasNext()) {
                        formula = (Formula) formsIt.next();
                        if ((formula.theFormula.indexOf("(",2) == -1)
                            && !formula.sourceFile.endsWith(_cacheFileSuffix)) {

                            arglist = formula.argumentsToArrayList(2);
                            for (int i = 0 ; i < arglist.size() ; i++) {
                                for (int j = 0 ; j < arglist.size() ; j++) {
                                    if (i != j) {
                                        arg1 = ((String) arglist.get(i)).intern();
                                        arg2 = ((String) arglist.get(j)).intern();
                                        if (StringUtil.isNonEmptyString(arg1)
                                            && StringUtil.isNonEmptyString(arg2)) {
                                            count += addRelationCacheEntry(c1, arg1, arg2);
                                            count += addRelationCacheEntry(c1, arg2, arg1);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                logger.finer(count + " cache entries added for " + relation);
                total += count;
            }
            logger.info("  Total: " + total + " cache entries computed in "
                               + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        logger.exiting("KB", "cacheGroundAssertions");
        
        return;
    }

    /** *************************************************************
     * Populates all caches with ground assertions, from which
     * closures can be computed.
     */
    private void cacheGroundAssertionsAndPredSubsumptionEntailments() {

        long t1 = System.currentTimeMillis();
        logger.entering("KB", "cacheGroundAssertionsAndPredSubsumptionEntailments");

        // System.out.println("formulas == " + formulas.toString());
        try {
            List symmetric = getCachedSymmetricRelationNames();
            List reflexive = getCachedReflexiveRelationNames();
            String relation = null;
            Set relationSet = new HashSet();
            String arg1 = null;
            String arg2 = null;
            Set formulae = new HashSet();
            List forms = null;
            Formula f = null;
            RelationCache c1 = null;
            RelationCache c2 = null;

            int total = 0;
            int count = -1;
            for (Iterator it = getCachedRelationNames().iterator(); it.hasNext();) {
                count = 0;
                relation = (String) it.next();
                relationSet.clear();
                relationSet.addAll(getTermsViaPredicateSubsumption("subrelation",
                                                                   2, relation, 1, true));
                relationSet.add(relation);
                // System.out.println("  " + relationSet);
                formulae.clear();
                for (Iterator itr = relationSet.iterator(); itr.hasNext();) {
                    forms = ask("arg", 0, (String) itr.next());
                    if (forms != null) formulae.addAll(forms);
                }
                // System.out.println(forms.size() + " " + relation + " assertions retrieved");
                if (!formulae.isEmpty()) {
                    // System.out.print(relation);
                    c1 = getRelationCache(relation, 1, 2);
                    c2 = getRelationCache(relation, 2, 1);
                    for (Iterator itf = formulae.iterator(); itf.hasNext();) {
                        f = (Formula) itf.next();
                        if ((f.theFormula.indexOf("(",2) == -1)
                            && !f.sourceFile.endsWith(_cacheFileSuffix)) {

                            arg1 = f.getArgument(1).intern();
                            arg2 = f.getArgument(2).intern();

                            if (StringUtil.isNonEmptyString(arg1)
                                && StringUtil.isNonEmptyString(arg2)) {

                                count += addRelationCacheEntry(c1, arg1, arg2);
                                count += addRelationCacheEntry(c2, arg2, arg1);

                                // symmetric
                                if (symmetric.contains(relation)) {
                                    count += addRelationCacheEntry(c1, arg2, arg1);
                                    count += addRelationCacheEntry(c2, arg1, arg2);
                                }

                                // reflexive
                                if (getCacheReflexiveAssertions()
                                    && reflexive.contains(relation)) {
                                    count += addRelationCacheEntry(c1, arg1, arg1);
                                    count += addRelationCacheEntry(c1, arg2, arg2);
                                    count += addRelationCacheEntry(c2, arg1, arg1);
                                    count += addRelationCacheEntry(c2, arg2, arg2);
                                }
                            }
                        }
                    }
                }

                // More ways of collecting implied disjointness
                // assertions.
                if (relation.equals("disjoint")) {
                    formulae.clear();
                    List partitions = ask("arg", 0, "partition");
                    List decompositions = ask("arg", 0, "disjointDecomposition");
                    if (partitions != null)
                        formulae.addAll(partitions);
                    if (decompositions != null)
                        formulae.addAll(decompositions);
                    c1 = getRelationCache(relation, 1, 2);
                    List arglist = null;
                    for (Iterator itf = formulae.iterator(); itf.hasNext();) {
                        f = (Formula) itf.next();
                        if ((f.theFormula.indexOf("(",2) == -1)
                            && !f.sourceFile.endsWith(_cacheFileSuffix)) {

                            arglist = f.argumentsToArrayList(2);
                            for (int i = 0 ; i < arglist.size(); i++) {
                                for (int j = 0; j < arglist.size(); j++) {
                                    if (i != j) {
                                        arg1 = ((String) arglist.get(i)).intern();
                                        arg2 = ((String) arglist.get(j)).intern();
                                        if (StringUtil.isNonEmptyString(arg1)
                                            && StringUtil.isNonEmptyString(arg2)) {
                                            count += addRelationCacheEntry(c1, arg1, arg2);
                                            count += addRelationCacheEntry(c1, arg2, arg1);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (count > 0) {
                    logger.finer(relation
                                       + ": "
                                       + count
                                       + " entries added for "
                                       + relationSet);
                    total += count;
                }
            }
            logger.info("Total: " + total + " new cache entries computed in "
                               + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        logger.exiting("KB", "cacheGroundAssertionsAndPredSubsumptionEntailments");
        return;
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
    public static ArrayList formulasToArrayLists(List formulaList) {

        ArrayList ans = new ArrayList();
        try {
            if (formulaList instanceof List) {
                Iterator it = formulaList.iterator();
                Formula f = null;
                while (it.hasNext()) {
                    f = (Formula) it.next();
                    ans.add(f.literalToArrayList());
                }
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
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
    public static ArrayList stringsToFormulas(List strings) {

        ArrayList ans = new ArrayList();
        try {
            if (strings instanceof List) {
                Iterator it = strings.iterator();
                while (it.hasNext()) {
                    Formula f = new Formula();
                    f.read((String)it.next());
                    ans.add(f);
                }
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Converts a literal (List object) to a String.
     *
     * @param literal A List representing a SUO-KIF formula.
     *
     * @return A String representing a SUO-KIF formula.
     */
    public static String literalListToString(List literal) {
        StringBuffer b = new StringBuffer();
        try {
            if (literal instanceof List) {
                b.append("(");
                for (int i = 0 ; i < literal.size() ; i++) {
                    if (i > 0) {
                        b.append(" ");
                    }
                    b.append((String)literal.get(i));
                }
                b.append(")");
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return b.toString();
    }

    /** *************************************************************
     * Converts a literal (List object) to a Formula.
     *
     * @param literal A List representing a SUO-KIF formula.
     *
     * @return A SUO-KIF Formula object, or null if no Formula can be
     * created.
     */
    public static Formula literalListToFormula(List lit) {
        Formula f = null;
        try {
            String theFormula = literalListToString(lit);
            if (StringUtil.isNonEmptyString(theFormula)) {
                f = new Formula();
                f.read(theFormula);
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
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
                                                           Set predicatesUsed) {

        ArrayList<String> result = new ArrayList<String>();
        try {
            if (StringUtil.isNonEmptyString(term1)
                && !StringUtil.isQuotedString(term1)
                && StringUtil.isNonEmptyString(term2)
                && !StringUtil.isQuotedString(term2)) {
                List formulae = askWithRestriction(argnum1, term1, argnum2, term2);
                Formula f = null;
                Iterator it = null;
                for (it = formulae.iterator(); it.hasNext();) {
                    f = (Formula) it.next();
                    result.add(f.getArgument(targetArgnum));
                }
                if (predicatesUsed instanceof Set) {
                    for (it = formulae.iterator(); it.hasNext();) {
                        f = (Formula) it.next();
                        predicatesUsed.add(f.car());
                    }
                }
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
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
        return getTermsViaAskWithRestriction(argnum1,
                                             term1,
                                             argnum2,
                                             term2,
                                             targetArgnum,
                                             null);
    }

    /** *************************************************************
     * Returns the first term found that corresponds to targetArgnum
     * in the Formulas obtained from the method call
     * askWithRestriction(argnum1, term1, argnum2, term2).
     *
     * @return A SUO-KIF term (String), or null is no answer can be
     *         retrieved.
     */
    public String getFirstTermViaAskWithRestriction(int argnum1,
                                                    String term1,
                                                    int argnum2,
                                                    String term2,
                                                    int targetArgnum) {

        String result = null;
        try {
            List terms = getTermsViaAskWithRestriction(argnum1,
                                                       term1,
                                                       argnum2,
                                                       term2,
                                                       targetArgnum);
            if (!terms.isEmpty()) {
                result = (String) terms.get(0);
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
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
       try {
           if (StringUtil.isNonEmptyString(term1) && StringUtil.isNonEmptyString(term2)) {
               ArrayList partial1 = ask("arg", argnum1, term1);
               ArrayList partial2 = ask("arg", argnum2, term2);
               ArrayList partial = partial1;
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
                   if (f.getArgument(arg).equals(term))
                       result.add(f);
               }
           }
       }
       catch (Exception ex) {
    	   logger.warning(ex.getStackTrace().toString());
           ex.printStackTrace();
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
    public ArrayList askWithTwoRestrictions(int argnum1, String term1,
                                            int argnum2, String term2,
                                            int argnum3, String term3) {

    	String[] args = new String[6];
    	args[0] = "argnum1 = " + argnum1;
    	args[1] = "term1 = " + term1;
    	args[0] = "argnum2 = " + argnum2;
    	args[1] = "term2 = " + term2;
    	args[0] = "argnum3 = " + argnum3;
    	args[1] = "term3 = " + term3;
    	
    	logger.entering("KB", "askWithTwoRestrictions", args);
    	//System.out.println("INFO in KB.askWithTwoRestrictions(): " + argnum1 + " " + term1 + " " +
    	//		argnum2 + " " + term2 + " " +
    	//		argnum3 + " " + term3);
        ArrayList result = new ArrayList();
        if (StringUtil.isNonEmptyString(term1)
            && StringUtil.isNonEmptyString(term2)
            && StringUtil.isNonEmptyString(term3)) {
            ArrayList partiala = new ArrayList();           // will get the smallest list
            ArrayList partialb = new ArrayList();           // next smallest
            ArrayList partialc = new ArrayList();           // biggest
            ArrayList partial1 = ask("arg",argnum1,term1);
            ArrayList partial2 = ask("arg",argnum2,term2);
            ArrayList partial3 = ask("arg",argnum3,term3);
            //System.out.println("INFO in KB.askWithTwoRestrictions():" + partial1.size() + " " +
            //		partial2.size() + " " + partial3.size() + " ");
            int arga = -1;
            String terma = "";
            int argb = -1;
            String termb = "";
            int argc = -1;
            String termc = "";
            if (partial1 == null || partial2 == null || partial3 == null)
                return result;
            if (partial1.size() > partial2.size() && partial1.size() > partial3.size()) {
                partialc = partial1;
                argc = argnum1;
                termc = term1;
                if (partial2.size() > partial3.size()) {
                    argb = argnum2;
                    termb = term2;
                    partialb = partial1;
                    arga = argnum3;
                    terma = term3;
                    partiala = partial3;
                }
                else {
                    argb = argnum3;
                    termb = term3;
                    partialb = partial3;
                    arga = argnum2;
                    terma = term2;
                    partiala = partial2;
                }
            }
            if (partial2.size() > partial1.size() && partial2.size() > partial3.size()) {
                partialc = partial2;
                argc = argnum2;
                termc = term2;
                if (partial1.size() > partial3.size()) {
                    argb = argnum1;
                    termb = term1;
                    partialb = partial1;
                    arga = argnum3;
                    terma = term3;
                    partiala = partial3;
                }
                else {
                    argb = argnum3;
                    termb = term3;
                    partialb = partial3;
                    arga = argnum1;
                    terma = term1;
                    partiala = partial1;
                }
            }
            if (partial3.size() > partial1.size() && partial3.size() > partial2.size()) {
                partialc = partial3;
                argc = argnum3;
                termc = term3;
                if (partial1.size() > partial2.size()) {
                    argb = argnum1;
                    termb = term1;
                    partialb = partial1;
                    arga = argnum2;
                    terma = term2;
                    partiala = partial2;
                }
                else {
                    argb = argnum2;
                    termb = term2;
                    partialb = partial2;
                    arga = argnum1;
                    terma = term1;
                    partiala = partial1;
                }
            }

            if (partiala != null) {
                Formula f = null;
                for (int i = 0; i < partiala.size(); i++) {
                    f = (Formula) partiala.get(i);
                    if (f.getArgument(argb).equals(termb)) {
                        if (f.getArgument(argc).equals(termc))
                            result.add(f);
                    }
                }
            }
        }
        logger.exiting("KB", "askWithTwoRestrictions", result);
        
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
        ArrayList ans = new ArrayList();
        try {

            List formulae = askWithTwoRestrictions(argnum1, term1,
                                                   argnum2, term2,
                                                   argnum3, term3);
            Formula f = null;
            for (int i = 0; i < formulae.size(); i++) {
                f = (Formula) formulae.get(i);
                ans.add(f.getArgument(targetArgnum));
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
            ans = new ArrayList();
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
        try {

            List<String> terms = getTermsViaAWTR(argnum1, term1,
                                                 argnum2, term2,
                                                 argnum3, term3,
                                                 targetArgnum);
            if (!terms.isEmpty()) {
                ans = (String) terms.get(0);
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getMessage());
            ex.printStackTrace(); 
        }
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
    public ArrayList<String> getTermsViaAsk(int knownArgnum,
                                            String knownArg,
                                            int targetArgnum) {

        ArrayList<String> result = new ArrayList<String>();
        try {
            List formulae = ask("arg", knownArgnum, knownArg);
            if (!formulae.isEmpty()) {
                TreeSet<String> ts = new TreeSet<String>();
                Formula f = null;
                for (Iterator it = formulae.iterator(); it.hasNext();) {
                    f = (Formula) it.next();
                    ts.add(f.getArgument(targetArgnum));
                }
                result.addAll(ts);
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
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
    public ArrayList ask(String kind, int argnum, String term) {
        ArrayList result = new ArrayList();
        try {
        	String msg = null;
            if (StringUtil.emptyString(term)) {
                msg = ("Error in KB.ask(\""
                       + kind + "\", "
                       + argnum + ", \""
                       + term + "\"): "
                       + "search term is null, or an empty string");
                logger.warning(msg);
                throw new Exception(msg);
            }
            if (term.length() > 1
                && term.charAt(0) == '"'
                && term.charAt(term.length()-1) == '"') {
                msg = ("Error in KB.ask(): Strings are not indexed.  No results for " + term);
                logger.warning(msg);
                throw new Exception(msg);
            }
            List tmp = null;
            if (kind.equals("arg"))
                tmp = (List) this.formulas.get(kind + "-" + argnum + "-" + term);
            else
                tmp = (List) this.formulas.get(kind + "-" + term);
            if (tmp != null) {
                result.addAll(tmp);
            }
            
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
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
     *
     * @param idxArgnum The argument position occupied by idxTerm in
     *                  each ground Formula to be retrieved
     *
     * @param idxTerm A constant that occupied idxArgnum position in
     *                each ground Formula to be retrieved
     *
     * @return an ArrayList of Formulas that satisfy the query, or an
     *         empy ArrayList if no Formulae are retrieved.
     */
    public ArrayList askWithPredicateSubsumption(String relation, int idxArgnum, String idxTerm) {
        /*
          System.out.println("ENTER KB.askWithPredicateSubsumption("
          + relation + ", "
          + idxArgnum + ", "
          + idxTerm + ")");
        */
        ArrayList ans = new ArrayList();
        try {
            if (StringUtil.isNonEmptyString(relation)
                && StringUtil.isNonEmptyString(idxTerm)
                && (idxArgnum >= 0)
                // && (idxArgnum < 7)
                ) {
                Set done = new HashSet();
                HashSet accumulator = new HashSet();
                ArrayList relns = new ArrayList();
                relns.add(relation);
                Iterator it = null;
                String reln = null;
                List formulae = null;
                Formula f = null;
                String arg = null;
                while (!relns.isEmpty()) {
                    it = relns.iterator();
                    while (it.hasNext()) {
                        reln = (String) it.next();
                        formulae = (List) this.askWithRestriction(0, reln, idxArgnum, idxTerm);
                        ans.addAll(formulae);
                        formulae = (List) this.askWithRestriction(0, "subrelation", 2, reln);
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
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        /*
          System.out.println("EXIT KB.askWithPredicateSubsumption("
          + relation + ", "
          + idxArgnum + ", "
          + idxTerm + ") == "
          + (ans.size() > 10 ? (ans.subList(0, 10) + " ...") : ans));
        */
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
        /*
                     System.out.println("ENTER KB.getTermsViaPredicateSubsumption("
                     + relation + ", "
                     + idxArgnum + ", "
                     + idxTerm + ", "
                     + targetArgnum + ", "
                     + useInverses + ")");
        */

        ArrayList<String> ans = new ArrayList<String>();
        try {
            if (StringUtil.isNonEmptyString(relation)
                && StringUtil.isNonEmptyString(idxTerm)
                && (idxArgnum >= 0)
                // && (idxArgnum < 7)
                ) {
                TreeSet<String> reduced = new TreeSet<String>();
                List<String> inverseSyns = null;
                List<String> inverses = null;
                if (useInverses) {
                    inverseSyns = getTermsViaAskWithRestriction(0,
                                                                "subrelation",
                                                                2,
                                                                "inverse",
                                                                1);
                    inverseSyns.addAll(getTermsViaAskWithRestriction(0,
                                                                     "equal",
                                                                     2,
                                                                     "inverse",
                                                                     1));
                    inverseSyns.addAll(getTermsViaAskWithRestriction(0,
                                                                     "equal",
                                                                     1,
                                                                     "inverse",
                                                                     2));
                    inverseSyns.add("inverse");
                    SetUtil.removeDuplicates(inverseSyns);
                    inverses = new ArrayList<String>();
                }
                List<String> accumulator = new ArrayList<String>();
                List<String> predicates = new ArrayList<String>();
                predicates.add(relation);
                while (!predicates.isEmpty()) {
                    for (String pred : predicates) {
                        reduced.addAll(getTermsViaAskWithRestriction(0,
                                                                     pred,
                                                                     idxArgnum,
                                                                     idxTerm,
                                                                     targetArgnum,
                                                                     predicatesUsed));
                        accumulator.addAll(getTermsViaAskWithRestriction(0,
                                                                         "subrelation",
                                                                         2,
                                                                         pred,
                                                                         1));
                        accumulator.addAll(getTermsViaAskWithRestriction(0,
                                                                         "equal",
                                                                         2,
                                                                         "subrelation",
                                                                         1));
                        accumulator.addAll(getTermsViaAskWithRestriction(0,
                                                                         "equal",
                                                                         1,
                                                                         "subrelation",
                                                                         2));
                        accumulator.remove(pred);
                        if (useInverses) {
                            for (String syn : inverseSyns) {
                                inverses.addAll(getTermsViaAskWithRestriction(0,
                                                                              syn,
                                                                              1,
                                                                              pred,
                                                                              2));
                                inverses.addAll(getTermsViaAskWithRestriction(0,
                                                                              syn,
                                                                              2,
                                                                              pred,
                                                                              1));
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
                    for (String inv : inverses) {
                        reduced.addAll(getTermsViaPredicateSubsumption(inv,
                                                                       targetArgnum,
                                                                       idxTerm,
                                                                       idxArgnum,
                                                                       false,
                                                                       predicatesUsed));
                    }
                }
                ans.addAll(reduced);
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }

        /*
                  System.out.println("EXIT KB.getTermsViaPredicateSubsumption("
                  + relation + ", "
                  + idxArgnum + ", "
                  + idxTerm + ", "
                  + targetArgnum + ", "
                  + useInverses + ")");
                  System.out.println("  ==> " + ans);
        */

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
        /*
          System.out.println("ENTER KB.getFirstTermViaPredicateSubsumption(\""
          + relation + "\", "
          + idxArgnum + ", \""
          + idxTerm + "\", "
          + targetArgnum + ", "
          + useInverses + ")");
        */

        String ans = null;
        try {
            if (StringUtil.isNonEmptyString(relation)
                && StringUtil.isNonEmptyString(idxTerm)
                && (idxArgnum >= 0)
                // && (idxArgnum < 7)
                ) {
                List terms = getTermsViaPredicateSubsumption(relation,
                                                             idxArgnum,
                                                             idxTerm,
                                                             targetArgnum,
                                                             useInverses);
                if (!terms.isEmpty()) {
                    ans = (String) terms.get(0);
                }
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }

        /*
          System.out.println("EXIT KB.getFirstTermViaPredicateSubsumption(\""
          + relation + "\", "
          + idxArgnum + ", \""
          + idxTerm + "\", "
          + targetArgnum + ", "
          + useInverses + ")");
          System.out.println("  ==> " + ans);
        */

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
        try {
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
                    accumulator.addAll(getTermsViaPredicateSubsumption(relation,idxArgnum,term,targetArgnum,useInverses));
            }
            ans.addAll(reduced);
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
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
    private ArrayList merge(KIF kif, String pathname) {
    	if (logger.isLoggable(Level.FINEST)) {
	    	String[] params =  {"kif = " + kif, "pathname = " + pathname};
	    	logger.entering("KB", "merge", params);
	    }
    	
        ArrayList formulasPresent = new ArrayList();
        try {
            // Add all the terms from the new formula into the KB's current list
            getTerms().addAll(kif.terms);

            Set keys = kif.formulas.keySet();
            Iterator it = keys.iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                ArrayList newFormulas = new ArrayList((ArrayList) kif.formulas.get(key));
                if (formulas.containsKey(key)) {
                    ArrayList oldFormulas = (ArrayList) formulas.get(key);
                    for (int i = 0; i < newFormulas.size(); i++) {
                        Formula newFormula = (Formula) newFormulas.get(i);
                        if (pathname != null) {
                            newFormula.sourceFile = pathname;
                        }
                        boolean found = false;
                        for (int j = 0; j < oldFormulas.size(); j++) {
                            Formula oldFormula = (Formula) oldFormulas.get(j);
                            if (newFormula.theFormula.equals(oldFormula.theFormula)) {
                                found = true;
                                formulasPresent.add(oldFormula);
                                // System.out.println("INFO in KB.merge)");
                                // System.out.println("  newFormula == " + newFormula);
                                // System.out.println("  oldFormula == " + oldFormula);
                            }
                        }
                        if (!found) {
                            // value.computeTheClausalForm();
                            oldFormulas.add(newFormula);
                            formulaMap.put(newFormula.theFormula.intern(), newFormula);
                        }
                    }
                }
                else {
                    formulas.put(key,newFormulas);
                    Iterator it2 = newFormulas.iterator();
                    Formula f = null;
                    while (it2.hasNext()) {
                        f = (Formula) it2.next();
                        if (StringUtil.isNonEmptyString(f.theFormula)) {
                            // f.computeTheClausalForm();
                            formulaMap.put(f.theFormula.intern(), f);
                        }
                    }
                }
            }
            /* collectParents();
               if (KBmanager.getMgr().getPref("cache") != null &&
               KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes"))
               cache();  */     // caching is too slow to perform for just one formula
        }
        catch (Exception ex) {
            logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        
    	logger.exiting("KB", "merge", formulasPresent);	    
        return formulasPresent;
    }

    /** *************************************************************
     *  Rename term2 as term1 throughout the knowledge base.  This
     *  is an operation with side effects - the term names in the KB
     *  are changed.
     */
    public void rename(String term2, String term1) {

        logger.info("Replace " + term2 + " with " + term1);
        TreeSet<Formula> formulas = new TreeSet<Formula>();
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
     * @param formulas an AraryList of Strings.
     * @param fname The fully qualified file name.
     * @return void
     */
    private void writeFormulas(ArrayList formulas, String fname) {

        FileWriter fr = null;
        try {
            fr = new FileWriter(fname,true);
            String ls = getLineSeparator();
            for (Iterator it = formulas.iterator(); it.hasNext();) {
                fr.write((String) it.next());
                fr.write(ls);
            }
        }
        catch (Exception e) {
            logger.severe("Error writing file " + fname + ". " + e.getStackTrace());
            e.printStackTrace();
        }
        finally {
            try {
                if (fr != null)
                    fr.close();
            }
            catch (Exception e2) {
            	logger.severe(e2.getStackTrace().toString());
                e2.printStackTrace();
            }
        }
        return;
    }

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
        	logger.severe("Error writing file " + fname + ". " + e.getStackTrace());
        }
        finally {
            if (fr != null)
                fr.close();
        }
        return flen;
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
    public String tellSTP2(String input) {

        STP2 stp2 = (STP2) engineMap.get("STP2");

        String result = null;
        try {
            result = stp2.assertFormula(input);
        } catch (IOException ioe) {
        	logger.warning(ioe.getStackTrace().toString());
            ioe.printStackTrace();
            return "<assertionResponse>" + ioe.getMessage() + "</assertionResponse>";
        }
        return result;
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

    	logger.entering("KB", "tell", "input = " + input);
        String result = "The formula could not be added";
        try {
            KBmanager mgr = KBmanager.getMgr();
            // 1. Parse the input string.
            KIF kif = new KIF();
            String msg = kif.parseStatement(input);
            if (msg != null) {
                result = "Error parsing \"" + input + "\": " + msg;
			} else if (kif.formulaSet.isEmpty()) {
                result = "The input could not be parsed";
			} else {

                // Make the pathname of the user assertions file.
                File dir = new File(this.kbDir);
                File file = new File(dir, (this.name + _userAssertionsString));
                String filename = file.getCanonicalPath();
                List formulasAlreadyPresent = merge(kif, filename);
                if (!formulasAlreadyPresent.isEmpty()) {
                    String sf = ((Formula)formulasAlreadyPresent.get(0)).sourceFile;
                    result = "The formula was already added from " + sf;
                }
                else {
                    ArrayList parsedFormulas = new ArrayList();
                    String fstr = null;
                    Formula parsedF = null;
                    Iterator it = kif.formulaSet.iterator();
                    boolean go = true;
                    while (go && it.hasNext()) {
                        // 2. Confirm that the input has been converted into
                        // at least one Formula object and stored in this.formulaMap.
                        fstr = (String) it.next();
                        parsedF = (Formula) this.formulaMap.get(fstr.intern());
                        if (parsedF == null) {
                            go = false;
						} else if (!parsedF.hasCorrectArity(this)) {
							result = result
									+ "<br/>Following formula does not have correct arity: "
									+ parsedF.htmlFormat(this);
						} else {
                            parsedFormulas.add(parsedF);
                        }
                    }
                    logger.info("parsedFormulas == " + parsedFormulas);
                    if (go && !parsedFormulas.isEmpty()) {
                        if (!constituents.contains(filename)) {
                            // System.out.println("INFO in KB.tell():
                            // Adding file: " + filename + " to: " +
                            // constituents.toString());

                            // 3. If the assertions file exists, delete it.
                            if (file.exists())
                                file.delete();
                            constituents.add(filename);
                            mgr.writeConfiguration();
                        }
                        for (Iterator pfit = parsedFormulas.iterator(); pfit.hasNext();) {
                            parsedF = (Formula) pfit.next();
                            // 4. Write the formula to the user assertions file.
                            parsedF.endFilePosition = writeUserAssertion(parsedF.theFormula,
                                                                         filename);
                            parsedF.sourceFile = filename;
                        }

                        result = "The formula has been added for browsing";

                        boolean allAdded = (inferenceEngine != null);
                        ArrayList processedFormulas = new ArrayList();
                        it = parsedFormulas.iterator();
                        for (Iterator pfit = parsedFormulas.iterator(); pfit.hasNext();) {
                            processedFormulas.clear();
                            parsedF = (Formula) pfit.next();
                            // 5. Preproccess the formula.
                            processedFormulas.addAll(parsedF.preProcess(false, this));
							errors.addAll(parsedF.getErrors());
                            if (processedFormulas.isEmpty()) {
                                allAdded = false;
                            }
                            else {
                                // 6. If TPTP != no, translate to TPTP.
                                if (!mgr.getPref("TPTP").equalsIgnoreCase("no")) {
                                    parsedF.tptpParse(false, this, processedFormulas);

                                    logger.info("theTptpFormulas == " 
                                                       + parsedF.getTheTptpFormulas());
                                }
                                // 7. If there is an inference engine, assert the formula to the
                                // inference engine's database.
                                if (inferenceEngine != null) {
                                    String ieResult = null;
                                    Formula processedF = null;
                                    Iterator it2 = processedFormulas.iterator();
                                    while (it2.hasNext()) {
                                        processedF = (Formula) it2.next();
                                        logger.info("Asserting formula ("
                                                           + processedF.theFormula
                                                           + ") to Vampire.");
                                        ieResult = inferenceEngine.assertFormula(processedF.theFormula);
                                        logger.info("Return from Vampire == ("
                                                           + ieResult + ")");
                                        if (ieResult.indexOf("Formula has been added") < 0) {
                                            allAdded = false;
                                        }
                                    }
                                }
                            }
                        }
                        result += (allAdded ? " and inference" : " but not for local inference");
                    }
                }
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
            // System.out.println("Error in KB.tell(): " + ioe.getMessage());
        }
        /* collectParents();
           if (mgr.getPref("cache") != null &&
           mgr.getPref("cache").equalsIgnoreCase("yes"))
           cache();        */   // caching is currently not efficient enough to invoke it after every assertion
        logger.exiting("KB", "tell", result);
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

    	if (logger.isLoggable(Level.FINER)){
    		String[] params = {"suoKifFormula = " + suoKifFormula, "timeout = " + timeout, "maxAnswers = " + maxAnswers};
    		logger.entering("KB", "ask", params);
    	}
        long t1 = System.currentTimeMillis();
        
        logger.info("query == " + suoKifFormula);
        String result = "";
        try {
            // Start by assuming that the ask is futile.
            result = ("<queryResponse>" + getLineSeparator()
                      + "  <answer result=\"no\" number=\"0\"> </answer>" + getLineSeparator()
                      + "  <summary proofs=\"0\"/>" + getLineSeparator()
                      + "</queryResponse>" + getLineSeparator());

            if (StringUtil.isNonEmptyString(suoKifFormula)) {
                Formula query = new Formula();
                query.read(suoKifFormula);
                ArrayList processedStmts = query.preProcess(true, this);
                logger.fine("processedStmts == " + processedStmts);
                //if (!processedStmts.isEmpty() && (this.inferenceEngine instanceof Vampire)) {
                if (!processedStmts.isEmpty() && this.inferenceEngine != null)
                    result = this.inferenceEngine.submitQuery(((Formula)processedStmts.get(0)).theFormula,timeout,maxAnswers);
                logger.info("result == " + result);
            }
        }
        catch (Exception ex) {
        	logger.severe(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        
        
        logger.info("Total time: " + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time from start of ASK to end.");
        logger.exiting("KB", "ask", result);

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

    	if (logger.isLoggable(Level.FINER)) {
    		String[] params = {"suoKifFormula = " + suoKifFormula, "timeout = " + timeout, "maxAnswers = " + maxAnswers, "engine = " + engine};
    		logger.entering("KB", "askEngine", params);
    	}
    	
        String result = "";
        try {
            // Start by assuming that the ask is futile.
            result = "<queryResponse>\n<answer result=\"no\" number=\"0\">\n</answer>\n<summary proofs=\"0\"/>\n</queryResponse>\n";
            if (Formula.isNonEmptyString(suoKifFormula)) {
                Formula query = new Formula();
                query.read(suoKifFormula);
                ArrayList processedStmts = query.preProcess(true, this);
                logger.fine("processedStmts == " + processedStmts);
                if (!processedStmts.isEmpty()) 
                    result = engine.submitQuery(((Formula)processedStmts.get(0)).theFormula,timeout,maxAnswers);                
            }
            result = result.replaceAll("&lt;","<");
            result = result.replaceAll("&gt;",">");
        }
        catch (Exception ex) {
        	logger.severe(ex.getStackTrace().toString());
            ex.printStackTrace();
        }        
        logger.exiting("KB", "askEngine", result);
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
        try {
            InferenceEngine.EngineFactory factory = SInE.getFactory();
            InferenceEngine engine = createInferenceEngine(factory);
            result = askEngine(suoKifFormula, timeout, maxAnswers, engine);
            if (engine != null)
                engine.terminate();
        }
        catch (Exception ex) {
        	logger.severe(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Submits a query to the STP inference engine.  Returns an XML
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
    public String askSTP(String suoKifFormula, int timeout, int maxAnswers) {

        String result = "";
        try {
            InferenceEngine engine = engineMap.get("STP");
            if (engine == null) {
                InferenceEngine.EngineFactory factory = STP.getFactory();
                engine = createInferenceEngine(factory);
                engineMap.put("STP",engine);
            }
            result = askEngine(suoKifFormula, timeout, maxAnswers, engine);
            if (engine != null)
                engine.terminate();
        }
        catch (Exception ex) {
        	logger.severe(ex.getStackTrace().toString());
            ex.printStackTrace();
        }

        return result;
    }

    /** *************************************************************
     * Submits a query to the STP2 inference engine.  Returns an XML
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
    public String askSTP2(String suoKifFormula, int timeout, int maxAnswers) {

        String result = "";
        try {
            InferenceEngine engine = null;
            if (!engineMap.containsKey("STP2")) {
                InferenceEngine.EngineFactory factory = STP2.getFactory();
                engine = createInferenceEngine(factory);
                engineMap.put("STP2",engine);
            }
            else
                engine = (InferenceEngine) engineMap.get("STP2");
            result = askEngine(suoKifFormula, timeout, maxAnswers, engine);
            if (engine != null)
                engine.terminate();
        }
        catch (Exception ex) {
        	logger.severe(ex.getStackTrace().toString());
            ex.printStackTrace();
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
            //InferenceEngine.EngineFactory factory=SInE.getFactory();
            //InferenceEngine engine=createInferenceEngine(factory);
            // result = askEngine(suoKifFormula, timeout, maxAnswers, engine);
            /*
	    result =
		("<queryResponse>" + getLineSeparator()
		 + "  <answer result=\"no \" number=\"0\">" + getLineSeparator()
		 + "  </answer>" + getLineSeparator()
		 + "  <summary proofs=\"0\"/>" + getLineSeparator()
		 + "</queryResponse>" + getLineSeparator());
             */
            List<Formula> selectedQuery = new ArrayList<Formula>();
            Formula newQ = new Formula();
            newQ.read(suoKifFormula);
            selectedQuery.add(newQ);

            //String kbFileName = "/Users/christophbenzmueller/Sigma/KBs/SumoMilo.kif";
            //logger.fine("kbFileName = " + kbFileName);

            List<String> selFs = null;

            if (flag.equals("LeoSine")) {
                SInE sine = new SInE(this.formulaMap.keySet());
                selFs = new ArrayList<String>(sine.performSelection(suoKifFormula));

                //SInE sine = SInE.getNewInstance(kbFileName);
                //selFs = new ArrayList<String>(sine.performSelection(suoKifFormula));
                sine.terminate();
            }
            else if (flag.equals("LeoLocal")) {
                selFs = new ArrayList<String>();
            }
            else if (flag.equals("LeoGlobal")) {
                selFs = new ArrayList<String>();
                for (Iterator it = this.formulaMap.values().iterator(); it.hasNext();) {
                    Formula entry = (Formula) it.next();
                    selFs.add(entry.toString());
                }
            }
            // add user asserted formulas
            try {
                File dir = new File(this.kbDir);
                File file = new File(dir, (this.name + _userAssertionsString));
                String filename = file.getCanonicalPath();
                BufferedReader userAssertedInput =  new BufferedReader(new FileReader(filename));

                try {
                    String line = null;
                    /*
                     * readLine is a bit quirky :
                     * it returns the content of a line MINUS the newline.
                     * it returns null only for the END of the stream.
                     * it returns an empty String if two newlines appear in a row.
                     */
                    while (( line = userAssertedInput.readLine()) != null){
                        selFs.add(line);
                        // System.out.println("/n asserted Formula: " + line);
                    }
                }
                finally {
                    userAssertedInput.close();
                }
            }
            catch (IOException ex){
                logger.severe(ex.getStackTrace().toString());
                ex.printStackTrace();
            }

            List<Formula> selectedFormulas = new ArrayList();
            Formula newF = new Formula();

            for (Iterator it = selFs.iterator(); it.hasNext();) {
                String entry = (String) it.next();
                newF = new Formula();
                newF.read(entry);
                selectedFormulas.add(newF);
            }

            logger.finer("selectedFormulas = " +  selFs.toString());

            THF thf = new THF();
            LeoProblem = thf.KIF2THF(selectedFormulas,selectedQuery,this);
            LeoInputFileW.write(LeoProblem);
            LeoInputFileW.close();

            String command = LeoExecutableFile.getCanonicalPath() + " -po 1 -t " + timeout + " " + LeoInputFile.getCanonicalPath();
            logger.finer("command = " + command);

            Process leo = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(leo.getInputStream()));
            while ((responseLine = reader.readLine()) != null) {
                LeoOutput += responseLine + "\n";
            }
            reader.close();

            logger.finer("LeoOutput == " + LeoOutput);

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
            //if (engine != null)
            //  engine.terminate();
        }
        catch (Exception ex) {
            logger.severe(ex.getStackTrace().toString());
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
    	
    	if (getTerms().contains(term.intern())) {
    		return true;
    	}
    	else if (getREMatch(term.intern()).size()==1) {
    		return true;
    	}

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

        return this.getAllInstances("Relation").size();
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
    public TreeSet getFormulas() {

        return new TreeSet(formulaMap.keySet());
    }

    /** ***************************************************************
     *  An accessor providing a Formulas.
     */
    public Formula getFormulaByKey(String key) {
        Formula f = null;
        ArrayList al = (ArrayList) formulas.get(key);
        if ((al != null) && !al.isEmpty())
            f = (Formula) al.get(0);
        return f;
    }

    /** ***************************************************************
     */
    public void rehashFormula(Formula f, String formID) {

        ArrayList al = (ArrayList) formulas.get(formID);
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
                logger.info("Formula hash collision for: "
                                   + formID
                                   + " and formula "
                                   + f.theFormula);
        }
        else
            logger.info("No formula for hash: "
                               + formID +
                               " and formula "
                               + f.theFormula);
    }

    /** ***************************************************************
     *  Count the number of rules in the knowledge base in order to
     *  present statistics to the user. Note that the number of rules
     *  is a subset of the number of formulas.
     *
     *  @return The int(eger) number of rules in the knowledge base.
     */
    public int getCountRules() {

        int count = 0;
        Formula f = null;
        for (Iterator it = formulaMap.values().iterator(); it.hasNext();) {
            f = (Formula) it.next();
            if (f.isRule()) {
                count++;
            }
        }
        return count;
    }

    /** ***************************************************************
     * Create an ArrayList of the specific size, filled with empty strings.
     */
    private ArrayList arrayListWithBlanks(int size) {

        ArrayList al = new ArrayList(size);
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
    private ArrayList getNearestKTerms(String term, int k) {

        ArrayList al;
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
        
        logger.finer("Number of terms in this KB == " + t.length);

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
    private ArrayList getNearestTerms(String term) {

        return getNearestKTerms(term,15);
    }

    /** ***************************************************************
     * Get the neighbors of this initial uppercase term (class or function).
     */
    public ArrayList getNearestRelations(String term) {

        term = Character.toUpperCase(term.charAt(0)) + term.substring(1,term.length());
        return getNearestTerms(term);
    }

    /** ***************************************************************
     * Get the neighbors of this initial lowercase term (relation).
     */
    public ArrayList getNearestNonRelations(String term) {

        term = Character.toLowerCase(term.charAt(0)) + term.substring(1,term.length());
        return getNearestTerms(term);
    }

    /** ***************************************************************
     * Get the alphabetically num lower neighbor of this initial term, which
     * must exist in the current KB otherwise an empty string is returned.
     */
    public String getAlphaBefore(String term, int num) {

        if (!getTerms().contains(term)) {
            ArrayList al = getNearestKTerms(term,0);
            term = (String) al.get(0);
        }
        if (getTerms().size() < 1)
            return "";
        ArrayList tal = new ArrayList(getTerms());
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
            ArrayList al = getNearestKTerms(term,0);
            term = (String) al.get(0);
        }
        if (getTerms().size() < 1)
            return "";
        ArrayList tal = new ArrayList(getTerms());
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
    protected List loadFormatMapsAttempted = new ArrayList();

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

        try {
            if (formatMap == null)
                formatMap = new HashMap();
            if (termFormatMap == null)
                termFormatMap = new HashMap();
            if (formatMap.get(lang) == null)
                formatMap.put(lang, new HashMap());
            if (termFormatMap.get(lang) == null)
                termFormatMap.put(lang, new HashMap());

            if (!loadFormatMapsAttempted.contains(lang)) {
                long t1 = System.currentTimeMillis();
                ArrayList col = askWithRestriction(0,"format",1,lang);
                if ((col == null) || col.isEmpty()) {
                    logger.warning("No relation format file loaded for language "
                                       + lang);
                }
                else {
                    Map langFormatMap = (Map) formatMap.get(lang);
                    Iterator ite = col.iterator();
                    while (ite.hasNext()) {
                        Formula f = (Formula) ite.next();
                        String key = f.getArgument(2);
                        String format = f.getArgument(3);
                        format = StringUtil.removeEnclosingQuotes(format);
                        langFormatMap.put(key, format);
                    }
                }

                t1 = System.currentTimeMillis();
                col = askWithRestriction(0,"termFormat",1,lang);
                if ((col == null) || col.isEmpty()) {
                    logger.warning("No term format file loaded for language: "
                                       + lang);
                }
                else {
                    Map langTermFormatMap = (Map) termFormatMap.get(lang);
                    Iterator ite = col.iterator();
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
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        language = lang;
    }

    /** ***************************************************************
     * Clears all loaded format and termFormat maps, for all
     * languages.
     *
     */
    protected void clearFormatMaps() {
        try {
            Map m = null;
            if (formatMap != null) {
                for (Iterator itf = formatMap.values().iterator(); itf.hasNext();) {
                    m = (Map) itf.next();
                    if (m != null) m.clear();
                }
                formatMap.clear();
            }
            if (termFormatMap != null) {
                for (Iterator itf = termFormatMap.values().iterator(); itf.hasNext();) {
                    m = (Map) itf.next();
                    if (m != null) m.clear();
                }
                termFormatMap.clear();
            }
            loadFormatMapsAttempted.clear();
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
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
    public HashMap getTermFormatMap(String lang) {
        if (!StringUtil.isNonEmptyString(lang)) {
            lang = "EnglishLanguage";
        }
        if ((termFormatMap == null) || termFormatMap.isEmpty()) {
            loadFormatMaps(lang);
        }
        HashMap langTermFormatMap = (HashMap) termFormatMap.get(lang);
        if ((langTermFormatMap == null) || langTermFormatMap.isEmpty()) {
            loadFormatMaps(lang);
        }
        return (HashMap) termFormatMap.get(lang);
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
    public HashMap getFormatMap(String lang) {

    	logger.entering("KB", "getFormatMap", "lang = " + lang);
        if (!StringUtil.isNonEmptyString(lang)) {
            lang = "EnglishLanguage";
        }
        if ((formatMap == null) || formatMap.isEmpty()) {
            loadFormatMaps(lang);
        }
        HashMap langFormatMap = (HashMap) formatMap.get(lang);
        if ((langFormatMap == null) || langFormatMap.isEmpty()) {
            loadFormatMaps(lang);
        }
        
        logger.exiting("KB", "getFormatMap", formatMap.get(lang));
        return (HashMap) formatMap.get(lang);
    }

    /** ***************************************************************
     * Deletes the user assertions file, and then reloads the KB.
     */
    public void deleteUserAssertions() {

        if (engineMap.containsKey("STP2")) {
            System.out.println("INFO in KB.deleteUserAssertions: Deleting STP2 contents.");
            STP2 stp2 = (STP2) engineMap.get("STP2");
            stp2.clear();
        }
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
                	logger.warning("Error writing configuration. " + ioe.getMessage());
                }
            }
        }
        return;
    }


	public void addNewConstituent(String filename) throws IOException {
		addConstituent(filename, true, false, true);
	}
    /** *************************************************************
     * Add a new KB constituent by reading in the file, and then
     * merging the formulas with the existing set of formulas.  All
     * assertion caches are rebuilt, the current Vampire process is
     * destroyed, and a new one is created.
     *
     * @param filename - the full path of the file being added.
     */
	public void addConstituent(String filename) throws IOException {
		addConstituent(filename, true, true, false);
    }

    /** *************************************************************
     * Add a new KB constituent by reading in the file, and then merging
     * the formulas with the existing set of formulas.
     *
     * @param filename - The full path of the file being added
     * @param buildCachesP - If true, forces the assertion caches to be rebuilt
     * @param loadVampireP - If true, destroys the old Vampire process and
     * starts a new one
     */
	public void addConstituent(String filename, boolean buildCachesP,
			boolean loadVampireP, boolean performArity) {

    	if (logger.isLoggable(Level.FINER)) {
			String[] params = { "filename = " + filename,
					"buildCachesP = " + buildCachesP,
					"loadVampireP = " + loadVampireP,
					"performArity = " + performArity };
        	logger.entering("KB", "addConstituent", params);
    	}
    	
        long t1 = System.currentTimeMillis();

        try {
            if (filename.endsWith(".owl") || filename.endsWith(".OWL") ||
                filename.endsWith(".rdf") || filename.endsWith(".RDF")) {
                OWLtranslator.read(filename);
                filename = filename + ".kif";
            }
            File constituent = new File(filename);
            String canonicalPath = constituent.getCanonicalPath();
            Iterator it;
            Iterator it2;
            KIF file = new KIF();
            String key;
            String internedFormula;
            ArrayList list;
            ArrayList newList;
            Formula f;

            if (constituents.contains(canonicalPath))
				errors.add("Error: " + canonicalPath + " already loaded.");
            logger.info("Adding " + canonicalPath + " to KB.");
            try {
                file.readFile(canonicalPath);
                errors.addAll(file.warningSet);
            }
            catch (Exception ex1) {
				StringBuilder error = new StringBuilder();
				error.append(ex1.getMessage());
                if (ex1 instanceof ParseException)
					error.append(" at line "
                                  + ((ParseException)ex1).getErrorOffset());
				error.append(" in file " + canonicalPath);
				logger.severe(error.toString());
				errors.add(error.toString());
            }

            logger.info("Parsed file " + canonicalPath + " containing " + file.formulas.keySet().size() + " KIF expressions");
            it = file.formulas.keySet().iterator();
            int count = 0;
            while (it.hasNext()) {
                // Iterate through the formulas in the file, adding them to the KB, at the appropriate key.
                key = (String) it.next();
                // Note that this is a slow operation that needs to be improved
                // System.out.println("INFO KB.addConstituent(): Key " + key);
                if ((count++ % 100) == 1) { System.out.print("."); }
                list = (ArrayList) formulas.get(key);
                if (list == null) {
                    list = new ArrayList();
                    formulas.put(key, list);
                }

                newList = (ArrayList) file.formulas.get(key);
                it2 = newList.iterator();
                while (it2.hasNext()) {
                    f = (Formula) it2.next();

					boolean correctArity = true;
					if (performArity) {
						System.out
								.println("Performing Arity Check in addConstituent");
						if (!f.hasCorrectArity(this)) {
							errors.add("The following formula rejected for incorrect arity: "
									+ f.theFormula);
							System.out
									.println("Formula rejected for incorrect arity: "
											+ f.theFormula);
							correctArity = false;
						}
					}

					if (correctArity) {
						internedFormula = f.theFormula.intern();

						if (!list.contains(f)) {
							list.add(f);
							formulaMap.put(internedFormula, f);
						} else {
							StringBuilder error = new StringBuilder();
							error.append("Warning: Duplicate axiom in ");
							error.append(f.sourceFile + " at line "
									+ f.startLine + "<br />");
							error.append(f.theFormula + "<p>");
							Formula existingFormula = (Formula) formulaMap
									.get(internedFormula);
							error.append("Warning: Existing formula appears in ");
							error.append(existingFormula.sourceFile
									+ " at line " + existingFormula.startLine
									+ "<br />");
							error.append("<p>");
							System.out.println("Duplicate detected.");
							errors.add(error.toString());
						}
					}
                }
            }

            synchronized (this.getTerms()) {
                this.getTerms().addAll(file.terms);
            }
            if (!constituents.contains(canonicalPath))
                constituents.add(canonicalPath);
            logger.info("File " + canonicalPath + " loaded in " + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");
            // Clear the formatMap and termFormatMap for this KB.
            clearFormatMaps();
            if (buildCachesP && !canonicalPath.endsWith(_cacheFileSuffix))
                buildRelationCaches();
            if (loadVampireP)
                loadVampire();
        }
        catch (Exception ex) {
			logger.severe(ex.getMessage() + "; \nStack Trace: "
					+ ex.getStackTrace());
        }
        
		logger.exiting("KB", "addConstituent", "Constituent " + filename
				+ "successfully added to KB: " + this.name);

    }

    /** ***************************************************************
     * Reload all the KB constituents.
     */
    public String reload() {

    	logger.entering("KB", "reload");
        String result = "";
        try {
            StringBuilder sb = new StringBuilder();
            ArrayList newConstituents = new ArrayList();
            String cName = null;
            synchronized (this.getTerms()) {
                for (Iterator ci = constituents.iterator(); ci.hasNext();) {
                    cName = (String) ci.next();

                    // Don't reuse the same cached data.  Instead, recompute
                    // it.
                    if (!cName.endsWith(_cacheFileSuffix))
                        newConstituents.add(cName);
                }
                constituents.clear();
                formulas.clear();
                formulaMap.clear();
                terms.clear();
                clearFormatMaps();
				errors.clear();

                int i = 0;
                for (Iterator nci = newConstituents.iterator(); nci.hasNext(); i++) {
                    cName = (String) nci.next();
                    if (i == 0) System.out.println("INFO in KB.reload()");
                    System.out.println("  constituent == " + cName);
					addConstituent(cName, false, false, false);
                }

				// Reperform arity checks on everything
				checkArity();

                // Rebuild the in-memory relation caches.
                buildRelationCaches();

                // If cache == yes, write the cache file.
                if (KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes"))
					cache();

                // At this point, we have reloaded all constituents, have
                // rebuilt the relation caches, and, if cache == yes, have
                // written out the _Cache.kif file.  Now we reload the
                // inference engine.
                loadVampire();
            }

            result = sb.toString();
        }
        catch (Exception ex) {
        	logger.severe(ex.getStackTrace().toString());
            ex.printStackTrace();
        }

        logger.exiting("KB", "reload");
        return result;
    }

    /** ***************************************************************
     * Write a KIF file consisting of all the formulas in the
     * knowledge base.
     * @param fname - the name of the file to write, including full path.
     */
    public void writeFile(String fname) throws IOException {

        FileWriter fr = null;
        PrintWriter pr = null;
        HashSet formulaSet = new HashSet();

        Iterator it = formulas.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            ArrayList list = (ArrayList) formulas.get(key);
            for (int i = 0; i < list.size(); i++) {
                Formula f = (Formula) list.get(i);
                String s = f.theFormula;
                formulaSet.add(s);
            }
        }
        try {
            fr = new FileWriter(fname);
            pr = new PrintWriter(fr);

            it = formulaSet.iterator();
            while (it.hasNext()) {
                String s = (String) it.next();
                pr.println(s);
                pr.println();
            }
        }
        catch (java.io.IOException e) {
        	logger.throwing("KB", "writeFile", new IOException("Error writing file " + fname));
            throw new IOException("Error writing file " + fname);
        }
        finally {
            if (pr != null) {
                pr.close();
            }
            if (fr != null) {
                fr.close();
            }
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
    private static HashMap REGEX_PATTERNS = null;

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
            if (al != null) {
                return ((Integer)al.get(1)).intValue();
            }
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
            REGEX_PATTERNS = new HashMap();
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
    public static ArrayList getMatches(String input, String patternKey, ArrayList accumulator) {

        ArrayList ans = null;
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
                            if (ans == null) {
                                ans = new ArrayList();
                            }
                            if (!(ans.contains(rv))) {
                                ans.add(rv);
                            }
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
    public static ArrayList getMatches(String input, String patternKey) {
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
    public ArrayList askWithLiteral(List queryLit) {

        ArrayList ans = new ArrayList();
        if ((queryLit instanceof List) && !(queryLit.isEmpty())) {
            String pred = (String) queryLit.get(0);
            if (pred.equals("instance")
                && isVariable((String)queryLit.get(1))
                && !(isVariable((String)queryLit.get(2)))) {
                String className = (String)queryLit.get(2);
                String inst = null;
                String fStr = null;
                Formula f = null;
                Set ai = getAllInstances(className);
                Iterator it = ai.iterator();
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
                String inst = null;
                String fStr = null;
                Formula f = null;
                Set ai = getAllInstances("Relation");
                Iterator it = ai.iterator();
                int valence = 0;
                while (it.hasNext()) {
                    inst = (String) it.next();
                    valence = getValence(inst);
                    if (valence > 0) {
                        fStr = ("(valence " + inst + " " + valence + ")");
                        f = new Formula();
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
    public ArrayList askWithLiteral(Formula queryLit) {
        List input = queryLit.literalToArrayList();
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
     */
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
    }

    /** ***************************************************************
     * This method retrieves all subclasses of className, using both
     * class and predicate (subrelation) subsumption.
     *
     * @param className The name of a Class.
     *
     * @return A Set of terms (string constants), which could be
     * empty.
     */
    public Set getAllSubClassesWithPredicateSubsumption(String className) {

        Set ans = new TreeSet();
        try {
            if (StringUtil.isNonEmptyString(className)) {

                // Get all subrelations of subrelation.
                Set metarelations = getCachedRelationValues("subrelation", "subrelation", 2, 1);
                metarelations.add("subrelation");
                Set relations = new HashSet();
                Iterator it = metarelations.iterator();
                String pred = null;

                // Get all subrelations of subclass.
                while (it.hasNext()) {
                    pred = (String) it.next();
                    relations.addAll(getCachedRelationValues(pred, "subclass", 2, 1));
                }
                relations.add("subclass");

                // Get all subclasses of className.
                for (it = relations.iterator(); it.hasNext();) {
                    pred = (String) it.next();
                    ans.addAll(getCachedRelationValues(pred, className, 2, 1));
                }
                /*
                  Set done = new HashSet();
                  Set accumulator = new HashSet();
                  List working = new ArrayList();
                  working.add(className);
                  String name = null;
                  List tmp = null;
                  Iterator it = null;
                  Formula f = null;
                  while (!working.isEmpty()) {
                  it = working.iterator();
                  while (it.hasNext()) {
                  name = (String) it.next();
                  tmp = (List) askWithPredicateSubsumption("subclass", 2, name);
                  for (int j = 0; j < tmp.size(); j++) {
                  f = (Formula) tmp.get(j);
                  if (!done.contains(f.theFormula)) {
                  accumulator.add(f.getArgument(1));
                  done.add(f.theFormula);
                  }
                  }
                  }
                  working.clear();
                  ans.addAll(accumulator);
                  working.addAll(accumulator);
                  accumulator.clear();
                  }
                */
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     * This method retrieves all superclasses of className, using both
     * class and predicate (subrelation) subsumption.
     *
     * @param className The name of a Class.
     *
     * @return A Set of terms (string constants), which could be
     * empty.
     */
    public Set getAllSuperClassesWithPredicateSubsumption(String className) {
        /*
          System.out.println("ENTER KB.getAllSuperClassesWithPredicateSubsumption("
          + className
          + ")");
        */
        Set ans = new TreeSet();
        try {
            if (StringUtil.isNonEmptyString(className)) {

                // Get all subrelations of subrelation.
                Set metarelations = getCachedRelationValues("subrelation", "subrelation", 2, 1);
                metarelations.add("subrelation");
                Set relations = new HashSet();
                Iterator it = metarelations.iterator();
                String pred = null;

                // Get all subrelations of subclass.
                while (it.hasNext()) {
                    pred = (String) it.next();
                    relations.addAll(getCachedRelationValues(pred, "subclass", 2, 1));
                }
                relations.add("subclass");

                // Get all superclasses of className.
                for (it = relations.iterator(); it.hasNext();) {
                    pred = (String) it.next();
                    ans.addAll(getCachedRelationValues(pred, className, 1, 2));
                }

                /*
                  Set done = new HashSet();
                  Set accumulator = new HashSet();
                  List working = new ArrayList();
                  working.add(className);
                  String name = null;
                  List tmp = null;
                  Iterator it = null;
                  Formula f = null;
                  while (!working.isEmpty()) {
                  it = working.iterator();
                  while (it.hasNext()) {
                  name = (String) it.next();
                  tmp = (List) askWithPredicateSubsumption("subclass", 1, name);
                  for (int j = 0; j < tmp.size(); j++) {
                  f = (Formula) tmp.get(j);
                  if (!done.contains(f.theFormula)) {
                  accumulator.add(f.getArgument(2));
                  done.add(f.theFormula);
                  }
                  }
                  }
                  working.clear();
                  ans.addAll(accumulator);
                  working.addAll(accumulator);
                  accumulator.clear();
                  }
                */
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        /*
          System.out.println("EXIT KB.getAllSuperClassesWithPredicateSubsumption("
          + className
          + ") == "
          + ans);
        */
        return ans;
    }

    /** ***************************************************************
     * This method retrieves all instances of className, using both
     * predicate (subrelation) and class subsumption.
     *
     * @param className The name of a Class
     *
     * @return A Set of terms (string constants), which could be
     * empty
     */
    public Set getAllInstancesWithPredicateSubsumption(String className) {
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
     * @return A Set of terms (string constants), which could be
     * empty
     */
    public Set getAllInstancesWithPredicateSubsumption(String className,
                                                       boolean gatherSubclasses) {
        Set ans = new TreeSet();
        try {
            if (StringUtil.isNonEmptyString(className)) {

                // Get all subrelations of subrelation.
                Set metarelations = getCachedRelationValues("subrelation", "subrelation", 2, 1);
                metarelations.add("subrelation");
                Set relations = new HashSet();
                Iterator it = metarelations.iterator();
                String pred = null;

                // Get all subrelations of instance.
                while (it.hasNext()) {
                    pred = (String) it.next();
                    relations.addAll(getCachedRelationValues(pred, "instance", 2, 1));
                }
                relations.add("instance");

                // Get all "local" or "immediate" instances of
                // className, using instance and all gathered
                // subrelations of instance.
                Iterator itr = null;
                for (itr = relations.iterator(); itr.hasNext();) {
                    pred = (String) itr.next();
                    ans.addAll(getCachedRelationValues(pred, className, 2, 1));
                }

                if (gatherSubclasses) {
                    Set subclasses = getAllSubClassesWithPredicateSubsumption(className);
                    // subclasses.add(className);
                    String cl = null;
                    for (Iterator its = subclasses.iterator(); its.hasNext();) {
                        cl = (String) its.next();
                        for (itr = relations.iterator(); itr.hasNext();) {
                            pred = (String) itr.next();
                            ans.addAll(getTermsViaAskWithRestriction(0,pred,2,cl,1));
                        }
                    }
                }

                /*
                  working.add(className);
                  String name = null;
                  List tmp = null;
                  Formula f = null;
                  Iterator it2 = null;
                  for (Iterator it1 = working.iterator(); it1.hasNext();) {
                  name = (String) it1.next();
                  tmp = (List) askWithPredicateSubsumption("instance", 2, name);
                  for (it2 = tmp.iterator(); it2.hasNext();) {
                  f = (Formula) it2.next();
                  ans.add(f.getArgument(1));
                  }
                  }
                */

            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     * This method retrieves all classes of which term is an instance,
     * using both class and predicate (subrelation) subsumption.
     *
     * @param term The name of a SUO-KIF term.
     *
     * @return A Set of terms (class names), which could be
     * empty.
     */
    public Set getAllInstanceOfsWithPredicateSubsumption(String term) {
        /*
          System.out.println("ENTER KB.getAllInstanceOfsWithPredicateSubsumption("
          + term
          + ")");
        */
        Set ans = new TreeSet();
        try {
            if (StringUtil.isNonEmptyString(term)) {

                // Get all subrelations of subrelation.
                Set metarelations = getCachedRelationValues("subrelation", "subrelation", 2, 1);
                metarelations.add("subrelation");
                Set relations = new HashSet();
                Iterator it = metarelations.iterator();
                String pred = null;

                // Get all subrelations of instance.
                while (it.hasNext()) {
                    pred = (String) it.next();
                    relations.addAll(getCachedRelationValues(pred, "instance", 2, 1));
                }
                relations.add("instance");

                // Get all classes of which term is an instance.
                Set classes = new HashSet();
                for (it = relations.iterator(); it.hasNext();) {
                    pred = (String) it.next();
                    classes.addAll(getCachedRelationValues(pred, term, 1, 2));
                }
                ans.addAll(classes);

                // Get all superclasses of classes.
                String cl = null;
                for (it = classes.iterator(); it.hasNext();) {
                    cl = (String) it.next();
                    ans.addAll(getAllSuperClassesWithPredicateSubsumption(cl));
                }

                /*
                  List tmp = (List) askWithPredicateSubsumption("instance", 1, term);
                  if ((tmp != null) && !tmp.isEmpty()) {
                  List working = new ArrayList();
                  Formula f = null;

                  // Initialize working
                  for (int i = 0; i < tmp.size(); i++) {
                  f = (Formula) tmp.get(i);
                  working.add(f.getArgument(2));
                  }
                  if (!working.isEmpty()) {
                  ans.addAll(working);
                  String className = null;
                  for (int j = 0; j < working.size(); j++) {
                  className = (String) working.get(j);
                  ans.addAll(getAllSuperClassesWithPredicateSubsumption(className));
                  }
                  }
                  }
                */

            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        /*
          System.out.println("EXIT KB.getAllInstanceOfsWithPredicateSubsumption("
          + term
          + ") == "
          + ans);
        */
        return ans;
    }

    /** ***************************************************************
     * This method retrieves the downward transitive closure of all Class
     * names contained in the input set.  The members of the input set are
     * not included in the result set.
     *
     * @param classNames A Set object containing SUO-KIF class names
     * (Strings).
     *
     * @return A Set of SUO-KIF class names, which could be empty.
     */
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
    }

    /** ***************************************************************
     * This method retrieves the downward transitive closure of all Class
     * names contained in the input set.  The members of the input set are
     * not included in the result set.
     *
     * @param className A String containing a SUO-KIF class name
     *
     * @return A Set of SUO-KIF class names, which could be empty.
     */
    public Set<String> getAllSubClasses(String className) {

    	HashSet<String> hs = new HashSet<String>();
    	hs.add(className);
    	return getAllSubClasses(hs);
    }

    /** ***************************************************************
     * This method retrieves all instances of the classes named in the
     * input set.
     *
     * @param classNames A Set object containing SUO-KIF class names
     * (Strings).
     *
     * @return A TreeSet, possibly empty, containing SUO-KIF constant names.
     */
    protected TreeSet<String> getAllInstances(Set classNames) {

        // System.out.println("ENTER KB.getAllInstances(" + classNames + ")");
        TreeSet ans = new TreeSet();
        try {
            if ((classNames instanceof Set) && !classNames.isEmpty()) {
                String name = null;
                for (Iterator it = classNames.iterator(); it.hasNext();) {
                    name = (String) it.next();
                    ans.addAll(getCachedRelationValues("instance", name, 2, 1));
                }
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        // System.out.println("EXIT KB.getAllInstances(" + classNames + ")");
        // System.out.println("=> " + ans);
        return ans;
    }

    /** ***************************************************************
     * This method retrieves all instances of the class named in the
     * input String.
     *
     * @param className The name of a SUO-KIF Class.
     *
     * @return A TreeSet, possibly empty, containing SUO-KIF constant names.
     */
    public TreeSet<String> getAllInstances(String className) {
        if (StringUtil.isNonEmptyString(className)) {
            TreeSet input = new TreeSet();
            input.add(className);
            return getAllInstances(input);
        }
        return new TreeSet();
    }

    /** ***************************************************************
     * This method tries to find or compute a valence for the input
     * relation.
     *
     * @param relnName A String, the name of a SUO-KIF Relation.
     *
     * @return An int value. -1 means that no valence value could be
     * found.  0 means that the relation is a VariableArityRelation.
     * 1-5 are the standard SUO-KIF valence values.
     */
    public int getValence(String relnName) {

        // boolean trace = relnName.equals("immediateSubclass");
        // System.out.println("INFO in KB.getValence(" + relnName + ")");

        int ans = -1;
        try {
            if (StringUtil.isNonEmptyString(relnName)) {

                // First, see if the valence has already been cached.
                if (relationValences != null) {
                    int[] rv = (int[]) relationValences.get(relnName);
                    if (rv != null) {
                        ans = rv[0];
                        return ans;
                    }
                }

                // Grab all of the superrelations too, since we have
                // already computed them.
                Set relnSet = getCachedRelationValues("subrelation", relnName, 1, 2);
                relnSet.add(relnName);

                Iterator it = relnSet.iterator();
                List literals = null;
                String relation = null;
                while (it.hasNext() && (ans < 0)) {

                    relation = (String) it.next();

                    // First, check to see if the KB actually contains an
                    // explicit valence value.  This is unlikely.
                    literals = askWithRestriction(1, relation, 0, "valence");
                    if ((literals != null) && !(literals.isEmpty())) {
                        Formula f = (Formula) literals.get(0);
                        String digit = f.getArgument(2);
                        if (StringUtil.isNonEmptyString(digit)) {
                            ans = Integer.parseInt(digit);
                            if (ans >= 0) {
                                break;
                            }
                        }
                    }

                    // See which valence-determining class the
                    // relation belongs to.

                    Set classNames = getCachedRelationValues("instance", relation, 1, 2);

                    // System.out.println("classNames == " + classNames);

                    if (classNames != null) {
                        String[][] tops = { {"VariableArityRelation", "0"},
                                            {"UnaryFunction",         "1"},
                                            {"BinaryRelation",        "2"},
                                            {"TernaryRelation",       "3"},
                                            {"QuaternaryRelation",    "4"},
                                            {"QuintaryRelation",      "5"},
                        };

                        for (int i = 0 ; (ans < 0) && (i < tops.length) ; i++) {

                            if (classNames.contains(tops[i][0])) {
                                ans = Integer.parseInt(tops[i][1]);

                                // Sigh.  It's never simple.  The kluge
                                // below is to deal with the fact that a
                                // function, by definition, has a valence
                                // one less than the corresponding
                                // predicate.  An instance of
                                // TernaryRelation that is also an instance
                                // of Function has a valence of 2, not 3.
                                if ((i > 1)
                                    && (relation.endsWith("Fn") || classNames.contains("Function"))
                                    && !(tops[i][0]).endsWith("Function")) {
                                    --ans;
                                }
                                break;
                            }
                        }
                    }
                }
                // Cache the answer, if there is one.
                if (ans >= 0) {
                    int[] rv = new int[1];
                    rv[0] = ans;
                    relationValences.put(relnName, rv);
                }
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }

        /*
          if (trace) {
          System.out.println("EXIT KB.getValence("
          + this.name + ", "
          + relnName + ")");
          System.out.println("  ==> " + ans);
          }
        */

        return ans;
    }

    /** ***************************************************************
     *
     * @return an ArrayList containing all predicates in this KB.
     *
     */
    public ArrayList collectPredicates() {
        return new ArrayList(getCachedRelationValues("instance", "Predicate", 2, 1));
    }

    /** ***************************************************************
     *
     * @param obj Any object
     *
     * @return true if obj is a String representation of a LISP empty
     * list, else false.
     *
     */
    public static boolean isEmptyList(Object obj) {
        return (StringUtil.isNonEmptyString(obj) && Formula.empty((String) obj));
    }

    /** ***************************************************************
     *
     * A utility method.
     *
     * @param objList A list of anything.
     *
     * @param label An optional label (String), or null.
     *
     * @return void
     *
     */
    public static void printAll(List objList, String label) {
        if (objList instanceof List) {
            Iterator it = objList.iterator();
            while (it.hasNext()) {
                if (StringUtil.isNonEmptyString(label)) {
                	logger.fine(label + ": " + it.next());
                }
                else {
                    logger.fine(it.next().toString());
                }
            }
        }
        return;
    }

    /** ***************************************************************
     *
     * A static utility method.
     *
     * @param obj Presumably, a String.
     *
     * @return true if obj is a SUO-KIF variable, else false.
     *
     */
    public static boolean isVariable(String obj) {
        if (StringUtil.isNonEmptyString(obj)) {
            return (obj.startsWith("?") || obj.startsWith("@"));
        }
        return false;
    }

    /** ***************************************************************
     *
     * A static utility method.
     *
     * @param obj A String.
     *
     * @return true if obj is a SUO-KIF logical quantifier, else
     * false.
     *
     */
    public static boolean isQuantifier(String obj) {

        return (StringUtil.isNonEmptyString(obj)
                && (obj.equals("forall") || obj.equals("exists")));
    }

    /** ***************************************************************
     *
     * A static utility method.
     *
     * @param obj Presumably, a String.
     *
     * @return true if obj is a SUO-KIF commutative logical operator,
     * else false.
     *
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
        try {
            if (StringUtil.isNonEmptyString(formatted)) {
                boolean isStaticFile = false;
                StringBuilder sb = new StringBuilder(formatted);
                String suffix = "";
                if (StringUtil.emptyString(href)) {
                    href = "";
                    suffix = ".html";
                    isStaticFile = true;
                }
                else if (!href.endsWith("&term=")) {
                    href += "&term=";
                }
                int i = -1;
                int j = -1;
                int start = 0;
                String term = "";
                String formToPrint = "";
                boolean namespace = false;
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
                        formToPrint = DocGen.getInstance(this.name).showTermName(this,
                                                                                 term,
                                                                                 language);
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
        }
        catch (Exception ex) {
            logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return formatted;
    }

    /** *************************************************************
     *  Pull all the formulas into one TreeSet of Strings.
     */
    private TreeSet collectAllFormulas(HashMap forms) {

        TreeSet ts = new TreeSet();
        List al = new ArrayList(forms.values());
        List al2 = null;

        Iterator it2 = null;
        for (Iterator it = al.iterator(); it.hasNext();) {
            al2 = (ArrayList) it.next();
            for (it2 = al2.iterator(); it2.hasNext();)
                ts.add(((Formula) it2.next()).theFormula);
        }
        return ts;
    }

    /** *************************************************************
     *  Pull all the formulas in an ArrayList into one TreeSet of Strings.
     */
    private TreeSet collectFormulasFromList(ArrayList forms) {

        TreeSet ts = new TreeSet();
        for (Iterator it = forms.iterator(); it.hasNext();)
            ts.add(((Formula) it.next()).theFormula);
        return ts;
    }

    /** *************************************************************
     * Save the contents of the current KB to a file.
     */
    public String writeInferenceEngineFormulas(TreeSet forms) {

        // System.out.println("file separator == " + File.separator);

        FileWriter fr = null;
        PrintWriter pr = null;
        String filename = null;
        try {
            String inferenceEngine = KBmanager.getMgr().getPref("inferenceEngine");
            File executable = null;
            if (StringUtil.isNonEmptyString(inferenceEngine)) {
                executable = new File(inferenceEngine);
                if (DEBUG || executable.exists()) {
                    File dir = executable.getParentFile();
                    File file = new File(dir, (this.name + "-v.kif"));
                    filename = file.getCanonicalPath();

                    // System.out.println("filename == " + filename);

                    fr = new FileWriter(filename);
                    pr = new PrintWriter(fr);
                    for (Iterator it = forms.iterator(); it.hasNext();) {
                        pr.println((String) it.next());
                        pr.println();
                    }
                    if (!file.exists()) {
                    	logger.throwing("KB", "writeInferenceEngineFormulas", new Exception("Error writing " + file.getCanonicalPath()));
                        throw new Exception("Error writing " + file.getCanonicalPath());
                    }
                }
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        if (pr != null) {
            try {
                pr.close();
            }
            catch (Exception e1) {
            	logger.warning(e1.getStackTrace().toString());
            }
        }
        if (fr != null) {
            try {
                fr.close();
            }
            catch (Exception e2) {
            	logger.warning(e2.getStackTrace().toString());
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

    	logger.info("Factory name = " + factory.getClass().getName());

    	InferenceEngine res = null;
        try {
            if (!formulaMap.isEmpty()) {
                logger.fine("preprocessing " + formulaMap.size() + " formulas");
                TreeSet<String> forms = preProcess(formulaMap.keySet());
                String filename = writeInferenceEngineFormulas(forms);
                boolean vFileSaved = Formula.isNonEmptyString(filename);
                if (vFileSaved)
                    logger.fine(forms.size() + " formulas saved to " + filename);
                else
                	logger.fine("new -v.kif file not written");
                if (vFileSaved && !factory.getClass().getName().equals("com.articulate.sigma.STP$STPEngineFactory")) {
                	logger.fine("getting new inference engine");
                    res = factory.createFromKBFile(filename);
                    logger.fine("created " + res);
                }
                if (factory.getClass().getName().equals("com.articulate.sigma.STP$STPEngineFactory")) {
                    res = factory.createWithFormulas(forms);
                }
            }
        }
        catch (Exception e) {
            logger.warning(e.getMessage());
            logger.warning("Stack trace = " + e.getStackTrace());
            e.printStackTrace();
        }
        return res;
    }

    /** *************************************************************
     *  Starts Vampire and collects, preprocesses and loads all of the
     *  constituents into it.
     */
    public void loadVampire() {

    	logger.entering("KB", "loadVampire");
        // System.out.println("INFO in KB.loadVampire()");
        KBmanager mgr = KBmanager.getMgr();
        try {
            if (!formulaMap.isEmpty()) {
            	logger.fine("preprocessing " + formulaMap.size() + " formulae");
                TreeSet forms = preProcess(formulaMap.keySet());
                String filename = writeInferenceEngineFormulas(forms);
                boolean vFileSaved = StringUtil.isNonEmptyString(filename);
                if (vFileSaved)
                	logger.fine(forms.size() + " formulae saved to " + filename);
                else
                	logger.fine("new -v.kif file not written");

                if (inferenceEngine instanceof InferenceEngine) {
                	logger.fine("terminating inference engine");
                    long t1 = System.currentTimeMillis();
                    inferenceEngine.terminate();
                    logger.fine("inference engine terminated in "
                                       + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");
                }
                inferenceEngine = null;
                if (StringUtil.isNonEmptyString(mgr.getPref("inferenceEngine")) && vFileSaved) {
                	logger.fine("getting new inference engine");
                    inferenceEngine = Vampire.getNewInstance(filename);
                }
                logger.fine("inferenceEngine == " + inferenceEngine);
            }
        }
        catch (Exception e) {
            logger.severe(e.getMessage());
            logger.severe(e.getStackTrace().toString());
            e.printStackTrace();
        }
		if (inferenceEngine == null)
            mgr.setError(mgr.getError() + "\n<br/>No local inference engine is available\n<br/>");
        return;
    }

    /** A utility array for profiling subtasks in KB.preProcess(). */
    protected static long[] ppTimers = { 0L,  // type pred (sortal) computation
                                         0L,  // pred var instantiation
                                         0L,  // row var expansion
                                         0L,  // Formula.getRowVarExpansionRange()
                                         0L,  // Formula.toNegAndPosLitsWithRenameInfo()
                                         0L,  // Formula.adjustExpansionCount()
                                         0L,  // Formula.preProcessRecurse()
                                         0L,  // Formula.makeQuantifiersExplicit()
                                         0L   // Formula.insertTypeRestrictions()
    };

    /** ***************************************************************
     */
    private void printPreProcessTimers(long[] ppTimers, TreeSet newTreeSet, long dur) {

        logger.info("PreProcess Timers: "
        			+ "\n " + (dur / 1000.0) + " seconds total to produce " + newTreeSet.size() + " formulas"
			        + "\n " + (ppTimers[1] / 1000.0) + " seconds instantiating predicate variables"
			        + "\n " + (ppTimers[2] / 1000.0) + " seconds expanding row variables"
			        + "\n " + (ppTimers[3] / 1000.0) + " seconds in Formula.getRowVarExpansionRange()"
			        + "\n " + (ppTimers[4] / 1000.0) + " seconds in Formula.toNegAndPosLitsWithRenameInfo()"
			        + "\n " + (ppTimers[5] / 1000.0) + " seconds in Formula.adjustExpansionCount()"
			        + "\n " + (ppTimers[0] / 1000.0) + " seconds adding type predicates"
			        + "\n " + (ppTimers[7] / 1000.0) + " seconds making quantifiers explicit"
			        + "\n " + (ppTimers[8] / 1000.0) + " seconds inserting type restrictions"
			        + "\n " + (ppTimers[6] / 1000.0) + " seconds in Formula.preProcessRecurse()");
    }

    /** ***************************************************************
     * Preprocess the knowledge base to work with Vampire.  This includes "holds"
     * prefixing, ticking nested formulas, expanding row variables, and
     * translating mathematical relation operators.
     * @return a TreeSet of Strings.
     */
    public TreeSet preProcess(Set forms) {

    	logger.entering("KB", "preProcess", forms);

    	TreeSet newTreeSet = new TreeSet();
        try {
            KBmanager mgr = KBmanager.getMgr();
            for (int i = 0 ; i < ppTimers.length ; i++)
                ppTimers[i] = 0L;
            resetSortalTypeCache();
            boolean tptpParseP = mgr.getPref("TPTP").equalsIgnoreCase("yes");
            long t1 = System.currentTimeMillis();
            String form = null;
            Formula f = null;
            // Formula newFormula = null;
            ArrayList processed = null;         // An ArrayList of Formula(s).

            Iterator it = null;
            int numberProcessed = 0;
            long t_prevTotal = 0L;
            long t_total = 0L;
            for (it = forms.iterator(); it.hasNext(); numberProcessed++) {
                long t_start = System.currentTimeMillis();
                form = (String) it.next();
                f = (Formula) formulaMap.get(form);
                // newFormula = new Formula();
                // newFormula.theFormula = new String(f.theFormula);
                // System.out.println("preProcess " + newFormula);
                // processed = newFormula.preProcess(false,this);   // not queries
                processed = f.preProcess(false, this);   // not queries

                if (tptpParseP) {
                    try {
                        f.tptpParse(false, this, processed);   // not a query
                    }
                    catch (ParseException pe) {
                        String er = ("Error in KB.preProcess(): " + pe.getMessage() + " at line "
                                     + f.startLine + " in file " + f.sourceFile);
                        mgr.setError(mgr.getError() + "\n<br/>" + er + "\n<br/>");
                        logger.warning(er);
                    }
                    catch (IOException ioe) {
                    	logger.warning(ioe.getMessage());
                    }
                }

				errors.addAll(f.getErrors());

                Formula p = null;
                for (Iterator itp = processed.iterator(); itp.hasNext();) {
                    p = (Formula) itp.next();
					if (StringUtil.isNonEmptyString(p.theFormula)) {
                        newTreeSet.add(p.theFormula);
						errors.addAll(p.getErrors());
					}
                }
                long t_elapsed = (System.currentTimeMillis() - t_start);
                t_total += t_elapsed;
                if ((numberProcessed > 0) && ((numberProcessed % 1000) == 0)) {
                    logger.info("Formulae per second per last 1000: "
                                       + (1000.0 / ((t_total - t_prevTotal) / 1000.0)));
                    t_prevTotal = t_total;
                }
                if (t_elapsed > 3000) {
                    logger.finer((t_elapsed / 1000.0) + " seconds to process form:"
                    				+ "\nf == " + f.toString()
                    				+ "\nprocessed == "
                                       + (processed.isEmpty()
                                          ? "no result forms generated"
                                          : (processed.get(0)
                                             + getLineSeparator()
                                             + "  and "
                                             + (processed.size() - 1)
                                             + " other result forms")));
                }
            }
            long dur = (System.currentTimeMillis() - t1);
            printPreProcessTimers(ppTimers,newTreeSet,dur);
            for (int i = 0 ; i < ppTimers.length ; i++)
                ppTimers[i] = 0L;
            if (tptpParseP) {
                int goodCount = 0;
                int badCount = 0;
                List badList = new ArrayList();
                for (it = formulaMap.values().iterator(); it.hasNext();) {
                    f = (Formula) it.next();
                    if (f.getTheTptpFormulas().isEmpty()) {
                        badCount++;
                        if (badCount < 11)
                            badList.add(f);
                    }
                    else {
                        goodCount++;
                        if (goodCount < 10)
                            logger.fine("Sample TPTP translation: " + f.getTheTptpFormulas().get(0));
                    }
                }
                logger.fine("TPTP translation succeeded for "
                                   + goodCount + " formula" + ((goodCount == 1) ? "" : "s"));
                boolean someAreBad = (badCount > 0);
                logger.fine("TPTP translation failed for "
                                   + badCount + " formula" + ((badCount == 1) ? "" : "s") + (someAreBad ? ":" : ""));
                if (someAreBad) {
                    it = badList.iterator();
                    for (int i = 1 ; it.hasNext() ; i++) {
                        f = (Formula) it.next();
                        logger.finer("[" + i + "]: " + f);
                    }
                    if (badCount > 10)
                        logger.finer("  " + (badCount - 10) + " more ...");
                }
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        clearSortalTypeCache();
        logger.exiting("KB", "preProcess");
        return newTreeSet;
    }

    /** *************************************************************
     */
    private void writePrologFormulas(ArrayList forms, PrintWriter pr) {

        TreeSet ts = new TreeSet();
        try {
            ts.addAll(forms);
            if (forms != null) {
                int i = 0;
                Formula formula = null;
                String result = null;
                for (Iterator it = ts.iterator(); it.hasNext();) {
                    formula = (Formula) it.next();
                    result = formula.toProlog();
                    if (result != null && result != "")
                        pr.println(result);
//                    if (i % 100 == 1) System.out.print(".");
                }
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * @param fname - the name of the file to write, including full path.
     */
    public String writePrologFile(String fname) {

        File file = null;
        PrintWriter pr = null;
        String result = null;

        try {
            file = new File(fname);
            
            logger.info("Writing " + file.getCanonicalPath());

            if ((WordNet.wn != null) && WordNet.wn.wordFrequencies.isEmpty())
                WordNet.wn.readWordFrequencies();
            pr = new PrintWriter(new FileWriter(file));
            pr.println("% Copyright (c) 2006-2009 Articulate Software Incorporated");
            pr.println("% This software released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.");
            pr.println("% This is a very lossy translation to prolog of the KIF ontologies available at www.ontologyportal.org\n");

            pr.println("% subAttribute");
            writePrologFormulas(ask("arg",0,"subAttribute"),pr);
            pr.println("\n% subrelation");
            writePrologFormulas(ask("arg",0,"subrelation"),pr);
            pr.println("\n% disjoint");
            writePrologFormulas(ask("arg",0,"disjoint"),pr);
            pr.println("\n% partition");
            writePrologFormulas(ask("arg",0,"partition"),pr);
            pr.println("\n% instance");
            writePrologFormulas(ask("arg",0,"instance"),pr);
            pr.println("\n% subclass");
            writePrologFormulas(ask("arg",0,"subclass"),pr);
            System.out.println(" ");

            pr.flush();
            result = file.getCanonicalPath();
        }
        catch (Exception e) {
        	logger.warning(e.getMessage());
            e.printStackTrace();
        }
        finally {
            try {
                if (pr != null) pr.close();
            }
            catch (Exception e1) {
            	logger.warning(e1.getStackTrace().toString());
                e1.printStackTrace();
            }
        }
        return result;
    }

    /** *************************************************************
     * This method translates the entire KB to TPTP format, storing
     * the translation for each Formula in the List identified by the
     * private member Formula.theTptpFormulas.  Use
     * Formula.getTheTptpFormulas() to accesss the TPTP sentences
     * (Strings) that constitute the translation for a single SUO-KIF
     * Formula.
     *
     * @return An int indicating the number of Formulas that were
     * successfully translated.
     */
    public int tptpParse() {

        int goodCount = 0;
        try {
            int badCount = 0;
            ArrayList badList = new ArrayList();
            Formula f = null;
            Iterator it = this.formulaMap.values().iterator();
            while (it.hasNext()) {
                f = (Formula) it.next();
                f.tptpParse(false, this);
                if (f.getTheTptpFormulas().isEmpty()) {
                    badCount++;
                    if (badList.size() < 11)
                        badList.add(f);
                }
                else
                    goodCount++;
            }
            logger.fine("TPTP translation succeeded for "
                               + goodCount + " formula" + ((goodCount == 1) ? "" : "s"));
            boolean someAreBad = (badCount > 0);
            logger.fine("TPTP translation failed for "
                               + badCount + " formula" + ((badCount == 1) ? "" : "s") + (someAreBad ? ":" : ""));
            if (someAreBad) {
                it = badList.iterator();
                for (int i = 1 ; it.hasNext() ; i++) {
                    f = (Formula) it.next();
                    logger.finer("[" + i + "]: " + f);
                }
                if (badCount > 10)
                    logger.finer("  " + (badCount - 10) + " more ...");
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        return goodCount;
    }

    /** *************************************************************
     */
    public String copyFile (String fileName) {

        String outputPath = "";
        FileReader in = null;
        FileWriter out = null;
        try {
            String sanitizedKBName = name.replaceAll("\\W","_");
            File inputFile = new File(fileName);
            File outputFile = File.createTempFile(sanitizedKBName, ".p", null);
            outputPath = outputFile.getCanonicalPath();

            in = new FileReader(inputFile);
            out = new FileWriter(outputFile);

            int c;
            while ((c = in.read()) != -1)
                out.write(c);
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            }
            catch (Exception ieo) {
            	logger.warning(ieo.getStackTrace().toString());
                ieo.printStackTrace();
            }
        }
        return outputPath;
    }

    /** *************************************************************
     */
    public void addToFile (String fileName, ArrayList<String> axioms, String conjecture) {

        DataOutputStream out = null;
        try {
            boolean append = true;
            FileOutputStream file = new FileOutputStream(fileName, append);
            out = new DataOutputStream(file);
            // add axioms
            if (axioms != null) {
                for (String axiom : axioms)
                    out.writeBytes(axiom);
                out.flush();
            }
            // add conjecture
            if (StringUtil.isNonEmptyString(conjecture)) {
                out.writeBytes(conjecture);
                out.flush();
            }
        }
        catch (Exception ex) {
        	logger.warning(ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        finally {
            try {
                if (out != null)
                    out.close();
            }
            catch (Exception ioe) {
            	logger.warning(ioe.getStackTrace().toString());
                ioe.printStackTrace();
            }
        }
        return;
    }

    /** ***************************************************************
     * @param kb - The KB used to compute variable arity relations.
     * @param relationMap is a Map of String keys and values where
     *                    the key is the renamed relation and the
     *                    value is the original name.
     */
    protected void printVariableArityRelationContent(PrintWriter pr, TreeMap<String,String> relationMap,
                                                     String sanitizedKBName, int axiomIndex, boolean onlyPlainFOL) {

        Iterator it = relationMap.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            String value = (String) relationMap.get(key);
            ArrayList result = ask("arg",1,value);
            if (result != null) {
                for (int i = 0; i < result.size(); i++) {
                    Formula f = (Formula) result.get(i);
                    String s = f.theFormula.replace(value,key);
                    if (onlyPlainFOL)
                        pr.println("%FOL fof(kb_" + sanitizedKBName + "_" + axiomIndex++ +
                                   ",axiom,(" + Formula.tptpParseSUOKIFString(s) + ")).");
                    else
                        pr.println("fof(kb_" + sanitizedKBName + "_" + axiomIndex++ +
                                   ",axiom,(" + Formula.tptpParseSUOKIFString(s) + ")).");
                }
            }
        }
    }

    /** *************************************************************
     *  Sets isQuestion and calls writeTPTPFile() below
     */
    public String writeTPTPFile(String fileName,
                                Formula conjecture,
                                boolean onlyPlainFOL,
                                String reasoner) {
        final boolean isQuestion = false;
        return writeTPTPFile(fileName,
                             conjecture,
                             onlyPlainFOL,
                             reasoner,
                             isQuestion);
    }

    /** *************************************************************
     *  Sets pw and calls writeTPTPFile() below
     */
    public String writeTPTPFile(String fileName,
                                Formula conjecture,
                                boolean onlyPlainFOL,
                                String reasoner,
                                boolean isQuestion) {
        final PrintWriter pw = null;
        return writeTPTPFile(fileName,
                             conjecture,
                             onlyPlainFOL,
                             reasoner,
                             isQuestion,
                             pw);
    }

    /** *************************************************************
     *  Write all axioms in the KB to TPTP format.
     *
     * @param fileName - the full pathname of the file to write
     */
    public String writeTPTPFile(String fileName, Formula conjecture, boolean onlyPlainFOL,
                                String reasoner, boolean isQuestion, PrintWriter pw) {

    	if (logger.isLoggable(Level.FINER)) {
    		String form = "";
    		if (conjecture != null)
    			form = conjecture.theFormula;
    		String[] params = {"fileName = " + fileName, "conjecture = " + form, "onlyPlainFOL = " + onlyPlainFOL,
    					"reasoner =" + reasoner, "isQuestion = " + isQuestion, "PrintWriter = " + pw};
    		logger.entering("KB", "writeTPTPFile", params);
    	}

    	String result = null;
        PrintWriter pr = null;
        Formula f = null;
        try {
            KBmanager mgr = KBmanager.getMgr();
            File outputFile;
            int axiomIndex = 1;   // a count appended to axiom names to make a unique ID
            TreeSet orderedFormulae;
            String theTPTPFormula;
            boolean sanitizedFormula;
            boolean commentedFormula;
            TreeMap relationMap = new TreeMap(); // A Map of varaible arity relations keyed by new name
            String sanitizedKBName = name.replaceAll("\\W","_");
            //----If file name is a directory, create filename therein
            if (fileName == null) {
                outputFile = File.createTempFile(sanitizedKBName, ".p", null);
                //----Delete temp file when program exits.
                outputFile.deleteOnExit();
            }
            else
                outputFile = new File(fileName);
            String canonicalPath = outputFile.getCanonicalPath();
            logger.info("Writing " + canonicalPath);
            if (pw instanceof PrintWriter)
                pr = pw;
            else
                pr = new PrintWriter(new FileWriter(outputFile));
            // If a PrintWriter object is passed in, we suppress this
            // copyright notice and assume that such a notice will be
            // provided somewhere is the wider calling context.
            if (pw == null) {
                pr.println("% Copyright 2010 Articulate Software Incorporated");
                pr.println("% www.ontologyportal.org www.articulatesoftware.com");
                pr.println("% This software released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.");
                pr.println("% This is a translation to TPTP of KB " + sanitizedKBName);
                pr.println("");
            }
            orderedFormulae = new TreeSet(new Comparator() {
                    public int compare(Object o1, Object o2) {
                        Formula f1 = (Formula) o1;
                        Formula f2 = (Formula) o2;
                        int fileCompare = f1.sourceFile.compareTo(f2.sourceFile);
                        if (fileCompare == 0) {
                            fileCompare = (new Integer(f1.startLine))
                                .compareTo(new Integer(f2.startLine));
                            if (fileCompare == 0) {
                                fileCompare = (new Long(f1.endFilePosition))
                                    .compareTo(new Long(f2.endFilePosition));
                            }
                        }
                        return fileCompare;
                    } });
            orderedFormulae.addAll(formulaMap.values());
            // if (onlyPlainFOL) {
            resetSortalTypeCache();
            // }
            List tptpFormulas = null;
            String oldSourceFile = "";
            String sourceFile = "";
            File sf = null;
            f = null;
            for (Iterator ite = orderedFormulae.iterator(); ite.hasNext();) {
                f = (Formula) ite.next();
                sf = new File(f.sourceFile);
                sourceFile = sf.getName();
                sourceFile = sourceFile.substring(0, sourceFile.lastIndexOf("."));
                // if (!sourceFile.equals(oldSourceFile))
                //     axiomIndex = 1;
                if (!sourceFile.equals(oldSourceFile)) {
                    logger.warning("Source file has changed to " + sourceFile);
                }
                oldSourceFile = sourceFile;
                // System.out.println("\n  f == " + f);
                tptpFormulas = f.getTheTptpFormulas();
                // System.out.println("  1 : tptpFormulas == " + tptpFormulas);
                //----If we are writing "sanitized" tptp, aka onlyPlainFOL,
                //----here we rename all VariableArityRelations so that each
                //----relation name has a numeric suffix corresponding to the
                //----number of the relation's arguments.  This is required
                //----for some provers, such as E and EP.
                if (onlyPlainFOL && !tptpFormulas.isEmpty()
                    && !mgr.getPref("holdsPrefix").equalsIgnoreCase("yes")
                    && f.containsVariableArityRelation(this)) {

                    Formula tmpF = new Formula();
                    tmpF.read(f.theFormula);
                    List processed = tmpF.preProcess(false, this);
                    List withRelnRenames = null;
                    if (!processed.isEmpty()) {
                        withRelnRenames = new ArrayList();
                        Formula f2 = null;
                        for (Iterator procit = processed.iterator(); procit.hasNext();) {
                            f2 = (Formula) procit.next();
                            withRelnRenames.add(f2.renameVariableArityRelations(this,relationMap));
                        }
                        tmpF.tptpParse(false, this, withRelnRenames);
                        tptpFormulas = tmpF.getTheTptpFormulas();
                        // System.out.println("  2 : tptpFormulas == " + tptpFormulas);
                        // System.out.println("");
                    }
                }
                for (Iterator tptpIt = tptpFormulas.iterator(); tptpIt.hasNext();) {
                    theTPTPFormula = (String) tptpIt.next();
                    // System.out.println("  theTPTPFormula == " + theTPTPFormula);
                    commentedFormula = false;
                    if (onlyPlainFOL) {
                        //----Remove interpretations of arithmetic
                        theTPTPFormula =
                            theTPTPFormula
                            .replaceAll("[$]less",
                                        "dollar_less").replaceAll("[$]greater",
                                                                  "dollar_greater")
                            .replaceAll("[$]time",
                                        "dollar_times").replaceAll("[$]divide",
                                                                   "dollar_divide")
                            .replaceAll("[$]plus",
                                        "dollar_plus").replaceAll("[$]minus",
                                                                  "dollar_minus");
                        //----Don't output ""ed ''ed and numbers
                        if (theTPTPFormula.matches(".*'[a-z][a-zA-Z0-9_]*\\(.*")
                            || theTPTPFormula.indexOf('"') >= 0) {
                           // || theTPTPFormula.matches(".*[(,]-?[0-9].*"))
                            pr.print("%FOL ");
                            commentedFormula = true;
                        }
                        if (reasoner.matches(".*(?i)Equinox.*")
                            && f.theFormula.indexOf("equal") > 2) {
                            Formula f2 = new Formula();
                            f2.read(f.cdr());
                            f2.read(f.car());
                            if (f2.theFormula.equals("equal")) {
                                pr.print("%FOL ");
                                commentedFormula = true;
                            }
                        }
                    }
                    pr.println("fof(kb_" + sanitizedKBName + "_" + axiomIndex++ +
                               ",axiom,(" + theTPTPFormula + ")).");
                    // pr.println("fof(kb_" + sourceFile + "_" + axiomIndex++ + ",axiom,(" + theTPTPFormula + ")).");
                    // if (commentedFormula) {
                    pr.println();
                    // }
                }
                pr.flush();
                if (f.getTheTptpFormulas().isEmpty()) {
                    String addErrStr = "No TPTP formula for <br/>" + f.htmlFormat(this);
					// mgr.setError(mgr.getError() + "<br/>\n" + addErrStr +
					// "\n<br/>");
					errors.add(addErrStr);
                    logger.info("No TPTP formula for\n" + f);
                }
            }
            printVariableArityRelationContent(pr,relationMap,sanitizedKBName,axiomIndex,onlyPlainFOL);
            //----Print conjecture if one has been supplied
            if (conjecture != null) {
                // conjecture.getTheTptpFormulas() should return a
                // List containing only one String, so the iteration
                // below is probably unnecessary.  I don't know if the
                // provers on the target server can even handle
                // multiple conjectures.
                String type = "conjecture";
                if (isQuestion) type = "question";
                for (Iterator tptpIt = conjecture.getTheTptpFormulas().iterator();
                     tptpIt.hasNext();) {
                    theTPTPFormula = (String) tptpIt.next();
                    pr.println("fof(prove_from_" + sanitizedKBName +
                               "," + type + ",(" + theTPTPFormula + ")).");
                }
            }
            result = canonicalPath;
        }
        catch (Exception ex) {
        	logger.warning(ex.getMessage()
        			+ "\nfileName == " + fileName
            		+ "\nf == " + f);
            ex.printStackTrace();
        }
        finally {
            try {
                clearSortalTypeCache();
                if (pr != null) pr.close();
            }
            catch (Exception ioe) {
            	logger.warning(ioe.getStackTrace().toString());
                ioe.printStackTrace();
            }
        }
        logger.exiting("KB", "writeTPTPFile", result);
        return result;
    }

    /** *************************************************************
     * Instances of RelationCache hold the cached extensions and, when
     * possible, the computed closures, of selected relations.
     * Canonical examples are the caches for subclass and instance.
     *
     */
    class RelationCache extends HashMap {

        private String relationName = "";

        public String getRelationName() {
            return relationName;
        }

        private int keyArgument = -1;

        public int getKeyArgument() {
            return keyArgument;
        }

        private int valueArgument = -1;

        public int getValueArgument() {
            return valueArgument;
        }

        private boolean isClosureComputed = false;

        public boolean getIsClosureComputed() {
            return isClosureComputed;
        }

        public void setIsClosureComputed(boolean computed) {
            isClosureComputed = computed;
            return;
        }

        private RelationCache() {
        }

        public RelationCache(String predName, int keyArg, int valueArg) {
            relationName = predName;
            keyArgument = keyArg;
            valueArgument = valueArg;
        }
    }

    /** *************************************************************
     */
    private String prettyPrint(String term) {

        if (term.endsWith("Fn"))
            term = term.substring(0,term.length()-2);
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < term.length(); i++) {
            if (Character.isLowerCase(term.charAt(i)) || !Character.isLetter(term.charAt(i)))
                result.append(term.charAt(i));
            else {
                if (i + 1 < term.length() && Character.isUpperCase(term.charAt(i+1)))
                    result.append(term.charAt(i));
                else {
                    if (i != 0)
                        result.append(" ");
                    result.append(Character.toLowerCase(term.charAt(i)));
                }
            }
        }
        return result.toString();

    }

    /** *************************************************************
     *  @return a string with termFormat expressions created for all
     *  the terms in the knowledge base
     */
    private String allTerms() {

        StringBuilder result = new StringBuilder();
        synchronized (getTerms()) {
            for (Iterator it = getTerms().iterator(); it.hasNext();) {
                String term = (String) it.next();
                result.append("(termFormat EnglishLanguage ");
                result.append(term);
                result.append(" \"");
                result.append(prettyPrint(term));
                result.append("\")\n");
            }
        }
        return result.toString();
    }

    /** *************************************************************
     */
    private String functionFormat(String term, int i) {

        switch (i) {
        case 1: return "the &%" + prettyPrint(term) + " of %1";
        case 2: return "the &%" + prettyPrint(term) + " of %1 and %2";
        case 3: return "the &%" + prettyPrint(term) + " of %1, %2 and %3";
        case 4: return "the &%" + prettyPrint(term) + " of %1, %2, %3 and %4";
        }
        return "";
    }

    /** *************************************************************
     */
    private String allFunctionsOfArity(int i) {

        String parent = "";
        switch (i) {
        case 1: parent = "UnaryFunction"; break;
        case 2: parent = "BinaryFunction"; break;
        case 3: parent = "TernaryFunction"; break;
        case 4: parent = "QuaternaryFunction"; break;
        }
        if (parent == "")
            return "";
        StringBuffer result = new StringBuffer();
        synchronized (getTerms()) {
            for (Iterator it = getTerms().iterator(); it.hasNext();) {
                String term = (String) it.next();
                if (childOf(term,parent))
                    result.append("(format EnglishLanguage "
                                  + term + " \""
                                  + functionFormat(term,i)
                                  + "\")\n");
            }
        }
        return result.toString();
    }

    /** *************************************************************
     */
    private String relationFormat(String term, int i) {

        switch (i) {
        case 2: return ("%2 is %n "
                        + LanguageFormatter.getArticle(term,1,1,"EnglishLanguage")
                        + "&%"
                        + prettyPrint(term)
                        + " of %1");
        case 3: return "%1 %n{doesn't} &%" + prettyPrint(term) + " %2 for %3";
        case 4: return "%1 %n{doesn't} &%" + prettyPrint(term) + " %2 for %3 with %4";
        case 5: return "%1 %n{doesn't} &%" + prettyPrint(term) + " %2 for %3 with %4 and %5";
        }
        return "";
    }

    /** *************************************************************
     */
    private String allRelationsOfArity(int i) {

        String parent = "";
        switch (i) {
        case 2: parent = "BinaryPredicate"; break;
        case 3: parent = "TernaryPredicate"; break;
        case 4: parent = "QuaternaryPredicate"; break;
        case 5: parent = "QuintaryPredicate"; break;
        }
        if (parent == "")
            return "";
        StringBuffer result = new StringBuffer();
        synchronized (this.getTerms()) {
            for (Iterator it = this.getTerms().iterator(); it.hasNext();) {
                String term = (String) it.next();
                if (childOf(term,parent))
                    result.append("(format EnglishLanguage " + term + " \"" + relationFormat(term,i) + "\")\n");
            }
        }
        return result.toString();
    }

    /** *************************************************************
     * This method currently takes one command-line argument, which
     * should be the absolute pathname of the directory in which the
     * source Merge,kif file is located.  The resulting tptp file will
     * be named TPTP-TEST-KB.tptp, and will be written to the same
     * directory.
     */
    public static void testTPTP(String[] args) {

        try {
            if (args[0] == null) {
                System.out.println("Usage: java -classpath <path> com.articulate.sigma.KB <kb-dir>");
                System.exit(1);
            }

            KBmanager mgr = KBmanager.getMgr();

            // These three parameters, along with the consituent
            // (.kif) files loaded, determine the set of SUO-KIF
            // assertions that will serve as the source for a
            // translation of the KB to TPTP.
            mgr.setPref("holdsPrefix", "no");
            mgr.setPref("typePrefix", "no");
            mgr.setPref("cache", "no");

            // This parameter determines if the entire KB will be
            // translated to TPTP as part of the loading and
            // processing of the .kif constituent files.
            mgr.setPref("TPTP", "yes");
            mgr.setPref("inferenceEngine", null);
            mgr.setPref("kbDir", args[0]);
            mgr.kbs.clear();
            mgr.addKB("TPTP-TEST-KB");
            KB kb = mgr.getKB("TPTP-TEST-KB");
            kb.constituents.clear();
            File kbDir = new File(mgr.getPref("kbDir"));
            File kifFileToLoad = new File(kbDir, "Merge.kif");
            kb.addConstituent(kifFileToLoad.getCanonicalPath(),
                              // Compute caches of "virtual" assertions,
                              true,
                              // Don't write a file of processed
                              // SUO-KIF formulas for the inference
                              // engine, and don't try to start an
                              // inference engine process.
					false, false);
			kb.checkArity();
            kb.preProcess(kb.getFormulas());
            File tptpFile = new File(kbDir, kb.name + ".tptp");
            String fileWritten = kb.writeTPTPFile(tptpFile.getCanonicalPath(), null, false, "none");
            if (StringUtil.isNonEmptyString(fileWritten)) {
                logger.info("File written: " + fileWritten);
            }
            else {
                logger.warning("Could not write " + tptpFile.getCanonicalPath());
            }
        }
        catch (Exception e) {
        	logger.warning(e.getStackTrace().toString());
            e.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * List all terms that don't have an externalImage link
     */
    private void termsWithNoPictureLinks() {

       synchronized (this.getTerms()) {
            for (Iterator it = getTerms().iterator(); it.hasNext();) {
                String term = (String) it.next();
                ArrayList al = askWithRestriction(0,"externalImage",1,term);
                if (al == null || al.size() < 1)
                    logger.info(term + " has no picture links.");
            }
        }
    }

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
            if (f.isRule() || f.car().equals("instance") || f.car().equals("subclass")) {
                continue;
            }
            StreamTokenizer_s st = new StreamTokenizer_s(new StringReader(formula));
            KIF.setupStreamTokenizer(st);
            ArrayList al = new ArrayList();
            boolean firstToken = true;
            String predicate = "link";
            try {
                while (st.nextToken() != StreamTokenizer_s.TT_EOF) {
                    if (st.ttype == StreamTokenizer_s.TT_WORD) {
                        String token = st.sval;
                        // System.out.println("INFO in KB.generateSemanticNetwork(): token: " + token);
                        if (firstToken && !Formula.isLogicalOperator(token))
                            predicate = token;
                        if (Formula.isTerm(token) && !Formula.isLogicalOperator(token) && !firstToken)
                            al.add(token);
                        if (firstToken)
                            firstToken = false;
                    }
                }
            } catch (IOException ioe) {
                logger.warning("Error parsing: " + formula);
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
                                                // resultSet.add(predicate + " " + firstSynset + " " + secondSynset);
                                                // System.out.println(predicate + " " + firstSynset + " " + secondSynset);
                                                char firstLetter = WordNetUtilities.posNumberToLetter(firstSynset.charAt(0));
                                                char secondLetter = WordNetUtilities.posNumberToLetter(secondSynset.charAt(0));
                                                logger.fine("u:ENG-30-" + firstSynset.substring(1) + "-" + firstLetter + " " +
                                                                   "v:ENG-30-" + secondSynset.substring(1) + "-" + secondLetter);
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
        /*
        if (resultSet != null) {
            Iterator it2 = resultSet.iterator();
            while (it2.hasNext()) {
                String entry = (String) it2.next();
                System.out.println(entry);
            }
        } */
    }

    /** *************************************************************
     *  Replace variables in a formula with "gensym" constants.
     */
    private void instantiateFormula(Formula pre, ArrayList<Formula> assertions) {

    	logger.info("pre = " + pre);
        ArrayList<ArrayList<String>> al = pre.collectVariables();
        ArrayList<String> vars = new ArrayList();
        vars.addAll((ArrayList) al.get(0));
        vars.addAll((ArrayList) al.get(1));
        logger.fine("vars = " + vars);
        TreeMap<String,String> m = new TreeMap();
        for (int i = 0; i < vars.size(); i++)
            m.put((String) vars.get(i),"gensym" + (new Integer(gensym++)).toString());
        logger.fine("m = " + m);
        pre = pre.substituteVariables(m);
        assertions.add(pre);
    }

    /** *************************************************************
     *  Take the precondition from a rule and try to separate it
     *  into separate clauses that can be solved and placed into
     *  "head".
     *  @param head is the current set of preconditions which will
     *              keep getting expanded until its not possible to
     *              find more, or the proof size limit is reached.
     *              Setting this list is a side effect.
     *  @param assertions is the list of ground assertions that are
     *                    needed as background knowledge to support
     *                    the proof.  Setting this list is a side
     *                    effect.
     */
    private void extractPreconditions(Formula pre, ArrayList<Formula> head,
                                      ArrayList<Formula> assertions) {

        String pred = pre.car();
        if (pred.equals("and")) {
            Formula f = new Formula();
            f.read(pre.cdr());
            while (!f.empty()) {
                Formula f2 = new Formula();
                f2.read(f.car());
                f.read(f.cdr());
                extractPreconditions(f2,head,assertions);
            }
        }
        else if (pred.equals("not")) {
            if (!head.contains(pre))
                head.add(pre);
        }
        else if (Formula.isLogicalOperator(pred)) {
            logger.info("Rejecting formula = \n" + pre.theFormula);
            instantiateFormula(pre,assertions);
        }
        else {
            if (!head.contains(pre))
                head.add(pre);
        }
    }

    /** *************************************************************
     *  Take the consequent from a rule and try to separate it
     *  into separate clauses
     *  @return an ArrayList of Formulas
     */
    private ArrayList<Formula> extractConsequentClauses(Formula form) {

        ArrayList<Formula> result = new ArrayList();
        String pred = form.car();
        if (pred.equals("and")) {
            Formula f = new Formula();
            f.read(form.cdr());
            while (!f.empty()) {
                Formula f2 = new Formula();
                f2.read(f.car());
                f.read(f.cdr());
                result.addAll(extractConsequentClauses(f2));
            }
        }
  //      else if (pred.equals("exists")) {
  //          result.addAll(extractConsequentClauses(form));
  //      }
        else {
            result.add(form);
        }
        return result;
    }

    /** *************************************************************
     *  Find the ground formula that satisfies this
     *  statement, which is assumed to be a simple clause.
     */
    private Formula findGroundFormulaInferenceMatch(Formula f) {

    	logger.entering("KB", "findGroundFormulaInferenceMatch", f);
        if (f.argumentsToArrayList(0) == null)
            return null;
        String arg1 = null;
        String arg2 = null;
        int argnum1 = 0;
        int argnum2 = 0;
        int i = 0;
        while (arg2 == null && i < f.argumentsToArrayList(0).size()) {
            String arg = f.getArgument(i++);
            if (arg1 == null) {
                arg1 = f.getArgument(i-1);
                argnum1 = i-1;
            }
            else {
                arg2 = f.getArgument(i-1);
                argnum2 = i-1;
            }
        }
        ArrayList<Formula> al = null;
        if (arg1 != null && arg2 == null) {
            al = ask("stmt",argnum1,arg1);
        }
        al = askWithRestriction(argnum1,arg1,argnum2,arg2);
        Formula f2 = null;
        for (int j = 0; j < al.size(); j++) {
            TreeMap m = f.unify(f2);
            if (m != null) {
                return f2.substitute(m);
            }
        }
        logger.exiting("KB", "findGroundFormulaInferenceMatch");
        return null;
    }

    /** *************************************************************
     *  Find the first rule that satisfies this
     *  statement, which is assumed to be a simple clause
     */
    private ArrayList<String> removeVariablesFromList(ArrayList<String> al) {

    	logger.entering("KB", "removeVariablesFromList", al);
        ArrayList<String> result = new ArrayList();
        if (al == null)
            return result;
        for (int i = 0; i < al.size(); i++) {
            String s = (String) al.get(i);
            if (!Formula.isVariable(s) && !result.contains(s))
                result.add(s);
        }
        logger.exiting("KB", "removeVariablesFromList", result);
        return result;
    }

    /** *************************************************************
     *  Find the first rule that satisfies this
     *  statement, which is assumed to be a simple clause
     */
    private Formula findRuleInferenceMatch(Formula f) {

    	logger.entering("KB", "findRuleInferenceMatch", f);
        ArrayList<String> termList = f.argumentsToArrayList(0);
        termList = removeVariablesFromList(termList);
        logger.fine("terms: " + termList);

        if (termList.size() > 0) {
            ArrayList<Formula> rules = ask("cons",0,(String) termList.get(termList.size()-1));
            logger.fine("Found " + rules.size() + " matching rules");
            for (int i = 0; i < rules.size(); i++) {
                Formula f2 = (Formula) rules.get(i);
                boolean match = true;
                String consequent = f2.getArgument(2);
                logger.fine("consequent: " + consequent);
                Formula f3 = new Formula();
                f3.read(consequent);
                String pred = f3.getArgument(0);
                if (f2.theFormula.indexOf("or") < 0 && f2.theFormula.indexOf("exists") < 0 &&
                    !f2.isHigherOrder()) {
                    for (int j = 1; j < termList.size(); j++) {
                        String s = (String) termList.get(j);
                        if (consequent.indexOf(s) < 0) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        logger.fine("Attempting to unify rule " + f2
                        		+ "\n with \n\n" + f);
                        TreeMap m = f.unify(f2);
                        if (m != null) {
                            logger.fine("Matched rule " + f2);
                            return f2;
                        }
                    }
                }
            }

        }
    	logger.exiting("KB", "findRuleInferenceMatch");

        return null;
    }

    /** *************************************************************
     *  Find the first rule or ground formula that satisfies this
     *  statement, which can be a simple clause or a more complex
     *  formula.
     */
    private Formula findInferenceMatch(Formula f) {
    	
    	logger.entering("KB", "findInferenceMatch", f);

        System.out.println("INFO in KB.findInferenceMatch(): " + f);
        if (f.isRule())         // exclude rules for now
            return null;
        Formula f2 = findGroundFormulaInferenceMatch(f);
        if (f2 == null) {
            Formula returnFormula = findRuleInferenceMatch(f);
            logger.exiting("KB", "findInferenceMatch", returnFormula);
            return returnFormula;
        }
        else {
        	logger.exiting("KB", "findInferenceMatch", f2);
        	return f2;
        }
        
    }

    /** *************************************************************
     *  Take the precondition from a rule and try to separate it
     *  into separate clauses that can be solved and placed into
     *  "head".
     *  @param head is the current set of preconditions which will
     *              keep getting expanded until its not possible to
     *              find more, or the proof size limit is reached.
     *              Setting this list is a side effect.
     *  @param assertions is the list of ground assertions that are
     *                    needed as background knowledge to support
     *                    the proof.  Setting this list is a side
     *                    effect.
     */
    private void findProofFromRule(Formula f, ArrayList<Formula> head,
                                   ArrayList<Formula> assertions,
                                   int numAxioms,
                                   ArrayList<Formula> axioms) {

        Formula pre = new Formula();
        pre.read(f.getArgument(1));
        axioms.add(f);
        extractPreconditions(pre,head,assertions);
        while (head.size() > 0) {
            ArrayList<Formula> newhead = new ArrayList<Formula>();
            newhead.addAll(head);
            for (int j = 0; j < head.size(); j++) {
                Formula f2 = (Formula) head.get(j);
                newhead.remove(f2);
                if (axioms.size() + head.size() < numAxioms) {
                    if (f2.isRule()) {
                        findProofFromRule(f2,newhead,assertions,numAxioms,axioms);
                        if (!axioms.contains(f2))
                            axioms.add(f2);
                    }
                    else {
                        Formula match = findInferenceMatch(f2);
                        logger.fine("match =" + match);
                        if (match == null) {
                            instantiateFormula(f2,assertions);
                        }
                        else {
                            logger.fine("Adding match to head");
                            if (!newhead.contains(match))
                                newhead.add(match);
                        }
                    }
                }
                else {
                    if (f2.isRule()) {
                        Formula ant = new Formula();
                        ant.read(f2.getArgument(1));
                        instantiateFormula(ant,assertions);
                    }
                    else
                        instantiateFormula(f2,assertions);
                }
            }
            head = new ArrayList<Formula>();
            head.addAll(newhead);

            logger.info("findProofFromRule(): head =");
            for (int j = 0; j < head.size(); j++) {
                Formula f2 = (Formula) head.get(j);
                logger.info(f2.theFormula);
            }
        }
    }

    /** *************************************************************
     *  Generate random proof trees, then write out a set of
     *  assertions, a query, and the proof.  The assertions and
     *  query then become a test for a theorem prover.
     *  Randomly find an axiom, then find ground statements or rules
     *  with conclusions that satisfy the axiom.  Repeat until all
     *  ground preconditions are found, or the number of axioms
     *  found is greater than numAxioms, at which point generate
     *  ground statements to satisfy any remaining preconditions.
     */
    private void generateRandomProof() {

    	logger.entering("KB", "generateRandomProof");

    	int numProofs = 1;
        ArrayList<Formula> formulaList = new ArrayList<Formula>();
        ArrayList<Formula> rules = new ArrayList<Formula>();

        formulaList.addAll(formulaMap.values());
        for (int i = 0; i < formulaList.size(); i++) {
            Formula f = (Formula) formulaList.get(i);
            if (f.isRule() && !rules.contains(f))
                rules.add(f);
        }
        formulaList.addAll(formulaMap.values());

        for (int i = 0; i < numProofs; i++) {
            int numAxioms = 5;
            Random r = new Random(4);
            ArrayList<Formula> axioms = new ArrayList<Formula>();
            ArrayList<Formula> head = null;
            ArrayList<Formula> assertions = null;
            Formula query = new Formula();
            while (axioms.size() < 2) {
                axioms = new ArrayList<Formula>();
                head = new ArrayList<Formula>();
                assertions = new ArrayList<Formula>();
                Formula f = (Formula) rules.get(r.nextInt(rules.size()));
                while (f.isHigherOrder())
                    f = (Formula) rules.get(r.nextInt(rules.size()));

                logger.fine("start rule =" + f);
                query = new Formula();
                query.read(f.getArgument(2));

                findProofFromRule(f,head,assertions,numAxioms,axioms);

                logger.fine("query =" + query);
                logger.fine("axioms = ");
                for (int j = 0; j < axioms.size(); j++) {
                    Formula f2 = (Formula) axioms.get(j);
                    logger.fine(f2.theFormula);
                }
                logger.fine("assertions = ");
                for (int j = 0; j < assertions.size(); j++) {
                    Formula f2 = (Formula) assertions.get(j);
                    logger.fine(f2.theFormula);
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
                        logger.fine(term + "->" + term2 + "->" + term3 +
                        			"\nf = "+ f + "\nf2 = " + f2);
                        if (Formula.atom(term3)) {
                            ArrayList<Formula> al3 = askWithRestriction(0,"instance",1,term3);
                            for (int k = 0; k < al3.size(); k++) {
                                Formula f3 = (Formula) al3.get(k);
                                String term4 = f3.getArgument(2);
                                logger.fine(term + "->" + term2 + "->" + term3 + "->" + term4 +
                            			"\nf = "+ f + "\nf2 = " + f2 + "\nf3 = " + f3);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public void writeDisplayText(String displayFormatPredicate, String displayTermPredicate, String language, String fname) throws IOException {   	
    	PrintWriter pr = null;
    	try {
    		pr = new PrintWriter(new FileWriter(fname, false));
    		
    		//get all formulas that have the display predicate as the predicate   		
    		ArrayList<Formula> formats = this.askWithRestriction(0, displayFormatPredicate, 1, language);
    		ArrayList<Formula> terms = this.askWithRestriction(0, displayTermPredicate, 1, language);
    		
    		HashMap termMap = new HashMap();
    		
    		for(int i=0; i < terms.size(); i++) {
    			Formula term = terms.get(i);
    			
    			String key = term.getArgument(2);
    			String value = term.getArgument(3);
    			
    			if (key != "" && value != "") {
    				termMap.put(key, value);
    			}
    		}
    		
    		for(int i=0; i < formats.size(); i++) {
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
	    				for (int k=1; k < arguments.size(); k++) {
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
    		logger.info("Write display text to file done!");
    		
    	}
    	catch (java.io.IOException e) {
    		logger.warning(e.getStackTrace().toString());
    		e.printStackTrace();
    	}
    	catch (Exception e) {
    		logger.warning(e.getStackTrace().toString());
    		e.printStackTrace();
    	}
        finally {
            if (pr != null) {
                pr.close();
            }
        }
    }
    
    private ArrayList<com.articulate.sigma.Formula> askWithRestrictions(int i,
			String displayFormatPredicate, int j) {
		// TODO Auto-generated method stub
		return null;
	}

	/** *************************************************************
     */
    public static void main(String[] args) {

        // testTPTP(args);
        /*
          try {
          KBmanager.getMgr().initializeOnce();
          } catch (IOException ioe ) {
          System.out.println(ioe.getMessage());
          }
          KB kb = KBmanager.getMgr().getKB("SUMO");
          kb.termsWithNoPictureLinks();
        */

        try {
        	System.out.println("Am running it!!!");
            KBmanager.getMgr().initializeOnce();
            WordNet.initOnce();
            KBmanager.getMgr().addKB("SUMO");
            KB kb = KBmanager.getMgr().getKB("SUMO");
            kb.addConstituent("/home/knomorosa/SourceForge/KBs/ReardenLabels.kif");
            
            kb.writeDisplayText("reardenFormat", "reardenDisplayFormat", "EnglishLanguage", "/home/knomorosa/Desktop/FileForRama.csv");
            
        } catch (Exception e) {
            System.out.println(e.getMessage());
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

        //System.out.println("-------------- Terms ---------------");
        //System.out.println(kb.allTerms());
        /* for (int i = 1; i < 5; i++) {
           System.out.println("-------------- Arity " + i + " Functions ---------------");
           System.out.println(kb.allFunctionsOfArity(i));
           System.out.println("-------------- Arity " + i + " Relations ---------------");
           System.out.println(kb.allRelationsOfArity(i+1));
           }
        */
    }
}
