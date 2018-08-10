package com.articulate.sigma.trans;

import com.articulate.sigma.FormulaPreprocessor;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.articulate.sigma.*;
import org.junit.Test;
import org.junit.Before;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 */
public class SUMOtoTFATest extends UnitTestBase {

    /** ***************************************************************
     */
    @Before
    public void init() {

        SUMOtoTFAform.initOnce();
    }

    /** ***************************************************************
     */
    @Test
    public void Test1() {

        String kifstring, expectedRes, actualRes;
        kifstring = "(instance Foo Bar)";
        expectedRes = "( s__instance(s__Foo, s__Bar) )";
        actualRes = SUMOtoTFAform.process(kifstring);
        System.out.println(actualRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void Test2() {

        String kifstring, expectedRes, actualRes;
        kifstring = "(forall (?X) (=> (instance ?X Human) (attribute ?X Mortal)))";
        expectedRes = "((! [V__X] : (s__instance(V__X,s__Human) => s__attribute(V__X,s__Mortal))))";
        actualRes = SUMOtoTFAform.process(kifstring);
        System.out.println(actualRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void Test3() {

        String kifstring, expectedRes, actualRes;
        kifstring = "(=>\n" +
                "    (and\n" +
                "        (subProcess ?S1 ?P)\n" +
                "        (subProcess ?S2 ?P))\n" +
                "    (relatedEvent ?S1 ?S2))";
        ArrayList<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        if (forms == null || forms.size() == 0)
            return;
        expectedRes = "";
        actualRes = SUMOtoTFAform.process(forms.get(0).toString());
        System.out.println(actualRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void Test4() {

        String kifstring, expectedRes, actualRes;
        kifstring = "(=>\n" +
                "    (and\n" +
                "        (instance ?DEV ElectricDevice)\n" +
                "        (instance ?EV Process)\n" +
                "        (instrument ?EV ?DEV))\n" +
                "    (exists (?R)\n" +
                "        (and\n" +
                "            (instance ?R Electricity)\n" +
                "            (resource ?EV ?R))))";
        ArrayList<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        if (forms == null || forms.size() == 0)
            return;
        expectedRes = "";
        actualRes = SUMOtoTFAform.process(forms.get(0).toString());
        System.out.println(actualRes);
        assertEquals(expectedRes, actualRes.trim());
    }

    /** ***************************************************************
     */
    @Test
    public void Test5() {

        FormulaPreprocessor.debug = true;
        String kifstring, expectedRes, actualRes;
        kifstring = "(=>\n" +
                "    (and\n" +
                "        (instance ?PROC Process)\n" +
                "        (eventLocated ?PROC ?LOC)\n" +
                "        (subProcess ?SUB ?PROC))\n" +
                "    (eventLocated ?SUB ?LOC))";
        ArrayList<Formula> forms = SUMOtoTFAform.fp.preProcess(new Formula(kifstring),false,kb);
        if (forms == null || forms.size() == 0)
            return;
        expectedRes = "";
        actualRes = SUMOtoTFAform.process(forms.get(0).toString());
        System.out.println(actualRes);
        assertEquals(expectedRes, actualRes.trim());
    }
}
