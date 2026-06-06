package com.articulate.sigma.tp.tests;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;

public class ITS {
    // Collect all inference test files
    // Parse their meta data
    // Assert their assertions with session isolation, loading KB cache.
    // Run conjecture, store results in result list.
    private String inferenceTestDir;

    private List<InferenceTest> inferenceTests = new ArrayList<>();

    public KB kb;

    public ITS(KB kb) {
        this.kb = kb;
        this.inferenceTestDir = KBmanager.getMgr().getPref("inferenceTestDir");
    }

    private void loadAllInferenceTests() {

        List<String> tqFilePaths = new ArrayList<>();
        Path root = Paths.get(this.inferenceTestDir);
        try (Stream<Path> paths = Files.walk(root)) {
            paths
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".tq"))
                .map(root::relativize)
                .map(p -> p.toString().replace(File.separatorChar, '/'))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(tqFilePaths::add);
        }
        catch (IOException e) {
            System.err.println("Error in InferenceTestSuite.loadInferenceTestPaths(): " + e.getMessage());
            e.printStackTrace();
        }
        for (String path : tqFilePaths) this.inferenceTests.add(new InferenceTest(path));
    }
}