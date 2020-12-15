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
import com.articulate.sigma.utils.StringUtil;
import com.igormaznitsa.prologparser.DefaultParserContext;
import com.igormaznitsa.prologparser.GenericPrologParser;
import com.igormaznitsa.prologparser.ParserContext;
import com.igormaznitsa.prologparser.PrologParser;
import com.igormaznitsa.prologparser.terms.*;
import com.igormaznitsa.prologparser.tokenizer.Op;

import java.io.*;
import java.util.*;

import static com.igormaznitsa.prologparser.terms.TermType.*;

public class TPTP3ProofProcessor {

	public static boolean debug = false;
	public String status;
	public boolean inconsistency = false;
	public ArrayList<String> bindings = new ArrayList<>();
	public HashMap<String,String> bindingMap = new HashMap<>();
	public HashMap<String,String> skolemTypes = new HashMap<>();
	public ArrayList<ProofStep> proof = new ArrayList<>();

	// a map of original ID keys and renumbered key values
	public HashMap<String,Integer> idTable = new HashMap<>();
	private int idCounter = 0;

	/** ***************************************************************
	 * Convert bindings in list to string
	 */
	public String toString () {

		StringBuilder sb = new StringBuilder();
		sb.append("Answers:");
		for (String s : bindings)
			sb.append(s + " ");
		sb.append("\n");
		for (ProofStep ps : proof)
			sb.append(ps + "\n");
		sb.append(status + "\n");
		return sb.toString();
	}

