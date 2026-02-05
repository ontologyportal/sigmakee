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
 * Exception thrown when a formula has an incorrect arity for a predicate.
 * This occurs when a predicate is used with the wrong number of arguments.
 */
public class ArityException extends ATPException {

    private final String formula;
    private final String predicateName;

    /**
     * Create a new ArityException
     *
     * @param formula The formula containing the arity error
     * @param predicateName The predicate with incorrect arity
     */
    public ArityException(String formula, String predicateName) {
        super(buildMessage(formula, predicateName), "Arity Validator");
        this.formula = formula;
        this.predicateName = predicateName;
    }

    private static String buildMessage(String formula, String predicateName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Arity error for predicate '").append(predicateName).append("'");
        if (formula != null && !formula.isEmpty()) {
            sb.append(" in formula: ").append(formula);
        }
        return sb.toString();
    }

    /**
     * @return The formula containing the arity error
     */
    public String getFormula() {
        return formula;
    }

    /**
     * @return The predicate name with incorrect arity
     */
    public String getPredicateName() {
        return predicateName;
    }

    @Override
    public String getSuggestion() {
        StringBuilder sb = new StringBuilder();
        sb.append("The predicate '").append(predicateName).append("' was used with the wrong number of arguments.\n\n");
        sb.append("To fix this:\n");
        sb.append("1. Check the expected arity of '").append(predicateName).append("' in the SUMO ontology\n");
        sb.append("2. Verify the number of arguments in your formula matches the expected arity\n");
        sb.append("3. Look for missing or extra arguments in the predicate application");
        return sb.toString();
    }

    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage());
        sb.append("\n\n").append(getSuggestion());
        return sb.toString();
    }
}
