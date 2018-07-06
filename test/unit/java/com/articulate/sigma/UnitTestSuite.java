package com.articulate.sigma;

import com.google.common.collect.Sets;
import org.junit.Test;
import com.articulate.sigma.nlg.UnitNLGTestSuite;
import com.articulate.sigma.wordNet.MultiWordsTest;
import java.util.ArrayList;
import java.util.Set;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import com.articulate.sigma.wordNet.*;

import static org.junit.Assert.*;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        FormulaArityCheckTest.class,
        FormulaDeepEqualsTest.class,
        FormulaLogicalEqualityTest.class,
        FormulaPreprocessorComputeVariableTypesTest.class,
        FormulaPreprocessorFindExplicitTypesTest.class,
        FormulaPreprocessorTest.class,
        FormulaTest.class,
        FormulaUnificationTest.class,
        KBcacheUnitTest.class,
        KBmanagerInitTest.class,
        PredVarInstTest.class,
        StringUtilTest.class,
        SUMOformulaToTPTPformulaTest.class,
        MultiWordsTest.class,
        WordNetTest.class,
        KBTest.class,
        UnitNLGTestSuite.class,
})
public class UnitTestSuite extends UnitTestBase {

}