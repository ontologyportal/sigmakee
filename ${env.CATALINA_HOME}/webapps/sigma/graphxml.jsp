<%@ include	file="Prelude.jsp" %>

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

  String skbName = null;
  skbName = request.getParameter("skb");
  if ( skbName == null)
  {
    out.println("Please select knowledge base");
    return;
  }
  else if ( SKBMgr.getDefaultSKBMgr().getSKB(skbName,false) == null)
  {
    out.println(" no such knowledge base " + skbName);
  }

%>
<html>
  <head>
    <title>Sigma KB Hyper-Browser</title>
    <!-- <style>@import url(kifb.css);</style> -->
  </head>
<body BGCOLOR=#FFFFFF>

<form action="graphxml.jsp">
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
      <font face="Arial,helvetica" SIZE=-1><b>[ <a href="home.jsp">Help</a></b>
      <%if (!isBrowseOnly) %>&nbsp;|&nbsp;
      <b><a href="prefs.jsp?back=true">Prefs</a></B><%%> <B>]</B></font><BR>
      <font face="Arial,helvetica" SIZE=-1><b>KB:</b></font>
      <%= Util.genSelector( SKBMgr.getDefaultSKBMgr().getSKBKeys(),  skbName,"skb") %>
    </td>
  </tr>
</table><BR>
<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

  <%
  String relation = null;
  String center = null;
  String upStr = null;
  int up = 0;
  String downStr = null;
  int down = 0;
  String view = null;

  String errMsg = "null";
  String graphXML = null;

  relation = request.getParameter("relation");
  if ( relation == null ) relation="subclass"; // default to "subclass"
  center = request.getParameter("center");
  if ( center == null) center ="center";
  upStr = request.getParameter("up") == null ? Integer.toString(1) : request.getParameter("up");
  up = Integer.parseInt(upStr);
  downStr = request.getParameter("down") == null ? Integer.toString(1) : request.getParameter("down");
  down = Integer.parseInt(downStr);
  view = request.getParameter("view");
  if (view == null || (!view.equals("graph") && !view.equals("text")))
  {
      view = "graph";  // default to graph
  }

  String submission = request.getParameter("submit");
  if ( submission != null && submission.equalsIgnoreCase("submit"))
  {
      // give me the graph in XML
      graphXML = SigmaUtil.getRelationMapInXMLStr(skbName,center, relation, up, down, "skb.jsp?req=skb_sc&skb="+skbName+"&term=" );

      %>
      <font face="Arial,helvetica"><b>Relation: </b>
      <a href="skb.jsp?req=skb_sc&skb=<%=skbName%>&term=<%=relation%>"> <%=relation%></a></font><p>
      <%
          /* Present the text layout (grpah layout is in the else) */
          if (view.equals("text"))
          {
              /** generate html from XML after applying the xslt template **/
              String xsltFileName = application.getRealPath("tree.xsl");
              String mapResult = Util.xsltTransform(graphXML, Util.getReader(xsltFileName));
              // xsltFileName = application.getRealPath("graph.xsl");
              // String graphStr = Util.xsltTransform(graphXML, Util.getReader(xsltFileName));
              %>
               <XML><%=graphXML%> </XML>
               <br> <%=mapResult%> <br>
              <%
          }
          else
          { // it is a graph
              /** generate html from XML after applying the xslt template **/
              String xsltFileName = application.getRealPath("graph.xsl");
              String edges = Util.xsltTransform(graphXML, Util.getReader(xsltFileName));
              String size = request.getParameter("size");
              if ( size == null || size.equalsIgnoreCase("null"))
              {
                size = "400";
              }
              String reqUrl = request.getRequestURL().toString();
              int pos = reqUrl.indexOf("graphxml.jsp");
              String base = reqUrl.substring(0,pos);
            %>
            <br>
            <applet codebase="./applet/classes" code="com.tks.applet.graph.Graph.class" width=<%=size%> height=<%=size%>>
            <param name="skb" value='<%=skbName%>'>
            <param name="edges" value='<%=edges %>'>
            <param name="center" value="<%=center%>">
            <param name="relation" value="<%=relation%>">
            <param name="up" value="<%=up%>">
            <param name="down" value="<%=down%>">
            <param name="graphxml_realpath" value="<%=base + "graphxml.jsp"%>" >
            <param name="skb_realpath" value="<%=base + "skb.jsp"%>" >
            alt="Your browser understands the &lt;APPLET&gt; tag but isn't running the applet, for some reason."
            Your browser is completely ignoring the &lt;APPLET&gt; tag!
            </applet><p>

            <B>Instructions:</B><BR>
            Click on "Big Scramble" or "Small Scramble" to spread out the
            graph.  Double-click on any node in the graph to go to the Sigma page for that
            term.  To make any node the center of the graph, double-click on it while
            holding down on the Ctrl key.<p>

                <%
          } // end else - graph
    } // closes if (submission != null)
  %>

  Relation: <input type="text" size="30" name="relation" value="<%=relation%>">
  Center: <input type="text" size="30" name="center" value="<%=center.equals("null")?"":center%>">
  Levels &quot;above&quot;:<input type="text" size="2" name="up" value="<%=upStr %>">
  Levels &quot;below&quot;:<input type="text" size="2" name="down" value="<%=downStr %>">
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
<small><i>&copy;2003 Teknowledge Corporation, All rights reserved</i></small>
</body>
</html>

