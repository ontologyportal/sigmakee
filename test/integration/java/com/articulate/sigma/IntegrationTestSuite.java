package com.articulate.sigma;

import com.articulate.sigma.VerbNet.IntegrationVerbNetTestSuite;
import com.articulate.sigma.nlg.IntegrationNLGTestSuite;
import com.articulate.sigma.trans.IntegrationTransTestSuite;
import org.junit.AfterClass;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    IntegrationSigmaTestSuite.class,
    IntegrationVerbNetTestSuite.class,
    IntegrationNLGTestSuite.class,
    IntegrationTransTestSuite.class
})
public class IntegrationTestSuite extends IntegrationTestBase {

    @AfterClass
    public static void shutDown() {
        KButilities.shutDownExecutorService();
    }
}