package com.articulate.sigma;

import java.util.*;
import java.util.regex.Pattern;



/**
 * Validates a .kif file using KButilities and also enforces
 * class/individual/attribute roles using a local symbol table
 * built from the uploaded file itself.
 * Error Types:
 *      - Parenthesis balance 
 *      - Type Checking []
 *      - Term below entity [GOOD]
 *      - Argument Number (Arity) [GOOD]
 *      - Variable quantification
 */
public class KifFileChecker_Copy {

    static boolean debug = false;
    private static final class FormSpan { final String text; final int startLine; FormSpan(String t, int s){ text=t; startLine=s; } }
    private static final boolean LOG_EXPECTED_TYPES = true;
    private static final Set<String> LOG_PREDICATES = new HashSet<>();

    private static final class LocalFacts {
        final Set<String> localsAreClasses = new HashSet<>();
        final Set<String> localsAreIndividuals = new HashSet<>();
        final Set<String> localsAreAttributes = new HashSet<>();
    }

    /**
     * Validates a list of SUO-KIF formulas line by line against the knowledge base.
     *
     * Behavior:
     * - Builds a local symbol table (`LocalFacts`) of classes, individuals, and attributes
     *   declared in the file.
     * - Splits the file into balanced KIF forms with their starting line numbers.
     * - For each form:
     *     • Skips comments and blank lines.
     *     • Validates syntax with `KButilities.isValidFormula`.
     *         – If invalid, collects error messages.
     *     • Converts the form into a `Formula` object.
     *     • Optionally logs expected type signatures for watched predicates.
     *     • Runs type checking (`KButilities.hasCorrectTypes`) and collects errors.
     *     • Runs semantic role checks (`roleChecksRec`) to ensure:
     *         – individuals vs. classes are used in the correct argument positions,
     *         – attributes are used where expected,
     *         – and optionally that classes are under `Entity`.
     * - Returns all collected messages; if none, the file is considered valid.
     * 
     * @param kb           The knowledge base used for syntax, type, and role checks.
     * @param lines        The raw KIF file contents, one string per line.
     * @param includeBelow If true, enforces "term must be below Entity" checks on classes.
     *
     * @return A list of error or info messages, each prefixed with a line number.
     *
     */
    public static List<String> check(KB kb, List<String> lines, boolean includeBelow) {
        // System.out.println("KifFileChecker: check() Checking lines : \n" + lines);
        if (lines == null) return Collections.emptyList();
        LocalFacts lf = buildLocalFacts(kb, lines);
        List<String> results = new ArrayList<>();
        KButilities.clearErrors();
        List<FormSpan> forms = splitKifFormsWithLines(String.join("\n", lines));
        for (FormSpan fs : forms) {
            String src = fs.text.trim();
            if (src.isEmpty() || src.startsWith(";")) continue;
            if (!KButilities.isValidFormula(kb, src)) {
                System.out.println("KifFileChecker.java check(): " + src);
                for (String err : KButilities.errors) {
                    results.add("Line " + fs.startLine + ": " + err);
                }
                KButilities.clearErrors();
                continue;
            }
            Formula f = new Formula(src);
            // if (LOG_EXPECTED_TYPES) {
            //     dumpExpectedTypesRec(kb, f, fs.startLine, results, LOG_PREDICATES);
            // }
            if (!KButilities.hasCorrectTypes(kb, f)) {
                for (String err : KButilities.errors) {
                    results.add("Line " + fs.startLine + ": " + err);
                }
                KButilities.clearErrors();
            }
            checkArgumentTypes(kb, f, fs.startLine, results);

            if ("names".equals(f.car())) {
                List<String> args = f.argumentsToArrayListString(1);
                if (args != null && args.size() == 2) {
                    String s = args.get(0);
                    if (s != null && !s.startsWith("\"")) {
                        results.add("Line " + fs.startLine + ": 'names' arg1 must be a quoted string → " + f.getFormula());
                    }
                }
            }
            //
            Set<String> unquantified = f.collectUnquantifiedVariables();
            if (!f.isRule() && !unquantified.isEmpty()) {
                results.add("Line " + fs.startLine + ": Unquantified variables → " + unquantified);
            }
            roleChecksRec(kb, f, fs.startLine, lf, results, includeBelow);
        }
        return results;
    }

