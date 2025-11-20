package com.articulate.sigma;

import org.junit.Test;

import com.articulate.sigma.ErrRec;
import com.articulate.sigma.TPTPFileChecker;
import com.articulate.sigma.UnitTestBase;

import java.util.*;
import static org.junit.Assert.*;

public class TPTPFileCheckerTest extends UnitTestBase {

    boolean debug = true;
    String divider = "\n------------------------------------------------------------\n";
    String passed = "PASSED ✅";
    String failed = "FAILED ❌";
    TPTPFileChecker tfc = new TPTPFileChecker();

    @Test
    public void testMainTPTPCheck_1() {

        String tptpString =
            "fooooof(kb_SUMO_UserAssertion_5,axiom,(( ( ! [V__R,V__X,V__Y,V__Z] : "
        + "(((s__instance(V__R,s__PositionalAttribute) & s__instance(V__X,s__Object) "
        + "& s__instance(V__Y,s__Object) & s__instance(V__Z,s__Object)) => "
        + "((s__orientation(V__X,V__Y,V__R) & s__orientation(V__Y,V__Z,V__R)) => "
        + "s__orientation(V__X,V__Z,V__R))) ) ) ))).";
        List<ErrRec> actual = tfc.check(tptpString, "fileName");
        List<ErrRec> expected = new ArrayList<>();
        ErrRec expected1 = new ErrRec(ErrRec.ERROR, "fileName", 0, 0, 1, "ANTLR parser found no valid formulas in input");
        ErrRec expected2 = new ErrRec(ErrRec.ERROR, "fileName", 0, 7, 8, "% SZS status SyntaxError : Line 1 Char 8 Token \"fooooof\" " + "continuing with \"kb_SUMO_UserAsserti\" : Wrong token type or value, " + "expected lower_word with value \"cnf\"");
        expected.add(expected1);
        expected.add(expected2);
        if (debug) {
            System.out.println(divider + "TEST = TPTPFileCheckerTest.testMainTPTPCheck1()");
            System.out.println("EXPECTED:\n" + expected);
            System.out.println("ACTUAL:\n" + actual);
            System.out.println("Test: " + (expected.equals(actual) ? "PASSED ✅" : "FAILED ❌"));
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testCheckWithAntlr_1() {
        
        String tptpString =
            "fooooof(kb_SUMO_UserAssertion_5,axiom,(( ( ! [V__R,V__X,V__Y,V__Z] : "
        + "(((s__instance(V__R,s__PositionalAttribute) & s__instance(V__X,s__Object) "
        + "& s__instance(V__Y,s__Object) & s__instance(V__Z,s__Object)) => "
        + "((s__orientation(V__X,V__Y,V__R) & s__orientation(V__Y,V__Z,V__R)) => "
        + "s__orientation(V__X,V__Z,V__R))) ) ) ))).";
        List<ErrRec> actual = tfc.checkWithAntlr(tptpString, "fileName");
        List<ErrRec> expected = new ArrayList<>();
        ErrRec expectedErr = new ErrRec(ErrRec.ERROR, "fileName", 0, 0, 1, "ANTLR parser found no valid formulas in input");
        expected.add(expectedErr);
        if (debug) {
            System.out.println(divider + "TEST = TPTPFileCheckerTest.testCheckWithAntlr_1()");
            System.out.println("EXPECTED: \n" + expected);
            System.out.println("ACTUAL: \n" + actual);
            System.out.println("Test: " + (expected.equals(actual) ? "PASSED ✅" : "FAILED ❌"));
        }
        assertEquals(expected, actual);
    }
    
    @Test
    public void testCheckWithTPTP4X_1() {

        String tptpString =
            "fooooof(kb_SUMO_UserAssertion_5,axiom,(( ( ! [V__R,V__X,V__Y,V__Z] : "
        + "(((s__instance(V__R,s__PositionalAttribute) & s__instance(V__X,s__Object) "
        + "& s__instance(V__Y,s__Object) & s__instance(V__Z,s__Object)) => "
        + "((s__orientation(V__X,V__Y,V__R) & s__orientation(V__Y,V__Z,V__R)) => "
        + "s__orientation(V__X,V__Z,V__R))) ) ) ))).";
        List<ErrRec> actual = tfc.check(tptpString, "fileName");
        List<ErrRec> expected = new ArrayList<>();
        expected.add(new ErrRec(
            ErrRec.ERROR, "fileName",
            0, 0, 1,
            "ANTLR parser found no valid formulas in input"));
        expected.add(new ErrRec(
            ErrRec.ERROR, "fileName",
            0, 7, 8,
            "% SZS status SyntaxError : Line 1 Char 8 Token \"fooooof\" "
        + "continuing with \"kb_SUMO_UserAsserti\" : Wrong token type or value, "
        + "expected lower_word with value \"cnf\""));
        if (debug) {
            System.out.println(divider + "TEST = TPTPFileCheckerTest.testCheckWithTPTP4X_1()");
            System.out.println("EXPECTED:\n" + expected);
            System.out.println("ACTUAL:\n" + actual);
            System.out.println("Test: " + (expected.equals(actual) ? passed : failed));
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testParseTptpOutput_1() {

        String tptpString = "fooooof(kb_SUMO_UserAssertion_5,axiom,(p)).";
        List<ErrRec> actual = tfc.check(tptpString, "fileName");
        assertFalse(actual.isEmpty());
        ErrRec first = actual.get(0);
        assertEquals("fileName", first.file);
        assertTrue(first.msg.contains("ANTLR parser found no valid formulas"));
        if (debug) {
            System.out.println(divider + "TEST: testCheck_invalidInput()");
            System.out.println("ACTUAL:\n" + actual);
            System.out.println("Test: " + passed);
        }
    }

    @Test
    public void testFormatTptpText_1() {

        String input = "fof(ax,axiom,(p=>q)).";
        String result = TPTPFileChecker.formatTptpText(input, "fileName");
        boolean tptp4XAvailable = true;
        try {
            Process proc = new ProcessBuilder("tptp4X", "--version").start();
            if (proc.waitFor() != 0)
                tptp4XAvailable = false;
        } catch (Exception e) {
            tptp4XAvailable = false;
        }
        if (!tptp4XAvailable) {
            assertEquals(input, result);
            if (debug) {
                System.out.println(divider + "TEST: formatTptpText (tptp4X missing)");
                System.out.println("RESULT:\n" + result);
                System.out.println("Test: " + passed);
            }
            return;
        }
        assertNotNull(result);
        assertNotEquals(input, result);
        assertTrue(result.contains("fof"));
        if (debug) {
            System.out.println(divider + "TEST: formatTptpText (tptp4X present)");
            System.out.println("INPUT:\n" + input);
            System.out.println("OUTPUT:\n" + result);
            System.out.println("Test: " + passed);
        }
    }

}