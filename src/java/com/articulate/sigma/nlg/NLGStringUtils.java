package com.articulate.sigma.nlg;

import java.util.List;

/**
 * String utilities related to NLG.
 */
public class NLGStringUtils {

    /******************************************************************
     * Assemble the input list together into a single string with "and" separating the penultimate and the last
     * item, and commas separating the other items.
     * @param input
     * @return
     */
    static String concatenateWithCommas(List<String> input) {
        if (input == null || input.isEmpty())   {
            return "";
        }

        StringBuilder sBuilder = new StringBuilder();
        for (int ct = 0; ct < input.size(); ct++)    {
            if (ct == input.size() - 1)  {
                // Last item in list.
                sBuilder.append(input.get(ct));
            }
            else if (ct == input.size() - 2)  {
                // Last item in list.
                sBuilder.append(input.get(ct)).append(" and ");
            }
            else {
                sBuilder.append(input.get(ct)).append(", ");
            }
        }

        return sBuilder.toString();
    }

    /*****************************************************************
     * Stolen from WordNetUtilities and put here so that some unit tests can work without WordNet loaded.
     * @param c
     * @return
     */
    static boolean isVowel(char c) {

        if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u')
            return true;
        else
            return false;
    }

    /**************************************************************************************************************
     * Is input string a variable (i.e., does it start with a question mark)?
     * @param str
     * @return
     */
    public static boolean isVariable(String str) {
        if(str.substring(0, 1).equals("?")) {
            return true;
        }
        return false;
    }
}
