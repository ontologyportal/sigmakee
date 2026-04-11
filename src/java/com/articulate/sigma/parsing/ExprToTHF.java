package com.articulate.sigma.parsing;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Translates {@link Expr} trees to THF (Typed Higher-order Form) TPTP syntax.
 *
 * <p>THF differs from FOF/TFF in two key ways:
 * <ol>
 *   <li>Function/relation application uses curried {@code @} syntax:
 *       {@code (s__pred @ arg1 @ arg2)} instead of {@code s__pred(arg1,arg2)}.</li>
 *   <li>Quantifiers carry full type annotations:
 *       {@code ! [V__X:$i, V__W:w] : body} instead of {@code ! [V__X] : body}.</li>
 * </ol>
 *
 * <p>Type information for variables is supplied as a {@code Map<String, Set<String>>}
 * where the key is the KIF variable name (e.g. {@code "?X"}) and the value is a set
 * of SUMO type strings (e.g. {@code {"World"}}, {@code {"Formula"}}).  The set is
 * interpreted by {@link #thfType(String, Map, boolean)} to produce one of the four
 * THF base types: {@code $i} (individual), {@code $o} (boolean), {@code w} (Kripke
 * world) or {@code m} (modal accessibility relation).
 *
 * <p>This class is the Expr-based counterpart of
 * {@link com.articulate.sigma.trans.THFnew#process} (modal) and
 * {@link com.articulate.sigma.trans.THFnew#processNonModal} (plain).</p>
 */
public class ExprToTHF {

    public static boolean debug = false;

    // -----------------------------------------------------------------------
    // Public entry points
    // -----------------------------------------------------------------------

    /**
     * Translate an {@link Expr} tree to a THF formula string (modal embedding).
     *
     * <p>Free variables are wrapped in a typed universal (or existential) quantifier.
     *
     * @param expr    the formula tree; may already contain world-argument nodes injected
     *                by {@link com.articulate.sigma.trans.Modals#processModalsExpr}
     * @param query   {@code true} → free vars get {@code ?} (existential), else {@code !}
     * @param typeMap variable → set-of-SUMO-types, built by
     *                {@link com.articulate.sigma.FormulaPreprocessor#findTypeRestrictionsExpr}
     *                plus world-variable entries from modal processing
     * @return the THF formula string
     */
    public static String translate(Expr expr, boolean query,
                                   Map<String, Set<String>> typeMap) {
        String body = translateExpr(expr, false, typeMap, true);
        Set<String> freeVars = ExprToTPTP.collectFreeVars(expr);
        if (!freeVars.isEmpty()) {
            String quantStr = query ? "? [" : "! [";
            String varList = buildTypedVarList(freeVars, typeMap, true);
            return "( " + quantStr + varList + "] : (" + body + " ) )";
        }
        return body;
    }

    /**
     * Translate an {@link Expr} tree to a THF formula string (non-modal / plain mode).
     *
     * <p>No world or modal types are used; variables are either {@code $i} or {@code $o}.
     *
     * @param expr    the formula tree
     * @param query   {@code true} → free vars get existential quantifier
     * @param typeMap variable type hints (only {@code "Formula"} is honoured to assign
     *                {@code $o}; everything else maps to {@code $i})
     * @return the THF formula string
     */
    public static String translateNonModal(Expr expr, boolean query,
                                           Map<String, Set<String>> typeMap) {
        String body = translateExpr(expr, false, typeMap, false);
        Set<String> freeVars = ExprToTPTP.collectFreeVars(expr);
        if (!freeVars.isEmpty()) {
            String quantStr = query ? "? [" : "! [";
            String varList = buildTypedVarList(freeVars, typeMap, false);
            return "( " + quantStr + varList + "] : (" + body + " ) )";
        }
        return body;
    }

    // -----------------------------------------------------------------------
    // Core translation
    // -----------------------------------------------------------------------

    /**
     * Translate a single {@link Expr} node.
     *
     * @param expr      the node to translate
     * @param isHead    {@code true} when this node is in head (predicate/function) position
     * @param typeMap   variable type map
     * @param modalMode {@code true} for modal embedding (enables world/modal types)
     */
    static String translateExpr(Expr expr, boolean isHead,
                                 Map<String, Set<String>> typeMap,
                                 boolean modalMode) {
        return switch (expr) {
            case Expr.Var  v  -> ExprToTPTP.translateVarName(v.name());
            case Expr.RowVar rv -> ExprToTPTP.translateVarName(rv.name());
            case Expr.NumLiteral n -> translateNumberTHF(n.value());
            case Expr.StrLiteral s ->
                    s.value().replaceAll("[\n\t\r\f]", " ").replaceAll("'", "");
            // Atoms use the same s__ prefix as FOF (arg-position __m logic is shared)
            case Expr.Atom a -> ExprToTPTP.translateAtom(a.name(), isHead, "fof");
            case Expr.SExpr se -> translateSExprTHF(se, typeMap, modalMode);
        };
    }

    private static String translateSExprTHF(Expr.SExpr se,
                                             Map<String, Set<String>> typeMap,
                                             boolean modalMode) {
        String headName = se.headName();
        if (headName == null) {
            if (se.head() == null) {
                // Explicit null head: variable list inside a quantifier (e.g. [?X, ?Y])
                return se.args().stream()
                        .map(a -> translateExpr(a, false, typeMap, modalMode))
                        .collect(Collectors.joining(","));
            } else {
                // Var/RowVar head: higher-order predicate application, e.g. (?REL ?X ?Y)
                // → (V__REL @ V__X @ V__Y)   (valid THF higher-order application)
                return translateApplicationTHF(se, typeMap, modalMode);
            }
        }

        return switch (headName) {
            case "not" -> {
                if (se.args().size() != 1) yield errorStr("not");
                yield "~(" + translateExpr(se.args().get(0), false, typeMap, modalMode) + ")";
            }
            case "and" -> {
                if (se.args().size() < 2) yield errorStr("and");
                yield "(" + se.args().stream()
                        .map(a -> translateExpr(a, false, typeMap, modalMode))
                        .collect(Collectors.joining(" & ")) + ")";
            }
            case "or" -> {
                if (se.args().size() < 2) yield errorStr("or");
                yield "(" + se.args().stream()
                        .map(a -> translateExpr(a, false, typeMap, modalMode))
                        .collect(Collectors.joining(" | ")) + ")";
            }
            case "xor" -> {
                if (se.args().size() < 2) yield errorStr("xor");
                yield "(" + se.args().stream()
                        .map(a -> translateExpr(a, false, typeMap, modalMode))
                        .collect(Collectors.joining(" <~> ")) + ")";
            }
            case "=>" -> {
                if (se.args().size() != 2) yield errorStr("=>");
                String ant = translateExpr(se.args().get(0), false, typeMap, modalMode);
                String con = translateExpr(se.args().get(1), false, typeMap, modalMode);
                yield "(" + ant + " => " + con + ")";
            }
            case "<=>" -> {
                if (se.args().size() != 2) yield errorStr("<=>");
                String lhs = translateExpr(se.args().get(0), false, typeMap, modalMode);
                String rhs = translateExpr(se.args().get(1), false, typeMap, modalMode);
                // Expand biconditional as two implications (mirrors THFnew.processLogOp)
                yield "((" + lhs + " => " + rhs + ") & (" + rhs + " => " + lhs + "))";
            }
            case "equal" -> {
                if (se.args().size() != 2) yield errorStr("equal");
                String lhs = translateExpr(se.args().get(0), false, typeMap, modalMode);
                String rhs = translateExpr(se.args().get(1), false, typeMap, modalMode);
                yield "(" + lhs + " = " + rhs + ")";
            }
            case "forall" -> translateQuantifierTHF("! ", se, typeMap, modalMode);
            case "exists" -> translateQuantifierTHF("? ", se, typeMap, modalMode);
            // accreln2 / accreln3: first argument is the modal operator (type m).
            // It must be translated in head position (no __m suffix) so its type stays m,
            // not $i.  This mirrors the old processRecurse() check at THFnew.java:168.
            case "accreln2", "accreln3" -> translateAccrelnTHF(se, typeMap, modalMode);
            default       -> translateApplicationTHF(se, typeMap, modalMode);
        };
    }

    /**
     * Translate a THF quantified expression with typed variable list.
     * <p>Example: {@code (forall (?X ?W1) body)}
     *   → {@code ( ! [V__X:$i,V__W1:w] : (body))}
     */
    private static String translateQuantifierTHF(String quantOp, Expr.SExpr se,
                                                  Map<String, Set<String>> typeMap,
                                                  boolean modalMode) {
        if (se.args().size() != 2) return errorStr(se.headName());
        Expr varListExpr = se.args().get(0);
        Expr body        = se.args().get(1);

        List<String> kifVars = new ArrayList<>();
        if (varListExpr instanceof Expr.SExpr varSe) {
            for (Expr v : varSe.args()) {
                String raw = switch (v) {
                    case Expr.Var   vv -> vv.name();
                    case Expr.RowVar rv -> rv.name();
                    default            -> null;
                };
                if (raw != null) kifVars.add(raw);
            }
        }
        if (kifVars.isEmpty())
            return translateExpr(body, false, typeMap, modalMode);

        String typedVarList = kifVars.stream()
                .map(v -> ExprToTPTP.translateVarName(v) + ":" + thfType(v, typeMap, modalMode))
                .collect(Collectors.joining(","));

        return "(" + quantOp + "[" + typedVarList + "] : (" +
               translateExpr(body, false, typeMap, modalMode) + "))";
    }

    /**
     * Translate an {@code accreln2} or {@code accreln3} application.
     *
     * <p>These accessibility-relation predicates are produced by the Kripke modal rewrite:
     * <ul>
     *   <li>{@code accreln2(pred, agent, W1, W2)} — for binary HOL predicates</li>
     *   <li>{@code accreln3(pred, a1, a2, W1, W2)} — for ternary HOL predicates</li>
     * </ul>
     * The first argument is always the modal operator itself (e.g. {@code holdsDuring},
     * {@code believes}) which has type {@code m} in the header.  It must be translated
     * in <em>head position</em> (no {@code __m} suffix) to preserve that type; using
     * argument-position translation would produce {@code s__holdsDuring__m : $i} which
     * mismatches the {@code m} type expected by {@code accreln2/3}.
     *
     * <p>This mirrors the old {@code processRecurse()} guard in {@code THFnew.java} that
     * set {@code hasArguments = true} for {@code regHOLpred} / {@code regHOL3pred} atoms.
     */
    private static String translateAccrelnTHF(Expr.SExpr se,
                                               Map<String, Set<String>> typeMap,
                                               boolean modalMode) {
        String head = translateExpr(se.head(), true, typeMap, modalMode);
        if (se.args().isEmpty()) return head;

        StringBuilder sb = new StringBuilder("(").append(head);
        for (int i = 0; i < se.args().size(); i++) {
            Expr arg = se.args().get(i);
            // Arg 0 is the modal operator (type m) — translate in head position (no __m)
            boolean argInHead = (i == 0);
            sb.append(" @ ").append(translateExpr(arg, argInHead, typeMap, modalMode));
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Translate a THF application using curried {@code @} syntax.
     * <p>Example: {@code (pred arg1 arg2)} → {@code (s__pred @ tptp_arg1 @ tptp_arg2)}
     */
    private static String translateApplicationTHF(Expr.SExpr se,
                                                   Map<String, Set<String>> typeMap,
                                                   boolean modalMode) {
        String head = translateExpr(se.head(), true, typeMap, modalMode);
        if (se.args().isEmpty()) return head;

        StringBuilder sb = new StringBuilder("(").append(head);
        for (Expr arg : se.args()) {
            sb.append(" @ ").append(translateExpr(arg, false, typeMap, modalMode));
        }
        sb.append(")");
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Type resolution
    // -----------------------------------------------------------------------

    /**
     * Map a KIF variable name to its THF base type.
     *
     * <p>In modal mode:
     * <ul>
     *   <li>{@code ?W<digits>} → {@code w} (Kripke world variable, by naming convention)</li>
     *   <li>type-set contains {@code "Formula"} → {@code (w > $o)}</li>
     *   <li>type-set contains {@code "World"}   → {@code w}</li>
     *   <li>type-set contains {@code "Modal"}   → {@code m}</li>
     *   <li>otherwise                           → {@code $i}</li>
     * </ul>
     *
     * <p>In non-modal mode:
     * <ul>
     *   <li>type-set contains {@code "Formula"} → {@code $o}</li>
     *   <li>otherwise                           → {@code $i}</li>
     * </ul>
     *
     * <p>This replicates the logic of {@link com.articulate.sigma.trans.THFnew#getTHFtype}
     * and {@link com.articulate.sigma.trans.THFnew#getTHFtypeNonModal} without introducing
     * a dependency from the {@code parsing} package to the {@code trans} package.</p>
     */
    public static String thfType(String kifVar, Map<String, Set<String>> typeMap,
                                  boolean modalMode) {
        if (modalMode) {
            // World variables identified by naming convention — mirrors getTHFtype()?W\d+ check
            if (kifVar.matches("\\?W\\d+")) return "w";
            Set<String> types = typeMap.get(kifVar);
            if (types == null) return "$i";
            if (types.contains("Formula")) return "(w > $o)";
            if (types.contains("World"))   return "w";
            if (types.contains("Modal"))   return "m";
            return "$i";
        } else {
            Set<String> types = typeMap.get(kifVar);
            if (types == null) return "$i";
            if (types.contains("Formula")) return "$o";
            return "$i";
        }
    }

    /**
     * Build a comma-separated typed-variable list string for THF quantifiers
     * and free-variable wrappers.
     * <p>Example output: {@code "V__X:$i,V__W1:w,V__F:(w > $o)"}
     */
    static String buildTypedVarList(Collection<String> kifVars,
                                     Map<String, Set<String>> typeMap,
                                     boolean modalMode) {
        return kifVars.stream()
                .map(v -> ExprToTPTP.translateVarName(v) + ":" + thfType(v, typeMap, modalMode))
                .collect(Collectors.joining(","));
    }

    // -----------------------------------------------------------------------
    // Number translation
    // -----------------------------------------------------------------------

    /**
     * THF uses the same {@code n__N} prefix as FOF for numeric constants.
     * (Numbers become {@code $i} constants tagged with {@code n__} so they
     * are well-typed in the higher-order domain.)
     */
    static String translateNumberTHF(String value) {
        String safe = value.replace('.', '_').replace('-', '_');
        return "n__" + safe;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String errorStr(String op) {
        if (debug)
            System.err.println("ExprToTHF: wrong number of arguments to " + op);
        return "";
    }
}
