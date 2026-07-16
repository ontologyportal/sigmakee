package com.articulate.sigma.jobscheduler;

import com.articulate.sigma.utils.LoggingUtils;
import com.articulate.sigma.parsing.CLIMapParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Scanner;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;


/******************************************************************
 */
public final class JobScheduler {

    /**  */
    private final ScheduledExecutorService executor;
    /**  */
    private final List<Job> jobs = new ArrayList<>();
    /**  */
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    /**  */
    private final JobScheduleXML scheduleXML;
    private static final Scanner INPUT = new Scanner(System.in);

    /******************************************************************
     */
    public JobScheduler() {

        executor = Executors.newSingleThreadScheduledExecutor();
        scheduleXML = new JobScheduleXML();
        try {
            jobs.addAll(scheduleXML.load());
            for (Job job : jobs) if (job.isEnabled() && job.getExecutionMode() == Job.ExecutionMode.TOMCAT) scheduleNextRun(job);
        }
        catch (IOException exception) {
            executor.shutdownNow();
            throw new IllegalStateException("Could not load the job schedule", exception);
        }
    }

    /******************************************************************
     */
    private void saveJobs() {

        try {
            scheduleXML.save(jobs);
            CronManager.sync(jobs);
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("CRON synchronization interrupted", exception);
        }
        catch (IOException exception) {
            throw new IllegalStateException("Could not save the job schedule", exception);
        }
    }

    /******************************************************************
     */
    public synchronized void addJob(Job job) {

        if (job == null) throw new IllegalArgumentException("Job cannot be null");
        if (job.getSchedule() == null) throw new IllegalArgumentException("Job must have a schedule");
        if (findJob(job.getId()) != null) throw new IllegalArgumentException("Duplicate job ID: " + job.getId());
        jobs.add(job);
        if (job.isEnabled() && job.getExecutionMode() == Job.ExecutionMode.TOMCAT) scheduleNextRun(job);
        saveJobs();
    }

    /******************************************************************
     */
    private void scheduleNextRun(Job job) {

        if (!job.isEnabled() || job.getExecutionMode() != Job.ExecutionMode.TOMCAT || executor.isShutdown()) return;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = job.getSchedule().nextRunAfter(now);
        if (nextRun == null) {
            LoggingUtils.log("No future execution for job: " + job.getTitle());
            return;
        }
        long delayMillis = Math.max(0, Duration.between(now, nextRun).toMillis());
        System.out.println("Scheduled \"" + job.getTitle() + "\" for " + nextRun);
        ScheduledFuture<?> future = executor.schedule(() -> executeJob(job), delayMillis, TimeUnit.MILLISECONDS);
        scheduledTasks.put(job.getId(), future);
    }

