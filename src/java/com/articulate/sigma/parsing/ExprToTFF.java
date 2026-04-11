package com.articulate.sigma.parsing;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * <p>This is a NEW class alongside the existing string-based
 * {@link com.articulate.sigma.trans.SUMOtoTFAform}; it does not modify that
 * class.  When a pre-built or freshly parsed {@link Expr} tree is available,
 * callers can use {@link #translate} to skip the heavyweight string-manipulation
 * pipeline that {@code SUMOtoTFAform.process()} performs.</p>
 *
 * <h3>Key difference from FOF (ExprToTPTP)</h3>
 * <p>TFF requires every bound and free variable to carry a sort annotation:
 * <pre>
 *   FOF:  ! [V__X,V__Y] : body
 *   TFF:  ! [V__X : s__Dog, V__Y : $i] : body
 * </pre>
 * This class infers sort annotations in a single pre-pass over the Expr tree
 * by collecting {@code (instance ?X Type)} constraints, then propagates them
 * through the quantifier traversal.  Variables with no inferred sort receive
 * the TFF catch-all sort {@code $i}.</p>
 *
 * <h3>Sort mapping</h3>
 * <ul>
 *   <li>Integer (and subclasses) → {@code $int}</li>
 *   <li>RealNumber → {@code $real}</li>
 *   <li>RationalNumber → {@code $rat}</li>
 *   <li>Any other SUMO class → {@code s__ClassName}</li>
 *   <li>Unknown / unresolved → {@code $i}</li>
 * </ul>
 *
 * <h3>Fallback</h3>
 * <p>Callers should fall back to
 * {@link com.articulate.sigma.trans.SUMOtoTFAform#process(Formula, boolean)}
 * when {@link #translate} returns {@code null} (parse failure or unsupported
 * construct) or when numeric type constraints require the full pre-processing
 * pipeline.</p>
 */
public class ExprToTFF {

    public static boolean debug = false;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Parse a KIF string, build an {@link Expr} tree, and translate it to TFF.
     *
     * <p>This is the primary entry point when the caller has only a KIF string
     * (e.g. after {@code renameVariableArityRelations} in
     * {@code SUMOKBtoTPTPKB.writeFile()}).  Returns {@code null} when the string
     * cannot be parsed; the caller should fall back to
     * {@link com.articulate.sigma.trans.SUMOtoTFAform#process} in that case.</p>
     *
     * @param kif   the KIF formula string
     * @param query {@code true} for query mode (free vars get {@code ?})
     * @param kb    knowledge base for numeric subclass sort resolution; may be {@code null}
     * @return TFF formula string, or {@code null} on parse failure
     */
    public static String translateKifString(String kif, boolean query, KB kb) {
        try {
            SuokifVisitor visitor = SuokifVisitor.parseSentence(kif);
            if (visitor.result == null || visitor.result.isEmpty()) return null;
            FormulaAST ast = visitor.result.get(0);
            if (ast == null || ast.expr == null) return null;
            return translate(ast.expr, query, kb);
        } catch (Exception e) {
            if (debug) System.err.println("ExprToTFF.translateKifString(): parse error for: " + kif + " — " + e.getMessage());
            return null;
        }
    }

    /**
     * Translate an {@link Expr} tree to a TFF formula string.
     *
     * <p>Variable sort annotations are inferred from {@code (instance ?X Type)}
     * patterns within the formula.  Variables not constrained by such patterns
     * receive the default sort {@code $i}.</p>
     *
     * @param expr  the formula tree (must not be null)
     * @param query {@code true} for query mode (free vars get {@code ?})
     * @param kb    knowledge base used to resolve numeric subclass sorts;
     *              may be {@code null} (falls back to {@code $i})
     * @return TFF formula string
     */
    public static String translate(Expr expr, boolean query, KB kb) {
        if (expr == null) return "";
        // Formulas with numeric literals require arithmetic sort handling (predicate name
        // mangling, numeric sort propagation) that only SUMOtoTFAform implements.
        // Return null to trigger fallback rather than producing a type-incorrect formula.
        if (containsNumericLiteral(expr)) return null;
        // Formulas involving functions/predicates with numeric-typed arguments also require
        // SUMOtoTFAform's predicate-name mangling. Check signatures when KB is available.
        if (kb != null && containsNumericSignature(expr, kb)) return null;
        Map<String, String> varSorts = inferVarSorts(expr, kb);
        String body = translateExpr(expr, false, varSorts);
        Set<String> freeVars = ExprToTPTP.collectFreeVars(expr);
        if (!freeVars.isEmpty()) {
            String quantStr = query ? "? [" : "! [";
            String varList = freeVars.stream()
                    .map(v -> ExprToTPTP.translateVarName(v) + " : "
                            + varSorts.getOrDefault(v, "$i"))
                    .collect(Collectors.joining(","));
            return "( " + quantStr + varList + "] : (" + body + " ) )";
        }
        return body;
    }

    /**
     * Map a SUMO class name to the corresponding TFF sort string.
     *
     * @param sumoType the SUMO class name (e.g. {@code "Dog"}, {@code "Integer"})
     * @param kb       KB for subclass checks; may be {@code null}
     * @return TFF sort string (e.g. {@code "$int"}, {@code "s__Dog"}, {@code "$i"})
     */
    public static String translateSortName(String sumoType, KB kb) {
        if (sumoType == null || sumoType.isEmpty()) return "$i";
        switch (sumoType) {
            case "$i": case "$tType": return sumoType;
            case "Integer":        return "$int";
            case "RealNumber":     return "$real";
            case "RationalNumber": return "$rat";
        }
        if (kb != null) {
            if (kb.isSubclass(sumoType, "Integer"))        return "$int";
            if (kb.isSubclass(sumoType, "RationalNumber")) return "$rat";
            if (kb.isSubclass(sumoType, "RealNumber"))     return "$real";
        }
        return Formula.TERM_SYMBOL_PREFIX + sumoType;
    }

    // -----------------------------------------------------------------------
    // Numeric content detection
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} if {@code expr} contains any {@link Expr.NumLiteral} node.
     *
     * <p>Formulas with numeric literals involve arithmetic functions (e.g.
     * {@code AbsoluteValueFn}, {@code difference}) whose TFF signatures use
     * {@code $real}/{@code $int}/{@code $rat}.  Variables appearing in those
     * functions need arithmetic sorts, but the same variables often also appear
     * in logical predicates (e.g. {@code s__instance}) that expect {@code $i}.
     * {@code SUMOtoTFAform} resolves this via predicate-name mangling; this class
     * does not, so such formulas must fall back to the legacy translator.</p>
     */
    private static boolean containsNumericLiteral(Expr expr) {
        return switch (expr) {
            case Expr.NumLiteral ignored -> true;
            case Expr.SExpr se -> {
                if (se.head() != null && containsNumericLiteral(se.head())) yield true;
                for (Expr a : se.args())
                    if (containsNumericLiteral(a)) yield true;
                yield false;
            }
            default -> false;
        };
    }

    /**
     * Returns {@code true} if any function or predicate in {@code expr} has a numeric
     * sort ({@code RealNumber}, {@code Integer}, {@code RationalNumber} or subclass)
     * in its KB signature.
     *
     * <p>Such formulas require {@code SUMOtoTFAform}'s predicate-name mangling to
     * reconcile numeric-typed arguments with {@code $i}-typed predicates
     * (e.g. {@code s__MeasureFn : ($real * $i) > $i} used inside
     * {@code s__measure(...)} which expects {@code $i}).</p>
     */
    private static boolean containsNumericSignature(Expr expr, KB kb) {
        return switch (expr) {
            case Expr.Atom a -> hasNumericSort(a.name(), kb);
            case Expr.SExpr se -> {
                if (se.head() != null && containsNumericSignature(se.head(), kb)) yield true;
                for (Expr a : se.args())
                    if (containsNumericSignature(a, kb)) yield true;
                yield false;
            }
            default -> false;
        };
    }

    private static boolean hasNumericSort(String name, KB kb) {
        if (kb.kbCache == null) return false;
        List<String> sig = kb.kbCache.getSignature(name);
        if (sig == null || sig.isEmpty()) return false;
        for (String sort : sig) {
            if (sort == null || sort.isEmpty()) continue;
            if ("Integer".equals(sort) || "RealNumber".equals(sort) || "RationalNumber".equals(sort))
                return true;
            if (kb.isSubclass(sort, "Integer")
                    || kb.isSubclass(sort, "RealNumber")
                    || kb.isSubclass(sort, "RationalNumber"))
                return true;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Sort inference
    // -----------------------------------------------------------------------

    /**
     * Single-pass BFS over {@code expr} collecting variable sort constraints
     * from {@code (instance ?X Type)} patterns.  First occurrence wins.
     */
    static Map<String, String> inferVarSorts(Expr expr, KB kb) {
        Map<String, String> sorts = new LinkedHashMap<>();
        collectVarSorts(expr, sorts, kb);
        return sorts;
    }

    private static void collectVarSorts(Expr expr, Map<String, String> sorts, KB kb) {
        if (!(expr instanceof Expr.SExpr se)) return;
        String head = se.headName();
        // (instance ?X Type) → type annotation for ?X
        if ("instance".equals(head) && se.args().size() == 2) {
            Expr arg1 = se.args().get(0);
            Expr arg2 = se.args().get(1);
            if (arg1 instanceof Expr.Var v && arg2 instanceof Expr.Atom a)
                sorts.putIfAbsent(v.name(), varSortName(a.name()));
        }
        if (se.head() != null) collectVarSorts(se.head(), sorts, kb);
        for (Expr a : se.args()) collectVarSorts(a, sorts, kb);
    }

    /**
     * Returns the TFF sort to use as a variable type annotation.
     *
     * <p>Always returns {@code $i} — the universal TFF individual sort that requires
     * no separate type declaration.  Named SUMO sorts (e.g. {@code s__Dog}) are only
     * valid if a {@code tff(..., type, s__Dog: $tType)} declaration precedes the axiom.
     * {@code ExprToTFF} does not generate those declarations (it skips
     * {@code SUMOtoTFAform.missingSorts()}), so any sort other than the built-in
     * {@code $i} would cause "Undeclared type constructor" prover errors.  The semantic
     * content ("X is a Dog") is preserved in the formula body via
     * {@code s__instance(V__X, s__Dog)} regardless of the variable's declared sort.</p>
     */
    private static String varSortName(String sumoType) {
        return "$i";
    }

    // -----------------------------------------------------------------------
    // Translation
    // -----------------------------------------------------------------------

    static String translateExpr(Expr expr, boolean isHead, Map<String, String> varSorts) {
        return switch (expr) {
            case Expr.Var      v  -> ExprToTPTP.translateVarName(v.name());
            case Expr.RowVar   rv -> ExprToTPTP.translateVarName(rv.name());
            case Expr.NumLiteral n -> n.value();
            case Expr.StrLiteral s ->
                    s.value().replaceAll("[\n\t\r\f]", " ").replaceAll("'", "");
            case Expr.Atom     a  -> ExprToTPTP.translateAtom(a.name(), isHead, "tff");
            case Expr.SExpr    se -> translateSExpr(se, varSorts);
        };
    }

    private static String translateSExpr(Expr.SExpr se, Map<String, String> varSorts) {
        String headName = se.headName();
        if (headName == null)
            return se.args().stream()
                    .map(a -> translateExpr(a, false, varSorts))
                    .collect(Collectors.joining(","));

        return switch (headName) {
            case "not" -> {
                if (se.args().size() != 1) yield tffError("not", se);
                yield "~(" + translateExpr(se.args().get(0), false, varSorts) + ")";
            }
            case "and" -> {
                if (se.args().size() < 2) yield tffError("and", se);
                yield "(" + se.args().stream()
                        .map(a -> translateExpr(a, false, varSorts))
                        .collect(Collectors.joining(" & ")) + ")";
            }
            case "or" -> {
                if (se.args().size() < 2) yield tffError("or", se);
                yield "(" + se.args().stream()
                        .map(a -> translateExpr(a, false, varSorts))
                        .collect(Collectors.joining(" | ")) + ")";
            }
            case "xor" -> {
                if (se.args().size() < 2) yield tffError("xor", se);
                yield "(" + se.args().stream()
                        .map(a -> translateExpr(a, false, varSorts))
                        .collect(Collectors.joining(" <~> ")) + ")";
            }
            case "=>" -> {
                if (se.args().size() != 2) yield tffError("=>", se);
                String ant = translateExpr(se.args().get(0), false, varSorts);
                String con = translateExpr(se.args().get(1), false, varSorts);
                yield "(" + ant + " => " + con + ")";
            }
            case "<=>" -> {
                if (se.args().size() != 2) yield tffError("<=>", se);
                String lhs = translateExpr(se.args().get(0), false, varSorts);
                String rhs = translateExpr(se.args().get(1), false, varSorts);
                yield "((" + lhs + " => " + rhs + ") & (" + rhs + " => " + lhs + "))";
            }
            case "equal" -> {
                if (se.args().size() != 2) yield tffError("equal", se);
                String lhs = translateExpr(se.args().get(0), false, varSorts);
                String rhs = translateExpr(se.args().get(1), false, varSorts);
                yield "(" + lhs + " = " + rhs + ")";
            }
            case "forall" -> translateQuantifier("! ", se, varSorts);
            case "exists" -> translateQuantifier("? ", se, varSorts);
            default       -> translateApplication(se, varSorts);
        };
    }

    /**
     * Translate a quantified sentence with typed variable list.
     * {@code (forall (?X ?Y) body)} → {@code ( ! [V__X : sort, V__Y : $i] : (body))}
     */
    private static String translateQuantifier(String quantOp, Expr.SExpr se,
                                              Map<String, String> varSorts) {
        if (se.args().size() != 2) return tffError(se.headName(), se);
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
                if (raw != null) {
                    String sort = varSorts.getOrDefault(raw, "$i");
                    tptpVars.add(ExprToTPTP.translateVarName(raw) + " : " + sort);
                }
            }
        }
        if (tptpVars.isEmpty()) return translateExpr(body, false, varSorts);
        String varList = String.join(",", tptpVars);
        return "(" + quantOp + "[" + varList + "] : ("
                + translateExpr(body, false, varSorts) + "))";
    }

    private static String translateApplication(Expr.SExpr se, Map<String, String> varSorts) {
        String head = translateExpr(se.head(), true, varSorts);
        if (se.args().isEmpty()) return head + "()";
        String args = se.args().stream()
                .map(a -> translateExpr(a, false, varSorts))
                .collect(Collectors.joining(","));
        return head + "(" + args + ")";
    }

    private static String tffError(String op, Expr.SExpr se) {
        if (debug) System.err.println("ExprToTFF: malformed " + op + ": " + se.toKifString());
        return null;
    }
}
