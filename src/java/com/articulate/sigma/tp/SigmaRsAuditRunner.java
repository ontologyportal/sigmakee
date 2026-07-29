package com.articulate.sigma.tp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class SigmaRsAuditRunner {

    public record Options(
        int timeoutSeconds,
        int contradictionLimit,
        double thoroughness,
        double scope
    ) {
        public Options {
            if (timeoutSeconds < 1)
                throw new IllegalArgumentException("timeoutSeconds must be positive");
            if (contradictionLimit < 1)
                throw new IllegalArgumentException("contradictionLimit must be positive");
            if (thoroughness <= 0.0 || thoroughness > 1.0)
                throw new IllegalArgumentException("thoroughness must be in (0, 1]");
            if (scope < 1.0)
                throw new IllegalArgumentException("scope must be at least 1");
        }

        public static Options defaults() {
            return new Options(60, 64, 1.0, 2.0);
        }
    }

    public record Result(
        List<String> command,
        int exitCode,
        boolean timedOut,
        String report,
        String diagnostics
    ) {}

    private final Path executable;

    public SigmaRsAuditRunner(Path executable) {
        this.executable = executable.toAbsolutePath().normalize();
    }

    public Result audit(
        List<Path> kifFiles,
        Path focusFile,
        Options options
    ) throws IOException, InterruptedException {

        List<String> command = new ArrayList<>();

        command.add(executable.toString());
        command.add("--ugly");
        command.add("audit");
        command.add("--backend");
        command.add("native");

        if (focusFile != null)
            command.add(focusFile.toAbsolutePath().normalize().toString());

        command.add("--timeout");
        command.add(Integer.toString(options.timeoutSeconds()));

        command.add("--limit");
        command.add(Integer.toString(options.contradictionLimit()));

        command.add("--thoroughness");
        command.add(Double.toString(options.thoroughness()));

        command.add("--scope");
        command.add(Double.toString(options.scope()));

        command.add("--proof");
        command.add("none");

        for (Path file : kifFiles) {
            command.add("-f");
            command.add(file.toAbsolutePath().normalize().toString());
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().put("NO_COLOR", "1");
        builder.environment().put("NO_PAGER", "1");

        Process process = builder.start();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> stdout = executor.submit(() ->
                new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
                )
            );

            Future<String> stderr = executor.submit(() ->
                new String(
                    process.getErrorStream().readAllBytes(),
                    StandardCharsets.UTF_8
                )
            );

            // Allow a little time beyond sigma-rs's internal prover timeout
            // for loading, selection, proof extraction, and cleanup.
            Duration wallLimit =
                Duration.ofSeconds(options.timeoutSeconds() + 30L);

            boolean completed =
                process.waitFor(wallLimit.toMillis(), TimeUnit.MILLISECONDS);

            if (!completed) {
                process.descendants().forEach(ProcessHandle::destroy);
                process.destroy();

                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.descendants().forEach(ProcessHandle::destroyForcibly);
                    process.destroyForcibly();
                    process.waitFor();
                }
            }

            return new Result(
                List.copyOf(command),
                completed ? process.exitValue() : -1,
                !completed,
                await(stdout),
                await(stderr)
            );
        }
    }

    private static String await(Future<String> future)
        throws IOException, InterruptedException {

        try {
            return future.get();
        }
        catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException io)
                throw io;
            throw new IOException("Unable to capture sigma-rs output", cause);
        }
    }

    public static void main(String[] args) {
        Path sigmaRs =
            Path.of("/home/shaun/workspace/sigma-rs/target/release/sumo");
        SigmaRsAuditRunner runner = new SigmaRsAuditRunner(sigmaRs);
        try {
            SigmaRsAuditRunner.Result result = runner.audit(
                List.of(
                    Path.of("/home/shaun/.sigmakee/KBs/Merge.kif"),
                    Path.of("/home/shaun/.sigmakee/KBs/Mid-level-ontology.kif")
                ),
                null,
                SigmaRsAuditRunner.Options.defaults()
            );
            System.out.println("Command: " + result.command());
            System.out.println("Exit code: " + result.exitCode());
            System.out.println("Java timeout: " + result.timedOut());
            if (!result.report().isBlank()) {
                System.out.println(result.report());
            }
            if (!result.diagnostics().isBlank()) {
                System.err.println(result.diagnostics());
            }
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("sigma-rs audit was interrupted");
        }
        catch (IOException exception) {
            System.err.println(
                "Unable to run sigma-rs audit: " + exception.getMessage()
            );
            exception.printStackTrace(System.err);
        }
    }
}