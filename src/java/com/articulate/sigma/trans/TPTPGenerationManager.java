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
import java.nio.file.Files;
import java.nio.file.Paths;
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

    /**
     * Start background generation of all TPTP formats for all KBs.
     * This should be called after KBmanager initialization is complete.
     *
     * FOF and TFF run SEQUENTIALLY on the same thread to avoid race conditions
     * with the static SUMOKBtoTPTPKB.lang and SUMOformulaToTPTPformula.lang fields.
     * THF generation can run in parallel (uses different code path).
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

        // Use 3 threads: FOF→TFF sequential on one thread, THF Modal on another, THF Plain on third
        executor = Executors.newFixedThreadPool(3);

        for (KB kb : KBmanager.getMgr().kbs.values()) {
            // FOF and TFF run SEQUENTIALLY on same thread to avoid lang field race condition
            executor.submit(() -> {
                System.out.println("TPTPGenerationManager: FOF Generating...");
                generateFOF(kb);   // FOF first
                System.out.println("TPTPGenerationManager: FOF Finished...");
//                KBmanager.serialize();
                System.out.println("TPTPGenerationManager: TFF Generating...");
                generateTFF(kb);   // TFF after FOF completes
                // Serialize once after both complete
                System.out.println("TPTPGenerationManager: TFF complete...");
//                KBmanager.serialize();
            });

            // THF can run in parallel (different code path, no shared lang field)
            executor.submit(() -> generateTHFModal(kb));
            executor.submit(() -> generateTHFPlain(kb));
        }

        executor.shutdown();
    }

    public static void generateProperFile(KB kb, String lang){
        if ("fof".equals(lang) || ("tptp".equals(lang))){
            generateFOF(kb);
        } else if ("tff".equals(lang) ) {
            generateTFF(kb);
        }

    }

    /**
     * Generate FOF (First-Order Form) TPTP file for a KB.
     */
    public static void generateFOF(KB kb) {
        if (!fofGenerating.compareAndSet(false, true)) {
            return; // Already generating
        }

        // Save current lang settings
        String originalLang = SUMOKBtoTPTPKB.lang;
        String originalLang2 = SUMOformulaToTPTPformula.lang;

        try {
            String kbDir = KBmanager.getMgr().getPref("kbDir");
            String infFilename = kbDir + File.separator + kb.name + ".tptp";
            File infFile = new File(infFilename);

            // Check if file already exists and is not stale
            if (infFile.exists() && !KBmanager.getMgr().infFileOld()) {
                System.out.println("TPTPGenerationManager: FOF file already exists and is current: " + infFilename);
                fofReady.set(true);
                return;
            }

            System.out.println("===== TPTPGenerationManager: Generating FOF file: " + infFilename);
            long startTime = System.currentTimeMillis();

            // Set BOTH static language fields to FOF
            SUMOKBtoTPTPKB.lang = "fof";
            SUMOformulaToTPTPformula.lang = "fof";

            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(infFilename)))) {
                if (!kb.formulaMap.isEmpty()) {
                    SUMOKBtoTPTPKB skb = new SUMOKBtoTPTPKB();
                    skb.kb = kb;
                    skb.writeFile(infFilename, null, false, pw);
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("==== TPTPGenerationManager: FOF generation complete in " + (elapsed / 1000.0) + "s");
            fofReady.set(true);
        } catch (Exception e) {
            System.err.println("TPTPGenerationManager: Error generating FOF: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Restore original lang settings
            SUMOKBtoTPTPKB.lang = originalLang;
            SUMOformulaToTPTPformula.lang = originalLang2;
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

        // Save current lang settings
        String originalLang = SUMOKBtoTPTPKB.lang;
        String originalLang2 = SUMOformulaToTPTPformula.lang;

        try {
            String kbDir = KBmanager.getMgr().getPref("kbDir");
            String infFilename = kbDir + File.separator + kb.name + ".tff";
            File infFile = new File(infFilename);

            // Check if file already exists and is not stale
            if (infFile.exists() && !KBmanager.getMgr().infFileOld()) {
                System.out.println("TPTPGenerationManager: TFF file already exists and is current: " + infFilename);
                tffReady.set(true);
                return;
            }

            System.out.println("==== TPTPGenerationManager: Generating TFF file: " + infFilename);
            long startTime = System.currentTimeMillis();

            // Set BOTH static language fields to TFF
            SUMOKBtoTPTPKB.lang = "tff";
            SUMOformulaToTPTPformula.lang = "tff";

            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(infFilename)))) {
                if (!kb.formulaMap.isEmpty()) {
                    SUMOKBtoTFAKB stff = new SUMOKBtoTFAKB();
                    stff.kb = kb;
                    SUMOtoTFAform.initOnce();
                    stff.writeSorts(pw);
                    stff.writeFile(infFilename, null, false, pw);
                    if (SUMOKBtoTPTPKB.CWA)
                        pw.println(StringUtil.arrayListToCRLFString(CWAUNA.run(kb)));
                    stff.printTFFNumericConstants(pw);
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("==== TPTPGenerationManager: TFF generation complete in " + (elapsed / 1000.0) + "s");
            tffReady.set(true);
        } catch (Exception e) {
            System.err.println("TPTPGenerationManager: Error generating TFF: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Restore original lang settings
            SUMOKBtoTPTPKB.lang = originalLang;
            SUMOformulaToTPTPformula.lang = originalLang2;
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
}
