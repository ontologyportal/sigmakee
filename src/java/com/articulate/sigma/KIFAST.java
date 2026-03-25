package com.articulate.sigma;

import com.articulate.sigma.parsing.Expr;
import com.articulate.sigma.parsing.FormulaAST;
import com.articulate.sigma.parsing.SuokifVisitor;
import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.util.*;

/**
 * ANTLR-based KIF parser that produces {@link FormulaAST} objects with populated
 * {@link Expr} trees. This is the Phase 2 replacement for {@link KIF}.
 *
 * <p>The parsing pipeline is:
 * <ol>
 *   <li>ANTLR {@code SuokifLexer + SuokifParser} → Concrete Syntax Tree</li>
 *   <li>{@link SuokifVisitor} → {@code FormulaAST} objects (each has {@code expr} populated
 *       by {@link com.articulate.sigma.parsing.SuokifToExpr})</li>
 *   <li>This class walks the {@code Expr} tree to build the same indexing maps that
 *       {@link KIF} builds with {@code StreamTokenizer}.</li>
 * </ol>
 *
 * <p>Produces the same three maps as {@link KIF}:
 * <ul>
 *   <li>{@code formulaMap : Map<String, FormulaAST>} — canonical formula string → FormulaAST</li>
 *   <li>{@code formulas   : Map<String, List<String>>} — predicate-position key → formula strings</li>
 *   <li>{@code terms      : Set<String>} — all non-variable constants seen</li>
 *   <li>{@code termFrequency : Map<String, Integer>} — occurrence count per term</li>
 * </ul>
 *
 * <p>The {@code formulas} map uses the same key scheme as {@link KIF#createKey}: {@code arg-N-term},
 * {@code ant-term}, {@code cons-term}, {@code stmt-term}.
 */
public class KIFAST {

    /** All non-variable constant symbols in the parsed input. */
    public Set<String> terms = new TreeSet<>();

    /** Occurrence count per term. */
    public Map<String, Integer> termFrequency = new HashMap<>();

    /**
     * Predicate-position index. Each key (e.g. {@code "arg-0-instance"}) maps to the list of
     * canonical formula strings that contain the corresponding term in that position.
     * This has the same structure as {@link KIF#formulas}.
     */
    public Map<String, List<String>> formulas = new HashMap<>();

    /**
     * Primary formula map. Maps the canonical KIF string of each formula to its
     * {@link FormulaAST} (which carries the {@link Expr} tree in {@code f.expr}).
     */
    public Map<String, FormulaAST> formulaMap = new HashMap<>();

    /** Canonical path of the file most recently read. */
    public String filename;

    /** Warnings produced during parsing. */
    public Set<String> warningSet = new TreeSet<>();

    /** Errors produced during parsing. */
    public Set<String> errorSet = new TreeSet<>();

    // -----------------------------------------------------------------------
    // Public API — mirrors KIF
    // -----------------------------------------------------------------------

    /**
     * Parse a KIF file into this instance.
     *
     * @param fname canonical path of the file to read
     * @return set of warning strings (may be empty)
     * @throws IOException if the file cannot be read
     */
    public Set<String> readFile(String fname) throws IOException {

        File file = new File(fname);
        if (!file.exists()) {
            String err = "error file " + fname + " does not exist";
            KBmanager.getMgr().setError(KBmanager.getMgr().getError() + "\n<br/>" + err + "\n<br/>");
            System.err.println("Error in KIFAST.readFile(): " + err);
            return warningSet;
        }
        this.filename = file.getCanonicalPath();
        SuokifVisitor visitor = SuokifVisitor.parseFile(file);
        integrateVisitorResult(visitor);
        return warningSet;
    }

