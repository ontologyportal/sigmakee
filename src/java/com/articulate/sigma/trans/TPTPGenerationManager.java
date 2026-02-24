/* This code is copyright Articulate Software (c) 2003-2025.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
in any writings, briefings, publications, presentations, or other representations
of any software which incorporates, builds on, or uses this code.

Manages background generation of TPTP translation files (FOF, TFF, THF).
This prevents on-demand delays when users first request inference by
pre-generating all formats at startup.
*/

package com.articulate.sigma.trans;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coordinates background generation of TPTP translation files.
 * Provides synchronization for inference requests that arrive before
 * background generation completes.
 */
public class TPTPGenerationManager {

    private static final AtomicBoolean fofGenerating = new AtomicBoolean(false);
    private static final AtomicBoolean tffGenerating = new AtomicBoolean(false);
    private static final AtomicBoolean thfModalGenerating = new AtomicBoolean(false);
    private static final AtomicBoolean thfPlainGenerating = new AtomicBoolean(false);

    private static volatile CountDownLatch fofLatch = new CountDownLatch(1);
    private static volatile CountDownLatch tffLatch = new CountDownLatch(1);
    private static volatile CountDownLatch thfModalLatch = new CountDownLatch(1);
    private static volatile CountDownLatch thfPlainLatch = new CountDownLatch(1);

    private static final AtomicBoolean fofReady = new AtomicBoolean(false);
    private static final AtomicBoolean tffReady = new AtomicBoolean(false);
    private static final AtomicBoolean thfModalReady = new AtomicBoolean(false);
    private static final AtomicBoolean thfPlainReady = new AtomicBoolean(false);

    private static ExecutorService executor = null;

    private static final Object GEN_LOCK = new Object();

    /**
     * When true, {@link #startBackgroundGeneration()} returns immediately without
     * spawning any threads.  All latches are counted down so {@code waitFor*()} calls
     * do not block.  Intended for tests that drive generation directly via
     * {@link #generateFOFToPath} / {@link #generateTFFToPath}.
     */
    private static final AtomicBoolean skipBackgroundGeneration = new AtomicBoolean(false);

    public static void setSkipBackgroundGeneration(boolean skip) {
        skipBackgroundGeneration.set(skip);
    }


    /**
     * Start background generation of all TPTP formats for all KBs.
     * This should be called after KBmanager initialization is complete.
     *
     * FOF and TFF now run in PARALLEL on separate threads since the shared
     * static lang/hideNumbers/qlist/varmap/numericConstantTypes/filterMessage
     * fields have been converted to ThreadLocal.
     */
    public static void startBackgroundGeneration() {
        System.out.println("TPTPGenerationManager: Starting background TPTP generation");

        // Reset all latches and flags for fresh generation
        fofLatch = new CountDownLatch(1);
        tffLatch = new CountDownLatch(1);
        thfModalLatch = new CountDownLatch(1);
        thfPlainLatch = new CountDownLatch(1);

        fofReady.set(false);
        tffReady.set(false);
        thfModalReady.set(false);
        thfPlainReady.set(false);

        if (skipBackgroundGeneration.get()) {
            System.out.println("TPTPGenerationManager: Background generation suppressed (skipBackgroundGeneration=true)");
            fofLatch.countDown();
            tffLatch.countDown();
            thfModalLatch.countDown();
            thfPlainLatch.countDown();
            return;
        }

        // Use 4 threads: FOF, TFF, THF Modal, THF Plain all in parallel
        executor = Executors.newFixedThreadPool(4);

        for (KB kb : KBmanager.getMgr().kbs.values()) {
            // FOF on its own thread
            executor.submit(() -> {
                String kbDir = KBmanager.getMgr().getPref("kbDir");
                String infFilename = kbDir + File.separator + kb.name + ".tptp";
                File infFile = new File(infFilename);
                if (infFile.exists() && !KBmanager.getMgr().infFileOld()) {
                    System.out.println("TPTPGenerationManager: FOF file is current: " + infFilename +
                            "; rebuilding axiomKey in background for incremental patching");
                    // Mark FOF file ready immediately (file is current for prover use).
                    // Rebuild axiomKey asynchronously — patchSessionTPTP degrades gracefully
                    // (no stale-axiom commenting-out) if a tell() arrives before rebuild completes.
                    new Thread(() -> rebuildAxiomKey(kb), "axiomKey-rebuild-" + kb.name).start();
                    fofReady.set(true);
                    fofLatch.countDown();
                } else {
                    generateFOF(kb);
                }
            });

            // TFF on its own thread (parallel with FOF)
            executor.submit(() -> {
                String kbDir = KBmanager.getMgr().getPref("kbDir");
                String infFilename = kbDir + File.separator + kb.name + ".tff";
                File infFile = new File(infFilename);
                if (infFile.exists() && !KBmanager.getMgr().infFileOld()) {
                    System.out.println("TPTPGenerationManager: TFF file already exists and is current: " + infFilename);
                    tffReady.set(true);
                    tffLatch.countDown();
                } else {
                    generateTFF(kb);
                }
            });

            // THF can run in parallel (different code path)
            executor.submit(() -> generateTHFModal(kb));
            executor.submit(() -> generateTHFPlain(kb));
        }

        executor.shutdown();
    }

