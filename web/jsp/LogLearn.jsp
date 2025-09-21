<%@ include file="Prelude.jsp" %>

<html>
<head>
    <title>Sigma KB Browse - Learning Logic</title>
</head>
<body BGCOLOR=#FFFFFF>

<%
    /**
     * This software is released under the GNU Public License
     * http://www.gnu.org/copyleft/gpl.html
     */

    GenPropFormulas gpf = new GenPropFormulas();
    String sigmaHome = System.getenv("SIGMA_HOME");
    if (StringUtil.emptyString(sigmaHome))
        sigmaHome = "SIGMA_HOME";
    String kbDir = KBmanager.getMgr().getPref("kbDir");

    // Parameters
    String numVars = request.getParameter("numVars");
    if (StringUtil.emptyString(numVars))
        numVars = "3";

    String depth = request.getParameter("depth");
    if (StringUtil.emptyString(depth))
        depth = "5";

    int numVarsInt = 3;
    int depthInt = 5;
    try {
        numVarsInt = Math.max(1, Math.min(7, Integer.parseInt(numVars)));
    } catch (NumberFormatException e) {
        numVarsInt = 3;
    }
    try {
        depthInt = Math.max(1, Math.min(7, Integer.parseInt(depth)));
    } catch (NumberFormatException e) {
        depthInt = 5;
    }

    String action = request.getParameter("submit");
    String generate = request.getParameter("generate"); // checkbox value

    // Role comes from Prelude.jsp
    if (role == null || role.equals("guest")) role = "user"; // default
%>

<form action="LogLearn.jsp">
    <%
        String pageName = "LogLearn";
        String pageString = "LogLearn";
    %>
    <%@ include file="CommonHeader.jsp" %>

    <b>Create logic problem with solutions</b><p>
    <table>
        <tr>
            <td align="right">Number of variables:&nbsp;</td>
            <td>
                <input type="number" id="numVars" name="numVars"
                       min="1" max="7" step="1" value="<%=numVars %>">
            </td>
        </tr>
        <tr>
            <td align="right">Formula depth:&nbsp;</td>
            <td>
                <input type="number" id="depth" name="depth"
                       min="1" max="7" step="1" value="<%=depth %>">
            </td>
        </tr>
        <% if ("admin".equalsIgnoreCase(role)) { %>
        <tr>
            <td align="right">Generate new formula:&nbsp;</td>
            <td>
                <input type="checkbox" id="generate" name="generate" value="true">
            </td>
        </tr>
        <tr>
            <td align="right">Populate full cache:&nbsp;</td>
            <td>
                <input type="submit" name="populate" value="Populate Cache">
            </td>
        </tr>
        <% } %>
        <tr>
            <td align="right">
                <input type="submit" name="submit" value="submit">&nbsp;&nbsp;
            </td>
            <td>
                <input type="submit" name="erase" value="erase">
            </td>
        </tr>
    </table>
</form><p>
<%
    String populate = request.getParameter("populate");

    if ("admin".equalsIgnoreCase(role) && populate != null) {
        out.println("<b>Started populating cache...</b><br>");
        try {
            GenPropFormulas.populateCachedFormulas();
            out.println("<p><b>Cache population complete!</b></p>");
        } catch (Exception e) {
            out.println("<p>Error populating cache: " + e.getMessage() + "</p>");
            e.printStackTrace();
        }
    }
%>

