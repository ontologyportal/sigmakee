<%
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

 show = new StringBuffer();       // Variable to contain the HTML page generated.
 String kbHref = null;
 String htmlDivider = "<table ALIGN='LEFT' WIDTH='50%'><tr><TD BGCOLOR='#A8BACF'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR><BR>\n";
 kbName = null;   // Name of the knowledge base
 kb = null;   // The knowledge base object.
 String formattedFormula = null;

 term = request.getParameter("term");
 language = request.getParameter("lang");
 if ( ! Formula.isNonEmptyString(language) ) {
    language = "en";
 }
 kbName = request.getParameter("kb");
 kb = KBmanager.getMgr().getKB(kbName);
 if (kb == null)
     response.sendRedirect("login.html");
 if (kb != null && kb.containsTerm(term))
    show.append("<title>Sigma KEE - " + term + "</title></HEAD>\n<BODY BGCOLOR=\"#FFFFFF\">\n");
 Map theMap = null;     // Map of natural language format strings.

 String hostname = KBmanager.getMgr().getPref("hostname");
 if (hostname == null)
    hostname = "localhost";
 String port = KBmanager.getMgr().getPref("port");
 if (port == null)
    port = "8080";
 kbHref = "http://" + hostname + ":" + port + "/sigma/" + parentPage + "?lang=" + language + "&simple=yes&kb=" + kbName;

 if (kb != null && (term == null || term.equals(""))) {       // Show statistics only when no term is specified.
    show.append("<b>Knowledge base statistics: </b><br><table>");
    show.append("<tr bgcolor=#eeeeee><td>Total Terms</td><td>Total Axioms</td><td>Total Rules</td><tr><tr align='center'>\n");
    show.append("<td>  " + kb.getCountTerms());
    show.append("</td><td> " + kb.getCountAxioms());
    show.append("</td><td> " + kb.getCountRules());
    show.append("</td><tr> </table><P>\n");
    show.append("Relations: " + kb.getCountRelations());
    show.append("<P>\n");
 }

 else if (kb != null && !kb.containsTerm(term)) {           // Show the alphabetic neighbors of a term
                                                           // that is not present in the KB.
    ArrayList relations = kb.getNearestRelations(term);
    ArrayList nonRelations = kb.getNearestNonRelations(term);
    show.append(" <FONT face='Arial,helvetica' size=+3> <b> ");
    if (term != null)
        show.append(term);
    show.append("</b></FONT><br><br>");
    show.append("<TABLE><tr><td>");
    show.append("<TABLE>");

    for (int i = 0; i < 30; i++) {
        String relation = (String) relations.get(i);
        String nonRelation = (String) nonRelations.get(i);
        if (relation != "" || nonRelation != "") {
            if (i == 15) {
                show.append("<TR>\n");
                show.append("  <TD><A href='SimpleBrowse.jsp?kb="+ kbName +"&term=");
                show.append(   relation + "'>" + relation + "</A>" + "</TD><TD>&nbsp;&nbsp;</TD>\n");
                show.append("  <TD><A href='SimpleBrowse.jsp?kb="+ kbName +"&term=");
                show.append(   nonRelation + "'>" + nonRelation + "</A>" + "</TD>\n");
                show.append("</TR>\n");
                show.append("<TR><TD><FONT SIZE=4 COLOR=\"RED\">" + term + " </FONT></TD><TD>&nbsp;&nbsp;</TD>\n");
                show.append("<TD><FONT SIZE=4 COLOR=\"RED\">" + term + " </FONT></TD></TR>\n");
            }
            else {
                show.append("<TR>\n");
                show.append("  <TD><A href='SimpleBrowse.jsp?kb="+ kbName +"&term=");
                show.append(   relation + "'>" + relation + "</A>" + "</TD><TD>&nbsp;&nbsp;</TD>\n");
                show.append("  <TD><A href='SimpleBrowse.jsp?kb="+ kbName +"&term=");
                show.append(   nonRelation + "'>" + nonRelation + "</A>" + "</TD>\n");
                show.append("</TR>\n");
            }
        }
    }
    show.append("</TABLE></td>");
    /*
    WordNet.initOnce();
    TreeMap tm = WordNet.wn.getSensesFromWord(term);
    if (tm != null) {
        show.append("<td width='10%'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></td>");
        show.append("<td width='40%' valign=top><small>");
        show.append(WordNetUtilities.formatWordsList(tm,kbName));
        show.append("</small></td>");
    }
    */
    show.append("</td></TABLE>");
 }
 else if (kb != null && kb.containsTerm(term)) {            // Build the HTML format for all the formulas in
                                                           // which the given term appears.
    show.append("<title>Sigma KEE - " + term + "</title>\n");
    ArrayList forms;
    show.append("<table width='95%'><tr><td width='50%'><FONT face='Arial,helvetica' size=+3><b>");
    if (term != null) {
        term = term.intern();
        //show.append(term);
        show.append("</b></FONT>");
    	if ( Character.isLowerCase(term.charAt(0)) || term.endsWith("Fn") ) {
    	    Map fm = kb.getFormatMap(language);
    	    String fmValue = null;
    	    if ( fm != null ) { fmValue = (String) fm.get(term); }
    	    if ( fmValue == null ) {
                System.out.println( "INFO in SimpleBrowse.jsp: No format map entry for \""
    				    + term
    				    + "\" in language "
    				    + language );
    	    }
    	}
    	else {
    	    Map tfm = kb.getTermFormatMap(language);
    	    String tfmValue = null;
    	    if ( tfm != null ) { tfmValue = (String) tfm.get(term); }
    	    if ( tfmValue != null ) {
                //show.append("(" + tfmValue + ")");
    	    }
    	    else {
                System.out.println( "INFO in SimpleBrowse.jsp: No term format map entry for \""
    				    + term
    				    + "\" in language "
    				    + language );
    	    }
    	}
        ArrayList pictures = kb.askWithRestriction(0,"externalImage",1,term);
        if (pictures != null && pictures.size() > 0) {
            show.append("<br>");
            for (int i = 0; i < pictures.size(); i++) {
                Formula f = (Formula) pictures.get(i);
                String url = f.getArgument(2);
                if (url.startsWith("\"http://upload.wikimedia.org")) {
                    String imageFile = url.substring(url.lastIndexOf("/")+1,url.length()-1);
                    show.append( "<a href=\"http://simple.wikipedia.org/wiki/Image:" 
                                 + imageFile + "\"><img width=100 src=" + url + "></a>" );
                }
                else {
                    show.append("<a href=" + url + "><img width=100 src=" + url + "></a>");
                }
            }
        }
        show.append("</td>");
        /* WordNet.initOnce();
        TreeMap tm = WordNet.wn.getWordsFromTerm(term);
        if (tm != null) {
            show.append("<td width='10%'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></td>");
            show.append("<td width='40%'><small>");
            show.append(WordNetUtilities.formatWords(tm,kbName));
            show.append("</small></td>");
        }
        else
            System.out.println("INFO in SimpleBrowse.jsp: No synsets for term " + term);
            */
        show.append("</tr></table>\n");
    }
    else {
        show.append ("</b></FONT></td></tr></table>\n");
    }
    show.append(DocGen.createPage(kb,kbHref,term));
    show.append("<table ALIGN='LEFT' WIDTH='50%'><tr><TD BGCOLOR='#A8BACF'>" +
                "<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr>" +
                "</table><BR>\n");

    if (!parentPage.equals("TreeView.jsp")) {
        show.append("\n<P><P><small><a href=\"http://" + hostname + ":" + port + "/sigma/TreeView.jsp" + 
                    "?lang=" + language + "&simple=yes&kb=" + kbName + 
                    "&term=" + term + "\">Show simplified definition with tree view</a></small>\n");
    }
    show.append("\n<P><P><small><a href=\"http://" + hostname + ":" + port + "/sigma/Browse.jsp" + 
                "?lang=" + language + "&kb=" + kbName + "&simple=no" + 
                "&term=" + term + "\">Show full definition (without tree view)</a></small><br>\n");
    show.append("\n<P><P><small><a href=\"http://" + hostname + ":" + port + "/sigma/TreeView.jsp" + 
                "?lang=" + language + "&kb=" + kbName + "&simple=no" + 
                "&term=" + term + "\">Show full definition (with tree view)</a></small><br>\n");
 }
%>
