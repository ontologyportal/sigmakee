package com.articulate.sigma.nlg;

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;

import java.util.Collection;

/**
 * Identifies specific Sumo CaseRole objects. Not all Sumo CaseRoles are listed here--only those whose behavior
 * must be defined for NLG. Any role not defined here is assumed to behave identically to the CaseRole which it
 * is a subrelation of (except for the formatted string property). For example, because "(subrelation result patient)",
 * we expect the "result" CaseRole to generally appear as the direct object of the sentence, just like the "patient"
 * case role.
 */
public enum CaseRole {
    AGENT, ATTENDS, DESTINATION, DIRECTION, EVENTPARTLYLOCATED, EXPERIENCER, INSTRUMENT, MOVES, ORIGIN, PATH, PATIENT, RESOURCE, OTHER;

    /******************************************************************
     * Is the input string a valid case role?
     * Note that this method is case-sensitive.
     * @param input
     * @return
     */
    public static boolean isKnownCaseRole(String input)    {
        try {
            CaseRole.valueOf(input);
        } catch (IllegalArgumentException e) {
            // The input isn't in our enum.
            return false;
        }
        return true;
    }

    /******************************************************************
     * From the input list, return the first string that is a valid case role.
     * @param list
     * @return
     *  a CaseRole object if its string is found in the list; else null
     */
    public static CaseRole getCaseRole(Collection<String> list)    {
        for (String input : list)    {
            input = input.toUpperCase();
            if (isKnownCaseRole(input))   {
                return CaseRole.valueOf(input);
            }
        }
        return null;
    }

    /******************************************************************
     * Return a valid case role based on the string input. If string does not match any case role,
     * return OTHER.
     * @param input
     * @return
     */
    public static CaseRole toCaseRole(String input, KB kb)     {
        CaseRole role = null;
        try {
            role = CaseRole.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            // The input isn't in our CaseRole enum. No problem--we deal with this below.
        }
        if (role == null) {
            if (kb.kbCache.parents.containsKey("subrelation") && kb.kbCache.parents.get("subrelation").containsKey(input)) {
                CaseRole inputRole = getCaseRole(kb.kbCache.parents.get("subrelation").get(input));
                if (inputRole != null) {
                    // The role has a subrelation relationship with the a known role.
                    role = inputRole;
                }
            }
        }
        if(role == null) {
            role = CaseRole.OTHER;
        }

        return role;
    }

}
