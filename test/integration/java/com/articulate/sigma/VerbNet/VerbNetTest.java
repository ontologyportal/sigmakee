package com.articulate.sigma.VerbNet;

import com.articulate.sigma.*;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.wordNet.*;

import java.util.*;

import org.junit.Test;
import static org.junit.Assert.*;

public class VerbNetTest extends IntegrationTestBase {

    /** *************************************************************
     */
    @Test
    public void testTerm() {

        String term = "SocialInteraction";
        Map<String,String> tm = WordNet.wn.getWordsFromTerm(term);
        System.out.println("testTerm(): words: " + tm);
        String verbs = VerbNet.formatVerbs(tm);
        System.out.println("testTerm(): verbs: " + verbs);
        assertTrue(!StringUtil.emptyString(verbs));
    }

    /** *************************************************************
     */
    @Test
    public void testWordList() {

        Map<String, List<String>> tm = WordNet.wn.getSenseKeysFromWord("object");
        System.out.println("testWordList(): senses: " + tm);
        String verbs = VerbNet.formatVerbsList(tm);
        System.out.println("testWordList(): verbs: " + verbs);
        assertTrue(!StringUtil.emptyString(verbs));
    }
}