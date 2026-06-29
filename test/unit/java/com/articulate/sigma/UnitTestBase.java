package com.articulate.sigma;

import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import com.articulate.sigma.parsing.Configuration;
import com.articulate.sigma.trans.TPTPGenerationManager;

import java.io.*;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.junit.Assert.fail;

/**
 * Base class for fast-running true unit tests.
 */
public class UnitTestBase extends SigmaTestBase {

    private static boolean unitConfigurationInitialized = false;
    public static final int NUM_KIF_FILES = 3;
    private static final String SIGMA_SRC = System.getenv("SIGMA_SRC");

    public static final String CONFIG_FILE_DIR = SIGMA_SRC + File.separator + "test/unit/java/resources";

    static Long totalKbMgrInitTime = Long.MAX_VALUE;

    private static Configuration buildUnitConfiguration() {

        String kbDir = KB_PATH;
        String userHome = System.getProperty("user.home");
        Configuration config = new Configuration();
        config.clearKbConstituentLists();
        config.setPreference("baseDir", new File(kbDir).getParentFile().getAbsolutePath());
        config.setPreference("kbDir", kbDir);
        config.setPreference("inferenceTestDir", kbDir + File.separator + "tests");
        config.setPreference("loadFresh", "true");
        config.setPreference("loadLexicons", "true");
        config.setPreference("cache", "true");
        config.setPreference("cacheDisjoint", "false");
        config.setPreference("cwa", "false");
        config.setPreference("maxPredicateArity", "6");
        config.setPreference("graphVizDir", "/usr/bin/dot");
        String eproverExec = new File("/usr/local/bin/e_ltb_runner").canExecute()
                ? "/usr/local/bin/e_ltb_runner"
                : userHome + File.separator + "Programs" + File.separator + "E" + File.separator + "PROVER" + File.separator + "e_ltb_runner";
        String vampireExec = new File("/usr/local/bin/vampire").canExecute()
                ? "/usr/local/bin/vampire"
                : userHome + File.separator + "Programs" + File.separator + "vampire" + File.separator + "build" + File.separator + "vampire";
        config.setPreference("eproverExec", eproverExec);
        config.setPreference("vampireExec", vampireExec);
        config.setPreference("tptpExec", userHome + File.separator + "workspace" + File.separator + "TPTP4X" + File.separator + "tptp4X");
        config.setPreference("systemsDir", userHome);
        List<String> constituents = Arrays.asList(
            "english_format.kif",
            "Merge.kif",
            "Mid-level-ontology.kif"
        );
        config.setKbConstituentList("SUMO", constituents);
        return config;
    }

    /***************************************************************
     * */
    @BeforeClass
    public static void setup()  {

        if (unitConfigurationInitialized && KBmanager.initialized) {
            kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getDefaultKbName());
            checkConfiguration();
            return;
        }
        System.out.println("UnitTestSuite.startUp(): SUMOKBtoTPTPKB.rapidParsing==" + SUMOKBtoTPTPKB.rapidParsing);
        System.out.println("UnitTestBase.setup(): building unit test configuration in code.");
        System.out.println("***** UnitTestBase.setup(): warning! Note that only KB files in the test configuration will be loaded! ***** ");
        long startTime = System.currentTimeMillis();
        Configuration config = buildUnitConfiguration();
        KBmanager.initialized = false;
        KBmanager.initializing = false;
        KBmanager.getMgr().kbs.clear();
        KBmanager.getMgr().initializeOnce(config);
        while (!TPTPGenerationManager.isFOFReady()) TPTPGenerationManager.waitForFOF(600);
        while (!TPTPGenerationManager.isTFFReady()) TPTPGenerationManager.waitForTFF(600);
        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getDefaultKbName());
        checkConfiguration();
        unitConfigurationInitialized = true;
        long endTime = System.currentTimeMillis();
        if (UnitTestBase.totalKbMgrInitTime == Long.MAX_VALUE) UnitTestBase.totalKbMgrInitTime = endTime - startTime;
    }

    /***************************************************************
     * */
    @AfterClass
    public static void checkKBCount() {

        KB defaultKB = KBmanager.getMgr().getKB(KBmanager.getMgr().getDefaultKbName());
        if (defaultKB == null) fail("KBmanager returned no default KB.");
        if (defaultKB.constituents.size() > NUM_KIF_FILES) {
            System.out.println("FAILURE: This test is running with the wrong configuration. Please investigate immediately, since the problem does not consistently appear.");
            System.out.println("  Because this test is changing the configuration, other tests may fail, even if this one passes.");
            System.out.println("  Nbr kif files: " + defaultKB.constituents.size());
            fail();
        }
    }
}
