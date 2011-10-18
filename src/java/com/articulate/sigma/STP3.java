

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

package com.articulate.sigma;

import java.io.*;
import java.util.*;

import com.articulate.sigma.STP2.FormulaRating;

/** ***************************************************************
 * The Sigma theorem prover. A simple resolution prover in Java.
 */
public class STP3 extends InferenceEngine {

    /** Randomly instantiate formulas in order to create proofs
     *  to use as tests. */
    boolean _PROOF_GENERATION = false;

    boolean _PROVE_DEBUG = false;
    boolean _GEN_DEBUG = false;

    /** Assigns an ID to proof steps */
    int _FORMULA_COUNTER = 0;

    /** seeded random number generator */
    Random random = new Random(1);

    /** A counter to indicate when to instantiate a formula in
     *  test creation. */
    int proofCounter = 0;

    /** A constant to indicate when to instantiate a formula in
     *  test creation. */
    static final int proofCounterInterval = 2;

     /** The knowledge base. Variables are odd numbered. */
    ArrayList<CNFFormula2> formulas = new ArrayList();

     /** Keys are CNF formula strings. */
    TreeMap<String, CNFFormula2> formulaMap = new TreeMap();

    /** These individual deduction steps, indexed by conclusion.
     *  Keys are CNF formula strings. One premise should have
     *  odd-numbered variables, the other premise, even-numbered
     *  ones. */
    TreeMap<String, ArrayList<CNFFormula2>> deductions = new TreeMap();

    /** Formulas containing these predicates will be ignored. */
    public static TreeSet<String> excludedPredicates = new TreeSet();

    /** To Be Used - Variables are even numbered. */
    ArrayList<FormulaRating> TBU = new ArrayList();

    public static int _GENSYM_COUNTER = 0;

    /** Used to give an "age" to each axiom, by incrementing a
     *  count each time a new axiom is added to the KB. */
    public static int _PSUEDO_TIME = 0;

    /** The indexes */
    TreeMap<Integer, ArrayList<CNFFormula2>> negPreds = new TreeMap();  // Formulas containing negated clauses with a given predicate
    TreeMap<Integer, ArrayList<CNFFormula2>> posPreds = new TreeMap();  // Formulas containing positive clauses with a given predicate
    TreeMap<Integer, Integer> termCounts = new TreeMap();    // Key is term index, value is the count of appearances
    //TreeMap<Integer, ArrayList<CNFFormula2>> posTermPointers = new TreeMap(); // appearance of a term in a positive literal
    //TreeMap<Integer, ArrayList<CNFFormula2>> negTermPointers = new TreeMap(); // appearance of a term in a negative literal

    /** Statistics */
    static int deductionsMade = 0;
    static int resolutionsAttempted = 0;
    static int subsumptions = 0;

    /** ***************************************************************
     * Convert to a String.
     */
    public String toString() {

        return "An STP3 instance";
    }

    /** ***************************************************************
     */
    public static class STP3EngineFactory extends EngineFactory {

        @Override
        public InferenceEngine createWithFormulas(Iterable<String> formulaSource) {  
            return new STP3(formulaSource);
        }

        @Override
        public InferenceEngine createFromKBFile(String kbFileName) {
            return STP3.getNewInstance(kbFileName);
        }
    }

    public static EngineFactory getFactory() {
        return new STP3EngineFactory();
    }

    /** *************************************************************
     */
    private STP3(String kbFileName) throws Exception {

        initExcluded();
        String error = null; 
        if (kbFileName == null) {
            error = "No file name. Empty inference engine created.";
            System.out.println(error);
        }
        long t_start = System.currentTimeMillis();

        File kbFile = null;
        if (error == null) {
            kbFile = new File(kbFileName);
            if (!kbFile.exists() ) {
                error = ("The file " + kbFileName + " does not exist");
                System.out.println("Error in STP(): " + error);
                KBmanager.getMgr().setError(KBmanager.getMgr().getError()
                                             + "\n<br/>" + error + "\n<br/>");
            }
        }
        
        if (error == null) {
            System.out.println("INFO in STP3(): Starting clausification with file: " + kbFileName);
            KB kb = new KB(kbFileName);
            kb.addConstituent(kbFileName);
            //kb.addConstituent(kbFileName);
            //KIF kif = new KIF();
            //kif.setParseMode(KIF.RELAXED_PARSE_MODE);
            //kif.readFile(kbFileName);

            //Iterator it = kif.formulas.values().iterator();
            Iterator it = kb.formulaMap.values().iterator();
            int counter = 0;
            System.out.println("INFO in STP3(): Formulas: " + kb.formulaMap.values().size());
            while (it.hasNext()) {
                Formula f = (Formula) it.next();
                ArrayList<Formula> processedForms = f.preProcess(false,kb);
                for (int j = 0; j < processedForms.size(); j++) {
                    Formula f2 = (Formula) processedForms.get(j);
                    if (f2.theFormula.indexOf('`') < 0 && !containsExcludedPredicates(f2.gatherRelationConstants())) {
                        //System.out.println("INFO in STP3(): Clausifying: " + f2);
                        ArrayList<CNFFormula2> cnfForms = clausifyFormula(f2);
                        ArrayList<CNFFormula2> simplifiedForms = new ArrayList();
                        for (int i = 0; i < cnfForms.size(); i++) {
                            CNFFormula2 cnf = (CNFFormula2) cnfForms.get(i);
                            cnf.normalizeVariables(false);
                            simplifiedForms.add(cnf.subsumedClauses());
                        }
                        formulas.addAll(simplifiedForms);
                    }
                    if (counter++ % 100 == 0) System.out.print(".");
                }               
            }
            System.out.println("\nINFO in STP3(): Done clausification in ");
            long t_elapsed = (System.currentTimeMillis() - t_start) / 1000;
            System.out.println(t_elapsed + " seconds.");            
        }
        buildIndexes();
        //System.out.println("INFO in STP(): clausified formulas: " + formulas);
    }    

    /** *************************************************************
     *  Backquoted clauses, signifying higher-order formulas, are
     *  rejected. This routine assumes that the formula strings have
     *  already been pre-processed.
     */
    public STP3(Iterable<String> formulaSource) { 
    
        initExcluded();
        Iterator it = formulaSource.iterator();
        while (it.hasNext()) {
            String s = (String) it.next();
            if (!s.contains("`")) {
                Formula f = new Formula();
                f.read(s);
                TreeSet excludedCopy = new TreeSet();
                excludedCopy.addAll(excludedPredicates);
                if (f.theFormula.indexOf('`') < 0 && !containsExcludedPredicates(f.gatherRelationConstants())) {
                    ArrayList<CNFFormula2> cnfForms = clausifyFormula(f);
                    ArrayList<CNFFormula2> simplifiedForms = new ArrayList();
                    for (int i = 0; i < cnfForms.size(); i++) {
                        CNFFormula2 cnf = (CNFFormula2) cnfForms.get(i);
                        cnf.normalizeVariables(false);
                        simplifiedForms.add(cnf.subsumedClauses());
                    }
                    formulas.addAll(simplifiedForms);
                }
            }
        }
        buildIndexes();
        System.out.println("INFO in STP3(): clausified formulas: " + formulas);
    }
    
    /** *************************************************************
     */
    private static void initExcluded() {
        excludedPredicates.add("documentation");
        excludedPredicates.add("equal");
        excludedPredicates.add("externalImage");
    }

    /** *************************************************************
     */
    private static boolean containsExcludedPredicates(HashSet preds) {

        //System.out.println("INFO in STP3().containsExcludedPredicates: checking predicates: " + preds);
        TreeSet excludedCopy = new TreeSet();
        excludedCopy.addAll(excludedPredicates);
        excludedCopy.retainAll(preds);
        //System.out.println("INFO in STP3().containsExcludedPredicates: pred sizes: " + excludedCopy.size()  + " " + excludedPredicates.size());
        if (excludedCopy.size() < 1) 
            return false;
        else
            return true;
    }

