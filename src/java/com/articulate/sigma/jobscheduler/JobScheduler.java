package com.articulate.sigma.jobscheduler;

import com.articulate.sigma.utils.LoggingUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.nio.file.Path;

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

    /******************************************************************
     */
    private Job requireJob(String jobId) {

        Job job = findJob(jobId);
        if (job == null) throw new IllegalArgumentException("Unknown job ID: " + jobId);
        return job;
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

    /******************************************************************
     */
    public static void main(String[] args) throws InterruptedException {

        JobScheduler scheduler = new JobScheduler();
        LocalDateTime firstRun = LocalDateTime.now().plusSeconds(3);
        Job testJob = new ConsistencyCheckJob("test-consistency-check", "SUMO", Schedule.onceAt(firstRun));
        scheduler.addJob(testJob);
        scheduler.saveJobs();
        System.out.println("Waiting for the test job to execute...");
        Thread.sleep(6_000);
        scheduler.shutdown();
        System.out.println("Scheduler stopped.");
    }
}