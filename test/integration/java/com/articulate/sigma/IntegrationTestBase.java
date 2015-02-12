package com.articulate.sigma;

import org.junit.BeforeClass;

import java.io.BufferedReader;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Base class for unit tests which are closer to integration tests because they require a large KB configuration.
 */

public class IntegrationTestBase extends SigmaTestBase {
    private static final String CONFIG_FILE_PATH = "resources/config_all.xml";
    private static final Class CLASS = IntegrationTestBase.class;

    /**
     * File object pointing to this test's resources directory.
     */
    public static final File RESOURCES_FILE;
    static  {
        URI uri = null;
        try {
            uri = CLASS.getClassLoader().getResource("./resources").toURI();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        RESOURCES_FILE = new File(uri);
    }

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
        if(IntegrationTestBase.totalKbMgrInitTime == Long.MAX_VALUE) {
            IntegrationTestBase.totalKbMgrInitTime = endTime - startTime;
        }
    }

}
