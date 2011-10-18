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
public class STP extends InferenceEngine {

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
    ArrayList<Formula> formulas = new ArrayList();

    /** Previously solved lemmas that is used to save the proof */
    TreeMap<String, ArrayList<Formula>> lemmas = new TreeMap();

    /** individual deduction steps, indexed by conclusion */
    TreeMap<String, ArrayList<Formula>> deductions = new TreeMap();

    /** To Be Used - also has a list of axioms used to derive the
     *  clause. */
    ArrayList<AnotherAVP> TBU = new ArrayList();

    //TreeSet<String> current = new TreeSet();
    //TreeSet<String> failed = new TreeSet();

    /** The indexes */
    TreeMap<String, Formula> negLits = new TreeMap();  // Note that (not (a b c)) will be stored as (a b c)
    TreeMap<String, Formula> posLits = new TreeMap();  // Appearance of positive clauses
    TreeMap<String, Integer> termCounts = new TreeMap();
    TreeMap<String, ArrayList<Formula>> posTermPointers = new TreeMap(); // appearance of a term in a positive literal
    TreeMap<String, ArrayList<Formula>> negTermPointers = new TreeMap(); // appearance of a term in a negative literal

    /** ***************************************************************
     * Convert to a String.
     */
    public String toString() {

        return "An STP instance";
    }

    /** ***************************************************************
     */
    public static class STPEngineFactory extends EngineFactory {

        @Override
        public InferenceEngine createWithFormulas(Iterable<String> formulaSource) {  
            return new STP(formulaSource);
        }

        @Override
        public InferenceEngine createFromKBFile(String kbFileName) {
            return STP.getNewInstance(kbFileName);
        }
    }

    public static EngineFactory getFactory() {
        return new STPEngineFactory();
    }

    /** *************************************************************
     */
    private STP(String kbFileName) throws Exception {
    
        String error = null;
               
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
            KIF kif = new KIF();
            kif.readFile(kbFileName);

            Iterator it = kif.formulas.values().iterator();
            while (it.hasNext()) {
                ArrayList<Formula> al = (ArrayList) it.next();
                formulas.addAll(al);
            }
        }
        
