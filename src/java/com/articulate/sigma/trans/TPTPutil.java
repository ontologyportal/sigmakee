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
import tptp_parser.TPTPFormula;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

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

        if (t.endsWith(Formula.termMentionSuffix) || t.endsWith(Formula.termMentionSuffix))
            return t.substring(0,t.length()-Formula.termMentionSuffix.length());
        else
            return t;
    }

    /** ***************************************************************
     *  Remove enclosing meta-information from a TPTP axiom.
     */
    private static String returnAndIndent(int level) {

    	StringBuffer result = new StringBuffer();
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

        String indentChars = "  ";
        String eolChars = "\n";

        //System.out.println("INFO in Formula.htmlTPTPFormat(): " + f.toString());
        //System.out.println("INFO in Formula.htmlTPTPFormat(): theTptpFormulas.size()" + f.theTptpFormulas.size());
        if (f.theTptpFormulas == null || f.theTptpFormulas.size() < 1) 
            return "No TPTP formula.  May not be expressible in strict first order.";        
        StringBuffer result = new StringBuffer();
        for (String formString : f.theTptpFormulas) {
            if (!StringUtil.emptyString(formString)) {
                //System.out.println("INFO in Formula.htmlTPTPFormat(): TPTP formula: " + formString);
                formString = formString.trim();
            }
            else {
                System.out.println("Error in Formula.htmlTPTPFormat(): empty TPTP formula: " + formString);
                continue;
            }
            formString = extractTPTPaxiom(formString);
            boolean inComment = false;
            boolean inToken = false;
            StringBuffer token = new StringBuffer();
            int level = 0;
            int tokenNum = 0;
            boolean inQuantifier = false;

            int flen = formString.length();
            char ch = '0';   // char at i
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
                            String tokenNoSuffix = removeTPTPSuffix(token.toString());
                            result.append("<a href=\"" + hyperlink + "&term=" + tokenNoSuffix + "\">s__" + token.toString() + "</a>");
                            token = new StringBuffer();
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
                    else if (formString.substring(i).startsWith("=>")) {
                        i++;
                        if (traditionalLogic)
                        	result.append("&rArr;");
                        else
                        	result.append("=&gt;");
                        result.append(returnAndIndent(level));
                    }
                    else {
                        if (formString.substring(i).startsWith("s__")) {
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
     * Is the axiom in a proof a source authored axiom from SUMO,
     * rather than one automatically derived or introduced by a
     * theorem prover
     */
    public static boolean sourceAxiom(TPTPFormula ps) {

        //System.out.println("sourceAxiom() supports: " + ps.supports.size());
        //System.out.println("sourceAxiom() ps.infRule: " + ps.infRule);
        return ps.supports.size() == 0 && !ps.infRule.startsWith("introduced");
    }

    /** ***************************************************************
     * Is there a citation as a containsFormula relation for this
     * axiom?
     */
    public static boolean citation(String sumoStep, String stepName, KB kb) {

        //System.out.println("\nTPTPutil.citation: sumoStep: " + sumoStep);
        //System.out.println("TPTPutil.citation: stepName: " + stepName);
        ArrayList<Formula> ciAxioms = kb.ask("arg",0,"containsFormula");
        //System.out.println("TPTPutil.citation: formulas: " + ciAxioms);
        for (Formula f : ciAxioms) {
            Formula arg = f.getArgument(2);
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

        ArrayList<Formula> ciAxioms = kb.ask("arg",0,"containsFormula");
        //System.out.println("TPTPutil.getCitationString: stepName: " + stepName);
        //System.out.println("TPTPutil.getCitationString: sumo: " + sumoStep);
        for (Formula f : ciAxioms) {
            Formula arg = f.getArgument(2);
            if (arg != null && arg.listP()) {
                if (arg.equals(new Formula(sumoStep))) {
                    //System.out.println("TPTPutil.getCitationString: formula arg: " + arg);
                    String term = f.getStringArgument(1);
                    ArrayList<Formula> comments = kb.askWithRestriction(0,"comment",1,term);
                    if (comments != null && comments.size() > 0)
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
        f.theTptpFormulas = new HashSet();
        //f.theTptpFormulas.add("fof(kb_ArabicCulture_20,axiom,(( s__subclass(s__Hajj,s__Translocation) ))).");
        f.theTptpFormulas.add("(! [V__P] : (s__instance(V__P,s__Agent) => ((s__attribute(V__P,s__Muslim) & s__capability(s__Hajj,s__agent__m,V__P)) => " +
                "s__modalAttribute('(? [V__H] : (s__instance(V__H,s__Process) & s__instance(V__H,s__Hajj) & s__agent(V__H,V__P)))',s__Obligation))))");
        System.out.println(TPTPutil.htmlTPTPFormat(f,"http://sigma.ontologyportal.org:4040/sigma?kb=SUMO&term=",false));
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("TPTPutil class");
        System.out.println("  options (with a leading '-'):");
        System.out.println("  f <file> - parse a TPTP3 proof file and output source axioms in SUO-KIF");
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
            KBmanager.prefOverride.put("loadLexicons","false");
            KBmanager.getMgr().initializeOnce();
            String kbName = KBmanager.getMgr().getPref("sumokbname");
            KB kb = KBmanager.getMgr().getKB(kbName);
            if (args != null && args.length > 1 && args[0].contains("f")) {
                try {
                    List<String> lines = FileUtil.readLines(args[1],false);
                    String query = "(";
                    StringBuffer answerVars = new StringBuffer("");
                    System.out.println("input: " + lines + "\n");
                    tpp.parseProofOutput((ArrayList<String>) lines, query, kb,answerVars);
                    System.out.println("TPTPutil.main(): " + tpp.proof.size() + " steps ");
                    System.out.println("TPTPutil.main(): showing only source axioms ");
                    for (TPTPFormula step : tpp.proof) {
                        //System.out.println(step);
                        if (TPTPutil.sourceAxiom(step)) {
                            Formula f = new Formula(step.sumo);
                            System.out.println(f.format("","  ","\n"));
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if (args != null && args.length > 1 && args[0].contains("i")) {
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
                    for (TPTPFormula step : tpp.proof) {
                        //System.out.println("TPTPutil.main(): step: " + step);
                        if (TPTPutil.sourceAxiom(step)) {
                            Formula f = new Formula(step.sumo);
                            String name = step.infRule;
                            //System.out.println("TPTPutil.main(): name: " + name);
                            String id = "";
                            if (name.startsWith("file(")) {
                                int firstParen = name.indexOf("(");
                                int firstComma = name.indexOf(",");
                                int secondParen = name.indexOf(")", firstComma + 1);
                                id = name.substring(firstComma + 1, secondParen);
                                //System.out.println("TPTPutil.main(): id: " + id);
                                if (KB.axiomKey.keySet().contains(id)) {
                                    //System.out.println("TPTPutil.main(): formula: " + KB.axiomKey.get(id));
                                    String str = getCitationString(KB.axiomKey.get(id).toString(), name, kb);
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
