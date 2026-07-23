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
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class EAxFilter {

    private static final Pattern FOF_AXIOM = Pattern.compile("(?s)^\\s*fof\\(\\s*([^,]+)\\s*,\\s*axiom\\s*,\\s*\\((.*)\\)\\s*\\)\\.\\s*$");
    private static final Pattern CONJECTURE = Pattern.compile("(?s)^\\s*(?:fof|cnf)\\(\\s*[^,]+\\s*,\\s*(?:conjecture|negated_conjecture)\\s*,.*$");

    public record EAxFilterOptions(String explicitSeeds, String seedSymbols, String seedSubsample, String seedMethod, Path filterFile, boolean forceTptp3) {

        /** Returns normal conjecture-driven filtering options. */
        public static EAxFilterOptions forConjecture() {

            return new EAxFilterOptions(null, null, null, null, null, true);
        }

        /** Returns contradiction filtering options driven by a negated-axiom conjecture. */
        public static EAxFilterOptions forContradictions() {

            return forConjecture();
        }

        /** Returns explicit symbol-seeding options. */
        public static EAxFilterOptions forExplicitSeeds(String seeds) {

            return new EAxFilterOptions(seeds, null, null, "l", null, true);
        }
    }

    public record EAxFilterResult(Path inputProblem, Path outputDirectory, List<Path> generatedProblems, List<String> command, List<String> stdout, List<String> stderr, int exitCode, long elapsedMs, boolean timedOut) {

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

    public record ContradictionProbe(Path problem, String axiomName, String axiom, String conjectureName) {}

    public record ProbeResult(ContradictionProbe probe, EAxFilterResult filterResult) {}

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
     * Runs e_axfilter against a complete TPTP problem containing a conjecture.
     * @param inputProblem complete TPTP problem containing its conjecture
     * @param outputDirectory unique empty directory for this filter run
     * @param options e_axfilter command-line options
     * @param timeoutSeconds maximum wall-clock runtime
     * @return process information and generated TPTP problem files
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
        processBuilder.directory(normalizedOutputDirectory.toFile());
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
        if (!timedOut && exitCode == 0) for (Path problem : generatedProblems) removeTypeDeclarations(problem);
        return new EAxFilterResult(normalizedInput, normalizedOutputDirectory, generatedProblems, command, stdout, stderr, exitCode, elapsedMs, timedOut);
    }

    /**
     * Creates a random negated-axiom conjecture and filters its ontology neighborhood.
     * @param inputProblem complete conjecture-free FOF ontology problem
     * @param outputDirectory unique empty directory for this probe
     * @param timeoutSeconds maximum wall-clock runtime
     * @param random random source used to select the source axiom
     * @return probe metadata and e_axfilter result
     */
    public ProbeResult generateContradictionProbe(Path inputProblem, Path outputDirectory, int timeoutSeconds, Random random) throws IOException, InterruptedException {

        Objects.requireNonNull(random, "random cannot be null");
        if (Files.exists(outputDirectory) && !isDirectoryEmpty(outputDirectory)) throw new IllegalArgumentException("e_axfilter output directory must be empty: " + outputDirectory);
        Path probeDirectory = outputDirectory.resolve("probe");
        Path filteredDirectory = outputDirectory.resolve("filtered");
        Files.createDirectories(probeDirectory);
        ContradictionProbe probe = createContradictionProbe(inputProblem, probeDirectory.resolve("contradiction-probe.p"), random);
        EAxFilterResult filterResult = generate(probe.problem(), filteredDirectory, EAxFilterOptions.forConjecture(), timeoutSeconds);
        return new ProbeResult(probe, filterResult);
    }

    /**
     * Copies a FOF ontology and appends the negation of one existing axiom as its conjecture.
     * @param inputProblem complete conjecture-free FOF ontology problem
     * @param probeProblem destination for the generated probe problem
     * @param random random source used to select the source axiom
     * @return generated probe metadata
     */
    public ContradictionProbe createContradictionProbe(Path inputProblem, Path probeProblem, Random random) throws IOException {

        Objects.requireNonNull(inputProblem, "inputProblem cannot be null");
        Objects.requireNonNull(probeProblem, "probeProblem cannot be null");
        Objects.requireNonNull(random, "random cannot be null");
        if (!Files.isRegularFile(inputProblem)) throw new IllegalArgumentException("TPTP input problem does not exist: " + inputProblem);
        String contents = Files.readString(inputProblem);
        List<String> statements = splitStatements(contents);
        if (statements.stream().anyMatch(statement -> CONJECTURE.matcher(statement).matches())) throw new IllegalArgumentException("Contradiction probe input must not already contain a conjecture");
        List<String> axioms = new ArrayList<>();
        for (String statement : statements) if (FOF_AXIOM.matcher(statement).matches()) axioms.add(statement);
        if (axioms.isEmpty()) throw new IllegalArgumentException("Contradiction probe input contains no FOF axioms: " + inputProblem);
        String selected = axioms.get(random.nextInt(axioms.size()));
        Matcher matcher = FOF_AXIOM.matcher(selected);
        if (!matcher.matches()) throw new IllegalStateException("Unable to parse selected FOF axiom");
        String axiomName = matcher.group(1).trim();
        String axiom = matcher.group(2).trim();
        String conjectureName = "negated_" + axiomName.replaceAll("[^A-Za-z0-9_]", "_");
        Path normalizedProbe = probeProblem.toAbsolutePath().normalize();
        if (normalizedProbe.getParent() != null) Files.createDirectories(normalizedProbe.getParent());
        String separator = contents.endsWith(System.lineSeparator()) ? "" : System.lineSeparator();
        Files.writeString(normalizedProbe, contents + separator + "fof(" + conjectureName + ",conjecture,(~(" + axiom + ")))." + System.lineSeparator());
        return new ContradictionProbe(normalizedProbe, axiomName, axiom, conjectureName);
    }

    private void validateArguments(Path inputProblem, Path outputDirectory, EAxFilterOptions options, int timeoutSeconds) {

        Objects.requireNonNull(inputProblem, "inputProblem cannot be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory cannot be null");
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
            return paths.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(".p")).filter(path -> !path.getFileName().toString().equals(inputFileName)).filter(path -> path.getFileName().toString().startsWith(inputBaseName + "_")).map(path -> path.toAbsolutePath().normalize()).sorted().toList();
        }
    }

    /** Removes TFF type declarations that E emits for an untyped problem. */
    private static void removeTypeDeclarations(Path problem) throws IOException {

        List<String> lines = Files.readAllLines(problem);
        lines.removeIf(line -> line.matches("\\s*tff\\([^,]+,\\s*type\\s*,.*"));
        Files.write(problem, lines);
    }

    /** Splits a TPTP document into complete top-level statements. */
    private static List<String> splitStatements(String contents) {

        List<String> statements = new ArrayList<>();
        StringBuilder statement = new StringBuilder();
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean escaped = false;
        boolean comment = false;
        for (int index = 0; index < contents.length(); index++) {
            char current = contents.charAt(index);
            if (comment) {
                if (current == '\n') comment = false;
                continue;
            }
            if (!singleQuoted && !doubleQuoted && current == '%') {
                comment = true;
                continue;
            }
            statement.append(current);
            if (escaped) {
                escaped = false;
                continue;
            }
            if ((singleQuoted || doubleQuoted) && current == '\\') {
                escaped = true;
                continue;
            }
            if (!doubleQuoted && current == '\'') singleQuoted = !singleQuoted;
            else if (!singleQuoted && current == '"') doubleQuoted = !doubleQuoted;
            else if (!singleQuoted && !doubleQuoted && current == '.') {
                String complete = statement.toString().trim();
                if (!complete.isEmpty()) statements.add(complete);
                statement.setLength(0);
            }
        }
        if (!statement.toString().trim().isEmpty()) throw new IllegalArgumentException("Incomplete TPTP statement at end of input");
        return statements;
    }

    private static String removeExtension(String filename) {

        int finalDot = filename.lastIndexOf('.');
        return finalDot <= 0 ? filename : filename.substring(0, finalDot);
    }

    private static boolean isDirectoryEmpty(Path directory) throws IOException {

        try (DirectoryStream<Path> contents = Files.newDirectoryStream(directory)) {
            return !contents.iterator().hasNext();
        }
    }

    private static List<String> readLinesIfPresent(Path file) throws IOException {

        return Files.isRegularFile(file) ? Files.readAllLines(file) : List.of();
    }

    private static void terminateProcess(Process process) throws InterruptedException {

        process.destroy();
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }
}
