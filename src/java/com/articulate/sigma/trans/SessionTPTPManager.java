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
import com.articulate.sigma.Formula;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.utils.LoggingUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Manages session-specific TPTP file lifecycle for isolated TQ test handling.
 * Each session that requires TPTP regeneration (due to schema-changing assertions)
 * gets its own subdirectory under $SIGMA_HOME/KBs/sessions/{sessionId}/.
 */
public class SessionTPTPManager {

    private static final boolean debug = false;

    /** When true, each {@link #patchSessionTPTP} call writes a human-readable log file to the session directory. */
    public static boolean debugPatch = true;

    /** Per-session locks for thread-safe generation */
    private static final ConcurrentHashMap<String, Object> sessionLocks = new ConcurrentHashMap<>();

    /** Track which sessions have generated files (avoids redundant regeneration) */
    private static final ConcurrentHashMap<String, Long> sessionGenerationTimestamps = new ConcurrentHashMap<>();

    /** Each session that performs a schema-level tell() gets its own deep copy of the shared KBcache. */
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

    /** Per-session lazy flag for askVampireForTQ(). */
    private static final ConcurrentHashMap<String, Boolean> precomputedRegenRequired = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Formula>> sessionDerivedTypeFacts = new ConcurrentHashMap<>();

    /*********************************************************************************
     * Return the session-specific axiom key for {@code sessionId}, creating an empty map on the first call.
     * @param sessionId The HTTP session ID
     * @return The session-specific axiom key
     */
    public static Map<String, Formula> getOrCreateSessionAxiomKey(String sessionId) {

        return sessionAxiomKeys.computeIfAbsent(sessionId, id -> new ConcurrentHashMap<>());
    }

    /*********************************************************************************
     * Return the session-specific axiom key for sessionId, or null if no patches have been applied to this session yet.
     * @param sessionId The HTTP session ID
     * @return The session axiom key, or null
     */
    public static Map<String, Formula> getSessionAxiomKey(String sessionId) {

        return sessionAxiomKeys.get(sessionId);
    }

