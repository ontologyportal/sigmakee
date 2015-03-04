package com.articulate.sigma.nlg;

import com.articulate.sigma.KB;

/**
 * Provides functionality related to nouns and NLG.
 */

public class Noun {
    /**************************************************************************************************************
     * Look at first letter of input to determine whether it should be preceded by "a" or "an".
     * @param temp
     * @return
     * "a" or "an"
     */
    public static String aOrAn(String temp) {
        String article;
        if(!NLGStringUtils.isVowel(temp.charAt(0)))   {
            article = "a";
        }
        else    {
            article = "an";
        }

        return article;
    }

    /**************************************************************************************************************
     * Determine whether the given noun requires an indefinite article ("a"/"an").
     * @param noun
     * @param kb
     * @return
     */
    public static boolean takesIndefiniteArticle(String noun, KB kb)  {
        // Return false if capitalized.
        if (noun.substring(0,1).matches("[A-Z]")) {
            return false;
        }

        // Return false if Entity.
        if (noun.equals("Entity"))   {
            return false;
        }

        // Capitalize so that the lookup below works.
        String temp = noun.substring(0, 1).toUpperCase() + noun.substring(1);
        return ! kb.isSubclass(temp, "Substance");
    }
}
