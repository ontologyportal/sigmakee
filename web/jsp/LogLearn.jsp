<%@ include file="Prelude.jsp" %>
<%@ page import="java.net.URLEncoder, java.nio.charset.Charset, java.io.*, com.articulate.sigma.tp.GenPropFormulas" %>

<html>
<head>
  <title>Sigma KB Browse - Learning Logic</title>
</head>
<body BGCOLOR=#FFFFFF>

<%
/**
 * This software is released under the GNU Public License
 * http://www.gnu.org/copyleft/gpl.html
 *
 * Pease A., and Benzmüller C. (2013).
 * Sigma: An Integrated Development Environment for Logical Theories.
 * AI Communications 26, pp79–97.
 * http://github.com/ontologyportal
 */

// ======================================================
// SECURITY AND ENVIRONMENT
// ======================================================
if ("guest".equals(role)) {
    response.sendRedirect("KBs.jsp");
    return;
}

GenPropFormulas gpf = new GenPropFormulas();
String kbDir = KBmanager.getMgr().getPref("kbDir");
String genDir = kbDir + File.separator + "GeneratedFormulas";
new File(genDir).mkdirs();

// ======================================================
// PARAMETERS
// ======================================================
String numVars = request.getParameter("numVars");
String depth = request.getParameter("depth");
String action = request.getParameter("submit");
String erase = request.getParameter("erase");
String generate = request.getParameter("generate");
String populate = request.getParameter("populate");

if (StringUtil.emptyString(numVars)) numVars = "3";
if (StringUtil.emptyString(depth)) depth = "5";

int numVarsInt = Math.max(1, Math.min(7, Integer.parseInt(numVars)));
int depthInt = Math.max(1, Math.min(7, Integer.parseInt(depth)));

%>

<form action="LogLearn.jsp" method="get">
  <%
      String pageName = "LogLearn";
      String pageString = "LogLearn";
  %>
  <%@ include file="CommonHeader.jsp" %>

  <table align="left" width="80%">
    <tr><td bgcolor="#AAAAAA"><img src="pixmaps/1pixel.gif" width="1" height="1"></td></tr>
  </table><br>

  <p><a href="/sigma/TableauxWorksheet.jsp">Blank Tableau Worksheet</a></p>
  <h2><b>Create logic problem with solutions</b></h2>
  <table>
    <tr>
      <td align="right">Number of variables:&nbsp;</td>
      <td><input type="number" name="numVars" min="1" max="7" value="<%=numVars%>"></td>
    </tr>
    <tr>
      <td align="right">Formula depth:&nbsp;</td>
      <td><input type="number" name="depth" min="1" max="7" value="<%=depth%>"></td>
    </tr>

    <% if ("admin".equalsIgnoreCase(role)) { %>
    <tr>
      <td align="right">Generate new formulas:&nbsp;</td>
      <td><input type="checkbox" name="generate" value="true"></td>
    </tr>
    <tr>
      <td align="right">Populate all caches:&nbsp;</td>
      <td><input type="submit" name="populate" value="Populate Cache"></td>
    </tr>
    <% } %>

    <tr>
      <td align="right"><input type="submit" name="submit" value="Submit">&nbsp;&nbsp;</td>
      <td><input type="submit" name="erase" value="Erase"></td>
    </tr>
  </table>
</form><p>

<%
/* ======================================================
   ADMIN: POPULATE ALL CACHES (calls populateCachedFormulas)
   ====================================================== */
if ("admin".equalsIgnoreCase(role) && populate != null) {
    out.println("<b>Started populating all caches for numvars=1–7, depth=1–7...</b><br>");
    try {
        GenPropFormulas.populateCachedFormulas();
        out.println("<p><b>Cache population complete!</b></p>");
    } catch (Exception e) {
        out.println("<p style='color:red'>Error populating cache: " + e.getMessage() + "</p>");
        e.printStackTrace();
    }
}

/* ======================================================
   SUBMIT HANDLER (User/Admin)
   ====================================================== */
