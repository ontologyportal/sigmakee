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

 KBPOS = request.getParameter("KBPOS");
 term = request.getParameter("term");
 
 if (KBPOS == null && term == null)
    KBPOS = "1";
 else if (KBPOS == null && term != null)
 	KBPOS = kb.REswitch(term);

  String POS = request.getParameter("POS");
  if (POS == null)
      POS = "0";
%>

<form action="<%=parentPage%>">
    <%
        String pageName = "Browse";
        String pageString = "Browsing Interface";
    %>
    <%@include file="CommonHeader.jsp" %>

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
              <option <%= POS.equals("0")?"selected":"" %> value="0">Any
              <option <%= POS.equals("1")?"selected":"" %> value="1">Noun
              <option <%= POS.equals("2")?"selected":"" %> value="2">Verb
              <option <%= POS.equals("3")?"selected":"" %> value="3">Adjective
              <option <%= POS.equals("4")?"selected":"" %> value="4">Adverb
          </select>
      </td>
      <td align="left" valign="top">
          <input type="submit" value="Show">
      </td>
  </tr>
  </table>
</form>
