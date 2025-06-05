package com.articulate.sigma.parsing;

import com.articulate.sigma.KB;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

// Instantiate predicate variables
public class PredVarInst {

    public static boolean predVarInstDone = false;
    public static boolean debug = false;

    private final KB kb;
    private VarTypes vt = null;

    /** ***************************************************************
     */
    public PredVarInst(KB kbin) {
        kb = kbin;
    }

    /** ***************************************************************
     * Note that if there is more than one predicate variable we have to
     * cycle through all the formulas generated for the first variable
     */
    public Set<FormulaAST> processOne(FormulaAST f) {

        vt = new VarTypes(null,kb); // no list of formulas since we'll just pass in one when calling constrainVars() below
        if (debug) System.out.println("PredVarInst.processOne()" + f);
        if (debug) System.out.println("PredVarInst.processOne(): varTypes" + f.varTypes);
        if (debug) System.out.println("PredVarInst.processOne(): f.predVarCache" + f.predVarCache);
        Set<FormulaAST> result = new HashSet<>(), newresult;
        result.add(new FormulaAST(f));
        Set<String> types, relations = new HashSet<>();
        FormulaAST fnew;
        for (String var : f.predVarCache) {
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
            newresult = new HashSet<>();
            for (FormulaAST f2 : result) {
                if (debug) System.out.println("PredVarInst.processOne(): relations: " + relations);
                for (String rel : relations) {
                    fnew = new FormulaAST(f2);
                    fnew = vt.constrainVars(rel, var, fnew);
                    if (debug) System.out.println("PredVarInst.processOne(): substituting: " + rel + " for " + var);
                    if (debug) System.out.println("PredVarInst.processOne(): in formula: " + fnew);
                    fnew.setFormula(fnew.getFormula().replace(var, rel)); // TODO: vulnerable to a match of variable name substrings
                    if (debug) System.out.println("PredVarInst.processOne(): with result: " + fnew);
                    for (Set<FormulaAST.RowStruct> frhs : fnew.rowVarStructs.values()) {
                        for (FormulaAST.RowStruct fr : frhs) {
                            if (fr.pred.equals(var)) {  // have to update the row var record to reflect the pred var substitution
                                fr.pred = rel;
                                fr.literal = fr.literal.replace(var, rel);
                            }
                        }
                    }
                    if (debug) System.out.println("PredVarInst.processOne(): rowVarStructs: " + fnew.rowVarStructs);
                    newresult.add(fnew);
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
