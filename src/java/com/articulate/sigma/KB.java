/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the termsof the GNU
license.  This software is released under the GNU Public License
<http://www.gnu.org/copyleft/gpl.html>.  Users of this code also consent,
by use of this code, to credit Articulate Software and Teknowledge in any
writings, briefings, publications, presentations, or other representations
of any software which incorporates, builds on, or uses this code.  Please
cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

/*************************************************************************************************/
package com.articulate.sigma;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.text.ParseException;

/** *****************************************************************
 *  Contains methods for reading, writing knowledge bases and their
 *  configurations.  Also contains the inference engine process for 
 *  the knowledge base.
 */
public class KB {

    private static boolean DEBUG = false;

    /** The inference engine process for this KB. */
    public Vampire inferenceEngine;                 

    /** The name of the knowledge base. */
    public String name;                       

    /** An ArrayList of Strings which are the full path file names of
     * the files which comprise the KB. 
     */
    public ArrayList constituents = new ArrayList();

    /** The natural language in which axiom paraphrases should be presented. */
    public String language = "EnglishLanguage";    

    /** The location of preprocessed KIF files, suitable for loading into Vampire. */
    public String kbDir = null;

    /** A HashMap of HashSets, which contain all the parent classes of a given class. */
    public HashMap parents = new HashMap();

    /** A HashMap of HashSets, which contain all the child classes of a given class. */
    public HashMap children = new HashMap();

    /** A HashMap of HashSets, which contain all the disjoint classes of a given class. */
    public HashMap disjoint = new HashMap();

    /** 
     * A threshold limiting the number of values that will be added to
     * a single relation cache table.
     */
    private static final int MAX_CACHE_SIZE = 1000000;

    /** A List of the names of cached transitive relations. */
    private List cachedTransitiveRelationNames = Arrays.asList("subclass", 
                                                               "subset",
                                                               "subrelation", 
                                                               "subAttribute", 
                                                               "subOrganization", 
                                                               "subCollection", 
                                                               "subProcess",
                                                               "geographicSubregion",
                                                               "geopoliticalSubdivision");

    /** A List of the names of cached reflexive relations. */
    private List cachedReflexiveRelationNames = Arrays.asList("subclass", 
                                                              "subset",
                                                              "subrelation", 
                                                              "subAttribute", 
                                                              "subOrganization", 
                                                              "subCollection", 
                                                              "subProcess");

    /** A List of the names of cached relations. */
    private List cachedRelationNames = Arrays.asList("instance", "disjoint");

    /** An ArrayList of RelationCache objects. */
    private ArrayList relationCaches = new ArrayList();

    /** *************************************************************
     * If true, assertions of the form (predicate x x) will be
     * included in the relation cache tables.
     */
    private boolean cacheReflexiveAssertions = false;

