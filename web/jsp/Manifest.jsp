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

/** This jsp page handles listing the files which comprise a knowledge base,
    adding new constituents (files), and deleting constituents.  It redirects
    to AddConstituent.jsp to add new constituents.  The page takes several
    parameters:
    kbName - the name of the knowledge base for which the manifest is displayed.
    constituent - a constituent to be added to the KB.
    delete - a constituent to be deleted from the KB.
*/

    String kbName = request.getParameter("kb");
    String writeProlog = request.getParameter("writeProlog");
    String constituent = request.getParameter("constituent");
    String delete = request.getParameter("delete");
    KB kb = KBmanager.getMgr().getKB(kbName);
    if (kb == null || kbName == null)
        response.sendRedirect("KBs.jsp");  // That KB does not exist    
    if (writeProlog != null) 
        kb.writePrologFile(kb.name + ".pl");    
    if (delete != null) {
        int i = kb.constituents.indexOf(constituent.intern());
        if (i == -1) {
            System.out.println("Error in Manifest.jsp: No such constituent: " + constituent.intern());
            kb.reload();
        }
        else {
            kb.constituents.remove(i);
            KBmanager.getMgr().writeConfiguration();
            kb.reload();       
        }
    }
    else if (constituent != null) {
        kb.addConstituent(constituent);
        KBmanager.getMgr().writeConfiguration();
    }
%>
<HTML>
<HEAD>
<TITLE>Sigma Knowledge Engineering Environment - Constituents of <%=kbName %></TITLE>
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
    <td><font face="Arial,helvetica" SIZE=-1><b>[ <A href="KBs.jsp">Home</A> ]</b></FONT></td>
  </tr>
</table>
<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<b>Files which are the <I>constituents</I> of the <B><%=kbName %></b> knowledge base </b>
<%
  if (kb.constituents == null || kb.constituents.size() <= 0) {
      %>
        <H3>No source files have been added to this knowledge base.</H3>
        <P>
        <%
  }
  else {
%>
      <P>
      <TABLE border="0" cellspacing="2" cellpadding="2">
        <Td>File Name</Td>
        <Td>Operations</Td>
<%
        for (int i = 0; i < kb.constituents.size(); i++) {
            String aConstituent = (String) kb.constituents.get(i);
%>
          <TR VALIGN="center" <%= (i % 2)==0? "bgcolor=#eeeeee":""%> >
          <TD><%=aConstituent%>&nbsp;</TD>
          <TD>

          <% if (KBmanager.getMgr().getPref("userName") != null && KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin")) { %>
                <A href="Manifest.jsp?delete=true&constituent=<%=aConstituent%>&kb=<%=kbName%>">Remove</A>            
          <%     } %>
          </TD>
          </TR>
<%
        }  // for
      %>
      </TABLE>
      <BR>
<%
  }   // if
%>
<P>

<% if (KBmanager.getMgr().getPref("userName") != null && KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin")) { %>
    <B>Add a new constituent</B><BR>
    <FORM name=kbUploader ID=kbUploader action="AddConstituent.jsp" method="POST" enctype="multipart/form-data">
        <INPUT type="hidden" name="kb" value=<%=kbName%>><br> 
        <B>KB Constituent</B><INPUT type="file" name="constituent"><BR>
        <INPUT type="submit" NAME="submit" VALUE="Load">
    </FORM>


    <FORM name=writeProlog ID=writeProlog action="Manifest.jsp" method="GET">
        <INPUT type="hidden" name="kb" value=<%=kbName%>><br> 
        <INPUT type="submit" NAME="writeProlog" VALUE="writeProlog">
    </FORM>

<% } %>

<P>
  <A HREF="KBs.jsp" >Return to home page</A>
</BODY>
</HTML>