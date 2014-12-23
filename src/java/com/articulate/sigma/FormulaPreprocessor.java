package com.articulate.sigma;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormulaPreprocessor {

    /** ***************************************************************
     * For any given formula, stop generating new pred var
     * instantiations and row var expansions if this threshold value
     * has been exceeded.  The default value is 2000.
     */
    private static final int AXIOM_EXPANSION_LIMIT = 2000;
    
    private static boolean debug = false;
    
    /** ***************************************************************
     * A + is appended to the type if the parameter must be a class
     *
     * @return the type for each argument to the given predicate, where
     * ArrayList element 0 is the result, if a function, 1 is the
     * first argument, 2 is the second etc.
     */
    private ArrayList<String> getTypeList(String pred, KB kb) {

        return kb.kbCache.signatures.get(pred);
    }

    /** ***************************************************************
     * Find the argument type restriction for a given predicate and
     * argument number that is inherited from one of its
     * super-relations.  A "+" is appended to the type if the
     * parameter must be a class.  Argument number 0 is used for the
     * return type of a Function.  Asking for a non-existent arg
     * will return null;
     */
    public static String findType(int numarg, String pred, KB kb) {

        ArrayList<String> sig = kb.kbCache.signatures.get(pred);
        if (sig == null) {
        	if (!kb.isInstanceOf(pred, "VariableArityRelation"))
        		System.out.println("Error in FormulaPreprocessor.findType(): no type information for predicate " + pred);
            return null;
        }
        if (numarg >= sig.size())
        	return null;
        
        return sig.get(numarg);
    }

    /** ***************************************************************
     * This method tries to remove all but the most specific relevant
     * classes from a List of sortal classes.
     *
     * @param types A List of classes (class name Strings) that
     * constrain the value of a SUO-KIF variable.
     *
     * @param kb The KB used to determine if any of the classes in the
     * List types are redundant.
     *
     * @return void
     */
    private void winnowTypeList(HashSet<String> types, KB kb) {

        long t1 = 0L;
        if (types.size() > 1) {
            Object[] valArr = types.toArray();
            String clX = null;
            String clY = null;
            for (int i = 0; i < valArr.length; i++) {
                boolean stop = false;
                for (int j = 0; j < valArr.length; j++) {
                    if (i != j) {
                        clX = (String) valArr[i];
                        clY = (String) valArr[j];
                        if (clX.equals(clY) || kb.isSubclass(clX, clY)) {
                            types.remove(clY);
                            if (types.size() < 2) {
                                stop = true;
                                break;
                            }
                        }
                    }
                }
                if (stop) break;
            }
        }
        return;
    }

    /** ***************************************************************
     * Add clauses for every variable in the antecedent to restrict its
     * type to the type restrictions defined on every relation in which
     * it appears.  For example
     * (=>
     *   (foo ?A B)
     *   (bar B ?A))
     *
     * (domain foo 1 Z)
     *
     * would result in
     *
     * (=>
     *   (instance ?A Z)
     *   (=>
     *     (foo ?A B)
     *     (bar B ?A)))
     */
    private Formula addTypeRestrictionsNew(Formula form, KB kb) {

    	//System.out.println("INFO in FormulaPreprocessor.addTypeRestrictionsNew()");
        String result = form.theFormula;
        Formula f = new Formula();
        f.read(result);
        FormulaPreprocessor fp = new FormulaPreprocessor();

    	//System.out.println("INFO in FormulaPreprocessor.addTypeRestrictionsNew(): before computer variable types");
        HashMap<String,HashSet<String>> varmap = fp.computeVariableTypes(f,kb);
    	//System.out.println("INFO in FormulaPreprocessor.addTypeRestrictionsNew(): variable types: \n" + varmap);
        StringBuffer sb = new StringBuffer();
        if (varmap.size() > 0) {
	        sb.append("(=> (and ");
	        Iterator<String> it = varmap.keySet().iterator();
	        while (it.hasNext()) {
	        	String var = it.next();	        
	        	HashSet<String> types = varmap.get(var);
	       		HashSet<String> insts = new HashSet<String>();
	       		HashSet<String> classes = new HashSet<String>();
	       		Iterator<String> it2 = types.iterator();
	       		while (it2.hasNext()) {
	       		    String type = it2.next();
	       		    if (type.endsWith("+")) 
	       		        classes.add(type.substring(0,type.length()-1));	       		    
	       		    else
	       		        insts.add(type);
	       		}
	       		if (insts != null) {
		       		winnowTypeList(insts,kb);
		       		Iterator<String> it3 = insts.iterator();
		       		while (it3.hasNext()) {
		       		    String s = it3.next();
	                     sb.append("(instance " + var + " " + s + ")");
		       		}
	       		}
	       		if (classes != null) {
		       		winnowTypeList(classes,kb);
	                Iterator<String> it3 = classes.iterator();
	                while (it3.hasNext()) {
	                    String s = it3.next();	                
	                    sb.append("(subclass " + var + " " + s + ")");
		       		}
	       		}
	        }
	        sb.append(") " + f + ")");
        }        	
        f.read(sb.toString());
        //if (debug) System.out.println("INFO in FormulaPreprocessor.addTypeRestrictionsNew(): \n" + f);
        return f;
    }
    
    /***************************************************************** 
     * This method returns a HashMap that maps each String variable in this the names
     * of types (classes) of which the variable must be an instance or the names
     * of types of which the variable must be a subclass. If the only instance
     * or subclass sortal that can be computed for a variable is Entity, the
     * lists will be empty.  Note that this does not capture explicit type
     * assertions such as (=> (instance ?Foo Bar) ...) just the restrictions
     * implicit from the arg types of relations.
     * 
     * @param kb is the KB used to compute the sortal constraints for each
     *            variable.
     * @return A HashMap of variable names and their types. Subclass
     *         restrictions are marked with a '+'. Instance restrictions have no
     *         special mark.
     */
    public HashMap<String,HashSet<String>> computeVariableTypes(Formula form, KB kb) {

        if (debug) System.out.println("INFO in FormulaPreprocessor.computeVariableTypes(): \n" + form);
        Formula f = new Formula();
        f.read(form.theFormula);
        //f.read(_f.makeQuantifiersExplicit(false));
        FormulaPreprocessor fp = new FormulaPreprocessor();
        HashMap<String,HashSet<String>> result = new HashMap<String,HashSet<String>>();
        return computeVariableTypesRecurse(kb,form,result);
    }
    
    /***************************************************************** 
     * Collect the types of any variables that are specifically defined
     * in the antecedent of a rule with an instance or subclass expression.
     * TODO: This may ultimately require CNF conversion and then checking negative
     * literals, but for now it's just a hack to grab preconditions.
     */
    public HashMap<String,HashSet<String>> findExplicitTypes(Formula form) {

        if (debug) System.out.println("INFO in FormulaPreprocessor.findExplicitTypes(): \n" + form);
        if (!form.isRule())
            return null;
        Formula f = new Formula();
        f.read(form.theFormula);
        HashMap<String,HashSet<String>> result = new HashMap<String,HashSet<String>>();
        Formula antecedent = f.cdrAsFormula().carAsFormula();
        Pattern p = Pattern.compile("\\(instance (\\?[a-zA-Z0-9]+) ([a-zA-Z0-9\\-_]+)");
        Matcher m = p.matcher(antecedent.theFormula);
        while (m.find()) {
            String var = m.group(1);
            String cl = m.group(2);
            HashSet<String> hs = new HashSet<String>();
            if (result.containsKey(var))
                hs = result.get(var);
            hs.add(cl);
            result.put(var, hs);
        }
        
        p = Pattern.compile("\\(subclass (\\?[a-zA-Z0-9]+) ([a-zA-Z0-9\\-]+)");
        m = p.matcher(f.theFormula);
        while (m.find()) {
            String var = m.group(1);
            String cl = m.group(2);
            HashSet<String> hs = new HashSet<String>();
            if (result.containsKey(var))
                hs = result.get(var);
            hs.add(cl + "+");
            result.put(var, hs);
        }
        return result;
    }
    
    /** ***************************************************************
     * utility method to add a String element to a HashMap of String
     * keys and a value of an HashSet of Strings
     */
    private static void addToMap(HashMap<String,HashSet<String>> map, String key, String element) {
        
        HashSet<String> al = map.get(key);
        if (al == null)
            al = new HashSet<String>();
        al.add(element);
        map.put(key, al);
    }
    
    /** ***************************************************************
     * utility method to merge two HashMaps of String
     * keys and a values of an HashSet of Strings
     */
    private static HashMap<String,HashSet<String>> mergeToMap(HashMap<String,HashSet<String>> map1, 
            HashMap<String,HashSet<String>> map2) {
        
        HashMap<String,HashSet<String>> result = new HashMap<String,HashSet<String>>();
        result.putAll(map1);
        Iterator<String> it = map2.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            HashSet<String> value = new HashSet<String>();
            if (result.containsKey(key)) {
                value = result.get(key);
            }
            value.addAll(map2.get(key));
            result.put(key, value);
        }
        return result;
    }
    
    /** ***************************************************************
     */
    public HashMap<String,HashSet<String>> computeVariableTypesRecurse(KB kb, Formula f,
            HashMap<String,HashSet<String>> input) {

        HashMap<String,HashSet<String>> result = new HashMap<String,HashSet<String>>();
    	if (f == null || StringUtil.emptyString(f.theFormula) || f.empty())
    		return result;
        //System.out.println("INFO in FormulaPreprocessor.computeVariableTypesRecurse(): \n" + f);
        String carstr = f.car();
        if (Formula.atom(carstr) && Formula.isLogicalOperator(carstr)) {// equals may require special treatment
            result.putAll(input);
    		for (int i = 1; i < f.listLength(); i++) 
    			result = mergeToMap(result,computeVariableTypesRecurse(kb,new Formula(f.getArgument(i)),input));        		
        }
        else if (f.isSimpleClause()) {
            String pred = carstr;
        	if (f.theFormula.indexOf("?") > -1 && !Formula.isVariable(pred)) {	        	
	        	Formula newf = f.cdrAsFormula();
	        	int argnum = 1;
	        	while (!newf.empty()) {
	        		String arg = newf.car();
	        		if (Formula.isVariable(arg)) {
	        			String cl = findType(argnum,pred,kb);
	        			//System.out.println("arg,pred,argnum,type: " + arg + ", " + pred + ", " + argnum + ", " + cl);
	        			if (StringUtil.emptyString(cl)) {
	        				if (!kb.kbCache.transInstOf(pred, "VariableArityRelation"))
	        					System.out.println("Error in FormulaPreprocessor.computeVariableTypesRecurse(): no type information for arg " +
	        						argnum + " of relation " + pred + " in formula: \n" + f);
	        			}
	        			else
	        			    addToMap(result,arg,cl);	        			
	        		}
	        		newf = newf.cdrAsFormula();
	        		argnum++;  // note that this will try an argument that doesn't exist, and terminate when it does
	        	}	
        	}
        }
        else {
        	result = mergeToMap(input,computeVariableTypesRecurse(kb,f.carAsFormula(),input));
        	result = mergeToMap(result,computeVariableTypesRecurse(kb,f.cdrAsFormula(),input));
        }        	
        //System.out.println("result: " + result);
        return result;
    }
    
    /** ***************************************************************
     * Pre-process a formula before sending it to the theorem prover. This includes
     * ignoring meta-knowledge like documentation strings, translating
     * mathematical operators, quoting higher-order formulas, expanding
     * row variables and prepending the 'holds__' predicate.
     * @return an ArrayList of Formula(s)
     */
    private String preProcessRecurse(Formula f, String previousPred, boolean ignoreStrings,
                                     boolean translateIneq, boolean translateMath) {

        StringBuilder result = new StringBuilder();
        if (f.listP() && !f.empty()) {
            String prefix = "";
            String pred = f.car();
            if (Formula.isQuantifier(pred)) {
                // The list of quantified variables.
                result.append(" ");
                result.append(f.cadr());
                // The formula following the list of variables.
                String next = f.caddr();
                Formula nextF = new Formula();
                nextF.read(next);
                result.append(" ");
                result.append(preProcessRecurse(nextF,"",ignoreStrings,translateIneq,translateMath));
            }
            else {
                Formula restF = f.cdrAsFormula();
                int argCount = 1;
                while (!restF.empty()) {
                    argCount++;
                    String arg = restF.car();
                    Formula argF = new Formula();
                    argF.read(arg);
                    if (argF.listP()) {
                        String res = preProcessRecurse(argF,pred,ignoreStrings,translateIneq,translateMath);
                        result.append(" ");
                        if (!Formula.isLogicalOperator(pred) &&
                            !Formula.isComparisonOperator(pred) &&
                            !Formula.isMathFunction(pred) &&
                            !argF.isFunctionalTerm()) {
                            result.append("`");
                        }
                        result.append(res);
                    }
                    else
                        result.append(" " + arg);
                    restF.theFormula = restF.cdr();
                }
                if (KBmanager.getMgr().getPref("holdsPrefix").equals("yes")) {
                    if (!Formula.isLogicalOperator(pred) && !Formula.isQuantifierList(pred,previousPred))
                        prefix = "holds_";
                    if (f.isFunctionalTerm())
                        prefix = "apply_";
                    if (pred.equals("holds")) {
                        pred = "";
                        argCount--;
                        prefix = prefix + argCount + "__ ";
                    }
                    else {
                        if (!Formula.isLogicalOperator(pred) &&
                            !Formula.isQuantifierList(pred,previousPred) &&
                            !Formula.isMathFunction(pred) &&
                            !Formula.isComparisonOperator(pred)) {
                            prefix = prefix + argCount + "__ ";
                        }
                        else
                            prefix = "";
                    }
                }
            }
            result.insert(0, pred);
            result.insert(0, prefix);
            result.insert(0, "(");
            result.append(")");
        }
        return result.toString();
    }

    /** ***************************************************************
     * Tries to successively instantiate predicate variables and then
     * expand row variables in this Formula, looping until no new
     * Formulae are generated.
     *
     * @param kb The KB to be used for processing this Formula
     *
     * @param addHoldsPrefix If true, predicate variables are not
     * instantiated
     *
     * @return an ArrayList of Formula(s), which could be empty.
     */
    private ArrayList<Formula> replacePredVarsAndRowVars(Formula form, KB kb, boolean addHoldsPrefix) {

        ArrayList<Formula> result = new ArrayList<Formula>();
        Formula startF = new Formula();
        startF.read(form.theFormula);
        LinkedHashSet<Formula> accumulator = new LinkedHashSet<Formula>();
        accumulator.add(startF);
        ArrayList<Formula> working = new ArrayList<Formula>();
        int prevAccumulatorSize = 0;
        Formula f = null;
        while (accumulator.size() != prevAccumulatorSize) {
            prevAccumulatorSize = accumulator.size();
            // Do pred var instantiations if we are not adding holds prefixes.
            if (!addHoldsPrefix) {
                working.clear();
                working.addAll(accumulator);
                accumulator.clear();
                Iterator<Formula> it = working.iterator(); 
                while (it.hasNext()) {
                    f = (Formula) it.next();
                    List<Formula> instantiations = PredVarInst.instantiatePredVars(f,kb);
                    form.errors.addAll(f.getErrors());
                    if (instantiations.isEmpty()) {
                        // If the accumulator is empty -- no pred var instantiations were possible -- add
                        // the original formula to the accumulator for possible row var expansion below.
                        accumulator.add(f);
                    }
                    else {
                        // If the formula can't be instantiated at all and so has been marked "reject",
                        // don't add anything.
                        Formula obj0 = instantiations.get(0);
                        String errStr = "No predicate instantiations for ";
                        if (instantiations == null || instantiations.size() == 0) {
                            errStr += f.theFormula;
                            form.errors.add(errStr);
                        }
                        else {
                            // It might not be possible to instantiate all pred vars until
                            // after row vars have been expanded, so we loop until no new formulae
                            // are being generated.
                            accumulator.addAll(instantiations);
                        }
                    }
                }
            }
            // Row var expansion. Iterate over the instantiated predicate formulas,
            // doing row var expansion on each.  If no predicate instantiations can be generated, the accumulator
            // will contain just the original input formula.
            if (!accumulator.isEmpty() && (accumulator.size() < AXIOM_EXPANSION_LIMIT)) {
                working.clear();
                working.addAll(accumulator);
                accumulator.clear();
                Iterator<Formula> it2 = working.iterator(); 
                while (it2.hasNext()) {
                    f = (Formula) it2.next();
                    RowVars rv = new RowVars();
                    accumulator.addAll(RowVars.expandRowVars(kb,f));
                    if (accumulator.size() > AXIOM_EXPANSION_LIMIT) {
                        System.out.println("  AXIOM_EXPANSION_LIMIT EXCEEDED: " + AXIOM_EXPANSION_LIMIT);
                        break;
                    }
                }
            }
        }
        result.addAll(accumulator);
        return result;
    }
    
    /** ***************************************************************
     * Returns true if this Formula appears not to have any of the
     * characteristics that would cause it to be rejected during
     * translation to TPTP form, or cause problems during inference.
     * Otherwise, returns false.
     *
     * @param query true if this Formula represents a query, else
     * false.
     *
     * @param kb The KB object to be used for evaluating the
     * suitability of this Formula.
     *
     * @return boolean
     */
    private static boolean isOkForInference(Formula f, boolean query, KB kb) {

        boolean pass = false;
        // kb isn't used yet, because the checks below are purely
        // syntactic.  But it probably will be used in the future.
            pass = !(// (equal ?X ?Y ?Z ...) - equal is strictly binary.
                     // No longer necessary?  NS: 2009-06-12
                     // this.theFormula.matches(".*\\(\\s*equal\\s+\\?*\\w+\\s+\\?*\\w+\\s+\\?*\\w+.*")

                     // The formula contains non-ASCII characters.
                     // was: this.theFormula.matches(".*[\\x7F-\\xFF].*")
                     // ||
                     StringUtil.containsNonAsciiChars(f.theFormula)

                     // (<relation> ?X ...) - no free variables in an
                     // atomic formula that doesn't contain a string
                     // unless the formula is a query.
                     || (!query
                         && !Formula.isLogicalOperator(f.car())
                         // The formula does not contain a string.
                         && (f.theFormula.indexOf('"') == -1)
                         // The formula contains a free variable.
                         && f.theFormula.matches(".*\\?\\w+.*"))

                     // ... add more patterns here, as needed.
                     || false
                     );
        return pass;
    }

    /** ***************************************************************
     * Adds statements of the form (instance <Entity> <SetOrClass>) if
     * they are not already in the KB.
     *
     * @param kb The KB to be used for processing the input Formulae
     * in variableReplacements
     *
     * @param isQuery If true, this method just returns the initial
     * input List, variableReplacements, with no additions
     *
     * @param variableReplacements A List of Formulae in which
     * predicate variables and row variables have already been
     * replaced, and to which (instance <Entity> <SetOrClass>)
     * Formulae might be added
     *
     * @return an ArrayList of Formula(s), which could be larger than
     * the input List, variableReplacements, or could be empty.
     */
    private ArrayList<Formula> addInstancesOfSetOrClass(Formula form, KB kb,
    		boolean isQuery, ArrayList<Formula> variableReplacements) {

        ArrayList<Formula> result = new ArrayList<Formula>();
        if ((variableReplacements != null) && !variableReplacements.isEmpty()) {
            if (isQuery)
                result.addAll(variableReplacements);
            else {
                HashSet<Formula> formulae = new HashSet<Formula>();
                String arg0 = null;
                Formula f = null;
                for (Iterator<Formula> it = variableReplacements.iterator(); it.hasNext();) {
                    f = it.next();
                    formulae.add(f);                    
                    if (f.listP() && !f.empty()) {  // Make sure every SetOrClass is stated to be such
                        arg0 = f.car();
                        int start = -1;
                        if (arg0.equals("subclass")) start = 0;
                        else if (arg0.equals("instance")) start = 1;
                        if (start > -1) {
                            ArrayList<String> args = 
                                    new ArrayList<String>(Arrays.asList(f.getArgument(1),f.getArgument(2)));
                            int argslen = args.size();
                            String ioStr = null;
                            Formula ioF = null;
                            String arg = null;
                            for (int i = start; i < argslen; i++) {
                                arg = args.get(i);
                                if (!Formula.isVariable(arg) && !arg.equals("SetOrClass") && Formula.atom(arg)) {
                                    StringBuilder sb = new StringBuilder();
                                    sb.setLength(0);
                                    sb.append("(instance ");
                                    sb.append(arg);
                                    sb.append(" SetOrClass)");
                                    ioF = new Formula();
                                    ioStr = sb.toString().intern();
                                    ioF.read(ioStr);
                                    ioF.sourceFile = form.sourceFile;
                                    if (!kb.formulaMap.containsKey(ioStr)) {
                                        //Map<String,Object> stc = kb.kbCache.getSortalTypeCache();
                                        //if (stc.get(ioStr) == null) {
                                            //stc.put(ioStr, ioStr);
                                            formulae.add(ioF);
                                        //}
                                    }
                                }
                            }
                        }
                    }
                }
                result.addAll(formulae);
            }
        }
        return result;
    }

    /** ***************************************************************
     * Pre-process a formula before sending it to the theorem
     * prover. This includes ignoring meta-knowledge like
     * documentation strings, translating mathematical operators,
     * quoting higher-order formulas, expanding row variables and
     * prepending the 'holds__' predicate.
     *
     * @param isQuery If true the Formula is a query and should be
     *                existentially quantified, else the Formula is a
     *                statement and should be universally quantified
     *
     * @param kb The KB to be used for processing this Formula
     *
     * @return an ArrayList of Formula(s), which could be empty.
     *
     */
    public ArrayList<Formula> preProcess(Formula form, boolean isQuery, KB kb) {

        if (isQuery) System.out.println("INFO in FormulaPreprocessor.preProcess(): input: " + form.theFormula);
        ArrayList<Formula> results = new ArrayList<Formula>();
        if (!StringUtil.emptyString(form.theFormula)) {
            KBmanager mgr = KBmanager.getMgr();
            if (!form.isBalancedList()) {
                String errStr = "Unbalanced parentheses or quotes in: " + form.theFormula;
                form.errors.add(errStr);
                return results;
            }
            boolean ignoreStrings = false;
            boolean translateIneq = true;
            boolean translateMath = true;
            Formula f = new Formula();
            f.read(form.theFormula);
            if (StringUtil.containsNonAsciiChars(f.theFormula))
                f.theFormula = StringUtil.replaceNonAsciiChars(f.theFormula);

            boolean addHoldsPrefix = mgr.getPref("holdsPrefix").equalsIgnoreCase("yes");
            ArrayList<Formula> variableReplacements = replacePredVarsAndRowVars(form,kb, addHoldsPrefix);
            form.errors.addAll(f.getErrors());

            ArrayList<Formula> accumulator = addInstancesOfSetOrClass(form,kb, isQuery, variableReplacements);
            // Iterate over the formulae resulting from predicate variable instantiation and row variable expansion,
            // passing each to preProcessRecurse for further processing.
            if (!accumulator.isEmpty()) {
                //boolean addSortals = mgr.getPref("typePrefix").equalsIgnoreCase("yes");
                Formula fnew = null;
                String theNewFormula = null;
                Iterator<Formula> it = accumulator.iterator(); 
                while (it.hasNext()) {
                    fnew = (Formula) it.next();
                    FormulaPreprocessor fp = new FormulaPreprocessor();
                    theNewFormula = fp.preProcessRecurse(fnew,"",ignoreStrings,translateIneq,translateMath);
                    fnew.read(theNewFormula);
                    form.errors.addAll(fnew.getErrors());                        
                    if (isOkForInference(fnew,isQuery, kb)) {
                        fnew.sourceFile = form.sourceFile;
                        if (!StringUtil.emptyString(theNewFormula))
                        	results.add(fnew);
                    }
                    else 
                    	form.errors.add("Formula rejected for inference: " + f.theFormula);                        
                }
            }
        }
        if (debug) System.out.println("INFO in FormulaPreprocessor.preProcess(): result: " + results);
        Iterator<Formula> it = results.iterator();
        while (it.hasNext()) {
        	Formula f = it.next();
            FormulaPreprocessor fp = new FormulaPreprocessor();
            f.read(fp.addTypeRestrictionsNew(f,kb).theFormula);
        }
        if (debug) System.out.println("INFO in FormulaPreprocessor.preProcess(): result: " + results);
        if (isQuery) System.out.println("INFO in FormulaPreprocessor.preProcess(): result: " + results);
        return results;
    }

    /** ***************************************************************
     */
    public static void testFindTypes() {
       
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB("SUMO");
        
        System.out.println();
        String strf = "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) " +
                "(element ?ELEMENT ?SET2))) (equal ?SET1 ?SET2))";
        Formula f = new Formula();
        f.read(strf);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        System.out.println("Formula: " + f);
        System.out.println("Var types: " + fp.computeVariableTypes(f,kb));
               
        System.out.println();
        strf = "(=> (and (attribute ?AREA LowTerrain) (part ?ZONE ?AREA)" + 
                " (slopeGradient ?ZONE ?SLOPE)) (greaterThan 0.03 ?SLOPE))";
        f.read(strf);
        fp = new FormulaPreprocessor();
        System.out.println("Formula: " + f);
        System.out.println("Var types: " + fp.computeVariableTypes(f,kb));
        
        System.out.println();
        strf = "(=> (and (typicalPart ?PART ?WHOLE) (instance ?X ?PART) " +
                "(equal ?PARTPROB (ProbabilityFn (exists (?Y) (and " +
                "(instance ?Y ?WHOLE) (part ?X ?Y))))) (equal ?NOTPARTPROB " +
                "(ProbabilityFn (not (exists (?Z) (and (instance ?Z ?WHOLE) " +
                "(part ?X ?Z))))))) (greaterThan ?PARTPROB ?NOTPARTPROB))";
        f.read(strf);
        fp = new FormulaPreprocessor();        
        System.out.println("Formula: " + f);
        System.out.println("Var types: " + fp.computeVariableTypes(f,kb));
        
        System.out.println();
        strf = "(<=> (instance ?REL TransitiveRelation) " +
                "(forall (?INST1 ?INST2 ?INST3) " + 
                "(=> (and (?REL ?INST1 ?INST2) " +
                "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";
        f.read(strf);
        fp = new FormulaPreprocessor();        
        System.out.println("Formula: " + f);
        System.out.println("Var types: " + fp.computeVariableTypes(f,kb));
        System.out.println("Explicit types: " + fp.findExplicitTypes(f));        
    }
    
    /** ***************************************************************
     */
    public static void testFindExplicit() {
       
        String formStr = "(<=> (instance ?REL TransitiveRelation) " +
                "(forall (?INST1 ?INST2 ?INST3) " + 
                "(=> (and (?REL ?INST1 ?INST2) " +
                "(?REL ?INST2 ?INST3)) (?REL ?INST1 ?INST3))))";
        Formula f = new Formula(formStr);
        FormulaPreprocessor fp = new FormulaPreprocessor();        
        System.out.println("Formula: " + f);
        Pattern p = Pattern.compile("\\(instance (\\?[a-zA-Z0-9]+) ([a-zA-Z0-9\\-_]+)");
        Matcher m = p.matcher(formStr);
        m.find();
        String var = m.group(1);
        String cl = m.group(2);
        System.out.println("FormulaPreprocessor.testExplicit(): " + var + " " + cl);
        System.out.println("Explicit types: " + fp.findExplicitTypes(f));
    }
    
    /** ***************************************************************
     */
    public static void testAddTypes() {
       
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB("SUMO");
        
        System.out.println();
    	String strf = "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) " +
                "(element ?ELEMENT ?SET2))) (equal ?SET1 ?SET2))";
    	Formula f = new Formula();
    	f.read(strf);
    	FormulaPreprocessor fp = new FormulaPreprocessor();
    	FormulaPreprocessor.debug = true;
    	System.out.println(fp.addTypeRestrictionsNew(f,kb));
    	
    	System.out.println();
    	strf = "(=> (and (attribute ?AREA LowTerrain) (part ?ZONE ?AREA)" + 
    			" (slopeGradient ?ZONE ?SLOPE)) (greaterThan 0.03 ?SLOPE))";
    	f.read(strf);
    	fp = new FormulaPreprocessor();
    	System.out.println(fp.addTypeRestrictionsNew(f,kb));
    	
    	System.out.println();
    	strf = "(=> (and (typicalPart ?PART ?WHOLE) (instance ?X ?PART) " +
    			"(equal ?PARTPROB (ProbabilityFn (exists (?Y) (and " +
                "(instance ?Y ?WHOLE) (part ?X ?Y))))) (equal ?NOTPARTPROB " +
                "(ProbabilityFn (not (exists (?Z) (and (instance ?Z ?WHOLE) " +
                "(part ?X ?Z))))))) (greaterThan ?PARTPROB ?NOTPARTPROB))";
    	f.read(strf);
    	fp = new FormulaPreprocessor();    	
    	System.out.println(fp.addTypeRestrictionsNew(f,kb));
    }
    
    /** ***************************************************************
     */
    public static void main(String[] args) {
       
        //testFindTypes();
        //testAddTypes();
        testFindExplicit();
    }

}
