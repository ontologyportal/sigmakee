package com.articulate.sigma;

import com.articulate.sigma.tp.EProver;
import com.articulate.sigma.tp.Vampire;
import com.articulate.sigma.trans.TPTP3ProofProcessor;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.StringReader;

import com.articulate.sigma.utils.StringUtil;
import org.junit.Test;
import static org.junit.Assert.*;

public class TPTP3Test extends IntegrationTestBase {

    /** ***************************************************************
     */
    @Test
    public void testParseProofFile () {

        System.out.println("-----------------------testParseProofFile--------------------------");
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        try {
            FileReader r = new FileReader(System.getProperty("user.home") + "/Programs/E/PROVER/eltb_out.txt");
            LineNumberReader lnr = new LineNumberReader(r);
            tpp.parseProofOutput(lnr, kb);
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        String result = tpp.proof.toString().trim();
        System.out.println("Result: " + result);
        if (!StringUtil.emptyString(result) && (tpp.proof.size() == 22))
            System.out.println("Success");
        else
            System.out.println("FAIL");
        assertTrue(!StringUtil.emptyString(result));
        assertEquals(tpp.proof.size(),22);
        System.out.println("\n\n");
    }

    /** ***************************************************************
     */
    @Test
    public void testE () {

        KBmanager.getMgr().prover = KBmanager.Prover.EPROVER;
        try {
            System.out.println("----------------------testE---------------------------");
            //KBmanager.getMgr().initializeOnce();
            //KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            EProver eprover = new EProver(KBmanager.getMgr().getPref("eprover"),
                    System.getenv("SIGMA_HOME") + "/KBs/SUMO.tptp");
            System.out.println("testE(): E completed initialization");
            String query = "(subclass ?X Entity)";
            String result = eprover.submitQuery(query, kb);
            StringReader sr = new StringReader(result);
            LineNumberReader lnr = new LineNumberReader(sr);
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            tpp.parseProofOutput(lnr, kb);
            result = tpp.proof.toString().trim();
            System.out.println("Proof: " + result);
            //System.out.println("HTML Proof: " + HTMLformatter.formatTPTP3ProofResult(tpp,query, "", "SUMO", "EnglishLanguage"));
            System.out.println("BindingsMap: " + tpp.bindingMap);
            System.out.println("Bindings: " + tpp.bindings);
            System.out.println("Status: " + tpp.status);
            String bindExpect = "[SetOrClass]";
            if (!StringUtil.emptyString(result) && (tpp.proof.size() == 6) && (tpp.bindings.toString().equals(bindExpect)))
                System.out.println("Success");
            else
                System.out.println("FAIL");
            assertEquals(bindExpect,tpp.bindings.toString());
            assertTrue(!StringUtil.emptyString(result));
            assertEquals(6,tpp.proof.size());
            eprover.terminate();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        System.out.println("\n\n");
    }

    /** ***************************************************************
     */
    @Test
    public void testVampireAvatar () {

        KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
        System.out.println("-------------------testVampireAvatar------------------------------");
        try {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            Vampire.mode = Vampire.ModeType.AVATAR;
            String query = "(subclass ?X Entity)";
            Vampire vampire = kb.askVampire(query,30,1);
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            tpp.parseProofOutput(vampire.output, query, kb, vampire.qlist);
            System.out.println(vampire.toString());
            String result = tpp.proof.toString().trim();
            System.out.println("Result: " + result);
            if (!StringUtil.emptyString(result) && (tpp.proof.size() == 4))
                System.out.println("Success");
            else
                System.out.println("FAIL");
            assertEquals(4,tpp.proof.size());
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
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
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            Vampire.mode = Vampire.ModeType.CASC;
            String query = "(subclass ?X Entity)";
            Vampire vampire = kb.askVampire(query,30,1);
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            tpp.parseProofOutput(vampire.output, query, kb, vampire.qlist);
            System.out.println(vampire.toString());
            String result = tpp.proof.toString().trim();
            String expected = "[PositiveInteger]";
            System.out.println("Result: " + result);
            if (!StringUtil.emptyString(result) &&
                    (tpp.proof.size() == 7) &&
                    (tpp.proof.get(7).sumo.equals("false")))
                System.out.println("Success");
            else
                System.out.println("FAIL");
            assertEquals(10,tpp.proof.size());
            System.out.println("answers: " + result);
            assertEquals("false",tpp.proof.get(7).sumo);
            result = tpp.bindings.toString();
            System.out.println("answers: " + result);
            if (!StringUtil.emptyString(result) && result.equals(expected))
                System.out.println("Success");
            else
                System.out.println("FAIL");
            assertEquals(expected,result);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
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
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            Vampire.mode = Vampire.ModeType.CASC;
            String query = "(subclass ?X Entity)";
            Vampire vampire = kb.askVampire(query,30,1);
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            tpp.parseProofOutput(vampire.output, query,kb, vampire.qlist);
            String expected = "[PositiveInteger]";
            System.out.println("expected: " + expected);
            String result = tpp.bindings.toString();
            System.out.println("Actual: " + result);
            if (!StringUtil.emptyString(result) && expected.equals(result))
                System.out.println("Success");
            else
                System.out.println("FAIL");
            assertEquals(expected,result);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
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
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

            Vampire.mode = Vampire.ModeType.CASC;
            String query = "(subclass ?X ?Y)";
            Vampire vampire = kb.askVampire(query,30,1);
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            tpp.parseProofOutput(vampire.output, query, kb, vampire.qlist);

            String expected = "[RealNumber, Quantity]";
            System.out.println("expected: " + expected);
            String result = tpp.bindings.toString();
            System.out.println("Actual: " + result);
            if (!StringUtil.emptyString(result) && expected.equals(result))
                System.out.println("Success");
            else
                System.out.println("FAIL");
            assertEquals(expected,result);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
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
        tpp.idTable.put("c_0_0", Integer.valueOf(0));
        tpp.idTable.put("c_0_3", Integer.valueOf(1));
        tpp.idTable.put("c_0_12", Integer.valueOf(2));
        tpp.idTable.put("c_0_13", Integer.valueOf(3));
        System.out.println("----------------------testParseProofStep---------------------------");
        String result = tpp.parseProofStep(ps1).toString().trim();
        System.out.println(tpp.parseProofStep(ps1));
        String expected = "0. (subclass Artifact Object) [1] null";
        assertEquals(expected,result);
        System.out.println();

        expected = "2. (not\n" +
                "  (exists (?X1)\n" +
                "    (and\n" +
                "      (subclass ?X1 Object)\n" +
                "      (not\n" +
                "        (answer\n" +
                "          (?X1)))))) [0] assume_negation";
        result = tpp.parseProofStep(ps2).toString().trim();
        System.out.println("Result: " + result);
        if (!StringUtil.emptyString(result) && expected.equals(result))
            System.out.println("Success");
        else
            System.out.println("FAIL");
        assertEquals(expected,result);
        System.out.println();

        expected = "3. false [2, 3] eval_answer_literal";
        result = tpp.parseProofStep(ps3).toString().trim();
        System.out.println("Result: " + result);
        if (!StringUtil.emptyString(result) && expected.equals(result))
            System.out.println("Success");
        else
            System.out.println("FAIL");
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
        tpp.idTable.put("f544", Integer.valueOf(0));
        tpp.idTable.put("f687", Integer.valueOf(1));
        System.out.println("----------------------testParseProofStep2---------------------------");
        String result = tpp.parseProofStep(ps1).toString().trim();
        System.out.println("Result: " + result);
        String expected = "0. false [0, 1] resolution";
        System.out.println("\n\n");
        if (!StringUtil.emptyString(result) && expected.equals(result))
            System.out.println("Success");
        else
            System.out.println("FAIL");
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
        tpp.idTable.put("f225", Integer.valueOf(0));
        System.out.println("----------------------testParseProofStep3---------------------------");
        String result = tpp.parseProofStep(ps1).toString().trim();
        System.out.println("Result: " + result);
        String expected = "0. (subclass Abstract Entity) [0] cnf_transformation";
        System.out.println("\n\n");
        if (!StringUtil.emptyString(result) && expected.equals(result))
            System.out.println("Success");
        else
            System.out.println("FAIL");
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
        tpp.idTable.put("f323", Integer.valueOf(0));
        tpp.idTable.put("f322", Integer.valueOf(1));
        System.out.println("----------------------testParseProofStep4---------------------------");
        String result = tpp.parseProofStep(ps1).toString().trim();
        System.out.println("Result: " + result);
        String expected = "0. false [0, 1] unit_resulting_resolution";
        System.out.println("\n\n");
        if (!StringUtil.emptyString(result) && expected.equals(result))
            System.out.println("Success");
        else
            System.out.println("FAIL");
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
        tpp.idTable.put("c_0_5", Integer.valueOf(0));
        tpp.idTable.put("c_0_6", Integer.valueOf(1));
        tpp.idTable.put("c_0_7", Integer.valueOf(2));
        System.out.println("----------------------testParseProofStep5---------------------------");
        String result = tpp.parseProofStep(ps1).toString().trim();
        System.out.println("Result: " + result);
        String expected = "0. false [0, 1, 2] cn";
        System.out.println("\n\n");
        if (!StringUtil.emptyString(result) && expected.equals(result))
            System.out.println("Success");
        else
            System.out.println("FAIL");
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
        String expected = "introduced:choice_axiom";
        System.out.println("\n\n");
        if (!StringUtil.emptyString(result) && expected.equals(result))
            System.out.println("Success");
        else
            System.out.println("FAIL");
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
            System.out.println("FAIL");
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
            System.out.println("FAIL");
        assertEquals(expected,result);
    }
}