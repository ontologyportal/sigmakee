package com.articulate.sigma;

import java.io.*;
import java.util.*;

/**
 * KifFileChecker
 *
 * Validates a .kif file for common SUO-KIF issues:
 *   • Balanced parentheses across lines
 *   • Logical operator shapes (via Formula.validArgs + a few explicit checks)
 *   • Unquantified variables (checked ONLY at top level)
 *   • Arity checks (KB signatures with small built-ins)
 *   • Light semantics: arguments “below Entity” for instance/subclass
 *     using Diagnostics.termNotBelowEntity(...) + local declarations
 *   • Unknown head symbol (flags misspelled predicates like "ibnstance")
 *   • Recurses into nested formulas (and/or/not/exists/forall/=>/<=>)
 */
public class KifFileChecker {

    /** Small built-in arities when the KB has no signature for a predicate. */
    private static final Map<String, Integer> BUILTIN_ARITY = new HashMap<>();
    static {
        BUILTIN_ARITY.put("instance", 2);
        BUILTIN_ARITY.put("subclass", 2);
        BUILTIN_ARITY.put("names",    2);
        BUILTIN_ARITY.put("editor",   2);
        BUILTIN_ARITY.put("attribute",2);
    }

    /** Entry point: validate a KIF file end-to-end. */
    public static List<String> check(File file) throws IOException {

        final List<String> out = new ArrayList<>();
        final StringBuilder buf = new StringBuilder();
        final Set<String> localClasses = new HashSet<>();
        int lineNo = 0;
        int startLine = 0;

        final KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        final String fileName = file.getName();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String raw;
            while ((raw = br.readLine()) != null) {
                lineNo++;
                final String line = raw.trim();
                if (line.isEmpty() || line.startsWith(";")) continue;

                if (buf.length() == 0) startLine = lineNo;
                buf.append(' ').append(line);

                final String chunk = buf.toString().trim();
                final Formula probe = new Formula();
                probe.read(chunk);

                // Only act when the accumulated S-expression is balanced
                if (!probe.isBalancedList()) continue;

                // Build the formula object once
                final Formula f = new Formula();
                f.read(chunk);
                f.startLine = startLine;
                f.endLine   = lineNo;
                f.setSourceFile(fileName);

                // Validate deeply (this also records local classes as it goes)
                validateDeep(f, fileName, startLine, kb, localClasses, out, /*topLevel=*/true);

                buf.setLength(0); // reset for the next formula
            }
        }

        // Trailing unbalanced chunk at EOF
        if (buf.length() > 0) {
            out.add(lineMsg(startLine, "Unbalanced parentheses nearby."));
        }

