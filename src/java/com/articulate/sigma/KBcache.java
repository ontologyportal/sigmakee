/** This code is copyright Infosys 2017-, Articulate Software  2003-2017.  Some
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
https://github.com/ontologyportal

Note that this class, and therefore, Sigma, depends upon several terms
being present in the ontology in order to function as intended.  They are:
  domain
  domainSubclass
  Entity
  instance
  Relation
  subclass
  subrelation
  TransitiveRelation
*/

/*************************************************************************************************/

package com.articulate.sigma;

import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import com.articulate.sigma.trans.SUMOtoTFAform;
import com.articulate.sigma.utils.AVPair;
import com.articulate.sigma.utils.StringUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class KBcache implements Serializable {

    // The String constant that is the suffix for files of cached assertions.
    public static final String _cacheFileSuffix = "_Cache.kif";

    // all the transitive relations that are known to be appropriate to use at the time
    // this code was created
    public static final List<String> intendedTransRels =
            Arrays.asList("subclass", "subrelation", "subAttribute"); //, "located", "geographicSubregion");

    public static boolean debug = false;

    /** Errors found during processing formulas */
    public static Set<String> errors = new TreeSet<>();

    public KB kb = null;

    // all the relations in the kb
    public Set<String> relations = new HashSet<>();

    // all the functions in the kb
    public Set<String> functions = new HashSet<>();

    // all relations that are not functions
    public Set<String> predicates = new HashSet<>();

    // all the transitive relations in the kb
    public Set<String> transRels = new HashSet<>();

    // all the transitive relations between instances in the kb that must have the same type
    public Set<String> instRels = new HashSet<>();

    // all the transitive relations between instances in the kb
    public Set<String> instTransRels = new HashSet<>();

    /** All the cached "parent" relations of all transitive relations
     * meaning the relations between all first arguments and the
     * transitive closure of second arguments.  The external HashMap
     * pairs relation name String keys to values that are the parent
     * relationships.  The interior HashMap is the set of terms and
     * their transitive closure of parents.
     */
    public Map<String, Map<String, Set<String>>> parents = new HashMap<>();

    /** Parent relations from instances, including those that are
     * transitive through (instance,instance) relations, such as
     * subAttribute and subrelation.  May not do what you think
     * since the key is the child (instance)
     */
    public Map<String, Set<String>> instanceOf = new HashMap<>();

    // all the instances of a class key, including through subrelation
    // and subAttribute
    public Map<String, Set<String>> instances = new HashMap<>();

    /** A temporary list of instances built during creation of the
     * children map, in order to efficiently create the instances map
     **/
    public Set<String> insts = new HashSet<>();

    /** All the cached "child" relations of all transitive relations
     * meaning the relations between all first arguments and the
     * transitive closure of second arguments.  The external HashMap
     * pairs relation name String keys to values that are the child
     * relationships.  The interior HashMap is the set of terms and
     * their transitive closure of children.
     */
    public Map<String, Map<String, Set<String>>> children = new HashMap<>();

    /** Relation name keys and argument types with 0th arg always ""
     * except in the case of Functions where the 0th arg will be the
     * function range.
     * Variable arity relations may have a type for the last argument,
     * which will be the type repeated for all extended arguments.
     * Note that types can be functions, rather than just terms. Note that
     * types (when there's a domainSubclass etc) are designated by a
     * '+' appended to the class name.
     **/
    public Map<String, List<String>> signatures = new HashMap<>();

    // The number of arguments to each relation.  Variable arity is -1
    public Map<String, Integer> valences = new HashMap<>();

    /** Disjoint relationships which were explicitly defined in "partition", "disjoint",
     * and "disjointDecomposition" expressions
     **/
    public Map<String, Set<String>> explicitDisjoint = new HashMap<>();

    // each pair of classes as class1\tclass2
    // transitive closure of classes based on explicitDisjoint
    public Set<String> disjoint = new HashSet<>();

    // each pair of relations as rel1\trel2
    public Set<String> disjointRelations = new HashSet<>();

    public boolean initialized = false;

    private static final float LOAD_FACTOR = 0.75f;

    /****************************************************************
     * empty constructor for testing only
     */
    public KBcache() {

    }

    /****************************************************************
     */
    public KBcache(KB kbin) {

        relations = new HashSet<>(kbin.getCountTerms()/3,LOAD_FACTOR);
        functions = new HashSet<>(kbin.getCountTerms()/9,LOAD_FACTOR);
        predicates = new HashSet<>(kbin.getCountTerms()/3,LOAD_FACTOR);
        transRels = new HashSet<>(60,LOAD_FACTOR);
        // instRels = new HashSet<String>();
        instTransRels = new HashSet<>(50,LOAD_FACTOR);
        parents = new HashMap<>(60,LOAD_FACTOR);
        instanceOf = new HashMap<>(kbin.getCountTerms()/3,LOAD_FACTOR);
        instances = new HashMap<>(kbin.getCountTerms(),LOAD_FACTOR);
        insts = new HashSet<>(kbin.getCountTerms(),LOAD_FACTOR);
        children = new HashMap<>(60,LOAD_FACTOR);
        signatures = new HashMap<>(kbin.getCountTerms()/3,LOAD_FACTOR);
        valences = new HashMap<>(kbin.getCountTerms()/3,LOAD_FACTOR);
        explicitDisjoint = new HashMap<>(kbin.getCountTerms()/3,LOAD_FACTOR);
        disjoint = new HashSet<>(kbin.getCountTerms()/3,LOAD_FACTOR);
        disjointRelations = new HashSet<>(kbin.getCountTerms()/3,LOAD_FACTOR);
        this.kb = kbin;
    }

    /****************************************************************
     */
    public KBcache(KBcache kbCacheIn, KB kbIn) {

        this.kb = kbIn;
        if (kbCacheIn.relations != null) {
            this.relations = Sets.newHashSet(kbCacheIn.relations);
        }
        if (kbCacheIn.transRels != null) {
            this.transRels = Sets.newHashSet(kbCacheIn.transRels);
        }
        if (kbCacheIn.instTransRels != null) {
            this.instTransRels = Sets.newHashSet(kbCacheIn.instTransRels);
        }
        if (kbCacheIn.parents != null) {
            String outerKey;
            Map<String, Set<String>> newInnerMap, oldInnerMap;
            String innerKey;
            Set newInnerSet;
            for (Map.Entry<String, Map<String, Set<String>>> outerEntry : kbCacheIn.parents.entrySet()) {
                outerKey = outerEntry.getKey();

                newInnerMap = Maps.newHashMap();
                oldInnerMap = outerEntry.getValue();
                for (Map.Entry<String, Set<String>> innerEntry : oldInnerMap.entrySet()) {
                    innerKey = innerEntry.getKey();

                    newInnerSet = Sets.newHashSet(innerEntry.getValue());
                    newInnerMap.put(innerKey, newInnerSet);
                }
                this.parents.put(outerKey, newInnerMap);
            }
        }
        if (kbCacheIn.instanceOf != null) {
            String key;
            Set<String> newSet;
            for (Map.Entry<String, Set<String>> entry : kbCacheIn.instanceOf.entrySet()) {
                key = entry.getKey();
                newSet = Sets.newHashSet(entry.getValue());
                this.instanceOf.put(key, newSet);
            }
        }
        if (kbCacheIn.insts != null) {
            this.insts = Sets.newHashSet(kbCacheIn.insts);
        }
        if (kbCacheIn.children != null) {
            String outerKey, innerKey;
            Map<String, Set<String>> newInnerMap, oldInnerMap;
            Set newInnerSet;
            for (Map.Entry<String, Map<String, Set<String>>> outerEntry : kbCacheIn.children.entrySet()) {
                outerKey = outerEntry.getKey();

                newInnerMap = Maps.newHashMap();
                oldInnerMap = outerEntry.getValue();
                for (Map.Entry<String, Set<String>> innerEntry : oldInnerMap.entrySet()) {
                    innerKey = innerEntry.getKey();
                    if (innerEntry.getValue() != null) {
                        newInnerSet = Sets.newHashSet(innerEntry.getValue());
                        newInnerMap.put(innerKey, newInnerSet);
                    }
                }
                this.children.put(outerKey, newInnerMap);
            }
        }

        if (kbCacheIn.signatures != null) {
            String key;
            List<String> newSet;
            for (Map.Entry<String, List<String>> entry : kbCacheIn.signatures.entrySet()) {
                key = entry.getKey();
                newSet = Lists.newArrayList(entry.getValue());
                this.signatures.put(key, newSet);
            }
        }
        if (kbCacheIn.valences != null) {
            this.valences = Maps.newHashMap(kbCacheIn.valences);
        }
        if (kbCacheIn.explicitDisjoint != null) {
            String key;
            Set<String> newSet;
            for (Map.Entry<String, Set<String>> entry : kbCacheIn.explicitDisjoint.entrySet()) {
                key = entry.getKey();
                newSet = Sets.newHashSet(entry.getValue());
                this.explicitDisjoint.put(key, newSet);
            }
        }
    }

    /***************************************************************
     * Experimental: Utility method to perform a merge with the KBcache input
     *
     * @param kbCacheIn the incoming cache to merge
     */
    public void mergeCaches(KBcache kbCacheIn) {

        if (kbCacheIn.children != null)
            this.children.putAll(kbCacheIn.children);
        if (kbCacheIn.disjoint != null)
            this.disjoint.addAll(kbCacheIn.disjoint);
        if (kbCacheIn.disjointRelations != null)
            this.disjointRelations.addAll(kbCacheIn.disjointRelations);
        if (kbCacheIn.explicitDisjoint != null)
            this.explicitDisjoint.putAll(kbCacheIn.explicitDisjoint);
        if (kbCacheIn.functions != null)
            this.functions.addAll(kbCacheIn.functions);
        if (kbCacheIn.instRels != null)
            this.instRels.addAll(kbCacheIn.instRels);
        if (kbCacheIn.instTransRels != null)
            this.instTransRels.addAll(kbCacheIn.instTransRels);
        if (kbCacheIn.instanceOf != null)
            this.instanceOf.putAll(kbCacheIn.instanceOf);
        if (kbCacheIn.instances != null)
            this.instances.putAll(kbCacheIn.instances);
        if (kbCacheIn.insts != null)
            this.insts.addAll(kbCacheIn.insts);
        if (kbCacheIn.parents != null)
            this.parents.putAll(kbCacheIn.parents);
        if (kbCacheIn.predicates != null)
            this.predicates.addAll(kbCacheIn.predicates);
        if (kbCacheIn.relations != null)
            this.relations.addAll(kbCacheIn.relations);
        if (kbCacheIn.signatures != null)
            this.signatures.putAll(kbCacheIn.signatures);
        if (kbCacheIn.transRels != null)
            this.transRels.addAll(kbCacheIn.transRels);
        if (kbCacheIn.valences != null)
            this.valences.putAll(kbCacheIn.valences);
    }

    /***************************************************************
     * Experimental: Utility method to clear the KBcache
     */
    public void clearCaches() {

        children.clear();
        disjoint.clear();
        disjointRelations.clear();
        explicitDisjoint.clear();
        functions.clear();
        instRels.clear();
        instTransRels.clear();
        instanceOf.clear();
        instances.clear();
        insts.clear();
        parents.clear();
        predicates.clear();
        relations.clear();
        signatures.clear();
        transRels.clear();
        valences.clear();
    }

    /**************************************************************
     * An ArrayList utility method
     */
    public int getArity(String rel) {

        if (valences == null) {
            System.err.println("Error in KBcache.getArity(): null valences");
            return 0;
        }
        if (!valences.containsKey(rel)) {
            System.err.println("Error in KBcache.getArity(): " + rel + " not found");
            return 0;
        }
        return valences.get(rel);
    }

    /** ***************************************************************
     * An ArrayList utility method
     */
    private void arrayListReplace(List<String> al, int index, String newEl) {

        if (index > al.size()) {
            System.err.println("Error in KBcache.arrayListReplace(): index " + index +
                    " out of bounds.");
            return;
        }
        al.remove(index);
        al.add(index,newEl);
    }

    /** ***************************************************************
     * Find whether the given child has the given parent for the given
     * transitive relation.
     * @return false if they are equal
     */
    public boolean childOfP(String rel, String parent, String child) {

        if (debug) System.out.println("INFO in KBcache.childOfP(): relation, parent, child: "
                + rel + " " + parent + " " + child);
        if (parent.equals(child)) {
            return false;
        }
        Map<String,Set<String>> childMap = children.get(rel);
        if (childMap == null)
            return false;
        Set<String> childSet = childMap.get(parent);
        if (debug) System.out.println("INFO in KBcache.childOfP(): children of " + parent + " : " + childSet);
        if (childSet == null) {
        	if (debug) System.out.println("INFO in KBcache.childOfP(): null childset for relation, parent, child: "
                + rel + " " + parent + " " + child);
        	return false;
        }
        if (debug) System.out.println("INFO in KBcache.childOfP(): child set contains " + child + " : " + childSet.contains(child));
        return childSet.contains(child);
    }

    /** *************************************************************
     * Returns true if i is an instance of c, else returns false.
     *
     * @param i A String denoting an instance.
     * @param c A String denoting a Class.
     * @return true or false.
     */
    public boolean isInstanceOf(String i, String c) {

        if (instanceOf.containsKey(i)) {
            Set<String> hashSet = instanceOf.get(i);
            if (hashSet == null) {
                System.err.println("Error in KBcache.isInstanceOf(): null result for " + i);
                return false;
            }
            return hashSet.contains(c);
        }
        else
            return false;
    }

    /** ***************************************************************
     * Find whether the given instance has the given parent class.
     * Include paths the have transitive relations between instances such
     * as an Attribute that is a subAttribute of another instance, which
     * in turn then is an instance of the given class.
     * Return false if they are equal.
     */
    public boolean transInstOf(String child, String parent) {

        Set<String> prents = instanceOf.get(child);
        if (prents != null)
            return prents.contains(parent);
        else
            return false;
    }

    /** ***************************************************************
     * Find whether the given class has the given parent class.
     */
    public boolean subclassOf(String child, String parent) {

    	Map<String,Set<String>> prentsForRel = parents.get("subclass");
    	if (prentsForRel != null) {
            Set<String> prents = prentsForRel.get(child);
            if (prents != null)
                return prents.contains(parent);
            else
                return false;
        }
    	return false;
    }

    /** ***************************************************************
     * Find whether the given class is the subAttribute of the given parent class.
     */
    public boolean subAttributeOf(String child, String parent) {

        Map<String,Set<String>> prentsForRel = parents.get("subAttribute");
        if (prentsForRel != null) {
            Set<String> prents = prentsForRel.get(child);
            if (prents != null)
                return prents.contains(parent);
            else
                return false;
        }
        return false;
    }

    /** ***************************************************************
     */
    public void addInstance(String child, String parent) {

        Set<String> is = instances.get(parent);
        if (is == null) {
            is = new HashSet<>();
            instances.put(parent, is);
        }
        is.add(child);
    }

    /** ***************************************************************
     * Add a new instance from an existing one plus a suffix, updating the caches
     */
    public void extendInstance(String term, String suffix) {

        String sep = "__";
        //if (suffix.matches("\\d__.*"))  // variable arity has appended single underscore before arity
        //    sep = "_";
        String newTerm = term + sep + suffix;
        //if (kb.terms.contains(newTerm)) {
        //    System.out.println("Warning in KBcache.extendInstance(): term already exists: " + newTerm);
        //    System.out.println("Warning in KBcache.extendInstance(): sig " + signatures.get(newTerm));
        //}
        kb.terms.add(newTerm);
        kb.capterms.put(newTerm.toUpperCase(),newTerm);
        Set<String> iset = instanceOf.get(term);
        instanceOf.put(newTerm,iset);
        //if (newTerm.endsWith("Fn"))
        //    System.out.println("KBcache.extendInstance(): instance parents of: " + newTerm + " are: " + iset);
        //System.out.println("extendInstance(): new term: " + newTerm + " parents: " + iset);
        relations.add(newTerm);

        // math and logic ops are not transitive
        //transRels = new HashSet<String>();
        // all the transitive relations between instances in the kb
        //instTransRels = new HashSet<String>();

        /** All the cached "parent" relations of all transitive relations
         * meaning the relations between all first arguments and the
         * transitive closure of second arguments.  The external HashMap
         * pairs relation name String keys to values that are the parent
         * relationships.  The interior HashMap is the set of terms and
         * their transitive closure of parents.
         */
        //parents = new HashMap<String, HashMap<String, HashSet<String>>>();

        // all the instances of a class key, including through subrelation
        // and subAttribute
        //instances = new HashMap<>();

        // logic, math op are not transitive so no need to update "children"

        /** Relation name keys and argument types with 0th arg always ""
         * except in the case of Functions where the 0th arg will be the
         * function range.
         * Note that types can be functions, rather than just terms. Note that
         * types (when there's a domainSubclass etc) are designated by a
         * '+' appended to the class name.
         **/
        List<String> sig = signatures.get(term);

        if (sig == null && term != null && term.equals("equal")) {
            sig = new ArrayList<>();
            sig.add("Entity");
            sig.add("Entity");
        }
        if (sig == null)
            System.err.println("Error in KBcache.extendInstance(): no sig for term " + term);
        List<String> newsig = SUMOtoTFAform.relationExtractSigFromName(newTerm);
        signatures.put(newTerm,newsig);
        // The number of arguments to each relation.  Variable arity is -1
        valences.put(newTerm,valences.get(term));
        if (term.endsWith("Fn"))
            functions.add(newTerm);
    }

    /** ***************************************************************
     * Record instances and their explicitly defined parent classes
     */
    public void buildDirectInstances() {

        List<Formula> forms = kb.ask("arg", 0, "instance");
        Formula f;
        String child, parent;
        Map<String,Set<String>> superclasses;
        Set<String> iset;
        for (int i = 0; i < forms.size(); i++) {
            f = forms.get(i);
            child = f.getStringArgument(1);
            parent = f.getStringArgument(2);
            addInstance(child,parent);
            superclasses = parents.get("subclass");
            iset = new HashSet<>();
            if (instanceOf.get(child) != null)
                iset = instanceOf.get(child);
            iset.add(parent);
            if (superclasses != null && superclasses.get(parent) != null)
                iset.addAll(superclasses.get(parent));
            instanceOf.put(child, iset);
        }
    }

    /** ***************************************************************
     * Add transitive relationships to instances in the "instances" map
     */
    public void addTransitiveInstances() {

        Set<String> allInst;
        for (String s : instances.keySet()) {
            allInst = instances.get(s);
            allInst.addAll(getInstancesForType(s));
            instances.put(s,allInst);
        }
    }

    /** ***************************************************************
     */
    public void buildDisjointRelationsMap() {

        Set<String> pairs = new HashSet<>();
        List<Formula> explicitDisjointFormulae = new ArrayList<>();
        explicitDisjointFormulae.addAll(kb.ask("arg", 0, "disjointRelation"));
        String arg1, arg2;
        Set<String> children1, children2;
        for (Formula f : explicitDisjointFormulae) {
            arg1 = f.getStringArgument(1);
            arg2 = f.getStringArgument(2);
            pairs.add(arg1 + "\t" + arg2);
            children1 = getChildRelations(arg1);
            if (children1 == null)
                children1 = new HashSet<>();
            children1.add(arg1);
            children2 = getChildRelations(arg2);
            if (children2 == null)
                children2 = new HashSet<>();
            children2.add(arg2);
            for (String c1 : children1) {
                for (String c2 : children2) {
                    if (!c1.equals(c2))
                        disjointRelations.add(c1 + "\t" + c2);
                }
            }
        }
    }

    /** ***************************************************************
     * build a disjoint-relations-map which were explicitly defined in
     * "partition", "exhaustiveDecomposition", "disjointDecomposition"
     * and "disjoint" expressions;
     */
    public void buildExplicitDisjointMap() {

        if (debug) System.out.println("buildExplicitDisjointMap()");
        List<Formula> explicitDisjointFormulae = new ArrayList<>();
        explicitDisjointFormulae.addAll(kb.ask("arg", 0, "partition"));
        explicitDisjointFormulae.addAll(kb.ask("arg", 0, "disjoint"));
        explicitDisjointFormulae.addAll(kb.ask("arg", 0, "disjointDecomposition"));
        //System.out.println("buildExplicitDisjointMap(): all explicit: " + explicitDisjointFormulae);
        List<String> arguments;
        Set<String> vals;
        for (Formula f : explicitDisjointFormulae) {
            if (debug) System.out.println("buildExplicitDisjointMap(): check formula: " + f.getFormula());
            if (f.car().equals("disjoint"))
                arguments = f.argumentsToArrayListString(1);
            else
                arguments = f.argumentsToArrayListString(2);
            //System.out.println("buildExplicitDisjointMap(): arguments: " + arguments);
            for (String key : arguments) {
                for (String val : arguments) {
                    if (key.equals(val))
                        continue;
                    if (!explicitDisjoint.containsKey(key)) {
                        vals = new HashSet<>();
                        vals.add(val);
                        explicitDisjoint.put(key, vals);
                        //System.out.println("buildExplicitDisjointMap(): " + key + ", " + vals);
                    }
                    else {
                        vals = explicitDisjoint.get(key);
                        vals.add(val);
                        explicitDisjoint.put(key, vals);
                        //System.out.println("buildExplicitDisjointMap(): " + key + ", " + vals);
                    }
                }
            }
        }
    }

    /** ***************************************************************
     * cache the transitive closure of disjoint relations
     */
    public void buildDisjointMap() {

        long t1 = System.currentTimeMillis();
        Set<String> vals, children1, children2;
        for (String p1 : explicitDisjoint.keySet()) {
            vals = explicitDisjoint.get(p1);
            children1 = getChildClasses(p1);
            if (children1 == null)
                children1 = new HashSet<>();
            children1.add(p1);
            for (String p2 : vals) {
                children2 = getChildClasses(p2);
                if (children2 == null)
                    children2 = new HashSet<>();
                children2.add(p2);
                for (String c1 : children1) {
                    for (String c2 : children2) {
                        if (!c1.equals(c2)) {
                            disjoint.add(c1 + "\t" + c2);
                            //System.out.println("buildDisjointMap(): " + c1 + "\t" + c2);
                        }
                    }
                }
            }
        }
        //System.out.println("buildDisjointMap():  " + ((System.currentTimeMillis() - t1) / KButilities.ONE_K.0)
        //        + " seconds to process " + disjoint.size() + " entries");
    }

    /** ***************************************************************
     * check if there are any two types in typeSet are disjoint or not;
     */
    public boolean checkDisjoint(KB kb, Set<String> typeSet) {

        List<String> typeList = new ArrayList<>(typeSet);
        String c1, c2;
        int size = typeList.size();
        for (int i = 0; i < size; i++) {
            c1 = typeList.get(i);
            for (int j = i+1; j < size; j++) {
                c2 = typeList.get(j);
                if (checkDisjoint(kb,c1,c2)) {
                    System.err.println("KBcache.checkDisjoint(): disjoint classes " + c1 + " and " + c2);
                    return true;
                }
            }
        }
        return false;
    }

    /** ***************************************************************
     * check if rel1 and rel2 are disjoint
     * return true if rel1 and rel2 are disjoint; otherwise return false.
     * TODO: can find spurious type conflict when in scope of disjunctions
     */
    public boolean checkDisjoint(KB kb, String c1, String c2) {

        if (!StringUtil.emptyString(c1) && Formula.listP(c1)) {
            Formula c1f = new Formula(c1);
            String pred = c1f.car();
            if (KButilities.isFunction(kb,pred))
                c1 = getRange(pred);
        }
        if (!StringUtil.emptyString(c2) && Formula.listP(c2)) {
            Formula c2f = new Formula(c2);
            String pred = c2f.car();
            if (KButilities.isFunction(kb,pred))
                c2 = getRange(pred);
        }
        if (!StringUtil.emptyString(c1) && !StringUtil.emptyString(c2) && c1.endsWith("+") && !c2.endsWith("+") && !c2.endsWith("Class")) {
            String err = "KBcache.checkDisjoint(): mixing class and instance: " + c1 + ", " + c2;
            errors.add(err);
            System.err.println(err);
            return true;
        }
        if (!StringUtil.emptyString(c1) && !StringUtil.emptyString(c2) && c2.endsWith("+") && !c1.endsWith("+") && !c1.endsWith("Class")) {
            String err = "KBcache.checkDisjoint(): mixing class and instance: " + c1 + ", " + c2;
            System.err.println(err);
            errors.add(err);
            return true;
        }
        if (disjoint.contains(c1 + "\t" + c2) || disjoint.contains(c2 + "\t" + c1)) {
            String err = "KBcache.checkDisjoint(): disjoint terms: " + c1 + ", " + c2;
            System.err.println(err);
            errors.add(err);
            return true;
        }
        else
            return false;
    }

    /** ***************************************************************
     * return true if rel1 and rel2 are explicitly defined as disjoint
     * relations; otherwise return false.
     */
    public boolean isExplicitDisjoint(Map<String, Set<String>> explicitDisjointRelations,
                                      String c1, String c2) {

        if (explicitDisjointRelations.containsKey(c1)) {
            return explicitDisjointRelations.get(c1).contains(c2);
        }
        else if (explicitDisjointRelations.containsKey(c2)) {
            return explicitDisjointRelations.get(c2).contains(c1);
        }
        else
            return false;
    }

    /** ***************************************************************
     * Cache whether a given instance has a given parent class.
     * Include paths the have transitive relations between instances such
     * as an Attribute that is a subAttribute of another instance, which
     * in turn then is an instance of the given class.
     * TODO: make sure that direct instances are recorded too
     */
    public void buildTransInstOf() {

        // Iterate through the temporary list of instances built during creation of the @see children map
        List<Formula> forms, forms2;
        String rel, cl;
        Map<String,Set<String>> prentList, superclasses;
        Set<String> prents, pset, iset, supers;
        for (String child : insts) {
            forms = kb.ask("arg",1,child);
            if (debug) System.out.println("buildTransInstOf(): forms: " + forms);
            for (Formula f : forms) {
                rel = f.getStringArgument(0);
                if (debug) System.out.println("buildTransInstOf(): rel: " + rel);
                if (instTransRels.contains(rel) && intendedTransRels.contains(rel) &&
                        !rel.equals("subclass") && !rel.equals("relatedInternalConcept")) {
                    prentList = parents.get(rel);
                    if (debug) System.out.println("buildTransInstOf(): prentList: " + prentList);
                    if (prentList != null) {
                        prents = prentList.get(f.getStringArgument(1));  // include all parents of the child
                        if (debug) System.out.println("buildTransInstOf(): prents: " + prents);
                        if (prents != null) {
                            for (String p : prents) {
                                forms2 = kb.askWithRestriction(0,"instance",1,p);
                                if (debug) System.out.println("buildTransInstOf(): forms2: " + forms2);
                                for (Formula f2 : forms2) {
                                    cl = f2.getStringArgument(2);
                                    if (debug) System.out.println("buildTransInstOf(): cl: " + cl);
                                    superclasses = parents.get("subclass");
                                    pset = new HashSet<>();
                                    if (instanceOf.get(child) != null)
                                        pset = instanceOf.get(child);
                                    pset.add(cl);
                                    if (superclasses != null && superclasses.get(cl) != null)
                                        pset.addAll(superclasses.get(cl));
                                    instanceOf.put(child, pset);
                                }
                            }
                        }
                    }
                }
                else if (rel.equals("instance")) {
                	cl = f.getStringArgument(2);
                    if (debug) System.out.println("buildTransInstOf(): cl2: " + cl);
                    superclasses = parents.get("subclass");
                    iset = new HashSet<>();
                    if (instanceOf.get(child) != null)
                        iset = instanceOf.get(child);
                    iset.add(cl);
                    if (superclasses != null) {
                        supers = superclasses.get(cl);
                        if (supers != null && !supers.isEmpty())
                            iset.addAll(supers);
                    }
                    instanceOf.put(child, iset);
                }
            }
        }
        buildDirectInstances(); // TODO: This was called already earlier in the buildCaches() parent method
    }

    /** ***************************************************************
     * since domains are collected before we know the instances of
     * VariableArityRelation we need to go back and correct valences
     */
    public void correctValences() {

        Set<String> hs = instances.get("VariableArityRelation");
        if (hs == null)
            return;
        for (String s : hs) {
            valences.put(s,-1);
        }
    }

    /** ***************************************************************
     * @return the most specific parent of a set of classes
     */
    public String mostSpecificParent(Set<String> p1) {

        Map<String,Set<String>> subclasses = children.get("subclass");
        TreeSet<AVPair> countIndex = new TreeSet<>();
        Set<String> classes;
        int count;
        String countString;
        AVPair avp;
        for (String cl : p1) {
            classes = subclasses.get(cl);
            if (classes == null)
                System.err.println("Error in KBcache.mostSpecificParent(): no subclasses for : " + cl);
            else {
                count = classes.size();
                countString = Integer.toString(count);
                countString = StringUtil.fillString(countString, '0', 10, true);
                avp = new AVPair(countString, cl);
                countIndex.add(avp);
            }
        }
        return countIndex.first().value;
    }

    /** ***************************************************************
     * @return the most specific parent of the two parameters or null if
     * there is no common parent.  TODO: Take into
     * account that there are instances, classes, relations, and attributes,
     */
    public String getCommonParent(String t1, String t2) {

        Set<String> p1 = new HashSet<>();
        Set<String> p2 = new HashSet<>();
        if (kb.isInstance(t1)) {
            Set<String> temp = getParentClassesOfInstance(t1);
            if (temp != null)
                p1.addAll(temp);
        }
        else {
            Set<String> temp = getParentClasses(t1);
            if (temp != null)
                p1.addAll(temp);
        }
        if (kb.isInstance(t2)) {
            Set<String> temp = getParentClassesOfInstance(t2);
            if (temp != null)
                p2.addAll(temp);
        }
        else {
            Set<String> temp = getParentClasses(t2);
            if (temp != null)
                p2.addAll(temp);
        }
        p1.retainAll(p2);
        if (p1.isEmpty())
            return null;
        if (p1.size() == 1)
            return p1.iterator().next();

        return mostSpecificParent(p1);
    }

    /** ***************************************************************
     */
    public String getCommonChild(Set<String> t2) {

        String common = "Entity";
        System.out.println("types " + t2);
        for (String c1 : t2) {
            if (debug) System.out.println("term depth " + c1 + " : " + kb.termDepth(c1));
            if (debug) System.out.println("term depth " + common + " : " + kb.termDepth(common));
            if (kb.compareTermDepth(c1,common) > 0)
                common = c1;
        }
        return common;
    }

    /** ***************************************************************
     * return parent classes for the given cl from subclass expressions.
     */
    public Set<String> getParentClasses(String cl) {

        Map<String,Set<String>> ps = parents.get("subclass");
        if (ps != null)
            return ps.get(cl);
        else
            return null;
    }

    /** ***************************************************************
     * return child relations for the given rel from subrelation expressions.
     */
    public Set<String> getChildRelations(String rel) {

        Map<String,Set<String>> ps = children.get("subrelation");
        if (ps != null)
            return ps.get(rel);
        else
            return null;
    }

    /** ***************************************************************
     * return child classes for the given cl from subclass expressions.
     */
    public Set<String> getChildClasses(String cl) {

        Map<String,Set<String>> ps = children.get("subclass");
        if (ps != null)
            return ps.get(cl);
        else
            return null;
    }

    /** ***************************************************************
     * return child term for the given cl from rel expressions.
     */
    public Set<String> getChildTerms(String cl, String rel) {

        Map<String,Set<String>> ps = children.get(rel);
        if (ps != null)
            return ps.get(cl);
        else
            return null;
    }

    /** ***************************************************************
     * return child classes for the given cl from subclass expressions.
     */
    public Set<String> getChildInstances(String cl) {

        Set<String> result = new HashSet<>();
        Map<String,Set<String>> ps = children.get("subclass");
        Set<String> insts;
        if (ps != null && ps.values() != null) {
            for (String cc : ps.get(cl)) {
                insts = getInstancesForType(cc);
                if (insts != null)
                    result.addAll(insts);
            }
            return result;
        }
        else
            return null;
    }

    /** ***************************************************************
     * return classes for the given instance cl.
     *
     * For example, if we know (instance UnitedStates Nation), then
     * getParentClassesOfInstances(UnitedStates) returns Nation and its
     * super classes from subclass expressions.
     */
    public Set<String> getParentClassesOfInstance(String cl) {

        Set<String> ps = instanceOf.get(cl);
        if (ps != null)
            return ps;
        else
            return new HashSet<>();
    }

    /** ***************************************************************
     * Get all instances for the given input class
     *
     * For example, given the class "Nation", getInstancesForType(Nation)
     * returns all instances, like "America", "Austria", "Albania", etc.
     *
     * Follow instances through transitive relations if applicable from
     * the set of [subAttribute, subrelation].
     *
     * TODO: do we need a DownwardHeritableRelation so that this
     * list doesn't need to be hardcoded?
     */
    public Set<String> getInstancesForType(String cl) {

        if (debug) System.out.println("getInstancesForType(): " + cl);
        if (cl.equals("Class"))
            return (HashSet<String>) getChildClasses("Entity");
        Set<String> instancesForType = new HashSet<>();
        Map<String,Set<String>> ps = children.get("subclass");
        Set<String> classes = new HashSet<>();
        if (ps != null)
            classes = ps.get(cl);
        if (debug) System.out.println("getInstancesForType(): subclasses of " + cl + " : " + classes);
        if (classes == null)
            classes = new HashSet<>();
        classes.add(cl);
        Set<String> is;
        for (String c : classes) {
            is = instances.get(c);
            if (debug) System.out.println("getInstancesForType(): instances of " + c + " : " + is);
            if (is != null)
                instancesForType.addAll(is);
        }
        if (debug) System.out.println("getInstancesForType(): " + instancesForType);
        Set<String> instancesForType2 = new HashSet<>();
        Map<String,Set<String>> attr = children.get("subAttribute");
        Map<String,Set<String>> arel = children.get("subrelation");
        Set<String> temp;
        for (String i : instancesForType) {
            if (attr != null) {
                temp = attr.get(i);
                if (temp != null)
                    instancesForType2.addAll(temp);
            }
            if (arel != null) {
                temp = arel.get(i);
                if (temp != null)
                    instancesForType2.addAll(temp);
            }
        }
        instancesForType2.addAll(instancesForType);
        if (debug) System.out.println("getInstancesForType(): 2: " + instancesForType2);
        return instancesForType2;
    }

    /** ***************************************************************
     */
    public List<String> getSignature(String rel) {

        return signatures.get(rel);
    }

    /** ***************************************************************
     * Get the range (return type) of a Function.
     * @return null if argument is not a function
     */
    public String getRange(String f) {

        if (!kb.isFunction(f))
            return null;
        List<String> sig = getSignature(f);
        if (sig == null || sig.isEmpty())
            return null;
        return sig.get(0);
    }

    /** ***************************************************************
     * Get the HashSet of the given arguments from an ArrayList of Formulas.
     */
    public static Set<String> collectArgFromFormulas(int arg, List<Formula> forms) {

        Set<String> subs = new HashSet<>();
        String sub;
        for (Formula f : forms) {
            sub = f.getStringArgument(arg);
            //System.out.println("collectArgFromFormulas(): " + f + "\n" + arg + "\n" + sub);
            subs.add(sub);
        }
        //System.out.println("collectArgFromFormulas(): subs: " + subs);
        return subs;
    }

    /** ***************************************************************
     * Do a proper search for relations (including Functions), utilizing
     * the formal definitions, rather than the convention of initial
     * lower case letter.  This means getting any instance of Relation
     * tracing back through subclasses as well.
     */
    public void buildTransitiveRelationsSet() {

        if (debug) System.out.println("INFO in KBcache.buildTransitiveRelationsSet(): begin");
        Set<String> rels = new HashSet<>();
        rels.add("TransitiveRelation");
        Set<String> relSubs;
        List<Formula> forms;
        while (!rels.isEmpty()) {
            relSubs = new HashSet<>();
            for (String rel : rels) {
                forms = kb.askWithRestriction(0, "subclass", 2, rel);
                if (forms != null && !forms.isEmpty()) {
                    if (debug) System.out.println("INFO in KBcache.buildTransitiveRelationsSet(): subclasses: " + forms);
                    relSubs.addAll(collectArgFromFormulas(1,forms));
                }
                else
                    if (debug) System.out.println("INFO in KBcache.buildTransitiveRelationsSet(): no subclasses for : " + rels);
                forms = kb.askWithRestriction(0,"instance",2,rel);
                if (forms != null && !forms.isEmpty())
                    transRels.addAll(collectArgFromFormulas(1,forms));
                forms = kb.askWithRestriction(0,"subrelation",2,rel);
                if (forms != null && !forms.isEmpty())
                    transRels.addAll(collectArgFromFormulas(1,forms));
            }
            rels.clear();
            rels.addAll(relSubs);
        }
    }

    /** ***************************************************************
     * Do a proper search for relations (including Functions), utilizing
     * the formal definitions, rather than the convention of initial
     * lower case letter.  This means getting any instance of Relation
     * tracing back through subclasses as well.
     */
    public void buildRelationsSet() {

        Set<String> rels = new HashSet<>();
        rels.add("Relation");
        List<Formula> forms;
        Set<String> relSubs;
        while (!rels.isEmpty()) {
            if (debug) System.out.println("INFO in KBcache.buildRelationsSet(): rels: " + rels);
            relSubs = new HashSet<>();
            for (String rel : rels) {
                if (debug) System.out.println("INFO in KBcache.buildRelationsSet(): rel: " + rel);
                forms = kb.askWithRestriction(0,"subclass",2,rel);
                if (debug) System.out.println("INFO in KBcache.buildRelationsSet(): forms1: " + forms);
                if (forms != null)
                    relSubs.addAll(collectArgFromFormulas(1,forms));
                forms = kb.askWithRestriction(0,"instance",2,rel);
                //System.out.println("INFO in KBcache.buildRelationsSet(): forms2: " + forms);
                if (forms != null) {
                    relations.addAll(collectArgFromFormulas(1,forms));
                    relSubs.addAll(collectArgFromFormulas(1,forms));
                }
                forms = kb.askWithRestriction(0,"subrelation",2,rel);
                //System.out.println("INFO in KBcache.buildRelationsSet(): forms3: " + forms);
                if (forms != null) {
                    relations.addAll(collectArgFromFormulas(1,forms));
                    relSubs.addAll(collectArgFromFormulas(1,forms));
                }
            }
            if (debug) System.out.println("INFO in KBcache.buildRelationsSet(): relSubs: " + relSubs);
            rels.clear();
            rels.addAll(relSubs);
        }
    }

    /** ***************************************************************
     */
    public void buildFunctionsSet() {

        for (String s : relations)
            if (isInstanceOf(s,"Function")) // can't use isFunction since that checks KBcache.functions
                functions.add(s);
            else
                predicates.add(s);
    }

    /** ***************************************************************
     * Find the parent "roots" of any transitive relation - terms that
     * appear only as argument 2
     */
    private Set<String> findRoots(String rel) {

        Set<String> result = new HashSet<>();
        List<Formula> forms = kb.ask("arg",0,rel);
        Set<String> arg1s = collectArgFromFormulas(1,forms);
        Set<String> arg2s = collectArgFromFormulas(2,forms);
        arg2s.removeAll(arg1s);
        result.addAll(arg2s);
        //System.out.println("findRoots(): rel, roots: " + rel + ":" + result);
        return result;
    }

    /** ***************************************************************
     * Find the child "roots" of any transitive relation - terms that
     * appear only as argument 1
     */
    private Set<String> findLeaves(String rel) {

        Set<String> result = new HashSet<>();
        List<Formula> forms = kb.ask("arg",0,rel);
        Set<String> arg1s = collectArgFromFormulas(1,forms);
        Set<String> arg2s = collectArgFromFormulas(2,forms);
        arg1s.removeAll(arg2s);
        result.addAll(arg1s);
        return result;
    }

    /** ***************************************************************
     * Build "parent" relations based on breadth first search algorithm.
     */
    private void breadthFirstBuildParents(String root, String rel) {

        Map<String,Set<String>> relParents = parents.get(rel);
        if (relParents == null) {
            System.err.println("Error in KBcache.breadthFirstBuildParents(): no relation " + rel);
            return;
        }
        int threshold = 10;      // maximum time that a term can be traversed in breadthFirstBuildParents()
        Map<String, Integer> appearanceCount = new HashMap<>();  // for each term, we count how many times it has been traversed
        Deque<String> Q = new ArrayDeque<>();
        Q.add(root);
        String t;
        List<Formula> forms;
        Set<String> newParents, oldParents, newTermParents;
        while (!Q.isEmpty()) {
            t = Q.remove();
            //System.out.println("visiting " + t);
            forms = kb.askWithRestriction(0,rel,2,t);
            if (forms != null) {
                for (String newTerm: collectArgFromFormulas(1,forms)) {
                    newParents = new HashSet<>();
                    oldParents = relParents.get(t);
                    if (oldParents == null) {
                        oldParents = new HashSet<>();
                        relParents.put(t, oldParents);
                    }
                    newParents.addAll(oldParents);
                    newParents.add(t);
                    newTermParents = relParents.get(newTerm);
                    if (newTermParents != null)
                        newParents.addAll(newTermParents);
                    relParents.put(newTerm, newParents);

                    if (appearanceCount.get(newTerm) == null) {
                        appearanceCount.put(newTerm, 1);
                        Q.addFirst(newTerm);
                    }
                    else if (appearanceCount.get(newTerm) <= threshold) {
                        appearanceCount.put(newTerm, appearanceCount.get(newTerm)+1);
                        Q.addFirst(newTerm);
                    }
                }
            }
        }
    }

    /** ***************************************************************
     * Build "children" relations based on breadth first search algorithm.
     * Note that this routine expects to build "up" from the leaves.
     */
    private void breadthFirstBuildChildren(String leaf, String rel) {

        Map<String,Set<String>> relChildren = children.get(rel);
        if (relChildren == null) {
            System.err.println("Error in KBcache.breadthFirstBuildChildren(): no relation " + rel);
            return;
        }
        //if (debug) System.out.println("INFO in KBcache.breadthFirstBuildChildren(): trying relation " + rel);
        Deque<String> Q = new ArrayDeque<>();
        Set<String> V = new HashSet<>();
        Q.add(leaf);
        V.add(leaf);
        String child;
        List<Formula> forms;
        Set<String> newChildren, oldChildren, newTermChildren;
        while (!Q.isEmpty()) {
            child = Q.remove();
            //if (debug) System.out.println("visiting " + child);
            forms = kb.askWithRestriction(0,rel,1,child);
            if (debug) System.out.println("forms " + forms);
            if (forms != null) {
                for (String newTerm : collectArgFromFormulas(2,forms)) {
                    //if (debug && newTerm.indexOf("RealNumber") > -1)
                    //    System.out.println("visiting parent  " + newTerm);
                    newChildren = new HashSet<>();
                    oldChildren = relChildren.get(child);
                    //if (debug) System.out.println("existing children of " + child +  ": " + oldChildren);
                    if (oldChildren == null) {
                        oldChildren = new HashSet<>();
                        relChildren.put(child, oldChildren);
                    }
                    newChildren.addAll(oldChildren);
                    newChildren.add(child);
                    newTermChildren = relChildren.get(newTerm);
                    if (newTermChildren != null)
                        newChildren.addAll(newTermChildren);
                    relChildren.put(newTerm, newChildren);
                    //if (debug && newTerm.indexOf("RealNumber") > -1)
                    //    System.out.println("new children of  " + newTerm +  ": " + newChildren);
                    if (!V.contains(newTerm)) { // this is a DAG, not a tree, so we may have to visit nodes more than once
                        V.add(newTerm);
                        Q.addFirst(newTerm);
                    }
                }
            }
        }
        insts.addAll(relChildren.keySet());
    }

    /** ***************************************************************
     */
    private Set<String> visited = new HashSet<>();

    /** ***************************************************************
     * Build "children" relations recursively from the root
     */
    private Set<String> buildChildrenNew(String term, String rel) {

        if (debug) System.out.println("buildChildrenNew(): looking at " + term + " with relation " + rel);
        if (children.get(rel) == null)
            children.put(rel,new HashMap<>());
        Map<String,Set<String>> allChildren = children.get(rel);
        if (visited.contains(term))
            return allChildren.get(term);
        visited.add(term);
        if (debug) System.out.println("buildChildrenNew(): " + kb.ask("arg",0,"subrelation"));
        List<Formula> forms = kb.askWithRestriction(0,rel,2,term); // argument 2 is the "parent" in any binary relation
        if (debug) System.out.println("buildChildrenNew(): forms  " + forms);
        if (forms == null || forms.isEmpty())
            return new HashSet<>();
        Set<String> collectedChildren = new HashSet<>();
        String newTerm;
        Set<String> lclChildren;
        for (Formula f : forms) {
            if (f.isCached() || StringUtil.emptyString(f.sourceFile))
                continue;
            //System.out.println(f.sourceFile);
            newTerm = f.getStringArgument(1);// argument 1 is the "child" in any binary relation
            if (debug) System.out.println("buildChildrenNew(): new term " + newTerm);
            lclChildren = buildChildrenNew(newTerm, rel);
            if (debug) System.out.println("buildChildrenNew(): children of " + newTerm + " are " + lclChildren);
            if (allChildren.containsKey(newTerm) && allChildren.get(newTerm) != null)
                lclChildren.addAll(allChildren.get(newTerm));
            allChildren.put(newTerm, lclChildren);
            if (lclChildren != null)
                collectedChildren.addAll(lclChildren);
            collectedChildren.add(newTerm);
        }
        //collectedChildren.add(term);
        if (debug) System.out.println("buildChildrenNew(): return  " + term + " with " + collectedChildren);
        if (debug) System.out.println();
        return collectedChildren;
    }

    /** ***************************************************************
     * Find all instances
     */
    public void buildInsts() {

        Set<String> rels = new HashSet<>(50,LOAD_FACTOR);
        rels.add("instance");
        rels.add("subAttribute");
        rels.add("subField");
        List<Formula> forms;
        String arg;
        for (String r : rels) {
            //System.out.println("buildInsts(): rel:  " + r);
            forms = kb.ask("arg",0,r);
            for (Formula f : forms) {
                //System.out.println("buildInsts(): form:  " + f);
                arg = f.getStringArgument(1);
                insts.add(arg);
            }
        }
    }

    /** ***************************************************************
     * For each transitive relation, find its transitive closure.  If
     * rel is transitive, and (rel A B) and (rel B C) then the entry for
     * rel is a HashMap where the key A has value ArrayList of {B,C}.
     */
    public void buildParents() {

        if (debug) System.out.println("INFO in KBcache.buildParents():");
        Map<String,Set<String>> value;
        Set<String> roots;
        for (String rel : transRels) {
            value = new HashMap<>(50, LOAD_FACTOR);
            roots = findRoots(rel);
            if (debug) System.out.println("INFO in KBcache.buildParents(): roots for rel: " +
                    rel + "\n" + roots);
            parents.put(rel, value);
            for(String root : roots)
                breadthFirstBuildParents(root, rel);
        }
    }

    /** ***************************************************************
     * For each transitive relation, find its transitive closure.  If
     * rel is transitive, and (rel A B) and (rel B C) then the entry for
     * rel is a HashMap where the key A has value ArrayList of {B,C}. Note
     * that this routine builds "up" from the leaves
     */
    public void buildChildren() {

        if (debug) System.out.println("INFO in KBcache.buildChildren()");
        Map<String,Set<String>> value;
        Set<String> roots, c;
        for (String rel : transRels) {
            if (debug) System.out.println("INFO in KBcache.buildChildren(): rel: " + rel);
            value = new HashMap<>(50,LOAD_FACTOR);
            roots = findRoots(rel);
            if (debug) System.out.println("INFO in KBcache.buildChildren(): roots: " + roots);
            children.put(rel, value);
            for (String root : roots) {
                visited.clear(); // reset the visited list for each new root and relation
                c = buildChildrenNew(root, rel);
                if (c != null)
                    value.put(root,c);
                insts.add(root);// TODO: shouldn't need this
            }
        }
    }

    /** ***************************************************************
     * Fill an array of String with the specified String up to but
     * not including the index, starting from the 1st argument and
     * ignoring the 0th argument.
     */
    private static void fillArray(String st, String[] ar, int start, int end) {

        for (int i = start; i < end; i++)
            if (StringUtil.emptyString(ar[i]))
                ar[i] = st;
    }

    /** ***************************************************************
     * Fill an array of String with the specified String up to but
     * not including the index, starting from the end of the array
     */
    private static void fillArrayList(String st, List<String> ar, int start, int end) {

        for (int i = start; i < end; i++)
            if (i > ar.size()-1 || StringUtil.emptyString(ar.get(i)))
                ar.add(st);
    }

    /** ***************************************************************
     * Build the argument type list for every relation. If the argument
     * is a domain subclass, append a "+" to the argument type.  If
     * no domain is defined for the given relation and argument position,
     * inherit it from the parent.  If there is no argument type, send
     * an error to the Sigma error list.
     * Relation name keys and argument types with 0th arg always "" except
     *   for functions which will have the range type as their 0th argument
     * public Map&lt;String,List&lt;String&gt;&gt; signatures = new HashMap&lt;&gt;();
     */
    public void collectDomains() {

        if (debug) System.out.println("INFO in KBcache.collectDomains(): relations " + relations);
        String[] domainArray;
        int maxIndex, arg;
        List<Formula> forms;
        List<String> domains;
        Formula form;
        String arg2, type;
        for (String rel : relations) {
            domainArray = new String[Formula.MAX_PREDICATE_ARITY];
            maxIndex = 0;
            domainArray[0] = "";
            forms = kb.askWithRestriction(0,"domain",1,rel);
            if (debug) System.out.println("INFO in KBcache.collectDomains(): forms " + forms);
            if (forms != null) {
                for (int i = 0; i < forms.size(); i++) {
                    form = forms.get(i);
                    if (debug) System.out.println("INFO in KBcache.collectDomains(): form " + form);
                    arg2 = form.getStringArgument(2);
                    if (StringUtil.emptyString(arg2) || !StringUtil.isNumeric(arg2)) {
                        System.err.println("Error in KBcache.collectDomains(): arg2 not a number in:  " + form);
                        continue;
                    }
                    arg = Integer.parseInt(form.getStringArgument(2));
                    type = form.getStringArgument(3);
                    domainArray[arg] = type;
                    if (arg > maxIndex)
                        maxIndex = arg;
                }
            }

            forms = kb.askWithRestriction(0,"domainSubclass",1,rel);
            if (forms != null) {
                for (int i = 0; i < forms.size(); i++) {
                    form = forms.get(i);
                    arg = Integer.parseInt(form.getStringArgument(2));
                    type = form.getStringArgument(3);
                    domainArray[arg] = type + "+";
                    if (arg > maxIndex)
                        maxIndex = arg;
                }
            }

            forms = kb.askWithRestriction(0,"range",1,rel);
            if (forms != null) {
                if (forms.size() > 1)
                    System.out.println("Warning in KBcache.collectDomains(): more than one range statement" + forms);
                for (int i = 0; i < forms.size(); i++) {
                    form = forms.get(i);
                    type = form.getStringArgument(2);
                    domainArray[0] = type;
                }
            }

            forms = kb.askWithRestriction(0,"rangeSubclass",1,rel);
            if (forms != null) {
                if (forms.size() > 1)
                    System.out.println("Warning in KBcache.collectDomains(): more than one rangeSubclass statement" + forms);
                for (int i = 0; i < forms.size(); i++) {
                    form = forms.get(i);
                    type = form.getStringArgument(2);
                    domainArray[0] = type + "+";
                }
            }

            fillArray("Entity",domainArray,1,maxIndex); // set default arg type of Entity in case user forgets
            domains = new ArrayList<>();
            for (int i = 0; i <= maxIndex; i++)
                domains.add(domainArray[i]);
            if (debug) System.out.println("INFO in KBcache.collectDomains(): rel: " + rel);
            if (debug) System.out.println("INFO in KBcache.collectDomains(): domains: " + domains);
            signatures.put(rel,domains);
            valences.put(rel, maxIndex);
        }
        inheritDomains();
    }

    /** ***************************************************************
     * Note that this routine forces child relations to have arguments
     * that are the same or more specific than their parent relations.
     */
    private void breadthFirstInheritDomains(String root) {

        String rel = "subrelation";
        Map<String,Set<String>> relParents = parents.get("subrelation");
        if (relParents == null) {
            System.err.println("Error in KBcache.breadthFirstInheritDomains(): no parents using relation subrelation");
            System.err.println(parents);
            return;
        }
        Deque<String> Q = new ArrayDeque<>();
        Set<String> V = new HashSet<>();
        Q.add(root);
        V.add(root);
        String t, childArgType, parentArgType;
        List<String> tdomains, newDomains;
        List<Formula> forms;
        while (!Q.isEmpty()) {
            t = Q.remove();
            tdomains = signatures.get(t);
            forms = kb.askWithRestriction(0,rel,2,t);
            if (forms != null) {
                for (String newTerm : collectArgFromFormulas(1,forms)) {
                    newDomains = signatures.get(newTerm);
                    if (debug) System.out.println("KBcache.breadthFirstInheritDomains(); newDomains: " + newDomains);
                    if (valences.get(t) == null) {
                        System.err.println("Error in KBcache.breadthFirstInheritDomains(): no valence for " + t);
                        continue;
                    }
                    else if (valences.get(newTerm) == null || valences.get(newTerm) < valences.get(t)) {
                        fillArrayList("Entity",newDomains,valences.get(newTerm)+1,valences.get(t)+1);
                        if (debug) System.out.println("KBcache.breadthFirstInheritDomains(); valences: " + valences.get(t));
                        valences.put(newTerm, valences.get(t));
                    }
                    for (int i = 1; i <= valences.get(t); i++) {
                        childArgType = newDomains.get(i);
                        parentArgType = tdomains.get(i);
                        if (debug) System.out.println("KBcache.breadthFirstInheritDomains(); childArgType: " + childArgType);
                        if (debug) System.out.println("KBcache.breadthFirstInheritDomains(); parentArgType: " + parentArgType);
                        // If child-relation does not have definition of argument-type, we use parent-relation's argument-type
                        // TODO: if parent-relation does not have definition of argument-type, we continue to find its parent until we find the definition of argument-type
                        if (kb.askWithTwoRestrictions(0, "domain", 1, newTerm, 3, childArgType).isEmpty()) {
                            arrayListReplace(newDomains,i,parentArgType);
                        }
                    }
                    if (!V.contains(newTerm)) {
                        V.add(newTerm);
                        Q.addFirst(newTerm);
                    }
                }
            }
        }
    }

    /** *************************************************************
     * Delete and writes the cache .kif file then call addConstituent() so
     * that the file can be processed and loaded by the inference engine.
     * @deprecated This is not needed since we have storeCacheAsFormulas()
     */
    @Deprecated
    public void writeCacheFile() {

        long millis = System.currentTimeMillis();
        try {
            File dir = new File(KBmanager.getMgr().getPref("kbDir"));
            File f = new File(dir, (kb.name + _cacheFileSuffix));
            System.out.println("INFO in KBcache.writeCacheFile(): " + f.getName());
            if (f.exists())
                f.delete();
            String filename = f.getCanonicalPath();
            try (FileWriter fw = new FileWriter(f, true)) {
                Map<String, Set<String>> valSet;
                Set<String> prents, vSet;
                String tuple;
                for (String rel : parents.keySet()) {
                    valSet = parents.get(rel);
                    for (String child : valSet.keySet()) {
                        prents = valSet.get(child);
                        for (String parent : prents) {
                            tuple = "(" + rel + " " + child + " " + parent + ")";
                            if (!kb.formulaMap.containsKey(tuple)) {
                                fw.write(tuple + System.getProperty("line.separator"));
                            }
                        }
                    }
                }

                for (String inst : instanceOf.keySet()) {
                    vSet = instanceOf.get(inst);
                    for (String parent : vSet) {
                        tuple = "(instance " + inst + " " + parent + ")";
                        if (!kb.formulaMap.containsKey(tuple)) {
                            fw.write(tuple + System.getProperty("line.separator"));
                        }
                    }
                }
            }
            System.out.printf("KBcache.writeCacheFile(): done writing cache file, in %d seconds%n", (System.currentTimeMillis() - millis) / KButilities.ONE_K);
            millis = System.currentTimeMillis();
            kb.constituents.remove(filename);
            kb.addConstituent(filename);
            System.out.printf("KBcache.writeCacheFile(): add cache file, in %d seconds%n", (System.currentTimeMillis() - millis) / KButilities.ONE_K);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** *************************************************************
     * Add the cached formulas as though they were from a file.
     * There's no need to write the file since if it hasn't been created,
     * it must be created new.  If it has been created already, then it
     * will be written out as part of the serialized info.
     */
    public void storeCacheAsFormulas() {

        StringBuilder sb = new StringBuilder();
        System.out.println("KBcache.storeCacheAsFormulas()");
        long cacheCount = 0;
        KIF kif = new KIF();
        kif.filename = kb.name + _cacheFileSuffix;
        long millis = System.currentTimeMillis();
        Map<String,Set<String>> valSet;
        Set<String> prents, vSet;
        String tuple;
        for (String rel : parents.keySet()) {
            valSet = parents.get(rel);
            for (String child : valSet.keySet()) {
                prents = valSet.get(child);
                for (String parent : prents) {
                    tuple = "(" + rel + " " + child + " " + parent + ")";
                    if (!kb.formulaMap.containsKey(tuple)) {
                        sb.append(tuple);
                        cacheCount++;
                    }
                }
            }
        }
        System.out.println("KBcache.storeCacheAsFormulas(): finished relations, starting instances");
        for (String inst : instanceOf.keySet()) {
            vSet = instanceOf.get(inst);
            for (String parent : vSet) {
                tuple = "(instance " + inst + " " + parent + ")";
                if (!kb.formulaMap.containsKey(tuple)) {
                    sb.append(tuple);
                    cacheCount++;
                }
            }
        }
        try (Reader r = new StringReader(sb.toString())) {
            kif.parse(r);
        } catch (IOException ioe) {
            System.err.println("Error in KBcache.storeCacheAsFormulas(): " + ioe.getMessage());
            return;
        }
        if (KBmanager.getMgr().getPref("cache").equals("yes"))
            kb.addConstituentInfo(kif);

        System.out.printf("KBcache.storeCacheAsFormulas(): done creating cache formulas, in %d m/s%n", (System.currentTimeMillis() - millis));
        System.out.printf("KBcache.storeCacheAsFormulas(): cached statements: %d%n", cacheCount);
    }

    /** ***************************************************************
     * Find domain and domainSubclass definitions that impact a child
     * relation.  If the type of an argument is less specific than
     * the same type of a parent's argument, use that of the parent.
     */
    public void inheritDomains() {

        Set<String> roots = findRoots("subrelation");
        for (String root : roots) {
            breadthFirstInheritDomains(root);
        }
    }

    /** ***************************************************************
     * Compile the set of transitive relations that are between instances
     */
    public void buildInstTransRels() {

        List<String> sig;
        String signatureElement;
        boolean instrel;
        for (String rel : transRels) {
            //System.out.println("KBcache.buildInstTransRels(): -------------------: " + rel);
            instrel = true;
            sig = signatures.get(rel);
            if (sig == null)
                System.err.println("Error in KBcache.buildInstTransRels(): Error " + rel + " not found.");
            else {
                for (int i = 0; i < sig.size(); i++) {
                    signatureElement = sig.get(i);
                    //System.out.println("KBcache.buildInstTransRels(): " + signatureElement);
                    if (signatureElement.endsWith("+") || signatureElement.equals("Class")) {
                        //System.out.println("KBcache.buildInstTransRels(): " + rel + " is between classes");
                        instrel = false;
                        break;
                    }
                }
                if (instrel)
                    instTransRels.add(rel);
            }
        }
    }

    /** ***************************************************************
     * Main entry point for the class.
     */
    public void buildCaches() {

        clearCaches(); // ensure a clean slate
//        if (!SUMOKBtoTPTPKB.rapidParsing)
            _buildCaches();
//        else
//            _t_buildCaches();
    }

    /** ***************************************************************
     * Threaded version.
     * Not much help timewise
     */
    private void _t_buildCaches() {

        Future<?> future;
        List<Future<?>> futures = new ArrayList<>();

        long startMillis = System.currentTimeMillis();
        if (debug) System.out.println("INFO in KBcache.buildCaches()");

        Runnable r = () -> {
            long millis = System.currentTimeMillis();
            buildInsts();
            System.out.printf("KBcache.buildCaches(): buildInsts:                  %d m/s%n", (System.currentTimeMillis() - millis));
            millis = System.currentTimeMillis();
            buildRelationsSet();
            System.out.printf("KBcache.buildCaches(): buildRelationsSet:           %d m/s%n", (System.currentTimeMillis() - millis));
            millis = System.currentTimeMillis();
            buildTransitiveRelationsSet();
            System.out.printf("KBcache.buildCaches(): buildTransitiveRelationsSet: %d m/s%n", (System.currentTimeMillis() - millis));
            millis = System.currentTimeMillis();
            buildParents();
            System.out.printf("KBcache.buildCaches(): buildParents:                %d m/s%n", (System.currentTimeMillis() - millis));
            millis = System.currentTimeMillis();
            buildChildren(); // note that buildTransInstOf() depends on this
            System.out.printf("KBcache.buildCaches(): buildChildren:               %d m/s%n", (System.currentTimeMillis() - millis));
            millis = System.currentTimeMillis();
            collectDomains();  // note that buildInstTransRels() depends on this
            System.out.printf("KBcache.buildCaches(): collectDomains:              %d m/s%n", (System.currentTimeMillis() - millis));
            millis = System.currentTimeMillis();
            buildInstTransRels();
            System.out.printf("KBcache.buildCaches(): buildInstTransRels:          %d m/s%n", (System.currentTimeMillis() - millis));
            millis = System.currentTimeMillis();
            buildDirectInstances();
            System.out.printf("KBcache.buildCaches(): buildDirectInstances:        %d m/s%n", (System.currentTimeMillis() - millis));
            millis = System.currentTimeMillis();
            addTransitiveInstances();
            System.out.printf("KBcache.buildCaches(): addTransitiveInstances:      %d m/s%n", (System.currentTimeMillis() - millis));
            millis = System.currentTimeMillis();
            buildTransInstOf();
            correctValences(); // correct VariableArityRelation valences
            System.out.printf("KBcache.buildCaches(): buildTransInstOf:            %d m/s%n", (System.currentTimeMillis() - millis));
            millis = System.currentTimeMillis();
            buildExplicitDisjointMap(); // find relations under partition definition
            System.out.printf("KBcache.buildCaches(): buildExplicitDisjointMap:    %d m/s%n", (System.currentTimeMillis() - millis));
            if (KBmanager.getMgr().getPref("cacheDisjoint").equals("true")) {
                millis = System.currentTimeMillis();
//            buildExplicitDisjointMap();
                buildDisjointMap();
                System.out.printf("KBcache.buildCaches(): buildDisjointMap:            %d m/s%n", (System.currentTimeMillis() - millis));
            }
            millis = System.currentTimeMillis();
            buildFunctionsSet();
            System.out.printf("KBcache.buildCaches(): buildFunctionsSet:           %d m/s%n", (System.currentTimeMillis() - millis));
            millis = System.currentTimeMillis();
            storeCacheAsFormulas();
            System.out.printf("KBcache.buildCaches(): store cached formulas:       %d m/s%n", (System.currentTimeMillis() - millis));
        }; // end Runnable
        future = KButilities.EXECUTOR_SERVICE.submit(r);
        futures.add(future);

        for (Future<?> f : futures)
            try {
                f.get(); // waits for task completion
            } catch (InterruptedException | ExecutionException ex) {
                System.err.printf("Error in KBcache.buildCaches(): %s%n", ex.getMessage());
                ex.printStackTrace();
            }

        System.out.printf("INFO in KBcache.buildCaches(): size: %d%n", instanceOf.keySet().size());
        System.out.printf("KBcache.buildCaches():                              %d total seconds%n", (System.currentTimeMillis() - startMillis) / KButilities.ONE_K);
        initialized = true;
    }

    /** ***************************************************************
     * Conventional/sequential version
     */
    private void _buildCaches() {

        long millis = System.currentTimeMillis();
        long startMillis = millis;
        if (debug) System.out.println("INFO in KBcache.buildCaches()");
        buildInsts();
        System.out.printf("KBcache.buildCaches(): buildInsts:                  %d m/s%n", (System.currentTimeMillis() - millis));
        millis = System.currentTimeMillis();
        buildRelationsSet();
        System.out.printf("KBcache.buildCaches(): buildRelationsSet:           %d m/s%n", (System.currentTimeMillis() - millis));
        millis = System.currentTimeMillis();
        buildTransitiveRelationsSet();
        System.out.printf("KBcache.buildCaches(): buildTransitiveRelationsSet: %d m/s%n", (System.currentTimeMillis() - millis));
        millis = System.currentTimeMillis();
        buildParents();
        System.out.printf("KBcache.buildCaches(): buildParents:                %d m/s%n", (System.currentTimeMillis() - millis));
        millis = System.currentTimeMillis();
        buildChildren(); // note that buildTransInstOf() depends on this
        System.out.printf("KBcache.buildCaches(): buildChildren:               %d m/s%n", (System.currentTimeMillis() - millis));
        millis = System.currentTimeMillis();
        collectDomains();  // note that buildInstTransRels() depends on this
        System.out.printf("KBcache.buildCaches(): collectDomains:              %d m/s%n", (System.currentTimeMillis() - millis));
        millis = System.currentTimeMillis();
        buildInstTransRels();
        System.out.printf("KBcache.buildCaches(): buildInstTransRels:          %d m/s%n", (System.currentTimeMillis() - millis));
        millis = System.currentTimeMillis();
        buildDirectInstances();
        System.out.printf("KBcache.buildCaches(): buildDirectInstances:        %d m/s%n", (System.currentTimeMillis() - millis));
        millis = System.currentTimeMillis();
        addTransitiveInstances();
        System.out.printf("KBcache.buildCaches(): addTransitiveInstances:      %d m/s%n", (System.currentTimeMillis() - millis));
        millis = System.currentTimeMillis();
        buildTransInstOf();
        correctValences(); // correct VariableArityRelation valences
        System.out.printf("KBcache.buildCaches(): buildTransInstOf:            %d m/s%n", (System.currentTimeMillis() - millis));
        millis = System.currentTimeMillis();
        buildExplicitDisjointMap(); // find relations under partition definition
        System.out.printf("KBcache.buildCaches(): buildExplicitDisjointMap:    %d m/s%n", (System.currentTimeMillis() - millis));
        if (KBmanager.getMgr().getPref("cacheDisjoint").equals("true")) {
            millis = System.currentTimeMillis();
//            buildExplicitDisjointMap();
            buildDisjointMap();
            System.out.printf("KBcache.buildCaches(): buildDisjointMap:            %d m/s%n", (System.currentTimeMillis() - millis));
        }
        millis = System.currentTimeMillis();
        buildFunctionsSet();
        System.out.printf("KBcache.buildCaches(): buildFunctionsSet:           %d m/s%n", (System.currentTimeMillis() - millis));
        millis = System.currentTimeMillis();
        //writeCacheFile();
        storeCacheAsFormulas();
        System.out.printf("KBcache.buildCaches(): store cached formulas:       %d m/s%n", (System.currentTimeMillis() - millis));

        System.out.printf("INFO in KBcache.buildCaches(): size: %d%n", instanceOf.keySet().size());
        System.out.printf("KBcache.buildCaches():                              %d total seconds%n", (System.currentTimeMillis() - startMillis) / KButilities.ONE_K);
        initialized = true;
    }

    /** ***************************************************************
     * Copy all relevant information from a VariableArityRelation to a new
     * predicate that is a particular fixed arity. Fill the signature from
     * final argument type in the predicate
     */
    public void copyNewPredFromVariableArity(String pred, String oldPred, int arity) {

        if (debug) System.out.println("copyNewPredFromVariableArity(): pred,oldPred: " + pred + ", " + oldPred);
        List<String> oldSig = signatures.get(oldPred);
        List<String> newSig = new ArrayList<>();
        if (oldSig != null)
            newSig = new ArrayList(oldSig);
        if (signatures.keySet().contains(oldPred))
            signatures.put(pred,newSig);
        String lastType = oldSig.get(oldSig.size()-1);
        for (int i = oldSig.size(); i <= arity; i++) {
            newSig.add(lastType);
        }
        if (instanceOf.keySet().contains(oldPred))
            instanceOf.put(pred, instanceOf.get(oldPred));
        valences.put(pred,arity);
        if (kb.isFunction(oldPred))
            kb.kbCache.functions.add(pred);
        kb.terms.add(pred);
    }

    /** ***************************************************************
     * @return the type of the last argument to the given relation,
     * which will be the type of all the expanded row variables
     */
    public String variableArityType(String r) {

        List<String> sig = getSignature(r);
        if (sig == null)
            System.err.println("Error in variableArityType() null signature for " + r);
        String type = sig.get(sig.size() - 1);
        return type;
    }

    /** *************************************************************
     */
    public static void showState(KBcache nkbc) {

        System.out.println("-------------- relations ----------------");
        Iterator<String> it = nkbc.relations.iterator();
        while (it.hasNext())
            System.out.print(it.next() + " ");
        System.out.println();
        System.out.println("-------------- transitives ----------------");
        it = nkbc.transRels.iterator();
        while (it.hasNext())
            System.out.print(it.next() + " ");
        System.out.println();
        System.out.println("-------------- parents ----------------");
        it = nkbc.parents.keySet().iterator();
        String rel;
        Map<String,Set<String>> relmap;
        while (it.hasNext()) {
            rel = it.next();
            System.out.println("Relation: " + rel);
            relmap = nkbc.parents.get(rel);
            for (String term : relmap.keySet()) {
                System.out.println(term + ": " + relmap.get(term));
            }
            System.out.println();
        }
        System.out.println();
        System.out.println("-------------- children ----------------");
        it = nkbc.children.keySet().iterator();
        while (it.hasNext()) {
            rel = it.next();
            System.out.println("Relation: " + rel);
            relmap = nkbc.children.get(rel);
            for (String term : relmap.keySet()) {
                System.out.println(term + ": " + relmap.get(term));
            }
            System.out.println();
        }
        System.out.println();
        System.out.println("-------------- disjoint ----------------");
        System.out.println(nkbc.explicitDisjoint);
        System.out.println();
        System.out.println("-------------- domains ----------------");
        Iterator<String> it3 = nkbc.relations.iterator();
        List<String> domains;
        while (it3.hasNext()) {
            rel = it3.next();
            domains = nkbc.signatures.get(rel);
            System.out.println(rel + ": " + domains);
        }
        System.out.println();
        System.out.println("-------------- valences ----------------");
        Integer arity;
        for (String r : nkbc.valences.keySet()) {
            arity = nkbc.valences.get(r);
            System.out.println(r + ": " + arity);
        }
        System.out.println();
        System.out.println("-------------- signatures ----------------");
        List<String> sig;
        for (String r : nkbc.signatures.keySet()) {
            sig = nkbc.signatures.get(r);
            System.out.println(r + ": " + sig);
        }
        System.out.println();
        System.out.println("-------------- insts ----------------");
        for (String inst : nkbc.insts)
            System.out.print(inst + ", ");
        System.out.println();
        System.out.println();
        System.out.println("-------------- instancesOf ----------------");
        for (String inst : nkbc.instanceOf.keySet())
            System.out.println(inst + ": " + nkbc.instanceOf.get(inst));
        System.out.println();
        System.out.println();
        System.out.println("-------------- instances ----------------");
        for (String inst : nkbc.instances.keySet())
            System.out.println(inst + ": " + nkbc.instances.get(inst));
    }

    /** *************************************************************
     */
    public static void showAll(KBcache nkbc) {

        System.out.println("KBcache.showAll(): transRels: " + nkbc.transRels);
        System.out.println("KBcache.showAll(): instTransRels: " + nkbc.instTransRels);
        System.out.println("KBcache.showAll(): instTransRels: " + nkbc.instanceOf);
        System.out.println("KBcache.showAll(): subclass signature: " + nkbc.signatures.get("subclass"));
        System.out.println("KBcache.showAll(): PrimaryColor: " + nkbc.instanceOf.get("PrimaryColor"));
        System.out.println("KBcache.showAll(): ColorAttribute: " + nkbc.instanceOf.get("ColorAttribute"));
        System.out.println("KBcache.showAll(): PrimaryColor: " + nkbc.getInstancesForType("PrimaryColor"));
        System.out.println("KBcache.showAll(): ColorAttribute: " + nkbc.getInstancesForType("ColorAttribute"));
        System.out.println("KBcache.showAll(): FormOfGovernment: " + nkbc.getInstancesForType("FormOfGovernment"));
    }

    /** *************************************************************
     */
    public static void showChildrenOf(KBcache nkbc, String term) {

        Set<String> classes = nkbc.getChildClasses(term);
        System.out.println("KBcache.showChildren(): children of " + term + ": " +
                classes);
    }

    /** *************************************************************
     */
    public static void showChildren(KBcache nkbc) {

        String term = "Integer";
        Set<String> classes = nkbc.getChildClasses(term);
        System.out.println("KBcache.showChildren(): children of " + term + ": " +
                classes);
        //nkbc.children = new HashMap<>();
        //nkbc.buildChildrenNew("Entity","subclass");
        term = "Integer";
        classes = nkbc.getChildClasses(term);
        System.out.println("KBcache.showChildren(): children of " + term + ": " +
                classes);
        term = "PositiveInteger";
        classes = nkbc.getChildClasses(term);
        System.out.println("KBcache.showChildren(): children of " + term + ": " +
                classes);
        term = "PositiveRealNumber";
        classes = nkbc.getChildClasses(term);
        System.out.println("KBcache.showChildren(): children of " + term + ": " +
                classes);
        term = "NonnegativeRealNumber";
        classes = nkbc.getChildClasses(term);
        System.out.println("KBcache.showChildren(): children of " + term + ": " +
                classes);
        term = "Number";
        classes = nkbc.getChildClasses(term);
        System.out.println("KBcache.showChildren(): children of " + term + ": " +
                classes);
        term = "RealNumber";
        classes = nkbc.getChildClasses(term);
        System.out.println("KBcache.showChildren(): children of " + term + ": " +
                classes);
    }

    /** *************************************************************
     * Informational routine to show the sizes of the caches as a way
     * to determine what might be the best sizes to pre-allocate, relative
     * to the number of statements in a knowledge base
     */
    public static void showSizes(KBcache nkbc) {

        System.out.println("KBcache.showSizes(): relations size: " + nkbc.relations.size());
        System.out.println("KBcache.showSizes(): functions size: " + nkbc.functions.size());
        System.out.println("KBcache.showSizes(): predicates size: " + nkbc.predicates.size());
        System.out.println("KBcache.showSizes(): transRels size: " + nkbc.transRels.size());
        System.out.println("KBcache.showSizes(): instRels size: " + nkbc.instRels.size());
        System.out.println("KBcache.showSizes(): instTransRels size: " + nkbc.instTransRels.size());
        System.out.println("KBcache.showSizes(): parents keySet size (# relations): " + nkbc.parents.keySet().size());
        int total = 0;
        for (Map<String, Set<String>> seconds : nkbc.parents.values())
            total = total + seconds.keySet().size();
        System.out.println("KBcache.showSizes(): parents average values size (# parents for relations): " +
                total / nkbc.parents.keySet().size());
        System.out.println("KBcache.showSizes(): instanceOf size: " + nkbc.instanceOf.size());
        total = 0;
        for (Set<String> seconds : nkbc.instances.values())
            total = total + seconds.size();
        System.out.println("KBcache.showSizes(): instances average values size (# instances ): " +
                total / nkbc.instances.keySet().size());
        total = 0;
        for (Map<String, Set<String>> seconds : nkbc.children.values())
            total = total + seconds.keySet().size();
        System.out.println("KBcache.showSizes(): children average values size (# children for relations): " +
                total / nkbc.children.keySet().size());
        System.out.println("KBcache.showSizes(): signature size: " + nkbc.signatures.size());
        System.out.println("KBcache.showSizes(): explicitDisjoint size: " + nkbc.explicitDisjoint.size());
        System.out.println("KBcache.showSizes(): disjointRelations size: " + nkbc.disjointRelations.size());
    }

    /** ***************************************************************
     */
    public static void printHelp() {

        System.out.println("Sigma Knowledge Engineering Environment: KBcache");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -a - show All cache contents");
        System.out.println("  -s - show size of cache elements");
        System.out.println("  -c term - show children of term");
        System.out.println("  -t - show complete state of cache");
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
                System.err.println(e.getMessage());
            }
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            System.out.println("**** Finished loading KB ***");
            System.out.println(HTMLformatter.showStatistics(kb));
            KBcache nkbc = kb.kbCache;
            if (args.length > 0 && args[0].equals("-a")) {
                showAll(nkbc);
            }
            else if (args.length > 0 && args[0].equals("-s")) {
                showSizes(nkbc);
            }
            else if (args.length > 1 && args[0].equals("-c")) {
                showChildrenOf(nkbc,args[1]);
            }
            else if (args.length > 0 && args[0].equals("-t")) {
                showState(nkbc);
            }
            else {
                printHelp();
            }
        }
    }
}
