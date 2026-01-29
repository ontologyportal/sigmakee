/** This code is copyright Articulate Software (c) 2024.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also sigmakee.sourceforge.net
*/

package com.articulate.sigma.tp;

import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when an ATP process crashes or is killed by a signal.
 * This typically indicates either:
 * - A malformed input formula that caused a segmentation fault
 * - Memory exhaustion leading to OOM killer
 * - External termination of the process
 */
public class ProverCrashedException extends ATPException {

    // Signal constants for common crashes (Unix)
    public static final int SIGHUP = 1;
    public static final int SIGINT = 2;
    public static final int SIGQUIT = 3;
    public static final int SIGABRT = 6;
    public static final int SIGKILL = 9;
    public static final int SIGSEGV = 11;
    public static final int SIGTERM = 15;
    public static final int SIGXCPU = 24;  // CPU time limit exceeded

    private final int exitCode;            // Non-zero exit code
    private final String signalName;       // "SIGSEGV", "SIGABRT", "SIGKILL", etc. or null
    private final int signalNumber;        // Signal number (e.g., 11 for SIGSEGV)

    /**
     * Create a new ProverCrashedException
     *
     * @param engineName The name of the prover
     * @param exitCode The process exit code
     * @param stdout The captured stdout lines (may be partial)
     * @param stderr The captured stderr lines (may be partial)
     */
    public ProverCrashedException(String engineName, int exitCode,
                                   List<String> stdout, List<String> stderr, ATPResult result) {
        super(buildMessage(engineName, exitCode), engineName, result, stdout, stderr);
        this.exitCode = exitCode;
        this.signalNumber = extractSignalNumber(exitCode);
        this.signalName = signalNumberToName(this.signalNumber);
    }

    /**
     * Create a new ProverCrashedException with just exit code
     */
    public ProverCrashedException(String engineName, int exitCode) {
        this(engineName, exitCode, null, null, null);
    }

    private static String buildMessage(String engineName, int exitCode) {
        int sig = extractSignalNumber(exitCode);
        if (sig > 0) {
            return engineName + " crashed with signal " + signalNumberToName(sig) +
                    " (exit code " + exitCode + ")";
        }
        return engineName + " exited with error code " + exitCode;
    }

    /**
     * On Unix, exit code = 128 + signal number when killed by signal
     */
    private static int extractSignalNumber(int exitCode) {
        if (exitCode > 128 && exitCode < 160) {
            return exitCode - 128;
        }
        return -1;
    }

    /**
     * Convert a Unix signal number to its name
     */
    private static String signalNumberToName(int sig) {
        switch (sig) {
            case SIGHUP:
                return "SIGHUP";
            case SIGINT:
                return "SIGINT";
            case SIGQUIT:
                return "SIGQUIT";
            case SIGABRT:
                return "SIGABRT";
            case SIGKILL:
                return "SIGKILL";
            case SIGSEGV:
                return "SIGSEGV";
            case SIGTERM:
                return "SIGTERM";
            case SIGXCPU:
                return "SIGXCPU";
            default:
                return sig > 0 ? "SIG" + sig : null;
        }
    }

    /**
     * @return The process exit code
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * @return The signal name if killed by signal, null otherwise
     */
    public String getSignalName() {
        return signalName;
    }

    /**
     * @return The signal number if killed by signal, -1 otherwise
     */
    public int getSignalNumber() {
        return signalNumber;
    }

    /**
     * @return true if the process was killed by a signal
     */
    public boolean wasKilledBySignal() {
        return signalNumber > 0;
    }

    /**
     * @return true if this was a segmentation fault
     */
    public boolean wasSegmentationFault() {
        return signalNumber == SIGSEGV;
    }

    /**
     * @return true if this was an abort (often assertion failure)
     */
    public boolean wasAborted() {
        return signalNumber == SIGABRT;
    }

    /**
     * @return true if the process was forcibly killed (SIGKILL/SIGTERM)
     */
    public boolean wasForciblyStopped() {
        return signalNumber == SIGKILL || signalNumber == SIGTERM;
    }

    /**
     * @return true if CPU time limit was exceeded
     */
    public boolean wasCpuLimitExceeded() {
        return signalNumber == SIGXCPU;
    }


    @Override
    public String getSuggestion() {
        if (signalNumber == SIGSEGV) {
            return "SIGSEGV (segmentation fault) often indicates:\n" +
                    "- A malformed formula that the prover couldn't handle\n" +
                    "- A bug in the prover itself\n" +
                    "- Incompatible input format\n\n" +
                    "Try checking the input formula syntax and simplifying the query.";
        } else if (signalNumber == SIGABRT) {
            return "SIGABRT (abort) typically indicates an internal assertion failure.\n" +
                    "This may be a bug in the prover. Try simplifying the input.";
        } else if (signalNumber == SIGKILL || signalNumber == SIGTERM) {
            return "The process was killed, possibly due to:\n" +
                    "- Memory limits (OOM killer)\n" +
                    "- External termination\n" +
                    "- System resource constraints\n\n" +
                    "Try increasing memory limits or simplifying the query.";
        } else if (signalNumber == SIGXCPU) {
            return "CPU time limit exceeded. The prover ran out of allocated CPU time.\n" +
                    "Try increasing the timeout or simplifying the query.";
        } else if (exitCode == 1) {
            return "Exit code 1 often indicates a general error.\n" +
                    "Check the prover output for specific error messages.";
        }
        return "Check the prover output for error details.";
    }

    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage());

        if (wasKilledBySignal()) {
            sb.append("\nSignal: ").append(signalName).append(" (").append(signalNumber).append(")");
        }

        if (hasStderr()) {
            sb.append("\n\nStderr output:");
            int linesToShow = Math.min(15, getStderr().size());
            for (int i = 0; i < linesToShow; i++) {
                sb.append("\n  ").append(getStderr().get(i));
            }
            if (getStderr().size() > linesToShow) {
                sb.append("\n  ... (").append(getStderr().size() - linesToShow).append(" more lines)");
            }
        }

        sb.append("\n\n").append(getSuggestion());
        return sb.toString();
    }
}
