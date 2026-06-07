package com.articulate.sigma.tp.tests;

import java.util.List;
import java.io.File;
import java.io.IOException;

import com.articulate.sigma.*;
import com.articulate.sigma.trans.*;
import com.articulate.sigma.tp.*;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.utils.LoggingUtils;
import com.articulate.sigma.parsing.CLIMapParser;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.*;

public class InferenceTest {

    private String filePath;
    private String minLang = "fof";
    private boolean tptpRegenRequired = false;
    private String note;
    private int timeout = 30;
    private String query;
    private List<String> assertions = new ArrayList<>();
    private List<String> requiredConstituents = new ArrayList<>();
    private List<String> expectedAnswers = new ArrayList<>();
    public InferenceTestResult result = new InferenceTestResult();

    public class InferenceTestResult {
        public boolean success;
        public float execTime;
        public String szsStatus;
        public List<String> proof = new ArrayList<>();
        public List<String> answers = new ArrayList<>();
        public boolean contradictionFound;
    }

    public InferenceTest(String filePath) {
        this.filePath = filePath;
        readTestFile();
        validateTestData();
    }

    public void runTest(KB kb, String proverType, boolean closedWorldAssumption, boolean modusPonens, boolean dropOnePremise, boolean holUseModals) {
        
        runTest(kb, proverType, this.minLang, "CASC", closedWorldAssumption, modusPonens, dropOnePremise, holUseModals, this.timeout, this.expectedAnswers.size());
    }

    public void runTest(KB kb, String proverType, String language, String vampireMode, boolean closedWorldAssumption, boolean modusPonens, boolean dropOnePremise, boolean holUseModals, int timeout, int maxAnswers) {
        String sessionId = "tq-" + UUID.randomUUID();
        TheoremProverController tpc = new TheoremProverController();
        try {
            TPTPGenerationManager.waitForAllTPTP(600);
            applyAssertions(kb, sessionId, language);
            populateResult(
                kb,
                tpc.runQuery(
                    kb,
                    sessionId,
                    this.query,
                    this.filePath,
                    "TEST_FILE",
                    proverType,
                    language,
                    vampireMode,
                    closedWorldAssumption,
                    modusPonens,
                    dropOnePremise,
                    holUseModals,
                    timeout,
                    maxAnswers
                )
            );
        }
        finally {
            reset(kb, sessionId);
        }
    }

