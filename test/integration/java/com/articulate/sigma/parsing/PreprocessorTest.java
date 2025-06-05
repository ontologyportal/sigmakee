package com.articulate.sigma.parsing;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.KBmanager;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;

import org.junit.After;
import org.junit.Test;

public class PreprocessorTest extends IntegrationTestBase {

    SuokifVisitor sv;
    Preprocessor pre;

    @After
    public void afterClass() {
        sv = null;
        pre = null;
    }

    /** ***************************************************************
     */
    @Test
    public void test1() {

        System.out.println("===================== PreprocessorTest.test1() =====================");
        long start = System.currentTimeMillis();
        Path path = Paths.get(System.getenv("SIGMA_HOME") + File.separator + "KBs" + File.separator + "Merge.kif");
        sv = SuokifVisitor.parseFile(path.toFile());
        pre = new Preprocessor(KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname")));

        sv.hasPredVar.removeAll(sv.multiplePredVar); // remove explosive rules with multiple predicate variables
        sv.rules.removeAll(sv.multiplePredVar);
        sv.hasRowVar.removeAll(sv.multiplePredVar);

        Collection<FormulaAST> rules = pre.preprocess(sv.hasPredVar,sv.hasRowVar,sv.rules);
        Set<FormulaAST> result = pre.reparse(rules);
        if (result.size() < 100)
            System.out.println("PreprocessorTest.test1(): " + result);
        else
            System.out.println("PreprocessorTest.test1() results too large to show");
        long end = (System.currentTimeMillis()-start)/1000;
        System.out.println("PreprocessorTest.init(): total preprocess time: " + end + " seconds");
    }

    /** ***************************************************************
     */
    @Test
    public void test2() {

        System.out.println("===================== PreprocessorTest.test2() =====================");
        String input = "(=>\n" +
                "  (and\n" +
                "    (maxValue ?REL ?ARG ?N)\n" +
                "    (?REL @ARGS)\n" +
                "    (equal ?VAL\n" +
                "      (ListOrderFn\n" +
                "        (ListFn @ARGS) ?ARG)))\n" +
                "  (greaterThan ?N ?VAL))";
        sv = SuokifVisitor.parseString(input);
        System.out.println("PreprocessorTest.test2(): # rules: " + sv.rules.size());
        pre = new Preprocessor(KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname")));
        System.out.println("PreprocessorTest.test2(): # before preprocess: " + sv.rules.size());
        Collection<FormulaAST> rules = pre.preprocess(sv.hasPredVar,sv.hasRowVar,sv.rules);
        System.out.println("PreprocessorTest.test2(): # after preprocess: " + rules.size());
        Set<FormulaAST> result = pre.reparse(rules);
        if (result.size() < 100)
            System.out.println("PreprocessorTest.test2(): " + result);
        else
            System.out.println("PreprocessorTest.test2() results too large to show");
    }
}
