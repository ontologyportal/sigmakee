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

<a href="WNDiag.jsp?kb=<%=kbName%>">Run WordNet diagnostics</a><p>

<%
  // Terms without parents
  List<String> termsWithoutParent = Diagnostics.termsNotBelowEntity(kb);
  out.println(HTMLformatter.htmlDivider("Error: Terms without a root at Entity"));
  out.println(HTMLformatter.termList(termsWithoutParent,kbHref));

  // Children of disjoint parents
  List<String> disjoint = Diagnostics.childrenOfDisjointParents(kb);
  out.println("<br>" + HTMLformatter.htmlDivider("Error: Terms with disjoint parents"));
  out.println(HTMLformatter.termList(disjoint,kbHref));

  out.println("<br>" + HTMLformatter.htmlDivider("Error: Formulae with type conflicts"));
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

  // relations without format
  List<String> termsWithoutFormat = Diagnostics.relationsWithoutFormat(kb);
  out.println("<br>" + HTMLformatter.htmlDivider("Warning: Relations without format"));
  out.println(HTMLformatter.termList(termsWithoutFormat,kbHref));

  // Terms without documentation
  List<String> termsWithoutDoc = Diagnostics.termsWithoutDoc(kb);
  out.println("<br>" + HTMLformatter.htmlDivider("Warning: Terms without documentation"));
  out.println(HTMLformatter.termList(termsWithoutDoc,kbHref));

  // Terms with multiple documentation
  List<String> termsWithMultipleDoc = Diagnostics.termsWithMultipleDoc(kb);
  out.println("<br>" + HTMLformatter.htmlDivider("Warning: Terms with multiple documentation"));
  out.println(HTMLformatter.termList(termsWithMultipleDoc,kbHref));

  // Terms differing only in capitalization
  List<String> termCapDiff = Diagnostics.termCapDiff(kb);
  out.println("<br>" + HTMLformatter.htmlDivider("Warning: Terms differing only in capitalization"));
  out.println(HTMLformatter.termList(termCapDiff,kbHref));

  // Members (instances) of a parent class that are not also members
  // of one of the subclasses that constitute the exhaustive
  // decomposition of the parent class.
  List<String> termsMissingFromPartition = Diagnostics.membersNotInAnyPartitionClass(kb);
  out.println("<br>" + HTMLformatter.htmlDivider("Warning: Instances of a partitioned class that are not instances of one of the class's partitioning subclasses"));
  out.println(HTMLformatter.termList(termsMissingFromPartition,kbHref));

  List<String> norule = Diagnostics.termsWithoutRules(kb);
  out.println("<br>" + HTMLformatter.htmlDivider("Warning: Terms that do not appear in any rules"));
  out.println(HTMLformatter.termList(norule,kbHref));

  List<Formula> noquant = Diagnostics.quantifierNotInBody(kb);
  out.println("<br>" + HTMLformatter.htmlDivider("Warning: Formulae with extraneous quantified variables"));
  for (Formula f : noquant)
	  out.println(f.htmlFormat(kbHref) + "<p>");

  List<Formula> noquantconseq = Diagnostics.unquantsInConseq(kb);
  out.println("<br>" + HTMLformatter.htmlDivider("Warning: Formulae with unquantified variable appearing only in consequent"));
  for (Formula f : noquantconseq)
	  out.println(f.htmlFormat(kbHref) + "<p>");

  out.println("<br>" + HTMLformatter.htmlDivider("Warning: Files with mutual dependencies"));
  out.println(Diagnostics.printTermDependency(kb,kbHref));

  System.out.println("  > " + ((System.currentTimeMillis() - t0) / 1000.0)
                     + " seconds to run all diagnostics");
%>
<%@ include file="Postlude.jsp" %>
  </body>
</html>
