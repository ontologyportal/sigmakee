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
import java.util.concurrent.ConcurrentHashMap;

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

    /** Per-session locks for thread-safe generation */
    private static final ConcurrentHashMap<String, Object> sessionLocks = new ConcurrentHashMap<>();

    /** Track which sessions have generated files (avoids redundant regeneration) */
    private static final ConcurrentHashMap<String, Long> sessionGenerationTimestamps = new ConcurrentHashMap<>();

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

        // Check if session file is newer than user assertions file
        String kbDir = KBmanager.getMgr().getPref("kbDir");
        File uaKif = new File(kbDir, kb.name + KB._userAssertionsString);

        if (!uaKif.exists()) {
            // No user assertions, session file is valid
            return true;
        }

        try {
            long sessionFileTime = Files.getLastModifiedTime(sessionFile).toMillis();
            long uaFileTime = uaKif.lastModified();
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

                if ("tptp".equals(lang) || "fof".equals(lang)) {
                    TPTPGenerationManager.generateFOFToPath(kb, tmpFile);
                } else if ("tff".equals(lang)) {
                    TPTPGenerationManager.generateTFFToPath(kb, tmpFile);
                } else {
                    throw new IllegalArgumentException("Unsupported TPTP language: " + lang);
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
     * Clean up all session-specific files for a given session.
     * Called when a session is destroyed.
     *
     * @param sessionId The HTTP session ID to clean up
     */
    public static void cleanupSession(String sessionId) {

        if (StringUtil.emptyString(sessionId)) {
            return;
        }

        // Remove locks and timestamps
        sessionLocks.remove(sessionId);
        sessionGenerationTimestamps.remove(sessionId);

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
}
