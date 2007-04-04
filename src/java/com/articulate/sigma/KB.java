package com.articulate.sigma;

/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.text.ParseException;

/** *****************************************************************
 *  Contains methods for reading, writing knowledge bases and their
 *  configurations.  Also contains the inference engine process for 
 *  the knowledge base.
 */
public class KB {

     /** The inference engine process for this KB. */
    public Vampire inferenceEngine;                 
     /** The name of the knowledge base. */
    public String name;                       
     /** An ArrayList of Strings which are the full path file names of the files which comprise the KB. */
    public ArrayList constituents = new ArrayList();
     /** The natural language in which axiom paraphrases should be presented. */
    public String language = "en";    
     /** The location of preprocessed KIF files, suitable for loading into Vampire. */
    public String kbDir = null;
    /** A HashMap of HashSets, which contain all the parent classes of a given class. */
    public HashMap parents = new HashMap();
    /** A HashMap of HashSets, which contain all the child classes of a given class. */
    public HashMap children = new HashMap();
    /** A HashMap of HashSets, which contain all the disjoint classes of a given class. */
    public HashMap disjoint = new HashMap();
    /** The instance of the CELT process. */
    public CELT celt = null;
    /** A Set of Strings, which are all the terms in the KB. */
    public TreeSet terms = new TreeSet(); 

    private String _userAssertionsString = "_UserAssertions.kif";
    private HashSet formulaSet = new HashSet(); // A Set of all the formula Strings in the KB.
    private HashMap formulas = new HashMap();   // A HashMap of ArrayLists of Formulas, containing all the formulas in the KB
                                                // Keys are both the formula itself, and term indexes created in KIF.createKey()

    private HashMap formatMap = null;           // The natural language formatting strings for relations in the KB.
    private HashMap termFormatMap = null;       // The natural language strings for terms in the KB.

    /** *************************************************************
     * Constructor which takes the name of the KB and the location
     * where KBs preprocessed for Vampire should be placed.
     */
    public KB(String n, String dir) {

        name = n;
        kbDir = dir;
        try {
	    KBmanager mgr = KBmanager.getMgr();
	    if (mgr != null) { 
		String loadCelt = mgr.getPref("loadCELT");
		if ( (loadCelt != null) && loadCelt.equalsIgnoreCase("yes") ) {
		    celt = new CELT();
		}
	    }
        }
        catch (IOException ioe) {
            System.out.println("Error in KB(): " + ioe.getMessage());
            celt = null;
        }
    }

    /** *************************************************************
     * Get an ArrayList of Strings containing the language identifiers 
     * of available natural language formatting templates.
     * 
     * @return an ArrayList of Strings containing the language identifiers
     */
    public ArrayList availableLanguages() {

        ArrayList al = new ArrayList();
        ArrayList result = new ArrayList();
        ArrayList col = ask("arg",0,"format");
        if (col != null) {
            for (int i = 0; i < col.size(); i++) {
                String lang = ((Formula) col.get(i)).theFormula;
                int langStart = lang.indexOf(" ");
                int langEnd = lang.indexOf(" ",langStart+1);
                lang = lang.substring(langStart+1, langEnd);
                if (!al.contains(lang.intern())) 
                    al.add(lang.intern());
            }
        }
        return al;
    }
    
    /** *************************************************************
     * Collect the second argument of a statement.  This assumes that
     * the relation is either "instance" or "subclass"
     */
    private void cacheElements(String statementType, ArrayList cached) {

        ArrayList forms = ask("arg",0,statementType);
        System.out.print("INFO in KB.cacheElements(): ");
        if (forms != null) {
            for (int i = 0; i < forms.size(); i++) {
                Formula formula = (Formula) forms.get(i);
                if (!formula.sourceFile.substring(formula.sourceFile.length()-11,formula.sourceFile.length()).equalsIgnoreCase("_Cache.kif")) {
                    String child = formula.theFormula.substring(10,formula.theFormula.indexOf(" ",10));
                    String parent = formula.theFormula.substring(formula.theFormula.indexOf(" ",10)+1,formula.theFormula.indexOf(")",10));
                    TreeSet formulaStrings = collectFormulasFromList(forms);
                    HashSet newParents = (HashSet) parents.get(parent);
                    if (newParents != null) {
                        Iterator it = newParents.iterator();
                        while (it.hasNext()) {
                            String newParent = (String) it.next();
                            if (newParent.indexOf("(") == -1) {
                                String newFormula = "(" + statementType + " " + child + " " + newParent + ")";
                                if (!formulaStrings.contains(newFormula.intern()))
                                    cached.add(newFormula);                
                            }
                        }
                        if (i % 100 == 1) System.out.print(".");
                    }                
                }
            }
        }
        System.out.println(" ");
    }

    /** *************************************************************
     * Cache subclass and instance statements in the knowledge base.
     */
    public void cache() {

        String filename = kbDir + File.separator + File.separator + this.name + "_Cache.kif"; // Note that the double separator shouldn't be needed.
        filename = filename.intern();
        ArrayList cached = new ArrayList();
        cacheElements("instance",cached);
        cacheElements("subclass",cached);

        try {
            File f = new File(filename);
            if (f.exists())
                f.delete();
            writeFormulas(cached,filename);
            if (constituents.contains(filename)) 
                constituents.remove(filename);
            System.out.println("INFO in KB.cache(): Adding: " + filename);
            addConstituent(filename);
            KBmanager.getMgr().writeConfiguration();
        }
        catch (Exception e) {
            System.out.println("Error in KB.cache(): " + e.getMessage());
        }
    }

    /** *************************************************************
     */

    private void addParentChildEntry(String parent, String child) {
        
        //System.out.println("INFO in KB.addParentChildEntry(): Add " + parent + " and " + child);
        if (parents.containsKey(child)) {
            HashSet existingParents = (HashSet) parents.get(child);
            if (!existingParents.contains(parent))
                existingParents.add(parent);
        }
        else {
            HashSet parentList = new HashSet();
            parentList.add(parent);
            parents.put(child, parentList);
        }

        if (children.containsKey(parent)) {
            HashSet existingChildren = (HashSet) children.get(parent);
            if (!existingChildren.contains(child))
                existingChildren.add(child);
        }
        else {
            HashSet childList = new HashSet();
            childList.add(child);
            children.put(parent, childList);
        }
    }

    /** *************************************************************
     * Cycle through all terms, adding targets until there are no more 
     * changes.  This routine is calculating the transitive closure of
     * the given relation with which the list was constructed.  The list
     * is a HashMap of ArrayLists, where the key of the HashMap is related
     * to the values in its associated ArrayList but a particular transitive
     * relation.
     */
    private void calculateTransitiveClosure(HashMap list) {

        System.out.print("INFO in KB.calculateTransitiveClosure()");
        //System.out.println();
        boolean changed = true;
        while (changed) {                                   
            changed = false;
            Iterator it = list.keySet().iterator();                       
            int count = 0;
            int divisor = list.keySet().size() / 100;
            while (it.hasNext()) {                              // Iterate through the terms.
                String term = (String) it.next();                 
                //System.out.println("INFO in KB.calculateTransitiveClosure(): Term: " + term);
                HashSet targets = (HashSet) list.get(term);
                if (targets != null) {
                    Object[] targetArray = targets.toArray();
                    for (int i = 0; i <targetArray.length; i++) {      // Iterate through the targets of the term
                        String targetTerm = (String) targetArray[i];
                        //System.out.println("INFO in KB.calculateTransitiveClosure(): TargetTerm: " + targetTerm);
                        HashSet newTargets = (HashSet) list.get(targetTerm);
                        if (newTargets != null) {
                            Iterator it3 = newTargets.iterator();
                            while (it3.hasNext()) {                     // Iterator through the target's targets.
                                String newTarget = ((String) it3.next()).intern();
                                //System.out.println("INFO in KB.calculateTransitiveClosure(): NewTarget: " + newTarget);
                                if (!targets.contains(newTarget)) {
                                    targets.add(newTarget);
                                    list.put(term,targets);
                                    changed = true;
                                }
                            }
                        }
                    }
                }
                if (divisor != 0 && count++ % divisor == 1) System.out.print(".");
            }
        }
        System.out.println(" ");
    }

    /** *************************************************************
     * debugging utility
     */
    private void printParents() {

        System.out.println("INFO in printParents():  Printing parents.");
        System.out.println();
        Iterator it = parents.keySet().iterator();
        while (it.hasNext()) {
            String parent = (String) it.next();
            System.out.print(parent + " ");
            System.out.println((HashSet) parents.get(parent));
        }
        System.out.println();
    }

    /** *************************************************************
     * debugging utility
     */
    private void printChildren() {

        System.out.println("INFO in printChildren():  Printing children.");
        System.out.println();
        Iterator it = children.keySet().iterator();
        while (it.hasNext()) {
            String child = (String) it.next();
            System.out.print(child + " ");
            System.out.println((HashSet) children.get(child));
        }
        System.out.println();
    }
    