	/** ***************************************************************
	 * Join TPTP3 proof statements that are formatted over multiple lines. Note
	 * that comment lines are left unchanged.
	 */
	private static ArrayList<String> joinLines(ArrayList<String> inputs) {

		ArrayList<String> outputs = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();
		boolean before = true;
		for (String s : inputs) {
			if (s.trim().startsWith("%")) {
				sb = new StringBuffer();
				outputs.add(s.trim());
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
		return outputs;
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
	 */
	public String getInferenceType(String supportId) {

		String inferenceType = null;
		if (supportId.startsWith("inference(")) {
			int firstParen = supportId.indexOf("(");
			int firstComma = supportId.indexOf(",");
			inferenceType = supportId.substring(firstParen+1, firstComma);
		}
		else if (supportId.startsWith("file(")) {
			int firstParen = supportId.indexOf("(");
			int firstComma = supportId.indexOf(",");
			int secondParen = supportId.indexOf(")",firstComma+1);
			inferenceType = supportId.substring(firstComma+1, secondParen);
		}
		else if (supportId.startsWith("introduced(")) {
			int firstParen = supportId.indexOf("(");
			int firstComma = supportId.indexOf(",");
			inferenceType = "introduced:" + supportId.substring(firstParen+1, firstComma);
		}
		return inferenceType;
	}

	/** ***************************************************************
	 * Parse support / proof statements in the response
     */
	public ArrayList<Integer> parseSupports(String supportId) {

		if (debug) System.out.println("Info in TPTP3ProofProcessor.parseSupports(): " + supportId);

		ArrayList<Integer> prems = new ArrayList<Integer>();
		if (supportId.startsWith("[") && !supportId.equals("[]")) {
			//supportId = trimBrackets(supportId).trim();
            supportId = StringUtil.removeEnclosingCharPair(supportId,1,'[',']').trim();
			//System.out.println("Info in TPTP3ProofProcessor.parseSupports()2: " + supportId);
			if (supportId.startsWith("inference("))
				return getSupports(supportId + ".");
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
			return getSupports(supportId + ".");
		}
		else if (supportId.startsWith("introduced(")) { // there should never be premises for this
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

		//System.out.println("parseProofStep() last char: " + line.charAt(line.length()-1));
		//System.out.println("parseProofStep() second to last char: " + line.charAt(line.length()-2));
		if (StringUtil.emptyString(line))
			return null;
		if (line.contains(System.lineSeparator()))
			System.out.println("warning in TPTP3ProofProcessor.parseProofStep() carriage return in: " + line);
		line = line.replaceAll(System.lineSeparator(),"");
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
		if (debug) System.out.println("TPTP3ProofProcessor.parseProofStep(): ID       : " + id);
		Integer intID = Integer.valueOf(idCounter++);
		idTable.put(id,intID);
		ps.number = intID;

		//int comma2 = withoutWrapper.indexOf(",",comma1 +1 );
		//String formulaType = withoutWrapper.substring(comma1 + 1,comma2).trim();
		String formulaType = args.get(2);
		ps.formulaType = formulaType;
		if (formulaType.equals("negated_conjecture")) {
			if (debug) System.out.println("TPTP3ProofProcessor.parseProofStep(): found negated_conjecture, setting inconsistency to false");
			inconsistency = false;
		}
		if (debug) System.out.println("TPTP3ProofProcessor.parseProofStep(): type     : " + formulaType);
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
            stmnt = StringUtil.removeEnclosingCharPair(stmnt,1,'(',')');
			//stmnt = trimParens(stmnt);
		if (debug) System.out.println("TPTP3ProofProcessor.parseProofStep(): stmnt    : " + stmnt);
		line = line.replaceAll("\\$answer\\(","answer(");
		if (debug) System.out.println("TPTP3ProofProcessor.parseProofStep(): after remove $answer: " + line);
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
			System.out.println("TPTP3ProofProcessor.parseProofStep(): with input: " + line);
		}
		if (debug) System.out.println("TPTP3ProofProcessor.parseProofStep(): KIF stmnt : " + stmnt);
		ps.axiom = stmnt;
		//String supportId = rest.substring(statementEnd+2,rest.length()).trim();
		String supportId = args.get(4);
		if (debug) System.out.println("TPTP3ProofProcessor.parseProofStep(): supportID: " + supportId);
		// add an inference type
		ps.inferenceType = getInferenceType(supportId.trim());
		ps.premises.addAll(parseSupports(supportId.trim()));
		return ps;
	}

	/** ***************************************************************
	 * Return bindings from TPTP3 answer tuples
	 */
	public void processAnswers (String line) {

		if (debug) System.out.println("INFO in processAnswers(): line: " + line);
		//String trimmed = trimBrackets(line);
        String trimmed = StringUtil.removeEnclosingCharPair(line,1,'[',']');
		if (debug) System.out.println("INFO in processAnswers(): trimmed: " + trimmed);
		if (trimmed == null) {
			System.out.println("Error in TPTP3ProofProcessor.processAnswers() bad format: " + line);
			return;
		}
		String[] answers = trimmed.split("\\|");
		if (debug) System.out.println("INFO in processAnswers(): answers: " + Arrays.toString(answers));
		for (int i = 0; i < answers.length; i++) {
			if (answers[i].equals("_"))
				break;
			//String answer = trimBrackets(answers[i]);
            String answer = answers[i];
			if (debug) System.out.println("INFO in processAnswers(): answer: " + answer);
			if (answer != null) {
				if (answer.startsWith("["))
					answer = StringUtil.removeEnclosingCharPair(answer,1,'[',']');
				String[] esks = answer.split(",");
				for (String esk : esks) {
					if (debug) System.out.println("INFO in processAnswers(): esk: " + esk);
					answer = removeEsk(esk);
					answer = removePrefix(answer);
					if (debug) System.out.println("INFO in processAnswers(): binding: " + answer);
					bindings.add(answer);
				}
			}
		}
	}

	/** ***************************************************************
	 * Return the answer clause, or null if not present
	 */
	public Formula extractAnswerClause(Formula ax) {

		if (debug) System.out.println("extractAnswerClause(): " + ax.getFormula());
		if (!ax.listP())
			return null;
		String pred = ax.car();
		if (debug) System.out.println("extractAnswerClause(): pred: " + pred);
		if (Formula.atom(pred)) {
			if (KBmanager.getMgr().prover == KBmanager.Prover.VAMPIRE && pred.equals("ans0"))
				return ax;
			if (KBmanager.getMgr().prover == KBmanager.Prover.EPROVER && pred.equals("answer"))
				return ax;
		}
		else {
			Formula predF = extractAnswerClause(ax.carAsFormula());
			if (debug) System.out.println("extractAnswerClause(): predF: " + predF);
			if (predF != null)
				return predF;
		}
		ArrayList<Formula> args = ax.complexArgumentsToArrayList(1);
		if (debug) System.out.println("extractAnswerClause(): args: " + args);

		if (args == null)
			return null;
		if (debug) System.out.println("extractAnswerClause(): args size: " + args.size());
		for (Formula f : args) {
			if (debug) System.out.println("extractAnswerClause(): check arg: " + f);
			Formula argF = extractAnswerClause(f);
			if (debug) System.out.println("extractAnswerClause(): argF: " + argF);
			if (argF != null)
				return argF;
			else
				if (debug) System.out.println("extractAnswerClause(): returns null for check arg: " + argF);
		}
		return null;
	}

	/** ***************************************************************
	 * Remove duplicates in an Array without changing the order
	 */
	public ArrayList<String> removeDupInArray(ArrayList<String> input) {

		ArrayList<String> result = new ArrayList<>();
		for (String s : input)
			if (!result.contains(s))
				result.add(s);
		return result;
	}

	/** ***************************************************************
	 * Put bindings from TPTP3 proof answer variables into bindingMap
	 */
	public void processAnswersFromProof(String query) {

		Formula qform = new Formula(query);
		ArrayList<String> answers = null;
		ArrayList<String> vars = qform.collectAllVariablesOrdered();
		vars = removeDupInArray(vars);
		if (debug) System.out.println("processAnswersFromProof(): vars: " + vars);
		if (debug) System.out.println("processAnswersFromProof(): proof: " + proof);
		for (ProofStep ps : proof) {
			if (debug) System.out.println("processAnswersFromProof(): ps: " + ps);
			if (ps != null && !StringUtil.emptyString(ps.axiom) &&
					ps.axiom.contains("ans0") && !ps.axiom.contains("?")) {
				if (debug) System.out.println("processAnswersFromProof(): has ans clause: " + ps);
				Formula answerClause = extractAnswerClause(new Formula(ps.axiom));
				if (debug) System.out.println("processAnswersFromProof(): answerClause: " + answerClause);
				if (answerClause != null)
					answers = answerClause.complexArgumentsToArrayListString(1);
				if (debug) System.out.println("processAnswersFromProof(): answers: " + answers);
				break;
			}
		}
		if (answers == null || vars.size() != answers.size()) {
			if (debug) System.out.println("Error in processAnswersFromProof(): null result");
			return;
		}
		for (int i = 0; i < vars.size(); i++) {
			bindingMap.put(vars.get(i),answers.get(i));
		}
	}

	/** ***************************************************************
	 * Return the most specific type for skolem variable.
	 *
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
	public String findTypesForSkolemTerms(KB kb) {

		String result = "";
		if (debug) System.out.println("findTypesForSkolemTerms(): bindings: " + bindings);
		if (debug) System.out.println("findTypesForSkolemTerms(): bindings map: " + bindingMap);
		FormulaPreprocessor fp = new FormulaPreprocessor();
		for (String binding : bindingMap.values()) {
			if (binding.startsWith("esk") || binding.startsWith("(sK")) {
				ArrayList<String> skolemStmts = ProofProcessor.returnSkolemStmt(binding, proof);
				if (debug) System.out.println("findTypesForSkolemTerms(): skolem stmts: " + skolemStmts);
				HashSet<String> types = new HashSet<>();
				for (String skolemStmt : skolemStmts) {
					Formula f = new Formula(skolemStmt);
					ArrayList<String> l = f.complexArgumentsToArrayListString(0);
					if (l.size() != 3)
						continue;
					if (l.get(0).equals("names") || l.get(0).equals("instance") || l.get(0).equals("subclass"))
						types.add(l.get(2));
				}
				if (kb.kbCache.checkDisjoint(kb, types) == true) {
					// check if there are contradiction among the types returned
					//bindings.remove(binding);
					result = "Get type contradiction for " + binding + " in " + types;
					//bindings.add(binding);
				}
				else {
					fp.winnowTypeList(types, kb);
					if (types != null && types.size() > 0) {
						if (types.size() == 1) {
							result = "an instance of " + types.toArray()[0];
						}
						else {
							result = "an instance of ";
							boolean start = true;
							for (String t : types) {
								if (start) {
									result += t;
									start = false;
								}
								else {
									result += ", " + t;
								}
							}
						}
					}
				}
			}
			else {
				result = TPTP2SUMO.transformTerm(binding);
			}
		}
		if (debug) System.out.println("findTypesForSkolemTerms(): result: " + result);
		return result;
	}

	/** ***************************************************************
	 * remove skolem symbol with arity n
	 *
	 * For example,
	 * Input: esk2_1(s__Arc13_1)
	 * Output: s__Arc13_1
	 */
	private String removeEsk(String line) {

		if (line.startsWith("esk") || line.startsWith("sK")) {
			int leftParen = line.indexOf("(");
			int rightParen = line.indexOf(")");
			if (leftParen != -1 && rightParen != -1)
				return line.substring(leftParen+1, rightParen);
		}
		return line;
	}

	/** ***************************************************************
	 * Input: s__Arc13_1
	 * Output: Arc13_1
	 */
	private String removePrefix(String st) {

		System.out.println("removePrefix(): " + st);
		String tsp = Formula.termSymbolPrefix;
		if (st.startsWith(tsp))
			return st.substring(tsp.length(),st.length());
		else
			return st;
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
	public void parseProofOutput (LineNumberReader lnr, KB kb) {

		try {
			boolean inProof = false;
			boolean finishAnswersTuple = false;
			String line;
			while ((line = lnr.readLine()) != null) {
				if (line.indexOf("SZS output start") != -1) {
					inconsistency = true; // if negated_conjecture found in the proof then it's not inconsistent
					if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(lnr): found proof, seeing inconsistency to true");
					inProof = true;
					line = lnr.readLine();
				}
				if (line.indexOf("SZS status") != -1) {
					status = line.substring(13);
				}
				if (line.indexOf("SZS answers") != -1) {
					if (!finishAnswersTuple) {
						processAnswers(line.substring(20,line.lastIndexOf(']')+1).trim());
						finishAnswersTuple = true;
					}
				}
				if (inProof) {
					if (line.indexOf("SZS output end") != -1) {
						inProof = false;
					}
					else {
						ProofStep ps = parseProofStep(line);
						if (ps != null) {
							proof.add(ps);
							if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(lnr): adding line: " +
									line + "\nas " + ps);
						}
					}
				}
			}
			if (inconsistency) {
				System.out.println("*****************************************");
				System.out.println("TPTP3ProofProcessor.parseProofOutput(lnr): Danger! possible inconsistency!");
				System.out.println("*****************************************");
			}
		}
		catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		// remove unnecessary steps, eg: conjectures, duplicate trues
		proof = ProofStep.removeUnnecessary(proof);
		proof = ProofStep.removeDuplicates(proof);
		// find types for skolem terms
		findTypesForSkolemTerms(kb);
	}

    /** ***************************************************************
	 * Compute binding and proof from the theorem prover's response
     */
    public void parseProofOutput (ArrayList<String> lines, String kifQuery, KB kb) {

		//if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(): before reverse: " +
		//		lines);
    	//lines = joinNreverseInputLines(lines);
		//if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(): after reverse: " +
		//		lines);
		if (KBmanager.getMgr().prover == KBmanager.Prover.VAMPIRE)
			lines = joinNreverseInputLines(lines);
		if (KBmanager.getMgr().prover == KBmanager.Prover.EPROVER)
			lines = joinLines(lines);
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
					if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(): found proof, seeing inconsistency to true");
					inconsistency = true; // if negated_conjecture found in the proof then it's not inconsistent
                    inProof = true;
                    line = it.next();
                }
                if (line.indexOf("SZS status") != -1) {
                    status = line.substring(13);
					if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(): tpp.status: " +
							status);
                }
                if (line.indexOf("SZS answers") != -1) {
                    if (!finishAnswersTuple) {
                    	int end = line.lastIndexOf("]");
                    	if (end == -1 || (line.length() < end + 1))
							end = line.length();
						String bracketedAnswers = line.substring(20,end + 1);
                        processAnswers(bracketedAnswers);
                        finishAnswersTuple = true;
                    }
                }
                if (inProof) {
                    if (line.indexOf("SZS output end") != -1) {
                        inProof = false;
                    }
                    else {
                        ProofStep ps = parseProofStep(line);
                        if (ps != null) {
                            proof.add(ps);
							if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(2): adding line: " +
									line + "\nas " + ps);
                        }
                    }
                }
            }
			if (inconsistency) {
				System.out.println("*****************************************");
				System.out.println("TPTP3ProofProcessor.parseProofOutput(): Danger! possible inconsistency!");
				System.out.println("*****************************************");
			}
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
		if (debug) System.out.println("TPTP3ProofProcess.parseProofOutput(2): before pruning: " + this);
        // remove unnecessary steps, eg: conjectures, duplicate trues
        proof = ProofStep.removeUnnecessary(proof);
        proof = ProofStep.removeDuplicates(proof);
        processAnswersFromProof(kifQuery);
        // find types for skolem terms
        findTypesForSkolemTerms(kb);
		if (debug) System.out.println("TPTP3ProofProcess.parseProofOutput(2): result: " + this);
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
	public ArrayList<String> parseAnswerTuples(String st, KB kb, FormulaPreprocessor fp) {

		ArrayList<String> answers = new ArrayList<>();
		parseProofOutput(st, kb);
		if (bindings == null || bindings.isEmpty()) {
			if (proof != null && !proof.isEmpty()) {
				answers.add("Proof Found");		// for boolean queries
			}
			return answers;
		}
		return bindings;
	}

