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

import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.articulate.sigma.trans.TPTP3ProofProcessor;
import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.StringUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tptp_parser.*;

/** Process results from the inference engine.
 */
public class ProofProcessor {

     /** An ArrayList of BasicXMLelement (s). */
    private ArrayList<BasicXMLelement> xml = null;

    /** ***************************************************************
     * Take an ArrayList of BasicXMLelement (s) and process them as
     * needed
     */
    public ProofProcessor(ArrayList<BasicXMLelement> xmlInput) {

    	xml = new ArrayList<BasicXMLelement>(xmlInput);
    }
    
    /** ***************************************************************
     * Compare the answer with the expected answer.  Note that this method
     * is very unforgiving in that it requires the exact same format for the 
     * expected answer, including the order of variables.
     */
    public boolean equalsAnswer(int answerNum, String expectedAnswer) {

    	StringBuffer result = new StringBuffer();
    	ArrayList<BasicXMLelement> queryResponseElements = ((BasicXMLelement) xml.get(0)).subelements;
    	BasicXMLelement answer = queryResponseElements.get(answerNum);
    	if (((String) answer.attributes.get("result")).equalsIgnoreCase("no")) 
    		return false;
    	if (((String) answer.attributes.get("result")).equalsIgnoreCase("yes") &&
    			(expectedAnswer.equalsIgnoreCase("yes"))) 
    		return true;
    	BasicXMLelement bindingSet = (BasicXMLelement) answer.subelements.get(0);
    	if ( bindingSet != null ) {
    		String attr =  (String) bindingSet.attributes.get("type");
    		if ( (attr == null) || !(attr.equalsIgnoreCase("definite")) ) 
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
    	}
    	return result.toString().equalsIgnoreCase(expectedAnswer);
    }

    /** ***************************************************************
     * Looks for skolem function from proofsteps if query is not given.
     * There are two types of skolem functions:
     * one with arguments, for instance: (sk0 Human123) or
     * one without, for example: sk2
     * We need to find either of these in the proofs to see what relationship it goes into
     */
    public static ArrayList<String> returnSkolemStmt(String skolem, ArrayList<TPTPFormula> proofSteps) {

    	if (skolem.startsWith("(") && skolem.endsWith(")")) 
    		skolem = skolem.substring(1, skolem.length()-1);
    	skolem = skolem.split(" ")[0];
    	Pattern pattern = Pattern.compile("(\\([^\\(|.]*?\\(" + skolem + " .+?\\).*?\\)|\\([^\\(|.]*?" + skolem + "[^\\)|.]*?\\))");
    	Matcher match;

    	ArrayList<String> matches = new ArrayList<String>();
    	for (int i = 0; i < proofSteps.size(); i++) {
			TPTPFormula step = proofSteps.get(i);
    		match = pattern.matcher(step.sumo);
    		while (match.find()) {
    			for (int j = 1; j <= match.groupCount(); j++) {
    				if (!matches.contains(match.group(j)))
    					matches.add(match.group(j));
    			}
    		}
    	}        
    	if (matches.size()>0)
    		return matches;
    	return null;
    }

    /** ***************************************************************
     * if the answer clause is found, return null
     */
    private static Formula removeNestedAnswerClauseRecurse(Formula f) {

    	if (StringUtil.emptyString(f.getFormula().trim()))
    		return null;
    	if (f.getFormula().indexOf("answer") == -1)
    		return f;
    	String relation = f.car(); 
    	if (relation.equals("answer")) 
    		return null;
    	if (relation.equals("not")) {
    		Formula fcdar = f.cdrAsFormula().carAsFormula();
    		if (fcdar == null) {
    			System.out.println("Error in ProofProcessor.removeNestedAnswerClauseRecurse(): bad arg to not: '" + f.getFormula() + "'");
    			return null;
    		}
    		Formula fnew = removeNestedAnswerClauseRecurse(fcdar);
    		if (fnew == null)
    			return null;
    		else {
    			Formula result = new Formula();
    			result.read("(not " + fnew.getFormula() + ")");
    			return result;
    		}
    	}
    	boolean connective = false;
    	if (relation.equals("or") || relation.equals("and"))
    		connective = true;
    	ArrayList<Formula> arglist = new ArrayList<Formula>();
    	int arg = 1;
    	boolean foundAnswer = false;
    	String strArgs = "";
    	while (!StringUtil.emptyString(f.getArgument(arg))) {
    		Formula argForm = new Formula();
    		argForm.read(f.getStringArgument(arg));
    		Formula argRes = removeNestedAnswerClauseRecurse(argForm);
    		if (argRes == null) 
    			foundAnswer = true;    
    		else {
    			if (arg > 1)
    				strArgs = strArgs + " ";
    			strArgs = strArgs + argRes.getFormula();
    		}
    		arg = arg + 1;
    	}
    	Formula result = new Formula();
    	if (connective && foundAnswer && arg < 4)
    		result.read(strArgs);
    	else
    		result.read("(" + relation + " " + strArgs + ")");    
    	return result;
    }
    
