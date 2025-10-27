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

    /** ***************************************************************
     * Print CLI usage information.
     */
    public static void showHelp() {
        System.out.println("KifFileChecker - Headless KIF syntax/semantic checker");
        System.out.println("Options:");
        System.out.println("  -h              Show this help screen");
        System.out.println("  -o              Find Orphan Variables");
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
        if ("-o".equals(args[0]) && args.length > 1) {
            String fname = args[1];
            try {
                KifFileChecker kfc = new KifFileChecker();
                String contents = String.join("\n", FileUtil.readLines(fname));
                KIF kif = StringToKif(contents, fname, new ArrayList<>());
                for (Formula f : kif.formulaMap.values()) {
                    if (debug) System.out.println("Formula: " + f);
                    HashMap<String, HashSet<String>> links = Diagnostics.findOrphanVars(f);
                    for (Map.Entry<String, HashSet<String>> e : links.entrySet()) {
                        if (debug) System.out.println("  " + e.getKey() + " ↔ " + e.getValue());
                    }
                    if (debug) System.out.println("---------------------------------------------------");
                }
            } catch (Exception e) {
                System.err.println("Failed to process " + fname);
                e.printStackTrace();
            }
            return;
        }
        if ("-c".equals(args[0]) && args.length > 1) {
            String fname = args[1];
            try {
                KifFileChecker kfc = new KifFileChecker();
                String contents = String.join("\n", FileUtil.readLines(fname));
                List<ErrRec> errors = kfc.check(contents);
                if (debug) System.out.println("*******************************************************");
                if (errors.isEmpty()) {
                    if (debug) System.out.println("No errors found in " + fname);
                } else {
                    if (debug) System.out.println("Diagnostics for " + fname + ":");
                    for (ErrRec e : errors) {
                        if (debug) System.out.println(e.toString());
                    }
                }
            } catch (Exception e) {        
            if (debug) System.out.println("*******************************************************");
                System.err.println("Failed to read or check file: " + fname);
                e.printStackTrace();
            }
        } else {
            showHelp();
        }
    }

    /** ***************************************************************
     * Check KIF content without a filename.
     * @param contents the KIF text to check
     * @return list of error and warning diagnostics
     */
    public static List<ErrRec> check(String contents) {
        return check(contents, "(buffer)");
    }

    /** ***************************************************************
     * Runs syntax and semantic checks on KIF content, returning diagnostics.
     * @param contents raw KIF text to check
     * @return list of error/warning strings in "line:col: SEVERITY: message" format
     */
    public static List<ErrRec> check(String contents, String fileName) {

        Set<String> localIndividuals = new HashSet<>();
        Set<String> localSubclasses = new HashSet<>();
        SUMOtoTFAform.initOnce();
        List<ErrRec> msgs = new ArrayList<>();
        if (contents == null) return msgs;
        CheckSyntaxErrors(contents, fileName, msgs);
        if (!msgs.isEmpty())
            return msgs;
        KIF localKif = StringToKif(contents, fileName, msgs);
        // if (!parseKif(localKif, contents, fileName, msgs))
        //     return msgs;
        for (Formula f : localKif.formulaMap.values())
            harvestLocalFacts(f, localIndividuals, localSubclasses);
        String[] bufferLines = contents.split("\n", -1);
        KB kb = SUMOtoTFAform.kb;
        for (Formula f : localKif.formulaMap.values()) {
            if (debug) System.out.println("Checking formula: " + f);
            String formulaText = extractBufferSlice(bufferLines, f.startLine, f.endLine);
            int formulaStartLine = findFormulaInBuffer(formulaText, bufferLines);
            CheckQuantifiedVariableNotInStatement(fileName, f, formulaText, formulaStartLine, msgs);
            CheckExistentialInAntecedent(fileName, f, formulaText, formulaStartLine, msgs);
            CheckSingleUseVariables(fileName, f, formulaText, formulaStartLine, msgs);
            CheckOrphanVars(fileName, f, formulaText, formulaStartLine, msgs);
            CheckUnquantInConsequent(fileName, f, formulaText, formulaStartLine, msgs);
            Set<Formula> processed = CheckFormulaPreprocess(fileName, kb, f, formulaStartLine, msgs);
            CheckSUMOtoTFAformErrors(fileName, f, formulaStartLine, processed, msgs);
            CheckIsValidFormula(fileName, f, formulaStartLine, kb, formulaText, msgs);
            CheckTermsBelowEntity(fileName, f, formulaStartLine, formulaText, kb, localIndividuals, localSubclasses, msgs);
        }
        SortMessages(msgs);
        return msgs;
    }

    /** ***************************************************************
     * Check for quantified variables that do not appear in the statement body.
     * @param fileName          logical filename
     * @param f                 formula to check
     * @param formulaText       raw text of the formula
     * @param formulaStartLine  buffer line offset of the formula
     * @param msgs              list to collect error records
     */
    public static void CheckQuantifiedVariableNotInStatement(String fileName, Formula f, String formulaText, int formulaStartLine, List<ErrRec> msgs){
        
        String errorMessage = "Quantified variable not used in statement body - ";
        if (Diagnostics.quantifierNotInStatement(f)) {
            String[] quantifiers = { "forall", "exists" };
            int[] rel = { -1, -1 };
            String foundQuant = null;
            for (String q : quantifiers) {
                rel = findLineInFormula(formulaText, q);
                if (rel[0] != -1) {
                    foundQuant = q;
                    break;
                }
            }
            if (foundQuant != null) {
                int absLine = (formulaStartLine > 0 ? formulaStartLine - 1 : f.startLine - 1) + rel[0];
                int absCol = rel[1];
                String[] lines = formulaText.split("\n", -1);
                String offendingLine = (rel[0] >= 0 && rel[0] < lines.length) ? lines[rel[0]].trim() : "";
                msgs.add(new ErrRec(0, fileName, absLine, absCol, absCol + foundQuant.length(), errorMessage + offendingLine));
            } else {
                errorMessage += " (no quantifier found in text)";
                msgs.add(new ErrRec(0, fileName, f.startLine - 1, 0, 1, errorMessage));
            }
        }
    }

    /** ***************************************************************
     * Check for orphan variables — variables that do not co-occur
     * with any others in the same logical context.
     */
    public static void CheckOrphanVars(String fileName, Formula f, String formulaText, int formulaStartLine, List<ErrRec> msgs) {

        HashMap<String, HashSet<String>> cooccurs = Diagnostics.findOrphanVars(f);
        for (Map.Entry<String, HashSet<String>> entry : cooccurs.entrySet()) {
            String var = entry.getKey();
            HashSet<String> linked = entry.getValue();

            if (linked == null || linked.isEmpty()) {
                int[] rel = findLineInFormula(formulaText, var);
                int absLine = (formulaStartLine > 0 ? formulaStartLine - 1 : f.startLine - 1)
                            + (rel[0] >= 0 ? rel[0] : 0);
                int absCol = rel[1] >= 0 ? rel[1] : 0;
                String[] lines = formulaText.split("\n", -1);
                String offendingLine = (rel[0] >= 0 && rel[0] < lines.length) ? lines[rel[0]].trim() : "";
                msgs.add(new ErrRec(0, fileName, absLine, absCol, absCol + var.length(),
                        "Orphan variable (no co-occurrence): " + offendingLine));
            }
        }
    }


    /** ***************************************************************
     * Check for existential quantifiers in antecedents (illegal).
     * @param fileName         logical filename
     * @param f                formula to check
     * @param formulaText      raw text of the formula
     * @param formulaStartLine buffer line offset
     * @param msgs             list to collect error records
     */
    public static void CheckExistentialInAntecedent(String fileName, Formula f, String formulaText, int formulaStartLine, List<ErrRec> msgs) {

        String errorMessage = "Existential quantifier in antecedent - ";
        if (Diagnostics.existentialInAntecedent(f)) {
            int[] rel = findLineInFormula(formulaText, "exists");
            int absLine = (formulaStartLine > 0 ? formulaStartLine - 1 : f.startLine - 1) + (rel[0] >= 0 ? rel[0] : 0);
            int absCol = rel[1] >= 0 ? rel[1] : 0;
            String[] lines = formulaText.split("\n", -1);
            String offendingLine = (rel[0] >= 0 && rel[0] < lines.length) ? lines[rel[0]].trim() : "";
            msgs.add(new ErrRec(1, fileName, absLine, absCol, absCol + 7, errorMessage + offendingLine));
        }
    }

    /** ***************************************************************
     * Check for variables that appear only once in a formula.
     * @param fileName         logical filename
     * @param f                formula to check
     * @param formulaText      raw text of the formula
     * @param formulaStartLine buffer line offset
     * @param msgs             list to collect error records
     */
    public static void CheckSingleUseVariables(String fileName, Formula f, String formulaText, int formulaStartLine, List<ErrRec> msgs){
        
        String errorMessage = "Variable used only once - ";
        Set<String> singleUse = Diagnostics.singleUseVariables(f);
        if (singleUse != null) {
            for (String v : singleUse) {
                int[] rel = findLineInFormula(formulaText, v);
                int absLine = (formulaStartLine > 0 ? formulaStartLine - 1 : f.startLine - 1) + (rel[0] >= 0 ? rel[0] : 0);
                int absCol = rel[1] >= 0 ? rel[1] : 0;
                String[] lines = formulaText.split("\n", -1);
                String offendingLine = (rel[0] >= 0 && rel[0] < lines.length) ? lines[rel[0]].trim() : "";
                msgs.add(new ErrRec(1, fileName, absLine, absCol, absCol + v.length(), errorMessage + offendingLine));
            }
        }
    }

    /** ***************************************************************
     * Check for unquantified variables appearing in the consequent of implications.
     * @param fileName         logical filename
     * @param f                formula to check
     * @param formulaText      raw text of the formula
     * @param formulaStartLine buffer line offset
     * @param msgs             list to collect error records
     */
    public static void CheckUnquantInConsequent(String fileName, Formula f, String formulaText, int formulaStartLine, List<ErrRec> msgs){
        
        String errorMessage = "Unquantified variable in consequent - ";
        Set<String> unquant = Diagnostics.unquantInConsequent(f);
        if (unquant != null) {
            for (String uq : unquant) {
                int[] rel = findLineInFormula(formulaText, uq);
                int absLine = (formulaStartLine > 0 ? formulaStartLine - 1 : f.startLine - 1) + (rel[0] >= 0 ? rel[0] : 0);
                int absCol = rel[1] >= 0 ? rel[1] : 0;
                String[] lines = formulaText.split("\n", -1);
                String offendingLine = (rel[0] >= 0 && rel[0] < lines.length) ? lines[rel[0]].trim() : "";
                msgs.add(new ErrRec(0, fileName, absLine, absCol, absCol + uq.length(), errorMessage + offendingLine));
            }
        }
    }

    /** ***************************************************************
     * Run FormulaPreprocessor and record errors/warnings.
     * @param fileName         logical filename
     * @param kb               knowledge base
     * @param f                formula to preprocess
     * @param formulaStartLine buffer line offset
     * @param msgs             list to collect error records
     * @return processed formula set
     */
    public static Set<Formula> CheckFormulaPreprocess(String fileName, KB kb, Formula f, int formulaStartLine, List<ErrRec> msgs){
        
        FormulaPreprocessor fp = SUMOtoTFAform.fp;
        Set<Formula> processed = fp.preProcess(f, false, kb);
        if (f.errors != null)
            for (String er : f.errors)
                msgs.add(new ErrRec(0, fileName, (formulaStartLine > 0 ? formulaStartLine - 1 : f.startLine - 1), 1, 2, er));
        if (f.warnings != null)
            for (String w : f.warnings)
                msgs.add(new ErrRec(1, fileName, (formulaStartLine > 0 ? formulaStartLine - 1 : f.startLine - 1), 1, 2, w));
        return processed;
    }

    /** ***************************************************************
     * Check for translation errors after SUMO → TFA conversion.
     * @param fileName         logical filename
     * @param f                formula
     * @param formulaStartLine buffer line offset
     * @param processed        processed formulas
     * @param msgs             list to collect error records
     */
    private static void CheckSUMOtoTFAformErrors(String fileName, Formula f, int formulaStartLine, Set<Formula> processed, List<ErrRec> msgs) {

        if (SUMOtoTFAform.errors != null && !SUMOtoTFAform.errors.isEmpty() && processed.size() == 1) {
            int line = (formulaStartLine > 0 ? formulaStartLine - 1 : f.startLine - 1);
            for (String er : SUMOtoTFAform.errors)
                msgs.add(new ErrRec(0, fileName, line, 1, 2, er));
            SUMOtoTFAform.errors.clear();
        }
    }

    /** ***************************************************************
     * Check if the formula is structurally valid in the KB.
     * @param fileName         logical filename
     * @param f                formula
     * @param formulaStartLine buffer line offset
     * @param kb               knowledge base
     * @param formulaText      raw text of the formula
     * @param msgs             list to collect error records
     */
    private static void CheckIsValidFormula(String fileName, Formula f, int formulaStartLine, KB kb, String formulaText, List<ErrRec> msgs) {
        
        if (!KButilities.isValidFormula(kb, formulaText)) {
            int line = (formulaStartLine > 0 ? formulaStartLine - 1 : f.startLine - 1);
            for (String er : KButilities.errors)
                msgs.add(new ErrRec(0, fileName, line, 1, 2, er));
            KButilities.errors.clear();
        }
    }

    /** ***************************************************************
     * Check that all terms are below Entity in the KB hierarchy.
     * @param fileName         logical filename
     * @param f                formula
     * @param formulaStartLine buffer line offset
     * @param formulaText      raw text of the formula
     * @param kb               knowledge base
     * @param localIndividuals set of locally defined individuals
     * @param localSubclasses  set of locally defined subclasses
     * @param msgs             list to collect error records
     */
    private static void CheckTermsBelowEntity(String fileName, Formula f, int formulaStartLine, String formulaText, KB kb, Set<String> localIndividuals, Set<String> localSubclasses, List<ErrRec> msgs) {
        
        for (String t : f.collectTerms()) {
            if (Diagnostics.LOG_OPS.contains(t) || "Entity".equals(t) || Formula.isVariable(t) || StringUtil.isNumeric(t) || StringUtil.isQuotedString(t)) continue;
            boolean coveredByLocal = localIndividuals.contains(t) || localSubclasses.contains(t);
            if (!coveredByLocal && Diagnostics.termNotBelowEntity(t, kb)) {
                int[] rel = findLineInFormula(formulaText, t);
                int absLine = (formulaStartLine > 0 ? formulaStartLine - 1 : f.startLine - 1) + (rel[0] >= 0 ? rel[0] : 0);
                int absCol = rel[1] >= 0 ? rel[1] : 0;
                String[] lines = formulaText.split("\n", -1);
                String offendingLine = (rel[0] >= 0 && rel[0] < lines.length) ? lines[rel[0]].trim() : "";
                msgs.add(new ErrRec(0, fileName, absLine, absCol, absCol + t.length(), "Term not below Entity: " + offendingLine));
            }
        }
    }

    /** ***************************************************************
     * Sort diagnostic messages by line, column, type, and text.
     * @param msgs list of error records to sort
     */
    private static void SortMessages(List<ErrRec> msgs) {
        
        msgs.sort((e1, e2) -> {
            int c = Integer.compare(e1.line, e2.line);
            if (c != 0) return c;
            c = Integer.compare(e1.start, e2.start);
            if (c != 0) return c;
            c = Integer.compare(e1.type, e2.type);
            if (c != 0) return c;
            return e1.msg.compareTo(e2.msg);
        });
    }

    /** ***************************************************************
     * Check for syntax errors during parsing.
     * @param contents KIF text
     * @param fileName logical filename
     * @param msgs     list to collect error records
     */
    public static void CheckSyntaxErrors(String contents, String fileName, List<ErrRec> msgs) {

        SuokifVisitor sv = SuokifApp.process(contents);
        if (!sv.errors.isEmpty()) {
            for (String er : sv.errors) {
                int line = getLineNum(er);
                int offset  = getOffset(er);
                msgs.add(new ErrRec(1, fileName, (line == 0 ? line : line - 1), Math.max(offset, 1), Math.max(offset, 1) + 1, er));
            }
        }
    }
 
    /** ***************************************************************
     * Convert raw KIF string into a KIF object, collecting parse errors.
     * @param contents KIF text
     * @param fileName logical filename
     * @param msgs     list to collect error records
     * @return parsed KIF object
     */
    public static KIF StringToKif(String contents, String fileName, List<ErrRec> msgs) {

        KIF localKif = new KIF();
        try (Reader r = new StringReader(contents)) {
            localKif.parse(r);
            for (String er : localKif.errorSet)
                msgs.add(new ErrRec(0, fileName, 0, 0, 1, er));
        } catch (Exception e) {
            msgs.add(new ErrRec(1, fileName, 0, 0, 1, "Parse error: " + e.getMessage()));
        }
        return localKif;
    }

    /** ***************************************************************
     * Extract a formula's exact text between line numbers.
     *
     * @param bufferLines full text split into lines
     * @param startLine   starting line (1-based)
     * @param endLine     ending line
     * @return text slice containing the formula
     */
    private static String extractBufferSlice(String[] bufferLines, int startLine, int endLine) {
        if (startLine < 1 || endLine < startLine || startLine > bufferLines.length) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = startLine - 1; i < Math.min(endLine, bufferLines.length); i++) {
            sb.append(bufferLines[i]).append("\n");
        }
        return sb.toString();
    }

    /** ***************************************************************
     * Find the relative line and column offset of a substring (e.g., error term)
     * within a multi-line formula string.
     * @param formulaText the full text of the formula
     * @param term        the substring or token we are trying to locate (e.g. error term)
     * @return an int array [lineOffsetWithinFormula, columnOffsetWithinLine], or [-1,-1] if not found.
     */
    private static int[] findLineInFormula(String formulaText, String term) {

        if (formulaText == null || term == null || term.isEmpty())
            return new int[]{-1, -1};
        String[] lines = formulaText.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int searchStart = 0;
            while (searchStart < line.length()) {
                int pos = line.indexOf(term, searchStart);
                if (pos == -1) break;
                boolean validStart = (pos == 0 || !isTermChar(line.charAt(pos - 1)));
                boolean validEnd = (pos + term.length() >= line.length() || !isTermChar(line.charAt(pos + term.length())));
                if (validStart && validEnd)
                    return new int[]{i, pos};
                searchStart = pos + 1;
            }
        }
        return new int[]{-1, -1};
    }

    private static boolean isTermChar(char c) {
        return Character.isLetterOrDigit(c) || c == '.' || c == '_';
    }

    /** ***************************************************************
     * Recursively collect local individuals and subclasses from formulas.
     *
     * @param f                 formula to traverse
     * @param localIndividuals  set to populate with individuals
     * @param localSubclasses   set to populate with subclasses
     */
    private static void harvestLocalFacts(Formula f, Set<String> localIndividuals, Set<String> localSubclasses) {

        if (f == null || f.atom()) return;
        String functor = f.car();
        List<String> args = f.argumentsToArrayListString(1);
        if (args != null && args.size() == 2){
            String indivOrChild = args.get(0), parentClass = args.get(1);
            if(isConst(indivOrChild) && isConst(parentClass)){
                if ("instance".equals(functor)) {
                    localIndividuals.add(indivOrChild);
                } else if ("subclass".equals(functor) && args != null && args.size() == 2) {
                    localSubclasses.add(indivOrChild);
                }
            }
        }
        if (f.listP())
            for (int i = 1; i < f.listLength(); i++) 
                harvestLocalFacts(f.getArgument(i), localIndividuals, localSubclasses);
    }

    /** ***************************************************************
     * Check if a token represents a constant rather than a variable or literal.
     * @param tok token string
     * @return true if constant, false otherwise
     */
    private static boolean isConst(String tok) {
        return !(Formula.isVariable(tok) || StringUtil.isNumeric(tok) || StringUtil.isQuotedString(tok));
    }

    /** ***************************************************************
     * Find where a formula string appears in the buffer.
     * @param formulaStr  formula text
     * @param bufferLines full text split into lines
     * @return 1-based starting line index, or -1 if not found
     */
    private static int findFormulaInBuffer(String formulaStr, String[] bufferLines) {

        if (formulaStr == null || formulaStr.isEmpty()) return -1;
        String[] rawFormulaLines = formulaStr.split("\\R");
        List<String> formulaLines = new ArrayList<>();
        for (String line : rawFormulaLines) {
            String t = line.trim();
            if (!t.isEmpty()) {
                formulaLines.add(t);
            }
        }
        if (formulaLines.isEmpty()) return -1;
        for (int i = 0; i <= bufferLines.length - formulaLines.size(); i++) {
            boolean match = true;
            for (int j = 0; j < formulaLines.size(); j++) {
                String bufLine = bufferLines[i + j].trim();
                if (!bufLine.contains(formulaLines.get(j))) {
                    match = false;
                    break;
                }
            }
            if (match)
                return i + 1;
        }
        return -1;
    }

    /** ***************************************************************
     * sigmaAntlr generates line offsets
     * @param line error line text
     * @return the line offset of where the error/warning begins
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

    /** ***************************************************************
     * Parses KIF content into a KIF object and collects warnings/errors as ErrRec.
     * @param kif target KIF object
     * @param contents KIF text content
     * @param fileName the filename (used in ErrRec records)
     * @param msgs output list for formatted error/warning records
     * @return true if parse succeeded, false otherwise
     */
    private static boolean parseKif(KIF kif, String contents, String fileName, List<ErrRec> msgs) {

        boolean retVal = false;
        try (Reader r = new StringReader(contents)) {
            kif.parse(r);
            if (debug) System.out.println("parseKif(): done reading kif file");
            retVal = true;
        } catch (Exception e) {
            String msg = "Error in parseKif() with: " + kif.filename + ": " + e;
            System.err.println(msg);
            msgs.add(new ErrRec(0, fileName, 0, 1, 2, msg));
        } finally {
            for (String er : kif.errorSet) {
                int line = getLineNum(er);
                int col  = getOffset(er);
                msgs.add(new ErrRec(0, fileName, line == 0 ? line : line - 1, Math.max(col, 1), Math.max(col, 1) + 1, er));
            }
            for (String warn : kif.warningSet) {
                int line = getLineNum(warn);
                int col  = getOffset(warn);
                msgs.add(new ErrRec(1, fileName, line == 0 ? line : line - 1, Math.max(col, 1), Math.max(col, 1) + 1, warn));
            }
        }
        return retVal;
    }

    /**
     * Extract line number from a parser error line.
     * @param line error line text
     * @return line number or 0 if not found
     */
    private static int getLineNum(String line) {

        int result = -1;
        Pattern p = Pattern.compile("(\\d+):");
        Matcher m = p.matcher(line);
        if (m.find()) {
            try {
                result = Integer.parseInt(m.group(1));
            } catch (NumberFormatException nfe) {}
        }
        if (result < 0) {
            p = Pattern.compile("line(:?) (\\d+)");
            m = p.matcher(line);
            if (m.find()) {
                try {
                    result = Integer.parseInt(m.group(2));
                } catch (NumberFormatException nfe) {}
            }
        }
        if (result < 0 ) {
            p = Pattern.compile("line&#58; (\\d+)");
            m = p.matcher(line);
            if (m.find()) {
                try {
                    result = Integer.parseInt(m.group(1));
                } catch (NumberFormatException nfe) {}
            }
        }
        if (result < 0)
            result = 0;
        return result;
    }
}