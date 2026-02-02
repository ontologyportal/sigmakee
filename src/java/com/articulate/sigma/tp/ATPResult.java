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

import tptp_parser.TPTPFormula;

import java.util.*;

/**
 * Generic result structure for ATP (Automated Theorem Prover) runs.
 * Captures execution metadata, SZS status, output, and proof data.
 *
 * This class provides a unified interface for results from different provers:
 * - Vampire
 * - EProver
 * - LEO-III
 */
public class ATPResult {

    // === Execution Context ===
    private String engineName;           // "Vampire", "EProver", "LEO-III"
    private String engineMode;           // "CASC", "AVATAR", "HOL", etc.
    private String inputLanguage;        // "FOF", "TFF", "THF"
    private String inputSource;          // "custom" or filename
    private long timeoutMs;              // Configured timeout
    private List<String> commandLine;    // Actual command executed

    // === Execution Outcome ===
    private int exitCode = -1;           // Process exit code (-1 if not run)
    private long elapsedTimeMs = 0;      // Wall-clock time
    private boolean timedOut = false;    // Explicit timeout flag
    private String terminationSignal;    // "SIGKILL", "SIGSEGV", etc. or null

    // === Raw Output ===
    private List<String> stdout = new ArrayList<>();
    private List<String> stderr = new ArrayList<>();

    // === SZS Status ===
    private SZSStatus szsStatus = SZSStatus.NOT_RUN;
    private String szsStatusRaw;         // Raw status string from output
    private String szsDiagnostics;       // Details after colon in status line
    private String szsOutputType;        // "Proof", "Refutation", "Model", etc.

    // === Proof Data (populated by TPTP3ProofProcessor) ===
    private List<TPTPFormula> proofSteps;
    private Map<String, String> answerBindings;
    private boolean inconsistencyDetected;
    private List<String> parsingWarnings;

    // === Error Info ===
    private List<String> errorLines = new ArrayList<>();
    private String primaryError;
    private List<String> warnings = new ArrayList<>();

    /**
     * Default constructor
     */
    public ATPResult() {
        this.commandLine = new ArrayList<>();
    }

    // === Convenience Factory Methods ===

    /**
     * Create an ATPResult indicating the prover was not run
     */
    public static ATPResult notRun(String engineName, String reason) {
        ATPResult result = new ATPResult();
        result.setEngineName(engineName);
        result.setSzsStatus(SZSStatus.NOT_RUN);
        result.setPrimaryError(reason);
        return result;
    }

    /**
     * Create an ATPResult for a crashed prover
     */
    public static ATPResult crashed(String engineName, int exitCode, List<String> stdout, List<String> stderr) {
        ATPResult result = new ATPResult();
        result.setEngineName(engineName);
        result.setExitCode(exitCode);
        result.setStdout(stdout);
        result.setStderr(stderr);
        result.setSzsStatus(SZSStatus.CRASHED);

        // Extract signal info if available
        if (exitCode > 128 && exitCode < 160) {
            int signal = exitCode - 128;
            result.setTerminationSignal("SIG" + signal);
        }

        // Extract error info
        result.setErrorLines(SZSExtractor.extractErrorLines(stdout, stderr));
        result.setPrimaryError(SZSExtractor.getPrimaryError(stdout, stderr));

        return result;
    }

    /**
     * Create an ATPResult for a timeout
     */
    public static ATPResult timeout(String engineName, long timeoutMs, long elapsedMs,
                                    List<String> stdout, List<String> stderr) {
        ATPResult result = new ATPResult();
        result.setEngineName(engineName);
        result.setTimeoutMs(timeoutMs);
        result.setElapsedTimeMs(elapsedMs);
        result.setTimedOut(true);
        result.setStdout(stdout);
        result.setStderr(stderr);
        result.setSzsStatus(SZSStatus.TIMEOUT);
        return result;
    }

    // === Getters and Setters ===

    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public String getEngineMode() {
        return engineMode;
    }

    public void setEngineMode(String engineMode) {
        this.engineMode = engineMode;
    }

    public String getInputLanguage() {
        return inputLanguage;
    }

    public void setInputLanguage(String inputLanguage) {
        this.inputLanguage = inputLanguage;
    }

    public String getInputSource() {
        return inputSource;
    }

