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

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     
    public String returnAnswer(int answerNum) {
    	return returnAnswer(answerNum, "");
    }
    */
    /** ***************************************************************
     * Return the variable name and binding for the given answer.
     
    public String returnAnswer(int answerNum, String query) {

    	StringBuffer result = new StringBuffer();
    	ArrayList<String> skolemTypes = new ArrayList<String>();
    	//An ArrayList of BasicXMLelements 
    	ArrayList<BasicXMLelement> queryResponseElements = ((BasicXMLelement) xml.get(0)).subelements;
    	BasicXMLelement answer = queryResponseElements.get(answerNum);
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
    			//see if a skolem function is present in the value (skolem functions are labeled sk[0-9]+
    			if (value.matches(".*?sk[0-9]+.*?")) {
    				String skolemType = findSkolemType(answerNum, value, query, variable);
    				if (skolemType != "")
    					skolemTypes.add("; " + value + " is of type " + skolemType);    				
    			}
    			result = result.append(variable + " = " + value);
    			if (j < binding.subelements.size()-1) 
    				result = result.append(",  ");
    		}
    		if (i < bindingSet.subelements.size()-1) 
    			result = result.append(" , ");
    		while (skolemTypes.size() > 0) {
    			result = result.append(skolemTypes.get(0));
    			skolemTypes.remove(0);
    		}
    		result.append(";");
    	}
    	return result.toString();
    }
*/
    /** ***************************************************************
     * Looks for skolem function from proofsteps if query is not given.
     * There are two types of skolem functions:
     * one with arguments, for instance: (sk0 Human123) or
     * one without, for example: sk2
     * We need to find either of these in the proofs to see what relationship it goes into
     */
    public static ArrayList<String> returnSkolemStmt(String skolem, ArrayList<ProofStep> proofSteps) {

    	if (skolem.startsWith("(") && skolem.endsWith(")")) 
    		skolem = skolem.substring(1, skolem.length()-1);
    	skolem = skolem.split(" ")[0];
    	Pattern pattern = Pattern.compile("(\\([^\\(|.]*?\\(" + skolem + " .+?\\).*?\\)|\\([^\\(|.]*?" + skolem + "[^\\)|.]*?\\))");
    	Matcher match;

    	ArrayList<String> matches = new ArrayList<String>();
    	for (int i = 0; i < proofSteps.size(); i++) {
    		ProofStep step = (ProofStep) proofSteps.get(i);
    		match = pattern.matcher(step.axiom);
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
     * looks for skolem variable if a query string is given
     */
    private ArrayList<String> returnSkolemStmt(String query, String variable) {

    	if (!StringUtil.emptyString(query)) {
    		query = query.replaceAll("\\" + variable, "_SKOLEM");
    		Pattern pattern = Pattern.compile("(\\([^\\(\\)]*?_SKOLEM[^\\)\\(]*?\\))");
    		Matcher match = pattern.matcher(query);

    		while (match.find()) {
    			ArrayList<String> matches = new ArrayList<String>();
    			for (int i = 1; i <= match.groupCount(); i++) {
    				if (!matches.contains(match.group(i)))
    					matches.add(match.group(i));
    			}
    			return matches;   
    		}
    	}    
    	return null;
    }

    /** *********************************************************************************
     * @param answerNum The nth answer in the result set
     * @param value The value in the bindingSet being analyzed
     
    private String findSkolemType(int answerNum, String value, String query, String variable) {   

    	ArrayList<ProofStep> proofSteps = getProofSteps(answerNum); 
    	ArrayList<String> skolemRelationArr;
    	// try and look for the skolem function in the proofSteps and determine the 
    	// relation statement it appears in
    	if (query == "")
    		skolemRelationArr = returnSkolemStmt(value, proofSteps);
    	else
    		skolemRelationArr = returnSkolemStmt(query, variable);

    	if (skolemRelationArr != null) {   
    		for (int j = 0; j < skolemRelationArr.size(); j++) {
    			String skolemRelation = skolemRelationArr.get(j);
    			skolemRelation = skolemRelation.substring(1, skolemRelation.length()-1);

    			// prepare skolem function to have the form sk0 .+? or sk0 (for skolem functions that don't have an argument)
    			// because value from answer contains an instance and not necessarily a variable
    			String skolem = value;
    			if(skolem.startsWith("(") && skolem.endsWith(")"))
    				skolem = value.substring(1, value.length()-1);
    			skolem = skolem.split(" ")[0];

    			// remove skolem and replace with temp variable
    			skolemRelation = skolemRelation.replaceAll("\\("+ skolem + " [^\\)]+?\\)", "_SKOLEM");
    			skolemRelation = skolemRelation.replaceAll(skolem, "_SKOLEM");
    			// remove all other skolem functions in the skolemRelation (if present) and replace with temp variable
    			skolemRelation = skolemRelation.replaceAll("\\(.+?\\)", "?TEMP");
    			//LOGGER.finest("skolemRelation: " + skolemRelation);

    			if (skolemRelation.matches("instance\\s_SKOLEM\\s[^\\s]+")) {
    				String[] arguments = skolemRelation.split(" ");
    				return arguments[arguments.length-1];
    			}

    			// assemble regex for skolemRelation
    			String[] skolemArguments = skolemRelation.split(" ");
    			StringBuffer regexStmt = new StringBuffer();

    			for (int i = 0; i < skolemArguments.length; i++) {
    				if (skolemArguments[i].equals("_SKOLEM")) 
    					regexStmt.append("([^\\s\\(\\)]+) ");    
    				else if (skolemArguments[i].startsWith("?") || i!=0) 
    					regexStmt.append("[^\\s\\(\\)]+ ");    
    				else 
    					regexStmt.append(skolemArguments[i] + " ");    
    			}

    			regexStmt.deleteCharAt(regexStmt.length()-1);
    			regexStmt.insert(0, "\\(");
    			regexStmt.insert(regexStmt.length(), "\\)");

    			// resulting regexStmt from something like (relationshipName ?X0 _SKOLEM)
    			// should be \\(relationshipName [^\\s\\(\\)]+ ([^\\s\\(\\)]+)\\)    
    			Pattern pattern = Pattern.compile(regexStmt.toString());
    			Matcher match;

    			// look for the presence of above pattern in each of the proof steps
    			for (int i = 0; i < proofSteps.size(); i++){
    				ProofStep proof = (ProofStep)proofSteps.get(i);
    				String varName = "";
    				match = pattern.matcher(proof.axiom);    
    				boolean varNameFound = false;

    				// if it is found, extract the variable name being used
    				// and then see if an (instance ?VARNAME ?CLASS) relationship can be found
    				// that defines the class membership of varName
    				while (match.find()) {
    					int k = 1;
    					while(k <= match.groupCount() && !varNameFound) {
    						varName = match.group(k);
    						if (varName.startsWith("?"))
    							varNameFound = true;
    						k++;    
    						if (varNameFound) {
    							String regexString = ".*?\\(instance \\" + varName + " ([^\\s\\)]+)\\).*?";
    							Pattern varPattern = Pattern.compile(regexString);
    							match = varPattern.matcher(proof.axiom);
    							if (match.find()) {
    								if (match.group(1) != null)
    									return match.group(1);    
    								else varNameFound = false;    
    							}
    							else varNameFound = false;
    						}
    					}
    				}
    			}
    		}
    	}
    	return "cannot be determined.";
    }
*/
    /** ***************************************************************
     * if the answer clause is found, return null
     */
    private static Formula removeNestedAnswerClauseRecurse(Formula f) {

    	if (StringUtil.emptyString(f.theFormula.trim()))
    		return null;
    	if (f.theFormula.indexOf("answer") == -1)
    		return f;
    	String relation = f.car(); 
    	if (relation.equals("answer")) 
    		return null;
    	if (relation.equals("not")) {
    		Formula fcdar = f.cdrAsFormula().carAsFormula();
    		if (fcdar == null) {
    			System.out.println("Error in ProofProcessor.removeNestedAnswerClauseRecurse(): bad arg to not: '" + f.theFormula + "'");
    			return null;
    		}
    		Formula fnew = removeNestedAnswerClauseRecurse(fcdar);
    		if (fnew == null)
    			return null;
    		else {
    			Formula result = new Formula();
    			result.read("(not " + fnew.theFormula + ")");    
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
    		argForm.read(f.getArgument(arg));
    		Formula argRes = removeNestedAnswerClauseRecurse(argForm);
    		if (argRes == null) 
    			foundAnswer = true;    
    		else {
    			if (arg > 1)
    				strArgs = strArgs + " ";
    			strArgs = strArgs + argRes.theFormula;
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
    	return removeNestedAnswerFormula.theFormula;
    }

    /** ***************************************************************
     * Remove the $answer clause that Vampire returns, including any
     * surrounding "or".
     
    private static String removeAnswerClause(String st) {

    	if (st.indexOf("$answer") == -1)
    		return st;
    	// clean the substring with "answer" in it
    	st = st.replaceAll("\\(\\$answer\\s[\\(sk[0-9]+\\s[^\\)]+?\\)|[^\\(\\)]+?]+?\\)", "");

    	//count number of nested statements if statement starts with (or
    	//if nested statements is more than 2, keep or. If it is exactly 2 --
    	//which means it's just (or plus one other statement, remove or.
    	if (st.substring(0,3).equalsIgnoreCase("(or") || st.substring(0,3).equalsIgnoreCase("(and")){
    		boolean done = false;
    		String substr = st.substring(4, st.length()-1);
    		while (!done) {
    			String statement = " SUMO-AXIOM";
    			substr = substr.replaceAll("\\([^\\(|^\\)]+\\)", statement);
    			substr = substr.replaceAll("\\(not\\s[^\\(|^\\)]+\\)", statement);
    			if (substr.indexOf("(") == -1) 
    				done = true;        
    		}        
    		substr = substr.trim();        
    		if (substr.split(" ").length <= 2) {
    			st = st.substring(4, st.length()-1);
    		}
    	}         
    	return st;     
    }
*/
    /** ***************************************************************
     * Return an ArrayList of ProofSteps. It expects that the member variable
     * xml will contain a set of <answer> tags.
     
    public ArrayList<ProofStep> getProofSteps(int answerNum) {

    	BasicXMLelement proof;
    	ArrayList<BasicXMLelement> queryResponseElements = ((BasicXMLelement) xml.get(0)).subelements;
    	ArrayList<ProofStep> proofSteps = new ArrayList<ProofStep>();
    	BasicXMLelement answer = (BasicXMLelement) queryResponseElements.get(answerNum);

    	if (!((String) answer.attributes.get("result")).equalsIgnoreCase("no")) {
    		BasicXMLelement bindingOrProof = (BasicXMLelement) answer.subelements.get(0);
    		if (bindingOrProof.tagname.equalsIgnoreCase("proof")) 
    			proof = bindingOrProof;            // No binding set if query is for a true/false answer
    		else 
    			proof = (BasicXMLelement) answer.subelements.get(1);

    		ArrayList<BasicXMLelement> steps = proof.subelements;
    		for (int i = 0; i < steps.size(); i++) {
    			BasicXMLelement step = (BasicXMLelement) steps.get(i);
    			BasicXMLelement premises = (BasicXMLelement) step.subelements.get(0);
    			BasicXMLelement conclusion = (BasicXMLelement) step.subelements.get(1);
    			BasicXMLelement conclusionFormula = (BasicXMLelement) conclusion.subelements.get(0);
    			ProofStep processedStep = new ProofStep();
    			processedStep.formulaType = ((BasicXMLelement) conclusion.subelements.get(0)).tagname;
    			processedStep.axiom = Formula.postProcess(conclusionFormula.contents);

    			if (i == steps.size() - 1) 
    				processedStep.axiom = processedStep.axiom.replaceAll("\\$answer[\\s|\\n|\\r]+", "");                
    			else
    				processedStep.axiom = removeAnswerClause(processedStep.axiom);
    			//----If there is a conclusion role, record
    			if (conclusion.subelements.size() > 1) {
    				BasicXMLelement conclusionRole = (BasicXMLelement) conclusion.subelements.get(1);
    				if (conclusionRole.attributes.containsKey("type")) 
    					processedStep.formulaRole = (String) conclusionRole.attributes.get("type");                        
    			}
    			if (conclusionFormula.attributes.containsKey("number")) {
    				processedStep.number = new Integer(Integer.parseInt((String) conclusionFormula.attributes.get("number")));
    			}
    			for (int j = 0; j < premises.subelements.size(); j++) {
    				BasicXMLelement premise = (BasicXMLelement) premises.subelements.get(j);
    				BasicXMLelement formula = (BasicXMLelement) premise.subelements.get(0);
    				Integer premiseNum = new Integer(Integer.parseInt((String) formula.attributes.get("number"),10));
    				processedStep.premises.add(premiseNum);
    			}
    			proofSteps.add(processedStep);
    		}
    	}
    	return proofSteps;
    }
*/
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
     *  A method used only for testing.  It should not be called
     *  during normal operation.
     
    public static void test (String[] args) {

    	try {
    		FileReader r = new FileReader(args[0]);
    		LineNumberReader lr = new LineNumberReader(r);
    		String line;
    		StringBuffer result = new StringBuffer();
    		while ((line = lr.readLine()) != null) 
    			result.append(line + "\n");            

    		BasicXMLparser res = new BasicXMLparser(result.toString());
    		result = new StringBuffer();
    		ProofProcessor pp = new ProofProcessor(res.elements);
    		for (int i = 0; i < pp.numAnswers(); i++) {
    			ArrayList<ProofStep> proofSteps = pp.getProofSteps(i);
    			proofSteps = new ArrayList<ProofStep>(ProofStep.normalizeProofStepNumbers(proofSteps));
    			if (i != 0) 
    				result.append("\n");               
    			result.append("%----Answer " + (i+1) + " " + pp.returnAnswer(i,"") + "\n");
    			if (!pp.returnAnswer(i).equalsIgnoreCase("no")) 
    				result.append(tptpProof(proofSteps));               
    		}
    		System.out.println(result.toString());
    	}
    	catch (IOException ioe) {
    		System.out.println("Error in ProofProcessor.main(): IOException: " + ioe.getMessage());
    	}     
    }   
     */
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
			 while ((line = lr.readLine()) != null) {
				 if (lr.getLineNumber() % 1000 == 0)
					 System.out.print('.');
                 Pattern p = Pattern.compile("fof\\((kb_SUMO_\\d+)");
				 Matcher m = p.matcher(line);
				 if (m.find()) {
					 axioms.put(m.group(1),line);
                     Integer i = counts.get(m.group(1));
                     if (i == null)
                         counts.put(m.group(1),new Integer(1));
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
    		  KB kb = KBmanager.getMgr().getKB("SUMO");
    		  String stmt = "(subclass ?X Entity)";
    		  String result = kb.ask(stmt, 30, 3) + " ";
    		  result = HTMLformatter.formatProofResult(result,stmt,stmt,"<hr>\n","SUMO","EnglishLanguage");
    		  System.out.println(result);
    	  } 
    	  catch (Exception ex) {
    		  System.out.println(ex.getMessage());
    	  }
      }

     /** ***************************************************************
      *  A main method, used only for testing.  It should not be called
      *  during normal operation.
      */
       public static void main (String[] args) {

    	   //testRemoveAnswer();
		   tallyAxioms(args[0]);
       }
}