    private static void checkArgumentTypes(KB kb, Formula f, int lineNo, List<String> results) {
        if (!f.isSimpleClause(kb)) return;

        String pred = f.getStringArgument(0);
        List<String> sig = kb.kbCache.getSignature(pred);
        List<String> args = f.argumentsToArrayListString(1);

        if (sig == null || args == null) return;

        for (int i = 0; i < args.size(); i++) {
            String expected = sig.size() > i+1 ? sig.get(i+1) : null;
            String actual = args.get(i);

            if ("SymbolicString".equals(expected) && !isQuoted(actual)) {
                results.add("Line " + lineNo + ": " + pred + " arg" + (i+1) +
                            " must be a quoted string → " + actual);
            }
            if ("Class".equals(expected) && !isKnownClass(kb, actual, new LocalFacts())) {
                results.add("Line " + lineNo + ": " + pred + " arg" + (i+1) +
                            " must be a Class, found " + actual);
            }
            if ("Entity".equals(expected) && isKnownClass(kb, actual, new LocalFacts())) {
                results.add("Line " + lineNo + ": " + pred + " arg" + (i+1) +
                            " must be an Entity, but looks like a Class → " + actual);
            }
        }
    }


    /**
     * Builds a table of locally declared facts (classes, individuals, attributes)
     * from the given KIF source lines.
     * 
     * Behavior:
     * - Joins the lines and splits them into balanced forms with line numbers.
     * - Skips empty lines, comments, and malformed formulas.
     * - Parses each valid form into a Formula and delegates to
     *   collectLocalFactsRec() to classify terms.
     * 
     * @param kb     The knowledge base to use for validating formulas.
     * @param lines  The source file as a list of lines (Strings).
     * @return A LocalFacts object containing sets of symbols that appear
     *         locally as classes, individuals, or attributes.
     */
    private static LocalFacts buildLocalFacts(KB kb, List<String> lines) {
        LocalFacts lf = new LocalFacts();

        List<FormSpan> forms = splitKifFormsWithLines(String.join("\n", lines));
        for (FormSpan fs : forms) {
            String t = fs.text.trim();
            if (t.isEmpty() || t.startsWith(";")) continue;
            if (!KButilities.isValidFormula(kb, t)) continue; // skip malformed while building table
            Formula f = new Formula(t);
            collectLocalFactsRec(kb, f, lf);
        }
        return lf;
    }

