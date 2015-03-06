package com.articulate.sigma.nlg;

import java.util.List;

/**
 * A first-blush attempt at implementing the VerbProperties interface.
 */
public class VerbPropertiesSimpleImpl implements VerbProperties {


    /******************************************************************************************
     * Return a list of case roles for the given verb and the given grammar role. The list consists
     * of the case roles which, for this verb, are most likely to fill the given grammar role. The list will
     * be in order of likelihood, from most likely to less likely. For example ("give", SUBJECT) will return
     * [CaseRole.AGENT]; ("give", DIRECT_OBJECT) will return [CaseRole.PATIENT]; ("give", INDIRECT_OBJECT) will
     * return [CaseRole.DESTINATION].
     * @param verb
     *   the verb in infinitive form
     * @param grammarRole
     * @return
     */
    @Override
    public List<CaseRole> getCaseRolesForGrammarRole(String verb, SVOElement.SVOGrammarPosition grammarRole)   {
        return SVOGrammar.getCaseRolesForGrammarPosition(verb, grammarRole);
    }


    /******************************************************************************************
     * Return a list of prepositions for the given verb and the given case role. The list consists
     * of the prepositions which, for this verb, are most likely to precede the entity filling the given case role.
     * An empty string means "no preposition". The list will be in order of likelihood, from most likely to less likely.
     * For example ("give", Patient) will return "" because for this verb the Patient role does not take a preposition.
     * But ("give", DESTINATION) will return "to".
     * @param verb
     *   the verb in infinitive form
     * @param caseRole
     * @return
     *  the strings most likely to be used for this verb and case role, where empty string means "no preposition"
     */
    @Override
    public List<String> getPrepositionForCaseRole(String verb, CaseRole caseRole) {
        return Preposition.getPrepositionForCaseRole(verb, caseRole);
    }
}
