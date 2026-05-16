package com.articulate.sigma;

import java.util.LinkedHashMap;
import java.util.Map;

public class LoggingUtils {

    private static final Object PROGRESS_LOCK = new Object();

    private static final StackWalker STACK_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private static final int DEFAULT_BEFORE_WIDTH = 80;
    private static final int DEFAULT_BAR_WIDTH = 10;
    private static final int DEFAULT_GAP = 2;
    private static final int TYPE_WIDTH = 5;
    private static final int LOCATION_WIDTH = 30;
    private static final Object LOG_LOCK = new Object();
    private static boolean progressGroupCompleted = false;

    public static void printProgressBar(String type,
                                        String message,
                                        int current,
                                        int total) {

        printProgressBar(type, message, current, total, "");
    }

    public static void log(String message) {

        LogSite caller = getCaller();
        log("INFO", caller.className, caller.functionName, message);
    }


    public static void log(String type, String message) {

        LogSite caller = getCaller();
        log(type, caller.className, caller.functionName, message);
    }

    public static void log(String type, String className, String functionName, String message) {

        synchronized (LOG_LOCK) {
            if (type == null)
                type = "";
            if (className == null)
                className = "";
            if (functionName == null)
                functionName = "";
            if (message == null)
                message = "";
            String spacing = " ".repeat(DEFAULT_GAP);
            String location = "[" + className + "." + functionName + "()]";
            String line =
                    padRight(type, TYPE_WIDTH) +
                    spacing +
                    padRight(location, LOCATION_WIDTH) +
                    spacing +
                    message;
            if ("ERROR".equalsIgnoreCase(type)) System.err.println(line);
            else System.out.println(line);
        }
    }

    public static void printProgressBar(String type,
                                        String message,
                                        int current,
                                        int total,
                                        String after) {

        LogSite caller = getCaller();

        printProgressBarTable(
                type,
                caller.className,
                caller.functionName,
                message,
                current,
                total,
                after,
                DEFAULT_BEFORE_WIDTH,
                DEFAULT_BAR_WIDTH,
                DEFAULT_GAP
        );
    }

    public static void printProgressBarTable(String type,
                                             String className,
                                             String functionName,
                                             String message,
                                             int current,
                                             int total,
                                             String after,
                                             int beforeWidth,
                                             int barWidth,
                                             int gap) {

        synchronized (PROGRESS_LOCK) {
            if (total <= 0)
                total = 1;

            if (type == null)
                type = "";

            if (className == null)
                className = "";

            if (functionName == null)
                functionName = "";

            if (message == null)
                message = "";

            if (after == null)
                after = "";

            float progress = Math.max(0.0f, Math.min(1.0f, (float) current / total));
            int completedBlocks = Math.round(progress * barWidth);
            int percent = Math.round(progress * 100);

            String spacing = " ".repeat(Math.max(1, DEFAULT_GAP));
            String location = "[" + className + "." + functionName + "()]";

            String before =
                    padRight(type, TYPE_WIDTH) +
                    spacing +
                    padRight(location, LOCATION_WIDTH) +
                    spacing +
                    message;

            StringBuilder bar = new StringBuilder();

            bar.append("\r\033[2K");

            bar.append(padRight(before, beforeWidth));
            bar.append(spacing);

            bar.append("[");
            for (int i = 0; i < barWidth; i++) {
                if (i < completedBlocks)
                    bar.append("█");
                else
                    bar.append("░");
            }
            bar.append("]");

            bar.append(" ");
            bar.append(String.format("%3d%%", percent));

            if (!after.isEmpty()) {
                bar.append(spacing);
                bar.append(after);
            }

            System.out.print(bar);

            if (current >= total)
                System.out.println();
        }
    }

    private static LogSite getCaller() {

        return STACK_WALKER.walk(frames ->
                frames
                        .filter(frame ->
                                !frame.getClassName().equals(LoggingUtils.class.getName()))
                        .findFirst()
                        .map(frame -> new LogSite(
                                frame.getDeclaringClass().getSimpleName(),
                                frame.getMethodName()))
                        .orElse(new LogSite("UnknownClass", "unknownMethod"))
        );
    }

    private static String padRight(String value, int width) {

        if (value == null)
            value = "";

        if (width <= 0)
            return value;

        if (value.length() >= width)
            return value;

        return value + " ".repeat(width - value.length());
    }

    private static class LogSite {

        private final String className;
        private final String functionName;

        private LogSite(String className, String functionName) {

            this.className = className;
            this.functionName = functionName;
        }
    }

    private static final Map<String, ProgressState> PROGRESS_STATES = new LinkedHashMap<>();

    private static final int DEFAULT_COMBINED_BAR_WIDTH = 10;

    private static final long MIN_RENDER_INTERVAL_NANOS = 50_000_000L; // 50 ms

    private static long lastRenderNanos = 0L;

    private static String formatTaskProgress(ProgressState state, int barWidth) {

        int total = state.total <= 0 ? 1 : state.total;

        float progress = Math.max(0.0f, Math.min(1.0f, (float) state.current / total));
        int completedBlocks = Math.round(progress * barWidth);
        int percent = Math.round(progress * 100);

        StringBuilder result = new StringBuilder();

        result.append(padRight(state.taskName, 6));
        result.append(" [");

        for (int i = 0; i < barWidth; i++) {
            if (i < completedBlocks)
                result.append("█");
            else
                result.append("░");
        }

        result.append("] ");
        result.append(String.format("%3d%%", percent));

        if (state.done)
            result.append(" done");

        return result.toString();
    }

    private static boolean allProgressDone() {

        if (PROGRESS_STATES.isEmpty())
            return false;

        for (ProgressState state : PROGRESS_STATES.values()) {
            if (!state.done)
                return false;
        }
        return true;
    }

    private static class ProgressState {

        private final String taskName;
        private int current = 0;
        private int total = 1;
        private String after = "";
        private boolean done = false;

        private ProgressState(String taskName) {

            this.taskName = taskName;
        }
    }
}