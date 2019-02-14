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
import com.articulate.sigma.StringUtil;

import java.util.ArrayList;
import java.util.HashSet;

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

        if (t.endsWith("__m") || t.endsWith("__t"))         
            return t.substring(0,t.length()-3);
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
     * @param indentChars - the proper characters for indenting text.
     * @param eolChars - the proper character for end of line.
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
     */
	public static void main(String[] args) {
        Formula f = new Formula();
        f.theTptpFormulas = new HashSet();
        //f.theTptpFormulas.add("fof(kb_ArabicCulture_20,axiom,(( s__subclass(s__Hajj,s__Translocation) ))).");
        f.theTptpFormulas.add("(! [V__P] : (s__instance(V__P,s__Agent) => ((s__attribute(V__P,s__Muslim) & s__capability(s__Hajj,s__agent__m,V__P)) => " +
        		"s__modalAttribute('(? [V__H] : (s__instance(V__H,s__Process) & s__instance(V__H,s__Hajj) & s__agent(V__H,V__P)))',s__Obligation))))");
        System.out.println(TPTPutil.htmlTPTPFormat(f,"http://sigma.ontologyportal.org:4040/sigma?kb=SUMO&term=",false));
	}

}
