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

/** Process results from the Vampire inference engine.
 */
public class ProofProcessor {

     /** An ArrayList of BasicXMLelement (s). */
    private ArrayList xml = null;

    /******************************************************************
     * Take an ArrayList of BasicXMLelement (s) and process them as
     * needed
     */
    public ProofProcessor(ArrayList xmlInput) {

        xml = new ArrayList(xmlInput);
        System.out.print("INFO in ProofProcessor(): Number of XML elements is: ");
        System.out.println(xmlInput.size());
    }
    
    /******************************************************************
     * Compare the answer with the expected answer.  Note that this method
     * is very unforgiving in that it requires the exact same format for the 
     * expected answer, including the order of variables.
     */
    public boolean equalsAnswer(int answerNum, String expectedAnswer) {

        StringBuffer result = new StringBuffer();
         /** An ArrayList of BasicXMLelements */
        ArrayList queryResponseElements = ((BasicXMLelement) xml.get(0)).subelements;
        BasicXMLelement answer = (BasicXMLelement) queryResponseElements.get(answerNum);
        if (((String) answer.attributes.get("result")).equalsIgnoreCase("no")) 
            return false;
        if (((String) answer.attributes.get("result")).equalsIgnoreCase("yes") &&
            (expectedAnswer.equalsIgnoreCase("yes"))) 
            return true;
        BasicXMLelement bindingSet = (BasicXMLelement) answer.subelements.get(0);
        if (!((String) bindingSet.attributes.get("type")).equalsIgnoreCase("definite"))
            return false;
        BasicXMLelement binding = (BasicXMLelement) bindingSet.subelements.get(0); 
            // The bindingSet element should just have one subelement, since non-definite answers are rejected.
        for (int j = 0; j < binding.subelements.size(); j++) {
            BasicXMLelement variableBinding = (BasicXMLelement) binding.subelements.get(j);
            String variable = (String) variableBinding.attributes.get("name");
            String value = (String) variableBinding.attributes.get("value");
            result = result.append("(" + variable + " " + value + ")");
            if (j < binding.subelements.size()-1) 
                result = result.append(" ");
        }

        System.out.println("INFO in ProofProcessor().equalsAnswer: answer: " + result.toString() + " expected answer: " + expectedAnswer);
        return result.toString().equalsIgnoreCase(expectedAnswer);
    }

    /******************************************************************
     * Return the variable name and binding for the given answer.
     */
    public String returnAnswer(int answerNum) {

        StringBuffer result = new StringBuffer();
         /** An ArrayList of BasicXMLelements */
        ArrayList queryResponseElements = ((BasicXMLelement) xml.get(0)).subelements;
        BasicXMLelement answer = (BasicXMLelement) queryResponseElements.get(answerNum);
        if (((String) answer.attributes.get("result")).equalsIgnoreCase("no")) 
            return "no";
        BasicXMLelement bindingSet = (BasicXMLelement) answer.subelements.get(0);
        if (bindingSet.tagname.equalsIgnoreCase("proof")) {
            result = result.append("[" + (String) answer.attributes.get("result") + "] ");
            return result.toString();
        }
        result = result.append("[" + (String) bindingSet.attributes.get("type") + "] ");
        for (int i = 0; i < bindingSet.subelements.size(); i++) {
            BasicXMLelement binding = (BasicXMLelement) bindingSet.subelements.get(i);
            for (int j = 0; j < binding.subelements.size(); j++) {
                BasicXMLelement variableBinding = (BasicXMLelement) binding.subelements.get(j);
                String variable = (String) variableBinding.attributes.get("name");
                String value = (String) variableBinding.attributes.get("value");
                result = result.append(variable + " = " + value);
                if (j < binding.subelements.size()-1) 
                    result = result.append("&nbsp;&nbsp;");
            }
            if (i < bindingSet.subelements.size()-1) 
                result = result.append(" , ");
        }
        return result.toString();
    }

    /******************************************************************
     * Remove the $answer clause that Vampire returns, including any
     * surrounding "or".
     */
    private String removeAnswerClause(String st) {

        if (st.indexOf("$answer") == -1) 
            return st;
        st = st.trim();
        if (st.indexOf("$answer") == 1)
            return st.substring(9,st.length()-1);
        if (st.substring(0,3).equalsIgnoreCase("(or")) {
            int answer = st.indexOf("$answer");
            int end = st.indexOf(")",answer);
            st = st.substring(0,answer-1) + st.substring(end+2,st.length());
            return st.substring(3,st.length());
        }
        return st;
    }

    /******************************************************************
     * Return an ArrayList of ProofSteps. It expects that the member variable
     * xml will contain a set of <answer> tags.
     */
    public ArrayList getProofSteps(int answerNum) {
        
        BasicXMLelement proof;
        /** An ArrayList of BasicXMLelements */
        ArrayList queryResponseElements = ((BasicXMLelement) xml.get(0)).subelements;
         /** An ArrayList of ProofSteps */
        ArrayList proofSteps = new ArrayList();
        BasicXMLelement answer = (BasicXMLelement) queryResponseElements.get(answerNum);

        if (!((String) answer.attributes.get("result")).equalsIgnoreCase("no")) {
            BasicXMLelement bindingOrProof = (BasicXMLelement) answer.subelements.get(0);
            if (bindingOrProof.tagname.equalsIgnoreCase("proof")) 
                proof = bindingOrProof;            // No binding set if query is for a true/false answer
            else
                proof = (BasicXMLelement) answer.subelements.get(1);
            ArrayList steps = proof.subelements;

            for (int i = 0; i < steps.size(); i++) {
                BasicXMLelement step = (BasicXMLelement) steps.get(i);
                BasicXMLelement premises = (BasicXMLelement) step.subelements.get(0);
                BasicXMLelement conclusion = (BasicXMLelement) step.subelements.get(1);
                BasicXMLelement conclusionFormula = (BasicXMLelement) conclusion.subelements.get(0);
                ProofStep processedStep = new ProofStep();
                processedStep.axiom = Formula.postProcess(conclusionFormula.contents);
                processedStep.axiom = removeAnswerClause(processedStep.axiom);
                if (conclusionFormula.attributes.containsKey("number"))
                    processedStep.number = new Integer(Integer.parseInt((String) conclusionFormula.attributes.get("number")));
                for (int j = 0; j < premises.subelements.size(); j++) {
                    BasicXMLelement premise = (BasicXMLelement) premises.subelements.get(j);
                    BasicXMLelement formula = (BasicXMLelement) premise.subelements.get(0);
                    Integer premiseNum = new Integer(Integer.parseInt((String) formula.attributes.get("number"),10));
                    processedStep.premises.add((Integer) premiseNum);
                }
                proofSteps.add(processedStep);
            }
        }
        return proofSteps;
    }

    /******************************************************************
     * Return the number of answers contained in this proof.
     */
    public int numAnswers() {

        if (xml == null || xml.size() == 0) 
            return 0;
        BasicXMLelement queryResponse = (BasicXMLelement) xml.get(0);
        if (queryResponse.tagname.equalsIgnoreCase("queryResponse")) {
            return queryResponse.subelements.size()-1;   
        }      // Note that there is a <summary> element under the queryResponse element that shouldn't be counted, hence the -1
        else
            System.out.println("Error in ProofProcessor.numAnswers(): Bad tag: " + queryResponse.tagname);
        return 0;
    }
}
