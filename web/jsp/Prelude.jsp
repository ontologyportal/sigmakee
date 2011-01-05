<%@ page
   language="java"
   import="com.articulate.sigma.*,java.text.ParseException,java.net.URLConnection,java.net.URL,ClientHttpRequest.*,com.oreilly.servlet.*,com.oreilly.servlet.multipart.*,java.util.*,java.io.*, tptp_parser.*, TPTPWorld.*"
   pageEncoding="UTF-8"
   contentType="text/html;charset=UTF-8"
%>
<!DOCTYPE html
   PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en-US" xml:lang="en-US">
<%
      // System.out.println("ENTER Prelude.jsp");

  ArrayList userPages = new ArrayList();
  userPages.add("AllPictures.jsp");
  userPages.add("Browse.jsp");
  userPages.add("BrowseExtra.jsp");
  userPages.add("Graph.jsp");
  userPages.add("KBs.jsp");
  userPages.add("Manifest.jsp");
  userPages.add("OWL.jsp");
  userPages.add("SimpleBrowse.jsp");
  userPages.add("TreeView.jsp");
  userPages.add("WordNet.jsp");
  String URLString = request.getRequestURL().toString();
  String pageString = URLString.substring(URLString.lastIndexOf("/") + 1);
  //System.out.println("INFO in Prelude.jsp: calling page: " + pageString);
  //System.out.println("INFO in Prelude.jsp: userRole: " + KBmanager.getMgr().getPref("userRole"));

String userName = request.getParameter("userName");
String password = request.getParameter("password");
KBmanager mgr = KBmanager.getMgr();
if (StringUtil.isNonEmptyString(userName)) {
    mgr.setPref("userName",userName);
    if (StringUtil.isNonEmptyString(password)) {
        mgr.setPref("userRole",
                    (userName.equalsIgnoreCase("admin") && password.equalsIgnoreCase("admin"))
                    ? "administrator"
                    : "user");
    }
}
userName = mgr.getPref("userName");
String userRole = mgr.getPref("userRole");
if (StringUtil.emptyString(userName) || StringUtil.emptyString(userRole)) {
    response.sendRedirect("login.html");
    return;
}

if (mgr.initializing) {
    response.sendRedirect("init.jsp");
    return;
}
// System.out.println("ENTER Prelude.jsp");
// System.out.println("    userName == " + userName);
// System.out.println("    userRole == " + userRole);
// System.out.println("  pageString == " + pageString);
if (StringUtil.isNonEmptyString(userName)) {
    String kbName = request.getParameter("kb");
    KB kb = null;
    if (StringUtil.isNonEmptyString(kbName))
        kb = mgr.getKB(kbName);
    // System.out.println("      kbName == " + kbName);
    // System.out.println("          kb == " + kb);
}

if (!userRole.equalsIgnoreCase("administrator") && !userPages.contains(pageString)) { 
    mgr.setError("You are not authorized to visit " + pageString);
    response.sendRedirect("KBs.jsp");
    return;
}

/*
      out.println("<meta http-equiv=\"Refresh\" content=\"0;URL=login.html\">");
      out.println("</html>");
      // System.out.println("resetting (1)");
      // System.out.println("EXIT Prelude.jsp: 1");
      return;

  if (Formula.empty(KBmanager.getMgr().getPref("userName"))
      && Formula.empty(request.getParameter("userName"))
      && Formula.empty(request.getParameter("newuser"))) { 
      out.println("<meta http-equiv=\"Refresh\" content=\"0; URL=login.html\">");
      out.println("</html>");
      // System.out.println("resetting (2)");
      // System.out.println("EXIT Prelude.jsp: 2");
      return;
  }
  if (!pageString.startsWith("KBs.jsp") &&
      !pageString.startsWith("Properties.jsp") && 
      !pageString.startsWith("AddConstituent.jsp") && 
      !pageString.startsWith("AllPictures.jsp") && 
      !pageString.startsWith("Mapping.jsp") && 
      !pageString.startsWith("WordNet.jsp") && 
      !Formula.isNonEmptyString(request.getParameter("kb"))) { 
      out.println("<meta http-equiv=\"Refresh\" content=\"0; URL=KBs.jsp\">");
      out.println("</html>");
      //System.out.println("resetting (3)");
System.out.println("EXIT Prelude.jsp: 3");
      return;
  }
*/
%>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<%
/** This code is copyright Articulate Software (c) 2003-2007.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also www.ontologyportal.org
*/
if (StringUtil.isNonEmptyString(userName)) {
    String simple = request.getParameter("simple");
    if (StringUtil.isNonEmptyString(simple) && simple.equalsIgnoreCase("yes")) {
        out.println("");
        out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"simple.css\" />");
    }
}
%>