        /****************************************************************
     * Undo all parts of the state that have anything to do with user assertions made during inference.
     * Session-aware version for isolated TQ test handling.
     *
     * @param kb The knowledge base
     * @param sessionId Optional HTTP session ID for session-specific cleanup.
     *                  If null or empty, uses shared UA files.
     */
    public void reset(KB kb, String sessionId) {

        boolean sessionMode = sessionId != null && !sessionId.isEmpty();
        try {
            kb.withUserAssertionLock(() -> {
                if (sessionMode) {
                    java.nio.file.Path sessionDir = com.articulate.sigma.trans.SessionTPTPManager.getSessionDir(sessionId);
                    final String kbName = kb.name;
                    deleteIfExists(sessionDir.resolve(kbName + KB._userAssertionsString).toFile()); // *_UserAssertions.kif
                    deleteIfExists(sessionDir.resolve(kbName + KB._userAssertionsTPTP).toFile());   // *_UserAssertions.tptp
                    deleteIfExists(sessionDir.resolve(kbName + KB._userAssertionsTFF).toFile());    // *_UserAssertions.tff
                    deleteIfExists(sessionDir.resolve(kbName + KB._userAssertionsTHF).toFile());    // *_UserAssertions.thf
                    if (kb.termDepthCache != null) kb.termDepthCache.clear();
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
            throw re;
        }
        SessionTPTPManager.cleanupSession(sessionId);
    }

    /****************************************************************
     * Delete shared user assertion files (original behavior).
     *
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

    /****************************************************************
     */
    private static void deleteIfExists(File f) {
        if (f.exists() && !f.delete()) {
            System.out.println("WARN resetAllForInference(): failed to delete " + f.getAbsolutePath());
        }
    }

    public void populateResult(KB kb, ATPResult atpResult) {
        
        this.result.execTime = atpResult.getElapsedTimeMs();
        this.result.szsStatus = atpResult.getSzsStatus().getTptpName();
        // else if (atpResult.getSzsStatusRaw() != null)
        //     itd.SZSstatus = atpResult.getSzsStatusRaw();
        // else
        //     itd.SZSstatus = "Unknown";
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

    private static boolean sameAnswers(TPTP3ProofProcessor tpp, List<String> answerList) {

        if ((tpp == null || tpp.proof.isEmpty()) && (answerList == null || answerList.contains("no"))) return true;
        if (answerList != null && !answerList.isEmpty()) {
            if (answerList.get(0).equals("yes")) return !tpp.proof.isEmpty() && tpp.containsFalse;
            else return sameBindings(tpp.bindings, answerList);
        }
        return false;
    }

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

    public void validateTestData() {

        if (!this.minLang.equals("fof") && !minLang.equals("tff") && !minLang.equals("thf")) LoggingUtils.log("ERROR", "Invalid minLang!: " + this.minLang);
        if (this.timeout < 0) LoggingUtils.log("ERROR", "Invalid timeout!: " + this.timeout);
        if (StringUtil.emptyString(this.expectedAnswers)) LoggingUtils.log("ERROR", "No expected answers provided!");
        if (this.assertions.isEmpty()) LoggingUtils.log("WARN", "No assertions provided!");
        if (StringUtil.emptyString(this.query)) LoggingUtils.log("ERROR", "INVALID QUERY!: " + this.query);
    }

    public void readTestFile() {

        File testFile = new File(this.filePath);
        if (!isValidTestPath(testFile)) return;
        try {
            KIF testKif = new KIF();
            testKif.readFile(testFile.getCanonicalPath());
            System.out.println("Forms=" + testKif.formulasOrdered.values());
            for (Formula orderedFormulas : testKif.formulasOrdered.values()) {
                String formula = orderedFormulas.getFormula();
                if (formula.contains(";")) formula = formula.substring(0, formula.indexOf(";"));
                if (formula.startsWith("(file")) this.requiredConstituents.add(formula.substring(6, formula.length() - 1));
                else if (formula.startsWith("(minLang")) this.minLang = formula.substring(9, formula.length() - 1).trim().toLowerCase();
                else if (formula.startsWith("(regen")) this.tptpRegenRequired = formula.substring(7, formula.length() - 1).trim().equals("true");
                else if (formula.startsWith("(note")) this.note = formula.substring(6, formula.length() - 1);
                else if (formula.startsWith("(time")) this.timeout = Integer.parseInt(formula.substring(6, formula.length() - 1));
                else if (formula.startsWith("(query")) this.query = formula.substring(7, formula.length() - 1);
                else if (formula.startsWith("(answer")) parseAnswers(formula);
                else this.assertions.add(formula);
            }
        }
        catch (Exception e) {
            LoggingUtils.log("ERROR", "Failed reading test file: " + this.filePath);
            e.printStackTrace();
        }
    }

    public void parseAnswers(String s) {

        String answerstring = s;
        if (answerstring.equals("yes") || answerstring.equals("no")) this.expectedAnswers.add(answerstring);
        else {
            Formula ansForm = new Formula(answerstring);
            List<String> answers = ansForm.complexArgumentsToArrayListString(1);
            for (String a : answers) {
                if (TPTP3ProofProcessor.isSkolemRelation(a)) a = normalizeSkolem(a);
                this.expectedAnswers.add(a);
            }
        }
    }

    public boolean isValidTestPath(File testFile) {

        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(Paths.get(filePath))) {
                LoggingUtils.log("ERROR", filePath + "does not exist!");
                return false;
            }
            if (testFile.getCanonicalPath().endsWith(".tq")) return true;
            return false;
        }
        catch (Exception e) {
            LoggingUtils.log("ERROR", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /** ***************************************************************
     * skolem terms in a string are converted to 'sK1'
     */
    public static String normalizeSkolem(String s) {

        return s.replaceAll("sK[0-9 ]+","sK1").replaceAll("esk\\d+","esk1");
    }

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

    public void printResult() {

        System.out.println();
        System.out.println("====================================");
        System.out.println("Inference Test Result");
        System.out.println("====================================");
        System.out.println("File:       " + this.filePath);
        System.out.println("Note:       " + this.note);
        System.out.println("Query:      " + this.query);
        System.out.println("Timeout:    " + this.timeout + " seconds");
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
        if (this.result.proof != null && !this.result.proof.isEmpty()) {
            System.out.println();
            System.out.println("---- Proof / Prover Output ----");
            for (String line : this.result.proof) {
                if (!StringUtil.emptyString(line))
                    System.out.println(line);
            }
        }
        System.out.println("====================================");
    }

    private static void printHelp() {
        System.out.println("InferenceTest.main():");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  --r <pathToTest> run this test");
    }

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