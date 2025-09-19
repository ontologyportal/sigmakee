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
    GenPropFormulas gpf = new GenPropFormulas();
    String sigmaHome = System.getenv("SIGMA_HOME");
    if (StringUtil.emptyString(sigmaHome))
        sigmaHome = "SIGMA_HOME";
    String kbDir = KBmanager.getMgr().getPref("kbDir");
    String exFileDir = kbDir + File.separator + "exFiles";

    String numVars = request.getParameter("numVars");
    if (StringUtil.emptyString(numVars))
        numVars = "3";
    String depth = request.getParameter("depth");
    if (StringUtil.emptyString(depth))
        depth = "5";
        
    // Clamp between 1 and 7
    int numVarsInt = 3;
    int depthInt = 5;

    try {
        numVarsInt = Math.max(1, Math.min(7, Integer.parseInt(numVars)));
    } catch (NumberFormatException e) {
        numVarsInt = 3; // fallback
    }
    try {
        depthInt = Math.max(1, Math.min(7, Integer.parseInt(depth)));
    } catch (NumberFormatException e) {
        depthInt = 5; // fallback
    }
    String action = request.getParameter("submit");
    String erase = request.getParameter("erase");

%>
<form action="LogLearn.jsp">
    <%
        String pageName = "LogLearn";
        String pageString = "LogLearn";
    %>
    <%@include file="CommonHeader.jsp" %>

    <table align="left" width="80%"><tr><td bgcolor="#AAAAAA">
	<img src="pixmaps/1pixel.gif" width="1" height="1" border="0"></td></tr></table><br><p>


    <b>Create logic problem with solutions</b><P>
    <table>
        <tr>
        <td align="right">Number of variables:&nbsp;</td>
        <td>
            <input type="number" id="numVars" name="numVars" min="1" max="7" step="1" value="<%=numVars %>">
        </td>
        </tr>
        <tr>
        <td align="right">Formula depth:&nbsp;</td>
        <td>
            <input type="number" id="depth" name="depth" min="1" max="7" step="1" value="<%=depth %>">
        </td>
        </tr>
        <tr><td align="right"><input type="submit" name="submit" value="submit">&nbsp;&nbsp;</td>
        <td><input type="submit" name="erase" value="erase"></td></tr>
    </table>
</form><p>

<%
    if (action != null && action.equalsIgnoreCase("submit")) {
        out.println("Generate formulas<br>");
        gpf.init();
        if (numVars == null)
            numVars = "3";
        if (depth == null)
            depth = "5";
        gpf.generateFormulas(10, numVarsInt, depthInt);
        out.println();
        out.println("<hr><br>");
        out.println("<b>Contradiction</b>:<br> ");
        for (String s : gpf.contraResults) {
            out.println(s + "<br>");
            out.println("CNF: " + gpf.CNF.get(s) + "<br>");
            out.println("<a href=\"" + gpf.truthTables.get(s) + "\">truth table</a><br>");
            out.println("<a href=\"" + gpf.tableaux.get(s) + "\">tableau</a><p>");
        }
        out.println("<hr><br>");
        out.println("<b>Tautology</b>:<br>");
        for (String s : gpf.tautResults) {
            out.println(s + "<br>");
            out.println("<b>CNF</b>: " + gpf.CNF.get(s) + "<br>");
            out.println("<a href=\"" + gpf.truthTables.get(s) + "\">truth table</a><br>");
            out.println("<a href=\"" + gpf.tableaux.get(s) + "\">tableau</a><p>");
        }
        out.println("<hr><br>");
        out.println("<b>Satisfiable</b>:<br>");
        for (String s : gpf.satResults) {
            out.println(s + "<br>");
            out.println("<b>CNF</b>: " + gpf.CNF.get(s) + "<br>");
            out.println("<a href=\"" + gpf.truthTables.get(s) + "\">truth table</a><br>");
            out.println("<a href=\"" + gpf.tableaux.get(s) + "\">tableau</a><p>");
        }
    }
    if (erase != null && erase.equalsIgnoreCase("erase")) {
        gpf.init();
    }
%>
<P>

<%@ include file="Postlude.jsp" %>
</body>
</html>

