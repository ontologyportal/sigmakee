package com.articulate.sigma;

import java.io.File;

public class SigmaTestBase {
    static final String SIGMA_HOME = System.getenv("SIGMA_HOME");
    static final String KB_PATH = (new File(SIGMA_HOME, "KBs")).getAbsolutePath();

}
