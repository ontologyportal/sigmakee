package com.articulate.sigma;

import com.articulate.sigma.inference.InferenceTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        InferenceTest.class,
})
public class CorpusTestSuite extends IntegrationTestBase {

}