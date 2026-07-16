package com.articulate.sigma.jobscheduler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.List;
import java.io.File;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class CronManager {

    private static final String START = "# BEGIN SIGMAKEE JOBS";
    private static final String END = "# END SIGMAKEE JOBS";
    private static final String MAIN_CLASS = "com.articulate.sigma.jobscheduler.JobScheduler";

    private CronManager() {}

    public static void sync(List<Job> jobs) throws IOException, InterruptedException {
        Path launcher = installLauncher();
        String current = readCrontab();
        String managed = renderManagedBlock(jobs, launcher);
        String updated = replaceManagedBlock(current, managed);
        installCrontab(updated);
    }

    private static Path installLauncher() throws IOException {
        Path sigmaDir = Path.of(System.getProperty("sigmakee.source.dir", System.getProperty("user.dir")))
                .toAbsolutePath().normalize();
        Path launcher = Path.of(System.getProperty("user.home"), ".sigmakee", "bin", "sigmakee-job");
        Path java = Path.of(System.getProperty("java.home"), "bin", "java").toAbsolutePath().normalize();

        Files.createDirectories(launcher.getParent());

        String script = "#!/usr/bin/env bash\n"
                + "set -euo pipefail\n"
                + "cd " + quote(sigmaDir.toString()) + "\n"
                + "exec " + quote(java.toString())
                + " -cp " + quote("build/classes:lib/*")
                + " " + MAIN_CLASS + " \"$@\"\n";

        Files.writeString(launcher, script, StandardCharsets.UTF_8);

        if (!launcher.toFile().setExecutable(true, true))
            throw new IOException("Could not make launcher executable: " + launcher);

        return launcher;
    }

    private static String renderManagedBlock(List<Job> jobs, Path launcher) {
        StringBuilder result = new StringBuilder(START).append('\n');

        for (Job job : jobs) {
            if (job.isEnabled() && job.getExecutionMode() == Job.ExecutionMode.CRON) {
                validateJobId(job.getId());
                result.append(cronExpression(job)).append(' ').append(javaCommand(launcher, job.getId()));
            }
        }

        return result.append(END).append('\n').toString();
    }

    private static String javaCommand(Path launcher, String jobId) {
        Path logFile = Path.of(System.getProperty("user.home"), ".sigmakee", "logs", "jobs", jobId + ".log");
        try {
            Files.createDirectories(logFile.getParent());
        }
        catch (IOException exception) {
            throw new IllegalStateException("Could not create job log directory", exception);
        }
        String command = quote(launcher.toString()) + " --run " + quote(jobId)
                + " >> " + quote(logFile.toString()) + " 2>&1";
        return command.replace("%", "\\%");
    }

    private static String cronExpression(Job job) {

        Schedule schedule = job.getSchedule();
        int minute = schedule.getRunTime().getMinute();
        int hour = schedule.getRunTime().getHour();
        switch (schedule.getFrequency()) {
            case DAILY:
                return minute + " " + hour + " * * *";
            case WEEKLY:
                int day = schedule.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : schedule.getDayOfWeek().getValue();
                return minute + " " + hour + " * * " + day;
            case MONTHLY:
                return minute + " " + hour + " " + schedule.getDayOfMonth() + " * *";
            default:
                throw new IllegalArgumentException("CRON does not support " + schedule.getFrequency() + " jobs");
        }
    }

    private static String readCrontab() throws IOException, InterruptedException {
        Process process = new ProcessBuilder("crontab", "-l").start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();

        if (exitCode == 0) return output;
        if (error.toLowerCase().contains("no crontab")) return "";
        throw new IOException("Could not read crontab: " + error.trim());
    }

    private static String replaceManagedBlock(String current, String managed) {
        int start = current.indexOf(START);
        int end = current.indexOf(END);

        if (start < 0 && end < 0) {
            String separator = current.isBlank() || current.endsWith("\n") ? "" : "\n";
            return current + separator + managed;
        }

        if (start < 0 || end < start)
            throw new IllegalStateException("Existing SigmaKEE CRON block is malformed");

        end += END.length();
        if (end < current.length() && current.charAt(end) == '\n') end++;
        return current.substring(0, start) + managed + current.substring(end);
    }

    private static void installCrontab(String contents) throws IOException, InterruptedException {
        Path temporaryFile = Files.createTempFile("sigmakee-crontab-", ".txt");

        try {
            Files.writeString(temporaryFile, contents, StandardCharsets.UTF_8);
            Process process = new ProcessBuilder("crontab", temporaryFile.toString()).inheritIO().start();
            int exitCode = process.waitFor();
            if (exitCode != 0) throw new IOException("crontab exited with code " + exitCode);
        }
        finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static void validateJobId(String jobId) {
        if (jobId == null || !jobId.matches("[A-Za-z0-9._-]+"))
            throw new IllegalArgumentException("Invalid job ID for CRON: " + jobId);
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private static String absoluteClasspath() {
        String workingDirectory = System.getProperty("user.dir");
        return Arrays.stream(System.getProperty("java.class.path").split(Pattern.quote(File.pathSeparator)))
                .map(entry -> {
                    Path path = entry.isBlank() ? Path.of(workingDirectory) : Path.of(entry);
                    return path.isAbsolute() ? path.normalize().toString()
                            : Path.of(workingDirectory).resolve(path).normalize().toString();
                })
                .collect(Collectors.joining(File.pathSeparator));
    }
}