package com.articulate.sigma.trans;

import com.articulate.sigma.*;

import java.util.*;

import static junit.framework.TestCase.assertTrue;

import com.articulate.sigma.Formula;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

//This software is released under the GNU Public License
//<http://www.gnu.org/copyleft/gpl.html>.
// Copyright 2019 Infosys
// adam.pease@infosys.com

public class SUMOtoTFAKBTest extends IntegrationTestBase {

    /****************************************************************
     */
    @BeforeClass
    public static void init() {

        System.out.println("\n======================== SUMOtoTFAKBTest.init(): ");
        SUMOKBtoTFAKB skbtfakb = new SUMOKBtoTFAKB();
        skbtfakb.initOnce();
        SUMOformulaToTPTPformula.setLang("tff");
        SUMOtoTFAform.setNumericFunctionInfo();
    }

    @AfterClass
    public static void postClass() {
        KBmanager.initialized = false;
        SUMOKBtoTFAKB.initialized = false;
        SUMOtoTFAform.initialized = false;
    }

    @After
    public void tearDown() {

        SUMOtoTFAform.debug = false;
        SUMOKBtoTFAKB.debug = false;
    }

    /** *************************************************************
     */
    @Test
    public void testPartition() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAKBTest.testPartition(): ");
        List<String> sig = SUMOtoTFAform.relationExtractNonNumericSig("partition__5");
        System.out.println(sig);
        String expectedRes = "[, Class, Class, Class, Class, Class]";
        String result = sig.toString();
        System.out.println("testDynamicSortDef(): result: " + result);
        System.out.println("testDynamicSortDef(): expect: " + expectedRes);
        if (expectedRes.equals(result))
            System.out.println("testPartition(): Success!");
        else
            System.err.println("testPartition(): fail");
        assertTrue(expectedRes.equals(result));
    }

    /** *************************************************************
     */
    @Test
    public void testDynamicSortDef() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAKBTest.testDynamicSortDef(): ");

        String result = SUMOtoTFAform.sortFromRelation("ListFn__2Fn__2ReFn");
        String expectedRes = "s__ListFn__2Fn__2ReFn : (  $i * $real  ) > $i";
        System.out.println("testDynamicSortDef(): result: " + result);
        System.out.println("testDynamicSortDef(): expect: " + expectedRes);
        if (expectedRes.equals(result))
            System.out.println("testDynamicSortDef(): Success!");
        else
            System.err.println("testDynamicSortDef(): fail");
        assertTrue(expectedRes.equals(result));
    }

    /** *************************************************************
     */
    @Test
    public void testMissingSort() {

        SUMOtoTFAform.debug = true;
        SUMOKBtoTFAKB.debug = true;
        System.out.println();
        System.out.println("\n======================== SUMOtoTFAKBTest.testMissingSort(): ");
        SUMOtoTFAform stfa = new SUMOtoTFAform();
        // KIF formula — missingSortsExpr walks a KIF Expr tree, not a TPTP string
        String f = "(instance (ListFn__2ReFn ?ROW1 ?ROW2) Entity)";
        Formula f1 = new Formula(f);
        Set<String> result = stfa.missingSortsExpr(f1.expr);
        String expectedRes = "s__ListFn__2ReFn : (  $i * $real  ) > $i";
        String resultStr = "";
        if (result != null && !result.isEmpty())
            resultStr = result.iterator().next().trim();
        System.out.println("testMissingSort(): result: " + resultStr);
        System.out.println("testMissingSort(): expect: " + expectedRes);
        if (!resultStr.isBlank() && resultStr.equals(expectedRes))
            System.out.println("testMissingSort(): Success!");
        else
            System.err.println("testMissingSort(): fail");
        assertTrue(resultStr.equals(expectedRes));
    }
}
