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

/**
 * Enumeration of SZS (TPTP Standard for System status values) statuses.
 * Based on the TPTP SZS Ontology: https://tptp.org/UserDocs/SZSOntology/
 *
 * These statuses are used to represent the outcome of automated theorem prover runs.
 */
public enum SZSStatus {

    // === Success statuses ===
    THEOREM("Theorem", Category.SUCCESS, "The conjecture is a theorem of the axioms"),
    UNSATISFIABLE("Unsatisfiable", Category.SUCCESS, "The formula set is unsatisfiable"),
    SATISFIABLE("Satisfiable", Category.SUCCESS, "The formula set is satisfiable"),
    COUNTER_SATISFIABLE("CounterSatisfiable", Category.SUCCESS, "The negated conjecture is satisfiable"),
    EQUIVALENT("Equivalent", Category.SUCCESS, "Two formulas are equivalent"),
    EQUI_SATISFIABLE("EquiSatisfiable", Category.SUCCESS, "Formulas are equisatisfiable"),
    TAUTOLOGY("Tautology", Category.SUCCESS, "The formula is a tautology"),
    CONTRADICTORY("Contradictory", Category.SUCCESS, "The formula is contradictory"),

    // === Incomplete/Unknown ===
    UNKNOWN("Unknown", Category.UNKNOWN, "Cannot determine the status"),
    GAVE_UP("GaveUp", Category.UNKNOWN, "The prover gave up without determining status"),
    INCOMPLETE("Incomplete", Category.UNKNOWN, "The search was incomplete"),
    STOPPED("Stopped", Category.UNKNOWN, "The prover was stopped"),

    // === Failure (not an error, but didn't prove) ===
    TIMEOUT("Timeout", Category.FAILURE, "Time limit exceeded"),
    RESOURCE_OUT("ResourceOut", Category.FAILURE, "Memory or other resource limit exceeded"),
    USER("User", Category.FAILURE, "User request to stop"),

    // === Error statuses ===
    INPUT_ERROR("InputError", Category.ERROR, "Error in the input"),
    SYNTAX_ERROR("SyntaxError", Category.ERROR, "Syntax error in input"),
    TYPE_ERROR("TypeError", Category.ERROR, "Type checking failed"),
    SEMANTIC_ERROR("SemanticError", Category.ERROR, "Semantic error in input"),
    OS_ERROR("OSError", Category.ERROR, "Operating system error"),
    INAPPROPRIATE("Inappropriate", Category.ERROR, "Problem type not supported"),
    ERROR("Error", Category.ERROR, "Generic error"),

    // === Internal (sigmakee-generated, not from prover) ===
    NOT_RUN("NotRun", Category.ERROR, "Prover was never executed"),
    CRASHED("Crashed", Category.ERROR, "Prover process crashed or was killed by signal"),
    OUTPUT_PARSE_ERROR("OutputParseError", Category.ERROR, "Could not parse prover output");

    /**
     * Category groupings for SZS statuses
     */
    public enum Category {
        SUCCESS,   // Prover successfully determined status
        FAILURE,   // Prover did not succeed but no error occurred
        ERROR,     // An error occurred
        UNKNOWN    // Status could not be determined
    }

    private final String tptpName;
    private final Category category;
    private final String description;

    SZSStatus(String tptpName, Category category, String description) {
        this.tptpName = tptpName;
        this.category = category;
        this.description = description;
    }

    /**
     * @return The TPTP standard name for this status (e.g., "Theorem", "Timeout")
     */
    public String getTptpName() {
        return tptpName;
    }

    /**
     * @return The category this status belongs to
     */
    public Category getCategory() {
        return category;
    }

    /**
     * @return Human-readable description of this status
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return true if this status indicates successful proof/determination
     */
    public boolean isSuccess() {
        return category == Category.SUCCESS;
    }

    /**
     * @return true if this status indicates failure (timeout, resource limit, etc.)
     */
    public boolean isFailure() {
        return category == Category.FAILURE;
    }

    /**
     * @return true if this status indicates an error occurred
     */
    public boolean isError() {
        return category == Category.ERROR;
    }

    /**
     * @return true if this status indicates unknown outcome
     */
    public boolean isUnknown() {
        return category == Category.UNKNOWN;
    }

