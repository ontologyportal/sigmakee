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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base exception class for all ATP (Automated Theorem Prover) related errors.
 * Provides rich context about the execution environment and any available output.
 *
 * Extends RuntimeException so these exceptions don't need to be declared in method
 * signatures, allowing them to propagate through existing code without modification.
 *
 * Subclasses include:
 * - ExecutableNotFoundException: Prover binary not found/executable
 * - ProverCrashedException: Process killed by signal (SIGSEGV, etc.)
 * - ProverTimeoutException: Explicit timeout exception
 * - FormulaTranslationException: SUMO-to-TPTP translation failure
 */
public class ATPException extends RuntimeException {

    // === Execution Context ===
    private final String engineName;           // "Vampire", "EProver", "LEO-III"
    private final List<String> commandLine;    // Full command attempted
    private final String workingDirectory;     // Where command was run
    private final long timeoutMs;              // Configured timeout

    // === Available Output (may be partial) ===
    private final List<String> stdout;
    private final List<String> stderr;

    // Handle Partial Results
    private ATPResult result = null;


    /**
     * Simple constructor with just message and engine name
     */
    public ATPException(String message, String engineName) {
        super(message);
        this.engineName = engineName;
        this.commandLine = Collections.emptyList();
        this.workingDirectory = null;
        this.timeoutMs = 0;
        this.stdout = Collections.emptyList();
        this.stderr = Collections.emptyList();
    }

    /**
     * Simple constructor with just message and engine name
     */
    public ATPException(String message, String engineName, ATPResult result, List<String> stdout, List<String> stderr) {
        super(message);
        this.engineName = engineName;
        this.commandLine = Collections.emptyList();
        this.workingDirectory = null;
        this.timeoutMs = 0;
        this.result = result;
        this.stdout = stdout;
        this.stderr = stderr;

    }

    /**
     * Simple constructor with just message and engine name
     */
    public ATPException(String message, String engineName, List<String> stdout, List<String> stderr) {
        super(message);
        this.engineName = engineName;
        this.commandLine = Collections.emptyList();
        this.workingDirectory = null;
        this.timeoutMs = 0;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    /**
     * Constructor with message, engine name, and cause
     */
    public ATPException(String message, String engineName, Throwable cause) {
        super(message, cause);
        this.engineName = engineName;
        this.commandLine = Collections.emptyList();
        this.workingDirectory = null;
        this.timeoutMs = 0;
        this.stdout = Collections.emptyList();
        this.stderr = Collections.emptyList();
    }

    /**
     * Private constructor used by Builder
     */
    private ATPException(Builder builder) {
        super(builder.message, builder.cause);
        this.engineName = builder.engineName;
        this.commandLine = builder.commandLine != null
                ? Collections.unmodifiableList(new ArrayList<>(builder.commandLine))
                : Collections.emptyList();
        this.workingDirectory = builder.workingDirectory;
        this.timeoutMs = builder.timeoutMs;
        this.stdout = builder.stdout != null
                ? Collections.unmodifiableList(new ArrayList<>(builder.stdout))
                : Collections.emptyList();
        this.stderr = builder.stderr != null
                ? Collections.unmodifiableList(new ArrayList<>(builder.stderr))
                : Collections.emptyList();
        this.result = builder.result;
    }



    // === Getters ===

    public String getEngineName() {
        return engineName;
    }

    public List<String> getCommandLine() {
        return commandLine;
    }

    /**
     * @return The command line as a single string
     */
    public String getCommandLineString() {
        if (commandLine == null || commandLine.isEmpty()) {
            return "";
        }
        return String.join(" ", commandLine);
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public List<String> getStdout() {
        return stdout;
    }

    public List<String> getStderr() {
        return stderr;
    }

    /**
     * @return true if there is stderr output available
     */
    public boolean hasStderr() {
        return stderr != null && !stderr.isEmpty();
    }

    /**
     * @return true if there is stdout output available
     */
    public boolean hasStdout() {
        return stdout != null && !stdout.isEmpty();
    }

    /**
     * Get a suggestion for how to resolve this error.
     * Subclasses should override with more specific suggestions.
     *
     * @return Suggestion text for the user
     */
    public String getSuggestion() {
        return "Check the prover configuration and input.";
    }

    /**
     * Generate a detailed, human-readable error message including context.
     *
     * @return Detailed message suitable for display in UI
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();

        // Primary message
        sb.append(getMessage());

        // Engine name
        if (engineName != null && !engineName.isEmpty()) {
            sb.append("\nEngine: ").append(engineName);
        }

        // Command line
        if (!commandLine.isEmpty()) {
            sb.append("\nCommand: ").append(getCommandLineString());
        }

        // Working directory
        if (workingDirectory != null && !workingDirectory.isEmpty()) {
            sb.append("\nWorking directory: ").append(workingDirectory);
        }

        // Timeout
        if (timeoutMs > 0) {
            sb.append("\nTimeout: ").append(timeoutMs).append("ms");
        }

        // Stderr (often contains error details)
        if (hasStderr()) {
            sb.append("\nStderr (").append(stderr.size()).append(" lines):");
            int linesToShow = Math.min(10, stderr.size());
            for (int i = 0; i < linesToShow; i++) {
                sb.append("\n  ").append(stderr.get(i));
            }
            if (stderr.size() > linesToShow) {
                sb.append("\n  ... (").append(stderr.size() - linesToShow).append(" more lines)");
            }
        }

        // Suggestion
        String suggestion = getSuggestion();
        if (suggestion != null && !suggestion.isEmpty()) {
            sb.append("\n\nSuggestion: ").append(suggestion);
        }

        return sb.toString();
    }

    /**
     * Get stderr as a single string
     */
    public String getStderrString() {
        if (stderr == null || stderr.isEmpty()) {
            return "";
        }
        return String.join("\n", stderr);
    }

    /**
     * Get stdout as a single string
     */
    public String getStdoutString() {
        if (stdout == null || stdout.isEmpty()) {
            return "";
        }
        return String.join("\n", stdout);
    }

    public ATPResult getResult() {
        return result;
    }

    // === Setters ===

    public void setResult(ATPResult result) {
        this.result = result;
    }

    // === Builder Pattern ===

    /**
     * Builder for constructing ATPException with full context
     */
    public static class Builder {
        private String message;
        private Throwable cause;
        private String engineName;
        private List<String> commandLine;
        private String workingDirectory;
        private long timeoutMs;
        private List<String> stdout;
        private List<String> stderr;
        private ATPResult result;

        public Builder() {
        }

        public Builder message(String msg) {
            this.message = msg;
            return this;
        }

        public Builder cause(Throwable t) {
            this.cause = t;
            return this;
        }

        public Builder engineName(String name) {
            this.engineName = name;
            return this;
        }

        public Builder commandLine(List<String> cmd) {
            this.commandLine = cmd;
            return this;
        }

        public Builder commandLine(String[] cmd) {
            if (cmd != null) {
                this.commandLine = new ArrayList<>();
                Collections.addAll(this.commandLine, cmd);
            }
            return this;
        }

        public Builder workingDirectory(String dir) {
            this.workingDirectory = dir;
            return this;
        }

        public Builder timeoutMs(long ms) {
            this.timeoutMs = ms;
            return this;
        }

        public Builder stdout(List<String> out) {
            this.stdout = out;
            return this;
        }

        public Builder stderr(List<String> err) {
            this.stderr = err;
            return this;
        }

        public Builder result(ATPResult result) {
            this.result = result;
            return this;
        }


        public ATPException build() {
            return new ATPException(this);
        }
    }

    /**
     * Create a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
