package com.articulate.sigma;

import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Base class for unit tests which are closer to integration tests because they require a large KB configuration.
 */

public class IntegrationTestBase extends SigmaTestBase {
    //private static final String CONFIG_FILE_PATH = "resources/config_all.xml";
    private static final Class CLASS = IntegrationTestBase.class;

    protected static KB kbBackup;

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
//    private static final BufferedReader xmlReader;
//    static  {
//        xmlReader = SigmaTestBase.getXmlReader(CONFIG_FILE_PATH, CLASS);
//    }

    @BeforeClass
    public static void setup() throws IOException {
        long startTime = System.currentTimeMillis();

        //SigmaTestBase.doSetUp(xmlReader);
        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB("SUMO");

        kbBackup = new KB(kb);

        checkConfiguration();

        long endTime = System.currentTimeMillis();

        // Update the init time only if it has its initialized value.
        if(IntegrationTestBase.totalKbMgrInitTime == Long.MAX_VALUE) {
            IntegrationTestBase.totalKbMgrInitTime = endTime - startTime;
        }
    }

    /****************************************************************
     * Undo all parts of the state that have anything to do with user assertions made during inference.
     * @throws IOException
     */
    public static void resetAllForInference() throws IOException {
        kb = new KB(kbBackup);
        KBmanager.getMgr().kbs.put("SUMO", kb);
        kb.deleteUserAssertions();

        // Remove the assertions in the files.
        File userAssertionsFile = new File(KB_PATH, "SUMO" + kb._userAssertionsString);
        if (userAssertionsFile.exists()) {
            userAssertionsFile.delete();
            userAssertionsFile.createNewFile();
            userAssertionsFile.deleteOnExit();
        }

        String tptpFileName = userAssertionsFile.getAbsolutePath().replace(".kif", ".tptp");
        userAssertionsFile = new File(tptpFileName);
        if (userAssertionsFile.exists()) {
            userAssertionsFile.delete();
            userAssertionsFile.createNewFile();
            userAssertionsFile.deleteOnExit();
        }
    }
}
