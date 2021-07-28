package com.articulate.sigma.trans;

import TPTPWorld.TPTPFormula;
import TPTPWorld.TPTPParser;
import org.junit.BeforeClass;
import org.junit.Test;
import com.articulate.sigma.*;

import java.io.BufferedReader;
import java.io.StringReader;

public class TPTP2SUMOTest {

    /****************************************************************
     */
    @BeforeClass
    public static void init() {

    }

    /** *************************************************************
     */
    @Test
    public void testPartition() {

        String input = "fof(f658,plain,(" +
                "sQ4_eqProxy(s__BobTheGolfer,s__JohnTheGolfer) | " +
                "sQ4_eqProxy(s__JohnsGolfGame,s__BobsGolfGame) | " +
                "~s__instance(s__JohnTheGolfer,s__Human) | " +
                "~s__instance(s__JohnsGolfGame,s__Golf) | " +
                "~ans0(s__BobTheGolfer,s__JohnsGolfGame,s__BobsGolfGame,s__JohnTheGolfer))).";
        try {
            StringReader reader = new StringReader(input);
            // kif = TPTP2SUMO.convert(reader, false);
            TPTPParser tptpP = TPTPParser.parse(new BufferedReader(reader));
            System.out.println(tptpP.Items.get(0));
            for (String id : tptpP.ftable.keySet()) {
                TPTPFormula tptpF = tptpP.ftable.get(id);
                String kif = TPTP2SUMO.convertType(tptpF, 0, 0, true).toString();
                System.out.println(TPTP2SUMO.collapseConnectives(new Formula(kif)));
            }
        }
        catch (Exception e) {
            System.out.println("e: " + e);
        }
    }
}
