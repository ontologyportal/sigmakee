package com.articulate.sigma;

import java.util.*;

public class RowVars {

    public static boolean DEBUG = false;
    
    /** ***************************************************************
     * @return a HashSet, possibly empty, containing row variable
     * names, each of which will start with the row variable
     * designator '@'.
     */
    private static HashSet<String> findRowVars(Formula f) {

        HashSet<String> result = new HashSet<String>();
        if (!StringUtil.emptyString(f.theFormula)
            && f.theFormula.contains(Formula.R_PREF)) {
            Formula fnew = new Formula();
            fnew.read(f.theFormula);
            while (fnew.listP() && !fnew.empty()) {
                String arg = fnew.getArgument(0);
                if (arg.startsWith(Formula.R_PREF))
                    result.add(arg);
                else {
                    Formula argF = new Formula();
                    argF.read(arg);
                    if (argF.listP())
                        result.addAll(findRowVars(argF));
                }
                fnew.read(fnew.cdr());
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
    private static HashMap<String,HashSet<String>> getRowVarRelationsRecurse(Formula f) {
        
        HashMap<String,HashSet<String>> result = new HashMap<String,HashSet<String>>();
        if (!f.theFormula.contains("@"))
            return result;
        String pred = f.getArgument(0);
        if (f.isSimpleClause()) {            
            HashSet<String> rowvars = findRowVars(f);
            Iterator<String> it = rowvars.iterator();
            while (it.hasNext()) {
                String var = it.next();
                addToValueSet(result,pred,var);
            }
            return result;
        }
        if (Formula.isLogicalOperator(pred)) {
            if (Formula.isQuantifier(pred)) {
                Formula arg3 = new Formula(f.getArgument(2));
                if (arg3 != null)
                    return getRowVarRelationsRecurse(new Formula(f.getArgument(2)));
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
     *       (holds__ ?REL1 @ROW))
     *    (holds__ ?REL2 @ROW))
     *
     * would become
     *
     * (=>
     *    (and
     *       (subrelation ?REL1 ?REL2)
     *       (holds__ ?REL1 ?ARG1))
     *    (holds__ ?REL2 ?ARG1))
     *
     * (=>
     *    (and
     *       (subrelation ?REL1 ?REL2)
     *       (holds__ ?REL1 ?ARG1 ?ARG2))
     *    (holds__ ?REL2 ?ARG1 ?ARG2))
     * etc.
     *
     * @return an ArrayList of Formulas, or an empty ArrayList.
     */
    public static ArrayList<Formula> expandRowVars(KB kb, Formula f) {       
        
        ArrayList<String> result = new ArrayList<String>();
        result.add(f.theFormula);
        HashSet<String> rowvars = findRowVars(f);
        Iterator<String> it = rowvars.iterator();
        while (it.hasNext()) {
            String var = it.next();
            if (DEBUG)
                System.out.println("Info in RowVars.expandRowVars(): var: " + var);
            String replaceVar = var.replace('@', '?');
            ArrayList<String> newresult = new ArrayList<String>();
            StringBuffer replaceString = new StringBuffer();
            for (int j = 0; j < 7; j++) {
                if (j > 0)
                    replaceString.append(" ");
                replaceString.append(replaceVar + Integer.toString(j+1));
                if (DEBUG)
                    System.out.println("Info in RowVars.expandRowVars(): replace: " + replaceString);
                for (int i = 0; i < result.size(); i++) {
                    String form = result.get(i);
                    form = form.replaceAll("\\"+var, replaceString.toString());
                    if (DEBUG)
                        System.out.println("Info in RowVars.expandRowVars(): form: " + form);
                    newresult.add(form);
                }
            }
            result = newresult;
        }
        ArrayList<Formula> formresult = new ArrayList<Formula>();
        for (int i = 0; i < result.size(); i++) {
            Formula newf = new Formula(result.get(i));
            formresult.add(newf);
        }
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
        KB kb = KBmanager.getMgr().getKB("SUMO");
        RowVars.DEBUG = false;
        System.out.println("Info in RowVars.main(): " + expandRowVars(kb,f));
    }
}
