<%@ page
   language="java"
   import="com.articulate.sigma.*,java.text.ParseException,java.net.URLConnection,java.net.URL,ClientHttpRequest.*,com.oreilly.servlet.*,java.util.*,java.io.*, tptp_parser.*, TPTPWorld.*"
   pageEncoding="UTF-8"
   contentType="text/html;charset=UTF-8"
%>
<!DOCTYPE html
   PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en-US" xml:lang="en-US">
<%
  ArrayList userPages = new ArrayList();
  userPages.add("AllPictures.jsp");
  userPages.add("Browse.jsp");
  userPages.add("BrowseExtra.jsp");
  userPages.add("Graph.jsp");
  userPages.add("KBs.jsp");
  userPages.add("Manifest.jsp");
  userPages.add("SimpleBrowse.jsp");
  userPages.add("TreeView.jsp");
  userPages.add("WordNet.jsp");
  String URLString = request.getRequestURL().toString();
  String pageString = URLString.substring(URLString.lastIndexOf("/") + 1);
  //System.out.println("INFO in Prelude.jsp: calling page: " + pageString);
  //System.out.println("INFO in Prelude.jsp: userRole: " + KBmanager.getMgr().getPref("userRole"));
  if (KBmanager.getMgr().getPref("userRole") != null && 
      !KBmanager.getMgr().getPref("userRole").equalsIgnoreCase("administrator") && !userPages.contains(pageString)) { 
      out.println("<meta http-equiv=\"Refresh\" content=\"0;URL=login.html\">");
      out.println("</html>");
      //System.out.println("resetting (1)");
      return;
  }
  if (Formula.empty(KBmanager.getMgr().getPref("userName"))
      && Formula.empty(request.getParameter("userName"))
      && Formula.empty(request.getParameter("newuser"))) { 
      out.println("<meta http-equiv=\"Refresh\" content=\"0; URL=login.html\">");
      out.println("</html>");
      //System.out.println("resetting (2)");
      return;
  }
  if (!pageString.startsWith("KBs.jsp") &&
      !pageString.startsWith("Properties.jsp") && 
      !pageString.startsWith("AddConstituent.jsp") && 
      !pageString.startsWith("AllPictures.jsp") && 
      !pageString.startsWith("WordNet.jsp") && 
      !Formula.isNonEmptyString(request.getParameter("kb"))) { 
      out.println("<meta http-equiv=\"Refresh\" content=\"0; URL=KBs.jsp\">");
      out.println("</html>");
      //System.out.println("resetting (3)");
      return;
  }
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
  String smpl = request.getParameter("simple");
  if (Formula.isNonEmptyString(smpl) && smpl.equals("yes")) {
%>
      <link rel="stylesheet" type="text/css" href="simple.css" />
<%
  }
%>
