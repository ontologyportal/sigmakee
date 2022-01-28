package com.articulate.sigma;

import com.articulate.sigma.utils.StringUtil;

import java.util.*;

//This software is released under the GNU Public License
//<http://www.gnu.org/copyleft/gpl.html>.
// Copyright 2019 Infosys, 2020- Articulate Software
// apease@articulatesoftware.com

public class RowVars {

    public static boolean DEBUG = false;
    public static final int MAX_ARITY = 5;

    /** ***************************************************************
     * @return a HashSet, possibly empty, containing row variable
     * names, each of which will start with the row variable
     * designator '@'.
     */
    public static HashSet<String> findRowVars(Formula f) {

        if (DEBUG) System.out.println("Info in RowVars.findRowVars(): f: " + f);
        HashSet<String> result = new HashSet<String>();
        if (!StringUtil.emptyString(f.getFormula())
            && f.getFormula().contains(Formula.R_PREF)) {
            if (DEBUG) System.out.println("Info in RowVars.findRowVars(): contains at least one");
            Formula fnew = new Formula();
            fnew.read(f.getFormula());
            while (fnew.listP() && !fnew.empty()) {
                String arg = fnew.getStringArgument(0);
                if (DEBUG) System.out.println("Info in RowVars.findRowVars(): arg: " + arg);
                if (arg.startsWith(Formula.R_PREF))
                    result.add(arg);
                else if (Formula.listP(arg)) {
                    Formula argF = new Formula();
                    argF.read(arg);
                    if (argF.listP())
                        result.addAll(findRowVars(argF));
                }
                fnew.read(fnew.cdr());
                if (DEBUG) System.out.println("Info in RowVars.findRowVars(): fnew.cdr(): " + fnew);
            }
        }
        return result;
    }

    /** ***************************************************************
     * given in @param ar which is a list for each variable of all the
     * predicates in which it appears as an argument, find the minimum
     * arity allowed by predicate arities, as given by 
     * @seeAlso kb.kbCache.valences
     */
    private static HashMap<String,Integer> getRowVarMaxArities(HashMap<String,HashSet<String>> ar, KB kb) {

        HashMap<String,Integer> arities = new HashMap<String,Integer>();
        Iterator<String> it = ar.keySet().iterator();
        while (it.hasNext()) {
            String rowvar = it.next();
            HashSet<String> preds = ar.get(rowvar);
            Iterator<String> it2 = preds.iterator();
            while (it2.hasNext()) {
                String pred = it2.next();
                if (kb.kbCache.isInstanceOf(pred,"VariableArityRelation"))
                    System.out.println("Error in RowVars.getRowVarMaxArities(): contains variable arity relation: " + pred);
                //System.out.println("INFO in RowVars.getRowVarMaxArities(): " + kb.kbCache.valences);
                //System.out.println("INFO in RowVars.getRowVarMaxArities(): pred: " + pred);
                if (kb.kbCache.valences.get(pred) != null) {
                    int arity = kb.kbCache.valences.get(pred).intValue();
                    if (DEBUG) System.out.println("INFO in RowVars.getRowVarMaxArities(): pred: " + pred + " arity: " + arity);
                    if (arities.containsKey(pred)) {
                        if (arity < arities.get(rowvar).intValue())
                            arities.put(rowvar, Integer.valueOf(arity));
                    }
                    else
                        arities.put(rowvar, Integer.valueOf(arity));
                }
                else
                    System.out.println("Error in RowVars.getRowVarMaxArities(): no arity for " + pred);
            }
        }
        return arities;
    }

