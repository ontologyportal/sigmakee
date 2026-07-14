package com.articulate.sigma.jobscheduler;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

public class Schedule {

    public enum Frequency {
        ONCE,
        DAILY,
        WEEKLY,
        MONTHLY
    }

    private Frequency frequency;
    private LocalTime runTime;
    private LocalDate runDate;
    private DayOfWeek dayOfWeek;
    private Integer dayOfMonth;

    private Schedule() {
    }

    public static Schedule onceAt(LocalDateTime dateTime) {

        Schedule schedule = new Schedule();
        schedule.frequency = Frequency.ONCE;
        schedule.runDate = dateTime.toLocalDate();
        schedule.runTime = dateTime.toLocalTime();
        return schedule;
    }

    public static Schedule dailyAt(LocalTime runTime) {

        Schedule schedule = new Schedule();
        schedule.frequency = Frequency.DAILY;
        schedule.runTime = runTime;
        return schedule;
    }   

    public static Schedule weeklyAt(
            DayOfWeek dayOfWeek,
            LocalTime runTime) {

        Schedule schedule = new Schedule();
        schedule.frequency = Frequency.WEEKLY;
        schedule.dayOfWeek = dayOfWeek;
        schedule.runTime = runTime;
        return schedule;
    }

    public static Schedule monthlyAt(int dayOfMonth, LocalTime runTime) {

        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new IllegalArgumentException(
                    "Day of month must be between 1 and 31");
        }
        Schedule schedule = new Schedule();
        schedule.frequency = Frequency.MONTHLY;
        schedule.dayOfMonth = dayOfMonth;
        schedule.runTime = runTime;
        return schedule;
    }

    public LocalDateTime nextRunAfter(LocalDateTime now) {

        return switch (frequency) {
            case ONCE -> nextOneTimeRun(now);
            case DAILY -> nextDailyRun(now);
            case WEEKLY -> nextWeeklyRun(now);
            case MONTHLY -> nextMonthlyRun(now);
        };
    }

    private LocalDateTime nextOneTimeRun(LocalDateTime now) {

        LocalDateTime scheduledTime =
                LocalDateTime.of(runDate, runTime);

        return scheduledTime.isAfter(now)
                ? scheduledTime
                : null;
    }

    private LocalDateTime nextDailyRun(LocalDateTime now) {

        LocalDateTime candidate =
                LocalDateTime.of(now.toLocalDate(), runTime);

        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }

        return candidate;
    }

    private LocalDateTime nextWeeklyRun(LocalDateTime now) {

        LocalDate date = now.toLocalDate()
                .with(TemporalAdjusters.nextOrSame(dayOfWeek));

        LocalDateTime candidate =
                LocalDateTime.of(date, runTime);

        if (!candidate.isAfter(now)) {
            candidate = candidate.plusWeeks(1);
        }

        return candidate;
    }

    private LocalDateTime nextMonthlyRun(LocalDateTime now) {

        YearMonth month = YearMonth.from(now);
        LocalDateTime candidate = monthlyDateTime(month);

        if (!candidate.isAfter(now)) {
            candidate = monthlyDateTime(month.plusMonths(1));
        }

        return candidate;
    }

    private LocalDateTime monthlyDateTime(YearMonth month) {

        // A job configured for the 31st runs on the final day of
        // shorter months.
        int validDay = Math.min(
                dayOfMonth,
                month.lengthOfMonth());

        return LocalDateTime.of(
                month.atDay(validDay),
                runTime);
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public LocalTime getRunTime() {
        return runTime;
    }

    public LocalDate getRunDate() {
        return runDate;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }
}