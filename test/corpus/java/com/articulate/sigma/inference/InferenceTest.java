package com.articulate.sigma.inference;

import com.articulate.sigma.InferenceTestSuite;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * Created by qingqingcai on 3/9/15.
 */
public class InferenceTest {

    private static KB kb;

    @BeforeClass
    public static void setKB() {

        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB("SUMO");
    }

    @Test
    public void testTQG1() {
        String testpath = "test/corpus/java/resources/InferenceTestData/TQG1.kif.tq";
        ArrayList<String> expectedAnswers = new ArrayList<>();
        ArrayList<String> actualAnswers = new ArrayList<>();
        System.out.println(kb.eprover);
        InferenceTestSuite.inferenceUnitTest(testpath, kb, expectedAnswers, actualAnswers);
        assertEquals(expectedAnswers, actualAnswers);
    }

    @Test
    public void testTQG5() {
        String testpath = "test/corpus/java/resources/InferenceTestData/TQG5.kif.tq";
        ArrayList<String> expectedAnswers = new ArrayList<>();
        ArrayList<String> actualAnswers = new ArrayList<>();
        InferenceTestSuite.inferenceUnitTest(testpath, kb, expectedAnswers, actualAnswers);
        assertEquals(expectedAnswers, actualAnswers);
    }

    @Test
    public void testTQG8() {
        String testpath = "test/corpus/java/resources/InferenceTestData/TQG8.kif.tq";
        ArrayList<String> expectedAnswers = new ArrayList<>();
        ArrayList<String> actualAnswers = new ArrayList<>();
        InferenceTestSuite.inferenceUnitTest(testpath, kb, expectedAnswers, actualAnswers);
        assertEquals(expectedAnswers, actualAnswers);
    }

    @Test
    public void testTQG9() {
        String testpath = "test/corpus/java/resources/InferenceTestData/TQG9.kif.tq";
        ArrayList<String> expectedAnswers = new ArrayList<>();
        ArrayList<String> actualAnswers = new ArrayList<>();
        InferenceTestSuite.inferenceUnitTest(testpath, kb, expectedAnswers, actualAnswers);
        assertEquals(expectedAnswers, actualAnswers);
    }
}
