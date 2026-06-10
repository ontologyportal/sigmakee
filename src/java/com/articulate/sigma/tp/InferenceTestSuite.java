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

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.parsing.CLIMapParser;
import com.articulate.sigma.utils.LoggingUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.Collections;

/** Manages loading, grouping, and running inference tests for a KB. */
public class InferenceTestSuite {

    /** Debug logging level. */
    public int debug = 0;
    /** Directory containing inference test files. */
    private final String inferenceTestDir = KBmanager.getMgr().getPref("inferenceTestDir");
    /** Loaded inference tests keyed by test path. */
    private final Map<String, InferenceTest> inferenceTests = new LinkedHashMap<>();
    /** Loaded inference tests grouped by category. */
    private final Map<String, List<InferenceTest>> inferenceTestCategories = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    /** Knowledge base used to run inference tests. */
    public KB kb;

    /********************************************************************
     * Creates an inference test suite for the given KB and loads all tests. 
     * @param kb knowledge base used to run inference tests. 
     */
    public InferenceTestSuite(KB kb) {
        this.kb = kb;
        loadAllInferenceTests();
    }

    /********************************************************************
     * Returns all loaded inference tests by test path. 
     * @return unmodifiable map of test paths to inference tests. 
     */
    public Map<String, InferenceTest> getInferenceTests() { return Collections.unmodifiableMap(this.inferenceTests); }

    /********************************************************************
     * Clears stored results for all loaded inference tests. 
     */
    public void clearAllTestResults() { for (InferenceTest test : this.inferenceTests.values()) test.result = null; }

    /******************************************************************** 
     * Runs a single inference test with options from the tests meta-predicates. 
     * @param testPath path of the test to run.
     * @param proverType prover to use.
     * @param closedWorldAssumption whether to use the closed world assumption.
     * @param modusPonens whether to enable modus ponens.
     * @param dropOnePremise whether to drop one premise during inference.
     * @param holUseModals whether HOL modal translation is enabled. 
     */
    public void runTest(String testPath, String proverType, boolean closedWorldAssumption, boolean modusPonens, boolean dropOnePremise, boolean holUseModals) {
        
        this.inferenceTests.get(testPath).runTest(this.kb, proverType, closedWorldAssumption, modusPonens, dropOnePremise, holUseModals);
    }

    /********************************************************************
     * Runs a single inference test overriding the test's meta-predicate options. 
     * @param testPath path of the test to run. 
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
    public void runTestOverload(String testPath, String proverType, String language, String vampireMode, boolean closedWorldAssumption, boolean modusPonens, boolean dropOnePremise, boolean holUseModals, int timeout, int maxAnswers) {
        
        this.inferenceTests.get(testPath).runTest(this.kb, proverType, language, vampireMode, closedWorldAssumption, modusPonens, dropOnePremise, holUseModals, timeout, maxAnswers);
    }

    /******************************************************************** 
     * Runs all loaded inference tests with the given prover options. 
     * @param proverType prover to use.
     * @param closedWorldAssumption whether to use the closed world assumption.
     * @param modusPonens whether to enable modus ponens. 
     * @param dropOnePremise whether to drop one premise during inference. 
     * @param holUseModals whether HOL modal translation is enabled. 
     */
    public void runAllTests(String proverType, boolean closedWorldAssumption, boolean modusPonens, boolean dropOnePremise, boolean holUseModals) {

        for (Map.Entry<String, InferenceTest> entry : this.inferenceTests.entrySet()) {
            String testPath = entry.getKey();
            InferenceTest test = entry.getValue();
            try {
                if (debug > 0) LoggingUtils.log("Running test: " + testPath);
                test.runTest(this.kb, proverType, closedWorldAssumption, modusPonens, dropOnePremise, holUseModals);
                if (debug > 0) test.printResult();
            }
            catch (Exception e) {
                LoggingUtils.log("ERROR", "Test failed with exception: " + testPath);
                LoggingUtils.log("ERROR", e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
                test.printResult();
            }
        }
    }

    /********************************************************************
     * Runs all inference tests containing the specified meta-predicate category. 
     * @param category category of tests to run. 
     * @param proverType prover to use. 
     * @param closedWorldAssumption whether to use the closed world assumption. 
     * @param modusPonens whether to enable modus ponens. 
     * @param dropOnePremise whether to drop one premise during inference. 
     * @param holUseModals whether HOL modal translation is enabled. 
     */
    public void runTestsInCategory(String category, String proverType, boolean closedWorldAssumption, boolean modusPonens, boolean dropOnePremise, boolean holUseModals) {

        List<InferenceTest> tests = this.inferenceTestCategories.get(category);
        if (tests == null || tests.isEmpty()) {
            LoggingUtils.log("WARN", "No inference tests found for category: " + category);
            return;
        }
        for (InferenceTest test : tests) {
            if (debug > 0) LoggingUtils.log("Running test: " + test.filePath);
            test.runTest(this.kb, proverType, closedWorldAssumption, modusPonens, dropOnePremise, holUseModals);
            if (debug > 0) test.printResult();
        }
    }

    /********************************************************************
     * Loads all .tq inference tests from the configured test directory. 
     */
    private void loadAllInferenceTests() {

        Path root = Paths.get(this.inferenceTestDir).toAbsolutePath().normalize();
        try (Stream<Path> paths = Files.walk(root)) {
            paths
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".tq"))
                .map(p -> p.toAbsolutePath().normalize().toString().replace(File.separatorChar, '/'))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(testPath -> {
                    InferenceTest test = new InferenceTest(testPath);
                    this.inferenceTests.put(testPath, test);
                    if (test.category == null || test.category.isBlank()) test.category = "Uncategorized";
                    this.inferenceTestCategories
                        .computeIfAbsent(test.category, k -> new ArrayList<>())
                        .add(test);
                });
        }
        catch (IOException e) {
            System.err.println("Error in ITS.loadAllInferenceTests(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /********************************************************************
     * Prints command line usage options. 
     */
    private static void printHelp() {

        System.out.println("InferenceTestSuite.main() options:");
        System.out.println("    -r              - run all tests");
        System.out.println("    -p              - print all test paths");
        System.out.println("    -pc             - print test paths by category");
        System.out.println("    --rc <category> - run all tests in category");
        System.out.println("    --r <path>      - run single test");
    }

    /********************************************************************
     * Runs the inference test suite from the command line. 
     * @param args command line arguments. 
     */
    public static void main(String args[]) {

        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        if (argMap.isEmpty() || argMap.containsKey("h")) {
            printHelp();
            return;
        }
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        InferenceTestSuite inferenceTestSuite = new InferenceTestSuite(kb);
        if(argMap.containsKey("r")) {
            LoggingUtils.log("Running all " + inferenceTestSuite.inferenceTests.size() + " inference tests!");
            inferenceTestSuite.runAllTests("VAMPIRE", false, false, false, false);
        }
        else if(argMap.containsKey("p")) {
            System.out.println("Printing " + inferenceTestSuite.inferenceTests.size() + " test paths!");
            for (Map.Entry<String, InferenceTest> entry : inferenceTestSuite.inferenceTests.entrySet()) System.out.println(entry.getKey());
        }
        else if(argMap.containsKey("pc")) {
            for (Map.Entry<String, List<InferenceTest>> entry : inferenceTestSuite.inferenceTestCategories.entrySet()) {
                System.out.println("Category: " + entry.getKey());
                for (InferenceTest test : entry.getValue()) System.out.println("    " + test.filePath);
            }
        }
    }
}
