

/** This code is copyright Articulate Software (c) 2003.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following articles in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
  
(the following article need be cited only if using CELT)
Pease, A., and Murray, W., (2003).  An English to Logic Translator for
Ontology-based Knowledge Representation Languages.  In Proceedings of
the 2003 IEEE International Conference on Natural Language Processing
and Knowledge Engineering, Beijing, China, pp 777-783.

*/

package com.articulate.celt;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import javax.swing.tree.*;

import com.articulate.sigma.*;

/**
 * A Java reimplementation of the CELT prolog system. The CELT interpreter
 * relies on some meta-information embedded in the KIF syntax of 
 * GrammarNode.KIFtemplate.
 * 
 * #VARNAME - replace the identifier with the KIFexpression for the node with
 *   the same name.  This identifier must be terminated by a whitespace or 
 *   close parenthesis. Multiple variables as well as occurrances of the same
 *   variable are allowed.
 * 
 * %ONEVARNAME=ANOTHERVARNAME - replace variables appearing in child nodes 
 *   as ?ONEVARNAME with ?ANOTHERVARNAME.  This expression must be terminated
 *   by a whitespace.  Multiple variables replacement statements are allowed.
 * 
 * %ONEVARNAME=@ANOTHERVARNAME - will replace variables appearing in child nodes 
 *   as ?ONEVARNAME with ANOTHERVARNAME
 */
public class CELTinterpreter {

    public DefaultTreeModel grammarTree = null;
    public HashMap vars = new HashMap();

    /** ***************************************************************
     */
    public CELTinterpreter(DefaultTreeModel model) {

        grammarTree = model;
    }

    /** ***************************************************************
     * Delete any '#' variables which haven't been matched during interpretation.
     */
    private String removeMetaVariables(String s) {

        if (s == null) 
            return null;
        s = s.replaceAll("#\\w+","");
        return s.replaceAll("\\%\\w+=\\w+","");
    }

    /** ***************************************************************
     * Delete any variables that appear in a quantifier list, but not
     * in the body of the quantified clause.  For example, in
     *   (exists (?FOO ?BAR) (instance ?FOO Kloz))
     * the variable ?BAR is not needed.
     */
    private String removeUnusedQuantifiedVariables(String s) {

        System.out.println("INFO in CELTinterpreter.removeUnusedQuantifiedVariables(): Input: " + s);
        if (s == null) 
            return null;
        Pattern p = Pattern.compile(".*\\(exists \\(([^\\)]+)\\).*");
        Matcher m = p.matcher(s);
        if (m.matches()) {
            String vars = m.group(1);
            System.out.println("INFO in CELTinterpreter.removeUnusedQuantifiedVariables(): Vars: " + vars);
            String quantifierString = new String("(exists (" + vars + ")");
            int quantifierIndex = s.indexOf(quantifierString);
            int quantifierEnd = quantifierIndex + quantifierString.length();
            String varList[] = vars.split(" ");
            for (int i = 0; i < varList.length; i++) {
                if (s.indexOf(varList[i],quantifierEnd) == -1) 
                    s = s.replaceFirst("\\" + varList[i],"");    // backslashes are needed to escape the leading '?'
            }
        }
        else {
            p = Pattern.compile("\\(forall \\(([^\\)]+)\\)");
            m = p.matcher(s);
            if (m.matches()) {
                String vars = m.group(1);
                String quantifierString = new String("(forall (" + vars + ")");
                int quantifierIndex = s.indexOf(quantifierString);
                int quantifierEnd = quantifierIndex + quantifierString.length();
                String varList[] = vars.split(" ");
                for (int i = 0; i < varList.length; i++) {
                    if (s.indexOf(varList[i],quantifierEnd) == -1) 
                        s = s.replaceFirst("\\" + varList[i],"");  // backslashes are needed to escape the leading '?'                                  
                }
            }
        }
        System.out.println("INFO in CELTinterpreter.removeUnusedQuantifiedVariables(): Output: " + s);
        return s;
    }

    /** ***************************************************************
     * Delete any variable reassignments.
     */
    private String removeVariableReassignments(String s) {

        if (s == null) 
            return null;
        return s.replaceAll("\\%\\w+=\\w+","");
    }

    /** ***************************************************************
     */
    private String processMultipleWNterms (String SUMOterms, GrammarNode nInfo) {
        
        StringBuffer result = new StringBuffer();
        SUMOterms = SUMOterms.replaceAll("[\\%\\&\\=\\+\\@]","");
        String termArray[] = SUMOterms.split(" ");
        ArrayList statements = new ArrayList();
        for (int i = 0; i < termArray.length; i++) {
            String statement = nInfo.KIFexpression;
            statement = statement.replaceAll("#" + nInfo.nodeName.toUpperCase(),termArray[i]);
            result = result.append(" " + statement);
        }        
        return result.toString().trim();
    }

