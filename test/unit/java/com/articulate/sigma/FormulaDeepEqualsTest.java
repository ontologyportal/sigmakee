package com.articulate.sigma;

import org.junit.Test;
import org.junit.Ignore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by sserban on 2/11/15.
 */
public class FormulaDeepEqualsTest extends UnitTestBase{

    /***************************************************************
     */
    @Test
    public void testDeepEquals() {

        Formula f1 = new Formula();
        f1.read("(=>" +
                "    (instance ?C WalkingCane)" +
                "    (hasPurpose ?C" +
                "        (exists (?W)" +
                "            (and" +
                "                (instance ?W Walking)" +
                "                (instrument ?W ?C)))))");

        Formula f2 = new Formula();
        f2.read("(=>" +
                "    (instance ?C WalkingCane)" +
                "    (hasPurpose ?C" +
                "        (exists (?W)" +
                "            (and" +
                "                (instance ?W Walking)" +
                "                (instrument ?W ?C)))))");

        //testing equal formulas
        assertTrue(f1.deepEquals(f1));

        //testing formulas that differ in variable reference
        f2.read("(or (not (instance ?X6 WalkingCane)) (hasPurpose ?X4 (and (instance (SkFn2 ?X6) Walking) (instrument (SkFn2 ?X6) ?X6))))");
        assertTrue(f1.deepEquals(f2));

        //testing unequal formulas
        f1 = new Formula();
        f1.read("(=>" +
                "    (instance ?C WalkingCane)" +
                "    (hasPurpose ?C" +
                "        (exists (?W)" +
                "            (and" +
                "                (instance ?W Walking)" +
                "                (instrument ?W ?C)))))");

        f2 = new Formula();
        f2.read("(=>" +
                "    (instance ?C WalkingCane)" +
                "    (hasPurpose ?C" +
                "        (exists (?W)" +
                "            (and" +
                "                (instance ?W Running)" +
                "                (instrument ?W ?C)))))");

        assertFalse(f1.deepEquals(f2));

        //testing commutative terms
        f1 = new Formula();
        f1.read("(=>" +
                "    (instance ?C WalkingCane)" +
                "    (hasPurpose ?C" +
                "        (exists (?W)" +
                "            (and" +
                "                (instance ?W Walking)" +
                "                (instrument ?W ?C)))))");

        f2 = new Formula();
        f2.read("(=>" +
                "    (instance ?C WalkingCane)" +
                "    (hasPurpose ?C" +
                "        (exists (?W)" +
                "            (and" +
                "                (instrument ?W ?C)" +
                "                (instance ?W Walking)))))");

        assertTrue(f1.deepEquals(f2));

    }

    /***************************************************************
     */
    @Test
    public void testDeepEquals2() {

        System.out.println("============= FormulaDeepEqualsTest.testDeepEquals2 ==================");
        Formula f1 = new Formula();
        f1.read("(exists (?Leigh-1 ?baby-4 ?blankets-6 ?swaddled-2)\n" +
                "  (and\n" +
                "    (orientation ?swaddled-2 ?blankets-6 Inside)\n" +
                "    (destination ?swaddled-2 ?blankets-6)\n" +
                "    (names ?Leigh-1 \"Leigh\")\n" +
                "    (instance ?baby-4 HumanBaby)\n" +
                "    (agent ?swaddled-2 ?Leigh-1)\n" +
                "    (patient ?swaddled-2 ?baby-4)\n" +
                "    (earlier\n" +
                "      (WhenFn ?swaddled-2) Now)\n" +
                "    (instance ?blankets-6 Blanket)\n" +
                "    (instance ?Leigh-1 Human)\n" +
                "    (instance ?swaddled-2 Covering)))");

        Formula f2 = new Formula();
        f2.read("(exists (?Leigh-1 ?baby-4 ?blankets-6 ?swaddled-2)\n" +
                "  (and\n" +
                "    (orientation ?swaddled-2 ?blankets-6 Inside)\n" +
                "    (destination ?swaddled-2 ?blankets-6)\n" +
                "    (names ?Leigh-1 \"Leigh\")\n" +
                "    (instance ?swaddled-2 Covering)\n" +
                "    (agent ?swaddled-2 ?Leigh-1)\n" +
                "    (patient ?swaddled-2 ?baby-4)\n" +
                "    (earlier\n" +
                "      (WhenFn ?swaddled-2) Now)\n" +
                "    (instance ?blankets-6 Blanket)\n" +
                "    (instance ?Leigh-1 Human)\n" +
                "    (instance ?baby-4 HumanBaby)) )");
        Formula.debug = true;
        //testing equal formulas
        assertTrue(f1.deepEquals(f2));
        Formula.debug = false;
    }

    /***************************************************************
     */
    @Test
    public void testDeepEqualsErrorCases(){

        Formula f = new Formula();
        f.read("(<=> (instance ?REL SymmetricRelation) (forall (?INST1 ?INST2) (=> (?REL ?INST1 ?INST2) (?REL ?INST2 ?INST1)))))");

        assertFalse(f.deepEquals(null));

        Formula compared = new Formula();
        assertFalse(f.deepEquals(compared));

        compared.read("");
        assertFalse(f.deepEquals(compared));

        compared.read("()");
        assertFalse(f.deepEquals(compared));

        assertTrue(f.deepEquals(f));
    }

