
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

/** ***************************************************************
 * The Sigma theorem prover. A simple resolution prover in Java.
 */
public class STP2 extends InferenceEngine {

    /** Randomly instantiate formulas in order to create proofs
     *  to use as tests. */
    boolean _PROOF_GENERATION = false;

    boolean _PROVE_DEBUG = false;

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

     /** The knowledge base */
    ArrayList<CNFFormula> formulas = new ArrayList();

     /** Keys are CNF formula strings. */
    TreeMap<String, CNFFormula> formulaMap = new TreeMap();

    /** individual deduction steps, indexed by conclusion. Keys are CNF formula strings. */
    TreeMap<String, ArrayList<CNFFormula>> deductions = new TreeMap();

    /** To Be Used - also has a list of axioms used to derive the
     *  clause. */
    ArrayList<FormulaRating> TBU = new ArrayList();

    public static int _GENSYM_COUNTER = 0;

    /** Used to give an "age" to each axiom, by incrementing a
     *  count each time a new axiom is added to the KB. */
    public static int _PSUEDO_TIME = 0;

    /** The indexes */
    TreeMap<CNFClause, CNFFormula> negLits = new TreeMap();  // Note that (not (a b c)) will be stored as (a b c)
    TreeMap<CNFClause, CNFFormula> posLits = new TreeMap();  // Appearance of positive clauses
    TreeMap<Integer, Integer> termCounts = new TreeMap();    // Key is term index, value is the count of appearances
    TreeMap<Integer, ArrayList<CNFFormula>> posTermPointers = new TreeMap(); // appearance of a term in a positive literal
    TreeMap<Integer, ArrayList<CNFFormula>> negTermPointers = new TreeMap(); // appearance of a term in a negative literal

    /** Statistics */
    static int deductionsMade = 0;
    static int resolutionsAttempted = 0;
    static int subsumptions = 0;

    /** ***************************************************************
     * Convert to a String.
     */
    public String toString() {

        return "An STP2 instance";
    }

    /** ***************************************************************
     */
    public static class STP2EngineFactory extends EngineFactory {

        @Override
        public InferenceEngine createWithFormulas(Iterable<String> formulaSource) {  
            return new STP2(formulaSource);
        }

        @Override
        public InferenceEngine createFromKBFile(String kbFileName) {
            return STP2.getNewInstance(kbFileName);
        }
    }

    public static EngineFactory getFactory() {
        return new STP2EngineFactory();
    }

