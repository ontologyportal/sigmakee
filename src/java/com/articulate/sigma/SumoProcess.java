package com.articulate.sigma;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * // FIXME: Is this a good name for the class? Change to SumoEvent?
 */
public class SumoProcess {
    private String name;

    // Use of TreeMultimap ensures iteration is predictable.
    private Multimap<ThematicRole, String> roles = TreeMultimap.create();

    private KB kb;

    public void addRole(ThematicRole role, String arg) {
        roles.put(role, arg);
    }

    public enum ThematicRole {
        ORIGIN, MANNER, EXPERIENCER, AGENT,
        /*INSTANCE,*/ RULE, PATIENT, INSTRUMENT, THEME, GOAL, BENEFACTIVE;

        /**
         * Is the string a ThematicRole?
         * Note that string check is case-insensitive.
         * @param input
         * @return true if string is a ThematicRole
         */
        public static boolean isThematicRole(String input)    {
            try {
                ThematicRole role = ThematicRole.valueOf(input.toUpperCase());
            } catch (IllegalArgumentException e) {
                // The relation isn't in our enum.
                return false;
            }
            return true;
        }
    }

    public SumoProcess(KB kb, String role, String roleArguments) {
        // Check kb arg.
        if(kb == null)    {
            throw new IllegalArgumentException("KB parameter is null");
        }
        this.kb = kb;

        // Check role arg.
        ThematicRole thematicRole = null;

        try {
            thematicRole = ThematicRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            // The role isn't in our enum.
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        String[] args = roleArguments.trim().split(" ");
        if(args.length != 2)    {
            throw new IllegalArgumentException("Expecting two tokens in roleArguments parameter: " + roleArguments);
        }

        if(! kb.isSubclass(args[0], "Process"))   {
            throw new IllegalArgumentException("Expecting first part of roleArguments parameter to be a Process: " + roleArguments);
        }

        name = args[0];
        roles.put(thematicRole, args[1]);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<String> getAgents() {
        return roles.get(ThematicRole.AGENT);
    }

    public void addAgents(Collection<String> roles) {
        List<String> newRoles = Lists.newArrayList(roles);
        this.roles.putAll(ThematicRole.AGENT, newRoles);
    }

    public Collection<String> getPatients() {
        return roles.get(ThematicRole.PATIENT);
    }

    public void addPatients(List<String> roles) {
        List<String> newRoles = Lists.newArrayList(roles);
        this.roles.putAll(ThematicRole.PATIENT, newRoles);
    }

    @Override
    public String toString()  {
        StringBuilder sb = new StringBuilder();
        // TODO: I don't understand the ordering here. See unit tests. Maybe I need to use getKey().
        for(Map.Entry<ThematicRole, String> mapPair : roles.entries())  {
            ThematicRole key = mapPair.getKey();
            String value = mapPair.getValue();

            sb.append(key.toString().toLowerCase()).append(" ").append(name).append(" ").append(value).append("\n");
        }

        return sb.toString();
    }

    public String toNaturalLanguage()   {
        // FIXME: first check if all variables resolved? If not, do what?
        StringBuilder sb = new StringBuilder();

        sb.append(formulateNaturalSubject()).append(" ").append(formulateNaturalVerb()).append(" ").append(formulateNaturalObject());
        String cleanedStr = sb.toString().trim();

        return cleanedStr + ".";
    }

    private String formulateNaturalObject() {
        return "";
    }

    private String formulateNaturalVerb() {
        // FIXME: this seems a very "heavy" way to convert from, e.g. "driving" to "drive"
        String verb = WordNet.wn.verbRootForm(name, name.toLowerCase());
        // FIXME: verbPlural is a misnomer; it finds the simple present singular form
        verb = WordNetUtilities.verbPlural(verb);
        return verb;
    }

    private String formulateNaturalSubject() {
        // TODO: assuming only one agent
        String noun = getAgents().iterator().next();

        if(kb.isSubclass(noun, "CorpuscularObject")) {
            noun = "A " + noun.toLowerCase();
        }

        return noun;
    }
}
