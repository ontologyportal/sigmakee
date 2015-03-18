package com.articulate.sigma.nlg;

import com.articulate.sigma.KB;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.util.List;
import java.util.Set;
import com.articulate.sigma.nlg.SVOElement.*;
import com.google.common.collect.Sets;

/**
 * A structure allowing one Subject-Verb-Object element in a sentence to "know" about the others.
 */
public class Sentence {
    private final KB kb;

    // TODO: consider having SVOElement and SumoProcess implement an interface; right now they all share one
    // method--getSurfaceForm( )
    private SVOElement subject = new SVOElement(SVOGrammarPosition.SUBJECT);

    private SumoProcess verb;

    private SVOElement directObject = new SVOElement(SVOGrammarPosition.DIRECT_OBJECT);

    private List<SVOElement> indirectObjects = Lists.newArrayList();

    private final Multimap<String, SumoProcessEntityProperty> entityProperties;

    /**
     * A list of the sentence's case roles.
     */
    private final Multimap<CaseRole, String> caseRoles;

    /**
     * A modifiable copy of the caseRoles object.
     */
    private Multimap<CaseRole, String> caseRolesScratchpad;


    /**************************************************************************************
     * Constructor.
     * @param roles
     * @param process
     * @param inKB
     * @param properties
     */
    public Sentence(Multimap<CaseRole, String> roles, SumoProcess process, KB inKB, Multimap<String, SumoProcessEntityProperty> properties)   {
        caseRoles = HashMultimap.create(roles);
        setCaseRolesScratchpad(roles);

        verb = new SumoProcess(process, inKB);

        this.kb = inKB;

        entityProperties = properties;
    }

    /**************************************************************************************
     * Get all the entities playing a given role in this process.
     * @param role
     * @param roles
     * @return
     */
    static Set<String> getRoleEntities(CaseRole role, Multimap<CaseRole, String> roles)   {
        return Sets.newTreeSet(roles.get(role));
    }


    /**************************************************************************************************************
     *
     * @return
     */
    public SVOElement getSubject() {
        return subject;
    }

    /**************************************************************************************************************
     *
     * @param subject
     */
    public void setSubject(SVOElement subject) {
        this.subject = subject;
    }

    /**************************************************************************************************************
     *
     * @return
     */
    public SumoProcess getVerb() {
        return verb;
    }

    /**************************************************************************************************************
     *
     * @param verb
     */
    public void setVerb(SumoProcess verb) {
        this.verb = verb;
    }

    /**************************************************************************************************************
     *
     * @return
     */
    public SVOElement getDirectObject() {
        return directObject;
    }

    /**************************************************************************************************************
     *
     * @param directObject
     */
    public void setDirectObject(SVOElement directObject) {
        this.directObject = directObject;
    }

    /**************************************************************************************************************
     *
     * @return
     */
    public List<SVOElement> getIndirectObjects() {
        return indirectObjects;
    }

    /**************************************************************************************************************
     *
     * @param indirectObjects
     */
    public void setIndirectObjects(List<SVOElement> indirectObjects) {
        this.indirectObjects = indirectObjects;
    }

    /**************************************************************************************************************
     * Return the case roles scratchpad.
     * @return
     */
    Multimap<CaseRole, String> getCaseRolesScratchpad() {
        return caseRolesScratchpad;
    }

    /**************************************************************************************************************
     * Set the case roles scratch pad with the given case roles.
     * @param roles
     */
    public void setCaseRolesScratchpad(Multimap<CaseRole, String> roles) {
        this.caseRolesScratchpad = HashMultimap.create(roles);
    }

    /**************************************************************************************************************
     * Attempt to perform natural language generation on this object.
     * @return
     *   a sentence in natural language, or empty string on failure
     */
    public String toNaturalLanguage() {

        formulateNaturalSubject();
        formulateNaturalVerb();
        formulateNaturalDirectObject();
        formulateNaturalIndirectObject();

        if (subject.getSurfaceForm().isEmpty())   {
            reset();
            doPassiveVoice();
        }

        // If no subject, perform no NLG.
        if (subject.getSurfaceForm().isEmpty())   {
            return "";
        }

        String indirectObjectStr = concatenateIndirectObjects();
        String cleanedStr = (subject.getSurfaceForm() + " " + verb.getSurfaceForm() + " " +
                directObject.getSurfaceForm() + " " + indirectObjectStr);

        return cleanedStr.replaceAll("\\s+", " ").trim();
    }

    /**************************************************************************************************************
     * Reset all fields so that we can retry natural language generation.
     */
    private void reset() {
        subject = new SVOElement(SVOGrammarPosition.SUBJECT);
        verb = new SumoProcess(verb, kb);
        directObject = new SVOElement(SVOGrammarPosition.DIRECT_OBJECT);
        indirectObjects = Lists.newArrayList();
        setCaseRolesScratchpad(caseRoles);
    }

    /**************************************************************************************************************
     *
     */
    private String concatenateIndirectObjects() {
        StringBuilder sBuild = new StringBuilder();

        for(SVOElement element : indirectObjects)   {
            sBuild.append(element.getSurfaceForm()).append(" ");
        }

        return sBuild.toString().trim();
    }

    /**************************************************************************************************************
     * Put the "indirect object" of this process into natural language.
     */
    void formulateNaturalIndirectObject() {
        VerbProperties verbProperties = new VerbPropertiesSimpleImpl();
        String verb = getVerb().getVerb();
        List<CaseRole> caseRolesToUse = verbProperties.getCaseRolesForGrammarRole(verb, SVOElement.SVOGrammarPosition.INDIRECT_OBJECT);

        for (CaseRole role : caseRolesToUse) {
            SVOElement element = new SVOElement(SVOGrammarPosition.INDIRECT_OBJECT);
            String obj = formulateNounPhraseForCaseRole(role, element, kb);
            if (! obj.isEmpty()) {
                List<String> preps = verbProperties.getPrepositionForCaseRole(verb, role);
                // TODO: for time being, take just the first one in the list
                String prep = preps.get(0);
                element.setSurfaceForm(prep + " " + obj + " ");
                indirectObjects.add(element);
                getCaseRolesScratchpad().removeAll(role);
            }
        }
    }


