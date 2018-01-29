<%@ include file="Prelude.jsp" %>
<html>
<%
String nonRelTerm = request.getParameter("nonrelation");
String relTerm = request.getParameter("relation");
String relREmatch = request.getParameter("relREmatch");
String nonRelREmatch = request.getParameter("nonRelREmatch");
String KBPOS = request.getParameter("KBPOS");
 
 if (StringUtil.emptyString(term))
     term = "";
 if (StringUtil.emptyString(nonRelTerm))
 	nonRelTerm = "";
 if (StringUtil.emptyString(relTerm))
    relTerm = "";

%>
<HEAD><!-- link rel="stylesheet" type="text/css" href="simple.css" /-->
    <TITLE>Simple Knowledge base Browser - <%=term%></TITLE></HEAD>
<BODY BGCOLOR="#FFFFFF">

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
August 9, Acapulco, Mexico. See also http://github.com/ontologyportal
*/

/**
 * SimpleBrowse.jsp responds to several HTTPD parameters:
 * term     = <name>   - the SUMO term to browse
 * kb       = <name>   - the name of the knowledge base
 * lang     = <lang>   - the name of the language used to display axiom paraphrases
 * */
%>

<%
 String parentPage = "SimpleBrowse.jsp";
 StringBuffer show = null;
 simple = "yes";
%>

<%@ include file="SimpleBrowseBody.jsp" %>

<!-- show KB header and KB search input -->

<%@ include file="SimpleBrowseHeader.jsp" %>

<br>
 <%=show.toString() %><BR>

<%@ include file="Postlude.jsp" %>
</BODY>
</HTML>
