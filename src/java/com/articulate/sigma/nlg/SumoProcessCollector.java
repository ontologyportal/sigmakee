package com.articulate.sigma.nlg;

import com.articulate.sigma.KB;
import com.google.common.collect.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This object represents a process or an event, holding information on its case roles as
 * well as the entities which play that role.
 */
public class SumoProcessCollector {
    private SumoProcess sumoProcess;

    // Use of TreeMultimap ensures iteration is predictable.
    private final Multimap<CaseRole, String> roles = TreeMultimap.create();

    private KB kb;

    /**************************************************************************************
     * Construct a SumoProcess.
     * @param kb
     * @param role
     * @param process
     * @param entity
     */
    public SumoProcessCollector(KB kb, String role, String process, String entity) {
        // Check kb arg.
        if (kb == null)    {
            throw new IllegalArgumentException("KB parameter is null");
        }
        this.kb = kb;

        // Check other args for null or empty.
        if (role == null || role.isEmpty())  {
            throw new IllegalArgumentException("Role parameter is null or empty.");
        }
        if (process == null || process.isEmpty())  {
            throw new IllegalArgumentException("Process parameter is null or empty.");
        }
        if (entity == null || entity.isEmpty())  {
            throw new IllegalArgumentException("Entity parameter is null or empty.");
        }

        // Check specific properties of other args.
        String msg = "role = " + role + "; process = " + process + "; entity = " + entity + ".";
        if (! kb.kbCache.isInstanceOf(role, "CaseRole")) {
            throw new IllegalArgumentException("Invalid role: " + msg);
        }

        if (! process.equals("Process") && ! kb.isSubclass(process, "Process"))   {
            throw new IllegalArgumentException("Process parameter is not a Process: " + msg);
        }

        entity = checkEntityCase(entity);

        sumoProcess = new SumoProcess(process);
        //name = process;

        CaseRole caseRole = CaseRole.toCaseRole(role);
        roles.put(caseRole, entity);
    }


    /**************************************************************************************
     * Add a new role/entity pair to this event.
     * @param role
     * @param arg
     */
    public void addRole(String role, String arg) {
        String entity = checkEntityCase(arg);

        CaseRole caseRole = CaseRole.toCaseRole(role);
        roles.put(caseRole, entity);
    }


    /**************************************************************************************
     * Get all the entities playing a given role in this process.
     * @param role
     * @param roleScratchPad
     * @return
     */
    static Set<String> getRoleEntities(CaseRole role, Multimap<CaseRole, String> roleScratchPad)   {
        return Sets.newTreeSet(roleScratchPad.get(role));
    }


    /**************************************************************************************
     * Get all the roles and their entities for this event.
     * @return
     */
    public Multimap<CaseRole, String> getRolesAndEntities()   {
        Multimap<CaseRole, String> all = TreeMultimap.create();
        all.putAll(roles);
        return all;

    }


    /**************************************************************************************************************
     * Return a defensive copy of our case roles.
     * @return
     */
    Multimap<CaseRole,String> createNewRoleScratchPad() {
        return HashMultimap.create(roles);
    }


    /**************************************************************************************
     * Retain capitalization of names and reified instances by lower-casing if the entity is an Entity.
     * @param entity
     * @return
     */
    private String checkEntityCase(String entity) {

        // FIXME: instances of human and geographical area/region are capitalized; also holidays/weekdays/months/centuries
        String temp = entity;
        if(kb.isSubclass(entity, "Entity"))    {
            temp = temp.toLowerCase();
        }
        return temp;
    }


    /**************************************************************************************************************
     * Write out a string representing all the fields in this object.
     * @return
     */
    @Override
    public String toString()  {
        StringBuilder sb = new StringBuilder();

        for(Map.Entry<CaseRole, String> mapPair : roles.entries())  {
            String key = mapPair.getKey().toString();
            String value = mapPair.getValue();

            sb.append(key.toLowerCase()).append(" ").append(sumoProcess).append(" ").append(value).append("\n");
        }

        return sb.toString();
    }


    /**************************************************************************************************************
     * Translate this process and its case roles into natural language.
     * @return
     *  a natural language translation, or empty string if one is not possible
     */
    public String toNaturalLanguage()   {
        // We need a scratch pad, on which we can "scratch out" the items that have been used up.
        Multimap<CaseRole, String> roleScratchPad = createNewRoleScratchPad();

        String subject = formulateNaturalSubject(roleScratchPad);

        // If no subject, perform no NLG.
        if(subject.isEmpty())   {
            return "";
        }

        String cleanedStr = (subject + " " + formulateNaturalVerb() + " " +
                formulateNaturalDirectObject(roleScratchPad) + " " + formulateNaturalIndirectObject(roleScratchPad)).trim();

        return cleanedStr;
    }

