/* This code is copyright Articulate Software (c) 2003.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code are also requested, to credit Articulate Software in any
writings, briefings, publications, presentations, or 
other representations of any software which incorporates,
builds on, or uses this code. Please cite the following
article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/

package com.articulate.sigma.trans;

import TPTPWorld.TPTPFormula;
import TPTPWorld.TPTPParser;
import com.articulate.sigma.*;
import com.articulate.sigma.tp.EProver;
import com.articulate.sigma.tp.Vampire;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TPTP3ProofProcessor {

	public static boolean debug = false;
	public String status;
	public ArrayList<String> bindings = new ArrayList<String>();
	public ArrayList<ProofStep> proof = new ArrayList<ProofStep>();
	public HashMap<String,Integer> idTable = new HashMap<String,Integer>();
	int idCounter = 0;

	/** ***************************************************************
	 * Convert bindings in list to string
	 */
	public String toString () {

		StringBuffer sb = new StringBuffer();
		sb.append("Answers:");
		for (int i = 0; i < bindings.size(); i++)
			sb.append(bindings.get(i) + " ");
		sb.append("\n");
		for (int i = 0; i < proof.size(); i++)
			sb.append(proof.get(i) + "\n");
		sb.append(status + "\n");
		return sb.toString();
	}

	/** ***************************************************************
	 * Join TPTP3 proof statements that are formatted over multiple lines and
	 * reverse them for Vampire, which presents proofs in reverse order. Note
	 * that comment lines are left unchanged.
	 */
	public static ArrayList<String> joinNreverseInputLines (ArrayList<String> inputs) {

		ArrayList<String> outputs = new ArrayList<String>();
		ArrayList<String> commentsBefore = new ArrayList<String>();
		ArrayList<String> commentsAfter = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();
		boolean before = true;
		for (String s : inputs) {
			if (s.trim().startsWith("%")) {
				if (before)
					commentsBefore.add(s);
				else
					commentsAfter.add(s);
				sb = new StringBuffer();
			}
			else if (s.trim().endsWith(").")) {
				before = false;
				sb.append(s);
				outputs.add(sb.toString());
				sb = new StringBuffer();
			}
			else
				sb.append(s.trim());
		}
		Collections.reverse(outputs);
		ArrayList<String> result = new ArrayList<String>();
		for (String s : commentsBefore)
			result.add(s);
		for (String s : outputs)
			result.add(s);
		for (String s : commentsAfter)
			result.add(s);
		return result;
	}

	/** ***************************************************************
	 * return the predicate and arguments to a valid prolog expression p(a1,a2...an)
	 * where a1..an are an atom, a string or a prolog expression
	 */
	public static ArrayList<String> getPrologArgs (String line) {

		ArrayList<String> result = new ArrayList<>();
		boolean inQuote = false;
		int parenLevel = 0;
		StringBuffer sb = new StringBuffer();
		char quoteChar;
		int i = 0;
		while (i < line.length()) {
			switch (line.charAt(i)) {
				case '(':
					if (parenLevel == 0) {
						result.add(sb.toString());
						sb = new StringBuffer();
						parenLevel++;
					}
					else {
						parenLevel++;
						sb.append(line.charAt(i));
					}
					break;
				case ')':
					parenLevel--;
					if (parenLevel == 0) {
						result.add(sb.toString());
						sb = new StringBuffer();
					}
					else
						sb.append(line.charAt(i));
					break;
				case '"':
					sb.append(line.charAt(i));
					do {
						i++;
						sb.append(line.charAt(i));
					} while (line.charAt(i) != '"');
					break;
				case '\'':
					sb.append(line.charAt(i));
					do {
						i++;
						sb.append(line.charAt(i));
					} while (line.charAt(i) != '\'');
					break;
				case ',':
					if (parenLevel == 1) {
						result.add(sb.toString());
						sb = new StringBuffer();
					}
					else
						sb.append(line.charAt(i));
					break;
				default:
					sb.append(line.charAt(i));
					break;
			}
			i++;
		}
		return result;
	}

	/** ***************************************************************
	 * Remove brackets if it contains
	 */
	public static String trimBrackets (String line) {

		if (line.startsWith("[") && line.endsWith("]"))
			return line.substring(1,line.length()-1);
		else {
			System.out.println("Error in TPTP3ProofProcessor.trimBrackets() bad format: " + line);
			return null;
		}
	}

	/** ***************************************************************
	 * Remove parentheses if line contains a matching pair
	 */
	public static String trimParens (String line) {

		if (line.indexOf("(") != -1 && line.indexOf(")") != -1) {
			return line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"));
		}
		else {
			System.out.println("Error in TPTP3ProofProcessor.trimParens() bad format: " + line);
			return null;
		}
	}

	/** ***************************************************************
	 */
	public ArrayList<Integer> parseInferenceObject(String supportId) {

		ArrayList<Integer> prems = new ArrayList<Integer>();
		int firstParen = supportId.indexOf("(");
		int secondParen = StringUtil.findBalancedParen(firstParen, supportId);
		int firstComma = supportId.indexOf(",");
		int secondComma = supportId.indexOf(",",firstComma+1);
		String supports = "";
		if (secondComma+1 >= secondParen)       // supportID = 10997,['proof']
			supports = "[" + supportId + "]";
		supports = supportId.substring(secondComma+1,secondParen);

		if (supports.startsWith("inference"))
			return parseInferenceObject(supports);
		else
			return parseSupports(supports.trim());
	}

	/** ***************************************************************
	 */
	public String getInferenceType(String supportId) {

		String inferenceType = null;
		if (supportId.startsWith("inference(")) {
			int firstParen = supportId.indexOf("(");
			int firstComma = supportId.indexOf(",");
			inferenceType = supportId.substring(firstParen+1, firstComma);
		}
		return inferenceType;
	}

	/** ***************************************************************
	 * Parse support / proof statements in E's response
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
					if (!supportSet[i].trim().startsWith("[symmetry]")) {
						Integer stepnum = idTable.get(supportSet[i].trim());
						if (stepnum == null)
							System.out.println("Error in TPTP3ProofProcessor.parseSupports() no id: " + stepnum +
									" for premises at step " + supportSet[i]);
						else
							prems.add(stepnum);
					}
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
		if (line.contains("\n"))
			System.out.println("Error in TPTP3ProofProcessor.parseProofStep() carriage return in: " + line);
		if (line.startsWith("%")) {
			if (debug) System.out.println("TPTP3ProofProcessor.parseProofStep() skipping comment: " + line);
			return null;
		}
		ProofStep ps = new ProofStep();
		ArrayList<String> args = getPrologArgs(line);
		if (args == null || args.size() < 3) {
			System.out.println("Error in TPTP3ProofProcessor.parseProofStep() format error in line: " + line);
			return ps;
		}
		//System.out.println("Info in TPTP3ProofProcessor.parseProofStep(): " + line);
		//int paren = line.indexOf("("); // the first paren in  "cnf(u402,..." or "fof(myId25,..."
		//if (paren == -1) {
		//	System.out.println("Error in TPTP3ProofProcessor.parseProofStep() bad format: " + line);
		//	return null;
		//}
		//String type = line.substring(0,paren); // fof, cnf, tff
		String type = args.get(0);
		if (!type.equals("cnf") && !type.equals("fof") && !type.equals("tff"))
			System.out.println("Error in TPTP3ProofProcessor.parseProofStep() bad type: " + type);
		if (!line.endsWith(").")) {
			System.out.println("Error in TPTP3ProofProcessor.parseProofStep() bad format: " + line);
			return null;
		}
		//String withoutWrapper = line.substring(paren + 1,line.length() - 2);
		//if (debug) System.out.println("without wrapper       : " + withoutWrapper);
		//int comma1 = withoutWrapper.indexOf(","); // the end of "cnf(u402," or "fof(myId25,"
		//String id = withoutWrapper.substring(0,comma1).trim();
		String id = args.get(1);
		if (debug) System.out.println("ID       : " + id);
		Integer intID = Integer.valueOf(idCounter++);
		idTable.put(id,intID);
		ps.number = intID;

		//int comma2 = withoutWrapper.indexOf(",",comma1 +1 );
		//String formulaType = withoutWrapper.substring(comma1 + 1,comma2).trim();
		String formulaType = args.get(2);
		ps.formulaType = formulaType;
		if (debug) System.out.println("type     : " + formulaType);
		//String rest = withoutWrapper.substring(comma2 + 1).trim();
		//int statementEnd = StringUtil.findBalancedParen(rest.indexOf("("), rest);	// startIndex =  index_of_first_"(", instead of 0;
		// TODO: check if exists "="
		//if (statementEnd == rest.length()-1)    // special case: rest = "s__Class30_1=s__Reptile, file('/var/folders/s4/38700c8541z9h0t0sy_6lmk40000gn/T//epr_diiVH1', i_0_23)"
		//	statementEnd = rest.indexOf(",");   // expected: "foo(s__Class30_1,s__Reptile), file('/var/folders/s4/38700c8541z9h0t0sy_6lmk40000gn/T//epr_diiVH1', i_0_23)"

		//String stmnt = rest;
		String stmnt = args.get(3);
		//if (rest.startsWith("("))
		//	stmnt = trimParens(rest);
		if (stmnt.startsWith("("))
			stmnt = trimParens(stmnt);
		if (debug) System.out.println("stmnt    : " + stmnt);
		line = line.replaceAll("\\$answer\\(","answer(");
		System.out.println("after remove $answer: " + line);
		StringReader reader = new StringReader(line);
		//StringReader reader = new StringReader(stmnt);
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
		if (debug) System.out.println("KIF stmnt : " + stmnt);
		ps.axiom = stmnt;
		//String supportId = rest.substring(statementEnd+2,rest.length()).trim();
		String supportId = args.get(4);;
		if (debug) System.out.println("supportID: " + supportId);
		// add an inference type
		ps.inferenceType = getInferenceType(supportId.trim());
		ps.premises.addAll(parseSupports(supportId.trim()));
		return ps;
	}

	/** ***************************************************************
	 * Return bindings from TPTP3 answer tuples
	 */
	public void processAnswers (String line) {

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
			if (answer != null) {
				String[] esks = answer.split(", ");
				for (String esk : esks) {
					answer = removeEsk(esk);
					bindings.add(answer);
				}
			}
		}
	}

	/** ***************************************************************
	 * Return the most specific type for skolem variable.
	 *
	 * @param tpp The structure learned from E's response
	 * @param kb The knowledge base used to find skolem term's types
	 *
	 * For example,
	 * original binding = esk3_0
	 * set binding = "An instance of Human" (Human is the most specific
	 *              type for esk3_0 in the given proof)
	 *
	 * original binding = esk3_1
	 * set binding = "An instance of Human, Agent" (If multiple types
	 *              are found for esk3_1)
	 */
	public static void findTypesForSkolemTerms(TPTP3ProofProcessor tpp, KB kb) {

		ArrayList<String> bindings = tpp.bindings;
		FormulaPreprocessor fp = new FormulaPreprocessor();
		for (int i = 0; i < bindings.size(); i++) {
			String binding = bindings.get(i);
			if (binding.startsWith("esk")) {
				ArrayList<String> skolemStmts = ProofProcessor.returnSkolemStmt(binding, tpp.proof);
				HashSet<String> types = new HashSet<>();
				for (String skolemStmt : skolemStmts) {

					Pattern p = Pattern.compile("\\(names ([a-zA-Z0-9\\-_]+) ([a-zA-Z0-9\\-]+)");
					Matcher m = p.matcher(skolemStmt);
					while (m.find()) {
						String cl = m.group(2);
						types.add(cl);
					}

					// only find instance/subclass expression if no specific name is found
					if (types.isEmpty()) {
						p = Pattern.compile("\\(instance ([a-zA-Z0-9\\-_]+) ([a-zA-Z0-9\\-_]+)");
						m = p.matcher(skolemStmt);
						while (m.find()) {
							String cl = m.group(2);
							types.add(cl);
						}

						p = Pattern.compile("\\(subclass ([a-zA-Z0-9\\-_]+) ([a-zA-Z0-9\\-]+)");
						m = p.matcher(skolemStmt);
						while (m.find()) {
							String cl = m.group(2);
							types.add(cl);
						}
					}
				}
				if (kb.kbCache.checkDisjoint(kb, types) == true) {
					// check if there are contradiction among the types returned from E
					bindings.remove(binding);
					binding = "Get type contradiction for " + binding + " in " + types;
					bindings.add(binding);
				}
				else {
					fp.winnowTypeList(types, kb);
					if (types!=null && types.size()>0) {
						if (types.size() == 1) {
							binding = "an instance of " + types.toArray()[0];
						}
						else {
							binding = "an instance of ";
							boolean start = true;
							for (String t : types) {
								if (start) { binding += t; start = false; }
								else { binding += ", " + t; }
							}
						}
						bindings.set(i, binding);
					}
				}
			}
			else {
				binding = TPTP2SUMO.transformTerm(binding);
				bindings.set(i, binding);
			}
		}
	}

	/** ***************************************************************
	 * remove skolem symbol with arity n
	 *
	 * For example,
	 * Input: esk2_1(s__Arc13_1)
	 * Output: s__Arc13_1
	 */
	private String removeEsk(String line) {

		if (line.startsWith("esk")) {
			int leftParen = line.indexOf("(");
			int rightParen = line.indexOf(")");
			if (leftParen != -1 && rightParen != -1)
				return line.substring(leftParen+1, rightParen);
		}
		return line;
	}

	/** ***************************************************************
	 * Print out prover's bindings
	 */
	public void printAnswers () {

		System.out.println("Answers:");
		for (int i = 0; i < bindings.size(); i++)
			System.out.println(bindings.get(i));
	}

	/** ***************************************************************
	 * Compute bindings and proof from theorem prover's response
	 */
	public static TPTP3ProofProcessor parseProofOutput (LineNumberReader lnr, KB kb) {

		TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
		try {
			boolean inProof = false;
			boolean finishAnswersTuple = false;
			String line;
			while ((line = lnr.readLine()) != null) {
				if (line.indexOf("SZS output start") != -1) {
					inProof = true;
					line = lnr.readLine();
				}
				if (line.indexOf("SZS status") != -1) {
					tpp.status = line.substring(13);
				}
				if (line.indexOf("SZS answers") != -1) {
					if (!finishAnswersTuple) {
						tpp.processAnswers(line.substring(20).trim());
						finishAnswersTuple = true;
					}
				}
				if (inProof) {
					if (line.indexOf("SZS output end") != -1) {
						inProof = false;
					}
					else {
						ProofStep ps = tpp.parseProofStep(line);
						if (ps != null) {
							tpp.proof.add(ps);
							if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(): adding line: " +
									line + "\nas " + ps);
						}
					}
				}
			}
		}
		catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		// remove unnecessary steps, eg: conjectures, duplicate trues
		tpp.proof = ProofStep.removeUnnecessary(tpp.proof);
		tpp.proof = ProofStep.removeDuplicates(tpp.proof);

		// find types for skolem terms
		findTypesForSkolemTerms(tpp, kb);
		return tpp;
	}

    /** ***************************************************************
	 * Compute binding and proof from E's response
     */
    public static TPTP3ProofProcessor parseProofOutput (ArrayList<String> lines, KB kb) {

		if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(): before reverse: " +
				lines);
    	lines = joinNreverseInputLines(lines);
		if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(): after reverse: " +
				lines);
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        try {
            boolean inProof = false;
            boolean finishAnswersTuple = false;
            String line;
            Iterator<String> it = lines.iterator();
            while (it.hasNext()) {
                line = it.next();
				if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(): looking at line: " +
						line);
				if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(): in proof: " +
						inProof);
				if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(): finishAnswersTuple: " +
						finishAnswersTuple);
                if (line.indexOf("SZS output start") != -1) {
                    inProof = true;
                    line = it.next();
                }
                if (line.indexOf("SZS status") != -1) {
                    tpp.status = line.substring(13);
					if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(): tpp.status: " +
							tpp.status);
                }
                if (line.indexOf("SZS answers") != -1) {
                    if (!finishAnswersTuple) {
                    	int end = line.lastIndexOf("]");
                    	if (end == -1 || (line.length() < end + 1))
							end = line.length();
						String bracketedAnswers = line.substring(20,end + 1);
                        tpp.processAnswers(bracketedAnswers);
                        finishAnswersTuple = true;
                    }
                }
                if (inProof) {
                    if (line.indexOf("SZS output end") != -1) {
                        inProof = false;
                    }
                    else {
                        ProofStep ps = tpp.parseProofStep(line);
                        if (ps != null) {
                            tpp.proof.add(ps);
                            if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(2): adding line: " +
									line + "\nas " + ps);
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
		if (debug) System.out.println("TPTP3ProofProcess.parseProofOutput(2): before pruning: " + tpp);
        // remove unnecessary steps, eg: conjectures, duplicate trues
        tpp.proof = ProofStep.removeUnnecessary(tpp.proof);
        tpp.proof = ProofStep.removeDuplicates(tpp.proof);

        // find types for skolem terms
        findTypesForSkolemTerms(tpp, kb);
		if (debug) System.out.println("TPTP3ProofProcess.parseProofOutput(2): result: " + tpp);
        return tpp;
    }
    
	/** ***************************************************************
	 * Return a list of answers if prover finds bindings for wh- queries.
	 * Return "Proof Found" if prover finds contradiction for boolean queries.
	 *
	 * For example,
	 * tuple_list = [[esk3_1(s__Org1_1)]|_]
	 * Output = [Org1_1]
	 *
	 * tuple_list = [[esk3_0]|_]
	 * Output = [An instance of Human] (Human is the most specific type
	 * for esk3_0 in the given proof)
	 */
	public static ArrayList<String> parseAnswerTuples(String st, KB kb, FormulaPreprocessor fp) {

		ArrayList<String> answers = new ArrayList<>();
		TPTP3ProofProcessor tpp = TPTP3ProofProcessor.parseProofOutput(st, kb);
		if (tpp.bindings == null || tpp.bindings.isEmpty()) {
			if (tpp.proof != null && !tpp.proof.isEmpty()) {
				answers.add("Proof Found");		// for boolean queries
			}
			return answers;
		}
		return tpp.bindings;
	}

	/** ***************************************************************
	 */
	public static TPTP3ProofProcessor parseProofOutput (String st, KB kb) {

		StringReader sr = new StringReader(st);
		LineNumberReader lnr = new LineNumberReader(sr);
		return parseProofOutput(lnr, kb);
	}

	/** ***************************************************************
	 */
	public static void testParseProofFile () {

		KB kb = null;
		TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
		try {
			FileReader r = new FileReader(System.getProperty("user.home") + "/Programs/E/PROVER/eltb_out.txt");
			LineNumberReader lnr = new LineNumberReader(r);
			tpp = parseProofOutput(lnr, kb);
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
			//KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
			KB kb = null;
			System.out.println("------------- INFO in EProver.main() completed initialization--------");
			EProver eprover = new EProver(System.getProperty("user.home") + "/Programs/E/PROVER/e_ltb_runner",
					System.getenv("SIGMA_HOME") + "/KBs/SUMO.tptp");
			String result = eprover.submitQuery("(subclass Patio Object)", kb);
			StringReader sr = new StringReader(result);
			LineNumberReader lnr = new LineNumberReader(sr);
			TPTP3ProofProcessor tpp = parseProofOutput(lnr, kb);
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
	public static void testVampire () {

		try {
			System.out.println("INFO in EProver.main()");
			//KBmanager.getMgr().initializeOnce();
			//KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
			KB kb = null;
			System.out.println("------------- INFO in EProver.main() completed initialization--------");
			EProver eprover = new EProver(System.getProperty("user.home") + "/Programs/E/PROVER/e_ltb_runner",
					System.getenv("SIGMA_HOME") + "/KBs/SUMO.tptp");

			String result = eprover.submitQuery("(subclass Patio Object)",kb);
			//Vampire vampire = new Vampire(s, timeout, tptpquery);
			StringReader sr = new StringReader(result);
			LineNumberReader lnr = new LineNumberReader(sr);
			TPTP3ProofProcessor tpp = parseProofOutput(lnr, kb);
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
	public static void testParseProofStep () {

		String ps1 = "fof(c_0_5, axiom, (s__subclass(s__Artifact,s__Object)), c_0_3).";
		String ps2 = "fof(c_0_2, negated_conjecture,(~(?[X1]:(s__subclass(X1,s__Object)&~$answer(esk1_1(X1)))))," +
				"inference(assume_negation,[status(cth)],[inference(add_answer_literal,[status(thm)],[c_0_0, theory(answers)])])).";
		String ps3 = "cnf(c_0_14,negated_conjecture,($false), " +
				"inference(eval_answer_literal,[status(thm)], [inference(spm,[status(thm)],[c_0_12, c_0_13, theory(equality)]), theory(answers)]), ['proof']).";
		TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
		tpp.idTable.put("c_0_0", Integer.valueOf(0));
		tpp.idTable.put("c_0_3", Integer.valueOf(1));
		tpp.idTable.put("c_0_12", Integer.valueOf(2));
		tpp.idTable.put("c_0_13", Integer.valueOf(3));
		System.out.println(tpp.parseProofStep(ps1));
		System.out.println();
		System.out.println(tpp.parseProofStep(ps1));
		System.out.println();
		System.out.println(tpp.parseProofStep(ps3));
	}

	/** ***************************************************************
	 */
	public static void main (String[] args) {

		//testParseProofStep();
		//testParseProofFile();
		testE();
	}
}
