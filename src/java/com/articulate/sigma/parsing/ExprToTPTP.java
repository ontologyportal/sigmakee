package com.articulate.sigma.parsing;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.utils.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

// SuokifVisitor used only in translateKifString() for on-demand ANTLR parsing

/**
 * Translates {@link Expr} trees to TPTP format (FOF or TFF).
 *
 * <p>This is the Expr-based counterpart of
 * {@link com.articulate.sigma.trans.SUMOformulaToTPTPformula#processRecurse(Formula, String)}.
 * All structural traversal is done by switching on sealed {@link Expr} subtypes;
 * no string-based parsing or ANTLR context objects are involved.</p>
 *
 * <h3>Key mapping</h3>
 * <pre>
 *   SUO-KIF                 TPTP FOF
 *   ───────────────────     ─────────────────────────────────────
 *   ?Var / @Row             V__Var / V__Row   (TERM_VARIABLE_PREFIX)
 *   SymbolName              s__SymbolName     (TERM_SYMBOL_PREFIX)
 *   relName (as arg)        s__relName__m     (+TERM_MENTION_SUFFIX for relations)
 *   (not A)                 ~(A)
 *   (and A B C)             (A & B & C)
 *   (or  A B C)             (A | B | C)
 *   (=> A B)                (A => B)
 *   (<=> A B)               ((A => B) & (B => A))
 *   (equal A B)             (A = B)
 *   (forall (?X) B)         ! [V__X] : (B)
 *   (exists (?X) B)         ? [V__X] : (B)
 *   (pred A B)              s__pred(A,B)
 *   free variables          wrapped in outer ! [...] : or ? [...]
 * </pre>
 */
public class ExprToTPTP {

    public static boolean debug = false;

    /**
     * Thread-local snapshot of the KB relations set.  Set this to
     * {@code kb.kbCache.relations} before translating a batch of formulas so
     * that {@link #shouldAddMention} can do a single O(1) {@code HashSet.contains()}
     * instead of the per-atom {@link KB#isRelationInAnyKB} call (which iterates
     * all KBs on every atom).  Remove it in the {@code finally} block after the
     * batch to avoid cross-request contamination.
     *
     * <p>When {@code null} (the default), falls back to the original per-atom
     * KB lookup so callers that don't set the snapshot continue to work correctly.</p>
     */
    public static final ThreadLocal<Set<String>> relationsThreadLocal = new ThreadLocal<>();

    // -----------------------------------------------------------------------
    // Public entry point
    // -----------------------------------------------------------------------

    /**
     * Translate an {@link Expr} tree to a TPTP formula string.
     *
     * <p>Unquantified (free) variables are wrapped in an implicit universal
     * quantifier — or an existential quantifier when {@code query} is true —
     * matching the behaviour of
     * {@link com.articulate.sigma.trans.SUMOformulaToTPTPformula#process(Formula, boolean, String)}.</p>
     *
     * @param expr  the formula tree
     * @param query {@code true} for query mode (free vars get {@code ?})
     * @param lang  "fof" or "tff"
     * @return the TPTP string
     */
    /**
     * Parse a KIF string via ANTLR, build an {@link Expr} tree, and translate it
     * to TPTP.  This is the primary entry point when the caller has only a KIF
     * string rather than a pre-built {@link Expr} (e.g. after
     * {@code preProcess}/{@code renameVariableArityRelations} in
     * {@code SUMOKBtoTPTPKB}).
     *
     * <p>Returns {@code null} when the string cannot be parsed (ANTLR produces no
     * result); the caller should fall back to the legacy string-based translator
     * in that case.</p>
     *
     * @param kif   the KIF formula string
     * @param query {@code true} for query mode
     * @param lang  "fof" or "tff"
     * @return TPTP string, or {@code null} on parse failure
     */
    public static String translateKifString(String kif, boolean query, String lang) {
        try {
            SuokifVisitor visitor = SuokifVisitor.parseSentence(kif);
            if (visitor.result == null || visitor.result.isEmpty()) return null;
            FormulaAST ast = visitor.result.get(0);
            if (ast == null || ast.expr == null) return null;
            return translate(ast.expr, query, lang);
        } catch (Exception e) {
            if (debug) System.err.println("ExprToTPTP.translateKifString(): parse error for: " + kif + " — " + e.getMessage());
            return null;
        }
    }

