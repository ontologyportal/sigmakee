package com.articulate.sigma.inference;

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class InferenceTest {

    private static final String SIGMA_SRC = System.getenv("SIGMA_SRC");

    private static KB kb;

    @Parameterized.Parameter(value = 0)
    public String fInput;

    /** ***************************************************************
     * Initializes the KB used by all parameterized inference tests.
     */
    @BeforeClass
    public static void setKB() {

        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        List<String> reqFiles = Arrays.asList("Merge.kif", "Mid-level-ontology.kif");
        for (String s : reqFiles) {
            if (!kb.containsFile(s)) {
                fail("Required file missing: " + s);
            }
        }
    }

    /** ***************************************************************
     * Returns all inference test files to run.
     */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> prepare() {

        String testDataDirectoryPath = SIGMA_SRC + File.separator +
                "test/corpus/java/resources/InferenceTestData";

        boolean enableIncludeTestsList = false;
        boolean enableExcludeTestsList = false;

        List<String> includeTestsList = Lists.newArrayList("QA1");
        List<String> excludeTestsList = Lists.newArrayList("TQG2", "TQG4", "TQG10");

        return getTestFiles(testDataDirectoryPath, includeTestsList, enableIncludeTestsList,
                excludeTestsList, enableExcludeTestsList);
    }

    /** ***************************************************************
     * Gets parameterized .kif.tq test files from the given directory.
     */
    public static Collection<Object[]> getTestFiles(String testDataDirectoryPath,
                                                    List<String> includeTestsList,
                                                    boolean enableIncludeTestsList,
                                                    List<String> excludeTestsList,
                                                    boolean enableExcludeTestsList) {

        Collection<Object[]> result = Lists.newArrayList();
        File folder = new File(testDataDirectoryPath);
        File[] files = folder.listFiles();

        assertNotNull("Inference test directory not found: " + testDataDirectoryPath, files);

        try {
            for (File fileEntry : files) {
                if (!fileEntry.getName().endsWith(".kif.tq"))
                    continue;

                String filename = fileEntry.getName().substring(0, fileEntry.getName().indexOf(".kif.tq"));
                boolean included = !enableIncludeTestsList || includeTestsList.contains(filename);
                boolean excluded = enableExcludeTestsList && excludeTestsList.contains(filename);

                if (included && !excluded)
                    result.add(new Object[]{fileEntry.getCanonicalPath()});
            }
        }
        catch (IOException e) {
            fail("Error reading inference tests from " + testDataDirectoryPath + ": " + e.getMessage());
        }

        return result;
    }

    /** ***************************************************************
     * Runs one .tq inference test and checks its result.
     */
    @Test
    public void test() {

        System.out.println("InferenceTest.test(): " + fInput);

        com.articulate.sigma.tp.InferenceTest test =
                new com.articulate.sigma.tp.InferenceTest(fInput);

        assertTrue("Invalid test data in " + fInput + ": " + test.errors,
                test.errors == null || test.errors.isEmpty());

        test.runTest(kb, "VAMPIRE", false, false, false, false);

        assertNotNull("No result produced for " + fInput, test.result);

        System.out.println("expected: " + test.expectedAnswers);
        System.out.println("actual: " + test.result.answers);

        if (test.result.contradictionFound)
            System.err.println("Failure (**inconsistent**) in " + fInput);
        else if (test.result.success)
            System.out.println("Success in " + fInput);
        else
            System.err.println("Failure in " + fInput);

        System.out.println("\n\n");

        assertFalse("Contradiction found in " + fInput, test.result.contradictionFound);
        assertTrue("Expected " + test.expectedAnswers + " but got " + test.result.answers + " in " + fInput,
                test.result.success);
    }
}