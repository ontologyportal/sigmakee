<%@ include	file="Prelude.jsp" %>

<html>
  <head>
    <title>Sigma KB Browser</title>
  </head>
<body BGCOLOR=#FFFFFF>

<script>
function getWidth() {
    if (self.innerWidth) {
       return self.innerWidth;
    }
    else if (document.documentElement && document.documentElement.clientHeight) {
        return document.documentElement.clientWidth;
    }
    else if (document.body) {
        return document.body.clientWidth;
    }
    return 0;
}

function setWidth(id) {
    document.getElementById(id).value = Math.round(getWidth() * 0.9);
}
</script>

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

  // If the request parameter can appear more than once in the query string, get all values
  String[] values = request.getParameterValues("columns");

  Graph g = new Graph();
  String view = ValidationUtils.sanitizeString(request.getParameter("view"), "text");
  String inst = request.getParameter("inst");
  String all = request.getParameter("all");
  String fileRestrict = ValidationUtils.sanitizeString(request.getParameter("fileRestrict"));

  if (term == null || term.equals("null")) term = "Process";

  String relation = ValidationUtils.sanitizeSumoTerm(request.getParameter("relation"));

  if (relation == null || relation.equals("null") || relation.equals("")) {
      if (KButilities.isRelation(kb,term))
          relation = "subrelation";
      else if (KButilities.isAttribute(kb,term))
          relation = "subAttribute";
      else
          relation = "subclass";
  }
  String up = ValidationUtils.sanitizeIntegerString(request.getParameter("up"), "1");
  int upint = ValidationUtils.sanitizeInteger(up, 1);
  if (upint > 10) upint = 1;

  String down = ValidationUtils.sanitizeIntegerString(request.getParameter("down"), "1");
  int downint = ValidationUtils.sanitizeInteger(down, 1);
  if (downint > 10) downint = 1;
  
  String limit = ValidationUtils.sanitizeString(request.getParameter("limit"));
  if(!StringUtil.isInteger(limit))
    limit = "";
  
  int limitInt = 100;
  try {
      limitInt = ValidationUtils.sanitizeInteger(limit);
      if (limitInt > 100 || limitInt < 10) limitInt = 100;
  } catch (NumberFormatException nfe) {
      limit = "";
  }
  String[] items = request.getParameterValues("columns");
  if (items != null) {
      Iterator<String> it = g.columnList.keySet().iterator();
      String key;
      while (it.hasNext()) {
          key = it.next();
          g.columnList.put(key,"no");
      }
      for (int i = 0; i < items.length; i++)
          g.columnList.put(items[i],"yes");
  }
%>

<form action="Graph.jsp">
    <%
        String pageName = "Graph";
        String pageString = "KB Graph";
    %>
    <%@include file="CommonHeader.jsp" %>
<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

      <font face="Arial,helvetica"><b>Relation: </b>
      <a href="Browse.jsp?kb=<%=kbName%>&term=<%=relation%>"> <%=relation%></a></font><p>
      <%
          /* Present the text layout (graph layout is in the else) */
          if (view.equals("text")) {
              Set<String> result = null;
              boolean instBool = false;
              if (!StringUtil.emptyString(inst) && inst.equals("inst"))
                  instBool = true;
              if (limit != null && limit != "")
                  result = g.createBoundedSizeGraph(kb,term,relation,limitInt,instBool,language);
              else
                  result = g.createGraph(kb,term,relation,upint,
                                         downint,limitInt,instBool,language);
              out.println("<p><table>\n");
              for (String element : result) {
                  out.println(element);
              }
              out.println("</table></p>\n");
          }
          else { // it is a graph
              int width = 500;
              String widthStr = KBmanager.getMgr().getPref("graphWidth");
              if (!StringUtil.emptyString(widthStr))
                  width = Integer.parseInt(widthStr);
              String scrWidth = request.getParameter("scrWidth");
              if (!StringUtil.emptyString(scrWidth))
                  width = Integer.parseInt(scrWidth);
              String edges = "";
              String fname = null;
              boolean graphAvailable = false;

              if (term != null && relation != null && kb != null && role.equalsIgnoreCase("admin")) {
                  fname = "GRAPH_" + kbName + "-" + term + "-" + relation;
                  try {
                      if (!StringUtil.emptyString(all))
                          relation = "all";
                      graphAvailable = g.createDotGraph(kb,term,relation,Integer.parseInt(up),
                                                        Integer.parseInt(down),limitInt,fname,fileRestrict);
                  }
                  catch (Exception ex) {
                      graphAvailable = false;
                  }
              }
              if (graphAvailable) {
                  String imageExt = KBmanager.getMgr().getPref("imageFormat");
                  out.println("<img src='graph/" + fname + ".dot." + imageExt + "'/>");
              }
              else {
                  out.println("<p><b>Error producing graph.</b></p>");
                  out.println(HTMLformatter.formatErrorsWarnings(g.errors,kb));
              }
          }
  %>

  Relation: <input type="text" size="30" name="relation" value="<%=relation %>">
  Term: <input type="text" size="30" name="term" value="<%=term %>"><p>
  Levels &quot;above&quot;:<input type="text" size="2" name="up" value="<%=up %>">
  Levels &quot;below&quot;:<input type="text" size="2" name="down" value="<%=down %>">
  Total term limit:<input type="text" size="2" name="limit" value="<%=limit %>">
  Show instances: <input type="checkbox" name="inst" value="inst" <%= (inst != null && inst.equals("inst")) ? "checked" : "" %>><br>
  All relations: <input type="checkbox" name="all" value="all" <%= (all != null && all.equals("all")) ? "checked" : "" %>>
  Restrict to file:<input type="text" size="30" name="fileRestrict" value="<%=fileRestrict %>"> <br>
  Columns to display:<%=HTMLformatter.createMultiMenu("columns",g.columnList) %>
  <input type="hidden" value="" onLoad="setWidth(this)" name="scrWidth" id="scrWidth"/>
      <script type="text/javascript">setWidth('scrWidth');</script>
  <p>
  <table border="0">
  <tr>
  <td>View format:</td>
  <% if (role.equalsIgnoreCase("admin")) { %>
         <td><input type="radio" name="view" value="graph" <%= (view.equals("graph")) ? "checked" : "" %>>graph</td>
  <% } %>
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

