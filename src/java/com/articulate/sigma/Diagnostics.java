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

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.articulate.sigma.KB;

/** *****************************************************************
 * A class that finds problems in a knowledge base.  It is not meant
 * to be instantiated.
 */
public class Diagnostics {

	
    private static List LOG_OPS = Arrays.asList("and","or","not","exists",
                                                "forall","=>","<=>","holds");

    /** *****************************************************************
     * Return a list of terms (for a given argument position) that do not 
     * have a specified relation.
     * @param kb the knowledge base
     * @param rel the relation name
     * @param argnum the argument position of the term
     * @param limit the maximum number of results to return, or -1 if all
     * @param letter the first letter of the term name
     */
    public static ArrayList<String> termsWithoutRelation(KB kb, String rel, int argnum, 
                                                 int limit, char letter) {

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
            if (limit > 0 && result.size() > limit) {
                result.add("limited to " + limit + " results");
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
        return termsWithoutRelation(kb,"documentation",1,100,' ');                                              
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
                String term = f.getArgument(1);   // Append term and language to make a key.
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
                if (result.size() > 99) {
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
        while (it.hasNext() && (count < 100)) {
            String term = it.next();
            if (LOG_OPS.contains(term) || term.equals("Entity") || StringUtil.isNumeric(term)) 
                continue;
            else {
                if (kb.kbCache.subclassOf(term,"Entity") || kb.kbCache.transInstOf(term,"Entity")) {
                    result.add(term); 
                    count++;
                }
            }
            if (count > 99) 
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
        synchronized (kb.getTerms()) {
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
                if (count > 99) {
                    result.add("limited to 100 results");
                    break;
                }
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
                String parent = form.getArgument(1);
                ArrayList<String> partition = form.argumentsToArrayList(2);
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
                        if (reduce.size() > 99) 
                            go = false;                        
                    }
                }
            }
            result.addAll(reduce);
            if (result.size() > 99) 
                result.add("limited to 100 results");            
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *****************************************************************
     * Find all terms which do not appear in any implication (rule).
     */
    public static ArrayList<String> termsWithoutRules(KB kb) {

        boolean isNaN = true;
        ArrayList<String> result = new ArrayList<String>();
        Iterator<String> it = kb.getTerms().iterator();
        synchronized (kb.getTerms()) {
            while (it.hasNext()) {
                String term = (String) it.next();
                isNaN = true;
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
                        result.add(term);
                }
                if (result.size() > 99) {
                    result.add("limited to 100 results");
                    break;
                }
            }
        }
        return result;
    }

    /** *****************************************************************
     * @return true if a quantifiers in a quantifier list is not found
     * in the body of the statement.
     */
    private static boolean quantifierNotInStatement(Formula f) {

        if (f.theFormula == null || f.theFormula.length() < 1 ||
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
        form.read(f.theFormula);
        if (form.car() != null && form.car().length() > 0) {    // This test shouldn't be needed.
            String rest = form.cdr();                   // Quantifier list plus rest of statement
            Formula quant = new Formula();
            quant.read(rest);
            String q = quant.car();                     // Now just the quantifier list.
            String body = quant.cdr();
            quant.read(q);
            ArrayList<String> qList = quant.argumentsToArrayList(0);  // Put all the quantified variables into a list.
            if (rest.indexOf("exists") != -1 || rest.indexOf("forall") != -1) { //nested quantifiers
                Formula restForm = new Formula();
                restForm.read(rest);
                restForm.read(restForm.cdr());
                if (quantifierNotInStatement(restForm)) 
                    return true;
            }
            for (int i = 0; i < qList.size(); i++) {
                String var = (String) qList.get(i);
                if (body.indexOf(var) == -1) 
                    return true;
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
    public static ArrayList<Formula> quantifierNotInBody(KB kb) {

        ArrayList<Formula> result = new ArrayList<Formula>();
		Iterator<Formula> it = kb.formulaMap.values().iterator();
		while (it.hasNext()) { 
			Formula form = (Formula) it.next();
			if ((form.theFormula.indexOf("forall") != -1)
					|| (form.theFormula.indexOf("exists") != -1)) {
				if (quantifierNotInStatement(form)) 
					result.add(form);					
			}
			if (result.size() > 19) 
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
     */
    private static void termLinks(KB kb, TreeMap termsUsed, TreeMap termsDefined) {
        
        List definitionalRelations = Arrays.asList("instance",
                                                   "subclass",
                                                   "domain",
                                                   "documentation",
                                                   "subrelation");
        synchronized (kb.getTerms()) {
            for (Iterator it = kb.getTerms().iterator(); it.hasNext();) { 
                // Check every term in the KB
                String term = (String) it.next();
                ArrayList forms = kb.ask("arg",1,term);     
                // Get every formula with the term as arg 1
                // Only definitional uses are in the arg 1 position
                if (forms != null && forms.size() > 0) {
                    for (int i = 0; i < forms.size(); i++) {
                        Formula formula = (Formula) forms.get(i);
                        String relation = formula.getArgument(0);
                        String filename = formula.sourceFile;
                        if (definitionalRelations.contains(relation)) 
                            addToMapList(termsDefined,term,filename);
                        else
                            addToMapList(termsUsed,term,filename);
                    }
                }
                forms = kb.ask("arg",2,term);   
                ArrayList newform;
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
        TreeMap termsUsed = new TreeMap();

        // A map of terms keys with an ArrayList as values listing files
        // in which the term is defined (meaning appearance in an
        // instance, subclass, domain, subrelation, or documentation statement).
        // 
        TreeMap termsDefined = new TreeMap();

        // A map of file names and ArrayList values listing term names defined
        // in the file;
        TreeMap fileDefines = new TreeMap();

        // A map of file names and ArrayList values listing term names used but not defined
        // in the file;
        TreeMap fileUses = new TreeMap();

        // A map of file name keys and TreeMap values listing file names
        // on which the given file depends.  The interior TreeMap file name
        // keys index ArrayLists of terms.  file -depends on-> filenames -that defines-> terms
        TreeMap fileDepends = new TreeMap();

        termLinks(kb,termsUsed,termsDefined);
        fileLinks(kb,fileDefines,fileUses,termsUsed,termsDefined);

        Iterator it = fileUses.keySet().iterator();
        while (it.hasNext()) {
            String fileUsesName = (String) it.next();
            ArrayList termUsedNames = (ArrayList) fileUses.get(fileUsesName);
            for (int i = 0; i < termUsedNames.size(); i++) {
                String term = (String) termUsedNames.get(i);
                ArrayList fileDependencies = (ArrayList) termsDefined.get(term);
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
            result = HTMLformatter.formatProofResult(proof,query.theFormula,
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
            result = HTMLformatter.formatProofResult(proof,query.theFormula,
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
                ArrayList processedQueries = fp.preProcess(query,false,kb); // may be multiple because of row vars.
                //System.out.println(" query = " + query);
                //System.out.println(" processedQueries = " + processedQueries);

                String processedQuery = null;
                Iterator q = processedQueries.iterator();

                System.out.println("INFO in Diagnostics.kbConsistencyCheck(): size = " + processedQueries.size());
                while (q.hasNext()) {
                    Formula f = (Formula) q.next();
                    System.out.println("INFO in Diagnostics.kbConsistencyCheck(): formula = " + f.theFormula);
                    processedQuery = f.makeQuantifiersExplicit(false);
                    System.out.println("INFO in Diagnostics.kbConsistencyCheck(): processedQuery = " + processedQuery);
                    proof = empty.ask(processedQuery,timeout,maxAnswers);
                    StringBuffer a = new StringBuffer();
                    a.append(reportAnswer(kb,proof,query,processedQuery,"Redundancy"));
                    //  if (answer.length() != 0) return answer;
                    answer.append(a);

                    StringBuffer negatedQuery = new StringBuffer();
                    negatedQuery.append("(not " + processedQuery + ")");
                    proof = empty.ask(negatedQuery.toString(),timeout,maxAnswers);
                    a.append(reportAnswer(kb,proof,query,negatedQuery.toString(),"Inconsistency"));
                    if (a.length() != 0) {
                        answer.append(a);
                        return answer.toString();
                    }
                }
                empty.tell(query.theFormula);
            }
        }
        catch ( Exception ex ) {
            return("Error in Diagnostics.kbConsistencyCheck() while executing query: " + ex.getMessage());
        }
        return "No contradictions or redundancies found.";
    }

    /** ***************************************************************
     * Test method for this class.
     */
    public static void main(String args[]) {

        //try {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB("SUMO");
            System.out.println(termsNotBelowEntity(kb));

        //}
        //catch (IOException ioe) {
        //    System.out.println("Error in Diagnostics.main(): " + ioe.getMessage());
        //    ioe.printStackTrace();
        //}      
    }


}
