package com.articulate.sigma.trans;

import com.articulate.sigma.UnitTestBase;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class TPTPUtilTest extends UnitTestBase {

    @Test
    public void testClearProofFile() {
        List<String> input = Arrays.asList(
                "% Header comment",
                "",
                "fof(f1, axiom,",
                "    (p(a) => q(a))).",
                "  fof(f2, conjecture,",
                "      (q(a))).",
                "% Footer comment"
        );

        List<String> expected = Arrays.asList(
                "% Header comment",
                "fof(f1, axiom, (p(a) => q(a))).",
                "fof(f2, conjecture, (q(a))).",
                "% Footer comment"
        );

        List<String> actual = TPTPutil.clearProofFile(input);
        assertEquals(expected, actual);
    }


    @Test
    public void dropOnePremiseFormulasFOFTest() {
        List<String> input = Arrays.asList(
                "% Refutation found. Thanks to Tanya!",
                "% SZS status Theorem for min-problem",
                "% SZS output start Proof for min-problem",
                "fof(f1,axiom,( s__instance(s__wife__m,s__Relation)), file('/Users/Mac_1/workspace/sigmakee/min-problem.tptp',unknown)).",
                "fof(f2,conjecture,( ? [X0] : s__instance(X0,s__Relation)), file('/Users/Mac_1/workspace/sigmakee/min-problem.tptp',unknown)).",
                "fof(f3,negated_conjecture,( ~? [X0] : s__instance(X0,s__Relation)), inference(negated_conjecture,[status(cth)],[f2])).",
                "fof(f4,plain,( ! [X0] : ~s__instance(X0,s__Relation)), inference(ennf_transformation,[],[f3])).",
                "fof(f5,plain,( s__instance(s__wife__m,s__Relation)), inference(cnf_transformation,[],[f1])).",
                "fof(f6,plain,( ( ! [X0] : (~s__instance(X0,s__Relation)) )), inference(cnf_transformation,[],[f4])).",
                "fof(f7,plain,( $false), inference(resolution,[],[f5,f6])).",
                "% SZS output end Proof for min-problem"
        );

        List<String> expected = Arrays.asList(
                "% Refutation found. Thanks to Tanya!",
                "% SZS status Theorem for min-problem",
                "% SZS output start Proof for min-problem",
                "fof(1,axiom, (s__instance(s__wife__m,s__Relation)), file('/Users/Mac_1/workspace/sigmakee/min-problem.tptp',unknown)).",
                "fof(2,conjecture, (?[X0] : s__instance(X0,s__Relation)), file('/Users/Mac_1/workspace/sigmakee/min-problem.tptp',unknown)).",
                "fof(3,negated_conjecture, (~?[X0] : s__instance(X0,s__Relation)), inference(negated_conjecture,[status(cth)],[2])).",
                "fof(4,plain, ($false), inference(resolution,[],[1,2])).",
                "% SZS output end Proof for min-problem"
        );

        List<String> out = TPTPutil.dropOnePremiseFormulasFOF(input);

        // Keeps header/footer comments
        assertEquals("% Refutation found. Thanks to Tanya!", out.get(0));
        assertEquals("% SZS output start Proof for min-problem",out.get(2));
        assertEquals("% SZS output end Proof for min-problem", out.get(out.size() - 1));

        assertEquals(expected, out);
    }

    @Test
    public void testExtractIncludesFromTPTP() throws IOException {
        // Create a temporary test file
        File tmp = File.createTempFile("test", ".tptp");
        tmp.deleteOnExit();

        String content =
                "include('SUMOMILO.tptp').\n" +
                "include('extra_axioms.tptp').\n" +
                "fof(kb1, axiom, (s__instance(s__Object, s__Class))).\n";

        try (FileWriter fw = new FileWriter(tmp)) {
            fw.write(content);
        }

        // Call method under test
        List<String> includes = TPTPutil.extractIncludesFromTPTP(tmp);

        // Assertions
        assertEquals("Should detect 2 include statements", 2, includes.size());
        assertTrue(includes.contains("SUMOMILO.tptp"));
        assertTrue(includes.contains("extra_axioms.tptp"));
    }

    @Test
    public void testExtractIncludesFromTPTP_NoIncludes() throws IOException {
        File tmp = File.createTempFile("test_no_include", ".tptp");
        tmp.deleteOnExit();
        try (FileWriter fw = new FileWriter(tmp)) {
            fw.write("fof(ax1, axiom, (p(a))).");
        }

        List<String> includes = TPTPutil.extractIncludesFromTPTP(tmp);
        assertTrue("Should return empty list when no includes", includes.isEmpty());
    }

    @Test
    public void testValidateIncludes_AllExist() throws IOException {
        File incDir = Files.createTempDirectory("includes_ok").toFile();
        incDir.deleteOnExit();

        new File(incDir, "SUMOMILO.tptp").createNewFile();
        new File(incDir, "extra_axioms.tptp").createNewFile();

        List<String> includes = Arrays.asList("SUMOMILO.tptp", "extra_axioms.tptp");
        String result = TPTPutil.validateIncludesInTPTPFiles(includes, incDir.getAbsolutePath());

        assertNull("Should return null when all includes exist", result);
    }

    @Test
    public void testValidateIncludes_MissingFile() throws IOException {
        File incDir = Files.createTempDirectory("includes_missing").toFile();
        incDir.deleteOnExit();

        new File(incDir, "SUMOMILO.tptp").createNewFile();

        List<String> includes = Arrays.asList("SUMOMILO.tptp", "extra_axioms.tptp");
        String result = TPTPutil.validateIncludesInTPTPFiles(includes, incDir.getAbsolutePath());

        assertNotNull("Should detect missing include", result);
        assertTrue("Error message should mention missing file", result.contains("Missing include file"));
    }

    @Test
    public void testValidateIncludes_MissingDirectory() {
        String badDir = "/nonexistent/path/to/includes";
        List<String> includes = Collections.singletonList("SUMOMILO.tptp");

        String result = TPTPutil.validateIncludesInTPTPFiles(includes, badDir);

        assertNotNull("Should detect missing include directory", result);
        assertTrue("Error should mention missing directory", result.contains("Include directory not found"));
    }

}
