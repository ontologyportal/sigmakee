package com.articulate.sigma;

import com.google.common.collect.Sets;
import org.junit.Test;
import com.articulate.sigma.nlg.HtmlParaphraseIntegrationTest;
import com.articulate.sigma.trans.*;
import java.util.ArrayList;
import java.util.Set;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static org.junit.Assert.*;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        CaseRoleTest.class,
        FormatTest.class,
        FormulaPreprocessorAddTypeRestrictionsTest.class,
        FormulaPreprocessorIntegrationTest.class,
        KBcacheTest.class,
        KbIntegrationTest.class,
        KBmanagerInitIntegrationTest.class,
        PredVarInstIntegrationTest.class,
        SUMOformulaToTPTPformulaTest.class,
        //HtmlParaphraseIntegrationTest.class,
})
public class IntegrationTestSuite extends IntegrationTestBase {

}