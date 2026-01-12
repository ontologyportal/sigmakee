<%@ page language="java" contentType="text/html; charset=US-ASCII"
import="com.articulate.sigma.*,java.net.URLConnection, javax.servlet.http.HttpServletRequest, java.io.*"
    pageEncoding="US-ASCII"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
<title>Approve User</title>
</head>
<body>

<%
/** This code is copyright Teknowledge (c) 2003, Articulate Software (c) 2003-2017,
    Infosys (c) 2017-present.

    This software is released under the GNU Public License
    <http://www.gnu.org/copyleft/gpl.html>.

    Please cite the following article in any publication with references:

    Pease A., and Benzm?ller C. (2013). Sigma: An Integrated Development Environment
    for Logical Theories. AI Communications 26, pp79-97.  See also
    http://github.com/ontologyportal
*/

String role = (String) session.getAttribute("role");
if (!role.equals("admin")) {
    response.sendRedirect("login.html");
    return;
}

String user = request.getParameter("user");

PasswordService ps = new PasswordService();
User u = User.fromDB(ps.conn, user);
u.role = "user";
u.toDB(ps.conn);
%>

<b>User approved.</b><P>

<form method="post" action="KBs.jsp">
<p>

<table align="left" border="0" >
  <TR>
    <TD VALIGN=CENTER>
	  <B><INPUT VALUE="Ok" TYPE="SUBMIT"></B>
    </TD>
  </TR>
</table>
</form>

</body>
</html>