    /*********************************************************************************
     * Start batch-tell mode for a session.
     * @param sessionId The HTTP session ID
     */
    public static void beginBatchTells(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) return;
        batchModeActive.add(sessionId);
        precomputedRegenRequired.put(sessionId, Boolean.FALSE);  // initialise flag
    }

    /*********************************************************************************
     * End batch-tell mode for a session.
     * @param sessionId The HTTP session ID
     */
    public static void endBatchTells(String sessionId) {
        batchModeActive.remove(sessionId);
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
     * @param sessionId The HTTP session ID
     */
    public static void setForceGeneration(String sessionId) {
        if (sessionId != null)
            precomputedRegenRequired.put(sessionId, Boolean.TRUE);
    }

    /*********************************************************************************
     * Read and clear the batch flag (one-shot).
     * @param sessionId The HTTP session ID
     * @return null if session was not in batch context;
     *         Boolean.TRUE if a Case B/default tell was deferred;
     *         Boolean.FALSE if only Case A patches were applied (no full regen needed)
     */
    public static Boolean consumeBatchFlag(String sessionId) {
        return precomputedRegenRequired.remove(sessionId);
    }

    /*********************************************************************************
     * Return the session-specific KBcache for {@code sessionId}, creating a deep copy of {@code kb.kbCache} on the first call.
     * @param sessionId The HTTP session ID
     * @param kb The knowledge base whose shared cache is the copy source
     * @return The session-specific KBcache (never null)
     */
    public static KBcache getOrCreateSessionCache(String sessionId, KB kb) {

        return sessionCaches.computeIfAbsent(sessionId, id -> {return new KBcache(kb.kbCache, kb);});
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
     * Apply an incremental KBcache update and patch the session TPTP file for one schema-level formula just added via tell().
     * @param kb        the shared KB (formulaMap already updated by merge())
     * @param sessionId the HTTP session
     * @param formula   the just-asserted formula
     * @param lang      TPTP language ("fof"/"tptp" or "tff")
     * @return path to the (updated) session TPTP file, or null if sessionId is empty
     */
    public static Path applyIncrementalUpdate(KB kb, String sessionId, Formula formula, String lang) {

        if (sessionId == null || sessionId.isEmpty() || formula == null) return null;
        String pred = formula.car();
        if (pred == null) return null;
        final String fileLang = "fof".equals(lang) ? "tptp" : lang;
        KBcache sessionCache = getOrCreateSessionCache(sessionId, kb);
        Set<Formula> affected = Collections.emptySet();
        Set<Formula> newFormulas = new LinkedHashSet<>();
        newFormulas.add(formula);
        try {
            switch (pred) {
                case "subclass":
                case "immediateSubclass": {
                    String child = formula.getStringArgument(1);
                    String parent = formula.getStringArgument(2);
                    sessionCache.addSubclass(child, parent);
                    affected = SUMOKBtoTPTPKB.findAffectedFormulasForSubclass(kb, sessionCache, child, parent);
                    break;
                }
                case "instance":
                case "immediateInstance": {
                    String inst = formula.getStringArgument(1);
                    String className = formula.getStringArgument(2);
                    sessionCache.addInstance(inst, className);
                    affected = SUMOKBtoTPTPKB.findAffectedFormulasForInstance(kb, sessionCache, inst, className);
                    newFormulas.addAll(materializeInheritedInstanceFacts(sessionId, kb, inst, className, sessionCache));
                    break;
                }
                case "domain":
                case "domainSubclass":
                    affected = SUMOKBtoTPTPKB.findAffectedFormulas(kb, sessionCache.addDomain(formula.getStringArgument(1), Integer.parseInt(formula.getStringArgument(2).trim()), formula.getStringArgument(3)));
                    break;
                case "range":
                case "rangeSubclass":
                    affected = SUMOKBtoTPTPKB.findAffectedFormulas(kb, sessionCache.addRange(formula.getStringArgument(1), formula.getStringArgument(2)));
                    break;
                case "subrelation":
                    affected = SUMOKBtoTPTPKB.findAffectedFormulas(kb, sessionCache.addSubrelation(formula.getStringArgument(1), formula.getStringArgument(2)));
                    break;
                case "disjoint":
                    sessionCache.addDisjoint(formula.getStringArgument(1), formula.getStringArgument(2));
                    break;
                default:
                    if (isBatchMode(sessionId)) {
                        setForceGeneration(sessionId);
                        return null;
                    }
                    return generateSessionTPTP(sessionId, kb, fileLang);
            }
        }
        catch (NumberFormatException e) {
            LoggingUtils.log("ERROR", "bad argNum in " + formula.getFormula() + " — falling back to full regen");
            return generateSessionTPTP(sessionId, kb, fileLang);
        }
        if (!affected.isEmpty() && affected.contains(formula)) {
            affected = new HashSet<>(affected);
            affected.remove(formula);
        }
        return patchSessionTPTP(sessionId, kb, fileLang, affected, newFormulas, sessionCache);
    }

    /*********************************************************************************
     * Get the path to a session-specific TPTP file.
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
     * @param sessionId The HTTP session ID
     * @param kbName The knowledge base name (e.g., "SUMO")
     * @return Path to the session-specific UserAssertions.kif file
     */
    public static Path getSessionUAPath(String sessionId, String kbName) {

        return getSessionDir(sessionId).resolve(kbName + KB._userAssertionsString);
    }

    /*********************************************************************************
     * Get the path to a session-specific UserAssertions TPTP file.
     * @param sessionId The HTTP session ID
     * @param kbName The knowledge base name (e.g., "SUMO")
     * @return Path to the session-specific UserAssertions.tptp file
     */
    public static Path getSessionUATPTPPath(String sessionId, String kbName) {

        return getSessionDir(sessionId).resolve(kbName + KB._userAssertionsTPTP);
    }

    /*********************************************************************************
     * Get the path to a session-specific UserAssertions TFF file.
     * @param sessionId The HTTP session ID
     * @param kbName The knowledge base name (e.g., "SUMO")
     * @return Path to the session-specific UserAssertions.tff file
     */
    public static Path getSessionUATFFPath(String sessionId, String kbName) {

        return getSessionDir(sessionId).resolve(kbName + KB._userAssertionsTFF);
    }

    /*********************************************************************************
     * Get the path to a session-specific UserAssertions THF file.
     * @param sessionId The HTTP session ID
     * @param kbName The knowledge base name (e.g., "SUMO")
     * @return Path to the session-specific UserAssertions.thf file
     */
    public static Path getSessionUATHFPath(String sessionId, String kbName) {

        return getSessionDir(sessionId).resolve(kbName + KB._userAssertionsTHF);
    }

    /*********************************************************************************
     * Get the session directory path.
     * @param sessionId The HTTP session ID
     * @return Path to the session directory
     */
    public static Path getSessionDir(String sessionId) {

        String kbDir = KBmanager.getMgr().getPref("kbDir");
        return Paths.get(kbDir, "sessions", sessionId);
    }

    /*********************************************************************************
     * Check if a session-specific TPTP file exists and is current.
     * @param sessionId The HTTP session ID
     * @param kb The knowledge base
     * @param lang The TPTP language ("tptp" or "tff")
     * @return true if the session file exists and is newer than user assertions
     */
    public static boolean isSessionFileValid(String sessionId, KB kb, String lang) {

        Path sessionFile = getSessionTPTPPath(sessionId, kb.name, lang);
        if (!Files.exists(sessionFile)) return false;
        Path sessionUAKif = getSessionUAPath(sessionId, kb.name);
        if (!Files.exists(sessionUAKif)) return true;
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
     * Generate a session-specific TPTP file with user assertions. Performs a full regeneration.
     * @param sessionId The HTTP session ID
     * @param kb The knowledge base
     * @param lang The TPTP language ("tptp" for FOF, "tff" for TFF)
     * @return The path to the generated session-specific TPTP file
     */
    public static Path generateSessionTPTP(String sessionId, KB kb, String lang) {

        Object lock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());
        synchronized (lock) {
            if (isSessionFileValid(sessionId, kb, lang)) {
                Set<Formula> derivedFacts = getSessionDerivedTypeFacts(sessionId);
                if (!derivedFacts.isEmpty()) patchSessionTPTP(sessionId, kb, lang, Collections.emptySet(), derivedFacts, getOrCreateSessionCache(sessionId, kb));
                return getSessionTPTPPath(sessionId, kb.name, lang);
            }
            Path sessionDir = getSessionDir(sessionId);
            Path sessionFile = getSessionTPTPPath(sessionId, kb.name, lang);
            Path tmpFile = sessionFile.resolveSibling(sessionFile.getFileName() + ".tmp");
            try {
                Files.createDirectories(sessionDir);
                Files.deleteIfExists(tmpFile);
                long startTime = System.currentTimeMillis();
                KBcache sessionCache = getOrCreateSessionCache(sessionId, kb);
                KBcache sharedCache = kb.kbCache;
                kb.kbCache = sessionCache;
                try {
                    if ("tptp".equals(lang) || "fof".equals(lang)) TPTPGenerationManager.generateFOFToPath(kb, tmpFile);
                    else if ("tff".equals(lang)) TPTPGenerationManager.generateTFFToPath(kb, tmpFile);
                    else throw new IllegalArgumentException("Unsupported TPTP language: " + lang);
                } finally {
                    kb.kbCache = sharedCache;
                }
                try {
                    Files.move(tmpFile, sessionFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                }
                catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmpFile, sessionFile, StandardCopyOption.REPLACE_EXISTING);
                }
                Set<Formula> derivedFacts = getSessionDerivedTypeFacts(sessionId);
                if (!derivedFacts.isEmpty()) patchSessionTPTP(sessionId, kb, lang, Collections.emptySet(), derivedFacts, getOrCreateSessionCache(sessionId, kb));
                sessionGenerationTimestamps.put(sessionId, System.currentTimeMillis());
                return sessionFile;
            }
            catch (IOException e) {
                LoggingUtils.log("ERROR", "Error generating session TPTP: " + e.getMessage());
                e.printStackTrace();
                try { Files.deleteIfExists(tmpFile); } catch (IOException ignore) {}
                throw new RuntimeException("Failed to generate session TPTP file", e);
            }
        }
    }


    /*********************************************************************************
     * Incrementally patch the session-specific TPTP file.
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

        final String normalizedLang = "tptp".equals(lang) ? "fof" : lang;
        final String ext            = "fof".equals(normalizedLang) ? "tptp" : normalizedLang;
        boolean hasAffected  = affected   != null && !affected.isEmpty();
        boolean hasNewForms  = newFormulas != null && !newFormulas.isEmpty();
        if (!hasAffected && !hasNewForms) return mergeBaseWithSessionUA(sessionId, kb, ext);
        Object lock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());
        synchronized (lock) {
            String kbDir = KBmanager.getMgr().getPref("kbDir");
            Path sharedBase  = Paths.get(kbDir, kb.name + "." + ext);
            Path sessionDir  = getSessionDir(sessionId);
            Path sessionFile = getSessionTPTPPath(sessionId, kb.name, ext);
            Path tmpFile     = sessionFile.resolveSibling(sessionFile.getFileName() + ".tmp");
            if (!Files.exists(sharedBase)) return null;
            Path baseFile = Files.exists(sessionFile) ? sessionFile : sharedBase;
            try {
                Files.createDirectories(sessionDir);
                Files.deleteIfExists(tmpFile);
                int affectedCount = hasAffected  ? affected.size()   : 0;
                int newCount      = hasNewForms  ? newFormulas.size() : 0;
                long startTime = System.currentTimeMillis();

                // 1. Build reverse index merging global axiomKey + this session's patch key
                Map<Formula, Set<String>> reverseIndex = buildReverseIndex(sessionId);

                // 2. Collect axiom names that must be commented out (retranslated formulas only)
                Set<String> toSkip = new HashSet<>();
                if (hasAffected) {
                    for (Formula f : affected) {
                        Set<String> names = reverseIndex.get(f);
                        if (names != null) toSkip.addAll(names);
                    }
                }
                KBcache sharedCache = kb.kbCache;
                kb.kbCache = sessionCache;
                Map<Formula, List<String>> retranslated;
                Map<Formula, List<String>> newTranslations;
                try {
                    retranslated    = hasAffected ? SUMOKBtoTPTPKB.retranslateFormulas(kb, affected, normalizedLang, sessionId) : Collections.emptyMap();
                    newTranslations = hasNewForms ? SUMOKBtoTPTPKB.retranslateFormulas(kb, newFormulas, normalizedLang, sessionId) : Collections.emptyMap();
                } finally {
                    kb.kbCache = sharedCache;
                }
                if (debugPatch) writePatchDebugLog(sessionDir, sessionId, normalizedLang, affected, toSkip, retranslated, newFormulas, newTranslations);
                String sanitizedKBName = kb.name.replaceAll("\\W", "_");
                // Session axiom key — isolated from the global axiomKey
                Map<String, Formula> sessionAxiomKey = getOrCreateSessionAxiomKey(sessionId);
                // Build a session-specific prefix so axiom names are globally unique
                // even when two sessions patch concurrently.
                // Use Math.abs(hashCode) of sessionId for a compact numeric discriminator;
                // prefix with "s" so the name starts with a letter (TPTP requirement).
                String sessionTag = "s" + Math.abs(sessionId.hashCode());
                AtomicInteger patchIdx = new AtomicInteger(1);
                try (BufferedReader reader = Files.newBufferedReader(baseFile, StandardCharsets.UTF_8);
                    BufferedWriter writer = Files.newBufferedWriter(tmpFile,  StandardCharsets.UTF_8)) {
                    Set<String> alreadyInBase = new HashSet<>();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String axiomName = extractAxiomName(line);
                        if (axiomName != null && toSkip.contains(axiomName)) writer.write("% [patched out] " + line);
                        else {
                            writer.write(line);
                            String existingBody = extractAxiomBody(line);
                            if (existingBody != null) alreadyInBase.add(existingBody);
                        }
                        writer.newLine();
                    }
                    writer.newLine();
                    writer.write("% --- Incremental patch: session " + sessionId + " ---");
                    writer.newLine();

                    // Helper: write one body string as a named axiom, recording in session key
                    Set<String> patchWritten = new HashSet<>(); // within-patch dedup

                    // Retranslated base formulas
                    for (Map.Entry<Formula, List<String>> entry : retranslated.entrySet()) {
                        for (String body : entry.getValue()) {
                            if (alreadyInBase.contains(body)) continue;
                            if (!patchWritten.add(body)) continue;
                            String name = "kb_" + sanitizedKBName + "_" + sessionTag + "_" + patchIdx.getAndIncrement();
                            writer.write(normalizedLang + "(" + name + ",axiom,(" + body + ")).");
                            writer.newLine();
                            sessionAxiomKey.put(name, entry.getKey());
                        }
                    }
                    for (Map.Entry<Formula, List<String>> entry : newTranslations.entrySet()) {
                        for (String body : entry.getValue()) {
                            if (alreadyInBase.contains(body)) continue;
                            if (!patchWritten.add(body)) continue;
                            String name = "kb_" + sanitizedKBName + "_" + sessionTag + "_" + patchIdx.getAndIncrement();
                            writer.write(normalizedLang + "(" + name + ",axiom,(" + body + ")).");
                            writer.newLine();
                            sessionAxiomKey.put(name, entry.getKey());
                        }
                    }
                }
                try {
                    Files.move(tmpFile, sessionFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmpFile, sessionFile, StandardCopyOption.REPLACE_EXISTING);
                }
                sessionGenerationTimestamps.put(sessionId, System.currentTimeMillis());
                return sessionFile;
            }
            catch (IOException e) {
                LoggingUtils.log("ERROR", e.getMessage());
                e.printStackTrace();
                try { Files.deleteIfExists(tmpFile); } catch (IOException ignore) {}
                throw new RuntimeException("Failed to patch session TPTP file", e);
            }
        }
    }

    /*********************************************************************************
     * Write a human-readable debug log for one {@link #patchSessionTPTP} call.
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
            int affectedCount = affected != null ? affected.size() : 0;
            w.write("--- AFFECTED FORMULAS (" + affectedCount + " formulas, " + toSkip.size() + " axiom names commented out) ---");
            w.newLine();
            if (affected != null && !affected.isEmpty()) {
                int idx = 1;
                for (Formula f : affected) {
                    w.newLine();
                    w.write("[" + idx++ + "] KIF: " + f.getFormula().replace('\n', ' '));
                    w.newLine();
                    w.write("    Source : " + f.sourceFile + " line " + f.startLine);
                    w.newLine();
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
                    List<String> bodies = retranslated.get(f);
                    if (bodies == null || bodies.isEmpty()) w.write("    STATUS : *** WARNING — no retranslated bodies produced;" + " formula is effectively removed from this session ***");
                    else {
                        w.write("    STATUS : OK — " + bodies.size() + " body(ies) regenerated");
                        w.newLine();
                        for (int b = 0; b < bodies.size(); b++) w.write("    Body[" + b + "]: " + bodies.get(b));
                    }
                    w.newLine();
                }
            }
            else {
                w.write("  (none)");
                w.newLine();
            }
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
                    if (bodies == null || bodies.isEmpty()) w.write("    STATUS : *** WARNING — no TPTP bodies produced for new formula ***");
                    else {
                        w.write("    STATUS : OK — " + bodies.size() + " body(ies)");
                        w.newLine();
                        for (int b = 0; b < bodies.size(); b++) w.write("    Body[" + b + "]: " + bodies.get(b));
                    }
                    w.newLine();
                }
            }
            else {
                w.write("  (none)");
                w.newLine();
            }
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
            LoggingUtils.log("ERROR", "failed to write log to "+ logFile + ": " + e.getMessage());
        }
    }

    /*********************************************************************************
     * Build a reverse index: Formula → Set of axiom names.
     * @param sessionId the HTTP session ID
     */
    private static Map<Formula, Set<String>> buildReverseIndex(String sessionId) {

        Map<Formula, Set<String>> rev = new HashMap<>();
        for (Map.Entry<String, Formula> e : SUMOKBtoTPTPKB.axiomKey.entrySet()) rev.computeIfAbsent(e.getValue(), k -> new HashSet<>()).add(e.getKey());
        Map<String, Formula> sessionKey = sessionAxiomKeys.get(sessionId);
        if (sessionKey != null) for (Map.Entry<String, Formula> e : sessionKey.entrySet()) rev.computeIfAbsent(e.getValue(), k -> new HashSet<>()).add(e.getKey());
        return rev;
    }

    /*********************************************************************************
     * Extract the axiom name from a TPTP formula line, or {@code null} if the
     * line is not a {@code kb_*} axiom.
     * @param line a raw line from a TPTP file
     * @return the axiom name (e.g., {@code "kb_SUMO_1234"}), or {@code null}
     */
    private static String extractAxiomName(String line) {

        String trimmed = line.trim();
        int parenIdx = trimmed.indexOf('(');
        if (parenIdx < 0) return null;
        String prefix = trimmed.substring(0, parenIdx);
        if (!prefix.equals("fof") && !prefix.equals("tff") && !prefix.equals("thf")) return null;
        int commaIdx = trimmed.indexOf(',', parenIdx);
        if (commaIdx < 0) return null;
        String name = trimmed.substring(parenIdx + 1, commaIdx);
        if (!name.startsWith("kb_")) return null;
        return name;
    }

    /*********************************************************************************
     * Extract the formula body from a single-line TPTP axiom, or {@code null} if the
     * line is not a single-line axiom or is a comment.
     * @param line a raw line from a TPTP file
     * @return the body string (e.g. {@code "![V1]: (subclass(V1,Entity))"}), or {@code null}
     */
    private static String extractAxiomBody(String line) {

        String trimmed = line.trim();
        if (trimmed.startsWith("%")) return null;
        if (!trimmed.endsWith(")).")) return null;
        int axiomIdx = trimmed.indexOf(",axiom,(");
        if (axiomIdx < 0) return null;
        int bodyStart = axiomIdx + ",axiom,(".length();
        return trimmed.substring(bodyStart, trimmed.length() - 3);
    }

    /*********************************************************************************
     * Remove all formulas tagged with {@code sessionId} from {@code kb.formulaMap} and the
     */
    private static void purgeSessionFormulas(KB kb, String sessionId) {

        kb.withUserAssertionLock(() -> {
            Set<String> toRemove = new HashSet<>();
            for (Map.Entry<String, Formula> e : kb.formulaMap.entrySet()) if (sessionId.equals(e.getValue().uaSessionId)) toRemove.add(e.getKey());
            if (toRemove.isEmpty()) return null;
            for (List<String> list : kb.formulas.values()) list.removeAll(toRemove);
            toRemove.forEach(kb.formulaMap::remove);
            LoggingUtils.log("Purged " + toRemove.size() + " UA formula(s) for session " + sessionId + " from KB " + kb.name);
            return null;
        });
    }

    /*********************************************************************************
     * Clean up all session-specific files for a given session.
     * Called when a session is destroyed.
     *
     * @param sessionId The HTTP session ID to clean up
     */
    public static void cleanupSession(String sessionId) {

        if (StringUtil.emptyString(sessionId)) return;
        sessionLocks.remove(sessionId);
        sessionGenerationTimestamps.remove(sessionId);
        sessionCaches.remove(sessionId);
        sessionDerivedTypeFacts.remove(sessionId);
        sessionAxiomKeys.remove(sessionId);
        batchModeActive.remove(sessionId);
        precomputedRegenRequired.remove(sessionId);
        for (KB kb : KBmanager.getMgr().kbs.values()) purgeSessionFormulas(kb, sessionId);
        Path sessionDir = getSessionDir(sessionId);
        if (!Files.exists(sessionDir)) return;
        LoggingUtils.log("Cleaning up session directory: " + sessionDir);
        try {
            Files.walk(sessionDir).sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    System.err.println("SessionTPTPManager: Failed to delete " + path + ": " + e.getMessage());
                }
            });
            LoggingUtils.log("Session cleanup complete for " + sessionId);
        }
        catch (IOException e) {
            System.err.println("SessionTPTPManager: Error during session cleanup: " + e.getMessage());
        }
    }

    /*********************************************************************************
     * Check if a session has generated TPTP files.
     * @param sessionId The HTTP session ID
     * @return true if this session has generated files
     */
    public static boolean hasSessionFiles(String sessionId) {

        return sessionGenerationTimestamps.containsKey(sessionId);
    }

    /*********************************************************************************
     * Get the timestamp of the last generation for a session.
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
                Files.createDirectories(sessionDir);
                Files.deleteIfExists(tmpFile);
                long startTime = System.currentTimeMillis();
                String kbDir = KBmanager.getMgr().getPref("kbDir");
                Path sharedBase = Paths.get(kbDir, kb.name + "." + lang);
                Path sessionUA = getSessionUATPTPPath(sessionId, kb.name);
                if ("tff".equals(lang)) sessionUA = getSessionUATFFPath(sessionId, kb.name);
                if (!Files.exists(sharedBase)) return generateSessionTPTP(sessionId, kb, lang);
                Files.copy(sharedBase, tmpFile, StandardCopyOption.REPLACE_EXISTING);
                if (Files.exists(sessionUA)) {
                    try (java.io.BufferedWriter writer = Files.newBufferedWriter(tmpFile, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND)) {
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
                try {
                    Files.move(tmpFile, sessionFile,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                }
                catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmpFile, sessionFile, StandardCopyOption.REPLACE_EXISTING);
                }
                sessionGenerationTimestamps.put(sessionId, System.currentTimeMillis());
                return sessionFile;
            }
            catch (IOException e) {
                LoggingUtils.log("ERROR", "Error merging base with UA: " + e.getMessage());
                e.printStackTrace();
                try {Files.deleteIfExists(tmpFile); } catch (IOException ignore) {}
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
     * @param <T>       the return type of the operation
     * @param sessionId the HTTP session ID (null or empty ⟹ no swap)
     * @param kb        the shared KB whose kbCache field is temporarily replaced
     * @param op        the operation to run with the session cache active
     * @return the value returned by {@code op}
     */
    public static <T> T withSessionCache(String sessionId, KB kb, Supplier<T> op) {

        if (sessionId == null || sessionId.isEmpty()) return op.get();
        KBcache sessionCache = getSessionCache(sessionId);
        if (sessionCache == null) return op.get();
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

    /********************************************************************
     * Materialize inherited type facts for a session instance assertion.
     * Example:
     *   (instance John Human)
     * becomes:
     *   (instance John Human)
     *   (instance John Physical)
     *   (instance John Object)
     * @author AI (needs careful review)
     * @param sessionId the session id
     * @param kb the KB
     * @param inst the instance term
     * @param directClass the directly asserted class
     * @param sessionCache the session-specific KBcache
     * @return derived type facts as Formula objects
     */
    private static Set<Formula> materializeInheritedInstanceFacts(String sessionId, KB kb, String inst, String directClass, KBcache sessionCache) {

        Set<Formula> result = new LinkedHashSet<>();
        if (StringUtil.emptyString(inst) || StringUtil.emptyString(directClass) || sessionCache == null || sessionCache.instanceOf == null) return result;
        Set<String> classes = sessionCache.instanceOf.get(inst);
        if (classes == null || classes.isEmpty()) return result;
        ConcurrentHashMap<String, Formula> stored = getOrCreateSessionDerivedTypeFacts(sessionId);
        for (String cls : new TreeSet<>(classes)) {
            if (StringUtil.emptyString(cls)) continue;
            if (cls.equals(directClass)) continue;
            if ("Entity".equals(cls)) continue;
            String kif = "(instance " + inst + " " + cls + ")";
            Formula f = stored.get(kif);
            if (f == null) {
                f = new Formula();
                f.read(kif);
                f.uaSessionId = sessionId;
                stored.put(kif, f);
            }
            result.add(f);
        }
        return result;
    }

    /********************************************************************
     * Return session-derived type facts, creating the map if needed.
     * @author AI (needs careful review)
     * @param sessionId the session id
     * @return derived type fact map
     */
    private static ConcurrentHashMap<String, Formula> getOrCreateSessionDerivedTypeFacts(String sessionId) {

        return sessionDerivedTypeFacts.computeIfAbsent(sessionId, id -> new ConcurrentHashMap<>());
    }

    /********************************************************************
     * Return all derived type facts for a session.
     * @author AI (needs careful review)
     * @param sessionId the session id
     * @return derived type facts
     */
    private static Set<Formula> getSessionDerivedTypeFacts(String sessionId) {

        Map<String, Formula> facts = sessionDerivedTypeFacts.get(sessionId);
        if (facts == null || facts.isEmpty()) return Collections.emptySet();
        return new LinkedHashSet<>(facts.values());
    }
}
