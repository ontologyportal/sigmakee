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

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.*;
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
    public static boolean tptpProof = false;

    // a map of original ID keys and renumbered key values
    public Map<String, Integer> idTable = new HashMap<>();
    private int idCounter = 0;

    /**
     * ***************************************************************
     */
    public TPTP3ProofProcessor() {
    }

    /**
     * ***************************************************************
     * Convert bindings in list to string
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("Answers:");
        if (bindingMap != null && !bindingMap.keySet().isEmpty()) {
            for (String s : bindingMap.keySet()) {
                sb.append(s).append(" = ").append(bindingMap.get(s)).append(" ");
            }
        } else {
            for (String s : bindings) {
                sb.append(s).append(" ");
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
                sb = new StringBuilder();
            } else if (s.trim().endsWith(").") || s.trim().endsWith("]")) {
                before = false;
                sb.append(s);
                outputs.add(sb.toString());
                sb = new StringBuilder();
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
//        boolean inQuote = false;
        int parenLevel = 0;
        StringBuilder sb = new StringBuilder();
//        char quoteChar;
        int i = 0;
        while (i < line.length()) {
            switch (line.charAt(i)) {
                case '(':
                    if (parenLevel == 0) {
                        result.add(sb.toString());
                        sb = new StringBuilder();
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
                        sb = new StringBuilder();
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
                        sb = new StringBuilder();
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
            int firstParen = supportId.indexOf("(");
            int firstComma = supportId.indexOf(",");
            inferenceType = supportId.substring(firstParen + 1, firstComma);
        } else if (supportId.startsWith("file(")) {
            int firstParen = supportId.indexOf("(");
            int firstComma = supportId.indexOf(",");
            int secondParen = supportId.indexOf(")", firstComma + 1);
            inferenceType = supportId.substring(firstComma + 1, secondParen);
        } else if (supportId.startsWith("introduced(")) {
            int firstParen = supportId.indexOf("(");
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

        if (debug) {
            System.out.println("Info in TPTP3ProofProcessor.parseSupports(): " + supportId);
        }

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
                if (debug) {
                    System.out.println("Info in TPTP3ProofProcessor.parseSupports(): support element: " + supportSet1);
                }
                if (!supportSet1.contains("(")) {
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

        if (debug) {
            System.out.println("-----------------------");
        }
        if (debug) {
            System.out.println("parseProofStep() line: " + line);
        }
        if (StringUtil.emptyString(line)) {
            return null;
        }
        line = line.replaceAll(System.lineSeparator(), "");
        if (line.startsWith("%")) {
            if (debug) {
                System.out.println("TPTP3ProofProcessor.parseProofStep() skipping comment: " + line);
            }
            return null;
        }
        ProofStep ps = new ProofStep();
        line = line.replaceAll("\\$answer\\(", "answer(");
        if (debug) {
            System.out.println("TPTP3ProofProcessor.parseProofStep(): after remove $answer: " + line);
        }
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
                        firstParen = tptpF.supports.get(0).indexOf("(");
                        firstComma = tptpF.supports.get(0).indexOf(",");
                        secondParen = tptpF.supports.get(0).indexOf(")", firstComma + 1);
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
            System.out.println("Error in TPTP3ProofProcessor.processAnswers() bad format: " + line);
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
        bindingMap = new HashMap<>();
        if (qlist != null && qlist.length() > 0) {
            List<String> qvarslist = Arrays.asList(qlist.toString().split(","));
            for (String s : qvarslist) {
                String news = s.replace("V__", "?");
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
                    && ps.sumo.contains("ans0") && !ps.sumo.contains("?")) {
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
                System.out.println("Error in processAnswersFromProof(): null answers");
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
     * Return the most specific type for skolem variable.
     *
     * @param kb The knowledge base used to find skolem term's types
     *
     * For example, original binding = esk3_0 set binding = "An instance of
     * Human" (Human is the most specific type for esk3_0 in the given proof)
     *
     * original binding = esk3_1 set binding = "An instance of Human, Agent" (If
     * multiple types are found for esk3_1)
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
                if (kb.kbCache.checkDisjoint(kb, types) == true) {
                    // check if there are contradiction among the types returned
                    //bindings.remove(binding);
                    result = "Type contradiction for " + binding + " in " + types;
                    //bindings.add(binding);
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
            int leftParen = line.indexOf("(");
            int rightParen = line.indexOf(")");
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
        String tsp = Formula.termSymbolPrefix;
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
     * ***************************************************************
     * Compute binding and proof from the theorem prover's response. Leave out
     * the statements of relation sorts that is part of the TFF proof output.
     *
     * @param qlist is the list of quantified variables in order of the original
     * query, which is the order Vampire and Eprover will follow when reporting
     * answers
     */
    public void parseProofOutput(List<String> lines, String kifQuery, KB kb, StringBuilder qlist) {

        if (debug) {
            System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): before reverse: "
                    + lines);
        }
        if (debug) {
            System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): # lines: "
                    + lines.size());
        }
        if (KBmanager.getMgr().prover == KBmanager.Prover.VAMPIRE) {
            lines = joinNreverseInputLines(lines);
        }
        if (debug) {
            System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): after reverse: "
                    + lines);
        }
        try {
            boolean inProof = false, finishAnswersTuple = false;
            int end;
            TPTPVisitor sv;
            TPTPFormula step;
            for (String line : lines) {
                if (debug) {
                    System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): looking at line: " + line);
                }
                if (debug) {
                    System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): in proof: " + inProof);
                }
                if (debug) {
                    System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): finishAnswersTuple: " + finishAnswersTuple);
                }
                if (line.contains("SZS output start")) {
                    if (debug) {
                        System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): found proof, setting inconsistency to true");
                    }
                    noConjecture = true; // if conjecture or negated_conjecture found in the proof then it's not inconsistent
                    inProof = true;
                    continue;
                }
                if (line.contains("SZS status")) {
                    status = line.substring(13, line.indexOf(" ", 14));
                    if (debug) {
                        System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): tpp.status: " + status);
                    }
                }
                if (line.contains("SZS answers")) {
                    if (debug) {
                        System.out.println("TPTP3ProofProcessor.parseProofOutput(ar): found answer line: " + line);
                    }
                    if (!finishAnswersTuple) {
                        end = line.lastIndexOf("]");
                        if (end == -1 || (line.length() < end + 1)) {
                            end = line.length();
                        }
                        String bracketedAnswers = line.substring(20, end + 1);
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
                        if (debug) {
                            System.out.println("TPTP3ProofProcessor.parseProofOutput(ar,2): line: " + line);
                        }
                        sv.parseString(line);
                        if (debug) {
                            System.out.println("TPTP3ProofProcessor.parseProofOutput(ar,2): result: " + sv.result);
                        }
                        if (sv.result != null) {
                            if (sv.result.values().size() > 1) {
                                System.out.println("Error in TPTP3ProofProcessor.parseProofOutput(ar,2): more than one line in " + line);
                            }
                            step = sv.result.values().iterator().next();
                            if (step.role.equals("negated_conjecture") || step.role.equals("conjecture")) {
                                noConjecture = false;
                            }
                            if (debug) {
                                System.out.println("TPTP3ProofProcessor.parseProofOutput(ar,2): step.sumo: " + step.sumo);
                            }
                            if (debug) {
                                System.out.println("TPTP3ProofProcessor.parseProofOutput(ar,2): step type: " + step.role);
                            }
                            if (!step.role.equals("type")) {
                                proof.add(step);
                            }
                            if (step.sumo.equals("false")) {
                                containsFalse = true;
                            }
                            if (debug) {
                                System.out.println("TPTP3ProofProcessor.parseProofOutput(ar,2): adding line: "
                                        + line + "\nas " + sv.result);
                            }
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
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
        if (debug) {
            System.out.println("TPTP3ProofProcess.parseProofOutput(ar,2): before pruning: " + this);
        }
        if (debug) {
            System.out.println("TPTP3ProofProcess.parseProofOutput(ar,2): here: ");
        }
        if (debug) {
            System.out.println("TPTP3ProofProcess.parseProofOutput(ar,2): bindings: " + bindings);
        }
        if (debug) {
            System.out.println("TPTP3ProofProcess.parseProofOutput(ar,2): query: " + kifQuery);
        }
        processAnswersFromProof(qlist, kifQuery);
        findTypesForSkolemTerms(kb);
        proof = TPTPFormula.normalizeProofStepNumbers(proof);
        if (debug) {
            System.out.println("TPTP3ProofProcess.parseProofOutput(ar,2): result: " + this);
        }
        if (debug) {
            System.out.println("TPTP3ProofProcessor.parseProofOutput(lnr): idTable: " + idTable);
        }
        if (debug) {
            System.out.println("TPTP3ProofProcessor.parseProofOutput(lnr): proof ids: ");
        }
        if (debug) {
            for (TPTPFormula ps : proof) {
                System.out.println(ps.name);
            }
        }
        if (debug) {
            System.out.println("TPTP3ProofProcess.parseProofOutput(): returning bindings: " + bindings);
        }
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
            System.out.println("Error in parseProofFromFile() no such file " + filename);
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
            if (tptpProof) {
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
                formatted = formatted.replaceAll("<=>", "&lt;=&gt;");
                formatted = formatted.replaceAll("=>", "=&gt;");
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
                formatted = formatted.replaceAll("<=>", "&lt;=&gt;");
                formatted = formatted.replaceAll("=>", "=&gt;");
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

    /**
     * *************************************************************
     */
    private void createProofDotGraphImage(String filename) throws IOException {

        try {
            String graphVizDir = KBmanager.getMgr().getPref("graphVizDir");
            String command = graphVizDir + File.separator + "dot " + filename + ".dot -Tgif";
            Process proc = Runtime.getRuntime().exec(command);
            System.out.println("Graph.createDotGraph(): exec command: " + command);
            File file = new File(filename + ".gif");
            try (InputStream img = new BufferedInputStream(proc.getInputStream())) {
                RenderedImage image = ImageIO.read(img);
                if (image != null) {
                    ImageIO.write(image, "gif", file);
                }
            }
            System.out.println("Graph.createDotGraph(): write image file: " + file);
        } catch (IOException e) {
            String err = "Error writing file " + filename + ".dot\n" + e.getMessage();
            throw new IOException(err);
        }
    }

    /**
     * *************************************************************
     * Create a proof in a format suitable for GraphViz' input format
     * http://www.graphviz.org/ Generate a GIF from the .dot output with a
     * command like dot SUMO-graph.dot -Tgif > graph.gif
     */
    public String createProofDotGraph() throws IOException {

        String sep = File.separator;
        String link = "graph" + sep + "proof.gif";
        String dir = System.getenv("CATALINA_HOME") + sep + "webapps"
                + sep + "sigma" + sep + "graph";
        String filename = dir + sep + "proof";

        try (Writer fw = new FileWriter(filename + ".dot"); PrintWriter pw = new PrintWriter(fw)) {
            File dirfile = new File(dir);
            if (!dirfile.exists()) {
                dirfile.mkdir();
            }
            System.out.println("Graph.createGraphBody(): creating file at " + filename + ".dot");

            Set<String> result = new HashSet<>();
            result.addAll(createProofDotGraphBody());
            pw.println("digraph G {");
            pw.println("  rankdir=LR");
            for (String s : result) {
                pw.println(s);
            }
            pw.println("}");
            createProofDotGraphImage(filename);
        } catch (IOException e) {
            String err = "Error writing file " + filename + ".dot\n" + e.getMessage();
            throw new IOException(err);
        }
        return link;
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
    private String renumberOneSupport(Map<String, TPTPFormula> ftable,
            String sup) {

        if (debug) {
            System.out.println("renumberOneSupport(): get formula for: " + sup);
        }
        TPTPFormula f = ftable.get(sup);
        if (debug) {
            System.out.println("renumberOneSupport(): formula: " + f);
        }
        if (f.supports.isEmpty()) {
            return sup;
        }
        String newsup = f.supports.get(0);
        while ((f.supports.size() < 2) && !TPTPutil.sourceAxiom(f) && !f.supports.isEmpty()) {
            newsup = f.supports.get(0);
            if (debug) {
                System.out.println("renumberOneSupport(): first support: " + newsup);
            }
            f = ftable.get(newsup);
            if (f == null) {
                System.out.println("renumberOneSupport(): Error no formula: " + newsup);
            }
        }
        if (debug) {
            System.out.println("renumberOneSupport(): returning: " + newsup);
        }
        return newsup;
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
     * Simplify proof to remove steps with single support other than axioms from
     * the KB
     */
    public List<TPTPFormula> simplifyProof(int level) {

        Map<String, TPTPFormula> ftable = new HashMap<>();
        for (TPTPFormula ps : proof) {
            ftable.put(ps.name, ps);
        }
        for (TPTPFormula ps : proof) {
            renumberSupport(ftable, ps);
        }
        List<TPTPFormula> result = new ArrayList<>();
        for (TPTPFormula ps : proof) {
            if ((ps.supports.size() != 1) || TPTPutil.sourceAxiom(ps)) {
                result.add(ps);
            }
        }
        return result;
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
        if (debug) {
            System.out.println("renumberProof(): table: " + table);
        }
        List<String> newSupports;
        for (TPTPFormula ps : proof) {
            ps.name = table.get(ps.name);
            newSupports = new ArrayList<>();
            for (String s : ps.supports) {
                newSupports.add(table.get(s));
            }
            ps.supports = newSupports;
            result.add(ps);
        }
        return result;
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
            if (debug) {
                System.out.println("TPTP3ProofProcessor.simpPara(): step: " + step);
            }
            if (debug) {
                System.out.println("TPTP3ProofProcessor.simpPara(): step sumo : " + step.sumo);
            }
            if (Pattern.compile("\\(ans\\d").matcher(step.sumo).find()) {
                step.sumo = FormulaUtil.removeAnswerClause(new Formula(step.sumo));
                if (debug) {
                    System.out.println("TPTP3ProofProcessor.simpPara(): after remove answer : " + step.sumo);
                }
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
        //System.out.println("TPTP3ProofProcessor.main() bindings: " + tpp.bindingMap);
        //System.out.println("TPTP3ProofProcessor.main() skolems: " + tpp.skolemTypes);
        //String link = tpp.createProofDotGraph();
        //System.out.println("Dot graph at: " + link);
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
                    Formula f;
                    for (TPTPFormula step : tpp.proof) {
                        System.out.println(":: " + step);
                        f = new Formula(step.sumo);
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
