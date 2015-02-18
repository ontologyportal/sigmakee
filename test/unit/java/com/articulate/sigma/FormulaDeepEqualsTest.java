package com.articulate.sigma;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
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
}
