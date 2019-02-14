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
import com.google.common.collect.Sets;
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
        KIF kif = new KIF();
        kif.parseStatement("(subAttribute Attorney Lawyer)");
        kif.parseStatement("(instance Attorney Profession)");
        kif.parseStatement("(instance Lawyer Profession)");
        kif.parseStatement("(subclass Profession Attribute)");
        kif.parseStatement("(subclass Attribute Entity)");
        kif.parseStatement("(instance rel Relation)");
        kif.parseStatement("(instance subclass TransitiveRelation)");
        kif.parseStatement("(instance subAttribute TransitiveRelation)");
        kif.parseStatement("(instance subrelation TransitiveRelation)");
        kif.parseStatement("(domain rel 1 Object)");
        kif.parseStatement("(domain rel 2 Object)");
        kif.parseStatement("(subclass Object Entity)");
        kif.parseStatement("(subrelation relsub rel)");
        kif.parseStatement("(subclass TransitiveRelation Relation)");
        kif.parseStatement("(instance relsub TransitiveRelation)");
        kif.parseStatement("(subclass Relation Entity)");
        kif.parseStatement("(subrelation CitizenryFn ResidentFn)");
        kif.parseStatement("(instance CitizenryFn Function)");
        kif.parseStatement("(instance ResidentFn Function)");
        kif.parseStatement("(subclass Function Relation)");
        kb.merge(kif,"");
        for (Formula f : kb.formulaMap.values())
            f.sourceFile = "test"; // without a source file kbCache assumes it's a cached formula and ignores it
        kb.kbCache.buildCaches();
        kb.kbCache.showState();
    }

    /** ***************************************************************
     */
    @Test
    public void testRelations() {

        System.out.println("Test relations");
        HashSet<String> expected = new HashSet<>(Arrays.asList("subAttribute", "rel", "relsub", "subclass",
                "subrelation", "CitizenryFn", "ResidentFn"));
        HashSet<String> actual = kb.kbCache.relations;
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void transRels() {

        System.out.println("Test transRels");
        HashSet<String> expected = new HashSet<>(Arrays.asList("relsub", "subclass", "subAttribute", "subrelation"));
        HashSet<String> actual = kb.kbCache.transRels;
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testParents() {

        System.out.println("Test parents");
        HashSet<String> expected = new HashSet<>(Arrays.asList("Relation", "Entity"));
        HashSet<String> actual = kb.kbCache.getParentClassesOfInstance("rel");
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testChildren() {

        System.out.println("Test children");
        HashSet<String> expected = new HashSet<>(Arrays.asList("relsub"));
        HashSet<String> actual = kb.kbCache.children.get("subrelation").get("rel");
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testSignatures() {

        System.out.println("Test signatures");
        ArrayList<String> expected = new ArrayList<>(Arrays.asList("", "Object", "Oject"));
        ArrayList<String> actual = kb.kbCache.signatures.get("rel");
        assertEquals(expected.subList(1,2), actual.subList(1,2));
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
    public void testInsts() {

        System.out.println("Test insts");
        assertTrue(kb.kbCache.insts.contains("rel"));
    }

    /** ***************************************************************
     */
    @Test
    public void testInstances() {

        System.out.println("Test instances");
        HashSet<String> expected = new HashSet<>(Arrays.asList("subAttribute", "rel", "relsub", "subclass",
                "subrelation", "CitizenryFn", "ResidentFn"));
        HashSet<String> actual = kb.kbCache.instances.get("Relation");
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testTransInsts() {

        System.out.println("Test testTransInsts");
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
    public void testInstancesForType() {

        System.out.println("Test testInstancesForType");
        HashSet<String> expected = new HashSet<>(Arrays.asList("subAttribute", "rel", "relsub", "subclass",
                "subrelation", "CitizenryFn", "ResidentFn"));
        HashSet<String> actual = kb.kbCache.getInstancesForType("Relation");
        assertEquals(expected,actual);
    }

}
