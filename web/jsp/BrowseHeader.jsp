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
 KBPOS = request.getParameter("KBPOS");
 term = request.getParameter("term");
 
 if (KBPOS == null && term == null)
    KBPOS = "1";
 else if (KBPOS == null && term != null)
 	KBPOS = kb.REswitch(term);
%>

<form action="<%=parentPage%>">
  <table width="95%" cellspacing="0" cellpadding="0">
      <tr>
          <td align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></td>
          <td>&nbsp;</td>
          <td align="left" valign="top"><img src="pixmaps/logoText-gray.gif"><br>
              <b>Browsing Interface</b></td>
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
out.println(HTMLformatter.createKBMenu(kbName)); 
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
          <td align="right"><b>KB Term:&nbsp;</b></td>
          <td align="left" valign="top" colspan="2">
              <input type="text" size="38"  name="term" value=<%= "\"" + (term==null?"":term) + "\"" %>>
          </td>
          <td align="left" valign="top">
              <input type="submit" value="Show">
              <img src="pixmaps/1pixel.gif" width="10"><a href="Intersect.jsp?kb=<%=kbName %>&lang=<%=language %>&flang=<%=flang %>&term1=<%=term %>">Term intersection</a>              
          </td>
         <br> 
      </tr>
      <tr>
          <td><img src="pixmaps/1pixel.gif" height="3"></td>
      </tr>
</form>

<!-- show WordNet search input -->
<form method="GET" action="WordNet.jsp">
  <tr>
      <td align="right"><b>English Word:&nbsp;</b></td>
        <input type="hidden" name="simple" value=<%=simple%>>
        <input type="hidden" name="kb" value=<%=kbName%>>
        <input type="hidden" name="lang" value=<%=language%>>
        <input type="hidden" name="flang" value=<%=flang%>>      
      <td align="left" valign="top">
          <input type="text" size="27" name="word">
          <img src="pixmaps/1pixel.gif" width="3"></td>
      <td align="left" valign="top">
          <select name="POS">
              <option value="1">Noun <option value="2">Verb <option value="3">Adjective <option value="4">Adverb
          </select>
      </td>
      <td align="left" valign="top">
          <input type="submit" value="Show">
      </td>
  </tr>
  </table>
</form>
