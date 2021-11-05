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

import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.util.*;

/** *****************************************************************
 * A class that finds problems in a knowledge base.  It is not meant
 * to be instantiated.
 */
public class Diagnostics {

    public static boolean debug = false;

    public static List LOG_OPS = Arrays.asList("and","or","not","exists",
                                                "forall","=>","<=>","holds");

    private static int resultLimit = 100;

    /** *****************************************************************
     * Return a list of terms (for a given argument position) that do not 
     * have a specified relation.
     * @param kb the knowledge base
     * @param rel the relation name
     * @param argnum the argument position of the term
     * @param letter the first letter of the term name
     */
    public static ArrayList<String> termsWithoutRelation(KB kb, String rel, int argnum, 
                                                         char letter) {

        ArrayList<String> result = new ArrayList<String>();
        Iterator<String> it = kb.getTerms().iterator();
        while (it.hasNext()) {
            String term = it.next();                
            if (LOG_OPS.contains(term) || StringUtil.isNumeric(term))  // Exclude the logical operators and numbers
                continue;                
        	ArrayList<Formula> forms = kb.ask("arg",argnum,term);
            if (forms == null || forms.isEmpty()) {
                if (letter < 'A' || term.charAt(0) == letter) 
                    result.add(term);                
            }
            else {
                boolean found = false;
                Iterator<Formula> it2 = forms.iterator();
                while (it2.hasNext()) {
                	Formula formula = (Formula) it2.next();
                	if (formula != null) {
	                    String pred = formula.car();
	                    if (pred.equals(rel)) {
	                        found = true;
	                        break;
	                    }
                	}
                	else
                		System.out.println("Error in Diagnostics.termsWithoutRelation(): null formula for: " + term);
                }
                if (!found) {
                    if (letter < 'A' || term.charAt(0) == letter) 
                        result.add(term);                    
                }
            }
            if (resultLimit > 0 && result.size() > resultLimit) {
                result.add("limited to " + resultLimit + " results");
                break;
            }
        }
        return result;
    }

    /** *****************************************************************
     * Return a list of terms that do not have a documentation string.
     */
    public static ArrayList termsWithoutDoc(KB kb) {

        System.out.println("INFO in Diagnostics.termsWithoutDoc(): "); 
        return termsWithoutRelation(kb,"documentation",1,' ');
    }

    /** *****************************************************************
     * Return a list of terms that have more than one documentation string.
     */
    public static ArrayList<String> termsWithMultipleDoc(KB kb) {
 
        Set<String> result = new HashSet();
        Set<String> withDoc = new HashSet();
        ArrayList<Formula> forms = kb.ask("arg", 0, "documentation");
        if (!forms.isEmpty()) {
            boolean isNaN = true;
            Iterator<Formula> it = forms.iterator();
            while (it.hasNext()) {
            	Formula f = it.next();                
                String term = f.getStringArgument(1);   // Append term and language to make a key.
                isNaN = true;
                try {
                    double dval = Double.parseDouble(term);
                    isNaN = Double.isNaN(dval);
                }
                catch (Exception nex) {
                }
                if (isNaN) {
                    String key = (term + f.getArgument(2));
                    if (withDoc.contains(key)) 
                        result.add(term);                
                    else 
                        withDoc.add(key);
                }
                if (resultLimit > 0 && result.size() > resultLimit) {
                    result.add("limited to 100 results");
                    break;
                }
            }
        }
        return new ArrayList(result);
    }