    public void setInputSource(String inputSource) {
        this.inputSource = inputSource;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public List<String> getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(List<String> commandLine) {
        this.commandLine = commandLine != null ? commandLine : new ArrayList<>();
    }

    public void setCommandLine(String[] commandLine) {
        this.commandLine = commandLine != null ? Arrays.asList(commandLine) : new ArrayList<>();
    }

    public String getCommandLineString() {
        return commandLine != null ? String.join(" ", commandLine) : "";
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public long getElapsedTimeMs() {
        return elapsedTimeMs;
    }

    public void setElapsedTimeMs(long elapsedTimeMs) {
        this.elapsedTimeMs = elapsedTimeMs;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    public String getTerminationSignal() {
        return terminationSignal;
    }

    public void setTerminationSignal(String terminationSignal) {
        this.terminationSignal = terminationSignal;
    }

    public List<String> getStdout() {
        return stdout;
    }

    public void setStdout(List<String> stdout) {
        this.stdout = stdout != null ? stdout : new ArrayList<>();
    }

    public List<String> getStderr() {
        return stderr;
    }

    public void setStderr(List<String> stderr) {
        this.stderr = stderr != null ? stderr : new ArrayList<>();
    }

    public SZSStatus getSzsStatus() {
        return szsStatus;
    }

    public void setSzsStatus(SZSStatus szsStatus) {
        this.szsStatus = szsStatus != null ? szsStatus : SZSStatus.UNKNOWN;
    }

    public String getSzsStatusRaw() {
        return szsStatusRaw;
    }

    public void setSzsStatusRaw(String szsStatusRaw) {
        this.szsStatusRaw = szsStatusRaw;
    }

    public String getSzsDiagnostics() {
        return szsDiagnostics;
    }

    public void setSzsDiagnostics(String szsDiagnostics) {
        this.szsDiagnostics = szsDiagnostics;
    }

    public String getSzsOutputType() {
        return szsOutputType;
    }

    public void setSzsOutputType(String szsOutputType) {
        this.szsOutputType = szsOutputType;
    }

    public List<TPTPFormula> getProofSteps() {
        return proofSteps;
    }

    public void setProofSteps(List<TPTPFormula> proofSteps) {
        this.proofSteps = proofSteps;
    }

    public Map<String, String> getAnswerBindings() {
        return answerBindings;
    }

    public void setAnswerBindings(Map<String, String> answerBindings) {
        this.answerBindings = answerBindings;
    }

    public boolean isInconsistencyDetected() {
        return inconsistencyDetected;
    }

    public void setInconsistencyDetected(boolean inconsistencyDetected) {
        this.inconsistencyDetected = inconsistencyDetected;
    }

    public List<String> getParsingWarnings() {
        return parsingWarnings;
    }

    public void setParsingWarnings(List<String> parsingWarnings) {
        this.parsingWarnings = parsingWarnings;
    }

    public List<String> getErrorLines() {
        return errorLines;
    }

    public void setErrorLines(List<String> errorLines) {
        this.errorLines = errorLines != null ? errorLines : new ArrayList<>();
    }

    public String getPrimaryError() {
        return primaryError;
    }

    public void setPrimaryError(String primaryError) {
        this.primaryError = primaryError;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    // === Convenience Methods ===

    /**
     * @return true if the prover found a proof or determined the status successfully
     */
    public boolean isSuccess() {
        return szsStatus != null && szsStatus.isSuccess();
    }

    /**
     * @return true if proof steps are available
     */
    public boolean hasProof() {
        return proofSteps != null && !proofSteps.isEmpty();
    }

    /**
     * @return true if answer bindings are available
     */
    public boolean hasAnswers() {
        return answerBindings != null && !answerBindings.isEmpty();
    }

    /**
     * @return true if there were errors (exit code != 0, error status, or error lines)
     */
    public boolean hasErrors() {
        if (exitCode != 0 && exitCode != -1) return true;
        if (szsStatus != null && szsStatus.isError()) return true;
        return !errorLines.isEmpty() || (stderr != null && !stderr.isEmpty());
    }

    /**
     * @return true if there are warnings
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    /**
     * @return true if stderr has content
     */
    public boolean hasStderr() {
        return stderr != null && !stderr.isEmpty();
    }

    /**
     * @return true if stdout has content
     */
    public boolean hasStdout() {
        return stdout != null && !stdout.isEmpty();
    }

    /**
     * Get a human-readable summary of the result
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();

        // Engine and status
        sb.append(engineName != null ? engineName : "Prover");
        if (engineMode != null) {
            sb.append(" (").append(engineMode).append(")");
        }
        sb.append(": ");
        sb.append(szsStatus != null ? szsStatus.getTptpName() : "Unknown");

        // Timing
        if (elapsedTimeMs > 0) {
            sb.append(" in ").append(elapsedTimeMs).append("ms");
        }
        if (timedOut) {
            sb.append(" (timed out)");
        }

        // Proof info
        if (hasProof()) {
            sb.append(", ").append(proofSteps.size()).append(" proof steps");
        }
        if (hasAnswers()) {
            sb.append(", ").append(answerBindings.size()).append(" answers");
        }

        return sb.toString();
    }

    /**
     * Get CSS class for UI styling based on status
     */
    public String getCssClass() {
        if (szsStatus == null) {
            return "szs-unknown";
        }
        return szsStatus.getCssClass();
    }

    /**
     * Get CSS class for the status badge
     */
    public String getStatusBadgeClass() {
        return getCssClass();
    }

    /**
     * Extract and populate SZS information from stdout
     */
    public void extractSzsFromOutput() {
        if (stdout == null || stdout.isEmpty()) {
            return;
        }

        // Extract SZS status
        SZSStatus extracted = SZSExtractor.extractStatus(stdout);
        if (extracted != null) {
            this.szsStatus = extracted;
        }

        // Extract raw status line
        this.szsStatusRaw = SZSExtractor.extractStatusLine(stdout);

        // Extract diagnostics
        this.szsDiagnostics = SZSExtractor.extractDiagnostics(stdout);

        // Extract output type
        this.szsOutputType = SZSExtractor.extractOutputType(stdout);

        // Extract error lines
        this.errorLines = SZSExtractor.extractErrorLines(stdout, stderr);

        // Get primary error
        this.primaryError = SZSExtractor.getPrimaryError(stdout, stderr);

        // Extract warnings
        this.warnings = SZSExtractor.extractWarnings(stdout);
    }

    /**
     * Finalize the result after prover execution.
     * Extracts SZS info and sets defaults based on execution outcome.
     */
    public void finalize(int exitCode, long elapsedMs, boolean timedOut) {
        this.exitCode = exitCode;
        this.elapsedTimeMs = elapsedMs;
        this.timedOut = timedOut;

        extractSzsFromOutput();

        if (szsStatus == null || szsStatus == SZSStatus.NOT_RUN) {
            szsStatus = SZSStatus.fromExitCode(exitCode, timedOut);
        }

        // ✅ If Vampire produced a solved result, it wins over any "Time limit reached!" noise
        if (szsStatus.isSuccess()) {          // implement: Theorem/Unsat/Sat/CounterSatisfiable/…
            this.timedOut = false;
            return;
        }

        // Heuristics only for non-solved/unknown/gave-up runs
        if (!this.timedOut && SZSExtractor.indicatesTimeout(stdout)) {
            this.timedOut = true;
            szsStatus = SZSStatus.TIMEOUT;
        }

        if (SZSExtractor.indicatesResourceOut(stdout)) {
            szsStatus = SZSStatus.RESOURCE_OUT;
        }
    }

    @Override
    public String toString() {
        return getSummary();
    }

    // === Builder Pattern ===

    /**
     * Builder for constructing ATPResult
     */
    public static class Builder {
        private final ATPResult result = new ATPResult();

        public Builder engineName(String name) {
            result.setEngineName(name);
            return this;
        }

        public Builder engineMode(String mode) {
            result.setEngineMode(mode);
            return this;
        }

        public Builder inputLanguage(String lang) {
            result.setInputLanguage(lang);
            return this;
        }

        public Builder inputSource(String source) {
            result.setInputSource(source);
            return this;
        }

        public Builder timeoutMs(long ms) {
            result.setTimeoutMs(ms);
            return this;
        }

        public Builder commandLine(List<String> cmd) {
            result.setCommandLine(cmd);
            return this;
        }

        public Builder commandLine(String[] cmd) {
            result.setCommandLine(cmd);
            return this;
        }

        public Builder exitCode(int code) {
            result.setExitCode(code);
            return this;
        }

        public Builder elapsedTimeMs(long ms) {
            result.setElapsedTimeMs(ms);
            return this;
        }

        public Builder timedOut(boolean timedOut) {
            result.setTimedOut(timedOut);
            return this;
        }

        public Builder stdout(List<String> stdout) {
            result.setStdout(stdout);
            return this;
        }

        public Builder stderr(List<String> stderr) {
            result.setStderr(stderr);
            return this;
        }

        public Builder szsStatus(SZSStatus status) {
            result.setSzsStatus(status);
            return this;
        }

        public ATPResult build() {
            return result;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
