<%@ page import="com.articulate.sigma.Formula" %>
<%@ page import="com.articulate.sigma.Diagnostics" %>
<%@ page import="com.articulate.sigma.KBmanager" %>
<%@ include file="fragments/universal/Prelude.jspf" %>
<html>
  <head>
    <title> Knowledge base Diagnostics</title>
  </head>
  <body bgcolor="#FFFFFF">
<%
/** This code is copyright Teknowledge (c) 2003, Articulate Software (c) 2003-2017, 2020-
    Infosys (c) 2017-2020.

    This software is released under the GNU Public License
    <http://www.gnu.org/copyleft/gpl.html>.

    Please cite the following article in any publication with references:

    Pease A., and Benzmüller C. (2013). Sigma: An Integrated Development Environment
    for Logical Theories. AI Communications 26, pp79-97.  See also
    http://github.com/ontologyportal
*/
  if (!role.equals("admin") && !role.equals("user")) {
    response.sendRedirect("KBs.jsp");
    return;
  }
  long t0 = System.currentTimeMillis();
  String kbHref = null;
  String formattedFormula = null;
  Map theMap = null;
  kbHref = HTMLformatter.createHrefStart() + "/sigma/Browse.jsp?lang=" + lang + "&kb=" + kbName + "&flang=" + flang;

  String termDependencyMessage = "";
  String termDependencyAction = request.getParameter("diagAction");

  if ("generateTermDependency".equals(termDependencyAction)) {
      if (Diagnostics.dependencyCacheExists(Diagnostics.TERM_DEPENDENCY_CACHE_FILE)) {
          termDependencyMessage = "Term dependency cache already exists at " + Diagnostics.dependencyCachePath(Diagnostics.TERM_DEPENDENCY_CACHE_FILE) + ".";
      }
      else {
          try {
              long depStart = System.currentTimeMillis();
              Diagnostics.saveDependenciesForAllKif(Diagnostics.TERM_DEPENDENCY_CACHE_FILE);
              kb = KBmanager.getMgr().getKB(kbName);
              double seconds = (System.currentTimeMillis() - depStart) / 1000.0;
              if (Diagnostics.dependencyCacheExists(Diagnostics.TERM_DEPENDENCY_CACHE_FILE)) {
                  termDependencyMessage = "Generated term dependency cache in " + seconds + " seconds at " + Diagnostics.dependencyCachePath(Diagnostics.TERM_DEPENDENCY_CACHE_FILE) + ".";
              }
              else {
                  termDependencyMessage = "Term dependency cache generation completed, but the cache file was not found.";
              }
          }
          catch (Exception e) {
              termDependencyMessage = "ERROR generating term dependency cache. See server logs. " + e.getClass().getSimpleName() + ": " + e.getMessage();
              e.printStackTrace();
          }
      }
  }
%>
<form action="Diagnostics.jsp">
    <%
        String pageName = "Diag";
        String pageString = "Knowledge Base Diagnostics";
    %>
    <%@include file="fragments/universal/CommonHeader.jspf" %>
</form>
<a href="WNDiag.jsp?kb=<%=kbName%>">Run WordNet diagnostics</a><p>
<form id="generateTermDependencyForm" method="post" action="Diagnostics.jsp">
  <input type="hidden" name="kb" value="<%=kbName%>">
  <input type="hidden" name="lang" value="<%=lang%>">
  <input type="hidden" name="flang" value="<%=flang%>">
  <input type="hidden" name="diagAction" value="generateTermDependency">
</form>
<%
  boolean termDependencyCacheExists = Diagnostics.dependencyCacheExists(Diagnostics.TERM_DEPENDENCY_CACHE_FILE);
  String termDependencyCachePath = Diagnostics.dependencyCachePath(Diagnostics.TERM_DEPENDENCY_CACHE_FILE).toString();
