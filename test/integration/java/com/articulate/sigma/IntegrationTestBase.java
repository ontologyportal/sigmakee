package com.articulate.sigma;

import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/** ***************************************************************
 * Base class for unit tests which are closer to integration tests because they require a large KB configuration.
 */

public class IntegrationTestBase extends SigmaTestBase {

    //private static final String CONFIG_FILE_PATH = "resources/config_all.xml";
    private static final Class CLASS = IntegrationTestBase.class;
    static Long totalKbMgrInitTime = Long.MAX_VALUE;
    protected static KB kbBackup;

    /** ***************************************************************
     * File object pointing to this test's resources directory.
     */
    public static final File RESOURCES_DIR;

    static  {
        String d = null;
        try {
            d = System.getenv("SIGMA_SRC") + File.separator + "test/integration/java/resources";
            System.out.println("IntegrationTestBase initialization with dir: " + d);
            File f = new File(d);
            if (!f.exists())
                throw new Exception();
        }
        catch (Exception e) {
            System.out.println("Error in IntegrationTestBase initialization with dir: " + d);
            e.printStackTrace();
        }
        RESOURCES_DIR = new File(d);
    }

    // Write out a meaningful error message if the config file path is bad.
//    private static final BufferedReader xmlReader;
//    static  {
//        xmlReader = SigmaTestBase.getXmlReader(CONFIG_FILE_PATH, CLASS);
//    }

    /** ***************************************************************
     */
    @BeforeClass
    public static void setup() throws IOException {

        long startTime = System.currentTimeMillis();

        //SigmaTestBase.doSetUp(xmlReader);
        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        kbBackup = new KB(kb);
        checkConfiguration();
        long endTime = System.currentTimeMillis();
        // Update the init time only if it has its initialized value.
        if (IntegrationTestBase.totalKbMgrInitTime == Long.MAX_VALUE) {
            IntegrationTestBase.totalKbMgrInitTime = endTime - startTime;
        }
    }

    /****************************************************************
     * Undo all parts of the state that have anything to do with user assertions made during inference.
     * @throws IOException
     */
    public static void resetAllForInference() throws IOException {

        kb = new KB(kbBackup);
        KBmanager.getMgr().kbs.put(KBmanager.getMgr().getPref("sumokbname"), kb);
        kb.deleteUserAssertions();

        // Remove the assertions in the files.
        File userAssertionsFile = new File(KB_PATH, KBmanager.getMgr().getPref("sumokbname") + kb._userAssertionsString);
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