    /***************************************************************
     */
    @Test
    public void testLogicallyEqualsErrorCases() {

        Formula f = new Formula();
        f.read("(<=> (instance ?REL SymmetricRelation) (forall (?INST1 ?INST2) (=> (?REL ?INST1 ?INST2) (?REL ?INST2 ?INST1)))))");

        assertFalse(f.logicallyEquals((Formula)null));

        Formula compared = new Formula();
        assertFalse(f.logicallyEquals(compared));

        compared.read("");
        assertFalse(f.logicallyEquals(compared));

        compared.read("()");
        assertFalse(f.logicallyEquals(compared));

        assertTrue(f.logicallyEquals(f));
    }

    /***************************************************************
     */
    @Test
    public void testUnifyWith() {

        Formula f1 = new Formula();
        f1.read("(=>" +
                "    (instance ?C WalkingCane)" +
                "    (hasPurpose ?C" +
                "        (exists (?W)" +
                "            (and" +
                "                (instance ?W Walking)" +
                "                (instrument ?W ?C)))))");

        Formula f2 = new Formula();
        f2.read("(=>" +
                "    (instance ?C WalkingCane)" +
                "    (hasPurpose ?C" +
                "        (exists (?W)" +
                "            (and" +
                "                (instance ?W Walking)" +
                "                (instrument ?W ?C)))))");

        //testing equal formulas
        assertTrue(f1.unifyWith(f2));

        //testing formulas that differ in variable reference
        f2.read("(or (not (instance ?X6 WalkingCane)) (hasPurpose ?X4 (and (instance (SkFn2 ?X6) Walking) (instrument (SkFn2 ?X6) ?X6))))");
        assertFalse(f1.unifyWith(f2));

        //testing unequal formulas
        f1 = new Formula();
        f1.read("(=>" +
                "    (instance ?C WalkingCane)" +
                "    (hasPurpose ?C" +
                "        (exists (?W)" +
                "            (and" +
                "                (instance ?W Walking)" +
                "                (instrument ?W ?C)))))");

        f2 = new Formula();
        f2.read("(=>" +
                "    (instance ?C WalkingCane)" +
                "    (hasPurpose ?C" +
                "        (exists (?W)" +
                "            (and" +
                "                (instance ?W Running)" +
                "                (instrument ?W ?C)))))");

        assertFalse(f1.unifyWith(f2));

        //testing commutative terms
        f1 = new Formula();
        f1.read("(=>" +
                "    (instance ?C WalkingCane)" +
                "    (hasPurpose ?C" +
                "        (exists (?W)" +
                "            (and" +
                "                (instance ?W Walking)" +
                "                (instrument ?W ?C)))))");

        f2 = new Formula();
        f2.read("(=>" +
                "    (instance ?C WalkingCane)" +
                "    (hasPurpose ?C" +
                "        (exists (?W)" +
                "            (and" +
                "                (instrument ?W ?C)" +
                "                (instance ?W Walking)))))");

        assertTrue(f1.unifyWith(f2));

    }

    /***************************************************************
     * Formula.unifyWith is deprecated
     */
    @Ignore
    @Test
    public void testUnifyWithMiscPredicates() {

        String s1 = "(=> (and (instance ?X4 Dog) (instance ?X5 Cat)) (equal ?X4 ?X5))";
        Formula f1 = new Formula();
        f1.read(s1);

        String s2 = "(=> (and (instance ?X11 Dog) (instance ?X12 Cat)) (equal ?X12 ?X11))";
        Formula f2 = new Formula();
        f2.read(s2);

        Formula.debug = true;
        //System.out.println("testUnifyWithMiscPredicates(): deepEquals: " +  f1.deepEquals(f2));
        long start = System.nanoTime();
        assertTrue(f1.unifyWith(f2));
        long stop = System.nanoTime();
        System.out.println("Execution time (in microseconds): " + ((stop - start) / 1000));
   }

    /***************************************************************
     */
    @Test
    public void testLogicallyEqualsPerformance() {

        String stmt = "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) " +
                "(element ?ELEMENT ?SET2))) (equal ?SET1 ?SET2))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Formula expected = new Formula();
        String expectedString = "(=> (and (instance ?SET1 Set) (instance ?SET2 Set)) " +
                "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) (element ?ELEMENT ?SET2))) " +
                "(equal ?SET1 ?SET2)))";

        expected.read(expectedString);

        Formula actual = fp.addTypeRestrictions(f, SigmaTestBase.kb);
        System.out.println("testLogicallyEqualsPerformance: expected: " + expected);
        System.out.println("testLogicallyEqualsPerformance: actual: " + actual);
        long start = System.nanoTime();
//        assertTrue(expected.logicallyEquals(actual));
        assertTrue(expected.unifyWith(actual));
        long stop = System.nanoTime();
        System.out.println("Execution time (in microseconds): " + ((stop - start) / 1000));
    }
}
