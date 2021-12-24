package com.articulate.sigma.trans;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import com.articulate.sigma.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;

public class TPTP2SUMOTest {

    /****************************************************************
     */
    @BeforeClass
    public static void init() {

    }

    /** *************************************************************
     */
    @Ignore
    @Test
    public void testPartition() {

        System.out.println("TPTP2SUMOTest.testPartition()");
        String input = "fof(f658,plain,(" +
                "sQ4_eqProxy(s__BobTheGolfer,s__JohnTheGolfer) | " +
                "sQ4_eqProxy(s__JohnsGolfGame,s__BobsGolfGame) | " +
                "~s__instance(s__JohnTheGolfer,s__Human) | " +
                "~s__instance(s__JohnsGolfGame,s__Golf) | " +
                "~ans0(s__BobTheGolfer,s__JohnsGolfGame,s__BobsGolfGame,s__JohnTheGolfer))).";
        try {
            // kif = TPTP2SUMO.convert(reader, false);
            tptp_parser.TPTPVisitor sv = new tptp_parser.TPTPVisitor();
            sv.parseString(input);
            HashMap<String, tptp_parser.TPTPFormula> hm = sv.result;
            for (String s : hm.keySet()) {
                System.out.println(hm.get(s));
                System.out.println("\t" + hm.get(s).sumo + "\n");
                System.out.println(TPTP2SUMO.collapseConnectives(new Formula(hm.get(s).sumo)));
            }
        }
        catch (Exception e) {
            System.out.println("e: " + e);
        }
    }

    /** *************************************************************
     */
    @Ignore
    @Test
    public void testCollapse() {

        System.out.println("TPTP2SUMOTest.testPartition()");
        Formula f = new Formula("(and (and (foo A B) (foo B B)) (bar C))");
        String result = TPTP2SUMO.collapseConnectives(f).toString();
        String expected = "(and\n" +
                "  (foo A B)\n" +
                "  (foo B B)\n" +
                "  (bar C))";
        System.out.println("result: " + result);
        assertEquals(expected,result);
    }

    /** *************************************************************
     */
    @Ignore
    @Test
    public void testCollapse2() {

        System.out.println("TPTP2SUMOTest.testPartition2()");
        Formula f = new Formula("(=> (and (and (foo A B) (foo B B)) (bar C)) (blah F G))");
        String result = TPTP2SUMO.collapseConnectives(f).toString();
        String expected = "(=>\n" +
                "  (and\n" +
                "    (foo A B)\n" +
                "    (foo B B)\n" +
                "    (bar C))\n" +
                "  (blah F G))";
        System.out.println("result: " + result);
        assertEquals(expected,result);
    }

    /** *************************************************************
     */
    @Test
    public void testCollapse3() {

        KBmanager.prefOverride.put("loadLexicons","false");
        KBmanager.getMgr().initializeOnce();
        System.out.println("TPTP2SUMOTest.testPartition3()");
        Formula f = new Formula("(forall (?X155 ?X156 ?X157 ?X158)\n" +
                "  (=>\n" +
                "    (and\n" +
                "      (instance ?X158 Organism)\n" +
                "      (instance ?X155 Organism))\n" +
                "    (=>\n" +
                "      (and\n" +
                "        (and\n" +
                "          (and\n" +
                "            (and\n" +
                "              (and\n" +
                "                (and\n" +
                "                  (and\n" +
                "                    (instance ?X157 Golf)\n" +
                "                    (instance ?X156 Golf))\n" +
                "                  (not\n" +
                "                    (equal ?X155 ?X158)))\n" +
                "                (not\n" +
                "                  (equal ?X156 ?X157)))\n" +
                "              (plays ?X157 ?X155))\n" +
                "            (plays ?X156 ?X158))\n" +
                "          (inhabits ?X155 UnitedKingdom))\n" +
                "        (inhabits ?X158 UnitedKingdom))\n" +
                "      (exists (?X159)\n" +
                "        (and\n" +
                "          (and\n" +
                "            (and\n" +
                "              (and\n" +
                "                (plays ?X159 ?X155)\n" +
                "                (plays ?X159 ?X158))\n" +
                "              (instance ?X159 Golf))\n" +
                "            (located ?X159 UnitedKingdom))\n" +
                "          (instance ?X159 TournamentSport))))))");
        String result = TPTP2SUMO.collapseConnectives(f).toString();
        String expected = "(forall (?A1 ?G1 ?G2 ?A2)\n" +
                "  (=>\n" +
                "    (and\n" +
                "      (instance ?A2 Organism)\n" +
                "      (instance ?A1 Organism))\n" +
                "(=>\n" +
                "  (and\n" +
                "    (inhabits ?A1 UnitedKingdom)\n" +
                "    (inhabits ?A2 UnitedKingdom)\n" +
                "    (plays ?G1 ?A1)\n" +
                "    (plays ?G2 ?A2)\n" +
                "    (not \n" +
                "      (equal ?G1 ?G2))\n" +
                "    (not \n" +
                "      (equal ?A1 ?A2))\n" +
                "    (instance ?G1 Golf)\n" +
                "    (instance ?G2 Golf))\n" +
                "  (exists (?G3)\n" +
                "    (and\n" +
                "      (instance ?G3 TournamentSport)\n" +
                "      (located ?G3 UnitedKingdom)\n" +
                "      (instance ?G3 Golf)\n" +
                "      (plays ?G3 ?A1)\n" +
                "      (plays ?G3 ?A2))))))";
        System.out.println("result: " + result);
        System.out.println("expected: " + expected);
        Formula fresult = new Formula(result);
        Formula fexpected = new Formula(expected);
        assertTrue(fexpected.deepEquals(fresult));
    }
}
