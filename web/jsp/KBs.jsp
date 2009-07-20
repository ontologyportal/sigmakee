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
    // KBmanager.getMgr().initializeOnce();
    System.out.println("INFO in KB.jsp: baseDir == " + KBmanager.getMgr().getPref("baseDir"));
    System.out.println("INFO in KB.jsp:   kbDir == " + KBmanager.getMgr().getPref("kbDir"));

/*
    if (request.getParameter("userName") != null)
       KBmanager.getMgr().setPref("userName",Login.validateUser(request.getParameter("userName"), request.getParameter("password")));
*/

/* new Password Validation */

    String prefUserName = KBmanager.getMgr().getPref("userName");
    String greeting = ("Welcome " + (StringUtil.isNonEmptyString(prefUserName)
                                     ? prefUserName
                                     : ""));
    
    if (StringUtil.isNonEmptyString(request.getParameter("userName"))) {
        if (StringUtil.isNonEmptyString(request.getParameter("password")) &&
            PasswordService.getInstance().authenticate(request.getParameter("userName"),
                                                       request.getParameter("password")))
            {
                KBmanager.newMgr(request.getParameter("userName")).initializeOnce();
                greeting = ("Welcome " + KBmanager.getMgr().getPref("userRole") + 
                            " " + KBmanager.getMgr().getPref("userName"));
            }
        else if (!request.getParameter("userName").equalsIgnoreCase("guest") &&
                 StringUtil.isNonEmptyString(request.getParameter("password1")) &&
                 StringUtil.isNonEmptyString(request.getParameter("password2")) &&
                 request.getParameter("password2").equals(request.getParameter("password1"))) {

            if (!PasswordService.getInstance().userExists(request.getParameter("userName"))) {
                User newuser = new User();
                newuser.username = request.getParameter("userName");
                newuser.setRole("user");
                newuser.password = PasswordService.getInstance().encrypt(request.getParameter("password1"));
                PasswordService.getInstance().addUser(newuser);
                KBmanager.newMgr(request.getParameter("userName")).initializeOnce();
            }
            else if (PasswordService.getInstance().authenticate(request.getParameter("userName"),
                                                                request.getParameter("oldpassword"))) {
                User updateuser = PasswordService.getInstance().getUser(request.getParameter("userName"));
                updateuser.password = PasswordService.getInstance().encrypt(request.getParameter("password1"));
                PasswordService.getInstance().updateUser(updateuser);
                KBmanager.newMgr(request.getParameter("userName")).initializeOnce();
            }
        }
        else {
            KBmanager.getMgr().setPref("userName","guest");
            //out.println("Could not verify user " + request.getParameter("userName") +
            greeting = "You are logged in as guest";
        }
    }

    String hostname = KBmanager.getMgr().getPref("hostname");
    if (StringUtil.emptyString(hostname))
        hostname = "localhost";
    String port = KBmanager.getMgr().getPref("port");
    if (StringUtil.emptyString(port))
        port = "8080";
%>

			
<table width="95%" cellspacing="0" cellpadding="0">
    <tr>
        <td valign="top">
            <table cellspacing="0" cellpadding="0">
                <tr>
                    <td align="left" valign="top"><img src="pixmaps/sigmaSymbol.gif"></td>
                    <td>&nbsp;&nbsp;</td>
                    <td align="left" valign="top"><img src="pixmaps/logoText.gif"><BR>
                        <b><%=greeting%></b></td>
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
  if (StringUtil.isNonEmptyString(remove) && remove.equalsIgnoreCase("true")) 
      KBmanager.getMgr().removeKB(kbName);


  if (KBmanager.getMgr().getKBnames() != null && !KBmanager.getMgr().getKBnames().isEmpty()) {
      System.out.println(KBmanager.getMgr().getKBnames().size());
      kbNames = KBmanager.getMgr().getKBnames().iterator();
      System.out.println("INFO in KB.jsp: Got KB names.");
  }

  boolean isAdministrator = 
      (StringUtil.isNonEmptyString(KBmanager.getMgr().getPref("userRole"))
       && KBmanager.getMgr().getPref("userRole").equalsIgnoreCase("administrator"));

  String defaultKB = null;
  if (KBmanager.getMgr().getKBnames() == null || KBmanager.getMgr().getKBnames().isEmpty())
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
          String language = HTMLformatter.language;
          language = HTMLformatter.processLanguage(language,kb);
          HTMLformatter.kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?lang=" + language;
