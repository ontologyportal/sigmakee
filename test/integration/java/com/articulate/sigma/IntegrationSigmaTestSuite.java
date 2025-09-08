package com.articulate.sigma;

import com.articulate.sigma.trans.SUMOformulaToTPTPformulaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

//This software is released under the GNU Public License
//<http://www.gnu.org/copyleft/gpl.html>.
// Copyright 2019 Infosys
// adam.pease@infosys.com

@RunWith(Suite.class)
@Suite.SuiteClasses({
    CaseRoleTest.class,
    FormatTest.class,
    FormulaPreprocessorAddTypeRestrictionsTest.class,
    FormulaPreprocessorIntegrationTest.class,
    KBcacheTest.class,
    KBmanagerInitIntegrationTest.class,
    KbIntegrationTest.class,
    PredVarInstIntegrationTest.class,
    SUMOformulaToTPTPformulaTest.class, // <- already tested in the UnitTestSuite
    TPTP3Test.class,
    WSDwKBtest.class,
    DiagnosticsTest.class
})
public class IntegrationSigmaTestSuite extends IntegrationTestBase {

}
