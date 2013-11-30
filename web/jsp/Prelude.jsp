<%@ page
   language="java"
   import="com.articulate.sigma.*,com.articulate.sigma.CCheckManager.*,java.text.ParseException,java.net.URLConnection, javax.servlet.http.HttpServletRequest, java.net.URL,ClientHttpRequest.*,com.oreilly.servlet.multipart.MultipartParser,com.oreilly.servlet.multipart.Part,com.oreilly.servlet.multipart.ParamPart,com.oreilly.servlet.multipart.FilePart,java.util.*,java.io.*, tptp_parser.*, TPTPWorld.*"
   pageEncoding="UTF-8"
   contentType="text/html;charset=UTF-8"
%>
<!DOCTYPE html
   PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en-US" xml:lang="en-US">
<%

//  import="com.articulate.sigma.*,java.text.ParseException,java.net.URLConnection,javax.servlet.http.HttpServletRequest, java.net.URL,ClientHttpRequest.*,com.oreilly.servlet.*,com.oreilly.servlet.multipart.*,java.util.*,java.io.*, tptp_parser.*, TPTPWorld.*"

/** This code is copyright Articulate Software (c) 2003-2011.  Some portions
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
  ArrayList userPages = new ArrayList();
  userPages.add("AllPictures.jsp");
  userPages.add("Browse.jsp");
  userPages.add("BrowseExtra.jsp");
  userPages.add("Graph.jsp");
  userPages.add("Intersect.jsp");
  userPages.add("KBs.jsp");
  userPages.add("Manifest.jsp");
  userPages.add("OWL.jsp");
  userPages.add("OMW.jsp");
  userPages.add("SimpleBrowse.jsp");
  userPages.add("TreeView.jsp");
  userPages.add("WordNet.jsp");
  String URLString = request.getRequestURL().toString();
  String pageString = URLString.substring(URLString.lastIndexOf("/") + 1);

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

if (!userRole.equalsIgnoreCase("administrator") && !userPages.contains(pageString)) { 
    mgr.setError("You are not authorized to visit " + pageString);
    response.sendRedirect("KBs.jsp");
    return;
}

%>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<%
if (StringUtil.isNonEmptyString(userName)) {
    String simple = request.getParameter("simple");
    if (StringUtil.isNonEmptyString(simple) && simple.equalsIgnoreCase("yes")) {
        out.println("");
        out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"simple.css\" />");
    }
}

 String kbName = request.getParameter("kb");
 if (kbName == null || StringUtil.emptyString(kbName)) 
     kbName = "SUMO";

 KB kb = null;
 if (kbName != null && StringUtil.isNonEmptyString(kbName)) {
     kb = KBmanager.getMgr().getKB(kbName);
     if (kb != null)
         TaxoModel.kbName = kbName;
 }
 else
     response.sendRedirect("login.html");

 String language = ""; // natural language for NL generation
 String flang = request.getParameter("flang");    // formal language
 flang = HTMLformatter.processFormalLanguage(flang);
 language = request.getParameter("lang");
 language = HTMLformatter.processNaturalLanguage(language,kb);
 String hostname = KBmanager.getMgr().getPref("hostname");
 if (hostname == null)
     hostname = "localhost";
 String port = KBmanager.getMgr().getPref("port");
 if (port == null)
     port = "8080";
%>
