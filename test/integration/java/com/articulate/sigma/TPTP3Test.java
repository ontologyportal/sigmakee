package com.articulate.sigma;

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.tp.EProver;
import com.articulate.sigma.tp.Vampire;
import com.articulate.sigma.trans.TPTP3ProofProcessor;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.StringReader;

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
            tpp = TPTP3ProofProcessor.parseProofOutput(lnr, kb);
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        String result = tpp.proof.toString().trim();
        System.out.println(result);
        assertTrue(!StringUtil.emptyString(result));
        assertEquals(tpp.proof.size(),22);
        System.out.println("\n\n");
    }

    /** ***************************************************************
     */
    @Test
    public void testE () {

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
            TPTP3ProofProcessor tpp = TPTP3ProofProcessor.parseProofOutput(lnr, kb);
            result = tpp.proof.toString().trim();
            System.out.println("Proof: " + result);
            //System.out.println("HTML Proof: " + HTMLformatter.formatTPTP3ProofResult(tpp,query, "", "SUMO", "EnglishLanguage"));
            System.out.println("BindingsMap: " + tpp.bindingMap);
            System.out.println("Bindings: " + tpp.bindings);
            System.out.println("Status: " + tpp.status);
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

        System.out.println("-------------------testVampireAvatar------------------------------");
        try {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            Vampire.mode = Vampire.ModeType.AVATAR;
            Vampire vampire = kb.askVampire("(subclass ?X Entity)",30,1);
            TPTP3ProofProcessor tpp = TPTP3ProofProcessor.parseProofOutput(vampire.output, kb);
            System.out.println(vampire.toString());
            String result = tpp.proof.toString().trim();
            System.out.println(result);
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

        System.out.println("-------------------testVampireCASC------------------------------");
        try {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            Vampire.mode = Vampire.ModeType.CASC;
            Vampire vampire = kb.askVampire("(subclass ?X Entity)",30,1);
            TPTP3ProofProcessor tpp = TPTP3ProofProcessor.parseProofOutput(vampire.output, kb);
            System.out.println(vampire.toString());
            String result = tpp.proof.toString().trim();
            System.out.println(result);
            assertEquals(7,tpp.proof.size());
            result = tpp.bindings.toString();
            System.out.println("answers: " + result);
            assertTrue(result.contains("Relation"));
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

        System.out.println("-------------------testVampireCASCBindings------------------------------");
        try {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            Vampire.mode = Vampire.ModeType.CASC;
            Vampire vampire = kb.askVampire("(subclass ?X Entity)",30,1);
            TPTP3ProofProcessor tpp = TPTP3ProofProcessor.parseProofOutput(vampire.output, kb);
            String expected = "[Relation]";
            System.out.println("expected: " + expected);
            String result = tpp.bindings.toString();
            System.out.println("Actual: " + result);
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

        System.out.println("-------------------testVampireCASCBindings2------------------------------");
        try {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

            Vampire.mode = Vampire.ModeType.CASC;
            Vampire vampire = kb.askVampire("(subclass ?X ?Y)",30,1);
            TPTP3ProofProcessor tpp = TPTP3ProofProcessor.parseProofOutput(vampire.output, kb);

            String expected = "[Relation, Entity]";
            System.out.println("expected: " + expected);
            String result = tpp.bindings.toString();
            System.out.println("Actual: " + result);
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
        System.out.println(result);
        assertEquals(expected,result);
        System.out.println();

        expected = "3. false [2, 3] eval_answer_literal";
        result = tpp.parseProofStep(ps3).toString().trim();
        System.out.println(result);
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
        assertEquals(expected,result);
    }

}