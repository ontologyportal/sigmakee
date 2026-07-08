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
        else if (!username.matches("[A-Za-z0-9_.-]+")) error = "Username can only contain letters, numbers, periods, hyphens, and underscores.";
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
            else error = "That username or email may already be registered.";
        }
    }
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
    <meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
    <title>Register</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background: #f5f7fa;
            margin: 0;
            padding: 40px;
        }

        .register-card {
            max-width: 520px;
            margin: 0 auto;
            background: white;
            padding: 28px 32px;
            border-radius: 8px;
            box-shadow: 0 2px 12px rgba(0,0,0,0.12);
        }

        h1 {
            margin-top: 0;
            font-size: 24px;
        }

        .form-row {
            margin-bottom: 16px;
        }

        label {
            display: block;
            font-weight: bold;
            margin-bottom: 6px;
        }

        input {
            width: 100%;
            box-sizing: border-box;
            padding: 9px 10px;
            border: 1px solid #bbb;
            border-radius: 4px;
            font-size: 14px;
        }

        .help {
            font-size: 12px;
            color: #666;
            margin-top: 4px;
        }

        .message {
            padding: 10px 12px;
            border-radius: 4px;
            margin-bottom: 18px;
        }

        .error {
            background: #fdecea;
            color: #8a1f11;
            border: 1px solid #f5c2c0;
        }

        .success {
            background: #eaf7ea;
            color: #1f6b2a;
            border: 1px solid #b9dfb9;
        }

        button {
            width: 100%;
            padding: 10px;
            background: #2f6feb;
            color: white;
            border: 0;
            border-radius: 4px;
            font-size: 15px;
            font-weight: bold;
            cursor: pointer;
        }

        button:hover {
            background: #255fc9;
        }

        .toplink {
            margin-bottom: 16px;
        }
    </style>
</head>
<body>
    <table width="95%" cellspacing="0" cellpadding="0">
        <tr>
            <td valign="top">
                <table cellspacing="0" cellpadding="0">
                    <tr>
                        <td align="left" valign="top">
                            <img src="pixmaps/sigmaSymbol.gif" alt="pixmaps/sigmaSymbol.gif">
                        </td>
                        <td>&nbsp;&nbsp;</td>
                        <td align="left" valign="top">
                            <img src="pixmaps/logoText.gif" alt="pixmaps/logoText.gif"><br>
                            <b>Sigma Account Registration </b>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
    <hr>
    <br>
    <div class="register-card">
        <h1>Create an Account</h1>
        <p>All fields are required. New accounts require admin approval.</p>

        <% if (error != null) { %>
            <div class="message error">
                <%= ValidationUtils.sanitizeString(error) %>
            </div>
        <% } %>

        <% if (success != null) { %>
            <div class="toplink">
                <a href="KBs.jsp">&larr; Home</a>
            </div>
            <div class="message success">
                <%= ValidationUtils.sanitizeString(success) %>
            </div>
        <% } else { %>

        <form method="post" action="Register.jsp">

            <div class="form-row">
                <label for="userName">Username</label>
                <input id="userName" name="userName" type="text" maxlength="20"
                       pattern="[A-Za-z0-9_.-]+" required>
                <div class="help">Letters, numbers, periods, hyphens, and underscores only.</div>
            </div>

            <div class="form-row">
                <label for="password">Password</label>
                <input id="password" name="password" type="password" maxlength="20" required>
            </div>

            <div class="form-row">
                <label for="firstName">First / given name</label>
                <input id="firstName" name="firstName" type="text" maxlength="20" required>
            </div>

            <div class="form-row">
                <label for="lastName">Last / surname</label>
                <input id="lastName" name="lastName" type="text" maxlength="20" required>
            </div>

            <div class="form-row">
                <label for="organization">Organization</label>
                <input id="organization" name="organization" type="text" maxlength="20" required>
            </div>

            <div class="form-row">
                <label for="email">Email</label>
                <input id="email" name="email" type="email" maxlength="40" required>
            </div>

            <div class="form-row">
                <label for="notRobot">Briefly explain why you are not a robot</label>
                <input id="notRobot" name="notRobot" type="text" maxlength="80" required>
            </div>

            <button type="submit">Register</button>
        </form>

        <% } %>
    </div>
    <%@include file="fragments/universal/Postlude.jspf" %>
</body>
</html>