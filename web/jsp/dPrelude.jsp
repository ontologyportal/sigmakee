
<%@ page
    language="java"
    import="com.articulate.sigma.*,com.articulate.delphi.*,java.util.*,java.io.*"
%>

<%
    if (session.getValue("userName") == null && request.getParameter("userName") == null) { 
        System.out.println("INFO in dPrelude.jsp: Redirecting to dlogin.jsp");
%>      

        <META HTTP-EQUIV="Refresh" CONTENT="0; URL=dlogin.jsp">
<%
    }
%>

<%
/** This code is copyright Articulate Software (c) 2005.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/
%>


