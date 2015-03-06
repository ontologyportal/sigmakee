package com.articulate.sigma.nlg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.util.List;
import java.util.Map;

/**
 * Handles properties of traditional Subject-Verb-Object grammar, allowing for both default cases and exceptions for
 * specific verbs.
 */
public class SVOGrammar {


    private static final Multimap<SVOElement.SVOGrammarPosition, CaseRole> defaultGrammarPositions = ArrayListMultimap.create();

    // "The subject of the sentence will usually be the agent or the experiencer, in that order of likelihood."
    private static final List<CaseRole> SUBJECT_CASEROLES = Lists.newArrayList(CaseRole.AGENT, CaseRole.EXPERIENCER, CaseRole.MOVES);

    // "The direct object of the sentence will usually be the patient."
    private static final List<CaseRole> DIRECTOBJECT_CASEROLES = Lists.newArrayList(CaseRole.PATIENT, CaseRole.MOVES);

    // "The indirect object of the sentence will usually be the instrument."
    private static final List<CaseRole> INDIRECTOBJECT_CASEROLES = Lists.newArrayList(CaseRole.DIRECTION, CaseRole.PATH, CaseRole.ORIGIN, CaseRole.DESTINATION, CaseRole.EVENTPARTLYLOCATED,
            CaseRole.INSTRUMENT, CaseRole.RESOURCE);

    // Insert default mappings into the multimap.
    static {
        defaultGrammarPositions.putAll(SVOElement.SVOGrammarPosition.SUBJECT, SUBJECT_CASEROLES);
        defaultGrammarPositions.putAll(SVOElement.SVOGrammarPosition.DIRECT_OBJECT, DIRECTOBJECT_CASEROLES);
        defaultGrammarPositions.putAll(SVOElement.SVOGrammarPosition.INDIRECT_OBJECT, INDIRECTOBJECT_CASEROLES);
    }

    // Mappings for verb/grammar role combinations that fall out of the default grammar role behavior.
    private static final Map<String, Multimap<SVOElement.SVOGrammarPosition, CaseRole>> specialVerbGrammarPositionBehaviorMap = Maps.newHashMap();
    static  {
        // agent: The man burned the wood.  patient: The wood burned.
        Multimap<SVOElement.SVOGrammarPosition, CaseRole> svoList = ArrayListMultimap.create();
        svoList.putAll(SVOElement.SVOGrammarPosition.SUBJECT, Lists.newArrayList(CaseRole.AGENT, CaseRole.PATIENT));
        // FIXME: make resource a known Case Role? svoList.putAll(SVOGrammarRole.DIRECT_OBJECT, Lists.newArrayList(CaseRole.PATIENT, CaseRole.RESOURCE));
        specialVerbGrammarPositionBehaviorMap.put("burn", svoList);

        // patient: The boy fell.
        svoList = ArrayListMultimap.create();
        svoList.putAll(SVOElement.SVOGrammarPosition.SUBJECT, Lists.newArrayList(CaseRole.EXPERIENCER, CaseRole.PATIENT));
        specialVerbGrammarPositionBehaviorMap.put("fall", svoList);

        // experiencer: Jack sees.
        svoList = ArrayListMultimap.create();
        svoList.putAll(SVOElement.SVOGrammarPosition.SUBJECT, Lists.newArrayList(CaseRole.EXPERIENCER));
        specialVerbGrammarPositionBehaviorMap.put("see", svoList);

        // TODO: Uncomment when benefits is made a CaseRole
        // benefits/patient: Mary helps Mark.
//        svoList = ArrayListMultimap.create();
//        svoList.putAll(SVOGrammarRole.DIRECT_OBJECT, Lists.newArrayList(CaseRole.PATIENT, CaseRole.BENEFITS));
//        specialVerbBehaviorMap.put("help", svoList);
    }

    /**************************************************************************************************************
     * Return the case roles most appropriate for a given grammar role and a verb. Returns default values if there
     * are no special rules for the given verb.
     * @param verb
     * @param grammarRole
     * @return
     */
    public static List<CaseRole> getCaseRolesForGrammarPosition(String verb, SVOElement.SVOGrammarPosition grammarRole)   {
        List<CaseRole> retList;

        // First try exception list.
        if (specialVerbGrammarPositionBehaviorMap.containsKey(verb) && specialVerbGrammarPositionBehaviorMap.get(verb).containsKey(grammarRole))    {
            Multimap<SVOElement.SVOGrammarPosition, CaseRole> mm = specialVerbGrammarPositionBehaviorMap.get(verb);
            retList = Lists.newArrayList(mm.get(grammarRole));
        }
        // Use the default.
        else    {
            retList = Lists.newArrayList(defaultGrammarPositions.get(grammarRole));
        }

        return retList;
    }
}
