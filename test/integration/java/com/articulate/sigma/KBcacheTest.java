package com.articulate.sigma;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;

import java.util.*;

import static org.junit.Assert.*;

public class KBcacheTest extends IntegrationTestBase {

    /** *************************************************************
     */
    @BeforeClass
    public static void requiredKB() {

        List<String> reqFiles =
                Arrays.asList("Merge.kif", "Mid-level-ontology.kif");
        for (String s : reqFiles) {
            if (!KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname")).containsFile(s)) {
                System.err.println("Error in KBcacheTest.requiredKB() required file " + s + " missing");
                System.out.println("only have files " +
                        KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname")).constituents);
                fail();
            }
        }
    }

    /** *************************************************************
     */
    @Test
    public void testIsParentOf1() {

//        KBcache cache = SigmaTestBase.kb.kbCache;
        //System.out.println("parents of Shirt (as instance): " + cache.getParentClassesOfInstance("Shirt"));
        //System.out.println("parents of Shirt: " + cache.parents.get("subclass").get("Shirt"));
        //System.out.println("childOfP(\"Shirt\", \"WearableItem\"): " + cache.childOfP("subclass", "WearableItem","Shirt"));
        //System.out.println("SigmaTestBase.kb.isChildOf(\"Shirt\", \"WearableItem\"): " + SigmaTestBase.kb.isChildOf("Shirt", "WearableItem"));
        //System.out.println("SigmaTestBase.kb.childOf(Shirt, WearableItem): " + SigmaTestBase.kb.childOf("Shirt", "WearableItem"));
        assertTrue(SigmaTestBase.kb.kbCache.parents.get("subclass").get("Shirt").contains("WearableItem"));
    }

    /** *************************************************************
     */
    @Test
    public void testBuildParents() {

        KBcache cache = SigmaTestBase.kb.kbCache;

        String child = "IrreflexiveRelation";
        HashSet<String> expected = new HashSet<>(Arrays.asList("Entity", "Relation", "InheritableRelation", "Abstract", "BinaryRelation"));
        Set<String> actual = cache.getParentClasses(child);
        assertEquals(expected, actual);

        child = "City";
        expected = new HashSet<>(Arrays.asList("Entity", "Physical", "Object", "Region", "GeographicArea", "AutonomousAgent", "GeopoliticalArea", "LandArea"));
        actual = cache.getParentClasses(child);
        assertEquals(expected, actual);

        child = "AsymmetricRelation";
        expected = new HashSet<>(Arrays.asList("Entity", "Abstract", "Relation", "InheritableRelation", "BinaryRelation", "AntisymmetricRelation", "IrreflexiveRelation"));
        actual = cache.getParentClasses(child);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     */
    @Test
    public void testBuildChildrenRealNumber() {

        System.out.println("\n============= testBuildChildren2 ==================");
        KBcache cache = SigmaTestBase.kb.kbCache;
        String parent = "RealNumber";

        TreeSet<String> expected = new TreeSet<>(Arrays.asList("RationalNumber","Integer","EvenInteger",
                "OddInteger","PrimeNumber","NonnegativeInteger","PositiveInteger","NegativeInteger",
                "IrrationalNumber","NonnegativeRealNumber","PositiveRealNumber","PositiveInteger",
                "NegativeRealNumber","NegativeInteger","BinaryNumber"));

        TreeSet<String> actual = new TreeSet<>(cache.getChildClasses(parent));

        System.out.println("KBcacheTest.testBuildChildren2(): actual: " + actual);
        System.out.println("KBcacheTest.testBuildChildren2(): expected: " + expected);
        if (actual.equals(expected))
            System.out.println("KBcacheTest.testBuildChildren2(): pass");
        else
            System.out.println("KBcacheTest.testBuildChildren2(): fail");
        assertEquals(expected, actual);
    }

    /** *************************************************************
     */
    @Test
    public void testTransitiveRelations() {

        System.out.println("\n============= testTransitiveRelations ==================");
        KBcache cache = SigmaTestBase.kb.kbCache;

        String relation = "abbreviation";
        System.out.println("testbuildTransInstOf(): testing: " + relation);
        HashSet<String> expected = new HashSet<>(Arrays.asList("Entity", "Relation", "InheritableRelation",
                "Abstract", "BinaryPredicate", "BinaryRelation", "Predicate"));
        Set<String> actual = cache.getParentClassesOfInstance(relation);
        assertEquals(expected, actual);

        relation = "during";  // TODO: since during is a subrelation of temporalPart it should be a superset here - bad test
        System.out.println("testbuildTransInstOf(): testing: " + relation);
        expected = new HashSet<>(Arrays.asList("Entity", "TransitiveRelation", "Abstract", "Relation",
                "InheritableRelation", "IrreflexiveRelation", "BinaryPredicate", "BinaryRelation", "Predicate"));
        actual = cache.getParentClassesOfInstance(relation);
        assertEquals(expected, actual);

        relation = "temporalPart";
        System.out.println("testbuildTransInstOf(): testing: " + relation);
        //expected = new HashSet<>(Arrays.asList("Entity", "TransitiveRelation", "AntisymmetricRelation",
        //        "Abstract", "TemporalRelation", "Relation", "InheritableRelation", "ReflexiveRelation",
        //        "PartialOrderingRelation", "BinaryRelation", "BinaryPredicate", "Predicate"));
        expected = new HashSet<>(Arrays.asList("Entity", "TransitiveRelation", "AntisymmetricRelation",
                "Abstract", "TotalValuedRelation", "Predicate", "TemporalRelation", "Relation",
                "InheritableRelation", "ReflexiveRelation", "BinaryPredicate", "PartialOrderingRelation",
                "BinaryRelation"));
        actual = cache.getParentClassesOfInstance(relation);
        assertEquals(expected, actual);
    }

    /** *************************************************************
     */
    @Test
    public void testIsChildOf1() {

        KBcache cache = SigmaTestBase.kb.kbCache;
        //System.out.println("parents of CitizenryFn (as instance): " + cache.getParentClassesOfInstance("CitizenryFn"));
        //System.out.println("parents of CitizenryFn: " + cache.parents.get("subclass").get("CitizenryFn"));
        assertTrue(SigmaTestBase.kb.isChildOf("CitizenryFn", "Function"));
    }

    /** *************************************************************
     */
    @Test
    public void testIsChildOf2() {

        KBcache cache = SigmaTestBase.kb.kbCache;
        //System.out.println("parents of Attorney (as instance): " + cache.getParentClassesOfInstance("Attorney"));
        //System.out.println("parents of Attorney: " + cache.parents.get("subclass").get("Attorney"));
        assertTrue(SigmaTestBase.kb.isChildOf("Attorney", "Attribute"));
    }

    /** *************************************************************
     */
    @Test
    public void testIsChildOf3() {

        KBcache cache = SigmaTestBase.kb.kbCache;
        //System.out.println("parents of Shirt (as instance): " + cache.getParentClassesOfInstance("Shirt"));
        //System.out.println("parents of Shirt: " + cache.parents.get("subclass").get("Shirt"));
        //System.out.println("childOfP(\"Shirt\", \"WearableItem\"): " + cache.childOfP("subclass", "WearableItem","Shirt"));
        //System.out.println("SigmaTestBase.kb.isChildOf(\"Shirt\", \"WearableItem\"): " + SigmaTestBase.kb.isChildOf("Shirt", "WearableItem"));
        assertTrue(SigmaTestBase.kb.isChildOf("Shirt", "WearableItem"));
    }

    /** *************************************************************
     */
    @Test
    public void testIsChildOf4() {

        KBcache cache = SigmaTestBase.kb.kbCache;
        //System.out.println("parents of Shirt (as instance): " + cache.getParentClassesOfInstance("Shirt"));
        //System.out.println("parents of Shirt: " + cache.parents.get("subclass").get("Shirt"));
        //System.out.println("childOfP(\"Shirt\", \"Process\"): " + cache.childOfP("subclass", "Process","Shirt"));
        //System.out.println("SigmaTestBase.kb.isChildOf(\"Shirt\", \"Process\"): " + SigmaTestBase.kb.isChildOf("Shirt", "Process"));
        assertFalse(SigmaTestBase.kb.isChildOf("Shirt", "Process"));
    }

    /** *************************************************************
     */
    @Test
    public void testIsChildOf5() {

        KBcache cache = SigmaTestBase.kb.kbCache;
        assertTrue(SigmaTestBase.kb.isChildOf("Integer", "RealNumber"));
    }

    /** *************************************************************
     */
    @Test
    public void testIsChildOf6() {

        KBcache cache = SigmaTestBase.kb.kbCache;
        assertTrue(SigmaTestBase.kb.isChildOf("Writing", "ContentDevelopment"));
    }


    /** *************************************************************
     */
    @Test
    public void testTransitiveRelations2() {

        System.out.println("\n============= testTransitiveRelations2 ==================");
        KBcache cache = SigmaTestBase.kb.kbCache;
        System.out.println("testTransitiveRelations2: " + cache.transRels);
        assertTrue(cache.transRels.contains("subAttribute"));
        assertTrue(cache.transRels.contains("subrelation"));
    }

    /** ***************************************************************
     * TODO: try to fix this
     */
    @Test
    @Ignore
    public void testDisjoint() {

        System.out.println("\n============= testDisjoint ==================");
        System.out.println("Test testDisjoint");
        HashSet<String> classes = new HashSet<>(Arrays.asList("Arthropod", "Bird"));
        System.out.println("KBcacheTest.testDisjoint(): Arthropod&Bird");
        System.out.println("KBcacheTest.testDisjoint(): disjoint? " + kb.kbCache.checkDisjoint(kb,"Arthropod", "Bird"));
        if (kb.kbCache.checkDisjoint(kb,"Arthropod", "Bird"))
            System.out.println("KBcacheTest.testDisjoint(): pass");
        else
            System.out.println("KBcacheTest.testDisjoint(): fail");
        assertTrue(kb.kbCache.checkDisjoint(kb,"Arthropod", "Bird"));

        System.out.println("KBcacheTest.testDisjoint(): classes: " + classes);
        System.out.println("KBcacheTest.testDisjoint(): disjoint? " + kb.kbCache.checkDisjoint(kb,classes));
        if (kb.kbCache.checkDisjoint(kb,classes))
            System.out.println("KBcacheTest.testDisjoint(): pass");
        else
            System.out.println("KBcacheTest.testDisjoint(): fail");
        assertTrue(kb.kbCache.checkDisjoint(kb,classes));

        classes = new HashSet<>(Arrays.asList("Table", "Chair"));
        System.out.println("KBcacheTest.testDisjoint(): classes: " + classes);
        System.out.println("KBcacheTest.testDisjoint(): disjoint? " + kb.kbCache.checkDisjoint(kb,classes));
        if (!kb.kbCache.checkDisjoint(kb,classes))
            System.out.println("KBcacheTest.testDisjoint(): pass");
        else
            System.out.println("KBcacheTest.testDisjoint(): fail");
        assertTrue(!kb.kbCache.checkDisjoint(kb,classes));

        classes = new HashSet<>(Arrays.asList("Table", "Agent"));
        System.out.println("KBcacheTest.testDisjoint(): classes: " + classes);
        System.out.println("KBcacheTest.testDisjoint(): disjoint? " + kb.kbCache.checkDisjoint(kb,classes));
        if (kb.kbCache.checkDisjoint(kb,classes))
            System.out.println("KBcacheTest.testDisjoint(): pass");
        else
            System.out.println("KBcacheTest.testDisjoint(): fail");
        assertTrue(kb.kbCache.checkDisjoint(kb,classes));
    }

    /** *************************************************************
     */
    @Test
    public void testSignature() {

        System.out.println("\n============= testSignature ==================");
        KBcache cache = SigmaTestBase.kb.kbCache;
        //System.out.println("parents of Shirt (as instance): " + cache.getParentClassesOfInstance("Shirt"));
        //System.out.println("parents of Shirt: " + cache.parents.get("subclass").get("Shirt"));
        //System.out.println("childOfP(\"Shirt\", \"WearableItem\"): " + cache.childOfP("subclass", "WearableItem","Shirt"));
        //System.out.println("SigmaTestBase.kb.isChildOf(\"Shirt\", \"WearableItem\"): " + SigmaTestBase.kb.isChildOf("Shirt", "WearableItem"));
        System.out.println("testSignature(): cache.getSignature(memberTypeCount): " + cache.getSignature("memberTypeCount"));
        ArrayList<String> expected = new ArrayList(Arrays.asList("", "Collection", "Class", "NonnegativeInteger"));
        List<String> actual = cache.getSignature("memberTypeCount");
        System.out.println("actual: " + actual);
        System.out.println("expoected: " + expected);
        assertTrue(actual.equals(expected));
    }

    /** *************************************************************
     */
    @Test
    public void testTransInst() {

        System.out.println("\n============= testTransInst ==================");
        KBcache cache = SigmaTestBase.kb.kbCache;
        System.out.println("testTransInst(): cache.transInstOf(Anger,Entity): " + cache.transInstOf("Anger","Entity"));
        System.out.println("testTransInst(): insts.contains(Anger): " + cache.insts.contains("Anger"));
        //System.out.println("testTransInst(): insts.contains(Anger): " + cache.insts.contains("Anger"));
        //System.out.println("testTransInst(): instancesOf: " + cache.instanceOf);
        assertTrue(cache.transInstOf("Anger","Entity"));
    }

    /** *************************************************************
     */
    @Test
    public void testRealization() {

        System.out.println("\n============= testRealization ==================");
        KBcache cache = SigmaTestBase.kb.kbCache;
        System.out.println("testRealization(): cache.isInstanceOf(realization,AntisymmetricRelation): " + cache.isInstanceOf("realization","AntisymmetricRelation"));
        System.out.println("testRealization(): cache.isInstanceOf(realization,SymmetricRelation): " + cache.isInstanceOf("realization","SymmetricRelation"));
        //System.out.println("testRealization(): cache.instances.get(AntisymmetricRelation): " + cache.instances.get("AntisymmetricRelation"));
        //System.out.println("testRealization(): cache.instances.get(SymmetricRelation): " + cache.instances.get("SymmetricRelation"));
        //System.out.println("testRealization(): cache.parents.get(intsance).get(realization): " + cache.parents.get("subrelation").get("realization"));
        //System.out.println("testRealization(): cache.instanceOf.get(realization): " + cache.instanceOf.get("realization"));
        assertTrue(!cache.isInstanceOf("realization","SymmetricRelation"));
        assertTrue(cache.isInstanceOf("realization","AntisymmetricRelation"));
    }

    /** *************************************************************
     */
    @Test
    public void testFunctions() {

        System.out.println("\n============= testFunctions ==================");
        KBcache cache = SigmaTestBase.kb.kbCache;
        System.out.println("cache.functions.contains(\"AfternoonFn\"): " + cache.functions.contains("AfternoonFn"));
        assertTrue(cache.functions.contains("AfternoonFn"));

    }

    /** *************************************************************
     */
    @Test
    public void testPredicates() {

        System.out.println("\n============= testPredicates ==================");
        KBcache cache = SigmaTestBase.kb.kbCache;
        Set<String> rels = cache.getChildInstances("Relation");
        boolean isInstanceOf;
        for (String rel : rels) {
            if (!rel.endsWith("Fn")) {
                isInstanceOf = cache.isInstanceOf(rel, "Predicate");
                if (!isInstanceOf) {
                    System.err.println("fail - " + rel + " not instance of Predicate");
                    System.err.println("parents of " + rel + ": " + cache.instanceOf.get(rel));
                }
                else
                    System.out.println("success for predicate: " + rel);
                assertTrue(rel + " is not an instance of Predicate", isInstanceOf);
            }
        }
    }
}
