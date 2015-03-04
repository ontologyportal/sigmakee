package com.articulate.sigma.nlg;

import com.articulate.sigma.KB;
import com.articulate.sigma.WordNet;
import com.articulate.sigma.WordNetUtilities;

import java.util.Collection;

/**
 * Handles verb functionality for a SUMO process.
 */
public class SumoProcess {
    private final String verb;

    public SumoProcess(String verb) {
        this.verb = verb;
    }

    @Override
    public String toString()    {
        return verb;
    }

    String formulateNaturalVerb(KB kb) {
        String verb = getVerbRootForm();

        if (verb == null || verb.isEmpty())  {
            verb = getNounFormOfVerb(this.verb, kb);
            return verb;
        }

        // FIXME: verbPlural is a misnomer; it finds the simple present singular form
        verb = WordNetUtilities.verbPlural(verb);
        return verb;
    }

    /**************************************************************************************************************
     * Get the root of the given verb.
     * @return
     */
    String getVerbRootForm() {

        return WordNet.wn.verbRootForm(verb, verb.toLowerCase());
    }

    /**************************************************************************************************************
     * For a process which does not have a language representation, get a reasonable way of paraphrasing it.
     * @return
     */
    private String getNounFormOfVerb(String verb, KB kb) {
        String phrase = "performs ";
        // Turn, e.g. "IntentionalProcess" into "intentional process".
        String formattedTerm = kb.getTermFormatMap("EnglishLanguage").get(verb);

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

        return phrase;
    }

    public String getVerb() {
        return verb;
    }

    /**
     * Return true if the given list includes "Process", or if one of its elements is a subclass of Process.
     * @param vals
     * @param kb
     * @return
     */
    public static boolean containsProcess(Collection<String> vals, KB kb) {
        for (String val : vals)  {
            if (val.equals("Process") || kb.isSubclass(val, "Process"))  {
                return true;
            }
        }
        return false;
    }
}
