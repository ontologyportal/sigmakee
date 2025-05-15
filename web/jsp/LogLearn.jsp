<%@ include	file="Prelude.jsp" %>

<html>
  <head>
    <title>Sigma KB Browse - Learning Logic</title>
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
    String exFileDir = kbDir + File.separator + "exFiles";

    String numVars = request.getParameter("numVars");
    String depth = request.getParameter("depth");
    String action = request.getParameter("action");

%>
<form action="LogLearn.jsp">
    <%
        String pageName = "LogLearn";
        String pageString = "LogLearn";
    %>
    <%@include file="CommonHeader.jsp" %>

    <table align="left" width="80%"><tr><td bgcolor="#AAAAAA">
	<img src="pixmaps/1pixel.gif" width="1" height="1" border="0"></td></tr></table><br><p>

<%
    if (action != null && action.equalsIgnoreCase("action")) {
        GenPropFormulas.generateFormulas(1);
        out.println();
        out.println("--------------------------<br>");
        out.println("Contradictions:<br>");
        for (String s : GenPropFormulas.contraResults) {
            out.println(s + "<br>");
            out.println("CNF: " + GenPropFormulas.CNF.get(s) + "<br>");
            out.println("<a href=\"" + GenPropFormulas.truthTables.get(s) + "\">truth table</a><br>");
            out.println("<a href=\"" + GenPropFormulas.tableaux.get(s) + "\">tableau</a><br>");
        }
        out.println("Tautologies:<br>");
        for (String s : GenPropFormulas.tautResults) {
            out.println(s + "<br>");
            out.println("CNF: " + GenPropFormulas.CNF.get(s) + "<br>");
            out.println("<a href=\"" + GenPropFormulas.truthTables.get(s) + "\">truth table</a><br>");
            out.println("<a href=\"" + GenPropFormulas.tableaux.get(s) + "\">tableau</a><br>");
        }
    }
%>
    <b>Create logic problem with solutions<P>
    <table>
        <tr><td align="right">Number of variables:&nbsp;</td><td><input type="text" name="numVars" size=20 value="<%=numVars %>"></td></tr>
        <tr><td align="right">Formula depth:&nbsp;</td><td><input type="text" name="depth" size=20 value="<%=depth %>"></td></tr>
        <tr><td align="right"><input type="submit" name="action" value="action">&nbsp;&nbsp;</td><td>submit</td></tr>
    </table>

</form><p>
<%@ include file="Postlude.jsp" %>
</body>
</html>

