/** This code is copyright Articulate Software (c) 2017.  Some portions
 copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
 This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 Users of this code also consent, by use of this code, to credit Articulate Software
 and Teknowledge in any writings, briefings, publications, presentations, or
 other representations of any software which incorporates, builds on, or uses this
 code.  Please cite the following article in any publication with references:

 Pease, A., (2003). The Sigma Ontology Development Environment,
 in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 August 9, Acapulco, Mexico. See also https://github.com/ontologyportal/sigmakee

 This class expects the following to be in the ontology.
 Their absence won't cause an exception, but will prevent correct behavior.
 VariableArityRelation
 subclass
 instance
 Class

 */
package com.articulate.sigma;

import com.articulate.sigma.parsing.*;
// Note: com.articulate.sigma.parsing.PredVarInst is referenced by fully-qualified name below
// to avoid shadowing com.articulate.sigma.PredVarInst (used by replacePredVarsAndRowVars).
import com.articulate.sigma.utils.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FormulaPreprocessor {

    /******************************************************************
     * For any given formula, stop generating new pred var instantiations
     * and row var expansions if this threshold value has been exceeded.
     * The default value is 2000.
     */
    private static final int AXIOM_EXPANSION_LIMIT = 2000;

    private static final Pattern INSTANCE_TYPE_PATTERN = Pattern.compile("\\(instance (\\?[a-zA-Z0-9\\-_]+) ([\\?a-zA-Z0-9\\-_]+)");
    private static final Pattern SUBCLASS_TYPE_PATTERN  = Pattern.compile("\\(subclass (\\?[a-zA-Z0-9\\-_]+) ([\\?a-zA-Z0-9\\-]+)");

    public static boolean debug = false;

    public static boolean addOnlyNonNumericTypes = false;

    public static Set<String> errors = new HashSet<>();

    /******************************************************************
     * A + is appended to the type if the parameter must be a class
     *
     * @return the type for each argument to the given predicate, where
     * ArrayList element 0 is the result, if a function, 1 is the first
     * argument, 2 is the second etc.
     */
    private List<String> getTypeList(String pred, KB kb) {

        return kb.kbCache.signatures.get(pred);
    }

    /******************************************************************
     * Find the argument type restriction for a given predicate and
     * argument number that is inherited from one of its super-relations.
     * A "+" is appended to the type if the parameter must be a class,
     * meaning that a domainSubclass is defined for this argument in one
     * of the loaded .kif files.  Argument number 0 is used for the return
     * type of a Function.  Asking for a non-existent arg will return null;
     */
    public static String findType(int numarg, String pred, KB kb) {

        List<String> sig = null;
        if (kb == null || kb.kbCache == null) {
            System.err.println("Error in FormulaPreprocessor.findType(): null cache");
            return null;
        }
        else if (kb.kbCache.signatures == null) System.err.println("Error in FormulaPreprocessor.findType(): null cache signatures");
        if (kb.kbCache != null && kb.kbCache.signatures != null) sig = kb.kbCache.signatures.get(pred);
        if (sig == null) {
            if (!kb.isInstanceOf(pred, "VariableArityRelation") && !Formula.isLogicalOperator(pred) &&
                !pred.equals(Formula.EQUAL)) {
                if (debug) LoggingUtils.log("ERROR", "no type information for predicate " + pred);
                if (debug) System.out.println("start of FormulaPreprocessor.findType: " + StringUtil.shorten(kb.kbCache.signatures.toString(),100) + "...");
                List<Formula> ar = kb.askWithRestriction(0,"domain",1,pred);
                if (debug) System.out.println("domains: " + ar);
            }
            return null;
        }
        if (numarg >= sig.size()) return null;
        return sig.get(numarg);
    }

    /******************************************************************
     * This method tries to remove all but the most specific relevant
     * classes from a List of sortal classes.
     * @param types A List of classes (class name Strings) that
     * constrain the value of a SUO-KIF variable.
     * @param kb The KB used to determine if any of the classes in the
     * List types are redundant.
     */
    public void winnowTypeList(Set<String> types, KB kb) {

        long t1 = 0L;
        if (types.size() > 1) {
            Object[] valArr = types.toArray();
            String clX = null;
            String clY = null;
            boolean stop;
            for (int i = 0; i < valArr.length; i++) {
                stop = false;
                for (int j = 0; j < valArr.length; j++) {
                    if (i != j) {
                        clX = (String) valArr[i];
                        clY = (String) valArr[j];
                        if (clX.equals(clY) || kb.isSubclass(clX, clY)) {
                            types.remove(clY);
                            if (types.size() < 2) {
                                stop = true;
                                break;
                            }
                        }
                    }
                }
                if (stop) break;
            }
        }
    }

    /******************************************************************
     */
    public Map<String,Set<String>> findAllTypeRestrictions(Formula form, KB kb) {

        if (debug) System.out.println("findAllTypeRestrictions: form \n" + form);
        Map<String,Set<String>> varDomainTypes = computeVariableTypesExpr(form.expr, kb);
        if (debug) System.out.println("FormulaPreprocessor.findAllTypeRestrictions: varDomainTypes " + varDomainTypes);
        // get variable types which are explicitly defined in formula
        Map<String,Set<String>> varExplicitTypes = findExplicitTypesClassesInAntecedentExpr(form.expr, kb);
        if (debug) System.out.println("FormulaPreprocessor.findAllTypeRestrictions: varExplicitTypes " + varExplicitTypes);
        // only keep variables which are not explicitly defined in formula
        Map<String,Set<String>> varmap = new HashMap<>();
        Set<String> types, domainTypes, explicitTypes;
        for (String var : varDomainTypes.keySet()) {
            types = new HashSet();
            domainTypes = varDomainTypes.get(var);
            explicitTypes = varExplicitTypes.get(var);
            if (domainTypes != null)
                types.addAll(domainTypes);
            if (explicitTypes != null)
                types.addAll(explicitTypes);
            varmap.put(var, types);
        }
        for (String var : varExplicitTypes.keySet()) {
            types = new HashSet();
            domainTypes = varDomainTypes.get(var);
            explicitTypes = varExplicitTypes.get(var);
            if (domainTypes != null)
                types.addAll(domainTypes);
            if (explicitTypes != null)
                types.addAll(explicitTypes);
            varmap.put(var, types);
        }
        if (debug) System.out.println("FormulaPreprocessor.findAllTypeRestrictions: returning: " + varmap);
        return varmap;
    }

    /***************************************************************************
     * Get the most specific type for variables.
     *
     * @param kb The KB to be used for processing
     * @param types a list of sumo types for a sumo term/variable
     * @return the most specific sumo type for the term/variable
     *
     * For example
     * types of ?Writing = [Entity, Physical, Process, IntentionalProcess,
     *                      ContentDevelopment, Writing]
     * return the most specific type Writing
     */
    protected String getMostRelevantType(KB kb, Set<String> types) {

        Set<String> insts = new TreeSet<>();
        for (String type : types) {
            if (!type.endsWith("+")) insts.add(type);
            else insts.add(type.substring(0, type.length()-1));
        }
        if (insts != null) {
            winnowTypeList(insts, kb);
            Iterator<String> it1 = insts.iterator();
            while (it1.hasNext()) return it1.next();
        }
        return null;
    }

    /*****************************************************************
     * Collect the types of any variables that are specifically defined
     * in the antecedent of a rule with an instance or subclass expression.
     * TODO: This may ultimately require CNF conversion and then checking negative
     * literals, but for now it's just a hack to grab preconditions.
     */
    public Map<String, Set<String>> findExplicitTypesInAntecedent(KB kb, Formula form) {

        if (!form.isRule())
            // TODO: Consider returning empty map instead of null. Check callers for special behavior on null.
            return null;

        Formula f = new Formula();
        f.read(form.getFormula());
        Formula antecedent = f.cdrAsFormula().carAsFormula();
        return findExplicitTypes(kb,antecedent);
    }

    /*****************************************************************
     * Collect variable names and their types from instance or subclass
     * expressions. subclass restrictions are marked with a '+'.
     *
     * @param form The formula in KIF syntax
     *
     * @return A map of variables paired with a set of sumo types collected
     * from instance and subclass expressions.
     *
     * TODO: This may ultimately require CNF conversion and then checking
     * negative literals, but for now it's just a hack to grab preconditions.
     */
    public Map<String, Set<String>> findExplicitTypes(KB kb, Formula form) {

        Map<String,Set<String>> varExplicitTypes = new HashMap<>();
        Map<String,Set<String>> varExplicitClasses = new HashMap<>();
        findExplicitTypesRecurseExpr(form.expr, false, varExplicitTypes, varExplicitClasses);
        varExplicitTypes.putAll(varExplicitClasses);
        return varExplicitTypes;
    }

    /******************************************************************
     * Expr analogue of {@link #computeVariableTypes(Formula, KB)}.
     *
     * <p>Walks the Expr tree and collects a {@code variable → types} map
     * by looking up each predicate's argument signature in
     * {@code kb.kbCache.signatures}.  No KIF string is constructed.</p>
     */
    public Map<String, Set<String>> computeVariableTypesExpr(Expr expr, KB kb) {
        return computeVariableTypesRecurseExpr(kb, expr, new HashMap<>());
    }

    private Map<String, Set<String>> computeVariableTypesRecurseExpr(KB kb, Expr expr,
                                                                      Map<String, Set<String>> input) {
        if (!(expr instanceof Expr.SExpr se)) return new HashMap<>();
        String head = se.headName();
        if (head == null) return new HashMap<>(); // var-list node inside a quantifier

        Map<String, Set<String>> result = new HashMap<>();

        if (Formula.isLogicalOperator(head) && !head.equals(Formula.EQUAL)) {
            result.putAll(input);
            int start = 0;
            if (Formula.isQuantifier(head)) start = 1; // skip the variable-list argument
            for (int i = start; i < se.args().size(); i++) {
                result = KButilities.mergeToMap(result,
                        computeVariableTypesRecurseExpr(kb, se.args().get(i), input), kb);
            }
        } else {
            // Predicate application (ground or with variables)
            String pred = head;
            if (!Formula.isVariable(pred)) {
                // Special case: (equal funcExpr ?var) or (equal ?var funcExpr)
                if (pred.equals(Formula.EQUAL) && se.args().size() >= 2) {
                    Expr a0 = se.args().get(0);
                    Expr a1 = se.args().get(1);
                    if (a0 instanceof Expr.Var vv && a1 instanceof Expr.SExpr fse) {
                        String fhead = fse.headName();
                        if (fhead != null && kb.isFunction(fhead)) {
                            String type = kb.kbCache.getRange(fhead);
                            MapUtils.addToMap(result, vv.name(), type != null ? type : "Entity");
                        }
                    }
                    if (a1 instanceof Expr.Var vv && a0 instanceof Expr.SExpr fse) {
                        String fhead = fse.headName();
                        if (fhead != null && kb.isFunction(fhead)) {
                            String type = kb.kbCache.getRange(fhead);
                            MapUtils.addToMap(result, vv.name(), type != null ? type : "Entity");
                        }
                    }
                }
                int argnum = 1;
                for (Expr arg : se.args()) {
                    if (arg instanceof Expr.Var vv) {
                        String cl = findType(argnum, pred, kb);
                        if (!StringUtil.emptyString(cl)) {
                            MapUtils.addToMap(result, vv.name(), cl);
                        }
                    } else if (arg instanceof Expr.SExpr argSe) {
                        String argHead = argSe.headName();
                        if (argHead != null && !Formula.isVariable(argHead) && kb.isFunction(argHead)) {
                            result = KButilities.mergeToMap(result,
                                    computeVariableTypesRecurseExpr(kb, arg, input), kb);
                        }
                        // Formula-position SExpr args (quantifiers, logical ops) are intentionally
                        // not recursed into: bound variables would leak into the outer result and
                        // free variables would acquire wrong types from the inner scope.
                        // Variables that are *directly* in a formula-position already receive type
                        // "Formula" via the Var branch above (findType returns "Formula" for them).
                    }
                    argnum++;
                }
            }
        }
        return result;
    }

    /******************************************************************
     * Expr analogue of {@link #findExplicitTypesClassesInAntecedent(KB, Formula)}.
     *
     * <p>Locates the antecedent of an implication
     * ({@code (=> A B)} → A, {@code (<=> A B)} → A, otherwise the whole
     * formula), then walks it collecting variables that are explicitly
     * typed with {@code (instance ?V T)} or {@code (subclass ?V T)}
     * expressions.</p>
     */
    private Map<String, Set<String>> findExplicitTypesClassesInAntecedentExpr(Expr expr, KB kb) {
        Expr antecedent = findAntecedentExpr(expr);
        Map<String, Set<String>> varExplicitTypes   = new HashMap<>();
        Map<String, Set<String>> varExplicitClasses = new HashMap<>();
        findExplicitTypesRecurseExpr(antecedent, false, varExplicitTypes, varExplicitClasses);
        return varExplicitTypes;
    }

    private static Expr findAntecedentExpr(Expr expr) {
        if (!(expr instanceof Expr.SExpr se)) return expr;
        String head = se.headName();
        if ((Formula.IF.equals(head) || Formula.IFF.equals(head)) && se.args().size() >= 2)
            return se.args().get(0);
        return expr;
    }

    private static void findExplicitTypesRecurseExpr(Expr expr, boolean isNegativeLiteral,
                                                      Map<String, Set<String>> varExplicitTypes,
                                                      Map<String, Set<String>> varExplicitClasses) {
        if (!(expr instanceof Expr.SExpr se)) return;
        String head = se.headName();
        if (head == null) return;

        if (Formula.isLogicalOperator(head)) {
            switch (head) {
                case Formula.UQUANT:
                case Formula.EQUANT:
                    // Skip variable-list; recurse into body only
                    if (se.args().size() == 2)
                        findExplicitTypesRecurseExpr(se.args().get(1), false,
                                varExplicitTypes, varExplicitClasses);
                    break;
                case Formula.NOT:
                    for (Expr arg : se.args())
                        findExplicitTypesRecurseExpr(arg, true,
                                varExplicitTypes, varExplicitClasses);
                    break;
                default:
                    for (Expr arg : se.args())
                        findExplicitTypesRecurseExpr(arg, false,
                                varExplicitTypes, varExplicitClasses);
                    break;
            }
        } else {
            if (isNegativeLiteral) return;
            // Match (instance ?V T) or (subclass ?V T) where V is a Var and T is an Atom
            if ("instance".equals(head) && se.args().size() == 2
                    && se.args().get(0) instanceof Expr.Var vv
                    && se.args().get(1) instanceof Expr.Atom typeAtom
                    && !typeAtom.name().startsWith(Formula.V_PREF)) {
                MapUtils.addToMap(varExplicitTypes, vv.name(), typeAtom.name());
            } else if ("subclass".equals(head) && se.args().size() == 2
                    && se.args().get(0) instanceof Expr.Var vv
                    && se.args().get(1) instanceof Expr.Atom typeAtom
                    && !typeAtom.name().startsWith(Formula.V_PREF)) {
                MapUtils.addToMap(varExplicitClasses, vv.name(), typeAtom.name() + "+");
            } else {
                // Recurse into any other predicate's args (handles function-nested expressions)
                for (Expr arg : se.args())
                    findExplicitTypesRecurseExpr(arg, false, varExplicitTypes, varExplicitClasses);
            }
        }
    }

    /******************************************************************
     * Fully Expr-based replacement for {link #findTypeRestrictions(Formula, KB)}.
     *
     * <p>No KIF string is ever constructed.  Merging logic is identical to
     * the original: domain types are kept for implicitly-typed variables;
     * for explicitly-typed variables only {@code "+"} (subclass) markers
     * from either source are retained.</p>
     *
     * <p>Was package-private; widened to {@code public} so that the
     * {@code trans} package (THFnew) can use it directly on Expr trees
     * without converting back to a Formula string.</p>
     */
    public Map<String, Set<String>> findTypeRestrictionsExpr(Expr expr, KB kb) {
        Map<String, Set<String>> varDomainTypes   = computeVariableTypesExpr(expr, kb);
        Map<String, Set<String>> varExplicitTypes = findExplicitTypesClassesInAntecedentExpr(expr, kb);

        Map<String, Set<String>> varmap = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : varDomainTypes.entrySet()) {
            String var = e.getKey();
            if (!varExplicitTypes.containsKey(var)) {
                varmap.put(var, e.getValue());
            } else {
                // Variable already has an explicit type — keep only subclass ("+") markers
                Set<String> types = new HashSet<>();
                for (String dt : e.getValue())
                    if (dt.endsWith("+")) types.add(dt);
                for (String et : varExplicitTypes.get(var))
                    if (et.endsWith("+")) types.add(et);
                varmap.put(var, types);
            }
        }
        return varmap;
    }

    /******************************************************************
     * Build a single type-guard {@link Expr}: {@code (instance ?V T)} or
     * {@code (subclass ?V T)} for a {@code T+} marker.
     * Returns {@code null} when the type should be skipped (Entity, World,
     * or a numeric type when {@link #addOnlyNonNumericTypes} is set).
     */
    private Expr buildTypeGuardExpr(String varName, String type, KB kb) {
        if (StringUtil.emptyString(type)) return null;
        if (type.endsWith("+")) {
            String t = type.substring(0, type.length() - 1);
            if (t.equals("Entity")) return null;
            return new Expr.SExpr(new Expr.Atom("subclass"),
                    List.of(new Expr.Var(varName), new Expr.Atom(t)));
        } else {
            if (type.equals("Entity") || type.equals("World")
                    || type.equals("ObjectiveNorm")) return null;
            if (addOnlyNonNumericTypes && kb.isSubclass(type, "Quantity")) return null;
            return new Expr.SExpr(new Expr.Atom("instance"),
                    List.of(new Expr.Var(varName), new Expr.Atom(type)));
        }
    }

    /******************************************************************
     * Wrap a non-empty list of guard {@link Expr}s into
     * {@code (and g1 g2 ...)} or return the single element directly
     * when the list has exactly one entry (eliminating a unary {@code and}).
     */
    private static Expr wrapAndExpr(List<Expr> guards) {
        if (guards.size() == 1) return guards.get(0);
        return new Expr.SExpr(new Expr.Atom(Formula.AND), guards);
    }

    /******************************************************************
     * Recursively walk an {@link Expr} tree and inject type guards
     * inside quantifier ({@code forall}/{@code exists}) bodies.
     * This is the Expr-based analogue of
     * {@link #addTypeRestrictionsRecurse(KB, Formula, StringBuilder)}.
     *
     * <ul>
     *   <li>{@code forall (?Z) body} with guard G becomes
     *       {@code forall (?Z) (=> G body)}</li>
     *   <li>{@code exists (?Z) body} with guard G becomes
     *       {@code exists (?Z) (and G body)}</li>
     *   <li>Other logical operators are recursed into transparently.</li>
     *   <li>Simple predicate clauses are returned unchanged.</li>
     * </ul>
     *
     * @param expr    Expr subtree to transform
     * @param varmap  type map for the current lexical scope
     * @param kb      knowledge base (for {@link #buildTypeGuardExpr})
     * @return transformed Expr; may be the same object when nothing changed
     */
    private Expr addTypeRestrictionsRecurseExpr(Expr expr,
                                                 Map<String, Set<String>> varmap,
                                                 KB kb) {
        if (!(expr instanceof Expr.SExpr se)) return expr;
        String head = se.headName();
        if (head == null) return expr; // variable-list node inside a quantifier

        boolean isLogOp = Formula.isLogicalOperator(head);
        boolean isEqual  = head.equals(Formula.EQUAL);
        if (!isLogOp && !isEqual) {
            return expr; // simple clause — return unchanged
        }

        if (head.equals(Formula.EQUANT) || head.equals(Formula.UQUANT)) {
            if (se.args().size() != 2) return expr;
            Expr varListExpr = se.args().get(0);
            Expr bodyExpr    = se.args().get(1);

            // Collect quantified variable names
            List<String> quantVarNames = new ArrayList<>();
            if (varListExpr instanceof Expr.SExpr varSe) {
                for (Expr v : varSe.args()) {
                    if (v instanceof Expr.Var vv)         quantVarNames.add(vv.name());
                    else if (v instanceof Expr.RowVar rv) quantVarNames.add(rv.name());
                }
            }

            // Compute type restrictions scoped to this sub-expression
            Map<String, Set<String>> subVarmap = findTypeRestrictionsExpr(expr, kb);

            // Build type guards for the quantified variables
            List<Expr> guards = new ArrayList<>();
            for (String qv : quantVarNames) {
                Set<String> types = subVarmap.get(qv);
                if (types != null) {
                    for (String t : new TreeSet<>(types)) {
                        Expr g = buildTypeGuardExpr(qv, t, kb);
                        if (g != null) guards.add(g);
                    }
                }
            }

            // Recurse into the body
            Expr newBody = addTypeRestrictionsRecurseExpr(bodyExpr, subVarmap, kb);

            if (guards.isEmpty()) {
                if (newBody == bodyExpr) return expr; // nothing changed
                return new Expr.SExpr(se.head(), List.of(varListExpr, newBody));
            }

            Expr guardedBody;
            if (head.equals(Formula.EQUANT)) {
                // (exists (?Z) (and (instance ?Z T) body))
                List<Expr> andArgs = new ArrayList<>(guards);
                andArgs.add(newBody);
                guardedBody = wrapAndExpr(andArgs);
            } else {
                // (forall (?Z) (=> (and (instance ?Z T)) body))
                Expr antecedent = wrapAndExpr(guards);
                guardedBody = new Expr.SExpr(new Expr.Atom(Formula.IF),
                        List.of(antecedent, newBody));
            }
            return new Expr.SExpr(se.head(), List.of(varListExpr, guardedBody));

        } else {
            // Other logical operators: recurse into all arguments
            boolean changed = false;
            List<Expr> newArgs = new ArrayList<>(se.args().size());
            for (Expr arg : se.args()) {
                Expr newArg = addTypeRestrictionsRecurseExpr(arg, varmap, kb);
                if (newArg != arg) changed = true;
                newArgs.add(newArg);
            }
            return changed ? new Expr.SExpr(se.head(), newArgs) : expr;
        }
    }

    /******************************************************************
     * Expr-based equivalent of {@link #addTypeRestrictions(Formula, KB)}.
     *
     * <p>Wraps the formula with type-guard antecedents for free variables
     * and injects type guards inside quantifier bodies via
     * {@link #addTypeRestrictionsRecurseExpr}.  Returns the original
     * {@code expr} reference unchanged when no type restrictions apply,
     * allowing the caller to detect this with reference equality.</p>
     *
     * <p>This is intentionally package-private for unit testing.</p>
     *
     * @param expr   the Expr tree (after variable-arity renaming)
     * @param varmap type map from {@link #findTypeRestrictionsExpr}
     * @param kb     knowledge base
     * @return type-guarded Expr, or {@code expr} itself if nothing changed
     */
    Expr addTypeRestrictionsExpr(Expr expr, Map<String, Set<String>> varmap, KB kb) {
        // Collect free (unquantified) variables — sort for deterministic output
        Set<String> freeVars = new TreeSet<>(ExprToTPTP.collectFreeVars(expr));

        // Build guards for free variables
        List<Expr> freeGuards = new ArrayList<>();
        for (String fv : freeVars) {
            Set<String> types = varmap.get(fv);
            if (types != null) {
                for (String t : new TreeSet<>(types)) {
                    Expr g = buildTypeGuardExpr(fv, t, kb);
                    if (g != null) freeGuards.add(g);
                }
            }
        }

        // Recursively inject guards into quantifier bodies
        Expr body = addTypeRestrictionsRecurseExpr(expr, varmap, kb);

        // Wrap with free-variable guards (if any)
        if (freeGuards.isEmpty()) {
            return body; // same reference as expr when nothing changed inside
        }
        Expr antecedent = wrapAndExpr(freeGuards);
        return new Expr.SExpr(new Expr.Atom(Formula.IF), List.of(antecedent, body));
    }

    /******************************************************************
     * Recursively rename any VariableArityRelation predicates in an
     * {@link Expr} tree by appending the arity suffix {@code __N}
     * (or {@code __NFn} for function symbols). This mirrors the
     * renaming done in {link #preProcessRecurse} and
     * {@link Formula#renameVariableArityRelations}, but operates
     * directly on the Expr AST without string reconstruction.
     */
    private Expr renameVariableArityInExpr(Expr expr, KB kb) {
        if (!(expr instanceof Expr.SExpr se)) return expr;
        // Recurse into all arguments first
        List<Expr> renamedArgs = se.args().stream()
                .map(a -> renameVariableArityInExpr(a, kb))
                .collect(Collectors.toList());
        Expr head = se.head();
        if (head instanceof Expr.Atom headAtom) {
            String pred = headAtom.name();
            if (kb.kbCache.transInstOf(pred, "VariableArityRelation")) {
                int arity = renamedArgs.size();
                String func = kb.kbCache.isInstanceOf(pred, "Function") ? Formula.FN_SUFF : "";
                String suffix = "__" + arity + func;
                if (!pred.endsWith(suffix)) {
                    kb.kbCache.copyNewPredFromVariableArity(pred + suffix, pred, arity);
                    head = new Expr.Atom(pred + suffix);
                }
            }
        }
        return new Expr.SExpr(head, renamedArgs);
    }

    /******************************************************************
     * Expr-based preprocessing: extended fast path that handles predicate
     * variables (Phase A), row variables (Phase B), and type restrictions
     * (Phase C) entirely within the Expr AST — no string round-trips.
     *
     * <p>Phase A — Predicate variable expansion:
     * If {@code fa.predVarCache} is non-empty,
     * {@link com.articulate.sigma.parsing.PredVarInst#processOne}
     * instantiates every predicate variable at the Expr level via
     * {@link com.articulate.sigma.parsing.PredVarInst#substituteVar}.
     * The result is a set of FormulaASTs, each with a concrete relation
     * in place of each pred-var.
     *
     * <p>Phase B — Row variable expansion:
     * For each FormulaAST from Phase A that has row variables,
     * {@link RowVar#expandRowVarExpr} splices the expanded argument lists
     * directly into the Expr tree.
     *
     * <p>Phase C — Variable-arity renaming + type restrictions:
     * Unchanged from the original no-var fast path.
     *
     * @param fa      the FormulaAST (must have a non-null {@code expr})
     * @param isQuery {@code true} for query mode
     * @param kb      the knowledge base
     * @return set of preprocessed Expr trees ready for
     *         {@link com.articulate.sigma.parsing.ExprToTPTP#translate};
     *         empty if pred-var instantiation yields no results
     */
    public Set<Expr> preProcessExpr(Formula fa, boolean isQuery, KB kb) {

        if (fa == null || fa.expr == null) return Set.of();
        // Phase A: predicate-variable expansion
        Collection<Formula> afterPredVar;
        if (fa.predVarCache != null && !fa.predVarCache.isEmpty()) {
            Set<Formula> pviResult = PredVarInst.instantiatePredVars(fa, kb);
            if (pviResult == null) return Set.of();
            afterPredVar = pviResult.isEmpty() ? List.of(fa) : pviResult;
        }
        else {
            afterPredVar = List.of(fa);
        }
        // Phase B: row-variable expansion
        Set<Expr> afterRowVar = new TreeSet<>(Comparator.comparing(Expr::toKifString));
        RowVar rv = new RowVar(kb);
        for (Formula fa2 : afterPredVar) {
            if (fa2.expr == null) continue;
            boolean hasRows = fa2.rowVarCache != null && !fa2.rowVarCache.isEmpty();
            if (hasRows) {
                Set<Expr> expanded = rv.expandRowVarExpr(fa2);
                if (expanded == null || expanded.isEmpty()) {
                    afterRowVar.add(fa2.expr);
                }
                else {
                    afterRowVar.addAll(expanded);
                }
                if (afterRowVar.size() > AXIOM_EXPANSION_LIMIT) {
                    System.err.println(
                            "Error in FormulaPreprocessor.preProcessExpr(): " +
                            "AXIOM_EXPANSION_LIMIT EXCEEDED: " +
                            AXIOM_EXPANSION_LIMIT);
                    break;
                }
            }
            else {
                afterRowVar.add(fa2.expr);
            }
        }
        if (afterRowVar.isEmpty()) return Set.of();
        // Phase C: Variable-arity renaming + type restrictions (unchanged from no-var fast path)
        // LinkedHashSet preserves the sorted order established by afterRowVar's TreeSet.
        Set<Expr> results = new LinkedHashSet<>();
        KBmanager mgr = KBmanager.getMgr();
        boolean typePrefix = mgr.getPref("typePrefix").equalsIgnoreCase("yes");
        for (Expr e : afterRowVar) {
            Expr renamed = renameVariableArityInExpr(e, kb);
            if (typePrefix && !isQuery) {
                Map<String, Set<String>> varmap = findTypeRestrictionsExpr(renamed, kb);
                results.add(addTypeRestrictionsExpr(renamed, varmap, kb));
            } 
            else {
                results.add(renamed);
            }
        }
        return results;
    }

    /******************************************************************
     * Bridge overload: wraps a bare {@link Expr} in a minimal
     * {@link Formula} (no pred-var / row-var caches) and delegates to
     * {@link #preProcessExpr(Formula, boolean, KB)}.  Keeps existing
     * callers and tests working without change.
     *
     * @param expr    the Expr tree from {@link Formula#expr}
     * @param isQuery {@code true} for query mode
     * @param kb      the knowledge base
     * @return set of preprocessed Expr trees ready for
     *         {@link com.articulate.sigma.parsing.ExprToTPTP#translate}
     */
    public Set<Expr> preProcessExpr(Expr expr, boolean isQuery, KB kb) {
        return preProcessExpr(new Formula(expr), isQuery, kb);
    }

    /******************************************************************
     */
    public static void testFindTypes() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        System.out.println();
        String strf = "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) " +
                "(element ?ELEMENT ?SET2))) (equal ?SET1 ?SET2))";
        Formula f = new Formula();
        f.read(strf);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        System.out.println("Formula: " + f);
        System.out.println("Var types: " + fp.computeVariableTypesExpr(f.expr,kb));

        System.out.println();
        strf = "(=> (and (attribute ?AREA LowTerrain) (part ?ZONE ?AREA)" +
                " (slopeGradient ?ZONE ?SLOPE)) (greaterThan 0.03 ?SLOPE))";
        f.read(strf);
        fp = new FormulaPreprocessor();
        System.out.println("Formula: " + f);
        System.out.println("Var types: " + fp.computeVariableTypesExpr(f.expr,kb));

        System.out.println();
        strf = "(=> (and (typicalPart ?PART ?WHOLE) (instance ?X ?PART) " +
                "(equal ?PARTPROB (ProbabilityFn (exists (?Y) (and " +
                "(instance ?Y ?WHOLE) (part ?X ?Y))))) (equal ?NOTPARTPROB " +
                "(ProbabilityFn (not (exists (?Z) (and (instance ?Z ?WHOLE) " +
                "(part ?X ?Z))))))) (greaterThan ?PARTPROB ?NOTPARTPROB))";
        f.read(strf);
        fp = new FormulaPreprocessor();
        System.out.println("Formula: " + f);
        System.out.println("Var types: " + fp.computeVariableTypesExpr(f.expr,kb));

        System.out.println();
        strf = "(<=> (instance ?REL TransitiveRelation) " +
                "(forall (?INST1 ?INST2 ?INST3) " +
                "(=> (and (?REL ?INST1 ?INST2) " +
                "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";
        f.read(strf);
        fp = new FormulaPreprocessor();
        System.out.println("Formula: " + f);
        System.out.println("Var types: " + fp.computeVariableTypesExpr(f.expr,kb));
        System.out.println("Explicit types: " + fp.findExplicitTypesInAntecedent(kb,f));
    }

    /******************************************************************
     */
    public static void testFindExplicit() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        String formStr = "(<=> (instance ?REL TransitiveRelation) " +
                "(forall (?INST1 ?INST2 ?INST3) " +
                "(=> (and (?REL ?INST1 ?INST2) " +
                "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";
        Formula f = new Formula(formStr);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        System.out.println("Formula: " + f);
        Pattern p = Pattern.compile("\\(instance (\\?[a-zA-Z0-9]+) ([a-zA-Z0-9\\-_]+)");
        Matcher m = p.matcher(formStr);
        m.find();
        String var = m.group(1);
        String cl = m.group(2);
        System.out.println("FormulaPreprocessor.testExplicit(): " + var + Formula.SPACE + cl);
        System.out.println("Explicit types: " + fp.findExplicitTypesInAntecedent(kb, f));
    }

    /******************************************************************
     */
    public static void testAddTypes() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        System.out.println();
        String strf = "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) " +
                "(element ?ELEMENT ?SET2))) (equal ?SET1 ?SET2))";
        Formula f = new Formula();
        f.read(strf);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        //FormulaPreprocessor.debug = true;
        Map<String, Set<String>> varmap = fp.findTypeRestrictionsExpr(f.expr, kb);
        System.out.println(fp.addTypeRestrictionsExpr(f.expr,varmap,kb));

        System.out.println();
        strf = "(=> (and (attribute ?AREA LowTerrain) (part ?ZONE ?AREA)" +
                " (slopeGradient ?ZONE ?SLOPE)) (greaterThan 0.03 ?SLOPE))";
        f.read(strf);
        fp = new FormulaPreprocessor();
        System.out.println(fp.addTypeRestrictionsExpr(f.expr,varmap,kb));

        System.out.println();
        strf = "(=> (and (typicalPart ?PART ?WHOLE) (instance ?X ?PART) " +
                "(equal ?PARTPROB (ProbabilityFn (exists (?Y) (and " +
                "(instance ?Y ?WHOLE) (part ?X ?Y))))) (equal ?NOTPARTPROB " +
                "(ProbabilityFn (not (exists (?Z) (and (instance ?Z ?WHOLE) " +
                "(part ?X ?Z))))))) (greaterThan ?PARTPROB ?NOTPARTPROB))";
        f.read(strf);
        fp = new FormulaPreprocessor();
        System.out.println(fp.addTypeRestrictionsExpr(f.expr,varmap,kb));
    }

    /******************************************************************
     */
    public static void testOne() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        System.out.println();
        System.out.println();
        FormulaPreprocessor fp;
        String strf = "(=>\n" +
                "    (equal\n" +
                "        (GreatestCommonDivisorFn @ROW) ?NUMBER)\n" +
                "    (forall (?ELEMENT)\n" +
                "        (=>\n" +
                "            (inList ?ELEMENT\n" +
                "                (ListFn @ROW))\n" +
                "            (equal\n" +
                "                (RemainderFn ?ELEMENT ?NUMBER) 0))))";
        Formula f = new Formula();
        f.read(strf);
        fp = new FormulaPreprocessor();
        //System.out.println(fp.findType(1,"part",kb));
        System.out.println(fp.preProcessExpr(f,false,kb));
    }

    /******************************************************************
     */
    public static void testTwo() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        System.out.println();
        System.out.println();
        FormulaPreprocessor fp;
        String strf = "(equal (AbsoluteValueFn ?NUMBER1) 2)";
        Formula f = new Formula();
        f.read(strf);
        fp = new FormulaPreprocessor();
        System.out.println("testTwo(): equality: " + fp.preProcessExpr(f,false,kb));
    }

    /******************************************************************
     */
    public static void testThree() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        System.out.println();
        System.out.println();
        FormulaPreprocessor fp;
        String strf = "\n" +
                "(<=>\n" +
                "    (and\n" +
                "        (equal\n" +
                "            (AbsoluteValueFn ?NUMBER1) ?NUMBER2)\n" +
                "        (instance ?NUMBER1 RealNumber)\n" +
                "        (instance ?NUMBER2 RealNumber))\n" +
                "    (or\n" +
                "        (and\n" +
                "            (instance ?NUMBER1 NonnegativeRealNumber)\n" +
                "            (equal ?NUMBER1 ?NUMBER2))\n" +
                "        (and\n" +
                "            (instance ?NUMBER1 NegativeRealNumber)\n" +
                "            (equal ?NUMBER2\n" +
                "                (SubtractionFn 0 ?NUMBER1)))))";
        Formula f = new Formula();
        f.read(strf);
        fp = new FormulaPreprocessor();
        System.out.println("testThree(): " + fp.preProcessExpr(f,false,kb));
    }

    /******************************************************************
     */
    public static void testFour() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        System.out.println();
        System.out.println();
        FormulaPreprocessor fp;
        String strf = "(forall (?NUMBER ?ELEMENT ?CLASS)\n" +
                "        (=>\n" +
                "          (equal ?ELEMENT\n" +
                "            (ListOrderFn\n" +
                "              (ListFn_1 ?FOO) ?NUMBER))\n" +
                "          (instance ?ELEMENT ?CLASS)))";
        Formula f = new Formula();
        f.read(strf);
        fp = new FormulaPreprocessor();
        Map<String, Set<String>> varmap = fp.findTypeRestrictionsExpr(f.expr, kb);
        System.out.println("testFour() signature for ListFn: " + kb.kbCache.signatures.get("ListFn"));
        System.out.println("testFour() valence for ListFn: " + kb.kbCache.valences.get("ListFn"));
        System.out.println("testFour() signature for ListFn_1: " + kb.kbCache.signatures.get("ListFn_1"));
        System.out.println("testFour() valence for ListFn_1: " + kb.kbCache.valences.get("ListFn_1"));
        System.out.println("testFour(): " + fp.addTypeRestrictionsExpr(f.expr,varmap,kb));
    }

    /******************************************************************
     */
    public static void testFive() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        System.out.println();
        System.out.println();
        FormulaPreprocessor fp;
        String strf = "(equal (AdditionFn 1 2) ?X)";
        Formula f = new Formula();
        f.read(strf);
        fp = new FormulaPreprocessor();
        System.out.println("testFive(): equality: " + fp.preProcessExpr(f,false,kb));
    }

    /******************************************************************
     */
    public static void test6() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        if (!kb.terms.contains("avgWorkHours")) {
            System.out.println("FormulaPreprocessor.test6(): Demographics.kif not loaded");
            return;
        }
        System.out.println();
        System.out.println();
        String strf = "(=>\n" +
                "  (avgWorkHours ?H ?N)\n" +
                "  (lessThan ?N 70.0))";
        Formula f = new Formula();
        f.read(strf);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        debug = true;
        System.out.println("test6(): " + fp.preProcessExpr(f,false,kb));
    }

    /******************************************************************
     */
    public static void test7() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        System.out.println();
        System.out.println();
        String strf = "(=>\n" +
                "  (and\n" +
                "    (confersObligation USGovernment ?A ?F)\n" +
                "    (not ?F)) \n" +
                "  (exists (?H)\n" +
                "    (and\n" +
                "      (instance ?H LegalAction) \n" +
                "      (plaintiff ?H USGovernment)\n" +
                "      (defendant ?H ?A))))";
        Formula f = new Formula();
        f.read(strf);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        debug = true;
        System.out.println("test7(): " + fp.preProcessExpr(f,false,kb));
    }
    /******************************************************************
     */
    public static void showHelp() {

        System.out.println("FormulaPreprocessor");
        System.out.println("  h - show this help screen");
        System.out.println("  t - run tests");
        System.out.println("  --types \"<fomula>\" - translate one formula with modals");
    }

    /***************************************************************
     */
    public static void main(String[] args) {

        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        if (argMap.isEmpty() || argMap.containsKey("h"))
            showHelp();
        else {
            if (argMap.containsKey("t")) {
                //testOne();
                //testTwo();
                //testThree();
                //testFour();
                test7();
                //testFindTypes();
                //testAddTypes();
                //testFindExplicit();
            }
            else if (argMap.containsKey("types")) {
                System.out.println("------------------------------------");
                System.out.println("Compute types for formula");
                KBmanager.getMgr().initializeOnce();
                KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
                System.out.println("Init complete");
                Formula f = new Formula(StringUtil.removeEnclosingQuotes(argMap.get("types").get(0)));
                FormulaPreprocessor fp = new FormulaPreprocessor();
                System.out.println("Formula: " + f);
                System.out.println("Var types: " + fp.computeVariableTypesExpr(f.expr, kb));
                System.out.println("Explicit types: " + fp.findExplicitTypesInAntecedent(kb, f));
            }
        }
    }
}