    /** ***************************************************************
     * @param ar a list for each variable of all the predicates in
     *           which it appears as an argument
     * @result the maximum arity allowed by predicate arities, as given by
     * @seeAlso kb.kbCache.valences
     */
    public static HashMap<String,Integer> getRowVarMaxAritiesWithOtherArgs(HashMap<String,HashSet<String>> ar, KB kb, Formula f) {

        if (DEBUG) System.out.println("getRowVarMaxAritiesWithOtherArgs() predicates: " + ar);
        if (DEBUG) System.out.println("getRowVarMaxAritiesWithOtherArgs() formula: " + f);
        HashMap<String,Integer> arities = new HashMap<String,Integer>();
        for (String rowvar : ar.keySet()) {
            HashSet<String> preds = ar.get(rowvar);
            for (String pred : preds) {
                // If row variables in an argument list with other arguments,
                // then #arguments which can be expanded = #arguments in pred - nonRowVar
                int nonRowVar = 0;
                boolean done = false;
                int start = f.getFormula().indexOf("(" + pred);
                int end = f.getFormula().indexOf(")", start);
                String simpleFS = FormulaUtil.getLiteralWithPredAndRowVar(pred,f);
                if (simpleFS == null)
                    continue;
                if (DEBUG) System.out.println("getRowVarMaxAritiesWithOtherArgs() looking at " + simpleFS);
                Formula simpleF = new Formula();
                simpleF.read(simpleFS);
                for (int i = 0; i < simpleF.listLength(); i++) {
                    if (simpleF.getStringArgument(i).startsWith(Formula.V_PREF)) // a '?'
                        nonRowVar++;
                }

                if (DEBUG) System.out.println("getRowVarMaxAritiesWithOtherArgs() non row var count " + nonRowVar);
                if (kb.kbCache!= null && kb.kbCache.valences != null &&
                        kb.kbCache.valences.get(pred) != null) {
                    int arity = kb.kbCache.valences.get(pred).intValue();
                    if (kb.isInstanceOf(pred,"VariableArityRelation"))
                        arity = Formula.MAX_PREDICATE_ARITY;
                    arity = arity - nonRowVar;
                    if (DEBUG)
                        System.out.println("getRowVarMaxAritiesWithOtherArgs() pred,arity " + pred + ", " + arity);
                    if (arities.containsKey(rowvar) && DEBUG)
                        System.out.println("getRowVarMaxAritiesWithOtherArgs() previous arity " + arities.get(rowvar));
                    if (arities.containsKey(rowvar)) {
                        if (arity < arities.get(rowvar))
                            arities.put(rowvar, arity);
                    }
                    else if (arity > 0)
                        arities.put(rowvar, arity);
                }
            }
        }
        if (DEBUG) System.out.println("getRowVarMaxAritiesWithOtherArgs() arities " + arities);
        return arities;
    }