    /** ***************************************************************
     * Rename variables according to meta information in the expression.
     * %ONEVARNAME=ANOTHERVARNAME - will replace variables appearing in child nodes 
     *   as ?ONEVARNAME with ?ANOTHERVARNAME
     * 
     * %ONEVARNAME=@ANOTHERVARNAME - will replace variables appearing in child nodes 
     *   as ?ONEVARNAME with ANOTHERVARNAME
     */
    private String renameVariables(String s) {

        if (s == null) 
            return null;
        // System.out.println("INFO in CELTinterpreter.renameVariables(): Input: " + s);
        int replaceend = 0;
        while (s.indexOf("%",replaceend) != -1) {
            int varstart = s.indexOf("%",replaceend) + 1;
            int varend = s.indexOf("=",varstart);
            if (varend == -1) {
                // System.out.println("Error in CELTinterpreter.renameVariables: Error parsing variable replacement expression: " + s);
                return null;
            }
            String variable = s.substring(varstart,varend);
            int replacestart = varend + 1;
            if (s.charAt(varend+1) == '@') 
                replacestart = varend + 2;
            replaceend = replacestart;
            while (replaceend < s.length() && 
                   Character.isJavaIdentifierPart(s.charAt(replaceend))) 
                replaceend++;
            String replacement = s.substring(replacestart,replaceend);

            // System.out.println("INFO in CELTinterpreter.renameVariables(): variable and replacement: " + variable + " " + replacement);
            if (s.charAt(varend+1) == '@') {
                s = s.replaceAll("\\?" + variable,replacement);
                s = s.replaceAll("\\?" + variable + "=\\?",replacement + "=\\?");  // also replace variable renames. 
            }
            else
                s = s.replaceAll("\\?" + variable,"?" + replacement);
        }
        // System.out.println("INFO in CELTinterpreter.renameVariables(): Output: " + s);
        return s;
    }


    /** ***************************************************************
     * Substitute interpretations takes from vars for variable names
     * in the nInfo.KIFexpression.  Variable names are all caps and preceded
     * by a '#' character.
     */
    private String performSubstitutions(GrammarNode nInfo, HashMap vars) {

        System.out.println("INFO in CELTinterpreter.performSubstitutions(): Node " + nInfo.nodeName);
        System.out.println(vars.keySet());
        System.out.println(vars.values());

        if (nInfo.KIFexpression == null) {
            if (nInfo.KIFtemplate == null) 
                return null;
            nInfo.KIFexpression = new String(nInfo.KIFtemplate);
        }
        // System.out.println("INFO in CELTinterpreter.performSubstitutions(): Before KIFexpression " + nInfo.KIFexpression);
        if (nInfo.nodeName.indexOf("WordNet") == -1) {
            int start = 0;
            while (start < nInfo.KIFexpression.length() && 
                   nInfo.KIFexpression.indexOf("#",start) != -1) {
                start = nInfo.KIFexpression.indexOf("#",start) + 1;
                int end = start;
                while (end < nInfo.KIFexpression.length() && 
                       Character.isJavaIdentifierPart(nInfo.KIFexpression.charAt(end))) 
                    end++;
                String variable = nInfo.KIFexpression.substring(start,end).intern();
                if (vars.containsKey(variable)) {
                    String substExpression = (String) vars.get(variable.intern());
                    nInfo.KIFexpression = nInfo.KIFexpression.replaceAll("#" + variable, substExpression);
                }
                start = end;
            }
        }
        else {
            if (nInfo.SUMOterm != null) {
                if (nInfo.SUMOterm.trim().indexOf(" ") != -1)     // There are multiple SUMO terms for this synset.
                    nInfo.KIFexpression = processMultipleWNterms(nInfo.SUMOterm,nInfo);
                else
                    nInfo.KIFexpression = nInfo.KIFtemplate.replaceAll("#" + nInfo.nodeName.toUpperCase(),nInfo.SUMOterm);
            }
        }
        // System.out.println("INFO in CELTinterpreter.performSubstitutions(): After KIFexpression " + nInfo.KIFexpression);
        return nInfo.KIFexpression;
    }

    /** ***************************************************************
     * Recursively interpret the semantics of each node in the parse tree.
     * 
     * @param nodeMap contains associations between a node name (String)
     *   and the KIF interpretation of the node
     * @param node is the node to be interpreted.
     */
    public String interpretNode(DefaultMutableTreeNode node, HashMap nodeMap) {

        if (nodeMap == null) 
            nodeMap = new HashMap();
        GrammarNode nInfo = (GrammarNode) node.getUserObject();
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
            GrammarNode childNInfo = (GrammarNode) childNode.getUserObject();
            if (childNInfo.selected) {
                HashMap childNodeMap = new HashMap();
                String kif = interpretNode(childNode,childNodeMap);
                if (kif != null) 
                    nodeMap.put(childNInfo.nodeName.toUpperCase().intern(),kif);
                nodeMap.putAll(childNodeMap);
                if (nInfo.KIFtemplate == null && kif != null)      // A node without a template gets the value of its last selected child node.
                    nInfo.KIFexpression = kif;      // This could be implemented instead by have each node in the grammar contain variables for each of its children.
            }
        }
        String result = performSubstitutions(nInfo,nodeMap);
        if (result != null) {
            result = renameVariables(result);
            result = removeVariableReassignments(result);
            result = removeMetaVariables(result);
            result = removeUnusedQuantifiedVariables(result);
        }
        grammarTree.nodeChanged(node);            // Let the changes to the node be visible in the GUI.
        return result;
    }

    public static void main(String args[]) {

        String s =  "(exists (?SUBJECT ?VERB ?OBJECT ?INDIRECTOBJECT) (and (agent ?VERB ?SUBJECT) (instance John Male) (instance ?VERB Impelling) (instance ?OBJECT TransportationDevice)))";
        Pattern p = Pattern.compile(".*\\(exists \\(([^\\)]+)\\).*");
        Matcher m = p.matcher(s);
        if (m.matches()) 
            System.out.println("Matches.");
        else
            System.out.println("Doesn't match.");
    }
}
