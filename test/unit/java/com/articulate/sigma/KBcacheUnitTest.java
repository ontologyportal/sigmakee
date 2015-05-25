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

import java.util.*;

import static org.junit.Assert.*;

public class KBcacheUnitTest {

    @Test
    public void testBasic() {

        KB kb = new KB("TestKB");
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
        kif.parseStatement("(instance ResidentFn Function)");
        kif.parseStatement("(subclass Function Relation)");
        kb.merge(kif,"");
        kb.kbCache.buildCaches();
        String rel = "rel";
        kb.kbCache.showState();
        System.out.println("Test relations");
        HashSet<String> expected = new HashSet<>(Arrays.asList("subAttribute","rel", "relsub", "subclass",
                "subrelation","CitizenryFn","ResidentFn"));
        HashSet<String> actual = kb.kbCache.relations;
        assertEquals(expected, actual);
        //transRels
        System.out.println("Test transRels");
        expected = new HashSet<>(Arrays.asList("relsub","subclass","subAttribute","subrelation"));
        actual = kb.kbCache.transRels;
        assertEquals(expected, actual);
        // parents
        //expected = new HashSet<>(Arrays.asList("rel", "relsub"));
        //actual = kb.kbCache.parents.get(rel);
        //assertEquals(expected, actual);
        // children
        //expected = new HashSet<>(Arrays.asList("rel", "relsub"));
        //actual = kb.kbCache.children.get(rel);
        //assertEquals(expected, actual);
        // signatures
        //expected = new HashSet<>(Arrays.asList("rel", "relsub"));
        //actual = kb.kbCache.signatures.get(rel);
        //assertEquals(expected, actual);
        // valences
        //expected = new HashSet<>(Arrays.asList("rel", "relsub"));
        //actual = kb.kbCache.valences;
        //assertEquals(expected, actual);

        // insts
        //System.out.println("Test insts");
        //expected = new HashSet<>(Arrays.asList("rel", "relsub"));
        //actual = kb.kbCache.insts;
        //assertEquals(expected, actual);

        //instances
        //expected = new HashSet<>(Arrays.asList("rel", "relsub"));
        //actual = kb.kbCache.instances;
        //assertEquals(expected, actual);

        assertTrue(kb.kbCache.transInstOf("Attorney", "Attribute"));
        //assertTrue(kb.isChildOf("CitizenryFn", "Function"));
    }
}
