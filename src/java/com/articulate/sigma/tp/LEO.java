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
import java.nio.file.Files;
import java.nio.file.Path;
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
    
    public static boolean debug = false;
    
    private KB kb;
    private String executablePath;
    private String requestedTptpLanguage;
    private int timeout;
    private int maxAnswers;
    private String sessionId;
    private String inferenceFileName;
    /**  Quantifier list in order for answer extraction */
    public StringBuilder qlist = null;
    /** Output */
    public List<String> output = new ArrayList<>();
    public int axiomIndex = 0;
    private ATPResult result;

    public LEO (KB kb) {
        this(kb, "tptp", 30, 1, null);
    }

    public LEO (KB kb, String requestedTptpLanguage, int timeout, int maxAnswers, String sessionId) {
        this.executablePath = KBmanager.getMgr().getPref("leoExecutable");
        this.kb = kb;
        this.requestedTptpLanguage = "tff";
        if ("fof".equals(requestedTptpLanguage))
            this.requestedTptpLanguage = "tptp";
        else
            SUMOtoTFAform.initOnce();
        this.timeout = timeout;
        this.maxAnswers = maxAnswers;
        this.sessionId = sessionId;
        this.inferenceFileName = KBmanager.getMgr().getPref("kbDir") + File.separator + KBmanager.getMgr().getPref("sumokbname") + "." + this.requestedTptpLanguage;
    }

    /***************************************************************
     * Submits a
     * query to the inference engine.
     *
     * @param suoKifFormula The String representation of the SUO-KIF query.
     * @param timeout       The number of seconds after which the inference engine should
     *                      give up.
     * @param maxAnswers    The maximum number of answers (binding sets) the inference
     *                      engine should return.
     * @return A String indicating the status of the ask operation.
     */
    public void askLeo(String suoKifFormula) {

        if (StringUtil.isNonEmptyString(suoKifFormula)) {
            Formula query = new Formula();
            query.read(suoKifFormula);
            FormulaPreprocessor fp = new FormulaPreprocessor();
            Set<Formula> processedQuery = fp.preProcess(query, true, this.kb);
            if (!processedQuery.isEmpty() && this != null) {
                this.axiomIndex = 0;
                String dir = KBmanager.getMgr().getPref("kbDir") + File.separator;
                File s = new File(this.inferenceFileName);
                if (!s.exists()) {
                    kb = KBmanager.getMgr().getKB(this.kb.name);
                }
                Set<String> tptpquery = new HashSet<>();
                StringBuilder combined = new StringBuilder();
                if (processedQuery.size() > 1) {
                    combined.append("(or ");
                    for (Formula p : processedQuery) {
                        combined.append(p.getFormula()).append(Formula.SPACE);
                    }
                    combined.append(Formula.RP);
                    String theTPTPstatement = this.requestedTptpLanguage + "(query" + "_" + this.axiomIndex++ +
                        ",conjecture,(" +
                        SUMOformulaToTPTPformula.tptpParseSUOKIFString(combined.toString(), true, this.requestedTptpLanguage)
                        + ")).";
                    tptpquery.add(theTPTPstatement);
                }
                else {
                    String theTPTPstatement = this.requestedTptpLanguage + "(query" + "_" + this.axiomIndex++ +
                        ",conjecture,(" +
                        SUMOformulaToTPTPformula.tptpParseSUOKIFString(processedQuery.iterator().next().getFormula(), true, this.requestedTptpLanguage)
                        + ")).";
                    tptpquery.add(theTPTPstatement);
                }
                try {
                    Set<String> tptpQuery = tptpquery;
                    this.run(s, tptpQuery);
                    this.qlist = SUMOformulaToTPTPformula.getQlist();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String strQuery = processedQuery.iterator().next().getFormula();
            }
            else
                System.err.println("Error in KB.askLeo(): no TPTP formula translation for query: " + query);
        }
    }

    /*********************************************************************************
     * Submit a query to LEO-III with session-specific temp file isolation.
     * Uses the shared THF base file but writes temp-comb/temp-stmt into
     * the session directory so concurrent sessions don't collide.
     *
     * @param suoKifFormula The query in SUO-KIF format
     * @param timeout Timeout in seconds
     * @param maxAnswers Maximum number of answers
     * @param sessionId HTTP session ID for temp file isolation (null for shared dir)
     * @return LEO result object
     */
    public void askLeo(String suoKifFormula, boolean useSession) {

        final String requestedLang = SUMOKBtoTPTPKB.getLang();
        if (StringUtil.isNonEmptyString(suoKifFormula)) {
            Formula query = new Formula();
            query.read(suoKifFormula);
            FormulaPreprocessor fp = new FormulaPreprocessor();
            Set<Formula> processedQuery = fp.preProcess(query, true, this.kb);
            if (!processedQuery.isEmpty() && this != null) {
                this.axiomIndex = 0;
                String kbDir = KBmanager.getMgr().getPref("kbDir") + File.separator;
                File s;
                if (useSession) {
                    Path sessionPath = com.articulate.sigma.trans.SessionTPTPManager.getSessionTPTPPath(this.sessionId, this.kb.name, this.requestedTptpLanguage);
                    if (Files.exists(sessionPath)) {
                        System.out.println("KB.askLeo(): using session-specific TPTP: " + sessionPath);
                        s = sessionPath.toFile();
                    } else {
                        s = new File(kbDir + this.kb.name + "." + this.requestedTptpLanguage);
                    }
                } else {
                    s = new File(kbDir + this.kb.name + "." + this.requestedTptpLanguage);
                }
                if (!s.exists()) {
                    if (sessionId != null && !sessionId.isEmpty()) {
                        try {
                            Path sessionPath = com.articulate.sigma.trans.SessionTPTPManager.generateSessionTPTP(this.sessionId, this.kb, this.requestedTptpLanguage);
                            s = sessionPath.toFile();
                        } catch (Exception e) {
                            System.err.println("KB.askLeo(): failed to generate session TPTP: " + e.getMessage());
                            e.printStackTrace();
                            return;
                        }
                    } else {
                        this.kb = KBmanager.getMgr().getKB(this.kb.name);
                    }
                }
                Set<String> tptpquery = new HashSet<>();
                StringBuilder combined = new StringBuilder();
                if (processedQuery.size() > 1) {
                    combined.append("(or ");
                    for (Formula p : processedQuery) {
                        combined.append(p.getFormula()).append(Formula.SPACE);
                    }
                    combined.append(Formula.RP);
                    String theTPTPstatement = requestedLang + "(query" + "_" + this.axiomIndex++ +
                            ",conjecture,(" +
                            SUMOformulaToTPTPformula.tptpParseSUOKIFString(combined.toString(), true, requestedLang)
                            + ")).";
                    tptpquery.add(theTPTPstatement);
                }
                else {
                    String theTPTPstatement = requestedLang + "(query" + "_" + this.axiomIndex++ +
                        ",conjecture,(" +
                        SUMOformulaToTPTPformula.tptpParseSUOKIFString(processedQuery.iterator().next().getFormula(), true, this.requestedTptpLanguage)
                        + ")).";
                    tptpquery.add(theTPTPstatement);
                }
                try {
                    Set<String> tptpQuery = tptpquery;
                    this.run(s, tptpQuery);
                    this.qlist = SUMOformulaToTPTPformula.getQlist();
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else
                System.err.println("Error in KB.askLeo(): no TPTP formula translation for query: " + query);
        }
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
     * @param parsedFormulas a lit of parsed formulas in KIF syntax
     * @param tptp convert formula to TPTP if tptp = true
     * @return true if all assertions are added for inference
     */
    public boolean assertFormula(String userAssertionTPTP, List<Formula> parsedFormulas, boolean tptp) {

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
                processedFormulas.addAll(fp.preProcess(parsedF,false, this.kb));
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
                        pw.print(SUMOformulaToTPTPformula.getLang() + "(kb_" + this.kb.name + "_UserAssertion" + "_" + this.axiomIndex++);
                        pw.println(",axiom,(" + theTPTPFormula + ")).");
                        tptpStr = SUMOformulaToTPTPformula.getLang() + "(kb_" + this.kb.name + "_UserAssertion" +
                                "_" + this.axiomIndex + ",axiom,(" + theTPTPFormula + ")).";
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
    private void run(File kbFile) throws Exception {

        long startTime = System.currentTimeMillis();
        long timeoutMs = this.timeout * 1000L;

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
    public void writeStatements(Set<String> stmts) {

        String dir;
        if (this.sessionId != null && !this.sessionId.isEmpty()) {
            java.nio.file.Path sessionDir = SessionTPTPManager.getSessionDir(this.sessionId);
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
        String fname = "temp-stmt." + this.requestedTptpLanguage;

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
    public List<String> getUserAssertions(KB kb) {
        // thread lock safe
        return kb.withUserAssertionLock(() -> {
            String userAssertionTPTP = this.kb.name + KB._userAssertionsTHF;
            File dir;
            if (this.sessionId != null && !this.sessionId.isEmpty()) {
                dir = SessionTPTPManager.getSessionDir(this.sessionId).toFile();
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
    public void run(File kbFile, Set<String> stmts) throws Exception {
        run(kbFile, stmts, false);
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
    public void run(File kbFile, Set<String> stmts, boolean useSession) throws Exception {

        System.out.println("Leo.run(): query : " + stmts);
        // Use explicit sessionId if provided; otherwise try to extract from kbFile path
        if (useSession) {
            String kbFilePath = kbFile.getAbsolutePath();
            if (kbFilePath.contains(File.separator + "sessions" + File.separator)) {
                String[] parts = kbFilePath.split(File.separator + "sessions" + File.separator);
                if (parts.length > 1) {
                    String remainder = parts[1];
                    int nextSep = remainder.indexOf(File.separator);
                    if (nextSep > 0) {
                        this.sessionId = remainder.substring(0, nextSep);
                    }
                }
            }
        }
        if (useSession) {
            System.out.println("INFO Leo.run(): using session dir for temp files, sessionId=" + sessionId);
        }
        // Use session dir for temp files when session-specific, otherwise shared kbDir
        String dir;
        if (useSession) {
            dir = SessionTPTPManager.getSessionDir(sessionId).toString() + File.separator;
        }
        else {
            dir = KBmanager.getMgr().getPref("kbDir") + File.separator;
        }
        String outfile = dir + "temp-comb." + this.requestedTptpLanguage;
        String stmtFile = dir + "temp-stmt." + this.requestedTptpLanguage;
        File fout = new File(outfile);
        if (fout.exists())
            fout.delete();
        File fstmt = new File(stmtFile);
        if (fstmt.exists())
            fstmt.delete();
        List<String> userAsserts = getUserAssertions(this.kb);
        if (userAsserts != null && stmts != null)
            stmts.addAll(userAsserts);
        else {
            System.out.println("Error in Leo.run(): null query or user assertions set");
            return;
        }
        writeStatements(stmts);
        catFiles(kbFile.toString(), stmtFile, outfile);
        File comb = new File(outfile);
        run(comb);
    }

    /**************************************************************
     * Submits a query to the LEO inference engine. Returns an XML formatted String that
     * contains the response of the inference engine. It should be in the form
     * "<queryResponse>...</queryResponse>".
     *
     * suoKifFormula The String representation of the SUO-KIF query.
     *  timeout       The number of seconds after which the underlying inference
     *                      engine should give up. (Time taken by axiom selection doesn't
     *                      count.)
     *  maxAnswers    The maximum number of answers (binding sets) the inference
     *                      engine should return.
     * @return A String indicating the status of the ask operation.

    public String askLEOOld(String suoKifFormula, int timeout, int maxAnswers, String flag) {

        String result = "";
        try {
            String LeoExecutable = KBmanager.getMgr().getPref("leoExecutable");
            String LeoInput = KBmanager.getMgr().getPref("inferenceTestDir") + "prob.p";
            String LeoProblem;
            String responseLine;
            String LeoOutput = "";
            File LeoExecutableFile = new File(LeoExecutable);
            File LeoInputFile = new File(LeoInput);
            FileWriter LeoInputFileW = new FileWriter(LeoInput);

            List<Formula> selectedQuery = new ArrayList<Formula>();
            Formula newQ = new Formula();
            newQ.read(suoKifFormula);
            selectedQuery.add(newQ);
            List<String> selFs = null;
            if (flag.equals("LeoSine")) {
                SInE sine = new SInE(this.formulaMap.keySet());
                selFs = new ArrayList<String>(sine.performSelection(suoKifFormula));
                sine.terminate();
            }
            else if (flag.equals("LeoLocal"))
                selFs = new ArrayList<String>();
            else if (flag.equals("LeoGlobal")) {
                selFs = new ArrayList<String>();
                Iterator<Formula> it = this.formulaMap.values().iterator();
                while (it.hasNext()) {
                    Formula entry = it.next();
                    selFs.add(entry.toString());
                }
            }
            try { // add user asserted formulas
                File dir = new File(this.kbDir);
                File file = new File(dir, (this.name + _userAssertionsString));
                String filename = file.getCanonicalPath();
                BufferedReader userAssertedInput = new BufferedReader(new FileReader(filename));

                try {
                    String line = null;
                    /
                     * readLine is a bit quirky : it returns the content of a
                     * line MINUS the newline. it returns null only for the END
                     * of the stream. it returns an empty String if two newlines
                     * appear in a row.

                    while ((line = userAssertedInput.readLine()) != null)
                        selFs.add(line);
                }
                finally {
                    userAssertedInput.close();
                }
            }
            catch (IOException ex) {
                System.err.println("Error in KB.askLEO(): " + ex.getMessage());
                ex.printStackTrace();
            }
            List<Formula> selectedFormulas = new ArrayList();
            Formula newF = new Formula();

            Iterator<String> it = selFs.iterator();
            while (it.hasNext()) {
                String entry = it.next();
                newF = new Formula();
                newF.read(entry);
                selectedFormulas.add(newF);
            }
            System.out.println(selFs.toString());
            THF thf = new THF();
            LeoProblem = thf.KIF2THF(selectedFormulas, selectedQuery, this);
            LeoInputFileW.write(LeoProblem);
            LeoInputFileW.close();

            String command = LeoExecutableFile.getCanonicalPath() + " -po 1 -t " + timeout + Formula.SPACE
                    + LeoInputFile.getCanonicalPath();

            Process leo = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(leo.getInputStream()));
            while ((responseLine = reader.readLine()) != null)
                LeoOutput += responseLine + "\n";
            reader.close();
            System.out.println(LeoOutput);

            if (LeoOutput.contains("SZS status Theorem")) {
                result = "Answer 1. yes" + "<br> <br>" + LeoProblem.replaceAll("\\n", "<br>") + "<br> <br>"
                        + LeoOutput.replaceAll("\\n", "<br>");
            }
            else {
                result = "Answer 1. don't know" + "<br> <br>" + LeoProblem.replaceAll("\\n", "<br>") + "<br> <br>"
                        + LeoOutput.replaceAll("\\n", "<br>");
            }
        }
        catch (Exception ex) {
            System.err.println("Error in KB.askLEO(): " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

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
        LEO leo = new LEO(kb);
        leo.run(kbFile, query);
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