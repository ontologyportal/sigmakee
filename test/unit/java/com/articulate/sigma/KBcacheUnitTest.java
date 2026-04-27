package com.articulate.sigma;

/**
 Note that this class, and therefore, Sigma, depends upon several terms
 being present in the ontology in order to function as intended.  They are:

 subclass
 subAttribute
 subrelation
 instance

 partition
 disjoint
 disjointDecomposition
 exhaustiveDecomposition
 exhaustiveAttribute

 domain
 domainSubclass
 Entity
 TransitiveRelation
 Relation
 */
import org.junit.Test;
import org.junit.BeforeClass;

import java.util.*;

import static org.junit.Assert.*;

public class KBcacheUnitTest {

    public static KB kb = new KB("TestKB");

    /** ***************************************************************
     */
    @BeforeClass
    public static void setup() {

        kb.kbCache = new KBcache(kb);
        KBmanager.getMgr().setPref("cacheDisjoint","true");
        KIFAST kif = new KIFAST();
        kif.parseStatement("(subAttribute Attorney Lawyer)");
        kif.parseStatement("(instance Attorney Profession)");
        kif.parseStatement("(instance Lawyer Profession)");
        kif.parseStatement("(subclass Profession Attribute)");
        kif.parseStatement("(subclass Attribute Entity)");
        kif.parseStatement("(instance rel Relation)");
        kif.parseStatement("(instance subclass TransitiveRelation)");
        kif.parseStatement("(instance subAttribute TransitiveRelation)");
        kif.parseStatement("(instance subrelation TransitiveRelation)");
        kif.parseStatement("(instance var VariableArityRelation)");
        kif.parseStatement("(domain var 1 Object)");
        kif.parseStatement("(domain var 2 Object)");
        kif.parseStatement("(domain rel 1 Object)");
        kif.parseStatement("(domain rel 2 Object)");
        kif.parseStatement("(subclass Object Entity)");
        kif.parseStatement("(subclass Furniture Object)");
        kif.parseStatement("(subclass Table Furniture)");
        kif.parseStatement("(subclass Chair Furniture)");
        kif.parseStatement("(subclass LadderBackChair Chair)");
        kif.parseStatement("(subrelation relsub rel)");
        kif.parseStatement("(subclass TransitiveRelation Relation)");
        kif.parseStatement("(subclass VariableArityRelation Relation)");
        //kif.parseStatement("(instance relsub TransitiveRelation)");
        kif.parseStatement("(subclass Relation Entity)");
        kif.parseStatement("(subrelation CitizenryFn ResidentFn)");
        kif.parseStatement("(instance CitizenryFn Function)");
        kif.parseStatement("(instance ResidentFn Function)");
        kif.parseStatement("(subclass Function Relation)");
        kif.parseStatement("(partition Animal Vertebrate Invertebrate)");
        kif.parseStatement("(subclass Dog Vertebrate)");
        kif.parseStatement("(subclass Jellyfish Invertebrate)");
        kb.merge(kif,"");
        for (Formula f : kb.formulaMap.values())
            f.sourceFile = "test"; // without a source file kbCache assumes it's a cached formula and ignores it
        kb.kbCache.buildCaches();
        KBcache.showState(kb.kbCache);
    }