	/** ***************************************************************
	 */
	public void parseProofOutput (String st, KB kb) {

		StringReader sr = new StringReader(st);
		LineNumberReader lnr = new LineNumberReader(sr);
		parseProofOutput(lnr, kb);
	}

	/** ***************************************************************
	 */
	private static void printPrologTerm (PrologTerm pt, String indent) {

		System.out.println(indent + pt.toString() + "\t" + pt.getType());
		if (pt.getType() == STRUCT) {
			System.out.println("arity: " + pt.getArity());
			for (PrologTerm pit : (PrologStruct) pt)
				printPrologTerm(pit, indent + "\t");
		}
		else if (pt.getType() == LIST) {
			System.out.println("arity: " + pt.getArity());
			for (PrologTerm pit : (PrologList) pt)
				printPrologTerm(pit,indent+"\t");
		}
	}

	/** ***************************************************************
	 */
	public static void printPrologTerm (PrologTerm pt) {

		printPrologTerm(pt,"");
	}

	/** ***************************************************************
	 */
	private ArrayList<Integer> getSupports (PrologTerm pt) {

		if (debug) System.out.println("TPTP3ProofProcess.getSupports(PrologTerm): " + pt);
		if (debug) System.out.println("getSupports(): " + pt.toString() + "\t" + pt.getType());
		ArrayList<Integer> supports = new ArrayList<>();
		if (pt.getType() == ATOM)
			supports.add(idTable.get(pt.toString()));
		else if (pt.getType() == LIST) {
			for (PrologTerm pit : (PrologList) pt)
				supports.addAll(getSupports(pit));
		}
		else if (pt.getType() == STRUCT) {
			String predString = pt.getFunctor().toString();
			if (predString.equals("inference"))
				supports.addAll(getSupports(((PrologStruct) pt).getTermAt(2)));
			else if (predString.equals("cnf") || predString.equals("fof") ||
					predString.equals("tff"))
				supports.addAll(getSupports(((PrologStruct) pt).getTermAt(3)));
		}
		return supports;
	}