    /** *************************************************************
     */
    public static STP3 getNewInstance(String kbFileName) {

        STP3 res = null;
        try {
            res = new STP3(kbFileName);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return res;
    }

    /** *************************************************************
     *  Utility debugging method. Prints the contents of
     *  TreeMap<String, ArrayList<Formula>> deductions
     */
    private void printDeductions() {

        System.out.println("\nINFO in printDeductions(): ");
        System.out.println(deductions.keySet().size() + " deductions");
        Iterator it = deductions.keySet().iterator();
        while (it.hasNext()) {       
            String s = (String) it.next();
            Formula d = new Formula();
            d.read(s);
            System.out.println(d);
            System.out.println("support:");
            ArrayList<CNFFormula2> support = (ArrayList) deductions.get(s);
            if (support != null) {
                for (int i = 0; i < support.size(); i++) {
                    CNFFormula2 f = (CNFFormula2) support.get(i);
                    System.out.println(f);
                }
            }
            else {
                CNFFormula2 f = (CNFFormula2) formulaMap.get(s);
                if (f != null) 
                    System.out.println(f.sourceFormula);                
            }
            System.out.println("-------------------");
        }
        System.out.println();
    }

    /** *************************************************************
     */
    private ArrayList<CNFFormula2> clausifyFormula(Formula forig) {

        ArrayList<CNFFormula2> newFormulas = new ArrayList();

        Formula fold = forig;
        //System.out.println("INFO in STP3.clausifyFormula(): to clausify: " + fold);
        //addToDeductions(null,null,fold,false);
        Formula f = Clausifier.clausify(fold);
        //Formula f = fold.clausify();
        if (f.car().equals("and")) {
            ArrayList<Formula> al = Clausifier.separateConjunctions(f);
            for (int i = 0; i < al.size(); i++) {
                Formula f2 = (Formula) al.get(i);
                f2 = Clausifier.toCanonicalClausalForm(f2);
                CNFFormula2 cnf = new CNFFormula2(f2);
                newFormulas.add(cnf);
                cnf.sourceFormula = forig;
                cnf.reason = "clausification";
                //System.out.println("INFO in STP3.clausifyFormula(): after clausification: " + f2);
                addToDeductions(null,null,cnf);
            }
        }
        else {
            CNFFormula2 fnew = new CNFFormula2(Clausifier.toCanonicalClausalForm(f));
            fnew.sourceFormula = forig;
            fnew.reason = "clausification";
            newFormulas.add(fnew);
            addToDeductions(null,null,fnew);
            //System.out.println("INFO in STP3.clausifyFormula(): after clausification: " + fnew);
        }        
        return newFormulas;
    }

    /** *************************************************************
     */
    private void indexOneFormula(CNFFormula2 f) {

        if (f == null || f.clauses.length < 1) {
            System.out.println("Error in STP3.indexOneFormula(): null formula");
            return;
        }
        //System.out.println("INFO in STP3.indexOneFormula(): " + f);
        TreeSet<Integer> terms = f.collectTerms();                // count the appearances of terms
        Iterator it2 = terms.iterator();
        while (it2.hasNext()) {
            Integer term = (Integer) it2.next();
            if (termCounts.keySet().contains(term)) {
                Integer i = (Integer) termCounts.get(term);
                termCounts.put(term,new Integer(i.intValue() + 1));
            }
            else
                termCounts.put(term,new Integer(1));
        }

        //f.setTermPointers(posTermPointers,negTermPointers);
        f.setPredPointers(posPreds,negPreds);
    }

    /** *************************************************************
     */
    private void buildIndexes() {

        Iterator<CNFFormula2> it = formulas.iterator();
        while (it.hasNext()) {
            CNFFormula2 f = (CNFFormula2) it.next();
            indexOneFormula(f);
        }
       // System.out.println("INFO in STP3.buildIndexes(): negTermPointers: " + negTermPointers);
       // System.out.println("INFO in STP3.buildIndexes(): posTermPointers: " + posTermPointers);
    }

    /** *************************************************************
     *  Extract one clause from a CNF formula
     */
    private CNFClause2 extractRandomClause(CNFFormula2 f) {

        if (f == null || f.clauses.length < 1) {
            System.out.println("Error in STP3.extractRandomClause(): bad formula: " + f);
            return null;
        }
        return f.clauses[random.nextInt(f.clauses.length)];
    }

    /** *************************************************************
     *  Note that this class will sort into reverse order, with the
     *  largest integer values first
     */
    public class FormulaRating implements Comparable {

        public int intval = 0;
        public int age = 0;
        public CNFFormula2 form = null;

        public String toString() {
            return String.valueOf(intval) + "\n" + form.toString();
        }
        public boolean equals(Object fr_obj) {
            assert !fr_obj.getClass().getName().equals("FormulaRating") : "FormulaRating() passed object not of type FormulaRating"; 
            FormulaRating fr = (FormulaRating) fr_obj;
            return form.equals(fr.form);
        }
        public int hashCode() {
            return form.hashCode();
        }
        public int compareTo(Object avp) throws ClassCastException {

            if (!avp.getClass().getName().equalsIgnoreCase("com.articulate.sigma.STP3$FormulaRating")) 
                throw new ClassCastException("Error in FormulaRating.compareTo(): "
                                             + "Class cast exception for argument of class: " 
                                             + avp.getClass().getName());
            FormulaRating arg = (FormulaRating) avp;
            return (arg.intval + arg.age) - (intval + age);
        }
    }

    /** *************************************************************
     *  Collect Formulas, based only on their predicates.  Note that
     *  formulas that have been removed by subsumption, but are
     *  still indexed, may be returned.
     *  @param posPreds are positive predicates from the formula to
     *                  be matched (with negative ones)
     *  @param negPreds are negative predicates from the formula to
     *                  be matched (with positive ones)
     *  @return a list of candidate formulas.
     */
    private TreeSet<CNFFormula2> collectCandidatesPred(TreeSet<Integer> posFormPreds, TreeSet<Integer> negFormPreds) {

        //if (_PROVE_DEBUG) System.out.println("INFO in STP3.collectCandidatesPred(): posPreds: " + posPreds);
        //if (_PROVE_DEBUG) System.out.println("INFO in STP3.collectCandidatesPred(): negPreds: " + negPreds);
        //if (_PROVE_DEBUG) System.out.println("INFO in STP3.collectCandidatesPred(): posFormPreds: " + posFormPreds);
        //if (_PROVE_DEBUG) System.out.println("INFO in STP3.collectCandidatesPred(): negFormPreds: " + negFormPreds);
        //if (_PROVE_DEBUG) System.out.println("INFO in STP3.collectCandidatesPred(): intToTermMap: " + CNFFormula2.intToTermMap);

        TreeSet<CNFFormula2> result = new TreeSet();
        Iterator it = posFormPreds.iterator();
        while (it.hasNext()) {      // find formulas that have any of the terms as the clause to be proven
            Integer term = (Integer) it.next();
            if (negPreds.containsKey(term)) {
                ArrayList<CNFFormula2> al = negPreds.get(term);
                result.addAll(al);
            }
        }
        Iterator it2 = negFormPreds.iterator();
        while (it2.hasNext()) {      // find formulas that have any of the terms as the clause to be proven
            Integer term = (Integer) it2.next();
            if (posPreds.containsKey(term)) {
                ArrayList<CNFFormula2> al = posPreds.get(term);
                result.addAll(al);
            }
        }

        //if (_PROVE_DEBUG) System.out.println("INFO in STP3.collectCandidatesPred(): result: " + result);
        return result;
    }

    /** *************************************************************
     */
    private CNFFormula2 getRandomFormula() {

        CNFFormula2 form = null;
        CNFFormula2 result = null;
        do {            
            form = (CNFFormula2) formulas.get(random.nextInt(formulas.size()));
            System.out.println("INFO in STP3.getRandomFormula(): form:\n" + form);
            result = findInstantiation(form);
            System.out.println("INFO in STP3.getRandomFormula(): result:\n" + result);
        } while (result == null);
        if (result.clauses.length == form.clauses.length && form.clauses.length > 1)         
            formulas.remove(form);
        return result;
    }

    /** *************************************************************
     *  Instantiate a formula, or, randomly, just the negation of a
     *  clause from the formula.
     */
    private CNFFormula2 findInstantiation(CNFFormula2 f) {

        if (f == null || f.clauses.length < 1) {
            System.out.println("Error in STP3.findInstantiation(): empty formula");
            return null;
        }

        //if (f.isGround()) {
        //    return null;
        //}

        if (_GEN_DEBUG) System.out.println("\nINFO in STP3.findInstantiation(): checking instantiations for \n" + f);
        CNFFormula2 instantiationCandidate = new CNFFormula2();
        //proofCounter++;
        if (f.clauses.length == 1) {
            if (!f.isGround()) {
                instantiationCandidate = instantiationCandidate.addClause(f.clauses[0].deepCopy());
                instantiationCandidate.clauses[0].negated = !instantiationCandidate.clauses[0].negated;
                instantiationCandidate.generateVariableValues();  
            }
            else {
                if (_PROVE_DEBUG) System.out.println("\nINFO in STP3.findInstantiation(): returning ground formula (1):\n" + f);
                instantiationCandidate = instantiationCandidate.addClause(f.clauses[0].deepCopy());
                instantiationCandidate.clauses[0].negated = !instantiationCandidate.clauses[0].negated;
            }
        }
        else if (random.nextInt(4) != 0) {   // 3 in 4 chance to instantiate a clause, 1 in 4 to do the whole formula
            CNFClause2 clause = extractRandomClause(f.deepCopy());
            if (clause.isGround()) {
                clause.negated = !clause.negated;
                instantiationCandidate = instantiationCandidate.addClause(clause);
                if (_GEN_DEBUG) System.out.println("\nINFO in STP3.findInstantiation(): asserting ground formula (2):\n" + clause);
            }
            else {
                clause.generateVariableValues();
                clause.negated = !clause.negated;
                instantiationCandidate = instantiationCandidate.addClause(clause);
                if (_GEN_DEBUG) System.out.println("\nINFO in STP3.findInstantiation(): adding instantiated clause:\n" + instantiationCandidate);
                //proofCounter = 0;
            }
        }
        else {
            for (int i = 0; i < f.clauses.length; i++) {
                CNFClause2 clause = f.clauses[i].deepCopy();
                clause.negated = !clause.negated;
                if (!clause.isGround()) 
                    clause.generateVariableValues();
                instantiationCandidate = instantiationCandidate.addClause(clause);     
                if (_GEN_DEBUG) System.out.println("\nINFO in STP3.findInstantiation(): instantiated formula:\n" + instantiationCandidate);
            }
        }        
        instantiationCandidate.inferenceStepCount = f.inferenceStepCount + 1;
        return instantiationCandidate;
    }

    /** *************************************************************
     *  Record a single successful resolution, (or assertion used in
     *  the proof) indexed by conclusion.
     */
    private void addToDeductions(CNFFormula2 f1, CNFFormula2 f2, CNFFormula2 con) {

 //       if (f1 == null & f2 == null) 
 //           return;
        _PSUEDO_TIME++;
        STP3.deductionsMade++;
        CNFFormula2 newcon = con.deepCopy();
        //newcon = newcon.normalizeVariables(true);
        newcon.stringRep = null;
        CNFFormula2 f1new = null;
        CNFFormula2 f2new = null;
        ArrayList twoSupports = new ArrayList();
        if (f1 != null) {  
            f1new = f1.deepCopy();
            //f1new = f1new.normalizeVariables(true);
            f1new.stringRep = null;
            twoSupports.add(f1new);
            if (deductions.get(f1new.toString()) == null) 
                deductions.put(f1new.toString().trim(),null);
        }
        if (f2 != null) {
            f2new = f2.deepCopy();
            //f2new = f2new.normalizeVariables(false);
            f2new.stringRep = null;
            twoSupports.add(f2new);
            if (deductions.get(f2new.toString()) == null) 
                deductions.put(f2new.toString().trim(),null);
        }
        deductions.put(newcon.toString().trim(),twoSupports);
        if (_PROVE_DEBUG) System.out.println("INFO in STP3.addToDeductions()");
        if (_PROVE_DEBUG) System.out.println("conclusion: " + newcon);
        if (f1 != null)         
            if (_PROVE_DEBUG) System.out.println("premise 1: " + f1new);
        if (f2 != null)         
            if (_PROVE_DEBUG) System.out.println("premise 2: " + f2new);
    }

    /** *************************************************************
     *  Determine whether a formula in the knowledge base already
     *  subsumes the given formula
     *  @param form is a formula that might be subsumed by an
     *              existing formula.  It should have even-numbered
     *              variables.
     *  @result true if the formula is subsumed, false otherwise
     */
    private boolean subsumedByKB(CNFFormula2 form) {

        ArrayList<CNFFormula2> removeList = new ArrayList();
        if (_PROVE_DEBUG) System.out.println("INFO in STP3.subsumedByKB(): Checking if \n" + form + "\n would be subsumed by KB of " + 
                           formulas.size() + " entries");
        Iterator it = formulas.iterator();
        while (it.hasNext()) {
            CNFFormula2 kbForm = (CNFFormula2) it.next();
            if (_PROVE_DEBUG) System.out.println("INFO in STP3.subsumedByKB(): Checking against\n" + kbForm);
            if (form.subsumedBy(kbForm)) {
                if (_PROVE_DEBUG) System.out.println("INFO in STP3.subsumedByKB(): new formula \n" + form + "\n is subsumed by KB formula\n" + kbForm);
                STP3.subsumptions++;
                return true;            
            }
            if (kbForm.subsumedBy(form)) {
                if (_PROVE_DEBUG) System.out.println("INFO in STP3.subsumedByKB(): KB formula \n" + kbForm + "\n is subsumed by new formula\n" + form);
                STP3.subsumptions++;
                removeList.add(kbForm);
            }
        }
        for (int i = 0; i < removeList.size(); i++) 
            formulas.remove((CNFFormula2) removeList.get(i));
        return false;
    }

    /** *************************************************************
     *  Get a formula from the To Be Used collection
     */
    private FormulaRating getFromTBU() {

        if (TBU.size() < 1) {
            System.out.println("Error in STP3.getFromTBU(): empty TBU");
            return null;
        }
        if (random.nextInt(4) == 3) {
            int clauseNum = random.nextInt(TBU.size());
            if (_PROVE_DEBUG) System.out.println("\n\nINFO in STP3.getFromTBU(): getting random formula from TBU: \n" + 
                                                 TBU.get(clauseNum));
            return (FormulaRating) TBU.remove(clauseNum);
        }
        else
            return (FormulaRating) TBU.remove(0);
    }

    /** *************************************************************
     */
    private void addInstantiation(CNFFormula2 f) {

        if (_GEN_DEBUG) System.out.println("INFO in STP3.addInstantiation()");
        CNFFormula2 fnew = findInstantiation(f);
        if (fnew != null && !formulas.contains(fnew)) {
            FormulaRating avpNew = new FormulaRating();
            fnew = fnew.normalizeVariables(true);
            avpNew.form = fnew.subsumedClauses();
            avpNew.form.reason = "generation";
            avpNew.intval = 10000-fnew.size();
            avpNew.age = _PSUEDO_TIME;
            if (_GEN_DEBUG) System.out.println("INFO in STP3.addInstantiation(): new instantiated formula:\n" + avpNew.form);
            if (!TBU.contains(avpNew)) {
                //addToDeductions(f,candidate,resultForm);
                TBU.add(avpNew);
                Collections.sort(TBU);
            }
        }
    }

    /** *************************************************************
     *  Find support for a formula
     *  @return an ArrayList of Formulas that consitute support for
     *          the clause.  Return an empty ArrayList if no proof
     *          is found.
     */
    private void prove(int secondsLimit) {

        boolean _BASIC_STATUS = true;

        System.out.println("INFO in STP3.prove(): timeout limit: " + secondsLimit);
        long t_start = System.currentTimeMillis();
        long t_elapsed = (System.currentTimeMillis() - t_start) / 1000;
        while (TBU.size() > 0) {
            if (_PROVE_DEBUG) System.out.println("\n\nINFO in STP3.prove(): TBU (0): " + TBU);
            if (_PROVE_DEBUG) System.out.println("\nTBU length: " + TBU.size());
            FormulaRating avp = getFromTBU();
            //System.out.println("INFO in STP3.prove(): attempting to prove: " + avp.form);
            CNFFormula2 f = avp.form.deepCopy();

            TreeSet<Integer> posFormPreds = new TreeSet();
            TreeSet<Integer> negFormPreds = new TreeSet();
            f.getPredicates(posFormPreds,negFormPreds);
            TreeSet<CNFFormula2> candidates = collectCandidatesPred(posFormPreds,negFormPreds);               

            if (candidates != null && candidates.size() > 0) {
                Iterator it = candidates.iterator();
                while (it.hasNext()) {
                    CNFFormula2 candidate = (CNFFormula2) it.next();
                    if (!formulas.contains(candidate)) // handle the case where a formula removed by subsumption is still indexed
                        continue;
                    if (_PROVE_DEBUG) System.out.println("INFO in STP3.prove(): checking candidate:\n" + candidate +
                                           "\n with formula: \n" + f);
                    //if (_PROVE_DEBUG) System.out.println("\n\nINFO in STP3.prove(): TBU (1): " + TBU);
                    CNFFormula2 resultForm = new CNFFormula2();
                    TreeMap mappings = f.hyperResolve(candidate,resultForm);
                    if (_PROVE_DEBUG) System.out.println("\nINFO in STP3.prove(): resolve result (1):\n" + resultForm);
                    STP3.resolutionsAttempted++;
                    if (resultForm != null && mappings != null && resultForm.empty()) {  // Successful resolution! Result is empty list
                        if (_PROVE_DEBUG || _BASIC_STATUS) System.out.println("\nINFO in STP3.prove(): sucessful resolution:" + resultForm + 
                                                             "\n from \n" + f + "\nand\n" + candidate);
                        if (_PROVE_DEBUG && resultForm.toString().equals("()")) System.out.println("\nINFO in STP3.prove(): valid empty set result");
                        addToDeductions(f,candidate,resultForm);
                        return;
                    }
                    if (mappings != null) {  // && mappings.keySet().size() > 0
                        if (_PROVE_DEBUG || _BASIC_STATUS) System.out.println("\nINFO in STP3.prove(): resolve result:\n" + resultForm);
                        if (_PROVE_DEBUG || _BASIC_STATUS) System.out.println("for candidate (step " + candidate.inferenceStepCount + ")\n" + 
                                                                              candidate + "\n with formula (step " + f.inferenceStepCount + ")\n " + f);
                        FormulaRating avpNew = new FormulaRating();
                        if (!formulas.contains(resultForm)) {
                            resultForm = resultForm.normalizeVariables(true);
                            CNFFormula2 subsumption = resultForm.subsumedClauses();
                            if (!resultForm.deepEquals(subsumption)) {
                                subsumption.reason = "clause subsumption";
                                addToDeductions(resultForm,null,subsumption);
                                if (_PROVE_DEBUG || _BASIC_STATUS) System.out.println("\nINFO in STP3.prove(): resolve result:\n" + resultForm +
                                                                                      "\nreduced to\n" + subsumption);
                            }
                            avpNew.form = subsumption;
                            avpNew.intval = 10000-resultForm.size();
                            avpNew.age = _PSUEDO_TIME;
                            if (!TBU.contains(avpNew) && !subsumedByKB(resultForm)) {
                                if (_PROVE_DEBUG) System.out.println("\nINFO in STP3.prove(): adding result to TBU:\n" + resultForm);
                                //if (_PROVE_DEBUG) System.out.println("\n\nINFO in STP3.prove(): TBU (2): " + TBU);
                                addToDeductions(f,candidate,resultForm);
                                TBU.add(avpNew);
                                Collections.sort(TBU);
                                //if (_PROVE_DEBUG) System.out.println("\n\nINFO in STP3.prove(): TBU (2.5): " + TBU);
                            }
                            //else
                                //if (_PROVE_DEBUG) System.out.println("\n\nINFO in STP3.prove(): TBU (3): " + TBU);

                        }
                    }
                    else {
                        //if (_PROVE_DEBUG) System.out.println("INFO in STP3.prove(): candidate that did not resolve:\n" + candidate +
                        //                   "\n with formula: \n" + f);      
                    }
                    t_elapsed = (System.currentTimeMillis() - t_start) / 1000;
                    if (t_elapsed > secondsLimit) {
                        System.out.println("INFO in STP3.prove(): Timeout before answer found (in checking candidate formulas.");
                        return;
                    }

                    if (_PROVE_DEBUG || _BASIC_STATUS) System.out.println("INFO in STP3.prove(): inference step count: " + resultForm.inferenceStepCount);
                    if (_PROOF_GENERATION && resultForm.inferenceStepCount > 0 && !candidate.reason.equals("generation") && TBU.size() > 0)
                        // if ((t_elapsed > (secondsLimit * 0.01) && random.nextInt(5) == 5) || (t_elapsed > (secondsLimit * 0.02))) 
                            addInstantiation(getFromTBU().form);                
                }
                // if (_PROOF_GENERATION && (TBU.size() == 0 || t_elapsed > (secondsLimit * 0.75))) {
                if (_PROOF_GENERATION && TBU.size() == 0 && !f.reason.equals("generation")) 
                    addInstantiation(f);                
            }
            indexOneFormula(f);       // all lemmas must be added to the knowledge base for completeness
            f = f.normalizeVariables(false);
            formulas.add(f.subsumedClauses());
            if (STP3.resolutionsAttempted % 10 == 0) System.out.print(".");

            t_elapsed = (System.currentTimeMillis() - t_start) / 1000;
            System.out.println("INFO in STP3.prove(): t_elapsed: " + t_elapsed);
            if (t_elapsed > secondsLimit) {
                System.out.println("INFO in STP3.prove(): Timeout before answer found (in TBU loop).");
                return;
            }
            if (_PROOF_GENERATION && TBU.size() == 0 && f.reason != null && !f.reason.equals("generation")) 
                addInstantiation(f);                
        }
        System.out.println("INFO in STP3.prove(): Ran out of candidates in TBU.");
    }

    /** *************************************************************
     *  @param f is the formula of the proof step to be printed
     *  @param formIds are all the formulas used in the proof
     *  @param alreadyPrinted is a list of the Integer ids (the
     *                        index from the formIds ArrayList) of
     *                        formulas that have already been
     *                        printed in the proof.
     */
    private String formatResultRecurse (CNFFormula2 f, ArrayList<String> formIds, ArrayList<Integer> alreadyPrinted) {

        if (alreadyPrinted.contains(formIds.indexOf(f.toString().trim()))) 
            return null;
        alreadyPrinted.add(new Integer(formIds.indexOf(f.toString().trim())));
        //System.out.println("INFO in STP3.formatResultRecurse(): alreadyPrinted: " + alreadyPrinted);
        //System.out.println("INFO in STP3.formatResultRecurse(): checking formula: " + f);
        //System.out.println("INFO in STP3.formatResultRecurse(): this formula id: " + formIds.indexOf(f.toString().trim()));

        StringBuffer result = new StringBuffer();
        ArrayList<CNFFormula2> support = (ArrayList) deductions.get(f.toString().trim());
        //System.out.println("INFO in STP3.formatResultRecurse(): support: " + support);
        if (support != null && support.size() > 0) {
            CNFFormula2 f1 = (CNFFormula2) support.get(0);
            //System.out.println("INFO in STP3.formatResultRecurse(): internal checking formula: " + f1);
            CNFFormula2 f2 = null;
            if (support.size() > 1) {
                f2 = (CNFFormula2) support.get(1);
                //System.out.println("INFO in STP3.formatResultRecurse(): internal checking formula 2: " + f2);
            }
            //System.out.println("INFO in STP3.formatResultRecurse(): recursing on formula 1: " + f1);
            String f1result = formatResultRecurse(f1,formIds,alreadyPrinted);
            if (f1result != null) 
                result.append(f1result);
            if (f2 != null) {
                //System.out.println("INFO in STP3.formatResultRecurse(): recursing on formula 2: " + f2);
                String f2result = formatResultRecurse(f2,formIds,alreadyPrinted);
                if (f2result != null) 
                    result.append(f2result); 
            }
            result.append("<proofStep>\n");
            result.append("<premises>\n");
            if (f1 != null) {
                if (formIds.indexOf(f1.toString().trim()) < 0) 
                    formIds.add(f1.toString().trim());
                result.append("<premise>\n<formula number='" + 
                              formIds.indexOf(f1.toString().trim()) + "'>\n");
                f1.stringRep = null;
                result.append(f1.toStringFormat(false).trim() + "\n");
                result.append("</formula>\n</premise>\n");
            }
            if (f2 != null) {
                if (formIds.indexOf(f2.toString().trim()) < 0) 
                    formIds.add(f2.toString().trim());
                result.append("<premise>\n<formula number='" + 
                              formIds.indexOf(f2.toString().trim()) + "'>\n");
                f2.stringRep = null;
                result.append(f2.toStringFormat(false).trim() + "\n");
                result.append("</formula>\n</premise>\n");
            }
            result.append("</premises>\n");
        }
        else {
            result.append("<proofStep>\n");
            result.append("<premises>\n");
            result.append("</premises>\n");
        }
        if (formIds.indexOf(f.toString().trim()) < 0) 
            formIds.add(f.toString().trim());      
        result.append("<conclusion>\n<formula number='" + formIds.indexOf(f.toString().trim()));
        f.stringRep = null;
        result.append("'>\n" + f.toStringFormat(false).trim() + "\n</formula>\n</conclusion>\n");
        result.append("</proofStep>\n");

        //System.out.println(result);
        return result.toString();
    }

    /** *************************************************************
     *  Get all the deductions that belong in the proof.
     */
    private TreeMap<String, ArrayList<CNFFormula2>> getDeductionsRecurse (CNFFormula2 form) {
         
        TreeMap<String, ArrayList<CNFFormula2>> result = new TreeMap();
        ArrayList<CNFFormula2> support = (ArrayList) deductions.get(form.toString().trim());
        //System.out.println("INFO in STP3.getDeductionsRecurse(): support: " + support);
        if (support != null && support.size() > 0) {
            CNFFormula2 f1 = (CNFFormula2) support.get(0);
            //System.out.println("INFO in STP3.getDeductionsRecurse(): internal checking formula: " + f1);
            CNFFormula2 f2 = null;
            if (support.size() > 1) {
                f2 = (CNFFormula2) support.get(1);
                result.put(form.toString().trim(),support);
                //System.out.println("INFO in STP3.getDeductionsRecurse(): internal checking formula 2: " + f2);
            }
            if (f1 != null && !f1.toString().equals("()")) {
                //System.out.println("INFO in STP3.getDeductionsRecurse(): recursing on formula 1: " + f1);
                result.putAll(getDeductionsRecurse(f1));
            }
            if (f2 != null && !f2.toString().equals("()")) {
                //System.out.println("INFO in STP3.getDeductionsRecurse(): recursing on formula 2: " + f2);
                result.putAll(getDeductionsRecurse(f2));
            }
        }
        //System.out.println("INFO in STP3.getDeductionsRecurse(): result: " + result);
        return result;
    }

    /** *************************************************************
     */
    private TreeMap<Integer,Integer> remap (TreeMap<Integer,Integer> map,
                                            TreeMap<Integer,Integer> newMap) {

        //System.out.println("INFO in STP3.remap(): map\n" + map);
        //System.out.println("INFO in STP3.remap(): newMap\n" + newMap);
        TreeMap<Integer,Integer> result = new TreeMap();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            Integer key = (Integer) it.next();
            Integer value = (Integer) map.get(key);
            if (CNFFormula.isVariable(value)) {
                Integer newValue = (Integer) newMap.get(value);
                result.put(key,newValue);
            }
            else
                result.put(key,value);
        }

        //System.out.println("INFO in STP3.remap(): \n" + result);
        return result;
    }

