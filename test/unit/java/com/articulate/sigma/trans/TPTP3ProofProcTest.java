package com.articulate.sigma.trans;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.StringUtil;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

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

        System.out.println("=============================");
        String label = "testGetPrologArgs1";
        System.out.println("TPTP3ProofProcTest: " + label);
        String input = "fof(c_0_5, axiom, (s__subclass(s__Artifact,s__Object)), c_0_3).";
        String expected = "[fof, c_0_5,  axiom,  (s__subclass(s__Artifact,s__Object)),  c_0_3]";
        test(input,expected,"testGetPrologArgs1");
    }

    /** ***************************************************************
     */
    @Test
    public void testGetPrologArgs2() {

        System.out.println("=============================");
        String label = "testGetPrologArgs2";
        System.out.println("TPTP3ProofProcTest: " + label);
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

        System.out.println("=============================");
        String label = "testGetPrologArgs3";
        System.out.println("TPTP3ProofProcTest: " + label);
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

        System.out.println("=============================");
        String label = "testGetPrologArgs4";
        System.out.println("TPTP3ProofProcTest: " + label);
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
        String label = "testParseProofStep";
        System.out.println("TPTP3ProofProcTest: " + label);
        String ps1 = "fof(c_0_5, axiom, (s__subclass(s__Artifact,s__Object)), file('/home/apease/.sigmakee/KBs/temp-comb.tptp', kb_SUMO_1234)).";
        String ps2 = "fof(c_0_2, negated_conjecture,(~(?[X1]:(s__subclass(X1,s__Object)&~$answer(esk1_1(X1)))))," +
                "inference(assume_negation,[status(cth)],[inference(add_answer_literal,[status(thm)],[c_0_0, theory(answers)])])).";
        String ps3 = "cnf(c_0_14,negated_conjecture,($false), " +
                "inference(eval_answer_literal,[status(thm)], [inference(spm,[status(thm)],[c_0_12, c_0_13, theory(equality)]), theory(answers)]), ['proof']).";
        String ps4 = "fof(f185,conjecture,(" +
                "  ? [X15] : s__subclass(X15,s__Entity))," +
                "  file('/home/apease/.sigmakee/KBs/temp-comb.tptp',unknown)).";
        String ps5 = "fof(f768,axiom,(! [X155,X156,X157,X158] : " +
                "((s__instance(X158,s__Organism) & " +
                "s__instance(X155,s__Organism)) => " +
                "((s__instance(X157,s__Golf) & " +
                "s__instance(X156,s__Golf) & " +
                "X155 != X158 & X156 != X157 & " +
                "s__plays(X157,X155) & " +
                "s__plays(X156,X158) & " +
                "s__inhabits(X155,s__UnitedKingdom) & " +
                "s__inhabits(X158,s__UnitedKingdom)) => " +
                "? [X159] : (s__plays(X159,X155) & s__plays(X159,X158) & " +
                "s__instance(X159,s__Golf) & s__located(X159,s__UnitedKingdom) & " +
                "s__instance(X159,s__TournamentSport))))),  file('/home/apease/.sigmakee/KBs/temp-comb.tptp',kb_SUMO_768)).";
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        tpp.idTable.put("c_0_0", Integer.valueOf(0));
        tpp.idTable.put("c_0_3", Integer.valueOf(1));
        tpp.idTable.put("c_0_12", Integer.valueOf(2));
        tpp.idTable.put("c_0_13", Integer.valueOf(3));
 //       System.out.println(tpp.parseProofStep(ps1));
        System.out.println();
 //       System.out.println(tpp.parseProofStep(ps2));
        System.out.println();
 //       System.out.println(tpp.parseProofStep(ps3));
        System.out.println();
        tpp.idTable.put("f185", Integer.valueOf(4));
 //       System.out.println(tpp.parseProofStep(ps4));
        System.out.println();
        String result = tpp.parseProofStep(ps5).toString();
        System.out.println(result);
        assertTrue(result.contains("inhabits"));
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

    /** ***************************************************************
     */
    @Test
    public void testExtractAnswerClause () {

        System.out.println("========================");
        String label = "testExtractAnswerClause";
        System.out.println(label);
        String input = "(forall (?X0) (or (not (instance ?X0 Relation)) (not (ans0 ?X0))))";
        KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        Formula ans = tpp.extractAnswerClause(new Formula(input));
        if (ans == null)
            System.out.println("Fail ans == null");
        assertFalse(ans == null);
        String actual = ans.toString();
        String expected = "(ans0 ?X0)";
        System.out.println("Actual: " + actual);
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
    public void testProcessAnswersFromProof () {

        System.out.println("========================");
        String label = "testProcessAnswersFromProof";
        System.out.println("TPTP3ProofProcTest: " + label);
        ArrayList<String> input = new ArrayList();
        input.add("% SZS status Theorem for temp-comb");
        input.add("% SZS answers Tuple [[s__TransitFn__m]|_] for temp-comb");
        input.add("% SZS output start Proof for temp-comb");
        input.add("fof(f916,plain,( $false), inference(unit_resulting_resolution,[],[f915,f914])).");
        input.add("fof(f914,plain,( ~ans0(s__TransitFn__m)), inference(resolution,[],[f601,f822])).");
        input.add("fof(f822,plain,( s__instance(s__TransitFn__m,s__Relation)), inference(cnf_transformation,[],[f200])).");
        input.add("fof(f200,axiom,( s__instance(s__TransitFn__m,s__Relation)), file('/home/apease/.sigmakee/KBs/temp-comb.tptp',kb_SUMO_200)).");
        input.add("fof(f601,plain,( ( ! [X0] : (~s__instance(X0,s__Relation) | ~ans0(X0)) )), inference(cnf_transformation,[],[f553])).");
        input.add("fof(f553,plain,( ! [X0] : (~s__instance(X0,s__Relation) | ~ans0(X0))), inference(ennf_transformation,[],[f395])).");
        input.add("fof(f395,plain,( ~? [X0] : (s__instance(X0,s__Relation) & ans0(X0))), inference(rectify,[],[f394])).");
        input.add("fof(f394,plain,( ~? [X16] : (s__instance(X16,s__Relation) & ans0(X16))), inference(answer_literal,[],[f393])).");
        input.add("fof(f393,negated_conjecture,( ~? [X16] : s__instance(X16,s__Relation)), inference(negated_conjecture,[],[f392])).");
        input.add("fof(f392,conjecture,( ? [X16] : s__instance(X16,s__Relation)), file('/home/apease/.sigmakee/KBs/temp-comb.tptp',query_0)).");
        input.add("fof(f915,plain,( ( ! [X0] : (ans0(X0)) )), introduced(answer_literal,[])).");
        KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        String query = "(instance ?X Relation)";
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        StringBuffer sb = new StringBuffer("X0");
        tpp.parseProofOutput(input,query,kb,sb);
        tpp.processAnswersFromProof(new StringBuffer("X"),query);
        String actual = tpp.bindingMap.toString();
        String expected = "{X=TransitFn}";
        System.out.println("Actual: " + actual);
        System.out.println("Expected: " + expected);
        if (!StringUtil.emptyString(actual) && actual.equals(expected))
            System.out.println(label + " : Success");
        else
            System.out.println(label + " : fail!");
        assertEquals(expected, actual);
    }
}