    public static void generateProperFile(KB kb, String lang) {
        if (skipBackgroundGeneration.get()) return;
        synchronized (GEN_LOCK) {
            if ("fof".equals(lang) || "tptp".equals(lang)) {
                generateFOF(kb);
            } else if ("tff".equals(lang)) {
                generateTFF(kb);
            }
        }
    }

    /**
     * Generate FOF (First-Order Form) TPTP file for a KB.
     */
    public static void generateFOF(KB kb) {

        if (!fofGenerating.compareAndSet(false, true)) {
            return; // Already generating
        }

        String kbDir = KBmanager.getMgr().getPref("kbDir");
        String infFilename = kbDir + File.separator + kb.name + ".tptp";

        Path target = java.nio.file.Paths.get(infFilename);
        Path tmp    = java.nio.file.Paths.get(infFilename + ".tmp");

        try {
            System.out.println("===== TPTPGenerationManager: Generating FOF file: " + infFilename);
            long startTime = System.currentTimeMillis();

            // Ensure we don't leave a stale tmp around
            try { java.nio.file.Files.deleteIfExists(tmp); } catch (Exception ignore) {}

            // Set BOTH static language fields to FOF
            SUMOKBtoTPTPKB.setLang("fof");
            SUMOformulaToTPTPformula.setLang("fof");
            SUMOformulaToTPTPformula.setHideNumbers(true);

            // IMPORTANT: write to tmp, not to target
            try (java.io.PrintWriter pw = new java.io.PrintWriter(
                    java.nio.file.Files.newBufferedWriter(tmp, java.nio.charset.StandardCharsets.UTF_8))) {

                SUMOKBtoTPTPKB skb = new SUMOKBtoTPTPKB();
                skb.kb = kb;

                // Keep passing the "real" filename for stable file(...) metadata, but write into pw(tmp)
                skb.writeFile(infFilename, null, false, pw);
            }

            // Atomic replace (or fallback)
            try {
                Files.move(tmp, target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target,
                        StandardCopyOption.REPLACE_EXISTING);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("==== TPTPGenerationManager: FOF generation complete in " + (elapsed / 1000.0) + "s");
            fofReady.set(true);

        } catch (Exception e) {
            System.err.println("TPTPGenerationManager: Error generating FOF: " + e.getMessage());
            e.printStackTrace();
            // best effort cleanupT
            try { java.nio.file.Files.deleteIfExists(tmp); } catch (Exception ignore) {}
        } finally {
            // Clean up ThreadLocal state to prevent leaks in thread pools
            SUMOformulaToTPTPformula.clearThreadLocal();
            SUMOKBtoTPTPKB.clearThreadLocal();
            SUMOtoTFAform.clearThreadLocal();
            fofGenerating.set(false);
            fofLatch.countDown();
        }
    }


    /**
     * Generate TFF (Typed First-order Form) TPTP file for a KB.
     */
    private static void generateTFF(KB kb) {

        if (!tffGenerating.compareAndSet(false, true)) {
            return; // Already generating
        }

        String kbDir = KBmanager.getMgr().getPref("kbDir");
        String infFilename = kbDir + File.separator + kb.name + ".tff";

        Path target = Paths.get(infFilename);
        Path tmp    = Paths.get(infFilename + ".tmp");

        try {
            System.out.println("==== TPTPGenerationManager: Generating TFF file: " + infFilename);
            long startTime = System.currentTimeMillis();

            // Ensure we don't leave a stale tmp around
            try { Files.deleteIfExists(tmp); } catch (Exception ignore) {}

            // Set BOTH static language fields to TFF
            SUMOKBtoTPTPKB.setLang("tff");
            SUMOformulaToTPTPformula.setLang("tff");

            // IMPORTANT: write to tmp, not target
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(tmp, StandardCharsets.UTF_8))) {

                if (!kb.formulaMap.isEmpty()) {
                    SUMOKBtoTFAKB stff = new SUMOKBtoTFAKB();
                    stff.kb = kb;

                    SUMOtoTFAform.initOnce();

                    stff.writeSorts(pw);
                    // Keep passing the "real" filename for metadata, but write into pw(tmp)
                    stff.writeFile(infFilename, null, false, pw);

                    if (SUMOKBtoTPTPKB.CWA)
                        pw.println(StringUtil.arrayListToCRLFString(CWAUNA.run(kb)));

                    stff.printTFFNumericConstants(pw);
                }
            }

            // Atomic replace (or fallback)
            try {
                Files.move(tmp, target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target,
                        StandardCopyOption.REPLACE_EXISTING);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("==== TPTPGenerationManager: TFF generation complete in " + (elapsed / 1000.0) + "s");
            tffReady.set(true);

        } catch (Exception e) {
            System.err.println("TPTPGenerationManager: Error generating TFF: " + e.getMessage());
            e.printStackTrace();
            // best-effort cleanup
            try { Files.deleteIfExists(tmp); } catch (Exception ignore) {}
        } finally {
            // Clean up ThreadLocal state to prevent leaks in thread pools
            SUMOformulaToTPTPformula.clearThreadLocal();
            SUMOKBtoTPTPKB.clearThreadLocal();
            SUMOtoTFAform.clearThreadLocal();
            tffGenerating.set(false);
            tffLatch.countDown();
        }
    }