        clausifyFormulas();
        buildIndexes();
    }    

    /** *************************************************************
     */
    public STP(Iterable<String> formulaSource) { 
    
        Iterator it = formulaSource.iterator();
        while (it.hasNext()) {
            String s = (String) it.next();
            Formula f = new Formula();
            f.read(s);
            formulas.add(f);
        }

        clausifyFormulas();
        // System.out.println("INFO in STP(): clausified formulas: " + formulas);
        buildIndexes();
    }
    
    /** *************************************************************
     */
    public static STP getNewInstance(String kbFileName) {

        STP res = null;
        try {
            res = new STP(kbFileName);
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
            ArrayList<Formula> support = (ArrayList) deductions.get(s);
            if (support != null) {
                for (int i = 0; i < support.size(); i++) {
                    Formula f = (Formula) support.get(i);
                    System.out.println(f);
                }
            }
            System.out.println("-------------------");
        }
        System.out.println();
    }

    /** *************************************************************
     */
    private void clausifyFormulas() {

        ArrayList<Formula> newFormulas = new ArrayList();
        Iterator<Formula> it = formulas.iterator();
        while (it.hasNext()) {
            Formula fold = (Formula) it.next();
            // System.out.println("INFO in STP.clausifyFormulas(): to clausify: " + fold);
            addToDeductions(null,null,fold,false);
            Formula f = Clausifier.clausify(fold);
            if (f.car().equals("and")) {
                ArrayList<Formula> al = Clausifier.separateConjunctions(f);
                for (int i = 0; i < al.size(); i++) {
                    Formula f2 = (Formula) al.get(i);
                    f2 = Clausifier.toCanonicalClausalForm(f2);
                    newFormulas.add(f2);
                    ArrayList support = new ArrayList();
                    support.add(fold);
                    lemmas.put(f2.theFormula,support);
                    // System.out.println("INFO in STP.clausifyFormulas(): after clausification: " + f2);
                    addToDeductions(fold,null,f2,false);
                }
            }
            else {
                ArrayList support = new ArrayList();
                support.add(fold);
                Formula fnew = Clausifier.toCanonicalClausalForm(f);
                lemmas.put(fnew.theFormula,support);
                newFormulas.add(f);
                if (fold.theFormula.equals(fnew.theFormula)) 
                    addToDeductions(null,null,fnew,false);         // clausification didn't change the formula
                else {
                    addToDeductions(fold,null,fnew,false);
                    // System.out.println("INFO in STP.clausifyFormulas(): added clausify deduction: " + fold + " : " + fnew);
                }
                // System.out.println("INFO in STP.clausifyFormulas(): after clausification: " + fnew);
            }
        }
        formulas = newFormulas;
    }

    /** *************************************************************
     *  Add a new term pointer to either posTermPointers or
     *  negTermPointers
     */
    private void addToPointers(TreeMap<String, ArrayList<Formula>> pointers, String clause, Formula f) {

        ArrayList<Formula> al = null;
        if (pointers.get(clause) == null) {
            al = new ArrayList();
            pointers.put(clause,al);
        }
        else
            al = (ArrayList) pointers.get(clause);
        al.add(f);
    }
    
    /** *************************************************************
     *  Side effect: Add pointers to f in the negTermPointers Map
     *  for the terms occurring in clause
     */
    private void addNegTermPointers(String clause, Formula f) {

        Formula c = new Formula();
        c.read(clause);
        while (!c.empty()) {
            String car = c.car();
            if (Formula.atom(car)) {
                if (!Formula.isVariable(car) && !Formula.LOGICAL_OPERATORS.contains(car)) 
                    addToPointers(negTermPointers,car,f);            
            }
            else 
                addNegTermPointers(car,f);
            c.read(c.cdr());
        }
    }
    
    /** *************************************************************
     *  Side effect: Add pointers to f in the posTermPointers Map
     *  for the terms occurring in clause
     */
    private void addPosTermPointers(String clause, Formula f) {

        Formula c = new Formula();
        c.read(clause);
        while (!c.empty()) {
            String car = c.car();
            if (Formula.atom(car)) {
                if (!Formula.isVariable(car) && !Formula.LOGICAL_OPERATORS.contains(car)) 
                    addToPointers(posTermPointers,car,f);            
            }
            else 
                addPosTermPointers(car,f);            
            c.read(c.cdr());
        }
    }


    /** *************************************************************
     */
    private void indexOneFormula(Formula f) {

        if (f == null || f.theFormula == null || f.theFormula == "") {
            System.out.println("Error in STP.indexOneFormula(): null formula");
            return;
        }
        if (f.theFormula.contains("`")) {
            return;
        }
        //System.out.println("INFO in STP.indexOneFormula(): " + f);
        ArrayList<String> terms = f.collectTerms();                // count the appearances of terms
        Iterator it2 = terms.iterator();
        while (it2.hasNext()) {
            String s = (String) it2.next();
            if (termCounts.keySet().contains(s)) {
                Integer i = (Integer) termCounts.get(s);
                termCounts.put(s,new Integer(i.intValue() + 1));
            }
            else
                termCounts.put(s,new Integer(1));
        }

        Formula fclause = Clausifier.clausify(f);
        ArrayList<Formula> clauseList = new ArrayList();
        if (fclause.car().equals("and"))
            clauseList = Clausifier.separateConjunctions(fclause);
        else
            clauseList.add(fclause);                  // a formula that is not a conjunction will result in a clauseList of one element
        for (int i = 0; i < clauseList.size(); i++) {
            fclause = (Formula) clauseList.get(i);
            Formula clauses = new Formula();
            if (fclause.isSimpleClause() || fclause.isSimpleNegatedClause()) 
                clauses.read(fclause.theFormula);
            else
                clauses.read(fclause.cdr());  // should remove the initial "or"
            while (!clauses.empty()) {
                //System.out.println("INFO in STP.buildIndexes(): clauses: " + clauses);
                String clause = null;
                if (!clauses.isSimpleClause() && !clauses.isSimpleNegatedClause()) {
                    clause = clauses.car();
                }
                else {
                    clause = clauses.theFormula;
                    clauses.theFormula = "()";
                }
                clause = Clausifier.normalizeVariables(clause);
                Formula c = new Formula();
                c.read(clause);
                String negP = c.car();
                if (negP.equals("not")) {        // note that if there are multiple such formulas only one will be kept
                    negLits.put(c.cdr(),f);
                    addNegTermPointers(c.cdr(),f);
                }
                else {
                    posLits.put(clause,f);
                    addPosTermPointers(clause,f);
                }
                if (!clauses.empty())                 
                    clauses.read(clauses.cdr());
            }
        }
    }

    /** *************************************************************
     */
    private void buildIndexes() {

        Iterator<Formula> it = formulas.iterator();
        while (it.hasNext()) {
            Formula f = (Formula) it.next();
            indexOneFormula(f);
        }
       // System.out.println("INFO in STP.buildIndexes(): negTermPointers: " + negTermPointers);
       // System.out.println("INFO in STP.buildIndexes(): posTermPointers: " + posTermPointers);
    }

    /** *************************************************************
     *  Extract one clause from a CNF formula
     */
    private Formula extractRandomClause(Formula f) {

        if (f == null || f.empty() || f.isSimpleClause() || f.isSimpleNegatedClause() || !f.car().equals("or")) {
            System.out.println("Error in STP.extractRandomClause(): bad formula: " + f);
            return null;
        }
        Formula fnew = new Formula();
        fnew.read(f.cdr());
        ArrayList<String> clauses = new ArrayList();
        while (!fnew.empty()) {
            clauses.add(fnew.car());
            fnew.read(fnew.cdr());
        }
        fnew.read((String) clauses.get(random.nextInt(clauses.size())));
        return fnew;
    }

    /** *************************************************************
     *  Note that this class will sort into reverse order, with the
     *  largest integer values first
     */
    public class AnotherAVP implements Comparable {

        public int intval = 0;
        public Formula form = null;

        public String toString() {
            return String.valueOf(intval) + "\n" + form.toString();
        }
        public boolean equals(Object avp_obj) {
            
            assert !avp_obj.getClass().getName().equals("AnotherAVP") : "AnotherAVP.equals() passed object not of type AnotherAVP"; 
            AnotherAVP avp = (AnotherAVP) avp_obj;
            return form.equals(avp.form);
        }
        public int hashCode() {
            return form.hashCode();
        }
        public int compareTo(Object avp) throws ClassCastException {

            if (!avp.getClass().getName().equalsIgnoreCase("com.articulate.sigma.STP$AnotherAVP")) 
                throw new ClassCastException("Error in AnotherAVP.compareTo(): "
                                             + "Class cast exception for argument of class: " 
                                             + avp.getClass().getName());
            return ((AnotherAVP) avp).intval - intval;
        }
    }

    /** *************************************************************
     *  Collect Formulas, ranked by string length (better would be
     *  number of clauses)
     *  @param negated indicates whether to select only those
     *                 Formulas in which the terms appear in negated
     *                 clauses, or vice versa
     */
    private ArrayList<AnotherAVP> collectCandidates(ArrayList<String> terms, String negated) {

        //System.out.println("INFO in STP.collectCandidates(): " + terms);
        //System.out.println("INFO in STP.collectCandidates(): negated " + negated);
        TreeMap<Formula, Integer> tm = new TreeMap();
        ArrayList result = new ArrayList();
        Iterator it = terms.iterator();
        while (it.hasNext()) {      // find formulas that have all the terms as the clause to be proven
            String term = (String) it.next();
            if (!Formula.LOGICAL_OPERATORS.contains(term) && !Formula.isVariable(term)) {
                ArrayList<Formula> pointers = null;
                if (negated.equals("true")) 
                    pointers = (ArrayList) posTermPointers.get(term);
                else if (negated.equals("false")) 
                    pointers = (ArrayList) negTermPointers.get(term);
                else if (negated.equals("both")) {
                    pointers = (ArrayList) posTermPointers.get(term);
                    ArrayList<Formula> morePointers = (ArrayList) negTermPointers.get(term);
                    if (pointers != null) {
                        if (morePointers != null) 
                            pointers.addAll(morePointers);                        
                    }
                    else
                        pointers = morePointers;
                    //System.out.println("INFO in STP.collectCandidates(): pointers " + pointers);
                }
                else
                    System.out.println("Error in STP.collectCandidates(): negated must be true, false or both " + negated);
                if (pointers != null) {
                    for (int i = 0; i < pointers.size(); i ++) {
                        Formula f = (Formula) pointers.get(i);
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
            Formula f = (Formula) it.next();
            Integer num = (Integer) tm.get(f);
            AnotherAVP avp = new AnotherAVP();
            //avp.intval = num.intValue();
            avp.intval = 10000-f.theFormula.length();   // sort by smallest size axiom is best (first)
            avp.form = f;
            result.add(avp);
        }
        Collections.sort(result);

        //System.out.println("INFO in STP.collectCandidates(): result " + result);
        return result;
    }


    /** *************************************************************
     *  Instantiate a formula, or, randomly, just the negation of a
     *  clause from the formula, or negate a ground formula
     */
    private Formula findInstantiation(Formula f) {

        if (_PROVE_DEBUG) System.out.println("\nINFO in STP.prove(): checking instantiations:\n" + proofCounter);
        Formula instantiationCandidate = null;
        proofCounter++;
        if (f.isSimpleClause() || f.isSimpleNegatedClause()) {
            if (!f.isGround()) {
                instantiationCandidate = f.instantiateVariables();                            
            }
            else {
                if (random.nextInt(10) == 1) {
                    Formula fnew = new Formula();
                    if (f.isSimpleClause()) 
                        fnew.read("(not " + f.theFormula + ")");
                    else {      // formula is negated
                        fnew.read(f.cdr());
                        fnew.read(fnew.car());
                    }
                    if (_PROVE_DEBUG) System.out.println("\nINFO in STP.prove(): asserting ground formula (1):\n" + fnew);
                    return fnew;
                }
            }
        }
        if (_PROVE_DEBUG) System.out.println("\nINFO in STP.prove(): f:\n" + f);
        if (_PROVE_DEBUG) System.out.println("\nINFO in STP.prove(): instantiationCandidate:\n" + instantiationCandidate);
        if ((proofCounter > proofCounterInterval) && instantiationCandidate != null && 
            !instantiationCandidate.isGround()) {
            if (_PROVE_DEBUG) System.out.println("\nINFO in STP.prove(): adding instantiated formula:\n" + instantiationCandidate);
            proofCounter = 0;
            return instantiationCandidate;
        }
        else if ((proofCounter > proofCounterInterval) && instantiationCandidate == null) {
            if (random.nextInt(2) == 0) {
                Formula clause = null;
                if (!f.isGround())
                    clause = extractRandomClause(f);
                if (clause != null && !clause.isGround()) {
                    instantiationCandidate = clause.instantiateVariables();
                    if (!clause.car().equals("not")) 
                        instantiationCandidate.read("(not " + instantiationCandidate.theFormula + ")");
                    else {
                        instantiationCandidate.read(instantiationCandidate.cdr());
                        instantiationCandidate.read(instantiationCandidate.car());
                    }
                    if (_PROVE_DEBUG) System.out.println("\nINFO in STP.prove(): adding instantiated clause:\n" + instantiationCandidate);
                    proofCounter = 0;
                    return instantiationCandidate;
                }
                if (clause != null && clause.isGround() && random.nextInt(10) == 0) {
                    Formula fnew = new Formula();
                    if (clause.isSimpleClause()) 
                        fnew.read("(not " + clause.theFormula + ")");
                    else {      // formula is negated
                        fnew.read(clause.cdr());
                        fnew.read(fnew.car());
                    }
                    if (_PROVE_DEBUG) System.out.println("\nINFO in STP.prove(): asserting ground formula (2):\n" + fnew);
                    return fnew;
                }
            }
            else {
                if (!f.isGround()) {
                    instantiationCandidate = f.instantiateVariables();
                    if (_PROVE_DEBUG) System.out.println("\nINFO in STP.prove(): adding instantiated candidate:\n" + instantiationCandidate);
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
    private void addToDeductions(Formula f1, Formula f2, Formula con, boolean sortClauses) {

 //       if (f1 == null & f2 == null) 
 //           return;
        Formula newcon = new Formula();
        Formula f1new = new Formula();
        Formula f2new = new Formula();
        newcon.read(Clausifier.normalizeVariables(con.theFormula));
        if (sortClauses) 
            newcon = Clausifier.toCanonicalClausalForm(newcon);
        ArrayList twoSupports = new ArrayList();
        if (f1 != null) {        
            f1new.read(Clausifier.normalizeVariables(f1.theFormula));
            if (sortClauses) 
                f1new = Clausifier.toCanonicalClausalForm(f1new);
            twoSupports.add(f1new);
            if (deductions.get(f1new.theFormula) == null) 
                deductions.put(f1new.theFormula,null);
        }
        if (f2 != null) {
            f2new.read(Clausifier.normalizeVariables(f2.theFormula));
            if (sortClauses) 
                f2new = Clausifier.toCanonicalClausalForm(f2new);
            twoSupports.add(f2new);
            if (deductions.get(f2new.theFormula) == null) 
                deductions.put(f2new.theFormula,null);
        }
        deductions.put(newcon.theFormula,twoSupports);
        System.out.println("INFO in STP.addToDeductions()");
        System.out.println("conclusion: " + newcon);
        if (f1 != null)         
            System.out.println("premise 1: " + f1new);
        if (f2 != null)         
            System.out.println("premise 2: " + f2new);
    }

    /** *************************************************************
     *  Find support for a formula
     *  @return an ArrayList of Formulas that consitute support for
     *          the clause.  Return an empty ArrayList if no proof
     *          is found.
     */
    private ArrayList<Formula> prove() {

        ArrayList<Formula> result = new ArrayList();
        while (TBU.size() > 0) {
            if (_PROVE_DEBUG) System.out.println("\n\nINFO in STP.prove(): TBU: " + TBU);
            System.out.println("\nTBU length: " + TBU.size());
            //System.out.println("INFO in STP.prove(): lemmas: " + lemmas);
            AnotherAVP avp = (AnotherAVP) TBU.remove(0);
            //if (!lemmas.containsKey(form)) {
            String norm = Clausifier.normalizeVariables(avp.form.theFormula); 

            //System.out.println("INFO in STP.prove(): attempting to prove: " + avp.form);
            Formula f = new Formula();
            f.read(norm);

            ArrayList<String> al = f.collectTerms();
            ArrayList<AnotherAVP> candidates = null;
            if (f.isSimpleClause()) 
                candidates = collectCandidates(al,"false");                
            else if (f.isSimpleNegatedClause()) 
                candidates = collectCandidates(al,"true");                
            else
                candidates = collectCandidates(al,"both");                

            if (candidates != null && candidates.size() > 0) {
                for (int i = 0; i < candidates.size(); i++) {
                    AnotherAVP avpCan = (AnotherAVP) candidates.get(i);
                    Formula candidate = avpCan.form;
                    if (_PROVE_DEBUG) System.out.println("INFO in STP.prove(): checking candidate:\n" + candidate);
                    Formula resultForm = new Formula();
                    TreeMap mappings = f.resolve(candidate,resultForm);
                    if (resultForm != null && resultForm.empty()) {  // Successful resolution! Result is empty list
                        ArrayList support = new ArrayList();
                        if (lemmas.get(avpCan.form.theFormula) != null) 
                            support.addAll((ArrayList) lemmas.get(avpCan.form.theFormula));
                        if (lemmas.get(f.theFormula) != null) 
                            support.addAll((ArrayList) lemmas.get(f.theFormula));
                        support.add(f);
                        support.add(avpCan.form);
                        addToDeductions(f,candidate,resultForm,true);
                        return support;
                    }
                    if (mappings != null) {  // && mappings.keySet().size() > 0
                        if (_PROVE_DEBUG) System.out.println("\nINFO in STP.prove(): resolve result:\n" + resultForm);
                        if (_PROVE_DEBUG) System.out.println("for candidate\n" + candidate + "\n with formula\n " + avp.form);
                        ArrayList support = new ArrayList();
                        if (lemmas.get(avpCan.form.theFormula) != null) 
                            support.addAll((ArrayList) lemmas.get(avpCan.form.theFormula));
                        if (lemmas.get(f.theFormula) != null) 
                            support.addAll((ArrayList) lemmas.get(f.theFormula));
                        support.add(f);
                        support.add(avpCan.form);
                        lemmas.put(resultForm.theFormula,support);
                        AnotherAVP avpNew = new AnotherAVP();
                        if (!formulas.contains(resultForm)) {
                            avpNew.form = resultForm;
                            avpNew.intval = 10000-resultForm.theFormula.length();
                            if (!TBU.contains(avpNew)) {
                                addToDeductions(f,candidate,resultForm,true);
                                TBU.add(avpNew);
                                Collections.sort(TBU);
                            }
                        }
                    }
                    //else
                        //System.out.println("INFO in STP.prove(): candidate did not resolve\n" + candidate);                    
                }
                if (_PROOF_GENERATION) {
                    Formula fnew = findInstantiation(f);
                    if (fnew != null && !formulas.contains(fnew)) {
                        AnotherAVP avpNew = new AnotherAVP();
                        avpNew.form = fnew;
                        avpNew.intval = 10000-fnew.theFormula.length();
                        if (!TBU.contains(avpNew)) {
                            //addToDeductions(f,candidate,resultForm);
                            TBU.add(avpNew);
                            Collections.sort(TBU);
                        }
                    }
                }
            }
            //indexOneFormula(f);       // all lemmas must be added to the knowledge base for completeness
            //formulas.add(f);
            //}
        }
        return result;
    }

    /** *************************************************************
     *  @param f is the formula of the proof step to be printed
     *  @param formIds are all the formulas used in the proof
     *  @param alreadyPrinted is a list of the Integer ids (the
     *                        index from the formIds ArrayList) of
     *                        formulas that have already been
     *                        printed in the proof.
     */
    private String formatResultRecurse (Formula f, ArrayList<String> formIds, ArrayList<Integer> alreadyPrinted) {

        if (alreadyPrinted.contains(formIds.indexOf(f.theFormula))) 
            return null;
        alreadyPrinted.add(new Integer(formIds.indexOf(f.theFormula)));
        System.out.println("INFO in STP.formatResultRecurse(): alreadyPrinted: " + alreadyPrinted);
        System.out.println("INFO in STP.formatResultRecurse(): checking formula: " + f);
        System.out.println("INFO in STP.formatResultRecurse(): this formula id: " + formIds.indexOf(f.theFormula));

        StringBuffer result = new StringBuffer();
        ArrayList<Formula> support = (ArrayList) deductions.get(f.theFormula);
        System.out.println("INFO in STP.formatResultRecurse(): support: " + support);
        if (support != null && support.size() > 0) {
            Formula f1 = (Formula) support.get(0);
            System.out.println("INFO in STP.formatResultRecurse(): internal checking formula: " + f1);
            Formula f2 = null;
            if (support.size() > 1) {
                f2 = (Formula) support.get(1);
                System.out.println("INFO in STP.formatResultRecurse(): internal checking formula 2: " + f2);
            }
            System.out.println("INFO in STP.formatResultRecurse(): recursing on formula 1: " + f1);
            String f1result = formatResultRecurse(f1,formIds,alreadyPrinted);
            if (f1result != null) 
                result.append(f1result);
            if (f2 != null) {
                System.out.println("INFO in STP.formatResultRecurse(): recursing on formula 2: " + f2);
                String f2result = formatResultRecurse(f2,formIds,alreadyPrinted);
                if (f2result != null) 
                    result.append(f2result); 
            }
            result.append("<proofStep>\n");
            result.append("<premises>\n");
            if (f1 != null) {
                if (formIds.indexOf(f1.theFormula) < 0) 
                    formIds.add(f1.theFormula);
                result.append("<premise>\n<formula number='" + formIds.indexOf(f1.theFormula) + "'>\n");
                result.append(f1.theFormula + "\n");
                result.append("</formula>\n</premise>\n");
            }
            if (f2 != null) {
                if (formIds.indexOf(f2.theFormula) < 0) 
                    formIds.add(f2.theFormula);
                result.append("<premise>\n<formula number='" + formIds.indexOf(f2.theFormula) + "'>\n");
                result.append(f2.theFormula + "\n");
                result.append("</formula>\n</premise>\n");
            }
            result.append("</premises>\n");
        }
        else {
            result.append("<proofStep>\n");
            result.append("<premises>\n");
            result.append("</premises>\n");
        }
        if (formIds.indexOf(f.theFormula) < 0) 
            formIds.add(f.theFormula);        
        result.append("<conclusion>\n<formula number='" + formIds.indexOf(f.theFormula) + "'>\n" + f.theFormula + "\n</formula>\n</conclusion>\n");
        result.append("</proofStep>\n");

        System.out.println(result);
        return result.toString();
    }

    /** *************************************************************
     */
    public String formatResultNew (boolean ground) {

        System.out.println("INFO in STP.formatResultNew(): ");
        printDeductions();
        ArrayList<Integer> alreadyPrinted = new ArrayList();
        ArrayList<String> formIds = new ArrayList();
        formIds.addAll(deductions.keySet());
        StringBuffer result = new StringBuffer();
        if (!deductions.containsKey("()")) {
            result.append("<queryResponse>\n<answer result='no' number='0'>\n</answer>\n</queryResponse>\n");
        }
        else {
            result.append("<queryResponse>\n<answer result='yes' number='1'>\n");
            result.append("<bindingSet type='definite'>\n<binding>yes<var name='' value=''/>\n</binding>\n</bindingSet>\n");
            result.append("<proof>\n");
            Formula f = new Formula();
            f.read("()");
            result.append(formatResultRecurse(f,formIds,alreadyPrinted));
            result.append("</proof>\n</answer>\n<summary proofs='1'/>\n</queryResponse>\n");
        }
        System.out.println("INFO in STP.formatResult(): " + result.toString());
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
    public String submitQuery (String formula,int timeLimit,int bindingsLimit) {

        boolean ground = false;
        Formula negQuery = new Formula();
        String rawNegQuery = "(not " + formula + ")";
        if (negQuery.isGround()) {
            ground = true;
        }
        negQuery.read("(not " + formula + ")");
        negQuery = Clausifier.clausify(negQuery);     // negation will be pushed in
        if (negQuery.theFormula.equals(rawNegQuery)) 
            addToDeductions(null,null,negQuery,true);
        else {
            Formula originalQuery = new Formula();
            originalQuery.read("(not " + formula + ")");
            addToDeductions(originalQuery,null,negQuery,true);
        }
        System.out.println("INFO in STP.submitQuery(): clausified query: " + negQuery);
        AnotherAVP avp = null;
        if (negQuery.car().equals("and")) {
            ArrayList<Formula> al = Clausifier.separateConjunctions(negQuery);
            for (int i = 0; i < al.size(); i++) {
                Formula f2 = (Formula) al.get(i);
                avp = new AnotherAVP();
                avp.form = f2;
                avp.intval = f2.theFormula.length();
                TBU.add(avp);
                Collections.sort(TBU);
                System.out.println("INFO in STP.submitQuery(): adding to TBU: " + avp);
            }
        }
        else {
            avp = new AnotherAVP();
            avp.form = negQuery;
            avp.intval = negQuery.theFormula.length();
            TBU.add(avp);
            Collections.sort(TBU);
        }
        ArrayList<Formula> res = prove(); 
        System.out.println("INFO in STP.submitQuery(): " + deductions);
        // return res.toString();
        return formatResultNew(ground);       
    }

    /** *************************************************************
     * Add an assertion.
     *
     * @param formula asserted formula in the KIF syntax
     * @return answer to the assertion (in the XML syntax)
     * @throws IOException should not normally be thrown
     */
    @Override
    public String assertFormula(String form) throws IOException {

        Formula f = new Formula();
        f.read(form);
        formulas.add(f);
        //Formulas asserted through this method will always be used.
        
        return null;
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
    public static void tq1Abbrev() {

        ArrayList al = new ArrayList();
        al.add("(=> (i ?X290 C) (exists (?X12) (m ?X12 ?X290)))");
        al.add("(s O C)");
        al.add("(i SOC SOC)");
        al.add("(i Org1-1 O)");
        al.add("(=> (s ?X403 ?X404) (and (i ?X403 SOC) (i ?X404 SOC)))");
        al.add("(=> (and (i ?X403 SOC) (i ?X404 SOC)) " +
               "(=> (and (s ?X403 ?X404) (i ?X405 ?X403)) (i ?X405 ?X404)))");
        Formula query = new Formula();
        query.read("(exists (?MEMBER) (m ?MEMBER Org1-1))");
        STP stp = new STP(al);
        System.out.println(stp.submitQuery(query.theFormula,0,0)); 

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
    public static void tq1() {

        ArrayList al = new ArrayList();
        al.add("(=> (instance ?X290 Collection) (exists (?X12) (member ?X12 ?X290)))");
        al.add("(subclass Organization Collection)");
        al.add("(instance SetOrClass SetOrClass)");
        al.add("(instance Org1-1 Organization)");
        al.add("(=> (subclass ?X403 ?X404) (and (instance ?X403 SetOrClass) (instance ?X404 SetOrClass)))");
        al.add("(=> (and (instance ?X403 SetOrClass) (instance ?X404 SetOrClass)) " +
               "(=> (and (subclass ?X403 ?X404) (instance ?X405 ?X403)) (instance ?X405 ?X404)))");
        Formula query = new Formula();
        query.read("(exists (?MEMBER) (member ?MEMBER Org1-1))");
        STP stp = new STP(al);
        System.out.println(stp.submitQuery(query.theFormula,0,0)); 
    }

    /** *************************************************************
     */
    public static void tq2() {

        ArrayList al = new ArrayList();
        al.add("(=> (p ?X) (q ?X))");
        al.add("(=> (or (q ?X) (r ?X)) (t ?X))");
        al.add("(p a)");
        Formula query = new Formula();
        query.read("(or (t a) (r a))");
        STP stp = new STP(al);
        System.out.println(stp.submitQuery(query.theFormula,0,0)); 
    }

    /** *************************************************************
     *  example from Russel and Norvig
     */
    public static void rnTest() {

        ArrayList<String> al = new ArrayList();
        al.add("(=> (and (attribute ?X American) (instance ?Y Weapon) (instance ?Z Nation) " +
                        "(attribute ?Z Hostile) (instance ?S Selling) (agent ?S ?X) (patient ?S ?Y) (recipient ?S ?Z))" +
                   "(attribute ?X Criminal))");
        al.add("(possesses Aardvark M1-Missile)");
        al.add("(instance M1-Missile Missile)");
        al.add("(=> (and (possesses Aardvark ?X) (instance ?X Missile))"+
                   "(and (instance ?S Selling) (agent ?S West) (patient ?S ?X) (recipient ?S Aardvark)))");
        al.add("(=> (instance ?X Missile) (instance ?X Weapon))");
        al.add("(=> (enemies ?X America) (attribute ?X Hostile))");
        al.add("(attribute West American)");
        al.add("(instance Aardvark Nation)");
        al.add("(enemies Aardvark America)");
        al.add("(instance America Nation)");
        Formula query = new Formula();
        query.read("(attribute ?X Criminal)");

        System.out.println("********************************************");
        Iterator<String> it = al.iterator();
        while (it.hasNext()) {
            Formula f = new Formula();
            String s = (String) it.next();
            f.read(s);
            System.out.println(f);
        }
        STP stp = new STP(al);
        System.out.println("Result: " + stp.submitQuery(query.theFormula,0,0)); 
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
/*
        Formula query = new Formula();
        Formula negQuery = new Formula();
        query.read("(exists (?MEMBER) (member ?MEMBER Org1-1))");
        negQuery.read("(not " + query.theFormula + ")");
        negQuery = negQuery.clausify();     // negation will be pushed in
        System.out.println("STP.main() result: " + negQuery);

        CNFFormula cnf = new CNFFormula();
        cnf.read(negQuery.theFormula);
        System.out.println("STP.main() result for cnf: " + cnf);         
         */
        //tq1Abbrev();
        //tq1();
        //tq2();
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

