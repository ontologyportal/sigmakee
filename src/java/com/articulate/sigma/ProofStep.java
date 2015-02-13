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

import java.util.ArrayList;
import java.util.HashMap;

 /** A trivial structure to hold the elements of a proof step. */
public class ProofStep {
  
	public static final String QUERY = "[Query]";
	public static final String NEGATED_QUERY = "[Negated Query]";
	public static final String INSTANTIATED_QUERY = "[Instantiated Query]";
   
     /** A String of the type clause or formula */
    public String formulaType = null;

     /** A String of the role of the formula */
    public String formulaRole = null;

     /** A String of the inference type, e.g. add_answer_literal | assume_negation | etc. */
     public String inferenceType = null;


     /** A String containing a valid KIF expression, that is the axiom 
      *  expressing the conclusion of this proof step. */
    public String axiom = null;

     /** The number assigned to this proof step, initially by EProver and
      *  then normalized by ProofStep.normalizeProofStepNumbers() */
    public Integer number = new Integer(0);

     /** An ArrayList of Integer(s), which reference prior proof steps from
      *  which this axiom is derived. Note that the numbering is what
      *  the ProofProcessor assigns, not necessarily the proof
      *  numbers returned directly from the inference engine. */
    public ArrayList<Integer> premises = new ArrayList();
    
    /** ***************************************************************
     * Take an ArrayList of ProofSteps and renumber them consecutively
     * starting at 1.  Update the ArrayList of premises so that they
     * reflect the renumbering.
     */
    public static ArrayList<ProofStep> normalizeProofStepNumbers(ArrayList<ProofStep> proofSteps) {

        // old number, new number
        HashMap<Integer,Integer> numberingMap = new HashMap();

        //System.out.println("INFO in ProofStep.normalizeProofStepNumbers()");
        int newIndex = 1;
        for (int i = 0; i < proofSteps.size(); i++) {
            //System.out.println("INFO in ProofStep.normalizeProofStepNumbers(): numberingMap: " + numberingMap);
            ProofStep ps = (ProofStep) proofSteps.get(i);
            //System.out.println("INFO in ProofStep.normalizeProofStepNumbers(): Checking proof step: " + ps);
            Integer oldIndex = new Integer(ps.number);
            if (numberingMap.containsKey(oldIndex)) 
                ps.number = (Integer) numberingMap.get(oldIndex);
            else {
                //System.out.println("INFO in ProofStep.normalizeProofStepNumbers(): adding new step: " + newIndex);
                ps.number = new Integer(newIndex);            
                numberingMap.put(oldIndex,new Integer(newIndex++));
            }
            for (int j = 0; j < ps.premises.size(); j++) {
                Integer premiseNum = ps.premises.get(j);
                //System.out.println("INFO in ProofStep.normalizeProofStepNumbers(): old premise num: " + premiseNum);
                Integer newNumber = null;
                if (numberingMap.get(premiseNum) != null) 
                    newNumber = new Integer((Integer) numberingMap.get(premiseNum));
                else {
                    newNumber = new Integer(newIndex++);
                    numberingMap.put(premiseNum,new Integer(newNumber));
                }
                //System.out.println("INFO in ProofStep.normalizeProofStepNumbers(): new premise num: " + newNumber);
                ps.premises.set(j,newNumber);
            }
        }
        return proofSteps;
    }

    /** ***************************************************************
     * Take an ArrayList of ProofSteps and renumber them consecutively
     * starting at 1.  Update the ArrayList of premises so that they
     * reflect the renumbering.
     */
    public static ArrayList<ProofStep> removeDuplicates(ArrayList<ProofStep> proofSteps) {

    	System.out.println("INFO in ProofSteps.removeDuplicates()");
        // old number, new number
        HashMap<Integer,Integer> numberingMap = new HashMap<Integer,Integer>();
        
        // formula string, proof step number
        HashMap<String,Integer> formulaMap = new HashMap<String,Integer>();
        
        // proof step number, proof step
        HashMap<Integer,ProofStep> reverseFormulaMap = new HashMap<Integer,ProofStep>();
        
        ArrayList<ProofStep> newProofSteps = new ArrayList<ProofStep>();
        ArrayList<ProofStep> dedupedProofSteps = new ArrayList<ProofStep>();
        
        int counter = 1;
        for (int i = 0; i < proofSteps.size(); i++) {
            ProofStep ps = (ProofStep) proofSteps.get(i);
            Integer index = new Integer(ps.number);
            reverseFormulaMap.put(index,ps);
            String s = Clausifier.normalizeVariables(ps.axiom);
            if (formulaMap.keySet().contains(s) && ps.premises.size()==1) {   // If the step is a duplicate, relate the current step number
            	Integer fNum = (Integer) formulaMap.get(s);                   // to the existing number of the formula
            	numberingMap.put(index,fNum);
            }
            else {
            	numberingMap.put(index,counter);
            	formulaMap.put(s,counter);
            	counter++;
            	dedupedProofSteps.add(ps);
            }
        }
        for (int i = 0; i < dedupedProofSteps.size(); i++) { 
            ProofStep ps = (ProofStep) dedupedProofSteps.get(i);            
        	Integer newIndex = new Integer(ps.number);
        	if (numberingMap.keySet().contains(newIndex))
        		newIndex = (Integer) numberingMap.get(newIndex);
        	ProofStep psNew = new ProofStep();
        	psNew.formulaRole = ps.formulaRole;
        	psNew.formulaType = ps.formulaType;
            psNew.inferenceType = ps.inferenceType;
            String s = Clausifier.normalizeVariables(ps.axiom);
        	psNew.axiom = s;
        	psNew.number = newIndex;
        	ArrayList<Integer> newPremises = new ArrayList();        	   
        	for (int j = 0; j < ps.premises.size(); j++) {
        		Integer premiseNum = ps.premises.get(j);
        		Integer newNumber = null;
        		if (numberingMap.get(premiseNum) != null) 
        			newNumber = new Integer((Integer) numberingMap.get(premiseNum));
        		else 
        			newNumber = new Integer(premiseNum);                
        		newPremises.add(newNumber);                    
        	}
        	psNew.premises = newPremises;
        	newProofSteps.add(psNew);            
        }
        return newProofSteps;
    }

    /** ***************************************************************
     * created a new by qingqing
     * remove unnecessary steps, which should not appear in proof
     * Unnecessary steps could be:
     * (1) conjectures;
     * (2) Successful resolution theorem proving results in a contradiction;
     */
    public static ArrayList<ProofStep> removeUnnecessary(ArrayList<ProofStep> proofSteps) {

        ArrayList<ProofStep> results = new ArrayList<ProofStep>();
        boolean firstTimeSeeFALSE = true;
        for (int i = 0; i < proofSteps.size(); i++) {
            ProofStep ps = proofSteps.get(i);

            if (ps.formulaType!= null && !ps.formulaType.equals("conjecture")) {    // conjecture is not allowed in the proof step
                if (ps.axiom.equalsIgnoreCase("FALSE")) {
                    if (firstTimeSeeFALSE) {    // only add when it is the first time to see contradiction
                        results.add(ps);
                        firstTimeSeeFALSE = false;
                    }
                } else {
                    results.add(ps);
                }
            }
        }
        results = normalizeProofStepNumbers(results);
        return results;
    }


    /** ***************************************************************
     */
    public String toString() {

        StringBuffer sb = new StringBuffer();
        sb.append(number + ". " + axiom + " " + premises + "\n");
        return sb.toString();
    }
}
