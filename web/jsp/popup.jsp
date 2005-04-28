
<%@ include file="dPrelude.jsp" %>

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
    String projectName = request.getParameter("project");
    Delphi delphi = Delphi.getInstance();
    out.println("<h2>" + projectName + "</h2>");
    Project p = (Project) delphi.projects.get(projectName);
    if (p.description != null) {
        out.println(p.description);
    }
    out.println("<P>");
%>

