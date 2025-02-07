package com.articulate.sigma;

import java.util.ArrayList;
import java.util.List;

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
    public List<TaxoNode> parents = new ArrayList<>();
    public List<TaxoNode> children = new ArrayList<>();
    public boolean childrenExpanded = false;
    public TaxoNode oneChild = null;                // only one child may be displayed

    /** ***************************************************************
     */
    public String toHTML(String kbHref, int indentLevel) {

        StringBuilder sb = new StringBuilder();
        int width = indentLevel * 10;
        if (parents == null || parents.isEmpty()) {
            KB kb = KBmanager.getMgr().getKB(TaxoModel.kbName);
            List<Formula> forms = kb.askWithRestriction(0,TaxoModel.relation,1,name);
            forms = TaxoModel.removeCached(forms);
            if (!forms.isEmpty())
                sb.append("<span style='white-space: nowrap;'><img src='pixmaps/trans.gif' width=").append(width).append(" height=5><a href=\"").append(kbHref).append(name).append("&up=").append(name).append("\"><img border=0 height=11 src='pixmaps/arrowup.gif'></a>&nbsp;");
            else
                sb.append("<span style='white-space: nowrap;'><img src='pixmaps/trans.gif' width=").append(width).append(" height=5>&nbsp;");
        }
        else
            sb.append("<span style='white-space: nowrap;'><img src='pixmaps/trans.gif' width=").append(width).append(" height=5><a href=\"").append(kbHref).append(name).append("&down=").append(name).append("\"><img border=0 height=11 src='pixmaps/arrowdown.gif'></a>&nbsp;");
        if (childrenExpanded)
            sb.append("<a href=\"").append(kbHref).append(name).append("&contract=").append(name).append("\"><img border=0 src='pixmaps/minus.gif'></a>&nbsp;");
        else
            sb.append("<a href=\"").append(kbHref).append(name).append("&expand=").append(name).append("\"><img border=0 src='pixmaps/plus.gif'></a>&nbsp;");
        sb.append("<a href=\"").append(kbHref).append(name).append("\">").append(name).append("</a></span><br>\n");
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

