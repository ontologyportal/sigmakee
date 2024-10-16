package com.articulate.sigma.nlg;

import com.articulate.sigma.UnitTestBase;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    CaseRoleTest.class,
    //HtmlParaphraseMockTest.class, TODO: restore tests
    //HtmlParaphraseTest.class,
    KryoTest.class,
    LanguageFormatterStackTest.class,
    LanguageFormatterTest.class,
    NLGStringUtilsTest.class,
    NLGUtilsTest.class,
    SentenceSimpleTest.class,
    SumoProcessCollectorSimpleTest.class,
    SumoProcessCollectorTest.class,
    SumoProcessEntityPropertySimpleTest.class,
    VerbPropertiesTest.class
})
public class UnitNLGTestSuite extends UnitTestBase {

}