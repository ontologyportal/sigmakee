package com.articulate.sigma;

/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

import java.util.*;
import java.io.*;

/** Handle operations for creating a graphical representation of partial
 *  ordering relations.  Supports Graph.jsp.  */
public class Graph {

    /** *************************************************************
     * Create a ArrayList with a set of terms comprising a hierarchy
     * Each term String will be prefixed with an appropriate number of
     * indentChars. 
     *
     * @param kb the knowledge base being graphed
     * @param term the term in the KB being graphed
     * @param relation the binary relation that is used to forms the arcs
     *                 in the graph.
     * @param above the number of levels above the given term in the graph
     * @param below the number of levels below the given term in the graph
     * @param indentChars a String of characters to be used for indenting the terms
     */
    public static ArrayList createGraph(KB kb, String term, String relation, 
                                        int above, int below, String indentChars) {

        ArrayList result = new ArrayList();
        result = createGraphBody(kb,term,relation,above,0,indentChars,above,true);
        result.addAll(createGraphBody(kb,term,relation,0,below,indentChars,above,false));
        return result;
    }

    /** *************************************************************
     * The main body for createGraph().
     */
    private static ArrayList createGraphBody(KB kb, String term, String relation, 
                                        int above, int below, String indentChars,int level, boolean show) {

        ArrayList result = new ArrayList();
        ArrayList parents = new ArrayList();
        ArrayList children = new ArrayList();

        if (above > 0) {
            ArrayList stmtAbove = kb.askWithRestriction(0,relation,1,term);
            for (int i = 0; i < stmtAbove.size(); i++) {
                Formula f = (Formula) stmtAbove.get(i);
                String newTerm = f.getArgument(2);
                result.addAll(createGraphBody(kb,newTerm,relation,above-1,0,indentChars,level-1,true));
            }
        }

        StringBuffer prefix = new StringBuffer();
        for (int i = 0; i < level; i++)
            prefix = prefix.append(indentChars);
        
        String hostname = KBmanager.getMgr().getPref("hostname");
        if (hostname == null)
            hostname = "localhost";
        String port = KBmanager.getMgr().getPref("port");
        if (port == null)
            port = "8080";
        String kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?lang=" + kb.language + "&kb=" + kb.name;
        String formattedTerm = "<a href=\"" + kbHref + "&term=" + term + "\">" + term + "</a>";
        
        if (show) 
            result.add(prefix + formattedTerm);
        
        if (below > 0) {
            ArrayList stmtBelow = kb.askWithRestriction(0,relation,2,term);
            for (int i = 0; i < stmtBelow.size(); i++) {
                Formula f = (Formula) stmtBelow.get(i);
                String newTerm = f.getArgument(1);
                result.addAll(createGraphBody(kb,newTerm,relation,0,below-1,indentChars,level+1,true));
            }
        }
        
        return result;
    }

}
