package com.articulate.sigma.nlg;

import com.articulate.sigma.UnitTestBase;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Set;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static org.junit.Assert.*;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        CaseRoleTest.class,
        //HtmlParaphraseMockTest.class, TODO: restore tests
        //HtmlParaphraseTest.class,
        LanguageFormatterStackTest.class,
        LanguageFormatterTest.class,
        NLGStringUtilsTest.class,
        NLGUtilsTest.class,
        SentenceSimpleTest.class,
        SumoProcessCollectorSimpleTest.class,
        SumoProcessCollectorTest.class,
        SumoProcessEntityPropertySimpleTest.class,
        VerbPropertiesTest.class,
})
public class UnitNLGTestSuite extends UnitTestBase {

}