    /** ***************************************************************
     * @param ar a list for each variable of all the predicates in
     *           which it appears as an argument
     * @result the minimum arity allowed by predicate arities, as given by
     * @seeAlso kb.kbCache.valences
     *
     */
    public static HashMap<String,Integer> getRowVarMinAritiesWithOtherArgs(HashMap<String,HashSet<String>> ar, KB kb, Formula f) {

        if (DEBUG) System.out.println("getRowVarMinAritiesWithOtherArgs(): f: " + f);
        if (DEBUG) System.out.println("getRowVarMinAritiesWithOtherArgs(): ar: " + ar);
        HashMap<String,Integer> arities = new HashMap<String,Integer>();
        for (String rowvar : ar.keySet()) {
            HashSet<String> preds = ar.get(rowvar);
            for (String pred : preds) {
                if (DEBUG) System.out.println("getRowVarMinAritiesWithOtherArgs() pred " + pred);
                // If row variables in an argument list with other arguments,
                // then #arguments which can be expanded = #arguments in pred - nonRowVar
                int startIndex = 0;
                while (startIndex < f.getFormula().length()) {
                    int start = f.getFormula().indexOf("(" + pred, startIndex);
                    if (start == -1) {
                        startIndex = f.getFormula().length();
                        continue;
                    }
                    int end = f.getFormula().indexOf(")", start);
                    startIndex = end + 1;
                    String simpleFS = f.getFormula().substring(start, end + 1);
                    if (simpleFS.indexOf("@") == -1)
                        continue;
                    int nonRowVar = 0;
                    if (DEBUG)
                        System.out.println("getRowVarMinAritiesWithOtherArgs() looking at " + simpleFS);
                    Formula simpleF = new Formula();
                    simpleF.read(simpleFS);
                    for (int i = 0; i < simpleF.listLength(); i++) {
                        if (simpleF.getStringArgument(i).startsWith(Formula.V_PREF)) // a '?'
                            nonRowVar++;
                    }

                    if (DEBUG)
                        System.out.println("getRowVarMinAritiesWithOtherArgs() non row var count " + nonRowVar);
                    if (kb.kbCache != null && kb.kbCache.valences != null &&
                            kb.kbCache.valences.get(pred) != null) {
                        int arity = kb.kbCache.valences.get(pred).intValue();
                        if (kb.isInstanceOf(pred, "VariableArityRelation"))
                            arity = 1;
                        arity = arity - nonRowVar;
                        if (DEBUG)
                            System.out.println("getRowVarMinAritiesWithOtherArgs() pred,arity " + pred + ", " + arity);
                        if (arities.containsKey(rowvar) && DEBUG)
                            System.out.println("getRowVarMinAritiesWithOtherArgs() previous arity " + arities.get(rowvar));
                        if (arities.containsKey(rowvar)) {
                            if (arity > arities.get(rowvar))
                                arities.put(rowvar, arity);
                        }
                        else if (arity > 0)
                            arities.put(rowvar, arity);
                    }
                }
            }
        }
        if (DEBUG) System.out.println("getRowVarMinAritiesWithOtherArgs() arities " + arities);
        return arities;
    }

    /** ***************************************************************
     * Merge the key,value pairs for a multiple value ArrayList
     */
    private static HashMap<String,HashSet<String>> 
        mergeValueSets(HashMap<String,HashSet<String>> ar1, HashMap<String,HashSet<String>> ar2) {
        
        HashMap<String,HashSet<String>> result = new HashMap<String,HashSet<String>>();
        result.putAll(ar1);
        for (String key  : ar2.keySet()) {
            HashSet<String> values = ar2.get(key);
            HashSet<String> arg1values = ar1.get(key);
            if (arg1values == null)
                result.put(key, values);
            else {
                arg1values.addAll(values);
            }
        }
        return result;
    }
    
    /** ***************************************************************
     * Add a key,value pair for a multiple value ArrayList
     */
    private static HashMap<String,HashSet<String>> 
        addToValueSet(HashMap<String,HashSet<String>> ar, String key, String value) {
        
        HashSet<String> val = ar.get(key);
        if (val == null) 
            val = new HashSet<String>();
        val.add(value);
        ar.put(key, val);
        return ar;
    }
    
    /** ***************************************************************
     */
    private static HashMap<String,HashSet<String>> getRowVarRelLogOps(Formula f, String pred) {
    
        if (DEBUG) System.out.println("Info in RowVars.getRowVarRelLogOps(): pred: " + pred + " f: " + f);
        HashMap<String,HashSet<String>> result = new HashMap<String,HashSet<String>>();
        if (Formula.isQuantifier(pred)) {
            Formula arg2 = new Formula(f.getArgument(2));
            if (arg2 != null)
                return getRowVarRelations(arg2);
        }
        else if (pred.equals(Formula.NOT)) {
            Formula arg1 = new Formula(f.getArgument(1));
            if (arg1 != null)
                return getRowVarRelations(arg1);
            else
                return result;
        }
        else if (pred.equals(Formula.EQUAL) || pred.equals(Formula.IFF)  || pred.equals(Formula.IF)) {
            Formula arg1 = new Formula(f.getArgument(1));
            Formula arg2 = new Formula(f.getArgument(2));
            if (arg1 != null && arg2 != null)
                return mergeValueSets(getRowVarRelations(arg1),getRowVarRelations(arg2));
            else
                return result;
        }
        else {  // AND or OR
            ArrayList<String> args = f.complexArgumentsToArrayListString(1);
            if (DEBUG) System.out.println("Info in RowVars.getRowVarRelLogOps(): args: " + args);
            for (int i = 0; i < args.size(); i++) {
                Formula f2 = new Formula(args.get(i));
                result = mergeValueSets(result,getRowVarRelations(f2));
            }
            return result;
        }
        return result;
    }
    
