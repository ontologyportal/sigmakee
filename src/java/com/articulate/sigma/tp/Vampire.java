/** This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also sigmakee.sourceforge.net
*/

package com.articulate.sigma.tp;

import com.articulate.sigma.*;
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.articulate.sigma.trans.SessionTPTPManager;
import com.articulate.sigma.trans.TPTP3ProofProcessor;
import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.StringUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Class for invoking the latest research version of Vampire from Java
 * A previous version invoked the KIF version of Vampire from Java
 * but that's 15 years old now.  The current Vampire does TPTP3 output
 * instead of XML.

 * @author Andrei Voronkov
 * @since 14/08/2003, Acapulco
 * @author apease
 */
public class Vampire {

    public StringBuilder qlist = null; // quantifier list in order for answer extraction
    public List<String> output = new ArrayList<>();
    public static int axiomIndex = 0;
    public enum ModeType {AVATAR, CASC, CUSTOM}; // Avatar is faster but doesn't provide answer variables.
                                                 // Custom takes value from env var
    public enum Logic { FOL, HOL }

    public Logic logic = Logic.FOL;
    public static ModeType mode = null;
    public static boolean debug = false;
    public static boolean askQuestion = true;

    // === NEW: Full result structure for error handling ===
    private ATPResult result;

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

