package com.articulate.sigma.parsing;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.utils.FileUtil;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import org.junit.After;

public class TPTPWriterTest  extends IntegrationTestBase {

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

        System.out.println("===================== TPTPWriterTest.test1() =====================");
        long start = System.currentTimeMillis();
        Path path = Paths.get(System.getenv("SIGMA_HOME") + File.separator + "KBs" + File.separator + "Merge.kif");
        sv = SuokifVisitor.parseFile(path.toFile());
        pre = new Preprocessor(KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname")));

        Preprocessor.removeMultiplePredVar(sv); // remove explosive rules with multiple predicate variables

        System.out.println("TPTPWriterTest.test1(): sourceFile after parsing: " + sv.rules.iterator().next().sourceFile);
        Collection<FormulaAST> rules = pre.preprocess(sv.hasPredVar,sv.hasRowVar,sv.rules);
        System.out.println("TPTPWriterTest.test1(): sourceFile after preprocessing:" + rules.iterator().next().sourceFile);
        // HashSet<FormulaAST> result = pre.reparse(rules); done already in preprocess
        if (rules.size() < 100)
            System.out.println("TPTPWriterTest.test1(): " + rules);
        else
            System.out.println("TPTPWriterTest.test1() results too large to show");
        long end = System.currentTimeMillis();
        System.out.println("TPTPWriterTest.test1(): total preprocess time: " + ((end-start)/1000) + " seconds");
        TPTPWriter tptpW = new TPTPWriter();
        for (FormulaAST f : rules) {
            System.out.println("fof(kb_" + FileUtil.noExt(FileUtil.noPath(f.sourceFile)) + "_" + f.startLine + ",axiom," + tptpW.visitSentence(f.parsedFormula) + ").");
        }
        long end2 = System.currentTimeMillis();
        System.out.println("TPTPWriterTest.test1(): total write time: " + ((end2-end)/1000)  + " seconds");
    }

    /** ***************************************************************
     */
    @Test
    public void test2() {

        System.out.println("===================== TPTPWriterTest.test2() =====================");
        String s = "(=>\n" +
                "    (equal\n" +
                "        (MinFn ?NUMBER1 ?NUMBER2) ?NUMBER)\n" +
                "    (or\n" +
                "        (and\n" +
                "            (equal ?NUMBER ?NUMBER1)\n" +
                "            (lessThan ?NUMBER1 ?NUMBER2))\n" +
                "        (and\n" +
                "            (equal ?NUMBER ?NUMBER2)\n" +
                "            (lessThan ?NUMBER2 ?NUMBER1))\n" +
                "        (and\n" +
                "            (equal ?NUMBER ?NUMBER1)\n" +
                "            (equal ?NUMBER ?NUMBER2))))";
        sv = SuokifVisitor.parseString(s);
        pre = new Preprocessor(KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname")));
        sv.hasPredVar.removeAll(sv.multiplePredVar); // remove explosive rules with multiple predicate variables
        sv.rules.removeAll(sv.multiplePredVar);
        sv.hasRowVar.removeAll(sv.multiplePredVar);
        System.out.println("TPTPWriterTest.test2(): sourceFile after parsing: " + sv.rules.iterator().next().sourceFile);
        Collection<FormulaAST> rules = pre.preprocess(sv.hasPredVar,sv.hasRowVar,sv.rules);
        System.out.println("TPTPWriterTest.test2(): sourceFile after preprocessing:" + rules.iterator().next().sourceFile);
        // HashSet<FormulaAST> result = pre.reparse(rules); done already in preprocess
        if (rules.size() < 100)
            System.out.println("TPTPWriterTest.test2(): " + rules);
        else
            System.out.println("TPTPWriterTest.test2() results too large to show");
        TPTPWriter tptpW = new TPTPWriter();
        for (FormulaAST f : rules) {
            System.out.println("fof(kb_" + FileUtil.noExt(FileUtil.noPath(f.sourceFile)) + "_" + f.startLine + ",axiom," + tptpW.visitSentence(f.parsedFormula) + ").");
        }
    }
}
