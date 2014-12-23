package com.articulate.sigma;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.*;

import TPTPWorld.TPTPFormula;
import TPTPWorld.TPTPParser;

public class TPTP3ProofProcessor {

	public String status;
	public ArrayList<String> bindings = new ArrayList<String>();
	public ArrayList<ProofStep> proof = new ArrayList<ProofStep>();
	private HashMap<String,Integer> idTable = new HashMap<String,Integer>();
	int idCounter = 0;

	/** ***************************************************************
	 */
	public String toString () {

		StringBuffer sb = new StringBuffer();
		sb.append("Answers:");
		for (int i = 0; i < bindings.size(); i++)
			sb.append(bindings.get(i) + " ");
		for (int i = 0; i < proof.size(); i++)
			sb.append(proof.get(i));
		return sb.toString();
	}
	
    /** ***************************************************************
     */
	public static String trimBrackets (String line) {

		// System.out.println("Info in trimBrackets: " + line);
		if (line.startsWith("[") && line.endsWith("]"))
			return line.substring(1,line.length()-1);
		else {
			System.out.println("Error in TPTP3ProofProcessor.trimBrackets() bad format: " + line);
			return null;
		}
	}
     
    /** ***************************************************************
     */
	public static String trimParens (String line) {

		// System.out.println("Info in trimBrackets: " + line);
		if (line.startsWith("(") && line.endsWith(")"))
			return line.substring(1,line.length()-1);
		else {
			System.out.println("Error in TPTP3ProofProcessor.trimParens() bad format: " + line);
			return null;
		}
	}
	
	/** ***************************************************************
     */
	public ArrayList<Integer> parseInferenceObject(String supportId) {
		
		//System.out.println("Info in TPTP3ProofProcessor.parseInferenceObject(): " + supportId);

		ArrayList<Integer> prems = new ArrayList<Integer>();
		int firstParen = supportId.indexOf("(");
		int secondParen = StringUtil.findBalancedParen(firstParen, supportId);
		int firstComma = supportId.indexOf(",");
		int secondComma = supportId.indexOf(",",firstComma+1);
		String supports = supportId.substring(secondComma+1,secondParen);
		//System.out.println("Info in TPTP3ProofProcessor.parseInferenceObject(): supports: " + supports);

		if (supports.startsWith("inference")) 
			return parseInferenceObject(supports);
		else 
			return parseSupports(supports.trim()); 
	}

	/** ***************************************************************
     */
	public ArrayList<Integer> parseSupports(String supportId) {

		//System.out.println("Info in TPTP3ProofProcessor.parseSupports(): " + supportId);

		ArrayList<Integer> prems = new ArrayList<Integer>();
		if (supportId.startsWith("[")) {
			supportId = trimBrackets(supportId).trim();
			//System.out.println("Info in TPTP3ProofProcessor.parseSupports()2: " + supportId);
			if (supportId.startsWith("inference(")) 
				return parseInferenceObject(supportId);
			String[] supportSet = supportId.split(",");
			for (int i = 0; i < supportSet.length; i++) {
				//System.out.println("Info in TPTP3ProofProcessor.parseSupports(): support element: " + supportSet[i]);
				if (supportSet[i].indexOf("(") == -1) {
					Integer stepnum = idTable.get(supportSet[i].trim());
					if (stepnum == null) 
						System.out.println("Error in TPTP3ProofProcessor.parseSupports() no id: " + stepnum +
								" for premises at step " + supportSet[i]);
					else
						prems.add(stepnum);
				}
			}
		}
		else if (supportId.startsWith("file(")) {
			return prems;
		}
		else if (supportId.startsWith("inference(")) {
			return parseInferenceObject(supportId);
		}
		else if (supportId.startsWith("introduced(")) {
			return prems;
		}
		else {
			Integer stepnum = idTable.get(supportId);
			if (stepnum == null) {
				System.out.println("Error in TPTP3ProofProcessor.parseSupports() no id: " + stepnum +
						" for premises at step " + supportId);
				return prems;
			}
			else {
				prems.add(stepnum);
				return prems;
			}
		}    		
		return prems;
	}
	