%>
          <TR VALIGN="center" <%= odd==false? "bgcolor=#eeeeee":""%>>
            <TD><%=kbName%></TD>
<%
          out.println("<TD>");
          if (odd) odd = false;
          if (kb.constituents == null || kb.constituents.isEmpty())
              out.println("No <A href=\"Manifest.jsp?kb=" + kbName + "\">constituents</A>");
          else {
              out.print("(" + kb.constituents.size() + ")&nbsp;");
              out.println("<A href=\"Manifest.jsp?kb=" + kbName + "\">Manifest</A>");
          }
          out.println("</TD>");          
          out.println("<TD><A href=\"Browse.jsp?kb=" + kbName + "&lang=" + language + "\">Browse</A></TD>");                                                      
          out.println("<TD><A href=\"Graph.jsp?kb=" + kbName + "&lang=" + language + "\">Graph</A></TD>");    

          if (isAdministrator) {

              out.println("<TD><A href=\"Diag.jsp?kb=" + kbName + "&lang=" + language + "\">Diagnostics</A></TD>");                                                 

              if (kb.inferenceEngine != null) {
                  out.println("<TD><A href=\"CCheck.jsp?kb=" + kbName + "&lang=" + language + "\">Consistency Check</A></TD>"); 
              }

              out.println("<TD><A HREF=\"InferenceTestSuite.jsp?test=inference&kb=" + kbName + "&lang=" + language + "\">Inference Tests</A></TD>");

              if (kb.celt != null) {
              out.println("<TD><A HREF=\"InferenceTestSuite.jsp?test=english&kb=" + kbName + "&lang=" + language + "\">CELT Tests</A></TD>");
              }

              out.println("<TD><A href=\"WNDiag.jsp?kb=" + kbName + "&lang=" + language + "\">WordNet Check</A></TD>");

              out.println("<TD><A href=\"AskTell.jsp?kb=" + kbName + "\">Ask/Tell</A>&nbsp;</TD>");

              out.println("<TD><A href=\"KBs.jsp?remove=true&kb=" + kbName + "\">Remove</A></TD></TR>");
          }
      }
%>
      </TABLE>
<%
  }
%>
<P>
<P>

<% 
    if (isAdministrator) { %>
    <B>Add a new knowledge base</B>
    <FORM name=kbUploader ID=kbUploader action="AddConstituent.jsp" method="POST" enctype="multipart/form-data">
        <B>KB Name</B><INPUT type="TEXT" name=kb><br> 
        <B>KB Constituent</B><INPUT type=file name=constituent><BR>
        <INPUT type="submit" NAME="submit" VALUE="Submit">
    </FORM><P>
<%  } 

  if (isAdministrator) {
      out.println("<A href=\"MiscUtilities.jsp?kb=" + kbName + "\">Other Utilities</A> |");
      out.println("<a href=\"Mapping.jsp\">Ontology Mappings</a><P>");

      kbNames = KBmanager.getMgr().getKBnames().iterator();
    
      while (kbNames.hasNext()) {
         kbName = (String) kbNames.next();
         KB kb = (KB) KBmanager.getMgr().getKB(kbName);
         if (kb.errors.size() > 0)
             out.println("<b>Errors in KB " + kb.name + "</b><br>\n");
         out.println(HTMLformatter.formatErrors(kb,HTMLformatter.kbHref + "&kb=" + kb.name));  
         kb.errors = new TreeSet();
     }  
   
     out.println("<p>\n");
     if (KBmanager.getMgr().getError().length() > 0)
         out.println("<b>Other Errors</b>\n");
     out.println(KBmanager.getMgr().getError());  
  }
%>
</ul>
<p>

<%@ include file="Postlude.jsp" %>
</BODY>
</HTML>

 
