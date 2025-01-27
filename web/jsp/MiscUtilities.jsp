<%@ include	file="Prelude.jsp" %>

<html>
  <head>
    <title>Sigma KB Browse - Misc Utilities</title>
  </head>
<body BGCOLOR=#FFFFFF>

<%
/**
    This software is released under the GNU Public License
    <http://www.gnu.org/copyleft/gpl.html>.

    Please cite the following article in any publication with references:

    Pease A., and Benzmüller C. (2013). Sigma: An Integrated Development Environment
    for Logical Theories. AI Communications 26, pp79-97.  See also
    http://github.com/ontologyportal
*/

  String sigmaHome = System.getenv("SIGMA_HOME");
  if (StringUtil.emptyString(sigmaHome))
      sigmaHome = "SIGMA_HOME";
  String kbDir = KBmanager.getMgr().getPref("kbDir");
  String graphDir = KBmanager.getMgr().getPref("graphDir");
  File kbDirFile = new File(kbDir);
  String namespace = "";
  String relation = "";
  String ontology = "";
 // String filename = "";
  String action = "";
  String status = "";

  if (role == null || !role.equalsIgnoreCase("admin")) {
       response.sendRedirect("KBs.jsp");
  }
  else {
      namespace = request.getParameter("namespace");
      if (namespace == null)
          namespace = "";
      if (StringUtil.emptyString(kbName) || (KBmanager.getMgr().getKB(kbName) == null)) {
          System.out.println(" no such knowledge base " + kbName);
          Set<String> names = KBmanager.getMgr().getKBnames();
          if (names != null && !names.isEmpty()) {
              for (String kbStr : names) {
                  System.out.println("kbName == " + kbStr);
              }
          }
      }
      ontology = request.getParameter("ontology");
      if (StringUtil.emptyString(ontology) || ontology.equalsIgnoreCase("null"))
          ontology = "";

      relation = request.getParameter("relation");
      filename = request.getParameter("filename");
      action = request.getParameter("action");
      String writeOWL = request.getParameter("writeOWL");

      if (StringUtil.emptyString(action) || action.equalsIgnoreCase("null"))
          action = "";
      if (writeOWL != null)
          OMWordnet.generateOMWOWLformat(kb);
      if (StringUtil.isNonEmptyString(action)) {
          if (kb != null) {
              if (action.equals("dotGraph")) {
                  Graph g = new Graph();
                  g.createDotGraph(kb, term, relation, 2, 2, 100, filename, "");

              }
          }
      }
  }
%>

<form action="MiscUtilities.jsp">
    <%
        String pageName = "MiscUtilities";
        String pageString = "MiscUtilities";
    %>
    <%@include file="CommonHeader.jsp" %>

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
    <b>Create dotted graph format (for <a href="http://www.graphviz.org">GraphViz</a>)</b><P>
    <table>
        <tr><td align="right">Term:&nbsp;</td><td><input type="text" name="term" size=20 value=""></td></tr>
        <tr><td align="right">Relation:&nbsp;</td><td><input type="text" name="relation" size=20 value=""></td></tr>
        <tr><td align="right">Filename:&nbsp;</td><td><input type="text" name="filename" size=20 value="<%=kbName + "-graph.dot"%>">(saved in <%=graphDir%>)</td></tr>
        <tr><td align="right"><input type="submit" name="action" value="dotGraph">&nbsp;&nbsp;</td><td>Generate graph file</td></tr>
    </table>

    <table align="left" width="80%"><tr><td bgcolor="#AAAAAA">
    <img src="pixmaps/1pixel.gif" width="1" height="1" border="0"></td></tr></table><br><p>

    Write OMW in OWL: <INPUT type="submit" NAME="writeOWL" VALUE="writeOWL">

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

