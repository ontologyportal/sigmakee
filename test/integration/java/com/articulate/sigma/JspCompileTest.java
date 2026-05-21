package com.articulate.sigma;

import org.apache.jasper.JspC;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/*****************************************************************
 * Verifies that top-level JSP files compile successfully.
 */
public class JspCompileTest {

    private static final Set<String> JSP_FRAGMENTS = Set.of(
            "Prelude.jsp",
            "CommonHeader.jsp",
            "Postlude.jsp"
    );

    /*****************************************************************
     * Compiles top-level JSP files under the web application directory.
     * @throws Exception if JSP compilation fails.
     */
    @Test
    public void testJspCompilation() throws Exception {

        Path webDir = Path.of("web");
        Path jspDir = webDir.resolve("jsp");
        File outputDir = new File("build/jsp-compile");

        assertTrue("Web directory does not exist: " + webDir.toAbsolutePath(), Files.exists(webDir));
        assertTrue("JSP directory does not exist: " + jspDir.toAbsolutePath(), Files.exists(jspDir));
        assertTrue("Could not create JSP output directory: " + outputDir.getAbsolutePath(),
                outputDir.exists() || outputDir.mkdirs());
        List<String> jspFiles = Files.walk(jspDir)
                .filter(path -> path.toString().endsWith(".jsp"))
                .filter(path -> !JSP_FRAGMENTS.contains(path.getFileName().toString()))
                .map(path -> webDir.relativize(path).toString())
                .map(path -> path.replace(File.separatorChar, '/'))
                .toList();
        JspC jspc = new JspC();
        jspc.setUriroot(webDir.toAbsolutePath().toString());
        jspc.setJspFiles(String.join(",", jspFiles));
        jspc.setOutputDir(outputDir.getAbsolutePath());
        jspc.setCompile(true);
        jspc.setFailOnError(true);
        jspc.setVerbose(1);
        jspc.setWebXmlInclude(new File(outputDir, "generated-web.xml").getAbsolutePath());
        jspc.execute();
    }
}