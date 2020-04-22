package com.articulate.sigma.trans;

import com.articulate.sigma.FormulaPreprocessor;
import com.articulate.sigma.trans.*;
import com.articulate.sigma.*;
import org.junit.Test;
import org.junit.Before;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.BeforeClass;

/**
 */
public class TPTP3ProofProcTest extends UnitTestBase {

    /** ***************************************************************
     */
    @BeforeClass
    public static void init() {

    }

    /** ***************************************************************
     */
    public void test(String input, String expected, String label) {

        System.out.println("=============================");
        System.out.println("TPTP3ProofProcTest: " + label);
        System.out.println();
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        String actual = tpp.getPrologArgs(input).toString();
        System.out.println("Expected: " + expected);
        if (!StringUtil.emptyString(actual) && actual.equals(expected))
            System.out.println(label + " : Success");
        else
            System.out.println(label + " : fail!");
        assertEquals(expected, actual);
    }

    /** ***************************************************************
     */
    @Test
    public void testGetPrologArgs1() {

        String input = "fof(c_0_5, axiom, (s__subclass(s__Artifact,s__Object)), c_0_3).";
        String expected = "[fof, c_0_5,  axiom,  (s__subclass(s__Artifact,s__Object)),  c_0_3]";
        test(input,expected,"testGetPrologArgs1");
    }

    /** ***************************************************************
     */
    @Test
    public void testGetPrologArgs2() {

        String input = "fof(c_0_2, negated_conjecture,(~(?[X1]:(s__subclass(X1,s__Object)&~$answer(esk1_1(X1)))))," +
                "inference(assume_negation,[status(cth)],[inference(add_answer_literal,[status(thm)],[c_0_0, theory(answers)])])).";
        String expected = "[fof, c_0_2,  negated_conjecture, (~(?[X1]:(s__subclass(X1,s__Object)&~$answer(esk1_1(X1))))), " +
                "inference(assume_negation,[status(cth)],[inference(add_answer_literal,[status(thm)],[c_0_0, theory(answers)])])]";
        test(input,expected,"testGetPrologArgs2");
    }

    /** ***************************************************************
     */
    @Test
    public void testGetPrologArgs3() {
        String input = "cnf(c_0_14,negated_conjecture,($false), " +
                    "inference(eval_answer_literal,[status(thm)], " +
                    "[inference(spm,[status(thm)],[c_0_12, c_0_13, theory(equality)]), theory(answers)]), ['proof']).";
        String expected = "[cnf, c_0_14, negated_conjecture, ($false),  inference(eval_answer_literal,[status(thm)], " +
                    "[inference(spm,[status(thm)],[c_0_12, c_0_13, theory(equality)]), theory(answers)]),  ['proof']]";
        test(input,expected,"testGetPrologArgs3");
    }

    /** ***************************************************************
     */
    @Test
    public void testGetPrologArgs4() {

        String input = "fof(f185,conjecture,(" +
                    "  ? [X15] : s__subclass(X15,s__Entity))," +
                    "  file('/home/apease/.sigmakee/KBs/temp-comb.tptp',unknown)).";
        String expected = "[fof, f185, conjecture, (  ? [X15] : s__subclass(X15,s__Entity)),   " +
                "file('/home/apease/.sigmakee/KBs/temp-comb.tptp',unknown)]";
        test(input,expected,"testGetPrologArgs4");
    }

    /** ***************************************************************
     */
    @Test
    public void testParseProofStep () {

        System.out.println("========================");
        String ps1 = "fof(c_0_5, axiom, (s__subclass(s__Artifact,s__Object)), c_0_3).";
        String ps2 = "fof(c_0_2, negated_conjecture,(~(?[X1]:(s__subclass(X1,s__Object)&~$answer(esk1_1(X1)))))," +
                "inference(assume_negation,[status(cth)],[inference(add_answer_literal,[status(thm)],[c_0_0, theory(answers)])])).";
        String ps3 = "cnf(c_0_14,negated_conjecture,($false), " +
                "inference(eval_answer_literal,[status(thm)], [inference(spm,[status(thm)],[c_0_12, c_0_13, theory(equality)]), theory(answers)]), ['proof']).";
        String ps4 = "fof(f185,conjecture,(" +
                "  ? [X15] : s__subclass(X15,s__Entity))," +
                "  file('/home/apease/.sigmakee/KBs/temp-comb.tptp',unknown)).";
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        tpp.idTable.put("c_0_0", Integer.valueOf(0));
        tpp.idTable.put("c_0_3", Integer.valueOf(1));
        tpp.idTable.put("c_0_12", Integer.valueOf(2));
        tpp.idTable.put("c_0_13", Integer.valueOf(3));
        System.out.println(tpp.parseProofStep(ps1));
        System.out.println();
        System.out.println(tpp.parseProofStep(ps2));
        System.out.println();
        System.out.println(tpp.parseProofStep(ps3));
        System.out.println();
        tpp.idTable.put("f185", Integer.valueOf(4));
        System.out.println(tpp.parseProofStep(ps4));
    }

    /** ***************************************************************
     */
    @Test
    public void testParseAnswers () {

        System.out.println("========================");
        String label = "testParseAnswers";
        System.out.println("TPTP3ProofProcTest: " + label);
        System.out.println();
        String line = "[[s__A,s__B]|_]";
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        tpp.processAnswers(line);
        String actual = tpp.bindings.toString();
        String expected = "[A, B]";
        System.out.println("Actual: " + actual);
        System.out.println("Expected: " + expected);
        if (!StringUtil.emptyString(actual) && actual.equals(expected))
            System.out.println(label + " : Success");
        else
            System.out.println(label + " : fail!");
        assertEquals(expected, actual);

        line = "% SZS answers Tuple [[s__A,s__B]|_] for temp-comb";
        tpp = new TPTP3ProofProcessor();
        tpp.processAnswers(line.substring(20,line.lastIndexOf(']')+1).trim());
        actual = tpp.bindings.toString();
        expected = "[A, B]";
        System.out.println("Actual: " + actual);
        System.out.println("Expected: " + expected);
        if (!StringUtil.emptyString(actual) && actual.equals(expected))
            System.out.println(label + " : Success");
        else
            System.out.println(label + " : fail!");
        assertEquals(expected, actual);
    }
}
