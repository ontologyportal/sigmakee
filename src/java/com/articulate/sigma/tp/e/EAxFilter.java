package com.articulate.sigma.tp.e;

import com.articulate.sigma.KBmanager;
import com.articulate.sigma.utils.StringUtil;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class EAxFilter {

    public record EAxFilterOptions(String explicitSeeds, String seedSymbols, String seedSubsample, String seedMethod, Path filterFile, boolean forceTptp3) {

        /**
         * Default filtering for a problem containing a meaningful conjecture.
         * The conjecture's symbols are used as the initial SinE seeds.
         */
        public static EAxFilterOptions forConjecture() {return new EAxFilterOptions(null, null, null, null, null, true);}

        /**
         * Artificial predicate seeding for a consistency problem whose
         * conjecture is $false and therefore contains no useful seed symbols.
         */
        public static EAxFilterOptions forContradictions() {return new EAxFilterOptions(null, "p", "m100", "l", null, true);}

        /**
         * Explicitly seed filtering from one or more TPTP symbols.
         */
        public static EAxFilterOptions forExplicitSeeds(String seeds) {return new EAxFilterOptions(seeds, null, null, "l", null, true);}
    }

    public record EAxFilterResult(
            Path inputProblem,
            Path outputDirectory,
            List<Path> generatedProblems,
            List<String> command,
            List<String> stdout,
            List<String> stderr,
            int exitCode,
            long elapsedMs,
            boolean timedOut) {

        public EAxFilterResult {

            generatedProblems = List.copyOf(generatedProblems);
            command = List.copyOf(command);
            stdout = List.copyOf(stdout);
            stderr = List.copyOf(stderr);
        }

        public boolean succeeded() {

            return !timedOut && exitCode == 0 && !generatedProblems.isEmpty();
        }
    }

    private final Path eaxFilterExecutable;

    public EAxFilter() {

        String executable = KBmanager.configuration.getEaxFilterExec();
        this.eaxFilterExecutable = StringUtil.isNonEmptyString(executable) ? Paths.get(executable) : null;
    }

    public static boolean isAvailable() {

        String executable = KBmanager.configuration.getEaxFilterExec();
        return StringUtil.isNonEmptyString(executable) && Files.isRegularFile(Paths.get(executable)) && Files.isExecutable(Paths.get(executable));
    }

    /**
     * Runs e_axfilter against a complete TPTP problem.
     * The input problem is copied into the output directory because
     * e_axfilter generates its output files alongside the input file.
     * The output directory must be empty so that stale files from a
     * previous run cannot be mistaken for newly generated problems.
     * @param inputProblem complete TPTP problem containing its conjecture
     * @param outputDirectory unique empty directory for this filter run
     * @param options e_axfilter command-line options
     * @param timeoutSeconds maximum wall-clock runtime
     * @return process information and generated TPTP problem files
     * @throws IOException if files cannot be prepared or the process cannot start
     * @throws InterruptedException if the calling thread is interrupted
     */
    public EAxFilterResult generate(Path inputProblem, Path outputDirectory, EAxFilterOptions options, int timeoutSeconds) throws IOException, InterruptedException {

        validateArguments(inputProblem, outputDirectory, options, timeoutSeconds);
        Files.createDirectories(outputDirectory);
        if (!isDirectoryEmpty(outputDirectory)) throw new IllegalArgumentException("e_axfilter output directory must be empty: " + outputDirectory);
        Path normalizedInput = inputProblem.toAbsolutePath().normalize();
        Path normalizedOutputDirectory = outputDirectory.toAbsolutePath().normalize();
        Path localProblem = normalizedOutputDirectory.resolve(normalizedInput.getFileName());
        Files.copy(normalizedInput, localProblem, StandardCopyOption.REPLACE_EXISTING);
        List<String> command = createCommand(localProblem, options);
        Path stdoutFile = normalizedOutputDirectory.resolve("e_axfilter.stdout.log");
        Path stderrFile = normalizedOutputDirectory.resolve("e_axfilter.stderr.log");
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory( normalizedOutputDirectory.toFile());
        processBuilder.redirectOutput(stdoutFile.toFile());
        processBuilder.redirectError(stderrFile.toFile());
        long startTime = System.currentTimeMillis();
        Process process = processBuilder.start();
        boolean completed;
        try {
            completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        }
        catch (InterruptedException exception) {
            terminateProcess(process);
            Thread.currentThread().interrupt();
            throw exception;
        }
        boolean timedOut = !completed;
        if (timedOut) terminateProcess(process);
        long elapsedMs = System.currentTimeMillis() - startTime;
        int exitCode = timedOut ? -1 : process.exitValue();
        List<String> stdout = readLinesIfPresent(stdoutFile);
        List<String> stderr = readLinesIfPresent(stderrFile);
        List<Path> generatedProblems = findGeneratedProblems(normalizedOutputDirectory, localProblem);
        return new EAxFilterResult(normalizedInput, normalizedOutputDirectory, generatedProblems, command, stdout, stderr, exitCode, elapsedMs, timedOut);
    }

    private void validateArguments(Path inputProblem, Path outputDirectory, EAxFilterOptions options, int timeoutSeconds) {

        Objects.requireNonNull(inputProblem,"inputProblem cannot be null");
        Objects.requireNonNull(outputDirectory,"outputDirectory cannot be null");
        Objects.requireNonNull(options, "options cannot be null");
        if (eaxFilterExecutable == null || !Files.isRegularFile(eaxFilterExecutable)) throw new IllegalStateException("e_axfilter executable does not exist: " + eaxFilterExecutable);
        if (!Files.isExecutable(eaxFilterExecutable)) throw new IllegalStateException("e_axfilter is not executable: " + eaxFilterExecutable);
        if (!Files.isRegularFile(inputProblem)) throw new IllegalArgumentException("TPTP input problem does not exist: " + inputProblem);
        if (timeoutSeconds <= 0) throw new IllegalArgumentException("timeoutSeconds must be greater than zero");
        if (options.filterFile() != null && !Files.isRegularFile(options.filterFile())) throw new IllegalArgumentException("e_axfilter definition file does not exist: " + options.filterFile());
        if (StringUtil.isNonEmptyString(options.explicitSeeds()) && StringUtil.isNonEmptyString(options.seedSymbols())) throw new IllegalArgumentException("explicitSeeds and seedSymbols cannot both be set");
    }

    private List<String> createCommand(Path localProblem, EAxFilterOptions options) {

        List<String> command = new ArrayList<>();
        command.add(eaxFilterExecutable.toAbsolutePath().normalize().toString());
        command.add("--verbose=1");
        if (options.forceTptp3()) command.add("--tstp-format");
        if (options.filterFile() != null) command.add("--filter=" + options.filterFile().toAbsolutePath().normalize());
        if (StringUtil.isNonEmptyString(options.explicitSeeds())) command.add("--seeds=" + options.explicitSeeds());
        if (StringUtil.isNonEmptyString(options.seedSymbols())) command.add("--seed-symbols=" + options.seedSymbols());
        if (StringUtil.isNonEmptyString(options.seedSubsample())) command.add("--seed-subsample=" + options.seedSubsample());
        if (StringUtil.isNonEmptyString(options.seedMethod())) command.add("--seed-method=" + options.seedMethod());
        command.add(localProblem.getFileName().toString());
        return command;
    }

    private List<Path> findGeneratedProblems(Path outputDirectory, Path localProblem) throws IOException {
        
        String inputFileName = localProblem.getFileName().toString();
        String inputBaseName = removeExtension(inputFileName);
        try (Stream<Path> paths = Files.list(outputDirectory)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(path ->
                        path.getFileName()
                            .toString()
                            .endsWith(".p"))
                .filter(path ->
                        !path.getFileName()
                            .toString()
                            .equals(inputFileName))
                .filter(path ->
                        path.getFileName()
                            .toString()
                            .startsWith(inputBaseName + "_"))
                .map(path ->
                        path.toAbsolutePath()
                        .normalize())
                .sorted()
                .toList();
        }
    }

    private static String removeExtension(String filename) {

        int finalDot = filename.lastIndexOf('.');
        if (finalDot <= 0) return filename;
        return filename.substring(0, finalDot);
    }

    private static boolean isDirectoryEmpty(Path directory) throws IOException {

        try (DirectoryStream<Path> contents = Files.newDirectoryStream(directory)) {
            return !contents.iterator().hasNext();
        }
    }

    private static List<String> readLinesIfPresent(Path file) throws IOException {
        
        if (!Files.isRegularFile(file)) return List.of();
        return Files.readAllLines(file);
    }

    private static void terminateProcess(Process process) throws InterruptedException {
        
        process.destroy();
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }
}