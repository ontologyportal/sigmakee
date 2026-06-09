/** This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also sigmakee.sourceforge.net */

package com.articulate.sigma.tp;

import com.articulate.sigma.*;
import com.articulate.sigma.trans.*;
import com.articulate.sigma.utils.*;
import com.articulate.sigma.parsing.CLIMapParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.*;
import java.io.File;

/** Represents a single .tq inference test, including metadata, assertions, expected answers, and results. */
public class InferenceTest {

    /** Path to the .tq inference test file. */
    public String filePath;
    /** Minimum TPTP language required by the test. */
    public String minLang = "fof";
    /** Whether session TPTP files must be regenerated after applying assertions. */
    public boolean tptpRegenRequired = false;
    /** Optional descriptive note for the test. */
    public String note;
    /** Optional category used to group the test. */
    public String category;
    /** Prover timeout in seconds. */
    public int timeout = 30;
    /** Query formula to send to the prover. */
    public String query;
    /** Validation errors found in the test. */
    public List<String> errors = new ArrayList<>();
    /** Test assertions to add before running the query. */
    public List<String> assertions = new ArrayList<>();
    /** KB constituent files required by the test. */
    public List<String> requiredConstituents = new ArrayList<>();
    /** Expected prover answers for the test. */
    public List<String> expectedAnswers = new ArrayList<>();
    /** Result produced by running the test. */
    public InferenceTestResult result = null;

    /** Stores the prover output and success state for an inference test run. */
    public static class InferenceTestResult {

        /** Whether the test result matched the expected answers. */
        public boolean success;
        /** Prover execution time in milliseconds. */
        public float execTime;
        /** SZS status returned by the prover. */
        public String szsStatus;
        /** Combined prover proof, stdout, stderr, and error output. */
        public List<String> proof = new ArrayList<>();
        /** Answers returned by the prover. */
        public List<String> answers = new ArrayList<>();
        /** Whether the prover reported an inconsistency in the ontology. */
        public boolean contradictionFound;
    }

    /********************************************************************
     * Creates an inference test from the given file path. 
     * @param filePath path to the .tq test file. 
     */
    public InferenceTest(String filePath) {

        this.filePath = filePath;
        readTestFile();
        validateTestData();
    }

    /********************************************************************
     * Runs this test using its meta-predicate options.
     * @param kb knowledge base used for the test.
     * @param proverType prover to use.
     * @param closedWorldAssumption whether to use the closed world assumption.
     * @param modusPonens whether to enable modus ponens.
     * @param dropOnePremise whether to drop one premise during inference.
     * @param holUseModals whether HOL modal translation is enabled. 
     */
    public void runTest(KB kb, String proverType, boolean closedWorldAssumption, boolean modusPonens, boolean dropOnePremise, boolean holUseModals) {
        
        runTest(kb, proverType, this.minLang, "CASC", closedWorldAssumption, modusPonens, dropOnePremise, holUseModals, this.timeout, this.expectedAnswers.size());
    }

    /********************************************************************
     * Runs this test overriding the meta-predicate options.
     * @param kb knowledge base used for the test.
     * @param proverType prover to use.
     * @param language target logical language.
     * @param vampireMode Vampire execution mode.
     * @param closedWorldAssumption whether to use the closed world assumption.
     * @param modusPonens whether to enable modus ponens.
     * @param dropOnePremise whether to drop one premise during inference.
     * @param holUseModals whether HOL modal translation is enabled.
     * @param timeout prover timeout in seconds.
     * @param maxAnswers maximum number of answers to return.
     */
    public void runTest(KB kb, String proverType, String language, String vampireMode, boolean closedWorldAssumption, boolean modusPonens, boolean dropOnePremise, boolean holUseModals, int timeout, int maxAnswers) {
        
        if (!this.errors.isEmpty()) {
            LoggingUtils.log("ERROR", "Cannot run test, has errors!: " + this.errors);
            return;
        }
        this.result = new InferenceTestResult();
        String sessionId = "tq-" + UUID.randomUUID();
        TheoremProverController tpc = new TheoremProverController();
        try {
            TPTPGenerationManager.waitForAllTPTP(600);
            applyAssertions(kb, sessionId, language);
            populateResult(kb, tpc.runQuery(kb, sessionId, this.query, this.filePath, "TEST_FILE", proverType, language, vampireMode, closedWorldAssumption, modusPonens, dropOnePremise, holUseModals, timeout, maxAnswers));
        }
        finally {
            reset(kb, sessionId);
        }
    }

