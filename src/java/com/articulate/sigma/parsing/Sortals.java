package com.articulate.sigma.parsing;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBcache;
import com.articulate.sigma.utils.StringUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Add type guards to formulas
public class Sortals {

    private final KB kb;

    public boolean debug = false;
    public long disjointTime = 0;

    /** ***************************************************************
     */
    public Sortals(KB kbin) {
        kb = kbin;
    }

    /** ***************************************************************
     * Add type guards to a formula by making it the consequent of a rule
     * and making type tests into a new antecedent
     */
    public String addSortals(FormulaAST f, Map<String, Set<String>> types) {

        if (types.keySet().isEmpty()) return f.getFormula();
        if (debug) System.out.println("Sortals.addSortals(): types: " + types);
        StringBuilder result = new StringBuilder();
        if (!types.keySet().isEmpty())
            result.append("(=> ");
        if (types.keySet().size() > 1)
            result.append("(and ");

        Set<String> v;
        for (String k : types.keySet()) {
            v = types.get(k);
            for (String t : v) {
                if (t.endsWith("+"))
                    result.append("(subclass ").append(k).append(Formula.SPACE).append(t.substring(0, t.length() - 1)).append(") ");
                else
                    result.append("(instance ").append(k).append(Formula.SPACE).append(t).append(") ");
            }
        }
        if (!types.keySet().isEmpty())
            result.deleteCharAt(result.length()-1);
        if (types.keySet().size() > 1)
            result.append(") ");
        result.append(f.getFormula());
        result.append(Formula.RP);
        if (debug) System.out.println("Sortals.addSortals(): result: " + result);
        return result.toString();
    }

    /** ***************************************************************
     * Find the most specific type in a list of types.  This assumes that
     * the list has already been tested for disjointness
     */
    public String mostSpecificType(Set<String> types) {

        if (types.size() == 1)
            return types.iterator().next();
        long start = System.currentTimeMillis();
        if (kb.kbCache.checkDisjoint(kb,types)) {
            System.err.println("Error in Sortals.mostSpecificType(): disjoint type spec: " + types);
            KBcache.errors.clear();
            return "";
        }
        long end = (System.currentTimeMillis()-start);
        disjointTime = disjointTime + end;
        return kb.kbCache.getCommonChild(types);
    }

    /** ***************************************************************
     * if variables in a formula has several possible type constraints,
     * based on their being arguments to relations, find the most
     * specific type for each

    public Map<String, String> mostSpecificTypes(Map<String, Set<String>> vmap) {

        Map<String, String> themap = new HashMap<>();
        for (String var : vmap.keySet()) {
            themap.put(var,mostSpecificType(vmap.get(var)));
        }
        return themap;
    }
*/
    /** ***************************************************************
     * If a type is already specified for a variable in a rule with an
     * instance or subclass statement, remove it from the type list so
     * that it won't be added as a type guard
     */
    public Map<String, Set<String>> removeExplicitTypes(Map<String,Set<String>> typesMap,
                                                       Map<String, Set<String>> explicit) {

        Map<String, Set<String>> result = new HashMap<>();
        Set<String> expTypes, types, newtypes;
        for (String var : typesMap.keySet()) {
            expTypes = explicit.get(var);
            types = typesMap.get(var);
            newtypes = new HashSet<>();
            if (expTypes == null)
                newtypes.addAll(types);
            else {
                for (String t : types) {
                    if (!expTypes.contains(t))
                        newtypes.add(t);
                }
            }
            if (!newtypes.isEmpty())
                result.put(var, newtypes);
        }
        return result;
    }

    /** ***************************************************************
     * Eliminate more general types in favor of their more specific
     * subclasses (if any)
     */
    public void elimSubsumedTypes(FormulaAST f) {

        Set<String> types, remove;
        for (String var : f.varTypes.keySet()) {
            types = f.varTypes.get(var);
            remove = new HashSet<>();
            for (String type1 : types) {
                for (String type2 : types) {
                    if (!StringUtil.emptyString(type1) && !StringUtil.emptyString(type2) && !type1.equals(type2)) {
                        if (kb.kbCache.subclassOf(type1, type2))
                            remove.add(type2);
                        if (kb.kbCache.subclassOf(type2,type1))
                            remove.add(type1);
                    }
                }
            }
            types.removeAll(remove);
        }
    }

    /** ***************************************************************
     * Find the most specific type constraint for each variable
     */
    public void winnowAllTypes(FormulaAST f) {

        if (debug) System.out.println("Sortals.winnowAllTypes():input: " + f.varTypes);
        elimSubsumedTypes(f);
        if (debug) System.out.println("Sortals.winnowAllTypes():output: " + f.varTypes);
    }

    /** ***************************************************************
     * Find the most specific type constraint for each variable and
     * create a new String of the formula with type guards
     */
    public String addSortals(FormulaAST f) {

        if (debug) System.out.println("Sortals.addSortals():types: " + f.varTypes);
        Map<String, Set<String>> types = removeExplicitTypes(f.varTypes,f.explicitTypes);
        if (debug) System.out.println("Sortals.addSortals():after removeExplicitTypes: " + f.varTypes);
        String result = addSortals(f,types);
        f.setFormula(result);
        return result;
    }
}
