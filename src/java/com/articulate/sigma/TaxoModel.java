package com.articulate.sigma;
import java.util.ArrayList;
import java.util.HashMap;

import com.articulate.sigma.KB;

/** This code is copyright Articulate Software (c) 2003.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
in any writings, briefings, publications, presentations, or 
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
    public static String kbName = "";
    public static String defaultTerm = "Entity";
    public static String termPage = "SimpleBrowse.jsp";
    public static HashMap<String,TaxoNode> nodeMap = new HashMap<String,TaxoNode>();
    public static HashMap<String,TaxoNode> rootList = new HashMap<String,TaxoNode>();

    /** ***************************************************************
     * Remove the old tree and start over from termName.
     */
    public static void newTree (String termName) {

        rootList.clear();  // = new HashMap();
        TaxoNode n = new TaxoNode();
        n.name = termName;
        String key = kbName + ":" + n.name;
        rootList.put(key,n);
        nodeMap.put(key,n);
    }

    /** ***************************************************************
     * Remove any cached formulas from a list.
     */
    public static ArrayList<Formula> removeCached (ArrayList<Formula> forms) {

        ArrayList<Formula> result = new ArrayList<Formula>();
        for (int i = 0; i < forms.size(); i++) {
            Formula f = (Formula) forms.get(i);
            //if (f == null || f.sourceFile == null) {
            //	System.out.println("Error in TaxoModel.removeCached(): null formula or sourceFile field: " + f);
            //	System.out.println(f.sourceFile);
            //	System.out.println(KB._cacheFileSuffix);
            //}
            if (f == null || f.sourceFile == null || !KButilities.isCacheFile(f.sourceFile))
                result.add(f);
        }
        return result;
    }

    /** ***************************************************************
     * Remove the parents of this node.
     */
    public static void collapseParentNodes (String nodeName) {

        String key = kbName + ":" + nodeName;
        TaxoNode n = (TaxoNode) nodeMap.get(key);
        if (n == null) {
            System.out.println("Error in TaxoModel.collapseParentNodes(): Bad key: " + key);
            return;
        }
        for (int i = 0; i < n.parents.size(); i++) {
            TaxoNode parent = (TaxoNode) n.parents.get(i);
            collapseParentNodes(parent.name);
            String parentKey = kbName + ":" + parent.name;
            nodeMap.remove(parentKey);
            if (rootList.containsKey(parentKey)) 
                rootList.remove(parentKey);
        }
        n.parents = new ArrayList<TaxoNode>();
        rootList.put(key,n);
    }

    /** ***************************************************************
     * Gather information from the knowledge base to create the data for
     * the parents of this node.
     */
    public static void expandParentNodes (String nodeName) {

        String key = kbName + ":" + nodeName;
        TaxoNode n = (TaxoNode) nodeMap.get(key);
        if (n == null) {
            System.out.println("Error in TaxoModel.expandParentNodes(): Bad key: " + key);
            return;
        }
        n.parents = new ArrayList<TaxoNode>();
        rootList.clear();  // = new HashMap();
        KB kb = KBmanager.getMgr().getKB(kbName);
        ArrayList<Formula> forms = kb.askWithPredicateSubsumption(relation,1,nodeName);
        forms = removeCached(forms);
        for (int i = 0; i < forms.size(); i++) {
            Formula form = (Formula) forms.get(i);
            TaxoNode parent = new TaxoNode();
            parent.name = form.getStringArgument(2);
            if (parent.name.equals(n.name)) 
                return;
            parent.childrenExpanded = false;
            parent.oneChild = n;
            n.parents.add(parent);
            String parentKey = kbName + ":" + parent.name;
            if (!nodeMap.containsKey(parentKey)) 
                nodeMap.put(parentKey,parent);
            rootList.put(parentKey,parent);
        }
    }

    /** ***************************************************************
     * Remove the children of this node.Called as the result of an &contract=term
     * parameter sent to TreeView.jsp
     */
    public static void collapseNode (String nodeName) {

        String key = kbName + ":" + nodeName;
        TaxoNode n = (TaxoNode) nodeMap.get(key);
        if (n == null) {
            System.out.println("Error in TaxoModel.collapseNode(): Bad key: " + key);
            return;
        }
        n.childrenExpanded = false;
        if (n.oneChild != null) {
            collapseNode(n.oneChild.name);
            String oneChildKey = kbName + ":" + n.oneChild.name;
            nodeMap.remove(oneChildKey);
        }
        n.oneChild = null;
        for (int i = 0; i < n.children.size(); i++) {
            TaxoNode child = (TaxoNode) n.children.get(i);
            collapseNode(child.name);
            String childKey = kbName + ":" + child.name;
            nodeMap.remove(childKey);
        }
        n.children = new ArrayList();
    }

    /** ***************************************************************
     * Gather information from the knowledge base to create the data for
     * the children of this node.  Called as the result of an &expand=term
     * parameter sent to TreeView.jsp
     */
    public static void expandNode (String nodeName) {

        String key = kbName + ":" + nodeName;
        TaxoNode n = (TaxoNode) nodeMap.get(key);
        if (n == null) {
            System.out.println("Error in TaxoModel.expandNode(): Bad key: " + key);
            return;
        }
        n.childrenExpanded = true;
        n.oneChild = null;
        n.children = new ArrayList();
        KB kb = KBmanager.getMgr().getKB(kbName);
        ArrayList forms = kb.askWithPredicateSubsumption(relation,2,nodeName);
        // kb.askWithRestriction(0,relation,2,nodeName);
        forms = removeCached(forms);
        for (int i = 0; i < forms.size(); i++) {
            Formula form = (Formula) forms.get(i);
            TaxoNode child = new TaxoNode();
            child.name = form.getStringArgument(1);
            n.children.add(child);
            String childKey = kbName + ":" + child.name;
            nodeMap.put(childKey,child);
        }
    }

    /** ***************************************************************
     * If the given name is already displayed, do nothing, otherwise
     * create a new tree with that one node.
     */
    public static void displayTerm (String nodeName) {

        String key = kbName + ":" + nodeName;
        if (!nodeMap.containsKey(key)) {
            nodeMap.clear();  // = new HashMap();
            TaxoNode n = new TaxoNode();
            n.name = nodeName;
            nodeMap.put(key,n);
            rootList.clear(); // = new HashMap();
            rootList.put(key,n);
        }
    }

    /** ***************************************************************
     */
    public static String toHTML(String kbHref) {

        StringBuffer sb = new StringBuffer();
        Object[] objArr = rootList.values().toArray();
        for (int i = 0; i < objArr.length; i++) {
            TaxoNode n = (TaxoNode) objArr[i];
            sb.append(n.toHTML(kbHref,0));
        }
        return sb.toString();
    }
}
