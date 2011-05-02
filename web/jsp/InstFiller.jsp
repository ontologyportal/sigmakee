<%@ include	file="Prelude.jsp" %>

<%
/** This code is copyright Rearden Commerce (c) 2011.  Some portions
copyright Articulate Software (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Rearden Commerce
and Articulate Software in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/

  String submit = request.getParameter("submit");
  String term = request.getParameter("term");
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
  <table width="95%" cellspacing="0" cellpadding="0">
      <tr>
          <td align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></td>
          <td>&nbsp;</td>
          <td align="left" valign="top"><img src="pixmaps/logoText-gray.gif"><br>
              <b>Instance Filler</b></td>
          <td valign="bottom"></td>
          <td>
          <span class="navlinks">
          <b>[&nbsp;<a href="KBs.jsp">Home</a>&nbsp;|&nbsp;
<%
              if (kb.inferenceEngine != null && KBmanager.getMgr().getPref("userRole") != null &&
                  KBmanager.getMgr().getPref("userRole").equalsIgnoreCase("administrator")) {
                  out.println("<a href=\"AskTell.jsp?kb=" + kbName + "&lang=" + language + "\">Ask/Tell</a>&nbsp;|&nbsp;");
              }
%>
              <a href="Graph.jsp?kb=<%=kbName %>&lang=<%=language %>&term=<%=term %>">Graph</a>&nbsp;|&nbsp;
<%
              if (KBmanager.getMgr().getPref("userRole") != null &&
                  KBmanager.getMgr().getPref("userRole").equalsIgnoreCase("administrator")) {
                  out.println("<a href=\"Properties.jsp\">Preferences</a>");
              }
%>
             &nbsp;]
           </b>
           </span>
           <br>
              <img src="pixmaps/1pixel.gif" height="3"> <br>
              <b>KB:&nbsp;</b>
<%
              ArrayList kbnames = new ArrayList();
              kbnames.addAll(KBmanager.getMgr().getKBnames());
              out.println(HTMLformatter.createMenu("kb",kbName,kbnames));
%>
              <% if (kb != null) { %>
              <b>Language:&nbsp;</b><%= HTMLformatter.createMenu("lang",language,kb.availableLanguages()) %>
              <% } %>
              <P><b>Formal Language:&nbsp;</b><%= HTMLformatter.createMenu("flang",flang,HTMLformatter.availableFormalLanguages) %>
          </td>
      </tr>
  </table>
  <br>
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
