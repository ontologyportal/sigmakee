package com.articulate.sigma;

import org.junit.BeforeClass;

import java.io.BufferedReader;

/**
 * Base class for unit tests which are closer to integration tests because they require a large KB configuration.
 */

public class IntegrationTestBase extends SigmaTestBase {
    private static final String CONFIG_FILE_PATH = "resources/config_all.xml";
    private static final Class CLASS = IntegrationTestBase.class;

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
            IntegrationTestBase.totalKbMgrInitTime = endTime - startTime;
    }

}