    /**
     * Return a comma-separated list of TPTP variable names for the free variables
     * in the given KIF formula string.  Returns an empty {@link StringBuilder} if
     * the string cannot be parsed or has no free variables.
     *
     * <p>This mirrors the side-effect of
     * {@link com.articulate.sigma.trans.SUMOformulaToTPTPformula#tptpParseSUOKIFString}
     * that sets its {@code qlistTL} ThreadLocal, but without requiring that legacy
     * path to run.</p>
     */
    public static StringBuilder getQlist(String kif) {
        if (kif == null || kif.isEmpty()) return new StringBuilder();
        try {
            SuokifVisitor visitor = SuokifVisitor.parseSentence(kif);
            if (visitor.result == null || visitor.result.isEmpty()) return new StringBuilder();
            FormulaAST ast = visitor.result.get(0);
            if (ast == null || ast.expr == null) return new StringBuilder();
            Set<String> freeVars = collectFreeVars(ast.expr);
            StringBuilder sb = new StringBuilder();
            for (String v : freeVars) {
                if (sb.length() > 0) sb.append(",");
                sb.append(translateVarName(v));
            }
            return sb;
        } catch (Exception e) {
            return new StringBuilder();
        }
    }

    /**
     * Fast-path overload: return a comma-separated list of TPTP variable names
     * for the free variables in an already-parsed {@link Expr} tree.
     *
     * <p>Use this when the caller already holds a pre-built {@link Expr} (e.g.
     * from {@link FormulaAST#expr}) to avoid redundant ANTLR re-parsing.
     * Produces the same result as {@link #getQlist(String)} for the same formula.</p>
     *
     * @param expr the pre-built expression tree; {@code null} is handled safely
     * @return comma-separated TPTP variable list (e.g. {@code "V__X,V__Y"}),
     *         or an empty {@link StringBuilder} if there are no free variables
     */
    public static StringBuilder getQlist(Expr expr) {
        if (expr == null) return new StringBuilder();
        Set<String> freeVars = collectFreeVars(expr);
        StringBuilder sb = new StringBuilder();
        for (String v : freeVars) {
            if (sb.length() > 0) sb.append(",");
            sb.append(translateVarName(v));
        }
        return sb;
    }

    public static String translate(Expr expr, boolean query, String lang) {
        String body = translateExpr(expr, false, lang);
        Set<String> freeVars = collectFreeVars(expr);
        if (!freeVars.isEmpty()) {
            String quantStr = query ? "? [" : "! [";
            String varList = freeVars.stream()
                    .map(ExprToTPTP::translateVarName)
                    .collect(Collectors.joining(","));
            return "( " + quantStr + varList + "] : (" + body + " ) )";
        }
        return body;
    }

    /**
     * Translate a single {@link Expr} node.
     *
     * @param isHead {@code true} when this node is the head (predicate/function
     *               position) of an S-expression; {@code false} when it appears
     *               as a leaf argument.  This determines whether the
     *               {@code __m} mention-suffix is applied.
     */
    static String translateExpr(Expr expr, boolean isHead, String lang) {
        return switch (expr) {
            case Expr.Var  v  -> translateVarName(v.name());
            case Expr.RowVar rv -> translateVarName(rv.name());
            case Expr.NumLiteral n -> translateNumber(n.value(), lang);
            case Expr.StrLiteral s ->
                    // Strip \n etc. and single-quotes, matching translateWord_1 type==34 branch
                    s.value().replaceAll("[\n\t\r\f]", " ").replaceAll("'", "");
            case Expr.Atom a -> translateAtom(a.name(), isHead, lang);
            case Expr.SExpr se -> translateSExpr(se, lang);
        };
    }

    // -----------------------------------------------------------------------
    // Atom translation
    // -----------------------------------------------------------------------