    /** *************************************************************
     */
    private STP2(String kbFileName) throws Exception {
    
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
            System.out.println("INFO in STP2(): Starting clausification with file: " + kbFileName);
            KB kb = new KB(kbFileName);
            kb.addConstituent(kbFileName);
            //kb.addConstituent(kbFileName);
            //KIF kif = new KIF();
            //kif.setParseMode(KIF.RELAXED_PARSE_MODE);
            //kif.readFile(kbFileName);

            //Iterator it = kif.formulas.values().iterator();
            Iterator it = kb.formulaMap.values().iterator();
            int counter = 0;
            //System.out.println("INFO in STP2(): Formulas: " + kb.formulaMap.values().size());
            while (it.hasNext()) {
                Formula f = (Formula) it.next();
                ArrayList<Formula> processedForms = f.preProcess(false,kb);
                for (int j = 0; j < processedForms.size(); j++) {
                    Formula f2 = (Formula) processedForms.get(j);
                    if (f2.theFormula.indexOf('`') < 0) {
                        //System.out.println("INFO in STP2(): Clausifying: " + f2);
                        ArrayList<CNFFormula> cnfForms = clausifyFormula(f2);
                        formulas.addAll(cnfForms);
                    }
                    if (counter++ % 100 == 0) System.out.print(".");
                }               
            }
            System.out.println("\nINFO in STP2(): Done clausification in ");
            long t_elapsed = (System.currentTimeMillis() - t_start) / 1000;
            System.out.println(t_elapsed + " seconds.");            
        }
        buildIndexes();
    }    

    /** *************************************************************
     *  Backquoted clauses, signifying higher-order formulas, are
     *  rejected.
     */
    public STP2(Iterable<String> formulaSource) { 
    
        Iterator it = formulaSource.iterator();
        while (it.hasNext()) {
            String s = (String) it.next();
            if (!s.contains("`")) {
                Formula f = new Formula();
                f.read(s);
                ArrayList<CNFFormula> cnfForms = clausifyFormula(f);
                formulas.addAll(cnfForms);
            }
        }
        System.out.println("INFO in STP(): clausified formulas: " + formulas);
        buildIndexes();
    }
    
    /** *************************************************************
     */
    public static STP2 getNewInstance(String kbFileName) {

        STP2 res = null;
        try {
            res = new STP2(kbFileName);
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
            ArrayList<CNFFormula> support = (ArrayList) deductions.get(s);
            if (support != null) {
                for (int i = 0; i < support.size(); i++) {
                    CNFFormula f = (CNFFormula) support.get(i);
                    System.out.println(f);
                }
            }
            else {
                CNFFormula f = (CNFFormula) formulaMap.get(s);
                if (f != null) 
                    System.out.println(f.sourceFormula);                
            }
            System.out.println("-------------------");
        }
        System.out.println();
    }

    /** *************************************************************
     */
    private ArrayList<CNFFormula> clausifyFormula(Formula forig) {

        ArrayList<CNFFormula> newFormulas = new ArrayList();

        Formula fold = forig;
        //System.out.println("INFO in STP2.clausifyFormula(): to clausify: " + fold);
        //addToDeductions(null,null,fold,false);
        Formula f = Clausifier.clausify(fold);
        //Formula f = fold.clausify();
        if (f.car().equals("and")) {
            ArrayList<Formula> al = Clausifier.separateConjunctions(f);
            for (int i = 0; i < al.size(); i++) {
                Formula f2 = (Formula) al.get(i);
                f2 = Clausifier.toCanonicalClausalForm(f2);
                CNFFormula cnf = new CNFFormula(f2);
                newFormulas.add(cnf);
                //System.out.println("INFO in STP2.clausifyFormula(): after clausification: " + f2);
                addToDeductions(null,null,cnf);
            }
        }
        else {
            CNFFormula fnew = new CNFFormula(Clausifier.toCanonicalClausalForm(f));
            newFormulas.add(fnew);
            addToDeductions(null,null,fnew);
            //System.out.println("INFO in STP2.clausifyFormula(): after clausification: " + fnew);
        }        
        return newFormulas;
    }

    /** *************************************************************
     */
    private void indexOneFormula(CNFFormula f) {

        if (f == null || f.clauses.size() < 1) {
            System.out.println("Error in STP2.indexOneFormula(): null formula");
            return;
        }
        //System.out.println("INFO in STP2.indexOneFormula(): " + f);
        ArrayList<Integer> terms = f.collectTerms();                // count the appearances of terms
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

        f.setTermPointers(posTermPointers,negTermPointers);
    }

    /** *************************************************************
     */
    private void buildIndexes() {

        Iterator<CNFFormula> it = formulas.iterator();
        while (it.hasNext()) {
            CNFFormula f = (CNFFormula) it.next();
            indexOneFormula(f);
        }
       // System.out.println("INFO in STP2.buildIndexes(): negTermPointers: " + negTermPointers);
       // System.out.println("INFO in STP2.buildIndexes(): posTermPointers: " + posTermPointers);
    }

    /** *************************************************************
     *  Extract one clause from a CNF formula
     */
    private CNFClause extractRandomClause(CNFFormula f) {

        if (f == null || f.clauses.size() < 1) {
            System.out.println("Error in STP2.extractRandomClause(): bad formula: " + f);
            return null;
        }
        ArrayList<CNFClause> al = new ArrayList();
        al.addAll(f.clauses);        
        return (CNFClause) al.get(random.nextInt(f.clauses.size()));
    }

    /** *************************************************************
     *  Note that this class will sort into reverse order, with the
     *  largest integer values first
     */
    public class FormulaRating implements Comparable {

        public int intval = 0;
        public int age = 0;
        public CNFFormula form = null;

        public String toString() {
            return String.valueOf(intval) + "\n" + form.toString();
        }
        public boolean equals(FormulaRating avp) {
            return form.equals(avp.form);
        }
        public int hashCode() {
            return form.hashCode();
        }
        public int compareTo(Object avp) throws ClassCastException {

            if (!avp.getClass().getName().equalsIgnoreCase("com.articulate.sigma.STP2$FormulaRating")) 
                throw new ClassCastException("Error in FormulaRating.compareTo(): "
                                             + "Class cast exception for argument of class: " 
                                             + avp.getClass().getName());
            FormulaRating arg = (FormulaRating) avp;
            return (arg.intval + arg.age) - (intval + age);
        }
    }

    /** *************************************************************
     *  Collect Formulas, ranked by string length (better would be
     *  number of clauses)
     *  @param negated refers to whether the formula under
     *                 consideration is negated and therefore
     *                 indicates whether to select only those
     *                 Formulas in which the terms appear in negated
     *                 clauses, or vice versa
     */
    private ArrayList<FormulaRating> collectCandidates(ArrayList<Integer> terms, String negated) {

        if (_PROVE_DEBUG) System.out.println("INFO in STP2.collectCandidates(): " + terms);
        if (_PROVE_DEBUG) System.out.println("INFO in STP2.collectCandidates(): negated " + negated);
        TreeMap<CNFFormula, Integer> tm = new TreeMap();
        ArrayList result = new ArrayList();
        Iterator it = terms.iterator();
        while (it.hasNext()) {      // find formulas that have all the terms as the clause to be proven
            Integer term = (Integer) it.next();
            if (!CNFFormula.isLogicalOperator(term) && !CNFFormula.isVariable(term)) {
                ArrayList<CNFFormula> pointers = null;
                if (negated.equals("true")) 
                    pointers = (ArrayList) posTermPointers.get(term);
                else if (negated.equals("false")) 
                    pointers = (ArrayList) negTermPointers.get(term);
                else if (negated.equals("both")) {
                    pointers = (ArrayList) posTermPointers.get(term);
                    ArrayList<CNFFormula> morePointers = (ArrayList) negTermPointers.get(term);
                    if (pointers != null) {
                        if (morePointers != null) 
                            pointers.addAll(morePointers);                        
                    }
                    else
                        pointers = morePointers;
                    //System.out.println("INFO in STP2.collectCandidates(): pointers " + pointers);
                }
                else
                    System.out.println("Error in STP2.collectCandidates(): negated must be true, false or both " + negated);
                if (pointers != null) {
                    for (int i = 0; i < pointers.size(); i ++) {
                        CNFFormula f = (CNFFormula) pointers.get(i);
                        if (!tm.keySet().contains(f)) {
                            Integer count = new Integer(0);
                            tm.put(f,count);
                        }
                        Integer newCount = (Integer) tm.get(f);
                        newCount = new Integer(newCount.intValue() + 1);
                        tm.put(f,newCount);
                    }
                }
            }
        }

        it = tm.keySet().iterator();
        while (it.hasNext()) {      // find formulas ordered by the number of terms from the clause to be proven
            CNFFormula f = (CNFFormula) it.next();
            Integer num = (Integer) tm.get(f);
            FormulaRating avp = new FormulaRating();
            //avp.intval = num.intValue();
            avp.intval = 10000-f.size();   // sort by smallest size axiom is best (first)
            avp.form = f;
            result.add(avp);
        }
        Collections.sort(result);

        //System.out.println("INFO in STP2.collectCandidates(): result " + result);
        return result;
    }

    /** *************************************************************
     */
    private CNFFormula getRandomFormula() {

        return (CNFFormula) formulas.get(random.nextInt(formulas.size()));
    }

    /** *************************************************************
     *  Instantiate a formula, or, randomly, just the negation of a
     *  clause from the formula, or negate a ground formula
     */
    private CNFFormula findInstantiation(CNFFormula f) {

        if (_PROVE_DEBUG) System.out.println("\nINFO in STP2.prove(): checking instantiations:\n" + proofCounter);
        CNFFormula instantiationCandidate = new CNFFormula();
        proofCounter++;
        if (f.clauses.size() == 1) {
            if (!f.isGround()) {
                instantiationCandidate = f.instantiateVariables();                            
            }
            else {
                if (random.nextInt(10) == 1) {
                    CNFClause c = f.firstClause();
                    c.negated = !c.negated;
                    CNFFormula fnew = new CNFFormula();
                    fnew.addClause(c,f);
                    if (_PROVE_DEBUG) System.out.println("\nINFO in STP2.prove(): asserting ground formula (1):\n" + fnew);
                    return fnew;
                }
            }
        }
        if (_PROVE_DEBUG) System.out.println("\nINFO in STP2.prove(): f:\n" + f);
        if (_PROVE_DEBUG) System.out.println("\nINFO in STP2.prove(): instantiationCandidate:\n" + instantiationCandidate);
        if ((proofCounter > proofCounterInterval) && instantiationCandidate != null && 
            !instantiationCandidate.isGround()) {
            if (_PROVE_DEBUG) System.out.println("\nINFO in STP2.prove(): adding instantiated formula:\n" + instantiationCandidate);
            proofCounter = 0;
            return instantiationCandidate;
        }
        else if ((proofCounter > proofCounterInterval) && instantiationCandidate == null) {
            if (random.nextInt(2) == 0) {
                CNFClause clause = null;
                if (!f.isGround())
                    clause = extractRandomClause(f);
                if (clause != null && !clause.isGround(f.functions)) {
                    instantiationCandidate.addClause(clause.generateVariableValues(f.functions),f);
                    clause.negated = !clause.negated;
                    if (_PROVE_DEBUG) System.out.println("\nINFO in STP2.prove(): adding instantiated clause:\n" + instantiationCandidate);
                    proofCounter = 0;
                    return instantiationCandidate;
                }
                if (clause != null && clause.isGround(f.functions) && random.nextInt(10) == 0) {
                    clause.negated = !clause.negated;
                    CNFFormula fnew = new CNFFormula();
                    fnew.addClause(clause,f);
                    if (_PROVE_DEBUG) System.out.println("\nINFO in STP2.prove(): asserting ground formula (2):\n" + clause);
                    return fnew;
                }
            }
            else {
                if (!f.isGround()) {
                    instantiationCandidate = f.instantiateVariables();
                    if (_PROVE_DEBUG) System.out.println("\nINFO in STP2.prove(): adding instantiated candidate:\n" + instantiationCandidate);
                    proofCounter = 0;
                    return instantiationCandidate;
                }
            }
        }
        return null;
    }

    /** *************************************************************
     *  Record a single successful resolution, (or assertion used in
     *  the proof) indexed by conclusion.
     */
    private void addToDeductions(CNFFormula f1, CNFFormula f2, CNFFormula con) {

 //       if (f1 == null & f2 == null) 
 //           return;
        _PSUEDO_TIME++;
        STP2.deductionsMade++;
        CNFFormula newcon = con.deepCopy();
        newcon = newcon.normalizeVariables();
        CNFFormula f1new = null;
        CNFFormula f2new = null;
        ArrayList twoSupports = new ArrayList();
        if (f1 != null) {  
            f1new = f1.deepCopy();
            f1new = f1new.normalizeVariables();
            twoSupports.add(f1new);
            if (deductions.get(f1new.toString()) == null) 
                deductions.put(f1new.toString().trim(),null);
        }
        if (f2 != null) {
            f2new = f2.deepCopy();
            f2new = f2new.normalizeVariables();
            twoSupports.add(f2new);
            if (deductions.get(f2new.toString()) == null) 
                deductions.put(f2new.toString().trim(),null);
        }
        deductions.put(newcon.toString().trim(),twoSupports);
        System.out.println("INFO in STP2.addToDeductions()");
        System.out.println("conclusion: " + newcon);
        if (f1 != null)         
            System.out.println("premise 1: " + f1new);
        if (f2 != null)         
            System.out.println("premise 2: " + f2new);
    }

    /** *************************************************************
     *  Determine whether a formula in the knowledge base already
     *  subsumes the given formula
     *  @param form is a formula that might be subsumed by an
     *              existing formula
     *  @result true if the formula is subsumed, false otherwise
     */
    private boolean subsumedByKB(CNFFormula form) {

        ArrayList<CNFFormula> removeList = new ArrayList();
        if (_PROVE_DEBUG) System.out.println("INFO in STP2.subsumedByKB(): Checking if \n" + form + "\n would be subsumed by KB of" + 
                           formulas.size() + " entries");
        Iterator it = formulas.iterator();
        while (it.hasNext()) {
            CNFFormula kbForm = (CNFFormula) it.next();
            if (_PROVE_DEBUG) System.out.println("INFO in STP2.subsumedByKB(): Checking against\n" + kbForm);
            if (form.subsumedBy(kbForm)) {
                System.out.println("INFO in STP2.subsumedByKB(): new formula \n" + form + "\n is subsumed by KB formula\n" + kbForm);
                STP2.subsumptions++;
                return true;            
            }
            if (kbForm.subsumedBy(form)) {
                System.out.println("INFO in STP2.subsumedByKB(): KB formula \n" + kbForm + "\n is subsumed by new formula\n" + form);
                STP2.subsumptions++;
                removeList.add(kbForm);
            }
        }
        for (int i = 0; i < removeList.size(); i++) 
            formulas.remove((CNFFormula) removeList.get(i));
        return false;
    }

    /** *************************************************************
     *  Get a formula from the To Be Used collection
     */
    private FormulaRating getFromTBU() {

        if (random.nextInt(4) == 3) {
            int clauseNum = random.nextInt(TBU.size());
            if (_PROVE_DEBUG) System.out.println("\n\nINFO in STP2.getFromTBU(): getting random formula from TBU: \n" + 
                                                 TBU.get(clauseNum));
            return (FormulaRating) TBU.remove(clauseNum);
        }
        else
            return (FormulaRating) TBU.remove(0);
    }

    /** *************************************************************
     *  Find support for a formula
     *  @return an ArrayList of Formulas that consitute support for
     *          the clause.  Return an empty ArrayList if no proof
     *          is found.
     */
    private void prove(int secondsLimit) {

        boolean _BASIC_STATUS = false;

        System.out.println("INFO in STP2.prove(): timeout limit: " + secondsLimit);
        long t_start = System.currentTimeMillis();
        long t_elapsed = (System.currentTimeMillis() - t_start) / 1000;
        while (TBU.size() > 0) {
            //if (_PROVE_DEBUG) System.out.println("\n\nINFO in STP2.prove(): TBU: " + TBU);
            if (_PROVE_DEBUG) System.out.println("\nTBU length: " + TBU.size());
            //System.out.println("INFO in STP2.prove(): lemmas: " + lemmas);
            FormulaRating avp = getFromTBU();
            //if (!lemmas.containsKey(form)) {
            avp.form = avp.form.normalizeVariables();
            //System.out.println("INFO in STP2.prove(): attempting to prove: " + avp.form);
            CNFFormula f = avp.form.deepCopy();

            ArrayList<Integer> al = f.collectTerms();
            ArrayList<FormulaRating> candidates = null;
            if (f.clauses.size() == 1) {
                if (f.firstClause().negated)                 
                    candidates = collectCandidates(al,"true");                
                else
                    candidates = collectCandidates(al,"false");                
            }
            else
                candidates = collectCandidates(al,"both");                

            if (candidates != null && candidates.size() > 0) {
                for (int i = 0; i < candidates.size(); i++) {
                    FormulaRating avpCan = (FormulaRating) candidates.get(i);
                    CNFFormula candidate = avpCan.form;
                    if (_PROVE_DEBUG) System.out.println("INFO in STP2.prove(): checking candidate:\n" + candidate +
                                           "\n with formula: \n" + f);
                    CNFFormula resultForm = new CNFFormula();
                    TreeMap mappings = f.hyperResolve(candidate,resultForm);
                    STP2.resolutionsAttempted++;
                    if (resultForm != null && mappings != null && resultForm.empty()) {  // Successful resolution! Result is empty list
                        if (_PROVE_DEBUG || _BASIC_STATUS) System.out.println("\nINFO in STP2.prove(): sucessful resolution:" + resultForm + 
                                                             "\n from \n" + f + "\nand\n" + candidate);
                        if (_PROVE_DEBUG && resultForm.toString().equals("()")) System.out.println("\nINFO in STP2.prove(): valid empty set result");
                        addToDeductions(f,candidate,resultForm);
                        return;
                    }
                    if (mappings != null) {  // && mappings.keySet().size() > 0
                        if (_PROVE_DEBUG || _BASIC_STATUS) System.out.println("\nINFO in STP2.prove(): resolve result:\n" + resultForm);
                        if (_PROVE_DEBUG || _BASIC_STATUS) System.out.println("for candidate\n" + candidate + "\n with formula\n " + f);
                        FormulaRating avpNew = new FormulaRating();
                        if (!formulas.contains(resultForm)) {
                            resultForm.normalizeVariables();
                            avpNew.form = resultForm;
                            avpNew.intval = 10000-resultForm.size();
                            avpNew.age = _PSUEDO_TIME;
                            if (!TBU.contains(avpNew) && !subsumedByKB(resultForm)) {
                                if (_PROVE_DEBUG) System.out.println("\nINFO in STP2.prove(): adding result to TBU:\n" + resultForm);
                                addToDeductions(f,candidate,resultForm);
                                TBU.add(avpNew);
                                Collections.sort(TBU);
                            }

                        }
                    }
                    else {
                        //if (_PROVE_DEBUG) System.out.println("INFO in STP2.prove(): candidate that did not resolve:\n" + candidate +
                        //                   "\n with formula: \n" + f);      
                    }
                    t_elapsed = (System.currentTimeMillis() - t_start) / 1000;
                    if (t_elapsed > secondsLimit) {
                        System.out.println("INFO in STP2.prove(): Timeout before answer found (in checking candidate formulas.");
                        return;
                    }
                }
                if (_PROOF_GENERATION && (TBU.size() == 0 || t_elapsed > (secondsLimit * 0.75))) {
                    CNFFormula fnew = findInstantiation(f);
                    if (fnew != null && !formulas.contains(fnew)) {
                        FormulaRating avpNew = new FormulaRating();
                        avpNew.form = fnew;
                        avpNew.intval = 10000-fnew.size();
                        avpNew.age = _PSUEDO_TIME;
                        if (!TBU.contains(avpNew)) {
                            //addToDeductions(f,candidate,resultForm);
                            TBU.add(avpNew);
                            Collections.sort(TBU);
                        }
                    }
                }
            }
            indexOneFormula(f);       // all lemmas must be added to the knowledge base for completeness
            formulas.add(f); 
            if (STP2.resolutionsAttempted % 10 == 0) System.out.print(".");

            t_elapsed = (System.currentTimeMillis() - t_start) / 1000;
            System.out.println("INFO in STP2.prove(): t_elapsed: " + t_elapsed);
            if (t_elapsed > secondsLimit) {
                System.out.println("INFO in STP2.prove(): Timeout before answer found (in TBU loop).");
                return;
            }
        }
    }

    /** *************************************************************
     *  @param f is the formula of the proof step to be printed
     *  @param formIds are all the formulas used in the proof
     *  @param alreadyPrinted is a list of the Integer ids (the
     *                        index from the formIds ArrayList) of
     *                        formulas that have already been
     *                        printed in the proof.
     */
    private String formatResultRecurse (CNFFormula f, ArrayList<String> formIds, ArrayList<Integer> alreadyPrinted) {

        if (alreadyPrinted.contains(formIds.indexOf(f.toString().trim()))) 
            return null;
        alreadyPrinted.add(new Integer(formIds.indexOf(f.toString().trim())));
        //System.out.println("INFO in STP2.formatResultRecurse(): alreadyPrinted: " + alreadyPrinted);
        //System.out.println("INFO in STP2.formatResultRecurse(): checking formula: " + f);
        //System.out.println("INFO in STP2.formatResultRecurse(): this formula id: " + formIds.indexOf(f.toString().trim()));

        StringBuffer result = new StringBuffer();
        ArrayList<CNFFormula> support = (ArrayList) deductions.get(f.toString().trim());
        //System.out.println("INFO in STP2.formatResultRecurse(): support: " + support);
        if (support != null && support.size() > 0) {
            CNFFormula f1 = (CNFFormula) support.get(0);
            //System.out.println("INFO in STP2.formatResultRecurse(): internal checking formula: " + f1);
            CNFFormula f2 = null;
            if (support.size() > 1) {
                f2 = (CNFFormula) support.get(1);
                //System.out.println("INFO in STP2.formatResultRecurse(): internal checking formula 2: " + f2);
            }
            //System.out.println("INFO in STP2.formatResultRecurse(): recursing on formula 1: " + f1);
            String f1result = formatResultRecurse(f1,formIds,alreadyPrinted);
            if (f1result != null) 
                result.append(f1result);
            if (f2 != null) {
                //System.out.println("INFO in STP2.formatResultRecurse(): recursing on formula 2: " + f2);
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
    private TreeMap<String, ArrayList<CNFFormula>> getDeductionsRecurse (CNFFormula form) {
         
        TreeMap<String, ArrayList<CNFFormula>> result = new TreeMap();
        ArrayList<CNFFormula> support = (ArrayList) deductions.get(form.toString().trim());
        //System.out.println("INFO in STP2.getDeductionsRecurse(): support: " + support);
        if (support != null && support.size() > 0) {
            CNFFormula f1 = (CNFFormula) support.get(0);
            //System.out.println("INFO in STP2.getDeductionsRecurse(): internal checking formula: " + f1);
            CNFFormula f2 = null;
            if (support.size() > 1) {
                f2 = (CNFFormula) support.get(1);
                result.put(form.toString().trim(),support);
                //System.out.println("INFO in STP2.getDeductionsRecurse(): internal checking formula 2: " + f2);
            }
            if (f1 != null && !f1.toString().equals("()")) {
                //System.out.println("INFO in STP2.getDeductionsRecurse(): recursing on formula 1: " + f1);
                result.putAll(getDeductionsRecurse(f1));
            }
            if (f2 != null && !f2.toString().equals("()")) {
                //System.out.println("INFO in STP2.getDeductionsRecurse(): recursing on formula 2: " + f2);
                result.putAll(getDeductionsRecurse(f2));
            }
        }
        //System.out.println("INFO in STP2.getDeductionsRecurse(): result: " + result);
        return result;
    }

    /** *************************************************************
     */
    private TreeMap<Integer,Integer> remap (TreeMap<Integer,Integer> map,
                                            TreeMap<Integer,Integer> newMap) {

        //System.out.println("INFO in STP2.remap(): map\n" + map);
        //System.out.println("INFO in STP2.remap(): newMap\n" + newMap);
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

        //System.out.println("INFO in STP2.remap(): \n" + result);
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
    private TreeMap<Integer,Integer> extractAnswerVarsRecurse (CNFFormula form,
                                                               TreeMap<String, ArrayList<CNFFormula>> relevantDeductions) {

        //System.out.println("INFO in STP2.extractAnswerVarsRecurse(): checking formula: \n" + form);
        TreeMap<Integer,Integer> result = new TreeMap();
        ArrayList<Integer> vars = form.collectVariables();
        //System.out.println("INFO in STP2.extractAnswerVarsRecurse(): variables: \n" + vars);
        Iterator it = relevantDeductions.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            CNFFormula nextForm = null;
            ArrayList<CNFFormula> value = (ArrayList<CNFFormula>) relevantDeductions.get(key);
            if (value != null && value.size() > 1) {
                TreeMap map = null;
                CNFFormula resolveResult = new CNFFormula();
                CNFFormula f1 = (CNFFormula) value.get(0);
                CNFFormula f2 = (CNFFormula) value.get(1);
                if (f1.equals(form)) {
                    //System.out.println("INFO in STP2.extractAnswerVarsRecurse(): form (argNew): \n" + form);
                    //System.out.println("INFO in STP2.extractAnswerVarsRecurse(): f1: \n" + f1);
                    //System.out.println("INFO in STP2.extractAnswerVarsRecurse(): f2: \n" + f2);
                    CNFFormula f2new = form.unifyVariableScope(f2);
                    //System.out.println("INFO in STP2.extractAnswerVarsRecurse(): f2new: \n" + f2new);
                    nextForm = f2new.deepCopy();
                    //System.out.println("INFO in STP2.extractAnswerVarsRecurse(): nextForm: \n" + nextForm);
                    map = form.hyperResolve(f2new,resolveResult);
                }
                else if (f2.equals(form)) {    
                    //System.out.println("INFO in STP2.extractAnswerVarsRecurse(): form (argNew): \n" + form);
                    //System.out.println("INFO in STP2.extractAnswerVarsRecurse(): f1: \n" + f1);
                    //System.out.println("INFO in STP2.extractAnswerVarsRecurse(): f2: \n" + f2);
                    CNFFormula f1new = form.unifyVariableScope(f1);
                    //System.out.println("INFO in STP2.extractAnswerVarsRecurse(): f1new: \n" + f1new);
                    nextForm = f1new.deepCopy();
                    map = form.hyperResolve(f1new,resolveResult);
                }
                //System.out.println("INFO in STP2.extractAnswerVarsRecurse(): map: \n" + map);
                if (map != null && map.keySet().size() > 0) {
                    boolean allConstants = true;
                    Iterator it2 = vars.iterator();
                    while (it2.hasNext()) {
                        Integer variable = (Integer) it2.next();
                        Integer varValue = (Integer) map.get(variable);
                        if (varValue != null) {
                            result.put(variable,varValue);
                            if (!CNFClause.isConstant(varValue)) 
                                allConstants = false;
                        }
                    }
                    //System.out.println("INFO in STP2.extractAnswerVarsRecurse(): " + result);
                    if (allConstants) {
                        //System.out.println("INFO in STP2.extractAnswerVarsRecurse(): all constants");
                        //System.out.println("INFO in STP2.extractAnswerVarsRecurse(): result: \n" + result);
                        return result;
                    }
                    else {
                        TreeMap<Integer,Integer> newResult = new TreeMap();
                        //System.out.println("INFO in STP2.extractAnswerVarsRecurse(): nextForm(2): \n" + nextForm);
                        newResult = extractAnswerVarsRecurse(resolveResult,relevantDeductions);
                        //System.out.println("INFO in STP2.extractAnswerVarsRecurse(): newResult " + newResult);
                        return remap(result,newResult);
                    }
                }
            }
        }

        //System.out.println("INFO in STP2.extractAnswerVarsRecurse() 2: " + result);
        return result;
    }

    /** *************************************************************
     *  Create a map of bindings for answer variables
     * 
     *  @return a map of variables that appear in the formula to the
     *          values they get in the proof.
     */
    private TreeMap<Integer,Integer> extractAnswerVars (CNFFormula form) {

        CNFFormula f = new CNFFormula();
        f.read("()");
        TreeMap<String, ArrayList<CNFFormula>> relevantDeductions = getDeductionsRecurse(f);
        return extractAnswerVarsRecurse(form,relevantDeductions);
    }

    /** *************************************************************
     *  Create XML markup for reporting answer variables
     */
    private String formatVariableBindings (TreeMap<Integer,Integer> valueMap) {

        StringBuffer result = new StringBuffer();

        //System.out.println("INFO in STP2.formatVariableBindings(): " + valueMap);
        Iterator it = valueMap.keySet().iterator();
        while (it.hasNext()) {
            Integer key = (Integer) it.next();
            if (!CNFFormula.isVariable(key)) {
                System.out.println("Error in STP2.formatVariableBindings(): key is not variable: " + key);
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
                    binding = (String) CNFFormula.intToTermMap.get(value);
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

        CNFFormula fQuery = new CNFFormula();
        fQuery.read(query);
        boolean ground = false;
        if (fQuery.isGround()) 
            ground = true;        
        //System.out.println("INFO in STP2.formatResultNew(): deduction keys " + deductions.keySet());
        //printDeductions();
        ArrayList<Integer> alreadyPrinted = new ArrayList();
        ArrayList<String> formIds = new ArrayList();
        formIds.addAll(deductions.keySet());

        StringBuffer result = new StringBuffer();
        if (!deductions.containsKey("()")) {
            System.out.println("INFO in STP2.formatResultNew(): no empty clause in result");
            result.append("<queryResponse>\n<answer result='no' number='0'>\n</answer>\n</queryResponse>\n");
        }
        else {
            System.out.println("INFO in STP2.formatResultNew(): successful resolution: empty clause in result");
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
            CNFFormula f = new CNFFormula();
            f.read("()");
            result.append(formatResultRecurse(f,formIds,alreadyPrinted));
            result.append("</proof>\n</answer>\n<summary proofs='1'/>\n</queryResponse>\n");
        }
        //System.out.println("INFO in STP2.formatResult(): " + result.toString());
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

        System.out.println("INFO in STP2.submitQuery(): query: " + formula);
        System.out.println("INFO in STP2.submitQuery(): formulas: " + formulas.size());
        //System.out.println("INFO in STP2.submitQuery(): formulas: " + formulas);
        System.out.println("INFO in STP2.submitQuery(): deductions: " + deductions.keySet().size());
        ArrayList result = new ArrayList();
        Formula negQuery = new Formula();
        Formula rawNegQuery = new Formula();
        rawNegQuery.read("(not " + formula + ")");
        negQuery.read("(not " + formula + ")");
        negQuery = Clausifier.clausify(negQuery);     // negation will be pushed in
        if (negQuery.equals(rawNegQuery)) {
            CNFFormula cnf = new CNFFormula();
            cnf.read(negQuery.theFormula);
            addToDeductions(null,null,cnf);
        }
        else {
            CNFFormula originalQuery = new CNFFormula();
            originalQuery.read("(not " + formula + ")");
            CNFFormula cnf = new CNFFormula();
            cnf.read(negQuery.theFormula);
            addToDeductions(originalQuery,null,cnf);
        }
        System.out.println("INFO in STP2.submitQuery(): clausified query: " + negQuery);
        FormulaRating avp = null;
        if (negQuery.car().equals("and")) {

            System.out.println("INFO in STP2.submitQuery(): conjunctive query, ignore previous non-CNF errors.");
            ArrayList<Formula> al = Clausifier.separateConjunctions(negQuery);
            for (int i = 0; i < al.size(); i++) {
                Formula f2 = (Formula) al.get(i);
                avp = new FormulaRating();
                avp.form = new CNFFormula(f2);
                avp.intval = avp.form.size();
                avp.age = _PSUEDO_TIME;
                TBU.add(avp);
                Collections.sort(TBU);
                //System.out.println("INFO in STP2.submitQuery(): adding to TBU: " + avp);
            }
        }
        else {
            avp = new FormulaRating();
            avp.form = new CNFFormula(negQuery);
            avp.intval = avp.form.size();
            avp.age = _PSUEDO_TIME;
            TBU.add(avp);
            Collections.sort(TBU);
        }

        long t_start = System.currentTimeMillis();
        // ArrayList<CNFFormula> res = prove(timeLimit); 
        prove(timeLimit); 
        //System.out.println("INFO in STP2.submitQuery(): deductions: \n" + deductions);
        // return res.toString();
        System.out.println("=============================");
        long t_elapsed = (System.currentTimeMillis() - t_start) / 1000;
        System.out.println(t_elapsed + " seconds. " + timeLimit + " seconds time limit.");            
        System.out.println("INFO in STP2.submitQuery(): deductions made:       " + STP2.deductionsMade);
        System.out.println("INFO in STP2.submitQuery(): resolutions attempted: " + STP2.resolutionsAttempted);
        System.out.println("INFO in STP2.submitQuery(): subsumptions:          " + STP2.subsumptions);
        System.out.println("=============================");
        String XMLresult = formatResultNew(negQuery.theFormula);
        System.out.println("INFO in STP2.submitQuery(): result: \n" + XMLresult);
        return XMLresult;
    }

    /** *************************************************************
     *  Removes all formulas added by previous deductions.
     */
    public void clear() {

        System.out.println("INFO in STP2.clear()");
        deductions = new TreeMap();
        TBU = new ArrayList();
        negLits = new TreeMap();  // Note that (not (a b c)) will be stored as (a b c)
        posLits = new TreeMap();  // Appearance of positive clauses
        termCounts = new TreeMap();    // Key is term index, value is the count of appearances
        posTermPointers = new TreeMap(); // appearance of a term in a positive literal
        negTermPointers = new TreeMap(); // appearance of a term in a negative literal
        ArrayList<CNFFormula> newFormulas = new ArrayList();
        TreeMap<String, CNFFormula> newFormulaMap = new TreeMap();
        Iterator it = formulas.iterator();
        while (it.hasNext()) {
            CNFFormula cnf = (CNFFormula) it.next();
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
        ArrayList<CNFFormula> cnfForms = clausifyFormula(f);
        if (cnfForms == null) 
            return ("<assertionResponse>Error in clausification</assertionResponse>");        
        for (int i = 0; i < cnfForms.size(); i++) {
            CNFFormula cnf = (CNFFormula) cnfForms.get(i);
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
        STP2 stp = null;
        try {
            stp = new STP2("/home/apease/Sigma/KBs/Merge.kif");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        stp.random = new Random(2);
        stp._PROOF_GENERATION = true;
        CNFFormula f = stp.getRandomFormula();
        System.out.println("---------------------------------------");
        System.out.println("INFO in STP2.generateTestProblem(): Starting generation from query: " + f);
        stp.submitQuery(f.toString(),60,0); 
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
        CNFFormula query = new CNFFormula();
        // query.readNonCNF("(exists (?MEMBER) (m ?MEMBER Org1-1))");
        query.readNonCNF("(m ?MEMBER Org1-1)");
        STP2 stp2 = new STP2(al);
        System.out.println(stp2.submitQuery(query.toString(),10,0)); 

        /*
(or
  (not (instance ?X3 Collection))
  (member (SkFn 1 ?X3) ?X3)), 

(subclass Organization Collection), 

(or
  (instance ?X6 SetOrClass)
  (not (subclass ?X7 ?X6))), 

(or
  (instance ?X8 SetOrClass)
  (not (subclass ?X8 ?X9))), 

(or
  (not (instance ?X13 SetOrClass))
  (not (instance ?X14 SetOrClass))
  (not (subclass ?X13 ?X14))
  (not (instance ?X15 ?X13))
  (instance ?X15 ?X14))

query:
(not (member ?X33 Org1-1))

(not (instance Org1-1 Collection))

(or
  (not (instance ?X13 SetOrClass))
  (not (instance Collection SetOrClass))
  (not (subclass ?X13 Collection))
  (not (instance Org1-1 ?X13)))
        */
    }

    /** *************************************************************
     */
    public static void tq1back() {

        ArrayList<String> al = new ArrayList();
        CNFFormula query = new CNFFormula();
        query.readNonCNF("(member ?MEMBER Org1-1)");
        STP2 stp = null;
        try {
            stp = new STP2("/home/apease/Sigma/KBs/tqBackground.kif");
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
        CNFFormula query = new CNFFormula();
        query.readNonCNF("(member ?MEMBER Org1-1)");
        STP2 stp = null;
        try {
            stp = new STP2("/home/apease/Sigma/KBs/Merge.kif");
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
    public static void tq1() {

        ArrayList<String> al = new ArrayList();
        al.add("(=> (instance ?X290 Collection) (exists (?X12) (member ?X12 ?X290)))");
        al.add("(subclass Organization Collection)");
        al.add("(instance SetOrClass SetOrClass)");
        al.add("(instance Org1-1 Organization)");
        al.add("(=> (subclass ?X403 ?X404) (and (instance ?X403 SetOrClass) (instance ?X404 SetOrClass)))");
        al.add("(=> (and (instance ?X403 SetOrClass) (instance ?X404 SetOrClass)) " +
               "(=> (and (subclass ?X403 ?X404) (instance ?X405 ?X403)) (instance ?X405 ?X404)))");
        CNFFormula query = new CNFFormula();
        query.readNonCNF("(member ?MEMBER Org1-1)");
        STP2 stp = new STP2(al);
        stp.submitQuery(query.sourceFormula.theFormula,60,0); 
    }

    /** *************************************************************
     */
    public static void tq2() {

        ArrayList al = new ArrayList();
        al.add("(=> (p ?X) (q ?X))");
        al.add("(=> (or (q ?X) (r ?X)) (t ?X))");
        al.add("(p a)");
        CNFFormula query = new CNFFormula();
        query.read("(or (t a) (r a))");
        STP2 stp = new STP2(al);
        System.out.println("INFO in STP2.tq2()");
        stp.submitQuery(query.sourceFormula.theFormula,30,0); 
    }

    /** *************************************************************
     */
    public static void tq12back() {

        ArrayList<String> al = new ArrayList();
        CNFFormula query = new CNFFormula();
        query.readNonCNF("(instance Organism12-1 Organism)");
        STP2 stp = null;
        try {
            stp = new STP2("/home/apease/Sigma/KBs/tqBackground.kif");
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
        CNFFormula query = new CNFFormula();
        query.readNonCNF("(and (not (or a b)) (or a (not b)) (or (not a) b) (or (not a) (not b)))");
        STP2 stp = new STP2(al);
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
        CNFFormula query = new CNFFormula();
        query.read("(attribute ?X Criminal)");

        System.out.println("********************************************");
        Iterator<String> it = al.iterator();
        while (it.hasNext()) {
            String s = (String) it.next();
            Formula f = new Formula();
            f.read(s);
            System.out.println(f);
        }
        STP2 stp = new STP2(al);
        System.out.println("Query: " + query.sourceFormula); 
        System.out.println("Result: ");
        stp.submitQuery(query.sourceFormula.theFormula,10,0); 
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
        //tq12back();
        //generateTestProblem();
        //tq2();
        // factorizationTest();
        rnTest();
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

