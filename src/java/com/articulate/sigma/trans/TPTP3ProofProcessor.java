/*
This code is copyright Articulate Software (c) 2003.
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
import com.articulate.sigma.nlg.LanguageFormatter;
import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.StringUtil;

import com.igormaznitsa.prologparser.DefaultParserContext;
import com.igormaznitsa.prologparser.GenericPrologParser;
import com.igormaznitsa.prologparser.ParserContext;
import com.igormaznitsa.prologparser.PrologParser;
import com.igormaznitsa.prologparser.terms.*;
import com.igormaznitsa.prologparser.tokenizer.Op;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tptp_parser.*;

import static com.igormaznitsa.prologparser.terms.TermType.*;

public class TPTP3ProofProcessor {

    public static boolean debug = false;
    public String status;
    public boolean noConjecture = false;
    public boolean inconsistency = false;
    public boolean containsFalse = false;
    public List<String> bindings = new ArrayList<>();
    public Map<String, String> bindingMap = new HashMap<>();
    public Map<String, String> skolemTypes = new HashMap<>();
    public List<TPTPFormula> proof = new ArrayList<>();

    public enum GraphFormulaFormat { SUO_KIF, TPTP }
    private GraphFormulaFormat graphFormulaFormat = GraphFormulaFormat.SUO_KIF;

    // a map of original ID keys and renumbered key values
    public Map<String, Integer> idTable = new HashMap<>();
    private int idCounter = 0;

    /**
     * ***************************************************************
     */
    public TPTP3ProofProcessor() {
    }

    /* Convert bindings in list to string */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("Answers:");
        if (bindingMap != null && !bindingMap.keySet().isEmpty()) {
            for (String s : bindingMap.keySet()) {
                sb.append(s).append(" = ").append(bindingMap.get(s)).append(Formula.SPACE);
            }
        } else {
            for (String s : bindings) {
                sb.append(s).append(Formula.SPACE);
            }
        }
        sb.append("\n");
        for (TPTPFormula ps : proof) {
            sb.append(ps).append("\n");
        }
        sb.append(status).append("\n");
        return sb.toString();
    }

    /**
     * ***************************************************************
     * Join TPTP3 proof statements that are formatted over multiple lines. Note
     * that comment lines are left unchanged.
     */
    public static List<String> joinLines(ArrayList<String> inputs) {

        List<String> outputs = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean before = true;
        for (String s : inputs) {
            if (s.trim().startsWith("%")) {
                sb = new StringBuilder();
                outputs.add(s.trim());
            } else if (s.trim().endsWith(").")) {
                before = false;
                sb.append(s);
                outputs.add(sb.toString());
                sb = new StringBuilder();
            } else {
                sb.append(s.trim());
            }
        }
        return outputs;
    }

    /**
     * ***************************************************************
     * Join TPTP3 proof statements that are formatted over multiple lines and
     * reverse them for Vampire, which presents proofs in reverse order. Note
     * that comment lines are left unchanged.
     */
    public static List<String> joinNreverseInputLines(List<String> inputs) {

        List<String> outputs = new ArrayList<>();
        List<String> commentsBefore = new ArrayList<>();
        List<String> commentsAfter = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean before = true;
        for (String s : inputs) {
            if (s.trim().startsWith("%")) {
                if (before) {
                    commentsBefore.add(s);
                } else {
                    commentsAfter.add(s);
                }
                sb.setLength(0); // reset
            } else if (s.trim().endsWith(").") || s.trim().endsWith("]")) {
                before = false;
                sb.append(s);
                outputs.add(sb.toString());
                sb.setLength(0); // reset
            } else {
                sb.append(s.trim());
            }
        }
        Collections.reverse(outputs);
        List<String> result = new ArrayList<>();
        for (String s : commentsBefore) {
            result.add(s);
        }
        for (String s : outputs) {
            result.add(s);
        }
        for (String s : commentsAfter) {
            result.add(s);
        }
        return result;
    }

    /**
     * ***************************************************************
     * return the predicate and arguments to a valid prolog expression
     * p(a1,a2...an) where a1..an are an atom, a string or a prolog expression
     */
    public static List<String> getPrologArgs(String line) {

        List<String> result = new ArrayList<>();
        int parenLevel = 0;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < line.length()) {
            switch (line.charAt(i)) {
                case '(':
                    if (parenLevel == 0) {
                        result.add(sb.toString());
                        sb.setLength(0); // reset
                        parenLevel++;
                    } else {
                        parenLevel++;
                        sb.append(line.charAt(i));
                    }
                    break;
                case ')':
                    parenLevel--;
                    if (parenLevel == 0) {
                        result.add(sb.toString());
                        sb.setLength(0); // reset
                    } else {
                        sb.append(line.charAt(i));
                    }
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
                        sb.setLength(0); // reset
                    } else {
                        sb.append(line.charAt(i));
                    }
                    break;
                default:
                    sb.append(line.charAt(i));
                    break;
            }
            i++;
        }
        return result;
    }

    /**
     * ***************************************************************
     */
    public String getInferenceType(String supportId) {

        String inferenceType = null;
        if (supportId.startsWith("inference(")) {
            int firstParen = supportId.indexOf(Formula.LP);
            int firstComma = supportId.indexOf(",");
            inferenceType = supportId.substring(firstParen + 1, firstComma);
        } else if (supportId.startsWith("file(")) {
            int firstParen = supportId.indexOf(Formula.LP);
            int firstComma = supportId.indexOf(",");
            int secondParen = supportId.indexOf(Formula.RP, firstComma + 1);
            inferenceType = supportId.substring(firstComma + 1, secondParen);
        } else if (supportId.startsWith("introduced(")) {
            int firstParen = supportId.indexOf(Formula.LP);
            int firstComma = supportId.indexOf(",");
            inferenceType = "introduced:" + supportId.substring(firstParen + 1, firstComma);
        }
        return inferenceType;
    }

    /**
     * ***************************************************************
     * Parse support / proof statements in the response
     */
    public List<Integer> supportIdToInts(List<String> supportIds) {

        List<Integer> prems = new ArrayList<>();
        for (String s : supportIds) {
            prems.add(getNumFromIDtable(s));
        }
        return prems;
    }

    /**
     * ***************************************************************
     * Parse support / proof statements in the response
     */
    public List<Integer> parseSupports(String supportId) {

        if (debug) System.out.println("Info in TPTP3ProofProcessor.parseSupports(): " + supportId);

        List<Integer> prems = new ArrayList<>();
        if (supportId.startsWith("[") && !supportId.equals("[]")) {
            //supportId = trimBrackets(supportId).trim();
            supportId = StringUtil.removeEnclosingCharPair(supportId, 1, '[', ']').trim();
            //System.out.println("Info in TPTP3ProofProcessor.parseSupports()2: " + supportId);
            if (supportId.startsWith("inference(")) {
                return getSupports(supportId + ".");
            }
            String[] supportSet = supportId.split(",");
            Integer stepnum;
            for (String supportSet1 : supportSet) {
                if (debug) System.out.println("Info in TPTP3ProofProcessor.parseSupports(): support element: " + supportSet1);
                if (!supportSet1.contains(Formula.LP)) {
                    if (!supportSet1.trim().startsWith("[symmetry]")) {
                        stepnum = getNumFromIDtable(supportSet1.trim());
                        if (stepnum == null) {
                            System.err.println("Error in TPTP3ProofProcessor.parseSupports() no id: " + stepnum
                                    + " for premises at step " + supportSet1);
                        } else {
                            prems.add(stepnum);
                        }
                    }
                }
            }
        } else if (supportId.startsWith("file(")) {
            return prems;
        } else if (supportId.startsWith("inference(")) {
            return getSupports(supportId + ".");
        } else if (supportId.startsWith("introduced(")) { // there should never be premises for this
            return prems;
        } else {
            Integer stepnum = getNumFromIDtable(supportId);
            if (stepnum == null) {
                return prems;
            } else {
                prems.add(stepnum);
                return prems;
            }
        }
        return prems;
    }

    /**
     * ***************************************************************
     * Parse a step like the following into its constituents fof(c_0_5, axiom,
     * (s__subclass(s__Artifact,s__Object)), c_0_3).
     *
     * fof(c_0_2, negated_conjecture,
     * (~(?[X1]:(s__subclass(X1,s__Object)&~$answer(esk1_1(X1))))),
     * inference(assume_negation,[status(cth)],[inference(add_answer_literal,[status(thm)],[c_0_0,
     * theory(answers)])])).
     */
    public ProofStep parseProofStep(String line) {

        if (debug) System.out.println("-----------------------");
        if (debug) System.out.println("parseProofStep() line: " + line);
        if (StringUtil.emptyString(line)) {
            return null;
        }
        line = line.replaceAll(System.lineSeparator(), "");
        if (line.startsWith("%")) {
            if (debug) System.out.println("TPTP3ProofProcessor.parseProofStep() skipping comment: " + line);
            return null;
        }
        ProofStep ps = new ProofStep();
        line = line.replaceAll("\\$answer\\(", "answer(");
        if (debug) System.out.println("TPTP3ProofProcessor.parseProofStep(): after remove $answer: " + line);
        try {
            TPTPVisitor sv = new TPTPVisitor();
            sv.parseString(line);
            Map<String, TPTPFormula> hm = sv.result;
            if (hm != null) {
                TPTPFormula tptpF;
                int firstParen, firstComma, secondParen;
                for (String tptpid : hm.keySet()) {
                    tptpF = hm.get(tptpid);
                    ps.axiom = TPTP2SUMO.collapseConnectives(new Formula(tptpF.sumo)).toString();
                    ps.formulaType = tptpF.type;
                    System.out.println("TPTP3ProofProcessor.parseProofStep(): type: " + tptpF.type);
                    idTable.put(tptpF.name, getNumFromIDtable(tptpF.name));
                    ps.number = getNumFromIDtable(tptpF.name);
                    ps.formulaRole = tptpF.role;
                    ps.inferenceType = tptpF.infRule;
                    ps.input = line;
                    ps.premises.addAll(supportIdToInts(tptpF.supports));
                    if (tptpF.supports != null && tptpF.supports.size() == 1 && tptpF.supports.get(0).startsWith("file(")) {
                        firstParen = tptpF.supports.get(0).indexOf(Formula.LP);
                        firstComma = tptpF.supports.get(0).indexOf(",");
                        secondParen = tptpF.supports.get(0).indexOf(Formula.RP, firstComma + 1);
                        ps.sourceID = tptpF.supports.get(0).substring(firstComma + 1, secondParen);
                    }
                }
            } else {
                ps.axiom = line;
            }
        } catch (Exception e) {
            System.err.println("Error in TPTP3ProofProcessor.parseProofStep(): " + e.getMessage());
            e.printStackTrace();
            System.out.println("TPTP3ProofProcessor.parseProofStep(): with input: " + line);
            ps.axiom = line;
        }
        if (ps.formulaRole.trim().equals("negated_conjecture") || ps.formulaRole.trim().equals("conjecture")) {
            if (debug) {
                System.out.println("TPTP3ProofProcessor.parseProofStep(): found conjecture or negated_conjecture, setting inconsistency to false");
            }
            noConjecture = false;
        }
        return ps;
    }

    /**
     * ***************************************************************
     * Return bindings from TPTP3 answer tuples
     */
    public void processAnswers(String line) {

        if (debug) {
            System.out.println("INFO in processAnswers(): line: " + line);
        }
        //String trimmed = trimBrackets(line);
        String trimmed = StringUtil.removeEnclosingCharPair(line, 1, '[', ']');
        if (debug) {
            System.out.println("INFO in processAnswers(): trimmed: " + trimmed);
        }
        if (trimmed == null) {
            System.err.println("Error in TPTP3ProofProcessor.processAnswers() bad format: " + line);
            return;
        }
        String[] answers = trimmed.split("\\|");
        if (debug) {
            System.out.println("INFO in processAnswers(): answers: " + Arrays.toString(answers));
        }
        String answer;
        String[] esks;
        for (String answer1 : answers) {
            if (answer1.equals("_")) {
                break;
            }
            //String answer = trimBrackets(answers[i]);
            answer = answer1;
            if (debug) {
                System.out.println("INFO in processAnswers(): answer: " + answer);
            }
            if (answer != null) {
                if (answer.startsWith("[")) {
                    answer = StringUtil.removeEnclosingCharPair(answer, 1, '[', ']');
                }
                esks = answer.split(",");
                for (String esk : esks) {
                    if (debug) {
                        System.out.println("INFO in processAnswers(): esk: " + esk);
                    }
                    //answer = removeEsk(esk);
                    answer = removePrefix(esk);
                    if (debug) {
                        System.out.println("INFO in processAnswers(): binding: " + answer);
                    }
                    bindings.add(answer);
                }
            }
        }
        if (debug) {
            System.out.println("INFO in processAnswers(): returning bindings: " + bindings);
        }
    }

    /**
     * ***************************************************************
     * Return the answer clause, or null if not present
     */
    public Formula extractAnswerClause(Formula ax) {

        if (debug) {
            System.out.println("extractAnswerClause(): " + ax.getFormula());
        }
        if (!ax.listP()) {
            return null;
        }
        String pred = ax.car();
        if (debug) {
            System.out.println("extractAnswerClause(): pred: " + pred);
        }
        if (Formula.atom(pred)) {
            if (KBmanager.getMgr().prover == KBmanager.Prover.VAMPIRE && pred.equals("ans0")) {
                return ax;
            }
            if (KBmanager.getMgr().prover == KBmanager.Prover.EPROVER && pred.equals("answer")) {
                return ax;
            }
        } else {
            Formula predF = extractAnswerClause(ax.carAsFormula());
            if (debug) {
                System.out.println("extractAnswerClause(): predF: " + predF);
            }
            if (predF != null) {
                return predF;
            }
        }
        List<Formula> args = ax.complexArgumentsToArrayList(1);
        if (debug) {
            System.out.println("extractAnswerClause(): args: " + args);
        }

        if (args == null) {
            return null;
        }
        if (debug) {
            System.out.println("extractAnswerClause(): args size: " + args.size());
        }
        Formula argF;
        for (Formula f : args) {
            if (debug) {
                System.out.println("extractAnswerClause(): check arg: " + f);
            }
            argF = extractAnswerClause(f);
            if (debug) {
                System.out.println("extractAnswerClause(): argF: " + argF);
            }
            if (argF != null) {
                return argF;
            } else if (debug) {
                System.out.println("extractAnswerClause(): returns null for check arg: " + argF);
            }
        }
        return null;
    }

    /**
     * ***************************************************************
     * Remove duplicates in an Array without changing the order
     */
    public List<String> removeDupInArray(List<String> input) {

        List<String> result = new ArrayList<>();
        for (String s : input) {
            if (!result.contains(s)) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * ***************************************************************
     * Put bindings from TPTP3 proof answer variables into bindingMap
     */
    public void processAnswersFromProof(StringBuilder qlist, String query) {

        Formula qform = new Formula(query);
        List<String> answers = null;
        List<String> vars = qform.collectAllVariablesOrdered();
        vars = removeDupInArray(vars);
        List<String> qvars = new ArrayList<>();
        if (debug) {
            System.out.println("processAnswersFromProof(): vars: " + vars);
        }
        if (debug) {
            System.out.println("processAnswersFromProof(): qlist: " + qlist);
        }
        if (debug) {
            System.out.println("processAnswersFromProof(): query: " + query);
        }
        if (debug) {
            System.out.println("processAnswersFromProof(): bindingMap: " + bindingMap);
        }
        bindingMap.clear();
        String news;
        if (qlist != null && qlist.length() > 0) {
            List<String> qvarslist = Arrays.asList(qlist.toString().split(","));
            for (String s : qvarslist) {
                news = s.replace(Formula.TERM_VARIABLE_PREFIX, Formula.V_PREF);
                qvars.add(news);
            }
        }
        if (debug) {
            System.out.println("processAnswersFromProof(): qvars: " + qvars);
        }

        if (debug) {
            System.out.println("processAnswersFromProof(): proof: " + proof);
        }
        Formula answerClause;
        for (TPTPFormula ps : proof) {
            if (debug) {
                System.out.println("processAnswersFromProof(): ps: " + ps);
            }
            if (ps != null && !StringUtil.emptyString(ps.sumo)
                    && ps.sumo.contains("ans0") && !ps.sumo.contains(Formula.V_PREF)) {
                if (debug) {
                    System.out.println("processAnswersFromProof(): has ans clause: " + ps);
                }
                answerClause = extractAnswerClause(new Formula(ps.sumo));
                if (debug) {
                    System.out.println("processAnswersFromProof(): answerClause: " + answerClause);
                }
                if (answerClause != null) {
                    answers = answerClause.complexArgumentsToArrayListString(1);
                }
                if (debug) {
                    System.out.println("processAnswersFromProof(): answers: " + answers);
                }
                break;
            }
        }
        if (answers == null || vars.size() != answers.size()) {
            if (debug) {
                System.err.println("Error in processAnswersFromProof(): null answers");
            }
            return;
        }
        for (int i = 0; i < qvars.size(); i++) {
            bindingMap.put(qvars.get(i), answers.get(i));
        }
        if (debug) {
            System.out.println("processAnswersFromProof(): bindingMap: " + bindingMap);
        }
    }

    /**
     * ***************************************************************
     * Returns the most specific type for skolem variable.
     *
     * @param kb The knowledge base used to find skolem term's types
     *
     * For example, original binding = esk3_0 set binding = "An instance of
     * Human" (Human is the most specific type for esk3_0 in the given proof)
     *
     * original binding = esk3_1 set binding = "An instance of Human, Agent" (If
     * multiple types are found for esk3_1)
     *
     * @return the most specific type for skolem variable
     */
    public String findTypesForSkolemTerms(KB kb) {

        String result = "";
        if (debug) {
            System.out.println("findTypesForSkolemTerms(): bindings: " + bindings);
        }
        if (debug) {
            System.out.println("findTypesForSkolemTerms(): bindings map: " + bindingMap);
        }
        FormulaPreprocessor fp = new FormulaPreprocessor();
        String binding;
        List<String> skolemStmts, l;
        Set<String> types;
        Formula f;
        boolean start;
        for (String var : bindingMap.keySet()) {
            binding = bindingMap.get(var);
            if (binding.startsWith("esk") || binding.startsWith("(sK")) {
                skolemStmts = ProofProcessor.returnSkolemStmt(binding, proof);
                if (debug) {
                    System.out.println("findTypesForSkolemTerms(): skolem stmts: " + skolemStmts);
                }
                types = new HashSet<>();
                for (String skolemStmt : skolemStmts) {
                    f = new Formula(skolemStmt);
                    l = f.complexArgumentsToArrayListString(0);
                    if (l.size() != 3) {
                        continue;
                    }
                    if (l.get(0).equals("names") || l.get(0).equals("instance") || l.get(0).equals("subclass")) {
                        types.add(l.get(2));
                    }
                }
                if (kb.kbCache.checkDisjoint(kb, types)) {
                    // check if there are contradiction among the types returned
                    //bindings.remove(binding);
                    result = "Type contradiction for " + binding + " in " + types;
                    //bindings.add(binding);
                    KBcache.errors.clear();
                } else {
                    fp.winnowTypeList(types, kb);
                    if (types != null && !types.isEmpty()) {
                        if (types.size() == 1) {
                            result = "an instance of " + types.toArray()[0];
                        } else {
                            result = "an instance of ";
                            start = true;
                            for (String t : types) {
                                if (start) {
                                    result += t;
                                    start = false;
                                } else {
                                    result += ", " + t;
                                }
                            }
                        }
                    }
                }
                bindingMap.put(var, result);
            } else {
                result = TPTP2SUMO.transformTerm(binding);
            }
        }
        if (debug) {
            System.out.println("findTypesForSkolemTerms(): result: " + result);
        }
        if (debug) {
            System.out.println("findTypesForSkolemTerms(): bindingMap: " + bindingMap);
        }
        return result;
    }

    /**
     * ***************************************************************
     * Input: s__Arc13_1 Output: Arc13_1
     */
    public static boolean isSkolemRelation(String s) {

        return s.startsWith("esk") || s.startsWith("sK");
    }

    /**
     * ***************************************************************
     * remove skolem symbol with arity n
     *
     * For example, Input: esk2_1(s__Arc13_1) Output: s__Arc13_1
     */
    private String removeEsk(String line) {

        if (isSkolemRelation(line)) {
            int leftParen = line.indexOf(Formula.LP);
            int rightParen = line.indexOf(Formula.RP);
            if (leftParen != -1 && rightParen != -1) {
                return line.substring(leftParen + 1, rightParen);
            }
        }
        return line;
    }

    /**
     * ***************************************************************
     * Input: s__Arc13_1 Output: Arc13_1
     */
    private String removePrefix(String st) {

        //System.out.println("TPTP3ProofProcessor.removePrefix(): " + st);
        String tsp = Formula.TERM_SYMBOL_PREFIX;
        if (st.startsWith(tsp)) {
            return st.substring(tsp.length(), st.length());
        } else {
            return st;
        }
    }

    /**
     * ***************************************************************
     * Print out prover's bindings
     */
    public void printAnswers() {

        System.out.println("Answers:");
        for (int i = 0; i < bindings.size(); i++) {
            System.out.println(bindings.get(i));
        }
    }

    /**
     * ***************************************************************
     * Compute bindings and proof from theorem prover's response
     */
    public void parseProofOutput(LineNumberReader lnr, KB kb) {

        List<String> lines = new ArrayList<>();
        try {
            String line;
            while ((line = lnr.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (debug) {
            System.out.println("TPTP3ProofProcessor.parseProofOutput(lnr): "
                    + lines);
        }
        parseProofOutput(lines, "", kb, new StringBuilder());
    }

    /**
     * Parses TPTP (Thousands of Problems for Theorem Provers) proof output from a theorem prover
     * and extracts proof steps, answer bindings, and status information.
     *
     * This method processes the textual output lines from automated theorem provers (such as Vampire or E Prover),
     * identifying and extracting:
     * <ul>
     *   <li>SZS status information (e.g., "Refutation", "Theorem", "CounterSatisfiable")</li>
     *   <li>Answer bindings for query variables in bracketed format</li>
     *   <li>Individual proof steps in TPTP formula format (fof/cnf/tff)</li>
     *   <li>Proof metadata including support relationships and inference rules</li>
     * </ul>
     *
     * The method performs several post-processing operations:
     * <ul>
     *   <li>Detects potential knowledge base inconsistencies (refutations without conjectures)</li>
     *   <li>Strips proof step numbers from Vampire output if present</li>
     *   <li>Filters out type declarations in TFF proofs</li>
     *   <li>Normalizes proof step numbering for consistent display</li>
     *   <li>Extracts types for Skolem terms introduced during proving</li>
     * </ul>
     *
     * Special handling for Vampire prover: Input lines are joined and reversed before processing
     * since Vampire presents proofs in reverse order.
     *
     * @param lines the proof output lines returned by the theorem prover
     * @param kifQuery the original query in KIF (Knowledge Interchange Format) syntax
     * @param kb the knowledge base being queried
     * @param qlist a StringBuilder containing the list of quantified variables in order,
     *              used for answer extraction and binding
     *
     * @see TPTPVisitor
     * @see TPTPFormula
     * @see #processAnswers(String)
     * @see #processAnswersFromProof(StringBuilder, String)
     * @see #findTypesForSkolemTerms(KB)
     */
    public void parseProofOutput(List<String> lines, String kifQuery, KB kb, StringBuilder qlist) {

        if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): before reverse: " + lines);
        if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): # lines: " + lines.size());
        if (KBmanager.getMgr().prover == KBmanager.Prover.VAMPIRE) {
//            lines = joinNreverseInputLines(lines);
            lines = TPTPutil.clearProofFile(lines);
        }
        if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): after reverse: " + lines);
        try {
            String bracketedAnswers, szs_status = "SZS status", scratch;
            boolean inProof = false, finishAnswersTuple = false;
            int end, idx;
            TPTPVisitor sv;
            TPTPFormula step;
            for (String line : lines) {
                if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): looking at line: " + line);
                if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): in proof: " + inProof);
                if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): finishAnswersTuple: " + finishAnswersTuple);
                if (line.contains("SZS output start")) {
                    if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): found proof, setting inconsistency to true");
                    noConjecture = true; // if conjecture or negated_conjecture found in the proof then it's not inconsistent
                    inProof = true;
                    continue;
                }
                if (line.contains(szs_status)) {
                    idx = line.indexOf(szs_status);
                    scratch = line.substring(idx + szs_status.length()).trim();
                    if (scratch.contains(Formula.SPACE)) {
                        idx = scratch.indexOf(Formula.SPACE);
                        status = scratch.substring(0, idx);
                    }
                    else
                        status = scratch;
                    if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): tpp.status: " + status);
                }
                if (line.contains("SZS answers")) {
                    if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): found answer line: " + line);
                    if (!finishAnswersTuple) {
                        end = line.lastIndexOf("]");
                        if (end == -1 || (line.length() < end + 1)) {
                            end = line.length();
                        }
                        bracketedAnswers = line.substring(20, end + 1);
                        processAnswers(bracketedAnswers);
                        finishAnswersTuple = true;
                    }
                }
                if (inProof) {
                    if (line.contains("SZS output end")) {
                        inProof = false;
                    } else {
                        if (line.matches("^\\d+\\..*")) {  // strip proof step number
                            int period = line.indexOf('.');
                            line = line.substring(period + 2);
                        }
                        sv = new TPTPVisitor();
                        if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar,2): line: " + line);
                        sv.parseString(line);
                        if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar,2): result: " + sv.result);
                        if (sv.result != null) {
                            if (sv.result.values().size() > 1) {
                                System.err.println("Error in TPTP3ProofProcessor.parseProofOutput(ar,2): more than one line in " + line);
                            }
                            step = sv.result.values().iterator().next();
                            if (step.role.equals("negated_conjecture") || step.role.equals("conjecture")) {
                                noConjecture = false;
                            }
                            if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar,2): step.sumo: " + step.sumo);
                            if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar,2): step type: " + step.role);
                            if (!step.role.equals("type")) {
                                proof.add(step);
                            }
                            if (step.sumo.equals("false")) {
                                containsFalse = true;
                            }
                            if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(ar,2): adding line: "
                                        + line + "\nas " + sv.result);
                        }
                    }
                }
            }
            if ((status.equals("Refutation") || status.equals("CounterSatisfiable")) && noConjecture) {
                inconsistency = true;
                System.err.println("*****************************************");
                System.err.println("TPTP3ProofProcessor.parseProofOutput(ar): Danger! possible inconsistency!");
                System.err.println("*****************************************");
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
        if (debug) System.out.println("TPTP3ProofProcess.parseProofOutput(ar,2): before pruning: " + this);
        if (debug) System.out.println("TPTP3ProofProcess.parseProofOutput(ar,2): here: ");
        if (debug) System.out.println("TPTP3ProofProcess.parseProofOutput(ar,2): bindings: " + bindings);
        if (debug) System.out.println("TPTP3ProofProcess.parseProofOutput(ar,2): query: " + kifQuery);
        processAnswersFromProof(qlist, kifQuery);
        findTypesForSkolemTerms(kb);
        proof = TPTPFormula.normalizeProofStepNumbers(proof);
        if (debug) System.out.println("TPTP3ProofProcess.parseProofOutput(ar,2): result: " + this);
        if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(lnr): idTable: " + idTable);
        if (debug) System.out.println("TPTP3ProofProcessor.parseProofOutput(lnr): proof ids: ");
        if (debug) {
            for (TPTPFormula ps : proof) {
                System.out.println(ps.name);
            }
        }
        if (debug) System.out.println("TPTP3ProofProcess.parseProofOutput(): returning bindings: " + bindings);
    }

    /**
     * ***************************************************************
     * Return a list of answers if prover finds bindings for wh- queries. Return
     * "Proof Found" if prover finds contradiction for boolean queries.
     *
     * For example, tuple_list = [[esk3_1(s__Org1_1)]|_] Output = [Org1_1]
     *
     * tuple_list = [[esk3_0]|_] Output = [An instance of Human] (Human is the
     * most specific type for esk3_0 in the given proof)
     */
    public List<String> parseAnswerTuples(List<String> st, String strQuery, KB kb, StringBuilder qlist) {

        List<String> answers = new ArrayList<>();
        parseProofOutput(st, strQuery, kb, qlist);
        if (bindings == null || bindings.isEmpty()) {
            if (proof != null && !proof.isEmpty()) {
                answers.add("Proof Found");		// for boolean queries
            }
            return answers;
        }
        return bindings;
    }

    /**
     * ***************************************************************
     */
    public void parseProofOutput(String st, KB kb) {

        try (StringReader sr = new StringReader(st);
            LineNumberReader lnr = new LineNumberReader(sr)) {
            parseProofOutput(lnr, kb);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * ***************************************************************
     */
    public void parseProofFromFile(String filename, KB kb) {

        File f = new File(filename);
        if (!f.exists()) {
            System.err.println("Error in parseProofFromFile() no such file " + filename);
        }
        try (FileReader fr = new FileReader(filename); LineNumberReader lnr = new LineNumberReader(fr)) {
            parseProofOutput(lnr, kb);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ***************************************************************
     */
    private static void printPrologTerm(PrologTerm pt, String indent) {

        System.out.println(indent + pt.toString() + "\t" + pt.getType());
        if (pt.getType() == STRUCT) {
            System.out.println("arity: " + pt.getArity());
            for (PrologTerm pit : (PrologStruct) pt) {
                printPrologTerm(pit, indent + "\t");
            }
        } else if (pt.getType() == LIST) {
            System.out.println("arity: " + pt.getArity());
            for (PrologTerm pit : (PrologList) pt) {
                printPrologTerm(pit, indent + "\t");
            }
        }
    }

    /**
     * ***************************************************************
     */
    public static void printPrologTerm(PrologTerm pt) {

        printPrologTerm(pt, "");
    }

    /**
     * ***************************************************************
     * @return an int from the idTable or add a new id to the table and give it
     * a new number
     */
    public int getNumFromIDtable(String id) {

        if (debug) {
            System.out.println("getNumFromIDtable(): " + id);
        }
        if (!idTable.containsKey(id)) {
            idTable.put(id, idCounter++);
        }
        if (debug) {
            System.out.println("getNumFromIDtable(): id,counter: " + id + "," + idCounter);
        }
        return idTable.get(id);
    }

    /**
     * ***************************************************************
     */
    private List<Integer> getSupports(PrologTerm pt) {

        if (debug) {
            System.out.println("TPTP3ProofProcess.getSupports(PrologTerm): " + pt);
        }
        if (debug) {
            System.out.println("getSupports(): string,type: " + pt.toString() + "\t" + pt.getType());
        }
        List<Integer> supports = new ArrayList<>();
        if (null != pt.getType()) {
            switch (pt.getType()) {
                case ATOM:
                    supports.add(getNumFromIDtable(pt.toString()));
                    break;
                case LIST:
                    for (PrologTerm pit : (PrologList) pt) {
                        supports.addAll(getSupports(pit));
                    }
                    break;
                case STRUCT:
                    String predString = pt.getFunctor().toString();
                    if (predString.equals("inference")) {
                        supports.addAll(getSupports(((PrologStruct) pt).getTermAt(2)));
                    } else if (predString.equals("cnf") || predString.equals("fof")
                            || predString.equals("tff") || predString.equals("thf")) {
                        supports.addAll(getSupports(((PrologStruct) pt).getTermAt(3)));
                    }
                    break;
                default:
                    break;
            }
        }
        return supports;
    }

    /**
     * ***************************************************************
     */
    private List<Integer> getSupports(String input) {

        if (debug) {
            System.out.println("TPTP3ProofProcess.getSupports(String): " + input);
        }
        List<Integer> supports = new ArrayList<>();
        try (Reader reader = new StringReader(input)) {
            if (debug) {
                System.out.println(input);
            }
            DefaultParserContext dpc = new DefaultParserContext(ParserContext.FLAG_CURLY_BRACKETS).addOps(Op.SWI);
            PrologParser parser = new GenericPrologParser(reader, dpc);
            for (PrologTerm pt : parser) {
                supports.addAll(getSupports(pt));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return supports;
    }

    /**
     * *************************************************************
     */
    private List<String> createProofDotGraphBody() {

        List<String> lines = new ArrayList<>();
        String line, formatted, formula;
        String[] split;
        StringBuilder sb;
        Formula f;
        for (TPTPFormula ps : proof) {
            if ((GraphFormulaFormat.TPTP).equals(graphFormulaFormat)) {
                if (StringUtil.emptyString(ps.name)) {
                    continue;
                }
                if (debug) {
                    System.out.println("createProofDotGraphBody()" + ps.name);
                }
                if (debug) {
                    System.out.println("createProofDotGraphBody()" + ps);
                }
                line = StringUtil.wordWrap(ps.formula, 40);
                split = line.split(System.getProperty("line.separator"));
                sb = new StringBuilder();
                for (String s : split) {
                    sb.append(s).append(" <br align=\"left\"/> ");
                }
                formatted = sb.toString();
                formatted = formatted.replaceAll("&", "&amp;"); // the 'and' character in TPTP
                formatted = formatted.replaceAll(Formula.IFF, "&lt;=&gt;");
                formatted = formatted.replaceAll(Formula.IF, "=&gt;");
                String newline = "n" + ps.name + " [shape=\"box\" label = < " + formatted + " <br align=\"left\"/> > ]";
                lines.add(newline);
            } else {
                if (ps.formula.contains("[") || ps.formula.contains("]") || ps.formula.contains("$") || ps.formula.contains("&")) // must be a TPTP formula
                {
                    formula = ps.formula.replaceAll("&", "&amp;"); // shouldn't see this char in KIF but is in TPTP
                }
                f = new Formula(ps.sumo);
                formatted = f.format("", "&nbsp;&nbsp;", " <br align=\"left\"/> ");
                if (StringUtil.emptyString(ps.sumo)) {
                    formatted = ps.formula;
                }
                if (debug) {
                    System.out.println("createProofDotGraphBody(): ps.sumo: " + ps.sumo);
                }
                if (debug) {
                    System.out.println("createProofDotGraphBody(): formatted: " + formatted);
                }
                formatted = formatted.replaceAll(Formula.IFF, "&lt;=&gt;");
                formatted = formatted.replaceAll(Formula.IF, "=&gt;");
                formatted = formatted.replace("[", " &#91;");
                formatted = formatted.replace("]", " &#93;");
                if (debug) {
                    System.out.println("createProofDotGraphBody(): replaced: " + formatted);
                }
                line = "n" + ps.name + " [shape=\"box\" label = < " + formatted + " <br align=\"left\"/> > ]";
                lines.add(line);
            }
        }
        for (TPTPFormula ps : proof) {
            if (ps.supports != null && !ps.supports.isEmpty()) {
                for (String p : ps.supports) {
                    line = "n" + p + " -> n" + ps.name + " [ label=\"" + ps.infRule + "\" ]; ";
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    /** *************************************************************
     * Creates a specified formatted image from a generated *.dot file
     * from GraphViz.
     *
     * @param filename the generated *.dot filename to create an image from
     * @return the path to the generated image file
     */
    public static String createProofDotGraphImage(String filename) throws IOException {

        int exitCode;
        String retVal = "";
        String graphVizDir = KBmanager.getMgr().getPref("graphVizDir");
        String imageExt = KBmanager.getMgr().getPref("imageFormat");
        if (imageExt == null || imageExt.isBlank())
            imageExt = "png"; // default
        File file = new File(filename + "." + imageExt);

        List<String> cmd = new ArrayList<>();
        cmd.add(graphVizDir + File.separator + "dot");
        cmd.add("-T" + imageExt);
        cmd.add("-O");
        cmd.add(filename);
        try {
            // Build a proof image from an input file
            // From: https://graphviz.org/doc/info/command.html#-O
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(file.getParentFile());
            File log = new File(file.getParentFile(),"log");
            if (log.exists())
                log.delete();
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log)); // <- in case of any errors
            Process proc = pb.start();
            exitCode = proc.waitFor();
        } catch (InterruptedException e) {
            String err = "Error writing file " + file + "\n" + e.getMessage();
            throw new IOException(err);
        }
        retVal = file.getAbsolutePath();
        return retVal;
    }

    /**
     * *************************************************************
     * Create a proof in a format suitable for GraphViz' input format
     * http://www.graphviz.org/. Generate a proof imate from the .dot output
     * with a command like <code>dot SUMO-graph.dot -Tgif > graph.gif</code>
     */
    public String createProofDotGraph() throws IOException {

        String sep = File.separator;
        String dir = System.getenv("CATALINA_HOME") + sep + "webapps"
                + sep + "sigma" + sep + "graph";
        File dirfile = new File(dir);
        if (!dirfile.exists())
            dirfile.mkdirs();
        String filename = dirfile.getPath() + sep + "proof.dot";
        Path path = Paths.get(filename);
        try (Writer bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8); PrintWriter pw = new PrintWriter(bw, true)) {
            Set<String> result = new HashSet<>();
            result.addAll(createProofDotGraphBody());
            pw.println("digraph G {");
            pw.println("  node [color=black, fontcolor=black];"); // Black text and borders
            pw.println("  edge [color=black];"); // Black edges
            pw.println("  rankdir=LR");
            for (String s : result)
                pw.println(s);
            pw.println("}");
        } catch (IOException e) {
            String err = "Error writing file " + path + "\n" + e.getMessage();
            throw new IOException(err);
        }
        return createProofDotGraphImage(path.toString());
    }

    public void setGraphFormulaFormat(GraphFormulaFormat fmt) {
        this.graphFormulaFormat = (fmt == null) ? GraphFormulaFormat.SUO_KIF : fmt;
    }

    /**
     * ***************************************************************
     * Print proof removing some steps based on proof level 1 = full proof 2 =
     * remove steps with single support 3 = show only axioms from the KB
     */
    public void printProof(int level) {

        for (TPTPFormula ps : proof) {
            switch (level) {
                case 1:
                    System.out.println(ps);
                    break;
                case 2:
                    if (ps.supports.size() != 1) {
                        System.out.println(ps);
                    }
                    break;
                case 3:
                    if (TPTPutil.sourceAxiom(ps)) {
                        System.out.println(ps);
                    }
                    break;
            }
        }
    }

    /**
     * ***************************************************************
     * Look through the chain of supporting steps for a source axiom or an axiom
     * with more than one premise.
     */
    private String renumberOneSupport(Map<String,TPTPFormula> ftable, String sup) {

        if (debug) System.out.println("renumberOneSupport(): get formula for: " + sup);

        TPTPFormula f = ftable.get(sup);
        if (f == null) return sup;

        if (debug) System.out.println("renumberOneSupport(): formula: " + f);

        // Don’t lift through: headers, axioms, multi-premise, or no supports
        if (isHeaderRole(f) || f.supports == null || f.supports.size() != 1 || TPTPutil.sourceAxiom(f))
            return sup;

        String newsup = sup;
        Set<String> seen = new HashSet<>();

        while (f != null
                && f.supports != null
                && f.supports.size() == 1
                && !TPTPutil.sourceAxiom(f)
                && !isHeaderRole(f)
                && seen.add(f.name)) {

            String parent = f.supports.get(0);
            TPTPFormula pf = ftable.get(parent);

            // If the next parent is a header, stop THERE (don’t climb past it)
            if (pf != null && isHeaderRole(pf)) {
                newsup = parent;
                break;
            }

            newsup = parent;
            if (debug) System.out.println("renumberOneSupport(): climb to: " + newsup);

            f = pf;
            if (f == null) break;
        }

        if (debug) System.out.println("renumberOneSupport(): returning: " + newsup);
        return newsup;
    }


    private boolean isHeaderRole(TPTPFormula x) {
        if (x == null || x.role == null) return false;
        String r = x.role.trim();
        return "negated_conjecture".equals(r) || "conjecture".equals(r);
    }

    /**
     * ***************************************************************
     * Look through the chain of supporting steps for a source axiom or an axiom
     * with more than one premise.
     */
    public void renumberSupport(Map<String, TPTPFormula> ftable,
            TPTPFormula ps) {

        if (debug) {
            System.out.println("renumberSupport(): renumbering formula " + ps + " with supports " + ps.supports);
        }
        List<String> newsup = new ArrayList<>();
        for (String sup : ps.supports) {
            newsup.add(renumberOneSupport(ftable, sup));
        }
        if (debug) {
            System.out.println("renumberSupport(): new supports for formula " + ps + " is " + newsup);
        }
        ps.supports = newsup;
    }

    /**
     * ***************************************************************
     * Simplify proof to remove steps with single support other than axioms from the KB
     * (plus optional readability filtering for ans*).
     */
    public List<TPTPFormula> simplifyProof(int level) {

        Map<String, TPTPFormula> ftable = new HashMap<>();
        for (TPTPFormula ps : proof) {
            ftable.put(ps.name, ps);
        }

        // Renumber supports (lift single-premise chains)
        for (TPTPFormula ps : proof) {
            renumberSupport(ftable, ps);
        }

        // 1) First pass: decide which nodes to keep
        List<TPTPFormula> result = new ArrayList<>();
        Set<String> keepNames = new HashSet<>();

        for (TPTPFormula ps : proof) {

            String role = (ps.role == null) ? "" : ps.role.trim();
            boolean isHeader = "negated_conjecture".equals(role) || "conjecture".equals(role);

            String f = (ps.formula == null) ? "" : ps.formula;

            boolean isAns = ANSWER_PRED_PATTERN.matcher(f).find();

            boolean keepByOnePremiseRule =
                    (ps.supports.size() != 1) || TPTPutil.sourceAxiom(ps) || "negated_conjecture".equals(role);

            boolean keep = keepByOnePremiseRule;

            // Drop ans-scaffolding for readability, but keep headers
            if (!isHeader && isAns) keep = false;

            // OPTIONAL: always keep $false as an end-marker for the UI
            boolean isFalse = "$false".equals(f.trim());
            if (isFalse) keep = true;

            if (keep) {
                result.add(ps);
                keepNames.add(ps.name);
            }
        }

        // 2) Second pass: rewire supports to kept nodes (splice out dropped nodes)
        for (TPTPFormula ps : result) {
            if (ps.supports == null || ps.supports.isEmpty()) continue;

            List<String> oldSup = new ArrayList<>(ps.supports);
            LinkedHashSet<String> rewired = new LinkedHashSet<>();

            for (String s : oldSup) {
                if (keepNames.contains(s)) {
                    rewired.add(s);
                } else {
                    // splice: replace missing support with its kept ancestors
                    List<String> expanded = expandToKeptSupports(s, ftable, keepNames, new HashSet<>());
                    rewired.addAll(expanded);
                }
            }
            ps.supports = new ArrayList<>(rewired);

        }

        return result;
    }


    private static final Pattern ANSWER_PRED_PATTERN =
            Pattern.compile("\\b(ans\\d*|answer)\\s*\\(");


    // Helper: expand a missing support into kept ancestors (flatten), with cycle guard.
    private List<String> expandToKeptSupports(String sup,
                                              Map<String,TPTPFormula> ftable,
                                              Set<String> keepNames,
                                              Set<String> seen) {

        if (sup == null) return Collections.emptyList();
        if (keepNames.contains(sup)) return Collections.singletonList(sup);
        if (!seen.add(sup)) return Collections.emptyList(); // cycle guard

        TPTPFormula f = ftable.get(sup);
        if (f == null || f.supports == null || f.supports.isEmpty())
            return Collections.emptyList();

        List<String> out = new ArrayList<>();
        for (String parent : f.supports) {
            out.addAll(expandToKeptSupports(parent, ftable, keepNames, seen));
        }
        return out;
    }


    /**
     * ***************************************************************
     * renumber a proof after simplification
     */
    public List<TPTPFormula> renumberProof(List<TPTPFormula> proof) {

        List<TPTPFormula> result = new ArrayList<>();
        Map<String, String> table = new HashMap<>();

        int num = 1;
        for (TPTPFormula ps : proof) {
            table.put(ps.name, Integer.toString(num++));
        }

        if (debug) System.out.println("renumberProof(): table: " + table);

        for (TPTPFormula ps : proof) {
            String newName = table.get(ps.name);
            ps.name = newName; // should never be null

            List<String> newSupports = new ArrayList<>();
            for (String s : ps.supports) {
                String mapped = table.get(s);
                if (mapped != null) {
                    newSupports.add(mapped);
                } else if (debug) {
                    System.out.println("renumberProof(): WARNING missing support '" + s
                            + "' for step " + newName + " (dropping support)");
                    // Alternative: throw to catch logic errors early:
                    // throw new IllegalStateException("Missing support '" + s + "' for step " + newName);
                }
            }
            ps.supports = newSupports;
            result.add(ps);
        }
        return result;
    }


    public static List<String> reorderVampire4_8 (List<String> cleaned){

        // 1. Locate proof section
        int start = -1, end = -1;
        for (int i = 0; i < cleaned.size(); i++) {
            if (cleaned.get(i).contains("SZS output start")) start = i;
            if (cleaned.get(i).contains("SZS output end"))   { end = i; break; }
        }

        // 2. If there is no proper proof block, do not try to reorder
        if (start < 0 || end < 0 || end <= start) {
            // No proof (e.g. Vampire timed out, SAT, UNKNOWN, truncated output)
            return cleaned;
        }

        // 3. Split into before / proof / after
        List<String> before = (start > 0) ? cleaned.subList(0, start) : Collections.emptyList();
        List<String> proof  = (start >= 0 && end > start) ? new ArrayList<>(cleaned.subList(start+1, end)) : new ArrayList<>();
        List<String> after  = (end >= 0 && end+1 < cleaned.size()) ? cleaned.subList(end+1, cleaned.size()) : Collections.emptyList();

        // 4. Reorder only the proof lines
        List<String> proofReordered = TPTP3ProofProcessor.reorderVampireProofAnyDialect(proof);

        // 5. Put everything back together
        List<String> normalized = new ArrayList<>(before);
        normalized.add(cleaned.get(start));
        normalized.addAll(proofReordered);
        normalized.add(cleaned.get(end));
        normalized.addAll(after);
        return normalized;
    }


    /**
     * Reorder a Vampire 4.8-style reverse proof into 5.0-style forward order.
     * Works for FOF/TFF/THF (and CNF) blocks. Input and output are lists of lines.
     *
     * Expected input: ONLY the proof section lines (between SZS start/end).
     * If you pass the whole file, non-formula lines will be preserved around the reordered blocks.
     */
    public static List<String> reorderVampireProofAnyDialect(List<String> lines) {

        // ---- Dialects we recognise as "formula blocks" ----
        final Set<String> DIALECTS = new LinkedHashSet<>(Arrays.asList("fof(", "tff(", "thf(", "cnf("));

        // id is the 1st argument inside the parentheses, up to the first comma.
        final Pattern HEAD_ID =
                Pattern.compile("^\\s*(fof|tff|thf|cnf)\\(([^,\\s]+)\\s*,", Pattern.CASE_INSENSITIVE);

        // Grab only parents from the [...] that follows inference(...) or introduced(...)
        final Pattern PARENTS =
                Pattern.compile("(?:inference|introduced)\\([^\\]]*\\[(.*?)\\]\\)", Pattern.CASE_INSENSITIVE);

        // Only accept formula ids like f1234
        final Pattern FORMULA_ID = Pattern.compile("\\bf\\d+\\b", Pattern.CASE_INSENSITIVE);

        // Parent ids inside the block: be liberal—Vampire usually uses fNNN.
        // We accept tokens like f123, t456, c789, etc.
        final Pattern ANY_ID_TOKEN = Pattern.compile("\\b[a-z]\\d+\\b", Pattern.CASE_INSENSITIVE);

        // --- Split into three buckets: prefix (non-formula), blocks (formula items), suffix (non-formula) ---
        List<String> prefix = new ArrayList<>();
        List<String> suffix = new ArrayList<>();
        List<ProofItem> items = new ArrayList<>();

        boolean seenBlock = false;
        boolean collecting = false;
        List<String> cur = new ArrayList<>();
        int appearanceIdx = 0; // stable order within the original proof

        for (String ln : lines) {
            String trimmed = ln.trim();
            boolean startsBlock = DIALECTS.stream().anyMatch(d -> trimmed.startsWith(d));
            if (!collecting && !seenBlock && !startsBlock) {
                // Still before first block
                prefix.add(ln);
                continue;
            }
            if (startsBlock && !collecting) {
                collecting = true;
                seenBlock = true;
                cur.clear();
            }
            if (collecting) {
                cur.add(ln);
                if (trimmed.endsWith(").")) {
                    String blockText = String.join("\n", cur);
                    Matcher m = HEAD_ID.matcher(cur.get(0));
                    String id = null;
                    if (m.find()) id = m.group(2); // raw id token (e.g., f5276)

                    if (id == null) {
                        // If we cannot parse, treat as non-formula noise
                        suffix.addAll(cur);
                    } else {
                        // NEW: collect parents only from [ ... ] inside inference(...) / introduced(...)
                        Set<String> parents = new LinkedHashSet<>();

                        Matcher br = PARENTS.matcher(blockText);
                        while (br.find()) {
                            String inside = br.group(1);              // content of [...]
                            Matcher ids = FORMULA_ID.matcher(inside); // f123, f77, etc.
                            while (ids.find()) {
                                String pid = ids.group();
                                if (!pid.equalsIgnoreCase(id)) {
                                    parents.add(pid);
                                }
                            }
                        }
                        boolean containsFalse = blockText.contains("$false");
                        items.add(new ProofItem(id, blockText, parents, appearanceIdx++, containsFalse));
                    }
                    collecting = false;
                    cur.clear();
                }
                continue;
            }
            // After we’ve seen blocks, any stray non-formula lines go to suffix
            suffix.add(ln);
        }

        // If nothing to reorder, return original
        if (items.isEmpty()) return new ArrayList<>(lines);

        // Find the $false node (target). If multiple, pick the last by appearance.
        ProofItem falseItem = null;
        for (ProofItem it : items) if (it.containsFalse) falseItem = it;

        // If no $false present, nothing to derive a frontier from; keep original
        if (falseItem == null) return new ArrayList<>(lines);

        // Build quick lookup by id
        Map<String, ProofItem> byId = new HashMap<>();
        for (ProofItem it : items) byId.put(it.id, it);

        // Compute the subset reachable backwards from $false (typical proof-relevant subgraph)
        Set<String> reachable = new LinkedHashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push(falseItem.id);
        while (!stack.isEmpty()) {
            String id = stack.pop();
            if (!reachable.add(id)) continue;
            for (String p : byId.getOrDefault(id, ProofItem.EMPTY).parents) {
                if (byId.containsKey(p)) stack.push(p);
            }
        }

        // Topological sort over the reachable subgraph
        Map<String, Integer> indeg = new HashMap<>();
        Map<String, Set<String>> children = new HashMap<>();
        for (String id : reachable) {
            indeg.put(id, 0);
            children.put(id, new LinkedHashSet<>());
        }
        for (String id : reachable) {
            ProofItem it = byId.get(id);
            if (it == null) continue;
            for (String p : it.parents) {
                if (!reachable.contains(p)) continue;
                children.get(p).add(id);
                indeg.put(id, indeg.get(id) + 1);
            }
        }

        // Seed with in-degree 0 nodes (premises). Tie-break by numeric part if present, otherwise by appearance order.
        Comparator<String> idCmp = (a, b) -> {
            long na = numericTail(a), nb = numericTail(b);
            if (na != -1 && nb != -1) return Long.compare(na, nb);
            // fallback: appearance index
            return Integer.compare(byId.get(a).appearance, byId.get(b).appearance);
        };
        PriorityQueue<String> q = new PriorityQueue<>(idCmp);
        for (Map.Entry<String,Integer> e : indeg.entrySet()) if (e.getValue() == 0) q.add(e.getKey());

        List<String> topo = new ArrayList<>();
        while (!q.isEmpty()) {
            String u = q.poll();
            topo.add(u);
            for (String v : children.getOrDefault(u, Collections.emptySet())) {
                indeg.put(v, indeg.get(v) - 1);
                if (indeg.get(v) == 0) q.add(v);
            }
        }

        // Cycle or parse oddity: bail out, keep original
        if (topo.size() != reachable.size()) {
            if (debug) System.out.println("RVPA: topo/reachable mismatch, returning original order");
            return new ArrayList<>(lines);
        }
        // Unreachable formula blocks (rare noise): keep them in their original order before the reachable ones
        List<ProofItem> unreachable = new ArrayList<>();
        for (ProofItem it : items) if (!reachable.contains(it.id)) unreachable.add(it);
        unreachable.sort(Comparator.comparingInt(x -> x.appearance));

        // Rebuild
        List<String> out = new ArrayList<>(prefix);
        for (ProofItem it : unreachable) out.add(it.blockText);
        for (String id : topo) out.add(byId.get(id).blockText);
        out.addAll(suffix);
        return joinAndSplitStable(out);
    }

    /** Helper to keep types tidy */
    private static final class ProofItem {
        static final ProofItem EMPTY = new ProofItem("", "", Collections.emptySet(), -1, false);
        final String id;
        final String blockText;
        final Set<String> parents;
        final int appearance;
        final boolean containsFalse;
        ProofItem(String id, String blockText, Set<String> parents, int appearance, boolean containsFalse) {
            this.id = id; this.blockText = blockText; this.parents = parents;
            this.appearance = appearance; this.containsFalse = containsFalse;
        }
    }

    /** Extract numeric tail from ids like f95855/t123/etc., or -1 if absent. */
    private static long numericTail(String id) {
        int i = id.length() - 1;
        while (i >= 0 && Character.isDigit(id.charAt(i))) i--;
        if (i == id.length() - 1) return -1;
        try { return Long.parseLong(id.substring(i + 1)); } catch (Exception e) { return -1; }
    }

    /** Ensure output is a flat list of lines (preserve existing newlines inside blocks). */
    private static List<String> joinAndSplitStable(List<String> chunks) {
        List<String> result = new ArrayList<>();
        for (String ch : chunks) {
            // split on \n but keep empty lines if present
            String[] arr = ch.split("\\R", -1);
            Collections.addAll(result, arr);
        }
        return result;
    }


    /** Format a TPTPFormula as a single fof(...) line. */
    public static String toFOFLine(final TPTPFormula f) {
        final String name = (f.name == null || f.name.isEmpty()) ? "f0" : f.name;
        final String role = (f.role == null || f.role.isEmpty()) ? "plain" : f.role;
        final String body = ensureParenWrapped(collapseWs(f.formula));

        final String source;
        if (TPTPutil.sourceAxiom(f)) {
            // Authored axiom → file('path',unknown)
            String path = "";

            path = (f.infRule == null || f.infRule.isEmpty()) ? "unknown" : f.infRule;

            if ("definition".equals(f.infRule)) {
                source = "introduced(definition,[],[choice_axiom])";
            }else{
                source = path;
            }

        } else {
            // derived step → inference(rule, attrs, [supports])
            final String rule =
                    (f.infRule != null && !f.infRule.isEmpty()) ? f.infRule
                            : ("negated_conjecture".equalsIgnoreCase(role) ? "negated_conjecture" : "resolution");
            final String attrs = "negated_conjecture".equalsIgnoreCase(rule) ? "[status(cth)]" : "[]";
            final String supp =
                    (f.supports != null && !f.supports.isEmpty()) ? "[" + String.join(",", f.supports) + "]" : "[]";
            source = "inference(" + rule + "," + attrs + "," + supp + ")";
        }
        return "fof(" + name + "," + role + ", " + body + ", " + source + ").";
    }



    /* ---------- tiny helpers (no new deps) ---------- */

    private static String collapseWs(String s) {
        return (s == null) ? "" : s.replaceAll("\\s+", " ").trim();
    }

    private static String ensureParenWrapped(String s) {
        if (s.isEmpty()) return "( )";
        return (s.startsWith("(") && s.endsWith(")")) ? s : "( " + s + " )";
    }

    /**
     * ***************************************************************
     * just replace skolem terms with "something" for now
     */
    public String replaceSkolems(String s) {

        return s.replaceAll("sK\\d", "something");
    }

    /**
     * ***************************************************************
     * simplify and paraphrase a proof
     */
    public void simpPara() {

        List<TPTPFormula> result = simplifyProof(2);
        if (debug) {
            System.out.println("TPTP3ProofProcessor.simpPara(): reduced to " + result.size() + " steps ");
        }
        result = renumberProof(result);
        StringBuilder list;
        String listStr, english;
        for (TPTPFormula step : result) {
            if (debug)  System.out.println("TPTP3ProofProcessor.simpPara(): step: " + step);
            if (debug) System.out.println("TPTP3ProofProcessor.simpPara(): step sumo : " + step.sumo);
            if (Pattern.compile("\\(ans\\d").matcher(step.sumo).find()) {
                step.sumo = FormulaUtil.removeAnswerClause(new Formula(step.sumo));
                if (debug) System.out.println("TPTP3ProofProcessor.simpPara(): after remove answer : " + step.sumo);
            }
            System.out.print(step.name + ".  ");
            if (step.supports.size() > 1) {
                list = new StringBuilder();
                System.out.print("Because of ");
                for (String s : step.supports) {
                    list.append(s).append(", ");
                }
                list.delete(list.length() - 2, list.length());
                listStr = list.toString();
                System.out.print(list.substring(0, list.lastIndexOf(",")) + " and"
                        + list.substring(list.lastIndexOf(",") + 1, list.length()) + ", ");
                english = LanguageFormatter.toEnglish(step.sumo);
                english = replaceSkolems(english);
                System.out.println(english);
            } else {
                String s = LanguageFormatter.toEnglish(step.sumo);
                s = replaceSkolems(s);
                if (step.role.equals("conjecture")) {
                    System.out.println("Our conjecture is that " + s);
                } else {
                    System.out.println(Character.toUpperCase(s.charAt(0)) + s.substring(1));
                }
            }
            System.out.println();
        }
    }

    /**
     * ***************************************************************
     */
    private void testPrologParser() {

        //Reader reader = new StringReader("hello(world). some({1,2,3})."); // power(X,Y,Z) :- Z is X ** Y.");
        String input = "cnf(c_0_8, negated_conjecture, ($false), "
                + "inference(cn,[status(thm)],"
                + "[inference(rw,[status(thm)],"
                + "[inference(rw,[status(thm)],[c_0_5, c_0_6]), c_0_7])]),"
                + " ['proof']).";
        idTable.put("c_0_5", 0);
        idTable.put("c_0_6", 1);
        idTable.put("c_0_7", 2);
        Reader reader = new StringReader(input);
        System.out.println(input);
//		PrologParser parser = new GenericPrologParser(reader, new DefaultParserContext(ParserContext.FLAG_CURLY_BRACKETS, Op.SWI));
        DefaultParserContext dpc = new DefaultParserContext(ParserContext.FLAG_CURLY_BRACKETS).addOps(Op.SWI);
        PrologParser parser = new GenericPrologParser(reader, dpc);
        //for (PrologTerm pt : parser) {
        //	printPrologTerm(pt);
        //}
        System.out.println("--------------------------");
        for (PrologTerm pt : parser) {
            System.out.println(getSupports(pt));
        }
    }

    /**
     * ***************************************************************
     */
    public static void showHelp() {

        System.out.println("TPTP3ProofProcessor class");
        System.out.println("  options (with a leading '-'):");
        System.out.println("  f <file> - parse a TPTP3 proof file");
        System.out.println("    e - parse proof as an E proof (forward style)");
        System.out.println("  s <file> - parse, simplify and paraphrase a TPTP3 proof file");
        System.out.println("    e - parse proof as an E proof (forward style)");
        System.out.println("  t - run test");
        System.out.println("  h - show this help");
    }

    /**
     * ***************************************************************
     */
    public static void main(String[] args) throws IOException {

        System.out.println("INFO in TPTP3ProofProcessor.main()");
        if (args == null) {
            System.out.println("no command given");
        } else {
            System.out.println(args.length + " : " + Arrays.toString(args));
        }
        if (args != null && args.length > 0 && args[0].equals("-h")) {
            showHelp();
        } else {
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            // need lexicons to paraphrase proofs!
            //KBmanager.prefOverride.put("loadLexicons","false");
            KBmanager.getMgr().initializeOnce();
            String kbName = KBmanager.getMgr().getPref("sumokbname");
            KB kb = KBmanager.getMgr().getKB(kbName);
            if (args != null && args.length > 1 && args[0].contains("f")) {
                try {
                    if (args[0].contains("e")) {
                        KBmanager.getMgr().prover = KBmanager.Prover.EPROVER;
                    } else {
                        KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
                    }
                    List<String> lines = FileUtil.readLines(args[1], false);
                    String query = "(maximumPayloadCapacity ?X (MeasureFn ?Y ?Z))";
                    StringBuilder answerVars = new StringBuilder("?X ?Y ?Z");
                    System.out.println("input: " + lines + "\n");
                    tpp.parseProofOutput(lines, query, kb, answerVars);
                    tpp.createProofDotGraph();
                    System.out.println("TPTP3ProofProcessor.main(): " + tpp.proof.size() + " steps ");
                    Formula f = new Formula();
                    for (TPTPFormula step : tpp.proof) {
                        System.out.println(":: " + step);
                        f.setFormula(step.sumo);
                        System.out.println(f.format("", "  ", "\n"));
                    }
                    System.out.println("TPTP3ProofProcessor.main() bindings: " + tpp.bindingMap);
                    System.out.println("TPTP3ProofProcessor.main() skolems: " + tpp.skolemTypes);
                    //String link = tpp.createProofDotGraph();
                    //System.out.println("Dot graph at: " + link);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (args != null && args.length > 1 && args[0].contains("s")) {
                try {
                    if (args[0].contains("e")) {
                        KBmanager.getMgr().prover = KBmanager.Prover.EPROVER;
                    } else {
                        KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
                    }
                    String skolems = tpp.findTypesForSkolemTerms(kb);
                    System.out.println("skolems: " + skolems);
                    LanguageFormatter.setKB(KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname")));
                    List<String> lines = FileUtil.readLines(args[1], false);
                    String query = "";
                    StringBuilder answerVars = new StringBuilder("");
                    //System.out.println("input: " + lines + "\n");
                    tpp.parseProofOutput(lines, query, kb, answerVars);
                    //tpp.printProof(1);
                    //tpp.createProofDotGraph();
                    //System.out.println("TPTP3ProofProcessor.main(): " + tpp.proof.size() + " steps ");
                    tpp.simpPara();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (args != null && args.length > 0 && args[0].contains("t")) {
                tpp.testPrologParser();
            } else {
                showHelp();
            }
        }
    }
}
