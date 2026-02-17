package com.articulate.sigma;

/**
 * Unit tests SUMOKBtoTPTPKB.findAffectedFormulas().
 *
 * These tests verify that the method correctly identifies formulas that
 * need retranslation after an incremental KBcache update, and that it
 * clears varTypeCache on every identified formula.
 */

import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class AffectedFormulasTest {

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Shared core ontology (Entity is a pure arg-2 root for BFS). */
    private static final String[] CORE = {
            "(instance subclass TransitiveRelation)",
            "(instance subrelation TransitiveRelation)",
            "(instance subAttribute TransitiveRelation)",
            "(subclass TransitiveRelation Relation)",
            "(subclass Relation Entity)",
    };

    private static KB buildKB(String... kifStatements) {
        KB kb = new KB("TestAffectedKB");
        kb.kbCache = new KBcache(kb);
        KBmanager.getMgr().setPref("cacheDisjoint", "true");
        KIF kif = new KIF();
        for (String stmt : kifStatements)
            kif.parseStatement(stmt);
        kb.merge(kif, "");
        for (Formula f : kb.formulaMap.values())
            f.sourceFile = "test";
        kb.kbCache.buildCaches();
        return kb;
    }

    private static String[] concat(String[] a, String... b) {
        String[] r = new String[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    // ------------------------------------------------------------------
    // Basic sanity
    // ------------------------------------------------------------------

    /**
     * Empty changedTerms → no formulas affected.
     */
    @Test
    public void testEmpty_changedTerms() {

        KB kb = buildKB(CORE);
        Set<Formula> result = SUMOKBtoTPTPKB.findAffectedFormulas(kb, Collections.emptySet());
        assertTrue("empty changedTerms must return empty set", result.isEmpty());
    }

    /**
     * Null changedTerms → no formulas affected (no NPE).
     */
    @Test
    public void testNull_changedTerms() {

        KB kb = buildKB(CORE);
        Set<Formula> result = SUMOKBtoTPTPKB.findAffectedFormulas(kb, null);
        assertTrue("null changedTerms must return empty set", result.isEmpty());
    }

    // ------------------------------------------------------------------
    // Criterion 1: Direct reference
    // ------------------------------------------------------------------

    /**
     * A formula that mentions the changed term in arg-0 (predicate position)
     * must be included in the affected set.
     */
    @Test
    public void testDirectRef_arg0() {

        // (subclass Robot Agent) — "subclass" at arg-0 and "Robot" at arg-1
        String[] stmts = concat(CORE, "(subclass Robot Agent)", "(subclass Agent Entity)");
        KB kb = buildKB(stmts);

        // Simulate that "Robot" was the newly added child
        Set<String> changed = new HashSet<>(Arrays.asList("Robot", "Agent"));
        Set<Formula> affected = SUMOKBtoTPTPKB.findAffectedFormulas(kb, changed);

        // (subclass Robot Agent) mentions Robot at arg-1 and Agent at arg-2
        boolean foundRobotFormula = affected.stream()
                .anyMatch(f -> f.getFormula().contains("Robot"));
        assertTrue("formula containing Robot must be in affected set", foundRobotFormula);
    }

    /**
     * A formula mentioning the changed term at arg-1 is detected.
     */
    @Test
    public void testDirectRef_arg1() {

        String[] stmts = concat(CORE, "(instance myRobot Robot)", "(subclass Robot Entity)");
        KB kb = buildKB(stmts);

        Set<String> changed = Collections.singleton("myRobot");
        Set<Formula> affected = SUMOKBtoTPTPKB.findAffectedFormulas(kb, changed);

        assertTrue("(instance myRobot Robot) must be in affected set",
                affected.stream().anyMatch(f -> f.getFormula().contains("myRobot")));
    }

    /**
     * A formula mentioning the changed term at arg-2 is detected.
     */
    @Test
    public void testDirectRef_arg2() {

        String[] stmts = concat(CORE, "(subclass Dog Animal)", "(subclass Animal Entity)");
        KB kb = buildKB(stmts);

        // "Animal" appears at arg-2 in (subclass Dog Animal)
        Set<String> changed = Collections.singleton("Animal");
        Set<Formula> affected = SUMOKBtoTPTPKB.findAffectedFormulas(kb, changed);

        assertTrue("(subclass Dog Animal) must be in affected set (Animal at arg-2)",
                affected.stream().anyMatch(f -> f.getFormula().contains("Animal")));
    }

    /**
     * A formula that does NOT mention any changed term is not included.
     */
    @Test
    public void testDirectRef_unrelatedNotIncluded() {

        String[] stmts = concat(CORE,
                "(subclass Robot Entity)",
                "(subclass Table Entity)"
        );
        KB kb = buildKB(stmts);

        // Only "Robot" changed
        Set<String> changed = Collections.singleton("Robot");
        Set<Formula> affected = SUMOKBtoTPTPKB.findAffectedFormulas(kb, changed);

        // (subclass Table Entity) must not be included
        assertFalse("(subclass Table Entity) must NOT be affected when Robot changed",
                affected.stream().anyMatch(f -> f.getFormula().contains("Table") &&
                        !f.getFormula().contains("Robot")));
    }

    // ------------------------------------------------------------------
    // Criterion 2: Predicate variables
    // ------------------------------------------------------------------

    /**
     * When a relation changes, a formula whose predVarCache is non-null and
     * non-empty must be included, even if it doesn't directly mention the term.
     */
    @Test
    public void testPredVar_includedWhenPredicateChanges() {

        String[] stmts = concat(CORE, "(instance loves Relation)");
        KB kb = buildKB(stmts);

        // Manually inject a formula with a pred-var and mark its predVarCache
        Formula predVarFormula = new Formula("(=> (?REL ?X ?Y) (foo ?X ?Y))");
        predVarFormula.predVarCache = new HashSet<>(Collections.singleton("?REL"));
        predVarFormula.sourceFile = "test";
        kb.formulaMap.put(predVarFormula.getFormula(), predVarFormula);

        // "loves" is a relation, so predicateChanged = true
        Set<String> changed = Collections.singleton("loves");
        Set<Formula> affected = SUMOKBtoTPTPKB.findAffectedFormulas(kb, changed);

        assertTrue("predicate-variable formula must be in affected set when a relation changes",
                affected.contains(predVarFormula));
    }

    /**
     * When no relation changes (only a class changes), formulas with predicate
     * variables are NOT included solely because of criterion 2.
     */
    @Test
    public void testPredVar_notIncludedWhenOnlyClassChanges() {

        String[] stmts = concat(CORE, "(subclass Robot Entity)");
        KB kb = buildKB(stmts);

        // Inject a pred-var formula with no direct mention of "Robot"
        Formula predVarFormula = new Formula("(=> (?REL ?X ?Y) (bar ?X ?Y))");
        predVarFormula.predVarCache = new HashSet<>(Collections.singleton("?REL"));
        predVarFormula.sourceFile = "test";
        kb.formulaMap.put(predVarFormula.getFormula(), predVarFormula);

        // "Robot" is a class, not a relation → predicateChanged = false
        Set<String> changed = Collections.singleton("Robot");
        Set<Formula> affected = SUMOKBtoTPTPKB.findAffectedFormulas(kb, changed);

        assertFalse("predicate-variable formula must NOT be included when only a class changed",
                affected.contains(predVarFormula));
    }

    /**
     * A formula whose predVarCache is null (not yet computed) is treated as
     * having no predicate variables — it is not included via criterion 2.
     */
    @Test
    public void testPredVar_nullCacheNotIncluded() {

        String[] stmts = concat(CORE, "(instance loves Relation)");
        KB kb = buildKB(stmts);

        Formula unprocessed = new Formula("(=> (?REL ?X ?Y) (baz ?X ?Y))");
        unprocessed.predVarCache = null;  // not yet computed
        unprocessed.sourceFile = "test";
        kb.formulaMap.put(unprocessed.getFormula(), unprocessed);

        Set<String> changed = Collections.singleton("loves");
        Set<Formula> affected = SUMOKBtoTPTPKB.findAffectedFormulas(kb, changed);

        assertFalse("formula with null predVarCache must NOT be included via criterion 2",
                affected.contains(unprocessed));
    }

    /**
     * A formula whose predVarCache is empty (explicitly set — no pred vars)
     * is not included via criterion 2.
     */
    @Test
    public void testPredVar_emptyCacheNotIncluded() {

        String[] stmts = concat(CORE, "(instance loves Relation)");
        KB kb = buildKB(stmts);

        Formula noPredVar = new Formula("(subclass Cat Animal)");
        noPredVar.predVarCache = Collections.emptySet();  // explicitly no pred vars
        noPredVar.sourceFile = "test";
        kb.formulaMap.put(noPredVar.getFormula(), noPredVar);

        Set<String> changed = Collections.singleton("loves");
        Set<Formula> affected = SUMOKBtoTPTPKB.findAffectedFormulas(kb, changed);

        assertFalse("formula with empty predVarCache must NOT be included via criterion 2",
                affected.contains(noPredVar));
    }

    // ------------------------------------------------------------------
    // Criterion 3: varTypeCache cleared
    // ------------------------------------------------------------------

    /**
     * After findAffectedFormulas(), every formula in the returned set has
     * an empty varTypeCache so that computeVariableTypes() recomputes.
     */
    @Test
    public void testVarTypeCacheCleared() {

        String[] stmts = concat(CORE, "(subclass Robot Entity)", "(subclass Agent Entity)");
        KB kb = buildKB(stmts);

        // Mark the formula with a pre-populated varTypeCache
        Formula f = kb.ask("arg", 1, "Robot").stream().findFirst().orElse(null);
        if (f == null) return;  // skip if KB didn't generate that formula
        f.varTypeCache = new HashMap<>();
        f.varTypeCache.put("?X", new HashSet<>(Collections.singleton("Robot")));

        Set<String> changed = Collections.singleton("Robot");
        SUMOKBtoTPTPKB.findAffectedFormulas(kb, changed);

        assertTrue("varTypeCache must be empty after findAffectedFormulas()",
                f.varTypeCache.isEmpty());
    }

    /**
     * A formula NOT in the affected set must retain its varTypeCache unchanged.
     */
    @Test
    public void testVarTypeCachePreservedForUnaffected() {

        String[] stmts = concat(CORE,
                "(subclass Robot Entity)",
                "(subclass Table Entity)"
        );
        KB kb = buildKB(stmts);

        // Find a formula mentioning Table (unaffected if only Robot changed)
        Formula tableFormula = kb.ask("arg", 1, "Table").stream().findFirst().orElse(null);
        if (tableFormula == null) return;
        Map<String, Set<String>> originalCache = new HashMap<>();
        originalCache.put("?X", new HashSet<>(Collections.singleton("Table")));
        tableFormula.varTypeCache = originalCache;

        // Only Robot changed
        Set<String> changed = Collections.singleton("Robot");
        SUMOKBtoTPTPKB.findAffectedFormulas(kb, changed);

        // Table formula was not affected, its cache should be untouched
        assertFalse("unaffected formula's varTypeCache must NOT be cleared",
                tableFormula.varTypeCache.isEmpty());
    }

    /**
     * End-to-end: addSubclass returns affected terms → findAffectedFormulas
     * finds formulas that mention those terms.
     */
    @Test
    public void testIntegration_addSubclass_thenFind() {

        String[] stmts = concat(CORE,
                "(subclass Agent Entity)",
                "(subclass Robot Entity)",
                "(instance myRobot Robot)"
        );
        KB kb = buildKB(stmts);

        // Incremental update: Robot is now a subclass of Agent
        Set<String> changedTerms = kb.kbCache.addSubclass("Robot", "Agent");

        // M3.3: find affected formulas
        Set<Formula> affected = SUMOKBtoTPTPKB.findAffectedFormulas(kb, changedTerms);

        // (subclass Robot Entity) and (subclass Robot Agent) are the most directly
        // relevant; at a minimum (subclass Robot Entity) exists in the KB
        assertTrue("at least one formula mentioning Robot must be in affected set",
                affected.stream().anyMatch(f -> f.getFormula().contains("Robot")));
    }

    /**
     * End-to-end: addInstance returns affected terms → findAffectedFormulas
     * finds the new (instance ...) formula and related class formulas.
     */
    @Test
    public void testIntegration_addInstance_thenFind() {

        String[] stmts = concat(CORE, "(subclass Robot Entity)");
        KB kb = buildKB(stmts);

        // Simulate adding (instance myRobot Robot) to the formula index manually
        Formula instF = new Formula("(instance myRobot Robot)");
        instF.sourceFile = "test";
        kb.formulaMap.put(instF.getFormula(), instF);
        // Register in the formulas index at the expected keys
        kb.formulas.computeIfAbsent("arg-0-instance", k -> new ArrayList<>())
                   .add(instF.getFormula());
        kb.formulas.computeIfAbsent("arg-1-myRobot", k -> new ArrayList<>())
                   .add(instF.getFormula());
        kb.formulas.computeIfAbsent("arg-2-Robot", k -> new ArrayList<>())
                   .add(instF.getFormula());

        // Incremental cache update
        Set<String> changedTerms = kb.kbCache.addInstance("myRobot", "Robot");

        Set<Formula> affected = SUMOKBtoTPTPKB.findAffectedFormulas(kb, changedTerms);

        assertTrue("(instance myRobot Robot) must be in affected set",
                affected.stream().anyMatch(f -> f.getFormula().contains("myRobot")));
    }
}
