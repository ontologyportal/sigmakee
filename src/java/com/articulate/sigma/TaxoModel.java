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
    public static TaxoNode nodeTree = new TaxoNode();


    /** ***************************************************************
     * Create all nodes from the given term up to the root.
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
     * Create all nodes from the given term up to the root.
     */
    public static void createHierarchy (String nodeName) {

        nodeMap = new HashMap();
        nodeTree = new TaxoNode();
        nodeTree.name = nodeName;
        nodeMap.put(nodeName,nodeTree);
        expandNode(nodeName);
        TaxoNode child = nodeTree;
        ArrayList forms = new ArrayList();
        TaxoNode parent = new TaxoNode();
        do {
            System.out.println("INFO in TaxoModel.createHierarchy()");
            KB kb = KBmanager.getMgr().getKB(kbName);
            forms = kb.askWithRestriction(0,relation,1,nodeName);
            forms = removeCached(forms);
            System.out.println(relation + " " + nodeName);
            System.out.println(forms.size());
            for (int i = 0; i < forms.size(); i++) {
                Formula form = (Formula) forms.get(i);
                parent = new TaxoNode();
                parent.name = form.getArgument(2);
                if (parent.name.equals(child.name)) 
                    return;
                parent.childrenExpanded = true;
                parent.children.add(child);
                nodeMap.put(parent.name,parent);
            }
            System.out.println(parent.name);
            child = parent;
            nodeName = parent.name;
        } while (forms != null && forms.size() > 0);
    }

    /** ***************************************************************
     * Expand or contract a node based on its current state.
     */
    public static void toggleNode (String nodeName) {

        if (!nodeMap.keySet().contains(nodeName)) 
            createHierarchy(nodeName);        
        TaxoNode n = (TaxoNode) nodeMap.get(nodeName);
        n.childrenExpanded = ! n.childrenExpanded;
        if (n.childrenExpanded) 
            expandNode(nodeName);
        else
            n.children = new ArrayList();
    }

    /** ***************************************************************
     * Gather information from the knowledge base to create the data for
     * the children of this node.
     */
    public static void expandNode (String nodeName) {

        TaxoNode n = (TaxoNode) nodeMap.get(nodeName);
        n.childrenExpanded = true;
        KB kb = KBmanager.getMgr().getKB(kbName);
        ArrayList forms = kb.askWithRestriction(0,relation,2,nodeName);
        forms = removeCached(forms);
        for (int i = 0; i < forms.size(); i++) {
            Formula form = (Formula) forms.get(i);
            TaxoNode child = new TaxoNode();
            child.name = form.getArgument(1);
            child.childrenExpanded = false;
            n.children.add(child);
            nodeMap.put(child.name,child);
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
        sb.append(nodeTree.toHTML(kbHref));
        return sb.toString();
    }
}
