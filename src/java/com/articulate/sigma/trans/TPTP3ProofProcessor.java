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

import com.articulate.sigma.*;
import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.StringUtil;
import com.igormaznitsa.prologparser.DefaultParserContext;
import com.igormaznitsa.prologparser.GenericPrologParser;
import com.igormaznitsa.prologparser.ParserContext;
import com.igormaznitsa.prologparser.PrologParser;
import com.igormaznitsa.prologparser.terms.*;
import com.igormaznitsa.prologparser.tokenizer.Op;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.*;

import tptp_parser.*;

import static com.igormaznitsa.prologparser.terms.TermType.*;

public class TPTP3ProofProcessor {

	public static boolean debug = false;
	public String status;
	public boolean noConjecture = false;
	public boolean inconsistency = false;
	public boolean containsFalse = false;
	public ArrayList<String> bindings = new ArrayList<>();
	public HashMap<String,String> bindingMap = new HashMap<>();
	public HashMap<String,String> skolemTypes = new HashMap<>();
	public ArrayList<TPTPFormula> proof = new ArrayList<>();
	public static boolean tptpProof = false;

	// a map of original ID keys and renumbered key values
	public HashMap<String,Integer> idTable = new HashMap<>();
	private int idCounter = 0;

	/** ***************************************************************
	 */
	public TPTP3ProofProcessor() {
	}

	/** ***************************************************************
	 * Convert bindings in list to string
	 */
	public String toString () {

		StringBuilder sb = new StringBuilder();
		sb.append("Answers:");
		if (bindingMap != null && bindingMap.keySet().size() > 0) {
			for (String s : bindingMap.keySet()) {
				sb.append(s + " = " + bindingMap.get(s) + " ");
			}
		}
		else {
			for (String s : bindings)
				sb.append(s + " ");
		}
		sb.append("\n");
		for (TPTPFormula ps : proof)
			sb.append(ps + "\n");
		sb.append(status + "\n");
		return sb.toString();
	}

