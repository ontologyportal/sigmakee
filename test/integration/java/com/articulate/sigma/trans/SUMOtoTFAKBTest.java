package com.articulate.sigma.trans;

import com.articulate.sigma.*;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.assertEquals;
import org.junit.BeforeClass;
import org.junit.Ignore;

//This software is released under the GNU Public License
//<http://www.gnu.org/copyleft/gpl.html>.
// Copyright 2019 Infosys
// adam.pease@infosys.com

public class SUMOtoTFAKBTest extends IntegrationTestBase {

    private static SUMOKBtoTFAKB skbtfakb = null;

    /****************************************************************
     */
    @BeforeClass
    public static void init() {

        SUMOtoTFAform.initOnce();
        SUMOtoTFAform.setNumericFunctionInfo();
        skbtfakb = new SUMOKBtoTFAKB();
        SUMOformulaToTPTPformula.lang = "tff";
        skbtfakb.initOnce();
    }

    /** *************************************************************
     */
    @Test
    public void testPartition() {

        System.out.println();
        System.out.println("\n======================== SUMOtoTFAKBTest.testPartition(): ");
        ArrayList<String> sig = SUMOtoTFAform.relationExtractNonNumericSig("partition__5");
        System.out.println(sig);
        String expectedRes = "[\"\",Class, Class, Class, Class, Class]";
        String result = sig.toString();
        if (expectedRes.equals(result))
            System.out.println("testPartition(): Success!");
        else
            System.out.println("testPartition(): fail");
        assertTrue(expectedRes.equals(result));
    }
}