    /********************************************************************
     * Clears session assertion state after an inference test.
     * @param kb knowledge base to reset.
     * @param sessionId session identifier to clean up. 
     */
    public void reset(KB kb, String sessionId) {

        try {
            kb.withUserAssertionLock(() -> {
                java.nio.file.Path sessionDir = com.articulate.sigma.trans.SessionTPTPManager.getSessionDir(sessionId);
                final String kbName = kb.name;
                deleteIfExists(sessionDir.resolve(kbName + KB._userAssertionsString).toFile()); // *_UserAssertions.kif
                deleteIfExists(sessionDir.resolve(kbName + KB._userAssertionsTPTP).toFile());   // *_UserAssertions.tptp
                deleteIfExists(sessionDir.resolve(kbName + KB._userAssertionsTFF).toFile());    // *_UserAssertions.tff
                deleteIfExists(sessionDir.resolve(kbName + KB._userAssertionsTHF).toFile());    // *_UserAssertions.thf
                if (kb.termDepthCache != null) kb.termDepthCache.clear();
                return Boolean.TRUE;
            });
        }
        catch (RuntimeException re) {
            Throwable cause = re.getCause();
            throw re;
        }
        SessionTPTPManager.cleanupSession(sessionId);
    }

    /****************************************************************
     * Delete shared user assertion files.
     * @param kb The knowledge base
     */
    private void deleteSharedUserAssertionFiles(KB kb) {

        final File dir = new File(KBmanager.getMgr().getPref("kbDir"));
        final String kbName = kb.name;
        deleteIfExists(new File(dir, kbName + KB._userAssertionsString)); // *_UserAssertions.kif
        deleteIfExists(new File(dir, kbName + KB._userAssertionsTPTP));   // *_UserAssertions.tptp
        deleteIfExists(new File(dir, kbName + KB._userAssertionsTFF));    // *_UserAssertions.tff
        deleteIfExists(new File(dir, kbName + KB._userAssertionsTHF));    // *_UserAssertions.thf
    }

    /********************************************************************
     * Deletes a file if it exists. 
     * @param f file to delete. 
     */
    private static void deleteIfExists(File f) { if (f.exists() && !f.delete()) System.out.println("WARN resetAllForInference(): failed to delete " + f.getAbsolutePath()); }

    /********************************************************************
     * Populates this test result from an ATP result.
     * @param kb knowledge base used to parse the proof.
     * @param atpResult ATP result to convert into a test result. 
     */
    public void populateResult(KB kb, ATPResult atpResult) {
        
        this.result.execTime = atpResult.getElapsedTimeMs();
        this.result.szsStatus = atpResult.getSzsStatus().getTptpName();
        if (this.result.szsStatus == null) this.result.szsStatus = "See Proof for Details";
        this.result.proof = new ArrayList<>();
        this.result.proof.addAll(atpResult.getStdout());
        this.result.proof.addAll(atpResult.getStderr());
        this.result.proof.add(atpResult.getPrimaryError());
        TPTP3ProofProcessor tpp = atpResult.getParsedProofProcessor(kb, this.query);
        this.result.szsStatus = tpp.status;
        this.result.answers = new ArrayList<>();
        this.result.answers.addAll(tpp.bindings);
        if (tpp.status != null && tpp.status.startsWith("Theorem") && this.result.answers.isEmpty()) this.result.answers.add("yes");
        if (tpp.inconsistency) {
            this.result.contradictionFound = true;
            this.result.answers = new ArrayList<>();
        }
        boolean different = true;
        if (tpp.proof != null && (tpp.status == null || !tpp.status.startsWith("Timeout"))) different = !sameAnswers(tpp, this.expectedAnswers);
        this.result.success = !(different || tpp.noConjecture);
    }

