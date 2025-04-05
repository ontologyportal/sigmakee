package com.articulate.sigma.inference;

import com.articulate.sigma.InferenceTestSuite;
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

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class InferenceTest {

    private static final String SIGMA_SRC = System.getenv("SIGMA_SRC");

    private static KB kb;

    @Parameterized.Parameter(value= 0)
    public String fInput;

    /** ***************************************************************
     */
    @BeforeClass
    public static void setKB() {

        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        List<String> reqFiles =
                Arrays.asList("Merge.kif", "Mid-level-ontology.kif");
        for (String s : reqFiles) {
            if (!kb.containsFile(s)) {
                System.err.println("Error in InferenceTest.setKB(): required file " + s + " missing");
                System.exit(-1);
            }
        }
    }

    /** ***************************************************************
     */
    @Parameterized.Parameters(name="{0}")
    public static <T> Collection<T> prepare() {

        String testDataDirectoryPath = SIGMA_SRC + File.separator + "test/corpus/java/resources/InferenceTestData";
        boolean enableIncludeTestsList = false;   // If enableIncludeTestsList=true, only run test files in includeTestsList
        boolean enableExcludeTestsList = false;   // If enableIncludeTestsList=false & enableExcludeTestsList=true, only run test files NOT in excludeTestsLists
                                                  // If enableIncludeTestsList=false & enableExcludeTestsList=false, run all test files in InferenceTestData
        List<String> includeTestsList = Lists.newArrayList("QA1");
        List<String> excludeTestsList = Lists.newArrayList("TQG2", "TQG4", "TQG10");
        return getTestFiles(testDataDirectoryPath, includeTestsList, enableIncludeTestsList,
                excludeTestsList, enableExcludeTestsList);
    }

    /** ***************************************************************
     */
    public static <T> Collection<T> getTestFiles(String testDataDirectoryPath,
                  List<String> includeTestsList, boolean enableIncludeTestsList,
                  List<String> excludeTestsList, boolean enableExcludeTestsList) {

        Collection<T> result = Lists.newArrayList();
        File folder = new File(testDataDirectoryPath);
        try {
            for (File fileEntry : folder.listFiles()) {
                if (fileEntry.getName().endsWith(".kif.tq")) {
                    String filename = fileEntry.getName().substring(0, fileEntry.getName().indexOf(".kif.tq"));
                    if (enableIncludeTestsList) {       // only consider files in includeTestsList
                        if (includeTestsList.contains(filename)) {
                            String path = fileEntry.getCanonicalPath();
                            result.add((T) new Object[]{path});
                        }
                    }
                    else if (enableExcludeTestsList) {  // only consider files NOT in excludeTestsList
                        if (!excludeTestsList.contains(filename)) {
                            String path = fileEntry.getCanonicalPath();
                            result.add((T) new Object[]{path});
                        }
                    }
                    else {                              // consider all files in InferenceTestData directory
                        String path = fileEntry.getCanonicalPath();
                        result.add((T) new Object[]{path});
                    }
                }
            }
        }
        catch (IOException e) {
            System.err.println("Error in InferenceTest.getTestFiles(): using path: " + testDataDirectoryPath);
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
     */
    @Test
    public void test() {

        System.out.println("InferenceTest.test(): " + fInput);
        InferenceTestSuite its = new InferenceTestSuite();
        InferenceTestSuite.InfTestData itd = its.inferenceUnitTest(fInput,kb);
        System.out.println("expected: " + itd.expectedAnswers);
        System.out.println("actual: " + itd.actualAnswers);
        if (itd.inconsistent)
            System.err.println("Failure (**inconsistent**) in " + fInput);
        else if (itd.expectedAnswers.equals(itd.actualAnswers))
            System.out.println("Success in " + fInput);
        else
            System.err.println("Failure in " + fInput);
        System.out.println("\n\n");
        assertEquals(itd.expectedAnswers, itd.actualAnswers);
    }
}
