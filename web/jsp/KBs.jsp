<%@ include file="Prelude.jsp" %>

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
    out.println("<html>");
    out.println("  <head>");
    out.println("    <title>Sigma Knowledge Engineering Environment - Main</title>");
    out.println("  </head>");
    out.println("  <body bgcolor=\"#FFFFFF\">");
    String baseDir = mgr.getPref("baseDir");
    String kbDir = mgr.getPref("kbDir");
    if (debug) System.out.println("INFO in KBs.jsp: baseDir == " + baseDir);
    if (debug) System.out.println("INFO in KBs.jsp:   kbDir == " + kbDir);
    if (debug) System.out.println("KBs.jsp: username: " + username);
    boolean isAdministrator = role.equalsIgnoreCase("admin");
    if (debug) System.out.println("INFO in KBs.jsp: ************ Initializing Sigma ***************");
    KBmanager.getMgr().initializeOnce();
    String pageName = "KBs";
    String pageString = "Knowledge Bases";
%>

<%@include file="CommonHeader.jsp" %>

<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'>
<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<!-- Global Knowledge Bases -->
<%
  Iterator<String> kbNames = null;
  String removeResult = "";
  String remove = request.getParameter("remove");  // Delete the given KB
  if (StringUtil.isNonEmptyString(kbName) && StringUtil.isNonEmptyString(remove) && remove.equalsIgnoreCase("true"))
      removeResult = KBmanager.getMgr().removeKB(kbName);

  if (KBmanager.getMgr().getKBnames() != null && !KBmanager.getMgr().getKBnames().isEmpty()) {
      if (debug) System.out.println(KBmanager.getMgr().getKBnames().size());
      kbNames = KBmanager.getMgr().getKBnames().iterator();
      if (debug) System.out.println("INFO in KBs.jsp: Got KB names.");
  }
  String defaultKB = null;
  if (KBmanager.getMgr().getKBnames() == null || KBmanager.getMgr().getKBnames().isEmpty())
      out.println("<H2>No Knowledge Bases loaded.</H2>");
  else {
%>
      <P><TABLE border="0" cellpadding="2" cellspacing="2" NOWRAP>
           <TR>
             <TH>Knowledge Bases</TH>
           </TR>
<%
      if (debug) System.out.println("Showing knowledge bases.");
      boolean first = true;
      boolean odd = true;
      String kbName2 = null;
      while (kbNames.hasNext()) {
          kbName2 = kbNames.next();
          if (first) {
              defaultKB = kbName2;
              first = false;
          }
          kb = (KB) KBmanager.getMgr().getKB(kbName2);
          HTMLformatter.kbHref = HTMLformatter.createHrefStart() + "/sigma/Browse.jsp?lang=" + language;
%>
          <TR VALIGN="center" <%= odd==false? "bgcolor=#eeeeee":""%>>
            <TD><%=kbName2%></TD>
<%
          out.println("<TD>");
          if (odd) odd = false;
          if (kb.constituents == null || kb.constituents.isEmpty())
              out.println("No <A href=\"Manifest.jsp?kb=" + kbName2 + "\">constituents</A>");
          else {
              out.print("(" + kb.constituents.size() + ")&nbsp;");
              out.println("<A href=\"Manifest.jsp?kb=" + kbName2 + "\">Manifest</A>");
          }
          out.println("</TD>");
          out.println("<TD><A href=\"Browse.jsp?kb=" + kbName2 + "&lang=" + language + "\">Browse</A></TD>");
          out.println("<TD><A href=\"Graph.jsp?kb=" + kbName2 + "&lang=" + language + "\">Graph</A></TD>");
          out.println("<TD><A href=\"TestStmnt.jsp?kb=" + kbName2 + "&lang=" + language + "\">Test Stmt</A></TD>");

          if (isAdministrator) {
              out.println("<TD><A href=\"Diag.jsp?kb=" + kbName2 + "&lang=" + language + "\">Diagnostics</A></TD>");
              if (kb.eprover != null)
                  out.println("<TD><A href=\"CCheck.jsp?kb=" + kbName2 + "&lang=" + language + "&page=0\">Consistency Check</A></TD>");
              out.println("<TD><A HREF=\"InferenceTestSuite.jsp?test=inference&kb=" + kbName2 + "&lang=" + language + "\">Inference Tests</A></TD>");
              if (kb.celt != null)
                  out.println("<TD><A HREF=\"InferenceTestSuite.jsp?test=english&kb=" + kbName2 + "&lang=" + language + "\">CELT Tests</A></TD>");
              out.println("<TD><A href=\"WNDiag.jsp?kb=" + kbName2 + "&lang=" + language + "\">WordNet Check</A></TD>");
              out.println("<TD><A href=\"AskTell.jsp?kb=" + kbName2 + "\">Ask/Tell</A>&nbsp;</TD>");
              out.println("<TD><A href=\"KBs.jsp?remove=true&kb=" + kbName2 + "\">Remove</A></TD></TR>");
          }
      }
%>
      </TABLE>
<%
  }
%>