    /**************************************************************************************************************
     * Put the "direct object" of this process into natural language.
     */
    void formulateNaturalDirectObject() {
        StringBuilder sBuild = new StringBuilder();

        VerbProperties verbProperties = new VerbPropertiesSimpleImpl();
        List<CaseRole> caseRolesToUse = verbProperties.getCaseRolesForGrammarRole(getVerb().getVerb(), SVOElement.SVOGrammarPosition.DIRECT_OBJECT);

        // It's possible that the direct object slot has already been filled, e.g. by the verb if we didn't know how
        // to translate the process into natural language. Since only one direct object is allowed, in this case we need to
        // precede any objects formulated here with a prefix, and add the data to the indirect object list.
        String prefix = "";
        SVOElement element = directObject;
        if (! directObject.getSurfaceForm().isEmpty())  {
            prefix = "on ";
            element = new SVOElement(SVOGrammarPosition.INDIRECT_OBJECT);
            indirectObjects.add(element);
        }

        for (CaseRole role : caseRolesToUse) {
            String obj = formulateNounPhraseForCaseRole(role, directObject, kb);
            if (! obj.isEmpty()) {
                sBuild.append(prefix).append(" ").append(obj).append(" ");
                getCaseRolesScratchpad().removeAll(role);
                break;
            }
        }

        element.setSurfaceForm(sBuild.toString().replaceAll("\\s+", " ").trim());
    }

    /**************************************************************************************************************
     *
     */
    void formulateNaturalVerb() {
        verb.formulateNaturalVerb(this);
    }


    /**************************************************************************************************************
     * Put the subject of this process into natural language.
     */
    void formulateNaturalSubject( ) {
        StringBuilder sBuild = new StringBuilder();

        VerbProperties verbProperties = new VerbPropertiesSimpleImpl();
        List<CaseRole> caseRolesToUse = verbProperties.getCaseRolesForGrammarRole(getVerb().getVerb(), SVOElement.SVOGrammarPosition.SUBJECT);

        for (CaseRole role : caseRolesToUse) {
            String obj = formulateNounPhraseForCaseRole(role, subject, kb);
            if (! obj.isEmpty()) {
                sBuild.append(" ").append(obj).append(" ");
                getCaseRolesScratchpad().removeAll(role);
                break;
            }
        }

        subject.setSurfaceForm(sBuild.toString().replaceAll("\\s+", " ").trim());
    }

    /**************************************************************************************************************
     * Return all the entities of the given role in the correct case.
     * Assumes the role will be some kind of noun.
     * @param role
     * @param element
     * @param kb
     * @return
     */
    private String formulateNounPhraseForCaseRole(CaseRole role, SVOElement element, KB kb) {
        if (! getCaseRolesScratchpad().containsKey(role))    {
            return "";
        }
        Set<String> rawNouns = Sentence.getRoleEntities(role, getCaseRolesScratchpad());

        List<String> fixedNouns = Lists.newArrayList();
        // We're assuming that only names and reified objects are in uppercase.
        for (String noun : rawNouns) {
            String temp = noun;
            if (! NLGStringUtils.isVariable(noun)) {
                temp = addProperties(noun);
                if  (Noun.takesIndefiniteArticle(noun, kb)) {
                    temp = Noun.aOrAn(temp) + " " + temp;
                }
                // Replace the noun with its SUMO representation if it has one.
                String kbStr = SumoProcessCollector.getProperFormOfEntity(noun, kb);
                temp = temp.replaceAll(noun, kbStr);
            }
            fixedNouns.add(temp);
            element.addConsumedCaseRole(role);
        }

        return NLGStringUtils.concatenateWithCommas(fixedNouns);
    }

    /**************************************************************************************************************
     * Add properties like adjectives to the given noun.
     * @param noun
     * @return
     */
    private String addProperties(String noun) {
        if (entityProperties.isEmpty() || entityProperties.get(noun).isEmpty()) {
            return noun;
        }

        String retVal = noun;
        for (SumoProcessEntityProperty prop : entityProperties.get(noun))   {
            retVal = prop.getSurfaceFormForNoun(retVal, kb);
        }
        return retVal;
    }

    /**************************************************************************************************************
     * If we haven't managed to create a subject, try creating one with Patient. The result will be "experiences" +
     * the process verb in a noun form. This can be seen as a precursor of correct passive voice.
     *
     */
    private void doPassiveVoice() {
        List<CaseRole> caseRolesToUse = Lists.newArrayList(CaseRole.EXPERIENCER, CaseRole.MOVES, CaseRole.PATIENT, CaseRole.RESOURCE, CaseRole.ATTENDS);

        // Set subject.
        StringBuilder sBuild = new StringBuilder();
        for (CaseRole role : caseRolesToUse) {
            String obj = formulateNounPhraseForCaseRole(role, subject, kb);
            if (! obj.isEmpty()) {
                sBuild.append(" ").append(obj).append(" ");
                getCaseRolesScratchpad().removeAll(role);
                break;
            }
        }

        subject.setSurfaceForm(sBuild.toString().replaceAll("\\s+", " ").trim());

        // Set verb to "experiences" and the direct object to a noun form of the process.
        verb.setVerbAndDirectObject(this);
    }

}
