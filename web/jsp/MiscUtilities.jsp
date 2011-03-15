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
  String sigmaHome = System.getenv("SIGMA_HOME");
  if (StringUtil.emptyString(sigmaHome))
      sigmaHome = "SIGMA_HOME";
  String kbDir = KBmanager.getMgr().getPref("kbDir");
  File kbDirFile = new File(kbDir);
  String namespace = "";
  String term = "";
  String relation = "";
  String ontology = "";
  String filename = "";
  String action = "";
  String status = "";

  if (!KBmanager.getMgr().getPref("userRole").equalsIgnoreCase("administrator")) {
       response.sendRedirect("KBs.jsp");     
  }
  else {
      namespace = request.getParameter("namespace");
      if (namespace == null) 
          namespace = "";
      if (StringUtil.emptyString(kbName) || (KBmanager.getMgr().getKB(kbName) == null)) {
          System.out.println(" no such knowledge base " + kbName);
          Set names = KBmanager.getMgr().getKBnames();
          if (names != null && names.size() > 0) {
              Iterator it = names.iterator();
              if (it.hasNext()) {
                  kbName = (String) it.next();
                  System.out.println("kbName == " + kbName);
              }
          }
      }
      ontology = request.getParameter("ontology");
      if (StringUtil.emptyString(ontology) || ontology.equalsIgnoreCase("null"))
          ontology = "";

      term = request.getParameter("term");
      relation = request.getParameter("relation");
      filename = request.getParameter("filename");
      action = request.getParameter("action");
      if (StringUtil.emptyString(action) || action.equalsIgnoreCase("null")) 
          action = "";
      if (StringUtil.isNonEmptyString(action)) {
          if (kb != null) {
              /* if (action.equalsIgnoreCase("generateDocs")
                  || action.equalsIgnoreCase("generateSingle")) {
                  if (ontology == null) ontology = "";
                  String formatType = "dd";
                  if (ontology.matches(".*(?i)ddex.*"))
                      formatType = "dd1";
                  else if (ontology.matches(".*(?i)ccli.*"))
                      formatType = "dd2";
                  if (action.equalsIgnoreCase("generateSingle"))
                      formatType = "tab";
                  boolean isSimple = (action.equalsIgnoreCase("generateSingle")
                                      || DocGen.getControlTokens().contains(formatType));
                  status = DocGen.generateDocumentsFromKB(kbName,
                                                          ontology, 
                                                          DocGen.getControlBitValue(formatType),
                                                          isSimple,
                                                          null,   // was header
                                                          null);  // was footer
              }
              else*/ if (action.equals("dotGraph")) {
                  Graph g = new Graph();
                  g.createDotGraph(kb, term, relation, filename);
              }
          }
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
          <span class="navlinks">
          <b>[&nbsp;<a href="KBs.jsp">Home</a>&nbsp;|&nbsp;
          <a href="Properties.jsp">Preferences</a>&nbsp;]</b>
          </span>
          <br>
          <b>KB:&nbsp;</b>
          <%
            ArrayList kbnames = new ArrayList();
            kbnames.addAll(KBmanager.getMgr().getKBnames());
            out.println(HTMLformatter.createMenu("kb", kbName, kbnames)); 
          %>              
          &nbsp;<b>Language:&nbsp;</b><%= HTMLformatter.createMenu("lang",language,KBmanager.getMgr().allAvailableLanguages()) %>
        </td>
      </tr>
    </table>
<p>

    <table align="left" width="80%"><tr><td bgcolor="#AAAAAA">
	<img src="pixmaps/1pixel.gif" width="1" height="1" border="0"></td></tr></table><br><p>

<%
               if (action.equalsIgnoreCase("generateDocs") 
                   || action.equalsIgnoreCase("generateSingle")) {
                   if (StringUtil.isNonEmptyString(status)) {
                       if (!status.trim().startsWith("Error")) 
                           out.println("HTML files have been written to " + status);                       
                       else 
                           out.println(status);                       
                       out.println("<br><br>");
                   }
               }
%>
    <b>Create dotted graph format (for <a href="www.graphviz.org">GraphViz</a>)</b><P>
    <table>
        <tr><td align="right">Term:&nbsp;</td><td><input type="text" name="term" size=20 value=""></td></tr>
        <tr><td align="right">Relation:&nbsp;</td><td><input type="text" name="relation" size=20 value=""></td></tr>
        <tr><td align="right">Filename:&nbsp;</td><td><input type="text" name="filename" size=20 value="<%=kbName + "-graph.dot"%>">(saved in <%=sigmaHome%>)</td></tr>
        <tr><td align="right"><input type="submit" name="action" value="dotGraph">&nbsp;&nbsp;</td><td>Generate graph file</td></tr>
    </table>

</form><p>

    <table align="left" width="80%"><tr><td bgcolor="#AAAAAA">
	<img src="pixmaps/1pixel.gif" width="1" height="1" border="0"></td></tr></table><br><p>

<b>Generate KIF from a DIF (.dif) or CSV (.csv) file</b>
<p>
<form action="ProcessFile.jsp"  id="misc" method="POST" enctype="multipart/form-data">
  <table>
    <tr>
      <td align="right">KB:&nbsp;</td>
      <td><input type="text" size="30" name="kb" value=<%=kbName %> ></td>
    </tr>
    <tr>
      <td align="right">Ontology:&nbsp;</td>
      <td><input type="text" size="30" name="ontology" value=<%=ontology %> ></td>
    </tr>
    <tr>
      <td align="right">Data file:&nbsp;</td>
      <td><input type="file" name="dataFile"></td>
    </tr>
    <tr>
      <td>&nbsp;</td>
      <td>
        <input type="checkbox" name="load" value="yes">&nbsp;Load the generated KIF file
      </td>
    </tr>
    <tr>
      <td align="right"><input type="submit" name="action" value="kifFromDataFile">&nbsp;&nbsp;</td>
      <td>
        <small>
          The KIF file will have the same base name as the data
          file, but with the extension .kif and maybe with an
          infixed integer.  It will be saved in the directory
          <%=kbDirFile.getCanonicalPath()%>.
        </small>
      </td>
    </tr>
  </table>
</form>
<p>

<%@ include file="Postlude.jsp" %>

</body>
</html>

