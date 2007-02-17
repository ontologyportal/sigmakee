<%@ page
    language="java"
    import="com.articulate.sigma.*,java.text.ParseException,java.net.URLConnection,java.net.URL,ClientHttpRequest.*,com.oreilly.servlet.*,java.util.*,java.io.*"    
    pageEncoding="UTF-8"
    contentType="text/html;charset=UTF-8"
%>

<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<%
 if (KBmanager.getMgr().getPref("userName") == null && request.getParameter("userName") == null) { %>      

 <META HTTP-EQUIV="Refresh" CONTENT="0; URL=login.html">
 <%
       // response.setContentLength(0);
       // response.sendRedirect("login.html");     
 }
%>

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
%>


