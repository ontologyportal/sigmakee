package com.articulate.sigma;

import org.junit.Test;

import java.util.*;

import static junit.framework.TestCase.assertTrue;

public class FormatTest extends IntegrationTestBase {

    /****************************************************************
     */
    @Test
    public void testNegativePositiveFormat() {

        Map<String, String> phraseMap = SigmaTestBase.kb.getFormatMap("EnglishLanguage");

        StringBuilder problems = new StringBuilder("\n");
        String value;
        for (Map.Entry<String, String> entry : phraseMap.entrySet())   {
            value = entry.getValue();
            if (value.matches(".*%(p|n)\\(.*"))  {
                problems.append("Syntax error. '").append(entry.getKey()).append("' has a p (pos) or n (neg) followed by a left paren instead of a left curly brace: '").append(value).append("'\n");
            }
            // Note use of ? for non-greedy matching.
            if (value.matches(".*%(p|n)\\{[^\\}]+?\\).*"))  {
                problems.append("Syntax error. '").append(entry.getKey()).append("' has a p (pos) or n (neg) followed by a right paren instead of a right curly brace: '").append(value).append("'\n");
            }
        }

        // If a trimmed problems list is not empty, then all the problems will be printed out.
        assertTrue(problems.toString(), problems.toString().trim().isEmpty());
    }

    /****************************************************************
     */
    @Test
    public void testFormatMatchingCharacters() {

        Map<String, String> phraseMap = SigmaTestBase.kb.getFormatMap("EnglishLanguage");

        StringBuilder problems = new StringBuilder("\n");
        String value;
        int leftParensCt, rightParensCt, leftCurlyCt, rightCurlyCt, leftSquareBracket, rightSquareBracket;
        for (Map.Entry<String, String> entry : phraseMap.entrySet()) {
            value = entry.getValue();
            leftParensCt = value.length() - value.replace("(", "").length();
            rightParensCt = value.length() - value.replace(")", "").length();
            if (leftParensCt != rightParensCt) {
                problems.append("Syntax error. '").append(entry.getKey()).append("' contains unmatching parentheses: '").append(value).append("'\n");
            }
            leftCurlyCt = value.length() - value.replace("{", "").length();
            rightCurlyCt = value.length() - value.replace("}", "").length();
            if (leftCurlyCt != rightCurlyCt) {
                problems.append("Syntax error. '").append(entry.getKey()).append("' contains unmatching curly braces: '").append(value).append("'\n");
            }
            leftSquareBracket = value.length() - value.replace("[", "").length();
            rightSquareBracket = value.length() - value.replace("]", "").length();
            if (leftSquareBracket != rightSquareBracket) {
                problems.append("Syntax error. '").append(entry.getKey()).append("' contains unmatching square brackets: '").append(value).append("'\n");
            }
        }

        // If a trimmed problems list is not empty, then all the problems will be printed out.
        assertTrue(problems.toString(), problems.toString().trim().isEmpty());
    }
}
