package com.articulate.sigma.mlpipeline;

import com.articulate.sigma.*;
import com.articulate.sigma.trans.SUMOtoTFAform;
import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.StringUtil;

import java.util.Collection;
import java.util.List;

import static com.articulate.sigma.utils.FileUtil.*;

public class TestSQUAD {

    public static void main(String[] args) {

        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        System.out.println("KBmutilities.main(): completed init");
        List<String> lines = readLines(args[0]);
        int syntaxErrorCount = 0;
        int termErrorCount = 0;
        int typeErrorCount = 0;
        int correctCount = 0;
        for (String s : lines) {
            if (!StringUtil.emptyString(s) && s.startsWith("Response: ")) {
                String resp = s.substring(9);
                resp = resp.replace("</s>","");
                KIF kif = new KIF();
                kif.parseStatement(resp);
                if (kif.errorSet != null && kif.errorSet.size() > 0) {
                    System.out.println("Errors: " + kif.errorSet);
                    syntaxErrorCount++;
                }
                else {
                    Formula f = kif.formulaMap.values().iterator().next();
                    System.out.println(f);
                    if (f.errors.size() > 0)
                        System.out.println("Errors: " + f.errors);
                    Collection<String> terms = f.collectTerms();
                    boolean termMissing = false;
                    SUMOtoTFAform.errors.clear();
                    boolean typeError = !KButilities.hasCorrectTypes(kb,f);
                    for (String t : terms) {
                        if (!kb.containsTerm(t) && !t.startsWith("?") && !t.equals("You") &&
                                !t.equals("Now") && !StringUtil.isNumeric(t) && !StringUtil.quoted(t)) {
                            System.out.println("term not in KB: " + t);
                            termMissing = true;
                        }
                    }
                    if (termMissing)
                        termErrorCount++;
                    else if (typeError)
                        typeErrorCount++;
                    else {
                        correctCount++;
                        System.out.println("May be correct. No missing terms or syntax errors");
                    }
                }
            }
            else if (!StringUtil.emptyString(s) && s.startsWith("Input: ")) {
                System.out.println(s);
            }
            else
                System.out.println();
        }
        System.out.println();
        System.out.println("number correct: " + correctCount);
        System.out.println("bad formulas: " + syntaxErrorCount);
        System.out.println("bad types: " + typeErrorCount);
        System.out.println("bad terms: " + termErrorCount);
    }
}
