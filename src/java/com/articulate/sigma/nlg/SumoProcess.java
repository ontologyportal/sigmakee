package com.articulate.sigma.nlg;

import com.articulate.sigma.KB;
import com.articulate.sigma.wordNet.WordNet;
import com.articulate.sigma.wordNet.WordNetUtilities;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Sets;

/**
 * Handles verb functionality for a SUMO process.
 */
public class SumoProcess {
    private final String verb;

    private VerbProperties.Polarity polarity = VerbProperties.Polarity.AFFIRMATIVE;

    private final KB kb;

    public SumoProcess(SumoProcess process, KB kb) {
        this.verb = process.getVerb();
        this.polarity = process.getPolarity();
        this.kb = kb;
    }

    private String surfaceForm;

    public SumoProcess(String verb, KB kb) {
        this.verb = verb;
        this.kb = kb;
    }

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
        String verbSurfaceForm = SumoProcess.getVerbRootForm(this.verb);

        if (verbSurfaceForm == null || verbSurfaceForm.isEmpty())  {
            setVerbAndDirectObject(sentence);
        }
        else {
            // Prefix "doesn't" or "don't" for negatives.
            String polarityPrefix = SumoProcess.getPolarityPrefix(this.polarity, sentence.getSubject().getSingularPlural());

            if (sentence.getSubject().getSingularPlural().equals(SVOElement.NUMBER.SINGULAR) &&
                    this.polarity.equals(VerbProperties.Polarity.AFFIRMATIVE)) {
                verbSurfaceForm = SumoProcess.verbRootToThirdPersonSingular(verbSurfaceForm);
            }

            setSurfaceForm(polarityPrefix + verbSurfaceForm);
        }
    }

    /**************************************************************************************************************
     * Return the correct prefix for negative sentences--"don't" or "doesn't". For affirmatives return empty string.
     * @param polarity
     * @param singularPlural
     * @return
     */
    private static String getPolarityPrefix(VerbProperties.Polarity polarity, SVOElement.NUMBER singularPlural) {
        if (polarity.equals(VerbProperties.Polarity.AFFIRMATIVE)) {
            return "";
        }

        // Singular negative.
        if (singularPlural.equals(SVOElement.NUMBER.SINGULAR)) {
            return "doesn't ";
        }

        // Plural negative
        return "don't ";
    }

    /**************************************************************************************************************
     * For a process which does not have a language representation, get a reasonable way of paraphrasing it.
     * Sets both verb and direct object of the sentence.
     */
    void setVerbAndDirectObject(Sentence sentence) {
        // Prefix "doesn't" or "don't" for negatives.
        String polarityPrefix = SumoProcess.getPolarityPrefix(this.polarity, sentence.getSubject().getSingularPlural());

        // Set the verb, depending on the subject's case role and number.
        String surfaceForm = "experience";

        Multiset<CaseRole> experienceSubjectCaseRoles = HashMultiset.create(Sets.newHashSet(CaseRole.AGENT));
        if(! Multisets.intersection(sentence.getSubject().getConsumedCaseRoles(), experienceSubjectCaseRoles).isEmpty())    {
            surfaceForm = "perform";
        }

        if (sentence.getSubject().getSingularPlural().equals(SVOElement.NUMBER.SINGULAR) &&
                this.getPolarity().equals(VerbProperties.Polarity.AFFIRMATIVE))    {
            surfaceForm = surfaceForm + "s";
        }
        setSurfaceForm(polarityPrefix + surfaceForm);

        // Now determine and set the direct object (which may end up displacing what would otherwise have been a
        // direct object for this sentence).
        // Turn, e.g. "IntentionalProcess" into "intentional process".
        String formattedTerm = this.kb.getTermFormatMap("EnglishLanguage").get(this.verb);

        String phrase = "";
        if (formattedTerm != null && ! formattedTerm.isEmpty()) {
            if (!kb.isSubclass(this.verb, "Substance")) {
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

    /**************************************************************************************
     * Getter and setter for polarity field.
     *
     */
    VerbProperties.Polarity getPolarity() {
        return polarity;
    }

    public void setPolarity(VerbProperties.Polarity polarity) {
        this.polarity = polarity;
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
