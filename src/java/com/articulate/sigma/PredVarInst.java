package com.articulate.sigma;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class PredVarInst {
    
    //private static Formula _f;
    
    // The implied arity of a predicate variable from its use in a Formula
    private static HashMap<String,Integer> predVarArity = new HashMap<String,Integer>();
    
    // All predicates that meet that class membership and arity constraints for the given variable
    private static HashMap<String,HashSet<String>> candidatePredicates = new HashMap<String,HashSet<String>>();
    
    /** ***************************************************************
     * Returns an ArrayList of the Formulae that result from replacing
     * all arg0 predicate variables in the input Formula with
     * predicate names.
     *
     * Note this routine seems to check for correct arity after
     * substituting in a relation, but checking upfront would
     * be more efficient.
     *
     * @param kb A KB that is used for processing the Formula.
     *
     * @return An ArrayList of Formulas, or an empty ArrayList if no
     * instantiations can be generated.
     
     public static ArrayList<Formula> instantiatePredVars(Formula input, KB kb) {
     
    	_f = input;
     ArrayList<Formula> ans = new ArrayList<Formula>();
     if (_f.listP()) {
     String arg0 = _f.getArgument(0);
     // First we do some checks to see if it is worth processing the formula.
     if (Formula.isLogicalOperator(arg0) && _f.theFormula.matches(".*\\(\\s*\\?\\w+.*")) {
     // Get all pred vars, and then compute query lits for the pred vars, indexed by var.
     HashMap<String,HashSet<String>> varsWithTypes = findPredVarTypes(kb);
     //System.out.println("INFO in PredVarInst.instantiatePredVars(): " + varsWithTypes);
     if (!varsWithTypes.containsKey("arg0"))
     ans.add(_f); // The formula has no predicate variables in arg0 position, so just return it
     else {
     List indexedQueryLits = prepareIndexedQueryLiterals(kb, varsWithTypes);
     List substForms = new ArrayList();
     List varQueryTuples = null;
     List substTuples = null;
     List litsToRemove = null;
     
					// First, gather all substitutions.
     Iterator it1 = indexedQueryLits.iterator();
     while (it1.hasNext()) {
     varQueryTuples = (List) it1.next();
     substTuples = computeSubstitutionTuples(kb, varQueryTuples);
     if (substTuples != null && !substTuples.isEmpty()) {
     if (substForms.isEmpty())
     substForms.add(substTuples);
     else {
     int stSize = substTuples.size();
     int iSize = -1;
     int sfSize = substForms.size();
     int sfLast = (sfSize - 1);
     for (int i = 0 ; i < sfSize ; i++) {
     iSize = ((List) substForms.get(i)).size();
     if (stSize < iSize) {
     substForms.add(i, substTuples);
     break;
     }
     if (i == sfLast)
     substForms.add(substTuples);
     }
     }
     }
     }
     
     if (!substForms.isEmpty()) {
     // Try to simplify the Formula.
     Formula f = _f;
     Iterator it2 = substForms.iterator();
     while (it2.hasNext()) {
     substTuples = (List) it2.next();
     litsToRemove = (List) substTuples.get(0);
     Iterator it3 = litsToRemove.iterator();
     while (it3.hasNext()) {
     List lit = (List) it3.next();
     f = maybeRemoveMatchingLits(lit);
     }
     }
     
     // Now generate pred var instantiations from the possibly simplified formula.
     List<String> templates = new ArrayList<String>();
     templates.add(f.theFormula);
     HashSet<String> accumulator = new HashSet<String>();
     String template = null;
     String var = null;
     String term = null;
     ArrayList quantVars = null;
     int i = 0;
     // Iterate over all var plus query lits forms, getting
     // a list of substitution literals.
     Iterator it4 = substForms.iterator();
     while (it4.hasNext()) {
     substTuples = (List) it4.next();
     if ((substTuples instanceof List) && !substTuples.isEmpty()) {
     // Iterate over all ground lits ...
     // Remove litsToRemove, which we have already used above.
     litsToRemove = (List) substTuples.remove(0);
     
     // Remove and hold the tuple that indicates the variable substitution pattern.
     List varTuple = (List) substTuples.remove(0);
     Iterator it5 = substTuples.iterator();
     while (it5.hasNext()) {
     List groundLit = (List) it5.next();
     String groundTerm = (String) groundLit.get(1);
     // Iterate over all formula templates, substituting terms from each
     // ground lit for vars in the template.
     Iterator it6 = templates.iterator();
     Formula templateF = new Formula();
     while (it6.hasNext()) {
     template = (String) it6.next();
     templateF.read(template);
     quantVars = templateF.collectVariables().get(0);
     for (i = 0; i < varTuple.size(); i++) {
     var = (String) varTuple.get(i);
     if (Formula.isVariable(var)) {
     term = (String) groundLit.get(i);
     // Don't replace variables that are explicitly quantified.
     if (!quantVars.contains(var)) {
     List patternStrings = Arrays.asList("(\\W*\\()(\\s*holds\\s+\\"
     + var + ")(\\W+)",
     // "(\\W*\\()(\\s*\\" + var + ")(\\W+)",
     "(\\W*)(\\" + var + ")(\\W+)");
     int pslen = patternStrings.size();
     List patterns = new ArrayList();
     for (int j = 0; j < pslen; j++)
     patterns.add(Pattern.compile((String) (patternStrings.get(j))));
     int plen = patterns.size();
     Pattern p = null;
     Matcher m = null;
     for (int j = 0; j < plen; j++) {
     p = (Pattern) patterns.get(j);
     m = p.matcher(template);
     template = m.replaceAll("$1" + term + "$3");
     }
     }
     }
     }
     if (hasCorrectArity(new Formula(template), kb))
     accumulator.add(template);
     else {
     // System.out.println("Formula rejected because of incorrect arity: " + template);
     break;
     }
     }
     }
     templates.clear();
     templates.addAll(accumulator);
     accumulator.clear();
     }
     }
     ans.addAll(KB.stringsToFormulas(templates));
     }
     //if (ans.isEmpty())
					//	ans.add("reject");
     }
     }
     }
     return ans;
     }
     
     /** ***************************************************************
     * There are two type conditions:
     * one type condition is extracted from domain expression;
     * second type condition is specifically define in the antecedent
     * of a rule with an instance or subclass expression;
     *
     * @param input formula
     * @param types type condition extracted from domain expression
     *
     * @return add explicit type condition into types
     */
    private static HashMap<String,HashSet<String>> addExplicitTypes(Formula input, HashMap<String,HashSet<String>> types) {
        
        HashMap<String,HashSet<String>> result = new HashMap<String,HashSet<String>>();
        FormulaPreprocessor fp = new FormulaPreprocessor();
    	HashMap<String,HashSet<String>> explicit = fp.findExplicitTypesInAntecedent(input);
        if (explicit == null || explicit.keySet() == null || explicit.keySet().size() == 0)
            return types;
        Iterator<String> it = explicit.keySet().iterator();
        while (it.hasNext()) {
            String var = it.next();
            HashSet<String> hs = new HashSet<String>();
            if (types.containsKey(var))
                hs = types.get(var);
            hs.addAll(explicit.get(var));
            result.put(var,hs);
        }
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
    public static ArrayList<Formula> instantiatePredVars(Formula input, KB kb) {
        
        ArrayList<Formula> result = new ArrayList<Formula>();
        //System.out.println("INFO in PredVarInst.instantiatePredVars(): formula: " + input);
        // If there are no predicate variables, return empty()
        if (gatherPredVars(input).size() == 0)
            return result;
        HashMap<String,HashSet<String>> varTypes = findPredVarTypes(input,kb);
        varTypes = addExplicitTypes(input,varTypes);
        Iterator<String> it = varTypes.keySet().iterator();
        while (it.hasNext()) {
            String var = it.next();
            //System.out.println("INFO in PredVarInst.instantiatePredVars(): checking var: " + var);
            //System.out.println("INFO in PredVarInst.instantiatePredVars(): var arity: " + predVarArity.get(var));
            //System.out.println("INFO in PredVarInst.instantiatePredVars(): varTypes: " + varTypes.get(var));
            Iterator<String> it2 = kb.kbCache.relations.iterator();
            // predVarArity should match arity of substituted relation
            while (it2.hasNext()) {
                String rel = it2.next();
                //System.out.println("INFO in PredVarInst.instantiatePredVars(): checking rel: " + rel);
                //System.out.println("INFO in PredVarInst.instantiatePredVars(): with valence: " + kb.kbCache.valences.get(rel));
                //System.out.println("INFO in PredVarInst.instantiatePredVars(): var arity: " + predVarArity.get(var));
                if (kb.kbCache.valences.get(rel).equals(predVarArity.get(var))) {
                    //System.out.println("INFO in PredVarInst.instantiatePredVars(): matching arity");
                    boolean ok = true;
                    Iterator<String> it3 = varTypes.get(var).iterator();
                    while (it3.hasNext()) {
                        String varType = it3.next();
                        //System.out.println("INFO in PredVarInst.instantiatePredVars(): checking rel type: rel, varType: " +
                        //		rel + ", " + varType);
                        if (!kb.isInstanceOf(rel, varType)) {
                            ok = false;
                            break;
                        }
                    }
                    //System.out.println("INFO in PredVarInst.instantiatePredVars(): formula (2): " + input);
                    if (ok == true) {
                        //Pattern p = Pattern.compile("\\" + var + "([^a-zA-Z0-9])");
                        //Matcher m = p.matcher(input.theFormula);
                        //String fstr = m.replaceAll(rel + "$1");
                        Formula f = input.deepCopy();
                        f = f.replaceVar(var, rel);
                        //System.out.println("INFO in PredVarInst.instantiatePredVars(): formula (3): " + f);
                        //System.out.println("INFO in PredVarInst.instantiatePredVars(): ok to substitute: " + rel);
                        Formula f2 = input.deepCopy();
                        f2.theFormula = f.theFormula;
                        result.add(f);
                    }
                }
            }
        }
        // If there are predicate variables but cannot be initialized, return null
        if (result.size() == 0) {
            String errStr = "No predicate instantiations for ";
            errStr += input.theFormula;
            input.errors.add(errStr);
            return null;
        }
        return result;
    }
    
    /** ***************************************************************
     * Returns the number of SUO-KIF variables (only ? variables, not
     * @ROW variables) in the input query literal.
     *
     * @param queryLiteral A List representing a Formula.
     *
     * @return An int.
     
     private static int getVarCount(List<String> queryLiteral) {
     
     int ans = 0;
     if (queryLiteral instanceof List) {
     String term = null;
     Iterator<String> it = queryLiteral.iterator();
     while (it.hasNext()) {
     term = it.next();
     if (term.startsWith("?"))
     ans++;
     }
     }
     return ans;
     }
     
     /** ***************************************************************
     */
    private static String hasCorrectArityRecurse(Formula f, KB kb) {
        
        //System.out.println("INFO in PredVarInst.hasCorrectArityRecurse() f: " + f);
        if (f == null || StringUtil.emptyString(f.theFormula) || f.empty() ||
            Formula.atom(f.theFormula) || f.isVariable())
            return null;
        if (f.isSimpleClause()) {
            //System.out.println("INFO in PredVarInst.hasCorrectArityRecurse() simple clause f: " + f);
            String rel = f.getArgument(0);
            if (kb.kbCache.transInstOf(rel,"VariableArityRelation")) {
                //System.out.println("INFO in PredVarInst.hasCorrectArityRecurse() variable arity: " + f);
                return null;
            }
            ArrayList<String> l = f.complexArgumentsToArrayList(1);
            if (l == null) {
                System.out.println("INFO in PredVarInst.hasCorrectArityRecurse() not checked: not a simple clause : " + f);
                return null;
            }
            for (int i = 0; i < l.size(); i++) {
                String arg = l.get(i);
                if (Formula.isFunction(arg)) {
                    String result = hasCorrectArityRecurse(new Formula(arg),kb);
                    if (!StringUtil.emptyString(result))
                        return result;
                }
            }
            int val = 0;
            Integer intval = kb.kbCache.valences.get(rel);
            if (kb.kbCache.valences.get(rel) != null)
                val = intval.intValue();
            //System.out.println("INFO in PredVarInst.hasCorrectArityRecurse(): " + f);
            //System.out.println("INFO in PredVarInst.hasCorrectArityRecurse(): " + l);
            //System.out.println("INFO in PredVarInst.hasCorrectArityRecurse() rel, val,actual: " + rel + "," + val + ": " + l.size());
            //System.out.println("INFO in PredVarInst.hasCorrectArityRecurse() l: " + l);
            
            if (val > 0 && val != l.size()) {
                System.out.println("Error in PredVarInst.hasCorrectArityRecurse() expected arity " +
                                   val + " but found " + l.size() + " for relation " + rel);
                return rel;
            }
        }
        else {
            if (Formula.isQuantifier(f.car()))
                return hasCorrectArityRecurse(f.cddrAsFormula(),kb);
            else {
                String result = hasCorrectArityRecurse(f.carAsFormula(),kb);
                if (!StringUtil.emptyString(result))
                    return result;
                result = hasCorrectArityRecurse(f.cdrAsFormula(),kb);
                if (!StringUtil.emptyString(result))
                    return result;
            }
        }
        return null;
    }
    
    /** ***************************************************************
     * If arity is correct, return null, otherwise, return the predicate
     * that has its arity violated in the given formula.
     */
    public static String hasCorrectArity(Formula f, KB kb) {
        
        return hasCorrectArityRecurse(f,kb);
    }
    
    /** ***************************************************************
     * This method returns an ArrayList of query answer literals.  The
     * first element is an ArrayList of query literals that might be
     * used to simplify the Formula to be instantiated.  The second
     * element is the query literal (ArrayList) that will be used as a
     * template for doing the variable substitutions.  All subsequent
     * elements are ground literals (ArrayLists).
     *
     * @param kb A KB to query for answers.
     *
     * @param queryLits A List of query literals.  The first item in
     * the list will be a SUO-KIF variable (String), which indexes the
     * list.  Each subsequent item is a query literal (List).
     *
     * @return An ArrayList of literals, or an empty ArrayList if no
     * query answers can be found.
     
     private static ArrayList computeSubstitutionTuples(KB kb, List queryLits) {
     
     ArrayList result = new ArrayList();
     if (kb != null && queryLits != null && !queryLits.isEmpty()) {
     String idxVar = (String) queryLits.get(0);
     int i = 0;
     int j = 0;
     
     // Sort the query lits by number of variables.
     ArrayList sortedQLits = new ArrayList(queryLits);
     sortedQLits.remove(0);
     if (sortedQLits.size() > 1) {
     Comparator comp = new Comparator() {
     public int compare(Object o1, Object o2) {
     Integer c1 = Integer.valueOf(getVarCount((List) o1));
     Integer c2 = Integer.valueOf(getVarCount((List) o2));
     return c1.compareTo(c2);
     }
     };
     Collections.sort(sortedQLits, Collections.reverseOrder(comp));
     }
     
     // Put instance literals last.
     List tmplist = new ArrayList(sortedQLits);
     List ioLits = new ArrayList();
     sortedQLits.clear();
     List ql = null;
     for (Iterator iql = tmplist.iterator(); iql.hasNext();) {
     ql = (List) iql.next();
     if (((String)(ql.get(0))).equals("instance"))
     ioLits.add(ql);
     else
     sortedQLits.add(ql);
     }
     sortedQLits.addAll(ioLits);
     
     // Literals that will be used to try to simplify the
     // formula before pred var instantiation.
     ArrayList simplificationLits = new ArrayList();
     
     // The literal that will serve as the pattern for
     // extracting var replacement terms from answer
     // literals.
     List keyLit = null;
     
     // The list of answer literals retrieved using the
     // query lits, possibly built up via a sequence of
     // multiple queries.
     ArrayList answers = null;
     
     Set working = new HashSet();
     ArrayList accumulator = null;
     
     boolean satisfiable = true;
     boolean tryNextQueryLiteral = true;
     
     // The first query lit for which we get an answer is the key lit.
     for (i = 0; (i < sortedQLits.size()) && tryNextQueryLiteral; i++) {
     ql = (List) sortedQLits.get(i);
     accumulator = kb.askWithLiteral(ql);
     satisfiable = ((accumulator != null) && !accumulator.isEmpty());
     tryNextQueryLiteral = (satisfiable || (getVarCount(ql) > 1));
     // !((String)(ql.get(0))).equals("instance")
     if (satisfiable) {
     simplificationLits.add(ql);
     if (keyLit == null) {
     keyLit = ql;
     answers = KB.formulasToArrayLists(accumulator);
     }
     else {  // if (accumulator.size() < answers.size()) {
     accumulator = KB.formulasToArrayLists(accumulator);
     
     // Winnow the answers list.
     working.clear();
     List ql2 = null;
     int varPos = ql.indexOf(idxVar);
     String term = null;
     for (j = 0; j < accumulator.size(); j++) {
     ql2 = (List) accumulator.get(j);
     term = (String) (ql2.get(varPos));
     // if (!term.endsWith("Fn")) {
     working.add(term);
     // }
     }
     accumulator.clear();
     accumulator.addAll(answers);
     answers.clear();
     varPos = keyLit.indexOf(idxVar);
     for (j = 0; j < accumulator.size(); j++) {
     ql2 = (List) accumulator.get(j);
     term = (String) (ql2.get(varPos));
     if (working.contains(term))
     answers.add(ql2);
     }
     }
     }
     }
     if (satisfiable && (keyLit != null)) {
     result.add(simplificationLits);
     result.add(keyLit);
     result.addAll(answers);
     }
     else
     result.clear();
     }
     return result;
     }
     
     /** ***************************************************************
     * This method returns an ArrayList in which each element is
     * another ArrayList.  The head of each element is a variable.
     * The subsequent objects in each element are query literals
     * (ArrayLists).
     *
     * @param kb The KB to use for computing variable type signatures.
     *
     * @param varTypeMap A Map from variables to their types, as
     * explained in the javadoc entry for gatherPredVars(kb)
     *
     * @see Formula.gatherPredVars(KB kb)
     *
     * @return An ArrayList, or null if the input formula contains no
     * predicate variables.
     
     private static ArrayList prepareIndexedQueryLiterals(KB kb, Map varTypeMap) {
     
     ArrayList ans = new ArrayList();
     HashSet<String> varsWithTypes = ((varTypeMap instanceof Map)
     ? varTypeMap
     : gatherPredVars());
     if (!varsWithTypes.isEmpty()) {
     String yOrN = (String) varsWithTypes.get("arg0");
     // If the formula doesn't contain any arg0 pred vars, do nothing.
     if (!StringUtil.emptyString(yOrN) && yOrN.equalsIgnoreCase("yes")) {
     // Try to simplify the formula.
     ArrayList varWithTypes = null;
     ArrayList indexedQueryLits = null;
     
     String var = null;
     for (Iterator it = varsWithTypes.keySet().iterator(); it.hasNext();) {
     var = (String) it.next();
     if (Formula.isVariable(var)) {
     varWithTypes = (ArrayList) varsWithTypes.get(var);
     indexedQueryLits = gatherPredVarQueryLits(kb, varWithTypes);
     if (!indexedQueryLits.isEmpty()) {
     ans.add(indexedQueryLits);
     }
     }
     }
     }
     }
     return ans;
     }
     
     /** ***************************************************************
     * Get a set of all the predicate variables in the formula
     */
    private static HashSet<String> gatherPredVarRecurse(Formula f) {
        
        HashSet<String> ans = new HashSet<String>();
        //System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): " + f);
        if (f == null || f.empty() || Formula.atom(f.theFormula) || f.isVariable())
            return ans;
        if (f.isSimpleClause()) {
            String arg0 = f.getArgument(0);
            //System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): simple clause with: " + arg0);
            if (arg0.startsWith("?")) {
                ArrayList<String> arglist = f.argumentsToArrayList(1);
                if (arglist != null && arglist.size() > 0) {// a variable could be an argument to a higher-order formula
                    //System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): adding: " + arg0 +
                    //        " with arglist: " + arglist);
                    ans.add(arg0);
                    predVarArity.put(arg0,new Integer(arglist.size()));
                }
                else {
                    //System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): not a predicate var: " + arg0);
                }
            }
        }
        else if (Formula.isQuantifier(f.car())) {
            //System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): found quantifier: " + f);
            Formula f2 = f.cddrAsFormula();
            ans.addAll(gatherPredVarRecurse(f2));
        }
        else {
            //System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): not simple or quant: " + f);
            ans.addAll(gatherPredVarRecurse(f.carAsFormula()));
            ans.addAll(gatherPredVarRecurse(f.cdrAsFormula()));
        }
        //System.out.println("INFO in PredVarInst.gatherPredVarRecurse(): returning: " + ans);
        return ans;
    }
    
    /** ***************************************************************
     * Add a key,value pair for a multiple value ArrayList
     
     private static HashMap<String,HashSet<String>>
     addToArrayList(HashMap<String,HashSet<String>> ar, String key, String value) {
     
     HashSet<String> val = ar.get(key);
     if (val == null)
     val = new HashSet<String>();
     val.add(value);
     ar.put(key, val);
     return ar;
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
    static HashMap<String, HashSet<String>> findPredVarTypes(Formula f, KB kb) {
        
        HashSet<String> predVars = gatherPredVars(f);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        HashMap<String,HashSet<String>> typeMap = fp.computeVariableTypes(f,kb);
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
     * This method collects and returns all predicate variables
     *
     * @param kb The KB to be used for computations involving
     * assertions.
     */
    protected static HashSet<String> gatherPredVars(Formula f) {
        
        HashSet<String> varlist = null;
        HashMap<String,HashSet<String>> ans = new HashMap<String,HashSet<String>>();
        if (!StringUtil.emptyString(f.theFormula)) {
            varlist = gatherPredVarRecurse(f);
        }
        return varlist;
    }
    
    /** ***************************************************************
     * This method tries to remove literals from the Formula that
     * match litArr.  It is intended for use in simplification of this
     * Formula during predicate variable instantiation, and so only
     * attempts removals that are likely to be safe in that context.
     *
     * @param litArr A List object representing a SUO-KIF atomic
     * formula.
     *
     * @return A new Formula with at least some occurrences of litF
     * removed, or the original Formula if no removals are possible.
     
     private static Formula maybeRemoveMatchingLits(List litArr) {
     Formula f = KB.literalListToFormula(litArr);
     return maybeRemoveMatchingLits(_f,f);
     }
     
     /** ***************************************************************
     * This method tries to remove literals from the Formula that
     * match litF.  It is intended for use in simplification of this
     * Formula during predicate variable instantiation, and so only
     * attempts removals that are likely to be safe in that context.
     *
     * @param litF A SUO-KIF literal (atomic Formula).
     *
     * @return A new Formula with at least some occurrences of litF
     * removed, or the original Formula if no removals are possible.
     
     private static Formula maybeRemoveMatchingLits(Formula input, Formula litF) {
     
     Formula result = null;
     Formula f = input;
     if (f.listP() && !f.empty()) {
     StringBuilder litBuf = new StringBuilder();
     String arg0 = f.car();
     if (Arrays.asList(Formula.IF, Formula.IFF).contains(arg0)) {
     String arg1 = f.getArgument(1);
     String arg2 = f.getArgument(2);
     if (arg1.equals(litF.theFormula)) {
     Formula arg2F = new Formula();
     arg2F.read(arg2);
     litBuf.append(maybeRemoveMatchingLits(arg2F,litF).theFormula);
     }
     else if (arg2.equals(litF.theFormula)) {
     Formula arg1F = new Formula();
     arg1F.read(arg1);
     litBuf.append(maybeRemoveMatchingLits(arg1F,litF).theFormula);
     }
     else {
     Formula arg1F = new Formula();
     arg1F.read(arg1);
     Formula arg2F = new Formula();
     arg2F.read(arg2);
     litBuf.append("(" + arg0 + " "
     + maybeRemoveMatchingLits(arg1F,litF).theFormula + " "
     + maybeRemoveMatchingLits(arg2F,litF).theFormula + ")");
     }
     }
     else if (Formula.isQuantifier(arg0)
     || arg0.equals("holdsDuring")
     || arg0.equals("KappaFn")) {
     Formula arg2F = new Formula();
     arg2F.read(f.caddr());
     litBuf.append("(" + arg0 + " " + f.cadr() + " "
     + maybeRemoveMatchingLits(arg2F,litF).theFormula + ")");
     }
     else if (Formula.isCommutative(arg0)) {
     List litArr = f.literalToArrayList();
     if (litArr.contains(litF.theFormula))
     litArr.remove(litF.theFormula);
     String args = "";
     int len = litArr.size();
     for (int i = 1 ; i < len ; i++) {
     Formula argF = new Formula();
     argF.read((String) litArr.get(i));
     args += (" " + maybeRemoveMatchingLits(argF,litF).theFormula);
     }
     if (len > 2)
     args = ("(" + arg0 + args + ")");
     else
     args = args.trim();
     litBuf.append(args);
     }
     else {
     litBuf.append(f.theFormula);
     }
     Formula newF = new Formula();
     newF.read(litBuf.toString());
     result = newF;
     }
     if (result == null)
     result = input;
     return result;
     }
     
     /** ***************************************************************
     * Return true if the input predicate can take relation names a
     * arguments, else returns false.
     
     private static boolean isPossibleRelnArgQueryPred (KB kb, String predicate) {
     
     ArrayList<String> sig = kb.kbCache.signatures.get(predicate);
     for (int i = 1; i < sig.size(); i++) {
     String argType = sig.get(i);
     if (!argType.endsWith("+")) {   // domainSubclass
     HashSet<String> prents = kb.kbCache.getParentClasses(argType);
     if (prents != null && prents.contains("Relation"))
     return true;
     }
     }
     return false;
     }
     
     /** ***************************************************************
     * This method collects and returns literals likely to be of use
     * as templates for retrieving predicates to be substituted for
     * var.
     *
     * @param varWithTypes A List containing a variable followed,
     * optionally, by class names indicating the type of the variable.
     *
     * @return An ArrayList of literals (Lists) with var at the head.
     * The first element of the ArrayList is the variable (String).
     * Subsequent elements are Lists corresponding to SUO-KIF
     * formulas, which will be used as query templates.
     
     private static ArrayList gatherPredVarQueryLits(KB kb, List varWithTypes) {
     
     ArrayList ans = new ArrayList();
     String var = (String) varWithTypes.get(0);
     Set added = new HashSet();
     
     // Get the clauses for this Formula.
     List clauses = _f.getClauses();
     Map varMap = _f.getVarMap();
     String qlString = null;
     ArrayList queryLit = null;
     
     if (clauses != null) {
     Iterator it2 = null;
     Formula f = null;
     Iterator it1 = clauses.iterator();
     while (it1.hasNext()) {
     List clause = (List) it1.next();
     List negLits = (List) clause.get(0);
     // List poslits = (List) clause.get(1);
     if (!negLits.isEmpty()) {
     int flen = -1;
     String arg = null;
     String arg0 = null;
     String term = null;
     String origVar = null;
     List lit = null;
     boolean working = true;
     for (int ci = 0;
     ci < 1;
     // (ci < clause.size()) && ans.isEmpty();
     ci++) {
     // Try the neglits first.  Then try the poslits only if there still are no resuls.
     lit = (List)(clause.get(ci));
     it2 = lit.iterator();
     // System.out.println("  lit == " + lit);
     while (it2.hasNext()) {
     f = (Formula) it2.next();
     if (f.theFormula.matches(".*SkFn\\s+\\d+.*")
     || f.theFormula.matches(".*Sk\\d+.*"))
     continue;
     flen = f.listLength();
     arg0 = f.getArgument(0);
     // System.out.println("  var == " + var + "\n  f.theFormula == " + f.theFormula + "\n  arg0 == " + arg0);
     if (!StringUtil.emptyString(arg0)) {
     // If arg0 corresponds to var, then var has to be of type Predicate, not of
     // types Function or List.
     if (Formula.isVariable(arg0)) {
     origVar = Clausifier.getOriginalVar(arg0, varMap);
     if (origVar.equals(var)
     && !varWithTypes.contains("Predicate")) {
     varWithTypes.add("Predicate");
     }
     }
     else {
     queryLit = new ArrayList();
     queryLit.add(arg0);
     boolean foundVar = false;
     for (int i = 1; i < flen; i++) {
     arg = f.getArgument(i);
     if (!Formula.listP(arg)) {
     if (Formula.isVariable(arg)) {
     arg = Clausifier.getOriginalVar(arg, varMap);
     if (arg.equals(var))
     foundVar = true;
     }
     queryLit.add(arg);
     }
     }
     // System.out.println("  arg0 == " + arg0 + "\n  queryLit == " + queryLit);
     if (queryLit.size() != flen)
     continue;
     // If the literal does not start with a variable or with "holds" and does not
     // contain Skolem terms, but does contain the variable in which we're interested,
     // it is probably suitable as a query template, or might serve as a starting
     // place.  Use it, or a literal obtained with it.
     if (isPossibleRelnArgQueryPred(kb, arg0) && foundVar) {
     // || arg0.equals("disjoint"))
     term = "";
     if (queryLit.size() > 2)
     term = (String) queryLit.get(2);
     if (!(arg0.equals("instance")
     && term.equals("Relation"))) {
     String queryLitStr = queryLit.toString().intern();
     if (!added.contains(queryLitStr)) {
     ans.add(queryLit);
     // System.out.println("  queryLitStr == " + queryLitStr);
     added.add(queryLitStr);
     }
     }
     }
     }
     }
     }
     }
     }
     }
     }
     
     // If we have previously collected type info for the variable,
     // convert that info query lits now.
     String argType = null;
     int vtLen = varWithTypes.size();
     if (vtLen > 1) {
     for (int j = 1 ; j < vtLen ; j++) {
     argType = (String) varWithTypes.get(j);
     if (!argType.equals("Relation")) {
     queryLit = new ArrayList();
     queryLit.add("instance");
     queryLit.add(var);
     queryLit.add(argType);
     qlString = queryLit.toString().intern();
     if (!added.contains(qlString)) {
     ans.add(queryLit);
     added.add(qlString);
     }
     }
     }
     }
     // Add the variable to the front of the answer list, if it contains
     // any query literals.
     if (!ans.isEmpty())
     ans.add(0, var);
     return ans;
     }
     
     /** ***************************************************************
     */
    public static void arityTest() {
        
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB("SUMO");
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
        KB kb = KBmanager.getMgr().getKB("SUMO");
        System.out.println("INFO in PredVarInst.test(): completed loading KBs");
        if (kb.kbCache.transInstOf("exhaustiveAttribute","VariableArityRelation")) {
            System.out.println("INFO in PredVarInst.test() variable arity: ");
        }
        else
            System.out.println("INFO in PredVarInst.test() not variable arity: ");
        System.out.println("INFO in PredVarInst.test(): " + kb.kbCache.instances.get("partition"));
        System.out.println("INFO in PredVarInst.test(): " + kb.kbCache.insts.contains("partition"));
        //String formStr = "(=> (inverse ?REL1 ?REL2) (forall (?INST1 ?INST2) (<=> (?REL1 ?INST1 ?INST2) (?REL2 ?INST2 ?INST1))))";
        String formStr = "(<=> (instance ?REL TransitiveRelation) " +
        "(forall (?INST1 ?INST2 ?INST3) " +
        "(=> (and (?REL ?INST1 ?INST2) " +
        "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";
        //Formula f = kb.formulaMap.get(formStr);
        Formula f = new Formula(formStr);
        System.out.println("Formula: " + f);
        System.out.println("Pred vars: " + gatherPredVars(f));
        System.out.println("Pred vars with types: " + findPredVarTypes(f,kb));
        FormulaPreprocessor fp = new FormulaPreprocessor();
        System.out.println("Explicit types: " + fp.findExplicitTypesInAntecedent(f));
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
        System.out.println("Pred vars: " + gatherPredVars(f));
        System.out.println("Instantiated: " + instantiatePredVars(f,kb));
        
    }
    
    /** ***************************************************************
     */
    public static void main(String[] args) {
        
        //arityTest();
        test();
        /*
         KBmanager.getMgr().initializeOnce();
         KB kb = KBmanager.getMgr().getKB("SUMO");
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