	/** ***************************************************************
	 * Join TPTP3 proof statements that are formatted over multiple lines. Note
	 * that comment lines are left unchanged.
	 */
	public static ArrayList<String> joinLines(ArrayList<String> inputs) {

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
	public ArrayList<Integer> supportIdToInts(ArrayList<String> supportIds) {

		ArrayList<Integer> prems = new ArrayList<Integer>();
		for (String s : supportIds)
			prems.add(getNumFromIDtable(s));
		return prems;
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
				if (debug) System.out.println("Info in TPTP3ProofProcessor.parseSupports(): support element: " + supportSet[i]);
				if (supportSet[i].indexOf("(") == -1) {
					if (!supportSet[i].trim().startsWith("[symmetry]")) {
						Integer stepnum = getNumFromIDtable(supportSet[i].trim());
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
			Integer stepnum = getNumFromIDtable(supportId);
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

		if (debug) System.out.println("-----------------------");
		if (debug) System.out.println("parseProofStep() line: " + line);
		if (StringUtil.emptyString(line))
			return null;
		line = line.replaceAll(System.lineSeparator(),"");
		if (line.startsWith("%")) {
			if (debug) System.out.println("TPTP3ProofProcessor.parseProofStep() skipping comment: " + line);
			return null;
		}
		ProofStep ps = new ProofStep();
		line = line.replaceAll("\\$answer\\(","answer(");
		if (debug) System.out.println("TPTP3ProofProcessor.parseProofStep(): after remove $answer: " + line);
		try {
			TPTPVisitor sv = new TPTPVisitor();
			sv.parseString(line);
			HashMap<String, TPTPFormula> hm = sv.result;
			if (hm != null) {
				for (String tptpid : hm.keySet()) {
					TPTPFormula tptpF = hm.get(tptpid);
					ps.axiom = TPTP2SUMO.collapseConnectives(new Formula(tptpF.sumo)).toString();
					ps.formulaType = tptpF.type;
					System.out.println("TPTP3ProofProcessor.parseProofStep(): type: " + tptpF.type);
					idTable.put(tptpF.name,getNumFromIDtable(tptpF.name));
					ps.number = getNumFromIDtable(tptpF.name);
					ps.formulaRole = tptpF.role;
					ps.inferenceType = tptpF.infRule;
					ps.input = line;
					ps.premises.addAll(supportIdToInts(tptpF.supports));
					if (tptpF.supports != null && tptpF.supports.size() == 1 && tptpF.supports.get(0).startsWith("file(")) {
						int firstParen = tptpF.supports.get(0).indexOf("(");
						int firstComma = tptpF.supports.get(0).indexOf(",");
						int secondParen = tptpF.supports.get(0).indexOf(")", firstComma + 1);
						ps.sourceID = tptpF.supports.get(0).substring(firstComma + 1, secondParen);
					}
				}
			}
			else
				ps.axiom = line;
		}
		catch (Exception e) {
			System.out.println("Error in TPTP3ProofProcessor.parseProofStep(): " + e.getMessage());
			e.printStackTrace();
			System.out.println("TPTP3ProofProcessor.parseProofStep(): with input: " + line);
			ps.axiom = line;
		}
		if (ps.formulaRole.trim().equals("negated_conjecture") || ps.formulaRole.trim().equals("conjecture")) {
			if (debug) System.out.println("TPTP3ProofProcessor.parseProofStep(): found conjecture or negated_conjecture, setting inconsistency to false");
			noConjecture = false;
		}
		return ps;
	}

	/** ***************************************************************
	 * Return bindings from TPTP3 answer tuples
	 */
	public void processAnswers(String line) {

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
					//answer = removeEsk(esk);
					answer = removePrefix(esk);
					if (debug) System.out.println("INFO in processAnswers(): binding: " + answer);
					bindings.add(answer);
				}
			}
		}
		if (debug) System.out.println("INFO in processAnswers(): returning bindings: " + bindings);
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
	public void processAnswersFromProof(StringBuffer qlist, String query) {

		Formula qform = new Formula(query);
		ArrayList<String> answers = null;
		ArrayList<String> vars = qform.collectAllVariablesOrdered();
		vars = removeDupInArray(vars);
		ArrayList<String> qvars = new ArrayList<>();
		if (debug) System.out.println("processAnswersFromProof(): vars: " + vars);
		if (debug) System.out.println("processAnswersFromProof(): qlist: " + qlist);
		if (debug) System.out.println("processAnswersFromProof(): query: " + query);
		if (debug) System.out.println("processAnswersFromProof(): bindingMap: " + bindingMap);
		bindingMap = new HashMap<>();
		if (qlist != null && qlist.length() > 0) {
			List<String> qvarslist = Arrays.asList(qlist.toString().split(","));
			for (String s : qvarslist) {
				String news = s.replace("V__","?");
				qvars.add(news);
			}
		}
		if (debug) System.out.println("processAnswersFromProof(): qvars: " + qvars);

		if (debug) System.out.println("processAnswersFromProof(): proof: " + proof);
		for (TPTPFormula ps : proof) {
			if (debug) System.out.println("processAnswersFromProof(): ps: " + ps);
			if (ps != null && !StringUtil.emptyString(ps.sumo) &&
					ps.sumo.contains("ans0") && !ps.sumo.contains("?")) {
				if (debug) System.out.println("processAnswersFromProof(): has ans clause: " + ps);
				Formula answerClause = extractAnswerClause(new Formula(ps.sumo));
				if (debug) System.out.println("processAnswersFromProof(): answerClause: " + answerClause);
				if (answerClause != null)
					answers = answerClause.complexArgumentsToArrayListString(1);
				if (debug) System.out.println("processAnswersFromProof(): answers: " + answers);
				break;
			}
		}
		if (answers == null || vars.size() != answers.size()) {
			if (debug) System.out.println("Error in processAnswersFromProof(): null answers");
			return;
		}
		for (int i = 0; i < qvars.size(); i++) {
			bindingMap.put(qvars.get(i),answers.get(i));
		}
		if (debug) System.out.println("processAnswersFromProof(): bindingMap: " + bindingMap);
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
		for (String var : bindingMap.keySet()) {
			String binding = bindingMap.get(var);
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
					result = "Type contradiction for " + binding + " in " + types;
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
				bindingMap.put(var,result);
			}
			else {
				result = TPTP2SUMO.transformTerm(binding);
			}
		}
		if (debug) System.out.println("findTypesForSkolemTerms(): result: " + result);
		if (debug) System.out.println("findTypesForSkolemTerms(): bindingMap: " + bindingMap);
		return result;
	}

	/** ***************************************************************
	 * Input: s__Arc13_1
	 * Output: Arc13_1
	 */
	public static boolean isSkolemRelation(String s) {
		if (s.startsWith("esk") || s.startsWith("sK"))
			return true;
		else
			return false;
	}

