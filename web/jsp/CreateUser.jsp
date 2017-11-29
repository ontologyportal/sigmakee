<%@ page language="java" contentType="text/html; charset=US-ASCII"
    import="com.articulate.sigma.KBmanager,com.articulate.sigma.StringUtil,java.net.URLConnection, javax.servlet.http.HttpServletRequest, java.io.*"
    pageEncoding="US-ASCII"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
<title>CreateUser</title>
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

KBmanager.getMgr().initializeOnce();
String firstName = request.getParameter("firstName");
String lastName = request.getParameter("lastName");
String userName = request.getParameter("userName");
String password = request.getParameter("password");
String organization = request.getParameter("organization");
String email = request.getParameter("email");
String notRobot = request.getParameter("notRobot");

if (StringUtil.emptyString(firstName) ||
    StringUtil.emptyString(lastName) ||
    StringUtil.emptyString(userName) ||
    StringUtil.emptyString(password) ||
    StringUtil.emptyString(organization) ||
    StringUtil.emptyString(email)  ||
    StringUtil.emptyString(notRobot)) {
	response.sendRedirect("login.html");
}
else {
    PasswordService ps = new PasswordService();
    User u = new User();
    u.username = userName;
    u.password = password;
    u.role = "guest";
    u.attributes.put("organization",organization);
    u.attributes.put("email",email);
    u.attributes.put("firstName",firstName);
    u.attributes.put("lastName",lastName);
    u.attributes.put("registrId",ps.encrypt(Long.valueOf(System.currentTimeMillis()).toString()));
    u.toDB(ps.conn);
    ps.mailModerator(u);
    response.sendRedirect("KBs.jsp");
}
%>