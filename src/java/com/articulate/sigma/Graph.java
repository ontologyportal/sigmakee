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
     * Create a graph of a bounded size by incrementing the number of
     * levels above and below until the limit is reached or there are
     * no more levels in the knowledge base from the given term and 
     * relation.
     */
    public static ArrayList createBoundedSizeGraph(KB kb, String term, String relation, 
                                        int size, String indentChars) {

        ArrayList result = new ArrayList();
        ArrayList oldresult = new ArrayList();
        int above = 1;
        int below = 1;
        int oldlimit = -1;
        while ((result.size() < size) && (result.size() != oldlimit)) {
            oldlimit = result.size();
            //System.out.println("INFO in Graph.createBoundedSizeGraph(): result size : " + result.size());
            //System.out.println("INFO in Graph.createBoundedSizeGraph(): above, below, oldlimit : " + above + 
            //                   " " + below + " " + oldlimit);
            oldresult = result;
            HashSet checkAbove = new HashSet();
            HashSet checkBelow = new HashSet();
            result = createGraphBody(kb,checkAbove,term,relation,above,0,indentChars,above,true);
            result.addAll(createGraphBody(kb,checkBelow,term,relation,0,below,indentChars,above,false));
            above++;
            below++;
        }
        return oldresult;
    }

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
	HashSet checkAbove = new HashSet();
	HashSet checkBelow = new HashSet();
        result = createGraphBody(kb,checkAbove,term,relation,above,0,indentChars,above,true);
        result.addAll(createGraphBody(kb,checkBelow,term,relation,0,below,indentChars,above,false));
        return result;
    }

    /** *************************************************************
     * The main body for createGraph().
     */
    private static ArrayList createGraphBody(KB kb, Set check, String term, String relation, 
					     int above, int below, String indentChars,int level, boolean show) {

        ArrayList result = new ArrayList();
        ArrayList parents = new ArrayList();
        ArrayList children = new ArrayList();

	if ( ! check.contains(term) ) {
	    if (above > 0) {
		ArrayList stmtAbove = kb.askWithRestriction(0,relation,1,term);
		for (int i = 0; i < stmtAbove.size(); i++) {
		    Formula f = (Formula) stmtAbove.get(i);
		    String newTerm = f.getArgument(2);
		    if ( ! newTerm.equals(term) && !f.sourceFile.endsWith("_Cache.kif")) {
			result.addAll(createGraphBody(kb,check,newTerm,relation,above-1,0,indentChars,level-1,true));
		    }
		    check.add( term );
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
		    if ( ! newTerm.equals(term)  && !f.sourceFile.endsWith("_Cache.kif")) {
			result.addAll(createGraphBody(kb,check,newTerm,relation,0,below-1,indentChars,level+1,true));
		    }
		    check.add( term );
		}
	    }
	}
        
        return result;
    }

    /** *************************************************************
     * Create a ArrayList with a set of terms comprising a hierarchy
     *
     * @param kb the knowledge base being graphed
     * @param term the term in the KB being graphed
     * @param relation the binary relation that is used to forms the arcs
     *                 in the graph.
     * @param above the number of levels above the given term in the graph
     * @param below the number of levels below the given term in the graph
     * @param indentChars a String of characters to be used for indenting the terms
     */
    public static HashSet createDotGraph(KB kb, String term, String relation) {

        HashSet result = new HashSet();
	HashSet start = new HashSet();
	HashSet checked = new HashSet();
        start.add(term);
        createDotGraphBody(kb,start,checked,relation,true,result);
        start.add(term);
        createDotGraphBody(kb,start,checked,relation,false,result);
        return result;
    }

    /** *************************************************************
     * The main body for createGraph().
     */
    private static void createDotGraphBody(KB kb, HashSet startSet, HashSet checkedSet, 
                                                String relation, boolean upSearch, HashSet result) {

        //System.out.println("StartSet: " + startSet.toString());
        //System.out.println("CheckedSet: " + checkedSet.toString());

        while (startSet.size() > 0) {
            Iterator it = startSet.iterator();
            String term = (String) it.next();
            boolean removed = startSet.remove(term);
            if (!removed) 
                System.out.println("Error in createDotGraphBody(): " + term + " not removed");
            ArrayList stmts;
            if (upSearch)
                stmts = kb.askWithRestriction(0,relation,1,term);
            else
                stmts = kb.askWithRestriction(0,relation,2,term);
            for (int i = 0; i < stmts.size(); i++) {
                Formula f = (Formula) stmts.get(i);
                String newTerm;
                if (upSearch) 
                    newTerm = f.getArgument(2);
                else 
                    newTerm = f.getArgument(1);
                String s = "  \"" + term + "\" -> \"" + newTerm + "\";";
                //System.out.println(s);
                result.add(s);
                checkedSet.add(term);
                startSet.add(newTerm);
                createDotGraphBody(kb,startSet,checkedSet,relation,upSearch,result);
            } 
        }
    }


    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {

        try {
            KBmanager.getMgr().initializeOnce();
        } catch (IOException ioe ) {
            System.out.println(ioe.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB("SUMO");

        Graph.createBoundedSizeGraph(kb, "Process", "subclass", 50, "  ");
        /*
        Graph g = new Graph();
        HashSet result = g.createDotGraph(kb,"Entity","subclass");
        System.out.println("digraph G {");
        System.out.println("  rankdir=LR");
        Iterator it = result.iterator();
        while (it.hasNext()) {
            String s = (String) it.next();
            System.out.println(s);
        }
        System.out.println("}");
        */
    }
}