	/** ***************************************************************
	 */
	private ArrayList<Integer> getSupports (String input) {

		if (debug) System.out.println("TPTP3ProofProcess.getSupports(String): " + input);
		ArrayList<Integer> supports = new ArrayList<>();
		Reader reader = new StringReader(input);
		if (debug) System.out.println(input);
		DefaultParserContext dpc = new DefaultParserContext(ParserContext.FLAG_CURLY_BRACKETS).addOps(Op.SWI);
		PrologParser parser = new GenericPrologParser(reader,dpc);
		for (PrologTerm pt : parser)
			supports.addAll(getSupports(pt));
		return supports;
	}

	/** ***************************************************************
	 */
	private void testPrologParser () {

		//Reader reader = new StringReader("hello(world). some({1,2,3})."); // power(X,Y,Z) :- Z is X ** Y.");
		String input = "cnf(c_0_8, negated_conjecture, ($false), " +
				"inference(cn,[status(thm)]," +
				"[inference(rw,[status(thm)]," +
				"[inference(rw,[status(thm)],[c_0_5, c_0_6]), c_0_7])])," +
				" ['proof']).";
		idTable.put("c_0_5", Integer.valueOf(0));
		idTable.put("c_0_6", Integer.valueOf(1));
		idTable.put("c_0_7", Integer.valueOf(2));
		Reader reader = new StringReader(input);
		System.out.println(input);
//		PrologParser parser = new GenericPrologParser(reader, new DefaultParserContext(ParserContext.FLAG_CURLY_BRACKETS, Op.SWI));
		DefaultParserContext dpc = new DefaultParserContext(ParserContext.FLAG_CURLY_BRACKETS).addOps(Op.SWI);
		PrologParser parser = new GenericPrologParser(reader,dpc);
		//for (PrologTerm pt : parser) {
		//	printPrologTerm(pt);
		//}
		System.out.println("--------------------------");
		for (PrologTerm pt : parser)
			System.out.println(getSupports(pt));
	}

	/** ***************************************************************
	 */
	public static void main (String[] args) {

		TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
		tpp.testPrologParser();
	}
}
