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

import com.sun.org.apache.bcel.internal.generic.NEW;

/** ***************************************************************
 * The Sigma theorem prover. A simple resolution prover in Java.
 */
public class STP extends InferenceEngine {

     /** The knowledge base */
    ArrayList<Formula> formulas = new ArrayList();

    /** Previously solved lemmas that is used to prevent cycles  */
    TreeMap<String, ArrayList<Formula>> lemmas = new TreeMap();
    TreeSet<String> current = new TreeSet();
    TreeSet<String> failed = new TreeSet();

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
     * Convert to a String.
     */
    public static class STPEngineFactory extends EngineFactory {

        public InferenceEngine createWithFormulas(Iterable<String> formulaSource) {  
            return new STP(formulaSource);
        }

        public InferenceEngine createFromKBFile(String kbFileName) {
            return STP.getNewInstance(kbFileName);
        }
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
                System.out.println("INFO in STP(): " + error);
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
        System.out.println("INFO in STP(): formulas: " + formulas);
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
     */
    private void clausifyFormulas() {

        ArrayList<Formula> newFormulas = new ArrayList();
        Iterator<Formula> it = formulas.iterator();
        while (it.hasNext()) {
            Formula f = (Formula) it.next();
            f = f.clausify();
            newFormulas.add(f);
            //System.out.println("INFO in STP.clausifyFormulas(): " + f);
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
    private void buildIndexes() {

        Iterator<Formula> it = formulas.iterator();
        while (it.hasNext()) {
            Formula f = (Formula) it.next();
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

            Formula fclause = f.clausify();
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
                clause = Formula.normalizeVariables(clause);
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
       // System.out.println("INFO in STP.buildIndexes(): negTermPointers: " + negTermPointers);
       // System.out.println("INFO in STP.buildIndexes(): posTermPointers: " + posTermPointers);
    }

    /** *************************************************************
     *  Assumes that query is a simple clause.
     *  @param negated says whether the query was originally
     *                 negated.  If so, the negation must have been
     *                 removed before invoking this method.
     *  @return a unification Map of variable names and values.
     */
    private TreeMap<String, String> resolve(Formula candidate, Formula query, boolean negated) {

        System.out.println("INFO is STP.resolve(): Attempting resolution of " + candidate + " with ");
        if (negated) System.out.print("negated ");
        System.out.println(query);
        TreeMap<String, String> result = new TreeMap();
        if (!query.isSimpleClause()) 
            return result;
        if (query.theFormula.indexOf("?") == 1)    // predicate variable
            return result;       
        else {
            if (candidate.isSimpleClause()) 
                return candidate.unify(query);
            else {
                Formula newCandidate = new Formula();
                newCandidate.theFormula = candidate.theFormula;
                if (newCandidate.car().equals("or")) {
                    newCandidate.read(newCandidate.cdr());
                    Formula clause = new Formula();
                    while (!newCandidate.empty()) {
                        clause.read(newCandidate.car());
                        if ((!negated && clause.car().equals("not")) ||
                            (negated && !clause.car().equals("not"))) {
                            if (clause.car().equals("not")) {
                                clause.read(clause.cdr());   // remove the enclosing
                                clause.read(clause.car());   // (not ...)
                            }
                            result = clause.unify(query);
                            if (result != null && result.keySet().size() > 0) 
                                return result;
                        }
                        newCandidate.read(newCandidate.cdr());
                    }
                }
                else {
                    System.out.println("Error is STP.resolve(): complex formula not in CNF: " + candidate);
                    return result;
                }
            }
        }
        return result;
    }

    /** *************************************************************
     *  Remove a clause that is part of another Formula.  This is
     *  typically done when a clause unifies with its negation in
     *  another Formula.  Both Fomulas should be in their original
     *  form - i.e. if "removal" is a negated clause to be unified
     *  with a positive clause in "source", then "removed" should be
     *  negated when passed to this routine.
     *  This routine assumes that resolution has already been found
     *  between "source" and "removal", and that "removal" is a
     *  simple or simple negated clause.  It also assumes that the
     *  variable substitutions have occured after unification, so
     *  that the clauses to be resolved are syntactically identical.
     *  If there's only one clause after removal, then the routine
     *  will remove the enclosing "or".
     */
    private Formula removeClause(Formula source, Formula removal) {

        System.out.println("INFO in STP.removeClause(): " + removal.theFormula + " from " + source.theFormula);
        Formula result = new Formula();
        Formula source2 = new Formula();

        boolean negatedRemoval = removal.car().equals("not");
        Formula removal2 = new Formula();
        if (negatedRemoval) {
            removal2.read(removal.cdr());
            removal2.read(removal2.car());  // remove the "not" from "removal", if present
            if (source.isSimpleClause()) {  // two simple and opposite clauses resolve to the empty list
                result.read("()");
                return result;
            }
        }
        else {
            removal2.read(removal.theFormula);
            if (source.isSimpleNegatedClause()) { // two simple and opposite clauses resolve to the empty list
                result.read("()");
                return result;
            }
        }
        if (!source.car().equals("or")) {
            System.out.println("Error in STP.removeClause(): formula not in CNF: " + source);
            return result;
        }
        source2.read(source.cdr());     // remove the initial "or"
        while (!source2.empty()) {
            boolean match = false;
            Formula clause = new Formula();
            clause.read(source2.car());     // get the first clause 
            System.out.println("INFO in STP.removeClause(): clause: " + clause);
            source2.read(source2.cdr());    // remove that clause from source2
            if ((clause.car().equals("not") && !negatedRemoval) ||
                (!clause.car().equals("not") && negatedRemoval)) {
                Formula newClause = new Formula();
                if (clause.car().equals("not")) {
                    newClause.read(clause.cdr());
                    newClause.read(newClause.car()); // remove the "not"
                }
                else
                    newClause.read(clause.theFormula);

                System.out.println("INFO in STP.removeClause(): checking for match between: \n" + removal2.theFormula + 
                                   " and \n" + newClause.theFormula);
                //System.out.println("Compare: " + newClause.theFormula.compareTo(removal2.theFormula));
                //System.out.println("Bytes1: " + newClause.theFormula.getBytes());
                //System.out.println("Bytes2: " + removal2.theFormula.getBytes());
                if (newClause.theFormula.equals(removal2.theFormula)) {
                    match = true;
                    System.out.println("Match!");
                }
            }
            if (!match) {
                if (result.theFormula == null || result.theFormula.length() < 1 ||
                    (!result.empty() && !result.car().equals("or"))) {
                    if (result.theFormula == null || result.theFormula.length() < 1 ||
                        result.empty()) 
                        result.theFormula = clause.theFormula;
                    else
                        result.read("(or " + result.theFormula + " " + clause.theFormula + ")");
                }
                else {
                    if (result.empty())                    
                        result.read(clause.theFormula);
                    else {
                        clause.theFormula = "(" + clause.theFormula + ")";
                        result = result.append(clause);
                    }
                }
            }
        }

        System.out.println("INFO in STP.removeClause(): result: " + result);
        return result;
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
        public int compareTo(Object avp) throws ClassCastException {

            if (!avp.getClass().getName().equalsIgnoreCase("com.articulate.sigma.STP$AnotherAVP")) 
                throw new ClassCastException("Error in AnotherAVP.compareTo(): "
                                             + "Class cast exception for argument of class: " 
                                             + avp.getClass().getName());
            return ((AnotherAVP) avp).intval - intval;
        }
    }

    /** *************************************************************
     *  Collect Formulas, ranked by the number of terms they cover
     *  from the supplied ArrayList.
     *  @param negated indicates whether to select only those
     *                 Formulas in which the terms appear in negated
     *                 clauses, or vice versa
     */
    private ArrayList<AnotherAVP> collectCandidates(ArrayList<String> terms, boolean negated) {

        System.out.println("INFO in STP.collectCandidates(): " + terms);
        TreeMap<Formula, Integer> tm = new TreeMap();
        ArrayList result = new ArrayList();
        Iterator it = terms.iterator();
        while (it.hasNext()) {      // find formulas that have all the terms as the clause to be proven
            String term = (String) it.next();
            if (!Formula.LOGICAL_OPERATORS.contains(term) && !Formula.isVariable(term)) {
                ArrayList<Formula> pointers = null;
                if (negated) 
                    pointers = (ArrayList) posTermPointers.get(term);
                else
                    pointers = (ArrayList) negTermPointers.get(term);
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
            avp.intval = num.intValue();
            avp.form = f;
            result.add(avp);
        }
        Collections.sort(result);

        System.out.println("INFO in STP.collectCandidates(): result " + result);
        return result;
    }

    /** *************************************************************
     *  Find support for a single clause
     *  @return an ArrayList of Formulas that consitute support for
     *          the clause.  Return an empty ArrayList if no proof
     *          is found.
     */
    private ArrayList<Formula> proveClauseLit(Formula f, boolean negated) {

        System.out.println("INFO in STP.proveClauseLit(): " + f + " negated:" + negated);
        ArrayList<Formula> result = new ArrayList();
        Formula fnew = new Formula();
        if (negated) {
            fnew.read(f.cdr());    // remove the "not"
            fnew.read(fnew.car()); // remove the remaining extract parentheses
                                    
            if (posLits.keySet().contains(f.theFormula)) {
                Formula exactMatch = (Formula) posLits.get(fnew.theFormula);
                result.add(exactMatch);
                return result;
            }
        }
        else {
            fnew.read(f.theFormula);
            if (negLits.keySet().contains(f.theFormula)) {
                Formula exactMatch = (Formula) negLits.get(f.theFormula);
                result.add(exactMatch);
                return result;
            }
        }
        ArrayList<String> al = fnew.collectTerms();
        ArrayList<AnotherAVP> candidates = collectCandidates(al,negated);

        System.out.println("INFO in STP.proveClauseLit(): candidate unifiers: " + candidates);
        Iterator it = candidates.iterator();
        while (it.hasNext()) {
            AnotherAVP avp = (AnotherAVP) it.next();
            Formula candidate = avp.form;
            if (candidate.car().equals("not")) {
                System.out.println("Error in STP.proveClauseLit(): retrieved a negative " +
                                   "literal candidate unifier: " + candidate + " for " + f);
            }
            else {
                Formula f3 = new Formula();
                f3.read(fnew.theFormula);
                TreeMap<String, String> m = resolve(candidate,fnew,negated);
                if (m != null && m.keySet().size() > 0) {
                    Formula newCandidate = new Formula();
                    newCandidate.read(candidate.theFormula);
                    //System.out.println("INFO in STP.proveClauseNegLit(): before substitution1: " + newCandidate.theFormula);
                    newCandidate = newCandidate.substitute(m);
                    //System.out.println("INFO in STP.proveClauseNegLit(): after substitution1: " + newCandidate.theFormula);
                    //System.out.println("INFO in STP.proveClauseNegLit(): before substitution2: " + f3.theFormula);
                    f3 = f3.substitute(m);
                    //System.out.println("INFO in STP.proveClauseNegLit(): after substitution2: " + f3.theFormula);
                    f3.theFormula = "(not " + f3.theFormula + ")";
                    //System.out.println("INFO in STP.proveClauseNegLit(): after read: " + f3.theFormula);
                    newCandidate = removeClause(newCandidate,f3);  
                    if (!newCandidate.empty()) {
                        ArrayList<Formula> newResult = null;
                        if (!current.contains(newCandidate.theFormula))                         
                            newResult = prove(newCandidate);
                        if (newResult != null && newResult.size() > 0) {
                            result.add(candidate);
                            result.addAll(newResult);
                            return result;
                        }
                    }
                    else {
                        result.add(candidate);
                        return result;
                    }
                }
            }
        }
        return result;
    }

    /** *************************************************************
     *  Find support for a single clause
     *  @return an ArrayList of Formulas that consitute support for
     *          the clause.  Return an empty ArrayList if no proof
     *          is found.
     */
    private ArrayList<Formula> proveClause(Formula f) {

        System.out.println("INFO in STP.proveClause(): " + f);
        ArrayList<Formula> result = new ArrayList();
        if (f.isSimpleClause())
            return proveClauseLit(f,false);        
        else if (f.isSimpleNegatedClause()) 
            return proveClauseLit(f,true);
        else 
            System.out.println("Error in STP.proveClause(): Not a simple clause: " + f);
        return null;
    }

    /** *************************************************************
     *  Find support for a formula
     *  @return an ArrayList of Formulas that consitute support for
     *          the clause.  Return an empty ArrayList if no proof
     *          is found.
     */
    private ArrayList<Formula> prove(Formula f) {

        String norm = Formula.normalizeVariables(f.theFormula); // Don't try to prove if we've already
        if (lemmas.keySet().contains(norm))                     // succeeded or failed on this formula.
            return (ArrayList<Formula>) lemmas.get(norm);
        if (failed.contains(f.theFormula)) 
            return null;

        ArrayList<Formula> result = new ArrayList();
        System.out.println("INFO in STP.prove(): " + f);
        current.add(f.theFormula);
        if (f.isSimpleClause() || f.isSimpleNegatedClause()) 
            return proveClause(f);
        else {
            String orTerm = f.car();
            Formula clauses = new Formula();  
            clauses.read(Formula.normalizeVariables(f.theFormula));
            while (!clauses.empty()) {
                Formula c = new Formula();
                c.read(clauses.theFormula);
                norm = Formula.normalizeVariables(c.theFormula);
                ArrayList<Formula> res = null;
                if (!current.contains(c.theFormula))                 
                    res = prove(c);
                if (res != null && res.size() > 0) {
                    System.out.println("INFO in STP.prove(): Successfully proved " + c);
                    result.addAll(res);
                    lemmas.put(norm,res);
                    if (current.contains(norm)) 
                        current.remove(norm);
                }
                else {
                    failed.add(Formula.normalizeVariables(c.theFormula));
                    if (current.contains(norm)) 
                        current.remove(norm);
                }
                clauses.read(clauses.cdr());
            }
        }
        if (result.size() > 0) 
            lemmas.put(f.theFormula,result);        
        return result;
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

        ArrayList result = new ArrayList();
        Formula negQuery = new Formula();
        negQuery.read("(not " + formula + ")");
        negQuery = negQuery.clausify();     // negation will be pushed in
        System.out.println("INFO in STP.submitQuery(): clausified query: " + negQuery);
        if (negQuery.car().equals("or"))         
            negQuery.read(negQuery.cdr());  // remove the initial "or"
        while (!negQuery.empty()) {
            String clause = null;
            if (negQuery.isSimpleClause() || negQuery.isSimpleNegatedClause())
                clause = negQuery.theFormula;
            else
                clause = negQuery.car();
            clause = Formula.normalizeVariables(clause);
            System.out.println("INFO in STP.submitQuery(): clause: " + clause);
            Formula c = new Formula();
            c.read(clause);
            ArrayList<Formula> res = prove(c);
            if (res != null && res.size() > 0) 
                return "Success! " + res.toString();      // success if any clause in the disjunction is proven
            negQuery.read(negQuery.cdr());
        }
        return "fail";          // getting here means each clause failed to be proven
    }

    /** *************************************************************
     * Add an assertion.
     *
     * @param formula asserted formula in the KIF syntax
     * @return answer to the assertion (in the XML syntax)
     * @throws IOException should not normally be thrown
     */
    @Override
    public String assertFormula(String formula) throws IOException {

        //formulas.add(formula);
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
    public static void tq1() {

        ArrayList al = new ArrayList();
        al.add("(=> (instance ?X290 Collection) (exists (?X12) (member ?X12 ?X290)))");
        al.add("(subclass Organization Collection)");
        al.add("(=> (subclass ?X403 ?X404) (and (instance ?X403 SetOrClass) (instance ?X404 SetOrClass)))");
        al.add("(=> (and (instance ?X403 SetOrClass) (instance ?X404 SetOrClass)) " +
               "(=> (and (subclass ?X403 ?X404) (instance ?X405 ?X403)) (instance ?X405 ?X404)))");
        Formula query = new Formula();
        query.read("(exists (?MEMBER) (member ?MEMBER Org1-1))");
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
        al.add("(possesses Nono M1-Missile)");
        al.add("(instance M1-Missile Missile)");
        al.add("(=> (and (possesses Nono ?X) (instance ?X Missile))"+
                   "(and (instance ?S Selling) (agent ?S West) (patient ?S ?X) (recipient ?S Nono)))");
        al.add("(=> (instance ?X Missile) (instance ?X Weapon))");
        al.add("(=> (enemies ?X America) (attribute ?X Hostile))");
        al.add("(attribute West American)");
        al.add("(instance Nono Nation)");
        al.add("(enemies Nono America)");
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
        System.out.println(stp.submitQuery(query.theFormula,0,0)); 
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
        //tq1();
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