    /******************************************************************
     */
    private void executeJob(Job job) {

        scheduledTasks.remove(job.getId());
        if (!job.isEnabled()) return;
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            LoggingUtils.log("Starting \"" + job.getTitle() + "\" at " + startedAt);
            job.run();
            LoggingUtils.log("Completed \"" + job.getTitle() + "\" at " + LocalDateTime.now());
        }
        catch (Exception exception) {
            LoggingUtils.log("ERROR", "Job failed: " + job.getTitle());
            exception.printStackTrace();
        }
        finally {
            if (job.getSchedule().getFrequency() != Schedule.Frequency.ONCE) scheduleNextRun(job);
        }
    }

    public void runJobNow(String jobId) throws Exception {

        Job job = requireJob(jobId);
        if (!job.isEnabled()) throw new IllegalStateException("Job is disabled: " + jobId);
        job.run();
    }

    /******************************************************************
     * Updates a job and reschedules it when Tomcat execution is enabled.
     */
    public synchronized void updateJob(String jobId, boolean enabled, Job.ExecutionMode executionMode, Schedule schedule) {

        if (executionMode == null) throw new IllegalArgumentException("Execution mode cannot be null");
        if (schedule == null) throw new IllegalArgumentException("Schedule cannot be null");
        Job job = requireJob(jobId);
        cancelScheduledTask(jobId);
        job.setEnabled(enabled);
        job.setExecutionMode(executionMode);
        job.setSchedule(schedule);
        if (enabled && executionMode == Job.ExecutionMode.TOMCAT) scheduleNextRun(job);
        saveJobs();
    }

    /******************************************************************
     */
    public synchronized void setEnabled(String jobId, boolean enabled) {
        
        Job job = requireJob(jobId);
        job.setEnabled(enabled);
        cancelScheduledTask(jobId);
        if (enabled && job.getExecutionMode() == Job.ExecutionMode.TOMCAT) scheduleNextRun(job);
        saveJobs();
    }

    /******************************************************************
     */
    public synchronized void removeJob(String jobId) {

        requireJob(jobId);
        cancelScheduledTask(jobId);
        jobs.removeIf(job -> job.getId().equals(jobId));
        saveJobs();
    }

    /******************************************************************
     */
    public synchronized List<Job> getJobs() {

        return Collections.unmodifiableList(new ArrayList<>(jobs));
    }

    /******************************************************************
     */
    public Path getScheduleFile() {

        return scheduleXML.getScheduleFile();
    }

    public Job getJob(String jobId) {
        return requireJob(jobId);
    }

    /******************************************************************
     */
    private Job requireJob(String jobId) {

        Job job = findJob(jobId);
        if (job == null) throw new IllegalArgumentException("Unknown job ID: " + jobId);
        return job;
    }

    private static void listJobs(JobScheduler scheduler) {
        System.out.printf("%-9s %-24s %-7s %-24s %s%n", "STATUS", "ID", "MODE", "SCHEDULE", "TYPE");
        for (Job job : scheduler.getJobs()) {
            System.out.printf("%-9s %-24s %-7s %-24s %s%n",
                job.isEnabled() ? "ENABLED" : "DISABLED",
                job.getId(),
                job.getExecutionMode(),
                formatSchedule(job.getSchedule()),
                job.getType());
        }
    }

    /******************************************************************
     */
    private Job findJob(String jobId) {

        for (Job job : jobs) if (job.getId().equals(jobId)) return job;
        return null;
    }

    /******************************************************************
     */
    private void cancelScheduledTask(String jobId) {

        ScheduledFuture<?> future = scheduledTasks.remove(jobId);
        if (future != null) future.cancel(false);
    }

    /******************************************************************
     */
    public synchronized void shutdown() {

        for (ScheduledFuture<?> future : scheduledTasks.values()) future.cancel(false);
        scheduledTasks.clear();
        executor.shutdown();
    }

    private static String required(Map<String,List<String>> options, String key) {

        List<String> values = options.get(key);
        if (values == null || values.size() != 1 || values.get(0).isBlank())
            throw new IllegalArgumentException("--" + key + " requires one value");
        return values.get(0);
    }

    private static int integer(Map<String,List<String>> options, String key, int fallback) {

        List<String> values = options.get(key);
        if (values == null || values.isEmpty()) return fallback;
        try {
            return Integer.parseInt(values.get(0));
        }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException("--" + key + " must be an integer");
        }
    }

    private static void addJobFromCommandLine(JobScheduler scheduler, Map<String,List<String>> options) {
    
        String id = required(options, "id");
        String type = required(options, "type");
        Job.ExecutionMode mode = Job.ExecutionMode.valueOf(required(options, "mode").toUpperCase());
        Schedule schedule = parseSchedule(options);
        Job job;
        switch (type.toLowerCase()) {
            case "consistency-check":
                job = new ConsistencyCheckJob(id, required(options, "kb"), schedule);
                break;
            case "eaxfilter-contradiction":
                job = new EAxFilterContradictionJob(id, required(options, "kb"), schedule,
                        integer(options, "cnf-timeout", 300),
                        integer(options, "filter-timeout", 300),
                        integer(options, "vampire-timeout", 30));
                break;
            case "sumo-update":
                job = new SumoUpdateJob(id, schedule);
                break;
            default:
                throw new IllegalArgumentException("Unknown job type: " + type);
        }
        if (mode == Job.ExecutionMode.CRON && schedule.getFrequency() == Schedule.Frequency.ONCE)
            throw new IllegalArgumentException("ONCE jobs are not supported by CRON; use TOMCAT or the at command");
        if (mode == Job.ExecutionMode.CRON && schedule.getFrequency() == Schedule.Frequency.MONTHLY && schedule.getDayOfMonth() > 28)
            throw new IllegalArgumentException("CRON monthly jobs currently require a day between 1 and 28");
        job.setExecutionMode(mode);
        job.setEnabled(!options.containsKey("disabled"));
        scheduler.addJob(job);
        if (mode == Job.ExecutionMode.CRON) System.out.println("Run --cron " + id + " to print its crontab entry.");
    }

    private static void editJobFromCommandLine(JobScheduler scheduler, Map<String,List<String>> options) {

        String id = required(options, "edit");
        Job current = scheduler.getJob(id);
        boolean enabled = current.isEnabled();
        if (options.containsKey("enabled")) enabled = true;
        if (options.containsKey("disabled")) enabled = false;
        Job.ExecutionMode mode = current.getExecutionMode();
        if (options.containsKey("mode")) mode = Job.ExecutionMode.valueOf(required(options, "mode").toUpperCase());
        Schedule schedule = hasScheduleOptions(options) ? parseSchedule(options) : current.getSchedule();
        validateModeAndSchedule(mode, schedule);
        scheduler.updateJob(id, enabled, mode, schedule);
        System.out.println("Updated job: " + id);
    }

    private static boolean hasScheduleOptions(Map<String,List<String>> options) {
        return options.containsKey("frequency") || options.containsKey("time") || options.containsKey("date") || options.containsKey("day");
    }

    private static void validateModeAndSchedule(Job.ExecutionMode mode, Schedule schedule) {

        if (mode != Job.ExecutionMode.CRON) return;
        if (schedule.getFrequency() == Schedule.Frequency.ONCE) throw new IllegalArgumentException("ONCE jobs are not supported by CRON");
        if (schedule.getFrequency() == Schedule.Frequency.MONTHLY && schedule.getDayOfMonth() > 28)
            throw new IllegalArgumentException("CRON monthly jobs currently require a day between 1 and 28");
    }

    private static Schedule parseSchedule(Map<String,List<String>> options) {

        String frequency = required(options, "frequency").toUpperCase();
        LocalTime time = LocalTime.parse(required(options, "time"));
        switch (Schedule.Frequency.valueOf(frequency)) {
            case ONCE:
                return Schedule.onceAt(LocalDate.parse(required(options, "date")).atTime(time));
            case DAILY:
                return Schedule.dailyAt(time);
            case WEEKLY:
                return Schedule.weeklyAt(DayOfWeek.valueOf(required(options, "day").toUpperCase()), time);
            case MONTHLY:
                return Schedule.monthlyAt(integer(options, "day", -1), time);
            default:
                throw new IllegalArgumentException("Unsupported frequency: " + frequency);
        }
    }

    private static String formatSchedule(Schedule schedule) {
        switch (schedule.getFrequency()) {
            case ONCE:
                return schedule.getRunDate() + " " + schedule.getRunTime();
            case DAILY:
                return "daily at " + schedule.getRunTime();
            case WEEKLY:
                return schedule.getDayOfWeek() + " at " + schedule.getRunTime();
            case MONTHLY:
                return "day " + schedule.getDayOfMonth() + " at " + schedule.getRunTime();
            default:
                return schedule.getFrequency().name();
        }
    }

    private static void executeCommand(JobScheduler scheduler, Map<String,List<String>> options) throws Exception {
        if (options.containsKey("list")) listJobs(scheduler);
        else if (options.containsKey("add")) addJobFromCommandLine(scheduler, options);
        else if (options.containsKey("edit")) editJobFromCommandLine(scheduler, options);
        else if (options.containsKey("remove")) scheduler.removeJob(required(options, "remove"));
        else if (options.containsKey("enable")) scheduler.setEnabled(required(options, "enable"), true);
        else if (options.containsKey("disable")) scheduler.setEnabled(required(options, "disable"), false);
        else if (options.containsKey("run")) scheduler.runJobNow(required(options, "run"));
        else if (options.containsKey("sync-cron")) {
            CronManager.sync(scheduler.getJobs());
            System.out.println("SigmaKEE CRON jobs synchronized.");
        }
        else throw new IllegalArgumentException("Unknown command. Use --help.");
    }

    private static void interactiveMenu(JobScheduler scheduler) {
        while (true) {
            System.out.println();
            System.out.println("SigmaKEE Job Scheduler");
            System.out.println("1. List jobs");
            System.out.println("2. Add job");
            System.out.println("3. Edit job");
            System.out.println("4. Enable job");
            System.out.println("5. Disable job");
            System.out.println("6. Run job now");
            System.out.println("7. Delete job");
            System.out.println("8. Synchronize CRON");
            System.out.println("0. Exit");

            try {
                switch (prompt("Selection")) {
                    case "1":
                        listJobs(scheduler);
                        break;
                    case "2":
                        addJobInteractively(scheduler);
                        break;
                    case "3":
                        editJobInteractively(scheduler);
                        break;
                    case "4":
                        scheduler.setEnabled(selectJob(scheduler), true);
                        System.out.println("Job enabled.");
                        break;
                    case "5":
                        scheduler.setEnabled(selectJob(scheduler), false);
                        System.out.println("Job disabled.");
                        break;
                    case "6":
                        scheduler.runJobNow(selectJob(scheduler));
                        System.out.println("Job completed.");
                        break;
                    case "7":
                        deleteJobInteractively(scheduler);
                        break;
                    case "8":
                        CronManager.sync(scheduler.getJobs());
                        System.out.println("CRON jobs synchronized.");
                        break;
                    case "0":
                        return;
                    default:
                        System.out.println("Enter a number from 0 through 8.");
                }
            }
            catch (Exception exception) {
                System.err.println("Error: " + exception.getMessage());
            }
        }
    }

    private static void addJobInteractively(JobScheduler scheduler) {
        Map<String,List<String>> options = new java.util.HashMap<>();
        put(options, "add", "");
        put(options, "id", requiredPrompt("Job ID"));

        System.out.println("Job type:");
        System.out.println("1. Consistency check");
        System.out.println("2. E axiom-filter contradiction search");
        System.out.println("3. SUMO update");

        String typeChoice = prompt("Selection");
        switch (typeChoice) {
            case "1":
                put(options, "type", "consistency-check");
                put(options, "kb", promptDefault("Knowledge base", "SUMO"));
                break;
            case "2":
                put(options, "type", "eaxfilter-contradiction");
                put(options, "kb", promptDefault("Knowledge base", "SUMO"));
                put(options, "cnf-timeout", promptDefault("CNF timeout in seconds", "300"));
                put(options, "filter-timeout", promptDefault("Filter timeout in seconds", "300"));
                put(options, "vampire-timeout", promptDefault("Vampire timeout in seconds", "30"));
                break;
            case "3":
                put(options, "type", "sumo-update");
                break;
            default:
                throw new IllegalArgumentException("Invalid job type");
        }

        System.out.println("Execution mode:");
        System.out.println("1. Tomcat");
        System.out.println("2. CRON");
        String mode = prompt("Selection");
        if ("1".equals(mode)) put(options, "mode", "tomcat");
        else if ("2".equals(mode)) put(options, "mode", "cron");
        else throw new IllegalArgumentException("Invalid execution mode");

        addScheduleOptions(options);

        if (!confirm("Enable this job?", true)) put(options, "disabled", "");
        addJobFromCommandLine(scheduler, options);
    }

    private static void addScheduleOptions(Map<String,List<String>> options) {
        System.out.println("Frequency:");
        System.out.println("1. Once");
        System.out.println("2. Daily");
        System.out.println("3. Weekly");
        System.out.println("4. Monthly");

        String frequency = prompt("Selection");
        switch (frequency) {
            case "1":
                put(options, "frequency", "once");
                put(options, "date", requiredPrompt("Date (YYYY-MM-DD)"));
                break;
            case "2":
                put(options, "frequency", "daily");
                break;
            case "3":
                put(options, "frequency", "weekly");
                put(options, "day", requiredPrompt("Day of week"));
                break;
            case "4":
                put(options, "frequency", "monthly");
                put(options, "day", requiredPrompt("Day of month (1-31)"));
                break;
            default:
                throw new IllegalArgumentException("Invalid frequency");
        }

        put(options, "time", requiredPrompt("Time (HH:mm, 24-hour format)"));
    }

    private static void editJobInteractively(JobScheduler scheduler) {
        String id = selectJob(scheduler);
        Job job = scheduler.getJob(id);
        boolean enabled = confirm("Enabled?", job.isEnabled());

        System.out.println("Execution mode:");
        System.out.println("1. Tomcat");
        System.out.println("2. CRON");
        String defaultMode = job.getExecutionMode() == Job.ExecutionMode.TOMCAT ? "1" : "2";
        String modeChoice = promptDefault("Selection", defaultMode);
        Job.ExecutionMode mode;

        if ("1".equals(modeChoice)) mode = Job.ExecutionMode.TOMCAT;
        else if ("2".equals(modeChoice)) mode = Job.ExecutionMode.CRON;
        else throw new IllegalArgumentException("Invalid execution mode");

        Schedule schedule = job.getSchedule();
        if (confirm("Change the schedule?", false)) {
            Map<String,List<String>> options = new java.util.HashMap<>();
            addScheduleOptions(options);
            schedule = parseSchedule(options);
        }

        validateModeAndSchedule(mode, schedule);
        scheduler.updateJob(id, enabled, mode, schedule);
        System.out.println("Updated job: " + id);
    }

    private static String selectJob(JobScheduler scheduler) {
        if (scheduler.getJobs().isEmpty()) throw new IllegalStateException("No jobs are configured");
        listJobs(scheduler);
        String id = requiredPrompt("Job ID");
        scheduler.getJob(id);
        return id;
    }

    private static void deleteJobInteractively(JobScheduler scheduler) {
        String id = selectJob(scheduler);
        if (!confirm("Delete job '" + id + "'?", false)) {
            System.out.println("Deletion cancelled.");
            return;
        }

        scheduler.removeJob(id);
        System.out.println("Deleted job: " + id);
    }

    private static String prompt(String label) {
        System.out.print(label + ": ");
        return INPUT.nextLine().trim();
    }

    private static String requiredPrompt(String label) {
        while (true) {
            String value = prompt(label);
            if (!value.isBlank()) return value;
            System.out.println("A value is required.");
        }
    }

    private static String promptDefault(String label, String defaultValue) {
        System.out.print(label + " [" + defaultValue + "]: ");
        String value = INPUT.nextLine().trim();
        return value.isBlank() ? defaultValue : value;
    }

    private static boolean confirm(String label, boolean defaultValue) {
        String suffix = defaultValue ? " [Y/n]: " : " [y/N]: ";
        System.out.print(label + suffix);
        String value = INPUT.nextLine().trim();

        if (value.isBlank()) return defaultValue;
        return "y".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }

    private static void put(Map<String,List<String>> options, String key, String value) {
        options.put(key, value.isEmpty() ? Collections.emptyList() : List.of(value));
    }

    /******************************************************************
     */
    public static void showHelp() {
        
        System.out.println("JobScheduler");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  --list");
        System.out.println("  --add --id <id> --type <type> --mode <tomcat|cron>");
        System.out.println("        --frequency <once|daily|weekly|monthly> --time <HH:mm>");
        System.out.println("        [--date <YYYY-MM-DD>] [--day <MONDAY|1-31>] [--kb <name>]");
        System.out.println("        [--cnf-timeout <sec>] [--filter-timeout <sec>]");
        System.out.println("        [--vampire-timeout <sec>] [--disabled]");
        System.out.println("  --edit <id> [--mode <tomcat|cron>] [--enabled|--disabled]");
        System.out.println("        [--frequency <frequency> --time <HH:mm> ...]");
        System.out.println("  --remove <id>");
        System.out.println("  --enable <id>");
        System.out.println("  --disable <id>");
        System.out.println("  --run <id>");
        System.out.println("  --sync-cron");
        System.out.println();
        System.out.println("Job types:");
        System.out.println("  consistency-check");
        System.out.println("  eaxfilter-contradiction");
        System.out.println("  sumo-update");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  List all jobs:");
        System.out.println("    JobScheduler --list");
        System.out.println();
        System.out.println("  Run a consistency check once in Tomcat:");
        System.out.println("    JobScheduler --add --id check-once --type consistency-check --kb SUMO");
        System.out.println("      --mode tomcat --frequency once --date 2026-07-20 --time 02:00");
        System.out.println();
        System.out.println("  Run a consistency check every Sunday in Tomcat:");
        System.out.println("    JobScheduler --add --id weekly-check --type consistency-check --kb SUMO");
        System.out.println("      --mode tomcat --frequency weekly --day SUNDAY --time 02:00");
        System.out.println();
        System.out.println("  Run the E axiom-filter job daily through CRON:");
        System.out.println("    JobScheduler --add --id daily-eaxfilter --type eaxfilter-contradiction --kb SUMO");
        System.out.println("      --mode cron --frequency daily --time 03:30");
        System.out.println("      --cnf-timeout 300 --filter-timeout 300 --vampire-timeout 30");
        System.out.println();
        System.out.println("  Update SUMO on the first day of each month:");
        System.out.println("    JobScheduler --add --id monthly-sumo-update --type sumo-update");
        System.out.println("      --mode cron --frequency monthly --day 1 --time 01:00");
        System.out.println();
        System.out.println("  Change a job's time and execution mode:");
        System.out.println("    JobScheduler --edit weekly-check --mode cron");
        System.out.println("      --frequency weekly --day MONDAY --time 03:00");
        System.out.println();
        System.out.println("  Enable, disable, run, or remove a job:");
        System.out.println("    JobScheduler --enable weekly-check");
        System.out.println("    JobScheduler --disable weekly-check");
        System.out.println("    JobScheduler --run weekly-check");
        System.out.println("    JobScheduler --remove weekly-check");
        System.out.println();
        System.out.println("  Rebuild the managed CRON entries:");
        System.out.println("    JobScheduler --sync-cron");
        System.out.println();
        System.out.println("Full Java invocation:");
        System.out.println("  java -cp \"$SIGMA_CP\" com.articulate.sigma.jobscheduler.JobScheduler <options>");
    }

    /******************************************************************
     */
    public static void main(String[] args) {
        JobScheduler scheduler = null;

        try {
            scheduler = new JobScheduler();

            if (args == null || args.length == 0) {
                interactiveMenu(scheduler);
                return;
            }

            Map<String,List<String>> options = CLIMapParser.parse(args);

            if (options.containsKey("h") || options.containsKey("help")) showHelp();
            else if (options.containsKey("menu") || options.containsKey("interactive")) interactiveMenu(scheduler);
            else executeCommand(scheduler, options);
        }
        catch (Exception exception) {
            System.err.println("JobScheduler: " + exception.getMessage());
            System.exit(1);
        }
        finally {
            if (scheduler != null) scheduler.shutdown();
        }
    }
}