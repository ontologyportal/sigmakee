package com.articulate.sigma;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.*;

import static org.junit.Assert.fail;

/**
 * Base class for fast-running true unit tests.
 */

public class UnitTestBase  extends SigmaTestBase {
    private static final String CONFIG_FILE_PATH = "resources/config_topOnly.xml";
    private static final Class CLASS = UnitTestBase.class;
    public static final int NUM_KIF_FILES = 3;

    static Long totalKbMgrInitTime = Long.MAX_VALUE;

    // Write out a meaningful error message if the config file path is bad.
    private static final BufferedReader xmlReader;
    static  {
        xmlReader = SigmaTestBase.getXmlReader(CONFIG_FILE_PATH, CLASS);
    }

    @BeforeClass
    public static void setup()  {
        long startTime = System.currentTimeMillis();

        SigmaTestBase.doSetUp(xmlReader);

        long endTime = System.currentTimeMillis();

        // Update the init time only if it has its initialized value.
        if(UnitTestBase.totalKbMgrInitTime == Long.MAX_VALUE) {
            UnitTestBase.totalKbMgrInitTime = endTime - startTime;
        }
    }


    @AfterClass
    public static void checkKBCount()    {
        if (KBmanager.getMgr().getKB("SUMO").constituents.size() > NUM_KIF_FILES) {
            System.out.println("FAILURE: This test is running with the wrong configuration. Please investigate immediately, since the problem does not consistently appear.");
            System.out.println("  Because this test is changing the configuration, other tests may fail, even if this one passes.");
            System.out.println("  Nbr kif files: " + KBmanager.getMgr().getKB("SUMO").constituents.size());
            fail();
        }
    }
}
