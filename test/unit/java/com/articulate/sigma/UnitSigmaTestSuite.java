package com.articulate.sigma;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

//This software is released under the GNU Public License
//<http://www.gnu.org/copyleft/gpl.html>.
// Copyright 2019 Infosys
// adam.pease@infosys.com

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
    FormulaUtilTest.class,
    KBTest.class,
    KBcacheUnitTest.class,
    KIFTest.class,
    KButilitiesTest.class,
    KBmanagerInitTest.class,
    PredVarInstTest.class,
    RowVarTest.class
})
public class UnitSigmaTestSuite extends UnitTestBase {

}