    /********************************************************************
     * Checks whether actual proof answers match expected answers.
     * @param tpp parsed proof processor.
     * @param answerList expected answers.
     * @return true if answers match. 
     */
    private static boolean sameAnswers(TPTP3ProofProcessor tpp, List<String> answerList) {

        if ((tpp == null || tpp.proof.isEmpty()) && (answerList == null || answerList.contains("no"))) return true;
        if (answerList != null && !answerList.isEmpty()) {
            if (answerList.get(0).equals("yes")) return !tpp.proof.isEmpty() && tpp.containsFalse;
            else return sameBindings(tpp.bindings, answerList);
        }
        return false;
    }

    /********************************************************************
     * Checks whether actual bindings match expected bindings.
     * @param actualAnswerList bindings returned by the prover.
     * @param expectedAnswerList expected bindings.
     * @return true if bindings match. 
     */
    private static boolean sameBindings(List<String> actualAnswerList, List<String> expectedAnswerList) {

        if (actualAnswerList == null || actualAnswerList.isEmpty()) return expectedAnswerList == null || expectedAnswerList.isEmpty();
        if (actualAnswerList.size() != expectedAnswerList.size()) return false;
        for (int i = 0; i < actualAnswerList.size(); i++) {
            String actualRes = actualAnswerList.get(i);
            String expectedRes = expectedAnswerList.get(i);
            if (TPTP3ProofProcessor.isSkolemRelation(actualRes)) {
                actualRes = normalizeSkolem(TPTP2SUMO.formToSUMO(actualRes));
                if (!normalizeSkolem(expectedRes).equals(actualRes)) return false;
            }
            else if (!expectedRes.equals(actualRes)) return false;
        }
        return true;
    }

    /********************************************************************
     * Applies test assertions to the KB for the given session.
     * @param kb knowledge base to update.
     * @param sessionId session identifier for assertions.
     * @param language target logical language. 
     */
    public void applyAssertions(KB kb, String sessionId, String language) {

        if (this.tptpRegenRequired) SessionTPTPManager.beginBatchTells(sessionId);
        try {
            for (String statement : this.assertions) if (!StringUtil.emptyString(statement)) kb.tell(statement, sessionId);
        }
        finally {
            if (this.tptpRegenRequired) SessionTPTPManager.endBatchTells(sessionId);
        }
        if (this.tptpRegenRequired) SessionTPTPManager.generateSessionTPTP(sessionId, kb, sessionFileLang(language));
    }

    /********************************************************************
     * Validates parsed test metadata and required constituents, adds errors to this.errors. 
     */
    public void validateTestData() {
        
        if (this.assertions.isEmpty()) LoggingUtils.log("WARN", "No assertions provided!");
        if (!this.minLang.equals("fof") && !minLang.equals("tff") && !minLang.equals("thf")) errors.add("Invalid minLang!: " + this.minLang);
        if (this.timeout < 0) errors.add("Invalid timeout!: " + this.timeout);
        if (StringUtil.emptyString(this.expectedAnswers)) errors.add("No expected answers provided!");
        if (StringUtil.emptyString(this.query)) errors.add("INVALID QUERY!: " + this.query);
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        for (String constituent : this.requiredConstituents) {
            boolean found = false;
            for (String loaded : kb.constituents) {
                if (new File(loaded).getName().equals(constituent)) {
                    found = true;
                    break;
                }
            }
            if (!found) errors.add("Required constituent " + constituent + " not loaded!");
        }
        for (String error : errors) LoggingUtils.log("ERROR", error);
    }

    /********************************************************************
     * Reads and parses this test's .tq file. 
     */
    public void readTestFile() {

        File testFile = new File(this.filePath);
        if (!isValidTestPath(testFile)) return;
        try {
            KIF testKif = new KIF();
            testKif.readFile(testFile.getCanonicalPath());
            for (Formula orderedFormulas : testKif.formulasOrdered.values()) {
                String formula = orderedFormulas.getFormula();
                if (formula.contains(";")) formula = formula.substring(0, formula.indexOf(";"));
                if (formula.startsWith("(file")) this.requiredConstituents.add(formula.substring(6, formula.length() - 1));
                else if (formula.startsWith("(minLang")) this.minLang = formula.substring(9, formula.length() - 1).trim().toLowerCase();
                else if (formula.startsWith("(regen")) this.tptpRegenRequired = formula.substring(7, formula.length() - 1).trim().equals("true");
                else if (formula.startsWith("(note")) this.note = formula.substring(6, formula.length() - 1);
                else if (formula.startsWith("(category")) this.category = formula.substring(10, formula.length() - 1);
                else if (formula.startsWith("(time")) this.timeout = Integer.parseInt(formula.substring(6, formula.length() - 1));
                else if (formula.startsWith("(query")) this.query = formula.substring(7, formula.length() - 1);
                else if (formula.startsWith("(answer")) parseAnswersFromMetaPredicate(formula);
                else this.assertions.add(formula);
            }
        }
        catch (Exception e) {
            LoggingUtils.log("ERROR", "Failed reading test file: " + this.filePath);
            e.printStackTrace();
        }
    }

