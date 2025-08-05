package com.articulate.sigma;

import com.articulate.sigma.tp.EProver;
import com.articulate.sigma.tp.Vampire;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.trans.TPTP3ProofProcessor;
import com.articulate.sigma.utils.FileUtil;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

public class TPTP3Test extends IntegrationTestBase {

    /** ***************************************************************
     */
    private void parseProofFile () {

        System.out.println("-----------------------testParseProofFile--------------------------");
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
//        String expected = "6. (exists (?X1)\n" +
//                          "  (subclass ?X1 Entity)) []"; // GitHub workflow test chooses different kb_SUMO for proof
        File file = new File(System.getenv("SIGMA_SRC") + "/prover_out.txt");
        file.deleteOnExit();
        List<String> lines = FileUtil.readLines(file.getPath(), true);
        String result;
        for (String line : lines) {
            result = tpp.parseProofStep(line).toString().trim();
            System.out.println("Result: " + result);
            if (!StringUtil.emptyString(result))
                System.out.println("Success");
            else
                System.err.println("FAIL");
            assertTrue(!StringUtil.emptyString(result));
        }
        System.out.println("\n\n");
    }

    /** ***************************************************************
     */
    @Test
    public void testE() {

        KBmanager.getMgr().prover = KBmanager.Prover.EPROVER;
        try {
            System.out.println("----------------------testE---------------------------");
            String query = "(subclass ?X Entity)";
            EProver eprover = kb.askEProver(query, 30, 1);
            String result;
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            tpp.parseProofOutput(eprover.output, "?X", kb, eprover.qlist);
            result = tpp.proof.toString().trim();
            File file = new File(System.getenv("SIGMA_SRC") + "/prover_out.txt");
            file.setWritable(true);
            file.createNewFile();
            try (Writer w = new FileWriter(file); Writer pw = new PrintWriter(new BufferedWriter(w))) {
                pw.write(result);
            }
            System.out.println("Proof: " + result);
            //System.out.println("HTML Proof: " + HTMLformatter.formatTPTP3ProofResult(tpp,query, "", "SUMO", "EnglishLanguage"));
            System.out.println("Bindings: " + tpp.bindings);
            System.out.println("Status: " + tpp.status);
//            String bindExpect = "[Class]"; // but shows as [Abstract] on Ubuntu 24.04?
            if (!StringUtil.emptyString(result) && (tpp.proof.size() == 7) && !tpp.bindings.isEmpty() && tpp.status.equals("Theorem"))
                System.out.println("Success");
            else
                System.err.println("FAIL");
            assertTrue(!tpp.bindings.isEmpty());
            assertTrue(!StringUtil.emptyString(result));
            assertTrue(tpp.status.equals("Theorem"));
            assertEquals(7,tpp.proof.size());
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
        System.out.println("\n\n");
        parseProofFile();
    }

    /** ***************************************************************
     */
    @Test
    public void testVampireAvatar () {

        KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
        System.out.println("-------------------testVampireAvatar------------------------------");
        try {
            KBmanager.getMgr().initializeOnce();
            Vampire.mode = Vampire.ModeType.AVATAR;
            String query = "(subclass ?X Entity)";
            Vampire vampire = kb.askVampire(query,30,1);
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            tpp.parseProofOutput(vampire.output, query, kb, vampire.qlist);
            System.out.println(vampire.toString());
            String result = tpp.proof.toString().trim();
            System.out.println("Result: " + result);
            if (!StringUtil.emptyString(result) && (tpp.proof.size() == 8))
                System.out.println("Success");
            else
                System.err.println("FAIL");
            assertEquals(8,tpp.proof.size());
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
        System.out.println("\n\n");
    }

    /** ***************************************************************
     */
    @Test
    public void testVampireCASC () {

        KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
        System.out.println("-------------------testVampireCASC------------------------------");
        try {
            KBmanager.getMgr().initializeOnce();
            Vampire.mode = Vampire.ModeType.CASC;
            String query = "(subclass ?X Entity)";
            Vampire vampire = kb.askVampire(query,30,1);
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            tpp.parseProofOutput(vampire.output, query, kb, vampire.qlist);
            System.out.println(vampire.toString());
            String result = tpp.proof.toString().trim();
            String expected = "[]";
            System.out.println("Result: " + result);
            if (!StringUtil.emptyString(result) &&
                    (tpp.proof.size() == 8) &&
                    (tpp.proof.get(0).sumo.equals("false")))
                System.out.println("Success");
            else
                System.err.println("FAIL");
            assertEquals(8,tpp.proof.size());
            assertEquals("false",tpp.proof.get(0).sumo);
            result = tpp.bindings.toString();
            System.out.println("answers: " + result);
            if (!StringUtil.emptyString(result) && result.equals(expected))
                System.out.println("Success");
            else
                System.err.println("FAIL");
            assertEquals(expected,result);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
        System.out.println("\n\n");
    }

    /** ***************************************************************
     */
    @Test
    public void testVampireCASCBindings () {

        KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
        System.out.println("-------------------testVampireCASCBindings------------------------------");
        try {
            KBmanager.getMgr().initializeOnce();
            Vampire.mode = Vampire.ModeType.CASC;
            String query = "(subclass ?X Entity)";
            Vampire vampire = kb.askVampire(query,30,1);
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            tpp.parseProofOutput(vampire.output, query,kb, vampire.qlist);
            String expected = "[]";
            System.out.println("expected: " + expected);
            String result = tpp.bindings.toString();
            System.out.println("Actual: " + result);
            if (!StringUtil.emptyString(result) && expected.equals(result))
                System.out.println("Success");
            else
                System.err.println("FAIL");
            assertEquals(expected,result);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
        System.out.println("\n\n");
    }

    /** ***************************************************************
     */
    @Test
    public void testVampireCASCBindings2 () {

        KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
        System.out.println("-------------------testVampireCASCBindings2------------------------------");
        try {
            KBmanager.getMgr().initializeOnce();
            Vampire.mode = Vampire.ModeType.CASC;
            String query = "(subclass ?X ?Y)";
            Vampire vampire = kb.askVampire(query,30,1);
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            tpp.parseProofOutput(vampire.output, query, kb, vampire.qlist);
            String expected = "[]";
            System.out.println("expected: " + expected);
            String result = tpp.bindings.toString();
            System.out.println("Actual: " + result);
            if (!StringUtil.emptyString(result) && expected.equals(result))
                System.out.println("Success");
            else
                System.err.println("FAIL");
            assertEquals(expected,result);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
        System.out.println("\n\n");
    }

    /** ***************************************************************
     */
    @Test
    public void testParseProofStep () {

        String ps1 = "fof(c_0_5, axiom, (s__subclass(s__Artifact,s__Object)), c_0_3).";
        String ps2 = "fof(c_0_2, negated_conjecture,(~(?[X1]:(s__subclass(X1,s__Object)&~$answer(esk1_1(X1)))))," +
                "inference(assume_negation,[status(cth)],[inference(add_answer_literal,[status(thm)],[c_0_0, theory(answers)])])).";
        String ps3 = "cnf(c_0_14,negated_conjecture,($false), " +
                "inference(eval_answer_literal,[status(thm)], [inference(spm,[status(thm)],[c_0_12, c_0_13, theory(equality)]), theory(answers)]), ['proof']).";
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        tpp.idTable.put("c_0_0", 0);
        tpp.idTable.put("c_0_3", 1);
        tpp.idTable.put("c_0_12", 2);
        tpp.idTable.put("c_0_13", 3);
        System.out.println("----------------------testParseProofStep---------------------------");
        String result = tpp.parseProofStep(ps1).toString().trim();
        System.out.println("Result: " + result);
        String expected = "0. (subclass Artifact Object) [1]";
        assertEquals(expected,result);
        System.out.println();

        expected = "1. (not\n" +
                "  (exists (?X1)\n" +
                "    (and\n" +
                "      (subclass ?X1 Object)\n" +
                "      (not\n" +
                "        (answer\n" +
                "          (esk1_1 ?X1)))))) [2] assume_negation";
        result = tpp.parseProofStep(ps2).toString().trim();
        System.out.println("Result: " + result);
        if (!StringUtil.emptyString(result) && expected.equals(result))
            System.out.println("Success");
        else
            System.err.println("FAIL");
        assertEquals(expected,result);
        System.out.println();

        expected = "3. false [4, 5] eval_answer_literal";
        result = tpp.parseProofStep(ps3).toString().trim();
        System.out.println("Result: " + result);
        if (!StringUtil.emptyString(result) && expected.equals(result))
            System.out.println("Success");
        else
            System.err.println("FAIL");
        assertEquals(expected,result);
        System.out.println("\n\n");
    }

    /** ***************************************************************
     */
    @Test
    public void testParseProofStep2 () {

        String ps1 = "fof(f852,plain,(\n" +
                "  $false),\n" +
                "  inference(resolution,[],[f544,f687])).";
        //ps1 = ps1.replaceAll("\n","");
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        tpp.idTable.put("f544", 0);
        tpp.idTable.put("f687", 1);
        System.out.println("----------------------testParseProofStep2---------------------------");
        String result = tpp.parseProofStep(ps1).toString().trim();
        System.out.println("Result: " + result);
        String expected = "0. false [0, 1] resolution";
        System.out.println("\n\n");
        if (!StringUtil.emptyString(result) && expected.equals(result))
            System.out.println("Success");
        else
            System.err.println("FAIL");
        assertEquals(expected,result);
    }

    /** ***************************************************************
     */
    @Test
    public void testParseProofStep3 () {

        String ps1 = "fof(f559,plain,(\n" +
                "  s__subclass(s__Abstract,s__Entity)),\n" +
                "  inference(cnf_transformation,[],[f225])).";
        //ps1 = ps1.replaceAll("\n","");
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        tpp.idTable.put("f225", 0);
        System.out.println("----------------------testParseProofStep3---------------------------");
        String result = tpp.parseProofStep(ps1).toString().trim();
        System.out.println("Result: " + result);
        String expected = "0. (subclass Abstract Entity) [0] cnf_transformation";
        System.out.println("\n\n");
        if (!StringUtil.emptyString(result) && expected.equals(result))
            System.out.println("Success");
        else
            System.err.println("FAIL");
        assertEquals(expected,result);
    }

    /** ***************************************************************
     */
    @Test
    public void testParseProofStep4 () {

        String ps1 = "fof(f324,plain,(\n" +
                "  $false),\n" +
                "  inference(unit_resulting_resolution,[],[f323,f322])).";
        //ps1 = ps1.replaceAll("\n","");
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        tpp.idTable.put("f323", 0);
        tpp.idTable.put("f322", 1);
        System.out.println("----------------------testParseProofStep4---------------------------");
        String result = tpp.parseProofStep(ps1).toString().trim();
        System.out.println("Result: " + result);
        String expected = "0. false [0, 1] unit_resulting_resolution";
        System.out.println("\n\n");
        if (!StringUtil.emptyString(result) && expected.equals(result))
            System.out.println("Success");
        else
            System.err.println("FAIL");
        assertEquals(expected,result);
    }

    /** ***************************************************************
     */
    @Test
    public void testParseProofStep5 () {

        String ps1 = "cnf(c_0_8, negated_conjecture, ($false), " +
                "inference(cn,[status(thm)]," +
                  "[inference(rw,[status(thm)]," +
                    "[inference(rw,[status(thm)],[c_0_5, c_0_6]), c_0_7])])," +
                " ['proof']).\n";
        //ps1 = ps1.replaceAll("\n","");
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        tpp.idTable.put("c_0_5", 0);
        tpp.idTable.put("c_0_6", 1);
        tpp.idTable.put("c_0_7", 2);
        System.out.println("----------------------testParseProofStep5---------------------------");
        String result = tpp.parseProofStep(ps1).toString().trim();
        System.out.println("Result: " + result);
        String expected = "0. false [1] cn";
        System.out.println("\n\n");
        if (!StringUtil.emptyString(result) && expected.equals(result))
            System.out.println("Success");
        else
            System.err.println("FAIL");
        assertEquals(expected,result);
    }

    /** ***************************************************************
     */
    @Test
    public void testParseProofStep6 () {

        String ps1 = "fof(f16682,plain,(\n" +
                "  ! [X0] : (? [X1] : (s__member(X1,X0) & s__instance(X1,s__Object)) => " +
                "(s__member(sK5(X0),X0) & s__instance(sK5(X0),s__Object)))),\n" +
                "  introduced(choice_axiom,[])).\n";
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        System.out.println("----------------------testParseProofStep6---------------------------");
        ProofStep ps = tpp.parseProofStep(ps1);
        String result = ps.inferenceType;
        System.out.println("Result: " + result);
        String expected = "choice_axiom";
        System.out.println("\n\n");
        if (!StringUtil.emptyString(result) && expected.equals(result))
            System.out.println("Success");
        else
            System.err.println("FAIL");
        assertEquals(expected,result);
    }

    /** ***************************************************************
     */
    @Test
    public void testExtractAnswerClauseVamp () {

        KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
        System.out.println("========================");
        String label = "testExtractAnswerClauseVamp";
        System.out.println(label);
        String input = "(forall (?X0) (or (not (instance ?X0 Relation)) (not (ans0 ?X0))))";
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        Formula ans = tpp.extractAnswerClause(new Formula(input));
        String result = ans.toString();
        System.out.println("result: " + ans);
        String expected = "(ans0 ?X0)";
        if (!StringUtil.emptyString(result) && expected.equals(result))
            System.out.println("Success");
        else
            System.err.println("FAIL");
    }

    /** ***************************************************************
     */
    @Test
    public void testExtractAnswerClauseE () {

        KBmanager.getMgr().prover = KBmanager.Prover.EPROVER;
        System.out.println("========================");
        String label = "testExtractAnswerClauseE";
        System.out.println(label);
        String input = "(forall (?VAR1) (or (not (subclass ?VAR1 Object)) (answer (?VAR1))))";
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        Formula ans = tpp.extractAnswerClause(new Formula(input));
        String result = ans.getFormula();
        System.out.println("result: " + ans);
        String expected = "(answer (?VAR1))";
        if (!StringUtil.emptyString(result) && expected.equals(result))
            System.out.println("Success");
        else
            System.err.println("FAIL");
        assertEquals(expected,result);
    }
}