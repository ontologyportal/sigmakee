package com.articulate.sigma;

import com.articulate.sigma.nlg.NLGUtils;
import com.articulate.sigma.wordNet.WordNet;

import com.google.common.collect.Lists;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class SigmaTestBase {

    static final String SIGMA_HOME = System.getenv("SIGMA_HOME");
    protected static final String KB_PATH = (new File(SIGMA_HOME, "KBs")).getAbsolutePath();

    protected static KB kb;

    /***************************************************************
     * */
    protected static void checkConfiguration() {

        List<String> problemList = Lists.newArrayList();
        if (kb == null) {
            problemList.add("KBmanager returned no default KB.");
        }
        if (NLGUtils.getKeywordMap() == null || NLGUtils.getKeywordMap().isEmpty()) {
            problemList.add("LanguageFormatter.keywordMap is empty.");
        }
        if (WordNet.wn == null || WordNet.wn.synsetsToWords.isEmpty()) {
            problemList.add("WordNet mappings are empty.");
        }
        if (kb != null && (kb.constituents == null || kb.constituents.isEmpty())) {
            problemList.add("KB has no loaded constituents.");
        }
        if (!problemList.isEmpty()) {
            StringBuilder sBuild = new StringBuilder();
            final String NEWLINE_AND_SPACES = "\n   ";
            for (String problem : problemList) {
                sBuild.append(NEWLINE_AND_SPACES).append(problem);
            }
            System.err.println("Configuration failed. Problems:" + sBuild.toString());
            throw new IllegalStateException("Configuration failed. Problems:" + sBuild.toString());
        }
    }

    /***************************************************************
     * */
    public static <T> void displayCollectionStringDiffs(Collection<T> coll1, Collection<T> coll2) {

        for (T obj : coll1) {
            if (!coll2.contains(obj)) {
                System.out.println("Found in parameter 1 but not 2: " + obj.toString());
            }
        }
        for (T obj : coll2) {
            if (!coll1.contains(obj)) {
                System.out.println("Found in parameter 2 but not 1: " + obj.toString());
            }
        }
    }
}