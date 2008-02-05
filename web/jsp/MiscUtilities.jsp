
<%@ include	file="Prelude.jsp" %>

<html>
  <head>
    <title>Sigma KB Browse - Misc Utilities</title>
  </head>
<body BGCOLOR=#FFFFFF>

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

String kbName = "";
String namespace = "";
String language = "";
KB kb = null;

  if (!KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin")) {
       response.sendRedirect("KBs.jsp");     
  }
  else {
      kbName = request.getParameter("kb");
      namespace = request.getParameter("namespace");
      if (namespace == null) 
          namespace = "";
      if (kbName == null || KBmanager.getMgr().getKB(kbName) == null) 
          System.out.println(" no such knowledge base " + kbName);
      else
          kb = KBmanager.getMgr().getKB(kbName);
      language = request.getParameter("lang");
      String action = request.getParameter("action");
      language = HTMLformatter.processLanguage(language,kb);
      if (action != null && action != "" && action.equals("generateDocs")) {
           DocGen.generateHTML(kb);
      }
  }

%>

<table width=95% cellspacing=0 cellpadding=0>
  <tr>
    <td valign="top">
      <table cellspacing=0 cellpadding=0>
        <tr>
        <td align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></td>
        <TD>&nbsp;</TD>
        <td align="left" valign="top"><img src="pixmaps/logoText-gray.gif"></td>
        </tr>
       </table>
    </td>
    <td valign="bottom">
    </td>
    <td>
      <font face="Arial,helvetica" SIZE=-1><b>[ <a href="KBs.jsp">Home</a></b>
      <b><a href="Properties.jsp">Prefs</a></B> <B>]</B></font><BR>
      <font face="Arial,helvetica" SIZE=-1><b>KB:</b></font>
<form action="MiscUtilities.jsp">
<%
  ArrayList kbnames = new ArrayList();
  kbnames.addAll(KBmanager.getMgr().getKBnames());
  out.println(HTMLformatter.createMenu("kb",kbName,kbnames)); 
%>              
            </b>
<%
  if (kb != null) {
%>
            <b>Language:&nbsp;<%= HTMLformatter.createMenu("lang",language,kb.availableLanguages()) %></b>
<%
  }
%>
    </td>
  </tr>
</table><BR>

<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>
  <input type="submit" name="action" value="generateDocs">
</form>

<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><P>

<b>Generate KIF from CSV</b><p>

<form action="ProcessFile.jsp"  ID=misc method="POST" enctype="multipart/form-data">
<input type="radio" name="action" value="kifFromCSV">KIF from CSV<p>

  Namespace: <input type="text" size="30" name="namespace" value="<%=namespace %>"><p>
    <B>CSV file</B><INPUT type=file name=csvFile><BR>
      <p>

  <input type="submit" name="submit" value="submit">Save to Sigma KBs directory with same filename and .kif extension<br>
    <small>Note that if this file is already loaded, Sigma will need to be restarted to see the new version</small><p>
</form>
<p>
<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<%@ include file="Postlude.jsp" %>
</body>
</html>