%>
<%
  // Terms without parents
  List<String> termsWithoutParent = Diagnostics.termsNotBelowEntity(kb);
  out.println("<details>");
  out.println("<summary><b style=\"color:DarkRed;\">Error: Terms without a root at Entity</b><hr></summary>");
  out.println(HTMLformatter.termList(termsWithoutParent,kbHref));
  out.println("</details></br>");

  // Terms with unloaded constituents
  out.println("<details>");
  out.println("<summary>");
  out.println("<b style=\"color:DarkRed;\">Error: Terms with unloaded constituents</b>");
  if (!termDependencyCacheExists) out.println("<button type=\"submit\" form=\"generateTermDependencyForm\" " + "onclick=\"event.stopPropagation();\" " + "style=\"margin-left:12px;\">Generate term dependency cache</button>");
  out.println("<hr>");
  out.println("</summary>");
  if (termDependencyMessage != null && !termDependencyMessage.isEmpty()) {
      out.println("<div style=\"padding:8px; border:1px solid #AAAAAA; background:#F5F5F5; margin:10px 0;\">");
      out.println(termDependencyMessage);
      out.println("</div>");
  }
  if (termDependencyCacheExists) out.println(Diagnostics.printMissingConstituentDependencies(kb, kbHref));
  else {
      out.println("The term dependency cache has not been generated yet.<br>");
      out.println("Required cache file: " + termDependencyCachePath);
  }
  out.println("</details></br>");

  // Children of disjoint parents
  List<String> disjoint = Diagnostics.childrenOfDisjointParents(kb);
  out.println("<details>");
  out.println("<summary><b style=\"color:DarkRed;\">Error: Terms with disjoint parents</b><hr></summary>");
  out.println(HTMLformatter.termList(disjoint,kbHref));
  out.println("</details></br>");

  // Children of disjoint parents
  List<String> parts = Diagnostics.partitionViolation(kb);
  out.println("<details>");
  out.println("<summary><b style=\"color:DarkRed;\">Error: Partition violations</b><hr></summary>");
  for (String s : parts) {
      out.println(s + "<br>\n");
  }
  out.println(HTMLformatter.termList(disjoint,kbHref));
  out.println("</details></br>");

  // Formulae with type conflicts
  out.println("<details>");
  out.println("<summary><b style=\"color:DarkRed;\">Error: Formulae with type conflicts</b><hr></summary>");
  out.println(Diagnostics.printFormulaeWithTypeViolations(kb, kbHref));
  out.println("</details></br>");

  // relations without format
  List<String> termsWithoutFormat = Diagnostics.relationsWithoutFormat(kb);
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Relations without format</b><hr></summary>");
  out.println(HTMLformatter.termList(termsWithoutFormat,kbHref));
  out.println("</details></br>");

  // Terms without documentation
  List<String> termsWithoutDoc = Diagnostics.termsWithoutDoc(kb);
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Terms without documentation</b><hr></summary>");
  out.println(HTMLformatter.termList(termsWithoutDoc,kbHref));
  out.println("</details></br>");

  // Terms with multiple documentation
  List<String> termsWithMultipleDoc = Diagnostics.termsWithMultipleDoc(kb);
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Terms with multiple documentation</b><hr></summary>");
  out.println(HTMLformatter.termList(termsWithMultipleDoc,kbHref));
  out.println("</details></br>");

  // Terms differing only in capitalization
  List<String> termCapDiff = Diagnostics.termCapDiff(kb);
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Terms differing only in capitalization</b><hr></summary>");
  out.println(HTMLformatter.termList(termCapDiff,kbHref));
  out.println("</details></br>");

  // Members (instances) of a parent class that are not also members
  // of one of the subclasses that constitute the exhaustive
  // decomposition of the parent class.
  List<String> termsMissingFromPartition = Diagnostics.membersNotInAnyPartitionClass(kb);
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Instances of a partitioned class that are not instances of one of the class's partitioning subclasses</b><hr></summary>");
  out.println(HTMLformatter.termList(termsMissingFromPartition,kbHref));
  out.println("</details></br>");

  // Terms without rules
  List<String> norule = Diagnostics.termsWithoutRules(kb);
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Terms that do not appear in any rules</b><hr></summary>");
  out.println(HTMLformatter.termList(norule,kbHref));
  out.println("</details></br>");

  // Formulae extraneous quanitified variables
  List<Formula> noquant = Diagnostics.quantifierNotInBody(kb);
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Formulae with extraneous quantified variables</b><hr></summary>");
  for (Formula f : noquant)
	  out.println(f.htmlFormat(kbHref) + "<p>");
  out.println("</details></br>");

  // Formulae with unquantified variables appearing only in consequent
  List<Formula> noquantconseq = Diagnostics.unquantsInConseq(kb);
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Formulae with unquantified variable appearing only in consequent</b><hr></summary>");
  for (Formula f : noquantconseq)
	  out.println(f.htmlFormat(kbHref) + "<p>");
  out.println("</details></br>");

  // Files with mutual term dependencies
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Files with mutual dependencies</b><hr></summary>");
  out.println(Diagnostics.printMutualDependencies(kb,kbHref));
  out.println("</details></br>");

  // Diagnostic runtime
  System.out.println("  > " + ((System.currentTimeMillis() - t0) / 1000.0)
                     + " seconds to run all diagnostics");
%>
<%@ include file="fragments/universal/Postlude.jspf" %>
  </body>
</html>