<%@ page language="java" contentType="text/html; charset=US-ASCII"
import="com.articulate.sigma.*"
    pageEncoding="US-ASCII"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
<title>login</title>
</head>
<body>

<%
/** This code is copyright Teknowledge (c) 2003, Articulate Software (c) 2003-2017,
    Infosys (c) 2017-present.

    This software is released under the GNU Public License
    <http://www.gnu.org/copyleft/gpl.html>.

    Please cite the following article in any publication with references:

    Pease A., and Benzmüller C. (2013). Sigma: An Integrated Development Environment
    for Logical Theories. AI Communications 26, pp79-97.  See also
    http://github.com/ontologyportal
*/

String userName = request.getParameter("userName");
String password = request.getParameter("password");

PasswordService ps = new PasswordService();
if (ps.userExists(userName)) {
    User u = User.fromDB(ps.conn,userName);
    if (ps.encrypt(password).equals(u.password)) {
        session.setAttribute("user",u.username);
        session.setAttribute("role",u.role);
        ServletContext siblingContext = request.getSession().getServletContext().getContext("/sigma");
        siblingContext.setAttribute("user",u.username);
        siblingContext.setAttribute("role",u.role);
        System.out.println("login.jsp: Set sibling context");
        System.out.println("login.jsp: Successful login for " + u.username + " with role " + u.role);
        response.sendRedirect("KBs.jsp");
    }
    else {
        System.out.println("Bad login attempt in login.jsp - no matching password for " + u.username);
        response.sendRedirect("login.html");
    }
}
else {
    System.out.println("Bad login attempt in login.jsp - no such user: " + userName);
    response.sendRedirect("login.html");
}
%>

</body>
</html>