<%
    if ("user".equalsIgnoreCase(role) && "submit".equalsIgnoreCase(action)) {
        // Users only retrieve from cache
        out.println("<b>Retrieved cached formula:</b><br>");
        String formulaHtml = gpf.getRandomGeneratedFormula(numVarsInt, depthInt);
        if (formulaHtml != null)
            out.println(formulaHtml);
        else
            out.println("<p>No cached formulas found for numVars=" + numVarsInt
                        + ", depth=" + depthInt + "</p>");
    }

    if ("admin".equalsIgnoreCase(role) && "submit".equalsIgnoreCase(action)) {
        if ("true".equals(generate)) {
            out.println("Generate formulas<br>");
            gpf.init();
            gpf.generateFormulas(10, numVarsInt, depthInt);

            // Path for the HTML output file, unique to numVars/depth
            String filePath = kbDir + File.separator + "GeneratedFormulas" + File.separator + "numvar" + numVarsInt + "_depth" + depthInt + ".html";

            java.io.FileWriter fw = null;
            java.io.PrintWriter fileOut = null;
            try {
                fw = new java.io.FileWriter(filePath, true); // append mode
                fileOut = new java.io.PrintWriter(fw);

                out.println("<hr><br>");
                fileOut.println("<hr><br>");
                out.println("<b>Contradiction</b>:<br>");
                fileOut.println("<b>Contradiction</b>:<br>");
                for (String s : gpf.contraResults) {
                    out.println(s + "<br>");
                    fileOut.println(s + "<br>");
                    out.println("CNF: " + gpf.CNF.get(s) + "<br>");
                    fileOut.println("CNF: " + gpf.CNF.get(s) + "<br>");
                    out.println("<a href=\"" + gpf.truthTables.get(s) + "\">truth table</a><br>");
                    fileOut.println("<a href=\"" + gpf.truthTables.get(s) + "\">truth table</a><br>");
                    out.println("<a href=\"" + gpf.tableaux.get(s) + "\">tableau</a><p>");
                    fileOut.println("<a href=\"" + gpf.tableaux.get(s) + "\">tableau</a><p>");
                }

                out.println("<hr><br>");
                fileOut.println("<hr><br>");
                out.println("<b>Tautology</b>:<br>");
                fileOut.println("<b>Tautology</b>:<br>");
                for (String s : gpf.tautResults) {
                    out.println(s + "<br>");
                    fileOut.println(s + "<br>");
                    out.println("<b>CNF</b>: " + gpf.CNF.get(s) + "<br>");
                    fileOut.println("<b>CNF</b>: " + gpf.CNF.get(s) + "<br>");
                    out.println("<a href=\"" + gpf.truthTables.get(s) + "\">truth table</a><br>");
                    fileOut.println("<a href=\"" + gpf.truthTables.get(s) + "\">truth table</a><br>");
                    out.println("<a href=\"" + gpf.tableaux.get(s) + "\">tableau</a><p>");
                    fileOut.println("<a href=\"" + gpf.tableaux.get(s) + "\">tableau</a><p>");
                }

                out.println("<hr><br>");
                fileOut.println("<hr><br>");
                out.println("<b>Satisfiable</b>:<br>");
                fileOut.println("<b>Satisfiable</b>:<br>");
                for (String s : gpf.satResults) {
                    out.println(s + "<br>");
                    fileOut.println(s + "<br>");
                    out.println("<b>CNF</b>: " + gpf.CNF.get(s) + "<br>");
                    fileOut.println("<b>CNF</b>: " + gpf.CNF.get(s) + "<br>");
                    out.println("<a href=\"" + gpf.truthTables.get(s) + "\">truth table</a><br>");
                    fileOut.println("<a href=\"" + gpf.truthTables.get(s) + "\">truth table</a><br>");
                    out.println("<a href=\"" + gpf.tableaux.get(s) + "\">tableau</a><p>");
                    fileOut.println("<a href=\"" + gpf.tableaux.get(s) + "\">tableau</a><p>");
                }

                out.println("<p>Saved formulas to: " + filePath + "</p>");
                fileOut.println("<!--DELIMITER-->");

            } catch (Exception e) {
                out.println("<p>Error writing file: " + e.getMessage() + "</p>");
            } finally {
                if (fileOut != null) fileOut.close();
                if (fw != null) fw.close();
            }
        } else {
            out.println("<b>Retrieved cached formula:</b><br>");
            String formulaHtml = gpf.getRandomGeneratedFormula(numVarsInt, depthInt);
            if (formulaHtml != null)
                out.println(formulaHtml);
            else
                out.println("<p>No cached formulas found for numVars=" + numVarsInt
                            + ", depth=" + depthInt + "</p>");
        }
    }
%>

<%@ include file="Postlude.jsp" %>
</body>
</html>