        return out;
    }

    /** Validate f and all nested subformulas. Quantifier check only at top level. */
    private static void validateDeep(Formula f,
                                     String fileName,
                                     int startLine,
                                     KB kb,
                                     Set<String> localClasses,
                                     List<String> out,
                                     boolean topLevel) {

        // Learn any local class facts introduced by this literal (e.g., subclass/instance Class)
        recordLocalClassFacts(f, localClasses, kb);

        // Validate this literal itself (quantifier check only at top level)
        out.addAll(validateFormula(f, fileName, startLine, kb, localClasses, topLevel));

        // Recurse into sentence-valued arguments
        final String pred = f.car();
        if (pred == null || pred.isEmpty()) return;

        final List<String> args = f.argumentsToArrayListString(1);
        if (args == null) return;

        switch (pred) {
            case "not":
                if (!args.isEmpty())
                    descend(args.get(0), f, fileName, startLine, kb, localClasses, out, false);
                break;

            case "and":
            case "or":
            case "=>":
            case "<=>":
                for (String a : args)
                    descend(a, f, fileName, startLine, kb, localClasses, out, false);
                break;

            case "exists":
            case "forall":
                // (exists (?vars) BODY) — args: [varlist, body]
                if (args.size() >= 2)
                    descend(args.get(1), f, fileName, startLine, kb, localClasses, out, false);
                break;

            default:
                // Non-logical heads have no sentence-valued arguments to descend into
                break;
        }
    }

    /** Helper to descend into a child S-expression, if present. */
    private static void descend(String s,
                                Formula parent,
                                String fileName,
                                int startLine,
                                KB kb,
                                Set<String> localClasses,
                                List<String> out,
                                boolean topLevel) {
        if (s == null) return;
        s = s.trim();
        if (!s.startsWith("(")) return; // quick guard against constants/vars/strings

        Formula g = new Formula();
        g.read(s);
        if (!g.isBalancedList()) return;

        g.startLine = startLine;  // reuse parent's start; exact mapping optional
        g.endLine   = parent.endLine;
        g.setSourceFile(fileName);

        validateDeep(g, fileName, startLine, kb, localClasses, out, topLevel);
    }

    /** Validate a single complete, balanced formula (non-recursive). */
    private static List<String> validateFormula(Formula f,
                                                String fileName,
                                                int startLine,
                                                KB kb,
                                                Set<String> localClasses,
                                                boolean topLevel) {

        final List<String> errors = new ArrayList<>();
        final Set<String> seen = new HashSet<>();

        // 1) Syntax & logical-operator argument checks (Formula’s own validator)
        String argError = f.validArgs(fileName, startLine);
        if (!argError.isEmpty() && seen.add(argError)) {
            errors.add(lineMsg(startLine, argError));
        }
        for (String err : f.getErrors()) {
            if (seen.add(err)) {
                errors.add(lineMsg(startLine, err));
            }
        }

        // 2) Quantification: ONLY check at top level.
        if (topLevel) {
            Set<String> unquant = f.collectUnquantifiedVariables();
            if (!f.isRule() && !unquant.isEmpty()) {
                errors.add(lineMsg(startLine, "Unquantified variables → " + unquant));
            }
        }

        // 3) Head symbol and arguments
        final String pred = f.car();
        final List<String> args = (pred == null || pred.isEmpty())
                ? null : f.argumentsToArrayListString(1);

        // 3a) Unknown predicate/operator in the head (catch typos like 'ibnstance', 'sudclass')
        if (pred != null && !pred.isEmpty() && !Formula.isLogicalOperator(pred) && !Formula.isVariable(pred)) {
            boolean known = (kb != null) && !Diagnostics.termNotBelowEntity(pred, kb);
            if (!known) {
                String msg = "Unknown predicate/operator '" + pred + "' (not a SUMO term below Entity).";
                if (seen.add(msg)) errors.add(lineMsg(startLine, msg));
            }
        }

        if (pred != null && !pred.isEmpty()) {
            // 3b) Explicit shapes for core logical operators
            if ("not".equals(pred)) {
                int n = size(args);
                if (n != 1) {
                    String msg = "'not' must have exactly one argument → " + f.getFormula();
                    if (seen.add(msg)) errors.add(lineMsg(startLine, msg));
                }
            }
            if ("=>".equals(pred) || "<=>".equals(pred)) {
                int n = size(args);
                if (n != 2) {
                    String msg = "'" + pred + "' must have exactly two arguments → " + f.getFormula();
                    if (seen.add(msg)) errors.add(lineMsg(startLine, msg));
                }
            }

            // 3c) General arity check from KB signature or built-ins
            int expected = expectedArity(pred, kb);
            if (expected != -1 && args != null && args.size() != expected) {
                String msg = "Arity error for '" + pred + "'. Expected " + expected +
                             " args, found " + args.size() + " → " + f.getFormula();
                if (seen.add(msg)) errors.add(lineMsg(startLine, msg));
            }

            // 4) Light semantic checks via Diagnostics + local declarations
            if (kb != null && args != null) {
                // instance(x, C): C should be below Entity
                if ("instance".equals(pred) && args.size() == 2) {
                    String cls = args.get(1);
                    if (!isBelowEntity(cls, localClasses, kb)) {
                        String msg = "'instance' expects a Class below Entity as arg2, found: " + cls;
                        if (seen.add(msg)) errors.add(lineMsg(startLine, msg));
                    }
                }
                // subclass(C1, C2): both should be below Entity
                else if ("subclass".equals(pred) && args.size() == 2) {
                    String c1 = args.get(0), c2 = args.get(1);
                    boolean ok1 = isBelowEntity(c1, localClasses, kb);
                    boolean ok2 = isBelowEntity(c2, localClasses, kb);
                    if (!ok1 || !ok2) {
                        String msg = "'subclass' expects Classes below Entity → " + f.getFormula();
                        if (seen.add(msg)) errors.add(lineMsg(startLine, msg));
                    }
                }
                // attribute(x, A): A should be below Attribute
                else if ("attribute".equals(pred) && args.size() == 2) {
                    String attr = args.get(1);
                    boolean attrOK = kb.kbCache.subclassOf(attr, "Attribute")
                                       || kb.kbCache.transInstOf(attr, "Attribute");
                    if (!attrOK) {
                        String msg = "'attribute' expects arg2 below Attribute, found: " + attr;
                        if (seen.add(msg)) errors.add(lineMsg(startLine, msg));
                    }
                }
                // names("string", x): first arg must be quoted
                else if ("names".equals(pred) && args.size() == 2) {
                    String s = args.get(0);
                    if (!isQuoted(s)) {
                        String msg = "'names' arg1 must be a quoted string → " + f.getFormula();
                        if (seen.add(msg)) errors.add(lineMsg(startLine, msg));
                    }
                }
            }
        }

        return errors;
    }

    /** Record local declarations so they count as “below Entity” during this run. */
    private static void recordLocalClassFacts(Formula f, Set<String> localClasses, KB kb) {
        final String pred = f.car();
        if (pred == null || pred.isEmpty()) return;

        final List<String> args = f.argumentsToArrayListString(1);
        if (args == null) return;

        if ("subclass".equals(pred) && args.size() == 2) {
            // Always allow the left-hand side (a new local class)
            localClasses.add(args.get(0));
            // Only allow the superclass if it's truly a class below Entity
            if (!Diagnostics.termNotBelowEntity(args.get(1), kb)) {
                localClasses.add(args.get(1));
            }
        }
        else if ("instance".equals(pred) && args.size() == 2 && "Class".equals(args.get(1))) {
            localClasses.add(args.get(0));
        }
    }

    /** True if term is “below Entity”, using local declarations + Diagnostics. */
    private static boolean isBelowEntity(String term, Set<String> localClasses, KB kb) {
        if (term == null || term.isEmpty()) return false;
        if (localClasses != null && localClasses.contains(term)) return true;
        if (kb == null) return false;
        // Diagnostics returns true when NOT below Entity; invert that.
        return !Diagnostics.termNotBelowEntity(term, kb);
    }

    /** Arity from KB signature, else from small built-ins; -1 if unknown. */
    private static int expectedArity(String pred, KB kb) {
        if (kb != null && kb.kbCache != null) {
            final List<String> sig = kb.kbCache.getSignature(pred);
            if (sig != null) return sig.size() - 1; // subtract predicate position
        }
        return BUILTIN_ARITY.getOrDefault(pred, -1);
    }

    private static int size(List<?> l) { return (l == null) ? 0 : l.size(); }

    private static boolean isQuoted(String s) {
        return s != null && s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"");
    }

    private static String lineMsg(int line, String msg) {
        return "Line " + line + ": " + msg;
    }
}
