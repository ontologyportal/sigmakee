package com.articulate.sigma.parsing;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RowVar {

    public KB kb = null;
    public boolean debug = false;

    /** ***************************************************************
     */
    public RowVar(KB kbin) {

        if (!PredVarInst.predVarInstDone)
            System.err.println("Error! in RowVar(): Predicate variable instantiation is required and has not been completed");
        kb = kbin;
    }

    /** ***************************************************************
     */
    public List<String> getVarSubStrings(String var) {

        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        String varName = var.substring(1), varList;
        for (int i = 1; i <= 7; i++) {
            sb.append(Formula.V_PREF).append(varName).append(i).append(Formula.SPACE);
            varList = sb.toString();
            varList = varList.substring(0, varList.length() - 1);
            result.add(varList);
        }
        return result;
    }

    /** ***************************************************************
     */
    public Set<FormulaAST> expandVariableArityRowVar(Set<FormulaAST> flist, String var) {

        List<String> varLists = getVarSubStrings(var);
        Set<FormulaAST> result = new HashSet<>();
        List<FormulaAST> formulaList;
        FormulaAST f2, fnew;
        String literal, pred, varName, varList, newliteral, fnSuffix, newPredName;
        StringBuilder sb = new StringBuilder();
        int predArity;
        for (FormulaAST f : flist) {
            formulaList = new ArrayList<>();
            for (int i = 1; i <= 7; i++) {
                f2 = new FormulaAST(f);
                formulaList.add(f2);
            }
            if (debug) System.out.println("expandVariableArityRowVar(): row var structs " + f.rowVarStructs);
            for (FormulaAST.RowStruct rs : f.rowVarStructs.get(var)) {
                if (debug) System.out.println("expandVariableArityRowVar(): variable row struct " + rs);
                literal = rs.literal;
                pred = rs.pred;
                sb.setLength(0); // reset
                varName = var.substring(1);
                for (int i = 0; i <= 6; i++) {
                    fnew = formulaList.get(i);
                    varList = varLists.get(i);
                    if (debug) System.out.println("expandVariableArityRowVar(): replace varname : @" +
                            varName + " with varlist: " + varList);
                    newliteral = literal.replace(Formula.R_PREF + varName, varList);
                    fnSuffix = Formula.FN_SUFF;
                    if (!pred.endsWith(Formula.FN_SUFF))
                        fnSuffix = "";
                    if (debug) System.out.println("expandVariableArityRowVar(): literal arity: " + rs.arity);
                    if (debug) System.out.println("expandVariableArityRowVar(): i: " + i);
                    if (rs.arity > 1)
                        predArity = i + rs.arity;
                    else
                        predArity = i+1;
                    newPredName = pred + "_" + predArity + fnSuffix;
                    if (!pred.equals("__quantList")) {
                        if (debug) System.out.println("expandVariableArityRowVar(): replace pred : " +
                                pred + " with new pred: " + newPredName + " in " + newliteral);
                        newliteral = newliteral.replace(pred, newPredName);
                    }
                    rs.literal = newliteral;
                    if (debug) System.out.println("expandVariableArityRowVar(): fnew before " + fnew.getFormula());
                    if (debug) System.out.println("expandVariableArityRowVar(): literal " + literal);
                    if (debug) System.out.println("expandVariableArityRowVar(): newliteral " + newliteral);
                    if (!pred.equals("__quantList"))
                        fnew.setFormula(fnew.getFormula().replace(literal, newliteral));
                    else {
                        if (debug) System.out.println("expandVariableArityRowVar(): replace (" + literal + ") with (" + newliteral + ")");
                        fnew.setFormula(fnew.getFormula().replace(Formula.LP + literal + Formula.RP, Formula.LP + newliteral + Formula.RP));
                    }
                    if (debug) System.out.println("expandVariableArityRowVar(): fnew after " + fnew.getFormula());
                }
                if (debug) System.out.println("expandVariableArityRowVar(): formulaList: " + formulaList);
            }
            result.addAll(formulaList);
        }
        if (debug) System.out.println("expandVariableArityRowVar():result: " + result);
        return result;
    }

    /** ***************************************************************
     * @return arities implied on every row var by their relations and arguments.
     * if the pred var is only an argument to variable arity relations, return -1
     *
     */
    public Map<String, Integer> findArities(FormulaAST f) {

        if (debug && f.getFormula().contains(" maxValue ")) {
            System.out.println("findArities():" + f);
            f.printCaches();
        }
        Map<String, Integer> arities = new HashMap<>();
        int predArity, rowVarArity;
        for (Set<FormulaAST.RowStruct> rshs : f.rowVarStructs.values()) {
            // Sort by predicate name so the "last wins" overwrite is deterministic across JVM runs.
            // RowStruct uses default Object.hashCode() (identity-based), so HashSet iteration order
            // varies between JVM instances; sorting by pred name makes it stable.
            List<FormulaAST.RowStruct> sortedRshs = rshs.stream()
                    .sorted(Comparator.comparing(rs -> rs.pred))
                    .collect(Collectors.toList());
            for (FormulaAST.RowStruct rs : sortedRshs) {
                if (debug) System.out.println("findArities(): variable " + rs.rowvar + " pred: " + rs.pred);
                if (KB.isVariable(rs.pred)) {
                    System.err.println("Error in RowVar.findArities(): variable pred: " + rs.pred + " in "  + f);
                }
                else if (kb.kbCache.isInstanceOf(rs.pred,"VariableArityPredicate") && arities.get(rs.rowvar) == null )
                    arities.put(rs.rowvar,-1);
                else {
                    if (rs.pred.equals("__quantList"))
                        continue;
                    predArity = kb.kbCache.getArity(rs.pred);
                    rowVarArity = predArity;
                    if (rs.arity > 1)
                        rowVarArity = rowVarArity - (rs.arity - 1); // if there's more than one argument, var arity is reduced
                    if (predArity == -1)
                        rowVarArity = -1;
                    if (debug) System.out.println("findArities(): variable " + rs.rowvar + " pred: " +
                            rs.pred + " pred arity: " + predArity + " row arity: " + rowVarArity + " rs.arity: " + rs.arity + " in "  + f);
                    if (arities.get(rs.rowvar) == null || predArity != -1) {
                        Integer existing = arities.get(rs.rowvar);
                        if (existing != null && existing > 0 && rowVarArity > 0 && !existing.equals(rowVarArity)) {
                            // Two predicates sharing this row variable require different expansion sizes.
                            // Generating one size for both would apply at least one predicate with the
                            // wrong arity (e.g. a binary pred with 3 args).  Mark as conflict so
                            // expandRowVar() can drop this formula rather than emit an invalid one.
                            arities.put(rs.rowvar, 0);
                            if (debug) System.out.println("findArities(): arity conflict for " + rs.rowvar +
                                    " between " + existing + " and " + rowVarArity + " — marking as 0");
                        } else if (existing == null || existing != 0) {
                            arities.put(rs.rowvar, rowVarArity);
                            if (debug) System.out.println("findArities(): put " + rs.rowvar + " row arity: " + rowVarArity);
                        }
                    }
                }
            }
        }

        // Second pass: domain-type conflict detection.
        // Two predicates sharing a row variable may have the same expansion arity
        // but incompatible domain types at some expanded position (e.g., one expects
        // Integer/$int, the other expects Physical/$i).  Generating one expansion for
        // both would produce a formula with a type mismatch.  Detect this by checking,
        // for each row-var expansion position, whether any pair of predicates require
        // numeric vs. non-numeric types.  If so, mark the row var with sentinel 0 so
        // that expandRowVar() drops the formula.
        for (Map.Entry<String, Integer> entry : new HashMap<>(arities).entrySet()) {
            String rowVarName = entry.getKey();
            int rvArity = entry.getValue();
            if (rvArity <= 0) continue; // already conflicted or variable-arity

            Set<FormulaAST.RowStruct> rsSet = f.rowVarStructs.get(rowVarName);
            if (rsSet == null) continue;

            // For each expansion position (1..rvArity), collect the domain types
            // contributed by each predicate that uses this row var.
            Map<Integer, Set<String>> posTypes = new HashMap<>();
            for (FormulaAST.RowStruct rs : rsSet) {
                if (rs.pred.equals("__quantList")) continue;
                List<String> sig = kb.kbCache.signatures.get(rs.pred);
                if (sig == null) continue;
                int pArity = kb.kbCache.getArity(rs.pred);
                if (pArity <= 0) continue;
                // The row var occupies absolute positions rs.arity..pArity in the predicate
                for (int absPos = rs.arity; absPos <= pArity; absPos++) {
                    if (absPos >= sig.size()) continue;
                    String type = sig.get(absPos);
                    if (type == null || type.isEmpty()) continue;
                    int expansionPos = absPos - (rs.arity - 1); // 1-indexed relative to row var
                    posTypes.computeIfAbsent(expansionPos, k -> new HashSet<>()).add(type);
                }
            }

            // If any expansion position has both a numeric and a non-numeric domain
            // type, the expansion would generate an ill-typed formula.  Mark as 0.
            boolean conflict = false;
            for (Set<String> types : posTypes.values()) {
                boolean hasNumeric = false, hasNonNumeric = false;
                for (String type : types) {
                    if (kb.isSubclass(type, "RealNumber") || type.equals("RealNumber"))
                        hasNumeric = true;
                    else
                        hasNonNumeric = true;
                }
                if (hasNumeric && hasNonNumeric) {
                    conflict = true;
                    break;
                }
            }
            if (conflict) {
                arities.put(rowVarName, 0);
                if (debug) System.out.println("findArities(): domain type conflict for " + rowVarName +
                        " (numeric and non-numeric types at same expansion position) — marking as 0");
            }
        }

        return arities;
    }

    /** ***************************************************************
     * @return a HashSet of FormulaAST.  If just one row variable is an
     * argument to a fixed arity relation, then there will be just one
     * element in the returned set.  But if there one or more row
     * variables are arguments to VariableArityRelations then there will
     * be multiple returned formulas
     */
    public Set<FormulaAST> expandRowVar(FormulaAST f) {

        if (debug) System.out.println("expandRowVar(): f: " + f);
        Map<String, Integer> varArities = findArities(f);
        Set<FormulaAST> result = new HashSet<>();
        if (debug) System.out.println("expandRowVar(): variable arity vars list " + varArities);
        Set<FormulaAST> flist = new HashSet<>();
        flist.add(f);
        String varName, literal, pred, newliteral, fnSuffix, newPredName;
        StringBuilder sb = new StringBuilder();
        for (String var : varArities.keySet()) {
            result = new HashSet<>();
            if (debug) System.out.println("expandRowVar(): expanding var: " + var);
            int arity = varArities.get(var);
            if (debug) System.out.println("expandRowVar(): var arity: " + arity);
            if (arity == 0) {
                // Arity conflict: two predicates sharing this row variable require different
                // expansion sizes.  Any expansion would apply at least one predicate with the
                // wrong number of arguments, producing an invalid formula.  Drop it entirely.
                if (debug) System.out.println("expandRowVar(): dropping formula due to row-var arity conflict for " + var + " in " + f);
                return new HashSet<>();
            }
            if (arity == -1)
                result.addAll(expandVariableArityRowVar(flist,var));
            else {
                varName = var.substring(1);
                sb.setLength(0); // reset
                for (int i = 1; i <= arity; i++)
                    sb.append(Formula.V_PREF).append(varName).append(i).append(Formula.SPACE);
                if (sb.length() > 0)
                    sb.deleteCharAt(sb.length() - 1);
                else{
                    System.out.println("RowVar.expandRowVar(): null string for " + f);
                    continue;
                }

                for (FormulaAST.RowStruct rs : f.rowVarStructs.get(var)) {
                    if (debug) System.out.println("expandRowVar(): variable row struct " + rs);
                    literal = rs.literal;
                    pred = rs.pred;
                    if (!pred.equals("__quantList") && kb.kbCache.valences.get(pred) == null) {
                        System.out.println("RowVar.expandRowVar(): null valence for " + pred + " in " + f);
                        continue;
                    }
                    if (kb.kbCache.valences.get(pred) == -1 && rs.rowvar.equals(var)) {
                        newliteral = literal.replace(Formula.R_PREF + varName, sb.toString());
                        // and we don't want a false match to a part of a var name
                        fnSuffix = Formula.FN_SUFF;
                        if (!pred.endsWith(Formula.FN_SUFF))
                            fnSuffix = "";
                        newPredName = pred;
                        if (!pred.equals("__quantList"))
                            newPredName = pred + "_" + Integer.toString(arity) + fnSuffix;
                        if (debug) System.out.println("expandRowVar(): replace pred : " +
                                pred + " with new pred: " + newPredName);
                        if (debug) System.out.println("expandRowVar(): in literal : " +
                                newliteral);
                        if (!pred.equals("__quantList"))
                            newliteral = newliteral.replace(pred, newPredName);
                        rs.literal = newliteral;
                        f.setFormula(f.getFormula().replace(literal, newliteral));
                    }
                }

                f.setFormula(f.getFormula().replace(var, sb.toString()));
                result.add(f);
            }
            flist = new HashSet<>();
            flist.addAll(result);
            if (debug) System.out.println("expandRowVar() result for var: " + var);
            if (debug) {
                for (FormulaAST fp : flist) {
                    if (fp.getFormula().contains(Formula.R_PREF))
                        System.err.println("Error in RowVar.expandRowVar(): row var in output -");
                    System.out.println(fp.getFormula() + "\n");
                }
            }
            result = new HashSet<>();
            result.addAll(flist);
        }
        return result;
    }

    /** ***************************************************************
     */
    public Set<FormulaAST> expandRowVar(Set<FormulaAST> rowvars) {

        kb.kbCache.valences.put("__quantList",-1); // quantifier list doesn't have a real predicate
        Set<FormulaAST> result = new HashSet<>();
        for (FormulaAST f : rowvars)
            if (!f.higherOrder && !f.containsNumber)
            result.addAll(expandRowVar(f));
        return result;
    }

    /** ***************************************************************
     * Expr-based row-variable expansion.  Mirrors {@link #expandRowVar(FormulaAST)}
     * but operates on the structured {@link Expr} AST instead of the KIF string,
     * avoiding all string-level find/replace operations.
     *
     * @param fa the FormulaAST whose {@code expr} field will be expanded
     * @return set of expanded Expr trees (one per arity combination);
     *         returns a singleton containing {@code fa.expr} if no row vars found
     */
    public Set<Expr> expandRowVarExpr(FormulaAST fa) {

        kb.kbCache.valences.put("__quantList", -1); // quantifier list doesn't have a real predicate
        Map<String, Integer> arities = findArities(fa);
        if (arities.isEmpty()) return Set.of(fa.expr);

        Set<Expr> working = new HashSet<>();
        working.add(fa.expr);

        for (Map.Entry<String, Integer> entry : arities.entrySet()) {
            String rowVarName = entry.getKey();
            int arity = entry.getValue();
            String varBase = rowVarName.substring(1); // strip '@'

            Set<Expr> next = new HashSet<>();
            if (arity == 0) {
                // Arity conflict: two predicates sharing this row variable require different
                // expansion sizes.  Mirrors the guard in expandRowVar(FormulaAST).
                if (debug) System.out.println("expandRowVarExpr(): dropping formula due to row-var arity conflict for " + rowVarName);
                return new HashSet<>();
            } else if (arity == -1) {
                // VariableArityRelation: generate 7 expansions (n = 1..7)
                for (int n = 1; n <= 7; n++) {
                    List<Expr> vars = new ArrayList<>();
                    for (int i = 1; i <= n; i++)
                        vars.add(new Expr.Var("?" + varBase + i));
                    for (Expr e : working)
                        next.add(spliceRowVar(e, rowVarName, vars, fa, n));
                }
            } else {
                // Fixed arity
                List<Expr> vars = new ArrayList<>();
                for (int i = 1; i <= arity; i++)
                    vars.add(new Expr.Var("?" + varBase + i));
                for (Expr e : working)
                    next.add(spliceRowVar(e, rowVarName, vars, fa, arity));
            }
            working = next;
        }
        return working;
    }

    /** ***************************************************************
     * Recursively splice {@code vars} in place of all occurrences of
     * {@code Expr.RowVar(rowVarName)} in the Expr tree rooted at {@code expr}.
     * When a substitution is made and the enclosing SExpr head is a
     * VariableArityRelation (valence == -1), the head is renamed to
     * {@code pred_totalArity[Fn]}.  Does NOT mutate any RowStruct fields.
     */
    private Expr spliceRowVar(Expr expr, String rowVarName, List<Expr> vars, FormulaAST fa, int n) {

        if (!(expr instanceof Expr.SExpr se)) return expr;

        List<Expr> newArgs = new ArrayList<>();
        boolean spliced = false;
        for (Expr arg : se.args()) {
            if (arg instanceof Expr.RowVar rv && rv.name().equals(rowVarName)) {
                newArgs.addAll(vars); // splice N vars in place of 1 RowVar
                spliced = true;
            } else {
                newArgs.add(spliceRowVar(arg, rowVarName, vars, fa, n));
            }
        }

        Expr newHead = se.head();
        if (spliced && newHead instanceof Expr.Atom headAtom) {
            String pred = headAtom.name();
            if (!pred.equals("__quantList")) {
                Integer valence = kb.kbCache.valences.get(pred);
                if (valence != null && valence == -1) {
                    // VariableArityRelation: rename predicate to pred_totalArity[Fn].
                    // Use newArgs.size() directly — it is always correct after @ROW splicing
                    // and avoids the Set-ordering ambiguity in computeTotalArity() when the
                    // same pred appears with different arities under the same row variable
                    // (e.g. (ListFn @ROW ?ITEM) and (ListFn @ROW) in the same formula).
                    int totalArity = newArgs.size();
                    String fnSuffix = pred.endsWith(Formula.FN_SUFF) ? Formula.FN_SUFF : "";
                    String newPredName = pred + "__" + totalArity + fnSuffix;
                    newHead = new Expr.Atom(newPredName);
                    kb.kbCache.copyNewPredFromVariableArity(newPredName, pred, totalArity);
                }
            }
        }
        return new Expr.SExpr(newHead, newArgs);
    }

    /** ***************************************************************
     * Compute the total predicate arity after splicing {@code n} expanded
     * row-var arguments into a VariableArityRelation.  Mirrors the arithmetic
     * in {@link #expandVariableArityRowVar} and {@link #expandRowVar(FormulaAST)}.
     */
    private int computeTotalArity(String pred, String rowVarName, int n, FormulaAST fa) {

        Set<FormulaAST.RowStruct> rsSet = fa.rowVarStructs.get(rowVarName);
        if (rsSet != null) {
            for (FormulaAST.RowStruct rs : rsSet) {
                if (rs.pred.equals(pred))
                    return rs.arity > 1 ? rs.arity - 1 + n : n;
            }
        }
        return n; // fallback: no matching RowStruct found
    }
}
