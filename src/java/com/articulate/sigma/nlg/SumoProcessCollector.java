package com.articulate.sigma.nlg;

import com.articulate.sigma.KB;
import com.google.common.collect.*;

import java.util.Map;

import static com.articulate.sigma.nlg.VerbProperties.*;

/**
 * This object represents a process or an event, holding information on its case roles as
 * well as the entities which play that role.
 */
public class SumoProcessCollector {

    private SumoProcess sumoProcess;

    private VerbProperties.Polarity polarity = Polarity.AFFIRMATIVE;

    // Use of TreeMultimap ensures iteration is predetermined.
    private final Multimap<CaseRole, String> roles = TreeMultimap.create();

    private KB kb;

    private Multimap<String, SumoProcessEntityProperty> entityProperties = TreeMultimap.create();

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

//        entity = SumoProcessCollector.getProperFormOfEntity(entity, kb);

        sumoProcess = new SumoProcess(process, kb);
        //name = process;

        CaseRole caseRole = CaseRole.toCaseRole(role, kb);
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
     * Getter and setter for polarity field.
     *
     */
    public Polarity getPolarity() {
        return polarity;
    }

    public void setPolarity(Polarity polarity) {
        this.polarity = polarity;
        this.sumoProcess.setPolarity(polarity);
    }

    /**************************************************************************************
     * Add a new role/entity pair to this event.
     * @param role
     * @param arg
     */
    public void addRole(String role, String arg) {
        String msg = "role = " + role + "; entity = " + arg + ".";
        if (! kb.kbCache.isInstanceOf(role, "CaseRole")) {
            throw new IllegalArgumentException("Invalid role: " + msg);
        }
        CaseRole caseRole = CaseRole.toCaseRole(role, kb);
        roles.put(caseRole, arg);
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
    public static String getProperFormOfEntity(String entity, KB kb) {

        // FIXME: instances of human and geographical area/region are capitalized; also holidays/weekdays/months/centuries
        String temp = entity;
        if (kb.isSubclass(entity, "Entity"))    {
            if (kb.getTermFormatMap("EnglishLanguage").containsKey(entity))     {
                temp = kb.getTermFormatMap("EnglishLanguage").get(entity);
            }
            else    {
                temp = temp.toLowerCase();
            }
        }
        return temp;
    }


    /**************************************************************************************************************
     * Write out a string representing all the fields in this object.
     * @return
     */
    @Override
    public String toString()  {
        // FIXME: current implementation doesn't indicate polarity
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
    public String toNaturalLanguage( )   {
        Sentence sentence = new Sentence(createNewRoleScratchPad(), sumoProcess, kb, entityProperties);
        return sentence.toNaturalLanguage();
    }

    /**************************************************************************************************************
     * Translate this process and its case roles into natural language.
     * @return
     *  a natural language translation, or empty string if one is not possible
     * @param properties
     */

    public void setEntityProperties(Multimap<String, SumoProcessEntityProperty> properties)   {
        entityProperties = properties;
    }

    /**************************************************************************************************************
     * Merge the roles of the given SumoProcessCollector into this object.
     * If new SumoProcessCollector's polarity is Negative, set this object's to the same.
     * @param newProcessCollector
     */
    public void merge(SumoProcessCollector newProcessCollector) {
        String thisVerb = this.sumoProcess.getVerb();
        String newVerb = newProcessCollector.getSumoProcess().getVerb();

        if (! thisVerb.equals(newVerb))   {
            String msg = "Cannot merge because the objects do not have identical processes: process1 = " +
                    thisVerb + "; process2 = " + newVerb;
            throw new IllegalArgumentException(msg);
        }

        roles.putAll(newProcessCollector.roles);

        // If either Collector has a Negative polarity, the merged one should as well.
        if (newProcessCollector.getPolarity().equals(Polarity.NEGATIVE))    {
            this.setPolarity(Polarity.NEGATIVE);
        }
    }
}