<!-- User Knowledge Bases -->
<%
boolean showUserKBs = (username != null && username.length() > 0 && !"guest".equalsIgnoreCase(username));
if (showUserKBs) {
    out.println("<hr>");
    UserKBmanager userMgr = (UserKBmanager) session.getAttribute("kbManager");
    if (userMgr == null) {
        out.println("<P>No user KB manager found in session.</P>");
    } else {
        java.util.Set<String> userKBnames = userMgr.getKBnames();
        if (userKBnames.isEmpty()) {
            out.println("<P><TABLE border='0' cellpadding='2' cellspacing='2' NOWRAP>");
            out.println("<TR><TH>User Knowledge Bases</TH></TR>");
            out.println("<TR><TD>No User Knowledge Bases loaded.</TD></TR>");
            out.println("</TABLE></P>");
        } else {
%>
<P><TABLE border="0" cellpadding="2" cellspacing="2" NOWRAP>
  <TR><TH>User Knowledge Bases</TH></TR>
<%
            boolean oddU = true;
            for (String kbNameU : userKBnames) {
                KB kbU = userMgr.getKB(kbNameU);
                HTMLformatter.kbHref = HTMLformatter.createHrefStart() + "/sigma/Browse.jsp?lang=" + language;
%>
  <TR VALIGN="center" <%= oddU ? "" : "bgcolor=#eeeeee" %>>
    <TD><%= kbNameU %></TD>
<%
                out.println("<TD>");
                if (oddU) oddU = false; else oddU = true;
                if (kbU.constituents == null || kbU.constituents.isEmpty())
                    out.println("No <A href=\"Manifest.jsp?kb=" + kbNameU + "\">constituents</A>");
                else {
                    out.print("(" + kbU.constituents.size() + ")&nbsp;");
                    out.println("<A href=\"Manifest.jsp?kb=" + kbNameU + "\">Manifest</A>");
                }
                out.println("</TD>");
                //NOT READY FOR THE PUBLIC
                out.println("<TD><A href=\"Browse.jsp?kb=" + kbNameU + "\">Browse</A></TD>");
                out.println("<TD><A href=\"Graph.jsp?kb=" + kbNameU  + "\">Graph</A></TD>");
                out.println("<TD><A href=\"TestStmnt.jsp?kb=" + kbNameU + "\">Test Stmt</A></TD>");

                if (isAdministrator) {
                    out.println("<TD><A href=\"Diag.jsp?kb=" + kbNameU + "\">Diagnostics</A></TD>");
                    if (kbU.eprover != null)
                        out.println("<TD><A href=\"CCheck.jsp?kb=" + kbNameU + "&page=0\">Consistency Check</A></TD>");
                    out.println("<TD><A HREF=\"InferenceTestSuite.jsp?test=inference&kb=" + kbNameU + "&lang=" + language + "\">Inference Tests</A></TD>");
                    if (kbU.celt != null)
                        out.println("<TD><A HREF=\"InferenceTestSuite.jsp?test=english&kb=" + kbNameU + "&lang=" + language + "\">CELT Tests</A></TD>");
                    out.println("<TD><A href=\"WNDiag.jsp?kb=" + kbNameU + "&lang=" + language + "\">WordNet Check</A></TD>");
                    out.println("<TD><A href=\"AskTell.jsp?kb=" + kbNameU + "\">Ask/Tell</A>&nbsp;</TD>");
                    out.println("<TD><A href=\"KBs.jsp?remove=true&kb=" + kbNameU + "\">Remove</A></TD></TR>");
                }
            }
%>
</TABLE></P>
<%
        }
    }
}
%>
<P>
<P>

<!-- Add A New Knowledge Base -->
<%
    if (isAdministrator || role.equalsIgnoreCase("user")) { 
        out.println("<hr>");        
%>      
<P>
<b>Add a New Knowledge Base</b>
<form name="kbUploader" id="kbUploader" action="AddConstituent.jsp" method="POST" enctype="multipart/form-data">
  <table>
    <tr>
      <td><b>KB Name:</b>&nbsp;</td>
      <td><input type="text" name="kb" size="60"></td>
    </tr>
    <tr>
      <td><b>KB Constituent:</b>&nbsp;</td>
      <td><input type="file" name="constituent" size="60"></td>
    </tr>
<% if (isAdministrator) { %>
    <tr>
      <td><b>Make Global:</b>&nbsp;</td>
      <td><input type="checkbox" name="makeGlobal" value="true"></td>
    </tr>
<% } %>
  </table>
  <input type="submit" name="submit" value="Submit">
</form>
<p>
<%  }
  if (isAdministrator) {
      out.println("<a href=\"MiscUtilities.jsp?kb="
                  + kbName
                  + "\">More Output Utilities</a>");
      out.println(" | <a href=\"Mapping.jsp\">Ontology Mappings</a>");
      out.println(" | <a href=\"WordSense.jsp?lang=" + language + "\">Sense/Sentiment Analysis</a>");
      out.println("<p>");
      kbNames = KBmanager.getMgr().getKBnames().iterator();
      String kbName3 = null;
      boolean kbErrorsFound = false;
      while (kbNames.hasNext()) {
          kbName3 = (String) kbNames.next();
          kb = (KB) KBmanager.getMgr().getKB(kbName3);
          System.out.println("INFO in KBs.jsp href: " + HTMLformatter.kbHref);
          if (!kb.errors.isEmpty()) {
              out.println("<br/><b>Errors in KB " + kb.name + "</b><br>\n");
              kbErrorsFound = true;
           	  out.println(HTMLformatter.formatErrorsWarnings(kb.errors,kb));
          }
          if (!kb.warnings.isEmpty()) {
              out.println("<br/><b>Warnings in KB " + kb.name + "</b><br>\n");
              out.println(HTMLformatter.formatErrorsWarnings(kb.warnings,kb));
          }
     }
     out.println("<p>\n");
     if (KBmanager.getMgr().getError().length() > 0) {
         out.print("<br/><b>");
         out.println("System Warnings and Error Notices</b>\n<br>\n");
         out.println(KBmanager.getMgr().getError());
     }
     if (!StringUtil.emptyString(removeResult))
     	out.println("<br/>" + removeResult + "<br/>");
  }
%>
</ul>
<p>

<%@ include file="Postlude.jsp" %>

</BODY>
</HTML>