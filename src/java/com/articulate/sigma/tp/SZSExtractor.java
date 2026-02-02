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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for extracting SZS (TPTP Standard for System status values)
 * information from prover output.
 *
 * Handles output formats from:
 * - Vampire: "% SZS status Theorem for SUMO"
 * - EProver: "# SZS status Theorem"
 * - LEO-III: "% SZS status Theorem for problem"
 */
public class SZSExtractor {

    // Pattern: "% SZS status Theorem for problem" or "SZS status Timeout"
    // Also handles "# SZS status" (EProver format)
    private static final Pattern STATUS_PATTERN = Pattern.compile(
            "[%#]?\\s*SZS\\s+status\\s+(\\w+)(?:\\s+for\\s+\\S+)?(?:\\s*:\\s*(.*))?",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern: "% SZS output start Proof" / "% SZS output start CNFRefutation"
    private static final Pattern OUTPUT_START_PATTERN = Pattern.compile(
            "[%#]?\\s*SZS\\s+output\\s+start\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern: "% SZS output end Proof"
    private static final Pattern OUTPUT_END_PATTERN = Pattern.compile(
            "[%#]?\\s*SZS\\s+output\\s+end\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE
    );

    // Common error indicators in prover output
    private static final String[] ERROR_INDICATORS = {
            "Error", "error:", "ERROR:", "ERROR in",
            "Syntax error", "syntax error",
            "Type error", "type error", "TypeError",
            "Parse error", "parse error",
            "exception", "Exception", "EXCEPTION",
            "FATAL", "fatal", "Fatal",
            "Failure", "failure",
            "Warning", "warning", "WARNING",
            "Unknown", "unknown type",
            "undefined", "Undefined",
            "cannot", "Cannot",
            "Invalid", "invalid",
            "Illegal", "illegal",
            "Unexpected", "unexpected"
    };

    // Indicators that are warnings rather than errors
    private static final String[] WARNING_INDICATORS = {
            "Warning", "warning", "WARNING",
            "deprecated", "Deprecated"
    };

    /**
     * Private constructor - utility class
     */
    private SZSExtractor() {
    }

    /**
     * Extract the SZS status from prover output.
     *
     * @param output List of output lines from the prover
     * @return The extracted SZSStatus, or null if none found
     */
    public static SZSStatus extractStatus(List<String> output) {
        if (output == null || output.isEmpty()) {
            return null;
        }

        for (String line : output) {
            Matcher m = STATUS_PATTERN.matcher(line);
            if (m.find()) {
                String statusStr = m.group(1);
                return SZSStatus.fromString(statusStr);
            }
        }

        return null;
    }

    /**
     * Extract the raw SZS status line from prover output.
     *
     * @param output List of output lines from the prover
     * @return The full status line, or null if none found
     */
    public static String extractStatusLine(List<String> output) {
        if (output == null || output.isEmpty()) {
            return null;
        }

        for (String line : output) {
            if (STATUS_PATTERN.matcher(line).find()) {
                return line.trim();
            }
        }

        return null;
    }

    /**
     * Extract the raw status string (just the status word) from output.
     *
     * @param output List of output lines
     * @return The status word (e.g., "Theorem", "Timeout"), or null
     */
    public static String extractStatusRaw(List<String> output) {
        if (output == null || output.isEmpty()) {
            return null;
        }

        for (String line : output) {
            Matcher m = STATUS_PATTERN.matcher(line);
            if (m.find()) {
                return m.group(1);
            }
        }

        return null;
    }

    /**
     * Extract diagnostics from SZS status line (text after colon).
     * Example: "% SZS status TypeError : line 42, bad type"
     * Returns: "line 42, bad type"
     *
     * @param output List of output lines
     * @return Diagnostics string, or null if none
     */
    public static String extractDiagnostics(List<String> output) {
        if (output == null || output.isEmpty()) {
            return null;
        }

        for (String line : output) {
            Matcher m = STATUS_PATTERN.matcher(line);
            if (m.find() && m.group(2) != null) {
                return m.group(2).trim();
            }
        }

        return null;
    }

    /**
     * Extract the SZS output type (Proof, Refutation, Model, etc.)
     *
     * @param output List of output lines
     * @return Output type string, or null if none found
     */
    public static String extractOutputType(List<String> output) {
        if (output == null || output.isEmpty()) {
            return null;
        }

        for (String line : output) {
            Matcher m = OUTPUT_START_PATTERN.matcher(line);
            if (m.find()) {
                return m.group(1);
            }
        }

        return null;
    }

    /**
     * Extract lines that appear to contain error messages from stdout.
     *
     * @param output List of stdout lines
     * @return List of lines containing error indicators
     */
    public static List<String> extractErrorLines(List<String> output) {
        List<String> errors = new ArrayList<>();

        if (output == null || output.isEmpty()) {
            return errors;
        }

        for (String line : output) {
            if (isErrorLine(line) && !isWarningOnly(line)) {
                errors.add(line);
            }
        }

        return errors;
    }

    /**
     * Extract error lines from both stdout and stderr.
     *
     * @param stdout List of stdout lines
     * @param stderr List of stderr lines
     * @return Combined list of error lines (stderr takes precedence)
     */
    public static List<String> extractErrorLines(List<String> stdout, List<String> stderr) {
        List<String> errors = new ArrayList<>();

        // Stderr lines are generally more relevant
        if (stderr != null && !stderr.isEmpty()) {
            for (String line : stderr) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    errors.add(trimmed);
                }
            }
        }

        // Add stdout error lines if not too many stderr lines
        if (errors.size() < 20) {
            List<String> stdoutErrors = extractErrorLines(stdout);
            for (String line : stdoutErrors) {
                if (errors.size() >= 50) break;
                if (!errors.contains(line)) {
                    errors.add(line);
                }
            }
        }

        return errors;
    }

