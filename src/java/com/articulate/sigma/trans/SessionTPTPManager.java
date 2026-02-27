/* This code is copyright Articulate Software (c) 2003-2025.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
in any writings, briefings, publications, presentations, or other representations
of any software which incorporates, builds on, or uses this code.

Manages session-specific TPTP file generation for isolated user assertion handling.
This prevents TQ tests from overwriting the shared base TPTP files.
*/

package com.articulate.sigma.trans;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Manages session-specific TPTP file lifecycle for isolated TQ test handling.
 *
 * Each session that requires TPTP regeneration (due to schema-changing assertions)
 * gets its own subdirectory under $SIGMA_HOME/KBs/sessions/{sessionId}/.
 *
 * This ensures that user assertions in TQ tests don't affect the shared base
 * TPTP files used by other sessions.
 */
public class SessionTPTPManager {

    private static final boolean debug = false;

    /**
     * When true, each {@link #patchSessionTPTP} call writes a human-readable log file
     * to the session directory named {@code patch-debug-&lt;timestamp&gt;.log}.
     *
     * <p>The log records:
     * <ul>
     *   <li>Every formula in the <em>affected</em> set (commented out), its axiom
     *       name(s), its retranslated TPTP bodies, and a STATUS flag that warns if
     *       the formula produced no new translation (i.e., was effectively lost).</li>
     *   <li>Every new tell() formula and its TPTP bodies.</li>
     * </ul>
     *
     * Set to {@code false} to suppress the log files in production.
     */
    public static boolean debugPatch = true;

    /** Per-session locks for thread-safe generation */
    private static final ConcurrentHashMap<String, Object> sessionLocks = new ConcurrentHashMap<>();

    /** Track which sessions have generated files (avoids redundant regeneration) */
    private static final ConcurrentHashMap<String, Long> sessionGenerationTimestamps = new ConcurrentHashMap<>();

    /**
     * Per-session KBcache copies.
     * Each session that performs a schema-level tell() gets its own deep copy of the
     * shared KBcache.  This keeps session-specific incremental updates (M3.2) from
     * mutating the shared base cache that all other sessions depend on.
     */
    private static final ConcurrentHashMap<String, KBcache> sessionCaches = new ConcurrentHashMap<>();

    /**
     * Per-session axiom key: axiom name → source Formula.
     *
     * <p>The global {@code SUMOKBtoTPTPKB.axiomKey} is treated as <em>read-only</em>
     * after initial KB generation — it maps base-KB axiom names ({@code kb_SUMO_N})
     * to their source formulas.  Every axiom name created by {@code patchSessionTPTP}
     * (both retranslated base formulas and new {@code tell()} assertions) is recorded
     * here instead, keeping sessions fully isolated.
     *
     * <p>Entries are removed in {@link #cleanupSession}.
     */
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Formula>>
            sessionAxiomKeys = new ConcurrentHashMap<>();

    /** Sessions currently in batch-tell mode (Case B/default TPTP regens suppressed). */
    private static final Set<String> batchModeActive = ConcurrentHashMap.newKeySet();

    /**
     * Per-session lazy flag for askVampireForTQ().
     *   null  → session not in batch context
     *   false → batch active/done; no Case B/default tells hit (Case A patches applied)
     *   true  → batch active/done; at least one Case B/default tell was deferred
     */
    private static final ConcurrentHashMap<String, Boolean> precomputedRegenRequired =
            new ConcurrentHashMap<>();

    /*********************************************************************************
     * Return the session-specific axiom key for {@code sessionId}, creating an empty
     * map on the first call.
     *
     * @param sessionId The HTTP session ID
     * @return The session-specific axiom key (never null)
     */
    public static Map<String, Formula> getOrCreateSessionAxiomKey(String sessionId) {

        return sessionAxiomKeys.computeIfAbsent(sessionId, id -> new ConcurrentHashMap<>());
    }

    /*********************************************************************************
     * Return the session-specific axiom key for {@code sessionId}, or {@code null}
     * if no patches have been applied to this session yet.
     *
     * @param sessionId The HTTP session ID
     * @return The session axiom key, or null
     */
    public static Map<String, Formula> getSessionAxiomKey(String sessionId) {

        return sessionAxiomKeys.get(sessionId);
    }

