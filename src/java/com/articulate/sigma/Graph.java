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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;

import javax.imageio.ImageIO;

/** Handle operations for creating a graphical representation of partial
 *  ordering relations.  Supports Graph.jsp.  */
public class Graph {

	private static Logger logger = null;
    private int graphsize = 0;                     // a limit counter to prevent pathologically large graphs
    public TreeMap columnList = new TreeMap();     // A map of the fields to display in the graph
                                                   // in addition to the indented term name with 
                                                   // option names as keys and "yes", "no" as values.

    /** *************************************************************
     */
    public Graph() {

    	if (logger == null)
    		logger = Logger.getLogger(this.getClass().getName());    	
        columnList.put("documentation","yes");
        columnList.put("direct-children","yes");
        columnList.put("graph","yes");
    }

    /** *************************************************************
     *  Show a count of the number of "children" which consists of
     *  instance, subclass, subrelation and subAttribute links
     */
    private String generateChildrenColumn(KB kb, String term) {

        // probably should find all instances of PartialOrderingRelation
        ArrayList relations = new ArrayList();
        relations.add("instance");
        relations.add("subclass");
        relations.add("subrelation");
        relations.add("subAttribute");

        int count = 0;
        for (int i = 0; i < relations.size(); i++) {
            ArrayList children = kb.askWithRestriction(0,(String) relations.get(i),2,term);
            if (children != null) 
                count = count + children.size();
        }
        if (count > 0)
            return Integer.toString(count);       
        else
            return ".";
    }

    /** *************************************************************
     *  Create a link from the term to the graph page for the term,
     *  given the href input that already includes the kb and lang
     *  parameters.
     */
    private String generateGraphLink(KB kb, String term, String href) {

        String result = href.replace("Browse.jsp","Graph.jsp");
        return "<a href=\"" + result + "&term=" + term + "&relation=subclass\">^</a>";
    }

    /** *************************************************************
     */
    private String generateDocumentationColumn(KB kb, String term, String href, String language) {

        String docString = "";
        ArrayList docStmts = kb.askWithRestriction(0,"documentation",1,term);
        if (docStmts.size() > 0) {
            Formula doc = (Formula) docStmts.get(0);
            docString = doc.getArgument(3);
            if (!DB.emptyString(docString)) {
                if (docString.length() > 100) 
                    docString = docString.substring(1,100) + "...";
                else
                    docString = docString.substring(1,docString.length()-1);
                return kb.formatDocumentation(href,docString,language);
            }
        }
        return "";
    }

    /** *************************************************************
     * Count the number of elements in columnList with the value
     * "yes".
     */
    public int columnCount() {

        int counter = 0;
        Iterator it = columnList.keySet().iterator();
        while (it.hasNext()) {
            String col = (String) it.next();
            String val = (String) columnList.get(col);
            if (val.equals("yes")) 
                counter++;
        }
        return counter;
    }

    /** *************************************************************
     * Create a <table> header that shows each of the columns to be
     * displayed in the HTML-based graph.
     */
    private String createColumnHeader() {

        StringBuffer result = new StringBuffer();
        result.append("<tr bgcolor=#EEEEEE><td></td>");             // a blank column for the indented terms
        Iterator it = columnList.keySet().iterator();
        while (it.hasNext()) {
            String col = (String) it.next();
            String val = (String) columnList.get(col);
            if (val.equals("yes")) {
                result.append("<td>" + col + "</td>");
            }
        }
        result.append("</tr>");
        return result.toString();
    }

    /** *************************************************************
     * Create a <table> header that shows each of the columns to be
     * displayed in the HTML-based graph.
     */
    private String createGraphEntry(KB kb, String prefix, String kbHref, String term, String language) {

        StringBuffer result = new StringBuffer();
        result.append("<tr>");
        String formattedTerm = "<a href=\"" + kbHref + "&term=" + term + "\">" + term + "</a>";
        result.append("<td>" + prefix + formattedTerm + "</td>");
        Iterator it = columnList.keySet().iterator();
        while (it.hasNext()) {
            String col = (String) it.next();
            String val = (String) columnList.get(col);
            if (val.equals("yes")) {
                if (col.equals("documentation")) 
                    result.append("<td><small>" + generateDocumentationColumn(kb,term,kbHref,language) + "</small></td>");
                if (col.equals("direct-children")) 
                    result.append("<td>" + generateChildrenColumn(kb,term) + "</td>");
                if (col.equals("graph")) 
                    result.append("<td>" + generateGraphLink(kb,term,kbHref) + "</td>");
            }
        }
        result.append("</tr>");
        return result.toString();
    }

