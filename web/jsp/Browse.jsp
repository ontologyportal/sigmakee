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
August 9, Acapulco, Mexico. See also http://sigmakee.sourceforge.net
*/

/**
 * Browse.jsp responds to several HTTPD parameters:
 * term     = <name>   - the SUMO term to browse
 * kb       = <name>   - the name of the knowledge base
 * lang     = <lang>   - the name of the language used to display axiom paraphrases
 * */

 String term = request.getParameter("term");
 if (!Formula.isNonEmptyString(term))
     term = "";
%>
<html>
<HEAD><TITLE>Knowledge base Browser - <%=term%></TITLE></HEAD>
<BODY BGCOLOR="#FFFFFF">

<%
 String parentPage = "Browse.jsp";

 String simple = "no";
 String kbName = "";
 String language = "";
 StringBuffer show = null;
 KB kb = null;
%>

<%@ include file="BrowseBody.jsp"%>

<!-- show KB header and KB search input -->

<%@ include file="BrowseHeader.jsp" %>

<br>
 <%=show.toString() %><BR>

<%@ include file="Postlude.jsp" %>
</BODY>
</HTML>
