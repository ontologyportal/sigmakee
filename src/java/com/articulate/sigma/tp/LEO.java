/** This code is a modified version of the LEO class.  It connects LEO-III
 * to the newer THF translator (THFnew) instead of the older THF translator.
 * The primary changes are:
 *  - Import the THFnew class.
 *  - When a THF file needs to be generated, call THFnew.transPlainTHF to
 *    translate the SUMO knowledge base into a THF file.
 *  - Rename the generated “_plain.thf” file to the expected “.thf” name so
 *    that the rest of the code can find it.
 *
 * Other behaviour is left unchanged.  This file lives in the same package as
 * the original LEO class so that it can be dropped in as a replacement.
 */

package com.articulate.sigma.tp;

import com.articulate.sigma.Formula;
import com.articulate.sigma.FormulaPreprocessor;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.trans.*;
import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.StringUtil;
import tptp_parser.TPTPFormula;

import java.io.*;
import java.util.*;

/**
 * Class for invoking the latest version of LEO-III from Java.  It now uses
 * THFnew to translate the knowledge base into THF syntax.  To avoid bugs
 * or crashes from the old THF implementation, only the connection to
 * THFnew has been changed; all other logic remains the same.
 *
 * It should invoke a command like:
 *     ~/workspace/Leo-III/Leo-III-1.6/bin/leo3 /home/user/.sigmakee/KBs/SUMO.thf -t 60 -p
 */
public class LEO {

    public StringBuilder qlist = null; // quantifier list in order for answer extraction
    public List<String> output = new ArrayList<>();
    public static int axiomIndex = 0;
    public static boolean debug = false;

    // === NEW: Full result structure for error handling ===
    private ATPResult result;

    /** *************************************************************
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        for (String s : output)
            sb.append(s).append("\n");
        return sb.toString();
    }

    /** *************************************************************
     * Get the full ATPResult for this run.
     * This provides detailed execution metadata, SZS status, and error info.
     *
     * @return The ATPResult, or null if run() hasn't been called
     */
    public ATPResult getResult() {
        return result;
    }

    /** *************************************************************
     * Check if there was an error during execution.
     *
     * @return true if the result indicates an error
     */
    public boolean hasError() {
        return result != null && result.hasErrors();
    }

    /** *************************************************************
     * Get the SZS status from the last run.
     *
     * @return The SZSStatus, or SZSStatus.NOT_RUN if not run
     */
    public SZSStatus getSzsStatus() {
        return result != null ? result.getSzsStatus() : SZSStatus.NOT_RUN;
    }

    /** *************************************************************
     */
    private static String[] createCommandList(File executable, int timeout, File kbFile) {

        String opts = executable.toString() + " " + kbFile.toString() + " -t " + Integer.toString(timeout) + " -p";
        String[] optar = opts.split(" ");
        return optar;
    }

