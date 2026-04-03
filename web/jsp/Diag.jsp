<%@ include file="Prelude.jsp" %>
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
  System.out.println("INFO in Diag.jsp: Running diagnostics");
  long t0 = System.currentTimeMillis();
  String kbHref = null;
  String formattedFormula = null;
  Map theMap = null;
  kbHref = HTMLformatter.createHrefStart() + "/sigma/Browse.jsp?lang=" + language + "&kb=" + kbName + "&flang=" + flang;
%>
<form action="Diag.jsp">
    <%
        String pageName = "Diag";
        String pageString = "Knowledge Base Diagnostics";
    %>
    <%@include file="CommonHeader.jsp" %>
</form>
<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'>
<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>
<a href="WNDiag.jsp?kb=<%=kbName%>">Run WordNet diagnostics</a><p>
<%
  // Terms without parents
  List<String> termsWithoutParent = Diagnostics.termsNotBelowEntity(kb);
  out.println("<details>");
  out.println("<summary><b>Error: Terms without a root at Entity</b><hr></summary>");
  out.println(HTMLformatter.termList(termsWithoutParent,kbHref));
  out.println("</details></br>");

  // Children of disjoint parents
  List<String> disjoint = Diagnostics.childrenOfDisjointParents(kb);
  out.println("<details>");
  out.println("<summary><b>Error: Terms with disjoint parents</b><hr></summary>");
  out.println(HTMLformatter.termList(disjoint,kbHref));
  out.println("</details></br>");

  // Children of disjoint parents
  List<String> parts = Diagnostics.partitionViolation(kb);
  out.println("<details>");
  out.println("<summary><b>Error: Partition violations</b><hr></summary>");
  for (String s : parts) {
      out.println(s + "<br>\n");
  }
  out.println(HTMLformatter.termList(disjoint,kbHref));
  out.println("</details></br>");

  // Formulae with type conflicts
  out.println("<details>");
  out.println("<summary><b>Error: Formulae with type conflicts</b><hr></summary>");
  KButilities.clearErrors();
  kb.kbCache.errors.clear();
  SUMOtoTFAform.errors.clear();
  for (Formula f : kb.formulaMap.values()) {
      if (!KButilities.hasCorrectTypes(kb,f)) {
          out.println(f.htmlFormat(kbHref) + "<br>");
          out.println(KButilities.errors + "<P>");
      }
      KButilities.clearErrors();
      kb.kbCache.errors.clear();
      SUMOtoTFAform.errors.clear();
  }
  out.println("</details></br>");

  // relations without format
  List<String> termsWithoutFormat = Diagnostics.relationsWithoutFormat(kb);
  out.println("<details>");
  out.println("<summary><b>Warning: Relations without format</b><hr></summary>");
  out.println(HTMLformatter.termList(termsWithoutFormat,kbHref));
  out.println("</details></br>");

  // Terms without documentation
  List<String> termsWithoutDoc = Diagnostics.termsWithoutDoc(kb);
  out.println("<details>");
  out.println("<summary><b>Warning: Terms without documentation</b><hr></summary>");
  out.println(HTMLformatter.termList(termsWithoutDoc,kbHref));
  out.println("</details></br>");

  // Terms with multiple documentation
  List<String> termsWithMultipleDoc = Diagnostics.termsWithMultipleDoc(kb);
  out.println("<details>");
  out.println("<summary><b>Warning: Terms with multiple documentation</b><hr></summary>");
  out.println(HTMLformatter.termList(termsWithMultipleDoc,kbHref));
  out.println("</details></br>");

  // Terms differing only in capitalization
  List<String> termCapDiff = Diagnostics.termCapDiff(kb);
  out.println("<details>");
  out.println("<summary><b>Warning: Terms differing only in capitalization</b><hr></summary>");
  out.println(HTMLformatter.termList(termCapDiff,kbHref));
  out.println("</details></br>");

  // Members (instances) of a parent class that are not also members
  // of one of the subclasses that constitute the exhaustive
  // decomposition of the parent class.
  List<String> termsMissingFromPartition = Diagnostics.membersNotInAnyPartitionClass(kb);
  out.println("<details>");
  out.println("<summary><b>Warning: Instances of a partitioned class that are not instances of one of the class's partitioning subclasses</b><hr></summary>");
  out.println(HTMLformatter.termList(termsMissingFromPartition,kbHref));
  out.println("</details></br>");

  // Terms without rules
  List<String> norule = Diagnostics.termsWithoutRules(kb);
  out.println("<details>");
  out.println("<summary><b>Warning: Terms that do not appear in any rules</b><hr></summary>");
  out.println(HTMLformatter.termList(norule,kbHref));
  out.println("</details></br>");

  // Formulae extraneous quanitified variables
  List<Formula> noquant = Diagnostics.quantifierNotInBody(kb);
  out.println("<details>");
  out.println("<summary><b>Warning: Formulae with extraneous quantified variables</b><hr></summary>");
  for (Formula f : noquant)
	  out.println(f.htmlFormat(kbHref) + "<p>");
  out.println("</details></br>");

  // Formulae with unquantified variables appearing only in consequent
  List<Formula> noquantconseq = Diagnostics.unquantsInConseq(kb);
  out.println("<details>");
  out.println("<summary><b>Warning: Formulae with unquantified variable appearing only in consequent</b><hr></summary>");
  for (Formula f : noquantconseq)
	  out.println(f.htmlFormat(kbHref) + "<p>");
  out.println("</details></br>");

  // Files with mutual term dependencies
  out.println("<details>");
  out.println("<summary><b>Warning: Files with mutual dependencies</b><hr></summary>");
  out.println(Diagnostics.printTermDependency(kb,kbHref));
  out.println("</details></br>");

  // Diagnostic runtime
  System.out.println("  > " + ((System.currentTimeMillis() - t0) / 1000.0)
                     + " seconds to run all diagnostics");
%>
<%@ include file="Postlude.jsp" %>
  </body>
</html>
