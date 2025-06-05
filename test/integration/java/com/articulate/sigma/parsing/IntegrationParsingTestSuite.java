package com.articulate.sigma.parsing;

import com.articulate.sigma.IntegrationTestBase;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author <a href="mailto:terry.norbraten@gmail.com?subject=com.articulate.sigma.parsing.IntegrationParsingTestSuite">Terry Norbraten</a>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    PredVarInstTest.class,
    PreprocessorTest.class,
    RowVarTest.class,
    SortalTest.class,
    TPTPWriterTest.class,
    TypeTest.class
})
public class IntegrationParsingTestSuite extends IntegrationTestBase {

} // end class file IntegrationParsingTestSuite.java