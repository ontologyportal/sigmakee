
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
  long t1 = t0;
  long t2 = t0;
  String kbHref = null;
  String formattedFormula = null;
  Map theMap = null;

  kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?lang=" + language + "&kb=" + kbName + "&flang=" + flang;
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
                ArrayList kbnames = new ArrayList();
                kbnames.addAll(KBmanager.getMgr().getKBnames());
                out.println(HTMLformatter.createMenu("kb",kbName,kbnames)); 
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
  t1 = System.currentTimeMillis();
  ArrayList termsWithoutParent = Diagnostics.termsWithoutParent(kb);
  t2 = System.currentTimeMillis();
  System.out.println("  > " + ((t2 - t1) / 1000.0) 
                     + " seconds collecting terms without parents");
  out.println(HTMLformatter.htmlDivider("Error: Terms without a root at Entity"));
// out.println("<br>");
// out.println("<br>");
  out.println(HTMLformatter.termList(termsWithoutParent,kbHref));
// out.println("<br>");

  // Children of disjoint parents
  t1 = System.currentTimeMillis();
  ArrayList disjoint = Diagnostics.childrenOfDisjointParents(kb);
  t2 = System.currentTimeMillis();
  System.out.println("  > " + ((t2 - t1) / 1000.0) 
                     + " seconds collecting children of disjoint parents");
  out.println("<br>");
  out.println(HTMLformatter.htmlDivider("Error: Terms with disjoint parents"));
  out.println(HTMLformatter.termList(disjoint,kbHref));

  // Terms without documentation
  t1 = System.currentTimeMillis();
  ArrayList termsWithoutDoc = Diagnostics.termsWithoutDoc(kb);
  t2 = System.currentTimeMillis();
  System.out.println("  > " + ((t2 - t1) / 1000.0) 
                     + " seconds collecting terms without documentation");
  out.println("<br>");
  out.println(HTMLformatter.htmlDivider("Warning: Terms without documentation"));
  out.println(HTMLformatter.termList(termsWithoutDoc,kbHref));


  // Terms with multiple documentation
  t1 = System.currentTimeMillis();
  ArrayList termsWithMultipleDoc = Diagnostics.termsWithMultipleDoc(kb);
  t2 = System.currentTimeMillis();
  System.out.println("  > " + ((t2 - t1) / 1000.0) 
                     + " seconds collecting terms with multiple documentation");
  out.println("<br>");
  out.println(HTMLformatter.htmlDivider("Warning: Terms with multiple documentation"));
  out.println(HTMLformatter.termList(termsWithMultipleDoc,kbHref));

  // Members (instances) of a parent class that are not also members
  // of one of the subclasses that constitute the exhaustive
  // decomposition of the parent class.
  t1 = System.currentTimeMillis();
  ArrayList termsMissingFromPartition = Diagnostics.membersNotInAnyPartitionClass(kb);
  t2 = System.currentTimeMillis();
  System.out.println("  > " + ((t2 - t1) / 1000.0) 
                     + " seconds processing instances of partitioned classes");
  out.println("<br>");
  out.println(HTMLformatter.htmlDivider("Warning: Instances of a partitioned class that are not instances of one of the class's partitioning subclasses"));
  out.println("<br>");
  out.println(HTMLformatter.termList(termsMissingFromPartition,kbHref));

  // Terms that do not occur in any rules
  t1 = System.currentTimeMillis();
  ArrayList norule = Diagnostics.termsWithoutRules(kb);
  t2 = System.currentTimeMillis();
  System.out.println("  > " + ((t2 - t1) / 1000.0) 
                     + " seconds collecting terms without rules");
  out.println("<br>");
  out.println(HTMLformatter.htmlDivider("Warning: Terms that do not appear in any rules"));
  out.println(HTMLformatter.termList(norule,kbHref));

  System.out.println("Going on to Formulae with extraneous quantifiers.");
  // Formulae with extraneous quantifiers
  t1 = System.currentTimeMillis();
  ArrayList noquant = Diagnostics.quantifierNotInBody(kb);
  t2 = System.currentTimeMillis();
  System.out.println("  > " + ((t2 - t1) / 1000.0) 
                     + " seconds collecting formulae with extraneous quantifiers");
  if (!noquant.isEmpty()) {
      out.println("<br>");
      out.println(HTMLformatter.htmlDivider("Warning: Formulae with extraneous quantified variables"));
      // out.println("<br>");
      // out.println("<br>");
      Iterator it = noquant.iterator();
      while (it.hasNext()) {
          Formula f = (Formula) it.next();
          out.println(f.htmlFormat(kb));
          out.println("<br><br>");
      }
  }
  // Files with mutual dependencies
  t1 = System.currentTimeMillis();
  out.println("<br>");
  out.println(HTMLformatter.htmlDivider("Warning: Files with mutual dependencies"));
  out.println(Diagnostics.printTermDependency(kb,kbHref));

  System.out.println("  > " + ((System.currentTimeMillis() - t0) / 1000.0) 
                     + " seconds to run all diagnostics");
%>

<%@ include file="Postlude.jsp" %>

  </body>
</html>