	/** ***************************************************************
	 * remove skolem symbol with arity n
	 *
	 * For example,
	 * Input: esk2_1(s__Arc13_1)
	 * Output: s__Arc13_1
	 */
	private String removeEsk(String line) {

		if (isSkolemRelation(line)) {
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

		//System.out.println("TPTP3ProofProcessor.removePrefix(): " + st);
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

		ArrayList<String> lines = new ArrayList<>();
		try {
			String line;
			while ((line = lnr.readLine()) != null)
				lines.add(line);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(lnr): " +
				lines);
		parseProofOutput(lines,"",kb,new StringBuffer());
	}

    /** ***************************************************************
	 * Compute binding and proof from the theorem prover's response. Leave
	 * out the statements of relation sorts that is part of the TFF proof output.
	 * @param qlist is the list of quantifiers in order of the original query,
	 *              which is the order Vampire and Eprover will follow when
	 *              reporting answers
     */
    public void parseProofOutput (ArrayList<String> lines, String kifQuery, KB kb, StringBuffer qlist) {

		if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): before reverse: " +
				lines);
		if (KBmanager.getMgr().prover == KBmanager.Prover.VAMPIRE)
			lines = joinNreverseInputLines(lines);
        try {
            boolean inProof = false;
            boolean finishAnswersTuple = false;
            for (String line: lines) {
				if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): looking at line: " + line);
				if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): in proof: " + inProof);
				if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): finishAnswersTuple: " + finishAnswersTuple);
                if (line.indexOf("SZS output start") != -1) {
					if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): found proof, setting inconsistency to true");
					noConjecture = true; // if conjecture or negated_conjecture found in the proof then it's not inconsistent
                    inProof = true;
                    continue;
                }
                if (line.indexOf("SZS status") != -1) {
                    status = line.substring(13);
					if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): tpp.status: " + status);
                }
                if (line.indexOf("SZS answers") != -1) {
					if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): found answer line: " + line);
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
						TPTPVisitor sv = new TPTPVisitor();
						if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar,2): line: " + line);
						sv.parseString(line);
						if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar,2): result: " + sv.result);
                        if (sv.result != null) {
                        	if (sv.result.values().size() > 1)
                        		System.out.println("Error in TPTP3ProofProcessor.parseProofOutput(ar,2): more than one line in " + line);
                        	TPTPFormula step = sv.result.values().iterator().next();
                        	if (step.role.equals("negated_conjecture") || step.role.equals("conjecture"))
                        		noConjecture = false;
							if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar,2): step.sumo: " + step.sumo);
							if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar,2): step type: " + step.role);
                        	if (!step.role.equals("type"))
                            	proof.add(step);
                        	if (step.sumo.equals("false"))
                        		containsFalse = true;
							if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar,2): adding line: " +
									line + "\nas " + sv.result);
						}
                    }
                }
            }
			if ((status.equals("Refutation") || status.equals("CounterSatisfiable")) && noConjecture) {
				inconsistency = true;
				System.out.println("*****************************************");
				System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): Danger! possible inconsistency!");
				System.out.println("*****************************************");
			}
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
		if (debug) System.out.println("TPTP3ProofProcess.parseProofOutput(ar,2): before pruning: " + this);
		if (debug) System.out.println("TPTP3ProofProcess.parseProofOutput(ar,2): here: ");
		if (debug) System.out.println("TPTP3ProofProcess.parseProofOutput(ar,2): bindings: " + bindings);
		if (debug) System.out.println("TPTP3ProofProcess.parseProofOutput(ar,2): query: " + kifQuery);
        processAnswersFromProof(qlist,kifQuery);
        findTypesForSkolemTerms(kb);
        proof = TPTPFormula.normalizeProofStepNumbers(proof);
		if (debug) System.out.println("TPTP3ProofProcess.parseProofOutput(ar,2): result: " + this);
		if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(lnr): idTable: " + idTable);
		if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(lnr): proof ids: ");
		if (debug)
			for (TPTPFormula ps : proof)
				System.out.println(ps.name);
		if (debug) System.out.println("TPTP3ProofProcess.parseProofOutput(): returning bindings: " + bindings);
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
	public ArrayList<String> parseAnswerTuples(ArrayList<String> st, String strQuery, KB kb, StringBuffer qlist) {

		ArrayList<String> answers = new ArrayList<>();
		parseProofOutput(st, strQuery, kb, qlist);
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
	public void parseProofFromFile (String filename, KB kb) {

		try {
			File f = new File(filename);
			if (!f.exists()) {
				System.out.println("Error in parseProofFromFile() no such file " + filename);
			}
			FileReader fr = new FileReader(filename);
			LineNumberReader lnr = new LineNumberReader(fr);
			parseProofOutput(lnr, kb);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
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
	 * @return an int from the idTable or add a new id to the table and
	 * give it a new number
	 */
	public int getNumFromIDtable(String id) {

		if (debug) System.out.println("getNumFromIDtable(): " + id);
		if (!idTable.containsKey(id))
			idTable.put(id,idCounter++);
		if (debug) System.out.println("getNumFromIDtable(): id,counter: " + id + "," + idCounter);
		return idTable.get(id);
	}

	/** ***************************************************************
	 */
	private ArrayList<Integer> getSupports (PrologTerm pt) {

		if (debug) System.out.println("TPTP3ProofProcess.getSupports(PrologTerm): " + pt);
		if (debug) System.out.println("getSupports(): string,type: " + pt.toString() + "\t" + pt.getType());
		ArrayList<Integer> supports = new ArrayList<>();
		if (pt.getType() == ATOM)
			supports.add(getNumFromIDtable(pt.toString()));
		else if (pt.getType() == LIST) {
			for (PrologTerm pit : (PrologList) pt)
				supports.addAll(getSupports(pit));
		}
		else if (pt.getType() == STRUCT) {
			String predString = pt.getFunctor().toString();
			if (predString.equals("inference"))
				supports.addAll(getSupports(((PrologStruct) pt).getTermAt(2)));
			else if (predString.equals("cnf") || predString.equals("fof") ||
					predString.equals("tff") || predString.equals("thf"))
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

	/** *************************************************************
	 */
	private ArrayList<String> createProofDotGraphBody() {

		ArrayList<String> lines = new ArrayList<>();
		for (TPTPFormula ps : proof) {
			if (tptpProof) {
				if (StringUtil.emptyString(ps.name))
					continue;
				if (debug) System.out.println("createProofDotGraphBody()" + ps.name);
				if (debug) System.out.println("createProofDotGraphBody()" + ps);
				String line = StringUtil.wordWrap(ps.formula,40);
				String[] split = line.split(System.getProperty("line.separator"));
				StringBuffer sb = new StringBuffer();
				for (String s : split)
					sb.append(s + " <br align=\"left\"/> ");
				String formatted = sb.toString();
				formatted = formatted.replaceAll("&", "&amp;"); // the 'and' character in TPTP
				formatted = formatted.replaceAll("<=>", "&lt;=&gt;");
				formatted = formatted.replaceAll("=>", "=&gt;");
				String newline = "n" + ps.name + " [shape=\"box\" label = < " + formatted + " <br align=\"left\"/> > ]";
				lines.add(newline);
			}
			else {
				String formula = "";
				if (ps.formula.contains("[") || ps.formula.contains("]") || ps.formula.contains("$") || ps.formula.contains("&")) // must be a TPTP formula
					formula = ps.formula.replaceAll("&", "&amp;"); // shouldn't see this char in KIF but is in TPTP
				Formula f = new Formula(ps.sumo);
				String formatted = f.format("", "&nbsp;&nbsp;", " <br align=\"left\"/> ");
				if (StringUtil.emptyString(ps.sumo))
					formatted = ps.formula;
				if (debug) System.out.println("createProofDotGraphBody(): ps.sumo: " + ps.sumo);
				if (debug) System.out.println("createProofDotGraphBody(): formatted: " + formatted);
				formatted = formatted.replaceAll("<=>", "&lt;=&gt;");
				formatted = formatted.replaceAll("=>", "=&gt;");
				formatted = formatted.replace("[", " &#91;");
				formatted = formatted.replace("]", " &#93;");
				if (debug) System.out.println("createProofDotGraphBody(): replaced: " + formatted);
				String line = "n" + ps.name + " [shape=\"box\" label = < " + formatted + " <br align=\"left\"/> > ]";
				lines.add(line);
			}
		}
		for (TPTPFormula ps : proof) {
			if (ps.supports != null && ps.supports.size() > 0) {
				for (String p : ps.supports) {
					String line = "n" + p + " -> n" + ps.name + " [ label=\"" + ps.infRule + "\" ]; ";
					lines.add(line);
				}
			}
		}
		return lines;
	}

	/** *************************************************************
	 */
	private void createProofDotGraphImage(String filename) throws IOException {

		try {
			String graphVizDir = KBmanager.getMgr().getPref("graphVizDir");
			String command = graphVizDir + File.separator + "dot " + filename + ".dot -Tgif";
			Process proc = Runtime.getRuntime().exec(command);
			System.out.println("Graph.createDotGraph(): exec command: " + command);
			BufferedInputStream img = new BufferedInputStream(proc.getInputStream());
			RenderedImage image = ImageIO.read(img);
			File file = new File(filename + ".gif");
			if (image != null)
				ImageIO.write(image, "gif", file);
			System.out.println("Graph.createDotGraph(): write image file: " + file);
		}
		catch (java.io.IOException e) {
			String err = "Error writing file " + filename + ".dot\n" + e.getMessage();
			throw new IOException(err);
		}
	}

	/** *************************************************************
	 * Create a proof
	 * in a format suitable for GraphViz' input format
	 * http://www.graphviz.org/
	 * Generate a GIF from the .dot output with a command like
	 *  dot SUMO-graph.dot -Tgif > graph.gif
	 */
	public String createProofDotGraph() throws IOException {

		FileWriter fw = null;
		PrintWriter pw = null;
		String sep = File.separator;
		String link = "graph" + sep + "proof.gif";
		String dir = System.getenv("CATALINA_HOME") + sep + "webapps" +
				sep + "sigma" + sep + "graph";
		String filename = dir + sep + "proof";

		try {
			File dirfile = new File(dir);
			if (!dirfile.exists())
				dirfile.mkdir();
			fw = new FileWriter(filename + ".dot");
			System.out.println("Graph.createGraphBody(): creating file at " + filename + ".dot");
			pw = new PrintWriter(fw);
			HashSet<String> result = new HashSet<String>();
			result.addAll(createProofDotGraphBody());
			pw.println("digraph G {");
			pw.println("  rankdir=LR");
			for (String s : result)
				pw.println(s);
			pw.println("}");
			pw.close();
			fw.close();
			createProofDotGraphImage(filename);
		}
		catch (java.io.IOException e) {
			String err = "Error writing file " + filename + ".dot\n" + e.getMessage();
			throw new IOException(err);
		}
		finally {
			if (pw != null) pw.close();
			if (fw != null) fw.close();
		}
		return link;
	}

	/** ***************************************************************
	 * Print proof removing some steps based on proof level
	 * 1 = full proof
	 * 2 = remove steps with single support
	 * 3 = show only axioms from the KB
	 */
	public void printProof(int level) {

		for (TPTPFormula ps : proof) {
			switch (level) {
				case 1 :
					System.out.println(ps); break;
				case 2 :
					if (ps.supports.size() != 1)
						System.out.println(ps); break;
				case 3 :
					if (TPTPutil.sourceAxiom(ps))
						System.out.println(ps); break;
			}
		}
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
	public static void showHelp() {

		System.out.println("TPTP3ProofProcessor class");
		System.out.println("  options (with a leading '-'):");
		System.out.println("  f <file> - parse a TPTP3 proof file");
		System.out.println("    e - parse proof as an E proof (forward style)");
		System.out.println("  t - run test");
		System.out.println("  h - show this help");
	}

	/** ***************************************************************
	 */
	public static void main(String[] args) throws IOException {

		System.out.println("INFO in TPTP3ProofProcessor.main()");
		if (args == null)
			System.out.println("no command given");
		else
			System.out.println(args.length + " : " + Arrays.toString(args));
		if (args != null && args.length > 0 && args[0].equals("-h"))
			showHelp();
		else {
			TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
			KBmanager.prefOverride.put("loadLexicons","false");
			KBmanager.getMgr().initializeOnce();
			String kbName = KBmanager.getMgr().getPref("sumokbname");
			KB kb = KBmanager.getMgr().getKB(kbName);
			if (args != null && args.length > 1 && args[0].contains("f")) {
				try {
					if (args[0].contains("e"))
						KBmanager.getMgr().prover = KBmanager.Prover.EPROVER;
					else
						KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
					List<String> lines = FileUtil.readLines(args[1],false);
					String query = "(maximumPayloadCapacity ?X (MeasureFn ?Y ?Z))";
					StringBuffer answerVars = new StringBuffer("?X ?Y ?Z");
					System.out.println("input: " + lines + "\n");
					tpp.parseProofOutput((ArrayList<String>) lines, query, kb,answerVars);
					tpp.createProofDotGraph();
					System.out.println("TPTP3ProofProcessor.main(): " + tpp.proof.size() + " steps ");
					for (TPTPFormula step : tpp.proof) {
						System.out.println(":: " + step);
						Formula f = new Formula(step.sumo);
						System.out.println(f.format("","  ","\n"));
					}
					System.out.println("TPTP3ProofProcessor.main() bindings: " + tpp.bindingMap);
					System.out.println("TPTP3ProofProcessor.main() skolems: " + tpp.skolemTypes);
					//String link = tpp.createProofDotGraph();
					//System.out.println("Dot graph at: " + link);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			else if (args != null && args.length > 0 && args[0].contains("t"))
				tpp.testPrologParser();
			else
				showHelp();
		}
	}
}
