package com.articulate.sigma;

import com.articulate.sigma.utils.StringUtil;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

public class FormulaAST {

    int startLine;
    int endLine;
    String sourceFile;

    // note that because car() and cdr() return pointers to Term(s) their
    // contents must never be altered
    private Term formula = null;
    private String asString = null;

    // An ArrayList of String messages with a message that has a reserved character of ':'
    // dividing the message from a formula or term that will be hyperlinked and formmated
    public TreeSet<String> errors = new TreeSet<String>();

    /** Warnings found during execution. */
    public TreeSet<String> warnings = new TreeSet<String>();

    // caches of frequently computed sets of variables in the formula
    public HashSet<String> allVarsCache = new HashSet<>();
    public ArrayList<HashSet<String>> allVarsPairCache = new ArrayList<HashSet<String>>();
    public HashSet<String> quantVarsCache = new HashSet<>();
    public HashSet<String> unquantVarsCache = new HashSet<>();
    public HashSet<String> existVarsCache = new HashSet<>();
    public HashSet<String> univVarsCache = new HashSet<>();
    public HashSet<String> termCache = new HashSet<>();

    /*****************************************************************
     */
    // note that because car() and cdr() return pointers to Term(s) their
    // contents must never be altered
    public class Term {
        String value;
    }

    public abstract class Variable extends Term {}
    public class RowVar extends Variable {}
    public class RegVar extends Variable {}

    public abstract class Constant extends Term {}
    public class Logop extends Constant {}
    public class UserTerm extends Constant {}
    public class StringTerm extends Constant {}
    public class NumberTerm extends Constant {}

    public class ListTerm extends Term {
        // note a null list is different from an initialized but empty list - "()"
        public ArrayList<Term> listElements = null;
    }

    /*****************************************************************
     */
    public boolean atom() {

        if (formula.getClass() != ListTerm.class)
            return true;
        return false;
    }

    /*****************************************************************
     */
    public boolean listP() {

        if (formula.getClass() == ListTerm.class)
            return true;
        return false;
    }
    /*****************************************************************
     */
    public boolean isVariable() {

        if (this.formula == null)
            return false;
        if (this.formula.getClass() != Variable.class)
            return false;
        return true;
    }

    /*****************************************************************
     */
    public boolean empty() {

        if (!listP())
            return false;
        ListTerm elements = (ListTerm) formula;
        if (elements == null || elements.listElements == null)
            return false;
        if (elements.listElements.size() != 0)
            return false;
        return true;
    }

    /*****************************************************************
     * This is a non-destructive operation that creates just a new FormulaAST
     * with the Term member, but no caches or auxilliary information,
     * which likely wouldn't be accurate if it were copied.
     */
    public FormulaAST car() {

        if (!listP())
            return null;
        ListTerm elements = (ListTerm) formula;
        if (elements == null)
            return null;
        FormulaAST result = new FormulaAST();
        result.formula = elements.listElements.get(0);
        return result;
    }

    /*****************************************************************
     * This is a non-destructive operation that creates just a new FormulaAST
     * with the Term member, but no caches or auxilliary information,
     * which likely wouldn't be accurate if it were copied.
     */
    public FormulaAST cdr() {

        if (!listP())
            return null;
        ListTerm elements = (ListTerm) formula;
        if (elements == null || elements.listElements.size() < 1)
            return null;
        FormulaAST result = new FormulaAST();
        result.formula = new ListTerm();
        ListTerm newelements = (ListTerm) result.formula;
        newelements.listElements = new ArrayList<>();
        for (Term t : elements.listElements)
            newelements.listElements.add(t);
        result.formula = elements.listElements.get(0);
        return result;
    }

    /*****************************************************************
     */
    public String toString() {

        if (asString != null)
            return asString;
        StringBuffer sb = new StringBuffer();
        if (formula == null)
            return null;
        if (formula.getClass().equals(Constant.class) || formula.getClass().equals(Variable.class))
            return formula.value;
        ArrayList<Term> elems = ((ListTerm) formula).listElements;
        if (elems == null || elems.size() < 1)
            return null;
        else {
            sb.append("(");
            for (int i = 0; i < elems.size(); i++) {
                Term t = elems.get(i);
                sb.append(t.toString());
                if (i < elems.size() - 1)
                    sb.append(" ");
            }
            sb.append(")");
        }
        asString = sb.toString();
        return sb.toString();
    }

    /** ***************************************************************
     *  @return a unique ID by appending the hashCode() of the
     *  formula String to the file name in which it appears
     */
    public String createID() {

        String fname = sourceFile;
        if (!StringUtil.emptyString(fname) && fname.lastIndexOf(File.separator) > -1)
            fname = fname.substring(fname.lastIndexOf(File.separator) + 1);
        int hc = formula.hashCode();
        String result = null;
        if (hc < 0)
            result = "N" + (Integer.valueOf(hc)).toString().substring(1) + fname;
        else
            result = (Integer.valueOf(hc)).toString() + fname;
        return result;
    }

