<%@include file="Prelude.jsp" %>
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

  // @param file: the file name.
  // @param line: the line number in the file the cursor should point to at start.

if (!KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin"))         
       response.sendRedirect("KBs.jsp");     

  String file = request.getParameter("file");
  String line = request.getParameter("line");
  String srcText = "";

%>

<HTML>
<HEAD>
<TITLE>Sigma Knowledge Engineering Environment - Edit File </TITLE>
<style>@import url(kifb.css);</style>
</HEAD>
<BODY  BGCOLOR=#FFFFFF>
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
    <td><b>[ <A href="home.jsp">Home</A> ]</b></td>
  </tr>
</table>
<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>


<SCRIPT LANGUAGE=JavaScript>

   function update_onclick(){

       alert("in javascript");
       document.updateForm.action = "edit_file.jsp";
       document.updateForm.method = "Post";
       document.updateForm.submit();
   }

   function textAreaOnChange() {

       document.updateForm.sourceChanged.value = "true";
   }

</Script>

<Table>
<TR>
<TD><B> Editing Constituent:
  <A href="EditFile.jsp?file=<%=file%>"><%=file%></A></B></TD>
</TR>
<TR><TD>&nbsp;</TD></TR>
</TABLE>

<Form name="updateForm" method="POST" >
 <Table>
   <TR><TD><textarea rows="15" wrap="virtual" name="text_update" cols="100" onChange="textAreaOnChange()" >
           <%= srcText %></textarea> </TD></TR>
   <TR><TD align=center><input type = submit name = "action" value="Update"></TD></TR>
 </Table>
</Form>

</BODY>
</HTML>