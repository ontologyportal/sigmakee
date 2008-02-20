
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
String term = "";
String relation = "";
String header = DocGen.header;
String footer = DocGen.footer;
String filename = "";
KB kb = null;

  if (!KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin")) {
       response.sendRedirect("KBs.jsp");     
  }
  else {
      kbName = request.getParameter("kb");
      namespace = request.getParameter("namespace");
      if (namespace == null) 
          namespace = "";
      if (kbName == null || KBmanager.getMgr().getKB(kbName) == null) {
          System.out.println(" no such knowledge base " + kbName);
          Set names = KBmanager.getMgr().getKBnames();
          if (names != null && names.size() > 0) {
              Iterator it = names.iterator();
              if (it.hasNext()) 
                  kbName = (String) it.next();
          }
      }
      else
          kb = KBmanager.getMgr().getKB(kbName);
      language = request.getParameter("lang");
      header = request.getParameter("header");
      footer = request.getParameter("footer");
      term = request.getParameter("term");
      relation = request.getParameter("relation");
      filename = request.getParameter("filename");
      if (!DB.emptyString(header)) 
          DocGen.header = header;
      if (!DB.emptyString(footer)) 
          DocGen.footer = footer;
      String action = request.getParameter("action");
      language = HTMLformatter.processLanguage(language,kb);
      if (action != null && action != "" && action.equals("generateDocs")) 
          DocGen.generateHTML(kb,language);      
      if (action != null && action != "" && action.equals("generateSingle")) 
          DocGen.generateSingleHTML(kb,language);  
      if (action != null && action != "" && action.equals("dotGraph")) {
          Graph g = new Graph();
          g.createDotGraph(kb, term, relation, filename);
      }
  }

%>

<form action="MiscUtilities.jsp">
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
          <font face="Arial,helvetica" SIZE=-1><b>KB:</b></font><p>
          <%
            ArrayList kbnames = new ArrayList();
            kbnames.addAll(KBmanager.getMgr().getKBnames());
            out.println(HTMLformatter.createMenu("kb",kbName,kbnames)); 
          %>              
          <b>Language:&nbsp;<%= HTMLformatter.createMenu("lang",language,KBmanager.getMgr().allAvailableLanguages()) %></b>
        </td>
      </tr>
    </table><BR>
    <table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'>
        <IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR><p>
    <b>Generate HTML</b><P>
    <table>
        <tr><td>Document header:&nbsp;</td><td><input type="text" name="header" size=100 value="<%=DocGen.header%>"></td></tr>
        <tr><td>Document footer:&nbsp;</td><td><input type="text" name="footer" size=100 value="<%=DocGen.footer%>"></td></tr>
        <tr><td><input type="submit" name="action" value="generateDocs">&nbsp;&nbsp;</td><td>Generate all HTML pages for the KB</td></tr>
        <tr><td><input type="submit" name="action" value="generateSingle">&nbsp;&nbsp;</td><td>Generate a single HTML page for the KB</td></tr>
    </table><p>

    <table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'>
        <IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR><p>
    <b>Create dotted graph format (for <a href="www.graphviz.org">GraphViz</a>)</b><P>
    <table>
        <tr><td>Term:&nbsp;</td><td><input type="text" name="term" size=20 value=""></td></tr>
        <tr><td>Relation:&nbsp;</td><td><input type="text" name="relation" size=20 value=""></td></tr>
        <tr><td>Filename:&nbsp;</td><td><input type="text" name="filename" size=20 value="<%=kbName + "-graph.dot"%>">(saved in $SIGMA_HOME)</td></tr>
        <tr><td><input type="submit" name="action" value="dotGraph">&nbsp;&nbsp;</td><td>Generate graph file</td></tr>
    </table>

</form><p>


<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'>
    <IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><br><P>
<b>Generate KIF from CSV</b><p>

<form action="ProcessFile.jsp"  ID=misc method="POST" enctype="multipart/form-data">
    <table>
        <tr><td>Namespace: </td><td><input type="text" size="30" name="namespace" value="<%=namespace %>"></td></tr>
        <tr><td>CSV file</td><td><INPUT type=file name=csvFile></td></tr>
        <tr><td><input type="submit" name="action" value="kifFromCSV">&nbsp;&nbsp;</td><td>Save to 
                Sigma KBs directory with same filename and .kif extension<br>
                <small>Note that if this file is already loaded, Sigma will need to be restarted 
                to see the new version</small></td></tr>
    </table>
</form><p>

<%@ include file="Postlude.jsp" %>
</body>
</html>

