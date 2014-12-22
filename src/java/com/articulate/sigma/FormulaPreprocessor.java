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
        /*
        // build the sortalTypeCache key.
        StringBuilder sb = new StringBuilder("gtl");
        sb.append(pred);
        sb.append(kb.name);
        String key = sb.toString();
        HashMap<String, Object> stc = kb.kbCache.getSortalTypeCache();
        ArrayList<Object> result = (ArrayList<Object>) stc.get(key);
        if (result == null) {
            result = new ArrayList<Object>();
            int valence = kb.getValence(pred);
            int len = Formula.MAX_PREDICATE_ARITY + 1;
            if (valence == 0) 
                len = 2;                
            else if (valence > 0) 
                len = valence + 1;                
            String[] r = new String[len];

            ArrayList<Formula> al = kb.askWithRestriction(0,"domain",1,pred);
            ArrayList<Formula> al2 = kb.askWithRestriction(0,"domainSubclass",1,pred);
            ArrayList<Formula> al3 = kb.askWithRestriction(0,"range",1,pred);
            ArrayList<Formula> al4 = kb.askWithRestriction(0,"rangeSubclass",1,pred);
            r = addToTypeList(pred,al,r,false);
            r = addToTypeList(pred,al2,r,true);
            r = addToTypeList(pred,al3,r,false);
            r = addToTypeList(pred,al4,r,true);
            for (int i = 0; i < r.length; i++)
                result.add(r[i]);
            stc.put(key, result);
        }        
        return result;
        */
    }

    /** ***************************************************************
     * A utility helper method for computing predicate data types.
     
    private String[] addToTypeList(String pred, ArrayList al, String[] result, boolean classP) {

        // If the relations in al start with "(range", argnum will
        // be 0, and the arg position of the desired classnames
        // will be 2.
        int argnum = 0;
        int clPos = 2;
        for (int i = 0; i < al.size(); i++) {
            Formula f = (Formula) al.get(i);
            if (f.theFormula.startsWith("(domain")) {
                argnum = Integer.parseInt(f.getArgument(2));
                clPos = 3;
            }
            String cl = f.getArgument(clPos);
            String errStr = null;
            String mgrErrStr = null;
            if ((argnum < 0) || (argnum >= result.length)) {
                errStr = "Possible arity confusion for: " + pred;
                _f.errors.add(errStr);
            }
            else if (StringUtil.emptyString(result[argnum])) {
                if (classP) { cl += "+"; }
                result[argnum] = cl;
            }
            else {
                if (!cl.equals(result[argnum])) {
                    errStr = ("Multiple types asserted for argument " + argnum
                              + " of " + pred + ": " + cl + ", " + result[argnum]);
                    _f.errors.add(errStr);
                }
            }
        }
        return result;
    }
*/
    /** ***************************************************************
     * Find the argument type restriction for a given predicate and
     * argument number that is inherited from one of its
     * super-relations.  A "+" is appended to the type if the
     * parameter must be a class.  Argument number 0 is used for the
     * return type of a Function.  Asking for a non-existent arg
     * will return null;
     */
    public static String findType(int numarg, String pred, KB kb) {

        //HashMap<String,ArrayList<String>>
        ArrayList<String> sig = kb.kbCache.signatures.get(pred);
        if (sig == null) {
        	if (!kb.isInstanceOf(pred, "VariableArityRelation"))
        		System.out.println("Error in FormulaPreprocessor.findType(): no type information for predicate " + pred);
            return null;
        }
        if (numarg >= sig.size())
        	return null;
        //System.out.println("INFO in FormulaPreprocessor.findType(): signature for " + pred + " is: " + sig);
        //System.out.println("INFO in FormulaPreprocessor.findType(): arg " + numarg);
        
        return sig.get(numarg);
        /*
        String result = null;
        boolean isCached = false;
        boolean cacheResult = false;
        // build the sortalTypeCache key.
        StringBuilder sb = new StringBuilder("ft");
        sb.append(numarg);
        sb.append(pred);
        sb.append(kb.name);
        String key = sb.toString();
        Map<String,Object> stc = kb.kbCache.getSortalTypeCache();
        result = (String) (stc.get(key));
        isCached = (result != null);
        cacheResult = !isCached;

        if (result == null) {
            boolean found = false;
            Set<String> accumulator = new HashSet<String>();
            accumulator.add(pred);
            List<String> relations = new ArrayList<String>();
            while (!found && !accumulator.isEmpty()) {
                relations.clear();
                relations.addAll(accumulator);
                accumulator.clear();
                Iterator<String> it = relations.iterator();
                while (!found && it.hasNext()) {
                    String r = (String) it.next();
                    List<Formula> formulas = null;
                    if (numarg > 0) {
                        formulas = kb.askWithRestriction(0,"domain",1,r);
                        for (Formula f : formulas) {
                            int argnum = Integer.parseInt(f.getArgument(2));
                            if (argnum == numarg) {
                                result = f.getArgument(3);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            formulas = kb.askWithRestriction(0,"domainSubclass",1,r);
                            for (Formula f : formulas) {
                                int argnum = Integer.parseInt(f.getArgument(2));
                                if (argnum == numarg) {
                                    result = (f.getArgument(3) + "+");
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                    else if (numarg == 0) {
                        formulas = kb.askWithRestriction(0,"range",1,r);
                        if (!formulas.isEmpty()) {
                            Formula f = (Formula) formulas.get(0);
                            result = f.getArgument(2);
                            found = true;
                        }
                        if (!found) {
                            formulas = kb.askWithRestriction(0,"rangeSubclass",1,r);
                            if (!formulas.isEmpty()) {
                                Formula f = (Formula) formulas.get(0);
                                result = (f.getArgument(2) + "+");
                                found = true;
                            }
                        }
                    }
                }
                if (!found) {
                    for (String r : relations)
                        accumulator.addAll(kb.getTermsViaAskWithRestriction(1,r,0,"subrelation",2));
                }
            }
            if (cacheResult && (result != null))
                stc.put(key, result);
        }        
        return result;
        */
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
     * Does much of the real work for addTypeRestrictions() by
     * recursing through the Formula and collecting type constraint
     * information for the variable var.
     *
     * @param ios A List of classes (class name Strings) of which any
     * binding for var must be an instance.
     *
     * @param scs A List of classes (class name Strings) of which any
     * binding for var must be a subclass.
     *
     * @param var A SUO-KIF variable.
     *
     * @param kb The KB used to determine predicate and variable arg
     * types.
     *
     * @return void
     
    private void computeTypeRestrictions(List<String> ios, List<String> scs, String var, KB kb) {

        if (debug) 
        	System.out.println("INFO in FormulaPreprocessor.computeTypeRestrictions(): var: " + _f);
        String pred = null;
        if (!_f.listP() || !_f.theFormula.contains(var))
            return;
        Formula f = new Formula();
        f.read(_f.theFormula);
        pred = f.car();
        if (Formula.isQuantifier(pred)) {
            String arg2 = f.getArgument(2);
            if (arg2.contains(var)) {
                Formula nextF = new Formula();
                nextF.read(arg2);
                FormulaPreprocessor fp = new FormulaPreprocessor(nextF);
                fp.computeTypeRestrictions(ios, scs, var, kb);
            }
        }
        else if (Formula.isLogicalOperator(pred)) {
            int len = f.listLength();
            for (int i = 1; i < len; i++) {
                String argI = f.getArgument(i);
                if (argI.contains(var)) {
                    Formula nextF = new Formula();
                    nextF.read(argI);
                    FormulaPreprocessor fp = new FormulaPreprocessor(nextF);
                    fp.computeTypeRestrictions(ios, scs, var, kb);
                }
            }
        }
        else {
            int len = f.listLength();
            int valence = kb.getValence(pred);
            List types = getTypeList(pred,kb);
            int numarg = 0;
            for (int i = 1; i < len; i++) {
                numarg = i;
                if (valence == 0) // pred is a VariableArityRelation
                    numarg = 1;                   
                String arg = f.getArgument(i);
                if (arg.contains(var)) {
                    if (Formula.listP(arg)) {
                        Formula nextF = new Formula();
                        nextF.read(arg);
                        FormulaPreprocessor fp = new FormulaPreprocessor(nextF);
                        fp.computeTypeRestrictions(ios, scs, var, kb);
                    }
                    else if (var.equals(arg)) {
                        String type = null;
                        if (numarg < types.size()) 
                            type = (String) types.get(numarg);                            
                        if (type == null) 
                            type = findType(numarg,pred,kb);                            
                        if (StringUtil.isNonEmptyString(type) && !type.startsWith("Entity")) {
                            boolean sc = false;
                            while (type.endsWith("+")) {
                                sc = true;
                                type = type.substring(0, type.length() - 1);
                            }
                            if (sc) {
                                if (!scs.contains(type)) 
                                    scs.add(type);                                    
                            }
                            else if (!ios.contains(type)) 
                                ios.add(type);                                
                        }
                    }
                }
            }            
            if (pred.equals("equal")) {   // Special treatment for equal
                String arg1 = f.getArgument(1);
                String arg2 = f.getArgument(2);
                String term = null;
                if (var.equals(arg1)) { term = arg2; }
                else if (var.equals(arg2)) { term = arg1; }
                if (!StringUtil.emptyString(term)) {
                    if (Formula.listP(term)) {
                        Formula nextF = new Formula();
                        nextF.read(term);
                        if (nextF.isFunctionalTerm()) {
                            String fn = nextF.car();
                            List classes = getTypeList(fn, kb);
                            String cl = null;
                            if (!classes.isEmpty()) 
                                cl = (String) classes.get(0);                                
                            if (cl == null) 
                                cl = findType(0, fn, kb);                                
                            if (StringUtil.isNonEmptyString(cl) && !cl.startsWith("Entity")) {
                                boolean sc = false;
                                while (cl.endsWith("+")) {
                                    sc = true;
                                    cl = cl.substring(0, cl.length() - 1);
                                }
                                if (sc) {
                                    if (!scs.contains(cl)) 
                                        scs.add(cl);                                        
                                }
                                else if (!ios.contains(cl)) 
                                    ios.add(cl);                                    
                            }
                        }
                    }
                    else {
                        HashSet<String> instanceOfs = kb.kbCache.instances.get(term);
                        if ((instanceOfs != null) && !instanceOfs.isEmpty()) {
                            Iterator<String> it = instanceOfs.iterator();
                            while (it.hasNext()) {
                                String io = (String) it.next();
                                if (!io.equals("Entity") && !ios.contains(io)) 
                                    ios.add(io);                                    
                            }
                        }
                    }
                }
            }
            // Special treatment for instance or subclass, only if var.equals(arg1)
            // and arg2 is a functional term.
            else if (Arrays.asList("instance", "subclass").contains(pred)) {
                String arg1 = f.getArgument(1);
                String arg2 = f.getArgument(2);
                if (var.equals(arg1) && Formula.listP(arg2)) {
                    Formula nextF = new Formula();
                    nextF.read(arg2);
                    if (nextF.isFunctionalTerm()) {
                        String fn = nextF.car();
                        List classes = getTypeList(fn, kb);
                        String cl = null;
                        if (!classes.isEmpty()) 
                            cl = (String) classes.get(0);                            
                        if (cl == null) 
                            cl = findType(0, fn, kb);                            
                        if (StringUtil.isNonEmptyString(cl) && !cl.startsWith("Entity")) {
                            while (cl.endsWith("+")) 
                                cl = cl.substring(0, cl.length() - 1);                                
                            if (pred.equals("subclass")) {
                                if (!scs.contains(cl)) 
                                    scs.add(cl);                                    
                            }
                            else if (!ios.contains(cl)) 
                                ios.add(cl);                                
                        }
                    }
                }
            }
        }
        if (debug) {
        	System.out.println("INFO in FormulaPreprocessor.computeTypeRestrictions(): var: " + var);
        	System.out.println(ios);
        	System.out.println(scs);
        }
        return;
    }
*/
    /** ***************************************************************
     * When invoked on a Formula that begins with explicit universal
     * quantification, this method returns a String representation of
     * the Formula with type constraints added for the top level
     * quantified variables, if possible.  Otherwise, a String
     * representation of the original Formula is returned.
     *
     * @param shelf A List of quaternary ArrayLists, each of which
     * contains type information about a variable     
     * @param kb The KB used to determine predicate and variable arg
     * types.     
     * @return A String representation of a Formula, with type
     * restrictions added.
     
    private String insertTypeRestrictionsU(List shelf, KB kb) {

        String result = _f.theFormula;
        String varlist = _f.getArgument(1);
        Formula varlistF = new Formula();
        varlistF.read(varlist);
        List newShelf = makeNewShelf(shelf);
        int vlen = varlistF.listLength();
        for (int i = 0; i < vlen; i++) 
            addVarDataQuad(varlistF.getArgument(i), "U", newShelf);            
        String arg2 = _f.getArgument(2);
        Formula nextF = new Formula();
        nextF.read(arg2);
        FormulaPreprocessor fp = new FormulaPreprocessor(nextF);
        String processedArg2 = fp.insertTypeRestrictionsR(newShelf, kb);
        Set<String> constraints = new LinkedHashSet<String>();
        Iterator it = newShelf.iterator();
        while (it.hasNext()) {
            List quad = (List) it.next();
            String var = (String) quad.get(0);
            String token = (String) quad.get(1);
            if (token.equals("U")) {
                List<String> ios = (List<String>) quad.get(2);
                List<String> scs = (List<String>) quad.get(3);
                if (!scs.isEmpty()) {
                    winnowTypeList(scs, kb);
                    if (!scs.isEmpty()) {
                        if (!ios.contains("SetOrClass"))
                            ios.add("SetOrClass");
                        Iterator<String> it2 = scs.iterator();
                        while (it2.hasNext()) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("(subclass " + var + " " + it2.next() + ")");
                            String constraint = sb.toString();
                            if (!processedArg2.contains(constraint)) 
                                constraints.add(constraint);                                
                        }
                    }
                }
                if (!ios.isEmpty()) {
                    winnowTypeList(ios, kb);
                    Iterator<String> it2 = ios.iterator();
                    while (it2.hasNext()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("(instance " + var + " " + it2.next() + ")");
                        String constraint = sb.toString();
                        if (!processedArg2.contains(constraint)) 
                            constraints.add(constraint);                            
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("(forall ");
        sb.append(varlistF.theFormula);
        if (constraints.isEmpty()) {
            sb.append(" ");
            sb.append(processedArg2);
        }
        else {
            sb.append(" (=>");
            int clen = constraints.size();
            if (clen > 1) 
                sb.append(" (and");       
            Iterator<String> it2 = constraints.iterator();
            while (it2.hasNext()) {
                sb.append(" ");
                sb.append(it2.next().toString());
            }
            if (clen > 1) 
                sb.append(")");                
            sb.append(" ");
            sb.append(processedArg2);
            sb.append(")");
        }
        sb.append(")");
        result = sb.toString();
        return result;
    }
*/
    /** ***************************************************************
     * When invoked on a Formula that begins with explicit existential
     * quantification, this method returns a String representation of
     * the Formula with type constraints added for the top level
     * quantified variables, if possible.  Otherwise, a String
     * representation of the original Formula is returned.
     *
     * @param shelf A List of quaternary ArrayLists, each of which
     * contains type information about a variable     
     * @param kb The KB used to determine predicate and variable arg
     * types.     
     * @return A String representation of a Formula, with type
     * restrictions added.
     
    private String insertTypeRestrictionsE(List shelf, KB kb) {
    
        String result = _f.theFormula;
        String varlist = _f.getArgument(1);
        Formula varlistF = new Formula();
        varlistF.read(varlist);
        List newShelf = makeNewShelf(shelf);
        int vlen = varlistF.listLength();
        for (int i = 0; i < vlen; i++) 
            addVarDataQuad(varlistF.getArgument(i), "E", newShelf);            

        String arg2 = _f.getArgument(2);
        Formula nextF = new Formula();
        nextF.read(arg2);

        FormulaPreprocessor fp = new FormulaPreprocessor(nextF);
        String processedArg2 = fp.insertTypeRestrictionsR(newShelf, kb);
        nextF.read(processedArg2);

        Set constraints = new LinkedHashSet();
        StringBuilder sb = new StringBuilder();

        List quad = null;
        String var = null;
        String token = null;
        List ios = null;
        List scs = null;
        Iterator it2 = null;
        String constraint = null;
        for (Iterator it = newShelf.iterator(); it.hasNext();) {
            quad = (List) it.next();
            var = (String) quad.get(0);
            token = (String) quad.get(1);
            if (token.equals("E")) {
                ios = (List) quad.get(2);
                scs = (List) quad.get(3);
                if (!ios.isEmpty()) {
                    winnowTypeList(ios, kb);
                    for (it2 = ios.iterator(); it2.hasNext();) {
                        sb.setLength(0);
                        sb.append("(instance " + var + " " + it2.next().toString() + ")");
                        constraint = sb.toString();
                        if (!processedArg2.contains(constraint)) {
                            constraints.add(constraint);
                        }
                    }
                }
                if (!scs.isEmpty()) {
                    winnowTypeList(scs, kb);
                    for (it2 = scs.iterator(); it2.hasNext();) {
                        sb.setLength(0);
                        sb.append("(subclass " + var + " " + it2.next().toString() + ")");
                        constraint = sb.toString();
                        if (!processedArg2.contains(constraint)) {
                            constraints.add(constraint);
                        }
                    }
                }
            }
        }
        sb.setLength(0);
        sb.append("(exists ");
        sb.append(varlistF.theFormula);
        if (constraints.isEmpty()) {
            sb.append(" ");
            sb.append(processedArg2);
        }
        else {
            sb.append(" (and");
            for (it2 = constraints.iterator(); it2.hasNext();) {
                sb.append(" ");
                sb.append(it2.next().toString());
            }
            if (nextF.car().equals("and")) {
                int nextFLen = nextF.listLength();
                for (int k = 1; k < nextFLen; k++) {
                    sb.append(" ");
                    sb.append(nextF.getArgument(k));
                }
            }
            else {
                sb.append(" ");
                sb.append(nextF.theFormula);
            }
            sb.append(")");
        }
        sb.append(")");
        result = sb.toString();
        return result;
    }
*/
    /** ***************************************************************
     * When invoked on a Formula, this method returns a String
     * representation of the Formula with type constraints added for
     * all explicitly quantified variables, if possible.  Otherwise, a
     * String representation of the original Formula is returned.
     *
     * @param shelf A List, each element of which is a quaternary List
     * containing a SUO-KIF variable String, a token "U" or "E"
     * indicating how the variable is quantified, a List of instance
     * classes, and a List of subclass classes     
     * @param kb The KB used to determine predicate and variable arg
     * types.     
     * @return A String representation of a Formula, with type
     * restrictions added.
     
    private String insertTypeRestrictionsR(List shelf, KB kb) {

        String result = _f.theFormula;
        if (Formula.listP(_f.theFormula)
            && !Formula.empty(_f.theFormula)
            && _f.theFormula.matches(".*\\?\\w+.*")) {
            StringBuilder sb = new StringBuilder();
            Formula f = new Formula();
            f.read(_f.theFormula);
            int len = f.listLength();
            String arg0 = f.car();
            if (Formula.isQuantifier(arg0) && (len == 3)) {
                FormulaPreprocessor fp = new FormulaPreprocessor(f);
                if (arg0.equals("forall")) 
                    sb.append(fp.insertTypeRestrictionsU(shelf, kb));                    
                else 
                    sb.append(fp.insertTypeRestrictionsE(shelf, kb));                    
            }
            else {
                sb.append("(");
                String argI = null;
                for (int i = 0; i < len; i++) {
                    argI = f.getArgument(i);
                    if (i > 0) {
                        sb.append(" ");
                        if (Formula.isVariable(argI)) {
                            String type = findType(i, arg0, kb);
                            if (StringUtil.isNonEmptyString(type)
                                && !type.startsWith("Entity")) {
                                boolean sc = false;
                                while (type.endsWith("+")) {
                                    sc = true;
                                    type = type.substring(0, type.length() - 1);
                                }
                                if (sc) 
                                    addScForVar(argI, type, shelf);                                    
                                else 
                                    addIoForVar(argI, type, shelf);                                    
                            }
                        }
                    }
                    Formula nextF = new Formula();
                    nextF.read(argI);
                    FormulaPreprocessor fp = new FormulaPreprocessor(nextF);
                    sb.append(fp.insertTypeRestrictionsR(shelf, kb));
                }
                sb.append(")");
            }
            result = sb.toString();
        }
        return result;
    }
    */
    /** ***************************************************************
     
    private void addVarDataQuad(String var, String quantToken, List shelf) {
        
        ArrayList quad = new ArrayList();
        quad.add(var);              // e.g., "?X"
        quad.add(quantToken);       // "U" or "E"
        quad.add(new ArrayList());  // ios
        quad.add(new ArrayList());  // scs
        shelf.add(0, quad);
        return;
    }
    */
    /** ***************************************************************
     
    private ArrayList getIosForVar(String var, List shelf) {
        
        ArrayList result = null;
        ArrayList quad = null;
        for (Iterator si = shelf.iterator(); si.hasNext();) {
            quad = (ArrayList) si.next();
            if (var.equals((String) (quad.get(0)))) {
                result = (ArrayList) (quad.get(2));
                break;
            }
        }
        return result;
    }
*/
    /** ***************************************************************
     
    private ArrayList getScsForVar(String var, List shelf) {
        
        ArrayList result = null;
        ArrayList quad = null;
        for (Iterator si = shelf.iterator(); si.hasNext();) {
            quad = (ArrayList) si.next();
            if (var.equals((String) (quad.get(0)))) {
                result = (ArrayList) (quad.get(3));
                break;
            }
        }
        return result;
    }
*/
    /** ***************************************************************
     
    private void addIoForVar(String var, String io, List shelf) {
        
        if (StringUtil.isNonEmptyString(io)) {
            ArrayList ios = getIosForVar(var, shelf);
            if ((ios != null) && !ios.contains(io))                     
                ios.add(io);                
        }
        return;
    }
*/
    /** ***************************************************************
     
    private void addScForVar(String var, String sc, List shelf) {
        
        if (StringUtil.isNonEmptyString(sc)) {
            ArrayList scs = getScsForVar(var, shelf);
            if ((scs != null) && !scs.contains(sc)) 
                scs.add(sc);                
        }
        return;
    }
*/
    /** ***************************************************************
     
    private ArrayList makeNewShelf(List shelf) {
        
        return new ArrayList(shelf);
    }
*/
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
     
    private String addTypeRestrictions(KB kb) {

        String result = _f.theFormula;
        Formula f = new Formula();
        f.read(_f.makeQuantifiersExplicit(false));
        FormulaPreprocessor fp = new FormulaPreprocessor(f);
        //System.out.println("INFO in FormulaPreprocessor.addTypeRestrictions(): " + f);
        result = fp.insertTypeRestrictionsR(new ArrayList(), kb);
        return result;
    }
*/
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
     * keys and a value of an ArrayList of Strings
     
    private static void addToMap(HashMap<String,ArrayList<String>> map, String key, String element) {
    	
		ArrayList<String> al = map.get(key);
		if (al == null)
			al = new ArrayList<String>();
		al.add(element);
		map.put(key, al);
    }
    */
    /** ***************************************************************
     * utility method to add a String element to a HashMap of String
     * keys and a value of an ArrayList of Strings
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
     * This method returns a HashMap that maps each String variable in this
     * Formula to an ArrayList that contains a pair of ArrayLists.
     * The first ArrayList of the pair contains the names of types
     * (classes) of which the variable must be an instance.  The
     * second ArrayList of the pair contains the names of types of
     * which the variable must be a subclass.  Either list in the pair
     * could be empty.  If the only instance or subclass sortal that
     * can be computed for a variable is Entity, the lists will be
     * empty.
     *
     * @param kb The KB used to compute the sortal constraints for
     * each variable.     
     * @return A HashMap
     
    public void computeVariableTypes(KB kb, HashMap<String,ArrayList<String>> instmap, 
    		HashMap<String,ArrayList<String>> classmap) {

        if (debug) System.out.println("INFO in FormulaPreprocessor.computeVariableTypes(): \n" + _f);
        Formula f = new Formula();
        //f.read(_f.makeQuantifiersExplicit(false));
        FormulaPreprocessor fp = new FormulaPreprocessor(f);
        fp.computeVariableTypesR(instmap,classmap, kb);
    }

    /** ***************************************************************
     * A recursive utility method used to collect type information for
     * the variables in this Formula.
     *
     * @param map A HashMap used to store type information for the
     * variables in this Formula.     
     * @param kb The KB used to compute the sortal constraints for
     * each variable.     
     * @return void
     
    private void computeVariableTypesR(HashMap<String,ArrayList<String>> instmap, HashMap<String,ArrayList<String>> classmap, KB kb) {

        if (_f.listP() && !_f.empty()) {
            int len = _f.listLength();
            String arg0 = _f.car();
            if (Formula.isQuantifier(arg0) && (len == 3)) 
                computeVariableTypesQ(instmap, classmap, kb);                
            else {
                for (int i = 0; i < len; i++) {
                    Formula nextF = new Formula();
                    nextF.read(_f.getArgument(i));
                    FormulaPreprocessor fp = new FormulaPreprocessor(nextF);
                    fp.computeVariableTypesR(instmap, classmap, kb);
                }
            }
        }
        return;
    }

    /** ***************************************************************
     * A recursive utility method used to collect type information for
     * the variables in this Formula, which is assumed to have forall
     * or exists as its arg0.
     *
     * @param map A HashMap used to store type information for the
     * variables in this Formula.     
     * @param kb The KB used to compute the sortal constraints for
     * each variable.     
     * @return void
     
    private void computeVariableTypesQ(HashMap<String,ArrayList<String>> instmap, HashMap<String,ArrayList<String>> classmap, KB kb) {

        Formula varlistF = new Formula();
        varlistF.read(_f.getArgument(1));
        // System.out.println("varlistF == " + varlistF);
        int vlen = varlistF.listLength();
        // System.out.println("vlen == " + vlen);
        Formula nextF = new Formula();
        nextF.read(_f.getArgument(2));
        // System.out.println("nextF == " + nextF);
        String var = null;
        for (int i = 0; i < vlen; i++) {
            ArrayList<String> ios = new ArrayList<String>();
            ArrayList<String> scs = new ArrayList<String>();
            var = varlistF.getArgument(i);
            // System.out.println("i == " + i + ", var == " + var);
            FormulaPreprocessor fp = new FormulaPreprocessor(nextF);
            fp.computeTypeRestrictions(ios, scs, var, kb);
            if (!scs.isEmpty()) {
                winnowTypeList(scs, kb);
                if (!scs.isEmpty() && !ios.contains("SetOrClass"))
                    ios.add("SetOrClass");
            }
            if (!ios.isEmpty()) 
                winnowTypeList(ios, kb);                
            instmap.put(var,ios);
            instmap.put(var,scs);
        }
        FormulaPreprocessor fp = new FormulaPreprocessor(nextF);
        fp.computeVariableTypesR(instmap, classmap, kb);
        return;
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
