package com.articulate.sigma.trans;

import com.articulate.sigma.*;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;

import static org.junit.Assert.*;

/**
 * TPTP generation test that uses the current config.xml as-is.
 * Loads whatever KIF files are configured — may be a subset of the full SUMO KB.
 * Useful for fast iteration during development when config.xml is trimmed down.
 *
 * Does NOT wait for background FOF generation or validate which KIF files are loaded.
 *
 * Run manually:
 *   ant compile.test
 *   java -Xmx10g -Dsigma.tff.profile=true -cp "build/classes:build/test/classes:lib/*" \
 *     org.junit.runner.JUnitCore com.articulate.sigma.trans.TPTPGenerationTest
 *
 * @see TPTPGenerationFullKBTest for the full SUMO KB variant
 */
public class TPTPGenerationTest {

    protected static KB kb;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setup() {

        System.out.println("\n===== TPTPGenerationTest: Initializing from current config.xml =====");
        long startTime = System.currentTimeMillis();
        // Suppress background SUMO.tptp/SUMO.tff generation: this test drives
        // generation directly via generateFOFToPath/generateTFFToPath.
        TPTPGenerationManager.setSkipBackgroundGeneration(true);
        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("===== KB initialization time: " + (elapsed / 1000.0) + "s =====");
        System.out.println("===== Formula count: " + kb.formulaMap.size() + " =====");
        assertNotNull("KB should be loaded", kb);
        assertFalse("KB formulaMap should not be empty", kb.formulaMap.isEmpty());
    }

