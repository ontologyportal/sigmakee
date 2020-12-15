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

import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import javax.imageio.ImageIO;

import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.StringUtil;

/** Handle operations for creating a graphical representation of partial
 *  ordering relations.  Supports Graph.jsp.  */
public class Graph {
	
	  // a limit counter to prevent pathologically large graphs
    private int graphsize = 0;                    
    
      // A map of the fields to display in the graph in addition to the 
      // indented term name with option names as keys and "yes", "no" as values.
    public TreeMap<String,String> columnList = new TreeMap<String,String>();

    public static String indent = "&nbsp;&nbsp;&nbsp;&nbsp;";

    public TreeSet<String> errors = new TreeSet<>();
    
    /** *************************************************************
     */
    public Graph() {
  	
        columnList.put("documentation","yes");
        columnList.put("direct-children","yes");
        columnList.put("graph","yes");
    }

    /** *************************************************************
     *  @return in a String a count of the number of "children" which consists of
     *  instance, subclass, subrelation and subAttribute links.
     */
    private String generateChildrenColumn(KB kb, String term) {

        // probably should find all instances of PartialOrderingRelation
        ArrayList<String> relations = new ArrayList<String>();
        relations.add("instance");
        relations.add("subclass");
        relations.add("subrelation");
        relations.add("subAttribute");

        int count = 0;
        for (int i = 0; i < relations.size(); i++) {
            ArrayList<Formula> children = kb.askWithRestriction(0,relations.get(i),2,term);
            if (children != null) 
                count = count + children.size();
        }
        if (count > 0)
            return Integer.toString(count);       
        else
            return ".";
    }

    /** *************************************************************
     *  @return a String URL link from the term to the graph page for the term,
     *  given the href input that already includes the kb and lang
     *  parameters.
     */
    private String generateGraphLink(KB kb, String term, String href) {

        String result = href.replace("Browse.jsp","Graph.jsp");
        return "<a href=\"" + result + "&term=" + term + "&relation=subclass\">^</a>";
    }

