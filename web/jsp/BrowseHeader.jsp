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
August 9, Acapulco, Mexico. See also http://sigmakee.sourceforge.net
*/
%>

<FORM action="<%=parentPage%>">
  <table width="95%" cellspacing="0" cellpadding="0">
      <tr>
          <td align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></td>
          <td>&nbsp;</td>
          <td align="left" valign="top"><img src="pixmaps/logoText-gray.gif"><br>
              <B>Browsing Interface</B></td>
          <td valign="bottom"></td>
          <td><b>[ <a href="KBs.jsp">Home</b></a>&nbsp;|&nbsp;
              <A href="AskTell.jsp?kb=<%=kbName %>&lang=<%=language %>"><b>Ask/Tell</b></A>&nbsp;|&nbsp;
              <A href="Graph.jsp?kb=<%=kbName %>&lang=<%=language %>&term=<%=term %>"><B>Graph</B></A>&nbsp;|&nbsp;
              <a href="Properties.jsp"><b>Prefs</b></a>
              <B>]</B> <br>
              <img src="pixmaps/1pixel.gif" HEIGHT="3"><br>
              <b>KB:&nbsp;
<%
              ArrayList kbnames = new ArrayList();
              kbnames.addAll(KBmanager.getMgr().getKBnames());
              out.println(HTMLformatter.createMenu("kb",kbName,kbnames));
%>
              </b>
              <% if (kb != null) { %>
              <b>Language:&nbsp;<%= HTMLformatter.createMenu("lang",language,kb.availableLanguages()) %></b>
              <% } %>
          </td>
      </tr>
  </table>
  <br>
  <table cellspacing="0" cellpadding="0">
      <tr>
          <td WIDTH="100"><font face="Arial,helvetica"><b>KB Term:&nbsp;</b></font></TD>
          <TD align="left" valign="top" COLSPAN="2">
              <INPUT type="text" size="38" name="term" value=<%= "\"" + (term==null?"":term) + "\"" %>>
          </td>
          <td align="left" valign="top">
              <INPUT type="submit" value="Show">
          </TD>
        <INPUT type="hidden" name="simple" value=<%=simple%>><br> 
      </tr>
      <TR>
          <TD><IMG SRC="pixmaps/1pixel.gif" Height="3"></TD>
      </TR>
</FORM>
<!-- show WordNet search input -->
<form method="GET" action="WordNet.jsp">
  <tr>
      <td WIDTH="100"><font face="Arial,helvetica"><b>English Word:&nbsp;</b></font></TD>
      <td align="left" valign="top">
          <input type="text" size="27" name="word">
          <IMG SRC="pixmaps/1pixel.gif" WIDTH="3"></TD>
      <TD align="left" valign="top">
          <select name="POS">
              <option value="1">Noun <option value="2">Verb <option value="3">Adjective <option value="4">Adverb
          </select>
      </TD>
      <td align="left" valign="top">
          <input type="submit" value="Show">
      </TD>
  </TR>
  </TABLE>
</form>
