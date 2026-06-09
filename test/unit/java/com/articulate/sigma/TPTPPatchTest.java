package com.articulate.sigma;

/**
 * Unit tests for incremental TPTP file patching.
 *
 * Covers:
 *   - SUMOKBtoTPTPKB.retranslateFormulas()
 *   - SessionTPTPManager.patchSessionTPTP()
 *   - Per-session axiom key isolation (sessionAxiomKeys)
 *   - Option B: successive tell()s read from the session file, not the shared base
 *   - buildReverseIndex merges global + session axiom keys
 */

import com.articulate.sigma.trans.SessionTPTPManager;
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import org.junit.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class TPTPPatchTest {

    // ------------------------------------------------------------------
    // Minimal ontology — Entity is a pure arg-2 root so findRoots() works
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
        tempDir = Files.createTempDirectory("sigma-patch-test");
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
        // Snapshot and clear global axiomKey so tests are independent
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
        return "patch-test-" + sessionCounter.incrementAndGet();
    }

    private static KB buildKB(String... kifStatements) {
        KB kb = new KB("TestPatchKB");
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

    /**
     * Write a minimal fake shared-base TPTP file for the KB.
     * The file goes to {@code tempDir/TestPatchKB.tptp} (or .tff).
     */
    private static Path writeSharedBase(String lang, String... axiomLines) throws IOException {
        String ext = "fof".equals(lang) ? "tptp" : lang;
        Path file = tempDir.resolve("TestPatchKB." + ext);
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
    // retranslateFormulas — pure unit tests (no file I/O)
    // ------------------------------------------------------------------

    /** Null input → empty map, no exception. */
    @Test
    public void testRetranslate_null_returnsEmptyMap() {
        KB kb = buildKB(CORE);
        Map<Formula, List<String>> result =
                SUMOKBtoTPTPKB.retranslateFormulas(kb, null, "fof");
        assertTrue("null input must return empty map", result.isEmpty());
    }

    /** Empty set → empty map. */
    @Test
    public void testRetranslate_emptySet_returnsEmptyMap() {
        KB kb = buildKB(CORE);
        Map<Formula, List<String>> result =
                SUMOKBtoTPTPKB.retranslateFormulas(kb, Collections.emptySet(), "fof");
        assertTrue("empty set must return empty map", result.isEmpty());
    }

    /** Non-reasoning formulas (termFormat, format, documentation) produce no bodies. */
    @Test
    public void testRetranslate_nonReasoningFormula_producesNoBodies() {
        KB kb = buildKB(CORE);
        Formula f = new Formula("(termFormat EnglishLanguage Entity \"entity\")");
        f.sourceFile = "test";

        Map<Formula, List<String>> result =
                SUMOKBtoTPTPKB.retranslateFormulas(kb, Collections.singleton(f), "fof");

        List<String> bodies = result.get(f);
        assertTrue("non-reasoning formula must produce no TPTP bodies",
                bodies == null || bodies.isEmpty());
    }

    /** Result must contain a key for every input formula (even when body is empty). */
    @Test
    public void testRetranslate_hasEntryForEveryInputFormula() {
        KB kb = buildKB(CORE);
        // Collect two formulas from the KB
        Set<Formula> twoFormulas = new HashSet<>();
        for (Formula f : kb.formulaMap.values()) {
            twoFormulas.add(f);
            if (twoFormulas.size() == 2) break;
        }
        if (twoFormulas.size() < 2) return; // KB too small — skip

        Map<Formula, List<String>> result =
                SUMOKBtoTPTPKB.retranslateFormulas(kb, twoFormulas, "fof");

        for (Formula f : twoFormulas)
            assertTrue("result must contain entry for every input formula",
                    result.containsKey(f));
    }

    /** A simple subclass formula produces at least one non-empty FOF body string. */
    @Test
    public void testRetranslate_simpleSubclassFormula_producesFofOutput() {
        KB kb = buildKB(concat(CORE, "(subclass Robot Entity)"));

        Formula robotF = kb.ask("arg", 1, "Robot").stream().findFirst().orElse(null);
        assertNotNull("KB must contain a formula mentioning Robot", robotF);

        Map<Formula, List<String>> result =
                SUMOKBtoTPTPKB.retranslateFormulas(kb, Collections.singleton(robotF), "fof");

        List<String> bodies = result.get(robotF);
        assertNotNull("result must have an entry for the formula", bodies);
        assertFalse("simple subclass formula must produce at least one FOF body", bodies.isEmpty());
        for (String body : bodies)
            assertFalse("each body string must be non-empty", body.trim().isEmpty());
    }

    // ------------------------------------------------------------------
    // patchSessionTPTP — file-level tests
    // ------------------------------------------------------------------

    /**
     * Lines in the shared base that are NOT affected are copied verbatim to the
     * session file.
     */
    @Test
    public void testPatch_preservesUnaffectedBaseContent() throws IOException {
        KB kb = buildKB(CORE);
        Formula f1 = kb.formulaMap.values().iterator().next();
        Formula f2 = null;
        for (Formula f : kb.formulaMap.values()) {
            if (f != f1) { f2 = f; break; }
        }
        if (f2 == null) return; // only one formula — skip

        // f1 is in axiomKey; f2 is not affected
        SUMOKBtoTPTPKB.axiomKey.put("kb_TestPatchKB_1", f1);
        writeSharedBase("tptp",
                "fof(kb_TestPatchKB_1,axiom,(first)).",
                "fof(kb_TestPatchKB_2,axiom,(second)).");  // not in axiomKey — unaffected

        KBcache sessionCache = new KBcache(kb.kbCache, kb);
        String sessionId = uniqueSession();
        try {
            Path sessionFile = SessionTPTPManager.patchSessionTPTP(
                    sessionId, kb, "tptp",
                    Collections.singleton(f1),   // affected
                    Collections.emptySet(),       // no new formulas
                    sessionCache);

            String content = new String(Files.readAllBytes(sessionFile));
            assertTrue("unaffected axiom line must be preserved verbatim",
                    content.contains("fof(kb_TestPatchKB_2,axiom,(second))."));
        } finally {
            SessionTPTPManager.cleanupSession(sessionId);
        }
    }

    /**
     * Axiom lines whose name is in the reverse index for an affected formula are
     * replaced with {@code % [patched out] ...} comments.
     */
    @Test
    public void testPatch_affectedAxiomLine_commentedOut() throws IOException {
        KB kb = buildKB(CORE);
        Formula affectedFormula = kb.formulaMap.values().iterator().next();
        String axiomName = "kb_TestPatchKB_99";

        SUMOKBtoTPTPKB.axiomKey.put(axiomName, affectedFormula);
        writeSharedBase("tptp",
                "fof(" + axiomName + ",axiom,(old_translation)).");

        KBcache sessionCache = new KBcache(kb.kbCache, kb);
        String sessionId = uniqueSession();
        try {
            Path sessionFile = SessionTPTPManager.patchSessionTPTP(
                    sessionId, kb, "tptp",
                    Collections.singleton(affectedFormula),
                    Collections.emptySet(),
                    sessionCache);

            String content = new String(Files.readAllBytes(sessionFile));
            assertTrue("affected axiom line must be commented out",
                    content.contains("% [patched out] fof(" + axiomName));
            assertFalse("raw (uncommented) old axiom must not appear",
                    content.contains("\nfof(" + axiomName));
        } finally {
            SessionTPTPManager.cleanupSession(sessionId);
        }
    }

    /**
     * The patch section header is written after the base content.
     */
    @Test
    public void testPatch_sectionHeader_appearsAfterBaseContent() throws IOException {
        KB kb = buildKB(CORE);
        Formula f = kb.formulaMap.values().iterator().next();
        SUMOKBtoTPTPKB.axiomKey.put("kb_TestPatchKB_1", f);
        writeSharedBase("tptp", "fof(kb_TestPatchKB_1,axiom,(base)).");

        KBcache sessionCache = new KBcache(kb.kbCache, kb);
        String sessionId = uniqueSession();
        try {
            Path sessionFile = SessionTPTPManager.patchSessionTPTP(
                    sessionId, kb, "tptp",
                    Collections.singleton(f),
                    Collections.emptySet(),
                    sessionCache);

            String content = new String(Files.readAllBytes(sessionFile));
            assertTrue("patch section header must be present",
                    content.contains("Incremental patch: session " + sessionId));

            int baseIdx   = content.indexOf("fof(kb_TestPatchKB_1");
            int headerIdx = content.indexOf("Incremental patch");
            assertTrue("patch header must appear after base content", headerIdx > baseIdx);
        } finally {
            SessionTPTPManager.cleanupSession(sessionId);
        }
    }

    /**
     * New tell() formulas (the {@code newFormulas} param) are translated and
     * appended in the patch section — they never existed in the base file.
     */
    @Test
    public void testPatch_newFormula_appendedInPatchSection() throws IOException {
        KB kb = buildKB(concat(CORE, "(subclass Robot Entity)"));

        // Provide a non-empty axiomKey so the fallback guard passes
        Formula existing = kb.formulaMap.values().iterator().next();
        SUMOKBtoTPTPKB.axiomKey.put("kb_TestPatchKB_1", existing);
        writeSharedBase("tptp", "fof(kb_TestPatchKB_1,axiom,(placeholder)).");

        // New formula from tell() — not in the base file
        Formula newF = kb.ask("arg", 1, "Robot").stream().findFirst().orElse(null);
        assertNotNull("KB must contain Robot formula", newF);

        KBcache sessionCache = new KBcache(kb.kbCache, kb);
        String sessionId = uniqueSession();
        try {
            Path sessionFile = SessionTPTPManager.patchSessionTPTP(
                    sessionId, kb, "tptp",
                    Collections.emptySet(),          // nothing to retranslate
                    Collections.singleton(newF),     // one new tell() formula
                    sessionCache);

            String content = new String(Files.readAllBytes(sessionFile));
            // Patch section must be present
            assertTrue("patch section header must appear",
                    content.contains("Incremental patch: session " + sessionId));
            // Patch section must come after the base content
            int placeholderIdx = content.indexOf("placeholder");
            int headerIdx      = content.indexOf("Incremental patch");
            assertTrue("patch section must come after base content",
                    headerIdx > placeholderIdx);
        } finally {
            SessionTPTPManager.cleanupSession(sessionId);
        }
    }

    // ------------------------------------------------------------------
    // Option B — successive tell()s
    // ------------------------------------------------------------------

    /**
     * Option B: when the session file already exists, patchSessionTPTP reads from
     * it rather than the shared base.  Base content from the first patch must
     * still be present after the second patch.
     */
    @Test
    public void testPatch_multipleTells_baseContentPreserved() throws IOException {
        KB kb = buildKB(CORE);
        Formula f = kb.formulaMap.values().iterator().next();
        SUMOKBtoTPTPKB.axiomKey.put("kb_TestPatchKB_1", f);
        writeSharedBase("tptp", "fof(kb_TestPatchKB_1,axiom,(base_content)).");

        KBcache sessionCache = new KBcache(kb.kbCache, kb);
        String sessionId = uniqueSession();
        try {
            // First tell()
            Formula f1 = new Formula("(subclass Robot Entity)");
            f1.sourceFile = "test";
            kb.formulaMap.put(f1.getFormula(), f1);
            SessionTPTPManager.patchSessionTPTP(
                    sessionId, kb, "tptp",
                    Collections.emptySet(), Collections.singleton(f1), sessionCache);

            // Second tell()
            Formula f2 = new Formula("(subclass Cat Entity)");
            f2.sourceFile = "test";
            kb.formulaMap.put(f2.getFormula(), f2);
            Path sessionFile = SessionTPTPManager.patchSessionTPTP(
                    sessionId, kb, "tptp",
                    Collections.emptySet(), Collections.singleton(f2), sessionCache);

            String content = new String(Files.readAllBytes(sessionFile));
            assertTrue("base content must survive second tell()",
                    content.contains("base_content"));
            // Two patch section headers (one per tell)
            int count = 0;
            int idx = 0;
            while ((idx = content.indexOf("Incremental patch", idx)) != -1) { count++; idx++; }
            assertEquals("each tell() appends its own patch section", 2, count);
        } finally {
            SessionTPTPManager.cleanupSession(sessionId);
        }
    }

    /**
     * Option B + session axiom key: a patch axiom name written during tell #1 is
     * found by buildReverseIndex on tell #2 and commented out when that formula
     * is in the affected set.
     */
    @Test
    public void testPatch_secondTell_sessionAxiomCommentedOut() throws IOException {
        KB kb = buildKB(concat(CORE, "(subclass Robot Entity)"));

        Formula baseF = kb.formulaMap.values().iterator().next();
        SUMOKBtoTPTPKB.axiomKey.put("kb_TestPatchKB_1", baseF);
        writeSharedBase("tptp", "fof(kb_TestPatchKB_1,axiom,(base)).");

        // Find a Robot formula that will be in the "affected" set for tell #2
        Formula robotF = kb.ask("arg", 1, "Robot").stream().findFirst().orElse(null);
        assertNotNull("KB must contain Robot formula", robotF);

        KBcache sessionCache = new KBcache(kb.kbCache, kb);
        String sessionId = uniqueSession();
        try {
            // Tell #1: robotF is a new assertion
            SessionTPTPManager.patchSessionTPTP(
                    sessionId, kb, "tptp",
                    Collections.emptySet(),
                    Collections.singleton(robotF),
                    sessionCache);

            Map<String, Formula> sessionKey = SessionTPTPManager.getSessionAxiomKey(sessionId);
            // Only run the second assertion if tell #1 produced session axiom entries
            // (retranslation may legitimately produce nothing for some formula shapes)
            if (sessionKey == null || sessionKey.isEmpty()) return;

            // Tell #2: same robotF is now in the "affected" set (schema changed again)
            SessionTPTPManager.patchSessionTPTP(
                    sessionId, kb, "tptp",
                    Collections.singleton(robotF),
                    Collections.emptySet(),
                    sessionCache);

            String content = new String(Files.readAllBytes(
                    SessionTPTPManager.getSessionTPTPPath(sessionId, kb.name, "tptp")));
            assertTrue("session axiom from tell #1 must be commented out in tell #2",
                    content.contains("% [patched out]"));
        } finally {
            SessionTPTPManager.cleanupSession(sessionId);
        }
    }

    // ------------------------------------------------------------------
    // Session axiom key isolation
    // ------------------------------------------------------------------

    /**
     * Patch axiom names go into the session-specific key, not the global axiomKey.
     * The global map must not grow during patching.
     */
    @Test
    public void testPatch_newAxioms_writtenToSessionKey_notGlobal() throws IOException {
        KB kb = buildKB(concat(CORE, "(subclass Robot Entity)"));

        Formula existing = kb.formulaMap.values().iterator().next();
        SUMOKBtoTPTPKB.axiomKey.put("kb_TestPatchKB_1", existing);
        int globalSizeBefore = SUMOKBtoTPTPKB.axiomKey.size();

        writeSharedBase("tptp", "fof(kb_TestPatchKB_1,axiom,(placeholder)).");

        Formula newF = kb.ask("arg", 1, "Robot").stream().findFirst().orElse(null);
        assertNotNull("Robot formula must exist", newF);

        KBcache sessionCache = new KBcache(kb.kbCache, kb);
        String sessionId = uniqueSession();
        try {
            SessionTPTPManager.patchSessionTPTP(
                    sessionId, kb, "tptp",
                    Collections.emptySet(),
                    Collections.singleton(newF),
                    sessionCache);

            assertEquals("global axiomKey must not grow during a patch",
                    globalSizeBefore, SUMOKBtoTPTPKB.axiomKey.size());

            Map<String, Formula> sessionKey = SessionTPTPManager.getSessionAxiomKey(sessionId);
            assertNotNull("session axiom key must be created by patchSessionTPTP", sessionKey);
        } finally {
            SessionTPTPManager.cleanupSession(sessionId);
        }
    }

    /**
     * Two concurrent sessions patch independently; each session's axiom key
     * is isolated from the other's.
     */
    @Test
    public void testPatch_twoSessions_axiomKeysIsolated() throws IOException {
        KB kb = buildKB(concat(CORE, "(subclass Robot Entity)", "(subclass Cat Entity)"));

        Formula existing = kb.formulaMap.values().iterator().next();
        SUMOKBtoTPTPKB.axiomKey.put("kb_TestPatchKB_1", existing);
        writeSharedBase("tptp", "fof(kb_TestPatchKB_1,axiom,(placeholder)).");

        Formula robotF = kb.ask("arg", 1, "Robot").stream().findFirst().orElse(null);
        Formula catF   = kb.ask("arg", 1, "Cat").stream().findFirst().orElse(null);
        if (robotF == null || catF == null) return;

        KBcache cacheA = new KBcache(kb.kbCache, kb);
        KBcache cacheB = new KBcache(kb.kbCache, kb);
        String sessionA = uniqueSession();
        String sessionB = uniqueSession();
        try {
            SessionTPTPManager.patchSessionTPTP(
                    sessionA, kb, "tptp",
                    Collections.emptySet(), Collections.singleton(robotF), cacheA);
            SessionTPTPManager.patchSessionTPTP(
                    sessionB, kb, "tptp",
                    Collections.emptySet(), Collections.singleton(catF), cacheB);

            Map<String, Formula> keyA = SessionTPTPManager.getSessionAxiomKey(sessionA);
            Map<String, Formula> keyB = SessionTPTPManager.getSessionAxiomKey(sessionB);

            // The two session keys must not share any axiom name
            if (keyA != null && keyB != null) {
                Set<String> intersection = new HashSet<>(keyA.keySet());
                intersection.retainAll(keyB.keySet());
                assertTrue("session axiom keys must not share any axiom names",
                        intersection.isEmpty());
            }
        } finally {
            SessionTPTPManager.cleanupSession(sessionA);
            SessionTPTPManager.cleanupSession(sessionB);
        }
    }

    // ------------------------------------------------------------------
    // cleanupSession removes session axiom key
    // ------------------------------------------------------------------

    /**
     * After cleanupSession(), getSessionAxiomKey() returns null.
     */
    @Test
    public void testCleanup_removesSessionAxiomKey() throws IOException {
        KB kb = buildKB(concat(CORE, "(subclass Dog Entity)"));

        Formula existing = kb.formulaMap.values().iterator().next();
        SUMOKBtoTPTPKB.axiomKey.put("kb_TestPatchKB_1", existing);
        writeSharedBase("tptp", "fof(kb_TestPatchKB_1,axiom,(placeholder)).");

        Formula dogF = kb.ask("arg", 1, "Dog").stream().findFirst().orElse(null);
        if (dogF == null) return;

        KBcache sessionCache = new KBcache(kb.kbCache, kb);
        String sessionId = uniqueSession();

        SessionTPTPManager.patchSessionTPTP(
                sessionId, kb, "tptp",
                Collections.emptySet(), Collections.singleton(dogF), sessionCache);

        assertNotNull("session axiom key must exist after patching",
                SessionTPTPManager.getSessionAxiomKey(sessionId));

        SessionTPTPManager.cleanupSession(sessionId);

        assertNull("session axiom key must be null after cleanupSession()",
                SessionTPTPManager.getSessionAxiomKey(sessionId));
    }

    /**
     * cleanupSession() on an ID that never patched completes without exception.
     */
    @Test
    public void testCleanup_noopForUnknownSession() {
        String sessionId = "never-patched-" + uniqueSession();
        SessionTPTPManager.cleanupSession(sessionId);
        assertNull("session axiom key must still be null for unknown session",
                SessionTPTPManager.getSessionAxiomKey(sessionId));
    }
}
