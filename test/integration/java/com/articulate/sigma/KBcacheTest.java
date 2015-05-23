package com.articulate.sigma;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KBcacheTest extends IntegrationTestBase {

    @Test
    public void testbuildParents() {

        KBcache cache = SigmaTestBase.kb.kbCache;

        String child = "IrreflexiveRelation";
        HashSet<String> expected = new HashSet<>(Arrays.asList("Entity", "Relation", "InheritableRelation", "Abstract", "BinaryRelation"));
        HashSet<String> actual = cache.getParentClasses(child);
        assertEquals(expected, actual);

        child = "City";
        expected = new HashSet<>(Arrays.asList("Entity", "Physical", "Object", "Region", "GeographicArea", "Agent", "GeopoliticalArea", "LandArea"));
        actual = cache.getParentClasses(child);
        assertEquals(expected, actual);

        child = "AsymmetricRelation";
        expected = new HashSet<>(Arrays.asList("Entity", "Abstract", "Relation", "InheritableRelation", "BinaryRelation", "AntisymmetricRelation", "IrreflexiveRelation"));
        actual = cache.getParentClasses(child);
        assertEquals(expected, actual);
    }

    @Test
    public void testbuildChildren() {

        KBcache cache = SigmaTestBase.kb.kbCache;

        String parent = "BiologicalAttribute";
        HashSet<String> expected = new HashSet<>(Arrays.asList("AnimacyAttribute", "BodyPosition", "DevelopmentalAttribute",
                "DiseaseOrSyndrome", "Fingerprint", "PsychologicalAttribute", "SexAttribute", "VisualAcuityAttribute",
                "AquiredImmunoDeficiencySyndrome", "Disability", "PhysicalDisability", "SensoryDisability",
                "InfectiousDisease", "BacterialDisease", "VaccinatableDisease", "Influenza", "TickBorneEncephalitis",
                "ViralDisease", "EmotionalState", "HemorrhagicFever", "Hepatitis", "RiftValleyFever", "LiteracyAttribute",
                "VenezuelanEquineEncephalitis", "ConsciousnessAttribute", "LifeThreateningDisease", "NonspecificDisease",
                "PsychologicalDysfunction", "Neurosis", "Psychosis", "TyphoidFever", "StateOfMind", "TraitAttribute"));
        HashSet<String> actual = cache.getChildClasses(parent);
        assertEquals(expected, actual);
        parent = "AsymmetricRelation";
        expected = new HashSet<>(Arrays.asList("PropositionalAttitude", "CaseRole"));
        actual = cache.getChildClasses(parent);
        assertEquals(expected, actual);
    }

    @Test
    public void testbuildTransInstOf() {

        KBcache cache = SigmaTestBase.kb.kbCache;

        String relation = "originalExpressedInLanguage";
        HashSet<String> expected = new HashSet<>(Arrays.asList("Entity", "Relation", "InheritableRelation",
                "Abstract", "BinaryPredicate", "BinaryRelation", "Predicate"));
        HashSet<String> actual = cache.getParentClassesOfInstance(relation);
        assertEquals(expected, actual);

        relation = "during";  // TODO: since during is a subrelation of temporalPart it should be a superset here - bad test
        expected = new HashSet<>(Arrays.asList("Entity", "TransitiveRelation", "Abstract", "Relation", "InheritableRelation", "IrreflexiveRelation", "BinaryRelation"));
        actual = cache.getParentClassesOfInstance(relation);
        assertEquals(expected, actual);

        relation = "temporalPart";
        expected = new HashSet<>(Arrays.asList("Entity", "TransitiveRelation", "AntisymmetricRelation",
                "Abstract", "TemporalRelation", "Relation", "InheritableRelation", "ReflexiveRelation",
                "PartialOrderingRelation", "BinaryRelation", "BinaryPredicate", "Predicate"));
        actual = cache.getParentClassesOfInstance(relation);
        assertEquals(expected, actual);
    }

    @Test
    public void testIsChildOf1() {

        KBcache cache = SigmaTestBase.kb.kbCache;
        System.out.println("parents of CitizenryFn: " + cache.getParentClassesOfInstance("CitizenryFn"));
        System.out.println("parents of CitizenryFn: " + cache.parents.get("CitizenryFn"));
        assertTrue(SigmaTestBase.kb.isChildOf("CitizenryFn", "Function"));
    }

    @Test
    public void testIsChildOf2() {

        KBcache cache = SigmaTestBase.kb.kbCache;
        System.out.println("parents of Attorney: " + cache.getParentClassesOfInstance("Attorney"));
        System.out.println("parents of Attorney: " + cache.parents.get("Attorney"));
        assertTrue(SigmaTestBase.kb.isChildOf("Attorney", "Attribute"));
    }

    @Test
    public void testTransitiveRelations() {

        KBcache cache = SigmaTestBase.kb.kbCache;
        System.out.println("transRels: " + cache.transRels);
        assertTrue(cache.transRels.contains("subAttribute"));
        assertTrue(cache.transRels.contains("subrelation"));
    }
}
