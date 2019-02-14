package com.articulate.sigma.trans;

import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by qingqingcai on 1/14/15.
 */
public class SUMOformulaToTPTPformulaTest {

    /** ***************************************************************
     */
    @Test
    public void TesttptpParseSUOKIFString1() {

        String kifstring, expectedRes, actualRes;

        // test1: passed
        kifstring = "(=> " +
                "(instance ?X P)" +
                "(instance ?X Q))";
        expectedRes = "( ( ! [V__X] : (s__instance(V__X,s__P) => s__instance(V__X,s__Q)) ) )";
        actualRes = SUMOformulaToTPTPformula.tptpParseSUOKIFString(kifstring, false);
        System.out.println(actualRes);
        assertEquals(expectedRes, actualRes);
    }

    /** ***************************************************************
     */
    @Test
    public void TesttptpParseSUOKIFString2() {

        String kifstring, expectedRes, actualRes;
        // test2: passed
        kifstring = "(=> " +
                "(or" +
                "(instance ?X Q)" +
                "(instance ?X R))" +
                "(instance ?X ?T))";
        expectedRes = "( ( ! [V__X,V__T] : ((s__instance(V__X,s__Q) | s__instance(V__X,s__R)) => s__instance(V__X,V__T)) ) )";
        actualRes = SUMOformulaToTPTPformula.tptpParseSUOKIFString(kifstring, false);
        System.out.println(actualRes);
        assertEquals(expectedRes, actualRes);
    }

    /** ***************************************************************
     */
    @Test
    public void TesttptpParseSUOKIFString3() {

        String kifstring, expectedRes, actualRes;
        // test3: passed
        kifstring = "(or " +
                "(not " +
                "(instance ?X Q))" +
                "(instance ?X R))";
        expectedRes = "( ( ! [V__X] : ((~ s__instance(V__X,s__Q)) | s__instance(V__X,s__R)) ) )";
        actualRes = SUMOformulaToTPTPformula.tptpParseSUOKIFString(kifstring, false);
        System.out.println(actualRes);
        assertEquals(expectedRes, actualRes);
    }

    /** ***************************************************************
     */
    @Test
    public void TesttptpParseSUOKIFString4() {

        String kifstring, expectedRes, actualRes;
        // test3: passed
        kifstring = "(<=>\n" +
                "    (instance ?NUMBER NegativeRealNumber)\n" +
                "    (and\n" +
                "        (lessThan ?NUMBER 0)\n" +
                "        (instance ?NUMBER RealNumber)))";
        expectedRes = "( ( ! [V__NUMBER] : (s__instance(V__NUMBER,s__NegativeRealNumber) <=> (less(V__NUMBER,n__0) & s__instance(V__NUMBER,s__RealNumber))) ) )";
        actualRes = SUMOformulaToTPTPformula.tptpParseSUOKIFString(kifstring, false);
        System.out.println(actualRes);
        assertEquals(expectedRes, actualRes);
    }

    /** ***************************************************************
     */
    @Test
    public void TesttptpParseSUOKIFString5() {

        String kifstring, expectedRes, actualRes;
        // test3: passed
        kifstring = "(<=>\n" +
                "    (instance ?NUMBER NegativeRealNumber)\n" +
                "    (and\n" +
                "        (lessThan ?NUMBER 0.001)\n" +    // nonsense just to test number hiding
                "        (instance ?NUMBER RealNumber)))";
        expectedRes = "( ( ! [V__NUMBER] : (s__instance(V__NUMBER,s__NegativeRealNumber) <=> (less(V__NUMBER,n__0_001) & s__instance(V__NUMBER,s__RealNumber))) ) )";
        actualRes = SUMOformulaToTPTPformula.tptpParseSUOKIFString(kifstring, false);
        System.out.println(actualRes);
        assertEquals(expectedRes, actualRes);
    }

    /** ***************************************************************
     */
    @Test
    public void sumo2TPTPhigherOrder() {

        String kifstring, expectedRes, actualRes;

        kifstring = "(=> (and (instance ?GUN Gun) (effectiveRange ?GUN ?LM) " +
                "(distance ?GUN ?O ?LM1) (instance ?O Organism) (not (exists (?O2) " +
                "(between ?O ?O2 ?GUN))) (lessThanOrEqualTo ?LM1 ?LM)) " +
                "(capability (KappaFn ?KILLING (and (instance ?KILLING Killing) " +
                "(patient ?KILLING ?O))) instrument ?GUN))";
        expectedRes = "";
        actualRes = SUMOformulaToTPTPformula.tptpParseSUOKIFString(kifstring, false);
        System.out.println(actualRes);
        assertEquals(expectedRes, actualRes);
    }
}