    /** *************************************************************
     *  @param relevantDeductions is the map of deductions that
     *                            actually appear in the proof
     *  @param form is the string representation of the formula that
     *              is recursively checked for unifications with
     *              other formulas, to determine the ultimate value
     *              of its variables
     *  @return a map of variables that appear in the formula to the
     *          values they get in the proof.
     */
    private TreeMap<Integer,Integer> extractAnswerVarsRecurse (CNFFormula2 form,
                                                               TreeMap<String, ArrayList<CNFFormula2>> relevantDeductions) {

        //System.out.println("INFO in STP3.extractAnswerVarsRecurse(): checking formula: \n" + form);
        TreeMap<Integer,Integer> result = new TreeMap();
        TreeSet<TermCell> vars = form.collectVariables();
        //System.out.println("INFO in STP3.extractAnswerVarsRecurse(): variables: \n" + vars);
        Iterator it = relevantDeductions.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            CNFFormula2 nextForm = null;
            ArrayList<CNFFormula2> value = (ArrayList<CNFFormula2>) relevantDeductions.get(key);
            if (value != null && value.size() > 1) {
                TreeMap map = null;
                CNFFormula2 resolveResult = new CNFFormula2();
                CNFFormula2 f1 = (CNFFormula2) value.get(0);
                CNFFormula2 f2 = (CNFFormula2) value.get(1);
                if (f1.equals(form)) {
                    //System.out.println("INFO in STP3.extractAnswerVarsRecurse(): form (argNew): \n" + form);
                    //System.out.println("INFO in STP3.extractAnswerVarsRecurse(): f1: \n" + f1);
                    //System.out.println("INFO in STP3.extractAnswerVarsRecurse(): f2: \n" + f2);
                    CNFFormula2 f2new = form.deepCopy();
                    //System.out.println("INFO in STP3.extractAnswerVarsRecurse(): f2new: \n" + f2new);
                    nextForm = f2new.deepCopy();
                    //System.out.println("INFO in STP3.extractAnswerVarsRecurse(): nextForm: \n" + nextForm);
                    map = form.hyperResolve(f2new,resolveResult);
                }
                else if (f2.equals(form)) {    
                    //System.out.println("INFO in STP3.extractAnswerVarsRecurse(): form (argNew): \n" + form);
                    //System.out.println("INFO in STP3.extractAnswerVarsRecurse(): f1: \n" + f1);
                    //System.out.println("INFO in STP3.extractAnswerVarsRecurse(): f2: \n" + f2);
                    CNFFormula2 f1new = form.deepCopy();
                    //System.out.println("INFO in STP3.extractAnswerVarsRecurse(): f1new: \n" + f1new);
                    nextForm = f1new.deepCopy();
                    map = form.hyperResolve(f1new,resolveResult);
                }
                //System.out.println("INFO in STP3.extractAnswerVarsRecurse(): map: \n" + map);
                if (map != null && map.keySet().size() > 0) {
                    boolean allConstants = true;
                    Iterator it2 = vars.iterator();
                    while (it2.hasNext()) {
                        TermCell variable = (TermCell) it2.next();
                        Integer varValue = (Integer) map.get(new Integer(variable.term));
                        if (varValue != null) {
                            result.put(new Integer(variable.term),varValue);
                            if (!TermCell.isConstant(varValue)) 
                                allConstants = false;
                        }
                    }
                    //System.out.println("INFO in STP3.extractAnswerVarsRecurse(): " + result);
                    if (allConstants) {
                        //System.out.println("INFO in STP3.extractAnswerVarsRecurse(): all constants");
                        //System.out.println("INFO in STP3.extractAnswerVarsRecurse(): result: \n" + result);
                        return result;
                    }
                    else {
                        TreeMap<Integer,Integer> newResult = new TreeMap();
                        //System.out.println("INFO in STP3.extractAnswerVarsRecurse(): nextForm(2): \n" + nextForm);
                        newResult = extractAnswerVarsRecurse(resolveResult,relevantDeductions);
                        //System.out.println("INFO in STP3.extractAnswerVarsRecurse(): newResult " + newResult);
                        return remap(result,newResult);
                    }
                }
            }
        }