    /**
     * Extract warning lines from output.
     *
     * @param output List of output lines
     * @return List of warning lines
     */
    public static List<String> extractWarnings(List<String> output) {
        List<String> warnings = new ArrayList<>();

        if (output == null || output.isEmpty()) {
            return warnings;
        }

        for (String line : output) {
            if (isWarningOnly(line)) {
                warnings.add(line);
            }
        }

        return warnings;
    }

    /**
     * Check if a line contains an error indicator.
     */
    private static boolean isErrorLine(String line) {
        if (line == null) return false;
        for (String indicator : ERROR_INDICATORS) {
            if (line.contains(indicator)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a line is a warning (not an error).
     */
    private static boolean isWarningOnly(String line) {
        if (line == null) return false;

        // Check for warning indicators
        boolean hasWarning = false;
        for (String indicator : WARNING_INDICATORS) {
            if (line.contains(indicator)) {
                hasWarning = true;
                break;
            }
        }

        if (!hasWarning) return false;

        // Make sure it's not also an error
        for (String indicator : ERROR_INDICATORS) {
            if (line.contains(indicator) && !isInArray(indicator, WARNING_INDICATORS)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isInArray(String s, String[] arr) {
        for (String item : arr) {
            if (item.equals(s)) return true;
        }
        return false;
    }

    /**
     * Check if output indicates the prover timed out.
     * Looks for both SZS status and common timeout patterns.
     *
     * @param output List of output lines
     * @return true if timeout is indicated
     */
    public static boolean indicatesTimeout(List<String> output) {
        if (output == null || output.isEmpty()) {
            return false;
        }

        SZSStatus status = extractStatus(output);
        if (status == SZSStatus.TIMEOUT) {
            return true;
        }

        // Check for common timeout patterns
        for (String line : output) {
            String lower = line.toLowerCase();
            if (lower.contains("time limit") ||
                    lower.contains("time out") ||
                    lower.contains("timeout") ||
                    lower.contains("timed out") ||
                    lower.contains("cpu time limit")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if output indicates resource exhaustion.
     *
     * @param output List of output lines
     * @return true if resource exhaustion is indicated
     */
    public static boolean indicatesResourceOut(List<String> output) {
        if (output == null || output.isEmpty()) {
            return false;
        }

        SZSStatus status = extractStatus(output);
        if (status == SZSStatus.RESOURCE_OUT) {
            return true;
        }

        // Check for memory limit patterns
        for (String line : output) {
            String lower = line.toLowerCase();
            if (lower.contains("memory limit") ||
                    lower.contains("out of memory") ||
                    lower.contains("memory exhausted") ||
                    lower.contains("oom")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the primary error message from the output.
     * Tries to find the most informative single error line.
     *
     * @param stdout Stdout lines
     * @param stderr Stderr lines
     * @return Primary error message, or null if none found
     */
    public static String getPrimaryError(List<String> stdout, List<String> stderr) {
        // Prefer stderr
        if (stderr != null && !stderr.isEmpty()) {
            for (String line : stderr) {
                String trimmed = line.trim();
                // Skip empty lines and common noise
                if (!trimmed.isEmpty() &&
                        !trimmed.startsWith("#") &&
                        !trimmed.startsWith("%") &&
                        trimmed.length() > 5) {
                    return trimmed;
                }
            }
        }

        // Fall back to first error line in stdout
        List<String> errors = extractErrorLines(stdout);
        if (!errors.isEmpty()) {
            return errors.get(0);
        }

        return null;
    }

    /**
     * Determine if output suggests success (proof found, etc.)
     *
     * @param output List of output lines
     * @return true if output indicates success
     */
    public static boolean indicatesSuccess(List<String> output) {
        SZSStatus status = extractStatus(output);
        return status != null && status.isSuccess();
    }

    /**
     * Determine if output contains a proof.
     *
     * @param output List of output lines
     * @return true if a proof section is present
     */
    public static boolean containsProof(List<String> output) {
        if (output == null || output.isEmpty()) {
            return false;
        }

        String outputType = extractOutputType(output);
        if (outputType != null) {
            String lower = outputType.toLowerCase();
            return lower.contains("proof") || lower.contains("refutation");
        }

        // Check for proof markers
        for (String line : output) {
            if (line.contains("Proof found") ||
                    line.contains("proof found") ||
                    line.contains("SZS output start")) {
                return true;
            }
        }

        return false;
    }
}