    /**
     * Translate an atomic symbol.
     *
     * @param name    the KIF symbol name
     * @param isHead  {@code true} when used as a predicate/function head
     */
    static String translateAtom(String name, boolean isHead, String lang) {

        if (name == null || name.isEmpty()) return "";

        // Variables — these normally arrive as Var/RowVar nodes, but defend
        char ch0 = name.charAt(0);
        if (ch0 == '?' || ch0 == '@')
            return translateVarName(name);

        // Numbers: pure numeric literals pass through; mixed digit+letter tokens (e.g. 5GNetwork)
        // must fall through so they receive the s__ prefix and become valid TPTP functors.
        if (StringUtil.isNumeric(name))
            return translateNumber(name, lang);

        // Inequality predicates used as terms (not in head position) need the __m
        // mention suffix so the same symbol is not used both as a predicate and as
        // a term — that is a TPTP type error for all languages (FOF and TFF alike).
        if (Formula.isInequality(name) && !isHead)
            return Formula.TERM_SYMBOL_PREFIX + name + Formula.TERM_MENTION_SUFFIX;

        // Logical operators used in head position → their TPTP equivalents
        // (These are handled structurally in translateSExpr; if we arrive here,
        //  the operator must be appearing as a bare atom in arg position.)
        String tptpOp = logicalOpToTPTP(name);
        if (tptpOp != null && isHead)
            return tptpOp; // shouldn't normally happen — SExpr handles head dispatch

        // Special constants. When used as head they translate directly; in argument
        // position they become distinct-object constants (single-quoted) so the prover
        // does not confuse them with the defined TPTP propositions $true/$false.
        if (Formula.LOG_TRUE.equals(name))
            return isHead ? "$true"  : "'" + "$true"  + Formula.TERM_MENTION_SUFFIX + "'";
        if (Formula.LOG_FALSE.equals(name))
            return isHead ? "$false" : "'" + "$false" + Formula.TERM_MENTION_SUFFIX + "'";

        // TFF arithmetic functions (only when used as head)
        if ("tff".equals(lang) && isHead) {
            String tffFn = tffArithFunction(name);
            if (tffFn != null) return tffFn;
        }

        // Regular symbol
        String term = name.replace('.', '_').replace('-', '_');
        if (!isHead) {
            // Leaf argument: add __m to relations / lowercase terms / Fn-suffixed terms
            boolean addMention = shouldAddMention(term, lang);
            if (addMention) term += Formula.TERM_MENTION_SUFFIX;
        }
        return Formula.TERM_SYMBOL_PREFIX + term;
    }

    /** Return true when an atom in argument position should get the {@code __m} mention suffix. */
    private static boolean shouldAddMention(String name, String lang) {
        if (name.endsWith(Formula.TERM_MENTION_SUFFIX)) return false; // already has it
        if (Formula.isInequality(name)) return false;
        if (name.endsWith(Formula.FN_SUFF)) return true;
        if (!name.isEmpty() && Character.isLowerCase(name.charAt(0))) return true;
        // Ask the KB — relation names used as terms need __m
        // Fast path: use pre-captured relations snapshot when available (avoids per-atom KB walk)
        Set<String> rels = relationsThreadLocal.get();
        if (rels != null) return rels.contains(name);
        // Fallback: ask the KB directly (unit-test / legacy context)
        try {
            return KB.isRelationInAnyKB(name);
        } catch (Exception ignored) {
            return false; // KB not available (unit-test context)
        }
    }

    /** Map KIF logical operator names to TPTP operator strings, or null if not an operator. */
    private static String logicalOpToTPTP(String name) {
        return switch (name) {
            case "forall"  -> "! ";
            case "exists"  -> "? ";
            case "not"     -> "~";
            case "and"     -> " & ";
            case "or"      -> " | ";
            case "xor"     -> " <~> ";
            case "=>"      -> " => ";
            case "<=>"     -> " <=> ";
            case "equal"   -> " = ";
            default        -> null;
        };
    }

