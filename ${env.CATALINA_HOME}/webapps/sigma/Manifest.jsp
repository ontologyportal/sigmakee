<%@ include file="Prelude.jsp" %>

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
    String skbName = request.getParameter("skb");
    /* Validate the request */
    SigmaKB skb = SKBMgr.getDefaultSKBMgr().getSKB(skbName,false);
    String[] all = skb.listAllCttNames();
    if (skb == null )
        response.sendRedirect("home.jsp");  // That KB does not exist    
%>
<HTML>
<HEAD>
<TITLE>Sigma Knowledge Engineering Environment - Source Constituency for <%=skbName %></TITLE>
<!-- <style>@import url(kifb.css);</style> -->
</HEAD>
<BODY BGCOLOR=#FFFFFF>
<table width=95% cellspacing=0 cellpadding=0>
  <tr>
    <td valign="top">
      <table cellspacing=0 cellpadding=0>
        <tr>
          <td align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></td>
          <td>&nbsp;</td>
          <td align="left" valign="top"><img src="pixmaps/logoText-gray.gif"></td>
        </tr>
      </table>
    </td>
    <td valign="bottom"></td>
    <td><font face="Arial,helvetica" SIZE=-1><b>[ <A href="home.jsp">Help</A> ]</b></FONT></td>
  </tr>
</table>
<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<b>Files which are the <I>constituents</I> of the <B><%=skbName.substring(0,skbName.length()-4)%></b> knowledge base </b>
  <%  if (all == null || all.length <= 0) {
      %>
        <H3>No source files have been added to this knowledge base.</H3>
        <P>
        <A HREF="new_kb.jsp?skb=<%=skbName%>">
        <font title="This source will be used first">Add a New Constituent</font></A>
        <%
  }
  else {
%>
      <P> <A HREF="new_kb.jsp?skb=<%=skbName%>&order=first">
      <font title="This source will be used first">Add a New First Constituent</font></A>
      <TABLE border="0" cellspacing="2" cellpadding="2">
        <TD><IMG SRC='pixmaps/1pixel.gif'></TD>
        <Td>File Name</Td>
        <Td>Operations</Td>
        <Td>Source URI Setup </Td>
<%
        for (int i = 0; i < all.length; i++) {
            String aCtt = all[i].toString();
            SigmaCtt sCtt = skb.getSigmaCtt(aCtt);
            if (sCtt == null) {
                System.out.println("Error in manifests.jsp: No constituent " + aCtt);
                continue;
            }
%>
          <TR VALIGN="center" <%= (i % 2)==0? "bgcolor=#eeeeee":""%> >
          <TD nowrap TITLE="Color Indicates Status - Green means file is current and red means that the file is out of date">
<%
            if (sCtt.isURISyn()) { // source in sync
%>
                <img border=0 SRC="pixmaps/green.gif">
<%
            }
            else { // source is not in sync
%>
                <A href="Util.jsp?req=synSkbCtt&skb=<%=skbName%>&ctt=<%=Util.getFileName(aCtt)%>"><img border=0 SRC="pixmaps/red.gif"></A>
<%
            }
%>
            </TD>
          <TD > <A href="edit_file.jsp?skb=<%=skbName%>&file=<%=aCtt%>"> <%=Util.getFileLabel(aCtt)%></A>&nbsp;</TD>
          <TD>
            <A href="Util.jsp?req=removeSkbCtt&skb=<%=skbName%>&constituent=<%=Util.getFileName(aCtt)%>">Remove</A>
            <A href="edit_file.jsp?skb=<%=skbName%>&file=<%=aCtt%>">Edit</A>
          </TD>
<%
%>
          <TD>
          <FORM action="Util.jsp?req=updateCttURI&skb=<%=skbName%>&ctt=<%=aCtt%>" method="POST">
            <INPUT type="text" size=50 name="uriSyn" value=<%=sCtt.getURISyn()%>>
            <INPUT type="submit" value="Update">
          </FORM>
          </TD>
          </TR>
<%
        }  // for
      %>
      </TABLE>
      <BR> <A HREF="new_kb.jsp?skb=<%=skbName%>&order=last">
      <font title="This source will be used last">Add a New Last Constituent</font></A>
<%
    }   // if
%>
<P>
<P>
  <A HREF="home.jsp" title="home.jsp to main page">Return to home page</A>
</BODY>
</HTML>