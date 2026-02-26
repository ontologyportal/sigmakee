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
import java.io.File;
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
        System.out.println("  -o <file.kif>   Find Orphan Variables in the given KIF file");
        System.out.println("  -c <file.kif>   Check the given KIF file and print diagnostics");
        System.out.println("  -C <kifString>  Check the given KIF string and print diagnostics (filename = \"fileName\")");
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
                    for (Map.Entry<String, HashSet<String>> e : links.entrySet())
                        if (debug) System.out.println("  " + e.getKey() + " ↔ " + e.getValue());
                    if (debug) System.out.println("---------------------------------------------------");
                }
            } catch (Exception e) {
                System.err.println("Failed to process " + fname);
                e.printStackTrace();
            }
            return;
        }
        // NEW: check a raw KIF string
        if ("-C".equals(args[0]) && args.length > 1) {
            // Everything after -C is treated as the KIF string; simplest is args[1]
            // (If you want to support spaces w/out quotes, you could join args[1..] here.)
            String kifString = args[1];
            KifFileChecker kfc = new KifFileChecker();
            List<ErrRec> errors = kfc.check(kifString, "fileName");  // <- filename forced to "fileName"
            System.out.println("*******************************************************");
            if (errors.isEmpty()) {
                System.out.println("No errors found in fileName");
            } else {
                System.out.println("Diagnostics for fileName:");
                for (ErrRec e : errors) {
                    System.out.println(e.toString());
                }
            }
            return;
        }
        if ("-c".equals(args[0]) && args.length > 1) {
            String fname = args[1];
            try {
                KifFileChecker kfc = new KifFileChecker();
                String contents = String.join("\n", FileUtil.readLines(fname));
                List<ErrRec> errors = kfc.check(contents, fname);
                if (debug) System.out.println("*******************************************************");
                if (errors.isEmpty()) {
                    if (debug) System.out.println("No errors found in " + fname);
                } else {
                    if (debug) System.out.println("Diagnostics for " + fname + ":");
                    if (debug) for (ErrRec e : errors)
                        System.out.println(e.toString());
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
        // CheckSyntaxErrors(contents, fileName, msgs);
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
            // CheckSUMOtoTFAformErrors(fileName, kb, f, formulaStartLine, processed, msgs);
            CheckIsValidFormula(fileName, f, formulaStartLine, kb, formulaText, msgs);
            CheckTermsBelowEntity(fileName, f, formulaStartLine, formulaText, kb, localIndividuals, localSubclasses, msgs);
        }
        SortMessages(msgs);
        // Check if file is not in KB config and has "no type information" errors.
        // If so, prepend a single WARNING advising the user to add it.
        if (fileName != null && !fileName.equals("(buffer)") && !fileName.equals("fileName")) {
            if (!isFileInKB(fileName, kb)) {
                long typeErrCount = msgs.stream()
                    .filter(e -> e.type == ErrRec.ERROR && e.msg != null
                            && e.msg.contains("no type information for arg"))
                    .count();
                if (typeErrCount > 1) {
                    String basename = new File(fileName).getName();
                    String suggestion = "sigma-config.sh add " + fileName;
                    msgs.add(0, new ErrRec(
                        ErrRec.WARNING, fileName, 1, 1, 2,
                        "This file is not loaded into the KB. " + (int)typeErrCount
                        + " type errors may be false positives. "
                        + "To add permanently, run: " + suggestion));
                }
            }
        }
        if (debug) for (ErrRec e : msgs) {
            System.out.println(e);
        }
        return msgs;
    }

    /** ***************************************************************
     * Check whether the given file is loaded in the KB as a constituent.
     * Compares by basename since constituents may be stored as just
     * filenames or as full paths depending on config.
     *
     * @param fileName  the file path being checked
     * @param kb        the current KB (may be null)
     * @return true if the file is a constituent of the KB
     */
    public static boolean isFileInKB(String fileName, KB kb) {

        if (kb == null || fileName == null)
            return false;
        String basename = new File(fileName).getName();
        for (String constituent : kb.constituents) {
            String cBase = new File(constituent).getName();
            if (cBase.equals(basename))
                return true;
        }
        return false;
    }

    /** ***************************************************************
     * Pretty-print KIF contents using the KIF parser and Formula.toString().
     * Preserves top-level forms and tries to keep comments and blank lines
     * in roughly the same places.
     */
    /** ***************************************************************
     * Pretty-print KIF contents using the KIF parser and Formula.toString().
     *
     * Key behavior:
     *  - We only rewrite the top-level formula spans.
     *  - Everything outside those spans (comments, blank lines, etc.)
     *    is copied EXACTLY as in the original text.
     *  - If a formula span contains a ';' comment, we DO NOT reformat it.
     *    We keep the original slice to avoid moving inline comments.
     */
    public static String formatKif(String contents) {

        if (contents == null || contents.trim().isEmpty())
            return contents;

        // 1. Find top-level formula spans (character offsets).
        final List<int[]> spans = new ArrayList<>();
        scanTopLevelSpans(contents, spans);
        if (spans.isEmpty())
            return contents;

        // 2. Parse KIF to get canonical formula strings.
        KIF kif = new KIF();
        try (StringReader sr = new StringReader(contents)) {
            kif.parse(sr);
        } catch (Exception e) {
            System.err.println("KifFileChecker.formatKif(): parse failed - returning original: " + e.getMessage());
            return contents;
        }

        if (kif.formulasOrdered == null || kif.formulasOrdered.isEmpty())
            return contents;

        List<String> formattedForms = new ArrayList<>();
        for (Formula f : kif.formulasOrdered.values()) {
            String s = (f == null) ? "" : f.toString();
            formattedForms.add(s == null ? "" : s.trim());
        }

        // There may be mismatches; be defensive.
        int count = Math.min(spans.size(), formattedForms.size());
        StringBuilder out = new StringBuilder(contents.length() + 256);
        int cursor = 0;

        for (int i = 0; i < count; i++) {
            int[] span = spans.get(i);
            int spanStart = span[0];
            int spanEnd   = span[1];

            // Copy everything BEFORE this formula exactly as-is.
            if (cursor < spanStart) {
                out.append(contents, cursor, spanStart);
            }

            // --- NEW: extend span to end of line, but not into the next top-level formula ---
            int nextSpanStart = (i + 1 < count) ? spans.get(i + 1)[0] : contents.length();
            int endOfLine = contents.indexOf('\n', spanEnd);
            if (endOfLine == -1) {
                endOfLine = contents.length();
            } else {
                // include the newline as part of this slice
                endOfLine = endOfLine;  // keep as position *before* '\n'; tail append will add it
            }
            int extendedEnd = Math.min(endOfLine, nextSpanStart);

            if (extendedEnd < spanEnd) {
                extendedEnd = spanEnd;  // safety, should not happen
            }

            String rawFormula = contents.substring(spanStart, Math.min(extendedEnd, contents.length()));
            String formatted  = formattedForms.get(i);

            // If the original slice contains a ';' comment anywhere,
            // we DO NOT reformat it. This preserves inline and internal comments.
            if (rawFormula.indexOf(';') >= 0) {
                out.append(rawFormula);
            } else {
                // Reformat-only formulas with no comments.
                if (formatted != null && !formatted.isEmpty()) {
                    out.append(formatted);
                    // DO NOT force a newline here; tail copy will preserve original whitespace.
                } else {
                    out.append(rawFormula);
                }
            }
            cursor = extendedEnd;
        }

        // 3. Append the tail (after the last formula) exactly as-is.
        if (cursor < contents.length()) {
            out.append(contents.substring(cursor));
        }

        String result = out.toString();
        return result.trim().isEmpty() ? contents : result;
    }

    /** ***************************************************************
     * Scan KIF text to find top-level formula spans.
     * Each span is [startIndex, endIndex) in the original string.
     *
     * - We track parentheses depth.
     * - We ignore any parentheses that occur inside ';' comments or strings.
     * - This is used ONLY to locate the outermost formula blocks.
     */
    private static void scanTopLevelSpans(String s, List<int[]> spans) {

        int n = s.length();
        int depth = 0;
        int i = 0;
        int currentSpanStart = -1;
        boolean inString = false;
        boolean escaping = false;

        while (i < n) {
            char ch = s.charAt(i);
            if (ch == '"' && !escaping) {
                inString = !inString;
                i++;
                continue;
            }
            escaping = (ch == '\\' && !escaping);
            if (!inString) {
                if (ch == ';') {
                    int j = i + 1;
                    while (j < n) {
                        char cj = s.charAt(j);
                        if (cj == '\n' || cj == '\r')
                            break;
                        j++;
                    }
                    i = j;
                    continue;
                }
                if (ch == '\r') {
                    i++;
                    continue;
                }
                if (ch == '(') {
                    if (depth == 0) {
                        currentSpanStart = i;
                    }
                    depth++;
                } else if (ch == ')') {
                    depth = Math.max(0, depth - 1);
                    if (depth == 0 && currentSpanStart >= 0) {
                        spans.add(new int[]{ currentSpanStart, i + 1 });
                        currentSpanStart = -1;
                    }
                }
            }
            i++;
        }
    }

    /** ***************************************************************
     * Strip KIF-style line comments (starting with ';' and running to
     * end-of-line), but *ignore* ';' inside double-quoted strings.
     *
     * This is used only for feeding clean text into KButilities, while
     * keeping the original text for line/column mapping and messages.
     */
    private static String stripKifComments(String text) {

        if (text == null || text.isEmpty())
            return "";

        StringBuilder out = new StringBuilder(text.length());
        String[] lines = text.split("\\R", -1);   // keep empty lines

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            boolean inString = false;
            boolean escaping = false;
            int cutPos = line.length();

            for (int j = 0; j < line.length(); j++) {
                char c = line.charAt(j);

                // Handle string toggling & escaping
                if (c == '"' && !escaping) {
                    inString = !inString;
                }
                escaping = (c == '\\' && !escaping);

                // First ';' outside of a string starts a comment
                if (!inString && c == ';') {
                    cutPos = j;
                    break;
                }
            }

            // Append code portion (before ';' if present)
            out.append(line, 0, cutPos);

            // Re-add newline between lines (but not after last)
            if (i < lines.length - 1) {
                out.append('\n');
            }
        }

        return out.toString();
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
                int absLine = (formulaStartLine > 0 ? formulaStartLine : f.startLine) + rel[0];
                int absCol = rel[1];
                String[] lines = formulaText.split("\n", -1);
                String offendingLine = (rel[0] >= 0 && rel[0] < lines.length) ? lines[rel[0]].trim() : "";
                msgs.add(new ErrRec(0, fileName, absLine, absCol, absCol + foundQuant.length(), errorMessage + offendingLine));
            } else {
                errorMessage += " (no quantifier found in text)";
                msgs.add(new ErrRec(0, fileName, f.startLine, 0, 1, errorMessage));
            }
        }
    }

    /**
     * Check for disconnected variable groups
     * @param fileName          logical filename
     * @param f                 formula to check
     * @param formulaText       raw text of the formula
     * @param formulaStartLine  buffer line offset of the formula
     * @param msgs              list to collect error records
     */
    public static void CheckOrphanVars(String fileName, Formula f, String formulaText, int formulaStartLine, List<ErrRec> msgs) {
        
        HashMap<String, HashSet<String>> cooccurs = Diagnostics.findOrphanVars(f);
        if (!cooccurs.isEmpty()) {
            ArrayList<HashSet<String>> groups = Diagnostics.findDisconnectedGroups(cooccurs);
            ArrayList<HashSet<String>> multiVarGroups = new ArrayList<>();
            for (HashSet<String> group : groups)
                if (group.size() > 1)
                    multiVarGroups.add(group);
            if (multiVarGroups.size() > 1) {
                StringBuilder groupMsg = new StringBuilder();
                groupMsg.append("Formula has ").append(multiVarGroups.size()).append(" disconnected variable groups: ");   
                for (int i = 0; i < multiVarGroups.size(); i++) {
                    groupMsg.append("Group ").append(i + 1).append(": ").append(multiVarGroups.get(i));
                    if (i < multiVarGroups.size() - 1)
                        groupMsg.append("; ");
                }
                int absLine;
                if (formulaStartLine > 0) {
                    absLine = formulaStartLine;
                } else {
                    absLine = f.startLine;
                }
                msgs.add(new ErrRec(0, fileName, absLine, 0, formulaText.length(), groupMsg.toString()));
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
            int absLine = (formulaStartLine > 0 ? formulaStartLine : f.startLine) + (rel[0] >= 0 ? rel[0] : 0);
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
                int absLine = (formulaStartLine > 0 ? formulaStartLine : f.startLine) + (rel[0] >= 0 ? rel[0] : 0);
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
                int absLine = (formulaStartLine > 0 ? formulaStartLine : f.startLine) + (rel[0] >= 0 ? rel[0] : 0);
                int absCol = rel[1] >= 0 ? rel[1] : 0;
                String[] lines = formulaText.split("\n", -1);
                String offendingLine = (rel[0] >= 0 && rel[0] < lines.length) ? lines[rel[0]].trim() : "";
                msgs.add(new ErrRec(0, fileName, absLine, absCol, absCol + uq.length(), errorMessage + offendingLine));
            }
        }
    }

    /** ***************************************************************
     * Run FormulaPreprocessor and record errors/warnings.
     * Computes accurate absolute line/column using findLineInFormula()
     * by locating the offending token inside the formula text.
     * @param fileName         logical filename
     * @param kb               knowledge base
     * @param f                formula to preprocess
     * @param formulaStartLine buffer line offset
     * @param msgs             list to collect error records
     * @return processed formula set
     */
    public static Set<Formula> CheckFormulaPreprocess(String fileName, KB kb, Formula f,
                                                    int formulaStartLine, List<ErrRec> msgs) {

        FormulaPreprocessor fp = SUMOtoTFAform.fp;
        Set<Formula> processed = fp.preProcess(f, false, kb);
        String formulaText = f.getFormula();
        if (formulaText == null) formulaText = "";
        java.util.function.Function<String, String> extractToken = (String msg) -> {
            Matcher m = Pattern.compile("relation\\s+([\\w-]+)").matcher(msg);
            if (m.find()) return m.group(1);
            m = Pattern.compile("([\\w-]+)\\s+in formula").matcher(msg);
            if (m.find()) return m.group(1);
            m = Pattern.compile("\\(([\\w-]+)\\s").matcher(msg);
            if (m.find()) return m.group(1);
            return null;
        };
        if (f.errors != null) {
            for (String er : f.errors) {
                if (debug) System.out.println("CheckFormulaPreprocess(): addingError = " + er);
                String token = extractToken.apply(er);
                int[] rel = {-1, -1};
                if (token != null)
                    rel = findLineInFormula(formulaText, token);
                if (debug) System.out.println("CheckFormulaPreprocess(): rel = " + rel);
                int absLine = (formulaStartLine > 0 ? formulaStartLine : f.startLine)
                            + ((rel[0] >= 0) ? rel[0] : 0);
                int absCol = (rel[1] >= 0) ? rel[1] : 1;

                msgs.add(new ErrRec(
                    0, fileName,
                    absLine, absCol, absCol + (token != null ? token.length() : 1),
                    er
                ));
            }
        }
        if (f.warnings != null) {
            for (String w : f.warnings) {
                if (debug) System.out.println("CheckFormulaPreprocess(): addingWarning = " + w);
                String token = extractToken.apply(w);
                int[] rel = {-1, -1};
                if (token != null)
                    rel = findLineInFormula(formulaText, token);

                int absLine = (formulaStartLine > 0 ? formulaStartLine : f.startLine)
                            + ((rel[0] >= 0) ? rel[0] : 0);
                int absCol = (rel[1] >= 0) ? rel[1] : 1;

                msgs.add(new ErrRec(
                    1, fileName,
                    absLine, absCol, absCol + (token != null ? token.length() : 1),
                    w
                ));
            }
        }

        return processed;
    }

    /**
     * Correct invocation of SUMO→TFA translation errors.
     * Mirrors SUMOjEdit.checkErrorsBody() behavior.
     *
     * Steps:
     *   (1) Clear errors for this formula
     *   (2) Run SUMOtoTFAform.process(f,false)
     *   (3) Collect any populated SUMOtoTFAform.errors
     *   (4) Compute line/column using token matching
     *   (5) Clear for next formula
     */
    public static void CheckSUMOtoTFAformErrors(
            String fileName,
            KB kb,
            Formula f,
            int formulaStartLine,
            Set<Formula> processed,
            List<ErrRec> msgs) {
        
        FormulaPreprocessor fp = SUMOtoTFAform.fp;
        SUMOtoTFAform.errors.clear();
        processed = fp.preProcess(f, false, kb);
        try {
            SUMOtoTFAform.process(f, false);
        } catch (Exception ex) {
            SUMOtoTFAform.errors.add("TFA translation exception: " + ex.getMessage());
        }
        if (SUMOtoTFAform.errors.isEmpty())
            return;
        int line = (formulaStartLine > 0 ? formulaStartLine : f.startLine);
        if (line < 0) line = 0;
        for (String er : SUMOtoTFAform.errors)
            msgs.add(new ErrRec(ErrRec.ERROR, fileName, line,0, 1, er));
        SUMOtoTFAform.errors.clear();
    }

    /** ***************************************************************
     * Check if the formula is structurally valid in the KB.
     * Computes absolute line/column by matching the offending token
     * inside the formula text using findLineInFormula().
     *
     * @param fileName         logical filename
     * @param f                formula
     * @param formulaStartLine buffer line offset
     * @param kb               knowledge base
     * @param formulaText      raw text of the formula
     * @param msgs             list to collect error records
     */
