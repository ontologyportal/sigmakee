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
    /** A HashMap of ArrayLists, which contain all the parent classes of a given class. */
    public HashMap parents = new HashMap();
    /** A HashMap of ArrayLists, which contain all the child classes of a given class. */
    public HashMap children = new HashMap();
    /** A HashMap of ArrayLists, which contain all the disjoint classes of a given class. */
    public HashMap disjoint = null;
    /** The instance of the CELT process. */
    public CELT celt = null;
    /** A Set of Strings, which are all the terms in the KB. */
    public TreeSet terms = new TreeSet(); 

    private String _userAssertionsString = "_UserAssertions.kif";
    private HashSet formulaSet = new HashSet(); // A Set of all the formula Strings in the KB.
    private HashMap formulas = new HashMap();   // A HashMap of ArrayLists of Formulas, containing all the formulas in the KB
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
            if (KBmanager.getMgr().getPref("loadCELT").equalsIgnoreCase("yes")) 
                celt = new CELT();
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
                    ArrayList newParents = (ArrayList) parents.get(parent);
                    if (newParents != null) {
                        for (int j = 0; j < newParents.size(); j++) {
                            String newParent = (String) newParents.get(j);
                            if (newParent.indexOf("(") == -1) {
                                String newFormula = "(" + statementType + " " + child + " " + newParent + ")";
                                if (!formulaStrings.contains(newFormula.intern()))
                                    cached.add(newFormula);                
                            }
                        }
                        System.out.print(".");
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
            ArrayList existingParents = (ArrayList) parents.get(child);
            if (!existingParents.contains(parent))
                existingParents.add(parent);
        }
        else {
            ArrayList parentList = new ArrayList();
            parentList.add(parent);
            parents.put(child, parentList);
        }

        if (children.containsKey(parent)) {
            ArrayList existingChildren = (ArrayList) children.get(parent);
            if (!existingChildren.contains(child))
                existingChildren.add(child);
        }
        else {
            ArrayList childList = new ArrayList();
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

        boolean changed = true;
        while (changed) {                                   
            changed = false;
            Iterator it = list.keySet().iterator();                       
            while (it.hasNext()) {
                String term = (String) it.next();                 
                ArrayList targets = (ArrayList) list.get(term);
                if (targets != null) {
                    for (int i = 0; i < targets.size(); i++) {
                        String targetTerm = (String) targets.get(i);
                        ArrayList newTargets = (ArrayList) list.get(targetTerm);
                        if (newTargets != null) {
                            for (int j = 0; j < newTargets.size(); j++) {
                                String newTarget = ((String) newTargets.get(j)).intern();
                                if (!targets.contains(newTarget)) {
                                    targets.add(newTarget);
                                    list.put(term,targets);
                                    changed = true;
                                }
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
     * debugging utility
     */
    private void printParents() {

        System.out.println("INFO in printParents():  Printing parents.");
        System.out.println();
        Iterator it = parents.keySet().iterator();
        while (it.hasNext()) {
            String parent = (String) it.next();
            System.out.print(parent + " ");
            System.out.println((ArrayList) parents.get(parent));
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
            System.out.println((ArrayList) children.get(child));
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

        System.out.println("INFO in KB.collectParentsAndChildren(): Caching class hierarchy.");
        Iterator it = terms.iterator();                       
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
                            formula.theFormula.substring(1,9).equalsIgnoreCase("subclass")) { 
                            String parent = formula.theFormula.substring(formula.theFormula.indexOf(" ",10)+1,
                                                                         formula.theFormula.indexOf(")",10));
                            String child = formula.theFormula.substring(10,formula.theFormula.indexOf(" ",10));
                            addParentChildEntry(parent.intern(),child.intern());
                        }
                    }
                    if (i % 100 == 1) System.out.print(".");
                }
            }
        }

        System.out.print("INFO in KB.collectParentsAndChildren(): Calculating transitive closure.");
        calculateTransitiveClosure(parents);
        calculateTransitiveClosure(children);
        //printParents();
        //printChildren();
    }

    /** *************************************************************
     * Add entries to the list of disjoint classes.  Called only by
     * collectDisjointness().
     */
    private void addDisjointnessEntry(String term1, String term2) {

        if (disjoint.containsKey(term1.intern())) {
            ArrayList existingDisjoint = (ArrayList) disjoint.get(term1.intern());
            if (!existingDisjoint.contains(term2.intern()))
                existingDisjoint.add(term2.intern());
            disjoint.put(term1.intern(), existingDisjoint);
        }
        else {
            ArrayList disjointList = new ArrayList();
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

        Iterator it = terms.iterator();                       
        while (it.hasNext()) {
            String term = (String) it.next();                 // Collect the immediate disjoints for each term.
            ArrayList forms = ask("arg",1,term);
            if (forms != null && forms.size() > 0) { 
                for (int i = 0; i < forms.size(); i++) {
                    Formula formula = (Formula) forms.get(i);
                    if (formula.theFormula.indexOf("(",2) == -1 &&
                        !formula.sourceFile.substring(formula.sourceFile.length()-11,formula.sourceFile.length()).equalsIgnoreCase("_Cache.kif")) {    // Ignore cases where parent class is a function
                        if (formula.theFormula.substring(1,9).equalsIgnoreCase("disjoint")) { 
                            String disjointStr = formula.theFormula.substring(formula.theFormula.indexOf(" ",10)+1,
                                                                              formula.theFormula.indexOf(")",10));
                            addDisjointnessEntry(term,disjointStr);
                            addDisjointnessEntry(disjointStr,term);
                        }
                    }
                    if (i % 100 == 1) System.out.print(".");
                }
            }
        }
    }

    /** *************************************************************
     * Collect disjointness from all statements with the relations
     * "partition" and "disjointDecomposition" and store in the
     * "disjoint" HashMap.
     */
    private void collectPartitions() {

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
                if (i % 100 == 1) System.out.print(".");
            }
        }
    }

    /** *************************************************************
     * Augment the disjoint HashMap by adding all the children (subclasses
     * and instances) of each target class.  For example, if A and B
     * are disjoint and C is a subclass of B, A is also disjoint with C.
     */
    private void addDisjointChildren() {

        Iterator it = disjoint.keySet().iterator();      
        while (it.hasNext()) {
            String term = (String) it.next();                 
            ArrayList dis = (ArrayList) disjoint.get(term);
            if (dis != null) {
                ArrayList newDises = new ArrayList();
                newDises.addAll(dis);
                for (int i = 0; i < dis.size(); i++) {
                    String disTerm = (String) dis.get(i);
                    ArrayList kids = (ArrayList) children.get(disTerm);
                    if (kids != null) {
                        for (int j = 0; j < kids.size(); j++) {
                            String newDis = ((String) kids.get(j)).intern();
                            if (!newDises.contains(newDis)) {
                                newDises.add(newDis);
                            }
                        }
                    }
                    if (i % 100 == 1) System.out.print(".");
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
        
        System.out.print("INFO in KB.collectDisjointness(): Begin.");
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
     * in the indicated argument positions.
     */
    public ArrayList askWithRestriction(int argnum1, String term1, int argnum2, String term2) {
        
        ArrayList partial = ask("arg",argnum1,term1);
        ArrayList result = new ArrayList();
        if (partial != null) {
            for (int i = 0; i < partial.size(); i++) {
                Formula f = (Formula) partial.get(i);
                if (f.getArgument(argnum2).equalsIgnoreCase(term2)) {
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
        theFormulas = f.preProcess();

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
     *  Count the number of terms in the knowledge base in order to
     *  present statistics to the user.
     *
     *  @return The int(eger) number of terms in the knowledge base.
     */
    public int getCountTerms() {

        return terms.size();
    }      
    
    /** ***************************************************************
     *  Count the number of formulas in the knowledge base in order to
     *  present statistics to the user.
     *
     *  @return The int(eger) number of formulas in the knowledge base.
     */
    public int getCountAxioms() {

        TreeSet formulaSet = new TreeSet();
        Iterator ite = formulas.values().iterator();
        while (ite.hasNext()) {
            ArrayList al = (ArrayList) ite.next();
            for (int i = 0; i < al.size(); i++) {
                formulaSet.add(((Formula) al.get(i)).theFormula);
            }
        }        
        return formulaSet.size();
    } // POD would be better to just count them. 

    /** ***************************************************************
     *  an accessor providing a TreeSet of un-preProcess-ed Formula.
     *  @return A Treeset of un-preProcess(ed) formulas.
     */
    public TreeSet getFormulas() {

        TreeSet formulaSet = new TreeSet();
        Iterator ite = formulas.values().iterator();
        while (ite.hasNext()) {
            ArrayList al = (ArrayList) ite.next();
            for (int i = 0; i < al.size(); i++) {
                formulaSet.add((Formula) al.get(i));
            }
        }        
        return formulaSet;
    }      
    
    /** ***************************************************************
     *  Count the number of rules in the knowledge base in order to
     *  present statistics to the user. Note that the number of rules
     *  is a subset of the number of formulas.
     *
     *  @return The int(eger) number of rules in the knowledge base.
     */
    public int getCountRules() {

        TreeSet formulaSet = new TreeSet();
        Iterator ite = formulas.values().iterator();
        while (ite.hasNext()) {
            ArrayList al = (ArrayList) ite.next();
            for (int i = 0; i < al.size(); i++) {
                if (((Formula) al.get(i)).theFormula.substring(1,3).compareTo("=>") == 0)
                    formulaSet.add(((Formula) al.get(i)).theFormula);
            }
        }        
        return formulaSet.size();
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
    public void addConstituent (String filename) throws IOException, ParseException {

        Iterator it;
        KIF file = new KIF();
        String key;
        ArrayList list;
        ArrayList newList;

        if (constituents.contains(filename.intern())) return;
        System.out.println("INFO KB.addConstituent(): Adding " + filename);
        try { 
            file.readFile(filename);
        }
        catch (IOException ioe) {
            throw new IOException(ioe.getMessage());
        }
        catch (ParseException pe) {
            throw new ParseException(pe.getMessage(),pe.getErrorOffset());
        }
        formulaSet.addAll(file.formulaSet);
        System.out.print("Read file: " + filename + " of size: ");
        System.out.println(file.formulas.keySet().size());
        it = file.formulas.keySet().iterator();
        while (it.hasNext()) {                // Iterate through the formulas in the file, adding them to the KB, at the appropriate key.
            key = (String) it.next();         // Note that this is a slow operation that needs to be improved
            if (formulas.containsKey(key)) {
                list = (ArrayList) formulas.get(key);
                if (list == null) 
                    throw new ParseException("Error: Bad data in existing constituents at key: " + key,0); 
                newList = (ArrayList) file.formulas.get(key);
                for (int i = 0; i < newList.size(); i++) {          // Don't add formulas to the KB that already exist in the same file.
                    Formula f = (Formula) newList.get(i);           // This inner loop is the slow part
                    boolean found = false;
                    for (int j = 0; j < list.size(); j++) {         
                        if (f.deepEquals((Formula) list.get(j))) 
                            found = true;
                    }
                    if (!found) 
                        list.add(newList.get(i));
                }
            }
            else {
                ArrayList forms = (ArrayList) file.formulas.get(key);
                formulas.put(key,forms);
            }
        }
        it = file.terms.iterator();
        while (it.hasNext()) {
            key = (String) it.next();
            this.terms.add(key);
        }
        constituents.add(filename.intern());
        System.out.print("INFO KB.addConstituent(): Number of constituents ");
        System.out.println(constituents.size());
        System.out.print("INFO KB.addConstituent(): Number of formulas ");
        System.out.println(file.formulas.values().size());
        loadVampire();
        if (filename.substring(filename.lastIndexOf(File.separator),filename.length()).compareTo("_Cache.kif") != 0) {
            collectParentsAndChildren();
            collectDisjointness();
        }
    }

    /** ***************************************************************
     * Reload all the KB constituents.
     */
    public void reload() {
        
        System.out.println("INFO in KB.reload(): Reloading.");
        ArrayList newConstituents = new ArrayList(constituents);
        constituents = new ArrayList();
        language = "en";                  
        formulas = new HashMap();       
        terms = new TreeSet();          
        formatMap = null; 

        for (int i = 0; i < newConstituents.size(); i++) {
            try {
                addConstituent((String) newConstituents.get(i));        
            }
            catch (IOException ioe) {
                System.out.println("Error in KB.reload(): " + ioe.getMessage());
            }
            catch (ParseException pe) {
                System.out.print("Error in KB.reload(): " + pe.getMessage());
                System.out.println(" at line: " + (new Integer(pe.getErrorOffset()).toString()));
            }
        }
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
     * Write the XML configuration file.
     */
    public void writeConfiguration(PrintWriter pw) {

        pw.println("<kb name=\"" + name + "\">");
        //System.out.println("<kb name=\"" + name + "\">");
        for (int i = 0; i < constituents.size(); i++) {
            String filename = (String) constituents.get(i);
            filename = KBmanager.escapeFilename(filename);
            pw.println("  <constituent filename=\"" + filename + "\"/>");
            //System.out.println("  <constituent filename=\"" + filename + "\"/>");
        }
        pw.println("</kb>");
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
        String inferenceEngineDir = inferenceEngine.substring(0,inferenceEngine.lastIndexOf(File.separator));
        String filename = inferenceEngineDir + File.separator + this.name + "-v.kif";
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
        Iterator it = forms.iterator();
        while (it.hasNext()) {
            newFormula = new Formula();
            newFormula.theFormula = (String) it.next();
            processed = newFormula.preProcess();
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
     * A test method.
     */
    public static void main(String args[]) {

        KB kb = new KB("foo","");
        try {
            kb.addConstituent("C:\\Program Files\\Apache Tomcat 4.0\\KBs\\test.txt");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        Iterator it = kb.terms.iterator();
        while (it.hasNext()) 
            System.out.println((String) it.next());

        it = kb.formulas.values().iterator();
        while (it.hasNext()) { 
            ArrayList a = (ArrayList) it.next();
            for (int i = 0; i < a.size(); i++) {
                Formula f = (Formula) a.get(i);
                System.out.println(f.theFormula);
            }
        }

        it = kb.formulas.keySet().iterator();
        while (it.hasNext())  
            System.out.println((String) it.next());            
        
        System.out.println();

        ArrayList al;
        al = (ArrayList) kb.formulas.get("stmt-John-1");

        for (int i = 0; i < al.size(); i++) {
            System.out.println(((Formula) al.get(i)).theFormula);            
            System.out.println(((Formula) al.get(i)).sourceFile);            
        }
    }
}
