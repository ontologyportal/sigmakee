

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
%>

<%
    String projectName = request.getParameter("projectName");
    String description = request.getParameter("description");
    Delphi delphi = Delphi.getInstance();

    if (projectName != null && projectName.length() != 0) {
        Project newProject = new Project();
        newProject.name = projectName;
        newProject.description = description;
        delphi.projects.put(projectName,newProject);

        try {
            delphi.save();
        }
        catch (java.io.IOException e) {
            System.out.println("Error in addProject.jsp: Error writing project file. " + e.getMessage());
        }
    }

    out.println("<B>Projects:</B><P>");
    Iterator it = delphi.projects.keySet().iterator();
    while (it.hasNext()) {
        String name = (String) it.next();
        Project p = (Project) delphi.projects.get(name);
        out.println("<A HREF=\"Project.jsp?name=" + p.name + "\">" + p.name + "</A><BR>");
    }
%>

<P>
<FORM METHOD="POST"  ACTION="addProject.jsp">
<B>New project name:</B><INPUT NAME="projectName" TYPE="TEXT" MAXLENGTH="10" SIZE="10"><P>
<B>Description</B><P>
<textarea NAME="description" TYPE=textarea rows=20 cols=80></TEXTAREA><P>
<CENTER><B><INPUT name="newProject" VALUE="New project" TYPE="SUBMIT"></B></center>
</FORM><P>
              
