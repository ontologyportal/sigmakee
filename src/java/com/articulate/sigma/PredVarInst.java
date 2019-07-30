package com.articulate.sigma;

/** This code is copyright Articulate Software (c) 2014. Infosys 2019-
 This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 Users of this code also consent, by use of this code, to credit Articulate Software
 and Teknowledge in any writings, briefings, publications, presentations, or
 other representations of any software which incorporates, builds on, or uses this
 code.  Please cite the following article in any publication with references:

 Pease, A., (2003). The Sigma Ontology Development Environment,
 in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 August 9, Acapulco, Mexico.  See also sigmakee.sourceforge.net

 Note that relations that are automatically created by SUMOKBtoTFAKB are excluded
 and this depends on double underscore in the names of those predicates
 */

import com.articulate.sigma.trans.SUMOtoTFAform;

import java.util.*;

public class PredVarInst {
    
    // The implied arity of a predicate variable from its use in a Formula
    public static HashMap<String,Integer> predVarArity = new HashMap<String,Integer>();
    
    // All predicates that meet that class membership and arity constraints for the given variable
    private static HashMap<String,HashSet<String>> candidatePredicates = new HashMap<String,HashSet<String>>();

    //The list of logical terms that not related to arity check, will skip these predicates
    private static List<String> logicalTerms=Arrays.asList(new String[]{"forall","exists","=>","and","or","<=>","not", "equal"});

    public static boolean debug = false;

    // a debugging option to reject formulas with more than one predicate variable, to save time
    public static boolean rejectDoubles = true;

    /** ***************************************************************
     * There are two type conditions:
     * one type condition is extracted from domain expression;
     * second type condition is specifically define in the antecedent
     * of a rule with an instance or subclass expression;
     *
     * @param input formula
     * @param types type condition extracted from domain expression.
     *              This is a HashMap in which the keys are predicate variables,
     *      and the values are HashSets containing one or more class
     *      names that indicate the type constraints that apply to the
     *      variable
     *
     * @return add explicit type condition into types
     */
    protected static HashMap<String,HashSet<String>> addExplicitTypes(KB kb, Formula input, HashMap<String,HashSet<String>> types) {
        
        HashMap<String,HashSet<String>> result = new HashMap<String,HashSet<String>>();
        FormulaPreprocessor fp = new FormulaPreprocessor();
    	HashMap<String,HashSet<String>> explicit = fp.findExplicitTypesInAntecedent(kb,input);
        if (explicit == null || explicit.keySet() == null || explicit.keySet().size() == 0)
            return types;
        Iterator<String> it = explicit.keySet().iterator();
        while (it.hasNext()) {
            String var = it.next();
            HashSet<String> hs = new HashSet<String>();
            if (types.containsKey(var)) { // only add keys from 'types' which contains all the pred vars
                hs = types.get(var);
                hs.addAll(explicit.get(var));
                result.put(var, hs);
            }
        }
        return result;
    }

    /** ***************************************************************
     */
    private static boolean isTypeExpansion(String rel) {

        if (rel.indexOf("__") == -1)
            return false;
        return true;
    }

