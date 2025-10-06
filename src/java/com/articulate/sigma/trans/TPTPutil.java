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

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.tp.Vampire;
import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.utils.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tptp_parser.TPTPFormula;

public class TPTPutil {

    /** ***************************************************************
     *  Remove enclosing meta-information from a TPTP axiom.
     */
    private static String extractTPTPaxiom(String t) {

        return t.substring(1,t.length()-1).trim();
    }

    /** ***************************************************************
     *  Remove enclosing meta-information from a TPTP axiom.
     */
    private static String removeTPTPSuffix(String t) {

        if (t.endsWith(Formula.TERM_MENTION_SUFFIX) || t.endsWith(Formula.TERM_MENTION_SUFFIX))
            return t.substring(0,t.length()-Formula.TERM_MENTION_SUFFIX.length());
        else
            return t;
    }

    /** ***************************************************************
     *  Remove enclosing meta-information from a TPTP axiom.
     */
    private static String returnAndIndent(int level) {

    	StringBuilder result = new StringBuilder();
        result.append("<br>\n");
        for (int i = 0; i < level; i++)
            result.append("&nbsp;&nbsp;");
        return result.toString();
    }

    /** ***************************************************************
     * Format a formula for either text or HTML presentation by inserting
     * the proper hyperlink code, characters for indentation and end of line.
     * A standard LISP-style pretty printing is employed where an open
     * parenthesis triggers a new line and added indentation.
     *
     * @param hyperlink - the URL to be referenced to a hyperlinked term.
     */
    public static String htmlTPTPFormat(Formula f, String hyperlink, boolean traditionalLogic) {

//        String indentChars = "  ";
//        String eolChars = "\n";

        //System.out.println("INFO in Formula.htmlTPTPFormat(): " + f.toString());
        //System.out.println("INFO in Formula.htmlTPTPFormat(): theTptpFormulas.size()" + f.theTptpFormulas.size());
        if (f.theTptpFormulas == null || f.theTptpFormulas.size() < 1){
            String tff = SUMOtoTFAform.process(f, false);
            if (tff != null)
                return htmlizeSUMOTFA(tff, hyperlink);
            return "No TPTP formula.  May not be expressible in strict first order.";
        }
        StringBuilder result = new StringBuilder();
        boolean inComment, inToken, inQuantifier;
        StringBuilder token;
        int level, tokenNum, flen;
        char ch;
        String tokenNoSuffix;
        for (String formString : f.theTptpFormulas) {
            if (!StringUtil.emptyString(formString)) {
                //System.out.println("INFO in Formula.htmlTPTPFormat(): TPTP formula: " + formString);
                formString = formString.trim();
            }
            else {
                System.err.println("Error in Formula.htmlTPTPFormat(): empty TPTP formula: " + formString);
                continue;
            }
            formString = extractTPTPaxiom(formString);
            inComment = false;
            inToken = false;
            token = new StringBuilder();
            level = 0;
            tokenNum = 0;
            inQuantifier = false;

            flen = formString.length();
            for (int i = 0; i < flen; i++) {
                // System.out.println("INFO in format(): " + formatted.toString());
                ch = formString.charAt(i);
                if (inComment) {     // In a comment
                    result.append(ch);
                    if (ch == '\'')
                        inComment = false;
                }
                else {
                    if (inToken) {
                        if (!Character.isJavaIdentifierPart(ch)) {
                            inToken = false;
                            tokenNoSuffix = removeTPTPSuffix(token.toString());
                            result.append("<a href=\"").append(hyperlink).append("&term=").append(tokenNoSuffix).append("\">s__").append(token.toString()).append("</a>");
                            token = new StringBuilder();
                            result.append(ch);
                            tokenNum++;
                        }
                        else
                            token.append(ch);
                    }
                    else if (ch == '\'') {
                        inComment = true;
                        result.append(ch);
                    }
                    else if (ch == '(') {
                        level++;
                        result.append(ch);
                    }
                    else if (ch == ':') {
                    	if (!traditionalLogic)
                            result.append(ch);
                        result.append(returnAndIndent(level));
                    }
                    else if (ch == '!') {
                    	if (!traditionalLogic)
                            result.append(ch);
                    	else
                            result.append("&forall;");
                    }
                    else if (ch == '?') {
                    	if (!traditionalLogic)
                            result.append(ch);
                       	else
                            result.append("&exist;");
                    }
                    else if (ch == '&') {
                    	if (traditionalLogic)
                            result.append("&and;");
                    	else
                            result.append(ch);
                        result.append(returnAndIndent(level));
                    }
                    else if (ch == '|') {
                    	if (traditionalLogic)
                            result.append("&or;");
                    	else
                            result.append(ch);
                        result.append(returnAndIndent(level));
                    }
                    else if (formString.substring(i).startsWith("<~>")) {
                        i = i + 2;
                    	if (traditionalLogic)
                            result.append("&xor;");
                    	else
                            result.append(ch);
                        result.append(returnAndIndent(level));
                    }
                    else if (ch == '~') {
                    	if (traditionalLogic)
                            result.append("&not;");
                    	else
                            result.append(ch);
                    }
                    else if (ch == ')') {
                        level--;
                        tokenNum = 0;
                        result.append(ch);
                        if ((i+1 < formString.length()) && formString.charAt(i+1) != ')')
                        	result.append(returnAndIndent(level));
                    }
                    else if (formString.substring(i).startsWith(Formula.IF)) {
                        i++;
                        if (traditionalLogic)
                            result.append("&rArr;");
                        else
                            result.append("=&gt;");
                        result.append(returnAndIndent(level));
                    }
                    else {
                        if (formString.substring(i).startsWith(Formula.TERM_SYMBOL_PREFIX)) {
                            inToken = true;
                            i = i + 2;
                        }
                        else
                            result.append(ch);
                    }
                }
            }
            result.append("<P>\n");
        }
        return result.toString();
    }

