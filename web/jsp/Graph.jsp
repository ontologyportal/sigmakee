<%@ include	file="Prelude.jsp" %>

<html>
  <head>
    <title>Sigma KB Browser</title>
  </head>
<body BGCOLOR=#FFFFFF>

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

  String kbName = request.getParameter("kb");
  String view = "text";
  String language = request.getParameter("lang");
  String relation = request.getParameter("relation");
  if (relation == null) relation = "subclass";
  String term = request.getParameter("term");
  if (term == null) term = "Process";
  String up = request.getParameter("up");
  if (up == null) up = "1";
  String down = request.getParameter("down");
  if (down == null) down = "1";
  String limit = request.getParameter("limit");
  try {
      Integer.parseInt(limit);
  } catch (NumberFormatException nfe) {
      limit = "";
  }
  KB kb = null;
  if (kbName == null || KBmanager.getMgr().getKB(kbName) == null) 
      System.out.println(" no such knowledge base " + kbName);
  else
      kb = KBmanager.getMgr().getKB(kbName);

%>

<form action="Graph.jsp">
<table width=95% cellspacing=0 cellpadding=0>
  <tr>
    <td valign="top">
      <table cellspacing=0 cellpadding=0>
        <tr>
        <td align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></td>
        <TD>&nbsp;</TD>
        <td align="left" valign="top"><img src="pixmaps/logoText-gray.gif"></td>
        </tr>
       </table>
    </td>
    <td valign="bottom">
    </td>
    <td>
      <font face="Arial,helvetica" SIZE=-1><b>[ <a href="KBs.jsp">Home</a></b>
      <b><a href="Properties.jsp">Prefs</a></B> <B>]</B></font><BR>
      <font face="Arial,helvetica" SIZE=-1><b>KB:</b></font>

<%
  ArrayList kbnames = new ArrayList();
  kbnames.addAll(KBmanager.getMgr().getKBnames());
  out.println(HTMLformatter.createMenu("kb",kbName,kbnames)); 
%>              
            </b>
            <b>Language:&nbsp;<%= HTMLformatter.createMenu("lang",language,kb.availableLanguages()) %></b>
    </td>
  </tr>
</table><BR>
<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

      <font face="Arial,helvetica"><b>Relation: </b>
      <a href="Browse.jsp?kb=<%=kbName%>&term=<%=relation%>"> <%=relation%></a></font><p>
      <%
          /* Present the text layout (grpah layout is in the else) */
          if (view.equals("text")) {
              ArrayList result = null;
              if (limit != null && limit != "")
                  result = Graph.createBoundedSizeGraph(kb,term,relation,Integer.parseInt(limit),"&nbsp;&nbsp;&nbsp;&nbsp;");              
              else
                  result = Graph.createGraph(kb,term,relation,Integer.parseInt(up),Integer.parseInt(down),"&nbsp;&nbsp;&nbsp;&nbsp;");
              for (int i = 0; i < result.size(); i++) {
                  String element = (String) result.get(i);
                  out.println(element+"<BR>");
              }
              out.println("<P>");
          }
          else { // it is a graph
              int width = 200;
              int size = 200;
              String edges = "";
              %>
              <br>
              <applet codebase="./applet/classes" code="com.articulate.Graph" width=<%=size%> height=<%=size%>>
              <param name="kb" value='<%=kbName%>'>
              <param name="edges" value='<%=edges %>'>
              <param name="term" value="<%=term%>">
              <param name="relation" value="<%=relation%>">
              <param name="up" value="<%=up%>">
              <param name="down" value="<%=down%>">
              <param name="path" value="graphxml.jsp">
              alt="Your browser understands the &lt;APPLET&gt; tag but isn't running the applet, for some reason."
              Your browser is ignoring the &lt;APPLET&gt; tag!
              </applet><p>

              <B>Instructions:</B><BR>
              Click on "Big Scramble" or "Small Scramble" to spread out the
              graph.  Double-click on any node in the graph to go to the Sigma page for that
              term.  To make any node the center of the graph, double-click on it while
              holding down on the Ctrl key.<p>

                <%
          } // end else - graph
  %>

  Relation: <input type="text" size="30" name="relation" value="<%=relation %>">
  Term: <input type="text" size="30" name="term" value="<%=term %>"><p>
  Levels &quot;above&quot;:<input type="text" size="2" name="up" value="<%=up %>">
  Levels &quot;below&quot;:<input type="text" size="2" name="down" value="<%=down %>">
  Total term limit:<input type="text" size="2" name="limit" value="<%=limit %>">
  <p>
  <table border="0">
  <tr>
  <td>View format:</td>
  <td><input type="radio" name="view" value="graph" <%= (view.equals("graph")) ? "checked" : "" %>>graph</td>
  <td><input type="radio" name="view" value="text" <%= (view.equals("text")) ? "checked" : "" %>>text</td>
  </tr>
  </table>
  <p>
  <input type="submit" name="submit" value="submit">
</form>
<p>
<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<%@ include file="Postlude.jsp" %>
</body>
</html>

