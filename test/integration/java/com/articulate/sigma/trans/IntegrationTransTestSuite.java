package com.articulate.sigma.trans;

import com.articulate.sigma.*;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

//This software is released under the GNU Public License
//<http://www.gnu.org/copyleft/gpl.html>.
// Copyright 2019 Infosys
// adam.pease@infosys.com

@RunWith(Suite.class)
@Suite.SuiteClasses({
    SUMOtoTFAKBTest.class,
    SUMOtoTFAformTest.class,
    THFtest.class,
    TPTP2SUMOTest.class
})
public class IntegrationTransTestSuite extends IntegrationTestBase {

}