    /** ***************************************************************
     * Parse a step like the following into its constituents
     *   fof(c_0_5, axiom, (s__subclass(s__Artifact,s__Object)), c_0_3).
     *   
     *   fof(c_0_2, 
     *       negated_conjecture, 
     *       (~(?[X1]:(s__subclass(X1,s__Object)&~$answer(esk1_1(X1))))), 
     *       inference(assume_negation,[status(cth)],[inference(add_answer_literal,[status(thm)],[c_0_0, theory(answers)])])).
     */
	public ProofStep parseProofStep (String line) {

		if (StringUtil.emptyString(line))
			return null;
		ProofStep ps = new ProofStep();
		//System.out.println("Info in TPTP3ProofProcessor.parseProofStep(): " + line);
		int paren = line.indexOf("(");
		if (paren == -1) {
			System.out.println("Error in TPTP3ProofProcessor.parseProofStep() bad format: " + line);
			return null;
		}
		String type = line.substring(0,paren);
		if (!line.endsWith(").")) {
			System.out.println("Error in TPTP3ProofProcessor.parseProofStep() bad format: " + line);
			return null;
		}
		String withoutWrapper = line.substring(paren + 1,line.length() - 2);
		int comma1 = withoutWrapper.indexOf(",");
		String id = withoutWrapper.substring(0,comma1).trim();
		// System.out.println("ID       : " + id);
		Integer intID = new Integer(idCounter++);
		idTable.put(id,intID);
		ps.number = intID;

		int comma2 = withoutWrapper.indexOf(",",comma1+1);
		String formulaType = withoutWrapper.substring(comma1 + 1,comma2).trim();
		// System.out.println("type     : " + formulaType);
		String rest = withoutWrapper.substring(comma2+1).trim();
		int statementEnd = StringUtil.findBalancedParen(0, rest);
		
		String stmnt = trimParens(rest.substring(0,statementEnd+1).trim());
		// System.out.println("stmnt    : " + stmnt);
		//line = line.replaceAll("\\$answer\\(","answer(");
		//System.out.println("after remove $answer: " + line);
		StringReader reader = new StringReader(line);
		// kif = TPTP2SUMO.convert(reader, false);
		try {
			TPTPParser tptpP = TPTPParser.parse(new BufferedReader(reader));
			// System.out.println(tptpP.Items.get(0));
			Iterator<String> it = tptpP.ftable.keySet().iterator();
			while (it.hasNext()) {
				String tptpid = it.next();
				TPTPFormula tptpF = tptpP.ftable.get(tptpid);
				stmnt = TPTP2SUMO.convertType(tptpF,0,0,true).toString();
			}
		}
		catch (Exception e) {
			System.out.println("Error in TPTP3ProofProcessor.parseProofStep(): " + e.getMessage());
			e.printStackTrace();
			System.out.println("with input: " + line);
		}
		// System.out.println("KIF stmnt : " + stmnt);
		ps.axiom = stmnt;
		String supportId = rest.substring(statementEnd+2,rest.length()).trim();     	 
		// System.out.println("supportID: " + supportId);
		ps.premises.addAll(parseSupports(supportId.trim())); 
		return ps;
	}
      
    /** ***************************************************************
     */
	public void processAnswers (String line) {

		//System.out.println("Info in TPTP3ProofProcessor.processAnswers(): " + line);
		String trimmed = trimBrackets(line);
		if (trimmed == null) {
			System.out.println("Error in TPTP3ProofProcessor.processAnswers() bad format: " + line);
			return;
		}
		String[] answers = trimmed.split("\\|");
		for (int i = 0; i < answers.length; i++) {
			if (answers[i].equals("_"))
				break;
			String answer = trimBrackets(answers[i]);
			if (answer != null)
				bindings.add(answer);
		}
	}
     
     /** ***************************************************************
      */
     public void printAnswers () {

    	 System.out.println("Answers:");
    	 for (int i = 0; i < bindings.size(); i++)
    		 System.out.println(bindings.get(i));
     }