    /**
     * Parse KIF from a {@link Reader}.  The entire content is read into memory first
     * so that ANTLR can operate on a complete input stream.
     *
     * @param r source reader
     * @return set of warning strings (may be empty)
     */
    public Set<String> parse(Reader r) {

        if (r == null) {
            String err = "No Input Reader Specified";
            warningSet.add(err);
            System.err.println("Error in KIFAST.parse(): " + err);
            return warningSet;
        }
        try {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = r.read(buf)) != -1)
                sb.append(buf, 0, n);
            SuokifVisitor visitor = SuokifVisitor.parseString(sb.toString());
            integrateVisitorResult(visitor);
        }
        catch (IOException ex) {
            String msg = ex.getMessage();
            warningSet.add("Warning in KIFAST.parse(Reader): " + msg);
            ex.printStackTrace();
        }
        return warningSet;
    }

    /**
     * Parse a single formula string.
     *
     * @param formula the KIF formula string
     * @return null on success, error message string on failure
     */
    public String parseStatement(String formula) {

        try {
            SuokifVisitor visitor = SuokifVisitor.parseString(formula);
            if (!visitor.errors.isEmpty())
                return "Error parsing: " + formula + " — " + visitor.errors;
            integrateVisitorResult(visitor);
            return null;
        }
        catch (Exception e) {
            System.err.println("Error parsing " + formula);
            e.printStackTrace();
            return e.getMessage();
        }
    }

    // -----------------------------------------------------------------------
    // Internal: integrate SuokifVisitor results into this instance's maps
    // -----------------------------------------------------------------------
    private void integrateVisitorResult(SuokifVisitor visitor) {

        if (visitor == null) return;
        int dupCount = 0;
        for (FormulaAST f : visitor.result.values()) {
            if (f == null || StringUtil.emptyString(f.getFormula())) continue;
            // Normalize whitespace (collapse sequences to single space) to match KIF's
            // normalizeSpaceChars() behaviour so that formula-map keys are identical.
            String kifStr = StringUtil.normalizeSpaceChars(f.getFormula());
            if (!kifStr.equals(f.getFormula())) f.setFormula(kifStr);
            // Duplicate detection
            if (formulaMap.containsKey(kifStr)) {
                if (!"no".equals(KBmanager.getMgr().getPref("reportDup"))) {
                    String warning = "Duplicate axiom at line: " + f.startLine
                            + " of " + (filename != null ? filename : "<string>")
                            + ": " + kifStr;
                    warningSet.add(warning);
                    System.err.println(warning);
                }
                dupCount++;
                continue; // skip duplicate
            }
            if (filename != null && f.sourceFile == null)
                f.sourceFile = filename;
            formulaMap.put(kifStr, f);
            // Build predicate-position keys from the Expr tree
            Set<String> keySet = new LinkedHashSet<>();
            keySet.add(kifStr);          // formula string is itself a key
            keySet.add(f.createID());    // hash-based ID key
            if (f.expr != null)
                generateExprKeys(f.expr, keySet);
            for (String key : keySet) {
                formulas.computeIfAbsent(key, k -> new ArrayList<>()).add(kifStr);
            }
        }
        if (dupCount > 0) {
            String warning = "WARNING in KIFAST, " + dupCount + " duplicate statement"
                    + (dupCount > 1 ? "s " : " ")
                    + "detected in " + (filename != null ? filename : "<string>");
            warningSet.add(warning);
        }
        warningSet.addAll(visitor.errors);
    }

    // -----------------------------------------------------------------------
    // Key generation from Expr tree (mirrors KIF.createKey() logic)
    // -----------------------------------------------------------------------

    /**
     * Walk the top-level {@link Expr} and populate {@code keySet} with all the
     * predicate-position index keys that {@link KIF} would have generated for the
     * same formula.
     *
     * <p>Key scheme (identical to {@link KIF#createKey}):
     * <ul>
     *   <li>{@code arg-N-term} — non-rule formula, term at argument position N, parenLevel 1</li>
     *   <li>{@code ant-term}   — term inside the antecedent of a rule</li>
     *   <li>{@code cons-term}  — term inside the consequent of a rule</li>
     *   <li>{@code stmt-term}  — term at parenLevel > 1, not in a rule branch</li>
     * </ul>
     */
    private void generateExprKeys(Expr expr, Set<String> keySet) {

        if (!(expr instanceof Expr.SExpr se)) {
            // Bare atom or variable at top level — unusual but possible
            if (expr instanceof Expr.Atom a) {
                addTerm(a.name());
                keySet.add(KIF.createKey(a.name(), false, false, 0, 1));
            }
            return;
        }

        String headName = se.headName();
        boolean isRule = "=>".equals(headName) || "<=>".equals(headName);

        // Head atom (e.g. "instance", "=>", "and", "forall")
        if (headName != null) {
            addTerm(headName);
            keySet.add(KIF.createKey(headName, false, false, 0, 1));
        }

        if (isRule) {
            List<Expr> args = se.args();
            // antecedent (first arg)
            if (!args.isEmpty())
                collectRuleKeys(args.get(0), true, false, keySet);
            // consequent (second arg)
            if (args.size() > 1)
                collectRuleKeys(args.get(1), false, true, keySet);
        } else {
            // Non-rule: direct args at argumentNum = i+1, parenLevel = 1
            List<Expr> args = se.args();
            for (int i = 0; i < args.size(); i++)
                collectArgKeys(args.get(i), i + 1, keySet);
        }
    }

    /**
     * Collect keys for a direct argument of a non-rule top-level formula
     * (parenLevel = 1, argumentNum = argNum).
     */
    private void collectArgKeys(Expr arg, int argNum, Set<String> keySet) {

        switch (arg) {
            case Expr.Atom a -> {
                addTerm(a.name());
                keySet.add(KIF.createKey(a.name(), false, false, argNum, 1));
            }
            case Expr.SExpr se -> collectNestedKeys(se, false, false, keySet);
            // Var, RowVar, NumLiteral, StrLiteral: no term, no key
            default -> { }
        }
    }

    /**
     * Recursively collect keys for all atoms inside a rule branch (antecedent or consequent).
     * All atoms at any depth get {@code ant-term} or {@code cons-term}.
     */
    private void collectRuleKeys(Expr expr, boolean inAnt, boolean inCons, Set<String> keySet) {

        switch (expr) {
            case Expr.Atom a -> {
                addTerm(a.name());
                keySet.add(KIF.createKey(a.name(), inAnt, inCons, 0, 2));
            }
            case Expr.SExpr se -> {
                if (se.head() instanceof Expr.Atom head) {
                    addTerm(head.name());
                    keySet.add(KIF.createKey(head.name(), inAnt, inCons, 0, 2));
                }
                for (Expr child : se.args())
                    collectRuleKeys(child, inAnt, inCons, keySet);
            }
            default -> { }
        }
    }

    /**
     * Recursively collect {@code stmt-term} keys for all atoms nested inside a non-rule
     * expression (parenLevel > 1, not ant/cons).
     */
    private void collectNestedKeys(Expr expr, boolean inAnt, boolean inCons, Set<String> keySet) {

        switch (expr) {
            case Expr.Atom a -> {
                addTerm(a.name());
                // parenLevel > 1 → "stmt-term" (or "ant-"/"cons-" if in rule branch)
                keySet.add(KIF.createKey(a.name(), inAnt, inCons, 0, 2));
            }
            case Expr.SExpr se -> {
                if (se.head() instanceof Expr.Atom head) {
                    addTerm(head.name());
                    keySet.add(KIF.createKey(head.name(), inAnt, inCons, 0, 2));
                }
                for (Expr child : se.args())
                    collectNestedKeys(child, inAnt, inCons, keySet);
            }
            default -> { }
        }
    }

    // -----------------------------------------------------------------------
    // Term accounting
    // -----------------------------------------------------------------------

    private void addTerm(String name) {
        terms.add(name);
        termFrequency.merge(name, 1, Integer::sum);
    }

    // -----------------------------------------------------------------------
    // main — smoke test
    // -----------------------------------------------------------------------

    public static void main(String[] args) {

        String exp = "(documentation foo \"(written by John Smith).\")\n"
                + "(instance Foo Bar)\n"
                + "(=>\n  (instance ?X Man)\n  (attribute ?X Mortal))";
        System.out.println("Input:\n" + exp);
        KIFAST kif = new KIFAST();
        kif.parse(new StringReader(exp));
        System.out.println("formulaMap keys: " + kif.formulaMap.keySet());
        System.out.println("terms: " + kif.terms);
        System.out.println("formulas sample (arg-0-instance): " + kif.formulas.get("arg-0-instance"));
        System.out.println("formulas sample (ant-instance): " + kif.formulas.get("ant-instance"));
        System.out.println("formulas sample (cons-attribute): " + kif.formulas.get("cons-attribute"));
        for (Map.Entry<String, FormulaAST> e : kif.formulaMap.entrySet()) {
            FormulaAST f = e.getValue();
            System.out.println("  formula: " + f.getFormula());
            System.out.println("  expr:    " + (f.expr != null ? f.expr.toKifString() : "null"));
        }
    }
}
