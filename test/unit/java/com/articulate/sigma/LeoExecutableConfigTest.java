package com.articulate.sigma;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class LeoExecutableConfigTest extends UnitTestBase {

    /**
     * Verifies that the leoExecutable preference points to a real executable.
     *
     * This is an opt-in local integration test because config.xml lives under
     * ~/.sigmakee (or equivalent) and differs per machine/OS.
     *
     * Enable by setting: RUN_EXTERNAL_ATP_TESTS=true
     * 
     * 
     * Author: Simon Deng, NPS ORISE Intern 2025, adam.pease@nps.edu
     * @author <a href="mailto:adam.pease@nps.edu?subject=com.articulate.sigma.jedit.FormatSuoKifAxiomsGUITest">Simon Deng, NPS ORISE Intern 2025</a>
     */
    
    @Test
    public void testLeoExecutablePrefIsValidExecutable() {

        // Skip by default (so CI doesn't fail on machines without LEO installed)
        Assume.assumeTrue(
                "Set RUN_EXTERNAL_ATP_TESTS=true to enable external ATP configuration checks",
                "true".equalsIgnoreCase(System.getenv("RUN_EXTERNAL_ATP_TESTS"))
        );

        KBmanager.getMgr().initializeOnce();

        String leoPath = KBmanager.getMgr().getPref("leoExecutable");
        assertNotNull("leoExecutable pref must not be null", leoPath);
        assertFalse("leoExecutable pref must not be empty", leoPath.trim().isEmpty());

        File exe = new File(leoPath);

        assertTrue("leoExecutable does not exist: " + leoPath, exe.exists());
        assertTrue("leoExecutable is not a file: " + leoPath, exe.isFile());
        assertTrue("leoExecutable is not executable: " + leoPath, exe.canExecute());
    }
}