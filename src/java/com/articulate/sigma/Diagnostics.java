package com.articulate.sigma;

/** This code is copyright Articulate Software (c) 2003.
 This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 Users of this code also consent, by use of this code, to credit Articulate Software
 and Teknowledge in any writings, briefings, publications, presentations, or
 other representations of any software which incorporates, builds on, or uses this
 code.  Please cite the following article in any publication with references:

 Pease, A., (2003). The Sigma Ontology Development Environment,
 in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 August 9, Acapulco, Mexico.
 */

import com.articulate.sigma.trans.TPTP3ProofProcessor;
import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/** *****************************************************************
 * A class that finds problems in a knowledge base.  It is not meant
 * to be instantiated.
 */
public class Diagnostics {

    public static boolean debug = false;

    public static List<String> LOG_OPS = Arrays.asList(Formula.AND,Formula.OR,Formula.XOR,Formula.NOT,Formula.EQUANT,
            Formula.UQUANT,Formula.IF,Formula.IFF, "holds");

    public static Map<String, Set<String>> varLinksParentMap = new HashMap<>(); // parent map for getVariableLinks(Formula f, KB kb)

    private static final int RESULT_LIMIT = 100;

    /** *****************************************************************
     * Return a list of terms (for a given argument position) that do not
     * have a specified relation.
     * @param kb the knowledge base
     * @param rel the relation name
     * @param argnum the argument position of the term
     * @param letter the first letter of the term name
     */
    public static List<String> termsWithoutRelation(KB kb, String rel, int argnum,
                                                    char letter) {

        List<String> result = new ArrayList<>();
        List<Formula> forms;
        Iterator<Formula> it2;
        Formula formula;
        String pred;
        for (String term : kb.getTerms()) {
            if (LOG_OPS.contains(term) || StringUtil.isNumeric(term))  // Exclude the logical operators and numbers
                continue;
            forms = kb.ask("arg",argnum,term);
            if (forms == null || forms.isEmpty()) {
                if (letter < 'A' || term.charAt(0) == letter)
                    result.add(term);
            }
            else {
                boolean found = false;
                it2 = forms.iterator();
                while (it2.hasNext()) {
                    formula = (Formula) it2.next();
                    if (formula != null) {
                        pred = formula.car();
                        if (pred.equals(rel)) {
                            found = true;
                            break;
                        }
                    }
                    else
                        System.err.println("Error in Diagnostics.termsWithoutRelation(): null formula for: " + term);
                }
                if (!found) {
                    if (letter < 'A' || term.charAt(0) == letter)
                        result.add(term);
                }
            }
            if (RESULT_LIMIT > 0 && result.size() > RESULT_LIMIT) {
                result.add("limited to " + RESULT_LIMIT + " results");
                break;
            }
        }
        return result;
    }

    /** *****************************************************************
     * Return a list of terms that do not have a documentation string.
     */
    public static List termsWithoutDoc(KB kb) {

        System.out.println("INFO in Diagnostics.termsWithoutDoc(): ");
        return termsWithoutRelation(kb,"documentation",1,' ');
    }

    /** *****************************************************************
     * Return a list of terms that have more than one documentation string.
     */
    public static List<String> termsWithMultipleDoc(KB kb) {

        Set<String> result = new HashSet();
        Set<String> withDoc = new HashSet();
        List<Formula> forms = kb.ask("arg", 0, "documentation");
        String term, key;
        double dval;
        if (!forms.isEmpty()) {
            boolean isNaN;
            for (Formula f : forms) {
                term = f.getStringArgument(1);   // Append term and language to make a key.
                isNaN = true;
                try {
                    dval = Double.parseDouble(term);
                    isNaN = Double.isNaN(dval);
                }
                catch (NumberFormatException nex) {
                }
                if (isNaN) {
                    key = (term + f.getArgument(2));
                    if (withDoc.contains(key))
                        result.add(term);
                    else
                        withDoc.add(key);
                }
                if (RESULT_LIMIT > 0 && result.size() > RESULT_LIMIT) {
                    result.add("limited to 100 results");
                    break;
                }
            }
        }
        return new ArrayList<>(result);
    }

    /** *****************************************************************
     * Returns true if term has an explicitly stated parent, or a
     * parent can be inferred from the transitive relation caches,
     * else returns false.

     private static boolean hasParent(KB kb, String term) {

     Iterator<String> it = preds.iterator();
     while (it.hasNext()) {
     String pred = it.next();
     Map<String,Set<String>> predvals = kb.kbCache.parents.get(term);
     if (predvals != null) {
     Set<String> cached = predvals.get(term);
     if ((cached != null) && !cached.isEmpty())
     return true;
     }
     }
     return false;
     }
     */
    /** *****************************************************************
     * Return a list of terms that do not have Entity as a parent term.
     */
    public static boolean termNotBelowEntity(String term, KB kb) {

        if (LOG_OPS.contains(term) || term.equals(Formula.EQUAL) || term.equals("Entity") || StringUtil.isNumeric(term))
            return false;
        else if (!kb.kbCache.subclassOf(term,"Entity") && !kb.kbCache.transInstOf(term,"Entity"))
            return true;
        return false;
    }

    /** *****************************************************************
     * Return a list of terms that do not have Entity as a parent term.
     */
    public static List<String> termsNotBelowEntity(KB kb) {

        System.out.println("INFO in Diagnostics.termsNotBelowEntity(): ");
        List<String> result = new ArrayList<>();
        int count = 0;
        String term;
        Iterator<String> it = kb.getTerms().iterator();
        while (it.hasNext() && (RESULT_LIMIT < 1 || count < RESULT_LIMIT)) {
            term = it.next();
            if (!termNotBelowEntity(term,kb))
                continue;
            else {
                result.add(term);
                count++;
            }
            if (RESULT_LIMIT > 0 && count > RESULT_LIMIT)
                result.add("limited to 100 results");
        }
        return result;
    }

