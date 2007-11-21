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
August 9, Acapulco, Mexico. See also http://sigmakee.sourceforge.net
*/

 show = new StringBuffer();       // Variable to contain the HTML page generated.
 kbName = null;   // Name of the knowledge base
 kb = null;   // The knowledge base object.
 String formattedFormula = null;

 term = request.getParameter("term");
 language = request.getParameter("lang");
 if (!Formula.isNonEmptyString(language))
    language = "EnglishLanguage";
 HTMLformatter.language = language;
 kbName = request.getParameter("kb");
 if (Formula.isNonEmptyString(kbName)) {
     kb = KBmanager.getMgr().getKB(kbName);
     if (kb != null)
         TaxoModel.kbName = kbName;
 }
 if (kb == null)
     response.sendRedirect("login.html");
 Map theMap = null;     // Map of natural language format strings.

 String hostname = KBmanager.getMgr().getPref("hostname");
 if (hostname == null)
    hostname = "localhost";
 String port = KBmanager.getMgr().getPref("port");
 if (port == null)
    port = "8080";
 HTMLformatter.kbHref = "http://" + hostname + ":" + port + "/sigma/" + parentPage + "?lang=" + language + "&kb=" + kbName;

 if (kb != null && (term == null || term.equals("")))        // Show statistics only when no term is specified.
    show.append(HTMLformatter.showStatistics(kb));
 else if (kb != null && !kb.containsTerm(term)) {           // Show the alphabetic neighbors of a term 
    show.append(HTMLformatter.showNeighborTerms(kb,term));
    WordNet.initOnce();
    TreeMap tm = WordNet.wn.getSensesFromWord(term);
    if (tm != null) {
        show.append("<td width='10%'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></td>");
        show.append("<td width='40%' valign=top><small>");
        show.append(WordNetUtilities.formatWordsList(tm,kbName));
        show.append("</small></td>");
    }
    show.append("</td></TABLE>");
 }
 else if (kb != null && kb.containsTerm(term)) {                // Build the HTML format for all the formulas in                                                         
    show.append("<title>Sigma KEE - " + term + "</title>\n");   // which the given term appears.
    ArrayList forms;
    show.append("<table width='95%'><tr><td width='50%'><FONT face='Arial,helvetica' size=+3><b>");
    if (term != null) {
    	term = term.intern();
        show.append(term);
        show.append("</b></FONT>");
    	if (Character.isLowerCase(term.charAt(0)) || term.endsWith("Fn")) {
    	    Map fm = kb.getFormatMap(language);
    	    String fmValue = null;
    	    if (fm != null)
                fmValue = (String) fm.get(term); 
    	    if (fmValue == null)
                System.out.println("INFO in BrowseBody.jsp: No format map entry for \"" +
                                   term + "\" in language " + language);	   
    	}
    	else {
    	    Map tfm = kb.getTermFormatMap(language);
    	    String tfmValue = null;
    	    if (tfm != null)
                tfmValue = (String) tfm.get(term);
    	    if (tfmValue != null) 
                show.append("(" + tfmValue + ")");	    
    	    else
                System.out.println("INFO in BrowseBody.jsp: No term format map entry for \"" +
                                   term + "\" in language " + language);	   
    	}
        show.append(HTMLformatter.showPictures(kb,term));
        show.append("</td>");
        WordNet.initOnce();
        TreeMap tm = WordNet.wn.getWordsFromTerm(term);
        if (tm != null) {
            show.append("<td width='10%'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></td>");
            show.append("<td width='40%'><small>");
            show.append(WordNetUtilities.formatWords(tm,kbName));
            show.append("</small></td>");
        }
        else
            System.out.println("INFO in BrowseBody.jsp: No synsets for term " + term);
        show.append("</tr></table>\n");
    }
    else
        show.append ("</b></FONT></td></tr></table>\n");

    int limit = 25;
    if (KBmanager.getMgr().getPref("userName") != null && 
        KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin")) {
        limit = 200;
    }
    show.append(HTMLformatter.showFormulasLimit(kb,term,limit));
    forms = kb.ask("ant",0,term);
    show.append(HTMLformatter.browserSectionFormatLimit(forms,"antecedent", kb, language,limit));

    forms = kb.ask("cons",0,term);
    show.append(HTMLformatter.browserSectionFormatLimit(forms,"consequent", kb, language,limit));

    forms = kb.ask("stmt",0,term);
    show.append(HTMLformatter.browserSectionFormatLimit(forms,"statement", kb, language,limit));

    forms = kb.ask("arg",0,term);
    show.append(HTMLformatter.browserSectionFormatLimit(forms,"appearance as argument number 0", kb, language,limit));

    show.append("<P><table ALIGN='LEFT' WIDTH='50%'><tr><TD BGCOLOR='#A8BACF'>" +
                "<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr>" +
                "</table><BR>\n");
    if (!parentPage.equals("TreeView.jsp")) 
        show.append("\n<small><a href=\"http://" + hostname + ":" + port + "/sigma/TreeView.jsp" + 
                    "?lang=" + language + "&kb=" + kbName + 
                    "&term=" + term + "\">Show full definition with tree view</a></small><br>\n");
    
    show.append("\n<small><a href=\"http://" + hostname + ":" + port + "/sigma/SimpleBrowse.jsp" + 
                "?lang=" + language + "&kb=" + kbName + "&simple=yes" + 
                "&term=" + term + "\">Show simplified definition (without tree view)</a></small><br>\n");
    show.append("\n<small><a href=\"http://" + hostname + ":" + port + "/sigma/TreeView.jsp" + 
                "?lang=" + language + "&kb=" + kbName + "&simple=yes" + 
                "&term=" + term + "\">Show simplified definition (with tree view)</a></small><br>\n");
 }
%>
