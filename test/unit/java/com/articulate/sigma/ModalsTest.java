package com.articulate.sigma;

import com.articulate.sigma.trans.Modals;

import org.junit.Test;
import org.junit.Assume;
import java.io.File;

import static org.junit.Assert.*;

public class ModalsTest extends UnitTestBase {

    @Test
    public void testDualityAxiom() {
        String fstr =
                "(<=> " +
                        "    (modalAttribute ?FORMULA Prohibition) " +
                        "    (not (modalAttribute ?FORMULA Permission)))";

        Formula f = new Formula(fstr);
        Formula out = Modals.processModals(f, kb);

        String result = out.getFormula();
        System.out.println("\nDuality Test:\n" + result);

        assertTrue(result.contains(
        "(<=>" +
        "  (=>" +
        "    (accreln Prohibition CW ?W1) ?FORMULA)" +
        "  (=>" +
        "    (accreln Prohibition CW ?W1) ?FORMULA)" +
        "  (not" +
        "    (=>" +
        "      (accreln Permission CW ?W1) ?FORMULA)" +
        "    (=>" +
        "      (accreln Permission CW ?W1) ?FORMULA)))"));
    }

    @Test
    public void testDeepNestedModals() {
        String fstr =
                "(holdsDuring ?T " +
                        "   (knows John " +
                        "       (believes Mary " +
                        "           (knows Bill " +
                        "               (believes Sue " +
                        "                   (=> " +
                        "                       (acquaintance Bill Sue) " +
                        "                       (acquaintance Bill Jane)))))))";

        Formula f = new Formula(fstr);
        Formula out = Modals.processModals(f, kb);

        String result = out.getFormula();
        System.out.println("\nDeep Modal Test:\n" + result);

        assertTrue(result.contains("?W1"));
        assertTrue(result.contains("?W2"));
        assertTrue(result.contains("?W3"));
        assertTrue(result.contains("?W4"));
        assertTrue(result.contains("?W5"));
        assertTrue(result.contains("accreln"));
    }

    @Test
    public void testPureFOLNoModals() {
        String fstr =
                "(=> " +
                        "    (and " +
                        "        (instance ?EXPRESS ExpressingApproval) " +
                        "        (agent ?EXPRESS ?AGENT) " +
                        "        (patient ?EXPRESS ?THING)) " +
                        "    (or " +
                        "        (wants ?AGENT ?THING) " +
                        "        (desires ?AGENT ?THING)))";

        Formula f = new Formula(fstr);
        Formula out = Modals.processModals(f, kb);

        String result = out.getFormula();
        System.out.println("\nPure FOL Test:\n" + result);

        assertFalse(result.contains("accreln"));
        assertTrue(result.contains("wants"));
        assertTrue(result.contains("desires"));
    }

    @Test
    public void testMixedWorldSensitivePredicates() {
        String fstr =
                "(=> " +
                        "  (instance ?ARGUMENT Argument ?W1) " +
                        "  (exists (?PREMISES ?CONCLUSION) " +
                        "    (and " +
                        "      (instance ?PREMISES Formula) " +
                        "      (instance ?CONCLUSION Argument) " +
                        "      (and " +
                        "        (equal (PremisesFn ?ARGUMENT ?W1) ?PREMISES) " +
                        "        (conclusion ?CONCLUSION ?ARGUMENT ?W1)))))";

        Formula f = new Formula(fstr);
        Formula out = Modals.processModals(f, kb);

        String result = out.getFormula();
        System.out.println("\nMixed World-Sensitive Test:\n" + result);

        assertFalse(result.contains("accreln"));
        assertTrue(result.contains("?W1"));
    }
}