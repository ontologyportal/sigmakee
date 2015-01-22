package com.articulate.sigma;

import org.junit.BeforeClass;

import java.io.File;

public class SigmaTestBase {
    static final String SIGMA_HOME = System.getenv("SIGMA_HOME");
    static final String KB_PATH = (new File(SIGMA_HOME, "KBs")).getAbsolutePath();

    protected static KB kb;

    @BeforeClass
    public static void setup()  {
        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        kb = KBmanager.getMgr().getKB("SUMO");
    }
}
