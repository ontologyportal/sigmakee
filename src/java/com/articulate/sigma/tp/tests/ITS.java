package com.articulate.sigma.tp.tests;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.parsing.CLIMapParser;
import com.articulate.sigma.utils.LoggingUtils;

public class ITS {
    private final String inferenceTestDir = KBmanager.getMgr().getPref("inferenceTestDir");
    private final Map<String, InferenceTest> inferenceTests = new LinkedHashMap<>();
    private final Map<String, List<InferenceTest>> inferenceTestCategories = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    public KB kb;

    public ITS(KB kb) {
        this.kb = kb;
        loadAllInferenceTests();
    }

    public void runAllTests(String proverType, boolean closedWorldAssumption, boolean modusPonens, boolean dropOnePremise, boolean holUseModals) {

        for (Map.Entry<String, InferenceTest> entry : this.inferenceTests.entrySet()) {
            String testPath = entry.getKey();
            InferenceTest test = entry.getValue();
            LoggingUtils.log("Running test: " + testPath);
            try {
                test.runTest(this.kb, proverType, closedWorldAssumption, modusPonens, dropOnePremise, holUseModals);
                test.printResult();
                LoggingUtils.log("Test complete!");
            }
            catch (Exception e) {
                LoggingUtils.log("ERROR", "Test failed with exception: " + testPath);
                LoggingUtils.log("ERROR", e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
                test.printResult();
            }
        }
    }

    public void runTest(String testPath, String proverType, boolean closedWorldAssumption, boolean modusPonens, boolean dropOnePremise, boolean holUseModals) {
        
        this.inferenceTests.get(testPath).runTest(this.kb, proverType, closedWorldAssumption, modusPonens, dropOnePremise, holUseModals);
    }

    public void runTestOverload(String testPath, String proverType, String language, String vampireMode, boolean closedWorldAssumption, boolean modusPonens, boolean dropOnePremise, boolean holUseModals, int timeout, int maxAnswers) {
        
        this.inferenceTests.get(testPath).runTest(this.kb, proverType, language, vampireMode, closedWorldAssumption, modusPonens, dropOnePremise, holUseModals, timeout, maxAnswers);
    }

    private void printAllTestPaths() {

        System.out.println("Printing " + this.inferenceTests.size() + " test paths!");
        for (Map.Entry<String, InferenceTest> entry : this.inferenceTests.entrySet()) System.out.println(entry.getKey());
    }

    private void printAllTestPathsByCategory() {

        for (Map.Entry<String, List<InferenceTest>> entry : this.inferenceTestCategories.entrySet()) {
            System.out.println("Category: " + entry.getKey());
            for (InferenceTest test : entry.getValue()) System.out.println("    " + test.filePath);
        }
    }

    public void runTestsInCategory(String category, String proverType, boolean closedWorldAssumption, boolean modusPonens, boolean dropOnePremise, boolean holUseModals) {

        List<InferenceTest> tests = this.inferenceTestCategories.get(category);
        if (tests == null || tests.isEmpty()) {
            LoggingUtils.log("WARN", "No inference tests found for category: " + category);
            return;
        }
        for (InferenceTest test : tests) {
            LoggingUtils.log("Running test: " + test.filePath);
            test.runTest(this.kb, proverType, closedWorldAssumption, modusPonens, dropOnePremise, holUseModals);
            test.printResult();
            LoggingUtils.log("Test complete!");
        }
    }

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

    private static void printHelp() {
        System.out.println("InferenceTestSuite.main() options:");
        System.out.println("    -r              - run all tests");
        System.out.println("    -p              - print all test paths");
        System.out.println("    -pc             - print test paths by category");
        System.out.println("    --rc <category> - run all tests in category");
        System.out.println("    --r <path>      - run single test");
    }

    public static void main(String args[]) {
        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        if (argMap.isEmpty() || argMap.containsKey("h")) {
            printHelp();
            return;
        }
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        ITS inferenceTestSuite = new ITS(kb);
        if(argMap.containsKey("r")) {
            LoggingUtils.log("Running all " + inferenceTestSuite.inferenceTests.size() + " inference tests!");
            inferenceTestSuite.runAllTests("VAMPIRE", false, false, false, false);
        }
        if(argMap.containsKey("p")) {
            inferenceTestSuite.printAllTestPaths();
        }
    }
}