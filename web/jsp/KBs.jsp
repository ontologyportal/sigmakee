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
<%
    System.out.println("INFO in KBs.jsp: ************ Initializing Sigma ***************");
    KBmanager.getMgr().initializeOnce();
    System.out.println("INFO in KB.jsp: baseDir == " + KBmanager.getMgr().getPref("baseDir"));
    System.out.println("INFO in KB.jsp:   kbDir == " + KBmanager.getMgr().getPref("kbDir"));
    if (request.getParameter("userName") != null)
       KBmanager.getMgr().setPref("userName",Login.validateUser(request.getParameter("userName"), request.getParameter("password")));
%>

			
<table width="95%" cellspacing="0" cellpadding="0">
    <tr>
        <td valign="top">
            <table cellspacing="0" cellpadding="0">
                <tr>
                    <td align="left" valign="top"><img src="pixmaps/sigmaSymbol.gif"></td>
                    <td>&nbsp;&nbsp;</td>
                    <td align="left" valign="top"><img src="pixmaps/logoText.gif"><BR>
                        <B>Welcome  <%=KBmanager.getMgr().getPref("userName")%></B></td>
                </tr>
                
            </table>
        </td>
        <td><font face="Arial,helvetica" SIZE=-1>
        <b>[ <A href="Properties.jsp">Preferences</b></A>&nbsp;</FONT>
        <font face="Arial,helvetica" SIZE=-1> ]</b></FONT></td>
    </tr>
</table>
<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'>
<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<%
  Iterator kbNames = null;
  String kbName = request.getParameter("kb");
  String remove = request.getParameter("remove");  // Delete the given KB
  if (remove != null && remove.equalsIgnoreCase("true")) 
      KBmanager.getMgr().removeKB(kbName);


  if (KBmanager.getMgr().getKBnames() != null && KBmanager.getMgr().getKBnames().size() > 0) {
      System.out.println(KBmanager.getMgr().getKBnames().size());
      kbNames = KBmanager.getMgr().getKBnames().iterator();
      System.out.println("INFO in KB.jsp: Got KB names.");
  }
  String defaultKB = null;
  if (KBmanager.getMgr().getKBnames() == null || KBmanager.getMgr().getKBnames().size() == 0)
      out.println("<H2>No Knowledge Bases loaded.</H2>");
  else {
%>
      <P><TABLE border="0" cellpadding="2" cellspacing="2" NOWRAP>
           <TR>
             <TH>Knowledge Bases</TH>
           </TR>
<%
      System.out.println("Showing knowledge bases.");
      boolean first = true;
      boolean odd = true;
      while (kbNames.hasNext()) {
          kbName = (String) kbNames.next();
          if (first) {
              defaultKB = kbName;
              first = false;
          }
          KB kb = (KB) KBmanager.getMgr().getKB(kbName);
%>
          <TR VALIGN="center" <%= odd==false? "bgcolor=#eeeeee":""%>>
            <TD><%=kbName%></TD>
<%
          out.println("<TD>");
          if (odd) odd = false;
          if (kb.constituents == null || kb.constituents.size() == 0)
              out.println("No <A href=\"Manifest.jsp?kb=" + kbName + "\">constituents</A>");
          else {
              out.print("(" + kb.constituents.size() + ")&nbsp;");
              out.println("<A href=\"Manifest.jsp?kb=" + kbName + "\">Manifest</A>");
          }
          out.println("</TD>");          
          out.println("<TD><A href=\"Browse.jsp?kb=" + kbName + "&lang=en\">Browse</A></TD>");                                                      
          out.println("<TD><A href=\"Graph.jsp?kb=" + kbName + "&lang=en\">Graph</A></TD>");                                                      
          out.println("<TD><A href=\"Diag.jsp?kb=" + kbName + "&lang=en\">Diagnostics</A></TD>");                                                 
          if (kb.inferenceEngine != null && KBmanager.getMgr().getPref("userName") != null && 
              KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin")) {
              out.println("<TD><A href=\"CCheck.jsp?kb=" + kbName + "&lang=en\">Consistency Check</A></TD>"); 
	  }
	  else {
              out.println("<TD>Consistency Check</TD>"); 
	  }
          if (kb.inferenceEngine != null && KBmanager.getMgr().getPref("userName") != null && 
              KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin")) {
              out.println("<TD><A HREF=\"InferenceTestSuite.jsp?test=inference&kb=" + kbName + "&lang=en\">Inference Tests</A></TD>");
	  }
	  else {
              out.println("<TD>Inference Tests</TD>");
	  }
          if (kb.celt != null && KBmanager.getMgr().getPref("userName") != null && 
              KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin")) {
              out.println("<TD><A HREF=\"InferenceTestSuite.jsp?test=english&kb=" + kbName + "&lang=en\">CELT Tests</A></TD>");
	  }
	  else {
              out.println("<TD>CELT Tests</TD>");
	  }
          if (KBmanager.getMgr().getPref("userName") != null && 
              KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin")) {
              out.println("<TD><A href=\"WNDiag.jsp?kb=" + kbName + "&lang=en\">WordNet Check</A></TD>");                                                           
	  }
	  else {
              out.println("<TD>WordNet Check</TD>");
	  }
          out.println("<TD><A href=\"AskTell.jsp?kb=" + kbName + "\">Ask/Tell</A>&nbsp;</TD>");

          if (KBmanager.getMgr().getPref("userName") != null 
	      && KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin")) {
              out.println("<TD><A href=\"KBs.jsp?remove=true&kb=" + kbName + "\">Remove</A></TD></TR>");
	  }
	  else {
              out.println("<TD>Remove</TD></TR>");
	  }
      }
%>
      </TABLE>
<%
  }
%>
<P>
<P>

<% if (KBmanager.getMgr().getPref("userName") != null && KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin")) { %>
    <B>Add a new knowledge base</B>
    <FORM name=kbUploader ID=kbUploader action="AddConstituent.jsp" method="POST" enctype="multipart/form-data">
        <B>KB Name</B><INPUT type="TEXT" name=kb><br> 
        <B>KB Constituent</B><INPUT type=file name=constituent><BR>
        <INPUT type="submit" NAME="submit" VALUE="Submit">
    </FORM><P>
<%  } 

  if (KBmanager.getMgr().getError() != "") {
      out.println(KBmanager.getMgr().getError());  
      KBmanager.getMgr().setError("");
  }
%>
</ul>
<p>

<%@ include file="Postlude.jsp" %>
</BODY>
</HTML>