public static void CheckIsValidFormula(String fileName,
                                       Formula f,
                                       int formulaStartLine,
                                       KB kb,
                                       String formulaText,
                                       List<ErrRec> msgs) {

    if (formulaText == null)
        formulaText = "";

    // Strip KIF comments for the *validation* call only
    String codeOnly = stripKifComments(formulaText).trim();
    if (codeOnly.isEmpty()) {
        return;
    }

    if (!KButilities.isValidFormula(kb, codeOnly)) {
        for (String er : KButilities.errors) {

            String token = null;
            Matcher m = Pattern.compile("relation\\s+([\\w-]+)").matcher(er);
            if (m.find())
                token = m.group(1);
            else {
                m = Pattern.compile("[:\\s]\\s*([A-Za-z][A-Za-z0-9_-]*)\\)?$").matcher(er);
                if (m.find()) token = m.group(1);
            }

            int[] rel = {-1, -1};
            if (token != null)
                rel = findLineInFormula(formulaText, token);  // still use ORIGINAL text

            int absLine = (formulaStartLine > 0 ? formulaStartLine : f.startLine)
                        + ((rel[0] >= 0) ? rel[0] : 0);
            int absCol = (rel[1] >= 0) ? rel[1] : 1;

            String[] lines = formulaText.split("\n", -1);
            String offendingLine =
                (rel[0] >= 0 && rel[0] < lines.length)
                    ? lines[rel[0]].trim()
                    : formulaText.trim();

            msgs.add(new ErrRec(
                0,
                fileName,
                absLine,
                absCol,
                absCol + (token != null ? token.length() : 1),
                er + (token != null ? " [token: " + token + "] " : "") + " " + offendingLine
            ));
        }
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
    public static void CheckTermsBelowEntity(String fileName, Formula f, int formulaStartLine, String formulaText, KB kb, Set<String> localIndividuals, Set<String> localSubclasses, List<ErrRec> msgs) {
        
        for (String t : f.collectTerms()) {
            if (Diagnostics.LOG_OPS.contains(t) || "Entity".equals(t) || Formula.isVariable(t) || StringUtil.isNumeric(t) || StringUtil.isQuotedString(t)) continue;
            boolean coveredByLocal = localIndividuals.contains(t) || localSubclasses.contains(t);
            if (!coveredByLocal && Diagnostics.termNotBelowEntity(t, kb)) {
                int[] rel = findLineInFormula(formulaText, t);
                int absLine = (formulaStartLine > 0 ? formulaStartLine : f.startLine) + (rel[0] >= 0 ? rel[0] : 0);
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
                msgs.add(new ErrRec(ErrRec.ERROR, fileName, (line == 0 ? line : line), Math.max(offset, 1), Math.max(offset, 1) + 1, er));
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
    public static KIF StringToKif(String contents, String fileName, List<ErrRec> errorList) {

        KIF localKif = new KIF();
        try (Reader r = new StringReader(contents)) {
            localKif.parse(r);
            for (String er : localKif.errorSet) {
                ErrRec rec = parseKifError(er, fileName);
                errorList.add(rec);
            }
        } catch (Exception e) {
            ErrRec rec = new ErrRec(
                ErrRec.ERROR,
                fileName,
                0,
                0,
                1,
                e.getMessage()
            );
            errorList.add(rec);
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
    public static String extractBufferSlice(String[] bufferLines, int startLine, int endLine) {
    
        if (startLine < 1 || endLine < startLine || startLine > bufferLines.length)
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = startLine - 1; i < Math.min(endLine, bufferLines.length); i++)
            sb.append(bufferLines[i]).append("\n");
        return sb.toString();
    }

    /** ***************************************************************
     * Find the relative line and column offset of a substring (e.g., error term)
     * within a multi-line formula string.
     * @param formulaText the full text of the formula
     * @param term        the substring or token we are trying to locate (e.g. error term)
     * @return an int array [lineOffsetWithinFormula, columnOffsetWithinLine], or [-1,-1] if not found.
     */
    public static int[] findLineInFormula(String formulaText, String term) {

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
    public static void harvestLocalFacts(Formula f, Set<String> localIndividuals, Set<String> localSubclasses) {

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
    public static boolean isConst(String tok) {
        
        return !(Formula.isVariable(tok) || StringUtil.isNumeric(tok) || StringUtil.isQuotedString(tok));
    }

    /** ***************************************************************
     * Find where a formula string appears in the buffer.
     * @param formulaStr  formula text
     * @param bufferLines full text split into lines
     * @return 1-based starting line index, or -1 if not found
     */
    public static int findFormulaInBuffer(String formulaStr, String[] bufferLines) {

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
                msgs.add(new ErrRec(0, fileName, line == 0 ? line : line, Math.max(col, 1), Math.max(col, 1) + 1, er));
            }
            for (String warn : kif.warningSet) {
                int line = getLineNum(warn);
                int col  = getOffset(warn);
                msgs.add(new ErrRec(1, fileName, line == 0 ? line : line, Math.max(col, 1), Math.max(col, 1) + 1, warn));
            }
        }
        return retVal;
    }

    /** ***************************************************************
     * sigmaAntlr generates line offsets
     * @param line error line text
     * @return the line offset of where the error/warning begins
     */
    public static int getOffset(String line) {

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
     * Extract line number from a parser error line.
     * @param line error line text
     * @return line number or 0 if not found
     */
    public static int getLineNum(String line) {

        if (debug) System.out.println("KifFileChecker.getLineNumber(" + line + ")");
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

    public static ErrRec parseKifError(String raw, String fileName) {

        int line = 0;
        int start = 0;
        int end = 1;
        Pattern p1 = Pattern.compile("near line[: ]+([0-9]+)");
        Matcher m1 = p1.matcher(raw);
        if (m1.find())
            line = Integer.parseInt(m1.group(1));
        Pattern p2 = Pattern.compile("line[: ]*([0-9]+)[^0-9]+([0-9]+)");
        Matcher m2 = p2.matcher(raw);
        if (m2.find()) {
            line = Integer.parseInt(m2.group(1));
            start = Integer.parseInt(m2.group(2));
            end = start + 1;
        }
        Pattern p3 = Pattern.compile("line[ ]+([0-9]+)");
        Matcher m3 = p3.matcher(raw);
        if (m3.find() && line == 0)
            line = Integer.parseInt(m3.group(1));
        if (end < start) end = start + 1;
        return new ErrRec(ErrRec.ERROR, fileName, line, start, end, raw);
    }
}