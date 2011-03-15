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

 /** A trivial structure to hold the elements of a proof step. */
public class ProofStep {
  
	public static final String 
		QUERY = "[Query]",		
		NEGATED_QUERY = "[Negated Query]",
		INSTANTIATED_QUERY = "[Instantiated Query]";
   
     /** A String of the type clause or formula */
    public String formulaType = null;

     /** A String of the role of the formula */
    public String formulaRole = null;

     /** A String containing a valid KIF expression, that is the axiom 
      *  expressing the conclusion of this proof step. */
    public String axiom = null;

     /** The number assigned to this proof step, initially by Vampire and
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

        // old number, new number
        HashMap<Integer,Integer> numberingMap = new HashMap<Integer,Integer>();
        HashMap<String,Integer> formulaMap = new HashMap<String,Integer>();
        ArrayList<ProofStep> newProofSteps = new ArrayList<ProofStep>();
        
        for (int i = 0; i < proofSteps.size(); i++) {
            ProofStep ps = (ProofStep) proofSteps.get(i);
            Integer index = new Integer(ps.number);
            String s = Clausifier.normalizeVariables(ps.axiom);
            if (formulaMap.keySet().contains(s)) {
            	Integer fNum = (Integer) formulaMap.get(s); 
            	numberingMap.put(index,fNum);
            }
            else {
            	formulaMap.put(s,index);
                Integer newIndex = index;
                if (numberingMap.keySet().contains(index))
                	newIndex = (Integer) numberingMap.get(index);
                ProofStep psNew = new ProofStep();
                psNew.formulaRole = ps.formulaRole;
                psNew.formulaType = ps.formulaType;
                psNew.axiom = s;
                psNew.number = newIndex;
                for (int j = 0; j < ps.premises.size(); j++) {
                    Integer premiseNum = ps.premises.get(j);
                    Integer newNumber = null;
                    if (numberingMap.get(premiseNum) != null) 
                        newNumber = new Integer((Integer) numberingMap.get(premiseNum));
                    else 
                        newNumber = new Integer(premiseNum);                
                    psNew.premises.set(j,newNumber);                    
                }
                newProofSteps.add(psNew);
            }
        }
        return newProofSteps;
    }

    /** ***************************************************************
     */
    public String toString() {

        StringBuffer sb = new StringBuffer();
        sb.append("Proof step: " + number + " with " + 
                  premises.size() + " premises: " + premises + "\n");
        sb.append(axiom);
        return sb.toString();
    }
}
