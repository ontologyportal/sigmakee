/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. See also http://github.com/ontologyportal

 Authors:
 Adam Pease
 Infosys LTD.
*/

package com.articulate.sigma;

import java.util.LinkedHashMap;
import java.util.Map;

/********************************************************************
 * Class to replace System calls to make logging easier and more consistent.
 * @author Shaun Rose
 */
public class LoggingUtils {

    /** Shared StackWalker used to identify the class and method that called the logger. */
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    /** Lock used to prevent progress bar output from interleaving across threads. */
    private static final Object PROGRESS_LOCK = new Object();
    /** Lock used to prevent normal log output from interleaving across threads. */
    private static final Object LOG_LOCK = new Object();
    
    /** Default number of spaces between log columns. */
    private static final int DEFAULT_GAP = 2;
    /** Fixed width of the log type column. */
    private static final int TYPE_WIDTH = 5;
    /** Fixed width of the caller location column. */
    private static final int LOCATION_WIDTH = 60;
    /** Default width of the entire text section before the progress bar. */
    private static final int DEFAULT_BEFORE_WIDTH = 80;
    /** Default number of characters used for the message column. */
    private static final int MESSAGE_WIDTH = 35;
    /** Default number of characters used to draw the progress bar. */
    private static final int DEFAULT_BAR_WIDTH = 10;

    /********************************************************************
     * Prints a progress bar for a given task in table format with the name of the class and type of log.
     * @param type Log type, usually INFO or ERROR.
     * @param message Message to be printed inbetween the class.function and progress bar.
     * @param current The current step in the progress, between 0 and total
     * @param total Maximum number of steps needed to show 100% complete. 
     */
    public static void printProgressBar(String type, String message, int current, int total) {

        printProgressBar(type, message, current, total, "");
    }

    /********************************************************************
     * Basic tabular log with format 'INFO  Class.Function()  message'
     * @param message Message to be appended to the log.
     */
    public static void log(String message) {

        LogSite caller = getCaller();
        log("INFO", caller.className, caller.functionName, message);
    }

    /********************************************************************
     * Basic tabular log with format 'TYPE  Class.Function()  message'
     * @param type Log type, usually INFO or ERROR
     * @param message Message to be appended to the log.
     */
    public static void log(String type, String message) {

        LogSite caller = getCaller();
        log(type, caller.className, caller.functionName, message);
    }

    /********************************************************************
     * Basic tabular log with format 'TYPE  Class.Function()  message'
     * @param type Log type, usually INFO or ERROR
     * @param className name of the caller class
     * @param functionName name of the caller function
     * @param message Message to be appended to the log.
     */
    public static void log(String type, String className, String functionName, String message) {

        synchronized (LOG_LOCK) {
            if (type == null) type = "";
            if (className == null) className = "";
            if (functionName == null) functionName = "";
            if (message == null) message = "";
            String spacing = " ".repeat(DEFAULT_GAP);
            String location = "[" + className + "." + functionName + "()]";
            String line = padRight(type, TYPE_WIDTH) + spacing + padRight(location, LOCATION_WIDTH) + spacing + message;
            if ("ERROR".equalsIgnoreCase(type)) System.err.println(line);
            else System.out.println(line);
        }
    }

    /********************************************************************
     * Prints a tabular progress bar for a given task in table format with the name of the class and type of log.
     * @param type Log type, usually INFO or ERROR.
     * @param message Message to be printed inbetween the class.function and progress bar.
     * @param current The current step in the progress, between 0 and total
     * @param total Maximum number of steps needed to show 100% complete. 
     * @param after Message to be printed after the progress bar.
     */
    public static void printProgressBar(String type, String message, int current, int total, String after) {

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
            DEFAULT_BAR_WIDTH
        );
    }

    /********************************************************************
     * Prints a tabular progress bar for a given task in table format with the name of the class and type of log.
     * @param type Log type, usually INFO or ERROR.
     * @param className name of the caller class
     * @param functionName name of the caller function
     * @param message Message to be printed inbetween the class.function and progress bar.
     * @param current The current step in the progress, between 0 and total
     * @param total Maximum number of steps needed to show 100% complete. 
     * @param after Message to be printed after the progress bar.
     * @param beforeWidth width in characters of the message column before the progress bar
     * @param barWidth width in characters of the progress bar column
     */
    public static void printProgressBarTable(String type, String className, String functionName, String message, int current, int total, String after, int beforeWidth, int barWidth) {

        synchronized (PROGRESS_LOCK) {
            if (total <= 0) total = 1;
            if (type == null) type = "";
            if (className == null) className = "";
            if (functionName == null) functionName = "";
            if (message == null) message = "";
            if (after == null) after = "";
            float progress = Math.max(0.0f, Math.min(1.0f, (float) current / total));
            int completedBlocks = Math.round(progress * barWidth);
            int percent = Math.round(progress * 100);
            String spacing = " ".repeat(Math.max(1, DEFAULT_GAP));
            String location = "[" + className + "." + functionName + "()]";
            StringBuilder bar = new StringBuilder();
            bar.append("\r\033[2K");
            bar.append(padRight(type, TYPE_WIDTH));
            bar.append(spacing);
            bar.append(padRight(location, LOCATION_WIDTH));
            bar.append(spacing);
            bar.append(padRight(message, MESSAGE_WIDTH));
            bar.append(spacing);
            bar.append(spacing);
            bar.append("[");
            for (int i = 0; i < barWidth; i++) {
                if (i < completedBlocks) bar.append("█");
                else bar.append("░");
            }
            bar.append("]");
            bar.append(" ");
            bar.append(String.format("%3d%%", percent));
            if (!after.isEmpty()) {
                bar.append(spacing);
                bar.append(after);
            }
            System.out.print(bar);
            if (current >= total) System.out.println();
        }
    }

    /********************************************************************
     * Helper method for printing columns of a certain width
     * @param value String to be printed at the left side of this column
     * @param width Minumum width of the column.
     */
    private static String padRight(String value, int width) {

        if (value == null) value = "";
        if (width <= 0) return value;
        if (value.length() >= width) return value;
        return value + " ".repeat(width - value.length());
    }

    /********************************************************************
     * Helper class representing the caller class and function calling the log function
     */
    private static class LogSite {

        private final String className;
        private final String functionName;
        private LogSite(String className, String functionName) {
            this.className = className;
            this.functionName = functionName;
        }
    }

    /********************************************************************
     * Returns a LogSite object reprenting the class and function calling the log.
     * @return LogSite object with className and functionName
     */
    private static LogSite getCaller() {

        return STACK_WALKER.walk(frames -> 
            frames.filter(frame -> !frame.getClassName().equals(LoggingUtils.class.getName()))
            .findFirst()
            .map(frame -> new LogSite(frame.getDeclaringClass().getSimpleName(), frame.getMethodName()))
            .orElse(new LogSite("UnknownClass", "unknownMethod"))
        );
    }
}