    /** TFF arithmetic function name mapping, or null if not applicable. */
    private static String tffArithFunction(String name) {
        return switch (name) {
            case "AdditionFn"       -> "sum";
            case "SubtractionFn"    -> "difference";
            case "MultiplicationFn" -> "product";
            case "DivisionFn"       -> "quotient";
            default                 -> null;
        };
    }

    // -----------------------------------------------------------------------
    // Number translation
    // -----------------------------------------------------------------------

    static String translateNumber(String value, String lang) {
        if ("tff".equals(lang)) return value; // TFF passes numbers through
        // FOF: optionally hide numbers behind n__ prefix
        // Match SUMOformulaToTPTPformula behaviour: hideNumbers is true by default
        boolean hideNumbers = true; // TODO: read from KBmanager.getMgr().getPref("hideNumbers")
        if (hideNumbers) {
            String safe = value.replace('.', '_').replace('-', '_');
            return "n__" + safe;
        }
        return value;
    }

    // -----------------------------------------------------------------------
    // S-expression (compound) translation
    // -----------------------------------------------------------------------

    private static String translateSExpr(Expr.SExpr se, String lang) {
        String headName = se.headName();
        if (headName == null) {
            // Null-head var list (inside quantifier) — shouldn't appear here in isolation
            return se.args().stream()
                    .map(a -> translateExpr(a, false, lang))
                    .collect(Collectors.joining(","));
        }

        return switch (headName) {
            // ---- logical connectives ----
            case "not" -> {
                if (se.args().size() != 1) yield errorStr("not", se);
                yield "~(" + translateExpr(se.args().get(0), false, lang) + ")";
            }
            case "and" -> {
                if (se.args().size() < 2) yield errorStr("and", se);
                yield "(" + se.args().stream()
                        .map(a -> translateExpr(a, false, lang))
                        .collect(Collectors.joining(" & ")) + ")";
            }
            case "or" -> {
                if (se.args().size() < 2) yield errorStr("or", se);
                yield "(" + se.args().stream()
                        .map(a -> translateExpr(a, false, lang))
                        .collect(Collectors.joining(" | ")) + ")";
            }
            case "xor" -> {
                if (se.args().size() < 2) yield errorStr("xor", se);
                yield "(" + se.args().stream()
                        .map(a -> translateExpr(a, false, lang))
                        .collect(Collectors.joining(" <~> ")) + ")";
            }
            case "=>" -> {
                if (se.args().size() != 2) yield errorStr("=>", se);
                String ant = translateExpr(se.args().get(0), false, lang);
                String con = translateExpr(se.args().get(1), false, lang);
                // EProver needs extra parens on consequent
                if (isEProver())
                    yield "(" + ant + " => (" + con + "))";
                yield "(" + ant + " => " + con + ")";
            }
            case "<=>" -> {
                if (se.args().size() != 2) yield errorStr("<=>", se);
                String lhs = translateExpr(se.args().get(0), false, lang);
                String rhs = translateExpr(se.args().get(1), false, lang);
                // Expand biconditional as two implications — matches SUMOformulaToTPTPformula
                yield "((" + lhs + " => " + rhs + ") & (" + rhs + " => " + lhs + "))";
            }
            case "equal" -> {
                if (se.args().size() != 2) yield errorStr("equal", se);
                String lhs = translateExpr(se.args().get(0), false, lang);
                String rhs = translateExpr(se.args().get(1), false, lang);
                yield "(" + lhs + " = " + rhs + ")";
            }

            // ---- quantifiers ----
            case "forall" -> translateQuantifier("! ", se, lang);
            case "exists" -> translateQuantifier("? ", se, lang);

            // ---- relation / function application ----
            default -> translateApplication(se, lang);
        };
    }