    /** ***************************************************************
     * Remove the $answer clause that eProver returns, including any
     * surrounding connective.
     */
    public static String removeNestedAnswerClause(String st) {

    	if (st == null || st.indexOf("answer") == -1)
    		return st;
    	// clean the substring with "answer" in it
    	Formula f = new Formula();
    	f.read(st);

		// if there are no nested answers, return the original one
		Formula removeNestedAnswerFormula = removeNestedAnswerClauseRecurse(f);
		if (removeNestedAnswerFormula == null)
			return st;
    	return removeNestedAnswerFormula.getFormula();
    }


    /** ***************************************************************
     * Return the number of answers contained in this proof.
     */
    public int numAnswers() {

    	if (xml == null || xml.size() == 0) 
    		return 0;
    	BasicXMLelement queryResponse = (BasicXMLelement) xml.get(0);
    	if (queryResponse.tagname.equalsIgnoreCase("queryResponse")) 
    		return queryResponse.subelements.size()-1;   
    	// Note that there is a <summary> element under the queryResponse element that shouldn't be counted, hence the -1
    	else
    		System.out.println("Error in ProofProcessor.numAnswers(): Bad tag: " + queryResponse.tagname);
    	return 0;
    }

    /** ***************************************************************
     * Convert XML proof to TPTP format
     */
    public static String tptpProof(ArrayList<ProofStep> proofSteps) {

    	StringBuffer result = new StringBuffer();
    	try {
    		for (int j = 0; j < proofSteps.size(); j++) {
    			ProofStep step = (ProofStep) proofSteps.get(j);
    			boolean isLeaf = step.premises.isEmpty() || 
    					(step.premises.size() == 1 && ((Integer)(step.premises.get(0))).intValue() == 0);
    			//----All are fof because the conversion from SUO-KIF quantifies the variables
    			result.append("fof(");
    			result.append(step.number);
    			result.append(",");
    			if (isLeaf) 
    				result.append("axiom");                
    			else 
    				result.append("plain");                
    			result.append(",");
    			result.append(SUMOformulaToTPTPformula.tptpParseSUOKIFString(step.axiom,false));

    			if (!isLeaf) {
    				result.append(",inference(rule,[],[" + step.premises.get(0));
    				for (int parent = 1; parent < step.premises.size(); parent++) 
    					result.append("," + step.premises.get(parent));                    
    				result.append("])");
    			}
    			result.append("  ).\n");
    		}
    	}
    	catch (Exception ex) {
    		ex.printStackTrace();
    	}
    	return(result.toString());
    }

     /** ***************************************************************
      */
     public static void testRemoveAnswer() {

    	 String stmt = "(not (exists (?VAR1) (and (subclass ?VAR1 Object) " +
    			 "(not (answer (esk1_1 ?VAR1))))))";
    	 System.out.println(removeNestedAnswerClause(stmt));
    	 stmt = "(forall (?VAR1) (or (not (subclass ?VAR1 Object)) " +
    			 "(answer (esk1_1 ?VAR1))))";
    	 System.out.println(removeNestedAnswerClause(stmt));
     }

