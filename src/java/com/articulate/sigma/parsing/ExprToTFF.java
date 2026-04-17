package com.articulate.sigma.parsing;

import com.articulate.sigma.Formula;
import com.articulate.sigma.FormulaPreprocessor;
import com.articulate.sigma.KB;
import com.articulate.sigma.KButilities;
import com.articulate.sigma.trans.SUMOKBtoTFAKB;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.articulate.sigma.trans.SUMOtoTFAform;
import com.articulate.sigma.utils.StringUtil;

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
 * <h3>Arithmetic handling</h3>
 * <p>Formulas containing numeric literals or numeric-typed signatures are handled
 * by {@link #translateArithmetic}, which runs an Expr-tree preprocessing pipeline
 * mirroring the string-based preprocessing in
 * {@link com.articulate.sigma.trans.SUMOtoTFAform#processExpr}.</p>
 *
 * <h3>Fallback</h3>
 * <p>Callers should fall back to
 * {@link com.articulate.sigma.trans.SUMOtoTFAform#process(Formula, boolean)}
 * when {@link #translate} returns {@code null} (parse failure or unsupported
 * construct).</p>
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

    /***************************************************************
     * Translate an {@link Expr} tree to a TFF formula string.
     *
     * <p>Variable sort annotations are inferred from {@code (instance ?X Type)}
     * patterns within the formula.  Variables not constrained by such patterns
     * receive the default sort {@code $i}.</p>
     *
     * <p>Formulas containing numeric literals or numeric-typed signatures are
     * routed to {@link #translateArithmetic} which runs the full preprocessing
     * pipeline.  When {@code kb} is {@code null} and numeric literals are present,
     * returns {@code null} (caller must fall back to the string-based pipeline).</p>
     *
     * @param expr  the formula tree (must not be null)
     * @param query {@code true} for query mode (free vars get {@code ?})
     * @param kb    knowledge base used to resolve numeric subclass sorts;
     *              may be {@code null} (falls back to {@code $i})
     * @return TFF formula string, or {@code null} if not handled here
     */
    public static String translate(Expr expr, boolean query, KB kb) {
        if (expr == null) return "";
        // Route arithmetic formulas through the full preprocessing pipeline.
        // Also route formulas containing Pi or NumberE atoms — instantiateNumericConstants
        // must run (in the arithmetic path) to drop e.g. (instance Pi RealNumber).
        if (containsNumericLiteral(expr) || containsPiOrNumberEAtom(expr)) {
            if (kb != null) return translateArithmetic(expr, query, kb);
            return null;  // can't do arithmetic sort handling without KB
        }
        if (kb != null && containsNumericSignature(expr, kb))
            return translateArithmetic(expr, query, kb);

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
    // Arithmetic / numeric handling — Expr-based preprocessing pipeline
    // -----------------------------------------------------------------------

    /***************************************************************
     * Translate an Expr containing numeric content (literals or numeric-typed
     * signatures) to a TFF string.
     *
     * <p>Mirrors the preprocessing pipeline of
     * {@link SUMOtoTFAform#processExpr(Expr, boolean)} but operates entirely on
     * {@link Expr} trees rather than KIF strings.  Steps:</p>
     * <ol>
     *   <li>Substitute {@code Pi} / {@code NumberE} with their decimal values</li>
     *   <li>Remove {@code (instance ?X Integer)} / {@code (instance ?X RealNumber)}</li>
     *   <li>Replace sub-numeric instance constraints with their definitions</li>
     *   <li>Eliminate degenerate logical operators (fixed-point)</li>
     *   <li>Build variable type map from type restrictions</li>
     *   <li>Reject formulas with inconsistent variable types</li>
     *   <li>Iterative function-variable type propagation</li>
     *   <li>Remove remaining numeric instance assertions</li>
     *   <li>Final degenerate-logop cleanup</li>
     *   <li>Translate via {@link SUMOtoTFAform#processRecurseExpr}</li>
     *   <li>Wrap result with typed quantifier if free variables exist</li>
     * </ol>
     *
     * @param expr  the formula tree (must not be null)
     * @param query {@code true} for query mode (free vars get {@code ?})
     * @param kb    knowledge base (must not be null)
     * @return TFF string, {@code ""} for vacuous formulas, {@code null} on error
     */
    public static String translateArithmetic(Expr expr, boolean query, KB kb) {
        if (expr == null || kb == null) return null;

        // Ensure SUMOtoTFAform static state is initialised
        SUMOtoTFAform.kb = kb;
        SUMOtoTFAform.initOnce();
        SUMOformulaToTPTPformula.setHideNumbers(false);

        // 1. Substitute Pi / NumberE with numeric literals
        Expr e = instantiateNumericConstantsExpr(expr);
        if (e == null) return "";

        // 2. Remove (instance ?X Integer) / (instance ?X RealNumber) globally
        e = modifyPrecondExpr(e);
        if (e == null) return "";

        // 3. Replace sub-numeric instance constraints with their definitions (fixed-point)
        e = modifyTypesToConstraintsExpr(e);
        if (e == null) return "";

        // 4. Eliminate degenerate logical operators (fixed-point, up to 5 iterations)
        int counter = 0;
        Expr prev = null;
        while (!e.equals(prev) && counter < 5) {
            counter++;
            prev = e;
            Expr next = elimUnitaryLogopsExpr(e);
            if (next == null) return "";
            e = next;
        }

        // 5. Build variable type map from type restrictions
        FormulaPreprocessor fprep = new FormulaPreprocessor();
        Map<String, Set<String>> varmap = fprep.findTypeRestrictionsExpr(e, kb);

        // 6. Reject formulas with inconsistent variable types
        if (inconsistentVarTypesExpr(varmap, kb)) {
            if (debug) System.err.println("ExprToTFF.translateArithmetic(): " +
                    "rejected inconsistent variable types: " + varmap + " in: " + e.toKifString());
            return "";
        }

        // 7. Annotate operator names with context-derived type suffixes and propagate
        //    variable types (mirrors constrainFunctVars / constrainFunctVarsRecurse /
        //    constrainOp).  Fixed-point loop: repeat until the annotated tree stabilises.
        counter = 0;
        Map<String, Set<String>> oldmap;
        do {
            counter++;
            oldmap = cloneVarmap(varmap);
            e = constrainFunctVarsExpr(e, "Entity", varmap, kb);
            if (e == null) return "";
            Map<String, Set<String>> newTypes = fprep.findTypeRestrictionsExpr(e, kb);
            mergeVarmapConstraint(varmap, newTypes, kb);
        } while (!varmap.equals(oldmap) && counter < 5);

        // 8. Remove remaining numeric instance assertions using the updated varmap
        e = removeNumericInstanceExpr(e, varmap);
        if (e == null) return "";

        // 9. Final degenerate-logop cleanup
        Expr cleaned = elimUnitaryLogopsExpr(e);
        if (cleaned == null) return "";
        e = cleaned;

        if (!(e instanceof Expr.SExpr)) return "";

        // 10. Seed any variable not yet in varmap with "Entity" to prevent
        //     bestSpecificTerm(emptySet) → null → bestOfPair(null,…) NPE.
        seedMissingVarsToVarmap(e, varmap);

        // 11. Translate body via processRecurseExpr (uses getVarmap() ThreadLocal)
        SUMOtoTFAform.setVarmap(varmap);
        String body = SUMOtoTFAform.processRecurseExpr(e, "Entity");
        if (body == null || body.isBlank()) return "";

        // 12. Wrap with typed quantifier if there are free variables
        return buildQuantifiedResultArith(body, query, varmap,
                ExprToTPTP.collectFreeVars(e), kb);
    }

    /***************************************************************
     * Substitute numeric constants with their decimal values.
     *
     * <p>Mirrors {@link SUMOtoTFAform#instantiateNumericConstants(Formula)}:
     * if the top-level formula is {@code (instance NumberE ...)} or
     * {@code (instance Pi ...)}, returns {@code null} (the formula is dropped).
     * Otherwise replaces every occurrence of {@code NumberE} / {@code Pi} with
     * their decimal representations.</p>
     *
     * @return transformed Expr, or {@code null} if the formula should be dropped
     */
    public static Expr instantiateNumericConstantsExpr(Expr expr) {
        if (expr == null) return null;
        // If top-level is (instance NumberE/Pi ...), drop the formula
        if (expr instanceof Expr.SExpr se && "instance".equals(se.headName())) {
            Set<String> atoms = collectAtomNamesExpr(expr);
            if (atoms.contains("NumberE") || atoms.contains("Pi")) return null;
            return expr;  // instance formula without the constants: unchanged
        }
        // For all other formulas: replace Atom("NumberE"/"Pi") with NumLiteral
        Map<String, String> constants = Map.of("NumberE", "2.718282", "Pi", "3.141592653589793");
        return replaceAtomWithNumLiteralExpr(expr, constants);
    }

    /***************************************************************
     * Remove all {@code (instance ?X Integer)} and {@code (instance ?X RealNumber)}
     * occurrences from the formula tree.
     *
     * <p>Mirrors the regex-based removal in
     * {@link SUMOtoTFAform#modifyPrecond(Formula)}.  Removed nodes create
     * unary logical operators that are cleaned up by
     * {@link #elimUnitaryLogopsExpr(Expr)}.</p>
     *
     * @return transformed Expr (may contain unary logical ops), or the original if no match
     */
    public static Expr modifyPrecondExpr(Expr expr) {
        if (expr == null) return null;
        return removeInstanceOfExpr(expr, Set.of("Integer", "RealNumber"));
    }

    /***************************************************************
     * Replace {@code (instance ?X SubType)} where {@code SubType} is a numeric
     * sub-type that has a constraint in {@link SUMOtoTFAform#numericConstraints}
     * with the corresponding constraint expression.
     *
     * <p>Mirrors {@link SUMOtoTFAform#modifyTypesToConstraints(Formula)} using a
     * fixed-point iteration (up to 10 passes).</p>
     *
     * @return transformed Expr, or the original if no substitutable constraints found
     */
    public static Expr modifyTypesToConstraintsExpr(Expr expr) {
        if (expr == null) return null;
        Expr prev = null;
        int counter = 0;
        while (counter < 10) {
            counter++;
            prev = expr;
            expr = replaceNumericConstraintPass(expr);
            if (expr == null) return null;
            if (expr.equals(prev)) break;
        }
        return expr;
    }

    /***************************************************************
     * Eliminate degenerate logical and quantifier operators:
     * <ul>
     *   <li>{@code (op)} with no arguments → removed (returns {@code null})</li>
     *   <li>{@code (op singleArg)} with one argument → replaced by {@code singleArg}</li>
     * </ul>
     *
     * <p>Mirrors {@link SUMOtoTFAform#elimUnitaryLogops(Formula)}.  Applied
     * recursively; re-applies when the tree changes.</p>
     *
     * @return simplified Expr, or {@code null} if the whole formula should be dropped
     */
    public static Expr elimUnitaryLogopsExpr(Expr expr) {
        if (expr == null) return null;
        if (!(expr instanceof Expr.SExpr se)) return expr;
        String head = se.headName();
        boolean isLogOrQuant = head != null && (
                "and".equals(head) || "or".equals(head) || "xor".equals(head) ||
                "=>".equals(head)  || "<=>".equals(head) ||
                "forall".equals(head) || "exists".equals(head));
        if (isLogOrQuant) {
            if (se.args().isEmpty()) return null;           // (op) → drop
            if (se.args().size() == 1) {
                // (op singleArg) → recurse into the single arg
                return elimUnitaryLogopsExpr(se.args().get(0));
            }
        }
        // Recurse into all children, dropping nulls
        Expr newHead = se.head() != null ? elimUnitaryLogopsExpr(se.head()) : null;
        List<Expr> newArgs = new ArrayList<>();
        for (Expr a : se.args()) {
            Expr r = elimUnitaryLogopsExpr(a);
            if (r != null) newArgs.add(r);
        }
        Expr result = new Expr.SExpr(newHead, newArgs);
        // If the tree changed, recurse once more to catch newly-created unary ops
        if (!result.equals(expr)) return elimUnitaryLogopsExpr(result);
        return result;
    }

    /***************************************************************
     * Annotate function/predicate operator names with context-derived type suffixes
     * and propagate variable types into {@code varmap}.
     *
     * <p>Mirrors {@link SUMOtoTFAform#constrainFunctVarsRecurse} /
     * {@link SUMOtoTFAform#constrainOp} but operates entirely on {@link Expr} trees.
     * The key purpose is to rewrite operator atoms so that, e.g.,
     * {@code (DivisionFn 10 2)} in context {@code (equal … 5)} becomes
     * {@code (DivisionFn__0In1In2InFn 10 2)}.  The type-annotated name is then read
     * back by {@code relationExtractSigFromName} during the recursive TFF translation,
     * giving the correct return-type encoding in the output.</p>
     *
     * <p>Steps for each compound node:</p>
     * <ol>
     *   <li>Collect the declared KB signature for the operator.</li>
     *   <li>Refine with any type already encoded in the operator name
     *       (from a previous iteration) via {@code relationExtractSigFromName}.</li>
     *   <li>Refine further with the actual argument types.</li>
     *   <li>For equal-type operators: unify all positions to the most specific type.</li>
     *   <li>For function operators: overwrite position 0 (return type) with the
     *       most specific of the declared return type and {@code parentType}.</li>
     *   <li>If the refined signature differs from the KB signature, rename the
     *       operator atom with the type suffix via {@code makePredFromArgTypes}.</li>
     *   <li>Recurse into each child with the refined type for its position.</li>
     * </ol>
     *
     * @param expr       the Expr subtree to annotate
     * @param parentType the type expected by the enclosing context
     * @param varmap     mutable variable-type map (updated as a side-effect)
     * @param kb         knowledge base
     * @return a new Expr tree with operator atoms type-annotated
     */
    public static Expr constrainFunctVarsExpr(Expr expr, String parentType,
                                               Map<String, Set<String>> varmap, KB kb) {
        if (expr == null) return null;

        // Variable: record the context type in varmap
        if (expr instanceof Expr.Var v) {
            varmap.computeIfAbsent(v.name(), k -> new HashSet<>()).add(parentType);
            return expr;
        }

        // Non-compound leaves (NumLiteral, StrLiteral, RowVar, Atom): return as-is
        if (!(expr instanceof Expr.SExpr se)) return expr;

        String op = se.headName();
        if (op == null) return expr;

        // Logical operators: recurse into each arg using the operator's KB signature
        // (or Entity when no signature is available) — mirrors the logic-branch of
        // constrainFunctVarsRecurse.
        if (Formula.isLogicalOperator(op)) {
            List<String> sig = kb.kbCache.getSignature(op);
            List<Expr> newArgs = new ArrayList<>();
            List<Expr> seArgs = se.args();
            for (int i = 0; i < seArgs.size(); i++) {
                String argType = (sig != null && i + 1 < sig.size()) ? sig.get(i + 1) : "Entity";
                newArgs.add(constrainFunctVarsExpr(seArgs.get(i), argType, varmap, kb));
            }
            return new Expr.SExpr(se.head(), newArgs);
        }

        // Non-logical compound: determine the best signature and annotate the operator
        return constrainOpExpr(se, op, parentType, varmap, kb);
    }

    /***************************************************************
     * Core of {@link #constrainFunctVarsExpr}: determines the refined type
     * signature for {@code se} (operator {@code op}, context {@code parentType})
     * and returns a new {@link Expr.SExpr} whose head atom carries the
     * type-annotated operator name and whose children are recursively annotated.
     *
     * <p>Mirrors {@link SUMOtoTFAform#constrainOp}.</p>
     */
    private static Expr constrainOpExpr(Expr.SExpr se, String op, String parentType,
                                         Map<String, Set<String>> varmap, KB kb) {
        // ---- Step 1-2: declared KB signature, refined with any name encoding ----
        List<String> sig        = kb.kbCache.getSignature(op);
        List<String> typeFromName = SUMOtoTFAform.relationExtractSigFromName(op);
        List<String> newsig     = SUMOtoTFAform.mostSpecificSignature(sig, typeFromName);

        // ---- Step 3: collect actual-arg types ----
        // Position 0 = type of the operator token itself (mirrors collectArgTypes[0]).
        // Functions are relations in SUMO, so kb.isRelation returns true → "Entity".
        List<String> argtypes = new ArrayList<>();
        if (kb.kbCache.isInstance(op)) {
            argtypes.add(kb.isRelation(op) ? "Entity" : "");
        } else {
            argtypes.add("");
        }
        for (Expr arg : se.args())
            argtypes.add(argTypeOf(arg, varmap, kb));

        newsig = SUMOtoTFAform.mostSpecificSignature(newsig, argtypes);

        // ---- Step 4: for equal-type ops, unify all positions ----
        if (SUMOtoTFAform.isEqualTypeOp(op)) {
            String mostSpecific = SUMOtoTFAform.mostSpecificType(newsig);
            if (!mostSpecific.isEmpty())
                newsig.replaceAll(t -> SUMOtoTFAform.constrainTerm(mostSpecific, t.isEmpty() ? mostSpecific : t));
        }

        // ---- Step 5: for functions, push parentType into return-type position ----
        if (kb.isFunction(op) && !newsig.isEmpty()) {
            String opType = (sig != null && !sig.isEmpty() && !sig.get(0).isEmpty()) ? sig.get(0) : "Entity";
            if ("Entity".equals(opType))
                newsig.set(0, SUMOtoTFAform.constrainTerm(opType, newsig.get(0).isEmpty() ? opType : newsig.get(0)));
            newsig.set(0, SUMOtoTFAform.constrainTerm(parentType, newsig.get(0).isEmpty() ? parentType : newsig.get(0)));
        }

        // ---- Step 6: rename operator if signature differs from KB declaration ----
        String baseOp = SUMOtoTFAform.withoutSuffix(op);
        String newOp = op;
        if (!SUMOtoTFAform.equalTFFsig(newsig, sig, op)
                || KButilities.isVariableArity(kb, baseOp)
                || SUMOtoTFAform.needsForcedTypeSuffix(op)) {
            String candidate = SUMOtoTFAform.makePredFromArgTypes(new Formula(baseOp), newsig);
            // makePredFromArgTypes returns baseOp unchanged when no suffix is needed
            if (!candidate.equals(baseOp)) newOp = candidate;
        }

        // ---- Step 7: recurse into children with their refined types ----
        List<Expr> newArgs = new ArrayList<>();
        List<Expr> seArgs = se.args();
        for (int i = 0; i < seArgs.size(); i++) {
            String argType = (i + 1 < newsig.size()) ? newsig.get(i + 1) : "Entity";
            if (argType == null || argType.isEmpty()) argType = "Entity";
            newArgs.add(constrainFunctVarsExpr(seArgs.get(i), argType, varmap, kb));
        }

        return new Expr.SExpr(new Expr.Atom(newOp), newArgs);
    }

    /***************************************************************
     * Returns the SUMO type of a single {@link Expr} node, using {@code varmap}
     * to resolve variable types.
     *
     * <p>Mirrors {@link SUMOtoTFAform#findTypeExpr(Expr)} (private there) but
     * takes explicit {@code varmap} and {@code kb} parameters.</p>
     */
    static String argTypeOf(Expr expr, Map<String, Set<String>> varmap, KB kb) {
        if (expr instanceof Expr.NumLiteral n) {
            if (StringUtil.isInteger(n.value())) return "Integer";
            if (StringUtil.isNumeric(n.value()))  return "RealNumber";
            return "Entity";
        }
        if (expr instanceof Expr.Var v) {
            Set<String> types = varmap.get(v.name());
            return types != null && !types.isEmpty()
                    ? SUMOtoTFAform.bestSpecificTerm(types) : "Entity";
        }
        if (expr instanceof Expr.Atom a) {
            // Mirrors collectArgTypes branch for bare-atom arguments.
            // In SUMO all functions are also relations, so isRelation must be
            // checked first; otherwise a function used as a term (e.g. MohsScaleFn
            // as the first arg of domainSubclass) would incorrectly return its
            // range type instead of "Entity", causing a spurious type-suffix.
            if (kb.kbCache.isInstance(a.name())) {
                if (kb.isRelation(a.name()))
                    return "Entity";
                if (kb.isFunction(a.name())) {
                    String range = kb.kbCache.getRange(a.name());
                    return range != null && !range.isEmpty() ? range : "Entity";
                }
                // Non-relation, non-function instance: use most specific parent type
                Set<String> parents = kb.immediateParents(a.name());
                String most = kb.mostSpecificTerm(parents);
                return most != null && !most.isEmpty() ? most : "Entity";
            }
            // Not a KB instance: return "" to match collectArgTypes else-branch
            return "";
        }
        if (expr instanceof Expr.SExpr se2 && se2.headName() != null) {
            // Function application: return type is the function's range
            if (kb.isFunction(se2.headName())) {
                String range = kb.kbCache.getRange(se2.headName());
                return range != null && !range.isEmpty() ? range : "Entity";
            }
        }
        return "Entity";
    }

    /***************************************************************
     * Check whether the given variable type map contains mutually exclusive types.
     *
     * <p>Mirrors {@link SUMOtoTFAform#inconsistentVarTypes()} but takes an explicit
     * varmap instead of reading the ThreadLocal.</p>
     *
     * @param varmap map of variable name → set of asserted types
     * @param kb     knowledge base for disjointness checks
     * @return {@code true} if any variable has two disjoint types
     */
    public static boolean inconsistentVarTypesExpr(Map<String, Set<String>> varmap, KB kb) {
        if (varmap == null || kb == null) return false;
        for (Map.Entry<String, Set<String>> entry : varmap.entrySet()) {
            Set<String> types = entry.getValue();
            if (types == null) continue;
            String[] typeArr = types.toArray(new String[0]);
            for (int i = 0; i < typeArr.length; i++) {
                for (int j = i + 1; j < typeArr.length; j++) {
                    // Strip the "+" suffix before comparison.
                    // "+" is SUMO's subclass-constraint marker (from domainSubclass):
                    //   "Entity"  = variable must be an instance of Entity
                    //   "Entity+" = variable must be a subclass of Entity
                    // These are NOT logically disjoint; mixing them is a quantifier
                    // annotation concern handled elsewhere, not an inconsistency.
                    // checkDisjoint returns true (and logs a spurious error) when one
                    // type has "+" and the other doesn't, so we compare bare type names.
                    String b1 = typeArr[i].endsWith("+")
                            ? typeArr[i].substring(0, typeArr[i].length() - 1) : typeArr[i];
                    String b2 = typeArr[j].endsWith("+")
                            ? typeArr[j].substring(0, typeArr[j].length() - 1) : typeArr[j];
                    if (!b1.equals(b2) && kb.kbCache != null
                            && kb.kbCache.checkDisjoint(kb, b1, b2)) {
                        if (debug) System.err.println("ExprToTFF.inconsistentVarTypesExpr(): " +
                                "disjoint types " + typeArr[i] + ", " + typeArr[j] +
                                " for var " + entry.getKey());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /***************************************************************
     * Remove numeric {@code (instance ...)} assertions from the formula tree,
     * recording the numeric types found into {@code varmap} as a side-effect.
     *
     * <p>Mirrors {@link SUMOtoTFAform#removeNumericInstance(String)}.  For each
     * {@code (instance ?X Type)} node encountered:
     * <ul>
     *   <li>If {@code Type} is {@code RealNumber}/{@code RationalNumber}/{@code Integer}:
     *       add to varmap and remove the node.</li>
     *   <li>If the variable already has a numeric type in varmap: remove.</li>
     *   <li>If {@code Type} is a numeric sub-type with a known constraint:
     *       replace with the constraint expression.</li>
     * </ul></p>
     *
     * @param expr   formula tree to process
     * @param varmap mutable variable-type map (updated as a side-effect)
     * @return transformed Expr, or {@code null} if the node should be dropped
     */
    public static Expr removeNumericInstanceExpr(Expr expr, Map<String, Set<String>> varmap) {
        if (expr == null) return null;
        if (!(expr instanceof Expr.SExpr se)) return expr;
        String head = se.headName();

        if ("instance".equals(head) && se.args().size() == 2) {
            Expr arg0 = se.args().get(0);
            Expr arg1 = se.args().get(1);

            // (instance NumberE/Pi ...) → drop
            if (arg0 instanceof Expr.Atom atomArg &&
                    ("NumberE".equals(atomArg.name()) || "Pi".equals(atomArg.name())))
                return null;

            if (arg0 instanceof Expr.Var varArg && arg1 instanceof Expr.Atom typeAtom) {
                String var  = varArg.name();
                String type = typeAtom.name();

                // (instance ?X RealNumber/RationalNumber/Integer) → record type and remove
                if ("RealNumber".equals(type) || "RationalNumber".equals(type) || "Integer".equals(type)) {
                    varmap.computeIfAbsent(var, k -> new HashSet<>()).add(type);
                    return null;
                }

                // If ?X already has a numeric type, drop this instance assertion
                Set<String> existingTypes = varmap.get(var);
                if (existingTypes != null && (existingTypes.contains("RealNumber") ||
                        existingTypes.contains("RationalNumber") || existingTypes.contains("Integer")))
                    return null;

                // (instance ?X SubNumericType) → replace with constraint expression
                if (isBuiltInOrSubNumericType(type)) {
                    String cons   = SUMOtoTFAform.numericConstraints.get(type);
                    String origVar = SUMOtoTFAform.numericVars.get(type);
                    if (cons != null && !cons.isEmpty() && origVar != null) {
                        String newCons = cons.replace(Formula.V_PREF + origVar, var);
                        Expr constraintExpr = parseKifToExpr(newCons);
                        if (constraintExpr != null) return constraintExpr;
                    }
                }
            }
        }

        // Compound formula: recurse, filtering out null children
        Expr newHead = se.head() != null ? removeNumericInstanceExpr(se.head(), varmap) : null;
        List<Expr> newArgs = new ArrayList<>();
        for (Expr a : se.args()) {
            Expr r = removeNumericInstanceExpr(a, varmap);
            if (r != null) newArgs.add(r);
        }
        return new Expr.SExpr(newHead, newArgs);
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

    /***************************************************************
     * Returns {@code true} if {@code expr} contains an {@link Expr.Atom} named
     * {@code "Pi"} or {@code "NumberE"}.
     *
     * <p>Formulas containing these atoms (e.g. {@code (instance Pi RealNumber)})
     * must go through the arithmetic path so that
     * {@link SUMOtoTFAform#instantiateNumericConstants} can drop them or substitute
     * their decimal values.</p>
     */
    private static boolean containsPiOrNumberEAtom(Expr expr) {
        return switch (expr) {
            case Expr.Atom a -> "Pi".equals(a.name()) || "NumberE".equals(a.name());
            case Expr.SExpr se -> {
                if (se.head() != null && containsPiOrNumberEAtom(se.head())) yield true;
                for (Expr a : se.args())
                    if (containsPiOrNumberEAtom(a)) yield true;
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

    // -----------------------------------------------------------------------
    // Arithmetic preprocessing helpers
    // -----------------------------------------------------------------------

    /***************************************************************
     * Walk {@code expr} collecting all {@link Expr.Atom} names into a set.
     */
    static Set<String> collectAtomNamesExpr(Expr expr) {
        Set<String> result = new HashSet<>();
        collectAtomNamesExprHelper(expr, result);
        return result;
    }

    private static void collectAtomNamesExprHelper(Expr expr, Set<String> result) {
        switch (expr) {
            case Expr.Atom a -> result.add(a.name());
            case Expr.SExpr se -> {
                if (se.head() != null) collectAtomNamesExprHelper(se.head(), result);
                for (Expr a : se.args()) collectAtomNamesExprHelper(a, result);
            }
            default -> { /* Var, RowVar, NumLiteral, StrLiteral — no atom name */ }
        }
    }

    /***************************************************************
     * Recursively replace named atoms with numeric literals throughout the tree.
     *
     * @param replacements map from atom name (e.g. {@code "Pi"}) to numeric value string
     * @return new Expr tree with replacements applied
     */
    static Expr replaceAtomWithNumLiteralExpr(Expr expr, Map<String, String> replacements) {
        return switch (expr) {
            case Expr.Atom a -> {
                String repl = replacements.get(a.name());
                yield repl != null ? new Expr.NumLiteral(repl) : a;
            }
            case Expr.SExpr se -> {
                Expr newHead = se.head() != null
                        ? replaceAtomWithNumLiteralExpr(se.head(), replacements) : null;
                List<Expr> newArgs = se.args().stream()
                        .map(a -> replaceAtomWithNumLiteralExpr(a, replacements))
                        .collect(Collectors.toList());
                yield new Expr.SExpr(newHead, newArgs);
            }
            default -> expr;
        };
    }

    /***************************************************************
     * Walk the tree and remove any {@code (instance ?X TypeName)} node where
     * {@code TypeName} is in {@code typesToRemove}.
     *
     * <p>Returns {@code null} for removed nodes; parent callers filter nulls from
     * arg lists, which may produce unary logical operators to be cleaned up by
     * {@link #elimUnitaryLogopsExpr}.</p>
     */
    static Expr removeInstanceOfExpr(Expr expr, Set<String> typesToRemove) {
        if (!(expr instanceof Expr.SExpr se)) return expr;
        String head = se.headName();
        // Check for (instance ?X TypeToRemove)
        if ("instance".equals(head) && se.args().size() == 2) {
            Expr arg0 = se.args().get(0);
            Expr arg1 = se.args().get(1);
            if (arg0 instanceof Expr.Var && arg1 instanceof Expr.Atom a
                    && typesToRemove.contains(a.name()))
                return null;  // remove this node
        }
        // Recurse; filter nulls from args (preserved to create unary logops)
        Expr newHead = se.head() != null ? removeInstanceOfExpr(se.head(), typesToRemove) : null;
        List<Expr> newArgs = new ArrayList<>();
        for (Expr a : se.args()) {
            Expr r = removeInstanceOfExpr(a, typesToRemove);
            if (r != null) newArgs.add(r);
        }
        return new Expr.SExpr(newHead, newArgs);
    }

    /***************************************************************
     * Single pass: replace any {@code (instance ?X SubType)} node where
     * {@code SubType} has an entry in {@link SUMOtoTFAform#numericConstraints}
     * with the corresponding constraint expression (variable-substituted).
     */
    static Expr replaceNumericConstraintPass(Expr expr) {
        if (!(expr instanceof Expr.SExpr se)) return expr;
        String head = se.headName();
        if ("instance".equals(head) && se.args().size() == 2) {
            Expr arg0 = se.args().get(0);
            Expr arg1 = se.args().get(1);
            if (arg0 instanceof Expr.Var v && arg1 instanceof Expr.Atom typeAtom) {
                String type    = typeAtom.name();
                String cons    = SUMOtoTFAform.numericConstraints.get(type);
                String origVar = cons != null ? SUMOtoTFAform.numericVars.get(type) : null;
                if (cons != null && !cons.isEmpty() && origVar != null) {
                    String newCons = cons.replace(Formula.V_PREF + origVar, v.name());
                    Expr constraintExpr = parseKifToExpr(newCons);
                    if (constraintExpr != null) return constraintExpr;
                }
            }
        }
        // Recurse
        Expr newHead = se.head() != null ? replaceNumericConstraintPass(se.head()) : null;
        List<Expr> newArgs = se.args().stream()
                .map(ExprToTFF::replaceNumericConstraintPass)
                .collect(Collectors.toList());
        return new Expr.SExpr(newHead, newArgs);
    }

    /***************************************************************
     * @return {@code true} if {@code type} is a TFF built-in numeric type or
     *         one of the SUMO sub-types tracked in {@link SUMOKBtoTFAKB#iChildren},
     *         {@link SUMOKBtoTFAKB#rChildren}, or {@link SUMOKBtoTFAKB#lChildren}.
     */
    static boolean isBuiltInOrSubNumericType(String type) {
        if (type == null || type.isEmpty()) return false;
        return "Integer".equals(type) || "RealNumber".equals(type) || "RationalNumber".equals(type)
                || SUMOKBtoTFAKB.iChildren.contains(type)
                || SUMOKBtoTFAKB.rChildren.contains(type)
                || SUMOKBtoTFAKB.lChildren.contains(type);
    }

    /***************************************************************
     * Parse a KIF string to an {@link Expr}, returning {@code null} on failure.
     */
    static Expr parseKifToExpr(String kif) {
        if (kif == null || kif.isEmpty()) return null;
        try {
            SuokifVisitor visitor = SuokifVisitor.parseSentence(kif);
            if (visitor.result == null || visitor.result.isEmpty()) return null;
            FormulaAST ast = visitor.result.get(0);
            return ast != null ? ast.expr : null;
        } catch (Exception ex) {
            if (debug) System.err.println("ExprToTFF.parseKifToExpr(): parse error for: " + kif);
            return null;
        }
    }

    /***************************************************************
     * Walk {@code expr} and ensure every {@link Expr.Var} node has an entry in
     * {@code varmap}.  Variables not already mapped receive {@code "Entity"} as
     * their type.
     *
     * <p>This mirrors the side-effect of {@code SUMOtoTFAform.constrainOp()}
     * (line ~1943) which calls
     * {@code MapUtils.addToMap(getVarmap(), variable, parentType)} for every
     * variable encountered during the tree walk.  Without this guarantee,
     * {@code processCompOpExpr} crashes when it calls
     * {@code bestSpecificTerm(getVarmap().get(v))} for a variable absent from the
     * map (returns null) and then passes that null to
     * {@code bestOfPair(null, best)} which immediately NPEs.</p>
     */
    static void seedMissingVarsToVarmap(Expr expr, Map<String, Set<String>> varmap) {
        switch (expr) {
            case Expr.Var v -> {
                // Guard against BOTH absent keys AND existing empty sets.
                // findTypeRestrictionsExpr can add a variable with an empty Set when the
                // domain-type / explicit-type intersection yields no "+" entries.
                // bestSpecificTerm(emptySet) returns null → bestOfPair(null, …) NPEs.
                Set<String> existing = varmap.get(v.name());
                if (existing == null || existing.isEmpty()) {
                    Set<String> seed = new HashSet<>();
                    seed.add("Entity");
                    varmap.put(v.name(), seed);
                }
            }
            case Expr.SExpr se -> {
                if (se.head() != null) seedMissingVarsToVarmap(se.head(), varmap);
                for (Expr a : se.args()) seedMissingVarsToVarmap(a, varmap);
            }
            default -> { /* atoms, literals — no variables */ }
        }
    }

    /***************************************************************
     * Deep-clone a variable-type map.
     */
    static Map<String, Set<String>> cloneVarmap(Map<String, Set<String>> varmap) {
        Map<String, Set<String>> clone = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : varmap.entrySet())
            clone.put(e.getKey(), new HashSet<>(e.getValue()));
        return clone;
    }

    /***************************************************************
     * Merge {@code newTypes} into {@code varmap}, keeping the more specific type
     * for each variable.
     *
     * <p>Mirrors the logic of the private
     * {@code SUMOtoTFAform.constrainTypeRestriction(Map)} method: for each variable,
     * picks the new type if it is a subclass of the existing type; otherwise keeps
     * the existing type.</p>
     */
    static void mergeVarmapConstraint(Map<String, Set<String>> varmap,
                                      Map<String, Set<String>> newTypes, KB kb) {
        for (Map.Entry<String, Set<String>> entry : newTypes.entrySet()) {
            String k = entry.getKey();
            Set<String> newVarTypes = entry.getValue();
            if (newVarTypes == null || newVarTypes.isEmpty()) continue;
            Set<String> oldVarTypes = varmap.get(k);
            if (oldVarTypes == null || oldVarTypes.isEmpty()) {
                // No existing type — take the new types
                varmap.put(k, newVarTypes);
                continue;
            }
            if (kb == null) continue;  // can't compare subclass without KB
            String newt = SUMOtoTFAform.mostSpecificType(newVarTypes);
            String oldt = SUMOtoTFAform.mostSpecificType(oldVarTypes);
            if (newt == null || newt.isEmpty()) {
                // keep existing
            } else if (oldt == null || oldt.isEmpty()) {
                varmap.put(k, newVarTypes);
            } else if (kb.isSubclass(newt, oldt)) {
                varmap.put(k, newVarTypes);
            }
            // else keep existing (oldt is more specific or unrelated — keep conservative)
        }
    }

    /***************************************************************
     * Build the TFF quantifier wrapper for the translated body.
     *
     * <p>Mirrors {@code SUMOtoTFAform.buildQuantifiedResult(Formula, String, boolean)}
     * but takes explicit parameters instead of using a Formula and a ThreadLocal.</p>
     *
     * @param body     already-translated TFF body string
     * @param query    {@code true} for existential (query) mode
     * @param varmap   variable → type-set map built during preprocessing
     * @param freeVars free (unquantified) variables in the preprocessed Expr
     * @param kb       KB for sort name translation
     * @return quantified TFF string, or {@code body} if there are no typed free vars
     */
    static String buildQuantifiedResultArith(String body, boolean query,
                                             Map<String, Set<String>> varmap,
                                             Set<String> freeVars, KB kb) {
        StringBuilder qlist = new StringBuilder();
        for (String var : freeVars) {
            Set<String> types = varmap.get(var);
            if (types == null || types.isEmpty()) continue;
            String t = SUMOtoTFAform.mostSpecificType(types);
            if (t == null || t.isEmpty()) continue;
            String oneVar = SUMOformulaToTPTPformula.translateWord(var, var.charAt(0), false);
            qlist.append(oneVar).append(" : ")
                 .append(SUMOKBtoTFAKB.translateSort(kb, t))
                 .append(",");
        }
        if (qlist.length() > 1) {
            qlist.deleteCharAt(qlist.length() - 1);
            String quant = query ? "?" : "!";
            return quant + " [" + qlist + "] : (" + body + ")";
        }
        return body;
    }
}
