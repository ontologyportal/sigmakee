<%@ include file="Prelude.jsp" %>
<html>                                             
<HEAD><TITLE> Knowledge base Browser</TITLE></HEAD>
<BODY BGCOLOR="#FFFFFF">

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
August 9, Acapulco, Mexico.
*/

  StringBuffer show = new StringBuffer();       // Variable to contain the HTML page generated.
  String kbHref = null;
  String htmlDivider = "<table ALIGN='LEFT' WIDTH='50%'><tr><TD BGCOLOR='#A8BACF'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR><BR>\n";
  String kbName = null;   // Name of the knowledge base
  KB kb = null;   // The knowledge base object.
  String formattedFormula = null;

  String term = request.getParameter("term");
  String language = request.getParameter("lang");
  if (language == null)
      language = "en";
  kbName = request.getParameter("kb");
  kb = KBmanager.getMgr().getKB(kbName);
  if (kb == null)
       response.sendRedirect("login.html");     
  Map theMap = null;     // Map of natural language format strings.

  String hostname = KBmanager.getMgr().getPref("hostname");
  if (hostname == null)
      hostname = "localhost";
  String port = KBmanager.getMgr().getPref("port");
  if (port == null)
      port = "8080";
  kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?lang=" + language + "&kb=" + kbName;

  if (kb != null && (term == null || term.equals(""))) {       // Show statistics only when no term is specified.
      show.append("<b>Knowledge base statistics: </b><br><table>");
      show.append("<tr bgcolor=#eeeeee><td>Total Terms</td><td>Total Axioms</td><td>Total Rules</td><tr><tr align='center'>\n");
      show.append("<td>  " + kb.getCountTerms());
      show.append("</td><td> " + kb.getCountAxioms());
      show.append("</td><td> " + kb.getCountRules());
      show.append("</td><tr> </table>\n");  
  }
  
  else if (kb != null && !kb.containsTerm(term)) {           // Show the alphabetic neighbors of a term 
                                                             // that is not present in the KB.      
      System.out.println("Doesn't contain " + term);
      ArrayList relations = kb.getNearestRelations(term);
      ArrayList nonRelations = kb.getNearestNonRelations(term);
      show.append(" <FONT face='Arial,helvetica' size=+3> <b> ");          
      if (term != null) 
          show.append(term);
      show.append("</b></FONT><br><br>");
      show.append("<TABLE>");
    
      for (int i = 0; i < 30; i++) {
          String relation = (String) relations.get(i);
          String nonRelation = (String) nonRelations.get(i);
          if (relation != "" || nonRelation != "") {
              if (i == 15) {
                  show.append("<TR>\n");
                  show.append("  <TD><A href='Browse.jsp?kb="+ kbName +"&term=");
                  show.append(   relation + "'>" + relation + "</A>" + "</TD><TD>&nbsp;&nbsp;</TD>\n");
                  show.append("  <TD><A href='Browse.jsp?kb="+ kbName +"&term=");
                  show.append(   nonRelation + "'>" + nonRelation + "</A>" + "</TD>\n");
                  show.append("</TR>\n");
                  show.append("<TR><TD><FONT SIZE=4 COLOR=\"RED\">" + term + " </FONT></TD><TD>&nbsp;&nbsp;</TD>\n");
                  show.append("<TD><FONT SIZE=4 COLOR=\"RED\">" + term + " </FONT></TD></TR>\n");             
              }
              else {
                  show.append("<TR>\n");
                  show.append("  <TD><A href='Browse.jsp?kb="+ kbName +"&term=");
                  show.append(   relation + "'>" + relation + "</A>" + "</TD><TD>&nbsp;&nbsp;</TD>\n");
                  show.append("  <TD><A href='Browse.jsp?kb="+ kbName +"&term=");
                  show.append(   nonRelation + "'>" + nonRelation + "</A>" + "</TD>\n");
                  show.append("</TR>\n");
              }
          }
      }
      show.append("</TABLE>");
  }

  else if (kb != null && kb.containsTerm(term)) {            // Build the HTML format for all the formulas in 
                                                             // which the given term appears.
      ArrayList forms;
      show.append("<table width='95%'><tr><td width='50%'><FONT face='Arial,helvetica' size=+3><b>");
      if (term != null) { 
          show.append(term);
          show.append("</b></FONT>");
          if (kb.getTermFormatMap(language) != null && kb.getTermFormatMap(language).containsKey(term.intern()))
              show.append("(" + (String) kb.getTermFormatMap(language).get(term.intern()) + ")");
          show.append("</td>");
          WordNet.initOnce();
          TreeMap tm = WordNet.wn.getWordsFromTerm(term);
          if (tm != null) {
              show.append("<td width='10%'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></td>");
              show.append("<td width='40%'><small>");
              show.append(WordNet.wn.formatWords(tm));
              show.append("</small></td>");
          }
          else
              System.out.println("INFO in Browse.jsp: No synsets for term " + term);
          show.append("</tr></table>\n");
      }
      else {
          show.append ("</b></FONT></td></tr></table>\n");
      }

      for (int arg = 1; arg < 6; arg++) {
          forms = kb.ask("arg",arg,term);
          if (forms != null && forms.size() > 0) {
              Collections.sort(forms);
              show.append("<br><b>&nbsp;appearance as argument number " + (new Integer(arg)).toString() + "</B>");
              show.append(htmlDivider + "<TABLE width='95%'>");
              for (int i = 0; i < forms.size(); i++) {
                  Formula f = (Formula) forms.get(i);
                  if (KBmanager.getMgr().getPref("showcached").equalsIgnoreCase("yes") ||
                     !f.sourceFile.substring(f.sourceFile.length()-11,f.sourceFile.length()).equalsIgnoreCase("_Cache.kif")) {
                      show.append("<TR><TD WIDTH='50%' valign=top>");
                      formattedFormula = f.htmlFormat(kbHref) + "</td>\n<TD width='10%' valign=top BGCOLOR=#B8CADF>";
                      if (f.theFormula.length() > 14 && f.theFormula.substring(1,14).compareTo("documentation") == 0) 
                          show.append(kb.formatDocumentation(kbHref,formattedFormula));                                              
                      else
                          show.append(formattedFormula);
                      String sourceFilename = f.sourceFile.substring(f.sourceFile.lastIndexOf(File.separator) + 1,f.sourceFile.length());
                      show.append("<A href=\"EditFile.jsp?file=" + f.sourceFile + "&line=");
                      show.append((new Integer(f.startLine)).toString() + "\">");
                      show.append(sourceFilename);
                      show.append(" " + (new Integer(f.startLine)).toString() + "-" + (new Integer(f.endLine)).toString());
                      show.append("</A>");
                      show.append("</TD>\n<TD width='40%' valign=top>");
                      if (f.theFormula.substring(1,14).compareTo("documentation") == 0 || f.theFormula.substring(1,7).compareTo("format") == 0) 
                          show.append("</TD></TR>\n");
                      else
                          show.append(NLformatter.htmlParaphrase(kbHref,f.theFormula, kb.getFormatMap(language), kb.getTermFormatMap(language), language) + "</TD></TR>\n"); 
                  }
              }
              show.append("</TABLE>\n");
          }
      }     
      forms = kb.ask("ant",0,term);
      show.append(HTMLformatter.browserSectionFormat(forms,"antecedent", htmlDivider, kbHref, kb, language));

      forms = kb.ask("cons",0,term);
      show.append(HTMLformatter.browserSectionFormat(forms,"consequent", htmlDivider, kbHref, kb, language));

      forms = kb.ask("stmt",0,term);
      show.append(HTMLformatter.browserSectionFormat(forms,"statement", htmlDivider, kbHref, kb, language));

      forms = kb.ask("arg",0,term);
      show.append(HTMLformatter.browserSectionFormat(forms,"appearance as argument number 0", htmlDivider, kbHref, kb, language));
  }