    /** *************************************************************
     * Sets the private instance variable cacheReflexiveAssertions to
     * val.
     *
     * @param val true or false
     *
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
     * Returns a list of the names of cached relations.
     * 
     * @return An ArrayList of relation names (Strings).
     */
    private ArrayList getCachedRelationNames() {
        ArrayList relationNames = new ArrayList(cachedRelationNames);
        String name = null;
        for (Iterator it = getCachedTransitiveRelationNames().iterator(); it.hasNext();) {
            name = (String) it.next();
            if (!relationNames.contains(name)) relationNames.add(name);
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
     * Returns a list of the names of cached reflexive relations.
     * 
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
     * Returns an ArrayList of RelationCache objects.
     * 
     * @return An ArrayList of RelationCache objects.
     */
    protected ArrayList getRelationCaches() {
        return this.relationCaches;
    }

    /** The instance of the CELT process. */
    public CELT celt = null;

    /** A Set of Strings, which are all the terms in the KB. */
    public TreeSet terms = new TreeSet(); 

    /** The String constant that is the suffix for files of user assertions. */
    public static final String _userAssertionsString = "_UserAssertions.kif";

    /** The String constant that is the suffix for files of cached assertions. */
    public static final String _cacheFileSuffix      = "_Cache.kif";

    /** 
     * A Map of all the Formula objects in the KB.  Each key is a
     * String representation of a Formula.  Each value is the Formula
     * object corresponding to the key.
     */
    public HashMap formulaMap = new HashMap(); 

    /** 
     * A HashMap of ArrayLists of Formulae, containing all the
     * formulae in the KB.  Keys are the formula itself, a formula ID, and term
     * indexes created in KIF.createKey().
     */
    private HashMap formulas = new HashMap();                                                   

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

    /** whether the contents of the KB have been modified without updating the caches */
    public boolean modifiedContents = false;

    /** Returns the platform-specific line separator String. */
    protected String getLineSeparator() {
        return System.getProperty("line.separator");
    }

    /** ***************************************************************
     * This Map is used to cache sortal predicate argument type data
     * whenever Formula.findType() or Formula.getTypeList() are going
     * to called hundreds or thousands of times inside
     * KB.preProcess().  The Map is cleared after each such use in
     * KB.preProcess().
     */
    protected HashMap sortalTypeCache = null;

    /** ***************************************************************
     * Clears the Map KB.sortalTypeCache if the variable is not null.
     *
     * @return void
     */
    protected void clearSortalTypeCache() {
        try {
            if (sortalTypeCache != null) {
                System.out.println("INFO in KB.clearSortalTypeCache()");
                System.out.println("  Clearing " + sortalTypeCache.size() + " entries");
                Object obj = null;
                for (Iterator it = sortalTypeCache.values().iterator(); it.hasNext();) {
                    obj = it.next();
                    if (obj instanceof Collection) {
                        ((Collection) obj).clear();
                    }
                }
                sortalTypeCache.clear();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** ***************************************************************
     * Calls KB.clearSortalTypeCache(), or creates a new HashMap for
     * sortalTypeCache if one does not already exist.
     *
     * @return void
     */
    protected void resetSortalTypeCache() {
        if (sortalTypeCache == null) {
            sortalTypeCache = new HashMap();
        }
        else {
            clearSortalTypeCache();
        }
        System.out.println("INFO in KB.resetSortalTypeCache()");
        System.out.println("  sortalTypeCache == " + sortalTypeCache);
        return;
    }

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
                initRelationCaches();
                String loadCelt = mgr.getPref("loadCELT");
                if ((loadCelt != null) && loadCelt.equalsIgnoreCase("yes")) {
                    celt = new CELT();
                }
            }
        }
        catch (IOException ioe) {
            System.out.println("Error in KB(): " + ioe.getMessage());
            celt = null;
        }
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
     * RelationCache objects.
     * 
     * @return void
     */
    protected void initRelationCaches() {
        // System.out.println("INFO in initRelationCaches()");
        Iterator it = null;
        if (relationCaches.isEmpty()) {
            it = getCachedRelationNames().iterator();
            String relname = null;
            while (it.hasNext()) {
                relname = (String) it.next();
                relationCaches.add(new RelationCache(relname, 1, 2));

                // Since disjoint is symmetric, we put all
                // disjointness entries in one table.  All transitive
                // binary relations are cached in two RelationCaches,
                // one that looks "upward" from the keys, and another
                // that looks "downward" from the keys.
                if (!relname.equals("disjoint")) {
                    relationCaches.add(new RelationCache(relname, 2, 1));
                }
            }
        }
        else {
            RelationCache cache = null;
            it = relationCaches.iterator();
            while (it.hasNext()) {
                cache = (RelationCache) it.next();
                cache.clear();
            }
        }

        // We still set these legacy variables.  Eventually, they
        // should be removed.
        parents  = getRelationCache("subclass", 1, 2);
        children = getRelationCache("subclass", 2, 1);
        disjoint = getRelationCache("disjoint", 1, 2);

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
        if (StringUtil.isNonEmptyString(relName)) {
            Iterator it = getRelationCaches().iterator();
            RelationCache cache = null;
            while (it.hasNext()) {
                cache = (RelationCache) it.next();
                if (cache.getRelationName().equals(relName)
                    && (cache.getKeyArgument() == keyArg)
                    && (cache.getValueArgument() == valueArg)) {
                    return cache;
                }
            }
        }
        return null;
    }
    
    /** *************************************************************
     * Writes the cache .kif file, and then calls addConstituent() so
     * that the file can be processed and loaded by the inference
     * engine.
     *
     * @return a String indicating any errors, or the empty string if
     * there were no errors.
     */
    public String cache() {

        System.out.println("ENTER KB.cache()");
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
                    System.out.println("INFO in KB.cache()");
                    System.out.println("  User cache file == " + f.getCanonicalPath());
                    if (f.exists()) {
                        System.out.println("  Deleting " + f.getCanonicalPath());
                        f.delete();
                        if (f.exists()) {
                            System.out.println("  Could not delete " + f.getCanonicalPath());
                        }
                    }
                    String filename = f.getCanonicalPath();
                    fr = new FileWriter(f, true);
                    System.out.println("  Appending statements to " + f.getCanonicalPath());
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
                                        tuple = ("(" + relation + " " + arg1 + " " + arg2 + ")");
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
                    System.out.println("INFO in KB.cache()");
                    System.out.println("  Adding " + filename);
                    result = addConstituent(filename, false, false);
                    KBmanager.getMgr().writeConfiguration();
                }
            }
        }
        catch (Exception ex) {
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
        System.out.println("EXIT KB.cache()");
        return result;
    }

    /** *************************************************************
     * Adds one value to the cache, indexed under keyTerm.
     *
     * @param cache The RelationCache object to be updated.
     *
     * @param keyTerm The String that is the key for this entry.
     *
     * @param valueTerm The String that is the value for this entry.
     * 
     * @return The int value 1 if a new entry is added, else 0.
     */
    private int addRelationCacheEntry(RelationCache cache, String keyTerm, String valueTerm) {
        int count = 0;
        if ((cache != null) && (keyTerm != null) && (valueTerm != null)) {
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
     * @param relation A String, the name of a relation.
     *
     * @param term A String (key) that indexes a HashSet.
     *
     * @param keyArg An int value that, with relation and valueArg,
     * identifies a RelationCache.
     *
     * @param valueArg An int value that, with relation and keyArg,
     * identifies a RelationCache.
     * 
     * @return A HashSet, or null if no HashSet corresponds to term.
     */
    public HashSet getCachedRelationValues(String relation, 
                                           String term, 
                                           int keyArg, 
                                           int valueArg) {
        RelationCache cache = getRelationCache(relation, keyArg, valueArg);
        if (cache != null) {
            return (HashSet) cache.get(term);
        }
        return null;
    }

    /** *************************************************************
     * This method computes the transitive closure for the relation
     * identified by relationName.  The results are stored in the
     * RelationCache object for the relation and "direction" (looking
     * from the arg1 keys toward arg2 parents, or looking from the
     * arg2 keys toward arg1 children).
     *
     * @param relationName The name of a relation.
     *
     * @return void
     */
    private void computeTransitiveCacheClosure(String relationName) {

        System.out.println("ENTER KB.computeTransitiveCacheClosure(" + relationName + ")");

        try {
            long t1 = System.currentTimeMillis();
            int count = 0;
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
                            if ((keyTerm == null) || keyTerm.equals("")) {
                                System.out.println("Error in KB.computeTransitiveCacheClosure(" 
                                                   + relationName + ")");
                                System.out.println("  keyTerm == " 
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
                                // Here we try to make sure that every Relation
                                // has at least some entry in the "instance"
                                // caches, since this information is sometimes
                                // considered redundant and so could be left out
                                // of .kif files.
                                valTerm = "Relation";
                                if (keyTerm.endsWith("Fn")) {
                                    valTerm = "Function";
                                }
                                else if (Character.isLowerCase(keyTerm.charAt(0)) 
                                         && (keyTerm.indexOf("(") == -1)) {
                                    valTerm = "Predicate";
                                }
                                addRelationCacheEntry(inst1, keyTerm, valTerm);
                                addRelationCacheEntry(inst2, valTerm, keyTerm);
                            }
                        }
                        if (changed) {
                            c1.setIsClosureComputed(true);
                            c2.setIsClosureComputed(true);
                        }
                    }
                }
            }

            System.out.println("  " + count + " " + relationName + " entries computed in " 
                               + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");

            System.out.println("EXIT KB.computeTransitiveCacheClosure(" + relationName + ")");

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
        return;
    }

    /** *************************************************************
     * This method computes the closure for the cache of the instance
     * relation, in both directions.
     *
     * @return void
     */
    private void computeInstanceCacheClosure() {

        System.out.println("INFO in KB.computeInstanceCacheClosure()");

        try {
            long t1 = System.currentTimeMillis();
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

            int count = 0;
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
            System.out.println("  " + count + " instance entries computed in " 
                               + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * This method computes the closure for the cache of the disjoint
     * relation.
     *
     * @return void
     */
    private void computeDisjointCacheClosure() {

        System.out.println("INFO in KB.computeDisjointCacheClosure()");

        try {
            long t1 = System.currentTimeMillis();
            RelationCache dc1 = getRelationCache("disjoint", 1, 2);
            RelationCache sc2 = getRelationCache("subclass", 2, 1);
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

            int count = -1;
            int passes = 0;
            boolean changed = true;

            // One pass is sufficient.
            // while (changed) {
            dc1KeySet = dc1.keySet();
            dc1KeyArr = dc1KeySet.toArray();
            changed = false;
            for (int i = 0 ; (i < dc1KeyArr.length) && (count < MAX_CACHE_SIZE) ; i++) {

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
            // }

            System.out.println("  " + count + " disjoint entries computed in " 
                               + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");
            // printDisjointness();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
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

        System.out.println("INFO in KB.cacheRelnsWithRelnArgs()");

        try {
            long t1 = System.currentTimeMillis();
            if (relnsWithRelnArgs == null) {
                relnsWithRelnArgs = new HashMap();
            }
            relnsWithRelnArgs.clear();
            Set relnClasses = getCachedRelationValues("subclass", "Relation", 2, 1);
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
                                System.out.println("Error in KB.cacheRelnsWithRelnArgs():");
                                System.out.println("  reln == " + reln 
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
            System.out.println("  "
                               + relnsWithRelnArgs.size()
                               + " relation argument entries computed in " 
                               + ((System.currentTimeMillis() - t1) / 1000.0)
                               + " seconds");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * Returns a boolean[] if the input relation has at least one
     * argument that must be filled by a relation name.
     * 
     */    
    protected boolean[] getRelnArgSignature(String relation) {
        if (relnsWithRelnArgs != null) {
            return (boolean[]) relnsWithRelnArgs.get(relation);
        }
        return null;
    }

    private HashMap relationValences = new HashMap();

    private void cacheRelationValences() {
        System.out.println("INFO in KB.cacheRelationValences()");
        try {
            long t1 = System.currentTimeMillis();
            Set relations = getCachedRelationValues ("instance", "Relation", 2, 1);
            if (relations != null) {
                RelationCache ic1 = getRelationCache("instance", 1, 2);
                RelationCache ic2 = getRelationCache("instance", 2, 1);
                String reln = null;
                String className = null;
                int valence = -1;
                Iterator it = relations.iterator();
                while (it.hasNext()) {
                    reln = (String) it.next();

                    // Here we evaluate getValence() to build the
                    // relationValences cache, and use its return
                    // value to fill in any info that might be missing
                    // from the "instance" cache.
                    valence = getValence(reln);
                    className = null;
                    if (valence >= 0) {
                        switch (valence) {
                        case 0 : 
                            className = "VariableArityRelation"; 
                            break;
                        case 1 : 
                            if (reln.endsWith("Fn")) {
                                className = "UnaryFunction";
                            }
                            else {
                                className = "UnaryRelation";
                            }
                            break;
                        case 2 : 
                            if (reln.endsWith("Fn")) {
                                className = "BinaryFunction";
                            }
                            else {
                                className = "BinaryRelation";
                            }
                            break;
                        case 3 : 
                            if (reln.endsWith("Fn")) {
                                className = "TernaryFunction";
                            }
                            else {
                                className = "TernaryRelation";
                            }
                            break;
                        case 4 : 
                            if (reln.endsWith("Fn")) {
                                className = "QuaternaryFunction";
                            }
                            else {
                                className = "QuaternaryRelation";
                            }
                            break;
                        case 5 : 
                            className = "QuintaryRelation";
                            break;
                        default : break;
                        }
                    }
                    if (className != null) {
                        addRelationCacheEntry(ic1, reln, className);
                        addRelationCacheEntry(ic2, className, reln);
                    }
                }
            }
            System.out.println("  "
                               + relationValences.size()
                               + " relation valence entries computed in " 
                               + ((System.currentTimeMillis() - t1) / 1000.0)
                               + " seconds");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    protected String getArgType(String reln, int argPos) {
        String className = null;
        try {
            List formulas = askWithRestriction(1, reln, 0, "domain");
            if (! ((formulas == null) || formulas.isEmpty())) {
                String argN = null;
                Formula f = null;
                Iterator it = formulas.iterator();
                while (it.hasNext()) {
                    f = (Formula) it.next();
                    argN = f.getArgument(2);
                    if (Integer.parseInt(argN) == argPos) {
                        className = f.getArgument(3);
                        break;
                    }
                }
            }
        }
        catch (Exception ex) {
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
            ex.printStackTrace();
        }
        return ans;
    }

    protected ArrayList listRelnsWithRelnArgs() {
        if (relnsWithRelnArgs != null) {
            return new ArrayList(relnsWithRelnArgs.keySet());
        }
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
                        if (input.indexOf(reln) >= 0) {
                            return true;
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /** *************************************************************
     * debugging utility
     */
    private void printParents() {

        System.out.println("INFO in printParents():  Printing parents.");
        System.out.println();
        Iterator it = parents.keySet().iterator();
        while (it.hasNext()) {
            String parent = (String) it.next();
            System.out.print(parent + " ");
            System.out.println((HashSet) parents.get(parent));
        }
        System.out.println();
    }

    /** *************************************************************
     * debugging utility
     */
    private void printChildren() {

        System.out.println("INFO in printChildren():  Printing children.");
        System.out.println();
        Iterator it = children.keySet().iterator();
        while (it.hasNext()) {
            String child = (String) it.next();
            System.out.print(child + " ");
            System.out.println((HashSet) children.get(child));
        }
        System.out.println();
    }
    
    /** *************************************************************
     * debugging utility
     */
    private void printDisjointness() {

        System.out.println("INFO in printDisjointness():  Printing disjoint.");
        System.out.println();
        Iterator it = disjoint.keySet().iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            System.out.print(term + " is disjoint with ");
            System.out.println((Set) disjoint.get(term));
        }
        System.out.println();
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
            Set classes = getCachedRelationValues("instance", i, 1, 2); 
            // was: getAllInstancesWithPredicateSubsumption(c);
            if (classes != null) {
                ans = classes.contains(c);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
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
        HashSet childs = (HashSet) children.get(parent);
        if (childs != null && childs.contains(child)) 
            return true;
        else {
            ArrayList al = instancesOf(child);
            Iterator it = al.iterator();
            while (it.hasNext()) {
                Formula form = (Formula) it.next();
                Formula f = new Formula();
                f.read(form.theFormula);
                f.read(f.cdr());
                f.read(f.cdr());                
                String superTerm = f.car();
                if (superTerm.equals(parent)) 
                    return true;
                if (childs != null && childs.contains(superTerm)) 
                    return true;
            }
        }
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
        boolean ans = false;
        try {
            if (StringUtil.isNonEmptyString(c1) && StringUtil.isNonEmptyString(c2)) {
                Set terms = getCachedRelationValues("subclass", c1, 1, 2); 
                // was: getAllSubClassesWithPredicateSubsumption(c2);
                if (terms != null) {
                    ans = terms.contains(c2);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Builds all of the relation caches for the current KB.
     */
    public void buildRelationCaches() {

        try {
            long t1 = System.currentTimeMillis();
            initRelationCaches();
            cacheGroundAssertions();

            Iterator it = getCachedTransitiveRelationNames().iterator();
            String relationName = null;
            while (it.hasNext()) {
                relationName = (String) it.next();
                computeTransitiveCacheClosure(relationName);
            }

            computeInstanceCacheClosure();
            computeDisjointCacheClosure();
            cacheRelnsWithRelnArgs();
            cacheRelationValences();

            System.out.println("Total elapsed time to build all relation caches: "
                               + ((System.currentTimeMillis() - t1) / 1000.0)
                               + " seconds");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * Populates all caches with ground assertions, from which
     * closures can be computed.
     */
    private void cacheGroundAssertions() {

        System.out.println("INFO in KB.cacheGroundAssertions(): ");

        // System.out.println("formulas == " + formulas.toString());

        try {
            long t1 = System.currentTimeMillis();
            String relation = null;
            String arg1 = null;
            String arg2 = null;
            List forms = null;
            Formula formula = null;
            RelationCache c1 = null;
            RelationCache c2 = null;

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
                    formsIt = forms.iterator();
                    while (formsIt.hasNext()) {
                        formula = (Formula) formsIt.next();
                        if ((formula.theFormula.indexOf("(",2) == -1) 
                            && !formula.sourceFile.endsWith(_cacheFileSuffix)) {

                            arg1 = formula.getArgument(1).intern();
                            arg2 = formula.getArgument(2).intern();
                            if (StringUtil.isNonEmptyString(arg1) && StringUtil.isNonEmptyString(arg2)) {
                                count += addRelationCacheEntry(c1, arg1, arg2);
                                count += addRelationCacheEntry(c2, arg2, arg1);

                                // Special cases.
                                if (getCacheReflexiveAssertions()
                                    && getCachedReflexiveRelationNames().contains(relation)) {
                                    count += addRelationCacheEntry(c1, arg1, arg1);
                                    count += addRelationCacheEntry(c1, arg2, arg2);
                                    count += addRelationCacheEntry(c2, arg1, arg1);
                                    count += addRelationCacheEntry(c2, arg2, arg2);
                                }
                                else if (relation.equals("disjoint")) {
                                    count += addRelationCacheEntry(c1, arg2, arg1);
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
                    if (partitions != null) {
                        forms.addAll(partitions);
                    }
                    if (decompositions != null) {
                        forms.addAll(decompositions);
                    }
                    c1 = getRelationCache(relation, 1, 2);
                    List arglist = null;
                    formsIt = forms.iterator();
                    while (formsIt.hasNext()) {
                        formula = (Formula) formsIt.next();
                        if ((formula.theFormula.indexOf("(",2) == -1) 
                            && !(formula.sourceFile.endsWith(_cacheFileSuffix))) {

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
                System.out.println("  " + count + " cache entries added for " + relation);
                total += count;
            }
            System.out.println("  " + total + " ground assertions cached in " 
                               + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
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
                && StringUtil.isNonEmptyString(term2)) {
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
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Returns an ArrayList of Formulas in which the two terms
     * provided appear in the indicated argument positions.  If there
     * are no Formula(s) matching the given terms and respective
     * argument positions, return an empty ArrayList.  Iterate
     * through the smallest list of results.
     *
     * @return ArrayList
     */
    public ArrayList askWithRestriction(int argnum1, String term1, int argnum2, String term2) {

        ArrayList result = new ArrayList();
        try {
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
            for (int i = 0; i < partial.size(); i++) {
                f = (Formula) partial.get(i);
                if (f.getArgument(arg).equals(term)) {
                    result.add(f);
                }
            }
        }
        catch (Exception ex) {
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

        ArrayList result = new ArrayList();
        ArrayList partiala = new ArrayList();           // will get the smallest list
        ArrayList partialb = new ArrayList();           // next smallest
        ArrayList partialc = new ArrayList();           // biggest
        ArrayList partial1 = ask("arg",argnum1,term1);
        ArrayList partial2 = ask("arg",argnum2,term2);
        ArrayList partial3 = ask("arg",argnum3,term3);
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
        return result;
    }

    /** *************************************************************
     * Returns an ArrayList containing the SUO-KIF terms that match
     * the request.
     *
     * @return An ArrayList of terms, or an empty ArrayList if no
     *         matches can be found.
     */
    public ArrayList getTermsViaAWTR(int argnum1, String term1, 
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

            List terms = getTermsViaAWTR(argnum1, term1,
                                         argnum2, term2,
                                         argnum3, term3,
                                         targetArgnum);
            if (!terms.isEmpty()) {
                ans = (String) terms.get(0);
            }
        }
        catch (Exception ex) {
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
                System.out.println(msg);
                throw new Exception(msg);
            }
            if (term.length() > 1 
                && term.charAt(0) == '"' 
                && term.charAt(term.length()-1) == '"') {
                msg = ("Error in KB.ask(): Strings are not indexed.  No results for " + term);
                System.out.println(msg);
                throw new Exception(msg);
            }
            List tmp = null;
            if (kind.equals("arg"))
                tmp = (List) formulas.get(kind + "-" + argnum + "-" + term);        
            else 
                tmp = (List) formulas.get(kind + "-" + term);
            if (tmp != null) {
                result.addAll(tmp);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Returns an ArrayList containing the Formulae retrieved,
     * possibly via multiple asks that recursively use relation and
     * all of its subrelations.  Note that the Formulas might be
     * formed with different predicates, but all of the predicates
     * will be subrelations of relation, and will be related to each
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
    public ArrayList<String> getTransitiveClosureViaPredicateSubsumption(String relation, 
                                                                         int idxArgnum, 
                                                                         String idxTerm,
                                                                         int targetArgnum,
                                                                         boolean useInverses) {
        ArrayList<String> ans = new ArrayList<String>();
        try {
            Set<String> reduced = new TreeSet<String>();
            Set<String> accumulator = 
                new TreeSet<String>(getTermsViaPredicateSubsumption(relation, 
                                                                    idxArgnum, 
                                                                    idxTerm,
                                                                    targetArgnum,
                                                                    useInverses));
            ArrayList<String> working = new ArrayList<String>();
            while (!accumulator.isEmpty()) {
                reduced.addAll(accumulator);
                working.clear();
                working.addAll(accumulator);
                accumulator.clear();
                for (String term : working) {
                    accumulator.addAll(getTermsViaPredicateSubsumption(relation, 
                                                                       idxArgnum, 
                                                                       term,
                                                                       targetArgnum,
                                                                       useInverses));
                }
            }
            ans.addAll(reduced);
        }
        catch (Exception ex) {
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

        ArrayList formulasPresent = new ArrayList();
        try {
            // Add all the terms from the new formula into the KB's current list
            terms.addAll(kif.terms);                                   

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
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return formulasPresent;
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
            System.out.println("Error writing file " + fname);
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
            System.out.println("Error writing file " + fname);
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
    public String tell(String input) {

        System.out.println("ENTER KB.tell(" + input + ")");

        String result = "The formula could not be added";
        try {
            KBmanager mgr = KBmanager.getMgr();
            // 1. Parse the input string.
            KIF kif = new KIF();
            String msg = kif.parseStatement(input);
            if (msg != null) {
                result = "Error parsing \"" + input + "\": " + msg;
            }
            else if (kif.formulaSet.isEmpty()) {
                result = "The input could not be parsed";
            }
            else {
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
                        // 2. at least one Formula object and stored in
                        // 2. this.formulaMap.
                        fstr = (String) it.next();
                        parsedF = (Formula) this.formulaMap.get(fstr.intern());
                        if (parsedF == null) {
                            go = false;
                        }
                        else {
                            parsedFormulas.add(parsedF);
                        }
                    }
                    System.out.println("INFO in KB.tell()");
                    System.out.println("  parsedFormulas == " + parsedFormulas);
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
                            if (processedFormulas.isEmpty()) {
                                allAdded = false;
                            }
                            else {
                                // 6. If TPTP != no, translate to TPTP.
                                if (!mgr.getPref("TPTP").equalsIgnoreCase("no")) {
                                    parsedF.tptpParse(false, this, processedFormulas);

                                    System.out.println("INFO in KB.tell()");
                                    System.out.println("  theTptpFormulas == " 
                                                       + parsedF.getTheTptpFormulas());
                                }
                                // 7. If there is an inference engine,
                                // 7. assert the formula to the
                                // 7. inference engine's database.
                                if (inferenceEngine != null) {
                                    String ieResult = null;
                                    Formula processedF = null;
                                    Iterator it2 = processedFormulas.iterator();
                                    while (it2.hasNext()) {
                                        processedF = (Formula) it2.next();
                                        System.out.println("ENTER Vampire.assertFormula(" 
                                                           + processedF.theFormula 
                                                           + ")");
                                        ieResult = inferenceEngine.assertFormula(processedF.theFormula);
                                        System.out.println("EXIT Vampire.assertFormula(" 
                                                           + processedF.theFormula + ")");
                                        System.out.println("  " + ieResult);
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
            ex.printStackTrace();
            // System.out.println("Error in KB.tell(): " + ioe.getMessage());
        }
        /* collectParents();
           if (mgr.getPref("cache") != null &&
           mgr.getPref("cache").equalsIgnoreCase("yes"))
           cache();        */   // caching is currently not efficient enough to invoke it after every assertion
        System.out.println("EXIT KB.tell(" + input + ")");
        System.out.println("  ==> " + result);
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

        System.out.println("INFO in " + this.name + ".ask(" 
                           + suoKifFormula + ", " 
                           + timeout + ", " 
                           + maxAnswers + ")");

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

                System.out.println("  processedStmts == " + processedStmts);

                if (!processedStmts.isEmpty() && (this.inferenceEngine instanceof Vampire)) {
                    result = 
                        this.inferenceEngine
                        .submitQuery(((Formula)processedStmts.get(0)).theFormula,
                                     timeout,
                                     maxAnswers);
                }
            }
        }
        catch (Exception ex) {
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

        return terms.contains(term.intern());
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

        return terms.size();
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

        TreeSet newFormulaSet = new TreeSet();
        newFormulaSet.addAll(formulaMap.keySet());
        return newFormulaSet;
    }
    
    /** ***************************************************************
     *  An accessor providing a Formulas.
     */
    public Formula getFormulaByKey(String key) {

        ArrayList al = (ArrayList) formulas.get(key);
        if (al != null && al.size() > 0) 
            return (Formula) al.get(0);
        else
            return null;
    }
    
    /** ***************************************************************
     */
    public void rehashFormula(Formula f, String formID) {

        ArrayList al = (ArrayList) formulas.get(formID);
        if (al != null && al.size() > 0) {
            if (al.size() == 1) {
                formulas.remove(formID);
                al = new ArrayList();
                al.add(f);
                if (!formulas.keySet().contains(f.createID())) 
                    formulas.put(f.createID(),al);
            }
            else
                System.out.println("INFO in KB.rehashFormula(): Formula hash collision for: " + formID + " and formula " + f.theFormula);
        }
        else
            System.out.println("INFO in KB.rehashFormula(): No formula for hash: " + formID + " and formula " + f.theFormula);
    }
    
    /** ***************************************************************
     *  Count the number of rules in the knowledge base in order to
     *  present statistics to the user. Note that the number of rules
     *  is a subset of the number of formulas.
     *
     *  @return The int(eger) number of rules in the knowledge base.
     */
    public int getCountRules() {

        List symbols = Arrays.asList("=>", "<=>");
        int count = 0;
        Formula f = null;
        String arg0 = null;
        for (Iterator it = formulaMap.values().iterator(); it.hasNext();) {
            f = (Formula) it.next();
            arg0 = f.car();
            if (symbols.contains(arg0)) {
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
     *  Return all the "headwords" of terms, which is their XML name
     *  without a namespace.  This is overly specific to a certain
     *  kind of XML manipulation and needs redesign.
     */
    private TreeSet getHeadwords() {

        TreeSet result = new TreeSet();
        ArrayList headwordForms = ask("arg",0,"hasHeadword");
        if (headwordForms != null & headwordForms.size() > 0) {
            for (int i = 0; i < headwordForms.size(); i++) {
                Formula f = (Formula) headwordForms.get(i);
                result.add(StringUtil.removeEnclosingQuotes(f.getArgument(2)));
            }
            return result;
        }
        else
            return null;
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
        //TreeSet headwords = getHeadwords();
        //if (headwords != null & headwords.size() > 0) 
        //    t = headwords.toArray();
        //else
        t = terms.toArray();
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
        System.out.println(t.length);
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

        if (!terms.contains(term)) {
            ArrayList al = getNearestKTerms(term,0);
            term = (String) al.get(0);
        }
        if (terms.size() < 1) 
            return "";
        ArrayList tal = new ArrayList(terms);
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

        if (!terms.contains(term)) {
            ArrayList al = getNearestKTerms(term,0);
            term = (String) al.get(0);
        }
        if (terms.size() < 1) 
            return "";
        ArrayList tal = new ArrayList(terms);
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
     * string values.  @see formatMap is the same but for relation
     * format strings.
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
                    System.out.println("WARNING in KB.loadFormatMaps(): "
                                       + "No relation format file loaded for language " 
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
                        if (format.indexOf("$") < 0) 
                            format = format.replaceAll("\\x26\\x25", "\\&\\%"+key+"\\$");                   
                        langFormatMap.put(key, format);
                    }
                    // formatMap.put(lang,newFormatMap);
                    /*
                      System.out.println("INFO in KB.loadFormatMaps(" + this.name + ", " + lang + "): "
                      + ((System.currentTimeMillis() - t1) / 1000.0)
                      + " seconds to build KB.formatMap");
                    */
                }

                t1 = System.currentTimeMillis();
                col = askWithRestriction(0,"termFormat",1,lang);
                if ((col == null) || col.isEmpty()) {
                    System.out.println("WARNING in KB.loadFormatMaps(): "
                                       + "No term format file loaded for language: " 
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
                        //if (format.indexOf("$") < 0)
                        //    format = format.replaceAll("\\x26\\x25", "\\&\\%"+key+"\\$");
                        langTermFormatMap.put(key,format);
                    }
                    // termFormatMap.put(lang,newTermFormatMap);
                    /*
                      System.out.println("INFO in KB.loadFormatMaps(" + this.name + ", " + lang + "): "
                      + ((System.currentTimeMillis() - t1) / 1000.0)
                      + " seconds to build KB.termFormatMap");
                    */
                }
                loadFormatMapsAttempted.add(lang);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        language = lang;
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
        //System.out.println("ENTER getFormatMap(" + this.name + ", " + lang + ")");
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
        // System.out.println("EXIT getFormatMap(" + this.name + ", " + lang + ")");
        return (HashMap) formatMap.get(lang);
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
                    System.out.println("Error in KB.deleteUserAssertions: Error writing configuration: " + ioe.getMessage());
                }
            }
        }
        return;
    }

    /** *************************************************************
     * Add a new KB constituent by reading in the file, and then
     * merging the formulas with the existing set of formulas.  All
     * assertion caches are rebuilt, the current Vampire process is
     * destroyed, and a new one is created.
     *
     * @param filename - the full path of the file being added.
     */
    public String addConstituent(String filename) throws IOException {
        return addConstituent(filename, true,  true);
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
    public String addConstituent(String filename, boolean buildCachesP, boolean loadVampireP) {

        System.out.println("ENTER KB.addConstituent(" 
                           + filename + ", " 
                           + buildCachesP + ", " 
                           + loadVampireP + ")");
        long t1 = System.currentTimeMillis();
        StringBuffer result = new StringBuffer();

        try {
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

            if (constituents.contains(canonicalPath)) return ("Error: " 
                                                              + canonicalPath 
                                                              + " already loaded.");
            System.out.println("INFO in KB.addConstituent(" 
                               + filename + ", " 
                               + buildCachesP + ", " 
                               + loadVampireP + ")");
            System.out.println("  Adding " + canonicalPath);
            try { 
                file.readFile(canonicalPath);
                errors.addAll(file.warningSet);
            }
            catch (Exception ex1) {
                result.append(ex1.getMessage());
                if (ex1 instanceof ParseException)
                    result.append(" at line " 
                                  + ((ParseException)ex1).getErrorOffset());
                result.append(" in file " + canonicalPath);
                return result.toString();
            }

            System.out.println("INFO in KB.addConstituent(" 
                               + filename + ", " 
                               + buildCachesP + ", " 
                               + loadVampireP + ")");
            System.out.println("  Parsed file " 
                               + canonicalPath + " containing " 
                               + file.formulas.keySet().size()
                               + " KIF expressions");
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
                    internedFormula = f.theFormula.intern();
                    if (!list.contains(f)) {
                        list.add(f);        
                        formulaMap.put(internedFormula, f);
                    }
                    else {
                        result.append("Warning: Duplicate axiom in ");
                        result.append(f.sourceFile + " at line " + f.startLine + "<BR>");
                        result.append(f.theFormula + "<P>");
                        Formula existingFormula = (Formula) formulaMap.get(internedFormula);
                        result.append("Warning: Existing formula appears in ");
                        result.append(existingFormula.sourceFile + " at line " 
                                      + existingFormula.startLine + "<BR>");
                        result.append("<P>");
                    }
                }
            }
            System.out.println("x");

            this.terms.addAll(file.terms);

            if (!constituents.contains(canonicalPath)) {
                constituents.add(canonicalPath);
            }

            System.out.println("INFO in KB.addConstituent(" 
                               + filename + ", " 
                               + buildCachesP + ", " 
                               + loadVampireP + ")");
            System.out.println("  File " 
                               + canonicalPath + " loaded in "
                               + ((System.currentTimeMillis() - t1) / 1000.0) 
                               + " seconds");

            // Clear the formatMap and termFormatMap for this KB.
            loadFormatMapsAttempted.clear();
            if (formatMap != null) { formatMap.clear(); }
            if (termFormatMap != null) { termFormatMap.clear(); }

            if (buildCachesP && !canonicalPath.endsWith(_cacheFileSuffix)) 
                buildRelationCaches();            

            if (loadVampireP) 
                loadVampire();            
        }
        catch (Exception ex) {
            result.append(ex.getMessage());
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        System.out.println("EXIT KB.addConstituent(" 
                           + filename + ", " 
                           + buildCachesP + ", " 
                           + loadVampireP + ")");
        return result.toString();
    }

    /** ***************************************************************
     * Reload all the KB constituents.
     */
    public String reload() {

        System.out.println("ENTER KB.reload()");
        StringBuffer result = new StringBuffer();
        try {
            ArrayList newConstituents = new ArrayList();
            Iterator ci = constituents.iterator();
            String cName = null;
            while (ci.hasNext()) {
                cName = (String) ci.next();

                // Don't reuse the same cached data.  Instead, recompute
                // it.
                if (!(cName.endsWith(_cacheFileSuffix))) 
                    newConstituents.add(cName);                
            }
            constituents.clear();
            formulas.clear();
            formulaMap.clear();
            terms.clear();
            if (formatMap != null) 
                formatMap.clear();            
            if (termFormatMap != null) 
                termFormatMap.clear();            

            ci = newConstituents.iterator();
            while (ci.hasNext()) {
                cName = (String) ci.next();
                System.out.println("INFO in KB.reload()");
                System.out.println("  constituent == " + cName);
                result.append(addConstituent(cName, false, false));
            }           

            // Rebuild the in-memory relation caches.
            buildRelationCaches();

            // If cache == yes, write the cache file.
            if (KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes"))
                result.append(this.cache());            

            // At this point, we have reloaded all constituents, have
            // rebuilt the relation caches, and, if cache == yes, have
            // written out the _Cache.kif file.  Now we reload the
            // inferfence engine.
            loadVampire();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT KB.reload()");
        return result.toString();
    }

    /** ***************************************************************
     * Write a KIF file.
     * @param fname - the name of the file to write, including full path.
     */
    public void writeFile(String fname) throws IOException {

        FileWriter fr = null;
        PrintWriter pr = null;
        Iterator it;
        HashSet formulaSet = new  HashSet();
        ArrayList formulaArray;
        String key;
        ArrayList list;
        Formula f;
        String s;

        try {
            fr = new FileWriter(fname);
            pr = new PrintWriter(fr);

            it = formulas.keySet().iterator();
            while (it.hasNext()) {
                key = (String) it.next();
                list = (ArrayList) formulas.get(key);
                for (int i = 0; i < list.size(); i++) {
                    f = (Formula) list.get(i);
                    s = f.toString();
                    pr.println(s);
                    pr.println();
                }
            }
        }
        catch (java.io.IOException e) {
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
            if (al != null) {
                return (Pattern) al.get(0);
            }
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
        if (accumulator != null) {
            ans = accumulator;
        }
        if (REGEX_PATTERNS == null) {
            KB.compilePatterns();
        }
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
                    if (StringUtil.isNonEmptyString(term)
                        && !isVariable(term)) {
                        constant = term;
                        cidx = i;
                        break;
                    }
                }
                if (constant != null) {
                    ans = askWithRestriction(cidx, constant, 0, pred);
                }
                else {
                    ans = ask("arg", 0, pred);
                }
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
    private Set getAllSuperClasses(Set classNames) {
        Set ans = new HashSet();
        try {
            if ((classNames instanceof Set) && !(classNames.isEmpty())) {
                List accumulator = new ArrayList();
                List working = new ArrayList();
                String arg2 = null;
                working.addAll(classNames);
                while (!(working.isEmpty())) {
                    for (int i = 0 ; i < working.size() ; i++) {            
                        List nextLits = askWithRestriction(1, 
                                                           (String) working.get(i), 
                                                           0, 
                                                           "subclass");
            
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
            }   
        }
        catch (Exception ex) {
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
            }   
        }
        catch (Exception ex) {
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
     * class and predicate (subrelation) subsumption.
     *
     * @param className The name of a Class.
     *
     * @return A Set of terms (string constants), which could be
     * empty.
     */
    public Set getAllInstancesWithPredicateSubsumption(String className) {
        Set ans = new TreeSet();
        try {
            if (StringUtil.isNonEmptyString(className)) {
                List working = 
                    new ArrayList(getAllSubClassesWithPredicateSubsumption(className));
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
            }   
        }
        catch (Exception ex) {
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
            }   
        }
        catch (Exception ex) {
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
                        List nextLits = askWithRestriction(2, 
                                                           (String) working.get(i), 
                                                           0, 
                                                           "subclass");
            
                        if (nextLits != null) {
                            for (int j = 0 ; j < nextLits.size() ; j++) {
                                Formula f = (Formula) nextLits.get(j);
                                arg1 = f.getArgument(1);
                                if (! working.contains(arg1)) {
                                    accumulator.add(arg1);
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
            ex.printStackTrace();
        }
        return ans;
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
    protected TreeSet getAllInstances(Set classNames) {
        // System.out.println("ENTER KB.getAllInstances(" + classNames + ")");
        TreeSet ans = new TreeSet();
        try {
            if ((classNames instanceof Set) && !(classNames.isEmpty())) {
                Set partial = null;
                String name = null;
                Iterator it = classNames.iterator();
                while (it.hasNext()) {
                    name = (String) it.next();
                    partial = getCachedRelationValues("instance", name, 2, 1);
                    if (partial != null) {
                        ans.addAll(partial);
                    }
                }
            }
        }
        catch (Exception ex) {
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
    public TreeSet getAllInstances(String className) {
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
                if (relnSet == null) {
                    relnSet = new HashSet();
                }
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

                                // Sigh.  It's never simple.  The
                                // kluge below is to deal with the
                                // fact that a function, by
                                // definition, has a valence one less
                                // than the corresponding predicate.
                                // An instance of TernaryRelation that
                                // is also an instance of Function has
                                // a valence of 2, not 3.
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
        ArrayList ans = new ArrayList();
        Set preds = getCachedRelationValues("instance", "Predicate", 2, 1);
        if (preds != null) {
            ans.addAll(preds);
        }
        // System.out.println("Found " + ans.size() + " predicates");
        return ans;
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
        return ((obj instanceof String) && Formula.empty((String) obj));
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
                    System.out.println(label + ": " + it.next());
                }
                else {
                    System.out.println(it.next());
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
     * that term in the browser.  Handle (and ignore) suffixes on the term.
     * For example "&%Processes" would get properly linked to the term "Process",
     * if present in the knowledge base.
     */
    public String formatDocumentation(String href, String documentation, String language) {
        String newFormula = documentation;
        try {
            if (StringUtil.isNonEmptyString(newFormula)) {
                String suffix = "";
                if (DB.emptyString(href)) 
                    suffix = ".html";
                else if (!href.endsWith("&term=")) {
                    href = href + "&term=";
                }
                int i;
                int j;
                String term = "";
                boolean namespace = false;

                while (newFormula.indexOf("&%") != -1) {
                    i = newFormula.indexOf("&%");
                    j = i + 2;
                    //while (Character.isJavaIdentifierPart(newFormula.charAt(j)) && j < newFormula.length()) 
                    //    j++;
                    while ((j < newFormula.length())
                           && (Character.isJavaIdentifierPart(newFormula.charAt(j)) 
                               || newFormula.charAt(j) == '^')) {
                        if (newFormula.charAt(j) == '^') {
                            newFormula = newFormula.replaceFirst("\\^","_");
                            namespace = true;
                        }
                        j++;
                    }
                    //System.out.println("Candidate term: " + newFormula.substring(i+2,j));
            
                    while (!containsTerm(newFormula.substring(i+2,j)) && j > i + 2) 
                        j--;
                    term = newFormula.substring(i+2,j);
                    String termPrint = DocGen.getInstance().showTermName(this, term, language);

                    /*
                    //if (namespace) {
                    ArrayList al = null;
                    if (StringUtil.isNonEmptyString(term)) {
                    al = askWithRestriction(0,"termFormat",2,term);
                    }
                    if (al != null && al.size() > 0) {
                    for (int k = 0; k < al.size(); k++) {
                    Formula f = (Formula) al.get(k);
                    String lang = f.getArgument(1);
                    if (termPrint.equals(term) && lang.equals("EnglishLanguage")) {
                    termPrint = f.getArgument(3);
                    termPrint = StringUtil.removeEnclosingQuotes(termPrint);
                    }
                    if (language.equals(lang)) {
                    termPrint = f.getArgument(3);
                    termPrint = StringUtil.removeEnclosingQuotes(termPrint);
                    }
                    }
                    // termPrint = termPrint.replaceAll("\"","");
                    }
                    //}
                    */
                    if (term != "" && containsTerm(newFormula.substring(i+2,j))) {
                        newFormula = newFormula.substring(0,i) +
                            "<a href=\"" + href + term + suffix + "\">" + termPrint + "</a>" +
                            newFormula.substring(j,newFormula.toString().length());
                    }
                    else
                        newFormula = (newFormula.substring(0,i) 
                                      + newFormula.substring(j,newFormula.toString().length()));
                }
            }
        }
        catch (Exception ex) {
            System.out.println("ERROR in KB.formatDocumentation("
                               + href + ", "
                               + documentation + ", "
                               + language + ")");
            ex.printStackTrace();
        }
        return newFormula;
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
                    Iterator it = forms.iterator();
                    while (it.hasNext()) {
                        pr.println((String) it.next());                       
                        pr.println();
                    }
                    if (!file.exists()) {
                        filename = null;
                        throw new Exception("Error writing " + file.getCanonicalPath());
                    }
                }
            }
        }
        catch (Exception ex) {
            System.out.println("Error in KB.writeInferenceEngineFormulas(): " + ex.getMessage());
            ex.printStackTrace();
        }
        if (pr != null) {
            try {
                pr.close();
            }
            catch (Exception e1) {
            }
        }
        if (fr != null) {
            try {
                fr.close();
            }
            catch (Exception e2) {
            }
        }
        return filename;
    }

    /** *************************************************************
     *  Starts Vampire and collects, preprocesses and loads all of the
     *  constituents into it.
     */
    public void loadVampire() {

        // System.out.println("INFO in KB.loadVampire()");

        KBmanager mgr = KBmanager.getMgr();
        try {
            if (!formulaMap.isEmpty()) {

                System.out.println("INFO in KB.loadVampire(): preprocessing " 
                                   + formulaMap.size() 
                                   + " formulae");

                TreeSet forms = preProcess(formulaMap.keySet());
                String filename = writeInferenceEngineFormulas(forms);
                boolean vFileSaved = StringUtil.isNonEmptyString(filename);
                if (vFileSaved) {
                    System.out.println("INFO in KB.loadVampire(): " 
                                       + forms.size() 
                                       + " formulae saved to " 
                                       + filename);
                }
                else {
                    System.out.println("INFO in KB.loadVampire(): new -v.kif file not written");
                }

                if (inferenceEngine instanceof Vampire) {
                    System.out.println("INFO in KB.loadVampire(): terminating inference engine");
                    long t1 = System.currentTimeMillis();
                    inferenceEngine.terminate();
                    System.out.println("INFO in KB.loadVampire(): inference engine terminated in "
                                       + ((System.currentTimeMillis() - t1) / 1000.0)
                                       + " seconds");
                }

                inferenceEngine = null;

                if (StringUtil.isNonEmptyString(mgr.getPref("inferenceEngine")) && vFileSaved) {
                    System.out.println("INFO in KB.loadVampire(): getting new inference engine");
                    inferenceEngine = Vampire.getNewInstance(filename);
                }
                System.out.println("INFO in KB.loadVampire(): inferenceEngine == " 
                                   + inferenceEngine);        
            }
        }
        catch (Exception e) {
            System.out.println("Error in KB.loadVampire(): " + e.getMessage());
            e.printStackTrace();
        }
        if (!(inferenceEngine instanceof Vampire)) {
            mgr.setError(mgr.getError()
                         + "\n<br/>No local inference engine is available\n<br/>");
        }
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
     * Preprocess the knowledge base to work with Vampire.  This includes "holds"
     * prefixing, ticking nested formulas, expanding row variables, and
     * translating mathematical relation operators.
     * @return a TreeSet of Strings. 
     */
    public TreeSet preProcess(Set forms) {

        System.out.println("ENTER KB.preProcess()");

        TreeSet newTreeSet = new TreeSet();
        try {
            KBmanager mgr = KBmanager.getMgr();
            for (int i = 0 ; i < ppTimers.length ; i++) {
                ppTimers[i] = 0L;
            }
            
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
                        String er = ("Error in KB.preProcess(): " 
                                     + pe.getMessage() 
                                     + " at line " 
                                     + f.startLine
                                     + " in file "
                                     + f.sourceFile);
                        mgr.setError(mgr.getError() + "\n<br/>" + er + "\n<br/>");
                        System.out.println(er);
                    }
                    catch (IOException ioe) {
                        System.out.println("Error in KB.preProcess: " + ioe.getMessage());
                    }
                }

                Formula p = null;
                for (Iterator itp = processed.iterator(); itp.hasNext();) {
                    p = (Formula) itp.next();
                    if (StringUtil.isNonEmptyString(p.theFormula))
                        newTreeSet.add(p.theFormula);
                }
                long t_elapsed = (System.currentTimeMillis() - t_start);
                t_total += t_elapsed;
                if ((numberProcessed > 0) && ((numberProcessed % 1000) == 0)) {
                    System.out.println("Formulae per second per last 1000: "
                                       + (1000.0 / ((t_total - t_prevTotal) / 1000.0)));
                    t_prevTotal = t_total;
                }
                if (t_elapsed > 3000) {
                    System.out.println("DEBUG: "
                                       + (t_elapsed / 1000.0)
                                       + " seconds to process form:");
                    System.out.println("  f == " + f.toString());
                    System.out.println("  processed == " 
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
            System.out.println("INFO in KB.preProcess()");
            System.out.println("  "
                               + (dur / 1000.0)
                               + " seconds total to produce "
                               + newTreeSet.size()
                               + " formulas");
            System.out.println("    "
                               + (ppTimers[1] / 1000.0)
                               + " seconds instantiating predicate variables");
            System.out.println("    "
                               + (ppTimers[2] / 1000.0)
                               + " seconds expanding row variables");
            System.out.println("      "
                               + (ppTimers[3] / 1000.0)
                               + " seconds in Formula.getRowVarExpansionRange()");
            System.out.println("        "
                               + (ppTimers[4] / 1000.0)
                               + " seconds in Formula.toNegAndPosLitsWithRenameInfo()");
            System.out.println("      "
                               + (ppTimers[5] / 1000.0)
                               + " seconds in Formula.adjustExpansionCount()");
            System.out.println("    "
                               + (ppTimers[0] / 1000.0)
                               + " seconds adding type predicates");
            System.out.println("      "
                               + (ppTimers[7] / 1000.0)
                               + " seconds making quantifiers explicit");
            System.out.println("      "
                               + (ppTimers[8] / 1000.0)
                               + " seconds inserting type restrictions");
            System.out.println("    "
                               + (ppTimers[6] / 1000.0)
                               + " seconds in Formula.preProcessRecurse()");
            for (int i = 0 ; i < ppTimers.length ; i++) {
                ppTimers[i] = 0L;
            }

            if (tptpParseP) {
                int goodCount = 0;
                int badCount = 0;
                List badList = new ArrayList();
                for (it = formulaMap.values().iterator(); it.hasNext();) {
                    f = (Formula) it.next();
                    if (f.getTheTptpFormulas().isEmpty()) {
                        badCount++;
                        if (badCount < 11) {
                            badList.add(f);
                        }
                    }
                    else {
                        goodCount++;
                        if (goodCount < 10) {
                            System.out.println("Sample TPTP translation: " 
                                               + f.getTheTptpFormulas().get(0));
                        }
                    }
                }
                System.out.println("INFO in KB.preProcess(): TPTP translation succeeded for "
                                   + goodCount
                                   + " formula"
                                   + ((goodCount == 1) ? "" : "s"));
                boolean someAreBad = (badCount > 0);
                System.out.println("INFO in KB.preProcess(): TPTP translation failed for "
                                   + badCount
                                   + " formula"
                                   + ((badCount == 1) ? "" : "s")
                                   + (someAreBad ? ":" : ""));
                if (someAreBad) {
                    it = badList.iterator();
                    for (int i = 1 ; it.hasNext() ; i++) {
                        f = (Formula) it.next();
                        System.out.println("[" + i + "]: " + f);
                    }
                    if (badCount > 10) {
                        System.out.println("  " + (badCount - 10) + " more ...");
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
     
        clearSortalTypeCache();

        System.out.println("EXIT KB.preProcess()");

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
                    if (i % 100 == 1) System.out.print(".");
                }
            }
        }
        catch (Exception ex) {
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

            System.out.println("INFO in KB.writePrologFile(): Writing " + file.getCanonicalPath());

            if ((WordNet.wn != null) && WordNet.wn.wordFrequencies.isEmpty()) {
                WordNet.wn.readWordFrequencies();
            }
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
            e.printStackTrace();
            System.out.println("Error in KB.writePrologFile(): " + e.getMessage());
        }
        finally {
            try {
                if (pr != null) pr.close();
            }
            catch (Exception e1) {
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
                    if (badList.size() < 11) {
                        badList.add(f);
                    }
                }
                else {
                    goodCount++;
                }
            }
            System.out.println("INFO in KB.tptpParse(): TPTP translation succeeded for "
                               + goodCount
                               + " formula"
                               + ((goodCount == 1) ? "" : "s"));
            boolean someAreBad = (badCount > 0);
            System.out.println("INFO in KB.tptpParse(): TPTP translation failed for "
                               + badCount
                               + " formula"
                               + ((badCount == 1) ? "" : "s")
                               + (someAreBad ? ":" : ""));
            if (someAreBad) {
                it = badList.iterator();
                for (int i = 1 ; it.hasNext() ; i++) {
                    f = (Formula) it.next();
                    System.out.println("[" + i + "]: " + f);
                }
                if (badCount > 10) {
                    System.out.println("  " + (badCount - 10) + " more ...");
                }
            }
        }
        catch (Exception ex) {
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
            ex.printStackTrace();
        }
        finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            }
            catch (Exception ieo) {
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
                for (String axiom : axioms) {
                    out.writeBytes(axiom);
                }
                out.flush();        
            }
            // add conjecture
            if (StringUtil.isNonEmptyString(conjecture)) {
                out.writeBytes(conjecture);
                out.flush();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (out != null) {
                    out.close();
                }
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }
        return;
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
    public String writeTPTPFile(String fileName,
                                Formula conjecture, 
                                boolean onlyPlainFOL, 
                                String reasoner,
                                boolean isQuestion,
                                PrintWriter pw) {

        System.out.println("INFO in KB.writeTPTPFile(" 
                           + fileName + ", " 
                           + conjecture + ", " 
                           + onlyPlainFOL + ", " 
                           + reasoner + ", "
                           + isQuestion + ", "
                           + pw + ")");

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

            String sanitizedKBName = name.replaceAll("\\W","_");

            //----If file name is a directory, create filename therein
            if (fileName == null) {
                outputFile = File.createTempFile(sanitizedKBName, ".p", null);
                //----Delete temp file when program exits.
                outputFile.deleteOnExit();
            } else {
                outputFile = new File(fileName);
            }
            String canonicalPath = outputFile.getCanonicalPath();

            System.out.println("INFO in KB.writeTPTPFile(): Writing " + canonicalPath);

            if (pw instanceof PrintWriter) {
                pr = pw;
            }
            else {
                pr = new PrintWriter(new FileWriter(outputFile));
            }

            // If a PrintWriter object is passed in, we suppress this
            // copyright notice and assume that such a notice will be
            // provided somewhere is the wider calling context.
            if (pw == null) {
                pr.println("% Copyright 2009 Articulate Software Incorporated");
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
                    System.out.println("WARNING in KB.writeTPTPFile(" 
                                       + fileName + ", " 
                                       + conjecture + ", " 
                                       + onlyPlainFOL + ", " 
                                       + reasoner + ", "
                                       + isQuestion + ", "
                                       + pw + ")");
                    System.out.println("  Source file has changed to " + sourceFile);
                }
                oldSourceFile = sourceFile;

                // System.out.println("");
                // System.out.println("  f == " + f);

                tptpFormulas = f.getTheTptpFormulas();

                // System.out.println("  1 : tptpFormulas == " + tptpFormulas);

                //----If we are writing "sanitized" tptp, aka onlyPlainFOL,
                //----here we rename all VariableArityRelations so that each
                //----relation name has a numeric suffix corresponding to the
                //----number of the relation's arguments.  This is required
                //----for some provers, such as E and EP.
                if (onlyPlainFOL 
                    && !tptpFormulas.isEmpty() 
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
                            withRelnRenames.add(f2.renameVariableArityRelations(this));
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
                        if (theTPTPFormula.indexOf('\'') >= 0 
                            || theTPTPFormula.indexOf('"') >= 0 
                            || theTPTPFormula.matches(".*[(,]-?[0-9].*")) {
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
                    // pr.println("fof(kb_" + sourceFile + "_" + axiomIndex++ +
                    //            ",axiom,(" + theTPTPFormula + ")).");
                    // if (commentedFormula) {
                    pr.println();
                    // }
                }
                if (f.getTheTptpFormulas().isEmpty()) {
                    String addErrStr = "No TPTP formula for <br/>" + f.htmlFormat(this);
                    mgr.setError(mgr.getError() + "<br/>\n" + addErrStr + "\n<br/>");
                    System.out.println("INFO in KB.writeTPTPFile(): No TPTP formula for\n" + f);
                }
            }
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
            System.out.println("Error in KB.writeTPTPFile(): " + ex.getMessage());
            System.out.println("  fileName == " + fileName);
            System.out.println("  f == " + f);
            ex.printStackTrace();
        }
        finally {
            try {
                clearSortalTypeCache();
                if (pr != null) pr.close();
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }
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

        StringBuffer result = new StringBuffer();
        Iterator it = terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            result.append("(termFormat EnglishLanguage " + term + " \"" + prettyPrint(term) + "\")\n");
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
        Iterator it = terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            if (childOf(term,parent))
                result.append("(format EnglishLanguage " + term + " \"" + functionFormat(term,i) + "\")\n");            
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
        Iterator it = terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            if (childOf(term,parent))
                result.append("(format EnglishLanguage " + term + " \"" + relationFormat(term,i) + "\")\n");            
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
                              false  
                              );
            kb.preProcess(kb.getFormulas());
            File tptpFile = new File(kbDir, kb.name + ".tptp");
            String fileWritten = kb.writeTPTPFile(tptpFile.getCanonicalPath(), null, false, "none");
            if (StringUtil.isNonEmptyString(fileWritten)) {
                System.out.println("File written: " + fileWritten);
            }
            else {
                System.out.println("Could not write " + tptpFile.getCanonicalPath());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * List all terms that don't have an externalImage link
     */
    private void termsWithNoPictureLinks() {

        Iterator it = terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            ArrayList al = askWithRestriction(0,"externalImage",1,term);
            if (al == null || al.size() < 1) 
                System.out.println(term + ", ");            
        }
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
        String foo = "(rel bar \"test\")";
        Formula f = new Formula();
        f.read(foo);
        System.out.println(f.getArgument(2).equals("\"test\""));

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
