<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
    import="com.articulate.sigma.user.UserManager,
            com.articulate.sigma.utils.ValidationUtils,
            com.articulate.sigma.utils.StringUtil" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Forgot Password</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background: #f5f7fa;
            margin: 0;
            padding: 40px;
        }

        .password-card {
            max-width: 460px;
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

        .help {
            color: #555;
            line-height: 1.4;
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

        button {
            width: 100%;
            margin-top: 16px;
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

        .message {
            padding: 10px 12px;
            border-radius: 4px;
            background: #eaf7ea;
            color: #1f6b2a;
            border: 1px solid #b9dfb9;
            margin-bottom: 18px;
        }

        .link-row {
            margin-top: 18px;
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
                            <b>Sigma Password Reset </b>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
    <hr>
    <br>
    <div class="password-card">
        <h1>Reset Password</h1>
        <%
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method)) {
            String email = request.getParameter("email");
            UserManager userManager = (UserManager) application.getAttribute("userManager");
            userManager.requestPasswordReset(email);
        %>
            <div class="message">
                If an account exists for that email address, a password reset link will be sent.
            </div>
            <div class="link-row">
                <a href="login.jsp">&larr; Return to login</a>
            </div>
        <%
        } else {
        %>
            <p class="help">
                Enter your email address and we will send you a password reset link if an account exists.
            </p>
            <form method="post" action="ForgotPassword.jsp">
                <label for="email">Email address</label>
                <input id="email" type="email" name="email" maxlength="40" required>
                <button type="submit">Send reset link</button>
            </form>
            <div class="link-row">
                <a href="login.jsp">&larr; Return to login</a>
            </div>
        <%
        }
        %>
    </div>
</body>
</html>