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

        String leoSetting = KBmanager.getMgr().getPref("leoExecutable");
        assertNotNull("leoExecutable pref must not be null", leoSetting);
        assertFalse("leoExecutable pref must not be empty", leoSetting.trim().isEmpty());

        File exe = resolveExecutable(leoSetting);

        assertNotNull(
                "leoExecutable could not be resolved. Set leoExecutable to either:\n" +
                "  (1) an absolute path (or ~/...) to the LEO-III executable, OR\n" +
                "  (2) a command available on PATH (e.g., leo3)\n" +
                "Current leoExecutable=" + leoSetting,
                exe
        );

        assertTrue("leoExecutable does not exist: " + exe.getAbsolutePath(), exe.exists());
        assertTrue("leoExecutable is not a file: " + exe.getAbsolutePath(), exe.isFile());
        assertTrue("leoExecutable is not executable: " + exe.getAbsolutePath(), exe.canExecute());
    }

    /**
     * Resolve leoExecutable in a portable way.
     *
     * Accepts either:
     *  - an absolute/relative path (including "~/..."), or
     *  - a command name resolved from PATH (and PATHEXT on Windows).
     *
     * Returns the executable File if resolvable, otherwise null.
     */
    private static File resolveExecutable(String leoSettingRaw) {

        String leoSetting = stripQuotes(leoSettingRaw.trim());
        if (leoSetting.isEmpty())
            return null;

        // Expand "~/" or "~\"
        if (leoSetting.startsWith("~" + File.separator) || leoSetting.startsWith("~/") || leoSetting.startsWith("~\\")) {
            String home = System.getProperty("user.home");
            leoSetting = home + leoSetting.substring(1);
        }

        File candidate = new File(leoSetting);

        // Treat as path if it is absolute OR contains any path separator
        if (candidate.isAbsolute() || leoSetting.contains("/") || leoSetting.contains("\\") ) {
            return candidate.exists() ? candidate : null;
        }

        // Otherwise treat as a command name and resolve via PATH
        return findOnPath(leoSetting);
    }

    private static File findOnPath(String command) {

        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.trim().isEmpty())
            return null;

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String[] pathDirs = pathEnv.split(java.util.regex.Pattern.quote(File.pathSeparator));

        // Windows: try PATHEXT when command has no extension
        String[] winExts = new String[0];
        if (isWindows) {
            String pathext = System.getenv("PATHEXT");
            if (pathext != null && !pathext.trim().isEmpty()) {
                winExts = pathext.split(";");
            } else {
                winExts = new String[] { ".EXE", ".BAT", ".CMD" };
            }
        }

        for (String dir : pathDirs) {
            if (dir == null || dir.trim().isEmpty())
                continue;

            // Try as-is first
            File f = new File(dir, command);
            if (f.exists() && f.isFile() && f.canExecute())
                return f;

            // Windows: try extensions
            if (isWindows && !command.contains(".")) {
                for (String ext : winExts) {
                    if (ext == null || ext.trim().isEmpty())
                        continue;
                    File fx = new File(dir, command + ext.toLowerCase());
                    if (fx.exists() && fx.isFile() && fx.canExecute())
                        return fx;
                    File fX = new File(dir, command + ext.toUpperCase());
                    if (fX.exists() && fX.isFile() && fX.canExecute())
                        return fX;
                }
            }
        }

        return null;
    }

    private static String stripQuotes(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        if ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'"))) {
            return t.substring(1, t.length() - 1).trim();
        }
        return t;
    }
}