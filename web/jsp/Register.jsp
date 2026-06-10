<%@ page language="java" contentType="text/html; charset=US-ASCII" pageEncoding="US-ASCII"%>
<%@ page import="com.articulate.sigma.user.UserManager" %>
<%@ page import="com.articulate.sigma.utils.StringUtil" %>
<%@ page import="com.articulate.sigma.utils.ValidationUtils" %>

<%
    String error = null;
    String success = null;

    if ("true".equals(request.getParameter("registered"))) success = "Your account creation was successful! You will receive an email upon admin approval.";
    if ("POST".equalsIgnoreCase(request.getMethod())) {
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String username = request.getParameter("userName");
        String password = request.getParameter("password");
        String organization = request.getParameter("organization");
        String email = request.getParameter("email");
        String notRobot = request.getParameter("notRobot");
        if (StringUtil.emptyString(firstName) ||
            StringUtil.emptyString(lastName) ||
            StringUtil.emptyString(username) ||
            StringUtil.emptyString(password) ||
            StringUtil.emptyString(organization) ||
            StringUtil.emptyString(email) ||
            StringUtil.emptyString(notRobot)) {
            error = "Please fill out all required fields.";
        }
        else if (!username.matches("[A-Za-z0-9_-]+")) error = "Username can only contain letters, numbers, hyphens, and underscores.";
        else {
            UserManager userManager = (UserManager) application.getAttribute("userManager");
            boolean created = userManager.registerGuest(
                username.trim(),
                password,
                email.trim(),
                firstName.trim(),
                lastName.trim(),
                organization.trim(),
                notRobot.trim()
            );
            if (created) {
                response.sendRedirect("Register.jsp?registered=true");
                return;
            }
            else {
                error = "That username or email may already be registered.";
            }
        }
    }
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
        <title>Register</title>
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
        %>
        <% if (error != null) { %>
            <div style="color:red;">
                <%= ValidationUtils.sanitizeString(error) %>
            </div>
        <% } %>
        <% if (success != null) { %>
            <div class="toplink">
                <a href="KBs.jsp">&larr; Home</a>
            </div>
            <div style="color:green;">
                <%= ValidationUtils.sanitizeString(success) %>
            </div>
        <% } 
            else {
        %>
        <form method="post" action="Register.jsp">
            <p>
            <table align="left" border="0">
                <tr>
                    <td colspan="2">
                        All fields are required<p>
                    </td>
                </tr>
                <tr>
                    <td valign="top" align="right">
                        <b>first/given name:</b>
                    </td>
                    <td valign="top">
                        <b><input name="firstName" type="text" maxlength="20" size="10"></b>
                    </td>
                </tr>
                <tr>
                    <td valign="top" align="right">
                        <b>last/surname:</b>
                    </td>
                    <td valign="top">
                        <b><input name="lastName" type="text" maxlength="20" size="10"></b>
                    </td>
                </tr>
                <tr>
                    <td valign="top" align="right">
                        <b>User name:</b>
                    </td>
                    <td valign="top">
                        <b><input name="userName" type="text" maxlength="20" size="10"></b>
                    </td>
                </tr>

                <tr>
                    <td valign="top" align="right">
                        <b>Password:</b>
                    </td>
                    <td valign="top">
                        <b><input name="password" type="password" maxlength="20" size="6"></b>
                    </td>
                </tr>
                <tr>
                    <td valign="top" align="right">
                        <b>Organization:</b>
                    </td>
                    <td valign="top">
                        <b><input name="organization" type="text" maxlength="20" size="10"></b>
                    </td>
                </tr>
                <tr>
                    <td valign="top" align="right">
                        <b>Email:</b>
                    </td>
                    <td valign="top">
                        <b><input name="email" type="text" maxlength="40" size="40"></b>
                    </td>
                </tr>
                <tr>
                    <td valign="top" align="right">
                        <b>Say briefly why you're not a robot:</b>
                    </td>
                    <td valign="top">
                        <b><input name="notRobot" type="text" maxlength="80" size="80"></b>
                    </td>
                </tr>
                <tr>
                    <td valign="center">
                        <b><input value="Register" type="submit"></b>
                    </td>
                </tr>
            </table>
        </form>
        <% 
            } 
        %>
    </body>
</html>