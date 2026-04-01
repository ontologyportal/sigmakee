package com.articulate.sigma.parsing;

import com.articulate.sigma.Formula;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pure {@link Expr} → {@link Expr} CNF clausification pipeline.
 *
 * <p>This is the Expr-based counterpart of {@link com.articulate.sigma.Clausifier}.
 * Every step is a stateless static method; the only mutable state is the
 * {@code int[]} counter(s) threaded through the call tree for generating
 * unique names.</p>
 *
 * <p>Pipeline (mirrors the string-based Clausifier):
 * <ol>
 *   <li>{@link #equivalencesOut(Expr)} — eliminate {@code <=>}</li>
 *   <li>{@link #implicationsOut(Expr)} — eliminate {@code =>}</li>
 *   <li>{@link #negationsIn(Expr)}     — push {@code not} to narrowest scope (NNF)</li>
 *   <li>{@link #renameVariables(Expr, int[])} — give each variable a unique name</li>
 *   <li>{@link #existentialsOut(Expr, int[])} — Skolemize existential quantifiers</li>
 *   <li>{@link #universalsOut(Expr)}   — drop {@code forall} wrappers</li>
 *   <li>{@link #disjunctionsIn(Expr)}  — distribute {@code or} over {@code and}</li>
 *   <li>{@link #standardizeApart(Expr, int[])} — rename vars per clause</li>
 * </ol>
 */
public class ClausifierExpr {

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------

    /**
     * Full CNF clausification pipeline.  Returns a new {@link Expr} tree in
     * conjunctive normal form (a conjunction of disjunctions of literals).
     */
    public static Expr clausify(Expr expr) {
        int[] varIdx    = {0};
        int[] skolemIdx = {0};
        expr = equivalencesOut(expr);
        expr = implicationsOut(expr);
        expr = negationsIn(expr);
        expr = renameVariables(expr, varIdx);
        expr = existentialsOut(expr, skolemIdx);
        expr = universalsOut(expr);
        expr = disjunctionsIn(expr);
        expr = standardizeApart(expr, varIdx);
        return expr;
    }

    // -----------------------------------------------------------------------
    // Internal factory helpers
    // -----------------------------------------------------------------------

    /** Build an S-expression with an Atom head. */
    static Expr.SExpr sexpr(String headName, List<Expr> args) {
        return new Expr.SExpr(new Expr.Atom(headName), args);
    }

    static Expr.SExpr sexpr(String headName, Expr... args) {
        return sexpr(headName, List.of(args));
    }

    private static boolean isQuantifier(String name) {
        return "forall".equals(name) || "exists".equals(name);
    }

    /** Return the variable name for a Var or RowVar, or null for any other node. */
    private static String varName(Expr node) {
        return switch (node) {
            case Expr.Var  v  -> v.name();
            case Expr.RowVar rv -> rv.name();
            default           -> null;
        };
    }

    // -----------------------------------------------------------------------
    // Step 1: equivalencesOut — eliminate <=>
    //   (<=> A B)  →  (and (=> A B) (=> B A))
    // -----------------------------------------------------------------------

    public static Expr equivalencesOut(Expr expr) {
        return switch (expr) {
            case Expr.SExpr se when "<=>".equals(se.headName()) && se.args().size() == 2 -> {
                Expr a = equivalencesOut(se.args().get(0));
                Expr b = equivalencesOut(se.args().get(1));
                yield sexpr("and", sexpr("=>", a, b), sexpr("=>", b, a));
            }
            case Expr.SExpr se -> {
                Expr newHead = se.head() != null ? equivalencesOut(se.head()) : null;
                List<Expr> newArgs = se.args().stream()
                        .map(ClausifierExpr::equivalencesOut)
                        .toList();
                yield new Expr.SExpr(newHead, newArgs);
            }
            default -> expr;
        };
    }

    // -----------------------------------------------------------------------
    // Step 2: implicationsOut — eliminate =>
    //   (=> A B)  →  (or (not A) B)
    // -----------------------------------------------------------------------

    public static Expr implicationsOut(Expr expr) {
        return switch (expr) {
            case Expr.SExpr se when "=>".equals(se.headName()) && se.args().size() == 2 -> {
                Expr a = implicationsOut(se.args().get(0));
                Expr b = implicationsOut(se.args().get(1));
                yield sexpr("or", sexpr("not", a), b);
            }
            case Expr.SExpr se -> {
                Expr newHead = se.head() != null ? implicationsOut(se.head()) : null;
                List<Expr> newArgs = se.args().stream()
                        .map(ClausifierExpr::implicationsOut)
                        .toList();
                yield new Expr.SExpr(newHead, newArgs);
            }
            default -> expr;
        };
    }

    // -----------------------------------------------------------------------
    // Step 3: negationsIn — NNF, push 'not' to narrowest scope
    // Fixed-point iteration; termination is detected via structural Expr equality
    // (records provide structural equals() automatically).
    // -----------------------------------------------------------------------

    public static Expr negationsIn(Expr expr) {
        Expr prev = expr;
        Expr curr = negationsIn_1(expr);
        while (!prev.equals(curr)) {
            prev = curr;
            curr = negationsIn_1(curr);
        }
        return curr;
    }

    static Expr negationsIn_1(Expr expr) {
        if (!(expr instanceof Expr.SExpr se) || se.args().isEmpty()) return expr;
        String headName = se.headName();

        if ("not".equals(headName) && se.args().size() == 1) {
            Expr inner = se.args().get(0);
            if (inner instanceof Expr.SExpr innerSe) {
                String ih = innerSe.headName();

                // (not (not X)) → recurse into X
                if ("not".equals(ih) && innerSe.args().size() == 1)
                    return negationsIn_1(innerSe.args().get(0));

                // De Morgan: (not (and A B ...)) → (or (not A) (not B) ...)
                if ("and".equals(ih)) {
                    List<Expr> pushed = innerSe.args().stream()
                            .map(a -> negationsIn_1(sexpr("not", a)))
                            .toList();
                    return sexpr("or", pushed);
                }

                // De Morgan: (not (or A B ...)) → (and (not A) (not B) ...)
                if ("or".equals(ih)) {
                    List<Expr> pushed = innerSe.args().stream()
                            .map(a -> negationsIn_1(sexpr("not", a)))
                            .toList();
                    return sexpr("and", pushed);
                }

                // (not (forall (?X..) body)) → (exists (?X..) (not body))
                if ("forall".equals(ih) && innerSe.args().size() == 2) {
                    Expr varList = innerSe.args().get(0);
                    Expr body    = innerSe.args().get(1);
                    return sexpr("exists", varList, negationsIn_1(sexpr("not", body)));
                }

                // (not (exists (?X..) body)) → (forall (?X..) (not body))
                if ("exists".equals(ih) && innerSe.args().size() == 2) {
                    Expr varList = innerSe.args().get(0);
                    Expr body    = innerSe.args().get(1);
                    return sexpr("forall", varList, negationsIn_1(sexpr("not", body)));
                }

                // (not (pred A B ...)) — non-reducible: recurse into inner
                return sexpr("not", negationsIn_1(inner));
            }
            // (not <atom-or-var>) — nothing to push through
            return expr;
        }

        // Quantifier: recurse only into body; leave var list unchanged
        if (isQuantifier(headName) && se.args().size() == 2) {
            Expr varList = se.args().get(0);
            Expr body    = negationsIn_1(se.args().get(1));
            return sexpr(headName, varList, body);
        }

        // All other compound nodes: recurse into all args
        List<Expr> newArgs = se.args().stream()
                .map(ClausifierExpr::negationsIn_1)
                .toList();
        return new Expr.SExpr(se.head(), newArgs);
    }

    // -----------------------------------------------------------------------
    // Step 4: renameVariables — assign each occurrence of a variable a fresh
    //   unique name to prevent variable capture in later steps.
    // -----------------------------------------------------------------------

    /**
     * Rename all variables (Var and RowVar) to unique names.
     * Implicitly universally quantified variables (appearing free at the top
     * level) are renamed consistently; each explicitly quantified variable gets
     * a fresh name scoped to its quantifier.
     *
     * @param varIdx  A single-element array used as a mutable counter; it is
     *                updated in place so the same array can be reused across
     *                subsequent steps that also need fresh names.
     */
    public static Expr renameVariables(Expr expr, int[] varIdx) {
        Map<String, String> topLevelVars  = new HashMap<>();
        Map<String, String> scopedRenames = new HashMap<>();
        return renameVariables_1(expr, topLevelVars, scopedRenames, varIdx);
    }

    private static Expr renameVariables_1(Expr expr,
                                           Map<String, String> topLevelVars,
                                           Map<String, String> scopedRenames,
                                           int[] varIdx) {
        return switch (expr) {
            case Expr.Var v -> {
                String rnv = scopedRenames.getOrDefault(v.name(),
                             topLevelVars.get(v.name()));
                if (rnv == null) {
                    rnv = Formula.VX + (++varIdx[0]);
                    topLevelVars.put(v.name(), rnv);
                }
                yield new Expr.Var(rnv);
            }
            case Expr.RowVar rv -> {
                String rnv = scopedRenames.getOrDefault(rv.name(),
                             topLevelVars.get(rv.name()));
                if (rnv == null) {
                    rnv = Formula.RVAR + "VAR" + (++varIdx[0]);
                    topLevelVars.put(rv.name(), rnv);
                }
                yield new Expr.RowVar(rnv);
            }
            case Expr.SExpr se when isQuantifier(se.headName()) && se.args().size() == 2 -> {
                // Create a child scope: copy scopedRenames so we don't leak
                Map<String, String> childScope = new HashMap<>(scopedRenames);
                Expr varListExpr = se.args().get(0);

                // Rename each variable in the var list to a fresh name
                List<Expr> newVarList = new ArrayList<>();
                if (varListExpr instanceof Expr.SExpr varSe) {
                    for (Expr v : varSe.args()) {
                        String old = varName(v);
                        if (old != null) {
                            boolean isRow = (v instanceof Expr.RowVar);
                            String fresh = isRow
                                    ? Formula.RVAR + "VAR" + (++varIdx[0])
                                    : Formula.VX   + (++varIdx[0]);
                            childScope.put(old, fresh);
                            newVarList.add(isRow ? new Expr.RowVar(fresh) : new Expr.Var(fresh));
                        } else {
                            newVarList.add(v);
                        }
                    }
                }
                Expr newVarListExpr = new Expr.SExpr(null, newVarList);
                Expr newBody = renameVariables_1(se.args().get(1), topLevelVars, childScope, varIdx);
                yield sexpr(se.headName(), newVarListExpr, newBody);
            }
            case Expr.SExpr se -> {
                // null-head var list or ordinary compound
                Expr newHead = se.head() != null
                        ? renameVariables_1(se.head(), topLevelVars, scopedRenames, varIdx)
                        : null;
                List<Expr> newArgs = se.args().stream()
                        .map(a -> renameVariables_1(a, topLevelVars, scopedRenames, varIdx))
                        .toList();
                yield new Expr.SExpr(newHead, newArgs);
            }
            default -> expr;
        };
    }

    // -----------------------------------------------------------------------
    // Step 5: existentialsOut — Skolemization
    //   Replace each existentially quantified variable with a Skolem constant
    //   (if no surrounding universals) or a Skolem function applied to the
    //   universally quantified variables in scope.
    // -----------------------------------------------------------------------

    /**
     * Eliminate all existential quantifiers via Skolemization.
     *
     * @param skolemIdx Mutable counter for generating unique Skolem names.
     */
    public static Expr existentialsOut(Expr expr, int[] skolemIdx) {
        Set<String> iUQVs      = new TreeSet<>();   // implicitly universally quantified
        Set<String> scopedVars = new TreeSet<>();
        collectIUQVars(expr, iUQVs, scopedVars);
        return existentialsOut_1(expr, new HashMap<>(), iUQVs, new TreeSet<>(), skolemIdx);
    }

    /** Collect variables that appear free (i.e., implicitly universally quantified). */
    private static void collectIUQVars(Expr expr, Set<String> iuqvs, Set<String> scopedVars) {
        switch (expr) {
            case Expr.Var v -> {
                if (!scopedVars.contains(v.name())) iuqvs.add(v.name());
            }
            case Expr.RowVar rv -> {
                if (!scopedVars.contains(rv.name())) iuqvs.add(rv.name());
            }
            case Expr.SExpr se when isQuantifier(se.headName()) && se.args().size() == 2 -> {
                Set<String> child = new TreeSet<>(scopedVars);
                Expr varListExpr = se.args().get(0);
                if (varListExpr instanceof Expr.SExpr varSe)
                    varSe.args().stream().map(ClausifierExpr::varName)
                            .filter(Objects::nonNull).forEach(child::add);
                collectIUQVars(se.args().get(1), iuqvs, child);
            }
            case Expr.SExpr se -> {
                for (Expr a : se.args()) collectIUQVars(a, iuqvs, scopedVars);
            }
            default -> { }
        }
    }

    /**
     * @param evSubs      existential var → Skolem term substitutions
     * @param iUQVs       implicitly universally quantified variables
     * @param scopedUQVs  explicitly universally quantified variables currently in scope
     */
    private static Expr existentialsOut_1(Expr expr,
                                           Map<String, Expr> evSubs,
                                           Set<String> iUQVs,
                                           Set<String> scopedUQVs,
                                           int[] skolemIdx) {
        return switch (expr) {
            case Expr.Var v -> {
                Expr sub = evSubs.get(v.name());
                yield sub != null ? sub : expr;
            }
            case Expr.SExpr se when "forall".equals(se.headName()) && se.args().size() == 2 -> {
                // Accumulate universally quantified vars for Skolem generation
                Set<String> childUQVs = new TreeSet<>(scopedUQVs);
                Expr varListExpr = se.args().get(0);
                if (varListExpr instanceof Expr.SExpr varSe)
                    varSe.args().stream().map(ClausifierExpr::varName)
                            .filter(Objects::nonNull).forEach(childUQVs::add);
                Expr newBody = existentialsOut_1(se.args().get(1), evSubs, iUQVs, childUQVs, skolemIdx);
                yield sexpr("forall", varListExpr, newBody);
            }
            case Expr.SExpr se when "exists".equals(se.headName()) && se.args().size() == 2 -> {
                // Build Skolem term for each existential variable
                Set<String> uQVs = new TreeSet<>(iUQVs);
                uQVs.addAll(scopedUQVs);
                Map<String, Expr> childSubs = new HashMap<>(evSubs);
                Expr varListExpr = se.args().get(0);
                if (varListExpr instanceof Expr.SExpr varSe) {
                    for (Expr v : varSe.args()) {
                        String eVar = varName(v);
                        if (eVar != null)
                            childSubs.put(eVar, newSkolemTerm(uQVs, skolemIdx));
                    }
                }
                // Drop the existential quantifier; return the skolemized body
                yield existentialsOut_1(se.args().get(1), childSubs, iUQVs, scopedUQVs, skolemIdx);
            }
            case Expr.SExpr se -> {
                Expr newHead = se.head() != null
                        ? existentialsOut_1(se.head(), evSubs, iUQVs, scopedUQVs, skolemIdx)
                        : null;
                List<Expr> newArgs = se.args().stream()
                        .map(a -> existentialsOut_1(a, evSubs, iUQVs, scopedUQVs, skolemIdx))
                        .toList();
                yield new Expr.SExpr(newHead, newArgs);
            }
            default -> expr;
        };
    }

    /** Create a new Skolem constant or function. */
    private static Expr newSkolemTerm(Set<String> uqVars, int[] skolemIdx) {
        int idx = ++skolemIdx[0];
        if (uqVars.isEmpty())
            return new Expr.Atom(Formula.SK_PREF + idx);
        List<Expr> args = uqVars.stream()
                .map(v -> v.startsWith(Formula.R_PREF) ? (Expr) new Expr.RowVar(v) : new Expr.Var(v))
                .collect(Collectors.toList());
        return sexpr(Formula.SK_PREF + Formula.FN_SUFF + idx, args);
    }

    // -----------------------------------------------------------------------
    // Step 6: universalsOut — drop (forall (...) body) → body
    // -----------------------------------------------------------------------

    public static Expr universalsOut(Expr expr) {
        return switch (expr) {
            case Expr.SExpr se when "forall".equals(se.headName()) && se.args().size() == 2 ->
                    universalsOut(se.args().get(1));
            case Expr.SExpr se -> {
                Expr newHead = se.head() != null ? universalsOut(se.head()) : null;
                List<Expr> newArgs = se.args().stream()
                        .map(ClausifierExpr::universalsOut)
                        .toList();
                yield new Expr.SExpr(newHead, newArgs);
            }
            default -> expr;
        };
    }

    // -----------------------------------------------------------------------
    // Step 7: nestedOperatorsOut — flatten nested and/or, remove (not (not X))
    // -----------------------------------------------------------------------

    static Expr nestedOperatorsOut(Expr expr) {
        Expr prev = expr;
        Expr curr = nestedOperatorsOut_1(expr);
        while (!prev.equals(curr)) {
            prev = curr;
            curr = nestedOperatorsOut_1(curr);
        }
        return curr;
    }

    private static Expr nestedOperatorsOut_1(Expr expr) {
        if (!(expr instanceof Expr.SExpr se)) return expr;
        String headName = se.headName();
        if (headName == null) return expr; // null-head var list

        if ("and".equals(headName) || "or".equals(headName) || "not".equals(headName)) {
            List<Expr> newArgs = new ArrayList<>();
            for (Expr lit : se.args()) {
                if (lit instanceof Expr.SExpr litSe && headName.equals(litSe.headName())) {
                    if ("not".equals(headName)) {
                        // (not (not X)) → X
                        if (litSe.args().size() == 1)
                            return nestedOperatorsOut_1(litSe.args().get(0));
                        newArgs.add(nestedOperatorsOut_1(lit));
                    } else {
                        // (and (and A B) C) → splice A, B into parent
                        for (Expr inner : litSe.args())
                            newArgs.add(nestedOperatorsOut_1(inner));
                    }
                } else {
                    newArgs.add(nestedOperatorsOut_1(lit));
                }
            }
            return sexpr(headName, newArgs);
        }

        // Other compound: recurse into args
        List<Expr> newArgs = se.args().stream()
                .map(ClausifierExpr::nestedOperatorsOut_1)
                .toList();
        return new Expr.SExpr(se.head(), newArgs);
    }

    // -----------------------------------------------------------------------
    // Step 8: disjunctionsIn — distribute or over and
    //   (or P (and Q R)) → (and (or P Q) (or P R))
    // -----------------------------------------------------------------------

    public static Expr disjunctionsIn(Expr expr) {
        Expr prev = expr;
        Expr curr = disjunctionsIn_1(nestedOperatorsOut(expr));
        while (!prev.equals(curr)) {
            prev = curr;
            curr = disjunctionsIn_1(nestedOperatorsOut(curr));
        }
        return curr;
    }

    private static Expr disjunctionsIn_1(Expr expr) {
        if (!(expr instanceof Expr.SExpr se)) return expr;
        String headName = se.headName();

        if ("or".equals(headName)) {
            List<Expr> conjuncts = new ArrayList<>(); // args of the first (and …) child
            List<Expr> disjuncts = new ArrayList<>(); // remaining disjuncts

            for (Expr arg : se.args()) {
                if (conjuncts.isEmpty()
                        && arg instanceof Expr.SExpr argSe
                        && "and".equals(argSe.headName())) {
                    // Splice: the (and …) children become conjuncts, each recursed
                    for (Expr c : argSe.args())
                        conjuncts.add(disjunctionsIn_1(c));
                } else {
                    disjuncts.add(arg); // raw — processed when building (or ci D…)
                }
            }

            if (conjuncts.isEmpty()) return expr; // no (and …) child found

            // For each conjunct ci, build (or ci D1 D2 …) and recurse
            List<Expr> clauses = new ArrayList<>();
            for (Expr ci : conjuncts) {
                List<Expr> newOrArgs = new ArrayList<>();
                newOrArgs.add(ci);
                newOrArgs.addAll(disjuncts);
                clauses.add(disjunctionsIn_1(sexpr("or", newOrArgs)));
            }
            return sexpr("and", clauses);
        }

        // Non-or compound: recurse into args
        List<Expr> newArgs = se.args().stream()
                .map(ClausifierExpr::disjunctionsIn_1)
                .toList();
        return new Expr.SExpr(se.head(), newArgs);
    }

    // -----------------------------------------------------------------------
    // Step 9: standardizeApart — give each clause its own fresh variable names
    // -----------------------------------------------------------------------

    /**
     * Rename variables so that no two clauses in the conjunction share a name.
     *
     * @param varIdx Mutable counter; continues from wherever renameVariables left off.
     */
    public static Expr standardizeApart(Expr expr, int[] varIdx) {
        // Split top-level (and …) into individual clauses
        List<Expr> clauses;
        if (expr instanceof Expr.SExpr se && "and".equals(se.headName())) {
            clauses = new ArrayList<>(se.args());
        } else {
            clauses = new ArrayList<>();
            clauses.add(expr);
        }

        List<Expr> newClauses = new ArrayList<>();
        for (Expr clause : clauses) {
            Map<String, String> renames = new HashMap<>();
            newClauses.add(standardizeApart_1(clause, renames, varIdx));
        }

        if (newClauses.size() > 1)
            return sexpr("and", newClauses);
        return newClauses.get(0);
    }

    /** Rename every variable in {@code expr} using the per-clause {@code renames} map. */
    private static Expr standardizeApart_1(Expr expr,
                                            Map<String, String> renames,
                                            int[] varIdx) {
        return switch (expr) {
            case Expr.Var v -> {
                String rnv = renames.computeIfAbsent(v.name(), k -> Formula.VX + (++varIdx[0]));
                yield new Expr.Var(rnv);
            }
            case Expr.RowVar rv -> {
                String rnv = renames.computeIfAbsent(rv.name(), k -> Formula.RVAR + (++varIdx[0]));
                yield new Expr.RowVar(rnv);
            }
            case Expr.SExpr se -> {
                Expr newHead = se.head() != null
                        ? standardizeApart_1(se.head(), renames, varIdx)
                        : null;
                List<Expr> newArgs = se.args().stream()
                        .map(a -> standardizeApart_1(a, renames, varIdx))
                        .toList();
                yield new Expr.SExpr(newHead, newArgs);
            }
            default -> expr; // Atom, NumLiteral, StrLiteral — unchanged
        };
    }

    // -----------------------------------------------------------------------
    // Utility: normalizeVariables
    //   Rename all vars depth-first from index 1 — used for canonical form
    //   comparison.  Mirrors Clausifier.normalizeVariables().
    // -----------------------------------------------------------------------

    /**
     * Rename all variables in depth-first order starting from index 1, using
     * {@code ?VAR1}, {@code ?VAR2}, … for object variables and
     * {@code @ROWVAR1}, {@code @ROWVAR2}, … for row variables.
     *
     * @param replaceSkolemTerms If {@code true}, Skolem constants and
     *                           functions are also replaced with fresh
     *                           variable names (used when building query
     *                           expressions from the negated formula).
     */
    public static Expr normalizeVariables(Expr expr, boolean replaceSkolemTerms) {
        int[] idxs = {1, 1}; // [varIdx, rowVarIdx] — start from 1 to match original
        Map<String, String> vmap = new HashMap<>();
        return normalizeVariables_1(expr, idxs, vmap, replaceSkolemTerms);
    }

    public static Expr normalizeVariables(Expr expr) {
        return normalizeVariables(expr, false);
    }

    private static Expr normalizeVariables_1(Expr expr, int[] idxs,
                                              Map<String, String> vmap,
                                              boolean replaceSkolemTerms) {
        return switch (expr) {
            case Expr.Var v -> {
                String nv = vmap.computeIfAbsent(v.name(), k -> Formula.VVAR + (idxs[0]++));
                yield new Expr.Var(nv);
            }
            case Expr.RowVar rv -> {
                String nv = vmap.computeIfAbsent(rv.name(), k -> Formula.RVAR + "VAR" + (idxs[1]++));
                yield new Expr.RowVar(nv);
            }
            case Expr.Atom a when replaceSkolemTerms && Formula.isSkolemTerm(a.name()) -> {
                String nv = vmap.computeIfAbsent(a.name(), k -> Formula.VVAR + (idxs[0]++));
                yield new Expr.Var(nv);
            }
            case Expr.SExpr se when replaceSkolemTerms
                    && se.head() instanceof Expr.Atom ha
                    && Formula.isSkolemTerm(ha.name()) -> {
                // Skolem function term: treat the whole term as a variable
                String key = se.toKifString();
                String nv  = vmap.computeIfAbsent(key, k -> Formula.VVAR + (idxs[0]++));
                yield new Expr.Var(nv);
            }
            case Expr.SExpr se -> {
                Expr newHead = se.head() != null
                        ? normalizeVariables_1(se.head(), idxs, vmap, replaceSkolemTerms)
                        : null;
                List<Expr> newArgs = se.args().stream()
                        .map(a -> normalizeVariables_1(a, idxs, vmap, replaceSkolemTerms))
                        .toList();
                yield new Expr.SExpr(newHead, newArgs);
            }
            default -> expr;
        };
    }
}