    /** *****************************************************************
     * Return a list of terms that have parents which are disjoint.
     */
    public static List<String> childrenOfDisjointParents(KB kb) {

        List<String> result = new ArrayList<>();

        /*
        int count = 0;
        Iterator<String> it = kb.getTerms().iterator();
        while (it.hasNext()) {
            boolean contradiction = false;
            String term = it.next();
            boolean isNaN = true;
            try {
                double dval = Double.parseDouble(term);
                isNaN = Double.isNaN(dval);
            }
            catch (Exception nex) {
            }
            if (isNaN) {
                HashSet<String> parentSet = kb.kbCache.getParentClasses(term);
                Object[] parents = null;
                if ((parentSet != null) && !parentSet.isEmpty())
                    parents = parentSet.toArray();
                if (parents != null) {
                    for (int i = 0 ; (i < parents.length) && !contradiction ; i++) {
                        String termX = (String) parents[i];
                        Set<String> disjoints = kb.kbCache.getCachedRelationValues("disjoint", termX, 1, 2);
                        if ((disjoints != null) && !disjoints.isEmpty()) {
                            for (int j = (i + 1) ; j < parents.length ; j++) {
                                String termY = (String) parents[j];
                                if (disjoints.contains(termY)) {
                                    result.add(term);
                                    contradiction = true;
                                    count++;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (resultLimit > 0 && count > resultLimit) {
                result.add("limited to 100 results");
                break;
            }
        }
        */
        return result;
    }

