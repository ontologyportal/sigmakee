<%@ page language="java" contentType="text/html; charset=US-ASCII" pageEncoding="US-ASCII" import="com.articulate.sigma.user.UserManager, com.articulate.sigma.utils.ValidationUtils" %>
<%
String error = "";
if ("POST".equalsIgnoreCase(request.getMethod())) {
    String username = ValidationUtils.sanitizeString(request.getParameter("username"));
    String password = request.getParameter("password");
    UserManager userManager = (UserManager) application.getAttribute("userManager");
    boolean loggedIn = userManager.login(request, username, password);
    if (loggedIn) {
        System.out.println("login.jsp: Successful login for " + username);
        response.sendRedirect("KBs.jsp");
        return;
    }
    else {
        System.err.println("Bad login attempt in login.jsp for " + username);
        error = "Incorrect username or password!";
    }
}
%>
<html>
<head>
    <title>Sigma Login</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background: #f5f7fa;
            margin: 0;
            padding: 40px;
        }

        .login-card {
            max-width: 420px;
            margin: 0 auto;
            background: white;
            padding: 28px 32px;
            border-radius: 8px;
            box-shadow: 0 2px 12px rgba(0,0,0,0.12);
        }

        .logo-row {
            text-align: center;
            margin-bottom: 20px;
        }

        .logo-row img {
            margin: 4px;
        }

        h1 {
            margin: 10px 0 6px;
            font-size: 24px;
            text-align: center;
        }

        .subtitle {
            text-align: center;
            color: #555;
            margin-bottom: 22px;
        }

        .form-row {
            margin-bottom: 16px;
        }

        label {
            display: block;
            font-weight: bold;
            margin-bottom: 6px;
        }

        input[type="text"],
        input[type="password"] {
            width: 100%;
            box-sizing: border-box;
            padding: 9px 10px;
            border: 1px solid #bbb;
            border-radius: 4px;
            font-size: 14px;
        }

        .message.error {
            padding: 10px 12px;
            border-radius: 4px;
            background: #fdecea;
            color: #8a1f11;
            border: 1px solid #f5c2c0;
            margin-bottom: 18px;
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

        .links {
            margin-top: 18px;
            text-align: center;
        }

        .register-box {
            margin-top: 22px;
            padding-top: 18px;
            border-top: 1px solid #ddd;
            text-align: center;
        }

        .note {
            color: #666;
            font-size: 13px;
            margin-top: 8px;
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
                            <b>Sigma Login </b>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
    <hr>
    <br>
    <div class="login-card">
        <h1>Sigma Login</h1>
        <div class="subtitle">Sign in to access SigmaKEE.</div>

        <% if (!error.isEmpty()) { %>
            <div class="message error"><%= error %></div>
        <% } %>

        <form method="POST" action="login.jsp">
            <div class="form-row">
                <label for="username">Username</label>
                <input id="username" name="username" type="text" maxlength="20" required autofocus>
            </div>

            <div class="form-row">
                <label for="password">Password</label>
                <input id="password" name="password" type="password" maxlength="20" required>
            </div>

            <button type="submit">Log In</button>
        </form>

        <div class="links">
            <a href="ForgotPassword.jsp">Forgot your password?</a>
        </div>

        <div class="register-box">
            <form method="POST" action="Register.jsp">
                <button type="submit">Register New Account</button>
            </form>
            <div class="note">New accounts require moderator approval.</div>
        </div>
    </div>

    <%@include file="fragments/universal/Postlude.jspf" %>
</body>
</html>