      /** ***************************************************************
       */
     public static void testParseProofStep () {

    	 String ps1 = "fof(c_0_5, axiom, (s__subclass(s__Artifact,s__Object)), c_0_3).";
    	 String ps2 = "fof(c_0_2, negated_conjecture,(~(?[X1]:(s__subclass(X1,s__Object)&~$answer(esk1_1(X1)))))," + 
    			 "inference(assume_negation,[status(cth)],[inference(add_answer_literal,[status(thm)],[c_0_0, theory(answers)])])).";
    	 String ps3 = "cnf(c_0_14,negated_conjecture,($false), " +
    			 "inference(eval_answer_literal,[status(thm)], [inference(spm,[status(thm)],[c_0_12, c_0_13, theory(equality)]), theory(answers)]), ['proof']).";
    	 TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
    	 tpp.idTable.put("c_0_0", new Integer(0));
    	 tpp.idTable.put("c_0_3", new Integer(1));    	 
    	 tpp.idTable.put("c_0_12", new Integer(2));
    	 tpp.idTable.put("c_0_13", new Integer(3));
    	 System.out.println(tpp.parseProofStep(ps1));
    	 System.out.println();
    	 System.out.println(tpp.parseProofStep(ps1));
    	 System.out.println();
    	 System.out.println(tpp.parseProofStep(ps3));
     }

     /** ***************************************************************
      */
    public static TPTP3ProofProcessor parseProofOutput (LineNumberReader lnr) {
    	
       	TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
    	try {
    		boolean inProof = false;
    		String line;
    		while ((line = lnr.readLine()) != null) {
    			if (line.indexOf("SZS output start") != -1) {
    				inProof = true;
    				line = lnr.readLine();
    			}
    			if (line.indexOf("SZS status") != -1) {
    				tpp.status = line.substring(15);
    			}
    			if (line.indexOf("SZS answers") != -1) {
    				tpp.processAnswers(line.substring(20).trim());
    				tpp.printAnswers();
    			}
    			if (inProof) {
    				if (line.indexOf("SZS output end") != -1) {
    					inProof = false;
    				}
    				else {
    					ProofStep ps = tpp.parseProofStep(line);
    					if (ps != null) {
    						tpp.proof.add(ps);
    					}
    				}
    			}
    		}
    	} 
    	catch (Exception ex) {
    		System.out.println(ex.getMessage());
    	}
    	tpp.proof = ProofStep.removeDuplicates(tpp.proof);
    	return tpp;
    }
    
     /** ***************************************************************
      */
    public static TPTP3ProofProcessor parseProofOutput (String st) {
    	
    	StringReader sr = new StringReader(st);
    	LineNumberReader lnr = new LineNumberReader(sr);
    	return parseProofOutput(lnr);
 
    }
    
     /** ***************************************************************
      */
    public static void testParseProofFile () {

    	TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
    	try {
    		FileReader r = new FileReader("/home/apease/Programs/E/PROVER/eltb_out.txt");
    		LineNumberReader lnr = new LineNumberReader(r);
    		tpp = parseProofOutput(lnr);
    	} 
    	catch (Exception ex) {
    		System.out.println(ex.getMessage());
    	}
    	System.out.println("-------------------------------------------------");
    	System.out.println(tpp.proof);
    }
      
    /** ***************************************************************
     */
    public static void testE () {

    	try {
    		System.out.println("INFO in EProver.main()");
    		//KBmanager.getMgr().initializeOnce();
    		//KB kb = KBmanager.getMgr().getKB("SUMO");
    		KB kb = null;
    		System.out.println("------------- INFO in EProver.main() completed initialization--------");
    		EProver eprover = new EProver("/home/apease/Programs/E/PROVER/e_ltb_runner",
    				"/home/apease/Sigma/KBs/SUMO.tptp");

    		String result = eprover.submitQuery("(subclass Patio Object)",kb);
    		StringReader sr = new StringReader(result);
    		LineNumberReader lnr = new LineNumberReader(sr);
    		TPTP3ProofProcessor tpp = parseProofOutput(lnr);
    		System.out.println("-------------------------------------------------");
        	System.out.println(tpp.proof);
    		eprover.terminate();
    	} 
    	catch (Exception e) {
    		System.out.println(e.getMessage());
    	}
    }
   
    /** ***************************************************************
     */
    public static void main (String[] args) {

    	//testParseProofStep();
    	//testParseProofFile();
    	testE();
    }
}
