<%
/** This code is copyright Teknowledge (c) 2003, Articulate Software (c) 2003-2017,
    Infosys (c) 2017-present.

    This software is released under the GNU Public License
    <http://www.gnu.org/copyleft/gpl.html>.

    Please cite the following article in any publication with references:

    Pease A., and Benzmüller C. (2013). Sigma: An Integrated Development Environment
    for Logical Theories. AI Communications 26, pp79-97.  See also
    http://github.com/ontologyportal

    This page is designed to be included in others and must have the following variables set:
    pageName, pageString, role, language, kb

    welcomString should be derived from Prelude.jsp
*/
%>
<%
    // --- Natural Language handling ---
    if (request.getParameter("lang") != null && !request.getParameter("lang").isEmpty()) {
        session.setAttribute("lang", request.getParameter("lang"));
        language = request.getParameter("lang");
    }
    String sessionLang = (String) session.getAttribute("lang");
    if (sessionLang != null && !sessionLang.isEmpty()) {
        language = sessionLang;
    }
    else if (language == null || language.isEmpty()) {
        language = "EnglishLanguage";
    }
    // --- Formal Language handling ---
    if (request.getParameter("flang") != null && !request.getParameter("flang").isEmpty()) {
        session.setAttribute("flang", request.getParameter("flang"));
        flang = request.getParameter("flang");
    }
    if (flang != null && !flang.isEmpty()) {
        flang = flang;
    }
    else if (flang == null || flang.isEmpty()) {
        // Pick your default formal language
        flang = "KIF";  // for example
    }
%>


<TABLE width="95%" cellspacing="0" cellpadding="0">
  <TR>
      <TD align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></TD>
      <TD align="left" valign="top">
        <img src="pixmaps/logoText-gray.gif"><br>
        <B><%=pageString %></B>
        <%=welcomeString%>
        <%
            boolean showLogout = (username != null && !username.trim().isEmpty());
            if (showLogout) {
        %>
           &nbsp;<a href="logout.jsp"><b>logout</b></a>
        <% } %>
      </TD>
      <TD valign="bottom"></TD>
      <TD>
        <font FACE="Arial, Helvetica" SIZE=-1><b>[&nbsp;
        <%
            if (pageName == null || !pageName.equals("KBs"))
                out.println("<A href=\"KBs.jsp\"><b>Home</b></A>&nbsp;|&nbsp");
            if (kb != null && kb.eprover != null && role != null && role.equalsIgnoreCase("admin") && (pageName == null || !pageName.equals("AskTell")))
                out.println("<a href=\"AskTell.jsp?kb=" + kbName + "&lang=" + language + "\"><b>Ask/Tell</b></a>&nbsp;|&nbsp;");
            if (pageName == null || !pageName.equals("Graph"))
                out.println("<A href=\"Graph.jsp?kb=" + kbName + "&term=" + term + "&inst=inst" +
                "&lang=" + language + "\"><B>Graph</B></A>&nbsp;|&nbsp");
            if (pageName == null || !pageName.equals("LogLearn"))
                out.println("<A href=\"LogLearn.jsp\"><b>LogLearn</b></A>&nbsp;|&nbsp;");
            if (role != null && !role.equalsIgnoreCase("guest") && (pageName == null || !pageName.equals("NLP")))
                out.println("<A href=\"" + HTMLformatter.createHrefStart() + "/sigmanlp/NLP.jsp\"><b>NLP</b></A>&nbsp;|&nbsp");
            if (role != null && role.equalsIgnoreCase("admin") &&  (pageName == null || !pageName.equals("Prefs")))
                out.println("<A href=\"Properties.jsp\"><b>Prefs</b></A>&nbsp;|&nbsp");
            if (!awsMode && role != null && !role.equalsIgnoreCase("guest") && (pageName == null || !pageName.equals("Editor")))
                out.println("<A href=\"Editor.jsp\"><b>Editor</b></A>&nbsp;|&nbsp;");
            if (role != null && role.equalsIgnoreCase("admin") && (pageName == null || !pageName.equals("manageUsers"))) {
                out.println("<A href=\"ManageUsers.jsp\"><b>Manage Users</b></A>&nbsp;|&nbsp;");
            }
        %>
        ]&nbsp;
        <b>KB:&nbsp;
<%
        List<String> kbnames = new ArrayList<>();
        kbnames.addAll(KBmanager.getMgr().getKBnames());
        out.println(HTMLformatter.createMenu("kb",kbName,kbnames));
%>
        </b>
        <b>Language:&nbsp;
        <form method="get" action="<%=pageName%>.jsp" style="display:inline;">
            <%= HTMLformatter.createMenu("lang", language, kb.availableLanguages(), "onchange='this.form.submit()'")%>
            <input type="hidden" name="kb" value="<%=kbName%>" />
        </form>
        </b>&nbsp;
        <b>Formal Language:&nbsp;
        <form method="get" action="<%=pageName%>.jsp" style="display:inline;">
            <%= HTMLformatter.createMenu("flang", flang, HTMLformatter.availableFormalLanguages, "onchange='this.form.submit()'") %>
            <input type="hidden" name="kb" value="<%=kbName%>" />
        </form>
      <br>
      </td>
  </TR>
</TABLE>