    /**
     * Parse a string to an SZSStatus enum value.
     * Handles various formats including with/without "SZS status" prefix.
     *
     * @param s The string to parse (case-insensitive)
     * @return The matching SZSStatus, or UNKNOWN if not recognized
     */
    public static SZSStatus fromString(String s) {
        if (s == null || s.isEmpty()) {
            return UNKNOWN;
        }

        // Remove common prefixes
        String cleaned = s.trim();
        if (cleaned.toLowerCase().startsWith("szs status ")) {
            cleaned = cleaned.substring(11).trim();
        }
        // Handle "% SZS status Theorem for problem" format
        int forIndex = cleaned.toLowerCase().indexOf(" for ");
        if (forIndex > 0) {
            cleaned = cleaned.substring(0, forIndex).trim();
        }
        // Handle diagnostics after colon
        int colonIndex = cleaned.indexOf(':');
        if (colonIndex > 0) {
            cleaned = cleaned.substring(0, colonIndex).trim();
        }

        // Try exact match first (case-insensitive)
        for (SZSStatus status : values()) {
            if (status.tptpName.equalsIgnoreCase(cleaned)) {
                return status;
            }
        }

        // Try enum name match
        for (SZSStatus status : values()) {
            if (status.name().equalsIgnoreCase(cleaned)) {
                return status;
            }
        }

        // Handle some common variations
        String upper = cleaned.toUpperCase().replace(" ", "_").replace("-", "_");
        switch (upper) {
            case "THM":
            case "THEOREM":
                return THEOREM;
            case "UNSAT":
            case "UNSATISFIABLE":
                return UNSATISFIABLE;
            case "SAT":
            case "SATISFIABLE":
                return SATISFIABLE;
            case "CSA":
            case "COUNTERSATISFIABLE":
                return COUNTER_SATISFIABLE;
            case "TMO":
            case "TIMEOUT":
            case "TIME_LIMIT":
            case "TIMELIMIT":
                return TIMEOUT;
            case "RSO":
            case "RESOURCEOUT":
            case "RESOURCE_OUT":
            case "MEMORY_LIMIT":
            case "MEMORYLIMIT":
                return RESOURCE_OUT;
            case "GUP":
            case "GAVEUP":
            case "GAVE_UP":
                return GAVE_UP;
            case "INE":
            case "INPUTERROR":
            case "INPUT_ERROR":
                return INPUT_ERROR;
            case "SYE":
            case "SYNTAXERROR":
            case "SYNTAX_ERROR":
                return SYNTAX_ERROR;
            case "TYE":
            case "TYPEERROR":
            case "TYPE_ERROR":
                return TYPE_ERROR;
            case "SEE":
            case "SEMANTICERROR":
            case "SEMANTIC_ERROR":
                return SEMANTIC_ERROR;
            case "OSE":
            case "OSERROR":
            case "OS_ERROR":
                return OS_ERROR;
            case "IAP":
            case "INAPPROPRIATE":
                return INAPPROPRIATE;
            case "STP":
            case "STOPPED":
                return STOPPED;
            case "UNK":
            case "UNKNOWN":
                return UNKNOWN;
            case "INC":
            case "INCOMPLETE":
                return INCOMPLETE;
            default:
                return UNKNOWN;
        }
    }

    /**
     * Determine SZS status from process exit code and timeout flag.
     * Used when no explicit SZS status is found in output.
     *
     * @param exitCode The process exit code
     * @param timedOut Whether the process timed out
     * @return Inferred SZSStatus
     */
    public static SZSStatus fromExitCode(int exitCode, boolean timedOut) {
        if (timedOut) {
            return TIMEOUT;
        }
        if (exitCode == 0) {
            return UNKNOWN; // Success exit but no SZS status found
        }
        // Check for signal-based exit codes (128 + signal number)
        if (exitCode > 128 && exitCode < 160) {
            return CRASHED;
        }
        // Generic non-zero exit
        return ERROR;
    }

    /**
     * Get CSS class name for UI styling based on category
     */
    public String getCssClass() {
        switch (category) {
            case SUCCESS:
                return "szs-success";
            case FAILURE:
                return "szs-failure";
            case ERROR:
                return "szs-error";
            case UNKNOWN:
            default:
                return "szs-unknown";
        }
    }

    @Override
    public String toString() {
        return tptpName;
    }
}
