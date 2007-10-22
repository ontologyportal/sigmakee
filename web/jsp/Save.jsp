<%@ include	file="Prelude.jsp" %>
<HTML>
<HEAD>
<TITLE>Sigma KB Save As</TITLE>
<style>@import url(kifb.css);</style>
</HEAD>

<BODY BGCOLOR=#FFFFFF>
<table width=95% cellspacing=0 cellpadding=0><tr><td valign="top">
<table cellspacing=0 cellpadding=0><tr><td align="left" valign="top">
<img src="pixmaps/sigmaSymbol-gray.gif"></td><td>&nbsp;</td><td align="left" valign="top">
<img src="pixmaps/logoText-gray.gif"><BR>&nbsp;&nbsp;&nbsp;<font COLOR=teal></font></td></tr></table></td>
<td valign="bottom"></td><td>
<font face="Arial,helvetica"><b>[ <A href="home.jsp">Help</A> ]</b></FONT></td></tr></table>
<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

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
August 9, Acapulco, Mexico.
*/

  if (!KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin"))         
       response.sendRedirect("KBs.jsp");     

    String skbName = request.getParameter("skb");
    String[] formats = new String[3];
    formats[0] = "KIF";
    formats[1] = "PROTEGE";
    formats[2] = "DAML";
%>

<FORM action="Util.jsp">

SKB: <%= Util.genSelector( SKBMgr.getDefaultSKBMgr().getSKBKeys(), skbName, "skb") %>

<h4> To Save as </h4>

<font face="Arial,helvetica"><b>File Format:&nbsp;</b></font>
<%= Util.genSelector( formats,  formats[0], "format") %>

<INPUT type="hidden" name="req" value="save_as">
<INPUT type="submit" name="action" value="Save">
</FORM>
<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>


<%@ include file="Postlude.jsp" %>
</BODY>
</HTML>
