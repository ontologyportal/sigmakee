package com.articulate.sigma.parsing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.articulate.sigma.Formula;

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

        Map<String, Set<String>> localTypeMap = copyTypeMap(typeMap);
        markPlainFormulaVars(expr, localTypeMap);
        String body = translateExpr(expr, false, localTypeMap, false);
        Set<String> freeVars = collectFreeVarsNonModal(expr);

        if (!freeVars.isEmpty()) {
            String quantStr = query ? "? [" : "! [";
            String varList = buildTypedVarList(freeVars, localTypeMap, false);
            return "( " + quantStr + varList + "] : (" + body + " ) )";
        }
        return body;
    }

    private static Map<String, Set<String>> copyTypeMap(Map<String, Set<String>> typeMap) {

        Map<String, Set<String>> result = new java.util.HashMap<>();
        if (typeMap == null)
            return result;

        for (Map.Entry<String, Set<String>> e : typeMap.entrySet())
            result.put(e.getKey(), new java.util.HashSet<>(e.getValue()));

        return result;
    }

    private static void addFormulaType(String var, Map<String, Set<String>> typeMap) {

        typeMap.computeIfAbsent(var, k -> new java.util.HashSet<>()).add("Formula");
    }

    /**
     * Plain THF uses SUMO Formula variables as THF propositions ($o).
     * Detect the common cases directly so user assertions do not emit
     * V__A:$i together with (~ V__A).
     */
    private static void markPlainFormulaVars(Expr e, Map<String, Set<String>> typeMap) {

        if (!(e instanceof Expr.SExpr se))
            return;

        String headName = se.headName();
        List<Expr> args = se.args();

        if ("instance".equals(headName) && args.size() == 2 &&
                args.get(0) instanceof Expr.Var v &&
                args.get(1) instanceof Expr.Atom a &&
                "Formula".equals(a.name())) {
            addFormulaType(v.name(), typeMap);
        }

        if (("not".equals(headName) || "~".equals(headName)) && args.size() == 1 &&
                args.get(0) instanceof Expr.Var v) {
            addFormulaType(v.name(), typeMap);
        }

        if ("modalAttribute".equals(headName) && !args.isEmpty() &&
                args.get(0) instanceof Expr.Var v) {
            addFormulaType(v.name(), typeMap);
        }

        if (se.head() != null)
            markPlainFormulaVars(se.head(), typeMap);

        for (Expr arg : args)
            markPlainFormulaVars(arg, typeMap);
    }

    // -----------------------------------------------------------------------
    // Core translation
    // -----------------------------------------------------------------------

    private static boolean isHead(Expr.SExpr se, String name) {
        return se.head() instanceof Expr.Atom a && name.equals(a.name());
    }


    private static String translateKappaFnNonModal(Expr.SExpr se, Map<String, Set<String>> typeMap) {

        List<Expr> args = se.args();
        if (args.size() != 2)
            return translateApplicationTHF(se, typeMap, false);
        Expr varExpr = args.get(0);
        Expr bodyExpr = args.get(1);
        if (!(varExpr instanceof Expr.Var v))
            return translateApplicationTHF(se, typeMap, false);
        String vName = ExprToTPTP.translateVarName(v.name());
        String vType = "$i";
        String body = translateExpr(bodyExpr, false, typeMap, false);
        return "(s__KappaFn @ (^ [" + vName + ":" + vType + "] : (" + body + ")))";
    }

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

    public static Set<String> collectPlainFormulaAtoms(Expr e) {

        Set<String> result = new java.util.LinkedHashSet<>();
        collectPlainFormulaAtoms(e, result);
        return result;
    }

    public static String plainFormulaAtomName(String name) {

        String t = ExprToTPTP.translateAtom(name, true, "fof");
        if (t.startsWith("s__"))
            return "p__" + t.substring(3);
        return "p__" + t;
    }

    private static String translatePlainFormulaArg(Expr e, Map<String, Set<String>> typeMap) {

        if (e instanceof Expr.Atom a)
            return plainFormulaAtomName(a.name());
        return translateExpr(e, false, typeMap, false);
    }

    private static void collectPlainFormulaAtoms(Expr e, Set<String> result) {

        if (!(e instanceof Expr.SExpr se))
            return;

        String headName = se.headName();
        List<Expr> args = se.args();

        if ("instance".equals(headName) && args.size() == 2 &&
                args.get(0) instanceof Expr.Atom a0 &&
                args.get(1) instanceof Expr.Atom a1 &&
                "Formula".equals(a1.name())) {
            result.add(a0.name());
        }

        if ("modalAttribute".equals(headName) && args.size() >= 1 &&
                args.get(0) instanceof Expr.Atom a) {
            result.add(a.name());
        }

        if (("not".equals(headName) || "~".equals(headName)) && args.size() == 1 &&
                args.get(0) instanceof Expr.Atom a) {
            result.add(a.name());
        }

        if (se.head() != null)
            collectPlainFormulaAtoms(se.head(), result);

        for (Expr arg : args)
            collectPlainFormulaAtoms(arg, result);
    }

    private static String translateSExprTHF(Expr.SExpr se,
                                         Map<String, Set<String>> typeMap,
                                         boolean modalMode) {
        
        String headName = se.headName();
        if (headName == null) {
            if (se.head() == null) {
                return se.args().stream()
                        .map(a -> translateExpr(a, false, typeMap, modalMode))
                        .collect(Collectors.joining(","));
            }
            return translateApplicationTHF(se, typeMap, modalMode);
        }

        if (!modalMode && "KappaFn".equals(headName)) {
            String kappa = translateKappaFnNonModal(se, typeMap);
            if (kappa != null)
                return kappa;
        }
        if (!modalMode && "instance".equals(headName) && se.args().size() == 2) {
            Expr arg0 = se.args().get(0);
            Expr arg1 = se.args().get(1);
            if ((arg0 instanceof Expr.Var || arg0 instanceof Expr.Atom) && arg1 instanceof Expr.Atom a && "Formula".equals(a.name()))
                return "$true";
        }
        if (!modalMode && "modalAttribute".equals(headName) && se.args().size() == 2) {
            String formulaArg = translatePlainFormulaArg(se.args().get(0), typeMap);
            String attrArg = translateExpr(se.args().get(1), false, typeMap, false);
            return "(s__modalAttribute @ " + formulaArg + " @ " + attrArg + ")";
        }
        return switch (headName) {
            case "not" -> {
                if (se.args().size() != 1) yield errorStr("not");
                yield "(~ " + translateExpr(se.args().get(0), false, typeMap, modalMode) + ")";
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
            // accreln2 / accreln3 / accreln3norm: first argument is the modal operator (type m).
            // It must be translated in head position (no __m suffix) so its type stays m,
            // not $i.  This mirrors the old processRecurse() check at THFnew.java:168.
            // accreln3norm is the variant used for regHOL3Modalpred predicates (confersNorm,
            // deprivesNorm) where the second extra argument is Modal-typed; the same
            // arg-0 = modal-operator convention applies.
            case "accreln2", "accreln3", "accreln3norm" -> translateAccrelnTHF(se, typeMap, modalMode);
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

        if (kifVar == null) return "$i";

        // Generated row variables are term variables, not formula variables.
        // They can accidentally inherit Formula from broad type restrictions.
        if (kifVar.matches("\\?ROW\\d+") || kifVar.matches("V__ROW\\d+"))
            return "$i";

        if (modalMode) {
            Set<String> types = typeMap == null ? null : typeMap.get(kifVar);

            // Defensive fallback in case caller passes translated TPTP var name.
            if (types == null && kifVar.startsWith("V__"))
                types = typeMap == null ? null : typeMap.get("?" + kifVar.substring(3));

            if (types == null) {
                if (kifVar.matches("\\?W+\\d+") || kifVar.matches("V__W+\\d+"))
                    return "w";
                return "$i";
            }

            if (types.contains("World")) return "w";
            if (types.contains("Formula")) return "(w > $o)";
            if (types.contains("Modal")) return "m";
            return "$i";
        }
        else {
            Set<String> types = typeMap == null ? null : typeMap.get(kifVar);
            if (types == null && kifVar.startsWith("V__"))
                types = typeMap == null ? null : typeMap.get("?" + kifVar.substring(3));

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

    private static Set<String> collectFreeVarsNonModal(Expr e) {

        Set<String> free = new java.util.LinkedHashSet<>();
        collectFreeVarsNonModal(e, new java.util.HashSet<>(), free);
        return free;
    }

    private static void collectFreeVarsNonModal(Expr e, Set<String> bound, Set<String> free) {

        if (e instanceof Expr.Var v) {
            if (!bound.contains(v.name()))
                free.add(v.name());
            return;
        }
        if (!(e instanceof Expr.SExpr se))
            return;
        if ("KappaFn".equals(se.headName()) &&
                se.args().size() == 2 &&
                se.args().get(0) instanceof Expr.Var v) {
            Set<String> scoped = new java.util.HashSet<>(bound);
            scoped.add(v.name());
            collectFreeVarsNonModal(se.args().get(1), scoped, free);
            return;
        }
        if ((Formula.EQUANT.equals(se.headName()) || Formula.UQUANT.equals(se.headName())) &&
                se.args().size() >= 2) {
            Set<String> scoped = new java.util.HashSet<>(bound);
            collectQuantifierVars(se.args().get(0), scoped);
            collectFreeVarsNonModal(se.args().get(1), scoped, free);
            return;
        }
        if (se.head() != null)
            collectFreeVarsNonModal(se.head(), bound, free);
        for (Expr arg : se.args())
            collectFreeVarsNonModal(arg, bound, free);
    }

    private static void collectQuantifierVars(Expr varListExpr, Set<String> bound) {

        if (!(varListExpr instanceof Expr.SExpr varList))
            return;
        for (Expr v : varList.args()) {
            switch (v) {
                case Expr.Var vv -> bound.add(vv.name());
                case Expr.RowVar rv -> bound.add(rv.name());
                default -> { }
            }
        }
    }
}