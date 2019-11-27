package com.articulate.sigma.trans;

import com.articulate.sigma.StringUtil;
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
    public void test(String kif, String expected, String label) {

        System.out.println("=============================");
        System.out.println("SUMOformulaToTPTPformulaTest: " + label);
        System.out.println();
        String actualRes = SUMOformulaToTPTPformula.tptpParseSUOKIFString(kif, false);
        if (!StringUtil.emptyString(actualRes))
            actualRes = actualRes.replaceAll("  "," ");
        System.out.println("Actual:   " + actualRes);
        System.out.println("Expected: " + expected);
        if (!StringUtil.emptyString(actualRes) && actualRes.equals(expected))
            System.out.println(label + " : Success");
        else
            System.out.println(label + " : fail!");
        assertEquals(expected, actualRes);
    }

    /** ***************************************************************
     */
    @Test
    public void string1() {

        String kifstring, expectedRes, actualRes;

        kifstring = "(=> " +
                "(instance ?X P)" +
                "(instance ?X Q))";
        expectedRes = "( ( ! [V__X] : (s__instance(V__X,s__P) => s__instance(V__X,s__Q)) ) )";
        test(kifstring,expectedRes,"string1");
    }

    /** ***************************************************************
     */
    @Test
    public void string2() {

        String kifstring, expectedRes, actualRes;
        kifstring = "(=> " +
                "(or" +
                "(instance ?X Q)" +
                "(instance ?X R))" +
                "(instance ?X ?T))";
        expectedRes = "( ( ! [V__X,V__T] : ((s__instance(V__X,s__Q) | s__instance(V__X,s__R)) => s__instance(V__X,V__T)) ) )";
        test(kifstring,expectedRes,"string2");
    }

    /** ***************************************************************
     */
    @Test
    public void string3() {

        String kifstring, expectedRes, actualRes;
        kifstring = "(or " +
                "(not " +
                "(instance ?X Q))" +
                "(instance ?X R))";
        expectedRes = "( ( ! [V__X] : ((~ s__instance(V__X,s__Q)) | s__instance(V__X,s__R)) ) )";
        test(kifstring,expectedRes,"string3");
    }

    /** ***************************************************************
     */
    @Test
    public void string4() {

        String kifstring, expectedRes, actualRes;
        kifstring = "(<=>\n" +
                "    (instance ?NUMBER NegativeRealNumber)\n" +
                "    (and\n" +
                "        (lessThan ?NUMBER 0)\n" +
                "        (instance ?NUMBER RealNumber)))";
        expectedRes = "( ( ! [V__NUMBER] : (s__instance(V__NUMBER,s__NegativeRealNumber) <=> " +
                "(s__lessThan(V__NUMBER,0) & s__instance(V__NUMBER,s__RealNumber))) ) )";
        test(kifstring,expectedRes,"string4");
    }

    /** ***************************************************************
     */
    @Test
    public void string5() {

        String kifstring, expectedRes, actualRes;
        kifstring = "(<=>\n" +
                "    (instance ?NUMBER NegativeRealNumber)\n" +
                "    (and\n" +
                "        (lessThan ?NUMBER 0.001)\n" +
                "        (instance ?NUMBER RealNumber)))";
        expectedRes = "( ( ! [V__NUMBER] : (s__instance(V__NUMBER,s__NegativeRealNumber) <=> " +
                "(s__lessThan(V__NUMBER,0.001) & s__instance(V__NUMBER,s__RealNumber))) ) )";
        test(kifstring,expectedRes,"string5");
    }

    /** ***************************************************************
     */
    @Test
    public void string6() {

        String kifstring, expectedRes, actualRes;
        kifstring = "(<=> (temporalPart ?POS (WhenFn ?THING)) (time ?THING ?POS))";
        expectedRes = "( ( ! [V__POS,V__THING] : (s__temporalPart(V__POS,s__WhenFn(V__THING)) <=> s__time(V__THING,V__POS)) ) )";
        test(kifstring,expectedRes,"string6");
    }

    /** ***************************************************************
     */
    @Test
    public void hol() {

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
        test(kifstring,expectedRes,"hol");
    }

    /** ***************************************************************
     */
    @Test
    public void string7() {

        String kifstring, expectedRes, actualRes;
        kifstring = "(<=> (exists (?BUILD) (and (instance ?BUILD Constructing) " +
                "(result ?BUILD ?ARTIFACT))) (instance ?ARTIFACT StationaryArtifact))";
        expectedRes = "( ( ! [V__ARTIFACT] : ((? [V__BUILD] : " +
                "(s__instance(V__BUILD,s__Constructing) & s__result(V__BUILD,V__ARTIFACT))) <=> " +
                "s__instance(V__ARTIFACT,s__StationaryArtifact)) ) )";
        test(kifstring,expectedRes,"string7");
    }

    /** ***************************************************************
     */
    @Test
    public void embedded() {

        String kifstring, expectedRes, actualRes;

        //SUMOformulaToTPTPformula.debug = true;
        kifstring = "(instance equal BinaryPredicate)";
        expectedRes = "( s__instance(s__equal__m,s__BinaryPredicate) )";
        test(kifstring,expectedRes,"embedded");
    }
}
