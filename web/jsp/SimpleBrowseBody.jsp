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
 kbName = null;   // Name of the knowledge base
 kb = null;   // The knowledge base object.
 String formattedFormula = null;

 term = request.getParameter("term");
 language = request.getParameter("lang");
 language = HTMLformatter.processLanguage(language);
 
 kbName = request.getParameter("kb");
 kb = null;
 if (Formula.isNonEmptyString(kbName)) {
     kb = KBmanager.getMgr().getKB(kbName);
     if (kb != null) 
         TaxoModel.kbName = kbName;     
 }
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
 HTMLformatter.kbHref = "http://" + hostname + ":" + port + "/sigma/" + parentPage + "?lang=" + language + "&simple=yes&kb=" + kbName;

 if (kb != null && (term == null || term.equals("")))        // Show statistics only when no term is specified.
     show.append(HTMLformatter.showStatistics(kb));
 else if (kb != null && !kb.containsTerm(term)) {           // Show the alphabetic neighbors of a term                                                           
    show.append(HTMLformatter.showNeighborTerms(kb,term));
    show.append("</td></TABLE>");
 }
 else if (kb != null && kb.containsTerm(term)) {                // Build the HTML format for all the formulas in                                                           
    show.append("<title>Sigma KEE - " + term + "</title>\n");   // which the given term appears.
    ArrayList forms;
    show.append("<table width='95%'><tr><td width='50%'><FONT face='Arial,helvetica' size=+3><b>");
    if (term != null) {
        term = term.intern();
        //show.append(term);
        show.append("</b></FONT>");
    	if (Character.isLowerCase(term.charAt(0)) || term.endsWith("Fn")) {
    	    Map fm = kb.getFormatMap(language);
    	    String fmValue = null;
    	    if (fm != null) 
                fmValue = (String) fm.get(term);
    	    if (fmValue == null) 
                System.out.println("INFO in SimpleBrowseBody.jsp: No format map entry for \"" +
                                   term + "\" in language " + language);    	    
    	}
    	else {
    	    Map tfm = kb.getTermFormatMap(language);
    	    String tfmValue = null;
    	    if (tfm != null) 
                tfmValue = (String) tfm.get(term); 
    	    if (tfmValue == null)
                System.out.println("INFO in SimpleBrowseBody.jsp: No term format map entry for \"" +
                                   term + "\" in language " + language);    	    
    	}
        show.append(HTMLformatter.showPictures(kb,term));
        show.append("</td>");
        show.append("</tr></table>\n");
    }
    else 
        show.append ("</b></FONT></td></tr></table>\n");  
    int limit = Integer.decode(KBmanager.getMgr().getPref("userBrowserLimit")).intValue();
    if (KBmanager.getMgr().getPref("userName") != null && 
        KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin")) {
        limit = Integer.decode(KBmanager.getMgr().getPref("adminBrowserLimit")).intValue();
    }
    show.append(DocGen.createPage(kb,HTMLformatter.kbHref,term,limit));
    show.append("<P><table ALIGN='LEFT' WIDTH='50%'><tr><TD BGCOLOR='#A8BACF'>" +
                "<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr>" +
                "</table><BR>\n");

    if (!parentPage.equals("TreeView.jsp")) 
        show.append("\n<small><a href=\"http://" + hostname + ":" + port + "/sigma/TreeView.jsp" + 
                    "?lang=" + language + "&simple=yes&kb=" + kbName + 
                    "&term=" + term + "\">Show simplified definition with tree view</a></small><br>\n");
    
    show.append("\n<small><a href=\"http://" + hostname + ":" + port + "/sigma/Browse.jsp" + 
                "?lang=" + language + "&kb=" + kbName + "&simple=no" + 
                "&term=" + term + "\">Show full definition (without tree view)</a></small><br>\n");
    show.append("\n<small><a href=\"http://" + hostname + ":" + port + "/sigma/TreeView.jsp" + 
                "?lang=" + language + "&kb=" + kbName + "&simple=no" + 
                "&term=" + term + "\">Show full definition (with tree view)</a></small><br>\n");
 }
%>
