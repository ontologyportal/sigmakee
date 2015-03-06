package com.articulate.sigma.nlg;

import com.articulate.sigma.KB;
import com.articulate.sigma.WordNet;
import com.articulate.sigma.WordNetUtilities;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Sets;

/**
 * Handles verb functionality for a SUMO process.
 */
public class SumoProcess {
    private final String verb;
    private final KB kb;

    public SumoProcess(String verb, KB kb) {
        this.verb = verb;
        this.kb = kb;
    }

    private String surfaceForm;

    /**************************************************************************************************************
     * Indirectly invoked by SumoProcessCollector.toString( ).
     * @return
     */
    @Override
    public String toString()    {
        return verb;
    }

    /**************************************************************************************************************
     *
     * @return
     */
    public String getSurfaceForm() {
        return surfaceForm;
    }

    /**************************************************************************************************************
     *
     * @param surfaceForm
     */
    void setSurfaceForm(String surfaceForm) {
        this.surfaceForm = surfaceForm;
    }

    /**************************************************************************************************************
     * Try to phrase the verb into natural language, setting this object's internal state appropriately.
     * @param sentence
     */
    public void formulateNaturalVerb(Sentence sentence) {
        String verbSurfaceForm = getVerbRootForm(this.verb);

        if (verbSurfaceForm == null || verbSurfaceForm.isEmpty())  {
            setVerbAndDirectObject(this.verb, kb, sentence);
        }
        else {
            if(sentence.getSubject().getSingularPlural().equals(SVOElement.NUMBER.SINGULAR)) {
                verbSurfaceForm = verbRootToThirdPersonSingular(verbSurfaceForm);

            }
            setSurfaceForm(verbSurfaceForm);
        }
    }

    /**************************************************************************************************************
     * For a process which does not have a language representation, get a reasonable way of paraphrasing it.
     * Sets both verb and direct object of the sentence.
     */
    void setVerbAndDirectObject(String verb, KB kb, Sentence sentence) {
        // First set the verb, depending on the subject's case role and number.
        String surfaceForm = "experience";

        Multiset<CaseRole> experienceSubjectCaseRoles = HashMultiset.create(Sets.newHashSet(CaseRole.AGENT));
        if(! Multisets.intersection(sentence.getSubject().getConsumedCaseRoles(), experienceSubjectCaseRoles).isEmpty())    {
            surfaceForm = "perform";
        }

        if (sentence.getSubject().getSingularPlural().equals(SVOElement.NUMBER.SINGULAR))    {
            surfaceForm = surfaceForm + "s";
        }
        setSurfaceForm(surfaceForm);

        // Now determine and set the direct object (which may end up displacing what would otherwise have been a
        // direct object for this sentence).
        // Turn, e.g. "IntentionalProcess" into "intentional process".
        String formattedTerm = kb.getTermFormatMap("EnglishLanguage").get(verb);

        String phrase = "";
        if (formattedTerm != null && ! formattedTerm.isEmpty()) {
            if (!kb.isSubclass(verb, "Substance")) {
                String article = Noun.aOrAn(formattedTerm);
                phrase = phrase + article + " ";
            }
            phrase = phrase + formattedTerm;
        }
        else    {
            phrase = phrase + "a " + this.verb.toLowerCase();
        }

        sentence.getDirectObject().setSurfaceForm(phrase);
        sentence.getDirectObject().addConsumedCaseRole(CaseRole.PATIENT);
    }

    /**************************************************************************************************************
     *
     * @return
     */
    public String getVerb() {
        return verb;
    }

    /**************************************************************************************************************
     *
     * @param verbRoot
     * @return
     */
    public static String verbRootToThirdPersonSingular(String verbRoot) {
        // FIXME: verbPlural is a misnomer; it finds the simple present singular form
        return WordNetUtilities.verbPlural(verbRoot);
    }

    /**************************************************************************************************************
     * Get the root of the given verb.
     * @param gerund
     *   the verb in gerund (-ing) form.
     * @return
     *   the root of the given verb, or null if not found
     */
    public static String getVerbRootForm(String gerund) {
        return WordNet.wn.verbRootForm(gerund, gerund.toLowerCase());
    }

}
