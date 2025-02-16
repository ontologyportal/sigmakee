package com.articulate.sigma;

import com.articulate.sigma.trans.*;
import com.articulate.sigma.nlg.*;
import com.articulate.sigma.wordNet.*;

import org.junit.AfterClass;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

//This software is released under the GNU Public License
//<http://www.gnu.org/copyleft/gpl.html>.
// Copyright 2019 Infosys
// adam.pease@infosys.com

@RunWith(Suite.class)
@Suite.SuiteClasses({
    UnitSigmaTestSuite.class,
    UnitNLGTestSuite.class,
    UnitTransTestSuite.class,
    UnitWordNetTestSuite.class
})
public class UnitTestSuite extends UnitTestBase {

    @AfterClass
    public static void shutDown() {
        KButilities.shutDownExecutorService();
    }
}
