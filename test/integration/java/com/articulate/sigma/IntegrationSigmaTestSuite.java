package com.articulate.sigma;

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
    TPTP3Test.class,
    WSDwKBTest.class
})
public class IntegrationSigmaTestSuite extends UnitTestBase {

}
