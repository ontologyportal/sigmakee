<%@ include file="Prelude.jsp" %>
<html>                                             
  <head>
    <title> Knowledge base Diagnostics</title>
  </head>
  <body bgcolor="#FFFFFF">

<%
/** This code is copyright Teknowledge (c) 2003, Articulate Software (c) 2003-2017,
    Infosys (c) 2017-present.

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
  ArrayList<String> termsWithoutParent = Diagnostics.termsNotBelowEntity(kb);
  out.println(HTMLformatter.htmlDivider("Error: Terms without a root at Entity"));
  out.println(HTMLformatter.termList(termsWithoutParent,kbHref));

  // Children of disjoint parents
  ArrayList<String> disjoint = Diagnostics.childrenOfDisjointParents(kb);
  out.println("<br>");
  out.println(HTMLformatter.htmlDivider("Error: Terms with disjoint parents"));
  out.println(HTMLformatter.termList(disjoint,kbHref));

  // Terms without documentation
  ArrayList<String> termsWithoutDoc = Diagnostics.termsWithoutDoc(kb);
  out.println("<br>");
  out.println(HTMLformatter.htmlDivider("Warning: Terms without documentation"));
  out.println(HTMLformatter.termList(termsWithoutDoc,kbHref));

  // Terms with multiple documentation
  ArrayList<String> termsWithMultipleDoc = Diagnostics.termsWithMultipleDoc(kb);
  out.println("<br>");
  out.println(HTMLformatter.htmlDivider("Warning: Terms with multiple documentation"));
  out.println(HTMLformatter.termList(termsWithMultipleDoc,kbHref));

  // Members (instances) of a parent class that are not also members
  // of one of the subclasses that constitute the exhaustive
  // decomposition of the parent class.
  ArrayList<String> termsMissingFromPartition = Diagnostics.membersNotInAnyPartitionClass(kb);
  out.println("<br>");
  out.println(HTMLformatter.htmlDivider("Warning: Instances of a partitioned class that are not instances of one of the class's partitioning subclasses"));
  out.println(HTMLformatter.termList(termsMissingFromPartition,kbHref));

  ArrayList<String> norule = Diagnostics.termsWithoutRules(kb);
  out.println("<br>");
  out.println(HTMLformatter.htmlDivider("Warning: Terms that do not appear in any rules"));
  out.println(HTMLformatter.termList(norule,kbHref));

  ArrayList<Formula> noquant = Diagnostics.quantifierNotInBody(kb);
  out.println("<br>");
  out.println(HTMLformatter.htmlDivider("Warning: Formulae with extraneous quantified variables"));
  Iterator<Formula> it = noquant.iterator();
  while (it.hasNext()) {
	  Formula f = it.next();
	  out.println(f.htmlFormat(kbHref));
	  out.println("<p>");
  }
  //out.println(HTMLformatter.termList(noquant,kbHref));
      
  out.println("<br>");
  out.println(HTMLformatter.htmlDivider("Warning: Files with mutual dependencies"));
  out.println(Diagnostics.printTermDependency(kb,kbHref));

  System.out.println("  > " + ((System.currentTimeMillis() - t0) / 1000.0) 
                     + " seconds to run all diagnostics");
%>
<%@ include file="Postlude.jsp" %>
  </body>
</html>
