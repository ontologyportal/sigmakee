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
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/
    out.println("<html>");
    out.println("  <head>");
    out.println("    <title>Sigma Knowledge Engineering Environment - Main</title>");
    out.println("  </head>");
    out.println("  <body bgcolor=\"#FFFFFF\">");

// KBmanager mgr = KBmanager.getMgr();
    String baseDir = mgr.getPref("baseDir");
    String kbDir = mgr.getPref("kbDir");
    System.out.println("INFO in KBs.jsp: baseDir == " + baseDir);
    System.out.println("INFO in KBs.jsp:   kbDir == " + kbDir);
    String rUserName = request.getParameter("userName");
    String rPassword = request.getParameter("password");
    String mUserName = mgr.getPref("userName");

    // Set a default greeting.
    String greeting = ("Welcome " + (StringUtil.isNonEmptyString(mUserName)
                                     ? mUserName
                                     : " to Sigma") + "!");

// If the user is already logged in, do nothing.

// If the user is known, log them in.

// If the user is unknown, initiate the new account creation routine
// (user role, not admin role).

// If no KBmanager object exists, create one.  If the KBmanager object
// already exists, use the existing object.

// If the KBmanager object has not been initialized, invoke
// mgr.initializeOnce(<config-file-pathname>).  If the KBmanager
// object has already been initialized, do nothing.

// FUTURE: Create a qualified user name by concatenating the browser
// name (or other data pulled from the HTTP header) with the user's
// name.  For now, only ever create one KBmanager object, but use the
// table of user names and roles/privileges to control the actions
// available to, or permitted for, each user.  NS: 09/14/2009.

    System.out.println("INFO in KBs.jsp: ************ Initializing Sigma ***************");
    KBmanager.getMgr().initializeOnce();
%>

<%
/*
    if (request.getParameter("userName") != null)
       KBmanager.getMgr().setPref("userName",Login.validateUser(request.getParameter("userName"), request.getParameter("password")));
*/

/* new Password Validation */

/*
    PasswordService psvc = PasswordService.getInstance();
    if (StringUtil.isNonEmptyString(rUserName)) {
        if (StringUtil.isNonEmptyString(rPassword) 
            && psvc.authenticate(rUserName,rPassword)) {
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
            greeting = "You are logged in as \"guest\"";
        }
    }

    
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
            greeting = "You are logged in as \"guest\"";
        }
    }
*/
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
        <td>
        <span class="navlinks">
          <b>[&nbsp;<a href="Properties.jsp">Preferences</a>&nbsp;]</b>
        </span>
        </td>
    </tr>
</table>
<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'>
<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<%
  Iterator kbNames = null;
  String remove = request.getParameter("remove");  // Delete the given KB
  if (StringUtil.isNonEmptyString(kbName) && StringUtil.isNonEmptyString(remove) && remove.equalsIgnoreCase("true"))
      KBmanager.getMgr().removeKB(kbName);

  if (KBmanager.getMgr().getKBnames() != null && !KBmanager.getMgr().getKBnames().isEmpty()) {
      System.out.println(KBmanager.getMgr().getKBnames().size());
      kbNames = KBmanager.getMgr().getKBnames().iterator();
      System.out.println("INFO in KBs.jsp: Got KB names.");
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
      String kbName2 = null;
      while (kbNames.hasNext()) {
          kbName2 = (String) kbNames.next();
          if (first) {
              defaultKB = kbName2;
              first = false;
          }
          kb = (KB) KBmanager.getMgr().getKB(kbName2);
          HTMLformatter.kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?lang=" + language;
%>
          <TR VALIGN="center" <%= odd==false? "bgcolor=#eeeeee":""%>>
            <TD><%=kbName2%></TD>
<%
          out.println("<TD>");
          if (odd) odd = false;
          if (kb.constituents == null || kb.constituents.isEmpty())
              out.println("No <A href=\"Manifest.jsp?kb=" + kbName2 + "\">constituents</A>");
          else {
              out.print("(" + kb.constituents.size() + ")&nbsp;");
              out.println("<A href=\"Manifest.jsp?kb=" + kbName2 + "\">Manifest</A>");
          }
          out.println("</TD>");          
          out.println("<TD><A href=\"Browse.jsp?kb=" + kbName2 + "&lang=" + language + "\">Browse</A></TD>");                                                      
          out.println("<TD><A href=\"Graph.jsp?kb=" + kbName2 + "&lang=" + language + "\">Graph</A></TD>");    

          if (isAdministrator) {
              out.println("<TD><A href=\"Diag.jsp?kb=" + kbName2 + "&lang=" + language + "\">Diagnostics</A></TD>");                                                 
              if (kb.inferenceEngine != null) 
                  out.println("<TD><A href=\"CCheck.jsp?kb=" + kbName2 + "&lang=" + language + "\">Consistency Check</A></TD>");               
              out.println("<TD><A HREF=\"InferenceTestSuite.jsp?test=inference&kb=" + kbName2 + "&lang=" + language + "\">Inference Tests</A></TD>");
              if (kb.celt != null) 
                  out.println("<TD><A HREF=\"InferenceTestSuite.jsp?test=english&kb=" + kbName2 + "&lang=" + language + "\">CELT Tests</A></TD>");              
              out.println("<TD><A href=\"WNDiag.jsp?kb=" + kbName2 + "&lang=" + language + "\">WordNet Check</A></TD>");
              out.println("<TD><A href=\"AskTell.jsp?kb=" + kbName2 + "\">Ask/Tell</A>&nbsp;</TD>");
              out.println("<TD><A href=\"KBs.jsp?remove=true&kb=" + kbName2 + "\">Remove</A></TD></TR>");
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
    <b>Add a new knowledge base</b>
    <form name="kbUploader" id="kbUploader" action="AddConstituent.jsp" method="POST" enctype="multipart/form-data">
  <table>
    <tr>
      <td>
        <b>KB Name:</b>&nbsp;
      </td>
      <td>
        <input type="text" name="kb" size="60">
      </td>
    </tr>
    <tr>
      <td>
        <b>KB Constituent:</b>&nbsp;
      </td>
      <td>
        <input type="file" name="constituent" size="60">
      </td>
    </tr>
  </table>
        <input type="submit" name="submit" value="Submit">
    </form><p>
<%  } 

  if (isAdministrator) {
      out.println("<a href=\"MiscUtilities.jsp?kb=" 
                  + kbName 
                  + "\">More Output Utilities</a>");
      out.println(" | <a href=\"Mapping.jsp\">Ontology Mappings</a>");
      out.println("<p>");

      kbNames = KBmanager.getMgr().getKBnames().iterator();
      String kbName3 = null;
      boolean kbErrorsFound = false;
      while (kbNames.hasNext()) {
         kbName3 = (String) kbNames.next();
         kb = (KB) KBmanager.getMgr().getKB(kbName3);
         if (!kb.errors.isEmpty()) {
             out.println("<b>Errors in KB " + kb.name + "</b><br>\n");
             kbErrorsFound = true;
         }
         out.println(HTMLformatter.formatErrors(kb,HTMLformatter.kbHref + "&kb=" + kb.name));  
         kb.errors.clear();
     }  
   
     out.println("<p>\n");
     if (KBmanager.getMgr().getError().length() > 0) {
         out.print("<b>");
         if (kbErrorsFound) out.print("Other ");
         out.println("Warnings and Error Notices</b>\n<br>\n");
         out.println(KBmanager.getMgr().getError());
         KBmanager.getMgr().setError("");
     }
  }
%>
</ul>
<p>

<%@ include file="Postlude.jsp" %>
</BODY>
</HTML>

 
