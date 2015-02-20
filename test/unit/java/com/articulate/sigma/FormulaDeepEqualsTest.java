package com.articulate.sigma;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by sserban on 2/11/15.
 */
public class FormulaDeepEqualsTest extends UnitTestBase{

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

    @Test
    public void testLogicallyEqualsErrorCases(){
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
        assertTrue(f1.unifyWith(f1));

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

    @Test
    public void testUnifyWithOnAnd() {
        String s1 = "(=>\n" +
                "  (and\n" +
                "    (instance ?SET1 Set)\n" +
                "    (instance ?SET2 Set)\n" +
                "    (instance ?ELEMENT Entity))\n" +
                "  (=>\n" +
                "    (forall (?ELEMENT)\n" +
                "      (<=>\n" +
                "        (element ?ELEMENT ?SET1)\n" +
                "        (element ?ELEMENT ?SET2)))\n" +
                "    (equal ?SET1 ?SET2)))";
        Formula f1 = new Formula();
        f1.read(s1);

        String s2 = "(=>\n" +
                "  (and\n" +
                "    (instance ?SET1 Set)\n" +
                "    (instance ?SET2 Set)\n" +
                "    (instance ?ELEMENT Entity))\n" +
                "  (=>\n" +
                "    (forall (?ELEMENT)\n" +
                "      (<=>\n" +
                "        (element ?ELEMENT ?SET1)\n" +
                "        (element ?ELEMENT ?SET2)))\n" +
                "    (equal ?SET1 ?SET2)))";
        Formula f2 = new Formula();
        f2.read(s2);

        long start = System.nanoTime();
        assertTrue(f1.unifyWith(f2));
        long stop = System.nanoTime();
        System.out.println("Execution time (in microseconds): " + ((stop - start)/1000));

    }

    @Test
    public void testLogicallyEqualsPerformance() {
        String stmt = "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) " +
                "(element ?ELEMENT ?SET2))) (equal ?SET1 ?SET2))";
        Formula f = new Formula();
        f.read(stmt);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        Formula expected = new Formula();
//        String expectedString = "(=> (and (instance ?SET2 Set) (instance ?ELEMENT Entity) (instance ?SET1 Set)) " +
//                "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) (element ?ELEMENT ?SET2))) " +
//                "(equal ?SET1 ?SET2)))";
        String expectedString = "(=> (and  (instance ?SET1 Set) (instance ?SET2 Set) (instance ?ELEMENT Entity)) " +
                "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) (element ?ELEMENT ?SET2))) " +
                "(equal ?SET1 ?SET2)))";
        expected.read(expectedString);

        Formula actual = fp.addTypeRestrictionsNew(f, SigmaTestBase.kb);
        long start = System.nanoTime();
//        assertTrue(expected.logicallyEquals(actual));
        assertTrue(expected.unifyWith(actual));
        long stop = System.nanoTime();
        System.out.println("Execution time (in microseconds): " + ((stop - start)/1000));

    }
}
