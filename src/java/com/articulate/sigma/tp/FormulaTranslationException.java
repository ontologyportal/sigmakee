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

import java.util.List;

/**
 * Exception thrown when a formula cannot be translated from SUO-KIF
 * to a target format (TPTP, TFF, THF).
 */
public class FormulaTranslationException extends ATPException {

    private final String targetLanguage;    // "FOF", "TFF", "THF"
    private int errorLine;            // Line number in formula, if known
    private final int errorColumn;          // Column number, if known
    private final String errorDetail;       // Specific error detail

    /**
     * Create a new FormulaTranslationException
     *
     * @param message Error message describing the translation failure
     * @param targetLanguage The target language (FOF, TFF, THF)
     */
    public FormulaTranslationException(String message, String targetLanguage) {
        super(buildMessage(message, targetLanguage), targetLanguage + " Parser");
        this.targetLanguage = targetLanguage;
        this.errorLine = -1;
        this.errorColumn = -1;
        this.errorDetail = message;
    }

    /**
     * Create a new FormulaTranslationException with position info
     *
     * @param message Error message
     * @param targetLanguage Target language
     * @param line Line number where error occurred (1-based)
     * @param column Column number where error occurred (1-based)
     */
    public FormulaTranslationException(String message, String targetLanguage,
                                        int line, int column) {
        super(buildMessage(message, targetLanguage), targetLanguage + " Parser");
        this.targetLanguage = targetLanguage;
        this.errorLine = line;
        this.errorColumn = column;
        this.errorDetail = message;
    }

    /**
     * Create a new FormulaTranslationException with a cause
     */
    public FormulaTranslationException(String message, String targetLanguage, Throwable cause) {
        super(buildMessage(message, targetLanguage), targetLanguage + " Parser", cause);
        this.targetLanguage = targetLanguage;
        this.errorLine = -1;
        this.errorColumn = -1;
        this.errorDetail = message;
    }

    /**
     * Create a new FormulaTranslationException with a std errors
     */
    public FormulaTranslationException(String message, String targetLanguage, int line, List<String> stdout, List<String> stderr) {
        super(buildMessage(message, targetLanguage), targetLanguage + " Parser", stdout, stderr);
        this.targetLanguage = targetLanguage;
        this.errorLine = line;
        this.errorColumn = -1;
        this.errorDetail = message;
    }

    private static String buildMessage(String message, String targetLanguage) {
        StringBuilder sb = new StringBuilder();
        sb.append("Formula translation to ");
        sb.append(targetLanguage != null ? targetLanguage : "TPTP");
        sb.append(" failed");
        if (message != null && !message.isEmpty()) {
            sb.append(": ").append(message);
        }
        return sb.toString();
    }

    /**
     * @return The target language (FOF, TFF, THF)
     */
    public String getTargetLanguage() {
        return targetLanguage;
    }

    /**
     * @return The line number where the error occurred, or -1 if unknown
     */
    public int getErrorLine() {
        return errorLine;
    }

    /**
     * @return The column number where the error occurred, or -1 if unknown
     */
    public int getErrorColumn() {
        return errorColumn;
    }

    /**
     * @return true if position information is available
     */
    public boolean hasPosition() {
        return errorLine > 0;
    }

    /**
     * @return The specific error detail
     */
    public String getErrorDetail() {
        return errorDetail;
    }

    @Override
    public String getSuggestion() {
        StringBuilder sb = new StringBuilder();
        sb.append("Check that the formula is valid SUO-KIF");
        if (targetLanguage != null) {
            sb.append(" and compatible with ").append(targetLanguage).append(" translation");
        }
        sb.append(".\n\n");
        sb.append("Common issues:\n");
        sb.append("- Unbalanced parentheses\n");
        sb.append("- Unknown predicates or functions\n");
        sb.append("- Higher-order constructs in first-order context\n");
        sb.append("- Row variables (@) in contexts that don't support them\n");
        sb.append("- Missing variable bindings in quantified formulas");
        if (errorLine != -1) {
            sb.append("\n");
            sb.append(" Since a specific line caused this issue ");
            sb.append(" open the generated SUMO.* files in a text editor and check the line indicated above.");
            sb.append(" On the previous line it will point to the SUO-KIF formula that caused the error.");
            sb.append("\n");
        }


        return sb.toString();
    }

    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage());

        if (hasPosition()) {
            sb.append("\nError at line ").append(errorLine);
            if (errorColumn > 0) {
                sb.append(", column ").append(errorColumn);
            }
        }

        sb.append("\n\n").append(getSuggestion());
        return sb.toString();
    }
}