        //System.out.println("INFO in STP3.extractAnswerVarsRecurse() 2: " + result);
        return result;
    }

    /** *************************************************************
     *  Create a map of bindings for answer variables
     * 
     *  @return a map of variables that appear in the formula to the
     *          values they get in the proof.
     */
    private TreeMap<Integer,Integer> extractAnswerVars (CNFFormula2 form) {

        CNFFormula2 f = new CNFFormula2();
        f.read("()");
        TreeMap<String, ArrayList<CNFFormula2>> relevantDeductions = getDeductionsRecurse(f);
        return extractAnswerVarsRecurse(form,relevantDeductions);
    }

    /** *************************************************************
     *  Create XML markup for reporting answer variables
     */
    private String formatVariableBindings (TreeMap<Integer,Integer> valueMap) {

        StringBuffer result = new StringBuffer();

        //System.out.println("INFO in STP3.formatVariableBindings(): " + valueMap);
        Iterator it = valueMap.keySet().iterator();
        while (it.hasNext()) {
            Integer key = (Integer) it.next();
            if (!TermCell.isVariable(key)) {
                System.out.println("Error in STP3.formatVariableBindings(): key is not variable: " + key);
                return null;
            }
            String binding = null;
            Integer value = (Integer) valueMap.get(key);
            if (CNFClause.isFunction(value)) 
                binding = "function";
            else {
                if (CNFClause.isVariable(value)) 
                    binding = "?VAR" + String.valueOf(value);                
                else
                    binding = (String) CNFFormula2.intToTermMap.get(value);
            }
            result.append("<binding>\n<var name='");
            result.append("?VAR" + key);            
            result.append("' value='");
            result.append(binding);
            result.append("'/>\n</binding>\n");
        }
        return result.toString();
    }

    /** *************************************************************
     */
    private String formatResultNew (String query) {

        CNFFormula2 fQuery = new CNFFormula2();
        fQuery.read(query);
        boolean ground = false;
        if (fQuery.isGround()) 
            ground = true;        
        //System.out.println("INFO in STP3.formatResultNew(): deduction keys " + deductions.keySet());
        //printDeductions();
        ArrayList<Integer> alreadyPrinted = new ArrayList();
        ArrayList<String> formIds = new ArrayList();
        formIds.addAll(deductions.keySet());

        StringBuffer result = new StringBuffer();
        if (!deductions.containsKey("()")) {
            System.out.println("INFO in STP3.formatResultNew(): no empty clause in result");
            result.append("<queryResponse>\n<answer result='no' number='0'>\n</answer>\n</queryResponse>\n");
        }
        else {
            System.out.println("INFO in STP3.formatResultNew(): successful resolution: empty clause in result");
            result.append("<queryResponse>\n<answer result='yes' number='1'>\n");
            if (ground) 
                result.append("<bindingSet type='definite'>\n<binding>yes<var name='' value=''/>\n</binding>\n</bindingSet>\n");
            else {
                TreeMap<Integer,Integer> valueMap = extractAnswerVars(fQuery);
                result.append("<bindingSet type='definite'>\n");
                result.append(formatVariableBindings(valueMap));
                result.append("</bindingSet>\n");
            }
            result.append("<proof>\n");
            CNFFormula2 f = new CNFFormula2();
            f.read("()");
            result.append(formatResultRecurse(f,formIds,alreadyPrinted));
            result.append("</proof>\n</answer>\n<summary proofs='1'/>\n</queryResponse>\n");
        }
        //System.out.println("INFO in STP3.formatResult(): " + result.toString());
        return result.toString();
    }

    /** *************************************************************
     * Submit a query.
     *
     * @param formula query in the KIF syntax (not negated)
     * @param timeLimit time limit for answering the query (in seconds)
     * @param bindingsLimit limit on the number of bindings
     * @return answer to the query (in the XML syntax)
     * @throws IOException should not normally be thrown
     */
    @Override
    public String submitQuery (String formula, int timeLimit, int bindingsLimit) {

        System.out.println("INFO in STP3.submitQuery(): query: " + formula);
        System.out.println("INFO in STP3.submitQuery(): formulas: " + formulas.size());
        //System.out.println("INFO in STP3.submitQuery(): formulas: " + formulas);
        System.out.println("INFO in STP3.submitQuery(): deductions: " + deductions.keySet().size());
        Formula negQuery = new Formula();
        Formula rawNegQuery = new Formula();
        rawNegQuery.read("(not " + formula + ")");
        negQuery.read("(not " + formula + ")");
        negQuery = Clausifier.clausify(negQuery);     // negation will be pushed in
        if (negQuery.equals(rawNegQuery)) {
            CNFFormula2 cnf = new CNFFormula2();
            cnf.read(negQuery.theFormula);
            cnf = cnf.normalizeVariables(true);
            addToDeductions(null,null,cnf);
        }
        else {
            CNFFormula2 originalQuery = new CNFFormula2();
            originalQuery.read("(not " + formula + ")");
            CNFFormula2 cnf = new CNFFormula2();
            cnf.read(negQuery.theFormula);
            cnf = cnf.normalizeVariables(true);
            addToDeductions(originalQuery,null,cnf);
        }
        System.out.println("INFO in STP3.submitQuery(): clausified query: " + negQuery);
        FormulaRating avp = null;
        if (negQuery.car().equals("and")) {
            System.out.println("INFO in STP3.submitQuery(): conjunctive query, ignore previous non-CNF errors.");
            ArrayList<Formula> al = Clausifier.separateConjunctions(negQuery);
            for (int i = 0; i < al.size(); i++) {
                Formula f2 = (Formula) al.get(i);
                avp = new FormulaRating();                
                avp.form = new CNFFormula2(f2);
                avp.form = avp.form.normalizeVariables(true);
                CNFFormula2 subsumption = avp.form.subsumedClauses();
                if (!subsumption.deepEquals(avp.form)) {
                    subsumption.reason = "clause subsumption";
                    addToDeductions(avp.form,null,subsumption);
                }
                avp.form = subsumption;
                avp.intval = avp.form.size();
                avp.age = _PSUEDO_TIME;
                TBU.add(avp);
                Collections.sort(TBU);
                System.out.println("INFO in STP3.submitQuery(): adding to TBU: " + avp);
            }
        }
        else {
            avp = new FormulaRating();
            avp.form = new CNFFormula2(negQuery);
            avp.form = avp.form.normalizeVariables(true);
            CNFFormula2 subsumption = avp.form.subsumedClauses();
            if (!subsumption.deepEquals(avp.form)) {
                subsumption.reason = "clause subsumption";
                addToDeductions(avp.form,null,subsumption);
            }
            avp.form = subsumption;
            avp.intval = avp.form.size();
            avp.age = _PSUEDO_TIME;
            TBU.add(avp);
            Collections.sort(TBU);
        }

        long t_start = System.currentTimeMillis();
        // ArrayList<CNFFormula> res = prove(timeLimit); 
        prove(timeLimit); 
        //System.out.println("INFO in STP3.submitQuery(): deductions: \n" + deductions);
        // return res.toString();
        System.out.println("=============================");
        long t_elapsed = (System.currentTimeMillis() - t_start) / 1000;
        System.out.println(t_elapsed + " seconds. " + timeLimit + " seconds time limit.");            
        System.out.println("INFO in STP3.submitQuery(): deductions made:       " + STP3.deductionsMade);
        System.out.println("INFO in STP3.submitQuery(): resolutions attempted: " + STP3.resolutionsAttempted);
        System.out.println("INFO in STP3.submitQuery(): subsumptions:          " + STP3.subsumptions);
        System.out.println("=============================");
        String XMLresult = formatResultNew(negQuery.theFormula);
        System.out.println("INFO in STP3.submitQuery(): result: \n" + XMLresult);
        return XMLresult;
    }

    /** *************************************************************
     *  Removes all formulas added by previous deductions.
     */
    public void clear() {

        System.out.println("INFO in STP3.clear()");
        deductions = new TreeMap();
        TBU = new ArrayList();
        negPreds = new TreeMap();  
        posPreds = new TreeMap();  // Appearance of positive clauses
        termCounts = new TreeMap();    // Key is term index, value is the count of appearances
        //posTermPointers = new TreeMap(); // appearance of a term in a positive literal
        //negTermPointers = new TreeMap(); // appearance of a term in a negative literal
        ArrayList<CNFFormula2> newFormulas = new ArrayList();
        TreeMap<String, CNFFormula2> newFormulaMap = new TreeMap();
        Iterator it = formulas.iterator();
        while (it.hasNext()) {
            CNFFormula2 cnf = (CNFFormula2) it.next();
            if (cnf.sourceFormula != null) {
                newFormulas.add(cnf);
                newFormulaMap.put(cnf.toString(),cnf);
                indexOneFormula(cnf);
            }
        }
        formulas = newFormulas;
        formulaMap = newFormulaMap;

        /** Statistics */
        deductionsMade = 0;
        resolutionsAttempted = 0;
        subsumptions = 0;
    }

    /** *************************************************************
     * Add an assertion.  This method will clausify and index it.
     *
     * @param formula asserted formula in the KIF syntax
     * @return answer to the assertion (in the XML syntax)
     * @throws IOException should not normally be thrown
     */
    @Override
    public String assertFormula(String form) throws IOException {

        Formula f = new Formula();
        f.read(form);
        ArrayList<CNFFormula2> cnfForms = clausifyFormula(f);
        if (cnfForms == null) 
            return ("<assertionResponse>Error in clausification</assertionResponse>");        
        for (int i = 0; i < cnfForms.size(); i++) {
            CNFFormula2 cnf = (CNFFormula2) cnfForms.get(i);
            indexOneFormula(cnf);
            formulas.add(cnf);
        }
        return ("<assertionResponse>ok</assertionResponse>");
    }
    
    /** *************************************************************
     * Terminates this instance of InferenceEngine. 
     * <font color='red'><b>Warning:</b></font>After calling this functions
     * no further assertions or queries can be done.
     * 
     * Some inference engines might not need/support termination. In that case this
     * method does nothing.
     *
     * @throws IOException should not normally be thrown
     */
    public void terminate()
    	throws IOException
    {
    }

    /** *************************************************************
     */
    public static void generateTestProblem() {

        ArrayList<String> al = new ArrayList();
        STP3 stp = null;
        try {
            stp = new STP3("/home/apease/Sigma/KBs/Merge.kif");
            //stp = new STP3("/home/apease/Sigma/KBs/tqBackground.kif");
            //al.add("(=> (instance ?X290 Collection) (exists (?X12) (member ?X12 ?X290)))");
            //al.add("(subclass Organization Collection)");
            //al.add("(instance SetOrClass SetOrClass)");
            //stp.assertFormula("(instance Org1-1 Organization)");
            //al.add("(=> (subclass ?X403 ?X404) (and (instance ?X403 SetOrClass) (instance ?X404 SetOrClass)))");
            //al.add("(=> (and (instance ?X403 SetOrClass) (instance ?X404 SetOrClass)) " +
            //       "(=> (and (subclass ?X403 ?X404) (instance ?X405 ?X403)) (instance ?X405 ?X404)))");
            //stp = new STP3(al);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("INFO in STP3.generateTestProblem(): Number of formulas: " + stp.formulas.size());
        long seed = System.currentTimeMillis() % 9876;
        //System.out.println("INFO in STP3.generateTestProblem(): Seed: " + seed);
        //stp.random = new Random(seed);
        stp.random = new Random(9642);
        stp._PROOF_GENERATION = true;
        long startTime = System.currentTimeMillis();
        String xmlResult = "";
        //while (System.currentTimeMillis() < startTime + 60*1000 && xmlResult.length() < 1000) {
            CNFFormula2 f = stp.getRandomFormula();
            System.out.println("---------------------------------------");
            System.out.println("INFO in STP3.generateTestProblem(): Starting generation from query: " + f);
            xmlResult = stp.submitQuery(f.toString(),60,0); 
            if (xmlResult.length() > 1000) 
                System.out.println(xmlResult);            
            stp.clear();
        //}
    }

    /** *************************************************************
     */
    public static void generateTestProblem2() {

        ArrayList<String> al = new ArrayList();
        STP3 stp = null;
        try {
            al.add("(=> (and (a ?x ?y) (b ?x ?y)) (c ?x ?y))");
            al.add("(=> (and (y ?x ?y) (z ?x ?y)) (a ?x ?y))");
            al.add("(=> (and (v ?x ?y) (w ?x ?y)) (b ?x ?y))");
            al.add("(=> (and (t ?x ?y) (u ?x ?y)) (w ?x ?y))");
            al.add("(=> (and (r ?x ?y) (s ?x ?y)) (v ?x ?y))");
            stp = new STP3(al);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("INFO in STP3.generateTestProblem2(): Number of formulas: " + stp.formulas.size());
        long seed = System.currentTimeMillis() % 9876;
        System.out.println("INFO in STP3.generateTestProblem2(): Seed: " + seed);
        stp.random = new Random(seed);
        //stp.random = new Random(2);
        stp._PROOF_GENERATION = true;
        long startTime = System.currentTimeMillis();
        String xmlResult = "";
        //while (System.currentTimeMillis() < startTime + 60*1000 && xmlResult.length() < 1000) {
            CNFFormula2 f = new CNFFormula2();
            f.read("(c foo bar)");
            System.out.println("---------------------------------------");
            System.out.println("INFO in STP3.generateTestProblem2(): Starting generation from query: " + f);
            xmlResult = stp.submitQuery(f.toString(),60,0); 
            if (xmlResult.length() > 1000) 
                System.out.println(xmlResult);            
            stp.clear();
        //}
    }

    /** *************************************************************
     */
    public static void generateTestProblem3() {

        ArrayList<String> al = new ArrayList();
        STP3 stp = null;
        try {
            al.add("(=> (and (a ?x) (b ?x)) (c ?x))");
            al.add("(=> (and (y ?x) (z ?x)) (a ?x))");
            al.add("(=> (and (v ?x) (w ?x)) (b ?x))");
            stp = new STP3(al);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("INFO in STP3.generateTestProblem3(): Number of formulas: " + stp.formulas.size());
        long seed = System.currentTimeMillis() % 9876;
        System.out.println("INFO in STP3.generateTestProblem3(): Seed: " + seed);
        stp.random = new Random(seed);
        //stp.random = new Random(2);
        stp._PROOF_GENERATION = true;
        long startTime = System.currentTimeMillis();
        String xmlResult = "";
        //while (System.currentTimeMillis() < startTime + 60*1000 && xmlResult.length() < 1000) {
            CNFFormula2 f = new CNFFormula2();
            f.read("(c foo)");
            System.out.println("---------------------------------------");
            System.out.println("INFO in STP3.generateTestProblem3(): Starting generation from query: " + f);
            xmlResult = stp.submitQuery(f.toString(),10,0); 
            if (xmlResult.length() > 1000) 
                System.out.println(xmlResult);            
            stp.clear();
        //}
    }

    /** *************************************************************
     */
    public static void generateTestProblem4() {

        ArrayList<String> al = new ArrayList();
        STP3 stp = null;
        try {
            al.add("(=> (and (a ?x) (b ?x)) (c ?x))");
            stp = new STP3(al);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("INFO in STP3.generateTestProblem4(): Number of formulas: " + stp.formulas.size());
        long seed = System.currentTimeMillis() % 9876;
        System.out.println("INFO in STP3.generateTestProblem4(): Seed: " + seed);
        stp.random = new Random(seed);
        //stp.random = new Random(2);
        stp._PROOF_GENERATION = true;
        long startTime = System.currentTimeMillis();
        String xmlResult = "";
        CNFFormula2 f = new CNFFormula2();
        f.read("(c foo)");
        System.out.println("---------------------------------------");
        System.out.println("INFO in STP3.generateTestProblem4(): Starting generation from query: " + f);
        xmlResult = stp.submitQuery(f.toString(),3,0); 
        if (xmlResult.length() > 1000) 
            System.out.println(xmlResult);            
        stp.clear();
    }

    /** *************************************************************
     */
    public static void tq1Abbrev() {

        ArrayList<String> al = new ArrayList();
        al.add("(=> (i ?X290 C) (exists (?X12) (m ?X12 ?X290)))");
        al.add("(s O C)");
        al.add("(i SOC SOC)");
        al.add("(i Org1-1 O)");
        al.add("(=> (s ?X403 ?X404) (and (i ?X403 SOC) (i ?X404 SOC)))");
        al.add("(=> (and (i ?X403 SOC) (i ?X404 SOC)) " +
               "(=> (and (s ?X403 ?X404) (i ?X405 ?X403)) (i ?X405 ?X404)))");
        CNFFormula2 query = new CNFFormula2();
        // query.readNonCNF("(exists (?MEMBER) (m ?MEMBER Org1-1))");
        query.readNonCNF("(m ?MEMBER Org1-1)");
        STP3 stp3 = new STP3(al);
        System.out.println(stp3.submitQuery(query.toString(),10,0)); 
    }

    /** *************************************************************
     */
    public static void tq1back() {

        ArrayList<String> al = new ArrayList();
        CNFFormula2 query = new CNFFormula2();
        query.readNonCNF("(member ?MEMBER Org1-1)");
        STP3 stp = null;
        try {
            stp = new STP3("/home/apease/Sigma/KBs/tqBackground.kif");
            stp.assertFormula("(=> (instance ?X290 Collection) (exists (?X12) (member ?X12 ?X290)))");
            stp.assertFormula("(subclass Organization Collection)");
            stp.assertFormula("(instance SetOrClass SetOrClass)");
            stp.assertFormula("(instance Org1-1 Organization)");
            stp.assertFormula("(=> (subclass ?X403 ?X404) (and (instance ?X403 SetOrClass) (instance ?X404 SetOrClass)))");
            stp.assertFormula("(=> (and (instance ?X403 SetOrClass) (instance ?X404 SetOrClass)) " +
                   "(=> (and (subclass ?X403 ?X404) (instance ?X405 ?X403)) (instance ?X405 ?X404)))");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        stp.submitQuery(query.sourceFormula.theFormula,60,0); 
    }

    /** *************************************************************
     */
    public static void tq1SUMO() {

        ArrayList<String> al = new ArrayList();
        CNFFormula2 query = new CNFFormula2();
        query.readNonCNF("(member ?MEMBER Org1-1)");
        STP3 stp = null;
        try {
            stp = new STP3("/home/apease/Sigma/KBs/Merge.kif");
            stp.assertFormula("(=> (instance ?X290 Collection) (exists (?X12) (member ?X12 ?X290)))");
            stp.assertFormula("(subclass Organization Collection)");
            stp.assertFormula("(instance SetOrClass SetOrClass)");
            stp.assertFormula("(instance Org1-1 Organization)");
            stp.assertFormula("(=> (subclass ?X403 ?X404) (and (instance ?X403 SetOrClass) (instance ?X404 SetOrClass)))");
            stp.assertFormula("(=> (and (instance ?X403 SetOrClass) (instance ?X404 SetOrClass)) " +
                   "(=> (and (subclass ?X403 ?X404) (instance ?X405 ?X403)) (instance ?X405 ?X404)))");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        stp.submitQuery(query.sourceFormula.theFormula,60,0); 
    }

    /** *************************************************************
     */
    public static void tq1cacheSUMO() {

        ArrayList<String> al = new ArrayList();
        CNFFormula2 query = new CNFFormula2();
        query.readNonCNF("(member ?MEMBER Org1-1)");
        STP3 stp = null;
        try {
            stp = new STP3("/home/apease/Sigma/KBs/Merge.kif");
            stp.assertFormula("(=> (instance ?X290 Collection) (exists (?X12) (member ?X12 ?X290)))");
            stp.assertFormula("(instance Org1-1 Organization)");
            stp.assertFormula("(instance Org1-1 Collection)");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        stp.submitQuery(query.sourceFormula.theFormula,60,0); 
    }

    /** *************************************************************
     */
    public static void tq1() {

        ArrayList<String> al = new ArrayList();
        al.add("(=> (instance ?X290 Collection) (exists (?X12) (member ?X12 ?X290)))");
        al.add("(subclass Organization Collection)");
        al.add("(instance SetOrClass SetOrClass)");
        al.add("(instance Org1-1 Organization)");
        al.add("(=> (subclass ?X403 ?X404) (and (instance ?X403 SetOrClass) (instance ?X404 SetOrClass)))");
        al.add("(=> (and (instance ?X403 SetOrClass) (instance ?X404 SetOrClass)) " +
               "(=> (and (subclass ?X403 ?X404) (instance ?X405 ?X403)) (instance ?X405 ?X404)))");
        CNFFormula2 query = new CNFFormula2();
        query.readNonCNF("(member ?MEMBER Org1-1)");
        STP3 stp = new STP3(al);
        stp.submitQuery(query.sourceFormula.theFormula,60,0); 
    }

    /** *************************************************************
     */
    public static void tq2() {

        ArrayList al = new ArrayList();
        al.add("(=> (p ?X) (q ?X))");
        al.add("(=> (or (q ?X) (r ?X)) (t ?X))");
        al.add("(p a)");
        CNFFormula2 query = new CNFFormula2();
        query.read("(or (t a) (r a))");
        STP3 stp = new STP3(al);
        System.out.println("INFO in STP3.tq2()");
        stp.submitQuery(query.sourceFormula.theFormula,30,0); 
    }

    /** *************************************************************
     */
    public static void tq12back() {

        ArrayList<String> al = new ArrayList();
        CNFFormula2 query = new CNFFormula2();
        query.readNonCNF("(instance Organism12-1 Organism)");
        STP3 stp = null;
        try {
            stp = new STP3("/home/apease/Sigma/KBs/tqBackground.kif");
            stp.assertFormula("(instance Organism12-1 Object)");
            stp.assertFormula("(attribute Organism12-1 Living)");
            stp.assertFormula("(=> (attribute ?X Living) (instance ?X Organism))");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        stp.submitQuery(query.sourceFormula.theFormula,60,0); 
    }

    /** *************************************************************
     */
    public static void factorizationTest() {

        ArrayList al = new ArrayList();
        al.add("(=> (p c) (p d))");
        CNFFormula2 query = new CNFFormula2();
        query.readNonCNF("(and (not (or a b)) (or a (not b)) (or (not a) b) (or (not a) (not b)))");
        STP3 stp = new STP3(al);
        System.out.println(stp.submitQuery(query.sourceFormula.theFormula,30,0)); 
    }

    /** *************************************************************
     *  example from Russel and Norvig
     */
    public static void rnTest() {

        ArrayList<String> al = new ArrayList();
        al.add("(instance Nono Nation)");
        al.add("(=> (and (attribute ?X American) (instance ?Y Weapon) (instance ?Z Nation) " +
                        "(attribute ?Z Hostile) (instance ?S Selling) (agent ?S ?X) (patient ?S ?Y) (recipient ?S ?Z))" +
                   "(attribute ?X Criminal))");
        al.add("(possesses Nono M1-Missile)");
        al.add("(instance M1-Missile Missile)");
        al.add("(=> (and (possesses Nono ?X) (instance ?X Missile))"+
                   "(and (instance ?S Selling) (agent ?S West) (patient ?S ?X) (recipient ?S Nono)))");
        al.add("(=> (instance ?X Missile) (instance ?X Weapon))");
        al.add("(=> (enemies ?X America) (attribute ?X Hostile))");
        al.add("(attribute West American)");
        al.add("(enemies Nono America)");
        al.add("(instance America Nation)");
        CNFFormula2 query = new CNFFormula2();
        query.read("(attribute ?X Criminal)");

        System.out.println("********************************************");
        Iterator<String> it = al.iterator();
        while (it.hasNext()) {
            String s = (String) it.next();
            Formula f = new Formula();
            f.read(s);
            System.out.println(f);
        }
        STP3 stp = new STP3(al);
        System.out.println("Query: " + query.sourceFormula); 
        System.out.println("Result: ");
        stp.submitQuery(query.sourceFormula.theFormula,60,0); 
    }

    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {

        /**
        ArrayList al = new ArrayList();
        al.add("(instance Adam Human)");
        al.add("(=> (instance ?Y Human) (attribute ?Y Mortal))");
        Formula query = new Formula();
        query.read("(attribute ?X Mortal)");
        STP stp = new STP(al);
        System.out.println(stp.submitQuery(query.theFormula,0,0)); 
    */
        //tq1Abbrev();
        //tq1();
        //tq1back();
        //tq1cacheSUMO();
        //tq1SUMO();
        //tq12back();
        generateTestProblem();
        //generateTestProblem4();
        //tq2();
        //factorizationTest();
        //rnTest();
/**
         ArrayList al = new ArrayList();
        STP stp = new STP(al);
        Formula f1 = new Formula();
        f1.read("(or (not (instance Org1-1 Collection)) (member (SkFn 1 Org1-1) Org1-1))");
        Formula f2 = new Formula();
        f2.read("(not (member (SkFn 1 Org1-1) Org1-1))");
        System.out.println(stp.removeClause(f1,f2));
        */
    }
}

