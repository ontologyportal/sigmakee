package com.articulate.sigma;

import com.articulate.sigma.tp.Vampire;
import com.articulate.sigma.trans.*;
import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.StringUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;


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
public class InferenceTestSuite {

    /** Total time */
    public static long totalTime = 0;

    /** Default timeout for queries with unspecified timeouts or override when selected */
    public static int _DEFAULT_TIMEOUT = 30;

    public static boolean overrideTimeout = false;

    public KB kb = null;

    public static boolean debug = false;

    // save TPTP translations of each problem as <probName>.p
    public static boolean saveTPTP =  true;

    public static Set<String> metaPred = new HashSet(
            Arrays.asList("note", "time", "query", "answer"));



    public static class OneResult {
        public boolean pass;
        public long millis;
        public String html;

        public java.util.List<String> expected;
        public java.util.List<String> actual;

        public List<String> proofText;
    }

    /** Thin wrapper for the JSP buttons: returns PASS/FAIL + time + a tiny HTML summary. */
    public OneResult runOne(final KB kb,
                            final String engine,
                            final int timeoutSec,
                            final String tqPath,
                            final boolean modusPonens) {
        long t0 = System.currentTimeMillis();
        InfTestData itd;
        itd = runSingleTestFile(kb, engine, timeoutSec, tqPath, modusPonens);

        System.out.println("---- InferenceTestSuite.runOne(1):");
        System.out.println(itd.proof);

        if (itd.proof.isEmpty()){
            OneResult err = new OneResult();
            err.pass = false;
            err.millis = System.currentTimeMillis() - t0;
            err.html = "<div style='color:#b00'><b>ERROR:</b> "
                    + "An error occurred during the execution"
                    + "</div>";
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

    /** Runs exactly one .tq test file (like the inner body of test()) and returns the InfTestData. */
    private InfTestData runSingleTestFile(final KB kb,
                                          final String engine,        // "Vampire" | "EProver" | "LEO"
                                          final int timeoutSec,
                                          final String tqPath,
                                          final boolean modusPonens) {

        this.kb = kb;

        // Choose prover (same mechanism test() uses)
        switch (engine.toUpperCase()) {
            case "EPROVER": KBmanager.getMgr().prover = KBmanager.Prover.EPROVER; break;
            case "LEO":     KBmanager.getMgr().prover = KBmanager.Prover.LEO;     break;
            default:        KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE; break;
        }

        // InferenceTestSuite.useModusPonens = modusPonens;

        // Parse .tq → InfTestData (same as test())
        final File tf = new File(tqPath);
        InfTestData itd = readTestFile(tf);

        // Apply timeout policy (mirror test(); if overrideTimeout is true, force default)
        if (overrideTimeout) {
            itd.timeout = timeoutSec > 0 ? timeoutSec : _DEFAULT_TIMEOUT;   // was _DEFAULT_TIMEOUT only
        } else {
            itd.timeout = timeoutSec > 0 ? timeoutSec : (itd.timeout > 0 ? itd.timeout : _DEFAULT_TIMEOUT);
        }

        // --- Isolated assertion block (prevents KB pollution) ---
        compareFiles(itd);

        // Step 1: assert formulas for this test
        List<String> asserted = new ArrayList<>(itd.statements);
        for (String s : asserted) {
            kb.tell(s);
        }

        try {
            // Step 2: reload KB before running queries
            KBmanager.getMgr().loadKBforInference(kb);

            // Step 3: run the queries as before
            int maxAnswers = Math.max(1, itd.expectedAnswers.size());
            Formula theQuery = new Formula();
            theQuery.read(itd.query);
            Set<Formula> theQueries = new FormulaPreprocessor().preProcess(theQuery, true, kb);

            itd.actualAnswers = new ArrayList<>();
            for (Formula f : theQueries) {
                TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
                String q = f.getFormula();
                if (f.isHigherOrder(kb) && !"thf".equals(SUMOformulaToTPTPformula.lang)) {
                    System.out.println("Skipping higher-order query not in THF: " + q);
                    continue;
                }

                switch (KBmanager.getMgr().prover) {
                    case VAMPIRE:
                        com.articulate.sigma.tp.Vampire vampire;
                        if (modusPonens) {
                            vampire = kb.askVampireModensPonens(q, itd.timeout, maxAnswers);
                        }else{
                            vampire = kb.askVampire(q, itd.timeout, maxAnswers);
                        }
                        System.out.println("vampire-output");
                        System.out.println(vampire.output);
                        tpp.parseProofOutput(vampire.output, q, kb, vampire.qlist);
                        itd.proof = vampire.output;
                        break;
                    case EPROVER:
                        com.articulate.sigma.tp.EProver eprover = kb.askEProver(q, itd.timeout, maxAnswers);
                        tpp.parseProofOutput(eprover.output, q, kb, eprover.qlist);
                        itd.proof = eprover.output;
                        break;
                    case LEO:
                        com.articulate.sigma.tp.LEO leo = kb.askLeo(q, itd.timeout, maxAnswers);
                        tpp.parseProofOutput(leo.output, q, kb, leo.qlist);
                        itd.proof = leo.output;
                        break;
                    default:
                        System.err.println("Unknown prover: " + KBmanager.getMgr().prover);
                }

                if (tpp.status != null && tpp.status.startsWith("Theorem") && itd.actualAnswers.isEmpty())
                    itd.actualAnswers.add("yes");

                if (tpp.inconsistency) {
                    itd.inconsistent = true;
                    itd.actualAnswers = new ArrayList<>();
                }

                if (tpp.bindings != null) itd.actualAnswers.addAll(tpp.bindings);

                boolean different = true;
                if (tpp.proof != null && (tpp.status == null || !tpp.status.startsWith("Timeout"))) {
                    different = !sameAnswers(tpp, itd.expectedAnswers);
                }
                itd.success = !(different || tpp.noConjecture);
            }
        }
        finally {
            try {
                // Remove any test assertions
                resetAllForInference(kb);
            } catch (Exception e) {
                System.err.println("Warning: could not reset KB after test: " + e.getMessage());
            }
        }
        // --- end isolation block ---

        return itd;
    }

    private static String esc(String s){
        if(s==null)return "";
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
        return test(kb, _DEFAULT_TIMEOUT, "");
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
        public boolean inconsistent = false;
        public boolean success = false;
        public float execTime = 0;
        public String SZSstatus = "";
        public List<String> proof = new ArrayList<>();
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
        if (debug) System.out.println("INFO in InferenceTestSuite.readTestFile(): answerString: " + answerstring);
        if (answerstring.equals("yes") || answerstring.equals("no")) {
            itd.expectedAnswers.add(answerstring);
        }
        else {
            Formula ansForm = new Formula(answerstring);
            if (debug) System.out.println("INFO in InferenceTestSuite.readTestFile(): answer form: " + ansForm);
            List<String> answers = ansForm.complexArgumentsToArrayListString(1);
            if (debug) System.out.println("INFO in InferenceTestSuite.readTestFile(): answers: " + answers);
            //answerstring = normalizeSkolem(answerstring);
            //answerstring = StringUtil.removeEnclosingCharPair(answerstring,1,'(',')');
            for (String a : answers) {
                if (TPTP3ProofProcessor.isSkolemRelation(a))
                    a = normalizeSkolem(a);
                if (debug) System.out.println("INFO in InferenceTestSuite.readTestFile(): answers normalized: " + a);
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

        InfTestData ifd = null;
        try {
            if (f.getCanonicalPath().endsWith(".tq")) {
                ifd = new InfTestData();
                ifd.filename = f.getName();
                KIF test = new KIF();
                test.readFile(f.getCanonicalPath());
                System.out.println("INFO in InferenceTestSuite.readTestFile(): num formulas: " +
                        String.valueOf(test.formulaMap.keySet().size()));
                for (String formula : test.formulaMap.keySet()) {
                    if (formula.contains(";"))
                        formula = formula.substring(0, formula.indexOf(";"));
                    System.out.println("INFO in InferenceTestSuite.readTestFile(): Formula: " + formula);
                    if (formula.startsWith("(note"))
                        ifd.note = formula.substring(6, formula.length() - 1);
                    else if (formula.startsWith("(query"))
                        ifd.query = formula.substring(7, formula.length() - 1);
                    else if (formula.startsWith("(answer")) {
                        parseAnswers(formula,ifd);
                    }
                    else if (formula.startsWith("(time"))
                        ifd.timeout = Integer.parseInt(formula.substring(6, formula.length() - 1));
                    else if (formula.startsWith("(file")) {
                        String filename = formula.substring(6, formula.length() - 1);
                        if (!ifd.kbFiles.contains(filename))
                            ifd.kbFiles.add(filename);
                    }
                    else
                        ifd.statements.add(formula);
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
        return ifd;
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
     */
    public void saveTPTP(InfTestData itd) {

        String name = FileUtil.noExt(FileUtil.noPath(itd.filename));
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        String kbDir = KBmanager.getMgr().getPref("kbDir");
        String sep = File.separator;
        try {
            String langExt = SUMOformulaToTPTPformula.lang;
            if (langExt.equals("fof"))
                langExt = "tptp";
            Files.copy(Paths.get(kbDir + sep + kbName + "." + langExt),
                    Paths.get(kbDir + sep + "KB.ax"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(Paths.get(kbDir + sep + "temp-stmt." + langExt),
                    Paths.get(kbDir + sep + name + ".p"), StandardCopyOption.REPLACE_EXISTING);
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
        totalTime = 0;
        long duration = 0;
        result = result.append("<h2>Inference tests</h2>\n");
        result = result.append("<table><tr><td>name</td><td>test file</td><td>result</td><td>Time (ms)</td></tr>");

        File outputDir = setOutputDir();
        clearOutputDir(outputDir);
        List<File> files = new ArrayList();
        String error = getTestFiles(files,outputDir);
        copyTestFiles(files,outputDir);
        if (error != null)
            return error;

        List<InfTestData> tests = readTestFiles(files);
        System.out.println("INFO in InferenceTestSuite.test(): number of files: " + files.size());
        int counter = 0;
        Formula theQuery;
        FormulaPreprocessor fp;
        SUMOKBtoTFAKB stfa;
        Set<Formula> theQueries;
        long start;
        String lineHtml;
        String rfn;
        String resultsFilename;
        File resultsFile;
        TPTP3ProofProcessor tpp;
        String resultString;
        for (InfTestData itd : tests) {
            kb.deleteUserAssertionsAndReload();
            for (String s : itd.statements)
                if (!StringUtil.emptyString(s))
                    kb.tell(s);
            compareFiles(itd);
            maxAnswers = itd.expectedAnswers.size();
            try {
                System.out.println("====================================");
                System.out.println("INFO in InferenceTestSuite.test(): Note: " + itd.note);
                System.out.println("INFO in InferenceTestSuite.test(): Query: " + itd.query);
                theQuery = new Formula(itd.query);
                fp = new FormulaPreprocessor();
                stfa = new SUMOKBtoTFAKB();
                stfa.initOnce();
                SUMOtoTFAform.initOnce();
                theQueries = fp.preProcess(theQuery,true,kb);
                for (Formula processed : theQueries) {
                    if (processed.isHigherOrder(kb)) {
                        System.out.println("Error in InferenceTestSuite.test(): skipping higher order query: " +
                                processed + " in test " + itd.note);
                        continue;
                    }
                    start = System.currentTimeMillis();
                    System.out.println("INFO in InferenceTestSuite.test(): Query " + processed + " is posed to " + KBmanager.getMgr().prover);
                    int actualTimeout = itd.timeout;
                    if (overrideTimeout)
                        actualTimeout = defaultTimeout;
                    if (KBmanager.getMgr().prover == KBmanager.Prover.EPROVER)
                        proof = kb.askEProver(processed.getFormula(), actualTimeout, maxAnswers) + " ";
                    if (KBmanager.getMgr().prover == KBmanager.Prover.VAMPIRE)
                        proof = kb.askVampire(processed.getFormula(), actualTimeout, maxAnswers) + " ";
                    if (KBmanager.getMgr().prover == KBmanager.Prover.LEO)
                        proof = kb.askLeo(processed.getFormula(), actualTimeout, maxAnswers) + " ";
                    duration = System.currentTimeMillis() - start;
                    System.out.print("INFO in InferenceTestSuite.test(): Duration: ");
                    System.out.println(duration);
                    itd.execTime = duration;
                    totalTime = totalTime + duration;
                    if (saveTPTP)
                        saveTPTP(itd);
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
        if (overrideTimeout)
            itd.timeout = _DEFAULT_TIMEOUT;
        compareFiles(itd);
        for (String formula : itd.statements)
             kb.tell(formula);
        KBmanager.getMgr().loadKBforInference(kb);
        System.out.println("INFO in InferenceTestSuite.inferenceUnitTest(): expected answers: " + itd.expectedAnswers);
        int maxAnswers = itd.expectedAnswers.size();
        Formula theQuery = new Formula();
        theQuery.read(itd.query);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        Set<Formula> theQueries = fp.preProcess(theQuery,true,kb);
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        String processedStmt;
        Vampire vampire;
        com.articulate.sigma.tp.EProver eprover;
        com.articulate.sigma.tp.LEO leo;
        for (Formula f : theQueries) {
            processedStmt = f.getFormula();
            if (f.isHigherOrder(kb) && !SUMOformulaToTPTPformula.lang.equals("thf")) {
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
                    vampire = kb.askVampire(processedStmt, itd.timeout, maxAnswers);
                    System.out.println("InferenceTestSuite.inferenceUnitTest(): proof: " + vampire.toString());
                    tpp.parseProofOutput(vampire.output, processedStmt, kb,vampire.qlist);
                    break;
                case EPROVER:
                    eprover = kb.askEProver(processedStmt, itd.timeout, maxAnswers);
                    System.out.println("InferenceTestSuite.inferenceUnitTest(): proof: " + eprover.toString());
                    tpp.parseProofOutput(eprover.output, processedStmt, kb,eprover.qlist);
                    break;
                case LEO:
                    leo = kb.askLeo(processedStmt, itd.timeout, maxAnswers);
                    System.out.println("InferenceTestSuite.inferenceUnitTest(): proof: " + leo.toString());
                    tpp.parseProofOutput(leo.output, processedStmt, kb,leo.qlist);
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

    /** ***************************************************************
     * Test method
     */
    public boolean cmdLineTest(String filename) {

        System.out.println("InferenceTestSuite.cmdLineTest(): trying: " + filename);
        try {
            kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            String path = KBmanager.getMgr().getPref("inferenceTestDir");
            InfTestData itd = inferenceUnitTest(path + File.separator + filename,kb);
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

    /****************************************************************
     * Undo all parts of the state that have anything to do with user assertions made during inference.
     */
    public static void resetAllForInference(KB kb) throws IOException {

        try {
            kb.withUserAssertionLock(() -> {

                System.out.println("in InferenceTestSuite.resetAllForInference(): delete user assertions");

                // 1) Purge UA formulas from in-memory KB indexes
                int before = kb.countUserAssertionFormulasInMemory();
                int purged = kb.purgeUserAssertionsFromMemory();
                int after  = kb.countUserAssertionFormulasInMemory();

                if (KB.debug) {
                    System.out.println("resetAllForInference(): UA in-memory before=" + before
                            + ", purged=" + purged
                            + ", after=" + after);
                }

                // 2) Remove UA from constituents + delete UA.<current-lang> inference artifact (if any)
                kb.deleteUserAssertions();

                // 3) Delete UA files on disk (KIF + translated variants)
                final File dir = new File(KBmanager.getMgr().getPref("kbDir"));
                final String kbName = kb.name;

                deleteIfExists(new File(dir, kbName + KB._userAssertionsString)); // *_UserAssertions.kif
                deleteIfExists(new File(dir, kbName + KB._userAssertionsTPTP));   // *_UserAssertions.tptp
                deleteIfExists(new File(dir, kbName + KB._userAssertionsTFF));    // *_UserAssertions.tff
                deleteIfExists(new File(dir, kbName + KB._userAssertionsTHF));    // *_UserAssertions.thf

                // 4) Clear cheap caches that can be polluted by accumulating terms
                if (kb.termDepthCache != null) {
                    kb.termDepthCache.clear();
                }

                return Boolean.TRUE;
            });
        }
        catch (RuntimeException re) {
            // Unwrap IOException thrown inside the lock
            Throwable cause = re.getCause();
            if (cause instanceof IOException) throw (IOException) cause;
            throw re;
        }
    }

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
        System.out.println("  -t <name> - run named test file in config.xml inferenceTestDir");
        System.out.println("  -i <mode> - run test files known to pass in the given mode in config.xml inferenceTestDir");
        System.out.println("  -a <mode> - run all test files in the given mode in config.xml inferenceTestDir");
        System.out.println("     e - run with eprover (add letter to options above)");
        System.out.println("     v - run with vampire (add letter to options above)");
        System.out.println("     l - run with LEO-III (add letter to options above)");
        System.out.println("     f - run with TF0 language");
        System.out.println("     0 - run with TH0 language");
        System.out.println("     o - override test timeout with global timeout of " + _DEFAULT_TIMEOUT + " sec");
    }

     /** ***************************************************************
     * Test method
     */
    public static void main(String[] args) {

        System.out.println("args: " + Arrays.toString(args));
        if (args != null && args.length > 0) {
            if (args[0].equals("-h"))
                showHelp();
            else {
                KBmanager.getMgr().initializeOnce();
                InferenceTestSuite its = new InferenceTestSuite();
                if (args[0].indexOf('l') != -1)
                    SUMOKBtoTPTPKB.lang = "thf";
                if (args[0].indexOf('f') != -1)
                    SUMOKBtoTPTPKB.lang = "tff";
                KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
                try {
                    resetAllForInference(kb);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                if (args[0].indexOf('e') != -1) {
                    KBmanager.getMgr().prover = KBmanager.Prover.EPROVER;
                    kb.loadEProver();
                }
                if (args[0].indexOf('l') != -1) {
                    SUMOformulaToTPTPformula.lang = "thf";
                    KBmanager.getMgr().prover = KBmanager.Prover.LEO;
                    kb.loadLeo();
                }
                if (args[0].indexOf('f') != -1) {
                    SUMOformulaToTPTPformula.lang = "tff";
                    SUMOKBtoTFAKB skbtfakb = new SUMOKBtoTFAKB();
                    skbtfakb.initOnce();
                    SUMOtoTFAform.initOnce();
                }
                if (args[0].indexOf('0') != -1) {
                    SUMOformulaToTPTPformula.lang = "thf";
                }
                if (args[0].indexOf('v') != -1)
                    KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
                if (args[0].indexOf('0') != -1)
                    overrideTimeout = true;
                System.out.println("in InferenceTestSuite.main(): using prover: " + KBmanager.getMgr().prover);

                if (args.length > 1 && args[0].contains("t")) {
                    its.cmdLineTest(args[1]);
                }
                else if (args.length > 1 && args[0].contains("i")) {
                    its.runPassing();
                }
                else if (args.length > 0 && args[0].contains("a")) {
                    try {
                        its.test(kb);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        else
            showHelp();
    }
}


