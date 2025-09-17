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
    private Map<String, Set<String>> localInstances = new HashMap<>();
    private Map<String, Set<String>> localSubclasses = new HashMap<>();
    private Set<String> localIndividuals = new HashSet<>();
    private Set<String> localClasses = new HashSet<>();

    /**
 * Runs syntax and semantic checks on KIF content, returning diagnostics.
 * @param contents raw KIF text to check
 * @return list of error/warning strings in "line:col: SEVERITY: message" format
 */
public List<String> check(String contents) {

    SUMOtoTFAform.initOnce();
    List<String> msgs = new ArrayList<>();
    if (contents == null) contents = "";
    if (debug) System.out.println("*******************************************************");

    // ---------- 1) Syntax Errors ----------
    SuokifVisitor sv = SuokifApp.process(contents);
    if (!sv.errors.isEmpty()) {
        for (String er : sv.errors) {
            int line = getLineNum(er);
            int col  = getOffset(er);
            msgs.add(fmt(line == 0 ? 1 : line, Math.max(col, 1), "ERROR", er));
        }
        return msgs;
    }

    // ---------- 2) Parse to KIF ----------
    KIF kif = new KIF();
    kif.filename = "(buffer)";
    try (Reader r = new StringReader(contents)) {
        kif.parse(r);
    } catch (Exception e) {
        msgs.add(fmt(1, 1, "ERROR", "Parse failure: " + e));
        return msgs;
    }
    // if (!parseKif(kif, contents, msgs)) {
    //     return msgs;
    // }

    String[] bufferLines = contents.split("\n", -1);
    harvestLocalFacts(kif);

    KB kb = SUMOtoTFAform.kb;
    FormulaPreprocessor fp = SUMOtoTFAform.fp;

    // ---------- 3) Semantic checks ----------
    for (Formula f : kif.formulaMap.values()) {
        int formulaLine = findFormulaInBuffer(f.toString(), bufferLines);
        if (debug) System.out.println("Checking formula: " + f);

        // Quantifier not in statement
        if (Diagnostics.quantifierNotInStatement(f)) {
            msgs.add(fmt(formulaLine > 0 ? formulaLine : f.startLine, 1,
                         "ERROR", "Quantifier not in statement"));
        }
        // Existential quantifier in Antecedent
        if (Diagnostics.existentialInAntecedent(f)) {
            msgs.add(fmt(formulaLine > 0 ? formulaLine : f.startLine, 1,
                         "ERROR", "Existential quantifier in Antecedent"));
        }
        // Single-use variables
        Set<String> singleUse = Diagnostics.singleUseVariables(f);
        if (singleUse != null) {
            for (String v : singleUse) {
                reportAllOccurrencesInBuffer(v,
                        "Variable used only once: " + v,
                        bufferLines, msgs, "WARNING");
            }
        }

        // Preprocessor warnings/errors
        Set<Formula> processed = fp.preProcess(f, false, kb);
        if (f.errors != null) {
            for (String er : f.errors) {
                msgs.add(fmt(formulaLine > 0 ? formulaLine : f.startLine, 1,
                             "ERROR", er));
            }
        }
        if (f.warnings != null) {
            for (String w : f.warnings) {
                msgs.add(fmt(formulaLine > 0 ? formulaLine : f.startLine, 1,
                             "WARNING", w));
            }
        }

        // SUMOtoTFAform errors
        if (SUMOtoTFAform.errors != null && !SUMOtoTFAform.errors.isEmpty()
                && processed.size() == 1) {
            for (String er : SUMOtoTFAform.errors) {
                msgs.add(fmt(formulaLine > 0 ? formulaLine : f.startLine, 1,
                             "ERROR", er));
            }
            SUMOtoTFAform.errors.clear();
        }

        // KButilities validity check
        if (!KButilities.isValidFormula(kb, f.toString())) {
            for (String er : KButilities.errors) {
                msgs.add(fmt(formulaLine > 0 ? formulaLine : f.startLine, 1,
                             "ERROR", er));
            }
            KButilities.errors.clear();
        }

        // Unquantified variables in consequent
        Set<String> unquant = Diagnostics.unquantInConsequent(f);
        if (unquant != null) {
            for (String uq : unquant) {
                reportAllOccurrencesInBuffer(uq,
                        "Unquantified variable in consequent: " + uq,
                        bufferLines, msgs, "ERROR");
            }
        }

        // Term below Entity + unknown terms
        Set<String> terms = f.collectTerms();
        for (String t : terms) {
            if (skip(t)) continue;

            boolean coveredByLocal = localIndividuals.contains(t) || localClasses.contains(t);
            if (!coveredByLocal && Diagnostics.termNotBelowEntity(t, kb)) {
            msgs.add(fmt(formulaLine > 0 ? formulaLine : f.startLine, 1,
                                        "ERROR", "Term not below Entity: " + t));
            }
        }
    }

    Pattern linePattern = Pattern.compile("Line\\s*#(\\d+)");
    msgs.sort(Comparator.comparingInt((String m) -> {
        Matcher matcher = linePattern.matcher(m);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return Integer.MAX_VALUE;
    }));

    return msgs;
}


    /** Helper: report all occurrences of a term in buffer */
    private static void reportAllOccurrencesInBuffer(String term,
                                                    String errorMessage,
                                                    String[] bufferLines,
                                                    List<String> msgs,
                                                    String severity) {
        for (int lineNum = 0; lineNum < bufferLines.length; lineNum++) {
            String line = bufferLines[lineNum];
            String trimmed = line.trim();
            if (trimmed.startsWith(";;") || trimmed.startsWith(";")) continue;
            int searchStart = 0;
            while (searchStart < line.length()) {
                int pos = findTermInLine(line, term, searchStart);
                if (pos == -1) break;
                // lineNum+1 for 1-based display, pos+1 for 1-based column
                msgs.add(fmt(lineNum + 1, pos + 1, severity, errorMessage));
                searchStart = pos + term.length();
            }
        }
    }