    /** ***************************************************************
     * @param input formula
     * @param kb knowledge base
     * @return A list of formulas where predicate variables are instantiated;
     *         There are three possible returns:
     *         return null if input contains predicate variables but cannot be instantiated;
     *         return empty if input contains no predicate variables;
     *         return a list of instantiated formulas if the predicate variables are instantiated;
     */
    public static Set<Formula> instantiatePredVars(Formula input, KB kb) {

        if (debug) System.out.println("instantiatePredVars(): input: " + input);
        Set<Formula> result = new HashSet<Formula>();
        HashSet<String> predVars = gatherPredVars(kb,input);
        if (predVars.size() > 1 && rejectDoubles) {
            SUMOtoTFAform.filterMessage = "reject axioms with more than one predicate variable";
            System.out.println("instantiatePredVars(): reject axioms with more than one predicate variable: \n" + input);
            return null;
        }
        if (debug) System.out.println("instantiatePredVars(): predVars: " + predVars);
        if (predVars == null )
            return null;
        if (predVars.size() == 0)   // Return empty if input does not have predicate variables
            return result;
        // 1. get types for predicate variables from domain definitions
        HashMap<String,HashSet<String>> varTypes = findPredVarTypes(input,kb);
        // 2. add explicitly defined types for predicate variables
        varTypes = addExplicitTypes(kb,input,varTypes);
        if (debug) System.out.println("instantiatePredVars(): types: " + varTypes);
        Iterator<String> it = varTypes.keySet().iterator();
        while (it.hasNext()) {
            String var = it.next();
            if (predVarArity == null || var == null || predVarArity.get(var) == null)
                System.out.println("instantiatePredVars(): pred var arity null for: " + var +
                    " in " + input);
            // 3.1 check: predVarArity should match arity of substituted relation
            for (String rel : kb.kbCache.relations) {
                if (debug) System.out.println("instantiatePredVars(): check relation: " + rel);
                if (debug) System.out.println("instantiatePredVars(): pred var arity: " + predVarArity.get(var));
                if (debug) System.out.println("instantiatePredVars(): relation arity: " + kb.kbCache.valences.get(rel));
                if (rel.equals("equal"))
                    continue;
                if (isTypeExpansion(rel)) {
                    if (debug) System.out.println("instantiatePredVars(): type expansion of relation: " + rel);
                    continue;
                }
                if (predVarArity == null || var == null) System.out.println("instantiatePredVars(): pred var arity null for: " + var);
                Integer arityInteger = predVarArity.get(var);
                int arity = 0;
                if (arityInteger != null)
                    arity = arityInteger.intValue();
                if (kb.kbCache.valences.get(rel).equals(arity) || arity == 0) {  // 0 arity means "any"
                    boolean ok = true;
                    if (debug) System.out.println("instantiatePredVars(): types for var: " + varTypes.get(var));
                    for (String varType : varTypes.get(var)) {
                        //if (debug) System.out.println("instantiatePredVars(): checking whether relation: " + rel + " is an instance of " + varType);
                        // 3.2 check: candidate relation should be the instance of predicate variables' types
                        if (!kb.isInstanceOf(rel, varType)) {
                            if (debug) System.out.println("instantiatePredVars(): checking relation: " + rel + " is not an instance of " + varType);
                            if (debug) System.out.println("instantiatePredVars(): cache is null: " + (kb.kbCache == null));
                            ok = false;
                            break;
                        }
                        else
                            if (debug) System.out.println("instantiatePredVars(): relation: " + rel + " is an instance of " + varType);
                    }
                    // 4. If ok, instantiate the predicate variable using the candidate relation
                    if (ok == true) {
                        if (debug) System.out.println("instantiatePredVars(): replacing: " + var + " with " + rel);
                        Formula f = input.deepCopy();
                        f = f.replaceVar(var, rel);
                        Formula f2 = input.deepCopy();
                        f2.theFormula = f.theFormula;
                        result.add(f);
                    }
                }
            }
        }
        if (result.size() == 0) {   // Return null if input contains predicate variables but cannot be initialized
            String errStr = "No predicate instantiations for ";
            errStr += input.theFormula;
            input.errors.add(errStr);
            return null;
        }
        return result;
    }
     
    /** ***************************************************************
     */
    private static String hasCorrectArityRecurse(Formula f, KB kb)
            throws IllegalArgumentException, TypeNotPresentException {

        if (f == null || StringUtil.emptyString(f.theFormula) || f.empty() ||
                Formula.atom(f.theFormula) || f.isVariable())
            return null;
        String rel = f.getArgument(0);
        ArrayList<String> l = f.complexArgumentsToArrayList(1);
        int val = 0;
        //if the relation position is also a list, this condition is due to Formula.cdr()
        if (Formula.listP(rel)) {
            Formula p = new Formula();
            p.read(rel);
            String res = hasCorrectArityRecurse(p, kb);
            if (!StringUtil.emptyString(res))
                return res;
        }
        else {
            // (term) like wors, just return null, this condition is due to
            if (l == null) {
                return null;
            }
            Integer intval = null;
            if (kb.kbCache != null && kb.kbCache.valences != null)
                intval= kb.kbCache.valences.get(rel);
            if (intval != null)
                val = intval.intValue();
            else {
                if (l.size() != 0 && !logicalTerms.contains(rel) && !rel.startsWith("?")) {
                    System.out.printf("INFO in PredVarInst.hasCorrectArityRecurse(): Predicate %s does not have an arity defined in KB, can't get the arity number!\n%s\n", rel, f, f.getSourceFile(), f.startLine);
                    //throw new IllegalArgumentException();
                }
            }
            //check the arity number of current level predict
            if (kb.kbCache == null || !kb.kbCache.transInstOf(rel, "VariableArityRelation")) {
                //System.out.println("INFO in PredVarInst.hasCorrectArityRecurse() variable arity: " + f);
                if (val > 0 && val != l.size()) {
                    System.out.println("Error in PredVarInst.hasCorrectArityRecurse() expected arity " +
                            val + " but found " + l.size() + " for relation " + rel + " in formula: " + f);
                    throw new IllegalArgumentException(rel);
                }
            }
            if (f.isSimpleClause(kb)) {
                //check if the clause has function clause and check arity of function clause
                for (String arg : l) {
                    if ((Formula.atom(arg) && kb.isFunction(arg)) ||
                            (Formula.listP(arg) && kb.isFunctional(arg)))   {
                        String result = hasCorrectArityRecurse(new Formula(arg), kb);
                        if (!StringUtil.emptyString(result))
                            return result;
                    }
                }
            }
            else {
                //if quantified, just check the third argument
                if (Formula.isQuantifier(f.car()))
                    return hasCorrectArityRecurse(f.cddrAsFormula(), kb);
            }
        }
        if (l != null && l.size() != 0) {
            for (String k : l) {
                if (Formula.atom(k))
                    continue;
                Formula ff = new Formula();
                ff.read(k);
                String res = hasCorrectArityRecurse(ff, kb);
                if (!StringUtil.emptyString(res))
                    return res;
            }
        }
        return null;
    }
    