    /** *****************************************************************
     * Returns true if term has an explicitly stated parent, or a
     * parent can be inferred from the transitive relation caches,
     * else returns false.
     
    private static boolean hasParent(KB kb, String term) {
        
        Iterator<String> it = preds.iterator();
        while (it.hasNext()) {
        	String pred = it.next();
        	HashMap<String,HashSet<String>> predvals = kb.kbCache.parents.get(term);
            if (predvals != null) {
                HashSet<String> cached = predvals.get(term);
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
    public static ArrayList<String> termsNotBelowEntity(KB kb) {

        System.out.println("INFO in Diagnostics.termsNotBelowEntity(): "); 
        ArrayList<String> result = new ArrayList<String>();
        int count = 0;
        Iterator<String> it = kb.getTerms().iterator();
        while (it.hasNext() && (resultLimit < 1 || count < resultLimit)) {
            String term = it.next();
            if (LOG_OPS.contains(term) || term.equals("Entity") || StringUtil.isNumeric(term)) 
                continue;
            else {
                if (!kb.kbCache.subclassOf(term,"Entity") && !kb.kbCache.transInstOf(term,"Entity")) {
                    result.add(term); 
                    count++;
                }
            }
            if (resultLimit > 0 && count > resultLimit)
                result.add("limited to 100 results");            
        }        
        return result;
    }

    /** *****************************************************************
     * Return a list of terms that have parents which are disjoint.
     */
    public static ArrayList<String> childrenOfDisjointParents(KB kb) {

        ArrayList<String> result = new ArrayList<String>();
        
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
    public static ArrayList<String> membersNotInAnyPartitionClass(KB kb) {
        
        ArrayList<String> result = new ArrayList<String>();
        try {
            TreeSet<String> reduce = new TreeSet<String>();
            // Use all partition statements and all
            // exhaustiveDecomposition statements.
            ArrayList<Formula> forms = kb.ask("arg",0,"partition");
            if (forms == null) 
                forms = new ArrayList<Formula>();
            ArrayList<Formula> forms2 = kb.ask("arg",0,"exhaustiveDecomposition");
            if (forms2 != null) 
                forms.addAll(forms2);
            boolean go = true;
            Iterator<Formula> it = forms.iterator();
            while (go && it.hasNext()) {
                Formula form = it.next();
                String parent = form.getStringArgument(1);
                ArrayList<String> partition = form.argumentsToArrayListString(2);
                List<String> instances = kb.getTermsViaPredicateSubsumption("instance",2,parent,1,true);
                if ((instances != null) && !instances.isEmpty()) {
                    boolean isInstanceSubsumed = false;
                    boolean isNaN = true;
                    String inst = null;
                    Iterator<String> it2 = instances.iterator();
                    while (go && it2.hasNext()) {
                        isInstanceSubsumed = false;
                        isNaN = true;
                        inst = it2.next();                        
                        try {   // For diagnostics, try to avoid treating numbers as bonafide terms.
                            double dval = Double.parseDouble(inst);
                            isNaN = Double.isNaN(dval);
                        }
                        catch (Exception nex) {
                        }
                        if (isNaN) {
                        	Iterator<String> it3 = partition.iterator();
                            while (it3.hasNext()) {
                                String pclass = it3.next();
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
                        if (resultLimit < 1 || reduce.size() > resultLimit)
                            go = false;                        
                    }
                }
            }
            result.addAll(reduce);
            if (resultLimit > 0 && result.size() > resultLimit)
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
    public static ArrayList<String> relationsWithoutFormat(KB kb) {

        ArrayList<String> result = new ArrayList<>();
        for (String rel : kb.kbCache.relations) {
            ArrayList<Formula> forms = kb.askWithRestriction(0,"format",2,rel);
            if (forms == null || forms.size() == 0)
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
        catch (Exception nex) {
        }
        if (isNaN) {
            ArrayList<Formula> forms = kb.ask("ant",0,term);
            ArrayList<Formula> forms2 = kb.ask("cons",0,term);
            if (((forms == null) || forms.isEmpty())
                    && ((forms2 == null) || forms2.isEmpty()))
                return true;
        }
        return false;
    }

    /** *****************************************************************
     * Find all terms which do not appear in any implication (rule).
     */
    public static ArrayList<String> termsWithoutRules(KB kb) {

        boolean isNaN = true;
        ArrayList<String> result = new ArrayList<String>();
        Iterator<String> it = kb.getTerms().iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            isNaN = true;
            if (termWithoutRules(kb,term))
                result.add(term);
            if (resultLimit > 0 && result.size() > resultLimit) {
                result.add("limited to 100 results");
                break;
            }
        }
        return result;
    }

    /** *****************************************************************
     * @return a list of variables used only once.
     */
    public static HashSet<String> singleUseVariables(Formula f) {

        HashSet<String> result = new HashSet<String>();
        Set<String> vars = f.collectAllVariables();
        for (String v : vars) {
            int index = f.getFormula().indexOf(v);
            int index2 = f.getFormula().indexOf(v,index+v.length());
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
    public static HashSet<String> unquantInConsequent(Formula f) {

        HashSet<String> result = new HashSet<String>();
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
    public static ArrayList<Formula> unquantsInConseq(KB kb) {

        ArrayList<Formula> result = new ArrayList<Formula>();
        Iterator<Formula> it = kb.formulaMap.values().iterator();
        while (it.hasNext()) {
            Formula form = (Formula) it.next();
            if ((form.getFormula().indexOf("forall") != -1)
                    || (form.getFormula().indexOf("exists") != -1)) {
                if (!unquantInConsequent(form).isEmpty())
                    result.add(form);
            }
            if (resultLimit > 0 && result.size() > resultLimit)
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
        if (!Arrays.asList("forall", "exists").contains(f.car())) {
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
            ArrayList<String> qList = quant.argumentsToArrayListString(0);  // Put all the quantified variables into a list.
            if (rest.indexOf("exists") != -1 || rest.indexOf("forall") != -1) { //nested quantifiers
                Formula restForm = new Formula();
                restForm.read(rest);
                restForm.read(restForm.cdr());
                if (quantifierNotInStatement(restForm)) 
                    return true;
            }
            if (qList != null) {
                for (int i = 0; i < qList.size(); i++) {
                    String var = (String) qList.get(i);
                    if (body.indexOf(var) == -1)
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
    public static ArrayList<Formula> quantifierNotInBody(KB kb, String fname) {

        ArrayList<Formula> result = new ArrayList<Formula>();
        Iterator<Formula> it = kb.formulaMap.values().iterator();
        while (it.hasNext()) {
            Formula form = (Formula) it.next();
            if (!FileUtil.noPath(form.sourceFile).equals(fname))
                continue;
            if ((form.getFormula().indexOf("forall") != -1)
                    || (form.getFormula().indexOf("exists") != -1)) {
                if (quantifierNotInStatement(form))
                    result.add(form);
            }
            if (resultLimit > 0 && result.size() > resultLimit)
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
    public static ArrayList<Formula> quantifierNotInBody(KB kb) {

        ArrayList<Formula> result = new ArrayList<Formula>();
		Iterator<Formula> it = kb.formulaMap.values().iterator();
		while (it.hasNext()) { 
			Formula form = (Formula) it.next();
			if ((form.getFormula().indexOf("forall") != -1)
					|| (form.getFormula().indexOf("exists") != -1)) {
				if (quantifierNotInStatement(form)) 
					result.add(form);					
			}
            if (resultLimit > 0 && result.size() > resultLimit)
				return result;				
		}
        return result;
    }

    /** *****************************************************************
     * Add a key to a map and a value to the ArrayList corresponding
     * to the key.  Results are a side effect.
     */
    public static void addToMapList(TreeMap m, String key, String value) {

        ArrayList al = (ArrayList) m.get(key);
        if (al == null) {
            al = new ArrayList();
            m.put(key,al);
        }
        if (!al.contains(value)) 
            al.add(value);
    }

    /** *****************************************************************
     * Add a key to a map and a key, value to the map
     * corresponding to the key.  Results are a side effect.
     */
    public static void addToDoubleMapList(TreeMap m, String key1, String key2, String value) {

        TreeMap tm = (TreeMap) m.get(key1);
        if (tm == null) {
            tm = new TreeMap();
            m.put(key1,tm);
        }
        addToMapList(tm,key2,value);
    }

    /** *****************************************************************
     * Find all the terms used and defined in a KB.  Terms are defined by
     * their appearance in definitionalRelations
     */
    private static void termLinks(KB kb, TreeMap termsUsed, TreeMap termsDefined) {
        
        List definitionalRelations = Arrays.asList("instance", "subclass",
                "subAttribute", "domain", "domainSubclass", "range",
                "rangeSubclass", "documentation", "subrelation");

        for (String term : kb.getTerms()) {
            ArrayList<Formula> forms = kb.ask("arg",1,term);
            // Get every formula with the term as arg 1
            // Only definitional uses are in the arg 1 position
            if (forms != null && forms.size() > 0) {
                for (Formula formula : forms) {
                    String relation = formula.getStringArgument(0);
                    String filename = formula.sourceFile;
                    if (definitionalRelations.contains(relation))
                        addToMapList(termsDefined,term,filename);
                    else
                        addToMapList(termsUsed,term,filename);
                }
            }
            forms = kb.ask("arg",2,term);
            ArrayList<Formula> newform;
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
            if (forms != null && forms.size() > 0) {
                for (int i = 0; i < forms.size(); i++) {
                    Formula formula = (Formula) forms.get(i);
                    String filename = formula.sourceFile;
                    addToMapList(termsUsed,term,filename);
                }
            }
        }
        return;
    }

    /** *****************************************************************
     */
    private static void fileLinks(KB kb, TreeMap fileDefines, TreeMap fileUses, 
                                  TreeMap termsUsed, TreeMap termsDefined) {

        Iterator it = termsUsed.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            ArrayList values = (ArrayList) termsUsed.get(key);
            for (int i = 0; i < values.size(); i++) {
                String value = (String) values.get(i);
                addToMapList(fileUses,value,key);
            }
        }
        it = termsDefined.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            ArrayList values = (ArrayList) termsDefined.get(key);
            for (int i = 0; i < values.size(); i++) {
                String value = (String) values.get(i);
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
    private static TreeMap termDependency(KB kb) {

        System.out.println("INFO in Diagnostics.termDependency()");

        // A map of terms keys with an ArrayList as values listing files
        // in which the term is used.
        TreeMap<String,ArrayList<String>> termsUsed = new TreeMap();

        // A map of terms keys with an ArrayList as values listing files
        // in which the term is defined (meaning appearance in an
        // instance, subclass, domain, subrelation, or documentation statement).
        // 
        TreeMap<String,ArrayList<String>> termsDefined = new TreeMap();

        // A map of file names and ArrayList values listing term names defined
        // in the file;
        TreeMap<String,ArrayList<String>> fileDefines = new TreeMap();

        // A map of file names and ArrayList values listing term names used but not defined
        // in the file;
        TreeMap<String,ArrayList<String>> fileUses = new TreeMap();

        // A map of file name keys and TreeMap values listing file names
        // on which the given file depends.  The interior TreeMap file name
        // keys index ArrayLists of terms.  file -depends on-> filenames -that defines-> terms
        TreeMap<String,TreeMap<String,ArrayList<String>>> fileDepends = new TreeMap();

        termLinks(kb,termsUsed,termsDefined);
        fileLinks(kb,fileDefines,fileUses,termsUsed,termsDefined);

        for (String fileUsesName : fileUses.keySet()) {
            ArrayList termUsedNames = fileUses.get(fileUsesName);
            for (int i = 0; i < termUsedNames.size(); i++) {
                String term = (String) termUsedNames.get(i);
                ArrayList fileDependencies = termsDefined.get(term);
                if (fileDependencies != null) {
                    String fileDepend = null;
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
    private static int dependencySize(TreeMap depend, String f, String f2) {

        TreeMap tm = (TreeMap) depend.get(f2);
        if (tm != null) {
            ArrayList al = (ArrayList) tm.get(f);
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

        StringBuffer result = new StringBuffer();

        // A map of file name keys and TreeMap values listing file names
        // on which the given file depends.  The interior TreeMap file name
        // keys index ArrayLists of terms.  file -depends on-> filenames -that defines-> terms
        TreeMap fileDepends = Diagnostics.termDependency(kb);
		System.out.println(fileDepends);
        Iterator it = fileDepends.keySet().iterator();
        while (it.hasNext()) {
            String f = (String) it.next();
            // result.append("File " + f + " depends on: ");
            TreeMap tm = (TreeMap) fileDepends.get(f);
            Iterator it2 = tm.keySet().iterator();
            while (it2.hasNext()) {
                String f2 = (String) it2.next();                
                ArrayList al = (ArrayList) tm.get(f2);

				if (al != null && al.size() < 40) {
					result.append("<br/>File " + f + " dependency size on file " + f2 + " is " + al.size() + " with terms:<br/>");
					for (int i = 0; i < al.size(); i++) {
						String term = (String) al.get(i);
						result.append("<a href=\"" + kbHref + "&term=" + term + "\">" + term + "</a>");
						if (i < al.size() - 1)
							result.append(", ");
					}
					result.append("<P>");
                }
                else {
					int i = dependencySize(fileDepends, f, f2);
					if (i > 0)
						result.append("<br/>File " + f + " dependency size on file " + f2 + " is " + i + "<P>");
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
     */
    private static KB makeEmptyKB(String kbName) {

        String kbDir = (String)KBmanager.getMgr().getPref("kbDir");
        if (KBmanager.getMgr().existsKB(kbName)) {
            KBmanager.getMgr().removeKB(kbName);
        }
        File dir = new File( kbDir );
        File emptyCFile = new File( dir, "emptyConstituent.txt" );
        String emptyCFilename = emptyCFile.getAbsolutePath();
        FileWriter fw = null; 
        PrintWriter pw = null;
        KBmanager.getMgr().addKB(kbName);
        KB empty = KBmanager.getMgr().getKB(kbName);
        System.out.println("empty = " + empty);

        try { // Fails elsewhere if no constituents, or empty constituent, thus...
            fw = new FileWriter( emptyCFile );
            pw = new PrintWriter(fw);   
            pw.println("(instance instance BinaryPredicate)\n");
            if (pw != null) pw.close();
            if (fw != null) fw.close();
            empty.addConstituent(emptyCFilename);
        }
        catch (java.io.IOException e) {
            System.out.println("Error writing file " + emptyCFilename);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
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
        StringBuffer html = new StringBuffer();

        if (proof.indexOf("Syntax error detected") != -1) {
            html = html.append("Syntax error in formula : <br><br>");
            html = html.append(query.format(kbHref,"&nbsp;","<br>") + "<br><br>");
            result = HTMLformatter.formatProofResult(proof,query.getFormula(),
                                                     pQuery,lineHtml,kbName,language);
            html = html.append(result);
            return html.toString();
        }
            
        BasicXMLparser res = new BasicXMLparser(proof);
        ProofProcessor pp = new ProofProcessor(res.elements);
        String ansstr = null;
        //ansstr = pp.returnAnswer(0);
        if (!ansstr.equalsIgnoreCase("no")) {
            html = html.append(testType + ": <br><br>");
            html = html.append(query.format(kbHref,"&nbsp;","<br>") + "<br><br>");
            result = HTMLformatter.formatProofResult(proof,query.getFormula(),
                                                     pQuery,lineHtml,kbName,language);
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
        String result = null;

        StringBuffer answer = new StringBuffer();
        KB empty = makeEmptyKB("consistencyCheck");

        System.out.println("=================== Consistency Testing ===================");
        try {
            Formula theQuery = new Formula();
            Collection allFormulas = kb.formulaMap.values();
            Iterator it = allFormulas.iterator();
            while (it.hasNext()) {
                Formula query = (Formula) it.next();
                FormulaPreprocessor fp = new FormulaPreprocessor();
                Set<Formula> processedQueries = fp.preProcess(query,false,kb); // may be multiple because of row vars.
                //System.out.println(" query = " + query);
                //System.out.println(" processedQueries = " + processedQueries);

                String processedQuery = null;
                Iterator q = processedQueries.iterator();

                System.out.println("INFO in Diagnostics.kbConsistencyCheck(): size = " + processedQueries.size());
                while (q.hasNext()) {
                    Formula f = (Formula) q.next();
                    System.out.println("INFO in Diagnostics.kbConsistencyCheck(): formula = " + f.getFormula());
                    processedQuery = f.makeQuantifiersExplicit(false);
                    System.out.println("INFO in Diagnostics.kbConsistencyCheck(): processedQuery = " + processedQuery);
                    proof = empty.askEProver(processedQuery,timeout,maxAnswers) + " ";
                    StringBuffer a = new StringBuffer();
                    a.append(reportAnswer(kb,proof,query,processedQuery,"Redundancy"));
                    //  if (answer.length() != 0) return answer;
                    answer.append(a);

                    StringBuffer negatedQuery = new StringBuffer();
                    negatedQuery.append("(not " + processedQuery + ")");
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
        catch ( Exception ex ) {
            return("Error in Diagnostics.kbConsistencyCheck() while executing query: " + ex.getMessage());
        }
        return "No contradictions or redundancies found.";
    }

    /** ***************************************************************
     * Make a table of terms and the files in which they are defined
     */
    public static void termDefsByFile(KB kb) {

        HashSet<String> alreadyCounted = new HashSet<>();
        TreeMap<String,ArrayList<String>> termsUsed = new TreeMap<>();
        TreeMap<String,ArrayList<String>> termsDefined = new TreeMap<>();
        termLinks(kb, termsUsed, termsDefined);
        for (String t : termsDefined.keySet()) {
            for (String fname : termsDefined.get(t)) {
                if (KButilities.isCacheFile(fname) || alreadyCounted.contains(t))
                    continue;
                alreadyCounted.add(t);
                ArrayList<Formula> tforms = kb.askWithRestriction(0, "termFormat", 2, t);
                HashSet<String> tformstrs = new HashSet<>();
                for (Formula f : tforms) {
                    String str = f.getStringArgument(3);
                    tformstrs.add(str);
                }
                String simpleName = fname.substring(fname.lastIndexOf('/')+1,fname.length());
                System.out.print(t + "\t" + simpleName + "\t");
                int i = 0;
                for (String str : tformstrs) {
                    if (i < 3)
                        System.out.print(str + "\t");
                    i++;
                }
                System.out.println();
            }
        }
    }

    /** ***************************************************************
     * Make a table of terms and the files in which they are defined
     */
    public static void termDefsByGivenFile(KB kb, HashSet<String> files) {

        System.out.println("termDefsByGivenFile(): files: " + files);
        HashSet<String> alreadyCounted = new HashSet<>();
        HashMap<String,HashSet<String>> termsByFile = new HashMap<>();
        for (Formula f : kb.formulaMap.values()) {
            if (debug) if (f.getFormula().contains("AppleComputerCorporation"))
                System.out.println("termDefsByGivenFile(): " + f);
            String fname = f.sourceFile;
            String simpleName = fname.substring(fname.lastIndexOf('/')+1,fname.length());
            if (debug) if (f.getFormula().contains("AppleComputerCorporation"))
                System.out.println("termDefsByGivenFile(): simple name: " + simpleName);
            HashSet<String> terms = (HashSet) f.collectTerms();
            HashSet<String> goodTerms = new HashSet<>();
            if (debug) if (f.getFormula().contains("AppleComputerCorporation"))
                System.out.println("termDefsByGivenFile(): terms: " + terms);
            for (String t : terms) {
                if (!Formula.isVariable(t) && t.charAt(0) != '"' && !StringUtil.isNumeric(t))
                    goodTerms.add(t);
            }
            if (termsByFile.keySet() != null && termsByFile.keySet().contains(simpleName)) {
                HashSet<String> ts = termsByFile.get(simpleName);
                ts.addAll(goodTerms);
            }
            else {
                if (debug) System.out.println("termDefsByGivenFile(): new file: " + simpleName);
                termsByFile.put(simpleName, goodTerms);
            }
        }
        for (String fname : termsByFile.keySet()) {  // make all terms not in file set already counted
            if (files.contains(fname) || fname.equals("domainEnglishFormat.kif") &&
                    fname.equals("english_format.kif") || KButilities.isCacheFile(fname))
                continue;
            for (String term : termsByFile.get(fname)) {
                if (debug) if (term.equals("AppleComputerCorporation"))
                    System.out.println("termDefsByGivenFile(): adding to already counted: " + term + " from file: " + fname);
                alreadyCounted.add(term);
            }
        }
        for (String fname : termsByFile.keySet()) {
            for (String term : termsByFile.get(fname)) {
                if (debug) if (term.equals("AppleComputerCorporation"))
                    System.out.println("termDefsByGivenFile(): found term: " + term);
                if (alreadyCounted.contains(term))
                    continue;
                alreadyCounted.add(term);
                if (debug) if (term.equals("AppleComputerCorporation"))
                    System.out.println("termDefsByGivenFile(): added to already counted (2): " + term);
                ArrayList<Formula> tforms = kb.askWithRestriction(0, "termFormat", 2, term);
                HashSet<String> tformstrs = new HashSet<>();
                for (Formula f : tforms) {
                    String str = f.getStringArgument(3);
                    tformstrs.add(str);
                }
                System.out.print(term + "\t" + fname + "\t");
                int i = 0;
                for (String str : tformstrs) {
                    if (i < 3)
                        System.out.print(str + "\t");
                    i++;
                }
                System.out.println();
            }
        }
    }

    /** ***************************************************************
     * Make a table of terms and the files in which they are defined
     */
    public static void addLabels(KB kb, HashSet<String> file) {

        for (String term : file) {
            ArrayList<Formula> tforms = kb.askWithRestriction(0, "termFormat", 2, term);
            HashSet<String> tformstrs = new HashSet<>();
            for (Formula f : tforms) {
                String str = f.getStringArgument(3);
                tformstrs.add(str);
            }
            System.out.print(term + "\t");
            int i = 0;
            for (String str : tformstrs) {
                if (i < 3)
                    System.out.print(str + "\t");
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
     * diff the terms in two KBs (small first, then big) and print
     * all the remainder with their filename and termFormats
     */
    public static void diffTerms(KB kb, String f1, String f2) {

        HashSet<String> small = new HashSet<>();
        small.addAll(FileUtil.readLines(f1,false));
        HashSet<String> big = new HashSet<>();
        big.addAll(FileUtil.readLines(f2,false));
        big.removeAll(small);
        for (String term : big) {
            ArrayList<Formula> tforms = kb.askWithRestriction(0, "termFormat", 2, term);
            HashSet<String> tformstrs = new HashSet<>();
            for (Formula f : tforms) {
                String str = f.getStringArgument(3);
                tformstrs.add(str);
            }
            System.out.print(term + "\t"); //  + fname + "\t");
            int i = 0;
            for (String str : tformstrs) {
                if (i < 3)
                    System.out.print(str + "\t");
                i++;
            }
            System.out.println();
        }
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("Diagnostics");
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
    }

    /** ***************************************************************
     * Test method for this class.
     */
    public static void main(String args[]) {

        KBmanager.getMgr().initializeOnce();
        //resultLimit = 0; // don't limit number of results on command line
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        if (args != null && args.length > 0 && args[0].equals("-t")) {
            termDefsByFile(kb);
        }
        if (args != null && args.length > 1 && args[0].equals("-f")) {
            HashSet<String> files = new HashSet<>();
            List<String> lines = FileUtil.readLines(args[1],false);
            files.addAll(lines);
            termDefsByGivenFile(kb,files);
        }
        if (args != null && args.length > 1 && args[0].equals("-l")) {
            HashSet<String> files = new HashSet<>();
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
        else
            showHelp();
    }
}