    /** *****************************************************************
     * Returns a list of terms, each of which is an instance of some
     * exhaustively decomposed class but is not an instance of any of
     * the subclasses that constitute the exhaustive decomposition.
     * For example, given (instance E A) and (partition A B C D), then
     * E is included in the list of terms to be returned if E is not a
     * instance of B, C, or D.
     */
    public static List<String> membersNotInAnyPartitionClass(KB kb) {

        List<String> result = new ArrayList<>();
        try {
            Set<String> reduce = new TreeSet<>();
            // Use all partition statements and all
            // exhaustiveDecomposition statements.
            List<Formula> forms = kb.ask("arg",0,"partition");
            if (forms == null)
                forms = new ArrayList<>();
            List<Formula> forms2 = kb.ask("arg",0,"exhaustiveDecomposition");
            if (forms2 != null)
                forms.addAll(forms2);
            boolean go = true;
            Iterator<Formula> it = forms.iterator();
            Formula form;
            String parent, inst;
            List<String> partition, instances;
            boolean isInstanceSubsumed, isNaN;
            Iterator<String> it2;
            double dval;
            while (go && it.hasNext()) {
                form = it.next();
                parent = form.getStringArgument(1);
                partition = form.argumentsToArrayListString(2);
                instances = kb.getTermsViaPredicateSubsumption("instance",2,parent,1,true);
                if ((instances != null) && !instances.isEmpty()) {
                    it2 = instances.iterator();
                    while (go && it2.hasNext()) {
                        isInstanceSubsumed = false;
                        isNaN = true;
                        inst = it2.next();
                        try {   // For diagnostics, try to avoid treating numbers as bonafide terms.
                            dval = Double.parseDouble(inst);
                            isNaN = Double.isNaN(dval);
                        }
                        catch (NumberFormatException nex) {
                        }
                        if (isNaN) {
                            for (String pclass : partition) {
                                if (kb.isInstanceOf(inst, pclass)) {
                                    isInstanceSubsumed = true;
                                    break;
                                }
                            }
                            if (isInstanceSubsumed)
                                continue;
                            else
                                reduce.add(inst);
                        }
                        if (RESULT_LIMIT < 1 || reduce.size() > RESULT_LIMIT)
                            go = false;
                    }
                }
            }
            result.addAll(reduce);
            if (RESULT_LIMIT > 0 && result.size() > RESULT_LIMIT)
                result.add("limited to 100 results");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *****************************************************************
     * Find all relational terms that are missing an NLG format expression
     */
    public static List<String> relationsWithoutFormat(KB kb) {

        List<String> result = new ArrayList<>();
        List<Formula> forms;
        for (String rel : kb.kbCache.relations) {
            forms = kb.askWithRestriction(0,"format",2,rel);
            if (forms == null || forms.isEmpty())
                result.add(rel);
        }
        return result;
    }

    /** *****************************************************************
     * Term does not appear in any implication (rule).
     */
    public static boolean termWithoutRules(KB kb, String term) {

        boolean isNaN = true;
        try {
            double dval = Double.parseDouble(term);
            isNaN = Double.isNaN(dval);
        }
        catch (NumberFormatException nex) {
        }
        if (isNaN) {
            List<Formula> forms = kb.ask("ant",0,term);
            List<Formula> forms2 = kb.ask("cons",0,term);
            if (((forms == null) || forms.isEmpty())
                    && ((forms2 == null) || forms2.isEmpty()))
                return true;
        }
        return false;
    }

    /** *****************************************************************
     * Find all terms which do not appear in any implication (rule).
     */
    public static List<String> termsWithoutRules(KB kb) {

        boolean isNaN = true;
        List<String> result = new ArrayList<>();
        for (String term : kb.getTerms()) {
            isNaN = true;
            if (termWithoutRules(kb,term))
                result.add(term);
            if (RESULT_LIMIT > 0 && result.size() > RESULT_LIMIT) {
                result.add("limited to 100 results");
                break;
            }
        }
        return result;
    }

    /** *****************************************************************
     * @return a list of variables used only once.
     */
    public static Set<String> singleUseVariables(Formula f) {

        Set<String> result = new HashSet<>();
        Set<String> vars = f.collectAllVariables();
        int index, index2;
        for (String v : vars) {
            index = f.getFormula().indexOf(v);
            index2 = f.getFormula().indexOf(v,index+v.length());
            if (index2 == -1)
                result.add(v);
        }
        return result;
    }

    /** *****************************************************************
     * @return a list of variables only in the consequent that are
     * unquantified
     * TODO: if there's an implication in the consequent, test if
     * the interior consequent has a variable not found in the interior
     * antecedent
     */
    public static Set<String> unquantInConsequent(Formula f) {

        Set<String> result = new HashSet<>();
        if (!f.isRule())
            return result;
        Formula ante = new Formula(FormulaUtil.antecedent(f));
        Formula conseq = new Formula(FormulaUtil.consequent(f));

        Set<String> anteVars = ante.collectUnquantifiedVariables();
        Set<String> consVars = conseq.collectUnquantifiedVariables();
        if (consVars.isEmpty())
            return result;
        consVars.removeAll(anteVars);
        result.addAll(consVars);
        return result;
    }

    /** *****************************************************************
     */
    public static List<Formula> unquantsInConseq(KB kb) {

        List<Formula> result = new ArrayList<>();
        for (Formula form : kb.formulaMap.values()) {
            if ((form.getFormula().contains(Formula.UQUANT))
                    || (form.getFormula().contains(Formula.EQUANT))) {
                if (!unquantInConsequent(form).isEmpty())
                    result.add(form);
            }
            if (RESULT_LIMIT > 0 && result.size() > RESULT_LIMIT)
                return result;
        }
        return result;
    }

    /** *****************************************************************
     * @return true if a quantifiers in a quantifier list is not found
     * in the body of the statement.
     */
    public static boolean quantifierNotInStatement(Formula f) {

        if (f.getFormula() == null || f.getFormula().length() < 1 ||
                !f.listP() || f.empty())
            return false;
        if (!Arrays.asList(Formula.UQUANT, Formula.EQUANT).contains(f.car())) {
            Formula f1 = new Formula();
            f1.read(f.car());
            Formula f2 = new Formula();
            f2.read(f.cdr());
            return (quantifierNotInStatement(f1) || quantifierNotInStatement(f2));
        }
        Formula form = new Formula();
        form.read(f.getFormula());
        if (form.car() != null && form.car().length() > 0) {    // This test shouldn't be needed.
            String rest = form.cdr();                   // Quantifier list plus rest of statement
            Formula quant = new Formula();
            quant.read(rest);
            String q = quant.car();                     // Now just the quantifier list.
            String body = quant.cdr();
            quant.read(q);
            List<String> qList = quant.argumentsToArrayListString(0);  // Put all the quantified variables into a list.
            if (rest.contains(Formula.EQUANT) || rest.contains(Formula.UQUANT)) { //nested quantifiers
                Formula restForm = new Formula();
                restForm.read(rest);
                restForm.read(restForm.cdr());
                if (quantifierNotInStatement(restForm))
                    return true;
            }
            String var;
            if (qList != null) {
                for (int i = 0; i < qList.size(); i++) {
                    var = (String) qList.get(i);
                    if (!body.contains(var))
                        return true;
                }
            }
        }
        return false;
    }

    /** *****************************************************************
     * Find cases where a variable appears in a quantifier list, but not
     * in the body of the quantified expression.  For example
     * (exists (?FOO) (bar ?FLOO Shmoo))
     * @return an ArrayList of Formula(s).
     */
    public static List<Formula> quantifierNotInBody(KB kb, String fname) {

        List<Formula> result = new ArrayList<>();
        Iterator<Formula> it = kb.formulaMap.values().iterator();
        Formula form;
        while (it.hasNext()) {
            form = (Formula) it.next();
            if (!FileUtil.noPath(form.sourceFile).equals(fname))
                continue;
            if ((form.getFormula().contains(Formula.UQUANT))
                    || (form.getFormula().contains(Formula.EQUANT))) {
                if (quantifierNotInStatement(form))
                    result.add(form);
            }
            if (RESULT_LIMIT > 0 && result.size() > RESULT_LIMIT)
                return result;
        }
        return result;
    }

    /** *****************************************************************
     * Find cases where a variable appears in a quantifier list, but not
     * in the body of the quantified expression.  For example
     * (exists (?FOO) (bar ?FLOO Shmoo))
     * @return an ArrayList of Formula(s).
     */
    public static List<Formula> quantifierNotInBody(KB kb) {

        List<Formula> result = new ArrayList<>();
        Iterator<Formula> it = kb.formulaMap.values().iterator();
        Formula form;
        while (it.hasNext()) {
            form = (Formula) it.next();
            if ((form.getFormula().contains(Formula.UQUANT))
                    || (form.getFormula().contains(Formula.EQUANT))) {
                if (quantifierNotInStatement(form))
                    result.add(form);
            }
            if (RESULT_LIMIT > 0 && result.size() > RESULT_LIMIT)
                return result;
        }
        return result;
    }

    /** *****************************************************************
     * Add a key to a map and a value to the ArrayList corresponding
     * to the key.  Results are a side effect.
     */
    public static void addToMapList(Map<String,List<String>> m, String key, String value) {

        List<String> al = m.get(key);
        if (al == null) {
            al = new ArrayList<>();
            m.put(key,al);
        }
        if (!al.contains(value))
            al.add(value);
    }

    /** *****************************************************************
     * Add a key to a map and a key, value to the map
     * corresponding to the key.  Results are a side effect.
     */
    public static void addToDoubleMapList(Map<String,Map<String,List<String>>> m, String key1, String key2, String value) {

        Map<String,List<String>> tm = m.get(key1);
        if (tm == null) {
            tm = new TreeMap<>();
            m.put(key1,tm);
        }
        addToMapList(tm,key2,value);
    }

    /** *****************************************************************
     * Find all the terms used and defined in a KB.  Terms are defined by
     * their appearance in definitionalRelations
     */
    private static void termLinks(KB kb, Map<String,List<String>> termsUsed, Map<String,List<String>> termsDefined) {

        List<String> definitionalRelations = Arrays.asList("instance", "subclass",
                "subAttribute", "domain", "domainSubclass", "range",
                "rangeSubclass", "documentation", "subrelation");

        List<Formula> forms, newform;
        String relation, filename;
        Formula form;
        for (String term : kb.getTerms()) {
            forms = kb.ask("arg",1,term);
            // Get every formula with the term as arg 1
            // Only definitional uses are in the arg 1 position
            if (forms != null && !forms.isEmpty()) {
                for (Formula formula : forms) {
                    relation = formula.getStringArgument(0);
                    filename = formula.sourceFile;
                    if (definitionalRelations.contains(relation))
                        addToMapList(termsDefined,term,filename);
                    else
                        addToMapList(termsUsed,term,filename);
                }
            }
            forms = kb.ask("arg",2,term);
            for (int i = 3; i < 7; i++) {
                newform = kb.ask("arg",i,term);
                if (newform != null)
                    forms.addAll(newform);
            }
            newform = kb.ask("ant",-1,term);
            if (newform != null)
                forms.addAll(newform);
            newform = kb.ask("cons",-1,term);
            if (newform != null)
                forms.addAll(newform);
            newform = kb.ask("stmt",-1,term);
            if (newform != null)
                forms.addAll(newform);
            if (forms != null && !forms.isEmpty()) {
                for (int i = 0; i < forms.size(); i++) {
                    form = (Formula) forms.get(i);
                    filename = form.sourceFile;
                    addToMapList(termsUsed,term,filename);
                }
            }
        }
    }

    /** *****************************************************************
     */
    private static void fileLinks(KB kb, Map<String,List<String>> fileDefines, Map<String,List<String>> fileUses,
                                  Map<String,List<String>> termsUsed, Map<String,List<String>> termsDefined) {

        Iterator<String> it = termsUsed.keySet().iterator();
        String key, value;
        List<String> values;
        while (it.hasNext()) {
            key = it.next();
            values = termsUsed.get(key);
            for (int i = 0; i < values.size(); i++) {
                value = (String) values.get(i);
                addToMapList(fileUses,value,key);
            }
        }
        it = termsDefined.keySet().iterator();
        while (it.hasNext()) {
            key = it.next();
            values = termsDefined.get(key);
            for (int i = 0; i < values.size(); i++) {
                value = (String) values.get(i);
                addToMapList(fileDefines,value,key);
            }
        }
    }

    /** *****************************************************************
     * Return a list of terms that have basic definitional
     * information (instance, subclass, domain, subrelation,
     * documentation) in a KB constituent that also uses terms
     * defined in another file, which would entail a mutual file
     * dependency, rather than a hierarchy of files.
     * @return a TreeMap of file name keys and an ArrayList of the
     *         files on which it depends. The interior TreeMap file
     *         name keys index ArrayLists of terms.  file -depends
     *         on->filenames -that defines-> terms
     */
    private static Map<String,Map<String,List<String>>> termDependency(KB kb) {

        System.out.println("INFO in Diagnostics.termDependency()");

        // A map of terms keys with an ArrayList as values listing files
        // in which the term is used.
        Map<String,List<String>> termsUsed = new TreeMap<>();

        // A map of terms keys with an ArrayList as values listing files
        // in which the term is defined (meaning appearance in an
        // instance, subclass, domain, subrelation, or documentation statement).
        //
        Map<String,List<String>> termsDefined = new TreeMap<>();

        // A map of file names and ArrayList values listing term names defined
        // in the file;
        Map<String,List<String>> fileDefines = new TreeMap<>();

        // A map of file names and ArrayList values listing term names used but not defined
        // in the file;
        Map<String,List<String>> fileUses = new TreeMap<>();

        // A map of file name keys and TreeMap values listing file names
        // on which the given file depends.  The interior TreeMap file name
        // keys index ArrayLists of terms.  file -depends on-> filenames -that defines-> terms
        Map<String,Map<String,List<String>>> fileDepends = new TreeMap<>();

        termLinks(kb,termsUsed,termsDefined);
        fileLinks(kb,fileDefines,fileUses,termsUsed,termsDefined);

        List<String> termUsedNames, fileDependencies;
        String term, fileDepend;
        for (String fileUsesName : fileUses.keySet()) {
            termUsedNames = fileUses.get(fileUsesName);
            for (int i = 0; i < termUsedNames.size(); i++) {
                term = (String) termUsedNames.get(i);
                fileDependencies = termsDefined.get(term);
                if (fileDependencies != null) {
                    for (int j = 0; j < fileDependencies.size(); j++) {
                        fileDepend = (String) fileDependencies.get(j);
                        if (!fileDepend.equals(fileUsesName))
                            addToDoubleMapList(fileDepends,fileUsesName,fileDepend,term);
                    }
                }
            }
        }
        return fileDepends;
    }

    /** *****************************************************************
     * Check the size of the dependency list.
     * @param depend is a map of file name keys and TreeMap values
     *               listing file names on which the given file
     *               depends. The interior TreeMap file name keys
     *               index ArrayLists of terms. file -depends on->
     *               filename -that defines-> terms
     */
    private static int dependencySize(Map<String,Map<String,List<String>>> depend, String f, String f2) {

        Map<String,List<String>> tm = depend.get(f2);
        List<String> al;
        if (tm != null) {
            al = (ArrayList) tm.get(f);
            if (al != null)
                return al.size();
        }
        return 0;
    }

    /** *****************************************************************
     * Show file dependencies.  If two files depend on each other,
     * show only the smaller list of dependencies, under the
     * assumption that that is the erroneous set.
     */
    public static String printTermDependency(KB kb, String kbHref) {

        // A list of String of filename1-filename2 of pairs already examined so that
        // the routine doesn't waste time examining filename2-filename1

        StringBuilder result = new StringBuilder();

        // A map of file name keys and TreeMap values listing file names
        // on which the given file depends.  The interior TreeMap file name
        // keys index ArrayLists of terms.  file -depends on-> filenames -that defines-> terms
        Map<String,Map<String,List<String>>> fileDepends = Diagnostics.termDependency(kb);
        System.out.println(fileDepends);
        Map<String,List<String>> tm;
        List<String> al;
        String term;
        int i;
        for (String f : fileDepends.keySet()) {
            // result.append("File " + f + " depends on: ");
            tm = fileDepends.get(f);
            for (String f2 : tm.keySet()) {
                al = tm.get(f2);

                if (al != null && al.size() < 40) {
                    result.append("<br/>File ").append(f).append(" dependency size on file ").append(f2).append(" is ").append(al.size()).append(" with terms:<br/>");
                    for (int ix = 0; ix < al.size(); ix++) {
                        term = (String) al.get(ix);
                        result.append("<a href=\"").append(kbHref).append("&term=").append(term).append("\">").append(term).append("</a>");
                        if (ix < al.size() - 1)
                            result.append(", ");
                    }
                    result.append("<P>");
                }
                else {
                    i = dependencySize(fileDepends, f, f2);
                    if (i > 0)
                        result.append("<br/>File ").append(f).append(" dependency size on file ").append(f2).append(" is ").append(i).append("<P>");
                }
                // if (al != null
                // && (dependencySize(fileDepends, f, f2) > al.size() || al
                // .size() < 40))
                // !examined.contains(f + "-" + f2) && !examined.contains(f2 +
                // "-" + f)
                // { // show mutual dependencies of comparable size
                // result.append("\nFile " + f2 + " dependency size on file " +
                // f + " is " + dependencySize(fileDepends,f,f2) + "<br>\n");
                // result.append("\nFile " + f + " dependency size on file " +
                // f2 + " is " + al.size() + "\n");
                // result.append(" with terms:<br>\n ");
                // for (int i = 0; i < al.size(); i++) {
                // String term = (String) al.get(i);
                // result.append("<a href=\"" + kbHref + "&term=" + term + "\">"
                // + term + "</a>");
                // if (i < al.size()-1)
                // result.append(", ");
                // }
                // result.append("<P>\n");
                // }
                // else {
                // int i = dependencySize(fileDepends,f,f2);
                // int j = dependencySize(fileDepends,f2,f);
                // // && !examined.contains(f + "-" + f2) &&
                // !examined.contains(f2 + "-" + f)
                // if (i > 0 )
                // result.append("\nFile " + f2 + " dependency size on file " +
                // f + " is " + i + "<P>\n");
                // if (j > 0 )
                // result.append("\nFile " + f + " dependency size on file " +
                // f2 + " is " + j + "<P>\n");
                // }
                // if (!examined.contains(f + "-" + f2))
                // examined.add(f + "-" + f2);
            }
            result.append("\n\n");
        }
        return result.toString();
    }

    /** *****************************************************************
     * Make an empty KB for use in Diagnostics.
     *
     * @param kbName the name of the empty KB to make
     */
    public static KB makeEmptyKB(String kbName) {

        String kbDir = (String)KBmanager.getMgr().getPref("kbDir");
        if (KBmanager.getMgr().existsKB(kbName)) {
            KBmanager.getMgr().removeKB(kbName);
        }
        File dir = new File( kbDir );
        File emptyCFile = new File( dir, "emptyConstituent.txt");
        String emptyCFilename = emptyCFile.getAbsolutePath();
        KBmanager.getMgr().addKB(kbName);
        KB empty = KBmanager.getMgr().getKB(kbName);
        System.out.println("empty = " + empty);

        // Fails elsewhere if no constituents, or empty constituent, thus...
        try (Writer fw = new FileWriter( emptyCFile );
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println("(instance instance BinaryPredicate)\n");
            empty.addConstituent(emptyCFilename);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        return empty;
    }

    /** *****************************************************************
     * Returns "" if answer is OK, otherwise reports it.
     */
    private static String reportAnswer(KB kb, String proof, Formula query, String pQuery, String testType) {

        String language = kb.language;
        String kbName = kb.name;
        String hostname = KBmanager.getMgr().getPref("hostname");
        String result = null;
        if (hostname == null || hostname.length() == 0)
            hostname = "localhost";
        String port = KBmanager.getMgr().getPref("port");
        if (port == null || port.length() == 0)
            port = "8080";
        String kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?lang=" + language + "&kb=" + kbName;
        String lineHtml = "<table ALIGN='LEFT' WIDTH=40%%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";
        StringBuilder html = new StringBuilder();

        if (proof.contains("Syntax error detected")) {
            html = html.append("Syntax error in formula : <br><br>");
            html = html.append(query.format(kbHref,"&nbsp;","<br>")).append("<br><br>");
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            tpp.parseProofOutput(result, kb);
            result = HTMLformatter.formatTPTP3ProofResult(tpp,query.getFormula(), lineHtml,kbName,language);
            html = html.append(result);
            return html.toString();
        }

        BasicXMLparser res = new BasicXMLparser(proof);
        ProofProcessor pp = new ProofProcessor(res.elements);
        String ansstr = null;
        //ansstr = pp.returnAnswer(0);
        if (!ansstr.equalsIgnoreCase("no")) {
            html = html.append(testType).append(": <br><br>");
            html = html.append(query.format(kbHref,"&nbsp;","<br>")).append("<br><br>");
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            tpp.parseProofOutput(result, kb);
            result = HTMLformatter.formatTPTP3ProofResult(tpp,query.getFormula(), lineHtml,kbName,language);
            html = html.append(result);
            return html.toString();
        }
        return "";
    }

    /** *****************************************************************
     * Iterating through all formulas, return a proof of an inconsistent
     * or redundant one, if such a thing exists.
     */
    public static String kbConsistencyCheck(KB kb) {

        int timeout = 10;
        int maxAnswers = 1;
        String proof;

        StringBuilder answer = new StringBuilder();
        KB empty = makeEmptyKB("consistencyCheck");

        System.out.println("=================== Consistency Testing ===================");
        try {
            FormulaPreprocessor fp;
            String processedQuery;
            Set<Formula> processedQueries;
            StringBuilder a, negatedQuery;
            Collection<Formula> allFormulas = kb.formulaMap.values();
            for (Formula query : allFormulas) {
                fp = new FormulaPreprocessor();
                processedQueries = fp.preProcess(query,false,kb); // may be multiple because of row vars.
                //System.out.println(" query = " + query);
                //System.out.println(" processedQueries = " + processedQueries);

                System.out.println("INFO in Diagnostics.kbConsistencyCheck(): size = " + processedQueries.size());
                for (Formula f : processedQueries) {
                    System.out.println("INFO in Diagnostics.kbConsistencyCheck(): formula = " + f.getFormula());
                    processedQuery = f.makeQuantifiersExplicit(false);
                    System.out.println("INFO in Diagnostics.kbConsistencyCheck(): processedQuery = " + processedQuery);
                    proof = empty.askEProver(processedQuery,timeout,maxAnswers) + " ";
                    a = new StringBuilder();
                    a.append(reportAnswer(kb,proof,query,processedQuery,"Redundancy"));
                    //  if (answer.length() != 0) return answer;
                    answer.append(a);

                    negatedQuery = new StringBuilder();
                    negatedQuery.append(Formula.LP).append(Formula.NOT).append(Formula.SPACE).append(processedQuery).append(Formula.RP);
                    proof = empty.askEProver(negatedQuery.toString(),timeout,maxAnswers) + " ";
                    a.append(reportAnswer(kb,proof,query,negatedQuery.toString(),"Inconsistency"));
                    if (a.length() != 0) {
                        answer.append(a);
                        return answer.toString();
                    }
                }
                empty.tell(query.getFormula());
            }
        }
        catch (Exception ex) {
            return("Error in Diagnostics.kbConsistencyCheck() while executing query: " + ex.getMessage());
        }
        return "No contradictions or redundancies found.";
    }

    /** ***************************************************************
     * Make a table of terms and the files in which they are defined
     */
    public static void termDefsByFile(KB kb) {

        Set<String> alreadyCounted = new HashSet<>();
        Map<String,List<String>> termsUsed = new TreeMap<>();
        Map<String,List<String>> termsDefined = new TreeMap<>();
        List<Formula> tforms;
        Set<String> tformstrs;
        String simpleName;
        termLinks(kb, termsUsed, termsDefined);
        String str;
        for (String t : termsDefined.keySet()) {
            for (String fname : termsDefined.get(t)) {
                if (KButilities.isCacheFile(fname) || alreadyCounted.contains(t))
                    continue;
                alreadyCounted.add(t);
                tforms = kb.askWithRestriction(0, "termFormat", 2, t);
                tformstrs = new HashSet<>();
                for (Formula f : tforms) {
                    str = f.getStringArgument(3);
                    tformstrs.add(str);
                }
                simpleName = fname.substring(fname.lastIndexOf('/')+1,fname.length());
                System.out.print(t + "\t" + simpleName + "\t");
                int i = 0;
                for (String st : tformstrs) {
                    if (i < 3)
                        System.out.print(st + "\t");
                    i++;
                }
                System.out.println();
            }
        }
    }

    /** ***************************************************************
     * Make a table of terms and the files in which they are defined
     */
    public static void termDefsByGivenFile(KB kb, Set<String> files) {

        System.out.println("termDefsByGivenFile(): files: " + files);
        Set<String> alreadyCounted = new HashSet<>();
        Map<String,Set<String>> termsByFile = new HashMap<>();
        String fname, simpleName, str;
        Set<String> terms, goodTerms, ts, tformstrs;
        List<Formula> tforms;
        for (Formula f : kb.formulaMap.values()) {
            if (debug) if (f.getFormula().contains("AppleComputerCorporation"))
                System.out.println("termDefsByGivenFile(): " + f);
            fname = f.sourceFile;
            simpleName = fname.substring(fname.lastIndexOf('/')+1,fname.length());
            if (debug) if (f.getFormula().contains("AppleComputerCorporation"))
                System.out.println("termDefsByGivenFile(): simple name: " + simpleName);
            terms = f.collectTerms();
            goodTerms = new HashSet<>();
            if (debug) if (f.getFormula().contains("AppleComputerCorporation"))
                System.out.println("termDefsByGivenFile(): terms: " + terms);
            for (String t : terms) {
                if (!Formula.isVariable(t) && t.charAt(0) != '"' && !StringUtil.isNumeric(t))
                    goodTerms.add(t);
            }
            if (termsByFile.keySet() != null && termsByFile.keySet().contains(simpleName)) {
                ts = termsByFile.get(simpleName);
                ts.addAll(goodTerms);
            }
            else {
                if (debug) System.out.println("termDefsByGivenFile(): new file: " + simpleName);
                termsByFile.put(simpleName, goodTerms);
            }
        }
        for (String fnam : termsByFile.keySet()) {  // make all terms not in file set already counted
            if (files.contains(fnam) || fnam.equals("domainEnglishFormat.kif") &&
                    fnam.equals("english_format.kif") || KButilities.isCacheFile(fnam))
                continue;
            for (String term : termsByFile.get(fnam)) {
                if (debug) if (term.equals("AppleComputerCorporation"))
                    System.out.println("termDefsByGivenFile(): adding to already counted: " + term + " from file: " + fnam);
                alreadyCounted.add(term);
            }
        }
        for (String fnam : termsByFile.keySet()) {
            for (String term : termsByFile.get(fnam)) {
                if (debug) if (term.equals("AppleComputerCorporation"))
                    System.out.println("termDefsByGivenFile(): found term: " + term);
                if (alreadyCounted.contains(term))
                    continue;
                alreadyCounted.add(term);
                if (debug) if (term.equals("AppleComputerCorporation"))
                    System.out.println("termDefsByGivenFile(): added to already counted (2): " + term);
                tforms = kb.askWithRestriction(0, "termFormat", 2, term);
                tformstrs = new HashSet<>();
                for (Formula f : tforms) {
                    str = f.getStringArgument(3);
                    tformstrs.add(str);
                }
                System.out.print(term + "\t" + fnam + "\t");
                int i = 0;
                for (String st : tformstrs) {
                    if (i < 3)
                        System.out.print(st + "\t");
                    i++;
                }
                System.out.println();
            }
        }
    }

    /** ***************************************************************
     * Make a table of terms and the files in which they are defined
     */
    public static void addLabels(KB kb, Set<String> file) {

        List<Formula> tforms;
        Set<String> tformstrs;
        String str;
        int i;
        for (String term : file) {
            tforms = kb.askWithRestriction(0, "termFormat", 2, term);
            tformstrs = new HashSet<>();
            for (Formula f : tforms) {
                str = f.getStringArgument(3);
                tformstrs.add(str);
            }
            System.out.print(term + "\t");
            i = 0;
            for (String st : tformstrs) {
                if (i < 3)
                    System.out.print(st + "\t");
                i++;
            }
            System.out.println();
        }
    }

    /** ***************************************************************
     */
    public static void printAllTerms(KB kb) {

        for (String t : kb.terms)
            System.out.println(t);
    }

    /** ***************************************************************
     * Find all terms that differ only in capitalization
     */
    public static List<String> termCapDiff(KB kb) {

        List<String> result = new ArrayList<>();
        for (String t1 : kb.terms) {
            for (String t2 : kb.terms) {
                if ((t1 == null ? t2 != null : !t1.equals(t2)) && t1.equalsIgnoreCase(t2))
                    result.add(t1 + " " + t2);
            }
        }
        return result;
    }

    /** ***************************************************************
     * diff the terms in two KBs (small first, then big) and print
     * all the remainder with their filename and termFormats
     */
    public static void diffTerms(KB kb, String f1, String f2) {

        Set<String> small = new HashSet<>();
        small.addAll(FileUtil.readLines(f1,false));
        Set<String> big = new HashSet<>();
        big.addAll(FileUtil.readLines(f2,false));
        big.removeAll(small);
        List<Formula> tforms;
        Set<String> tformstrs;
        String str;
        for (String term : big) {
            tforms = kb.askWithRestriction(0, "termFormat", 2, term);
            tformstrs = new HashSet<>();
            for (Formula f : tforms) {
                str = f.getStringArgument(3);
                tformstrs.add(str);
            }
            System.out.print(term + "\t"); //  + fname + "\t");
            int i = 0;
            for (String st : tformstrs) {
                if (i < 3)
                    System.out.print(st + "\t");
                i++;
            }
            System.out.println();
        }
    }

    /** ***************************************************************
     * Recursively extract variables from a KIF formula
     *
     * @param f the original Formula to process
     * @param kb the current knowledge base
     * @return a map where each variable is collected from its containing atom as its key
     */
    public static Map<String, Set<String>> extractVariables(Formula f, KB kb) {

        Map<String, Set<String>> links = new HashMap<>();

        if (f.getFormula() == null || f.getFormula().isBlank())
            return links;

        KIF kifInstance = new KIF();
        String parsedf = kifInstance.parseStatement(f.getFormula()); // getFormula() translates formula into a string
        if (parsedf != null && !parsedf.isBlank()) {
            System.err.println("Error in: " + Diagnostics.class.getName() + "extractVariables: " + parsedf);
            return links;
        }

        Set<String> variables;
        List<String> simpleArgs;
        List<Formula> complexArgs;

        // check if formula is a simple argument
        if (f.isSimpleClause(kb)) { // base case: if the formula is a simple argument
            simpleArgs = f.argumentsToArrayListString(1); // get the variables in the formula
            variables = new HashSet<>();
            for (String arg : simpleArgs) {
                if (new Formula(arg).isVariable())
                    variables.add(arg); // extracts ?H from (instance ?H Human)
            }
            if (!variables.isEmpty())
                links.put(f.getFormula(), variables);
        }
        else { // recursive case
            if (Formula.isQuantifier(f.car()))
                complexArgs = f.complexArgumentsToArrayList(2); // Don't allow quantifier args here
            else
                complexArgs = f.complexArgumentsToArrayList(1);
            for (Formula complexForm : complexArgs)
                links.putAll(extractVariables(complexForm, kb));
        }
        return links;
    }

    /** ***************************************************************
     * Recursively extract variable co-occurrences from a KIF formula
     *
     * @param f the original Formula to process
     * @param kb the current knowledge base
     * @return a map where each variable is linked to others it appears with
     */
    public static Map<String, Set<String>> getVariableLinks(Formula f, KB kb) {

        Map<String, Set<String>> links = new HashMap<>();

        if (f.getFormula() == null || f.getFormula().isBlank())
            return links;

        KIF kifInstance = new KIF();
        String parsedf = kifInstance.parseStatement(f.getFormula()); // getFormula() translates formula into a string
        if (parsedf != null && !parsedf.isBlank()) {
            System.err.println("Error in: " + Diagnostics.class.getName() + "getVariableLinks: " + parsedf);
            return links;
        }

        Set<String> variables;
        List<String> simpleArgs;
        List<Formula> complexArgs;
        Map<String, Set<String>> tempMap;
        Set<String> linkedVars;

        // check if formula is a simple argument
        if (f.isSimpleClause(kb)) { // base case: if the formula is a simple argument
            simpleArgs = f.argumentsToArrayListString(1); // get the variables in the formula
            variables = new HashSet<>();
            for (String arg : simpleArgs) {
                if (new Formula(arg).isVariable())
                    variables.add(arg); // extracts ?H from (instance ?H Human)
            }
            for (String var : variables) {
                linkedVars = new HashSet<>(variables);
                linkedVars.remove(var); // prevents self links (?X -> {?X})
                if (links.containsKey(var)) { // if the variable already exists in the map
                    links.get(var).addAll(linkedVars); // merges other variables that exists for var
                } else {
                    links.put(var, linkedVars); // create a new entry with the variable and its linked variables
                }
            }
        }
        else { // recursive case
            if (Formula.isQuantifier(f.car()))
                complexArgs = f.complexArgumentsToArrayList(2); // Don't allow quantifier args here
            else
                complexArgs = f.complexArgumentsToArrayList(1);
            for (Formula complexForm : complexArgs) {
                tempMap = getVariableLinks(complexForm, kb);
                for (String key : tempMap.keySet()) {
                    if (links.containsKey(key)) { // if the variable already exists in the map
                        links.get(key).addAll(tempMap.get(key)); // add the linked variables to the existing set
                    } else {
                        links.put(key, new HashSet<>(tempMap.get(key)));
                    }
                }
            }
        }
        return links;
    }

    /** ***************************************************************
     * Recursively extract variable co-occurrences from a KIF file
     *
     * @param fKif the KIF file to process
     * @param kb the current knowledge base
     * @return a map where each variable is linked to others it appears with
     */
    public static Map<String, Set<String>> getVariableLinks(File fKif, KB kb) {

        Map<String, Set<String>> links = new HashMap<>();

        if (!fKif.exists())
            return links;

        KIF kifInstance = new KIF();
        kifInstance.setParseMode(KIF.RELAXED_PARSE_MODE);
        try {
            kifInstance.readFile(fKif.getPath()); // getFormula() translates formula into a string
        } catch (Exception e) {
            System.err.println("Error in: " + Diagnostics.class.getName() + "getVariableLinks: " + e);
            return links;
        }

        Set<String> variables;
        List<String> simpleArgs;
        List<Formula> complexArgs;
        Set<String> linkedVars;
        Formula f;
        Set<String> keys = kifInstance.formulaMap.keySet();

        for (String key : keys) {
            f = kifInstance.formulaMap.get(key);
            // check if formula is a simple argument
            if (f.isSimpleClause(kb)) { // base case: if the formula is a simple argument
                simpleArgs = f.argumentsToArrayListString(1); // get the variables in the formula
                variables = new HashSet<>();
                for (String arg : simpleArgs) {
                    if (new Formula(arg).isVariable())
                        variables.add(arg); // extracts ?H from (instance ?H Human)
                }
                for (String var : variables) {
                    linkedVars = new HashSet<>(variables);
                    linkedVars.remove(var); // prevents self links (?X -> {?X})
                    if (links.containsKey(var)) { // if the variable already exists in the map
                        links.get(var).addAll(linkedVars); // merges other variables that exists for var
                    } else {
                        links.put(var, linkedVars); // create a new entry with the variable and its linked variables
                    }
                }
            }
            else {
                if (Formula.isQuantifier(f.car()))
                    complexArgs = f.complexArgumentsToArrayList(2); // Don't allow quantifier args here
                else
                    complexArgs = f.complexArgumentsToArrayList(1);
                for (Formula complexForm : complexArgs)
                    links.putAll(getVariableLinks(complexForm, kb));
            }
        }
        return links;
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("Diagnostics class");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -t - print term def by file");
        System.out.println("  -l <fname> - add labels for a file of terms");
        System.out.println("  -f <fname> - print term def by file");
        System.out.println("  -p - print all terms in KB");
        System.out.println("  -d <f1> <f2> - print all terms in f2 not in f1");
        System.out.println("  -o - terms not below Entity (Orphans)");
        System.out.println("  -c - terms without documentation");
        System.out.println("  -q - quantifier not in body");
        System.out.println("  -v <fname> - extract variable co-occurrences from a kif file");
    }

    /** ***************************************************************
     * Test method for this class.
     */
    public static void main(String args[]) {

        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                System.out.println("Arg[" + i + "]: '" + args[i] + "'");
            }
        } else
            showHelp();

        KBmanager.getMgr().initializeOnce();
        //resultLimit = 0; // don't limit number of results on command line
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        if (args != null && args.length > 0 && args[0].equals("-t")) {
            termDefsByFile(kb);
        }
        if (args != null && args.length > 1 && args[0].equals("-f")) {
            Set<String> files = new HashSet<>();
            List<String> lines = FileUtil.readLines(args[1],false);
            files.addAll(lines);
            termDefsByGivenFile(kb,files);
        }
        if (args != null && args.length > 1 && args[0].equals("-l")) {
            Set<String> files = new HashSet<>();
            List<String> lines = FileUtil.readLines(args[1],false);
            files.addAll(lines);
            addLabels(kb,files);
        }
        else if (args != null && args.length > 0 && args[0].equals("-o")) {
            System.out.println(termsNotBelowEntity(kb));
        }
        else if (args != null && args.length > 0 && args[0].equals("-c")) {
            System.out.println(termsWithoutDoc(kb));
        }
        else if (args != null && args.length > 0 && args[0].equals("-q")) {
            System.out.println(quantifierNotInBody(kb));
        }
        else if (args != null && args.length > 0 && args[0].equals("-p")) {
            printAllTerms(kb);
        }
        else if (args != null && args.length > 2 && args[0].equals("-d")) {
            diffTerms(kb,args[1],args[2]);
        }
        else if (args != null && args.length > 0 && args[0].equals("-h")) {
            showHelp();
        }
        else if (args != null && args.length > 0 && args[0].equals("-v")) {
//            Formula simpleFormula = new Formula("(instance ?H Human)");
//            varLinksParentMap.putAll(getVariableLinks(simpleFormula, kb));

//            Formula complexFormula = new Formula("(=>\n" +
//                                                 "  (and\n" +
//                                                 "    (P ?A ?B)\n" +
//                                                 "    (P ?B ?C))\n" +
//                                                 "  (exists (?X ?Z)\n" +
//                                                 "    (and\n" +
//                                                 "      (Q ?X)\n" +
//                                                 "      (M ?Z ?C))))"
//                                                );
//            Formula complexFormula = new Formula("(=>\n" +
//                                                   "  (instance ?WARGAMING Wargaming)\n" +
//                                                   "  (exists (?MILITARYOFFICER ?SIMULATION ?TOOL)\n" +
//                                                   "    (and\n" +
//                                                   "      (instance ?MILITARYOFFICER MilitaryOfficer)\n" +
//                                                   "      (instance ?SIMULATION Imagining)\n" +
//                                                   "      (instance ?TOOL Device)\n" +
//                                                   "      (agent ?WARGAMING ?MILITARYOFFICER)\n" +
//                                                   "      (patient ?WARGAMING ?SIMULATION)\n" +
//                                                   "      (instrument ?WARGAMING ?TOOL))))"
//                                                );

//            varLinksParentMap.putAll(getVariableLinks(complexFormula, kb));

            if (args[1] != null && !args[1].isBlank()) {
                String path = args[1];
                Path inPath = Paths.get(path);
                try (Stream<Path> paths = Files.walk(inPath)) {
                    paths.filter(f -> f.toString().endsWith(".kif")).sorted().forEach(f -> {
                        varLinksParentMap.putAll(getVariableLinks(f.toFile(), kb));
                    });
                } catch (IOException e) {
                    System.err.println("Error processing input: " + e);
                }
            }

            // Results output
            System.out.printf("Resuls of getVariableLinks():%n");
            for (String key : varLinksParentMap.keySet()) {
               System.out.println("  " + key + " -> " + varLinksParentMap.get(key));
            }
        }
    }
}
