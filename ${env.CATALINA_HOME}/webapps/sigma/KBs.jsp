<%@ include file="Prelude.jsp" %>

<HTML>
<HEAD>
<TITLE>Sigma Knowledge Engineering Environment - Main</TITLE>
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
%>
</HEAD>

<BODY BGCOLOR=#FFFFFF>
<table width="95%" cellspacing="0" cellpadding="0">
    <tr>
        <td valign="top">
            <table cellspacing="0" cellpadding="0">
                <tr>
                    <td align="left" valign="top"><img src="pixmaps/sigmaSymbol.gif"></td>
                    <td>&nbsp;&nbsp;</td>
                    <td align="left" valign="top"><img src="pixmaps/logoText.gif"><BR>
                        &nbsp;&nbsp;&nbsp;</td>
                </tr>
            </table>
        </td>
        <td><font face="Arial,helvetica" SIZE=-1>
        <b>[ <A href="prefs.jsp?back=true">Prefs</b></A>&nbsp;|&nbsp;</FONT>
        <font face="Arial,helvetica" SIZE=-1> ]</b></FONT></td>
    </tr>
</table>
<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'>
<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<%
  String[] kbNames = null;
  if (KBmanager.getMgr().getKBnames() != null && KBmanager.getMgr().getKBnames().size() > 0) {
      System.out.println("Attempting first cast.");
      kbNames = (String[]) KBmanager.getMgr().getKBnames().toArray();
  }
  String defaultKB = null;
  if (kbNames == null || kbNames.length == 0)
      out.println("<H2>No Knowledge Bases loaded.</H2>");
  else {
      defaultKB = kbNames[0];
%>
      <P><TABLE border="0" cellpadding="2" cellspacing="2" NOWRAP>
           <TR>
             <TH>Knowledge Bases</TH>
           </TR>
<%
      for (int iKB = 0; iKB < kbNames.length; iKB++) {
          String kbName = kbNames[iKB];
          System.out.println("Attempting second cast.");
          KB kb = (KB) KBmanager.getMgr().getKB(kbName);
          ArrayList constituents = kb.constituents;
%>
          <TR VALIGN="center" <%= (iKB % 2)==0? "bgcolor=#eeeeee":""%>>
            <TD><img src="pixmaps/green.gif">
              &nbsp;&nbsp;</TD>
<%
          out.println("<TD>");
          if (constituents == null || constituents.size() == 0)
              out.println("No <A href=\"Manifest.jsp?kb=" + defaultKB + "\">constituents</A>");
          else {
              out.print("("+constituents.size()+")&nbsp;");
              out.println("<A href=\"Manifest.jsp?kb=" + kbName + "\">Manifest</A>");
          }
          out.println("</TD>");          
%>
            <TD>
              <font COLOR=green> <A href='Browse.jsp?kb=<%=defaultKB%>'>Browse</A></font>
            </TD>
<%
          // Load inference engine here
          if (kb.inferenceEngine == null) {
              out.println("<TD><font COLOR=green>");
              out.println("Ask_Tell"); 
          }
          else  { // inference engine is available
              out.println("<A href='AskTell.jsp?kb=" + defaultKB + "'>Ask_Tell</A>"); // inference
              out.println("&nbsp;");
          }  
          out.println("</font></TD>");
%>
            <TD><A href="">Remove</A>
            </TD>
          </TR>
<%
      }
%>
      </TABLE>
<%
  }
%>

<FORM name=kbUploader ID=kbUploader action="CreateKB.jsp" method="POST" enctype="multipart/form-data">
    <B>KB Name</B><INPUT type="TEXT" name=kbName><br> 
    <B>KB Constituent</B><INPUT type=file name=filename><BR>
    <INPUT type=submit>
</FORM>

</ul>
</BODY>
</HTML>

