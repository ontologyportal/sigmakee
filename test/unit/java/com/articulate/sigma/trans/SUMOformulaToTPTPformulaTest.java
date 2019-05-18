package com.articulate.sigma.trans;

import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by qingqingcai on 1/14/15. Copyright IPsoft 2015
 * //This software is released under the GNU Public License
 * //<http://www.gnu.org/copyleft/gpl.html>.
 * // Copyright 2019 Infosys
 * // adam.pease@infosys.com
 */
public class SUMOformulaToTPTPformulaTest {

    /** ***************************************************************
     */
    @Test
    public void TesttptpParseSUOKIFString1() {

        String kifstring, expectedRes, actualRes;

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
        kifstring = "(<=>\n" +
                "    (instance ?NUMBER NegativeRealNumber)\n" +
                "    (and\n" +
                "        (lessThan ?NUMBER 0)\n" +
                "        (instance ?NUMBER RealNumber)))";
        expectedRes = "( ( ! [V__NUMBER] : (s__instance(V__NUMBER,s__NegativeRealNumber) <=> " +
                "(s__lessThan(V__NUMBER,0) & s__instance(V__NUMBER,s__RealNumber))) ) )";
        actualRes = SUMOformulaToTPTPformula.tptpParseSUOKIFString(kifstring, false);
        System.out.println(actualRes);
        assertEquals(expectedRes, actualRes);
    }

    /** ***************************************************************
     */
    @Test
    public void TesttptpParseSUOKIFString5() {

        String kifstring, expectedRes, actualRes;
        kifstring = "(<=>\n" +
                "    (instance ?NUMBER NegativeRealNumber)\n" +
                "    (and\n" +
                "        (lessThan ?NUMBER 0.001)\n" +
                "        (instance ?NUMBER RealNumber)))";
        expectedRes = "( ( ! [V__NUMBER] : (s__instance(V__NUMBER,s__NegativeRealNumber) <=> " +
                "(s__lessThan(V__NUMBER,0.001) & s__instance(V__NUMBER,s__RealNumber))) ) )";
        actualRes = SUMOformulaToTPTPformula.tptpParseSUOKIFString(kifstring, false);
        System.out.println(actualRes);
        assertEquals(expectedRes, actualRes);
    }

    /** ***************************************************************
     */
    @Test
    public void TesttptpParseSUOKIFString6() {

        String kifstring, expectedRes, actualRes;
        kifstring = "(<=> (temporalPart ?POS (WhenFn ?THING)) (time ?THING ?POS))";
        expectedRes = "( ( ! [V__POS,V__THING] : (s__temporalPart(V__POS,s__WhenFn(V__THING)) <=> s__time(V__THING,V__POS)) ) )";
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
        expectedRes = "( ( ! [V__GUN,V__LM,V__O,V__LM1,V__KILLING] : " +
                "((s__instance(V__GUN,s__Gun) & s__effectiveRange(V__GUN,V__LM) & " +
                "s__distance(V__GUN,V__O,V__LM1) & s__instance(V__O,s__Organism) & " +
                "(~ (? [V__O2] : s__between(V__O,V__O2,V__GUN))) & " +
                "s__lessThanOrEqualTo(V__LM1,V__LM)) => " +
                "s__capability(s__KappaFn(V__KILLING,(s__instance(V__KILLING,s__Killing) & " +
                "s__patient(V__KILLING,V__O))),s__instrument__m,V__GUN)) ) )";
        actualRes = SUMOformulaToTPTPformula.tptpParseSUOKIFString(kifstring, false);
        System.out.println(actualRes);
        assertEquals(expectedRes, actualRes);
    }
}
