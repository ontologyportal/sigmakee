package com.articulate.sigma;

import org.junit.BeforeClass;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import com.articulate.sigma.trans.SUMOtoTFAform;

public class KifFileCheckerTest extends UnitTestBase {

    boolean debug = true;
    String divider = "\n------------------------------------------------------------\n";
    String passed = "PASSED ✅";
    String failed = "FAILED ❌";
    static KB kb;
    KifFileChecker kfc = new KifFileChecker();

    @BeforeClass
    public static void initKB() {

        SUMOtoTFAform.initOnce();
        kb = SUMOtoTFAform.kb;
        assertNotNull("KB should be initialized", kb);
    }

    @Test
    public void testCheckQuantifiedVariableNotInStatement1() {
        
        String kifString =
            "(exists (?X)\n" +
            "  (and\n" +
            "    (instance Shaun Human)\n" +
            "    (attribute Shaun Mortal)))";
        Formula kifFormula = new Formula(kifString);
        List<ErrRec> errorList = new ArrayList<>();
        kfc.CheckQuantifiedVariableNotInStatement("fileName", kifFormula, kifString, 0, errorList);
        ErrRec expected = new ErrRec( ErrRec.ERROR, "fileName", 0, 1, 7, "Quantified variable not used in statement body - (exists (?X)");
        ErrRec actual = errorList.get(0);
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testCheckQuantifiedVariableNotInStatement1()");
            System.out.println("Expected: " + expected);
            System.out.println("Actual: " + actual);
            System.out.println("Test: " + (expected.equals(actual) ? "PASSED ✅" : "FAILED ❌"));
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testCheckOrphanVars1() {

        String kifString = "(exists (?X ?Y ?A ?B)\n" + 
                           "  (and\n" + 
                           "    (instance ?X CausingHappiness)\n" + 
                           "    (patient ?X ?Y)\n" + 
                           "    (owns ?A ?B)))";
        Formula kifFormula = new Formula(kifString);
        List<ErrRec> errorList = new ArrayList<>();
        kfc.CheckOrphanVars("fileName", kifFormula, kifString, 0, errorList);
        ErrRec expected = new ErrRec( ErrRec.ERROR, "fileName", 0, 0, 102, "Formula has 2 disconnected variable groups: Group 1: [?A, ?B]; Group 2: [?X, ?Y]");
        ErrRec actual = errorList.get(0);
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testCheckOrphanVars1()");
            System.out.println("Expected: " + expected);
            System.out.println("Actual: " + actual);
            System.out.println("Test: " + (expected.equals(actual) ? "PASSED ✅" : "FAILED ❌"));
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testCheckExistentialInAntecedent1() {

        String kifString = "(=>\n" +
                        "  (exists (?X)\n" +
                        "   (instance ?X Human))\n" +
                        "   (instance Shaun Human))";
        Formula kifFormula = new Formula(kifString);
        List<ErrRec> errorList = new ArrayList<>();
        kfc.CheckExistentialInAntecedent("fileName", kifFormula, kifString, 0, errorList);
        ErrRec expected = new ErrRec( ErrRec.WARNING, "fileName", 1, 3, 10, "Existential quantifier in antecedent - (exists (?X)");
        ErrRec actual = errorList.get(0);
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testCheckExistentialInAntecedent1()");
            System.out.println("Expected: " + expected);
            System.out.println("Actual: " + actual);
            System.out.println("Test: " + (expected.equals(actual) ? "PASSED ✅" : "FAILED ❌"));
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testCheckSingleUseVariables1() {

        String kifString = "(=>\n" + 
                        "  (instance ?X Man)\n" +
                        "  (instance Shaun Man))";
        Formula kifFormula = new Formula(kifString);
        List<ErrRec> errorList = new ArrayList<>();
        kfc.CheckSingleUseVariables("fileName", kifFormula, kifString, 0, errorList);
        ErrRec expected = new ErrRec( ErrRec.WARNING, "fileName", 1, 12, 14, "Variable used only once - (instance ?X Man)");
        ErrRec actual = errorList.get(0);
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testCheckSingleUseVariables1()");
            System.out.println("Expected: " + expected);
            System.out.println("Actual: " + actual);
            System.out.println("Test: " + (expected.equals(actual) ? "PASSED ✅" : "FAILED ❌"));
        }
        assertEquals(expected, actual);
    }
    
    @Test
    public void testCheckUnquantInConsequent1() {

        String kifString = "(=>\n" +
                           "  (and\n" +
                           "    (instance ?X Man)\n" +
                           "    (instance ?X Human))\n" +
                           "  (instance ?Y Man))";
        Formula kifFormula = new Formula(kifString);
        List<ErrRec> errorList = new ArrayList<>();
        kfc.CheckUnquantInConsequent("fileName", kifFormula, kifString, 0, errorList);
        ErrRec expected = new ErrRec( ErrRec.ERROR, "fileName", 4, 12, 14, "Unquantified variable in consequent - (instance ?Y Man))");
        ErrRec actual = errorList.get(0);
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testCheckSingleUseVariables1()");
            System.out.println("Expected: " + expected);
            System.out.println("Actual: " + actual);
            System.out.println("Test: " + (expected.equals(actual) ? "PASSED ✅" : "FAILED ❌"));
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testCheckFormulaPreprocess1() {

        String kifString = "(=>\n" +
                           "  (instance ?X Man)\n" +
                           "  (attribute ?X Mortal)";
        Formula kifFormula = new Formula(kifString);
        List<ErrRec> errorList = new ArrayList<>();
        kfc.CheckFormulaPreprocess("fileName", kb, kifFormula, 0, errorList);
        ErrRec expected = new ErrRec( ErrRec.ERROR, "fileName", 1, 3, 11, "Unbalanced parentheses or quotes in: (=>\n" +
                        "  (instance ?X Man)\n" +
                        "  (attribute ?X Mortal)");
        ErrRec actual = errorList.get(0);
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testCheckSingleUseVariables1()");
            System.out.println("Expected: " + expected);
            System.out.println("Actual: " + actual);
            System.out.println("Test: " + (expected.equals(actual) ? "PASSED ✅" : "FAILED ❌"));
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testCheckIsValidFormula1() {
        
        String kifString = "(=>\n" +
                              "(instance ?X Man)\n" + 
                              "(instance ?X Woman)";
        Formula kifFormula = new Formula(kifString);
        List<ErrRec> errorList = new ArrayList<>();
        kfc.CheckIsValidFormula("fileName", kifFormula, 0, kb, kifString, errorList);
        ErrRec expected = new ErrRec(ErrRec.ERROR, "fileName", 0, 1, 2,
            "Parsing error in: Formula:\n" +
            "\tMissed closing parenthesis near line: 1\n" +
            "\tfor token: null and form: null\n" +
            "\tand expression: (=> (instance ?X Man) (instance ?X Woman)\n" +
            "\tand keySet: [ant-instance, cons-instance, arg-0-=>, ant-Man, cons-Woman] (=>\n" +
            "(instance ?X Man)\n" +
            "(instance ?X Woman)");
        // ErrRec actual = new ErrRec();
        // if(!errorList.isEmpty()) actual = errorList.get(0);
        ErrRec actual = errorList.get(0);
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testCheckIsValidFormula1()");
            System.out.println("Expected: " + expected);
            System.out.println("Actual: " + actual);
            System.out.println("Test: " + (expected.equals(actual) ? "PASSED ✅" : "FAILED ❌"));
        }
        assertEquals(expected, actual);
    }

    // @Test
    // public void testCheckIsValidFormula2() {

    //     // String kifString = "(and\n" +
    //     //                       "(instance ?X Man)\n" + 
    //     //                       "(instance ?X Running))";

    //     String kifString = "(=>\n" +
    //                           "(instance ?X Man)\n" + 
    //                           "(instance ?X Running))";
    //     Formula kifFormula = new Formula(kifString);
    //     List<ErrRec> errorList = new ArrayList<>();
    //     kfc.CheckIsValidFormula("fileName", kifFormula, 0, kb, kifString, errorList);
    //     ErrRec expected = new ErrRec(ErrRec.ERROR, "fileName", 0, 1, 2,
    //         "Parsing error in: Formula:\n" +
    //         "\tMissed closing parenthesis near line: 1\n" +
    //         "\tfor token: null and form: null\n" +
    //         "\tand expression: (=> (instance ?X Man) (instance ?X Woman)\n" +
    //         "\tand keySet: [ant-instance, cons-instance, arg-0-=>, ant-Man, cons-Woman] (=>\n" +
    //         "(instance ?X Man)\n" +
    //         "(instance ?X Woman)");
    //     ErrRec actual = new ErrRec();
    //     // if(!errorList.isEmpty()) actual = errorList.get(0);
    //     actual = errorList.get(0);
    //     if (debug) {
    //         System.out.println(divider + "TEST = KifFileCheckerTest.testCheckIsValidFormula2()");
    //         System.out.println("Expected: " + expected);
    //         System.out.println("Actual: " + actual);
    //         System.out.println("Test: " + (expected.equals(actual) ? "PASSED ✅" : "FAILED ❌"));
    //     }
    //     assertEquals(expected, actual);
    // }

    // DOES NOT WORK!!!
    // @Test
    // public void testCheckSUMOtoTFAformErrors1() {

    //     if (debug) System.out.println(divider + "TEST = KifFileCheckerTest.testCheckSUMOtoTFAformerrorList1()");
    //     String kifString = "(and\n" +
    //                           "(instance ?X Man)\n" + 
    //                           "(instance ?X Running))";
    //     Formula kifFormula = new Formula(kifString);
    //     List<ErrRec> errorList = new ArrayList<>();
    //     Set<Formula> processed = kfc.CheckFormulaPreprocess("fileName", kb, kifFormula, 0, errorList);
    //     kfc.CheckSUMOtoTFAformErrors("fileName", kb, kifFormula, 0, processed, errorList);
    //     ErrRec expected = new ErrRec( ErrRec.ERROR, "fileName", 1, 3, 11, 
    //                     "Unbalanced parentheses or quotes in: (=>\n" +
    //                     "  (instance ?X Man)\n" +
    //                     "  (attribute ?X Mortal)");
    //     ErrRec actual = new ErrRec();
    //     if (errorList.get(0) != null) actual = errorList.get(0);
    //     if (debug) {
    //         System.out.println(divider + "TEST = KifFileCheckerTest.testCheckSUMOtoTFAformerrorList1()");
    //         System.out.println("Expected: " + expected);
    //         System.out.println("Actual: " + actual);
    //         System.out.println("Test: " + (expected.equals(actual) ? "PASSED ✅" : "FAILED ❌"));
    //     }
    //     assertEquals(expected, actual);
    // }

    @Test
    public void testCheckTermsBelowEntity1() {

        String kifString = "(instance Shaun Supercalifragilisticexpialidocious)";
        Formula kifFormula = new Formula(kifString);
        Set<String> localIndividuals = new HashSet<>();
        Set<String> localSubclasses = new HashSet<>();
        List<ErrRec> errorList = new ArrayList<>();
        ErrRec expected = new ErrRec(ErrRec.ERROR, "fileName", 0, 16, 50,"Term not below Entity: (instance Shaun Supercalifragilisticexpialidocious)");
        kfc.CheckTermsBelowEntity("fileName", kifFormula, 0, kifString, kb, localIndividuals, localSubclasses, errorList);
        ErrRec actual = errorList.get(0);
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testCheckTermsBelowEntity1()");
            System.out.println("Expected: " + expected);
            System.out.println("Actual: " + actual);
            System.out.println("Test: " + (expected.equals(actual) ? "PASSED ✅" : "FAILED ❌"));
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testCheckSyntaxErrors1() {
        
        String kifString = "(instance Shaun Human";
        List<ErrRec> errorList = new ArrayList<>();
        kfc.CheckSyntaxErrors(kifString, "fileName", errorList);
        ErrRec expected = new ErrRec(ErrRec.ERROR, "fileName", 1, 21, 22,"Parse error at line:charposn 1:21: -> extraneous input '<EOF>' expecting {'(', ')', FUNWORD, IDENTIFIER, NUMBER, STRING, REGVAR, ROWVAR}");
        ErrRec actual = errorList.get(0);
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testCheckSyntaxerrorList1()");
            System.out.println("Expected: " + expected);
            System.out.println("Actual: " + actual);
            System.out.println("Test: " + (expected.equals(actual) ? "PASSED ✅" : "FAILED ❌"));
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testStringToKif1() {

        String kifString = ";Testing\n" + 
                           "(=>\n)" + 
                           "  (instance Shaun Human\n" + 
                           "  (instance Shaun DomesticDog))";
        List<ErrRec> errorList = new ArrayList<>();
        KIF localKif = kfc.StringToKif(kifString, "fileName", errorList);
        ErrRec expected = new ErrRec(ErrRec.ERROR, "fileName", 2, 0, 1, "Parsing error in: Formula: Invalid number of arguments near line: 2 : Wrong number of arguments for '<=>' or '=>' : (=>)");
        ErrRec actual = errorList.get(0);
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testStringToKif()");
            System.out.println("Expected: " + expected);
            System.out.println("Actual: " + actual);
            System.out.println("Test: " + (expected.equals(actual) ? "PASSED ✅" : "FAILED ❌"));
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testextractBufferSlice1() {

        String[] buffer = new String[] {
            "; Comment line",
            "",
            "(=>",
            "  (instance ?X Man)",
            "  (attribute ?X Mortal)",
            ")",
            "",
            "(instance Shaun Human)"
        };
        String expected =
            "(=>\n" +
            "  (instance ?X Man)\n" +
            "  (attribute ?X Mortal)\n" +
            ")\n";
        String actual = kfc.extractBufferSlice(buffer, 3, 6);
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testextractBufferSlice1()");
            System.out.println("Expected:\n" + expected);
            System.out.println("Actual:\n" + actual);
            System.out.println("Test: " + (expected.equals(actual) ? passed : failed));
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testfindLineInFormula1() {

        String formula =
            "(=>\n" +
            "  (instance ?X Man)\n" +
            "  (attribute ?X Happy)\n" +
            ")";
        String term = "?X";
        int[] expected = new int[]{1, 12};
        int[] actual = kfc.findLineInFormula(formula, term);
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testfindLineInFormula1()");
            System.out.println("Formula:\n" + formula);
            System.out.println("Term: " + term);
            System.out.println("Expected: line=" + expected[0] + " col=" + expected[1]);
            System.out.println("Actual:   line=" + actual[0] + " col=" + actual[1]);
            System.out.println("Test: " + ((expected[0] == actual[0] && expected[1] == actual[1]) ? passed : failed));
        }
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testharvestLocalFacts1() {

        String kif =
            "(and\n" +
            "   (instance Shaun Human)\n" +
            "   (subclass Dog Mammal)\n" +
            "   (instance ?X Something)\n" +
            "   (instance \"Bob\" Human)\n" +
            "   (instance 123 Human)\n" +
            ")";
        Formula f = new Formula(kif);
        Set<String> localIndividuals = new HashSet<>();
        Set<String> localSubclasses  = new HashSet<>();
        kfc.harvestLocalFacts(f, localIndividuals, localSubclasses);
        Set<String> expectedIndividuals = new HashSet<>(Arrays.asList("Shaun"));
        Set<String> expectedSubclasses  = new HashSet<>(Arrays.asList("Dog"));
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testharvestLocalFacts1()");
            System.out.println("localIndividuals: " + localIndividuals);
            System.out.println("localSubclasses:  " + localSubclasses);
            System.out.println("Expected Individuals: " + expectedIndividuals);
            System.out.println("Expected Subclasses:  " + expectedSubclasses);
            System.out.println("Test: " + ((localIndividuals.equals(expectedIndividuals) && localSubclasses.equals(expectedSubclasses)) ? passed : failed));
        }
        assertEquals(expectedIndividuals, localIndividuals);
        assertEquals(expectedSubclasses, localSubclasses);
    }

    @Test
    public void testisConst1() {

        String[] trueConsts = {"Human", "Man", "Dog123", "Super_Cat", "foo-bar", "HELLO",};
        String[] nonConsts = {"?X", "@Row", "123", "3.14", "\"Shaun\"", "\"Hello World\""};
        boolean allPassed = true;
        for (String tok : trueConsts) {
            boolean actual = kfc.isConst(tok);
            if (!actual) allPassed = false;
            if (debug) {
                System.out.println(divider + "TEST = KifFileCheckerTest.testisConst1() — TRUE cases");
                System.out.println("Token: " + tok);
                System.out.println("Expected: true");
                System.out.println("Actual:   " + actual);
                System.out.println("Subtest: " + (actual ? passed : failed));
            }
            assertTrue("Expected constant: " + tok, actual);
        }
        for (String tok : nonConsts) {
            boolean actual = KifFileChecker.isConst(tok);
            if (actual) allPassed = false;
            if (debug) {
                System.out.println(divider + "TEST = KifFileCheckerTest.testisConst1() — FALSE cases");
                System.out.println("Token: " + tok);
                System.out.println("Expected: false");
                System.out.println("Actual:   " + actual);
                System.out.println("Subtest: " + (!actual ? passed : failed));
            }
            assertFalse("Expected NOT a constant: " + tok, actual);
        }
    }

    @Test
    public void testFindFormulaInBuffer1() {

        String[] buffer = new String[]{
            "; Comment line",
            "",
            "(=>",
            "  (instance ?X Man)",
            "  (attribute ?X Happy)",
            ")",
            "",
            "(instance Shaun Human)"
        };
        String formula =
            "(=>\n" +
            "  (instance ?X Man)\n" +
            "  (attribute ?X Happy)\n" +
            ")";
        int actual = kfc.findFormulaInBuffer(formula, buffer);
        int expected = 3;
        if (debug) {
            System.out.println(divider + "TEST = testFindFormulaInBuffer1()");
            System.out.println("Expected: " + expected);
            System.out.println("Actual:   " + actual);
            System.out.println("Test: " + ((expected == actual) ? passed : failed));
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testgetOffset1() {

        String errorLine = "Parse error at line:charposn 1:21: -> extraneous input '<EOF>' "
            + "expecting {'(', ')', FUNWORD, IDENTIFIER, NUMBER, STRING, REGVAR, ROWVAR}";
        int actual = kfc.getOffset(errorLine);
        int expected = 21;
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testgetOffset1()");
            System.out.println("Input line:    " + errorLine);
            System.out.println("Expected col:  " + expected);
            System.out.println("Actual col:    " + actual);
            System.out.println("Test: " + ((expected == actual) ? passed : failed));
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testgetLineNum1() {

        String errorLine = "1:21: Parse error at line:charposn 1:21: -> extraneous input '<EOF>' "
            + "expecting {'(', ')', FUNWORD, IDENTIFIER, NUMBER, STRING, REGVAR, ROWVAR}";
        int actual = kfc.getLineNum(errorLine);
        int expected = 1;
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testgetLineNum1()");
            System.out.println("Input line:     " + errorLine);
            System.out.println("Expected line:  " + expected);
            System.out.println("Actual line:    " + actual);
            System.out.println("Test: " + ((expected == actual) ? passed : failed));
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testIsFileInKB_constituentFound() {

        boolean actual = KifFileChecker.isFileInKB("Merge.kif", kb);
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testIsFileInKB_constituentFound()");
            System.out.println("Expected: true");
            System.out.println("Actual:   " + actual);
            System.out.println("Test: " + (actual ? passed : failed));
        }
        assertTrue("Merge.kif should be found in KB constituents", actual);
    }

    @Test
    public void testIsFileInKB_constituentNotFound() {

        boolean actual = KifFileChecker.isFileInKB("NonExistentFile.kif", kb);
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testIsFileInKB_constituentNotFound()");
            System.out.println("Expected: false");
            System.out.println("Actual:   " + actual);
            System.out.println("Test: " + (!actual ? passed : failed));
        }
        assertFalse("NonExistentFile.kif should NOT be found in KB constituents", actual);
    }

    @Test
    public void testIsFileInKB_nullInputs() {

        assertFalse("null fileName should return false",
            KifFileChecker.isFileInKB(null, kb));
        assertFalse("null kb should return false",
            KifFileChecker.isFileInKB("Merge.kif", null));
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testIsFileInKB_nullInputs()");
            System.out.println("Test: " + passed);
        }
    }

    @Test
    public void testIsFileInKB_fullPathMatchesBasename() {

        boolean actual = KifFileChecker.isFileInKB("/some/path/to/Merge.kif", kb);
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testIsFileInKB_fullPathMatchesBasename()");
            System.out.println("Expected: true (full path should match by basename)");
            System.out.println("Actual:   " + actual);
            System.out.println("Test: " + (actual ? passed : failed));
        }
        assertTrue("Full path /some/path/to/Merge.kif should match constituent Merge.kif by basename", actual);
    }

    @Test
    public void testCheckNotInKB_warningEmitted() {

        // KIF content with custom relations that will trigger "no type information" errors
        String kifString =
            "(=>\n" +
            "  (myCustomRelation ?X ?Y)\n" +
            "  (myOtherCustomRelation ?Y ?Z))\n" +
            "(=>\n" +
            "  (anotherFakeRelation ?A ?B)\n" +
            "  (yetAnotherFake ?B ?C))";
        List<ErrRec> msgs = KifFileChecker.check(kifString, "NotInKB.kif");
        // Count warnings about file not being in KB
        long notInKBWarnings = msgs.stream()
            .filter(e -> e.type == ErrRec.WARNING && e.msg != null
                    && e.msg.startsWith("This file is not loaded into the KB"))
            .count();
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testCheckNotInKB_warningEmitted()");
            System.out.println("Total messages: " + msgs.size());
            System.out.println("'Not in KB' warnings: " + notInKBWarnings);
            for (ErrRec e : msgs)
                System.out.println("  " + e);
        }
        // Should have exactly one "not in KB" warning if there are type errors
        long typeErrs = msgs.stream()
            .filter(e -> e.type == ErrRec.ERROR && e.msg != null
                    && e.msg.contains("no type information for arg"))
            .count();
        if (typeErrs > 1) {
            assertEquals("Should have exactly 1 'not in KB' warning when type errors > 1",
                1, notInKBWarnings);
            // Verify it's the first message
            assertTrue("Warning should be first in the list",
                msgs.get(0).type == ErrRec.WARNING
                && msgs.get(0).msg.startsWith("This file is not loaded into the KB"));
            if (debug) System.out.println("Test: " + passed);
        }
        else {
            assertEquals("Should have no 'not in KB' warning when type errors <= 1",
                0, notInKBWarnings);
            if (debug) System.out.println("Test: " + passed + " (not enough type errors to trigger warning)");
        }
    }

    @Test
    public void testCheckInKB_noWarning() {

        // Use a file name that IS in the KB
        String kifString = "(instance Shaun Human)";
        List<ErrRec> msgs = KifFileChecker.check(kifString, "Merge.kif");
        long notInKBWarnings = msgs.stream()
            .filter(e -> e.type == ErrRec.WARNING && e.msg != null
                    && e.msg.startsWith("This file is not loaded into the KB"))
            .count();
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testCheckInKB_noWarning()");
            System.out.println("Messages: " + msgs.size());
            System.out.println("'Not in KB' warnings: " + notInKBWarnings);
            System.out.println("Test: " + (notInKBWarnings == 0 ? passed : failed));
        }
        assertEquals("File in KB should produce no 'not in KB' warning", 0, notInKBWarnings);
    }

    @Test
    public void testCheckBuffer_noWarning() {

        // The (buffer) sentinel should never trigger the warning
        String kifString =
            "(=>\n" +
            "  (myCustomRelation ?X ?Y)\n" +
            "  (myOtherCustomRelation ?Y ?Z))";
        List<ErrRec> msgs = KifFileChecker.check(kifString, "(buffer)");
        long notInKBWarnings = msgs.stream()
            .filter(e -> e.type == ErrRec.WARNING && e.msg != null
                    && e.msg.startsWith("This file is not loaded into the KB"))
            .count();
        if (debug) {
            System.out.println(divider + "TEST = KifFileCheckerTest.testCheckBuffer_noWarning()");
            System.out.println("'Not in KB' warnings: " + notInKBWarnings);
            System.out.println("Test: " + (notInKBWarnings == 0 ? passed : failed));
        }
        assertEquals("(buffer) sentinel should not trigger 'not in KB' warning", 0, notInKBWarnings);
    }
}