	 /****************************************************************
      * Tally the number of appearances of a particular axiom label in
      * a file.  This is intended to be used to analyse E output that
      * looks for contradictions, with the intuition that axioms that
      * appear in most or all of the contractions found are the source
      * of the problem.
	 */
	 public static void tallyAxioms(String file) {

         HashMap<String,String> axioms = new HashMap<>();
         HashMap<String,Integer> counts = new HashMap<>();
		 LineNumberReader lr = null;
		 try {
			 String line;
			 File f = new File(file);
			 if (f == null) {
				 System.out.println("Error in ProofProcessor.tallyAxioms(): The file does not exist " + file);
				 return;
			 }
			 FileReader r = new FileReader(f);
			 // System.out.println( "INFO in WordNet.readNouns(): Reading file " + nounFile.getCanonicalPath() );
			 lr = new LineNumberReader(r);
			 String kbName = KBmanager.getMgr().getPref("sumokbname");
			 while ((line = lr.readLine()) != null) {
				 if (lr.getLineNumber() % 1000 == 0)
					 System.out.print('.');
                 Pattern p = Pattern.compile("fof\\((kb_" + kbName + "_\\d+)");
				 Matcher m = p.matcher(line);
				 if (m.find()) {
					 axioms.put(m.group(1),line);
                     Integer i = counts.get(m.group(1));
                     if (i == null)
                         counts.put(m.group(1),Integer.valueOf(1));
                     else {
                         i++;
                         counts.put(m.group(1), i);
                     }
				 }
			 }
		 }
		 catch (Exception ex) {
			 ex.printStackTrace();
		 }

         Iterator<String> it = axioms.keySet().iterator();
         while (it.hasNext()) {
             String key = it.next();
             String val = axioms.get(key);
             System.out.println(key + "\t" + val);
         }

         System.out.println();
         it = counts.keySet().iterator();
         while (it.hasNext()) {
             String key = it.next();
             Integer val = counts.get(key);
             System.out.println(key + "\t" + val);
         }
         return;
	 }

	  /** ***************************************************************
       */
      public static void testFormatProof() {

    	  try {
    		  KBmanager.getMgr().initializeOnce();
    		  KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
    		  String stmt = "(subclass ?X Entity)";
    		  String result = kb.askEProver(stmt, 30, 3) + " ";
			  TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
			  StringBuffer qlist = new StringBuffer();
			  qlist.append("?X");
			  tpp.parseProofOutput(result,kb);
    		  result = HTMLformatter.formatTPTP3ProofResult(tpp,stmt,"<hr>\n",
					  KBmanager.getMgr().getPref("sumokbname"),"EnglishLanguage");
    		  System.out.println(result);
    	  } 
    	  catch (Exception ex) {
    		  System.out.println(ex.getMessage());
    	  }
      }

	/** ***************************************************************
	 */
	public static void testFormatProof2(String filename) {

		try {
			KBmanager.getMgr().initializeOnce();
			KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
			List<String> lines = TPTP3ProofProcessor.joinLines((ArrayList<String>) FileUtil.readLines(filename,false));
			String query = "";
			StringBuffer answerVars = new StringBuffer("");
			TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
			tpp.parseProofOutput((ArrayList<String>) lines, query, kb,answerVars);
			String result = HTMLformatter.formatTPTP3ProofResult(tpp,"","<hr>\n",
					KBmanager.getMgr().getPref("sumokbname"),"EnglishLanguage");
			System.out.println(result);
		}
		catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	/** ***************************************************************
	 */
	public static void showHelp() {

		System.out.println("ProofProcessor class");
		System.out.println("  options (with a leading '-'):");
		System.out.println("  f <filename> - format a proof from a file");
		System.out.println("  h - show this help");
	}

	/** ***************************************************************
	*  A main method, used only for testing.  It should not be called
	*  during normal operation.
	*/
	public static void main (String[] args) {

	   System.out.println("INFO in ProofProcessor.main()");
	   System.out.println("args:" + args.length + " : " + Arrays.toString(args));
	   if (args == null) {
		   System.out.println("no command given");
		   showHelp();
	   }
	   else if (args != null && args.length > 0 && args[0].equals("-h"))
		   showHelp();
	   else {
		   if (args.length > 1 && args[0].equals("-f"))
		   		testFormatProof2(args[1]);
	   }
	}
}
