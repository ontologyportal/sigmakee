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
 * Exception thrown when an ATP process times out.
 * Distinguishes between "hard" timeouts (we killed the process)
 * and "soft" timeouts (the prover reported timeout via SZS status).
 */
public class ProverTimeoutException extends ATPException {

    private final long timeoutMs;           // Configured timeout
    private final long elapsedMs;           // Actual elapsed time
    private final boolean hardTimeout;      // true if we killed it, false if prover reported timeout

    /**
     * Create a new ProverTimeoutException
     *
     * @param engineName The name of the prover
     * @param timeoutMs The configured timeout in milliseconds
     * @param elapsedMs The actual elapsed time in milliseconds
     * @param hardTimeout true if we had to kill the process, false if prover self-reported
     */
    public ProverTimeoutException(String engineName, long timeoutMs, long elapsedMs, boolean hardTimeout) {
        super(buildMessage(engineName, timeoutMs, hardTimeout), engineName);
        this.timeoutMs = timeoutMs;
        this.elapsedMs = elapsedMs;
        this.hardTimeout = hardTimeout;
    }

    /**
     * Create a new ProverTimeoutException with output
     */
    public ProverTimeoutException(String engineName, long timeoutMs, long elapsedMs, boolean hardTimeout,
                                   List<String> stdout, List<String> stderr, ATPResult result) {
        super(buildMessage(engineName, timeoutMs, hardTimeout), engineName, result, stdout, stderr);
        this.timeoutMs = timeoutMs;
        this.elapsedMs = elapsedMs;
        this.hardTimeout = hardTimeout;
    }

    private static String buildMessage(String engineName, long timeoutMs, boolean hard) {
        String type = hard ? "Hard timeout" : "Timeout";
        long timeoutSec = timeoutMs / 1000;
        return type + ": " + engineName + " exceeded " + timeoutSec + "s limit";
    }

    /**
     * @return The configured timeout in milliseconds
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * @return The configured timeout in seconds
     */
    public long getTimeoutSeconds() {
        return timeoutMs / 1000;
    }

    /**
     * @return The actual elapsed time in milliseconds
     */
    public long getElapsedMs() {
        return elapsedMs;
    }

    /**
     * @return The actual elapsed time in seconds
     */
    public long getElapsedSeconds() {
        return elapsedMs / 1000;
    }

    /**
     * @return true if this was a "hard" timeout (we killed the process)
     */
    public boolean isHardTimeout() {
        return hardTimeout;
    }

    /**
     * @return true if the prover reported timeout itself (soft timeout)
     */
    public boolean isSoftTimeout() {
        return !hardTimeout;
    }

    @Override
    public String getSuggestion() {
        StringBuilder sb = new StringBuilder();
        sb.append("The prover could not find a proof within the time limit.\n\n");
        sb.append("You can try:\n");
        sb.append("1. Increasing the timeout (currently ").append(timeoutMs / 1000).append("s)\n");
        sb.append("2. Simplifying the query\n");
        sb.append("3. Adding more specific axioms to guide the proof\n");
        sb.append("4. Using a different prover mode (e.g., CASC vs Avatar for Vampire)\n");
        sb.append("5. Breaking the problem into smaller parts");
        return sb.toString();
    }

    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage());
        sb.append("\nElapsed time: ").append(elapsedMs).append("ms");
        sb.append("\nConfigured timeout: ").append(timeoutMs).append("ms");
        sb.append("\nTimeout type: ").append(hardTimeout ? "Hard (process killed)" : "Soft (prover self-reported)");
        sb.append("\n\n").append(getSuggestion());
        return sb.toString();
    }
}