        /** ***************************************************************
     * Take a plain SUMOtoTFAform string and wrap every s__ token
     * in a hyperlink, similar to htmlTPTPFormat().
     *
     * @param tfa        the raw SUMOtoTFAform string
     * @param hyperlink  the base URL (e.g. "http://sigma.ontologyportal.org:4040/sigma?kb=SUMO&term=")
     * @return HTML-formatted string with linked terms
     */
    public static String htmlizeSUMOTFA(String tfa, String hyperlink) {
        if (tfa == null || tfa.isEmpty())
            return "";

        StringBuilder result = new StringBuilder();
        StringBuilder token = new StringBuilder();
        boolean inToken = false;

        for (int i = 0; i < tfa.length(); i++) {
            char ch = tfa.charAt(i);

            if (inToken) {
                if (!Character.isJavaIdentifierPart(ch) && ch != '_') {
                    // end of token
                    String tok = token.toString();
                    result.append("<a href=\"")
                          .append(hyperlink)
                          .append(tok)
                          .append("\">")
                          .append(tok)
                          .append("</a>");
                    token.setLength(0);
                    inToken = false;
                    result.append(ch);
                } else {
                    token.append(ch);
                }
            } else {
                // detect start of s__ token
                if (ch == 's' && i + 2 < tfa.length() && tfa.charAt(i+1) == '_' && tfa.charAt(i+2) == '_') {
                    inToken = true;
                    token.append("s__");
                    i += 2; // skip ahead
                } else {
                    result.append(ch);
                }
            }
        }

        // flush last token if it ended the string
        if (inToken && token.length() > 0) {
            String tok = token.toString();
            result.append("<a href=\"")
                  .append(hyperlink)
                  .append(tok)
                  .append("\">")
                  .append(tok)
                  .append("</a>");
        }

        return result.toString();
    }


