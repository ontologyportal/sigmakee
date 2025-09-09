package com.articulate.sigma;

import com.articulate.sigma.parsing.SuokifApp;
import com.articulate.sigma.parsing.SuokifVisitor;
import com.articulate.sigma.trans.SUMOtoTFAform;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.utils.FileUtil;

import java.io.Reader;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// import errorlist.DefaultErrorSource;

/**
 * Headless KIF checker that mirrors SUMOjEdit.checkErrorsBody logic,
 * but returns diagnostics as strings: "line:col: SEVERITY: message".
 *
 * No UI / jEdit dependencies.
 */
public class KifFileChecker {
    public static boolean debug = true;

    /**
     * Headless version of SUMOjEdit.checkErrorsBody().
     * Collects syntax + semantic errors, returns as strings with line/col/severity.
     */
    public static List<String> check(String contents) {

        SUMOtoTFAform.initOnce();
        List<String> msgs = new ArrayList<>();
        if (contents == null) contents = "";

        int counter = 0, idx, line, offset;
        // ---------- 1) Syntax Errors ----------
        SuokifVisitor sv = SuokifApp.process(contents);
        if (!sv.errors.isEmpty()) {
            for (String er : sv.errors) {
                line = getLineNum(er);
                int col  = getOffset(er);
                msgs.add(fmt(line == 0 ? 1 : line, Math.max(col, 1), "ERROR", er));
            }
            return msgs;
        } else System.out.println("KifFileChecker.check() -> SuokifApp.process() No Errors Found");
        
        // ---------- 2) Parse to KIF ----------
        KIF kif = new KIF();              // ✅ create new KIF object
        kif.filename = "(buffer)";        // give it a pseudo filename
        try (Reader r = new StringReader(contents)) {
            kif.parse(r);                 // parse the contents into this kif
        } catch (Exception e) {
            msgs.add(fmt(1, 1, "ERROR", "Parse failure: " + e));
            return msgs;
        }

        if (!parseKif(kif, contents, msgs)) {
            return msgs;
        } else System.out.println("KifFileChecker.check() -> parseKif() No Errors Found");
        
        KB kb = SUMOtoTFAform.kb;
        FormulaPreprocessor fp = SUMOtoTFAform.fp;

        // ---------- 3) Semantic checks ----------
        Set<String> notBelowEntityTerms = new HashSet<>();
        Set<String> unknownTerms = new HashSet<>();
        Set<String> result, unquant, terms;
        Set<Formula> processed;
        String err, term;
        FileSpec defn;
        for (Formula f : kif.formulaMap.values()) {
            System.out.println("\n---------------------------------------------\n" + 
                                 "KifFileChecker.check() -> Checking formula \n" + f.toString());
            
            // Quantifier Not In Statement
            if (Diagnostics.quantifierNotInStatement(f)) {
                msgs.add(fmt(f.startLine, 1, "ERROR", "Quantifier not in statement"));
            } else System.out.println("KifFileChecker.check() -> Diagnostics.quantifierNotInStatement() false");

            // Variables used once
            Set<String> singleUse = Diagnostics.singleUseVariables(f);
            if (singleUse != null && !singleUse.isEmpty()) {
                for (String v : singleUse) {
                    msgs.add(fmt(f.startLine, 1, "WARNING", "Variable(s) only used once: " + v));
                }
            } else System.out.println("KifFileChecker.check() -> singleUserVariables() " + singleUse);

            // Preprocessor warnings/errors
            processed = fp.preProcess(f, false, kb);
            if (f.errors != null && !f.errors.isEmpty()) {
                for (String er : f.errors) {
                    msgs.add(fmt(f.startLine, 1, "ERROR", er));
                }
            } else if (f.warnings != null && !f.warnings.isEmpty()) {
                for (String w : f.warnings) {
                    msgs.add(fmt(f.startLine, 1, "WARNING", w));
                }
            }

            // Sumo to TFA form errors
            if (SUMOtoTFAform.errors != null && !f.errors.isEmpty() && processed.size() == 1) {
                for (String er : SUMOtoTFAform.errors) {
                    msgs.add(fmt(f.startLine, 1, "ERROR", er));
                    System.out.println("KifFileChecker.check() -> SUMOtoTFAform error: " + er);
                }
                SUMOtoTFAform.errors.clear();
            }

            // KButilities isValidFormula()
            if (!KButilities.isValidFormula(kb, f.toString())) {
                for (String er : KButilities.errors) {
                    msgs.add(fmt(f.startLine, 1, "ERROR", er));
                }
                KButilities.errors.clear();
            } else System.out.println("KifFileChecker.check() -> KButilities.isValidFormula() true");


            // Unquantified variables in consequent
            unquant = Diagnostics.unquantInConsequent(f);
            if (unquant != null && !unquant.isEmpty()) {
                for (String uq : unquant) {
                    msgs.add(fmt(f.startLine, 1, "ERROR", "Unquantified var: " + uq));
                }
            } else System.out.println("KifFileChecker.check() -> f.collectUnquantifiedVariables() " + unquant);

            // PredVarInst arity check
            term = PredVarInst.hasCorrectArity(f, kb);
            if (!StringUtil.emptyString(term)) {
                msgs.add(fmt(f.startLine, 1, "ERROR", "Arity error of predicate: " + term));
            } else System.out.println("KifFileChecker.check() -> PredVarInst.hasCorrectArity() " + term);
        
            // Term below Entity + unknown terms
            terms = f.collectTerms();
            for (String t : terms) {
                if (Diagnostics.LOG_OPS.contains(t) || "Entity".equals(t)
                    || Formula.isVariable(t) || StringUtil.isNumeric(t)
                    || StringUtil.isQuotedString(t)) {
                    continue;
                }
                if (Diagnostics.termNotBelowEntity(t, kb) && !notBelowEntityTerms.contains(t)) {
                    notBelowEntityTerms.add(t);
                    msgs.add(fmt(f.startLine, 1, "ERROR", "term not below Entity: " + t));
                }
                // Unknown term check (no definition in KB)
                defn = findDefn(t, kb, kif);
                if (defn == null && !unknownTerms.contains(t)) {
                    unknownTerms.add(t);
                    err = "unknown term: " + t;
                    msgs.add(fmt(f.startLine, 0, "WARNING", err));
                    System.out.println("KifFileChecker.check() -> " + err);
                }

            }
        }

        return msgs;
    }