    /**
     * Generate THF Modal (Higher-order Form with modals) file for a KB.
     */
    private static void generateTHFModal(KB kb) {
        if (!thfModalGenerating.compareAndSet(false, true)) {
            return; // Already generating
        }

        try {
            String kbDir = KBmanager.getMgr().getPref("kbDir");
            String thfFilename = kbDir + File.separator + kb.name + "_modals.thf";
            File thfFile = new File(thfFilename);

            // Check if file already exists and is not stale
            if (thfFile.exists() && !KBmanager.getMgr().infFileOld()) {
                System.out.println("TPTPGenerationManager: THF Modal file already exists and is current: " + thfFilename);
                thfModalReady.set(true);
                return;
            }

            System.out.println("==== TPTPGenerationManager: Generating THF Modal file: " + thfFilename);
            long startTime = System.currentTimeMillis();

            THFnew.transModalTHF(kb);

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("==== TPTPGenerationManager: THF Modal generation complete in " + (elapsed / 1000.0) + "s");
            thfModalReady.set(true);
        } catch (Exception e) {
            System.err.println("TPTPGenerationManager: Error generating THF Modal: " + e.getMessage());
            e.printStackTrace();
        } finally {
            thfModalGenerating.set(false);
            thfModalLatch.countDown();
        }
    }