    @Test
    public void testGenerateFOF() throws Exception {

        Path outputPath = tempFolder.newFile("SUMO.tptp").toPath();

        System.out.println("\n===== TPTPGenerationTest: Generating FOF =====");
        long startTime = System.currentTimeMillis();
        TPTPGenerationManager.generateFOFToPath(kb, outputPath);
        long elapsed = System.currentTimeMillis() - startTime;

        printFileReport("FOF", outputPath, elapsed);

        // Save a persistent copy for cross-run comparison.
        // Run 1 → /tmp/SUMO_fof_run1.tptp, Run 2 → /tmp/SUMO_fof_run2.tptp
        // After two runs: diff /tmp/SUMO_fof_run1.tptp /tmp/SUMO_fof_run2.tptp
        Path run1 = Paths.get("/tmp/SUMO_fof_run1.tptp");
        Path run2 = Paths.get("/tmp/SUMO_fof_run2.tptp");
        Path saveTo = Files.exists(run1) ? run2 : run1;
        Files.copy(outputPath, saveTo, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("===== FOF copy saved to: " + saveTo + " =====");
        if (Files.exists(run1) && Files.exists(run2))
            System.out.println("===== Both runs saved. Compare with: diff " + run1 + " " + run2 + " =====");

        File file = outputPath.toFile();
        assertTrue("FOF file should exist", file.exists());
        assertTrue("FOF file should be non-empty (was " + file.length() + " bytes)", file.length() > 0);

        String content = new String(Files.readAllBytes(outputPath));
        assertTrue("FOF file should contain fof() declarations", content.contains("fof("));
        assertFalse("FOF file should not contain tff() declarations", content.contains("tff("));
    }

    @Test
    public void testGenerateTFF() throws Exception {

        Path outputPath = tempFolder.newFile("SUMO.tff").toPath();

        System.out.println("\n===== TPTPGenerationTest: Generating TFF =====");
        long startTime = System.currentTimeMillis();
        TPTPGenerationManager.generateTFFToPath(kb, outputPath);
        long elapsed = System.currentTimeMillis() - startTime;

        printFileReport("TFF", outputPath, elapsed);

        // Save a persistent copy for cross-run comparison.
        // Run 1 → /tmp/SUMO_tff_run1.tff, Run 2 → /tmp/SUMO_tff_run2.tff
        // After two runs: diff /tmp/SUMO_tff_run1.tff /tmp/SUMO_tff_run2.tff
        Path run1 = Paths.get("/tmp/SUMO_tff_run1.tff");
        Path run2 = Paths.get("/tmp/SUMO_tff_run2.tff");
        Path saveTo = Files.exists(run1) ? run2 : run1;
        Files.copy(outputPath, saveTo, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("===== TFF copy saved to: " + saveTo + " =====");
        if (Files.exists(run1) && Files.exists(run2))
            System.out.println("===== Both runs saved. Compare with: diff " + run1 + " " + run2 + " =====");

        File file = outputPath.toFile();
        assertTrue("TFF file should exist", file.exists());
        assertTrue("TFF file should be non-empty (was " + file.length() + " bytes)", file.length() > 0);

        String content = new String(Files.readAllBytes(outputPath));
        assertTrue("TFF file should contain tff() declarations", content.contains("tff("));
    }

    @Test
    public void testGenerateTHFModal() throws Exception {

        System.out.println("\n===== TPTPGenerationTest: Generating THF Modal =====");
        long startTime = System.currentTimeMillis();
        THFnew.transModalTHF(kb);
        long elapsed = System.currentTimeMillis() - startTime;

        String kbDir = KBmanager.getMgr().getPref("kbDir");
        Path filePath = Paths.get(kbDir, kb.name + "_modals.thf");
        printFileReport("THF Modal", filePath, elapsed);

        File file = filePath.toFile();
        assertTrue("THF Modal file should exist: " + file.getAbsolutePath(), file.exists());
        assertTrue("THF Modal file should be non-empty (was " + file.length() + " bytes)", file.length() > 0);

        String content = new String(Files.readAllBytes(filePath));
        assertTrue("THF Modal file should contain thf() declarations", content.contains("thf("));
    }

    @Test
    public void testGenerateTHFPlain() throws Exception {

        System.out.println("\n===== TPTPGenerationTest: Generating THF Plain =====");
        long startTime = System.currentTimeMillis();
        THFnew.transPlainTHF(kb);
        long elapsed = System.currentTimeMillis() - startTime;

        String kbDir = KBmanager.getMgr().getPref("kbDir");
        Path filePath = Paths.get(kbDir, kb.name + "_plain.thf");
        printFileReport("THF Plain", filePath, elapsed);

        File file = filePath.toFile();
        assertTrue("THF Plain file should exist: " + file.getAbsolutePath(), file.exists());
        assertTrue("THF Plain file should be non-empty (was " + file.length() + " bytes)", file.length() > 0);

        String content = new String(Files.readAllBytes(filePath));
        assertTrue("THF Plain file should contain thf() declarations", content.contains("thf("));
    }

    @Test
    public void testGenerateAllFormatsBaseline() throws Exception {

        System.out.println("\n\n========================================================");
        System.out.println("  TPTP Generation Baseline — Current Config");
        System.out.println("  Formula count: " + kb.formulaMap.size());
        System.out.println("========================================================\n");

        Path fofPath = tempFolder.newFile("SUMO_baseline.tptp").toPath();
        Path tffPath = tempFolder.newFile("SUMO_baseline.tff").toPath();
        String kbDir = KBmanager.getMgr().getPref("kbDir");
        Path thfModalPath = Paths.get(kbDir, kb.name + "_modals.thf");
        Path thfPlainPath = Paths.get(kbDir, kb.name + "_plain.thf");

        long totalStart = System.currentTimeMillis();

        // FOF
        long fofStart = System.currentTimeMillis();
        TPTPGenerationManager.generateFOFToPath(kb, fofPath);
        long fofElapsed = System.currentTimeMillis() - fofStart;

        // TFF
        long tffStart = System.currentTimeMillis();
        TPTPGenerationManager.generateTFFToPath(kb, tffPath);
        long tffElapsed = System.currentTimeMillis() - tffStart;

        // THF Modal
        long thfModalStart = System.currentTimeMillis();
        THFnew.transModalTHF(kb);
        long thfModalElapsed = System.currentTimeMillis() - thfModalStart;

        // THF Plain
        long thfPlainStart = System.currentTimeMillis();
        THFnew.transPlainTHF(kb);
        long thfPlainElapsed = System.currentTimeMillis() - thfPlainStart;

        long totalElapsed = System.currentTimeMillis() - totalStart;

        // Collect file details
        long fofSize = fofPath.toFile().length();
        long tffSize = tffPath.toFile().length();
        long thfModalSize = thfModalPath.toFile().length();
        long thfPlainSize = thfPlainPath.toFile().length();

        long fofLines = Files.lines(fofPath).count();
        long tffLines = Files.lines(tffPath).count();
        long thfModalLines = Files.lines(thfModalPath).count();
        long thfPlainLines = Files.lines(thfPlainPath).count();

        String fofHash = sha256(fofPath);
        String tffHash = sha256(tffPath);
        String thfModalHash = sha256(thfModalPath);
        String thfPlainHash = sha256(thfPlainPath);

        long fofAxioms = countPattern(fofPath, "fof(");
        long tffAxioms = countPattern(tffPath, "tff(");
        long thfModalAxioms = countPattern(thfModalPath, "thf(");
        long thfPlainAxioms = countPattern(thfPlainPath, "thf(");

        System.out.println("\n========== TPTP Generation Baseline Results ==========");
        System.out.println("  Config: current config.xml (" + kb.formulaMap.size() + " formulas)");
        System.out.println();
        printFileSummary("FOF",       fofElapsed,      fofSize,      fofLines,      fofAxioms,      fofHash);
        printFileSummary("TFF",       tffElapsed,      tffSize,      tffLines,      tffAxioms,      tffHash);
        printFileSummary("THF Modal", thfModalElapsed, thfModalSize, thfModalLines, thfModalAxioms, thfModalHash);
        printFileSummary("THF Plain", thfPlainElapsed, thfPlainSize, thfPlainLines, thfPlainAxioms, thfPlainHash);
        System.out.println("  ----------------------------------------------------");
        System.out.printf("  %-14s %10.1fs%n", "Total:", totalElapsed / 1000.0);
        System.out.println("======================================================\n");

        // Verify all files are non-empty
        assertTrue("FOF file should be non-empty", fofSize > 0);
        assertTrue("TFF file should be non-empty", tffSize > 0);
        assertTrue("THF Modal file should be non-empty", thfModalSize > 0);
        assertTrue("THF Plain file should be non-empty", thfPlainSize > 0);
    }

    // ---- Helpers ----

    private static void printFileSummary(String label, long elapsedMs, long bytes,
                                         long lines, long axioms, String sha256) {
        System.out.printf("  %-14s %8.1fs  %,12d bytes  %,8d lines  %,7d axioms%n",
                label + ":", elapsedMs / 1000.0, bytes, lines, axioms);
        System.out.println("    SHA-256: " + sha256);
    }

    private static void printFileReport(String label, Path path, long elapsedMs) throws Exception {
        long size = Files.size(path);
        long lines = Files.lines(path).count();
        String hash = sha256(path);
        System.out.printf("===== %s generation time: %.1fs =====%n", label, elapsedMs / 1000.0);
        System.out.printf("%s file: %,d bytes, %,d lines, SHA-256: %s%n", label, size, lines, hash);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(path)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1)
                md.update(buf, 0, n);
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder(64);
        for (byte b : digest)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static long countPattern(Path path, String pattern) throws IOException {
        long count = 0;
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(pattern))
                    count++;
            }
        }
        return count;
    }
}