    /** ***************************************************************
     */
    @Test
    public void testRelations() {

        System.out.println("Test relations");
        HashSet<String> expected = new HashSet<>(Arrays.asList("subAttribute",
                "var", "rel", "subclass", "CitizenryFn", "ResidentFn", "relsub",
                "subrelation"));
        Set<String> actual = kb.kbCache.relations;
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testFunctions() {

        System.out.println("Test functions");
        HashSet<String> expected = new HashSet<>(Arrays.asList("CitizenryFn", "ResidentFn"));
        Set<String> actual = kb.kbCache.functions;
        System.out.println("functions:" + actual);
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testPredicates() {

        System.out.println("Test predicates");
        Set<String> expected = new HashSet<>(Arrays.asList("subAttribute",
                "var", "rel", "subclass", "relsub",
                "subrelation"));
        Set<String> actual = kb.kbCache.predicates;
        System.out.println("predicates:" + actual);
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void transRels() {

        System.out.println("Test transRels");
        HashSet<String> expected = new HashSet<>(Arrays.asList("subclass", "subAttribute", "subrelation"));
        Set<String> actual = kb.kbCache.transRels;
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testParents() {

        System.out.println("Test parents");
        HashSet<String> expected = new HashSet<>(Arrays.asList("Relation", "Entity"));
        Set<String> actual = kb.kbCache.getParentClassesOfInstance("rel");
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testChildren() {

        System.out.println("Test children");
        HashSet<String> expected = new HashSet<>(Arrays.asList("relsub"));
        System.out.println("testChildren(): subrelations: " + kb.kbCache.children.get("subrelation"));
        Set<String> actual = null;
        if (kb.kbCache.children.get("subrelation") != null)
            actual = kb.kbCache.children.get("subrelation").get("rel");
        System.out.println("testChildren(): actual: " + actual);
        System.out.println("testChildren(): expected: " + expected);
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testSignatures() {

        System.out.println("Test signatures");
        List<String> expected = new ArrayList<>(Arrays.asList("", "Object", "Object"));
        List<String> actual = kb.kbCache.signatures.get("rel");
        assertEquals(expected.subList(1,2), actual.subList(1,2));
    }

    /** ***************************************************************
     */
    @Test
    public void testVarSignatures() {

        System.out.println("Test var signatures");
        String expected = "Object";
        System.out.println("testVarSignatures() expected: " + expected);
        String actual = kb.kbCache.variableArityType("var");
        System.out.println("testVarSignatures() actual: " + actual);
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testValences() {

        System.out.println("Test valences");
        int expected = 2;
        int actual = kb.kbCache.valences.get("rel");
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testValences2() {

        System.out.println("Test valences 2");
        int expected = -1;
        int actual = kb.kbCache.valences.get("var");
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testInsts() {

        System.out.println("Test insts");
        assertTrue(kb.kbCache.insts.contains("rel"));
    }

    /** ***************************************************************
     */
    @Test
    public void testInstances() {

        System.out.println("Test instances");
        HashSet<String> expected = new HashSet<>(Arrays.asList("subAttribute",
                "var", "rel", "relsub", "subclass",
                "subrelation", "CitizenryFn", "ResidentFn"));
        Set<String> actual = kb.kbCache.instances.get("Relation");
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testTransInsts() {

        System.out.println("Test testTransInsts");
        System.out.println("kb.kbCache.transInstOf(\"Attorney\", \"Attribute\"): " +
                kb.kbCache.transInstOf("Attorney", "Attribute"));
        assertTrue(kb.kbCache.transInstOf("Attorney", "Attribute"));
    }

    /** ***************************************************************
     */
    @Test
    public void testIsChildOf() {

        System.out.println("Test testIsChildOf");
        assertTrue(kb.isChildOf("CitizenryFn", "Function"));
    }

    /** ***************************************************************
     */
    @Test
    public void testCommonParent() {

        System.out.println("Test testCommonParent");
        String actual = kb.kbCache.getCommonParent("LadderBackChair", "Table");
        String expected = "Furniture";
        System.out.println("Test testCommonParent(): result: " + actual);
        assertEquals(expected,actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testInstancesForType() {

        System.out.println("Test testInstancesForType");
        HashSet<String> expected = new HashSet<>(Arrays.asList("subAttribute",
                "var", "subclass", "rel", "CitizenryFn", "ResidentFn", "relsub",
                "subrelation"));
        Set<String> actual = kb.kbCache.getInstancesForType("Relation");
        assertEquals(expected,actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testDisjoint() {

        boolean resultPass;
        System.out.println("Test testDisjoint");
        Set<String> classes = new HashSet<>(Arrays.asList("Dog", "Jellyfish"));
        System.out.println("KBcacheUnitTest.testDisjoint(): Dog&Jellyfish");
        resultPass = kb.kbCache.checkDisjoint(kb,"Dog", "Jellyfish");
        System.out.println("KBcacheUnitTest.testDisjoint(): disjoint? " + resultPass);
        if (resultPass)
            System.out.println("KBcacheUnitTest.testDisjoint(): pass");
        else
            System.err.println("KBcacheUnitTest.testDisjoint(): fail");
        assertTrue(resultPass);

        System.out.println("KBcacheUnitTest.testDisjoint(): classes: " + classes);
        resultPass = kb.kbCache.checkDisjoint(kb,classes);
        System.out.println("KBcacheUnitTest.testDisjoint(): disjoint? " + resultPass);
        if (resultPass)
            System.out.println("KBcacheUnitTest.testDisjoint(): pass");
        else
            System.err.println("KBcacheUnitTest.testDisjoint(): fail");
        assertTrue(resultPass);

        classes = new HashSet<>(Arrays.asList("Physical", "LengthMeasure"));
        System.out.println("KBcacheUnitTest.testDisjoint(): classes: " + classes);
        resultPass = kb.kbCache.checkDisjoint(kb,classes);
        System.out.println("KBcacheUnitTest.testDisjoint(): disjoint? " + resultPass);
        if (!resultPass)
            System.out.println("KBcacheUnitTest.testDisjoint(): pass");
        else
            System.err.println("KBcacheUnitTest.testDisjoint(): fail");
        assertTrue(!resultPass);
    }

    /** ***************************************************************
     */
    @Test
    public void testCollectArgsFromFormulas() {

        System.out.println("Test testCollectArgsFromFormulas");
        String rel = "TransitiveRelation";
        List<Formula> forms = kb.askWithRestriction(0,"instance",2,rel);
        System.out.println("INFO in KBcache.testCollectArgsFromFormulas(): forms2: " + forms);
        Set<String> actual = new HashSet<>();
        if (forms != null)
            actual.addAll(KBcache.collectArgFromFormulas(1,forms));
        Set<String> expected = new HashSet<>(Arrays.asList("subAttribute",
                 "subclass", "subrelation"));
        System.out.println("INFO in KBcache.testCollectArgsFromFormulas(): actual: " + actual);
        System.out.println("INFO in KBcache.testCollectArgsFromFormulas(): expected: " + expected);
        assertEquals(expected,actual);
    }

    // -----------------------------------------------------------------------
    // KBcache copy constructor tests
    // Each test creates a fresh deep copy and verifies a field that was
    // previously missing from the copy constructor.
    // -----------------------------------------------------------------------

    /** ***************************************************************
     * Copy constructor: functions set is present and equal to the original.
     */
    @Test
    public void testCopyConstructorFunctions() {

        System.out.println("Test testCopyConstructorFunctions");
        KBcache copy = new KBcache(kb.kbCache, kb);
        assertEquals(kb.kbCache.functions, copy.functions);
    }

    /** ***************************************************************
     * Copy constructor: predicates set is present and equal to the original.
     */
    @Test
    public void testCopyConstructorPredicates() {

        System.out.println("Test testCopyConstructorPredicates");
        KBcache copy = new KBcache(kb.kbCache, kb);
        assertEquals(kb.kbCache.predicates, copy.predicates);
    }

    /** ***************************************************************
     * Copy constructor: instRels set is present and equal to the original.
     */
    @Test
    public void testCopyConstructorInstRels() {

        System.out.println("Test testCopyConstructorInstRels");
        KBcache copy = new KBcache(kb.kbCache, kb);
        assertEquals(kb.kbCache.instRels, copy.instRels);
    }

    /** ***************************************************************
     * Copy constructor: instances map values are present and equal.
     * We check the "Relation" class bucket which is well-populated in setup.
     */
    @Test
    public void testCopyConstructorInstances() {

        System.out.println("Test testCopyConstructorInstances");
        KBcache copy = new KBcache(kb.kbCache, kb);
        // Verify every entry in the original is represented identically in the copy
        for (Map.Entry<String, Set<String>> entry : kb.kbCache.instances.entrySet()) {
            assertTrue("instances key missing in copy: " + entry.getKey(),
                    copy.instances.containsKey(entry.getKey()));
            assertEquals("instances value mismatch for key " + entry.getKey(),
                    entry.getValue(), copy.instances.get(entry.getKey()));
        }
    }

    /** ***************************************************************
     * Copy constructor: disjoint set is present and equal to the original.
     * The test KB has (partition Animal Vertebrate Invertebrate) which
     * puts at least one pair into disjoint.
     */
    @Test
    public void testCopyConstructorDisjoint() {

        System.out.println("Test testCopyConstructorDisjoint");
        KBcache copy = new KBcache(kb.kbCache, kb);
        assertFalse("disjoint set should be non-empty after partition", copy.disjoint.isEmpty());
        assertEquals(kb.kbCache.disjoint, copy.disjoint);
    }

    /** ***************************************************************
     * Copy constructor: initialized flag is copied.
     */
    @Test
    public void testCopyConstructorInitialized() {

        System.out.println("Test testCopyConstructorInitialized");
        KBcache copy = new KBcache(kb.kbCache, kb);
        assertEquals(kb.kbCache.initialized, copy.initialized);
        assertTrue("initialized should be true after buildCaches()", copy.initialized);
    }

    /** ***************************************************************
     * Copy constructor independence: mutating a copied set must not alter
     * the original (proves deep copy, not shallow reference sharing).
     * We test functions, predicates, instances, and disjoint.
     */
    @Test
    public void testCopyConstructorIndependence() {

        System.out.println("Test testCopyConstructorIndependence");
        KBcache copy = new KBcache(kb.kbCache, kb);

        // Snapshot original sizes before mutation
        int origFunctionsSize      = kb.kbCache.functions.size();
        int origPredicatesSize     = kb.kbCache.predicates.size();
        int origDisjointSize       = kb.kbCache.disjoint.size();
        int origInstancesRelSize   = kb.kbCache.instances.containsKey("Relation")
                ? kb.kbCache.instances.get("Relation").size() : 0;

        // Mutate the copy
        copy.functions.add("__testSentinel__");
        copy.predicates.add("__testSentinel__");
        copy.disjoint.add("__testSentinel__");
        if (copy.instances.containsKey("Relation"))
            copy.instances.get("Relation").add("__testSentinel__");

        // Original must be unchanged
        assertEquals("functions size changed in original after copy mutation",
                origFunctionsSize, kb.kbCache.functions.size());
        assertEquals("predicates size changed in original after copy mutation",
                origPredicatesSize, kb.kbCache.predicates.size());
        assertEquals("disjoint size changed in original after copy mutation",
                origDisjointSize, kb.kbCache.disjoint.size());
        if (kb.kbCache.instances.containsKey("Relation")) {
            assertEquals("instances['Relation'] size changed in original after copy mutation",
                    origInstancesRelSize, kb.kbCache.instances.get("Relation").size());
        }
    }
}
