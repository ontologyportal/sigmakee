/** This code is copyright Articulate Software (c) 2017.  Some portions
 copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
 This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 Users of this code also consent, by use of this code, to credit Articulate Software
 and Teknowledge in any writings, briefings, publications, presentations, or
 other representations of any software which incorporates, builds on, or uses this
 code.  Please cite the following article in any publication with references:

 Pease, A., (2003). The Sigma Ontology Development Environment,
 in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 August 9, Acapulco, Mexico. See also https://github.com/ontologyportal/sigmakee

 This class expects the following to be in the ontology.
 Their absence won't cause an exception, but will prevent correct behavior.
 VariableArityRelation
 subclass
 instance
 Class

 */
package com.articulate.sigma;

import com.articulate.sigma.parsing.Expr;
import com.articulate.sigma.parsing.ExprToTPTP;
import com.articulate.sigma.parsing.FormulaAST;
import com.articulate.sigma.parsing.RowVar;
// Note: com.articulate.sigma.parsing.PredVarInst is referenced by fully-qualified name below
// to avoid shadowing com.articulate.sigma.PredVarInst (used by replacePredVarsAndRowVars).
import com.articulate.sigma.utils.*;
import com.articulate.sigma.trans.SUMOtoTFAform;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FormulaPreprocessor {

    /** ***************************************************************
     * For any given formula, stop generating new pred var instantiations
     * and row var expansions if this threshold value has been exceeded.
     * The default value is 2000.
     */
    private static final int AXIOM_EXPANSION_LIMIT = 2000;

    private static final Pattern INSTANCE_TYPE_PATTERN = Pattern.compile("\\(instance (\\?[a-zA-Z0-9\\-_]+) ([\\?a-zA-Z0-9\\-_]+)");
    private static final Pattern SUBCLASS_TYPE_PATTERN  = Pattern.compile("\\(subclass (\\?[a-zA-Z0-9\\-_]+) ([\\?a-zA-Z0-9\\-]+)");

    public static boolean debug = false;

    public static boolean addOnlyNonNumericTypes = false;

    public static Set<String> errors = new HashSet<>();

    /** ***************************************************************
     * A + is appended to the type if the parameter must be a class
     *
     * @return the type for each argument to the given predicate, where
     * ArrayList element 0 is the result, if a function, 1 is the first
     * argument, 2 is the second etc.
     */
    private List<String> getTypeList(String pred, KB kb) {

        return kb.kbCache.signatures.get(pred);
    }

    /** ***************************************************************
     */
    private boolean hasFormulaType(Formula form,
                                   Map<String,Set<String>> varmap) {

        if (debug) System.out.println("hasFormulaType(): form: " + form);
        if (debug) System.out.println("hasFormulaType(): varmap: " + varmap);
        for (Set<String> hs : varmap.values()) {
            for (String t : hs)
                if (t.equals("Formula")) {
                    if (debug) System.out.println("hasFormulaType(): has a Formula argument: " + form);
                    return true;
                }
        }
        return false;
    }

    /** ***************************************************************
     * Find the argument type restriction for a given predicate and
     * argument number that is inherited from one of its super-relations.
     * A "+" is appended to the type if the parameter must be a class,
     * meaning that a domainSubclass is defined for this argument in one
     * of the loaded .kif files.  Argument number 0 is used for the return
     * type of a Function.  Asking for a non-existent arg will return null;
     */
    public static String findType(int numarg, String pred, KB kb) {

        List<String> sig = null;
        if (kb == null || kb.kbCache == null) {
            System.err.println("Error in FormulaPreprocessor.findType(): null cache");
            return null;
        }
        else if (kb.kbCache.signatures == null)
            System.err.println("Error in FormulaPreprocessor.findType(): null cache signatures");
        if (kb.kbCache != null && kb.kbCache.signatures != null)
            sig = kb.kbCache.signatures.get(pred);
        if (sig == null) {
            if (!kb.isInstanceOf(pred, "VariableArityRelation") && !Formula.isLogicalOperator(pred) &&
                !pred.equals(Formula.EQUAL)) {
                if (debug) System.out.println("Error in FormulaPreprocessor.findType(): " +
                        "no type information for predicate " + pred);
                if (debug) System.out.println("start of FormulaPreprocessor.findType: " + StringUtil.shorten(kb.kbCache.signatures.toString(),100) + "...");
                List<Formula> ar = kb.askWithRestriction(0,"domain",1,pred);
                if (debug) System.out.println("domains: " + ar);
            }
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
     */
    public void winnowTypeList(Set<String> types, KB kb) {

        long t1 = 0L;
        if (types.size() > 1) {
            Object[] valArr = types.toArray();
            String clX = null;
            String clY = null;
            boolean stop;
            for (int i = 0; i < valArr.length; i++) {
                stop = false;
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
    }

    /** ***************************************************************
     * Find all the type restrictions on the variables in a formula,
     * including constraints from relation argument typing as well as
     * explicitly stated types from instance and subclass expressions.
     */
    public Map<String,Set<String>> findTypeRestrictions(Formula form, KB kb) {

        if (debug) System.out.println("findTypeRestrictions: form \n" + form);
        Map<String,Set<String>> varDomainTypes = computeVariableTypes(form, kb);
        if (debug) System.out.println("findTypeRestrictions: varDomainTypes " + varDomainTypes);
        // get variable types which are explicitly defined in formula
        Map<String,Set<String>> varExplicitTypes = findExplicitTypesClassesInAntecedent(kb,form);
        if (debug) System.out.println("findTypeRestrictions: varExplicitTypes " + varExplicitTypes);
        // only keep variables which are not explicitly defined in formula
        Map<String,Set<String>> varmap = new HashMap<>();
        Set<String> types, domainTypes, explicitTypes;
        for (String var : varDomainTypes.keySet()) {
            if (!varExplicitTypes.containsKey(var)) {
                // var is not explicitly defined
                varmap.put(var, varDomainTypes.get(var));
            }
            else {
                // var is explicitly defined
                domainTypes = varDomainTypes.get(var);
                explicitTypes = varExplicitTypes.get(var);
                types = new HashSet();
                for (String dt : domainTypes) {
                    if (dt.endsWith("+")) types.add(dt); // '+' denotes domainSubclass
                }
                for (String et : explicitTypes) {
                    if (et.endsWith("+")) types.add(et);  // '+' denotes domainSubclass
                }
                varmap.put(var, types);
            }
        }
        return varmap;
    }

    /** ***************************************************************
     */
    public Map<String,Set<String>> findAllTypeRestrictions(Formula form, KB kb) {

        if (debug) System.out.println("findAllTypeRestrictions: form \n" + form);
        Map<String,Set<String>> varDomainTypes = computeVariableTypes(form, kb);
        if (debug) System.out.println("FormulaPreprocessor.findAllTypeRestrictions: varDomainTypes " + varDomainTypes);
        // get variable types which are explicitly defined in formula
        Map<String,Set<String>> varExplicitTypes = findExplicitTypesClassesInAntecedent(kb,form);
        if (debug) System.out.println("FormulaPreprocessor.findAllTypeRestrictions: varExplicitTypes " + varExplicitTypes);
        // only keep variables which are not explicitly defined in formula
        Map<String,Set<String>> varmap = new HashMap<>();
        Set<String> types, domainTypes, explicitTypes;
        for (String var : varDomainTypes.keySet()) {
            types = new HashSet();
            domainTypes = varDomainTypes.get(var);
            explicitTypes = varExplicitTypes.get(var);
            if (domainTypes != null)
                types.addAll(domainTypes);
            if (explicitTypes != null)
                types.addAll(explicitTypes);
            varmap.put(var, types);
        }
        for (String var : varExplicitTypes.keySet()) {
            types = new HashSet();
            domainTypes = varDomainTypes.get(var);
            explicitTypes = varExplicitTypes.get(var);
            if (domainTypes != null)
                types.addAll(domainTypes);
            if (explicitTypes != null)
                types.addAll(explicitTypes);
            varmap.put(var, types);
        }
        if (debug) System.out.println("FormulaPreprocessor.findAllTypeRestrictions: returning: " + varmap);
        return varmap;
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
    public Formula addTypeRestrictions(Formula form, KB kb) {

        if (debug) System.out.println("addTypeRestrictions(): form " + form);
        // get variable types from domain definitions
        Map<String,Set<String>> varmap = findTypeRestrictions(form,kb);

        if (debug) System.out.println("addTypeRestrictions(: varmap " + varmap);
        // compute quantifiedVariables and unquantifiedVariables
        //ArrayList<ArrayList<String>> quantifiedUnquantifiedVariables =
        //        form.collectQuantifiedUnquantifiedVariables();
        Set<String> unquantifiedVariables = form.collectUnquantifiedVariables();
        if (hasFormulaType(form,varmap)) // one of the types of the variables is Formula
            form.higherOrder = true;
        // add sortals for unquantifiedVariables
        StringBuilder sb = new StringBuilder();
        boolean begin = true;
        Set<String> types;
        for (String unquantifiedV : unquantifiedVariables) {
            //String unquantifiedV = unquantifiedVariables.get(i);
            types = varmap.get(unquantifiedV);
            if (types != null && !types.isEmpty()) {
                for (String t : new TreeSet<>(types)) {
                    if (StringUtil.emptyString(t))
                        continue;
                    if (begin) {
                        sb.append("(=> \n  (and \n");  // TODO: need test for singular list
                        begin = false;
                    }
                    if (!t.endsWith("+")) {
                        if (!addOnlyNonNumericTypes || !kb.isSubclass(t,"Quantity")) {
                            if (!t.equals("Entity") && !t.equals("World")) // trap world type in THF that is already restricted with THF language type restriction
                                sb.append(" (instance ").append(unquantifiedV).append(Formula.SPACE).append(t).append(") ");
                        }
                    }
                    else
                        sb.append(" (subclass ").append(unquantifiedV).append(Formula.SPACE).append(t.substring(0,t.length()-1)).append(") ");
                }
            }
        }

        if (!begin)
            sb.append(")\n");
        if (debug) System.out.println("addTypeRestrictions: sb: " + sb);
        // recursively add sortals for existentially quantified variables
       // if ((form.theFormula.indexOf(Formula.EQUANT) > -1) ||
       //         (form.theFormula.indexOf(Formula.UQUANT) > -1))
        addTypeRestrictionsRecurse(kb, form, sb);

        if (!begin)
            sb.append(")\n");

        Formula f = new Formula();
        f.read(sb.toString());
        if (StringUtil.emptyString(f.getFormula()) || f.empty())
            f.read(form.getFormula());
        f.read(SUMOtoTFAform.elimUnitaryLogops(f));
        if (debug) System.out.println("addTypeRestrictions: result: " + f);
        if (debug) System.out.println("addTypeRestrictions: form at end: " + form);
        if (debug) System.out.println("addTypeRestrictions: sb at end: '" + sb + "'");
        if (debug) System.out.println("addTypeRestrictions(: varmap at end" + varmap);
        f.varTypeCache = varmap;
        return f;
    }

    /** ***************************************************************
     * Recursively add sortals for existentially quantified variables
     *
     * @param kb The KB used to add type restrictions.
     * @param f The formula in KIF syntax
     * @param sb A StringBuilder used to store the new formula with sortals
     */
    private void addTypeRestrictionsRecurse(KB kb, Formula f, StringBuilder sb) {

        if (debug) System.out.println("addTypeRestrictionsRecurse: input: " + f);
        if (debug) System.out.println("addTypeRestrictionsRecurse: sb: " + sb);
        if (f == null || StringUtil.emptyString(f.getFormula()) || f.empty())
            return;

        String carstr = f.car();
        if (debug) System.out.println("addTypeRestrictionsRecurse: carstr: " + carstr);
        if (Formula.atom(carstr) && (Formula.isLogicalOperator(carstr) || carstr.equals(Formula.EQUAL))) {
            sb.append(Formula.LP).append(carstr).append(Formula.SPACE);
            if (debug) System.out.println("addTypeRestrictionsRecurse: interior sb: " + sb);
            if (carstr.equals(Formula.EQUANT) || carstr.equals(Formula.UQUANT)) {
                // If we see existentially quantified variables, like (exists (?X ?Y) ...),
                //   and if ?X, ?Y are not explicitly restricted in the following statements,
                // we need to add type restrictions for ?X, ?Y
                sb.append(f.getArgument(1)).append(Formula.SPACE);
                List<String> quantifiedVariables = collectVariables(f.getStringArgument(1));
                // set addSortals = true, if at least one variable is existentially quantified variable,
                // and it is not explicitly restricted
                boolean addSortals = false;
                Map<String,Set<String>> varDomainTypes = computeVariableTypes(f, kb);
                Map<String,Set<String>> varExplicitTypes = findExplicitTypesClassesInAntecedent(kb,f);
                // only keep variables which are not explicitly defined in formula
                Map<String,Set<String>> varmap = new HashMap<>(varDomainTypes);
                if (varExplicitTypes != null) {
                    for (String v : varExplicitTypes.keySet())
                        varmap.remove(v);
                }
                Set<String> types;
                for (String ev : quantifiedVariables) {
                    types = varmap.get(ev);
                    if (types != null && !types.isEmpty()) {
                        addSortals = true;
                        break;
                    }
                }
                if (addSortals) {
                    if (carstr.equals(Formula.EQUANT)) sb.append("(and ");
                    else if (carstr.equals(Formula.UQUANT)) sb.append("(=> (and ");
                }
                String existentiallyQV;
                for (int i = 0; i < quantifiedVariables.size(); i++) {
                    existentiallyQV = quantifiedVariables.get(i);
                    types = varmap.get(existentiallyQV);
                    if (types != null && !types.isEmpty()) {
                        for (String t : new TreeSet<>(types)) {
                            if (StringUtil.emptyString(t))
                                continue;
                            if (!t.endsWith("+")) {
                                if (!t.equals("Entity"))
                                    sb.append(" (instance ").append(existentiallyQV).append(Formula.SPACE).append(t).append(") ");
                            }
                            else
                                sb.append(" (subclass ").append(existentiallyQV).append(Formula.SPACE).append(t.substring(0,t.length()-1)).append(") ");
                        }
                    }
                }
                if (addSortals && carstr.equals(Formula.UQUANT))
                    sb.append(Formula.RP);
                for (int i = 2 ; i < f.listLength(); i++)
                    addTypeRestrictionsRecurse(kb, new Formula(f.getArgument(i)), sb);
                if (addSortals)
                    sb.append(Formula.RP);
            }
            else {
                if (debug) System.out.println("addTypeRestrictionsRecurse: input interior: " + f);
                if (debug) System.out.println("addTypeRestrictionsRecurse: args: " + f.complexArgumentsToArrayList(1));
                if (debug) System.out.println("addTypeRestrictionsRecurse: list length: " + f.listLength());

                if (debug)
                    for (int i = 1; i < f.listLength(); i++) {
                        Formula newF = new Formula(f.getArgument(i));
                        System.out.println("addTypeRestrictionsRecurse: " + f.getArgument(i) + " : " + newF + " : " + newF.getFormula());
                    }
                // recurse from the first argument if the formula is not in (exists ...) / (forall ...) scope
                for (int i = 1; i < f.listLength(); i++)
                    addTypeRestrictionsRecurse(kb, new Formula(f.getArgument(i)), sb);
            }
            sb.append(Formula.RP);
        }
        else if (f.isSimpleClause(kb) || f.atom()) {
            if (debug) System.out.println("addTypeRestrictionsRecurse: simple clause or atom: " + f);
            sb.append(f).append(Formula.SPACE);
        }
        else {
            if (debug) System.out.println("addTypeRestrictionsRecurse: here3: f: " + f);
            sb.append(Formula.LP);
            List<String> args = f.complexArgumentsToArrayListString(0);
            for (String s : args)
                addTypeRestrictionsRecurse(kb, new Formula(s), sb);
            sb.append(Formula.RP);
            if (debug) System.out.println("addTypeRestrictionsRecurse: here3: sb: " + sb);
        }
    }

    /** ***************************************************************
     * Collect variables from strings.
     *
     * For example,
     * Input = (?X ?Y ?Z)
     * Output = a list of ?X, ?Y and ?Z
     *
     * Input = ?X
     * Output = a list of ?X
     */
    private List<String> collectVariables(String argstr) {

        List<String> arglist = new ArrayList<>();
        if (argstr.startsWith(Formula.V_PREF)) {
            arglist.add(argstr);
            return arglist;
        }
        else if (argstr.startsWith(Formula.LP)) {
            arglist.addAll(Arrays.asList(argstr.substring(1, argstr.length()-1).split(Formula.SPACE)));
            return arglist;
        }
        else {
            String errStr = "Error in FormulaPreprocessor.collectVariables ...";
            System.err.println(errStr);
            // TODO: report to error log?
            return null;
        }
    }

    /** ************************************************************************
     * Get the most specific type for variables.
     *
     * @param kb The KB to be used for processing
     * @param types a list of sumo types for a sumo term/variable
     * @return the most specific sumo type for the term/variable
     *
     * For example
     * types of ?Writing = [Entity, Physical, Process, IntentionalProcess,
     *                      ContentDevelopment, Writing]
     * return the most specific type Writing
     */
    protected String getMostRelevantType(KB kb, Set<String> types) {

        Set<String> insts = new TreeSet<>();
        for (String type : types) {
            if (!type.endsWith("+"))
                insts.add(type);
            else
                insts.add(type.substring(0, type.length()-1));
        }
        if (insts != null) {
            winnowTypeList(insts, kb);
            Iterator<String> it1 = insts.iterator();
            while (it1.hasNext()) {
                return it1.next();
            }
        }

        return null;
    }

    /*****************************************************************
     * Collect the types of any variables that are specifically defined
     * in the antecedent of a rule with an instance or subclass expression.
     * TODO: This may ultimately require CNF conversion and then checking negative
     * literals, but for now it's just a hack to grab preconditions.
     */
    public Map<String, Set<String>> findExplicitTypesInAntecedent(KB kb, Formula form) {

        if (!form.isRule())
            // TODO: Consider returning empty map instead of null. Check callers for special behavior on null.
            return null;

        Formula f = new Formula();
        f.read(form.getFormula());
        Formula antecedent = f.cdrAsFormula().carAsFormula();

        return findExplicitTypes(kb,antecedent);
    }

    /*****************************************************************
     * Collect the types of any variables that are specifically defined
     * in the antecedent of a rule with an instance expression;
     * Collect the super classes of any variables that are specifically
     * defined in the antecedent of a rule with an subclass expression;
     */
    public Map<String, Set<String>> findExplicitTypesClassesInAntecedent(KB kb, Formula form) {

        Formula f = new Formula();
        f.read(form.getFormula());
        Formula antecedent = findAntecedent(f);
        Map<String, Set<String>> varExplicitTypes = new HashMap<>();
        Map<String, Set<String>> varExplicitClasses = new HashMap<>();
        findExplicitTypesClasses(kb, antecedent, varExplicitTypes, varExplicitClasses);
        return varExplicitTypes;
    }

    /** ***************************************************************
     * Return a formula's antecedents
     */
    private static Formula findAntecedent(Formula f) {

        if (!f.getFormula().contains(Formula.IF) && !f.getFormula().contains(Formula.IFF))
            return f;
        String carstr = f.car();
        if (Formula.atom(carstr) && Formula.isLogicalOperator(carstr)) {
            if (carstr.equals(Formula.IF) || carstr.equals(Formula.IFF))
                return f.cdrAsFormula().carAsFormula();
            else
                return f;
        }
        return f;
    }

    /*****************************************************************
     * Collect variable names and their types from instance or subclass
     * expressions. subclass restrictions are marked with a '+'.
     *
     * @param form The formula in KIF syntax
     *
     * @return A map of variables paired with a set of sumo types collected
     * from instance and subclass expressions.
     *
     * TODO: This may ultimately require CNF conversion and then checking
     * negative literals, but for now it's just a hack to grab preconditions.
     */
    public Map<String, Set<String>> findExplicitTypes(KB kb, Formula form) {

        Map<String,Set<String>> varExplicitTypes = new HashMap<>();
        Map<String,Set<String>> varExplicitClasses = new HashMap<>();
        findExplicitTypesRecurse(kb, form, false, varExplicitTypes, varExplicitClasses);

        varExplicitTypes.putAll(varExplicitClasses);
        return varExplicitTypes;
    }

    /*****************************************************************
     * Collect variable names and their types from instance or subclass
     * expressions.
     *
     * @param form The formula in KIF syntax
     * @param varExplicitTypes A map of variables paired with sumo types
     *                         collected from instance expressions
     * @param varExplicitClasses A map of variables paired with sumo types
     *                           collected from subclass expression
     */
    public void findExplicitTypesClasses(KB kb, Formula form,
           Map<String,Set<String>> varExplicitTypes,
           Map<String,Set<String>> varExplicitClasses) {

        findExplicitTypesRecurse(kb, form, false, varExplicitTypes, varExplicitClasses);
    }

    /*****************************************************************
     * Recursively collect a variable name and its types.
     */
    public static void findExplicitTypesRecurse(KB kb, Formula form, boolean isNegativeLiteral,
                 Map<String,Set<String>> varExplicitTypes,
                 Map<String, Set<String>> varExplicitClasses) {

        if (form == null || StringUtil.emptyString(form.getFormula()) || form.empty())
            return;

        String carstr = form.car();

        if (Formula.atom(carstr) && Formula.isLogicalOperator(carstr)) {
            switch (carstr) {
                case Formula.EQUANT:
                case Formula.UQUANT:
                    for (int i = 2 ; i < form.listLength(); i++)  // (exists (?X ?Y) (foo1 ?X ?Y)), recurse from the second argument
                        findExplicitTypesRecurse(kb,new Formula(form.getArgument(i)), false, varExplicitTypes, varExplicitClasses);
                    break;
                case Formula.NOT:
                    for (int i = 1; i < form.listLength(); i++)   // (not (foo1 ?X ?Human)), set isNegativeLiteral = true, and recurse from the first argument
                        findExplicitTypesRecurse(kb,new Formula(form.getArgument(i)), true, varExplicitTypes, varExplicitClasses);
                    break;
                default:
                    for (int i = 1; i < form.listLength(); i++)   // eg. (and (foo1 ?X ?Y) (foo2 ?X ?Z)), recurse from the first argument
                        findExplicitTypesRecurse(kb,new Formula(form.getArgument(i)), false, varExplicitTypes, varExplicitClasses);
                    break;
            }
        }
        else if (form.isSimpleClause(kb)) {
            if (isNegativeLiteral)  // If form is negative literal, do not add explicit type for the variable
                return;
            Matcher m = INSTANCE_TYPE_PATTERN.matcher(form.getFormula());
            String var, cl;
            Set<String> hs;
            while (m.find()) {
                var = m.group(1);
                cl = m.group(2);
                hs = new HashSet<>();
                if (!cl.startsWith(Formula.V_PREF)) {
                    if (varExplicitTypes.containsKey(var))
                        hs = varExplicitTypes.get(var);
                    hs.add(cl);
                }
                else {
                    if (varExplicitTypes.containsKey(var))
                        hs = varExplicitTypes.get(var);
                }
                if (hs != null && !hs.isEmpty())
                    varExplicitTypes.put(var, hs);
            }

            m = SUBCLASS_TYPE_PATTERN.matcher(form.getFormula());
            while (m.find()) {
                var = m.group(1);
                cl = m.group(2);
                hs = new HashSet<>();
                if (!cl.startsWith(Formula.V_PREF)) {
                    if (varExplicitClasses.containsKey(var))
                        hs = varExplicitClasses.get(var);
                    hs.add(cl + "+");
                }
                else {
                    if (varExplicitClasses.containsKey(var))
                        hs = varExplicitClasses.get(var);
                }
                if (hs != null && !hs.isEmpty())
                    varExplicitClasses.put(var, hs);
            }
        }
        else {
            findExplicitTypesRecurse(kb,form.carAsFormula(), false, varExplicitTypes, varExplicitClasses);
            findExplicitTypesRecurse(kb,form.cdrAsFormula(), false, varExplicitTypes, varExplicitClasses);
        }
    }

    /*****************************************************************
     * This method returns a HashMap that maps each String variable in
     * this the names of types (classes) of which the variable must be
     * an instance or the names of types of which the variable must be
     * a subclass. Note that this method does not capture explicit type
     * from assertions such as (=> (instance ?Foo Bar) ...). This method
     * just consider restrictions implicitly defined from the arg types
     * of relations.
     *
     * @param kb The KB to be used to compute the sortal constraints
     *           for each variable.
     * @return A HashMap of variable names and their types. Subclass
     *         restrictions are marked with a '+', meaning that a
     *         domainSubclass is defined for this argument in one of
     *         the loaded .kif files. Instance restrictions have no
     *         special mark.
     */
    public Map<String,Set<String>> computeVariableTypes(Formula form, KB kb) {

        if (!form.varTypeCache.keySet().isEmpty() && KBmanager.initialized) { // type lists can change as KBs are read
            if (debug) System.out.println("INFO in FormulaPreprocessor.computeVariableTypes(): returning cached types for \n" + form);
            if (debug) System.out.println("INFO in FormulaPreprocessor.computeVariableTypes(): types: " + form.varTypeCache);
            return form.varTypeCache;
        }
        if (debug) System.out.println("INFO in FormulaPreprocessor.computeVariableTypes(): \n" + form);
        Formula f = new Formula();
        f.read(form.getFormula());
        Map<String,Set<String>> result = new HashMap<>();
        return computeVariableTypesRecurse(kb,form,result);
    }

    /** ***************************************************************
     * @param arg0 is a function
     * @param arg1 is a variable
     * @result is a side effect on the result map
     */
    private void setEqualsVartype(KB kb, String arg0, String arg1,
                                  Map<String,Set<String>> result) {

        Formula func = new Formula(arg0);
        String fstr = func.car();
        String type = kb.kbCache.getRange(fstr);
        if (type == null) {
            type = "Entity";
            System.err.println("Error in FormulaPreprocessor.setEqualsVartype() no function type for " + fstr);
        }
        MapUtils.addToMap(result,arg1,type);
    }

    /** ***************************************************************
     *  @rparam input A HashMap of variable names and their types. Subclass
     *               restrictions are marked with a '+', meaning that a
     *               domainSubclass is defined for this argument in one of
     *               the loaded .kif files. Instance restrictions have no
     *               special mark.
     */
    private Map<String,Set<String>> computeVariableTypesRecurse(KB kb, Formula f,
                                                                       Map<String,Set<String>> input) {

        if (kb == null)
            System.err.println("Error in FormulaPreprocessor.computeVariableTypesRecurse() kb = null found while processing: \n" + f);
        Map<String,Set<String>> result = new HashMap<>();
        if (f == null || StringUtil.emptyString(f.getFormula()) || f.empty() || f.isVariable() || f.atom())
            return result;
        if (debug) System.out.println("INFO in FormulaPreprocessor.computeVariableTypesRecurse(): input formula \n" + f);
        String carstr = f.car();
        if (Formula.atom(carstr) && Formula.isLogicalOperator(carstr) && !carstr.equals(Formula.EQUAL)) {
            if (debug) System.out.println("INFO in FormulaPreprocessor.computeVariableTypesRecurse(): logical op " + carstr);
            result.putAll(input);
            int start = 1;
            if (Formula.isQuantifier(carstr))  // skip the quantified variable list
                start = 2;
            Formula farg;
            for (int i = start; i <= f.listLength(); i++) {
                farg = f.getArgument(i);
                if (farg != null) {
                    farg.sourceFile = f.sourceFile;
                    result = KButilities.mergeToMap(result, computeVariableTypesRecurse(kb, farg, input), kb);
                }
            }
        }
        else { //if (f.isSimpleClause(kb)) { // simple clauses include functions
            String pred = carstr, errStr;
            if (debug) System.out.println("INFO in FormulaPreprocessor.computeVariableTypesRecurse(): simple clause ");
            if (f.getFormula().contains(Formula.V_PREF) && !Formula.isVariable(pred)) {
                List<Formula> args = f.complexArgumentsToArrayList(1);
                if (args == null) {
                    errStr = "Error in FormulaPreprocessor.computeVariableTypesRecurse(): no arguments found in: \n" + f;
                    if (debug) System.err.println(errStr);
                    errors.add(errStr);
                }
                if (pred.equals(Formula.EQUAL) && args.size() > 1) {
                    if (args.get(0).isVariable() && args.get(1).listP() &&
                            kb.isFunctional(args.get(1)))
                        setEqualsVartype(kb, args.get(1).getFormula(), args.get(0).getFormula(),result);
                    if (args.get(1).isVariable() && args.get(0).listP() &&
                            kb.isFunctional(args.get(0)))
                        setEqualsVartype(kb, args.get(0).getFormula(), args.get(1).getFormula(),result);
                }
                int argnum = 1;
                if (args != null) {
                    String cl;
                    for (Formula arg : args) {
                        if (debug) System.out.println("arg,pred,argnum: " + arg + ", " + pred + ", " + argnum +
                                "\nnewf: " + args + "\nis function?: " + kb.isFunctional(arg));
                        if (arg.isVariable()) {
                            cl = findType(argnum, pred, kb);
                            if (debug) System.out.println("cl: " + cl);
                            if (StringUtil.emptyString(cl)) {
                                if (kb.kbCache == null || !kb.kbCache.transInstOf(pred, "VariableArityRelation") &&
                                        !pred.equals(Formula.EQUAL)) {
                                    errStr = "Error in FormulaPreprocessor.computeVariableTypesRecurse(): " +
                                            "no type information for arg " + argnum + " of relation " + pred + " in formula: \n" + f;
                                    if (debug) System.err.println(errStr);
                                    if (debug) System.err.printf("Formula origin: %s%n", f.sourceFile);
                                    if (debug) System.err.println("sig: " + kb.kbCache.getSignature(pred));
                                    if (debug) System.err.println("sig count: " + kb.kbCache.signatures.keySet().size());
                                    errors.add(errStr);
                                }
                            }
                            else
                                MapUtils.addToMap(result, arg.getFormula(), cl);
                        }
                        else if (arg.listP() && kb.isFunctional(arg)) { // If formula is function then recurse.
                            if (debug) System.out.println("arg is a function: " + arg);
                            result = KButilities.mergeToMap(result, computeVariableTypesRecurse(kb, new Formula(arg), input), kb);
                        }
                        argnum++;
                    }
                }
            }
        }
        /* else {
            //result = mergeToMap(input,computeVariableTypesRecurse(kb,f.carAsFormula(),input), kb);
            ArrayList<String> args = f.complexArgumentsToArrayList(0);
            if (debug) System.out.println("INFO in FormulaPreprocessor.computeVariableTypesRecurse(): not simple clause{, args: " + args);
            for (String s : args)
                result = mergeToMap(result,computeVariableTypesRecurse(kb,new Formula(s),input), kb);
            //result = mergeToMap(result,computeVariableTypesRecurse(kb,f.cdrAsFormula(),input), kb);
        } */
        if (debug) System.out.println("INFO in FormulaPreprocessor.computeVariableTypesRecurse(): exiting from\n" + f);
        if (debug) System.out.println("INFO in FormulaPreprocessor.computeVariableTypesRecurse(): with result:" + result);
        f.varTypeCache.putAll(result);
        return result;
    }

    /** ***************************************************************
     * Pre-process a formula before sending it to the theorem prover.
     * This includes ignoring meta-knowledge like documentation strings,
     * translating mathematical operators, quoting higher-order formulas,
     * adding a numerical suffix to VariableArityRelations based on their count,
     * expanding row variables and prepending the 'holds__' predicate.
     * @return an String as Formula
     */
    private String preProcessRecurse(Formula f, String previousPred, boolean ignoreStrings,
                                     boolean translateIneq, boolean translateMath,
                                     KB kb) {

        if (debug) System.out.println("preProcessRecurse: " + f);
        StringBuilder result = new StringBuilder();
        if (f.listP() && !f.empty()) {
            String prefix = "";
            String pred = f.car();
            if (Formula.isQuantifier(pred)) {
                // The list of quantified variables.
                result.append(Formula.SPACE);
                result.append(f.cadr());
                // The formula following the list of variables.
                String next = f.caddr();
                Formula nextF = new Formula();
                nextF.read(next);
                result.append(Formula.SPACE);
                result.append(preProcessRecurse(nextF,"",ignoreStrings,translateIneq,translateMath,kb));
            }
            else {
                if (kb.isInstanceOf(pred,"VariableArityRelation")) {
                    int arity = f.complexArgumentsToArrayList(0).size()-1;
                    String oldPred = pred;

                    // note this has to match with Formula.renameVariableArityRelations()
                    String func = "";
                    if (pred.endsWith(Formula.FN_SUFF))
                        func = Formula.FN_SUFF;
                    if (!pred.endsWith("__" + arity + func)) {
                        //System.out.println("preProcessRecurse(): adding " + "__" + arity + func + " to " + pred);
                        pred = pred + "__" + arity + func;
                    }
                    kb.kbCache.copyNewPredFromVariableArity(pred,oldPred,arity);
                    if (debug) System.out.println("preProcessRecurse(): pred: " + pred);
                }
                Formula restF = f.cdrAsFormula();
                //if (debug) System.out.println("preProcessRecurse: restF: " + restF);
                int argCount = 1;
                String arg, res;
                Formula argF;
                while (!restF.empty()) {
                    argCount++;
                    arg = restF.car();
                    argF = new Formula();
                    argF.read(arg);
                    if (argF.listP()) {
                        res = preProcessRecurse(argF,pred,ignoreStrings,translateIneq,translateMath,kb);
                        result.append(Formula.SPACE);
                        /* if (!Formula.isLogicalOperator(pred) &&
                                !Formula.isComparisonOperator(pred) &&
                                !Formula.isMathFunction(pred) &&
                                !kb.isFunctional(argF.theFormula)) {
                            result.append("`");
                        } */
                        result.append(res);
                    }
                    else
                        result.append(Formula.SPACE).append(arg);
                    restF.read(restF.cdr());
                    //if (debug) System.out.println("preProcessRecurse: restF: " + restF);
                    //if (debug) System.out.println("preProcessRecurse: result: " + result);
                }
                /*
                if (KBmanager.getMgr().getPref("holdsPrefix").equals("yes")) {
                    if (!Formula.isLogicalOperator(pred) && !Formula.isQuantifierList(pred,previousPred))
                        prefix = "holds_";
                    if (kb.isFunctional(f))
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
                */
            }
            result.insert(0, pred);
            result.insert(0, prefix);
            result.insert(0, Formula.LP);
            result.append(Formula.RP);
            //if (debug) System.out.println("preProcessRecurse: result: " + result);
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
    protected List<Formula> replacePredVarsAndRowVars(Formula form, KB kb, boolean addHoldsPrefix) {

        List<Formula> result = new ArrayList<>();
        if (debug) System.out.println("FormulaPreprocessor.replacePredVarsAndRowVars(): " + form);
        Formula startF = new Formula();
        startF.read(form.getFormula());
        Set<String> predVars = PredVarInst.gatherPredVars(kb,startF);
        Set<Formula> accumulator = new LinkedHashSet<>();
        accumulator.add(startF);
        List<Formula> working = new ArrayList<>();
        int prevAccumulatorSize = 0;
        Set<Formula> instantiations;
        List<Formula> ar;
        while (accumulator.size() != prevAccumulatorSize) {
            if (debug) System.out.println("FormulaPreprocessor.replacePredVarsAndRowVars(): prevAccumulatorSize: " + prevAccumulatorSize);
            if (debug) System.out.println("FormulaPreprocessor.replacePredVarsAndRowVars(): accumulatorSize: " + accumulator.size());
            prevAccumulatorSize = accumulator.size();
            // Initialize predicate variables if we are not adding holds prefixes.
            if (!addHoldsPrefix) {
                working.clear();
                working.addAll(accumulator);
                accumulator.clear();
                for (Formula f : working) {
                    instantiations = PredVarInst.instantiatePredVars(f,kb);
                    if (predVars.size() > 1) {
                        if (debug) System.out.println("FormulaPreprocessor.replacePredVarsAndRowVars(): returning doubles: " + instantiations);
                        if (debug) System.out.println(SUMOtoTFAform.getFilterMessage());
                    }
                    if (debug) System.out.println("FormulaPreprocessor.replacePredVarsAndRowVars(): pred vars repl: " + f + "\n" + instantiations);
                    form.errors.addAll(f.getErrors());

                    // If the accumulator is null -- the formula can't be instantiated at all and has been marked "reject",
                    //    don't add anything
                    // If the accumulator is empty -- no pred var instantiations were possible,
                    //    add the original formula to the accumulator for possible row var expansion below.
                    if (instantiations != null) {
                        if (instantiations.isEmpty()) {
                            accumulator.add(f);
                        }
                        else {
                            accumulator.addAll(instantiations);
                        }
                    }
                }
            }

            if (debug) System.out.println("FormulaPreprocessor.replacePredVarsAndRowVars(): accumulator: " + accumulator);
            if (debug) System.out.println("FormulaPreprocessor.replacePredVarsAndRowVars(): accumulator size: " + accumulator.size());
            if (debug) System.out.println("FormulaPreprocessor.replacePredVarsAndRowVars(): starting on row var replacement: ");
            // Row var expansion. Iterate over the instantiated predicate formulas,
            // doing row var expansion on each.  If no predicate instantiations can be generated, the accumulator
            // will contain just the original input formula.
            if (!accumulator.isEmpty() && (accumulator.size() < AXIOM_EXPANSION_LIMIT)) {
                working.clear();
                working.addAll(accumulator);
                accumulator.clear();
                for (Formula f : working) {
                    ar = RowVars.expandRowVars(kb,f);
                    if (ar == null || ar.isEmpty())
                        accumulator.add(f);
                    else
                        accumulator.addAll(ar);
                    if (accumulator.size() > AXIOM_EXPANSION_LIMIT) {
                        System.err.println("Error in FormulaPreprocessor.replacePredVarsAndRowVars(): AXIOM_EXPANSION_LIMIT EXCEEDED: " + AXIOM_EXPANSION_LIMIT);
                        break;
                    }
                }
            }
        }
        result.addAll(accumulator);
        if (debug) System.out.println("FormulaPreprocessor.replacePredVarsAndRowVars(): result: " + result);
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

        boolean pass;
        // kb isn't used yet, because the checks below are purely
        // syntactic.  But it probably will be used in the future.
        pass = !(
                StringUtil.containsNonAsciiChars(f.getFormula())
                        // (<relation> ?X ...) - no free variables in an
                        // atomic formula that doesn't contain a string
                        // unless the formula is a query.
                        || (!query
                        && !Formula.isLogicalOperator(f.car())
                        // The formula does not contain a string.
                        && (f.getFormula().indexOf('"') == -1)
                        // The formula contains a free variable.
                        && f.getFormula().matches(".*\\?\\w+.*"))

                        // ... add more patterns here, as needed.
                        || false
        );
        return pass;
    }

    /** ***************************************************************
     * Adds statements of the form (instance <Entity> <Class>) if
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
     * replaced, and to which (instance <Entity> <Class>)
     * Formulae might be added
     *
     * @return an ArrayList of Formula(s), which could be larger than
     * the input List, variableReplacements, or could be empty.
     */
    private List<Formula> addInstancesOfSetOrClass(Formula form, KB kb,
                               boolean isQuery, List<Formula> variableReplacements) {

        List<Formula> result = new ArrayList<>();
        if ((variableReplacements != null) && !variableReplacements.isEmpty()) {
            if (isQuery)
                result.addAll(variableReplacements);
            else {
                Set<Formula> formulae = new TreeSet<>();
                String arg0, ioStr, arg;
                Formula f, ioF;
                int start, argslen;
                List<String> args;
                StringBuilder sb = new StringBuilder();
                for (Iterator<Formula> it = variableReplacements.iterator(); it.hasNext();) {
                    f = it.next();
                    formulae.add(f);
                    if (f.listP() && !f.empty()) {  // Make sure every Class is stated to be such
                        arg0 = f.car();
                        start = -1;
                        if (arg0.equals("subclass")) start = 0;
                        else if (arg0.equals("instance")) start = 1;
                        if (start > -1) {
                            args = new ArrayList<>(Arrays.asList(f.getStringArgument(1),
                                                                  f.getStringArgument(2)));
                            argslen = args.size();
                            for (int i = start; i < argslen; i++) {
                                arg = args.get(i);
                                if (!Formula.isVariable(arg) && !arg.equals("Class") && Formula.atom(arg)) {
                                    sb.setLength(0);
                                    sb.append("(instance ");
                                    sb.append(arg);
                                    sb.append(" Class)");
                                    ioF = new Formula();
                                    ioStr = sb.toString().intern();
                                    ioF.read(ioStr);
                                    ioF.sourceFile = form.sourceFile;
                                    if (!kb.formulaMap.containsKey(ioStr)) {
                                        formulae.add(ioF);
                                    }
                                }
                            }
                        }
                    }
                }
                result.addAll(new TreeSet<>(formulae));
            }
        }
        return result;
    }

    /** ***************************************************************
     * Pre-process a formula before sending it to the theorem prover.
     * This includes ignoring meta-knowledge like documentation strings,
     * translating mathematical operators, quoting higher-order formulas,
     * expanding row variables and instantiating predicate variables
     *
     * @param form a formula to process
     *
     * @param isQuery If true the Formula is a query and should be
     *                existentially quantified, else the Formula is a
     *                statement and should be universally quantified
     *
     * @param kb The KB to be used for processing this Formula
     *
     * @return an Set of Formula(s), which could be empty.
     *
     */
    // -----------------------------------------------------------------------
    // Pure-Expr type-restriction pipeline (no string round-trip)
    // -----------------------------------------------------------------------

    /** ***************************************************************
     * Expr analogue of {@link #computeVariableTypes(Formula, KB)}.
     *
     * <p>Walks the Expr tree and collects a {@code variable → types} map
     * by looking up each predicate's argument signature in
     * {@code kb.kbCache.signatures}.  No KIF string is constructed.</p>
     */
    Map<String, Set<String>> computeVariableTypesExpr(Expr expr, KB kb) {
        return computeVariableTypesRecurseExpr(kb, expr, new HashMap<>());
    }

    private Map<String, Set<String>> computeVariableTypesRecurseExpr(KB kb, Expr expr,
                                                                      Map<String, Set<String>> input) {
        if (!(expr instanceof Expr.SExpr se)) return new HashMap<>();
        String head = se.headName();
        if (head == null) return new HashMap<>(); // var-list node inside a quantifier

        Map<String, Set<String>> result = new HashMap<>();

        if (Formula.isLogicalOperator(head) && !head.equals(Formula.EQUAL)) {
            // Logical operator: recurse into subformulas
            result.putAll(input);
            int start = 0;
            if (Formula.isQuantifier(head)) start = 1; // skip the variable-list argument
            for (int i = start; i < se.args().size(); i++) {
                result = KButilities.mergeToMap(result,
                        computeVariableTypesRecurseExpr(kb, se.args().get(i), input), kb);
            }
        } else {
            // Predicate application (ground or with variables)
            String pred = head;
            if (!Formula.isVariable(pred)) {
                // Special case: (equal funcExpr ?var) or (equal ?var funcExpr)
                if (pred.equals(Formula.EQUAL) && se.args().size() >= 2) {
                    Expr a0 = se.args().get(0);
                    Expr a1 = se.args().get(1);
                    if (a0 instanceof Expr.Var vv && a1 instanceof Expr.SExpr fse) {
                        String fhead = fse.headName();
                        if (fhead != null && kb.isFunction(fhead)) {
                            String type = kb.kbCache.getRange(fhead);
                            MapUtils.addToMap(result, vv.name(), type != null ? type : "Entity");
                        }
                    }
                    if (a1 instanceof Expr.Var vv && a0 instanceof Expr.SExpr fse) {
                        String fhead = fse.headName();
                        if (fhead != null && kb.isFunction(fhead)) {
                            String type = kb.kbCache.getRange(fhead);
                            MapUtils.addToMap(result, vv.name(), type != null ? type : "Entity");
                        }
                    }
                }
                int argnum = 1;
                for (Expr arg : se.args()) {
                    if (arg instanceof Expr.Var vv) {
                        String cl = findType(argnum, pred, kb);
                        if (!StringUtil.emptyString(cl)) {
                            MapUtils.addToMap(result, vv.name(), cl);
                        }
                    } else if (arg instanceof Expr.SExpr argSe) {
                        String argHead = argSe.headName();
                        if (argHead != null && !Formula.isVariable(argHead) && kb.isFunction(argHead)) {
                            result = KButilities.mergeToMap(result,
                                    computeVariableTypesRecurseExpr(kb, arg, input), kb);
                        }
                    }
                    argnum++;
                }
            }
        }
        return result;
    }

    /** ***************************************************************
     * Expr analogue of {@link #findExplicitTypesClassesInAntecedent(KB, Formula)}.
     *
     * <p>Locates the antecedent of an implication
     * ({@code (=> A B)} → A, {@code (<=> A B)} → A, otherwise the whole
     * formula), then walks it collecting variables that are explicitly
     * typed with {@code (instance ?V T)} or {@code (subclass ?V T)}
     * expressions.</p>
     */
    private Map<String, Set<String>> findExplicitTypesClassesInAntecedentExpr(Expr expr, KB kb) {
        Expr antecedent = findAntecedentExpr(expr);
        Map<String, Set<String>> varExplicitTypes   = new HashMap<>();
        Map<String, Set<String>> varExplicitClasses = new HashMap<>();
        findExplicitTypesRecurseExpr(antecedent, false, varExplicitTypes, varExplicitClasses);
        return varExplicitTypes;
    }

    private static Expr findAntecedentExpr(Expr expr) {
        if (!(expr instanceof Expr.SExpr se)) return expr;
        String head = se.headName();
        if ((Formula.IF.equals(head) || Formula.IFF.equals(head)) && se.args().size() >= 2)
            return se.args().get(0);
        return expr;
    }

    private static void findExplicitTypesRecurseExpr(Expr expr, boolean isNegativeLiteral,
                                                      Map<String, Set<String>> varExplicitTypes,
                                                      Map<String, Set<String>> varExplicitClasses) {
        if (!(expr instanceof Expr.SExpr se)) return;
        String head = se.headName();
        if (head == null) return;

        if (Formula.isLogicalOperator(head)) {
            switch (head) {
                case Formula.UQUANT:
                case Formula.EQUANT:
                    // Skip variable-list; recurse into body only
                    if (se.args().size() == 2)
                        findExplicitTypesRecurseExpr(se.args().get(1), false,
                                varExplicitTypes, varExplicitClasses);
                    break;
                case Formula.NOT:
                    for (Expr arg : se.args())
                        findExplicitTypesRecurseExpr(arg, true,
                                varExplicitTypes, varExplicitClasses);
                    break;
                default:
                    for (Expr arg : se.args())
                        findExplicitTypesRecurseExpr(arg, false,
                                varExplicitTypes, varExplicitClasses);
                    break;
            }
        } else {
            if (isNegativeLiteral) return;
            // Match (instance ?V T) or (subclass ?V T) where V is a Var and T is an Atom
            if ("instance".equals(head) && se.args().size() == 2
                    && se.args().get(0) instanceof Expr.Var vv
                    && se.args().get(1) instanceof Expr.Atom typeAtom
                    && !typeAtom.name().startsWith(Formula.V_PREF)) {
                MapUtils.addToMap(varExplicitTypes, vv.name(), typeAtom.name());
            } else if ("subclass".equals(head) && se.args().size() == 2
                    && se.args().get(0) instanceof Expr.Var vv
                    && se.args().get(1) instanceof Expr.Atom typeAtom
                    && !typeAtom.name().startsWith(Formula.V_PREF)) {
                MapUtils.addToMap(varExplicitClasses, vv.name(), typeAtom.name() + "+");
            } else {
                // Recurse into any other predicate's args (handles function-nested expressions)
                for (Expr arg : se.args())
                    findExplicitTypesRecurseExpr(arg, false, varExplicitTypes, varExplicitClasses);
            }
        }
    }

    /** ***************************************************************
     * Fully Expr-based replacement for {@link #findTypeRestrictions(Formula, KB)}.
     *
     * <p>No KIF string is ever constructed.  Merging logic is identical to
     * the original: domain types are kept for implicitly-typed variables;
     * for explicitly-typed variables only {@code "+"} (subclass) markers
     * from either source are retained.</p>
     *
     * <p>This is intentionally package-private for unit testing.</p>
     */
    Map<String, Set<String>> findTypeRestrictionsExpr(Expr expr, KB kb) {
        Map<String, Set<String>> varDomainTypes   = computeVariableTypesExpr(expr, kb);
        Map<String, Set<String>> varExplicitTypes = findExplicitTypesClassesInAntecedentExpr(expr, kb);

        Map<String, Set<String>> varmap = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : varDomainTypes.entrySet()) {
            String var = e.getKey();
            if (!varExplicitTypes.containsKey(var)) {
                varmap.put(var, e.getValue());
            } else {
                // Variable already has an explicit type — keep only subclass ("+") markers
                Set<String> types = new HashSet<>();
                for (String dt : e.getValue())
                    if (dt.endsWith("+")) types.add(dt);
                for (String et : varExplicitTypes.get(var))
                    if (et.endsWith("+")) types.add(et);
                varmap.put(var, types);
            }
        }
        return varmap;
    }

    /** ***************************************************************
     * Build a single type-guard {@link Expr}: {@code (instance ?V T)} or
     * {@code (subclass ?V T)} for a {@code T+} marker.
     * Returns {@code null} when the type should be skipped (Entity, World,
     * or a numeric type when {@link #addOnlyNonNumericTypes} is set).
     */
    private Expr buildTypeGuardExpr(String varName, String type, KB kb) {
        if (StringUtil.emptyString(type)) return null;
        if (type.endsWith("+")) {
            String t = type.substring(0, type.length() - 1);
            if (t.equals("Entity")) return null;
            return new Expr.SExpr(new Expr.Atom("subclass"),
                    List.of(new Expr.Var(varName), new Expr.Atom(t)));
        } else {
            if (type.equals("Entity") || type.equals("World")) return null;
            if (addOnlyNonNumericTypes && kb.isSubclass(type, "Quantity")) return null;
            return new Expr.SExpr(new Expr.Atom("instance"),
                    List.of(new Expr.Var(varName), new Expr.Atom(type)));
        }
    }

    /** ***************************************************************
     * Wrap a non-empty list of guard {@link Expr}s into
     * {@code (and g1 g2 ...)} or return the single element directly
     * when the list has exactly one entry (eliminating a unary {@code and}).
     */
    private static Expr wrapAndExpr(List<Expr> guards) {
        if (guards.size() == 1) return guards.get(0);
        return new Expr.SExpr(new Expr.Atom(Formula.AND), guards);
    }

    /** ***************************************************************
     * Recursively walk an {@link Expr} tree and inject type guards
     * inside quantifier ({@code forall}/{@code exists}) bodies.
     * This is the Expr-based analogue of
     * {@link #addTypeRestrictionsRecurse(KB, Formula, StringBuilder)}.
     *
     * <ul>
     *   <li>{@code forall (?Z) body} with guard G becomes
     *       {@code forall (?Z) (=> G body)}</li>
     *   <li>{@code exists (?Z) body} with guard G becomes
     *       {@code exists (?Z) (and G body)}</li>
     *   <li>Other logical operators are recursed into transparently.</li>
     *   <li>Simple predicate clauses are returned unchanged.</li>
     * </ul>
     *
     * @param expr    Expr subtree to transform
     * @param varmap  type map for the current lexical scope
     * @param kb      knowledge base (for {@link #buildTypeGuardExpr})
     * @return transformed Expr; may be the same object when nothing changed
     */
    private Expr addTypeRestrictionsRecurseExpr(Expr expr,
                                                 Map<String, Set<String>> varmap,
                                                 KB kb) {
        if (!(expr instanceof Expr.SExpr se)) return expr;
        String head = se.headName();
        if (head == null) return expr; // variable-list node inside a quantifier

        boolean isLogOp = Formula.isLogicalOperator(head);
        boolean isEqual  = head.equals(Formula.EQUAL);
        if (!isLogOp && !isEqual) {
            return expr; // simple clause — return unchanged
        }

        if (head.equals(Formula.EQUANT) || head.equals(Formula.UQUANT)) {
            if (se.args().size() != 2) return expr;
            Expr varListExpr = se.args().get(0);
            Expr bodyExpr    = se.args().get(1);

            // Collect quantified variable names
            List<String> quantVarNames = new ArrayList<>();
            if (varListExpr instanceof Expr.SExpr varSe) {
                for (Expr v : varSe.args()) {
                    if (v instanceof Expr.Var vv)         quantVarNames.add(vv.name());
                    else if (v instanceof Expr.RowVar rv) quantVarNames.add(rv.name());
                }
            }

            // Compute type restrictions scoped to this sub-expression
            Map<String, Set<String>> subVarmap = findTypeRestrictionsExpr(expr, kb);

            // Build type guards for the quantified variables
            List<Expr> guards = new ArrayList<>();
            for (String qv : quantVarNames) {
                Set<String> types = subVarmap.get(qv);
                if (types != null) {
                    for (String t : new TreeSet<>(types)) {
                        Expr g = buildTypeGuardExpr(qv, t, kb);
                        if (g != null) guards.add(g);
                    }
                }
            }

            // Recurse into the body
            Expr newBody = addTypeRestrictionsRecurseExpr(bodyExpr, subVarmap, kb);

            if (guards.isEmpty()) {
                if (newBody == bodyExpr) return expr; // nothing changed
                return new Expr.SExpr(se.head(), List.of(varListExpr, newBody));
            }

            Expr guardedBody;
            if (head.equals(Formula.EQUANT)) {
                // (exists (?Z) (and (instance ?Z T) body))
                List<Expr> andArgs = new ArrayList<>(guards);
                andArgs.add(newBody);
                guardedBody = wrapAndExpr(andArgs);
            } else {
                // (forall (?Z) (=> (and (instance ?Z T)) body))
                Expr antecedent = wrapAndExpr(guards);
                guardedBody = new Expr.SExpr(new Expr.Atom(Formula.IF),
                        List.of(antecedent, newBody));
            }
            return new Expr.SExpr(se.head(), List.of(varListExpr, guardedBody));

        } else {
            // Other logical operators: recurse into all arguments
            boolean changed = false;
            List<Expr> newArgs = new ArrayList<>(se.args().size());
            for (Expr arg : se.args()) {
                Expr newArg = addTypeRestrictionsRecurseExpr(arg, varmap, kb);
                if (newArg != arg) changed = true;
                newArgs.add(newArg);
            }
            return changed ? new Expr.SExpr(se.head(), newArgs) : expr;
        }
    }

    /** ***************************************************************
     * Expr-based equivalent of {@link #addTypeRestrictions(Formula, KB)}.
     *
     * <p>Wraps the formula with type-guard antecedents for free variables
     * and injects type guards inside quantifier bodies via
     * {@link #addTypeRestrictionsRecurseExpr}.  Returns the original
     * {@code expr} reference unchanged when no type restrictions apply,
     * allowing the caller to detect this with reference equality.</p>
     *
     * <p>This is intentionally package-private for unit testing.</p>
     *
     * @param expr   the Expr tree (after variable-arity renaming)
     * @param varmap type map from {@link #findTypeRestrictionsExpr}
     * @param kb     knowledge base
     * @return type-guarded Expr, or {@code expr} itself if nothing changed
     */
    Expr addTypeRestrictionsExpr(Expr expr, Map<String, Set<String>> varmap, KB kb) {
        // Collect free (unquantified) variables
        Set<String> freeVars = ExprToTPTP.collectFreeVars(expr);

        // Build guards for free variables
        List<Expr> freeGuards = new ArrayList<>();
        for (String fv : freeVars) {
            Set<String> types = varmap.get(fv);
            if (types != null) {
                for (String t : new TreeSet<>(types)) {
                    Expr g = buildTypeGuardExpr(fv, t, kb);
                    if (g != null) freeGuards.add(g);
                }
            }
        }

        // Recursively inject guards into quantifier bodies
        Expr body = addTypeRestrictionsRecurseExpr(expr, varmap, kb);

        // Wrap with free-variable guards (if any)
        if (freeGuards.isEmpty()) {
            return body; // same reference as expr when nothing changed inside
        }
        Expr antecedent = wrapAndExpr(freeGuards);
        return new Expr.SExpr(new Expr.Atom(Formula.IF), List.of(antecedent, body));
    }

    /** ***************************************************************
     * Recursively rename any VariableArityRelation predicates in an
     * {@link Expr} tree by appending the arity suffix {@code __N}
     * (or {@code __NFn} for function symbols). This mirrors the
     * renaming done in {@link #preProcessRecurse} and
     * {@link Formula#renameVariableArityRelations}, but operates
     * directly on the Expr AST without string reconstruction.
     */
    private Expr renameVariableArityInExpr(Expr expr, KB kb) {
        if (!(expr instanceof Expr.SExpr se)) return expr;
        // Recurse into all arguments first
        List<Expr> renamedArgs = se.args().stream()
                .map(a -> renameVariableArityInExpr(a, kb))
                .collect(Collectors.toList());
        Expr head = se.head();
        if (head instanceof Expr.Atom headAtom) {
            String pred = headAtom.name();
            if (kb.kbCache.transInstOf(pred, "VariableArityRelation")) {
                int arity = renamedArgs.size();
                String func = kb.kbCache.isInstanceOf(pred, "Function") ? Formula.FN_SUFF : "";
                String suffix = "__" + arity + func;
                if (!pred.endsWith(suffix)) {
                    kb.kbCache.copyNewPredFromVariableArity(pred + suffix, pred, arity);
                    head = new Expr.Atom(pred + suffix);
                }
            }
        }
        return new Expr.SExpr(head, renamedArgs);
    }

    /** ***************************************************************
     * Expr-based preprocessing: extended fast path that handles predicate
     * variables (Phase A), row variables (Phase B), and type restrictions
     * (Phase C) entirely within the Expr AST — no string round-trips.
     *
     * <p>Phase A — Predicate variable expansion:
     * If {@code fa.predVarCache} is non-empty,
     * {@link com.articulate.sigma.parsing.PredVarInst#processOne}
     * instantiates every predicate variable at the Expr level via
     * {@link com.articulate.sigma.parsing.PredVarInst#substituteVar}.
     * The result is a set of FormulaASTs, each with a concrete relation
     * in place of each pred-var.
     *
     * <p>Phase B — Row variable expansion:
     * For each FormulaAST from Phase A that has row variables,
     * {@link RowVar#expandRowVarExpr} splices the expanded argument lists
     * directly into the Expr tree.
     *
     * <p>Phase C — Variable-arity renaming + type restrictions:
     * Unchanged from the original no-var fast path.
     *
     * @param fa      the FormulaAST (must have a non-null {@code expr})
     * @param isQuery {@code true} for query mode
     * @param kb      the knowledge base
     * @return set of preprocessed Expr trees ready for
     *         {@link com.articulate.sigma.parsing.ExprToTPTP#translate};
     *         empty if pred-var instantiation yields no results
     */
    public Set<Expr> preProcessExpr(FormulaAST fa, boolean isQuery, KB kb) {

        if (fa == null || fa.expr == null) return Set.of();

        // Phase A: Predicate variable expansion (must precede row-var expansion)
        Collection<FormulaAST> afterPredVar;
        if (fa.predVarCache != null && !fa.predVarCache.isEmpty()) {
            com.articulate.sigma.parsing.PredVarInst pvi = new com.articulate.sigma.parsing.PredVarInst(kb);
            afterPredVar = pvi.processOne(fa); // returns List — no hashCode() calls
            if (afterPredVar == null)
                return Set.of(); // null = formula marked "reject" by processOne
            if (afterPredVar.isEmpty())
                afterPredVar = List.of(fa); // empty = no KB instances found for this pred-var type;
                                             // keep original (mirrors string-path: accumulator.add(f))
        } else {
            afterPredVar = List.of(fa);
        }

        // Phase B: Row variable expansion
        // Use TreeSet sorted by KIF string so Phase C iterates axioms in deterministic order.
        Set<Expr> afterRowVar = new TreeSet<>(Comparator.comparing(Expr::toKifString));
        // predVarInstDone must be true before constructing RowVar (see RowVar constructor)
        com.articulate.sigma.parsing.PredVarInst.predVarInstDone = true;
        RowVar rv = new RowVar(kb);
        for (FormulaAST fa2 : afterPredVar) {
            if (fa2.expr == null) continue;
            boolean hasRows = (fa2.rowVarCache != null && !fa2.rowVarCache.isEmpty())
                           || (fa.rowVarCache != null && !fa.rowVarCache.isEmpty());
            if (hasRows) {
                // When row-var expansion is not applicable (HOL), keep the
                // original expr without expansion — mirrors string-path: if expandRowVars() returns
                // null/empty, accumulator.add(f) keeps the original formula.
                // Note: containsNumber is NOT a barrier here; the string path expands row vars
                // even in numeric formulas (e.g. GreatestCommonDivisorFn @ROW with literal 0).
                if (fa2.higherOrder) {
                    afterRowVar.add(fa2.expr);
                    continue;
                }
                Set<Expr> expanded = rv.expandRowVarExpr(fa2);
                // empty result means arity conflict → drop this formula (do not fall back to
                // the unexpanded original, which still contains raw row variables)
                afterRowVar.addAll(expanded);
                if (afterRowVar.size() > AXIOM_EXPANSION_LIMIT) {
                    System.err.println("Error in FormulaPreprocessor.preProcessExpr(): " +
                            "AXIOM_EXPANSION_LIMIT EXCEEDED: " + AXIOM_EXPANSION_LIMIT);
                    break;
                }
            } else {
                // No row vars: pass through regardless of HOL/containsNumber status
                afterRowVar.add(fa2.expr);
            }
        }
        if (afterRowVar.isEmpty()) return Set.of();

        // Phase C: Variable-arity renaming + type restrictions (unchanged from no-var fast path)
        // LinkedHashSet preserves the sorted order established by afterRowVar's TreeSet.
        Set<Expr> results = new LinkedHashSet<>();
        KBmanager mgr = KBmanager.getMgr();
        boolean typePrefix = mgr.getPref("typePrefix").equalsIgnoreCase("yes");
        for (Expr e : afterRowVar) {
            Expr renamed = renameVariableArityInExpr(e, kb);
            if (typePrefix && !isQuery) {
                Map<String, Set<String>> varmap = findTypeRestrictionsExpr(renamed, kb);
                results.add(addTypeRestrictionsExpr(renamed, varmap, kb));
            } else {
                results.add(renamed);
            }
        }
        return results;
    }

    /** ***************************************************************
     * Bridge overload: wraps a bare {@link Expr} in a minimal
     * {@link FormulaAST} (no pred-var / row-var caches) and delegates to
     * {@link #preProcessExpr(FormulaAST, boolean, KB)}.  Keeps existing
     * callers and tests working without change.
     *
     * @param expr    the Expr tree from {@link FormulaAST#expr}
     * @param isQuery {@code true} for query mode
     * @param kb      the knowledge base
     * @return set of preprocessed Expr trees ready for
     *         {@link com.articulate.sigma.parsing.ExprToTPTP#translate}
     */
    public Set<Expr> preProcessExpr(Expr expr, boolean isQuery, KB kb) {
        FormulaAST fa = new FormulaAST();
        fa.expr = expr;
        return preProcessExpr(fa, isQuery, kb);
    }

    public Set<Formula> preProcess(Formula form, boolean isQuery, KB kb) {

        if (debug) System.out.println("INFO in FormulaPreprocessor.preProcess(): form: " + form);
        Set<Formula> results = new HashSet<>();
//        FormulaPreprocessor fp = new FormulaPreprocessor();
        if (!StringUtil.emptyString(form.getFormula())) {
            KBmanager mgr = KBmanager.getMgr();
            if (!form.isBalancedList()) {
                String errStr = "Unbalanced parentheses or quotes in: " + form.getFormula();
                System.err.println("Error in preProcess(): " + errStr);
                form.errors.add(errStr);
                return results;
            }
            boolean ignoreStrings = false;
            boolean translateIneq = true;
            boolean translateMath = true;
            Formula f = new Formula();
            f.read(form.getFormula());
            f.startLine = form.startLine;
            if (StringUtil.containsNonAsciiChars(f.getFormula()))
                f.read(StringUtil.replaceNonAsciiChars(f.getFormula()));

            boolean addHoldsPrefix = mgr.getPref("holdsPrefix").equalsIgnoreCase("yes");
            List<Formula> variableReplacements = replacePredVarsAndRowVars(form,kb, addHoldsPrefix);
            if (debug) System.out.println("FormulaPreprocessor.preProcess(): just after replacePredVarsAndRowVars() ");
            form.errors.addAll(f.getErrors());

            if (debug) System.out.println("FormulaPreprocessor.preProcess(): variableReplacements: " + variableReplacements);
            List<Formula> accumulator = addInstancesOfSetOrClass(form,kb, isQuery, variableReplacements);
            if (debug) System.out.println("FormulaPreprocessor.preProcess(): accumulator: " + accumulator);
            // Iterate over the formulae resulting from predicate variable instantiation and row variable expansion,
            // passing each to preProcessRecurse for further processing.
            if (!accumulator.isEmpty()) {
                String theNewFormula;
                for (Formula fnew : accumulator) {
                    theNewFormula = preProcessRecurse(fnew,"",ignoreStrings,translateIneq,translateMath,kb);
                    fnew.read(theNewFormula);
                    //if (debug) System.out.println("preProcess: fnew: " + fnew);
                    form.errors.addAll(fnew.getErrors());
                    fnew.sourceFile = form.sourceFile;
                    if (!StringUtil.emptyString(theNewFormula))
                        results.add(fnew);
                    if (debug) System.out.println("FormulaPreprocessor.preProcess(): results: " + results);
                }
            }
        }
        if (debug) System.out.println("INFO in FormulaPreprocessor.preProcess(): 1 result: " + results);

        // If typePrefix==yes and isQuery==false, add a "sortal" antecedent to every axiom
        KBmanager mgr = KBmanager.getMgr();
        boolean typePrefix = mgr.getPref("typePrefix").equalsIgnoreCase("yes");
        //System.out.println("INFO in FormulaPreprocessor.preProcess(): type prefix: " + typePrefix);
        //System.out.println("INFO in FormulaPreprocessor.preProcess(): !isQuery: " + !isQuery);
        if (typePrefix && !isQuery) {
            Formula fnew;
            for (Formula f : results) {
                if (debug) System.out.println("INFO in FormulaPreprocessor.preProcess(): form: " + f);
                fnew = f;
                //if (addTypes)
                    fnew.read(addTypeRestrictions(f,kb).getFormula());
                //else
                //    if (debug) System.out.println("preProcess(): not adding types");
                f.read(fnew.getFormula());
                f.higherOrder = fnew.higherOrder;
                f.varTypeCache = fnew.varTypeCache;
                if (debug) System.out.println("INFO in FormulaPreprocessor.preProcess(): varTypeCache: " + f.varTypeCache);
            }
        }

        if (debug) System.out.println("INFO in FormulaPreprocessor.preProcess(): 2 result: " + results);
        return results;
    }

    /** ***************************************************************
     */
    public static void testFindTypes() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

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
        System.out.println("Explicit types: " + fp.findExplicitTypesInAntecedent(kb,f));
    }

    /** ***************************************************************
     */
    public static void testFindExplicit() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
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
        System.out.println("FormulaPreprocessor.testExplicit(): " + var + Formula.SPACE + cl);
        System.out.println("Explicit types: " + fp.findExplicitTypesInAntecedent(kb, f));
    }

    /** ***************************************************************
     */
    public static void testAddTypes() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        System.out.println();
        String strf = "(=> (forall (?ELEMENT) (<=> (element ?ELEMENT ?SET1) " +
                "(element ?ELEMENT ?SET2))) (equal ?SET1 ?SET2))";
        Formula f = new Formula();
        f.read(strf);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        //FormulaPreprocessor.debug = true;
        System.out.println(fp.addTypeRestrictions(f,kb));

        System.out.println();
        strf = "(=> (and (attribute ?AREA LowTerrain) (part ?ZONE ?AREA)" +
                " (slopeGradient ?ZONE ?SLOPE)) (greaterThan 0.03 ?SLOPE))";
        f.read(strf);
        fp = new FormulaPreprocessor();
        System.out.println(fp.addTypeRestrictions(f,kb));

        System.out.println();
        strf = "(=> (and (typicalPart ?PART ?WHOLE) (instance ?X ?PART) " +
                "(equal ?PARTPROB (ProbabilityFn (exists (?Y) (and " +
                "(instance ?Y ?WHOLE) (part ?X ?Y))))) (equal ?NOTPARTPROB " +
                "(ProbabilityFn (not (exists (?Z) (and (instance ?Z ?WHOLE) " +
                "(part ?X ?Z))))))) (greaterThan ?PARTPROB ?NOTPARTPROB))";
        f.read(strf);
        fp = new FormulaPreprocessor();
        System.out.println(fp.addTypeRestrictions(f,kb));
    }

    /** ***************************************************************
     */
    public static void testOne() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        System.out.println();
        System.out.println();
        FormulaPreprocessor fp;
        String strf = "(=>\n" +
                "    (equal\n" +
                "        (GreatestCommonDivisorFn @ROW) ?NUMBER)\n" +
                "    (forall (?ELEMENT)\n" +
                "        (=>\n" +
                "            (inList ?ELEMENT\n" +
                "                (ListFn @ROW))\n" +
                "            (equal\n" +
                "                (RemainderFn ?ELEMENT ?NUMBER) 0))))";
        Formula f = new Formula();
        f.read(strf);
        fp = new FormulaPreprocessor();
        //System.out.println(fp.findType(1,"part",kb));
        System.out.println(fp.preProcess(f,false,kb));
    }

    /** ***************************************************************
     */
    public static void testTwo() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        System.out.println();
        System.out.println();
        FormulaPreprocessor fp;
        String strf = "(equal (AbsoluteValueFn ?NUMBER1) 2)";
        Formula f = new Formula();
        f.read(strf);
        fp = new FormulaPreprocessor();
        System.out.println("testTwo(): equality: " + fp.preProcess(f,false,kb));
    }

    /** ***************************************************************
     */
    public static void testThree() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        System.out.println();
        System.out.println();
        FormulaPreprocessor fp;
        String strf = "\n" +
                "(<=>\n" +
                "    (and\n" +
                "        (equal\n" +
                "            (AbsoluteValueFn ?NUMBER1) ?NUMBER2)\n" +
                "        (instance ?NUMBER1 RealNumber)\n" +
                "        (instance ?NUMBER2 RealNumber))\n" +
                "    (or\n" +
                "        (and\n" +
                "            (instance ?NUMBER1 NonnegativeRealNumber)\n" +
                "            (equal ?NUMBER1 ?NUMBER2))\n" +
                "        (and\n" +
                "            (instance ?NUMBER1 NegativeRealNumber)\n" +
                "            (equal ?NUMBER2\n" +
                "                (SubtractionFn 0 ?NUMBER1)))))";
        Formula f = new Formula();
        f.read(strf);
        fp = new FormulaPreprocessor();
        System.out.println("testThree(): " + fp.preProcess(f,false,kb));
    }

    /** ***************************************************************
     */
    public static void testFour() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        System.out.println();
        System.out.println();
        FormulaPreprocessor fp;
        String strf = "(forall (?NUMBER ?ELEMENT ?CLASS)\n" +
                "        (=>\n" +
                "          (equal ?ELEMENT\n" +
                "            (ListOrderFn\n" +
                "              (ListFn_1 ?FOO) ?NUMBER))\n" +
                "          (instance ?ELEMENT ?CLASS)))";
        Formula f = new Formula();
        f.read(strf);
        fp = new FormulaPreprocessor();
        System.out.println("testFour() signature for ListFn: " + kb.kbCache.signatures.get("ListFn"));
        System.out.println("testFour() valence for ListFn: " + kb.kbCache.valences.get("ListFn"));
        System.out.println("testFour() signature for ListFn_1: " + kb.kbCache.signatures.get("ListFn_1"));
        System.out.println("testFour() valence for ListFn_1: " + kb.kbCache.valences.get("ListFn_1"));
        System.out.println("testFour(): " + fp.addTypeRestrictions(f,kb));
    }

    /** ***************************************************************
     */
    public static void testFive() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        System.out.println();
        System.out.println();
        FormulaPreprocessor fp;
        String strf = "(equal (AdditionFn 1 2) ?X)";
        Formula f = new Formula();
        f.read(strf);
        fp = new FormulaPreprocessor();
        System.out.println("testFive(): equality: " + fp.preProcess(f,false,kb));
    }

    /** ***************************************************************
     */
    public static void test6() {

        System.out.println("------------------------------------");
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));

        if (!kb.terms.contains("avgWorkHours")) {
            System.out.println("FormulaPreprocessor.test6(): Demographics.kif not loaded");
            return;
        }
        System.out.println();
        System.out.println();
        String strf = "(=>\n" +
                "  (avgWorkHours ?H ?N)\n" +
                "  (lessThan ?N 70.0))";
        Formula f = new Formula();
        f.read(strf);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        debug = true;
        System.out.println("test6(): " + fp.preProcess(f,false,kb));
    }

    /** ***************************************************************
     */
    public static void main(String[] args) {

        //testOne();
        //testTwo();
        //testThree();
        //testFour();

        test6();
        //testFindTypes();
        //testAddTypes();
        //testFindExplicit();
    }

}