if ("submit".equalsIgnoreCase(action)) {

    boolean admin = "admin".equalsIgnoreCase(role);

    // ---------------------------
    // ADMIN: Generate new cache entries
    // ---------------------------
    if (admin && "true".equals(generate)) {
        out.println("<b>Generating and caching new formulas...</b><br>");
        gpf.init();
        gpf.generateFormulas(10, numVarsInt, depthInt);

        String filePath = genDir + File.separator + "numvar" + numVarsInt + "_depth" + depthInt + ".html";
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, true))) {

            pw.println("<hr><br>");
            pw.println("<b>Contradiction</b>:<br>");
            out.println("<hr><br><b>Contradiction</b>:<br>");
            for (String s : gpf.contraResults) {
                String cnf = gpf.CNF.get(s);
                String tt = gpf.truthTables.get(s);
                String tb = gpf.tableaux.get(s);
                String worksheet = URLEncoder.encode(s, Charset.defaultCharset());

                out.println(s + "<br>CNF: " + cnf + "<br>");
                pw.println(s + "<br>CNF: " + cnf + "<br>");
                out.println("<a href=\"" + tt + "\">truth table</a><br>");
                pw.println("<a href=\"" + tt + "\">truth table</a><br>");
                out.println("<a href=\"" + tb + "\">tableau</a><br>");
                pw.println("<a href=\"" + tb + "\">tableau</a><br>");
                out.println("<a href=\"/sigma/TableauxWorksheet.jsp?f=" + worksheet + "\">tableau worksheet</a><p>");
                pw.println("<a href=\"/sigma/TableauxWorksheet.jsp?f=" + worksheet + "\">tableau worksheet</a><p>");
            }

            pw.println("<hr><br><b>Tautology</b>:<br>");
            out.println("<hr><br><b>Tautology</b>:<br>");
            for (String s : gpf.tautResults) {
                String cnf = gpf.CNF.get(s);
                String tt = gpf.truthTables.get(s);
                String tb = gpf.tableaux.get(s);
                String worksheet = URLEncoder.encode(s, Charset.defaultCharset());

                out.println(s + "<br>CNF: " + cnf + "<br>");
                pw.println(s + "<br>CNF: " + cnf + "<br>");
                out.println("<a href=\"" + tt + "\">truth table</a><br>");
                pw.println("<a href=\"" + tt + "\">truth table</a><br>");
                out.println("<a href=\"" + tb + "\">tableau</a><br>");
                pw.println("<a href=\"" + tb + "\">tableau</a><br>");
                out.println("<a href=\"/sigma/TableauxWorksheet.jsp?f=" + worksheet + "\">tableau worksheet</a><p>");
                pw.println("<a href=\"/sigma/TableauxWorksheet.jsp?f=" + worksheet + "\">tableau worksheet</a><p>");
            }

            pw.println("<hr><br><b>Satisfiable</b>:<br>");
            out.println("<hr><br><b>Satisfiable</b>:<br>");
            for (String s : gpf.satResults) {
                String cnf = gpf.CNF.get(s);
                String tt = gpf.truthTables.get(s);
                String tb = gpf.tableaux.get(s);
                String worksheet = URLEncoder.encode(s, Charset.defaultCharset());

                out.println(s + "<br>CNF: " + cnf + "<br>");
                pw.println(s + "<br>CNF: " + cnf + "<br>");
                out.println("<a href=\"" + tt + "\">truth table</a><br>");
                pw.println("<a href=\"" + tt + "\">truth table</a><br>");
                out.println("<a href=\"" + tb + "\">tableau</a><br>");
                pw.println("<a href=\"" + tb + "\">tableau</a><br>");
                out.println("<a href=\"/sigma/TableauxWorksheet.jsp?f=" + worksheet + "\">tableau worksheet</a><p>");
                pw.println("<a href=\"/sigma/TableauxWorksheet.jsp?f=" + worksheet + "\">tableau worksheet</a><p>");
            }

            pw.println("<!--DELIMITER-->");
            out.println("<p>Saved formulas to: " + filePath + "</p>");
        } catch (Exception e) {
            out.println("<p style='color:red'>Error writing cache: " + e.getMessage() + "</p>");
            e.printStackTrace();
        }
    }
    // ---------------------------
    // USER (retrieve cached)
    // ---------------------------
    else {
        out.println("<b>Retrieved cached formula:</b><br>");
        String formulaHtml = GenPropFormulas.getRandomGeneratedFormula(numVarsInt, depthInt);
        if (formulaHtml != null)
            out.println(formulaHtml);
        else
            out.println("<p>No cached formulas found for numVars=" + numVarsInt +
                        ", depth=" + depthInt + "</p>");
    }
}

/* ======================================================
   ERASE (reset)
   ====================================================== */
if (erase != null && erase.equalsIgnoreCase("erase")) {
    gpf.init();
    out.println("<p>Reset generator state.</p>");
}
%>

<%@ include file="Postlude.jsp" %>
</body>
</html>