    /*********************************************************************************
     * Start batch-tell mode for a session.  Suppresses expensive Case B and default
     * full regenerations during the tell loop; the deferred decision is recorded in
     * {@code precomputedRegenRequired} for {@code askVampireForTQ()} to consume.
     *
     * @param sessionId The HTTP session ID
     */
    public static void beginBatchTells(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) return;
        batchModeActive.add(sessionId);
        precomputedRegenRequired.put(sessionId, Boolean.FALSE);  // initialise flag
    }

    /*********************************************************************************
     * End batch-tell mode for a session.  The {@code precomputedRegenRequired} flag
     * is preserved so that the immediately following {@code askVampireForTQ()} call
     * can consume it via {@link #consumeBatchFlag}.
     *
     * @param sessionId The HTTP session ID
     */
    public static void endBatchTells(String sessionId) {
        batchModeActive.remove(sessionId);
        // precomputedRegenRequired preserved for askVampireForTQ()
    }

    /*********************************************************************************
     * @return true if {@code sessionId} is currently in batch-tell mode
     */
    public static boolean isBatchMode(String sessionId) {
        return sessionId != null && batchModeActive.contains(sessionId);
    }

    /*********************************************************************************
     * Record that at least one Case B or default-case tell was deferred during
     * batch mode (so {@code askVampireForTQ()} must do a full regen).
     *
     * @param sessionId The HTTP session ID
     */
    public static void setForceGeneration(String sessionId) {
        if (sessionId != null)
            precomputedRegenRequired.put(sessionId, Boolean.TRUE);
    }

    /*********************************************************************************
     * Read and clear the batch flag (one-shot).
     *
     * @param sessionId The HTTP session ID
     * @return {@code null} if session was not in batch context;
     *         {@code Boolean.TRUE} if a Case B/default tell was deferred;
     *         {@code Boolean.FALSE} if only Case A patches were applied (no full regen needed)
     */
    public static Boolean consumeBatchFlag(String sessionId) {
        return precomputedRegenRequired.remove(sessionId);
    }

    /*********************************************************************************
     * Return the session-specific KBcache for {@code sessionId}, creating a deep
     * copy of {@code kb.kbCache} on the first call.  Subsequent calls return the
     * same (possibly incrementally-updated) copy.
     *
     * <p>The copy constructor used here correctly copies all fields
     * including {@code functions}, {@code predicates}, {@code instRels},
     * {@code instances}, {@code disjoint}, {@code disjointRelations}, and
     * {@code initialized}.
     *
     * @param sessionId The HTTP session ID
     * @param kb        The knowledge base whose shared cache is the copy source
     * @return The session-specific KBcache (never null)
     */
    public static KBcache getOrCreateSessionCache(String sessionId, KB kb) {

        return sessionCaches.computeIfAbsent(sessionId, id -> {
            if (debug) System.out.println("SessionTPTPManager: Creating KBcache copy for session " + id);
            return new KBcache(kb.kbCache, kb);
        });
    }

    /*********************************************************************************
     * Return the session-specific KBcache for {@code sessionId}, or {@code null}
     * if no copy has been created yet.
     *
     * @param sessionId The HTTP session ID
     * @return The session KBcache, or null
     */
    public static KBcache getSessionCache(String sessionId) {

        return sessionCaches.get(sessionId);
    }

    /*********************************************************************************
     * Apply an incremental KBcache update and patch the session TPTP file for
     * one schema-level formula just added via tell().
     *
     * Supported predicates are dispatched to targeted KBcache update methods.
     * Unsupported predicates (partition, exhaustiveDecomposition, etc.) fall
     * back to a full generateSessionTPTP() call.
     *
     * @param kb        the shared KB (formulaMap already updated by merge())
     * @param sessionId the HTTP session
     * @param formula   the just-asserted formula
     * @param lang      TPTP language ("fof"/"tptp" or "tff")
     * @return path to the (updated) session TPTP file, or null if sessionId is empty
     */
    public static Path applyIncrementalUpdate(KB kb, String sessionId, Formula formula, String lang) {

        if (sessionId == null || sessionId.isEmpty() || formula == null)
            return null;

        String pred = formula.car();
        if (pred == null) return null;

        // Normalize translator lang ("fof") to file extension ("tptp") so that all
        // downstream methods (generateSessionTPTP, patchSessionTPTP) use consistent
        // file paths that match the shared base file (SUMO.tptp, not SUMO.fof).
        final String fileLang = "fof".equals(lang) ? "tptp" : lang;

        KBcache sessionCache = getOrCreateSessionCache(sessionId, kb);
        Set<Formula> affected = Collections.emptySet();

        try {
            switch (pred) {
                case "subclass":
                case "immediateSubclass": {
                    String child = formula.getStringArgument(1);
                    String parent = formula.getStringArgument(2);
                    sessionCache.addSubclass(child, parent);
                    affected = SUMOKBtoTPTPKB.findAffectedFormulasForSubclass(
                            kb, sessionCache, child, parent);
                    break;
                }
                case "instance":
                case "immediateInstance": {
                    String inst = formula.getStringArgument(1);
                    String className = formula.getStringArgument(2);
                    sessionCache.addInstance(inst, className);
                    affected = SUMOKBtoTPTPKB.findAffectedFormulasForInstance(
                            kb, sessionCache, inst, className);
                    break;
                }
                case "domain":
                case "domainSubclass":
                    // domain/range DO change argument-type signatures used in type guards,
                    // so existing formulas that use the relation may need retranslation.
                    affected = SUMOKBtoTPTPKB.findAffectedFormulas(kb,
                            sessionCache.addDomain(
                                    formula.getStringArgument(1),
                                    Integer.parseInt(formula.getStringArgument(2).trim()),
                                    formula.getStringArgument(3)));
                    break;
                case "range":
                case "rangeSubclass":
                    affected = SUMOKBtoTPTPKB.findAffectedFormulas(kb,
                            sessionCache.addRange(
                                    formula.getStringArgument(1), formula.getStringArgument(2)));
                    break;
                case "subrelation":
                    // subrelation extends the predicate hierarchy; predicate-variable
                    // formulas that enumerate sub-predicates may need re-expansion.
                    affected = SUMOKBtoTPTPKB.findAffectedFormulas(kb,
                            sessionCache.addSubrelation(
                                    formula.getStringArgument(1), formula.getStringArgument(2)));
                    break;
                case "disjoint":
                    // disjoint updates the disjoint set but does not change type guards
                    // or predicate variable expansion; no formulas need retranslation.
                    sessionCache.addDisjoint(
                            formula.getStringArgument(1), formula.getStringArgument(2));
                    break;
                default:
                    // Complex predicates (partition, exhaustiveDecomposition, etc.)
                    // — fall back to full session regeneration, unless batch mode is active
                    System.out.println("SessionTPTPManager.applyIncrementalUpdate: " +
                                "no incremental handler for '" + pred + "'" +
                                (isBatchMode(sessionId) ? ", deferring regen (batch mode)" : ", falling back to full regen"));
                    if (isBatchMode(sessionId)) {
                        setForceGeneration(sessionId);   // full regen deferred to askVampireForTQ()
                        return null;
                    }
                    return generateSessionTPTP(sessionId, kb, fileLang);
            }
        }
        catch (NumberFormatException e) {
            System.err.println("SessionTPTPManager.applyIncrementalUpdate: bad argNum in " +
                    formula.getFormula() + " — falling back to full regen");
            return generateSessionTPTP(sessionId, kb, fileLang);
        }

        // The new formula was already added to kb.formulaMap by KB.tell() before this
        // method runs, so findAffectedFormulas* may include it in the affected set.
        // Remove it here to avoid a double-append: it belongs only in newFormulas.
        if (!affected.isEmpty() && affected.contains(formula)) {
            affected = new HashSet<>(affected);
            affected.remove(formula);
        }

        return patchSessionTPTP(sessionId, kb, fileLang, affected, Collections.singleton(formula), sessionCache);
    }

    /*********************************************************************************
     * Get the path to a session-specific TPTP file.
     *
     * @param sessionId The HTTP session ID
     * @param kbName The knowledge base name (e.g., "SUMO")
     * @param lang The TPTP language ("tptp" for FOF, "tff" for TFF)
     * @return Path to the session-specific TPTP file
     */
    public static Path getSessionTPTPPath(String sessionId, String kbName, String lang) {

        String kbDir = KBmanager.getMgr().getPref("kbDir");
        return Paths.get(kbDir, "sessions", sessionId, kbName + "." + lang);
    }

    /*********************************************************************************
     * Get the path to a session-specific UserAssertions KIF file.
     *
     * @param sessionId The HTTP session ID
     * @param kbName The knowledge base name (e.g., "SUMO")
     * @return Path to the session-specific UserAssertions.kif file
     */
    public static Path getSessionUAPath(String sessionId, String kbName) {

        return getSessionDir(sessionId).resolve(kbName + KB._userAssertionsString);
    }

    /*********************************************************************************
     * Get the path to a session-specific UserAssertions TPTP file.
     *
     * @param sessionId The HTTP session ID
     * @param kbName The knowledge base name (e.g., "SUMO")
     * @return Path to the session-specific UserAssertions.tptp file
     */
    public static Path getSessionUATPTPPath(String sessionId, String kbName) {

        return getSessionDir(sessionId).resolve(kbName + KB._userAssertionsTPTP);
    }

    /*********************************************************************************
     * Get the path to a session-specific UserAssertions TFF file.
     *
     * @param sessionId The HTTP session ID
     * @param kbName The knowledge base name (e.g., "SUMO")
     * @return Path to the session-specific UserAssertions.tff file
     */
    public static Path getSessionUATFFPath(String sessionId, String kbName) {

        return getSessionDir(sessionId).resolve(kbName + KB._userAssertionsTFF);
    }

    /*********************************************************************************
     * Get the path to a session-specific UserAssertions THF file.
     *
     * @param sessionId The HTTP session ID
     * @param kbName The knowledge base name (e.g., "SUMO")
     * @return Path to the session-specific UserAssertions.thf file
     */
    public static Path getSessionUATHFPath(String sessionId, String kbName) {

        return getSessionDir(sessionId).resolve(kbName + KB._userAssertionsTHF);
    }

    /*********************************************************************************
     * Get the session directory path.
     *
     * @param sessionId The HTTP session ID
     * @return Path to the session directory
     */
    public static Path getSessionDir(String sessionId) {

        String kbDir = KBmanager.getMgr().getPref("kbDir");
        return Paths.get(kbDir, "sessions", sessionId);
    }

    /*********************************************************************************
     * Check if a session-specific TPTP file exists and is current.
     *
     * @param sessionId The HTTP session ID
     * @param kb The knowledge base
     * @param lang The TPTP language ("tptp" or "tff")
     * @return true if the session file exists and is newer than user assertions
     */
    public static boolean isSessionFileValid(String sessionId, KB kb, String lang) {

        Path sessionFile = getSessionTPTPPath(sessionId, kb.name, lang);
        if (!Files.exists(sessionFile)) {
            return false;
        }

        // Check if session file is newer than SESSION-SPECIFIC user assertions file
        Path sessionUAKif = getSessionUAPath(sessionId, kb.name);

        if (!Files.exists(sessionUAKif)) {
            // No session-specific user assertions, session file is valid
            return true;
        }

        try {
            long sessionFileTime = Files.getLastModifiedTime(sessionFile).toMillis();
            long uaFileTime = Files.getLastModifiedTime(sessionUAKif).toMillis();
            return sessionFileTime >= uaFileTime;
        }
        catch (IOException e) {
            return false;
        }
    }

    /***********************************************************************************
     * Generate a session-specific TPTP file with user assertions included.
     * This performs a full regeneration to ensure schema-changing predicates
     * (subclass, domain, etc.) are fully integrated.
     *
     * @param sessionId The HTTP session ID
     * @param kb The knowledge base
     * @param lang The TPTP language ("tptp" for FOF, "tff" for TFF)
     * @return The path to the generated session-specific TPTP file
     */
    public static Path generateSessionTPTP(String sessionId, KB kb, String lang) {

        // Get or create session-specific lock
        Object lock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());

        synchronized (lock) {
            // Double-check if file is already valid (race condition protection)
            if (isSessionFileValid(sessionId, kb, lang)) {
                if (debug) System.out.println("SessionTPTPManager: Session file already valid for " + sessionId);
                return getSessionTPTPPath(sessionId, kb.name, lang);
            }

            Path sessionDir = getSessionDir(sessionId);
            Path sessionFile = getSessionTPTPPath(sessionId, kb.name, lang);
            Path tmpFile = sessionFile.resolveSibling(sessionFile.getFileName() + ".tmp");

            try {
                // Create session directory if needed
                Files.createDirectories(sessionDir);

                // Clean up any stale tmp file
                Files.deleteIfExists(tmpFile);

                System.out.println("SessionTPTPManager: Generating session-specific " + lang +
                                   " for session " + sessionId);
                long startTime = System.currentTimeMillis();

                // Use session-specific KBcache so schema changes from this session's tell()
                // calls are reflected in the generated file, without affecting other sessions.
                // We swap kb.kbCache temporarily.  The per-session lock above ensures no
                // concurrent same-session swaps; the restoration in the finally block ensures
                // the shared cache is always put back.
                KBcache sessionCache = getOrCreateSessionCache(sessionId, kb);
                KBcache sharedCache = kb.kbCache;
                kb.kbCache = sessionCache;
                try {
                    if ("tptp".equals(lang) || "fof".equals(lang)) {
                        TPTPGenerationManager.generateFOFToPath(kb, tmpFile);
                    } else if ("tff".equals(lang)) {
                        TPTPGenerationManager.generateTFFToPath(kb, tmpFile);
                    } else {
                        throw new IllegalArgumentException("Unsupported TPTP language: " + lang);
                    }
                } finally {
                    kb.kbCache = sharedCache;
                }

                // Atomic move
                try {
                    Files.move(tmpFile, sessionFile,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                }
                catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmpFile, sessionFile, StandardCopyOption.REPLACE_EXISTING);
                }

                long elapsed = System.currentTimeMillis() - startTime;
                System.out.println("SessionTPTPManager: Session " + lang + " generation complete in " +
                                   (elapsed / 1000.0) + "s for " + sessionId);

                // Track generation timestamp
                sessionGenerationTimestamps.put(sessionId, System.currentTimeMillis());

                return sessionFile;

            }
            catch (IOException e) {
                System.err.println("SessionTPTPManager: Error generating session TPTP: " + e.getMessage());
                e.printStackTrace();
                // Cleanup on failure
                try { Files.deleteIfExists(tmpFile); } catch (IOException ignore) {}
                throw new RuntimeException("Failed to generate session TPTP file", e);
            }
        }
    }


    /*********************************************************************************
     * Incrementally patch the session-specific TPTP file.
     *
     * <p>Handles two categories of output in a single atomic write:
     * <ol>
     *   <li><b>Retranslated base axioms</b> ({@code affected}) — existing KB formulas
     *       whose TPTP translation changes because the session KBcache was updated
     *       (e.g., new type guards after {@code addSubclass}).  Their old axiom lines
     *       are commented out and the new translations are appended.</li>
     *   <li><b>New tell() assertion axioms</b> ({@code newFormulas}) — formulas that
     *       were just added to the KB via {@code tell()} and have never been translated.
     *       They are translated with the session KBcache and appended at the end.</li>
     * </ol>
     *
     * <p>All new axiom names are recorded in the <em>session-specific</em> axiom key
     * ({@link #getOrCreateSessionAxiomKey}) rather than in the global
     * {@code SUMOKBtoTPTPKB.axiomKey}, keeping sessions fully isolated.
     *
     * <p>If the session already has a patched file it is used as the base (Option B),
     * so previous patches are preserved across multiple {@code tell()} calls.
     *
     * <p>Falls back to {@link #generateSessionTPTP} when the shared base file is
     * missing or the global {@code axiomKey} has not been populated yet.
     *
     * @param sessionId    the HTTP session ID
     * @param kb           the knowledge base
     * @param lang         TPTP language: "fof" / "tptp" for FOF, "tff" for TFF
     * @param affected     existing KB formulas that need retranslation (may be empty)
     * @param newFormulas  formulas just added via {@code tell()} (may be empty)
     * @param sessionCache the session-specific KBcache (already updated by M3.2)
     * @return the path to the session-specific TPTP file
     */
    public static Path patchSessionTPTP(
            String sessionId, KB kb, String lang,
            Set<Formula> affected, Set<Formula> newFormulas, KBcache sessionCache) {

        // Normalize lang: "fof" and "tptp" both mean FOF; file extension is always "tptp".
        final String normalizedLang = "tptp".equals(lang) ? "fof" : lang;
        final String ext            = "fof".equals(normalizedLang) ? "tptp" : normalizedLang;

        // If axiomKey is not yet populated (warm start rebuild still in progress),
        // proceed anyway: toSkip will be empty so stale axioms won't be commented out,
        // but new tell() formulas are still appended correctly.  This is always correct
        // for brand-new terms (affected set is empty) and produces harmless redundancy
        // for existing terms (old + new translations both present until next regen).
        if (SUMOKBtoTPTPKB.axiomKey.isEmpty()) {
            System.out.println("SessionTPTPManager.patchSessionTPTP: axiomKey not yet populated " +
                    "(warm-start rebuild in progress) for session " + sessionId +
                    ". Stale axioms will not be commented out; new formulas will be appended.");
        }

        // Trivial case: nothing at all to write → ensure session file exists
        boolean hasAffected  = affected   != null && !affected.isEmpty();
        boolean hasNewForms  = newFormulas != null && !newFormulas.isEmpty();
        if (!hasAffected && !hasNewForms) {
            if (debug)
                System.out.println("SessionTPTPManager.patchSessionTPTP: nothing to patch for " + sessionId);
            return mergeBaseWithSessionUA(sessionId, kb, ext);
        }

        Object lock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());

        synchronized (lock) {
            String kbDir = KBmanager.getMgr().getPref("kbDir");
            Path sharedBase  = Paths.get(kbDir, kb.name + "." + ext);
            Path sessionDir  = getSessionDir(sessionId);
            Path sessionFile = getSessionTPTPPath(sessionId, kb.name, ext);
            Path tmpFile     = sessionFile.resolveSibling(sessionFile.getFileName() + ".tmp");

            // Shared base not yet available (background generation still in progress).
            // The tell() assertion is already persisted in kb.formulaMap and the session
            // UA .kif file; the session TPTP file will be created on the next tell() or ask().
            if (!Files.exists(sharedBase)) {
                System.out.println("SessionTPTPManager.patchSessionTPTP: shared base not yet " +
                        "available (" + sharedBase + "); deferring TPTP patch for session " +
                        sessionId + " until generation completes.");
                return null;
            }

            // Patch from the existing session file when present, so previous
            // patches survive successive tell() calls within the same session.
            Path baseFile = Files.exists(sessionFile) ? sessionFile : sharedBase;

            try {
                Files.createDirectories(sessionDir);
                Files.deleteIfExists(tmpFile);

                int affectedCount = hasAffected  ? affected.size()   : 0;
                int newCount      = hasNewForms  ? newFormulas.size() : 0;
                System.out.println("SessionTPTPManager: Patching session " + normalizedLang +
                        " for session " + sessionId +
                        " (" + affectedCount + " retranslated, " + newCount + " new)");
                long startTime = System.currentTimeMillis();

                // 1. Build reverse index merging global axiomKey + this session's patch key
                Map<Formula, Set<String>> reverseIndex = buildReverseIndex(sessionId);

                // 2. Collect axiom names that must be commented out (retranslated formulas only)
                Set<String> toSkip = new HashSet<>();
                if (hasAffected) {
                    for (Formula f : affected) {
                        Set<String> names = reverseIndex.get(f);
                        if (names != null)
                            toSkip.addAll(names);
                    }
                }
                if (debug)
                    System.out.println("SessionTPTPManager.patchSessionTPTP: " +
                            toSkip.size() + " axiom names to comment out");

                // 3. Retranslate affected + new formulas against the session cache,
                //    in a single kb.kbCache swap to minimise the swap window.
                KBcache sharedCache = kb.kbCache;
                kb.kbCache = sessionCache;
                Map<Formula, List<String>> retranslated;
                Map<Formula, List<String>> newTranslations;
                try {
                    retranslated    = hasAffected ? SUMOKBtoTPTPKB.retranslateFormulas(
                            kb, affected,   normalizedLang) : Collections.emptyMap();
                    newTranslations = hasNewForms ? SUMOKBtoTPTPKB.retranslateFormulas(
                            kb, newFormulas, normalizedLang) : Collections.emptyMap();
                } finally {
                    kb.kbCache = sharedCache;
                }

                // 3b. Optionally write a human-readable debug log to the session dir
                if (debugPatch) {
                    writePatchDebugLog(sessionDir, sessionId, normalizedLang,
                            affected, toSkip, retranslated,
                            newFormulas, newTranslations);
                }

                // 4. Write patched file
                String sanitizedKBName = kb.name.replaceAll("\\W", "_");
                // Session axiom key — isolated from the global axiomKey
                Map<String, Formula> sessionAxiomKey = getOrCreateSessionAxiomKey(sessionId);
                // Build a session-specific prefix so axiom names are globally unique
                // even when two sessions patch concurrently.
                // Use Math.abs(hashCode) of sessionId for a compact numeric discriminator;
                // prefix with "s" so the name starts with a letter (TPTP requirement).
                String sessionTag = "s" + Math.abs(sessionId.hashCode());
                // Sequential index within this patch call (restarts from 1 each patch).
                AtomicInteger patchIdx = new AtomicInteger(1);

                try (BufferedReader reader = Files.newBufferedReader(baseFile, StandardCharsets.UTF_8);
                     BufferedWriter writer = Files.newBufferedWriter(tmpFile,  StandardCharsets.UTF_8)) {

                    // --- Copy base, commenting out stale axioms ---
                    // Also build alreadyInBase to avoid re-appending bodies already present.
                    Set<String> alreadyInBase = new HashSet<>();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String axiomName = extractAxiomName(line);
                        if (axiomName != null && toSkip.contains(axiomName)) {
                            writer.write("% [patched out] " + line);
                        } else {
                            writer.write(line);
                            String existingBody = extractAxiomBody(line);
                            if (existingBody != null) alreadyInBase.add(existingBody);
                        }
                        writer.newLine();
                    }

                    // --- Append section header ---
                    writer.newLine();
                    writer.write("% --- Incremental patch: session " + sessionId + " ---");
                    writer.newLine();

                    // Helper: write one body string as a named axiom, recording in session key
                    Set<String> patchWritten = new HashSet<>(); // within-patch dedup

                    // Retranslated base formulas
                    for (Map.Entry<Formula, List<String>> entry : retranslated.entrySet()) {
                        for (String body : entry.getValue()) {
                            if (alreadyInBase.contains(body)) continue; // already in base file
                            if (!patchWritten.add(body)) continue;      // already in this patch
                            String name = "kb_" + sanitizedKBName + "_" + sessionTag
                                    + "_" + patchIdx.getAndIncrement();
                            writer.write(normalizedLang + "(" + name + ",axiom,(" + body + ")).");
                            writer.newLine();
                            sessionAxiomKey.put(name, entry.getKey()); // session key only
                        }
                    }

                    // New tell() assertion formulas
                    for (Map.Entry<Formula, List<String>> entry : newTranslations.entrySet()) {
                        for (String body : entry.getValue()) {
                            if (alreadyInBase.contains(body)) continue; // already in base file
                            if (!patchWritten.add(body)) continue;      // already in this patch
                            String name = "kb_" + sanitizedKBName + "_" + sessionTag
                                    + "_" + patchIdx.getAndIncrement();
                            writer.write(normalizedLang + "(" + name + ",axiom,(" + body + ")).");
                            writer.newLine();
                            sessionAxiomKey.put(name, entry.getKey()); // session key only
                        }
                    }
                }

                // 5. Atomic move to session file path
                try {
                    Files.move(tmpFile, sessionFile,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmpFile, sessionFile, StandardCopyOption.REPLACE_EXISTING);
                }

                long elapsed = System.currentTimeMillis() - startTime;
                System.out.println("SessionTPTPManager: Patch complete in " + elapsed +
                        "ms for session " + sessionId);

                sessionGenerationTimestamps.put(sessionId, System.currentTimeMillis());
                return sessionFile;

            } catch (IOException e) {
                System.err.println("SessionTPTPManager.patchSessionTPTP: error: " + e.getMessage());
                e.printStackTrace();
                try { Files.deleteIfExists(tmpFile); } catch (IOException ignore) {}
                throw new RuntimeException("Failed to patch session TPTP file", e);
            }
        }
    }

    /*********************************************************************************
     * Write a human-readable debug log for one {@link #patchSessionTPTP} call.
     *
     * <p>The file is named {@code patch-debug-<timestamp>.log} and placed in the
     * session directory.  It records:
     * <ul>
     *   <li>Every <em>affected</em> formula: the KIF text, axiom name(s) commented out,
     *       retranslated TPTP bodies, and a {@code STATUS} warning when no body was
     *       produced (formula was effectively removed).</li>
     *   <li>Every new tell() formula and its TPTP bodies.</li>
     * </ul>
     *
     * Errors writing the log are printed to stderr but do NOT interrupt patching.
     */
    private static void writePatchDebugLog(
            Path sessionDir,
            String sessionId,
            String lang,
            Set<Formula> affected,
            Set<String> toSkip,
            Map<Formula, List<String>> retranslated,
            Set<Formula> newFormulas,
            Map<Formula, List<String>> newTranslations) {

        Path logFile = sessionDir.resolve("patch-debug-" + System.currentTimeMillis() + ".log");
        try (BufferedWriter w = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8)) {

            w.write("=== Patch Debug Log ===");        w.newLine();
            w.write("Session   : " + sessionId);      w.newLine();
            w.write("Language  : " + lang);            w.newLine();
            w.write("Timestamp : " + new java.util.Date()); w.newLine();
            w.newLine();

            // --- Affected (retranslated) formulas ---
            int affectedCount = affected != null ? affected.size() : 0;
            w.write("--- AFFECTED FORMULAS (" + affectedCount + " formulas, " +
                    toSkip.size() + " axiom names commented out) ---");
            w.newLine();

            if (affected != null && !affected.isEmpty()) {
                int idx = 1;
                for (Formula f : affected) {
                    w.newLine();
                    w.write("[" + idx++ + "] KIF: " + f.getFormula().replace('\n', ' '));
                    w.newLine();
                    w.write("    Source : " + f.sourceFile + " line " + f.startLine);
                    w.newLine();

                    // Find axiom names via the global axiomKey reverse-lookup
                    Set<String> names = new java.util.HashSet<>();
                    for (Map.Entry<String, Formula> e : SUMOKBtoTPTPKB.axiomKey.entrySet()) {
                        if (e.getValue().equals(f)) names.add(e.getKey());
                    }
                    // Also check session axiom keys
                    Map<String, Formula> sessKey = sessionAxiomKeys.get(sessionId);
                    if (sessKey != null) {
                        for (Map.Entry<String, Formula> e : sessKey.entrySet()) {
                            if (e.getValue().equals(f)) names.add(e.getKey());
                        }
                    }
                    if (names.isEmpty()) {
                        w.write("    Axioms : (none found in axiomKey)");
                    } else {
                        w.write("    Axioms : " + names);
                    }
                    w.newLine();

                    // Retranslated bodies
                    List<String> bodies = retranslated.get(f);
                    if (bodies == null || bodies.isEmpty()) {
                        w.write("    STATUS : *** WARNING — no retranslated bodies produced;" +
                                " formula is effectively removed from this session ***");
                    } else {
                        w.write("    STATUS : OK — " + bodies.size() + " body(ies) regenerated");
                        w.newLine();
                        for (int b = 0; b < bodies.size(); b++) {
                            w.write("    Body[" + b + "]: " + bodies.get(b));
                        }
                    }
                    w.newLine();
                }
            } else {
                w.write("  (none)");
                w.newLine();
            }

            // --- New tell() formulas ---
            w.newLine();
            int newCount = newFormulas != null ? newFormulas.size() : 0;
            w.write("--- NEW TELL() FORMULAS (" + newCount + ") ---");
            w.newLine();

            if (newFormulas != null && !newFormulas.isEmpty()) {
                int idx = 1;
                for (Formula f : newFormulas) {
                    w.newLine();
                    w.write("[" + idx++ + "] KIF: " + f.getFormula().replace('\n', ' '));
                    w.newLine();
                    List<String> bodies = newTranslations.get(f);
                    if (bodies == null || bodies.isEmpty()) {
                        w.write("    STATUS : *** WARNING — no TPTP bodies produced for new formula ***");
                    } else {
                        w.write("    STATUS : OK — " + bodies.size() + " body(ies)");
                        w.newLine();
                        for (int b = 0; b < bodies.size(); b++) {
                            w.write("    Body[" + b + "]: " + bodies.get(b));
                        }
                    }
                    w.newLine();
                }
            } else {
                w.write("  (none)");
                w.newLine();
            }

            // --- Axiom names that were commented out ---
            w.newLine();
            w.write("--- AXIOM NAMES COMMENTED OUT (" + toSkip.size() + ") ---");
            w.newLine();
            for (String name : new java.util.TreeSet<>(toSkip)) {
                w.write("  " + name);
                w.newLine();
            }

            w.newLine();
            w.write("=== End of Patch Debug Log ===");
            w.newLine();

        } catch (IOException e) {
            System.err.println("SessionTPTPManager.writePatchDebugLog: failed to write log to "
                    + logFile + ": " + e.getMessage());
        }
    }

    /*********************************************************************************
     * Build a reverse index: Formula → Set of axiom names.
     *
     * <p>Merges two sources:
     * <ol>
     *   <li>The global {@code SUMOKBtoTPTPKB.axiomKey} — base KB axioms, read-only.</li>
     *   <li>The session-specific axiom key for {@code sessionId} — patch axioms written
     *       by previous {@code patchSessionTPTP} calls in this session.</li>
     * </ol>
     *
     * @param sessionId the HTTP session ID
     */
    private static Map<Formula, Set<String>> buildReverseIndex(String sessionId) {

        Map<Formula, Set<String>> rev = new HashMap<>();
        // Global base axioms (read-only after initial generation)
        for (Map.Entry<String, Formula> e : SUMOKBtoTPTPKB.axiomKey.entrySet()) {
            rev.computeIfAbsent(e.getValue(), k -> new HashSet<>()).add(e.getKey());
        }
        // Session-specific patch axioms from previous patchSessionTPTP() calls
        Map<String, Formula> sessionKey = sessionAxiomKeys.get(sessionId);
        if (sessionKey != null) {
            for (Map.Entry<String, Formula> e : sessionKey.entrySet()) {
                rev.computeIfAbsent(e.getValue(), k -> new HashSet<>()).add(e.getKey());
            }
        }
        return rev;
    }

    /*********************************************************************************
     * Extract the axiom name from a TPTP formula line, or {@code null} if the
     * line is not a {@code kb_*} axiom.
     *
     * <p>Handles lines of the form:
     * <pre>fof(kb_SUMO_1234,axiom,(body)).</pre>
     * <pre>tff(kb_SUMO_1234,axiom,(body)).</pre>
     *
     * @param line a raw line from a TPTP file
     * @return the axiom name (e.g., {@code "kb_SUMO_1234"}), or {@code null}
     */
    private static String extractAxiomName(String line) {

        String trimmed = line.trim();
        // Must start with a known TPTP language keyword followed by '('
        int parenIdx = trimmed.indexOf('(');
        if (parenIdx < 0) return null;
        String prefix = trimmed.substring(0, parenIdx);
        if (!prefix.equals("fof") && !prefix.equals("tff") && !prefix.equals("thf"))
            return null;
        // Extract name: between '(' and the first ','
        int commaIdx = trimmed.indexOf(',', parenIdx);
        if (commaIdx < 0) return null;
        String name = trimmed.substring(parenIdx + 1, commaIdx);
        // Only match KB axiom names (kb_*), not conjecture/question lines
        if (!name.startsWith("kb_")) return null;
        return name;
    }

    /*********************************************************************************
     * Extract the formula body from a single-line TPTP axiom, or {@code null} if the
     * line is not a single-line axiom or is a comment.
     *
     * <p>Handles the format written by {@code _tWriteFile} and {@code patchSessionTPTP}:
     * <pre>fof(name,axiom,(body)).</pre>
     * where {@code body} may itself contain parentheses.
     *
     * <p>Multi-line axioms (where the body contains newlines) are not recognised and
     * return {@code null}; deduplication for those is silently skipped.
     *
     * @param line a raw line from a TPTP file
     * @return the body string (e.g. {@code "![V1]: (subclass(V1,Entity))"}), or {@code null}
     */
    private static String extractAxiomBody(String line) {

        String trimmed = line.trim();
        if (trimmed.startsWith("%")) return null;
        // Single-line axioms end with ")).": closing the (body) wrapper and the fof() call.
        if (!trimmed.endsWith(")).")) return null;
        int axiomIdx = trimmed.indexOf(",axiom,(");
        if (axiomIdx < 0) return null;
        int bodyStart = axiomIdx + ",axiom,(".length();
        // Remove the trailing ")). " → last 3 chars
        return trimmed.substring(bodyStart, trimmed.length() - 3);
    }

    /*********************************************************************************
     * Clean up all session-specific files for a given session.
     * Called when a session is destroyed.
     *
     * @param sessionId The HTTP session ID to clean up
     */
    public static void cleanupSession(String sessionId) {

        if (StringUtil.emptyString(sessionId)) {
            return;
        }

        // Remove locks, timestamps, session KBcache, session axiom key, and batch state
        sessionLocks.remove(sessionId);
        sessionGenerationTimestamps.remove(sessionId);
        sessionCaches.remove(sessionId);
        sessionAxiomKeys.remove(sessionId);
        batchModeActive.remove(sessionId);
        precomputedRegenRequired.remove(sessionId);

        Path sessionDir = getSessionDir(sessionId);

        if (!Files.exists(sessionDir)) {
            return;
        }

        System.out.println("SessionTPTPManager: Cleaning up session directory: " + sessionDir);

        try {
            // Delete all files in the session directory
            Files.walk(sessionDir)
                    .sorted((a, b) -> b.compareTo(a)) // Reverse order for depth-first deletion
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("SessionTPTPManager: Failed to delete " + path + ": " + e.getMessage());
                        }
                    });

            System.out.println("SessionTPTPManager: Session cleanup complete for " + sessionId);

        }
        catch (IOException e) {
            System.err.println("SessionTPTPManager: Error during session cleanup: " + e.getMessage());
        }
    }

    /*********************************************************************************
     * Check if a session has generated TPTP files.
     *
     * @param sessionId The HTTP session ID
     * @return true if this session has generated files
     */
    public static boolean hasSessionFiles(String sessionId) {

        return sessionGenerationTimestamps.containsKey(sessionId);
    }

    /*********************************************************************************
     * Get the timestamp of the last generation for a session.
     *
     * @param sessionId The HTTP session ID
     * @return The timestamp in milliseconds, or -1 if no generation has occurred
     */
    public static long getSessionGenerationTimestamp(String sessionId) {

        Long timestamp = sessionGenerationTimestamps.get(sessionId);
        return timestamp != null ? timestamp : -1L;
    }

    /*********************************************************************************
     * Merge shared base TPTP file with session-specific UserAssertions.
     * This is faster than full regeneration when only UA files have changed.
     *
     * @param sessionId The HTTP session ID
     * @param kb The knowledge base
     * @param lang The TPTP language ("tptp" for FOF, "tff" for TFF)
     * @return The path to the merged session-specific TPTP file
     */
    public static Path mergeBaseWithSessionUA(String sessionId, KB kb, String lang) {

        Object lock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());

        synchronized (lock) {
            Path sessionDir = getSessionDir(sessionId);
            Path sessionFile = getSessionTPTPPath(sessionId, kb.name, lang);
            Path tmpFile = sessionFile.resolveSibling(sessionFile.getFileName() + ".tmp");

            try {
                // Create session directory if needed
                Files.createDirectories(sessionDir);

                // Clean up any stale tmp file
                Files.deleteIfExists(tmpFile);

                System.out.println("SessionTPTPManager: Merging shared base with session UA for session " + sessionId);
                long startTime = System.currentTimeMillis();

                // Get paths
                String kbDir = KBmanager.getMgr().getPref("kbDir");
                Path sharedBase = Paths.get(kbDir, kb.name + "." + lang);
                Path sessionUA = getSessionUATPTPPath(sessionId, kb.name);
                if ("tff".equals(lang)) {
                    sessionUA = getSessionUATFFPath(sessionId, kb.name);
                }

                // Check if shared base exists
                if (!Files.exists(sharedBase)) {
                    System.err.println("SessionTPTPManager: Shared base file not found: " + sharedBase);
                    System.err.println("SessionTPTPManager: Falling back to full regeneration");
                    return generateSessionTPTP(sessionId, kb, lang);
                }

                // Copy shared base to tmp file
                Files.copy(sharedBase, tmpFile, StandardCopyOption.REPLACE_EXISTING);

                // Append session UA if it exists
                if (Files.exists(sessionUA)) {
                    // Append UA content to the tmp file
                    try (java.io.BufferedWriter writer = Files.newBufferedWriter(tmpFile,
                            StandardCharsets.UTF_8,
                            java.nio.file.StandardOpenOption.APPEND)) {
                        writer.newLine();
                        writer.write("% Session-specific User Assertions");
                        writer.newLine();
                        Files.lines(sessionUA, StandardCharsets.UTF_8).forEach(line -> {
                            try {
                                writer.write(line);
                                writer.newLine();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }

                // Atomic move
                try {
                    Files.move(tmpFile, sessionFile,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                }
                catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmpFile, sessionFile, StandardCopyOption.REPLACE_EXISTING);
                }

                long elapsed = System.currentTimeMillis() - startTime;
                System.out.println("SessionTPTPManager: Merge complete in " + elapsed + "ms for " + sessionId);

                // Track generation timestamp
                sessionGenerationTimestamps.put(sessionId, System.currentTimeMillis());

                return sessionFile;

            }
            catch (IOException e) {
                System.err.println("SessionTPTPManager: Error merging base with UA: " + e.getMessage());
                e.printStackTrace();
                // Cleanup on failure
                try { Files.deleteIfExists(tmpFile); } catch (IOException ignore) {}
                throw new RuntimeException("Failed to merge base with session UA", e);
            }
        }
    }

    /*********************************************************************************
     * Runs {@code op} with {@code kb.kbCache} temporarily swapped to the session-specific
     * KBcache for {@code sessionId}, if one exists.  If no session cache exists (no schema-
     * level tell() has been made this session), {@code op} runs against the shared cache
     * unchanged.
     *
     * <p>Acquires the per-session lock for the duration of the swap so that concurrent
     * operations within the same session see a consistent kbCache state.
     *
     * @param <T>       the return type of the operation
     * @param sessionId the HTTP session ID (null or empty ⟹ no swap)
     * @param kb        the shared KB whose kbCache field is temporarily replaced
     * @param op        the operation to run with the session cache active
     * @return the value returned by {@code op}
     */
    public static <T> T withSessionCache(String sessionId, KB kb, Supplier<T> op) {

        if (sessionId == null || sessionId.isEmpty()) return op.get();

        KBcache sessionCache = getSessionCache(sessionId);  // null = no schema tells yet
        if (sessionCache == null) return op.get();          // nothing to swap

        Object lock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());
        synchronized (lock) {
            KBcache shared = kb.kbCache;
            kb.kbCache = sessionCache;
            try {
                return op.get();
            } finally {
                kb.kbCache = shared;
            }
        }
    }
}
