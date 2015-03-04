package com.articulate.sigma.nlg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.util.List;
import java.util.Map;

/**
 * Handles preposition behavior for case roles. Handles both default behavior--e.g. "the direction case role
 * is usually marked with the preposition 'toward'"--as well as exceptions for certain verbs.
 */
public class Preposition {
    // Mappings for verb + preposition.

    private static final Multimap<CaseRole, String> defaultPrepositions = ArrayListMultimap.create();

    // Read this as: "Usually destinations follow the preposition 'to'."
    private static final List<String> DESTINATION_PREPOSITIONS = Lists.newArrayList("to");

    private static final List<String> DIRECTION_PREPOSITIONS = Lists.newArrayList("toward");

    private static final List<String> EVENTPARTLYLOCATED_PREPOSITIONS = Lists.newArrayList("in");

    private static final List<String> INSTRUMENT_PREPOSITIONS = Lists.newArrayList("with");

    private static final List<String> ORIGIN_PREPOSITIONS = Lists.newArrayList("from");

    private static final List<String> PATH_PREPOSITIONS = Lists.newArrayList("along");

    private static final List<String> RESOURCE_PREPOSITIONS = Lists.newArrayList("out of", "from");

    // Insert default mappings into the multimap.
    static {
        defaultPrepositions.putAll(CaseRole.INSTRUMENT, INSTRUMENT_PREPOSITIONS);
        defaultPrepositions.putAll(CaseRole.DESTINATION, DESTINATION_PREPOSITIONS);
        defaultPrepositions.putAll(CaseRole.DIRECTION, DIRECTION_PREPOSITIONS);
        defaultPrepositions.putAll(CaseRole.EVENTPARTLYLOCATED, EVENTPARTLYLOCATED_PREPOSITIONS);
        defaultPrepositions.putAll(CaseRole.ORIGIN, ORIGIN_PREPOSITIONS);
        defaultPrepositions.putAll(CaseRole.PATH, PATH_PREPOSITIONS);
        defaultPrepositions.putAll(CaseRole.RESOURCE, RESOURCE_PREPOSITIONS);
    }

    // Mappings for verbs that fall out of the default preposition-case role behavior.
    private static final Map<String, Multimap<CaseRole, String>> specialVerbPrepositionBehaviorMap = Maps.newHashMap();
    // FIXME: Uncomment when benefits is made a CaseRole
//    static {
//        // "The benefits role for the verb 'help' does not take a preposition."
//        Multimap<CaseRole, String> prepsForCaseRole = ArrayListMultimap.create();
//        prepsForCaseRole.put(CaseRole.BENEFITS, "");
//        specialVerbPrepositionBehaviorMap.put("help", prepsForCaseRole);
//    }

    public static List<String> getPrepositionForCaseRole(String verb, CaseRole caseRole)   {
        // The default is empty string--no preposition.
        List<String> retList = Lists.newArrayList("");

        // First try exception list.
        if (specialVerbPrepositionBehaviorMap.containsKey(verb) && specialVerbPrepositionBehaviorMap.get(verb).containsKey(caseRole))    {
            Multimap<CaseRole, String> mm = specialVerbPrepositionBehaviorMap.get(verb);
            retList = Lists.newArrayList(mm.get(caseRole));
        }
        // Use the default.
        else    {
            retList = Lists.newArrayList(defaultPrepositions.get(caseRole));
            // If retList is empty, insert empty string into the first element, representing no preposition.
            if (retList.isEmpty())  {
                retList.add("");
            }
        }

        return retList;
    }

}