    /** ***************************************************************
     * @see #validArgs() validArgs below for documentation
     */
    private String validArgsRecurse(FormulaAST f, String filename, Integer lineNo) {

        if (f.formula == null || !f.listP() || f.atom() || f.empty())
            return "";
        FormulaAST predForm = f.car();
        String pred = "";
        if (predForm.atom() && predForm.formula.getClass() == Logop.class) {
            pred = predForm.formula.value;
        }
        else
            return "";
        FormulaAST rest = f.cdr();
        FormulaAST restF = new FormulaAST();
        restF.read(rest);
        int argCount = 0;
        while (!restF.empty()) {
            argCount++;
            FormulaAST arg = restF.car();
            FormulaAST argF = new FormulaAST();
            argF.read(arg);
            String result = validArgsRecurse(argF, filename, lineNo);
            if (result != "")
                return result;
            restF.formula = restF.cdr().formula;
        }
        String location = "";
        if ((filename != null) && (lineNo != null))
            location = "near line " + lineNo + " in " + filename;
        if (pred.equals(Formula.AND) || pred.equals(Formula.OR)) {
            if (argCount < 2) {
                String errString = "Too few arguments for 'and' or 'or' at " + location + ": " + f.toString();
                errors.add(errString);
                return errString;
            }
        }
        else if (pred.equals(Formula.UQUANT) || pred.equals(Formula.EQUANT)) {
            if (argCount != 2) {
                String errString = "Wrong number of arguments for quantifer at " + location + ": " + f.toString();
                errors.add(errString);
                return errString;
            }
            else {
                FormulaAST quantF = new FormulaAST();
                quantF.read(rest);
                if (!quantF.car().listP()) {
                    String errString = "No var list for quantifier at " + location + ": " + f.toString();
                    errors.add(errString);
                    return errString;
                }
            }
        }
        else if (pred.equals(Formula.IFF) || pred.equals(Formula.IF)) {
            if (argCount != 2) {
                String errString = "Wrong number of arguments for '<=>' or '=>' at " + location + ": " + f.toString();
                errors.add(errString);
                return errString;
            }
        }
        else if (pred.equals(Formula.EQUAL)) {
            if (argCount != 2) {
                String errString = "Wrong number of arguments for 'equals' at " + location + ": " + f.toString();
                errors.add(errString);
                return errString;
            }
        }
        else if (!predForm.isVariable() && (argCount > (Formula.MAX_PREDICATE_ARITY + 1))) {
            //System.out.println("info in KIF.parse(): pred: " + pred);
            //System.out.println("info in KIF.parse(): " + this);
            String errString = "Maybe too many arguments at " + location + ": " + f.toString();
            errors.add(errString);
            return errString;
        }
        return "";
    }

    /** ***************************************************************
     * Test whether the Formula uses logical operators and predicates
     * with the correct number of arguments.  "equals", "<=>", and
     * "=>" are strictly binary.  "or", and "and" are binary or
     * greater. "not" is unary.  "forall" and "exists" are unary with
     * an argument list.  Warn if we encounter a formula that has more
     * arguments than MAX_PREDICATE_ARITY.
     *
     * @param filename If not null, denotes the name of the file being
     * parsed.
     *
     * @param lineNo If not null, indicates the location of the
     * expression (formula) being parsed in the file being read.
     *
     * @return an empty String if there are no problems or an error message
     * if there are.
     */
    public String validArgs(String filename, Integer lineNo) {

        if (formula == null)
            return "";
        Formula f = new Formula();
        String result = validArgsRecurse(this, filename, lineNo);
        return result;
    }

    /** ***************************************************************
     * Test whether the Formula uses logical operators and predicates
     * with the correct number of arguments.  "equals", "<=>", and
     * "=>" are strictly binary.  "or", and "and" are binary or
     * greater. "not" is unary.  "forall" and "exists" are unary with
     * an argument list.  Warn if we encounter a formula that has more
     * arguments than MAX_PREDICATE_ARITY.
     *
     * @return an empty String if there are no problems or an error message
     * if there are.
     */
    public String validArgs() {
        return this.validArgs(null, null);
    }

    /** ***************************************************************
     * Not yet implemented!  Test whether the Formula has variables that are not properly
     * quantified.  The case tested for is whether a quantified variable
     * in the antecedent appears in the consequent or vice versa.
     *
     *  @return an empty String if there are no problems or an error message
     *  if there are.
     */
    public String badQuantification() {
        return "";
    }

    /*****************************************************************
     */
    public void read(FormulaAST input) {

        formula = input.formula;
    }

    /*****************************************************************
     */
    public void read(String input) {

        StringReader sr = new StringReader(input);
        KIFAST kif = new KIFAST();
        TreeSet<String> errors = kif.parse(sr);
        if (kif.formulaMap == null || kif.formulaMap.size() == 0 || kif.formulaMap.size() > 0) {
            System.out.println("Error in FormulaAST.read(): no or multiple formulas in " + input);
        }
        else {
            formula = kif.formulaMap.values().iterator().next().formula;
        }
    }
}