    /** *************************************************************
     * Create a graph of a bounded size by incrementing the number of
     * levels above and below until the limit is reached or there are
     * no more levels in the knowledge base from the given term and 
     * relation.
     */
    public ArrayList createBoundedSizeGraph(KB kb, String term, String relation, 
                                        int size, String indentChars, String language) {

        ArrayList result = new ArrayList();
        ArrayList oldresult = new ArrayList();
        int above = 1;
        int below = 1;
        int oldlimit = -1;
        while ((result.size() < size) && (result.size() != oldlimit)) {
            oldlimit = result.size();
            oldresult = result;
            HashSet checkAbove = new HashSet();
            HashSet checkBelow = new HashSet();
            result = createGraphBody(kb,checkAbove,term,relation,above,0,indentChars,above,true,language);
            result.addAll(createGraphBody(kb,checkBelow,term,relation,0,below,indentChars,above,false,language));
            above++;
            below++;
        }
        return oldresult;
    }

    /** *************************************************************
     * Create an ArrayList with a set of terms comprising a hierarchy
     * Each term String will be prefixed with an appropriate number of
     * indentChars. creatGraphBody() does most of the work.
     *
     * @param kb the knowledge base being graphed
     * @param term the term in the KB being graphed
     * @param relation the binary relation that is used to forms the arcs
     *                 in the graph.
     * @param above the number of levels above the given term in the graph
     * @param below the number of levels below the given term in the graph
     * @param indentChars a String of characters to be used for indenting the terms
     */
    public ArrayList createGraph(KB kb, String term, String relation, 
                                 int above, int below, String indentChars, String language) {

        graphsize = 0;
        ArrayList result = new ArrayList();  // a list of Strings
        HashSet checkAbove = new HashSet();
        HashSet checkBelow = new HashSet();
        result.add(createColumnHeader());
        result.addAll(createGraphBody(kb,checkAbove,term,relation,above,0,indentChars,above,true,language));
        result.addAll(createGraphBody(kb,checkBelow,term,relation,0,below,indentChars,above,false,language));
        if (graphsize == 100)
            result.add("<P>Graph size limited to 100 terms.<P>\n");
        return result;
    }

