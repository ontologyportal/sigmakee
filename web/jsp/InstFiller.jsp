<%@ include	file="Prelude.jsp" %>

<%

/** This code is copyright Teknowledge (c) 2003, Rearden Commerce (c) 2011,
    Articulate Software (c) 2003-2017, Infosys (c) 2017-present.

    This software is released under the GNU Public License
    <http://www.gnu.org/copyleft/gpl.html>.

    Please cite the following article in any publication with references:

    Pease A., and Benzmüller C. (2013). Sigma: An Integrated Development Environment
    for Logical Theories. AI Communications 26, pp79-97.  See also
    http://github.com/ontologyportal
*/

if (!role.equalsIgnoreCase("admin")) {
    response.sendRedirect("login.html");
    return;
}
  String submit = request.getParameter("submit");
  if (StringUtil.emptyString(term))
      term = "UnitedStates";
  Enumeration params = request.getParameterNames();
  TreeMap<String, ArrayList<String>> cbset = new TreeMap<String, ArrayList<String>>();
  while (params.hasMoreElements()) {
      String elem = (String) params.nextElement();
      if (elem.startsWith("checkbox-") && request.getParameter(elem) != null) {
          ArrayList<String> al = new ArrayList<String>();
          for (int i = 0; i < 6; i++) {
              String arg = request.getParameter(elem.substring(9) + "--" + Integer.toString(i));
              if (arg != null)
                  al.add(arg);
          }
          cbset.put(elem.substring(9),al);
      }
  }
  String kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?kb=" + kbName;
%>
<head>
  <title>Sigma - Instance Filler</title>
</head>
<BODY BGCOLOR=#FFFFFF>
<form action="InstFiller.jsp" method="GET">

    <%
        String pageName = "InstFiller";
        String pageString = "Instance Filler";
    %>
    <%@include file="CommonHeader.jsp" %>

  <table cellspacing="0" cellpadding="0">
      <tr>
          <td align="left">
              <font face="Arial,helvetica"><b>Term:</b></font>
              <input type="text" name="term" VALUE=<%= "\"" + (request.getParameter("term")==null?"":request.getParameter("term")) + "\"" %>><P>
              <%=EditGUI.genInstPage(kb,term,kbHref)%>
              <%=EditGUI.genClassPage(kb,term,kbHref)%>
              <P>
              <input type="submit" name="submit" value="Submit">
          </td>
      </tr>
      <tr><td>
<%
          if (submit != null)
               out.println(EditGUI.assertFacts(kb,cbset,kbHref));
%>
      </td></tr></table>
</form><BR>

<%@ include file="Postlude.jsp" %>
</body>