    /**
     * Generate THF Plain (Higher-order Form without modals) file for a KB.
     */
    private static void generateTHFPlain(KB kb) {
        if (!thfPlainGenerating.compareAndSet(false, true)) {
            return; // Already generating
        }

        try {
            String kbDir = KBmanager.getMgr().getPref("kbDir");
            String thfFilename = kbDir + File.separator + kb.name + "_plain.thf";
            File thfFile = new File(thfFilename);

            // Check if file already exists and is not stale
            if (thfFile.exists() && !KBmanager.getMgr().infFileOld()) {
                System.out.println("TPTPGenerationManager: THF Plain file already exists and is current: " + thfFilename);
                thfPlainReady.set(true);
                return;
            }

            System.out.println("==== TPTPGenerationManager: Generating THF Plain file: " + thfFilename);
            long startTime = System.currentTimeMillis();

            THFnew.transPlainTHF(kb);

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("==== TPTPGenerationManager: THF Plain generation complete in " + (elapsed / 1000.0) + "s");
            thfPlainReady.set(true);
        } catch (Exception e) {
            System.err.println("TPTPGenerationManager: Error generating THF Plain: " + e.getMessage());
            e.printStackTrace();
        } finally {
            thfPlainGenerating.set(false);
            thfPlainLatch.countDown();
        }
    }

