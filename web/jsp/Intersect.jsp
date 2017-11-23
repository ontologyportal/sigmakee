<%@ include	file="Prelude.jsp" %>

<html>
  <head>
    <title>Sigma KB Term Intersection</title>
  </head>
<body BGCOLOR=#FFFFFF>

<%
/** This code is copyright Articulate Software (c) 2013.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

  String term1 = request.getParameter("term1");
  if (term1 == null || term1.equals("null"))
  	term1 = "Object";
  String term2 = request.getParameter("term2");
  if (term2 == null || term2.equals("null")) 
  	term2 = "subclass";
  ArrayList<Formula> forms = KButilities.termIntersection(kb,term1,term2);
%>

<form action="Intersect.jsp">
<table width=95% cellspacing=0 cellpadding=0>
  <tr>
    <td valign="top">
      <table cellspacing=0 cellpadding=0>
        <tr>
        <td align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></td>
        <TD>&nbsp;</TD>
        <td align="left" valign="top"><img src="pixmaps/logoText-gray.gif"><br><%
         if (!StringUtil.emptyString(username))
             out.println(" : Welcome " + username);
         else
             out.println("<a href=\"login.html\">log in</a>"); %></td>
        </tr>
       </table>
    </td>
    <td valign="bottom">
    </td>
    <td>
      <font face="Arial,helvetica" SIZE=-1><b>[ <a href="KBs.jsp">Home</a></b>
      <b><a href="Properties.jsp">Prefs</a></B> <B>]</B></font><BR>
      <font face="Arial,helvetica" SIZE=-1><b>KB:</b></font>

<%
out.println(HTMLformatter.createKBMenu(kbName)); 
%>              
            </b>
            <b>Language:&nbsp;<%= HTMLformatter.createMenu("lang",language,kb.availableLanguages()) %></b>
    </td>
  </tr>
</table><BR>
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

