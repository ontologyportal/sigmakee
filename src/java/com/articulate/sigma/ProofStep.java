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
     
     /** A String of the type clause or formula */
    public String formulaType = null;

     /** A String containing a valid KIF expression, that is the axiom 
      *  expressing the conclusion of this proof step. */
    public String axiom = null;

     /** The number assigned to this proof step, initially by Vampire and
      *  then normalized by ProofStep.normalizeProofStepNumbers() */
    public Integer number = new Integer(0);

     /** An ArrayList of Integer(s), which reference prior proof steps from
      *  which this axiom is derived. 0 signifies the axiom is directly
      *  from the knowledge base, rather than deduced. Note that the 
      *  numbering is what the ProofProcessor assigns, not necessarily
      *  the proof numbers returned directly from Vampire. */
    public ArrayList premises = new ArrayList();
    
    /** ***************************************************************
     * Take an ArrayList of ProofSteps and renumber them consecutively
     * starting at 1.  Update the ArrayList of premises so that they
     * reflect the renumbering.
     */
    public static ArrayList normalizeProofStepNumbers(ArrayList proofSteps) {

        HashMap numberingMap = new HashMap();

        for (int i = 0; i < proofSteps.size(); i++) {
            ProofStep ps = (ProofStep) proofSteps.get(i);
            numberingMap.put(ps.number,new Integer(i + 1));
            ps.number = new Integer(i + 1);
            for (int j = 0; j < ps.premises.size(); j++) {
                ps.premises.set(j,(Integer) numberingMap.get(ps.premises.get(j)));
            }
        }
        return proofSteps;
    }
}