    /**
     * Wait for FOF generation to complete.
     * @param timeoutSec Maximum time to wait in seconds
     * @return true if generation completed successfully, false if timed out
     */
    public static boolean waitForFOF(int timeoutSec) {
        if (fofReady.get()) return true;
        try {
            System.out.println("TPTPGenerationManager: Waiting for FOF generation (timeout: " + timeoutSec + "s)...");
            boolean completed = fofLatch.await(timeoutSec, TimeUnit.SECONDS);
            if (completed) {
                System.out.println("TPTPGenerationManager: FOF generation wait completed");
            } else {
                System.out.println("TPTPGenerationManager: FOF generation wait timed out");
            }
            return completed && fofReady.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Wait for TFF generation to complete.
     * @param timeoutSec Maximum time to wait in seconds
     * @return true if generation completed successfully, false if timed out
     */
    public static boolean waitForTFF(int timeoutSec) {
        if (tffReady.get()) return true;
        try {
            System.out.println("TPTPGenerationManager: Waiting for TFF generation (timeout: " + timeoutSec + "s)...");
            boolean completed = tffLatch.await(timeoutSec, TimeUnit.SECONDS);
            if (completed) {
                System.out.println("TPTPGenerationManager: TFF generation wait completed");
            } else {
                System.out.println("TPTPGenerationManager: TFF generation wait timed out");
            }
            return completed && tffReady.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Wait for THF Modal generation to complete.
     * @param timeoutSec Maximum time to wait in seconds
     * @return true if generation completed successfully, false if timed out
     */
    public static boolean waitForTHFModal(int timeoutSec) {
        if (thfModalReady.get()) return true;
        try {
            System.out.println("TPTPGenerationManager: Waiting for THF Modal generation (timeout: " + timeoutSec + "s)...");
            boolean completed = thfModalLatch.await(timeoutSec, TimeUnit.SECONDS);
            if (completed) {
                System.out.println("TPTPGenerationManager: THF Modal generation wait completed");
            } else {
                System.out.println("TPTPGenerationManager: THF Modal generation wait timed out");
            }
            return completed && thfModalReady.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Wait for THF Plain generation to complete.
     * @param timeoutSec Maximum time to wait in seconds
     * @return true if generation completed successfully, false if timed out
     */
    public static boolean waitForTHFPlain(int timeoutSec) {
        if (thfPlainReady.get()) return true;
        try {
            System.out.println("TPTPGenerationManager: Waiting for THF Plain generation (timeout: " + timeoutSec + "s)...");
            boolean completed = thfPlainLatch.await(timeoutSec, TimeUnit.SECONDS);
            if (completed) {
                System.out.println("TPTPGenerationManager: THF Plain generation wait completed");
            } else {
                System.out.println("TPTPGenerationManager: THF Plain generation wait timed out");
            }
            return completed && thfPlainReady.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Check if FOF generation is ready.
     */
    public static boolean isFOFReady() {
        return fofReady.get();
    }

    /**
     * Check if TFF generation is ready.
     */
    public static boolean isTFFReady() {
        return tffReady.get();
    }

    /**
     * Check if THF Modal generation is ready.
     */
    public static boolean isTHFModalReady() {
        return thfModalReady.get();
    }

    /**
     * Check if THF Plain generation is ready.
     */
    public static boolean isTHFPlainReady() {
        return thfPlainReady.get();
    }

    /**
     * Check if FOF generation is currently in progress.
     */
    public static boolean isFOFGenerating() {
        return fofGenerating.get();
    }

    /**
     * Check if TFF generation is currently in progress.
     */
    public static boolean isTFFGenerating() {
        return tffGenerating.get();
    }

    /**
     * Check if THF Modal generation is currently in progress.
     */
    public static boolean isTHFModalGenerating() {
        return thfModalGenerating.get();
    }

    /**
     * Check if THF Plain generation is currently in progress.
     */
    public static boolean isTHFPlainGenerating() {
        return thfPlainGenerating.get();
    }

    /**
     * Check if any background generation is currently in progress.
     * Used to prevent concurrent serialization during background generation.
     */
    public static boolean isBackgroundGenerating() {
        return fofGenerating.get() || tffGenerating.get() ||
               thfModalGenerating.get() || thfPlainGenerating.get();
    }

    /**
     * Shutdown the executor service gracefully.
     */
    public static void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /*********************************************************************************
     * Generate FOF (First-Order Form) TPTP to a custom output path.
     * This is used for session-specific TPTP generation.
     *
     * @param kb The knowledge base
     * @param outputPath The path to write the TPTP file
     * @throws IOException if file operations fail
     */
    public static void generateFOFToPath(KB kb, Path outputPath) throws IOException {

        try {
            System.out.println("TPTPGenerationManager: Generating FOF to custom path: " + outputPath);
            long startTime = System.currentTimeMillis();

            // Set ThreadLocal language fields to FOF
            SUMOKBtoTPTPKB.setLang("fof");
            SUMOformulaToTPTPformula.setLang("fof");
            SUMOformulaToTPTPformula.setHideNumbers(true);

            // Redirect axiomKey writes to a session-local map so this session-specific
            // generation does not overwrite the global SUMOKBtoTPTPKB.axiomKey, which
            // must only track shared base-KB axiom names.
            SUMOKBtoTPTPKB.localAxiomKeyOverride.set(new HashMap<>());
            try {
                try (PrintWriter pw = new PrintWriter(
                        Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8))) {

                    SUMOKBtoTPTPKB skb = new SUMOKBtoTPTPKB();
                    skb.kb = kb;
                    skb.writeFile(kb.name, null, false, pw);
                }
            } finally {
                SUMOKBtoTPTPKB.localAxiomKeyOverride.remove();
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("TPTPGenerationManager: FOF generation to custom path complete in " +
                               (elapsed / 1000.0) + "s");

        } finally {
            SUMOformulaToTPTPformula.clearThreadLocal();
            SUMOKBtoTPTPKB.clearThreadLocal();
            SUMOtoTFAform.clearThreadLocal();
        }
    }

    /**
     * Re-runs the FOF translation pipeline against the shared KB, writing to a
     * null {@link java.io.PrintWriter} (no disk I/O), solely to populate
     * {@link SUMOKBtoTPTPKB#axiomKey} in memory.
     *
     * <p>Called on warm starts when {@code SUMO.tptp} already exists and is current,
     * so normal {@link #generateFOF} is skipped.  Without this, {@code axiomKey}
     * would stay empty until the server is restarted with changed KIF files, causing
     * every first {@code tell()} in a session to fall back to full TPTP regeneration.
     *
     * <p>Runs on a background thread; {@link #fofReady} is already {@code true} by
     * the time this is launched.  {@link SessionTPTPManager#patchSessionTPTP} degrades
     * gracefully (no stale-axiom commenting-out) if {@code tell()} arrives before
     * this completes.
     *
     * @param kb the shared knowledge base (must contain only base formulas, no user assertions)
     */
    private static void rebuildAxiomKey(KB kb) {
        try {
            System.out.println("TPTPGenerationManager: Rebuilding axiomKey in background (warm start, no I/O)...");
            long start = System.currentTimeMillis();
            String kbDir = KBmanager.getMgr().getPref("kbDir");
            String infFilename = kbDir + java.io.File.separator + kb.name + ".tptp";
            SUMOKBtoTPTPKB.setLang("fof");
            SUMOformulaToTPTPformula.setLang("fof");
            SUMOformulaToTPTPformula.setHideNumbers(true);
            // Null writer: we want the axiomKey side-effect only, not file output.
            try (java.io.PrintWriter pw = new java.io.PrintWriter(java.io.Writer.nullWriter())) {
                SUMOKBtoTPTPKB skb = new SUMOKBtoTPTPKB();
                skb.kb = kb;
                skb.writeFile(infFilename, null, false, pw);
            }
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("TPTPGenerationManager: axiomKey rebuilt in " +
                    (elapsed / 1000.0) + "s — " + SUMOKBtoTPTPKB.axiomKey.size() + " entries");
        } catch (Exception e) {
            System.err.println("TPTPGenerationManager: Failed to rebuild axiomKey: " + e.getMessage());
            e.printStackTrace();
        } finally {
            SUMOformulaToTPTPformula.clearThreadLocal();
            SUMOKBtoTPTPKB.clearThreadLocal();
            SUMOtoTFAform.clearThreadLocal();
        }
    }

    /**
     * Generate TFF (Typed First-order Form) TPTP to a custom output path.
     * This is used for session-specific TPTP generation.
     *
     * @param kb The knowledge base
     * @param outputPath The path to write the TPTP file
     * @throws IOException if file operations fail
     */
    public static void generateTFFToPath(KB kb, Path outputPath) throws IOException {

        try {
            System.out.println("TPTPGenerationManager: Generating TFF to custom path: " + outputPath);
            long startTime = System.currentTimeMillis();

            // Set ThreadLocal language fields to TFF
            SUMOKBtoTPTPKB.setLang("tff");
            SUMOformulaToTPTPformula.setLang("tff");

            try (PrintWriter pw = new PrintWriter(
                    Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8))) {

                if (!kb.formulaMap.isEmpty()) {
                    SUMOKBtoTFAKB stff = new SUMOKBtoTFAKB();
                    stff.kb = kb;

                    SUMOtoTFAform.initOnce();

                    stff.writeSorts(pw);
                    stff.writeFile(outputPath.toString(), null, false, pw);

                    if (SUMOKBtoTPTPKB.CWA) {
                        pw.println(StringUtil.arrayListToCRLFString(CWAUNA.run(kb)));
                    }

                    stff.printTFFNumericConstants(pw);
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("TPTPGenerationManager: TFF generation to custom path complete in " +
                               (elapsed / 1000.0) + "s");

        } finally {
            SUMOformulaToTPTPformula.clearThreadLocal();
            SUMOKBtoTPTPKB.clearThreadLocal();
            SUMOtoTFAform.clearThreadLocal();
        }
    }

    /**
     * Get the generation lock for external synchronization.
     * Used by SessionTPTPManager to coordinate with background generation.
     */
    public static Object getGenerationLock() {
        return GEN_LOCK;
    }
}