    /**************************************************************************************************************
     * Return all the entities of the given role in the correct case.
     * Assumes the role will be some kind of noun.
     * @return
     */
    String formulateNounPhraseForCaseRole(CaseRole role, Multimap<CaseRole, String> roleScratchPad)    {
        if (! roles.containsKey(role))    {
            return "";
        }
        Set<String> nouns = getRoleEntities(role, roleScratchPad);

        StringBuilder sb = new StringBuilder();
        // We're assuming that only names and reified objects are in uppercase.
        for(String noun : nouns) {
            String temp = noun;
            if (! NLGStringUtils.isVariable(temp) && Noun.takesIndefiniteArticle(noun, kb)) {
                temp = Noun.aOrAn(temp) + " " + temp;
            }
            sb.append(temp).append(" and ");
        }
        // Remove last "and" if it exists.
        String output = sb.toString().replaceAll(" and $", "");
        return output;
    }

    /**************************************************************************************************************
     * Put the "direct object" of this process into natural language.
     * @return
     * @param roleScratchPad
     */
    String formulateNaturalDirectObject(Multimap<CaseRole, String> roleScratchPad) {
        StringBuilder sBuild = new StringBuilder();

        VerbProperties verbProperties = new VerbPropertiesSimpleImpl();
        String verb = sumoProcess.getVerb();

        List<CaseRole> caseRolesToUse = verbProperties.getCaseRolesForGrammarRole(verb, SVOGrammar.SVOGrammarPosition.DIRECT_OBJECT);
        for(CaseRole role : caseRolesToUse) {
            String obj = formulateNounPhraseForCaseRole(role, roleScratchPad);
            if (! obj.isEmpty()) {
                sBuild.append(" ").append(obj).append(" ");
                roleScratchPad.removeAll(role);
                break;
            }
        }

        return sBuild.toString().replaceAll("\\s+", " ").trim();
    }

    /**************************************************************************************************************
     * Put the "direct object" of this process into natural language.
     * @return
     * @param roleScratchPad
     */
    private String formulateNaturalIndirectObject(Multimap<CaseRole, String> roleScratchPad) {
        StringBuilder sBuild = new StringBuilder();

        VerbProperties verbProperties = new VerbPropertiesSimpleImpl();
        String verb = sumoProcess.getVerb();

        List<CaseRole> caseRolesToUse = verbProperties.getCaseRolesForGrammarRole(verb, SVOGrammar.SVOGrammarPosition.INDIRECT_OBJECT);
        for(CaseRole role : caseRolesToUse) {
            String obj = formulateNounPhraseForCaseRole(role, roleScratchPad);
            if (! obj.isEmpty()) {
                List<String> preps = verbProperties.getPrepositionForCaseRole(verb, role);
                // TODO: for time being, take just the first one in the list
                String prep = preps.get(0);
                sBuild.append(prep + " ").append(obj).append(" ");
                roleScratchPad.removeAll(role);
            }
        }

        return sBuild.toString().replaceAll("\\s+", " ").trim();
    }


    /**************************************************************************************************************
     * Put the subject of this process into natural language.
     * @return
     * @param roleScratchPad
     */
    String formulateNaturalSubject(Multimap<CaseRole, String> roleScratchPad) {
        StringBuilder sBuild = new StringBuilder();

        VerbProperties verbProperties = new VerbPropertiesSimpleImpl();
        String verb = sumoProcess.getVerb();

        List<CaseRole> caseRolesToUse = verbProperties.getCaseRolesForGrammarRole(verb, SVOGrammar.SVOGrammarPosition.SUBJECT);
        for(CaseRole role : caseRolesToUse) {
            String obj = formulateNounPhraseForCaseRole(role, roleScratchPad);
            if (! obj.isEmpty()) {
                sBuild.append(" ").append(obj).append(" ");
                roleScratchPad.removeAll(role);
                break;
            }
        }

        return sBuild.toString().replaceAll("\\s+", " ").trim();

    }

    /**************************************************************************************************************
     * Put the verb of this process into natural language.
     * @return
     */
    String formulateNaturalVerb() {
        return sumoProcess.formulateNaturalVerb(kb);
    }

}
