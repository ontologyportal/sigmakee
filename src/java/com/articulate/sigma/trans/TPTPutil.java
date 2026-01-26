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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        //System.out.println("INFO in Formula.htmlTPTPFormat(): getTheTptpFormulas().size()" + f.getTheTptpFormulas().size());
        Set<String> tptpFormulas = f.getTheTptpFormulas();
        if (tptpFormulas == null || tptpFormulas.size() < 1){
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
        for (String formString : tptpFormulas) {
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
     * Cleans and normalizes the contents of a TPTP proof file.
     *
     * Steps performed:
     * 1. Keeps comment lines ("%") that appear before the first fof(...) line and after the last one.
     * 2. Merges multi-line fof(...) formulas into single lines by concatenating
     *    their continuation lines until a closing ")." is found.
     * 3. Returns a cleaned version of the proof containing:
     *      - Header comments
     *      - All compacted fof(...) lines
     *      - Footer comments
     *
     */
    public static List<String> clearProofFile(List<String> lines) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = null;

        // start of a TPTP formula line (FOF/TFF/THF)
        Pattern start = Pattern.compile("^(fof|tff|thf)\\(");

        for (String raw : lines) {
            String s = raw == null ? "" : raw.trim();
            if (s.isEmpty()) continue;

            // If we’re currently merging a multi-line TPTP item
            if (cur != null) {
                cur.append(' ').append(s);
                if (s.endsWith(").")) {             // end of the item
                    out.add(cur.toString());
                    cur = null;
                }
                continue;
            }

            // New TPTP item starts
            if (start.matcher(s).find()) {
                cur = new StringBuilder(s);
                if (s.endsWith(").")) {             // single-line case
                    out.add(cur.toString());
                    cur = null;
                }
                continue;
            }

            // Keep useful non-formula lines (comments and SZS markers/answers)
            if (s.startsWith("%") || s.startsWith("SZS ")) {
                out.add(raw);
            }
        }

        // Flush unterminated item just in case
        if (cur != null) out.add(cur.toString());
        return out;
    }

    /**
     * Processes a TPTP proof file and removes all proof steps
     * that have only a single premise (e.g., trivial inferences or direct copies).
     *
     * Steps performed:
     * 1. Initializes the Sigma knowledge base (KBmanager) to ensure ontology data is loaded.
     * 2. Cleans the raw proof lines using clearProofFile(), preserving only header/footer comments
     *    and merging multi-line fof(...) entries into single lines.
     * 3. Parses the proof output into structured TPTPFormula objects.
     * 4. Calls simplifyProof(1) to remove all single-premise proof steps.
     * 5. Renumbers the remaining proof steps to maintain consistent references.
     * 6. Reconstructs the proof by:
     *      - Keeping the original header and footer comments.
     *      - Converting each remaining TPTPFormula back into fof(...) format.
     *      - Combining them into a normalized list of proof lines.
     *
     * Returns a cleaned proof where all trivial one-premise derivations are dropped,
     * maintaining logical consistency and readability.
     */
    public static List<String> dropOnePremiseFormulasFOF(List<String> proofLines) {
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        KBmanager.getMgr().initializeOnce();
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        KB kb = KBmanager.getMgr().getKB(kbName);

        List<String> cleaned_proofLines = clearProofFile(proofLines);

        tpp.parseProofOutput(cleaned_proofLines, "", kb, new StringBuilder());  // builds tpp.proof (List<TPTPFormula>)
        List<TPTPFormula> keep = tpp.simplifyProof(1); // drop 1-premise,
        List<TPTPFormula> renumberProof = tpp.renumberProof(keep); // rewire+renumber

        List<String> firstComments = new ArrayList<>();
        List<String> lastComments = new ArrayList<>();
        boolean beforeFOF = true;
        boolean afterFOF = false;
        // Keep the comments before and after the fof lines
        for (String line : cleaned_proofLines) {
            String trim = line.trim();
            if (beforeFOF && trim.startsWith("%")) {
                firstComments.add(line);
            } else if (trim.startsWith("fof(")) {
                beforeFOF = false;
            } else if (!beforeFOF && trim.startsWith("%")) {
                afterFOF = true;
                lastComments.add(line);
            } else if (afterFOF && trim.startsWith("%")) {
                lastComments.add(line);
            }
        }

        // Build the fof lines from the renumbered proof
        List<String> fofBody = new ArrayList<>();
        for (TPTPFormula step : renumberProof) fofBody.add(TPTP3ProofProcessor.toFOFLine(step));

        // --- build final output ---
        List<String> finalLines = new ArrayList<>();
        finalLines.addAll(firstComments);
        finalLines.addAll(fofBody);
        finalLines.addAll(lastComments);

        return finalLines;
    }


    static String norm(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    private static String stripVarNumbers(String s) {
        if (s == null) return "";
        // Removes digits after ?X or X (TPTP variable names)
        return s.replaceAll("\\?[A-Z]+\\d+", "?X")
                .replaceAll("\\bX\\d+\\b", "X");
    }

    public static List<String> replaceFOFinfRule(List<String> proofLines, List<TPTPFormula> authored_lines) {
        Pattern filePat = Pattern.compile("file\\([^)]*\\)");

        for (TPTPFormula authored_step : authored_lines) {
            String targetFormula = stripVarNumbers(norm(authored_step.formula));
            if (targetFormula.startsWith("(") && targetFormula.endsWith(")")) {
                targetFormula = targetFormula.substring(1, targetFormula.length() - 1);
            }
            String replacement = authored_step.infRule; // must be full: file('path',tag)

            for (int i = 0; i < proofLines.size(); i++) {
                String original = proofLines.get(i);
                String line = original; // keep original for printing
                String lineTrim = original.trim();

                boolean startsFof = lineTrim.startsWith("fof(");
                boolean hasRole = line.contains(",axiom,") || line.contains(",conjecture,");
                boolean containsFormula = stripVarNumbers(norm(original)).contains(targetFormula);

                if (startsFof && hasRole) {
                    if (containsFormula) {
                        Matcher m = filePat.matcher(original);
                        if (m.find()) {
                            String replaced = m.replaceAll(replacement);
                            proofLines.set(i, replaced);
                        }
                    }
                }
            }
        }

        return proofLines;
    }


    // Save authored axioms + conjecture as clean TPTP, one fof(...) per line.
    public static List<TPTPFormula> writeMinTPTP(List<TPTPFormula> proof) {

        String outName = "min-problem.tptp";

        String outPath = java.nio.file.Paths.get(System.getProperty("user.dir"), outName)
                .toAbsolutePath().toString();

        List<String> out = new ArrayList<String>();
        List<TPTPFormula> authored_lines = new ArrayList<>();
        out.add("% Generated by TPTPutil: authored axioms + conjecture");
        String conjecture = null;

        for (TPTPFormula step : proof) {
            if (TPTPutil.sourceAxiom(step)) {          // only authored axioms
                out.add(oneLine(step.toString()));     // TPTP 'fof(...)' line
                authored_lines.add(step);
            }
            if ("conjecture".equals(step.role)) {
                conjecture = oneLine(step.toString());
            }
        }
        if (conjecture == null) System.out.println("-- ERROR: TPTPUtil.writeMinTPTP: No Conjecture found!");

        FileUtil.writeLines(outPath, out); // overwrite = false append

        return authored_lines;
    }

    private static String oneLine(String s) {
        String t = s.replace('\r', ' ').replace('\n', ' ').trim().replaceAll("\\s+", " ");
        return t.endsWith(".") ? t : (t + ".");
    }

    public static List<TPTPFormula> processProofLines(List<String> inputLines) {

        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        KBmanager.getMgr().initializeOnce();
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        KB kb = KBmanager.getMgr().getKB(kbName);

        // Clear file before processing
        List<String> lines = clearProofFile(inputLines);

        String query = Formula.LP;
        StringBuilder answerVars = new StringBuilder("");

        // Parse proof output
        tpp.parseProofOutput(lines, query, kb, answerVars);

        System.out.println("TPTPutil.main(): " + tpp.proof.size() + " steps ");
        System.out.println("TPTPutil.main(): showing only source axioms ");

        // Write minimal TPTP file with authored axioms
        return tpp.proof;
    }

    public static List<String> extractIncludesFromTPTP(File tptpFile) {
        List<String> includes = new ArrayList<>();
        Pattern pattern = Pattern.compile("include\\s*\\(\\s*'([^']+)'\\s*\\)", Pattern.CASE_INSENSITIVE);

        try {
            try (BufferedReader br = new BufferedReader(new FileReader(tptpFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    Matcher m = pattern.matcher(line);
                    while (m.find()) {
                        includes.add(m.group(1));  // capture the filename inside quotes
                    }
                }
            }
        }catch (IOException e) {
            System.out.println("ERROR: TPTPutil.extractIncludesFromTPTP: " + e.getMessage());
            e.printStackTrace();
        }
        return includes;
    }

    public static String validateIncludesInTPTPFiles(List<String> includes, String includesPath) {


        File incDir = new File(includesPath);

        // 1) Check if includes directory exists
        if (!incDir.exists() || !incDir.isDirectory()) {
            return ("Include directory not found: " + includesPath);
        }

        // 2) Check each include file inside that folder
        for (String inc : includes) {
            File f = new File(incDir, inc);
            if (!f.exists()) {
                return ("Missing include file: " + f.getAbsolutePath());
            }
        }

        return null;
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

//                    System.out.println("---- DEBUG: File lines read ----");
//                    System.out.println("Total lines: " + lines.size());
//                    for (int i = 0; i < lines.size(); i++) {
//                        System.out.println("Line " + i + ": " + lines.get(i));
//                    }
//                    System.out.println("---- END DEBUG ----");

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
                tpp = new TPTP3ProofProcessor();
                if (args[0].contains("p"))
                    tpp.setGraphFormulaFormat(TPTP3ProofProcessor.GraphFormulaFormat.TPTP);
                Vampire vamp = kb.askVampire(args[1], timeout, 1);
                System.out.println("KB.main(): completed Vampire query with result: " + StringUtil.arrayListToCRLFString(vamp.output));
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