    /** ***************************************************************
     * If arity is correct, return null, otherwise, return the predicate
     * that has its arity violated in the given formula.
     */
    public static String hasCorrectArity(Formula f, KB kb) {

        String res = null;
        try {
            res = hasCorrectArityRecurse(f,kb);
        }
        catch(IllegalArgumentException e){
            System.out.printf("FileName:%s\nLine number:%d\n",f.getSourceFile(),f.startLine);
            return e.getMessage();
        }
        return res;
    }

    /** ***************************************************************
     */
    private static boolean containsRowVariable(ArrayList<String> arglist) {

        for (String s : arglist)
            if (s.startsWith("@"))
                return true;
        return false;
    }

     /** ***************************************************************
     * Get a set of all the predicate variables in the formula.  If
      * the argument list has a row variable, return 0 as the value, meaning
      * any possible arity of 1 - maxArity
     */
    protected static HashSet<String> gatherPredVarRecurse(KB kb, Formula f) {
        
        HashSet<String> ans = new HashSet<String>();
        //System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): " + f);
        if (f == null || f.empty() || Formula.atom(f.theFormula) || f.isVariable())
            return ans;
        if (f.isSimpleClause(kb)) {
            String arg0 = f.getArgument(0);
            //System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): simple clause with: " + arg0);
            if (arg0.startsWith("?")) {
                ArrayList<String> arglist = f.complexArgumentsToArrayList(1);
                if (arglist != null && arglist.size() > 0) {// a variable could be an argument to a higher-order formula
                    //System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): adding: " + arg0 +
                    //        " with arglist: " + arglist);
                    ans.add(arg0);
                    if (containsRowVariable(arglist))
                        predVarArity.put(arg0,0);  // note that when expanding row vars we expand them to Formula.MAX_ARITY
                    else
                        predVarArity.put(arg0,Integer.valueOf(arglist.size()));
                }
                else {
                    //System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): not a predicate var: " + arg0);
                }
            }
        }
        else if (Formula.isQuantifier(f.car())) {
            //System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): found quantifier: " + f);
            Formula f2 = f.cddrAsFormula();
            ans.addAll(gatherPredVarRecurse(kb,f2));
        }
        else {
            //System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): not simple or quant: " + f);
            ans.addAll(gatherPredVarRecurse(kb,f.carAsFormula()));
            ans.addAll(gatherPredVarRecurse(kb,f.cdrAsFormula()));
        }
        //System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): returning: " + ans);
        return ans;
    }

     /** ***************************************************************
     * Get a set of all the types for predicate variables in the formula.
     *
     * @return a HashMap in which the keys are predicate variables,
     * and the values are HashSets containing one or more class
     * names that indicate the type constraints that apply to the
     * variable.  If no predicate variables can be gathered from the
     * Formula, the HashMap will be empty.  Note that predicate variables
     * must logically be instances (of class Relation).
     */
    protected static HashMap<String, HashSet<String>> findPredVarTypes(Formula f, KB kb) {
        
        HashSet<String> predVars = gatherPredVars(kb,f);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        HashMap<String,HashSet<String>> typeMap = fp.computeVariableTypes(f, kb);
        HashMap<String,HashSet<String>> result = new HashMap<String,HashSet<String>>();
        Iterator<String> it = predVars.iterator();
        while (it.hasNext()) {
            String var = it.next();
            if (typeMap.containsKey(var))
                result.put(var, typeMap.get(var));
        }
        return result;
    }
    
