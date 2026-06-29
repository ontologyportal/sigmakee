package com.articulate.sigma;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.articulate.sigma.trans.TPTPGenerationManager;
import com.articulate.sigma.parsing.Configuration;
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import org.junit.BeforeClass;

/** ***************************************************************
 * Base class for unit tests which are closer to integration tests because they require a large KB configuration.
 */

public class IntegrationTestBase extends SigmaTestBase {

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
            if (!f.exists()) throw new Exception();
        }
        catch (Exception e) {
            System.err.println("Error in IntegrationTestBase initialization with dir: " + d);
            e.printStackTrace();
        }
        RESOURCES_DIR = new File(d);
    }

    private static Configuration buildIntegrationConfiguration() {

        String kbDir = KB_PATH;
        Configuration config = new Configuration();
        config.clearKbConstituentLists();
        config.setPreference("baseDir", System.getenv("SIGMA_HOME"));
        config.setPreference("kbDir", kbDir);
        config.setPreference("inferenceTestDir", kbDir + File.separator + "tests");
        config.setPreference("loadFresh", "true");
        config.setPreference("loadLexicons", "true");
        config.setPreference("cache", "true");
        config.setPreference("cacheDisjoint", "false");
        config.setPreference("cwa", "false");
        config.setPreference("maxPredicateArity", "6");
        config.setPreference("graphVizDir", "/usr/bin/dot");
        String userHome = System.getProperty("user.home");
        String eproverExec = new File("/usr/local/bin/e_ltb_runner").canExecute()
                ? "/usr/local/bin/e_ltb_runner"
                : userHome + File.separator + "Programs" + File.separator + "E" + File.separator + "PROVER" + File.separator + "e_ltb_runner";
        String vampireExec = new File("/usr/local/bin/vampire").canExecute()
                ? "/usr/local/bin/vampire"
                : userHome + File.separator + "Programs" + File.separator + "vampire" + File.separator + "build" + File.separator + "vampire";
        config.setPreference("eproverExec", eproverExec);
        config.setPreference("vampireExec", vampireExec);
        config.setPreference("tptpExec", System.getProperty("user.home") + File.separator + "workspace" + File.separator + "TPTP4X" + File.separator + "tptp4X");
        config.setPreference("systemsDir", System.getProperty("user.home"));
        List<String> constituents = Arrays.asList(
            "english_format.kif",
            "domainEnglishFormat.kif",
            "Merge.kif",
            "Mid-level-ontology.kif",
            "ArabicCulture.kif",
            "Cars.kif",
            "Catalog.kif",
            "Communications.kif",
            "CountriesAndRegions.kif",
            "Dining.kif",
            "Economy.kif",
            "engineering.kif",
            "FinancialOntology.kif",
            "Food.kif",
            "Geography.kif",
            "Government.kif",
            "Hotel.kif",
            "Justice.kif",
            "Languages.kif",
            "Media.kif",
            "MilitaryDevices.kif",
            "Military.kif",
            "MilitaryPersons.kif",
            "MilitaryProcesses.kif",
            "Music.kif",
            "naics.kif",
            "People.kif",
            "QoSontology.kif",
            "Sports.kif",
            "TransnationalIssues.kif",
            "Transportation.kif",
            "TransportDetail.kif",
            "VirusProteinAndCellPart.kif",
            "WMD.kif"
        );
        config.setKbConstituentList("SUMO", constituents);
        return config;
    }

    /** ***************************************************************
     */
    @BeforeClass
    public static void setup() throws IOException {

        System.out.println("IntegrationTestBase.startUp(): SUMOKBtoTPTPKB.rapidParsing==" + SUMOKBtoTPTPKB.rapidParsing);
        long startTime = System.currentTimeMillis();
        Configuration config = buildIntegrationConfiguration();
        KBmanager.initialized = false;
        KBmanager.initializing = false;
        KBmanager.getMgr().kbs.clear();
        KBmanager.getMgr().initializeOnce(config);
        while (!TPTPGenerationManager.isFOFReady()) TPTPGenerationManager.waitForFOF(600);
        while (!TPTPGenerationManager.isTFFReady()) TPTPGenerationManager.waitForTFF(600);
        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getDefaultKbName());
        kbBackup = new KB(kb);
        checkConfiguration();
        long endTime = System.currentTimeMillis();
        if (IntegrationTestBase.totalKbMgrInitTime == Long.MAX_VALUE) IntegrationTestBase.totalKbMgrInitTime = endTime - startTime;
    }

    /****************************************************************
     * Undo all parts of the state that have anything to do with user assertions made during inference.
     * @throws IOException
     */
    public static void resetAllForInference() throws IOException {

        kb = new KB(kbBackup);
        KBmanager.getMgr().kbs.put(KBmanager.getMgr().getDefaultKbName(), kb);
        kb.deleteUserAssertions();
        File userAssertionsFile = new File(KB_PATH, KBmanager.getMgr().getDefaultKbName() + KB._userAssertionsString);
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
