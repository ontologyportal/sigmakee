package com.articulate.sigma.parsing;

import com.articulate.sigma.UnitTestBase;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author <a href="mailto:terry.norbraten@gmail.com?subject=com.articulate.sigma.parsing.UnitParsingTestSuite">Terry Norbraten</a>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    SUOKIFparseTest.class,
    SUOKIFCacheTest.class,
    ExprTest.class,
    SuokifToExprTest.class,
    KIFASTTest.class,
    PredVarInstExprTest.class,
    ClausifierExprTest.class,
    ExprToTPTPTest.class,
    ExprToTFFTest.class,
    ExprToTHFTest.class,
    ModalsExprTest.class,
    FormulaASTTest.class
})
public class UnitParsingTestSuite  extends UnitTestBase {

} // end class file UnitParsingTestSuite.java