    /** ***************************************************************
     * Collect and return all predicate variables for the given formula
     */
    protected static HashSet<String> gatherPredVars(KB kb, Formula f) {
        
        HashSet<String> varlist = null;
        HashMap<String,HashSet<String>> ans = new HashMap<String,HashSet<String>>();
        if (!StringUtil.emptyString(f.theFormula)) {
            varlist = gatherPredVarRecurse(kb,f);
        }
        return varlist;
    }

     /** ***************************************************************
     */
    public static void arityTest() {
        
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        System.out.println("INFO in PredVarInst.test(): completed loading KBs");
        String formStr = "(=> " +
        "(and " +
        "(instance ?SOUND RadiatingSound) " +
        "(agent ?SOUND ?OBJ) " +
        "(attribute ?SOUND Audible)) " +
        "(exists (?HUMAN) " +
        "(and " +
        "(instance ?HUMAN Human) " +
        "(capability " +
        "(KappaFn ?HEAR " +
        "(and " +
        "(instance ?HEAR Hearing) " +
        "(agent ?HEAR ?HUMAN) " +
        "(destination ?HEAR ?HUMAN) " +
        "(origin ?HEAR ?OBJ))) agent ?HUMAN)))) ";
        Formula f = new Formula(formStr);
        System.out.println("INFO in PredVarInst.arityTest(): formula: " + f);
        System.out.println("INFO in PredVarInst.arityTest(): correct arity: " + hasCorrectArity(f,kb));
    }
    
    /** ***************************************************************
     */
    public static void test() {
        
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        System.out.println("INFO in PredVarInst.test(): completed loading KBs");
        if (kb.kbCache.transInstOf("exhaustiveAttribute","VariableArityRelation")) {
            System.out.println("INFO in PredVarInst.test() variable arity: ");
        }
        else
            System.out.println("INFO in PredVarInst.test() not variable arity: ");
        System.out.println("INFO in PredVarInst.test(): " + kb.kbCache.instanceOf.get("partition"));
        System.out.println("INFO in PredVarInst.test(): " + kb.kbCache.insts.contains("partition"));
        //String formStr = "(=> (inverse ?REL1 ?REL2) (forall (?INST1 ?INST2) (<=> (?REL1 ?INST1 ?INST2) (?REL2 ?INST2 ?INST1))))";
        String formStr = "(<=> (instance ?REL TransitiveRelation) " +
        "(forall (?INST1 ?INST2 ?INST3) " +
        "(=> (and (?REL ?INST1 ?INST2) " +
        "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";
        //Formula f = kb.formulaMap.get(formStr);
        Formula f = new Formula(formStr);
        System.out.println("Formula: " + f);
        System.out.println("Pred vars: " + gatherPredVars(kb,f));
        System.out.println("Pred vars with types: " + findPredVarTypes(f,kb));
        FormulaPreprocessor fp = new FormulaPreprocessor();
        System.out.println("Explicit types: " + fp.findExplicitTypesInAntecedent(kb,f));
        System.out.println("Instantiated: " + instantiatePredVars(f,kb));
        System.out.println();
        
        formStr = "(=> " +
        "(instance ?JURY Jury) " +
        "(holdsRight " +
        "(exists (?DECISION) " +
        "(and " +
        "(instance ?DECISION LegalDecision) " +
        "(agent ?DECISION ?JURY))) ?JURY))";
        //f = kb.formulaMap.get(formStr);
        f = new Formula(formStr);
        System.out.println("Formula: " + f);
        System.out.println("Pred vars: " + gatherPredVars(kb,f));
        System.out.println("Instantiated: " + instantiatePredVars(f,kb));
        
    }
    
    /** ***************************************************************
     */
    public static void main(String[] args) {
        
        //arityTest();
        test();
        /*
         KBmanager.getMgr().initializeOnce();
         KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
         String formStr = "(<=> (instance ?REL TransitiveRelation) " +
         "(forall (?INST1 ?INST2 ?INST3) " +
         "(=> (and (?REL ?INST1 ?INST2) " +
         "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";
         formStr = "(=> (inverse ?REL1 ?REL2) (forall (?INST1 ?INST2) (<=> (?REL1 ?INST1 ?INST2) (?REL2 ?INST2 ?INST1))))";
         // formStr = " (=> (reflexiveOn ?REL ?CLASS) (forall (?INST) (=> (instance ?INST ?CLASS) (?REL ?INST ?INST))))";
         Formula f = kb.formulaMap.get(formStr);
         if (f == null) {
        	System.out.println("Error " + formStr + " not found.");
        	formStr = kb.formulas.get("ant-reflexiveOn").get(0);
        	f = kb.formulaMap.get(formStr);
         }
         
         System.out.println(instantiatePredVars(f,kb));
         */
    }
}
