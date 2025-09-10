/** This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the termsof the GNU
license.  This software is released under the GNU Public License
<http://www.gnu.org/copyleft/gpl.html>.  Users of this code also consent,
by use of this code, to credit Articulate Software and Teknowledge in any
writings, briefings, publications, presentations, or other representations
of any software which incorporates, builds on, or uses this code.  Please
cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. see also
http://sigmakee.sourceforge.net
*/

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

/**
 * Headless KIF checker that mirrors SUMOjEdit.checkErrorsBody logic,
 * but returns diagnostics as strings: "line:col: SEVERITY: message".
 *
 * No UI / jEdit dependencies.
 */
public class KifFileChecker {
    public static boolean debug = false;
    // New maps and sets to track local definitions
    private static final Map<String, Set<String>> localInstances = new HashMap<>();
    private static final Map<String, Set<String>> localSubclasses = new HashMap<>();
    private static final Set<String> localIndividuals = new HashSet<>();
    private static final Set<String> localClasses = new HashSet<>();

    /**
     * Runs syntax and semantic checks on KIF content, returning diagnostics.
     * @param contents raw KIF text to check
     * @return list of error/warning strings in "line:col: SEVERITY: message" format
     */
    public static List<String> check(String contents) {

        SUMOtoTFAform.initOnce();
        List<String> msgs = new ArrayList<>();
        if (contents == null) contents = "";
        int counter = 0, idx, line, offset;
        // ---------- 1) Syntax Errors ----------
        if (debug) System.out.println("*******************************************************");
        SuokifVisitor sv = SuokifApp.process(contents);
        if (!sv.errors.isEmpty()) {
            for (String er : sv.errors) {
                line = getLineNum(er);
                int col  = getOffset(er);
                msgs.add(fmt(line == 0 ? 1 : line, Math.max(col, 1), "ERROR", er));
            }
            return msgs;
        } else if (debug) System.out.println("KifFileChecker.check() -> SuokifApp.process() No Errors Found");
        // ---------- 2) Parse to KIF ----------
        KIF kif = new KIF();
        kif.filename = "(buffer)";
        try (Reader r = new StringReader(contents)) {
            kif.parse(r);
        } catch (Exception e) {
            msgs.add(fmt(1, 1, "ERROR", "Parse failure: " + e));
            return msgs;
        }
        if (!parseKif(kif, contents, msgs)) {
            return msgs;
        } else if (debug) System.out.println("KifFileChecker.check() -> parseKif() No Errors Found");
        harvestLocalFacts(kif);
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
            if (debug) System.out.println("\n---------------------------------------------\n" + 
                                 "KifFileChecker.check() -> Checking formula \n" + f.toString());
            // Quantifier Not In Statement
            if (Diagnostics.quantifierNotInStatement(f)) {
                msgs.add(fmt(f.startLine, 1, "ERROR", "Quantifier not in statement"));
            } else if (debug) System.out.println("KifFileChecker.check() -> Diagnostics.quantifierNotInStatement() false");
            // Variables used once
            Set<String> singleUse = Diagnostics.singleUseVariables(f);
            if (singleUse != null && !singleUse.isEmpty()) {
                for (String v : singleUse)
                    msgs.add(fmt(f.startLine, 1, "WARNING", "Variable(s) only used once: " + v));
            } else if (debug) System.out.println("KifFileChecker.check() -> singleUserVariables() " + singleUse);
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
                    if (debug) System.out.println("KifFileChecker.check() -> SUMOtoTFAform error: " + er);
                }
                SUMOtoTFAform.errors.clear();
            }
            // KButilities isValidFormula()
            if (!KButilities.isValidFormula(kb, f.toString())) {
                for (String er : KButilities.errors) {
                    msgs.add(fmt(f.startLine, 1, "ERROR", er));
                }
                KButilities.errors.clear();
            } else if (debug) System.out.println("KifFileChecker.check() -> KButilities.isValidFormula() true");
            // Unquantified variables in consequent
            unquant = Diagnostics.unquantInConsequent(f);
            if (unquant != null && !unquant.isEmpty()) {
                for (String uq : unquant) {
                    msgs.add(fmt(f.startLine, 1, "ERROR", "Unquantified var: " + uq));
                }
            } else if (debug) System.out.println("KifFileChecker.check() -> f.collectUnquantifiedVariables() " + unquant);
            // PredVarInst arity check
            term = PredVarInst.hasCorrectArity(f, kb);
            if (!StringUtil.emptyString(term)) {
                msgs.add(fmt(f.startLine, 1, "ERROR", "Arity error of predicate: " + term));
            } else if (debug) System.out.println("KifFileChecker.check() -> PredVarInst.hasCorrectArity() " + term);
            // Term below Entity + unknown terms
            terms = f.collectTerms();
            for (String t : terms) {
                if (Diagnostics.LOG_OPS.contains(t) || "Entity".equals(t)
                    || Formula.isVariable(t) || StringUtil.isNumeric(t)
                    || StringUtil.isQuotedString(t)) {
                    continue;
                }
                boolean coveredByLocal = false;
                // If it's a locally introduced individual, check its class chain
                if (localIndividuals.contains(t)) {
                    Set<String> classes = localInstances.getOrDefault(t, Collections.emptySet());
                    for (String c : classes) {
                        if ("Entity".equals(c) || kb.isSubclass(c, "Entity") || localClasses.contains(c)) {
                            coveredByLocal = true;
                            break;
                        }
                    }
                }
                // If it's a locally introduced class, allow it
                else if (localClasses.contains(t)) {
                    coveredByLocal = true;
                }

                if (!coveredByLocal) {
                    if (Diagnostics.termNotBelowEntity(t, kb) && !notBelowEntityTerms.contains(t)) {
                        notBelowEntityTerms.add(t);
                        msgs.add(fmt(f.startLine, 1, "ERROR", "term not below Entity: " + t));
                    }

                    // defn = findDefn(t, kb, kif);
                    // if (defn == null && !unknownTerms.contains(t)) {
                    //     unknownTerms.add(t);
                    //     err = "unknown term: " + t;
                    //     msgs.add(fmt(f.startLine, 0, "WARNING", err));
                    // }
                }
            }
        }
        // Sort messages by line number, then column number
        msgs.sort(Comparator.comparingInt((String m) -> {
            try {
                return Integer.parseInt(m.split(":")[0]);
            } catch (Exception e) {
                return Integer.MAX_VALUE; // put malformed lines at the end
            }
        }).thenComparingInt(m -> {
            try {
                return Integer.parseInt(m.split(":")[1]);
            } catch (Exception e) {
                return Integer.MAX_VALUE;
            }
        }));
        return msgs;
    }

    /** Collect local facts from formulas: instance, subclass, names. */
    private static void harvestLocalFacts(KIF kif) {
        localInstances.clear();
        localSubclasses.clear();
        localIndividuals.clear();
        localClasses.clear();

        for (Formula f : kif.formulaMap.values()) {
            if (f == null || f.atom()) continue;
            String functor = f.car();
            List<String> args = f.argumentsToArrayListString(1);
            if (args == null) continue;

            if ("instance".equals(functor) && args.size() == 2) {
                String indiv = args.get(0), cls = args.get(1);
                if (isConst(indiv) && isConst(cls)) {
                    localIndividuals.add(indiv);
                    localClasses.add(cls);
                    localInstances.computeIfAbsent(indiv, k -> new HashSet<>()).add(cls);
                }
            }
            else if ("subclass".equals(functor) && args.size() == 2) {
                String child = args.get(0), parent = args.get(1);
                if (isConst(child) && isConst(parent)) {
                    localClasses.add(child);
                    localClasses.add(parent);
                    localSubclasses.computeIfAbsent(child, k -> new HashSet<>()).add(parent);
                }
            }
            else if ("names".equals(functor) && args.size() == 2) {
                String target = args.get(1);
                if (isConst(target)) localIndividuals.add(target);
            }
        }
    }

    private static boolean isConst(String tok) {
        return !(Formula.isVariable(tok) || StringUtil.isNumeric(tok) || StringUtil.isQuotedString(tok));
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
     * Builds a FileSpec for the first matching definition of a term in a list of formulas.
     * @param forms list of candidate formulas
     * @param currentFName current filename (no path)
     * @return FileSpec containing the definition’s filepath and line number
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

    /**
     * Prints usage statistics for a parsed KIF file including terms, axioms, and rules.
     * @param kif parsed KIF object
     * @param filename source filename of the KIF
     * @param kb knowledge base to resolve definitions
     */
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
     * Extracts the numeric column offset from an error/warning line string.
     * @param line error/warning line text
     * @return offset as int (0 if none found)
     */
    private static int getOffset(String line) {

        int result = -1;
        Pattern p = Pattern.compile("\\:(\\d+)\\:");
        Matcher m = p.matcher(line);
        if (m.find()) {
            try {
                result = Integer.parseInt(m.group(1));
            } catch (NumberFormatException nfe) {}
        }
        if (result < 0)
            result = 0;
        return result;
    }

    /**
     * Determines if a term should be skipped from semantic checks.
     * @param t term string
     * @return true if logical operator, Entity, variable, number, or quoted string
     */
    private static boolean skip(String t) {
        return Diagnostics.LOG_OPS.contains(t)
                || "Entity".equals(t)
                || Formula.isVariable(t)
                || StringUtil.isNumeric(t)
                || StringUtil.isQuotedString(t);
    }

    /**
     * Checks whether a term exists in the given knowledge base.
     * @param kb knowledge base instance
     * @param t term string
     * @return true if KB contains the term, false otherwise
     */
    private static boolean isKnownTerm(KB kb, String t) {

        try {
            if (kb != null && kb.containsTerm(t)) return true;
        } catch (Throwable ignore) {}
        return false;
    }

    /**
     * Parses KIF content into a KIF object and collects warnings/errors.
     * @param kif target KIF object
     * @param contents KIF text content
     * @param msgs output list for formatted error/warning messages
     * @return true if parse succeeded, false otherwise
     */
    private static boolean parseKif(KIF kif, String contents, List<String> msgs) {

        boolean retVal = false;
        try (Reader r = new StringReader(contents)) {
            kif.parse(r);
            if (debug) System.out.println("parseKif(): done reading kif file");
            retVal = true;
        } catch (Exception e) {
            String msg = "Error in parseKif() with: " + kif.filename + ": " + e;
            System.err.println(msg);
            msgs.add(fmt(1, 1, "ERROR", msg));
        } finally {
            for (String er : kif.errorSet) {
                int line = getLineNum(er);
                int col  = getOffset(er);
                msgs.add(fmt(line == 0 ? 1 : line, Math.max(col, 1), "ERROR", er));
            }
            for (String warn : kif.warningSet) {
                int line = getLineNum(warn);
                int col  = getOffset(warn);
                msgs.add(fmt(line == 0 ? 1 : line, Math.max(col, 1), "WARNING", warn));
            }
        }
        return retVal;
    }

    /**
     * Formats diagnostics into "line:col: SEVERITY: message".
     * @param line1 line number
     * @param col1 column number
     * @param sev severity string (ERROR/WARNING)
     * @param msg message text
     * @return formatted diagnostic string
     */
    private static String fmt(int line1, int col1, String sev, String msg) {
        return line1 + ":" + col1 + ": " + sev + ": " + msg;
    }

    /**
     * Extracts line number from an error/warning line string.
     * @param line error/warning line text
     * @return line number (0 if not found)
     */
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

        /** ***************************************************************
     * Print CLI usage information.
     */
    public static void showHelp() {
        System.out.println("KifFileChecker - Headless KIF syntax/semantic checker");
        System.out.println("Options:");
        System.out.println("  -h              Show this help screen");
        System.out.println("  -c <file.kif>   Check the given KIF file and print diagnostics");
    }

    /** ***************************************************************
     * Command-line entry point.
     * Usage examples:
     *   java com.articulate.sigma.KifFileChecker -h
     *   java com.articulate.sigma.KifFileChecker -c myfile.kif
     */
    public static void main(String[] args) {
        if (args == null || args.length == 0 || "-h".equals(args[0])) {
            showHelp();
            return;
        }

        if ("-c".equals(args[0]) && args.length > 1) {
            String fname = args[1];
            try {
                String contents = String.join("\n", FileUtil.readLines(fname));
                List<String> errors = check(contents);        
                System.out.println("*******************************************************");
                if (errors.isEmpty()) {
                    System.out.println("No errors found in " + fname);
                } else {
                    System.out.println("Diagnostics for " + fname + ":");
                    for (String e : errors) {
                        System.out.println(e);
                    }
                }
            } catch (Exception e) {
                    
            System.out.println("*******************************************************");
                System.err.println("Failed to read or check file: " + fname);
                e.printStackTrace();
            }
        } else {
            showHelp();
        }
    }
}