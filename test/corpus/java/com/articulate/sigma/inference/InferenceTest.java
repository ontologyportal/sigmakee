package com.articulate.sigma.inference;

import com.articulate.sigma.InferenceTestSuite;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.google.common.collect.Lists;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class InferenceTest {

    private static KB kb;

    @Parameterized.Parameter(value= 0)
    public String fInput;

    /** ***************************************************************
     */
    @BeforeClass
    public static void setKB() {

        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB("SUMO");
    }

    /** ***************************************************************
     */
    @Parameterized.Parameters(name="{0}")
    public static <T> Collection<T> prepare() {

        return getTestFiles("test/corpus/java/resources/InferenceTestData");
    }

    /** ***************************************************************
     */
    public static <T> Collection<T> getTestFiles(String testDataDirectoryPath) {

        Collection<T> result = Lists.newArrayList();
        File folder = new File(testDataDirectoryPath);
        try {
            for (File fileEntry : folder.listFiles()) {
                if (fileEntry.getName().endsWith(".kif.tq")) {
                    String path = fileEntry.getCanonicalPath();
                    result.add((T) new Object[]{path});
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /** ***************************************************************
     */
    @Test
    public void test() {

        System.out.println(fInput);
        ArrayList<String> expectedAnswers = new ArrayList<>();
        ArrayList<String> actualAnswers = new ArrayList<>();
        InferenceTestSuite.inferenceUnitTest(fInput, kb, expectedAnswers, actualAnswers);
        assertEquals(expectedAnswers, actualAnswers);
    }
}
