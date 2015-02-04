package com.articulate.sigma;

import org.junit.BeforeClass;

import java.io.*;

/**
 * Base class for fast-running true unit tests.
 */

public class UnitTestBase  extends SigmaTestBase {
    private static final String CONFIG_FILE_PATH = "resources/config_topOnly.xml";
    private static final Class CLASS = UnitTestBase.class;

    // Write out a meaningful error message if the config file path is bad.
    private static final BufferedReader xmlReader;
    static  {
        xmlReader = SigmaTestBase.getXmlReader(CONFIG_FILE_PATH, CLASS);
    }

    @BeforeClass
    public static void setup()  {
        SigmaTestBase.doSetUp(xmlReader);
    }
}
