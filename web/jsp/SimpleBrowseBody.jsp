<%
/* This code is copyright Articulate Software (c) 2003-2011.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://github.com/ontologyportal
*/
 show = new StringBuilder();       // Variable to contain the HTML page generated.
 String formattedFormula = null;
 term = request.getParameter("term");
 nonRelTerm = request.getParameter("nonrelation");
 relTerm = request.getParameter("relation");

 Map theMap = null;     // Map of natural language format strings.

 HTMLformatter.kbHref = HTMLformatter.createHrefStart() + "/sigma/" + parentPage + "?lang=" + language + "&simple=yes&kb=" + kbName;

 if (kb != null && StringUtil.emptyString(term) && StringUtil.emptyString(relTerm) && StringUtil.emptyString(nonRelTerm))       // Show statistics only when no term is specified.
     show.append(HTMLformatter.showStatistics(kb));
 else if (kb != null && term != null && !kb.containsTerm(term)) {           // Show the alphabetic neighbors of a term
    show.append(HTMLformatter.showNeighborTerms(kb,term));
    show.append("</td></TABLE>");
 }
 else if ((kb != null) && (term == null) && (nonRelTerm != null) && (relTerm != null)) {
    show.append(HTMLformatter.showNeighborTerms(kb,nonRelTerm, relTerm));
    show.append("</td></table>");
 }
 else if (kb != null && kb.containsTerm(term)) {                // Build the HTML format for all the formulas in
    show.append("<title>Sigma KEE - " + term + "</title>\n");   // which the given term appears.
    List forms;
    show.append("<table width='95%'><tr><td width='50%'><FONT face='Arial,helvetica' size=+3><b>");
    Map<String, String> tfm = kb.getTermFormatMap(language);
    if (term != null) {
        term = term.intern();
        //show.append(term);
        show.append("</b></FONT>");
    	if (Character.isLowerCase(term.charAt(0)) || term.endsWith("Fn")) {
    	    String fmValue = null;
    	    if (tfm != null)
                fmValue = tfm.get(term);
    	    if (fmValue == null)
                System.out.println("INFO in SimpleBrowseBody.jsp: No format map entry for \"" +
                                   term + "\" in language " + language);
    	}
    	else {
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
    if (role != null && role.equalsIgnoreCase("admin")) {
        limit = Integer.decode(KBmanager.getMgr().getPref("adminBrowserLimit")).intValue();
    }

    String ontology = KBmanager.getMgr().getPref("sumokbname");
    String formatToken = kb.name;
    String defaultNS = language;
    DocGen gen = DocGen.getInstance();
    Map<String, Map<String, List<String>>> alphaList = gen.getAlphaList(kb); // tfm
    show.append(DocGen.getInstance(kb.name).createPage(kb,HTMLformatter.kbHref,term,alphaList,limit,defaultNS,formatToken));
    show.append("<P><table ALIGN='LEFT' WIDTH='50%'><tr><TD BGCOLOR='#A8BACF'>" +
                "<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr>" +
                "</table><BR>\n");

    if (!parentPage.equals("TreeView.jsp"))
        show.append("\n<small><a href=\"" + HTMLformatter.createHrefStart() + "/sigma/TreeView.jsp" +
                    "?lang=" + language + "&simple=yes&kb=" + kbName +
                    "&term=" + term + "\">Show simplified definition with tree view</a></small><br>\n");

    show.append("\n<small><a href=\"" + HTMLformatter.createHrefStart() + "/sigma/Browse.jsp" +
                "?lang=" + language + "&kb=" + kbName + "&simple=no" +
                "&term=" + term + "\">Show full definition (without tree view)</a></small><br>\n");
    show.append("\n<small><a href=\"" + HTMLformatter.createHrefStart() + "/sigma/TreeView.jsp" +
                "?lang=" + language + "&kb=" + kbName + "&simple=no" +
                "&term=" + term + "\">Show full definition (with tree view)</a></small><br>\n");
 }
%>
