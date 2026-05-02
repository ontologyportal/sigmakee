package com.articulate.sigma.parsing;

import com.articulate.sigma.KB;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

// Instantiate predicate variables
public class PredVarInst {

    public static boolean predVarInstDone = false;
    public static boolean debug = false;

    /** Mirror of FormulaPreprocessor.AXIOM_EXPANSION_LIMIT — stop generating new
     *  pred-var instantiations once this many have accumulated for a single formula. */
    private static final int AXIOM_EXPANSION_LIMIT = 2000;

    private final KB kb;
    private VarTypes vt = null;

    /** ***************************************************************
     */
    public PredVarInst(KB kbin) {
        kb = kbin;
    }

    /**
     * Recursively substitute all occurrences of {@code Var(varName)} with
     * {@code Atom(atomName)} in the given {@link Expr} tree.  Returns a new
     * tree (all nodes are immutable records; unchanged sub-trees are reused).
     */
    public static Expr substituteVar(Expr expr, String varName, String atomName) {

        return switch (expr) {
            case Expr.Var v when v.name().equals(varName) -> new Expr.Atom(atomName);
            case Expr.SExpr se -> {
                Expr newHead = se.head() != null ? substituteVar(se.head(), varName, atomName) : null;
                List<Expr> newArgs = se.args().stream()
                        .map(a -> substituteVar(a, varName, atomName))
                        .toList();
                yield new Expr.SExpr(newHead, newArgs);
            }
            default -> expr; // Atom, RowVar, NumLiteral, StrLiteral, other Var — unchanged
        };
    }

    /**
     * Walk the Expr tree to find where {@code varName} appears as the head of an SExpr.
     * Returns 0 if found with a RowVar argument (variable arity → accept any relation arity).
     * Returns the arg count if found with only regular args.
     * Returns -1 if not found in this subtree (caller treats as 0 = accept any).
     */
    private static int getPredVarArity(String varName, Expr expr) {
        return switch (expr) {
            case Expr.SExpr se -> {
                // Check if this SExpr's head IS the pred var
                if (se.head() instanceof Expr.Var v && v.name().equals(varName)) {
                    for (Expr arg : se.args()) {
                        if (arg instanceof Expr.RowVar) yield 0; // row var → any arity
                    }
                    yield se.args().size();
                }
                // Recurse into head
                if (se.head() != null) {
                    int r = getPredVarArity(varName, se.head());
                    if (r >= 0) yield r;
                }
                // Recurse into args
                for (Expr arg : se.args()) {
                    int r = getPredVarArity(varName, arg);
                    if (r >= 0) yield r;
                }
                yield -1; // not found in this subtree
            }
            default -> -1; // leaf node, not found
        };
    }

    /** ***************************************************************
     * Note that if there is more than one predicate variable we have to
     * cycle through all the formulas generated for the first variable
     */
    public List<FormulaAST> processOne(FormulaAST f) {

        vt = new VarTypes(null,kb); // no list of formulas since we'll just pass in one when calling constrainVars() below
        if (debug) System.out.println("PredVarInst.processOne()" + f);
        if (debug) System.out.println("PredVarInst.processOne(): varTypes" + f.varTypes);
        if (debug) System.out.println("PredVarInst.processOne(): f.predVarCache" + f.predVarCache);
        // Use ArrayList (not HashSet) to avoid triggering Formula.hashCode() → Clausifier.normalizeVariables()
        // on every add(). Each substitution produces a structurally distinct formula so deduplication is not needed.
        List<FormulaAST> result = new ArrayList<>(), newresult;
        result.add(new FormulaAST(f));
        Set<String> types, relations = new TreeSet<>();  // TreeSet: deterministic sorted iteration
        FormulaAST fnew;
        // Iterate pred vars in sorted order for deterministic output
        for (String var : new TreeSet<>(f.predVarCache)) {
            if (debug) System.out.println("PredVarInst.processOne(): substituting for var: " + var);
            types = f.varTypes.get(var);
            relations.clear();
            if (types != null) {
                for (String type : types) {
                    if (debug) System.out.println("PredVarInst.processOne(): var,type: " + var + ":" + type);
                    if (relations.isEmpty())
                        relations.addAll(kb.kbCache.getInstancesForType(type));
                    else
                        relations.retainAll(kb.kbCache.getInstancesForType(type));
                }
            }
            // Arity filtering: mirror instantiatePredVars logic.
            // Determine the arity this pred var requires from its usage site in the Expr tree.
            // arity == 0 → row var present → accept any arity relation
            // arity == N → only accept relations with valence N
            if (f.expr != null) {
                int arity = getPredVarArity(var, f.expr);
                if (arity < 0) arity = 0; // not found → conservative: accept any
                if (arity > 0) {
                    final int requiredArity = arity;
                    relations.removeIf(rel -> {
                        Integer valence = kb.kbCache.valences.get(rel);
                        return valence == null || !valence.equals(requiredArity);
                    });
                }
            }
            newresult = new ArrayList<>();
            outer:
            for (FormulaAST f2 : result) {
                if (debug) System.out.println("PredVarInst.processOne(): relations: " + relations);
                for (String rel : relations) {
                    fnew = new FormulaAST(f2);
                    fnew = vt.constrainVars(rel, var, fnew);
                    if (debug) System.out.println("PredVarInst.processOne(): substituting: " + rel + " for " + var);
                    if (debug) System.out.println("PredVarInst.processOne(): in formula: " + fnew);
                    if (fnew.expr != null) {
                        // Expr-based substitution: precise, no substring-match issues
                        fnew.expr = substituteVar(fnew.expr, var, rel);
                        fnew.setFormula(fnew.expr.toKifString());
                    } else {
                        fnew.setFormula(fnew.getFormula().replace(var, rel)); // TODO: vulnerable to a match of variable name substrings
                    }
                    if (debug) System.out.println("PredVarInst.processOne(): with result: " + fnew);
                    for (Set<FormulaAST.RowStruct> frhs : fnew.rowVarStructs.values()) {
                        for (FormulaAST.RowStruct fr : frhs) {
                            if (fr.pred.equals(var)) {  // have to update the row var record to reflect the pred var substitution
                                fr.pred = rel;
                                fr.literal = fr.literal.replace(var, rel);
                                // Predicate variables don't increment argnum during parsing, so
                                // rs.arity is 1 short (it counts only non-pred args). Now that a
                                // concrete predicate occupies the head position, add 1 so findArities()
                                // computes the correct rowVarArity for @ROW expansion.
                                fr.arity += 1;
                            }
                        }
                    }
                    if (debug) System.out.println("PredVarInst.processOne(): rowVarStructs: " + fnew.rowVarStructs);
                    newresult.add(fnew);
                    if (newresult.size() > AXIOM_EXPANSION_LIMIT) {
                        System.err.println("Error in PredVarInst.processOne(): " +
                                "AXIOM_EXPANSION_LIMIT EXCEEDED: " + AXIOM_EXPANSION_LIMIT);
                        break outer;
                    }
                }
            }
            result = newresult;
        }
        return result;
    }

    /** ***************************************************************
     */
    public Set<FormulaAST> processAll(Collection<FormulaAST> fs) {

        if (debug) System.out.println("PredVarInst.processAll()");
        Set<FormulaAST> result = new HashSet<>();
        for (FormulaAST fast : fs) {
            if (fast.higherOrder || fast.containsNumber) continue;
            result.addAll(processOne(fast));
        }
        predVarInstDone = true;
        return result;
    }
}
