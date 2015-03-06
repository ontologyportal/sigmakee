package com.articulate.sigma.nlg;

import com.articulate.sigma.KB;
import com.google.common.collect.*;

import java.util.Map;

/**
 * This object represents a process or an event, holding information on its case roles as
 * well as the entities which play that role.
 */
public class SumoProcessCollector {
    private SumoProcess sumoProcess;

    // Use of TreeMultimap ensures iteration is predetermined.
    private final Multimap<CaseRole, String> roles = TreeMultimap.create();

    private KB kb;

    /**************************************************************************************
     * Construct a SumoProcessCollector.
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

        sumoProcess = new SumoProcess(process, kb);
        //name = process;

        CaseRole caseRole = CaseRole.toCaseRole(role);
        roles.put(caseRole, entity);
    }


    /**************************************************************************************
     *
     * @return
     */
    public SumoProcess getSumoProcess() {
        return sumoProcess;
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
        Sentence sentence = new Sentence(createNewRoleScratchPad(), sumoProcess, kb);
        return sentence.toNaturalLanguage();
    }

}
