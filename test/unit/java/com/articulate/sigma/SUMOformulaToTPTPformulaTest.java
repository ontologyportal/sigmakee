package com.articulate.sigma;

import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * Created by qingqingcai on 1/14/15.
 */
public class SUMOformulaToTPTPformulaTest {

    @Test
    public void TesttptpParseSUOKIFString() {

        String kifstring, expectedRes, actualRes;

        // test1: passed
        kifstring = "(=> " +
                "(instance ?X P)" +
                "(instance ?X Q))";
        expectedRes= "( ( ! [V__X] : (s__instance(V__X,s__P) => s__instance(V__X,s__Q)) ) )";
        actualRes = SUMOformulaToTPTPformula.tptpParseSUOKIFString(kifstring, false);
        assertEquals(expectedRes, actualRes);

        // test2: passed
        kifstring = "(=> " +
                "(or" +
                "(instance ?X Q)" +
                "(instance ?X R))" +
                "(instance ?X ?T))";
        expectedRes = "( ( ! [V__X,V__T] : ((s__instance(V__X,s__Q) | s__instance(V__X,s__R)) => s__instance(V__X,V__T)) ) )";
        actualRes = SUMOformulaToTPTPformula.tptpParseSUOKIFString(kifstring, false);
        assertEquals(expectedRes, actualRes);


        // test3: passed
        kifstring = "(or " +
                "(not " +
                "(instance ?X Q))" +
                "(instance ?X R))";
        expectedRes = "( ( ! [V__X] : ((~ s__instance(V__X,s__Q)) | s__instance(V__X,s__R)) ) )";
        actualRes = SUMOformulaToTPTPformula.tptpParseSUOKIFString(kifstring, false);
        assertEquals(expectedRes, actualRes);
    }
}