    /********************************************************************
     * Parses expected answers from the answer meta-predicate.
     * @param answerString answer formula to parse. 
     */
    public void parseAnswersFromMetaPredicate(String answerString) {

        if (answerString.equals("yes") || answerString.equals("no")) this.expectedAnswers.add(answerString);
        else {
            Formula ansForm = new Formula(answerString);
            List<String> answers = ansForm.complexArgumentsToArrayListString(1);
            for (String a : answers) {
                if (TPTP3ProofProcessor.isSkolemRelation(a)) a = normalizeSkolem(a);
                this.expectedAnswers.add(a);
            }
        }
    }

    /********************************************************************
     * Checks whether the test file exists and has a .tq extension.
     * @param testFile test file to validate.
     * @return true if the file is a valid test path. 
     */
    public boolean isValidTestPath(File testFile) {

        try {
            boolean valid = testFile.exists() && testFile.getCanonicalPath().endsWith(".tq");
            if (!valid) LoggingUtils.log("ERROR", filePath + " is not a valid .tq file!");
            return valid;
        }
        catch (Exception e) {
            LoggingUtils.log("ERROR", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /********************************************************************
     * Normalizes skolem terms in a string.
     * @param s string containing skolem terms. 
     * @return string with normalized skolem terms. 
     */
    public static String normalizeSkolem(String s) { return s.replaceAll("sK[0-9 ]+","sK1").replaceAll("esk\\d+","esk1"); }

    /********************************************************************
     * Convert ATP language name to the session TPTP file extension.
     * @param language ATP language: FOF, TFF, THF
     * @return file extension used by SessionTPTPManager
     */
    private static String sessionFileLang(String language) {

        if (StringUtil.emptyString(language)) return "tptp";
        else if ("FOF".equalsIgnoreCase(language) || "TPTP".equalsIgnoreCase(language)) return "tptp";
        else if ("TFF".equalsIgnoreCase(language)) return "tff";
        else if ("THF".equalsIgnoreCase(language)) return "thf";
        return language.toLowerCase();
    }

    /********************************************************************
     * Prints this test's result summary. 
     */
    public void printResult() {

        System.out.println("====================================");
        System.out.println("Inference Test Results");
        System.out.println("    File:       " + this.filePath);
        System.out.println("    Note:       " + this.note);
        System.out.println("    Query:      " + this.query);
        System.out.println("    Timeout:    " + this.timeout + " seconds");
        if (this.result == null) {
            System.out.println("Result:     null");
            System.out.println("====================================");
            return;
        }
        System.out.println("Success:    " + this.result.success);
        System.out.println("SZS:        " + this.result.szsStatus);
        System.out.println("Expected:   " + this.expectedAnswers);
        System.out.println("Actual:     " + this.result.answers);
        System.out.println("Exec time:  " + this.result.execTime + " ms");
        System.out.println("Contradiction: " + this.result.contradictionFound);
        System.out.println("====================================");
    }

    /********************************************************************
     * Prints command line usage options. 
     */
    private static void printHelp() {

        System.out.println("InferenceTest.main():");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  --r <pathToTest> run this test");
    }

    /********************************************************************
     * Runs an inference test from the command line.
     * @param args command line arguments. 
     */
    public static void main (String[] args) throws Exception {

        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        if (argMap.isEmpty() || argMap.containsKey("h")) {
            printHelp();
            return;
        }
        if(argMap.containsKey("r") && argMap.get("r").size() == 1) {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            InferenceTest test = new InferenceTest(argMap.get("r").get(0));
            test.runTest(kb, "VAMPIRE", false, false, false, false);
            test.printResult();
        }
    }
}