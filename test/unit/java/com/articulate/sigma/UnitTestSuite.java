package com.articulate.sigma;

import com.google.common.collect.Sets;
import com.articulate.sigma.trans.SUMOformulaToTPTPformulaTest;
import org.junit.Test;
import com.articulate.sigma.nlg.UnitNLGTestSuite;
import com.articulate.sigma.wordNet.MultiWordsTest;
import java.util.ArrayList;
import java.util.Set;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import com.articulate.sigma.wordNet.*;
import com.articulate.sigma.trans.*;

import static org.junit.Assert.*;

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
        FormulaUtilTest.class,
        FormulaUnificationTest.class,
        KBcacheUnitTest.class,
        KBmanagerInitTest.class,
        KBTest.class,
        MultiWordsTest.class,
        PredVarInstTest.class,
        RowVarTest.class,
        SUMOformulaToTPTPformulaTest.class,
        TPTP3ProofProcTest.class,
        UnitNLGTestSuite.class,
        WordNetTest.class,
})
public class UnitTestSuite extends UnitTestBase {

}
