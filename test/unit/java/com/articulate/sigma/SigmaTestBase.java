package com.articulate.sigma;

import com.articulate.sigma.nlg.NLGUtils;
import com.articulate.sigma.parsing.Configuration;
import com.articulate.sigma.wordNet.WordNet;

import com.google.common.collect.Lists;

import java.io.*;
import java.util.*;

public class SigmaTestBase {

    static final String SIGMA_HOME = System.getenv("SIGMA_HOME");
    protected static final String KB_PATH = (new File(SIGMA_HOME, "KBs")).getAbsolutePath();

    protected static KB kb;

    /****************************************************************
     * Performs the KB load.
     * @param reader ignored legacy parameter
     */
    protected static void doSetUp(Reader reader) {

        if (!KBmanager.initialized) {
            KBmanager mgr = KBmanager.getMgr();

            /*
             * The manager owns the Configuration now. initializeOnce(KB_PATH)
             * reads KB_PATH/config.xml, installs the Configuration object,
             * loads lexicons according to that Configuration, and loads KBs.
             */
            mgr.initializeOnce(KB_PATH);
        }

        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getDefaultKbName());
        checkConfiguration();
    }

    /***************************************************************
     * */
    protected static void checkConfiguration() {

        List<String> problemList = Lists.newArrayList();
        if (NLGUtils.getKeywordMap() == null || NLGUtils.getKeywordMap().isEmpty()) {
            problemList.add("LanguageFormatter.keywordMap is empty.");
        }
        if (WordNet.wn.synsetsToWords.isEmpty()) {
            problemList.add("WordNet mappings are empty.");
        }
        List kbnames = Arrays.asList("english_format.kif","domainEnglishFormat.kif",
            "Merge.kif", "Mid-level-ontology.kif", "ArabicCulture.kif", "Cars.kif",
            "Catalog.kif", "Communications.kif", "CountriesAndRegions.kif", "Dining.kif",
            "Economy.kif", "engineering.kif", "FinancialOntology.kif", "Food.kif",
            "Geography.kif", "Government.kif", "Hotel.kif", "Justice.kif", "Languages.kif",
            "Media.kif", "MilitaryDevices.kif", "Military.kif", "MilitaryPersons.kif",
            "MilitaryProcesses.kif", "Music.kif", "naics.kif", "People.kif",
            "QoSontology.kif", "Sports.kif", "TransnationalIssues.kif", "Transportation.kif",
            "TransportDetail.kif","VirusProteinAndCellPart.kif","WMD.kif");

        if (!KBmanager.getMgr().getKBnames().containsAll(kbnames)) {
            problemList.add("KB missing one or more files. Expected: " + kbnames +
                    " actual:" + KBmanager.getMgr().getKBnames());
        }
        if (!problemList.isEmpty()) {
            StringBuilder sBuild = new StringBuilder();
            final String NEWLINE_AND_SPACES = "\n   ";
            for (String problem : problemList) {
                sBuild.append(NEWLINE_AND_SPACES).append(problem);
            }
            System.err.println("Configuration failed. Problems:" + sBuild.toString());
            throw new IllegalStateException("Configuration failed. Problems:" + sBuild.toString());
        }
    }

    /****************************************************************
     * Gets a BufferedReader for the xml file that is this test's configuration.
     * @param path XML path.
     * @param theClass test class.
     * @return reader for XML file.
     */
    protected static Reader getXmlReader(String path, Class<?> theClass)  {

        Reader xmlReader = null;
        try {
            xmlReader = new BufferedReader(new FileReader(path));
        }
        catch (FileNotFoundException ex)  {
            ex.printStackTrace();
            System.err.println(ex.getMessage());
            System.err.println("SigmaTestBase.getXmlReader(): Could not find: " + path);
        }
        return xmlReader;
    }

    /***************************************************************
     * */
    public static <T> void displayCollectionStringDiffs(Collection<T> coll1, Collection<T> coll2) {

        for (T obj : coll1) {
            if (!coll2.contains(obj)) {
                System.out.println("Found in parameter 1 but not 2: " + obj.toString());
            }
        }
        for (T obj : coll2) {
            if (!coll1.contains(obj)) {
                System.out.println("Found in parameter 2 but not 1: " + obj.toString());
            }
        }
    }
}