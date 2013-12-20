package com.articulate.sigma;
import java.util.ArrayList;

import com.articulate.sigma.KB;

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
 * Class that holds information about each node in the tree.
 * A node may either display all its children, or just one, which leads
 * to a particular child node the user is interested in.
 */
public class TaxoNode {

    public String name = ""; // the name of the SUMO term;
    public ArrayList<TaxoNode> parents = new ArrayList<TaxoNode>();    
    public ArrayList<TaxoNode> children = new ArrayList<TaxoNode>();   
    public boolean childrenExpanded = false;
    public TaxoNode oneChild = null;                // only one child may be displayed

    /** ***************************************************************
     */
    public String toHTML(String kbHref, int indentLevel) {

        StringBuffer sb = new StringBuffer();
        int width = indentLevel * 10;
        if (parents == null || parents.size() == 0) {
            KB kb = KBmanager.getMgr().getKB(TaxoModel.kbName);
            ArrayList<Formula> forms = kb.askWithRestriction(0,TaxoModel.relation,1,name);
            forms = TaxoModel.removeCached(forms);
            if (forms.size() > 0) 
                sb.append("<span style='white-space: nowrap;'><img src='pixmaps/trans.gif' width=" + width + " height=5><a href=\"" + kbHref + name + "&up=" + name + "\"><img border=0 height=11 src='pixmaps/arrowup.gif'></a>&nbsp;");
            else
                sb.append("<span style='white-space: nowrap;'><img src='pixmaps/trans.gif' width=" + width + " height=5>&nbsp;");
        }
        else
            sb.append("<span style='white-space: nowrap;'><img src='pixmaps/trans.gif' width=" + width + " height=5><a href=\"" + kbHref + name + "&down=" + name + "\"><img border=0 height=11 src='pixmaps/arrowdown.gif'></a>&nbsp;");
        if (childrenExpanded) 
            sb.append("<a href=\"" + kbHref + name + "&contract=" + name + "\"><img border=0 src='pixmaps/minus.gif'></a>&nbsp;");
        else
            sb.append("<a href=\"" + kbHref + name + "&expand=" + name + "\"><img border=0 src='pixmaps/plus.gif'></a>&nbsp;");
        sb.append("<a href=\"" + kbHref + name + "\">" + name + "</a></span><br>\n");
        if (childrenExpanded) {
            for (int i = 0; i < children.size(); i++) {
                TaxoNode child = (TaxoNode) children.get(i);
                sb.append(child.toHTML(kbHref,indentLevel+1));
            }
        }
        if (oneChild != null) 
            sb.append(oneChild.toHTML(kbHref,indentLevel+1));        
        return sb.toString();
    }
}

