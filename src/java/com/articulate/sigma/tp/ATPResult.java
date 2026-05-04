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

import com.articulate.sigma.security.ValidationUtils;

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

    private String engineName;           // "Vampire", "EProver", "LEO-III"
    private String engineMode;           // "CASC", "AVATAR", "HOL", etc.
    private String inputLanguage;        // "FOF", "TFF", "THF"
    private String inputSource;          // "custom" or filename
    private long timeoutMs;              // Configured timeout
    private List<String> commandLine;    // Actual command executed

    private int exitCode = -1;           // Process exit code (-1 if not run)
    private long elapsedTimeMs = 0;      // Wall-clock time
    private boolean timedOut = false;    // Explicit timeout flag
    private String terminationSignal;    // "SIGKILL", "SIGSEGV", etc. or null

    private List<String> stdout = new ArrayList<>();
    private List<String> stderr = new ArrayList<>();

    private StringBuilder qlist = null;

    private SZSStatus szsStatus = SZSStatus.NOT_RUN;
    private String szsStatusRaw;
    private String szsDiagnostics;
    private String szsOutputType; 

    private List<TPTPFormula> proofSteps;
    private Map<String, String> answerBindings;
    private boolean inconsistencyDetected;
    private List<String> parsingWarnings;

    private List<String> errorLines = new ArrayList<>();
    private String primaryError;
    private List<String> warnings = new ArrayList<>();


    /**
     * Default constructor
     */
    public ATPResult() {
        this.commandLine = new ArrayList<>();
    }

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
    public static ATPResult timeout(String engineName, long timeoutMs, long elapsedMs, List<String> stdout, List<String> stderr) {
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

    public String getEngineName() {return engineName;}

    public void setEngineName(String engineName) {this.engineName = engineName;}

    public String getEngineMode() {return engineMode;}

    public void setEngineMode(String engineMode) {this.engineMode = engineMode;}

    public String getInputLanguage() {return inputLanguage;}

    public void setInputLanguage(String inputLanguage) {this.inputLanguage = inputLanguage;}

    public String getInputSource() {return inputSource;}

    public void setInputSource(String inputSource) {this.inputSource = inputSource;}

    public long getTimeoutMs() {return timeoutMs;}

    public void setTimeoutMs(long timeoutMs) {this.timeoutMs = timeoutMs;}

    public List<String> getCommandLine() {return commandLine;}

    public void setCommandLine(List<String> commandLine) {this.commandLine = commandLine != null ? commandLine : new ArrayList<>();}

    public void setCommandLine(String[] commandLine) {this.commandLine = commandLine != null ? Arrays.asList(commandLine) : new ArrayList<>();}

    public String getCommandLineString() {return commandLine != null ? String.join(" ", commandLine) : "";}

    public int getExitCode() {return exitCode;}

    public void setExitCode(int exitCode) {this.exitCode = exitCode;}

    public long getElapsedTimeMs() {return elapsedTimeMs;}

    public void setElapsedTimeMs(long elapsedTimeMs) {this.elapsedTimeMs = elapsedTimeMs;}

    public boolean isTimedOut() {return timedOut;}

    public void setTimedOut(boolean timedOut) {this.timedOut = timedOut;}

    public String getTerminationSignal() {return terminationSignal;}

    public void setTerminationSignal(String terminationSignal) {this.terminationSignal = terminationSignal;}

    public List<String> getStdout() {return stdout;}

    public void setStdout(List<String> stdout) {this.stdout = stdout != null ? stdout : new ArrayList<>();}

    public List<String> getStderr() {return stderr;}

    public void setStderr(List<String> stderr) {this.stderr = stderr != null ? stderr : new ArrayList<>();}

    public StringBuilder getQList() {return qlist;}

    public void setQList (StringBuilder qlist) {this.qlist = qlist;}

    public SZSStatus getSzsStatus() {return szsStatus;}

    public void setSzsStatus(SZSStatus szsStatus) {this.szsStatus = szsStatus != null ? szsStatus : SZSStatus.UNKNOWN;}

    public String getSzsStatusRaw() {return szsStatusRaw;}

    public void setSzsStatusRaw(String szsStatusRaw) {this.szsStatusRaw = szsStatusRaw;}

    public String getSzsDiagnostics() {return szsDiagnostics;}

    public void setSzsDiagnostics(String szsDiagnostics) {this.szsDiagnostics = szsDiagnostics;}

    public String getSzsOutputType() {return szsOutputType;}

    public void setSzsOutputType(String szsOutputType) {this.szsOutputType = szsOutputType;}

    public List<TPTPFormula> getProofSteps() {return proofSteps;}

    public void setProofSteps(List<TPTPFormula> proofSteps) {this.proofSteps = proofSteps;}

    public Map<String, String> getAnswerBindings() {return answerBindings;}

    public void setAnswerBindings(Map<String, String> answerBindings) {this.answerBindings = answerBindings;}

    public boolean isInconsistencyDetected() {return inconsistencyDetected;}

    public void setInconsistencyDetected(boolean inconsistencyDetected) {this.inconsistencyDetected = inconsistencyDetected;}

    public List<String> getParsingWarnings() {return parsingWarnings;}

    public void setParsingWarnings(List<String> parsingWarnings) {this.parsingWarnings = parsingWarnings;}

    public List<String> getErrorLines() {return errorLines;}

    public void setErrorLines(List<String> errorLines) {this.errorLines = errorLines != null ? errorLines : new ArrayList<>();}

    public String getPrimaryError() {return primaryError;}

    public void setPrimaryError(String primaryError) {this.primaryError = primaryError;}

    public List<String> getWarnings() {return warnings;}

    public void setWarnings(List<String> warnings) {this.warnings = warnings != null ? warnings : new ArrayList<>();}

    public boolean isSuccess() { return szsStatus != null && szsStatus.isSuccess();}

    public boolean hasProof() {return proofSteps != null && !proofSteps.isEmpty();}

    public boolean hasAnswers() {return answerBindings != null && !answerBindings.isEmpty();}

    public boolean hasErrors() {
        if (exitCode != 0 && exitCode != -1) return true;
        if (szsStatus != null && szsStatus.isError()) return true;
        return !errorLines.isEmpty() || (stderr != null && !stderr.isEmpty());
    }

    public boolean hasWarnings() {return warnings != null && !warnings.isEmpty();}

    public boolean hasStderr() {return stderr != null && !stderr.isEmpty();}

    public boolean hasStdout() {return stdout != null && !stdout.isEmpty();}

    public String getSummary() {

        StringBuilder sb = new StringBuilder();
        sb.append(engineName != null ? engineName : "Prover");
        if (engineMode != null) sb.append(" (").append(engineMode).append(")");
        sb.append(": ");
        sb.append(szsStatus != null ? szsStatus.getTptpName() : "Unknown");
        if (elapsedTimeMs > 0) sb.append(" in ").append(elapsedTimeMs).append("ms");
        if (timedOut) sb.append(" (timed out)");
        if (hasProof()) sb.append(", ").append(proofSteps.size()).append(" proof steps");
        if (hasAnswers()) sb.append(", ").append(answerBindings.size()).append(" answers");
        return sb.toString();
    }

    /**
     * Get CSS class for UI styling based on status
     */
    public String getCssClass() {return (szsStatus == null) ?  "szs-unknown" : szsStatus.getCssClass();}

    /**
     * Get CSS class for the status badge
     */
    public String getStatusBadgeClass() {return getCssClass();}

    /**
     * Extract and populate SZS information from stdout
     */
    public void extractSzsFromOutput() {
        if (stdout == null || stdout.isEmpty()) return;
        SZSStatus extracted = SZSExtractor.extractStatus(stdout);
        if (extracted != null) this.szsStatus = extracted;
        this.szsStatusRaw = SZSExtractor.extractStatusLine(stdout);
        this.szsDiagnostics = SZSExtractor.extractDiagnostics(stdout);
        this.szsOutputType = SZSExtractor.extractOutputType(stdout);
        this.errorLines = SZSExtractor.extractErrorLines(stdout, stderr);
        this.primaryError = SZSExtractor.getPrimaryError(stdout, stderr);
        this.warnings = SZSExtractor.extractWarnings(stdout);
    }

    /** 
     * Render the ATPResult panel showing SZS status, timing, and diagnostics 
     * @return html of the ATP result
     */
    public String resultPanelToHTML() {

        if (this == null) return "";
        StringBuilder html = new StringBuilder();
        String statusName = this.getSzsStatus() != null ? this.getSzsStatus().getTptpName() : "Unknown";
        String cssClass = this.getCssClass();
        String szsUrl = "https://tptp.org/UserDocs/SZSOntology/";
        String engineInfo = this.getEngineName() != null ? this.getEngineName() : "Prover";
        if (this.getEngineMode() != null && !this.getEngineMode().isEmpty()) engineInfo += " (" + this.getEngineMode() + ")";
        html.append("<div class='atp-result-panel'>");
        html.append("<div class='result-header'>");
        html.append("<a class='szs-link' href='" + szsUrl + "' target='_blank' rel='noopener noreferrer'>" + "<span class='szs-badge " + cssClass + "'>" + ValidationUtils.sanitizeString((statusName) + "</span>" + "</a>"));
        html.append("<span class='engine-tag'>" + ValidationUtils.sanitizeString((engineInfo) + "</span>"));
        html.append("</div>");
        html.append("<div class='result-meta'>");
        if (this.getInputLanguage() != null) html.append("<span>Input: " + ValidationUtils.sanitizeString((this.getInputLanguage()) + "</span>"));
        html.append("<span>Time: " + this.getElapsedTimeMs() + "ms");
        if (this.getTimeoutMs() > 0) html.append(" / " + this.getTimeoutMs() + "ms limit");
        html.append("</span>");
        if (this.getExitCode() != 0 && this.getExitCode() != -1) html.append("<span>Exit: " + this.getExitCode() + "</span>");
        if (this.isTimedOut()) html.append("<span style='color:#856404;'>Timed out</span>");
        html.append("</div>");
        if (this.hasErrors() || this.hasStderr()) {
            html.append("<details class='result-errors' open>");
            html.append("<summary>Diagnostics</summary>");
            html.append("<pre>");
            if (this.getPrimaryError() != null && !this.getPrimaryError().isEmpty()) html.append(ValidationUtils.sanitizeString((this.getPrimaryError())));
            if (this.getSzsDiagnostics() != null && !this.getSzsDiagnostics().isEmpty()) html.append("SZS: " + ValidationUtils.sanitizeString((this.getSzsDiagnostics())));
            List<String> errorLines = this.getErrorLines();
            if (errorLines != null && !errorLines.isEmpty()) {
                for (int i = 0; i < Math.min(20, errorLines.size()); i++) html.append(ValidationUtils.sanitizeString((errorLines.get(i)))).append("\n");
                if (errorLines.size() > 20) html.append("... (" + (errorLines.size() - 20) + " more lines)");
            }
            List<String> stderr = this.getStderr();
            if (stderr != null && !stderr.isEmpty() && (errorLines == null || errorLines.isEmpty())) {
                for (int i = 0; i < Math.min(15, stderr.size()); i++) html.append(ValidationUtils.sanitizeString((stderr.get(i)))).append("\n");
                if (stderr.size() > 15) html.append("... (" + (stderr.size() - 15) + " more lines)");
            }
            html.append("</pre>");
            html.append("</details>");
        }
        List<String> stdout = this.getStdout();
        if (stdout != null && !stdout.isEmpty()) {
            html.append("<details class='result-raw'>");
            html.append("<summary>Raw Prover Output (" + stdout.size() + " lines)</summary>");
            html.append("<pre>");
            int total = stdout.size();
            int start = Math.max(0, total - 200);
            html.append(ValidationUtils.sanitizeString((stdout.get(start))));
            for (int i = start + 1; i < total; i++) html.append("\n" + ValidationUtils.sanitizeString((stdout.get(i))));
            if (total > 200) html.append("... (" + (total - 200) + " earlier lines omitted)");
            html.append("</pre>");
            html.append("</details>");
        }
        html.append("</div>");
        return html.toString();
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
        if (szsStatus == null || szsStatus == SZSStatus.NOT_RUN) szsStatus = SZSStatus.fromExitCode(exitCode, timedOut);
        if (szsStatus.isSuccess()) {
            this.timedOut = false;
            return;
        }
        if (!this.timedOut && SZSExtractor.indicatesTimeout(stdout)) {
            this.timedOut = true;
            szsStatus = SZSStatus.TIMEOUT;
        }
        if (SZSExtractor.indicatesResourceOut(stdout)) szsStatus = SZSStatus.RESOURCE_OUT;
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