    /**
     * ***************************************************************
     * Utility class that contains searched term line and filepath information
     */
    public static class FileSpec {

        public String filepath = "";
        public int line = -1;
    }

    /**
     * ***************************************************************
     * @return a FileSpec with searched term info
     */
    private static FileSpec filespecFromForms(java.util.List<Formula> forms, String currentFName) {

        FileSpec fs = new FileSpec();
        for (Formula f : forms) {
            if (FileUtil.noPath(f.getSourceFile()).equals(currentFName) && !f.getSourceFile().endsWith("_Cache.kif")) {
                fs.filepath = f.sourceFile;
                fs.line = f.startLine - 1; // jedit starts from 0, SUMO starts from 1
                return fs;
            }
        }
        for (Formula f : forms) {
            if (!f.getSourceFile().endsWith("_Cache.kif")) {
                fs.filepath = f.sourceFile;
                fs.line = f.startLine - 1; // jedit starts from 0, SUMO starts from 1
                return fs;
            }
        }
        return fs;
    }

    /**
     * ***************************************************************
     * Note that the "definition" of a term is collection of axioms so look for,
     * in order: instance, subclass, subAttribute, subrelation, domain, documentation
     * @param term the term to search for
     * @return a FileSpec with searched term info
     */
    private static FileSpec findDefn(String term, KB kb, KIF kif) {

        if (StringUtil.emptyString(kif.filename))
            kif.filename = "(buffer)";

        String currentFName = FileUtil.noPath(kif.filename);
        java.util.List<Formula> forms = kb.askWithRestriction(0, "instance", 1, term);
        if (forms != null && !forms.isEmpty())
            return(filespecFromForms(forms, currentFName));
        forms = kb.askWithRestriction(0, "subclass", 1, term);
        if (forms != null && !forms.isEmpty())
            return(filespecFromForms(forms, currentFName));
        forms = kb.askWithRestriction(0, "subAttribute", 1, term);
        if (forms != null && !forms.isEmpty())
            return(filespecFromForms(forms, currentFName));
        forms = kb.askWithRestriction(0, "subrelation", 1, term);
        if (forms != null && !forms.isEmpty())
            return(filespecFromForms(forms, currentFName));
        forms = kb.askWithRestriction(0, "domain", 1, term);
        if (forms != null && !forms.isEmpty())
            return(filespecFromForms(forms, currentFName));
        forms = kb.askWithRestriction(0, "documentation", 1, term);
        if (forms != null && !forms.isEmpty())
            return(filespecFromForms(forms, currentFName));
        return null; // nothing found
    }


