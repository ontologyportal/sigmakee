
<%@ include file="Prelude.jsp" %>
<html>                                             
  <head>
    <title> Knowledge base Browser</title>
  </head>

  <body bgcolor="#FFFFFF">

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
August 9, Acapulco, Mexico.
*/
  System.out.println("INFO in Diag.jsp: Running diagnostics");
  long t0 = System.currentTimeMillis();
  String kbHref = null;
  String formattedFormula = null;
  Map theMap = null;
  kbHref = HTMLformatter.createHrefStart() + "/sigma/Browse.jsp?lang=" + language + "&kb=" + kbName + "&flang=" + flang;
%>
<form action="Diag.jsp">
    <table width="95%" cellspacing="0" cellpadding="0">
        <tr>
            <td align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></td>
            <td>&nbsp;</td>
            <td align="left" valign="top"><img src="pixmaps/logoText-gray.gif"><br>
                <b>Knowledge Base Diagnostics</b></td>
            <td valign="bottom"></td>
            <td><b>[ <a href="KBs.jsp">Home</b></a>&nbsp;|&nbsp;
                <A href="AskTell.jsp?kb=<%=kbName %>&lang=<%=language %>"><b>Ask/Tell</b></A>&nbsp;|&nbsp;
                <a href="Properties.jsp"><b>Prefs</b></a>&nbsp;
                <b>]</b> <br>
                <img src="pixmaps/1pixel.gif" height="3"><br>
                <b>KB:&nbsp;
<%
out.println(HTMLformatter.createKBMenu(kbName)); 
%>              
                </b>
                <b>Language:&nbsp;<%= HTMLformatter.createMenu("lang",language,kb.availableLanguages()) %></b>
            </td>
        </tr>
    </table>
    <br>
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