    /** *************************************************************
     * @return an HTML-formatted String consisting of the first 100 
     * characters of the documentation string for the term parameter
     */
    private String generateDocumentationColumn(KB kb, String term, String href, String language) {

        String docString = "";
        ArrayList<Formula> docStmts = kb.askWithRestriction(0,"documentation",1,term);
        if (docStmts.size() > 0) {
            Formula doc = (Formula) docStmts.get(0);
            docString = doc.getStringArgument(3);
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
     * @return a count of the number of elements in columnList with the value
     * "yes".
     */
    public int columnCount() {

        int counter = 0;
        Iterator<String> it = columnList.keySet().iterator();
        while (it.hasNext()) {
            String col = it.next();
            String val = columnList.get(col);
            if (val.equals("yes")) 
                counter++;
        }
        return counter;
    }

    /** *************************************************************
     * @return in a String a <table> header that shows each of the columns to be
     * displayed in the HTML-based graph.
     */
    private String createColumnHeader() {

        StringBuffer result = new StringBuffer();
        result.append("<tr bgcolor=#EEEEEE><td></td>");             // a blank column for the indented terms
        Iterator<String> it = columnList.keySet().iterator();
        while (it.hasNext()) {
            String col = it.next();
            String val = columnList.get(col);
            if (val.equals("yes")) 
                result.append("<td>" + col + "</td>");            
        }
        result.append("</tr>");
        return result.toString();
    }

    /** *************************************************************
     * @return in a String a <table> header that shows each of the columns to be
     * displayed in the HTML-based graph.
     */
    private String createGraphEntry(KB kb, String prefix, String kbHref, String term, String language) {

        String ital = "";
        String italEnd = "";
        if (kb.isInstance(term)) { // italicize instances
            ital = "<i>";
            italEnd = "</i>";
        }
        StringBuffer result = new StringBuffer();
        result.append("<tr>");
        String formattedTerm = "<a href=\"" + kbHref + "&term=" + term + "\">" + term + "</a>";
        result.append("<td>" + prefix + ital + formattedTerm + italEnd + "</td>");
        Iterator<String> it = columnList.keySet().iterator();
        while (it.hasNext()) {
            String col = it.next();
            String val = columnList.get(col);
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
     * relation. creatGraphBody() does most of the work.
     */
    public LinkedHashSet<String> createBoundedSizeGraph(KB kb, String term, String relation,
                                        int size, boolean instances, String language) {

        LinkedHashSet<String> result = new LinkedHashSet<String>();
        LinkedHashSet<String> oldresult = new LinkedHashSet<String>();
        int above = 1;
        int below = 1;
        int oldlimit = -1;
        while ((result.size() < size) && (result.size() != oldlimit)) {
            oldlimit = result.size();
            oldresult = result;
            HashSet<String> checkAbove = new HashSet<String>();
            HashSet<String> checkBelow = new HashSet<String>();
            result = createGraphBody(kb,checkAbove,term,relation,above,0,above,true,instances,language);
            result.addAll(createGraphBody(kb,checkBelow,term,relation,0,below,above,false,instances,language));
            above++;
            below++;
        }
        return oldresult;
    }

    /** *************************************************************
     * Create an ArrayList with a set of terms comprising a hierarchy
     * Each term String will be prefixed with an appropriate number of
     * indentChars. @see creatGraphBody() does most of the work.
     *
     * @param kb the knowledge base being graphed
     * @param term the term in the KB being graphed
     * @param relation the binary relation that is used to forms the arcs
     *                 in the graph.
     * @param above the number of levels above the given term in the graph
     * @param below the number of levels below the given term in the graph
     * @param instances whether to display instances below subclass relations
     */
    public LinkedHashSet<String> createGraph(KB kb, String term, String relation,
                                 int above, int below, int termLimit, boolean instances, String language) {

        graphsize = 0;
        LinkedHashSet<String> result = new LinkedHashSet<String>();  // a list of Strings
        HashSet<String> checkAbove = new HashSet<String>();
        HashSet<String> checkBelow = new HashSet<String>();
        result.add(createColumnHeader());
        result.addAll(createGraphBody(kb,checkAbove,term,relation,above,0,above,true,instances,language));
        result.addAll(createGraphBody(kb,checkBelow,term,relation,0,below,above,false,instances,language));
        if (graphsize == 100)
            result.add("<P>Graph size limited to 100 terms.<P>\n");
        return result;
    }

    /** *************************************************************
     * The main body for @see createGraph(). Creates an indented,
     * HTML-formatted display of terms.
     * @param check collects all the terms added to the graph so
     *              far, which is used to prevent cycles
     */
    private LinkedHashSet<String> createGraphBody(KB kb, Set<String> check, String term, String relation,
                                      int above, int below, int level,
                                      boolean show, boolean instances, String language) {

        LinkedHashSet<String> result = new LinkedHashSet<String>();
        int graphMax = Integer.valueOf(KBmanager.getMgr().getPref("adminBrowserLimit")).intValue();
        if (!check.contains(term) && graphsize < graphMax) {
            if (above > 0) {
                ArrayList<Formula> stmtAbove = new ArrayList<Formula>();
                if (!DB.emptyString(relation) && relation.equals("all"))
                    stmtAbove = kb.ask("arg",1,term);
                else 
                    stmtAbove = kb.askWithRestriction(0,relation,1,term);
                for (int i = 0; i < stmtAbove.size(); i++) {
                    Formula f = stmtAbove.get(i);
                    String newTerm = f.getStringArgument(2);
                    if (!newTerm.equals(term) && !KButilities.isCacheFile(f.sourceFile))
                        result.addAll(createGraphBody(kb,check,newTerm,relation,above-1,0,level-1,true,instances,language));
                    check.add(term);
                }
            }

            StringBuffer prefix = new StringBuffer();
            for (int i = 0; i < level; i++)
                prefix = prefix.append(indent);

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
                else {
                    String endStr = "<tr><td><b>Truncating graph at 100 terms</b><P></td></tr>\n";
                    if (!result.contains(endStr))
                        result.add(endStr);
                    return result;
                }
            }
            if (below > 0) {
                ArrayList<Formula> stmtBelow = kb.askWithRestriction(0,relation,2,term);
                for (int i = 0; i < stmtBelow.size(); i++) {
                    Formula f = stmtBelow.get(i);
                    String newTerm = f.getStringArgument(1);
                    if (!newTerm.equals(term) && !KButilities.isCacheFile(f.sourceFile))
                        result.addAll(createGraphBody(kb,check,newTerm,relation,0,below-1,level+1,true,instances,language));
                    check.add(term);
                }
                if (instances && (stmtBelow == null || stmtBelow.size() == 0) && relation.equals("subclass")) {
                    stmtBelow = kb.askWithRestriction(0,"instance",2,term);
                    for (int i = 0; i < stmtBelow.size(); i++) {
                        Formula f = stmtBelow.get(i);
                        String newTerm = f.getStringArgument(1);
                        if (!newTerm.equals(term) && !KButilities.isCacheFile(f.sourceFile))
                            result.addAll(createGraphBody(kb,check,newTerm,relation,0,below-1,level+1,true,instances,language));
                        check.add(term);
                    }
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
    public boolean createDotGraph(KB kb, String term, String relation, int above, int below,
                                  int limitInt, String fname, String fileRestrict) throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null;
        String sep = File.separator;
        String dir = System.getenv("CATALINA_HOME") + sep + "webapps" +
                sep + "sigma" + sep + "graph";
        String filename = System.getenv("CATALINA_HOME") + sep + "webapps" +
                sep + "sigma" + sep + "graph" + sep + fname;
        String graphVizDir = KBmanager.getMgr().getPref("graphVizDir");
        try {
            File dirfile = new File(dir);
            if (!dirfile.exists())
                dirfile.mkdir();
            fw = new FileWriter(filename + ".dot");
            System.out.println("Graph.createGraphBody(): creating file at " + filename);
            pw = new PrintWriter(fw);
            HashSet<String> result = new HashSet<String>();
            HashSet<String> start = new HashSet<String>();
            HashSet<String> checked = new HashSet<String>();
            start.add(term);
            if (relation.equals("all"))
                result = createDotGraphNetBody(kb, start, checked, limitInt, fileRestrict);
            else {
                result = createDotGraphBody(kb, start, checked, relation, above, below, true);
                start.add(term);
                result.addAll(createDotGraphBody(kb, start, checked, relation, above, below, false));
            }
            pw.println("digraph G {");
            pw.println("  rankdir=LR");
            Iterator<String> it = result.iterator();
            while (it.hasNext()) {
                String s = it.next();
                pw.println(s);
            }
            pw.println("}");
            pw.close();
            fw.close();
            
            String command = graphVizDir + File.separator + "dot " + filename + ".dot -Tgif";            
            Process proc = Runtime.getRuntime().exec(command);
            System.out.println("Graph.createDotGraph(): exec command: " + command);
            BufferedInputStream img = new BufferedInputStream(proc.getInputStream());            
            RenderedImage image = ImageIO.read(img);            
            File file = new File(filename + ".gif");
            ImageIO.write(image, "gif", file);
            System.out.println("Graph.createDotGraph(): write image file: " + file);
            return true;
        }
        catch (java.io.IOException e) {
            String err = "Error writing file " + filename + ".dot\n" + e.getMessage();
            errors.add(err);
            throw new IOException(err);
        }
        finally {
            if (pw != null) pw.close();
            if (fw != null) fw.close();            
        }
    }

    /** *************************************************************
     * The main body for createDotGraph().
     */
    private boolean rejectedTerm(String s) {

        if (StringUtil.emptyString(s))
            return true;
        if (Formula.isLogicalOperator(s) || Formula.isMathFunction(s) ||
                Formula.isComparisonOperator(s) || Formula.DOC_PREDICATES.contains(s) ||
                StringUtil.isNumeric(s) || Formula.DEFN_PREDICATES.contains(s) ||
                StringUtil.isQuotedString(s))
            return true;
        return false;
    }

    /** *************************************************************
     * The main body for createDotGraph() when no relation is specified.
     * Don't graph math, logical operators or documentation relations
     */
    private HashSet<String> createDotGraphNetBody(KB kb, HashSet<String> startSet, HashSet<String> checkedSet,
                                               int size, String fileRestrict) {

        if (!StringUtil.emptyString(fileRestrict) && !kb.containsFile(fileRestrict)) {
            System.out.println("Error in createDotGraphNetBody(): no such file: " + fileRestrict);
            return null;
        }
        System.out.println("createDotGraphNetBody(): start set: " + startSet);
        HashSet<String> result = new HashSet<String>();
        HashSet<String> newStartSet = new HashSet<String>();
        while (startSet.size() > 0) {
            Iterator<String> it = startSet.iterator();
            String term = it.next();
            System.out.println("createDotGraphNetBody(): checking term: " + term);
            boolean removed = startSet.remove(term);
            if (!removed) {
                String err = "Error in Graph.createDotGraphNetBody(): " + term + " not removed";
                errors.add(err);
                System.out.println(err);
            }
            if (StringUtil.isQuotedString(term) || Formula.isLogicalOperator(term))
                continue;
            ArrayList<Formula> stmts;

            stmts = kb.ask("arg",1, term);
            for (int i = 0; i < stmts.size(); i++) {
                Formula f = stmts.get(i);
                if (f.isCached())
                    continue;
                else {
                    String parent = f.getStringArgument(2);
                    if (rejectedTerm(parent))
                        continue;
                    if (!StringUtil.emptyString(fileRestrict) && !FileUtil.noPath(f.getSourceFile()).equals(fileRestrict))
                        continue;
                    String rel = f.getStringArgument(0);
                    if (Formula.DOC_PREDICATES.contains(rel))
                        continue;
                    String link = "[ label = \"" + rel + "\" ]";
                    String arrow = " -> ";
                    String s = "  \"" + parent + "\"" + arrow + "\"" + term + "\" " + link + ";";
                    System.out.println("createDotGraphNetBody(): result in adding parents: " + s);
                    graphsize++;
                    if (graphsize < size)
                        result.add(s);
                    else
                        return result;
                    checkedSet.add(term);
                    if (!checkedSet.contains(parent))
                        newStartSet.add(parent);
                }
            }

            stmts = kb.ask("arg",2, term);
            for (int i = 0; i < stmts.size(); i++) {
                Formula f = stmts.get(i);
                if (f.isCached())
                    continue;
                else {
                    String parent = f.getStringArgument(1);
                    if (rejectedTerm(parent))
                        continue;
                    if (!StringUtil.emptyString(fileRestrict) && !FileUtil.noPath(f.getSourceFile()).equals(fileRestrict))
                        continue;
                    String rel = f.getStringArgument(0);
                    if (Formula.DOC_PREDICATES.contains(rel))
                        continue;
                    String link = "[ label = \"" + rel + "\" ]";
                    String arrow = " -> ";
                    String s = "  \"" + term + "\"" + arrow + "\"" + parent + "\" " + link + ";";
                    System.out.println("createDotGraphNetBody(): result in adding children: " + s);
                    graphsize++;
                    if (graphsize < size)
                        result.add(s);
                    else
                        return result;
                    checkedSet.add(term);
                    if (!checkedSet.contains(parent))
                        newStartSet.add(parent);
                }
            }

            stmts = kb.ask("ant",0,term);
            ArrayList<Formula> cons = kb.ask("ant",0,term);
            if (cons != null)
                stmts.addAll(cons);
            for (int i = 0; i < stmts.size(); i++) {
                Formula f = stmts.get(i);
                if (f.isCached())
                    continue;
                else {
                    Set<String> terms = f.collectTerms();
                    for (String s : terms) {
                        if (rejectedTerm(s) || term.equals(s))
                            continue;
                        if (!StringUtil.emptyString(fileRestrict) && !FileUtil.noPath(f.getSourceFile()).equals(fileRestrict))
                            continue;
                        String rel = "link";
                        String link = "[ dir=none, label = \"" + rel + "\" ]";
                        String arrow = " -> ";
                        String str = "  \"" + term + "\"" + arrow + "\"" + s + "\" " + link + ";";
                        graphsize++;
                        if (graphsize < size)
                            result.add(str);
                        else
                            return result;
                        checkedSet.add(term);
                        if (!checkedSet.contains(s))
                            newStartSet.add(s);
                    }
                }
            }
            result.addAll(createDotGraphNetBody(kb,newStartSet,checkedSet,size,fileRestrict));
        }
        return result;
    }

    /** *************************************************************
     * The main body for createDotGraph().
     */
    private HashSet<String> createDotGraphBody(KB kb, HashSet<String> startSet, HashSet<String> checkedSet, 
                                   String relation, int above, int below, boolean upSearch) {

        System.out.println("createDotGraph(): start set: " + startSet);
        HashSet<String> result = new HashSet<String>();
        HashSet<String> newStartSet = new HashSet<String>();
        newStartSet.addAll(startSet);
        
        if (upSearch) {
            above--;
            if (above < 0) return result;
        }
        else {
            below--;
            if (below < 0) return result;
        }
        while (startSet.size() > 0) {
            Iterator<String> it = startSet.iterator();
            String term = (String) it.next();
            System.out.println("createDotGraph(): checking term: " + term);
            boolean removed = startSet.remove(term);
            if (!removed) {
                String err = "Error in Graph.createDotGraphBody(): " + term + " not removed";
                errors.add(err);
                System.out.println(err);
            }
            if (StringUtil.isQuotedString(term))
                continue;
            ArrayList<Formula> stmts;
            if (upSearch) {
                stmts = kb.askWithRestriction(0, relation, 1, term);
            }
            else {
                stmts = kb.askWithRestriction(0, relation, 2, term);
            }

            for (int i = 0; i < stmts.size(); i++) {
                Formula f = stmts.get(i);
                if (f.isCached())
                    continue;
                else {
                    String child = f.getStringArgument(1);
                    String parent = f.getStringArgument(2);
                    String link = "";
                    String arrow = "->";
                    String rel = f.getStringArgument(0);
                    if (relation.equals("all")) {
                        link = "[ label = \"" + rel + "\" ]";
                        arrow = "--";
                    }
                    String s = "  \"" + parent + "\"" + arrow + "\"" + child + "\" " + link + ";";
                    graphsize++;
                    if (graphsize < 100)
                        result.add(s);
                    else
                        return result;
                    checkedSet.add(term);
                    if (upSearch) {
                        newStartSet.add(parent);
                    }
                    else {
                        newStartSet.add(child);
                    }
                }
                result.addAll(createDotGraphBody(kb,newStartSet,checkedSet,relation,above,below,upSearch));
            } 
        }
        return result;
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("Graphing");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -g <term> <rel> - create a dot graph file with a term and relation");
    }

    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {

        System.out.println("INFO in Graph.main()");
        if (args != null && args.length > 1 && args[0].equals("-h")) {
            showHelp();
        }
        KBmanager.getMgr().initializeOnce();
        if (args != null && args.length > 2 && args[0].equals("-g")) {
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            Graph g = new Graph();
            String start = args[1];
            String relation = args[2];
            HashSet<String> checked = new HashSet<String>();
            HashSet<String> startSet = new HashSet<String>();
            String fileRestrict = "";
            startSet.add(start);
            try {
                    g.createDotGraph(kb, start, relation, 2, 2, 100, "graph.txt", fileRestrict);
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
                e.getStackTrace();
            }
        }
        else
            showHelp();
    }
}
