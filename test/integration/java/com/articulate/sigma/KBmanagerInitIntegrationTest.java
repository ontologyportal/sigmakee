package com.articulate.sigma;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import com.articulate.sigma.utils.*;

import static org.junit.Assert.*;

public class KBmanagerInitIntegrationTest extends IntegrationTestBase {

    private static Set<String> kifSet = Sets.newHashSet();

    /** ***************************************************************
     */
    @BeforeClass
    public static void setKB() {

        //kifSet.add("ArabicCulture.kif");
        //kifSet.add("Biography.kif");
        //kifSet.add("Cars.kif");
        //kifSet.add("Catalog.kif");
        //kifSet.add("Communications.kif");
        //kifSet.add("CountriesAndRegions.kif");
        //kifSet.add("Dining.kif");
        //kifSet.add("Economy.kif");
        //kifSet.add("engineering.kif");
        kifSet.add("SUMO_Cache.kif");
        //kifSet.add("FinancialOntology.kif");
        //kifSet.add("domainEnglishFormat.kif");
        //kifSet.add("english_format.kif");
        //kifSet.add("Food.kif");
        //kifSet.add("Geography.kif");
        //kifSet.add("Government.kif");
        //kifSet.add("Hotel.kif");
        //kifSet.add("Justice.kif");
        //kifSet.add("Languages.kif");
        //kifSet.add("Media.kif");
        kifSet.add("Merge.kif");
        kifSet.add("Mid-level-ontology.kif");
        //kifSet.add("Military.kif");
        //kifSet.add("MilitaryDevices.kif");
        //kifSet.add("MilitaryPersons.kif");
        //kifSet.add("MilitaryProcesses.kif");
        //kifSet.add("Music.kif");
        //kifSet.add("naics.kif");
        //kifSet.add("People.kif");
        //kifSet.add("QoSontology.kif");
        //kifSet.add("Sports.kif");
        //kifSet.add("TransnationalIssues.kif");
        //kifSet.add("Transportation.kif");
        //kifSet.add("TransportDetail.kif");
        //kifSet.add("UXExperimentalTerms.kif");
        //kifSet.add("VirusProteinAndCellPart.kif");
        //kifSet.add("WMD.kif");
     }

    /** ***************************************************************
     * Verify that you are running your tests with the expected configuration.
     */
    @Test
    @Ignore
    public void testNbrKifFilesLoaded()   {

        Set<String> expectedKifFiles = Sets.newHashSet(kifSet);
        Set<String> actualKifFiles = new HashSet<>();
        for (String s : SigmaTestBase.kb.constituents)
            actualKifFiles.add(FileUtil.noPath(s));
        System.out.println("testNbrKifFilesLoaded: actual: " + actualKifFiles);
        System.out.println("testNbrKifFilesLoaded: expected: " + expectedKifFiles);
        //filterExpectedKifs(actualKifFiles, expectedKifFiles);
        assertTrue(actualKifFiles.size() > 2);
        for (String f : expectedKifFiles) {
            System.out.println("testNbrKifFilesLoaded: contains: " + f + " : " + actualKifFiles.contains(f));
            assertTrue(actualKifFiles.contains(f));
        }
    }

    /** ***************************************************************
     */
    private void filterExpectedKifs(List<String> actualKifFiles, Set<String> expectedKifFiles) {

        List<String> remainingActualKifFiles = Lists.newArrayList(actualKifFiles);
        for (String file : actualKifFiles) {
            String fileName = file.substring(file.lastIndexOf("/") + 1);
            if (kifSet.contains(fileName))  {
                remainingActualKifFiles.remove(file);
                expectedKifFiles.remove(fileName);
            }
            else if (fileName.startsWith("SUMO_"))  {
                // Remove kif knowledge added after initialization--the cache as well as Interpreter assertions.
                remainingActualKifFiles.remove(file);
            }
        }
        actualKifFiles.clear();
        actualKifFiles.addAll(remainingActualKifFiles);
    }

    /** ***************************************************************
     * Verify how long the base class's KBmanager initialization took.
     */
    @Test
    public void testInitializationTime()   {

        assertTrue("Actual time = " + IntegrationTestBase.totalKbMgrInitTime, IntegrationTestBase.totalKbMgrInitTime < 350000);
        // Just in case something whacky is going on, make sure it's greater than some minimum, too.
        assertTrue("Actual time = " + IntegrationTestBase.totalKbMgrInitTime, IntegrationTestBase.totalKbMgrInitTime > 30000);
    }
}