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
  String term = request.getParameter("term");
  if (StringUtil.emptyString(term))
      term = "UnitedStates";
  String kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?kb=" + kbName + "&term=";
%>
<head>
  <title>Sigma - Instance Filler</title>
</head>
<BODY BGCOLOR=#FFFFFF>
<table width=95% cellspacing=0 cellpadding=0>
    <tr><td valign="top">
        <table cellspacing=0 cellpadding=0>
            <tr><td align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></td>
                <td>&nbsp;</td><td align="left" valign="top"><img src="pixmaps/logoText-gray.gif">
<BR>&nbsp;&nbsp;&nbsp;<font COLOR=teal></font></td></tr></table>
        </td><td valign="bottom"></td><td>
<font face="Arial,helvetica" SIZE=-1><b>[ <A href="KBs.jsp">Home</A> ]</b></FONT></td></tr></table>

<h3>SUMO Instance-Slot Filler</h3>
<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<form action="InstFiller.jsp" method="GET">
  <font face="Arial,helvetica"><b>Term:</b></font>
  <input type="text" name="term" VALUE=<%= "\"" + (request.getParameter("term")==null?"":request.getParameter("term")) + "\"" %>><P>
  <%=EditGUI.genInstPage(kb,term,kbHref)%>
  <input type="submit" value="Submit">
</form>

<BR>

<%@ include file="Postlude.jsp" %>
</body>
