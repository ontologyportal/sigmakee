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
  // If the request parameter can appear more than once in the query string, get all values
  String[] values = request.getParameterValues("columns");
  //if (values != null) {
  //    for (int i = 0; i < values.length; i++) 
  //        System.out.println("  value[" + i + "] == " + values[i]);      
  //}

  Graph g = new Graph();
  String view = request.getParameter("view");
  if (view == null)
  	view = "text";
  String term = request.getParameter("term");
  if (term == null || term.equals("null")) term = "Process";
  String relation = request.getParameter("relation");
  if (relation == null || relation.equals("null") || relation.equals("")) {
      if (KButilities.isRelation(kb,term))
          relation = "subrelation";
      else if (KButilities.isAttribute(kb,term))
          relation = "subAttribute";
      else
          relation = "subclass";
  }
  String up = request.getParameter("up");
  if (up == null) up = "1";
  int upint = Integer.parseInt(up);
  if (upint > 10) 
      upint = 1;
  String down = request.getParameter("down");
  if (down == null) down = "1";
  int downint = Integer.parseInt(down);
  if (downint > 10) 
      downint = 1;
  String limit = request.getParameter("limit");
  try {
      Integer.parseInt(limit);
  } 
  catch (NumberFormatException nfe) {
      limit = "";
  }
  String[] items = request.getParameterValues("columns");
  if (items != null) {
      Iterator<String> it = g.columnList.keySet().iterator();
      while (it.hasNext()) {
          String key = it.next();
          g.columnList.put(key,"no");
      }
      for (int i = 0; i < items.length; i++)
          g.columnList.put(items[i],"yes");
  }
  
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
out.println(HTMLformatter.createKBMenu(kbName)); 
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
          /* Present the text layout (graph layout is in the else) */
          if (view.equals("text")) {
              ArrayList result = null;
              if (limit != null && limit != "")
                  result = g.createBoundedSizeGraph(kb,term,relation,Integer.parseInt(limit),"&nbsp;&nbsp;&nbsp;&nbsp;",language);              
              else
                  result = g.createGraph(kb,term,relation,Integer.parseInt(up),Integer.parseInt(down),"&nbsp;&nbsp;&nbsp;&nbsp;",language);
              out.println("<table>\n");
              for (int i = 0; i < result.size(); i++) {
                  String element = (String) result.get(i);
                  out.println(element);
              }
              out.println("</table><P>\n");
          }
          else { // it is a graph
              int width = 200;
              int size = 200;
              String edges = "";
   			  String fname = null;
   			  boolean graphAvailable = false;
   			  
   			  if (term != null && relation != null && kb != null && userRole.equalsIgnoreCase("administrator")) {
   			      fname = "GRAPH_" + kbName + "-" + term + "-" + relation;
   			      try {
   			          graphAvailable = g.createDotGraph(kb, term, relation, fname); 
   			      }
   			      catch (Exception ex) {
   			          graphAvailable = false;
   			      }	  	  		
   			  }
			 
   			  if (graphAvailable) {
   			      %>
   			      <img src="graph/<%=fname%>.gif"></img>
   			      <%
   			  }
   			  else {
   			      %> <p> Error producing graph. </p>
   			      <% 
   			  }
          } // end else - graph
  %>

  Relation: <input type="text" size="30" name="relation" value="<%=relation %>">
  Term: <input type="text" size="30" name="term" value="<%=term %>"><p>
  Levels &quot;above&quot;:<input type="text" size="2" name="up" value="<%=up %>">
  Levels &quot;below&quot;:<input type="text" size="2" name="down" value="<%=down %>">
  Total term limit:<input type="text" size="2" name="limit" value="<%=limit %>"><br>
  Columns to display:<%=HTMLformatter.createMultiMenu("columns",g.columnList) %>
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

<%@ include file="Postlude.jsp" %>
</body>
</html>

