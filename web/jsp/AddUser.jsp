
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
    if (session.getValue("userName") != null) {        
        User user = PasswordService.getInstance().getUser((String) session.getValue("userName"));
        out.println("Welcome new user " + user.username + "<P>\n");
%>


<FORM METHOD="POST"  ACTION="Projects.jsp">
<table>
<tr><td><B>User name: </td><td><%=user.username%></B></td></tr>
<tr><td><B>Email address</B></td><td><INPUT NAME="email" TYPE="TEXT" MAXLENGTH="30" SIZE="10"></td></tr>
<tr><td><B>University name</B></td><td><INPUT NAME="university" TYPE="TEXT" MAXLENGTH="30" SIZE="10"></td></tr>
<tr><td><B>Home town</B></td><td><INPUT NAME="town" TYPE="TEXT" MAXLENGTH="30" SIZE="10"></td></tr>
<tr><td><B>Home town distance from university</B></td><td><INPUT NAME="distance" TYPE="TEXT" MAXLENGTH="30" SIZE="10"></td></tr>
<tr><td><B>Major course of study</B></td><td><INPUT NAME="major" TYPE="TEXT" MAXLENGTH="30" SIZE="10"></td></tr>
<tr><td><B>Degrees</B></td><td><INPUT NAME="degrees" TYPE="TEXT" MAXLENGTH="30" SIZE="10"></td></tr>
<tr><td><B>Years of education</B></td><td><INPUT NAME="years" TYPE="TEXT" MAXLENGTH="30" SIZE="10"></td></tr>
<tr><td><B>Age</B></td><td><INPUT NAME="age" TYPE="TEXT" MAXLENGTH="30" SIZE="10"></td></tr>
</TABLE><P>
<input TYPE="hidden" NAME="userName" VALUE=<%="\"" + user.username + "\""%>>
<input TYPE="hidden" NAME="addUser" VALUE="yes">
<INPUT name="submit" VALUE="submit" TYPE="SUBMIT">
</FORM>

<%
    }      
    else
        out.print("<META HTTP-EQUIV=\"Refresh\" CONTENT=\"0; URL=dlogin.jsp\">");
%>