    public void showStats(KIF kif, String filename, KB kb) {

        try {
            int termCount = 0;
            int otherTermCount = 0;
            FileSpec defn;
            String thisNoPath;

            for (String t : kif.terms) {
                if (t == null) {
                    System.out.println("showStats(): null term");
                    continue;
                }
                defn = findDefn(t, kb, kif);
                if (defn == null) {
                    if (!Formula.isLogicalOperator(t)) {
                        System.out.println("showStats(): no definition found for: " + t);
                        continue;
                    } else {
                        thisNoPath = "";
                    }
                }
                else {
                    thisNoPath = FileUtil.noPath(defn.filepath);
                }

                if (thisNoPath.equals(filename) || StringUtil.emptyString(thisNoPath)) {
                    System.out.println("showStats(): ******* in this file: " + t);
                    termCount++;
                } else {
                    if (!Formula.isLogicalOperator(t)) {
                        otherTermCount++;
                    }
                }
            }

            System.out.println("showStats(): # terms: " + termCount);
            System.out.println("showStats(): # terms used from other files: " + otherTermCount);
            System.out.println("showStats(): # axioms: " + kif.formulaMap.keySet().size());

            int ruleCount = 0;
            for (Formula f : kif.formulaMap.values()) {
                if (f.isRule()) {
                    ruleCount++;
                }
            }
            System.out.println("showStats(): # rules: " + ruleCount);
            System.out.println("showStats(): done reading kif file");

        } catch (Exception e) {
            String msg = "Error in showStats() with: " + kif.filename + ": " + e;
            System.err.println(msg);
            e.printStackTrace();
        }

        System.out.println("showStats(): complete");
    }


        /**
     * ***************************************************************
     * sigmaAntlr generates line offsets
     * @return the line offset of where the error/warning begins
     */
    private static int getOffset(String line) {

        int result = -1;
        Pattern p = Pattern.compile("\\:(\\d+)\\:");
        Matcher m = p.matcher(line);
        if (m.find()) {
//            Log.log(Log.MESSAGE, this, ":getOffset(): found offset number: " + m.group(1));
            try {
                result = Integer.parseInt(m.group(1));
            } catch (NumberFormatException nfe) {}
        }
        if (result < 0)
            result = 0;
        return result;
    }

    private static boolean skip(String t) {
        return Diagnostics.LOG_OPS.contains(t)
                || "Entity".equals(t)
                || Formula.isVariable(t)
                || StringUtil.isNumeric(t)
                || StringUtil.isQuotedString(t);
    }

    private static boolean isKnownTerm(KB kb, String t) {
        try {
            if (kb != null && kb.containsTerm(t)) return true;
        } catch (Throwable ignore) {}
        return false;
    }

    private static boolean parseKif(KIF kif, String contents, List<String> msgs) {
        boolean retVal = false;
        try (Reader r = new StringReader(contents)) {
            kif.parse(r);
            System.out.println("parseKif(): done reading kif file");
            retVal = true;
        } catch (Exception e) {
            String msg = "Error in parseKif() with: " + kif.filename + ": " + e;
            System.err.println(msg);
            msgs.add(fmt(1, 1, "ERROR", msg));
        } finally {
            // Surface warnings and errors in the same text format
            for (String warn : kif.warningSet) {
                int line = getLineNum(warn);
                int col  = getOffset(warn);
                msgs.add(fmt(line == 0 ? 1 : line, Math.max(col, 1), "WARNING", warn));
            }
            for (String er : kif.errorSet) {
                int line = getLineNum(er);
                int col  = getOffset(er);
                msgs.add(fmt(line == 0 ? 1 : line, Math.max(col, 1), "ERROR", er));
            }
        }
        return retVal;
    }


    private static String fmt(int line1, int col1, String sev, String msg) {
        return line1 + ":" + col1 + ": " + sev + ": " + msg;
    }

    // Mirrors SUMOjEdit’s extractors so line/col match existing diagnostics
    private static int getLineNum(String line) {
        int result = -1;
        Matcher m = Pattern.compile("(\\d+):").matcher(line);
        if (m.find()) { try { result = Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {} }
        if (result < 0) {
            m = Pattern.compile("line:(\\d+)").matcher(line);
            if (m.find()) { try { result = Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {} }
        }
        if (result < 0) {
            m = Pattern.compile("line&#58; (\\d+)").matcher(line);
            if (m.find()) { try { result = Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {} }
        }
        return Math.max(0, result);
    }

    // private static int getOffset(String line) {
    //     int result = -1;
    //     Matcher m = Pattern.compile("\\:(\\d+)\\:").matcher(line);
    //     if (m.find()) { try { result = Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {} }
    //     return Math.max(0, result);
    // }
    }