    /** ***************************************************************
     * Is the axiom in a proof a source authored axiom from SUMO,
     * rather than one automatically derived or introduced by a
     * theorem prover
     */
    public static boolean sourceAxiom(TPTPFormula ps) {

        //System.out.println("sourceAxiom() supports: " + ps.supports.size());
        //System.out.println("sourceAxiom() ps.infRule: " + ps.infRule);
        return ps.supports.isEmpty() && !ps.infRule.startsWith("introduced");
    }

    /** ***************************************************************
     * Is there a citation as a containsFormula relation for this
     * axiom?
     */
    public static boolean citation(String sumoStep, String stepName, KB kb) {

        //System.out.println("\nTPTPutil.citation: sumoStep: " + sumoStep);
        //System.out.println("TPTPutil.citation: stepName: " + stepName);
        List<Formula> ciAxioms = kb.ask("arg",0,"containsFormula");
        //System.out.println("TPTPutil.citation: formulas: " + ciAxioms);
        Formula arg;
        for (Formula f : ciAxioms) {
            arg = f.getArgument(2);
            //System.out.println("TPTPutil.citation: formula arg: " + arg);
            if (arg != null && arg.listP()) {
                if (arg.equals(new Formula(sumoStep))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** ***************************************************************
     * Is there a citation as a containsFormula relation for this
     * axiom?
     */
    public static String getCitationString(String sumoStep, String stepName, KB kb) {

        List<Formula> ciAxioms = kb.ask("arg",0,"containsFormula");
        //System.out.println("TPTPutil.getCitationString: stepName: " + stepName);
        //System.out.println("TPTPutil.getCitationString: sumo: " + sumoStep);
        Formula arg;
        String term;
        List<Formula> comments;
        for (Formula f : ciAxioms) {
            arg = f.getArgument(2);
            if (arg != null && arg.listP()) {
                if (arg.equals(new Formula(sumoStep))) {
                    //System.out.println("TPTPutil.getCitationString: formula arg: " + arg);
                    term = f.getStringArgument(1);
                    comments = kb.askWithRestriction(0,"comment",1,term);
                    if (comments != null && !comments.isEmpty())
                        return comments.get(0).getStringArgument(2);
                }
            }
        }
        return "";
    }

    /** ***************************************************************
     */
    public static void test() {

        Formula f = new Formula();
        //f.theTptpFormulas.add("fof(kb_ArabicCulture_20,axiom,(( s__subclass(s__Hajj,s__Translocation) ))).");
        f.theTptpFormulas.add("(! [V__P] : (s__instance(V__P,s__Agent) => ((s__attribute(V__P,s__Muslim) & s__capability(s__Hajj,s__agent__m,V__P)) => " +
                "s__modalAttribute('(? [V__H] : (s__instance(V__H,s__Process) & s__instance(V__H,s__Hajj) & s__agent(V__H,V__P)))',s__Obligation))))");
        System.out.println(TPTPutil.htmlTPTPFormat(f,"http://sigma.ontologyportal.org:4040/sigma?kb=SUMO&term=",false));
    }

    /**
     * Cleans a Vampire proof log file so only the valid proof lines remain.
     * Keeps only lines between "% SZS output start" and "% SZS output end",
     * merges multi-line fof(...) statements into single lines,
     * and returns the processed list ready for TPTP3ProofProcessor.
     */
    private static List<String> clearProofFile(List<String> lines) {
        // --- 0) locate markers and status ---
        String statusLine = null;
        int startIdx = -1;
        int endIdx = -1;

        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            if (statusLine == null && l.contains("% SZS status")) statusLine = l;
            if (startIdx == -1 && l.contains("% SZS output start")) startIdx = i;
            if (startIdx != -1 && l.contains("% SZS output end")) { endIdx = i; break; }
        }

        // Fallback: if we didn't find a proper proof block, return empty (caller can handle)
        if (startIdx == -1 || endIdx == -1 || endIdx < startIdx) {
            System.err.println("[clearProofFile] No SZS proof block found.");
            return new ArrayList<String>();
        }

        // --- 1) extract the proof section, inclusive of start/end ---
        List<String> section = new ArrayList<String>(lines.subList(startIdx, endIdx + 1));

        // --- 2) merge multi-line fof(...) into single lines ---
        List<String> mergedFofs = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean insideFof = false;

        for (int i = 0; i < section.size(); i++) {
            String trimmed = section.get(i).trim();

            // skip markers here; we'll add them explicitly later
            if (trimmed.contains("% SZS output start") || trimmed.contains("% SZS output end")) {
                continue;
            }

            if (trimmed.startsWith("fof(")) {
                insideFof = true;
                current.setLength(0);
                current.append(trimmed);
            } else if (insideFof) {
                current.append(" ").append(trimmed);
            }

            if (insideFof && trimmed.endsWith(").")) {
                mergedFofs.add(current.toString());
                insideFof = false;
            }
        }

        // --- 3) build the final list: status + start + merged fof + end ---
        List<String> finalLines = new ArrayList<String>();
        if (statusLine != null) finalLines.add(statusLine);
        finalLines.add(lines.get(startIdx));   // % SZS output start ...
        finalLines.addAll(mergedFofs);
        finalLines.add(lines.get(endIdx));     // % SZS output end ...

        return finalLines;
    }

    // Save authored axioms + conjecture as clean TPTP, one fof(...) per line.
    private static void writeMinTPTP(List<TPTPFormula> proof) {

        String outName = "min-problem.tptp";

        String outPath = java.nio.file.Paths.get(System.getProperty("user.dir"), outName)
                .toAbsolutePath().toString();

        List<String> out = new ArrayList<String>();
        out.add("% Generated by TPTPutil: authored axioms + conjecture");
        String conjecture = null;

        for (TPTPFormula step : proof) {
            if (TPTPutil.sourceAxiom(step)) {          // only authored axioms
                out.add(oneLine(step.toString()));     // TPTP 'fof(...)' line
            } else if ("conjecture".equals(step.role)) {
                conjecture = oneLine(step.toString());
            }
        }
        if (conjecture == null) System.out.println("% No Conjecture found!");

        FileUtil.writeLines(outPath, out); // overwrite = false append
    }

    private static String oneLine(String s) {
        String t = s.replace('\r', ' ').replace('\n', ' ').trim().replaceAll("\\s+", " ");
        return t.endsWith(".") ? t : (t + ".");
    }


    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("TPTPutil class");
        System.out.println("  options (with a leading '-'):");
        System.out.println("  f <file> - parse a TPTP3 proof file and output source axioms in SUO-KIF");
        System.out.println("  i <query> <file> - get a proof and output to file");
        System.out.println("  l <file> - parse a TPTP3 proof and substitute in legal argument text");
        System.out.println("  t - run test");
        System.out.println("  h - show this help");
    }

    /** ***************************************************************
     */
    public static void main(String[] args) throws IOException {

        System.out.println("INFO in TPTPutil.main()");
        if (args == null)
            System.out.println("no command given");
        else
            System.out.println(args.length + " : " + Arrays.toString(args));
        if (args != null && args.length > 0 && args[0].equals("-h"))
            showHelp();
        else {
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            // need lexicons to paraphrase proofs!
            //KBmanager.prefOverride.put("loadLexicons","false");
            KBmanager.getMgr().initializeOnce();
            String kbName = KBmanager.getMgr().getPref("sumokbname");
            KB kb = KBmanager.getMgr().getKB(kbName);
            if (args != null && args.length > 1 && args[0].contains("f")) {
                try {
                    List<String> lines = FileUtil.readLines(args[1],false);

                    System.out.println("---- DEBUG: File lines read ----");
                    System.out.println("Total lines: " + lines.size());
                    for (int i = 0; i < lines.size(); i++) {
                        System.out.println("Line " + i + ": " + lines.get(i));
                    }
                    System.out.println("---- END DEBUG ----");

                    // Clear file before processing from TPTP3ProofProcessor.parseProofOutput
                    lines = clearProofFile(lines);

                    String query = Formula.LP;
                    StringBuilder answerVars = new StringBuilder("");
//                    System.out.println("input: \n" + lines + "\n");
                    tpp.parseProofOutput(lines, query, kb,answerVars);
                    System.out.println("TPTPutil.main(): " + tpp.proof.size() + " steps ");
                    System.out.println("TPTPutil.main(): showing only source axioms ");
                    Formula f;
                    for (TPTPFormula step : tpp.proof) { // all steps not only authored & derived
                        if (TPTPutil.sourceAxiom(step)) { // filters only the authored axioms
                            f = new Formula(step.sumo);
                            System.out.println(f.format("","  ","\n"));
                        }
                    }

                    // Creates a new TPTP file in the curent directory that contains only the
                    // authored axioms.
                    writeMinTPTP(tpp.proof);

                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if (args != null && args.length > 2 && args[0].contains("i")) {
                int timeout = 30;
                if (args[0].contains("p"))
                    TPTP3ProofProcessor.tptpProof = true;
                Vampire vamp = kb.askVampire(args[1], timeout, 1);
                System.out.println("KB.main(): completed Vampire query with result: " + StringUtil.arrayListToCRLFString(vamp.output));
                tpp = new TPTP3ProofProcessor();
                tpp.parseProofOutput(vamp.output, args[1], kb, vamp.qlist);
                ArrayList<String> out = new ArrayList<>();
                for (TPTPFormula ps : tpp.proof)
                    out.add(ps.toString());
                FileUtil.writeLines(args[2],out);
            }
            else if (args != null && args.length > 1 && args[0].contains("l")) {
                try {
                    KB.force = true;
                    kb.loadVampire();
                    Vampire vamp = kb.askVampire(args[1], 30, 1);
                    //System.out.println("KB.main(): completed Vampire query with result: " + StringUtil.arrayListToCRLFString(vamp.output));
                    tpp = new TPTP3ProofProcessor();
                    tpp.parseProofOutput(vamp.output, args[1], kb, vamp.qlist);
                    //System.out.println("TPTPutil.main(): " + tpp.proof.size() + " steps ");
                    //System.out.println("TPTPutil.main(): showing only source axioms ");
                    //System.out.println("TPTPutil.main(): axiomKey: " + KB.axiomKey);
                    String id, str, name;
                    int firstParen, firstComma, secondParen;
                    Formula f;
                    for (TPTPFormula step : tpp.proof) {
                        //System.out.println("TPTPutil.main(): step: " + step);
                        if (TPTPutil.sourceAxiom(step)) {
                            f = new Formula(step.sumo);
                            name = step.infRule;
                            //System.out.println("TPTPutil.main(): name: " + name);
                            if (name.startsWith("file(")) {
                                firstParen = name.indexOf(Formula.LP);
                                firstComma = name.indexOf(",");
                                secondParen = name.indexOf(Formula.RP, firstComma + 1);
                                id = name.substring(firstComma + 1, secondParen);
                                //System.out.println("TPTPutil.main(): id: " + id);
                                if (KB.axiomKey.keySet().contains(id)) {
                                    //System.out.println("TPTPutil.main(): formula: " + KB.axiomKey.get(id));
                                    str = getCitationString(KB.axiomKey.get(id).toString(), name, kb);
                                    if (!StringUtil.emptyString(str))
                                        System.out.println("\n" + str);
                                }
                            }
                            System.out.println("\n" + f.format("","  ","\n"));
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if (args != null && args.length > 0 && args[0].contains("t"))
                test();
            else
                showHelp();
        }
    }
}
