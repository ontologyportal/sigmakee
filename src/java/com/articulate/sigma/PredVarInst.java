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
import com.articulate.sigma.utils.StringUtil;

import java.util.*;

public class PredVarInst {

    // The implied arity of a predicate variable from its use in a Formula
    public static Map<String,Integer> predVarArity = new HashMap<>();

    // All predicates that meet that class membership and arity constraints for the given variable
//    private static Map<String,Set<String>> candidatePredicates = new HashMap<>();

    //The list of logical terms that not related to arity check, will skip these predicates
    private static final List<String> LOGICAL_TERMS = Arrays.asList(new String[]{"forall","exists","=>","and","or","<=>","not", "equal"});

    public static boolean debug = false;

    // a debugging option to reject formulas with more than one predicate variable, to save time
    public static boolean rejectDoubles = false;
    public static boolean doublesHandled = false;

    /** ***************************************************************
     */
    public static void init() {

        doublesHandled = false;
//        candidatePredicates = new HashMap<>();
        predVarArity = new HashMap<>();
    }

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
    protected static Map<String,Set<String>> addExplicitTypes(KB kb, Formula input, Map<String,Set<String>> types) {

        Map<String,Set<String>> result = new HashMap<>();
        FormulaPreprocessor fp = new FormulaPreprocessor();
    	Map<String,Set<String>> explicit = fp.findExplicitTypesInAntecedent(kb,input);
        if (explicit == null || explicit.keySet() == null || explicit.keySet().isEmpty())
            return types;

        Set<String> hs;
        for (String var : explicit.keySet()) {
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

        return rel.contains("__");
    }

    /** ***************************************************************
     * (=>
     *   (and
     *     (instance ?REL1 Predicate)
     *     (instance ?REL2 Predicate)
     *     (disjointRelation ?REL1 ?REL2)
     *     (not
     *       (equal ?REL1 ?REL2))
     *     (?REL1 @ROW2))
     *   (not
     *     (?REL2 @ROW2)))
     *

     */
    private static Set<Formula> handleDouble1(KB kb) {

        String origForm = "(=> (and (instance ?REL1 Predicate) (instance ?REL2 Predicate) (disjointRelation ?REL1 ?REL2) (not (equal ?REL1 ?REL2)) (?REL1 @ROW2)) (not (?REL2 @ROW2)))";
        if (debug) System.out.println("PredVarInst.handleDouble1(): " + origForm);
        Set<Formula> result = new HashSet<>();
        String arg1, arg2;
        int arity1, arity2;
        StringBuilder vars;
        Formula newf, orig;
        for (String s : kb.kbCache.disjointRelations) {
            arg1 = s.substring(0,s.indexOf("\t"));
            arg2 = s.substring(s.indexOf("\t")+1);
            arity1 = kb.kbCache.getArity(arg1);
            arity2 = kb.kbCache.getArity(arg2);
            if (arity1 != arity2)
                continue;
            vars = new StringBuilder();
            for (int i = 1; i <= arity1; i++)
                vars.append(" ?ROW").append(i);
            newf = new Formula("(=> (\" + arg1 + vars + \") (not (\" + arg2 + vars + \")))");
            newf.sourceFile = "Merge.kif";
            orig = kb.formulaMap.get(origForm);
            if (orig != null) {
                newf.startLine = orig.startLine;
                newf.derivation.operator = "predvar";
                newf.derivation.parents.add(orig);
            }
            result.add(newf);
        }
        if (debug) System.out.println("PredVarInst.handleDouble1(): # results: " + result.size());
        return result;
    }

    /** ***************************************************************
     * (=>
     *   (and
     *     (subrelation ?REL1 ?REL2)
     *     (instance ?REL1 Predicate)
     *     (instance ?REL2 Predicate)
     *     (?REL1 @ROW))
     *   (?REL2 @ROW))
     */
    private static Set<Formula> handleDouble2(KB kb) {

        String origForm = "(=> (and (subrelation ?REL1 ?REL2) (instance ?REL1 Predicate) " +
                "(instance ?REL2 Predicate) (?REL1 @ROW)) (?REL2 @ROW))";
        if (debug) System.out.println("PredVarInst.handleDouble2(): " + origForm);
        Set<Formula> result = new HashSet<>();
        for (String r1 : kb.kbCache.relations) {
            if (debug) System.out.println("handleDouble2(): relation: " + r1);
            if (debug) System.out.println("handleDouble2(): subrelation children: " + kb.kbCache.children.get("subrelation"));
            if (debug) System.out.println("handleDouble2(): in function set " + kb.kbCache.functions.contains(r1));
            if (debug) System.out.println("handleDouble2(): is function " + kb.isFunction(r1));
            if (kb.kbCache.functions.contains(r1)) // || kb.isFunction(r1))
                continue;
            Set<String> children = kb.kbCache.children.get("subrelation").get(r1);
            if (children != null) {
                int arity1, arity2;
                StringBuilder vars;
                Formula newf;
                for (String r2 : children) {
                    arity1 = kb.kbCache.getArity(r1);
                    arity2 = kb.kbCache.getArity(r2);
                    if (arity1 != arity2)
                        continue;
                    vars = new StringBuilder();
                    for (int i = 1; i <= arity1; i++)
                        vars.append(" ?ROW").append(i);
                    newf = new Formula("(=> (" + r2 + vars + ") (" + r1 + vars + "))");
                    newf.sourceFile = "Merge.kif";
                    Formula orig = kb.formulaMap.get(origForm);
                    if (orig != null) {
                        newf.startLine = orig.startLine;
                        newf.derivation.operator = "predvar";
                        newf.derivation.parents.add(orig);
                    }
                    result.add(newf);
                }
            }
        }
        if (debug) System.out.println("PredVarInst.handleDouble2(): results: " + result);
        return result;
    }

    /** ***************************************************************
     * A bit of a hack to produce the statements that would result from
     * the only two axioms in SUMO with two predicate variables
     */
    protected static Set<Formula> handleDoubles(KB kb) {

        Set<Formula> result = new HashSet<>();
        Set<Formula> result1 = handleDouble1(kb);
        if (result1 != null)
            result.addAll(result1);
        Set<Formula> result2 = handleDouble2(kb);
        if (result2 != null)
            result.addAll(result2);
        if (result1 != null && result2 != null)
            doublesHandled = true;
        if (debug) System.out.println("PredVarInst.handleDoubles(): handled with result: " + result);
        return result;
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
        Set<Formula> result = new HashSet<>();
        Set<String> predVars = gatherPredVars(kb,input);
        if (predVars.size() > 1) {
            if (rejectDoubles) {
                SUMOtoTFAform.filterMessage = "reject axioms with more than one predicate variable";
                if (debug) System.out.println("instantiatePredVars(): reject axioms with more than one predicate variable: \n" + input);
                return null;
            }
            else {
                if (!doublesHandled) {
                    SUMOtoTFAform.filterMessage = "axiom with more than one predicate variable";
                    if (debug) System.out.println("instantiatePredVars(): should handle: \n" + input);
                    return handleDoubles(kb);
                }
                else {
                    SUMOtoTFAform.filterMessage = "axiom with more than one predicate variable";
                    if (debug) System.out.println("instantiatePredVars(): should have already handled: \n" + input);
                    return null;
                }
            }
        }

        if (debug) System.out.println("instantiatePredVars(): predVars: " + predVars);
        if (predVars == null )
            return null;
        if (predVars.isEmpty())   // Return empty if input does not have predicate variables
            return result;
        // 1. get types for predicate variables from domain definitions
        Map<String,Set<String>> varTypes = findPredVarTypes(input,kb);
        // 2. add explicitly defined types for predicate variables
        varTypes = addExplicitTypes(kb,input,varTypes);
        if (debug) System.out.println("instantiatePredVars(): types: " + varTypes);
        Collection<String> rels;
        Integer arityInteger;
        boolean ok;
        Formula f, f2;
        for (String var : varTypes.keySet()) {
            if (predVarArity == null || var == null || predVarArity.get(var) == null)
                System.out.println("instantiatePredVars(): pred var arity null for: " + var +
                    " in " + input);
            // 3.1 check: predVarArity should match arity of substituted relation
            rels = kb.kbCache.predicates;
            if (varTypes.get(var).contains("Function"))
                rels = kb.kbCache.functions;
            for (String rel : rels) {
                //if (kb.isFunction(rel) || rel.endsWith("Fn")) { // can't substitute a function for where a relation is expected
                //    if (debug) System.out.println("instantiatePredVars(): excluding function: " + rel);
                //    continue;
                //}
                if (debug) System.out.println("instantiatePredVars(): check relation: " + rel);
                if (debug) System.out.println("instantiatePredVars(): pred var arity: " + predVarArity.get(var));
                if (debug) System.out.println("instantiatePredVars(): relation arity: " + kb.kbCache.valences.get(rel));
                if (rel.equals("equal") || Formula.isLogicalOperator(rel) ||
                        Formula.isComparisonOperator(rel) || Formula.isMathFunction(rel))
                    continue;
                if (isTypeExpansion(rel)) {
                    if (debug) System.out.println("instantiatePredVars(): type expansion of relation: " + rel);
                    continue;
                }
                if (predVarArity == null || var == null) System.out.println("instantiatePredVars(): pred var arity null for: " + var);
                arityInteger = predVarArity.get(var);
                int arity = 0;
                if (arityInteger != null)
                    arity = arityInteger;
                if (kb.kbCache.valences.get(rel).equals(arity) || arity == 0) {  // 0 arity means "any"
                    ok = true;
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
                    if (ok) {
                        if (debug) System.out.println("instantiatePredVars(): replacing: " + var + " with " + rel);
                        f = input.deepCopy();
                        f = f.replaceVar(var, rel);
                        if (debug) System.out.println("instantiatePredVars(): replaced: " + f);
                        f2 = input.deepCopy();
                        f2.read(f.getFormula());
                        f.derivation.operator = "predvar";
                        f.derivation.parents.add(input);
                        result.add(f);
                    }
                }
            }
        }
        if (result.isEmpty()) {   // Return null if input contains predicate variables but cannot be initialized
            String errStr = "No predicate instantiations for ";
            errStr += input.getFormula();
            input.errors.add(errStr);
            return null;
        }
        return result;
    }

    /** ***************************************************************
     * @return null if correct arity, otherwise return a message
     */
    private static String hasCorrectArityRecurse(Formula f, KB kb)
            throws IllegalArgumentException, TypeNotPresentException {

        //System.out.print("INFO in PredVarInst.hasCorrectArityRecurse(): " + f);
        if (f == null || StringUtil.emptyString(f.getFormula()) || f.empty() ||
                Formula.atom(f.getFormula()) || f.isVariable())
            return null;
        String rel = f.getStringArgument(0);
        List<Formula> argList = f.complexArgumentsToArrayList(1);
        if (argList == null || argList.isEmpty()) {
            return null;
        }
        //System.out.print("INFO in PredVarInst.hasCorrectArityRecurse(): args" + l);
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
            Integer intval = null;
            if (kb.kbCache != null && kb.kbCache.valences != null)
                intval= kb.kbCache.valences.get(rel);
            if (intval != null)
                val = intval;
            else {
                if (!LOGICAL_TERMS.contains(rel) && !rel.startsWith("?")) {
                    System.out.printf("%nINFO in PredVarInst.hasCorrectArityRecurse(): " +
                            "Predicate %s does not have an arity defined in KB, " +
                            "can't get the arity number!\n%s\n", rel, f, f.getSourceFile(), f.startLine);
                    //throw new IllegalArgumentException();
                }
            }
            if (kb.kbCache == null || !kb.kbCache.transInstOf(rel, "VariableArityRelation")) {
                //System.out.println("INFO in PredVarInst.hasCorrectArityRecurse() variable arity: " + f);
                if (val > 0 && val != argList.size()) {
                    System.err.println("Error in PredVarInst.hasCorrectArityRecurse() expected arity " +
                            val + " but found " + argList.size() + " for relation " + rel + " in formula: " + f);
                    throw new IllegalArgumentException(rel);
                }
            }
            if (f.isSimpleClause(kb)) {
                //check if the clause has function clause and check arity of function clause
                String result;
                for (Formula arg : argList) {
                    if ((arg.atom() && kb.isFunction(arg.getFormula())) ||
                            (arg.listP() && kb.isFunctional(arg)))   {
                        result = hasCorrectArityRecurse(new Formula(arg), kb);
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
        String res;
        for (Formula k : argList) {
            if (k.atom())
                continue;
            res = hasCorrectArityRecurse(k, kb);
            if (!StringUtil.emptyString(res))
                return res;
        }
        return null;
    }

    /** ***************************************************************
     * If arity is correct, return null, otherwise, return the predicate
     * that has its arity violated in the given formula.
     */
    public static String hasCorrectArity(Formula f, KB kb) {

        String res;
        try {
            res = hasCorrectArityRecurse(f,kb);
        }
        catch (IllegalArgumentException e){
            System.err.printf("FileName:%s\nLine number:%d\n",f.getSourceFile(),f.startLine);
            return e.getMessage();
        }
        return res;
    }

    /** ***************************************************************
     */
    private static boolean containsRowVariable(List<Formula> arglist) {

        for (Formula s : arglist)
            if (s.isRowVar())
                return true;
        return false;
    }

    /** ***************************************************************
     * Get a set of all the predicate variables in the formula.  If
     * the argument list has a row variable, return 0 as the value, meaning
     * any possible arity of 1 - maxArity
     */
    protected static Set<String> gatherPredVarRecurse(KB kb, Formula f) {

        Set<String> ans = new HashSet<>();
        if (debug) System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): " + f);
        if (f == null || f.empty() || Formula.atom(f.getFormula()) || f.isVariable())
            return ans;
        if (f.isSimpleClause(kb)) {
            Formula arg0 = f.getArgument(0);
            if (debug) System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): simple clause with: " + arg0);
            if (arg0.isRegularVariable()) {
                List<Formula> arglist = f.complexArgumentsToArrayList(1);
                if (arglist != null && !arglist.isEmpty()) {// a variable could be an argument to a higher-order formula
                    if (debug) System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): adding: " + arg0 +
                            " with arglist: " + arglist);
                    ans.add(arg0.getFormula());
                    if (containsRowVariable(arglist))
                        predVarArity.put(arg0.getFormula(),0);  // note that when expanding row vars we expand them to Formula.MAX_ARITY
                    else
                        predVarArity.put(arg0.getFormula(),arglist.size());
                }
                else {
                    if (debug) System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): not a predicate var: " + arg0);
                }
            }
        }
        else if (Formula.isQuantifier(f.car())) {
            if (debug) System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): found quantifier: " + f);
            Formula f2 = f.cddrAsFormula();
            ans.addAll(gatherPredVarRecurse(kb,f2));
        }
        else {
            if (debug) System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): not simple or quant: " + f);
            ans.addAll(gatherPredVarRecurse(kb,f.carAsFormula()));
            ans.addAll(gatherPredVarRecurse(kb,f.cdrAsFormula()));
        }
        if (debug) System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): returning: " + ans);
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
    protected static Map<String, Set<String>> findPredVarTypes(Formula f, KB kb) {

        Set<String> predVars = gatherPredVars(kb,f);
        if (debug) System.out.println("findPredVarTypes(): predVars: " + predVars);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        //HashMap<String,HashSet<String>> typeMap = fp.computeVariableTypes(f, kb);  // <- this skips explicit types
        //HashMap<String,HashSet<String>> typeMap = fp.findTypeRestrictions(f, kb);  // <- won't get instance relations
        Map<String,Set<String>> typeMap = fp.findAllTypeRestrictions(f, kb);
        if (debug) System.out.println("findPredVarTypes(): typeMap: " + typeMap);
        Map<String,Set<String>> result = new HashMap<>();
        for (String var : predVars) {
            if (typeMap.containsKey(var))
                result.put(var, typeMap.get(var));
        }
        if (debug) System.out.println("findPredVarTypes(): " + result);
        return result;
    }

    /** ***************************************************************
     * Collect and return all predicate variables for the given formula
     */
    public static Set<String> gatherPredVars(KB kb, Formula f) {

        if (debug) System.out.println("INFO in PredVarInst.gatherPredVars(): " + f);
        if (f.predVarCache != null) {
            if (debug) System.out.println("INFO in PredVarInst.gatherPredVars(): returning cache " + f.predVarCache);
            return f.predVarCache;
        }
        Set<String> varlist = null;
        if (!StringUtil.emptyString(f.getFormula())) {
            varlist = gatherPredVarRecurse(kb,f);
        }
        f.predVarCache = varlist;
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

//        arityTest();
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
