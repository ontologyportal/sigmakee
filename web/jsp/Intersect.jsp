<%@ include	file="Prelude.jsp" %>

<html>
  <head>
    <title>Sigma KB Term Intersection</title>
  </head>
<body BGCOLOR=#FFFFFF>

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

  String term1 = request.getParameter("term1");
  if (StringUtil.emptyString(term1) || term1.equals("null"))
      term1 = "Object";
  else
      term1 = StringUtil.replaceNonIdChars(StringUtil.removeHTML(term1));
  String term2 = request.getParameter("term2");
  if (StringUtil.emptyString(term2) || term2.equals("null"))
      term2 = "subclass";
  else
      term2 = StringUtil.replaceNonIdChars(StringUtil.removeHTML(term2));
  List<Formula> forms = KButilities.termIntersection(kb,term1,term2);
%>

<form action="Intersect.jsp">

    <%
        String pageName = "Intersect";
        String pageString = "Term Intersection";
    %>
    <%@include file="CommonHeader.jsp" %>

<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

  Term 1: <input type="text" size="30" name="term1" value="<%=term1 %>">
  Term 2: <input type="text" size="30" name="term2" value="<%=term2 %>"><p>

  <p>
  <input type="submit" name="submit" value="submit">
</form>
  <table width="95%">
  <%
  if (forms == null || forms.size() == 0)
      out.println("<b>No intersection of terms " + term1 + " and " + term2 + " found.</b><P>");
  else
      out.println(HTMLformatter.formatFormulaList(forms,"",  kb, language,  flang, 0, 0, ""));
  %>
  </table>
<p>

<%@ include file="Postlude.jsp" %>
</body>
</html>

