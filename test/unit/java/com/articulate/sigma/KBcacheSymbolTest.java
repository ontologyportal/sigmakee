package com.articulate.sigma;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for the M5 symbol-indexed taxonomy in {@link KBcache}:
 * {@link KBcache#buildSymbolTaxonomy()}, {@link KBcache#getParentsSymbol},
 * and {@link KBcache#getChildrenSymbol}.
 */
public class KBcacheSymbolTest {

    private static final String[] CORE = {
            "(instance subclass TransitiveRelation)",
            "(instance subrelation TransitiveRelation)",
            "(subclass TransitiveRelation Relation)",
            "(subclass Relation Entity)",
    };

    /** Build a fresh KB+cache from a list of KIF statements. */
    private static KB buildKB(String... kifStatements) {
        KB kb = new KB("TestSymKB");
        kb.kbCache = new KBcache(kb);
        KBmanager.getMgr().setPref("cacheDisjoint", "false");
        KIFAST kif = new KIFAST();
        for (String stmt : kifStatements)
            kif.parseStatement(stmt);
        kb.merge(kif, "");
        for (Formula f : kb.formulaMap.values())
            f.sourceFile = "test";
        kb.kbCache.buildCaches(); // calls buildSymbolTaxonomy() internally
        return kb;
    }

    // ------------------------------------------------------------------
    // SymbolTable is populated
    // ------------------------------------------------------------------

    @Test
    public void testSymbolTableNonNull() {
        KB kb = buildKB(CORE);
        assertNotNull(kb.kbCache.getSymbolTable());
    }

    @Test
    public void testSymbolTableContainsKnownTerms() {
        KB kb = buildKB(CORE);
        SymbolTable st = kb.kbCache.getSymbolTable();
        assertNotNull(st);
        assertTrue(st.contains("Entity"));
        assertTrue(st.contains("Relation"));
        assertTrue(st.contains("TransitiveRelation"));
    }

    // ------------------------------------------------------------------
    // getParentsSymbol matches string-map result
    // ------------------------------------------------------------------

    @Test
    public void testGetParentsSymbolMatchesStringMap() {
        KB kb = buildKB(CORE);
        KBcache cache = kb.kbCache;

        Set<String> strParents = new HashSet<>();
        Map<String, Set<String>> relMap = cache.parents.get("subclass");
        if (relMap != null && relMap.get("TransitiveRelation") != null)
            strParents.addAll(relMap.get("TransitiveRelation"));

        Set<String> symParents = cache.getParentsSymbol("TransitiveRelation", "subclass");
        assertEquals(strParents, symParents);
    }

    @Test
    public void testGetParentsSymbolTransitiveAncestors() {
        KB kb = buildKB(CORE);
        // TransitiveRelation subclass Relation, Relation subclass Entity
        // → transitive parents of TransitiveRelation should include Entity
        Set<String> parents = kb.kbCache.getParentsSymbol("TransitiveRelation", "subclass");
        assertTrue("Expected Entity in transitive parents", parents.contains("Entity"));
        assertTrue("Expected Relation in transitive parents", parents.contains("Relation"));
    }

    @Test
    public void testGetParentsSymbolUnknownTermReturnsEmpty() {
        KB kb = buildKB(CORE);
        Set<String> parents = kb.kbCache.getParentsSymbol("NoSuchTerm", "subclass");
        assertTrue(parents.isEmpty());
    }

    @Test
    public void testGetParentsSymbolUnknownRelReturnsEmpty() {
        KB kb = buildKB(CORE);
        Set<String> parents = kb.kbCache.getParentsSymbol("Entity", "noSuchRel");
        assertTrue(parents.isEmpty());
    }

    // ------------------------------------------------------------------
    // getChildrenSymbol matches string-map result
    // ------------------------------------------------------------------

    @Test
    public void testGetChildrenSymbolMatchesStringMap() {
        KB kb = buildKB(CORE);
        KBcache cache = kb.kbCache;

        Set<String> strChildren = new HashSet<>();
        Map<String, Set<String>> relMap = cache.children.get("subclass");
        if (relMap != null && relMap.get("Entity") != null)
            strChildren.addAll(relMap.get("Entity"));

        Set<String> symChildren = cache.getChildrenSymbol("Entity", "subclass");
        assertEquals(strChildren, symChildren);
    }

    @Test
    public void testGetChildrenSymbolIncludesDescendants() {
        KB kb = buildKB(CORE);
        // Entity → Relation → TransitiveRelation
        Set<String> children = kb.kbCache.getChildrenSymbol("Entity", "subclass");
        assertTrue("Expected Relation in children of Entity", children.contains("Relation"));
        assertTrue("Expected TransitiveRelation in children of Entity", children.contains("TransitiveRelation"));
    }

    @Test
    public void testGetChildrenSymbolUnknownTermReturnsEmpty() {
        KB kb = buildKB(CORE);
        Set<String> children = kb.kbCache.getChildrenSymbol("NoSuchTerm", "subclass");
        assertTrue(children.isEmpty());
    }

    // ------------------------------------------------------------------
    // Fallback when taxonomy not built
    // ------------------------------------------------------------------

    @Test
    public void testFallbackWhenTaxonomyNotBuilt() {
        // Manually create a KBcache without calling buildCaches()
        KB kb = new KB("TestFallbackKB");
        kb.kbCache = new KBcache(kb);
        // symbols/parentsBySymbol/childrenBySymbol are all null — fallback to string map
        Set<String> parents = kb.kbCache.getParentsSymbol("Dog", "subclass");
        assertTrue(parents.isEmpty()); // empty string map, not an exception
    }
}
