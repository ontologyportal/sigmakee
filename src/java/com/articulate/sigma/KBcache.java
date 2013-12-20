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

package com.articulate.sigma;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.articulate.sigma.KB;

/** *****************************************************************
 *  Contains methods for reading, writing knowledge bases and their
 *  configurations.  Also contains the inference engine process for
 *  the knowledge base.
 */
public class KBcache {

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
    private ArrayList<RelationCache> relationCaches = new ArrayList<RelationCache>();

    /** The String constant that is the suffix for files of cached assertions. */
    public static final String _cacheFileSuffix      = "_Cache.kif";

    /** If true, assertions of the form (predicate x x) will be
     * included in the relation cache tables. */
    private boolean cacheReflexiveAssertions = false;
    
    public HashMap relnsWithRelnArgs = null;
    
    private KB kb = null;
    
    /** *************************************************************
    * @return An ArrayList of RelationCache objects.
    */
    protected ArrayList<RelationCache> getRelationCaches() {
        return this.relationCaches;
    }
    
    /** ***************************************************
    * Return ArrayList of all nonrelTerms in an ArrayList
    *
    * @return An ArrayList of nonrelTerms
    */
    public static ArrayList<String> getAllNonRelTerms(ArrayList list) {
        
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
    * Return ArrayList of all relTerms in an ArrayList
    *
    * @return An ArrayList of relTerms
    */
    public static ArrayList<String> getAllRelTerms(ArrayList list) {
        
        ArrayList<String> relTerms = new ArrayList();
        Iterator<String> itr = list.iterator();
        while(itr.hasNext()) {
            String t = itr.next();
            if (Character.isLowerCase(t.charAt(0))) 
                relTerms.add(t);
        }
        return relTerms;
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
    private ArrayList<String> getCachedRelationNames() {
        
        ArrayList<String> relationNames = new ArrayList();
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
    private ArrayList<String> getCachedTransitiveRelationNames() {
        
        ArrayList<String> ans = new ArrayList<String>(cachedTransitiveRelationNames);
        try {
            Set<String> trset = kb.getAllInstancesWithPredicateSubsumption("TransitiveRelation");
            String name = null;
            for (Iterator<String> it = trset.iterator(); it.hasNext();) {
                name = it.next();
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
    private ArrayList<String> getCachedSymmetricRelationNames() {
        
        ArrayList<String> ans = new ArrayList<String>();
        try {
            Set symmset = kb.getAllInstancesWithPredicateSubsumption("SymmetricRelation");
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
    private ArrayList<String> getCachedReflexiveRelationNames() {
        
        ArrayList<String> ans = new ArrayList();
        try {
            List<String> allcached = getCachedRelationNames();
            List<String> reflexives = new ArrayList<String>(cachedReflexiveRelationNames);
            Iterator<String> it = kb.getAllInstancesWithPredicateSubsumption("ReflexiveRelation").iterator();
            while (it.hasNext()) {
                String name = it.next();
                if (!reflexives.contains(name)) 
                    reflexives.add(name);
            }
            it = reflexives.iterator();
            while (it.hasNext()) {
                String name = (String) it.next();
                if (allcached.contains(name)) 
                    ans.add(name);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
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
        
        if (sortalTypeCache == null) 
            sortalTypeCache = new HashMap<String, Object>();        
        return sortalTypeCache;
    }

    /** ***************************************************************
     * Clears the Map returned by KB.getSortalTypeCache().
     *
     * @return void
     */
    protected void clearSortalTypeCache() {
        
        try {
            //logger.info("Clearing " + getSortalTypeCache().size() + " entries");
            Object obj = null;
            for (Iterator it = getSortalTypeCache().values().iterator(); it.hasNext();) {
                obj = it.next();
                if (obj instanceof Collection) 
                    ((Collection) obj).clear();                
            }
            getSortalTypeCache().clear();
        }
        catch (Exception ex) {
            //logger.warning(ex.getStackTrace().toString());
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
        
        if (getSortalTypeCache().isEmpty()) 
            // logger.info("KB.getSortalTypeCache() == " + getSortalTypeCache());   
            System.out.println("KB.getSortalTypeCache() == " + getSortalTypeCache());
        else 
            clearSortalTypeCache();        
        return;
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
     */
    protected void initRelationCaches(boolean clearExistingCaches) {

        System.out.println("KBcache.initRelationCaches(): clearExistingCaches = " + clearExistingCaches);
        if (clearExistingCaches) {  // Clear all cache maps. 
            Iterator<RelationCache> it = getRelationCaches().iterator();
            while (it.hasNext()) {
                RelationCache rc = it.next();
                rc.clear();
            }            
            getRelationCaches().clear();  // Discard all cache maps.
        }
        List symmetric = getCachedSymmetricRelationNames();
        Iterator<String> it2 = getCachedRelationNames().iterator();
        while (it2.hasNext()) {
            String relname = it2.next();
            getRelationCache(relname, 1, 2);
            System.out.println("  " + relname);
            // We put each symmetric relation -- disjoint and a
            // few others -- into just one RelationCache table
            // apiece.  All transitive binary relations are cached
            // in two RelationCaches, one that looks "upward" from
            // the keys, and another that looks "downward" from
            // the keys.
            if (!symmetric.contains(relname)) 
                getRelationCache(relname, 2, 1);            
        }
        return;
    }

    /** *************************************************************     
     */
    public HashSet<String> getParents(String term) {
        return getRelationCache("subclass", 1, 2).get(term);
    }

    /** *************************************************************     
     */
    public HashSet<String> getChildren(String term) {
        return getRelationCache("subclass", 2, 1).get(term);
    }
    
    /** *************************************************************     
     */
    public HashSet<String> getDisjoint(String term) {
        return getRelationCache("disjoint", 1, 2).get(term);
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
     * @param valueArg An int value that indicates the argument
     * position of the cache values.     
     * @return a RelationCache object, or null if there is no cache
     * corresponding to the input arguments.
     */
    private RelationCache getRelationCache(String relName, int keyArg, int valueArg) {
        
        RelationCache result = null;
        if (StringUtil.isNonEmptyString(relName)) {
            RelationCache cache = null;
            Iterator<RelationCache> it = getRelationCaches().iterator();
            while (it.hasNext()) {
                cache = it.next();
                if (cache.getRelationName().equals(relName)
                    && (cache.getKeyArgument() == keyArg)
                    && (cache.getValueArgument() == valueArg)) {
                    result = cache;
                    break;
                }
            }
            if (result == null) {
                cache = new RelationCache(relName, keyArg, valueArg);
                getRelationCaches().add(cache);
                result = cache;
            }
        }        
        return result;
    }

    /** *************************************************************
     */
    private boolean isClosureComputed(ArrayList<RelationCache> caches) {
        
        if (caches == null) 
            return false;
        boolean isClosureComputed = false;
        RelationCache rc = null;
        Iterator<RelationCache> it = caches.iterator();
        while (it.hasNext()) {
            rc = (RelationCache) it.next();
            if (rc.getIsClosureComputed()) {
                isClosureComputed = true;
                break;
            }
        }
        return isClosureComputed;
    }
    
    /** *************************************************************
     * Delete and writes the cache .kif file then call addConstituent() so
     * that the file can be processed and loaded by the inference engine.
     */
    public void cache() {
                     
        ArrayList<RelationCache> caches = getRelationCaches();
        if (!isClosureComputed(caches)) // Don't bother writing the cache file cache closure not computed
            return;        
        FileWriter fw = null;
        try {
            File dir = new File(KBmanager.getMgr().getPref("kbDir"));
            File f = new File(dir, (kb.name + _cacheFileSuffix));
            if (f.exists()) 
                f.delete();                                           
            String filename = f.getCanonicalPath();
            fw = new FileWriter(f, true);
            Iterator<RelationCache> it = caches.iterator();
            while (it.hasNext()) {
                RelationCache rc = it.next();
                if (rc.getKeyArgument() == 1) {
                    String relation = rc.getRelationName();                    
                    if (!relation.equals("disjoint")) {  // Too many disjoint classes to cache
                        Iterator<String> it2 = null;
                        it2 = rc.keySet().iterator();
                        while (it2.hasNext()) {
                            String arg1 = it2.next();
                            Set valSet = (Set) rc.get(arg1);
                            Iterator<String> it3 = null;
                            it3 = valSet.iterator();
                            while (it3.hasNext()) {
                                String arg2 = it3.next();
                                StringBuilder sb = new StringBuilder();
                                sb.append("(" + relation + " " + arg1 + " " + arg2 + ")");
                                String tuple = sb.toString();
                                if (!kb.formulaMap.containsKey(tuple.intern())
                                    && (getCacheReflexiveAssertions() || !arg1.equals(arg2))) {
                                    fw.write(tuple);
                                    fw.write(System.getProperty("line.separator"));
                                }
                            }
                        }
                    }
                }
            }
            if (fw != null) {
                fw.close();
                fw = null;
            }
            kb.constituents.remove(filename);
            kb.addConstituent(filename, false, false, true);
            KBmanager.getMgr().writeConfiguration();
        }                   
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (fw != null) 
                    fw.close();                
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
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
        if ((cache != null) && StringUtil.isNonEmptyString(keyTerm) && StringUtil.isNonEmptyString(valueTerm)) {
            HashSet<String> valueSet = cache.get(keyTerm);
            if (valueSet == null) {
                valueSet = new HashSet<String>();
                cache.put(keyTerm, valueSet);
            }
            if (valueSet.add(valueTerm)) 
                count++;            
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
    public HashSet<String> getCachedRelationValues(String relation, String term,
                                           int keyArg, int valueArg) {
        
        HashSet<String> ans = new HashSet();
        try {
            RelationCache cache = getRelationCache(relation, keyArg, valueArg);
            if (cache != null) {
                HashSet<String> values = cache.get(term);
                if (values != null) 
                    ans.addAll(values);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * This method computes the transitive closure for the relation
     * identified by relationName.  The results are stored in the
     * RelationCache object for the relation and "direction" (looking
     * from the arg1 keys toward arg2 parents, or looking from the
     * arg2 keys toward arg1 children).
     *
     * @param relationName The name of a relation
     */
    private void computeTransitiveCacheClosure(String relationName) {

        // System.out.print(" " + relationName);
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
                Object[] valArr = null;
                boolean changed = true;
                while (changed) {
                    changed = false;
                    Iterator<String> it1 = c1Keys.iterator();
                    while (it1.hasNext()) {
                        String keyTerm = it1.next();
                        String valTerm = null;
                        if (StringUtil.emptyString(keyTerm)) 
                            System.out.println("Error in KB.computeTransitiveCacheClosure(" + relationName + ") \n   keyTerm == " +
                                           ((keyTerm == null) ? null : "\"" + keyTerm + "\""));
                        else {
                            HashSet<String> valSet = c1.get(keyTerm);
                            valArr = valSet.toArray();
                            for (int i = 0; i < valArr.length; i++) {
                                valTerm = (String) valArr[i];
                                HashSet<String> valSet2 = c1.get(valTerm);
                                long count = 0L;
                                if (valSet2 != null) {
                                    Iterator<String> it2 = valSet2.iterator();
                                    while (it2.hasNext() && (count < MAX_CACHE_SIZE)) {
                                        if (valSet.add(it2.next())) {
                                            changed = true;
                                            count++;
                                        }
                                    }
                                }
                                if (count < MAX_CACHE_SIZE) {
                                    valSet2 = c2.get(valTerm);
                                    if (valSet2 == null) {
                                        valSet2 = new HashSet<String>();
                                        c2.put(valTerm, valSet2);
                                    }
                                    if (valSet2.add(keyTerm)) {
                                        changed = true;
                                        count++;
                                    }
                                }
                            }
                            // Here we try to ensure that instances of Relation have at least some entry in the
                            // "instance" caches, since this information is sometimes considered
                            // redundant and so could be left out of .kif files.
                            if (isSubrelationCache) {
                                valTerm = "Relation";
                                if (keyTerm.endsWith("Fn")) 
                                    valTerm = "Function";                                    
                                else {                                        
                                    if (Character.isLowerCase(keyTerm.charAt(0)) && !keyTerm.contains("(")) 
                                        valTerm = "Predicate";                                        
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
        return;
    }

    /** *************************************************************
     * This method computes the closure for the cache of the instance
     * relation, in both directions.
     */
    private void computeInstanceCacheClosure() {

        try {
            RelationCache ic1 = getRelationCache("instance", 1, 2);
            RelationCache ic2 = getRelationCache("instance", 2, 1);
            RelationCache sc1 = getRelationCache("subclass", 1, 2);
            Set ic1KeySet = ic1.keySet();
            Iterator it1 = ic1KeySet.iterator();
            Object[] ic1ValArr = null;
            String ic1ValTerm = null;
            while (it1.hasNext()) {
                String ic1KeyTerm = (String) it1.next();
                HashSet<String> ic1ValSet = ic1.get(ic1KeyTerm);
                long count = 0L;
                ic1ValArr = ic1ValSet.toArray();
                for (int i = 0 ; i < ic1ValArr.length ; i++) {
                    ic1ValTerm = (String) ic1ValArr[i];
                    if (ic1ValTerm != null) {
                        HashSet<String> sc1ValSet = sc1.get(ic1ValTerm);
                        if (sc1ValSet != null) {
                            Iterator<String> it2 = sc1ValSet.iterator();
                            while (it2.hasNext() && (count < MAX_CACHE_SIZE)) {
                                if (ic1ValSet.add(it2.next())) 
                                    count++;                                
                            }
                        }
                    }
                }
                if (count < MAX_CACHE_SIZE) {
                    Iterator<String> it2 = ic1ValSet.iterator();
                    while (it2.hasNext()) {
                        ic1ValTerm = (String) it2.next();
                        HashSet<String> ic2ValSet = ic2.get(ic1ValTerm);
                        if (ic2ValSet == null) {
                            ic2ValSet = new HashSet();
                            ic2.put(ic1ValTerm, ic2ValSet);
                        }
                        if (ic2ValSet.add(ic1KeyTerm)) 
                            count++;                        
                    }
                }
            }
            ic1.setIsClosureComputed(true);
            ic2.setIsClosureComputed(true);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }       
        return;
    }

    /** *************************************************************
     * This method computes the closure for the caches of symmetric
     * relations.  As currently implemented, it really applies to only
     * disjoint.
     */
    private void computeSymmetricCacheClosure(String relationName) {

    	System.out.println("INFO in KBcache.cacheRelnsWithRelnArgs(): " + relationName);
        RelationCache dc1 = getRelationCache(relationName, 1, 2);
        RelationCache sc2 = (relationName.equals("disjoint")
                             ? getRelationCache("subclass", 2, 1)
                             : null);
        if (sc2 != null) {
            boolean changed = true;
            while (changed) {
                changed = false;
                Set dc1KeySet = dc1.keySet();
                Object[] dc1KeyArr = dc1KeySet.toArray();
                for (int i = 0; (i < dc1KeyArr.length); i++) {
                    String dc1KeyTerm = (String) dc1KeyArr[i];
                    HashSet<String> dc1ValSet = dc1.get(dc1KeyTerm);
                    Object[] dc1ValArr = dc1ValSet.toArray();
                    for (int j = 0 ; j < dc1ValArr.length ; j++) {
                        String dc1ValTerm = (String) dc1ValArr[j];
                        Set sc2ValSet = sc2.get(dc1ValTerm);
                        if (sc2ValSet != null) {
                            if (dc1ValSet.addAll(sc2ValSet)) 
                                changed = true;                            
                        }
                    }
                    Set sc2ValSet = sc2.get(dc1KeyTerm);
                    if (sc2ValSet != null) {
                        Iterator<String> it3 = sc2ValSet.iterator();
                        while (it3.hasNext()) {
                            String sc2ValTerm = it3.next();
                            HashSet<String> dc1ValSet2 = dc1.get(sc2ValTerm);
                            if (dc1ValSet2 == null) {
                                dc1ValSet2 = new HashSet<String>();
                                dc1.put(sc2ValTerm, dc1ValSet2);
                            }
                            if (dc1ValSet2.addAll(dc1ValSet)) 
                                changed = true;                                
                        }
                    }
                }
                if (changed)
                    dc1.setIsClosureComputed(true);
            }
        }
        return;
    }

    /** *************************************************************
     * This method builds a cache of all Relations in the current KB
     * for which at least one argument must be filled by a relation
     * name (or a variable denoting a relation name).  This method
     * should be called only after the subclass cache has been built.
     */
    private void cacheRelnsWithRelnArgs() {

    	System.out.println("INFO in KBcache.cacheRelnsWithRelnArgs()");
        if (relnsWithRelnArgs == null) 
            relnsWithRelnArgs = new HashMap();            
        relnsWithRelnArgs.clear();
        Set relnClasses = getCachedRelationValues("subclass", "Relation", 2, 1);
        if (relnClasses != null)
            relnClasses.add("Relation");
        if (relnClasses != null) {
            boolean[] signature = null;
            Iterator<String> it = relnClasses.iterator();
            while (it.hasNext()) {
                String relnClass = it.next();
                ArrayList<Formula> formulas = kb.askWithRestriction(3, relnClass, 0, "domain");
                if (formulas != null) {
                    Iterator<Formula> it2 = formulas.iterator();
                    while (it2.hasNext()) {
                        Formula f = it2.next();
                        String reln = f.getArgument(1);
                        int valence = kb.getValence(reln);
                        if (valence < 1) 
                            valence = Formula.MAX_PREDICATE_ARITY;                            
                        signature = (boolean[]) relnsWithRelnArgs.get(reln);
                        if (signature == null) {
                            signature = new boolean[ valence + 1 ];
                            for (int j = 0 ; j < signature.length ; j++) 
                                signature[j] = false;                                
                            relnsWithRelnArgs.put(reln, signature);
                        }
                        int argPos = Integer.parseInt(f.getArgument(2));
                        signature[argPos] = true;
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
                for (int i = 0 ; i < signature.length ; i++) 
                    signature[i] = (i == 2);                    
                relnsWithRelnArgs.put("format", signature);
            }
        }
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
    public HashMap relationValences = new HashMap();

    /** *************************************************************
     *  */
    private void cacheRelationValences() {

    	System.out.println("INFO in KBcache.cacheRelationValences()");
        HashSet<String> relations = getCachedRelationValues("instance", "Relation", 2, 1);
        if (relations != null) {
            List<String> namePrefixes = Arrays.asList("VariableArity","Unary","Binary",
                                                      "Ternary","Quaternary","Quintary");
            int nplen = namePrefixes.size();
            RelationCache ic1 = getRelationCache("instance", 1, 2);
            RelationCache ic2 = getRelationCache("instance", 2, 1);
            Iterator<String> it = relations.iterator();
            while (it.hasNext()) {
                String reln = it.next();
                // Evaluate getValence() to build the relationValences cache, and use its 
                // return value to fill in any info that might be missing from the "instance" cache.
                int valence = kb.getValence(reln);
                if ((valence > -1) && (valence < nplen)) {
                    StringBuilder sb = new StringBuilder();
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
                    String className = sb.toString();
                    if (StringUtil.isNonEmptyString(className)) {
                        addRelationCacheEntry(ic1, reln, className);
                        addRelationCacheEntry(ic2, className, reln);
                    }
                }
            }    
        }
        return;
    }

    /** *************************************************************
     * Populates all caches with ground assertions, from which
     * closures can be computed.
     */
    private void cacheGroundAssertions() {

    	System.out.println("INFO in KBcache.cacheGroundAssertions()");
        ArrayList<String> symmetric = getCachedSymmetricRelationNames();
        ArrayList<String> reflexive = getCachedReflexiveRelationNames();
        HashSet<String> subInverses = 
                new HashSet<String>(kb.getTermsViaPredicateSubsumption("subrelation",
                                                                      2, "inverse", 1,true));
        subInverses.add("inverse");
        Iterator<String> it = getCachedRelationNames().iterator();
        while (it.hasNext()) {
            String relation = it.next();
            ArrayList<Formula> forms = kb.ask("arg", 0, relation);
            if (forms != null) {
                RelationCache c1 = getRelationCache(relation, 1, 2);
                RelationCache c2 = getRelationCache(relation, 2, 1);
                RelationCache inv1 = (subInverses.contains(relation)
                                    ? getRelationCache("inverse", 1, 2)
                                    : null);
                boolean isSubInverse = (inv1 != null);
                Iterator<Formula> formsIt = forms.iterator();
                while (formsIt.hasNext()) {
                    Formula formula = formsIt.next();
                    if ((formula.theFormula.indexOf("(",2) == -1)
                        && !formula.sourceFile.endsWith(_cacheFileSuffix)) {
                        String arg1 = formula.getArgument(1).intern();
                        String arg2 = formula.getArgument(2).intern();
                        if (StringUtil.isNonEmptyString(arg1)
                            && StringUtil.isNonEmptyString(arg2)) {
                            addRelationCacheEntry(c1, arg1, arg2);
                            addRelationCacheEntry(c2, arg2, arg1);
                            // Special cases.
                            if (getCacheReflexiveAssertions()
                                && reflexive.contains(relation)) {
                                addRelationCacheEntry(c1, arg1, arg1);
                                addRelationCacheEntry(c1, arg2, arg2);
                                addRelationCacheEntry(c2, arg1, arg1);
                                addRelationCacheEntry(c2, arg2, arg2);
                            }
                            if (symmetric.contains(relation))
                                addRelationCacheEntry(c1, arg2, arg1);
                            if (isSubInverse) {
                                addRelationCacheEntry(c1, arg2, arg1);
                                addRelationCacheEntry(inv1, arg1, arg2);
                                addRelationCacheEntry(inv1, arg2, arg1);
                            }
                        }
                    }
                }
            }
            // More ways of collecting implied disjointness assertions.
            if (relation.equals("disjoint")) {
                ArrayList<Formula> partitions = kb.ask("arg", 0, "partition");
                ArrayList<Formula> decompositions = kb.ask("arg", 0, "disjointDecomposition");
                HashSet<Formula> formset = new HashSet<Formula>();
                if (partitions != null)
                    formset.addAll(partitions);
                if (decompositions != null)
                    formset.addAll(decompositions);
                RelationCache c1 = getRelationCache(relation, 1, 2);
                Iterator<Formula> formsIt = formset.iterator();
                while (formsIt.hasNext()) {
                    Formula formula = (Formula) formsIt.next();
                    if ((formula.theFormula.indexOf("(",2) == -1)
                        && !formula.sourceFile.endsWith(_cacheFileSuffix)) {
                        ArrayList<String> arglist = formula.argumentsToArrayList(2);
                        for (int i = 0 ; i < arglist.size() ; i++) {
                            for (int j = 0 ; j < arglist.size() ; j++) {
                                if (i != j) {
                                    String arg1 = ((String) arglist.get(i)).intern();
                                    String arg2 = ((String) arglist.get(j)).intern();
                                    if (StringUtil.isNonEmptyString(arg1)
                                        && StringUtil.isNonEmptyString(arg2)) {
                                        addRelationCacheEntry(c1, arg1, arg2);
                                        addRelationCacheEntry(c1, arg2, arg1);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return;
    }

    /** *************************************************************
     * Populates all caches with ground assertions, from which
     * closures can be computed.
     */
    private void cacheGroundAssertionsAndPredSubsumptionEntailments() {

    	System.out.println("INFO in KBcache.cacheGroundAssertionsAndPredSubsumptionEntailments()");
        ArrayList<String> symmetric = getCachedSymmetricRelationNames();
        ArrayList<String> reflexive = getCachedReflexiveRelationNames();
        Iterator<String> it = getCachedRelationNames().iterator();
        while (it.hasNext()) {
            String relation = it.next();
            ArrayList<String> relationSet = 
                    kb.getTermsViaPredicateSubsumption("subrelation", 2, relation, 1, true);
            relationSet.add(relation);
            HashSet<Formula> formulae = new HashSet<Formula>();
            Iterator<String> itr = relationSet.iterator();
            while (itr.hasNext()) {
                ArrayList<Formula> forms = kb.ask("arg", 0, (String) itr.next());
                if (forms != null) 
                    formulae.addAll(forms);
            }
            if (!formulae.isEmpty()) {
                RelationCache c1 = getRelationCache(relation, 1, 2);
                RelationCache c2 = getRelationCache(relation, 2, 1);
                Iterator<Formula> itf = formulae.iterator();
                while (itf.hasNext()) {
                    Formula f = itf.next();
                    if ((f.theFormula.indexOf("(",2) == -1)
                        && !f.sourceFile.endsWith(_cacheFileSuffix)) {
                        String arg1 = f.getArgument(1).intern();
                        String arg2 = f.getArgument(2).intern();
                        if (StringUtil.isNonEmptyString(arg1) && StringUtil.isNonEmptyString(arg2)) {
                            addRelationCacheEntry(c1, arg1, arg2);
                            addRelationCacheEntry(c2, arg2, arg1);
                            if (symmetric.contains(relation)) {
                                addRelationCacheEntry(c1, arg2, arg1);
                                addRelationCacheEntry(c2, arg1, arg2);
                            }
                            if (getCacheReflexiveAssertions()
                                && reflexive.contains(relation)) {
                                addRelationCacheEntry(c1, arg1, arg1);
                                addRelationCacheEntry(c1, arg2, arg2);
                                addRelationCacheEntry(c2, arg1, arg1);
                                addRelationCacheEntry(c2, arg2, arg2);
                            }
                        }
                    }
                }
            }
            // More ways of collecting implied disjointness assertions.
            if (relation.equals("disjoint")) {
                formulae.clear();
                ArrayList<Formula> partitions = kb.ask("arg", 0, "partition");
                ArrayList<Formula> decompositions = kb.ask("arg", 0, "disjointDecomposition");
                if (partitions != null)
                    formulae.addAll(partitions);
                if (decompositions != null)
                    formulae.addAll(decompositions);
                RelationCache c1 = getRelationCache(relation, 1, 2);
                Iterator<Formula> itf = formulae.iterator();
                while (itf.hasNext()) {
                    Formula f = (Formula) itf.next();
                    if ((f.theFormula.indexOf("(",2) == -1) && !f.sourceFile.endsWith(_cacheFileSuffix)) {
                        ArrayList<String> arglist = f.argumentsToArrayList(2);
                        for (int i = 0 ; i < arglist.size(); i++) {
                            for (int j = 0; j < arglist.size(); j++) {
                                if (i != j) {
                                    String arg1 = arglist.get(i).intern();
                                    String arg2 = arglist.get(j).intern();
                                    if (StringUtil.isNonEmptyString(arg1)
                                        && StringUtil.isNonEmptyString(arg2)) {
                                        addRelationCacheEntry(c1, arg1, arg2);
                                        addRelationCacheEntry(c1, arg2, arg1);
                                    }
                                }
                            }
                        }
                    }
                }
            }               
        }
        return;
    }
    
    /** *************************************************************
     * Builds all of the relation caches for the current KB.  If
     * RelationCache Map objects already exist, they are cleared and
     * discarded.  New RelationCache Maps are created, and all caches
     * are rebuilt.
     */
    public void buildRelationCaches() {

    	System.out.println("INFO in KBcache.buildRelationCaches()");
        long t1 = System.currentTimeMillis();
        long totalCacheEntries = 0L;
        initRelationCaches(true);
        boolean changed = false;
        for (int i = 1; i < 5; i++) {
            changed = false;
            cacheGroundAssertionsAndPredSubsumptionEntailments();  // 1
            Iterator<String> it = getCachedTransitiveRelationNames().iterator();
            while (it.hasNext()) {
                String relationName = (String) it.next();
                computeTransitiveCacheClosure(relationName);  //2
            }
            computeInstanceCacheClosure();
            it = getCachedSymmetricRelationNames().iterator();
            while (it.hasNext()) {
                String relationName = it.next();
                if (Arrays.asList("disjoint").contains(relationName))
                    computeSymmetricCacheClosure(relationName); // 3
            }
            cacheRelnsWithRelnArgs(); // 4
            cacheRelationValences();  // 5
            long entriesAfterThisIteration = 0L;
            Iterator<RelationCache> it2 = getRelationCaches().iterator();
            while (it2.hasNext()) {
                RelationCache relationCache = it2.next();
                if (!relationCache.isEmpty()) {
                    Iterator itv = relationCache.values().iterator();
                    while (itv.hasNext()) 
                        entriesAfterThisIteration += ((Set) itv.next()).size();                        
                }
            }
            if (entriesAfterThisIteration > totalCacheEntries)
                totalCacheEntries = entriesAfterThisIteration;
            else
                return;
        }
        System.out.println("Error: KBcache.buildRelationCaches() terminated early.");
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
    public void buildRelationCaches(KB kb) {

        this.kb = kb;
        buildRelationCaches();
        return;
    }
    
    /** *************************************************************
     * Instances of RelationCache hold the cached extensions and, when
     * possible, the computed closures, of selected relations.
     * Canonical examples are the caches for subclass and instance.   
     * The key is the name of the term and the values are the terms
     * that exist in the transitive closure of the relation for that term.    
     */
    class RelationCache extends HashMap<String,HashSet<String>> {

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
    public static void main(String[] args) {

    }
}