    /** *************************************************************
     * The main body for createGraph(). Creates an indented,
     * HTML-formatted display of terms.
     * @param check collects all the terms added to the graph so
     *              far, which is used to prevent cycles
     */
    private ArrayList createGraphBody(KB kb, Set check, String term, String relation, 
                                      int above, int below, String indentChars,int level, 
                                      boolean show, String language) {

        System.out.println("ENTER Graph.createGraphBody(" + kb.name + ", " + check + ", "
                           + term + ", " + relation + ", " + above + ", " + below + ", "
                           + indentChars + ", " + level + ", " + show + ")");
        ArrayList result = new ArrayList();
        int graphMax = Integer.valueOf(KBmanager.getMgr().getPref("adminBrowserLimit")).intValue();
        if (!check.contains(term) && graphsize < graphMax) {
            if (above > 0) {
                ArrayList stmtAbove = new ArrayList();
                if (!DB.emptyString(relation) && relation.equals("all"))
                    stmtAbove = kb.ask("arg",1,term);
                else 
                    stmtAbove = kb.askWithRestriction(0,relation,1,term);
                for (int i = 0; i < stmtAbove.size(); i++) {
                    Formula f = (Formula) stmtAbove.get(i);
                    String newTerm = f.getArgument(2);
                    if (!newTerm.equals(term) && !f.sourceFile.endsWith("_Cache.kif"))
                        result.addAll(createGraphBody(kb,check,newTerm,relation,above-1,0,indentChars,level-1,true,language));		    
                    check.add(term);
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
            if (show) {
                graphsize++;
                if (graphsize < 100)                 
                    result.add(createGraphEntry(kb,prefix.toString(),kbHref,term,language));
            }
            if (below > 0) {
                ArrayList stmtBelow = kb.askWithRestriction(0,relation,2,term);
                for (int i = 0; i < stmtBelow.size(); i++) {
                    Formula f = (Formula) stmtBelow.get(i);
                    String newTerm = f.getArgument(1);
                    if (!newTerm.equals(term) && !f.sourceFile.endsWith("_Cache.kif"))
                        result.addAll(createGraphBody(kb,check,newTerm,relation,0,below-1,indentChars,level+1,true,language));            
                    check.add(term);
                }
            }
        }
        return result;
    }

    /** *************************************************************
     * Create a ArrayList with a set of terms comprising a hierarchy
     * in a format suitable for GraphViz' input format
     * http://www.graphviz.org/
     * Generate a GIF from the .dot output with a command like
     *  dot SUMO-graph.dot -Tgif > graph.gif
     *
     * @param kb the knowledge base being graphed
     * @param term the term in the KB being graphed
     * @param relation the binary relation that is used to forms the arcs
     *                 in the graph.
     */
    public boolean createDotGraph(KB kb, String term, String relation, String fname) throws IOException {

        
        String[] params = {"kb=" + kb, "term=" + term, "relation=" + relation, "fname=" + fname};
        System.out.println("INFO in Graph.createDotGraph():" + params);

        FileWriter fw = null;
        PrintWriter pw = null; 
        String filename = KBmanager.getMgr().getPref("graphDir") + File.separator + fname;
        logger.finer("Full filename = " + filename);
        try {
            fw = new FileWriter(filename + ".dot");
            pw = new PrintWriter(fw);
            HashSet result = new HashSet();
            HashSet start = new HashSet();
            HashSet checked = new HashSet();
            start.add(term);
            createDotGraphBody(kb,start,checked,relation,true,result);
            start.add(term);
            createDotGraphBody(kb,start,checked,relation,false,result);
            pw.println("digraph G {");
            pw.println("  rankdir=LR");
            Iterator it = result.iterator();
            while (it.hasNext()) {
                String s = (String) it.next();
                pw.println(s);
            }
            pw.println("}");
            pw.close();
            fw.close();
            logger.info(filename + ".dot created.");
            
            String command = "dot " + filename + ".dot -Tgif";
            logger.finer("command = " + command);
            
            Process proc = Runtime.getRuntime().exec(command);
            BufferedInputStream img = new BufferedInputStream(proc.getInputStream());            
            RenderedImage image = ImageIO.read(img);
            
            File file = new File(filename + ".gif");
            ImageIO.write(image, "gif", file);
            logger.info(filename + ".gif created.");
            
            return true;
        }
        catch (java.io.IOException e) {
            throw new IOException("Error writing file " + filename + "\n" + e.getMessage());
        }
        finally {
            if (pw != null) 
                pw.close();
            if (fw != null)
                fw.close();            
        }
    }

    /** *************************************************************
     * The main body for createGraph().
     */
    private void createDotGraphBody(KB kb, HashSet startSet, HashSet checkedSet, 
                                                String relation, boolean upSearch, HashSet result) {

        while (startSet.size() > 0) {
            Iterator it = startSet.iterator();
            String term = (String) it.next();
                        
            boolean removed = startSet.remove(term);
            if (!removed) 
                System.out.println("Error in createDotGraphBody(): " + term + " not removed");
            ArrayList stmts;
            String newTerm;
            if (upSearch) 
                stmts = kb.askWithRestriction(0,relation,1,term);
            else 
                stmts = kb.askWithRestriction(0,relation,2,term);
            
            for (int i = 0; i < stmts.size(); i++) {
                Formula f = (Formula) stmts.get(i);
                String parent = f.getArgument(2); 
                String child = f.getArgument(1);                      
                String s = "  \"" + parent + "\" -> \"" + child + "\";";
                result.add(s);
                checkedSet.add(term);
                if (upSearch)
                    startSet.add(parent);
                else
                    startSet.add(child);                
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
        } catch (Exception ex ) {
            System.out.println(ex.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB("SUMO");

        Graph g = new Graph();
        g.createBoundedSizeGraph(kb, "Process", "subclass", 50, "  ", "EnglishLanguage");
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
