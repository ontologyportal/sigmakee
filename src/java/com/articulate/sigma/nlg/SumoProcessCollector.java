package com.articulate.sigma.nlg;

import com.articulate.sigma.KB;
import com.articulate.sigma.WordNet;
import com.articulate.sigma.WordNetUtilities;
import com.google.common.collect.*;

import java.util.Map;
import java.util.Set;

/**
 * This object represents a process or an event, holding information on its case roles as
 * well as the entities which play that role.
 */
public class SumoProcessCollector {
    // FIXME: Don't list each CaseRole--isKnownRole( ) should use subrelation ("(subrelation plaintiff agent)").
    private static final Set<String> knownRoles = Sets.newHashSet("agent", "plaintiff", "patient", "destination", "instrument");

    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    // Use of TreeMultimap ensures iteration is predictable.
    private final Multimap<String, String> roles = TreeMultimap.create();

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

        name = process;
        roles.put(role, entity);
    }


    /**************************************************************************************
     * Add a new role/entity pair to this event.
     * @param role
     * @param arg
     */
    public void addRole(String role, String arg) {
        String entity = checkEntityCase(arg);

        roles.put(role, entity);
    }


    /**************************************************************************************
     * Get all the entities playing a given role in this process.
     * @param role
     * @return
     */
    public Set<String> getRoleEntities(String role)   {
        return Sets.newTreeSet(roles.get(role));
    }


    /**************************************************************************************
     * Get all the roles and their entities for this event.
     * @return
     */
    public Multimap<String, String> getRolesAndEntities()   {
        Multimap<String, String> all = TreeMultimap.create();
        all.putAll(roles);
        return all;

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


    /** ***************************************************************
     * Stolen from WordNetUtilities and put here so that some unit tests can work without WordNet loaded.
     */
    private static boolean isVowel(char c) {

        if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u')
            return true;
        else
            return false;
    }

    /**************************************************************************************************************
     * Write out a string representing all the fields in this object.
     * @return
     */
    @Override
    public String toString()  {
        StringBuilder sb = new StringBuilder();

        for(Map.Entry<String, String> mapPair : roles.entries())  {
            String key = mapPair.getKey();
            String value = mapPair.getValue();

            sb.append(key.toLowerCase()).append(" ").append(name).append(" ").append(value).append("\n");
        }

        return sb.toString();
    }


    /**************************************************************************************************************
     * Translate this process and its case roles into natural language.
     * @return
     *  a natural language translation, or empty string if one is not possible
     */
    public String toNaturalLanguage()   {
        if(! isValid())     {
            return "";
        }

        String cleanedStr = (formulateNaturalSubject() + " " + formulateNaturalVerb() + " " + formulateNaturalDirectObject() + " " + formulateNaturalIndirectObject()).trim();

        return cleanedStr;
    }

    /**************************************************************************************************************
     * Verify that this process is in a state that can be translated into natural language.
     * @return
     */
    boolean isValid() {
        // Verify there is an agent.
        // FIXME: Don't list each CaseRole--use subrelation ("(subrelation plaintiff agent)").
        if (getRoleEntities("agent").isEmpty() && getRoleEntities("plaintiff").isEmpty())    {
//      if (getRoleEntities("agent").isEmpty())    {
            return false;
        }

        // FIXME: ? Make sure no case roles have unresolved variables as the entity

        return true;
    }


    /**************************************************************************************************************
     * Return all the entities of the given role in the correct case.
     * Assumes the role will be some kind of noun.
     * @return
     */
    String formulateNaturalCaseRoleNouns(String role)    {
        if (! roles.containsKey(role))    {
            // FIXME: Find some alternate for the subject.
            return "";
        }
        // FIXME: assuming only one agent
        Set<String> nouns = getRoleEntities(role);

        StringBuilder sb = new StringBuilder();
        // We're assuming that only names and reified objects are in uppercase.
        for(String noun : nouns) {
            String temp = noun;
            if (! isVariable(temp) && takesIndefiniteArticle(noun)) {
                temp = aOrAn(temp) + " " + temp;
            }
            sb.append(temp).append(" and ");
        }
        // Remove last "and" if it exists.
        String output = sb.toString().replaceAll(" and $", "");
        return output;
    }

    /**************************************************************************************************************
     *
     * @param str
     * @return
     */
    private boolean isVariable(String str) {
        if(str.substring(0, 1).equals("?")) {
            return true;
        }
        return false;
    }

    /**************************************************************************************************************
     * Look at first letter of input to determine whether it should be preceded by "a" or "an".
     * @param temp
     * @return
     * "a" or "an"
     */
    private String aOrAn(String temp) {
        String article;
        if(!SumoProcessCollector.isVowel(temp.charAt(0)))   {
            article = "a";
        }
        else    {
            article = "an";
        }

        return article;
    }

    /**************************************************************************************************************
     * Determine whether the given noun requires an indefinite article ("a"/"an").
     * @return
     */
    boolean takesIndefiniteArticle(String noun)  {
        //return kb.isSubclass(noun, "CorpuscularObject");

        // Return false if capitalized.
        if (noun.substring(0,1).matches("[A-Z]")) {
            return false;
        }

        // Return false if Entity.
        // TODO: I doubt this is a perfect solution; LanguageFormatter might instead have to replace both "entity" and "an entity" with "the entity".
        if (noun.equals("Entity"))   {
            return false;
        }

        // Capitalize so that the lookup below works.
        String temp = noun.substring(0, 1).toUpperCase() + noun.substring(1);
        return ! kb.isSubclass(temp, "Substance");
    }

    /**************************************************************************************************************
     * Put the "direct object" of this process into natural language.
     * @return
     */
    String formulateNaturalDirectObject() {
        // FIXME: If empty, find some alternate for the dir obj.
        return formulateNaturalCaseRoleNouns("patient");
    }

    /**************************************************************************************************************
     * Put the "direct object" of this process into natural language.
     * @return
     */
    private String formulateNaturalIndirectObject() {
        StringBuilder indirObj = new StringBuilder();
        String destinationObj = formulateNaturalCaseRoleNouns("destination");
        if (! destinationObj.isEmpty()) {
            indirObj.append("to ").append(destinationObj).append(" ");
        }
        String instrumentObj = formulateNaturalCaseRoleNouns("instrument");
        if (! instrumentObj.isEmpty()) {
            return "with " + instrumentObj;
        }
        String retVal = indirObj.toString().replaceAll("\\s+", " ");
        return retVal;
    }


    /**************************************************************************************************************
     * Put the subject of this process into natural language.
     * @return
     */
    String formulateNaturalSubject() {
        // FIXME: If empty, find some alternate for the subject, e.g. experiencer.
        // FIXME: Don't list each CaseRole--use subrelation ("(subrelation plaintiff agent)").
        String retVal = formulateNaturalCaseRoleNouns("agent");
        if(retVal.isEmpty())    {
            retVal = formulateNaturalCaseRoleNouns("plaintiff");
        }
        return retVal;
    }

    /**************************************************************************************************************
     * Put the verb of this process into natural language.
     * @return
     */
    String formulateNaturalVerb() {
        String verb = getVerbRootForm();

        if (verb == null || verb.isEmpty())  {
            verb = getNounFormOfVerb(name);
            //verb = "performs " + noun;
            return verb;
        }

        // FIXME: verbPlural is a misnomer; it finds the simple present singular form
        verb = WordNetUtilities.verbPlural(verb);
        return verb;
    }


    /**************************************************************************************************************
     * For a process which does not have a language representation, get a reasonable way of paraphrasing it.
     * @return
     */
    private String getNounFormOfVerb(String verb) {
        String phrase = "performs ";
        // Turn, e.g. "IntentionalProcess" into "intentional process".
        String formattedTerm = kb.getTermFormatMap("EnglishLanguage").get(verb);

        if (formattedTerm != null && ! formattedTerm.isEmpty()) {
            if (!kb.isSubclass(verb, "Substance")) {
                String article = aOrAn(formattedTerm);
                phrase = phrase + article + " ";
            }
            phrase = phrase + formattedTerm;
        }
        else    {
            phrase = phrase + "a " + name.toLowerCase();
        }

        return phrase;
    }

    /**************************************************************************************************************
     * Get the root of the given verb.
     * @return
     */
    String getVerbRootForm() {

        return WordNet.wn.verbRootForm(name, name.toLowerCase());
    }


    /**
     * Is the given role "known"? That is, do we know how to generate natural language for it?
     * @param role
     * @return
     */
    public static boolean isKnownRole(String role) {
        // Do not generate if there are any roles we aren't yet equipped to handle.
        if (SumoProcessCollector.knownRoles.contains(role))    {
                return true;
        }
        return false;
    }
}
