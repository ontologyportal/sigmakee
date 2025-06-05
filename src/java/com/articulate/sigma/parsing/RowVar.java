package com.articulate.sigma.parsing;

import com.articulate.sigma.KB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            sb.append("?").append(varName).append(i).append(" ");
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
                    newliteral = literal.replace("@" + varName, varList);
                    fnSuffix = "Fn";
                    if (!pred.endsWith("Fn"))
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
                        fnew.setFormula(fnew.getFormula().replace("(" + literal + ")", "(" + newliteral + ")"));
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
            for (FormulaAST.RowStruct rs : rshs) {
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
                        arities.put(rs.rowvar, rowVarArity);
                        if (debug) System.out.println("findArities(): put " + rs.rowvar + " row arity: " + rowVarArity);
                    }
                }
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
            if (arity == -1)
                result.addAll(expandVariableArityRowVar(flist,var));
            else {
                varName = var.substring(1);
                sb.setLength(0); // reset
                for (int i = 1; i <= arity; i++)
                    sb.append("?").append(varName).append(i).append(" ");
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
                        newliteral = literal.replace("@" + varName, sb.toString());
                        // and we don't want a false match to a part of a var name
                        fnSuffix = "Fn";
                        if (!pred.endsWith("Fn"))
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
                    if (fp.getFormula().contains("@"))
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
}