    /**
     * Translate a quantified sentence.
     * {@code (forall (?X ?Y) body)} → {@code ( ! [V__X,V__Y] : (body))}
     */
    private static String translateQuantifier(String quantOp, Expr.SExpr se, String lang) {
        if (se.args().size() != 2) return errorStr(se.headName(), se);
        Expr varListExpr = se.args().get(0);
        Expr body        = se.args().get(1);

        List<String> tptpVars = new ArrayList<>();
        if (varListExpr instanceof Expr.SExpr varSe) {
            for (Expr v : varSe.args()) {
                String raw = switch (v) {
                    case Expr.Var   vv -> vv.name();
                    case Expr.RowVar rv -> rv.name();
                    default            -> null;
                };
                if (raw != null) tptpVars.add(translateVarName(raw));
            }
        }
        if (tptpVars.isEmpty()) return translateExpr(body, false, lang);
        String varList = String.join(",", tptpVars);
        return "(" + quantOp + "[" + varList + "] : (" + translateExpr(body, false, lang) + "))";
    }

    /**
     * Translate a regular relation or function application.
     * {@code (pred arg1 arg2)} → {@code s__pred(tptp_arg1,tptp_arg2)}
     */
    private static String translateApplication(Expr.SExpr se, String lang) {
        // Head
        String head = translateExpr(se.head(), true, lang);

        if (se.args().isEmpty()) return head + "()";

        // Args: each arg is a leaf in argument position
        StringBuilder argStr = new StringBuilder();
        for (Expr arg : se.args()) {
            argStr.append(translateExpr(arg, false, lang)).append(",");
        }
        argStr.deleteCharAt(argStr.length() - 1); // strip trailing comma

        return head + "(" + argStr + ")";
    }

    // -----------------------------------------------------------------------
    // Variable name translation
    // -----------------------------------------------------------------------

    /**
     * Translate a SUO-KIF variable name (with {@code ?} or {@code @} prefix)
     * to a TPTP variable name (with {@code V__} prefix).
     *
     * <p>Example: {@code ?X} → {@code V__X}, {@code @ROW} → {@code V__ROW}.</p>
     */
    static String translateVarName(String kifName) {
        return Formula.TERM_VARIABLE_PREFIX + kifName.substring(1).replace('-', '_');
    }

    // -----------------------------------------------------------------------
    // Free variable collection
    // -----------------------------------------------------------------------

    /**
     * Collect all variables that appear free (unbound by any quantifier) in
     * the given expression.  The result is a {@link LinkedHashSet} to preserve
     * depth-first traversal order — matching
     * {@link Formula#collectUnquantifiedVariables()}.
     */
    public static Set<String> collectFreeVars(Expr expr) {
        Set<String> bound = new LinkedHashSet<>();
        Set<String> free  = new LinkedHashSet<>();
        collectFreeVars(expr, bound, free);
        return free;
    }

    private static void collectFreeVars(Expr expr, Set<String> bound, Set<String> free) {
        switch (expr) {
            case Expr.Var v -> {
                if (!bound.contains(v.name())) free.add(v.name());
            }
            case Expr.RowVar rv -> {
                if (!bound.contains(rv.name())) free.add(rv.name());
            }
            case Expr.SExpr se when isQuantifier(se.headName()) && se.args().size() == 2 -> {
                // Add var list to bound scope for the body
                Set<String> childBound = new LinkedHashSet<>(bound);
                Expr varListExpr = se.args().get(0);
                if (varListExpr instanceof Expr.SExpr varSe) {
                    for (Expr v : varSe.args()) {
                        String name = switch (v) {
                            case Expr.Var   vv -> vv.name();
                            case Expr.RowVar rv -> rv.name();
                            default            -> null;
                        };
                        if (name != null) childBound.add(name);
                    }
                }
                collectFreeVars(se.args().get(1), childBound, free);
            }
            case Expr.SExpr se -> {
                for (Expr child : se.args())
                    collectFreeVars(child, bound, free);
            }
            default -> { /* Atom, NumLiteral, StrLiteral — no variables */ }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static boolean isQuantifier(String name) {
        return "forall".equals(name) || "exists".equals(name);
    }

    private static boolean isEProver() {
        try {
            KBmanager mgr = KBmanager.getMgr();
            return mgr != null && mgr.prover == KBmanager.Prover.EPROVER;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String errorStr(String op, Expr.SExpr se) {
        if (debug)
            System.err.println("ExprToTPTP: wrong number of arguments to " + op + " in " + se.toKifString());
        return "";
    }
}