    /** ***************************************************************
     * Recurse through the formula looking for row variables.  If found,
     * add it to a map that has row variables as keys and a set of 
     * predicate names as values. 
     */
    protected static HashMap<String,HashSet<String>> getRowVarRelations(Formula f) {

        if (DEBUG) System.out.println("Info in RowVars.getRowVarRelations(): f: " + f);
        HashMap<String,HashSet<String>> result = new HashMap<String,HashSet<String>>();
        if (!f.getFormula().contains("@") || f.empty() || f.atom())
            return result;
        String pred = f.getStringArgument(0);
        if (DEBUG) System.out.println("Info in RowVars.getRowVarRelations(): pred: " + pred);
        if (!f.getFormula().substring(1).contains("(")) {  // no higher order or functions
            if (DEBUG) System.out.println("Info in RowVars.getRowVarRelations(): simple clause f: " + f);
            HashSet<String> rowvars = findRowVars(f);
            if (DEBUG) System.out.println("Info in RowVars.getRowVarRelations(): found rowvars: " + rowvars);
            Iterator<String> it = rowvars.iterator();
            while (it.hasNext()) {
                String var = it.next();
                if (DEBUG) System.out.println("Info in RowVars.getRowVarRelations(): adding var,pred: " + var + ", " + pred);
                addToValueSet(result,var,pred);
            }
            return result;
        }
        if (Formula.isLogicalOperator(pred)) {
            return getRowVarRelLogOps(f,pred);
        }
        else {  // regular predicate
            ArrayList<String> args = f.complexArgumentsToArrayListString(1);
            for (int i = 0; i < args.size(); i++) {
                Formula f2 = new Formula(args.get(i));
                if (f2.getFormula().startsWith("@")) {
                    if (DEBUG) System.out.println("Info in RowVars.getRowVarRelations(): adding var,pred: " +
                            f2.getFormula() + ", " + pred);
                    addToValueSet(result,f2.getFormula(),pred);
                }
                else if (f2.getFormula().contains("@"))
                    result = mergeValueSets(result,getRowVarRelations(f2));
            }
        }
        return result;
    }
    