/** Find a term in a line with boundary checks */
private static int findTermInLine(String line, String term, int startPos) {
    int pos = line.indexOf(term, startPos);
    while (pos != -1) {
        boolean validStart = (pos == 0 || !isTermChar(line.charAt(pos - 1)));
        boolean validEnd = (pos + term.length() >= line.length()
                || !isTermChar(line.charAt(pos + term.length())));
        if (validStart && validEnd) return pos;
        pos = line.indexOf(term, pos + 1);
    }
    return -1;
}

private static boolean isTermChar(char c) {
    return Character.isLetterOrDigit(c) || c == '-' || c == '_';
}


    /** Collect local facts from formulas: instance, subclass, names. */
    private void harvestLocalFacts(KIF kif) {
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
                }
            }
            else if ("subclass".equals(functor) && args.size() == 2) {
                String child = args.get(0), parent = args.get(1);
                if (isConst(child) && isConst(parent)) {
                    localClasses.add(child);
                }
            }
        }
    }

    private static boolean isConst(String tok) {
        return !(Formula.isVariable(tok) || StringUtil.isNumeric(tok) || StringUtil.isQuotedString(tok));
    }

    /** Find where a formula string appears in the buffer */
/** Find where a formula string appears in the buffer by matching all lines consecutively. */
/** Find where a formula string appears in the buffer by matching all lines consecutively. */
private static int findFormulaInBuffer(String formulaStr, String[] bufferLines) {
    if (formulaStr == null || formulaStr.isEmpty()) return -1;

    // Split into trimmed, non-empty formula lines
    String[] rawFormulaLines = formulaStr.split("\\R");
    List<String> formulaLines = new ArrayList<>();
    for (String line : rawFormulaLines) {
        String t = line.trim();
        if (!t.isEmpty()) {
            formulaLines.add(t);
        }
    }
    if (formulaLines.isEmpty()) return -1;

    if (debug) {
        System.out.println("KifFileChecker.findFormulaInBuffer(): candidate formula lines:");
        for (String l : formulaLines) System.out.println("  >> " + l);
    }

    // Scan the buffer for a block of consecutive lines matching the formula
    for (int i = 0; i <= bufferLines.length - formulaLines.size(); i++) {
        boolean match = true;
        for (int j = 0; j < formulaLines.size(); j++) {
            String bufLine = bufferLines[i + j].trim();
            if (!bufLine.contains(formulaLines.get(j))) {
                match = false;
                break;
            }
        }
        if (match) {
            if (debug) {
                System.out.println("KifFileChecker.findFormulaInBuffer(): matched full formula starting at buffer line " + (i + 1));
            }
            return i + 1; // return 1-based start line of the formula
        }
    }

    return -1; // not found
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
        return sev + " in Formula on Line #" + line1 + ": " + msg;
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
                KifFileChecker kfc = new KifFileChecker();
                String contents = String.join("\n", FileUtil.readLines(fname));
                List<String> errors = kfc.check(contents);        
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