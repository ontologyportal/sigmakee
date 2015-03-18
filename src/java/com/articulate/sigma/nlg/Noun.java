package com.articulate.sigma.nlg;

import com.articulate.sigma.KB;

/**
 * Provides functionality related to nouns and NLG.
 */

public class Noun {


    /**************************************************************************************************************
     * Look at first letter of input to determine whether it should be preceded by "a" or "an".
     * @param input
     * @return
     * "a" or "an"
     */
    public static String aOrAn(String input) {
        String article;
        String temp = input.toLowerCase();
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
        // Return false if Entity.
        if (noun.equals("Entity"))   {
            return false;
        }

        if (kb.isSubclass(noun, "Entity")) {
            return !kb.isSubclass(noun, "Substance");
        }
        return false;
    }
}
