package com.articulate.sigma;

import com.articulate.sigma.parsing.FormulaAST;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Derivation implements Serializable {

    public String operator = "input";
    public List<FormulaAST> parents = new ArrayList<>();

    /** ***************************************************************
     */
    public Derivation() {}

    /** ***************************************************************
     */
    public Derivation(String op, List<FormulaAST> par) {

        operator = op;
        if (par != null)
            parents.addAll(par);
    }

    /** ***************************************************************
     */
    public Derivation deepCopy() {

        Derivation result = new Derivation();
        result.operator = this.operator;
        result.parents = new ArrayList<>();
        if (parents != null) {
            for (FormulaAST f : parents)
                result.parents.add(f.deepCopy());
        }
        return result;
    }

    /** ***************************************************************
     * Return a string for the derivation
     */
    public String toString() {

        if (operator.equals("input"))
            return "input";
        else {
            StringBuilder sb = new StringBuilder();
            sb.append("inference(").append(operator).append(", ");
            for (FormulaAST f : parents) {
                sb.append(f.getFormula());
                sb.append(":").append(f.derivation.toString()).append(")");
            }
            return sb.toString();
        }
    }

    /** ***************************************************************
     * Return a list of all derived objects that are used in this
     * derivation.
     */
    public List<FormulaAST> getParents() {

        if (operator.equals("input"))
            return new ArrayList<>();
        else {
            List<FormulaAST> res = new ArrayList<>();
            for (FormulaAST p : parents)
                res.addAll(p.derivation.parents);
            return res;
        }
    }
}
