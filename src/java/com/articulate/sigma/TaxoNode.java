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
 * Class that holds information about each node in the tree.
 */
public class TaxoNode {

    public String name = ""; // the name of the SUMO term;
    public ArrayList parents = new ArrayList();       // ArrayList of TaxoNode(s)
    public ArrayList children = new ArrayList();    // ArrayList of TaxoNode(s)
    public boolean childrenExpanded = false;

    /** ***************************************************************
     */
    public String toHTML(String kbHref) {

        StringBuffer sb = new StringBuffer();
        sb.append("<li><a href=\"" + kbHref + name + "\">" + name + "</a></li>\n");
        if (childrenExpanded) {
            sb.append("<ul>");
            for (int i = 0; i < children.size(); i++) {
                TaxoNode child = (TaxoNode) children.get(i);
                sb.append(child.toHTML(kbHref));
            }
            sb.append("</ul>\n");
        }
        return sb.toString();
    }
}

