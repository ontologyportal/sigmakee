<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
    import="com.articulate.sigma.user.User,
            com.articulate.sigma.user.UserManager,
            com.articulate.sigma.user.EmailService,
            com.articulate.sigma.utils.StringUtil,
            com.articulate.sigma.security.ValidationUtils" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Approve User</title>
</head>
<body>
<%
/**
This code is copyright Teknowledge (c) 2003, Articulate Software (c) 2003-2017,
Infosys (c) 2017-present.

This software is released under the GNU Public License
<http://www.gnu.org/copyleft/gpl.html>.

Please cite the following article in any publication with references:

Pease A., and Benzm?ller C. (2013). Sigma: An Integrated Development Environment
for Logical Theories. AI Communications 26, pp79-97.  See also
http://github.com/ontologyportal
*/
EmailService emailService = new EmailService();
String role = (String) session.getAttribute("role");
if (StringUtil.emptyString(role) || !"admin".equals(role)) {
    response.sendRedirect("login.jsp");
    return;
}
String username = request.getParameter("user");
if (StringUtil.emptyString(username)) {
    out.println("<b>Missing user.</b>");
    return;
}
username = username.trim();
UserManager userManager = new UserManager();
User user = userManager.getUser(request, username);
if (user == null) {
    out.println("<b>No user found for username: " +
        ValidationUtils.sanitizeString(username) + "</b>");
    return;
}
String method = request.getMethod();
if ("POST".equalsIgnoreCase(method)) {
    boolean approved = userManager.updateUserRole(request, username, "user");
    if (approved) {
        emailService.sendAccountApprovedNotification(user);
%>
    <b>User approved.</b>
    <p>
    User <b><%= ValidationUtils.sanitizeString(username) %></b> now has role <b>user</b>.
    </p>
    <form method="post" action="KBs.jsp">
        <p>
        <table align="left" border="0">
            <tr>
                <td valign="center">
                    <b><input value="Ok" type="submit"></b>
                </td>
            </tr>
        </table>
    </form>
<%
    }
    else out.println("<b>Unable to approve user.</b>");
    return;
}
String safeUsername = ValidationUtils.sanitizeString(user.getUsername());
String safeFirstName = ValidationUtils.sanitizeString(user.getFirstName());
String safeLastName = ValidationUtils.sanitizeString(user.getLastName());
String safeEmail = ValidationUtils.sanitizeString(user.getEmail());
String safeOrganization = ValidationUtils.sanitizeString(user.getOrganization());
String safeRole = ValidationUtils.sanitizeString(user.getRole());
%>
<h2>Approve User</h2>
<p>Please confirm that you want to approve this registration request.</p>
<table border="0">
    <tr>
        <td align="right"><b>Username:</b></td>
        <td><%= safeUsername %></td>
    </tr>
    <tr>
        <td align="right"><b>First name:</b></td>
        <td><%= safeFirstName %></td>
    </tr>
    <tr>
        <td align="right"><b>Last name:</b></td>
        <td><%= safeLastName %></td>
    </tr>
    <tr>
        <td align="right"><b>Email:</b></td>
        <td><%= safeEmail %></td>
    </tr>
    <tr>
        <td align="right"><b>Organization:</b></td>
        <td><%= safeOrganization %></td>
    </tr>
    <tr>
        <td align="right"><b>Current role:</b></td>
        <td><%= safeRole %></td>
    </tr>
</table>
<form method="post" action="ApproveUser.jsp">
    <input type="hidden" name="user" value="<%= safeUsername %>">
    <p>
        <b><input value="Approve" type="submit"></b>
    </p>
</form>
</body>
</html>