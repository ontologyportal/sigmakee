<%@ include file="fragments/universal/Prelude.jspf" %>
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
   String nonRelTerm = request.getParameter("nonrelation");
   String relTerm = request.getParameter("relation");
   String parentPage = "Browse.jsp";
   StringBuilder show = null;
   if (StringUtil.emptyString(nonRelTerm)) nonRelTerm = "";
   if (StringUtil.emptyString(relTerm)) relTerm = "";
   if (flang.equals("OWL")) {
      response.sendRedirect(HTMLformatter.createHrefStart() + "/sigma/OWL.jsp?" + "kb=" + kbName + "&flang=" + flang + "&lang=" + lang + "&term=" + term);
      return;
   }
%>
<html>
   <head><title><%=term%> - Sigma Knowledge base Browser</title></head>
   <body bgcolor="#FFFFFF">
      <%@ include file="fragments/browse/BrowseBody.jspf"%>
      <%@ include file="fragments/browse/BrowseHeader.jspf" %>
      <br>
         <%=show.toString() %>
      <br>
      <%@ include file="fragments/universal/Postlude.jspf" %>
   </body>
</html>