    /** *************************************************************
     * debugging utility
     */
    private void printDisjointness() {

        System.out.println("INFO in printDisjointness():  Printing disjoint.");
        System.out.println();
        Iterator it = disjoint.keySet().iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            System.out.print(term + " is disjoint with ");
            System.out.println((ArrayList) disjoint.get(term));
        }
        System.out.println();
    }
    
    /** *************************************************************
     * Determine whether a particular term is an immediate instance,
     * which has a statement of the form (instance term otherTerm).
     * Note that this does not count for terms such as Attribute(s)
     * and Relation(s), which may be defined as subAttribute(s) or
     * subrelation(s) of another instance.  If the term is not an
     * instance, return an empty ArrayList.  Otherwise, return an
     * ArrayList of the Formula(s) in which the given term is 
     * defined as an instance.
     */
    public ArrayList instancesOf(String term) {

        //System.out.println("INFO in KB.instancesOf()");
        return askWithRestriction(1,term,0,"instance");           
    }

    /** *************************************************************
     * Determine whether a particular class or instance "child" is a
     * child of the given "parent".  
     */
    public boolean childOf(String child, String parent) {

        if (child.equals(parent)) 
            return true;
        HashSet childs = (HashSet) children.get(parent);
        if (childs != null && childs.contains(child)) 
            return true;
        else {
            ArrayList al = instancesOf(child);
            Iterator it = al.iterator();
            while (it.hasNext()) {
                Formula form = (Formula) it.next();
                Formula f = new Formula();
                f.read(form.theFormula);
                f.read(f.cdr());
                f.read(f.cdr());                
                String superTerm = f.car();
                if (superTerm.equals(parent)) 
                    return true;
                if (childs != null && childs.contains(superTerm)) 
                    return true;
            }
        }
        return false;
    }

    /** *************************************************************
     * Collect all the parent and child classes of each class or instance.  Store
     * them in the global variables parents and children, which are HashMap(s) of 
     * ArrayLists.  The key is a class, and the value is an ArrayList of
     * Strings, which are the class' parent or child classes.  Note that this 
     * routine does not check to make sure that the second argument of
     * "instance" is a class.
     */
    private void collectParentsAndChildren() {

        parents = new HashMap();
        ArrayList al;

        System.out.print("INFO in KB.collectParentsAndChildren(): Caching class hierarchy.");
        Iterator it = terms.iterator(); 
        int count = 0;
        int divisor = terms.size() / 100;
        while (it.hasNext()) {                   // Collect the immediate parents and children for each term.
            String term = (String) it.next();                 
            
            //System.out.println("INFO in KB.collectParentsAndChildren(): Term: " + term);
            //System.out.println("INFO in KB.collectParentsAndChildren(): Collect arg1 terms.");
            ArrayList forms = ask("arg",1,term);
            TreeSet f = new TreeSet();
            if (forms != null) 
                f.addAll(forms);

            forms = ask("arg",2,term);
            f = new TreeSet();
            if (forms != null) 
                f.addAll(forms);

            forms = new ArrayList();
            if (f != null) 
                forms.addAll(f);

            if (forms != null && forms.size() > 0) {            
                for (int i = 0; i < forms.size(); i++) {
                    Formula formula = (Formula) forms.get(i);
                    if (formula.theFormula.indexOf("(",2) == -1 &&      // Ignore cases where parent class is a function
                        !formula.sourceFile.substring(formula.sourceFile.length()-11,formula.sourceFile.length()).equalsIgnoreCase("_Cache.kif")) {    
                        if (formula.theFormula.substring(1,9).equalsIgnoreCase("instance") || 
                            formula.theFormula.substring(1,9).equalsIgnoreCase("subclass") || 
                            formula.theFormula.substring(1,12).equalsIgnoreCase("subrelation") || 
                            formula.theFormula.substring(1,12).equalsIgnoreCase("subAttribute")) { 
                            String parent = formula.theFormula.substring(formula.theFormula.indexOf(" ",10)+1,
                                                                         formula.theFormula.indexOf(")",10));
                            String child = formula.theFormula.substring(10,formula.theFormula.indexOf(" ",10));
                            addParentChildEntry(parent.intern(),child.intern());
                        }
                    }
                }
            }
            if (count++ % 10 == 1) System.out.print(".");
        }
        System.out.println();

        //System.out.print("INFO in KB.collectParentsAndChildren(): Caching parents.");
        calculateTransitiveClosure(parents);
        //System.out.print("INFO in KB.collectParentsAndChildren(): Caching children.");
        calculateTransitiveClosure(children);
        //System.out.print("INFO in KB.collectParentsAndChildren(): Print parents.");
        //printParents();
        //System.out.print("INFO in KB.collectParentsAndChildren(): Print children.");
        //printChildren();
    }

    /** *************************************************************
     * Add entries to the list of disjoint classes.  Called only by
     * collectDisjointness().
     */
    private void addDisjointnessEntry(String term1, String term2) {

        if (disjoint.containsKey(term1.intern())) {
            HashSet existingDisjoint = (HashSet) disjoint.get(term1.intern());
            if (!existingDisjoint.contains(term2.intern()))
                existingDisjoint.add(term2.intern());
            disjoint.put(term1.intern(), existingDisjoint);
        }
        else {
            HashSet disjointList = new HashSet();
            disjointList.add(term2.intern());
            disjoint.put(term1.intern(), disjointList);
        }
    }

    /** *************************************************************
     * Collect all assertions of the form (disjoint Arg1 Arg2) and store
     * them in the variable "disjoint".  Calls addDisjointnessEntry to
     * perform the actual addition to the disjoint HashMap.
     */
    private void collectDisjointAssertions() {

        System.out.print("INFO in collectDisjointAssertions().");
        Iterator it = terms.iterator(); 
        int count = 0;
        while (it.hasNext()) {
            String term = (String) it.next();                 // Collect the immediate disjoints for each term.
            ArrayList forms = ask("arg",1,term);
            if (forms != null && forms.size() > 0) { 
                for (int i = 0; i < forms.size(); i++) {
                    Formula formula = (Formula) forms.get(i);
                    if (formula.theFormula.indexOf("(",2) == -1 &&
                        !formula.sourceFile.endsWith("_Cache.kif")) {    // Ignore cases where parent class is a function
                        if (formula.theFormula.substring(1,9).equalsIgnoreCase("disjoint")) { 
                            String disjointStr = formula.theFormula.substring(formula.theFormula.indexOf(" ",10)+1,
                                                                              formula.theFormula.indexOf(")",10));
                            addDisjointnessEntry(term,disjointStr);
                            addDisjointnessEntry(disjointStr,term);
                        }
                    }
                }
            }
            if (count++ % 100 == 1) System.out.print(".");
        }
        System.out.println();
    }

    /** *************************************************************
     * Collect disjointness from all statements with the relations
     * "partition" and "disjointDecomposition" and store in the
     * "disjoint" HashMap.
     */
    private void collectPartitions() {

        System.out.print("INFO in collectPartitions().");
        ArrayList forms = ask("arg",0,"partition");
        ArrayList forms2 = ask("arg",0,"disjointDecomposition");
        if (forms == null) 
            forms = new ArrayList();
        if (forms2 != null) 
            forms.addAll(forms2);
        if (forms != null && forms.size() > 0) { 
            for (int i = 0; i < forms.size(); i++) {
                Formula formula = (Formula) forms.get(i);
                ArrayList mutualDisjoints = formula.argumentsToArrayList(2);
                for (int x = 0; x < mutualDisjoints.size(); x++) {
                    for (int y = 0; y < mutualDisjoints.size(); y++) {
                        if (x != y) {
                            String term1 = (String) mutualDisjoints.get(x);
                            String term2 = (String) mutualDisjoints.get(y);
                            addDisjointnessEntry(term1,term2);
                        }
                    }
                }
                if (i % 1000 == 1) System.out.print(".");
            }
        }
        System.out.println();
    }

    /** *************************************************************
     * Augment the disjoint HashMap by adding all the children (subclasses
     * and instances) of each target class.  For example, if A and B
     * are disjoint and C is a subclass of B, A is also disjoint with C.
     */
    private void addDisjointChildren() {

        System.out.print("INFO in addDisjointChildren().");
        Iterator it = disjoint.keySet().iterator();      
        while (it.hasNext()) {
            String term = (String) it.next();                 
            HashSet dis = (HashSet) disjoint.get(term);
            if (dis != null) {
                HashSet newDises = new HashSet();
                newDises.addAll(dis);
                int i = 0;
                Iterator it2 = dis.iterator();
                while (it2.hasNext()) {
                    String disTerm = (String) it2.next();
                    i++;
                    HashSet k = ((HashSet) children.get(disTerm));
                    if (k != null) {
                        Object[] kids = ((HashSet) children.get(disTerm)).toArray();
                        for (int j = 0; j < kids.length; j++) {
                            String newDis = ((String) kids[j]).intern();
                            if (!newDises.contains(newDis)) {
                                newDises.add(newDis);
                            }
                        }
                    }
                    if (i % 1000 == 1) System.out.print(".");
                }
                disjoint.put(term,newDises);
            }
        }
        System.out.println(" ");
    }

    /** *************************************************************
     * Collect all the classes which are disjoint with each class.
     * Initially just collects statements with the relations "disjoint",
     * "partition" and "disjointDecomposition".
     */
    private void collectDisjointness() {
        
        disjoint = new HashMap();
        collectDisjointAssertions();
        //printDisjointness();
        collectPartitions();
        //printDisjointness();
        addDisjointChildren();
        //printDisjointness();
    }

    /** *************************************************************
     * Get an ArrayList of Formulas in which the two terms provided appear
     * in the indicated argument positions.  If there are no Formula(s)
     * matching the given terms and respective argument positions,
     * return an empty ArrayList.
     */
    public ArrayList askWithRestriction(int argnum1, String term1, int argnum2, String term2) {

        ArrayList partial = ask("arg",argnum1,term1);
        ArrayList result = new ArrayList();
        if (partial != null) {
            for (int i = 0; i < partial.size(); i++) {
                Formula f = (Formula) partial.get(i);
                if (f.getArgument(argnum2).equals(term2)) {
                    result.add(f);
                }
            }
        }
        return result;
    }

    /** *************************************************************
     * Get an ArrayList which are the Formulas the match the request.
     *
     * @param kind - May be one of "ant", "cons", "stmt", or "arg", @see KIF.createKey()
     * @param term - The term that appears in the statements being requested.
     * @param argnum - The argument position of the term being asked for.  The
     * first argument after the predicate is "1". This parameter is ignored if
     * the kind is "ant", "cons" or "stmt".
     * @return an ArrayList of Formula(s), or null if no match found.
     */
    public ArrayList ask(String kind, int argnum, String term) {
        
        if (kind.compareTo("arg") == 0) 
            return (ArrayList) formulas.get(kind + "-" + (new Integer(argnum)).toString() + "-" + term);        
        else 
            return (ArrayList) formulas.get(kind + "-" + term);        
    }

    /** *************************************************************
     *  Merge a KIF object containing a single formula into the current KB
     */
    private void merge(KIF kif) {

        terms.addAll(kif.terms);                                   // Add all the terms from the new formula into the KB's current list
        Set keys = kif.formulas.keySet();
        Iterator it = keys.iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            ArrayList newValues = new ArrayList((ArrayList) kif.formulas.get(key));
            if (formulas.containsKey(key)) {
                ArrayList values = (ArrayList) formulas.get(key);
                for (int i = 0; i < newValues.size(); i++) {
                    Formula value = (Formula) newValues.get(i);
                    if (key.charAt(0) == '(') {                    // The key is the formula itself. 
                        String filename = value.sourceFile;        // Check if the formula has already been asserted from the same file.
                        boolean found = false;
                        for (int j = 0; j < values.size(); j++) {
                            Formula oldValue = (Formula) newValues.get(j);
                            if (oldValue.sourceFile.compareTo (filename) == 0) 
                                found = true;
                        }
                        if (!found) 
                            values.add(value);                        
                    }
                    else
                        values.add(value);            
                }
            }
            else
                formulas.put(key,newValues);                            
        }
        /* collectParents();
        if (KBmanager.getMgr().getPref("cache") != null &&
            KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes"))
            cache();  */     // caching is too slow to perform for just one formula
    }

    /** *************************************************************
     *  Write a assertions to a file.
     * @param formulas an AraryList of Strings
     * @param fname the fully qualified file name
     */
    private void writeFormulas(ArrayList formulas, String fname) throws IOException {

        FileWriter fr = null;

        try {
            fr = new FileWriter(fname,true);
            for (int i = 0; i < formulas.size(); i++) {
                fr.write((String) formulas.get(i));
                fr.write("\n");
            }
        }
        catch (java.io.IOException e) {
            System.out.println("Error writing file " + fname);
        }
        finally {
            if (fr != null) 
                fr.close();           
        }
    }

    /** *************************************************************
     *  Write a single user assertions to the end of a file.
     */
    private void writeUserAssertion(String formula, String fname) throws IOException {

        FileWriter fr = null;

        try {
            fr = new FileWriter(fname,true);   
            fr.write(formula);
            fr.write("\n");
        }
        catch (java.io.IOException e) {
            System.out.println("Error writing file " + fname);
        }
        finally {
            if (fr != null) 
                fr.close();           
        }
    }

    /** *************************************************************
     *  Add a formula to the knowledge base.  Returns an XML formatted
     * String that contains the response of the inference engine.  It
     * should be in the form "<assertionResponse>...</assertionResponse>"
     * where the body should be " Formula has been added to the session 
     * database" if all went well.
     */
    public String tell(String formula) {

        KIF kif = new KIF();
        kif.parseStatement(formula,this.name + _userAssertionsString);
        merge(kif);
        Formula f = new Formula();
        ArrayList theFormulas = null;
        f.theFormula = formula;
        theFormulas = f.preProcess(false,this);  // tell is not a query

        try {
            Iterator itf = theFormulas.iterator();
            while (itf.hasNext()) {
                f.theFormula = ((Formula)itf.next()).theFormula;

                String filename = kbDir + File.separator + this.name + _userAssertionsString;
                filename = filename.intern();
                File file = new File(filename);
                if (!constituents.contains(filename)) {
                    System.out.println("INFO in KB.tell(): Adding file: " + filename + " to: " + constituents.toString());
                    if (file.exists())                      // If the assertions file exists
                        file.delete();
                    constituents.add(filename);
                    KBmanager.getMgr().writeConfiguration();
                }
                writeUserAssertion(formula,filename);
                return inferenceEngine.assertFormula(f.theFormula);
            }
        }
        catch (IOException ioe) {
            System.out.println("Error in KB.tell(): " + ioe.getMessage());
        }
        /* collectParents();
        if (KBmanager.getMgr().getPref("cache") != null &&
            KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes"))
             cache();        */   // caching is currently not efficient enough to invoke it after every assertion
        return "";
    }

    /** ***************************************************************
     *  Take a term and return whether the term exists in the knowledge base.
     */
    public boolean containsTerm(String term) {

        return terms.contains(term.intern());
    }      
    
    /** ***************************************************************
     *  Take a formula string and return whether it exists in the knowledge base.
     */
    public boolean containsFormula(String formula) {

        return formulaSet.contains(formula.intern());
    }      
    
    /** ***************************************************************
     *  Count the number of terms in the knowledge base in order to
     *  present statistics to the user.
     *
     *  @return The int(eger) number of terms in the knowledge base.
     */
    public int getCountTerms() {

        return terms.size();
    }      
    
    /** ***************************************************************
     *  Count the number of relations in the knowledge base in order to
     *  present statistics to the user.
     *
     *  @return The int(eger) number of relations in the knowledge base.
     */
    public int getCountRelations() {

        int result = 0;
        Iterator it = terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            if (childOf(term,"Relation")) {            
                result++;
                System.out.println("INFO in KB.getCountRelations(): " + term);
            }
        }
        return result;
    }      
    
    /** ***************************************************************
     *  Count the number of formulas in the knowledge base in order to
     *  present statistics to the user.
     *
     *  @return The int(eger) number of formulas in the knowledge base.
     */
    public int getCountAxioms() {

        /** TreeSet formulaSet = new TreeSet();
        Iterator ite = formulas.values().iterator();
        while (ite.hasNext()) {
            ArrayList al = (ArrayList) ite.next();
            for (int i = 0; i < al.size(); i++) {
                formulaSet.add(((Formula) al.get(i)).theFormula);
            }
        }   */     
        return formulaSet.size();
    }  

    /** ***************************************************************
     *  an accessor providing a TreeSet of un-preProcess-ed Formula(s).
     *  @return A Treeset of un-preProcess(ed) Formula(s).
     */
    public TreeSet getFormulas() {

        TreeSet newFormulaSet = new TreeSet();
        Iterator it = formulas.values().iterator();
        while (it.hasNext()) {
            ArrayList al = (ArrayList) it.next();
            newFormulaSet.addAll(al);
        }
        return newFormulaSet;
    }      
    
    /** ***************************************************************
     *  Count the number of rules in the knowledge base in order to
     *  present statistics to the user. Note that the number of rules
     *  is a subset of the number of formulas.
     *
     *  @return The int(eger) number of rules in the knowledge base.
     */
    public int getCountRules() {

        TreeSet newFormulaSet = new TreeSet();
        Iterator ite = formulas.values().iterator();
        while (ite.hasNext()) {
            ArrayList al = (ArrayList) ite.next();
            for (int i = 0; i < al.size(); i++) {
                if (((Formula) al.get(i)).theFormula.substring(1,3).compareTo("=>") == 0)
                    newFormulaSet.add(((Formula) al.get(i)).theFormula);
            }
        }        
        return newFormulaSet.size();
    }
 
    /** ***************************************************************
     * Create an ArrayList of the specific size, filled with empty strings.
     */
    private ArrayList arrayListWithBlanks(int size) {

        ArrayList al = new ArrayList(size);
        for (int i = 0; i < size; i++) 
            al.add("");
        return al;
    }

    /** ***************************************************************
     * Get the alphabetically nearest terms to the given term, which
     * is not in the KB.  Elements 0-14 should be alphabetically lesser and 
     * 15-29 alphabetically greater.  If the term is at the beginning or end
     * of the alphabet, fill in blank items with the empty string: "".
     */
    private ArrayList getNearestTerms(String term) {

        ArrayList al = arrayListWithBlanks(30);
        Object[] t = terms.toArray();
        int i = 0;
        while (i < t.length && ((String) t[i]).compareTo(term) < 0) 
            i++;
        int lower = i;
        while (i - lower < 15 && lower > 0) { 
            lower--;
            al.set(15 - (i - lower),(String) t[lower]);
        }
        int upper = i-1;
        System.out.println(t.length);
        while (upper - i < 14 && upper < t.length-1) {        
            upper++;
            al.set(15 + (upper - i),(String) t[upper]);
        }
        return al;       
    }

    /** ***************************************************************
     * Get the neighbors of this initial uppercase term (class or function).
     */
    public ArrayList getNearestRelations(String term) {

        term = Character.toUpperCase(term.charAt(0)) + term.substring(1,term.length());
        return getNearestTerms(term);
    }
    
    /** ***************************************************************
     * Get the neighbors of this initial lowercase term (relation).
     */
    public ArrayList getNearestNonRelations(String term) {

        term = Character.toLowerCase(term.charAt(0)) + term.substring(1,term.length());
        return getNearestTerms(term);
    }

    /** ***************************************************************
     */
    private void reloadFormatMaps(String lang) {

        String l;

        formatMap = new HashMap();
        termFormatMap = new HashMap();
        System.out.println("INFO in KB.getFormatMap(): Reading the format maps for " + lang);
        if (lang == null)
            l = language;
        else 
            l = lang;
        ArrayList col = this.ask("arg",0,"format");
        if (col == null) {
            System.out.println("Error in KB.getFormatMap(): No relation formatting file loaded for language: " + lang);
            return;
        }
        //System.out.println("Number of format statements: " + (new Integer(col.size())).toString());
        formatMap = new HashMap();
        Iterator ite = col.iterator();
        while (ite.hasNext()) {
            String strFormat = ((Formula) ite.next()).theFormula;
            if (strFormat.substring(8,10).compareTo(l) == 0) {
                int after = strFormat.indexOf("format");
                int theFirstQuote = strFormat.indexOf("\"");
                int theSecondQuote = strFormat.indexOf("\"", theFirstQuote+1);
                String key = strFormat.substring(after+9,theFirstQuote-1).trim();
                String format = strFormat.substring(theFirstQuote+1, theSecondQuote).trim();
                if (format.indexOf("$") < 0)
                    format = format.replaceAll("\\x26\\x25", "\\&\\%"+key+"\\$");
                formatMap.put(key,format);
            }
        }

        col = this.ask("arg",0,"termFormat");
        if (col == null) {
            System.out.println("Error in KB.getTermFormatMap(): No term formatting file loaded for language: " + lang);
            return;
        }
        //System.out.println("Number of format statements: " + (new Integer(col.size())).toString());
        termFormatMap = new HashMap();
        ite = col.iterator();
        while (ite.hasNext()) {
            String strFormat = ((Formula) ite.next()).theFormula;
            if (strFormat.substring(12,14).compareTo(l) == 0) {
                int after = strFormat.indexOf("termFormat");
                int theFirstQuote = strFormat.indexOf("\"");
                int theSecondQuote = strFormat.indexOf("\"", theFirstQuote+1);
                if (after < 0 || theFirstQuote < 1) {
                    System.out.println("Error in KB.getTermFormatMap(): Bad format statement: " + strFormat);
                    return;
                }
                String key = strFormat.substring(after+13,theFirstQuote-1).trim().intern();
                String format = strFormat.substring(theFirstQuote+1, theSecondQuote).trim();
                //if (format.indexOf("$") < 0)
                //    format = format.replaceAll("\\x26\\x25", "\\&\\%"+key+"\\$");
                termFormatMap.put(key,format);
            }
        }
        language = lang;
    }

    /** ***************************************************************
     *  This method creates an association list (Map) of the natural language
     *  format string and the relation name for which that format string applies.  If
     *  the map has already been built and the language hasn't changed, just return
     *  the existing map.  This is a case of "lazy evaluation".
     *
     *  @return An instance of Map where the keys are relation names and the values are format strings.
     */
    public HashMap getFormatMap(String lang) {

        if ((formatMap == null) || (!lang.equalsIgnoreCase(language)))
            reloadFormatMaps(lang);        
        return formatMap;
    }

    /** ***************************************************************
     * Delete the user assertions file.
     */
    public void deleteUserAssertions() {

        for (int i = 0; i < constituents.size(); i++) {
            if (((String) constituents.get(i)).indexOf(_userAssertionsString) != -1) {
                constituents.remove(i);
                try {
                    KBmanager.getMgr().writeConfiguration();
                }
                catch (IOException ioe) {
                    System.out.println("Error in KB.deleteUserAssertions: Error writing configuration: " + ioe.getMessage());
                }
                reload();
                return;
            }
        }
    }

    /** ***************************************************************
     *  This method creates an association list (Map) of the natural language
     *   string and the term for which that format string applies.  If
     *  the map has already been built and the language hasn't changed, just return
     *  the existing map.  This is a case of "lazy evaluation".
     *
     *  @return An instance of Map where the keys are terms and the values are format strings.
     */
    public HashMap getTermFormatMap(String lang) {

        //System.out.println("INFO in KB.getTermFormatMap(): Reading the format map for " + lang + " with language=" + language);
        if ((termFormatMap == null) || (!lang.equalsIgnoreCase(language))) 
            reloadFormatMaps(lang);
        return termFormatMap;
    }

    /** *************************************************************
     * Add a new KB constituent by reading in the file, and then merging
     * the formulas with the existing set of formulas.
     * @param filename - the full path of the file being added.
     */
    public String addConstituent (String filename) throws IOException {

        StringBuffer result = new StringBuffer();
        Iterator it;
        KIF file = new KIF();
        String key;
        ArrayList list;
        ArrayList newList;

        if (constituents.contains(filename.intern())) return "Error: " + filename + " already loaded.";
        System.out.println("INFO KB.addConstituent(): Adding " + filename);
        try { 
            file.readFile(filename);
        }
        catch (IOException ioe) {
            throw new IOException(ioe.getMessage());
        }
        catch (ParseException pe) {
            result.append(pe.getMessage() + "At line: " + pe.getErrorOffset());
            //KBmanager.getMgr().setError(result.toString());
            return result.toString();
        }
        System.out.print("INFO in KB.addConstituent(): Read file: " + filename + " of size: ");
        System.out.print(file.formulas.keySet().size());
        it = file.formulas.keySet().iterator();
        int count = 0;
        while (it.hasNext()) {                // Iterate through the formulas in the file, adding them to the KB, at the appropriate key.
            key = (String) it.next();         // Note that this is a slow operation that needs to be improved
            // System.out.println("INFO KB.addConstituent(): Key " + key);
            if (count++ % 100 == 1) System.out.print(".");            
            if (formulas.containsKey(key)) {
                list = (ArrayList) formulas.get(key);
                if (list == null) 
                     return "Error: Bad data in existing constituents at key: " + key; 
                newList = (ArrayList) file.formulas.get(key);
                for (int i = 0; i < newList.size(); i++) {          // Don't add formulas to the KB that already exist in the same file.
                    //if (i % 100 == 1) System.out.print("+");    
                    Formula f = (Formula) newList.get(i);
                    if (!formulaSet.contains(f.theFormula.intern()))
                        list.add(newList.get(i));                   // Add Formula to the existing ArrayList of Formulas 
                    else {
                        result.append("Warning: Duplicate axiom in ");
                        result.append(f.sourceFile + " at line " + f.startLine + "<BR>");
                        result.append(f.theFormula + "<P>");
                        Formula existingFormula = (Formula) ((ArrayList) formulas.get(f.theFormula.intern())).get(0);
                        result.append("Warning: Existing formula appears in ");
                        result.append(existingFormula.sourceFile + " at line " + existingFormula.startLine + "<BR>");
                        result.append("<P>");
                    }
                                                                    // This inner loop is the slow part
                    /**boolean found = false;
                    for (int j = 0; j < list.size(); j++) {         
                        if (j % 10000 == 1) System.out.print("!");            
                        if (f.deepEquals((Formula) list.get(j))) 
                            found = true;
                    }
                    if (!found) 
                        list.add(newList.get(i));  */                 // Add Formula to the existing ArrayList of Formulas
                }
            }
            else {
                ArrayList forms = (ArrayList) file.formulas.get(key);
                formulas.put(key,forms);
            }
        }
        formulaSet.addAll(file.formulaSet);
        //System.out.println();
        it = file.terms.iterator();
        while (it.hasNext()) {
            key = (String) it.next();
            this.terms.add(key);
        }
        constituents.add(filename.intern());
        //System.out.print("INFO KB.addConstituent(): Number of constituents ");
        //System.out.println(constituents.size());
        //System.out.print("INFO KB.addConstituent(): Number of formulas ");
        //System.out.println(file.formulas.values().size());
        loadVampire();
	System.out.println( "filename == " + filename );
	int startIdx = filename.lastIndexOf(File.separator);
	System.out.println( "startIdx == " + startIdx );
	int lastIdx = filename.length();
	System.out.println( "lastIdx == " + lastIdx );
	String subStr = filename.substring( startIdx, lastIdx );
	System.out.println( "subStr == " + subStr );
        if (subStr.compareTo("_Cache.kif") != 0) {
            collectParentsAndChildren();
            collectDisjointness();
        }
        return result.toString();
    }

    /** ***************************************************************
     * Reload all the KB constituents.
     */
    public String reload() {

        StringBuffer result = new StringBuffer();
        System.out.println("INFO in KB.reload(): Reloading.");
        ArrayList newConstituents = new ArrayList(constituents);
        constituents = new ArrayList();
        language = "en";                  
        formulas = new HashMap();       
        formulaSet = new HashSet();       
        terms = new TreeSet();          
        formatMap = null; 
        termFormatMap = null; 

        for (int i = 0; i < newConstituents.size(); i++) {
            try {
                result.append(addConstituent((String) newConstituents.get(i)));        
            }
            catch (IOException ioe) {
                System.out.println("Error in KB.reload(): " + ioe.getMessage());
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     * Write a KIF file.
     * @param fname - the name of the file to write, including full path.
     */
    public void writeFile(String fname) throws IOException {

        FileWriter fr = null;
        PrintWriter pr = null;
        Iterator it;
        HashSet formulaSet = new  HashSet();
        ArrayList formulaArray;
        String key;
        ArrayList list;
        Formula f;
        String s;

        try {
            fr = new FileWriter(fname);
            pr = new PrintWriter(fr);

            it = formulas.keySet().iterator();
            while (it.hasNext()) {
                key = (String) it.next();
                list = (ArrayList) formulas.get(key);
                for (int i = 0; i < list.size(); i++) {
                    f = (Formula) list.get(i);
                    s = f.toString();
                    pr.println(s);
                    pr.println();
                }
            }
        }
        catch (java.io.IOException e) {
            throw new IOException("Error writing file " + fname);
        }
        finally {
            if (pr != null) {
                pr.close();
            }
            if (fr != null) {
                fr.close();
            }
        }
    }

    /** *************************************************************
     * Create the XML configuration element.
     */
    public SimpleElement writeConfiguration() {

        SimpleElement se = new SimpleElement("kb");
        se.setAttribute("name",name);
        for (int i = 0; i < constituents.size(); i++) {
            SimpleElement constituent = new SimpleElement("constituent");
            String filename = (String) constituents.get(i);
            filename = KBmanager.escapeFilename(filename);
            constituent.setAttribute("filename",filename);
            se.addChildElement(constituent);
        }
        return se;
    }

    /** *************************************************************
     * A HashMap for holding compiled regular expression patterns.
     * The map is initialized by calling compilePatterns().
     */
    private HashMap patterns = null;

    /** ***************************************************************
     * This method returns a compiled regular expression Pattern
     * object indexed by key.
     *
     * @param key A String that is the retrieval key for a compiled
     * regular expression Pattern.
     *
     * @return A compiled regular expression Pattern instance.
     */
    private Pattern getCompiledPattern(String key) {
	if ( isNonEmptyString(key) && (patterns != null) ) {
	    ArrayList al = (ArrayList) this.patterns.get( key );
	    if ( al != null ) {
		return (Pattern) al.get( 0 );
	    }
	}
	return null;
    }

    /** ***************************************************************
     * This method returns the int value that identifies the regular
     * expression binding group to be returned when there is a match.
     *
     * @param key A String that is the retrieval key for the binding
     * group index associated with a compiled regular expression
     * Pattern.
     *
     * @return An int that indexes a binding group.
     */
    private int getPatternGroupIndex(String key) {
	if ( isNonEmptyString(key) && (patterns != null) ) {
	    ArrayList al = (ArrayList) this.patterns.get( key );
	    if ( al != null ) {
		return ((Integer)al.get( 1 )).intValue();
	    }
	}
	return -1;
    }

    /** ***************************************************************
     * This method compiles and stores regular expression Pattern
     * objects and binding group indexes as two cell ArrayList
     * objects.  Each ArrayList is indexed by a String retrieval key.
     *
     * @return void
     */
    private void compilePatterns() {
	if ( this.patterns == null ) {
	    this.patterns = new HashMap();
	    String[][] patternArray = 
		{ { "row_var", "\\@ROW\\d*", "0" },
		  // { "open_lit", "\\(\\w+\\s+\\?\\w+\\s+.\\w+\\s*\\)", "0" },
		  { "open_lit", "\\(\\w+\\s+\\?\\w+[a-zA-Z_0-9-?\\s]+\\)", "0" },
		  { "pred_var_1", "\\(holds\\s+(\\?\\w+)\\W", "1" },
		  { "pred_var_2", "\\((\\?\\w+)\\W", "1" }
		};
	    String pName   = null;
	    Pattern p      = null;
	    Integer groupN = null;
	    ArrayList pVal = null;
	    for ( int i = 0 ; i < patternArray.length ; i++ ) {
		pName  = patternArray[i][0];
		p      = Pattern.compile( patternArray[i][1] );
		groupN = new Integer( patternArray[i][2] );
		pVal   = new ArrayList();
		pVal.add( p );
		pVal.add( groupN );
		patterns.put( pName, pVal );
	    }
	}
	return;
    }

    /** ***************************************************************
     * This method finds regular expression matches in an input string
     * using a compiled Pattern and binding group index retrieved with
     * patternKey.  If the ArrayList accumulator is provided, match
     * results are added to it and it is returned.  If accumulator is
     * not provided (is null), then a new ArrayList is created and
     * returned if matches are found.
     *
     * @param input The input String in which matches are sought.
     *
     * @param patternKey A String used as the retrieval key for a
     * regular expression Pattern object, and an int index identifying
     * a binding group.
     *
     * @param accumulator An optional ArrayList to which matches are
     * added.  Note that if accumulator is provided, it will be the
     * return value even if no new matches are found in the input
     * String.
     *
     * @return An ArrayList, or null if no matches are found and
     * accumulator is not provided.
     */
    private ArrayList getMatches(String input, String patternKey, ArrayList accumulator) {
	ArrayList ans = null;
	if ( accumulator != null ) {
	    ans = accumulator;
	}
	compilePatterns();
	if ( isNonEmptyString(input) && isNonEmptyString(patternKey) ) {
	    Pattern p = this.getCompiledPattern( patternKey );
	    if ( p != null ) {
		Matcher m = p.matcher( input );
		int gidx = getPatternGroupIndex( patternKey );
		if ( gidx >= 0 ) {
		    while ( m.find() ) {
			String rv = m.group( gidx );
			if ( isNonEmptyString(rv) ) {
			    if ( ans == null ) {
				ans = new ArrayList();
			    }
			    if ( !(ans.contains(rv)) ) {
				ans.add( rv );
			    }
			}
		    }
		}
	    }
	}
	return ans;
    }

    /** ***************************************************************
     * This method finds regular expression matches in an input string
     * using a compiled Pattern and binding group index retrieved with
     * patternKey, and returns the results, if any, in an ArrayList.
     *
     * @param input The input String in which matches are sought.
     *
     * @param patternKey A String used as the retrieval key for a
     * regular expression Pattern object, and an int index identifying
     * a binding group.
     *
     * @return An ArrayList, or null if no matches are found.
     */
    private ArrayList getMatches(String input, String patternKey) {
	return this.getMatches( input, patternKey, null );
    }

    /** ***************************************************************
     * This method gathers all formulas in the input KB that have
     * predicate variables, and generates a list of formulas, possibly
     * simpler than the original, in which the predicate variables
     * have been replaced with appropriate instances of Predicate.
     *
     * @param kb The KB to process.
     *
     * @return An ArrayList of formulas in which predicate variables
     * have been replaced with instances of Predicate (or sometimes
     * Relation).
     */
    public static ArrayList instantiatePredicateMetaRules(KB kb) {
	if ( kb instanceof KB ) {
	    kb.cache();
	    return kb.instantiatePredicateMetaRules();
	}
	return null;
    }

    /** ***************************************************************
     * This method gathers all formulas in the KB that have predicate
     * variables, and generates a list of formulas, possibly simpler
     * than the original, in which the predicate variables have been
     * replaced with appropriate instances of Predicate.
     *
     * @return An ArrayList of formulas in which predicate variables
     * have been replaced with instances of Predicate (or sometimes
     * Relation).
     */
    public ArrayList instantiatePredicateMetaRules() {
	System.out.print( "INFO in instantiatePredicateMetaRules(): " );
	// long start = System.currentTimeMillis();
	ArrayList ans = new ArrayList();
	ArrayList working = new ArrayList();
	ArrayList accumulator = new ArrayList();
	ArrayList metaRules = collectPredicateMetaRules();

	// System.out.println( metaRules.size() + " predicate meta rules found" );

	if ( metaRules.size() > 0 ) {
	    List formulaData = null;
	    Formula formula = null;
	    String formStr = null;
	    List queryLits = null;
	    int qlSize = -1;
	    int qlLastI = -1;
	    List queryLit = null;
	    List ansLits = null;
	    int alSize = -1;
	    List groundLit = null;
	    String template = null;
	    String var = null;
	    String term = null;
	    boolean tried = false;
	    int totalCount = 0;
	    for ( int h = 0 ; h < metaRules.size() ; h++ ) {
		working.clear();
		accumulator.clear();
		formulaData = (List) metaRules.get( h );
		tried = ((String) formulaData.get( 0 )).equals("yes");
		formula = (Formula) formulaData.get( 1 );
		queryLits = (List) formulaData.get( 2 );
		if ( tried ) {
		    formula = (Formula) formulaData.get( 3 );
		    queryLits = (List) formulaData.get( 4 );
		}

		formStr = formula.theFormula;
		qlSize = queryLits.size();
		qlLastI = (qlSize - 1);

		working.add( formStr );

		/*
		if ( tried ) {
		    System.out.println();
		    System.out.println( "Retrying a form for which a narrower query failed" );
		    System.out.println();
		}
		*/

		// Iterate over the query literals.
		for ( int i = 0 ; i < qlSize ; i++ ) {
		    queryLit = (List) queryLits.get( i );
		    int litLen = queryLit.size();

		    /*
		    System.out.println();
		    System.out.print( "[" + exampleCounter + "]: Applying " + queryLit );
		    exampleCounter += 1;
		    */

		    // Get the list of query answers.
		    ansLits = (List) collectPredVarLiterals( queryLit );

		    if ( !(ansLits instanceof List) || ansLits.isEmpty() ) {

			/*
			System.out.println();
			System.out.println( "No answers found; will retry later with a different query" );
			*/

			formulaData.set( 0, "yes" );
			metaRules.add( formulaData );
			break;
		    }
		    else {
			
			alSize = ansLits.size();
			// Iterate over the list of query answers.
			for ( int j = 0 ; j < alSize ; j++ ) {
			
			    /*
			    if ( (j == 0) ) {
				System.out.print( ", " + alSize + " forms, to " );
			    }
			    */

			    groundLit = literalToArrayList( (Formula) ansLits.get( j ) );
	   
			    int wLen = working.size();
			    // Iterate over the list of working
			    // templates (forms to be filled in).
			    for ( int k = 0 ; k < wLen ; k++ ) {
				template = (String) working.get( k );

				/*
				if ( (j == 0) && (k == 0) ) {
				    System.out.println( template );
				    System.out.println();
				}
				*/

				// Iterate over the terms (variables
				// and constants) in one answer
				// literal.
				for ( int m = 0 ; m < litLen ; m++ ) {
				    var = (String) queryLit.get( m );
				    if ( isVariable(var) ) {
					term = (String) groundLit.get( m );

					template = template.replace( ("holds " + var), term );
					template = template.replace( var, term );
				    }
				}

				/*
				if ( (j == 0) && (k < 5) ) {
				    System.out.println( "Example: " + template );
				    System.out.println();
				}
				*/

				accumulator.add( template );
				totalCount += 1;
				if ( (totalCount % 100) == 1 ) {
				    System.out.print( "." );
				}
			    }
			}
		    }
		    // If we have applied the results from all of the
		    // query literals to generate all the instances
		    // for a single rule, add the instances to ans.
		    if ( i == qlLastI ) {
			ans.addAll( accumulator );
		    }
		    // Otherwise, save the intermediate forms and
		    // apply the results of the next query literal to
		    // them.
		    else {
			working = accumulator;
			accumulator = new ArrayList();
		    }
		}
	    }
	}
	/*
	ans.size() + " expanded predicate rules generated" );
	System.out.println( (System.currentTimeMillis() - start) + " milliseconds" );
	System.out.println( "EXIT: instantiatePredicateMetaRules()" );
	System.out.println();
	*/
	System.out.println();
	return ans;
    }

    /** ***************************************************************
     * This method retrieves formulas by asking the query expression
     * queryLit, and returns the results, if any, in an ArrayList.
     *
     * @param queryLit The query, which is assumed to be a single
     * predicate and its arguments.  The arguments could be variables,
     * constants, or a mix of the two, but only the first constant
     * encountered will be used in the actual query.
     *
     * @return An ArrayList of Formula objects, or null if no answers
     * are retrieved.
     */
    private ArrayList collectPredVarLiterals(List queryLit) {
	ArrayList ans = null;
	if ( queryLit != null ) {
	    ans = new ArrayList();
	    String pred = (String) queryLit.get( 0 );
	    String constant = null;
	    int cidx = -1;
	    int qlLen = queryLit.size();
	    String term = null;
	    for ( int i = 1 ; i < qlLen ; i++ ) {
		term = (String) queryLit.get( i );
		if ( isNonEmptyString(term)
		     && !(isVariable(term)) ) {
		    constant = term;
		    cidx = i;
		    break;
		}
	    }
	    if ( constant != null ) {
		ans = askWithRestriction( cidx, constant, 0, pred );
	    }
	    else {
		ans = ask( "arg", 0, pred );
	    }
	}
	return ans;
    }

    /** ***************************************************************
     * This method returns a list of all formulas in the KB that have
     * predicate variables.  Each item in the returned list is an
     * ArrayList of the pattern [ tried, simplified-formula,
     * [query-lits], original-formula, [fallback-query-lits],
     * [row-vars] ].  tried == "yes" or "no", and indicates whether or
     * not we have already tried to find substitution bindings for
     * simplified-formula using query-lits.  If the value for tried is
     * "yes", then subsequent attempts to process the formula during
     * the same invocation of instantiatePredicateMetaRules() will use
     * original-formula and fallback-query-lits.  row-vars is just a
     * list of the row variables that occur in formula, and is
     * currently ignored.
     *
     * @return An ArrayList of the formulas in this KB that have have
     * predicate variables.  
     */
    private ArrayList collectPredicateMetaRules() {
	// System.out.println( "ENTER collectPredicateMetaRules()" );
	ArrayList ans = new ArrayList();
	TreeSet formulaSet = getFormulas();
	ArrayList formulaList = null;
	if ( formulaSet instanceof TreeSet ) {
	    formulaList = new ArrayList();
	    formulaList.addAll( formulaSet );
	}
	if ( formulaList != null ) {
	    
	    Formula f   = null;
	    String fStr = null;
	    ArrayList vars = new ArrayList();
	    int fLen = formulaList.size();
	    for ( int fi = 0 ; fi < fLen ; fi++ ) {
		f = (Formula) formulaList.get( fi );
		fStr = f.theFormula;
	       
		// First we do a string existence check to see if it
		// is worth calling gatherPredicateVariables.
		if ( isNonEmptyString(fStr) 
		     && ( (fStr.indexOf("holds") >= 0)
			  || (fStr.indexOf("(?") >= 0)
			  || (fStr.indexOf("@ROW") >= 0)) ) {

		    if ( f.car().equals("<=>") ) {
			Formula newF = new Formula();
			String fCadr = f.cadr();
			String fCaddr = f.caddr();
			String theNewFormula = ( "(=> "
						 + fCadr
						 + " "
						 + fCaddr
						 + ")" );
			// System.out.println( theNewFormula );
			newF.read( theNewFormula );
			formulaList.add( newF );
			newF = new Formula();
			theNewFormula = ( "(=> "
					  + fCaddr
					  + " "
					  + fCadr
					  + ")" );
			// System.out.println( theNewFormula );
			newF.read( theNewFormula );
			formulaList.add( newF );
			fLen = formulaList.size();
		    }
		    else {
		    
			gatherPredicateVariables( f, vars );

			int vSize = vars.size();
			if ( vSize > 0 ) {

			    // Try to simplify the formula.
			    String var = null;
			    Formula fToSimplify = f;
			    ArrayList rowVars = new ArrayList();
			    ArrayList predQueryLits = new ArrayList();
			    ArrayList simplified = null;
			    for ( int i = 0 ; i < vSize ; i++ ) {
				var = (String) vars.get( i );
				if ( var.startsWith("@") ) {
				    rowVars.add( var );
				}
				else {
				    simplified = simplifyPredVarRule( fToSimplify, var );
				    if ( simplified != null ) {
					fToSimplify = (Formula) simplified.get( 0 );
					predQueryLits.add ( (ArrayList) simplified.get( 1 ) );
				    }
				}
			    }
			    ArrayList backupQueryLits = new ArrayList();
			    for ( int i = 0 ; i < vSize ; i++ ) {
				var = (String) vars.get( i );
				if ( !(var.startsWith("@")) ) {
				    backupQueryLits.add( var );
				    if ( !(isVarInPredQuery(var, predQueryLits)) ) {
					String litStr = ( "(instance " + var + " Predicate)" );
					predQueryLits.add( literalToArrayList(litStr) );
				    }
				}
			    }
			    int bqLen = backupQueryLits.size();
			    for ( int i = 0 ; i < bqLen ; i++ ) {
				String bqVar = (String) backupQueryLits.remove( 0 );
				String bqLitStr = ( "(instance " + var + " Predicate)" );
				backupQueryLits.add( literalToArrayList(bqLitStr) );
			    }

			    // Collect any missing row vars, if possible.
			    rowVars = getMatches( fStr, "row_var", rowVars );

			    vars.clear();

			    vars.add( "no" );

			    vars.add( fToSimplify );

			    vars.add( predQueryLits );

			    vars.add( f );

			    vars.add( backupQueryLits );

			    if ( rowVars.size() > 0 ) {
				vars.add( rowVars );
			    }

			    // Add the formula and variables to the answer
			    // list.
			    ans.add( vars );

			    // Create a new vars ArrayList only if we have
			    // actually used the old one.
			    vars = new ArrayList();
		    
			}
		    }
		}
	    }
	}  
	// System.out.println( "EXIT collectPredicateMetaRules() => " + ans );
	return ans;
    }

    /** ***************************************************************
     * This method checks for the occurrence of a variable in a List
     * of literals, wherein each literal is a List.
     *
     * @param var The String that is being sought.
     *
     * @param litArrList A List of literal Lists.
     *
     * @return true if var is found, else false.
     */
    private boolean isVarInPredQuery(String var, List litArrList) {
	boolean ans = false;
	if ( isNonEmptyString(var) && (litArrList instanceof List) ) {
	    int lalLen = litArrList.size();
	    List elem = null;
	    for ( int i = 0 ; i < lalLen ; i++ ) {
		elem = (List) litArrList.get( i );
		ans = ( (elem instanceof List) && elem.contains(var) );
		if ( ans ) { break; }
	    }
	}
	return ans;
    }

    /** ***************************************************************
     * This recursive method gathers the predicate variables from one
     * Formula f and adds them to the List ans, which is assumed to be
     * an empty List at the top-level call.
     *
     * @param f An instance of Formula.
     *
     * @param ans An instance of List that is used to accumulate the
     * predicate variables found in the Formula f.
     *
     * @return void
     */
    private void gatherPredicateVariables(Formula f, List ans) {
	// System.out.println( "ENTER gatherPredicateVariables( " + f + ", " + ans + " )" );
	if ( (f != null) && f.listP() ) {
	    String head = f.car();
	    if ( isNonEmptyString(head) ) {
		Formula headF = new Formula();
		headF.read( head );
		if ( headF.listP() ) {
		    gatherPredicateVariables( headF, ans );
		    Formula tailF = f.cdrAsFormula();
		    if ( tailF != null ) {
			gatherPredicateVariables( tailF, ans );
		    }
		}
		else {
		    Formula nextF = new Formula();
		    if ( isVariable(head) 
			 &&
			 !(ans.contains(head)) ) {
			ans.add( head );
		    }
		    else if ( head.equals("holds") ) {
			String var = f.cadr();
			if ( isVariable(var) 
			     && !(ans.contains(var)) ) {
			    ans.add( var );
			}
		    }
		    else if ( head.equals("not") ) {
			nextF.read( f.cadr() );
			gatherPredicateVariables( nextF, ans );
		    }
		    else if ( head.equals("holdsDuring") 
			      || head.equals("KappaFn")
			      || isQuantifier(head) ) {
			String third = f.caddr();
			if ( isNonEmptyString(third) ) {
			    nextF.read( third );
			    gatherPredicateVariables( nextF, ans );
			}
		    }
		    else if ( isCommutative(head) ) {
			nextF.read( f.cdr() );
			gatherPredicateVariables( nextF, ans );
		    }
		    else if ( head.equals("=>") || head.equals("<=>") ) {
			nextF.read( f.cadr() );
			gatherPredicateVariables( nextF, ans );
			Formula lastF = new Formula();
			lastF.read( f.caddr() );
			gatherPredicateVariables( lastF, ans );
		    }
		}
	    }
	}
	return;
    }

    /** ***************************************************************
     * This method tries to simplify an input Formula by finding and
     * removing literals that contain the input variable and can be
     * turned into queries yielding bindings for the variable.
     *
     * @param oldF A Formula to simplify.
     *
     * @param var A String denoting a variable that occurs somewhere
     * in the input Formula.
     *
     * @return An ArrayList containing (0) a simplified version of the
     * input Formula and (1) a literal containing the input variable,
     * or null if no simplification was possible.
     */
    private ArrayList simplifyPredVarRule(Formula oldF, String var) {
	// System.out.println();
	// System.out.println( "ENTER simplifyPredVarRule( " + oldF + ", " + var + " )" );
	ArrayList ans = null;
	Formula newF = null;
	if ( (oldF != null) && oldF.listP() ) {
	    String head = oldF.car();
	    if ( isNonEmptyString(head) ) {
		if ( head.equals("=>") ) {
		    Formula fCdr = oldF.cdrAsFormula();
		    if ( (fCdr != null) && fCdr.listP() ) {
			String head2 = fCdr.car();
			if ( isNonEmptyString(head2) ) {
			    Formula fCadr = new Formula();
			    fCadr.read( head2 );
			    if ( fCadr.listP() ) {
				String head3 = fCadr.car();
				if ( isQuantifier(head3)
				     || head3.equals("not") ) {
				    ; // do nothing.
				}
				else if ( isCommutative(head3) ) {
				    ArrayList conjuncts = new ArrayList();
				    Formula cdrF = fCadr.cdrAsFormula();
				    String lit = null;
				    String litFCar = null;
				    String litFCadr = null;
				    String litFCaddr = null;
				    Formula litF = null;
				    boolean addToConjuncts = true;
				    while ( (cdrF != null)
					    && !(isEmptyList(cdrF.theFormula)) ) {
					// System.out.println( "cdrF == " + cdrF );
					lit = cdrF.car();
					if ( isNonEmptyString(lit) ) {
					    litF = new Formula();
					    litF.read( lit );
					    if ( litF.listP() ) {
						litFCar = litF.car();
						litFCadr = litF.cadr();
						if ( (lit.indexOf(var) >= 0)
						     && !(litFCar.equals("not"))
						     && !(litFCar.equals("holdsDuring"))
						     && !(litFCar.equals("holds"))
						     && !(litFCar.startsWith("?")) ) {
						    ArrayList litArr = this.getMatches( lit, "open_lit" );
						    if ( litArr != null ) {
							litArr.remove (0 );
							litArr.add( literalToArrayList( litF ) );
							if ( ans == null ) { 
							    ans = litArr; 
							}
							else {
							    ans.addAll( litArr );
							}
							addToConjuncts = false;
						    }
						}
					    }
					    if ( addToConjuncts ) {
						conjuncts.add( litF );
					    }
					}
					addToConjuncts = true;
					cdrF = cdrF.cdrAsFormula();
				    }
				    if ( ans != null ) {
					String newRuleStr = null;
					int cLen = conjuncts.size();
					if ( cLen > 1 ) {
					    newRuleStr = ("(" + head3);
					    for ( int i = 0 ; i < cLen ; i++ ) {
						litF = (Formula) conjuncts.get( i );
						lit = litF.theFormula;
						newRuleStr += (" " + lit);
					    }
					    newRuleStr += ")";
					}
					else if ( cLen > 0 ) {
					    litF = (Formula) conjuncts.get(0);
					    newRuleStr = litF.theFormula;
					}
					if ( isNonEmptyString(newRuleStr) ) {
					    newRuleStr = ( "(=> "
							   + newRuleStr
							   + " "
							   + oldF.caddr() 
							   + ")" );
					}
					else {
					    newRuleStr = oldF.caddr();
					}
					newF = new Formula();
					newF.read( newRuleStr );
				    }
				}
				else if ( (fCadr.theFormula.indexOf(var) >= 0)
					  && !(fCadr.car().equals("not"))
					  && !(fCadr.car().equals("holdsDuring"))
					  && !(fCadr.car().equals("holds"))
					  && !(fCadr.car().startsWith("?")) ) {
				    ArrayList litArr = this.getMatches( fCadr.theFormula, "open_lit" );
				    if ( litArr != null ) {
					litArr.remove( 0 );
					litArr.add( literalToArrayList( fCadr ) );
					if ( ans == null ) {
					    ans = litArr;
					}
					else {
					    ans.addAll( litArr );
					}
					String third = oldF.caddr();
					newF = new Formula();
					newF.read( third );
				    }
				}
			    }
			}
		    }
		}
	    }
	}
	if ( (ans != null) && (newF != null) ) {
	    /*
	    if ( newF.car().equals("forall") ) {
		newF.read( newF.caddr() );
	    }
	    */
	    ans.add( 0, newF );
	}
	// System.out.println( "EXIT simplifyPredVarRule(...) => " + ans );
	// System.out.println();
	return ans;
    }

    /** ***************************************************************
     *
     * @return an ArrayList containing all predicates in this KB.
     *
     */
    private ArrayList collectPredicates() {
        ArrayList ans = new ArrayList();
	if ( terms.size() > 0 ) {
	    Iterator it = terms.iterator();
	    while ( it.hasNext() ) {
		String term = (String) it.next();
		if ( childOf(term, "Predicate")
		     && !(Character.isUpperCase(term.charAt(0))) ) {            
		    ans.add( term );
		}
	    }
	}
	// System.out.println( "Found " + ans.size() + " predicates" );
        return ans;
    }

    /** ***************************************************************
     *
     * @param f A Formula, which is assumed to be a single literal (a
     * single predicate applied to its arguments, which could be
     * variables).
     *
     * @return an ArrayList representation of an ordered tuple, in
     * which each term in the input formula occupied one cell in the
     * array.
     *
     */
    private ArrayList literalToArrayList(Formula f) {
	ArrayList ans = null;
	if ( f instanceof Formula ) {
	    Formula newF = f;
	    String fCar = f.car();
	    while ( isNonEmptyString(fCar) && !(isEmptyList(fCar)) ) {
		if ( ans == null ) { ans = new ArrayList(); }
		ans.add( fCar );
		newF = newF.cdrAsFormula();
		if ( newF != null ) {
		    fCar = newF.car();
		}
	    }
	}
	return ans;
    }

    /** ***************************************************************
     *
     * @param formulaString The string translation of a Formula (aka
     * "theFormula").  The Formula is assumed to be a single literal
     * (a single predicate applied to its arguments, which could be
     * variables).
     *
     * @return an ArrayList representation of an ordered tuple, in
     * which each term in the input formula occupied one cell in the
     * array.
     *
     */
    private ArrayList literalToArrayList(String formulaString) {
	if ( isNonEmptyString(formulaString) ) {
	    Formula f = new Formula();
	    f.read( formulaString );
	    return literalToArrayList( f );
	}
	return null;
    }

    /** ***************************************************************
     *
     * @param obj Any object
     *
     * @return true if obj is a non-empty String, else false.
     *
     */
    public static boolean isNonEmptyString(Object obj) {
	return ( (obj instanceof String) && (((String) obj).length() > 0) );
    }

    /** ***************************************************************
     *
     * @param obj Any object
     *
     * @return true if obj is a String representation of a LISP empty
     * list, else false.
     *
     */
    public static boolean isEmptyList(Object obj) {
	return ( isNonEmptyString(obj) && Formula.empty((String)obj) );
    }

    /** ***************************************************************
     *
     * A utility method.
     *
     * @param objList A list of anything.
     *
     * @param label An optional label (String), or null.
     *
     * @return void
     *
     */
    public static void printAll(List objList, String label) {
	if ( objList instanceof List ) {
	    Iterator it = objList.iterator();
	    while ( it.hasNext() ) {
		if ( isNonEmptyString(label) ) {
		    System.out.println( label + ": " + it.next() );
		}
		else {
		    System.out.println( it.next() );
		}
	    }
	}
	return;
    }

    /** ***************************************************************
     *
     * A static utility method.
     *
     * @param obj Presumably, a String.
     *
     * @return true if obj is a SUO-KIF variable, else false.
     *
     */
    public static boolean isVariable(String obj) {
	if ( isNonEmptyString(obj) ) {
	    return ( obj.startsWith("?") || obj.startsWith("@") );
	}
	return false;
    }

    /** ***************************************************************
     *
     * A static utility method.
     *
     * @param obj Presumably, a String.
     *
     * @return true if obj is a SUO-KIF logical quantifier, else
     * false.
     *
     */
    public static boolean isQuantifier(String obj) {
	if ( isNonEmptyString(obj) ) {
	    return ( obj.equals("forall") || obj.equals("exists") );
	}
	return false;
    }

    /** ***************************************************************
     *
     * A static utility method.
     *
     * @param obj Presumably, a String.
     *
     * @return true if obj is a SUO-KIF commutative logical operator,
     * else false.
     *
     */
    public static boolean isCommutative(String obj) {
	if ( isNonEmptyString(obj) ) {
	    return ( obj.equals("and") || obj.equals("or") );
	}
	return false;
    }

    /** *************************************************************
     * Hyperlink terms identified with '&%' to the URL that brings up
     * that term in the browser.  Handle (and ignore) suffixes on the term.
     * For example "&%Processes" would get properly linked to the term "Process",
     * if present in the knowledge base.
     */
    public String formatDocumentation(String href, String documentation) {

        int i;
        int j;
        String term = "";
        String newFormula = documentation;

        while (newFormula.indexOf("&%") != -1) {
            i = newFormula.indexOf("&%");
            j = i + 2;
            while (Character.isJavaIdentifierPart(newFormula.charAt(j)) && j < newFormula.length()) 
                j++;
            //System.out.println("Candidate term: " + newFormula.substring(i+2,j));

            while (!containsTerm(newFormula.substring(i+2,j)) && j > i + 2) 
                j--;
            term = newFormula.substring(i+2,j);
            if (term != "" && containsTerm(newFormula.substring(i+2,j))) {
                newFormula = newFormula.substring(0,i) +
                    "<a href=\"" + href + "&term=" + term + "\">" + term + "</a>" +
                    newFormula.substring(j,newFormula.toString().length());
            }
            else
                newFormula = newFormula.substring(0,i) + newFormula.substring(j,newFormula.toString().length());
        }
        return newFormula;
    }
    
    /** *************************************************************
     *  Pull all the formulas into one TreeSet of Strings.
     */
    private TreeSet collectAllFormulas(HashMap forms) {

        TreeSet ts = new TreeSet();
        ArrayList al = new ArrayList(forms.values());

        for (int i = 0; i < al.size(); i++) {
            ArrayList al2 = (ArrayList) al.get(i);
            for (int j = 0; j < al2.size(); j++) 
                ts.add(((Formula) al2.get(j)).theFormula);
        }
        return ts;
    }
    
    /** *************************************************************
     *  Pull all the formulas in an ArrayList into one TreeSet of Strings.
     */
    private TreeSet collectFormulasFromList(ArrayList forms) {

        TreeSet ts = new TreeSet();
        for (int j = 0; j < forms.size(); j++) 
            ts.add(((Formula) forms.get(j)).theFormula);
        return ts;
    }

    /** *************************************************************
     * Save the contents of the current KB to a file.
     */
    public String writeInferenceEngineFormulas(TreeSet forms) throws IOException {

        String inferenceEngine = KBmanager.getMgr().getPref("inferenceEngine");
	System.out.println( "file separator == " + File.separator );
        String inferenceEngineDir = inferenceEngine.substring(0,inferenceEngine.lastIndexOf(File.separator));
	System.out.println( "inferenceEngineDir == " + inferenceEngineDir );
        String filename = inferenceEngineDir + File.separator + this.name + "-v.kif";
	System.out.println( "filename == " + filename );
        FileWriter fr = null;
        PrintWriter pr = null;

        try {
            fr = new FileWriter(filename);
            pr = new PrintWriter(fr);
            Iterator it = forms.iterator();
            while (it.hasNext())
                pr.println((String) it.next() + "\n");                       
        }
        catch (java.io.IOException e) {
            System.out.println("Error in KB.writeInferenceEngineFormulas(): Error writing file " + filename);
        }
        finally {
            if (pr != null) {
                pr.close();
            }
            if (fr != null) {
                fr.close();
            }
        }
        return this.name + "-v.kif";
    }

    /** *************************************************************
     *  Start Vampire and collect, preprocess and load all the 
     *  constituents into it.
     */
    private void loadVampire() {

        System.out.println("INFO in KB.loadVampire()");
        if (formulas.values().size() != 0) {
            TreeSet forms = collectAllFormulas(formulas);
            forms = preProcess(forms);
            try {
                String filename = writeInferenceEngineFormulas(forms);
                System.out.println("INFO in KB.loadVampire(): writing formulas to " + filename);
                inferenceEngine = new Vampire(filename);
            }
            catch (IOException ioe) {
                System.out.println("Error in KB.loadVampire(): " + ioe.getMessage());
            }
        }
        else {
            try {
                inferenceEngine = new Vampire(inferenceEngine.EMPTY_FILE);
            }
            catch (IOException ioe) {
                System.out.println("Error in KB.loadVampire(): " + ioe.getMessage());
            }
        }
    }

    /** ***************************************************************
     * Preprocess a the knowledge base to work with Vampire.  This includes "holds"
     * prefixing, ticking nested formulas, expanding row variables, and
     * translating mathematical relation operators.
     * @return a TreeSet of Strings. 
     */
    public TreeSet preProcess(TreeSet forms) {

        TreeSet newTreeSet = new TreeSet();
        Formula newFormula = null;
        ArrayList processed = null;         // An ArrayList of Formula(s).  
                                            // If the Formula which is to be preprocessed does not contain row
                                            // variables, then this list will have only one element.
        Iterator it = formulaSet.iterator();
        while (it.hasNext()) {
            newFormula = new Formula();
            String form = (String) it.next();
            ArrayList al = (ArrayList) formulas.get(form);
            Formula f = (Formula) al.get(0);
            newFormula.theFormula = new String(f.theFormula);
            processed = newFormula.preProcess(false,this);   // not queries
            if (KBmanager.getMgr().getPref("TPTP").equals("yes")) {  
                try {
                    f.tptpParse(false,this);   // not a query
                }
                catch (ParseException pe) {
                    String er = "Error in KB.preProcess(): " + pe.getMessage() + " for formula is file " + f.sourceFile + " at line " +  f.startLine;
                    KBmanager.getMgr().setError(KBmanager.getMgr().getError() + "<P>\n" + er);
                    System.out.println(er);
                }
                catch (IOException ioe) {
                    System.out.println("Error in KB.preProcess: " + ioe.getMessage());
                }

            }

            Iterator itp = processed.iterator();
            while (itp.hasNext()) {
                Object next = itp.next();
                //System.out.println("INFO in KB.preProcess(): " + next);
                Formula p = (Formula) next;
                if (p.theFormula != null) 
                    newTreeSet.add(p.theFormula);
            }
        }
        return newTreeSet;
    }

    /** *************************************************************
     */
    private void writePrologFormulas(ArrayList forms, PrintWriter pr) throws IOException {

        TreeSet ts = new TreeSet();
        ts.addAll(forms);
        if (forms != null) {
            int i = 0;
            Iterator it =  ts.iterator();
            while (it.hasNext()) {
                Formula formula = (Formula) it.next();
                String result = formula.toProlog();
                if (result != null && result != "") 
                    pr.println(result);
                if (i % 100 == 1) System.out.print(".");                                                            
            }
        }        
    }

    /** *************************************************************
     */
    public void writePrologFile(String fname) throws IOException {

        FileWriter fr = null;
        PrintWriter pr = null;

        System.out.println("INFO in KB.writePrologFile()");
        try {
            fr = new FileWriter(fname);
            pr = new PrintWriter(fr);
            pr.println("% Copyright © 2006 Articulate Software Incorporated");
            pr.println("% This software released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.");
            pr.println("% This is a very lossy translation to prolog of the KIF ontologies available at www.ontologyportal.org\n");

            pr.println("% subAttribute");
            writePrologFormulas(ask("arg",0,"subAttribute"),pr);
            pr.println("\n% subrelation");
            writePrologFormulas(ask("arg",0,"subrelation"),pr);
            pr.println("\n% disjoint");
            writePrologFormulas(ask("arg",0,"disjoint"),pr);
            pr.println("\n% partition");
            writePrologFormulas(ask("arg",0,"partition"),pr);
            pr.println("\n% instance");
            writePrologFormulas(ask("arg",0,"instance"),pr);
            pr.println("\n% subclass");
            writePrologFormulas(ask("arg",0,"subclass"),pr);            
            System.out.println(" ");
        }
        catch (java.io.IOException e) {
            throw new IOException("Error writing prolog file. " + e.getMessage());
        }
        finally {
            if (pr != null) {
                pr.close();
            }
            if (fr != null) {
                fr.close();
            }
        }
    }
    /** *************************************************************
     */
    public String writeTPTPFile(String fileName,Formula conjecture, boolean
                                onlyPlainFOL, String reasoner) throws IOException {

        String sanitizedKBName;
        File outputFile;
        PrintWriter pr = null;
        int axiomIndex = 1;
        TreeSet orderedFormulae;
        String theTPTPFormula;
        boolean sanitizedFormula;
        boolean commentedFormula;

        System.out.println("INFO in KB.writeTPTPFile()");
        sanitizedKBName = name.replaceAll("\\W","_");
        try {
            //----If file name is a directory, create filename therein
            if (fileName == null) {
                outputFile = File.createTempFile(sanitizedKBName, ".p",null);
                //----Delete temp file when program exits.
                outputFile.deleteOnExit();
            } else {
                outputFile = new File(fileName);
            }
            fileName = outputFile.getAbsolutePath();
            System.out.println("INFO writeTPTPFile to " + fileName);

            pr = new PrintWriter(new FileWriter(outputFile));
            pr.println("% Copyright 2007 Articulate Software Incorporated");
            pr.println("% This software released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.");
            pr.println("% This is a translation to TPTP of KB " + 
                       sanitizedKBName + "\n");

            orderedFormulae = new TreeSet(new Comparator() {
                public int compare(Object o1, Object o2) {
                    Formula f1 = (Formula) o1;
                    Formula f2 = (Formula) o2;
                    int fileCompare = f1.sourceFile.compareTo(f2.sourceFile);
                    if (fileCompare == 0) {
                        return((new Integer(f1.startLine)).compareTo(
                            new Integer(f2.startLine)));
                    } else {
                        return(fileCompare);
                    }
                } });
            Iterator ite = formulas.values().iterator();
            while (ite.hasNext()) {
                orderedFormulae.addAll((ArrayList) ite.next());
            }

            ite = orderedFormulae.iterator();
            while (ite.hasNext()) {
                Formula f = (Formula) ite.next();
                theTPTPFormula = f.theTPTPFormula;
                commentedFormula = false;
                if (onlyPlainFOL) {
                    //----Remove interpretations of arithmetic
                    theTPTPFormula = theTPTPFormula.
                        replaceAll("[$]less","dollar_less").replaceAll("[$]greater","dollar_greater").
                        replaceAll("[$]time","dollar_times").replaceAll("[$]divide","dollar_divide").
                        replaceAll("[$]plus","dollar_plus").replaceAll("[$]minus","dollar_minus");
                    //----Don't output ""ed ''ed and numbers
                    if (theTPTPFormula.indexOf('\'') >= 0 ||
                        theTPTPFormula.indexOf('"') >= 0 || 
                        theTPTPFormula.matches(".*[(,]-?[0-9].*")) {
                        pr.print("%FOL ");
                        commentedFormula = true;
                    }
                    if (reasoner.equals("Equinox---1.0b") && f.theFormula.indexOf("equal") > 2) {
                        Formula f2 = new Formula();
                        f2.read(f.cdr());
                        f2.read(f.car());
                        if (f2.theFormula.equals("equals")) {
                            pr.print("%FOL ");
                            commentedFormula = true;
                        }
                    }

                }
                pr.println("fof(kb_" + sanitizedKBName + "_" + axiomIndex++ +
                           ",axiom,(" + theTPTPFormula + ")).");
                if (commentedFormula) {
                    pr.println();
                }
            }
             //----Print conjecture if one has been supplied
            if (conjecture != null) {
                pr.println("fof(prove_from_" + sanitizedKBName + 
                           ",conjecture,(" + conjecture.theTPTPFormula + ")).");
            }
        }
        catch (java.io.IOException e) {
            throw new IOException("Error writing TPTP file. " + e.getMessage());
        }
        finally {
            if (pr != null) {
                pr.close();
            }
        }
        return(fileName);
    }

    /** *************************************************************
     * A test method.
     */
    public static void main (String args[]) {


	KBmanager mgr = KBmanager.getMgr();
	if ( mgr != null ) {
	    try {
		mgr.initializeOnce();
	    }
	    catch ( Exception ex ) {
		ex.printStackTrace();
	    }
	}

        KB kb = new KB("SUMO-TEST","c:\\nsiegel\\articulate\\work\\inference");
        try {
            kb.addConstituent("c:\\nsiegel\\articulate\\work\\KBs\\Merge.kif");
            // kb.addConstituent("c:\\nsiegel\\articulate\\work\\KBs\\Mid-level-ontology.kif");
        }
        catch (Exception e) {
	    e.printStackTrace();
        }
        
        System.out.println();
	kb.cache();

	// kb.instantiatePredicateMetaRules();

	ArrayList al = instantiatePredicateMetaRules( kb );

	if ( al != null ) {
	    for ( int i = 0 ; i < al.size() ; i++ ) {
		if ( (i > 0) &&  ((i % 50) == 0) ) {
		    System.out.println();
		    System.out.println( al.get( i ) );
		    System.out.println();
		}
	    }
	}
    }
}