%>

<!-- show KB header and KB search input -->

<FORM action="Browse.jsp">
    <table width="95%" cellspacing="0" cellpadding="0">
        <tr>
            <td align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></td>
            <td>&nbsp;</td>
            <td align="left" valign="top"><img src="pixmaps/logoText-gray.gif"><br>
                <B>Browsing Interface</B></td>
            <td valign="bottom"></td>
            <td><b>[ <a href="KBs.jsp">Home</b></a>&nbsp;|&nbsp;
                <A href="AskTell.jsp?kb=<%=kbName %>&lang=<%=language %>"><b>Ask/Tell</b></A>&nbsp;|&nbsp;
                <A href="Graph.jsp?kb=<%=kbName %>&lang=<%=language %>&term=<%=term %>"><B>Graph</B></A>&nbsp;|&nbsp;                                                      
                <a href="Properties.jsp"><b>Prefs</b></a>
                <B>]</B> <br>
                <img src="pixmaps/1pixel.gif" HEIGHT="3"><br>
                <b>KB:&nbsp;
<%
                ArrayList kbnames = new ArrayList();
                kbnames.addAll(KBmanager.getMgr().getKBnames());
                out.println(HTMLformatter.createMenu("kb",kbName,kbnames)); 
%>              
                </b>
                <% if (kb != null) { %>
                <b>Language:&nbsp;<%= HTMLformatter.createMenu("lang",language,kb.availableLanguages()) %></b>
                <% } %>
            </td>
        </tr>
    </table>
    <br>
    <table cellspacing="0" cellpadding="0">
        <tr>
            <td WIDTH="100"><font face="Arial,helvetica"><b>KB Term:&nbsp;</b></font></TD>
            <TD align="left" valign="top" COLSPAN="2">
                <INPUT type="text" size="38" name="term" value=<%= "\"" + (term==null?"":term) + "\"" %>>
            </td>
            <td align="left" valign="top">
                <INPUT type="submit" value="Show">
            </TD>
        </tr>
        <TR>
            <TD><IMG SRC="pixmaps/1pixel.gif" Height="3"></TD>
        </TR>
</FORM>
<!-- show WordNet search input -->
<form method="GET" action="WordNet.jsp">
    <tr>
        <td WIDTH="100"><font face="Arial,helvetica"><b>English Word:&nbsp;</b></font></TD>
        <td align="left" valign="top">
            <input type="text" size="27" name="word">
            <IMG SRC="pixmaps/1pixel.gif" WIDTH="3"></TD>
        <TD align="left" valign="top">
            <select name="POS">
                <option value="1">Noun <option value="2">Verb <option value="3">Adjective <option value="4">Adverb
            </select>
        </TD>
        <td align="left" valign="top">
            <input type="submit" value="Show">
        </TD>
    </TR>
    </TABLE>
</form>

<br>
 <%=show.toString() %><BR>
</BODY>
</HTML>

