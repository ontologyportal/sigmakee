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

    String userName = request.getParameter("userName");
    String password = request.getParameter("password");
    String newUser = request.getParameter("newUser");
    String addUser = request.getParameter("addUser");
    Delphi delphi = Delphi.getInstance();

    if (newUser != null) {                                              // New User
        if (userName != null) {
            if (password != null) {
                if (PasswordService.getInstance().userExists(userName)) {
                    out.print("<META HTTP-EQUIV=\"Refresh\" CONTENT=\"0; URL=dlogin.jsp?userName=");
                    out.print(userName);
                    out.print("&message=usernameAlreadyExists\">");
                }
                else {
                    User user = new User();
                    user.username = userName;
                    user.password = PasswordService.getInstance().encrypt(password);
                    //System.out.println("INFO in Projects.jsp: PAssword " + user.password);
                    PasswordService.getInstance().addUser(user);
                    session.putValue("userName",user.username);
                    out.print("<META HTTP-EQUIV=\"Refresh\" CONTENT=\"0; URL=AddUser.jsp\">");
                }
            }
            else {
                out.print("<META HTTP-EQUIV=\"Refresh\" CONTENT=\"0; URL=dlogin.jsp\">");
            }
        }
        else {
            out.print("<META HTTP-EQUIV=\"Refresh\" CONTENT=\"0; URL=dlogin.jsp\">");
        }
    }
    else { 
        User user = null;
        if (addUser != null) {
            user = PasswordService.getInstance().getUser((String) session.getValue("userName"));
            for (Enumeration e = request.getParameterNames() ; e.hasMoreElements() ;) {
                String attribute = (String) e.nextElement();
                if (!attribute.equals("userName") && 
                    !attribute.equals("password") &&
                    !attribute.equals("addUser") &&
                    !attribute.equals("project") &&
                    !attribute.equals("newUser")) {
                    String value = request.getParameter(attribute);
                    user.attributes.put(attribute,value);
                }
            }
            PasswordService.getInstance().updateUser(user);
        }
        else {                                                                 // not a new user
            if (PasswordService.getInstance().authenticate(userName, password)) {     // just logging in
                session.putValue("userName",userName);
            }
            else {
                if (userName != null && userName != "") {
                    out.print("<META HTTP-EQUIV=\"Refresh\" CONTENT=\"0; URL=dlogin.jsp\">");
                }
            }

            if (session.getValue("userName") != null) {                              // already logged in                   
                user = PasswordService.getInstance().getUser((String) session.getValue("userName")); 
            }
            else {
                out.print("<META HTTP-EQUIV=\"Refresh\" CONTENT=\"0; URL=dlogin.jsp\">");
            }
        }
%>
        <script type='text/javascript'>
            var xChildWindow = null;
            function xWinOpen(sUrl) {
                var features = "left=0,top=0,width=600,height=500,location=0,menubar=0," +
                               "resizable=1,scrollbars=1,status=0,toolbar=0";
                xChildWindow = window.open(sUrl, "myWinName", features);
                xChildWindow.focus();
                return false;
            }
        </SCRIPT>
<%   
        if (user != null) {
            out.println("Welcome user " + user.username + "<P>\n");
            out.println("<B>Projects:</B><P>");
            Iterator it = delphi.projects.keySet().iterator();
            while (it.hasNext()) {
                String name = (String) it.next();
                Project p = (Project) delphi.projects.get(name);
                out.println("<A HREF=\"Project.jsp?name=" + p.name + 
                            "\"onclick=\"xWinOpen('popup.jsp?project=" + p.name + "');window.location=href;return false\">" + 
                            p.name + "</A><BR>");
            }
            out.println("<P>");
            out.println("Select a project to begin.  A project allows you to view the decisions and criteria of others,");
            out.println("and to specify decisions, criteria and weights yourself.");
            out.println("<HR width=300><P>");
            out.println("<A href=\"addProject.jsp\">Add Project</A><P>");
        }
    }
%>

