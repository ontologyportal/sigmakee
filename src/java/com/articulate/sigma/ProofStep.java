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

import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import com.articulate.sigma.utils.FileUtil;

import java.util.ArrayList;
import java.util.HashMap;

 /** A trivial structure to hold the elements of a proof step. */
public class ProofStep {

    public static boolean debug = false;

	public static final String QUERY = "[Query]";
	public static final String NEGATED_QUERY = "[Negated Query]";
	public static final String INSTANTIATED_QUERY = "[Instantiated Query]";

	// the TPTP3 input
	public String input = null;

     /** A String giving the type of the clause or formula, such as 'conjecture', 'plain' or 'axiom' */
    public String formulaType = null;

     /** A String of the role of the formula */
    public String formulaRole = null;

     /** A String of the inference type, e.g. add_answer_literal | assume_negation | etc. */
     public String inferenceType = null;

     /** A String containing a valid SUO-KIF expression, that is the axiom
      *  expressing the conclusion of this proof step. */
    public String axiom = null;

    // ID of the SUMO axiom that corresponds to kb.axiomKey.keySet()
    public String sourceID = "";

     /** The number assigned to this proof step, initially by EProver and
      *  then normalized by ProofStep.normalizeProofStepNumbers() */
    public Integer number = 0;

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
        HashMap<Integer,Integer> numberingMap = new HashMap<>();
        System.out.println("INFO in ProofStep.normalizeProofStepNumbers(): begin with " + proofSteps.size() + " steps ");
        //if (debug) System.out.println("INFO in ProofStep.normalizeProofStepNumbers(): before: " + proofSteps);
        int newIndex = 1;
        for (int i = 0; i < proofSteps.size(); i++) {
            //System.out.println("INFO in ProofStep.normalizeProofStepNumbers(): numberingMap: " + numberingMap);
            ProofStep ps = (ProofStep) proofSteps.get(i);
            //System.out.println("INFO in ProofStep.normalizeProofStepNumbers(): Checking proof step: " + ps);
            Integer oldIndex = Integer.valueOf(ps.number);
            if (numberingMap.containsKey(oldIndex)) 
                ps.number = (Integer) numberingMap.get(oldIndex);
            else {
                //System.out.println("INFO in ProofStep.normalizeProofStepNumbers(): adding new step: " + newIndex);
                ps.number = Integer.valueOf(newIndex);
                numberingMap.put(oldIndex,Integer.valueOf(newIndex++));
            }
            for (int j = 0; j < ps.premises.size(); j++) {
                Integer premiseNum = ps.premises.get(j);
                //System.out.println("INFO in ProofStep.normalizeProofStepNumbers(): old premise num: " + premiseNum);
                Integer newNumber = null;
                if (numberingMap.get(premiseNum) != null) 
                    newNumber = Integer.valueOf((Integer) numberingMap.get(premiseNum));
                else {
                    newNumber = Integer.valueOf(newIndex++);
                    numberingMap.put(premiseNum,Integer.valueOf(newNumber));
                }
                //System.out.println("INFO in ProofStep.normalizeProofStepNumbers(): new premise num: " + newNumber);
                ps.premises.set(j,newNumber);
            }
        }
        //if (debug) System.out.println("INFO in ProofStep.normalizeProofStepNumbers(): after: " + proofSteps);
        return proofSteps;
    }

    /** ***************************************************************
     * Remove duplicate statements in the proof
     */
    public static ArrayList<ProofStep> removeDuplicates(ArrayList<ProofStep> proofSteps) {

        if (debug) System.out.println("INFO in ProofStep.removeDuplicates(): before: " + proofSteps);
        //	System.out.println("INFO in ProofSteps.removeDuplicates()");
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
            ProofStep ps = proofSteps.get(i);
            Integer index = ps.number;
            reverseFormulaMap.put(index,ps);
            String s = Clausifier.normalizeVariables(ps.axiom);
            if (formulaMap.keySet().contains(s) && ps.premises.size() == 1) {   // If the step is a duplicate, relate the current step number
            	Integer fNum = formulaMap.get(s);                   // to the existing number of the formula
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
            ProofStep ps = dedupedProofSteps.get(i);
        	Integer newIndex = Integer.valueOf(ps.number);
        	if (numberingMap.keySet().contains(newIndex))
        		newIndex = numberingMap.get(newIndex);
        	ProofStep psNew = new ProofStep();
        	psNew.formulaRole = ps.formulaRole;
        	psNew.formulaType = ps.formulaType;
            psNew.inferenceType = ps.inferenceType;
            String s = Clausifier.normalizeVariables(ps.axiom);
        	psNew.axiom = s;
        	psNew.number = newIndex;
        	psNew.input = ps.input;
        	ArrayList<Integer> newPremises = new ArrayList();        	   
        	for (int j = 0; j < ps.premises.size(); j++) {
        		Integer premiseNum = ps.premises.get(j);
        		Integer newNumber = null;
        		if (numberingMap.get(premiseNum) != null) 
        			newNumber = Integer.valueOf((Integer) numberingMap.get(premiseNum));
        		else 
        			newNumber = Integer.valueOf(premiseNum);
        		newPremises.add(newNumber);                    
        	}
        	psNew.premises = newPremises;
        	newProofSteps.add(psNew);            
        }
        if (debug) System.out.println("INFO in ProofStep.removeDuplicates(): after: " + newProofSteps);
        return newProofSteps;
    }

    /** ***************************************************************
     * created by qingqing
     * remove unnecessary steps, which should not appear in proof
     * Unnecessary steps could be:
     * (1) conjecture
     * (2) duplicate $false;
     */
    public static ArrayList<ProofStep> removeUnnecessary(ArrayList<ProofStep> proofSteps) {

        if (debug) System.out.println("INFO in ProofStep.removeUnnecessary(): before: " + proofSteps);
        ArrayList<ProofStep> results = new ArrayList<ProofStep>();
        boolean firstTimeSeeFALSE = true;
        for (int i = 0; i < proofSteps.size(); i++) {
            ProofStep ps = proofSteps.get(i);
            if (ps.formulaType != null &&
                    !ps.formulaType.equals("conjecture") ) { // conjecture is not allowed in the proof step
                if (ps.axiom.equalsIgnoreCase("FALSE")) {
                    if (firstTimeSeeFALSE) {    // only add when it is the first time to see contradiction
                        results.add(ps);
                        firstTimeSeeFALSE = false;
                    }
                }
                else if (ps.formulaType.equals("negated_conjecture")) { // negated conjecture has no supports
                    ps.premises = new ArrayList<>();
                    results.add(ps);
                }
                else
                    results.add(ps);
            }
        }
        if (debug) System.out.println("INFO in ProofStep.removeUnnecessary(): after: " + proofSteps);
        results = normalizeProofStepNumbers(results);
        return results;
    }

    /** ***************************************************************
     */
    public String toString() {

        StringBuffer sb = new StringBuffer();
        sb.append(number + ". " + new Formula(axiom).format("","  ","\n") + " " + premises + " ");
        if (inferenceType.startsWith("kb_")) {
            Formula originalF = SUMOKBtoTPTPKB.axiomKey.get(inferenceType);
            if (originalF != null) {
                sb.append(inferenceType + ":" + originalF.startLine + ":" + FileUtil.noPath(originalF.getSourceFile()) + "\n");
                if (originalF.derivation != null &&
                        originalF.derivation.operator != null &&
                        !originalF.derivation.operator.equals("input"))
                    sb.append(originalF.derivation.toString());
            }
            else
                sb.append(inferenceType + "\n");

        }
        else
            sb.append(inferenceType + "\n");
        return sb.toString();
    }
}
