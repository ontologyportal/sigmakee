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

<TABLE width="95%" cellspacing="0" cellpadding="0">
  <TR>
      <TD align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></TD>
      <TD align="left" valign="top"><img src="pixmaps/logoText-gray.gif"><br><B><%=pageString %></B><%=welcomeString%></TD>
      <TD valign="bottom"></TD>
      <TD>
        <font FACE="Arial, Helvetica" SIZE=-1><b>[&nbsp;
        <%
            if (pageName == null || !pageName.equals("KBs"))
                out.println("<A href=\"KBs.jsp\"><b>Home</b></A>&nbsp;|&nbsp");
            if (kb.eprover != null && role != null && role.equalsIgnoreCase("admin") && (pageName == null || !pageName.equals("AskTell")))
                out.println("<a href=\"AskTell.jsp?kb=" + kbName + "&lang=" + language + "\"><b>Ask/Tell</b></a>&nbsp;|&nbsp;");
            if (pageName == null || !pageName.equals("Graph"))
                out.println("<A href=\"Graph.jsp?kb=" + kbName + "&term=" + term + "&inst=inst" +
                "&lang=" + language + "\"><B>Graph</B></A>&nbsp;|&nbsp");
            if (role != null && !role.equalsIgnoreCase("guest") && (pageName == null || !pageName.equals("NLP")))
                out.println("<A href=\"" + HTMLformatter.createHrefStart() + "/sigmanlp/NLP.jsp\"><b>NLP</b></A>&nbsp;|&nbsp");
            if (role != null && role.equalsIgnoreCase("admin") &&  (pageName == null || !pageName.equals("Prefs")))
                out.println("<A href=\"Properties.jsp\"><b>Prefs</b></A>&nbsp;|&nbsp");
        %>
        ]&nbsp;
        <b>KB:&nbsp;
<%
        ArrayList kbnames = new ArrayList();
        kbnames.addAll(KBmanager.getMgr().getKBnames());
        out.println(HTMLformatter.createMenu("kb",kbName,kbnames));
%>
        </b>
        <b>Language:&nbsp;<%= HTMLformatter.createMenu("lang",language,kb.availableLanguages()) %></b>&nbsp;
        <P><b>Formal Language:&nbsp;</b><%= HTMLformatter.createMenu("flang",flang,HTMLformatter.availableFormalLanguages) %>
      <BR>
      </TD>
  </TR>
</TABLE><br>