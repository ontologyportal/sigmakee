package com.articulate.sigma.nlg;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * An element in a sentence conceived of as having a Subject-Verb-Object grammar.
 * Not all properties will be used by all the positions.
 */

public class SVOElement {
    public enum SVOGrammarPosition {SUBJECT, DIRECT_OBJECT, INDIRECT_OBJECT}

    public static enum NUMBER {
        SINGULAR, PLURAL;

        /**
         * Given a count, return singular or plural. If ct is 0, returns singular.
         * Throws IllegalArgumentException if ct < 0.
         * @param ct
         * @return
         */
        public static NUMBER getNumber(int ct)    {
            if (ct < 0)  {
                throw new IllegalArgumentException("ct cannot be less than zero to determine singular or plural.");
            }

            if (ct == 0 || ct == 1) {
                return SINGULAR;
            }
            else    {
                return PLURAL;
            }
        }
    }


    public final SVOGrammarPosition position;

    private String surfaceForm = "";

    /**
     * Performs two tasks: keeps track of which case roles have been used to fill this grammatical position; and
     * lets outsiders know if the grammatical position is singular or plural
     */
    private Multiset<CaseRole> consumedCaseRoles = HashMultiset.create();

    public SVOElement(SVOGrammarPosition pos)   {
        position = pos;
    }

    /**************************************************************************************************************
     *
     * @return
     */
    public String getSurfaceForm() {
        return surfaceForm;
    }

    /**************************************************************************************************************
     *
     * @param surfaceForm
     */
    public void setSurfaceForm(String surfaceForm) {
        this.surfaceForm = surfaceForm;
    }

    /**************************************************************************************************************
     *
     * @return
     */
    public Multiset<CaseRole> getConsumedCaseRoles() {
        return HashMultiset.create(consumedCaseRoles);
    }

    /**************************************************************************************************************
     *
     * @param role
     */
    public void addConsumedCaseRole(CaseRole role)  {
        consumedCaseRoles.add(role);
    }

    /**************************************************************************************************************
     *
     * @return
     */
    public NUMBER getSingularPlural()   {
        return NUMBER.getNumber(getConsumedCaseRoles().size());
    }
}