    private static boolean isQuoted(String s) {

        return s != null && s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"");
    }

    /**
     * Splits a block of SUO-KIF text into balanced top-level forms,
     * preserving their starting line numbers.
     *
     * Behavior:
     * - Strips ';' comments (outside of quoted strings).
     * - Tracks parentheses depth to detect balanced forms.
     * - Respects quoted strings so parentheses inside them don't affect depth.
     * - Groups consecutive lines until a balanced form is complete.
     * 
     * @param text  The raw KIF source text (may contain newlines, comments, and strings).
     * @return A list of FormSpan objects, each holding one complete top-level
     *         form (String) and the 1-based line number where that form starts.
     */
    private static List<FormSpan> splitKifFormsWithLines(String text) {
        List<FormSpan> forms = new ArrayList<>();
        String[] lines = text.replace("\r\n","\n").replace("\r","\n").split("\n", -1);
        StringBuilder cur = new StringBuilder();
        int depth = 0; boolean inStr = false; int formStart = 1;
        for (int ln = 0; ln < lines.length; ln++) {
            String rawLine = lines[ln];
            String line = rawLine;
            // strip ; comments outside strings
            StringBuilder sb = new StringBuilder();
            boolean cut = false;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"' && (i == 0 || line.charAt(i-1) != '\\')) inStr = !inStr;
                if (!inStr && c == ';') { cut = true; break; }
                sb.append(c);
            }
            if (cut) line = sb.toString();
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"' && (i == 0 || line.charAt(i-1) != '\\')) inStr = !inStr;
                else if (!inStr) {
                    if (c == '(') depth++;
                    else if (c == ')') depth--;
                }
            }
            if (cur.length() == 0) formStart = ln + 1;
            if (cur.length() > 0) cur.append('\n');
            cur.append(rawLine);
            if (depth == 0 && !inStr && cur.toString().trim().length() > 0) {
                forms.add(new FormSpan(cur.toString(), formStart));
                cur.setLength(0);
            }
        }
        if (cur.toString().trim().length() > 0) {
            forms.add(new FormSpan(cur.toString(), formStart));
        }
        return forms;
    }

    /**
     * Recursively inspects a formula and reports the expected argument/return types
     * of predicates, based on the knowledge base signatures.
     *
     * Behavior:
     * - If the formula is a simple clause:
     *     • Extracts its predicate name.
     *     • Skips it if not in the watchlist (if provided).
     *     • Looks up the predicate’s signature in the KB cache.
     *     • Formats a message describing the expected types for each argument,
     *       and optionally the return type if non-empty.
     *     • Appends this message to the output sink and logs it.
     * - If the formula is not a simple clause, recursively descends into its
     *   subformulas, handling quantified expressions correctly.
     *
     * @param kb      The knowledge base containing type signatures for predicates.
     * @param f       The formula to analyze.
     * @param lineNo  The source line number where the formula starts.
     * @param sink    A list to collect human-readable log or info messages.
     * @param watch   An optional set of predicate names to restrict logging to;
     *                if null or empty, all predicates are considered.
     */
    private static void dumpExpectedTypesRec(KB kb, Formula f, int lineNo, List<String> sink,
                                            Set<String> watch) {
        if (f == null || f.getFormula() == null) return;

        if (f.isSimpleClause(kb)) {
            String pred = f.getStringArgument(0);
            if (pred == null) return;

            // Skip if not in the watchlist
            if (watch != null && !watch.isEmpty() && !watch.contains(pred)) return;

            List<String> sig = kb.kbCache.getSignature(pred);
            if (sig == null) {
                logLine(sink, lineNo, pred + " — no signature available in KB.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(pred).append(" expects [");

            // NOTE: sig.get(0) is return type for functions; predicate args start at 1
            for (int i = 1; i < sig.size(); i++) {
                if (i > 1) sb.append(", ");
                sb.append(i).append(": ").append(describeType(sig.get(i)));
            }
            sb.append("]");

            // If there is a non-empty return type, show it too
            String ret = sig.get(0);
            if (ret != null && !ret.isEmpty()) {
                sb.append("  (return: ").append(describeType(ret)).append(")");
            }

            // ✅ Now actually log the info
            logLine(sink, lineNo, sb.toString());
            return;
        }

        // Recurse into sub-formulas
        List<Formula> subs = Formula.isQuantifier(f.car())
                ? f.complexArgumentsToArrayList(2)
                : f.complexArgumentsToArrayList(1);
        if (subs != null) {
            for (Formula sub : subs) {
                dumpExpectedTypesRec(kb, sub, lineNo, sink, watch);
            }
        }
    }

    private static void checkNamesOperator(String pred, List<String> args, Formula f, int line, List<String> errors, Set<String> seen) {
        if ("names".equals(pred) && args != null && args.size() == 2) {
            String s = args.get(0);
            if (!isQuoted(s)) 
                errors.add("Line " + line + ": 'names' arg1 must be a quoted string → " + f.getFormula());
        }
    }

    private static String describeType(String t) {
        if (t == null) return "<?> (unknown)";
        if ("SetOrClass".equals(t)) return "SetOrClass (class or set)";
        if (t.endsWith("+")) return t.substring(0, t.length()-1) + " (class position)";
        return t;
    }

    private static void logLine(List<String> sink, int lineNo, String msg) {
        sink.add("Line " + lineNo + " [info]: " + msg);
        System.out.println("Line " + lineNo + " [info]: " + msg);
    }

    private static void collectLocalFactsRec(KB kb, Formula f, LocalFacts lf) {
        if (f == null || f.getFormula() == null) return;
        if (f.isSimpleClause(kb)) {
            String head = f.getStringArgument(0);
            if ("instance".equals(head)) {
                String ind = f.getStringArgument(1);
                String cls = f.getStringArgument(2);
                if (isConcreteSymbol(ind)) lf.localsAreIndividuals.add(ind);
                if (isConcreteSymbol(cls)) lf.localsAreClasses.add(cls);
            } else if ("subclass".equals(head)) {
                String sub = f.getStringArgument(1);
                String sup = f.getStringArgument(2);
                if (isConcreteSymbol(sub)) lf.localsAreClasses.add(sub);
                if (isConcreteSymbol(sup)) lf.localsAreClasses.add(sup);
            } else if ("attribute".equals(head)) {
                String attr = f.getStringArgument(2);
                if (isConcreteSymbol(attr)) lf.localsAreAttributes.add(attr);
            }
            return;
        }
        List<Formula> subs = Formula.isQuantifier(f.car())
                ? f.complexArgumentsToArrayList(2)
                : f.complexArgumentsToArrayList(1);
        for (Formula sub : subs) collectLocalFactsRec(kb, sub, lf);
    }


    private static void roleChecksRec(KB kb, Formula f, int lineNo, LocalFacts lf,
                                      List<String> out, boolean includeBelow) {
        if (f == null || f.getFormula() == null) return;
        if (f.isSimpleClause(kb)) {
            String head = f.getStringArgument(0);
            if ("instance".equals(head)) {
                String ind = f.getStringArgument(1);
                String cls = f.getStringArgument(2);
                // arg1 must be an individual (not a class)
                if (isConcreteSymbol(ind) && isKnownClass(kb, ind, lf)) {
                    out.add("Line " + lineNo + ": " + ind + " (instance arg1) looks like a class, expected an individual.");
                }
                // arg2 must be a class
                if (isConcreteSymbol(cls) && !isKnownClass(kb, cls, lf)) {
                    out.add("Line " + lineNo + ": " + cls + " (instance arg2) is not recognized as a class.");
                }
                // optional “below Entity” on the class
                if (includeBelow && isConcreteSymbol(cls) && Diagnostics.termNotBelowEntity(cls, kb)) {
                    out.add("Line " + lineNo + ": Class " + cls + " is not below Entity.");
                }
            }
            else if ("subclass".equals(head)) {
                String sub = f.getStringArgument(1);
                String sup = f.getStringArgument(2);
                // both sides must be classes
                if (isConcreteSymbol(sub) && !isKnownClass(kb, sub, lf)) {
                    out.add("Line " + lineNo + ": " + sub + " (subclass) is not recognized as a class.");
                }
                if (isConcreteSymbol(sup) && !isKnownClass(kb, sup, lf)) {
                    out.add("Line " + lineNo + ": " + sup + " (superclass) is not recognized as a class.");
                }
                // optional “below Entity” only on the superclass (so new classes are allowed)
                if (includeBelow && isConcreteSymbol(sup) && Diagnostics.termNotBelowEntity(sup, kb)) {
                    out.add("Line " + lineNo + ": " + sup + " (superclass) is not below Entity.");
                }
            }
            else if ("attribute".equals(head)) {
                String obj  = f.getStringArgument(1);
                String attr = f.getStringArgument(2);
                // attr must be an Attribute
                if (isConcreteSymbol(attr) && !isKnownAttribute(kb, attr, lf)) {
                    out.add("Line " + lineNo + ": " + attr + " (attribute arg2) is not recognized as an Attribute.");
                }
            }
            return;
        }

        List<Formula> subs = Formula.isQuantifier(f.car())
                ? f.complexArgumentsToArrayList(2)
                : f.complexArgumentsToArrayList(1);
        for (Formula sub : subs) roleChecksRec(kb, sub, lineNo, lf, out, includeBelow);
    }

    private static boolean isKnownClass(KB kb, String sym, LocalFacts lf) {
        if (lf.localsAreClasses.contains(sym)) return true;
        // Accept if KB already knows it's a class (below Entity as class)
        return kb.kbCache.subclassOf(sym, "Entity") || kb.kbCache.transInstOf(sym, "Entity");
    }

    private static boolean isKnownAttribute(KB kb, String sym, LocalFacts lf) {
        if (lf.localsAreAttributes.contains(sym)) return true;
        return kb.kbCache.subclassOf(sym, "Attribute") || kb.kbCache.transInstOf(sym, "Attribute");
    }

    private static boolean isConcreteSymbol(String s) {
        return s != null && !s.isEmpty() && !Formula.isVariable(s) && s.charAt(0) != '"';
    }

}
