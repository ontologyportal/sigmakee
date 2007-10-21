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
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/
%>

<FORM action="<%=parentPage%>">
  <table width="95%" cellspacing="0" cellpadding="0">
      <tr>
          <td align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></td>
          <td>&nbsp;</td>
          <td align="left" valign="top"><img src="pixmaps/logoText-gray.gif"><br>
              <B>Simplified Browsing Interface</B></td>
          <td valign="bottom"></td>
          <td><b>[ <a href="KBs.jsp">Home</b></a>
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
