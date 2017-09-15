package com.articulate.sigma;

import com.articulate.sigma.nlg.NLGUtils;
import com.google.common.collect.Lists;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

public class SigmaTestBase {

    static final String SIGMA_HOME = System.getenv("SIGMA_HOME");
    protected static final String KB_PATH = (new File(SIGMA_HOME, "KBs")).getAbsolutePath();

    protected static KB kb;

    /****************************************************************
     * Performs the KB load.
     * @param reader
     */
    protected static void doSetUp(BufferedReader reader) {

        KBmanager manager = KBmanager.getMgr();
        SimpleElement configuration = null;
        if (! manager.initialized) {
            try {
                SimpleDOMParser sdp = new SimpleDOMParser();
                //sdp.setSkipProlog(false);
                configuration = sdp.parse(reader);
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            manager.initializing = true;
            KBmanager.getMgr().setDefaultAttributes();
            KBmanager.getMgr().setConfiguration(configuration);
            manager.initialized = true;
            manager.initializing = false;
        }
        kb = KBmanager.getMgr().getKB("SUMO");
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

        if (! problemList.isEmpty()) {
            StringBuilder sBuild = new StringBuilder();
            for (String problem : problemList) {
                final String NEWLINE_AND_SPACES = "\n   ";
                sBuild.append(NEWLINE_AND_SPACES).append(problem);
            }
            throw new IllegalStateException("Configuration failed. Problems:" + sBuild.toString());
        }
    }

    /****************************************************************
     * Gets a BufferedReader for the xml file that is this test's configuration.
     * @param path
     * @param theClass
     * @return
     */
    protected static BufferedReader getXmlReader(String path, Class theClass)  {

        BufferedReader xmlReader = null;
        try {
            //URI uri = theClass.getClassLoader().getResource(path).toURI();
            //URI uri = theClass.getResource(path).toURI();
            //File configFile = new File(uri);
           // String contents = StringUtil.getContents(configFile);
            //contents = contents.replaceAll("\\$SIGMA_HOME", SIGMA_HOME);
            //xmlReader = new BufferedReader(new StringReader(path));
            xmlReader = new BufferedReader(new FileReader(path));
            //xmlReader = new BufferedReader(new InputStreamReader(theClass.getResourceAsStream(path)));
        }
        catch (Exception ex)  {
            //try {
                //URI uri = theClass.getClassLoader().getResource(".").toURI();
                //URI uri = theClass.getResource(".").toURI();
                //String msg = "Could not find " + path + " in " + uri.toString();
                ex.printStackTrace();
                System.out.println(ex.getMessage());
                System.out.println("SigmaTestBase.getXmlReader(): Could not find " + path);
                //throw new IllegalStateException(msg);
            //}
            //catch (URISyntaxException e) {
            //    e.printStackTrace();
            //}
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
