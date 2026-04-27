package com.articulate.sigma;

/**
 * Unit tests for M3.5: wiring tell() to the incremental TPTP pipeline.
 *
 * Covers:
 *   - SessionTPTPManager.applyIncrementalUpdate() dispatch for each supported predicate
 *   - Unsupported-predicate fallback to generateSessionTPTP()
 *   - KB.tell() case (B): ground facts on transitive predicates
 *   - Session TPTP file patching end-to-end
 *   - Null / empty sessionId guard
 *   - Successive calls accumulate in the same session KBcache
 */

import com.articulate.sigma.trans.SessionTPTPManager;
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import org.junit.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class IncrementalTellPipelineTest {

    // ------------------------------------------------------------------
    // Minimal ontology — matches TPTPPatchTest / KBcacheIncrementalTest
    // ------------------------------------------------------------------
    private static final String[] CORE = {
            "(instance subclass TransitiveRelation)",
            "(instance subrelation TransitiveRelation)",
            "(instance subAttribute TransitiveRelation)",
            "(subclass TransitiveRelation Relation)",
            "(subclass Relation Entity)",
    };

    // ------------------------------------------------------------------
    // Class-level state
    // ------------------------------------------------------------------

    private static Path tempDir;
    private static final AtomicInteger sessionCounter = new AtomicInteger(0);

    /** Saved copy of the global axiomKey — restored after each test. */
    private Map<String, Formula> savedAxiomKey;

    // ------------------------------------------------------------------
    // Setup / teardown
    // ------------------------------------------------------------------

    @BeforeClass
    public static void setUpClass() throws IOException {
        tempDir = Files.createTempDirectory("sigma-m35-test");
        KBmanager.getMgr().setPref("kbDir", tempDir.toString());
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        Files.walk(tempDir)
             .sorted(Comparator.reverseOrder())
             .forEach(p -> { try { Files.delete(p); } catch (IOException ignore) {} });
    }

    @Before
    public void setUp() {
        savedAxiomKey = new HashMap<>(SUMOKBtoTPTPKB.axiomKey);
        SUMOKBtoTPTPKB.axiomKey.clear();
    }

    @After
    public void tearDown() {
        SUMOKBtoTPTPKB.axiomKey.clear();
        SUMOKBtoTPTPKB.axiomKey.putAll(savedAxiomKey);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static String uniqueSession() {
        return "m35-test-" + sessionCounter.incrementAndGet();
    }

    private static KB buildKB(String... kifStatements) {
        KB kb = new KB("TestM35KB");
        kb.kbCache = new KBcache(kb);
        KBmanager.getMgr().setPref("cacheDisjoint", "true");
        KIFAST kif = new KIFAST();
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

    /**
     * Write a minimal fake shared-base TPTP file for TestM35KB.
     */
    private static Path writeSharedBase(String lang, String... axiomLines) throws IOException {
        String ext = "fof".equals(lang) ? "tptp" : lang;
        Path file = tempDir.resolve("TestM35KB." + ext);
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            w.write("% Test shared base TPTP file");
            w.newLine();
            for (String line : axiomLines) {
                w.write(line);
                w.newLine();
            }
        }
        return file;
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    /**
     * After applyIncrementalUpdate with (subclass Robot Agent),
     * session KBcache must have Robot in parents["subclass"] containing "Agent".
     */
    @Test
    public void testApplyIncrementalUpdate_subclass_cacheUpdated() {
        KB kb = buildKB(concat(CORE, "(subclass Agent Entity)"));
        Formula formula = new Formula("(subclass Robot Agent)");
        formula.sourceFile = "test";
        kb.formulaMap.put(formula.getFormula(), formula);

        String sessionId = uniqueSession();
        try {
            SessionTPTPManager.applyIncrementalUpdate(kb, sessionId, formula, "fof");
            KBcache sessionCache = SessionTPTPManager.getSessionCache(sessionId);
            assertNotNull("session cache must be created", sessionCache);
            Map<String, Set<String>> subclassParents = sessionCache.parents.get("subclass");
            assertNotNull("parents[subclass] must not be null", subclassParents);
            Set<String> robotParents = subclassParents.get("Robot");
            assertNotNull("parents[subclass][Robot] must not be null after addSubclass", robotParents);
            assertTrue("parents[subclass][Robot] must contain 'Agent'", robotParents.contains("Agent"));
        } finally {
            SessionTPTPManager.cleanupSession(sessionId);
        }
    }

    /**
     * After applyIncrementalUpdate with (instance myRobot Robot),
     * session KBcache instanceOf must contain myRobot mapped to Robot (and its ancestors).
     */
    @Test
    public void testApplyIncrementalUpdate_instance_cacheUpdated() {
        KB kb = buildKB(concat(CORE,
                "(subclass Agent Entity)",
                "(subclass Robot Agent)"));
        Formula formula = new Formula("(instance myRobot Robot)");
        formula.sourceFile = "test";
        kb.formulaMap.put(formula.getFormula(), formula);

        String sessionId = uniqueSession();
        try {
            SessionTPTPManager.applyIncrementalUpdate(kb, sessionId, formula, "fof");
            KBcache sessionCache = SessionTPTPManager.getSessionCache(sessionId);
            assertNotNull("session cache must be created", sessionCache);
            Set<String> instanceClasses = sessionCache.instanceOf.get("myRobot");
            assertNotNull("instanceOf[myRobot] must not be null after addInstance", instanceClasses);
            assertTrue("instanceOf[myRobot] must contain 'Robot'", instanceClasses.contains("Robot"));
        } finally {
            SessionTPTPManager.cleanupSession(sessionId);
        }
    }

    /**
     * After applyIncrementalUpdate with (domain drives 1 Vehicle),
     * session KBcache signatures must be updated for "drives".
     */
    @Test
    public void testApplyIncrementalUpdate_domain_cacheUpdated() {
        KB kb = buildKB(concat(CORE,
                "(subclass Vehicle Entity)",
                "(instance drives BinaryRelation)"));
        Formula formula = new Formula("(domain drives 1 Vehicle)");
        formula.sourceFile = "test";
        kb.formulaMap.put(formula.getFormula(), formula);

        String sessionId = uniqueSession();
        try {
            SessionTPTPManager.applyIncrementalUpdate(kb, sessionId, formula, "fof");
            KBcache sessionCache = SessionTPTPManager.getSessionCache(sessionId);
            assertNotNull("session cache must be created", sessionCache);
            List<String> sig = sessionCache.signatures.get("drives");
            // After addDomain(drives, 1, Vehicle), index 1 of the signature should be Vehicle
            assertNotNull("signatures[drives] must not be null after addDomain", sig);
            assertTrue("signatures[drives] must have at least 2 elements", sig.size() >= 2);
            assertEquals("signatures[drives][1] must be 'Vehicle'", "Vehicle", sig.get(1));
        } finally {
            SessionTPTPManager.cleanupSession(sessionId);
        }
    }

    /**
     * After applyIncrementalUpdate with (range age PositiveInteger),
     * session KBcache signatures must be updated for "age" at index 0.
     */
    @Test
    public void testApplyIncrementalUpdate_range_cacheUpdated() {
        KB kb = buildKB(concat(CORE,
                "(subclass PositiveInteger Entity)",
                "(instance age UnaryFunction)"));
        Formula formula = new Formula("(range age PositiveInteger)");
        formula.sourceFile = "test";
        kb.formulaMap.put(formula.getFormula(), formula);

        String sessionId = uniqueSession();
        try {
            SessionTPTPManager.applyIncrementalUpdate(kb, sessionId, formula, "fof");
            KBcache sessionCache = SessionTPTPManager.getSessionCache(sessionId);
            assertNotNull("session cache must be created", sessionCache);
            List<String> sig = sessionCache.signatures.get("age");
            // addRange(age, PositiveInteger) → addDomain(age, 0, PositiveInteger)
            assertNotNull("signatures[age] must not be null after addRange", sig);
            assertFalse("signatures[age] must be non-empty", sig.isEmpty());
            assertEquals("signatures[age][0] must be 'PositiveInteger'", "PositiveInteger", sig.get(0));
        } finally {
            SessionTPTPManager.cleanupSession(sessionId);
        }
    }

    /**
     * After applyIncrementalUpdate with (subrelation directlyControls controls),
     * session KBcache parents["subrelation"]["directlyControls"] must contain "controls".
     */
    @Test
    public void testApplyIncrementalUpdate_subrelation_cacheUpdated() {
        KB kb = buildKB(concat(CORE,
                "(instance controls BinaryRelation)",
                "(instance directlyControls BinaryRelation)"));
        Formula formula = new Formula("(subrelation directlyControls controls)");
        formula.sourceFile = "test";
        kb.formulaMap.put(formula.getFormula(), formula);

        String sessionId = uniqueSession();
        try {
            SessionTPTPManager.applyIncrementalUpdate(kb, sessionId, formula, "fof");
            KBcache sessionCache = SessionTPTPManager.getSessionCache(sessionId);
            assertNotNull("session cache must be created", sessionCache);
            Map<String, Set<String>> subrelParents = sessionCache.parents.get("subrelation");
            assertNotNull("parents[subrelation] must not be null after addSubrelation", subrelParents);
            Set<String> dcParents = subrelParents.get("directlyControls");
            assertNotNull("parents[subrelation][directlyControls] must not be null", dcParents);
            assertTrue("parents[subrelation][directlyControls] must contain 'controls'",
                    dcParents.contains("controls"));
        } finally {
            SessionTPTPManager.cleanupSession(sessionId);
        }
    }

    /**
     * After applyIncrementalUpdate with (disjoint Animal Plant),
     * session KBcache disjoint set must be updated to reflect the new disjointness.
     */
    @Test
    public void testApplyIncrementalUpdate_disjoint_cacheUpdated() {
        KB kb = buildKB(concat(CORE,
                "(subclass Animal Entity)",
                "(subclass Plant Entity)"));
        Formula formula = new Formula("(disjoint Animal Plant)");
        formula.sourceFile = "test";
        kb.formulaMap.put(formula.getFormula(), formula);

        // Snapshot of shared cache disjoint set size before the call
        int sharedSize = kb.kbCache.disjoint.size();

        String sessionId = uniqueSession();
        try {
            SessionTPTPManager.applyIncrementalUpdate(kb, sessionId, formula, "fof");
            KBcache sessionCache = SessionTPTPManager.getSessionCache(sessionId);
            assertNotNull("session cache must be created", sessionCache);
            // The session cache disjoint set should be larger than the shared cache
            // (or at least as large, since addDisjoint adds entries for both directions).
            assertTrue("session cache disjoint set must grow after addDisjoint",
                    sessionCache.disjoint.size() > sharedSize ||
                    sessionCache.disjoint.contains("Animal") ||
                    sessionCache.disjoint.contains("Plant"));
        } finally {
            SessionTPTPManager.cleanupSession(sessionId);
        }
    }

    /**
     * An unsupported predicate (partition) must fall back to generateSessionTPTP()
     * and return a valid (non-null) session path.
     */
    @Test
    public void testApplyIncrementalUpdate_unsupported_fallsBackToFullRegen() throws IOException {
        KB kb = buildKB(concat(CORE,
                "(subclass Animal Entity)",
                "(subclass Plant Entity)",
                "(subclass Fungus Entity)"));

        // Provide a non-empty axiomKey so the fallback guard in patchSessionTPTP passes
        Formula existing = kb.formulaMap.values().iterator().next();
        SUMOKBtoTPTPKB.axiomKey.put("kb_TestM35KB_1", existing);
        writeSharedBase("tptp", "fof(kb_TestM35KB_1,axiom,(placeholder)).");

        Formula formula = new Formula("(partition Entity Animal Plant Fungus)");
        formula.sourceFile = "test";
        kb.formulaMap.put(formula.getFormula(), formula);

        String sessionId = uniqueSession();
        try {
            Path result = SessionTPTPManager.applyIncrementalUpdate(kb, sessionId, formula, "fof");
            assertNotNull("unsupported predicate must return a non-null path via full regen", result);
            assertTrue("returned path must point to an existing file", Files.exists(result));
        } finally {
            SessionTPTPManager.cleanupSession(sessionId);
        }
    }

    /**
     * A null sessionId must cause applyIncrementalUpdate() to return null immediately
     * without creating any session state.
     */
    @Test
    public void testApplyIncrementalUpdate_nullSessionId_returnsNull() {
        KB kb = buildKB(CORE);
        Formula formula = new Formula("(subclass Robot Entity)");
        formula.sourceFile = "test";

        Path result = SessionTPTPManager.applyIncrementalUpdate(kb, null, formula, "fof");
        assertNull("null sessionId must return null", result);
    }

    /**
     * An empty sessionId must also cause applyIncrementalUpdate() to return null.
     */
    @Test
    public void testApplyIncrementalUpdate_emptySessionId_returnsNull() {
        KB kb = buildKB(CORE);
        Formula formula = new Formula("(subclass Robot Entity)");
        formula.sourceFile = "test";

        Path result = SessionTPTPManager.applyIncrementalUpdate(kb, "", formula, "fof");
        assertNull("empty sessionId must return null", result);
    }

    /**
     * Two successive applyIncrementalUpdate calls on the same session accumulate both
     * updates in the session KBcache.
     */
    @Test
    public void testApplyIncrementalUpdate_multipleCalls_sessionCacheAccumulates() {
        KB kb = buildKB(concat(CORE,
                "(subclass Agent Entity)",
                "(subclass Vehicle Entity)"));

        Formula f1 = new Formula("(subclass Robot Agent)");
        f1.sourceFile = "test";
        kb.formulaMap.put(f1.getFormula(), f1);

        Formula f2 = new Formula("(subclass Car Vehicle)");
        f2.sourceFile = "test";
        kb.formulaMap.put(f2.getFormula(), f2);

        String sessionId = uniqueSession();
        try {
            SessionTPTPManager.applyIncrementalUpdate(kb, sessionId, f1, "fof");
            SessionTPTPManager.applyIncrementalUpdate(kb, sessionId, f2, "fof");

            KBcache sessionCache = SessionTPTPManager.getSessionCache(sessionId);
            assertNotNull("session cache must be created", sessionCache);

            Map<String, Set<String>> subclassParents = sessionCache.parents.get("subclass");
            assertNotNull("parents[subclass] must not be null", subclassParents);

            Set<String> robotParents = subclassParents.get("Robot");
            assertNotNull("parents[subclass][Robot] must not be null", robotParents);
            assertTrue("Robot must have Agent in parents after first tell", robotParents.contains("Agent"));

            Set<String> carParents = subclassParents.get("Car");
            assertNotNull("parents[subclass][Car] must not be null", carParents);
            assertTrue("Car must have Vehicle in parents after second tell", carParents.contains("Vehicle"));
        } finally {
            SessionTPTPManager.cleanupSession(sessionId);
        }
    }

    /**
     * After (subclass Robot Agent) + minimal shared base TPTP, the session TPTP file
     * must be created and contain the incremental patch section header.
     */
    @Test
    public void testApplyIncrementalUpdate_patchesSessionTPTPFile() throws IOException {
        KB kb = buildKB(concat(CORE, "(subclass Agent Entity)"));

        // Provide axiomKey and a shared base file so patchSessionTPTP doesn't fall back
        Formula existing = kb.formulaMap.values().iterator().next();
        SUMOKBtoTPTPKB.axiomKey.put("kb_TestM35KB_1", existing);
        writeSharedBase("tptp", "fof(kb_TestM35KB_1,axiom,(placeholder)).");

        Formula formula = new Formula("(subclass Robot Agent)");
        formula.sourceFile = "test";
        kb.formulaMap.put(formula.getFormula(), formula);

        String sessionId = uniqueSession();
        try {
            Path sessionFile = SessionTPTPManager.applyIncrementalUpdate(kb, sessionId, formula, "fof");
            assertNotNull("applyIncrementalUpdate must return a non-null path", sessionFile);
            assertTrue("session TPTP file must exist", Files.exists(sessionFile));
            String content = new String(Files.readAllBytes(sessionFile));
            assertTrue("session TPTP file must contain incremental patch section header",
                    content.contains("Incremental patch: session " + sessionId));
        } finally {
            SessionTPTPManager.cleanupSession(sessionId);
        }
    }
}
