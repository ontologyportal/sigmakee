package com.articulate.sigma.utils;

import com.articulate.sigma.*;
import com.articulate.sigma.tp.Vampire;
import com.articulate.sigma.trans.TPTP3ProofProcessor;
import com.articulate.sigma.wordNet.*;

public class PythonAPI {

    KB kb = null;

    public PythonAPI() {
        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
    }

    public String getAllSub(String term, String rel) {
        return kb.kbCache.getChildTerms(term,rel).toString();
    }

    public String getWords(String term) {
        return WordNet.wn.getWordsFromTerm(term).keySet().toString();
    }

    public String query(String q, int timeout) {

        TPTP3ProofProcessor tpp = null;
        kb.loadVampire();
        Vampire vamp = kb.askVampire(q, timeout, 1);
        System.out.println("KB.main(): completed query with result: " + StringUtil.arrayListToCRLFString(vamp.output));
        tpp = new TPTP3ProofProcessor();
        tpp.parseProofOutput(vamp.output, q, kb);
        return tpp.bindings + "\n\n" + tpp.proof;
    }

    public String formula(String kind, int argnum, String term) {
        return kb.ask(kind,argnum,term).toString();
    }

    public String tell(String form) {
        return kb.tell(form);
    }
}
