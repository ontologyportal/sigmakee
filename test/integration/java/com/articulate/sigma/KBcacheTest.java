package com.articulate.sigma;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

public class KBcacheTest extends IntegrationTestBase {

    @Test
    public void testbuildTransInstOf() {

        KBcache cache = SigmaTestBase.kb.kbCache;

        String relation = "originalExpressedInLanguage";
        HashSet<String> expected = new HashSet<>(Arrays.asList("Entity", "Relation", "InheritableRelation",
                "Abstract", "BinaryPredicate", "BinaryRelation", "Predicate"));
        HashSet<String> actual = cache.getParentClassesOfInstance(relation);
        assertEquals(expected, actual);

        relation = "during";
        expected = new HashSet<>(Arrays.asList("Entity", "TransitiveRelation", "AntisymmetricRelation",
                "Abstract", "Predicate", "TemporalRelation", "Relation", "InheritableRelation", "ReflexiveRelation",
                "IrreflexiveRelation", "BinaryPredicate", "PartialOrderingRelation", "BinaryRelation", "SymmetricRelation"));
        actual = cache.getParentClassesOfInstance(relation);
        assertEquals(expected, actual);

        relation = "temporalPart";
        expected = new HashSet<>(Arrays.asList("Entity", "TransitiveRelation", "AntisymmetricRelation",
                "Abstract", "Predicate", "TemporalRelation", "Relation", "InheritableRelation", "ReflexiveRelation",
                "BinaryPredicate", "PartialOrderingRelation", "BinaryRelation"));
        actual = cache.getParentClassesOfInstance(relation);
        assertEquals(expected, actual);
    }
}
