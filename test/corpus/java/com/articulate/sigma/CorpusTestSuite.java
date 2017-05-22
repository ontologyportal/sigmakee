package com.articulate.sigma;

import com.google.common.collect.Sets;
import com.articulate.sigma.inference.InferenceTest;
import org.junit.Test;
import com.articulate.sigma.nlg.HtmlParaphraseIntegrationTest;
import java.util.ArrayList;
import java.util.Set;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static org.junit.Assert.*;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        InferenceTest.class,
})
public class CorpusTestSuite extends IntegrationTestBase {

}