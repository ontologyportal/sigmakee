<%@include file="Prelude.jsp" %>
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

  // @param file: the file name.
  // @param line: the line number in the file the cursor should point to at start.

if (!role.equalsIgnoreCase("admin"))
    response.sendRedirect("KBs.jsp");

System.out.println("ENTER EditStmt.jsp");
  String formID = request.getParameter("formID");
System.out.println("  formID == " + formID);
  String KBName = request.getParameter("kb");
System.out.println("  kbName == " + KBName);
  String text = request.getParameter("text");
System.out.println("  text == " + text);
  KB kbEdit = KBmanager.getMgr().getKB(KBName);
System.out.println("  kb == " + kbEdit.name);
  Formula form = kbEdit.getFormulaByKey(formID);
System.out.println("  form == " + form);
if ((form != null) && StringUtil.isNonEmptyString(text)) {
    form.theFormula = text;
    kbEdit.rehashFormula(form,formID);
    formID = form.createID();
}
%>

<HTML>
<HEAD>
<TITLE>Sigma Knowledge Engineering Environment - Edit Statement </TITLE>
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
    <td><b>[ <A href="KBs.jsp">Home</A> ]</b></td>
  </tr>
</table>
<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>
<script language="JavaScript">
<!--
   function update_onclick() {
       alert("in javascript");
       document.updateForm.action = "edit_file.jsp";
       document.updateForm.method = "Post";
       document.updateForm.submit();
   }

   function textAreaOnChange() {
       document.updateForm.sourceChanged.value = "true";
   }
-->
</script>

<p>
  <strong>Work in Progress</strong>
</p>

<form name="updateForm" method="POST" >
 <Table>
   <TR>
     <TD>
       <textarea rows="15" wrap="virtual" name="text" cols="100" onChange="textAreaOnChange()" disabled="disabled"><%= form.theFormula %></textarea> 
     </TD>
   </TR>     
   <TR>
     <TD align=center>
       <input type="hidden" value="<%= formID %>">
       <input type="submit" name="action" value="Update" disabled="disabled">
     </TD>
   </TR>
 </Table>
</form>

</BODY>
</HTML>