    /** ***************************************************************
     * Expand row variables, keeping the information about the original
     * source formula.  Each variable is treated like a macro that
     * expands to up to seven regular variables.  For example
     *
     * (=>
     *    (and
     *       (subrelation ?REL1 ?REL2)
     *       (?REL1 @ROW))
     *    (?REL2 @ROW))
     *
     * would become
     *
     * (=>
     *    (and
     *       (subrelation ?REL1 ?REL2)
     *       (?REL1 ?ARG1))
     *    (?REL2 ?ARG1))
     *
     * (=>
     *    (and
     *       (subrelation ?REL1 ?REL2)
     *       (?REL1 ?ARG1 ?ARG2))
     *    (?REL2 ?ARG1 ?ARG2))
     * etc.
     *
     * @return an ArrayList of Formulas, or an empty ArrayList.
     */
    public static ArrayList<Formula> expandRowVars(KB kb, Formula f) {

        ArrayList<String> result = new ArrayList<String>();
        ArrayList<Formula> formresult = new ArrayList<Formula>();
        if (!f.getFormula().contains("@")) {
            // If there are no row variables, return the original formula
            formresult.add(f);
            return formresult;
        }
        if (DEBUG) System.out.println("Info in RowVars.expandRowVars(): f: " + f);
        HashMap<String,HashSet<String>> rels = getRowVarRelations(f);
        if (DEBUG) System.out.println("Info in RowVars.expandRowVars(): getRowVarRelations " + rels);
        HashMap<String,Integer> rowVarMaxArities = getRowVarMaxAritiesWithOtherArgs(rels, kb, f);
        if (DEBUG) System.out.println("Info in RowVars.expandRowVars(): rowVarMaxArities: " + rowVarMaxArities);
        HashMap<String,Integer> rowVarMinArities = getRowVarMinAritiesWithOtherArgs(rels, kb, f);
        if (DEBUG) System.out.println("Info in RowVars.expandRowVars(): rowVarMinArities: " + rowVarMinArities);
        result.add(f.getFormula());
        HashSet<String> rowvars = findRowVars(f);
        for (String var : rowvars) {
            if (DEBUG)
                System.out.println("Info in RowVars.expandRowVars(): var: " + var);
            String replaceVar = var.replace('@', '?');
            ArrayList<String> newresult = new ArrayList<String>();
            StringBuffer replaceString = new StringBuffer();
            int maxArity = MAX_ARITY;
            int minArity = 1;
            if (rowVarMaxArities.containsKey(var) && maxArity > rowVarMaxArities.get(var).intValue())
                maxArity = rowVarMaxArities.get(var).intValue();
            if (rowVarMinArities.containsKey(var) && minArity < rowVarMinArities.get(var).intValue())
                minArity = rowVarMinArities.get(var).intValue();
            if (DEBUG)
                System.out.println("Info in RowVars.expandRowVars(): maxArity: " + maxArity);
            if (DEBUG)
                System.out.println("Info in RowVars.expandRowVars(): minArity: " + minArity);
            for (int j = 1; j < minArity; j++) {
                if (j > 1)
                    replaceString.append(" ");
                replaceString.append(replaceVar + Integer.toString(j+1));
            }
            for (int j = minArity; j <= maxArity; j++) {
                if (j > 0)
                    replaceString.append(" ");
                replaceString.append(replaceVar + Integer.toString(j+1));
                //if (DEBUG)
                //    System.out.println("Info in RowVars.expandRowVars(): replace: " + replaceString);
                for (int i = 0; i < result.size(); i++) {
                    String form = result.get(i);
                    form = form.replaceAll("\\"+var, replaceString.toString());
                    if (DEBUG)
                        System.out.println("Info in RowVars.expandRowVars(1): form: " + form);
                    //if (j == maxArity - 1) {
                        newresult.add(form);
                        if (DEBUG)
                            System.out.println("Info in RowVars.expandRowVars(2): form: " + form);
                    //}
                }
            }
            result = newresult;
        }
        
        for (int i = 0; i < result.size(); i++) {
            Formula newf = new Formula(result.get(i));
            newf.derivation.operator = "rowvar";
            newf.derivation.parents.add(f);
            formresult.add(newf);
        }
        if (DEBUG)
            System.out.println("Info in RowVars.expandRowVars(): exiting with: " + formresult);
        return formresult;
    }

    /** ***************************************************************
     * */
    public static void main(String[] args) {
        
        //String fstring = "(=> (and (subrelation ?REL1 ?REL2) (?REL1 @ROW)) (?REL2 @ROW))";
        String fstring = "(=> (and (contraryAttribute @ROW1) (identicalListItems (ListFn @ROW1) (ListFn @ROW2))) (contraryAttribute @ROW2))"; 
        Formula f = new Formula(fstring);
        System.out.println("Info in RowVars.main(): " + findRowVars(f));
        KBmanager.getMgr().initializeOnce();
        System.out.println("Info in RowVars.main(): finished initialization");
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        RowVars.DEBUG = true;
        System.out.println("Info in RowVars.main(): " + getRowVarRelations(f));
    }
}