    /** *************************************************************
     * Add an assertion for inference.
     *
     * @param userAssertionTPTP asserted formula in the TPTP/TFF/THF syntax
     * @param kb Knowledge base
     * @param parsedFormulas a lit of parsed formulas in KIF syntax
     * @param tptp convert formula to TPTP if tptp = true
     * @return true if all assertions are added for inference
     */
    public static boolean assertFormula(String userAssertionTPTP, KB kb,
                                        List<Formula> parsedFormulas, boolean tptp) {

        if (debug) System.out.println("INFO in Leo.assertFormula(2):writing to file " + userAssertionTPTP);
        boolean allAdded = false;
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(userAssertionTPTP, true)))) {
            HashSet<Formula> processedFormulas = new HashSet();
            FormulaPreprocessor fp;
            Set<String> tptpFormulas;
            String tptpStr;
            for (Formula parsedF : parsedFormulas) {
                processedFormulas.clear();
                fp = new FormulaPreprocessor();
                processedFormulas.addAll(fp.preProcess(parsedF,false, kb));
                if (processedFormulas.isEmpty())
                    allAdded = false;
                else {   // 2. Translate to THF.
                    tptpFormulas = new HashSet<>();
                    if (tptp) {
                        for (Formula p : processedFormulas) {
                            // Use THFnew.processNonModal to translate each formula; we treat assertions as non-queries
                            String formula = THFnew.processNonModal(p, new HashMap<>(), false);
                            if (debug) System.out.println("INFO in LEO.assertFormula(2): formula " + formula);
                            tptpFormulas.add(formula);
                        }
                    }
                    // 3. Write to new tptp file
                    for (String theTPTPFormula : tptpFormulas) {
                        pw.print(SUMOformulaToTPTPformula.getLang() + "(kb_" + kb.name + "_UserAssertion" + "_" + axiomIndex++);
                        pw.println(",axiom,(" + theTPTPFormula + ")).");
                        tptpStr = SUMOformulaToTPTPformula.getLang() + "(kb_" + kb.name + "_UserAssertion" +
                                "_" + axiomIndex + ",axiom,(" + theTPTPFormula + ")).";
                        if (debug) System.out.println("INFO in LEO.assertFormula(2): TPTP for user assertion = " + tptpStr);
                    }
                    pw.flush();
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return allAdded;
    }

    /** *************************************************************
     * Creates a running instance of Leo.
     *
     * @param kbFile A File object denoting the initial knowledge base
     * to be loaded by the Leo executable.
     *
     * @throws ExecutableNotFoundException if the LEO-III executable is not found
     * @throws ProverCrashedException if LEO-III crashes with a non-zero exit code
     * @throws ProverTimeoutException if LEO-III times out
     * @throws Exception for other errors
     */
    private void run(File kbFile, int timeout) throws Exception {

        long startTime = System.currentTimeMillis();
        long timeoutMs = timeout * 1000L;

        // Initialize result structure
        result = new ATPResult.Builder()
                .engineName("LEO-III")
                .engineMode("default")
                .inputLanguage("THF")
                .inputSource(kbFile != null ? kbFile.getName() : "unknown")
                .timeoutMs(timeoutMs)
                .build();

        String leoex = KBmanager.getMgr().getPref("leoExecutable");
        if (StringUtil.emptyString(leoex)) {
            String msg = "Error in Leo.run(): no executable string in preferences";
            System.out.println(msg);
            result.setSzsStatus(SZSStatus.OS_ERROR);
            result.setPrimaryError(msg);
            throw new ExecutableNotFoundException("LEO-III", leoex, "leoExecutable");
        }
        File executable = new File(leoex);
        if (!executable.exists()) {
            String msg = "Error in Leo.run(): no executable " + leoex;
            System.err.println(msg);
            result.setSzsStatus(SZSStatus.OS_ERROR);
            result.setPrimaryError(msg);
            throw new ExecutableNotFoundException("LEO-III", leoex, "leoExecutable");
        }

        String[] cmds = createCommandList(executable, timeout, kbFile);
        result.setCommandLine(cmds);
        System.out.println("Leo.run(): Initializing Leo with:\n" + Arrays.toString(cmds));

        ProcessBuilder _builder = new ProcessBuilder(cmds);
        _builder.redirectErrorStream(false);  // Keep stderr separate

        Process _leo = _builder.start();

        // Read stdout and stderr in parallel
        List<String> stdoutLines = new ArrayList<>();
        List<String> stderrLines = new ArrayList<>();

        Thread stderrReader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(_leo.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    stderrLines.add(line);
                }
            } catch (IOException e) {
                // Ignore
            }
        });
        stderrReader.start();

        try (BufferedReader _reader = new BufferedReader(new InputStreamReader(_leo.getInputStream()))) {
            String line;
            while ((line = _reader.readLine()) != null) {
                stdoutLines.add(line);
                output.add(line);  // Maintain backward compatibility
            }
        }

        stderrReader.join(5000);

        int exitValue = _leo.waitFor();
        long elapsed = System.currentTimeMillis() - startTime;

        // Populate result
        result.setStdout(stdoutLines);
        result.setStderr(stderrLines);
        result.finalize(exitValue, elapsed, elapsed >= timeoutMs);

        if (exitValue != 0) {
            System.err.println("Leo.run(): Abnormal process termination (exit code " + exitValue + ")");
            if (!stderrLines.isEmpty()) {
                System.err.println("Stderr: " + stderrLines);
            }
            System.err.println(output);

            // Throw appropriate exception
            if (result.isTimedOut() || result.getSzsStatus() == SZSStatus.TIMEOUT) {
                throw new ProverTimeoutException("LEO-III", timeoutMs, elapsed, true, stdoutLines, stderrLines, result);
            } else if (exitValue > 128 && exitValue < 160) {
                throw new ProverCrashedException("LEO-III", exitValue, stdoutLines, stderrLines, result);
            }
        }
        System.out.println("Leo.run() done executing");
    }


    /** ***************************************************************
     * Write the statements to the temp-stmt.<type> file.
     * When sessionId is provided, writes to the session-specific directory,
     * creating it first if it does not yet exist.
     */
    public void writeStatements(Set<String> stmts, String type, String sessionId) {

        String dir;
        if (sessionId != null && !sessionId.isEmpty()) {
            java.nio.file.Path sessionDir = SessionTPTPManager.getSessionDir(sessionId);
            if (!java.nio.file.Files.exists(sessionDir)) {
                try {
                    java.nio.file.Files.createDirectories(sessionDir);
                    System.out.println("LEO.writeStatements(): created session dir " + sessionDir);
                }
                catch (IOException ex) {
                    System.err.println("Error in Leo.writeStatements(): could not create session dir " + sessionDir);
                    ex.printStackTrace();
                }
            }
            dir = sessionDir.toString();
        }
        else {
            dir = KBmanager.getMgr().getPref("kbDir");
        }
        String fname = "temp-stmt." + type;

        try (FileWriter fw = new FileWriter(dir + File.separator + fname); PrintWriter pw = new PrintWriter(fw)) {
            for (String s : stmts)
                pw.println(s);
        }
        catch (Exception e) {
            System.err.println("Error in Leo.writeStatements(): " + e.getMessage());
            System.err.println("Error writing file " + dir + File.separator + fname + "\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     * Read in two files and write their contents to a new file
     */
    public void catFiles(String f1, String f2, String fout) throws IOException {

        System.out.println("concatFiles(): " + f1 + " and " + f2 + " to " + fout);
        File f1file = new File(f1);
        File f2file = new File(f2);
        if (!f1file.exists())
            System.err.println("ERROR in concatFiles(): " + f1 + " does not exist");
        if (!f2file.exists())
            System.err.println("ERROR in concatFiles(): " + f2 + " does not exist");
        try (PrintWriter pw = new PrintWriter(fout);
             BufferedReader br = new BufferedReader(new FileReader(f1))) {
            String line = br.readLine();
            while (line != null) {
                pw.println(line);
                line = br.readLine();
            }

            try (BufferedReader bufr = new BufferedReader(new FileReader(f2))) {
                line = bufr.readLine();
                while (line != null) {
                    pw.println(line);
                    line = bufr.readLine();
                }
            }
        }
    }


    /** *************************************************************
     * Get user assertions with optional session isolation.
     *
     * @param kb The knowledge base
     * @param sessionId Optional HTTP session ID for session-specific UA files.
     *                  If null or empty, uses shared UA files.
     * @return List of user assertion THF formulas
     */
    public List<String> getUserAssertions(KB kb, String sessionId) {
        // thread lock safe
        return kb.withUserAssertionLock(() -> {
            String userAssertionTPTP = kb.name + KB._userAssertionsTHF;
            File dir;
            if (sessionId != null && !sessionId.isEmpty()) {
                dir = SessionTPTPManager.getSessionDir(sessionId).toFile();
            }
            else {
                dir = new File(KBmanager.getMgr().getPref("kbDir"));
            }
            String fname = dir + File.separator + userAssertionTPTP;
            File ufile = new File(fname);
            if (ufile.exists())
                return FileUtil.readLines(fname, false);
            else
                return new ArrayList<>();
        });
    }

    /** *************************************************************
     * Backward-compatible overload — delegates with null sessionId.
     * Session detection falls back to path extraction from kbFile.
     */
    public void run(KB kb, File kbFile, int timeout, Set<String> stmts) throws Exception {
        run(kb, kbFile, timeout, stmts, null);
    }

    /** *************************************************************
     * Creates a running instance of LEO-III adding a set of statements
     * in THF language to a file and then calling LEO.
     * Note that any query must be given as a "conjecture"
     *
     * @param stmts should be the query but the list gets expanded here with
     *              any other prior user assertions
     * @param sessionId explicit HTTP session ID for temp file isolation;
     *                  if null, falls back to extracting sessionId from kbFile path
     */
    public void run(KB kb, File kbFile, int timeout, Set<String> stmts, String sessionId) throws Exception {

        System.out.println("Leo.run(): query : " + stmts);
        String lang = "tff";
        if (SUMOKBtoTPTPKB.getLang().equals("fof"))
            lang = "tptp";

        // Use explicit sessionId if provided; otherwise try to extract from kbFile path
        if (sessionId == null || sessionId.isEmpty()) {
            String kbFilePath = kbFile.getAbsolutePath();
            if (kbFilePath.contains(File.separator + "sessions" + File.separator)) {
                String[] parts = kbFilePath.split(File.separator + "sessions" + File.separator);
                if (parts.length > 1) {
                    String remainder = parts[1];
                    int nextSep = remainder.indexOf(File.separator);
                    if (nextSep > 0) {
                        sessionId = remainder.substring(0, nextSep);
                    }
                }
            }
        }
        if (sessionId != null && !sessionId.isEmpty()) {
            System.out.println("INFO Leo.run(): using session dir for temp files, sessionId=" + sessionId);
        }

        // Use session dir for temp files when session-specific, otherwise shared kbDir
        String dir;
        if (sessionId != null && !sessionId.isEmpty()) {
            dir = SessionTPTPManager.getSessionDir(sessionId).toString() + File.separator;
        }
        else {
            dir = KBmanager.getMgr().getPref("kbDir") + File.separator;
        }
        String outfile = dir + "temp-comb." + lang;
        String stmtFile = dir + "temp-stmt." + lang;
        File fout = new File(outfile);
        if (fout.exists())
            fout.delete();
        File fstmt = new File(stmtFile);
        if (fstmt.exists())
            fstmt.delete();
        List<String> userAsserts = getUserAssertions(kb, sessionId);
        if (userAsserts != null && stmts != null)
            stmts.addAll(userAsserts);
        else {
            System.out.println("Error in Leo.run(): null query or user assertions set");
            return;
        }
        writeStatements(stmts, lang, sessionId);
        catFiles(kbFile.toString(),stmtFile,outfile);
        File comb = new File(outfile);
        run(comb,timeout);
    }

    /** *************************************************************
     */
    public static void main (String[] args) throws Exception {

        KBmanager.getMgr().initializeOnce();
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        KB kb = KBmanager.getMgr().getKB(kbName);
        String dir = KBmanager.getMgr().getPref("kbDir") + File.separator;
        String lang = "thf";
        String outfile = dir + "temp-comb." + lang;
        String stmtFile = dir + "temp-stmt." + lang;
        File f1 = new File(outfile);
        f1.delete();
        File f2 = new File(stmtFile);
        f2.delete();
        File f3 = new File(dir + kbName + KB._userAssertionsString);
        f3.delete();
        File f4 = new File(dir + kbName + KB._userAssertionsTPTP);
        f4.delete();
        File kbFile = new File(dir + kbName + "." + lang);
        System.out.println("Leo.main(): first test");
        HashSet<String> query = new HashSet<>();
        query.add("thf(conj1,conjecture,?[V__X:$i, V__Y:$i] : (subclass_THFTYPE_IiioI @ V__X @ V__Y)).");
        System.out.println("Leo.main(): calling Leo with: " + kbFile + ", 30, " + query);
        LEO leo = new LEO();
        leo.run(kb, kbFile, 30, query);
        System.out.println("----------------\nLeo output\n");
        for (String l : leo.output)
            System.out.println(l);
        String queryStr = "(subclass ?X ?Y)";
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        tpp.parseProofOutput(leo.output,queryStr,kb,leo.qlist);
        for (TPTPFormula step : tpp.proof) {
            System.out.println(":: " + step);
            Formula f = new Formula(step.sumo);
            System.out.println(f.format("","  ","\n"));
        }
        System.out.println("Leo.main(): bindings: " + tpp.bindings);
        //System.out.println("Leo.main(): proof: " + tpp.proof);
        System.out.println("-----------------\n");
        System.out.println("\n");
    }
}