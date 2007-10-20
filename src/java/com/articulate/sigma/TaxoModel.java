package com.articulate.sigma;
import java.util.*;

/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/

/** ***************************************************************
 * A model for an expandable tree widget
 */
public class TaxoModel {

    public static String kbHref = "";
    public static String relation = "subclass";
    public static String kbName = "SUMO";
    public static String defaultTerm = "Entity";
    public static String termPage = "SimpleBrowse.jsp";
    public static HashMap nodeMap = new HashMap();
    public static HashMap rootList = new HashMap();

    /** ***************************************************************
     * Remove any cached formulas from a list.
     */
    public static void newTree (String termName) {

        rootList = new HashMap();
        TaxoNode n = new TaxoNode();
        n.name = termName;
        rootList.put(n.name,n);
        nodeMap.put(n.name,n);
    }

    /** ***************************************************************
     * Remove any cached formulas from a list.
     */
    public static ArrayList removeCached (ArrayList forms) {

        ArrayList result = new ArrayList();
        for (int i = 0; i < forms.size(); i++) {
            Formula f = (Formula) forms.get(i);
            if (!f.sourceFile.endsWith("Cache.kif")) 
                result.add(f);
        }
        return result;
    }

    /** ***************************************************************
     * Remove the parents of this node.
     */
    public static void collapseParentNodes (String nodeName) {

        TaxoNode n = (TaxoNode) nodeMap.get(nodeName);
        for (int i = 0; i < n.parents.size(); i++) {
            TaxoNode parent = (TaxoNode) n.parents.get(i);
            collapseParentNodes(parent.name);
            nodeMap.remove(parent.name);
            if (rootList.keySet().contains(parent.name)) 
                rootList.remove(parent.name);
        }
        n.parents = new ArrayList();
        rootList.put(n.name,n);
    }

    /** ***************************************************************
     * Gather information from the knowledge base to create the data for
     * the parents of this node.
     */
    public static void expandParentNodes (String nodeName) {

        TaxoNode n = (TaxoNode) nodeMap.get(nodeName);
        n.parents = new ArrayList();
        rootList = new HashMap();
        KB kb = KBmanager.getMgr().getKB(kbName);
        ArrayList forms = kb.askWithRestriction(0,relation,1,nodeName);
        forms = removeCached(forms);
        for (int i = 0; i < forms.size(); i++) {
            Formula form = (Formula) forms.get(i);
            TaxoNode parent = new TaxoNode();
            parent.name = form.getArgument(2);
            if (parent.name.equals(n.name)) 
                return;
            parent.childrenExpanded = false;
            parent.oneChild = n;
            n.parents.add(parent);
            if (!nodeMap.keySet().contains(parent.name)) 
                nodeMap.put(parent.name,parent);
            rootList.put(parent.name,parent);
        }
    }

    /** ***************************************************************
     * Remove the children of this node.Called as the result of an &contract=term
     * parameter sent to TreeView.jsp
     */
    public static void collapseNode (String nodeName) {

        TaxoNode n = (TaxoNode) nodeMap.get(nodeName);
        n.childrenExpanded = false;
        if (n.oneChild != null) {
            collapseNode(n.oneChild.name);
            nodeMap.remove(n.oneChild.name);
        }
        n.oneChild = null;
        for (int i = 0; i < n.children.size(); i++) {
            TaxoNode child = (TaxoNode) n.children.get(i);
            collapseNode(child.name);
            nodeMap.remove(child.name);
        }
        n.children = new ArrayList();
    }

    /** ***************************************************************
     * Gather information from the knowledge base to create the data for
     * the children of this node.  Called as the result of an &expand=term
     * parameter sent to TreeView.jsp
     */
    public static void expandNode (String nodeName) {

        TaxoNode n = (TaxoNode) nodeMap.get(nodeName);
        n.childrenExpanded = true;
        n.oneChild = null;
        n.children = new ArrayList();
        KB kb = KBmanager.getMgr().getKB(kbName);
        ArrayList forms = kb.askWithRestriction(0,relation,2,nodeName);
        forms = removeCached(forms);
        for (int i = 0; i < forms.size(); i++) {
            Formula form = (Formula) forms.get(i);
            TaxoNode child = new TaxoNode();
            child.name = form.getArgument(1);
            n.children.add(child);
            nodeMap.put(child.name,child);
        }
    }

    /** ***************************************************************
     * If the given name is already displayed, do nothing, otherwise
     * create a new tree with that one node.
     */
    public static void displayTerm (String nodeName) {

        if (!nodeMap.keySet().contains(nodeName)) {
            nodeMap = new HashMap();
            TaxoNode n = new TaxoNode();
            n.name = nodeName;
            nodeMap.put(nodeName,n);
            rootList = new HashMap();
            rootList.put(n.name,n);
        }
    }

    /** ***************************************************************
     */
    public static String toHTML(String simple) {

        String hostname = KBmanager.getMgr().getPref("hostname");
        if (hostname == null)
           hostname = "localhost";
        String port = KBmanager.getMgr().getPref("port");
        if (port == null)
           port = "8080";
        kbHref = "http://" + hostname + ":" + port + "/sigma/TreeView.jsp?kb=" + kbName + 
            "&simple=" + simple + "&term=";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < rootList.values().size(); i++) {
            TaxoNode n = (TaxoNode) rootList.values().toArray()[i];
            sb.append(n.toHTML(kbHref,0));
        }
        return sb.toString();
    }
}
