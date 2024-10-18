package com.articulate.sigma.trans;

import com.articulate.sigma.UnitTestBase;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    SUMOformulaToTPTPformulaTest.class,
//    SUMOtoTFATest.class,
    TPTP3ProofProcTest.class
})
public class UnitTransTestSuite extends UnitTestBase {

}