package com.articulate.sigma;

/**
 * Unit tests for the M3.2 incremental KBcache update methods.
 *
 * For each method we follow the same pattern:
 *   1. Build a "before" KB and run buildCaches().
 *   2. Apply the incremental method.
 *   3. Build a "reference" KB that already contains the new assertion
 *      and run buildCaches() on it.
 *   4. Assert that the incremental result equals the reference result
 *      for every cache field that the method is supposed to update.
 *
 * This proves that an incremental update produces the same answer as a
 * full rebuild without needing to rebuild everything.
 */

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class KBcacheIncrementalTest {

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Build a fresh KB+cache from a list of KIF statements. */
    private static KB buildKB(String... kifStatements) {
        KB kb = new KB("TestIncrKB");
        kb.kbCache = new KBcache(kb);
        KBmanager.getMgr().setPref("cacheDisjoint", "true");
        KIF kif = new KIF();
        for (String stmt : kifStatements)
            kif.parseStatement(stmt);
        kb.merge(kif, "");
        // Tag all formulas so KBcache does not treat them as cached
        for (Formula f : kb.formulaMap.values())
            f.sourceFile = "test";
        kb.kbCache.buildCaches();
        return kb;
    }

    /**
     * Shared core ontology used by most tests (subclass / instance / transitive relations).
     * Entity is intentionally left as a pure arg-2 term (no subclass-of-Entity assertion
     * on Entity itself) so that findRoots("subclass") returns {Entity} and BFS runs correctly.
     */
    private static final String[] CORE = {
            "(instance subclass TransitiveRelation)",
            "(instance subrelation TransitiveRelation)",
            "(instance subAttribute TransitiveRelation)",
            "(subclass TransitiveRelation Relation)",
            "(subclass Relation Entity)",
    };

    // ------------------------------------------------------------------
    // addSubclass tests
    // ------------------------------------------------------------------

    /**
     * After addSubclass("Robot", "Agent"), parents["subclass"]["Robot"] must contain
     * "Agent" and all ancestors of "Agent".
     */
    @Test
    public void testAddSubclass_parents() {

        String[] before = concat(CORE,
                "(subclass Agent Entity)",
                "(subclass PhysicalAgent Agent)"
        );
        KB kb = buildKB(before);

        // Reference: rebuild with the new assertion included
        KB ref = buildKB(concat(before, "(subclass Robot Agent)"));

        // Incremental update on kb's cache
        kb.kbCache.addSubclass("Robot", "Agent");

        Set<String> incParents = kb.kbCache.parents.get("subclass").get("Robot");
        Set<String> refParents = ref.kbCache.parents.get("subclass").get("Robot");

        assertNotNull("parents[subclass][Robot] must not be null after addSubclass", incParents);
        assertEquals("parents[subclass][Robot] must match full rebuild", refParents, incParents);
    }

    /**
     * After addSubclass("Robot", "Agent"), children["subclass"]["Agent"] must contain "Robot".
     */
    @Test
    public void testAddSubclass_children() {

        String[] before = concat(CORE,
                "(subclass Agent Entity)",
                "(subclass PhysicalAgent Agent)"
        );
        KB kb  = buildKB(before);
        KB ref = buildKB(concat(before, "(subclass Robot Agent)"));

        kb.kbCache.addSubclass("Robot", "Agent");

        Set<String> incChildren = kb.kbCache.children.get("subclass").get("Agent");
        Set<String> refChildren = ref.kbCache.children.get("subclass").get("Agent");

        assertNotNull("children[subclass][Agent] must not be null", incChildren);
        assertEquals("children[subclass][Agent] must match full rebuild", refChildren, incChildren);
    }

    /**
     * Descendants of the new child also get the new parents.
     * "SubRobot" is a subclass of "Robot" before the call; after addSubclass("Robot","Agent"),
     * SubRobot's parents must include Agent and its ancestors.
     */
    @Test
    public void testAddSubclass_descendantsInheritNewParents() {

        String[] before = concat(CORE,
                "(subclass Agent Entity)",
                "(subclass Robot Entity)",
                "(subclass SubRobot Robot)"
        );
        KB kb  = buildKB(before);
        KB ref = buildKB(concat(before, "(subclass Robot Agent)"));

        kb.kbCache.addSubclass("Robot", "Agent");

        Set<String> incParents = kb.kbCache.parents.get("subclass").get("SubRobot");
        Set<String> refParents = ref.kbCache.parents.get("subclass").get("SubRobot");

        assertNotNull("parents[subclass][SubRobot] must not be null", incParents);
        assertEquals("SubRobot must inherit Agent as ancestor", refParents, incParents);
    }

    /**
     * Instances of child classes gain the new parent class in instanceOf.
     * "myRobot" is an instance of "Robot" before addSubclass("Robot","Agent");
     * afterward instanceOf["myRobot"] must include "Agent".
     */
    @Test
    public void testAddSubclass_instanceOfUpdated() {

        String[] before = concat(CORE,
                "(subclass Agent Entity)",
                "(subclass Robot Entity)",
                "(instance myRobot Robot)"
        );
        KB kb  = buildKB(before);
        KB ref = buildKB(concat(before, "(subclass Robot Agent)"));

        kb.kbCache.addSubclass("Robot", "Agent");

        Set<String> incInstanceOf = kb.kbCache.instanceOf.get("myRobot");
        Set<String> refInstanceOf = ref.kbCache.instanceOf.get("myRobot");

        assertNotNull("instanceOf[myRobot] must not be null", incInstanceOf);
        assertEquals("instanceOf[myRobot] must match full rebuild", refInstanceOf, incInstanceOf);
    }

    /**
     * instances["Agent"] update after addSubclass("Robot","Agent"):
     * If Agent already had direct instances before the call, the new Robot
     * instances should propagate.  If Agent had no entry yet (no direct instances),
     * the incremental update must match the full rebuild — i.e. leave it null.
     *
     * This test covers the "Agent had direct instances" case.
     */
    @Test
    public void testAddSubclass_instancesOfAncestorUpdated() {

        // Pre-populate Agent with a direct instance so instances["Agent"] already exists
        String[] before = concat(CORE,
                "(subclass Agent Entity)",
                "(subclass Robot Entity)",
                "(instance myAgent Agent)",   // gives Agent an existing instances entry
                "(instance myRobot Robot)"
        );
        KB kb  = buildKB(before);
        KB ref = buildKB(concat(before, "(subclass Robot Agent)"));

        kb.kbCache.addSubclass("Robot", "Agent");

        Set<String> incInstances = kb.kbCache.instances.get("Agent");
        Set<String> refInstances = ref.kbCache.instances.get("Agent");

        assertNotNull("instances[Agent] must not be null (it had a direct instance)", incInstances);
        assertEquals("instances[Agent] must match full rebuild", refInstances, incInstances);
    }

    // ------------------------------------------------------------------
    // addInstance tests
    // ------------------------------------------------------------------

    /**
     * addInstance("myRobot","Robot") must add "Robot" and its ancestors to
     * instanceOf["myRobot"].
     */
    @Test
    public void testAddInstance_instanceOf() {

        String[] before = concat(CORE,
                "(subclass Agent Entity)",
                "(subclass Robot Agent)"
        );
        KB kb  = buildKB(before);
        KB ref = buildKB(concat(before, "(instance myRobot Robot)"));

        kb.kbCache.addInstance("myRobot", "Robot");

        Set<String> incInstanceOf = kb.kbCache.instanceOf.get("myRobot");
        Set<String> refInstanceOf = ref.kbCache.instanceOf.get("myRobot");

        assertNotNull("instanceOf[myRobot] must not be null", incInstanceOf);
        assertEquals("instanceOf[myRobot] must match full rebuild", refInstanceOf, incInstanceOf);
    }

    /**
     * addInstance("myRobot","Robot") must add "myRobot" to instances["Robot"].
     * For ancestor classes that already have an instances entry the instance is
     * propagated there too.  Classes with no existing entry (like "Agent" here)
     * are left null — matching full-rebuild behaviour (addTransitiveInstances only
     * augments existing keySet entries).
     */
    @Test
    public void testAddInstance_instances() {

        String[] before = concat(CORE,
                "(subclass Agent Entity)",
                "(subclass Robot Agent)"
        );
        KB kb  = buildKB(before);
        KB ref = buildKB(concat(before, "(instance myRobot Robot)"));

        kb.kbCache.addInstance("myRobot", "Robot");

        // Direct class must always be populated
        assertTrue("instances[Robot] must contain myRobot",
                kb.kbCache.instances.getOrDefault("Robot", Collections.emptySet()).contains("myRobot"));

        // Verify incremental matches the full rebuild for Robot
        assertEquals("instances[Robot] must match full rebuild",
                ref.kbCache.instances.get("Robot"), kb.kbCache.instances.get("Robot"));

        // Agent has no pre-existing instances entry in the "before" KB;
        // neither the full rebuild nor the incremental update creates one.
        assertEquals("instances[Agent] must match full rebuild (both null)",
                ref.kbCache.instances.get("Agent"), kb.kbCache.instances.get("Agent"));
    }

    /**
     * addInstance must also add "myRobot" to the insts set.
     */
    @Test
    public void testAddInstance_insts() {

        String[] before = concat(CORE,
                "(subclass Robot Entity)"
        );
        KB kb = buildKB(before);
        kb.kbCache.addInstance("myRobot", "Robot");
        assertTrue("insts must contain myRobot after addInstance", kb.kbCache.insts.contains("myRobot"));
    }

    // ------------------------------------------------------------------
    // addDomain tests
    // ------------------------------------------------------------------

    /**
     * addDomain("loves", 1, "Agent") must set signatures["loves"][1] = "Agent".
     */
    @Test
    public void testAddDomain_signatureUpdated() {

        String[] before = concat(CORE,
                "(instance loves Relation)"
        );
        KB kb = buildKB(before);

        kb.kbCache.addDomain("loves", 1, "Agent");

        List<String> sig = kb.kbCache.signatures.get("loves");
        assertNotNull("signatures[loves] must not be null", sig);
        assertTrue("signatures[loves] must have at least 2 elements", sig.size() > 1);
        assertEquals("signatures[loves][1] must be Agent", "Agent", sig.get(1));
    }

    /**
     * addDomain must extend valences if argNum exceeds current arity.
     */
    @Test
    public void testAddDomain_valencesUpdated() {

        String[] before = concat(CORE,
                "(instance loves Relation)"
        );
        KB kb = buildKB(before);

        kb.kbCache.addDomain("loves", 2, "Human");

        int valence = kb.kbCache.valences.getOrDefault("loves", 0);
        assertTrue("valences[loves] must be >= 2 after addDomain(loves,2,...)", valence >= 2);
    }

    /**
     * addDomain must propagate to child relations that lack an explicit binding.
     */
    @Test
    public void testAddDomain_propagatesToSubrelations() {

        String[] before = concat(CORE,
                "(instance loves Relation)",
                "(instance adores Relation)",
                "(subrelation adores loves)"
        );
        KB kb = buildKB(before);

        // adores has no explicit domain 1 → should inherit from loves
        kb.kbCache.addDomain("loves", 1, "Agent");

        List<String> adoreSig = kb.kbCache.signatures.get("adores");
        assertNotNull("signatures[adores] must not be null", adoreSig);
        assertTrue("signatures[adores] must have at least 2 elements", adoreSig.size() > 1);
        assertEquals("adores must inherit domain 1 from loves", "Agent", adoreSig.get(1));
    }

    /**
     * addDomain must NOT overwrite an explicitly-defined binding in a child relation.
     */
    @Test
    public void testAddDomain_doesNotOverrideExplicit() {

        String[] before = concat(CORE,
                "(instance loves Relation)",
                "(instance adores Relation)",
                "(subrelation adores loves)",
                "(domain adores 1 Human)"       // explicit on adores
        );
        KB kb = buildKB(before);

        // loves gets a broader type; adores must keep its explicit "Human"
        kb.kbCache.addDomain("loves", 1, "Agent");

        List<String> adoreSig = kb.kbCache.signatures.get("adores");
        assertNotNull(adoreSig);
        assertTrue(adoreSig.size() > 1);
        assertEquals("adores explicit domain 1 must not be overwritten", "Human", adoreSig.get(1));
    }

    // ------------------------------------------------------------------
    // addRange tests
    // ------------------------------------------------------------------

    /**
     * addRange delegates to addDomain(rel, 0, type): signatures[rel][0] = type.
     */
    @Test
    public void testAddRange_signatureSlot0() {

        String[] before = concat(CORE,
                "(instance motherOf Relation)"
        );
        KB kb = buildKB(before);

        kb.kbCache.addRange("motherOf", "Woman");

        List<String> sig = kb.kbCache.signatures.get("motherOf");
        assertNotNull("signatures[motherOf] must not be null after addRange", sig);
        assertFalse("signatures[motherOf] must not be empty", sig.isEmpty());
        assertEquals("signatures[motherOf][0] must be Woman (range)", "Woman", sig.get(0));
    }

    // ------------------------------------------------------------------
    // addSubrelation tests
    // ------------------------------------------------------------------

    /**
     * After addSubrelation("biologicalMother","mother"),
     * parents["subrelation"]["biologicalMother"] must contain "mother" and its ancestors.
     */
    @Test
    public void testAddSubrelation_parents() {

        String[] before = concat(CORE,
                "(instance mother Relation)",
                "(instance biologicalMother Relation)",
                "(subrelation mother loves)",
                "(instance loves Relation)"
        );
        KB kb  = buildKB(before);
        KB ref = buildKB(concat(before, "(subrelation biologicalMother mother)"));

        kb.kbCache.addSubrelation("biologicalMother", "mother");

        Set<String> incParents = kb.kbCache.parents.get("subrelation").get("biologicalMother");
        Set<String> refParents = ref.kbCache.parents.get("subrelation").get("biologicalMother");

        assertNotNull("parents[subrelation][biologicalMother] must not be null", incParents);
        assertEquals("parents[subrelation][biologicalMother] must match full rebuild",
                refParents, incParents);
    }

    /**
     * After addSubrelation("biologicalMother","mother"),
     * children["subrelation"]["mother"] must contain "biologicalMother".
     */
    @Test
    public void testAddSubrelation_children() {

        String[] before = concat(CORE,
                "(instance mother Relation)",
                "(instance biologicalMother Relation)"
        );
        KB kb = buildKB(before);

        kb.kbCache.addSubrelation("biologicalMother", "mother");

        Set<String> kids = kb.kbCache.children.get("subrelation") != null
                ? kb.kbCache.children.get("subrelation").get("mother") : null;
        assertNotNull("children[subrelation][mother] must not be null", kids);
        assertTrue("children[subrelation][mother] must contain biologicalMother",
                kids.contains("biologicalMother"));
    }

    /**
     * addSubrelation must inherit parent's domain signatures into the child when
     * the child has no explicit binding.
     */
    @Test
    public void testAddSubrelation_inheritsDomains() {

        String[] before = concat(CORE,
                "(instance mother Relation)",
                "(domain mother 1 Human)",
                "(instance biologicalMother Relation)"
        );
        KB kb = buildKB(before);

        kb.kbCache.addSubrelation("biologicalMother", "mother");

        List<String> sig = kb.kbCache.signatures.get("biologicalMother");
        assertNotNull("signatures[biologicalMother] must not be null", sig);
        assertTrue(sig.size() > 1);
        assertEquals("biologicalMother must inherit domain 1 from mother", "Human", sig.get(1));
    }

    // ------------------------------------------------------------------
    // addDisjoint tests
    // ------------------------------------------------------------------

    /**
     * After addDisjoint("Vertebrate","Invertebrate"), disjoint must contain
     * the tab-separated pair in both orderings.
     */
    @Test
    public void testAddDisjoint_directPair() {

        String[] before = concat(CORE,
                "(subclass Vertebrate Entity)",
                "(subclass Invertebrate Entity)"
        );
        KB kb = buildKB(before);

        kb.kbCache.addDisjoint("Vertebrate", "Invertebrate");

        assertTrue("disjoint must contain Vertebrate\\tInvertebrate",
                kb.kbCache.disjoint.contains("Vertebrate\tInvertebrate"));
        assertTrue("disjoint must contain Invertebrate\\tVertebrate (symmetric)",
                kb.kbCache.disjoint.contains("Invertebrate\tVertebrate"));
    }

    /**
     * addDisjoint must also add all subclass children to the disjoint set.
     * Dog is a subclass of Vertebrate; Jellyfish is a subclass of Invertebrate.
     * After addDisjoint("Vertebrate","Invertebrate"), Dog and Jellyfish must be disjoint.
     */
    @Test
    public void testAddDisjoint_childrenAreDisjoint() {

        String[] before = concat(CORE,
                "(subclass Vertebrate Entity)",
                "(subclass Invertebrate Entity)",
                "(subclass Dog Vertebrate)",
                "(subclass Jellyfish Invertebrate)"
        );
        KB kb  = buildKB(before);
        KB ref = buildKB(concat(before, "(disjoint Vertebrate Invertebrate)"));

        kb.kbCache.addDisjoint("Vertebrate", "Invertebrate");

        assertTrue("Dog and Jellyfish must be disjoint after addDisjoint(Vertebrate, Invertebrate)",
                kb.kbCache.disjoint.contains("Dog\tJellyfish") ||
                kb.kbCache.disjoint.contains("Jellyfish\tDog"));

        // Verify against full-rebuild reference
        // (reference may have additional pairs from symmetric expansion; both must agree on Dog/Jellyfish)
        assertTrue("reference must also see Dog/Jellyfish disjoint",
                ref.kbCache.checkDisjoint(ref, "Dog", "Jellyfish"));
        assertTrue("incremental must also see Dog/Jellyfish disjoint",
                kb.kbCache.checkDisjoint(kb, "Dog", "Jellyfish"));
    }

    /**
     * addDisjoint must update explicitDisjoint symmetrically.
     */
    @Test
    public void testAddDisjoint_explicitDisjointUpdated() {

        String[] before = concat(CORE,
                "(subclass Vertebrate Entity)",
                "(subclass Invertebrate Entity)"
        );
        KB kb = buildKB(before);

        kb.kbCache.addDisjoint("Vertebrate", "Invertebrate");

        assertTrue("explicitDisjoint[Vertebrate] must contain Invertebrate",
                kb.kbCache.explicitDisjoint.getOrDefault("Vertebrate", Collections.emptySet())
                        .contains("Invertebrate"));
        assertTrue("explicitDisjoint[Invertebrate] must contain Vertebrate (symmetric)",
                kb.kbCache.explicitDisjoint.getOrDefault("Invertebrate", Collections.emptySet())
                        .contains("Vertebrate"));
    }

    // ------------------------------------------------------------------
    // Return value (affected terms) sanity checks
    // ------------------------------------------------------------------

    @Test
    public void testAddSubclass_returnsAffectedTerms() {

        String[] before = concat(CORE, "(subclass Agent Entity)");
        KB kb = buildKB(before);
        Set<String> affected = kb.kbCache.addSubclass("Robot", "Agent");
        assertTrue("affected must contain the new child class Robot", affected.contains("Robot"));
        assertTrue("affected must contain the parent Agent", affected.contains("Agent"));
    }

    @Test
    public void testAddInstance_returnsAffectedTerms() {

        String[] before = concat(CORE, "(subclass Robot Entity)");
        KB kb = buildKB(before);
        Set<String> affected = kb.kbCache.addInstance("myRobot", "Robot");
        assertTrue("affected must contain the instance myRobot", affected.contains("myRobot"));
        assertTrue("affected must contain the class Robot", affected.contains("Robot"));
    }

    @Test
    public void testAddDomain_returnsAffectedTerms() {

        String[] before = concat(CORE, "(instance loves Relation)");
        KB kb = buildKB(before);
        Set<String> affected = kb.kbCache.addDomain("loves", 1, "Agent");
        assertTrue("affected must contain loves", affected.contains("loves"));
    }

    @Test
    public void testAddDisjoint_returnsAffectedTerms() {

        String[] before = concat(CORE,
                "(subclass Vertebrate Entity)",
                "(subclass Invertebrate Entity)"
        );
        KB kb = buildKB(before);
        Set<String> affected = kb.kbCache.addDisjoint("Vertebrate", "Invertebrate");
        assertTrue("affected must contain Vertebrate", affected.contains("Vertebrate"));
        assertTrue("affected must contain Invertebrate", affected.contains("Invertebrate"));
    }

    // ------------------------------------------------------------------
    // Utility
    // ------------------------------------------------------------------

    private static String[] concat(String[] a, String... b) {
        String[] result = new String[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
