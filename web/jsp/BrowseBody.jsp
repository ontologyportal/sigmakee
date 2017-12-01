<%
/** This code is copyright Teknowledge (c) 2003, Articulate Software (c) 2003-2017,
    Infosys (c) 2017-present.

    This software is released under the GNU Public License
    <http://www.gnu.org/copyleft/gpl.html>.

    Please cite the following article in any publication with references:

    Pease A., and Benzmüller C. (2013). Sigma: An Integrated Development Environment
    for Logical Theories. AI Communications 26, pp79-97.  See also
    http://github.com/ontologyportal
*/

 show = new StringBuffer();       // Variable to contain the HTML page generated.
 String formattedFormula = null;
 term = request.getParameter("term");
 nonRelTerm = request.getParameter("nonrelation");
 relTerm = request.getParameter("relation");
 Map theMap = null;     // Map of natural language format strings.
 KBPOS = request.getParameter("KBPOS");
 relREmatch = request.getParameter("relREmatch");
 nonRelREmatch = request.getParameter("nonRelREmatch");

 if (KBPOS == null && term == null)
    KBPOS = "1";
 else if (KBPOS == null && term != null)
 	KBPOS = kb.REswitch(term);
 
 HTMLformatter.kbHref = HTMLformatter.createHrefStart() + "/sigma/" + parentPage + "?lang=" + language + "&flang=" + flang + "&kb=" + kbName;

 if (kb != null && StringUtil.emptyString(term) && StringUtil.emptyString(relTerm) && StringUtil.emptyString(nonRelTerm) && StringUtil.emptyString(relREmatch))        // Show statistics only when no term is specified.
    show.append(HTMLformatter.showStatistics(kb));
 else if (kb != null && term != null && !kb.containsTerm(term) && KBPOS.equals("1")) {           // Show the alphabetic neighbors of a term
    show.append(HTMLformatter.showNeighborTerms(kb,term));
    TreeMap tm = WordNet.wn.getSenseKeysFromWord(term);
    if (tm != null) {
        show.append("<td width=\"10%\"><img src=\"pixmaps/1pixel.gif\" width=\"1\" height=\"1\" border=\"0\"></td>");
        show.append("<td width=\"40%\" valign=\"top\"><small>");
        show.append(WordNetUtilities.formatWordsList(tm,kbName));
        show.append("</small></td>");
    }
    show.append("</td></table>");
 }
 else if ((kb != null) && (term == null) && (nonRelTerm != null) && (relTerm != null) && KBPOS.equals("1")) {
    show.append(HTMLformatter.showNeighborTerms(kb,nonRelTerm, relTerm));
    show.append("</td></table>");
 }
 else if ((kb != null) && (term != null) && kb.containsTerm(term)) {  // Build the HTML format for all the formulas in
     term = kb.simplifyTerm(term);
     
     show.append("<title>Sigma KEE - " + term + "</title>\n");   // which the given term appears.
     show.append("<table width=\"95%\"><tr><td width=\"50%\"><font face=\"Arial,helvetica\" size=\"+3\"><b>");

     term = term.intern();
     show.append(term);
     show.append("</b></font>");
     boolean isArabic = (language.matches(".*(?i)arabic.*")
                         || language.equalsIgnoreCase("ar"));
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
         if (tfmValue != null) {
             if (isArabic)
                 tfmValue = "<span dir=\"rtl\">" + tfmValue + "</span>";
             show.append("(" + tfmValue + ")");
         }
         else {
             if (language != null && language.equals("EnglishLanguage"))
                 System.out.println("INFO in BrowseBody.jsp: No term format map entry for \"" +
                                     term + "\" in language " + language);
         }
         if (role != null && role.equalsIgnoreCase("admin")) {
             show.append(" [<a href=\"" + HTMLformatter.createHrefStart() + "/sigma/InstFiller.jsp?lang=" + language +
        	    	 "&flang=" + flang + "&kb=" + kbName + "&term=" + term + "\">assert facts</a>]<br>");
         }
         show.append(HTMLformatter.showMap(kb,term));
         show.append(HTMLformatter.showPictures(kb,term));
         show.append("</td>");
         TreeMap tm = WordNet.wn.getWordsFromTerm(term);
         if (tm != null) {
             show.append("<td width=\"10%\"><img src=\"pixmaps/1pixel.gif\" width=\"1\" height=\"1\" border=\"0\"></td>");
             show.append("<td width=\"40%\"><small>");
             if (language.equals("EnglishLanguage")) 
                 show.append(WordNetUtilities.formatWords(tm,kbName));
             else 
                 show.append(OMWordnet.formatWords(term,kbName,language,HTMLformatter.createHrefStart() + "/sigma/"));
             show.append("</small></td>");
         }
         else
             System.out.println("INFO in BrowseBody.jsp: No synsets for term " + term);
         show.append("</tr></table>\n");
     }
     show.append ("</b></font></td></tr></table>\n");

     int limit = Integer.decode(KBmanager.getMgr().getPref("userBrowserLimit")).intValue();
     if (role != null && !role.equalsIgnoreCase("guest")) {
         limit = Integer.decode(KBmanager.getMgr().getPref("adminBrowserLimit")).intValue();
     }

     for (int arg = 1; arg < 6; arg++) {
         String argHeader = ("appearance as argument number " + arg);
         show.append(HTMLformatter.browserSectionFormatLimit(term, argHeader, kb, language,flang,0,limit,arg,"arg"));
     }

     show.append(HTMLformatter.browserSectionFormatLimit(term, "antecedent", kb, language,flang,0,limit,0,"ant"));
     show.append(HTMLformatter.browserSectionFormatLimit(term, "consequent", kb, language,flang,0,limit,0,"cons"));
     show.append(HTMLformatter.browserSectionFormatLimit(term, "statement", kb, language,flang,0,limit,0,"stmt"));
     show.append(HTMLformatter.browserSectionFormatLimit(term, "appearance as argument number 0", kb, language,flang,0,limit,0,"arg"));

     show.append("<p><table align=\"left\" width=\"50%\"><tr><td bgcolor=\"#A8BACF\">" +
                 "<img src=\"pixmaps/1pixel.gif\" width=\"1\" height=\"1\" border=\"0\"></td></tr>" +
                 "</table><br>\n");
     if (!parentPage.equals("TreeView.jsp"))
         show.append("\n<small><a href=\"" + HTMLformatter.createHrefStart() + "/sigma/TreeView.jsp" +
                     "?lang=" + language + "&flang=" + flang + "&kb=" + kbName +
                     "&term=" + term + "\">Show full definition with tree view</a></small><br>\n");

     show.append("\n<small><a href=\"" + HTMLformatter.createHrefStart() + "/sigma/SimpleBrowse.jsp" +
                 "?lang=" + language + "&flang=" + flang + "&kb=" + kbName + "&simple=yes" +
                 "&term=" + term + "\">Show simplified definition (without tree view)</a></small><br>\n");
     show.append("\n<small><a href=\"" + HTMLformatter.createHrefStart() + "/sigma/TreeView.jsp" +
                 "?lang=" + language + "&flang=" + flang + "&kb=" + kbName + "&simple=yes" +
                 "&term=" + term + "\">Show simplified definition (with tree view)</a></small><p>\n");
 }
 else if (kb != null && term != null && kb.containsRE(term) && 
         KBPOS.equals("2") && relREmatch == null) {
 	ArrayList<String> matches = kb.getREMatch(term);
 	ArrayList<String> relMatches = HTMLformatter.getAllRelTerms(kb,matches);
 	ArrayList<String> nonRelMatches = HTMLformatter.getAllNonRelTerms(kb,matches);
 	relREmatch = relMatches.size()>0?relMatches.get(0):"";
 	nonRelREmatch = nonRelMatches.size()>0?nonRelMatches.get(0):"";
 	show.append(HTMLformatter.showREMatches(kb, relREmatch, nonRelREmatch, term));
 }
 else if (kb != null && term != null && kb.containsRE(term) && KBPOS.equals("2") && relREmatch != null) 
 	show.append(HTMLformatter.showREMatches(kb, relREmatch, nonRelREmatch, term));
 
 else if (kb != null && term != null && !kb.containsRE(term) && KBPOS.equals("2") && relREmatch == null) 
  	show.append("<b>No Matches Could Be Found </b>");

%>
