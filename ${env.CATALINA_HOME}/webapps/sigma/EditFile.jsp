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

  // Edit a file or a statement in that file.
  // @param file: the file name that points to SigmaCtt.
  // @param stmtId: the statement Id that points to SigmaStmt.
  // @param textUpdate; the updated text

  String srcText = null;
  String action = request.getParameter("action");
  String skbName = request.getParameter("skb");
  String fileForEdit = request.getParameter("file");
  String stmtIdForEdit = request.getParameter("stmt_id");
  SigmaKB theSKB = SKBMgr.getDefaultSKBMgr().getSKB(skbName,false);

  if (action == null) { // open a new file for update  
      if (theSKB == null) {
          out.println("No such SKB");
          return;
      }
      session.setAttribute("skb",skbName);
      SigmaCtt sctt = theSKB.getSigmaCtt(fileForEdit);
      if (sctt == null) {
          out.println("No such Sigma file ");
          return;
      }
      else
          session.setAttribute("sigma_file", sctt);

      if (stmtIdForEdit != null) { // edit statement
          SigmaStmt ss = theSKB.getSigmaStmt(new SigmaStmtId(fileForEdit, Integer.parseInt(stmtIdForEdit)));
          if (ss == null ) {
              out.println(" No such Sigma Statement ");
              return;
          }
          session.setAttribute("sigma_stmt",ss);
          srcText = ss.m_exp;
      }
      else                                // edit file
          srcText = new String(sctt.getFileSourceChars());
  }
  else if (action.equalsIgnoreCase("Update")) { // update the file
      String textUpdate = request.getParameter("text_update");  // don't know whose update
      if (textUpdate == null)
          out.println(" no text to do the update ");
      SigmaStmt ss =  (SigmaStmt)session.getAttribute("sigma_stmt");
      SigmaCtt sctt = (SigmaCtt)session.getAttribute("sigma_file");
      skbName =       (String) session.getAttribute("skb");

      if (sctt == null)
          out.println("No such Sigma File to Edit");
      if (ss != null )
          ss.update(sctt, textUpdate); // try to update a statement
      else
          sctt.update(textUpdate);    // update a file
      
      theSKB.m_isSynNeeded = true;
      response.sendRedirect("home.jsp");      // finish editing
  }
%>

<HTML>
<HEAD>
<TITLE>Sigma Knowledge Engineering Environment - Editing Constituent </TITLE>
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
  <A href="edit_file.jsp?skb=<%=skbName%>&file=<%=fileForEdit%>"><%=fileForEdit%></A></B></TD>
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