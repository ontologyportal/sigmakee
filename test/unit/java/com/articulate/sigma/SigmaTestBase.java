package com.articulate.sigma;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

public class SigmaTestBase {
    static final String SIGMA_HOME = System.getenv("SIGMA_HOME");
    static final String KB_PATH = (new File(SIGMA_HOME, "KBs")).getAbsolutePath();

    protected static KB kb;

    /**
     * Performs the KB load.
     * @param reader
     */
    protected static void doSetUp(BufferedReader reader)    {
        KBmanager manager = KBmanager.getMgr();

        SimpleElement configuration = null;

        if(! manager.initialized) {
            try {
                SimpleDOMParser sdp = new SimpleDOMParser();
                configuration = sdp.parse(reader);
            } catch (IOException e) {
                e.printStackTrace();
            }

            manager.initializing = true;
            KBmanager.getMgr().setConfiguration(configuration);
            manager.initialized = true;
            manager.initializing = false;
        }

        kb = KBmanager.getMgr().getKB("SUMO");
    }

    /**
     * Gets a BufferedReader for the xml file that is this test's configuration.
     * @param path
     * @param theClass
     * @return
     */
    protected static BufferedReader getXmlReader(String path, Class theClass)  {
        BufferedReader xmlReader = null;
        try {
            URI uri = theClass.getClassLoader().getResource(path).toURI();
            //URI uri = theClass.getResource(path).toURI();
            File configFile = new File(uri);
            String contents = StringUtil.getContents(configFile);
            contents = contents.replaceAll("\\$SIGMA_HOME", SIGMA_HOME);
            xmlReader = new BufferedReader(new StringReader(contents));

            //xmlReader = new BufferedReader(new InputStreamReader(theClass.getResourceAsStream(path)));
        }
        catch (Exception ex)  {
            try {
                URI uri = theClass.getClassLoader().getResource(".").toURI();
                //URI uri = theClass.getResource(".").toURI();
                String msg = "Could not find " + path + " in " + uri.toString();
                throw new IllegalStateException(msg);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

        }
        return xmlReader;
    }

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