        String space = Formula.SPACE;
        StringBuilder opts = new StringBuilder("--output_axiom_names").append(space).append("on").append(space);
        if (mode == ModeType.AVATAR) {
            opts.append("-av").append(space).append("on").append(space).append("-p").append(space).append("tptp").append(space);
            if (askQuestion) {
                opts.append("-qa").append(space).append("plain").append(space);
            }
            opts.append("-t").append(space);
        }
        if (mode == ModeType.CASC) {
            opts.append("--mode").append(space).append("casc").append(space); // NOTE: [--mode casc] is a shortcut for [--mode portfolio --schedule casc --proof tptp]
            if (askQuestion) {
                opts.append("-qa").append(space).append("plain").append(space);
            }
            opts.append("-t").append(space);
        }
        if (mode == ModeType.CUSTOM) {
            if (askQuestion) {
                opts.append("-qa").append(space).append("plain").append(space);
            }
            opts.append(System.getenv("VAMPIRE_OPTS")).append(space);
        }
        String[] optar = opts.toString().split(Formula.SPACE);
        String[] cmds = new String[optar.length + 3];
        cmds[0] = executable.toString();
        System.arraycopy(optar, 0, cmds, 1, optar.length);
        cmds[optar.length+1] = Integer.toString(timeout);
        cmds[optar.length+2] = kbFile.toString();
        return cmds;
    }

    /** *************************************************************
     * don't include a timeout if @param timeout is 0
     */
    private static String[] createCustomCommandList(File executable,
                                                    int timeout, File kbFile,
                                                    Collection<String> commands) {

        String space = Formula.SPACE;
        StringBuilder opts = new StringBuilder();

        if (mode == ModeType.AVATAR) {
            opts.append("-av").append(space).append("on").append(space).append("-p").append(space).append("tptp").append(space);
        } else if (mode == ModeType.CASC) {
            opts.append("--mode").append(space).append("casc").append(space); // NOTE: [--mode casc] is a shortcut for [--mode portfolio --schedule casc --proof tptp]
        } else if (mode == ModeType.CUSTOM) {
            opts.append(System.getenv("VAMPIRE_OPTS"));
        } else {
            System.err.println("Error in Vampire.createCustomCommandList(): no mode selected");
        }


        for (String s : commands)
            opts.append(s).append(space);
        if (timeout != 0) {
            opts.append("-t").append(space);
            opts.append(timeout).append(space);
        }

        opts.append(kbFile.toString());
        String[] optar = opts.toString().split(Formula.SPACE);
        String[] cmds = new String[optar.length + 1];
        cmds[0] = executable.toString();
        System.arraycopy(optar, 0, cmds, 1, optar.length);
        return cmds;
    }

    /** *************************************************************
     * Add an assertion for inference.
     *
     * @param userAssertionTPTP asserted formula in the TPTP/TFF syntax
     * @param kb Knowledge base
     * @param parsedFormulas a lit of parsed formulas in KIF syntax
     * @param tptp convert formula to TPTP if tptp = true
     * @return true if all assertions are added for inference
     *
     * TODO: This function might not be necessary if we find a way to
     * directly add assertion into opened inference engine (e_ltb_runner)
     */
    public static boolean assertFormula(String userAssertionTPTP, KB kb,
                                 List<Formula> parsedFormulas, boolean tptp) {

        if (debug) System.out.println("INFO in Vampire.assertFormula(2):writing to file " + userAssertionTPTP);
        boolean allAdded = false;
        Set<Formula> processedFormulas = new HashSet();
        FormulaPreprocessor fp = new FormulaPreprocessor();
        Set<String> tptpFormulas = new HashSet<>();
        String tptpStr;
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(userAssertionTPTP, true)))) {
            for (Formula parsedF : parsedFormulas) {
                processedFormulas.clear();
                processedFormulas.addAll(fp.preProcess(parsedF,false, kb));
                if (processedFormulas.isEmpty())
                    allAdded = false;
                else {   // 2. Translate to TPTP/TFF.
                    tptpFormulas.clear();
                    if (tptp) {
                        for (Formula p : processedFormulas) {
                            if (!p.isHigherOrder(kb)) {
                                tptpStr = SUMOformulaToTPTPformula.tptpParseSUOKIFString(p.getFormula(), false);
                                if (debug) System.out.println("INFO in Vampire.assertFormula(2): formula " + tptpStr);
                                tptpFormulas.add(tptpStr);
                            }
                        }
                    }
                    // 3. Write to new tptp file
                    for (String theTPTPFormula : tptpFormulas) {
                        pw.print(SUMOformulaToTPTPformula.getLang() + "(kb_" + kb.name + "_UserAssertion" + "_" + axiomIndex++);
                        pw.println(",axiom,(" + theTPTPFormula + ")).");
                        tptpStr = SUMOformulaToTPTPformula.getLang() + "(kb_" + kb.name + "_UserAssertion" +
                                "_" + axiomIndex + ",axiom,(" + theTPTPFormula + ")).";
                        if (debug) System.out.println("INFO in Vampire.assertFormula(2): TPTP for user assertion = " + tptpStr);
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
     * Creates a running instance of Vampire and collects its output
     *
     * @param kbFile A File object denoting the initial knowledge base
     * to be loaded by the Vampire executable.
     * @param timeout the time given for Vampire to finish execution
     *
     * @throws ExecutableNotFoundException if the Vampire executable is not found
     * @throws ProverCrashedException if Vampire crashes with a non-zero exit code
     * @throws ProverTimeoutException if Vampire times out
     * @throws Exception for other errors
     */
    public void run(File kbFile, int timeout) throws Exception {

        long startTime = System.currentTimeMillis();
        long timeoutMs = timeout * 1000L;

        // Initialize result structure
        result = new ATPResult.Builder()
                .engineName("Vampire")
                .engineMode(mode != null ? mode.name() : "CASC")
                .inputLanguage(SUMOKBtoTPTPKB.getLang().toUpperCase())
                .inputSource(kbFile != null ? kbFile.getName() : "unknown")
                .timeoutMs(timeoutMs)
                .build();

        String vampex = KBmanager.getMgr().getPref("vampire");
        if (StringUtil.emptyString(vampex)) {
            String msg = "Error in Vampire.run(): no executable string in preferences";
            System.err.println(msg);
            result.setSzsStatus(SZSStatus.OS_ERROR);
            result.setPrimaryError(msg);
            throw new ExecutableNotFoundException("Vampire", vampex, "vampire");
        }

        File executable = new File(vampex);
        if (!executable.exists()) {
            String msg = "Error in Vampire.run(): no executable " + vampex;
            System.err.println(msg);
            result.setSzsStatus(SZSStatus.OS_ERROR);
            result.setPrimaryError(msg);
            throw new ExecutableNotFoundException("Vampire", vampex, "vampire");
        }

        String[] cmds = createCommandList(executable, timeout, kbFile);
        result.setCommandLine(cmds);
        System.out.println("Vampire.run(): Initializing Vampire with:\n" + Arrays.toString(cmds));

        ProcessBuilder _builder = new ProcessBuilder(cmds);
        _builder.redirectErrorStream(false);  // Keep stderr separate for better error capture

        Process _vampire = _builder.start();

        // Read stdout and stderr in parallel to avoid deadlock
        List<String> stdoutLines = new ArrayList<>();
        List<String> stderrLines = new ArrayList<>();

        // Start stderr reader thread
        Thread stderrReader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(_vampire.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    stderrLines.add(line);
                }
            } catch (IOException e) {
                // Ignore - stream may close on process termination
            }
        });
        stderrReader.start();

        // Read stdout in main thread
        try (BufferedReader _reader = new BufferedReader(new InputStreamReader(_vampire.getInputStream()))) {
            String line;
            while ((line = _reader.readLine()) != null) {
                stdoutLines.add(line);
                output.add(line);  // Maintain backward compatibility
            }
        }

        // Wait for stderr thread to finish
        stderrReader.join(5000);

        int exitValue = _vampire.waitFor();
        long elapsed = System.currentTimeMillis() - startTime;

        // Populate result
        result.setStdout(stdoutLines);
        result.setStderr(stderrLines);
        result.finalize(exitValue, elapsed, elapsed >= timeoutMs);

        if (exitValue != 0) {
            System.err.println("Error in Vampire.runCustom(): Abnormal process termination (exit code " + exitValue + ")");
            if (!stderrLines.isEmpty()) {
                System.err.println("Stderr: " + stderrLines);
            }

            // Throw appropriate exception
            if (result.isTimedOut() || result.getSzsStatus() == SZSStatus.TIMEOUT) {
                throw new ProverTimeoutException("Vampire", timeoutMs, elapsed, false, stdoutLines, stderrLines, result);
            } else if (exitValue > 128 && exitValue < 160) {
                throw new ProverCrashedException("Vampire", exitValue, stdoutLines, stderrLines, result);
            } else if (result != null
                    && result.getStderr() != null
                    && !result.getStderr().isEmpty()
                    && result.getStderr().get(0) != null
                    && (result.getStderr().get(0).contains("% Exception at proof search level") || (result.getStderr().get(0).contains("Parser exception")))
                    && result.getStderr().size() > 1)
            {

                int lineNo = -1;

                // stderr[1] example: "Parsing Error on line 65289"
                if (result.getStderr().size() > 1 && result.getStderr().get(1) != null) {
                    Matcher m = Pattern.compile("Parsing Error on line\\s+(\\d+)").matcher(result.getStderr().get(1));
                    if (m.find()) {
                        lineNo = Integer.parseInt(m.group(1));
                    }
                }

                String msg = "Vampire: exception at proof search level"
                        + (lineNo > 0 ? " (Parsing Error on line " + lineNo + ")" : "");

                // Best: use an overload that accepts the line number (see below)
                throw new FormulaTranslationException(msg, result.getInputLanguage(), lineNo, stdoutLines, stderrLines);
            }
        }
        System.out.println("Vampire.run() done executing");
    }

    /** *************************************************************
     * Creates a running instance of Vampire with custom command line
     * options.
     *
     * @param kbFile A File object denoting the initial knowledge base
     * to be loaded by the Vampire executable.
     *
     * @throws ExecutableNotFoundException if the Vampire executable is not found
     * @throws ProverCrashedException if Vampire crashes with a non-zero exit code
     * @throws ProverTimeoutException if Vampire times out
     * @throws Exception for other errors
     */
    public void runCustom(File kbFile, int timeout, Collection<String> commands) throws Exception {

        long startTime = System.currentTimeMillis();
        long timeoutMs = timeout * 1000L;

        output = new ArrayList<>();

        // Determine which executable to use
        String vampex = "";
        String configKey = "vampire";
        if (logic == Logic.HOL) {
            vampex = KBmanager.getMgr().getPref("vampire_hol");
            configKey = "vampire_hol";
        } else {
            vampex = KBmanager.getMgr().getPref("vampire");
        }

        // Initialize result structure
        result = new ATPResult.Builder()
                .engineName("Vampire")
                .engineMode(logic == Logic.HOL ? "HOL" : (mode != null ? mode.name() : "CUSTOM"))
                .inputLanguage(logic == Logic.HOL ? "THF" : SUMOKBtoTPTPKB.getLang().toUpperCase())
                .inputSource(kbFile != null ? kbFile.getName() : "unknown")
                .timeoutMs(timeoutMs)
                .build();

        if (StringUtil.emptyString(vampex)) {
            String msg = "Error in Vampire.runCustom(): no executable string in preferences";
            System.err.println(msg);
            result.setSzsStatus(SZSStatus.OS_ERROR);
            result.setPrimaryError(msg);
            throw new ExecutableNotFoundException("Vampire", vampex, configKey);
        }
        File executable = new File(vampex);
        if (!executable.exists()) {
            String msg = "Error in Vampire.runCustom(): no executable " + vampex;
            System.err.println(msg);
            result.setSzsStatus(SZSStatus.OS_ERROR);
            result.setPrimaryError(msg);
            throw new ExecutableNotFoundException("Vampire", vampex, configKey);
        }
        System.out.println("Vampire.runCustom(): vampire executable: " + vampex);

        String[] cmds = createCustomCommandList(executable, timeout, kbFile.getAbsoluteFile(), commands);
        result.setCommandLine(cmds);
        System.out.println("Vampire.runCustom(): Custom command list:\n" + Arrays.toString(cmds));

        ProcessBuilder _builder = new ProcessBuilder(cmds);
        _builder.redirectErrorStream(false);  // Keep stderr separate

        if (kbFile != null && kbFile.getParentFile() != null) {
            _builder.directory(kbFile.getParentFile());
            System.out.println("Vampire CWD: " + _builder.directory().getAbsolutePath());
        }

        Process _vampire = _builder.start();

        // Read stdout and stderr in parallel
        List<String> stdoutLines = new ArrayList<>();
        List<String> stderrLines = new ArrayList<>();

        Thread stderrReader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(_vampire.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    stderrLines.add(line);
                }
            } catch (IOException e) {
                // Ignore
            }
        });
        stderrReader.start();

        try (BufferedReader _reader = new BufferedReader(new InputStreamReader(_vampire.getInputStream()))) {
            String line;
            while ((line = _reader.readLine()) != null) {
                stdoutLines.add(line);
                output.add(line);  // Maintain backward compatibility
            }
        }

        stderrReader.join(5000);

        int exitValue = _vampire.waitFor();
        long elapsed = System.currentTimeMillis() - startTime;

        // Populate result
        result.setStdout(stdoutLines);
        result.setStderr(stderrLines);
        result.finalize(exitValue, elapsed, elapsed >= timeoutMs);

        if (exitValue != 0) {
            System.err.println("Error in Vampire.runCustom(): Abnormal process termination (exit code " + exitValue + ")");
            if (!stderrLines.isEmpty()) {
                System.err.println("Stderr: " + stderrLines);
            }

            // Throw appropriate exception
            if (result.isTimedOut() || result.getSzsStatus() == SZSStatus.TIMEOUT) {
                throw new ProverTimeoutException("Vampire", timeoutMs, elapsed, false, stdoutLines, stderrLines, result);
            } else if (exitValue > 128 && exitValue < 160) {
                throw new ProverCrashedException("Vampire", exitValue, stdoutLines, stderrLines, result);
            } else if (result != null
                    && result.getStderr() != null
                    && !result.getStderr().isEmpty()
                    && result.getStderr().get(0) != null
                    && (result.getStderr().get(0).contains("% Exception at proof search level") || (result.getStderr().get(0).contains("Parser exception")))
                    && result.getStderr().size() > 1)
            {

                int lineNo = -1;

                // stderr[1] example: "Parsing Error on line 65289"
                if (result.getStderr().size() > 1 && result.getStderr().get(1) != null) {
                    Matcher m = Pattern.compile("Parsing Error on line\\s+(\\d+)").matcher(result.getStderr().get(1));
                    if (m.find()) {
                        lineNo = Integer.parseInt(m.group(1));
                    }
                }

                String msg = "Vampire: exception at proof search level"
                        + (lineNo > 0 ? " (Parsing Error on line " + lineNo + ")" : "");

                // Best: use an overload that accepts the line number (see below)
                throw new FormulaTranslationException(msg, result.getInputLanguage(), lineNo, stdoutLines, stderrLines);
            }
        }
        System.out.println("Vampire.runCustom() done executing");
    }

    /** ***************************************************************
     * Write all the strings in @param stmts to temp-stmt.[tptp|tff|thf]
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
                    System.out.println("Vampire.writeStatements(): created session dir " + sessionDir);
                }
                catch (IOException ex) {
                    System.err.println("Error in writeStatements(): could not create session dir " + sessionDir);
                    ex.printStackTrace();
                }
            }
            dir = sessionDir.toString();
        }
        else {
            dir = KBmanager.getMgr().getPref("kbDir");
        }
        String fname = "temp-stmt." + type;

        try (FileWriter fw = new FileWriter(dir + File.separator + fname);
            PrintWriter pw = new PrintWriter(fw)) {
            for (String s : stmts)
                pw.println(s);
        }
        catch (IOException e) {
            System.err.println("Error in writeStatements(): " + e.getMessage());
            System.err.println("Error writing file " + dir + File.separator + fname + "\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     * Read in two files and write their contents to a new file
     */
    public void concatFiles(String f1, String f2, String fout) throws IOException {

        if (debug) System.out.println("concatFiles(): " + f1 + " and " + f2 + " to " + fout);
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
     * @return List of user assertion TPTP formulas
     */
    public List<String> getUserAssertions(KB kb, String sessionId) {

        // Thread safe
        return kb.withUserAssertionLock(() -> {
            String userAssertionTPTP = kb.name + KB._userAssertionsTPTP;
            if (SUMOKBtoTPTPKB.getLang().equals("tff"))
                userAssertionTPTP = kb.name + KB._userAssertionsTFF;

            // Determine directory based on sessionId
            File dir;
            if (sessionId != null && !sessionId.isEmpty()) {
                // Use session-specific directory
                java.nio.file.Path sessionDir = com.articulate.sigma.trans.SessionTPTPManager.getSessionDir(sessionId);
                dir = sessionDir.toFile();
            } else {
                // Use shared directory (original behavior)
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
     * Creates a running instance of Vampire adding a set of statements
     * in TFF or TPTP language to a file and then calling Vampire.
     * Note that any query must be given as a "conjecture"
     *
     * @param kb the current knowledge base
     * @param kbFile the current knowledge base TPTP file
     * @param timeout the timeout given to Vampire to find a proof
     * @param stmts a Set of user assertions
     * @param sessionId explicit HTTP session ID for temp file isolation;
     *                  if null, falls back to extracting sessionId from kbFile path
     *
     * @throws Exception of something goes south
     */
    public void run(KB kb, File kbFile, int timeout, Set<String> stmts, String sessionId) throws Exception {

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
            System.out.println("INFO Vampire.run(): using session dir for temp files, sessionId=" + sessionId);
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

        // Load UA files from session directory if session-specific, otherwise from shared directory
        List<String> userAsserts = getUserAssertions(kb, sessionId);
        if (userAsserts != null && stmts != null)
            stmts.addAll(userAsserts);
        else {
            System.err.println("Error in Vampire.run(): null query or user assertions set");
            return;
        }
        writeStatements(stmts, lang, sessionId);
        concatFiles(kbFile.toString(),stmtFile,outfile);
        File comb = new File(outfile);
        run(comb,timeout);
    }

    /** ***************************************************************
     */
    public static void printHelp() {

        System.out.println();
        System.out.println("Vampire class");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -p - run a test and process the proof");
        System.out.println("  -t - execute a test");
        System.out.println();
    }

    /** *************************************************************
     */
    public static void main (String[] args) throws Exception {

        System.out.println("INFO in Vampire.main()");
        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        if (argMap.isEmpty() || argMap.containsKey("h")) {
            printHelp();
            return;
        }
        KBmanager.getMgr().initializeOnce();
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        KB kb = KBmanager.getMgr().getKB(kbName);
        String dir = KBmanager.getMgr().getPref("kbDir") + File.separator;
        String lang = "tff";
        if (SUMOKBtoTPTPKB.getLang().equals("fof"))
            lang = "tptp";
        File kbFile = new File(dir + kbName + "." + lang);
        if (!kbFile.exists()) {
            System.err.println("Error in Vampire.main(): no KB file: " + kbFile);
            return;
        }

        Vampire vampire = new Vampire();
        Vampire.mode = Vampire.ModeType.CASC; // default
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();

        if (argMap.containsKey("t")) {
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

            System.out.println("Vampire.main(): first test");
            Set<String> query = new HashSet<>();
            query.add("tff(conj1,conjecture,?[V__X, V__Y] : (s__subclass(V__X,V__Y))).");
            System.out.println("Vampire.main(): calling Vampire with: " + kbFile + ", 30, " + query);
            vampire.run(kb, kbFile, 30, query);
            System.out.println("----------------\nVampire output\n");
            for (String l : vampire.output)
                System.out.println(l);
            String queryStr = "(subclass ?X ?Y)";
            tpp.parseProofOutput(vampire.output,queryStr,kb,vampire.qlist);
            System.out.println("Vampire.main(): bindings: " + tpp.bindings);
            System.out.println("Vampire.main(): proof: " + tpp.proof);
            System.out.println("-----------------\n");
            System.out.println();

            System.out.println("Vampire.main(): second test");
            System.out.println(kb.askVampire("(subclass ?X Entity)",30,1));
        }
        if (argMap.containsKey("p")) {
            vampire.run(kbFile, 60);

            String query = "(maximumPayloadCapacity ?X (MeasureFn ?Y ?Z))";
            StringBuilder answerVars = new StringBuilder("?X ?Y ?Z");
            System.out.println("input: " + vampire.output + "\n");
            tpp.parseProofOutput(vampire.output, query, kb, answerVars);
            tpp.createProofDotGraph();

            System.out.println("Vampire.main(): " + tpp.proof.size() + " steps ");
            System.out.println("Vampire.main() bindings: " + tpp.bindingMap);
            System.out.println("Vampire.main() skolems: " + tpp.skolemTypes);
            System.out.println("Vampire.main() proof[3]: {");
            tpp.printProof(3);
            System.out.println("}");
        }
    }
}
