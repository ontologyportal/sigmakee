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
    String projectName = request.getParameter("name");
    Delphi delphi = Delphi.getInstance();
    String newRow = request.getParameter("newRow");
    String save = request.getParameter("save");
    String newColumn = request.getParameter("newColumn");
    String text = request.getParameter("text");
    // String username = PasswordService.getInstance().currentUser.username;
    String username = (String) session.getValue("userName");

    //Enumeration names = session.getAttributeNames();
    //while (names.hasMoreElements()) {
        //String elem = (String) names.nextElement();
        //System.out.println(elem);
    //}
    //System.out.println("INFO in Project.jsp: userName " + username);

    //System.out.println("INFO in Project.jsp: ID " + session.getId());
    //System.out.println("INFO in Project.jsp: New? " + session.isNew());

    for (Enumeration e = request.getParameterNames() ; e.hasMoreElements() ;) {
        String rowcolumn = (String) e.nextElement();
        //System.out.println("INFO in Project.jsp: parameter: " + rowcolumn);
        if (rowcolumn.indexOf("!") != -1) {
            String value = request.getParameter(rowcolumn);
            rowcolumn = HTMLformatter.decodeFromURL(rowcolumn);
            delphi.setValue(projectName,username,rowcolumn,value);
        }
        else {
            ArrayList columns = delphi.getColumnNames(projectName,username);
            String value = request.getParameter(rowcolumn);
            rowcolumn = HTMLformatter.decodeFromURL(rowcolumn);
            //System.out.println("INFO in Project.jsp: columns: " + columns);
            //System.out.println("INFO in Project.jsp: value: " + value);
            if (columns.contains(rowcolumn)) { 
                //System.out.println("INFO in Project.jsp: changing weight for: " + rowcolumn);
                delphi.setWeight(projectName,username,rowcolumn,value);            
            }
        }
    }
    if (save != null || text != null) {
        try {
            delphi.save();
        }
        catch (java.io.IOException e) {
            System.out.println("Error in Project.jsp: Error writing project file. " + e.getMessage());
        }
    }
    if (newRow != null && text != null && text.length() > 0) 
        delphi.addRow(username,projectName,text);        
    if (newColumn != null && text != null && text.length() > 0) 
        delphi.addColumn(username,projectName,text);        
    
    Project project = (Project) delphi.projects.get(projectName);
    if (project == null)
        out.println("Error in Project.jsp: No such project " + projectName);
    else {
        out.println("<H2>" + project.name + "</H2>\n");
        out.println("<B>Average of all other users' judgements:</B>\n");
        TableAverage averageTable = project.average(username);
        out.println(averageTable.toTable().toHTML(true,projectName,null) + "<P>\n<HR>\n");
%>
        <FORM METHOD="POST"  ACTION="Project.jsp">
<%
        if (project.tables.keySet().contains(username)) {
            Table table = (Table) project.tables.get(username);            
            out.println("User: " + username + "<P>\n");
            out.println(table.toHTML(false,projectName,averageTable) + "<P>\n");
        }
        else {
            out.println("User: " + username + "<P>\n");
            out.println("<B>Empty table</B><P>\n");
        }
%>
        <B>New row or column:</B><INPUT NAME="text" TYPE="TEXT" MAXLENGTH="30" SIZE="10"><P>
        <INPUT name="newRow" VALUE="New row" TYPE="SUBMIT">
        <INPUT name="newColumn" VALUE="New column" TYPE="SUBMIT">
        <INPUT name="save" VALUE="save" TYPE="SUBMIT">
        <input TYPE="hidden" NAME="name" VALUE=<%="\"" + projectName + "\""%>>
        </FORM><P>
        Rows specify individual decisions.  Columns are used to specify different criteria that impact a decision.
        The weights in the top row allow the user to specify how important a critereon is with respect to a given
        decision.  The numbers in the body of the table state the degree to which a choice fulfills a given critereon.<P>
<%
    }
%>

