/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also https://github.com/ontologyportal
*/

/** A framework for doing a series of assertions and queries, and for comparing
 *  the actual result of queries against an expected result.  Also will
 *  keep track of the time needed for each query.  Tests are expected in files
 *  with a .tq extension contained in a directory specified by the
 *  "inferenceTestDir" parameter, and results are provided in the same directory
 *  with a .res extension.
 *
 *  The test files contain legal KIF expressions including several kinds of
 *  meta-information.  Meta-predicates include (note <String>), (query <Formula>),
 *  and (answer <term1>..<termn>).  There may be only one note and query statements,
 *  but there may be several answer statements, if multiple binding sets are
 *  expected.
 *
 *  Comments are allowed in test files, and are signified by a ';', after which
 *  all content on the line is ignored.
 *
 *  Note that since answers are provided in an ordered list, without reference
 *  to their respective variable names, that the inference engine is assumed to
 *  return bindings in the same order.
 *
 *  All test file statements must be valid SUO-KIF.  Allowable meta-predicates
 *  are: note, time, query, answer.  All other predicates are assumed to be
 *  SUO-KIF expressions.  'answer' may take multiple SUO-KIF statements where there
 *  could be more than one valid answer.
 */

package com.articulate.sigma;

import com.articulate.sigma.tp.EProver;
import com.articulate.sigma.tp.LEO;
import com.articulate.sigma.tp.Vampire;
import com.articulate.sigma.tp.ATPQuery;
import com.articulate.sigma.tp.TheoremProverController;
import com.articulate.sigma.tp.ATPResult;
import com.articulate.sigma.trans.*;
import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.utils.LoggingUtils;
import com.articulate.sigma.parsing.CLIMapParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class InferenceTestSuite {

    public static boolean debug = false;

    /** Default timeout for queries with unspecified timeouts or override when selected */
    public static int DEFAULT_TIMEOUT = 30;

    public static boolean OVERRIDE_TIMEOUT = false;

    /** Save TPTP translations of each problem as <probName>.p */
    public static boolean saveTPTP =  true;

    public static Set<String> metaPred = new HashSet(Arrays.asList("note", "time", "query", "answer", "file", "regen"));

    private KB kb = null;

    private String inferenceTestDir;

    private TheoremProverController theoremProverController = new TheoremProverController();

    private List<String> inferenceTestPaths = new ArrayList<>();

    public static boolean KEEP_FAILED_SESSION_DIRS = Boolean.parseBoolean(System.getProperty("sigma.keepFailedSessions", "false"));

    public static class OneResult {
        public boolean pass;
        public long millis;
        public String html;
        public java.util.List<String> expected;
        public java.util.List<String> actual;
        public List<String> proofText;
    }

    /** ***************************************************************
     */
    public class InfTestData {

        public String filename = "";
        public String note = "";
        public String query = "";
        public Set<String> kbFiles = new HashSet<>();
        public List<String> expectedAnswers = new ArrayList<>();
        public List<String> actualAnswers = new ArrayList<>();
        public int timeout = 30;
        public List<String> files = new ArrayList<>();
        public List<String> statements = new ArrayList<>();
        public boolean regen = false;
        public boolean inconsistent = false;
        public boolean success = false;
        public float execTime = 0;
        public String SZSstatus = "";
        public List<String> proof = new ArrayList<>();
    }

    public InferenceTestSuite(KB kb) {
        this.kb = kb;
        this.inferenceTestDir = KBmanager.getMgr().getPref("inferenceTestDir");
        this.inferenceTestPaths = loadInferenceTestPaths();
    }

    private List<String> getInferenceTestPaths() {return this.inferenceTestPaths;}

    private List<String> loadInferenceTestPaths() {

        File dir = new File(this.inferenceTestDir);
        File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".tq"));
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        List<String> inferenceTestPaths = new ArrayList<>();
        for (int i = 0; i < files.length; i++) inferenceTestPaths.add(files[i].getName());
        return inferenceTestPaths;
    }

    private InfTestData runTest(String testFilePath, String proverType, String language, String vampireMode,
                                boolean closedWorldAssumption, boolean modusPonens,
                                boolean dropOnePremise, boolean holUseModals, int timeout) {

        System.out.println("===============================================================================================================================================");
        LoggingUtils.log("Running " + testFilePath + " with " + proverType + " in " + language + " modusPonens=" + modusPonens);

        File testFile = new File(testFilePath);
        InfTestData itd = readTestFile(testFile);
        LoggingUtils.log("TQ regen=" + itd.regen + " for " + testFilePath);
        if (OVERRIDE_TIMEOUT) {
            itd.timeout = timeout > 0 ? timeout : DEFAULT_TIMEOUT;
        }
        else {
            itd.timeout = itd.timeout > 0 ? itd.timeout : (timeout > 0 ? timeout : DEFAULT_TIMEOUT);
        }

        compareFiles(itd);

        String sessionId = "tq-" + UUID.randomUUID();

        // Keep the session directory unless we prove the test completed successfully.
        boolean cleanupSessionDir = false;

        try {
            if (itd.regen) {
                SessionTPTPManager.beginBatchTells(sessionId);
            }
            try {
                for (String statement : itd.statements) {
                    if (!StringUtil.emptyString(statement)) {
                        this.kb.tell(statement, sessionId);
                    }
                }
            }
            finally {
                if (itd.regen) {
                    SessionTPTPManager.endBatchTells(sessionId);
                }
            }
            if (itd.regen) {
                String fileLang = sessionFileLang(language);
                LoggingUtils.log("Full session TPTP regeneration requested by "
                        + testFilePath + " for session " + sessionId);
                SessionTPTPManager.generateSessionTPTP(sessionId, this.kb, fileLang);
            }

            ATPQuery atpQuery = new ATPQuery(
                    this.kb,
                    sessionId,
                    itd.query,
                    null,
                    "CUSTOM",
                    proverType,
                    language,
                    vampireMode,
                    closedWorldAssumption,
                    modusPonens,
                    dropOnePremise,
                    holUseModals,
                    itd.timeout,
                    itd.expectedAnswers.size()
            );

            ATPResult atpResult = this.theoremProverController.ask(atpQuery);
            itd.execTime = atpResult.getElapsedTimeMs();

            if (atpResult.getSzsStatus() != null)
                itd.SZSstatus = atpResult.getSzsStatus().getTptpName();
            else if (atpResult.getSzsStatusRaw() != null)
                itd.SZSstatus = atpResult.getSzsStatusRaw();
            else
                itd.SZSstatus = "Unknown";

            itd.proof = new ArrayList<>();

            if (atpResult.getStdout() != null)
                itd.proof.addAll(atpResult.getStdout());

            if (atpResult.getStderr() != null && !atpResult.getStderr().isEmpty()) {
                itd.proof.add("---- STDERR ----");
                itd.proof.addAll(atpResult.getStderr());
            }

            if (atpResult.getPrimaryError() != null && !atpResult.getPrimaryError().isEmpty()) {
                itd.proof.add("---- PRIMARY ERROR ----");
                itd.proof.add(atpResult.getPrimaryError());
            }

            TPTP3ProofProcessor tpp = atpResult.getParsedProofProcessor(this.kb, itd.query);

            if (tpp.status != null)
                itd.SZSstatus = tpp.status;

            itd.actualAnswers = new ArrayList<>();

            if (tpp.bindings != null)
                itd.actualAnswers.addAll(tpp.bindings);

            if (tpp.status != null && tpp.status.startsWith("Theorem") && itd.actualAnswers.isEmpty())
                itd.actualAnswers.add("yes");

            if (tpp.inconsistency) {
                itd.inconsistent = true;
                itd.actualAnswers = new ArrayList<>();
            }

            boolean different = true;
            if (tpp.proof != null && (tpp.status == null || !tpp.status.startsWith("Timeout")))
                different = !sameAnswers(tpp, itd.expectedAnswers);

            itd.success = !(different || tpp.noConjecture);

            // Only delete the session directory if the test truly passed.
            cleanupSessionDir = itd.success && !itd.inconsistent;

            return itd;
        }
        finally {
            try {
                resetAllForInference(this.kb, sessionId);

                if (cleanupSessionDir) {
                    SessionTPTPManager.cleanupSession(sessionId);
                }
                else {
                    System.err.println("Preserving failed session directory for debugging:");
                    System.err.println("  sessionId: " + sessionId);
                    System.err.println("  dir: " + SessionTPTPManager.getSessionDir(sessionId));
                    System.err.println("  temp-comb: " + SessionTPTPManager.getSessionDir(sessionId).resolve("temp-comb.tptp"));
                }
            }
            catch (Exception e) {
                System.err.println("Warning: could not reset KB after test: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /** ***************************************************************
     * Thin wrapper for the JSP buttons: returns PASS/FAIL + time +
     * a tiny HTML summary.
     */
    public OneResult runOne(final KB kb, final String engine, final int timeoutSec, final String tqPath, final boolean modusPonens) {

        long t0 = System.currentTimeMillis();
        try {
            InfTestData itd = runTest(
                tqPath,
                engine,
                "FOF",
                "CASC",
                false,
                modusPonens,
                false,
                false,
                timeoutSec
            );
            if (itd.proof.isEmpty()){
                OneResult err = new OneResult();
                err.pass = false;
                err.millis = System.currentTimeMillis() - t0;
                err.html = "<div style='color:#b00'><b>ERROR:</b>An error occurred during the execution</div>";
                return err;
            }
            long ms = System.currentTimeMillis() - t0;
            boolean pass = itd.success && !itd.inconsistent;
            String statusTag = pass ? "<b style='color:#0a0'>PASS</b>" : "<b style='color:#b00'>FAIL</b>";
            String html =
                    statusTag +
                            "&nbsp; &bull; &nbsp;" +
                            "<span>" + ms + " ms </span>" +
                            "<span class='infoTip' title='Total runtime: includes KB loading, multiple prover calls, and postprocessing.'>&#9432;</span>" +
                            "<div style='color:#666'>Expected: " + esc(String.valueOf(itd.expectedAnswers)) + "</div>" +
                            "<div style='color:#666'>Actual: "   + esc(String.valueOf(itd.actualAnswers))   + "</div>" +
                            (itd.inconsistent ? "<div style='color:#b00'>Inconsistency detected</div>" : "");
            OneResult r = new OneResult();
            r.pass   = pass;
            r.millis = ms;
            r.html   = html;
            r.expected = (itd.expectedAnswers == null) ? java.util.Collections.emptyList() : new java.util.ArrayList<>(itd.expectedAnswers);
            r.actual   = (itd.actualAnswers   == null) ? java.util.Collections.emptyList() : new java.util.ArrayList<>(itd.actualAnswers);
            r.proofText = (itd.proof   == null) ? java.util.Collections.emptyList() : new java.util.ArrayList<>(itd.proof);
            return r;
        }
        catch (Throwable t) {
            System.err.println("InferenceTestSuite.runOne(): ERROR while running " + tqPath);
            t.printStackTrace();
            OneResult err = new OneResult();
            err.pass = false;
            err.millis = System.currentTimeMillis() - t0;
            err.html = "<div style='color:#b00'><b>ERROR:</b> "
                    + esc(t.getClass().getSimpleName() + ": " + t.getMessage())
                    + "</div>";
            err.expected = Collections.emptyList();
            err.actual = Collections.emptyList();
            err.proofText = new ArrayList<>();
            err.proofText.add(t.toString());
            return err;
        }
    }

    /** ***************************************************************
     */
    private static String esc(String s) {

        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;")
            .replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;"); }

    /** ***************************************************************
     * Compare the expected answers to the returned answers.  Return
     * true if no answers are found or if any pair of answers
     * is different.  Return false otherwise.
     *
     * TODO: If both answersList and tpp.bindings are a lit of entities,
     *       we enforce that all entity pair should be exactly the same;
     */
//    private static boolean sameAnswers(List<String> actualAnswerList, List<String> expectedAnswerList) {
//
//        if (debug) System.out.println("InferenceTestSuite.sameAnswers(1): expected answers: " + expectedAnswerList);
//        if (debug) System.out.println("InferenceTestSuite.sameAnswers(1): bindings: " + actualAnswerList);
//        String actualRes;
//        for (int i = 0; i < actualAnswerList.size(); i++) {
//            actualRes = actualAnswerList.get(i);
//            if (TPTP3ProofProcessor.isSkolemRelation(actualRes)) {
//                actualRes = normalizeSkolem(TPTP2SUMO.formToSUMO(actualRes));
//                if (!normalizeSkolem(expectedAnswerList.get(i)).equals(actualRes)) {
//                    if (debug) System.out.println("InferenceTestSuite.sameAnswers(1): different skolem answers: " +
//                            actualRes + " and " + expectedAnswerList.get(i));
//                    return false;    // return false if any pair of answers is different
//                }
//            }
//            else
//                if (!expectedAnswerList.get(i).equals(actualRes)) {
//                    if (debug) System.out.println("InferenceTestSuite.sameAnswers(1): different answers: " +
//                            actualRes + " and " + expectedAnswerList.get(i));
//                    return false;    // return false if any pair of answers is different
//                }
//        }
//        if (debug) System.out.println("InferenceTestSuite.sameAnswers(1): returning true");
//        return true;
//    }

    private static boolean sameAnswers(List<String> actualAnswerList, List<String> expectedAnswerList) {

        if (debug) {
            System.out.println("InferenceTestSuite.sameAnswers(1): expected answers: " + expectedAnswerList);
            System.out.println("InferenceTestSuite.sameAnswers(1): bindings: " + actualAnswerList);
        }

        // --- 1. Handle null or empty cases first -----------------------------------------------
        // If the prover produced no answers:
        //   - Return true only if the expected list is also empty (no answers expected).
        if (actualAnswerList == null || actualAnswerList.isEmpty()) {
            return expectedAnswerList == null || expectedAnswerList.isEmpty();
        }

        // If the prover and expected lists differ in length, they cannot be identical.
        if (actualAnswerList.size() != expectedAnswerList.size()) {
            if (debug) System.out.println("InferenceTestSuite.sameAnswers(1): answer count mismatch");
            return false;
        }

        // --- 2. Compare answers element by element ----------------------------------------------
        for (int i = 0; i < actualAnswerList.size(); i++) {
            String actualRes = actualAnswerList.get(i);
            String expectedRes = expectedAnswerList.get(i);

            // If the actual result is a Skolem relation (an existential witness term)
            // then normalize it for comparison with the expected answer.
            if (TPTP3ProofProcessor.isSkolemRelation(actualRes)) {
                actualRes = normalizeSkolem(TPTP2SUMO.formToSUMO(actualRes));

                // Compare normalized Skolemized answers.
                if (!normalizeSkolem(expectedRes).equals(actualRes)) {
                    if (debug) {
                        System.out.println("InferenceTestSuite.sameAnswers(1): different skolem answers: "
                                + actualRes + " and " + expectedRes);
                    }
                    return false;  // Mismatch → FAIL
                }
            }
            // Otherwise, do a direct string comparison for normal (non-Skolem) answers.
            else {
                if (!expectedRes.equals(actualRes)) {
                    if (debug) {
                        System.out.println("InferenceTestSuite.sameAnswers(1): different answers: "
                                + actualRes + " and " + expectedRes);
                    }
                    return false;  // Mismatch → FAIL
                }
            }
        }

        // --- 3. All checks passed ---------------------------------------------------------------
        if (debug) System.out.println("InferenceTestSuite.sameAnswers(1): returning true");
        return true;
    }

    /** ***************************************************************
     * Compare the expected answers to the returned answers.  Return
     * true if no answers are found or if any pair of answers
     * is different.  Return false otherwise.
     *
     * TODO: If both answersList and tpp.bindings are a lit of entities,
     *       we enforce that all entity pair should be exactly the same;
     */
    private static boolean sameAnswers(TPTP3ProofProcessor tpp, List<String> answerList) {

        if (debug) System.out.println("InferenceTestSuite.sameAnswers(2): expected answers: " + answerList);
        if (debug) System.out.println("InferenceTestSuite.sameAnswers(2): tpp proof size: " + tpp.proof.size());
        if (debug) System.out.println("InferenceTestSuite.sameAnswers(2): bindings: " + tpp.bindings);
        // TODO: tpp may be null for other reasons.
        // TODO: Here it says that if the prover produced no answers, we return true only if the expected list is also empty (no answers expected).
        if ((tpp == null || tpp.proof.isEmpty()) && (answerList == null || answerList.contains("no")))
            return true;         // return true if no answers are found in the inference engine
        if (answerList != null && !answerList.isEmpty()) {
            if (answerList.get(0).equals("yes"))
                return !tpp.proof.isEmpty() && tpp.containsFalse;   // return true if "yes" is expected, and we do find a contradiction (answer)
            else
                return sameAnswers(tpp.bindings, answerList);
        }
        return false;
    }

    /** ***************************************************************
     */
    private static File setOutputDir() throws IOException {

        String outputDirPath = KBmanager.getMgr().getPref("testOutputDir");
        if ((outputDirPath != null) && !outputDirPath.equals(""))
            return new File(outputDirPath);  // testOutputDir is set.
        else
            return null;
    }

    /** ***************************************************************
     */
    private static void clearOutputDir(File outputDir) throws IOException {

        if (outputDir == null) {
            System.out.println("Error in InferenceTestSuite.clearOutputDir(): null file input");
            return;
        }
        final File[] files = outputDir.listFiles();
        if (files != null)
            for (File f : files)
                f.delete();
    }

    /** ***************************************************************
     *  Copy test files to the output directory so that they are visible
     *  to Sigma as a Tomcat application.
     */
    private static void copyTestFiles(List<File> files, File outputDir) throws IOException  {

        if ((outputDir == null) || !outputDir.isDirectory()) {
            System.out.println("Error in InferenceTestSuite.copyTestFiles(): Could not find  " + outputDir);
            return;
        }
        String target;
        File fout;
        for (File f : files) {
            target = outputDir.getAbsolutePath() + File.separator + f.getName();
            fout = new File(target);
            if (!fout.exists()) {
                Files.copy(f.toPath(), fout.toPath());
                System.out.println("InferenceTestSuite.copyTestFiles(): copied file to: " + fout.getAbsolutePath());
            }
            else
                System.out.println("InferenceTestSuite.copyTestFiles(): already exists: " + fout.getAbsolutePath());
        }
    }

    /** ***************************************************************
     *  Note that 'files' variable is modified as a side effect.
     *  @return error messages, or null if none
     */
    private static String getTestFiles(List<File> files, File outputDir) throws IOException  {

        String inferenceTestDirPath = KBmanager.getMgr().getPref("inferenceTestDir");
        if ((inferenceTestDirPath == null) || inferenceTestDirPath.equals(""))
            return("Error in InferenceTestSuite.getTestFiles(): The Sigma preference \"inferenceTestDir\" has not been set");
        File inferenceTestDir = new File(inferenceTestDirPath);
        if (!inferenceTestDir.isDirectory() && !inferenceTestDir.mkdir())
            return("Error in InferenceTestSuite.getTestFiles(): Could not find or create " +
                    inferenceTestDir.getCanonicalPath());
        if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
            File baseDir = new File(KBmanager.getMgr().getPref("baseDir"));
            if (baseDir.isDirectory()) {
                File webappsDir = new File(baseDir,"webapps");
                if (webappsDir.isDirectory()) {
                    File waSigmaDir = new File(webappsDir,"sigma");
                    if (waSigmaDir.isDirectory())
                        outputDir = new File(waSigmaDir,"tests");
                }
            }
        }
        if (!outputDir.isDirectory())
            return("Error in InferenceTestSuite.getTestFiles(): Could not find or create " + outputDir);

        // At this point, inferenceTestDir and outputDir should be
        // set to viable values.
        File[] newfiles = inferenceTestDir.listFiles();
        System.out.println("INFO in InferenceTestSuite.getTestFiles(): number of files: " + newfiles.length);
        if (newfiles.length == 0) {
            System.err.println("Error in InferenceTestSuite.getTestFiles(): No test files found in " +
                    inferenceTestDir.getCanonicalPath());
            return("No test files found in " + inferenceTestDir.getCanonicalPath());
        }
        Arrays.sort(newfiles);
        for (File f : newfiles) {
            if (f.getName().endsWith(".tq"))
                files.add(f);
        }
        if (files.size() < 1) {
            System.err.println("INFO in InferenceTestSuite.getTestFiles(): No test files found in " +
                    inferenceTestDir.getCanonicalPath());
            return("No test files found in " + inferenceTestDir.getCanonicalPath());
        }
        return null;  // Normal exit, with "files" modified as a side effect
    }

    /** ***************************************************************
     * Convenience method that sets default parameters
    */
    public String test(KB kb) throws IOException  {
        return test(kb, DEFAULT_TIMEOUT, "");
    }

    /** ***************************************************************
     * Convenience method that sets some default parameters
     * @param timeout is a default timeout that is likely to be
     *                overwritten by a specification in the test
     *                data
    */
    public String test(KB kb, String systemChosen, int timeout) throws IOException  {

        if (systemChosen.equals("EProver"))
            KBmanager.getMgr().prover = KBmanager.Prover.EPROVER;
        if (systemChosen.equals("Vampire"))
            KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
        return test(kb, timeout, "");
    }

    /** ***************************************************************
     * Check if the required SUO-KIF file set is loaded and return false and
     * print an error if not.  Also issue a warning if extra non-required
     * files are present.
     */
    public boolean compareFiles(InfTestData itd) {

        boolean result = true;
        for (String f : itd.files) {
            if (!kb.containsFile(f)) {
                result = false;
                System.err.println("Error in InferenceTestSuite.compareFiles(): Required file " + f + " not in KB");
            }
        }
        for (String f : kb.constituents) {
            if (!itd.files.contains(f)) {
                System.out.println("InferenceTestSuite.compareFiles(): Extra, non-required file " + f + " found in KB");
            }
        }
        return result;
    }

    /** ***************************************************************
     * skolem terms in a string are converted to 'sK1'
     */
    public static String normalizeSkolem(String s) {

        //System.out.println("INFO in InferenceTestSuite.normalizeSkolem(): input: " + s);
        s = s.replaceAll("sK[0-9 ]+","sK1").replaceAll("esk\\d+","esk1");
        //System.out.println("INFO in InferenceTestSuite.normalizeSkolem(): result: " + s);
        return s;
    }

    /** ***************************************************************
     * parse answers
     */
    public static void parseAnswers(String s, InfTestData itd) {

        String answerstring = s;
        if (answerstring.equals("yes") || answerstring.equals("no")) {
            itd.expectedAnswers.add(answerstring);
        }
        else {
            FormulaAST ansForm = new FormulaAST(answerstring);
            List<String> answers = ansForm.complexArgumentsToArrayListString(1);
            //answerstring = normalizeSkolem(answerstring);
            //answerstring = StringUtil.removeEnclosingCharPair(answerstring,1,'(',')');
            for (String a : answers) {
                if (TPTP3ProofProcessor.isSkolemRelation(a))
                    a = normalizeSkolem(a);
                itd.expectedAnswers.add(a);
            }
        }
    }

    /** ***************************************************************
     * Read in a .tq inference test files and return an InfTestData
     * object or null if error
     * Note that if the expected answer is a skolem term, the function is converted to 'sK1'
     */
    public InfTestData readTestFile(File f) {

        InfTestData itd = null;
        try {
            if (f.getCanonicalPath().endsWith(".tq")) {
                itd = new InfTestData();
                itd.filename = f.getName();
                KIFAST test = new KIFAST();
                test.readFile(f.getCanonicalPath());
                System.out.println("INFO in InferenceTestSuite.readTestFile(): num formulas: " +
                        String.valueOf(test.formulaMap.size()));
                for (FormulaAST orderedF : test.formulaMap.values()) {
                    String formula = orderedF.getFormula();
                    if (formula.contains(";"))
                        formula = formula.substring(0, formula.indexOf(";"));
                    if (formula.startsWith("(note"))
                        itd.note = formula.substring(6, formula.length() - 1);
                    else if (formula.startsWith("(query"))
                        itd.query = formula.substring(7, formula.length() - 1);
                    else if (formula.startsWith("(answer")) {
                        parseAnswers(formula,itd);
                    }
                    else if (formula.startsWith("(time"))
                        itd.timeout = Integer.parseInt(formula.substring(6, formula.length() - 1));
                    else if (formula.startsWith("(file")) {
                        String filename = formula.substring(6, formula.length() - 1);
                        if (!itd.files.contains(filename)) itd.files.add(filename);
                    }
                    else if (formula.startsWith("(regen")) {
                        String regenValue = formula.substring(7, formula.length() - 1).trim();
                        itd.regen = regenValue.equalsIgnoreCase("yes") ||
                                regenValue.equalsIgnoreCase("true");
                    }
                    else itd.statements.add(formula);
                }
            }
            else {
                System.out.println("Error in readTestFile(): not a tq file: " + f.getName());
            }
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        return itd;
    }

    /********************************************************************
     * Convert ATP language name to the session TPTP file extension.
     * @param language ATP language: FOF, TFF, THF
     * @return file extension used by SessionTPTPManager
     */
    private static String sessionFileLang(String language) {

        if (StringUtil.emptyString(language))
            return "tptp";
        if ("FOF".equalsIgnoreCase(language) || "TPTP".equalsIgnoreCase(language))
            return "tptp";
        if ("TFF".equalsIgnoreCase(language))
            return "tff";
        if ("THF".equalsIgnoreCase(language))
            return "thf";
        return language.toLowerCase();
    }

    /** ***************************************************************
     * Read in all the .tq inference test files from the given list
     */
    public List<InfTestData> readTestFiles(List<File> files) {

        List<InfTestData> result = new ArrayList<>();
        InfTestData ifd;
        for (File f : files) {
            ifd = readTestFile(f);
            if (ifd != null)
                result.add(ifd);
        }
        return result;
    }

    /** ***************************************************************
     * Save TPTP translations using shared kbDir (backward-compatible).
     */
    public void saveTPTP(InfTestData itd) {
        saveTPTP(itd, null);
    }

    /** ***************************************************************
     * Save TPTP translations. When sessionId is provided, reads temp-stmt
     * from the session-specific directory.
     */
    public void saveTPTP(InfTestData itd, String sessionId) {

        String name = FileUtil.noExt(FileUtil.noPath(itd.filename));
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        String kbDir;
        if (sessionId != null && !sessionId.isEmpty()) {
            kbDir = com.articulate.sigma.trans.SessionTPTPManager.getSessionDir(sessionId).toString();
        }
        else {
            kbDir = KBmanager.getMgr().getPref("kbDir");
        }
        String sharedKbDir = KBmanager.getMgr().getPref("kbDir");
        String sep = File.separator;
        try {
            String langExt = SUMOformulaToTPTPformula.getLang();
            if (langExt.equals("fof"))
                langExt = "tptp";
            Files.copy(Paths.get(sharedKbDir + sep + kbName + "." + langExt),
                    Paths.get(sharedKbDir + sep + "KB.ax"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(Paths.get(kbDir + sep + "temp-stmt." + langExt),
                    Paths.get(sharedKbDir + sep + name + ".p"), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     */
    public void printResults(Collection<InfTestData> tests) {

        System.out.println();
        System.out.println("ALL TEST QUERIES FINISHED");
        System.out.println();
        for (InfTestData itd : tests) {
            System.out.print(itd.filename + "\t" + itd.SZSstatus + "\t" + itd.execTime);
            if (itd.inconsistent)
                System.out.println(" inconsistent!");
            else
                System.out.println();
        }
    }

    /** ***************************************************************
     * The main method that controls running a set of tests and returning
     * the result as an HTML page showing test results and links to proofs.
     * Note that this procedure deletes any prior user assertions.
     * If a test file has a set of KIF files different from what is already
     * loaded, create a new KB for it.
     */
    public String test(KB inputKB, int defaultTimeout, String TPTPlocation)
        throws IOException {

        kb = inputKB;
        if ("".equals(TPTPlocation) || TPTPlocation == null) {
            TPTPlocation = KBmanager.getMgr().getPref("systemsDir");
        }
        System.out.println("INFO in InferenceTestSuite.test(): Note that any prior user assertions will be deleted.");
        System.out.println("INFO in InferenceTestSuite.test(): Prover: " + KBmanager.getMgr().prover);
        StringBuilder result = new StringBuilder();
        int fail = 0;
        int pass = 0;
        String proof = null;

        String language = "EnglishLanguage";
        int maxAnswers;
        double totalTime = 0;
        long duration = 0;
        result = result.append("<h2>Inference tests</h2>\n");
        result = result.append("<table><tr><td>name</td><td>test file</td><td>result</td><td>Time (ms)</td></tr>");

        File outputDir = setOutputDir();
        clearOutputDir(outputDir);
        List<File> files = new ArrayList();
        String error = getTestFiles(files,outputDir);
        copyTestFiles(files,outputDir);
        if (error != null) return error;

        List<InfTestData> tests = readTestFiles(files);
        System.out.println("INFO in InferenceTestSuite.test(): number of files: " + files.size());
        int counter = 0;
        FormulaAST theQuery;
        FormulaPreprocessor fp;
        SUMOKBtoTFAKB stfa;
        Set<Expr> theQueries;
        long start;
        String lineHtml;
        String rfn;
        String resultsFilename;
        File resultsFile;
        TPTP3ProofProcessor tpp;
        String resultString;
        for (InfTestData itd : tests) {
            kb.deleteUserAssertionsAndReload();
            for (String s : itd.statements) if (!StringUtil.emptyString(s)) kb.tell(s);
            compareFiles(itd);
            maxAnswers = itd.expectedAnswers.size();
            try {
                System.out.println("====================================");
                System.out.println("INFO in InferenceTestSuite.test(): Note: " + itd.note);
                System.out.println("INFO in InferenceTestSuite.test(): Query: " + itd.query);
                theQuery = new FormulaAST();
                theQuery.read(itd.query);
                fp = new FormulaPreprocessor();
                stfa = new SUMOKBtoTFAKB();
                stfa.initOnce();
                SUMOtoTFAform.initOnce();
                theQueries = fp.preProcessExpr(theQuery, true, kb);
                for (Expr ex : theQueries) {
                    FormulaAST processed = new FormulaAST(ex.toKifString());
                    if (processed.isHigherOrder(kb)) {
                        System.out.println("Error in InferenceTestSuite.test(): skipping higher order query: " +
                                processed + " in test " + itd.note);
                        continue;
                    }
                    start = System.currentTimeMillis();
                    System.out.println("INFO in InferenceTestSuite.test(): Query " + processed + " is posed to " + KBmanager.getMgr().prover);
                    int actualTimeout = itd.timeout;
                    if (OVERRIDE_TIMEOUT)
                        actualTimeout = defaultTimeout;
                    if (KBmanager.getMgr().prover == KBmanager.Prover.EPROVER) {
                        com.articulate.sigma.tp.EProver eprover = new  com.articulate.sigma.tp.EProver(kb, "tptp", actualTimeout, maxAnswers);
                        eprover.askEProver(processed.getFormula());
                        proof = eprover.toString();
                    }
                    if (KBmanager.getMgr().prover == KBmanager.Prover.VAMPIRE) {
                        Vampire vampire = new Vampire(inputKB, "tptp", "CASC", false, actualTimeout, maxAnswers);
                        vampire.askVampire(processed.getFormula());
                        proof = vampire.toString() + " ";
                    }
                    if (KBmanager.getMgr().prover == KBmanager.Prover.LEO) {
                        LEO leo = new LEO(kb, "tptp", 30, 1, null);
                        leo.askLeo(processed.getFormula());
                        proof = leo.toString() + " "; 
                    }
                    duration = System.currentTimeMillis() - start;
                    System.out.print("INFO in InferenceTestSuite.test(): Duration: ");
                    System.out.println(duration);
                    itd.execTime = duration;
                    totalTime = totalTime + duration;
                    if (saveTPTP) saveTPTP(itd);
                }
            }
            catch (Exception ex) {
                result = result.append("<br>Error in InferenceTestSuite.test() while executing query ").append(itd.filename).append(": ").append(ex.getMessage()).append("<br>");
            }
            lineHtml = "<table ALIGN='LEFT' WIDTH=40%%><tr><TD BGCOLOR='#AAAAAA'>" +
                "<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";
            rfn = itd.filename;
            resultsFilename = rfn.substring(0,rfn.length()-3) + "-res.html";
            resultsFile = new File(outputDir, resultsFilename);
            tpp = new TPTP3ProofProcessor();
            try (FileWriter fw = new FileWriter(resultsFile); PrintWriter pw = new PrintWriter(fw)) {

                tpp.parseProofOutput(proof, kb);
                System.out.println("InferenceTestSuite.test() proof status: " + tpp.status + " for " + itd.note);
                itd.SZSstatus = tpp.status;
                if (tpp.status != null && (tpp.status.contains("Refutation") ||  tpp.status.contains("Theorem"))) {
                    pw.println(HTMLformatter.formatTPTP3ProofResult(tpp, itd.query, lineHtml, kb.name, language));
                    System.out.println("InferenceTestSuite.test() wrote results of: " + itd.note + " to " + resultsFile);
                }
                else
                    pw.println(tpp.status);
                itd.actualAnswers = tpp.bindings;
            }
            catch (IOException e) {
                throw new IOException("Error writing file " + resultsFile.getCanonicalPath());
            }
            if (tpp.inconsistency) {
                result.append("<h1>InferenceTestSuite.inferenceUnitTest(): Danger! possible inconsistency!</h1>");
                itd.inconsistent = true;
            }
            boolean different = true;
            if (proof != null && tpp.status != null && !tpp.status.startsWith("Timeout"))
                different = !sameAnswers(tpp,itd.expectedAnswers);
            if (different || tpp.noConjecture) {
                resultString = "fail";
                fail++;
            }
            else {
                resultString = "succeed";
                pass++;
            }
            result.append("<tr><td>").append(itd.note).append("</td><td><a href=\"tests/").append(itd.filename).append("\">").append(itd.filename).append("</a></td>");
            result.append("<td><a href=\"").append(outputDir.getName()).append("/").append(resultsFile.getName()).append("\">").append(resultString).append("</a></td>");
            result.append("<td>").append(String.valueOf(duration)).append("</td></tr>\n");
            System.out.println("Finished test #" + (counter+1) + " of " + files.size() + " : " + itd.filename);
            System.out.println(resultString);
            System.out.println();
            counter++;
        }

        printResults(tests);
        result.append("</table><P>\n");
        result.append("Total time: ");
        result.append(String.valueOf(totalTime/1000));
        result.append(" seconds<P>\n");

        result.append("Total correct: ");
        result.append(String.valueOf(pass));
        result.append("<P>\n");

        result.append("Total failed: ");
        result.append(String.valueOf(fail));
        result.append("<P>\n");
        kb.deleteUserAssertionsAndReload();
        return result.toString();
    }

    /** ***************************************************************
     * The method will be called in InferenceTest in unit test;
     * It takes a TQG file path, reading the kif statements and queries and expected answers;
     * It parses the theorem prover's inference output for actual answers;
     * Note that this procedure DOES NOT delete any prior user assertions.
     */
    public InfTestData inferenceUnitTest(String testpath, KB kb) {

        this.kb = kb; // tdn 10/15/24
        System.out.println("INFO in InferenceTestSuite.inferenceUnitTest(): testpath: " + testpath);
        // read the test file
        InfTestData itd = readTestFile(new File(testpath));
        if (OVERRIDE_TIMEOUT)
            itd.timeout = DEFAULT_TIMEOUT;
        compareFiles(itd);
        for (String formula : itd.statements)
             kb.tell(formula);
        System.out.println("INFO in InferenceTestSuite.inferenceUnitTest(): expected answers: " + itd.expectedAnswers);
        int maxAnswers = itd.expectedAnswers.size();
        FormulaAST theQuery = new FormulaAST();
        theQuery.read(itd.query);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        Set<Expr> theQueries = fp.preProcessExpr(theQuery,true,kb);
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        String processedStmt;
        Vampire vampire = new Vampire(kb, "tptp", "CASC", false, itd.timeout, maxAnswers);
        com.articulate.sigma.tp.EProver eprover = new EProver(kb, "tptp", itd.timeout, maxAnswers);
        com.articulate.sigma.tp.LEO leo = new LEO(kb, "tptp", itd.timeout, maxAnswers, null);
        for (Expr ex : theQueries) {
            FormulaAST f = new FormulaAST(ex.toKifString());
            processedStmt = f.getFormula();
            if (f.isHigherOrder(kb) && !SUMOformulaToTPTPformula.getLang().equals("thf")) {
                System.out.println("Error in InferenceTestSuite.inferenceUnitTest(): skipping higher order query: " +
                        processedStmt + " in test " + itd.note);
                continue;
            }
            System.out.println("\n============================");
            System.out.println("InferenceTestSuite.inferenceUnitTest(): ask: " + processedStmt);
            System.out.println("INFO in InferenceTestSuite.test(): Query is posed to " + KBmanager.getMgr().prover);
            if (null == KBmanager.getMgr().prover) {
                System.err.println("Error in InferenceTestSuite.inferenceUnitTest(): no prover or unknown prover: " + KBmanager.getMgr().prover);
                continue;
            }
            else switch (KBmanager.getMgr().prover) {
                case VAMPIRE:
                    vampire.askVampire(processedStmt);
                    System.out.println("InferenceTestSuite.inferenceUnitTest(): proof: " + vampire.toString());
                    tpp.parseProofOutput(vampire.output, processedStmt, kb,vampire.qlist);
                    break;
                case EPROVER:
                    eprover.askEProver(processedStmt);
                    System.out.println("InferenceTestSuite.inferenceUnitTest(): proof: " + eprover.toString());
                    tpp.parseProofOutput(eprover.output, processedStmt, kb,eprover.qlist);
                    break;
                case LEO:
                    leo.askLeo(processedStmt);
                    System.out.println("InferenceTestSuite.inferenceUnitTest(): proof: " + leo.toString());
                    tpp.parseProofOutput(leo.output, processedStmt, kb, leo.qlist);
                    break;
                default:
                    System.err.println("Error in InferenceTestSuite.inferenceUnitTest(): no prover or unknown prover: " + KBmanager.getMgr().prover);
                    continue;
            }
            if (tpp.inconsistency) {
                System.out.println("*****************************************");
                System.out.println("InferenceTestSuite.inferenceUnitTest(): Danger! possible inconsistency!");
                System.out.println("proof with no negated conjecture in " + itd.filename);
                System.out.println("*****************************************");
            }
            System.out.println("InferenceTestSuite.inferenceUnitTest(): bindings: " + tpp.bindings);
            System.out.println("InferenceTestSuite.inferenceUnitTest(): bindingMap: " + tpp.bindingMap);
            if (!tpp.noConjecture || tpp.inconsistency)
                System.out.println("InferenceTestSuite.inferenceUnitTest(): proof: " + tpp.proof);
            System.out.println("InferenceTestSuite.inferenceUnitTest(): inconsistency: " + tpp.noConjecture);

            itd.actualAnswers.addAll(tpp.bindings);
            System.out.println("InferenceTestSuite.inferenceUnitTest(): answers: " + itd.actualAnswers);
            //if (itd.actualAnswers != null)
            //    itd.actualAnswers.addAll(itd.actualAnswers);
            System.out.println("InferenceTestSuite.inferenceUnitTest(): tpp status: " + tpp.status);
            System.out.println("InferenceTestSuite.inferenceUnitTest(): actual answers: " + itd.actualAnswers);
            if (tpp.status != null && tpp.status.startsWith("Theorem") && itd.actualAnswers.isEmpty())
                itd.actualAnswers.add("yes");
            if (tpp.inconsistency) {
                itd.inconsistent = true;
                itd.actualAnswers = new ArrayList<>();
            }
            System.out.println("InferenceTestSuite.inferenceUnitTest(): actual answers(2): " + itd.actualAnswers);
            System.out.println("InferenceTestSuite.inferenceUnitTest(): expected answers(2): " + itd.expectedAnswers);
            System.out.println("InferenceTestSuite.inferenceUnitTest(): status: " + tpp.status);
            boolean different = true;
            if (tpp.proof != null  && !tpp.status.startsWith("Timeout"))
                different = !sameAnswers(tpp,itd.expectedAnswers);
            itd.success = !(different || tpp.noConjecture);
        }

        System.out.println("\n============================");
        return itd;
    }

    /** ***************************************************************
     * Test method
     */
    public void runPassing() {

        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        List<String> passingSet = Arrays.asList("TQG2","TQG3","TQG4","TQG13","TQG18","TQG30","TQG31","TQG32");
        for (String s : passingSet) {
            cmdLineTest(s + ".kif.tq");
        }
    }

    public boolean cmdLineTest(String filename) {

        LoggingUtils.log("trying: " + filename);
        try {
            kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            this.kb = kb;
            String proverType;
            switch (KBmanager.getMgr().prover) {
                case EPROVER:
                    proverType = "EPROVER";
                    break;
                case LEO:
                    proverType = "LEO";
                    break;
                case VAMPIRE:
                default:
                    proverType = "VAMPIRE";
                    break;
            }
            String language = "FOF";
            String vampireMode = "CASC";
            if ("tff".equalsIgnoreCase(SUMOformulaToTPTPformula.getLang())) language = "TFF";
            else if ("thf".equalsIgnoreCase(SUMOformulaToTPTPformula.getLang())) language = "THF";
            InfTestData itd = runTest(
                filename,
                proverType,
                language,
                vampireMode,
                false,
                false,
                false,
                false,
                DEFAULT_TIMEOUT
            );
            printTestResult(itd);
            if (itd.inconsistent) {
                System.out.println("*****************************************");
                System.out.println("InferenceTestSuite.cmdLineTest(): Danger! possible inconsistency!");
                System.out.println("proof with no negated conjecture in " + itd.filename);
                System.out.println("*****************************************");
            }
            if (itd.success) {
                System.out.println("InferenceTestSuite.cmdLineTest() : Success on " + filename);
                return true;
            }
            else {
                System.out.println("InferenceTestSuite.cmdLineTest() : Failure on " + filename);
                System.out.println("Expected: " + itd.expectedAnswers);
                System.out.println("Actual: " + itd.actualAnswers);
                System.out.println("SZS: " + itd.SZSstatus);
                return false;
            }
        }
        catch (Exception e) {
            System.err.println("Error in InferenceTestSuite.cmdLineTest()");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public void cmdLineAllTests() {

        int pass = 0;
        int fail = 0;
        long startAll = System.currentTimeMillis();

        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        this.kb = kb;

        String proverType;
        switch (KBmanager.getMgr().prover) {
            case EPROVER:
                proverType = "EPROVER";
                break;
            case LEO:
                proverType = "LEO";
                break;
            case VAMPIRE:
            default:
                proverType = "VAMPIRE";
                break;
        }

        String language = "FOF";
        String vampireMode = "CASC";

        if ("tff".equalsIgnoreCase(SUMOformulaToTPTPformula.getLang())) {
            language = "TFF";
        }
        else if ("thf".equalsIgnoreCase(SUMOformulaToTPTPformula.getLang())) {
            language = "THF";
        }

        System.out.println();
        System.out.println("====================================");
        System.out.println("Running all inference tests");
        System.out.println("Directory: " + this.inferenceTestDir);
        System.out.println("Tests: " + this.inferenceTestPaths.size());
        System.out.println("Prover: " + proverType);
        System.out.println("Language: " + language);
        System.out.println("====================================");
        System.out.println();

        for (String filename : this.inferenceTestPaths) {

            File testFile = new File(this.inferenceTestDir, filename);

            System.out.println();
            System.out.println("####################################");
            System.out.println("Running test: " + testFile.getAbsolutePath());
            System.out.println("####################################");

            InfTestData itd = runTest(
                testFile.getAbsolutePath(),
                proverType,
                language,
                vampireMode,
                false,
                false,
                false,
                false,
                DEFAULT_TIMEOUT
            );

            printTestResult(itd);

            if (itd.success && !itd.inconsistent) {
                pass++;
            }
            else {
                fail++;
            }
        }

        long totalMs = System.currentTimeMillis() - startAll;

        System.out.println();
        System.out.println("====================================");
        System.out.println("All inference tests complete");
        System.out.println("====================================");
        System.out.println("Total:  " + this.inferenceTestPaths.size());
        System.out.println("Passed: " + pass);
        System.out.println("Failed: " + fail);
        System.out.println("Time:   " + totalMs + " ms");
        System.out.println("====================================");
    }

    private void printTestResult(InfTestData itd) {

        System.out.println();
        System.out.println("====================================");
        System.out.println("Inference Test Result");
        System.out.println("====================================");
        System.out.println("File:       " + itd.filename);
        System.out.println("Note:       " + itd.note);
        System.out.println("Query:      " + itd.query);
        System.out.println("Timeout:    " + itd.timeout + " seconds");
        System.out.println("SZS:        " + itd.SZSstatus);
        System.out.println("Success:    " + itd.success);
        System.out.println("Inconsistent: " + itd.inconsistent);
        System.out.println("Exec time:  " + itd.execTime + " ms");
        System.out.println("Expected:   " + itd.expectedAnswers);
        System.out.println("Actual:     " + itd.actualAnswers);

        if (itd.proof != null && !itd.proof.isEmpty()) {
            System.out.println();
            System.out.println("---- Proof / Prover Output ----");
            for (String line : itd.proof) {
                System.out.println(line);
            }
        }

        System.out.println("====================================");
        System.out.println();
    }

    /****************************************************************
     * Undo all parts of the state that have anything to do with user assertions made during inference.
     * Defaults to shared UA files (backward compatible).
     */
    public static void resetAllForInference(KB kb) throws IOException {
        resetAllForInference(kb, null);
    }

    /****************************************************************
     * Undo all parts of the state that have anything to do with user assertions made during inference.
     * Session-aware version for isolated TQ test handling.
     *
     * @param kb The knowledge base
     * @param sessionId Optional HTTP session ID for session-specific cleanup.
     *                  If null or empty, uses shared UA files.
     */
    public static void resetAllForInference(KB kb, String sessionId) throws IOException {

        boolean sessionMode = sessionId != null && !sessionId.isEmpty();
        try {
            kb.withUserAssertionLock(() -> {
                if (sessionMode) {
                    deleteSessionUserAssertionFiles(kb, sessionId);
                    if (kb.termDepthCache != null)
                        kb.termDepthCache.clear();
                    return Boolean.TRUE;
                }
                int before = kb.countUserAssertionFormulasInMemory();
                int purged = kb.purgeUserAssertionsFromMemory();
                int after  = kb.countUserAssertionFormulasInMemory();
                if (KB.debug > 0) {
                    System.out.println("resetAllForInference(): UA in-memory before=" + before + ", purged=" + purged + ", after=" + after);
                }
                kb.deleteUserAssertions();
                deleteSharedUserAssertionFiles(kb);
                if (kb.termDepthCache != null) kb.termDepthCache.clear();
                return Boolean.TRUE;
            });
        }
        catch (RuntimeException re) {
            Throwable cause = re.getCause();
            if (cause instanceof IOException) throw (IOException) cause;
            throw re;
        }
    }

    /****************************************************************
     * Delete session-specific user assertion files.
     *
     * @param kb The knowledge base
     * @param sessionId The HTTP session ID
     */
    private static void deleteSessionUserAssertionFiles(KB kb, String sessionId) {

        java.nio.file.Path sessionDir = com.articulate.sigma.trans.SessionTPTPManager.getSessionDir(sessionId);
        final String kbName = kb.name;
        deleteIfExists(sessionDir.resolve(kbName + KB._userAssertionsString).toFile()); // *_UserAssertions.kif
        deleteIfExists(sessionDir.resolve(kbName + KB._userAssertionsTPTP).toFile());   // *_UserAssertions.tptp
        deleteIfExists(sessionDir.resolve(kbName + KB._userAssertionsTFF).toFile());    // *_UserAssertions.tff
        deleteIfExists(sessionDir.resolve(kbName + KB._userAssertionsTHF).toFile());    // *_UserAssertions.thf
    }

    /****************************************************************
     * Delete shared user assertion files (original behavior).
     *
     * @param kb The knowledge base
     */
    private static void deleteSharedUserAssertionFiles(KB kb) {
        final File dir = new File(KBmanager.getMgr().getPref("kbDir"));
        final String kbName = kb.name;
        deleteIfExists(new File(dir, kbName + KB._userAssertionsString)); // *_UserAssertions.kif
        deleteIfExists(new File(dir, kbName + KB._userAssertionsTPTP));   // *_UserAssertions.tptp
        deleteIfExists(new File(dir, kbName + KB._userAssertionsTFF));    // *_UserAssertions.tff
        deleteIfExists(new File(dir, kbName + KB._userAssertionsTHF));    // *_UserAssertions.thf
    }

    /****************************************************************
     */
    private static void deleteIfExists(File f) {
        if (f.exists() && !f.delete()) {
            System.out.println("WARN resetAllForInference(): failed to delete " + f.getAbsolutePath());
        }
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("============== InferenceTestSuite class =============");
        System.out.println("Test files are s-expressions that take valid SUMO formulas");
        System.out.println("or meta-predicates that are one of (note \"message\"), ");
        System.out.println("(query <formula>), (answer <term>), (time <integer>)");
        System.out.println("or (file \"path\")");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -p - print available inference test paths");
        System.out.println("  --test <name> - run named test file in config.xml inferenceTestDir");
        System.out.println("  --inf <mode> - run test files known to pass in the given mode in config.xml inferenceTestDir");
        System.out.println("  --all <mode> - run all test files in the given mode in config.xml inferenceTestDir");
        System.out.println("     e - run with eprover (add letter to options above)");
        System.out.println("     v - run with vampire (add letter to options above)");
        System.out.println("     l - run with LEO-III (add letter to options above)");
        System.out.println("     f - run with TF0 language");
        System.out.println("     0 - run with TH0 language");
        System.out.println("     o - override test timeout with global timeout of " + DEFAULT_TIMEOUT + " sec");
    }

     /** ***************************************************************
     * Test method
     */
    public static void main(String[] args) {

        System.out.println("INFO in KB.main()");
        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        if (argMap.isEmpty() || argMap.containsKey("h")) showHelp();
        else {
            KBmanager.getMgr().initializeOnce();
            InferenceTestSuite inferenceTestSuite = new InferenceTestSuite(KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname")));
            if (argMap.containsKey("l")) SUMOKBtoTPTPKB.setLang("thf");
            if (argMap.containsKey("f")) SUMOKBtoTPTPKB.setLang("tff");
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            try {
                resetAllForInference(kb);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            if (argMap.containsKey("p")) {
                LoggingUtils.log(inferenceTestSuite.getInferenceTestPaths().size() + " Inference tests found!");
                for (String path : inferenceTestSuite.getInferenceTestPaths()) LoggingUtils.log("  " + path);
            }
            else if (argMap.containsKey("e")) {
                KBmanager.getMgr().prover = KBmanager.Prover.EPROVER;
            }
            else if (argMap.containsKey("l")) {
                SUMOformulaToTPTPformula.setLang("thf");
                KBmanager.getMgr().prover = KBmanager.Prover.LEO;
            }
            else if (argMap.containsKey("f")) {
                SUMOformulaToTPTPformula.setLang("tff");
                SUMOKBtoTFAKB skbtfakb = new SUMOKBtoTFAKB();
                skbtfakb.initOnce();
                SUMOtoTFAform.initOnce();
            }
            else if (argMap.containsKey("0")) {
                SUMOformulaToTPTPformula.setLang("thf");
            }
            else if (argMap.containsKey("v"))
                KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
            else if (argMap.containsKey("o")) {
                OVERRIDE_TIMEOUT = true;
            }
            else if (argMap.containsKey("test") && argMap.get("test").size() == 1) {
                inferenceTestSuite.cmdLineTest(argMap.get("test").get(0));
            }
            else if (argMap.containsKey("inf")) {
                inferenceTestSuite.runPassing();
            }
            else if (argMap.containsKey("all")) {
                inferenceTestSuite.cmdLineAllTests();
            }
            else showHelp();
        }
    }
}