package com.articulate.sigma.trans;

import com.articulate.sigma.Formula;
import com.articulate.sigma.IntegrationTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import org.junit.Ignore;

//This software is released under the GNU Public License
//<http://www.gnu.org/copyleft/gpl.html>.

public class THFtest extends IntegrationTestBase {

    private static THF thf = null;

    /****************************************************************
     */
    @BeforeClass
    public static void init() {

        thf = new THF();
    }

    /** *************************************************************
     */
    public void test(String msg, String f, String expected) {

        System.out.println();
        System.out.println("\n======================== " + msg);
        String result = thf.oneKIF2THF(new Formula(f),false, kb);
        System.out.println("THFtest.test(): result: " + result);
        System.out.println("THFtest.test(): expect: " + expected);
        if (expected.equals(result))
            System.out.println("THFtest.test(): Success!");
        else
            System.err.println("THFtest.test(): fail");
        assertTrue(expected.equals(result));
    }

    /** *************************************************************
     */
    @Test
    @Ignore
    public void testTrans1() {

        String f = "(=> (and (instance ?ROW3 Language) (instance ?ROW1 SymbolicString)) " +
                "(=> (synonymousExternalConcept ?ROW1 ?ROW2 ?ROW3) " +
                "(relatedExternalConcept ?ROW1 ?ROW2 ?ROW3)))";
        String msg = "testTrans1";
        String expected = "thf(ax1641,axiom,((! [ROW3: $i,ROW1: $i,ROW2: $i]: " +
                "(((instance_THFTYPE_IiioI @ ROW3 @ lLanguage_THFTYPE_i) & " +
                "(instance_THFTYPE_IiioI @ ROW1 @ lSymbolicString_THFTYPE_i)) =>";

        test(msg, f, expected);
    }

}
